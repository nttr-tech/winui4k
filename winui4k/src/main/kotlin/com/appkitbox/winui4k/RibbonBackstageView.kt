package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel
import com.appkitbox.winui4k.ribbon.RibbonBackstagePlacement
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonPropertyChangeListener
import com.appkitbox.winui4k.ribbon.RibbonStrings

/**
 * Backstage (the full-screen view of the File tab; RibbonBackstage in RibbonSpace).
 *
 * Shows, in a popup covering the whole window, an accent-colored navigation pane (back button, title, and top and bottom
 * items) and the selected page (a large heading and its content). Selecting a page item switches the content; an action
 * item runs its command and (if specified) closes the Backstage. The content slides in. The model is
 * [com.appkitbox.winui4k.ribbon.RibbonBackstageModel].
 */
internal class RibbonBackstageView(private val ribbon: WRibbon) {
    private val model = ribbon.model.backstage
    private val root: XamlElement = XamlElement.load(
        "<Grid Background=\"{ThemeResource RibbonBackstageContentBackgroundBrush}\">" +
            "<Grid.ColumnDefinitions><ColumnDefinition x:Name=\"PART_PaneColumn\" Width=\"200\" /><ColumnDefinition Width=\"*\" /></Grid.ColumnDefinitions>" +
            "<Grid Background=\"{ThemeResource RibbonBackstagePaneBackgroundBrush}\"><Grid.RowDefinitions><RowDefinition Height=\"Auto\" />" +
            "<RowDefinition Height=\"Auto\" /><RowDefinition Height=\"Auto\" /><RowDefinition Height=\"*\" /><RowDefinition Height=\"Auto\" /></Grid.RowDefinitions>" +
            "<Button x:Name=\"PART_BackButton\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Foreground=\"{ThemeResource RibbonBackstagePaneForegroundBrush}\" " +
            "HorizontalAlignment=\"Left\" Width=\"40\" Height=\"40\" Margin=\"12,14,0,6\" CornerRadius=\"20\"><FontIcon Glyph=\"&#xE72B;\" FontSize=\"16\" /></Button>" +
            "<TextBlock x:Name=\"PART_Title\" Grid.Row=\"1\" Margin=\"20,0,12,10\" FontSize=\"18\" FontWeight=\"SemiBold\" " +
            "Foreground=\"{ThemeResource RibbonBackstagePaneForegroundBrush}\" TextTrimming=\"CharacterEllipsis\" />" +
            "<Border x:Name=\"PART_PaneHeader\" Grid.Row=\"2\" />" +
            "<ScrollViewer Grid.Row=\"3\" VerticalScrollBarVisibility=\"Auto\" HorizontalScrollBarVisibility=\"Disabled\">" +
            "<StackPanel x:Name=\"PART_TopItems\" /></ScrollViewer>" +
            "<StackPanel x:Name=\"PART_BottomItems\" Grid.Row=\"4\" Margin=\"0,0,0,12\" /></Grid>" +
            "<Grid Grid.Column=\"1\"><Grid.RowDefinitions><RowDefinition Height=\"Auto\" /><RowDefinition Height=\"*\" /></Grid.RowDefinitions>" +
            "<TextBlock x:Name=\"PART_PageTitle\" FontSize=\"30\" FontWeight=\"SemiLight\" Margin=\"40,34,40,10\" Foreground=\"{ThemeResource RibbonForegroundBrush}\" />" +
            "<ScrollViewer Grid.Row=\"1\" VerticalScrollBarVisibility=\"Auto\" HorizontalScrollBarVisibility=\"Disabled\">" +
            "<Grid x:Name=\"PART_Content\" Margin=\"40,8,40,32\"><Grid.ChildrenTransitions><TransitionCollection>" +
            "<EntranceThemeTransition FromHorizontalOffset=\"24\" FromVerticalOffset=\"0\" /></TransitionCollection></Grid.ChildrenTransitions></Grid>" +
            "</ScrollViewer></Grid></Grid>",
    )
    private val paneColumn = root.part("PART_PaneColumn")
    private val backButton = root.part("PART_BackButton")
    private val title = root.part("PART_Title")
    private val paneHeader = root.part("PART_PaneHeader")
    private val topItems = root.part("PART_TopItems")
    private val bottomItems = root.part("PART_BottomItems")
    private val pageTitle = root.part("PART_PageTitle")
    private val content = root.part("PART_Content")
    private val popup = WPopup(root)
    private val buttons = LinkedHashMap<RibbonBackstageItemModel, XamlElement>()
    private val itemsListener = RibbonListListener<RibbonBackstageItemModel> { buildNavigation() }
    private val modelListener = RibbonPropertyChangeListener { event -> onModelChanged(event.propertyName) }
    private val itemListener = RibbonPropertyChangeListener { buildNavigation() }
    private val subscribed = mutableListOf<RibbonBackstageItemModel>()

