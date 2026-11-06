package com.appkitbox.winui4k

import java.util.Collections
import java.util.Enumeration
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * ButtonGroup-style: allows only one of several toggle-type buttons ([WToggleButton] / [WCheckBox] / [WRadioButton]) and check and radio menu items
 * ([WToggleMenuFlyoutItem] / [WRadioMenuFlyoutItem]) to be selected. The member type is [WSelectable].
 *
 * When one of the buttons [add]ed is selected (by a click, or by [WToggleButton.isSelected] / [setSelected]), the other
 * buttons go back to unselected. The DESELECTED for the ItemListener of the deselected button arrives before the
 * SELECTED of the selected button (as in Swing).
 * Neither clicking the selected button nor setting [WToggleButton.isSelected] = false deselects it, and nothing is
 * delivered to the ItemListener (as with Swing's JToggleButton. WinUI buttons change state before notifying, so the
 * group restores the selection on the spot, and the notifications of that change and of the restoration are delivered
 * neither to ItemListeners nor to actions). To deselect all, use [clearSelection].
 *
 * WinUI's RadioButton is exclusive per parent (or per visual tree root when GroupName is set), so [add]ing a [WRadioButton] gives it
 * a unique [WRadioButton.groupName] dedicated to this group (all members of the group share the same name). Radio buttons of different groups
 * stay independent even under the same parent, and WinUI's arrow key focus navigation (which moves within the same GroupName) works within the group.
 * [remove] restores the groupName it had before joining the group. A [WRadioMenuFlyoutItem] also gets a unique name (WinUI's
 * RadioMenuFlyoutItem shares GroupName across the whole UI thread, so two groups with the same texts do not interfere with each other).
 * A [WRadioMenuFlyoutItem] [remove]d while checked is unchecked when another item of the group is selected, because WinUI remembers it
 * as the last selection for that name (in Swing it stays checked).
 *
 * There is no counterpart of Swing's `ButtonModel`, so [selection] / [isSelected] / [setSelected] deal with the buttons
 * themselves.
 *
 * Memory: the group and its buttons reference each other strongly (as in Swing). A button passes selection state
 * notifications to the group first, but its native handler references only the button weakly through
 * [ListenerAnchor] and does not capture the group. So a group and buttons that are removed from the window and no longer
 * referenced are collected together without forming a cycle across the native boundary. As with other listeners, a
 * button that has never been placed in a window is not collected until it is [remove]d from the group.
 * Use only from the UI thread.
 */
class WButtonGroup {
    private val members = ArrayList<WSelectable>()

    /** The selected button, or null if none is selected (like ButtonGroup.getSelection, but the button itself rather than a ButtonModel). */
    var selection: WSelectable? = null
        private set

    /** True while the group is changing a button's state. Notifications that arrive then are not subject to the exclusion control. */
    private var updating = false

    /** The button whose attempted deselection the group is currently reverting. Notifications of the reversion are delivered to no one. */
    private var reverting: WSelectable? = null

    /** The GroupName given to the radio buttons and radio menu items of this group (shared by the members). */
    private val groupName = newGroupName()

    /** The groupName the radio buttons and radio menu items had before joining the group (restored by remove). */
    private val originalGroupNames = IdentityHashMap<WSelectable, String>()

    /** The number of buttons in the group (equivalent to ButtonGroup.getButtonCount). */
    val buttonCount: Int get() = members.size

    /** A copy of the buttons in the group in the order they were [add]ed. For use from Kotlin (from Java, use [getElements]). */
    val buttons: List<WSelectable> get() = members.toList()

    /**
     * ButtonGroup.getElements-style: returns the buttons in the group in the order they were [add]ed.
     * It is a copy taken at the time of the call, so [add] / [remove] during the enumeration has no effect (safe, unlike
     * Swing's Vector.elements()).
     */
    val elements: Enumeration<WSelectable> get() = Collections.enumeration(buttons)

    /**
     * ButtonGroup.add-style: adds [button] to the group. Does nothing for a button that is already in it. If the button belongs to another group, removes it from that group.
     * A selected button becomes the [selection] if the group has no selection yet, and is deselected if it already has one.
     * A [WSelectable] implemented on one's own in Java throws [IllegalArgumentException].
     */
    fun add(button: WSelectable) {
        requireSupported(button)
        if (members.any { it === button }) return
        button.currentGroup()?.remove(button)
        members += button
        button.enterGroup(this)
        if (button.isSelected) {
            if (selection == null) {
                selection = button
            } else {
                withUpdating { button.isSelected = false }
            }
        }
        // Set the name after settling the selection (setting it while selected makes WinUI deselect the group's original selection instead)
        when (button) {
            is WRadioButton -> {
                originalGroupNames[button] = button.groupName
                button.groupName = groupName
            }
            is WRadioMenuFlyoutItem -> {
                originalGroupNames[button] = button.groupName
                button.groupName = groupName
            }
            else -> Unit
        }
    }

