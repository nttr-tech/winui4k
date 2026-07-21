package jp.hisano.winui4k.gallery

import jp.hisano.winui4k.swing.ExpandDirection
import jp.hisano.winui4k.swing.GridLength
import jp.hisano.winui4k.swing.ListViewSelectionMode
import jp.hisano.winui4k.swing.Orientation
import jp.hisano.winui4k.swing.SplitViewDisplayMode
import jp.hisano.winui4k.swing.SplitViewPanePlacement
import jp.hisano.winui4k.swing.WBorder
import jp.hisano.winui4k.swing.WButton
import jp.hisano.winui4k.swing.WCanvas
import jp.hisano.winui4k.swing.WColor
import jp.hisano.winui4k.swing.WCommand
import jp.hisano.winui4k.swing.WComponent
import jp.hisano.winui4k.swing.WExpander
import jp.hisano.winui4k.swing.WFlyout
import jp.hisano.winui4k.swing.WFrame
import jp.hisano.winui4k.swing.WGrid
import jp.hisano.winui4k.swing.WLabel
import jp.hisano.winui4k.swing.WList
import jp.hisano.winui4k.swing.WPanel
import jp.hisano.winui4k.swing.WRelativePanel
import jp.hisano.winui4k.swing.WSplitView
import jp.hisano.winui4k.swing.WTextField
import jp.hisano.winui4k.swing.WVariableSizedWrapGrid
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
    "Border" to ::buildBorderPage,
    "Button" to ::buildButtonPage,
    "Canvas" to ::buildCanvasPage,
    "Expander" to ::buildExpanderPage,
    "Grid" to ::buildGridPage,
    "ListView" to ::buildListViewPage,
    "RelativePanel" to ::buildRelativePanelPage,
    "SplitView" to ::buildSplitViewPage,
    "StackPanel" to ::buildStackPanelPage,
    "VariableSizedWrapGrid" to ::buildVariableSizedWrapGridPage,
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

/** A colored tile for the layout demos (a Border painted with a background). Fills its parent if a size isn't given. */
private fun buildTile(color: WColor, width: Double = Double.NaN, height: Double = Double.NaN, label: String = ""): WBorder {
    val tile = WBorder()
    tile.background = color
    tile.cornerRadius = 4.0
    if (!width.isNaN()) tile.width = width
    if (!height.isNaN()) tile.height = height
    if (label.isNotEmpty()) {
        tile.padding = 8.0
        tile.child = WLabel(label)
    }
    return tile
}

/** The Border page: lines up demos for trying out WBorder's various features. */
private fun buildBorderPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("Border").also { it.fontSize = 28.0 })
    page.add(WLabel("A container that draws a border, background, and rounded corners around a single child. Try out WBorder's various features."))

    page.add(buildBorderStyleExample())
    page.add(buildBorderBackgroundExample())
    return page
}

/** Switching the border thickness and corner rounding with buttons. */
private fun buildBorderStyleExample(): WComponent {
    val border = WBorder(WLabel("Content with a border"))
    border.borderColor = WColor.BLUE
    border.borderThickness = 2.0
    border.padding = 16.0

    val thicknessButton = WButton("Increase border thickness")
    thicknessButton.addActionListener {
        border.borderThickness = if (border.borderThickness >= 8.0) 2.0 else border.borderThickness + 2.0
    }

    val cornerButton = WButton("Toggle rounded corners")
    cornerButton.addActionListener {
        border.cornerRadius = if (border.cornerRadius > 0) 0.0 else 12.0
    }

    val colorButton = WButton("Change border color")
    var colorIndex = 0
    val colors = listOf(WColor.BLUE, WColor.RED, WColor.GREEN, WColor.ORANGE)
    colorButton.addActionListener {
        colorIndex = (colorIndex + 1) % colors.size
        border.borderColor = colors[colorIndex]
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(thicknessButton)
    buttons.add(cornerButton)
    buttons.add(colorButton)

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(buttons)
    return buildExample("Border and corner rounding (BorderBrush / BorderThickness / CornerRadius)", body)
}

/** Background color and padding. */
private fun buildBorderBackgroundExample(): WComponent {
    val border = WBorder(WLabel("Content with a background"))
    border.background = WColor.LIGHT_GRAY
    border.cornerRadius = 8.0
    border.padding = 16.0

    val paddingButton = WButton("Increase padding")
    paddingButton.addActionListener {
        border.padding = if (border.padding >= 48.0) 16.0 else border.padding + 16.0
    }

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(paddingButton)
    return buildExample("Background and padding (Background / Padding)", body)
}

/** The Canvas page: lines up demos for trying out WCanvas's various features. */
private fun buildCanvasPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("Canvas").also { it.fontSize = 28.0 })
    page.add(WLabel("A panel that positions children with absolute coordinates. Try out WCanvas's various features."))

    page.add(buildCanvasPositionExample())
    page.add(buildCanvasZIndexExample())
    return page
}

