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
    Messages.get("common.insert"),
    "N",
    group(
        "pages",
        Messages.get("insert.pages"),
        dropDown("coverPage", Messages.get("insert.cover.page"), glyph('\uE7C3'), LARGE, menuItem("cover.builtIn", Messages.get("insert.cover.built.in")), menuItem("cover.remove", Messages.get("insert.cover.remove"))),
        button("blankPage", Messages.get("insert.blank.page"), glyph('\uE8A5'), LARGE),
        button("pageBreak", Messages.get("insert.page.break"), glyph('\uE7C3'), LARGE).also { it.shortcut = "Ctrl+Enter" },
    ).also { it.icon = glyph('\uE7C3') },
    group(
        "tables",
        Messages.get("insert.tables"),
        dropDown(
            "insertTable",
            Messages.get("common.table"),
            glyph('\uE80A'),
            LARGE,
            RibbonGridPickerModel("tablePicker", Messages.get("insert.table.picker")),
            RibbonMenuSeparatorModel(),
            menuItem("table.insertDialog", Messages.get("common.insert.table"), glyph('\uE80A')),
            menuItem("table.draw", Messages.get("insert.table.draw"), RibbonIcons.PEN),
        ).also { it.keyTip = "T" },
    ).also { it.icon = glyph('\uE80A') },
    group(
        "illustrations",
        Messages.get("common.illustrations"),
        dropDown("pictures", Messages.get("common.pictures"), glyph('\uEB9F'), LARGE, menuItem("pictures.device", Messages.get("common.pictures.device")), menuItem("pictures.online", Messages.get("insert.pictures.online"))),
        dropDown("shapes", Messages.get("common.shapes"), glyph('\uE91B'), LARGE, menuItem("shapes.line", Messages.get("insert.shape.line")), menuItem("shapes.rectangle", Messages.get("common.shape.rectangle")), menuItem("shapes.oval", Messages.get("common.shape.oval"))),
        button("icons", Messages.get("common.icons"), glyph('\uED58'), LARGE),
        button("models3d", Messages.get("insert.models3d"), glyph('\uE7B8')),
        button("smartArt", "SmartArt", glyph('\uE9D2')),
        button("chart", Messages.get("common.chart"), glyph('\uE9D2')),
        dropDown("screenshot", Messages.get("common.screenshot"), glyph('\uE722'), MEDIUM, menuItem("screenshot.clipping", Messages.get("insert.screenshot.clipping"))),
    ).also { it.icon = glyph('\uEB9F') },
    group("media", Messages.get("common.media"), button("onlineVideo", Messages.get("insert.online.video"), glyph('\uE714'), LARGE)).also { it.icon = glyph('\uE714') },
    group(
        "links",
        Messages.get("insert.links"),
        button("link", Messages.get("insert.link"), glyph('\uE71B')).also { it.shortcut = "Ctrl+K" },
        button("bookmark", Messages.get("insert.bookmark"), glyph('\uE8A4')),
        button("crossReference", Messages.get("insert.cross.reference"), glyph('\uE8AD')),
    ).also { it.icon = glyph('\uE71B') },
    group("insertComments", Messages.get("common.comments"), button("newComment", Messages.get("insert.comment"), glyph('\uE90A'), LARGE).also { it.shortcut = "Ctrl+Alt+M" }).also { it.icon = glyph('\uE90A') },
    group(
        "headerFooter",
        Messages.get("insert.header.footer"),
        dropDown("header", Messages.get("insert.header"), glyph('\uE8A1'), MEDIUM, menuItem("header.blank", Messages.get("insert.blank")), menuItem("header.edit", Messages.get("insert.header.edit"))),
        dropDown("footer", Messages.get("insert.footer"), glyph('\uE8A1'), MEDIUM, menuItem("footer.blank", Messages.get("insert.blank")), menuItem("footer.edit", Messages.get("insert.footer.edit"))),
        dropDown("pageNumber", Messages.get("insert.page.number"), glyph('\uE8A1'), MEDIUM, menuItem("pageNumber.top", Messages.get("insert.page.number.top")), menuItem("pageNumber.bottom", Messages.get("insert.page.number.bottom"))),
    ).also { it.icon = glyph('\uE8A1') },
    group(
        "text",
        Messages.get("insert.text"),
        dropDown("textBox", Messages.get("insert.text.box"), glyph('\uE8D2'), LARGE, menuItem("textBox.simple", Messages.get("insert.text.box.simple")), menuItem("textBox.draw", Messages.get("insert.text.box.draw"))),
        button("quickParts", Messages.get("insert.quick.parts"), glyph('\uECAA')),
        button("wordArt", Messages.get("insert.word.art"), glyph('\uE8D2')),
        button("dropCap", Messages.get("insert.drop.cap"), glyph('\uE8D2')),
    ).also { it.icon = glyph('\uE8D2') },
    group(
        "symbols",
        Messages.get("insert.symbols"),
        split("equation", Messages.get("insert.equation"), glyph('\uE94C'), MEDIUM, menuItem("equation.new", Messages.get("insert.equation.new")), menuItem("equation.ink", Messages.get("insert.equation.ink"))).also { it.shortcut = "Alt+=" },
        dropDown("symbol", Messages.get("insert.symbol"), OMEGA, MEDIUM, menuItem("symbol.copyright", "©"), menuItem("symbol.trademark", "™"), menuItem("symbol.more", Messages.get("insert.symbol.more"))),
    ).also { it.icon = glyph('\uE94C') },
)

