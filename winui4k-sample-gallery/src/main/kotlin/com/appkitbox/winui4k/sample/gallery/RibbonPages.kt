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
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel
import com.appkitbox.winui4k.ribbon.RibbonBackstagePlacement
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonChromeStyle
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonCustomizePage
import com.appkitbox.winui4k.ribbon.RibbonDensity
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel
import com.appkitbox.winui4k.ribbon.RibbonFontComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonGridPickerModel
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonMinimizeBehavior
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonQuickAccessPosition
import com.appkitbox.winui4k.ribbon.RibbonReductionStrategy
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonScreenTip
import com.appkitbox.winui4k.ribbon.RibbonSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel
import com.appkitbox.winui4k.ribbon.RibbonVisibilityMode
import java.util.Locale

/** The ribbon page (a pure Kotlin version of RibbonSpace.WinUI; Office- and AutoCAD-style ribbons). */
internal fun buildRibbonPage(): WComponent {
    val page = buildPage(
        "Ribbon",
        "An Office-style ribbon. Changes to the model (RibbonModel) are reflected in the display, and user actions are written back to the model. " +
            "Resizing the window shrinks the groups, the Alt key shows KeyTips, and right-clicking lets you add to the QAT or customize.",
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
    applicationButtonLabel = "File"
    title = "Document1 - Word"
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(
        RibbonTabModel("design", "Design").also { tab ->
            tab.keyTip = "D"
            tab.groups.add(
                group(
                    "themes",
                    "Document Formatting",
                    button("themes.theme", "Themes", RibbonIcons.THEME, RibbonItemSize.LARGE),
                    RibbonColorPickerModel("themes.color", "Page Color", RibbonIcons.COLOR).also { it.size = RibbonItemSize.LARGE },
                ),
            )
        },
    )
    tabs.add(
        RibbonTabModel("view", "View").also { tab ->
            tab.keyTip = "W"
            tab.groups.add(
                group(
                    "views",
                    "Views",
                    toggle("view.reading", "Read Mode", RibbonIcons.READING_MODE, RibbonItemSize.LARGE),
                    toggle("view.print", "Print Layout", RibbonIcons.PAGE, RibbonItemSize.LARGE).also { it.isChecked = true },
                    RibbonCheckBoxModel("view.ruler", "Ruler"),
                    RibbonCheckBoxModel("view.grid", "Gridlines"),
                ),
            )
        },
    )
    val tableTools = RibbonContextualGroupModel("tableTools", "Table Tools").also {
        it.activation = RibbonContextualActivation.SELECT_ON_SHOW
    }
    contextualGroups.add(tableTools)
    tabs.add(
        RibbonTabModel("tableDesign", "Table Design").also { tab ->
            tab.contextualGroupId = "tableTools"
            tab.groups.add(group("tableStyles", "Table Styles", button("table.shading", "Shading", RibbonIcons.SHADING, RibbonItemSize.LARGE)))
        },
    )
    tabs.add(
        RibbonTabModel("tableLayout", "Layout").also { tab ->
            tab.contextualGroupId = "tableTools"
            tab.groups.add(group("rowsColumns", "Rows & Columns", button("table.insertRow", "Insert Above", RibbonIcons.INSERT_ROW, RibbonItemSize.LARGE)))
        },
    )
    quickAccessItems.add(button("qat.save", "Save", RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("qat.undo", "Undo", RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("qat.redo", "Redo", RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    quickAccessCandidates.addAll(listOf(findItem("paste")!!, findItem("bold")!!, findItem("insert.table")!!))
    tabStripItems.add(button("comments", "Comments", RibbonIcons.COMMENT))
    tabStripItems.add(button("share", "Share", RibbonIcons.SHARE))
    backstage.title = "Word"
    backstage.items.add(RibbonBackstageItemModel("home", "Home", RibbonIcons.HOME, WLabel("Your recent items appear here.")))
    backstage.items.add(RibbonBackstageItemModel("new", "New", RibbonIcons.NEW_DOCUMENT, WLabel("Create a document from a blank document or a template.")))
    backstage.items.add(RibbonBackstageItemModel("open", "Open", RibbonIcons.OPEN, WLabel("Open a file.")))
    backstage.items.add(RibbonBackstageItemModel("save", "Save", RibbonIcons.SAVE).also { it.hasSeparatorBefore = true })
    backstage.items.add(RibbonBackstageItemModel("print", "Print", RibbonIcons.PRINT, WLabel("Choose a printer and settings.")))
    backstage.items.add(
        RibbonBackstageItemModel("options", "Options", RibbonIcons.SETTINGS).also { it.placement = RibbonBackstagePlacement.BOTTOM },
    )
}

// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun createHomeTab(): RibbonTabModel = RibbonTabModel("home", "Home").also { tab ->
    tab.keyTip = "H"
    tab.groups.add(
        group(
            "clipboard",
            "Clipboard",
            RibbonSplitButtonModel("paste", "Paste").also { paste ->
                paste.icon = RibbonIcons.PASTE
                paste.size = RibbonItemSize.LARGE
                paste.shortcut = "Ctrl+V"
                paste.screenTip = RibbonScreenTip("Paste (Ctrl+V)", "Add content on the Clipboard to your document.")
                paste.menuItems.add(menuItem("paste.keep", "Keep Source Formatting", RibbonIcons.PASTE))
                paste.menuItems.add(menuItem("paste.text", "Keep Text Only", RibbonIcons.TEXT))
                paste.menuItems.add(RibbonMenuSeparatorModel())
                paste.menuItems.add(menuItem("paste.special", "Paste Special..."))
            },
            button("cut", "Cut", RibbonIcons.CUT).also { it.shortcut = "Ctrl+X" },
            button("copy", "Copy", RibbonIcons.COPY).also { it.shortcut = "Ctrl+C" },
            button("formatPainter", "Format Painter", RibbonIcons.FORMAT_PAINTER),
        ).also { it.isDialogLauncherVisible = true },
    )
    tab.groups.add(
        group(
            "font",
            "Font",
            RibbonRowModel(
                RibbonFontComboBoxModel("font.name").also { it.selectedItem = "Yu Gothic UI" },
                RibbonFontSizeComboBoxModel("font.size").also { it.selectedItem = 10.5 },
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("bold", "Bold", RibbonIcons.BOLD).also { it.shortcut = "Ctrl+B" },
                    toggle("italic", "Italic", RibbonIcons.ITALIC).also { it.shortcut = "Ctrl+I" },
                    toggle("underline", "Underline", RibbonIcons.UNDERLINE).also { it.shortcut = "Ctrl+U" },
                ),
                RibbonColorPickerModel("font.color", "Font Color", RibbonIcons.FONT_COLOR),
                RibbonColorPickerModel("font.highlight", "Text Highlight Color", RibbonIcons.HIGHLIGHT).also { it.showNoColor = true },
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
            "Paragraph",
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("bullets", "Bullets", RibbonIcons.BULLETS),
                    toggle("numbering", "Numbering", RibbonIcons.NUMBERING),
                ),
                RibbonSpinnerModel("indent", "Indent", 0.0).also {
                    it.unit = "ch"
                    it.maximum = 20.0
                },
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("align.left", "Align Left", RibbonIcons.ALIGN_LEFT).also {
                        it.groupName = "align"
                        it.isChecked = true
                    },
                    toggle("align.center", "Center", RibbonIcons.ALIGN_CENTER).also { it.groupName = "align" },
                    toggle("align.right", "Align Right", RibbonIcons.ALIGN_RIGHT).also { it.groupName = "align" },
                    toggle("align.justify", "Justify", RibbonIcons.ALIGN_JUSTIFY).also { it.groupName = "align" },
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
            "Styles",
            RibbonGalleryModel("styles.gallery", "Styles").also { gallery ->
                gallery.icon = RibbonIcons.FONT
                gallery.itemWidth = 76.0
                gallery.itemHeight = 54.0
                gallery.maxColumns = 5
                gallery.minColumns = 3
                gallery.isFilterEnabled = true
                listOf("Normal" to 14.0, "Heading 1" to 18.0, "Heading 2" to 16.0, "Title" to 22.0, "Subtitle" to 13.0, "Quote" to 13.0, "Emphasis" to 13.0)
                    .forEachIndexed { index, (name, size) ->
                        gallery.items.add(
                            RibbonGalleryItemModel("style$index", name, category = if (index < 3) "Common Styles" else "Other").also {
                                it.previewText = "AaBbCc"
                                it.previewFontSize = size
                                it.previewBold = index in 1..3
                            },
                        )
                    }
                gallery.selectedItem = gallery.items[0]
                gallery.menuItems.add(menuItem("styles.clear", "Clear Formatting", RibbonIcons.CLEAR_FORMATTING))
                gallery.menuItems.add(menuItem("styles.apply", "Apply Styles..."))
            },
        ).also { it.reductionOrder = 1 },
    )
    tab.groups.add(
        group(
            "editing",
            "Editing",
            button("find", "Find", RibbonIcons.FIND).also { it.shortcut = "Ctrl+F" },
            button("replace", "Replace", RibbonIcons.REPLACE).also { it.shortcut = "Ctrl+H" },
            RibbonDropDownButtonModel("select", "Select").also { select ->
                select.icon = RibbonIcons.SELECT
                select.menuItems.add(menuItem("select.all", "Select All", RibbonIcons.SELECT_ALL))
                select.menuItems.add(menuItem("select.objects", "Select Objects", RibbonIcons.POINTER))
            },
        ).also { it.reductionOrder = 2 },
    )
}

private fun createInsertTab(): RibbonTabModel = RibbonTabModel("insert", "Insert").also { tab ->
    tab.keyTip = "N"
    tab.groups.add(
        group(
            "tables",
            "Tables",
            RibbonDropDownButtonModel("insert.table", "Table").also { table ->
                table.icon = RibbonIcons.TABLE
                table.size = RibbonItemSize.LARGE
                table.menuItems.add(RibbonGridPickerModel("insert.tablePicker", "Insert Table"))
                table.menuItems.add(RibbonMenuSeparatorModel())
                table.menuItems.add(menuItem("insert.tableDialog", "Insert Table...", RibbonIcons.TABLE))
            },
        ),
    )
    tab.groups.add(
        group(
            "illustrations",
            "Illustrations",
            button("insert.picture", "Pictures", RibbonIcons.PICTURE, RibbonItemSize.LARGE),
            button("insert.shapes", "Shapes", RibbonIcons.SHAPES, RibbonItemSize.LARGE),
            button("insert.icons", "Icons", RibbonIcons.ICONS, RibbonItemSize.LARGE),
            button("insert.chart", "Chart", RibbonIcons.CHART, RibbonItemSize.LARGE),
        ),
    )
    tab.groups.add(group("links", "Links", button("insert.link", "Link", RibbonIcons.LINK, RibbonItemSize.LARGE)))
    tab.groups.add(group("comments", "Comments", button("insert.comment", "Comment", RibbonIcons.COMMENT, RibbonItemSize.LARGE)))
}

/** A Word-style ribbon, with Options for controlling its display, theme, and state. */
// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun buildOfficeRibbonExample(): WComponent {
    val model = createOfficeModel()
    val ribbon = WRibbon(model)
    val log = WLabel("Invoked: none").also { it.textWrapping = TextWrapping.WRAP }
    ribbon.addItemInvokedListener { event ->
        val parameter = event.parameter?.let { " (parameter: $it)" }.orEmpty()
        log.text = "Invoked: ${event.item.label ?: event.item.id}$parameter"
    }
    ribbon.addTabChangeListener { event -> log.text = "Tab: ${event.newTab?.label}" }
    ribbon.width = RIBBON_WIDTH

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Display mode"))
    options.add(
        WComboBox(listOf("Classic", "Simplified")).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { model.displayMode = if (combo.selectedIndex == 1) RibbonDisplayMode.SIMPLIFIED else RibbonDisplayMode.CLASSIC }
        },
    )
    options.add(optionsLabel("Visibility"))
    val modes = listOf(RibbonVisibilityMode.ALWAYS_SHOW, RibbonVisibilityMode.TABS_ONLY, RibbonVisibilityMode.FULL_SCREEN)
    options.add(
        WComboBox(listOf("Always show Ribbon", "Show tabs only", "Full-screen mode")).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { model.visibilityMode = modes[combo.selectedIndex.coerceAtLeast(0)] }
        },
    )
    options.add(optionsLabel("Density"))
    options.add(
        WComboBox(listOf("Standard", "Compact", "Touch")).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { model.density = RibbonDensity.entries[combo.selectedIndex.coerceAtLeast(0)] }
        },
    )
    options.add(optionsLabel("Theme (app colors)"))
    options.add(
        WComboBox(RibbonThemePalette.PRESETS.map { it.name }).also { combo ->
            combo.selectedIndex = 0
            combo.addListSelectionListener { WRibbonTheme.applyPalette(RibbonThemePalette.PRESETS[combo.selectedIndex.coerceAtLeast(0)]) }
        },
    )
    options.add(optionsLabel("Tab row color"))
    options.add(
        WComboBox(RibbonChromeStyle.entries.map { it.name }).also { combo ->
            combo.selectedIndex = RibbonChromeStyle.entries.indexOf(WRibbonTheme.chromeStyle)
            combo.addListSelectionListener { WRibbonTheme.applyChromeStyle(RibbonChromeStyle.entries[combo.selectedIndex.coerceAtLeast(0)]) }
        },
    )
    // The language of the strings the ribbon shows itself (ScreenTips, menus, customization, etc.). Shared by all ribbons
    options.add(optionsLabel("Ribbon UI language"))
    val cultures = RibbonStrings.builtInCultures.toList()
    options.add(
        WComboBox(cultures.map { tag -> Locale.forLanguageTag(tag).let { "${it.getDisplayName(it)} ($tag)" } }).also { combo ->
            combo.selectedIndex = cultures.indexOf(RibbonStrings.current.locale.language).coerceAtLeast(0)
            combo.addListSelectionListener { RibbonStrings.current = RibbonStrings.forCulture(cultures[combo.selectedIndex.coerceAtLeast(0)]) }
        },
    )
    options.add(
        WCheckBox("Show the QAT below the ribbon").also { check ->
            check.addItemListener { checked ->
                model.quickAccessPosition = if (checked == true) RibbonQuickAccessPosition.BELOW_RIBBON else RibbonQuickAccessPosition.ABOVE_RIBBON
            }
        },
    )
    options.add(
        WCheckBox("Table Tools (contextual tabs)").also { check ->
            check.addItemListener { checked -> model.setContextualGroupVisible("tableTools", checked == true) }
        },
    )
    val actions = WPanel(spacing = 6.0, orientation = Orientation.HORIZONTAL)
    actions.add(WButton("KeyTip").also { it.addActionListener { ribbon.showKeyTips() } })
    actions.add(WButton("File").also { it.addActionListener { ribbon.isBackstageOpen = true } })
    actions.add(WButton("Customize").also { it.addActionListener { ribbon.showCustomizeDialog(RibbonCustomizePage.RIBBON) } })
    options.add(actions)
    var saved: String? = null
    val stateRow = WPanel(spacing = 6.0, orientation = Orientation.HORIZONTAL)
    stateRow.add(WButton("Save state").also { it.addActionListener { saved = ribbon.saveStateToJson().also { log.text = "Saved the state (${it.length} characters)" } } })
    stateRow.add(WButton("Restore").also { it.addActionListener { saved?.let { json -> log.text = "Restored: ${ribbon.loadStateFromJson(json)}" } } })
    options.add(stateRow)

    val body = WPanel(spacing = 8.0)
    body.add(ribbon)
    body.add(log)
    return buildExample("Word-style ribbon (tabs, groups, galleries, colors, table insertion, QAT, Backstage, contextual tabs)", body, options)
}

