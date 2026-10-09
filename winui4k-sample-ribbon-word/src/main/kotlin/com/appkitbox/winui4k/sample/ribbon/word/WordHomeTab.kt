package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonFontComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonScreenTip
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.dropDown
import com.appkitbox.winui4k.sample.ribbon.common.group
import com.appkitbox.winui4k.sample.ribbon.common.menuItem
import com.appkitbox.winui4k.sample.ribbon.common.radioItem
import com.appkitbox.winui4k.sample.ribbon.common.split
import com.appkitbox.winui4k.sample.ribbon.common.tab
import com.appkitbox.winui4k.sample.ribbon.common.toggle

/** Word's [Home] tab (same layout as HOME in WordPage.xaml). */
internal fun createHomeTab(): RibbonTabModel =
    tab("home", "Home", "H", clipboardGroup(), fontGroup(), paragraphGroup(), stylesGroup(), editingGroup(), voiceGroup(), editorGroup())

private fun clipboardGroup(): RibbonGroupModel = group(
    "clipboard",
    "Clipboard",
    split(
        "paste",
        "Paste",
        RibbonIcons.PASTE,
        RibbonItemSize.LARGE,
        menuItem("paste.keep", "Keep Source Formatting"),
        menuItem("paste.merge", "Merge Formatting"),
        menuItem("paste.text", "Keep Text Only"),
        RibbonMenuSeparatorModel(),
        menuItem("paste.special", "Paste Special..."),
        menuItem("paste.default", "Set Default Paste..."),
    ).also {
        it.shortcut = "Ctrl+V"
        it.keyTip = "V"
        it.screenTip = RibbonScreenTip("Paste", "Add content from the Clipboard to your document.", "Ctrl+V").also { tip -> tip.helpText = "Tell me more" }
    },
    button("cut", "Cut", RibbonIcons.CUT).also {
        it.shortcut = "Ctrl+X"
        it.keyTip = "X"
    },
    button("copy", "Copy", RibbonIcons.COPY).also {
        it.shortcut = "Ctrl+C"
        it.keyTip = "C"
    },
    toggle("formatPainter", "Format Painter", RibbonIcons.FORMAT_PAINTER, RibbonItemSize.MEDIUM).also {
        it.keyTip = "FP"
        it.screenTip = RibbonScreenTip("Format Painter", "Apply the formatting of a selection to other content.")
    },
).also {
    it.icon = RibbonIcons.PASTE
    it.isDialogLauncherVisible = true
    it.reductionOrder = -1
}

@Suppress("LongMethod") // Declaratively builds the two rows of the [Font] group from top to bottom
private fun fontGroup(): RibbonGroupModel = group(
    "font",
    "Font",
    RibbonRowModel(
        RibbonFontComboBoxModel("fontFamily").also {
            it.selectedItem = "Segoe UI"
            it.keyTip = "FF"
        },
        RibbonFontSizeComboBoxModel("fontSize").also {
            it.selectedItem = DEFAULT_FONT_SIZE
            it.keyTip = "FS"
        },
        RibbonButtonGroupModel(
            button("growFont", "Increase Font Size", RibbonIcons.FONT_INCREASE, RibbonItemSize.SMALL).also {
                it.shortcut = "Ctrl+]"
                it.keyTip = "FG"
            },
            button("shrinkFont", "Decrease Font Size", RibbonIcons.FONT_DECREASE, RibbonItemSize.SMALL).also {
                it.shortcut = "Ctrl+["
                it.keyTip = "FK"
            },
        ),
        dropDown(
            "changeCase",
            "Change Case",
            RibbonIcon.text("Aa"),
            RibbonItemSize.SMALL,
            menuItem("case.sentence", "Sentence case."),
            menuItem("case.lower", "lowercase"),
            menuItem("case.upper", "UPPERCASE"),
            menuItem("case.capitalize", "Capitalize Each Word"),
            menuItem("case.toggle", "tOGGLE cASE"),
        ).also { it.keyTip = "7" },
        button("clearFormatting", "Clear All Formatting", RibbonIcons.CLEAR_FORMATTING, RibbonItemSize.SMALL).also { it.keyTip = "E" },
    ),
    RibbonRowModel(
        RibbonButtonGroupModel(
            format("bold", "Bold", RibbonIcons.BOLD, "Ctrl+B", "1", "Make your text bold."),
            format("italic", "Italic", RibbonIcons.ITALIC, "Ctrl+I", "2", "Italicize your text."),
            format("underline", "Underline", RibbonIcons.UNDERLINE, "Ctrl+U", "3", "Underline your text."),
            format("strikethrough", "Strikethrough", RibbonIcons.STRIKETHROUGH, null, "4"),
            format("subscript", "Subscript", RibbonIcons.SUBSCRIPT, "Ctrl+=", "5").also { it.groupName = "script" },
            format("superscript", "Superscript", RibbonIcons.SUPERSCRIPT, "Ctrl+Shift+=", "6").also { it.groupName = "script" },
        ),
        RibbonColorPickerModel("highlight", "Text Highlight Color", RibbonIcons.HIGHLIGHT).also {
            it.selectedColor = RibbonColor.parse("#FFFF00")
            it.showNoColor = true
            it.keyTip = "I"
        },
        RibbonColorPickerModel("fontColor", "Font Color", RibbonIcons.FONT_COLOR).also {
            it.selectedColor = RibbonColor.parse("#C00000")
            it.keyTip = "FC"
        },
    ),
).also {
    it.icon = RibbonIcons.FONT
    it.itemsLayout = RibbonGroupItemsLayout.ROWS
    it.rowCount = 2
    it.isDialogLauncherVisible = true
}

