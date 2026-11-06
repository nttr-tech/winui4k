package com.appkitbox.winui4k

/**
 * ColorSelectionModel-like: a model that holds the color selected by a color picker ([WColorPicker]).
 * As with Swing's JColorChooser, color changes are received by adding [addChangeListener] to the model (not to the [WColorPicker]),
 * and the source of the [WChangeEvent] passed to listeners is the model. The implementation is [WDefaultColorSelectionModel].
 */
interface WColorSelectionModel {
    /** The selected color. */
    var selectedColor: WColor

    /** Adds a color change listener (adding the same listener twice makes it called twice). */
    fun addChangeListener(listener: WChangeListener)

    /** Removes one listener added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener)
}

/**
 * DefaultColorSelectionModel-like: the standard implementation of [WColorSelectionModel]. The default color is white (as in Swing).
 *
 * Setting [selectedColor] notifies listeners only when the color actually changes (compared with [WColor.equals]).
 * As with Swing's EventListenerList, listeners receive notifications starting from the last one added. Adding or removing during a notification still delivers to the list at the start of the notification.
 */
open class WDefaultColorSelectionModel @JvmOverloads constructor(color: WColor = WColor.WHITE) : WColorSelectionModel {
    private var color = color
    private val listeners = ArrayList<WChangeListener>()

    override var selectedColor: WColor
        get() = color
        set(value) {
            if (value == color) return
            color = value
            fireStateChanged()
        }

    override fun addChangeListener(listener: WChangeListener) {
        listeners += listener
    }

    override fun removeChangeListener(listener: WChangeListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index >= 0) listeners.removeAt(index)
    }

    /** The listeners currently added (in the order added; Swing's getChangeListeners). */
    fun getChangeListeners(): Array<WChangeListener> = listeners.toTypedArray()

    /** Passes one event with this model as source to all added listeners (starting from the last one added). */
    protected fun fireStateChanged() {
        if (listeners.isEmpty()) return
        val event = WChangeEvent(this)
        for (listener in listeners.asReversed().toList()) listener.stateChanged(event)
    }

    override fun toString(): String = "${javaClass.name}[selectedColor=$color]"
}
