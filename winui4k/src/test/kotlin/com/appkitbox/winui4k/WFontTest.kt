package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Verifies WFont's behavior as a value, and that WControl.font / foreground reach the native object. */
class WFontTest : FunSpec() {
    init {
        test("style constants have the same values as AWT's Font and are visible from Java as static fields") {
            listOf(WFont.PLAIN, WFont.BOLD, WFont.ITALIC) shouldBe listOf(0, 1, 2)
            WFont::class.java.getField("BOLD").getInt(null) shouldBe 1
        }

        test("holds name, style, and size, and isBold and isItalic are determined by the style bits") {
            val plain = WFont("Segoe UI", WFont.PLAIN, 12.0)
            plain.name shouldBe "Segoe UI"
            plain.style shouldBe 0
            plain.size2D shouldBe 12.0
            plain.isBold.shouldBeFalse()
            plain.isItalic.shouldBeFalse()

            val boldItalic = WFont("Segoe UI", WFont.BOLD or WFont.ITALIC, 12.0)
            boldItalic.isBold.shouldBeTrue()
            boldItalic.isItalic.shouldBeTrue()
            WFont("Segoe UI", WFont.BOLD, 12.0).isItalic.shouldBeFalse()
            WFont("Segoe UI", WFont.ITALIC, 12.0).isBold.shouldBeFalse()
        }

        test("size is the rounded integer as in AWT, size2D returns the original value, and the Int constructor works too") {
            val font = WFont("Yu Gothic UI", WFont.PLAIN, 12.6)
            font.size shouldBe 13
            font.size2D shouldBe 12.6
            WFont("Yu Gothic UI", WFont.PLAIN, 14).size2D shouldBe 14.0
        }

        test("equals and hashCode match only when name, style, and size are all the same") {
            val base = WFont("Consolas", WFont.BOLD, 16.0)
            base shouldBe WFont("Consolas", WFont.BOLD, 16)
            base.hashCode() shouldBe WFont("Consolas", WFont.BOLD, 16.0).hashCode()
            base shouldNotBe WFont("Consolas Bold", WFont.BOLD, 16.0)
            base shouldNotBe WFont("Consolas", WFont.PLAIN, 16.0)
            base shouldNotBe WFont("Consolas", WFont.BOLD, 16.5)
        }

        test("deriveFont returns a new WFont with only the given attributes replaced, leaving the original unchanged") {
            val base = WFont("Consolas", WFont.PLAIN, 12.0)
            base.deriveFont(WFont.BOLD) shouldBe WFont("Consolas", WFont.BOLD, 12.0)
            base.deriveFont(20f) shouldBe WFont("Consolas", WFont.PLAIN, 20.0)
            base.deriveFont(WFont.ITALIC, 9f) shouldBe WFont("Consolas", WFont.ITALIC, 9.0)
            base shouldBe WFont("Consolas", WFont.PLAIN, 12.0)
        }

        test("a WFont with size 0 or less or NaN, or a style containing undefined bits, cannot be created") {
            shouldThrow<IllegalArgumentException> { WFont("Consolas", WFont.PLAIN, 0.0) }
            shouldThrow<IllegalArgumentException> { WFont("Consolas", WFont.PLAIN, -1.0) }
            shouldThrow<IllegalArgumentException> { WFont("Consolas", WFont.PLAIN, Double.NaN) }
            shouldThrow<IllegalArgumentException> { WFont("Consolas", 4, 12.0) }
            shouldThrow<IllegalArgumentException> { WFont("Consolas", -1, 12.0) }
        }

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
