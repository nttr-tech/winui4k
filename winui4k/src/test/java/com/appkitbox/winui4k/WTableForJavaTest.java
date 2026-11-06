package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appkitbox.winui4k.table.AbstractTableModel;
import com.appkitbox.winui4k.table.DefaultCellEditor;
import com.appkitbox.winui4k.table.DefaultTableCellRenderer;
import com.appkitbox.winui4k.table.DefaultTableModel;
import com.appkitbox.winui4k.table.RowFilter;
import com.appkitbox.winui4k.table.RowSorter;
import com.appkitbox.winui4k.table.SortOrder;
import com.appkitbox.winui4k.table.TableColumn;
import com.appkitbox.winui4k.table.TableModel;
import com.appkitbox.winui4k.table.TableModelEvent;
import com.appkitbox.winui4k.table.TableRowSorter;
import com.appkitbox.winui4k.table.TableStringConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
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

    /** A 3-row table of product names and prices. */
    private static DefaultTableModel products() {
        return new DefaultTableModel(new Object[][] {{"Apple", 150}, {"Orange", 80}, {"Grape", 480}}, new Object[] {"Product", "Price"});
    }

    /** Shows [table], runs [body] on the UI thread, and then removes it. */
    private static void shown(WTable table, Runnable body) {
        UiTestHarness.attachAndAwaitLoaded(table);
        try {
            onUiThread(() -> {
                table.updateLayout();
                body.run();
            });
        } finally {
            UiTestHarness.detach(table);
        }
    }

    @Test
    void passingRowAndColumnCountsToNewWTableGivesATableOfEmptyCells() {
        onUiThread(() -> {
            WTable table = new WTable(3, 2);
            assertEquals(3, table.getRowCount());
            assertEquals(2, table.getColumnCount());
            assertNull(table.getValueAt(2, 1));
            table.setValueAt("Bottom right", 2, 1);
            assertEquals("Bottom right", table.getModel().getValueAt(2, 1));
        });
    }

    @Test
    void anAnonymousRunnableReceivesRowSelectionChangesAndIsRemovedWithRemoveAndRowInvocationCanBeAddedAndRemovedWithAnIntConsumer() {
        WTable table = UiTestHarness.onUiThreadGet(() -> new WTable(products()));
        List<Integer> rows = new ArrayList<>();
        List<Integer> invoked = new ArrayList<>();
        shown(table, () -> {
            Runnable listener = new Runnable() {
                @Override
                public void run() {
                    rows.add(table.getSelectedRow());
                }
            };
            table.addRowSelectionListener(listener);
            IntConsumer onInvoked = invoked::add;
            table.addRowInvokedListener(onInvoked);
            table.setRowSelectionInterval(1, 1);
            table.removeRowSelectionListener(listener);
            table.removeRowInvokedListener(onInvoked);
            table.setRowSelectionInterval(2, 2);
        });
        assertEquals(Collections.singletonList(1), rows);
        assertEquals(Collections.emptyList(), invoked); // Selection changes do not call row invocation (double-click)
    }

    @Test
    void tableRowSorterSetSortKeysSortsReceivesListenersBeforeAndAfterAndCanBeCanceled() {
        DefaultTableModel model = products();
        WTable table = UiTestHarness.onUiThreadGet(() -> new WTable(model));
        List<String> events = new ArrayList<>();
        shown(table, () -> {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.addSortingListener(e -> events.add("sorting " + e.getModelColumn() + " " + e.getSortOrder()));
            table.addSortedListener(e -> events.add("sorted " + e.getModelColumn() + " " + e.getSortOrder()));
            List<RowSorter.SortKey> keys = new ArrayList<>();
            keys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
            sorter.setSortKeys(keys);
            assertEquals("Grape", table.getValueAt(0, 0));
            assertEquals(1, table.convertRowIndexToModel(2));

            TableSortListener veto = e -> e.setCanceled(true);
            table.addSortingListener(veto);
            List<RowSorter.SortKey> byName = new ArrayList<>();
            byName.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter.setSortKeys(byName);
            assertEquals("Grape", table.getValueAt(0, 0)); // Canceled, so it stays in descending order of price
            table.removeSortingListener(veto);
        });
        assertEquals("sorting 1 DESCENDING", events.get(0));
        assertEquals("sorted 1 DESCENDING", events.get(1));
    }

    @Test
    void rowFilterRegexFilterAndNumberFilterNarrowDownRows() {
        DefaultTableModel model = products();
        WTable table = UiTestHarness.onUiThreadGet(() -> new WTable(model));
        shown(table, () -> {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.setRowFilter(RowFilter.regexFilter("^O", 0));
            assertEquals(1, table.getRowCount());
            assertEquals("Orange", table.getValueAt(0, 0));
            sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 100, 1));
            assertEquals(2, table.getRowCount());
            assertEquals(2, table.convertRowIndexToModel(1));
            assertEquals(-1, table.convertRowIndexToView(1));
            sorter.setRowFilter(null);
            assertEquals(3, table.getRowCount());
        });
    }

    @Test
    void anAnonymousTableStringConverterSetWithSetStringConverterMakesRegexFilterMatchTheConvertedStrings() {
        DefaultTableModel model = products();
        WTable table = UiTestHarness.onUiThreadGet(() -> new WTable(model));
        shown(table, () -> {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            assertNull(sorter.getStringConverter());
            sorter.setRowFilter(RowFilter.regexFilter("^¥4", 1));
            assertEquals(0, table.getRowCount());
            TableStringConverter converter = new TableStringConverter() {
                @Override
                public String toString(TableModel model, int row, int column) {
                    return column == 1 ? "¥" + model.getValueAt(row, column) : String.valueOf(model.getValueAt(row, column));
                }
            };
            sorter.setStringConverter(converter);
            assertSame(converter, sorter.getStringConverter());
            assertEquals(1, table.getRowCount());
            assertEquals("Grape", table.getValueAt(0, 0));
        });
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

    @Test
    void useColumnMovingAndConversionDefaultRenderersAndEditorsAndGetCellRendererTheSameWayAsJTable() {
        onUiThread(() -> {
            WTable table = new WTable(products());
            table.moveColumn(1, 0);
            assertEquals("Price", table.getColumnName(0));
            assertEquals(1, table.convertColumnIndexToModel(0));
            assertEquals(0, table.convertColumnIndexToView(1));
            assertEquals(Object.class, table.getColumnClass(1));
            assertTrue(table.isCellEditable(0, 0));

            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
            DefaultCellEditor editor = new DefaultCellEditor(new WTextField());
            table.setDefaultRenderer(Object.class, renderer);
            table.setDefaultEditor(Object.class, editor);
            assertSame(renderer, table.getDefaultRenderer(String.class)); // Follows the superclass defaults
            assertSame(editor, table.getDefaultEditor(Object.class));
            assertSame(renderer, table.getCellRenderer(0, 0));
            assertSame(editor, table.getCellEditor(2, 1));

            table.removeColumn(table.getColumnModel().getColumn(0));
            assertEquals(1, table.getColumnCount());
            table.createDefaultColumnsFromModel();
            assertEquals(2, table.getColumnCount());
            assertEquals("Product", table.getColumnName(0));
            assertEquals(-1, table.rowAtPoint(new WPoint(0, 0))); // No rows before it is shown
            assertEquals(-1, table.columnAtPoint(new WPoint(0, 0)));
        });
    }

    @Test
    void selectAllDeselectRowAndSetShowGrid() {
        WTable table = onUiThreadGet(() -> new WTable(products()));
        shown(table, () -> {
            table.selectAll();
            assertEquals(2, table.getSelectedRow()); // Only one row can be selected, so the last row, as with a single-selection JTable
            assertEquals(1, table.getSelectedRowCount());
            table.deselectRow(2);
            assertEquals(-1, table.getSelectedRow());
            table.setShowGrid(false);
            assertFalse(table.getShowHorizontalLines());
            assertFalse(table.getShowVerticalLines());
            table.setShowGrid(true);
            assertTrue(table.getShowHorizontalLines());
            assertTrue(table.getShowVerticalLines());
        });
    }
}
