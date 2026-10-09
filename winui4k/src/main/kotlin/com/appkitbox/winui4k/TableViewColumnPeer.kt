package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.TabularInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableCellEditor
import com.appkitbox.winui4k.table.TableCellRenderer
import com.appkitbox.winui4k.table.TableColumn
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableRowSorter

/** The interface through which [TableViewColumnPeer] reads the state of the table ([WTable]). */
internal interface TableViewColumnHost {
    val table: WTable
    val model: TableModel
    val rows: TableRowCollection
    val sorter: TableRowSorter<*>?

    /** The value type of the model column (Object if there is no model column). */
    fun columnClassOf(modelIndex: Int): Class<*>

    /** Looks up the row object for an item passed from native code (an owned reference) and releases the item. */
    fun itemOf(pointer: ComPtr): TableRowItem?

    /** Rebuilds the native column of [peer]. */
    fun rebuild(peer: TableViewColumnPeer)

    /** The view column index of [peer]. */
    fun viewColumnOf(peer: TableViewColumnPeer): Int
}

/** The native TableViewColumn for one column, together with the [TableColumn] it came from. */
internal class TableViewColumnPeer(val column: TableColumn, private val host: TableViewColumnHost) {
    /** ITableViewColumn. */
    val native: ComPtr

    /** The IUnknown address of the native column (used to look up the column from event arguments). */
    val identity: Long

    /** The outer object of the renderer column's COM aggregation (implements GenerateElementCore). */
    private var outer: KComObject? = null

    private val renderer: TableCellRenderer?

    init {
        val modelIndex = column.modelIndex
        val columnClass = host.columnClassOf(modelIndex)
        val path = TableRowItem.keyOf(modelIndex)
        renderer = column.cellRenderer ?: host.table.getDefaultRenderer(columnClass)
        val template = column.cellTemplate ?: BOOLEAN_TEMPLATE.takeIf { isBooleanClass(columnClass) }
        native = when {
            renderer != null -> createRendererColumn()
            template != null -> createTemplateColumn(TableViewXaml.valueTemplate(path, template))
            else -> createTextColumn(path)
        }
        identity = native.queryInterface(KComObject.IID_IUNKNOWN).let { unknown ->
            unknown.ptr.address.also { unknown.release() }
        }
        Hstring.use(path) { h -> native.call(TabularInterop.ITableViewColumn_put_SortMemberPath, h) }
        val editor = column.cellEditor ?: host.table.getDefaultEditor(columnClass)
            ?: BOOLEAN_EDITOR.takeIf { isBooleanClass(columnClass) }
            ?: TEXT_EDITOR.takeIf { renderer != null || template != null }
        if (editor != null) {
            val editingTemplate = TableViewXaml.createDataTemplate(editor.getCellEditorXaml(path))
            native.call(TabularInterop.ITableViewColumn_put_CellEditingTemplate, editingTemplate.ptr)
            editingTemplate.release()
        }
        applyProperties()
        applySortProperties()
        column.changeListener = { _, change ->
            if (change == TableColumn.Change.CELL) host.rebuild(this) else applyProperties()
        }
        column.actualWidthProvider = { native.getDouble(TabularInterop.ITableViewColumn_get_ActualWidth) }
        column.sortOrderProvider = { sortOrder() }
    }

    /** TableViewTextColumn: displays cells in a TextBlock and binds the value with Binding = [path]. */
    private fun createTextColumn(path: String): ComPtr {
        val textColumn = Activation.composeDefault(
            TabularInterop.CLS_TableViewTextColumn,
            TabularInterop.IID_ITableViewTextColumnFactory,
        )
        try {
            val binding = TableViewXaml.createBinding(path)
            textColumn.call(TabularInterop.ITableViewTextColumn_put_Binding, binding.ptr)
            binding.release()
            return textColumn.queryInterface(TabularInterop.IID_ITableViewColumn)
        } finally {
            textColumn.release()
        }
    }

