package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WTextField.columns (JTextField.setColumns). As in Swing, the width is "columns x the width of 'm' + the
 * left and right padding and border". The expected width of 'm' is measured with a WLabel of the same font placed in
 * the same window, and the padding and border are read from the TextBox after it is shown.
 */
class WTextFieldColumnsTest : FunSpec() {
    /** A text field in a horizontal panel, and a label showing 'm' in the same font. */
    private class Row(val panel: WPanel, val field: WTextField, val m: WLabel)

    private fun shownRow(configure: WTextField.() -> Unit): Row {
        val row = onUiThreadGet {
            val panel = WPanel(orientation = Orientation.HORIZONTAL)
            val field = WTextField()
            val m = WLabel("m")
            panel.add(field)
            panel.add(m)
            field.configure()
            Row(panel, field, m)
        }
        UiTestHarness.attachAndAwaitLoaded(row.panel)
        return row
    }

    /** The expected width of the text field of [row] for [columns] columns (measured with the label set to the same font as the text field). */
    private fun expectedWidth(row: Row, columns: Int): Double = onUiThreadGet {
        row.m.font = row.field.font
        row.panel.updateLayout()
        val padding = XamlStructs.getThickness(row.field.control, XamlInterop.IControl_get_Padding)
        val border = XamlStructs.getThickness(row.field.control, XamlInterop.IControl_get_BorderThickness)
        columns * row.m.readDesiredSize().width + padding[0] + padding[2] + border[0] + border[2]
    }

    private fun actualWidthOf(row: Row): Double = onUiThreadGet {
        row.panel.updateLayout()
        row.field.actualWidth
    }

    init {
        test("columns defaults to 0, and a negative value throws IllegalArgumentException") {
            val field = onUiThreadGet { WTextField() }
            onUiThreadGet { field.columns } shouldBe 0
            shouldThrow<IllegalArgumentException> { onUiThread { field.columns = -1 } }
            onUiThreadGet { field.columns } shouldBe 0
        }

        test("columns set before being placed gives a width of columns x the width of 'm' + padding and border when shown") {
            val row = shownRow { columns = 20 }
            try {
                val expected = expectedWidth(row, 20)
                eventually(3.seconds) { actualWidthOf(row) shouldBe (expected plusOrMinus TOLERANCE) }
                onUiThreadGet { row.field.preferredSize().width } shouldBe (expected plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("changing columns after being shown changes the width, widening it by the width of 'm' times the difference in columns") {
            val row = shownRow { }
            try {
                onUiThread { row.field.columns = 10 }
                val ten = actualWidthOf(row)
                onUiThread { row.field.columns = 30 }
                val thirty = actualWidthOf(row)
                ten shouldBe (expectedWidth(row, 10) plusOrMinus TOLERANCE)
                thirty - ten shouldBe (expectedWidth(row, 30) - expectedWidth(row, 10) plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("enlarging font remeasures and widens the width for the same columns") {
            val row = shownRow { columns = 10 }
            try {
                eventually(3.seconds) { actualWidthOf(row) shouldBe (expectedWidth(row, 10) plusOrMinus TOLERANCE) }
                val before = actualWidthOf(row)
                onUiThread { row.field.font = WFont("Consolas", WFont.PLAIN, 24.0) }
                val after = actualWidthOf(row)
                after shouldBeGreaterThan before
                after shouldBe (expectedWidth(row, 10) plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("setting columns back to 0 restores the same width as a text field whose columns were never set") {
            val row = shownRow { columns = 40 }
            val plain = onUiThreadGet { WTextField() }
            onUiThread { row.panel.add(plain) }
            try {
                eventually(3.seconds) { actualWidthOf(row) shouldBe (expectedWidth(row, 40) plusOrMinus TOLERANCE) }
                onUiThread { row.field.columns = 0 }
                val restored = actualWidthOf(row)
                restored shouldBe (onUiThreadGet { plain.actualWidth } plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("a text field with an explicit width gives width priority over columns") {
            val row = shownRow {
                width = 300.0
                columns = 5
            }
            try {
                actualWidthOf(row) shouldBe (300.0 plusOrMinus TOLERANCE)
                onUiThreadGet { row.field.preferredSize().width } shouldBe 300.0
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }
    }

    private companion object {
        /** The tolerance (DIP) allowing for layout rounding (rounding of positions to the display scale). */
        const val TOLERANCE = 1.0
    }
}
