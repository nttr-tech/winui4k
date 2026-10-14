package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/** Tests the XAML that draws an SvgIcon and the layout size of WSvgIcon on a real window. */
class WSvgIconTest : FunSpec() {
    private val material = SvgIcon.parse(
        """<svg viewBox="0 -960 960 960"><path d="M480-80q-83 0-156-31.5T197-197L480-480Z"/></svg>""",
    )

    init {
        test("the XAML scales the design grid into a square of the icon size and fills the path with the foreground") {
            val xaml = SvgIconXaml.build(material, 20.0, "#FF112233")
            xaml shouldContain """<Viewbox Width="20" Height="20" Stretch="Uniform""""
            xaml shouldContain """<Canvas Width="960" Height="960">"""
            xaml shouldContain """<Path Data="F1 M480 880 q-83 0 -156 -31.5 T197 763 L480 480 Z" Fill="#FF112233" />"""
        }

        test("a fixed color in the SVG takes precedence over the foreground, and opacity is written only when below 1") {
            val icon = SvgIcon.parse("""<svg viewBox="0 0 24 24"><path fill="#E53935" opacity="0.5" d="M0 0h1"/></svg>""")
            val xaml = SvgIconXaml.build(icon, 16.0, "#FF000000")
            xaml shouldContain """Fill="#E53935""""
            xaml shouldContain """Opacity="0.5""""
            xaml shouldNotContain "#FF000000"
        }

        test("a stroked path is drawn with round caps and its stroke width, and without a fill when fill is none") {
            val icon = SvgIcon.parse("""<svg viewBox="0 0 24 24"><path fill="none" stroke="currentColor" stroke-width="2" d="M4 4L20 20"/></svg>""")
            val xaml = SvgIconXaml.build(icon, 16.0, "#FF000000")
            xaml shouldContain """Stroke="#FF000000" StrokeThickness="2" StrokeStartLineCap="Round""""
            xaml shouldNotContain "Fill="
        }

        test("without a foreground, paths follow the theme's primary text color") {
            SvgIconXaml.build(material, 16.0) shouldContain """Fill="{ThemeResource TextFillColorPrimaryBrush}""""
        }

        test("a WSvgIcon in a window takes the icon size, and follows changes to iconSize") {
            val icon = onUiThreadGet { WSvgIcon(material, 24.0) }
            UiTestHarness.attachAndAwaitLoaded(icon)
            try {
                val sizes = onUiThreadGet {
                    icon.updateLayout()
                    val before = icon.actualWidth to icon.actualHeight
                    icon.iconSize = 32.0
                    icon.updateLayout()
                    listOf(before, icon.actualWidth to icon.actualHeight)
                }
                sizes shouldBe listOf(24.0 to 24.0, 32.0 to 32.0)
            } finally {
                UiTestHarness.detach(icon)
            }
        }

        test("a WSvgIcon without an icon is empty, and setting the icon and foreground later is reflected") {
            val result = onUiThreadGet {
                val icon = WSvgIcon()
                val empty = icon.icon == null
                icon.icon = material
                icon.foreground = WColor.RED
                listOf(empty, icon.icon == material, icon.foreground == WColor.RED)
            }
            result shouldBe listOf(true, true, true)
        }
    }
}
