package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation

/**
 * Shared part for the selection state of check and radio menu items ([WToggleMenuFlyoutItem] / [WRadioMenuFlyoutItem]):
 * the ItemListener list, membership in a [WButtonGroup], and two-way sync with [WAction.SELECTED_KEY]. Each item holds one.
 *
 * Neither item has a Checked event, so changes are received through the change callback of the IsChecked dependency property
 * (DependencyObject.RegisterPropertyChangedCallback). [WRadioMenuFlyoutItem] has its own IsChecked
 * (RadioMenuFlyoutItem.IsCheckedProperty), separate from the base ToggleMenuFlyoutItem.IsChecked, so [isCheckedProperty]
 * tells which one to watch. The callback is called only when the value changes, for both programmatic changes and clicks (as in Swing).
 * The callback stays on the native side, so it does not capture the item strongly but looks it up through [WComponent.listenerAnchor]. Use only from the UI thread.
 */
internal class MenuItemSelection(
    private val item: WMenuFlyoutItem,
    private val selectable: WSelectable,
    private val isCheckedProperty: () -> ComPtr,
) {
    /** The user's ItemListeners. The callback is registered only once, when first needed. */
    val itemListeners = ItemListeners(item) { addCallback() }

    /** The [WButtonGroup] this item belongs to (changed only by [WButtonGroup.add] / [WButtonGroup.remove]). */
    var group: WButtonGroup? = null
        private set

    /** Whether an action is bound. While bound, it counts as one listener for writing back the selection state. */
    private var actionBound = false

    private fun addCallback() {
        val anchor = item.listenerAnchor
        val property = isCheckedProperty()
        try {
            TableViewXaml.registerPropertyChangedCallback(item.dependencyObject, property) { _ ->
                (anchor.get() as? WMenuFlyoutItem)?.menuSelection?.onCheckedChanged()
            }
        } finally {
            property.release()
        }
    }

    /**
     * Delivers one native change in the same order as Swing's JCheckBoxMenuItem / JRadioButtonMenuItem (same as [WToggleButton]).
     * 1. Exclusion by the [WButtonGroup] (the group reselects a selected item that was about to be deselected, and this notification is delivered to no one)
     * 2. The user's ItemListeners (in the order added)
     * 3. Write-back to SELECTED_KEY of [WMenuFlyoutItem.action] (only when it has a value)
     */
    fun onCheckedChanged() {
        val selected = selectable.isSelected
        val stateChange = if (selected) WItemEvent.SELECTED else WItemEvent.DESELECTED
        val current = group
        if (current != null && !current.onItemStateChanged(selectable, stateChange)) return
        itemListeners.fire(WItemEvent(item, stateChange))
        val action = item.action
        val value = action?.getValue(WAction.SELECTED_KEY)
        if (action != null && value != null && value != selected) action.putValue(WAction.SELECTED_KEY, selected)
    }

    /**
     * Matches the selection state to the action's SELECTED_KEY (false if absent). Throws [IllegalArgumentException] if it is not a Boolean.
     * Deselecting the group's selected item clears the group's selection too (same as [WToggleButton]).
     */
    fun applyActionSelected(action: WAction?) {
        val selected = action?.typedValue<Boolean>(WAction.SELECTED_KEY, "Boolean") == true
        if (selected == selectable.isSelected) return
        val current = group
        if (!selected && current != null && current.isSelected(selectable)) {
            current.clearSelection()
        } else {
            selectable.isSelected = selected
        }
    }

    /** Receives selection state notifications only while an action is bound (for writing back). */
    fun onActionChanged(action: WAction?) {
        val bound = action != null
        if (bound == actionBound) return
        actionBound = bound
        if (bound) {
            itemListeners.ensureNativeHandler()
            item.listenerAnchor.listenerAdded()
        } else {
            item.listenerAnchor.listenerRemoved()
        }
    }

    /** Becomes a member of [group]. It receives selection state notifications (for the exclusion control), so it is treated like holding one listener. */
    fun joinGroup(group: WButtonGroup) {
        check(this.group == null) { "Already belongs to another ButtonGroup" }
        this.group = group
        itemListeners.ensureNativeHandler()
        item.listenerAnchor.listenerAdded()
    }

    /** Stops being a member of the group. */
    fun leaveGroup() {
        if (group == null) return
        group = null
        item.listenerAnchor.listenerRemoved()
    }

    companion object {
        /** Gets the DependencyProperty from [slot] (get_IsCheckedProperty) of the Statics ([staticsIid]) of [className]. The caller releases it. */
        fun dependencyProperty(className: String, staticsIid: String, slot: Int): ComPtr {
            val statics = Activation.factory(className, staticsIid)
            return try {
                statics.getPtr(slot)
            } finally {
                statics.release()
            }
        }
    }
}
