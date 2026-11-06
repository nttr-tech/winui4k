package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WButtonBase.addChangeListener (like AbstractButton.addChangeListener: changes of ButtonModel's pressed, pointer, selected and enabled).
 */
@Tags("E2E")
class WButtonChangeListenerTest : FunSpec() {
    /** The sources of the events received by the listener attached to [button]. */
    private fun record(button: WButtonBase): MutableList<Any> {
        val sources = mutableListOf<Any>()
        button.addChangeListener(WChangeListener { e -> sources += e.source })
        return sources
    }

    init {
        context("Defaults") {
            test("getChangeListeners of a newly created button is empty") {
                onUiThreadGet { listOf(WButton(), WToggleButton(), WCheckBox(), WRadioButton()).map { it.getChangeListeners().size } } shouldContainExactly
                    listOf(0, 0, 0, 0)
            }
        }

        context("Events") {
            test("setting isEnabled to false delivers one change whose source is that button, and setting the same value delivers nothing") {
                val (button, sources) = onUiThreadGet {
                    val button = WButton("Save")
                    val sources = record(button)
                    button.isEnabled = false
                    button.isEnabled = false
                    button to sources.toList()
                }
                sources.size shouldBe 1
                sources[0] shouldBeSameInstanceAs button
            }

            test("changing the toggle's isSelected to true and then false delivers two changes, and setting the same value delivers nothing") {
                onUiThreadGet {
                    val toggle = WToggleButton("Bold")
                    val sources = record(toggle)
                    toggle.isSelected = true
                    toggle.isSelected = true
                    toggle.isSelected = false
                    sources.size
                } shouldBe 2
            }

            test("a change is also delivered when a three-state check box becomes indeterminate") {
                onUiThreadGet {
                    val box = WCheckBox("All").apply { isThreeState = true }
                    val sources = record(box)
                    box.isChecked = null
                    sources.size
                } shouldBe 1
            }

            test("doClick does not create a pressed state (UI Automation Invoke), so no pressed change is delivered") {
                val button = onUiThreadGet { WButton("Save") }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    val pressed = mutableListOf<Boolean>()
                    onUiThread {
                        // Pointer changes (depending on the real mouse position) may arrive, so look only at the pressed state when they do
                        button.addChangeListener(WChangeListener { pressed += button.isPressed })
                        button.doClick()
                    }
                    onUiThreadGet { pressed.filter { it } }.shouldBeEmpty()
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("a listener removed with removeChangeListener is not called and disappears from getChangeListeners") {
                val (calls, remaining) = onUiThreadGet {
                    val button = WButton("Save")
                    val calls = mutableListOf<String>()
                    val first = WChangeListener { calls += "first" }
                    val second = WChangeListener { calls += "second" }
                    button.addChangeListener(first)
                    button.addChangeListener(second)
                    button.removeChangeListener(first)
                    button.isEnabled = false
                    calls.toList() to button.getChangeListeners().toList()
                }
                calls shouldContainExactly listOf("second")
                remaining.size shouldBe 1
            }

            test("removing a listener that is not attached does nothing") {
                onUiThreadGet {
                    val button = WButton()
                    button.removeChangeListener(WChangeListener { })
                    button.getChangeListeners().toList()
                }.shouldBeEmpty()
            }
        }

        context("Input") {
            test("pressing Space on a focused button delivers a pressed (isPressed = true) change, and releasing it delivers the release change") {
                val button = onUiThreadGet { WButton("Save") }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    val pressedStates = mutableListOf<Boolean>()
                    onUiThread {
                        var last = false
                        // Exclude pointer changes (depending on the real mouse position) and record only pressed changes
                        button.addChangeListener(
                            WChangeListener {
                                if (button.isPressed != last) {
                                    last = button.isPressed
                                    pressedStates += last
                                }
                            },
                        )
                        check(button.requestFocusInWindow()) { "Cannot move the focus" }
                    }
                    TestKeyboard.keyDown(VK_SPACE, SCAN_SPACE)
                    eventually(3.seconds) { onUiThreadGet { pressedStates.toList() } shouldContainExactly listOf(true) }
                    TestKeyboard.keyUp(VK_SPACE, SCAN_SPACE)
                    eventually(3.seconds) { onUiThreadGet { pressedStates.toList() } shouldContainExactly listOf(true, false) }
                } finally {
                    UiTestHarness.detach(button)
                }
            }
        }
    }

    private companion object {
        const val VK_SPACE = 0x20
        const val SCAN_SPACE = 0x39
    }
}
