package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify the JSpinner.NumberEditor-like [WSpinner.NumberEditor] (which displays and parses NumberBox input with a DecimalFormat pattern).
 * The expected values are the texts Swing's `JSpinner.NumberEditor(spinner, pattern)` shows for the same values (DecimalFormat patterns, HALF_EVEN rounding).
 * The default locale is set to US during the tests so that the separator symbols do not change with the locale.
 */
@Tags("E2E")
class WSpinnerNumberEditorTest : FunSpec() {
    /** Reads the text of the template's input field (InputBox). */
    private fun WSpinner.inputText(): String {
        val box = checkNotNull(Xaml.templatePartOrNull(inspectable, "InputBox")) { "no InputBox" }
        return try {
            box.queryInterface(XamlInterop.IID_ITextBox).let { textBox ->
                try {
                    textBox.getString(XamlInterop.ITextBox_get_Text)
                } finally {
                    textBox.release()
                }
            }
        } finally {
            box.release()
        }
    }

    /** Types [text] into the input field of [spinner] on the window and commits it with Enter (the same path as user input). */
    private fun typeAndCommit(spinner: WSpinner, text: String) {
        onUiThread {
            check(spinner.requestFocusInWindow()) { "cannot move focus" }
            val box = checkNotNull(Xaml.templatePartOrNull(spinner.inspectable, "InputBox"))
            try {
                val textBox = box.queryInterface(XamlInterop.IID_ITextBox)
                try {
                    Hstring.use(text) { h -> textBox.call(XamlInterop.ITextBox_put_Text, h) }
                } finally {
                    textBox.release()
                }
            } finally {
                box.release()
            }
        }
        TestKeyboard.pressEnter()
    }

    private suspend fun withAttached(spinner: WSpinner, block: suspend () -> Unit) {
        UiTestHarness.attachAndAwaitLoaded(spinner)
        try {
            block()
        } finally {
            UiTestHarness.detach(spinner)
        }
    }

    /** A spinner for a price (step 0.5, 0 to 100) with a NumberEditor of [pattern]. */
    private fun priceSpinner(value: Double, pattern: String): WSpinner = onUiThreadGet {
        WSpinner(WSpinnerNumberModel(value, 0.0, 10000.0, 0.5)).also { it.editor = WSpinner.NumberEditor(it, pattern) }
    }

    private var savedLocale: Locale = Locale.getDefault()

    init {
        beforeTest {
            savedLocale = Locale.getDefault()
            Locale.setDefault(Locale.US)
        }
        afterTest { Locale.setDefault(savedLocale) }

        test("with a NumberEditor of pattern 0.00, 12.0 is shown as 12.00 and becomes 12.50 when the value changes") {
            val spinner = priceSpinner(12.0, "0.00")
            withAttached(spinner) {
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "12.00" }
                onUiThread { spinner.value = 12.5 }
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "12.50" }
            }
        }

        test("adding a NumberEditor after showing changes the display to the format, and setting editor back to null returns to NumberBox's default display (12)") {
            val spinner = onUiThreadGet { WSpinner(WSpinnerNumberModel(12.0, 0.0, 100.0, 0.5)) }
            withAttached(spinner) {
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "12" }
                onUiThread { spinner.editor = WSpinner.NumberEditor(spinner, "0.00") }
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "12.00" }
                onUiThread { spinner.editor = null }
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "12" }
            }
        }

        test("an entered 7.256 becomes the value without rounding, and the display follows the format as 7.26 (HALF_EVEN)") {
            val spinner = priceSpinner(12.0, "0.00")
            withAttached(spinner) {
                typeAndCommit(spinner, "7.256")
                eventually(3.seconds) {
                    onUiThreadGet { spinner.value to spinner.inputText() } shouldBe (7.256 to "7.26")
                }
            }
        }

        test("the grouped pattern #,##0.00 shows 1234.5 as 1,234.50, and entering 1,000.5 gives 1000.5") {
            val spinner = priceSpinner(1234.5, "#,##0.00")
            withAttached(spinner) {
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "1,234.50" }
                typeAndCommit(spinner, "1,000.5")
                eventually(3.seconds) {
                    onUiThreadGet { spinner.value to spinner.inputText() } shouldBe (1000.5 to "1,000.50")
                }
            }
        }

        test("committing text that cannot be read in the format (12abc, abc) leaves the value unchanged and restores the display") {
            val spinner = priceSpinner(12.0, "0.00")
            withAttached(spinner) {
                typeAndCommit(spinner, "12abc")
                eventually(3.seconds) { onUiThreadGet { spinner.value to spinner.inputText() } shouldBe (12.0 to "12.00") }
                typeAndCommit(spinner, "abc")
                eventually(3.seconds) { onUiThreadGet { spinner.value to spinner.inputText() } shouldBe (12.0 to "12.00") }
            }
        }

        test("expression input (acceptsExpression) also reads numbers of the format, and (1.5 + 2) * 2 becomes 7.00") {
            val spinner = priceSpinner(12.0, "0.00")
            onUiThread { spinner.acceptsExpression = true }
            withAttached(spinner) {
                typeAndCommit(spinner, "(1.5 + 2) * 2")
                eventually(3.seconds) { onUiThreadGet { spinner.value to spinner.inputText() } shouldBe (7.0 to "7.00") }
            }
        }

        test("getFormat returns the DecimalFormat of the pattern, getSpinner the spinner given at creation, and editor the one set") {
            onUiThreadGet {
                val spinner = WSpinner(WSpinnerNumberModel(1.0, 0.0, 10.0, 1.0))
                val editor = WSpinner.NumberEditor(spinner, "0.00")
                spinner.editor = editor
                spinner.editor shouldBeSameInstanceAs editor
                editor.spinner shouldBeSameInstanceAs spinner
                editor.format.format(12.0)
            } shouldBe "12.00"
        }

        test("an invalid DecimalFormat pattern throws IllegalArgumentException (as in Swing)") {
            val spinner = onUiThreadGet { WSpinner(1.0) }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { WSpinner.NumberEditor(spinner, "0.00.0") } }
        }
    }
}
