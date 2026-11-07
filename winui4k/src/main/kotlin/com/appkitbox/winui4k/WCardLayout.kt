package com.appkitbox.winui4k

/**
 * java.awt.CardLayout-like: stacks the children like cards and shows only one (for switching wizard or settings screens).
 *
 * Every child is placed at the same size filling the parent's area (what remains after subtracting [WLayoutPanel.insets] and [hgap] / [vgap]), and all but the shown one are
 * hidden with [WComponent.isVisible] = false. The first child added is shown first, and children added later are hidden (as in Swing).
 * Children are added with a name (a string constraint) with `panel.add(component, "name")`, and [show] selects by name. [first] / [last] / [next] /
 * [previous] switch in the order added (the order of [WLayoutPanel.components]); [next] after the last returns to the first, and [previous] before the first returns to the last.
 *
 * The preferred size is the maximum width and height of the children's preferred sizes plus twice the gaps and the insets (hidden children included, as in Swing).
 *
 * @throws IllegalArgumentException if the constraint when adding a child is not a string (null included) (as in Swing)
 */
class WCardLayout @JvmOverloads constructor(
    /** The gap at the left and right edges (CardLayout.get/setHgap-like). */
    var hgap: Double = 0.0,
    /** The gap at the top and bottom edges (CardLayout.get/setVgap-like). */
    var vgap: Double = 0.0,
) : WLayoutManager {
    /** The mapping from names to children (in the order added). Adding again with the same name replaces the child of that name (as in Swing). */
    private val cards = ArrayList<Pair<String, WComponent>>()

    override fun addLayoutComponent(component: WComponent, constraints: Any?) {
        require(constraints is String) { "cannot add to layout: constraint must be a string" }
        // Hide from the second on. The first one becomes the shown card
        val parent = component.parent as? WLayoutPanel
        if (parent != null && parent.layoutChildren.any { it !== component }) component.isVisible = false
        val index = cards.indexOfFirst { it.first == constraints }
        if (index >= 0) cards[index] = constraints to component else cards += constraints to component
    }

    override fun removeLayoutComponent(component: WComponent) {
        val index = cards.indexOfFirst { it.second === component }
        if (index >= 0) cards.removeAt(index)
    }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.preferredSize() }

    /** CardLayout.minimumLayoutSize-like: combines the children's [WComponent.minimumSize] with the same formula as the preferred size. */
    override fun minimumLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.minimumSize }

    private fun combinedSize(parent: WLayoutPanel, sizeOf: (WComponent) -> WSize): WSize {
        var width = 0.0
        var height = 0.0
        for (child in parent.layoutChildren) {
            val size = sizeOf(child)
            width = maxOf(width, size.width)
            height = maxOf(height, size.height)
        }
        val insets = parent.insets
        return WSize(insets.left + insets.right + width + hgap * 2, insets.top + insets.bottom + height + vgap * 2)
    }

    override fun layoutContainer(parent: WLayoutPanel) {
        val children = parent.layoutChildren
        if (children.isEmpty()) return
        val insets = parent.insets
        val width = maxOf(0.0, parent.layoutSize.width - (insets.left + insets.right + hgap * 2))
        val height = maxOf(0.0, parent.layoutSize.height - (insets.top + insets.bottom + vgap * 2))
        for (child in children) parent.setBounds(child, insets.left + hgap, insets.top + vgap, width, height)
        // If no card is shown (when the shown card was removed, etc.), show the first child (as in Swing)
        if (children.none { it.isVisible }) children[0].isVisible = true
    }

    /** CardLayout.first-like: shows the first child of [parent]. */
    fun first(parent: WLayoutPanel) {
        showAt(parent) { _, _ -> 0 }
    }

    /** CardLayout.last-like: shows the last child of [parent]. */
    fun last(parent: WLayoutPanel) {
        showAt(parent) { _, count -> count - 1 }
    }

    /** CardLayout.next-like: shows the child after the currently shown one (the first if it is the last). */
    fun next(parent: WLayoutPanel) {
        showAt(parent) { current, count -> if (current < 0) 0 else (current + 1) % count }
    }

    /** CardLayout.previous-like: shows the child before the currently shown one (the last if it is the first). */
    fun previous(parent: WLayoutPanel) {
        showAt(parent) { current, count -> if (current <= 0) count - 1 else current - 1 }
    }

    /** CardLayout.show-like: shows the child added with the name [name]. Does nothing if there is no child of that name (as in Swing). */
    fun show(parent: WLayoutPanel, name: String) {
        checkLayout(parent)
        val target = cards.firstOrNull { it.first == name }?.second ?: return
        val index = parent.layoutChildren.indexOfFirst { it === target }
        if (index < 0) return
        showAt(parent) { _, _ -> index }
    }

    /** Shows only the child at the position [choose] picks from the shown child's position and the number of children. Does nothing if there are no children. */
    private fun showAt(parent: WLayoutPanel, choose: (current: Int, count: Int) -> Int) {
        checkLayout(parent)
        val children = parent.layoutChildren
        if (children.isEmpty()) return
        val current = children.indexOfFirst { it.isVisible }
        val chosen = choose(current, children.size)
        for ((i, child) in children.withIndex()) child.isVisible = i == chosen
        parent.revalidate()
    }

    /** As in Swing, IllegalArgumentException if given a panel that does not use this manager. */
    private fun checkLayout(parent: WLayoutPanel) {
        require(parent.layout === this) { "wrong parent for CardLayout" }
    }

    override fun toString(): String = "WCardLayout[hgap=$hgap,vgap=$vgap]"
}