    /** TableViewTemplateColumn: creates cells from the DataTemplate [content]. */
    private fun createTemplateColumn(content: String): ComPtr {
        val templateColumn = Activation.composeDefault(
            TabularInterop.CLS_TableViewTemplateColumn,
            TabularInterop.IID_ITableViewTemplateColumnFactory,
        )
        try {
            val template = TableViewXaml.createDataTemplate(content)
            templateColumn.call(TabularInterop.ITableViewTemplateColumn_put_CellTemplate, template.ptr)
            template.release()
            return templateColumn.queryInterface(TabularInterop.IID_ITableViewColumn)
        } finally {
            templateColumn.release()
        }
    }

    /**
     * Creates a column that "derives" from TableViewColumn through COM aggregation and whose GenerateElementCore
     * returns a cell (Border) wrapping the renderer's component (equivalent to C#'s `class MyColumn : TableViewColumn`).
     */
    private fun createRendererColumn(): ComPtr {
        val outerObject = KComObject("WinUI4K.TableViewRendererColumn")
            .addInterface(
                TabularInterop.IID_ITableViewColumnOverrides,
                listOf(
                    // vtbl[6] GetSortMemberPathCore(this, out HSTRING)
                    KComObject.Method(DESC_PTR) { args ->
                        Ffi.backend.memory.putPtr(args[1] as Ptr, 0, Hstring.of(TableRowItem.keyOf(column.modelIndex)))
                        KComObject.S_OK
                    },
                    // vtbl[7] GenerateElementCore(this, Object dataItem, out FrameworkElement)
                    KComObject.Method(DESC_PTR_PTR) { args ->
                        Ffi.backend.memory.putPtr(args[2] as Ptr, 0, createRendererCell().ptr)
                        KComObject.S_OK
                    },
                ),
            )
        val factory = Activation.factory(TabularInterop.CLS_TableViewColumn, TabularInterop.IID_ITableViewColumnFactory)
        try {
            return Ffi.backend.withScope { scope ->
                val inner = scope.allocate(8)
                val instance = scope.allocate(8)
                factory.call(TabularInterop.ITableViewColumnFactory_CreateInstance, outerObject.primary, inner, instance)
                outerObject.innerUnknown = ComPtr(Ffi.backend.memory.getPtr(inner, 0))
                ComPtr(Ffi.backend.memory.getPtr(instance, 0))
            }.also { outer = outerObject }
        } finally {
            factory.release()
        }
    }

    /**
     * One cell of a renderer column: creates a Border that inherits the row's DataContext and binds its Tag to
     * `{Binding c<column>}`. Whenever the DataContext (the displayed row) or the Tag (the cell value) changes, the
     * renderer recreates the component and places it in the cell.
     * Returns an owned reference to IFrameworkElement (passed as is to GenerateElementCore's out parameter).
     */
    private fun createRendererCell(): ComPtr {
        val border = Activation.activate(XamlInterop.CLS_Border, XamlInterop.IID_IBorder)
        val element = border.queryInterface(XamlInterop.IID_IFrameworkElement)
        val dependency = border.queryInterface(XamlInterop.IID_IDependencyObject)
        try {
            TableViewXaml.setBinding(dependency, TableViewXaml.tagProperty, TableRowItem.keyOf(column.modelIndex))
            // The handler does not hold the element but gets it from sender (holding it would create a reference cycle
            // between the element and the handler, so neither would be released)
            element.addEventHandler(
                "WinUI4K.TableViewCellDataContextChangedHandler",
                XamlInterop.IID_DataContextChangedHandler,
                XamlInterop.IFrameworkElement_add_DataContextChanged,
            ) { sender, _ -> renderCell(sender) }
            TableViewXaml.registerPropertyChangedCallback(dependency, TableViewXaml.tagProperty) { sender ->
                renderCell(sender)
            }
        } finally {
            dependency.release()
            border.release()
        }
        return element
    }

