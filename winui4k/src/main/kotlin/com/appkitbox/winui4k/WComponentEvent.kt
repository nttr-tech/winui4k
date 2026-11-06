package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * ComponentEvent-style: an event that tells a [WComponentListener] that a component's size, position or visibility changed.
 * [getSource] (`source` in Kotlin) and [component] are the component that changed, and [id] is [COMPONENT_MOVED] /
 * [COMPONENT_RESIZED] / [COMPONENT_SHOWN] / [COMPONENT_HIDDEN]. It is also the base of [WContainerEvent] (same as Swing).
 * In events of [WFrame.addComponentListener], [getSource] is that [WFrame] and [component] is its [WFrame.contentPane]
 * (Swing's JFrame is a Component, so getComponent is also the window, but WinUI4K's WFrame is not a [WComponent]).
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's ComponentEvent.
 */
open class WComponentEvent private constructor(
    source: Any,
    /** The kind of event. `getID()` in Java, as in AWT. */
    @get:JvmName("getID") val id: Int,
    @Transient private val target: WComponent,
) : EventObject(source) {
    /** Creates an event with [id] from [source] ([component] is also [source]). */
    constructor(source: WComponent, id: Int) : this(source, id, source)

    /** An event with [id] of the window [window] ([WFrame.addComponentListener]). [component] is [contentPane]. */
    internal constructor(window: WFrame, id: Int, contentPane: WComponent) : this(window as Any, id, contentPane)

    /**
     * The component the event originated from (equivalent to ComponentEvent.getComponent). Usually the same as [getSource];
     * for [WFrame] events, its [WFrame.contentPane].
     */
    val component: WComponent get() = target

    companion object {
        /** The smallest ID of component events (same value as AWT's ComponentEvent.COMPONENT_FIRST). */
        const val COMPONENT_FIRST: Int = 100

        /** The largest ID of component events (same value as AWT's ComponentEvent.COMPONENT_LAST). */
        const val COMPONENT_LAST: Int = 103

        /** The position changed (same value as AWT's ComponentEvent.COMPONENT_MOVED). */
        const val COMPONENT_MOVED: Int = 100

        /** The size changed (same value as AWT's ComponentEvent.COMPONENT_RESIZED). */
        const val COMPONENT_RESIZED: Int = 101

        /** It was shown (same value as AWT's ComponentEvent.COMPONENT_SHOWN). */
        const val COMPONENT_SHOWN: Int = 102

        /** It was hidden (same value as AWT's ComponentEvent.COMPONENT_HIDDEN). */
        const val COMPONENT_HIDDEN: Int = 103
    }
}

/**
 * ComponentListener-style: the listener passed to [WComponent.addComponentListener]. [WComponentAdapter] also exists so that
 * you do not have to write the methods you do not use.
 */
interface WComponentListener : EventListener {
    /** Called when the size changed. */
    fun componentResized(e: WComponentEvent)

    /** Called when the position relative to the parent changed. */
    fun componentMoved(e: WComponentEvent)

    /** Called when [WComponent.isVisible] became true. */
    fun componentShown(e: WComponentEvent)

    /** Called when [WComponent.isVisible] became false. */
    fun componentHidden(e: WComponentEvent)
}

/** ComponentAdapter-style: a base class implementing all methods of [WComponentListener] as empty. Override only the methods you need. */
abstract class WComponentAdapter : WComponentListener {
    override fun componentResized(e: WComponentEvent) {
        // Does nothing by default
    }

    override fun componentMoved(e: WComponentEvent) {
        // Does nothing by default
    }

    override fun componentShown(e: WComponentEvent) {
        // Does nothing by default
    }

    override fun componentHidden(e: WComponentEvent) {
        // Does nothing by default
    }
}
