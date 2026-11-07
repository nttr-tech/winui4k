package com.appkitbox.winui4k

/**
 * ListCellRenderer-like: creates a component that shows one element of a [WList] / [WListBox] ([WList.cellRenderer]).
 *
 * A WinUI component can be placed in only one place at a time, so unlike Swing,
 * **return a new component on each call** (reusing the same instance fails, because it would be placed without being detached from the previous item).
 * Called each time the shown element changes (when an item is reused by scrolling, or an element is replaced), and when the element's selection state changes.
 * The selection color and border are drawn by WinUI's item, so there is no need to paint the background for [isSelected] (use it to change the text color or weight).
 */
fun interface WListCellRenderer<E> {
    /**
     * Returns a component that shows the element [value] at [index] of [list].
     * [isSelected] is whether the element is selected at the time of the call, and [cellHasFocus] whether that item has the keyboard focus at that time
     * (it is not called again just because the focus moved).
     */
    fun getListCellRendererComponent(list: WComponent, value: E, index: Int, isSelected: Boolean, cellHasFocus: Boolean): WComponent
}

/**
 * DefaultListCellRenderer-like: a renderer that shows elements with a [WLabel].
 *
 * The text is decided by [getText] (toString() by default; empty for null) and the icon by [getIcon] (by default the element if it is a [WIcon]; the text is then empty),
 * and the label decoration is changed by overriding [configure]. As with Swing's DefaultListCellRenderer,
 * items with icons are made like `object : WDefaultListCellRenderer() { override fun getIcon(value: Any?) = ... }`.
 *
 * Mapping from Swing overrides: write `setText(...)` after `super.getListCellRendererComponent(...)` in [getText], `setIcon(...)` in
 * [getIcon], and decoration such as `setForeground(...)` and `setFont(...)` in an override of [configure]. Changing the renderer itself (the label)
 * and returning `this`, as in Swing, does not work (the returned label is newly created on each call, so changes to the result of `super` do not carry over to the next item).
 */
open class WDefaultListCellRenderer : WListCellRenderer<Any?> {
    override fun getListCellRendererComponent(list: WComponent, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): WComponent {
        val label = WLabel(getText(value), getIcon(value))
        label.verticalAlignmentInParent = VerticalAlignment.CENTER
        configure(label, value, index, isSelected, cellHasFocus)
        return label
    }

    /** The display string of [value]. toString() by default (empty for null), and empty if the element is a [WIcon]. */
    protected open fun getText(value: Any?): String = if (value is WIcon) "" else value?.toString() ?: ""

    /** The icon for [value]. By default the element if it is a [WIcon], otherwise null. */
    protected open fun getIcon(value: Any?): WIcon? = value as? WIcon

    /** Decorates the created label (text color, weight, etc.). Does nothing by default. */
    protected open fun configure(label: WLabel, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
        // No decoration by default
    }
}
