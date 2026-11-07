package com.appkitbox.winui4k

import java.util.EventObject

/**
 * java.beans.PropertyChangeEvent-like: an event telling that the value of [propertyName] of [source] changed from [oldValue] to [newValue]
 * ("progress" / "state" of [WSwingWorker], etc.). java.beans is in the java.desktop module, which WinUI4K does not depend on, so
 * it has its own class of the same shape.
 */
class WPropertyChangeEvent(
    source: Any,
    /** The name of the changed value. */
    val propertyName: String,
    /** The value before the change. */
    val oldValue: Any?,
    /** The value after the change. */
    val newValue: Any?,
) : EventObject(source)

/** java.beans.PropertyChangeListener-like: a listener receiving a [WPropertyChangeEvent]. Can be written as a lambda in Java. */
fun interface WPropertyChangeListener {
    /** A value changed. */
    fun propertyChange(evt: WPropertyChangeEvent)
}