    /** Places in the cell [cell] (a Border) a component that the renderer draws from the DataContext row's value. */
    private fun renderCell(cell: Ptr) {
        val currentRenderer = renderer ?: return
        val element = ComPtr(cell).queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return
        val border = ComPtr(cell).queryInterfaceOrNull(XamlInterop.IID_IBorder)
        try {
            if (border == null) return
            val dataContext = element.getPtrOrNull(XamlInterop.IFrameworkElement_get_DataContext)
            val item = dataContext?.let { host.itemOf(it) }
            val modelIndex = column.modelIndex
            if (item == null || modelIndex !in 0 until host.model.getColumnCount()) {
                border.call(XamlInterop.IBorder_put_Child, null)
                return
            }
            val viewRow = host.table.convertRowIndexToView(item.modelRow)
            val component = currentRenderer.getTableCellRendererComponent(
                host.table,
                host.model.getValueAt(item.modelRow, modelIndex),
                viewRow >= 0 && host.table.isRowSelected(viewRow),
                false,
                viewRow,
                host.viewColumnOf(this),
            )
            border.call(XamlInterop.IBorder_put_Child, component.uiElement.ptr)
        } finally {
            border?.release()
            element.release()
        }
    }

    /** Writes the column attributes (header, width, frozen, visibility, editability, tooltip) to the native column. */
    fun applyProperties() {
        putHeader()
        if (column.headerTemplate == null) {
            native.call(TabularInterop.ITableViewColumn_put_HeaderTemplate, null)
        } else {
            val template = TableViewXaml.createDataTemplate(checkNotNull(column.headerTemplate))
            native.call(TabularInterop.ITableViewColumn_put_HeaderTemplate, template.ptr)
            template.release()
        }
        when (val toolTip = column.headerToolTip) {
            null -> native.call(TabularInterop.ITableViewColumn_put_HeaderToolTip, null)
            is WComponent -> native.call(TabularInterop.ITableViewColumn_put_HeaderToolTip, toolTip.uiElement.ptr)
            else -> {
                val boxed = PropertyValues.boxString(toolTip.toString())
                native.call(TabularInterop.ITableViewColumn_put_HeaderToolTip, boxed.ptr)
                boxed.release()
            }
        }
        val width = column.preferredWidth
        if (width.isNaN()) {
            XamlStructs.putGridLength(native, TabularInterop.ITableViewColumn_put_Width, 1.0, GRID_UNIT_AUTO)
        } else {
            XamlStructs.putGridLength(native, TabularInterop.ITableViewColumn_put_Width, width, GRID_UNIT_PIXEL)
        }
        native.call(TabularInterop.ITableViewColumn_put_MinWidth, column.minWidth)
        native.call(TabularInterop.ITableViewColumn_put_MaxWidth, column.maxWidth)
        native.putBool(TabularInterop.ITableViewColumn_put_CanResize, column.isResizable)
        native.call(
            TabularInterop.ITableViewColumn_put_FrozenEdge,
            if (column.isFrozen) TabularInterop.TableViewFrozenEdge_Leading else TabularInterop.TableViewFrozenEdge_None,
        )
        native.call(TabularInterop.ITableViewColumn_put_Visibility, if (column.isVisible) VISIBILITY_VISIBLE else VISIBILITY_COLLAPSED)
        native.putBool(TabularInterop.ITableViewColumn_put_IsReadOnly, !column.isEditable)
        native.call(TabularInterop.ITableViewColumn_put_SortCycle, column.sortCycle.native)
        val toolTipColumn = column.cellToolTipColumn
        if (toolTipColumn < 0) {
            native.call(TabularInterop.ITableViewColumn_put_CellToolTipBinding, null)
        } else {
            val binding = TableViewXaml.createBinding(TableRowItem.keyOf(toolTipColumn))
            native.call(TabularInterop.ITableViewColumn_put_CellToolTipBinding, binding.ptr)
            binding.release()
        }
        applySortProperties()
    }

    /** The column header: a string is used as is, a component as a UIElement, and null means the model column name. */
    private fun putHeader() {
        when (val header = column.headerValue) {
            is WComponent -> native.call(TabularInterop.ITableViewColumn_put_Header, header.uiElement.ptr)
            else -> {
                val text = header?.toString()
                    ?: column.modelIndex.takeIf { it in 0 until host.model.getColumnCount() }?.let { host.model.getColumnName(it) }
                    ?: ""
                val boxed = PropertyValues.boxString(text)
                native.call(TabularInterop.ITableViewColumn_put_Header, boxed.ptr)
                boxed.release()
            }
        }
    }

