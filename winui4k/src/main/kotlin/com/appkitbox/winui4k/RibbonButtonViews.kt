package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel

/** The icon foreground when disabled. */
internal const val DISABLED_ICON_BRUSH = "{ThemeResource RibbonDisabledForegroundBrush}"

/**
 * Push buttons and drop-down buttons (RibbonButton / RibbonDropDownButton in RibbonSpace).
 * For a drop-down model ([RibbonDropDownButtonModel]), a click opens the menu and a chevron is shown.
 */
internal open class RibbonButtonView(model: RibbonItemModel, host: RibbonItemHost, private val embedded: Boolean) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<Button Style=\"{StaticResource RibbonItemButtonStyle}\" />")

    /** The drop-down (only for a drop-down model). */
    private val flyout: WFlyoutBase? = (model as? RibbonDropDownButtonModel)?.let { dropDown ->
        RibbonMenus.createFlyout({ dropDown.menuItems.toList() }, { dropDown.dropDownContent }, host)
    }

    init {
        element.onClick { onClick() }
        if (embedded) element.horizontalAlignment = HorizontalAlignment.STRETCH
        flyout?.let { f ->
            RibbonMenus.onOpened(f) { element.goToState("Active") }
            RibbonMenus.onClosed(f) { element.goToState("Inactive") }
        }
    }

    /** Whether to show the chevron. */
    protected open val showChevron: Boolean get() = (model as? RibbonButtonModel)?.showChevron == true || flyout != null

    /** The content specification for [layout]. */
    protected open fun spec(layout: RibbonItemLayout): RibbonContentSpec = RibbonContentSpec(
        label = model.label,
        icon = model.icon,
        largeIcon = model.largeIcon,
        size = layout.size,
        metrics = layout.metrics,
        showLabel = showsLabel(layout),
        showChevron = showChevron,
        isSimplified = layout.isSimplified,
        iconForeground = if (isEffectivelyEnabled) RibbonIconXaml.ICON_BRUSH else DISABLED_ICON_BRUSH,
    )

    override fun measure(layout: RibbonItemLayout): WSize {
        val content = RibbonItemContent.layout(spec(layout), host.textWidths)
        return WSize(content.width + 2, content.height + 2)
    }

    override fun applyLayoutCore() {
        val content = RibbonItemContent.layout(spec(layout), host.textWidths)
        element.setContent(XamlElement.load(content.xaml))
        element.setSize(if (embedded) Double.NaN else content.width + 2, content.height + 2)
    }

    override fun refreshEnabled() {
        super.refreshEnabled()
        applyLayoutCore()
    }

    /** Click: opens the drop-down if there is one; otherwise runs the command. */
    protected open fun onClick() {
        val f = flyout
        if (f != null) f.showAt(element) else execute()
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        onClick()
        return true
    }

    /** Opens the drop-down. */
    fun openDropDown() {
        flyout?.showAt(element)
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> {
        val dropDown = model as? RibbonDropDownButtonModel ?: return super.overflowMenuItems()
        if (dropDown.dropDownContent != null || !RibbonMenus.isSimple(dropDown.menuItems)) return super.overflowMenuItems()
        val sub = WMenuFlyoutSubItem(model.label.orEmpty())
        RibbonMenus.setIcon(sub.inspectable, com.appkitbox.winui4k.internal.winui.XamlInterop.IMenuFlyoutSubItem_put_Icon, model.icon)
        RibbonMenus.createMenuItems(dropDown.menuItems, host).forEach { sub.add(it) }
        return listOf(sub)
    }
}

/**
 * A toggle button (RibbonToggleButton in RibbonSpace). Toggles with the same [RibbonToggleButtonModel.groupName]
 * behave like radio buttons (pressing a checked item again does not uncheck it).
 */
