package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonApplicationMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonApplicationMenuModel
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonNodeModel
import com.appkitbox.winui4k.ribbon.RibbonRecentItemModel
import com.appkitbox.winui4k.ribbon.RibbonSearchEngine
import com.appkitbox.winui4k.ribbon.RibbonSearchEntry
import com.appkitbox.winui4k.ribbon.RibbonSearchResult
import com.appkitbox.winui4k.ribbon.RibbonStrings

/**
 * An application menu in the style of AutoCAD's Menu Browser (and the Office 2007 application menu; RibbonSpace's
 * RibbonApplicationMenu): a command search box, commands on the left (hovering one shows its subcommands on the right),
 * pinnable recent documents, and buttons at the bottom. Setting the flyout from [createFlyout] to
 * [WRibbon.applicationMenu] makes the application button open it.
 */
class WRibbonApplicationMenu @JvmOverloads constructor(
    /** The menu's model. */
    val model: RibbonApplicationMenuModel = RibbonApplicationMenuModel(),
    /** The ribbon that the search box searches and that is notified of invocations (if null, only the menu's commands). */
    ribbon: WRibbon? = null,
) : WRibbonBar(XAML) {
    private val search = root.part("PART_Search")
    private val itemsPanel = root.part("PART_Items")
    private val paneHeader = root.part("PART_PaneHeader")
    private val panePanel = root.part("PART_Pane")
    private val footer = root.part("PART_Footer")
    private val host = RibbonStandaloneHost(this)
    private val footerStrip = RibbonItemStrip(model.footerItems, host, { RibbonItemLayout(RibbonItemSize.MEDIUM, RibbonMetrics.COMFORTABLE, false, true) }, spacing = 8.0)
    private val itemsListener = RibbonListListener<RibbonApplicationMenuItemModel> { rebuild() }
    private val recentListener = RibbonListListener<RibbonRecentItemModel> { rebuild() }
    private var flyout: WFlyout? = null
    private val commandButtons = LinkedHashMap<RibbonApplicationMenuItemModel, XamlElement>()
    private val paneButtons = mutableListOf<XamlElement>()

    /** The item whose subcommands are shown (null while recent documents or search results are shown). */
    var shownItem: RibbonApplicationMenuItemModel? = null
        private set

    init {
        this.ribbon = ribbon
        root.addChild(measureHost)
        footer.setChild(footerStrip.element)
        search.setPlaceholderText(RibbonStrings.current.searchCommands)
        search.setAutomationName(RibbonStrings.current.searchCommands)
        search.onTextChanged { onSearchChanged() }
        search.onPreviewKeyDown { e -> onSearchKey(e) }
        model.items.addListListener(itemsListener)
        model.recentItems.addListListener(recentListener)
        model.addPropertyChangeListener { rebuild() }
        footerStrip.attach()
        rebuild()
    }

    override fun barScope(): List<RibbonItemModel> = model.footerItems

    /** A flyout with this menu as its content (attached to the application button). */
    fun createFlyout(): WFlyout {
        flyout?.let { return it }
        val created = WFlyout()
        created.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
        RibbonMenus.applyFlyoutStyle(created, bare = true)
        Xaml.detach(this)
        created.content = this
        RibbonMenus.onOpening(created) {
            search.textBoxText = ""
            showRecent()
        }
        flyout = created
        return created
    }

    /** Search box: Enter runs the first result, and Down moves to the first result (or the first command if there is no query). */
    private fun onSearchKey(e: XamlKeyEvent) {
        when (e.key) {
            RibbonInputViews.VK_ENTER -> searchMenu(search.textBoxText).firstOrNull()?.let { execute(it.entry) }
            RibbonInputViews.VK_DOWN -> {
                val first = if (search.textBoxText.isNotEmpty()) paneButtons.firstOrNull() else commandButtons.values.firstOrNull()
                first?.focus(XamlInterop.FocusState_Keyboard)
            }
            else -> return
        }
        e.handled = true
    }

    private fun rebuild() {
        search.isVisible = model.isSearchVisible
        itemsPanel.clearChildren()
        commandButtons.clear()
        for (item in model.items.filter { it.isVisible }) {
            if (item.hasSeparatorBefore) itemsPanel.addChild(RibbonMenus.separator())
            val button = createItemButton(item)
            commandButtons[item] = button
            itemsPanel.addChild(button)
        }
        val current = shownItem
        if (current != null && current in model.items) showSubItems(current) else showRecent()
    }

    private fun createItemButton(item: RibbonApplicationMenuItemModel): XamlElement {
        val icon = RibbonIconXaml.build(item.icon, ICON_SIZE).orEmpty()
        val chevron = if (item.items.isNotEmpty()) "<FontIcon Grid.Column=\"2\" Glyph=\"&#xE76C;\" FontSize=\"10\" VerticalAlignment=\"Center\" />" else ""
        val button = XamlElement.load(
            "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Stretch\" " +
                "Height=\"48\" Padding=\"10,0\"><Grid ColumnSpacing=\"12\"><Grid.ColumnDefinitions><ColumnDefinition Width=\"28\" />" +
                "<ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
                "<Border VerticalAlignment=\"Center\" HorizontalAlignment=\"Center\">$icon</Border>" +
                "<TextBlock Grid.Column=\"1\" Text=\"${Xaml.escape(item.label)}\" FontSize=\"14\" VerticalAlignment=\"Center\" />$chevron</Grid></Button>",
        )
        button.isControlEnabled = item.isEnabled
        button.setAutomationName(item.label)
        button.setAutomationId("AppMenu_" + item.id)
        button.onClick { invoke(item) }
        button.onPointer(XamlInterop.IUIElement_add_PointerEntered) { onCommandHovered(item) }
        button.onFocus(true) { onCommandHovered(item) }
        button.onKeyDown { e ->
            // Right opens the subcommands and moves to the first one
            if (e.key == RibbonInputViews.VK_RIGHT && item.items.isNotEmpty()) {
                showSubItems(item)
                paneButtons.firstOrNull()?.focus(XamlInterop.FocusState_Keyboard)
                e.handled = true
            }
        }
        return button
    }

    /** A command received the pointer or focus: shows its subcommands on the right if any, otherwise reverts to the default (recent documents). */
    internal fun onCommandHovered(item: RibbonApplicationMenuItemModel) {
        if (search.textBoxText.isNotEmpty()) return
        if (item.items.isNotEmpty()) {
            showSubItems(item)
        } else if (shownItem != null) {
            showRecent()
        }
    }

    /** Clears the content on the right. */
    private fun clearPane() {
        panePanel.clearChildren()
        paneButtons.clear()
    }

    private fun addPaneButton(button: XamlElement) {
        paneButtons += button
        panePanel.addChild(button)
    }

    /** Gives the button of the item whose subcommands are shown the selected look. */
    private fun markShown() {
        val brush = WinUiUtilities.lookupApplicationResource("RibbonItemHoverBrush")
        try {
            for ((item, button) in commandButtons) {
                button.view(XamlInterop.IID_IControl).call(XamlInterop.IControl_put_Background, if (item === shownItem) brush.ptr else null)
            }
        } finally {
            brush.release()
        }
    }

    /** Shows subcommands on the right. */
    private fun showSubItems(item: RibbonApplicationMenuItemModel) {
        shownItem = item
        paneHeader.setText(item.label)
        clearPane()
        for (sub in item.items.filter { it.isVisible }) {
            val button = createDetailButton(sub.label, sub.description, sub.icon ?: item.icon, sub.isEnabled) { invoke(sub) }
            // Left returns to the original command
            button.onKeyDown { e ->
                if (e.key == RibbonInputViews.VK_LEFT) {
                    commandButtons[item]?.focus(XamlInterop.FocusState_Keyboard)
                    e.handled = true
                }
            }
            addPaneButton(button)
        }
        markShown()
    }

    /** Shows recent documents on the right (pinned ones first). */
    private fun showRecent() {
        shownItem = null
        paneHeader.setText(model.recentHeader ?: RibbonStrings.current.recentDocuments)
        clearPane()
        val ordered = model.recentItems.filter { it.isPinned } + model.recentItems.filter { !it.isPinned }
        for (recent in ordered) panePanel.addChild(createRecentRow(recent))
        markShown()
    }

    private fun createDetailButton(label: String?, description: String?, icon: com.appkitbox.winui4k.ribbon.RibbonIcon?, enabled: Boolean, action: () -> Unit): XamlElement {
        val iconXaml = RibbonIconXaml.build(icon, ICON_SIZE).orEmpty()
        val detail = description?.let {
            "<TextBlock Text=\"${Xaml.escape(it)}\" FontSize=\"12\" Opacity=\"0.75\" TextWrapping=\"Wrap\" />"
        }.orEmpty()
        val button = XamlElement.load(
            "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Stretch\" " +
                "Padding=\"8,6\"><Grid ColumnSpacing=\"12\"><Grid.ColumnDefinitions><ColumnDefinition Width=\"28\" /><ColumnDefinition Width=\"*\" />" +
                "</Grid.ColumnDefinitions><Border VerticalAlignment=\"Top\" Margin=\"0,2,0,0\">$iconXaml</Border>" +
                "<StackPanel Grid.Column=\"1\"><TextBlock Text=\"${Xaml.escape(label)}\" FontSize=\"14\" />$detail</StackPanel></Grid></Button>",
        )
        button.isControlEnabled = enabled
        button.setAutomationName(label)
        button.onClick { action() }
        return button
    }

    private fun createRecentRow(recent: RibbonRecentItemModel): XamlElement {
        val row = XamlElement.load(
            "<Grid><Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions></Grid>",
        )
        val open = createDetailButton(recent.label, recent.path, RibbonIcons.DOCUMENT, recent.isEnabled) { openRecent(recent) }
        paneButtons += open
        row.addChild(open)
        val pin = XamlElement.load(
            "<ToggleButton Style=\"{StaticResource RibbonItemToggleButtonStyle}\" Width=\"28\" Height=\"28\" VerticalAlignment=\"Center\">" +
                "<FontIcon Glyph=\"${if (recent.isPinned) "&#xE840;" else "&#xE718;"}\" FontSize=\"12\" /></ToggleButton>",
        )
        pin.setGridCell(0, 1)
        pin.toggleChecked = recent.isPinned
        val strings = RibbonStrings.current
        val text = if (recent.isPinned) strings.unpinPanel else strings.pinPanel
        pin.setAutomationName(text)
        pin.setToolTipValue(text)
        pin.onClick {
            recent.isPinned = !recent.isPinned
            showRecent()
        }
        row.addChild(pin)
        return row
    }

    private fun openRecent(recent: RibbonRecentItemModel) {
        flyout?.hide()
        val command = recent.command
        if (command != null && command.canExecute(recent)) command.execute(recent)
        recent.fireActionPerformed(recent)
        notifyInvoked(recent, null, recent)
    }

    /** Runs a command (for an item with subcommands, opens the subcommands). */
    fun invoke(item: RibbonApplicationMenuItemModel) {
        if (!item.isEnabled) return
        if (item.items.isNotEmpty()) {
            showSubItems(item)
            return
        }
        flyout?.hide()
        val command = item.command
        if (command != null) {
            if (command.canExecute(item.commandParameter)) command.execute(item.commandParameter)
        } else {
            item.commandId?.let { (ribbon?.model?.commandCatalog)?.execute(it, item.commandParameter) }
        }
        item.fireActionPerformed(item.commandParameter)
        notifyInvoked(item, item.commandId, item.commandParameter)
    }

    private fun notifyInvoked(node: RibbonNodeModel, commandId: String?, parameter: Any?) {
        fireItemInvoked(node, commandId, parameter)
        ribbon?.onItemInvoked(node, commandId, parameter)
    }

    private fun onSearchChanged() {
        val query = search.textBoxText
        if (query.isBlank()) {
            showRecent()
            return
        }
        shownItem = null
        markShown()
        paneHeader.setText(RibbonStrings.current.searchActions)
        clearPane()
        val results = searchMenu(query)
        for (result in results) {
            val entry = result.entry
            val icon = (entry.target as? RibbonNodeModel)?.icon
            addPaneButton(createDetailButton(entry.label, entry.path, icon, entry.isEnabled) { execute(entry) })
        }
        if (results.isEmpty()) {
            panePanel.addChild(
                XamlElement.load("<TextBlock Text=\"${Xaml.escape(RibbonStrings.current.searchNoResults)}\" Margin=\"10,4,10,4\" Opacity=\"0.7\" />"),
            )
        }
    }

    /** Searches the menu's commands and (if any) the ribbon's commands. */
    fun searchMenu(query: String): List<RibbonSearchResult> {
        val engine = RibbonSearchEngine()
        val entries = mutableListOf<RibbonSearchEntry>()
        fun collect(items: List<RibbonApplicationMenuItemModel>, path: String?) {
            for (item in items.filter { it.isVisible }) {
                val label = item.label ?: continue
                entries += RibbonSearchEntry("appmenu/" + item.id, label, path, item.description, target = item, isEnabled = item.isEnabled)
                collect(item.items, label)
            }
        }
        collect(model.items, null)
        ribbon?.let { entries += it.buildSearchEntries() }
        engine.setEntries(entries)
        return engine.search(query, MAX_RESULTS)
    }

    private fun execute(entry: RibbonSearchEntry) {
        when (val target = entry.target) {
            is RibbonApplicationMenuItemModel -> invoke(target)
            else -> {
                flyout?.hide()
                ribbon?.executeSearchEntry(entry)
            }
        }
    }

    private companion object {
        const val ICON_SIZE = 24.0
        const val MAX_RESULTS = 12

        const val XAML = "<Grid Width=\"700\" MinHeight=\"520\" Background=\"{ThemeResource RibbonPopupBackgroundBrush}\">" +
            "<Grid.RowDefinitions><RowDefinition Height=\"Auto\" /><RowDefinition Height=\"*\" /><RowDefinition Height=\"Auto\" /></Grid.RowDefinitions>" +
            "<Grid.ColumnDefinitions><ColumnDefinition Width=\"250\" /><ColumnDefinition Width=\"*\" /></Grid.ColumnDefinitions>" +
            "<TextBox x:Name=\"PART_Search\" Grid.ColumnSpan=\"2\" Margin=\"10,10,10,6\" />" +
            "<ScrollViewer Grid.Row=\"1\" VerticalScrollBarVisibility=\"Auto\" Background=\"{ThemeResource RibbonCommandBarBackgroundBrush}\">" +
            "<StackPanel x:Name=\"PART_Items\" Padding=\"4\" XYFocusKeyboardNavigation=\"Enabled\" /></ScrollViewer>" +
            "<Grid Grid.Row=\"1\" Grid.Column=\"1\"><Grid.RowDefinitions><RowDefinition Height=\"Auto\" /><RowDefinition Height=\"*\" /></Grid.RowDefinitions>" +
            "<TextBlock x:Name=\"PART_PaneHeader\" FontWeight=\"SemiBold\" FontSize=\"14\" Margin=\"12,8,12,4\" />" +
            "<ScrollViewer Grid.Row=\"1\" VerticalScrollBarVisibility=\"Auto\"><StackPanel x:Name=\"PART_Pane\" Padding=\"4\" XYFocusKeyboardNavigation=\"Enabled\" />" +
            "</ScrollViewer></Grid>" +
            "<Border x:Name=\"PART_Footer\" Grid.Row=\"2\" Grid.ColumnSpan=\"2\" HorizontalAlignment=\"Right\" Margin=\"10\" /></Grid>"
    }
}
