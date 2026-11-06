package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.text.ParseException

/**
 * Tests for WSpinner.commitEdit (like JSpinner.commitEdit). The text being typed is made by putting it directly into Text of the template's input field (InputBox)
 * (NumberBox does not read it until the focus leaves or Enter is pressed, so just putting it in does not commit it).
 */
@Tags("E2E")
class WSpinnerCommitEditTest : FunSpec() {
    private fun typeInto(spinner: WSpinner, text: String) {
        val input = checkNotNull(Xaml.appliedTemplatePartOrNull(spinner.inspectable, "InputBox"))
        val textBox = input.queryInterface(XamlInterop.IID_ITextBox)
        input.release()
        try {
            Hstring.use(text) { h -> textBox.call(XamlInterop.ITextBox_put_Text, h) }
        } finally {
            textBox.release()
        }
    }

    private fun typed(spinner: WSpinner): String {
        val input = checkNotNull(Xaml.appliedTemplatePartOrNull(spinner.inspectable, "InputBox"))
        val textBox = input.queryInterface(XamlInterop.IID_ITextBox)
        input.release()
        return try {
            textBox.getString(XamlInterop.ITextBox_get_Text)
        } finally {
            textBox.release()
        }
    }

    private suspend fun withLoaded(create: () -> WSpinner, block: suspend (WSpinner) -> Unit) {
        val spinner = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(spinner)
        try {
            block(spinner)
        } finally {
            UiTestHarness.detach(spinner)
        }
    }

    init {
        context("Reading and writing") {
            test("typed digits become the model's value with commitEdit, and the ChangeListener is called once") {
                withLoaded({ WSpinner(WSpinnerNumberModel(1, 0, 100, 1)) }) { spinner ->
                    onUiThreadGet {
                        val changes = mutableListOf<Any?>()
                        spinner.addChangeListener(WChangeListener { changes += spinner.model.value })
                        typeInto(spinner, "42")
                        val before = spinner.model.value
                        spinner.commitEdit()
                        listOf(before, spinner.model.value, changes.toList())
                    } shouldContainExactly listOf(1, 42, listOf(42))
                }
            }

            test("if the typed text is the same as the current value, commitEdit does not call the ChangeListener") {
                withLoaded({ WSpinner(WSpinnerNumberModel(5, 0, 10, 1)) }) { spinner ->
                    onUiThreadGet {
                        val changes = mutableListOf<Any?>()
                        spinner.addChangeListener(WChangeListener { changes += spinner.model.value })
                        typeInto(spinner, "5")
                        spinner.commitEdit()
                        spinner.model.value to changes.toList()
                    } shouldBe (5 to emptyList<Any?>())
                }
            }

            test("digits outside the range are clamped to the edge of the range and committed (NumberBox validation)") {
                withLoaded({ WSpinner(WSpinnerNumberModel(5, 0, 10, 1)) }) { spinner ->
                    onUiThreadGet {
                        typeInto(spinner, "99")
                        spinner.commitEdit()
                        spinner.model.value
                    } shouldBe 10
                }
            }

            test("a spinner with acceptsExpression evaluates the expression and commits it") {
                withLoaded({ WSpinner(2.0).apply { acceptsExpression = true } }) { spinner ->
                    onUiThreadGet {
                        typeInto(spinner, "(5 + 3) * 2")
                        spinner.commitEdit()
                        spinner.value
                    } shouldBe 16.0
                }
            }

            test("clearing the input on a spinner with the default model and calling commitEdit makes the value NaN (the existing handling of an empty field)") {
                withLoaded({ WSpinner(3.0) }) { spinner ->
                    onUiThreadGet {
                        typeInto(spinner, "")
                        spinner.commitEdit()
                        spinner.value.isNaN()
                    } shouldBe true
                }
            }
        }

        context("Boundaries and exceptions") {
            test("text that cannot be read as a number throws ParseException, the value does not change, and the typed text stays in the input field") {
                withLoaded({ WSpinner(WSpinnerNumberModel(7, 0, 10, 1)) }) { spinner ->
                    val changes = mutableListOf<Any?>()
                    onUiThread {
                        spinner.addChangeListener(WChangeListener { changes += spinner.model.value })
                        typeInto(spinner, "abc")
                        shouldThrow<ParseException> { spinner.commitEdit() }
                    }
                    onUiThreadGet { spinner.model.value to typed(spinner) } shouldBe (7 to "abc")
                    changes.shouldBeEmpty()
                }
            }

            test("clearing the input on a spinner given a model and calling commitEdit throws ParseException, and the value stays the previous one") {
                withLoaded({ WSpinner(WSpinnerNumberModel(4, 0, 10, 1)) }) { spinner ->
                    onUiThread {
                        typeInto(spinner, "")
                        shouldThrow<ParseException> { spinner.commitEdit() }
                    }
                    onUiThreadGet { spinner.model.value } shouldBe 4
                }
            }

            test("commitEdit before being placed in a window (no input field yet) does nothing") {
                onUiThreadGet {
                    val spinner = WSpinner(WSpinnerNumberModel(3, 0, 10, 1))
                    spinner.commitEdit()
                    spinner.model.value
                } shouldBe 3
            }
        }
    }
}
