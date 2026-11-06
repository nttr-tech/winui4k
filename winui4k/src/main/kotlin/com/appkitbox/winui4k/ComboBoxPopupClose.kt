package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The part that decides, when the drop-down of a [WComboBox] closes, whether it was a cancellation ([WPopupMenuListener.popupMenuCanceled]). As in Swing,
 * only closing with Esc or an outside click (or losing the focus) without choosing an item is a cancellation. These ways of closing are not cancellations:
 * - [WComboBox.hidePopup] / `isPopupVisible = false` ([hideRequested])
 * - releasing the pointer over an item (including reselecting the current item)
 * - Enter, Space (when not editable), Tab, F4, Alt + ↑↓ (the keys PopupKeyDown in ComboBox_Partial.cpp closes with, not as a cancellation)
 *
 * Key and pointer events inside the drop-down do not propagate outside the popup (to the ComboBox), and the ComboBox also receives them with handlers attached to the content
 * (SetupElementPopupChild in ComboBox_Partial.cpp), so it subscribes to the Child of the template part "Popup". The closing keys must be known before the ComboBox's
 * handler (KeyDown) closes it, so PreviewKeyDown, which arrives at the parent first, is used. A pointer released over an item is marked handled by ComboBoxItem
 * (PerformPointerUpAction in ComboBoxItem_Partial.cpp), so handled ones are received too. DropDownClosed comes after the closing
 * animation (FinishClosingDropDown), so both are in time. The native handlers capture only this object
 * (it does not reference the combo wrapper, so no cycle forms). Used only by the UI thread.
 */
internal class ComboBoxPopupClose {
    /** Asked to close with [WComboBox.hidePopup] etc. after opening. Reset on every opening. */
    var hideRequested = false

    /** An operation that closes it without canceling was done after opening. Reset on every opening. */
    private var closedByChoice = false

    /** Whether it was editable when opened (Space in an editable combo is text input and does not close it). */
    private var editable = false

    /** The identity of the subscribed drop-down content (the IUnknown address; 0 if not yet). No native reference is held. */
    private var hookedContent = 0L

    /** The drop-down opened (DropDownOpened). [comboBox] is the ComboBox (borrowed). */
    fun opened(comboBox: ComPtr, isEditable: Boolean) {
        hideRequested = false
        closedByChoice = false
        editable = isEditable
        hook(comboBox)
    }

    /** The drop-down closed (DropDownClosed). true if it was a cancellation. The marks are reset. */
    fun closed(): Boolean {
        val canceled = !hideRequested && !closedByChoice
        hideRequested = false
        closedByChoice = false
        return canceled
    }

    private fun hook(comboBox: ComPtr) {
        val content = dropDownContent(comboBox) ?: return
        try {
            val identity = Xaml.identityOf(content)
            if (identity == hookedContent) return
            hookedContent = identity
            content.addEventHandler(
                "WinUI4K.ComboBoxDropDownPreviewKeyDown",
                XamlInterop.IID_KeyEventHandler,
                XamlInterop.IUIElement_add_PreviewKeyDown,
            ) { _, args -> onPreviewKeyDown(XamlKeyEvent(ComPtr(args))) }
            Xaml.addHandlerHandledToo(
                content,
                XamlInterop.IUIElementStatics_get_PointerReleasedEvent,
                XamlInterop.IID_PointerEventHandler,
                FoundationInterop.IID_IReference_PointerEventHandler,
            ) { _, args -> onPointerReleased(args) }
        } finally {
            content.release()
        }
    }

    private fun onPreviewKeyDown(event: XamlKeyEvent) {
        val modifiers = Xaml.currentModifiers()
        closedByChoice = closedByChoice || when (event.key) {
            VK_RETURN, VK_TAB -> true
            VK_SPACE -> !editable
            VK_F4 -> modifiers == 0
            VK_UP, VK_DOWN -> (modifiers and Xaml.MODIFIER_MENU) != 0
            else -> false
        }
    }

    private fun onPointerReleased(args: Ptr) {
        val source = Xaml.originalSource(args) ?: return
        if (isInComboBoxItem(source)) closedByChoice = true
    }

    private companion object {
        /** The maximum number of parents walked when looking for the ComboBoxItem from an element inside an item. */
        const val MAX_ITEM_DEPTH = 32

        // Windows.System.VirtualKey
        const val VK_TAB = 9
        const val VK_RETURN = 13
        const val VK_SPACE = 32
        const val VK_UP = 38
        const val VK_DOWN = 40
        const val VK_F4 = 115

        /** The Child (IUIElement) of the template part "Popup" of [comboBox]. null before the template is applied. The caller releases it. */
        fun dropDownContent(comboBox: ComPtr): ComPtr? {
            val part = Xaml.appliedTemplatePartOrNull(comboBox, "Popup") ?: return null
            val popup = try {
                part.queryInterfaceOrNull(XamlInterop.IID_IPopup)
            } finally {
                part.release()
            } ?: return null
            val child = try {
                popup.getPtrOrNull(XamlInterop.IPopup_get_Child)
            } finally {
                popup.release()
            } ?: return null
            return try {
                child.queryInterfaceOrNull(XamlInterop.IID_IUIElement)
            } finally {
                child.release()
            }
        }

        /** Whether [element] (released by this function) or one of its ancestors is a ComboBoxItem. */
        fun isInComboBoxItem(element: ComPtr): Boolean {
            var current: ComPtr? = element
            var depth = 0
            try {
                while (current != null && depth++ < MAX_ITEM_DEPTH) {
                    val item = current.queryInterfaceOrNull(XamlInterop.IID_IComboBoxItem)
                    if (item != null) {
                        item.release()
                        return true
                    }
                    val parent = Xaml.parentOf(current)
                    current.release()
                    current = parent
                }
                return false
            } finally {
                current?.release()
            }
        }
    }
}
