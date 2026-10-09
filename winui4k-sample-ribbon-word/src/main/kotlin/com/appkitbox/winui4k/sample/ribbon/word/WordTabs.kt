package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonLabelModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.dropDown
import com.appkitbox.winui4k.sample.ribbon.common.group
import com.appkitbox.winui4k.sample.ribbon.common.menuItem
import com.appkitbox.winui4k.sample.ribbon.common.radioItem
import com.appkitbox.winui4k.sample.ribbon.common.split
import com.appkitbox.winui4k.sample.ribbon.common.tab
import com.appkitbox.winui4k.sample.ribbon.common.toggle

// Word's [Insert] [Design] [Layout] [Review] [View] [Help] tabs (same layout as WordPage.xaml)

private val LARGE = RibbonItemSize.LARGE
private val MEDIUM = RibbonItemSize.MEDIUM
private val SMALL = RibbonItemSize.SMALL

/** The [Insert] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declaratively builds the [Insert] tab's groups from top to bottom (the only branches are in the also lambdas)
internal fun createInsertTab(): RibbonTabModel = tab(
    "insert",
    "Insert",
    "N",
    group(
        "pages",
        "Pages",
        dropDown("coverPage", "Cover Page", glyph(''), LARGE, menuItem("cover.builtIn", "Built-In"), menuItem("cover.remove", "Remove Current Cover Page")),
        button("blankPage", "Blank Page", glyph(''), LARGE),
        button("pageBreak", "Page Break", glyph(''), LARGE).also { it.shortcut = "Ctrl+Enter" },
    ).also { it.icon = glyph('\uE7C3') },
    group(
        "tables",
        "Tables",
        dropDown(
            "insertTable",
            "Table",
            glyph('\uE80A'),
            LARGE,
            RibbonGridPickerModel("tablePicker", "Insert Table"),
            RibbonMenuSeparatorModel(),
            menuItem("table.insertDialog", "Insert Table...", glyph('')),
            menuItem("table.draw", "Draw Table", RibbonIcons.PEN),
        ).also { it.keyTip = "T" },
    ).also { it.icon = glyph('\uE80A') },
    group(
        "illustrations",
        "Illustrations",
        dropDown("pictures", "Pictures", glyph(''), LARGE, menuItem("pictures.device", "This Device..."), menuItem("pictures.online", "Online Pictures...")),
        dropDown("shapes", "Shapes", glyph(''), LARGE, menuItem("shapes.line", "Line"), menuItem("shapes.rectangle", "Rectangle"), menuItem("shapes.oval", "Oval")),
        button("icons", "Icons", glyph(''), LARGE),
        button("models3d", "3D Models", glyph('')),
        button("smartArt", "SmartArt", glyph('\uE9D2')),
        button("chart", "Chart", glyph('')),
        dropDown("screenshot", "Screenshot", glyph(''), MEDIUM, menuItem("screenshot.clipping", "Screen Clipping")),
    ).also { it.icon = glyph('\uEB9F') },
    group("media", "Media", button("onlineVideo", "Online Videos", glyph(''), LARGE)).also { it.icon = glyph('') },
    group(
        "links",
        "Links",
        button("link", "Link", glyph('')).also { it.shortcut = "Ctrl+K" },
        button("bookmark", "Bookmark", glyph('')),
        button("crossReference", "Cross-reference", glyph('')),
    ).also { it.icon = glyph('\uE71B') },
    group("insertComments", "Comments", button("newComment", "Comment", glyph(''), LARGE).also { it.shortcut = "Ctrl+Alt+M" }).also { it.icon = glyph('') },
    group(
        "headerFooter",
        "Header & Footer",
        dropDown("header", "Header", glyph(''), MEDIUM, menuItem("header.blank", "Blank"), menuItem("header.edit", "Edit Header")),
        dropDown("footer", "Footer", glyph(''), MEDIUM, menuItem("footer.blank", "Blank"), menuItem("footer.edit", "Edit Footer")),
        dropDown("pageNumber", "Page Number", glyph(''), MEDIUM, menuItem("pageNumber.top", "Top of Page"), menuItem("pageNumber.bottom", "Bottom of Page")),
    ).also { it.icon = glyph('\uE8A1') },
    group(
        "text",
        "Text",
        dropDown("textBox", "Text Box", glyph(''), LARGE, menuItem("textBox.simple", "Simple Text Box"), menuItem("textBox.draw", "Draw Text Box")),
        button("quickParts", "Quick Parts", glyph('')),
        button("wordArt", "WordArt", glyph('')),
        button("dropCap", "Drop Cap", glyph('')),
    ).also { it.icon = glyph('\uE8D2') },
    group(
        "symbols",
        "Symbols",
        split("equation", "Equation", glyph(''), MEDIUM, menuItem("equation.new", "Insert New Equation"), menuItem("equation.ink", "Ink Equation")).also { it.shortcut = "Alt+=" },
        dropDown("symbol", "Symbol", OMEGA, MEDIUM, menuItem("symbol.copyright", "©"), menuItem("symbol.trademark", "™"), menuItem("symbol.more", "More Symbols...")),
    ).also { it.icon = glyph('\uE94C') },
)

/** The [Design] tab. */
internal fun createDesignTab(): RibbonTabModel = tab(
    "design",
    "Design",
    "D",
    group(
        "docFormatting",
        "Document Formatting",
        dropDown("themes", "Themes", glyph(''), LARGE, radioItem("theme.office", "Office", "theme", checked = true), radioItem("theme.facet", "Facet", "theme"), radioItem("theme.ion", "Ion", "theme")),
        styleSetGallery(),
        dropDown("colors", "Colors", glyph(''), MEDIUM, radioItem("colors.office", "Office", "colors", checked = true), radioItem("colors.blue", "Blue", "colors"), radioItem("colors.green", "Green", "colors")),
        dropDown("fonts", "Fonts", glyph(''), MEDIUM, radioItem("fonts.office", "Office", "fonts", checked = true), radioItem("fonts.meiryo", "Meiryo", "fonts")),
        dropDown("paragraphSpacing", "Paragraph Spacing", glyph(''), MEDIUM, radioItem("pspacing.default", "Default", "pspacing", checked = true), radioItem("pspacing.compact", "Compact", "pspacing"), radioItem("pspacing.open", "Open", "pspacing")),
        RibbonCheckBoxModel("setAsDefault", "Set as Default"),
    ).also { it.icon = glyph('\uE790') },
    group(
        "pageBackground",
        "Page Background",
        dropDown("watermark", "Watermark", glyph(''), LARGE, menuItem("watermark.confidential", "Confidential"), menuItem("watermark.draft", "Draft"), RibbonMenuSeparatorModel(), menuItem("watermark.remove", "Remove Watermark")),
        RibbonColorPickerModel("pageColor", "Page Color", glyph('')).also {
            it.size = LARGE
            it.selectedColor = RibbonColor.WHITE
            it.showNoColor = true
            it.showAutomatic = false
        },
        button("pageBorders", "Page Borders", glyph(''), LARGE),
    ).also { it.icon = glyph('\uE790') },
)

