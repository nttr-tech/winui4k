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

    /** Positions children to fit [WLayoutPanel.layoutSize] (calls [WLayoutPanel.setBounds]). */
    fun layoutContainer(parent: WLayoutPanel)
}
