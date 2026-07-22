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
    }
}
