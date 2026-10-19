package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for WRepeatButton's repeat speed (delay / interval) and the text, doClick and ActionListener that work as in
 * JButton. Repeating while held requires actual mouse operation, so this checks reading back the settings and clicking
 * from the program. The Java-facing form is in WRepeatButtonForJavaTest.
 */
class WRepeatButtonTest : FunSpec() {
    init {
        test("the label passed to the constructor can be retrieved via text, and is an empty string when unset") {
            val (labeled, empty) = onUiThreadGet { WRepeatButton("Increase").text to WRepeatButton().text }
            labeled shouldBe "Increase"
            empty shouldBe ""
        }

        test("delay and interval read back the values set and do not affect each other") {
            val button = onUiThreadGet { WRepeatButton("Increase") }
            val initialInterval = onUiThreadGet { button.interval }
            val afterDelay = onUiThreadGet {
                button.delay = 600
                button.delay to button.interval
            }
            val afterInterval = onUiThreadGet {
                button.interval = 40
                button.delay to button.interval
            }
            val boundary = onUiThreadGet {
                button.delay = 0
                button.interval = 1
                button.delay to button.interval
            }
            // Changing only delay does not change interval, and changing only interval does not change delay
            afterDelay shouldBe (600 to initialInterval)
            afterInterval shouldBe (600 to 40)
            boundary shouldBe (0 to 1)
        }

        test("doClick calls the ActionListener once per call (a single click, not repeated)") {
            onUiThreadGet {
                val button = WRepeatButton("Increase")
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                val afterFirst = count
                button.doClick()
                afterFirst to count
            } shouldBe (1 to 2)
        }

        test("doClick on a disabled WRepeatButton does nothing, and removed listeners are not called") {
            val (disabled, removed) = onUiThreadGet {
                val button = WRepeatButton("Increase")
                var disabledCount = 0
                var removedCount = 0
                button.addActionListener { disabledCount++ }
                val removedListener: (WActionEvent) -> Unit = { removedCount++ }
                button.addActionListener(removedListener)
                button.removeActionListener(removedListener)
                button.isEnabled = false
                button.doClick()
                disabledCount to removedCount
            }
            disabled shouldBe 0
            removed shouldBe 0
        }

        test("clickMode, actionCommand and mnemonic can be used the same way as in WButton") {
            val (mode, command, mnemonic) = onUiThreadGet {
                val button = WRepeatButton("Increase")
                button.clickMode = ClickMode.PRESS
                button.setMnemonic('i')
                Triple(button.clickMode, button.actionCommand, button.mnemonic)
            }
            mode shouldBe ClickMode.PRESS
            command shouldBe "Increase"
            mnemonic shouldBe 73
        }
    }
}
