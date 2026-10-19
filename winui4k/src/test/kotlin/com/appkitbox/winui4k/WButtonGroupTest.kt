package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.Collections

/** Tests for the mutual exclusion and registration management of WButtonGroup (ButtonGroup-style). */
class WButtonGroupTest : FunSpec() {
    init {
        test("the number and order of added buttons appear in buttonCount and elements, and adding the same button twice counts once") {
            val (count, texts) = onUiThreadGet {
                val group = WButtonGroup()
                val first = WCheckBox("A")
                val second = WCheckBox("B")
                group.add(first)
                group.add(second)
                group.add(first)
                group.buttonCount to Collections.list(group.elements).map { it.text }
            }
            count shouldBe 2
            texts shouldContainExactly listOf("A", "B")
        }

        test("an empty group has a null selection, and it stays null after adding buttons unless they are selected") {
            val selection = onUiThreadGet {
                val group = WButtonGroup()
                group.add(WCheckBox("A"))
                group.selection
            }
            selection.shouldBeNull()
        }

        test("a button set with setSelected(true) becomes the selection, and the other buttons are deselected") {
            val states = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                val c = WToggleButton("C")
                listOf(a, b, c).forEach { group.add(it) }
                group.setSelected(a, true)
                val afterA = listOf(a.isSelected, b.isSelected, c.isSelected) to (group.selection === a)
                group.setSelected(c, true)
                val afterC = listOf(a.isSelected, b.isSelected, c.isSelected) to (group.selection === c)
                listOf(afterA, afterC)
            }
            states[0] shouldBe (listOf(true, false, false) to true)
            states[1] shouldBe (listOf(false, false, true) to true)
        }

