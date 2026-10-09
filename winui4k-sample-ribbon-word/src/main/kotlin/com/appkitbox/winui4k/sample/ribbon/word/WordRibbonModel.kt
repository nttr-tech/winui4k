package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel
import com.appkitbox.winui4k.ribbon.RibbonBackstagePlacement
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
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
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonScreenTip
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.group
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/**
 * The Word ribbon model (same layout as word.png in the RibbonSpace README): [File] [Home] [Insert] [Design] [Layout]
 * [Review] [View] [Help], the [Table Tools] contextual tabs, [Comments] [Editing] [Share] on the tab row, the QAT, and
 * the Backstage.
 */
@Suppress("LongMethod") // Declaratively builds the whole Word ribbon from top to bottom
internal fun createWordModel(): RibbonModel = RibbonModel().apply {
    applicationButtonLabel = "File"
    title = "Quarterly Report"
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(simpleTab("design", "Design", "G", group("formatting", "Document Formatting", button("design.themes", "Themes", RibbonIcons.THEME, RibbonItemSize.LARGE))))
    tabs.add(simpleTab("layout", "Layout", "P", group("pageSetup", "Page Setup", button("layout.margins", "Margins", RibbonIcons.PAGE, RibbonItemSize.LARGE))))
    tabs.add(simpleTab("review", "Review", "R", group("proofing", "Proofing", button("review.spelling", "Spelling & Grammar", RibbonIcons.SPELLING, RibbonItemSize.LARGE))))
    tabs.add(
        simpleTab(
            "view",
            "View",
            "W",
            group(
                "views",
                "Views",
                toggle("view.read", "Read Mode", RibbonIcons.READING_MODE, RibbonItemSize.LARGE),
                toggle("view.print", "Print Layout", RibbonIcons.PAGE, RibbonItemSize.LARGE).also { it.isChecked = true },
                RibbonCheckBoxModel("view.ruler", "Ruler"),
                RibbonCheckBoxModel("view.gridlines", "Gridlines"),
            ),
        ),
    )
    tabs.add(simpleTab("help", "Help", "Y", group("helpGroup", "Help", button("help.help", "Help", RibbonIcons.HELP, RibbonItemSize.LARGE))))
    contextualGroups.add(RibbonContextualGroupModel("tableTools", "Table Tools").also { it.activation = RibbonContextualActivation.SELECT_ON_SHOW })
    tabs.add(
        simpleTab("tableDesign", "Table Design", null, group("tableStyles", "Table Styles", button("table.shading", "Shading", RibbonIcons.SHADING, RibbonItemSize.LARGE)))
            .also { it.contextualGroupId = "tableTools" },
    )
    tabs.add(
        simpleTab("tableLayout", "Layout", null, group("rowsColumns", "Rows & Columns", button("table.insertAbove", "Insert Above", RibbonIcons.INSERT_ROW, RibbonItemSize.LARGE)))
            .also { it.contextualGroupId = "tableTools" },
    )
    quickAccessItems.add(button("qat.save", "Save", RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("qat.undo", "Undo", RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("qat.redo", "Redo", RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    quickAccessCandidates.addAll(listOf(findItem("paste")!!, findItem("bold")!!, findItem("insert.table")!!, findItem("find")!!))
    tabStripItems.add(button("comments", "Comments", RibbonIcons.COMMENT))
    tabStripItems.add(
        RibbonDropDownButtonModel("editingMode", "Editing").also { mode ->
            mode.icon = RibbonIcons.PEN
            mode.menuItems.add(
                menuItem("mode.editing", "Editing", RibbonIcons.PEN).also {
                    it.isCheckable = true
                    it.groupName = "mode"
                    it.isChecked = true
                },
            )
            mode.menuItems.add(
                menuItem("mode.reviewing", "Reviewing", RibbonIcons.TRACK_CHANGES).also {
                    it.isCheckable = true
                    it.groupName = "mode"
                },
            )
            mode.menuItems.add(
                menuItem("mode.viewing", "Viewing", RibbonIcons.VIEW).also {
                    it.isCheckable = true
                    it.groupName = "mode"
                },
            )
        },
    )
    tabStripItems.add(button("share", "Share", RibbonIcons.SHARE))
    createBackstage(this)
}

private fun simpleTab(id: String, label: String, keyTip: String?, vararg groups: RibbonGroupModel) =
    RibbonTabModel(id, label).also { tab ->
        tab.keyTip = keyTip
        groups.forEach { tab.groups.add(it) }
    }

private fun createBackstage(model: RibbonModel) {
    val backstage = model.backstage
    backstage.title = "Word"
    backstage.items.add(RibbonBackstageItemModel("home", "Home", RibbonIcons.HOME, WLabel("Recent: Quarterly Report.docx, Meeting Minutes.docx")))
    backstage.items.add(RibbonBackstageItemModel("new", "New", RibbonIcons.NEW_DOCUMENT, WLabel("Create from a blank document or a template.")))
    backstage.items.add(RibbonBackstageItemModel("open", "Open", RibbonIcons.OPEN, WLabel("Open from recent files, This PC, or OneDrive.")))
    backstage.items.add(RibbonBackstageItemModel("info", "Info", RibbonIcons.INFO, WLabel("Protect and inspect the document, and view its version history.")).also { it.hasSeparatorBefore = true })
    backstage.items.add(RibbonBackstageItemModel("save", "Save", RibbonIcons.SAVE))
    backstage.items.add(RibbonBackstageItemModel("saveAs", "Save As", RibbonIcons.SAVE_AS, WLabel("Choose where to save.")))
    backstage.items.add(RibbonBackstageItemModel("print", "Print", RibbonIcons.PRINT, WLabel("Printer and print settings.")))
    backstage.items.add(RibbonBackstageItemModel("share", "Share", RibbonIcons.SHARE, WLabel("Share with other people.")))
    backstage.items.add(RibbonBackstageItemModel("account", "Account", RibbonIcons.ACCOUNT, WLabel("The signed-in user.")).also { it.placement = RibbonBackstagePlacement.BOTTOM })
    backstage.items.add(RibbonBackstageItemModel("options", "Options", RibbonIcons.SETTINGS).also { it.placement = RibbonBackstagePlacement.BOTTOM })
}

@Suppress("LongMethod", "CyclomaticComplexMethod") // Declaratively builds Word's [Home] tab from top to bottom
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
                paste.keyTip = "V"
                paste.screenTip = RibbonScreenTip("Paste", "Add content from the Clipboard to your document.", "Ctrl+V")
                paste.menuItems.add(menuItem("paste.keep", "Keep Source Formatting", RibbonIcons.PASTE))
                paste.menuItems.add(menuItem("paste.merge", "Merge Formatting", RibbonIcons.FORMAT_PAINTER))
                paste.menuItems.add(menuItem("paste.text", "Keep Text Only", RibbonIcons.TEXT))
                paste.menuItems.add(RibbonMenuSeparatorModel())
                paste.menuItems.add(menuItem("paste.special", "Paste Special..."))
            },
            button("cut", "Cut", RibbonIcons.CUT).also {
                it.shortcut = "Ctrl+X"
                it.keyTip = "X"
            },
            button("copy", "Copy", RibbonIcons.COPY).also {
                it.shortcut = "Ctrl+C"
                it.keyTip = "C"
            },
            button("formatPainter", "Format Painter", RibbonIcons.FORMAT_PAINTER).also { it.keyTip = "FP" },
        ).also { it.isDialogLauncherVisible = true },
    )
    tab.groups.add(
        group(
            "font",
            "Font",
            RibbonRowModel(
                RibbonFontComboBoxModel("font.name").also { it.selectedItem = "Segoe UI" },
                RibbonFontSizeComboBoxModel("font.size").also { it.selectedItem = 16.0 },
                RibbonButtonGroupModel(
                    button("font.grow", "Increase Font Size", RibbonIcons.FONT_INCREASE, RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+]" },
                    button("font.shrink", "Decrease Font Size", RibbonIcons.FONT_DECREASE, RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+[" },
                ),
                RibbonDropDownButtonModel("font.case", "Change Case").also { case ->
                    case.icon = RibbonIcons.CHANGE_CASE
                    case.size = RibbonItemSize.SMALL
                    case.menuItems.add(menuItem("case.sentence", "Sentence case."))
                    case.menuItems.add(menuItem("case.upper", "UPPERCASE"))
                    case.menuItems.add(menuItem("case.lower", "lowercase"))
                },
                button("font.clear", "Clear All Formatting", RibbonIcons.CLEAR_FORMATTING, RibbonItemSize.SMALL),
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("bold", "Bold", RibbonIcons.BOLD).also {
                        it.shortcut = "Ctrl+B"
                        it.keyTip = "1"
                    },
                    toggle("italic", "Italic", RibbonIcons.ITALIC).also {
                        it.shortcut = "Ctrl+I"
                        it.keyTip = "2"
                    },
                    toggle("underline", "Underline", RibbonIcons.UNDERLINE).also {
                        it.shortcut = "Ctrl+U"
                        it.keyTip = "3"
                    },
                    toggle("strikethrough", "Strikethrough", RibbonIcons.STRIKETHROUGH),
                    toggle("subscript", "Subscript", RibbonIcons.SUBSCRIPT),
                ),
                button("font.effects", "Text Effects", RibbonIcons.EFFECTS, RibbonItemSize.SMALL),
                RibbonColorPickerModel("font.highlight", "Text Highlight Color", RibbonIcons.HIGHLIGHT).also { it.showNoColor = true },
                RibbonColorPickerModel("font.color", "Font Color", RibbonIcons.FONT_COLOR),
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
                RibbonSplitButtonModel("bullets", "Bullets").also {
                    it.icon = RibbonIcons.BULLETS
                    it.size = RibbonItemSize.SMALL
                    it.menuItems.add(menuItem("bullets.dot", "Filled Circle"))
                },
                RibbonSplitButtonModel("numbering", "Numbering").also {
                    it.icon = RibbonIcons.NUMBERING
                    it.size = RibbonItemSize.SMALL
                    it.menuItems.add(menuItem("numbering.123", "1. 2. 3."))
                },
                button("indent.decrease", "Decrease Indent", RibbonIcons.INDENT_DECREASE, RibbonItemSize.SMALL),
                button("indent.increase", "Increase Indent", RibbonIcons.INDENT_INCREASE, RibbonItemSize.SMALL),
                button("sort", "Sort", RibbonIcons.SORT, RibbonItemSize.SMALL),
                toggle("showFormatting", "Show/Hide", RibbonIcons.SHOW_FORMATTING),
            ),
            RibbonRowModel(
                RibbonButtonGroupModel(
                    toggle("align.left", "Align Left", RibbonIcons.ALIGN_LEFT).also {
                        it.groupName = "align"
                        it.isChecked = true
                        it.shortcut = "Ctrl+L"
                    },
                    toggle("align.center", "Center", RibbonIcons.ALIGN_CENTER).also {
                        it.groupName = "align"
                        it.shortcut = "Ctrl+E"
                    },
                    toggle("align.right", "Align Right", RibbonIcons.ALIGN_RIGHT).also {
                        it.groupName = "align"
                        it.shortcut = "Ctrl+R"
                    },
                    toggle("align.justify", "Justify", RibbonIcons.ALIGN_JUSTIFY).also {
                        it.groupName = "align"
                        it.shortcut = "Ctrl+J"
                    },
                ),
                RibbonSpinnerModel("lineSpacing", "Line Spacing", 1.0).also {
                    it.increment = 0.5
                    it.minimum = 1.0
                    it.maximum = 3.0
                    it.inputWidth = 52.0
                },
                RibbonColorPickerModel("shading", "Shading", RibbonIcons.SHADING).also { it.showNoColor = true },
                RibbonDropDownButtonModel("borders", "Borders").also {
                    it.icon = RibbonIcons.BORDERS
                    it.size = RibbonItemSize.SMALL
                    it.menuItems.add(menuItem("borders.all", "All Borders"))
                },
            ),
        ).also {
            it.itemsLayout = RibbonGroupItemsLayout.ROWS
            it.rowCount = 2
            it.isDialogLauncherVisible = true
        },
    )
    tab.groups.add(createStylesGroup())
    tab.groups.add(
        group(
            "editing",
            "Editing",
            RibbonDropDownButtonModel("find", "Find").also { find ->
                find.icon = RibbonIcons.FIND
                find.shortcut = "Ctrl+F"
                find.menuItems.add(menuItem("find.find", "Find", RibbonIcons.FIND))
                find.menuItems.add(menuItem("find.advanced", "Advanced Find..."))
                find.menuItems.add(menuItem("find.goto", "Go To..."))
            },
            button("replace", "Replace", RibbonIcons.REPLACE).also { it.shortcut = "Ctrl+H" },
            RibbonDropDownButtonModel("select", "Select").also { select ->
                select.icon = RibbonIcons.SELECT
                select.menuItems.add(menuItem("select.all", "Select All", RibbonIcons.SELECT_ALL))
                select.menuItems.add(menuItem("select.objects", "Select Objects", RibbonIcons.POINTER))
            },
        ).also { it.reductionOrder = 2 },
    )
    tab.groups.add(
        group(
            "voice",
            "Voice",
            RibbonSplitButtonModel("dictate", "Dictate").also {
                it.icon = RibbonIcons.RECORD
                it.size = RibbonItemSize.LARGE
                it.menuItems.add(menuItem("dictate.lang", "Japanese"))
            },
        ).also { it.reductionOrder = 3 },
    )
    tab.groups.add(group("editor", "Editor", button("editor.editor", "Editor", RibbonIcons.CHECKMARK, RibbonItemSize.LARGE)).also { it.reductionOrder = 3 })
}

/** The [Styles] gallery (live preview, categories, filter, and footer menu). */
private fun createStylesGroup(): RibbonGroupModel = group(
    "styles",
    "Styles",
    RibbonGalleryModel("styles.gallery", "Styles").also { gallery ->
        gallery.icon = RibbonIcons.FONT
        gallery.itemWidth = 88.0
        gallery.itemHeight = 56.0
        gallery.maxColumns = 3
        gallery.minColumns = 2
        gallery.dropDownColumns = 4
        gallery.isFilterEnabled = true
        val styles = listOf(
            Triple("Normal", 14.0, false),
            Triple("No Spacing", 14.0, false),
            Triple("Heading 1", 18.0, true),
            Triple("Heading 2", 15.0, true),
            Triple("Title", 22.0, false),
            Triple("Subtitle", 13.0, false),
            Triple("Quote", 13.0, false),
            Triple("Emphasis", 13.0, false),
        )
        styles.forEachIndexed { index, (name, size, bold) ->
            gallery.items.add(
                RibbonGalleryItemModel("style$index", name, category = if (index < 4) "Common Styles" else "More Styles").also {
                    it.previewText = "AaBbCcDd"
                    it.previewFontSize = size
                    it.previewBold = bold
                    it.previewItalic = name == "Emphasis"
                    if (index in 2..4) it.previewForeground = com.appkitbox.winui4k.ribbon.RibbonColor.parse("#1F4E99")
                },
            )
        }
        gallery.selectedItem = gallery.items[0]
        gallery.menuItems.add(menuItem("styles.create", "Create a Style", RibbonIcons.ADD))
        gallery.menuItems.add(menuItem("styles.clear", "Clear Formatting", RibbonIcons.CLEAR_FORMATTING))
        gallery.menuItems.add(menuItem("styles.apply", "Apply Styles..."))
    },
).also {
    it.reductionOrder = 1
    it.isDialogLauncherVisible = true
}

private fun createInsertTab(): RibbonTabModel = RibbonTabModel("insert", "Insert").also { tab ->
    tab.keyTip = "N"
    tab.groups.add(group("pages", "Pages", button("insert.cover", "Cover Page", RibbonIcons.PAGE), button("insert.blank", "Blank Page", RibbonIcons.NEW_DOCUMENT), button("insert.break", "Page Break", RibbonIcons.PAGE_BREAK)))
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
                table.menuItems.add(menuItem("insert.drawTable", "Draw Table", RibbonIcons.PEN))
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
    tab.groups.add(group("links", "Links", button("insert.link", "Link", RibbonIcons.LINK, RibbonItemSize.LARGE), button("insert.bookmark", "Bookmark", RibbonIcons.BOOKMARK)))
    tab.groups.add(group("comments", "Comments", button("insert.comment", "Comment", RibbonIcons.COMMENT, RibbonItemSize.LARGE)))
    tab.groups.add(group("symbols", "Symbols", button("insert.equation", "Equation", RibbonIcons.EQUATION, RibbonItemSize.LARGE), button("insert.symbol", "Symbol", RibbonIcons.SYMBOL, RibbonItemSize.LARGE)))
}
