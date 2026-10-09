package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonAdaptiveLayout
import com.appkitbox.winui4k.ribbon.RibbonGroupLayoutInfo
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonGroupState
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonPanelPresentation
import com.appkitbox.winui4k.ribbon.RibbonReductionStrategy
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedGroup
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedItem
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedLayout
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import kotlin.math.max

/** The contract of what hosts tabs (the ribbon). */
internal interface RibbonTabContainer : RibbonGroupContainer {
    /** The groups shown in a tab (in an order reflecting customized reordering, hiding, and custom groups). */
    fun effectiveGroups(tab: RibbonTabModel): List<RibbonGroupModel>

    /** How groups are reduced. */
    val reductionStrategy: RibbonReductionStrategy

    /** Whether groups are reduced to fit the width. */
    val isAdaptiveLayoutEnabled: Boolean
}

/**
 * The view of a tab's content (RibbonTab / RibbonGroupsPanel in RibbonSpace).
 *
 * Lays out groups horizontally; in the classic ribbon, groups are reduced to fit the width (adaptive layout), and in the
 * simplified ribbon, items that do not fit are moved to the "More Options" menu. If it still overflows, it scrolls with the
 * left and right arrows. The width of each group state is cached per group version.
 */
internal class RibbonTabView(val model: RibbonTabModel, private val container: RibbonTabContainer) {
    private val scrollHost = RibbonScrollHost()

    /** The element placed in the ribbon's tab content area. */
    val element: XamlElement get() = scrollHost.element

    private val canvas = XamlElement.load("<Canvas HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />")
    private val overflowButton = XamlElement.load(
        "<Button Style=\"{StaticResource RibbonItemButtonStyle}\" Visibility=\"Collapsed\"><FontIcon Glyph=\"&#xE712;\" FontSize=\"16\" /></Button>",
    )
    private val overflowMenu = WMenuFlyout()
    private val groups = mutableListOf<RibbonGroupView>()
    private val widthCache = HashMap<RibbonGroupView, Pair<String, List<Double>>>()

    /** The height at the last layout. */
    var contentHeight: Double = 0.0
        private set

    init {
        scrollHost.setContent(canvas)
        val strings = RibbonStrings.current
        overflowButton.setAutomationName(strings.moreOptions)
        overflowButton.setToolTipValue(strings.moreOptions)
        overflowButton.onClick { showOverflow() }
        overflowMenu.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_RIGHT
        RibbonMenus.applyMenuStyle(overflowMenu)
        RibbonMenus.onOpening(overflowMenu) { buildOverflowMenu() }
        RibbonMenus.onOpened(overflowMenu) { overflowButton.goToState("Active") }
        RibbonMenus.onClosed(overflowMenu) { overflowButton.goToState("Inactive") }
    }

    /** Syncs the group views with the model (and the customization). */
    fun syncGroups() {
        val wanted = container.effectiveGroups(model)
        val existing = groups.associateBy { it.model }
        groups.filter { it.model !in wanted }.forEach {
            it.dispose()
            widthCache.remove(it)
        }
        groups.clear()
        canvas.clearChildren()
        for (group in wanted) {
            val view = existing[group] ?: RibbonGroupView(group, container).also { it.attach() }
            groups += view
            canvas.addChild(view.element)
        }
        canvas.addChild(overflowButton)
    }

    /** The group views (in display order). */
    fun groupViews(): List<RibbonGroupView> = groups.toList()

    /** Discards the views. */
    fun dispose() {
        groups.forEach { it.dispose() }
        groups.clear()
        widthCache.clear()
    }

    /** The width of each group state (the cache is used if the version and dimensions are the same). */
    private fun widthsOf(group: RibbonGroupView, metrics: RibbonMetrics): List<Double> {
        val key = "${group.layoutVersion}|${System.identityHashCode(metrics)}|${container.showsGroupCaptions}|${container.panelPresentation}"
        widthCache[group]?.let { (cachedKey, widths) -> if (cachedKey == key) return widths }
        val widths = RibbonGroupState.entries.map { group.measureWidth(it, metrics) }
        widthCache[group] = key to widths
        return widths
    }

    /** Lays out the groups to fit [availableWidth]. Returns the content height. */
    fun layout(availableWidth: Double, metrics: RibbonMetrics, simplified: Boolean): Double {
        val shown = groups.filter { it.isShown }
        groups.filter { !it.isShown }.forEach { it.element.isVisible = false }
        shown.forEach { it.element.isVisible = true }
        val (extent, height) = if (simplified) layoutSimplified(shown, availableWidth, metrics) else layoutClassic(shown, availableWidth, metrics)
        contentHeight = height
        canvas.setSize(extent, height)
        scrollHost.update(extent, availableWidth, height)
        return height
    }

