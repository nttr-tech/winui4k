package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoWindow

/**
 * Excel-style ribbon demo (same layout as the RibbonSpace Excel demo; the ribbon is 100% MVVM: all content is in
 * [com.appkitbox.winui4k.ribbon.RibbonModel], and commands are referenced by id from the command catalog of [ExcelViewModel]).
 * Formula bar, sheet, a sample chart (selecting it shows [Chart Tools]), and a status bar with summary values.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.EXCEL)
        val viewModel = ExcelViewModel()
        val model = createExcelModel(viewModel)
        val ribbon = WRibbon(model)
        ribbon.addItemInvokedListener { event -> if (event.commandId == null) viewModel.record(event.item.label ?: event.item.id) }
        val window = RibbonDemoWindow("Budget 2026 - Excel (WinUI4K Ribbon)", RibbonDemoApp.EXCEL, ribbon)
        window.titleBar!!.subtitle = "• MVVM ribbon"
        window.setTop(ribbon)
        val sheet = SheetView(viewModel)
        // [Gridlines], [Formula Bar], and [Headings] on [View] are view-only state, so the view toggles them when the item is invoked
        ribbon.addItemInvokedListener { event ->
            when (event.item.id) {
                "gridlines" -> sheet.showGridlines = event.parameter == true
                "formulaBar" -> sheet.showFormulaBar = event.parameter == true
                "headings" -> sheet.showHeadings = event.parameter == true
            }
        }
        window.setContent(sheet.view)
        window.setStatusBar(statusBar(viewModel, ribbon))
        window.show()
    }
}

/** The formula bar and the sheet (the selected cell has a green border), plus a sample chart. */
private class SheetView(private val viewModel: ExcelViewModel) {
    private val nameBox = WTextField().also {
        it.width = NAME_BOX_WIDTH
        it.text = viewModel.selected
    }
    private val formula = WTextField()
    private val bar = WGrid()
    private val cells = HashMap<String, Pair<WBorder, WLabel>>()
    private val headers = mutableListOf<WComponent>()
    private val grid = WGrid()
    private val chart = SampleChart(viewModel)

    /** The sheet view. */
    val view: WComponent

    /** Whether to show gridlines. */
    var showGridlines: Boolean = true
        set(value) {
            field = value
            refresh()
        }

    /** Whether to show the formula bar. */
    var showFormulaBar: Boolean
        get() = bar.isVisible
        set(value) {
            bar.isVisible = value
        }

    /** Whether to show the row and column headings. */
    var showHeadings: Boolean = true
        set(value) {
            field = value
            headers.forEach { it.isVisible = value }
        }

    init {
        bar.columnSpacing = 8.0
        bar.addColumn(GridLength.AUTO)
        bar.addColumn(GridLength.AUTO)
        bar.addColumn(GridLength.star())
        bar.add(nameBox, row = 0, column = 0)
        bar.add(WRibbonTheme.label("fx", "RibbonSecondaryForegroundBrush", FX_SIZE).also { it.verticalAlignment = VerticalAlignment.CENTER }, row = 0, column = 1)
        bar.add(formula, row = 0, column = 2)
        bar.margin = BAR_MARGIN
        buildGrid()
        nameBox.addTextChangedListener { text ->
            val reference = text.trim().uppercase()
            if (reference in cells && reference != viewModel.selected) viewModel.selected = reference
        }
        viewModel.addChangeListener { refresh() }
        refresh()
        // As in Excel, the chart floats above the sheet (overlaid on a 4-column x 9-row range starting at column G)
        chart.view.margin = CHART_GAP
        grid.add(chart.view, row = 1, column = CHART_COLUMN, rowSpan = CHART_ROWS, columnSpan = CHART_COLUMNS)
        val sheet = WGrid()
        sheet.addRow(GridLength.AUTO)
        sheet.addRow(GridLength.star())
        sheet.add(bar, row = 0, column = 0)
        sheet.add(WScrollPane(grid), row = 1, column = 0)
        view = sheet
    }

    private fun buildGrid() {
        grid.addColumn(GridLength.pixel(ROW_HEADER_WIDTH))
        viewModel.columns.forEach { _ -> grid.addColumn(GridLength.pixel(CELL_WIDTH)) }
        repeat(viewModel.rowCount + 1) { grid.addRow(GridLength.pixel(CELL_HEIGHT)) }
        viewModel.columns.forEachIndexed { c, name -> grid.add(header(name).also { headers += it }, row = 0, column = c + 1) }
        for (r in 1..viewModel.rowCount) {
            grid.add(header(r.toString()).also { headers += it }, row = r, column = 0)
            viewModel.columns.forEachIndexed { c, name ->
                val label = WLabel("").also {
                    it.verticalAlignment = VerticalAlignment.CENTER
                    it.margin = CELL_PADDING
                }
                val cell = WBorder(label)
                grid.add(cell, row = r, column = c + 1)
                cells["$name$r"] = cell to label
            }
        }
    }

