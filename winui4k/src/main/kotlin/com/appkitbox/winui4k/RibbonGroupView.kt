package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonArrangeItem
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsArranger
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonGroupState
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonNodeModel
import com.appkitbox.winui4k.ribbon.RibbonPanelPresentation
import com.appkitbox.winui4k.ribbon.RibbonPropertyChangeListener
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedVisibility
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle
import kotlin.math.max

/** The contract of what hosts groups (a tab). */
internal interface RibbonGroupContainer : RibbonItemHost {
    /** Whether to show group captions. */
    val showsGroupCaptions: Boolean

    /** Whether a panel can be made floating by dragging its title. */
    val canFloatGroups: Boolean

    /** How panel buttons / panel titles are presented (AutoCAD's minimize state). */
    val panelPresentation: RibbonPanelPresentation

    /** The basis for popup colors (the ribbon's actual theme). */
    val themeSource: WComponent

    /** The size of a group changed, so lay out the tab again. */
    fun groupLayoutInvalidated(group: RibbonGroupView)

    /** A group became a floating panel or returned. */
    fun groupFloatingChanged(group: RibbonGroupView)

    /** Shows the group's right-click menu. */
    fun showGroupContextMenu(group: RibbonGroupView, x: Double, y: Double): Boolean

    /** The display name of a group (the new name if it was renamed by customization). */
    fun groupLabel(group: RibbonGroupModel): String? = group.label

    /** Routes key input in a popup to the ribbon's KeyTips and shortcuts. */
    fun attachPopupKeyboard(element: XamlElement) {
        // Does nothing by default
    }
}

/**
 * The view of a ribbon group (RibbonGroup / RibbonGroup.Panels in RibbonSpace).
 *
 * For each state (large, medium, small, collapsed, simplified ribbon), it decides the item sizes and arranges them in
 * columns (Office's classic arrangement), and shows the caption, the dialog launcher, and the arrow for the expanded panel
 * (slide-out, with pinning). In the collapsed state it becomes a large button that shows the items in a popup when clicked.
 * It can also become an AutoCAD floating panel (by dragging the title or "Float Panel"). The group itself is the host of
 * its items, and the radio scope of toggles is the group's items.
 */