/** The [Style Set] gallery on the [Design] tab (a gallery that shows only text samples). */
private fun styleSetGallery(): RibbonGalleryModel = RibbonGalleryModel("styleSets", "Style Set").also { gallery ->
    gallery.maxColumns = STYLE_SET_MAX_COLUMNS
    gallery.minColumns = STYLE_SET_MIN_COLUMNS
    gallery.itemWidth = STYLE_SET_ITEM_WIDTH
    gallery.itemHeight = STYLE_SET_ITEM_HEIGHT
    gallery.showLabels = false
    val sets = listOf(
        Triple("Office", null, "#FFFFFF"),
        Triple("Basic", "#1F3864", "#F2F2F2"),
        Triple("Lines", "#C55A11", "#FBE5D6"),
        Triple("Shaded", "#FFFFFF", "#2F5496"),
        Triple("Casual", "#375623", "#E2EFDA"),
        Triple("Lines (Stylish)", "#7030A0", "#EDE0F5"),
    )
    sets.forEachIndexed { index, (label, foreground, background) ->
        gallery.items.add(
            RibbonGalleryItemModel("styleSet$index", label).also {
                it.previewText = "Title"
                it.previewFontSize = STYLE_SET_FONT_SIZE
                it.previewForeground = foreground?.let(RibbonColor::parse)
                it.previewBackground = RibbonColor.parse(background)
            },
        )
    }
    gallery.selectedItem = gallery.items[0]
}

