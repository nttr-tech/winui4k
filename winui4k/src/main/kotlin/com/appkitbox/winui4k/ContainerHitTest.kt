package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr

/**
 * The groundwork of [WContainer.getComponentAt] / [WContainer.findComponentAt]. Looks up the elements at a point from the frontmost with the native hit test
 * (VisualTreeHelper.FindElementsInHostCoordinates, also including elements excluded from hit testing (IsHitTestVisible = false or panels without a background)),
 * and follows visual tree parents back to the child's wrapper.
 */
internal object ContainerHitTest {
    /**
     * Returns the child ([deepest] = false; a direct child) or descendant (true; the deepest one) at ([x], [y]) (DIP) seen from the top left of [container].
     * [container] where there is no child, and null outside [container].
     */
    fun componentAt(container: WContainer, x: Int, y: Int, deepest: Boolean): WComponent? {
        if (!container.contains(x, y)) return null
        val wrappers = HashMap<Long, WComponent>()
        collect(container, deepest, wrappers)
        if (wrappers.isEmpty()) return container
        val self = Xaml.identityOf(container.inspectable)
        // The point is checked at the center of a DIP cell (excluding the right and bottom edges, like Swing's 0 <= x < width)
        val hits = VisualHitTest.elementsAt(container.inspectable, x + HALF, y + HALF, includeAllElements = true)
        try {
            for (hit in hits) {
                ownerInChain(hit, self, wrappers)?.let { return it.component ?: container }
            }
            return container
        } finally {
            hits.forEach { it.release() }
        }
    }

    /** Puts the children of [container] (and descendants if [deep]) into [into] so that wrappers can be looked up from their native identity. */
    private fun collect(container: WContainer, deep: Boolean, into: MutableMap<Long, WComponent>) {
        for (child in container.componentList) {
            into[Xaml.identityOf(child.inspectable)] = child
            if (deep && child is WContainer) collect(child, deep = true, into = into)
        }
    }

    /** The result of [ownerInChain]. [component] is the first wrapper found while following parents (null if none = the container itself). */
    private class Owner(val component: WComponent?)

    /**
     * Follows visual tree parents from [hit] (borrowed) up to [self] (the container's identity) and returns the first wrapper of [wrappers] found on the way.
     * null if [self] is not reached (not an element inside the container).
     */
    private fun ownerInChain(hit: ComPtr, self: Long, wrappers: Map<Long, WComponent>): Owner? {
        var found: WComponent? = null
        var current = hit
        var owned = false
        try {
            while (true) {
                val identity = Xaml.identityOf(current)
                if (identity == self) return Owner(found)
                if (found == null) found = wrappers[identity]
                val parent = Xaml.parentOf(current) ?: return null
                if (owned) current.release()
                current = parent
                owned = true
            }
        } finally {
            if (owned) current.release()
        }
    }

    private const val HALF = 0.5
}
