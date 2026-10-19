package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.lang.ref.WeakReference

/**
 * A shared helper that determines the width and height from the number of columns (columns) and rows (rows) of an input
 * field, in the same way as Swing. Held by [WTextField] (columns only) and [WTextPane] (columns and rows).
 *
 * The width is "columns × the width of 'm' + left and right padding (Padding) and border (BorderThickness)", and the
 * height is "rows × the height of one line + top and bottom padding and border" (DIP). The width of 'm' and the height of
 * one line are measured by drawing in a TextBlock with the current [WControl.font] ([TextMetrics]; Swing's column width
 * is FontMetrics.charWidth('m') and the row height is FontMetrics.getHeight()). Full-width characters such as Japanese
 * are often wider than 'm', so it is a guideline rather than a width that exactly fits that many characters.
 *
 * Measuring requires a displayed window (the default padding and border are also decided only after it is placed in a
 * window), so values set before placing are turned into the width and height when it is placed (Loaded) (until then the
 * wrapper is held by [WComponent.listenerAnchor]). When [WControl.font] changes, it is remeasured with [fontChanged]. If
 * [WComponent.width] / [WComponent.height] is set explicitly, that takes precedence, and inside a [WLayoutPanel] it is
 * passed to the layout manager as the preferred size ([preferredSize]) (the assigned size is decided by the layout
 * manager).
 * Setting it back to 0 removes the width or height decided by that value.
 */
internal class TextExtent(private val owner: WControl) {
    /** The number of columns (0 means the width is not decided by columns). A negative value throws [IllegalArgumentException] (same as Swing). */
    var columns: Int = 0
        set(value) {
            require(value >= 0) { "columns must be 0 or more: $value" }
            if (value == field) return
            field = value
            apply()
        }

    /** The number of rows (0 means the height is not decided by rows). A negative value throws [IllegalArgumentException] (same as Swing). */
    var rows: Int = 0
        set(value) {
            require(value >= 0) { "rows must be 0 or more: $value" }
            if (value == field) return
            field = value
            apply()
        }

    /** Whether the wrapper is being held until it can be measured (until placed in a window) (counted in [WComponent.listenerAnchor]). */
    private var pinned = false

    /** Whether Loaded has been subscribed for remeasuring. */
    private var loadedSubscribed = false

    /** Called when [WControl.font] changes. Remeasures. */
    fun fontChanged() {
        if (columns > 0 || rows > 0) apply()
    }

    /** The width direction (the width derived from [columns]). */
    private val widthAxis = Axis({ owner.width }, { owner.appliedWidth }, owner::applyWidth)

    /** The height direction (the height derived from [rows]). */
    private val heightAxis = Axis({ owner.height }, { owner.appliedHeight }, owner::applyHeight)

    /**
     * One direction, either width or height. [measured] is the value (DIP) derived from the number of columns (rows); NaN if
     * 0 or not measured yet. [explicit] is the size the user set explicitly, [applied] is the value last written to the
     * native Width / Height, and [write] is the function that writes it.
     */
    private class Axis(val explicit: () -> Double, val applied: () -> Double, val write: (Double) -> Unit) {
        var measured = Double.NaN
        private var previous = Double.NaN
        private var isOurs = true

        /** Before remeasuring, remembers whether the current Width / Height is the size this helper decided (or unset). */
        fun begin() {
            previous = measured
            val current = applied()
            isOurs = current.isNaN() || current == previous
        }

        /** Set back to 0. Removes the size this helper decided. */
        fun clear() {
            measured = Double.NaN
            if (explicit().isNaN() && !previous.isNaN() && applied() == previous) write(Double.NaN)
        }

        /** Measured. Sets it as the Width / Height if there is neither an explicit size nor a layout manager assignment. */
        fun set(value: Double) {
            measured = value
            if (explicit().isNaN() && isOurs) write(value)
        }

        /** The preferred size value of [size] with this direction's value replaced. */
        fun preferred(size: Double): Double = if (explicit().isNaN() && !measured.isNaN()) measured else size
    }

    /** The preferred size of [size] (the preferred size from the content) with the dimensions decided by columns and rows replaced. */
    fun preferredSize(size: WSize): WSize = WSize(widthAxis.preferred(size.width), heightAxis.preferred(size.height))

    /** Derives the width and height from [columns] and [rows], and sets them as the Width / Height if there is neither an explicit size nor a layout manager assignment. */
    private fun apply() {
        widthAxis.begin()
        heightAxis.begin()
        if (columns == 0) widthAxis.clear()
        if (rows == 0) heightAxis.clear()
        if (columns == 0 && rows == 0) {
            releasePin()
            owner.invalidateNaturalSize()
            return
        }
        subscribeLoaded()
        val m = TextMetrics.measure(owner, checkNotNull(owner.font), "m")
        if (m == null) {
            // Not placed in a window yet. Keep the wrapper alive until it is measured when placed
            if (!pinned) {
                pinned = true
                owner.listenerAnchor.listenerAdded()
            }
            return
        }
        val padding = XamlStructs.getThickness(owner.control, XamlInterop.IControl_get_Padding)
        val border = XamlStructs.getThickness(owner.control, XamlInterop.IControl_get_BorderThickness)
        if (columns > 0) widthAxis.set(columns * m.width + padding[0] + padding[2] + border[0] + border[2])
        if (rows > 0) heightAxis.set(rows * m.height + padding[1] + padding[3] + border[1] + border[3])
        releasePin()
        owner.invalidateNaturalSize()
    }

    private fun releasePin() {
        if (!pinned) return
        pinned = false
        owner.listenerAnchor.listenerRemoved()
    }

    /** Remeasures on Loaded (subscribing only once). The handler strongly captures neither the input field nor this helper (because it remains on the native side). */
    private fun subscribeLoaded() {
        if (loadedSubscribed) return
        loadedSubscribed = true
        val extent = WeakReference(this)
        owner.frameworkElement.addEventHandler(
            "WinUI4K.TextExtentLoaded",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> extent.get()?.onLoaded() }
    }

    private fun onLoaded() {
        if (columns > 0 || rows > 0) apply()
    }
}
