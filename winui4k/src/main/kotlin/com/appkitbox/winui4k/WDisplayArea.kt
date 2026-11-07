package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * A rectangular area on the screen (x, y, width and height are all integer physical pixels).
 * As with java.awt.Rectangle, Java can read it either through the fields (`r.x`) or the getters (`r.getX()`).
 */
data class WRectangle(@JvmField val x: Int, @JvmField val y: Int, @JvmField val width: Int, @JvmField val height: Int) {
    /** A getter for Java (the same as [x]). */
    fun getX(): Int = x

    /** A getter for Java (the same as [y]). */
    fun getY(): Int = y

    /** A getter for Java (the same as [width]). */
    fun getWidth(): Int = width

    /** A getter for Java (the same as [height]). */
    fun getHeight(): Int = height
}

/**
 * GraphicsDevice-like: WinUI 3's DisplayArea (a display's viewable area info).
 * Obtained via [primary] (GraphicsEnvironment.getDefaultScreenDevice) / [all] (getScreenDevices) / [nearest].
 *
 * Provides [isPrimary] / [workArea] (the working area excluding things like the taskbar) /
 * [outerBounds] (the display's whole area).
 * The DisplayArea reference is released on the UI thread when this WDisplayArea is collected by GC (it can be read while kept in a variable).
 */
class WDisplayArea internal constructor(internal val displayArea: ComPtr) {
    init {
        // Release on the UI thread once it becomes unreachable (the same mechanism as WComponent)
        ComLifetime.adopt(this, displayArea)
    }

    /** Whether this is the primary display (DisplayArea.IsPrimary). */
    val isPrimary: Boolean get() = displayArea.getBool(WindowingInterop.IDisplayArea_get_IsPrimary)

    /** The working area excluding things like the taskbar (DisplayArea.WorkArea). Used for computing window centering, etc. */
    val workArea: WRectangle
        get() = XamlStructs.getRectInt32(displayArea, WindowingInterop.IDisplayArea_get_WorkArea).let {
            WRectangle(it[0], it[1], it[2], it[3])
        }

    /** The display's whole area (DisplayArea.OuterBounds). */
    val outerBounds: WRectangle
        get() = XamlStructs.getRectInt32(displayArea, WindowingInterop.IDisplayArea_get_OuterBounds).let {
            WRectangle(it[0], it[1], it[2], it[3])
        }

    /** An alias of [outerBounds] (GraphicsDevice.getDefaultConfiguration().getBounds()-like). */
    val bounds: WRectangle get() = outerBounds

    companion object {
        private val statics: ComPtr by lazy {
            Activation.factory(WindowingInterop.CLS_DisplayArea, WindowingInterop.IID_IDisplayAreaStatics)
        }

        /** Gets the primary display's DisplayArea (DisplayAreaStatics.Primary). */
        @JvmStatic
        fun primary(): WDisplayArea = WDisplayArea(statics.getPtr(WindowingInterop.IDisplayAreaStatics_get_Primary))

        /**
         * Gets the DisplayAreas of all connected displays (DisplayAreaStatics.FindAll).
         * GraphicsEnvironment.getScreenDevices-like. An empty list if there are no displays.
         */
        @JvmStatic
        fun all(): List<WDisplayArea> {
            val view = statics.getPtr(WindowingInterop.IDisplayAreaStatics_FindAll)
            try {
                return Ffi.backend.withScope { scope ->
                    val memory = Ffi.backend.memory
                    val size = scope.allocate(4)
                    view.call(FoundationInterop.IVectorView_get_Size, size)
                    (0 until memory.getInt(size, 0)).map { i ->
                        val out = scope.allocate(8)
                        view.call(FoundationInterop.IVectorView_GetAt, i, out)
                        WDisplayArea(ComPtr(memory.getPtr(out, 0)))
                    }
                }
            } finally {
                view.release()
            }
        }

        /**
         * Gets the DisplayArea closest to whatever display [appWindow] is on
         * (DisplayAreaStatics.GetFromWindowId, with the fallback fixed to Nearest).
         */
        @JvmStatic
        fun nearest(appWindow: WAppWindow): WDisplayArea = Ffi.backend.withScope { scope ->
            val windowId = XamlStructs.windowIdValue(scope, appWindow.id)
            WDisplayArea(
                statics.getPtr(
                    WindowingInterop.IDisplayAreaStatics_GetFromWindowId,
                    windowId,
                    WindowingInterop.DisplayAreaFallback_Nearest,
                ),
            )
        }

        /** The work area of the primary display (physical pixels). The DisplayArea is released right away. */
        internal fun primaryWorkArea(): WRectangle = workAreaOf(statics.getPtr(WindowingInterop.IDisplayAreaStatics_get_Primary))

        /** The work area of the display nearest to the screen point ([x], [y], physical pixels) (DisplayAreaStatics.GetFromPoint, Nearest). */
        internal fun workAreaAt(x: Int, y: Int): WRectangle = Ffi.backend.withScope { scope ->
            val point = scope.allocate(XamlStructs.POINT_INT32)
            Ffi.backend.memory.putInt(point.ptr, 0, x)
            Ffi.backend.memory.putInt(point.ptr, 4, y)
            workAreaOf(
                statics.getPtr(WindowingInterop.IDisplayAreaStatics_GetFromPoint, point, WindowingInterop.DisplayAreaFallback_Nearest),
            )
        }

        private fun workAreaOf(displayArea: ComPtr): WRectangle = try {
            XamlStructs.getRectInt32(displayArea, WindowingInterop.IDisplayArea_get_WorkArea).let { WRectangle(it[0], it[1], it[2], it[3]) }
        } finally {
            displayArea.release()
        }
    }
}
