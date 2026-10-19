package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for [WControl.border], the equivalent of JComponent.setBorder / getBorder, and the borders of [WBorderFactory].
 * Borders are checked by reading the native values (Control.BorderBrush / BorderThickness / Padding).
 * The native Thickness order is Left, Top, Right, Bottom, which differs from Swing's Insets order top, left, bottom, right.
 */
class WControlBorderTest : FunSpec() {
    /** The native border thickness [Left, Top, Right, Bottom]. */
    private fun thickness(control: WControl): List<Double> =
        XamlStructs.getThickness(control.control, XamlInterop.IControl_get_BorderThickness).toList()

    /** The native inner padding [Left, Top, Right, Bottom]. */
    private fun padding(control: WControl): List<Double> =
        XamlStructs.getThickness(control.control, XamlInterop.IControl_get_Padding).toList()

    /** The native border color (null if it is not a solid color or is unset). */
    private fun borderColor(control: WControl): WColor? {
        val brush = control.control.getPtrOrNull(XamlInterop.IControl_get_BorderBrush) ?: return null
        return try {
            colorOf(brush)
        } finally {
            brush.release()
        }
    }

    private class Native(val thickness: List<Double>, val padding: List<Double>, val color: WColor?)

    private fun native(control: WControl) = Native(thickness(control), padding(control), borderColor(control))

    private fun <T> ui(block: () -> T): T = onUiThreadGet(block)