    /**
     * ButtonGroup.remove-style: removes [button] from the group. If it was the [selection], [selection] becomes null
     * (the button's own selection state stays as it is). Does nothing for a button that is not in the group.
     */
    fun remove(button: WSelectable) {
        val index = members.indexOfFirst { it === button }
        if (index < 0) return
        members.removeAt(index)
        if (selection === button) selection = null
        button.exitGroup()
        when (button) {
            is WRadioButton -> button.groupName = originalGroupNames.remove(button) ?: ""
            is WRadioMenuFlyoutItem -> button.groupName = originalGroupNames.remove(button) ?: ""
            else -> Unit
        }
    }

    /** ButtonGroup.clearSelection-style: deselects all buttons and sets [selection] to null. */
    fun clearSelection() {
        val old = selection ?: return
        selection = null
        deselect(old)
    }

    /** ButtonGroup.isSelected-style: whether [button] is the [selection]. */
    fun isSelected(button: WSelectable): Boolean = selection === button

    /**
     * ButtonGroup.setSelected-style: if [selected] is true, selects [button] and deselects the previous [selection].
     * Setting it to false does nothing (the group cannot deselect a selected button; use [clearSelection] for that).
     * Buttons that are not in the group are ignored.
     */
    fun setSelected(button: WSelectable, selected: Boolean) {
        if (!selected || selection === button || members.none { it === button }) return
        val old = selection
        selection = button
        old?.let { deselect(it) }
        withUpdating { button.isSelected = true }
    }

    /**
     * Notification that a member's selection state changed (from [WToggleButton] and menu items, before their ItemListeners). A notification
     * for which this returns false is delivered neither to the button's ItemListeners nor to actions.
     * - Selected: makes the button the [selection] and deselects the previous [selection] (its DESELECTED is delivered first)
     * - The [selection] was deselected: if another member has been selected (when a button with the same GroupName is selected, WinUI's RadioButton
     *   sends this one's Unchecked before the selected one's Checked; RadioButton::OnChecked), makes that one the [selection].
     *   Otherwise it was only an attempt to deselect, so selects it again and delivers neither the deselection nor the reselection notification (in Swing the state does not change)
     */
    internal fun onItemStateChanged(button: WSelectable, stateChange: Int): Boolean {
        if (reverting === button) return false
        if (updating) return true
        if (stateChange == WItemEvent.SELECTED) {
            if (selection !== button) {
                val old = selection
                selection = button
                old?.let { deselect(it) }
            }
            return true
        }
        if (selection !== button) return true
        val selectedByWinUi = members.firstOrNull { it !== button && it.isSelected }
        if (selectedByWinUi != null) {
            selection = selectedByWinUi
            return true
        }
        reverting = button
        try {
            withUpdating { button.isSelected = true }
        } finally {
            reverting = null
        }
        return false
    }

    private fun deselect(button: WSelectable) {
        withUpdating { button.isSelected = false }
    }

    private inline fun withUpdating(action: () -> Unit) {
        val wasUpdating = updating
        updating = true
        try {
            action()
        } finally {
            updating = wasUpdating
        }
    }

    private companion object {
        private val counter = AtomicLong()

        /** Whether [WSelectable] is implemented by the library (Java does not enforce sealed, so a custom implementation can be passed). */
        fun requireSupported(button: Any) {
            require(button is WToggleButton || button is WToggleMenuFlyoutItem || button is WRadioMenuFlyoutItem) {
                "Unsupported WSelectable implementation: ${button.javaClass.name} (only WToggleButton and its subclasses, WToggleMenuFlyoutItem and " +
                    "WRadioMenuFlyoutItem can be added)"
            }
        }

        /** The group each [WSelectable] instance belongs to. */
        fun WSelectable.currentGroup(): WButtonGroup? = when (this) {
            is WToggleButton -> buttonGroup
            is WToggleMenuFlyoutItem -> menuSelection.group
            is WRadioMenuFlyoutItem -> menuSelection.group
        }

        fun WSelectable.enterGroup(group: WButtonGroup) = when (this) {
            is WToggleButton -> joinGroup(group)
            is WToggleMenuFlyoutItem -> menuSelection.joinGroup(group)
            is WRadioMenuFlyoutItem -> menuSelection.joinGroup(group)
        }

        fun WSelectable.exitGroup() = when (this) {
            is WToggleButton -> leaveGroup()
            is WToggleMenuFlyoutItem -> menuSelection.leaveGroup()
            is WRadioMenuFlyoutItem -> menuSelection.leaveGroup()
        }

        /** A name for giving each group a different WinUI GroupName (with a prefix that does not collide with other GroupNames). */
        fun newGroupName(): String = "WinUI4K.ButtonGroup#" + counter.incrementAndGet()
    }
}
