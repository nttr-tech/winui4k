package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableRowSorter
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Tests that check, by actually showing them, the column widths of WTable (shared across the table's width, as with JTable's default autoResizeMode AUTO_RESIZE_SUBSEQUENT_COLUMNS)
 * and the room for the sort indicator in the column header.
 */
@Tags("E2E")
class WTableColumnWidthTest : FunSpec() {
    /** A table with the 3 columns Name / Department / Age (the same as the sample in the JTable chapter). */
    private fun sampleTable(): WTable = WTable(
        arrayOf(
            arrayOf<Any?>("Alice", "Sales", 34),
            arrayOf<Any?>("Bob", "Engineering", 28),
            arrayOf<Any?>("Carol", "Admin", 45),
        ),
        arrayOf<Any?>("Name", "Department", "Age"),
    )

    /** Shows [component], runs [block] on the UI thread and then removes it. */
    private fun <T> withShown(component: WComponent, table: WTable, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(component)
        try {
            return onUiThreadGet {
                table.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(component)
        }
    }

    /** The width of the viewport of the scroll viewer that lays out the rows in the table (the width the columns share). */
    private fun bodyViewportWidth(table: WTable): Double {
        val viewers = mutableListOf<ComPtr>()
        collect(table.dependencyObject, "Microsoft.UI.Xaml.Controls.ScrollViewer", viewers)
        try {
            // The first is the column header's horizontal scroll, and the last is the body of the rows
            return VisualHitTest.actualSizeOf(viewers.last()).first
        } finally {
            viewers.forEach { it.release() }
        }
    }

    /** Collects into [out] the elements under [node] whose runtime class name is [className], in display order (owned references). */
    private fun collect(node: ComPtr, className: String, out: MutableList<ComPtr>) {
        for (child in tableTreeChildren(node)) {
            if (tableTreeClassName(child) == className) {
                child.addRef()
                out += child
            }
            collect(child, className, out)
            child.release()
        }
    }

    /** The left and right edges of [element] in the table's coordinates. */
    private fun horizontalEdges(table: WTable, element: ComPtr): Pair<Double, Double> {
        val (width, _) = VisualHitTest.actualSizeOf(element)
        val (left, _) = VisualHitTest.transform(element, table.uiElement, 0.0, 0.0)
        return left to left + width
    }

    /**
     * The right edge of the column name's text and the left edge of the sort indicator (in the table's coordinates) when [table] is sorted descending by column [column] (in display order).
     * There is one indicator per column header, in the columns' display order.
     */
    private fun headerTextRightAndIndicatorLeft(table: WTable, column: Int, header: String): Pair<Double, Double> {
        table.rowSorter!!.setSortKeys(listOf(RowSorter.SortKey(table.convertColumnIndexToModel(column), SortOrder.DESCENDING)))
        table.updateLayout()
        val text = checkNotNull(tableTreeFindText(table.dependencyObject, header)) { "header not found: $header" }
        val indicators = mutableListOf<ComPtr>()
        collect(table.dependencyObject, "Microsoft.UI.Private.Controls.SortIndicator", indicators)
        try {
            return horizontalEdges(table, text).second to horizontalEdges(table, indicators[column]).first
        } finally {
            text.release()
            indicators.forEach { it.release() }
        }
    }

    init {
        test("columns without a width split the table's width equally, leaving no space on the right") {
            val table = onUiThreadGet { sampleTable().also { it.width = 360.0 } }
            val (widths, viewport) = withShown(table, table) {
                table.columnModel.getColumns().map { it.width } to bodyViewportWidth(table)
            }
            widths.sum() shouldBe (viewport plusOrMinus 1.0)
            widths.forEach { it shouldBe (viewport / 3 plusOrMinus 1.0) }
        }

        test("a column with preferredWidth gets that width, and the columns without a width share the rest") {
            val table = onUiThreadGet {
                sampleTable().also {
                    it.width = 360.0
                    it.columnModel.getColumn(0).preferredWidth = 200.0
                }
            }
            val (widths, viewport) = withShown(table, table) {
                table.columnModel.getColumns().map { it.width } to bodyViewportWidth(table)
            }
            widths[0] shouldBe 200.0
            widths[1] shouldBe ((viewport - 200.0) / 2 plusOrMinus 1.0)
            widths[2] shouldBe ((viewport - 200.0) / 2 plusOrMinus 1.0)
        }

        test("when the table's width grows, the columns without a width grow too") {
            val table = onUiThreadGet { sampleTable().also { it.width = 300.0 } }
            val (before, after) = withShown(table, table) {
                val narrow = table.columnModel.getColumn(1).width
                table.width = 600.0
                table.updateLayout()
                narrow to table.columnModel.getColumn(1).width
            }
            after shouldBe (before + 100.0 plusOrMinus 1.0)
        }

        test("in a container where the width is not decided (a horizontally scrolling WScrollPane), columns without a width get TableView's default width (120)") {
            val table = onUiThreadGet { sampleTable() }
            val pane = onUiThreadGet {
                WScrollPane(table, WScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
                    .also {
                        it.width = 300.0
                        it.height = 200.0
                    }
            }
            withShown(pane, table) { table.columnModel.getColumns().map { it.width } } shouldBe listOf(120.0, 120.0, 120.0)
        }

        test("in the header of a column sorted descending, the sort indicator does not overlap the column name's text (with the default widths sharing the table's width)") {
            val table = onUiThreadGet {
                sampleTable().also {
                    it.width = 360.0
                    it.rowSorter = TableRowSorter(it.model)
                }
            }
            val (textRight, indicatorLeft) = withShown(table, table) { headerTextRightAndIndicatorLeft(table, 2, "Age") }
            textRight shouldBeLessThanOrEqual indicatorLeft
        }

        test("in a column narrower than its name, the name is truncated before the sort indicator, and the indicator does not overlap the name's text") {
            val table = onUiThreadGet {
                sampleTable().also {
                    it.width = 360.0
                    it.rowSorter = TableRowSorter(it.model)
                    it.columnModel.getColumn(1).preferredWidth = 90.0
                }
            }
            val (textRight, indicatorLeft) = withShown(table, table) { headerTextRightAndIndicatorLeft(table, 1, "Department") }
            textRight shouldBeLessThanOrEqual indicatorLeft
        }
    }
}
