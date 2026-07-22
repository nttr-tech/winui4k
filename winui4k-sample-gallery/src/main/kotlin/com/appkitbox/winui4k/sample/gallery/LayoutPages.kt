package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ExpandDirection
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SplitViewDisplayMode
import com.appkitbox.winui4k.SplitViewPanePlacement
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCanvas
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WExpander
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLinearGradientPaint
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRelativePanel
import com.appkitbox.winui4k.WSplitView
import com.appkitbox.winui4k.WVariableSizedWrapGrid

/**
 * Layout category: demo pages for Border / Canvas / Expander / Grid / RelativePanel / SplitView / StackPanel / VariableSizedWrapGrid.
 */

// region Border

/** The Border page: lines up demos for trying out WBorder's various features. */
internal fun buildBorderPage(): WComponent {
    val page = buildPage("Border", "A container that draws a border, background, and rounded corners around a single child. Try out WBorder's various features.")

    page.add(buildBorderStyleExample())
    page.add(buildBorderBackgroundExample())
    page.add(buildBorderGradientExample())
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

/** Switching between a gradient background and its angle. */
private fun buildBorderGradientExample(): WComponent {
    val stops = listOf(0.0 to WColor.BLUE, 1.0 to WColor.PURPLE)

    val border = WBorder(WLabel("Content with a gradient background").also { it.foreground = WColor.WHITE })
    border.backgroundGradient = WLinearGradientPaint(stops)
    border.cornerRadius = 8.0
    border.padding = 16.0
    border.width = 320.0
    border.height = 80.0
    border.horizontalAlignment = HorizontalAlignment.LEFT

    var angle = 90.0
    val angleButton = WButton("Change angle (90°)")
    angleButton.addActionListener {
        angle = if (angle >= 270.0) 0.0 else angle + 90.0
        border.backgroundGradient = WLinearGradientPaint(stops, angle = angle)
        angleButton.text = "Change angle (${angle.toInt()}°)"
    }

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(angleButton)
    return buildExample("Gradient background (LinearGradientBrush)", body)
}

// endregion

// region Canvas

/** The Canvas page: lines up demos for trying out WCanvas's various features. */
internal fun buildCanvasPage(): WComponent {
    val page = buildPage("Canvas", "A panel that positions children with absolute coordinates. Try out WCanvas's various features.")

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

// endregion

// region Expander

/** The Expander page: lines up demos for trying out WExpander's various features. */
internal fun buildExpanderPage(): WComponent {
    val page = buildPage("Expander", "A control that expands/collapses its content when the header is clicked. Try out WExpander's various features.")

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

// endregion

// region Grid

/** The Grid page: lines up demos for trying out WGrid's various features. */
internal fun buildGridPage(): WComponent {
    val page = buildPage("Grid", "A panel that defines rows and columns and places children into cells. Try out WGrid's various features.")

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

// endregion

// region RelativePanel

/** The RelativePanel page: lines up demos for trying out WRelativePanel's various features. */
internal fun buildRelativePanelPage(): WComponent {
    val page = buildPage("RelativePanel", "A panel that positions children relative to each other or to the panel. Try out WRelativePanel's various features.")

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

// endregion

// region SplitView

/** The SplitView page: lines up demos for trying out WSplitView's various features. */
internal fun buildSplitViewPage(): WComponent {
    val page = buildPage("SplitView", "A control that lines up a collapsible pane alongside content. Try out WSplitView's various features.")

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

// endregion

// region StackPanel

/** The StackPanel page: lines up demos for trying out WPanel's various features. */
internal fun buildStackPanelPage(): WComponent {
    val page = buildPage("StackPanel", "A panel that lines up children in one direction. Try out WPanel's various features.")

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

// endregion

// region VariableSizedWrapGrid

/** The VariableSizedWrapGrid page: lines up demos for trying out WVariableSizedWrapGrid's various features. */
internal fun buildVariableSizedWrapGridPage(): WComponent {
    val page = buildPage("VariableSizedWrapGrid", "A panel that wraps children by cell. Try out WVariableSizedWrapGrid's various features.")

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

// endregion
