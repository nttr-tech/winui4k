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
    }
}
