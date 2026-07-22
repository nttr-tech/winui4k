package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * WinUI 3's ItemContainer. No Swing equivalent.
 * Wraps each item in a [WItemsView], forming the unit that selection and clicks
 * (ItemInvoked) target. Set its contents as a single [child].
 */
class WItemContainer(child: WComponent? = null) : WControl(
    Activation.composeDefault(Abi.CLS_ItemContainer, Abi.IID_IItemContainerFactory),
) {
    /** The single child shown inside the container (ItemContainer.Child). */
    var child: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.IItemContainer_put_Child, value?.uiElement?.ptr)
        }

    init {
        if (child != null) this.child = child
    }
}
