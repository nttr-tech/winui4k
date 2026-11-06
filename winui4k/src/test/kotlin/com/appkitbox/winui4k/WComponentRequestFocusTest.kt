package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WComponent.isRequestFocusEnabled] / [WComponent.grabFocus] / [WComponent.isFocusOwner], corresponding to
 * JComponent.isRequestFocusEnabled / setRequestFocusEnabled, grabFocus and Component.isFocusOwner.
 * Expected values are taken from Swing's rules (requestFocusEnabled only decides whether it takes the focus when pressed with the mouse,
 * and does not stop requestFocus and grabFocus; isFocusOwner is the same as hasFocus).
 */
@Tags("E2E")
class WComponentRequestFocusTest : FunSpec() {
    /** Puts two buttons made by [create] in a panel, puts it in a window, and removes it after [check]. */
    private fun withTwoButtons(check: (WButton, WButton) -> Unit) {
        val (panel, first, second) = onUiThreadGet {
            val a = WButton("One")
            val b = WButton("Two")
            Triple(
                WPanel().also {
                    it.add(a)
                    it.add(b)
                },
                a,
                b,
            )
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            check(first, second)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        context("Defaults") {
            test("isRequestFocusEnabled defaults to true, and the native AllowFocusOnInteraction is also true") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.isRequestFocusEnabled to
                        button.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_AllowFocusOnInteraction)
                } shouldBe (true to true)
            }

            test("isFocusOwner of a button not in a window is false") {
                onUiThreadGet { WButton("Press").isFocusOwner } shouldBe false
            }
        }

        context("Reading and writing") {
            test("setting isRequestFocusEnabled to false makes AllowFocusOnInteraction false, and setting it back to true makes it true") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.isRequestFocusEnabled = false
                    val off = button.isRequestFocusEnabled to
                        button.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_AllowFocusOnInteraction)
                    button.isRequestFocusEnabled = true
                    val on = button.isRequestFocusEnabled to
                        button.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_AllowFocusOnInteraction)
                    off to on
                } shouldBe ((false to false) to (true to true))
            }
        }

        context("Swing compatibility") {
            test("even if isRequestFocusEnabled is false, requestFocusInWindow moves the focus (as in Swing, only mouse presses are stopped)") {
                withTwoButtons { first, _ ->
                    onUiThreadGet {
                        first.isRequestFocusEnabled = false
                        first.requestFocusInWindow() to first.hasFocus()
                    } shouldBe (true to true)
                }
            }

            test("grabFocus moves the focus like requestFocus, even if isRequestFocusEnabled is false") {
                withTwoButtons { first, second ->
                    onUiThreadGet {
                        second.isRequestFocusEnabled = false
                        first.grabFocus()
                        val afterFirst = first.hasFocus()
                        second.grabFocus()
                        Triple(afterFirst, second.hasFocus(), first.hasFocus())
                    } shouldBe Triple(true, true, false)
                }
            }

            test("isFocusOwner is the same as hasFocus, and becomes false when the focus moves to another button") {
                withTwoButtons { first, second ->
                    onUiThreadGet {
                        first.requestFocus()
                        val before = first.isFocusOwner to second.isFocusOwner
                        second.requestFocus()
                        val after = first.isFocusOwner to second.isFocusOwner
                        Triple(before, after, second.isFocusOwner == second.hasFocus())
                    } shouldBe Triple(true to false, false to true, true)
                }
            }
        }

        context("Boundaries and exceptions") {
            test("a button with isFocusable = false does not receive the focus even with grabFocus (the same as [WComponent.requestFocus])") {
                withTwoButtons { first, _ ->
                    onUiThreadGet {
                        first.isFocusable = false
                        first.grabFocus()
                        first.isFocusOwner
                    } shouldBe false
                }
            }
        }
    }
}