    /** Whether it is open. */
    var isOpen: Boolean = false
        private set

    init {
        popup.isLightDismissEnabled = false
        backButton.onClick { ribbon.isBackstageOpen = false }
        backButton.setAutomationName(RibbonStrings.current.back)
        root.onKeyDown { e -> onKeyDown(e) }
        ribbon.attachPopupKeyboard(root)
        model.items.addListListener(itemsListener)
        model.addPropertyChangeListener(modelListener)
        ribbon.addSizeChangedListener { if (isOpen) resize() }
        buildNavigation()
        refreshHeader()
    }

    /** Stops observing the model (when the ribbon's model is replaced). */
    fun dispose() {
        if (isOpen) close()
        model.items.removeListListener(itemsListener)
        model.removePropertyChangeListener(modelListener)
    }

    private fun onModelChanged(name: String) {
        when (name) {
            "selectedItem" -> showPage()
            "title", "navigationPaneWidth", "paneHeader", "showPageTitle" -> refreshHeader()
        }
    }

    private fun refreshHeader() {
        title.setText(model.title)
        title.isVisible = !model.title.isNullOrEmpty()
        // GridUnitType.Pixel = 1
        XamlStructs.putGridLength(paneColumn.view(XamlInterop.IID_IColumnDefinition), XamlInterop.IColumnDefinition_put_Width, model.navigationPaneWidth, 1)
        paneHeader.setChild(model.paneHeader?.also { Xaml.detach(it) })
        showPage()
    }

    /** Rebuilds the navigation items. */
    private fun buildNavigation() {
        subscribed.forEach { it.removePropertyChangeListener(itemListener) }
        subscribed.clear()
        topItems.clearChildren()
        bottomItems.clearChildren()
        buttons.clear()
        for (item in model.items) {
            item.addPropertyChangeListener(itemListener)
            subscribed += item
            val host = if (item.placement == RibbonBackstagePlacement.BOTTOM) bottomItems else topItems
            if (item.hasSeparatorBefore && item.isVisible) {
                host.addChild(
                    XamlElement.load("<Rectangle Height=\"1\" Margin=\"16,6,16,6\" Opacity=\"0.35\" Fill=\"{ThemeResource RibbonBackstagePaneForegroundBrush}\" />"),
                )
            }
            val icon = RibbonIconXaml.build(item.icon, NAV_ICON_SIZE, "{ThemeResource RibbonBackstagePaneForegroundBrush}").orEmpty()
            val button = XamlElement.load(
                "<Button Style=\"{StaticResource RibbonBackstageNavButtonStyle}\"><StackPanel Orientation=\"Horizontal\" Spacing=\"12\">" +
                    "<Border Width=\"18\" Height=\"18\">$icon</Border>" +
                    "<TextBlock Text=\"${Xaml.escape(item.label)}\" VerticalAlignment=\"Center\" /></StackPanel></Button>",
            )
            button.isControlEnabled = item.isEnabled
            button.isVisible = item.isVisible
            button.setAutomationName(item.label)
            item.id?.let { button.setAutomationId("Backstage_$it") }
            item.screenTip?.let { button.setToolTipValue(RibbonScreenTips.create(item.label, it, item.description, null, item.isEnabled)) }
            button.onClick { invoke(item) }
            host.addChild(button)
            buttons[item] = button
        }
        updateSelection()
    }

