package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.extension.ribbon.WRibbon
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstageItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstagePlacement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualActivation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomizePage
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDensity
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDisplayMode
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDropDownButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMinimizeBehavior
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonQuickAccessPosition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonReductionStrategy
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRowModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonScreenTip
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonVisibilityMode
import java.util.Locale

/** The ribbon page (a pure Kotlin version of RibbonSpace.WinUI; Office- and AutoCAD-style ribbons). */
internal fun buildRibbonPage(): WComponent {
    val page = buildPage(
        "Ribbon",
        Messages.get("ribbon.description"),
    )
    page.add(buildOfficeRibbonExample())
    page.add(buildCadRibbonExample())
    return page
}

// ---------------------------------------------------------------- Small building helpers

private fun button(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize = RibbonItemSize.MEDIUM) =
    RibbonButtonModel(id, label, icon).also { it.size = size }

private fun toggle(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize = RibbonItemSize.SMALL) =
    RibbonToggleButtonModel(id, label).also {
        it.icon = icon
        it.size = size
    }

private fun group(id: String, label: String, vararg items: RibbonItemModel) =
    RibbonGroupModel(id, label).also { g -> items.forEach { g.items.add(it) } }

private fun menuItem(id: String, label: String, icon: RibbonIcon? = null) = RibbonMenuItemModel(id, label, icon)

