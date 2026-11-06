package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * WindowEvent-like: an event that tells [WWindowListener] / [WWindowStateListener] / [WWindowFocusListener] that a window ([WFrame]) was
 * opened, is closing, was closed, was iconified, became active, and so on.
 * [getSource] (`source` in Kotlin) and [window] are the originating [WFrame], and [id] is the kind, such as [WINDOW_OPENED].
 * [oldState] / [newState] are [WFrame.NORMAL] / [WFrame.ICONIFIED] / [WFrame.MAXIMIZED_BOTH] for [WINDOW_STATE_CHANGED]
 * (0 for other events).
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's WindowEvent.
 */
class WWindowEvent @JvmOverloads constructor(
    source: WFrame,
    /** The kind of event. `getID()` in Java, as in AWT. */
    @get:JvmName("getID") val id: Int,
    /** The state before the change (equivalent to WindowEvent.getOldState). */
    val oldState: Int = 0,
    /** The state after the change (equivalent to WindowEvent.getNewState). */
    val newState: Int = 0,
) : EventObject(source) {
    /** The window that originated the event (equivalent to WindowEvent.getWindow). Same as [getSource]. */
    val window: WFrame get() = source as WFrame

    companion object {
        /** The smallest window event ID (the same value as AWT's WindowEvent.WINDOW_FIRST). */
        const val WINDOW_FIRST: Int = 200

        /** Shown for the first time (the same value as AWT's WindowEvent.WINDOW_OPENED). */
        const val WINDOW_OPENED: Int = 200

        /** The user tried to close it with the title bar × etc. (the same value as AWT's WindowEvent.WINDOW_CLOSING). */
        const val WINDOW_CLOSING: Int = 201

        /** Closed (the same value as AWT's WindowEvent.WINDOW_CLOSED). */
        const val WINDOW_CLOSED: Int = 202

        /** Iconified (the same value as AWT's WindowEvent.WINDOW_ICONIFIED). */
        const val WINDOW_ICONIFIED: Int = 203

        /** Restored from iconified (the same value as AWT's WindowEvent.WINDOW_DEICONIFIED). */
        const val WINDOW_DEICONIFIED: Int = 204

        /** Became active (the same value as AWT's WindowEvent.WINDOW_ACTIVATED). */
        const val WINDOW_ACTIVATED: Int = 205

        /** Stopped being active (the same value as AWT's WindowEvent.WINDOW_DEACTIVATED). */
        const val WINDOW_DEACTIVATED: Int = 206

        /** Gained the focus (the same value as AWT's WindowEvent.WINDOW_GAINED_FOCUS). */
        const val WINDOW_GAINED_FOCUS: Int = 207

        /** Lost the focus (the same value as AWT's WindowEvent.WINDOW_LOST_FOCUS). */
        const val WINDOW_LOST_FOCUS: Int = 208

        /** The state (normal, iconified, maximized) changed (the same value as AWT's WindowEvent.WINDOW_STATE_CHANGED). */
        const val WINDOW_STATE_CHANGED: Int = 209

        /** The largest window event ID (the same value as AWT's WindowEvent.WINDOW_LAST). */
        const val WINDOW_LAST: Int = 209
    }
}

/**
 * WindowListener-like: a listener passed to [WFrame.addWindowListener]. There is also [WWindowAdapter], which saves writing unused methods.
 */
interface WWindowListener : EventListener {
    /** Called only once, when first shown. */
    fun windowOpened(e: WWindowEvent)

    /**
     * Called when the user tries to close with the title bar ×, Alt+F4, etc., before [WFrame.defaultCloseOperation] is performed.
     * Not called when closed with [WFrame.dispose] (as in Swing).
     */
    fun windowClosing(e: WWindowEvent)

    /** Called after it is closed (disposed). */
    fun windowClosed(e: WWindowEvent)

    /** Called when iconified. */
    fun windowIconified(e: WWindowEvent)

    /** Called when restored from iconified. */
    fun windowDeiconified(e: WWindowEvent)

    /** Called when it becomes active. */
    fun windowActivated(e: WWindowEvent)

    /** Called when it stops being active. */
    fun windowDeactivated(e: WWindowEvent)
}

/** WindowStateListener-like: a listener passed to [WFrame.addWindowStateListener] for when the state changes between normal, iconified and maximized. */
fun interface WWindowStateListener : EventListener {
    /** Called when the state changes. [WWindowEvent.oldState] / [WWindowEvent.newState] tell the states before and after. */
    fun windowStateChanged(e: WWindowEvent)
}

/** WindowFocusListener-like: a listener passed to [WFrame.addWindowFocusListener] for when the window gains or loses the focus. */
interface WWindowFocusListener : EventListener {
    /** Called when it gains the focus. */
    fun windowGainedFocus(e: WWindowEvent)

    /** Called when it loses the focus. */
    fun windowLostFocus(e: WWindowEvent)
}

/**
 * WindowAdapter-like: a base class implementing all methods of [WWindowListener] / [WWindowStateListener] / [WWindowFocusListener] as empty.
 * Override only the methods you need.
 */
abstract class WWindowAdapter : WWindowListener, WWindowStateListener, WWindowFocusListener {
    override fun windowOpened(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowClosing(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowClosed(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowIconified(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowDeiconified(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowActivated(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowDeactivated(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowStateChanged(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowGainedFocus(e: WWindowEvent) {
        // Does nothing by default
    }

    override fun windowLostFocus(e: WWindowEvent) {
        // Does nothing by default
    }
}

/**
 * WindowConstants-like: constants for the action when the user tries to close, passed to [WFrame.defaultCloseOperation].
 * [WFrame] implements it, so Java can write `WFrame.EXIT_ON_CLOSE`, as in Swing.
 */
interface WWindowConstants {
    companion object {
        /** Do nothing (handle it yourself in [WWindowListener.windowClosing]; the same value as WindowConstants.DO_NOTHING_ON_CLOSE). */
        const val DO_NOTHING_ON_CLOSE: Int = 0

        /** Hide without closing (the same value as WindowConstants.HIDE_ON_CLOSE). */
        const val HIDE_ON_CLOSE: Int = 1

        /** Close and dispose (same as [WFrame.dispose]; the same value as WindowConstants.DISPOSE_ON_CLOSE). */
        const val DISPOSE_ON_CLOSE: Int = 2

        /** Close and exit the app ([WinUiUtilities.exit]; the same value as WindowConstants.EXIT_ON_CLOSE). */
        const val EXIT_ON_CLOSE: Int = 3
    }
}
