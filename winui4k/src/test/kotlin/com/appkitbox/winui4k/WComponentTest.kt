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
    }
}
