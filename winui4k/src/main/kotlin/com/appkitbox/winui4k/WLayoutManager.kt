package com.appkitbox.winui4k

/**
 * java.awt.Dimension-like: a width/height pair used in layout computations (in DIP).
 * Use the separate [WDimension] type for a window's pixel size ([WAppWindow.size]).
 */
data class WSize(val width: Double, val height: Double)

/** java.awt.Insets-like: a container's inner padding (in DIP). */
data class WInsets(
    val top: Double = 0.0,
    val left: Double = 0.0,
    val bottom: Double = 0.0,
    val right: Double = 0.0,
) {
    /** The same padding on all four sides. */
    constructor(all: Double) : this(all, all, all, all)
}

/**
 * java.awt.LayoutManager-like: the contract for the layout computation that positions
 * [WLayoutPanel]'s children. All computation happens in Kotlin, and the result is applied to the
 * children via [WLayoutPanel.setBounds]. Constrained addition (equivalent to LayoutManager2) is
 * folded in from the start.
 */
interface WLayoutManager {
    /** Notified, along with its constraints, when a child is added via [WLayoutPanel.add]. */
    fun addLayoutComponent(component: WComponent, constraints: Any?) {}

    /** Notified when a child is removed via [WLayoutPanel.remove]. */
    fun removeLayoutComponent(component: WComponent) {}

    /** Computes [parent]'s preferred size (including [WLayoutPanel.insets]). */
    fun preferredLayoutSize(parent: WLayoutPanel): WSize

    /**
     * Computes the minimum size of [parent] (like LayoutManager.minimumLayoutSize, including [WLayoutPanel.insets]).
     * The default is the same as [preferredLayoutSize]. [WLayoutPanel.minimumSize] returns this value.
     */
    fun minimumLayoutSize(parent: WLayoutPanel): WSize = preferredLayoutSize(parent)

    /**
     * Computes the maximum size of [parent] (like LayoutManager2.maximumLayoutSize).
     * The default is unlimited (both width and height are [Double.POSITIVE_INFINITY]). [WLayoutPanel.maximumSize] returns this value.
     */
    fun maximumLayoutSize(parent: WLayoutPanel): WSize = WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

    /**
     * A notification that cached computation results should be discarded (like LayoutManager2.invalidateLayout).
     * Called from [WLayoutPanel.invalidate] (that is, from [WLayoutPanel.revalidate] or setting [WLayoutPanel.layout]). Does nothing by default.
     */
    fun invalidateLayout(parent: WLayoutPanel) {}

    /** Positions children to fit [WLayoutPanel.layoutSize] (calls [WLayoutPanel.setBounds]). */
    fun layoutContainer(parent: WLayoutPanel)
}
