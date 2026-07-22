package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/** A rectangular area on screen (x, y, width, height are all integer physical pixels). */
data class WRectangle(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * WinUI 3's DisplayArea (a display's viewable area info). No Swing equivalent.
 * Obtained via [primary] / [nearest].
 *
 * Provides [isPrimary] / [workArea] (the working area excluding things like the taskbar) /
 * [outerBounds] (the display's whole area).
 */
class WDisplayArea internal constructor(private val displayArea: ComPtr) {
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

    companion object {
        private val statics: ComPtr by lazy {
            Activation.factory(WindowingInterop.CLS_DisplayArea, WindowingInterop.IID_IDisplayAreaStatics)
        }

        /** Gets the primary display's DisplayArea (DisplayAreaStatics.Primary). */
        fun primary(): WDisplayArea = WDisplayArea(statics.getPtr(WindowingInterop.IDisplayAreaStatics_get_Primary))

        /**
         * Gets the DisplayArea closest to whatever display [appWindow] is on
         * (DisplayAreaStatics.GetFromWindowId, with the fallback fixed to Nearest).
         */
        fun nearest(appWindow: WAppWindow): WDisplayArea = Ffi.backend.withScope { scope ->
            val windowId = XamlStructs.windowIdValue(scope, appWindow.id)
            WDisplayArea(
                statics.getPtr(
                    WindowingInterop.IDisplayAreaStatics_GetFromWindowId, windowId, WindowingInterop.DisplayAreaFallback_Nearest,
                ),
            )
        }
    }
}
