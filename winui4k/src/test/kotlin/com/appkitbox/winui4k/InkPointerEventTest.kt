package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winui.InkInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that InkPointerEvent returns null / empty without throwing even for event args that can no longer be read by
 * the time they reach the UI thread (which actually happens with the WinUI 2.5 experimental release), and checks the
 * values when the modifier keys can be read.
 * The event args are reproduced with a fake IPointerEventArgs implemented in Kotlin.
 */
class InkPointerEventTest : FunSpec() {
    /** A fake IPointerEventArgs. CurrentPoint and GetIntermediatePoints return CO_E_NOT_SUPPORTED; KeyModifiers returns [modifiers]. */
    private fun fakeArgs(modifiers: Int?): KComObject {
        val outPtr = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
        return KComObject("WinUI4K.Test.PointerEventArgs").addInterface(
            InkInterop.IID_IPointerEventArgs,
            listOf(
                // vtbl[6] get_CurrentPoint
                KComObject.Method(outPtr) { CO_E_NOT_SUPPORTED },
                // vtbl[7] get_KeyModifiers
                KComObject.Method(outPtr) { args ->
                    if (modifiers == null) {
                        CO_E_NOT_SUPPORTED
                    } else {
                        Ffi.backend.memory.putInt(args[1] as Ptr, 0, modifiers)
                        KComObject.S_OK
                    }
                },
                // vtbl[8] GetIntermediatePoints
                KComObject.Method(outPtr) { CO_E_NOT_SUPPORTED },
            ),
        )
    }

    init {
        test("for unreadable event args, the position and the like are null, intermediate points are empty, handled is false, and nothing throws") {
            onUiThreadGet {
                val presenter = WInkCanvas().inkPresenter
                val args = fakeArgs(modifiers = null)
                val event = InkPointerEvent(presenter, ComPtr(args.primary))
                event.isHandled = true // Does not throw even if it cannot be written
                val result = listOf(event.pointerPoint, event.modifiers, event.intermediatePoints, event.isHandled, event.source === presenter)
                event.dispose()
                args.release()
                result
            } shouldBe listOf(null, null, emptyList<Any>(), false, true)
        }

        test("readable modifier keys become a set of VirtualKeyModifier, and the same value is returned after the call") {
            onUiThreadGet {
                val args = fakeArgs(modifiers = VirtualKeyModifier.CONTROL.native or VirtualKeyModifier.SHIFT.native)
                val event = InkPointerEvent(WInkCanvas().inkPresenter, ComPtr(args.primary))
                val during = event.modifiers
                event.dispose()
                args.release()
                during to event.modifiers
            } shouldBe (setOf(VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT) to setOf(VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT))
        }

        test("values not read during the listener call are null afterwards, without being read") {
            onUiThreadGet {
                val args = fakeArgs(modifiers = VirtualKeyModifier.MENU.native)
                val event = InkPointerEvent(WInkCanvas().inkPresenter, ComPtr(args.primary))
                event.dispose()
                args.release()
                event.modifiers
            } shouldBe null
        }
    }

    private companion object {
        /** CO_E_NOT_SUPPORTED (the value returned on real hardware when CurrentPoint was read from the UI thread). */
        val CO_E_NOT_SUPPORTED = 0x80004021.toInt()
    }
}
