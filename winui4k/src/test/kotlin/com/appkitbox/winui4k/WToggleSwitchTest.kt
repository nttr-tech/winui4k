package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly

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
    }
}