internal class RibbonGroupView(val model: RibbonGroupModel, val container: RibbonGroupContainer) :
    RibbonItemHost, RibbonKeyTipTarget {
    /** The element placed in the tab. */
    val element: XamlElement = XamlElement.load(
        "<Grid HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" Background=\"Transparent\">" +
            "<Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
            "<Grid x:Name=\"PART_Body\"><Grid.RowDefinitions><RowDefinition Height=\"*\" /><RowDefinition Height=\"Auto\" /></Grid.RowDefinitions>" +
            "<Border x:Name=\"PART_ItemsPresenter\" Margin=\"4,2,4,0\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />" +
            "<Button x:Name=\"PART_CollapsedButton\" Style=\"{StaticResource RibbonItemButtonStyle}\" Visibility=\"Collapsed\" Margin=\"3,2,3,0\" />" +
            "<Grid x:Name=\"PART_CaptionRow\" Grid.Row=\"1\" Height=\"17\" Margin=\"{ThemeResource RibbonGroupCaptionMargin}\" " +
            "Background=\"{ThemeResource RibbonGroupCaptionBackgroundBrush}\" CornerRadius=\"{ThemeResource RibbonGroupCaptionCornerRadius}\">" +
            "<Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
            "<TextBlock x:Name=\"PART_Header\" Grid.ColumnSpan=\"3\" FontSize=\"11\" Foreground=\"{ThemeResource RibbonGroupCaptionForegroundBrush}\" " +
            "HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" TextTrimming=\"CharacterEllipsis\" Margin=\"16,0,16,1\" IsTextScaleFactorEnabled=\"False\" />" +
            "<Button x:Name=\"PART_SlideOutButton\" Grid.Column=\"1\" Style=\"{StaticResource RibbonDialogLauncherButtonStyle}\" Margin=\"0,0,1,1\" " +
            "Visibility=\"Collapsed\" Content=\"&#xE70D;\" />" +
            "<Button x:Name=\"PART_DialogLauncher\" Grid.Column=\"2\" Style=\"{StaticResource RibbonDialogLauncherButtonStyle}\" Margin=\"0,0,1,1\" Visibility=\"Collapsed\" />" +
            "</Grid></Grid>" +
            "<Rectangle x:Name=\"PART_Separator\" Grid.Column=\"1\" Width=\"1\" Margin=\"0,8,0,8\" Fill=\"{ThemeResource RibbonSeparatorBrush}\" />" +
            "</Grid>",
    )
    private val itemsPresenter = element.part("PART_ItemsPresenter")
    private val collapsedButton = element.part("PART_CollapsedButton")
    private val captionRow = element.part("PART_CaptionRow")
    private val header = element.part("PART_Header")
    private val slideOutButton = element.part("PART_SlideOutButton")
    private val launcher = element.part("PART_DialogLauncher")
    private val separator = element.part("PART_Separator")

    /** The Canvas that holds the items (moved to the collapsed popup or the floating panel). */
    internal val itemsPanel = XamlElement.load("<Canvas HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />")

    /** Stacks the items of the expanded panel vertically. */
    internal val slideOutPanel = XamlElement.load("<StackPanel Spacing=\"1\" />")

    private val views = mutableListOf<RibbonItemView>()
    private val slideOutViews = mutableListOf<RibbonItemView>()
    private val itemsListener = RibbonListListener<RibbonItemModel> { syncItems() }
    private val slideOutListener = RibbonListListener<RibbonItemModel> { syncSlideOut() }
    private val modelListener = RibbonPropertyChangeListener { event -> onModelChanged(event.propertyName) }

    /** The current state. */
    var state: RibbonGroupState = RibbonGroupState.LARGE
        private set

    /** Whether it is shown as the simplified ribbon. */
    var isSimplified: Boolean = false
        private set

    private var currentMetrics: RibbonMetrics = container.metrics
    private var inLineMask: List<Boolean>? = null
    private var applied = false

    /** A marker for discarding cached measurements (incremented when items are added or removed or sizes change). */
    var layoutVersion: Int = 0
        private set

    /** Whether it is hidden by customization. */
    var isHiddenByCustomization: Boolean = false

    private val collapsedFlyout = WFlyout()
    private var popupOpen = false

    /** The display name (reflecting a rename by customization). */
    val label: String? get() = container.groupLabel(model)

    /** Whether to show it (visible in the model, not hidden by customization, and not a floating panel). */
    val isShown: Boolean get() = model.isVisible && !isHiddenByCustomization && !isFloating

    override val metrics: RibbonMetrics get() = currentMetrics
    override val textWidths: RibbonTextWidths get() = container.textWidths
    override val commandCatalog get() = container.commandCatalog
    override val ribbon: WRibbon? get() = container.ribbon

    override fun itemInvoked(model: RibbonNodeModel, commandId: String?, parameter: Any?) {
        if (popupOpen) collapsedFlyout.hide()
        if (!isSlideOutPinned) closeSlideOut()
        container.itemInvoked(model, commandId, parameter)
    }

    override fun invalidateItemsLayout() {
        layoutVersion++
        applied = false
        container.groupLayoutInvalidated(this)
    }

    override fun showItemContextMenu(view: RibbonItemView, x: Double, y: Double): Boolean = container.showItemContextMenu(view, x, y)

    override fun radioScope(): List<RibbonItemModel> = RibbonModel.flatten(model.items + model.slideOutItems).toList()

    init {
        itemsPresenter.setChild(itemsPanel)
        collapsedButton.onClick { WinUiUtilities.invokeLater { openPopup() } }
        launcher.onClick { openDialogLauncher() }
        slideOutButton.onClick {
            if (isSlideOutOpen) {
                isSlideOutPinned = false
                closeSlideOut()
            } else {
                WinUiUtilities.invokeLater { openSlideOut() }
            }
        }
        collapsedFlyout.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
        RibbonMenus.applyFlyoutStyle(collapsedFlyout, bare = true)
        RibbonMenus.onOpened(collapsedFlyout) { collapsedButton.goToState("Active") }
        RibbonMenus.onClosed(collapsedFlyout) {
            collapsedButton.goToState("Inactive")
            restoreFromPopup()
        }
        element.onRightTapped { e ->
            val p = e.position(element)
            if (container.showGroupContextMenu(this, p[0], p[1])) e.markHandled()
        }
    }

    /** Starts observing the model. */
    fun attach() {
        model.addPropertyChangeListener(modelListener)
        model.items.addListListener(itemsListener)
        model.slideOutItems.addListListener(slideOutListener)
        syncItems()
        syncSlideOut()
        refreshHeader()
    }

    /** Stops observing the model. */
    fun dispose() {
        model.removePropertyChangeListener(modelListener)
        model.items.removeListListener(itemsListener)
        model.slideOutItems.removeListListener(slideOutListener)
        views.forEach { it.dispose() }
        views.clear()
        slideOutViews.forEach { it.dispose() }
        slideOutViews.clear()
        collapsedFlyout.hide()
        closeSlideOut()
        suspendFloat()
    }

    private fun onModelChanged(name: String) {
        when (name) {
            "label", "icon", "dialogLauncherCommand", "dialogLauncherCommandId", "isDialogLauncherVisible", "dialogLauncherScreenTip" -> {
                refreshHeader()
                invalidateItemsLayout()
            }
            "itemsLayout", "rowCount", "reductionOrder", "canCollapse", "simplifiedVisibility", "isVisible", "screenTip", "description" -> invalidateItemsLayout()
        }
    }

    /** Syncs the item views with the model (existing views are reused). */
    private fun syncItems() {
        val existing = views.associateBy { it.model }
        views.filter { it.model !in model.items }.forEach { it.dispose() }
        views.clear()
        itemsPanel.clearChildren()
        for (item in model.items) {
            val view = existing[item] ?: RibbonItemViews.create(item, this)?.also { it.attach() } ?: continue
            views += view
            itemsPanel.addChild(view.element)
        }
        invalidateItemsLayout()
    }

    private fun syncSlideOut() {
        val existing = slideOutViews.associateBy { it.model }
        slideOutViews.filter { it.model !in model.slideOutItems }.forEach { it.dispose() }
        slideOutViews.clear()
        slideOutPanel.clearChildren()
        for (item in model.slideOutItems) {
            val view = existing[item] ?: RibbonItemViews.create(item, this)?.also { it.attach() } ?: continue
            if (view is RibbonSeparatorView) view.isHorizontal = true
            slideOutViews += view
            slideOutPanel.addChild(view.element)
        }
        applySlideOutLayouts()
        updateSlideOutButton()
        invalidateItemsLayout()
    }

    /** Items of the expanded panel are laid out with labels (large becomes medium). */
    internal fun applySlideOutLayouts() {
        for (view in slideOutViews) {
            val preferred = view.model.effectiveSizeDefinition.getSize(RibbonGroupState.LARGE)
            val size = if (preferred == RibbonItemSize.LARGE) RibbonItemSize.MEDIUM else preferred
            view.applyLayout(RibbonItemLayout(size, currentMetrics))
        }
    }

    private fun refreshHeader() {
        header.setText(label)
        element.setAutomationName(label)
        val strings = RibbonStrings.current
        launcher.isVisible = model.hasDialogLauncher
        val launcherName = strings.dialogLauncher(label.orEmpty())
        launcher.setAutomationName(launcherName)
        launcher.setToolTipValue(
            RibbonScreenTips.create(launcherName, model.dialogLauncherScreenTip, null, null, true) ?: launcherName,
        )
        updateSlideOutButton()
    }

    // ---------------------------------------------------------------- Measurement and placement

    /** Applies the layout for [state] to the items. */
    private fun applyItemLayouts(state: RibbonGroupState, simplified: Boolean, metrics: RibbonMetrics) {
        for (view in views) view.applyLayout(itemLayout(view, state, simplified, metrics))
    }

    private fun itemLayout(view: RibbonItemView, state: RibbonGroupState, simplified: Boolean, metrics: RibbonMetrics): RibbonItemLayout {
        val size = if (simplified) RibbonItemSize.SMALL else view.model.effectiveSizeDefinition.getSize(state)
        return RibbonItemLayout(size, metrics, simplified, null, state)
    }

    /** Whether the item takes the full height (RibbonGroupItemsPanel.IsFullHeight in RibbonSpace). */
    private fun isFullHeight(view: RibbonItemView, size: WSize, layout: RibbonItemLayout): Boolean = when {
        view is RibbonSeparatorView -> !view.isHorizontal
        view is RibbonContainerView -> size.height > layout.metrics.rowHeight * FULL_HEIGHT_RATIO
        layout.size == RibbonItemSize.LARGE -> true
        else -> size.height > layout.metrics.rowHeight * FULL_HEIGHT_RATIO
    }

    /** The placement of the items in [state] (classic). */
    private fun arrangeClassic(state: RibbonGroupState, metrics: RibbonMetrics): Pair<List<Pair<RibbonItemView, DoubleArray>>, Double> {
        val shown = views.filter { it.isShown && it.model.isVisible }
        val layouts = shown.map { itemLayout(it, state, false, metrics) }
        val sizes = shown.mapIndexed { i, view -> view.measure(layouts[i]) }
        val contentHeight = metrics.groupContentHeight
        if (model.itemsLayout == RibbonGroupItemsLayout.ROWS) {
            val rows = max(1, shown.size)
            val divisor = if (model.rowCount == 3 && rows < 3) rows else max(rows, model.rowCount)
            val slot = contentHeight / divisor
            val top = (contentHeight - slot * rows) / 2
            val placements = shown.mapIndexed { i, view ->
                val h = minOf(sizes[i].height, slot)
                view to doubleArrayOf(0.0, top + i * slot + (slot - h) / 2, sizes[i].width, h)
            }
            return placements to (sizes.maxOfOrNull { it.width } ?: 0.0)
        }
        val arranged = RibbonGroupItemsArranger.arrange(
            shown.mapIndexed { i, view -> RibbonArrangeItem(sizes[i].width, sizes[i].height, isFullHeight(view, sizes[i], layouts[i])) },
            contentHeight,
            model.rowCount,
            columnSpacing = COLUMN_SPACING,
        )
        val placements = shown.mapIndexed { i, view ->
            val p = arranged.items[i]
            view to doubleArrayOf(p.x, p.y, p.width, p.height)
        }
        return placements to arranged.width
    }

    /** The width needed for the caption. */
    private fun captionWidth(metrics: RibbonMetrics): Double {
        if (!container.showsGroupCaptions) return 0.0
        val text = label.orEmpty()
        val textWidth = if (text.isEmpty()) 0.0 else textWidths.widths(listOf(text), metrics.captionFontSize, false)[0]
        val buttons = (if (model.hasDialogLauncher) 1 else 0) + (if (hasSlideOut) 1 else 0)
        val extra = if (buttons > 1) LAUNCHER_WIDTH else 0.0
        val margin = if (WRibbonTheme.style == RibbonThemeStyle.CAD) 2.0 else 0.0
        // Add a little slack so that the end is not truncated due to rounding differences between measurement and rendering
        return textWidth + CAPTION_TEXT_MARGIN * 2 + extra + margin + CAPTION_SLACK
    }

    internal val hasSlideOut: Boolean get() = model.slideOutItems.any { it.isVisible }

    private fun collapsedLayout(metrics: RibbonMetrics): RibbonItemLayout = if (container.panelPresentation == RibbonPanelPresentation.TITLES) {
        RibbonItemLayout(RibbonItemSize.MEDIUM, metrics, false, true)
    } else {
        RibbonItemLayout(RibbonItemSize.LARGE, metrics)
    }

    private fun collapsedSpec(metrics: RibbonMetrics): RibbonContentSpec {
        val layout = collapsedLayout(metrics)
        return RibbonContentSpec(
            label = label,
            icon = model.icon ?: firstIcon() ?: DEFAULT_GROUP_ICON,
            largeIcon = null,
            size = layout.size,
            metrics = metrics,
            showLabel = true,
            showChevron = true,
        )
    }

    private fun firstIcon(): RibbonIcon? = RibbonModel.flatten(model.items).firstNotNullOfOrNull { it.largeIcon ?: it.icon }

    /** The width of the group in [state] (for adaptive layout; MeasureWidth in RibbonSpace). */
    fun measureWidth(state: RibbonGroupState, metrics: RibbonMetrics): Double {
        if (state == RibbonGroupState.COLLAPSED) {
            val content = RibbonItemContent.layout(collapsedSpec(metrics), textWidths)
            return content.width + 2 + COLLAPSED_MARGIN * 2 + SEPARATOR_WIDTH
        }
        val (_, itemsWidth) = arrangeClassic(state, metrics)
        return max(itemsWidth + ITEMS_MARGIN * 2, captionWidth(metrics)) + SEPARATOR_WIDTH
    }

    /**
     * How an item is handled in the simplified ribbon (if the group's value is not AUTO, it takes precedence over all
     * its items).
     */
    private fun simplifiedVisibilityOf(view: RibbonItemView): RibbonSimplifiedVisibility =
        model.simplifiedVisibility.takeIf { it != RibbonSimplifiedVisibility.AUTO } ?: view.model.simplifiedVisibility

    /** The width of each item in the simplified ribbon (including spacing). */
    fun measureSimplifiedItemWidths(metrics: RibbonMetrics): List<Double> = views.map { view ->
        if (!view.isShown || simplifiedVisibilityOf(view) == RibbonSimplifiedVisibility.HIDDEN) {
            0.0
        } else {
            view.measure(itemLayout(view, RibbonGroupState.LARGE, true, metrics)).width + SIMPLIFIED_SPACING
        }
    }

    /** How each item is handled in the simplified ribbon (in the same order as the measurement). */
    fun simplifiedVisibilities(): List<RibbonSimplifiedVisibility> = views.map { view ->
        if (!view.isShown) RibbonSimplifiedVisibility.HIDDEN else simplifiedVisibilityOf(view)
    }

    /**
     * Applies the result of the adaptive layout. [mask] is the items kept in the row in the simplified ribbon. Returns
     * the group's width and height.
     */
    fun applyState(state: RibbonGroupState, simplified: Boolean, metrics: RibbonMetrics, mask: List<Boolean>? = null): WSize {
        this.state = state
        isSimplified = simplified
        currentMetrics = metrics
        inLineMask = mask
        if (popupOpen) return WSize(element.actualWidth, element.actualHeight)
        applied = true
        return if (simplified) applySimplified(metrics) else applyClassic(state, metrics)
    }

    private fun applyClassic(state: RibbonGroupState, metrics: RibbonMetrics): WSize {
        val collapsed = state == RibbonGroupState.COLLAPSED
        val itemState = if (collapsed) RibbonGroupState.LARGE else state
        applyItemLayouts(itemState, false, metrics)
        itemsPresenter.isVisible = !collapsed
        itemsPresenter.setMargin(ITEMS_MARGIN, 2.0, ITEMS_MARGIN, 0.0)
        collapsedButton.isVisible = collapsed
        val captions = container.showsGroupCaptions && !collapsed
        captionRow.isVisible = captions
        captionRow.setSize(Double.NaN, metrics.groupCaptionHeight)
        header.view(XamlInterop.IID_ITextBlock).call(XamlInterop.ITextBlock_put_FontSize, metrics.captionFontSize)
        separator.setMargin(0.0, SEPARATOR_MARGIN, 0.0, SEPARATOR_MARGIN)
        updateSlideOutButton()
        val height = 2 + metrics.groupContentHeight + if (captions) metrics.groupCaptionHeight else 0.0
        val width = if (collapsed) {
            val content = RibbonItemContent.layout(collapsedSpec(metrics), textWidths)
            collapsedButton.setContent(XamlElement.load(content.xaml))
            collapsedButton.setSize(content.width + 2, content.height + 2)
            collapsedButton.setAutomationName(label)
            // Use the group's ScreenTip (or its description) as the collapsed button's tooltip and automation help text
            collapsedButton.setToolTipValue(
                RibbonScreenTips.create(label, model.screenTip, model.description, null, model.isEnabled)
                    ?.takeIf { model.screenTip != null || model.description != null },
            )
            collapsedButton.setAutomationHelpText(model.screenTip?.description ?: model.description)
            content.width + 2 + COLLAPSED_MARGIN * 2 + SEPARATOR_WIDTH
        } else {
            placeClassic(itemState, metrics)
        }
        element.setSize(width, height)
        return WSize(width, height)
    }

    /** Arranges the items in columns and returns the group's width. */
    private fun placeClassic(state: RibbonGroupState, metrics: RibbonMetrics): Double {
        val (placements, itemsWidth) = arrangeClassic(state, metrics)
        val placed = placements.map { it.first }.toSet()
        for (view in views) view.element.isVisible = view in placed
        for ((view, p) in placements) view.element.setCanvasPosition(p[0], p[1])
        itemsPanel.setSize(itemsWidth, metrics.groupContentHeight)
        val inner = max(itemsWidth + ITEMS_MARGIN * 2, captionWidth(metrics))
        itemsPresenter.setSize(inner - ITEMS_MARGIN * 2, metrics.groupContentHeight)
        return inner + SEPARATOR_WIDTH
    }

    private fun applySimplified(metrics: RibbonMetrics): WSize {
        applyItemLayouts(RibbonGroupState.LARGE, true, metrics)
        itemsPresenter.isVisible = true
        itemsPresenter.setMargin(2.0, 0.0, 2.0, 0.0)
        collapsedButton.isVisible = false
        captionRow.isVisible = false
        separator.setMargin(SIMPLIFIED_SEPARATOR_MARGIN, SIMPLIFIED_SEPARATOR_VERTICAL, SIMPLIFIED_SEPARATOR_MARGIN, SIMPLIFIED_SEPARATOR_VERTICAL)
        updateSlideOutButton()
        var x = 0.0
        var count = 0
        for ((index, view) in views.withIndex()) {
            val inLine = view.isShown && simplifiedVisibilityOf(view) != RibbonSimplifiedVisibility.HIDDEN &&
                simplifiedVisibilityOf(view) != RibbonSimplifiedVisibility.OVERFLOW && (inLineMask?.getOrNull(index) ?: true)
            view.element.isVisible = inLine
            if (!inLine) continue
            val size = view.measure(view.layout)
            if (count > 0) x += SIMPLIFIED_SPACING
            view.element.setCanvasPosition(x, (metrics.simplifiedItemHeight - size.height) / 2)
            x += size.width
            count++
        }
        itemsPanel.setSize(x, metrics.simplifiedItemHeight)
        itemsPresenter.setSize(x, metrics.simplifiedItemHeight)
        val width = x + 4 + SIMPLIFIED_SEPARATOR_MARGIN * 2 + SEPARATOR_WIDTH
        element.setSize(width, metrics.simplifiedHeight)
        return WSize(width, metrics.simplifiedHeight)
    }

    /** The items not shown in the row in the simplified ribbon (moved to the overflow menu). */
    fun overflowViews(): List<RibbonItemView> = views.filterIndexed { index, view ->
        view.isShown && simplifiedVisibilityOf(view) != RibbonSimplifiedVisibility.HIDDEN &&
            (simplifiedVisibilityOf(view) == RibbonSimplifiedVisibility.OVERFLOW || inLineMask?.getOrNull(index) == false)
    } + slideOutViews.filter { it.isShown }

    /** The item views (including the expanded panel). */
    fun itemViews(): List<RibbonItemView> = views + slideOutViews

    /** Whether the items changed after the last applyState. */
    val needsLayout: Boolean get() = !applied

    // ---------------------------------------------------------------- Dialog launcher

    /** Runs the dialog launcher (the command → the catalog → the group's action notification). */
    fun openDialogLauncher() {
        val command = model.dialogLauncherCommand
        if (command != null) {
            if (command.canExecute(null)) command.execute(null)
        } else {
            model.dialogLauncherCommandId?.let { commandCatalog?.execute(it, null) }
        }
        model.fireActionPerformed(null)
        itemInvoked(model, model.dialogLauncherCommandId, null)
    }

    // ---------------------------------------------------------------- Collapsed popup

    /** Shows the items of the collapsed group in a popup. */
    fun openPopup() {
        if (popupOpen) return
        popupOpen = true
        itemsPresenter.setChild(null)
        val width = placeForPopup()
        val content = XamlElement.load("<StackPanel Padding=\"4,3,4,0\" />")
        content.addChild(itemsPanel)
        if (hasSlideOut) {
            closeSlideOut()
            Xaml.detach(slideOutPanel)
            applySlideOutLayouts()
            val host = XamlElement.load("<Border BorderThickness=\"0,1,0,0\" Padding=\"0,3,0,0\" Margin=\"0,2,0,0\" BorderBrush=\"{ThemeResource RibbonSeparatorBrush}\" />")
            host.setChild(slideOutPanel)
            content.addChild(host)
        }
        content.addChild(popupCaption())
        val chrome = XamlElement.load(
            "<Border BorderThickness=\"1\" CornerRadius=\"6\" Background=\"{ThemeResource RibbonCommandBarBackgroundBrush}\" " +
                "BorderBrush=\"{ThemeResource RibbonPopupBorderBrush}\" MinWidth=\"${Xaml.num(width)}\" />",
        )
        chrome.setChild(content)
        chrome.requestedTheme = container.themeSource.actualTheme
        container.attachPopupKeyboard(chrome)
        chrome.onKeyDown { e ->
            if (e.key == RibbonInputViews.VK_ESCAPE && !e.handled) {
                collapsedFlyout.hide()
                collapsedButton.focus()
                e.handled = true
            }
        }
        collapsedFlyout.content = chrome
        collapsedFlyout.showAt(if (collapsedButton.isVisible) collapsedButton else element)
    }

    private fun popupCaption(): XamlElement {
        val caption = XamlElement.load(
            "<Grid Height=\"${Xaml.num(currentMetrics.groupCaptionHeight)}\">" +
                "<TextBlock Text=\"${Xaml.escape(label)}\" HorizontalAlignment=\"Center\" FontSize=\"${Xaml.num(currentMetrics.captionFontSize)}\" " +
                "Margin=\"0,0,0,2\" Foreground=\"{ThemeResource RibbonSecondaryForegroundBrush}\" />" +
                "<Button x:Name=\"PART_Launcher\" Style=\"{StaticResource RibbonDialogLauncherButtonStyle}\" HorizontalAlignment=\"Right\" " +
                "Visibility=\"${if (model.hasDialogLauncher) "Visible" else "Collapsed"}\" /></Grid>",
        )
        val popupLauncher = caption.part("PART_Launcher")
        popupLauncher.setAutomationName(RibbonStrings.current.dialogLauncher(label.orEmpty()))
        popupLauncher.onClick { openDialogLauncher() }
        return caption
    }

    /** Closes the collapsed popup. */
    fun closePopup() {
        if (popupOpen) collapsedFlyout.hide()
    }

    /** Whether the popup is open. */
    val isPopupOpen: Boolean get() = popupOpen

    private fun restoreFromPopup() {
        if (!popupOpen) return
        popupOpen = false
        Xaml.detach(itemsPanel)
        Xaml.detach(slideOutPanel)
        itemsPresenter.setChild(itemsPanel)
        applyState(state, isSimplified, currentMetrics, inLineMask)
    }

    // ---------------------------------------------------------------- Expanded panel and floating panel

    private val slideOut = RibbonGroupSlideOut(this)
    private val floating = RibbonGroupFloating(this)

    /** Whether the expanded panel is pinned (it stays open, and reopens when the tab is selected again). */
    var isSlideOutPinned: Boolean
        get() = slideOut.isPinned
        set(value) {
            slideOut.isPinned = value
        }

    /** Whether the expanded panel is open. */
    val isSlideOutOpen: Boolean get() = slideOut.isOpen

    /** Opens the expanded panel (below the group, with light dismiss unless pinned). */
    fun openSlideOut() {
        val blocked = popupOpen || isFloating
        if (!hasSlideOut || blocked || !element.isVisible) return
        slideOut.open()
    }

    /** Closes the expanded panel (the pinned setting is kept). */
    fun closeSlideOut() = slideOut.close()

    internal fun focusSlideOutButton() {
        slideOutButton.focus()
    }

    /** Updates the expanded panel arrow (its direction depends on whether the panel is open). */
    internal fun updateSlideOutButton() {
        slideOutButton.isVisible = hasSlideOut && !isSimplified && state != RibbonGroupState.COLLAPSED
        slideOutButton.setContentText(if (slideOut.isOpen) "\uE70E" else "\uE70D")
        val name = RibbonStrings.current.expandPanel(label.orEmpty())
        slideOutButton.setAutomationName(name)
        slideOutButton.setToolTipValue(name)
    }

    /** Whether it is a floating panel. */
    val isFloating: Boolean get() = floating.isFloating

    /** The position of the floating panel (in window coordinates). */
    val floatingX: Double get() = floating.x

    /** The position of the floating panel (in window coordinates). */
    val floatingY: Double get() = floating.y

    /** Whether the floating panel is shown on screen. */
    val isFloatingPanelOpen: Boolean get() = floating.isOpen

    /** Floats the panel ([x], [y] are window coordinates; null means just below its position in the ribbon). */
    fun float(x: Double? = null, y: Double? = null) = floating.float(x, y)

    /** Returns the floating panel to its place in the ribbon. */
    fun returnToRibbon() = floating.returnToRibbon()

    /** Shows the floating panel (it stays closed while the ribbon is hidden). */
    fun showFloat() = floating.show()

    /** Closes the floating panel temporarily (when the ribbon is hidden). */
    fun suspendFloat() = floating.suspend()

    /** Arranges the items in the large state to show them in a popup or a floating panel. */
    internal fun placeForPopup(): Double {
        applyItemLayouts(RibbonGroupState.LARGE, false, currentMetrics)
        return placeClassic(RibbonGroupState.LARGE, currentMetrics)
    }

    /** Puts the items Canvas back into the group. */
    internal fun restoreItemsPanel() {
        Xaml.detach(itemsPanel)
        itemsPresenter.setChild(itemsPanel)
        invalidateItemsLayout()
    }

    init {
        floating.attachCaptionDrag(captionRow)
    }

    // ---------------------------------------------------------------- KeyTip

    override val keyTipLabel: String? get() = label
    override val explicitKeyTip: String? get() = model.keyTip
    override val keyTipAnchor: XamlElement get() = collapsedButton

    override fun onKeyTip(): RibbonKeyTipResult {
        openPopup()
        return RibbonKeyTipResult.Scope({ closePopup() }) { popupKeyTipTargets() }
    }

    private fun popupKeyTipTargets(): List<RibbonKeyTipTarget> =
        views.filter { it.isShown }.flatMap { it.keyTipTargets() } + launcherTargets() + slideOutViews.filter { it.isShown }.flatMap { it.keyTipTargets() }

    /** The targets that show KeyTips in the current display. */
    fun keyTipTargets(): List<RibbonKeyTipTarget> {
        if (!isShown) return emptyList()
        if (state == RibbonGroupState.COLLAPSED && !isSimplified && !popupOpen) return listOf(this)
        val items = views.filterIndexed { index, view -> view.isShown && (!isSimplified || inLineMask?.getOrNull(index) != false) }
            .flatMap { it.keyTipTargets() }
        val slideOut = if (hasSlideOut && slideOutButton.isVisible) listOf(slideOutTarget()) else emptyList()
        return items + (if (isSimplified) emptyList() else launcherTargets()) + slideOut
    }

    private fun launcherTargets(): List<RibbonKeyTipTarget> {
        if (!model.hasDialogLauncher) return emptyList()
        val group = this
        return listOf(
            object : RibbonKeyTipTarget {
                override val keyTipLabel: String? get() = group.label
                override val explicitKeyTip: String? get() = null
                override val keyTipAnchor: XamlElement get() = launcher

                override fun onKeyTip(): RibbonKeyTipResult {
                    openDialogLauncher()
                    return RibbonKeyTipResult.Close
                }
            },
        )
    }

    private fun slideOutTarget(): RibbonKeyTipTarget {
        val group = this
        return object : RibbonKeyTipTarget {
            override val keyTipLabel: String? get() = RibbonStrings.current.expandPanel(group.label.orEmpty())
            override val explicitKeyTip: String? get() = null
            override val keyTipAnchor: XamlElement get() = slideOutButton

            override fun onKeyTip(): RibbonKeyTipResult {
                openSlideOut()
                return RibbonKeyTipResult.Scope({ if (!isSlideOutPinned) closeSlideOut() }) { slideOutViews.filter { it.isShown }.flatMap { it.keyTipTargets() } }
            }
        }
    }

    private companion object {
        const val ITEMS_MARGIN = 4.0
        const val COLLAPSED_MARGIN = 3.0
        const val SEPARATOR_WIDTH = 1.0
        const val SEPARATOR_MARGIN = 8.0
        const val SIMPLIFIED_SEPARATOR_MARGIN = 4.0
        const val SIMPLIFIED_SEPARATOR_VERTICAL = 9.0
        const val SIMPLIFIED_SPACING = 1.0
        const val COLUMN_SPACING = 2.0
        const val FULL_HEIGHT_RATIO = 1.6
        const val CAPTION_TEXT_MARGIN = 16.0
        const val LAUNCHER_WIDTH = 16.0
        const val CAPTION_SLACK = 2.0

        /** The icon of the collapsed button for a group without an icon. */
        val DEFAULT_GROUP_ICON = RibbonIcon.glyph("")
    }
}
