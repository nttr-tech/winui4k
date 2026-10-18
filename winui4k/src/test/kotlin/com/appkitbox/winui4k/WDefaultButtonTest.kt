package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies WFrame.defaultButton (the equivalent of JRootPane.setDefaultButton).
 * The Enter key is sent with [TestKeyboard] to the input child window of the shared window (it arrives even when the
 * window is not in the foreground).
 */
class WDefaultButtonTest : FunSpec() {
    private val frame by lazy { UiTestHarness.sharedFrame() }

    /** Whether the Style set on the button is AccentButtonStyle or SubtleButtonStyle (null if neither). */
    private fun styleKeyOf(button: WButton): String? {
        val style = button.frameworkElement.getPtrOrNull(XamlInterop.IFrameworkElement_get_Style) ?: return null
        return try {
            listOf("AccentButtonStyle", "SubtleButtonStyle").firstOrNull { key ->
                val candidate = WinUiUtilities.lookupApplicationResource(key)
                try {
                    Xaml.sameObject(style, candidate)
                } finally {
                    candidate.release()
                }
            }
        } finally {
            style.release()
        }
    }

    /** The number of UIElement.KeyboardAccelerators. */
    private fun acceleratorCount(component: WComponent): Int {
        val accelerators = component.uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        return try {
            accelerators.getInt(FoundationInterop.IVector_get_Size)
        } finally {
            accelerators.release()
        }
    }

    /** Buttons that record the names of the buttons pressed, and the panel that holds them. */
    private class Form(val panel: WPanel, val clicks: MutableList<String>) {
        val ok = WButton("OK")
        val other = WButton("Other")
        val text = WTextField()
        val check = WCheckBox("Check")

        init {
            ok.addActionListener { clicks += "OK" }
            other.addActionListener { clicks += "Other" }
            listOf(ok, other, text, check).forEach { panel.add(it) }
        }
    }

    private fun showForm(): Form {
        val form = onUiThreadGet { Form(WPanel(), mutableListOf()) }
        UiTestHarness.attachAndAwaitLoaded(form.panel)
        return form
    }

    private fun closeForm(form: Form) {
        onUiThread { frame.defaultButton = null }
        UiTestHarness.detach(form.panel)
    }

    /** Moves focus to [focusTarget], presses Enter, and returns the record of pressed buttons. */
    private suspend fun pressEnterOn(form: Form, focusTarget: WComponent, expected: List<String>): List<String> {
        onUiThread {
            form.clicks.clear()
            check(focusTarget.requestFocusInWindow()) { "could not move focus" }
        }
        TestKeyboard.pressEnter()
        var clicks = emptyList<String>()
        eventually(3.seconds) {
            clicks = onUiThreadGet { form.clicks.toList() }
            clicks shouldBe expected
        }
        // Also verify that no delayed second click arrives
        Thread.sleep(SETTLE_MILLIS)
        return onUiThreadGet { form.clicks.toList() }
    }