internal class RibbonToggleButtonView(override val model: RibbonToggleButtonModel, host: RibbonItemHost, private val embedded: Boolean) :
    RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<ToggleButton Style=\"{StaticResource RibbonItemToggleButtonStyle}\" />")

    private var syncing = false

    init {
        if (embedded) element.horizontalAlignment = HorizontalAlignment.STRETCH
        element.onClick { onClick() }
    }

    override fun attach() {
        super.attach()
        syncChecked()
    }

    private fun spec(layout: RibbonItemLayout) = RibbonContentSpec(
        label = model.label,
        icon = model.icon,
        largeIcon = model.largeIcon,
        size = layout.size,
        metrics = layout.metrics,
        showLabel = showsLabel(layout),
        showChevron = model.showChevron,
        isSimplified = layout.isSimplified,
        iconForeground = if (isEffectivelyEnabled) RibbonIconXaml.ICON_BRUSH else DISABLED_ICON_BRUSH,
    )

    override fun measure(layout: RibbonItemLayout): WSize {
        val content = RibbonItemContent.layout(spec(layout), host.textWidths)
        return WSize(content.width + 2, content.height + 2)
    }

    override fun applyLayoutCore() {
        val content = RibbonItemContent.layout(spec(layout), host.textWidths)
        element.setContent(XamlElement.load(content.xaml))
        element.setSize(if (embedded) Double.NaN else content.width + 2, content.height + 2)
    }

    override fun refreshEnabled() {
        super.refreshEnabled()
        applyLayoutCore()
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "isChecked") syncChecked()
    }

    override fun applyCatalogChecked(isChecked: Boolean) {
        model.isChecked = isChecked
    }

    private fun syncChecked() {
        if (syncing) return
        syncing = true
        try {
            if (element.toggleChecked != model.isChecked) element.toggleChecked = model.isChecked
        } finally {
            syncing = false
        }
    }

    /**
     * Writes the post-click state back to the model (radio buttons are not unchecked), unchecks the others in the same
     * group, and executes.
     */
    private fun onClick() {
        val checked = element.toggleChecked == true
        setChecked(if (model.groupName != null && model.isChecked) true else checked)
        execute(model.commandParameter, model.isChecked)
    }

    private fun setChecked(checked: Boolean) {
        model.isChecked = checked
        syncChecked()
        val group = model.groupName
        if (checked && group != null) {
            host.radioScope().filterIsInstance<RibbonToggleButtonModel>()
                .filter { it !== model && it.groupName == group && it.isChecked }
                .forEach { it.isChecked = false }
        }
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        setChecked(if (model.groupName != null) true else !model.isChecked)
        execute(model.commandParameter, model.isChecked)
        return true
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = listOf(RibbonMenus.commandItem(this, model.isChecked))
}

/** A check box (RibbonCheckBox in RibbonSpace). Also supports the indeterminate state. */
internal class RibbonCheckBoxView(override val model: RibbonCheckBoxModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<ToggleButton Style=\"{StaticResource RibbonCheckBoxStyle}\" />")
    private val label: XamlElement = XamlElement.load("<TextBlock TextLineBounds=\"Full\" IsTextScaleFactorEnabled=\"False\" />")
    private var syncing = false

    init {
        element.setContent(label)
        element.onClick {
            if (!syncing) {
                model.isChecked = element.toggleChecked
                execute(model.commandParameter, model.isChecked)
            }
        }
    }

    override fun attach() {
        super.attach()
        syncChecked()
    }

    override fun measure(layout: RibbonItemLayout): WSize {
        val text = model.label.orEmpty()
        val textWidth = if (text.isEmpty()) 0.0 else host.textWidths.widths(listOf(text), layout.metrics.fontSize, false)[0] + LABEL_GAP
        val height = if (layout.isSimplified) layout.metrics.simplifiedItemHeight else layout.metrics.rowHeight
        return WSize(PADDING_LEFT + BOX_SIZE + textWidth + PADDING_RIGHT, height)
    }

    override fun applyLayoutCore() {
        label.setText(model.label)
        label.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_ITextBlock)
            .call(com.appkitbox.winui4k.internal.winui.XamlInterop.ITextBlock_put_FontSize, layout.metrics.fontSize)
        val size = measure(layout)
        element.setSize(size.width, size.height)
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "isChecked" -> syncChecked()
            "isThreeState" -> element.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IToggleButton)
                .putBool(com.appkitbox.winui4k.internal.winui.XamlInterop.IToggleButton_put_IsThreeState, model.isThreeState)
        }
    }

    override fun applyCatalogChecked(isChecked: Boolean) {
        model.isChecked = isChecked
    }

    private fun syncChecked() {
        syncing = true
        try {
            element.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IToggleButton)
                .putBool(com.appkitbox.winui4k.internal.winui.XamlInterop.IToggleButton_put_IsThreeState, model.isThreeState)
            if (element.toggleChecked != model.isChecked) element.toggleChecked = model.isChecked
        } finally {
            syncing = false
        }
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        model.isChecked = model.isChecked != true
        execute(model.commandParameter, model.isChecked)
        return true
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = listOf(RibbonMenus.commandItem(this, model.isChecked == true))

    private companion object {
        const val PADDING_LEFT = 4.0
        const val PADDING_RIGHT = 6.0
        const val BOX_SIZE = 14.0
        const val LABEL_GAP = 6.0
    }
}

