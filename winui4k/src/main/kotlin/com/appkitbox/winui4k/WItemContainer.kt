package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * WinUI 3's ItemContainer. No Swing equivalent.
 * Wraps each item in a [WItemsView], forming the unit that selection and clicks
 * (ItemInvoked) target. Set its contents as a single [child].
 */
class WItemContainer @JvmOverloads constructor(child: WComponent? = null) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ItemContainer, XamlInterop.IID_IItemContainerFactory),
) {
    /** The single child shown inside the container (ItemContainer.Child). */
    var child: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IItemContainer_put_Child, value?.uiElement?.ptr)
        }

    /**
     * Whether this container is selected (ItemContainer.IsSelected). If it is in a [WItemsView], assigning changes the view's selection
     * (also notified to [WItemsView.addListSelectionListener]).
     */
    var isSelected: Boolean
        get() = inspectable.getBool(XamlInterop.IItemContainer_get_IsSelected)
        set(value) = inspectable.putBool(XamlInterop.IItemContainer_put_IsSelected, value)

    init {
        if (child != null) this.child = child
    }
}
