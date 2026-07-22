package com.appkitbox.winui4k.extension.miglayout

import com.appkitbox.winui4k.WButtonBase
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContainer
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLayoutManager
import com.appkitbox.winui4k.WLayoutPanel
import com.appkitbox.winui4k.WPasswordField
import com.appkitbox.winui4k.WProgressBar
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WSize
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WTextField
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.ComponentWrapper
import net.miginfocom.layout.ConstraintParser
import net.miginfocom.layout.ContainerWrapper
import net.miginfocom.layout.Grid
import net.miginfocom.layout.LC
import net.miginfocom.layout.LayoutUtil
import net.miginfocom.layout.PlatformDefaults
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * A [WLayoutManager] that adapts MigLayout (miglayout-core) for use with [WLayoutPanel].
 * Following the upstream porting guide, the [ComponentWrapper]/[ContainerWrapper] adapters and
 * the layout coordination logic (the counterpart of the Swing port's net.miginfocom.swing.MigLayout)
 * are all gathered in this one class. The actual grid computation is entirely handled by
 * miglayout-core's [Grid].
 *
 * The coordinate system treats 1px as 1epx (WinUI's effective pixel, synonymous with DIP). Since
 * every XAML property (Width, Canvas.Left, DesiredSize, etc.) is in epx, the adapter's input and
 * output are epx end to end, and the conversion to physical pixels is done by WinUI's compositor
 * at render time (no conversion is needed on the Kotlin side). Unitless numbers (whose default
 * unit is lp) are also interpreted as equal to px — i.e. epx — via
 * [ComponentWrapper.getPixelUnitFactor] = 1. Since WinUI has no way to measure a minimum size, the
 * minimum size is treated as equal to the preferred size (the same policy as the SWT port). It can
 * still be made shrinkable by stating so explicitly in a component constraint, e.g. "width 0::".
 *
 * Constraints use the same string format as upstream:
 * ```
 * val panel = WLayoutPanel(MigLayoutManager("wrap 2", "[right][grow, fill]"))
 * panel.add(WLabel("Name:"))
 * panel.add(WTextField())
 * ```
 */
class MigLayoutManager(
    layoutConstraints: String = "",
    columnConstraints: String = "",
    rowConstraints: String = "",
) : WLayoutManager {
    private var lc: LC = parseLayout(layoutConstraints)
    private var columnSpecs: AC = ConstraintParser.parseColumnConstraints(ConstraintParser.prepare(columnConstraints))
    private var rowSpecs: AC = ConstraintParser.parseRowConstraints(ConstraintParser.prepare(rowConstraints))

    /** The overall layout constraints (e.g. "wrap 2, insets 0"). Call [WLayoutPanel.revalidate] to apply a change. */
    var layoutConstraints: String = layoutConstraints
        set(value) {
            field = value
            lc = parseLayout(value)
        }

    /** The column constraints (e.g. "[right][grow, fill]"). Call [WLayoutPanel.revalidate] to apply a change. */
    var columnConstraints: String = columnConstraints
        set(value) {
            field = value
            columnSpecs = ConstraintParser.parseColumnConstraints(ConstraintParser.prepare(value))
        }

    /** The row constraints (e.g. "[][grow]"). Call [WLayoutPanel.revalidate] to apply a change. */
    var rowConstraints: String = rowConstraints
        set(value) {
            field = value
            rowSpecs = ConstraintParser.parseRowConstraints(ConstraintParser.prepare(value))
        }

    /** Per-child component constraints, in addition order. */
    private val componentConstraints = LinkedHashMap<WComponent, CC>()

    /** Per-child adapters. Reused to preserve the equals/hashCode identity that Grid requires. */
    private val adapters = HashMap<WComponent, ComponentAdapter>()

    /** The parent panel's adapter (this manager is used exclusively by a single WLayoutPanel). */
    private var containerAdapter: ContainerAdapter? = null

    override fun addLayoutComponent(component: WComponent, constraints: Any?) {
        componentConstraints[component] = when (constraints) {
            null -> CC()
            is String -> ConstraintParser.parseComponentConstraint(ConstraintParser.prepare(constraints))
            is CC -> constraints
            else -> throw IllegalArgumentException(
                "constraints must be a String or net.miginfocom.layout.CC: $constraints",
            )
        }
    }

    override fun removeLayoutComponent(component: WComponent) {
        componentConstraints.remove(component)
        adapters.remove(component)
    }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize {
        val grid = buildGrid(parent)
        val insets = parent.insets
        return WSize(
            LayoutUtil.getSizeSafe(grid.width, LayoutUtil.PREF) + insets.left + insets.right,
            LayoutUtil.getSizeSafe(grid.height, LayoutUtil.PREF) + insets.top + insets.bottom,
        )
    }

    override fun layoutContainer(parent: WLayoutPanel) {
        val insets = parent.insets
        val bounds = intArrayOf(
            insets.left.roundToInt(),
            insets.top.roundToInt(),
            (parent.layoutSize.width - insets.left - insets.right).roundToInt(),
            (parent.layoutSize.height - insets.top - insets.bottom).roundToInt(),
        )
        // Grid.layout returning true means "a linked value etc. changed during layout and it needs
        // to be redone" (the same rebuild-then-relayout as the Swing port's MigLayout.layoutContainer)
        if (buildGrid(parent).layout(bounds, lc.alignX, lc.alignY, false)) {
            buildGrid(parent).layout(bounds, lc.alignX, lc.alignY, false)
        }
    }

    /**
     * Builds a [Grid] from the current children and size. Since Grid caches children's preferred
     * sizes at construction time, it is rebuilt on every layout pass so the latest measurements
     * are always used (a simplification of the Swing port's hash-comparison-based cache
     * invalidation).
     */
    private fun buildGrid(parent: WLayoutPanel): Grid {
        val container = containerAdapterFor(parent)
        val ccMap = HashMap<ComponentWrapper, CC>()
        parent.layoutChildren.forEach { child ->
            ccMap[adapterFor(child, parent)] = componentConstraints[child] ?: CC()
        }
        return Grid(container, lc, rowSpecs, columnSpecs, ccMap, null)
    }

    private fun containerAdapterFor(parent: WLayoutPanel): ContainerAdapter {
        val cached = containerAdapter
        if (cached != null && cached.component === parent) return cached
        return ContainerAdapter(parent).also { containerAdapter = it }
    }

    private fun adapterFor(component: WComponent, parent: WLayoutPanel): ComponentAdapter =
        adapters.getOrPut(component) { ComponentAdapter(component) }.also { it.panel = parent }

    /** A [ComponentWrapper] adapter that exposes a [WComponent] to miglayout-core. */
    private open inner class ComponentAdapter(val component: WComponent) : ComponentWrapper {
        /** The parent panel that setBounds is applied to. Updated on every buildGrid call. */
        var panel: WLayoutPanel? = null

        /** The most recent placement Grid fixed via setBounds (used to report the current values from getX etc.). */
        private var lastX = 0
        private var lastY = 0
        private var lastWidth = 0
        private var lastHeight = 0

        private val componentType: Int by lazy {
            when (component) {
                is WLabel -> ComponentWrapper.TYPE_LABEL
                is WTextField, is WPasswordField -> ComponentWrapper.TYPE_TEXT_FIELD
                is WCheckBox -> ComponentWrapper.TYPE_CHECK_BOX
                is WComboBox -> ComponentWrapper.TYPE_COMBO_BOX
                is WSlider -> ComponentWrapper.TYPE_SLIDER
                is WProgressBar -> ComponentWrapper.TYPE_PROGRESS_BAR
                is WScrollPane -> ComponentWrapper.TYPE_SCROLL_PANE
                is WButtonBase -> ComponentWrapper.TYPE_BUTTON
                is WContainer -> ComponentWrapper.TYPE_CONTAINER
                else -> ComponentWrapper.TYPE_UNKNOWN
            }
        }

        override fun getComponent(): Any = component

        override fun getX(): Int = lastX

        override fun getY(): Int = lastY

        override fun getWidth(): Int = lastWidth

        override fun getHeight(): Int = lastHeight

        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            lastX = x
            lastY = y
            lastWidth = width
            lastHeight = height
            panel?.setBounds(component, x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        }

        override fun isVisible(): Boolean = component.isVisible

        // WinUI's DesiredSize is only the preferred size and there's no way to measure a minimum
        // size, so minimum = preferred (same policy as the SWT port; can be made shrinkable with
        // a constraint like "width 0::")
        override fun getMinimumWidth(hHint: Int): Int = getPreferredWidth(hHint)

        override fun getMinimumHeight(wHint: Int): Int = getPreferredHeight(wHint)

        override fun getPreferredWidth(hHint: Int): Int = ceil(component.preferredSize().width).toInt()

        override fun getPreferredHeight(wHint: Int): Int = ceil(component.preferredSize().height).toInt()

        override fun getMaximumWidth(hHint: Int): Int {
            val maxWidth = component.maxWidth
            return if (maxWidth.isFinite()) ceil(maxWidth).toInt() else LayoutUtil.INF
        }

        override fun getMaximumHeight(wHint: Int): Int = LayoutUtil.INF

        override fun getBaseline(width: Int, height: Int): Int = -1

        override fun hasBaseline(): Boolean = false

        override fun getParent(): ContainerWrapper? = containerAdapter

        // 1px = 1epx (DIP). Unitless values (lp) are equal too. Monitor scaling is handled by
        // WinUI's compositor at render time, so correcting for it here would cause double scaling
        override fun getPixelUnitFactor(isHor: Boolean): Float = 1f

        // epx is fixed at 1/96 inch (at 100% scale)
        override fun getHorizontalScreenDPI(): Int = PlatformDefaults.getDefaultDPI()

        override fun getVerticalScreenDPI(): Int = PlatformDefaults.getDefaultDPI()

        // For the screen-size-relative unit ("sp"). There's no way to obtain it, so a headless
        // default is returned
        override fun getScreenWidth(): Int = 1024

        override fun getScreenHeight(): Int = 768

        // Screen-coordinate links (e.g. "x2 visual.x2") are not supported
        override fun getScreenLocationX(): Int = 0

        override fun getScreenLocationY(): Int = 0

        override fun getLinkId(): String? = null

        override fun getLayoutHashCode(): Int {
            val size = component.preferredSize()
            var hash = size.width.hashCode() * 31 + size.height.hashCode()
            if (component.isVisible) hash += 1324511
            return hash
        }

        override fun getVisualPadding(): IntArray? = null

        override fun paintDebugOutline(showVisualPadding: Boolean) {
            // Debug painting isn't supported (no way to paint directly onto the WinUI side)
        }

        override fun getComponentType(disregardScrollPane: Boolean): Int = componentType

        // The preferred size uses a cached measurement under an infinite constraint, so width and
        // height are independent
        override fun getContentBias(): Int = -1

        override fun hashCode(): Int = component.hashCode()

        override fun equals(other: Any?): Boolean =
            other is ComponentWrapper && component == other.component
    }

    /** A [ContainerWrapper] adapter that exposes the parent [WLayoutPanel] to miglayout-core. */
    private inner class ContainerAdapter(panel: WLayoutPanel) : ComponentAdapter(panel), ContainerWrapper {
        private val layoutPanel = panel

        override fun getComponents(): Array<ComponentWrapper> =
            layoutPanel.layoutChildren.map { adapterFor(it, layoutPanel) as ComponentWrapper }.toTypedArray()

        override fun getComponentCount(): Int = layoutPanel.layoutChildren.size

        override fun getLayout(): Any = this@MigLayoutManager

        override fun isLeftToRight(): Boolean = true

        override fun paintDebugCell(x: Int, y: Int, width: Int, height: Int) {
            // Debug painting isn't supported (no way to paint directly onto the WinUI side)
        }

        override fun getX(): Int = 0

        override fun getY(): Int = 0

        // The current layout area, used by Grid to resolve percentage units etc.
        override fun getWidth(): Int = layoutPanel.layoutSize.width.roundToInt()

        override fun getHeight(): Int = layoutPanel.layoutSize.height.roundToInt()

        // To avoid reentering preferredLayoutSize, the container's own preferred size returns the
        // current size
        override fun getPreferredWidth(hHint: Int): Int = width

        override fun getPreferredHeight(wHint: Int): Int = height

        override fun getParent(): ContainerWrapper? = null

        override fun getComponentType(disregardScrollPane: Boolean): Int = ComponentWrapper.TYPE_CONTAINER
    }

    private companion object {
        fun parseLayout(constraints: String): LC =
            ConstraintParser.parseLayoutConstraint(ConstraintParser.prepare(constraints))
    }
}
