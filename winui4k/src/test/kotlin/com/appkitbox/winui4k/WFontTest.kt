package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Verifies that WControl.font / foreground reach the native object (WFont's own behavior as a value is covered by
 * WFontForJavaTest).
 */
class WFontTest : FunSpec() {
    init {
        test("the family, weight, italic, and size set on WControl.font can be read back from the native object") {
            val button = onUiThreadGet { WButton("Font") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val (boldItalic, plain) = onUiThreadGet {
                    button.font = WFont("Consolas", WFont.BOLD or WFont.ITALIC, 20.0)
                    val boldItalic = button.font
                    button.font = WFont("Consolas", WFont.PLAIN, 20.0)
                    boldItalic to button.font
                }
                boldItalic shouldBe WFont("Consolas", WFont.BOLD or WFont.ITALIC, 20.0)
                plain shouldBe WFont("Consolas", WFont.PLAIN, 20.0)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("making the font larger increases the button's height after layout") {
            val button = onUiThreadGet {
                val button = WButton("Size")
                button.horizontalAlignment = HorizontalAlignment.LEFT
                button.verticalAlignment = VerticalAlignment.TOP
                button
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val (small, large) = onUiThreadGet {
                    button.font = WFont("Segoe UI", WFont.PLAIN, 12.0)
                    button.updateLayout()
                    val small = button.actualHeight
                    button.font = WFont("Segoe UI", WFont.PLAIN, 48.0)
                    button.updateLayout()
                    small to button.actualHeight
                }
                large shouldBeGreaterThan small
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("font = null returns to the native value before it was set (the theme default)") {
            val button = onUiThreadGet { WButton("Default") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val (before, changed, after) = onUiThreadGet {
                    val before = button.font
                    button.font = WFont("Consolas", WFont.BOLD or WFont.ITALIC, 33.0)
                    val changed = button.font
                    button.font = null
                    Triple(before, changed, button.font)
                }
                changed shouldNotBe before
                after shouldBe before
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("the color set on foreground can be read back from the native Foreground brush") {
            val button = onUiThreadGet { WButton("Text color") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val color = onUiThreadGet {
                    button.foreground = WColor(10, 120, 200, 255)
                    button.foreground
                }
                color shouldBe WColor(10, 120, 200, 255)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("foreground = null returns to the native value before it was set (the theme default)") {
            val button = onUiThreadGet { WButton("Default color") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val (before, changed, after) = onUiThreadGet {
                    val before = button.foreground
                    button.foreground = WColor(255, 0, 255)
                    val changed = button.foreground
                    button.foreground = null
                    Triple(before, changed, button.foreground)
                }
                changed shouldBe WColor(255, 0, 255)
                changed shouldNotBe before
                after shouldBe before
            } finally {
                UiTestHarness.detach(button)
            }
        }
    }
}
