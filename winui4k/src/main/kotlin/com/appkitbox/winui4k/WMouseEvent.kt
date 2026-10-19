package com.appkitbox.winui4k

import java.util.EventListener
import kotlin.math.floor

/**
 * MouseEvent-style: an event that tells a [WMouseListener] / [WMouseMotionListener] about mouse operations on a component.
 *
 * The position ([x] / [y] / [point]) is an integer obtained by flooring the DIPs from the top-left of this component (WinUI's
 * logical unit; like the user-space coordinates of Swing's HiDPI, the value before the display scale is applied). Use
 * [preciseX] / [preciseY] when fractions are needed. [button] is the button whose state changed (the button pressed,
 * released or clicked; [NOBUTTON] for moves, drags, enters and exits). [clickCount] is the number of consecutive presses
 * within Windows' double-click time and area. [isPopupTrigger] is true when the right button is released, as in Swing on
 * Windows.
 *
 * Calling [consume] sets the native PointerRoutedEventArgs.Handled, so the event no longer propagates to parent elements.
 * However, the control's own handling (pressing a button, selecting in a text field, and so on) runs before the listeners
 * and is not stopped (WinUI calls the control's handling before handlers attached to the element).
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's MouseEvent.
 */
@Suppress("LongParameterList") // The parameters follow the same order as AWT's MouseEvent constructor
open class WMouseEvent @JvmOverloads constructor(
    source: WComponent,
    id: Int,
    `when`: Long,
    modifiersEx: Int,
    /** The horizontal position from the top-left of the component (DIP). */
    val preciseX: Double,
    /** The vertical position from the top-left of the component (DIP). */
    val preciseY: Double,
    /** The number of consecutive presses (equivalent to MouseEvent.getClickCount). 0 for moves, drags, enters and exits. */
    val clickCount: Int,
    /** Whether this opens a popup menu (equivalent to MouseEvent.isPopupTrigger). On Windows, when the right button is released. */
    val isPopupTrigger: Boolean,
    /** The button whose state changed ([NOBUTTON] / [BUTTON1] / [BUTTON2] / [BUTTON3]; extra buttons are 4 / 5). */
    val button: Int = NOBUTTON,
) : WInputEvent(source, id, `when`, modifiersEx) {
    /** The horizontal position from the top-left of the component (floored DIP integer; equivalent to MouseEvent.getX). */
    val x: Int get() = floor(preciseX).toInt()

    /** The vertical position from the top-left of the component (floored DIP integer; equivalent to MouseEvent.getY). */
    val y: Int get() = floor(preciseY).toInt()

    /** The position from the top-left of the component (equivalent to MouseEvent.getPoint). */
    val point: WPoint get() = WPoint(x, y)

    /**
     * Returns the modifier keys and buttons in AWT's old format (equivalent to MouseEvent.getModifiers). For press, release
     * and click, adds the BUTTONn_MASK of [button]; for drag, adds the BUTTONn_MASK of the buttons being held.
     */
    override val modifiers: Int
        get() {
            var result = keyMasksOf(modifiersEx)
            val buttons = if (button != NOBUTTON) maskOfButton(button) else modifiersEx
            if (buttons and BUTTON1_DOWN_MASK != 0) result = result or BUTTON1_MASK
            if (buttons and BUTTON2_DOWN_MASK != 0) result = result or BUTTON2_MASK
            if (buttons and BUTTON3_DOWN_MASK != 0) result = result or BUTTON3_MASK
            return result
        }

    private fun maskOfButton(button: Int): Int = if (button in 1..BUTTON3) getMaskForButton(button) else 0

    companion object {
        /** The minimum ID of mouse events (the same value as AWT's MouseEvent.MOUSE_FIRST). */
        const val MOUSE_FIRST: Int = 500

        /** The maximum ID of mouse events (the same value as AWT's MouseEvent.MOUSE_LAST). */
        const val MOUSE_LAST: Int = 507

        /** Clicked (the same value as AWT's MouseEvent.MOUSE_CLICKED). */
        const val MOUSE_CLICKED: Int = 500

        /** A button was pressed (the same value as AWT's MouseEvent.MOUSE_PRESSED). */
        const val MOUSE_PRESSED: Int = 501

        /** A button was released (the same value as AWT's MouseEvent.MOUSE_RELEASED). */
        const val MOUSE_RELEASED: Int = 502

        /** Moved without pressing a button (the same value as AWT's MouseEvent.MOUSE_MOVED). */
        const val MOUSE_MOVED: Int = 503

        /** Entered the component (the same value as AWT's MouseEvent.MOUSE_ENTERED). */
        const val MOUSE_ENTERED: Int = 504

        /** Exited the component (the same value as AWT's MouseEvent.MOUSE_EXITED). */
        const val MOUSE_EXITED: Int = 505

        /** Moved while holding a button (the same value as AWT's MouseEvent.MOUSE_DRAGGED). */
        const val MOUSE_DRAGGED: Int = 506

        /** The wheel was rotated (the same value as AWT's MouseEvent.MOUSE_WHEEL). */
        const val MOUSE_WHEEL: Int = 507

        /** No button (the same value as AWT's MouseEvent.NOBUTTON). */
        const val NOBUTTON: Int = 0

        /** The left button (the same value as AWT's MouseEvent.BUTTON1). */
        const val BUTTON1: Int = 1

        /** The middle button (the same value as AWT's MouseEvent.BUTTON2). */
        const val BUTTON2: Int = 2

        /** The right button (the same value as AWT's MouseEvent.BUTTON3). */
        const val BUTTON3: Int = 3
    }
}

