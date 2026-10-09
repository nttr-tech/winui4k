package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Converts cell values written by bindings during editing to the type of the model's column.
 * Text boxes write strings, check boxes write Booleans, and NumberBox / Slider write Doubles, so the values are converted
 * to the type of [TableModel.getColumnClass] (Integer / Long / Double / Boolean / BigDecimal and so on) before being
 * passed to [TableModel.setValueAt].
 */
internal object TableCellValues {
    /** A marker for input that cannot be converted (such as a non-numeric string in a numeric column). */
    val INVALID = Any()

    /**
     * Converts [value] to the column type [type]. Returns [INVALID] if it cannot be converted.
     * A number with a fractional part cannot be written to an integer column (it is not truncated; [INVALID] is returned).
     * In columns other than String / Object, null stays null.
     */
    @Suppress("CyclomaticComplexMethod") // Enumerates the conversion for each column type in one when
    fun convert(value: Any?, type: Class<*>): Any? {
        if (value == null) return null
        val text = value.toString().trim()
        val whole = (value as? Number)?.let { number -> number.toLong().takeIf { it.toDouble() == number.toDouble() } }
        val number = value as? Number
        return when (type) {
            String::class.java -> value.toString()
            java.lang.Integer::class.java, Integer.TYPE ->
                whole?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt() ?: text.toIntOrNull()
            java.lang.Long::class.java, java.lang.Long.TYPE -> whole ?: text.toLongOrNull()
            java.lang.Short::class.java, java.lang.Short.TYPE ->
                whole?.takeIf { it in Short.MIN_VALUE..Short.MAX_VALUE }?.toShort() ?: text.toShortOrNull()
            java.lang.Byte::class.java, java.lang.Byte.TYPE ->
                whole?.takeIf { it in Byte.MIN_VALUE..Byte.MAX_VALUE }?.toByte() ?: text.toByteOrNull()
            java.lang.Double::class.java, java.lang.Double.TYPE -> number?.toDouble() ?: text.toDoubleOrNull()
            java.lang.Float::class.java, java.lang.Float.TYPE -> number?.toFloat() ?: text.toFloatOrNull()
            BigDecimal::class.java -> text.toBigDecimalOrNull()
            BigInteger::class.java -> whole?.let { BigInteger.valueOf(it) } ?: text.toBigIntegerOrNull()
            java.lang.Boolean::class.java, java.lang.Boolean.TYPE ->
                value as? Boolean ?: text.lowercase().toBooleanStrictOrNull()
            else -> value
        } ?: INVALID
    }
}

/**
 * The gateway through which [TableRowItem] reads and writes model values (implemented by WTableView).
 * Rows and columns are model indexes.
 */
internal interface TableRowValues {
    fun columnCount(): Int

    fun valueAt(row: Int, column: Int): Any?

    /** A binding (committing an edit) wrote [value] to column [column]. */
    fun valueWritten(item: TableRowItem, column: Int, value: Any?)
}

/**
 * A Kotlin-implemented COM object representing one row of a TableView (one item of ItemsSource).
 *
 * Implements IObservableMap<String, Object> and returns the model's cell values under the keys "c<column>" (e.g. "c0").
 * When the source has IMap<String, Object>, XAML bindings Lookup / Insert with the path name as the key
 * (MapPropertyAccess) and learn about per-key changes through MapChanged, so `{Binding c0}` becomes the value of the
 * model's column 0. Values are read from the model each time, so the row object does not copy them.
 * Row identity (how TableView tracks selection and focus) is determined by the identity of the COM object.
 */
internal class TableRowItem(private val values: TableRowValues, var modelRow: Int) {
    /** MapChanged subscriptions (token → AddRef'd handler). */
    private val handlers = LinkedHashMap<Long, ComPtr>()
    private var nextToken = 1L

