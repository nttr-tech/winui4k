package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify [WComponent.inputVerifier], which corresponds to JComponent.setInputVerifier. The expected values come from Swing's rules (if the check does not pass,
 * focus stays on the original component; if verifyInputWhenFocusTarget of the destination is false, no check is done; focus also stays if verifyTarget of the
 * destination's verifier is false).
 */
@Tags("E2E")
class WInputVerifierTest : FunSpec() {
    /** A verifier that accepts only digits. Remembers how many times it was called and the destination passed to shouldYieldFocus. */
    private class DigitsVerifier : WInputVerifier() {
        var verifyCount = 0
        val targets = ArrayList<WComponent>()

        override fun verify(input: WComponent): Boolean {
            verifyCount++
            return (input as WTextField).text.all { it.isDigit() }
        }

        override fun shouldYieldFocus(source: WComponent, target: WComponent): Boolean {
            targets += target
            return super.shouldYieldFocus(source, target)
        }
    }

    /** Puts a panel with two text fields and one button stacked vertically on the window, and removes it after [check]. */
    private fun withFields(check: (WTextField, WTextField, WButton) -> Unit) {
        val parts = onUiThreadGet { Triple(WTextField(), WTextField(), WButton("Cancel")) }
        val panel = onUiThreadGet {
            WPanel().also {
                it.add(parts.first)
                it.add(parts.second)
                it.add(parts.third)
            }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            check(parts.first, parts.second, parts.third)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        context("read and write") {
            test("inputVerifier defaults to null and verifyInputWhenFocusTarget defaults to true (as in Swing)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.inputVerifier to field.verifyInputWhenFocusTarget
                } shouldBe (null to true)
            }

            test("the set verifier can be read with inputVerifier") {
                onUiThreadGet {
                    val field = WTextField()
                    val verifier = DigitsVerifier()
                    field.inputVerifier = verifier
                    field.inputVerifier === verifier
                } shouldBe true
            }
        }

        context("focus moves") {
            test("if the input is not valid, focus stays even when requestFocusInWindow tries to move to another text field") {
                withFields { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "12a"
                        first.inputVerifier = DigitsVerifier()
                        val moved = second.requestFocusInWindow()
                        listOf(moved, first.hasFocus(), second.hasFocus())
                    } shouldBe listOf(false, true, false)
                }
            }

            test("if the input is valid, focus moves to another text field") {
                withFields { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "123"
                        val verifier = DigitsVerifier()
                        first.inputVerifier = verifier
                        second.requestFocusInWindow()
                        listOf(first.hasFocus(), second.hasFocus(), verifier.verifyCount == 1, verifier.targets == listOf(second))
                    } shouldBe listOf(false, true, true, true)
                }
            }

            test("with transferFocus (the same move as the Tab key) too, focus stays if the input is not valid") {
                withFields { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "x"
                        first.inputVerifier = DigitsVerifier()
                        first.transferFocus()
                        first.hasFocus() to second.hasFocus()
                    } shouldBe (true to false)
                }
            }

            test("once fixed and valid, focus moves on the next move") {
                withFields { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "x"
                        first.inputVerifier = DigitsVerifier()
                        val before = second.requestFocusInWindow()
                        first.text = "42"
                        val after = second.requestFocusInWindow()
                        listOf(before, after, second.hasFocus())
                    } shouldBe listOf(false, true, true)
                }
            }

            test("if verifyInputWhenFocusTarget of the destination is false, focus moves without a check (a cancel button)") {
                withFields { first, _, cancel ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "x"
                        val verifier = DigitsVerifier()
                        first.inputVerifier = verifier
                        cancel.verifyInputWhenFocusTarget = false
                        cancel.requestFocusInWindow()
                        listOf(cancel.hasFocus(), first.hasFocus(), verifier.verifyCount)
                    } shouldBe listOf(true, false, 0)
                }
            }

            test("if verifyTarget of the destination's verifier is false, focus stays even if the input of the component it moves from is valid") {
                withFields { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "1"
                        first.inputVerifier = DigitsVerifier()
                        second.inputVerifier = object : WInputVerifier() {
                            override fun verify(input: WComponent): Boolean = true

                            override fun verifyTarget(input: WComponent): Boolean = false
                        }
                        second.requestFocusInWindow()
                        first.hasFocus() to second.hasFocus()
                    } shouldBe (true to false)
                }
            }

            test("setting inputVerifier back to null lets focus move even if the input is not valid") {
                withFields { first, second, _ ->
                    onUiThreadGet {
                        first.requestFocusInWindow()
                        first.text = "x"
                        first.inputVerifier = DigitsVerifier()
                        first.inputVerifier = null
                        second.requestFocusInWindow()
                        second.hasFocus()
                    } shouldBe true
                }
            }

            test("when the component with the verifier does not have focus, it is not called for moves between other components") {
                withFields { first, second, cancel ->
                    onUiThreadGet {
                        first.text = "x"
                        val verifier = DigitsVerifier()
                        first.inputVerifier = verifier
                        second.requestFocusInWindow()
                        cancel.requestFocusInWindow()
                        cancel.hasFocus() to verifier.verifyCount
                    } shouldBe (true to 0)
                }
            }
        }
    }
}