        test("isSelected(button) returns true only for the selected button") {
            val flags = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                group.setSelected(b, true)
                group.isSelected(a) to group.isSelected(b)
            }
            flags shouldBe (false to true)
        }

        test("a button's doClick is also mutually exclusive, and doClick on a selected button does not deselect it (same as Swing's JToggleButton)") {
            val trace = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                a.doClick()
                val afterA = listOf(a.isSelected, b.isSelected)
                b.doClick()
                val afterB = listOf(a.isSelected, b.isSelected)
                b.doClick() // Pressing the selected one again does not deselect it
                val afterBAgain = listOf(a.isSelected, b.isSelected) to (group.selection === b)
                listOf(afterA, afterB, afterBAgain)
            }
            trace[0] shouldBe listOf(true, false)
            trace[1] shouldBe listOf(false, true)
            trace[2] shouldBe (listOf(false, true) to true)
        }

        test("doClick on a selected button still calls the ActionListener") {
            val actions = mutableListOf<String>()
            onUiThread {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                group.add(a)
                a.addActionListener { actions.add(it.actionCommand ?: "") }
                a.doClick()
                a.doClick()
            }
            actions shouldContainExactly listOf("A", "A")
        }

        test("setSelected(false) has no effect on the selected button and does nothing to an unselected button") {
            val states = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                group.setSelected(a, true)
                group.setSelected(a, false) // Ignored, as in Swing
                group.setSelected(b, false)
                listOf(a.isSelected, b.isSelected) to (group.selection === a)
            }
            states shouldBe (listOf(true, false) to true)
        }

        test("setting the button's own isSelected = false is also reverted for the group's selected button") {
            val selected = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                group.add(a)
                group.setSelected(a, true)
                a.isSelected = false
                a.isSelected to (group.selection === a)
            }
            selected shouldBe (true to true)
        }

        test("clearSelection deselects all buttons and sets selection to null, and buttons can be selected again afterwards") {
            val trace = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                group.setSelected(a, true)
                group.clearSelection()
                val cleared = listOf(a.isSelected, b.isSelected) to group.selection
                group.setSelected(b, true)
                cleared to (listOf(a.isSelected, b.isSelected) to (group.selection === b))
            }
            trace.first shouldBe (listOf(false, false) to null)
            trace.second shouldBe (listOf(false, true) to true)
        }

        test("adding a selected button makes it the selection, and a second selected button is changed to unselected") {
            val trace = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A").also { it.isSelected = true }
                val b = WCheckBox("B").also { it.isSelected = true }
                group.add(a)
                group.add(b)
                listOf(a.isSelected, b.isSelected) to (group.selection === a)
            }
            trace shouldBe (listOf(true, false) to true)
        }

        test("a removed button is no longer part of the mutual exclusion, and if it was selected, selection becomes null (the button stays selected)") {
            val trace = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                group.setSelected(a, true)
                group.remove(a)
                val afterRemove = (group.selection) to (a.isSelected to group.buttonCount)
                b.isSelected = true // a, which left the group, is not affected
                afterRemove to listOf(a.isSelected, b.isSelected)
            }
            trace.first shouldBe (null to (true to 1))
            trace.second shouldBe listOf(true, true)
        }

        test("the selection of buttons that were not removed is kept after remove") {
            val selected = onUiThreadGet {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                group.setSelected(b, true)
                group.remove(a)
                group.selection === b
            }
            selected shouldBe true
        }

        test("adding a button that belongs to another group removes it from the original group") {
            val trace = onUiThreadGet {
                val first = WButtonGroup()
                val second = WButtonGroup()
                val a = WCheckBox("A")
                first.add(a)
                second.add(a)
                listOf(first.buttonCount, second.buttonCount)
            }
            trace shouldContainExactly listOf(0, 1)
        }

        test("WToggleButton, WCheckBox and WRadioButton are mutually exclusive in a single group") {
            val trace = onUiThreadGet {
                val group = WButtonGroup()
                val toggle = WToggleButton("T")
                val check = WCheckBox("C")
                val radio = WRadioButton("R")
                listOf(toggle, check, radio).forEach { group.add(it) }
                toggle.doClick()
                check.doClick()
                val afterCheck = listOf(toggle.isSelected, check.isSelected, radio.isSelected)
                radio.doClick()
                afterCheck to listOf(toggle.isSelected, check.isSelected, radio.isSelected)
            }
            trace.first shouldBe listOf(false, true, false)
            trace.second shouldBe listOf(false, false, true)
        }

        test("for buttons with an ItemListener in a group, SELECTED is delivered on selection and DESELECTED on deselection") {
            val events = mutableListOf<String>()
            onUiThread {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WCheckBox("B")
                group.add(a)
                group.add(b)
                a.addItemListener(WItemListener { events.add("A:" + it.stateChange) })
                b.addItemListener(WItemListener { events.add("B:" + it.stateChange) })
                a.doClick()
                b.doClick()
            }
            events shouldContainExactly listOf(
                "A:${WItemEvent.SELECTED}",
                "A:${WItemEvent.DESELECTED}",
                "B:${WItemEvent.SELECTED}",
            )
        }

        test("whether the ItemListener is added before or after joining the group, DESELECTED of the deselected button arrives before SELECTED of the selected one") {
            // Swing's ButtonGroup.setSelected deselects the old button before changing the new button's state
            val events = mutableListOf<String>()
            onUiThread {
                val group = WButtonGroup()
                val a = WCheckBox("A")
                val b = WToggleButton("B")
                val c = WRadioButton("C")
                a.addItemListener(WItemListener { events.add("A:" + it.stateChange) })
                b.addItemListener(WItemListener { events.add("B:" + it.stateChange) })
                listOf(a, b, c).forEach { group.add(it) }
                c.addItemListener(WItemListener { events.add("C:" + it.stateChange) })
                a.doClick()
                b.doClick()
                c.doClick()
                a.doClick()
            }
            events shouldContainExactly listOf(
                "A:${WItemEvent.SELECTED}",
                "A:${WItemEvent.DESELECTED}",
                "B:${WItemEvent.SELECTED}",
                "B:${WItemEvent.DESELECTED}",
                "C:${WItemEvent.SELECTED}",
                "C:${WItemEvent.DESELECTED}",
                "A:${WItemEvent.SELECTED}",
            )
        }

        test("trying to deselect the selected button (click, isSelected = false, three-state indeterminate) delivers nothing to the ItemListener (as in Swing)") {
            val (events, states) = onUiThreadGet {
                val events = mutableListOf<Int>()
                val group = WButtonGroup()
                val a = WCheckBox("A").also { it.isThreeState = true }
                group.add(a)
                group.setSelected(a, true)
                a.addItemListener(WItemListener { events.add(it.stateChange) })
                a.doClick() // A three-state check box would normally advance to indeterminate
                a.isSelected = false
                a.isChecked = null
                events to listOf(a.isChecked, group.selection === a)
            }
            events.shouldBeEmpty()
            states shouldContainExactly listOf(true, true)
        }

        test("WRadioButtons in the same group get the same groupName (so that WinUI's arrow-key navigation works within the group)") {
            val names = onUiThreadGet {
                val group = WButtonGroup()
                val radios = listOf("A", "B", "C").map { WRadioButton(it).also { radio -> group.add(radio) } }
                radios.map { it.groupName }
            }
            names.toSet().size shouldBe 1
            names.first().isNotEmpty() shouldBe true
        }

        test("for radio buttons of a group placed in a window, DESELECTED of the deselected one arrives before SELECTED of the selected one") {
            val events = mutableListOf<String>()
            val (panel, radios) = onUiThreadGet {
                val panel = WPanel()
                val group = WButtonGroup()
                val radios = listOf("A", "B").map { text ->
                    WRadioButton(text).also {
                        panel.add(it)
                        it.addItemListener(WItemListener { e -> events.add(text + ":" + e.stateChange) })
                        group.add(it)
                    }
                }
                panel to radios
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val states = onUiThreadGet {
                    radios[0].doClick()
                    radios[1].doClick()
                    radios[1].isSelected = false // The group's selection is not cleared
                    radios.map { it.isSelected }
                }
                states shouldContainExactly listOf(false, true)
            } finally {
                UiTestHarness.detach(panel)
            }
            events shouldContainExactly listOf(
                "A:${WItemEvent.SELECTED}",
                "A:${WItemEvent.DESELECTED}",
                "B:${WItemEvent.SELECTED}",
            )
        }

        test("when the group has a selection, adding a selected radio button after placing it keeps the original selection and deselects the added one") {
            val (panel, radios) = onUiThreadGet {
                val panel = WPanel()
                val radios = listOf("A", "B").map { WRadioButton(it).also { radio -> panel.add(radio) } }
                // Give them different names so that both stay selected after being placed
                radios[0].groupName = "first"
                radios[1].groupName = "second"
                panel to radios
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val trace = onUiThreadGet {
                    radios.forEach { it.isSelected = true }
                    val group = WButtonGroup()
                    group.add(radios[0])
                    group.add(radios[1])
                    radios.map { it.isSelected } to (group.selection === radios[0])
                }
                trace shouldBe (listOf(true, false) to true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("WRadioButtons of different groups under the same parent are mutually exclusive independently per group") {
            val trace = onUiThreadGet {
                val panel = WPanel()
                val groupA = WButtonGroup()
                val groupB = WButtonGroup()
                val a1 = WRadioButton("A1")
                val a2 = WRadioButton("A2")
                val b1 = WRadioButton("B1")
                val b2 = WRadioButton("B2")
                listOf(a1, a2, b1, b2).forEach { panel.add(it) }
                groupA.add(a1)
                groupA.add(a2)
                groupB.add(b1)
                groupB.add(b2)
                a1.doClick()
                b1.doClick()
                val both = listOf(a1.isSelected, a2.isSelected, b1.isSelected, b2.isSelected)
                a2.doClick()
                both to listOf(a1.isSelected, a2.isSelected, b1.isSelected, b2.isSelected)
            }
            trace.first shouldBe listOf(true, false, true, false)
            trace.second shouldBe listOf(false, true, true, false)
        }

        test("the groupName of a WRadioButton takes a different value per group, and returns to the original value on remove") {
            val trace = onUiThreadGet {
                val groupA = WButtonGroup()
                val groupB = WButtonGroup()
                val a = WRadioButton("A").also { it.groupName = "mine" }
                val b = WRadioButton("B")
                groupA.add(a)
                groupB.add(b)
                val inGroup = a.groupName
                val differs = a.groupName != b.groupName && a.groupName.isNotEmpty() && b.groupName.isNotEmpty()
                groupA.remove(a)
                groupB.remove(b)
                Triple(inGroup != "mine", differs, a.groupName to b.groupName)
            }
            trace.first shouldBe true
            trace.second shouldBe true
            trace.third shouldBe ("mine" to "")
        }

        test("a group of WRadioButtons placed in a window is also mutually exclusive with doClick after being placed") {
            val (panel, radios) = onUiThreadGet {
                val panel = WPanel()
                val group = WButtonGroup()
                val radios = listOf("A", "B", "C").map { text ->
                    WRadioButton(text).also {
                        panel.add(it)
                        group.add(it)
                    }
                }
                panel to radios
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val states = onUiThreadGet {
                    radios[0].doClick()
                    radios[2].doClick()
                    radios.map { it.isSelected }
                }
                states shouldBe listOf(false, false, true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }
}
