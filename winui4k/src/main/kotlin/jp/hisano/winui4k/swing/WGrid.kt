package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winui.Abi
import jp.hisano.winui4k.winui.XamlStructs

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
        val AUTO = GridLength(0.0, 0)

        /** A fixed pixel size. */
        fun pixel(value: Double) = GridLength(value, 1)

        /** A proportional share of the remaining space (Star). [weight] is the share's weight. */
        fun star(weight: Double = 1.0) = GridLength(weight, 2)
    }
}

/**
 * GridBagLayout-like JPanel: WinUI 3's Grid.
 * Define rows/columns with [addRow] / [addColumn], and place children at a cell (row, column) with [add].
 */
class WGrid(
    rowSpacing: Double = 0.0,
    columnSpacing: Double = 0.0,
) : WContainer(
    Activation.composeDefault(Abi.CLS_Grid, Abi.IID_IGridFactory),
) {
    private val rowDefinitions: ComPtr by lazy {
        inspectable.getPtr(Abi.IGrid_get_RowDefinitions) // IVector<RowDefinition>
    }
    private val columnDefinitions: ComPtr by lazy {
        inspectable.getPtr(Abi.IGrid_get_ColumnDefinitions) // IVector<ColumnDefinition>
    }

    /** The spacing between rows (Grid.RowSpacing). */
    var rowSpacing: Double
        get() = inspectable.getDouble(Abi.IGrid_get_RowSpacing)
        set(value) = inspectable.call(Abi.IGrid_put_RowSpacing, value)

    /** The spacing between columns (Grid.ColumnSpacing). */
    var columnSpacing: Double
        get() = inspectable.getDouble(Abi.IGrid_get_ColumnSpacing)
        set(value) = inspectable.call(Abi.IGrid_put_ColumnSpacing, value)

    init {
        if (rowSpacing > 0) this.rowSpacing = rowSpacing
        if (columnSpacing > 0) this.columnSpacing = columnSpacing
    }

    /** Adds a row (appends a RowDefinition to Grid.RowDefinitions). */
    fun addRow(height: GridLength = GridLength.AUTO) {
        val definition = Activation.activate(Abi.CLS_RowDefinition).queryInterface(Abi.IID_IRowDefinition)
        XamlStructs.putGridLength(definition, Abi.IRowDefinition_put_Height, height.value, height.unitType)
        rowDefinitions.call(Abi.IVector_Append, definition.ptr)
        definition.release()
    }

    /** Adds a column (appends a ColumnDefinition to Grid.ColumnDefinitions). */
    fun addColumn(width: GridLength = GridLength.AUTO) {
        val definition = Activation.activate(Abi.CLS_ColumnDefinition).queryInterface(Abi.IID_IColumnDefinition)
        XamlStructs.putGridLength(definition, Abi.IColumnDefinition_put_Width, width.value, width.unitType)
        columnDefinitions.call(Abi.IVector_Append, definition.ptr)
        definition.release()
    }

    /** Adds a child placed at cell ([row], [column]) (the Grid.SetRow / SetColumn attached properties). */
    fun add(component: WComponent, row: Int, column: Int, rowSpan: Int = 1, columnSpan: Int = 1) {
        statics.call(Abi.IGridStatics_SetRow, component.frameworkElement.ptr, row)
        statics.call(Abi.IGridStatics_SetColumn, component.frameworkElement.ptr, column)
        if (rowSpan != 1) statics.call(Abi.IGridStatics_SetRowSpan, component.frameworkElement.ptr, rowSpan)
        if (columnSpan != 1) statics.call(Abi.IGridStatics_SetColumnSpan, component.frameworkElement.ptr, columnSpan)
        add(component)
    }

    private companion object {
        /** Attached-property operations for Grid (IGridStatics). */
        val statics: ComPtr by lazy { Activation.factory(Abi.CLS_Grid, Abi.IID_IGridStatics) }
    }
}