/** The [Design] tab. */
internal fun createDesignTab(): RibbonTabModel = tab(
    "design",
    Messages.get("common.design"),
    "D",
    group(
        "docFormatting",
        Messages.get("design.document.formatting"),
        dropDown("themes", Messages.get("design.themes"), glyph('\uE790'), LARGE, radioItem("theme.office", "Office", "theme", checked = true), radioItem("theme.facet", Messages.get("design.theme.facet"), "theme"), radioItem("theme.ion", Messages.get("design.theme.ion"), "theme")),
        styleSetGallery(),
        dropDown("colors", Messages.get("design.colors"), glyph('\uE790'), MEDIUM, radioItem("colors.office", "Office", "colors", checked = true), radioItem("colors.blue", Messages.get("design.colors.blue"), "colors"), radioItem("colors.green", Messages.get("design.colors.green"), "colors")),
        dropDown("fonts", Messages.get("design.fonts"), glyph('\uE8D2'), MEDIUM, radioItem("fonts.office", "Office", "fonts", checked = true), radioItem("fonts.meiryo", Messages.get("design.fonts.meiryo"), "fonts")),
        dropDown("paragraphSpacing", Messages.get("design.paragraph.spacing"), glyph('\uE9E9'), MEDIUM, radioItem("pspacing.default", Messages.get("design.spacing.default"), "pspacing", checked = true), radioItem("pspacing.compact", Messages.get("design.spacing.compact"), "pspacing"), radioItem("pspacing.open", Messages.get("design.spacing.open"), "pspacing")),
        RibbonCheckBoxModel("setAsDefault", Messages.get("design.set.as.default")),
    ).also { it.icon = glyph('\uE790') },
    group(
        "pageBackground",
        Messages.get("design.page.background"),
        dropDown("watermark", Messages.get("design.watermark"), glyph('\uE8D2'), LARGE, menuItem("watermark.confidential", Messages.get("design.watermark.confidential")), menuItem("watermark.draft", Messages.get("design.draft")), RibbonMenuSeparatorModel(), menuItem("watermark.remove", Messages.get("design.watermark.remove"))),
        RibbonColorPickerModel("pageColor", Messages.get("design.page.color"), glyph('\uE790')).also {
            it.size = LARGE
            it.selectedColor = RibbonColor.WHITE
            it.showNoColor = true
            it.showAutomatic = false
        },
        button("pageBorders", Messages.get("design.page.borders"), glyph('\uE80A'), LARGE),
    ).also { it.icon = glyph('\uE790') },
)

