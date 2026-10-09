package com.appkitbox.winui4k.ribbon

import java.util.EventObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Like java.beans.PropertyChangeEvent: the details of a change to a property of a ribbon model.
 *
 * [propertyName] is the Kotlin property name (`label`, `isChecked`, and so on; from Java, this corresponds to the
 * getter name without get / is).
 */
class RibbonPropertyChangeEvent(
    source: Any,
    /** The name of the property that changed. */
    val propertyName: String,
    /** The value before the change. */
    val oldValue: Any?,
    /** The value after the change. */
    val newValue: Any?,
) : EventObject(source) {
    override fun toString(): String = "RibbonPropertyChangeEvent($propertyName: $oldValue -> $newValue)"
}

/** Like java.beans.PropertyChangeListener: receives property change notifications from a model. */
fun interface RibbonPropertyChangeListener {
    /** Called when a property changes. */
    fun propertyChange(event: RibbonPropertyChangeEvent)
}

/**
 * The common base of ribbon models (the equivalent of RibbonSpace's ObservableObject and Swing's bean property change
 * notifications).
 *
 * Notifies [RibbonPropertyChangeListener]s only when a property actually changes (when it differs by equals).
 * Views ([com.appkitbox.winui4k.WRibbon] and others) subscribe to these notifications to update what they show, and
 * write the results of user operations (the selected tab, check states, input values, and so on) back to the model
 * (two-way sync).
 * Modify it only on the UI thread.
 */
abstract class RibbonObservable {
    private val listeners = mutableListOf<RibbonPropertyChangeListener>()

    /** Subscribes to property change notifications. */
    fun addPropertyChangeListener(listener: RibbonPropertyChangeListener) {
        listeners += listener
    }

    /** Removes a listener registered with [addPropertyChangeListener]. */
    fun removePropertyChangeListener(listener: RibbonPropertyChangeListener) {
        listeners -= listener
    }

    /** The registered listeners. */
    fun getPropertyChangeListeners(): List<RibbonPropertyChangeListener> = listeners.toList()

    /** Notifies a change of [propertyName]. Does nothing if [oldValue] and [newValue] are equal. */
    protected fun firePropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {
        if (oldValue == newValue || listeners.isEmpty()) return
        val event = RibbonPropertyChangeEvent(this, propertyName, oldValue, newValue)
        for (listener in listeners.toList()) listener.propertyChange(event)
    }

    /**
     * A delegate for a property that notifies changes (`var label: String? by observable(null)`).
     * [coerce] adjusts the assigned value (clamping to a range, for example). [onChanged] is called when the value changes.
     */
    protected fun <T> observable(
        initial: T,
        coerce: ((T) -> T)? = null,
        onChanged: ((old: T, new: T) -> Unit)? = null,
    ): ReadWriteProperty<RibbonObservable, T> = ObservableProperty(if (coerce == null) initial else coerce(initial), coerce, onChanged)

    private class ObservableProperty<T>(
        private var value: T,
        private val coerce: ((T) -> T)?,
        private val onChanged: ((old: T, new: T) -> Unit)?,
    ) : ReadWriteProperty<RibbonObservable, T> {
        override fun getValue(thisRef: RibbonObservable, property: KProperty<*>): T = value

        override fun setValue(thisRef: RibbonObservable, property: KProperty<*>, value: T) {
            val coerced = if (coerce == null) value else coerce(value)
            val old = this.value
            if (old == coerced) return
            this.value = coerced
            thisRef.firePropertyChange(property.name, old, coerced)
            onChanged?.invoke(old, coerced)
        }
    }
}