/** The [Layout] tab. */
@Suppress("LongMethod") // Declaratively builds the [Layout] tab's groups from top to bottom
internal fun createLayoutTab(): RibbonTabModel = tab(
    "layout",
    "Layout",
    "P",
    group(
        "pageSetup",
        "Page Setup",
        dropDown(
            "margins",
            "Margins",
            glyph('\uE7C3'),
            LARGE,
            radioItem("margins.normal", "Normal", "margins", checked = true),
            radioItem("margins.narrow", "Narrow", "margins"),
            radioItem("margins.moderate", "Moderate", "margins"),
            radioItem("margins.wide", "Wide", "margins"),
            RibbonMenuSeparatorModel(),
            menuItem("margins.custom", "Custom Margins..."),
        ),
        dropDown("orientation", "Orientation", glyph(''), LARGE, radioItem("orientation.portrait", "Portrait", "orientation", checked = true), radioItem("orientation.landscape", "Landscape", "orientation")),
        dropDown("size", "Size", glyph(''), LARGE, radioItem("size.a4", "A4", "size", checked = true), radioItem("size.b5", "B5", "size"), radioItem("size.letter", "Letter", "size")),
        dropDown("columns", "Columns", glyph(''), LARGE, radioItem("columns.one", "One", "columns", checked = true), radioItem("columns.two", "Two", "columns"), radioItem("columns.three", "Three", "columns")),
        dropDown("breaks", "Breaks", glyph(''), MEDIUM, menuItem("breaks.page", "Page"), menuItem("breaks.column", "Column"), menuItem("breaks.section", "Section Break (Next Page)")),
        dropDown("lineNumbers", "Line Numbers", RibbonIcon.path("M9 5h12v2H9z M9 11h12v2H9z M9 17h12v2H9z M4 3h2v5H5V4.2H4z"), MEDIUM, radioItem("lineNumbers.none", "None", "lineNumbers", checked = true), radioItem("lineNumbers.continuous", "Continuous", "lineNumbers")),
        dropDown("hyphenation", "Hyphenation", glyph(''), MEDIUM, radioItem("hyphenation.none", "None", "hyphenation", checked = true), radioItem("hyphenation.auto", "Automatic", "hyphenation")),
    ).also {
        it.icon = glyph('\uE7C3')
        it.isDialogLauncherVisible = true
    },
    group(
        "layoutParagraph",
        "Paragraph",
        RibbonLabelModel("indentLabel", "Indent").also { it.icon = RibbonIcons.INDENT_INCREASE },
        spinner("indentLeft", "Left:", 0.0, INDENT_MAX, CM_STEP, "cm", "0.0"),
        spinner("indentRight", "Right:", 0.0, INDENT_MAX, CM_STEP, "cm", "0.0"),
        RibbonLabelModel("spacingLabel", "Spacing").also { it.icon = glyph('') },
        spinner("spacingBefore", "Before:", 0.0, SPACING_MAX, PT_STEP, "pt", "0"),
        spinner("spacingAfter", "After:", SPACING_AFTER, SPACING_MAX, PT_STEP, "pt", "0"),
    ).also {
        it.icon = RibbonIcons.BULLETS
        it.isDialogLauncherVisible = true
    },
    group(
        "arrange",
        "Arrange",
        dropDown("position", "Position", glyph(''), LARGE, menuItem("position.inline", "In Line with Text"), menuItem("position.topLeft", "Top Left")),
        dropDown("wrapText", "Wrap Text", glyph(''), LARGE, radioItem("wrap.inline", "In Line with Text", "wrap", checked = true), radioItem("wrap.square", "Square", "wrap"), radioItem("wrap.tight", "Tight", "wrap")),
        split("bringForward", "Bring Forward", glyph(''), MEDIUM, menuItem("bringForward.front", "Bring to Front")),
        split("sendBackward", "Send Backward", glyph(''), MEDIUM, menuItem("sendBackward.back", "Send to Back")),
        button("selectionPane", "Selection Pane", glyph('')),
        dropDown("align", "Align", RibbonIcons.ALIGN_LEFT, SMALL, menuItem("align.left", "Align Left"), menuItem("align.center", "Align Center")),
        dropDown("group", "Group", glyph(''), SMALL, menuItem("group.group", "Group"), menuItem("group.ungroup", "Ungroup")),
        dropDown("rotate", "Rotate", glyph(''), SMALL, menuItem("rotate.right", "Rotate Right 90°"), menuItem("rotate.left", "Rotate Left 90°")),
    ).also { it.icon = glyph('\uE8F4') },
)