/**
 * MouseWheelEvent-style: an event that tells a [WMouseWheelListener] about wheel rotation.
 * As in Swing, [wheelRotation] is the number of notches with positive meaning toward the user (scrolling down); it is
 * WinUI's MouseWheelDelta (positive away from the user, 120 per notch) with its sign inverted, divided by 120. A
 * horizontal wheel (tilt) is reported as a vertical wheel with Shift held, as in Swing on Windows ([isShiftDown] is true,
 * positive is right), and can also be distinguished with [isHorizontal].
 */
@Suppress("LongParameterList") // The parameters follow the same order as AWT's MouseWheelEvent constructor
class WMouseWheelEvent @JvmOverloads constructor(
    source: WComponent,
    id: Int,
    `when`: Long,
    modifiersEx: Int,
    preciseX: Double,
    preciseY: Double,
    clickCount: Int,
    isPopupTrigger: Boolean,
    /** The scroll type ([WHEEL_UNIT_SCROLL] / [WHEEL_BLOCK_SCROLL]; equivalent to MouseWheelEvent.getScrollType). */
    val scrollType: Int,
    /** The number of lines to scroll per notch (equivalent to MouseWheelEvent.getScrollAmount; Windows' "lines to scroll at a time"). */
    val scrollAmount: Int,
    /** The number of notches rotated (positive toward the user = scrolling down; equivalent to MouseWheelEvent.getWheelRotation). 0 for less than one notch. */
    val wheelRotation: Int,
    /** The amount of rotation (1 notch = 1.0, positive toward the user; equivalent to MouseWheelEvent.getPreciseWheelRotation). Fractional on high-resolution wheels. */
    val preciseWheelRotation: Double = wheelRotation.toDouble(),
    /** Whether this is a horizontal wheel (tilt). A WinUI4K addition (Swing reports it as a vertical wheel with Shift held). */
    val isHorizontal: Boolean = false,
) : WMouseEvent(source, id, `when`, modifiersEx, preciseX, preciseY, clickCount, isPopupTrigger) {
    /** The number of lines to scroll ([scrollAmount] × [wheelRotation]; equivalent to MouseWheelEvent.getUnitsToScroll). */
    val unitsToScroll: Int get() = scrollAmount * wheelRotation

    companion object {
        /** Scrolls by lines (the same value as AWT's MouseWheelEvent.WHEEL_UNIT_SCROLL). */
        const val WHEEL_UNIT_SCROLL: Int = 0

        /** Scrolls by pages (the same value as AWT's MouseWheelEvent.WHEEL_BLOCK_SCROLL). */
        const val WHEEL_BLOCK_SCROLL: Int = 1
    }
}

/**
 * MouseListener-style: the listener passed to [WComponent.addMouseListener]. [WMouseAdapter] lets you skip unused methods.
 */
interface WMouseListener : EventListener {
    /** Called when a button is pressed and released (clicked) on the component. */
    fun mouseClicked(e: WMouseEvent)

    /** Called when a button is pressed on the component. */
    fun mousePressed(e: WMouseEvent)

    /** Called when a button pressed on the component is released (called even if released outside the component). */
    fun mouseReleased(e: WMouseEvent)

    /** Called when the mouse enters the component. */
    fun mouseEntered(e: WMouseEvent)

    /** Called when the mouse exits the component. */
    fun mouseExited(e: WMouseEvent)
}

/** MouseMotionListener-style: the listener passed to [WComponent.addMouseMotionListener]. */
interface WMouseMotionListener : EventListener {
    /** Called when the mouse is moved while holding a button pressed on the component (called even outside the component). */
    fun mouseDragged(e: WMouseEvent)

    /** Called when the mouse is moved over the component without pressing a button. */
    fun mouseMoved(e: WMouseEvent)
}

/** MouseWheelListener-style: the listener passed to [WComponent.addMouseWheelListener] (can be written as a lambda in Java). */
fun interface WMouseWheelListener : EventListener {
    /** Called when the wheel is rotated over the component. */
    fun mouseWheelMoved(e: WMouseWheelEvent)
}

/**
 * MouseAdapter-style: a base class implementing all methods of [WMouseListener] / [WMouseMotionListener] /
 * [WMouseWheelListener] as empty. Override only the methods you need, and add it once per listener kind you use (as in
 * Swing, addMouseListener alone does not cause mouseDragged and the like to be called).
 */
abstract class WMouseAdapter : WMouseListener, WMouseMotionListener, WMouseWheelListener {
    override fun mouseClicked(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mousePressed(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mouseReleased(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mouseEntered(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mouseExited(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mouseDragged(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mouseMoved(e: WMouseEvent) {
        // Does nothing by default
    }

    override fun mouseWheelMoved(e: WMouseWheelEvent) {
        // Does nothing by default
    }
}
