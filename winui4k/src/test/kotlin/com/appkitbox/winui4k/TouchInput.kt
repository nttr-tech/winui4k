package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Double-taps an element of the shared test window with injected touch input (user32 InjectTouchInput), so that WinUI
 * raises real input events (Tapped / DoubleTapped) from its own input pipeline. Touch input does not move the mouse
 * cursor. The window is made topmost while injecting (input goes to whatever window is under the point), and the
 * injection is refused if the point is not in the test window.
 */
internal object TouchInput {
    private const val USER32 = "user32.dll"

    private val initializeTouchInjection by lazy {
        Ffi.backend.function(USER32, "InitializeTouchInjection", CallDescriptor(ValueKind.I32, ArgKind.I32, ArgKind.I32))
    }
    private val injectTouchInput by lazy {
        Ffi.backend.function(USER32, "InjectTouchInput", CallDescriptor(ValueKind.I32, ArgKind.I32, ArgKind.PTR))
    }
    private val clientToScreen by lazy {
        Ffi.backend.function(USER32, "ClientToScreen", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR))
    }
    private val windowFromPoint by lazy {
        Ffi.backend.function(USER32, "WindowFromPoint", CallDescriptor(ValueKind.PTR, ArgKind.I64)) // POINT by value
    }
    private val getAncestor by lazy {
        Ffi.backend.function(USER32, "GetAncestor", CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.I32))
    }
    private val setWindowPos by lazy {
        Ffi.backend.function(
            USER32,
            "SetWindowPos",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.I32, ArgKind.I32, ArgKind.I32, ArgKind.I32, ArgKind.I32),
        )
    }

    private var initialized = false

    /** Double-taps the center of [element] (an element in the shared test window). */
    fun doubleTap(element: ComPtr) {
        val hwnd = Ptr(onUiThreadGet { UiTestHarness.sharedFrame() }.appWindow.id)
        val (x, y) = onUiThreadGet { screenCenterOf(element, hwnd) }
        setTopmost(hwnd, true)
        try {
            Thread.sleep(SETTLE_MILLIS)
            val target = getAncestor(windowFromPoint((y.toLong() shl 32) or (x.toLong() and 0xFFFFFFFFL)), GA_ROOT) as Ptr
            check(target == hwnd) { "The point ($x, $y) is not in the test window, so no touch input was injected" }
            if (!initialized) {
                check(initializeTouchInjection(1, TOUCH_FEEDBACK_NONE) as Int != 0) { "InitializeTouchInjection failed" }
                initialized = true
            }
            repeat(2) {
                inject(x, y, POINTER_FLAG_DOWN or POINTER_FLAG_INRANGE or POINTER_FLAG_INCONTACT)
                Thread.sleep(TAP_MILLIS)
                inject(x, y, POINTER_FLAG_UP)
                Thread.sleep(TAP_MILLIS)
            }
        } finally {
            setTopmost(hwnd, false)
        }
    }

    /** The center of [element] in screen pixels: its position in the window content scaled to pixels, plus the client origin. */
    private fun screenCenterOf(element: ComPtr, hwnd: Ptr): Pair<Int, Int> {
        val ui = element.queryInterface(XamlInterop.IID_IUIElement)
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        try {
            val position = Xaml.positionInRoot(ui)
            val centerX = position[0] + framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth) / 2
            val centerY = position[1] + framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight) / 2
            val root = ui.getPtr(XamlInterop.IUIElement_get_XamlRoot)
            val scale = try {
                root.getDouble(XamlInterop.IXamlRoot_get_RasterizationScale)
            } finally {
                root.release()
            }
            return Ffi.backend.withScope { scope ->
                val point = scope.allocate(8)
                Ffi.backend.memory.putInt(point, 0, 0)
                Ffi.backend.memory.putInt(point, 4, 0)
                clientToScreen(hwnd, point)
                Pair(
                    Ffi.backend.memory.getInt(point, 0) + (centerX * scale).toInt(),
                    Ffi.backend.memory.getInt(point, 4) + (centerY * scale).toInt(),
                )
            }
        } finally {
            framework.release()
            ui.release()
        }
    }

    /** Injects one touch frame (a POINTER_TOUCH_INFO) at ([x], [y]) with [flags]. */
    private fun inject(x: Int, y: Int, flags: Int) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val info = scope.allocate(POINTER_TOUCH_INFO_BYTES, 8)
            for (offset in 0 until POINTER_TOUCH_INFO_BYTES step 4) memory.putInt(info, offset, 0)
            // POINTER_INFO: pointerType, pointerId, frameId, pointerFlags, sourceDevice, hwndTarget, ptPixelLocation, ...
            memory.putInt(info, 0, PT_TOUCH)
            memory.putInt(info, 12, flags)
            memory.putInt(info, 32, x)
            memory.putInt(info, 36, y)
            // touchFlags (96) = TOUCH_FLAG_NONE, touchMask (100) = TOUCH_MASK_CONTACTAREA, rcContact (104) = a small square
            memory.putInt(info, 100, TOUCH_MASK_CONTACTAREA)
            memory.putInt(info, 104, x - CONTACT_RADIUS)
            memory.putInt(info, 108, y - CONTACT_RADIUS)
            memory.putInt(info, 112, x + CONTACT_RADIUS)
            memory.putInt(info, 116, y + CONTACT_RADIUS)
            check(injectTouchInput(1, info) as Int != 0) { "InjectTouchInput failed" }
        }
    }

    private fun setTopmost(hwnd: Ptr, topmost: Boolean) {
        setWindowPos(hwnd, Ptr(if (topmost) HWND_TOPMOST else HWND_NOTOPMOST), 0, 0, 0, 0, SWP_NOSIZE or SWP_NOMOVE or SWP_NOACTIVATE)
    }

    private const val GA_ROOT = 2
    private const val TOUCH_FEEDBACK_NONE = 3
    private const val PT_TOUCH = 2
    private const val POINTER_FLAG_INRANGE = 0x2
    private const val POINTER_FLAG_INCONTACT = 0x4
    private const val POINTER_FLAG_DOWN = 0x10000
    private const val POINTER_FLAG_UP = 0x40000
    private const val TOUCH_MASK_CONTACTAREA = 0x1
    private const val CONTACT_RADIUS = 2
    private const val POINTER_TOUCH_INFO_BYTES = 144L
    private const val HWND_TOPMOST = -1L
    private const val HWND_NOTOPMOST = -2L
    private const val SWP_NOSIZE = 0x1
    private const val SWP_NOMOVE = 0x2
    private const val SWP_NOACTIVATE = 0x10

    /** How long the window takes to come to the top, and the time between touch frames (well within the double-tap time). */
    private const val SETTLE_MILLIS = 200L
    private const val TAP_MILLIS = 40L
}
