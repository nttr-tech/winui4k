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
 * buttons go back to unselected. Clicking the selected button does not deselect it (as with Swing's JToggleButton).
 * When deselecting is attempted by a click or by [WToggleButton.isSelected] = false, the button's ItemListener first
 * receives DESELECTED and then immediately goes back to SELECTED (WinUI buttons change state before notifying, so the
 * notification cannot be stopped).
 *
 * WinUI's RadioButton is exclusive per parent (or per visual tree root when GroupName is set), so [add]ing a
 * [WRadioButton] gives it a unique [WRadioButton.groupName] dedicated to this group. Radio buttons of different groups
 * stay independent even under the same parent. [remove] restores the groupName it had before joining the group.
 *
 * There is no counterpart of Swing's `ButtonModel`, so [selection] / [isSelected] / [setSelected] deal with the buttons
 * themselves.
 *
 * Memory: the group and its buttons reference each other strongly (as in Swing). An ItemListener for the exclusion
 * control is attached internally to each button, but its native handler references only the button weakly through
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

    /** True while the group is changing a button's state. ItemListener notifications that arrive then are not subject to the exclusion control. */
    private var updating = false

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
        if (button is WRadioButton) {
            originalGroupNames[button] = button.groupName
            button.groupName = uniqueGroupName
        }
        button.joinGroup(this)
        if (button.isSelected) {
            if (selection == null) {
                selection = button
            } else {
                withUpdating { button.isSelected = false }
            }
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
     * Notification that a member's selection state changed (from the internal ItemListener of [WToggleButton]).
     * Makes the selected button the [selection] and deselects the previous [selection]. When the [selection] is
     * deselected (cancelled by a click), selects it again. WinUI's RadioButton delivers Unchecked for the old one after
     * Checked for the new one, and in that case [selection] is already the new one, so this does nothing.
     */
    internal fun onItemStateChanged(button: WToggleButton, stateChange: Int) {
        if (updating) return
        if (stateChange == WItemEvent.SELECTED) {
            if (selection === button) return
            val old = selection
            selection = button
            old?.let { deselect(it) }
        } else if (selection === button) {
            withUpdating { button.isSelected = true }
        }
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
        val uniqueGroupName: String get() = "WinUI4K.ButtonGroup#" + counter.incrementAndGet()
    }
}
