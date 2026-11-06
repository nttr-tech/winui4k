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
 * Tests for the AbstractButton form of WToggleSwitch (text is an alias of header, ActionListeners only for user toggles and doClick,
 * and actionCommand).
 */
@Tags("E2E")
class WToggleSwitchButtonTest : FunSpec() {
    private fun loaded(create: () -> WToggleSwitch): WToggleSwitch {
        val toggleSwitch = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(toggleSwitch)
        return toggleSwitch
    }

    init {
        context("Defaults") {
            test("a newly created switch has text equal to header (an empty string), empty actionListeners, and actionCommand equal to text") {
                onUiThreadGet {
                    val toggleSwitch = WToggleSwitch()
                    listOf(toggleSwitch.text, toggleSwitch.actionListeners.size, toggleSwitch.actionCommand)
                } shouldContainExactly listOf("", 0, "")
            }
        }

        context("Reading and writing") {
            test("setting text also changes header, and setting header also changes text") {
                onUiThreadGet {
                    val toggleSwitch = WToggleSwitch("Wi-Fi")
                    val first = toggleSwitch.text
                    toggleSwitch.text = "Bluetooth"
                    val header = toggleSwitch.header
                    toggleSwitch.header = "Airplane mode"
                    listOf(first, header, toggleSwitch.text)
                } shouldContainExactly listOf("Wi-Fi", "Bluetooth", "Airplane mode")
            }

            test("actionCommand returns text until set, and setting null returns to text") {
                onUiThreadGet {
                    val toggleSwitch = WToggleSwitch("Wi-Fi")
                    val before = toggleSwitch.actionCommand
                    toggleSwitch.actionCommand = "wifi"
                    val set = toggleSwitch.actionCommand
                    toggleSwitch.actionCommand = null
                    listOf(before, set, toggleSwitch.actionCommand)
                } shouldContainExactly listOf("Wi-Fi", "wifi", "Wi-Fi")
            }
        }

        context("Events") {
            test("toggling with doClick calls the ActionListener once with the switch as the source and text as the actionCommand") {
                val toggleSwitch = loaded { WToggleSwitch("Wi-Fi") }
                try {
                    val events = mutableListOf<WActionEvent>()
                    onUiThread {
                        toggleSwitch.addActionListener { e -> events += e }
                        toggleSwitch.doClick()
                    }
                    onUiThreadGet { events.size to toggleSwitch.isOn } shouldBe (1 to true)
                    events[0].source shouldBeSameInstanceAs toggleSwitch
                    events[0].actionCommand shouldBe "Wi-Fi"
                } finally {
                    UiTestHarness.detach(toggleSwitch)
                }
            }

            test("changing isOn / isSelected from the program calls ItemListeners but not ActionListeners (the same as Swing's setSelected)") {
                val (actions, items) = onUiThreadGet {
                    val toggleSwitch = WToggleSwitch("Wi-Fi")
                    val actions = mutableListOf<String>()
                    val items = mutableListOf<Int>()
                    toggleSwitch.addActionListener(WActionListener { actions += "action" })
                    toggleSwitch.addItemListener(WItemListener { e -> items += e.stateChange })
                    toggleSwitch.isOn = true
                    toggleSwitch.isSelected = false
                    actions.toList() to items.toList()
                }
                actions.shouldBeEmpty()
                items shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            }

            test("with doClick, ItemListeners are called before ActionListeners (the same as Swing)") {
                val toggleSwitch = loaded { WToggleSwitch("Wi-Fi") }
                try {
                    val calls = mutableListOf<String>()
                    onUiThread {
                        toggleSwitch.addActionListener { calls += "action" }
                        toggleSwitch.addItemListener(WItemListener { calls += "item" })
                        toggleSwitch.doClick()
                    }
                    onUiThreadGet { calls.toList() } shouldContainExactly listOf("item", "action")
                } finally {
                    UiTestHarness.detach(toggleSwitch)
                }
            }

            test("listeners removed with removeActionListener are not called (both lambdas and WActionListeners)") {
                val toggleSwitch = loaded { WToggleSwitch("Wi-Fi") }
                try {
                    val calls = mutableListOf<String>()
                    onUiThread {
                        val lambda = { _: WActionEvent -> calls += "lambda" }
                        val listener = WActionListener { calls += "listener" }
                        toggleSwitch.addActionListener(lambda)
                        toggleSwitch.addActionListener(listener)
                        toggleSwitch.removeActionListener(lambda)
                        toggleSwitch.removeActionListener(listener)
                        toggleSwitch.doClick()
                    }
                    onUiThreadGet { calls.toList() to toggleSwitch.actionListeners.size } shouldBe (emptyList<String>() to 0)
                } finally {
                    UiTestHarness.detach(toggleSwitch)
                }
            }
        }

        context("Boundaries and exceptions") {
            test("doClick on a disabled switch does nothing") {
                val toggleSwitch = loaded { WToggleSwitch("Wi-Fi").apply { isEnabled = false } }
                try {
                    val calls = mutableListOf<String>()
                    onUiThread {
                        toggleSwitch.addActionListener { calls += "action" }
                        toggleSwitch.doClick()
                    }
                    onUiThreadGet { calls.toList() to toggleSwitch.isOn } shouldBe (emptyList<String>() to false)
                } finally {
                    UiTestHarness.detach(toggleSwitch)
                }
            }
        }

        context("Input") {
            test("pressing and releasing Space on a focused switch toggles it and calls the ActionListener once") {
                val toggleSwitch = loaded { WToggleSwitch("Wi-Fi") }
                try {
                    val calls = mutableListOf<Boolean>()
                    onUiThread {
                        toggleSwitch.addActionListener { calls += toggleSwitch.isOn }
                        check(toggleSwitch.requestFocusInWindow()) { "Cannot move the focus" }
                    }
                    TestKeyboard.press(VK_SPACE, SCAN_SPACE)
                    eventually(3.seconds) { onUiThreadGet { calls.toList() } shouldContainExactly listOf(true) }
                } finally {
                    UiTestHarness.detach(toggleSwitch)
                }
            }
        }
    }

    private companion object {
        const val VK_SPACE = 0x20
        const val SCAN_SPACE = 0x39
    }
}
