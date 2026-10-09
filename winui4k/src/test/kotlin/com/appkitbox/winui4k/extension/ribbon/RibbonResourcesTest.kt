package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.ElementTheme
import com.appkitbox.winui4k.UiTestHarness
import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMetrics
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

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

        test("brush colors are taken according to the element's theme, and setThemeBrush follows switching between light and dark") {
            onUiThread { WRibbonTheme.apply(RibbonThemePalette.WORD, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.OFFICE) }
            val border = onUiThreadGet { WBorder() }
            UiTestHarness.attachAndAwaitLoaded(border)
            val applied = mutableListOf<WColor>()
            onUiThread {
                border.requestedTheme = ElementTheme.LIGHT
                WRibbonTheme.setThemeBrush(border, "RibbonAccentBrush") { applied += it }
            }
            val light = onUiThreadGet { WRibbonTheme.getBrushColor("RibbonAccentBrush", "Light")!! }
            val dark = onUiThreadGet { WRibbonTheme.getBrushColor("RibbonAccentBrush", "Dark")!! }
            onUiThreadGet { WRibbonTheme.getBrushColor(border, "RibbonAccentBrush") } shouldBe light
            onUiThread { border.requestedTheme = ElementTheme.DARK }
            onUiThreadGet { WRibbonTheme.getBrushColor(border, "RibbonAccentBrush") } shouldBe dark
            // ActualThemeChanged arrives asynchronously (after several layouts when all tests run in a row), so wait until it arrives
            eventually(THEME_TIMEOUT) {
                onUiThreadGet {
                    border.updateLayout()
                    applied.map { RibbonColor(it.alpha, it.red, it.green, it.blue) }
                } shouldBe listOf(light, dark)
            }
            UiTestHarness.detach(border)
        }

        test("corner radius resources follow the surface style and are returned in top-left, top-right, bottom-right, bottom-left order") {
            onUiThreadGet {
                WRibbonTheme.applyStyle(RibbonThemeStyle.OFFICE)
                WRibbonTheme.getCornerRadius("RibbonPopupCornerRadius").toList()
            } shouldBe listOf(6.0, 6.0, 6.0, 6.0)
            onUiThreadGet {
                WRibbonTheme.applyStyle(RibbonThemeStyle.CAD)
                WRibbonTheme.getCornerRadius("RibbonTabCornerRadius").toList()
            } shouldBe listOf(2.0, 2.0, 0.0, 0.0)
            onUiThreadGet { WRibbonTheme.getCornerRadius("Unknown", 3.0).toList() } shouldBe listOf(3.0, 3.0, 3.0, 3.0)
            onUiThread { WRibbonTheme.applyStyle(RibbonThemeStyle.OFFICE) }
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

    private companion object {
        val THEME_TIMEOUT = 5.seconds
    }
}
