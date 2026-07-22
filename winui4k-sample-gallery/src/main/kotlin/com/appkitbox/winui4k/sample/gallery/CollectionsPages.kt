package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.ListViewSelectionMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SelectionMode
import com.appkitbox.winui4k.SortDirection
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.TreeViewSelectionMode
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WList
import com.appkitbox.winui4k.WListBox
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTable
import com.appkitbox.winui4k.WTableColumn
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTree
import com.appkitbox.winui4k.WTreeNode

/**
 * Collections category: demo pages for ListBox / ListView / TableView / TreeView.
 */

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

// region TableView

/** The TableView page: lines up demos for trying out WTable's various features. */
internal fun buildTableViewPage(): WComponent {
    val page = buildPage(
        "TableView",
        "A table that displays data in rows and columns. Try out WTable's various " +
            "features, implemented on top of ListView based on the design of WinUI.TableView.",
    )

    page.add(buildSimpleTableExample())
    page.add(buildTableSortExample())
    page.add(buildTableRowOperationsExample())
    return page
}

/** Sample data for the TableView demos (product name, price, quantity). */
private fun buildProductTable(): WTable {
    val table = WTable(
        listOf(
            WTableColumn("Product", width = 160.0),
            WTableColumn("Price", width = 100.0),
            WTableColumn("Quantity", width = 100.0),
        ),
    )
    table.addRow("Apple", "150", "12")
    table.addRow("Orange", "80", "30")
    table.addRow("Grape", "480", "5")
    table.addRow("Peach", "320", "8")
    table.addRow("Cherry", "600", "3")
    table.width = 400.0
    return table
}

/** A basic table: responding to row selection (SelectionChanged). */
private fun buildSimpleTableExample(): WComponent {
    val result = WLabel("Selection: none")

    val table = buildProductTable()
    table.addRowSelectionListener {
        val row = table.selectedRow
        result.text = if (row < 0) {
            "Selection: none"
        } else {
            "Selection: ${table.getValueAt(row, 0)} (row = $row)"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(result)
    return buildExample("A basic table (row selection)", body)
}

/** Sorting columns: cycling through header clicks (ascending -> descending -> cleared) and sortBy / clearSort. */
private fun buildTableSortExample(): WComponent {
    val table = buildProductTable()

    val sortByPriceButton = WButton("Sort by price descending")
    sortByPriceButton.addActionListener {
        table.sortBy(1, SortDirection.DESCENDING)
    }

    val clearButton = WButton("Clear sort")
    clearButton.addActionListener {
        table.clearSort()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(sortByPriceButton)
    buttons.add(clearButton)

    val note = WLabel("Clicking a column header also cycles through ascending -> descending -> cleared.").also {
        it.foreground = TEXT_SECONDARY
        it.textWrapping = TextWrapping.WRAP
    }

    val body = WPanel(spacing = 8.0)
    body.add(note)
    body.add(buttons)
    body.add(table)
    return buildExample("Sorting columns (header click / SortBy / ClearSort)", body)
}

/** Adding and removing rows: addRow / removeRow / removeAllRows / setValueAt / rowCount. */
private fun buildTableRowOperationsExample(): WComponent {
    val table = buildProductTable()

    val count = WLabel("Row count: ${table.rowCount}")
    val updateCount = { count.text = "Row count: ${table.rowCount}" }

    var nextItemNumber = 1
    val addButton = WButton("Add row")
    addButton.addActionListener {
        table.addRow("New item $nextItemNumber", "${nextItemNumber * 100}", "1")
        nextItemNumber++
        updateCount()
    }

    val removeButton = WButton("Remove selected row")
    removeButton.addActionListener {
        val row = table.selectedRow
        if (row >= 0) {
            table.removeRow(row)
            updateCount()
        }
    }

    val incrementButton = WButton("Selected quantity +1")
    incrementButton.addActionListener {
        val row = table.selectedRow
        if (row >= 0) {
            val quantity = table.getValueAt(row, 2).toIntOrNull() ?: 0
            table.setValueAt(row, 2, "${quantity + 1}")
        }
    }

    val clearButton = WButton("Remove all")
    clearButton.addActionListener {
        table.removeAllRows()
        updateCount()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(addButton)
    buttons.add(removeButton)
    buttons.add(incrementButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(table)
    body.add(count)
    return buildExample("Adding and removing rows (AddRow / RemoveRow / SetValueAt)", body)
}

// endregion

// region TreeView

/** TreeView page: lines up demos for trying out WTree's various features. */
internal fun buildTreeViewPage(): WComponent {
    val page = buildPage("TreeView", "A tree that can expand and collapse hierarchical data. Try out WTree's various features.")

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
