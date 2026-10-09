package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.ItemsViewSelectionMode;
import com.appkitbox.winui4k.ListViewSelectionMode;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SelectionMode;
import com.appkitbox.winui4k.SpinButtonPlacementMode;
import com.appkitbox.winui4k.TableDensity;
import com.appkitbox.winui4k.TableSelectionMode;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.TreeViewSelectionMode;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WItemContainer;
import com.appkitbox.winui4k.WItemsView;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WListBox;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WTreeNode;
import com.appkitbox.winui4k.WUniformGridLayout;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/*
 * Collections category: demo pages for ItemsView / ListBox / ListView / Table / TreeView.
 */
final class CollectionsPages {
    private CollectionsPages() {
    }

    // region ItemsView

    /** The ItemsView page: lines up demos for trying out WItemsView's various features. */
    static WComponent buildItemsViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ItemsView",
                "A collection-display control with a swappable layout. Try out WItemsView's various features.");

        page.add(buildUniformGridItemsViewExample());
        return page;
    }

    /** A card grid: UniformGridLayout + ItemContainer + ItemInvoked. */
    private static WComponent buildUniformGridItemsViewExample() {
        WLabel result = new WLabel("Clicked: none");

        List<String> fruits = Arrays.asList("Apple", "Orange", "Grape", "Peach", "Cherry", "Banana");
        List<WItemContainer> containers = new ArrayList<>();
        for (String name : fruits) {
            WLabel label = new WLabel(name);
            label.setMargin(12.0, 12.0, 12.0, 12.0);
            WBorder card = new WBorder(label);
            card.setBackground(GalleryTheme.CARD_BACKGROUND());
            card.setBorderColor(GalleryTheme.CARD_BORDER());
            card.setBorderThickness(1.0);
            card.setCornerRadius(8.0);
            containers.add(new WItemContainer(card));
        }

        WUniformGridLayout layout = new WUniformGridLayout();
        layout.setMinItemWidth(160.0);
        layout.setMinColumnSpacing(12.0);
        layout.setMinRowSpacing(12.0);

        WItemsView itemsView = new WItemsView();
        itemsView.setLayout(layout);
        itemsView.setSelectionMode(ItemsViewSelectionMode.NONE);
        itemsView.setItemInvokedEnabled(true);
        itemsView.setItems(containers);
        itemsView.addItemInvokedListener(index -> {
            result.setText("Clicked: " + fruits.get(index) + " (index = " + index + ")");
        });
        itemsView.setWidth(520.0);
        itemsView.setHeight(200.0);
        itemsView.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WPanel body = new WPanel(10.0);
        body.add(itemsView);
        body.add(result);
        return GalleryScaffold.buildExample("A card grid (UniformGridLayout / ItemContainer / ItemInvoked)", body);
    }

    // endregion

    // region ListBox

    /** The ListBox page: lines up demos for trying out WListBox's various features. */
    static WComponent buildListBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ListBox", "A control for selecting an item from an always-visible list. Try out WListBox's various features.");

        page.add(buildListBoxColorExample());
        page.add(buildListBoxFontExample());
        page.add(buildListBoxSelectionModeExample());
        return page;
    }

    /** Color selection: mirrors example 1 from the official Gallery (inline items + SelectionChanged changes a rectangle's color). */
    private static WComponent buildListBoxColorExample() {
        // Same color names as the official Gallery. Repaints the rectangle below based on the selection
        Map<String, WColor> colors = new LinkedHashMap<>();
        colors.put("Blue", new WColor(0, 0, 255, 255));
        colors.put("Green", new WColor(0, 128, 0, 255));
        colors.put("Red", new WColor(255, 0, 0, 255));
        colors.put("Yellow", new WColor(255, 255, 0, 255));

        // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
        WBorder output = new WBorder();
        output.setWidth(100.0);
        output.setHeight(30.0);
        output.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WListBox listBox = new WListBox(new ArrayList<>(colors.keySet()));
        listBox.setWidth(200.0);
        listBox.setHorizontalAlignment(HorizontalAlignment.LEFT);
        listBox.addListSelectionListener(() -> {
            output.setBackground(colors.get(listBox.getSelectedItem()));
        });

        WPanel body = new WPanel(10.0);
        body.add(listBox);
        body.add(output);
        return GalleryScaffold.buildExample("A list box with inline items (SelectionChanged)", body);
    }

    /** Font selection: mirrors example 2 from the official Gallery (fixed height + initial selection + selection changes the font). */
    private static WComponent buildListBoxFontExample() {
        List<String> fonts = Arrays.asList("Arial", "Comic Sans MS", "Courier New", "Segoe UI", "Times New Roman");

        WLabel output = new WLabel("You can set the font used for this text.");
        output.setForeground(GalleryTheme.TEXT_SECONDARY());

        WListBox listBox = new WListBox(fonts);
        listBox.setWidth(200.0);
        listBox.setHeight(164.0);
        listBox.setHorizontalAlignment(HorizontalAlignment.LEFT);
        listBox.addListSelectionListener(() -> {
            String selected = listBox.getSelectedItem();
            if (selected != null) {
                output.setFontFamily(selected);
            }
        });
        listBox.setSelectedIndex(2); // Selects Courier New initially, same as the official Gallery

        WPanel body = new WPanel(10.0);
        body.add(listBox);
        body.add(output);
        return GalleryScaffold.buildExample("A list box with a fixed height (SelectedIndex / FontFamily)", body);
    }

    /** Selection mode: switching selectionMode plus selectAll / selectedItems / scrollIntoView. */
    private static WComponent buildListBoxSelectionModeExample() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            items.add("Option " + i);
        }
        WListBox listBox = new WListBox(items);
        listBox.setWidth(240.0);
        listBox.setHeight(200.0);
        listBox.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WLabel selection = new WLabel("Selected: none");
        listBox.addListSelectionListener(() -> {
            List<String> selectedItems = listBox.getSelectedItems();
            selection.setText(selectedItems.isEmpty() ? "Selected: none" : "Selection: " + String.join(", ", selectedItems));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (SelectionMode selectionMode : SelectionMode.values()) {
            WButton button = new WButton(selectionMode.name());
            button.addActionListener(() -> {
                listBox.setSelectionMode(selectionMode);
            });
            buttons.add(button);
        }
        WButton selectAllButton = new WButton("Select all");
        selectAllButton.addActionListener(() -> {
            listBox.selectAll();
        });
        buttons.add(selectAllButton);
        WButton scrollButton = new WButton("Scroll to end");
        scrollButton.addActionListener(() -> {
            listBox.scrollIntoView(listBox.getItemCount() - 1);
        });
        buttons.add(scrollButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(listBox);
        body.add(selection);
        return GalleryScaffold.buildExample("Selection mode (SelectionMode / SelectAll / SelectedItems / ScrollIntoView)", body);
    }

    // endregion

    // region ListView

    /** The ListView page: lines up demos for trying out WList's various features. */
    static WComponent buildListViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ListView", "A list that lines items up vertically for selection. Try out WList's various features.");

        page.add(buildSimpleListExample());
        page.add(buildListItemOperationsExample());
        page.add(buildListSelectionModeExample());
        page.add(buildListItemClickExample());
        return page;
    }

    /** A basic list: responding to selection changes (SelectionChanged). */
    private static WComponent buildSimpleListExample() {
        WLabel result = new WLabel("Selected: none");

        WList list = new WList(Arrays.asList("Apple", "Orange", "Grape", "Peach", "Cherry"));
        list.setWidth(240.0);
        list.addListSelectionListener(() -> {
            String item = list.getSelectedItem();
            result.setText(item == null ? "Selected: none" : "Selected: " + item + " (index = " + list.getSelectedIndex() + ")");
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(list);
        row.add(result);
        return GalleryScaffold.buildExample("A simple list", row);
    }

    /** Adding and removing items: addItem / removeItem / removeAllItems / itemCount. */
    private static WComponent buildListItemOperationsExample() {
        // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
        WList list = new WList(Arrays.asList("Item 1", "Item 2", "Item 3"));
        list.setWidth(240.0);
        list.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WLabel count = new WLabel("Item count: " + list.getItemCount());
        WTextField input = new WTextField("Item name to add");
        input.setWidth(200.0);

        WButton addButton = new WButton("Add");
        addButton.addActionListener(() -> {
            if (!input.getText().isEmpty()) {
                list.addItem(input.getText());
                input.setText("");
                count.setText("Item count: " + list.getItemCount());
            }
        });

        WButton removeButton = new WButton("Remove selected");
        removeButton.addActionListener(() -> {
            int index = list.getSelectedIndex();
            if (index >= 0) {
                list.removeItem(index);
                count.setText("Item count: " + list.getItemCount());
            }
        });

        WButton clearButton = new WButton("Remove all");
        clearButton.addActionListener(() -> {
            list.removeAllItems();
            count.setText("Item count: " + list.getItemCount());
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(input);
        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(clearButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(list);
        body.add(count);
        return GalleryScaffold.buildExample("Adding and removing items", body);
    }

    /** Selection mode: switching selectionMode and selectAll. */
    private static WComponent buildListSelectionModeExample() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            items.add("Option " + i);
        }
        WList list = new WList(items);
        list.setWidth(240.0);
        list.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WLabel mode = new WLabel("Selection mode: " + list.getSelectionMode());

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (ListViewSelectionMode selectionMode : ListViewSelectionMode.values()) {
            WButton button = new WButton(selectionMode.name());
            button.addActionListener(() -> {
                list.setSelectionMode(selectionMode);
                mode.setText("Selection mode: " + list.getSelectionMode());
            });
            buttons.add(button);
        }
        WButton selectAllButton = new WButton("Select all");
        selectAllButton.addActionListener(() -> {
            list.selectAll();
        });
        buttons.add(selectAllButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(list);
        body.add(mode);
        return GalleryScaffold.buildExample("Selection mode (SelectionMode / SelectAll)", body);
    }

    /** ItemClick: enabling isItemClickEnabled and receiving the clicked item. */
    private static WComponent buildListItemClickExample() {
        WLabel result = new WLabel("Clicked: none");

        WList list = new WList(Arrays.asList("Documents", "Pictures", "Music", "Video"));
        list.setWidth(240.0);
        list.setItemClickEnabled(true);
        list.addItemClickListener(item -> {
            result.setText("Clicked: " + item);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(list);
        row.add(result);
        return GalleryScaffold.buildExample("Item clicks (ItemClick)", row);
    }

    // endregion

    // region Table page

    /** The Table page: lines up demos for trying out WTable's (WinUI 3's TableView, Windows App SDK 2.5 experimental) various features. */
    static WComponent buildTablePage() {
        WPanel page = GalleryScaffold.buildPage(
                "Table",
                "A table that displays data in rows and columns (the TableView from Windows App SDK 2.5 experimental). "
                        + "Like Swing's JTable, try out the various features of WTable, built from TableModel / TableColumnModel / TableRowSorter.");

        page.add(buildTableBasicExample());
        page.add(buildTableSortFilterExample());
        page.add(buildTableEditingExample());
        page.add(buildTableGroupingExample());
        page.add(buildTableCellsExample());
        page.add(buildTableAppearanceExample());
        page.add(buildTableColumnsExample());
        return page;
    }

    /** Product data (product / category / price / stock / in stock). Returns column types to distinguish numeric and boolean columns. */
    private static final class ProductTableModel extends DefaultTableModel {
        ProductTableModel() {
            super(
                    Arrays.asList(
                            Arrays.<Object>asList("Apple", "Fruit", 150, 12, true),
                            Arrays.<Object>asList("Orange", "Fruit", 80, 30, true),
                            Arrays.<Object>asList("Grape", "Fruit", 480, 0, false),
                            Arrays.<Object>asList("Cabbage", "Vegetable", 200, 8, true),
                            Arrays.<Object>asList("Carrot", "Vegetable", 60, 25, true),
                            Arrays.<Object>asList("Tomato", "Vegetable", 120, 0, false),
                            Arrays.<Object>asList("Milk", "Dairy", 230, 15, true),
                            Arrays.<Object>asList("Cheese", "Dairy", 380, 4, true)),
                    Arrays.<Object>asList("Product", "Category", "Price", "Stock", "In stock"));
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 2:
                case 3:
                    return Integer.class;
                case 4:
                    return Boolean.class;
                default:
                    return String.class;
            }
        }
    }

    /** The table for the demos. Its height is fixed so it does not get lost in the page's scrolling. */
    private static WTable buildProductTable(TableModel model) {
        WTable table = new WTable(model);
        table.setWidth(620.0);
        table.setHeight(300.0);
        table.setHorizontalAlignment(HorizontalAlignment.LEFT);
        return table;
    }

    /** Creates a check box together with its change handler (for the Options panel). */
    private static WCheckBox optionCheckBox(String text, boolean checked, Consumer<Boolean> onChanged) {
        WCheckBox checkBox = new WCheckBox(text);
        checkBox.setChecked(checked);
        checkBox.addItemListener((value) -> onChanged.accept(Boolean.TRUE.equals(value)));
        return checkBox;
    }

    /** Basics: displaying a TableModel and selecting rows (SelectionMode / Select / SelectionChanged / view-model conversion). */
    private static WComponent buildTableBasicExample() {
        WTable table = buildProductTable(new ProductTableModel());
        WLabel result = new WLabel("Selected: none");
        table.addRowSelectionListener(() -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                result.setText("Selection: none");
            } else {
                int modelRow = table.convertRowIndexToModel(row);
                result.setText("Selection: " + table.getModel().getValueAt(modelRow, 0)
                        + " (view row = " + row + " / model row = " + modelRow + ")");
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(result);

        WComboBox selectionMode = new WComboBox(Arrays.asList("SINGLE (select one row)", "NONE (no selection)"));
        selectionMode.setSelectedIndex(0);
        selectionMode.addListSelectionListener(() -> table.setSelectionMode(
                selectionMode.getSelectedIndex() == 1 ? TableSelectionMode.NONE : TableSelectionMode.SINGLE));
        WButton selectThird = new WButton("Select the third row");
        selectThird.addActionListener(() -> table.selectRow(2));
        WButton clear = new WButton("Clear selection");
        clear.addActionListener(table::clearSelection);

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("Selection mode (SelectionMode)"));
        options.add(selectionMode);
        options.add(selectThird);
        options.add(clear);
        return GalleryScaffold.buildExample("Basics (TableModel / row selection / SelectionChanged)", body, options);
    }

    /** Sorting and filtering: TableRowSorter (header click / SortKeys / Comparator / RowFilter) and Sorting / Sorted. */
    private static WComponent buildTableSortFilterExample() {
        ProductTableModel model = new ProductTableModel();
        WTable table = buildProductTable(model);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        // Sort product names shortest first (alphabetically when the lengths are equal)
        sorter.setComparator(0, Comparator.comparingInt(String::length).thenComparing(Comparator.<String>naturalOrder()));
        table.setRowSorter(sorter);

        WLabel status = new WLabel("Click a column header to sort.");
        status.setTextWrapping(TextWrapping.WRAP);
        boolean[] cancelSorting = {false};
        table.addSortingListener((event) -> {
            event.setCanceled(cancelSorting[0]);
            status.setText(cancelSorting[0]
                    ? "Canceled sorting by \"" + table.getColumnName(event.getColumn()) + "\" (Cancel in Sorting)"
                    : "Sorting: " + table.getColumnName(event.getColumn()) + " → " + event.getSortOrder());
        });
        table.addSortedListener((event) -> status.setText("Sorted: " + table.getColumnName(event.getColumn())
                + " (" + event.getSortOrder() + ") / showing " + table.getRowCount() + " rows"));

        WTextField filterText = new WTextField("Filter by product (regular expression)");
        filterText.setWidth(240.0);
        filterText.addTextChangedListener((text) -> {
            sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter(text, 0));
            status.setText("Filtered: showing " + table.getRowCount() + " of " + model.getRowCount() + " rows");
        });

        WPanel body = new WPanel(8.0);
        body.add(filterText);
        body.add(table);
        body.add(status);

        WButton byPrice = new WButton("Highest price first");
        byPrice.addActionListener(() -> sorter.setSortKeys(
                Collections.singletonList(new RowSorter.SortKey(2, SortOrder.DESCENDING))));
        WButton clearSort = new WButton("Clear sort");
        clearSort.addActionListener(() -> sorter.setSortKeys(null));
        WCheckBox inStock = optionCheckBox("Only rows in stock", false,
                (only) -> sorter.setRowFilter(only ? RowFilter.regexFilter("true", 4) : null));
        WCheckBox cancel = optionCheckBox("Cancel sorting (Sorting)", false, (value) -> cancelSorting[0] = value);
        WCheckBox sortable = optionCheckBox("Category column is sortable", true, (value) -> sorter.setSortable(1, value));

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("TableRowSorter"));
        options.add(byPrice);
        options.add(clearSort);
        options.add(inStock);
        options.add(sortable);
        options.add(cancel);
        return GalleryScaffold.buildExample("Sorting and filtering (TableRowSorter / RowFilter / Sorting / Sorted)", body, options);
    }

    /** Editing: TableModel.isCellEditable / DefaultCellEditor / BeginningEdit / CellEditEnding / writing back to the model. */
    private static WComponent buildTableEditingExample() {
        DefaultTableModel model = new DefaultTableModel(
                Arrays.asList(
                        Arrays.<Object>asList(1, "Hanako Sato", "Development", 34, true),
                        Arrays.<Object>asList(2, "Ichiro Suzuki", "Sales", 41, true),
                        Arrays.<Object>asList(3, "Misaki Takahashi", "Design", 28, false),
                        Arrays.<Object>asList(4, "Ken Tanaka", "Development", 45, true)),
                Arrays.<Object>asList("ID", "Name", "Department", "Age", "Employed")) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 3) {
                    return Integer.class;
                }
                return columnIndex == 4 ? Boolean.class : String.class;
            }

            // The ID cannot be edited
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex != 0;
            }
        };
        WTable table = buildProductTable(model);
        table.setHeight(220.0);
        // Edit the department with a combo box and the age with a number box (the same idea as Swing's DefaultCellEditor)
        table.getColumnModel().getColumn(2).setCellEditor(
                new DefaultCellEditor(new WComboBox(Arrays.asList("Development", "Sales", "Design", "Admin"))));
        WSpinner ageSpinner = new WSpinner();
        ageSpinner.setMinimum(18.0);
        ageSpinner.setMaximum(70.0);
        ageSpinner.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(ageSpinner));

        WLabel log = new WLabel("Double-click or press F2 to start editing, Enter to commit, and Esc to cancel.");
        log.setTextWrapping(TextWrapping.WRAP);
        table.addBeginningEditListener((event) -> log.setText(
                "Edit started: " + table.getColumnName(event.getColumn()) + " (row " + (event.getModelRow() + 1) + ")"));
        table.addCellEditEndingListener((event) -> log.setText(
                "Edit ended: " + table.getColumnName(event.getColumn()) + " → " + event.getAction()));
        // The committed value is written back via TableModel.setValueAt, converted to the column type (getColumnClass)
        model.addTableModelListener((event) -> {
            if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() == event.getLastRow() && event.getColumn() >= 0) {
                Object value = model.getValueAt(event.getFirstRow(), event.getColumn());
                log.setText("Model updated: " + model.getColumnName(event.getColumn()) + " (row " + (event.getFirstRow() + 1)
                        + ") = " + value + " (" + (value == null ? "null" : value.getClass().getSimpleName()) + ")");
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(log);

        WCheckBox readOnly = optionCheckBox("Make the whole table read-only (IsReadOnly)", false, table::setReadOnly);
        WCheckBox ageEditable = optionCheckBox("Age column is editable (column IsReadOnly)", true,
                (value) -> table.getColumnModel().getColumn(3).setEditable(value));
        WButton commit = new WButton("Commit edit (CommitEdit)");
        commit.addActionListener(() -> log.setText("CommitEdit → " + table.stopCellEditing()));
        WButton cancel = new WButton("Cancel edit (CancelEdit)");
        cancel.addActionListener(() -> log.setText("CancelEdit → " + table.cancelCellEditing()));

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("Editing"));
        options.add(readOnly);
        options.add(ageEditable);
        options.add(commit);
        options.add(cancel);
        return GalleryScaffold.buildExample("Editing (isCellEditable / DefaultCellEditor / BeginningEdit / CellEditEnding)", body, options);
    }

    /** Grouping: groupBy / clearGrouping / ExpandAllGroups / CollapseAllGroups / GroupHeaderTemplate. */
    private static WComponent buildTableGroupingExample() {
        WTable table = buildProductTable(new ProductTableModel());
        table.groupBy(1); // Group by category

        WComboBox groupBy = new WComboBox(Arrays.asList("Category", "In stock", "Price range (300 yen or more / less)", "No grouping"));
        groupBy.setSelectedIndex(0);
        groupBy.addListSelectionListener(() -> {
            switch (groupBy.getSelectedIndex()) {
                case 0:
                    table.groupBy(1);
                    break;
                case 1:
                    table.groupBy(4);
                    break;
                case 2:
                    table.groupBy((model, row) -> ((Integer) model.getValueAt(row, 2)) >= 300 ? "300 yen or more" : "Under 300 yen");
                    break;
                default:
                    table.clearGrouping();
                    break;
            }
        });
        WButton expand = new WButton("Expand all");
        expand.addActionListener(table::expandAllGroups);
        WButton collapse = new WButton("Collapse all");
        collapse.addActionListener(table::collapseAllGroups);
        WCheckBox customHeader = optionCheckBox("Show headers with a template", false, (custom) -> table.setGroupHeaderTemplate(custom
                ? "<StackPanel Orientation=\"Horizontal\" Spacing=\"8\" Padding=\"8,4\">"
                        + "<TextBlock Text=\"{Binding KeyText}\" FontWeight=\"SemiBold\" Foreground=\"{ThemeResource AccentTextFillColorPrimaryBrush}\" />"
                        + "<TextBlock Text=\"{Binding ItemCount}\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />"
                        + "<TextBlock Text=\"items\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />"
                        + "</StackPanel>"
                : null));

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("Key to group by (GroupBy)"));
        options.add(groupBy);
        options.add(expand);
        options.add(collapse);
        options.add(customHeader);
        return GalleryScaffold.buildExample("Grouping (GroupBy / ExpandAllGroups / CollapseAllGroups / GroupHeaderTemplate)", table, options);
    }

    /** Cell display: TableCellRenderer / XAML cell templates / frozen columns / header and cell tooltips. */
    private static WComponent buildTableCellsExample() {
        DefaultTableModel model = new DefaultTableModel(
                Arrays.asList(
                        Arrays.<Object>asList("Review the design document", "In progress", 60, "Check the spec differences and sort out the review comments."),
                        Arrays.<Object>asList("Automate the tests", "Done", 100, "Add the E2E tests to CI and run them every night."),
                        Arrays.<Object>asList("Translate the documentation", "Not started", 0, "Translate the README and the API reference into English."),
                        Arrays.<Object>asList("Measure performance", "In progress", 35, "Measure display speed with a large data set (100,000 rows).")),
                Arrays.<Object>asList("Task", "Status", "Progress (%)", "Notes")) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Integer.class : String.class;
            }
        };
        WTable table = buildProductTable(model);
        table.setHeight(220.0);
        table.setWidth(520.0);

        TableColumn task = table.getColumnModel().getColumn(0);
        task.setFrozen(true); // The task name stays visible even when scrolling horizontally
        task.setPreferredWidth(180.0);
        task.setHeaderToolTip("This column is frozen at the left edge (FrozenEdge = Leading)");

        // Show the status as a colored label with a XAML template ({Binding} is the cell value)
        TableColumn state = table.getColumnModel().getColumn(1);
        state.setCellTemplate("<Border Background=\"{ThemeResource AccentFillColorDefaultBrush}\" CornerRadius=\"10\" "
                + "Padding=\"10,2\" Margin=\"8,0\" HorizontalAlignment=\"Left\">"
                + "<TextBlock Text=\"{Binding}\" Foreground=\"{ThemeResource TextOnAccentFillColorPrimaryBrush}\" /></Border>");
        state.setHeaderToolTip("Displayed with a XAML DataTemplate (TableViewTemplateColumn)");

        // Show the progress with a Java component (a progress bar) via a renderer
        TableColumn progress = table.getColumnModel().getColumn(2);
        progress.setPreferredWidth(160.0);
        progress.setCellRenderer((view, value, isSelected, hasFocus, row, column) -> {
            WProgressBar bar = new WProgressBar(0.0, 100.0, ((Integer) value).doubleValue());
            bar.setMargin(8.0, 0.0, 8.0, 0.0);
            bar.setVerticalAlignment(VerticalAlignment.CENTER);
            return bar;
        });
        progress.setHeaderToolTip("Displays a WProgressBar via TableCellRenderer (GenerateElementCore)");

        // The notes are long, so show the full text in a cell tooltip
        TableColumn memo = table.getColumnModel().getColumn(3);
        memo.setPreferredWidth(240.0);
        memo.setCellToolTipColumn(3);

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(GalleryScaffold.optionsLabel("Hover over a column header or a notes cell to show a tooltip. Columns beyond the table's width can be scrolled horizontally."));
        return GalleryScaffold.buildExample("Cell display (TableCellRenderer / cell templates / frozen columns / tooltips)", body);
    }

    /** Appearance: grid lines / header / density / row background / stripes / display when there are no rows. */
    private static WComponent buildTableAppearanceExample() {
        ProductTableModel model = new ProductTableModel();
        WTable table = buildProductTable(model);
        table.setEmptyText("No products to show");
        List<List<Object>> removedRows = new ArrayList<>();

        WCheckBox horizontal = optionCheckBox("Horizontal grid lines", table.getShowHorizontalLines(), table::setShowHorizontalLines);
        WCheckBox vertical = optionCheckBox("Vertical grid lines", table.getShowVerticalLines(), table::setShowVerticalLines);
        WCheckBox header = optionCheckBox("Show column headers", table.isHeaderVisible(), table::setHeaderVisible);
        WCheckBox striped = optionCheckBox("Stripes (AlternatingRowBackground)", false,
                (value) -> table.setAlternatingRowBackground(value ? new WColor(0x20, 0x80, 0x80, 0x80) : null));
        WCheckBox resizable = optionCheckBox("Resize columns by dragging (CanUserResizeColumns)", table.getCanUserResizeColumns(),
                table::setCanUserResizeColumns);
        WComboBox density = new WComboBox(Arrays.asList("COMPACT", "STANDARD", "COMFORTABLE"));
        density.setSelectedIndex(table.getDensity().ordinal());
        density.addListSelectionListener(() -> table.setDensity(TableDensity.values()[density.getSelectedIndex()]));
        WButton clearRows = new WButton("Remove all rows (EmptyTemplate)");
        clearRows.addActionListener(() -> {
            while (model.getRowCount() > 0) {
                List<Object> row = new ArrayList<>();
                for (int column = 0; column < model.getColumnCount(); column++) {
                    row.add(model.getValueAt(0, column));
                }
                removedRows.add(row);
                model.removeRow(0);
            }
        });
        WButton restoreRows = new WButton("Restore rows");
        restoreRows.addActionListener(() -> {
            for (List<Object> row : removedRows) {
                model.addRow(row);
            }
            removedRows.clear();
        });

        WPanel options = new WPanel(8.0);
        options.add(horizontal);
        options.add(vertical);
        options.add(header);
        options.add(striped);
        options.add(resizable);
        options.add(GalleryScaffold.optionsLabel("Density (Density)"));
        options.add(density);
        options.add(clearRows);
        options.add(restoreRows);
        return GalleryScaffold.buildExample("Appearance (GridLinesVisibility / HeadersVisibility / Density / stripes / empty display)", table, options);
    }

    /** Column operations: TableColumnModel (move / show and hide / add and remove) and column widths. */
    private static WComponent buildTableColumnsExample() {
        WTable table = buildProductTable(new ProductTableModel());
        WLabel status = new WLabel("");
        Runnable updateStatus = () -> {
            List<String> names = new ArrayList<>();
            for (int column = 0; column < table.getColumnCount(); column++) {
                names.add(table.getColumnName(column));
            }
            status.setText("Column order: " + String.join(" / ", names));
        };
        updateStatus.run();

        WButton moveLast = new WButton("Move the first column to the end");
        moveLast.addActionListener(() -> {
            table.moveColumn(0, table.getColumnCount() - 1);
            updateStatus.run();
        });
        WCheckBox hideCategory = optionCheckBox("Show the category column", true, (visible) -> {
            for (TableColumn column : table.getColumnModel().getColumns()) {
                if (column.getModelIndex() == 1) {
                    column.setVisible(visible);
                }
            }
        });
        TableColumn[] taxColumn = {null};
        DefaultTableCellRenderer rightAligned = new DefaultTableCellRenderer();
        rightAligned.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        WButton addTax = new WButton("Add / remove the price-with-tax column");
        addTax.addActionListener(() -> {
            if (taxColumn[0] == null) {
                TableColumn column = new TableColumn(2);
                column.setHeaderValue("Price with tax");
                column.setCellRenderer((view, value, isSelected, hasFocus, row, col) -> rightAligned.getTableCellRendererComponent(
                        view, (int) (((Integer) value) * 1.08) + " yen", isSelected, hasFocus, row, col));
                table.addColumn(column);
                taxColumn[0] = column;
            } else {
                table.removeColumn(taxColumn[0]);
                taxColumn[0] = null;
            }
            updateStatus.run();
        });
        WButton widen = new WButton("Set the product column width to 200");
        widen.addActionListener(() -> {
            for (TableColumn column : table.getColumnModel().getColumns()) {
                if (column.getModelIndex() == 0) {
                    column.setPreferredWidth(200.0);
                }
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(status);

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("TableColumnModel"));
        options.add(moveLast);
        options.add(hideCategory);
        options.add(addTax);
        options.add(widen);
        return GalleryScaffold.buildExample("Column operations (TableColumnModel / TableColumn)", body, options);
    }

    // endregion

    // region TreeView

    /** TreeView page: lines up demos for trying out WTree's various features. */
    static WComponent buildTreeViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TreeView", "A tree that can expand and collapse hierarchical data. Try out WTree's various features.");

        page.add(buildSimpleTreeExample());
        page.add(buildTreeMultiSelectExample());
        page.add(buildTreeExpandCollapseExample());
        return page;
    }

    /** Builds the same sample tree (Work Documents / Personal Documents) as the real Gallery. */
    private static WTree buildSampleTree() {
        WTree tree = new WTree();
        tree.setWidth(345.0);
        // Pin it to the left so the tree doesn't shift toward the center if the panel widens (e.g. from a long label)
        tree.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WTreeNode workFolder = new WTreeNode("Work Documents");
        workFolder.add(new WTreeNode("XYZ Functional Spec"));
        workFolder.add(new WTreeNode("Feature Schedule"));
        workFolder.setExpanded(true);

        WTreeNode remodelFolder = new WTreeNode("Home Remodel");
        remodelFolder.add(new WTreeNode("Contractor Contact Info"));
        remodelFolder.add(new WTreeNode("Paint Color Scheme"));
        remodelFolder.setExpanded(true);

        WTreeNode personalFolder = new WTreeNode("Personal Documents");
        personalFolder.add(remodelFolder);
        personalFolder.setExpanded(true);

        tree.addRootNode(workFolder);
        tree.addRootNode(personalFolder);
        return tree;
    }

    /** Basic tree: drag-to-reorder and responding to node clicks (ItemInvoked). */
    private static WComponent buildSimpleTreeExample() {
        WLabel result = new WLabel("Clicked: none");
        result.setTextWrapping(TextWrapping.WRAP);

        WTree tree = buildSampleTree();
        tree.setCanDragItems(true);
        tree.setCanReorderItems(true);
        tree.addItemInvokedListener(node -> {
            result.setText(node == null
                    ? "Clicked: none"
                    : "Click: " + node.getText() + " (depth = " + node.getDepth() + ")");
        });

        WPanel body = new WPanel(8.0);
        body.add(tree);
        body.add(result);
        return GalleryScaffold.buildExample("Simple tree (drag & drop reordering / ItemInvoked)", body);
    }

    /** Multiple selection: checkboxes from SelectionMode = MULTIPLE, plus SelectAll / SelectedNodes. */
    private static WComponent buildTreeMultiSelectExample() {
        WTree tree = buildSampleTree();
        tree.setSelectionMode(TreeViewSelectionMode.MULTIPLE);

        WLabel result = new WLabel("Selected: none");
        result.setTextWrapping(TextWrapping.WRAP);
        WButton showButton = new WButton("Show selection");
        showButton.addActionListener(() -> {
            StringBuilder names = new StringBuilder();
            for (WTreeNode node : tree.getSelectedNodes()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(node.getText());
            }
            result.setText(names.length() == 0 ? "Selected: none" : "Selected: " + names);
        });
        WButton selectAllButton = new WButton("Select all");
        selectAllButton.addActionListener(() -> {
            tree.selectAll();
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(showButton);
        buttons.add(selectAllButton);

        WPanel body = new WPanel(8.0);
        body.add(tree);
        body.add(buttons);
        body.add(result);
        return GalleryScaffold.buildExample("Multiple selection (SelectionMode / SelectAll / SelectedNodes)", body);
    }

    /** Expand and collapse: the Expand / Collapse methods and the Expanding / Collapsed events. */
    private static WComponent buildTreeExpandCollapseExample() {
        WLabel log = new WLabel("Event: none");
        log.setTextWrapping(TextWrapping.WRAP);

        WTree tree = buildSampleTree();
        tree.addExpandingListener(node -> {
            log.setText("Event: Expanding (" + (node != null ? node.getText() : null) + ")");
        });
        tree.addCollapsedListener(node -> {
            log.setText("Event: Collapsed (" + (node != null ? node.getText() : null) + ")");
        });

        WButton expandButton = new WButton("Expand all");
        expandButton.addActionListener(() -> {
            for (WTreeNode root : tree.getRootNodes()) {
                tree.expand(root);
            }
        });
        WButton collapseButton = new WButton("Collapse all");
        collapseButton.addActionListener(() -> {
            for (WTreeNode root : tree.getRootNodes()) {
                tree.collapse(root);
            }
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(expandButton);
        buttons.add(collapseButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(tree);
        body.add(log);
        return GalleryScaffold.buildExample("Expand and collapse (Expand / Collapse / Expanding / Collapsed)", body);
    }

    // endregion
}
