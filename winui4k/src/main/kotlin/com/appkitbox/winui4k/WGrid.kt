package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * Row/column sizing (Microsoft.UI.Xaml.GridLength).
 * GridUnitType values are extracted from the winmd (Auto=0, Pixel=1, Star=2).
 */
class GridLength private constructor(
    internal val value: Double,
    internal val unitType: Int,
) {
    companion object {
        /** Sized to fit its content (Auto). */
        @JvmField
        val AUTO = GridLength(0.0, 0)

        /** A fixed pixel size. */
        @JvmStatic
        fun pixel(value: Double) = GridLength(value, 1)

        /** A proportional share of the remaining space (Star). [weight] is the share's weight. */
        @JvmStatic
        @JvmOverloads
        fun star(weight: Double = 1.0) = GridLength(weight, 2)
    }
}

/**
 * GridBagLayout-like JPanel: WinUI 3's Grid.
 * Define rows/columns with [addRow] / [addColumn], and place children at a cell (row, column) with [add].
 *
 * Differences when moving from GridBagLayout:
 * - The position in [add] is in the order row, column, the reverse of GridBagConstraints' `gridx` (column) and `gridy` (row). Copying them in the order `gridx, gridy`
 *   swaps rows and columns (in Kotlin you can write them with names, `add(c, row = gridy, column = gridx)`). `gridwidth` / `gridheight` are
 *   columnSpan / rowSpan.
 * - `weightx` / `weighty` are given to columns and rows, not to children (pass [GridLength.star] to [addColumn] / [addRow]). Create the rows and columns first.
 * - `insets` is expressed with the child's [WComponent.setMargin] (in the order left, top, right, bottom), and `fill` / `anchor` with the child's [WComponent.horizontalAlignmentInParent] /
 *   [WComponent.verticalAlignmentInParent]. There is no `ipadx` / `ipady`.
 * - It is not a layout manager, so it cannot be passed to `setLayout` (it is the panel itself).
 */
class WGrid @JvmOverloads constructor(
    rowSpacing: Double = 0.0,
    columnSpacing: Double = 0.0,
) : WContainer(
    Activation.composeDefault(XamlInterop.CLS_Grid, XamlInterop.IID_IGridFactory),
) {
    private val rowDefinitions: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IGrid_get_RowDefinitions)) // IVector<RowDefinition>
    }
    private val columnDefinitions: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IGrid_get_ColumnDefinitions)) // IVector<ColumnDefinition>
    }

    /** The spacing between rows (Grid.RowSpacing). */
    var rowSpacing: Double
        get() = inspectable.getDouble(XamlInterop.IGrid_get_RowSpacing)
        set(value) = inspectable.call(XamlInterop.IGrid_put_RowSpacing, value)

    /** The spacing between columns (Grid.ColumnSpacing). */
    var columnSpacing: Double
        get() = inspectable.getDouble(XamlInterop.IGrid_get_ColumnSpacing)
        set(value) = inspectable.call(XamlInterop.IGrid_put_ColumnSpacing, value)

    init {
        if (rowSpacing > 0) this.rowSpacing = rowSpacing
        if (columnSpacing > 0) this.columnSpacing = columnSpacing
    }

    /** Adds a row (appends a RowDefinition to Grid.RowDefinitions). */
    @JvmOverloads
    fun addRow(height: GridLength = GridLength.AUTO) {
        val definition = Activation.activate(XamlInterop.CLS_RowDefinition, XamlInterop.IID_IRowDefinition)
        XamlStructs.putGridLength(definition, XamlInterop.IRowDefinition_put_Height, height.value, height.unitType)
        rowDefinitions.call(FoundationInterop.IVector_Append, definition.ptr)
        definition.release()
    }

    /** Adds a column (appends a ColumnDefinition to Grid.ColumnDefinitions). */
    @JvmOverloads
    fun addColumn(width: GridLength = GridLength.AUTO) {
        val definition = Activation.activate(XamlInterop.CLS_ColumnDefinition, XamlInterop.IID_IColumnDefinition)
        XamlStructs.putGridLength(definition, XamlInterop.IColumnDefinition_put_Width, width.value, width.unitType)
        columnDefinitions.call(FoundationInterop.IVector_Append, definition.ptr)
        definition.release()
    }

    /**
     * Adds a child placed in the cell ([row], [column]) (the Grid.SetRow / SetColumn attached properties).
     * The row comes first, the reverse of the order of GridBagConstraints' `gridx` (column) and `gridy` (row) ([rowSpan] is `gridheight`, and [columnSpan] is `gridwidth`).
     */
    @JvmOverloads
    fun add(component: WComponent, row: Int, column: Int, rowSpan: Int = 1, columnSpan: Int = 1) {
        statics.call(XamlInterop.IGridStatics_SetRow, component.frameworkElement.ptr, row)
        statics.call(XamlInterop.IGridStatics_SetColumn, component.frameworkElement.ptr, column)
        if (rowSpan != 1) statics.call(XamlInterop.IGridStatics_SetRowSpan, component.frameworkElement.ptr, rowSpan)
        if (columnSpan != 1) statics.call(XamlInterop.IGridStatics_SetColumnSpan, component.frameworkElement.ptr, columnSpan)
        add(component)
    }

    private companion object {
        /** Attached-property operations for Grid (IGridStatics). */
        val statics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Grid, XamlInterop.IID_IGridStatics) }
    }
}
