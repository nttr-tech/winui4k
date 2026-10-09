package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonFontComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonItemTextProvider
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.dropDown
import com.appkitbox.winui4k.sample.ribbon.shell.group
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.tab
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/**
 * Excel-style ribbon demo (same layout as excel.png in the RibbonSpace README; the ribbon is 100% MVVM: all content is in
 * [RibbonModel], and commands are referenced by id from the command catalog of [ExcelViewModel]). Formula bar, sheet, and
 * a status bar with summary values.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.EXCEL)
        val viewModel = ExcelViewModel()
        val model = createExcelModel(viewModel)
        val ribbon = WRibbon(model)
        ribbon.addItemInvokedListener { event -> if (event.commandId == null) viewModel.record(event.item.label ?: event.item.id.orEmpty()) }
        val window = RibbonDemoWindow("Budget 2026 - Excel (WinUI4K Ribbon)", RibbonDemoApp.EXCEL, ribbon)
        window.titleBar!!.subtitle = "• MVVM ribbon"
        window.setTop(ribbon)
        window.setContent(sheet(viewModel))
        window.setStatusBar(statusBar(viewModel, ribbon))
        window.show()
    }
}

/** The Excel ribbon model. Commands are bound by their id in the view model's catalog ([com.appkitbox.winui4k.ribbon.RibbonItemModel.commandId]). */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declaratively builds Excel's [Home] tab from top to bottom
private fun createExcelModel(viewModel: ExcelViewModel): RibbonModel = RibbonModel().apply {
    commandCatalog = viewModel.catalog
    applicationButtonLabel = "File"
    title = "Budget 2026"
    fun commandButton(commandId: String, size: RibbonItemSize = RibbonItemSize.SMALL) =
        RibbonButtonModel(commandId, viewModel.catalog.find(commandId)!!.label, viewModel.catalog.find(commandId)!!.icon).also {
            it.commandId = commandId
            it.size = size
        }
    val home = tab(
        "home", "Home", "H",
        group(
            "clipboard",
            "Clipboard",
            RibbonSplitButtonModel("paste", "Paste").also {
                it.icon = RibbonIcons.PASTE
                it.size = RibbonItemSize.LARGE
                it.menuItems.add(menuItem("paste.values", "Paste Values"))
            },
            button("cut", "Cut", RibbonIcons.CUT).also { it.shortcut = "Ctrl+X" },
            button("copy", "Copy", RibbonIcons.COPY).also { it.shortcut = "Ctrl+C" },
            button("formatPainter", "Format Painter", RibbonIcons.FORMAT_PAINTER),
        ).also { it.isDialogLauncherVisible = true },
        group(
            "font",
            "Font",
            RibbonRowModel(
                RibbonFontComboBoxModel("font.name").also {
                    it.selectedItem = "Aptos Narrow"
                    it.items.add(0, "Aptos Narrow")
                },
                RibbonFontSizeComboBoxModel("font.size").also { it.selectedItem = 11.0 },
                RibbonButtonGroupModel(
                    button("font.grow", "Increase Font Size", RibbonIcons.FONT_INCREASE, RibbonItemSize.SMALL),
                    button("font.shrink", "Decrease Font Size", RibbonIcons.FONT_DECREASE, RibbonItemSize.SMALL),
                ),
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("bold", "Bold", RibbonIcons.BOLD).also { it.shortcut = "Ctrl+B" },
                    toggle("italic", "Italic", RibbonIcons.ITALIC).also { it.shortcut = "Ctrl+I" },
                    toggle("underline", "Underline", RibbonIcons.UNDERLINE).also { it.shortcut = "Ctrl+U" },
                ),
                dropDown("borders", "Borders", RibbonIcons.BORDERS, RibbonItemSize.SMALL, menuItem("borders.bottom", "Bottom Border"), menuItem("borders.all", "All Borders")),
                RibbonColorPickerModel("fill", "Fill Color", RibbonIcons.FILL).also { it.showNoColor = true },
                RibbonColorPickerModel("fontColor", "Font Color", RibbonIcons.FONT_COLOR),
            ),
        ).also {
            it.itemsLayout = RibbonGroupItemsLayout.ROWS
            it.rowCount = 2
            it.isDialogLauncherVisible = true
        },
        group(
            "alignment",
            "Alignment",
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("align.left", "Align Left", RibbonIcons.ALIGN_LEFT).also {
                        it.groupName = "align"
                        it.isChecked = true
                    },
                    toggle("align.center", "Center", RibbonIcons.ALIGN_CENTER).also { it.groupName = "align" },
                    toggle("align.right", "Align Right", RibbonIcons.ALIGN_RIGHT).also { it.groupName = "align" },
                ),
            ),
            RibbonRowModel(
                toggle("wrap", "Wrap Text", RibbonIcons.REFRESH),
                dropDown("orientation", "Orientation", RibbonIcons.ROTATE, RibbonItemSize.SMALL, menuItem("orientation.ccw", "Angle Counterclockwise")),
            ),
        ).also {
            it.itemsLayout = RibbonGroupItemsLayout.ROWS
            it.rowCount = 2
            it.isDialogLauncherVisible = true
        },
        group(
            "number",
            "Number",
            RibbonComboBoxModel("number.format", null, NumberFormat.entries.toList()).also { combo ->
                combo.commandId = "number.formatList"
                combo.selectedItem = NumberFormat.GENERAL
                combo.inputWidth = 120.0
                combo.itemTextProvider = RibbonItemTextProvider { (it as? NumberFormat)?.label ?: it.toString() }
            },
            RibbonRowModel(
                commandButton("number.currency"),
                commandButton("number.percent"),
                commandButton("number.comma"),
                commandButton("number.increase"),
                commandButton("number.decrease"),
            ),
        ).also {
            it.itemsLayout = RibbonGroupItemsLayout.ROWS
            it.rowCount = 2
            it.isDialogLauncherVisible = true
        },
        group(
            "styles",
            "Styles",
            dropDown("conditional", "Conditional Formatting", RibbonIcons.FILTER, RibbonItemSize.MEDIUM, menuItem("conditional.highlight", "Highlight Cells Rules"), menuItem("conditional.bars", "Data Bars")),
            RibbonGalleryModel("cellStyles", "Cell Styles").also { gallery ->
                gallery.rows = 2
                gallery.maxColumns = 2
                gallery.minColumns = 2
                gallery.itemWidth = 86.0
                gallery.itemHeight = 30.0
                gallery.showLabels = false
                listOf(
                    Triple("Normal", "#FFFFFF", "#242424"),
                    Triple("Bad", "#FFC7CE", "#9C0006"),
                    Triple("Good", "#C6EFCE", "#006100"),
                    Triple("Neutral", "#FFEB9C", "#9C5700"),
                ).forEach { (name, background, foreground) ->
                    gallery.items.add(
                        RibbonGalleryItemModel("style.$name", name).also {
                            it.previewText = name
                            it.previewFontSize = 13.0
                            it.previewBackground = com.appkitbox.winui4k.ribbon.RibbonColor.parse(background)
                            it.previewForeground = com.appkitbox.winui4k.ribbon.RibbonColor.parse(foreground)
                        },
                    )
                }
            },
        ),
        group(
            "cells",
            "Cells",
            dropDown("cells.insert", "Insert", RibbonIcons.ADD, RibbonItemSize.SMALL, menuItem("cells.insertRows", "Insert Sheet Rows")),
            dropDown("cells.delete", "Delete", RibbonIcons.REMOVE, RibbonItemSize.SMALL, menuItem("cells.deleteRows", "Delete Sheet Rows")),
            dropDown("cells.format", "Format", RibbonIcons.GRID, RibbonItemSize.SMALL, menuItem("cells.height", "Row Height...")),
        ),
        group(
            "editing",
            "Editing",
            commandButton("editing.autoSum"),
            dropDown("fill", "Fill", RibbonIcons.CHEVRON_DOWN, RibbonItemSize.SMALL, menuItem("fill.down", "Down")),
            commandButton("editing.clear"),
            dropDown("sort", "Sort & Filter", RibbonIcons.SORT, RibbonItemSize.MEDIUM, menuItem("sort.az", "Sort A to Z", RibbonIcons.SORT_ASCENDING), menuItem("sort.za", "Sort Z to A", RibbonIcons.SORT_DESCENDING)),
            dropDown("findSelect", "Find & Select", RibbonIcons.FIND, RibbonItemSize.MEDIUM, menuItem("find.find", "Find..."), menuItem("find.replace", "Replace...")),
        ),
    )
    tabs.add(home)
    tabs.add(tab("insert", "Insert", "N", group("charts", "Charts", button("insert.chart", "Recommended Charts", RibbonIcons.CHART, RibbonItemSize.LARGE), button("insert.pivot", "PivotTable", RibbonIcons.PIVOT, RibbonItemSize.LARGE))))
    tabs.add(tab("formulas", "Formulas", "M", group("library", "Function Library", button("formulas.insert", "Insert Function", RibbonIcons.FUNCTION, RibbonItemSize.LARGE), commandButton("editing.autoSum", RibbonItemSize.LARGE))))
    tabs.add(tab("data", "Data", "A", group("sortFilter", "Sort & Filter", button("data.sortAsc", "Sort A to Z", RibbonIcons.SORT_ASCENDING), button("data.sortDesc", "Sort Z to A", RibbonIcons.SORT_DESCENDING), toggle("data.filter", "Filter", RibbonIcons.FILTER, RibbonItemSize.LARGE))))
    tabs.add(tab("view", "View", "W", group("show", "Show", toggle("view.gridlines", "Gridlines", RibbonIcons.GRIDLINES).also { it.isChecked = true }, button("view.freeze", "Freeze Panes", RibbonIcons.FREEZE, RibbonItemSize.LARGE))))
    quickAccessItems.add(button("qat.save", "Save", RibbonIcons.SAVE))
    quickAccessItems.add(button("qat.undo", "Undo", RibbonIcons.UNDO))
    quickAccessItems.add(button("qat.redo", "Redo", RibbonIcons.REDO))
    tabStripItems.add(button("comments", "Comments", RibbonIcons.COMMENT))
    tabStripItems.add(button("share", "Share", RibbonIcons.SHARE))
}

