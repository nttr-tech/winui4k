package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests the row objects (IObservableMap<String, Object>) and the row collection (IObservableVector<Object>) passed to
 * TableView's ItemsSource, using the same COM calls as XAML bindings / TableViewSource.
 */
class TableRowsTest : FunSpec() {
    /** A [TableRowValues] that holds 2 rows × 2 columns of values and records the values written to it. */
    private class FakeValues : TableRowValues {
        val data = mutableListOf(mutableListOf<Any?>("Apple", 150), mutableListOf<Any?>("Orange", 80))
        val written = mutableListOf<Triple<Int, Int, Any?>>()

        override fun columnCount(): Int = 2

        override fun valueAt(row: Int, column: Int): Any? = data[row][column]

        override fun valueWritten(item: TableRowItem, column: Int, value: Any?) {
            written += Triple(item.modelRow, column, value)
        }
    }

    /** The value obtained by a Lookup of [key] on row [item] as an IMap<String, Object>. */
    private fun lookup(item: TableRowItem, key: String): Any? {
        val map = ComPtr(item.comObject.primary)
        val boxed = Hstring.use(key) { h -> map.getPtrOrNull(FoundationInterop.IMap_Lookup, h) } ?: return null
        return try {
            PropertyValues.unboxAny(boxed)
        } finally {
            boxed.release()
        }
    }

    /** Inserts [value] at [key] on row [item] as an IMap<String, Object> (same as a binding write-back). */
    private fun insert(item: TableRowItem, key: String, value: Any?) {
        val map = ComPtr(item.comObject.primary)
        val boxed = PropertyValues.boxAny(value)
        try {
            Hstring.use(key) { h ->
                Ffi.backend.withScope { scope ->
                    map.call(FoundationInterop.IMap_Insert, h, boxed?.ptr, scope.allocate(1, 1))
                }
            }
        } finally {
            boxed?.release()
        }
    }

    init {
        test("a row object returns the model's cell value for a Lookup of the key c<column>") {
            onUiThreadGet {
                val rows = TableRowCollection(FakeValues())
                rows.insert(0, 2)
                listOf(lookup(rows[0], "c0"), lookup(rows[0], "c1"), lookup(rows[1], "c0"))
            } shouldBe listOf("Apple", 150, "Orange")
        }

        test("a Lookup of an out-of-range column or a malformed key fails (the binding treats it as unconnected)") {
            onUiThreadGet {
                val rows = TableRowCollection(FakeValues())
                rows.insert(0, 1)
                listOf("c2", "name", "c").map { key ->
                    runCatching { lookup(rows[0], key) }.exceptionOrNull() is WindowsRuntimeException
                }
            } shouldBe listOf(true, true, true)
        }

        test("a value written by Insert is unboxed and passed to TableRowValues along with its row and column numbers") {
            val values = FakeValues()
            onUiThreadGet {
                val rows = TableRowCollection(values)
                rows.insert(0, 2)
                insert(rows[1], "c1", "95")
                insert(rows[0], "c0", null)
            }
            values.written shouldBe listOf(Triple(1, 1, "95"), Triple(0, 0, null))
        }

        test("raiseValueChanged notifies MapChanged subscribers of the key of the changed column") {
            val keys = onUiThreadGet {
                val rows = TableRowCollection(FakeValues())
                rows.insert(0, 1)
                val observable = ComPtr(rows[0].comObject.primary).queryInterface(FoundationInterop.IID_IObservableMap_String_Object)
                val received = mutableListOf<String>()
                observable.addEventHandler("TestMapChangedHandler", FoundationInterop.IID_MapChangedEventHandler_String_Object, 6) { _, args ->
                    received += ComPtr(args).getString(7) // IMapChangedEventArgs.get_Key
                }
                rows[0].raiseValueChanged(1)
                rows[0].raiseAllValuesChanged()
                observable.release()
                received
            }
            keys shouldBe listOf("c1", "c0", "c1")
        }

        test("row insertions and removals are notified with their positions via VectorChanged, and row numbers are reassigned") {
            val (events, numbers) = onUiThreadGet {
                val rows = TableRowCollection(FakeValues())
                rows.insert(0, 2)
                val observable = ComPtr(rows.comObject.primary).queryInterface(FoundationInterop.IID_IObservableVector_Object)
                val received = mutableListOf<Pair<Int, Int>>()
                observable.addEventHandler("TestVectorChangedHandler", FoundationInterop.IID_VectorChangedEventHandler_Object, 6) { _, args ->
                    val changed = ComPtr(args)
                    received += changed.getInt(6) to changed.getInt(7) // CollectionChange, Index
                }
                rows.insert(1, 2) // Insert at rows 1 and 2 (the original row 1 moves to row 3)
                rows.remove(0, 1)
                observable.release()
                received to (0 until rows.size).map { rows[it].modelRow }
            }
            events shouldBe listOf(
                FoundationInterop.CollectionChange_ItemInserted to 1,
                FoundationInterop.CollectionChange_ItemInserted to 2,
                FoundationInterop.CollectionChange_ItemRemoved to 0,
            )
            numbers shouldBe listOf(0, 1, 2)
        }

        test("the row collection's IVector returns row objects through Size, GetAt, and IndexOf, and rejects modifications") {
            onUiThreadGet {
                val rows = TableRowCollection(FakeValues())
                rows.insert(0, 2)
                val vector = ComPtr(rows.comObject.primary)
                val size = vector.getInt(FoundationInterop.IVector_get_Size)
                val second = vector.getPtr(FoundationInterop.IVector_GetAt, 1)
                val found = rows.itemOf(second.ptr) === rows[1]
                val index = Ffi.backend.withScope { scope ->
                    val out = scope.allocate(4)
                    val foundFlag = scope.allocate(1, 1)
                    vector.call(FoundationInterop.IVector_IndexOf, second.ptr, out, foundFlag)
                    Ffi.backend.memory.getInt(out, 0)
                }
                second.release()
                val appendRejected = shouldThrow<WindowsRuntimeException> {
                    vector.call(FoundationInterop.IVector_Append, rows[0].comObject.primary)
                }.message != null
                listOf(size, found, index, appendRejected)
            } shouldBe listOf(2, true, 1, true)
        }
    }
}