/** A model with Word's [Home] / [Insert] / [Design] tabs, the [Table Tools] contextual tabs, and a Backstage. */
// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod")
private fun createOfficeModel(): RibbonModel = RibbonModel().apply {
    applicationButtonLabel = Messages.get("ribbon.word.file")
    title = Messages.get("ribbon.word.title")
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(
        RibbonTabModel("design", Messages.get("ribbon.word.tab.design")).also { tab ->
            tab.keyTip = "D"
            tab.groups.add(
                group(
                    "themes",
                    Messages.get("ribbon.word.group.documentformatting"),
                    button("themes.theme", Messages.get("ribbon.word.theme"), RibbonIcons.THEME, RibbonItemSize.LARGE),
                    RibbonColorPickerModel("themes.color", Messages.get("ribbon.word.pagecolor"), RibbonIcons.COLOR).also { it.size = RibbonItemSize.LARGE },
                ),
            )
        },
    )
    tabs.add(
        RibbonTabModel("view", Messages.get("ribbon.word.tab.view")).also { tab ->
            tab.keyTip = "W"
            tab.groups.add(
                group(
                    "views",
                    Messages.get("ribbon.word.group.views"),
                    toggle("view.reading", Messages.get("ribbon.word.readingmode"), RibbonIcons.READING_MODE, RibbonItemSize.LARGE),
                    toggle("view.print", Messages.get("ribbon.word.printlayout"), RibbonIcons.PAGE, RibbonItemSize.LARGE).also { it.isChecked = true },
                    RibbonCheckBoxModel("view.ruler", Messages.get("ribbon.word.ruler")),
                    RibbonCheckBoxModel("view.grid", Messages.get("ribbon.word.gridlines")),
                ),
            )
        },
    )
    val tableTools = RibbonContextualGroupModel("tableTools", Messages.get("ribbon.word.tabletools")).also {
        it.activation = RibbonContextualActivation.SELECT_ON_SHOW
    }
    contextualGroups.add(tableTools)
    tabs.add(
        RibbonTabModel("tableDesign", Messages.get("ribbon.word.tab.tabledesign")).also { tab ->
            tab.contextualGroupId = "tableTools"
            tab.groups.add(group("tableStyles", Messages.get("ribbon.word.group.tablestyles"), button("table.shading", Messages.get("ribbon.word.shading"), RibbonIcons.SHADING, RibbonItemSize.LARGE)))
        },
    )
    tabs.add(
        RibbonTabModel("tableLayout", Messages.get("ribbon.word.tab.layout")).also { tab ->
            tab.contextualGroupId = "tableTools"
            tab.groups.add(group("rowsColumns", Messages.get("ribbon.word.group.rowscolumns"), button("table.insertRow", Messages.get("ribbon.word.insertabove"), RibbonIcons.INSERT_ROW, RibbonItemSize.LARGE)))
        },
    )
    quickAccessItems.add(button("qat.save", Messages.get("ribbon.word.save"), RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("qat.undo", Messages.get("ribbon.word.undo"), RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("qat.redo", Messages.get("ribbon.word.redo"), RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    quickAccessCandidates.addAll(listOf(findItem("paste")!!, findItem("bold")!!, findItem("insert.table")!!))
    tabStripItems.add(button("comments", Messages.get("ribbon.word.comments"), RibbonIcons.COMMENT))
    tabStripItems.add(button("share", Messages.get("ribbon.word.share"), RibbonIcons.SHARE))
    backstage.title = "Word"
    backstage.items.add(RibbonBackstageItemModel("home", Messages.get("ribbon.word.home"), RibbonIcons.HOME, WLabel(Messages.get("ribbon.word.backstage.home"))))
    backstage.items.add(RibbonBackstageItemModel("new", Messages.get("ribbon.word.new"), RibbonIcons.NEW_DOCUMENT, WLabel(Messages.get("ribbon.word.backstage.new"))))
    backstage.items.add(RibbonBackstageItemModel("open", Messages.get("ribbon.word.open"), RibbonIcons.OPEN, WLabel(Messages.get("ribbon.word.backstage.open"))))
    backstage.items.add(RibbonBackstageItemModel("save", Messages.get("ribbon.word.save"), RibbonIcons.SAVE).also { it.hasSeparatorBefore = true })
    backstage.items.add(RibbonBackstageItemModel("print", Messages.get("ribbon.word.print"), RibbonIcons.PRINT, WLabel(Messages.get("ribbon.word.backstage.print"))))
    backstage.items.add(
        RibbonBackstageItemModel("options", Messages.get("ribbon.word.options"), RibbonIcons.SETTINGS).also { it.placement = RibbonBackstagePlacement.BOTTOM },
    )
}

// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun createHomeTab(): RibbonTabModel = RibbonTabModel("home", Messages.get("ribbon.word.home")).also { tab ->
    tab.keyTip = "H"
    tab.groups.add(
        group(
            "clipboard",
            Messages.get("ribbon.word.group.clipboard"),
            RibbonSplitButtonModel("paste", Messages.get("ribbon.word.paste")).also { paste ->
                paste.icon = RibbonIcons.PASTE
                paste.size = RibbonItemSize.LARGE
                paste.shortcut = "Ctrl+V"
                paste.screenTip = RibbonScreenTip(Messages.get("ribbon.word.paste.tiptitle"), Messages.get("ribbon.word.paste.tipbody"))
                paste.menuItems.add(menuItem("paste.keep", Messages.get("ribbon.word.paste.keep"), RibbonIcons.PASTE))
                paste.menuItems.add(menuItem("paste.text", Messages.get("ribbon.word.paste.text"), RibbonIcons.TEXT))
                paste.menuItems.add(RibbonMenuSeparatorModel())
                paste.menuItems.add(menuItem("paste.special", Messages.get("ribbon.word.paste.special")))
            },
            button("cut", Messages.get("ribbon.word.cut"), RibbonIcons.CUT).also { it.shortcut = "Ctrl+X" },
            button("copy", Messages.get("ribbon.word.copy"), RibbonIcons.COPY).also { it.shortcut = "Ctrl+C" },
            button("formatPainter", Messages.get("ribbon.word.formatpainter"), RibbonIcons.FORMAT_PAINTER),
        ).also { it.isDialogLauncherVisible = true },
    )
    tab.groups.add(
        group(
            "font",
            Messages.get("ribbon.word.group.font"),
            RibbonRowModel(
                RibbonFontComboBoxModel("font.name").also { it.selectedItem = "Yu Gothic UI" },
                RibbonFontSizeComboBoxModel("font.size").also { it.selectedItem = 10.5 },
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("bold", Messages.get("ribbon.word.bold"), RibbonIcons.BOLD).also { it.shortcut = "Ctrl+B" },
                    toggle("italic", Messages.get("ribbon.word.italic"), RibbonIcons.ITALIC).also { it.shortcut = "Ctrl+I" },
                    toggle("underline", Messages.get("ribbon.word.underline"), RibbonIcons.UNDERLINE).also { it.shortcut = "Ctrl+U" },
                ),
                RibbonColorPickerModel("font.color", Messages.get("ribbon.word.fontcolor"), RibbonIcons.FONT_COLOR),
                RibbonColorPickerModel("font.highlight", Messages.get("ribbon.word.highlight"), RibbonIcons.HIGHLIGHT).also { it.showNoColor = true },
            ),
        ).also {
            it.itemsLayout = RibbonGroupItemsLayout.ROWS
            it.rowCount = 2
            it.isDialogLauncherVisible = true
        },
    )
    tab.groups.add(
        group(
            "paragraph",
            Messages.get("ribbon.word.group.paragraph"),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("bullets", Messages.get("ribbon.word.bullets"), RibbonIcons.BULLETS),
                    toggle("numbering", Messages.get("ribbon.word.numbering"), RibbonIcons.NUMBERING),
                ),
                RibbonSpinnerModel("indent", Messages.get("ribbon.word.indent"), 0.0).also {
                    it.unit = Messages.get("ribbon.word.indent.unit")
                    it.maximum = 20.0
                },
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("align.left", Messages.get("ribbon.word.alignleft"), RibbonIcons.ALIGN_LEFT).also {
                        it.groupName = "align"
                        it.isChecked = true
                    },
                    toggle("align.center", Messages.get("ribbon.word.center"), RibbonIcons.ALIGN_CENTER).also { it.groupName = "align" },
                    toggle("align.right", Messages.get("ribbon.word.alignright"), RibbonIcons.ALIGN_RIGHT).also { it.groupName = "align" },
                    toggle("align.justify", Messages.get("ribbon.word.justify"), RibbonIcons.ALIGN_JUSTIFY).also { it.groupName = "align" },
                ),
            ),
        ).also {
            it.itemsLayout = RibbonGroupItemsLayout.ROWS
            it.rowCount = 2
        },
    )
    tab.groups.add(
        group(
            "styles",
            Messages.get("ribbon.word.styles"),
            RibbonGalleryModel("styles.gallery", Messages.get("ribbon.word.styles")).also { gallery ->
                gallery.icon = RibbonIcons.FONT
                gallery.itemWidth = 76.0
                gallery.itemHeight = 54.0
                gallery.maxColumns = 5
                gallery.minColumns = 3
                gallery.isFilterEnabled = true
                listOf(
                    Messages.get("ribbon.word.style.normal") to 14.0,
                    Messages.get("ribbon.word.style.heading1") to 18.0,
                    Messages.get("ribbon.word.style.heading2") to 16.0,
                    Messages.get("ribbon.word.style.title") to 22.0,
                    Messages.get("ribbon.word.style.subtitle") to 13.0,
                    Messages.get("ribbon.word.style.quote") to 13.0,
                    Messages.get("ribbon.word.style.emphasis") to 13.0,
                )
                    .forEachIndexed { index, (name, size) ->
                        gallery.items.add(
                            RibbonGalleryItemModel("style$index", name, category = if (index < 3) Messages.get("ribbon.word.style.category.frequent") else Messages.get("ribbon.word.style.category.other")).also {
                                it.previewText = Messages.get("ribbon.word.style.preview")
                                it.previewFontSize = size
                                it.previewBold = index in 1..3
                            },
                        )
                    }
                gallery.selectedItem = gallery.items[0]
                gallery.menuItems.add(menuItem("styles.clear", Messages.get("ribbon.word.clearformatting"), RibbonIcons.CLEAR_FORMATTING))
                gallery.menuItems.add(menuItem("styles.apply", Messages.get("ribbon.word.applystyles")))
            },
        ).also { it.reductionOrder = 1 },
    )
    tab.groups.add(
        group(
            "editing",
            Messages.get("ribbon.word.group.editing"),
            button("find", Messages.get("ribbon.word.find"), RibbonIcons.FIND).also { it.shortcut = "Ctrl+F" },
            button("replace", Messages.get("ribbon.word.replace"), RibbonIcons.REPLACE).also { it.shortcut = "Ctrl+H" },
            RibbonDropDownButtonModel("select", Messages.get("ribbon.word.select")).also { select ->
                select.icon = RibbonIcons.SELECT
                select.menuItems.add(menuItem("select.all", Messages.get("ribbon.word.selectall"), RibbonIcons.SELECT_ALL))
                select.menuItems.add(menuItem("select.objects", Messages.get("ribbon.word.selectobjects"), RibbonIcons.POINTER))
            },
        ).also { it.reductionOrder = 2 },
    )
}

