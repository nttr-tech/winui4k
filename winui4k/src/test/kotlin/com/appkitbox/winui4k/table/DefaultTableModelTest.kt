package com.appkitbox.winui4k.table

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests for data operations and change notifications (TableModelEvent) of DefaultTableModel / AbstractTableModel (no UI required). */
@Tags("Pure")
class DefaultTableModelTest : FunSpec() {
    /** Attaches a listener that records received events as (type, firstRow, lastRow, column). */
    private fun DefaultTableModel.recordEvents(): MutableList<List<Int>> {
        val events = mutableListOf<List<Int>>()
        addTableModelListener { e -> events += listOf(e.type, e.firstRow, e.lastRow, e.column) }
        return events
    }

    private fun model() = DefaultTableModel(listOf(listOf("a", 1), listOf("b", 2)), listOf("Name", "Count"))

    init {
        test("a model built from row data and column names returns the row count, column count, values, and column names, and every cell is editable") {
            val model = model()
            listOf(
                model.getRowCount(),
                model.getColumnCount(),
                model.getValueAt(1, 0),
                model.getColumnName(1),
                model.isCellEditable(0, 0),
            ) shouldBe listOf(2, 2, "b", "Count", true)
        }

        test("addRow / insertRow build rows matching the column count and notify INSERT") {
            val model = model()
            val events = model.recordEvents()
            model.addRow("c")
            model.insertRow(0, listOf("z", 9, "extra"))
            listOf(model.getValueAt(3, 0), model.getValueAt(3, 1), model.getValueAt(0, 1), model.getRowCount()) shouldBe
                listOf("c", null, 9, 4)
            events shouldBe listOf(
                listOf(TableModelEvent.INSERT, 2, 2, TableModelEvent.ALL_COLUMNS),
                listOf(TableModelEvent.INSERT, 0, 0, TableModelEvent.ALL_COLUMNS),
            )
        }

        test("setValueAt notifies a cell UPDATE, and removeRow notifies DELETE") {
            val model = model()
            val events = model.recordEvents()
            model.setValueAt(5, 0, 1)
            model.removeRow(1)
            listOf(model.getValueAt(0, 1), model.getRowCount()) shouldBe listOf(5, 1)
            events shouldBe listOf(listOf(TableModelEvent.UPDATE, 0, 0, 1), listOf(TableModelEvent.DELETE, 1, 1, TableModelEvent.ALL_COLUMNS))
        }

        test("setRowCount notifies the range of added or removed rows with INSERT / DELETE") {
            val model = model()
            val events = model.recordEvents()
            model.setRowCount(5)
            model.setRowCount(1)
            events shouldBe listOf(
                listOf(TableModelEvent.INSERT, 2, 4, TableModelEvent.ALL_COLUMNS),
                listOf(TableModelEvent.DELETE, 1, 4, TableModelEvent.ALL_COLUMNS),
            )
            model.getRowCount() shouldBe 1
        }

        test("moveRow moves rows and notifies UPDATE for the affected range") {
            val model = DefaultTableModel(listOf(listOf("a"), listOf("b"), listOf("c"), listOf("d")), listOf("x"))
            val events = model.recordEvents()
            model.moveRow(0, 1, 2) // move a, b after c, d
            (0 until 4).map { model.getValueAt(it, 0) } shouldBe listOf("c", "d", "a", "b")
            events shouldBe listOf(listOf(TableModelEvent.UPDATE, 0, 3, TableModelEvent.ALL_COLUMNS))
        }

        test("moveRow throws on an invalid range") {
            shouldThrow<IllegalArgumentException> { model().moveRow(1, 0, 0) }
        }

        test("addColumn adds a column and notifies a column structure change (HEADER_ROW)") {
            val model = model()
            val events = model.recordEvents()
            model.addColumn("Notes", listOf("Memo"))
            listOf(model.getColumnCount(), model.getColumnName(2), model.getValueAt(0, 2), model.getValueAt(1, 2)) shouldBe
                listOf(3, "Notes", "Memo", null)
            events.single()[1] shouldBe TableModelEvent.HEADER_ROW
        }

        test("unnamed columns get spreadsheet-style names (A, B, ..., Z, AA), and findColumn finds columns") {
            val model = DefaultTableModel(1, 28)
            listOf(model.getColumnName(0), model.getColumnName(25), model.getColumnName(26), model.findColumn("AB"), model.findColumn("Missing")) shouldBe
                listOf("A", "Z", "AA", 27, -1)
        }

        test("listeners are called starting from the most recently added, and removed listeners are not called") {
            val model = model()
            val order = mutableListOf<String>()
            val first = TableModelListener { order += "first" }
            model.addTableModelListener(first)
            model.addTableModelListener { order += "second" }
            model.setValueAt("x", 0, 0)
            model.removeTableModelListener(first)
            model.setValueAt("y", 0, 0)
            order shouldBe listOf("second", "first", "second")
        }

        context("Swing compatibility") {
            test("created from a column name array and a row count it has rows of null values, and the array versions of setDataVector, setColumnIdentifiers and addColumn give the same results as the List versions") {
                val model = DefaultTableModel(arrayOf("Name", "Price"), 1)
                listOf(model.getRowCount(), model.getColumnCount(), model.getValueAt(0, 0)) shouldBe listOf(1, 2, null)
                val events = model.recordEvents()
                model.setDataVector(arrayOf(arrayOf<Any?>("Apple", 120), arrayOf<Any?>("Orange")), arrayOf<Any?>("Name", "Price"))
                model.setColumnIdentifiers(arrayOf("Product", "Cost"))
                model.addColumn("Origin", arrayOf("Aomori"))
                model.getDataVector() shouldBe listOf(listOf("Apple", 120, "Aomori"), listOf("Orange", null, null))
                model.getColumnName(0) shouldBe "Product"
                events.map { it[1] } shouldBe listOf(TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW)
            }

            test("setNumRows notifies added and removed rows with INSERT / DELETE like setRowCount, and notifies nothing for the same row count") {
                val model = model()
                val events = model.recordEvents()
                model.setNumRows(3)
                model.setNumRows(3)
                model.setNumRows(0)
                events shouldBe listOf(
                    listOf(TableModelEvent.INSERT, 2, 2, TableModelEvent.ALL_COLUMNS),
                    listOf(TableModelEvent.DELETE, 0, 2, TableModelEvent.ALL_COLUMNS),
                )
                model.getRowCount() shouldBe 0
            }

            test("newDataAvailable, newRowsAdded and rowsRemoved deliver the passed event to listeners as is") {
                val model = model()
                val received = mutableListOf<TableModelEvent>()
                model.addTableModelListener { received += it }
                val updated = TableModelEvent(model, 0, 1)
                val inserted = TableModelEvent(model, 1, 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT)
                val deleted = TableModelEvent(model, 0, 0, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE)
                model.newDataAvailable(updated)
                model.newRowsAdded(inserted)
                model.rowsRemoved(deleted)
                received shouldBe listOf(updated, inserted, deleted)
                model.getRowCount() shouldBe 2
            }
        }
    }
}
