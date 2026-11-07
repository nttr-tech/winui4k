package com.appkitbox.winui4k

/**
 * The shared handling of add with constraints to the content of [WFrame] and [WContentDialog] (the part corresponding to JRootPane's contentPane).
 */
internal object ContentPanes {
    /**
     * Container.add(Component, Object, int)-like: adds a child with constraints to the content [container]. The default content is, as in JFrame / JDialog,
     * a [WLayoutPanel] with [WBorderLayout], so `frame.add(c, WBorderLayout.NORTH)` works as is. If constraints were silently dropped when the content
     * is replaced with a [WPanel] etc. or has a null layout, the arrangement would differ from Swing unnoticed, so passing a non-null constraint
     * to content without a layout manager throws [IllegalArgumentException].
     */
    fun add(container: WContainer, component: WComponent, constraints: Any?, index: Int) {
        if (constraints != null) {
            require(container is WLayoutPanel && container.layout != null) {
                "The content (${container.javaClass.simpleName}) has no layout manager, so the constraint ($constraints) cannot be used. " +
                    "Set a layout manager with setLayout, or add without a constraint"
            }
        }
        container.addImpl(component, constraints, index)
    }

    /**
     * The client area size that Window.pack fits to. [desired] is the size of Window.Content measured without constraints. If the content [content]
     * is a [WLayoutPanel], the Canvas's DesiredSize is 0, so the children's natural sizes are measured again and the layout manager's preferred size
     * is used, plus the height of the menu bar [menuBar] if any.
     */
    fun packedSize(desired: WSize, content: WComponent, menuBar: WMenuBar?): WSize {
        val panel = content as? WLayoutPanel ?: return desired
        panel.validate()
        val preferred = panel.preferredSize()
        val menu = menuBar?.readDesiredSize() ?: WSize(0.0, 0.0)
        return WSize(maxOf(preferred.width, menu.width), preferred.height + menu.height)
    }
}
