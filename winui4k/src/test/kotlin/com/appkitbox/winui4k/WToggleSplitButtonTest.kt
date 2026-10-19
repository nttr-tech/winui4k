package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for WToggleSplitButton's checked state and ItemListener.
 * ToggleSplitButton does not raise IsCheckedChanged for IsChecked changes before Loaded (it checks m_hasLoaded), so tests
 * that check events attach it to the shared window and wait for Loaded before changing the state.
 */
class WToggleSplitButtonTest : FunSpec() {
    init {
        test("isSelected reads and writes the same value as isChecked") {
            val (initial, afterSelected, afterUnchecked) = onUiThreadGet {
                val button = WToggleSplitButton("Bold")
                val initial = button.isSelected
                button.isSelected = true
                val afterSelected = button.isChecked to button.isSelected
                button.isChecked = false
                Triple(initial, afterSelected, button.isSelected)
            }
            initial shouldBe false
            afterSelected shouldBe (true to true)
            afterUnchecked shouldBe false
        }

        test("each isChecked change calls a Kotlin lambda-style ItemListener with the new value") {
            val received = mutableListOf<Boolean>()
            val button = onUiThreadGet { WToggleSplitButton().also { it.addItemListener { checked -> received.add(checked) } } }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    button.isChecked = true
                    button.isChecked = false
                }
            } finally {
                UiTestHarness.detach(button)
            }
            received shouldContainExactly listOf(true, false)
        }

        test("WItemListener receives an ItemEvent with SELECTED on check and DESELECTED on uncheck, and source is the button") {
            val received = mutableListOf<WItemEvent>()
            val button = onUiThreadGet {
                WToggleSplitButton("Bold").also { it.addItemListener(WItemListener { e -> received.add(e) }) }
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    button.isSelected = true
                    button.isSelected = false
                }
            } finally {
                UiTestHarness.detach(button)
            }
            received.map { it.stateChange } shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            received.all { it.source === button && it.itemSelectable === button && it.item === button }.shouldBeTrue()
        }

        test("a listener removed with removeItemListener is no longer called (both the lambda style and WItemListener)") {
            val lambdaStates = mutableListOf<Boolean>()
            val eventStates = mutableListOf<Int>()
            val button = onUiThreadGet { WToggleSplitButton() }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread {
                    val lambda: (Boolean) -> Unit = { lambdaStates.add(it) }
                    val listener = WItemListener { eventStates.add(it.stateChange) }
                    button.addItemListener(lambda)
                    button.addItemListener(listener)
                    button.isChecked = true
                    button.removeItemListener(lambda)
                    button.removeItemListener(listener)
                    button.isChecked = false
                }
            } finally {
                UiTestHarness.detach(button)
            }
            lambdaStates shouldContainExactly listOf(true)
            eventStates shouldContainExactly listOf(WItemEvent.SELECTED)
        }
    }
}
