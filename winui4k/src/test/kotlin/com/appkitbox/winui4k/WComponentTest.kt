package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Tests verifying the properties common to every WComponent (size / alignment / visibility /
 * theme / listeners) using the concrete class WButton.
 */
class WComponentTest : FunSpec() {
    init {
        test("explicitly setting width and height is reflected in preferredSize") {
            val size = onUiThreadGet {
                val button = WButton("Size")
                button.width = 120.0
                button.height = 48.0
                button.preferredSize()
            }
            size.width shouldBe 120.0
            size.height shouldBe 48.0
        }

        test("opacity returns exactly the value that was set") {
            onUiThreadGet {
                val button = WButton()
                button.opacity = 0.25
                button.opacity
            } shouldBe 0.25
        }

        test("toggling isVisible is reflected in Visibility") {
            val (hidden, shown) = onUiThreadGet {
                val button = WButton()
                button.isVisible = false
                val hidden = button.isVisible
                button.isVisible = true
                hidden to button.isVisible
            }
            hidden.shouldBeFalse()
            shown.shouldBeTrue()
        }

        test("horizontalAlignment and verticalAlignment return exactly the values that were set") {
            val (horizontal, vertical) = onUiThreadGet {
                val button = WButton()
                button.horizontalAlignment = HorizontalAlignment.RIGHT
                button.verticalAlignment = VerticalAlignment.BOTTOM
                button.horizontalAlignment to button.verticalAlignment
            }
            horizontal shouldBe HorizontalAlignment.RIGHT
            vertical shouldBe VerticalAlignment.BOTTOM
        }

        test("explicitly setting requestedTheme also resolves actualTheme to the same theme") {
            val (requested, actual) = onUiThreadGet {
                val button = WButton()
                button.requestedTheme = ElementTheme.DARK
                button.requestedTheme to button.actualTheme
            }
            requested shouldBe ElementTheme.DARK
            actual shouldBe ElementTheme.DARK
        }

        test("adding it to a window fires Loaded and yields its post-layout size") {
            val loaded = LinkedBlockingQueue<Boolean>()
            val button = onUiThreadGet {
                val button = WButton("Loaded test")
                button.width = 150.0
                button.height = 40.0
                button.addLoadedListener { loaded.add(true) }
                button
            }
            UiTestHarness.attach(button)
            try {
                withClue("The Loaded event never arrived") {
                    loaded.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldNotBeNull()
                }
                onUiThreadGet { button.actualWidth } shouldBe 150.0
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("the SizeChanged listener reports the size once layout settles it") {
            val sizes = LinkedBlockingQueue<Pair<Double, Double>>()
            val button = onUiThreadGet {
                val button = WButton("SizeChanged test")
                button.width = 130.0
                button.height = 44.0
                button.addSizeChangedListener { sizes.add(button.actualWidth to button.actualHeight) }
                button
            }
            UiTestHarness.attach(button)
            try {
                withClue("The SizeChanged event never arrived") {
                    val received = mutableListOf<Pair<Double, Double>>()
                    val first = sizes.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    first.shouldNotBeNull()
                    received.add(first)
                    sizes.drainTo(received)
                    received shouldContain (130.0 to 44.0)
                }
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("toolTipText and toolTip refer to the same value and can be cleared with null") {
            val values = onUiThreadGet {
                val button = WButton("Hint")
                button.toolTipText = "Saves the file"
                val viaText = button.toolTip
                button.toolTip = "Different wording"
                val viaToolTip = button.toolTipText
                button.toolTipText = null
                listOf(viaText, viaToolTip, button.toolTip, button.toolTipText)
            }
            values shouldBe listOf("Saves the file", "Different wording", null, null)
        }

        test("name is an empty string when unset, and the value set can be read back from the native object") {
            val (initial, named) = onUiThreadGet {
                val button = WButton()
                val initial = button.name
                button.name = "okButton"
                initial to button.name
            }
            initial shouldBe ""
            named shouldBe "okButton"
        }

        test("accessibleName and accessibleDescription are written to AutomationProperties") {
            val (initial, name, description, cleared) = onUiThreadGet {
                val button = WButton("Save")
                val initial = button.accessibleName
                button.accessibleName = "Save file"
                button.accessibleDescription = "Saves the current document"
                val name = button.accessibleName
                val description = button.accessibleDescription
                button.accessibleName = ""
                listOf(initial, name, description, button.accessibleName)
            }
            initial shouldBe ""
            name shouldBe "Save file"
            description shouldBe "Saves the current document"
            cleared shouldBe ""
        }

        test("minimumSize and maximumSize default to 0 and unlimited, and return the values that were set") {
            val (defaults, updated) = onUiThreadGet {
                val button = WButton()
                val defaults = button.minimumSize to button.maximumSize
                button.minimumSize = WSize(120.0, 36.0)
                button.maximumSize = WSize(400.0, 90.0)
                defaults to (button.minimumSize to button.maximumSize)
            }
            defaults.first shouldBe WSize(0.0, 0.0)
            defaults.second shouldBe WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            updated.first shouldBe WSize(120.0, 36.0)
            updated.second shouldBe WSize(400.0, 90.0)
        }

        test("minimumSize becomes the actual size after layout when larger than the content, and maximumSize caps the actual size") {
            val (wide, narrow) = onUiThreadGet {
                val wide = WButton("Short")
                wide.horizontalAlignment = HorizontalAlignment.LEFT
                wide.verticalAlignment = VerticalAlignment.TOP
                wide.minimumSize = WSize(300.0, 80.0)
                val narrow = WButton("A button with a very long label whose width exceeds the maximum")
                narrow.horizontalAlignment = HorizontalAlignment.LEFT
                narrow.verticalAlignment = VerticalAlignment.TOP
                narrow.maximumSize = WSize(60.0, 30.0)
                wide to narrow
            }
            val panel = onUiThreadGet {
                val box = WPanel()
                box.add(wide)
                box.add(narrow)
                box
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val sizes = onUiThreadGet {
                    wide.updateLayout()
                    narrow.updateLayout()
                    listOf(wide.actualWidth, wide.actualHeight, narrow.actualWidth, narrow.actualHeight)
                }
                sizes[0] shouldBe 300.0
                sizes[1] shouldBe 80.0
                sizes[2] shouldBe 60.0
                sizes[3] shouldBe 30.0
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("setting preferredSize sets width and height, which reach the actual size after layout") {
            val button = onUiThreadGet {
                val button = WButton("Preferred")
                button.horizontalAlignment = HorizontalAlignment.LEFT
                button.verticalAlignment = VerticalAlignment.TOP
                button.preferredSize = WSize(210.0, 44.0)
                button
            }
            onUiThreadGet { button.preferredSize } shouldBe WSize(210.0, 44.0)
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val sizes = onUiThreadGet { button.actualWidth to button.actualHeight }
                sizes shouldBe (210.0 to 44.0)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("a focusable button in the window gets hasFocus via requestFocus, and loses it when focus moves to another button") {
            val (first, second) = onUiThreadGet { WButton("One") to WButton("Two") }
            val panel = onUiThreadGet {
                val box = WPanel()
                box.add(first)
                box.add(second)
                box
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val results = onUiThreadGet {
                    val before = first.hasFocus()
                    val accepted = first.requestFocusInWindow()
                    val firstFocused = first.hasFocus()
                    second.requestFocus()
                    listOf(before, accepted, firstFocused, first.hasFocus(), second.hasFocus())
                }
                results shouldBe listOf(false, true, true, false, true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a button with isFocusable = false returns false from requestFocusInWindow and does not get hasFocus") {
            val button = onUiThreadGet {
                val button = WButton("Not focusable")
                button.isFocusable = false
                button
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val results = onUiThreadGet {
                    val accepted = button.requestFocusInWindow()
                    listOf(button.isFocusable, accepted, button.hasFocus())
                }
                results shouldBe listOf(false, false, false)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("a button not placed in a window returns false from requestFocusInWindow") {
            val results = onUiThreadGet {
                val button = WButton("Not placed")
                listOf(button.isFocusable, button.requestFocusInWindow(), button.hasFocus())
            }
            results shouldBe listOf(true, false, false)
        }
    }
}