/** Positioning: moving a child with SetLeft / SetTop. */
private fun buildCanvasPositionExample(): WComponent {
    val canvas = WCanvas()
    canvas.width = 320.0
    canvas.height = 160.0

    canvas.add(buildTile(WColor.LIGHT_GRAY, 320.0, 160.0), 0.0, 0.0) // background (visualizes the canvas bounds)
    val movingTile = buildTile(WColor.BLUE, 48.0, 48.0)
    var x = 16.0
    var y = 16.0
    canvas.add(movingTile, x, y)

    val moveButton = WButton("Move the tile")
    moveButton.addActionListener {
        x = if (x >= 256.0) 16.0 else x + 48.0
        y = if (y >= 96.0) 16.0 else y + 16.0
        canvas.setLocation(movingTile, x, y)
    }

    val body = WPanel(spacing = 8.0)
    body.add(canvas)
    body.add(moveButton)
    return buildExample("Absolute positioning (Canvas.Left / Canvas.Top)", body)
}

/** Stacking order: swapping front/back with SetZIndex. */
private fun buildCanvasZIndexExample(): WComponent {
    val canvas = WCanvas()
    canvas.width = 320.0
    canvas.height = 120.0

    val redTile = buildTile(WColor.RED, 64.0, 64.0)
    val greenTile = buildTile(WColor.GREEN, 64.0, 64.0)
    canvas.add(redTile, 16.0, 16.0)
    canvas.add(greenTile, 48.0, 40.0)

    var redOnTop = false
    val swapButton = WButton("Swap stacking order")
    swapButton.addActionListener {
        redOnTop = !redOnTop
        canvas.setZIndex(redTile, if (redOnTop) 1 else 0)
        canvas.setZIndex(greenTile, if (redOnTop) 0 else 1)
    }

    val body = WPanel(spacing = 8.0)
    body.add(canvas)
    body.add(swapButton)
    return buildExample("Stacking order (Canvas.ZIndex)", body)
}

/** The Expander page: lines up demos for trying out WExpander's various features. */
private fun buildExpanderPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("Expander").also { it.fontSize = 28.0 })
    page.add(WLabel("A control that expands/collapses its content when the header is clicked. Try out WExpander's various features."))

    page.add(buildExpanderBasicExample())
    page.add(buildExpanderDirectionExample())
    return page
}

/** Basic expand/collapse plus the Expanding / Collapsed events. */
private fun buildExpanderBasicExample(): WComponent {
    val state = WLabel("State: collapsed")

    val content = WPanel(spacing = 8.0)
    content.add(WLabel("This is the expanded content."))
    content.add(WButton("A button inside"))

    val expander = WExpander("Click to expand or collapse", content)
    expander.width = 320.0
    expander.addExpandListener { state.text = "State: expanded" }
    expander.addCollapseListener { state.text = "State: collapsed" }

    val toggleButton = WButton("Toggle from code")
    toggleButton.addActionListener { expander.isExpanded = !expander.isExpanded }

    val body = WPanel(spacing = 8.0)
    body.add(expander)
    body.add(toggleButton)
    body.add(state)
    return buildExample("Expand/collapse and events (IsExpanded / Expanding / Collapsed)", body)
}

/** The expand direction (ExpandDirection). */
private fun buildExpanderDirectionExample(): WComponent {
    val expander = WExpander("A header whose expand direction can change", WLabel("Can expand either up or down."))
    expander.width = 320.0

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (direction in ExpandDirection.entries) {
        buttons.add(
            WButton(direction.name).also { button ->
                button.addActionListener { expander.expandDirection = direction }
            },
        )
    }

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(expander)
    return buildExample("Expand direction (ExpandDirection)", body)
}

/** The Grid page: lines up demos for trying out WGrid's various features. */
private fun buildGridPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("Grid").also { it.fontSize = 28.0 })
    page.add(WLabel("A panel that defines rows and columns and places children into cells. Try out WGrid's various features."))

    page.add(buildGridCellExample())
    page.add(buildGridSpanExample())
    return page
}

/** Row/column definitions and cell placement: combining Auto / Pixel / Star. */
private fun buildGridCellExample(): WComponent {
    val grid = WGrid(rowSpacing = 4.0, columnSpacing = 4.0)
    grid.width = 320.0
    grid.addRow(GridLength.pixel(48.0))
    grid.addRow(GridLength.pixel(48.0))
    grid.addColumn(GridLength.pixel(80.0))
    grid.addColumn(GridLength.star())
    grid.addColumn(GridLength.star(2.0))

    grid.add(buildTile(WColor.RED, label = "80px"), row = 0, column = 0)
    grid.add(buildTile(WColor.GREEN, label = "1*"), row = 0, column = 1)
    grid.add(buildTile(WColor.BLUE, label = "2*"), row = 0, column = 2)
    grid.add(buildTile(WColor.ORANGE, label = "Row 2"), row = 1, column = 0)
    grid.add(buildTile(WColor.PURPLE, label = "Row 2"), row = 1, column = 2)

    return buildExample("Row/column definitions and cell placement (RowDefinitions / ColumnDefinitions)", grid)
}