/** AutoCAD style: cycling minimize states, floating panels, expanded panels, group-by-group reduction, and the panel visibility menu. */
// A sample that builds the ribbon's content declaratively from top to bottom (splitting it would make it harder to map to Office's ribbon)
@Suppress("LongMethod")
private fun buildCadRibbonExample(): WComponent {
    val model = RibbonModel().apply {
        applicationButtonLabel = "A"
        minimizeBehavior = RibbonMinimizeBehavior.CYCLE_ALL
        tabs.add(
            RibbonTabModel("cadHome", "Home").also { tab ->
                tab.groups.add(
                    group(
                        "draw",
                        "Draw",
                        button("draw.line", "Line", RibbonIcons.LINE, RibbonItemSize.LARGE),
                        button("draw.polyline", "Polyline", RibbonIcons.POLYGON, RibbonItemSize.LARGE),
                        button("draw.circle", "Circle", RibbonIcons.CIRCLE),
                        button("draw.rectangle", "Rectangle", RibbonIcons.RECTANGLE),
                    ).also { g ->
                        g.slideOutItems.add(button("draw.spline", "Spline", RibbonIcons.PEN))
                        g.slideOutItems.add(button("draw.hatch", "Hatch", RibbonIcons.FILL))
                    },
                )
                tab.groups.add(
                    group(
                        "modify",
                        "Modify",
                        button("modify.move", "Move", RibbonIcons.MOVE),
                        button("modify.rotate", "Rotate", RibbonIcons.ROTATE),
                        button("modify.trim", "Trim", RibbonIcons.CROP),
                        RibbonSeparatorModel(),
                        button("modify.erase", "Erase", RibbonIcons.ERASER),
                    ).also { it.reductionOrder = 1 },
                )
                tab.groups.add(
                    group(
                        "layers",
                        "Layers",
                        button("layers.properties", "Layer Properties", RibbonIcons.LAYERS, RibbonItemSize.LARGE),
                        toggle("layers.lock", "Lock", RibbonIcons.LOCK),
                    ).also { it.reductionOrder = 2 },
                )
                tab.groups.add(group("measure", "Measure", button("measure.distance", "Distance", RibbonIcons.MEASURE, RibbonItemSize.LARGE)))
            },
        )
        tabs.add(RibbonTabModel("cadInsert", "Insert").also { tab -> tab.groups.add(group("block", "Block", button("block.insert", "Insert", RibbonIcons.COMPONENT, RibbonItemSize.LARGE))) })
    }
    val ribbon = WRibbon(model)
    ribbon.isMinimizeButtonVisible = true
    ribbon.canFloatGroups = true
    ribbon.isVisibilityMenuEnabled = true
    ribbon.reductionStrategy = RibbonReductionStrategy.GROUP_BY_GROUP
    ribbon.width = CAD_RIBBON_WIDTH
    val log = WLabel("Drag a panel's title to make it a floating panel. The arrow in the [Draw] title opens the expanded panel.")
    log.textWrapping = TextWrapping.WRAP
    ribbon.addGroupFloatingListener { _, group, floating -> log.text = "${group.label}: ${if (floating) "floating panel" else "returned to the ribbon"}" }

    val options = WPanel(spacing = 8.0)
    options.add(
        WCheckBox("CAD look (RibbonThemeStyle.CAD)").also { check ->
            check.addItemListener { checked -> WRibbonTheme.applyStyle(if (checked == true) RibbonThemeStyle.CAD else RibbonThemeStyle.OFFICE) }
        },
    )
    options.add(WButton("Toggle minimize").also { it.addActionListener { ribbon.toggleMinimized() } })
    options.add(WButton("Float [Draw]").also { it.addActionListener { ribbon.floatGroup(model.findGroup("draw")!!) } })
    options.add(WButton("Return all to the ribbon").also { it.addActionListener { ribbon.returnAllPanelsToRibbon() } })
    options.add(optionsLabel("The minimize behavior can also be chosen with the arrow at the top right.").also { it.verticalAlignment = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(ribbon)
    body.add(log)
    return buildExample("AutoCAD-style ribbon (cycling minimize states, floating panels, expanded panels, group-by-group reduction)", body, options)
}

private const val RIBBON_WIDTH = 820.0
private const val CAD_RIBBON_WIDTH = 760.0