@Suppress("LongParameterList") // Lists a format toggle's attributes (id, label, icon, shortcut, KeyTip, description) on one line
private fun format(id: String, label: String, icon: RibbonIcon, shortcut: String?, keyTip: String, description: String? = null): RibbonToggleButtonModel =
    toggle(id, label, icon).also {
        it.shortcut = shortcut
        it.keyTip = keyTip
        if (description != null) it.screenTip = RibbonScreenTip(label, description, shortcut)
    }

@Suppress("LongMethod") // Declaratively builds the two rows of the [Paragraph] group from top to bottom
private fun paragraphGroup(): RibbonGroupModel = group(
    "paragraph",
    "Paragraph",
    RibbonRowModel(
        split(
            "bullets",
            "Bullets",
            RibbonIcons.BULLETS,
            RibbonItemSize.SMALL,
            menuItem("bullets.bullet", "• Filled Circle"),
            menuItem("bullets.circle", "○ Hollow Circle"),
            menuItem("bullets.square", "■ Square"),
            RibbonMenuSeparatorModel(),
            menuItem("bullets.define", "Define New Bullet..."),
        ).also {
            it.isCheckable = true
            it.keyTip = "U"
        },
        split(
            "numbering",
            "Numbering",
            RibbonIcons.NUMBERING,
            RibbonItemSize.SMALL,
            menuItem("numbering.arabic", "1. 2. 3."),
            menuItem("numbering.paren", "1) 2) 3)"),
            menuItem("numbering.roman", "I. II. III."),
            menuItem("numbering.alpha", "A. B. C."),
        ).also {
            it.isCheckable = true
            it.keyTip = "N"
        },
        RibbonButtonGroupModel(
            button("outdent", "Decrease Indent", RibbonIcons.INDENT_DECREASE, RibbonItemSize.SMALL).also { it.keyTip = "AO" },
            button("indent", "Increase Indent", RibbonIcons.INDENT_INCREASE, RibbonItemSize.SMALL).also { it.keyTip = "AI" },
        ),
        button("sort", "Sort", RibbonIcons.SORT, RibbonItemSize.SMALL).also { it.keyTip = "SO" },
        toggle("showMarks", "Show/Hide", RibbonIcon.text("¶")).also {
            it.shortcut = "Ctrl+Shift+8"
            it.keyTip = "8"
        },
    ),
    RibbonRowModel(
        RibbonButtonGroupModel(
            align("alignLeft", "Align Left", RibbonIcons.ALIGN_LEFT, "Ctrl+L", "AL").also { it.isChecked = true },
            align("alignCenter", "Center", RibbonIcons.ALIGN_CENTER, "Ctrl+E", "AC"),
            align("alignRight", "Align Right", RibbonIcons.ALIGN_RIGHT, "Ctrl+R", "AR"),
            align("justify", "Justify", RibbonIcons.ALIGN_JUSTIFY, "Ctrl+J", "AJ"),
        ),
        dropDown(
            "lineSpacing",
            "Line and Paragraph Spacing",
            glyph('\uE9E9'),
            RibbonItemSize.SMALL,
            radioItem("spacing.1.0", "1.0", "spacing"),
            radioItem("spacing.1.15", "1.15", "spacing", checked = true),
            radioItem("spacing.1.5", "1.5", "spacing"),
            radioItem("spacing.2.0", "2.0", "spacing"),
        ).also { it.keyTip = "K" },
        RibbonColorPickerModel("shading", "Shading", RibbonIcons.SHADING).also {
            it.selectedColor = RibbonColor.parse("#DEEBF7")
            it.showNoColor = true
            it.showAutomatic = false
            it.keyTip = "H"
        },
        split(
            "borders",
            "Borders",
            RibbonIcons.BORDERS,
            RibbonItemSize.SMALL,
            menuItem("borders.bottom", "Bottom Border"),
            menuItem("borders.top", "Top Border"),
            menuItem("borders.none", "No Border"),
            menuItem("borders.all", "All Borders"),
        ).also { it.keyTip = "B" },
    ),
).also {
    it.icon = RibbonIcons.BULLETS
    it.itemsLayout = RibbonGroupItemsLayout.ROWS
    it.rowCount = 2
    it.isDialogLauncherVisible = true
}

