package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonGridPickerModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMenuHeaderModel
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonNodeModel

/**
 * Creates WinUI menus from the model's menu items (RibbonElementFactory.CreateFlyout / CreateMenuItems in RibbonSpace).
 *
 * Menus are rebuilt from the model each time they open (so the checked state, the enabled state, and added or removed
 * items always match the model). If the items are only menu items, separators, and headers, a MenuFlyout is used; if they
 * include galleries, color palettes, table insertion, arbitrary items, or content, a rich Flyout stacked vertically is used.
 */
internal object RibbonMenus {
    /** Whether the entries are only menu items, separators, and headers (whether a MenuFlyout can represent them). */
    fun isSimple(entries: List<RibbonNodeModel>): Boolean =
        entries.all { it is RibbonMenuItemModel || it is RibbonMenuSeparatorModel || it is RibbonMenuHeaderModel }

    /**
     * Creates the drop-down for [entries] (and an optional [content]). Returns null if there are no items at all.
     * [anchorPlacement] is where it opens. [onChosen] is called when a menu item is chosen (for split buttons that
     * follow the last choice).
     */
    fun createFlyout(
        entries: () -> List<RibbonNodeModel>,
        content: () -> WComponent?,
        host: RibbonItemHost,
        placement: FlyoutPlacement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT,
        onChosen: ((RibbonMenuItemModel) -> Unit)? = null,
    ): WFlyoutBase {
        if (content() == null && isSimple(entries())) {
            val menu = WMenuFlyout()
            menu.placement = placement
            applyMenuStyle(menu)
            onOpening(menu) {
                menu.removeAll()
                createMenuItems(entries(), host, onChosen).forEach { menu.add(it) }
            }
            return menu
        }
        val flyout = WFlyout()
        flyout.placement = placement
        applyFlyoutStyle(flyout, bare = false)
        val flyoutHost = RibbonFlyoutHost(host, flyout)
        onOpening(flyout) {
            flyoutHost.disposeViews()
            flyout.content = createRichPanel(entries(), content(), flyoutHost, onChosen)
        }
        onClosed(flyout) { flyoutHost.disposeViews() }
        return flyout
    }

    /** Menu items created from models (used when proceeding into a menu with KeyTips). */
    private val menuModels = java.util.WeakHashMap<WMenuFlyoutItemBase, RibbonMenuItemModel>()

    /** The model of a menu item (only for items created by [createMenuItems]). */
    fun modelOf(item: WMenuFlyoutItemBase): RibbonMenuItemModel? = menuModels[item]

    /** Creates menu items (MenuFlyoutItem / Toggle / Radio / SubItem / Separator). */
    fun createMenuItems(
        entries: List<RibbonNodeModel>,
        host: RibbonItemHost,
        onChosen: ((RibbonMenuItemModel) -> Unit)? = null,
    ): List<WMenuFlyoutItemBase> = entries.filter { it.isVisible }.mapNotNull { entry ->
        when (entry) {
            is RibbonMenuSeparatorModel -> WMenuFlyoutSeparator()
            is RibbonMenuHeaderModel -> WMenuFlyoutItem(entry.label.orEmpty()).also {
                it.isEnabled = false
                it.setFontWeightSemiBold()
            }
            is RibbonMenuItemModel -> if (entry.items.isNotEmpty()) {
                WMenuFlyoutSubItem(entry.label.orEmpty()).also { sub ->
                    sub.isEnabled = entry.isEnabled
                    setIcon(sub.inspectable, XamlInterop.IMenuFlyoutSubItem_put_Icon, entry.icon)
                    decorate(sub, entry)
                    menuModels[sub] = entry
                    createMenuItems(entry.items, host, onChosen).forEach { sub.add(it) }
                }
            } else {
                createMenuItem(entry, entries, host, onChosen)
            }
            else -> null
        }
    }