    /**
     * Writes whether sorting is allowed and the comparator (TableRowSorter settings and the column's isSortable) to
     * the native column.
     */
    fun applySortProperties() {
        val currentSorter = host.sorter
        val modelIndex = column.modelIndex
        native.putBool(
            TabularInterop.ITableViewColumn_put_CanSort,
            column.isSortable && (currentSorter == null || currentSorter.isSortable(modelIndex)),
        )
        @Suppress("UNCHECKED_CAST") // The app specifies a comparator that matches the column's value type
        val comparator = currentSorter?.getComparator(modelIndex) as Comparator<Any?>?
        if (comparator == null) {
            native.call(TabularInterop.ITableViewColumn_put_CustomSortComparer, null)
            return
        }
        val comparer = KComObject("WinUI4K.TableViewSortComparer")
            .addInterface(
                TabularInterop.IID_ITableViewSortComparer,
                listOf(
                    // vtbl[6] Compare(this, Object a, Object b, out i4)
                    KComObject.Method(DESC_PTR_PTR_PTR) { args ->
                        val a = host.rows.itemOf(args[1] as Ptr)
                        val b = host.rows.itemOf(args[2] as Ptr)
                        val result = if (a == null || b == null) {
                            0
                        } else {
                            comparator.compare(host.model.getValueAt(a.modelRow, modelIndex), host.model.getValueAt(b.modelRow, modelIndex))
                        }
                        Ffi.backend.memory.putInt(args[3] as Ptr, 0, result)
                        KComObject.S_OK
                    },
                ),
            )
        native.call(TabularInterop.ITableViewColumn_put_CustomSortComparer, comparer.primary)
        comparer.release()
    }

    /** The current sort direction (TableViewColumn.SortDirection). */
    fun sortOrder(): SortOrder = sortOrderOf(native.getInt(TabularInterop.ITableViewColumn_get_SortDirection))

    /** The column was removed from the table: detaches the change notification and releases the native reference. */
    fun dispose() {
        if (column.changeListener != null) {
            column.changeListener = null
            column.actualWidthProvider = null
            column.sortOrderProvider = null
        }
        native.release()
        outer?.release()
        outer = null
    }
}

// Values extracted from winmd, like TableViewGridLinesVisibility
// GridUnitType: Auto = 0, Pixel = 1 / Visibility: Visible = 0, Collapsed = 1
private const val GRID_UNIT_AUTO = 0
private const val GRID_UNIT_PIXEL = 1
private const val VISIBILITY_VISIBLE = 0
private const val VISIBILITY_COLLAPSED = 1

private val DESC_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
private val DESC_PTR_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

/** The default display for Boolean columns (a check box that cannot be operated). */
private const val BOOLEAN_TEMPLATE =
    "<CheckBox IsChecked=\"{Binding}\" IsHitTestVisible=\"False\" IsTabStop=\"False\" MinWidth=\"0\" " +
        "Margin=\"8,0,0,0\" VerticalAlignment=\"Center\" />"

/** The default editor for Boolean columns. */
private val BOOLEAN_EDITOR = TableCellEditor { path ->
    "<CheckBox IsChecked=\"{Binding $path, Mode=TwoWay, UpdateSourceTrigger=Explicit}\" MinWidth=\"0\" " +
        "Margin=\"8,0,0,0\" VerticalAlignment=\"Center\" />"
}

/** The default editor (a text box) for template columns and renderer columns. */
private val TEXT_EDITOR = TableCellEditor { path ->
    "<TextBox Text=\"{Binding $path, Mode=TwoWay, UpdateSourceTrigger=Explicit}\" MinHeight=\"0\" " +
        "VerticalAlignment=\"Center\" Padding=\"8,4,8,4\" />"
}

private fun isBooleanClass(type: Class<*>): Boolean =
    type == java.lang.Boolean::class.java || type == java.lang.Boolean.TYPE

/** Converts a TableView SortDirection (the winmd value) to a [SortOrder]. */
internal fun sortOrderOf(direction: Int): SortOrder = when (direction) {
    TabularInterop.SortDirection_Ascending -> SortOrder.ASCENDING
    TabularInterop.SortDirection_Descending -> SortOrder.DESCENDING
    else -> SortOrder.UNSORTED
}