    init {
        test("a line border puts its color and thickness into BorderBrush and BorderThickness and leaves the inner padding unchanged") {
            val (before, after, border) = ui {
                val button = WButton("OK")
                val before = native(button)
                val line = WBorderFactory.createLineBorder(WColor.RED, 2)
                button.border = line
                Triple(before, native(button), line to button.border)
            }
            after.thickness shouldBe listOf(2.0, 2.0, 2.0, 2.0)
            after.color shouldBe WColor.RED
            after.padding shouldBe before.padding
            border.second shouldBeSameInstanceAs border.first
        }

        test("a line border without a thickness is 1") {
            val after = ui {
                val field = WTextField()
                field.border = WBorderFactory.createLineBorder(WColor.BLUE)
                native(field)
            }
            after.thickness shouldBe listOf(1.0, 1.0, 1.0, 1.0)
            after.color shouldBe WColor.BLUE
        }

        test("an empty border becomes the inner padding in Swing's argument order (top, left, bottom, right) and removes the default border line") {
            val after = ui {
                val button = WButton("OK")
                button.border = WBorderFactory.createEmptyBorder(5, 10, 15, 20)
                native(button)
            }
            // The native order is Left, Top, Right, Bottom
            after.padding shouldBe listOf(10.0, 5.0, 20.0, 15.0)
            after.thickness shouldBe listOf(0.0, 0.0, 0.0, 0.0)
        }

        test("a compound of a line and an empty border yields both the border line and the inner padding, regardless of outside/inside order") {
            val line = WBorderFactory.createLineBorder(WColor.GREEN, 3)
            val empty = WBorderFactory.createEmptyBorder(1, 2, 3, 4)
            for (compound in listOf(WBorderFactory.createCompoundBorder(line, empty), WBorderFactory.createCompoundBorder(empty, line))) {
                val after = ui {
                    val box = WCheckBox("Check")
                    box.border = compound
                    native(box)
                }
                after.thickness shouldBe listOf(3.0, 3.0, 3.0, 3.0)
                after.color shouldBe WColor.GREEN
                after.padding shouldBe listOf(2.0, 1.0, 4.0, 3.0)
            }
        }

        test("in nested compounds, the empty borders' padding is added up") {
            val inner = WBorderFactory.createCompoundBorder(WBorderFactory.createLineBorder(WColor.RED), WBorderFactory.createEmptyBorder(1, 2, 3, 4))
            val nested = WBorderFactory.createCompoundBorder(inner, WBorderFactory.createEmptyBorder(10, 20, 30, 40))
            val after = ui {
                val button = WButton("OK")
                button.border = nested
                native(button)
            }
            after.padding shouldBe listOf(22.0, 11.0, 44.0, 33.0)
            after.thickness shouldBe listOf(1.0, 1.0, 1.0, 1.0)
        }

        test("a compound with one side null is the same as the other border alone") {
            val line = WBorderFactory.createLineBorder(WColor.RED, 2)
            val alone = ui {
                val button = WButton("OK")
                button.border = line
                native(button)
            }
            val compound = ui {
                val button = WButton("OK")
                button.border = WBorderFactory.createCompoundBorder(null, line)
                native(button)
            }
            compound.thickness shouldBe alone.thickness
            compound.color shouldBe alone.color
            compound.padding shouldBe alone.padding
        }

        test("setting null restores the border color, thickness and inner padding to the theme defaults before setting, and getBorder is null") {
            val (before, after, border) = ui {
                val button = WButton("OK")
                val before = native(button)
                button.border = WBorderFactory.createCompoundBorder(
                    WBorderFactory.createLineBorder(WColor.RED, 4),
                    WBorderFactory.createEmptyBorder(9, 9, 9, 9),
                )
                button.border = null
                Triple(before, native(button), button.border)
            }
            after.thickness shouldBe before.thickness
            after.padding shouldBe before.padding
            after.color shouldBe before.color
            border.shouldBeNull()
        }

        test("switching to another border leaves nothing of the previous one (line to empty, empty to line)") {
            val (defaults, afterEmpty, afterLine) = ui {
                val fromLine = WButton("A")
                val defaults = native(fromLine)
                fromLine.border = WBorderFactory.createLineBorder(WColor.RED, 4)
                fromLine.border = WBorderFactory.createEmptyBorder(2, 2, 2, 2)
                val fromEmpty = WButton("B")
                fromEmpty.border = WBorderFactory.createEmptyBorder(8, 8, 8, 8)
                fromEmpty.border = WBorderFactory.createLineBorder(WColor.BLUE, 2)
                Triple(defaults, native(fromLine), native(fromEmpty))
            }
            // line -> empty: the line thickness and color are gone, and the padding is the empty border's value
            afterEmpty.thickness shouldBe listOf(0.0, 0.0, 0.0, 0.0)
            afterEmpty.color shouldBe defaults.color
            afterEmpty.padding shouldBe listOf(2.0, 2.0, 2.0, 2.0)
            // empty -> line: the padding returns to the default, and the border line is the line border's value
            afterLine.padding shouldBe defaults.padding
            afterLine.thickness shouldBe listOf(2.0, 2.0, 2.0, 2.0)
            afterLine.color shouldBe WColor.BLUE
        }

        test("a compound containing two lines throws IllegalArgumentException, leaving the previous border and native values as they were") {
            val before = WBorderFactory.createLineBorder(WColor.RED, 2)
            val twoLines = WBorderFactory.createCompoundBorder(WBorderFactory.createLineBorder(WColor.BLUE), WBorderFactory.createLineBorder(WColor.GREEN))
            val (border, after, snapshot) = ui {
                val button = WButton("OK")
                button.border = before
                val snapshot = native(button)
                shouldThrow<IllegalArgumentException> { button.border = twoLines }
                Triple(button.border, native(button), snapshot)
            }
            border shouldBeSameInstanceAs before
            after.thickness shouldBe snapshot.thickness
            after.color shouldBe snapshot.color
            after.padding shouldBe snapshot.padding
        }

        test("getBorderInsets is the space the border takes: the thickness for a line, the given values for empty, the sum for a compound") {
            WBorderFactory.createLineBorder(WColor.RED, 2).borderInsets shouldBe WInsets(2.0, 2.0, 2.0, 2.0)
            WBorderFactory.createEmptyBorder(1, 2, 3, 4).borderInsets shouldBe WInsets(top = 1.0, left = 2.0, bottom = 3.0, right = 4.0)
            WBorderFactory.createCompoundBorder(
                WBorderFactory.createLineBorder(WColor.RED, 2),
                WBorderFactory.createEmptyBorder(1, 2, 3, 4),
            ).borderInsets shouldBe WInsets(top = 3.0, left = 4.0, bottom = 5.0, right = 6.0)
            WBorderFactory.createCompoundBorder(null, null).borderInsets shouldBe WInsets(0.0, 0.0, 0.0, 0.0)
        }

        test("omitted arguments and factory compounds: createEmptyBorder() is 0, createCompoundBorder() has both null") {
            WBorderFactory.createEmptyBorder() shouldBe WEmptyBorder(0, 0, 0, 0)
            WBorderFactory.createCompoundBorder() shouldBe WCompoundBorder(null, null)
            WBorderFactory.createLineBorder(WColor.RED) shouldBe WLineBorder(WColor.RED, 1)
            val compound = WBorderFactory.createCompoundBorder(WBorderFactory.createEmptyBorder(1, 1, 1, 1), WBorderFactory.createLineBorder(WColor.RED))
            compound.outsideBorder shouldBe WEmptyBorder(1, 1, 1, 1)
            compound.insideBorder shouldBe WLineBorder(WColor.RED, 1)
        }

        test("negative thickness or padding throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> { WLineBorder(WColor.RED, -1) }
            shouldThrow<IllegalArgumentException> { WEmptyBorder(-1, 0, 0, 0) }
            shouldThrow<IllegalArgumentException> { WEmptyBorder(0, 0, 0, -2) }
        }
    }
}
