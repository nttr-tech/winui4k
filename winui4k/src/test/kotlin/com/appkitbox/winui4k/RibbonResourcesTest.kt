package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.ribbon.RibbonChromeStyle
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * E2E test verifying that the ribbon's theme resources, shared styles, and text-width measurement work in actual WinUI.
 */
class RibbonResourcesTest : FunSpec() {
    /** A Canvas placed in the window, used as the basis for text-width measurement. */
    private lateinit var host: XamlElement

    init {
        beforeSpec {
            host = onUiThreadGet { XamlElement.load("<Canvas Width=\"0\" Height=\"0\" Opacity=\"0\" IsHitTestVisible=\"False\" />") }
            UiTestHarness.attachAndAwaitLoaded(host)
        }

        afterSpec {
            UiTestHarness.detach(host)
            onUiThread { WRibbonTheme.apply(RibbonThemePalette.WORD, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.OFFICE) }
        }

        test("merging the theme resources makes the ribbon brush colors come from the palette") {
            onUiThreadGet {
                WRibbonTheme.ensureResources()
                WRibbonTheme.getBrushColor("RibbonAccentBrush")
            } shouldBe RibbonColor.parse("#185ABD")
        }

        test("applying a palette rewrites the colors of the brush instances in place (light and dark)") {
            val (light, dark) = onUiThreadGet {
                WRibbonTheme.applyPalette(RibbonThemePalette.EXCEL)
                WRibbonTheme.getBrushColor("RibbonAccentBrush", "Light") to WRibbonTheme.getBrushColor("RibbonAccentBrush", "Dark")
            }
            light shouldBe RibbonColor.parse("#107C41")
            dark shouldBe RibbonColor.parse("#5CC689")
        }

        test("the color of a single brush can be overridden") {
            onUiThreadGet {
                WRibbonTheme.setBrushColor("RibbonItemHoverBrush", RibbonColor.parse("#123456"))
                WRibbonTheme.getBrushColor("RibbonItemHoverBrush")
            } shouldBe RibbonColor.parse("#123456")
        }

        test("setting the surface style to CAD changes the surface colors and also swaps the shape resources") {
            onUiThreadGet {
                WRibbonTheme.applyStyle(RibbonThemeStyle.CAD)
                WRibbonTheme.getBrushColor("RibbonCommandBarBackgroundBrush", "Dark")
            } shouldBe RibbonColor.parse("#3B4453")
        }

        test("XAML that uses the shared styles (item buttons, inputs, tab headers) can be loaded") {
            onUiThreadGet {
                WRibbonTheme.ensureResources()
                listOf(
                    "<Button Style=\"{StaticResource RibbonItemButtonStyle}\" />",
                    "<ToggleButton Style=\"{StaticResource RibbonItemToggleButtonStyle}\" />",
                    "<ToggleButton Style=\"{StaticResource RibbonCheckBoxStyle}\" />",
                    "<ContentControl Style=\"{StaticResource RibbonSplitButtonHostStyle}\" />",
                    "<ContentControl Style=\"{StaticResource RibbonComboBoxHostStyle}\" />",
                    "<ContentControl Style=\"{StaticResource RibbonSpinnerHostStyle}\" />",
                    "<ContentControl Style=\"{StaticResource RibbonTextBoxHostStyle}\" />",
                    "<ContentControl Style=\"{StaticResource RibbonSliderHostStyle}\" />",
                    "<Button Style=\"{StaticResource RibbonTabHeaderStyle}\" />",
                    "<Button Style=\"{StaticResource RibbonBackstageNavButtonStyle}\" />",
                    "<Flyout FlyoutPresenterStyle=\"{StaticResource RibbonBareFlyoutPresenterStyle}\" />",
                ).forEach { Xaml.load(it).release() }
            }
        }

        test("text-width measurement is wider for longer strings and returns the same value from the cache for the same string") {
            val (short, long, again) = onUiThreadGet {
                val measurer = RibbonTextMeasurer(host)
                Triple(measurer.width("Cut", 12.0), measurer.width("Format Painter", 12.0), measurer.width("Cut", 12.0))
            }
            short shouldBeGreaterThan 0.0
            long shouldBeGreaterThan short
            again shouldBe short
        }

        test("the XAML for item content actually loads and has the computed size") {
            val size = onUiThreadGet {
                WRibbonTheme.ensureResources()
                val measurer = RibbonTextMeasurer(host)
                val layout = RibbonItemContent.layout(
                    RibbonContentSpec("Format Painter", RibbonIcons.FORMAT_PAINTER, null, RibbonItemSize.LARGE, RibbonMetrics.COMFORTABLE, showChevron = true),
                    measurer,
                )
                val element = XamlElement.load(layout.xaml)
                host.addChild(element)
                host.updateLayout()
                val desired = element.desiredSize()
                host.removeChild(element)
                desired.toList() to listOf(layout.width, layout.height)
            }
            size.first shouldBe size.second
        }
    }
}
