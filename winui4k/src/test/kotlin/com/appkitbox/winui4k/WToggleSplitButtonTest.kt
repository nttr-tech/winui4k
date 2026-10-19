package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.function.Consumer

/**
 * Tests for WToggleSplitButton's checked state, ItemListener, and doClick and ActionListener that behave as in JToggleButton.
 * ToggleSplitButton does not raise IsCheckedChanged for IsChecked changes before Loaded (it checks m_hasLoaded), so tests
 * that check events attach it to the shared window and wait for Loaded before changing the state.
 */
class WToggleSplitButtonTest : FunSpec() {
    init {
        test("isSelected reads and writes the same value as isChecked") {
            val (initial, afterSelected, afterUnchecked) = onUiThreadGet {
                val button = WToggleSplitButton("Bold")
                val initial = button.isSelected
                button.isSelected = true
                val afterSelected = button.isChecked to button.isSelected
                button.isChecked = false
                Triple(initial, afterSelected, button.isSelected)
            }
            initial shouldBe false
            afterSelected shouldBe (true to true)
            afterUnchecked shouldBe false
        }

        test("each isChecked change calls a Kotlin lambda-style ItemListener with the new value") {
            val received = mutableListOf<Boolean>()
            val button = onUiThreadGet { WToggleSplitButton().also { it.addItemListener { checked -> received.add(checked) } } }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    button.isChecked = true
                    button.isChecked = false
                }
            } finally {
                UiTestHarness.detach(button)
            }
            received shouldContainExactly listOf(true, false)
        }

        test("WItemListener receives an ItemEvent with SELECTED on check and DESELECTED on uncheck, and source is the button") {
            val received = mutableListOf<WItemEvent>()
            val button = onUiThreadGet {
                WToggleSplitButton("Bold").also { it.addItemListener(WItemListener { e -> received.add(e) }) }
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    button.isSelected = true
                    button.isSelected = false
                }
            } finally {
                UiTestHarness.detach(button)
            }
            received.map { it.stateChange } shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            received.all { it.source === button && it.itemSelectable === button && it.item === button }.shouldBeTrue()
        }

        test("even if the first listener changes the checked state, later listeners of the same notification receive the state at the time of the notification (same order as Swing)") {
            val first = mutableListOf<Int>()
            val second = mutableListOf<Int>()
            val button = onUiThreadGet {
                val button = WToggleSplitButton("Bold")
                var flipped = false
                button.addItemListener(
                    WItemListener { e ->
                        first += e.stateChange
                        if (!flipped) {
                            flipped = true
                            button.isChecked = false // The nested notification is delivered first, within this notification
                        }
                    },
                )
                button.addItemListener(WItemListener { e -> second += e.stateChange })
                button
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { button.isChecked = true }
            } finally {
                UiTestHarness.detach(button)
            }
            first shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            second shouldContainExactly listOf(WItemEvent.DESELECTED, WItemEvent.SELECTED)
        }

        test("a listener removed with removeItemListener is no longer called (both the lambda style and WItemListener)") {
            val lambdaStates = mutableListOf<Boolean>()
            val eventStates = mutableListOf<Int>()
            val button = onUiThreadGet { WToggleSplitButton() }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    val lambda: (Boolean) -> Unit = { lambdaStates.add(it) }
                    val listener = WItemListener { eventStates.add(it.stateChange) }
                    button.addItemListener(lambda)
                    button.addItemListener(listener)
                    button.isChecked = true
                    button.removeItemListener(lambda)
                    button.removeItemListener(listener)
                    button.isChecked = false
                }
            } finally {
                UiTestHarness.detach(button)
            }
            lambdaStates shouldContainExactly listOf(true)
            eventStates shouldContainExactly listOf(WItemEvent.SELECTED)
        }

        test("doClick toggles the checked state and calls the ActionListener once, and doing it again restores it and calls it once more") {
            val (states, clicks) = shown({ WToggleSplitButton("Bold") }) { button ->
                var clicks = 0
                button.addActionListener { clicks++ }
                val states = mutableListOf(button.isSelected)
                button.doClick()
                states += button.isSelected
                val afterFirst = clicks
                button.doClick()
                states += button.isSelected
                states to (afterFirst to clicks)
            }
            states shouldContainExactly listOf(false, true, false)
            clicks shouldBe (1 to 2)
        }

        test("doClick before being shown in a window does nothing, changing neither the checked state nor the ActionListener") {
            val (selected, clicks) = onUiThreadGet {
                val button = WToggleSplitButton("Bold")
                var clicks = 0
                button.addActionListener { clicks++ }
                button.doClick()
                button.isSelected to clicks
            }
            selected shouldBe false
            clicks shouldBe 0
        }

        test("doClick delivers SELECTED to the ItemListener and once to the ActionListener, in this order, and DESELECTED the second time") {
            val order = mutableListOf<String>()
            val button = onUiThreadGet {
                WToggleSplitButton("Bold").also {
                    it.addItemListener(WItemListener { e -> order += "item:${e.stateChange}" })
                    it.addActionListener { e -> order += "action:${e.actionCommand}" }
                }
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    button.doClick()
                    button.doClick()
                }
            } finally {
                UiTestHarness.detach(button)
            }
            // Swing's doClick fires the ActionEvent after the ItemEvent. ToggleSplitButton also raises Click after Toggle
            order shouldContainExactly listOf(
                "item:${WItemEvent.SELECTED}",
                "action:Bold",
                "item:${WItemEvent.DESELECTED}",
                "action:Bold",
            )
        }

        test("doClick on a disabled WToggleSplitButton neither changes the checked state nor calls the ActionListener") {
            val (selected, clicks) = shown({ WToggleSplitButton("Bold") }) { button ->
                var clicks = 0
                button.addActionListener { clicks++ }
                button.isEnabled = false
                button.doClick()
                button.isSelected to clicks
            }
            selected shouldBe false
            clicks shouldBe 0
        }

        test("WToggleSplitButton's doClick executes command with commandParameter and also toggles the checked state") {
            val (received, selected) = shown({ WToggleSplitButton("Bold") }) { button ->
                val received = mutableListOf<String?>()
                button.command = WCommand(true, Consumer { received += it })
                button.commandParameter = "Argument"
                button.doClick()
                received to button.isSelected
            }
            received shouldContainExactly listOf("Argument")
            selected shouldBe true
        }

        test("WToggleSplitButton supports the same content, icon, actionCommand and mnemonic as WSplitButton") {
            val (content, command, mnemonic) = onUiThreadGet {
                val button = WToggleSplitButton("Bold")
                button.icon = Symbol.BOLD
                button.actionCommand = "bold"
                button.setMnemonic('b')
                Triple(ButtonContents.contentOf(button), button.actionCommand, button.mnemonic)
            }
            content shouldBe listOf("Panel:HORIZONTAL:4.0", "Symbol:BOLD", "TextBlock:Bold")
            command shouldBe "bold"
            mnemonic shouldBe 66
        }
    }

    /** Shows the button created by [create] (run on the UI thread) in a window (waiting for Loaded), runs [act] on the UI thread, returns its result, and removes it. */
    private fun <T> shown(create: () -> WToggleSplitButton, act: (WToggleSplitButton) -> T): T {
        val button = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(button)
        try {
            return onUiThreadGet { act(button) }
        } finally {
            UiTestHarness.detach(button)
        }
    }
}
