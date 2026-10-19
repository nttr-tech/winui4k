package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests verifying WToggleSwitch's on/off state and Toggled listener. */
class WToggleSwitchTest : FunSpec() {
    init {
        test("isOn returns exactly the value that was set") {
            val (initial, on) = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Airplane mode")
                val initial = toggleSwitch.isOn
                toggleSwitch.isOn = true
                initial to toggleSwitch.isOn
            }
            initial.shouldBeFalse() // starts out off
            on.shouldBeTrue()
        }

        test("the ItemListener fires with the new value on every change to isOn") {
            val received = mutableListOf<Boolean>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                toggleSwitch.addItemListener { received.add(it) }
                toggleSwitch.isOn = true
                toggleSwitch.isOn = false
            }
            received shouldContainExactly listOf(true, false)
        }

        test("a listener removed via removeItemListener no longer fires") {
            val received = mutableListOf<Boolean>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                val listener: (Boolean) -> Unit = { received.add(it) }
                toggleSwitch.addItemListener(listener)
                toggleSwitch.isOn = true
                toggleSwitch.removeItemListener(listener)
                toggleSwitch.isOn = false
            }
            received shouldContainExactly listOf(true)
        }

        test("isSelected reads and writes the same value as isOn") {
            val (afterSelected, afterOff) = onUiThreadGet {
                val toggleSwitch = WToggleSwitch()
                toggleSwitch.isSelected = true
                val afterSelected = toggleSwitch.isOn to toggleSwitch.isSelected
                toggleSwitch.isOn = false
                afterSelected to toggleSwitch.isSelected
            }
            afterSelected shouldBe (true to true)
            afterOff shouldBe false
        }

        test("WItemListener receives an ItemEvent of SELECTED when turned on and DESELECTED when turned off, with the switch as source") {
            val received = mutableListOf<WItemEvent>()
            val toggleSwitch = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Airplane mode")
                toggleSwitch.addItemListener(WItemListener { received.add(it) })
                toggleSwitch.isOn = true
                toggleSwitch.isSelected = false
                toggleSwitch
            }
            received.map { it.stateChange } shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            received.all { it.source === toggleSwitch && it.itemSelectable === toggleSwitch && it.item === toggleSwitch }.shouldBeTrue()
        }

        test("removing the WItemListener with removeItemListener stops delivery") {
            val states = mutableListOf<Int>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                val listener = WItemListener { states.add(it.stateChange) }
                toggleSwitch.addItemListener(listener)
                toggleSwitch.isOn = true
                toggleSwitch.removeItemListener(listener)
                toggleSwitch.isOn = false
            }
            states shouldContainExactly listOf(WItemEvent.SELECTED)
        }
    }
}
