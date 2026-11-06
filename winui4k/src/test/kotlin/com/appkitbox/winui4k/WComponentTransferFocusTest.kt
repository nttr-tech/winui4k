package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WComponent.transferFocus] / [WComponent.transferFocusBackward], which correspond to Component.transferFocus / transferFocusBackward.
 * Expected values come from Swing's rules (move to the next / previous focusable component after this one, in the same order as
 * the Tab / Shift+Tab keys).
 */
@Tags("Integration")
class WComponentTransferFocusTest : FunSpec() {
    /** Places a panel with three buttons stacked vertically in a window and removes it after [check]. */
    private fun withThreeButtons(check: (WButton, WButton, WButton) -> Unit) {
        val buttons = onUiThreadGet { listOf(WButton("One"), WButton("Two"), WButton("Three")) }
        val panel = onUiThreadGet { WPanel().also { panel -> buttons.forEach { panel.add(it) } } }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            check(buttons[0], buttons[1], buttons[2])
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        context("Reading and writing") {
            test("transferFocus on the focused first button moves the focus to the second button") {
                withThreeButtons { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.transferFocus()
                        first.hasFocus() to second.hasFocus()
                    } shouldBe (false to true)
                }
            }

            test("transferFocusBackward on the focused second button moves the focus back to the first button") {
                withThreeButtons { first, second, _ ->
                    onUiThreadGet {
                        second.requestFocusInWindow()
                        second.transferFocusBackward()
                        first.hasFocus() to second.hasFocus()
                    } shouldBe (true to false)
                }
            }
        }

        context("Swing compatibility") {
            test("transferFocus on the first button without the focus also moves to the second button after it (not the one after the current focus)") {
                withThreeButtons { first, second, third ->
                    onUiThreadGet {
                        third.requestFocusInWindow()
                        first.transferFocus()
                        listOf(first.hasFocus(), second.hasFocus(), third.hasFocus())
                    } shouldBe listOf(false, true, false)
                }
            }

            test("if the next button cannot receive the focus (isFocusable = false), it is skipped and the focus moves to the one after it") {
                withThreeButtons { first, second, third ->
                    onUiThreadGet {
                        second.isFocusable = false
                        first.requestFocusInWindow()
                        first.transferFocus()
                        second.hasFocus() to third.hasFocus()
                    } shouldBe (false to true)
                }
            }
        }

        context("Boundaries and exceptions") {
            test("transferFocus and transferFocusBackward on a button not placed in a window do nothing (no exception)") {
                onUiThreadGet {
                    val button = WButton("Not placed")
                    button.transferFocus()
                    button.transferFocusBackward()
                    button.hasFocus()
                } shouldBe false
            }
        }
    }
}
