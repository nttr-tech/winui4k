package com.appkitbox.winui4k.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use DefaultTableModel in the same way developers who have used Swing's DefaultTableModel would write it in Java (no window is used). */
class DefaultTableModelForJavaTest {
    @Test
    void createdFromArraysChangesByAddRowSetValueAtAndSetNumRowsReachAnAnonymousTableModelListener() {
        DefaultTableModel model = new DefaultTableModel(new Object[][] {{"Apple", 120}}, new Object[] {"Name", "Price"});
        List<String> events = new ArrayList<>();
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                events.add(e.getType() + ":" + e.getFirstRow() + "-" + e.getLastRow());
            }
        });
        model.addRow(new Object[] {"Orange", 80});
        model.setValueAt(100, 0, 1);
        model.setNumRows(1);
        assertEquals(1, model.getRowCount());
        assertEquals(100, model.getValueAt(0, 1));
        assertEquals("Price", model.getColumnName(1));
        List<String> expected = new ArrayList<>();
        expected.add(TableModelEvent.INSERT + ":1-1");
        expected.add(TableModelEvent.UPDATE + ":0-0");
        expected.add(TableModelEvent.DELETE + ":1-1");
        assertEquals(expected, events);
    }

    @Test
    void createdFromAColumnNameArrayAndARowCountSetDataVectorSetColumnIdentifiersAndAddColumnCanBeCalledWithArrays() {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Name", "Price"}, 0);
        assertEquals(0, model.getRowCount());
        assertEquals(2, model.getColumnCount());
        model.setDataVector(new Object[][] {{"Apple", 120}, {"Orange", 80}}, new Object[] {"Name", "Price"});
        assertEquals(2, model.getRowCount());
        assertEquals(80, model.getValueAt(1, 1));
        model.setColumnIdentifiers(new Object[] {"Product", "Cost"});
        assertEquals("Product", model.getColumnName(0));
        model.addColumn("Origin", new Object[] {"Aomori"});
        assertEquals(3, model.getColumnCount());
        assertEquals("Aomori", model.getValueAt(0, 2));
        assertEquals(null, model.getValueAt(1, 2));
        assertEquals(0, model.findColumn("Product"));
    }

    @Test
    void aSubclassCanNotifyEventsItCreatedWithNewRowsAddedNewDataAvailableAndRowsRemoved() {
        DefaultTableModel model = new DefaultTableModel(2, 1);
        List<TableModelEvent> received = new ArrayList<>();
        model.addTableModelListener(received::add);
        TableModelEvent added = new TableModelEvent(model, 1, 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        model.newRowsAdded(added);
        model.newDataAvailable(new TableModelEvent(model));
        model.rowsRemoved(new TableModelEvent(model, 0, 0, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
        assertEquals(3, received.size());
        assertSame(added, received.get(0));
        assertEquals(Integer.MAX_VALUE, received.get(1).getLastRow());
        assertEquals(TableModelEvent.DELETE, received.get(2).getType());
    }
}