    private fun createMenuItem(
        entry: RibbonMenuItemModel,
        siblings: List<RibbonNodeModel>,
        host: RibbonItemHost,
        onChosen: ((RibbonMenuItemModel) -> Unit)?,
    ): WMenuFlyoutItem {
        val item: WMenuFlyoutItem = when {
            entry.isCheckable && entry.groupName != null -> WRadioMenuFlyoutItem(entry.label.orEmpty(), entry.groupName!!).also { it.isChecked = entry.isChecked }
            entry.isCheckable -> WToggleMenuFlyoutItem(entry.label.orEmpty()).also { it.isChecked = entry.isChecked }
            else -> WMenuFlyoutItem(entry.label.orEmpty())
        }
        item.isEnabled = entry.isEnabled && isCommandEnabled(entry, host)
        setIcon(item.inspectable, XamlInterop.IMenuFlyoutItem_put_Icon, entry.icon)
        entry.shortcut?.let { item.keyboardAcceleratorText = it }
        decorate(item, entry)
        menuModels[item] = entry
        item.addActionListener {
            invokeMenuItem(entry, siblings, host)
            onChosen?.invoke(entry)
        }
        return item
    }

    /**
     * Invokes a menu item: updates the checked state (a radio item unchecks the others in its group), runs the command,
     * and notifies the invocation.
     */
    fun invokeMenuItem(entry: RibbonMenuItemModel, siblings: List<RibbonNodeModel>, host: RibbonItemHost) {
        if (entry.isCheckable) {
            val group = entry.groupName
            if (group != null) {
                entry.isChecked = true
                siblings.filterIsInstance<RibbonMenuItemModel>().filter { it !== entry && it.groupName == group }.forEach { it.isChecked = false }
            } else {
                entry.isChecked = !entry.isChecked
            }
        }
        val parameter = entry.commandParameter
        val command = entry.command
        if (command != null) {
            if (command.canExecute(parameter)) command.execute(parameter)
        } else {
            entry.commandId?.let { host.commandCatalog?.execute(it, parameter) }
        }
        entry.fireActionPerformed(parameter)
        host.itemInvoked(entry, entry.commandId, parameter)
    }

    private fun isCommandEnabled(entry: RibbonMenuItemModel, host: RibbonItemHost): Boolean {
        val descriptor = host.commandCatalog?.find(entry.commandId)
        if (descriptor != null && !descriptor.isEnabled) return false
        val command = entry.command ?: descriptor?.command
        return command?.canExecute(entry.commandParameter) ?: true
    }

    /** Attaches the ScreenTip and the automation help text. */
    private fun decorate(item: WMenuFlyoutItemBase, entry: RibbonMenuItemModel) {
        val element = XamlElement(item.inspectable.also { it.addRef() })
        entry.screenTip?.let { element.setToolTipValue(RibbonScreenTips.create(entry.label, it, entry.description, entry.shortcut, entry.isEnabled)) }
        element.setAutomationHelpText(entry.screenTip?.description ?: entry.description)
    }

    /** Represents an item (a button, toggle, or check box) as a menu item, such as in the overflow menu. */
    fun commandItem(view: RibbonItemView, isChecked: Boolean? = null): WMenuFlyoutItem {
        val model = view.model
        val item = if (isChecked == null) WMenuFlyoutItem(model.label.orEmpty()) else WToggleMenuFlyoutItem(model.label.orEmpty()).also { it.isChecked = isChecked }
        item.isEnabled = view.isEffectivelyEnabled
        setIcon(item.inspectable, XamlInterop.IMenuFlyoutItem_put_Icon, model.icon)
        model.shortcut?.let { item.keyboardAcceleratorText = it }
        item.addActionListener { view.invoke() }
        return item
    }

    /** Sets a menu item's icon ([slot] is put_Icon). Icons that cannot be represented (such as line art) are not set. */
    fun setIcon(target: ComPtr, slot: Int, icon: RibbonIcon?) {
        val xaml = RibbonIconXaml.menuIcon(icon) ?: return
        val element = Xaml.load(xaml)
        try {
            val iconElement = element.queryInterface(XamlInterop.IID_IIconElement)
            target.call(slot, iconElement.ptr)
            iconElement.release()
        } finally {
            element.release()
        }
    }

    /** Gives a menu the ribbon look (RibbonMenuFlyoutPresenterStyle). */
    fun applyMenuStyle(menu: WMenuFlyout) {
        RibbonThemeResources.ensure()
        val style = WinUiUtilities.lookupApplicationResource("RibbonMenuFlyoutPresenterStyle")
        try {
            menu.inspectable.call(XamlInterop.IMenuFlyout_put_MenuFlyoutPresenterStyle, style.ptr)
        } finally {
            style.release()
        }
    }

