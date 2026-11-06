package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Measurements of the scope of keys in which WinUI's KeyboardAccelerator works (to compare with Swing's WHEN_FOCUSED / WHEN_ANCESTOR_OF_FOCUSED_COMPONENT /
 * WHEN_IN_FOCUSED_WINDOW; doc/verification.md). Confirms here why key bindings ([WComponent.getInputMap]) are not built
 * with accelerators. Keys are sent with [TestKeyboard] (modifier keys are not delivered, so F7 is used).
 */
@Tags("Integration")
class KeyboardAcceleratorScopeTest : FunSpec() {
    /** A form with an inner panel (with a button inside), an outer button and a button to which the accelerator is attached, arranged in a root panel. */
    private class Form {
        val root = WPanel()
        val inner = WPanel()
        val innerButton = WButton("Inner")
        val outerButton = WButton("Outer")
        val target = WButton("Target")
        val events = mutableListOf<String>()

        init {
            inner.add(innerButton)
            listOf(inner, outerButton, target).forEach { root.add(it) }
            target.addActionListener { events += "click" }
        }

        /** Adds an F7 accelerator to the KeyboardAccelerators of [owner]. Records [name] on Invoked and marks it handled if [handled]. */
        fun addAccelerator(owner: WComponent, name: String, handled: Boolean = true, scopeOwner: WComponent? = null) {
            val accelerator = createKeyboardAccelerator(VK_F7, 0)
            if (scopeOwner != null) {
                val dependency = scopeOwner.uiElement.queryInterface(XamlInterop.IID_IDependencyObject)
                try {
                    accelerator.call(XamlInterop.IKeyboardAccelerator_put_ScopeOwner, dependency.ptr)
                } finally {
                    dependency.release()
                }
            }
            accelerator.addEventHandler(
                "WinUI4K.TestAcceleratorInvoked",
                XamlInterop.IID_KeyboardAcceleratorInvokedHandler,
                XamlInterop.IKeyboardAccelerator_add_Invoked,
            ) { _, args ->
                events += name
                if (handled) ComPtr(args).putBool(XamlInterop.IKeyboardAcceleratorInvokedEventArgs_put_Handled, true)
            }
            owner.appendKeyboardAccelerator(accelerator)
        }

        /** Records "root" on KeyDown of the root panel (bubbling from the focus to the root) and marks it handled if [handled]. */
        fun recordRootKeyDown(handled: Boolean) {
            root.uiElement.addEventHandler(
                "WinUI4K.TestRootKeyDown",
                XamlInterop.IID_KeyEventHandler,
                XamlInterop.IUIElement_add_KeyDown,
            ) { _, args ->
                if (XamlKeyEvent(ComPtr(args)).key == VK_F7) {
                    events += "root"
                    if (handled) XamlKeyEvent(ComPtr(args)).handled = true
                }
            }
        }
    }

    private fun showForm(): Form {
        val form = onUiThreadGet { Form() }
        UiTestHarness.attachAndAwaitLoaded(form.root)
        return form
    }

    /** Moves the focus to [focusTarget], presses F7, waits until the record becomes [expected], and confirms it does not change after waiting a little. */
    private suspend fun pressF7On(form: Form, focusTarget: WComponent, expected: List<String>) {
        onUiThread {
            form.events.clear()
            check(focusTarget.requestFocusInWindow()) { "Cannot move the focus" }
        }
        TestKeyboard.press(VK_F7, SCAN_F7)
        eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe expected }
        Thread.sleep(SETTLE_MILLIS)
        onUiThreadGet { form.events.toList() } shouldContainExactly expected
    }

    init {
        context("Input") {
            test("an accelerator without a ScopeOwner works both when the focus is in a descendant of the owner and when it is outside the owner") {
                val form = showForm()
                try {
                    onUiThread { form.addAccelerator(form.inner, "inner") }
                    pressF7On(form, form.innerButton, listOf("inner"))
                    pressF7On(form, form.outerButton, listOf("inner"))
                } finally {
                    UiTestHarness.detach(form.root)
                }
            }

            test("with the owner as the ScopeOwner, it works only when the focus is inside the owner") {
                val form = showForm()
                try {
                    onUiThread { form.addAccelerator(form.inner, "inner", scopeOwner = form.inner) }
                    pressF7On(form, form.outerButton, emptyList())
                    pressF7On(form, form.innerButton, listOf("inner"))
                } finally {
                    UiTestHarness.detach(form.root)
                }
            }

            test("if not marked handled on Invoked, the owner button is pressed (a click-equivalent operation; no other accelerators are searched)") {
                val form = showForm()
                try {
                    onUiThread {
                        form.addAccelerator(form.target, "target", handled = false)
                        form.addAccelerator(form.inner, "inner")
                    }
                    pressF7On(form, form.outerButton, listOf("target", "click"))
                } finally {
                    UiTestHarness.detach(form.root)
                }
            }

            test("if the owner button is disabled or hidden, it does not work and the next accelerator is searched") {
                val form = showForm()
                try {
                    onUiThread {
                        form.addAccelerator(form.target, "target")
                        form.addAccelerator(form.inner, "inner")
                        form.target.isEnabled = false
                    }
                    pressF7On(form, form.outerButton, listOf("inner"))
                    onUiThread {
                        form.target.isEnabled = true
                        form.target.isVisible = false
                    }
                    pressF7On(form, form.outerButton, listOf("inner"))
                } finally {
                    UiTestHarness.detach(form.root)
                }
            }

            test("an accelerator outside the focus works after KeyDown reaches the root unhandled, and does not work if marked handled at the root") {
                val form = showForm()
                try {
                    onUiThread {
                        form.addAccelerator(form.target, "target")
                        form.recordRootKeyDown(handled = false)
                    }
                    pressF7On(form, form.outerButton, listOf("root", "target"))
                } finally {
                    UiTestHarness.detach(form.root)
                }
                val handledForm = showForm()
                try {
                    onUiThread {
                        handledForm.addAccelerator(handledForm.target, "target")
                        handledForm.recordRootKeyDown(handled = true)
                    }
                    pressF7On(handledForm, handledForm.outerButton, listOf("root"))
                } finally {
                    UiTestHarness.detach(handledForm.root)
                }
            }
        }
    }

    private companion object {
        const val VK_F7 = 0x76
        const val SCAN_F7 = 0x41
        const val SETTLE_MILLIS = 500L
    }
}