private fun align(id: String, label: String, icon: RibbonIcon, shortcut: String, keyTip: String): RibbonToggleButtonModel =
    toggle(id, label, icon).also {
        it.groupName = "align"
        it.shortcut = shortcut
        it.keyTip = keyTip
    }

/** The [Styles] gallery (live preview, categories, filter, and footer menu). Values are the style names applied to the body text. */
private fun stylesGroup(): RibbonGroupModel = group(
    "styles",
    "Styles",
    RibbonGalleryModel("stylesGallery", "Styles").also { gallery ->
        gallery.icon = glyph('\uE790')
        gallery.itemWidth = STYLE_ITEM_WIDTH
        gallery.maxColumns = STYLE_MAX_COLUMNS
        gallery.minColumns = STYLE_MIN_COLUMNS
        gallery.isFilterEnabled = true
        gallery.keyTip = "L"
        for (style in WordStyle.entries) {
            gallery.items.add(
                RibbonGalleryItemModel(style.name, style.label, category = style.category, value = style).also {
                    it.previewText = style.previewText
                    it.previewFontSize = style.previewFontSize
                    it.previewForeground = style.previewForeground?.let(RibbonColor::parse)
                    it.previewItalic = style.isItalic
                    it.previewBold = style == WordStyle.STRONG
                },
            )
        }
        gallery.selectedItem = gallery.items[0]
        gallery.menuItems.add(menuItem("styles.create", "Create a Style", glyph('')))
        gallery.menuItems.add(menuItem("styles.clear", "Clear Formatting", RibbonIcons.CLEAR_FORMATTING))
        gallery.menuItems.add(menuItem("styles.apply", "Apply Styles...", glyph('')))
    },
).also {
    it.icon = glyph('\uE790')
    it.isDialogLauncherVisible = true
    it.reductionOrder = 1
}

private fun editingGroup(): RibbonGroupModel = group(
    "editing",
    "Editing",
    dropDown(
        "find",
        "Find",
        RibbonIcons.FIND,
        RibbonItemSize.MEDIUM,
        menuItem("find.find", "Find").also { it.shortcut = "Ctrl+F" },
        menuItem("find.advanced", "Advanced Find..."),
        menuItem("find.goto", "Go To...").also { it.shortcut = "Ctrl+G" },
    ).also { it.keyTip = "FD" },
    button("replace", "Replace", RibbonIcons.REPLACE).also {
        it.shortcut = "Ctrl+H"
        it.keyTip = "R"
    },
    dropDown(
        "select",
        "Select",
        RibbonIcons.SELECT_ALL,
        RibbonItemSize.MEDIUM,
        menuItem("select.all", "Select All").also { it.shortcut = "Ctrl+A" },
        menuItem("select.objects", "Select Objects"),
        menuItem("select.pane", "Selection Pane..."),
    ).also { it.keyTip = "SL" },
).also { it.icon = RibbonIcons.FIND }

private fun voiceGroup(): RibbonGroupModel = group(
    "voice",
    "Voice",
    split("dictate", "Dictate", glyph(''), RibbonItemSize.LARGE, menuItem("dictate.ja", "Japanese"), menuItem("dictate.en", "English (United States)")).also { it.keyTip = "D" },
).also { it.icon = glyph('\uE720') }

private fun editorGroup(): RibbonGroupModel = group(
    "editor",
    "Editor",
    button("editorPane", "Editor", glyph(''), RibbonItemSize.LARGE).also { it.keyTip = "G" },
).also { it.icon = glyph('\uE8FB') }

private const val DEFAULT_FONT_SIZE = 16.0
private const val STYLE_ITEM_WIDTH = 76.0
private const val STYLE_MAX_COLUMNS = 6
private const val STYLE_MIN_COLUMNS = 3