    /** Invokes an item (selects it if it is a page; otherwise runs its command and closes if needed). */
    fun invoke(item: RibbonBackstageItemModel) {
        if (!item.isEnabled || !item.isVisible) return
        if (item.isPage) {
            model.selectedItem = item
            return
        }
        val command = item.command
        if (command != null) {
            if (command.canExecute(item.commandParameter)) command.execute(item.commandParameter)
        } else {
            item.commandId?.let { ribbon.model.commandCatalog?.execute(it, item.commandParameter) }
        }
        item.fireActionPerformed(item.commandParameter)
        ribbon.onItemInvoked(item, item.commandId, item.commandParameter)
        if (item.closesBackstage) ribbon.isBackstageOpen = false
    }

    private fun ensureSelection() {
        val selected = model.selectedItem
        val usable = selected != null && selected.isPage && selected.isVisible
        if (!usable || selected !in model.items) {
            model.selectedItem = model.items.firstOrNull { it.isPage && it.isVisible && it.isEnabled }
        }
    }

    private fun showPage() {
        val page = model.selectedItem?.takeIf { it.isPage }
        pageTitle.setText(page?.label)
        pageTitle.isVisible = model.showPageTitle && page != null
        content.clearChildren()
        page?.content?.let {
            Xaml.detach(it)
            content.addChild(it)
        }
        updateSelection()
    }

    private fun updateSelection() {
        for ((item, button) in buttons) {
            button.applyTemplate()
            button.goToState(if (item === model.selectedItem) "Selected" else "Unselected")
        }
    }

    /** Opens the Backstage (covering the whole window). */
    fun open() {
        if (isOpen) return
        isOpen = true
        ensureSelection()
        root.requestedTheme = ribbon.actualTheme
        resize()
        popup.horizontalOffset = 0.0
        popup.verticalOffset = 0.0
        popup.show(ribbon)
        showPage()
        WinUiUtilities.invokeLater {
            val target = buttons[model.selectedItem] ?: backButton
            target.focus(XamlInterop.FocusState_Programmatic)
        }
    }

    /** Closes the Backstage. */
    fun close() {
        if (!isOpen) return
        isOpen = false
        popup.hide()
    }

    private fun resize() {
        val size = Xaml.rootSize(ribbon) ?: return
        root.setSize(size[0], size[1])
    }

    private fun onKeyDown(e: XamlKeyEvent) {
        when (e.key) {
            RibbonInputViews.VK_ESCAPE -> {
                ribbon.isBackstageOpen = false
                e.handled = true
            }
            RibbonInputViews.VK_UP -> e.handled = Xaml.moveFocus(false)
            RibbonInputViews.VK_DOWN -> e.handled = Xaml.moveFocus(true)
        }
    }

    /** The KeyTip targets (the back button and the navigation items). */
    fun keyTipTargets(): List<RibbonKeyTipTarget> {
        val back = object : RibbonKeyTipTarget {
            override val keyTipLabel: String? get() = RibbonStrings.current.back
            override val explicitKeyTip: String? get() = null
            override val keyTipAnchor: XamlElement get() = backButton

            override fun onKeyTip(): RibbonKeyTipResult {
                ribbon.isBackstageOpen = false
                return RibbonKeyTipResult.Close
            }
        }
        return listOf(back) + buttons.entries.filter { it.key.isVisible }.map { (item, button) ->
            object : RibbonKeyTipTarget {
                override val keyTipLabel: String? get() = item.label
                override val explicitKeyTip: String? get() = item.keyTip
                override val keyTipAnchor: XamlElement get() = button
                override val isKeyTipEnabled: Boolean get() = item.isEnabled

                override fun onKeyTip(): RibbonKeyTipResult {
                    invoke(item)
                    return RibbonKeyTipResult.Close
                }
            }
        }
    }

    private companion object {
        const val NAV_ICON_SIZE = 18.0
    }
}