    val comObject: KComObject = KComObject("WinUI4K.TableRow")
        .addInterface(
            FoundationInterop.IID_IMap_String_Object,
            listOf(
                // vtbl[6] Lookup(this, HSTRING key, out IInspectable)
                KComObject.Method(DESC_PTR_PTR) { args ->
                    val column = columnOf(Hstring.read(args[1] as Ptr))
                    if (column < 0) return@Method E_BOUNDS
                    val boxed = PropertyValues.boxAny(values.valueAt(modelRow, column))
                    // Pass the newly created boxed reference (count 1) to out as is (the caller Releases it)
                    Ffi.backend.memory.putPtr(args[2] as Ptr, 0, boxed?.ptr ?: Ptr.NULL)
                    KComObject.S_OK
                },
                // vtbl[7] get_Size(this, out UINT32)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putInt(args[1] as Ptr, 0, values.columnCount())
                    KComObject.S_OK
                },
                // vtbl[8] HasKey(this, HSTRING key, out boolean)
                KComObject.Method(DESC_PTR_PTR) { args ->
                    val hasKey = columnOf(Hstring.read(args[1] as Ptr)) >= 0
                    Ffi.backend.memory.putByte(args[2] as Ptr, 0, if (hasKey) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetView(this, out IMapView) — not used by bindings
                KComObject.Method(DESC_PTR) { E_NOTIMPL },
                // vtbl[10] Insert(this, HSTRING key, IInspectable value, out boolean replaced)
                KComObject.Method(DESC_PTR_PTR_PTR) { args ->
                    val column = columnOf(Hstring.read(args[1] as Ptr))
                    if (column < 0) return@Method E_BOUNDS
                    val valuePtr = args[2] as Ptr
                    val value = if (valuePtr.isNull) null else PropertyValues.unboxAny(ComPtr(valuePtr))
                    Ffi.backend.memory.putByte(args[3] as Ptr, 0, 1)
                    values.valueWritten(this, column, value)
                    KComObject.S_OK
                },
                // vtbl[11] Remove(this, HSTRING) / vtbl[12] Clear(this) — columns cannot be removed
                KComObject.Method(DESC_PTR) { E_NOTIMPL },
                KComObject.Method(DESC_THIS) { E_NOTIMPL },
            ),
        )
        .addInterface(
            FoundationInterop.IID_IObservableMap_String_Object,
            listOf(
                // vtbl[6] add_MapChanged(this, MapChangedEventHandler<String, Object>, out token)
                KComObject.Method(DESC_PTR_PTR) { args ->
                    val handler = ComPtr(args[1] as Ptr)
                    handler.addRef()
                    val token = nextToken++
                    handlers[token] = handler
                    Ffi.backend.memory.putLong(args[2] as Ptr, 0, token)
                    KComObject.S_OK
                },
                // vtbl[7] remove_MapChanged(this, token)
                KComObject.Method(DESC_I64) { args ->
                    handlers.remove(args[1] as Long)?.release()
                    KComObject.S_OK
                },
            ),
        )
        .enableWeakReferences()

    /** Notifies the bindings showing column [column] that its value changed (MapChanged). */
    fun raiseValueChanged(column: Int) {
        if (handlers.isEmpty()) return
        val args = mapChangedArgs(keyOf(column))
        try {
            val sender = comObject.pointerFor(FoundationInterop.IID_IObservableMap_String_Object)
            for (handler in handlers.values.toList()) {
                // MapChangedEventHandler.Invoke(this, sender, args) — vtbl[3]
                handler.rawCall(3, DESC_INVOKE, sender, args.primary)
            }
        } finally {
            args.release()
        }
    }

    /** Notifies that the values of all columns changed. */
    fun raiseAllValuesChanged() {
        for (column in 0 until values.columnCount()) raiseValueChanged(column)
    }

    /**
     * When the row is removed from the table: releases the reference created on the Kotlin side (it is collected once
     * the native references are gone).
     */
    fun dispose() {
        comObject.release()
    }

    companion object {
        private const val KEY_PREFIX = "c"

        /** The binding path of column [column] (a model index). */
        fun keyOf(column: Int): String = "$KEY_PREFIX$column"

        /** The column number of the key "c<column>". -1 if it is not in that format. */
        private fun TableRowItem.columnOf(key: String): Int {
            if (!key.startsWith(KEY_PREFIX)) return -1
            val column = key.substring(KEY_PREFIX.length).toIntOrNull() ?: return -1
            return if (column in 0 until values.columnCount()) column else -1
        }

        /** Creates an IMapChangedEventArgs<String> (CollectionChange = ItemChanged, Key = [key]). */
        private fun mapChangedArgs(key: String): KComObject = KComObject("WinUI4K.MapChangedEventArgs")
            .addInterface(
                FoundationInterop.IID_IMapChangedEventArgs_String,
                listOf(
                    // vtbl[6] get_CollectionChange(this, out CollectionChange)
                    KComObject.Method(DESC_PTR) { args ->
                        Ffi.backend.memory.putInt(args[1] as Ptr, 0, FoundationInterop.CollectionChange_ItemChanged)
                        KComObject.S_OK
                    },
                    // vtbl[7] get_Key(this, out HSTRING) — passes an HSTRING that the caller frees
                    KComObject.Method(DESC_PTR) { args ->
                        Ffi.backend.memory.putPtr(args[1] as Ptr, 0, Hstring.of(key))
                        KComObject.S_OK
                    },
                ),
            )
    }
}

/**
 * The row collection passed to TableView's ItemsSource: a Kotlin-implemented IObservableVector<Object>
 * (+ IVector<Object> / IIterable<Object>). Its items are [TableRowItem]s, and their order always matches the model's row
 * order (sorting, filtering, and grouping in the view are projected separately by TableViewSource).
 * Changes from the TableView side (SetAt / Append and so on) are not accepted.
 */
internal class TableRowCollection(private val values: TableRowValues) {
    /** The items in model row order. */
    private val items = mutableListOf<TableRowItem>()