    /** Gives a flyout the ribbon look (with a border; with [bare], no border and no padding). */
    fun applyFlyoutStyle(flyout: WFlyout, bare: Boolean) {
        RibbonThemeResources.ensure()
        val style = WinUiUtilities.lookupApplicationResource(if (bare) "RibbonBareFlyoutPresenterStyle" else "RibbonFlyoutPresenterStyle")
        try {
            flyout.inspectable.call(XamlInterop.IFlyout_put_FlyoutPresenterStyle, style.ptr)
        } finally {
            style.release()
        }
    }

    /** Subscribes to FlyoutBase.Opening. */
    fun onOpening(flyout: WFlyoutBase, handler: () -> Unit) {
        flyout.flyoutBase.addEventHandler("WinUI4K.FlyoutOpening", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Opening) { _, _ ->
            handler()
        }
    }

    /** Subscribes to FlyoutBase.Opened. */
    fun onOpened(flyout: WFlyoutBase, handler: () -> Unit) {
        flyout.flyoutBase.addEventHandler("WinUI4K.FlyoutOpened", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Opened) { _, _ ->
            handler()
        }
    }

    /** Subscribes to FlyoutBase.Closed. */
    fun onClosed(flyout: WFlyoutBase, handler: () -> Unit) {
        flyout.flyoutBase.addEventHandler("WinUI4K.FlyoutClosed", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Closed) { _, _ ->
            handler()
        }
    }

    /** The content of a rich drop-down: stacks content, headers, separators, menu buttons, and items vertically. */
    private fun createRichPanel(
        entries: List<RibbonNodeModel>,
        content: WComponent?,
        host: RibbonFlyoutHost,
        onChosen: ((RibbonMenuItemModel) -> Unit)?,
    ): WComponent {
        val panel = XamlElement.load("<StackPanel Spacing=\"1\" MinWidth=\"180\" />")
        content?.let {
            // When the drop-down is reopened, it is still a child of the previous content, so detach it
            Xaml.detach(it)
            panel.addChild(it)
        }
        for (entry in entries.filter { it.isVisible }) {
            when (entry) {
                is RibbonMenuSeparatorModel -> panel.addChild(separator())
                is RibbonMenuHeaderModel -> panel.addChild(
                    XamlElement.load("<TextBlock Text=\"${Xaml.escape(entry.label)}\" FontWeight=\"SemiBold\" Margin=\"8,4,8,2\" />"),
                )
                is RibbonMenuItemModel -> panel.addChild(RibbonMenuButtonView(entry, entries, host, onChosen).element)
                is RibbonGridPickerModel, is RibbonColorPickerModel, is RibbonItemModel ->
                    host.createEmbedded(entry as RibbonItemModel)?.let { panel.addChild(it) }
            }
        }
        return panel
    }

    private var pointAnchor: Pair<WPopup, XamlElement>? = null

    /**
     * Opens [flyout] at the coordinates ([x], [y]) of [relativeTo] (for right-click menus).
     * FlyoutShowOptions.Position requires boxing a float struct (Point), which the FFI cannot handle, so the flyout is
     * opened relative to a 1px transparent anchor placed at that position.
     */
    fun showAtPoint(flyout: WFlyoutBase, relativeTo: XamlElement, x: Double, y: Double) {
        val (popup, anchor) = pointAnchor ?: run {
            val border = XamlElement.load("<Border Width=\"1\" Height=\"1\" Background=\"Transparent\" IsHitTestVisible=\"False\" />")
            val created = WPopup(border)
            created.isLightDismissEnabled = false
            (created to border).also { pointAnchor = it }
        }
        val origin = relativeTo.positionInRoot()
        popup.horizontalOffset = origin[0] + x
        popup.verticalOffset = origin[1] + y
        popup.show(relativeTo)
        anchor.updateLayout()
        flyout.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
        onClosed(flyout) { popup.hide() }
        flyout.showAt(anchor)
    }

    /** A separator (a 1px line of RibbonSeparatorBrush). */
    fun separator(): XamlElement = XamlElement.load("<Rectangle Height=\"1\" Margin=\"4\" Fill=\"{ThemeResource RibbonSeparatorBrush}\" />")

    private fun WMenuFlyoutItem.setFontWeightSemiBold() {
        val control = inspectable.queryInterface(XamlInterop.IID_IControl)
        try {
            com.appkitbox.winui4k.internal.winui.XamlStructs.putFontWeight(control, XamlInterop.IControl_put_FontWeight, SEMI_BOLD)
        } finally {
            control.release()
        }
    }

    private const val SEMI_BOLD = 600
}

