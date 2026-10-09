package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel
import com.appkitbox.winui4k.ribbon.RibbonBackstagePlacement
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonGridPickerModel
import com.appkitbox.winui4k.ribbon.RibbonGridSize
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonItemTextProvider
import com.appkitbox.winui4k.ribbon.RibbonMenuHeaderModel
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonRelayCommand
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonSizeDefinition
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel

// The Excel ribbon model (same layout as ExcelViewModel.BuildRibbon in the RibbonSpace Excel demo). Everything is in the
// model, and commands are referenced by their id in the view model's catalog (commandId)

private val LARGE = RibbonItemSize.LARGE
private val MEDIUM = RibbonItemSize.MEDIUM
private val SMALL = RibbonItemSize.SMALL

private fun button(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize = MEDIUM, commandId: String? = null) =
    RibbonButtonModel(id, label, icon).also {
        it.size = size
        it.commandId = commandId
    }

private fun dropDown(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize, vararg entries: String) =
    RibbonDropDownButtonModel(id, label).also { d ->
        d.icon = icon
        d.size = size
        entries.forEachIndexed { index, entry -> d.menuItems.add(RibbonMenuItemModel("$id.$index", entry)) }
    }

@Suppress("LongParameterList") // Lists the split button's attributes (id, label, icon, size, command, menu) on one line
private fun split(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize, commandId: String?, vararg entries: String) =
    RibbonSplitButtonModel(id, label).also { s ->
        s.icon = icon
        s.size = size
        s.commandId = commandId
        entries.forEachIndexed { index, entry -> s.menuItems.add(RibbonMenuItemModel("$id.$index", entry)) }
    }

private fun menu(id: String, label: String, icon: RibbonIcon?, commandId: String) = RibbonMenuItemModel(id, label, icon).also { it.commandId = commandId }

