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
        Messages.get("itemsview.description"),
    )

    page.add(buildUniformGridItemsViewExample())
    return page
}

/** A card grid: UniformGridLayout + ItemContainer + ItemInvoked. */
private fun buildUniformGridItemsViewExample(): WComponent {
    val result = WLabel(Messages.get("itemsview.click.none"))

    val fruits = listOf(
        Messages.get("itemsview.fruit.apple"),
        Messages.get("itemsview.fruit.mandarin"),
        Messages.get("itemsview.fruit.grapes"),
        Messages.get("itemsview.fruit.peach"),
        Messages.get("itemsview.fruit.cherry"),
        Messages.get("itemsview.fruit.banana"),
    )
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
        result.text = Messages.get("itemsview.click.status", fruits[index], index.toString())
    }
    itemsView.width = 520.0
    itemsView.height = 200.0
    itemsView.horizontalAlignmentInParent = HorizontalAlignment.LEFT

    val body = WPanel(spacing = 10.0)
    body.add(itemsView)
    body.add(result)
    return buildExample(Messages.get("itemsview.example.cardgrid.title"), body)
}

// endregion

// region ListBox

/** The ListBox page: lines up demos for trying out WListBox's various features. */
internal fun buildListBoxPage(): WComponent {
    val page = buildPage("ListBox", Messages.get("listbox.description"))

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
    output.horizontalAlignmentInParent = HorizontalAlignment.LEFT

    val listBox = WListBox(colors.keys.toList())
    listBox.width = 200.0
    listBox.horizontalAlignmentInParent = HorizontalAlignment.LEFT
    listBox.addListSelectionListener {
        output.background = colors[listBox.selectedItem]
    }

    val body = WPanel(spacing = 10.0)
    body.add(listBox)
    body.add(output)
    return buildExample(Messages.get("listbox.example.inline.title"), body)
}

/** Font selection: mirrors example 2 from the official Gallery (fixed height + initial selection + selection changes the font). */
private fun buildListBoxFontExample(): WComponent {
    val fonts = listOf("Arial", "Comic Sans MS", "Courier New", "Segoe UI", "Times New Roman")

    val output = WLabel("You can set the font used for this text.")
    output.foreground = TEXT_SECONDARY

    val listBox = WListBox(fonts)
    listBox.width = 200.0
    listBox.height = 164.0
    listBox.horizontalAlignmentInParent = HorizontalAlignment.LEFT
    listBox.addListSelectionListener {
        listBox.selectedItem?.let { output.fontFamily = it }
    }
    listBox.selectedIndex = 2 // Selects Courier New initially, same as the official Gallery

    val body = WPanel(spacing = 10.0)
    body.add(listBox)
    body.add(output)
    return buildExample(Messages.get("listbox.example.font.title"), body)
}

