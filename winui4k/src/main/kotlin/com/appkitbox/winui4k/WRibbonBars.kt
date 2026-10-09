package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonCommandCatalog
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonMenuBarItemModel
import com.appkitbox.winui4k.ribbon.RibbonMenuBarModel
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonNodeModel
import com.appkitbox.winui4k.ribbon.RibbonPropertyChangeListener
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonToolBarModel
import com.appkitbox.winui4k.ribbon.RibbonToolBarOrientation
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Hosts for items outside the ribbon (toolbars, status bars). Text widths are measured with the component's own hidden
 * Canvas, commands are resolved with its own catalog (or the ribbon's catalog if none), and invocations are reported to
 * listeners and the ribbon.
 */
internal class RibbonStandaloneHost(private val bar: WRibbonBar) : RibbonItemHost {
    private val measurer = RibbonTextMeasurer(bar.measureHost)
    override val metrics: RibbonMetrics get() = bar.barMetrics()
    override val textWidths: RibbonTextWidths get() = measurer
    override val commandCatalog: RibbonCommandCatalog? get() = bar.barCatalog() ?: bar.ribbon?.model?.commandCatalog
    override val ribbon: WRibbon? get() = bar.ribbon
    override val dropDownPlacement: FlyoutPlacement get() = bar.dropDownPlacement

    override fun itemInvoked(model: RibbonNodeModel, commandId: String?, parameter: Any?) {
        bar.fireItemInvoked(model, commandId, parameter)
        bar.ribbon?.onItemInvoked(model, commandId, parameter)
    }

    override fun invalidateItemsLayout() = bar.onBarLayout()

    override fun radioScope(): List<RibbonItemModel> = RibbonModel.flatten(bar.barScope()).toList()
}

/** The common part of bars outside the ribbon (a hidden Canvas for measuring and item invocation listeners). */
abstract class WRibbonBar internal constructor(xaml: String) : WComponent(RibbonThemeResources.load(xaml)) {
    internal val root: XamlElement = XamlElement(inspectable.also { it.addRef() })
    internal val measureHost: XamlElement = XamlElement.load(
        "<Canvas Width=\"0\" Height=\"0\" Opacity=\"0\" IsHitTestVisible=\"False\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />",
    )
    private val itemInvokedListeners = CopyOnWriteArrayList<RibbonItemInvokedListener>()

    /** The ribbon that shares command state and invocation notifications (optional). */
    var ribbon: WRibbon? = null

    /** Subscribes to item invocations ([RibbonItemInvokedEvent.getSource] is this bar; called even when not connected to a ribbon). */
    fun addItemInvokedListener(listener: RibbonItemInvokedListener) {
        itemInvokedListeners += listener
    }

    /** Unsubscribes a listener added with [addItemInvokedListener]. */
    fun removeItemInvokedListener(listener: RibbonItemInvokedListener) {
        itemInvokedListeners -= listener
    }

    /** The catalog that resolves item commands (the ribbon's if null). */
    internal open fun barCatalog(): RibbonCommandCatalog? = null

    /** The direction in which item drop-downs open. */
    internal open val dropDownPlacement: FlyoutPlacement get() = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT

    /** The item metrics. */
    internal open fun barMetrics(): RibbonMetrics = RibbonMetrics.COMFORTABLE

    /** The size of an item changed. */
    internal open fun onBarLayout() {
        // Does nothing by default
    }

    /** The scope of toggles that behave as radio buttons. */
    internal open fun barScope(): List<RibbonItemModel> = emptyList()

    internal fun fireItemInvoked(model: RibbonNodeModel, commandId: String?, parameter: Any?) {
        val event = RibbonItemInvokedEvent(this, model, commandId, parameter, ribbon)
        itemInvokedListeners.forEach { it.itemInvoked(event) }
    }
}

/**
 * A standalone command bar made of ribbon items (RibbonSpace's RibbonToolBar): a horizontal command bar, a tool options
 * bar, a vertical tool palette (one or two columns), or an activity rail. Items that do not fit move to the […]
 * overflow menu.
 */
class WRibbonToolBar @JvmOverloads constructor(
    /** The toolbar's model. */
    val model: RibbonToolBarModel = RibbonToolBarModel(),
) : WRibbonBar("<Grid Background=\"{ThemeResource RibbonToolBarBackgroundBrush}\" />") {
    private val canvas = XamlElement.load("<Canvas HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />")
    private val overflowButton = XamlElement.load(
        "<Button Style=\"{StaticResource RibbonItemButtonStyle}\" Visibility=\"Collapsed\"><FontIcon Glyph=\"&#xE712;\" FontSize=\"14\" /></Button>",
    )
    private val host = RibbonStandaloneHost(this)
    private val views = mutableListOf<RibbonItemView>()
    private var overflowed: List<RibbonItemView> = emptyList()
    private var pending = false
    private val itemsListener = RibbonListListener<RibbonItemModel> { sync() }
    private val modelListener = RibbonPropertyChangeListener { invalidateLayout() }

    init {
        root.addChild(measureHost)
        root.addChild(canvas)
        canvas.addChild(overflowButton)
        val name = RibbonStrings.current.moreOptions
        overflowButton.setAutomationName(name)
        overflowButton.setToolTipValue(name)
        overflowButton.onClick { showOverflow() }
        model.items.addListListener(itemsListener)
        model.addPropertyChangeListener(modelListener)
        addSizeChangedListener { invalidateLayout() }
        addLoadedListener { invalidateLayout() }
        root.onKeyDown { e -> onKeyDown(e) }
        sync()
    }

    override fun barCatalog(): RibbonCommandCatalog? = model.commandCatalog

    override fun barMetrics(): RibbonMetrics = RibbonMetrics.forDensity(model.density)

    override fun onBarLayout() = invalidateLayout()

    override fun barScope(): List<RibbonItemModel> = model.items

    private fun sync() {
        val existing = views.associateBy { it.model }
        views.filter { it.model !in model.items }.forEach { it.dispose() }
        views.clear()
        canvas.clearChildren()
        for (item in model.items) {
            val view = existing[item] ?: RibbonItemViews.create(item, host)?.also { it.attach() } ?: continue
            views += view
            canvas.addChild(view.element)
        }
        canvas.addChild(overflowButton)
        ribbon?.invalidateShortcuts()
        invalidateLayout()
    }

    /** The item views (in display order). */
    internal fun itemViews(): List<RibbonItemView> = views.toList()

    /** The items moved to the overflow menu. */
    val overflowItems: List<RibbonItemModel> get() = overflowed.map { it.model }

    private val isVertical: Boolean get() = model.orientation == RibbonToolBarOrientation.VERTICAL

    private fun itemLayout(): RibbonItemLayout {
        val metrics = RibbonMetrics.forDensity(model.density)
        return RibbonItemLayout(if (model.showLabels) RibbonItemSize.MEDIUM else RibbonItemSize.SMALL, metrics, true, if (model.showLabels) null else false)
    }

    /** Redoes the layout (batched into the next dispatch). */
    fun invalidateLayout() {
        if (pending) return
        pending = true
        WinUiUtilities.invokeLater {
            pending = false
            arrange()
        }
    }

    private fun arrange() {
        val layout = itemLayout()
        val vertical = isVertical
        for (view in views) {
            if (view is RibbonSeparatorView) view.isHorizontal = vertical
            view.applyLayout(layout)
        }
        val shown = views.filter { it.isShown }
        val sizes = shown.map { it.measure(layout) }
        // Separators do not occupy a cell, so the cell size is determined by the items other than separators
        val flow = Flow(vertical, if (vertical) model.columns else 1, cellSize(shown, sizes))
        val available = if (vertical) actualHeight else actualWidth
        val overflowSize = layout.metrics.simplifiedItemHeight
        val hidden = mutableListOf<RibbonItemView>()
        for ((index, view) in shown.withIndex()) {
            val extent = if (vertical) sizes[index].height else sizes[index].width
            val reserve = if (index < shown.size - 1 && model.isOverflowEnabled) overflowSize else 0.0
            val fits = available <= 0 || !model.isOverflowEnabled || flow.main + extent + reserve <= available
            view.element.isVisible = fits && hidden.isEmpty()
            if (view.element.isVisible) flow.place(view, sizes[index]) else hidden += view
        }
        flow.finishRow()
        finishArrange(flow, hidden, overflowSize)
    }

    private fun cellSize(shown: List<RibbonItemView>, sizes: List<WSize>): WSize {
        val cells = sizes.filterIndexed { i, _ -> shown[i] !is RibbonSeparatorView }
        return WSize(cells.maxOfOrNull { it.width } ?: 0.0, cells.maxOfOrNull { it.height } ?: 0.0)
    }

    /** Places the overflow button at the end and determines the toolbar's size. */
    private fun finishArrange(flow: Flow, hidden: List<RibbonItemView>, overflowSize: Double) {
        val vertical = isVertical
        overflowed = hidden
        overflowButton.isVisible = hidden.isNotEmpty()
        if (hidden.isNotEmpty()) {
            overflowButton.setSize(overflowSize, overflowSize)
            if (vertical) overflowButton.setCanvasPosition(0.0, flow.main) else overflowButton.setCanvasPosition(flow.main, 0.0)
            flow.main += overflowSize
        }
        if (vertical) {
            canvas.setSize(flow.cross, flow.main)
            root.setMinWidth(flow.cross)
        } else {
            canvas.setSize(flow.main, flow.cross)
            root.setMinHeight(flow.cross)
        }
    }

    /** Where items are flowed (one row horizontally, a grid of [columns] columns vertically; vertical separators span the whole row). */
    private class Flow(private val vertical: Boolean, private val columns: Int, private val cell: WSize) {
        var main = 0.0
        var cross = 0.0
        private var column = 0

        fun place(view: RibbonItemView, size: WSize) {
            when {
                vertical && view is RibbonSeparatorView -> {
                    finishRow()
                    view.element.setSize(columns * cell.width - SEPARATOR_INSET * 2, 1.0)
                    view.element.setCanvasPosition(SEPARATOR_INSET, main + SEPARATOR_GAP)
                    main += SEPARATOR_GAP * 2 + 1
                    cross = max(cross, columns * cell.width)
                }
                vertical -> {
                    view.element.setCanvasPosition(column * cell.width + (cell.width - size.width) / 2, main)
                    cross = max(cross, (column + 1) * cell.width)
                    column++
                    if (column >= columns) finishRow()
                }
                else -> {
                    view.element.setCanvasPosition(main, (cell.height - size.height) / 2)
                    main += size.width + SPACING
                    cross = max(cross, cell.height)
                }
            }
        }

        /** Closes the current row partway when vertical. */
        fun finishRow() {
            if (column == 0) return
            column = 0
            main += cell.height + SPACING
        }
    }

    /** Opens the overflow menu. */
    fun showOverflow() {
        if (overflowed.isEmpty()) return
        val menu = WMenuFlyout()
        RibbonMenus.applyMenuStyle(menu)
        for (view in overflowed) view.overflowMenuItems().forEach { menu.add(it) }
        menu.placement = if (isVertical) FlyoutPlacement.RIGHT_EDGE_ALIGNED_TOP else FlyoutPlacement.BOTTOM_EDGE_ALIGNED_RIGHT
        menu.showAt(overflowButton)
    }

    /** Moves between toolbar items with the arrow keys (except inside items that use them, such as combo boxes). */
    private fun onKeyDown(e: XamlKeyEvent) {
        val forward = if (isVertical) RibbonInputViews.VK_DOWN else RibbonInputViews.VK_RIGHT
        val back = if (isVertical) RibbonInputViews.VK_UP else RibbonInputViews.VK_LEFT
        if (e.key != forward && e.key != back) return
        val focused = views.indexOfFirst { it.element.hasFocus && it !is RibbonInputView && it !is RibbonSegmentedView }
        if (focused < 0) return
        val targets = views.filter { it.element.isVisible && it.isEffectivelyEnabled && it !is RibbonSeparatorView }
        val index = targets.indexOf(views[focused])
        targets.getOrNull((index + if (e.key == forward) 1 else -1).coerceIn(0, targets.size - 1))?.element?.focus()
        e.handled = true
    }

    private companion object {
        const val SPACING = 1.0
        const val SEPARATOR_INSET = 4.0
        const val SEPARATOR_GAP = 4.0
    }
}

/** A status bar (RibbonSpace's RibbonStatusBar): items on the left and right (page count, word count, view switching, zoom). */
class WRibbonStatusBar @JvmOverloads constructor(
    /** The status bar's model. */
    val model: RibbonStatusBarModel = RibbonStatusBarModel(),
) : WRibbonBar(
    "<Grid Background=\"{ThemeResource RibbonStatusBarBackgroundBrush}\" MinHeight=\"26\" Padding=\"8,0,8,0\">" +
        "<Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions></Grid>",
) {
    private val host = RibbonStandaloneHost(this)
    private val start = RibbonItemStrip(model.items, host, { layoutOf(it) }, spacing = SPACING)
    private val end = RibbonItemStrip(model.endItems, host, { layoutOf(it) }, spacing = SPACING)
    private val modelListener = RibbonPropertyChangeListener { applyLayouts() }

    init {
        root.addChild(measureHost)
        root.addChild(start.element)
        root.addChild(end.element)
        start.element.horizontalAlignment = HorizontalAlignment.LEFT
        end.element.setGridCell(0, 1)
        model.addPropertyChangeListener(modelListener)
        addLoadedListener { applyLayouts() }
        start.attach()
        end.attach()
    }

    override fun barCatalog(): RibbonCommandCatalog? = model.commandCatalog

    override fun barMetrics(): RibbonMetrics = STATUS_METRICS

    // It is at the bottom edge of the window, so drop-downs open upward (opening downward would go off screen)
    override val dropDownPlacement: FlyoutPlacement get() = FlyoutPlacement.TOP_EDGE_ALIGNED_LEFT

    override fun onBarLayout() = applyLayouts()

    override fun barScope(): List<RibbonItemModel> = model.items + model.endItems

    private fun layoutOf(view: RibbonItemView): RibbonItemLayout =
        RibbonItemLayout(RibbonItemSize.SMALL, STATUS_METRICS, true, model.showLabels && view.model.showLabel)

    private fun applyLayouts() {
        start.applyLayouts()
        end.applyLayouts()
    }

    /** The item views (left to right). */
    internal fun itemViews(): List<RibbonItemView> = start.views() + end.views()

    private companion object {
        const val SPACING = 2.0

        /** The status bar metrics (compact, with an item height of 22). */
        val STATUS_METRICS: RibbonMetrics = RibbonMetrics.COMPACT.copy(simplifiedItemHeight = 22.0)
    }
}

/**
 * The Office status bar zoom control ([-] slider [+] 100%; RibbonSpace's RibbonZoomControl).
 * Runs with the value as the argument only on user interaction.
 */
internal class RibbonZoomView(override val model: RibbonZoomModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load(
        "<StackPanel Orientation=\"Horizontal\" Spacing=\"2\" VerticalAlignment=\"Center\">" +
            "<Button x:Name=\"PART_ZoomOut\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Width=\"22\" Height=\"22\" Padding=\"0\">" +
            "<FontIcon Glyph=\"&#xE738;\" FontSize=\"10\" /></Button>" +
            "<Slider x:Name=\"PART_Slider\" Width=\"110\" VerticalAlignment=\"Center\" MinHeight=\"0\" />" +
            "<Button x:Name=\"PART_ZoomIn\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Width=\"22\" Height=\"22\" Padding=\"0\">" +
            "<FontIcon Glyph=\"&#xE710;\" FontSize=\"10\" /></Button>" +
            "<Button x:Name=\"PART_Percent\" Style=\"{StaticResource RibbonChromeButtonStyle}\" MinWidth=\"44\" Height=\"22\" Padding=\"4,0\" /></StackPanel>",
    )
    private val zoomOut = element.part("PART_ZoomOut")
    private val zoomIn = element.part("PART_ZoomIn")
    private val slider = element.part("PART_Slider")
    private val percent = element.part("PART_Percent")
    private var syncing = false

    override fun attach() {
        val strings = RibbonStrings.current
        zoomOut.setAutomationName(strings.zoomOut)
        zoomOut.setToolTipValue(strings.zoomOut)
        zoomIn.setAutomationName(strings.zoomIn)
        zoomIn.setToolTipValue(strings.zoomIn)
        slider.setAutomationName(strings.zoom)
        percent.setAutomationName(strings.zoom)
        zoomOut.onClick { zoomOut() }
        zoomIn.onClick { zoomIn() }
        percent.onClick { model.fireZoomDialogRequested() }
        slider.onValueChanged { value -> if (!syncing) setUserValue(value) }
        super.attach()
        sync()
    }

    /** The [-] button: steps down to a multiple of the step (run as a user interaction). */
    fun zoomOut() = setUserValue(ceil((model.value - model.step) / model.step) * model.step)

    /** The [+] button: steps up to a multiple of the step (run as a user interaction). */
    fun zoomIn() = setUserValue(floor((model.value + model.step) / model.step) * model.step)

    private fun setUserValue(value: Double) {
        val clamped = model.clamp(value)
        if (clamped == model.value) return
        model.value = clamped
        execute(clamped)
    }

    private fun sync() {
        syncing = true
        try {
            slider.setRange(model.minimum, max(model.minimum, model.maximum))
            if (slider.rangeValue != model.value) slider.rangeValue = model.value
        } finally {
            syncing = false
        }
        percent.setContentText(String.format(java.util.Locale.ROOT, "%.0f%%", model.value))
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "value" -> {
                val clamped = model.clamp(model.value)
                if (clamped != model.value) model.value = clamped else sync()
            }
            "minimum", "maximum" -> {
                model.value = model.clamp(model.value)
                sync()
            }
        }
    }

    override fun measure(layout: RibbonItemLayout): WSize = WSize(WIDTH, layout.metrics.simplifiedItemHeight)

    override fun applyLayoutCore() {
        // The size is fixed (buttons, slider, percentage)
    }

    override fun refreshToolTip() {
        // Each part has its own tooltip
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = emptyList()

    override fun invoke(): Boolean {
        slider.focus()
        return true
    }

    private companion object {
        const val WIDTH = 220.0
    }
}

/**
 * A toolbar that switches its content depending on the context (RibbonSpace's RibbonContextualToolBar): such as the
 * tool options bar of Photoshop / Illustrator, or an editor toolbar that follows the selection.
 */
class WRibbonContextualToolBar : WComponent(Xaml.load("<Grid />")) {
    private val root = XamlElement(inspectable.also { it.addRef() })
    private val contents = LinkedHashMap<String, WComponent>()
    private val listeners = CopyOnWriteArrayList<(String?) -> Unit>()

    /** The key of the current context (tool, selection kind, view). */
    var activeContext: String? = null
        set(value) {
            if (field == value) return
            field = value
            for ((key, content) in contents) content.isVisible = key == value
            listeners.forEach { it(value) }
        }

    /** Registers the content shown for [context]. */
    fun setContent(context: String, content: WComponent) {
        contents.remove(context)?.let { root.removeChild(it) }
        contents[context] = content
        Xaml.detach(content)
        root.addChild(content)
        content.isVisible = context == activeContext
    }

    /** Removes the content for [context]. */
    fun removeContent(context: String) {
        contents.remove(context)?.let { root.removeChild(it) }
    }

    /** The content for [context]. */
    fun getContent(context: String): WComponent? = contents[context]

    /** Subscribes to context changes (the argument is the new key). */
    fun addContextChangeListener(listener: (String?) -> Unit) {
        listeners += listener
    }
}

/**
 * A classic menu bar ([File] [Edit] [View] ...; RibbonSpace's RibbonMenuBar). While a menu is open, moving the pointer to
 * an adjacent header switches to that menu, and the Left / Right keys move between headers. Menus are built from the
 * model each time they open.
 */
class WRibbonMenuBar @JvmOverloads constructor(
    /** The menu bar's model. */
    val model: RibbonMenuBarModel = RibbonMenuBarModel(),
) : WRibbonBar("<StackPanel Orientation=\"Horizontal\" Spacing=\"0\" Background=\"Transparent\" />") {
    private val headers = mutableListOf<Pair<RibbonMenuBarItemModel, XamlElement>>()
    private var openMenu: Pair<RibbonMenuBarItemModel, WMenuFlyout>? = null
    private val host = RibbonStandaloneHost(this)
    private val listListener = RibbonListListener<RibbonMenuBarItemModel> { build() }

    init {
        model.items.addListListener(listListener)
        build()
    }

    /** Whether a menu is open. */
    val isMenuOpen: Boolean get() = openMenu != null

    private fun build() {
        root.clearChildren()
        root.addChild(measureHost)
        headers.clear()
        for (item in model.items) {
            val header = XamlElement.load(
                "<Button Style=\"{StaticResource RibbonChromeButtonStyle}\" Padding=\"10,4,10,4\" Height=\"28\" />",
            )
            header.setContentText(item.label.orEmpty())
            header.setAutomationName(item.label)
            header.onClick { open(item) }
            header.onPointer(XamlInterop.IUIElement_add_PointerEntered) {
                val current = openMenu
                if (current != null && current.first !== item) open(item)
            }
            header.onKeyDown { e -> onHeaderKey(item, e) }
            root.addChild(header)
            headers += item to header
        }
    }

    private fun onHeaderKey(item: RibbonMenuBarItemModel, e: XamlKeyEvent) {
        val index = headers.indexOfFirst { it.first === item }
        val target = when (e.key) {
            RibbonInputViews.VK_LEFT -> index - 1
            RibbonInputViews.VK_RIGHT -> index + 1
            RibbonInputViews.VK_DOWN -> {
                open(item)
                e.handled = true
                return
            }
            else -> return
        }
        headers.getOrNull((target + headers.size) % headers.size)?.second?.focus()
        e.handled = true
    }

    /** Opens the menu whose header is [header]. Returns false if it is not found. */
    fun openMenu(header: String): Boolean {
        val item = model.items.firstOrNull { it.label == header || it.id == header } ?: return false
        open(item)
        return true
    }

    /** Puts the focus on the first header (F10 / Alt). */
    fun focusFirst() {
        headers.firstOrNull()?.second?.focus()
    }

    private fun open(item: RibbonMenuBarItemModel) {
        openMenu?.second?.hide()
        val anchor = headers.firstOrNull { it.first === item }?.second ?: return
        item.fireOpening()
        val menu = WMenuFlyout()
        RibbonMenus.applyMenuStyle(menu)
        menu.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
        // Keep receiving pointer enter/exit on the headers while open, and switch menus when the pointer moves to an
        // adjacent header
        menu.overlayInputPassThroughElement = this
        RibbonMenus.createMenuItems(item.items.toList(), host).forEach { menu.add(it) }
        RibbonMenus.onClosed(menu) { if (openMenu?.second === menu) openMenu = null }
        openMenu = item to menu
        menu.showAt(anchor)
    }
}
