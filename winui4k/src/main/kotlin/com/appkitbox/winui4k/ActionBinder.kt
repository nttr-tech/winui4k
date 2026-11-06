package com.appkitbox.winui4k

import java.lang.ref.WeakReference

/**
 * The action-facing side of a component that can be bound to a [WAction] ([WButtonBase] / [WMenuFlyoutItem]).
 * Each component holds one private implementation and passes it to [ActionBinder] (so that public classes do not
 * implement an internal interface). The implementation may strongly reference the component ([ActionBinder] reaches it only weakly from the action).
 */
internal interface ActionTarget {
    /** Copies the value of [action] to the component item for [key] (unsets it if action is null). Does nothing for an unknown key. */
    fun applyActionValue(action: WAction?, key: String)

    /** Adds a listener that calls the action on click (the same list as the component's addActionListener). */
    fun addActionClickListener(listener: WActionListener)

    /** Removes a listener added with [addActionClickListener]. */
    fun removeActionClickListener(listener: WActionListener)
}

/**
 * Shared part of AbstractButton.setAction's binding: copies the action's values to the component (in [keys] order), keeps
 * following every change of the action's values, and calls the action's actionPerformed on click. [WButtonBase] and [WMenuFlyoutItem] each hold one.
 *
 * The action reaches [target] only through a weak reference ([Sync]). Since only the component references [target], an unused component can be
 * collected even if the action is shared, and then removes itself from the action on the next notification. Use only from the UI thread.
 */
internal class ActionBinder(private val target: ActionTarget, private val keys: List<String>) {
    private var binding: Binding? = null

    /** Unbinds the current binding (change notifications and click calls). The component's items stay as they are. */
    fun detach() {
        val current = binding ?: return
        binding = null
        current.action.removePropertyChangeListener(current.sync)
        target.removeActionClickListener(current.clickListener)
    }

    /**
     * Copies all values of [action] and binds it. If null, puts every item into the "no value" state.
     * Call after [detach] and after replacing the component's action field (so that the mapped selection state change
     * is not written back to the previous action; same order as Swing's setAction).
     */
    fun attach(action: WAction?) {
        if (action == null) {
            for (key in keys) target.applyActionValue(null, key)
            return
        }
        for (key in keys) target.applyActionValue(action, key)
        val sync = Sync(target)
        val clickListener = WActionListener { e -> action.actionPerformed(e) }
        action.addPropertyChangeListener(sync)
        target.addActionClickListener(clickListener)
        binding = Binding(action, sync, clickListener)
    }

    /** A listener that copies to the component when the action changes. Holds the [ActionTarget] weakly (removes itself when collected). */
    private class Sync(target: ActionTarget) : WActionChangeListener {
        private val reference = WeakReference(target)

        override fun actionChanged(action: WAction, key: String) {
            val target = reference.get()
            if (target == null) {
                action.removePropertyChangeListener(this)
            } else {
                target.applyActionValue(action, key)
            }
        }
    }

    private class Binding(val action: WAction, val sync: WActionChangeListener, val clickListener: WActionListener)
}
