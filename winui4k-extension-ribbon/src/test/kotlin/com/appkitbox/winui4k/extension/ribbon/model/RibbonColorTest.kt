package com.appkitbox.winui4k.extension.ribbon.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlin.math.abs

/**
 * Tests for colors (RibbonColor), the Office palettes (RibbonColorPalette), and the theme palettes (RibbonThemePalette).
 * Ported from RibbonSpace's ColorTests.
 */
class RibbonColorTest : FunSpec() {
    init {
        test("parses #RRGGBB, #AARRGGBB, #RGB, and colors without #") {
            RibbonColor.parse("#FF0000") shouldBe RibbonColor(255, 255, 0, 0)
            RibbonColor.parse("80FF0000") shouldBe RibbonColor(128, 255, 0, 0)
            RibbonColor.parse("#abc") shouldBe RibbonColor(255, 0xAA, 0xBB, 0xCC)
            RibbonColor.parse("  #0f6cbd ") shouldBe RibbonColor.fromRgb(0x0F, 0x6C, 0xBD)
        }

        test("hex notation is #RRGGBB when opaque and #AARRGGBB when translucent (uppercase)") {
            RibbonColor.parse("#185abd").toHex() shouldBe "#185ABD"
            RibbonColor.parse("#80FF0000").toHex() shouldBe "#80FF0000"
            RibbonColor.parse("#185ABD").toHex(includeAlpha = true) shouldBe "#FF185ABD"
            RibbonColor.parse("#185ABD").toString() shouldBe "#185ABD"
        }

        test("an invalid color yields null from tryParse and throws from parse") {
            RibbonColor.tryParse("#12345").shouldBeNull()
            RibbonColor.tryParse("zzzzzz").shouldBeNull()
            RibbonColor.tryParse("").shouldBeNull()
            RibbonColor.tryParse(null).shouldBeNull()
            shouldThrow<IllegalArgumentException> { RibbonColor.parse("#GG0000") }
            shouldThrow<IllegalArgumentException> { RibbonColor(256, 0, 0, 0) }
        }

        test("converting to HSL and back yields the original color (each component within 1)") {
            val color = RibbonColor.parse("#4472C4")
            val (h, s, l) = color.toHsl()
            val back = RibbonColor.fromHsl(h, s, l)
            abs(back.r - color.r) shouldBeLessThanOrEqual 1
            abs(back.g - color.g) shouldBeLessThanOrEqual 1
            abs(back.b - color.b) shouldBeLessThanOrEqual 1
            RibbonColor.fromHsl(0.0, 0.0, 0.5) shouldBe RibbonColor(255, 128, 128, 128) // 127.5 rounds half to even to 128
        }

        test("lightening and darkening change the lightness, fully lightening black gives white, and black/white contrast ratio is 21") {
            val blue = RibbonColor.parse("#4472C4")
            blue.lighten(0.4).lightness shouldBeGreaterThan blue.lightness
            blue.darken(0.25).lightness shouldBeLessThan blue.lightness
            RibbonColor.BLACK.lighten(1.0) shouldBe RibbonColor.WHITE
            RibbonColor.WHITE.darken(1.0) shouldBe RibbonColor.BLACK
            Math.round(RibbonColor.contrastRatio(RibbonColor.BLACK, RibbonColor.WHITE)) shouldBe 21L
            RibbonColor.contrastRatio(blue, blue) shouldBe 1.0
            RibbonColor.parse("#FFFF00").prefersDarkForeground shouldBe true
            RibbonColor.parse("#185ABD").prefersDarkForeground shouldBe false
        }

        test("the ratio is clamped to 0..1 and the opacity is preserved") {
            val translucent = RibbonColor.parse("#804472C4")
            translucent.lighten(5.0).a shouldBe 0x80
            translucent.lighten(5.0) shouldBe translucent.lighten(1.0)
            translucent.darken(-1.0) shouldBe translucent.darken(0.0)
            translucent.withAlpha(0xFF).toHex() shouldBe "#4472C4"
        }

        test("blend returns the original colors at both ends and linearly interpolates each component in between") {
            val black = RibbonColor.BLACK
            val white = RibbonColor.WHITE
            black.blend(white, 0.0) shouldBe black
            black.blend(white, 1.0) shouldBe white
            black.blend(white, 0.5) shouldBe RibbonColor(255, 128, 128, 128)
            black.blend(white, 2.0) shouldBe white
        }

        test("the Office theme grid is 10 colors x 6 rows, with white darkened, black lightened, and mid colors shaded both ways") {
            val grid = RibbonColorPalette.buildThemeGrid()
            grid.size shouldBe 6
            grid.forEach { it.size shouldBe 10 }
            grid[0] shouldBe RibbonColorPalette.OFFICE_THEME_COLORS
            grid[1][0].name shouldEndWith "Darker 5%" // the white column gets darker
            grid[1][1].name shouldEndWith "Lighter 50%" // the black column gets lighter
            grid[1][4].name shouldBe "Blue, Accent 1, Lighter 80%"
            grid[5][4].name shouldBe "Blue, Accent 1, Darker 50%"
            RibbonColorPalette.STANDARD_COLORS.size shouldBe 10
            RibbonColorPalette.HIGHLIGHT_COLORS.size shouldBe 15
        }

        test("shades of dark and light colors follow the direction of their lightness") {
            val dark = RibbonColorPalette.generateShades(RibbonColorSwatch(RibbonColor.parse("#1F1F1F"), "Dark"))
            dark.map { it.name } shouldBe listOf(
                "Dark, Lighter 90%",
                "Dark, Lighter 75%",
                "Dark, Lighter 50%",
                "Dark, Lighter 25%",
                "Dark, Lighter 10%",
            )
            val light = RibbonColorPalette.generateShades(RibbonColorSwatch(RibbonColor.parse("#E7E6E6"), "Light"))
            light.first().name shouldBe "Light, Darker 10%"
            light.forEach { it.color.lightness shouldBeLessThan RibbonColor.parse("#E7E6E6").lightness }
        }

        test("a grid can also be built from custom theme colors") {
            val colors = listOf(RibbonColorSwatch(RibbonColor.parse("#336699"), "Custom"))
            val grid = RibbonColorPalette.buildThemeGrid(colors)
            grid.map { it.size } shouldBe listOf(1, 1, 1, 1, 1, 1)
            grid[1][0].color.lightness shouldBeGreaterThan RibbonColor.parse("#336699").lightness
        }

        test("preset accents have contrast of at least 4.5 on white, and dark variants at least 3 on dark backgrounds") {
            RibbonThemePalette.PRESETS.size shouldBe 12
            for (palette in RibbonThemePalette.PRESETS) {
                withClue(palette.name) {
                    RibbonColor.contrastRatio(palette.accent, RibbonColor.WHITE) shouldBeGreaterThanOrEqual 4.5
                    RibbonColor.contrastRatio(palette.accentDark, RibbonColor.parse("#292929")) shouldBeGreaterThanOrEqual 3.0
                }
            }
        }

        test("a palette built from a single accent lightens the dark variant for a dark color and uses a light color as is") {
            val custom = RibbonThemePalette.fromAccent(RibbonColor.parse("#123456"))
            custom.name shouldBe "Custom"
            custom.accentDark.lightness shouldBeGreaterThan custom.accent.lightness
            val light = RibbonThemePalette.fromAccent(RibbonColor.parse("#A0D0FF"), "Sky")
            light.accentDark shouldBe light.accent
        }

        test("accent-derived colors change with the theme, and the text color on the accent picks the more readable one") {
            val word = RibbonThemePalette.WORD
            word.getAccent(false) shouldBe RibbonColor.parse("#185ABD")
            word.getAccent(true) shouldBe RibbonColor.parse("#6CA0F5")
            word.getAccentSubtle(true) shouldBe RibbonColor.parse("#336CA0F5")
            word.getAccentSubtleStrong(true) shouldBe RibbonColor.parse("#556CA0F5")
            word.getAccentSubtle(false).lightness shouldBeGreaterThan word.getAccentSubtleStrong(false).lightness
            word.getOnAccent(false) shouldBe RibbonColor.WHITE
            word.getOnAccent(true) shouldBe RibbonColor.BLACK
        }
    }
}