    /** COM identity (the address of IUnknown) → item. Used for IndexOf and for looking up items passed from native code. */
    private val byAddress = HashMap<Long, TableRowItem>()

    /** VectorChanged subscriptions (token → AddRef'd handler). */
    private val handlers = LinkedHashMap<Long, ComPtr>()
    private var nextToken = 1L

    val size: Int
        get() = items.size

    operator fun get(index: Int): TableRowItem = items[index]

    val comObject: KComObject = KComObject("WinUI4K.TableRowCollection")
        .addInterface(
            FoundationInterop.IID_IVector_Object,
            listOf(
                getAtMethod(), // vtbl[6] GetAt(this, UINT32, out IInspectable)
                sizeMethod(), // vtbl[7] get_Size(this, out UINT32)
                // vtbl[8] GetView(this, out IVectorView<Object>)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createView().primary)
                    KComObject.S_OK
                },
                indexOfMethod(), // vtbl[9] IndexOf(this, IInspectable, out UINT32, out boolean)
                KComObject.Method(DESC_I32_PTR) { E_NOTIMPL }, // vtbl[10] SetAt
                KComObject.Method(DESC_I32_PTR) { E_NOTIMPL }, // vtbl[11] InsertAt
                KComObject.Method(DESC_I32) { E_NOTIMPL }, // vtbl[12] RemoveAt
                KComObject.Method(DESC_PTR) { E_NOTIMPL }, // vtbl[13] Append
                KComObject.Method(DESC_THIS) { E_NOTIMPL }, // vtbl[14] RemoveAtEnd
                KComObject.Method(DESC_THIS) { E_NOTIMPL }, // vtbl[15] Clear
                getManyMethod(), // vtbl[16] GetMany(this, UINT32 start, UINT32 capacity, IInspectable*, out UINT32)
                KComObject.Method(DESC_I32_PTR) { E_NOTIMPL }, // vtbl[17] ReplaceAll
            ),
        )
        .addInterface(
            FoundationInterop.IID_IIterable_Object,
            listOf(
                // vtbl[6] First(this, out IIterator<Object>)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )
        .addInterface(
            FoundationInterop.IID_IObservableVector_Object,
            listOf(
                // vtbl[6] add_VectorChanged(this, VectorChangedEventHandler<Object>, out token)
                KComObject.Method(DESC_PTR_PTR) { args ->
                    val handler = ComPtr(args[1] as Ptr)
                    handler.addRef()
                    val token = nextToken++
                    handlers[token] = handler
                    Ffi.backend.memory.putLong(args[2] as Ptr, 0, token)
                    KComObject.S_OK
                },
                // vtbl[7] remove_VectorChanged(this, token)
                KComObject.Method(DESC_I64) { args ->
                    handlers.remove(args[1] as Long)?.release()
                    KComObject.S_OK
                },
            ),
        )
        .enableWeakReferences()

    /**
     * Inserts items for [count] rows starting at model row [row] and notifies. The row numbers of the following items
     * are shifted too.
     * Inserting and notifying are repeated one row at a time so that, during a notification, the collection seen by
     * subscribers matches the notification. Large insertions are combined into a single Reset notification.
     */
    fun insert(row: Int, count: Int) {
        val bulk = count > BULK_CHANGE_THRESHOLD
        for (i in 0 until count) {
            val item = TableRowItem(values, row + i)
            items.add(row + i, item)
            byAddress[item.comObject.primary.address] = item
            if (!bulk) {
                renumber(row + i + 1)
                raiseVectorChanged(FoundationInterop.CollectionChange_ItemInserted, row + i)
            }
        }
        if (bulk) {
            renumber(row + count)
            raiseVectorChanged(FoundationInterop.CollectionChange_Reset, 0)
        }
    }

    /**
     * Removes the items for [count] rows starting at model row [row] and notifies. Large removals are combined into a
     * single Reset notification.
     */
    fun remove(row: Int, count: Int) {
        val bulk = count > BULK_CHANGE_THRESHOLD
        // Removing from the end keeps the notified positions valid in the order of removal
        for (index in row + count - 1 downTo row) {
            val item = items.removeAt(index)
            byAddress.remove(item.comObject.primary.address)
            if (!bulk) {
                renumber(index)
                raiseVectorChanged(FoundationInterop.CollectionChange_ItemRemoved, index)
            }
            item.dispose()
        }
        if (bulk) {
            renumber(row)
            raiseVectorChanged(FoundationInterop.CollectionChange_Reset, 0)
        }
    }

    /**
     * Notifies that the values of model row [row] changed. If [column] is -1, all columns.
     * If [reshape] is true, also notifies it as a change to the row collection (ItemChanged), making TableViewSource
     * redo the sorting, filtering, and grouping of that row.
     */
    fun update(row: Int, column: Int, reshape: Boolean) {
        val item = items[row]
        if (column < 0) item.raiseAllValuesChanged() else item.raiseValueChanged(column)
        if (reshape) raiseVectorChanged(FoundationInterop.CollectionChange_ItemChanged, row)
    }

    /** Releases all items (when detached from the table). */
    fun clear() {
        if (items.isEmpty()) return
        for (item in items) item.dispose()
        items.clear()
        byAddress.clear()
        raiseVectorChanged(FoundationInterop.CollectionChange_Reset, 0)
    }

    /** Looks up an item passed from native code (a pointer to any interface). Returns null if it is not a row object. */
    fun itemOf(pointer: Ptr): TableRowItem? {
        if (pointer.isNull) return null
        val unknown = ComPtr(pointer).queryInterfaceOrNull(KComObject.IID_IUNKNOWN) ?: return null
        return try {
            byAddress[unknown.ptr.address]
        } finally {
            unknown.release()
        }
    }

    /** Realigns the row numbers with the indexes. */
    private fun renumber(from: Int) {
        for (index in from until items.size) items[index].modelRow = index
    }

    private fun raiseVectorChanged(change: Int, index: Int) {
        if (handlers.isEmpty()) return
        val args = KComObject("WinUI4K.VectorChangedEventArgs")
            .addInterface(
                FoundationInterop.IID_IVectorChangedEventArgs,
                listOf(
                    // vtbl[6] get_CollectionChange(this, out CollectionChange)
                    KComObject.Method(DESC_PTR) { a ->
                        Ffi.backend.memory.putInt(a[1] as Ptr, 0, change)
                        KComObject.S_OK
                    },
                    // vtbl[7] get_Index(this, out UINT32)
                    KComObject.Method(DESC_PTR) { a ->
                        Ffi.backend.memory.putInt(a[1] as Ptr, 0, index)
                        KComObject.S_OK
                    },
                ),
            )
        try {
            val sender = comObject.pointerFor(FoundationInterop.IID_IObservableVector_Object)
            for (handler in handlers.values.toList()) {
                // VectorChangedEventHandler.Invoke(this, sender, args) — vtbl[3]
                handler.rawCall(3, DESC_INVOKE, sender, args.primary)
            }
        } finally {
            args.release()
        }
    }

    /** Adds one reference to the item and writes it to out (the caller Releases it). */
    private fun putItem(out: Ptr, offset: Long, item: TableRowItem) {
        item.comObject.addRef()
        Ffi.backend.memory.putPtr(out, offset, item.comObject.primary)
    }

    private fun getAtMethod() = KComObject.Method(DESC_I32_PTR) { args ->
        val index = args[1] as Int
        if (index !in items.indices) return@Method E_BOUNDS
        putItem(args[2] as Ptr, 0, items[index])
        KComObject.S_OK
    }

    private fun sizeMethod() = KComObject.Method(DESC_PTR) { args ->
        Ffi.backend.memory.putInt(args[1] as Ptr, 0, items.size)
        KComObject.S_OK
    }

    private fun indexOfMethod() = KComObject.Method(DESC_PTR_PTR_PTR) { args ->
        val index = itemOf(args[1] as Ptr)?.modelRow ?: -1
        Ffi.backend.memory.putInt(args[2] as Ptr, 0, if (index >= 0) index else 0)
        Ffi.backend.memory.putByte(args[3] as Ptr, 0, if (index >= 0) 1 else 0)
        KComObject.S_OK
    }

    private fun getManyMethod() = KComObject.Method(DESC_I32_I32_PTR_PTR) { args ->
        val start = args[1] as Int
        val capacity = args[2] as Int
        val out = args[3] as Ptr
        var written = 0
        while (written < capacity && start + written < items.size) {
            putItem(out, written.toLong() * 8, items[start + written])
            written++
        }
        Ffi.backend.memory.putInt(args[4] as Ptr, 0, written)
        KComObject.S_OK
    }

    /** IVectorView<Object> (GetAt=6 get_Size=7 IndexOf=8 GetMany=9). */
    private fun createView(): KComObject = KComObject("WinUI4K.TableRowCollectionView").addInterface(
        FoundationInterop.IID_IVectorView_Object,
        listOf(getAtMethod(), sizeMethod(), indexOfMethod(), getManyMethod()),
    )

    /** IIterator<Object>. Each First returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject("WinUI4K.TableRowCollectionIterator").addInterface(
            FoundationInterop.IID_IIterator_Object,
            listOf(
                // vtbl[6] get_Current(this, out IInspectable)
                KComObject.Method(DESC_PTR) { args ->
                    if (index >= items.size) return@Method E_BOUNDS
                    putItem(args[1] as Ptr, 0, items[index])
                    KComObject.S_OK
                },
                // vtbl[7] get_HasCurrent(this, out boolean)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < items.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[8] MoveNext(this, out boolean)
                KComObject.Method(DESC_PTR) { args ->
                    index++
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < items.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetMany(this, UINT32 capacity, IInspectable* items, out UINT32 actual)
                KComObject.Method(DESC_I32_PTR_PTR) { args ->
                    val capacity = args[1] as Int
                    val out = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < items.size) {
                        putItem(out, written.toLong() * 8, items[index])
                        written++
                        index++
                    }
                    Ffi.backend.memory.putInt(args[3] as Ptr, 0, written)
                    KComObject.S_OK
                },
            ),
        )
    }
}

private val DESC_THIS = CallDescriptor(ValueKind.I32, ArgKind.PTR)
private val DESC_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
private val DESC_PTR_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
private val DESC_I32 = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32)
private val DESC_I32_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR)
private val DESC_I32_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_I32_I32_PTR_PTR =
    CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_I64 = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I64)

/** delegate.Invoke(this, sender, args) — vtbl[3]. */
private val DESC_INVOKE = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

/** Adding or removing more rows than this sends a single Reset notification instead of per-row notifications. */
private const val BULK_CHANGE_THRESHOLD = 64
private val E_BOUNDS = 0x8000000B.toInt()
private val E_NOTIMPL = 0x80004001.toInt()
