package com.appkitbox.winui4k

/**
 * java.awt.Dimension-like: a width/height pair used in layout computations (in DIP).
 * Use the separate [WDimension] type for a window's pixel size ([WAppWindow.size]).
 * Java can read it either through the fields (`size.width`) or the getters (`size.getWidth()`).
 */
data class WSize(@JvmField val width: Double, @JvmField val height: Double) {
    /** A getter for Java (the same as [width]). */
    fun getWidth(): Double = width

    /** A getter for Java (the same as [height]). */
    fun getHeight(): Double = height
}

/**
 * java.awt.Insets-like: the inner margins of a container (in DIP).
 * Java can read it either through the fields (`insets.top`) or the getters (`insets.getTop()`).
 */
data class WInsets(
    @JvmField val top: Double = 0.0,
    @JvmField val left: Double = 0.0,
    @JvmField val bottom: Double = 0.0,
    @JvmField val right: Double = 0.0,
) {
    /** The same padding on all four sides. */
    constructor(all: Double) : this(all, all, all, all)

    /** A getter for Java (the same as [top]). */
    fun getTop(): Double = top

    /** A getter for Java (the same as [left]). */
    fun getLeft(): Double = left

    /** A getter for Java (the same as [bottom]). */
    fun getBottom(): Double = bottom

    /** A getter for Java (the same as [right]). */
    fun getRight(): Double = right
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

    /**
     * The horizontal alignment of [parent] (like LayoutManager2.getLayoutAlignmentX, 0..1). Becomes the value of a [WLayoutPanel] that does not set
     * [alignmentX][WComponent.alignmentX], and is used by a nested [WBoxLayout]. Center (0.5) by default.
     */
    fun getLayoutAlignmentX(parent: WLayoutPanel): Float = WComponent.CENTER_ALIGNMENT

    /** The vertical alignment of [parent] (like LayoutManager2.getLayoutAlignmentY). Center (0.5) by default. See [getLayoutAlignmentX] for details. */
    fun getLayoutAlignmentY(parent: WLayoutPanel): Float = WComponent.CENTER_ALIGNMENT

    /** Positions children to fit [WLayoutPanel.layoutSize] (calls [WLayoutPanel.setBounds]). */
    fun layoutContainer(parent: WLayoutPanel)
}