    private fun refresh() {
        for ((key, pair) in cells) {
            pair.second.text = viewModel.displayOf(key)
            pair.second.horizontalAlignment = if (viewModel.valueOf(key) is Number) HorizontalAlignment.RIGHT else HorizontalAlignment.LEFT
            val selected = key == viewModel.selected
            pair.first.borderColor = if (selected) SELECTION else GRID_LINE
            pair.first.borderThickness = if (selected) {
                SELECTION_THICKNESS
            } else if (showGridlines) {
                GRID_THICKNESS
            } else {
                0.0
            }
        }
        if (nameBox.text != viewModel.selected) nameBox.text = viewModel.selected
        formula.text = viewModel.formulaOf(viewModel.selected)
        chart.refresh()
    }
}

/** A sample chart (columns of the per-quarter totals). Shown only while [Chart Tools] is visible (while the chart is selected). */
private class SampleChart(private val viewModel: ExcelViewModel) {
    private val bars = WGrid(columnSpacing = BAR_GAP)
    private val border = WBorder(WPanel(spacing = 8.0))
    private val quarters = viewModel.columns.subList(1, QUARTERS + 1)
    private val columns = quarters.map { WBorder(WLabel("")) }

    /** The chart view. */
    val view: WComponent = border

    init {
        val panel = border.child as WPanel
        panel.add(WLabel("Total by Quarter").also { it.fontWeight = SEMI_BOLD })
        panel.add(bars)
        border.background = WColor.WHITE
        border.borderColor = SELECTION
        border.borderThickness = SELECTION_THICKNESS
        border.padding = CHART_PADDING
        border.verticalAlignment = VerticalAlignment.TOP
        border.isVisible = viewModel.chartTools.isVisible
        viewModel.chartTools.addPropertyChangeListener { event -> if (event.propertyName == "isVisible") border.isVisible = viewModel.chartTools.isVisible }
        bars.addRow(GridLength.pixel(CHART_HEIGHT))
        bars.addRow(GridLength.AUTO)
        quarters.forEachIndexed { index, column ->
            bars.addColumn(GridLength.star())
            columns[index].background = SELECTION
            columns[index].verticalAlignment = VerticalAlignment.BOTTOM
            bars.add(columns[index], row = 0, column = index)
            bars.add(WLabel(viewModel.displayOf("${column}1")).also { it.horizontalAlignment = HorizontalAlignment.CENTER }, row = 1, column = index)
        }
        refresh()
    }

    /** Adjusts the bar heights to the totals. */
    fun refresh() {
        val totals = quarters.map { viewModel.seriesOf(it).sum() }
        val max = totals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        columns.forEachIndexed { index, bar -> bar.height = CHART_HEIGHT * totals[index] / max }
    }
}

private fun header(text: String): WComponent {
    val border = WBorder(
        WLabel(text).also {
            it.horizontalAlignment = HorizontalAlignment.CENTER
            it.verticalAlignment = VerticalAlignment.CENTER
        },
    )
    border.background = HEADER_BACKGROUND
    border.borderColor = GRID_LINE
    border.borderThickness = GRID_THICKNESS
    return border
}

/** The status bar (Ready / last command, and average, count, sum, and zoom). */
private fun statusBar(viewModel: ExcelViewModel, ribbon: WRibbon): WComponent {
    val model = RibbonStatusBarModel()
    val ready = RibbonLabelModel("status.ready", "Ready")
    model.items.add(ready)
    val average = RibbonLabelModel("status.average", "")
    val count = RibbonLabelModel("status.count", "")
    val sum = RibbonLabelModel("status.sum", "")
    model.endItems.add(average)
    model.endItems.add(count)
    model.endItems.add(sum)
    model.endItems.add(RibbonZoomModel("status.zoom", ZOOM))
    fun refresh() {
        val (avg, n, total) = viewModel.statistics()
        ready.label = viewModel.lastCommand
        average.label = "Average: ${"%,.0f".format(avg)}"
        count.label = "Count: $n"
        sum.label = "Sum: ${"%,.0f".format(total)}"
    }
    viewModel.addChangeListener { refresh() }
    refresh()
    val bar = WRibbonStatusBar(model)
    bar.ribbon = ribbon
    return bar
}

private const val ZOOM = 100.0
private const val QUARTERS = 4
private const val CHART_GAP = 8.0
private const val CHART_COLUMN = 7
private const val CHART_ROWS = 9
private const val CHART_COLUMNS = 4
private const val CHART_PADDING = 12.0
private const val CHART_HEIGHT = 160.0
private const val BAR_GAP = 12.0
private const val SEMI_BOLD = 600
private const val NAME_BOX_WIDTH = 90.0
private const val FX_SIZE = 14.0
private const val BAR_MARGIN = 6.0
private const val ROW_HEADER_WIDTH = 44.0
private const val CELL_WIDTH = 110.0
private const val CELL_HEIGHT = 28.0
private const val CELL_PADDING = 6.0
private const val GRID_THICKNESS = 0.5
private const val SELECTION_THICKNESS = 2.0
private val GRID_LINE = WColor(0xD4, 0xD4, 0xD4)
private val SELECTION = WColor(0x10, 0x7C, 0x41)
private val HEADER_BACKGROUND = WColor(0xF3, 0xF3, 0xF3)