/**
 * The host of items inside a drop-down (a rich Flyout). When an item is invoked, it closes the drop-down and notifies
 * the original host of the invocation. The views of the items inside are discarded when the drop-down closes.
 */
internal class RibbonFlyoutHost(private val parent: RibbonItemHost, private val flyout: WFlyoutBase) : RibbonItemHost {
    private val views = mutableListOf<RibbonItemView>()

    override val metrics get() = parent.metrics
    override val textWidths get() = parent.textWidths
    override val commandCatalog get() = parent.commandCatalog
    override val ribbon get() = parent.ribbon

    override fun itemInvoked(model: RibbonNodeModel, commandId: String?, parameter: Any?) {
        flyout.hide()
        parent.itemInvoked(model, commandId, parameter)
    }

    override fun invalidateItemsLayout() {
        views.forEach { it.applyLayout(it.layout.copy(metrics = metrics)) }
    }

    /** Creates the view of an embedded item (medium size with a label; the same as CreateFlyout in RibbonSpace). */
    fun createEmbedded(model: RibbonItemModel): WComponent? {
        val view = RibbonItemViews.create(model, this, embedded = true) ?: return null
        views += view
        view.attach()
        view.applyLayout(RibbonItemLayout(RibbonItemSize.MEDIUM, metrics, isSimplified = false, showLabel = true))
        return view.element
    }

    fun disposeViews() {
        views.forEach { it.dispose() }
        views.clear()
    }
}

/**
 * A menu item button inside a rich drop-down (CreateMenuElement in RibbonSpace).
 * Checkable items show a check mark, and items with a submenu open the submenu to the right.
 */
internal class RibbonMenuButtonView(
    private val entry: RibbonMenuItemModel,
    private val siblings: List<RibbonNodeModel>,
    private val host: RibbonItemHost,
    private val onChosen: ((RibbonMenuItemModel) -> Unit)?,
) {
    val element: XamlElement = XamlElement.load(
        "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Stretch\" />",
    )

    init {
        val m = host.metrics
        val check = if (entry.isCheckable && entry.isChecked) "&#xE73E;" else ""
        val icon = RibbonIconXaml.build(entry.icon, m.smallIconSize)
        val shortcut = entry.shortcut?.let {
            "<TextBlock Grid.Column=\"3\" Text=\"${Xaml.escape(it)}\" Opacity=\"0.7\" Margin=\"16,0,0,0\" VerticalAlignment=\"Center\" />"
        }.orEmpty()
        val chevron = if (entry.items.isNotEmpty()) {
            "<FontIcon Grid.Column=\"4\" Glyph=\"&#xE76C;\" FontSize=\"9\" Margin=\"8,0,0,0\" VerticalAlignment=\"Center\" />"
        } else {
            ""
        }
        val content = XamlElement.load(
            "<Grid ColumnSpacing=\"6\"><Grid.ColumnDefinitions><ColumnDefinition Width=\"16\" /><ColumnDefinition Width=\"Auto\" />" +
                "<ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
                (if (check.isNotEmpty()) "<FontIcon Glyph=\"$check\" FontSize=\"12\" VerticalAlignment=\"Center\" />" else "") +
                (icon?.let { "<Border Grid.Column=\"1\" VerticalAlignment=\"Center\">$it</Border>" } ?: "") +
                "<TextBlock Grid.Column=\"2\" Text=\"${Xaml.escape(entry.label)}\" VerticalAlignment=\"Center\" />" +
                shortcut + chevron + "</Grid>",
        )
        element.setContent(content)
        element.isControlEnabled = entry.isEnabled
        element.setAutomationName(entry.label)
        entry.screenTip?.let { element.setToolTipValue(RibbonScreenTips.create(entry.label, it, entry.description, entry.shortcut, entry.isEnabled)) }
        if (entry.items.isNotEmpty()) {
            val menu = WMenuFlyout()
            menu.placement = FlyoutPlacement.RIGHT_EDGE_ALIGNED_TOP
            RibbonMenus.applyMenuStyle(menu)
            RibbonMenus.onOpening(menu) {
                menu.removeAll()
                RibbonMenus.createMenuItems(entry.items, host, onChosen).forEach { menu.add(it) }
            }
            element.onClick { menu.showAt(element) }
        } else {
            element.onClick {
                RibbonMenus.invokeMenuItem(entry, siblings, host)
                onChosen?.invoke(entry)
            }
        }
    }
}
