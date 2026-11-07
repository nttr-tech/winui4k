package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstageItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstagePlacement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommand
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommandCatalog
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualActivation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.dropDown
import com.appkitbox.winui4k.sample.ribbon.common.menuItem
import com.appkitbox.winui4k.sample.ribbon.common.radioItem
import com.appkitbox.winui4k.sample.ribbon.common.split

/** An icon from a Segoe Fluent Icons glyph (the same character as Icon="&#x....;" in the RibbonSpace demos). */
internal fun glyph(code: Char): RibbonIcon = RibbonIcon.glyph(code.toString())

/** The id of the [Table Tools] contextual tab group. */
internal const val TABLE_TOOLS = "table"

/** The id of the [Picture Tools] contextual tab group. */
internal const val PICTURE_TOOLS = "picture"

/**
 * The Word ribbon model (same layout as WordPage.xaml in the RibbonSpace Word demo): [File] [Home] [Insert] [Design]
 * [Layout] [Review] [View] [Help], the [Table Tools] and [Picture Tools] contextual tabs, [Comments] [Editing] [Share] on
 * the tab row, the QAT, and the Backstage. [save] runs from [Save] in the Backstage and from "save" in the command catalog.
 */
internal fun createWordModel(save: () -> Unit): RibbonModel = RibbonModel().apply {
    applicationButtonLabel = Messages.get("common.file")
    title = Messages.get("title")
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(createDesignTab())
    tabs.add(createLayoutTab())
    tabs.add(createReviewTab())
    tabs.add(createViewTab())
    tabs.add(createHelpTab())
    contextualGroups.add(
        RibbonContextualGroupModel(TABLE_TOOLS, Messages.get("context.table.tools")).also {
            it.color = RibbonColor.parse("#0F7B6C")
            it.activation = RibbonContextualActivation.SELECT_ON_SHOW
        },
    )
    contextualGroups.add(RibbonContextualGroupModel(PICTURE_TOOLS, Messages.get("context.picture.tools")).also { it.color = RibbonColor.parse("#8764B8") })
    tabs.add(createTableDesignTab())
    tabs.add(createTableLayoutTab())
    tabs.add(createPictureFormatTab())
    quickAccessItems.add(button("qat.save", Messages.get("common.save"), RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("qat.undo", Messages.get("common.undo"), RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("qat.redo", Messages.get("common.redo"), RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    for (id in listOf("cut", "copy", "paste", "bold", "newComment")) quickAccessCandidates.add(findItem(id)!!)
    commandCatalog = RibbonCommandCatalog().also {
        it.register("save", Messages.get("common.save"), RibbonCommand { save() }, RibbonIcons.SAVE, "Ctrl+S", Messages.get("catalog.save.description"), Messages.get("common.file"))
    }
    addTabStripItems(this)
    createBackstage(this, save)
}

/** [Comments] [Editing] [Share] at the right end of the tab row. */
private fun addTabStripItems(model: RibbonModel) {
    model.tabStripItems.add(button("comments", Messages.get("common.comments"), glyph('\uE90A')).also { it.showLabel = true })
    model.tabStripItems.add(
        dropDown(
            "editingMode",
            Messages.get("common.editing"),
            glyph('\uE70F'),
            RibbonItemSize.MEDIUM,
            radioItem("mode.editing", Messages.get("common.editing"), "mode", checked = true),
            radioItem("mode.reviewing", Messages.get("mode.reviewing"), "mode"),
            radioItem("mode.viewing", Messages.get("mode.viewing"), "mode"),
        ),
    )
    model.tabStripItems.add(
        split(
            "share",
            Messages.get("common.share"),
            glyph('\uE72D'),
            RibbonItemSize.SMALL,
            menuItem("share.share", Messages.get("common.share")),
            menuItem("share.copyLink", Messages.get("common.share.copy.link")),
            menuItem("share.sendCopy", Messages.get("share.send.copy")),
        ),
    )
}

private fun createBackstage(model: RibbonModel, save: () -> Unit) {
    val backstage = model.backstage
    backstage.title = "Word"
    fun page(id: String, label: String, icon: RibbonIcon, keyTip: String?, content: WComponent?) =
        RibbonBackstageItemModel(id, label, icon, content).also { it.keyTip = keyTip }
    backstage.items.add(page("home", Messages.get("common.home"), glyph('\uE80F'), "H", backstageHome()))
    backstage.items.add(page("new", Messages.get("common.backstage.new"), glyph('\uE8A5'), "N", WLabel(Messages.get("backstage.new.text"))))
    backstage.items.add(page("open", Messages.get("common.backstage.open"), glyph('\uE8E5'), "O", WLabel(Messages.get("backstage.open.text"))))
    backstage.items.add(page("info", Messages.get("backstage.info"), glyph('\uE946'), "I", backstageInfo()).also { it.hasSeparatorBefore = true })
    backstage.items.add(page("save", Messages.get("common.save"), glyph('\uE74E'), "S", null).also { it.command = RibbonCommand { save() } })
    backstage.items.add(page("saveAs", Messages.get("backstage.save.as"), glyph('\uE792'), "A", WLabel(Messages.get("backstage.save.as.text"))))
    backstage.items.add(page("print", Messages.get("backstage.print"), glyph('\uE749'), "P", WLabel(Messages.get("backstage.print.text"))))
    backstage.items.add(page("export", Messages.get("common.export"), glyph('\uEDE1'), "E", WLabel(Messages.get("backstage.export.text"))))
    backstage.items.add(page("close", Messages.get("backstage.close"), glyph('\uE8BB'), "C", null))
    backstage.items.add(page("account", Messages.get("backstage.account"), glyph('\uE77B'), null, WLabel(Messages.get("backstage.account.text"))).also { it.placement = RibbonBackstagePlacement.BOTTOM })
    backstage.items.add(page("options", Messages.get("common.options"), glyph('\uE713'), null, WLabel(Messages.get("backstage.options.text"))).also { it.placement = RibbonBackstagePlacement.BOTTOM })
}

/** The Backstage [Home] page (greeting, template cards, and recent files). */
private fun backstageHome(): WComponent {
    val panel = WPanel(spacing = 16.0)
    panel.add(WLabel(Messages.get("backstage.greeting")).also { it.fontSize = GREETING_SIZE })
    val cards = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    cards.add(templateCard(Messages.get("backstage.template.blank"), "RibbonCommandBarBackgroundBrush"))
    cards.add(templateCard(Messages.get("backstage.template.report"), "RibbonAccentSubtleBrush"))
    cards.add(templateCard(Messages.get("backstage.template.resume"), "RibbonAccentSubtleStrongBrush"))
    panel.add(cards)
    panel.add(WLabel(Messages.get("backstage.recent")).also { it.fontWeight = SEMI_BOLD })
    panel.add(WLabel(Messages.get("backstage.recent.report")).also { it.opacity = SUBTLE })
    panel.add(WLabel(Messages.get("backstage.recent.roadmap")).also { it.opacity = SUBTLE })
    return panel
}

private fun templateCard(label: String, backgroundKey: String): WComponent {
    val card = WBorder(WLabel(label).also { it.verticalAlignmentInParent = VerticalAlignment.BOTTOM })
    card.width = CARD_WIDTH
    card.height = CARD_HEIGHT
    card.cornerRadius = CARD_RADIUS
    card.borderThickness = 1.0
    card.padding = CARD_PADDING
    WRibbonTheme.setThemeBrush(card, backgroundKey) { card.background = it }
    WRibbonTheme.setThemeBrush(card, "RibbonCommandBarBorderBrush") { card.borderColor = it }
    return card
}

/** The Backstage [Info] page (document protection and inspection). */
private fun backstageInfo(): WComponent {
    val panel = WPanel(spacing = 8.0)
    panel.add(WLabel(Messages.get("backstage.protect")).also { it.fontWeight = SEMI_BOLD })
    panel.add(WLabel(Messages.get("backstage.protect.text")).also { it.textWrapping = TextWrapping.WRAP })
    panel.add(WLabel(Messages.get("backstage.inspect")).also { it.fontWeight = SEMI_BOLD })
    panel.add(WLabel(Messages.get("backstage.inspect.text")).also { it.textWrapping = TextWrapping.WRAP })
    return panel
}

private const val GREETING_SIZE = 20.0
private const val SEMI_BOLD = 600
private const val SUBTLE = 0.8
private const val CARD_WIDTH = 150.0
private const val CARD_HEIGHT = 190.0
private const val CARD_RADIUS = 6.0
private const val CARD_PADDING = 10.0
