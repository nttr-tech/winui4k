package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRowModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonScreenTip
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel
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
    tab("home", Messages.get("common.home"), "H", clipboardGroup(), fontGroup(), paragraphGroup(), stylesGroup(), editingGroup(), voiceGroup(), editorGroup())

private fun clipboardGroup(): RibbonGroupModel = group(
    "clipboard",
    Messages.get("common.clipboard"),
    split(
        "paste",
        Messages.get("common.paste"),
        RibbonIcons.PASTE,
        RibbonItemSize.LARGE,
        menuItem("paste.keep", Messages.get("common.paste.keep")),
        menuItem("paste.merge", Messages.get("home.paste.merge")),
        menuItem("paste.text", Messages.get("common.paste.text")),
        RibbonMenuSeparatorModel(),
        menuItem("paste.special", Messages.get("common.paste.special")),
        menuItem("paste.default", Messages.get("home.paste.default")),
    ).also {
        it.shortcut = "Ctrl+V"
        it.keyTip = "V"
        it.screenTip = RibbonScreenTip(Messages.get("common.paste"), Messages.get("home.paste.tip"), "Ctrl+V").also { tip -> tip.helpText = Messages.get("home.paste.tip.help") }
    },
    button("cut", Messages.get("common.cut"), RibbonIcons.CUT).also {
        it.shortcut = "Ctrl+X"
        it.keyTip = "X"
    },
    button("copy", Messages.get("common.copy"), RibbonIcons.COPY).also {
        it.shortcut = "Ctrl+C"
        it.keyTip = "C"
    },
    toggle("formatPainter", Messages.get("common.format.painter"), RibbonIcons.FORMAT_PAINTER, RibbonItemSize.MEDIUM).also {
        it.keyTip = "FP"
        it.screenTip = RibbonScreenTip(Messages.get("common.format.painter"), Messages.get("home.format.painter.tip"))
    },
).also {
    it.icon = RibbonIcons.PASTE
    it.isDialogLauncherVisible = true
    it.reductionOrder = -1
}

