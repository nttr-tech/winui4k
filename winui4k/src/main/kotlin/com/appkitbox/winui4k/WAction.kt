package com.appkitbox.winui4k

import java.util.concurrent.CopyOnWriteArrayList

/**
 * javax.swing.Action-like: bundles a button's text, icon, tooltip, enabled state, and click handling in one place.
 * When passed to [WButton.action] or `WButton(action)`, the button copies values such as [NAME] into its own properties,
 * follows them whenever the values or [isEnabled] change, and calls [actionPerformed] on click. The same WAction may be
 * passed to multiple buttons.
 * Use it to share the same operation between a menu and a toolbar.
 *
 * Swing's Action uses java.beans.PropertyChangeListener for change notifications, but java.beans belongs to the
 * java.desktop module and this library avoids depending on java.desktop, so [WActionChangeListener] is used instead
 * (only the name of the changed key is notified; the old and new values are not included).
 *
 * It can be implemented from Java, but normally you extend [WAbstractAction], which already implements storing and
 * notifying for [getValue] and [putValue] (just like Swing's AbstractAction).
 */
interface WAction {
    /** Called when the button is clicked. The source of [e] is the clicked button. */
    fun actionPerformed(e: WActionEvent)

    /** The value associated with [key]. null if there is none. */
    fun getValue(key: String): Any?

    /** Associates a value with [key]. null removes the key. Notifies [WActionChangeListener] if the value changes. */
    fun putValue(key: String, value: Any?)

    /** Whether this action is enabled. While false, the bound buttons are disabled. */
    var isEnabled: Boolean

    /** Adds a listener that receives notifications of changes to the values or [isEnabled]. */
    fun addPropertyChangeListener(listener: WActionChangeListener)

    /** Removes a listener added with [addPropertyChangeListener]. Does nothing if it is not registered. */
    fun removePropertyChangeListener(listener: WActionChangeListener)

    companion object {
        /** The button's text ([WButtonBase.text]). The value is a String. Same key name as Swing's Action.NAME. */
        const val NAME: String = "Name"

        /** The button's icon ([WButton.icon]). The value is a [WIcon]. Same key name as Swing's Action.SMALL_ICON. */
        const val SMALL_ICON: String = "SmallIcon"

        /**
         * The button's tooltip ([WComponent.toolTipText]). The value is a String.
         * Same key name as Swing's Action.SHORT_DESCRIPTION.
         */
        const val SHORT_DESCRIPTION: String = "ShortDescription"

        /**
         * The button's mnemonic ([WButtonBase.mnemonic]). The value is an Int key code, the same as AWT's KeyEvent.VK_A
         * and so on (a Number or a Char is also accepted). Same key name as Swing's Action.MNEMONIC_KEY.
         */
        const val MNEMONIC_KEY: String = "MnemonicKey"

        /**
         * The button's action command ([WButtonBase.actionCommand]). The value is a String.
         * Same key name as Swing's Action.ACTION_COMMAND_KEY.
         */
        const val ACTION_COMMAND_KEY: String = "ActionCommandKey"

        /**
         * The key name used to report changes to [WAction.isEnabled] to [WActionChangeListener.actionChanged]
         * (same as Swing's "enabled").
         */
        const val ENABLED: String = "enabled"
    }
}

/**
 * A listener that receives changes to the values or enabled state of a [WAction] (replaces PropertyChangeListener).
 * Can be written as a lambda in Java.
 */
fun interface WActionChangeListener {
    /** The value of [key] (or [WAction.ENABLED]) of [action] changed. Read the new value from [action]. */
    fun actionChanged(action: WAction, key: String)
}

/**
 * AbstractAction-like: a base class that implements storing the values of a [WAction] and notifying their changes.
 * Extend it and implement only [actionPerformed]. In Java, it can be written as an anonymous class.
 * ```
 * new WButton(new WAbstractAction("Save", Symbol.SAVE) {
 *     public void actionPerformed(WActionEvent e) { save(); }
 * });
 * ```
 * Read and write the values from the UI thread (because the buttons are updated on the UI thread).
 */
abstract class WAbstractAction @JvmOverloads constructor(
    name: String? = null,
    icon: WIcon? = null,
) : WAction {
    private val values = HashMap<String, Any>()
    private val listeners = CopyOnWriteArrayList<WActionChangeListener>()
    private var enabled = true

    init {
        // There are no listeners yet in the constructor, so store the values directly (do not call the open putValue)
        if (name != null) values[WAction.NAME] = name
        if (icon != null) values[WAction.SMALL_ICON] = icon
    }

    override fun getValue(key: String): Any? = values[key]

    override fun putValue(key: String, value: Any?) {
        val old = if (value == null) values.remove(key) else values.put(key, value)
        if (old != value) fireChanged(key)
    }

    override var isEnabled: Boolean
        get() = enabled
        set(value) {
            if (enabled == value) return
            enabled = value
            fireChanged(WAction.ENABLED)
        }

    override fun addPropertyChangeListener(listener: WActionChangeListener) {
        listeners.add(listener)
    }

    override fun removePropertyChangeListener(listener: WActionChangeListener) {
        listeners.remove(listener)
    }

    private fun fireChanged(key: String) {
        for (listener in listeners) listener.actionChanged(this, key)
    }
}
