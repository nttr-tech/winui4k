package com.appkitbox.winui4k.table

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.TextTrimming
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WTable

/**
 * Like javax.swing.table.TableCellRenderer: creates the component that displays a cell.
 *
 * A WinUI component can be placed in only one location at a time, so unlike Swing,
 * **return a new component on every call** (reusing the same instance detaches it from the previous cell).
 * Rows are virtualized and reused, so this is called every time the row or value shown in a cell changes
 * (implemented with a column that overrides TableViewColumn.GenerateElementCore).
 */
fun interface TableCellRenderer {
    /**
     * Returns the component that displays [value] in the cell (row [row], column [column]; both are view indexes).
     * [isSelected] is whether the row is selected. [hasFocus] is always false (TableView focus is per row).
     */
    @Suppress("LongParameterList") // The parameters correspond 1:1 to Swing's TableCellRenderer
    fun getTableCellRendererComponent(
        table: WTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): WComponent
}

/**
 * Like javax.swing.table.DefaultTableCellRenderer: a renderer that displays the value's string in a label (WLabel).
 *
 * Override [getText] to change how the value is converted to a string, and [configure] to change how the label is
 * decorated. Strings that do not fit the cell width are truncated at the end (…).
 */
open class DefaultTableCellRenderer : TableCellRenderer {
    /** The horizontal alignment of the text. For numeric columns, [HorizontalAlignment.RIGHT] aligns the digits. */
    var horizontalAlignment: HorizontalAlignment = HorizontalAlignment.LEFT

    override fun getTableCellRendererComponent(
        table: WTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): WComponent {
        val label = WLabel(getText(value))
        label.verticalAlignment = VerticalAlignment.CENTER
        label.horizontalAlignment = horizontalAlignment
        label.textTrimming = TextTrimming.CHARACTER_ELLIPSIS
        label.setMargin(CELL_PADDING_HORIZONTAL, CELL_PADDING_VERTICAL, CELL_PADDING_HORIZONTAL, CELL_PADDING_VERTICAL)
        configure(label, value, isSelected, row, column)
        return label
    }

    /** The display string of [value]. The default is toString() (null becomes an empty string). */
    protected open fun getText(value: Any?): String = value?.toString() ?: ""

    /** Decorates the created label (text color, weight, and so on). Does nothing by default. */
    protected open fun configure(label: WLabel, value: Any?, isSelected: Boolean, row: Int, column: Int) {
        // No decoration by default
    }

    private companion object {
        /** Matches the default cell padding of TableViewTextColumn (Standard density: 8,4,8,4). */
        const val CELL_PADDING_HORIZONTAL = 8.0
        const val CELL_PADDING_VERTICAL = 4.0
    }
}
