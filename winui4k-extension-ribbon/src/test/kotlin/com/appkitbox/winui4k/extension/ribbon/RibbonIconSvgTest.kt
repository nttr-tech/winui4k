package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.SvgIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIconKind
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIconLayer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/** Tests for [RibbonIcon.svg] (SVG to layers) and for how [RibbonIconXaml] draws the layers it produces. */
class RibbonIconSvgTest : FunSpec() {
    private fun layersOf(icon: RibbonIcon) = RibbonIconLayer.parse(icon.value)

    init {
        test("a Material-style icon becomes one filled layer on a 960 grid with the origin-shifted data") {
            val icon = RibbonIcon.svg("""<svg viewBox="0 -960 960 960"><path d="M480-80L200-760h560Z"/></svg>""")
            icon.kind shouldBe RibbonIconKind.PATH
            icon.viewBoxSize shouldBe 960.0
            icon.foreground.shouldBeNull()
            layersOf(icon) shouldBe listOf(RibbonIconLayer("F1 M480 880 L200 200 h560 Z"))
        }

        test("viewBoxSize is the larger side of a non-square viewBox") {
            RibbonIcon.svg("""<svg viewBox="0 0 20 32"><path d="M0 0h1"/></svg>""").viewBoxSize shouldBe 32.0
            RibbonIcon.svg("""<svg viewBox="0 0 40 16"><path d="M0 0h1"/></svg>""").viewBoxSize shouldBe 40.0
        }

        test("fixed fill colors and opacity are kept per layer, and paths without a color have no layer color") {
            val icon = RibbonIcon.svg(
                """<svg viewBox="0 0 24 24"><path fill="#3da9f5" opacity="0.5" d="M0 0h4v4Z"/><path d="M10 10h4v4Z"/></svg>""",
            )
            layersOf(icon) shouldBe listOf(
                RibbonIconLayer("F1 M0 0 h4 v4 Z", 0.0, "#3DA9F5", 0.5),
                RibbonIconLayer("F1 M10 10 h4 v4 Z"),
            )
        }

        test("a stroke-only path becomes a stroke layer with its width and color") {
            val icon = RibbonIcon.svg(
                """<svg viewBox="0 0 24 24"><path fill="none" stroke="#FF0000" stroke-width="2" d="M2 2L20 20"/></svg>""",
            )
            layersOf(icon) shouldBe listOf(RibbonIconLayer("F1 M2 2 L20 20", 2.0, "#FF0000"))
        }

        test("a path that is both filled and stroked becomes a fill layer followed by a stroke layer") {
            val icon = RibbonIcon.svg(
                """<svg viewBox="0 0 24 24"><path fill="#00FF00" stroke="#0000FF" stroke-width="1.5" d="M2 2h10v10Z"/></svg>""",
            )
            layersOf(icon) shouldBe listOf(
                RibbonIconLayer("F1 M2 2 h10 v10 Z", 0.0, "#00FF00"),
                RibbonIconLayer("F1 M2 2 h10 v10 Z", 1.5, "#0000FF"),
            )
        }

        test("a path with neither fill nor stroke draws nothing and gets no layer") {
            val icon = RibbonIcon.svg("""<svg viewBox="0 0 24 24"><path fill="none" d="M0 0h4"/><path d="M5 5h4"/></svg>""")
            layersOf(icon) shouldBe listOf(RibbonIconLayer("F1 M5 5 h4"))
        }

        test("the fill rule prefix of each layer is kept so evenodd paths keep their holes") {
            val icon = RibbonIcon.svg(
                """<svg viewBox="0 0 24 24"><path fill-rule="evenodd" d="M0 0h10v10H0ZM2 2h6v6H2Z"/></svg>""",
            )
            layersOf(icon).single().data shouldBe "F0 M0 0 h10 v10 H0 Z M2 2 h6 v6 H2 Z"
        }

        test("the foreground argument sets the fixed color of the whole icon") {
            RibbonIcon.svg("""<svg viewBox="0 0 24 24"><path d="M0 0h4"/></svg>""", "#112233").foreground shouldBe "#112233"
            RibbonIcon.svg(SvgIcon.parse("""<svg viewBox="0 0 24 24"><path d="M0 0h4"/></svg>"""), "#112233").foreground shouldBe "#112233"
        }

        test("an SvgIcon and its source string give the same icon") {
            val svg = """<svg viewBox="0 -960 960 960"><path d="M0-960h10"/></svg>"""
            RibbonIcon.svg(svg) shouldBe RibbonIcon.svg(SvgIcon.parse(svg))
        }

        test("a malformed SVG is rejected with an IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> { RibbonIcon.svg("<svg") }
        }

        test("an SVG without any drawable path is rejected instead of producing an invisible icon") {
            shouldThrow<IllegalArgumentException> { RibbonIcon.svg("""<svg viewBox="0 0 24 24"><path fill="none" d="M0 0h4"/></svg>""") }
        }

        test("the string form of a layer with a fill rule prefix is still classified as a path") {
            RibbonIconXaml.classify(RibbonIcon.svg("""<svg viewBox="0 0 24 24"><path d="M0 0h4v4Z"/></svg>""").value) shouldBe RibbonIconKind.PATH
        }

        test("the icon is drawn on a Canvas of the design grid inside a Viewbox of the requested size") {
            val xaml = RibbonIconXaml.build(RibbonIcon.svg("""<svg viewBox="0 -960 960 960"><path d="M0-960h10v10Z"/></svg>"""), 32.0)!!
            xaml shouldContain "<Viewbox Width=\"32\" Height=\"32\""
            xaml shouldContain "<Canvas Width=\"960\" Height=\"960\">"
            xaml shouldContain "Data=\"F1 M0 0 h10 v10 Z\""
        }

        test("menu icons join the layers with only the first fill rule prefix, because a prefix is valid only at the start") {
            val icon = RibbonIcon.svg(
                """<svg viewBox="0 0 24 24"><path fill-rule="evenodd" d="M0 0h4v4Z"/><path d="M10 10h4v4Z"/><path d="M20 20h1"/></svg>""",
            )
            val xaml = RibbonIconXaml.menuIcon(icon)!!
            xaml shouldBe "<PathIcon Data=\"F0 M0 0 h4 v4 Z M10 10 h4 v4 Z M20 20 h1\" />"
        }

        test("a menu icon from a single layer is unchanged") {
            RibbonIconXaml.menuIcon(RibbonIcon.svg("""<svg viewBox="0 0 24 24"><path d="M0 0h4v4Z"/></svg>""")) shouldBe
                "<PathIcon Data=\"F1 M0 0 h4 v4 Z\" />"
        }

        test("an icon with a stroke layer has no menu icon, like other line art") {
            val icon = RibbonIcon.svg("""<svg viewBox="0 0 24 24"><path fill="none" stroke="#000" d="M0 0L5 5"/></svg>""")
            RibbonIconXaml.menuIcon(icon).shouldBeNull()
        }

        test("menu icons of hand-written layers without a fill rule prefix are not altered") {
            val xaml = RibbonIconXaml.menuIcon(RibbonIcon.layers(24.0, RibbonIconLayer("M0,0 L1,1 Z"), RibbonIconLayer("M5,5 L6,6 Z")))!!
            xaml shouldBe "<PathIcon Data=\"M0,0 L1,1 Z M5,5 L6,6 Z\" />"
            xaml shouldNotContain "F1"
        }
    }
}
