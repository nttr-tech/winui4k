package com.appkitbox.winui4k

import java.util.Collections
import java.util.Enumeration
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * ButtonGroup-style: allows only one of several toggle-type buttons ([WToggleButton] / [WCheckBox] / [WRadioButton])
 * to be selected.
 *
 * When one of the buttons [add]ed is selected (by a click, or by [WToggleButton.isSelected] / [setSelected]), the other
 * buttons go back to unselected. The DESELECTED for the ItemListener of the deselected button arrives before the
 * SELECTED of the selected button (as in Swing).
 * Neither clicking the selected button nor setting [WToggleButton.isSelected] = false deselects it, and nothing is
 * delivered to the ItemListener (as with Swing's JToggleButton. WinUI buttons change state before notifying, so the
 * group restores the selection on the spot, and the notifications of that change and of the restoration are delivered
 * neither to ItemListeners nor to actions). To deselect all, use [clearSelection].
 *
 * WinUI's RadioButton is exclusive per parent (or per visual tree root when GroupName is set), so [add]ing a
 * [WRadioButton] gives it a unique [WRadioButton.groupName] dedicated to this group (all members of the group share
 * the same name). Radio buttons of different groups stay independent even under the same parent, and WinUI's arrow
 * key focus navigation (which moves within the same GroupName) works within the group.
 * [remove] restores the groupName it had before joining the group.
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
    private val members = ArrayList<WToggleButton>()

    /** The selected button, or null if none is selected (like ButtonGroup.getSelection, but the button itself rather than a ButtonModel). */
    var selection: WToggleButton? = null
        private set

    /** True while the group is changing a button's state. Notifications that arrive then are not subject to the exclusion control. */
    private var updating = false

    /** The button whose attempted deselection the group is currently reverting. Notifications of the reversion are delivered to no one. */
    private var reverting: WToggleButton? = null

    /** The GroupName given to this group's radio buttons (shared by the members). */
    private val groupName = newGroupName()

    /** The groupName a radio button had before joining the group (restored by remove). */
    private val originalGroupNames = IdentityHashMap<WRadioButton, String>()

    /** The number of buttons in the group (equivalent to ButtonGroup.getButtonCount). */
    val buttonCount: Int get() = members.size

    /** A copy of the buttons in the group in the order they were [add]ed. For use from Kotlin (from Java, use [getElements]). */
    val buttons: List<WToggleButton> get() = members.toList()

    /**
     * ButtonGroup.getElements-style: returns the buttons in the group in the order they were [add]ed.
     * It is a copy taken at the time of the call, so [add] / [remove] during the enumeration has no effect (safe, unlike
     * Swing's Vector.elements()).
     */
    val elements: Enumeration<WToggleButton> get() = Collections.enumeration(buttons)

    /**
     * ButtonGroup.add-style: adds [button] to the group. Does nothing for a button that is already in it. If the button
     * belongs to another group, removes it from that group.
     * A selected button becomes the [selection] if the group has no selection yet, and is deselected if it already has one.
     */
    fun add(button: WToggleButton) {
        if (members.any { it === button }) return
        button.buttonGroup?.remove(button)
        members += button
        button.joinGroup(this)
        if (button.isSelected) {
            if (selection == null) {
                selection = button
            } else {
                withUpdating { button.isSelected = false }
            }
        }
        // Set the name after settling the selection (setting it while selected makes WinUI deselect the group's original selection instead)
        if (button is WRadioButton) {
            originalGroupNames[button] = button.groupName
            button.groupName = groupName
        }
    }

    /**
     * ButtonGroup.remove-style: removes [button] from the group. If it was the [selection], [selection] becomes null
     * (the button's own selection state stays as it is). Does nothing for a button that is not in the group.
     */
    fun remove(button: WToggleButton) {
        val index = members.indexOfFirst { it === button }
        if (index < 0) return
        members.removeAt(index)
        if (selection === button) selection = null
        button.leaveGroup()
        if (button is WRadioButton) {
            button.groupName = originalGroupNames.remove(button) ?: ""
        }
    }

    /** ButtonGroup.clearSelection-style: deselects all buttons and sets [selection] to null. */
    fun clearSelection() {
        val old = selection ?: return
        selection = null
        deselect(old)
    }

    /** ButtonGroup.isSelected-style: whether [button] is the [selection]. */
    fun isSelected(button: WToggleButton): Boolean = selection === button

    /**
     * ButtonGroup.setSelected-style: if [selected] is true, selects [button] and deselects the previous [selection].
     * Setting it to false does nothing (the group cannot deselect a selected button; use [clearSelection] for that).
     * Buttons that are not in the group are ignored.
     */
    fun setSelected(button: WToggleButton, selected: Boolean) {
        if (!selected || selection === button || members.none { it === button }) return
        val old = selection
        selection = button
        old?.let { deselect(it) }
        withUpdating { button.isSelected = true }
    }

    /**
     * Notification that a member's selection state changed (from [WToggleButton], before its ItemListeners). A
     * notification for which this returns false is delivered neither to the button's ItemListeners nor to actions.
     * - Selected: makes the button the [selection] and deselects the previous [selection] (its DESELECTED is delivered
     *   first)
     * - The [selection] was deselected: if another member has been selected (when a button with the same GroupName is
     *   selected, WinUI's RadioButton sends this one's Unchecked before the selected one's Checked;
     *   RadioButton::OnChecked), makes that one the [selection].
     *   Otherwise it was only an attempt to deselect, so selects it again and delivers neither the deselection nor the
     *   reselection notification (in Swing the state does not change)
     */
    internal fun onItemStateChanged(button: WToggleButton, stateChange: Int): Boolean {
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

    private fun deselect(button: WToggleButton) {
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

        /** A name for giving each group a different WinUI GroupName (with a prefix that does not collide with other GroupNames). */
        fun newGroupName(): String = "WinUI4K.ButtonGroup#" + counter.incrementAndGet()
    }
}
