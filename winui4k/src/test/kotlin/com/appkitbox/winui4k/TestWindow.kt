package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

/**
 * A yardstick for on-screen positions in tests: measures the top-left of the shared window's content
 * (InputSiteWindowClass) with Win32's ClientToScreen, and gets the pixels per DIP with GetDpiForWindow. These values do
 * not go through the WinUI API, so the expected values of [WComponent.locationOnScreen] can be derived independently.
 */
internal object TestWindow {
    private val clientToScreen by lazy {
        Ffi.backend.function("user32.dll", "ClientToScreen", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR))
    }
    private val getDpiForWindow by lazy {
        Ffi.backend.function("user32.dll", "GetDpiForWindow", CallDescriptor(ValueKind.I32, ArgKind.PTR))
    }

    /** The on-screen position (in pixels) of the top-left of the shared window's content. */
    fun contentOriginOnScreen(): WPoint = Ffi.backend.withScope { scope ->
        val point = scope.allocate(8)
        Ffi.backend.memory.putInt(point, 0, 0)
        Ffi.backend.memory.putInt(point, 4, 0)
        check(clientToScreen(TestKeyboard.inputSite(), point) as Int != 0) { "ClientToScreen failed" }
        WPoint(Ffi.backend.memory.getInt(point, 0), Ffi.backend.memory.getInt(point, 4))
    }

    /** The number of pixels per DIP of the shared window (96 DPI = 1.0). */
    fun pixelsPerDip(): Double = (getDpiForWindow(TestKeyboard.inputSite()) as Int) / DEFAULT_DPI

    private const val DEFAULT_DPI = 96.0
}
