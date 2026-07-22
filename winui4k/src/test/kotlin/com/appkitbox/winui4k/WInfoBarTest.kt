package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests verifying WInfoBar's displayed content (title / message / severity) and open state. */
class WInfoBarTest : FunSpec() {
    init {
        test("title and message return exactly the strings that were set") {
            val (title, message) = onUiThreadGet {
                val infoBar = WInfoBar()
                infoBar.title = "Saved"
                infoBar.message = "Your changes have been written to the file."
                infoBar.title to infoBar.message
            }
            title shouldBe "Saved"
            message shouldBe "Your changes have been written to the file."
        }

        test("isOpen starts out false, and setting it to true opens the bar") {
            val (initial, open) = onUiThreadGet {
                val infoBar = WInfoBar()
                val initial = infoBar.isOpen
                infoBar.isOpen = true
                initial to infoBar.isOpen
            }
            initial.shouldBeFalse()
            open.shouldBeTrue()
        }

        test("severity returns exactly the value that was set") {
            onUiThreadGet {
                val infoBar = WInfoBar()
                infoBar.severity = InfoBarSeverity.ERROR
                infoBar.severity
            } shouldBe InfoBarSeverity.ERROR
        }

        test("isClosable and isIconVisible return exactly the values that were set") {
            val (closable, iconVisible) = onUiThreadGet {
                val infoBar = WInfoBar()
                infoBar.isClosable = false
                infoBar.isIconVisible = false
                infoBar.isClosable to infoBar.isIconVisible
            }
            closable.shouldBeFalse()
            iconVisible.shouldBeFalse()
        }

        test("actionButton can be retrieved after being set, and cleared with null") {
            val (set, cleared, button) = onUiThreadGet {
                val infoBar = WInfoBar()
                val button = WButton("Retry")
                infoBar.actionButton = button
                val set = infoBar.actionButton
                infoBar.actionButton = null
                Triple(set, infoBar.actionButton, button)
            }
            set shouldBeSameInstanceAs button
            cleared shouldBe null
        }
    }
}
