package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for WAppBarToggleButton's selection state (isSelected / isChecked), doClick, ItemListener, and label position and
 * compact display. The aliases of label, icon and text are in WAppBarButtonTest, so they are not repeated here.
 * The Java-facing forms are WAppBarButtonForJavaTest (icon and label) and WAppBarToggleButtonForJavaTest (selection state).
 */
class WAppBarToggleButtonTest : FunSpec() {
    init {
        test("the constructor's label and icon can be read back, and the selection state is false when unset") {
            val (label, icon, selected) = onUiThreadGet {
                val button = WAppBarToggleButton("Bold", Symbol.BOLD)
                Triple(button.label, button.icon, button.isSelected)
            }
            label shouldBe "Bold"
            icon shouldBe Symbol.BOLD
            selected shouldBe false
        }

        test("isSelected reads and writes the same state as isChecked") {
            val (afterSelected, afterUnchecked) = onUiThreadGet {
                val button = WAppBarToggleButton("Bold")
                button.isSelected = true
                val afterSelected = button.isChecked to button.isSelected
                button.isChecked = false
                afterSelected to button.isSelected
            }
            afterSelected shouldBe (true to true)
            afterUnchecked shouldBe false
        }

        test("doClick toggles the selection state and calls the ActionListener once per call") {
            val (states, clicks) = onUiThreadGet {
                val button = WAppBarToggleButton("Bold")
                var clicks = 0
                button.addActionListener { clicks++ }
                val states = mutableListOf(button.isSelected)
                button.doClick()
                states += button.isSelected
                button.doClick()
                states += button.isSelected
                states to clicks
            }
            states shouldContainExactly listOf(false, true, false)
            clicks shouldBe 2
        }

        test("doClick while placed in a window delivers ItemEvents of SELECTED and DESELECTED to the ItemListener in order") {
            val received = mutableListOf<WItemEvent>()
            val button = onUiThreadGet {
                WAppBarToggleButton("Bold").also { it.addItemListener(WItemListener { e -> received += e }) }
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
            received.map { it.stateChange } shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            received.all { it.source === button } shouldBe true
        }

        test("doClick on a disabled WAppBarToggleButton does not change the selection state") {
            onUiThreadGet {
                val button = WAppBarToggleButton("Bold")
                button.isEnabled = false
                button.doClick()
                button.isSelected
            } shouldBe false
        }

        test("labelPosition is DEFAULT by default and reads back the value set, and isCompact can also be read and written") {
            val (initial, collapsed, compact) = onUiThreadGet {
                val button = WAppBarToggleButton("Bold")
                val initial = button.labelPosition to button.isCompact
                button.labelPosition = CommandBarLabelPosition.COLLAPSED
                button.isCompact = true
                Triple(initial, button.labelPosition, button.isCompact)
            }
            initial shouldBe (CommandBarLabelPosition.DEFAULT to false)
            collapsed shouldBe CommandBarLabelPosition.COLLAPSED
            compact shouldBe true
        }

        test("the action's SELECTED_KEY is written back to the action when doClick toggles the selection") {
            val action = object : WAbstractAction("Italic") {
                override fun actionPerformed(e: WActionEvent) = Unit
            }
            action.putValue(WAction.SELECTED_KEY, false)
            val (selected, key) = onUiThreadGet {
                val button = WAppBarToggleButton()
                button.action = action
                button.doClick()
                button.isSelected to action.getValue(WAction.SELECTED_KEY)
            }
            selected shouldBe true
            key shouldBe true
        }
    }
}