    private fun layoutClassic(shown: List<RibbonGroupView>, availableWidth: Double, metrics: RibbonMetrics): Pair<Double, Double> {
        overflowButton.isVisible = false
        val presentation = container.panelPresentation
        val states = when {
            presentation != RibbonPanelPresentation.FULL -> shown.map { RibbonGroupState.COLLAPSED }
            !container.isAdaptiveLayoutEnabled -> shown.map { RibbonGroupState.LARGE }
            else -> {
                val infos = shown.map { RibbonGroupLayoutInfo(widthsOf(it, metrics), it.model.reductionOrder, it.model.canCollapse) }
                RibbonAdaptiveLayout.compute(infos, availableWidth, 0.0, container.reductionStrategy).states
            }
        }
        var x = 0.0
        var height = 0.0
        for ((index, group) in shown.withIndex()) {
            val size = group.applyState(states[index], false, metrics)
            group.element.setCanvasPosition(x, 0.0)
            x += size.width
            height = max(height, size.height)
        }
        return x to height
    }

    private fun layoutSimplified(shown: List<RibbonGroupView>, availableWidth: Double, metrics: RibbonMetrics): Pair<Double, Double> {
        val overflowWidth = OVERFLOW_BUTTON_WIDTH
        val simplifiedGroups = shown.map { group ->
            val widths = group.measureSimplifiedItemWidths(metrics)
            val visibilities = group.simplifiedVisibilities()
            RibbonSimplifiedGroup(widths.indices.map { RibbonSimplifiedItem(widths[it], visibilities[it]) }, group.model.reductionOrder)
        }
        val result = RibbonSimplifiedLayout.compute(simplifiedGroups, availableWidth, overflowWidth)
        var x = 0.0
        for ((index, group) in shown.withIndex()) {
            val size = group.applyState(RibbonGroupState.LARGE, true, metrics, result.inLine[index])
            group.element.setCanvasPosition(x, 0.0)
            x += size.width
        }
        val hasOverflow = result.hasOverflow || shown.any { it.overflowViews().isNotEmpty() }
        overflowButton.isVisible = hasOverflow
        if (hasOverflow) {
            overflowButton.setSize(overflowWidth, metrics.simplifiedItemHeight)
            overflowButton.setCanvasPosition(x + 2, (metrics.simplifiedHeight - metrics.simplifiedItemHeight) / 2)
            x += overflowWidth + 4
        }
        return x to metrics.simplifiedHeight
    }

    /** Opens the simplified ribbon's overflow menu. */
    fun showOverflow() {
        if (overflowButton.isVisible) overflowMenu.showAt(overflowButton)
    }

    /** Builds the overflow menu (a header and items per group). */
    private fun buildOverflowMenu() {
        overflowMenu.removeAll()
        var first = true
        for (group in groups.filter { it.isShown }) {
            val items = group.overflowViews().flatMap { it.overflowMenuItems() }
            if (items.isEmpty()) continue
            if (!first) overflowMenu.add(WMenuFlyoutSeparator())
            first = false
            overflowMenu.add(
                WMenuFlyoutItem(group.model.label.orEmpty()).also { it.isEnabled = false },
            )
            items.forEach { overflowMenu.add(it) }
        }
    }

    /** Scrolls so that the group is visible. */
    fun bringIntoView(group: RibbonGroupView) {
        val position = group.element.actualOffset()
        scrollHost.bringIntoView(position[0], group.element.actualWidth)
    }

    /** The KeyTip targets (the groups' items and the overflow button). */
    fun keyTipTargets(simplified: Boolean): List<RibbonKeyTipTarget> {
        val targets = groups.filter { it.isShown }.flatMap { it.keyTipTargets() }
        if (!simplified || !overflowButton.isVisible) return targets
        val tab = this
        return targets + object : RibbonKeyTipTarget {
            override val keyTipLabel: String? get() = RibbonStrings.current.moreOptions
            override val explicitKeyTip: String? get() = "00"
            override val keyTipAnchor: XamlElement get() = overflowButton

            override fun onKeyTip(): RibbonKeyTipResult {
                tab.showOverflow()
                return RibbonKeyTipResult.Close
            }
        }
    }

    /** Discards the measurement cache. */
    fun invalidateWidths() = widthCache.clear()

    private companion object {
        /** The width of the "More Options" button. */
        const val OVERFLOW_BUTTON_WIDTH = 32.0
    }
}