/**
 * A split button (RibbonSplitButton in RibbonSpace). It has a primary action part and a drop-down part.
 * In the large layout they are split vertically (the icon on top; the label and chevron below); in the medium and small
 * layouts they are split horizontally (the chevron on the right).
 * With [RibbonSplitButtonModel.followLastChoice], the primary action part shows and runs the last chosen menu item.
 */
internal class RibbonSplitButtonView(override val model: RibbonSplitButtonModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<ContentControl Style=\"{StaticResource RibbonSplitButtonHostStyle}\" />")
    private val primary: XamlElement by lazy { element.templatePart("PART_PrimaryButton") }
    private val secondary: XamlElement by lazy { element.templatePart("PART_SecondaryButton") }
    private val flyout: WFlyoutBase = RibbonMenus.createFlyout(
        { model.menuItems.toList() },
        { model.dropDownContent },
        host,
        onChosen = { choice -> if (model.followLastChoice) model.lastChoice = choice },
    )

    init {
        primary.onClick { invokePrimary() }
        secondary.onClick { openDropDown() }
        RibbonMenus.onOpened(flyout) { element.goToState("DropDownOpen") }
        RibbonMenus.onClosed(flyout) { element.goToState("DropDownClosed") }
        secondary.onKeyDown { e ->
            if (e.key == VK_F4 || (e.key == VK_DOWN && Xaml.isKeyDown(VK_MENU))) {
                openDropDown()
                e.handled = true
            }
        }
    }

    override fun attach() {
        super.attach()
        updateStates()
    }

    private val isVertical: Boolean get() = layout.size == RibbonItemSize.LARGE && !layout.isSimplified

    /** How the primary action part is presented (the label and icon of the last chosen item when following the last choice). */
    private fun primaryPresentation(): Triple<String?, com.appkitbox.winui4k.ribbon.RibbonIcon?, com.appkitbox.winui4k.ribbon.RibbonIcon?> {
        val choice = model.lastChoice
        if (!model.followLastChoice || choice == null) return Triple(model.label, model.icon, model.largeIcon)
        return Triple(choice.label ?: model.label, choice.icon ?: model.icon, choice.largeIcon ?: choice.icon ?: model.largeIcon)
    }

    private fun parts(layout: RibbonItemLayout): Pair<RibbonContentLayout, RibbonContentLayout> {
        val (label, icon, largeIcon) = primaryPresentation()
        val vertical = layout.size == RibbonItemSize.LARGE && !layout.isSimplified
        val iconBrush = if (isEffectivelyEnabled) RibbonIconXaml.ICON_BRUSH else DISABLED_ICON_BRUSH
        val base = RibbonContentSpec(label, icon, largeIcon, layout.size, layout.metrics, isSimplified = layout.isSimplified, colorBar = model.colorBar, iconForeground = iconBrush)
        val primarySpec = if (vertical) {
            base.copy(part = RibbonContentPart.ICON_ONLY)
        } else {
            base.copy(showLabel = showsLabel(layout))
        }
        val secondarySpec = if (vertical) {
            base.copy(label = model.label, icon = null, largeIcon = null, part = RibbonContentPart.LABEL_AND_CHEVRON, showChevron = true, colorBar = null)
        } else {
            base.copy(label = null, icon = null, largeIcon = null, showChevron = true, colorBar = null)
        }
        return RibbonItemContent.layout(primarySpec, host.textWidths) to RibbonItemContent.layout(secondarySpec, host.textWidths)
    }

    override fun measure(layout: RibbonItemLayout): WSize {
        val (p, s) = parts(layout)
        val vertical = layout.size == RibbonItemSize.LARGE && !layout.isSimplified
        return if (vertical) WSize(maxOf(p.width, s.width) + 2, p.height + s.height + 2) else WSize(p.width + s.width + 2, maxOf(p.height, s.height) + 2)
    }

    override fun applyLayoutCore() {
        val (p, s) = parts(layout)
        primary.setContent(XamlElement.load(p.xaml))
        secondary.setContent(XamlElement.load(s.xaml))
        if (isVertical) {
            val width = maxOf(p.width, s.width)
            primary.setSize(width, p.height)
            secondary.setSize(width, s.height)
            primary.setCanvasPositionInGrid(0.0, 0.0)
            secondary.setCanvasPositionInGrid(0.0, p.height)
            element.setSize(width + 2, p.height + s.height + 2)
        } else {
            val height = maxOf(p.height, s.height)
            primary.setSize(p.width, height)
            secondary.setSize(s.width, height)
            primary.setCanvasPositionInGrid(0.0, 0.0)
            secondary.setCanvasPositionInGrid(p.width, 0.0)
            element.setSize(p.width + s.width + 2, height + 2)
        }
        val name = (primaryPresentation().first ?: model.label)?.replace('\n', ' ').orEmpty()
        primary.setAutomationName(name)
        secondary.setAutomationName(RibbonStrings.current.splitButtonOptions(name).trim())
        updateStates()
    }

    /** Places a part in the Grid with the top-left as the origin (positioned by margins within a single Grid cell). */
    private fun XamlElement.setCanvasPositionInGrid(x: Double, y: Double) {
        horizontalAlignment = HorizontalAlignment.LEFT
        verticalAlignment = VerticalAlignment.TOP
        setMargin(x, y, 0.0, 0.0)
    }

    override fun refreshEnabled() {
        super.refreshEnabled()
        runCatching {
            primary.isControlEnabled = isEffectivelyEnabled
            secondary.isControlEnabled = model.isEnabled
        }
        applyLayoutCore()
    }

    override fun toolTipTarget(): XamlElement = primary

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "isChecked", "isCheckable" -> updateStates()
            "lastChoice", "followLastChoice", "colorBar" -> {
                host.invalidateItemsLayout()
                applyLayoutCore()
            }
        }
    }

    override fun applyCatalogChecked(isChecked: Boolean) {
        model.isChecked = isChecked
    }

    private fun updateStates() {
        element.goToState(if (model.isCheckable && model.isChecked) "Checked" else "Unchecked")
    }

    /**
     * Primary action: toggles if checkable, runs the last chosen item when following the last choice, and otherwise
     * runs the command.
     */
    private fun invokePrimary() {
        if (model.isCheckable) model.isChecked = !model.isChecked
        val choice = model.lastChoice
        if (model.followLastChoice && choice != null) {
            RibbonMenus.invokeMenuItem(choice, model.menuItems, host)
            model.fireActionPerformed(choice.commandParameter)
            host.itemInvoked(model, model.commandId, choice.commandParameter)
        } else {
            execute(if (model.isCheckable) model.isChecked else model.commandParameter)
        }
    }

    /** Opens the drop-down. */
    fun openDropDown() {
        if (model.isEnabled) flyout.showAt(element)
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        invokePrimary()
        return true
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        openDropDown()
        return RibbonKeyTipResult.Close
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> {
        if (model.dropDownContent != null || !RibbonMenus.isSimple(model.menuItems)) {
            return listOf(RibbonMenus.commandItem(this, if (model.isCheckable) model.isChecked else null))
        }
        val sub = WMenuFlyoutSubItem(model.label.orEmpty())
        RibbonMenus.setIcon(sub.inspectable, com.appkitbox.winui4k.internal.winui.XamlInterop.IMenuFlyoutSubItem_put_Icon, model.icon)
        sub.add(RibbonMenus.commandItem(this))
        sub.add(WMenuFlyoutSeparator())
        RibbonMenus.createMenuItems(model.menuItems, host, onChosen = { if (model.followLastChoice) model.lastChoice = it }).forEach { sub.add(it) }
        return listOf(sub)
    }

    private companion object {
        const val VK_DOWN = 40
        const val VK_MENU = 18
        const val VK_F4 = 115
    }
}