@Suppress("LongParameterList") // The parameters map 1:1 to the spinner's value range, step, unit, and format
internal fun spinner(id: String, label: String, value: Double, maximum: Double, step: Double, unit: String, format: String): RibbonSpinnerModel =
    RibbonSpinnerModel(id, label, value).also {
        it.minimum = 0.0
        it.maximum = maximum
        it.increment = step
        it.unit = unit
        it.format = format
        it.inputWidth = SPINNER_WIDTH
    }

/** The [Review] tab. */
@Suppress("LongMethod") // Declaratively builds the [Review] tab's groups from top to bottom
internal fun createReviewTab(): RibbonTabModel = tab(
    "review",
    "Review",
    "R",
    group(
        "proofing",
        "Proofing",
        button("editorReview", "Editor", PROOFING, LARGE).also { it.shortcut = "F7" },
        button("thesaurus", "Thesaurus", glyph('')).also { it.shortcut = "Shift+F7" },
        button("wordCount", "Word Count", glyph('')),
    ).also { it.icon = PROOFING },
    group(
        "language",
        "Language",
        dropDown("translate", "Translate", glyph(''), LARGE, menuItem("translate.selection", "Translate Selection"), menuItem("translate.document", "Translate Document")),
        dropDown("languageSettings", "Language", glyph(''), LARGE, menuItem("language.proofing", "Set Proofing Language..."), menuItem("language.preferences", "Language Preferences...")),
    ).also { it.icon = glyph('\uE8C1') },
    group(
        "reviewComments",
        "Comments",
        button("newComment2", "New Comment", glyph(''), LARGE),
        button("deleteComment", "Delete", glyph('')),
        button("previousComment", "Previous", glyph('')),
        button("nextComment", "Next", glyph('')),
    ).also { it.icon = glyph('\uE90A') },
    group(
        "tracking",
        "Tracking",
        toggle("trackChanges", "Track Changes", glyph(''), LARGE).also { it.shortcut = "Ctrl+Shift+E" },
        RibbonComboBoxModel("markup", "Display for Review", listOf("Simple Markup", "All Markup", "No Markup", "Original")).also {
            it.selectedItem = it.items[0]
            it.text = it.items[0].toString()
            it.inputWidth = MARKUP_WIDTH
            it.showLabel = false
        },
        dropDown("showMarkup", "Show Markup", glyph(''), MEDIUM, menuItem("markup.comments", "Comments"), menuItem("markup.insertions", "Insertions and Deletions"), menuItem("markup.formatting", "Formatting")),
        button("reviewingPane", "Reviewing Pane", glyph('')),
    ).also {
        it.icon = glyph('\uE70F')
        it.isDialogLauncherVisible = true
    },
    group(
        "changes",
        "Changes",
        split("accept", "Accept", glyph(''), MEDIUM, menuItem("accept.next", "Accept and Move to Next"), menuItem("accept.all", "Accept All Changes")),
        split("reject", "Reject", glyph(''), MEDIUM, menuItem("reject.next", "Reject and Move to Next"), menuItem("reject.all", "Reject All Changes")),
    ).also { it.icon = glyph('\uE8FB') },
    group("protect", "Protect", button("restrictEditing", "Restrict Editing", glyph(''), LARGE)).also { it.icon = glyph('') },
)

