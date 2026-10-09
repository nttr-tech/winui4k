package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonItemModel

/**
 * Replaces how ribbon items are displayed (an override of Ribbon.ItemFactory / RibbonElementFactory.CreateItem in RibbonSpace).
 *
 * When set on [WRibbon.itemFactory], it is called each time an item view is created. An item for which a component is
 * returned shows that component; an item for which null is returned uses the default display. The same item also appears in
 * the QAT and menus, so return a new component on each call. To execute the item from interaction with the returned
 * component, use the commands or listeners of [RibbonItemModel].
 */
fun interface RibbonItemFactory {
    /** The component used to display [item] (null for the default display). */
    fun createItem(item: RibbonItemModel): WComponent?
}

/** An item view that hosts a component created by [RibbonItemFactory]. The component determines its own size. */
internal class RibbonFactoryItemView(model: RibbonItemModel, private val content: WComponent, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<Border VerticalAlignment=\"Center\" />")

    init {
        Xaml.detach(content)
        element.setChild(content)
    }

    override fun measure(layout: RibbonItemLayout): WSize {
        val desired = content.preferredSize()
        return WSize(desired.width, desired.height)
    }

    override fun applyLayoutCore() {
        // The component determines its own size and look
    }

    override fun refreshEnabled() {
        // The component has its own enabled state
    }

    override fun refreshToolTip() {
        // The component has its own tooltip
    }
}
