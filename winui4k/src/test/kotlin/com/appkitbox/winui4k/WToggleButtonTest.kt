package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Tests verifying WToggleButton's three-state checked handling and ItemListener. */
class WToggleButtonTest : FunSpec() {
    init {
        test("isChecked returns exactly whichever of true / false / null (indeterminate) was set") {
            val states = onUiThreadGet {
                val toggle = WToggleButton("Toggle")
                val initial = toggle.isChecked
                toggle.isChecked = true
                val checked = toggle.isChecked
                toggle.isChecked = null
                val indeterminate = toggle.isChecked
                toggle.isChecked = false
                listOf(initial, checked, indeterminate, toggle.isChecked)
            }
            states[0] shouldBe false // ToggleButton starts out unchecked
            states[1] shouldBe true
            states[2].shouldBeNull()
            states[3] shouldBe false
        }

        test("the ItemListener fires with the new value on every change to isChecked") {
            val received = mutableListOf<Boolean?>()
            onUiThread {
                val toggle = WToggleButton()
                toggle.addItemListener { received.add(it) }
                toggle.isChecked = true
                toggle.isChecked = null
                toggle.isChecked = false
            }
            received shouldContainExactly listOf(true, null, false)
        }

        test("a listener removed via removeItemListener no longer fires") {
            val received = mutableListOf<Boolean?>()
            onUiThread {
                val toggle = WToggleButton()
                val listener: (Boolean?) -> Unit = { received.add(it) }
                toggle.addItemListener(listener)
                toggle.isChecked = true
                toggle.removeItemListener(listener)
                toggle.isChecked = false
            }
            received shouldContainExactly listOf<Boolean?>(true)
        }

        test("isThreeState returns exactly the value that was set") {
            val (on, off) = onUiThreadGet {
                val toggle = WToggleButton()
                toggle.isThreeState = true
                val on = toggle.isThreeState
                toggle.isThreeState = false
                on to toggle.isThreeState
            }
            on.shouldBeTrue()
            off.shouldBeFalse()
        }

        test("doClick checks an unchecked toggle button and calls the ActionListener and the ItemListener once each") {
            val actions = mutableListOf<String>()
            val items = mutableListOf<Boolean?>()
            val checked = onUiThreadGet {
                val toggle = WToggleButton("Toggle")
                toggle.addActionListener { actions.add(it.actionCommand ?: "") }
                toggle.addItemListener { items.add(it) }
                toggle.doClick()
                toggle.isChecked
            }
            checked shouldBe true
            actions shouldContainExactly listOf("Toggle")
            items shouldContainExactly listOf<Boolean?>(true)
        }

        test("calling doClick repeatedly alternates between checked and unchecked") {
            val items = mutableListOf<Boolean?>()
            val states = onUiThreadGet {
                val toggle = WToggleButton()
                toggle.addItemListener { items.add(it) }
                toggle.doClick()
                val first = toggle.isSelected
                toggle.doClick()
                val second = toggle.isSelected
                toggle.doClick()
                listOf(first, second, toggle.isSelected)
            }
            states shouldContainExactly listOf(true, false, true)
            items shouldContainExactly listOf<Boolean?>(true, false, true)
        }

        test("doClick on an isThreeState toggle button advances through checked, indeterminate and unchecked in that order") {
            val states = onUiThreadGet {
                val toggle = WToggleButton()
                toggle.isThreeState = true
                List(3) {
                    toggle.doClick()
                    toggle.isChecked
                }
            }
            states shouldContainExactly listOf<Boolean?>(true, null, false)
        }

        test("doClick on a disabled toggle button does nothing, and toggles again once re-enabled") {
            val actions = mutableListOf<String>()
            val (whileDisabled, afterEnabled) = onUiThreadGet {
                val toggle = WToggleButton("Toggle")
                toggle.addActionListener { actions.add("action") }
                toggle.isEnabled = false
                toggle.doClick()
                val whileDisabled = toggle.isSelected
                toggle.isEnabled = true
                toggle.doClick()
                whileDisabled to toggle.isSelected
            }
            whileDisabled shouldBe false
            afterEnabled shouldBe true
            actions shouldContainExactly listOf("action") // the one call while disabled is not counted
        }

        test("WItemListener receives an ItemEvent with the state after the change, whose source, itemSelectable and item are the toggle button") {
            val received = mutableListOf<WItemEvent>()
            val toggle = onUiThreadGet {
                val toggle = WToggleButton("Toggle")
                toggle.isThreeState = true
                toggle.addItemListener(WItemListener { received.add(it) })
                toggle.isChecked = true
                toggle.isChecked = null
                toggle.isChecked = false
                toggle
            }
            received.map { it.stateChange } shouldContainExactly
                listOf(WItemEvent.SELECTED, WItemEvent.INDETERMINATE, WItemEvent.DESELECTED)
            received.all { it.source === toggle && it.itemSelectable === toggle && it.item === toggle }.shouldBeTrue()
            received.all { it.id == WItemEvent.ITEM_STATE_CHANGED }.shouldBeTrue()
        }

        test("ItemEvent constants have the same values as AWT's ItemEvent (SELECTED=1, DESELECTED=2, ITEM_STATE_CHANGED=701)") {
            WItemEvent.SELECTED shouldBe 1
            WItemEvent.DESELECTED shouldBe 2
            WItemEvent.ITEM_STATE_CHANGED shouldBe 701
            WItemEvent.INDETERMINATE shouldBe 3
        }

        test("setting isSelected also delivers an ItemEvent, and setting the same value again does not") {
            val states = mutableListOf<Int>()
            onUiThread {
                val toggle = WToggleButton()
                toggle.addItemListener(WItemListener { states.add(it.stateChange) })
                toggle.isSelected = true
                toggle.isSelected = true // not delivered because nothing changes
                toggle.isSelected = false
            }
            states shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
        }

        test("a WItemListener removed with removeItemListener no longer receives events, and only the remaining listeners are called") {
            val removed = mutableListOf<Int>()
            val kept = mutableListOf<Int>()
            onUiThread {
                val toggle = WToggleButton()
                val removedListener = WItemListener { removed.add(it.stateChange) }
                toggle.addItemListener(removedListener)
                toggle.addItemListener(WItemListener { kept.add(it.stateChange) })
                toggle.isSelected = true
                toggle.removeItemListener(removedListener)
                toggle.isSelected = false
            }
            removed shouldContainExactly listOf(WItemEvent.SELECTED)
            kept shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
        }

        test("registering the same WItemListener twice calls it twice, and removing it once brings it back to once") {
            val callsPerChange = mutableListOf<Int>()
            onUiThread {
                val toggle = WToggleButton()
                var calls = 0
                val listener = WItemListener { calls++ }
                toggle.addItemListener(listener)
                toggle.addItemListener(listener)
                toggle.isSelected = true
                callsPerChange.add(calls)
                toggle.removeItemListener(listener)
                calls = 0
                toggle.isSelected = false
                callsPerChange.add(calls)
            }
            callsPerChange shouldContainExactly listOf(2, 1)
        }

        test("the Kotlin lambda form (Boolean?) and a WItemListener can both be registered and removed separately") {
            val lambdaStates = mutableListOf<Boolean?>()
            val eventStates = mutableListOf<Int>()
            onUiThread {
                val toggle = WToggleButton()
                val lambda: (Boolean?) -> Unit = { lambdaStates.add(it) }
                toggle.addItemListener(lambda)
                toggle.addItemListener(WItemListener { eventStates.add(it.stateChange) })
                toggle.isSelected = true
                toggle.removeItemListener(lambda)
                toggle.isSelected = false
            }
            lambdaStates shouldContainExactly listOf<Boolean?>(true)
            eventStates shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
        }

        test("isSelected is false while indeterminate, and isSelected = false turns indeterminate into unchecked") {
            val (whileIndeterminate, afterSet) = onUiThreadGet {
                val toggle = WToggleButton()
                toggle.isThreeState = true
                toggle.isChecked = null
                val whileIndeterminate = toggle.isSelected
                toggle.isSelected = false
                whileIndeterminate to toggle.isChecked
            }
            whileIndeterminate shouldBe false
            afterSet shouldBe false
        }
    }
}