    init {
        test("defaultButton is null by default, and setting it makes that button's isDefaultButton true and shows it with the accent") {
            val form = showForm()
            try {
                val (initial, flags) = onUiThreadGet {
                    val initial = frame.defaultButton
                    frame.defaultButton = form.ok
                    initial to listOf(form.ok.isDefaultButton, form.other.isDefaultButton)
                }
                initial shouldBe null
                flags shouldContainExactly listOf(true, false)
                onUiThreadGet { frame.defaultButton } shouldBeSameInstanceAs form.ok
                onUiThreadGet { styleKeyOf(form.ok) } shouldBe "AccentButtonStyle"
                onUiThreadGet { form.ok.isAccent }.shouldBeFalse()
            } finally {
                closeForm(form)
            }
        }

        test("changing defaultButton to another button makes the previous one no longer the default button and removes its appearance and Enter accelerator") {
            val form = showForm()
            try {
                val result = onUiThreadGet {
                    frame.defaultButton = form.ok
                    val withOk = acceleratorCount(form.ok)
                    frame.defaultButton = form.other
                    listOf(
                        withOk,
                        acceleratorCount(form.ok),
                        acceleratorCount(form.other),
                        form.ok.isDefaultButton,
                        form.other.isDefaultButton,
                        styleKeyOf(form.ok),
                        styleKeyOf(form.other),
                    )
                }
                result shouldContainExactly listOf(1, 0, 1, false, true, null, "AccentButtonStyle")
            } finally {
                closeForm(form)
            }
        }

        test("setting defaultButton back to null restores the original appearance (isAccent, isContentAreaFilled = false) and removes the accelerator") {
            val form = showForm()
            try {
                val result = onUiThreadGet {
                    form.ok.isContentAreaFilled = false
                    form.other.isAccent = true
                    frame.defaultButton = form.ok
                    val okWhileDefault = styleKeyOf(form.ok)
                    frame.defaultButton = form.other
                    val otherWhileDefault = styleKeyOf(form.other)
                    frame.defaultButton = null
                    listOf(okWhileDefault, otherWhileDefault, styleKeyOf(form.ok), styleKeyOf(form.other), acceleratorCount(form.other))
                }
                result shouldContainExactly listOf("AccentButtonStyle", "AccentButtonStyle", "SubtleButtonStyle", "AccentButtonStyle", 0)
                onUiThreadGet { form.other.isAccent }.shouldBeTrue()
            } finally {
                onUiThread { form.other.isAccent = false }
                closeForm(form)
            }
        }

        test("setting the same button as defaultButton repeatedly keeps a single accelerator") {
            val form = showForm()
            try {
                onUiThreadGet {
                    frame.defaultButton = form.ok
                    frame.defaultButton = form.ok
                    acceleratorCount(form.ok)
                } shouldBe 1
            } finally {
                closeForm(form)
            }
        }

        test("with focus on a control that does not handle Enter (CheckBox), Enter presses the default button once") {
            val form = showForm()
            try {
                onUiThread { frame.defaultButton = form.ok }
                pressEnterOn(form, form.check, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("Enter with focus on the default button itself presses it only once (not twice)") {
            val form = showForm()
            try {
                onUiThread { frame.defaultButton = form.ok }
                pressEnterOn(form, form.ok, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("measured: with focus on a WButton that is not the default button, Enter presses that button and not the default button") {
            // ButtonBase marks Enter's KeyDown as handled (KeyDown in ButtonBaseKeyProcess.h), so it does not reach
            // the global accelerator, which runs only when the event is not handled
            val form = showForm()
            try {
                onUiThread { frame.defaultButton = form.ok }
                pressEnterOn(form, form.other, listOf("Other")) shouldContainExactly listOf("Other")
            } finally {
                closeForm(form)
            }
        }

        test("with focus on a single-line WTextField without an ActionListener, Enter presses the default button once (same as Swing)") {
            // A plain TextBox does not let the global accelerator run (see the measurement below), so WTextField presses the default
            // button in its KeyDown handler
            val form = showForm()
            try {
                onUiThread { frame.defaultButton = form.ok }
                pressEnterOn(form, form.text, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("measured: with focus on a plain WinUI single-line TextBox (not a WTextField), Enter does not press the default button") {
            // TextBox passes Enter's KeyDown to the text engine (RichEdit) and marks it as handled (CTextBoxBase::OnKeyDown in
            // TextBoxBase.cpp). When KeyDown ends up handled, the global accelerator does not run (KeyboardInputProcessor.cpp)
            val form = showForm()
            val textBox = onUiThreadGet { XamlElement.load("<TextBox />") }
            onUiThread { form.panel.add(textBox) }
            try {
                onUiThread { frame.defaultButton = form.ok }
                pressEnterOn(form, textBox, emptyList()) shouldContainExactly emptyList()
            } finally {
                closeForm(form)
            }
        }

        test("even with focus on a single-line WTextField, a disabled default button is not pressed by Enter") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    form.ok.isEnabled = false
                }
                pressEnterOn(form, form.text, emptyList()) shouldContainExactly emptyList()
            } finally {
                onUiThread { form.ok.isEnabled = true }
                closeForm(form)
            }
        }

        test("a disabled default button is not pressed by Enter") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    form.ok.isEnabled = false
                }
                pressEnterOn(form, form.check, emptyList()) shouldContainExactly emptyList()
            } finally {
                onUiThread { form.ok.isEnabled = true }
                closeForm(form)
            }
        }

        test("after setting defaultButton back to null, Enter does not press the former default button") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    frame.defaultButton = null
                }
                pressEnterOn(form, form.check, emptyList()) shouldContainExactly emptyList()
            } finally {
                closeForm(form)
            }
        }

        test("after changing defaultButton, Enter presses only the new default button") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    frame.defaultButton = form.other
                }
                pressEnterOn(form, form.check, listOf("Other")) shouldContainExactly listOf("Other")
            } finally {
                closeForm(form)
            }
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 700L
    }
}
