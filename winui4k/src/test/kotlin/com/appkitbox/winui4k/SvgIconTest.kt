package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/** Tests SvgIcon's parsing of SVG documents and the normalization of path data to a (0, 0) origin (no UI needed). */
class SvgIconTest : FunSpec() {
    init {
        test("a Material Symbols icon (viewBox 0 -960 960 960) is shifted down by 960 so its origin is (0, 0)") {
            val icon = SvgIcon.parse(
                """<svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24">""" +
                    """<path d="M480-80q-83 0-156-31.5T197-197L480-480Z"/></svg>""",
            )
            icon.width shouldBe 960.0
            icon.height shouldBe 960.0
            icon.paths shouldHaveSize 1
            // Absolute coordinates (M / T / L) move; relative ones (q) stay as they are
            icon.paths[0].data shouldBe "F1 M480 880 q-83 0 -156 -31.5 T197 763 L480 480 Z"
        }

        test("H and V shift only their own axis, and lowercase h / v stay as they are") {
            val icon = SvgIcon.parse("""<svg viewBox="-10 -20 100 100"><path d="M0 0H5V6h1v2"/></svg>""")
            icon.paths[0].data shouldBe "F1 M10 20 H15 V26 h1 v2"
        }

        test("an absolute arc shifts only its end point, and flags written without separators are read one digit at a time") {
            val icon = SvgIcon.parse("""<svg viewBox="0 -10 24 24"><path d="M2 2A5 5 0 0110 10a3 3 0 1 1 4 4"/></svg>""")
            icon.paths[0].data shouldBe "F1 M2 12 A5 5 0 0 1 10 20 a3 3 0 1 1 4 4"
        }

        test("a relative m at the start of the data is shifted as absolute, but its implicit line-to pairs stay relative") {
            val icon = SvgIcon.parse("""<svg viewBox="0 -10 24 24"><path d="m1 1 2 2z"/></svg>""")
            icon.paths[0].data shouldBe "F1 m1 11 2 2 z"
        }

        test("the implicit line-to pairs after an absolute M are shifted too") {
            val icon = SvgIcon.parse("""<svg viewBox="0 -10 24 24"><path d="M0 0 10 10 20 0Z"/></svg>""")
            icon.paths[0].data shouldBe "F1 M0 10 10 20 20 10 Z"
        }

        test("all control points of C / S / Q are shifted") {
            val icon = SvgIcon.parse("""<svg viewBox="0 -1 10 10"><path d="M0 0C1 1 2 2 3 3S4 4 5 5Q6 6 7 7"/></svg>""")
            icon.paths[0].data shouldBe "F1 M0 1 C1 2 2 3 3 4 S4 5 5 6 Q6 7 7 8"
        }

        test("compact numbers (1.5.5, -.5, exponents) are split into separate values") {
            val icon = SvgIcon.parse("""<svg viewBox="0 0 10 10"><path d="M1.5.5l-.5-1e1"/></svg>""")
            icon.paths[0].data shouldBe "F1 M1.5 0.5 l-0.5 -10"
        }

        test("fill-rule=evenodd maps to XAML's F0, and the default (nonzero) to F1") {
            val icon = SvgIcon.parse(
                """<svg viewBox="0 0 10 10"><path fill-rule="evenodd" d="M0 0h1"/><path d="M0 0h1"/></svg>""",
            )
            icon.paths.map { it.data } shouldBe listOf("F0 M0 0 h1", "F1 M0 0 h1")
        }

        test("a fixed fill color is kept, while a missing fill or currentColor follows the foreground (null)") {
            val icon = SvgIcon.parse(
                """<svg viewBox="0 0 10 10"><path fill="#E53935" d="M0 0h1"/><path fill="currentColor" d="M0 0h1"/>""" +
                    """<path d="M0 0h1"/><path fill="#abc" d="M0 0h1"/></svg>""",
            )
            icon.paths.map { it.fillColor } shouldBe listOf("#E53935", null, null, "#AABBCC")
            icon.paths.map { it.isFilled } shouldBe listOf(true, true, true, true)
        }

        test("a path with fill=none and a stroke becomes a stroked path with its stroke width") {
            val icon = SvgIcon.parse(
                """<svg viewBox="0 0 24 24"><path fill="none" stroke="currentColor" stroke-width="2" d="M4 4L20 20"/></svg>""",
            )
            val path = icon.paths.single()
            path.isFilled shouldBe false
            path.isStroked shouldBe true
            path.strokeColor shouldBe null
            path.strokeWidth shouldBe 2.0
        }

        test("opacity and fill-opacity are multiplied together") {
            val icon = SvgIcon.parse("""<svg viewBox="0 0 10 10"><path opacity="0.5" fill-opacity="0.5" d="M0 0h1"/></svg>""")
            icon.paths.single().opacity shouldBe 0.25
        }

        test("paths nested in groups are collected in document order") {
            val icon = SvgIcon.parse(
                """<svg viewBox="0 0 10 10"><g><path d="M1 1h1"/><g><path d="M2 2h1"/></g></g><path d="M3 3h1"/></svg>""",
            )
            icon.paths.map { it.data } shouldBe listOf("F1 M1 1 h1", "F1 M2 2 h1", "F1 M3 3 h1")
        }

        test("without a viewBox, the width and height attributes become the design size") {
            val icon = SvgIcon.parse("""<svg width="20" height="16"><path d="M0 0h1"/></svg>""")
            icon.width shouldBe 20.0
            icon.height shouldBe 16.0
        }

        test("a non-square viewBox keeps its width and height") {
            val icon = SvgIcon.parse("""<svg viewBox="0 0 32 16"><path d="M0 0h1"/></svg>""")
            icon.width shouldBe 32.0
            icon.height shouldBe 16.0
        }

        test("a document whose root is not svg is rejected") {
            shouldThrow<IllegalArgumentException> { SvgIcon.parse("""<html><path d="M0 0h1"/></html>""") }
        }

        test("malformed XML is rejected") {
            shouldThrow<IllegalArgumentException> { SvgIcon.parse("""<svg viewBox="0 0 10 10"><path d="M0 0h1">""") }
        }

        test("path data with an unknown command or a missing parameter is rejected") {
            shouldThrow<IllegalArgumentException> { SvgIcon.parse("""<svg viewBox="0 0 10 10"><path d="M0 0X1"/></svg>""") }
            shouldThrow<IllegalArgumentException> { SvgIcon.parse("""<svg viewBox="0 0 10 10"><path d="M0 0L1"/></svg>""") }
        }

        test("a document with a DOCTYPE is rejected (no external entities are resolved)") {
            val svg = """<?xml version="1.0"?><!DOCTYPE svg [<!ENTITY x SYSTEM "file:///C:/Windows/win.ini">]>""" +
                """<svg viewBox="0 0 10 10"><path d="&x;"/></svg>"""
            shouldThrow<IllegalArgumentException> { SvgIcon.parse(svg) }
        }

        test("fill, stroke and stroke-width set on the svg root or a group are inherited by the paths inside") {
            val icon = SvgIcon.parse(
                """<svg viewBox="0 0 24 24" fill="none" stroke="#112233" stroke-width="2"><g><path d="M0 0h1"/></g></svg>""",
            )
            val path = icon.paths.single()
            path.isFilled shouldBe false
            path.isStroked shouldBe true
            path.strokeColor shouldBe "#112233"
            path.strokeWidth shouldBe 2.0
        }

        test("an attribute on the path overrides the inherited one, and a sibling group keeps its own values") {
            val icon = SvgIcon.parse(
                """<svg viewBox="0 0 24 24" fill="#FF0000"><g fill="#00FF00"><path d="M0 0h1"/><path fill="#0000FF" d="M1 1h1"/></g>""" +
                    """<path d="M2 2h1"/></svg>""",
            )
            icon.paths.map { it.fillColor } shouldBe listOf("#00FF00", "#0000FF", "#FF0000")
        }

        test("fill-rule is inherited from the root or a group") {
            val icon = SvgIcon.parse("""<svg viewBox="0 0 10 10" fill-rule="evenodd"><path d="M0 0h1"/></svg>""")
            icon.paths.single().data shouldBe "F0 M0 0 h1"
        }

        test("a document without any size information is rejected") {
            shouldThrow<IllegalArgumentException> { SvgIcon.parse("""<svg><path d="M0 0h1"/></svg>""") }
        }
    }
}
