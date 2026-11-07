package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstageItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstagePlacement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDropDownButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemTextProvider
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuHeaderModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRelayCommand
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRowModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSizeDefinition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel

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
    applicationButtonLabel = Messages.get("common.file")
    title = Messages.get("title")
    contextualGroups.add(viewModel.chartTools)
    tabs.add(createHomeTab())
    tabs.add(createInsertTab(viewModel))
    tabs.add(createFormulasTab())
    tabs.add(createDataTab())
    tabs.add(createViewTab())
    tabs.add(createChartDesignTab())
    quickAccessItems.add(button("save", Messages.get("common.save"), RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("undo", Messages.get("common.undo"), RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("redo", Messages.get("common.redo"), RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    quickAccessCandidates.add(findItem("paste")!!)
    quickAccessCandidates.add(findItem("bold")!!)
    tabStripItems.add(button("comments", Messages.get("common.comments"), RibbonIcons.COMMENT).also { it.showLabelInSimplified = true })
    tabStripItems.add(split("share", Messages.get("common.share"), RibbonIcons.SHARE, SMALL, null, Messages.get("common.share"), Messages.get("common.share.copy.link")).also { it.showLabelInSimplified = true })
    backstage.title = "Excel"
    backstage.items.add(RibbonBackstageItemModel("home", Messages.get("common.home"), RibbonIcons.HOME, WLabel(Messages.get("backstage.home.text"))))
    backstage.items.add(RibbonBackstageItemModel("new", Messages.get("common.backstage.new"), RibbonIcons.NEW, WLabel(Messages.get("backstage.new.text"))))
    backstage.items.add(RibbonBackstageItemModel("open", Messages.get("common.backstage.open"), RibbonIcons.OPEN, WLabel(Messages.get("backstage.open.text"))))
    backstage.items.add(
        RibbonBackstageItemModel("save", Messages.get("common.save"), RibbonIcons.SAVE).also {
            it.command = RibbonRelayCommand.of({ viewModel.record(Messages.get("log.backstage.saved")) })
            it.hasSeparatorBefore = true
        },
    )
    backstage.items.add(RibbonBackstageItemModel("export", Messages.get("common.export"), RibbonIcons.EXPORT, WLabel(Messages.get("backstage.export.text"))))
    backstage.items.add(RibbonBackstageItemModel("options", Messages.get("common.options"), RibbonIcons.SETTINGS, WLabel(Messages.get("backstage.options.text"))).also { it.placement = RibbonBackstagePlacement.BOTTOM })
}

private fun createHomeTab(): RibbonTabModel = RibbonTabModel("home", Messages.get("common.home")).also { home ->
    home.keyTip = "H"
    home.groups.add(clipboardGroup())
    home.groups.add(fontGroup())
    home.groups.add(alignmentGroup())
    home.groups.add(numberGroup())
    home.groups.add(stylesGroup())
    home.groups.add(
        RibbonGroupModel("cells", Messages.get("home.cells"), RibbonIcons.TABLE).also { cells ->
            cells.items.add(split("insertCells", Messages.get("common.insert"), RibbonIcons.INSERT_ROW, MEDIUM, "insertCells", Messages.get("home.insert.cells"), Messages.get("home.insert.rows"), Messages.get("home.insert.columns")))
            cells.items.add(split("deleteCells", Messages.get("common.delete"), RibbonIcons.DELETE_ROW, MEDIUM, "deleteCells", Messages.get("home.delete.cells"), Messages.get("home.delete.rows"), Messages.get("home.delete.columns")))
            cells.items.add(dropDown("formatCells", Messages.get("home.format"), RibbonIcons.TABLE, MEDIUM, Messages.get("home.row.height"), Messages.get("home.column.width"), Messages.get("home.format.cells")))
        },
    )
    home.groups.add(
        RibbonGroupModel("editing", Messages.get("common.editing"), RibbonIcons.SUM).also { editing ->
            editing.items.add(split("autoSum", Messages.get("command.auto.sum"), RibbonIcons.SUM, MEDIUM, "autoSum", Messages.get("home.sum"), Messages.get("home.average"), Messages.get("home.count.numbers"), Messages.get("home.max"), Messages.get("home.min")))
            editing.items.add(dropDown("fill", Messages.get("command.fill"), RibbonIcons.CHEVRON_DOWN, MEDIUM, Messages.get("home.fill.down"), Messages.get("home.fill.right"), Messages.get("home.fill.series")))
            editing.items.add(
                RibbonDropDownButtonModel("clearMenu", Messages.get("command.clear")).also { clear ->
                    clear.icon = RibbonIcons.CLEAR
                    clear.size = MEDIUM
                    clear.menuItems.add(menu("clear.all", Messages.get("home.clear.all"), null, "clear"))
                    clear.menuItems.add(RibbonMenuItemModel("clear.formats", Messages.get("home.clear.formats")))
                },
            )
            editing.items.add(dropDown("sortFilterMenu", Messages.get("command.sort.filter"), RibbonIcons.FILTER, LARGE, Messages.get("home.sort.ascending"), Messages.get("home.sort.descending"), Messages.get("home.sort.custom"), Messages.get("home.filter")))
            editing.items.add(dropDown("findSelectMenu", Messages.get("command.find.select"), RibbonIcons.FIND, LARGE, Messages.get("home.find"), Messages.get("home.replace"), Messages.get("common.find.goto")))
        },
    )
}

private fun clipboardGroup(): RibbonGroupModel = RibbonGroupModel("clipboard", Messages.get("common.clipboard"), RibbonIcons.PASTE).also { clipboard ->
    clipboard.dialogLauncherCommandId = "formatCellsLauncher"
    clipboard.reductionOrder = -1
    clipboard.items.add(
        RibbonSplitButtonModel("paste", Messages.get("common.paste")).also { paste ->
            paste.icon = RibbonIcons.PASTE
            paste.commandId = "paste"
            paste.sizeDefinition = RibbonSizeDefinition.ALWAYS_LARGE
            paste.keyTip = "V"
            paste.shortcut = "Ctrl+V"
            paste.menuItems.add(RibbonMenuHeaderModel(Messages.get("common.paste")))
            paste.menuItems.add(menu("pasteValues", Messages.get("home.paste.values"), RibbonIcons.PASTE, "paste"))
            paste.menuItems.add(menu("pasteFormulas", Messages.get("home.paste.formulas"), RibbonIcons.FUNCTION, "paste"))
            paste.menuItems.add(menu("pasteTranspose", Messages.get("home.paste.transpose"), RibbonIcons.ROTATE, "paste"))
            paste.menuItems.add(RibbonMenuSeparatorModel())
            paste.menuItems.add(menu("pasteSpecial", Messages.get("common.paste.special"), null, "paste"))
        },
    )
    clipboard.items.add(button("cut", Messages.get("common.cut"), RibbonIcons.CUT, commandId = "cut").also { it.keyTip = "X" })
    clipboard.items.add(button("copy", Messages.get("common.copy"), RibbonIcons.COPY, commandId = "copy").also { it.keyTip = "C" })
    clipboard.items.add(button("formatPainter", Messages.get("common.format.painter"), RibbonIcons.FORMAT_PAINTER, commandId = "formatPainter").also { it.keyTip = "FP" })
}

@Suppress("LongMethod") // Builds the two rows of the [Font] group declaratively, from top to bottom
private fun fontGroup(): RibbonGroupModel = RibbonGroupModel("font", Messages.get("common.font"), RibbonIcons.FONT).also { font ->
    font.itemsLayout = RibbonGroupItemsLayout.ROWS
    font.rowCount = 2
    font.dialogLauncherCommandId = "formatCellsLauncher"
    val family = RibbonComboBoxModel("fontFamily", Messages.get("common.font"), FONT_FAMILIES).also {
        it.text = "Aptos Narrow"
        it.isEditable = true
        it.inputWidth = FONT_WIDTH
        it.previewFontFamily = true
        it.keyTip = "FF"
    }
    val size = RibbonComboBoxModel("fontSize", Messages.get("home.font.size"), FONT_SIZES).also {
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
                button("growFont", Messages.get("common.font.grow"), RibbonIcons.FONT_INCREASE, SMALL),
                button("shrinkFont", Messages.get("common.font.shrink"), RibbonIcons.FONT_DECREASE, SMALL),
            ),
        ),
    )
    val borders = RibbonSplitButtonModel("borders", Messages.get("common.borders")).also { b ->
        b.icon = RibbonIcons.BORDERS
        b.size = SMALL
        b.keyTip = "B"
        listOf(Messages.get("common.borders.bottom"), Messages.get("common.borders.top"), Messages.get("home.borders.left"), Messages.get("home.borders.right"), Messages.get("common.borders.none"), Messages.get("common.borders.all"), Messages.get("common.borders.outside"), Messages.get("home.borders.thick")).forEachIndexed { index, label ->
            b.menuItems.add(RibbonMenuItemModel("border.$index", label, RibbonIcons.BORDERS))
        }
    }
    font.items.add(
        RibbonRowModel(
            RibbonButtonGroupModel(
                toggle("bold", Messages.get("common.bold"), RibbonIcons.BOLD, "Ctrl+B", "1"),
                toggle("italic", Messages.get("common.italic"), RibbonIcons.ITALIC, "Ctrl+I", "2"),
                toggle("underline", Messages.get("common.underline"), RibbonIcons.UNDERLINE, "Ctrl+U", "3"),
            ),
            borders,
            RibbonColorPickerModel("fillColor", Messages.get("home.fill.color"), RibbonIcons.FILL).also {
                it.selectedColor = RibbonColor.parse("#FFFF00")
                it.showNoColor = true
                it.showAutomatic = false
                it.keyTip = "H"
            },
            RibbonColorPickerModel("fontColor", Messages.get("common.font.color"), RibbonIcons.FONT_COLOR).also {
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

private fun alignmentGroup(): RibbonGroupModel = RibbonGroupModel("alignment", Messages.get("home.alignment"), RibbonIcons.ALIGN_CENTER).also { alignment ->
    alignment.dialogLauncherCommandId = "formatCellsLauncher"
    fun align(id: String, label: String, icon: RibbonIcon) = RibbonToggleButtonModel(id, label).also {
        it.icon = icon
        it.size = SMALL
        it.groupName = "halign"
    }
    alignment.items.add(
        RibbonButtonGroupModel(
            align("alignLeft", Messages.get("common.align.left"), RibbonIcons.ALIGN_LEFT).also { it.isChecked = true },
            align("alignCenter", Messages.get("common.align.center"), RibbonIcons.ALIGN_CENTER),
            align("alignRight", Messages.get("common.align.right"), RibbonIcons.ALIGN_RIGHT),
        ),
    )
    alignment.items.add(
        RibbonToggleButtonModel("wrapText", Messages.get("command.wrap.text")).also {
            it.icon = RibbonIcons.REFRESH
            it.size = MEDIUM
            it.commandId = "wrapText"
        },
    )
    alignment.items.add(split("mergeCenter", Messages.get("command.merge.center"), RibbonIcons.MERGE, MEDIUM, "mergeCenter", Messages.get("home.merge.across"), Messages.get("home.merge.cells"), Messages.get("home.unmerge.cells")))
}

private fun numberGroup(): RibbonGroupModel = RibbonGroupModel("number", Messages.get("format.number"), RibbonIcons.CALCULATOR).also { number ->
    number.itemsLayout = RibbonGroupItemsLayout.ROWS
    number.rowCount = 2
    number.dialogLauncherCommandId = "formatCellsLauncher"
    number.items.add(
        RibbonRowModel(
            RibbonComboBoxModel("numberFormat", Messages.get("command.number.format"), NumberFormat.entries.toList()).also {
                it.commandId = "numberFormat"
                it.selectedItem = NumberFormat.GENERAL
                it.text = NumberFormat.GENERAL.label
                it.inputWidth = NUMBER_FORMAT_WIDTH
                it.keyTip = "N"
            },
        ),
    )
    // As in the RibbonSpace demo, use symbol characters as icons (the currency symbol follows the display language; ¥ in Japanese)
    number.items.add(
        RibbonRowModel(
            RibbonButtonGroupModel(
                button("accountingFormat", Messages.get("command.accounting"), RibbonIcon.text(Messages.get("number.currency.symbol")), SMALL, "accounting"),
                button("percentStyle", Messages.get("command.percent"), RibbonIcon.text("%"), SMALL, "percent"),
                button("commaStyle", Messages.get("command.comma"), RibbonIcon.text(","), SMALL, "comma"),
                button("increaseDecimalButton", Messages.get("command.increase.decimal"), RibbonIcon.text(".0"), SMALL, "increaseDecimal"),
                button("decreaseDecimalButton", Messages.get("command.decrease.decimal"), RibbonIcon.text(".00"), SMALL, "decreaseDecimal"),
            ),
        ),
    )
}

private fun stylesGroup(): RibbonGroupModel = RibbonGroupModel("styles", Messages.get("common.styles"), RibbonIcons.PALETTE).also { styles ->
    styles.reductionOrder = 1
    val conditional = RibbonDropDownButtonModel("conditionalFormatting", Messages.get("home.conditional.formatting")).also { c ->
        c.icon = RibbonIcons.FILTER
        c.size = LARGE
        c.keyTip = "L"
        for ((index, entry) in listOf(Messages.get("home.cf.highlight"), Messages.get("home.cf.top.bottom"), Messages.get("home.cf.data.bars"), Messages.get("home.cf.color.scales"), Messages.get("home.cf.icon.sets")).withIndex()) {
            val sub = RibbonMenuItemModel("cf.$index", entry)
            sub.items.add(RibbonMenuItemModel("cf.$index.1", Messages.get("home.cf.greater")))
            sub.items.add(RibbonMenuItemModel("cf.$index.2", Messages.get("home.cf.less")))
            sub.items.add(RibbonMenuItemModel("cf.$index.3", Messages.get("home.cf.between")))
            c.menuItems.add(sub)
        }
        c.menuItems.add(RibbonMenuSeparatorModel())
        c.menuItems.add(RibbonMenuItemModel("cf.new", Messages.get("home.cf.new")))
        c.menuItems.add(RibbonMenuItemModel("cf.manage", Messages.get("home.cf.manage")))
    }
    styles.items.add(conditional)
    styles.items.add(
        RibbonGalleryModel("cellStyles", Messages.get("home.cell.styles")).also { gallery ->
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
            gallery.menuItems.add(RibbonMenuItemModel("cs.new", Messages.get("home.cell.styles.new"), RibbonIcons.ADD))
            gallery.menuItems.add(RibbonMenuItemModel("cs.merge", Messages.get("home.cell.styles.merge"), RibbonIcons.MERGE))
        },
    )
}

private fun createInsertTab(viewModel: ExcelViewModel): RibbonTabModel = RibbonTabModel("insert", Messages.get("common.insert")).also { insert ->
    insert.keyTip = "N"
    insert.groups.add(
        RibbonGroupModel("tables", Messages.get("insert.tables"), RibbonIcons.TABLE).also { tables ->
            tables.items.add(split("pivotTable", Messages.get("command.pivot.table"), RibbonIcons.PIVOT, LARGE, "pivotTable", Messages.get("insert.pivot.from.range"), Messages.get("insert.pivot.from.external")))
            tables.items.add(button("recommendedPivot", Messages.get("insert.recommended.pivot"), RibbonIcons.PIVOT, LARGE))
            tables.items.add(
                RibbonDropDownButtonModel("table", Messages.get("command.table")).also { table ->
                    table.icon = RibbonIcons.TABLE
                    table.size = LARGE
                    table.menuItems.add(
                        RibbonGridPickerModel(
                            "tablePicker",
                            Messages.get("insert.table.picker"),
                            RibbonRelayCommand({ size -> (size as? RibbonGridSize)?.let { viewModel.record(Messages.get("log.table.inserted", it)) } }),
                        ),
                    )
                },
            )
        },
    )
    insert.groups.add(
        RibbonGroupModel("charts", Messages.get("insert.charts"), RibbonIcons.CHART).also { charts ->
            charts.dialogLauncherCommandId = "recommendedCharts"
            charts.items.add(button("recommendedCharts", Messages.get("command.recommended.charts"), RibbonIcons.CHART, LARGE, "recommendedCharts"))
            for ((id, label) in listOf("column" to Messages.get("insert.chart.column"), "line" to Messages.get("insert.chart.line"), "pie" to Messages.get("insert.chart.pie"), "bar" to Messages.get("insert.chart.bar"), "area" to Messages.get("insert.chart.area"), "scatter" to Messages.get("insert.chart.scatter"))) {
                charts.items.add(dropDown("chart.$id", label, RibbonIcons.CHART, SMALL, Messages.get("insert.chart.2d", label), Messages.get("insert.chart.3d", label)))
            }
            charts.items.add(RibbonButtonModel("selectChart", Messages.get("insert.select.chart"), RibbonIcons.CHART, viewModel.toggleChartCommand).also { it.size = LARGE })
        },
    )
}

private fun createFormulasTab(): RibbonTabModel = RibbonTabModel("formulas", Messages.get("home.paste.formulas")).also { formulas ->
    formulas.keyTip = "M"
    formulas.groups.add(
        RibbonGroupModel("functionLibrary", Messages.get("formulas.library"), RibbonIcons.FUNCTION).also { library ->
            library.items.add(button("insertFunction", Messages.get("command.insert.function"), RibbonIcons.FUNCTION, LARGE, "insertFunction"))
            val categories = listOf(Messages.get("command.auto.sum"), Messages.get("formulas.recent"), Messages.get("formulas.financial"), Messages.get("formulas.logical"), Messages.get("formulas.text"), Messages.get("formulas.date.time"), Messages.get("formulas.lookup"), Messages.get("formulas.math"))
            categories.forEachIndexed { index, label -> library.items.add(dropDown("fx.$index", label, RibbonIcons.FUNCTION, LARGE, Messages.get("formulas.functions", label))) }
        },
    )
    formulas.groups.add(
        RibbonGroupModel("calculation", Messages.get("formulas.calculation"), RibbonIcons.CALCULATOR).also { calculation ->
            calculation.items.add(button("calculateNow", Messages.get("command.calculate.now"), RibbonIcons.CALCULATOR, commandId = "calculateNow"))
            calculation.items.add(button("calculateSheet", Messages.get("formulas.calculate.sheet"), RibbonIcons.CALCULATOR))
        },
    )
}

private fun createDataTab(): RibbonTabModel = RibbonTabModel("data", Messages.get("data")).also { data ->
    data.keyTip = "A"
    data.groups.add(
        RibbonGroupModel("queries", Messages.get("data.queries"), RibbonIcons.DATABASE).also {
            it.items.add(split("refreshAll", Messages.get("command.refresh.all"), RibbonIcons.REFRESH, LARGE, "refreshAll", Messages.get("command.refresh.all"), Messages.get("data.refresh"), Messages.get("data.connection.properties")))
        },
    )
    data.groups.add(
        RibbonGroupModel("sortFilterGroup", Messages.get("command.sort.filter"), RibbonIcons.FILTER).also { sort ->
            sort.items.add(button("sortAZ", Messages.get("home.sort.ascending"), RibbonIcons.SORT_ASCENDING))
            sort.items.add(button("sortZA", Messages.get("home.sort.descending"), RibbonIcons.SORT_DESCENDING))
            sort.items.add(button("sortDialog", Messages.get("data.sort"), RibbonIcons.SORT, LARGE))
            sort.items.add(
                RibbonToggleButtonModel("filter", Messages.get("home.filter")).also {
                    it.icon = RibbonIcons.FILTER
                    it.size = LARGE
                    it.shortcut = "Ctrl+Shift+L"
                },
            )
        },
    )
}

private fun createViewTab(): RibbonTabModel = RibbonTabModel("view", Messages.get("common.view")).also { view ->
    view.keyTip = "W"
    view.groups.add(
        RibbonGroupModel("show", Messages.get("view.show"), RibbonIcons.VIEW).also { show ->
            show.items.add(RibbonCheckBoxModel("gridlines", Messages.get("view.gridlines"), true))
            show.items.add(RibbonCheckBoxModel("formulaBar", Messages.get("view.formula.bar"), true))
            show.items.add(RibbonCheckBoxModel("headings", Messages.get("view.headings"), true))
        },
    )
    view.groups.add(
        RibbonGroupModel("zoom", Messages.get("common.zoom"), RibbonIcons.ZOOM).also { zoom ->
            zoom.items.add(button("zoomDialog", Messages.get("common.zoom"), RibbonIcons.ZOOM, LARGE))
            zoom.items.add(button("zoom100", "100%", RibbonIcons.ZOOM, LARGE))
            zoom.items.add(
                RibbonSpinnerModel("zoomValue", Messages.get("view.zoom.value"), ZOOM_DEFAULT).also {
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
        RibbonGroupModel("window", Messages.get("common.window"), RibbonIcons.WINDOW).also { window ->
            window.items.add(button("newWindow", Messages.get("common.new.window"), RibbonIcons.WINDOW, LARGE, "newWindow"))
            window.items.add(dropDown("freezePanes", Messages.get("command.freeze.panes"), RibbonIcons.FREEZE, LARGE, Messages.get("command.freeze.panes"), Messages.get("view.freeze.top.row"), Messages.get("view.freeze.first.column")))
        },
    )
}

/** [Chart Design] under [Chart Tools]. */
private fun createChartDesignTab(): RibbonTabModel = RibbonTabModel("chartDesign", Messages.get("chart.design")).also { design ->
    design.contextualGroupId = "chart"
    design.keyTip = "JC"
    design.groups.add(
        RibbonGroupModel("chartLayouts", Messages.get("chart.layouts"), RibbonIcons.LAYOUT).also { layouts ->
            layouts.items.add(dropDown("addElement", Messages.get("chart.add.element"), RibbonIcons.ADD, LARGE, Messages.get("chart.axes"), Messages.get("chart.axis.titles"), Messages.get("chart.chart.title"), Messages.get("chart.data.labels"), Messages.get("chart.legend")))
            layouts.items.add(dropDown("quickLayout", Messages.get("chart.quick.layout"), RibbonIcons.LAYOUT, LARGE, Messages.get("chart.layout1"), Messages.get("chart.layout2"), Messages.get("chart.layout3")))
        },
    )
    design.groups.add(
        RibbonGroupModel("chartStylesGroup", Messages.get("chart.styles"), RibbonIcons.PALETTE).also { styles ->
            styles.reductionOrder = 1
            styles.items.add(
                RibbonDropDownButtonModel("chartColors", Messages.get("command.chart.colors")).also { colors ->
                    colors.icon = RibbonIcons.PALETTE
                    colors.size = LARGE
                    colors.menuItems.add(menu("chartColors.colorful", Messages.get("chart.colorful"), null, "chartStyles"))
                    colors.menuItems.add(menu("chartColors.monochrome", Messages.get("chart.monochromatic"), null, "chartStyles"))
                },
            )
            styles.items.add(
                RibbonGalleryModel("chartStyleGallery", Messages.get("chart.styles")).also { gallery ->
                    gallery.maxColumns = CHART_STYLE_MAX_COLUMNS
                    gallery.minColumns = CHART_STYLE_MIN_COLUMNS
                    gallery.itemWidth = CHART_STYLE_WIDTH
                    gallery.itemHeight = CHART_STYLE_HEIGHT
                    gallery.showLabels = false
                    for (i in 1..CHART_STYLES) gallery.items.add(RibbonGalleryItemModel("chartStyle$i", Messages.get("chart.style", i), RibbonIcons.CHART))
                },
            )
        },
    )
    design.groups.add(
        RibbonGroupModel("chartData", Messages.get("data"), RibbonIcons.DATABASE).also { data ->
            data.items.add(button("switchRowColumn", Messages.get("command.switch.row.column"), RibbonIcons.ROTATE, LARGE, "switchRowColumn"))
            data.items.add(button("selectData", Messages.get("command.select.data"), RibbonIcons.TABLE, LARGE, "selectData"))
        },
    )
    design.groups.add(
        RibbonGroupModel("type", Messages.get("chart.type"), RibbonIcons.CHART).also {
            it.items.add(button("changeChartType", Messages.get("command.change.chart.type"), RibbonIcons.CHART, LARGE, "changeChartType"))
        },
    )
}

private val FONT_FAMILIES = listOf("Aptos Narrow", "Arial", "Calibri", "Cambria", "Consolas", "Segoe UI", "Times New Roman", "Verdana", Messages.get("font.yu.gothic"), Messages.get("font.meiryo"))
private val FONT_SIZES = listOf(8.0, 9.0, 10.0, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0, 28.0, 36.0, 48.0, 72.0)

// Cell styles (label, category, background and text colors)
private val CELL_STYLES = listOf(
    Triple(Messages.get("cell.style.normal"), Messages.get("cell.style.good.bad.neutral"), "#FFFFFF" to "#242424"),
    Triple(Messages.get("cell.style.bad"), Messages.get("cell.style.good.bad.neutral"), "#FFC7CE" to "#9C0006"),
    Triple(Messages.get("cell.style.good"), Messages.get("cell.style.good.bad.neutral"), "#C6EFCE" to "#006100"),
    Triple(Messages.get("cell.style.neutral"), Messages.get("cell.style.good.bad.neutral"), "#FFEB9C" to "#9C5700"),
    Triple(Messages.get("cell.style.calculation"), Messages.get("cell.style.data.model"), "#F2F2F2" to "#FA7D00"),
    Triple(Messages.get("cell.style.check.cell"), Messages.get("cell.style.data.model"), "#A5A5A5" to "#FFFFFF"),
    Triple(Messages.get("cell.style.input"), Messages.get("cell.style.data.model"), "#FFCC99" to "#3F3F76"),
    Triple(Messages.get("cell.style.output"), Messages.get("cell.style.data.model"), "#F2F2F2" to "#3F3F3F"),
    Triple(Messages.get("cell.style.title"), Messages.get("cell.style.titles.headings"), "#FFFFFF" to "#44546A"),
    Triple(Messages.get("cell.style.heading1"), Messages.get("cell.style.titles.headings"), "#FFFFFF" to "#44546A"),
    Triple(Messages.get("cell.style.heading2"), Messages.get("cell.style.titles.headings"), "#FFFFFF" to "#44546A"),
    Triple(Messages.get("cell.style.total"), Messages.get("cell.style.titles.headings"), "#FFFFFF" to "#242424"),
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
