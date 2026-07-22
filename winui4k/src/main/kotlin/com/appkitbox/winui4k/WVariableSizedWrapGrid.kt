package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * FlowLayout-like JPanel: WinUI 3's VariableSizedWrapGrid.
 * Wraps children in cells of [itemWidth] × [itemHeight], and each child can span more than one
 * cell via the rowSpan / columnSpan parameters of [add].
 */
class WVariableSizedWrapGrid(
    itemWidth: Double = Double.NaN,
    itemHeight: Double = Double.NaN,
) : WContainer(
    Activation.activate(XamlInterop.CLS_VariableSizedWrapGrid, XamlInterop.IID_IVariableSizedWrapGrid),
) {
    /** The width of a single cell (VariableSizedWrapGrid.ItemWidth). */
    var itemWidth: Double = Double.NaN
        set(value) {
            field = value
            inspectable.call(XamlInterop.IVariableSizedWrapGrid_put_ItemWidth, value)
        }

    /** The height of a single cell (VariableSizedWrapGrid.ItemHeight). */
    var itemHeight: Double = Double.NaN
        set(value) {
            field = value
            inspectable.call(XamlInterop.IVariableSizedWrapGrid_put_ItemHeight, value)
        }

    /** The direction children are laid out in (VariableSizedWrapGrid.Orientation). Defaults to vertical. */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(XamlInterop.IVariableSizedWrapGrid_get_Orientation))
        set(value) = inspectable.call(XamlInterop.IVariableSizedWrapGrid_put_Orientation, value.native)

    /** The number of rows or columns before wrapping (VariableSizedWrapGrid.MaximumRowsOrColumns). -1 means unlimited. */
    var maximumRowsOrColumns: Int
        get() = inspectable.getInt(XamlInterop.IVariableSizedWrapGrid_get_MaximumRowsOrColumns)
        set(value) = inspectable.call(XamlInterop.IVariableSizedWrapGrid_put_MaximumRowsOrColumns, value)

    init {
        if (!itemWidth.isNaN()) this.itemWidth = itemWidth
        if (!itemHeight.isNaN()) this.itemHeight = itemHeight
    }

    /** Adds a child sized to span ([rowSpan] × [columnSpan]) cells (the RowSpan / ColumnSpan attached properties). */
    fun add(component: WComponent, rowSpan: Int, columnSpan: Int) {
        statics.call(XamlInterop.IVariableSizedWrapGridStatics_SetRowSpan, component.uiElement.ptr, rowSpan)
        statics.call(XamlInterop.IVariableSizedWrapGridStatics_SetColumnSpan, component.uiElement.ptr, columnSpan)
        add(component)
    }

    private companion object {
        /** Attached-property operations for VariableSizedWrapGrid (IVariableSizedWrapGridStatics). */
        val statics: ComPtr by lazy {
            Activation.factory(XamlInterop.CLS_VariableSizedWrapGrid, XamlInterop.IID_IVariableSizedWrapGridStatics)
        }
    }
}
