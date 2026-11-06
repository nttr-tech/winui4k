package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.appkitbox.winui4k.table.AbstractTableModel;
import com.appkitbox.winui4k.table.DefaultTableModel;
import com.appkitbox.winui4k.table.TableColumn;
import com.appkitbox.winui4k.table.TableModelEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTable from Java in the same way developers who have used Swing's JTable would write it.
 * Compiling at all verifies the shape of the API, and the bodies check the results of models, columns and selection.
 */
class WTableForJavaTest {
    @Test
    void canBeCreatedWithTheArrayConstructorAndGetColumnByColumnName() {
        onUiThread(() -> {
            Object[][] data = {{"Apple", 150}, {"Orange", 80}};
            String[] columnNames = {"Product", "Price"};
            WTable table = new WTable(data, columnNames);
            assertEquals(2, table.getRowCount());
            assertEquals("Orange", table.getValueAt(1, 0));
            TableColumn column = table.getColumn("Price");
            assertEquals(1, column.getModelIndex());
            assertNull(column.getHeaderValue());
        });
    }

    @Test
    void anAnonymousAbstractTableModelAndTableModelEventConstantsCanBeUsed() {
        onUiThread(() -> {
            final List<String> names = new ArrayList<>();
            names.add("One");
            AbstractTableModel model = new AbstractTableModel() {
                @Override
                public int getRowCount() {
                    return names.size();
                }

                @Override
                public int getColumnCount() {
                    return 1;
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    return names.get(rowIndex);
                }
            };
            List<Integer> types = new ArrayList<>();
            model.addTableModelListener(e -> types.add(e.getType()));
            WTable table = new WTable(model);
            names.add("Two");
            model.fireTableRowsInserted(1, 1);
            assertEquals(2, table.getRowCount());
            assertEquals(TableModelEvent.INSERT, (int) types.get(0));
        });
    }

    @Test
    void aListSelectionListenerOnGetSelectionModelReceivesTheSelectionCheckingGetValueIsAdjusting() {
        WTable table = UiTestHarness.onUiThreadGet(() -> new WTable(new DefaultTableModel(new Object[][] {{"a"}, {"b"}, {"c"}}, new Object[] {"Name"})));
        UiTestHarness.attachAndAwaitLoaded(table);
        try {
            onUiThread(() -> {
                table.updateLayout();
                List<Integer> rows = new ArrayList<>();
                table.getSelectionModel().addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        rows.add(table.getSelectedRow());
                    }
                });
                table.setRowSelectionInterval(2, 2);
                table.changeSelection(0, 0, false, false);
                assertEquals(0, table.getSelectedRow());
                assertArrayEquals(new int[] {0}, table.getSelectedRows());
                assertEquals(2, rows.size());
                assertEquals(0, (int) rows.get(1));
                assertEquals(-1, table.getSelectedColumn());
                assertFalse(table.getColumnSelectionAllowed());
                table.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                assertEquals(WListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
                assertThrows(IllegalArgumentException.class, () -> table.setRowSelectionInterval(0, 3));
            });
        } finally {
            UiTestHarness.detach(table);
        }
    }

    @Test
    void theThreeArgumentConstructorTakingASelectionModel() {
        onUiThread(() -> {
            WDefaultListSelectionModel selection = new WDefaultListSelectionModel();
            WTable table = new WTable(new DefaultTableModel(3, 2), null, selection);
            assertSame(selection, table.getSelectionModel());
            assertEquals(2, table.getColumnCount());
        });
    }
}
