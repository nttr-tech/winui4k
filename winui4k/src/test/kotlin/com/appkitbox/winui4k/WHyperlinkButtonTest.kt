package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify reading back WHyperlinkButton's navigateUri, and text, doClick and ActionListener as in JButton.
 * doClick on a button with navigateUri set would launch the default browser, so doClick is checked only with buttons
 * without navigateUri. The form seen from Java is in WHyperlinkButtonForJavaTest.
 */
class WHyperlinkButtonTest : FunSpec() {
    init {
        test("text and navigateUri passed to the constructor can be read back") {
            val (text, uri) = onUiThreadGet {
                val button = WHyperlinkButton("Documents", "https://example.com/docs/index.html")
                button.text to button.navigateUri
            }
            text shouldBe "Documents"
            uri shouldBe "https://example.com/docs/index.html"
        }

        test("navigateUri is an empty string when unset, the URI set can be read back, and an empty string clears it") {
            val (initial, set, cleared) = onUiThreadGet {
                val button = WHyperlinkButton("Links")
                val initial = button.navigateUri
                button.navigateUri = "https://example.com/a/b?x=1#top"
                val set = button.navigateUri
                button.navigateUri = ""
                Triple(initial, set, button.navigateUri)
            }
            initial shouldBe ""
            set shouldBe "https://example.com/a/b?x=1#top"
            cleared shouldBe ""
        }

        test("changing navigateUri to another URI lets the new URI be read back") {
            onUiThreadGet {
                val button = WHyperlinkButton("Links", "https://example.com/first")
                button.navigateUri = "https://example.org/second"
                button.navigateUri
            } shouldBe "https://example.org/second"
        }

        test("doClick on a WHyperlinkButton without navigateUri calls the ActionListener once per call") {
            onUiThreadGet {
                val button = WHyperlinkButton("Help")
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                val afterFirst = count
                button.doClick()
                afterFirst to count
            } shouldBe (1 to 2)
        }

        test("the WActionEvent of the ActionListener has the button itself as source and text as actionCommand") {
            val (sameSource, command) = onUiThreadGet {
                val button = WHyperlinkButton("Help")
                val events = mutableListOf<WActionEvent>()
                button.addActionListener { e -> events += e }
                button.doClick()
                (events.single().source === button) to events.single().actionCommand
            }
            sameSource shouldBe true
            command shouldBe "Help"
        }

        test("doClick on a disabled WHyperlinkButton does nothing") {
            onUiThreadGet {
                val button = WHyperlinkButton("Help")
                var count = 0
                button.addActionListener { count++ }
                button.isEnabled = false
                button.doClick()
                count
            } shouldBe 0
        }
    }
}