/** The [Style Set] gallery on the [Design] tab (a gallery that shows only text samples). */
private fun styleSetGallery(): RibbonGalleryModel = RibbonGalleryModel("styleSets", Messages.get("design.style.set")).also { gallery ->
    gallery.maxColumns = STYLE_SET_MAX_COLUMNS
    gallery.minColumns = STYLE_SET_MIN_COLUMNS
    gallery.itemWidth = STYLE_SET_ITEM_WIDTH
    gallery.itemHeight = STYLE_SET_ITEM_HEIGHT
    gallery.showLabels = false
    val sets = listOf(
        Triple("Office", null, "#FFFFFF"),
        Triple(Messages.get("design.style.set.basic"), "#1F3864", "#F2F2F2"),
        Triple(Messages.get("design.style.set.lines"), "#C55A11", "#FBE5D6"),
        Triple(Messages.get("design.style.set.shaded"), "#FFFFFF", "#2F5496"),
        Triple(Messages.get("design.style.set.casual"), "#375623", "#E2EFDA"),
        Triple(Messages.get("design.style.set.stylish"), "#7030A0", "#EDE0F5"),
    )
    sets.forEachIndexed { index, (label, foreground, background) ->
        gallery.items.add(
            RibbonGalleryItemModel("styleSet$index", label).also {
                it.previewText = Messages.get("style.title")
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
    Messages.get("common.layout"),
    "P",
    group(
        "pageSetup",
        Messages.get("layout.page.setup"),
        dropDown(
            "margins",
            Messages.get("layout.margins"),
            glyph('\uE7C3'),
            LARGE,
            radioItem("margins.normal", Messages.get("style.normal"), "margins", checked = true),
            radioItem("margins.narrow", Messages.get("layout.margins.narrow"), "margins"),
            radioItem("margins.moderate", Messages.get("layout.margins.moderate"), "margins"),
            radioItem("margins.wide", Messages.get("layout.margins.wide"), "margins"),
            RibbonMenuSeparatorModel(),
            menuItem("margins.custom", Messages.get("layout.margins.custom")),
        ),
        dropDown("orientation", Messages.get("layout.orientation"), glyph('\uE7C3'), LARGE, radioItem("orientation.portrait", Messages.get("layout.portrait"), "orientation", checked = true), radioItem("orientation.landscape", Messages.get("layout.landscape"), "orientation")),
        dropDown("size", Messages.get("layout.size"), glyph('\uE7C3'), LARGE, radioItem("size.a4", "A4", "size", checked = true), radioItem("size.b5", "B5", "size"), radioItem("size.letter", Messages.get("layout.size.letter"), "size")),
        dropDown("columns", Messages.get("layout.columns"), glyph('\uE8A9'), LARGE, radioItem("columns.one", Messages.get("layout.columns.one"), "columns", checked = true), radioItem("columns.two", Messages.get("layout.columns.two"), "columns"), radioItem("columns.three", Messages.get("layout.columns.three"), "columns")),
        dropDown("breaks", Messages.get("layout.breaks"), glyph('\uE7C3'), MEDIUM, menuItem("breaks.page", Messages.get("layout.breaks.page")), menuItem("breaks.column", Messages.get("layout.breaks.column")), menuItem("breaks.section", Messages.get("layout.breaks.section"))),
        dropDown("lineNumbers", Messages.get("layout.line.numbers"), RibbonIcon.path("M9 5h12v2H9z M9 11h12v2H9z M9 17h12v2H9z M4 3h2v5H5V4.2H4z"), MEDIUM, radioItem("lineNumbers.none", Messages.get("layout.none"), "lineNumbers", checked = true), radioItem("lineNumbers.continuous", Messages.get("layout.line.numbers.continuous"), "lineNumbers")),
        dropDown("hyphenation", Messages.get("layout.hyphenation"), glyph('\uE8D2'), MEDIUM, radioItem("hyphenation.none", Messages.get("layout.none"), "hyphenation", checked = true), radioItem("hyphenation.auto", Messages.get("layout.hyphenation.auto"), "hyphenation")),
    ).also {
        it.icon = glyph('\uE7C3')
        it.isDialogLauncherVisible = true
    },
    group(
        "layoutParagraph",
        Messages.get("common.paragraph"),
        RibbonLabelModel("indentLabel", Messages.get("layout.indent")).also { it.icon = RibbonIcons.INDENT_INCREASE },
        spinner("indentLeft", Messages.get("layout.indent.left"), 0.0, INDENT_MAX, CM_STEP, "cm", "0.0"),
        spinner("indentRight", Messages.get("layout.indent.right"), 0.0, INDENT_MAX, CM_STEP, "cm", "0.0"),
        RibbonLabelModel("spacingLabel", Messages.get("layout.spacing")).also { it.icon = glyph('\uE9E9') },
        spinner("spacingBefore", Messages.get("layout.spacing.before"), 0.0, SPACING_MAX, PT_STEP, "pt", "0"),
        spinner("spacingAfter", Messages.get("layout.spacing.after"), SPACING_AFTER, SPACING_MAX, PT_STEP, "pt", "0"),
    ).also {
        it.icon = RibbonIcons.BULLETS
        it.isDialogLauncherVisible = true
    },
    group(
        "arrange",
        Messages.get("layout.arrange"),
        dropDown("position", Messages.get("layout.position"), glyph('\uE7C2'), LARGE, menuItem("position.inline", Messages.get("layout.inline")), menuItem("position.topLeft", Messages.get("layout.position.top.left"))),
        dropDown("wrapText", Messages.get("layout.wrap.text"), glyph('\uE8D2'), LARGE, radioItem("wrap.inline", Messages.get("layout.inline"), "wrap", checked = true), radioItem("wrap.square", Messages.get("layout.wrap.square"), "wrap"), radioItem("wrap.tight", Messages.get("layout.wrap.tight"), "wrap")),
        split("bringForward", Messages.get("layout.bring.forward"), glyph('\uE746'), MEDIUM, menuItem("bringForward.front", Messages.get("common.bring.to.front"))),
        split("sendBackward", Messages.get("layout.send.backward"), glyph('\uE747'), MEDIUM, menuItem("sendBackward.back", Messages.get("common.send.to.back"))),
        button("selectionPane", Messages.get("layout.selection.pane"), glyph('\uE89F')),
        dropDown("align", Messages.get("layout.align"), RibbonIcons.ALIGN_LEFT, SMALL, menuItem("align.left", Messages.get("common.align.left")), menuItem("align.center", Messages.get("layout.align.center"))),
        dropDown("group", Messages.get("layout.group"), glyph('\uE8F4'), SMALL, menuItem("group.group", Messages.get("layout.group")), menuItem("group.ungroup", Messages.get("layout.ungroup"))),
        dropDown("rotate", Messages.get("layout.rotate"), glyph('\uE7AD'), SMALL, menuItem("rotate.right", Messages.get("layout.rotate.right")), menuItem("rotate.left", Messages.get("layout.rotate.left"))),
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
    Messages.get("review.tab"),
    "R",
    group(
        "proofing",
        Messages.get("review.proofing"),
        button("editorReview", Messages.get("home.editor"), PROOFING, LARGE).also { it.shortcut = "F7" },
        button("thesaurus", Messages.get("review.thesaurus"), glyph('\uE8F1')).also { it.shortcut = "Shift+F7" },
        button("wordCount", Messages.get("review.word.count"), glyph('\uE8EF')),
    ).also { it.icon = PROOFING },
    group(
        "language",
        Messages.get("review.language"),
        dropDown("translate", Messages.get("review.translate"), glyph('\uE8C1'), LARGE, menuItem("translate.selection", Messages.get("review.translate.selection")), menuItem("translate.document", Messages.get("review.translate.document"))),
        dropDown("languageSettings", Messages.get("review.language"), glyph('\uE774'), LARGE, menuItem("language.proofing", Messages.get("review.language.proofing")), menuItem("language.preferences", Messages.get("review.language.preferences"))),
    ).also { it.icon = glyph('\uE8C1') },
    group(
        "reviewComments",
        Messages.get("common.comments"),
        button("newComment2", Messages.get("review.new.comment"), glyph('\uE90A'), LARGE),
        button("deleteComment", Messages.get("common.delete"), glyph('\uE74D')),
        button("previousComment", Messages.get("review.previous"), glyph('\uE892')),
        button("nextComment", Messages.get("review.next"), glyph('\uE893')),
    ).also { it.icon = glyph('\uE90A') },
    group(
        "tracking",
        Messages.get("review.tracking"),
        toggle("trackChanges", Messages.get("review.track.changes"), glyph('\uE70F'), LARGE).also { it.shortcut = "Ctrl+Shift+E" },
        RibbonComboBoxModel("markup", Messages.get("review.markup"), listOf(Messages.get("review.markup.simple"), Messages.get("review.markup.all"), Messages.get("review.markup.none"), Messages.get("review.markup.original"))).also {
            it.selectedItem = it.items[0]
            it.text = it.items[0].toString()
            it.inputWidth = MARKUP_WIDTH
            it.showLabel = false
        },
        dropDown("showMarkup", Messages.get("review.show.markup"), glyph('\uE890'), MEDIUM, menuItem("markup.comments", Messages.get("common.comments")), menuItem("markup.insertions", Messages.get("review.markup.insertions")), menuItem("markup.formatting", Messages.get("review.markup.formatting"))),
        button("reviewingPane", Messages.get("review.reviewing.pane"), glyph('\uE89F')),
    ).also {
        it.icon = glyph('\uE70F')
        it.isDialogLauncherVisible = true
    },
    group(
        "changes",
        Messages.get("review.changes"),
        split("accept", Messages.get("review.accept"), glyph('\uE8FB'), MEDIUM, menuItem("accept.next", Messages.get("review.accept.next")), menuItem("accept.all", Messages.get("review.accept.all"))),
        split("reject", Messages.get("review.reject"), glyph('\uE711'), MEDIUM, menuItem("reject.next", Messages.get("review.reject.next")), menuItem("reject.all", Messages.get("review.reject.all"))),
    ).also { it.icon = glyph('\uE8FB') },
    group("protect", Messages.get("review.protect"), button("restrictEditing", Messages.get("review.restrict.editing"), glyph('\uE72E'), LARGE)).also { it.icon = glyph('\uE72E') },
)

/** The [View] tab. */
internal fun createViewTab(): RibbonTabModel = tab(
    "view",
    Messages.get("common.view"),
    "W",
    group(
        "views",
        Messages.get("view.views"),
        view("readMode", Messages.get("status.read"), glyph('\uE736'), LARGE),
        view("printLayout", Messages.get("status.print"), glyph('\uE7C3'), LARGE).also { it.isChecked = true },
        view("webLayout", Messages.get("status.web"), glyph('\uE774'), LARGE),
        view("outline", Messages.get("view.outline"), RibbonIcons.BULLETS, MEDIUM),
        view("draft", Messages.get("design.draft"), glyph('\uE8A5'), MEDIUM),
    ).also { it.icon = glyph('\uE7C3') },
    group(
        "show",
        Messages.get("view.show"),
        RibbonCheckBoxModel("ruler", Messages.get("common.ruler"), isChecked = true),
        RibbonCheckBoxModel("gridlines", Messages.get("common.gridlines")),
        RibbonCheckBoxModel("navigationPane", Messages.get("view.navigation.pane"), isChecked = true),
    ).also { it.icon = glyph('\uE890') },
    group(
        "zoom",
        Messages.get("common.zoom"),
        button("zoomDialog", Messages.get("common.zoom"), glyph('\uE71E'), LARGE),
        button("zoom100", "100%", glyph('\uE71E'), LARGE),
        button("onePage", Messages.get("view.one.page"), glyph('\uE7C3')),
        button("multiplePages", Messages.get("view.multiple.pages"), glyph('\uE8A9')),
        button("pageWidth", Messages.get("view.page.width"), glyph('\uE740')),
    ).also { it.icon = glyph('\uE71E') },
    group(
        "window",
        Messages.get("common.window"),
        button("newWindow", Messages.get("common.new.window"), glyph('\uE737'), LARGE),
        button("arrangeAll", Messages.get("view.arrange.all"), glyph('\uE8A9')),
        button("split", Messages.get("view.split"), glyph('\uE8A0')),
        toggle("sideBySide", Messages.get("view.side.by.side"), glyph('\uE8A9'), MEDIUM),
    ).also { it.icon = glyph('\uE737') },
    group("macros", Messages.get("view.macros"), split("macrosButton", Messages.get("view.macros"), glyph('\uE943'), MEDIUM, menuItem("macros.view", Messages.get("view.macros.view")), menuItem("macros.record", Messages.get("view.macros.record")))).also { it.icon = glyph('\uE943') },
)

private fun view(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize) = toggle(id, label, icon, size).also { it.groupName = "view" }

/** The [Help] tab. */
internal fun createHelpTab(): RibbonTabModel = tab(
    "help",
    Messages.get("common.help"),
    "Y",
    group(
        "helpGroup",
        Messages.get("common.help"),
        button("helpButton", Messages.get("common.help"), glyph('\uE897'), LARGE).also { it.shortcut = "F1" },
        button("feedback", Messages.get("help.feedback"), glyph('\uED15'), LARGE),
        button("showTraining", Messages.get("help.training"), glyph('\uE7BE'), LARGE),
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
