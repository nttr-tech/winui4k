package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.ViewManagementInterop
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * The Windows accent color and its light / dark variants (the Settings > Personalization > Colors accent), which
 * accent-styled controls such as an accent [WButton] use. Values of Windows.UI.ViewManagement.UIColorType.
 */
data class WAccentPalette(
    /** The accent color. */
    val accent: WColor,
    /** The accent, one step lighter. */
    val light1: WColor,
    /** The accent, two steps lighter. */
    val light2: WColor,
    /** The accent, three steps lighter. */
    val light3: WColor,
    /** The accent, one step darker. */
    val dark1: WColor,
    /** The accent, two steps darker. */
    val dark2: WColor,
    /** The accent, three steps darker. */
    val dark3: WColor,
)

/**
 * System color settings (Windows.UI.ViewManagement.UISettings): the accent color palette and a listener for when the
 * user changes it.
 */
object WSystemColors {
    private val settings: ComPtr by lazy { Activation.activate(ViewManagementInterop.CLS_UISettings, ViewManagementInterop.IID_IUISettings3) }
    private val listeners = CopyOnWriteArrayList<Consumer<WAccentPalette>>()
    private val lock = Any()
    private var token: Long? = null

    /** The current accent color (UIColorType.Accent). Call it on the UI thread. */
    @JvmStatic
    val accentColor: WColor
        get() = color(ViewManagementInterop.UIColorType_Accent)

    /** Reads the current accent color and its variants. Call it on the UI thread (COM is initialized only there). */
    @JvmStatic
    fun accentPalette(): WAccentPalette = WAccentPalette(
        accent = color(ViewManagementInterop.UIColorType_Accent),
        light1 = color(ViewManagementInterop.UIColorType_AccentLight1),
        light2 = color(ViewManagementInterop.UIColorType_AccentLight2),
        light3 = color(ViewManagementInterop.UIColorType_AccentLight3),
        dark1 = color(ViewManagementInterop.UIColorType_AccentDark1),
        dark2 = color(ViewManagementInterop.UIColorType_AccentDark2),
        dark3 = color(ViewManagementInterop.UIColorType_AccentDark3),
    )

    /**
     * Calls [listener] with the new palette when the system accent color changes. Windows raises the change on a
     * background thread; the listener is always called on the UI thread.
     */
    @JvmStatic
    fun addAccentChangeListener(listener: Consumer<WAccentPalette>) {
        synchronized(lock) {
            listeners += listener
            if (token == null) {
                token = settings.addEventHandler(
                    "WinUI4K.ColorValuesChanged",
                    ViewManagementInterop.IID_ColorValuesChangedHandler,
                    ViewManagementInterop.IUISettings3_add_ColorValuesChanged,
                ) { _, _ -> notifyAccentChanged() }
            }
        }
    }

    /** Unsubscribes a listener registered via [addAccentChangeListener]. */
    @JvmStatic
    fun removeAccentChangeListener(listener: Consumer<WAccentPalette>) {
        synchronized(lock) {
            listeners -= listener
            val registered = token
            if (listeners.isEmpty() && registered != null) {
                settings.removeEventHandler(ViewManagementInterop.IUISettings3_remove_ColorValuesChanged, registered)
                token = null
            }
        }
    }

    /** Delivers the change to the listeners on the UI thread (called from whichever thread Windows raised it on). */
    internal fun notifyAccentChanged() {
        WinUiUtilities.invokeLater {
            val palette = accentPalette()
            listeners.forEach { it.accept(palette) }
        }
    }

    private fun color(type: Int): WColor = Ffi.backend.withScope { scope ->
        val out = scope.allocate(COLOR_BYTES.toLong(), 1)
        settings.call(ViewManagementInterop.IUISettings3_GetColorValue, type, out)
        val memory = Ffi.backend.memory
        val argb = IntArray(COLOR_BYTES) { memory.getByte(out, it.toLong()).toInt() and 0xFF }
        WColor(red = argb[1], green = argb[2], blue = argb[3], alpha = argb[0])
    }

    private const val COLOR_BYTES = 4
}
