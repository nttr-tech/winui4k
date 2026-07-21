package jp.hisano.winui4k.gallery

import jp.hisano.winui4k.swing.ListViewSelectionMode
import jp.hisano.winui4k.swing.Orientation
import jp.hisano.winui4k.swing.WButton
import jp.hisano.winui4k.swing.WCommand
import jp.hisano.winui4k.swing.WComponent
import jp.hisano.winui4k.swing.WFlyout
import jp.hisano.winui4k.swing.WFrame
import jp.hisano.winui4k.swing.WLabel
import jp.hisano.winui4k.swing.WList
import jp.hisano.winui4k.swing.WPanel
import jp.hisano.winui4k.swing.WTextField
import jp.hisano.winui4k.winui.WinUiToolkit

/**
 * A WinUI 3 Gallery-style component gallery.
 * Shows a page navigation list on the left and the selected component's demo page on the right.
 * Add more pages to [pages] as they're added.
 */
fun main() {
    WinUiToolkit.launch {
        val frame = WFrame(title = "WinUI4K Gallery")

        val pageArea = WPanel()
        pageArea.add(pages.values.first()())

        val root = WPanel(spacing = 32.0, orientation = Orientation.HORIZONTAL)
        root.add(buildNavigationPane { buildPage ->
            pageArea.removeAll()
            pageArea.add(buildPage())
        })
        root.add(pageArea)

        frame.add(root)
        frame.isVisible = true
    }
}

/** Page name (WinUI control name) -> the function that builds its demo page. */
private val pages: Map<String, () -> WComponent> = linkedMapOf(
    "Button" to ::buildButtonPage,
    "ListView" to ::buildListViewPage,
)

/** The left-hand navigation. Clicking a button passes the selected page's builder to [onSelect]. */
private fun buildNavigationPane(onSelect: (() -> WComponent) -> Unit): WComponent {
    val pane = WPanel(spacing = 8.0)
    pane.add(WLabel("WinUI4K Gallery").also { it.fontSize = 20.0 })
    for ((name, buildPage) in pages) {
        pane.add(
            WButton(name).also { button ->
                button.width = 160.0
                button.addActionListener { onSelect(buildPage) }
            },
        )
    }
    return pane
}

/** The Button page: lines up demos for trying out WButton's various features. */
private fun buildButtonPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("Button").also { it.fontSize = 28.0 })
    page.add(WLabel("A button that responds to clicks. Try out WButton's various features."))

    page.add(buildSimpleButtonExample())
    page.add(buildFlyoutButtonExample())
    page.add(buildCommandButtonExample())
    return page
}

/** One demo section (heading + body). */
private fun buildExample(title: String, body: WComponent): WComponent {
    val section = WPanel(spacing = 8.0)
    section.add(WLabel(title).also { it.fontSize = 16.0 })
    section.add(body)
    return section
}

/** A basic button: responding to clicks and toggling isEnabled. */
private fun buildSimpleButtonExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val standardButton = WButton("Standard XAML Button")
    standardButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val toggleButton = WButton("Disable button")
    toggleButton.addActionListener {
        standardButton.isEnabled = !standardButton.isEnabled
        toggleButton.text = if (standardButton.isEnabled) "Disable button" else "Enable button"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(standardButton)
    row.add(toggleButton)
    row.add(result)
    return buildExample("Simple button", row)
}

/** A button with a flyout: opens a popup on click. */
private fun buildFlyoutButtonExample(): WComponent {
    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)

    flyoutContent.add(WLabel("Delete all items?"))
    flyoutContent.add(
        WButton("Yes, delete all").also { button ->
            button.addActionListener { flyout.hide() }
        },
    )

    val flyoutButton = WButton("Show options")
    flyoutButton.flyout = flyout
    return buildExample("Button with a flyout", flyoutButton)
}

/** A button with a WCommand: running the command and auto-disabling via isEnabled. */
private fun buildCommandButtonExample(): WComponent {
    val result = WLabel("Command has not run yet")
    val command = WCommand { parameter ->
        result.text = "Command ran (parameter = $parameter)"
    }

    val commandButton = WButton("Run command")
    commandButton.command = command
    commandButton.commandParameter = "Gallery"

    val toggleButton = WButton("Disable command")
    toggleButton.addActionListener {
        command.isEnabled = !command.isEnabled
        toggleButton.text = if (command.isEnabled) "Disable command" else "Enable command"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(commandButton)
    row.add(toggleButton)
    row.add(result)
    return buildExample("Button with a command", row)
}

/** The ListView page: lines up demos for trying out WList's various features. */
private fun buildListViewPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("ListView").also { it.fontSize = 28.0 })
    page.add(WLabel("A list that lines items up vertically for selection. Try out WList's various features."))

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
    val list = WList(listOf("Item 1", "Item 2", "Item 3"))
    list.width = 240.0

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