/** Selection mode: switching selectionMode plus selectAll / selectedItems / scrollIntoView. */
private fun buildListBoxSelectionModeExample(): WComponent {
    val listBox = WListBox((1..20).map { Messages.get("listbox.option", it.toString()) })
    listBox.width = 240.0
    listBox.height = 200.0
    listBox.horizontalAlignmentInParent = HorizontalAlignment.LEFT

    val selection = WLabel(Messages.get("listbox.selection.none"))
    listBox.addListSelectionListener {
        val items = listBox.selectedItems
        selection.text = if (items.isEmpty()) {
            Messages.get("listbox.selection.none")
        } else {
            Messages.get("listbox.selection.status", items.joinToString(", "))
        }
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
        WButton(Messages.get("listbox.selectall")).also { button ->
            button.addActionListener { listBox.selectAll() }
        },
    )
    buttons.add(
        WButton(Messages.get("listbox.scrolltoend")).also { button ->
            button.addActionListener { listBox.scrollIntoView(listBox.itemCount - 1) }
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(listBox)
    body.add(selection)
    return buildExample(Messages.get("listbox.example.selectionmode.title"), body)
}

// endregion

// region ListView

/** The ListView page: lines up demos for trying out WList's various features. */
internal fun buildListViewPage(): WComponent {
    val page = buildPage("ListView", Messages.get("listview.description"))

    page.add(buildSimpleListExample())
    page.add(buildListItemOperationsExample())
    page.add(buildListSelectionModeExample())
    page.add(buildListItemClickExample())
    return page
}

/** A basic list: responding to selection changes (SelectionChanged). */
private fun buildSimpleListExample(): WComponent {
    val result = WLabel(Messages.get("listview.selection.none"))

    val list = WList(listOf(Messages.get("listview.fruit.apple"), Messages.get("listview.fruit.mandarin"), Messages.get("listview.fruit.grapes"), Messages.get("listview.fruit.peach"), Messages.get("listview.fruit.cherry")))
    list.width = 240.0
    list.addListSelectionListener {
        val item = list.selectedItem
        result.text = if (item == null) {
            Messages.get("listview.selection.none")
        } else {
            Messages.get("listview.selection.status", item, list.selectedIndex.toString())
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(list)
    row.add(result)
    return buildExample(Messages.get("listview.example.simple.title"), row)
}

/** Adding and removing items: addItem / removeItem / removeAllItems / itemCount. */
private fun buildListItemOperationsExample(): WComponent {
    // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
    val list = WList((1..3).map { Messages.get("listview.item", it.toString()) })
    list.width = 240.0
    list.horizontalAlignmentInParent = HorizontalAlignment.LEFT

    val count = WLabel(Messages.get("listview.itemcount", list.itemCount.toString()))
    val input = WTextField().apply { placeholderText = Messages.get("listview.newitem.placeholder") }.also { it.width = 200.0 }

    val addButton = WButton(Messages.get("listview.add"))
    addButton.addActionListener {
        if (input.text.isNotEmpty()) {
            list.addItem(input.text)
            input.text = ""
            count.text = Messages.get("listview.itemcount", list.itemCount.toString())
        }
    }

    val removeButton = WButton(Messages.get("listview.removeselected"))
    removeButton.addActionListener {
        val index = list.selectedIndex
        if (index >= 0) {
            list.removeItem(index)
            count.text = Messages.get("listview.itemcount", list.itemCount.toString())
        }
    }

    val clearButton = WButton(Messages.get("listview.removeall"))
    clearButton.addActionListener {
        list.removeAllItems()
        count.text = Messages.get("listview.itemcount", list.itemCount.toString())
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
    return buildExample(Messages.get("listview.example.operations.title"), body)
}

/** Selection mode: switching selectionMode and selectAll. */
private fun buildListSelectionModeExample(): WComponent {
    val list = WList((1..5).map { Messages.get("listview.option", it.toString()) })
    list.width = 240.0
    list.horizontalAlignmentInParent = HorizontalAlignment.LEFT

    val mode = WLabel(Messages.get("listview.selectionmode.status", list.selectionMode.toString()))

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (selectionMode in ListViewSelectionMode.entries) {
        buttons.add(
            WButton(selectionMode.name).also { button ->
                button.addActionListener {
                    list.selectionMode = selectionMode
                    mode.text = Messages.get("listview.selectionmode.status", list.selectionMode.toString())
                }
            },
        )
    }
    buttons.add(
        WButton(Messages.get("listview.selectall")).also { button ->
            button.addActionListener { list.selectAll() }
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(list)
    body.add(mode)
    return buildExample(Messages.get("listview.example.selectionmode.title"), body)
}

/** ItemClick: enabling isItemClickEnabled and receiving the clicked item. */
private fun buildListItemClickExample(): WComponent {
    val result = WLabel(Messages.get("listview.click.none"))

    val list = WList(listOf(Messages.get("listview.folder.documents"), Messages.get("listview.folder.pictures"), Messages.get("listview.folder.music"), Messages.get("listview.folder.videos")))
    list.width = 240.0
    list.isItemClickEnabled = true
    list.addItemClickListener { item ->
        result.text = Messages.get("listview.click.status", item)
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(list)
    row.add(result)
    return buildExample(Messages.get("listview.example.itemclick.title"), row)
}

// endregion

// region Table page

/** The Table page: lines up demos for trying out WTable's (WinUI 3's TableView, Windows App SDK 2.6 experimental) various features. */
internal fun buildTablePage(): WComponent {
    val page = buildPage(
        "Table",
        Messages.get("table.description"),
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
        listOf(Messages.get("table.product.apple"), Messages.get("table.category.fruit"), 150, 12, true),
        listOf(Messages.get("table.product.mandarin"), Messages.get("table.category.fruit"), 80, 30, true),
        listOf(Messages.get("table.product.grapes"), Messages.get("table.category.fruit"), 480, 0, false),
        listOf(Messages.get("table.product.cabbage"), Messages.get("table.category.vegetable"), 200, 8, true),
        listOf(Messages.get("table.product.carrot"), Messages.get("table.category.vegetable"), 60, 25, true),
        listOf(Messages.get("table.product.tomato"), Messages.get("table.category.vegetable"), 120, 0, false),
        listOf(Messages.get("table.product.milk"), Messages.get("table.category.dairy"), 230, 15, true),
        listOf(Messages.get("table.product.cheese"), Messages.get("table.category.dairy"), 380, 4, true),
    ),
    listOf(
        Messages.get("table.column.name"),
        Messages.get("table.column.category"),
        Messages.get("table.column.price"),
        Messages.get("table.column.stock"),
        Messages.get("table.column.instock"),
    ),
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
    table.horizontalAlignmentInParent = HorizontalAlignment.LEFT
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
    val result = WLabel(Messages.get("table.basic.selection.none"))
    table.addRowSelectionListener {
        val row = table.selectedRow
        result.text = if (row < 0) {
            Messages.get("table.basic.selection.none")
        } else {
            val modelRow = table.convertRowIndexToModel(row)
            Messages.get(
                "table.basic.selection.status",
                table.model.getValueAt(modelRow, 0).toString(),
                row.toString(),
                modelRow.toString(),
            )
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(result)

    val selectionMode = WComboBox(listOf(Messages.get("table.basic.mode.single"), Messages.get("table.basic.mode.none")))
    selectionMode.selectedIndex = 0
    selectionMode.addListSelectionListener {
        table.selectionMode = if (selectionMode.selectedIndex == 1) TableSelectionMode.NONE else TableSelectionMode.SINGLE
    }
    val selectThird = WButton(Messages.get("table.basic.selectthird"))
    selectThird.addActionListener { table.selectRow(2) }
    val clear = WButton(Messages.get("table.basic.clearselection"))
    clear.addActionListener { table.clearSelection() }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("table.basic.selectionmode.label")))
    options.add(selectionMode)
    options.add(selectThird)
    options.add(clear)
    return buildExample(Messages.get("table.example.basic.title"), body, options)
}

/** Sorting and filtering: TableRowSorter (header click / SortKeys / Comparator / RowFilter) and Sorting / Sorted. */
private fun buildTableSortFilterExample(): WComponent {
    val model = ProductTableModel()
    val table = buildProductTable(model)
    val sorter = TableRowSorter(model)
    // Sort product names shortest first (alphabetically when the lengths are equal)
    sorter.setComparator(0, compareBy<String> { it.length }.thenBy { it })
    table.rowSorter = sorter

    val status = WLabel(Messages.get("table.sort.hint"))
    status.textWrapping = TextWrapping.WRAP
    var cancelSorting = false
    table.addSortingListener { event ->
        event.isCanceled = cancelSorting
        status.text = if (cancelSorting) {
            Messages.get("table.sort.canceled", table.getColumnName(event.column))
        } else {
            Messages.get("table.sort.sorting", table.getColumnName(event.column), event.sortOrder.toString())
        }
    }
    table.addSortedListener { event ->
        status.text = Messages.get(
            "table.sort.sorted",
            table.getColumnName(event.column),
            event.sortOrder.toString(),
            table.rowCount.toString(),
        )
    }

    val filterText = WTextField().apply { placeholderText = Messages.get("table.sort.filter.placeholder") }
    filterText.width = 240.0
    filterText.addTextChangedListener { text ->
        sorter.setRowFilter(if (text.isBlank()) null else RowFilter.regexFilter(text, 0))
        status.text = Messages.get("table.sort.filter.status", table.rowCount.toString(), model.getRowCount().toString())
    }

    val body = WPanel(spacing = 8.0)
    body.add(filterText)
    body.add(table)
    body.add(status)

    val byPrice = WButton(Messages.get("table.sort.byprice"))
    byPrice.addActionListener { sorter.setSortKeys(listOf(RowSorter.SortKey(2, SortOrder.DESCENDING))) }
    val clearSort = WButton(Messages.get("table.sort.clear"))
    clearSort.addActionListener { sorter.setSortKeys(null) }
    val inStock = optionCheckBox(Messages.get("table.sort.instockonly"), false) { only ->
        sorter.setRowFilter(if (only) RowFilter.regexFilter("true", 4) else null)
    }
    val cancel = optionCheckBox(Messages.get("table.sort.cancel"), false) { cancelSorting = it }
    val sortable = optionCheckBox(Messages.get("table.sort.categorysortable"), true) { sorter.setSortable(1, it) }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("TableRowSorter"))
    options.add(byPrice)
    options.add(clearSort)
    options.add(inStock)
    options.add(sortable)
    options.add(cancel)
    return buildExample(Messages.get("table.example.sortfilter.title"), body, options)
}

/** Editing: TableModel.isCellEditable / DefaultCellEditor / BeginningEdit / CellEditEnding / writing back to the model. */
@Suppress("LongMethod") // Declarative UI-building sample code
private fun buildTableEditingExample(): WComponent {
    val model = object : DefaultTableModel(
        listOf(
            listOf(1, Messages.get("table.edit.person1"), Messages.get("table.edit.dept.development"), 34, true),
            listOf(2, Messages.get("table.edit.person2"), Messages.get("table.edit.dept.sales"), 41, true),
            listOf(3, Messages.get("table.edit.person3"), Messages.get("table.edit.dept.design"), 28, false),
            listOf(4, Messages.get("table.edit.person4"), Messages.get("table.edit.dept.development"), 45, true),
        ),
        listOf(
            "ID",
            Messages.get("table.edit.column.name"),
            Messages.get("table.edit.column.department"),
            Messages.get("table.edit.column.age"),
            Messages.get("table.edit.column.active"),
        ),
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
    table.columnModel.getColumn(2).cellEditor = DefaultCellEditor(
        WComboBox(
            listOf(
                Messages.get("table.edit.dept.development"),
                Messages.get("table.edit.dept.sales"),
                Messages.get("table.edit.dept.design"),
                Messages.get("table.edit.dept.admin"),
            ),
        ),
    )
    table.columnModel.getColumn(3).cellEditor = DefaultCellEditor(
        WSpinner().also {
            it.minimum = 18.0
            it.maximum = 70.0
            it.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE
        },
    )

    val log = WLabel(Messages.get("table.edit.hint"))
    log.textWrapping = TextWrapping.WRAP
    table.addBeginningEditListener { event ->
        log.text = Messages.get("table.edit.beginning", table.getColumnName(event.column), (event.modelRow + 1).toString())
    }
    table.addCellEditEndingListener { event ->
        log.text = Messages.get("table.edit.ending", table.getColumnName(event.column), event.action.toString())
    }
    // The committed value is written back via TableModel.setValueAt, converted to the column type (getColumnClass)
    model.addTableModelListener { event ->
        if (event.type == TableModelEvent.UPDATE && event.firstRow == event.lastRow && event.column >= 0) {
            val value = model.getValueAt(event.firstRow, event.column)
            log.text = Messages.get(
                "table.edit.modelupdated",
                model.getColumnName(event.column),
                (event.firstRow + 1).toString(),
                value.toString(),
                value?.javaClass?.simpleName.toString(),
            )
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(log)

    val readOnly = optionCheckBox(Messages.get("table.edit.readonly"), false) { table.isReadOnly = it }
    val ageEditable = optionCheckBox(Messages.get("table.edit.ageeditable"), true) {
        table.columnModel.getColumn(3).isEditable = it
    }
    val commit = WButton(Messages.get("table.edit.commit"))
    commit.addActionListener { log.text = "CommitEdit → ${table.stopCellEditing()}" }
    val cancel = WButton(Messages.get("table.edit.cancel"))
    cancel.addActionListener { log.text = "CancelEdit → ${table.cancelCellEditing()}" }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("table.edit.label")))
    options.add(readOnly)
    options.add(ageEditable)
    options.add(commit)
    options.add(cancel)
    return buildExample(Messages.get("table.example.editing.title"), body, options)
}

/** Grouping: groupBy / clearGrouping / ExpandAllGroups / CollapseAllGroups / GroupHeaderTemplate. */
private fun buildTableGroupingExample(): WComponent {
    val table = buildProductTable()
    table.groupBy(1) // Group by category

    val groupBy = WComboBox(
        listOf(
            Messages.get("table.group.category"),
            Messages.get("table.group.instock"),
            Messages.get("table.group.pricerange"),
            Messages.get("table.group.none"),
        ),
    )
    groupBy.selectedIndex = 0
    groupBy.addListSelectionListener {
        when (groupBy.selectedIndex) {
            0 -> table.groupBy(1)
            1 -> table.groupBy(4)
            2 -> table.groupBy { model, row ->
                if ((model.getValueAt(row, 2) as Int) >= 300) Messages.get("table.group.over300") else Messages.get("table.group.under300")
            }
            else -> table.clearGrouping()
        }
    }
    val expand = WButton(Messages.get("table.group.expandall"))
    expand.addActionListener { table.expandAllGroups() }
    val collapse = WButton(Messages.get("table.group.collapseall"))
    collapse.addActionListener { table.collapseAllGroups() }
    val customHeader = optionCheckBox(Messages.get("table.group.customheader"), false) { custom ->
        table.groupHeaderTemplate = if (custom) {
            "<StackPanel Orientation=\"Horizontal\" Spacing=\"8\" Padding=\"8,4\">" +
                "<TextBlock Text=\"{Binding KeyText}\" FontWeight=\"SemiBold\" Foreground=\"{ThemeResource AccentTextFillColorPrimaryBrush}\" />" +
                "<TextBlock Text=\"{Binding ItemCount}\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />" +
                "<TextBlock Text=\"" + Messages.get("table.group.items") + "\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />" +
                "</StackPanel>"
        } else {
            null
        }
    }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("table.group.key.label")))
    options.add(groupBy)
    options.add(expand)
    options.add(collapse)
    options.add(customHeader)
    return buildExample(Messages.get("table.example.grouping.title"), table, options)
}

/** Cell display: TableCellRenderer / XAML cell templates / frozen columns / header and cell tooltips. */
private fun buildTableCellsExample(): WComponent {
    val model = object : DefaultTableModel(
        listOf(
            listOf(Messages.get("table.cells.task1"), Messages.get("table.cells.state.inprogress"), 60, Messages.get("table.cells.memo1")),
            listOf(Messages.get("table.cells.task2"), Messages.get("table.cells.state.done"), 100, Messages.get("table.cells.memo2")),
            listOf(Messages.get("table.cells.task3"), Messages.get("table.cells.state.notstarted"), 0, Messages.get("table.cells.memo3")),
            listOf(Messages.get("table.cells.task4"), Messages.get("table.cells.state.inprogress"), 35, Messages.get("table.cells.memo4")),
        ),
        listOf(
            Messages.get("table.cells.column.task"),
            Messages.get("table.cells.column.state"),
            Messages.get("table.cells.column.progress"),
            Messages.get("table.cells.column.memo"),
        ),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = if (columnIndex == 2) Integer::class.java else String::class.java
    }
    val table = buildProductTable(model)
    table.height = 220.0

    val task = table.columnModel.getColumn(0)
    task.isFrozen = true // The task name stays visible even when scrolling horizontally
    task.preferredWidth = 180.0
    task.headerToolTip = Messages.get("table.cells.task.tooltip")

    // Show the status as a colored label with a XAML template ({Binding} is the cell value)
    val state = table.columnModel.getColumn(1)
    state.cellTemplate = "<Border Background=\"{ThemeResource AccentFillColorDefaultBrush}\" CornerRadius=\"10\" " +
        "Padding=\"10,2\" Margin=\"8,0\" HorizontalAlignment=\"Left\">" +
        "<TextBlock Text=\"{Binding}\" Foreground=\"{ThemeResource TextOnAccentFillColorPrimaryBrush}\" /></Border>"
    state.headerToolTip = Messages.get("table.cells.state.tooltip")

    // Show the progress with a Kotlin component (a progress bar) via a renderer
    val progress = table.columnModel.getColumn(2)
    progress.preferredWidth = 160.0
    progress.cellRenderer = TableCellRenderer { _, value, _, _, _, _ ->
        val bar = WProgressBar(value = (value as Int).toDouble())
        bar.setMargin(8.0, 0.0, 8.0, 0.0)
        bar.verticalAlignmentInParent = VerticalAlignment.CENTER
        bar
    }
    progress.headerToolTip = Messages.get("table.cells.progress.tooltip")

    // The notes are long, so show the full text in a cell tooltip
    val memo = table.columnModel.getColumn(3)
    memo.preferredWidth = 240.0
    memo.cellToolTipColumn = 3

    val note = optionsLabel(Messages.get("table.cells.note"))
    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(note)
    table.width = 520.0
    return buildExample(Messages.get("table.example.cells.title"), body)
}

/** Appearance: grid lines / header / density / row background / stripes / display when there are no rows. */
private fun buildTableAppearanceExample(): WComponent {
    val model = ProductTableModel()
    val table = buildProductTable(model)
    table.emptyText = Messages.get("table.appearance.empty")
    val removedRows = mutableListOf<List<Any?>>()

    val horizontal = optionCheckBox(Messages.get("table.appearance.horizontal"), table.showHorizontalLines) { table.showHorizontalLines = it }
    val vertical = optionCheckBox(Messages.get("table.appearance.vertical"), table.showVerticalLines) { table.showVerticalLines = it }
    val header = optionCheckBox(Messages.get("table.appearance.header"), table.isHeaderVisible) { table.isHeaderVisible = it }
    val striped = optionCheckBox(Messages.get("table.appearance.striped"), false) {
        table.alternatingRowBackground = if (it) WColor(0x20, 0x80, 0x80, 0x80) else null
    }
    val resizable = optionCheckBox(Messages.get("table.appearance.resizable"), table.canUserResizeColumns) {
        table.canUserResizeColumns = it
    }
    val density = WComboBox(listOf("COMPACT", "STANDARD", "COMFORTABLE"))
    density.selectedIndex = table.density.ordinal
    density.addListSelectionListener { table.density = TableDensity.entries[density.selectedIndex] }
    val clearRows = WButton(Messages.get("table.appearance.clearrows"))
    clearRows.addActionListener {
        while (model.getRowCount() > 0) {
            removedRows += (0 until model.getColumnCount()).map { model.getValueAt(0, it) }
            model.removeRow(0)
        }
    }
    val restoreRows = WButton(Messages.get("table.appearance.restorerows"))
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
    options.add(optionsLabel(Messages.get("table.appearance.density.label")))
    options.add(density)
    options.add(clearRows)
    options.add(restoreRows)
    return buildExample(Messages.get("table.example.appearance.title"), table, options)
}

/** Column operations: TableColumnModel (move / show and hide / add and remove) and column widths. */
private fun buildTableColumnsExample(): WComponent {
    val model = ProductTableModel()
    val table = buildProductTable(model)
    val status = WLabel("")
    val updateStatus = {
        val names = (0 until table.columnCount).map { table.getColumnName(it) }
        status.text = Messages.get("table.columns.order", names.joinToString(" / "))
    }
    updateStatus()

    val moveLast = WButton(Messages.get("table.columns.movelast"))
    moveLast.addActionListener {
        table.moveColumn(0, table.columnCount - 1)
        updateStatus()
    }
    val hideCategory = optionCheckBox(Messages.get("table.columns.showcategory"), true) { visible ->
        table.columnModel.getColumns().first { it.modelIndex == 1 }.isVisible = visible
    }
    var taxColumn: TableColumn? = null
    val addTax = WButton(Messages.get("table.columns.addtax"))
    addTax.addActionListener {
        val existing = taxColumn
        if (existing == null) {
            val column = TableColumn(2)
            column.headerValue = Messages.get("table.columns.tax.header")
            column.cellRenderer = DefaultTableCellRenderer().also { it.horizontalAlignment = HorizontalAlignment.RIGHT }
                .let { base ->
                    TableCellRenderer { view, value, selected, focus, row, col ->
                        val price = Messages.get("table.columns.tax.value", ((value as Int) * 1.08).toInt().toString())
                        base.getTableCellRendererComponent(view, price, selected, focus, row, col)
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
    val widen = WButton(Messages.get("table.columns.widen"))
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
    return buildExample(Messages.get("table.example.columns.title"), body, options)
}

// endregion

// region Tree page

/** The Tree page: lines up demos for trying out WTree's various features. */
internal fun buildTreePage(): WComponent {
    val page = buildPage("Tree", Messages.get("tree.description"))

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
    tree.horizontalAlignmentInParent = HorizontalAlignment.LEFT

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
    val result = WLabel(Messages.get("tree.click.none"))
    result.textWrapping = TextWrapping.WRAP

    val tree = buildSampleTree()
    tree.canDragItems = true
    tree.canReorderItems = true
    tree.addItemInvokedListener { node ->
        result.text = if (node == null) {
            Messages.get("tree.click.none")
        } else {
            Messages.get("tree.click.status", node.text, node.depth.toString())
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(tree)
    body.add(result)
    return buildExample(Messages.get("tree.example.simple.title"), body)
}

/** Multiple selection: checkboxes from SelectionMode = MULTIPLE, plus SelectAll / SelectedNodes. */
private fun buildTreeMultiSelectExample(): WComponent {
    val tree = buildSampleTree()
    tree.selectionMode = TreeViewSelectionMode.MULTIPLE

    val result = WLabel(Messages.get("tree.selected.none"))
    result.textWrapping = TextWrapping.WRAP
    val showButton = WButton(Messages.get("tree.showselection"))
    showButton.addActionListener {
        val names = tree.selectedNodes.joinToString(", ") { it.text }
        result.text = if (names.isEmpty()) Messages.get("tree.selected.none") else Messages.get("tree.selected.status", names)
    }
    val selectAllButton = WButton(Messages.get("tree.selectall"))
    selectAllButton.addActionListener { tree.selectAll() }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(showButton)
    buttons.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(tree)
    body.add(buttons)
    body.add(result)
    return buildExample(Messages.get("tree.example.multiselect.title"), body)
}

/** Expand and collapse: the Expand / Collapse methods and the Expanding / Collapsed events. */
private fun buildTreeExpandCollapseExample(): WComponent {
    val log = WLabel(Messages.get("tree.event.none"))
    log.textWrapping = TextWrapping.WRAP

    val tree = buildSampleTree()
    tree.addExpandingListener { node -> log.text = Messages.get("tree.event.expanding", node?.text.toString()) }
    tree.addCollapsedListener { node -> log.text = Messages.get("tree.event.collapsed", node?.text.toString()) }

    val expandButton = WButton(Messages.get("tree.expandall"))
    expandButton.addActionListener {
        for (root in tree.rootNodes) tree.expand(root)
    }
    val collapseButton = WButton(Messages.get("tree.collapseall"))
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
    return buildExample(Messages.get("tree.example.expandcollapse.title"), body)
}

// endregion
