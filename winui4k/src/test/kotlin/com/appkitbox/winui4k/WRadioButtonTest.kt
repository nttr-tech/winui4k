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
    }
}
