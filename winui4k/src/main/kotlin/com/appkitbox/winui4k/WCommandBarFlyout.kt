package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * A context menu with a mini toolbar attached: WinUI 3's CommandBarFlyout.
 * Buttons added with [addPrimaryCommand] show in a row; items added with
 * [addSecondaryCommand] show in the menu below them. Set it on WComponent.contextFlyout to use it.
 */
class WCommandBarFlyout : WFlyoutBase(
    Activation.composeDefault(Abi.CLS_CommandBarFlyout, Abi.IID_ICommandBarFlyoutFactory),
) {
    private val primaryCommands: ComPtr by lazy {
        queryVector(Abi.ICommandBarFlyout_get_PrimaryCommands)
    }
    private val secondaryCommands: ComPtr by lazy {
        queryVector(Abi.ICommandBarFlyout_get_SecondaryCommands)
    }

    /** QIs an IObservableVector<ICommandBarElement> to an IVector view that has Append. */
    private fun queryVector(getSlot: Int): ComPtr {
        val observable = inspectable.getPtr(getSlot)
        return try {
            observable.queryInterface(Abi.IID_IVector_ICommandBarElement)
        } finally {
            observable.release()
        }
    }

    /**
     * Appends a command shown in the row-of-buttons mini toolbar (CommandBarFlyout.PrimaryCommands).
     * [element] must be an ICommandBarElement implementation (WAppBarButton /
     * WAppBarToggleButton / WAppBarSeparator).
     */
    fun addPrimaryCommand(element: WControl) {
        appendCommandBarElement(primaryCommands, element)
    }

    /** Appends a command shown in the menu below the mini toolbar (SecondaryCommands). */
    fun addSecondaryCommand(element: WControl) {
        appendCommandBarElement(secondaryCommands, element)
    }
}
