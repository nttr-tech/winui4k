package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.ItemsViewSelectionMode
import com.appkitbox.winui4k.ListViewSelectionMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SelectionMode
import com.appkitbox.winui4k.SpinButtonPlacementMode
import com.appkitbox.winui4k.TableDensity
import com.appkitbox.winui4k.TableSelectionMode
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.TreeViewSelectionMode
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WItemContainer
import com.appkitbox.winui4k.WItemsView
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WList
import com.appkitbox.winui4k.WListBox
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WProgressBar
import com.appkitbox.winui4k.WSpinner
import com.appkitbox.winui4k.WTable
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTree
import com.appkitbox.winui4k.WTreeNode
import com.appkitbox.winui4k.WUniformGridLayout
import com.appkitbox.winui4k.table.DefaultCellEditor
import com.appkitbox.winui4k.table.DefaultTableCellRenderer
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.RowFilter
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableCellRenderer
import com.appkitbox.winui4k.table.TableColumn
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableModelEvent
import com.appkitbox.winui4k.table.TableRowSorter

/*
 * Collections category: demo pages for ItemsView / ListBox / ListView / Table / Tree.
 */

// region ItemsView

/** The ItemsView page: lines up demos for trying out WItemsView's various features. */
internal fun buildItemsViewPage(): WComponent {
    val page = buildPage(
        "ItemsView",
        "A collection-display control with a swappable layout. Try out WItemsView's various features.",
    )

    page.add(buildUniformGridItemsViewExample())
    return page
}

/** A card grid: UniformGridLayout + ItemContainer + ItemInvoked. */
private fun buildUniformGridItemsViewExample(): WComponent {
    val result = WLabel("Clicked: none")

    val fruits = listOf("Apple", "Orange", "Grape", "Peach", "Cherry", "Banana")
    val containers = fruits.map { name ->
        val label = WLabel(name)
        label.setMargin(12.0, 12.0, 12.0, 12.0)
        val card = WBorder(label)
        card.background = CARD_BACKGROUND
        card.borderColor = CARD_BORDER
        card.borderThickness = 1.0
        card.cornerRadius = 8.0
        WItemContainer(card)
    }

    val layout = WUniformGridLayout()
    layout.minItemWidth = 160.0
    layout.minColumnSpacing = 12.0
    layout.minRowSpacing = 12.0

    val itemsView = WItemsView()
    itemsView.layout = layout
    itemsView.selectionMode = ItemsViewSelectionMode.NONE
    itemsView.isItemInvokedEnabled = true
    itemsView.setItems(containers)
    itemsView.addItemInvokedListener { index ->
        result.text = "Clicked: ${fruits[index]} (index = $index)"
    }
    itemsView.width = 520.0
    itemsView.height = 200.0
    itemsView.horizontalAlignment = HorizontalAlignment.LEFT

    val body = WPanel(spacing = 10.0)
    body.add(itemsView)
    body.add(result)
    return buildExample("A card grid (UniformGridLayout / ItemContainer / ItemInvoked)", body)
}

// endregion

// region ListBox

/** The ListBox page: lines up demos for trying out WListBox's various features. */
internal fun buildListBoxPage(): WComponent {
    val page = buildPage("ListBox", "A control for selecting an item from an always-visible list. Try out WListBox's various features.")

    page.add(buildListBoxColorExample())
    page.add(buildListBoxFontExample())
    page.add(buildListBoxSelectionModeExample())
    return page
}

/** Color selection: mirrors example 1 from the official Gallery (inline items + SelectionChanged changes a rectangle's color). */
private fun buildListBoxColorExample(): WComponent {
    // Same color names as the official Gallery. Repaints the rectangle below based on the selection
    val colors = linkedMapOf(
        "Blue" to WColor(0, 0, 255),
        "Green" to WColor(0, 128, 0),
        "Red" to WColor(255, 0, 0),
        "Yellow" to WColor(255, 255, 0),
    )

    // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
    val output = WBorder()
    output.width = 100.0
    output.height = 30.0
    output.horizontalAlignment = HorizontalAlignment.LEFT

    val listBox = WListBox(colors.keys.toList())
    listBox.width = 200.0
    listBox.horizontalAlignment = HorizontalAlignment.LEFT
    listBox.addListSelectionListener {
        output.background = colors[listBox.selectedItem]
    }

    val body = WPanel(spacing = 10.0)
    body.add(listBox)
    body.add(output)
    return buildExample("A list box with inline items (SelectionChanged)", body)
}