/** The Excel ribbon model. */
internal fun createExcelModel(viewModel: ExcelViewModel): RibbonModel = RibbonModel().apply {
    commandCatalog = viewModel.catalog
    applicationButtonLabel = "File"
    title = "Budget 2026.xlsx"
    contextualGroups.add(viewModel.chartTools)
    tabs.add(createHomeTab())
    tabs.add(createInsertTab(viewModel))
    tabs.add(createFormulasTab())
    tabs.add(createDataTab())
    tabs.add(createViewTab())
    tabs.add(createChartDesignTab())
    quickAccessItems.add(button("save", "Save", RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("undo", "Undo", RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("redo", "Redo", RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    quickAccessCandidates.add(findItem("paste")!!)
    quickAccessCandidates.add(findItem("bold")!!)
    tabStripItems.add(button("comments", "Comments", RibbonIcons.COMMENT).also { it.showLabelInSimplified = true })
    tabStripItems.add(split("share", "Share", RibbonIcons.SHARE, SMALL, null, "Share", "Copy Link").also { it.showLabelInSimplified = true })
    backstage.title = "Excel"
    backstage.items.add(RibbonBackstageItemModel("home", "Home", RibbonIcons.HOME, WLabel("Create a new workbook or open a recent one.")))
    backstage.items.add(RibbonBackstageItemModel("new", "New", RibbonIcons.NEW, WLabel("Blank workbook · Budget · Invoice · Calendar")))
    backstage.items.add(RibbonBackstageItemModel("open", "Open", RibbonIcons.OPEN, WLabel("Recent workbooks")))
    backstage.items.add(
        RibbonBackstageItemModel("save", "Save", RibbonIcons.SAVE).also {
            it.command = RibbonRelayCommand.of({ viewModel.record("Saved from Backstage") })
            it.hasSeparatorBefore = true
        },
    )
    backstage.items.add(RibbonBackstageItemModel("export", "Export", RibbonIcons.EXPORT, WLabel("Create a PDF or change the file type.")))
    backstage.items.add(RibbonBackstageItemModel("options", "Options", RibbonIcons.SETTINGS, WLabel("Excel Options")).also { it.placement = RibbonBackstagePlacement.BOTTOM })
}

private fun createHomeTab(): RibbonTabModel = RibbonTabModel("home", "Home").also { home ->
    home.keyTip = "H"
    home.groups.add(clipboardGroup())
    home.groups.add(fontGroup())
    home.groups.add(alignmentGroup())
    home.groups.add(numberGroup())
    home.groups.add(stylesGroup())
    home.groups.add(
        RibbonGroupModel("cells", "Cells", RibbonIcons.TABLE).also { cells ->
            cells.items.add(split("insertCells", "Insert", RibbonIcons.INSERT_ROW, MEDIUM, "insertCells", "Insert Cells...", "Insert Sheet Rows", "Insert Sheet Columns"))
            cells.items.add(split("deleteCells", "Delete", RibbonIcons.DELETE_ROW, MEDIUM, "deleteCells", "Delete Cells...", "Delete Sheet Rows", "Delete Sheet Columns"))
            cells.items.add(dropDown("formatCells", "Format", RibbonIcons.TABLE, MEDIUM, "Row Height...", "Column Width...", "Format Cells..."))
        },
    )
    home.groups.add(
        RibbonGroupModel("editing", "Editing", RibbonIcons.SUM).also { editing ->
            editing.items.add(split("autoSum", "AutoSum", RibbonIcons.SUM, MEDIUM, "autoSum", "Sum", "Average", "Count Numbers", "Max", "Min"))
            editing.items.add(dropDown("fill", "Fill", RibbonIcons.CHEVRON_DOWN, MEDIUM, "Down", "Right", "Series..."))
            editing.items.add(
                RibbonDropDownButtonModel("clearMenu", "Clear").also { clear ->
                    clear.icon = RibbonIcons.CLEAR
                    clear.size = MEDIUM
                    clear.menuItems.add(menu("clear.all", "Clear All", null, "clear"))
                    clear.menuItems.add(RibbonMenuItemModel("clear.formats", "Clear Formats"))
                },
            )
            editing.items.add(dropDown("sortFilterMenu", "Sort & Filter", RibbonIcons.FILTER, LARGE, "Sort A to Z", "Sort Z to A", "Custom Sort...", "Filter"))
            editing.items.add(dropDown("findSelectMenu", "Find & Select", RibbonIcons.FIND, LARGE, "Find...", "Replace...", "Go To..."))
        },
    )
}

private fun clipboardGroup(): RibbonGroupModel = RibbonGroupModel("clipboard", "Clipboard", RibbonIcons.PASTE).also { clipboard ->
    clipboard.dialogLauncherCommandId = "formatCellsLauncher"
    clipboard.reductionOrder = -1
    clipboard.items.add(
        RibbonSplitButtonModel("paste", "Paste").also { paste ->
            paste.icon = RibbonIcons.PASTE
            paste.commandId = "paste"
            paste.sizeDefinition = RibbonSizeDefinition.ALWAYS_LARGE
            paste.keyTip = "V"
            paste.shortcut = "Ctrl+V"
            paste.menuItems.add(RibbonMenuHeaderModel("Paste"))
            paste.menuItems.add(menu("pasteValues", "Values", RibbonIcons.PASTE, "paste"))
            paste.menuItems.add(menu("pasteFormulas", "Formulas", RibbonIcons.FUNCTION, "paste"))
            paste.menuItems.add(menu("pasteTranspose", "Transpose", RibbonIcons.ROTATE, "paste"))
            paste.menuItems.add(RibbonMenuSeparatorModel())
            paste.menuItems.add(menu("pasteSpecial", "Paste Special...", null, "paste"))
        },
    )
    clipboard.items.add(button("cut", "Cut", RibbonIcons.CUT, commandId = "cut").also { it.keyTip = "X" })
    clipboard.items.add(button("copy", "Copy", RibbonIcons.COPY, commandId = "copy").also { it.keyTip = "C" })
    clipboard.items.add(button("formatPainter", "Format Painter", RibbonIcons.FORMAT_PAINTER, commandId = "formatPainter").also { it.keyTip = "FP" })
}

@Suppress("LongMethod") // Builds the two rows of the [Font] group declaratively, from top to bottom
private fun fontGroup(): RibbonGroupModel = RibbonGroupModel("font", "Font", RibbonIcons.FONT).also { font ->
    font.itemsLayout = RibbonGroupItemsLayout.ROWS
    font.rowCount = 2
    font.dialogLauncherCommandId = "formatCellsLauncher"
    val family = RibbonComboBoxModel("fontFamily", "Font", FONT_FAMILIES).also {
        it.text = "Aptos Narrow"
        it.isEditable = true
        it.inputWidth = FONT_WIDTH
        it.previewFontFamily = true
        it.keyTip = "FF"
    }
    val size = RibbonComboBoxModel("fontSize", "Font Size", FONT_SIZES).also {
        it.text = "11"
        it.isEditable = true
        it.inputWidth = FONT_SIZE_WIDTH
        it.keyTip = "FS"
        it.itemTextProvider = RibbonItemTextProvider { value -> (value as Double).toInt().toString() }
    }
    font.items.add(
        RibbonRowModel(
            family,
            size,
            RibbonButtonGroupModel(
                button("growFont", "Increase Font Size", RibbonIcons.FONT_INCREASE, SMALL),
                button("shrinkFont", "Decrease Font Size", RibbonIcons.FONT_DECREASE, SMALL),
            ),
        ),
    )
    val borders = RibbonSplitButtonModel("borders", "Borders").also { b ->
        b.icon = RibbonIcons.BORDERS
        b.size = SMALL
        b.keyTip = "B"
        listOf("Bottom Border", "Top Border", "Left Border", "Right Border", "No Border", "All Borders", "Outside Borders", "Thick Outside Borders").forEachIndexed { index, label ->
            b.menuItems.add(RibbonMenuItemModel("border.$index", label, RibbonIcons.BORDERS))
        }
    }
    font.items.add(
        RibbonRowModel(
            RibbonButtonGroupModel(
                toggle("bold", "Bold", RibbonIcons.BOLD, "Ctrl+B", "1"),
                toggle("italic", "Italic", RibbonIcons.ITALIC, "Ctrl+I", "2"),
                toggle("underline", "Underline", RibbonIcons.UNDERLINE, "Ctrl+U", "3"),
            ),
            borders,
            RibbonColorPickerModel("fillColor", "Fill Color", RibbonIcons.FILL).also {
                it.selectedColor = RibbonColor.parse("#FFFF00")
                it.showNoColor = true
                it.showAutomatic = false
                it.keyTip = "H"
            },
            RibbonColorPickerModel("fontColor", "Font Color", RibbonIcons.FONT_COLOR).also {
                it.selectedColor = RibbonColor.parse("#C00000")
                it.keyTip = "FC"
            },
        ),
    )
}

private fun toggle(id: String, label: String, icon: RibbonIcon, shortcut: String, keyTip: String) = RibbonToggleButtonModel(id, label).also {
    it.icon = icon
    it.size = SMALL
    it.shortcut = shortcut
    it.keyTip = keyTip
}

private fun alignmentGroup(): RibbonGroupModel = RibbonGroupModel("alignment", "Alignment", RibbonIcons.ALIGN_CENTER).also { alignment ->
    alignment.dialogLauncherCommandId = "formatCellsLauncher"
    fun align(id: String, label: String, icon: RibbonIcon) = RibbonToggleButtonModel(id, label).also {
        it.icon = icon
        it.size = SMALL
        it.groupName = "halign"
    }
    alignment.items.add(
        RibbonButtonGroupModel(
            align("alignLeft", "Align Left", RibbonIcons.ALIGN_LEFT).also { it.isChecked = true },
            align("alignCenter", "Center", RibbonIcons.ALIGN_CENTER),
            align("alignRight", "Align Right", RibbonIcons.ALIGN_RIGHT),
        ),
    )
    alignment.items.add(
        RibbonToggleButtonModel("wrapText", "Wrap Text").also {
            it.icon = RibbonIcons.REFRESH
            it.size = MEDIUM
            it.commandId = "wrapText"
        },
    )
    alignment.items.add(split("mergeCenter", "Merge & Center", RibbonIcons.MERGE, MEDIUM, "mergeCenter", "Merge Across", "Merge Cells", "Unmerge Cells"))
}

private fun numberGroup(): RibbonGroupModel = RibbonGroupModel("number", "Number", RibbonIcons.CALCULATOR).also { number ->
    number.itemsLayout = RibbonGroupItemsLayout.ROWS
    number.rowCount = 2
    number.dialogLauncherCommandId = "formatCellsLauncher"
    number.items.add(
        RibbonRowModel(
            RibbonComboBoxModel("numberFormat", "Number Format", NumberFormat.entries.toList()).also {
                it.commandId = "numberFormat"
                it.selectedItem = NumberFormat.GENERAL
                it.text = NumberFormat.GENERAL.label
                it.inputWidth = NUMBER_FORMAT_WIDTH
                it.keyTip = "N"
            },
        ),
    )
    // As in the RibbonSpace demo, use symbol characters as icons ($ is replaced with ¥ to match the Japanese currency symbol)
    number.items.add(
        RibbonRowModel(
            RibbonButtonGroupModel(
                button("accountingFormat", "Accounting Number Format", RibbonIcon.text("¥"), SMALL, "accounting"),
                button("percentStyle", "Percent Style", RibbonIcon.text("%"), SMALL, "percent"),
                button("commaStyle", "Comma Style", RibbonIcon.text(","), SMALL, "comma"),
                button("increaseDecimalButton", "Increase Decimal", RibbonIcon.text(".0"), SMALL, "increaseDecimal"),
                button("decreaseDecimalButton", "Decrease Decimal", RibbonIcon.text(".00"), SMALL, "decreaseDecimal"),
            ),
        ),
    )
}

private fun stylesGroup(): RibbonGroupModel = RibbonGroupModel("styles", "Styles", RibbonIcons.PALETTE).also { styles ->
    styles.reductionOrder = 1
    val conditional = RibbonDropDownButtonModel("conditionalFormatting", "Conditional Formatting").also { c ->
        c.icon = RibbonIcons.FILTER
        c.size = LARGE
        c.keyTip = "L"
        for ((index, entry) in listOf("Highlight Cells Rules", "Top/Bottom Rules", "Data Bars", "Color Scales", "Icon Sets").withIndex()) {
            val sub = RibbonMenuItemModel("cf.$index", entry)
            sub.items.add(RibbonMenuItemModel("cf.$index.1", "Greater Than..."))
            sub.items.add(RibbonMenuItemModel("cf.$index.2", "Less Than..."))
            sub.items.add(RibbonMenuItemModel("cf.$index.3", "Between..."))
            c.menuItems.add(sub)
        }
        c.menuItems.add(RibbonMenuSeparatorModel())
        c.menuItems.add(RibbonMenuItemModel("cf.new", "New Rule..."))
        c.menuItems.add(RibbonMenuItemModel("cf.manage", "Manage Rules..."))
    }
    styles.items.add(conditional)
    styles.items.add(
        RibbonGalleryModel("cellStyles", "Cell Styles").also { gallery ->
            gallery.icon = RibbonIcons.PALETTE
            gallery.maxColumns = CELL_STYLE_MAX_COLUMNS
            gallery.minColumns = 2
            gallery.itemWidth = CELL_STYLE_WIDTH
            gallery.itemHeight = CELL_STYLE_HEIGHT
            gallery.showLabels = false
            gallery.dropDownColumns = CELL_STYLE_DROP_DOWN_COLUMNS
            gallery.rows = 2
            for ((label, category, colors) in CELL_STYLES) {
                gallery.items.add(
                    RibbonGalleryItemModel("cs.$label", label, category = category).also {
                        it.previewText = label
                        it.previewFontSize = CELL_STYLE_FONT_SIZE
                        it.previewBackground = RibbonColor.parse(colors.first)
                        it.previewForeground = RibbonColor.parse(colors.second)
                    },
                )
            }
            gallery.menuItems.add(RibbonMenuItemModel("cs.new", "New Cell Style...", RibbonIcons.ADD))
            gallery.menuItems.add(RibbonMenuItemModel("cs.merge", "Merge Styles...", RibbonIcons.MERGE))
        },
    )
}

private fun createInsertTab(viewModel: ExcelViewModel): RibbonTabModel = RibbonTabModel("insert", "Insert").also { insert ->
    insert.keyTip = "N"
    insert.groups.add(
        RibbonGroupModel("tables", "Tables", RibbonIcons.TABLE).also { tables ->
            tables.items.add(split("pivotTable", "PivotTable", RibbonIcons.PIVOT, LARGE, "pivotTable", "From Table/Range", "From External Data Source"))
            tables.items.add(button("recommendedPivot", "Recommended PivotTables", RibbonIcons.PIVOT, LARGE))
            tables.items.add(
                RibbonDropDownButtonModel("table", "Table").also { table ->
                    table.icon = RibbonIcons.TABLE
                    table.size = LARGE
                    table.menuItems.add(
                        RibbonGridPickerModel(
                            "tablePicker",
                            "Insert Table",
                            RibbonRelayCommand({ size -> (size as? RibbonGridSize)?.let { viewModel.record("Inserted a $it table") } }),
                        ),
                    )
                },
            )
        },
    )
    insert.groups.add(
        RibbonGroupModel("charts", "Charts", RibbonIcons.CHART).also { charts ->
            charts.dialogLauncherCommandId = "recommendedCharts"
            charts.items.add(button("recommendedCharts", "Recommended Charts", RibbonIcons.CHART, LARGE, "recommendedCharts"))
            for ((id, label) in listOf("column" to "Column", "line" to "Line", "pie" to "Pie", "bar" to "Bar", "area" to "Area", "scatter" to "Scatter")) {
                charts.items.add(dropDown("chart.$id", label, RibbonIcons.CHART, SMALL, "2-D $label", "3-D $label"))
            }
            charts.items.add(RibbonButtonModel("selectChart", "Select Sample Chart", RibbonIcons.CHART, viewModel.toggleChartCommand).also { it.size = LARGE })
        },
    )
}

private fun createFormulasTab(): RibbonTabModel = RibbonTabModel("formulas", "Formulas").also { formulas ->
    formulas.keyTip = "M"
    formulas.groups.add(
        RibbonGroupModel("functionLibrary", "Function Library", RibbonIcons.FUNCTION).also { library ->
            library.items.add(button("insertFunction", "Insert Function", RibbonIcons.FUNCTION, LARGE, "insertFunction"))
            val categories = listOf("AutoSum", "Recently Used", "Financial", "Logical", "Text", "Date & Time", "Lookup & Reference", "Math & Trig")
            categories.forEachIndexed { index, label -> library.items.add(dropDown("fx.$index", label, RibbonIcons.FUNCTION, LARGE, "$label Functions...")) }
        },
    )
    formulas.groups.add(
        RibbonGroupModel("calculation", "Calculation", RibbonIcons.CALCULATOR).also { calculation ->
            calculation.items.add(button("calculateNow", "Calculate Now", RibbonIcons.CALCULATOR, commandId = "calculateNow"))
            calculation.items.add(button("calculateSheet", "Calculate Sheet", RibbonIcons.CALCULATOR))
        },
    )
}

private fun createDataTab(): RibbonTabModel = RibbonTabModel("data", "Data").also { data ->
    data.keyTip = "A"
    data.groups.add(
        RibbonGroupModel("queries", "Queries & Connections", RibbonIcons.DATABASE).also {
            it.items.add(split("refreshAll", "Refresh All", RibbonIcons.REFRESH, LARGE, "refreshAll", "Refresh All", "Refresh", "Connection Properties..."))
        },
    )
    data.groups.add(
        RibbonGroupModel("sortFilterGroup", "Sort & Filter", RibbonIcons.FILTER).also { sort ->
            sort.items.add(button("sortAZ", "Sort A to Z", RibbonIcons.SORT_ASCENDING))
            sort.items.add(button("sortZA", "Sort Z to A", RibbonIcons.SORT_DESCENDING))
            sort.items.add(button("sortDialog", "Sort", RibbonIcons.SORT, LARGE))
            sort.items.add(
                RibbonToggleButtonModel("filter", "Filter").also {
                    it.icon = RibbonIcons.FILTER
                    it.size = LARGE
                    it.shortcut = "Ctrl+Shift+L"
                },
            )
        },
    )
}

private fun createViewTab(): RibbonTabModel = RibbonTabModel("view", "View").also { view ->
    view.keyTip = "W"
    view.groups.add(
        RibbonGroupModel("show", "Show", RibbonIcons.VIEW).also { show ->
            show.items.add(RibbonCheckBoxModel("gridlines", "Gridlines", true))
            show.items.add(RibbonCheckBoxModel("formulaBar", "Formula Bar", true))
            show.items.add(RibbonCheckBoxModel("headings", "Headings", true))
        },
    )
    view.groups.add(
        RibbonGroupModel("zoom", "Zoom", RibbonIcons.ZOOM).also { zoom ->
            zoom.items.add(button("zoomDialog", "Zoom", RibbonIcons.ZOOM, LARGE))
            zoom.items.add(button("zoom100", "100%", RibbonIcons.ZOOM, LARGE))
            zoom.items.add(
                RibbonSpinnerModel("zoomValue", "Zoom:", ZOOM_DEFAULT).also {
                    it.minimum = ZOOM_MIN
                    it.maximum = ZOOM_MAX
                    it.increment = ZOOM_STEP
                    it.unit = "%"
                    it.format = "0"
                },
            )
        },
    )
    view.groups.add(
        RibbonGroupModel("window", "Window", RibbonIcons.WINDOW).also { window ->
            window.items.add(button("newWindow", "New Window", RibbonIcons.WINDOW, LARGE, "newWindow"))
            window.items.add(dropDown("freezePanes", "Freeze Panes", RibbonIcons.FREEZE, LARGE, "Freeze Panes", "Freeze Top Row", "Freeze First Column"))
        },
    )
}

/** [Chart Design] under [Chart Tools]. */
private fun createChartDesignTab(): RibbonTabModel = RibbonTabModel("chartDesign", "Chart Design").also { design ->
    design.contextualGroupId = "chart"
    design.keyTip = "JC"
    design.groups.add(
        RibbonGroupModel("chartLayouts", "Chart Layouts", RibbonIcons.LAYOUT).also { layouts ->
            layouts.items.add(dropDown("addElement", "Add Chart Element", RibbonIcons.ADD, LARGE, "Axes", "Axis Titles", "Chart Title", "Data Labels", "Legend"))
            layouts.items.add(dropDown("quickLayout", "Quick Layout", RibbonIcons.LAYOUT, LARGE, "Layout 1", "Layout 2", "Layout 3"))
        },
    )
    design.groups.add(
        RibbonGroupModel("chartStylesGroup", "Chart Styles", RibbonIcons.PALETTE).also { styles ->
            styles.reductionOrder = 1
            styles.items.add(
                RibbonDropDownButtonModel("chartColors", "Change Colors").also { colors ->
                    colors.icon = RibbonIcons.PALETTE
                    colors.size = LARGE
                    colors.menuItems.add(menu("chartColors.colorful", "Colorful", null, "chartStyles"))
                    colors.menuItems.add(menu("chartColors.monochrome", "Monochromatic", null, "chartStyles"))
                },
            )
            styles.items.add(
                RibbonGalleryModel("chartStyleGallery", "Chart Styles").also { gallery ->
                    gallery.maxColumns = CHART_STYLE_MAX_COLUMNS
                    gallery.minColumns = CHART_STYLE_MIN_COLUMNS
                    gallery.itemWidth = CHART_STYLE_WIDTH
                    gallery.itemHeight = CHART_STYLE_HEIGHT
                    gallery.showLabels = false
                    for (i in 1..CHART_STYLES) gallery.items.add(RibbonGalleryItemModel("chartStyle$i", "Style $i", RibbonIcons.CHART))
                },
            )
        },
    )
    design.groups.add(
        RibbonGroupModel("chartData", "Data", RibbonIcons.DATABASE).also { data ->
            data.items.add(button("switchRowColumn", "Switch Row/Column", RibbonIcons.ROTATE, LARGE, "switchRowColumn"))
            data.items.add(button("selectData", "Select Data", RibbonIcons.TABLE, LARGE, "selectData"))
        },
    )
    design.groups.add(
        RibbonGroupModel("type", "Type", RibbonIcons.CHART).also {
            it.items.add(button("changeChartType", "Change Chart Type", RibbonIcons.CHART, LARGE, "changeChartType"))
        },
    )
}

private val FONT_FAMILIES = listOf("Aptos Narrow", "Arial", "Calibri", "Cambria", "Consolas", "Segoe UI", "Times New Roman", "Verdana", "Yu Gothic", "Meiryo")
private val FONT_SIZES = listOf(8.0, 9.0, 10.0, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0, 28.0, 36.0, 48.0, 72.0)

// Cell styles (label, category, background and text colors)
private val CELL_STYLES = listOf(
    Triple("Normal", "Good, Bad and Neutral", "#FFFFFF" to "#242424"),
    Triple("Bad", "Good, Bad and Neutral", "#FFC7CE" to "#9C0006"),
    Triple("Good", "Good, Bad and Neutral", "#C6EFCE" to "#006100"),
    Triple("Neutral", "Good, Bad and Neutral", "#FFEB9C" to "#9C5700"),
    Triple("Calculation", "Data and Model", "#F2F2F2" to "#FA7D00"),
    Triple("Check Cell", "Data and Model", "#A5A5A5" to "#FFFFFF"),
    Triple("Input", "Data and Model", "#FFCC99" to "#3F3F76"),
    Triple("Output", "Data and Model", "#F2F2F2" to "#3F3F3F"),
    Triple("Title", "Titles and Headings", "#FFFFFF" to "#44546A"),
    Triple("Heading 1", "Titles and Headings", "#FFFFFF" to "#44546A"),
    Triple("Heading 2", "Titles and Headings", "#FFFFFF" to "#44546A"),
    Triple("Total", "Titles and Headings", "#FFFFFF" to "#242424"),
)

private const val FONT_WIDTH = 128.0
private const val FONT_SIZE_WIDTH = 46.0
private const val NUMBER_FORMAT_WIDTH = 110.0
private const val CELL_STYLE_MAX_COLUMNS = 4
private const val CELL_STYLE_DROP_DOWN_COLUMNS = 6
private const val CELL_STYLE_WIDTH = 84.0
private const val CELL_STYLE_HEIGHT = 30.0
private const val CELL_STYLE_FONT_SIZE = 12.0
private const val CHART_STYLES = 14
private const val CHART_STYLE_MAX_COLUMNS = 6
private const val CHART_STYLE_MIN_COLUMNS = 3
private const val CHART_STYLE_WIDTH = 58.0
private const val CHART_STYLE_HEIGHT = 52.0
private const val ZOOM_DEFAULT = 100.0
private const val ZOOM_MIN = 10.0
private const val ZOOM_MAX = 400.0
private const val ZOOM_STEP = 10.0