private fun createInsertTab(): RibbonTabModel = RibbonTabModel("insert", Messages.get("ribbon.insert")).also { tab ->
    tab.keyTip = "N"
    tab.groups.add(
        group(
            "tables",
            Messages.get("ribbon.word.group.tables"),
            RibbonDropDownButtonModel("insert.table", Messages.get("ribbon.word.table")).also { table ->
                table.icon = RibbonIcons.TABLE
                table.size = RibbonItemSize.LARGE
                table.menuItems.add(RibbonGridPickerModel("insert.tablePicker", Messages.get("ribbon.word.inserttable")))
                table.menuItems.add(RibbonMenuSeparatorModel())
                table.menuItems.add(menuItem("insert.tableDialog", Messages.get("ribbon.word.inserttable.dialog"), RibbonIcons.TABLE))
            },
        ),
    )
    tab.groups.add(
        group(
            "illustrations",
            Messages.get("ribbon.word.group.illustrations"),
            button("insert.picture", Messages.get("ribbon.word.pictures"), RibbonIcons.PICTURE, RibbonItemSize.LARGE),
            button("insert.shapes", Messages.get("ribbon.word.shapes"), RibbonIcons.SHAPES, RibbonItemSize.LARGE),
            button("insert.icons", Messages.get("ribbon.word.icons"), RibbonIcons.ICONS, RibbonItemSize.LARGE),
            button("insert.chart", Messages.get("ribbon.word.chart"), RibbonIcons.CHART, RibbonItemSize.LARGE),
        ),
    )
    tab.groups.add(group("links", Messages.get("ribbon.word.group.links"), button("insert.link", Messages.get("ribbon.word.link"), RibbonIcons.LINK, RibbonItemSize.LARGE)))
    tab.groups.add(group("comments", Messages.get("ribbon.word.comments"), button("insert.comment", Messages.get("ribbon.word.comment"), RibbonIcons.COMMENT, RibbonItemSize.LARGE)))
}

