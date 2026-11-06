package com.appkitbox.winui4k

/**
 * Common processing of add / setLayout for the content of [WFrame] and [WContentDialog] (the part corresponding to JRootPane's contentPane).
 */
internal object ContentPanes {
    /**
     * Container.add(Component, Object, int)-like: adds a child with a constraint to the content [container].
     * Swing's JFrame has a BorderLayout as its default content, so `frame.add(c, BorderLayout.NORTH)` works as is, but WinUI4K's default content is
     * a vertical StackPanel ([WPanel]) that cannot use constraints. Silently discarding them would leave the arrangement different from Swing unnoticed, so a non-null
     * constraint passed to content without a layout manager throws [IllegalArgumentException] (telling the caller to call setLayout).
     */
    fun add(container: WContainer, component: WComponent, constraints: Any?, index: Int) {
        if (constraints != null) {
            require(container is WLayoutPanel && container.layout != null) {
                "The content (${container.javaClass.simpleName}) has no layout manager, so the constraint ($constraints) cannot be used. " +
                    "Decide how the content is arranged first with setLayout(WBorderLayout()) or the like"
            }
        }
        container.addImpl(component, constraints, index)
    }

    /** Creates a [WLayoutPanel] with [layout] holding the children of [from] moved in the same order (when setLayout replaces the default content). */
    fun movedToLayoutPanel(from: WContainer, layout: WLayoutManager?): WLayoutPanel {
        val panel = WLayoutPanel(layout)
        from.componentList.toList().forEach { panel.add(it) }
        return panel
    }
}
