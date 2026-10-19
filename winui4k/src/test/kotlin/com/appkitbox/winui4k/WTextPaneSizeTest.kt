package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the columns and rows of WTextPane (JTextArea.setColumns / setRows). As in Swing, the width is "columns x the
 * width of 'm' + the left and right padding and border", and the height is "rows x the height of one line + the top and
 * bottom padding and border". The expected size of 'm' is measured with a WLabel in the same font placed in the same
 * window, and the padding and border are read from the RichEditBox after it is shown.
 */
class WTextPaneSizeTest : FunSpec() {
    /** A text pane in a horizontally arranged panel, and a label showing 'm' in the same font. */
    private class Row(val panel: WPanel, val pane: WTextPane, val m: WLabel)

    private fun shownRow(configure: WTextPane.() -> Unit): Row {
        val row = onUiThreadGet {
            val panel = WPanel(orientation = Orientation.HORIZONTAL)
            val pane = WTextPane()
            val m = WLabel("m")
            panel.add(pane)
            panel.add(m)
            pane.configure()
            Row(panel, pane, m)
        }
        UiTestHarness.attachAndAwaitLoaded(row.panel)
        return row
    }

    /** Reads the padding and border (left+right total, top+bottom total) and the size of 'm', and computes the expected (width, height) of the text pane. */
    private fun expectedSize(row: Row, columns: Int, rows: Int): Pair<Double, Double> = onUiThreadGet {
        row.m.font = row.pane.font
        row.panel.updateLayout()
        val padding = XamlStructs.getThickness(row.pane.control, XamlInterop.IControl_get_Padding)
        val border = XamlStructs.getThickness(row.pane.control, XamlInterop.IControl_get_BorderThickness)
        val m = row.m.readDesiredSize()
        (columns * m.width + padding[0] + padding[2] + border[0] + border[2]) to
            (rows * m.height + padding[1] + padding[3] + border[1] + border[3])
    }

    private fun actualSizeOf(row: Row): Pair<Double, Double> = onUiThreadGet {
        row.panel.updateLayout()
        row.pane.actualWidth to row.pane.actualHeight
    }

    init {
        test("columns and rows set before placement become columns x the width of 'm' + padding and border, and rows x the line height + padding and border, when shown") {
            val row = shownRow {
                columns = 30
                rows = 6
            }
            try {
                val expected = expectedSize(row, 30, 6)
                eventually(3.seconds) {
                    val (width, height) = actualSizeOf(row)
                    width shouldBe (expected.first plusOrMinus TOLERANCE)
                    height shouldBe (expected.second plusOrMinus TOLERANCE)
                }
                val preferred = onUiThreadGet { row.pane.preferredSize() }
                preferred.width shouldBe (expected.first plusOrMinus TOLERANCE)
                preferred.height shouldBe (expected.second plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("changing rows after being shown changes only the height, growing by the line height times the difference in rows") {
            val row = shownRow { columns = 20 }
            try {
                onUiThread { row.pane.rows = 3 }
                val three = actualSizeOf(row)
                onUiThread { row.pane.rows = 8 }
                val eight = actualSizeOf(row)
                three.second shouldBe (expectedSize(row, 20, 3).second plusOrMinus TOLERANCE)
                eight.second - three.second shouldBe (expectedSize(row, 20, 8).second - expectedSize(row, 20, 3).second plusOrMinus TOLERANCE)
                eight.first shouldBe (three.first plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("changing columns after being shown changes only the width") {
            val row = shownRow { rows = 4 }
            try {
                onUiThread { row.pane.columns = 10 }
                val ten = actualSizeOf(row)
                onUiThread { row.pane.columns = 30 }
                val thirty = actualSizeOf(row)
                ten.first shouldBe (expectedSize(row, 10, 4).first plusOrMinus TOLERANCE)
                thirty.first - ten.first shouldBe (expectedSize(row, 30, 4).first - expectedSize(row, 10, 4).first plusOrMinus TOLERANCE)
                thirty.second shouldBe (ten.second plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("enlarging font remeasures and grows the size even with the same columns and rows") {
            val row = shownRow {
                columns = 10
                rows = 3
            }
            try {
                eventually(3.seconds) { actualSizeOf(row).second shouldBe (expectedSize(row, 10, 3).second plusOrMinus TOLERANCE) }
                val before = actualSizeOf(row)
                onUiThread { row.pane.font = WFont("Consolas", WFont.PLAIN, 24.0) }
                val after = actualSizeOf(row)
                after.first shouldBeGreaterThan before.first
                after.second shouldBeGreaterThan before.second
                after.first shouldBe (expectedSize(row, 10, 3).first plusOrMinus TOLERANCE)
                after.second shouldBe (expectedSize(row, 10, 3).second plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("setting rows back to 0 restores the same height as a text pane without rows set") {
            val row = shownRow { rows = 12 }
            val plain = onUiThreadGet { WTextPane() }
            onUiThread { row.panel.add(plain) }
            try {
                eventually(3.seconds) { actualSizeOf(row).second shouldBe (expectedSize(row, 0, 12).second plusOrMinus TOLERANCE) }
                onUiThread { row.pane.rows = 0 }
                actualSizeOf(row).second shouldBe (onUiThreadGet { plain.actualHeight } plusOrMinus TOLERANCE)
            } finally {
                UiTestHarness.detach(row.panel)
            }
        }

        test("a text pane with explicit width and height prefers the explicit size over columns and rows") {
            val row = shownRow {
                width = 300.0
                height = 120.0
                columns = 5
                rows = 2
            }
            try {
                val (width, height) = actualSizeOf(row)
                width shouldBe (300.0 plusOrMinus TOLERANCE)
                height shouldBe (120.0 plusOrMinus TOLERANCE)
                onUiThreadGet { row.pane.preferredSize() } shouldBe WSize(300.0, 120.0)
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