/** Font selection: mirrors example 2 from the official Gallery (fixed height + initial selection + selection changes the font). */
private fun buildListBoxFontExample(): WComponent {
    val fonts = listOf("Arial", "Comic Sans MS", "Courier New", "Segoe UI", "Times New Roman")

    val output = WLabel("You can set the font used for this text.")
    output.foreground = TEXT_SECONDARY

    val listBox = WListBox(fonts)
    listBox.width = 200.0
    listBox.height = 164.0
    listBox.horizontalAlignment = HorizontalAlignment.LEFT
    listBox.addListSelectionListener {
        listBox.selectedItem?.let { output.fontFamily = it }
    }
    listBox.selectedIndex = 2 // Selects Courier New initially, same as the official Gallery

    val body = WPanel(spacing = 10.0)
    body.add(listBox)
    body.add(output)
    return buildExample("A list box with a fixed height (SelectedIndex / FontFamily)", body)
}

/** Selection mode: switching selectionMode plus selectAll / selectedItems / scrollIntoView. */
private fun buildListBoxSelectionModeExample(): WComponent {
    val listBox = WListBox((1..20).map { "Option $it" })
    listBox.width = 240.0
    listBox.height = 200.0
    listBox.horizontalAlignment = HorizontalAlignment.LEFT

    val selection = WLabel("Selection: none")
    listBox.addListSelectionListener {
        val items = listBox.selectedItems
        selection.text = if (items.isEmpty()) "Selection: none" else "Selection: ${items.joinToString(", ")}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (selectionMode in SelectionMode.entries) {
        buttons.add(
            WButton(selectionMode.name).also { button ->
                button.addActionListener { listBox.selectionMode = selectionMode }
            },
        )
    }
    buttons.add(
        WButton("Select all").also { button ->
            button.addActionListener { listBox.selectAll() }
        },
    )
    buttons.add(
        WButton("Scroll to end").also { button ->
            button.addActionListener { listBox.scrollIntoView(listBox.itemCount - 1) }
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(listBox)
    body.add(selection)
    return buildExample("Selection mode (SelectionMode / SelectAll / SelectedItems / ScrollIntoView)", body)
}

// endregion

// region ListView

/** The ListView page: lines up demos for trying out WList's various features. */
internal fun buildListViewPage(): WComponent {
    val page = buildPage("ListView", "A list that lines items up vertically for selection. Try out WList's various features.")

    page.add(buildSimpleListExample())
    page.add(buildListItemOperationsExample())
    page.add(buildListSelectionModeExample())
    page.add(buildListItemClickExample())
    return page
}

/** A basic list: responding to selection changes (SelectionChanged). */
private fun buildSimpleListExample(): WComponent {
    val result = WLabel("Selected: none")

    val list = WList(listOf("Apple", "Orange", "Grape", "Peach", "Cherry"))
    list.width = 240.0
    list.addListSelectionListener {
        val item = list.selectedItem
        result.text = if (item == null) "Selected: none" else "Selected: $item (index = ${list.selectedIndex})"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(list)
    row.add(result)
    return buildExample("A simple list", row)
}

/** Adding and removing items: addItem / removeItem / removeAllItems / itemCount. */
private fun buildListItemOperationsExample(): WComponent {
    // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
    val list = WList(listOf("Item 1", "Item 2", "Item 3"))
    list.width = 240.0
    list.horizontalAlignment = HorizontalAlignment.LEFT

    val count = WLabel("Item count: ${list.itemCount}")
    val input = WTextField("Item name to add").also { it.width = 200.0 }

    val addButton = WButton("Add")
    addButton.addActionListener {
        if (input.text.isNotEmpty()) {
            list.addItem(input.text)
            input.text = ""
            count.text = "Item count: ${list.itemCount}"
        }
    }

    val removeButton = WButton("Remove selected")
    removeButton.addActionListener {
        val index = list.selectedIndex
        if (index >= 0) {
            list.removeItem(index)
            count.text = "Item count: ${list.itemCount}"
        }
    }

    val clearButton = WButton("Remove all")
    clearButton.addActionListener {
        list.removeAllItems()
        count.text = "Item count: ${list.itemCount}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(input)
    buttons.add(addButton)
    buttons.add(removeButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(list)
    body.add(count)
    return buildExample("Adding and removing items", body)
}

/** Selection mode: switching selectionMode and selectAll. */
private fun buildListSelectionModeExample(): WComponent {
    val list = WList((1..5).map { "Option $it" })
    list.width = 240.0
    list.horizontalAlignment = HorizontalAlignment.LEFT

    val mode = WLabel("Selection mode: ${list.selectionMode}")

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (selectionMode in ListViewSelectionMode.entries) {
        buttons.add(
            WButton(selectionMode.name).also { button ->
                button.addActionListener {
                    list.selectionMode = selectionMode
                    mode.text = "Selection mode: ${list.selectionMode}"
                }
            },
        )
    }
    buttons.add(
        WButton("Select all").also { button ->
            button.addActionListener { list.selectAll() }
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(list)
    body.add(mode)
    return buildExample("Selection mode (SelectionMode / SelectAll)", body)
}

/** ItemClick: enabling isItemClickEnabled and receiving the clicked item. */
private fun buildListItemClickExample(): WComponent {
    val result = WLabel("Clicked: none")

    val list = WList(listOf("Document", "Pictures", "Music", "Video"))
    list.width = 240.0
    list.isItemClickEnabled = true
    list.addItemClickListener { item ->
        result.text = "Clicked: $item"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(list)
    row.add(result)
    return buildExample("Item clicks (ItemClick)", row)
}

// endregion

// region Table page

/** The Table page: lines up demos for trying out WTable's (WinUI 3's TableView, Windows App SDK 2.5 experimental) various features. */
internal fun buildTablePage(): WComponent {
    val page = buildPage(
        "Table",
        "A table that displays data in rows and columns. " +
            "Like Swing's JTable, try out the various features of WTable, built from TableModel / TableColumnModel / TableRowSorter.",
    )

    page.add(buildTableBasicExample())
    page.add(buildTableSortFilterExample())
    page.add(buildTableEditingExample())
    page.add(buildTableGroupingExample())
    page.add(buildTableCellsExample())
    page.add(buildTableAppearanceExample())
    page.add(buildTableColumnsExample())
    return page
}

/** Product data (product / category / price / stock / in stock). Returns column types to distinguish numeric and boolean columns. */
private class ProductTableModel : DefaultTableModel(
    listOf(
        listOf("Apple", "Fruit", 150, 12, true),
        listOf("Orange", "Fruit", 80, 30, true),
        listOf("Grape", "Fruit", 480, 0, false),
        listOf("Cabbage", "Vegetable", 200, 8, true),
        listOf("Carrot", "Vegetable", 60, 25, true),
        listOf("Tomato", "Vegetable", 120, 0, false),
        listOf("Milk", "Dairy", 230, 15, true),
        listOf("Cheese", "Dairy", 380, 4, true),
    ),
    listOf("Product", "Category", "Price", "Stock", "In stock"),
) {
    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        2, 3 -> Integer::class.java
        4 -> java.lang.Boolean::class.java
        else -> String::class.java
    }
}

/** The table for the demos. Its height is fixed so it does not get lost in the page's scrolling. */
private fun buildProductTable(model: TableModel = ProductTableModel()): WTable {
    val table = WTable(model)
    table.width = 620.0
    table.height = 300.0
    table.horizontalAlignment = HorizontalAlignment.LEFT
    return table
}

/** Creates a check box together with its change handler (for the Options panel). */
private fun optionCheckBox(text: String, checked: Boolean, onChanged: (Boolean) -> Unit): WCheckBox {
    val checkBox = WCheckBox(text)
    checkBox.isChecked = checked
    checkBox.addItemListener { onChanged(it == true) }
    return checkBox
}

/** Basics: displaying a TableModel and selecting rows (SelectionMode / Select / SelectionChanged / view-model conversion). */
private fun buildTableBasicExample(): WComponent {
    val table = buildProductTable()
    val result = WLabel("Selected: none")
    table.addRowSelectionListener {
        val row = table.selectedRow
        result.text = if (row < 0) {
            "Selection: none"
        } else {
            val modelRow = table.convertRowIndexToModel(row)
            "Selection: ${table.model.getValueAt(modelRow, 0)} (view row = $row / model row = $modelRow)"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(result)

    val selectionMode = WComboBox(listOf("SINGLE (select one row)", "NONE (no selection)"))
    selectionMode.selectedIndex = 0
    selectionMode.addListSelectionListener {
        table.selectionMode = if (selectionMode.selectedIndex == 1) TableSelectionMode.NONE else TableSelectionMode.SINGLE
    }
    val selectThird = WButton("Select the third row")
    selectThird.addActionListener { table.selectRow(2) }
    val clear = WButton("Clear selection")
    clear.addActionListener { table.clearSelection() }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Selection mode (SelectionMode)"))
    options.add(selectionMode)
    options.add(selectThird)
    options.add(clear)
    return buildExample("Basics (TableModel / row selection / SelectionChanged)", body, options)
}

/** Sorting and filtering: TableRowSorter (header click / SortKeys / Comparator / RowFilter) and Sorting / Sorted. */
private fun buildTableSortFilterExample(): WComponent {
    val model = ProductTableModel()
    val table = buildProductTable(model)
    val sorter = TableRowSorter(model)
    // Sort product names shortest first (alphabetically when the lengths are equal)
    sorter.setComparator(0, compareBy<String> { it.length }.thenBy { it })
    table.rowSorter = sorter

    val status = WLabel("Click a column header to sort.")
    status.textWrapping = TextWrapping.WRAP
    var cancelSorting = false
    table.addSortingListener { event ->
        event.isCanceled = cancelSorting
        status.text = if (cancelSorting) {
            "Canceled sorting by \"${table.getColumnName(event.column)}\" (Cancel in Sorting)"
        } else {
            "Sorting: ${table.getColumnName(event.column)} → ${event.sortOrder}"
        }
    }
    table.addSortedListener { event ->
        status.text = "Sorted: ${table.getColumnName(event.column)} (${event.sortOrder}) / showing ${table.rowCount} rows"
    }

    val filterText = WTextField("Filter by product (regular expression)")
    filterText.width = 240.0
    filterText.addTextChangedListener { text ->
        sorter.setRowFilter(if (text.isBlank()) null else RowFilter.regexFilter(text, 0))
        status.text = "Filtered: showing ${table.rowCount} of ${model.getRowCount()} rows"
    }

    val body = WPanel(spacing = 8.0)
    body.add(filterText)
    body.add(table)
    body.add(status)

    val byPrice = WButton("Highest price first")
    byPrice.addActionListener { sorter.setSortKeys(listOf(RowSorter.SortKey(2, SortOrder.DESCENDING))) }
    val clearSort = WButton("Clear sort")
    clearSort.addActionListener { sorter.setSortKeys(null) }
    val inStock = optionCheckBox("Only rows in stock", false) { only ->
        sorter.setRowFilter(if (only) RowFilter.regexFilter("true", 4) else null)
    }
    val cancel = optionCheckBox("Cancel sorting (Sorting)", false) { cancelSorting = it }
    val sortable = optionCheckBox("Category column is sortable", true) { sorter.setSortable(1, it) }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("TableRowSorter"))
    options.add(byPrice)
    options.add(clearSort)
    options.add(inStock)
    options.add(sortable)
    options.add(cancel)
    return buildExample("Sorting and filtering (TableRowSorter / RowFilter / Sorting / Sorted)", body, options)
}

/** Editing: TableModel.isCellEditable / DefaultCellEditor / BeginningEdit / CellEditEnding / writing back to the model. */
private fun buildTableEditingExample(): WComponent {
    val model = object : DefaultTableModel(
        listOf(
            listOf(1, "Hanako Sato", "Development", 34, true),
            listOf(2, "Ichiro Suzuki", "Sales", 41, true),
            listOf(3, "Misaki Takahashi", "Design", 28, false),
            listOf(4, "Ken Tanaka", "Development", 45, true),
        ),
        listOf("ID", "Name", "Department", "Age", "Employed"),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            0, 3 -> Integer::class.java
            4 -> java.lang.Boolean::class.java
            else -> String::class.java
        }

        // The ID cannot be edited
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex != 0
    }
    val table = buildProductTable(model)
    table.height = 220.0
    // Edit the department with a combo box and the age with a number box (the same idea as Swing's DefaultCellEditor)
    table.columnModel.getColumn(2).cellEditor = DefaultCellEditor(WComboBox(listOf("Development", "Sales", "Design", "Admin")))
    table.columnModel.getColumn(3).cellEditor = DefaultCellEditor(
        WSpinner().also {
            it.minimum = 18.0
            it.maximum = 70.0
            it.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE
        },
    )

    val log = WLabel("Double-click or press F2 to start editing, Enter to commit, and Esc to cancel.")
    log.textWrapping = TextWrapping.WRAP
    table.addBeginningEditListener { event ->
        log.text = "Edit started: ${table.getColumnName(event.column)} (row ${event.modelRow + 1})"
    }
    table.addCellEditEndingListener { event ->
        log.text = "Edit ended: ${table.getColumnName(event.column)} → ${event.action}"
    }
    // The committed value is written back via TableModel.setValueAt, converted to the column type (getColumnClass)
    model.addTableModelListener { event ->
        if (event.type == TableModelEvent.UPDATE && event.firstRow == event.lastRow && event.column >= 0) {
            val value = model.getValueAt(event.firstRow, event.column)
            log.text = "Model updated: ${model.getColumnName(event.column)} (row ${event.firstRow + 1}) = $value " +
                "(${value?.javaClass?.simpleName})"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(log)

    val readOnly = optionCheckBox("Make the whole table read-only (IsReadOnly)", false) { table.isReadOnly = it }
    val ageEditable = optionCheckBox("Age column is editable (column IsReadOnly)", true) {
        table.columnModel.getColumn(3).isEditable = it
    }
    val commit = WButton("Commit edit (CommitEdit)")
    commit.addActionListener { log.text = "CommitEdit → ${table.stopCellEditing()}" }
    val cancel = WButton("Cancel edit (CancelEdit)")
    cancel.addActionListener { log.text = "CancelEdit → ${table.cancelCellEditing()}" }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Editing"))
    options.add(readOnly)
    options.add(ageEditable)
    options.add(commit)
    options.add(cancel)
    return buildExample("Editing (isCellEditable / DefaultCellEditor / BeginningEdit / CellEditEnding)", body, options)
}

/** Grouping: groupBy / clearGrouping / ExpandAllGroups / CollapseAllGroups / GroupHeaderTemplate. */
private fun buildTableGroupingExample(): WComponent {
    val table = buildProductTable()
    table.groupBy(1) // Group by category

    val groupBy = WComboBox(listOf("Category", "In stock", "Price range (300 yen or more / less)", "No grouping"))
    groupBy.selectedIndex = 0
    groupBy.addListSelectionListener {
        when (groupBy.selectedIndex) {
            0 -> table.groupBy(1)
            1 -> table.groupBy(4)
            2 -> table.groupBy { model, row -> if ((model.getValueAt(row, 2) as Int) >= 300) "300 yen or more" else "Under 300 yen" }
            else -> table.clearGrouping()
        }
    }
    val expand = WButton("Expand all")
    expand.addActionListener { table.expandAllGroups() }
    val collapse = WButton("Collapse all")
    collapse.addActionListener { table.collapseAllGroups() }
    val customHeader = optionCheckBox("Show headers with a template", false) { custom ->
        table.groupHeaderTemplate = if (custom) {
            "<StackPanel Orientation=\"Horizontal\" Spacing=\"8\" Padding=\"8,4\">" +
                "<TextBlock Text=\"{Binding KeyText}\" FontWeight=\"SemiBold\" Foreground=\"{ThemeResource AccentTextFillColorPrimaryBrush}\" />" +
                "<TextBlock Text=\"{Binding ItemCount}\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />" +
                "<TextBlock Text=\"items\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />" +
                "</StackPanel>"
        } else {
            null
        }
    }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Key to group by (GroupBy)"))
    options.add(groupBy)
    options.add(expand)
    options.add(collapse)
    options.add(customHeader)
    return buildExample("Grouping (GroupBy / ExpandAllGroups / CollapseAllGroups / GroupHeaderTemplate)", table, options)
}

/** Cell display: TableCellRenderer / XAML cell templates / frozen columns / header and cell tooltips. */
private fun buildTableCellsExample(): WComponent {
    val model = object : DefaultTableModel(
        listOf(
            listOf("Review the design document", "In progress", 60, "Check the spec differences and sort out the review comments."),
            listOf("Automate the tests", "Done", 100, "Add the E2E tests to CI and run them every night."),
            listOf("Translate the documentation", "Not started", 0, "Translate the README and the API reference into English."),
            listOf("Measure performance", "In progress", 35, "Measure display speed with a large data set (100,000 rows)."),
        ),
        listOf("Task", "Status", "Progress (%)", "Notes"),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = if (columnIndex == 2) Integer::class.java else String::class.java
    }
    val table = buildProductTable(model)
    table.height = 220.0

    val task = table.columnModel.getColumn(0)
    task.isFrozen = true // The task name stays visible even when scrolling horizontally
    task.preferredWidth = 180.0
    task.headerToolTip = "This column is frozen at the left edge (FrozenEdge = Leading)"

    // Show the status as a colored label with a XAML template ({Binding} is the cell value)
    val state = table.columnModel.getColumn(1)
    state.cellTemplate = "<Border Background=\"{ThemeResource AccentFillColorDefaultBrush}\" CornerRadius=\"10\" " +
        "Padding=\"10,2\" Margin=\"8,0\" HorizontalAlignment=\"Left\">" +
        "<TextBlock Text=\"{Binding}\" Foreground=\"{ThemeResource TextOnAccentFillColorPrimaryBrush}\" /></Border>"
    state.headerToolTip = "Displayed with a XAML DataTemplate (TableViewTemplateColumn)"

    // Show the progress with a Kotlin component (a progress bar) via a renderer
    val progress = table.columnModel.getColumn(2)
    progress.preferredWidth = 160.0
    progress.cellRenderer = TableCellRenderer { _, value, _, _, _, _ ->
        val bar = WProgressBar(value = (value as Int).toDouble())
        bar.setMargin(8.0, 0.0, 8.0, 0.0)
        bar.verticalAlignment = VerticalAlignment.CENTER
        bar
    }
    progress.headerToolTip = "Displays a WProgressBar via TableCellRenderer (GenerateElementCore)"

    // The notes are long, so show the full text in a cell tooltip
    val memo = table.columnModel.getColumn(3)
    memo.preferredWidth = 240.0
    memo.cellToolTipColumn = 3

    val note = optionsLabel("Hover over a column header or a notes cell to show a tooltip. Columns beyond the table's width can be scrolled horizontally.")
    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(note)
    table.width = 520.0
    return buildExample("Cell display (TableCellRenderer / cell templates / frozen columns / tooltips)", body)
}

/** Appearance: grid lines / header / density / row background / stripes / display when there are no rows. */
private fun buildTableAppearanceExample(): WComponent {
    val model = ProductTableModel()
    val table = buildProductTable(model)
    table.emptyText = "No products to show"
    val removedRows = mutableListOf<List<Any?>>()

    val horizontal = optionCheckBox("Horizontal grid lines", table.showHorizontalLines) { table.showHorizontalLines = it }
    val vertical = optionCheckBox("Vertical grid lines", table.showVerticalLines) { table.showVerticalLines = it }
    val header = optionCheckBox("Show column headers", table.isHeaderVisible) { table.isHeaderVisible = it }
    val striped = optionCheckBox("Stripes (AlternatingRowBackground)", false) {
        table.alternatingRowBackground = if (it) WColor(0x20, 0x80, 0x80, 0x80) else null
    }
    val resizable = optionCheckBox("Resize columns by dragging (CanUserResizeColumns)", table.canUserResizeColumns) {
        table.canUserResizeColumns = it
    }
    val density = WComboBox(listOf("COMPACT", "STANDARD", "COMFORTABLE"))
    density.selectedIndex = table.density.ordinal
    density.addListSelectionListener { table.density = TableDensity.entries[density.selectedIndex] }
    val clearRows = WButton("Remove all rows (EmptyTemplate)")
    clearRows.addActionListener {
        while (model.getRowCount() > 0) {
            removedRows += (0 until model.getColumnCount()).map { model.getValueAt(0, it) }
            model.removeRow(0)
        }
    }
    val restoreRows = WButton("Restore rows")
    restoreRows.addActionListener {
        for (row in removedRows) model.addRow(row)
        removedRows.clear()
    }

    val options = WPanel(spacing = 8.0)
    options.add(horizontal)
    options.add(vertical)
    options.add(header)
    options.add(striped)
    options.add(resizable)
    options.add(optionsLabel("Density (Density)"))
    options.add(density)
    options.add(clearRows)
    options.add(restoreRows)
    return buildExample("Appearance (GridLinesVisibility / HeadersVisibility / Density / stripes / empty display)", table, options)
}

/** Column operations: TableColumnModel (move / show and hide / add and remove) and column widths. */
private fun buildTableColumnsExample(): WComponent {
    val model = ProductTableModel()
    val table = buildProductTable(model)
    val status = WLabel("")
    val updateStatus = {
        val names = (0 until table.columnCount).map { table.getColumnName(it) }
        status.text = "Column order: ${names.joinToString(" / ")}"
    }
    updateStatus()

    val moveLast = WButton("Move the first column to the end")
    moveLast.addActionListener {
        table.moveColumn(0, table.columnCount - 1)
        updateStatus()
    }
    val hideCategory = optionCheckBox("Show the category column", true) { visible ->
        table.columnModel.getColumns().first { it.modelIndex == 1 }.isVisible = visible
    }
    var taxColumn: TableColumn? = null
    val addTax = WButton("Add / remove the price-with-tax column")
    addTax.addActionListener {
        val existing = taxColumn
        if (existing == null) {
            val column = TableColumn(2)
            column.headerValue = "Price with tax"
            column.cellRenderer = DefaultTableCellRenderer().also { it.horizontalAlignment = HorizontalAlignment.RIGHT }
                .let { base ->
                    TableCellRenderer { view, value, selected, focus, row, col ->
                        base.getTableCellRendererComponent(view, "${((value as Int) * 1.08).toInt()} yen", selected, focus, row, col)
                    }
                }
            table.addColumn(column)
            taxColumn = column
        } else {
            table.removeColumn(existing)
            taxColumn = null
        }
        updateStatus()
    }
    val widen = WButton("Set the product column width to 200")
    widen.addActionListener {
        table.columnModel.getColumns().first { it.modelIndex == 0 }.preferredWidth = 200.0
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(status)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("TableColumnModel"))
    options.add(moveLast)
    options.add(hideCategory)
    options.add(addTax)
    options.add(widen)
    return buildExample("Column operations (TableColumnModel / TableColumn)", body, options)
}

// endregion

// region Tree page

/** The Tree page: lines up demos for trying out WTree's various features. */
internal fun buildTreePage(): WComponent {
    val page = buildPage("Tree", "A tree whose hierarchical data can be expanded and collapsed. Try out WTree's various features.")

    page.add(buildSimpleTreeExample())
    page.add(buildTreeMultiSelectExample())
    page.add(buildTreeExpandCollapseExample())
    return page
}

/** Builds the same sample tree (Work Documents / Personal Documents) as the real Gallery. */
private fun buildSampleTree(): WTree {
    val tree = WTree()
    tree.width = 345.0
    // Pin it to the left so the tree doesn't shift toward the center if the panel widens (e.g. from a long label)
    tree.horizontalAlignment = HorizontalAlignment.LEFT

    val workFolder = WTreeNode("Work Documents")
    workFolder.add(WTreeNode("XYZ Functional Spec"))
    workFolder.add(WTreeNode("Feature Schedule"))
    workFolder.isExpanded = true

    val remodelFolder = WTreeNode("Home Remodel")
    remodelFolder.add(WTreeNode("Contractor Contact Info"))
    remodelFolder.add(WTreeNode("Paint Color Scheme"))
    remodelFolder.isExpanded = true

    val personalFolder = WTreeNode("Personal Documents")
    personalFolder.add(remodelFolder)
    personalFolder.isExpanded = true

    tree.addRootNode(workFolder)
    tree.addRootNode(personalFolder)
    return tree
}

/** Basic tree: drag-to-reorder and responding to node clicks (ItemInvoked). */
private fun buildSimpleTreeExample(): WComponent {
    val result = WLabel("Click: none")
    result.textWrapping = TextWrapping.WRAP

    val tree = buildSampleTree()
    tree.canDragItems = true
    tree.canReorderItems = true
    tree.addItemInvokedListener { node ->
        result.text = if (node == null) "Click: none" else "Click: ${node.text} (depth = ${node.depth})"
    }

    val body = WPanel(spacing = 8.0)
    body.add(tree)
    body.add(result)
    return buildExample("Simple tree (drag & drop reordering / ItemInvoked)", body)
}

/** Multiple selection: checkboxes from SelectionMode = MULTIPLE, plus SelectAll / SelectedNodes. */
private fun buildTreeMultiSelectExample(): WComponent {
    val tree = buildSampleTree()
    tree.selectionMode = TreeViewSelectionMode.MULTIPLE

    val result = WLabel("Selected: none")
    result.textWrapping = TextWrapping.WRAP
    val showButton = WButton("Show selection")
    showButton.addActionListener {
        val names = tree.selectedNodes.joinToString(", ") { it.text }
        result.text = if (names.isEmpty()) "Selected: none" else "Selected: $names"
    }
    val selectAllButton = WButton("Select all")
    selectAllButton.addActionListener { tree.selectAll() }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(showButton)
    buttons.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(tree)
    body.add(buttons)
    body.add(result)
    return buildExample("Multiple selection (SelectionMode / SelectAll / SelectedNodes)", body)
}

/** Expand and collapse: the Expand / Collapse methods and the Expanding / Collapsed events. */
private fun buildTreeExpandCollapseExample(): WComponent {
    val log = WLabel("Event: none")
    log.textWrapping = TextWrapping.WRAP

    val tree = buildSampleTree()
    tree.addExpandingListener { node -> log.text = "Event: Expanding (${node?.text})" }
    tree.addCollapsedListener { node -> log.text = "Event: Collapsed (${node?.text})" }

    val expandButton = WButton("Expand all")
    expandButton.addActionListener {
        for (root in tree.rootNodes) tree.expand(root)
    }
    val collapseButton = WButton("Collapse all")
    collapseButton.addActionListener {
        for (root in tree.rootNodes) tree.collapse(root)
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(expandButton)
    buttons.add(collapseButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(tree)
    body.add(log)
    return buildExample("Expand and collapse (Expand / Collapse / Expanding / Collapsed)", body)
}

// endregion