/** The formula bar and the sheet (the selected cell has a green border). */
private fun sheet(viewModel: ExcelViewModel): WComponent {
    val nameBox = WTextField().also {
        it.width = NAME_BOX_WIDTH
        it.text = viewModel.selected
    }
    val formula = WTextField()
    val bar = WGrid()
    bar.columnSpacing = 8.0
    bar.addColumn(GridLength.AUTO)
    bar.addColumn(GridLength.AUTO)
    bar.addColumn(GridLength.star())
    bar.add(nameBox, row = 0, column = 0)
    bar.add(WRibbonTheme.label("fx", "RibbonSecondaryForegroundBrush", FX_SIZE).also { it.verticalAlignment = VerticalAlignment.CENTER }, row = 0, column = 1)
    bar.add(formula, row = 0, column = 2)
    bar.margin = BAR_MARGIN

    val grid = WGrid()
    grid.addColumn(GridLength.pixel(ROW_HEADER_WIDTH))
    viewModel.columns.forEach { _ -> grid.addColumn(GridLength.pixel(CELL_WIDTH)) }
    repeat(viewModel.rowCount + 1) { grid.addRow(GridLength.pixel(CELL_HEIGHT)) }
    val cells = HashMap<String, Pair<WBorder, WLabel>>()
    viewModel.columns.forEachIndexed { c, name -> grid.add(header(name), row = 0, column = c + 1) }
    for (r in 1..viewModel.rowCount) {
        grid.add(header(r.toString()), row = r, column = 0)
        viewModel.columns.forEachIndexed { c, name ->
            val label = WLabel("").also {
                it.verticalAlignment = VerticalAlignment.CENTER
                it.margin = CELL_PADDING
            }
            val cell = WBorder(label)
            cell.borderColor = GRID_LINE
            cell.borderThickness = GRID_THICKNESS
            grid.add(cell, row = r, column = c + 1)
            cells["$name$r"] = cell to label
        }
    }
    fun refresh() {
        for ((key, pair) in cells) {
            pair.second.text = viewModel.displayOf(key)
            pair.second.horizontalAlignment = if (viewModel.valueOf(key) is Number) HorizontalAlignment.RIGHT else HorizontalAlignment.LEFT
            pair.first.borderColor = if (key == viewModel.selected) SELECTION else GRID_LINE
            pair.first.borderThickness = if (key == viewModel.selected) SELECTION_THICKNESS else GRID_THICKNESS
        }
        if (nameBox.text != viewModel.selected) nameBox.text = viewModel.selected
        formula.text = viewModel.formulaOf(viewModel.selected)
    }
    nameBox.addTextChangedListener { text ->
        val reference = text.trim().uppercase()
        if (reference in cells && reference != viewModel.selected) viewModel.selected = reference
    }
    viewModel.addChangeListener { refresh() }
    refresh()

    val sheet = WGrid()
    sheet.addRow(GridLength.AUTO)
    sheet.addRow(GridLength.star())
    sheet.add(bar, row = 0, column = 0)
    sheet.add(WScrollPane(grid), row = 1, column = 0)
    return sheet
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