/** A Word-style ribbon, with Options for controlling its display, theme, and state. */
// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun buildOfficeRibbonExample(): WComponent {
    val model = createOfficeModel()
    val ribbon = WRibbon(model)
    val log = WLabel(Messages.get("ribbon.log.invoked.none")).also { it.textWrapping = TextWrapping.WRAP }
    ribbon.addItemInvokedListener { event ->
        val label = event.item.label ?: event.item.id
        log.text = event.parameter?.let { Messages.get("ribbon.log.invokedwithparameter", label, it.toString()) } ?: Messages.get("ribbon.log.invoked", label)
    }
    ribbon.addTabChangeListener { event -> log.text = Messages.get("ribbon.log.tab", event.newTab?.label) }
    ribbon.width = RIBBON_WIDTH

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("ribbon.options.displaymode")))
    options.add(
        WComboBox(listOf(Messages.get("ribbon.options.displaymode.classic"), Messages.get("ribbon.options.displaymode.simplified"))).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { model.displayMode = if (combo.selectedIndex == 1) RibbonDisplayMode.SIMPLIFIED else RibbonDisplayMode.CLASSIC }
        },
    )
    options.add(optionsLabel(Messages.get("ribbon.options.visibility")))
    val modes = listOf(RibbonVisibilityMode.ALWAYS_SHOW, RibbonVisibilityMode.TABS_ONLY, RibbonVisibilityMode.FULL_SCREEN)
    options.add(
        WComboBox(listOf(Messages.get("ribbon.options.visibility.always"), Messages.get("ribbon.options.visibility.tabsonly"), Messages.get("ribbon.options.visibility.fullscreen"))).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { model.visibilityMode = modes[combo.selectedIndex.coerceAtLeast(0)] }
        },
    )
    options.add(optionsLabel(Messages.get("ribbon.options.density")))
    options.add(
        WComboBox(listOf(Messages.get("ribbon.options.density.standard"), Messages.get("ribbon.options.density.compact"), Messages.get("ribbon.options.density.touch"))).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { model.density = RibbonDensity.entries[combo.selectedIndex.coerceAtLeast(0)] }
        },
    )
    options.add(optionsLabel(Messages.get("ribbon.options.theme")))
    options.add(
        WComboBox(RibbonThemePalette.PRESETS.map { it.name }).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { WRibbonTheme.applyPalette(RibbonThemePalette.PRESETS[combo.selectedIndex.coerceAtLeast(0)]) }
        },
    )
    options.add(optionsLabel(Messages.get("ribbon.options.chrome")))
    options.add(
        WComboBox(RibbonChromeStyle.entries.map { it.name }).also { combo ->
            combo.selectedIndex = RibbonChromeStyle.entries.indexOf(WRibbonTheme.chromeStyle)
            combo.addListSelectionListener { WRibbonTheme.applyChromeStyle(RibbonChromeStyle.entries[combo.selectedIndex.coerceAtLeast(0)]) }
        },
    )
    // The language of the strings the ribbon shows itself (ScreenTips, menus, customization, etc.). Shared by all ribbons
    options.add(optionsLabel(Messages.get("ribbon.options.language")))
    val cultures = RibbonStrings.builtInCultures.toList()
    options.add(
        WComboBox(cultures.map { tag -> Locale.forLanguageTag(tag).let { "${it.getDisplayName(it)} ($tag)" } }).also { combo ->
            combo.selectedIndex = cultures.indexOf(RibbonStrings.current.locale.language).coerceAtLeast(0)
            combo.addListSelectionListener { RibbonStrings.current = RibbonStrings.forCulture(cultures[combo.selectedIndex.coerceAtLeast(0)]) }
        },
    )
    options.add(
        WCheckBox(Messages.get("ribbon.options.qatbelow")).also { check ->
            check.addItemListener { checked ->
                model.quickAccessPosition = if (checked == true) RibbonQuickAccessPosition.BELOW_RIBBON else RibbonQuickAccessPosition.ABOVE_RIBBON
            }
        },
    )
    options.add(
        WCheckBox(Messages.get("ribbon.options.tabletools")).also { check ->
            check.addItemListener { checked -> model.setContextualGroupVisible("tableTools", checked == true) }
        },
    )
    val actions = WPanel(spacing = 6.0, orientation = Orientation.HORIZONTAL)
    actions.add(WButton("KeyTip").also { it.addActionListener { ribbon.showKeyTips() } })
    actions.add(WButton(Messages.get("ribbon.word.file")).also { it.addActionListener { ribbon.isBackstageOpen = true } })
    actions.add(WButton(Messages.get("ribbon.options.customize")).also { it.addActionListener { ribbon.showCustomizeDialog(RibbonCustomizePage.RIBBON) } })
    options.add(actions)
    var saved: String? = null
    val stateRow = WPanel(spacing = 6.0, orientation = Orientation.HORIZONTAL)
    stateRow.add(WButton(Messages.get("ribbon.options.savestate")).also { it.addActionListener { saved = ribbon.saveStateToJson().also { log.text = Messages.get("ribbon.log.saved", it.length.toString()) } } })
    stateRow.add(WButton(Messages.get("ribbon.options.restore")).also { it.addActionListener { saved?.let { json -> log.text = Messages.get("ribbon.log.restored", ribbon.loadStateFromJson(json).toString()) } } })
    options.add(stateRow)

    val body = WPanel(spacing = 8.0)
    body.add(ribbon)
    body.add(log)
    return buildExample(Messages.get("ribbon.word.example.title"), body, options)
}

