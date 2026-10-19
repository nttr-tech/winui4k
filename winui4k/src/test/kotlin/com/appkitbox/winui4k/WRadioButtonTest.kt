package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests verifying WRadioButton's group name, checked state, and listeners. */
class WRadioButtonTest : FunSpec() {
    init {
        test("groupName returns exactly the value that was set") {
            onUiThreadGet {
                val radio = WRadioButton("Option A")
                radio.groupName = "settings"
                radio.groupName
            } shouldBe "settings"
        }

        test("setting isChecked = true reaches the ItemListener") {
            val received = mutableListOf<Boolean?>()
            onUiThread {
                val radio = WRadioButton("Option B")
                radio.addItemListener { received.add(it) }
                radio.isChecked = true
            }
            received shouldContainExactly listOf<Boolean?>(true)
        }

        test("radio buttons sharing a group added to the same parent select exclusively") {
            val (firstChecked, secondChecked) = onUiThreadGet {
                val panel = WPanel()
                val first = WRadioButton("A").apply { groupName = "exclusive" }
                val second = WRadioButton("B").apply { groupName = "exclusive" }
                panel.add(first)
                panel.add(second)
                first.isChecked = true
                second.isChecked = true // selecting this one automatically unchecks first
                (first.isChecked to second.isChecked)
            }
            firstChecked shouldBe false
            secondChecked shouldBe true
        }

        test("doClick selects the radio button, deselects the other radio buttons with the same parent, and calls the ActionListener") {
            val actions = mutableListOf<String>()
            val states = onUiThreadGet {
                val panel = WPanel()
                val first = WRadioButton("A")
                val second = WRadioButton("B")
                panel.add(first)
                panel.add(second)
                second.addActionListener { actions.add(it.actionCommand ?: "") }
                first.doClick()
                val afterFirst = first.isSelected to second.isSelected
                second.doClick()
                listOf(afterFirst, first.isSelected to second.isSelected)
            }
            states shouldContainExactly listOf(true to false, false to true)
            actions shouldContainExactly listOf("B")
        }

        test("doClick on a selected radio button keeps it selected and calls the ActionListener, but not the ItemListener") {
            val actions = mutableListOf<String>()
            val items = mutableListOf<Boolean?>()
            val selected = onUiThreadGet {
                val panel = WPanel()
                val radio = WRadioButton("A")
                panel.add(radio)
                radio.isSelected = true
                radio.addActionListener { actions.add("action") }
                radio.addItemListener { items.add(it) }
                radio.doClick()
                radio.isSelected
            }
            selected shouldBe true
            actions shouldContainExactly listOf("action")
            items shouldBe emptyList() // The state does not change, so no ItemEvent is fired (same as Swing's JRadioButton)
        }

        test("doClick on a disabled radio button does nothing") {
            val (selected, actionCount) = onUiThreadGet {
                val panel = WPanel()
                val radio = WRadioButton("A")
                panel.add(radio)
                var actionCount = 0
                radio.addActionListener { actionCount++ }
                radio.isEnabled = false
                radio.doClick()
                radio.isSelected to actionCount
            }
            selected shouldBe false
            actionCount shouldBe 0
        }

        test("selecting a radio button with the same parent delivers an ItemEvent of SELECTED to the selected one and DESELECTED to the deselected one") {
            val firstStates = mutableListOf<Int>()
            val secondStates = mutableListOf<Int>()
            onUiThread {
                val panel = WPanel()
                val first = WRadioButton("A")
                val second = WRadioButton("B")
                panel.add(first)
                panel.add(second)
                first.addItemListener(WItemListener { firstStates.add(it.stateChange) })
                second.addItemListener(WItemListener { secondStates.add(it.stateChange) })
                first.isSelected = true
                second.isSelected = true
            }
            firstStates shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            secondStates shouldContainExactly listOf(WItemEvent.SELECTED)
        }

        test("for radio buttons with the same parent placed in a window, WinUI sends Unchecked of the deselected one before Checked of the selected one") {
            // RadioButton::OnChecked fires Checked after UpdateRadioButtonGroup (IsChecked = false on the other buttons)
            val events = mutableListOf<String>()
            val (panel, radios) = onUiThreadGet {
                val panel = WPanel()
                val radios = listOf("A", "B").map { text ->
                    WRadioButton(text).also {
                        panel.add(it)
                        it.addItemListener(WItemListener { e -> events.add(text + ":" + e.stateChange + ":" + it.isSelected) })
                    }
                }
                panel to radios
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread {
                    radios[0].doClick()
                    radios[1].doClick()
                }
            } finally {
                UiTestHarness.detach(panel)
            }
            events shouldContainExactly listOf(
                "A:${WItemEvent.SELECTED}:true",
                "A:${WItemEvent.DESELECTED}:false",
                "B:${WItemEvent.SELECTED}:true",
            )
        }
    }
}