/** The [View] tab. */
internal fun createViewTab(): RibbonTabModel = tab(
    "view",
    "View",
    "W",
    group(
        "views",
        "Views",
        view("readMode", "Read Mode", glyph(''), LARGE),
        view("printLayout", "Print Layout", glyph(''), LARGE).also { it.isChecked = true },
        view("webLayout", "Web Layout", glyph(''), LARGE),
        view("outline", "Outline", RibbonIcons.BULLETS, MEDIUM),
        view("draft", "Draft", glyph(''), MEDIUM),
    ).also { it.icon = glyph('\uE7C3') },
    group(
        "show",
        "Show",
        RibbonCheckBoxModel("ruler", "Ruler", isChecked = true),
        RibbonCheckBoxModel("gridlines", "Gridlines"),
        RibbonCheckBoxModel("navigationPane", "Navigation Pane", isChecked = true),
    ).also { it.icon = glyph('\uE890') },
    group(
        "zoom",
        "Zoom",
        button("zoomDialog", "Zoom", glyph(''), LARGE),
        button("zoom100", "100%", glyph('\uE71E'), LARGE),
        button("onePage", "One Page", glyph('')),
        button("multiplePages", "Multiple Pages", glyph('')),
        button("pageWidth", "Page Width", glyph('')),
    ).also { it.icon = glyph('\uE71E') },
    group(
        "window",
        "Window",
        button("newWindow", "New Window", glyph(''), LARGE),
        button("arrangeAll", "Arrange All", glyph('')),
        button("split", "Split", glyph('')),
        toggle("sideBySide", "View Side by Side", glyph(''), MEDIUM),
    ).also { it.icon = glyph('\uE737') },
    group("macros", "Macros", split("macrosButton", "Macros", glyph(''), MEDIUM, menuItem("macros.view", "View Macros"), menuItem("macros.record", "Record Macro..."))).also { it.icon = glyph('') },
)

private fun view(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize) = toggle(id, label, icon, size).also { it.groupName = "view" }

/** The [Help] tab. */
internal fun createHelpTab(): RibbonTabModel = tab(
    "help",
    "Help",
    "Y",
    group(
        "helpGroup",
        "Help",
        button("helpButton", "Help", glyph(''), LARGE).also { it.shortcut = "F1" },
        button("feedback", "Feedback", glyph(''), LARGE),
        button("showTraining", "Show Training", glyph(''), LARGE),
    ).also { it.icon = glyph('\uE897') },
)

/** [Editor] in the [Proofing] group (not in the font, so it uses the same path as the RibbonSpace demo). */
private val PROOFING = RibbonIcon.path("M3 15l2.4-9h1.9l2.4 9H8.1l-.5-2H5.1l-.5 2z M5.5 11.5h1.7L6.3 8z M12 19l3 3 7-7-1.4-1.4-5.6 5.6-1.6-1.6z")

/**
 * The Ω for [Symbols] (character icons are drawn in SemiBold, but the default font's SemiBold renders Ω as a different
 * glyph, so a path is used. doc/troubleshooting.md).
 */
private val OMEGA = RibbonIcon.path(
    "M5 19h4v-2.2C6.6 15.6 5 13.2 5 10.5 5 6.4 8.1 3.5 12 3.5s7 2.9 7 7c0 2.7-1.6 5.1-4 6.3V19h4v1.5h-5.5v-4.6" +
        "c2.3-.9 4-3 4-5.4 0-3.2-2.4-5.5-5.5-5.5S6.5 7.3 6.5 10.5c0 2.4 1.7 4.5 4 5.4v4.6H5z",
)

private const val STYLE_SET_MAX_COLUMNS = 5
private const val STYLE_SET_MIN_COLUMNS = 3
private const val STYLE_SET_ITEM_WIDTH = 70.0
private const val STYLE_SET_ITEM_HEIGHT = 62.0
private const val STYLE_SET_FONT_SIZE = 16.0
private const val INDENT_MAX = 22.0
private const val SPACING_MAX = 1584.0
private const val SPACING_AFTER = 8.0
private const val CM_STEP = 0.1
private const val PT_STEP = 6.0
private const val SPINNER_WIDTH = 72.0
private const val MARKUP_WIDTH = 130.0
