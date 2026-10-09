package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIconKind
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIconLayer
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMetrics
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/** Tests for the UI-independent computations of the ribbon view (item content layout, icon XAML, theme colors). */
class RibbonViewLogicTest : FunSpec() {
    /** Text width measured at 6px per character (to verify the computations independently of real fonts). */
    private val sixPerChar = RibbonTextWidths { texts, _, _ -> texts.map { it.length * 6.0 } }

    private val metrics = RibbonMetrics.COMFORTABLE

    /** A content spec with an icon (Cut). Specify the fields to change with copy. */
    private fun spec(label: String?, size: RibbonItemSize) = RibbonContentSpec(label, RibbonIcons.CUT, null, size, metrics)

    private fun layout(spec: RibbonContentSpec) = RibbonItemContent.layout(spec, sixPerChar)

    init {
        test("a large item splits a two-word label into two lines, and its width is set by the longest line plus left and right padding") {
            val result = layout(spec("Format Painter", RibbonItemSize.LARGE))
            // max(icon 32, "Format" 36, "Painter" 42) + padding 5×2 - 2
            result.width shouldBe 50.0
            result.height shouldBe metrics.largeItemHeight
            result.xaml shouldContain "Text=\"Format\""
            result.xaml shouldContain "Text=\"Painter\""
        }

        test("a large item's width does not go below the minimum width, and the chevron of a one-word label is centered on the second line") {
            val result = layout(spec("Paste", RibbonItemSize.LARGE).copy(showChevron = true))
            result.width shouldBe metrics.largeItemMinWidth
            result.xaml shouldContain "&#xE70D;"
        }

        test("label splitting picks the word boundary that makes the longer line shortest, also accounting for the chevron width on the second line") {
            RibbonItemContent.splitLabel("Insert Table Of Contents", 12.0, false, sixPerChar) shouldBe ("Insert Table" to "Of Contents")
            // If the longer line has the same width, the earlier split is kept
            RibbonItemContent.splitLabel("Aa Bbbb Cc", 12.0, false, sixPerChar) shouldBe ("Aa" to "Bbbb Cc")
            // With a chevron (12px), the split that shortens the second line is chosen
            RibbonItemContent.splitLabel("Aa Bbbb Cc", 12.0, true, sixPerChar) shouldBe ("Aa Bbbb" to "Cc")
        }

        test("an explicit line break splits the label there, and a single word stays on one line") {
            RibbonItemContent.splitLabel("New\nSlide", 12.0, false, sixPerChar) shouldBe ("New" to "Slide")
            RibbonItemContent.splitLabel("Paste", 12.0, false, sixPerChar) shouldBe ("Paste" to "")
        }

        test("medium size is the total width of icon, spacing, label, and padding, at the row height") {
            val result = layout(spec("Cut", RibbonItemSize.MEDIUM))
            // 16 + (18 + 5) + 5×2
            result.width shouldBe 49.0
            result.height shouldBe metrics.rowHeight
        }

        test("at medium size, showLabel false hides the label, but without an icon the label is always shown") {
            layout(spec("Cut", RibbonItemSize.MEDIUM).copy(showLabel = false)).xaml shouldNotContain "Text=\"Cut\""
            layout(spec("Cut", RibbonItemSize.MEDIUM).copy(icon = null, showLabel = false)).xaml shouldContain "Text=\"Cut\""
        }

        test("small size is icon only, and its width is never narrower than its height") {
            val result = layout(spec("Cut", RibbonItemSize.SMALL))
            result.width shouldBe 26.0
            result.xaml shouldNotContain "Text=\"Cut\""
        }

        test("in the simplified ribbon even small size follows the label setting, and the height is the simplified item height") {
            val labeled = layout(spec("Cut", RibbonItemSize.SMALL).copy(isSimplified = true))
            labeled.height shouldBe metrics.simplifiedItemHeight
            labeled.xaml shouldContain "Text=\"Cut\""
            val iconOnly = layout(spec("Cut", RibbonItemSize.SMALL).copy(showLabel = false, isSimplified = true))
            iconOnly.width shouldBe metrics.simplifiedItemHeight
        }

        test("a row item's chevron is placed at the right edge, and its width and spacing are added to the width") {
            val result = layout(spec("Cut", RibbonItemSize.MEDIUM).copy(showChevron = true))
            result.width shouldBe 49.0 + 13.0
        }

        test("the top half of a large split button is icon only, and the bottom half has the height of the label and chevron") {
            val top = layout(spec("Paste", RibbonItemSize.LARGE).copy(showChevron = true, part = RibbonContentPart.ICON_ONLY))
            top.height shouldBe metrics.largeIconSize + 6
            top.xaml shouldNotContain "Text=\"Paste\""
            val bottom = layout(spec("Paste", RibbonItemSize.LARGE).copy(showChevron = true, part = RibbonContentPart.LABEL_AND_CHEVRON))
            bottom.height shouldBe metrics.largeItemHeight - metrics.largeIconSize - 6
            bottom.xaml shouldContain "Text=\"Paste\""
        }

        test("labels are escaped as XML") {
            layout(spec("A & B <C>", RibbonItemSize.MEDIUM)).xaml shouldContain "Text=\"A &amp; B &lt;C&gt;\""
        }

        test("determines the kind of a string icon") {
            RibbonIconXaml.classify("\uE77F") shouldBe RibbonIconKind.GLYPH
            RibbonIconXaml.classify("%") shouldBe RibbonIconKind.TEXT
            RibbonIconXaml.classify("M3 5h2.5l3 4.5") shouldBe RibbonIconKind.PATH
            RibbonIconXaml.classify("[stroke=1.5]M0,0 L4,4") shouldBe RibbonIconKind.PATH
            RibbonIconXaml.classify("ms-appx:///Assets/cut.png") shouldBe RibbonIconKind.IMAGE
            RibbonIconXaml.classify("icons/cut.svg") shouldBe RibbonIconKind.IMAGE
            RibbonIconXaml.classify("Mode") shouldBe RibbonIconKind.TEXT
        }

        test("glyph icons are FontIcons, and glyphs for characters not in the symbol font are drawn as text") {
            RibbonIconXaml.build(RibbonIcons.CUT, 16.0)!! shouldContain "<FontIcon"
            val percent = RibbonIconXaml.build(RibbonIcon.glyph("%"), 16.0)!!
            percent shouldContain "<TextBlock"
            percent shouldNotContain "<FontIcon"
        }

        test("an icon's fixed color takes precedence over the foreground setting, and layers without a color are drawn with the foreground") {
            RibbonIconXaml.build(RibbonIcons.CUT.withForeground("#FF0000"), 16.0, "{ThemeResource X}")!! shouldContain "Foreground=\"#FFFF0000\""
            val layered = RibbonIconXaml.build(
                RibbonIcon.layers(24.0, RibbonIconLayer("M0,0 L1,1", 1.5), RibbonIconLayer("M2,2 h1 v1 Z", 0.0, "#3DA9F5", 0.5)),
                32.0,
                "{ThemeResource RibbonIconBrush}",
            )!!
            layered shouldContain "Stroke=\"{ThemeResource RibbonIconBrush}\""
            layered shouldContain "StrokeStartLineCap=\"Round\""
            layered shouldContain "Fill=\"#FF3DA9F5\""
            layered shouldContain "Opacity=\"0.5\""
        }

        test("menu icons cannot be created from line art or text") {
            RibbonIconXaml.menuIcon(RibbonIcons.CUT)!! shouldContain "<FontIcon"
            RibbonIconXaml.menuIcon(RibbonIcon.path("M0,0 L1,1 Z"))!! shouldContain "<PathIcon"
            RibbonIconXaml.menuIcon(RibbonIcon.stroke("M0,0 L1,1")).shouldBeNull()
            RibbonIconXaml.menuIcon(RibbonIcon.text("A")).shouldBeNull()
            RibbonIconXaml.menuIcon(null).shouldBeNull()
        }

        test("setting a menu icon converter lets line-art icons also appear in menus as the substituted icon") {
            val line = RibbonIcon.stroke("M0,0 L1,1")
            WRibbonTheme.menuIconConverter = RibbonMenuIconConverter { icon -> if (icon == line) RibbonIcons.PEN else null }
            try {
                RibbonIconXaml.menuIcon(line)!! shouldContain "<FontIcon"
                RibbonIconXaml.menuIcon(RibbonIcon.text("A")).shouldBeNull()
            } finally {
                WRibbonTheme.menuIconConverter = null
            }
        }

        test("an image path without a scheme becomes a file URI") {
            RibbonIconXaml.imageUri("ms-appx:///a.png") shouldBe "ms-appx:///a.png"
            RibbonIconXaml.imageUri("C:/icons/a.png").startsWith("file:/") shouldBe true
            RibbonIconXaml.imageUri("https://example.com/a.png") shouldBe "https://example.com/a.png"
        }

        test("light and dark brush colors have every key for all palettes, decorations, and surface styles") {
            for (palette in RibbonThemePalette.PRESETS) {
                for (chrome in RibbonChromeStyle.entries) {
                    for (style in RibbonThemeStyle.entries) {
                        RibbonThemeColors.light(palette, chrome, style).keys shouldContainAll RibbonThemeColors.BRUSH_KEYS
                        RibbonThemeColors.dark(palette, chrome, style).keys shouldContainAll RibbonThemeColors.BRUSH_KEYS
                    }
                }
            }
        }

        test("the accent color comes from the palette, and with the colorful decoration the title bar also uses the accent color") {
            val word = RibbonThemePalette.WORD
            val neutral = RibbonThemeColors.light(word, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.OFFICE)
            neutral["RibbonAccentBrush"] shouldBe RibbonColor.parse("#185ABD")
            neutral["RibbonTitleBarBackgroundBrush"] shouldBe RibbonColor.parse("#F5F5F5")
            val colorful = RibbonThemeColors.light(word, RibbonChromeStyle.COLORFUL, RibbonThemeStyle.OFFICE)
            colorful["RibbonTitleBarBackgroundBrush"] shouldBe RibbonColor.parse("#185ABD")
            RibbonThemeColors.dark(word, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.OFFICE)["RibbonAccentBrush"] shouldBe
                RibbonColor.parse("#6CA0F5")
        }

        test("the CAD surface style changes the surface colors and shapes, while accent-derived colors stay with the palette") {
            val cad = RibbonThemeColors.dark(RibbonThemePalette.CAD, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.CAD)
            cad["RibbonCommandBarBackgroundBrush"] shouldBe RibbonColor.parse("#3B4453")
            cad["RibbonAccentBrush"] shouldBe RibbonThemePalette.CAD.getAccent(true)
            RibbonThemeColors.shapes(RibbonThemeStyle.CAD)["RibbonCommandBarCornerRadius"] shouldBe ("CornerRadius" to "0")
            RibbonThemeColors.shapes(RibbonThemeStyle.OFFICE)["RibbonCommandBarCornerRadius"] shouldBe ("CornerRadius" to "8")
        }

        test("high-contrast brushes reference system colors, and interaction states overlay the highlight color semi-transparently") {
            RibbonThemeColors.highContrastBrushXaml("RibbonAccentBrush") shouldContain "{ThemeResource SystemColorHighlightColor}"
            RibbonThemeColors.highContrastBrushXaml("RibbonItemHoverBrush") shouldContain "Opacity=\"0.3\""
            RibbonThemeColors.highContrastBrushXaml("RibbonPopupBackgroundBrush") shouldContain "SystemColorWindowColor"
            RibbonThemeColors.highContrastBrushXaml("RibbonForegroundBrush") shouldContain "SystemColorWindowTextColor"
            RibbonThemeColors.highContrastBrushXaml("RibbonShadowBrush") shouldContain "Transparent"
        }

        test("numbers in XAML attribute values are written independently of the locale") {
            Xaml.num(12.0) shouldBe "12"
            Xaml.num(1.5) shouldBe "1.5"
            Xaml.num(-0.25) shouldBe "-0.25"
            Xaml.escape("a\"b'c") shouldBe "a&quot;b&apos;c"
        }

        test("adds namespace declarations to the root element") {
            Xaml.withNamespaces("<Grid Width=\"1\" />") shouldContain "xmlns=\"http://schemas.microsoft.com/winfx/2006/xaml/presentation\""
            val declared = "<Grid xmlns=\"http://schemas.microsoft.com/winfx/2006/xaml/presentation\" />"
            Xaml.withNamespaces(declared) shouldBe declared
        }
    }
}
