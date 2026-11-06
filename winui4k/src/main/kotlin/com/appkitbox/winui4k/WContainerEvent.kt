package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * ContainerEvent-style: an event that tells a [WContainerListener] that a child was added to or removed from a [WContainer].
 * [getSource] (`source` in Kotlin) and [container] are the container whose children changed, [child] is the child added or removed,
 * and [id] is [COMPONENT_ADDED] or [COMPONENT_REMOVED].
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's ContainerEvent.
 */
class WContainerEvent(
    source: WContainer,
    /** The kind of event ([COMPONENT_ADDED] / [COMPONENT_REMOVED]). `getID()` in Java, as in AWT. */
    @get:JvmName("getID") val id: Int,
    /** The child that was added or removed (equivalent to ContainerEvent.getChild). */
    val child: WComponent,
) : EventObject(source) {
    /** The container whose children changed (equivalent to ContainerEvent.getContainer). Same as [getSource]. */
    val container: WContainer get() = source as WContainer

    /** The component the event originated from (equivalent to ComponentEvent.getComponent). Same as [getSource]. */
    val component: WComponent get() = source as WComponent

    companion object {
        /** The smallest ID of container events (same value as AWT's ContainerEvent.CONTAINER_FIRST). */
        const val CONTAINER_FIRST: Int = 300

        /** The largest ID of container events (same value as AWT's ContainerEvent.CONTAINER_LAST). */
        const val CONTAINER_LAST: Int = 301

        /** A child was added (same value as AWT's ContainerEvent.COMPONENT_ADDED). */
        const val COMPONENT_ADDED: Int = 300

        /** A child was removed (same value as AWT's ContainerEvent.COMPONENT_REMOVED). */
        const val COMPONENT_REMOVED: Int = 301
    }
}

/**
 * ContainerListener-style: the listener passed to [WContainer.addContainerListener]. [WContainerAdapter] also exists so that
 * you do not have to write the methods you do not use.
 */
interface WContainerListener : EventListener {
    /** Called after a child is added. */
    fun componentAdded(e: WContainerEvent)

    /** Called after a child is removed. */
    fun componentRemoved(e: WContainerEvent)
}

/** ContainerAdapter-style: a base class implementing all methods of [WContainerListener] as empty. Override only the methods you need. */
abstract class WContainerAdapter : WContainerListener {
    override fun componentAdded(e: WContainerEvent) {
        // Does nothing by default
    }

    override fun componentRemoved(e: WContainerEvent) {
        // Does nothing by default
    }
}