/** RowSpan / ColumnSpan and cell spacing. */
private fun buildGridSpanExample(): WComponent {
    val grid = WGrid(rowSpacing = 4.0, columnSpacing = 4.0)
    grid.width = 320.0
    repeat(2) { grid.addRow(GridLength.pixel(48.0)) }
    repeat(3) { grid.addColumn(GridLength.star()) }

    grid.add(buildTile(WColor.BLUE, label = "2 columns"), row = 0, column = 0, columnSpan = 2)
    grid.add(buildTile(WColor.GREEN, label = "2 rows"), row = 0, column = 2, rowSpan = 2)
    grid.add(buildTile(WColor.RED, label = "1 cell"), row = 1, column = 0)
    grid.add(buildTile(WColor.ORANGE, label = "1 cell"), row = 1, column = 1)

    val spacingButton = WButton("Toggle cell spacing")
    spacingButton.addActionListener {
        val next = if (grid.rowSpacing > 4.0) 4.0 else 16.0
        grid.rowSpacing = next
        grid.columnSpacing = next
    }

    val body = WPanel(spacing = 8.0)
    body.add(grid)
    body.add(spacingButton)
    return buildExample("Cell spans and spacing (RowSpan / ColumnSpan / Spacing)", body)
}

/** The RelativePanel page: lines up demos for trying out WRelativePanel's various features. */
private fun buildRelativePanelPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("RelativePanel").also { it.fontSize = 28.0 })
    page.add(WLabel("A panel that positions children relative to each other or to the panel. Try out WRelativePanel's various features."))

    page.add(buildRelativePanelSiblingExample())
    page.add(buildRelativePanelAlignExample())
    return page
}

/** Placement relative to sibling elements (RightOf / Below). */
private fun buildRelativePanelSiblingExample(): WComponent {
    val panel = WRelativePanel()
    panel.width = 320.0
    panel.height = 160.0

    val anchor = buildTile(WColor.BLUE, label = "Anchor")
    val right = buildTile(WColor.GREEN, label = "Right")
    val below = buildTile(WColor.RED, label = "Below")
    val rightBelow = buildTile(WColor.ORANGE, label = "Right+Below")

    panel.add(anchor)
    panel.add(right)
    panel.add(below)
    panel.add(rightBelow)

    panel.placeRightOf(right, anchor)
    panel.placeBelow(below, anchor)
    panel.placeRightOf(rightBelow, below)
    panel.placeBelow(rightBelow, right)

    return buildExample("Placement relative to siblings (RightOf / Below)", panel)
}

/** Placement relative to the panel (AlignXxxWithPanel). */
private fun buildRelativePanelAlignExample(): WComponent {
    val panel = WRelativePanel()
    panel.width = 320.0
    panel.height = 160.0

    panel.add(buildTile(WColor.LIGHT_GRAY, 320.0, 160.0)) // background (visualizes the panel bounds)

    val topRight = buildTile(WColor.GREEN, 48.0, 48.0)
    panel.add(topRight)
    panel.alignRightWithPanel(topRight)
    panel.alignTopWithPanel(topRight)

    val bottomRight = buildTile(WColor.RED, 48.0, 48.0)
    panel.add(bottomRight)
    panel.alignRightWithPanel(bottomRight)
    panel.alignBottomWithPanel(bottomRight)

    val center = buildTile(WColor.PURPLE, 48.0, 48.0)
    panel.add(center)
    panel.alignHorizontalCenterWithPanel(center)
    panel.alignVerticalCenterWithPanel(center)

    return buildExample("Placement relative to the panel (AlignXxxWithPanel)", panel)
}

/** The SplitView page: lines up demos for trying out WSplitView's various features. */
private fun buildSplitViewPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("SplitView").also { it.fontSize = 28.0 })
    page.add(WLabel("A control that lines up a collapsible pane alongside content. Try out WSplitView's various features."))

    page.add(buildSplitViewExample())
    return page
}

