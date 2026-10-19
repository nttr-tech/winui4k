package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WComponent.requestFocusInWindow] / [WComponent.hasFocus] / [WComponent.isFocusable].
 * NumberBox, AutoSuggestBox, DatePicker and TimePicker have IsTabStop false on themselves, but UIElement.Focus succeeds
 * by passing focus to a part inside the template, so requestFocusInWindow returns true.
 * Only components with isFocusable = false set explicitly return false, as ones that "do not accept focus".
 */
class WComponentFocusableTest : FunSpec() {
    /** isFocusable, the result of requestFocusInWindow, and hasFocus. */
    private data class Focus(val isFocusable: Boolean, val accepted: Boolean, val hasFocus: Boolean)

    private fun focusOf(component: WComponent) = onUiThreadGet {
        val accepted = component.requestFocusInWindow()
        Focus(component.isFocusable, accepted, component.hasFocus())
    }

    private fun <T : WComponent> attached(create: () -> T, check: (T) -> Unit) {
        val component = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(component)
        try {
            check(component)
        } finally {
            UiTestHarness.detach(component)
        }
    }

    init {
        context("controls whose template parts accept focus return true from requestFocusInWindow even if their own IsTabStop is false") {
            test("WSpinner (NumberBox)") {
                attached({ WSpinner(1.0) }) { focusOf(it) shouldBe Focus(isFocusable = true, accepted = true, hasFocus = true) }
            }
            test("WAutoSuggestBox") {
                attached({ WAutoSuggestBox("Find") }) { focusOf(it) shouldBe Focus(isFocusable = true, accepted = true, hasFocus = true) }
            }
            test("WDatePicker") {
                attached({ WDatePicker() }) { focusOf(it) shouldBe Focus(isFocusable = true, accepted = true, hasFocus = true) }
            }
            test("WTimePicker") {
                attached({ WTimePicker() }) { focusOf(it) shouldBe Focus(isFocusable = true, accepted = true, hasFocus = true) }
            }
        }

        test("a WSpinner with isFocusable = false does not accept focus, and accepts it again when set back to true (NumberBox's own IsTabStop stays false)") {
            attached({ WSpinner(1.0).also { it.isFocusable = false } }) { spinner ->
                focusOf(spinner) shouldBe Focus(isFocusable = false, accepted = false, hasFocus = false)
                onUiThreadGet { spinner.isFocusable = true }
                focusOf(spinner) shouldBe Focus(isFocusable = true, accepted = true, hasFocus = true)
                onUiThreadGet { spinner.uiElement.getBool(XamlInterop.IUIElement_get_IsTabStop) } shouldBe false
            }
        }

        test("a WAutoSuggestBox with isFocusable = false does not accept focus") {
            attached({ WAutoSuggestBox("Find").also { it.isFocusable = false } }) {
                focusOf(it) shouldBe Focus(isFocusable = false, accepted = false, hasFocus = false)
            }
        }

        test("a WButton with isFocusable = false does not accept focus, and accepts it again when set back to true") {
            attached({ WButton("OK").also { it.isFocusable = false } }) { button ->
                focusOf(button) shouldBe Focus(isFocusable = false, accepted = false, hasFocus = false)
                onUiThreadGet { button.isFocusable = true }
                focusOf(button) shouldBe Focus(isFocusable = true, accepted = true, hasFocus = true)
            }
        }

        test("requestFocusInWindow returns false for a WSpinner not placed in a window") {
            val spinner = onUiThreadGet { WSpinner(1.0) }
            focusOf(spinner) shouldBe Focus(isFocusable = true, accepted = false, hasFocus = false)
        }

        test("requestFocusInWindow returns false for an element that does not accept focus (WLabel)") {
            attached({ WLabel("Text") }) { label ->
                onUiThreadGet { label.requestFocusInWindow() } shouldBe false
                onUiThreadGet { label.hasFocus() } shouldBe false
            }
        }
    }
}
