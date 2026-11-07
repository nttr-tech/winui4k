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
import com.appkitbox.winui4k.WItemEvent;
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
 * Collections category: demo pages for ItemsView / ListBox / ListView / Table / Tree.
 */
final class CollectionsPages {
    private CollectionsPages() {
    }

    // region ItemsView

    /** The ItemsView page: lines up demos for trying out WItemsView's various features. */
    static WComponent buildItemsViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ItemsView",
                Messages.get("itemsview.description"));

        page.add(buildUniformGridItemsViewExample());
        return page;
    }

    /** A card grid: UniformGridLayout + ItemContainer + ItemInvoked. */
    private static WComponent buildUniformGridItemsViewExample() {
        WLabel result = new WLabel(Messages.get("itemsview.click.none"));

        List<String> fruits = Arrays.asList(
                Messages.get("itemsview.fruit.apple"),
                Messages.get("itemsview.fruit.mandarin"),
                Messages.get("itemsview.fruit.grapes"),
                Messages.get("itemsview.fruit.peach"),
                Messages.get("itemsview.fruit.cherry"),
                Messages.get("itemsview.fruit.banana"));
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
            result.setText(Messages.get("itemsview.click.status", fruits.get(index), String.valueOf(index)));
        });
        itemsView.setWidth(520.0);
        itemsView.setHeight(200.0);
        itemsView.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);

        WPanel body = new WPanel(10.0);
        body.add(itemsView);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("itemsview.example.cardgrid.title"), body);
    }

    // endregion

    // region ListBox

    /** The ListBox page: lines up demos for trying out WListBox's various features. */
    static WComponent buildListBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ListBox", Messages.get("listbox.description"));

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
        output.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);

        WListBox listBox = new WListBox(new ArrayList<>(colors.keySet()));
        listBox.setWidth(200.0);
        listBox.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
        listBox.addListSelectionListener(() -> {
            output.setBackground(colors.get(listBox.getSelectedItem()));
        });

        WPanel body = new WPanel(10.0);
        body.add(listBox);
        body.add(output);
        return GalleryScaffold.buildExample(Messages.get("listbox.example.inline.title"), body);
    }

    /** Font selection: mirrors example 2 from the official Gallery (fixed height + initial selection + selection changes the font). */
    private static WComponent buildListBoxFontExample() {
        List<String> fonts = Arrays.asList("Arial", "Comic Sans MS", "Courier New", "Segoe UI", "Times New Roman");

        WLabel output = new WLabel("You can set the font used for this text.");
        output.setForeground(GalleryTheme.TEXT_SECONDARY());

        WListBox listBox = new WListBox(fonts);
        listBox.setWidth(200.0);
        listBox.setHeight(164.0);
        listBox.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
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
        return GalleryScaffold.buildExample(Messages.get("listbox.example.font.title"), body);
    }

    /** Selection mode: switching selectionMode plus selectAll / selectedItems / scrollIntoView. */
    private static WComponent buildListBoxSelectionModeExample() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            items.add(Messages.get("listbox.option", String.valueOf(i)));
        }
        WListBox listBox = new WListBox(items);
        listBox.setWidth(240.0);
        listBox.setHeight(200.0);
        listBox.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);

        WLabel selection = new WLabel(Messages.get("listbox.selection.none"));
        listBox.addListSelectionListener(() -> {
            List<String> selectedItems = listBox.getSelectedItems();
            selection.setText(selectedItems.isEmpty()
                    ? Messages.get("listbox.selection.none")
                    : Messages.get("listbox.selection.status", String.join(", ", selectedItems)));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (SelectionMode selectionMode : SelectionMode.values()) {
            WButton button = new WButton(selectionMode.name());
            button.addActionListener(() -> {
                listBox.setSelectionMode(selectionMode);
            });
            buttons.add(button);
        }
        WButton selectAllButton = new WButton(Messages.get("listbox.selectall"));
        selectAllButton.addActionListener(() -> {
            listBox.selectAll();
        });
        buttons.add(selectAllButton);
        WButton scrollButton = new WButton(Messages.get("listbox.scrolltoend"));
        scrollButton.addActionListener(() -> {
            listBox.scrollIntoView(listBox.getItemCount() - 1);
        });
        buttons.add(scrollButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(listBox);
        body.add(selection);
        return GalleryScaffold.buildExample(Messages.get("listbox.example.selectionmode.title"), body);
    }

    // endregion

    // region ListView

    /** The ListView page: lines up demos for trying out WList's various features. */
    static WComponent buildListViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ListView", Messages.get("listview.description"));

        page.add(buildSimpleListExample());
        page.add(buildListItemOperationsExample());
        page.add(buildListSelectionModeExample());
        page.add(buildListItemClickExample());
        return page;
    }

    /** A basic list: responding to selection changes (SelectionChanged). */
    private static WComponent buildSimpleListExample() {
        WLabel result = new WLabel(Messages.get("listview.selection.none"));

        WList list = new WList(Arrays.asList(
                Messages.get("listview.fruit.apple"),
                Messages.get("listview.fruit.mandarin"),
                Messages.get("listview.fruit.grapes"),
                Messages.get("listview.fruit.peach"),
                Messages.get("listview.fruit.cherry")));
        list.setWidth(240.0);
        list.addListSelectionListener(() -> {
            String item = list.getSelectedItem();
            result.setText(item == null
                    ? Messages.get("listview.selection.none")
                    : Messages.get("listview.selection.status", item, String.valueOf(list.getSelectedIndex())));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(list);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("listview.example.simple.title"), row);
    }

    /** Adding and removing items: addItem / removeItem / removeAllItems / itemCount. */
    private static WComponent buildListItemOperationsExample() {
        // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
        WList list = new WList(Arrays.asList(Messages.get("listview.item", "1"), Messages.get("listview.item", "2"), Messages.get("listview.item", "3")));
        list.setWidth(240.0);
        list.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);

        WLabel count = new WLabel(Messages.get("listview.itemcount", String.valueOf(list.getItemCount())));
        WTextField input = new WTextField();
        input.setPlaceholderText(Messages.get("listview.newitem.placeholder"));
        input.setWidth(200.0);

        WButton addButton = new WButton(Messages.get("listview.add"));
        addButton.addActionListener(() -> {
            if (!input.getText().isEmpty()) {
                list.addItem(input.getText());
                input.setText("");
                count.setText(Messages.get("listview.itemcount", String.valueOf(list.getItemCount())));
            }
        });

        WButton removeButton = new WButton(Messages.get("listview.removeselected"));
        removeButton.addActionListener(() -> {
            int index = list.getSelectedIndex();
            if (index >= 0) {
                list.removeItem(index);
                count.setText(Messages.get("listview.itemcount", String.valueOf(list.getItemCount())));
            }
        });

        WButton clearButton = new WButton(Messages.get("listview.removeall"));
        clearButton.addActionListener(() -> {
            list.removeAllItems();
            count.setText(Messages.get("listview.itemcount", String.valueOf(list.getItemCount())));
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
        return GalleryScaffold.buildExample(Messages.get("listview.example.operations.title"), body);
    }

    /** Selection mode: switching selectionMode and selectAll. */
    private static WComponent buildListSelectionModeExample() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            items.add(Messages.get("listview.option", String.valueOf(i)));
        }
        WList list = new WList(items);
        list.setWidth(240.0);
        list.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);

        WLabel mode = new WLabel(Messages.get("listview.selectionmode.status", String.valueOf(list.getSelectionMode())));

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (ListViewSelectionMode selectionMode : ListViewSelectionMode.values()) {
            WButton button = new WButton(selectionMode.name());
            button.addActionListener(() -> {
                list.setSelectionMode(selectionMode);
                mode.setText(Messages.get("listview.selectionmode.status", String.valueOf(list.getSelectionMode())));
            });
            buttons.add(button);
        }
        WButton selectAllButton = new WButton(Messages.get("listview.selectall"));
        selectAllButton.addActionListener(() -> {
            list.selectAll();
        });
        buttons.add(selectAllButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(list);
        body.add(mode);
        return GalleryScaffold.buildExample(Messages.get("listview.example.selectionmode.title"), body);
    }

    /** ItemClick: enabling isItemClickEnabled and receiving the clicked item. */
    private static WComponent buildListItemClickExample() {
        WLabel result = new WLabel(Messages.get("listview.click.none"));

        WList list = new WList(Arrays.asList(
                Messages.get("listview.folder.documents"),
                Messages.get("listview.folder.pictures"),
                Messages.get("listview.folder.music"),
                Messages.get("listview.folder.videos")));
        list.setWidth(240.0);
        list.setItemClickEnabled(true);
        list.addItemClickListener(item -> {
            result.setText(Messages.get("listview.click.status", item));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(list);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("listview.example.itemclick.title"), row);
    }

    // endregion

    // region Table page

    /** The Table page: lines up demos for trying out WTable's (WinUI 3's TableView, Windows App SDK 2.6 experimental) various features. */
    static WComponent buildTablePage() {
        WPanel page = GalleryScaffold.buildPage(
                "Table",
                Messages.get("table.description"));

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
                            Arrays.<Object>asList(Messages.get("table.product.apple"), Messages.get("table.category.fruit"), 150, 12, true),
                            Arrays.<Object>asList(Messages.get("table.product.mandarin"), Messages.get("table.category.fruit"), 80, 30, true),
                            Arrays.<Object>asList(Messages.get("table.product.grapes"), Messages.get("table.category.fruit"), 480, 0, false),
                            Arrays.<Object>asList(Messages.get("table.product.cabbage"), Messages.get("table.category.vegetable"), 200, 8, true),
                            Arrays.<Object>asList(Messages.get("table.product.carrot"), Messages.get("table.category.vegetable"), 60, 25, true),
                            Arrays.<Object>asList(Messages.get("table.product.tomato"), Messages.get("table.category.vegetable"), 120, 0, false),
                            Arrays.<Object>asList(Messages.get("table.product.milk"), Messages.get("table.category.dairy"), 230, 15, true),
                            Arrays.<Object>asList(Messages.get("table.product.cheese"), Messages.get("table.category.dairy"), 380, 4, true)),
                    Arrays.<Object>asList(
                            Messages.get("table.column.name"),
                            Messages.get("table.column.category"),
                            Messages.get("table.column.price"),
                            Messages.get("table.column.stock"),
                            Messages.get("table.column.instock")));
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
        table.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
        return table;
    }

    /** Creates a check box together with its change handler (for the Options panel). */
    private static WCheckBox optionCheckBox(String text, boolean checked, Consumer<Boolean> onChanged) {
        WCheckBox checkBox = new WCheckBox(text);
        checkBox.setChecked(checked);
        checkBox.addItemListener(e -> onChanged.accept(e.getStateChange() == WItemEvent.SELECTED));
        return checkBox;
    }

    /** Basics: displaying a TableModel and selecting rows (SelectionMode / Select / SelectionChanged / view-model conversion). */
    private static WComponent buildTableBasicExample() {
        WTable table = buildProductTable(new ProductTableModel());
        WLabel result = new WLabel(Messages.get("table.basic.selection.none"));
        table.addRowSelectionListener(() -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                result.setText(Messages.get("table.basic.selection.none"));
            } else {
                int modelRow = table.convertRowIndexToModel(row);
                result.setText(Messages.get("table.basic.selection.status", String.valueOf(table.getModel().getValueAt(modelRow, 0)),
                        String.valueOf(row), String.valueOf(modelRow)));
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(result);

        WComboBox selectionMode = new WComboBox(Arrays.asList(Messages.get("table.basic.mode.single"), Messages.get("table.basic.mode.none")));
        selectionMode.setSelectedIndex(0);
        selectionMode.addListSelectionListener(() -> table.setSelectionMode(
                selectionMode.getSelectedIndex() == 1 ? TableSelectionMode.NONE : TableSelectionMode.SINGLE));
        WButton selectThird = new WButton(Messages.get("table.basic.selectthird"));
        selectThird.addActionListener(() -> table.selectRow(2));
        WButton clear = new WButton(Messages.get("table.basic.clearselection"));
        clear.addActionListener(table::clearSelection);

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel(Messages.get("table.basic.selectionmode.label")));
        options.add(selectionMode);
        options.add(selectThird);
        options.add(clear);
        return GalleryScaffold.buildExample(Messages.get("table.example.basic.title"), body, options);
    }

    /** Sorting and filtering: TableRowSorter (header click / SortKeys / Comparator / RowFilter) and Sorting / Sorted. */
    private static WComponent buildTableSortFilterExample() {
        ProductTableModel model = new ProductTableModel();
        WTable table = buildProductTable(model);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        // Sort product names shortest first (alphabetically when the lengths are equal)
        sorter.setComparator(0, Comparator.comparingInt(String::length).thenComparing(Comparator.<String>naturalOrder()));
        table.setRowSorter(sorter);

        WLabel status = new WLabel(Messages.get("table.sort.hint"));
        status.setTextWrapping(TextWrapping.WRAP);
        boolean[] cancelSorting = {false};
        table.addSortingListener((event) -> {
            event.setCanceled(cancelSorting[0]);
            status.setText(cancelSorting[0]
                    ? Messages.get("table.sort.canceled", table.getColumnName(event.getColumn()))
                    : Messages.get("table.sort.sorting", table.getColumnName(event.getColumn()), String.valueOf(event.getSortOrder())));
        });
        table.addSortedListener((event) -> status.setText(Messages.get("table.sort.sorted", table.getColumnName(event.getColumn()),
                String.valueOf(event.getSortOrder()), String.valueOf(table.getRowCount()))));

        WTextField filterText = new WTextField();
        filterText.setPlaceholderText(Messages.get("table.sort.filter.placeholder"));
        filterText.setWidth(240.0);
        filterText.addTextChangedListener((text) -> {
            sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter(text, 0));
            status.setText(Messages.get("table.sort.filter.status", String.valueOf(table.getRowCount()), String.valueOf(model.getRowCount())));
        });

        WPanel body = new WPanel(8.0);
        body.add(filterText);
        body.add(table);
        body.add(status);

        WButton byPrice = new WButton(Messages.get("table.sort.byprice"));
        byPrice.addActionListener(() -> sorter.setSortKeys(
                Collections.singletonList(new RowSorter.SortKey(2, SortOrder.DESCENDING))));
        WButton clearSort = new WButton(Messages.get("table.sort.clear"));
        clearSort.addActionListener(() -> sorter.setSortKeys(null));
        WCheckBox inStock = optionCheckBox(Messages.get("table.sort.instockonly"), false,
                (only) -> sorter.setRowFilter(only ? RowFilter.regexFilter("true", 4) : null));
        WCheckBox cancel = optionCheckBox(Messages.get("table.sort.cancel"), false, (value) -> cancelSorting[0] = value);
        WCheckBox sortable = optionCheckBox(Messages.get("table.sort.categorysortable"), true, (value) -> sorter.setSortable(1, value));

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("TableRowSorter"));
        options.add(byPrice);
        options.add(clearSort);
        options.add(inStock);
        options.add(sortable);
        options.add(cancel);
        return GalleryScaffold.buildExample(Messages.get("table.example.sortfilter.title"), body, options);
    }

    /** Editing: TableModel.isCellEditable / DefaultCellEditor / BeginningEdit / CellEditEnding / writing back to the model. */
    private static WComponent buildTableEditingExample() {
        DefaultTableModel model = new DefaultTableModel(
                Arrays.asList(
                        Arrays.<Object>asList(1, Messages.get("table.edit.person1"), Messages.get("table.edit.dept.development"), 34, true),
                        Arrays.<Object>asList(2, Messages.get("table.edit.person2"), Messages.get("table.edit.dept.sales"), 41, true),
                        Arrays.<Object>asList(3, Messages.get("table.edit.person3"), Messages.get("table.edit.dept.design"), 28, false),
                        Arrays.<Object>asList(4, Messages.get("table.edit.person4"), Messages.get("table.edit.dept.development"), 45, true)),
                Arrays.<Object>asList("ID", Messages.get("table.edit.column.name"), Messages.get("table.edit.column.department"),
                        Messages.get("table.edit.column.age"), Messages.get("table.edit.column.active"))) {
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
                new DefaultCellEditor(new WComboBox(Arrays.asList(Messages.get("table.edit.dept.development"), Messages.get("table.edit.dept.sales"),
                        Messages.get("table.edit.dept.design"), Messages.get("table.edit.dept.admin")))));
        WSpinner ageSpinner = new WSpinner();
        ageSpinner.setMinimum(18.0);
        ageSpinner.setMaximum(70.0);
        ageSpinner.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(ageSpinner));

        WLabel log = new WLabel(Messages.get("table.edit.hint"));
        log.setTextWrapping(TextWrapping.WRAP);
        table.addBeginningEditListener((event) -> log.setText(
                Messages.get("table.edit.beginning", table.getColumnName(event.getColumn()), String.valueOf(event.getModelRow() + 1))));
        table.addCellEditEndingListener((event) -> log.setText(
                Messages.get("table.edit.ending", table.getColumnName(event.getColumn()), String.valueOf(event.getAction()))));
        // The committed value is written back via TableModel.setValueAt, converted to the column type (getColumnClass)
        model.addTableModelListener((event) -> {
            if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() == event.getLastRow() && event.getColumn() >= 0) {
                Object value = model.getValueAt(event.getFirstRow(), event.getColumn());
                log.setText(Messages.get("table.edit.modelupdated", model.getColumnName(event.getColumn()),
                        String.valueOf(event.getFirstRow() + 1), String.valueOf(value),
                        value == null ? "null" : value.getClass().getSimpleName()));
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(log);

        WCheckBox readOnly = optionCheckBox(Messages.get("table.edit.readonly"), false, table::setReadOnly);
        WCheckBox ageEditable = optionCheckBox(Messages.get("table.edit.ageeditable"), true,
                (value) -> table.getColumnModel().getColumn(3).setEditable(value));
        WButton commit = new WButton(Messages.get("table.edit.commit"));
        commit.addActionListener(() -> log.setText("CommitEdit → " + table.stopCellEditing()));
        WButton cancel = new WButton(Messages.get("table.edit.cancel"));
        cancel.addActionListener(() -> log.setText("CancelEdit → " + table.cancelCellEditing()));

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel(Messages.get("table.edit.label")));
        options.add(readOnly);
        options.add(ageEditable);
        options.add(commit);
        options.add(cancel);
        return GalleryScaffold.buildExample(Messages.get("table.example.editing.title"), body, options);
    }

    /** Grouping: groupBy / clearGrouping / ExpandAllGroups / CollapseAllGroups / GroupHeaderTemplate. */
    private static WComponent buildTableGroupingExample() {
        WTable table = buildProductTable(new ProductTableModel());
        table.groupBy(1); // Group by category

        WComboBox groupBy = new WComboBox(Arrays.asList(Messages.get("table.group.category"), Messages.get("table.group.instock"),
                Messages.get("table.group.pricerange"), Messages.get("table.group.none")));
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
                    table.groupBy((model, row) -> ((Integer) model.getValueAt(row, 2)) >= 300 
                            ? Messages.get("table.group.over300")
                            : Messages.get("table.group.under300"));
                    break;
                default:
                    table.clearGrouping();
                    break;
            }
        });
        WButton expand = new WButton(Messages.get("table.group.expandall"));
        expand.addActionListener(table::expandAllGroups);
        WButton collapse = new WButton(Messages.get("table.group.collapseall"));
        collapse.addActionListener(table::collapseAllGroups);
        WCheckBox customHeader = optionCheckBox(Messages.get("table.group.customheader"), false, (custom) -> table.setGroupHeaderTemplate(custom
                ? "<StackPanel Orientation=\"Horizontal\" Spacing=\"8\" Padding=\"8,4\">"
                        + "<TextBlock Text=\"{Binding KeyText}\" FontWeight=\"SemiBold\" Foreground=\"{ThemeResource AccentTextFillColorPrimaryBrush}\" />"
                        + "<TextBlock Text=\"{Binding ItemCount}\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />"
                        + "<TextBlock Text=\"" + Messages.get("table.group.items") + "\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />"
                        + "</StackPanel>"
                : null));

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel(Messages.get("table.group.key.label")));
        options.add(groupBy);
        options.add(expand);
        options.add(collapse);
        options.add(customHeader);
        return GalleryScaffold.buildExample(Messages.get("table.example.grouping.title"), table, options);
    }

    /** Cell display: TableCellRenderer / XAML cell templates / frozen columns / header and cell tooltips. */
    private static WComponent buildTableCellsExample() {
        DefaultTableModel model = new DefaultTableModel(
                Arrays.asList(
                        Arrays.<Object>asList(Messages.get("table.cells.task1"), Messages.get("table.cells.state.inprogress"), 60, Messages.get("table.cells.memo1")),
                        Arrays.<Object>asList(Messages.get("table.cells.task2"), Messages.get("table.cells.state.done"), 100, Messages.get("table.cells.memo2")),
                        Arrays.<Object>asList(Messages.get("table.cells.task3"), Messages.get("table.cells.state.notstarted"), 0, Messages.get("table.cells.memo3")),
                        Arrays.<Object>asList(Messages.get("table.cells.task4"), Messages.get("table.cells.state.inprogress"), 35, Messages.get("table.cells.memo4"))),
                Arrays.<Object>asList(Messages.get("table.cells.column.task"), Messages.get("table.cells.column.state"),
                        Messages.get("table.cells.column.progress"), Messages.get("table.cells.column.memo"))) {
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
        task.setHeaderToolTip(Messages.get("table.cells.task.tooltip"));

        // Show the status as a colored label with a XAML template ({Binding} is the cell value)
        TableColumn state = table.getColumnModel().getColumn(1);
        state.setCellTemplate("<Border Background=\"{ThemeResource AccentFillColorDefaultBrush}\" CornerRadius=\"10\" "
                + "Padding=\"10,2\" Margin=\"8,0\" HorizontalAlignment=\"Left\">"
                + "<TextBlock Text=\"{Binding}\" Foreground=\"{ThemeResource TextOnAccentFillColorPrimaryBrush}\" /></Border>");
        state.setHeaderToolTip(Messages.get("table.cells.state.tooltip"));

        // Show the progress with a Java component (a progress bar) via a renderer
        TableColumn progress = table.getColumnModel().getColumn(2);
        progress.setPreferredWidth(160.0);
        progress.setCellRenderer((view, value, isSelected, hasFocus, row, column) -> {
            WProgressBar bar = new WProgressBar(0.0, 100.0, ((Integer) value).doubleValue());
            bar.setMargin(8.0, 0.0, 8.0, 0.0);
            bar.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
            return bar;
        });
        progress.setHeaderToolTip(Messages.get("table.cells.progress.tooltip"));

        // The notes are long, so show the full text in a cell tooltip
        TableColumn memo = table.getColumnModel().getColumn(3);
        memo.setPreferredWidth(240.0);
        memo.setCellToolTipColumn(3);

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(GalleryScaffold.optionsLabel(Messages.get("table.cells.note")));
        return GalleryScaffold.buildExample(Messages.get("table.example.cells.title"), body);
    }

    /** Appearance: grid lines / header / density / row background / stripes / display when there are no rows. */
    private static WComponent buildTableAppearanceExample() {
        ProductTableModel model = new ProductTableModel();
        WTable table = buildProductTable(model);
        table.setEmptyText(Messages.get("table.appearance.empty"));
        List<List<Object>> removedRows = new ArrayList<>();

        WCheckBox horizontal = optionCheckBox(Messages.get("table.appearance.horizontal"), table.getShowHorizontalLines(), table::setShowHorizontalLines);
        WCheckBox vertical = optionCheckBox(Messages.get("table.appearance.vertical"), table.getShowVerticalLines(), table::setShowVerticalLines);
        WCheckBox header = optionCheckBox(Messages.get("table.appearance.header"), table.isHeaderVisible(), table::setHeaderVisible);
        WCheckBox striped = optionCheckBox(Messages.get("table.appearance.striped"), false,
                (value) -> table.setAlternatingRowBackground(value ? new WColor(0x20, 0x80, 0x80, 0x80) : null));
        WCheckBox resizable = optionCheckBox(Messages.get("table.appearance.resizable"), table.getCanUserResizeColumns(),
                table::setCanUserResizeColumns);
        WComboBox density = new WComboBox(Arrays.asList("COMPACT", "STANDARD", "COMFORTABLE"));
        density.setSelectedIndex(table.getDensity().ordinal());
        density.addListSelectionListener(() -> table.setDensity(TableDensity.values()[density.getSelectedIndex()]));
        WButton clearRows = new WButton(Messages.get("table.appearance.clearrows"));
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
        WButton restoreRows = new WButton(Messages.get("table.appearance.restorerows"));
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
        options.add(GalleryScaffold.optionsLabel(Messages.get("table.appearance.density.label")));
        options.add(density);
        options.add(clearRows);
        options.add(restoreRows);
        return GalleryScaffold.buildExample(Messages.get("table.example.appearance.title"), table, options);
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
            status.setText(Messages.get("table.columns.order", String.join(" / ", names)));
        };
        updateStatus.run();

        WButton moveLast = new WButton(Messages.get("table.columns.movelast"));
        moveLast.addActionListener(() -> {
            table.moveColumn(0, table.getColumnCount() - 1);
            updateStatus.run();
        });
        WCheckBox hideCategory = optionCheckBox(Messages.get("table.columns.showcategory"), true, (visible) -> {
            for (TableColumn column : table.getColumnModel().getColumns()) {
                if (column.getModelIndex() == 1) {
                    column.setVisible(visible);
                }
            }
        });
        TableColumn[] taxColumn = {null};
        DefaultTableCellRenderer rightAligned = new DefaultTableCellRenderer();
        rightAligned.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        WButton addTax = new WButton(Messages.get("table.columns.addtax"));
        addTax.addActionListener(() -> {
            if (taxColumn[0] == null) {
                TableColumn column = new TableColumn(2);
                column.setHeaderValue(Messages.get("table.columns.tax.header"));
                column.setCellRenderer((view, value, isSelected, hasFocus, row, col) -> rightAligned.getTableCellRendererComponent(
                        view, Messages.get("table.columns.tax.value", String.valueOf((int) (((Integer) value) * 1.08))), isSelected, hasFocus, row, col));
                table.addColumn(column);
                taxColumn[0] = column;
            } else {
                table.removeColumn(taxColumn[0]);
                taxColumn[0] = null;
            }
            updateStatus.run();
        });
        WButton widen = new WButton(Messages.get("table.columns.widen"));
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
        return GalleryScaffold.buildExample(Messages.get("table.example.columns.title"), body, options);
    }

    // endregion

    // region Tree page

    /** The Tree page: lines up demos for trying out WTree's various features. */
    static WComponent buildTreePage() {
        WPanel page = GalleryScaffold.buildPage(
                "Tree", Messages.get("tree.description"));

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
        tree.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);

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
        WLabel result = new WLabel(Messages.get("tree.click.none"));
        result.setTextWrapping(TextWrapping.WRAP);

        WTree tree = buildSampleTree();
        tree.setCanDragItems(true);
        tree.setCanReorderItems(true);
        tree.addItemInvokedListener(node -> {
            result.setText(node == null
                    ? Messages.get("tree.click.none")
                    : Messages.get("tree.click.status", node.getText(), String.valueOf(node.getDepth())));
        });

        WPanel body = new WPanel(8.0);
        body.add(tree);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("tree.example.simple.title"), body);
    }

    /** Multiple selection: checkboxes from SelectionMode = MULTIPLE, plus SelectAll / SelectedNodes. */
    private static WComponent buildTreeMultiSelectExample() {
        WTree tree = buildSampleTree();
        tree.setSelectionMode(TreeViewSelectionMode.MULTIPLE);

        WLabel result = new WLabel(Messages.get("tree.selected.none"));
        result.setTextWrapping(TextWrapping.WRAP);
        WButton showButton = new WButton(Messages.get("tree.showselection"));
        showButton.addActionListener(() -> {
            StringBuilder names = new StringBuilder();
            for (WTreeNode node : tree.getSelectedNodes()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(node.getText());
            }
            result.setText(names.length() == 0 ? Messages.get("tree.selected.none") : Messages.get("tree.selected.status", names.toString()));
        });
        WButton selectAllButton = new WButton(Messages.get("tree.selectall"));
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
        return GalleryScaffold.buildExample(Messages.get("tree.example.multiselect.title"), body);
    }

    /** Expand and collapse: the Expand / Collapse methods and the Expanding / Collapsed events. */
    private static WComponent buildTreeExpandCollapseExample() {
        WLabel log = new WLabel(Messages.get("tree.event.none"));
        log.setTextWrapping(TextWrapping.WRAP);

        WTree tree = buildSampleTree();
        tree.addExpandingListener(node -> {
            log.setText(Messages.get("tree.event.expanding", String.valueOf(node != null ? node.getText() : null)));
        });
        tree.addCollapsedListener(node -> {
            log.setText(Messages.get("tree.event.collapsed", String.valueOf(node != null ? node.getText() : null)));
        });

        WButton expandButton = new WButton(Messages.get("tree.expandall"));
        expandButton.addActionListener(() -> {
            for (WTreeNode root : tree.getRootNodes()) {
                tree.expand(root);
            }
        });
        WButton collapseButton = new WButton(Messages.get("tree.collapseall"));
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
        return GalleryScaffold.buildExample(Messages.get("tree.example.expandcollapse.title"), body);
    }

    // endregion
}