/** AutoCAD style: cycling minimize states, floating panels, expanded panels, group-by-group reduction, and the panel visibility menu. */
// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod")
private fun buildCadRibbonExample(): WComponent {
    val model = RibbonModel().apply {
        applicationButtonLabel = "A"
        minimizeBehavior = RibbonMinimizeBehavior.CYCLE_ALL
        tabs.add(
            RibbonTabModel("cadHome", Messages.get("ribbon.word.home")).also { tab ->
                tab.groups.add(
                    group(
                        "draw",
                        Messages.get("ribbon.cad.group.draw"),
                        button("draw.line", Messages.get("ribbon.cad.line"), RibbonIcons.LINE, RibbonItemSize.LARGE),
                        button("draw.polyline", Messages.get("ribbon.cad.polyline"), RibbonIcons.POLYGON, RibbonItemSize.LARGE),
                        button("draw.circle", Messages.get("ribbon.cad.circle"), RibbonIcons.CIRCLE),
                        button("draw.rectangle", Messages.get("ribbon.cad.rectangle"), RibbonIcons.RECTANGLE),
                    ).also { g ->
                        g.slideOutItems.add(button("draw.spline", Messages.get("ribbon.cad.spline"), RibbonIcons.PEN))
                        g.slideOutItems.add(button("draw.hatch", Messages.get("ribbon.cad.hatch"), RibbonIcons.FILL))
                    },
                )
                tab.groups.add(
                    group(
                        "modify",
                        Messages.get("ribbon.cad.group.modify"),
                        button("modify.move", Messages.get("ribbon.cad.move"), RibbonIcons.MOVE),
                        button("modify.rotate", Messages.get("ribbon.cad.rotate"), RibbonIcons.ROTATE),
                        button("modify.trim", Messages.get("ribbon.cad.trim"), RibbonIcons.CROP),
                        RibbonSeparatorModel(),
                        button("modify.erase", Messages.get("ribbon.cad.erase"), RibbonIcons.ERASER),
                    ).also { it.reductionOrder = 1 },
                )
                tab.groups.add(
                    group(
                        "layers",
                        Messages.get("ribbon.cad.group.layers"),
                        button("layers.properties", Messages.get("ribbon.cad.layerproperties"), RibbonIcons.LAYERS, RibbonItemSize.LARGE),
                        toggle("layers.lock", Messages.get("ribbon.cad.lock"), RibbonIcons.LOCK),
                    ).also { it.reductionOrder = 2 },
                )
                tab.groups.add(group("measure", Messages.get("ribbon.cad.group.measure"), button("measure.distance", Messages.get("ribbon.cad.distance"), RibbonIcons.MEASURE, RibbonItemSize.LARGE)))
            },
        )
        tabs.add(
            RibbonTabModel("cadInsert", Messages.get("ribbon.insert")).also { tab ->
                tab.groups.add(group("block", Messages.get("ribbon.cad.group.block"), button("block.insert", Messages.get("ribbon.insert"), RibbonIcons.COMPONENT, RibbonItemSize.LARGE)))
            },
        )
    }
    val ribbon = WRibbon(model)
    ribbon.isMinimizeButtonVisible = true
    ribbon.canFloatGroups = true
    ribbon.isVisibilityMenuEnabled = true
    ribbon.reductionStrategy = RibbonReductionStrategy.GROUP_BY_GROUP
    ribbon.width = CAD_RIBBON_WIDTH
    val log = WLabel(Messages.get("ribbon.cad.hint"))
    log.textWrapping = TextWrapping.WRAP
    ribbon.addGroupFloatingListener { _, group, floating -> log.text = if (floating) Messages.get("ribbon.cad.log.floating", group.label) else Messages.get("ribbon.cad.log.docked", group.label) }

    val options = WPanel(spacing = 8.0)
    options.add(
        WCheckBox(Messages.get("ribbon.cad.options.cadstyle")).also { check ->
            check.addItemListener { checked -> WRibbonTheme.applyStyle(if (checked == true) RibbonThemeStyle.CAD else RibbonThemeStyle.OFFICE) }
        },
    )
    options.add(WButton(Messages.get("ribbon.cad.options.minimize")).also { it.addActionListener { ribbon.toggleMinimized() } })
    options.add(WButton(Messages.get("ribbon.cad.options.floatdraw")).also { it.addActionListener { ribbon.floatGroup(model.findGroup("draw")!!) } })
    options.add(WButton(Messages.get("ribbon.cad.options.dockall")).also { it.addActionListener { ribbon.returnAllPanelsToRibbon() } })
    options.add(optionsLabel(Messages.get("ribbon.cad.options.hint")).also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(ribbon)
    body.add(log)
    return buildExample(Messages.get("ribbon.cad.example.title"), body, options)
}

private const val RIBBON_WIDTH = 820.0
private const val CAD_RIBBON_WIDTH = 760.0