@Suppress("LongMethod") // Declaratively builds the two rows of the [Font] group from top to bottom
private fun fontGroup(): RibbonGroupModel = group(
    "font",
    Messages.get("common.font"),
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
            button("growFont", Messages.get("common.font.grow"), RibbonIcons.FONT_INCREASE, RibbonItemSize.SMALL).also {
                it.shortcut = "Ctrl+]"
                it.keyTip = "FG"
            },
            button("shrinkFont", Messages.get("common.font.shrink"), RibbonIcons.FONT_DECREASE, RibbonItemSize.SMALL).also {
                it.shortcut = "Ctrl+["
                it.keyTip = "FK"
            },
        ),
        dropDown(
            "changeCase",
            Messages.get("home.change.case"),
            RibbonIcon.text("Aa"),
            RibbonItemSize.SMALL,
            menuItem("case.sentence", Messages.get("home.case.sentence")),
            menuItem("case.lower", Messages.get("home.case.lower")),
            menuItem("case.upper", Messages.get("home.case.upper")),
            menuItem("case.capitalize", Messages.get("home.case.capitalize")),
            menuItem("case.toggle", Messages.get("home.case.toggle")),
        ).also { it.keyTip = "7" },
        button("clearFormatting", Messages.get("home.clear.formatting"), RibbonIcons.CLEAR_FORMATTING, RibbonItemSize.SMALL).also { it.keyTip = "E" },
    ),
    RibbonRowModel(
        RibbonButtonGroupModel(
            format("bold", Messages.get("common.bold"), RibbonIcons.BOLD, "Ctrl+B", "1", Messages.get("home.bold.tip")),
            format("italic", Messages.get("common.italic"), RibbonIcons.ITALIC, "Ctrl+I", "2", Messages.get("home.italic.tip")),
            format("underline", Messages.get("common.underline"), RibbonIcons.UNDERLINE, "Ctrl+U", "3", Messages.get("home.underline.tip")),
            format("strikethrough", Messages.get("home.strikethrough"), RibbonIcons.STRIKETHROUGH, null, "4"),
            format("subscript", Messages.get("home.subscript"), RibbonIcons.SUBSCRIPT, "Ctrl+=", "5").also { it.groupName = "script" },
            format("superscript", Messages.get("home.superscript"), RibbonIcons.SUPERSCRIPT, "Ctrl+Shift+=", "6").also { it.groupName = "script" },
        ),
        RibbonColorPickerModel("highlight", Messages.get("home.highlight"), RibbonIcons.HIGHLIGHT).also {
            it.selectedColor = RibbonColor.parse("#FFFF00")
            it.showNoColor = true
            it.keyTip = "I"
        },
        RibbonColorPickerModel("fontColor", Messages.get("common.font.color"), RibbonIcons.FONT_COLOR).also {
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
    Messages.get("common.paragraph"),
    RibbonRowModel(
        split(
            "bullets",
            Messages.get("common.bullets"),
            RibbonIcons.BULLETS,
            RibbonItemSize.SMALL,
            menuItem("bullets.bullet", Messages.get("common.bullets.dot")),
            menuItem("bullets.circle", Messages.get("home.bullets.circle")),
            menuItem("bullets.square", Messages.get("common.bullets.square")),
            RibbonMenuSeparatorModel(),
            menuItem("bullets.define", Messages.get("home.bullets.define")),
        ).also {
            it.isCheckable = true
            it.keyTip = "U"
        },
        split(
            "numbering",
            Messages.get("home.numbering"),
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
            button("outdent", Messages.get("home.outdent"), RibbonIcons.INDENT_DECREASE, RibbonItemSize.SMALL).also { it.keyTip = "AO" },
            button("indent", Messages.get("home.indent"), RibbonIcons.INDENT_INCREASE, RibbonItemSize.SMALL).also { it.keyTip = "AI" },
        ),
        button("sort", Messages.get("home.sort"), RibbonIcons.SORT, RibbonItemSize.SMALL).also { it.keyTip = "SO" },
        toggle("showMarks", Messages.get("home.show.marks"), RibbonIcon.text("¶")).also {
            it.shortcut = "Ctrl+Shift+8"
            it.keyTip = "8"
        },
    ),
    RibbonRowModel(
        RibbonButtonGroupModel(
            align("alignLeft", Messages.get("common.align.left"), RibbonIcons.ALIGN_LEFT, "Ctrl+L", "AL").also { it.isChecked = true },
            align("alignCenter", Messages.get("common.align.center"), RibbonIcons.ALIGN_CENTER, "Ctrl+E", "AC"),
            align("alignRight", Messages.get("common.align.right"), RibbonIcons.ALIGN_RIGHT, "Ctrl+R", "AR"),
            align("justify", Messages.get("home.justify"), RibbonIcons.ALIGN_JUSTIFY, "Ctrl+J", "AJ"),
        ),
        dropDown(
            "lineSpacing",
            Messages.get("home.line.spacing"),
            glyph('\uE9E9'),
            RibbonItemSize.SMALL,
            radioItem("spacing.1.0", "1.0", "spacing"),
            radioItem("spacing.1.15", "1.15", "spacing", checked = true),
            radioItem("spacing.1.5", "1.5", "spacing"),
            radioItem("spacing.2.0", "2.0", "spacing"),
        ).also { it.keyTip = "K" },
        RibbonColorPickerModel("shading", Messages.get("home.shading"), RibbonIcons.SHADING).also {
            it.selectedColor = RibbonColor.parse("#DEEBF7")
            it.showNoColor = true
            it.showAutomatic = false
            it.keyTip = "H"
        },
        split(
            "borders",
            Messages.get("common.borders"),
            RibbonIcons.BORDERS,
            RibbonItemSize.SMALL,
            menuItem("borders.bottom", Messages.get("common.borders.bottom")),
            menuItem("borders.top", Messages.get("common.borders.top")),
            menuItem("borders.none", Messages.get("common.borders.none")),
            menuItem("borders.all", Messages.get("common.borders.all")),
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
    Messages.get("common.styles"),
    RibbonGalleryModel("stylesGallery", Messages.get("common.styles")).also { gallery ->
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
        gallery.menuItems.add(menuItem("styles.create", Messages.get("home.styles.create"), glyph('\uE710')))
        gallery.menuItems.add(menuItem("styles.clear", Messages.get("home.styles.clear"), RibbonIcons.CLEAR_FORMATTING))
        gallery.menuItems.add(menuItem("styles.apply", Messages.get("home.styles.apply"), glyph('\uE790')))
    },
).also {
    it.icon = glyph('\uE790')
    it.isDialogLauncherVisible = true
    it.reductionOrder = 1
}

private fun editingGroup(): RibbonGroupModel = group(
    "editing",
    Messages.get("common.editing"),
    dropDown(
        "find",
        Messages.get("home.find"),
        RibbonIcons.FIND,
        RibbonItemSize.MEDIUM,
        menuItem("find.find", Messages.get("home.find")).also { it.shortcut = "Ctrl+F" },
        menuItem("find.advanced", Messages.get("home.find.advanced")),
        menuItem("find.goto", Messages.get("common.find.goto")).also { it.shortcut = "Ctrl+G" },
    ).also { it.keyTip = "FD" },
    button("replace", Messages.get("home.replace"), RibbonIcons.REPLACE).also {
        it.shortcut = "Ctrl+H"
        it.keyTip = "R"
    },
    dropDown(
        "select",
        Messages.get("home.select"),
        RibbonIcons.SELECT_ALL,
        RibbonItemSize.MEDIUM,
        menuItem("select.all", Messages.get("home.select.all")).also { it.shortcut = "Ctrl+A" },
        menuItem("select.objects", Messages.get("home.select.objects")),
        menuItem("select.pane", Messages.get("home.select.pane")),
    ).also { it.keyTip = "SL" },
).also { it.icon = RibbonIcons.FIND }

private fun voiceGroup(): RibbonGroupModel = group(
    "voice",
    Messages.get("home.voice"),
    split("dictate", Messages.get("home.dictate"), glyph('\uE720'), RibbonItemSize.LARGE, menuItem("dictate.ja", Messages.get("home.dictate.ja")), menuItem("dictate.en", Messages.get("home.dictate.en"))).also { it.keyTip = "D" },
).also { it.icon = glyph('\uE720') }

private fun editorGroup(): RibbonGroupModel = group(
    "editor",
    Messages.get("home.editor"),
    button("editorPane", Messages.get("home.editor"), glyph('\uE8FB'), RibbonItemSize.LARGE).also { it.keyTip = "G" },
).also { it.icon = glyph('\uE8FB') }

private const val DEFAULT_FONT_SIZE = 16.0
private const val STYLE_ITEM_WIDTH = 76.0
private const val STYLE_MAX_COLUMNS = 6
private const val STYLE_MIN_COLUMNS = 3
