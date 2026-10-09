package com.appkitbox.winui4k.table

import kotlin.jvm.JvmSynthetic

/**
 * The sort cycle of a column (TableViewSortCycle). Each click on the column header switches in this order.
 * Values extracted from the winmd.
 */
enum class SortCycle(internal val native: Int) {
    /** Ascending → descending → ascending …. */
    ASCENDING_DESCENDING(0),

    /** Ascending → descending → unsorted …. */
    ASCENDING_DESCENDING_NONE(1),

    /** Descending → ascending → descending …. */
    DESCENDING_ASCENDING(2),

    /** Descending → ascending → unsorted …. */
    DESCENDING_ASCENDING_NONE(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): SortCycle = entries.first { it.native == native }
    }
}

/**
 * Like javax.swing.table.TableColumn: the display attributes of one column of a [com.appkitbox.winui4k.WTable]
 * (corresponds to WinUI 3's TableViewColumn).
 *
 * [modelIndex] specifies which column of the [TableModel] to show. The display order is determined by the position in
 * the [TableColumnModel] and is independent of the model's column order. Property changes are applied immediately to
 * the WTable showing the column.
 *
 * How a cell is displayed is determined in this order of precedence:
 * 1. [cellRenderer] (a component built in Kotlin / Java; implements TableViewColumn's GenerateElementCore)
 * 2. [cellTemplate] (the content of a XAML DataTemplate; `{Binding}` is the cell value; TableViewTemplateColumn)
 * 3. A check box if the type of the model column is Boolean, otherwise a string (TableViewTextColumn)
 *
 * A cell can be edited when both the model's [TableModel.isCellEditable] and [isEditable] are true.
 * The editor is [cellEditor] (if unspecified, the default for the column type: a check box for Boolean, otherwise a text box).
 */
open class TableColumn @JvmOverloads constructor(
    modelIndex: Int = 0,
    preferredWidth: Double = Double.NaN,
    cellRenderer: TableCellRenderer? = null,
    cellEditor: TableCellEditor? = null,
) {
    /** The kind of column change (determines how much of the showing WTable is updated). */
    internal enum class Change {
        /** Column attributes (header, width, freezing, and so on). Can be written directly to the native column. */
        PROPERTY,

        /** How cells are displayed (renderer, template, editor, model column). Recreates the native column. */
        CELL,
    }

    /** The change notification registered by the showing WTable (applied to the native TableViewColumn). */
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var changeListener: ((TableColumn, Change) -> Unit)? = null

    /** The [TableModel] column this column shows (the model index). */
    var modelIndex: Int = modelIndex
        set(value) {
            field = value
            fireChanged(Change.CELL)
        }

    /**
     * The column identifier, used to find columns with [com.appkitbox.winui4k.WTable.getColumn] and others. If unset,
     * [headerValue].
     */
    var identifier: Any? = null
        get() = field ?: headerValue

    /**
     * The value shown in the column header (TableViewColumn.Header). Besides strings, components (WComponent) can be shown.
     * If null, the model's column name ([TableModel.getColumnName]) is shown.
     */
    var headerValue: Any? = null
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * The content of the column header's DataTemplate (XAML; TableViewColumn.HeaderTemplate). `{Binding}` is the header
     * value. The default namespace (xmlns) is added automatically. If null, the default display is used.
     */
    var headerTemplate: String? = null
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * The tooltip shown when the mouse hovers over the column header (TableViewColumn.HeaderToolTip).
     * Besides strings, components (WComponent) can be shown (use a separate instance for each column). Null means none.
     */
    var headerToolTip: Any? = null
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * The model column shown in the cell tooltip (TableViewColumn.CellToolTipBinding).
     * The value of that column in each row becomes the tooltip. -1 means no tooltip.
     * To show the full text of long values truncated (…) by the column width, specify the column's own [modelIndex].
     */
    var cellToolTipColumn: Int = -1
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /** The preferred column width (in pixels; TableViewColumn.Width). NaN means the default width (120). */
    var preferredWidth: Double = preferredWidth
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * The current column width (in pixels). While shown, returns the actual width (TableViewColumn.ActualWidth).
     * Setting it changes [preferredWidth].
     */
    var width: Double
        get() = actualWidthProvider?.invoke() ?: preferredWidth
        set(value) {
            preferredWidth = value
        }

    /** The showing WTable returns the actual column width. */
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var actualWidthProvider: (() -> Double)? = null

    /** The minimum column width (in pixels; TableViewColumn.MinWidth). */
    var minWidth: Double = DEFAULT_MIN_WIDTH
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /** The maximum column width (in pixels; TableViewColumn.MaxWidth). */
    var maxWidth: Double = Double.POSITIVE_INFINITY
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /** Whether the column width can be changed by dragging the column header border (TableViewColumn.CanResize). */
    var isResizable: Boolean = true
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * Whether to freeze the column at the leading side (TableViewColumn.FrozenEdge = Leading). Frozen columns do not
     * move when scrolling horizontally. Only columns contiguous from the first column can be frozen; specifying it on a
     * column in the middle is ignored.
     */
    var isFrozen: Boolean = false
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /** Whether to show the column (TableViewColumn.Visibility). */
    var isVisible: Boolean = true
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * Whether this column can be sorted by clicking the column header (TableViewColumn.CanSort).
     * It can also be changed with [TableRowSorter.setSortable].
     */
    var isSortable: Boolean = true
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /** The sort order cycled by clicking the column header (TableViewColumn.SortCycle). */
    var sortCycle: SortCycle = SortCycle.ASCENDING_DESCENDING
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /** The showing WTable returns the current sort direction. */
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var sortOrderProvider: (() -> SortOrder)? = null

    /**
     * The current sort direction of this column (TableViewColumn.SortDirection). [SortOrder.UNSORTED] before it is
     * shown or when not sorted.
     */
    val sortOrder: SortOrder
        get() = sortOrderProvider?.invoke() ?: SortOrder.UNSORTED

    /**
     * Whether the cells of this column can be edited (the inverse of TableViewColumn.IsReadOnly).
     * If false, this column cannot be edited even if the model allows editing.
     */
    var isEditable: Boolean = true
        set(value) {
            field = value
            fireChanged(Change.PROPERTY)
        }

    /**
     * The renderer that creates the component displaying a cell. If null, [cellTemplate] or the default display for the
     * column type.
     */
    var cellRenderer: TableCellRenderer? = cellRenderer
        set(value) {
            field = value
            fireChanged(Change.CELL)
        }

    /**
     * The content of the cell's DataTemplate (XAML; TableViewTemplateColumn.CellTemplate). `{Binding}` is the cell value.
     * The default namespace (xmlns) is added automatically. Example: `<TextBlock Text="{Binding}" Foreground="Red" />`.
     * [cellRenderer] takes precedence if set.
     */
    var cellTemplate: String? = null
        set(value) {
            field = value
            fireChanged(Change.CELL)
        }

    /** The editor used to edit cells (TableViewColumn.CellEditingTemplate). If null, the default for the column type. */
    var cellEditor: TableCellEditor? = cellEditor
        set(value) {
            field = value
            fireChanged(Change.CELL)
        }

    private fun fireChanged(change: Change) {
        changeListener?.invoke(this, change)
    }

    override fun toString(): String = "TableColumn(modelIndex=$modelIndex, headerValue=$headerValue)"

    private companion object {
        /** The default value of TableViewColumn.MinWidth (TableView-spec.md). */
        const val DEFAULT_MIN_WIDTH = 20.0
    }
}