/** Opening/closing the pane, display mode, and placement. */
private fun buildSplitViewExample(): WComponent {
    val paneContent = WPanel(spacing = 8.0)
    paneContent.add(WLabel("Pane"))
    paneContent.add(WButton("Menu 1"))
    paneContent.add(WButton("Menu 2"))

    val pane = WBorder(paneContent)
    pane.background = WColor.LIGHT_GRAY
    pane.padding = 8.0

    val mainContent = WBorder(WLabel("Main content"))
    mainContent.padding = 16.0

    val splitView = WSplitView(pane = pane, content = mainContent)
    splitView.width = 480.0
    splitView.height = 200.0
    splitView.openPaneLength = 160.0
    splitView.displayMode = SplitViewDisplayMode.INLINE

    val toggleButton = WButton("Toggle pane")
    toggleButton.addActionListener { splitView.isPaneOpen = !splitView.isPaneOpen }

    val modeButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    modeButtons.add(WLabel("DisplayMode:"))
    for (mode in SplitViewDisplayMode.entries) {
        modeButtons.add(
            WButton(mode.name).also { button ->
                button.addActionListener { splitView.displayMode = mode }
            },
        )
    }

    val placementButton = WButton("Flip pane placement")
    placementButton.addActionListener {
        splitView.panePlacement = when (splitView.panePlacement) {
            SplitViewPanePlacement.LEFT -> SplitViewPanePlacement.RIGHT
            SplitViewPanePlacement.RIGHT -> SplitViewPanePlacement.LEFT
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(splitView)
    body.add(toggleButton)
    body.add(modeButtons)
    body.add(placementButton)
    return buildExample("Opening/closing the pane (IsPaneOpen / DisplayMode / PanePlacement)", body)
}

/** The StackPanel page: lines up demos for trying out WPanel's various features. */
private fun buildStackPanelPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("StackPanel").also { it.fontSize = 28.0 })
    page.add(WLabel("A panel that lines up children in one direction. Try out WPanel's various features."))

    page.add(buildStackPanelExample())
    return page
}

/** The direction children line up in, and the spacing between them. */
private fun buildStackPanelExample(): WComponent {
    val panel = WPanel(spacing = 8.0)
    panel.add(buildTile(WColor.RED, 48.0, 48.0))
    panel.add(buildTile(WColor.GREEN, 48.0, 48.0))
    panel.add(buildTile(WColor.BLUE, 48.0, 48.0))

    val orientationButton = WButton("Toggle direction")
    orientationButton.addActionListener {
        panel.orientation = when (panel.orientation) {
            Orientation.VERTICAL -> Orientation.HORIZONTAL
            Orientation.HORIZONTAL -> Orientation.VERTICAL
        }
    }

    val spacingButton = WButton("Toggle spacing")
    spacingButton.addActionListener {
        panel.spacing = if (panel.spacing > 8.0) 8.0 else 24.0
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(orientationButton)
    buttons.add(spacingButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(panel)
    return buildExample("Direction and spacing (Orientation / Spacing)", body)
}

/** The VariableSizedWrapGrid page: lines up demos for trying out WVariableSizedWrapGrid's various features. */
private fun buildVariableSizedWrapGridPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("VariableSizedWrapGrid").also { it.fontSize = 28.0 })
    page.add(WLabel("A panel that wraps children by cell. Try out WVariableSizedWrapGrid's various features."))

    page.add(buildWrapGridSpanExample())
    return page
}

/** Cell spans and wrapping. */
private fun buildWrapGridSpanExample(): WComponent {
    val grid = WVariableSizedWrapGrid(itemWidth = 56.0, itemHeight = 56.0)
    grid.orientation = Orientation.HORIZONTAL
    grid.maximumRowsOrColumns = 4

    grid.add(buildTile(WColor.BLUE, label = "2×2"), rowSpan = 2, columnSpan = 2)
    grid.add(buildTile(WColor.RED, label = "1×1"), rowSpan = 1, columnSpan = 1)
    grid.add(buildTile(WColor.GREEN, label = "1×2"), rowSpan = 1, columnSpan = 2)
    grid.add(buildTile(WColor.ORANGE, label = "1×1"), rowSpan = 1, columnSpan = 1)
    grid.add(buildTile(WColor.PURPLE, label = "2×1"), rowSpan = 2, columnSpan = 1)
    grid.add(buildTile(WColor.GRAY, label = "1×1"), rowSpan = 1, columnSpan = 1)

    val maxButton = WButton("Toggle wrap count")
    maxButton.addActionListener {
        grid.maximumRowsOrColumns = if (grid.maximumRowsOrColumns == 4) 6 else 4
    }

    val orientationButton = WButton("Toggle direction")
    orientationButton.addActionListener {
        grid.orientation = when (grid.orientation) {
            Orientation.VERTICAL -> Orientation.HORIZONTAL
            Orientation.HORIZONTAL -> Orientation.VERTICAL
        }
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(maxButton)
    buttons.add(orientationButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(grid)
    return buildExample("Cell spans and wrapping (RowSpan / ColumnSpan / MaximumRowsOrColumns)", body)
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
