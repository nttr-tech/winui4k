package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.ItemsViewSelectionMode;
import com.appkitbox.winui4k.ListViewSelectionMode;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SelectionMode;
import com.appkitbox.winui4k.SortDirection;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.TreeViewSelectionMode;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WItemContainer;
import com.appkitbox.winui4k.WItemsView;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WListBox;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WTable;
import com.appkitbox.winui4k.WTableColumn;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WTreeNode;
import com.appkitbox.winui4k.WUniformGridLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*
 * Collections category: demo pages for ItemsView / ListBox / ListView / TableView / TreeView.
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

    // region TableView

    /** The TableView page: lines up demos for trying out WTable's various features. */
    static WComponent buildTableViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TableView",
                "A table that displays data in rows and columns. Try out WTable's various "
                        + "features, implemented on top of ListView based on the design of WinUI.TableView.");

        page.add(buildSimpleTableExample());
        page.add(buildTableSortExample());
        page.add(buildTableRowOperationsExample());
        return page;
    }

    /** Sample data for the TableView demos (product name, price, quantity). */
    private static WTable buildProductTable() {
        WTable table = new WTable(Arrays.asList(
                new WTableColumn("Product", 160.0),
                new WTableColumn("Price", 100.0),
                new WTableColumn("Quantity", 100.0)));
        table.addRow("Apple", "150", "12");
        table.addRow("Orange", "80", "30");
        table.addRow("Grape", "480", "5");
        table.addRow("Peach", "320", "8");
        table.addRow("Cherry", "600", "3");
        table.setWidth(400.0);
        return table;
    }

    /** A basic table: responding to row selection (SelectionChanged). */
    private static WComponent buildSimpleTableExample() {
        WLabel result = new WLabel("Selected: none");

        WTable table = buildProductTable();
        table.addRowSelectionListener(() -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                result.setText("Selection: none");
            } else {
                result.setText("Selection: " + table.getValueAt(row, 0) + " (row = " + row + ")");
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(table);
        body.add(result);
        return GalleryScaffold.buildExample("A basic table (row selection)", body);
    }

    /** Sorting columns: cycling through header clicks (ascending -> descending -> cleared) and sortBy / clearSort. */
    private static WComponent buildTableSortExample() {
        WTable table = buildProductTable();

        WButton sortByPriceButton = new WButton("Sort by price descending");
        sortByPriceButton.addActionListener(() -> {
            table.sortBy(1, SortDirection.DESCENDING);
        });

        WButton clearButton = new WButton("Clear sort");
        clearButton.addActionListener(() -> {
            table.clearSort();
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(sortByPriceButton);
        buttons.add(clearButton);

        WLabel note = new WLabel("Clicking a column header also cycles through ascending -> descending -> cleared.");
        note.setForeground(GalleryTheme.TEXT_SECONDARY());
        note.setTextWrapping(TextWrapping.WRAP);

        WPanel body = new WPanel(8.0);
        body.add(note);
        body.add(buttons);
        body.add(table);
        return GalleryScaffold.buildExample("Sorting columns (header click / SortBy / ClearSort)", body);
    }

    /** Adding and removing rows: addRow / removeRow / removeAllRows / setValueAt / rowCount. */
    private static WComponent buildTableRowOperationsExample() {
        WTable table = buildProductTable();

        WLabel count = new WLabel("Row count: " + table.getRowCount());
        Runnable updateCount = () -> count.setText("Row count: " + table.getRowCount());

        int[] nextItemNumber = {1};
        WButton addButton = new WButton("Add row");
        addButton.addActionListener(() -> {
            table.addRow("New item " + nextItemNumber[0], String.valueOf(nextItemNumber[0] * 100), "1");
            nextItemNumber[0]++;
            updateCount.run();
        });

        WButton removeButton = new WButton("Remove selected row");
        removeButton.addActionListener(() -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                table.removeRow(row);
                updateCount.run();
            }
        });

        WButton incrementButton = new WButton("Selected quantity +1");
        incrementButton.addActionListener(() -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int quantity;
                try {
                    quantity = Integer.parseInt(table.getValueAt(row, 2));
                } catch (NumberFormatException e) {
                    quantity = 0;
                }
                table.setValueAt(row, 2, String.valueOf(quantity + 1));
            }
        });

        WButton clearButton = new WButton("Remove all");
        clearButton.addActionListener(() -> {
            table.removeAllRows();
            updateCount.run();
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(incrementButton);
        buttons.add(clearButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(table);
        body.add(count);
        return GalleryScaffold.buildExample("Adding and removing rows (AddRow / RemoveRow / SetValueAt)", body);
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
