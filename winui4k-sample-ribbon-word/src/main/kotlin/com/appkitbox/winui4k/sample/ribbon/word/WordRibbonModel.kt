package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel
import com.appkitbox.winui4k.ribbon.RibbonBackstagePlacement
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonCommand
import com.appkitbox.winui4k.ribbon.RibbonCommandCatalog
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.dropDown
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.radioItem
import com.appkitbox.winui4k.sample.ribbon.shell.split

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
    applicationButtonLabel = "File"
    title = "Quarterly Report"
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(createDesignTab())
    tabs.add(createLayoutTab())
    tabs.add(createReviewTab())
    tabs.add(createViewTab())
    tabs.add(createHelpTab())
    contextualGroups.add(
        RibbonContextualGroupModel(TABLE_TOOLS, "Table Tools").also {
            it.color = RibbonColor.parse("#0F7B6C")
            it.activation = RibbonContextualActivation.SELECT_ON_SHOW
        },
    )
    contextualGroups.add(RibbonContextualGroupModel(PICTURE_TOOLS, "Picture Tools").also { it.color = RibbonColor.parse("#8764B8") })
    tabs.add(createTableDesignTab())
    tabs.add(createTableLayoutTab())
    tabs.add(createPictureFormatTab())
    quickAccessItems.add(button("qat.save", "Save", RibbonIcons.SAVE).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(button("qat.undo", "Undo", RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(button("qat.redo", "Redo", RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" })
    for (id in listOf("cut", "copy", "paste", "bold", "newComment")) quickAccessCandidates.add(findItem(id)!!)
    commandCatalog = RibbonCommandCatalog().also {
        it.register("save", "Save", RibbonCommand { save() }, RibbonIcons.SAVE, "Ctrl+S", "Save the document", "File")
    }
    addTabStripItems(this)
    createBackstage(this, save)
}

/** [Comments] [Editing] [Share] at the right end of the tab row. */
private fun addTabStripItems(model: RibbonModel) {
    model.tabStripItems.add(button("comments", "Comments", glyph('')).also { it.showLabel = true })
    model.tabStripItems.add(
        dropDown(
            "editingMode",
            "Editing",
            glyph('\uE70F'),
            RibbonItemSize.MEDIUM,
            radioItem("mode.editing", "Editing", "mode", checked = true),
            radioItem("mode.reviewing", "Reviewing", "mode"),
            radioItem("mode.viewing", "Viewing", "mode"),
        ),
    )
    model.tabStripItems.add(
        split(
            "share",
            "Share",
            glyph('\uE72D'),
            RibbonItemSize.SMALL,
            menuItem("share.share", "Share"),
            menuItem("share.copyLink", "Copy Link"),
            menuItem("share.sendCopy", "Send a Copy"),
        ),
    )
}

private fun createBackstage(model: RibbonModel, save: () -> Unit) {
    val backstage = model.backstage
    backstage.title = "Word"
    fun page(id: String, label: String, icon: RibbonIcon, keyTip: String?, content: WComponent?) =
        RibbonBackstageItemModel(id, label, icon, content).also { it.keyTip = keyTip }
    backstage.items.add(page("home", "Home", glyph(''), "H", backstageHome()))
    backstage.items.add(page("new", "New", glyph(''), "N", WLabel("Choose a template to create a new document.")))
    backstage.items.add(page("open", "Open", glyph(''), "O", WLabel("Open from recent files, OneDrive, or This PC.")))
    backstage.items.add(page("info", "Info", glyph(''), "I", backstageInfo()).also { it.hasSeparatorBefore = true })
    backstage.items.add(page("save", "Save", glyph(''), "S", null).also { it.command = RibbonCommand { save() } })
    backstage.items.add(page("saveAs", "Save As", glyph(''), "A", WLabel("Save a copy of this document.")))
    backstage.items.add(page("print", "Print", glyph(''), "P", WLabel("Print preview and printer settings.")))
    backstage.items.add(page("export", "Export", glyph(''), "E", WLabel("Create a PDF/XPS document or change the file type.")))
    backstage.items.add(page("close", "Close", glyph(''), "C", null))
    backstage.items.add(page("account", "Account", glyph(''), null, WLabel("Signed in as a demo user.")).also { it.placement = RibbonBackstagePlacement.BOTTOM })
    backstage.items.add(page("options", "Options", glyph(''), null, WLabel("Options for the WinUI4K ribbon.")).also { it.placement = RibbonBackstagePlacement.BOTTOM })
}

/** The Backstage [Home] page (greeting, template cards, and recent files). */
private fun backstageHome(): WComponent {
    val panel = WPanel(spacing = 16.0)
    panel.add(WLabel("Hello").also { it.fontSize = GREETING_SIZE })
    val cards = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    cards.add(templateCard("Blank document", "RibbonCommandBarBackgroundBrush"))
    cards.add(templateCard("Report", "RibbonAccentSubtleBrush"))
    cards.add(templateCard("Resume", "RibbonAccentSubtleStrongBrush"))
    panel.add(cards)
    panel.add(WLabel("Recent").also { it.fontWeight = SEMI_BOLD })
    panel.add(WLabel("Quarterly Report.docx — Documents").also { it.opacity = SUBTLE })
    panel.add(WLabel("Product Roadmap.docx — OneDrive").also { it.opacity = SUBTLE })
    return panel
}

private fun templateCard(label: String, backgroundKey: String): WComponent {
    val card = WBorder(WLabel(label).also { it.verticalAlignment = VerticalAlignment.BOTTOM })
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
    panel.add(WLabel("Protect Document").also { it.fontWeight = SEMI_BOLD })
    panel.add(WLabel("Control what types of changes people can make to this document.").also { it.textWrapping = TextWrapping.WRAP })
    panel.add(WLabel("Inspect Document").also { it.fontWeight = SEMI_BOLD })
    panel.add(WLabel("Before publishing this file, be aware that it contains: Document properties and author's name.").also { it.textWrapping = TextWrapping.WRAP })
    return panel
}

private const val GREETING_SIZE = 20.0
private const val SEMI_BOLD = 600
private const val SUBTLE = 0.8
private const val CARD_WIDTH = 150.0
private const val CARD_HEIGHT = 190.0
private const val CARD_RADIUS = 6.0
private const val CARD_PADDING = 10.0
