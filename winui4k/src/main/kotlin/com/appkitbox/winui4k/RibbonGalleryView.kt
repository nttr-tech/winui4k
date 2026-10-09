package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonStrings
import kotlin.math.max
import kotlin.math.min

/**
 * A gallery in the ribbon (RibbonGallery in RibbonSpace).
 *
 * When the group is large, the items are laid out in the ribbon ([RibbonGalleryModel.maxColumns] columns, or minColumns
 * columns when medium); the up and down buttons at the right edge scroll the rows, and the expand button opens a popup with
 * categories, a filter, and a footer menu. When the group is small, in the simplified ribbon, or with
 * [RibbonGalleryModel.isDropDownOnly], it becomes a large drop-down button. Placing the pointer or the
 * keyboard focus on an item notifies a live preview.
 */
internal class RibbonGalleryView(override val model: RibbonGalleryModel, host: RibbonItemHost, private val embedded: Boolean) :
    RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<Grid />")
    private val inlineRoot = XamlElement.load(
        "<Border BorderBrush=\"{ThemeResource RibbonGalleryItemBorderBrush}\" BorderThickness=\"1\" " +
            "CornerRadius=\"{ThemeResource RibbonControlCornerRadius}\" Margin=\"0,1,0,1\" VerticalAlignment=\"Center\" HorizontalAlignment=\"Left\">" +
            "<Grid><Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
            "<Canvas x:Name=\"PART_InlineHost\" Margin=\"2\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />" +
            "<Grid Grid.Column=\"1\" Width=\"16\" BorderBrush=\"{ThemeResource RibbonGalleryItemBorderBrush}\" BorderThickness=\"1,0,0,0\">" +
            "<Grid.RowDefinitions><RowDefinition Height=\"*\" /><RowDefinition Height=\"*\" /><RowDefinition Height=\"*\" /></Grid.RowDefinitions>" +
            "<Button x:Name=\"PART_UpButton\" Style=\"{StaticResource RibbonGalleryScrollButtonStyle}\"><FontIcon Glyph=\"&#xE70E;\" FontSize=\"7\" /></Button>" +
            "<Button x:Name=\"PART_DownButton\" Grid.Row=\"1\" Style=\"{StaticResource RibbonGalleryScrollButtonStyle}\"><FontIcon Glyph=\"&#xE70D;\" FontSize=\"7\" /></Button>" +
            "<Button x:Name=\"PART_MoreButton\" Grid.Row=\"2\" Style=\"{StaticResource RibbonGalleryScrollButtonStyle}\"><FontIcon Glyph=\"&#xE972;\" FontSize=\"8\" /></Button>" +
            "</Grid></Grid></Border>",
    )
    private val inlineHost = inlineRoot.part("PART_InlineHost")
    private val upButton = inlineRoot.part("PART_UpButton")
    private val downButton = inlineRoot.part("PART_DownButton")
    private val moreButton = inlineRoot.part("PART_MoreButton")
    private val dropDownButton = XamlElement.load("<Button Style=\"{StaticResource RibbonItemButtonStyle}\" Visibility=\"Collapsed\" />")
    private val flyout = WFlyout()
    override val dropDown: WFlyoutBase get() = flyout
    private var inlineItems: List<Pair<RibbonGalleryItemModel, XamlElement>> = emptyList()
    private var popupItems: List<Pair<RibbonGalleryItemModel, XamlElement>> = emptyList()
    private var firstRow = 0
    private var columns = 1
    private var previewing = false
    private var previewEndPending = false
    private val itemsListener = RibbonListListener<RibbonGalleryItemModel> { rebuild() }

    init {
        element.addChild(inlineRoot)
        element.addChild(dropDownButton)
        upButton.onClick { scrollRows(-1) }
        downButton.onClick { scrollRows(1) }
        moreButton.onClick { openDropDown() }
        dropDownButton.onClick { openDropDown() }
        val strings = RibbonStrings.current
        upButton.setAutomationName(strings.galleryUp)
        downButton.setAutomationName(strings.galleryDown)
        moreButton.setAutomationName(strings.galleryMore)
        moreButton.setToolTipValue(strings.galleryMore)
        flyout.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
        RibbonMenus.applyFlyoutStyle(flyout, bare = false)
        RibbonMenus.onOpened(flyout) { dropDownButton.goToState("Active") }
        RibbonMenus.onClosed(flyout) {
            dropDownButton.goToState("Inactive")
            previewEndPending = false
            preview(null)
            popupItems = emptyList()
        }
    }

    override fun attach() {
        model.items.addListListener(itemsListener)
        super.attach()
        rebuild()
    }

    override fun dispose() {
        super.dispose()
        model.items.removeListListener(itemsListener)
        inlineItems.forEach { (_, button) -> button.setContent(null) }
    }

    /** Whether to lay out the items in the ribbon (IsInline in RibbonSpace). */
    private fun isInline(layout: RibbonItemLayout): Boolean =
        !embedded && !model.isDropDownOnly && layout.size != RibbonItemSize.SMALL && !layout.isSimplified

    private fun columnsFor(layout: RibbonItemLayout): Int = max(1, if (layout.size == RibbonItemSize.MEDIUM) model.minColumns else model.maxColumns)

    /** The height of the items in the ribbon (shrunk so that the rows fit in the group's height). */
    private fun inlineItemHeight(layout: RibbonItemLayout): Double {
        val rows = max(1, model.rows)
        val available = max(MIN_AVAILABLE, layout.metrics.groupContentHeight - 4)
        return min(model.itemHeight, (available - (rows - 1) * SPACING) / rows)
    }

    private fun dropDownLayout(layout: RibbonItemLayout): RibbonItemLayout {
        val size = when {
            layout.isSimplified -> RibbonItemSize.SMALL
            embedded -> RibbonItemSize.MEDIUM
            layout.size == RibbonItemSize.MEDIUM && model.isDropDownOnly -> RibbonItemSize.MEDIUM
            else -> RibbonItemSize.LARGE
        }
        val showLabel = when {
            layout.isSimplified -> showsLabelInSimplified
            size == RibbonItemSize.MEDIUM -> true
            else -> null
        }
        return layout.copy(size = size, showLabel = showLabel)
    }

    private fun dropDownSpec(layout: RibbonItemLayout): RibbonContentSpec {
        val l = dropDownLayout(layout)
        return RibbonContentSpec(
            label = model.label,
            icon = model.icon ?: DEFAULT_ICON,
            largeIcon = model.largeIcon,
            size = l.size,
            metrics = l.metrics,
            showLabel = showsLabel(l),
            showChevron = true,
            isSimplified = l.isSimplified,
            iconForeground = if (isEffectivelyEnabled) RibbonIconXaml.ICON_BRUSH else DISABLED_ICON_BRUSH,
        )
    }

    override fun measure(layout: RibbonItemLayout): WSize {
        if (!isInline(layout)) {
            val content = RibbonItemContent.layout(dropDownSpec(layout), host.textWidths)
            return WSize(content.width + 2, content.height + 2)
        }
        val cols = columnsFor(layout)
        val rows = max(1, model.rows)
        val width = BORDER * 2 + PADDING * 2 + cols * model.itemWidth + (cols - 1) * SPACING + SCROLL_COLUMN
        val height = BORDER * 2 + PADDING * 2 + rows * inlineItemHeight(layout) + (rows - 1) * SPACING + MARGIN_VERTICAL
        return WSize(width, height)
    }

    override fun applyLayoutCore() {
        val inline = isInline(layout)
        inlineRoot.isVisible = inline
        dropDownButton.isVisible = !inline
        if (inline) {
            columns = columnsFor(layout)
            arrangeInline()
        } else {
            val content = RibbonItemContent.layout(dropDownSpec(layout), host.textWidths)
            dropDownButton.setContent(XamlElement.load(content.xaml))
            dropDownButton.setSize(if (embedded) Double.NaN else content.width + 2, content.height + 2)
            if (embedded) dropDownButton.horizontalAlignment = HorizontalAlignment.STRETCH
            dropDownButton.setAutomationName(model.label)
        }
        val size = measure(layout)
        element.setSize(if (embedded) Double.NaN else size.width, size.height)
    }

    override fun toolTipTarget(): XamlElement = dropDownButton

    override fun refreshEnabled() {
        super.refreshEnabled()
        runCatching {
            dropDownButton.isControlEnabled = isEffectivelyEnabled
            inlineItems.forEach { it.second.isControlEnabled = isEffectivelyEnabled }
            moreButton.isControlEnabled = isEffectivelyEnabled
        }
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "selectedItem" -> updateSelection()
            "itemRenderer", "showLabels" -> rebuild()
            "minColumns", "maxColumns", "rows", "itemWidth", "itemHeight", "isDropDownOnly" -> {
                host.invalidateItemsLayout()
                applyLayoutCore()
            }
        }
    }

    /** Rebuilds the items in the ribbon. */
    private fun rebuild() {
        inlineItems.forEach { (_, button) -> button.setContent(null) }
        inlineHost.clearChildren()
        inlineItems = model.items.filter { it.isVisible }.map { item ->
            val button = createContainer(item, popup = false)
            inlineHost.addChild(button)
            item to button
        }
        applyLayoutCore()
        host.invalidateItemsLayout()
    }

    /** Places the items in the ribbon on a grid and shows only the visible rows. */
    private fun arrangeInline() {
        val rows = max(1, model.rows)
        val itemHeight = inlineItemHeight(layout)
        clampFirstRow()
        for ((index, pair) in inlineItems.withIndex()) {
            val row = index / columns
            val column = index % columns
            val button = pair.second
            val visible = row >= firstRow && row < firstRow + rows
            button.isVisible = visible
            if (visible) {
                button.setSize(model.itemWidth, itemHeight)
                button.setCanvasPosition(column * (model.itemWidth + SPACING), (row - firstRow) * (itemHeight + SPACING))
            }
        }
        inlineHost.setSize(columns * model.itemWidth + (columns - 1) * SPACING, rows * itemHeight + (rows - 1) * SPACING)
        updateScrollButtons()
    }

    private val rowCount: Int get() = (inlineItems.size + columns - 1) / max(1, columns)

    private fun clampFirstRow() {
        firstRow = firstRow.coerceIn(0, max(0, rowCount - max(1, model.rows)))
    }

    private fun updateScrollButtons() {
        upButton.isControlEnabled = firstRow > 0
        downButton.isControlEnabled = firstRow < max(0, rowCount - max(1, model.rows))
    }

    /** The first visible row in the ribbon. */
    val firstVisibleRow: Int get() = firstRow

    /** Whether the items are laid out in the ribbon (only the drop-down button when shrunk or drop-down only). */
    val isInline: Boolean get() = inlineRoot.isVisible

    /** Scrolls the items in the ribbon by [delta] rows. */
    fun scrollRows(delta: Int) {
        firstRow += delta
        arrangeInline()
    }

    /** Creates the button for an item. */
    private fun createContainer(item: RibbonGalleryItemModel, popup: Boolean): XamlElement {
        val button = XamlElement.load(
            "<Button Style=\"{StaticResource RibbonGalleryItemStyle}\" HorizontalAlignment=\"Stretch\" VerticalAlignment=\"Stretch\" />",
        )
        button.setContent(createItemVisual(item, popup))
        if (popup) button.setSize(model.itemWidth, model.itemHeight)
        val label = item.label
        button.setAutomationName(label.orEmpty())
        if (!label.isNullOrEmpty()) button.setToolTipValue(label)
        button.onClick { pick(item) }
        button.onPointer(XamlInterop.IUIElement_add_PointerEntered) { preview(item) }
        button.onPointer(XamlInterop.IUIElement_add_PointerExited) { preview(null) }
        button.onFocus(true) {
            previewEndPending = false
            if (button.uiElement.getInt(XamlInterop.IUIElement_get_FocusState) == XamlInterop.FocusState_Keyboard) preview(item)
        }
        button.onFocus(false) {
            previewEndPending = true
            WinUiUtilities.invokeLater {
                if (previewEndPending) {
                    previewEndPending = false
                    preview(null)
                }
            }
        }
        button.applyTemplate()
        button.goToState(if (item === model.selectedItem) "Selected" else "Unselected")
        return button
    }

    /**
     * The look of an item, in order of priority: the rendering specification → a component (in the ribbon only; a label
     * in the popup) → a text sample → an icon. A label is shown below the heading.
     */
    private fun createItemVisual(item: RibbonGalleryItemModel, popup: Boolean): WComponent {
        model.itemRenderer?.let { return it.createComponent(item) }
        val content = item.content
        if (content != null) {
            if (!popup) {
                Xaml.detach(content)
                val box = XamlElement.load("<Viewbox Margin=\"2\" />")
                box.view(XamlInterop.IID_IViewbox).call(XamlInterop.IViewbox_put_Child, content.uiElement.ptr)
                return box
            }
            return XamlElement.load(
                "<TextBlock Text=\"${Xaml.escape(item.label ?: item.id)}\" HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" " +
                    "TextWrapping=\"Wrap\" TextAlignment=\"Center\" Margin=\"2\" />",
            )
        }
        val preview = previewXaml(item)
        val label = if (model.showLabels && !item.label.isNullOrEmpty()) {
            "<TextBlock Grid.Row=\"1\" Text=\"${Xaml.escape(item.label)}\" FontSize=\"11\" HorizontalAlignment=\"Center\" " +
                "TextTrimming=\"CharacterEllipsis\" Margin=\"2,0,2,2\" />"
        } else {
            ""
        }
        return XamlElement.load(
            "<Grid><Grid.RowDefinitions><RowDefinition Height=\"*\" /><RowDefinition Height=\"Auto\" /></Grid.RowDefinitions>$preview$label</Grid>",
        )
    }

    private fun previewXaml(item: RibbonGalleryItemModel): String {
        val background = item.previewBackground?.let { " Background=\"${it.toHex(includeAlpha = true)}\"" }.orEmpty()
        val text = item.previewText
        val inner = when {
            !text.isNullOrEmpty() -> {
                val foreground = item.previewForeground?.let { " Foreground=\"${it.toHex(includeAlpha = true)}\"" }.orEmpty()
                val family = item.previewFontFamily?.let { " FontFamily=\"${Xaml.escape(it)}\"" }.orEmpty()
                val weight = if (item.previewBold) " FontWeight=\"Bold\"" else ""
                val style = if (item.previewItalic) " FontStyle=\"Italic\"" else ""
                "<TextBlock Text=\"${Xaml.escape(text)}\" FontSize=\"${Xaml.num(item.previewFontSize)}\"$foreground$family$weight$style " +
                    "HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" TextTrimming=\"Clip\" />"
            }
            item.icon != null -> RibbonIconXaml.build(
                item.icon,
                PREVIEW_ICON_SIZE,
                item.previewForeground?.toHex(includeAlpha = true) ?: RibbonIconXaml.ICON_BRUSH,
            ).orEmpty()
            else -> ""
        }
        return "<Border CornerRadius=\"3\" Margin=\"3\"$background>$inner</Border>"
    }

    private fun updateSelection() {
        for ((item, button) in inlineItems + popupItems) {
            button.goToState(if (item === model.selectedItem) "Selected" else "Unselected")
        }
    }

    /** Selects the item and executes with its value (or the item itself if there is none) as the argument. */
    fun pick(item: RibbonGalleryItemModel) {
        preview(null)
        model.selectedItem = item
        updateSelection()
        flyout.hide()
        execute(item.value ?: item)
    }

    private fun preview(item: RibbonGalleryItemModel?) {
        if (item == null && !previewing) return
        previewing = item != null
        model.firePreview(item)
    }

    /** Opens the expanded gallery (a grid per category, a filter, and a footer menu). */
    override fun openDropDown() {
        if (!isEffectivelyEnabled || flyout.isOpen) return
        val cols = if (model.dropDownColumns > 0) model.dropDownColumns else max(model.maxColumns, MIN_DROP_DOWN_COLUMNS)
        val root = XamlElement.load("<StackPanel Spacing=\"4\" Padding=\"0\" />")
        val itemsHost = XamlElement.load("<StackPanel Spacing=\"2\" XYFocusKeyboardNavigation=\"Enabled\" TabFocusNavigation=\"Once\" />")
        val filter = if (model.isFilterEnabled) {
            XamlElement.load("<TextBox Margin=\"2,2,2,4\" />").also { box ->
                box.setPlaceholderText(RibbonStrings.current.galleryFilter)
                box.onTextChanged { fill(itemsHost, cols, box.textBoxText) }
                root.addChild(box)
            }
        } else {
            null
        }
        fill(itemsHost, cols, null)
        val scroll = XamlElement.load(
            "<ScrollViewer MaxHeight=\"${Xaml.num(model.maxDropDownHeight)}\" VerticalScrollBarVisibility=\"Auto\" HorizontalScrollBarVisibility=\"Disabled\" />",
        )
        scroll.setContent(itemsHost)
        root.addChild(scroll)
        addFooter(root)
        root.onKeyDown { e ->
            if (e.key == RibbonInputViews.VK_ESCAPE) {
                flyout.hide()
                e.handled = true
            }
        }
        flyout.content = root
        if (inlineRoot.isVisible) {
            // Open it over the inline gallery (PlaceBelow(overlapAnchor: IsInline) in RibbonSpace)
            RibbonMenus.showAtPoint(flyout, inlineRoot, 0.0, -1.0)
        } else {
            flyout.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
            flyout.showAt(dropDownButton)
        }
        // Start from the selected item (or the first one), and put the focus on the filter box if there are no items
        val initial = popupItems.firstOrNull { it.first === model.selectedItem } ?: popupItems.firstOrNull()
        WinUiUtilities.invokeLater {
            if (flyout.isOpen && initial?.second?.focus(XamlInterop.FocusState_Programmatic) != true) {
                filter?.focus(XamlInterop.FocusState_Programmatic)
            }
        }
    }

    private fun fill(itemsHost: XamlElement, cols: Int, query: String?) {
        itemsHost.clearChildren()
        val created = mutableListOf<Pair<RibbonGalleryItemModel, XamlElement>>()
        val items = model.items.filter { it.isVisible && (query.isNullOrBlank() || it.label.orEmpty().contains(query, ignoreCase = true)) }
        for ((category, group) in items.groupBy { it.category }) {
            if (!category.isNullOrEmpty()) {
                itemsHost.addChild(
                    XamlElement.load("<TextBlock Text=\"${Xaml.escape(category)}\" FontWeight=\"SemiBold\" FontSize=\"12\" Margin=\"4,6,4,2\" />"),
                )
            }
            val panel = XamlElement.load("<Canvas />")
            for ((index, item) in group.withIndex()) {
                val button = createContainer(item, popup = true)
                button.setCanvasPosition((index % cols) * (model.itemWidth + SPACING), (index / cols) * (model.itemHeight + SPACING))
                panel.addChild(button)
                created += item to button
            }
            val rows = (group.size + cols - 1) / cols
            panel.setSize(cols * model.itemWidth + (cols - 1) * SPACING, rows * model.itemHeight + max(0, rows - 1) * SPACING)
            itemsHost.addChild(panel)
        }
        popupItems = created
    }

    private fun addFooter(root: XamlElement) {
        val entries = model.menuItems.filter { it.isVisible }
        if (entries.isEmpty()) return
        root.addChild(RibbonMenus.separator())
        val footerHost = RibbonFlyoutHost(host, flyout)
        for (entry in entries) {
            when (entry) {
                is RibbonMenuSeparatorModel -> root.addChild(RibbonMenus.separator())
                is RibbonMenuItemModel -> root.addChild(RibbonMenuButtonView(entry, entries, footerHost, null).element)
                is com.appkitbox.winui4k.ribbon.RibbonItemModel -> footerHost.createEmbedded(entry)?.let { root.addChild(it) }
            }
        }
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        openDropDown()
        return RibbonKeyTipResult.Close
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        openDropDown()
        return true
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> {
        val item = WMenuFlyoutItem(model.label.orEmpty())
        item.isEnabled = isEffectivelyEnabled
        RibbonMenus.setIcon(item.inspectable, XamlInterop.IMenuFlyoutItem_put_Icon, model.icon)
        item.addActionListener { WinUiUtilities.invokeLater { openDropDown() } }
        return listOf(item)
    }

    private companion object {
        const val SPACING = 2.0
        const val PADDING = 2.0
        const val BORDER = 1.0
        const val MARGIN_VERTICAL = 2.0
        const val SCROLL_COLUMN = 16.0
        const val MIN_AVAILABLE = 24.0
        const val MIN_DROP_DOWN_COLUMNS = 5
        const val PREVIEW_ICON_SIZE = 28.0

        /** The drop-down button icon for a gallery without an icon (Icon ?? "" in RibbonSpace). */
        val DEFAULT_ICON = com.appkitbox.winui4k.ribbon.RibbonIcon.glyph("")
    }
}
