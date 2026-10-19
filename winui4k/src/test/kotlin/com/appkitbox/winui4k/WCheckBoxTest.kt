package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests verifying WCheckBox (a WToggleButton subclass)'s checked state and listeners. */
class WCheckBoxTest : FunSpec() {
    init {
        test("the label passed to the constructor can be retrieved via text") {
            onUiThreadGet { WCheckBox("I agree").text } shouldBe "I agree"
        }

        test("setting isChecked reaches the ItemListener with the value after the change") {
            val received = mutableListOf<Boolean?>()
            onUiThread {
                val checkBox = WCheckBox("Receive notifications")
                checkBox.addItemListener { received.add(it) }
                checkBox.isChecked = true
                checkBox.isChecked = false
            }
            received shouldContainExactly listOf<Boolean?>(true, false)
        }

        test("isThreeState = true also allows the indeterminate state (null)") {
            onUiThreadGet {
                val checkBox = WCheckBox()
                checkBox.isThreeState = true
                checkBox.isChecked = null
                checkBox.isChecked
            } shouldBe null
        }

        test("doClick toggles the check box and calls ActionListener and ItemListener once each") {
            val actions = mutableListOf<String>()
            val items = mutableListOf<Boolean?>()
            val states = onUiThreadGet {
                val checkBox = WCheckBox("I agree")
                checkBox.addActionListener { actions.add(it.actionCommand ?: "") }
                checkBox.addItemListener { items.add(it) }
                checkBox.doClick()
                val afterFirst = checkBox.isSelected
                checkBox.doClick()
                afterFirst to checkBox.isSelected
            }
            states shouldBe (true to false)
            actions shouldContainExactly listOf("I agree", "I agree")
            items shouldContainExactly listOf<Boolean?>(true, false)
        }

        test("doClick on a disabled check box does nothing") {
            val (selected, actionCount) = onUiThreadGet {
                val checkBox = WCheckBox("I agree")
                var actionCount = 0
                checkBox.addActionListener { actionCount++ }
                checkBox.isEnabled = false
                checkBox.doClick()
                checkBox.isSelected to actionCount
            }
            selected shouldBe false
            actionCount shouldBe 0
        }
    }
}
