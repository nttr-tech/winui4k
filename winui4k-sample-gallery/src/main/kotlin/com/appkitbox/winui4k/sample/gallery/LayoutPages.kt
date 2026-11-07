package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ContentAlignment
import com.appkitbox.winui4k.ExpandDirection
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SplitViewDisplayMode
import com.appkitbox.winui4k.SplitViewPanePlacement
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WBorderLayout
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCanvas
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WExpander
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLayoutPanel
import com.appkitbox.winui4k.WLinearGradientPaint
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRelativePanel
import com.appkitbox.winui4k.WSettingsCard
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WSplitView
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.WVariableSizedWrapGrid

/*
 * Layout category: demo pages for Border / Canvas / Expander / Grid / RelativePanel / SplitView / StackPanel / VariableSizedWrapGrid.
 */

// region Border

/** The Border page: lines up demos for trying out WBorder's various features. */
internal fun buildBorderPage(): WComponent {
    val page = buildPage("Border", Messages.get("border.description"))

    page.add(buildBorderStyleExample())
    page.add(buildBorderBackgroundExample())
    page.add(buildBorderGradientExample())
    return page
}

/** Switching the border thickness and corner rounding with buttons. */
private fun buildBorderStyleExample(): WComponent {
    val border = WBorder(WLabel(Messages.get("border.example.style.content")))
    border.borderColor = DEMO_BLUE
    border.borderThickness = 2.0
    border.padding = 16.0

    val thicknessButton = WButton(Messages.get("border.example.style.thicker"))
    thicknessButton.addActionListener {
        border.borderThickness = if (border.borderThickness >= 8.0) 2.0 else border.borderThickness + 2.0
    }

    val cornerButton = WButton(Messages.get("border.example.style.corner"))
    cornerButton.addActionListener {
        border.cornerRadius = if (border.cornerRadius > 0) 0.0 else 12.0
    }

    val colorButton = WButton(Messages.get("border.example.style.color"))
    var colorIndex = 0
    val colors = listOf(DEMO_BLUE, DEMO_RED, DEMO_GREEN, DEMO_ORANGE)
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
    return buildExample(Messages.get("border.example.style.title"), body)
}

/** Background color and padding. */
private fun buildBorderBackgroundExample(): WComponent {
    val border = WBorder(WLabel(Messages.get("border.example.background.content")))
    border.background = WColor.LIGHT_GRAY
    border.cornerRadius = 8.0
    border.padding = 16.0

    val paddingButton = WButton(Messages.get("border.example.background.padding"))
    paddingButton.addActionListener {
        border.padding = if (border.padding >= 48.0) 16.0 else border.padding + 16.0
    }

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(paddingButton)
    return buildExample(Messages.get("border.example.background.title"), body)
}

/** Switching between a gradient background and its angle. */
private fun buildBorderGradientExample(): WComponent {
    val stops = listOf(0.0 to DEMO_BLUE, 1.0 to WColor.PURPLE)

    val border = WBorder(WLabel(Messages.get("border.example.gradient.content")).also { it.foreground = WColor.WHITE })
    border.backgroundGradient = WLinearGradientPaint(stops)
    border.cornerRadius = 8.0
    border.padding = 16.0
    border.width = 320.0
    border.height = 80.0
    border.horizontalAlignmentInParent = HorizontalAlignment.LEFT

    var angle = 90.0
    val angleButton = WButton(Messages.get("border.example.gradient.angle", "90"))
    angleButton.addActionListener {
        angle = if (angle >= 270.0) 0.0 else angle + 90.0
        border.backgroundGradient = WLinearGradientPaint(stops, angle = angle)
        angleButton.text = Messages.get("border.example.gradient.angle", angle.toInt().toString())
    }

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(angleButton)
    return buildExample(Messages.get("border.example.gradient.title"), body)
}

// endregion

// region Canvas

/** The Canvas page: lines up demos for trying out WCanvas's various features. */
internal fun buildCanvasPage(): WComponent {
    val page = buildPage("Canvas", Messages.get("canvas.description"))

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
    val movingTile = buildTile(DEMO_BLUE, 48.0, 48.0)
    var x = 16.0
    var y = 16.0
    canvas.add(movingTile, x, y)

    val moveButton = WButton(Messages.get("canvas.example.position.move"))
    moveButton.addActionListener {
        x = if (x >= 256.0) 16.0 else x + 48.0
        y = if (y >= 96.0) 16.0 else y + 16.0
        canvas.setLocation(movingTile, x, y)
    }

    val body = WPanel(spacing = 8.0)
    body.add(canvas)
    body.add(moveButton)
    return buildExample(Messages.get("canvas.example.position.title"), body)
}

/** Stacking order: swapping front/back with SetZIndex. */
private fun buildCanvasZIndexExample(): WComponent {
    val canvas = WCanvas()
    canvas.width = 320.0
    canvas.height = 120.0

    val redTile = buildTile(DEMO_RED, 64.0, 64.0)
    val greenTile = buildTile(DEMO_GREEN, 64.0, 64.0)
    canvas.add(redTile, 16.0, 16.0)
    canvas.add(greenTile, 48.0, 40.0)

    var redOnTop = false
    val swapButton = WButton(Messages.get("canvas.example.zindex.swap"))
    swapButton.addActionListener {
        redOnTop = !redOnTop
        canvas.setZIndex(redTile, if (redOnTop) 1 else 0)
        canvas.setZIndex(greenTile, if (redOnTop) 0 else 1)
    }

    val body = WPanel(spacing = 8.0)
    body.add(canvas)
    body.add(swapButton)
    return buildExample(Messages.get("canvas.example.zindex.title"), body)
}

// endregion

// region LayoutPanel

/** The LayoutPanel page: lines up demos for trying out WLayoutPanel + WBorderLayout's various features. */
internal fun buildLayoutPanelPage(): WComponent {
    val page = buildPage(
        "LayoutPanel",
        Messages.get("layoutpanel.description"),
    )

    page.add(buildBorderLayoutBasicExample())
    page.add(buildBorderLayoutDynamicExample())
    page.add(buildBorderLayoutNestedExample())
    return page
}

/** Builds a fixed-size panel for BorderLayout (explicitly left-aligned since it sits inside a vertical WPanel). */
private fun buildBorderLayoutPanel(hgap: Double = 4.0, vgap: Double = 4.0): WLayoutPanel {
    val panel = WLayoutPanel(WBorderLayout(hgap = hgap, vgap = vgap))
    panel.width = 400.0
    panel.height = 220.0
    panel.horizontalAlignmentInParent = HorizontalAlignment.LEFT
    return panel
}

/** Basic placement across the 5 regions. */
private fun buildBorderLayoutBasicExample(): WComponent {
    val panel = buildBorderLayoutPanel()
    panel.add(buildTile(DEMO_BLUE, label = "North"), WBorderLayout.Constraint.NORTH)
    panel.add(buildTile(DEMO_GREEN, label = "South"), WBorderLayout.Constraint.SOUTH)
    panel.add(buildTile(DEMO_ORANGE, label = "West"), WBorderLayout.Constraint.WEST)
    panel.add(buildTile(WColor.PURPLE, label = "East"), WBorderLayout.Constraint.EAST)
    panel.add(buildTile(WColor.LIGHT_GRAY, label = "Center"), WBorderLayout.Constraint.CENTER)
    return buildExample(Messages.get("layoutpanel.example.basic.title"), panel)
}

/** Re-layout in response to visibility toggling, detaching, and content changes. */
private fun buildBorderLayoutDynamicExample(): WComponent {
    val panel = buildBorderLayoutPanel()
    val northTile = buildTile(DEMO_BLUE, label = "North")
    val westTile = buildTile(DEMO_ORANGE, label = "West")
    panel.add(northTile, WBorderLayout.Constraint.NORTH)
    panel.add(westTile, WBorderLayout.Constraint.WEST)
    panel.add(buildTile(WColor.LIGHT_GRAY, label = "Center"), WBorderLayout.Constraint.CENTER)

    val northButton = WButton(Messages.get("layoutpanel.example.dynamic.north"))
    northButton.addActionListener {
        northTile.isVisible = !northTile.isVisible
        panel.revalidate()
    }

    val westButton = WButton(Messages.get("layoutpanel.example.dynamic.west"))
    var westAttached = true
    westButton.addActionListener {
        if (westAttached) {
            panel.remove(westTile)
        } else {
            panel.add(westTile, WBorderLayout.Constraint.WEST)
        }
        westAttached = !westAttached
    }

    val widenButton = WButton(Messages.get("layoutpanel.example.dynamic.widen"))
    var wideWest = false
    widenButton.addActionListener {
        wideWest = !wideWest
        westTile.child = WLabel(if (wideWest) Messages.get("layoutpanel.example.dynamic.wide") else "West")
        westTile.invalidateNaturalSize()
        panel.revalidate()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(northButton)
    buttons.add(westButton)
    buttons.add(widenButton)

    val body = WPanel(spacing = 8.0)
    body.add(panel)
    body.add(buttons)
    return buildExample(Messages.get("layoutpanel.example.dynamic.title"), body)
}

/** Nesting another WLayoutPanel in CENTER (preferredSize is computed recursively in Kotlin). */
private fun buildBorderLayoutNestedExample(): WComponent {
    val innerPanel = WLayoutPanel(WBorderLayout(hgap = 4.0, vgap = 4.0))
    innerPanel.add(buildTile(DEMO_YELLOW, label = "Inner North"), WBorderLayout.Constraint.NORTH)
    innerPanel.add(buildTile(WColor.LIGHT_GRAY, label = "Inner Center"), WBorderLayout.Constraint.CENTER)

    val panel = buildBorderLayoutPanel()
    panel.add(buildTile(DEMO_BLUE, label = "North"), WBorderLayout.Constraint.NORTH)
    panel.add(buildTile(DEMO_ORANGE, label = "West"), WBorderLayout.Constraint.WEST)
    panel.add(innerPanel, WBorderLayout.Constraint.CENTER)
    return buildExample(Messages.get("layoutpanel.example.nested.title"), panel)
}

// endregion

// region Expander

/** The Expander page: lines up demos for trying out WExpander's various features. */
internal fun buildExpanderPage(): WComponent {
    val page = buildPage("Expander", Messages.get("expander.description"))

    page.add(buildExpanderBasicExample())
    page.add(buildExpanderDirectionExample())
    return page
}

/** Basic expand/collapse plus the Expanding / Collapsed events. */
private fun buildExpanderBasicExample(): WComponent {
    val state = WLabel(Messages.get("expander.example.basic.state.collapsed"))

    val content = WPanel(spacing = 8.0)
    content.add(WLabel(Messages.get("expander.example.basic.content")))
    content.add(WButton(Messages.get("expander.example.basic.button")))

    val expander = WExpander(Messages.get("expander.example.basic.header"), content)
    expander.width = 320.0
    expander.addExpandListener { state.text = Messages.get("expander.example.basic.state.expanded") }
    expander.addCollapseListener { state.text = Messages.get("expander.example.basic.state.collapsed") }

    val toggleButton = WButton(Messages.get("expander.example.basic.toggle"))
    toggleButton.addActionListener { expander.isExpanded = !expander.isExpanded }

    val body = WPanel(spacing = 8.0)
    body.add(expander)
    body.add(toggleButton)
    body.add(state)
    return buildExample(Messages.get("expander.example.basic.title"), body)
}

/** The expand direction (ExpandDirection). */
private fun buildExpanderDirectionExample(): WComponent {
    val expander = WExpander(Messages.get("expander.example.direction.header"), WLabel(Messages.get("expander.example.direction.content")))
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
    return buildExample(Messages.get("expander.example.direction.title"), body)
}

// endregion

// region Grid

/** The Grid page: lines up demos for trying out WGrid's various features. */
internal fun buildGridPage(): WComponent {
    val page = buildPage("Grid", Messages.get("grid.description"))

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

    grid.add(buildTile(DEMO_RED, label = "80px"), row = 0, column = 0)
    grid.add(buildTile(DEMO_GREEN, label = "1*"), row = 0, column = 1)
    grid.add(buildTile(DEMO_BLUE, label = "2*"), row = 0, column = 2)
    grid.add(buildTile(DEMO_ORANGE, label = Messages.get("grid.example.cell.row2")), row = 1, column = 0)
    grid.add(buildTile(WColor.PURPLE, label = Messages.get("grid.example.cell.row2")), row = 1, column = 2)

    return buildExample(Messages.get("grid.example.cell.title"), grid)
}

/** RowSpan / ColumnSpan and cell spacing. */
private fun buildGridSpanExample(): WComponent {
    val grid = WGrid(rowSpacing = 4.0, columnSpacing = 4.0)
    grid.width = 320.0
    repeat(2) { grid.addRow(GridLength.pixel(48.0)) }
    repeat(3) { grid.addColumn(GridLength.star()) }

    grid.add(buildTile(DEMO_BLUE, label = Messages.get("grid.example.span.columns")), row = 0, column = 0, columnSpan = 2)
    grid.add(buildTile(DEMO_GREEN, label = Messages.get("grid.example.span.rows")), row = 0, column = 2, rowSpan = 2)
    grid.add(buildTile(DEMO_RED, label = Messages.get("grid.example.span.cell")), row = 1, column = 0)
    grid.add(buildTile(DEMO_ORANGE, label = Messages.get("grid.example.span.cell")), row = 1, column = 1)

    val spacingButton = WButton(Messages.get("grid.example.span.spacing"))
    spacingButton.addActionListener {
        val next = if (grid.rowSpacing > 4.0) 4.0 else 16.0
        grid.rowSpacing = next
        grid.columnSpacing = next
    }

    val body = WPanel(spacing = 8.0)
    body.add(grid)
    body.add(spacingButton)
    return buildExample(Messages.get("grid.example.span.title"), body)
}

// endregion

// region RelativePanel

/** The RelativePanel page: lines up demos for trying out WRelativePanel's various features. */
internal fun buildRelativePanelPage(): WComponent {
    val page = buildPage("RelativePanel", Messages.get("relativepanel.description"))

    page.add(buildRelativePanelSiblingExample())
    page.add(buildRelativePanelAlignExample())
    return page
}

/** Placement relative to sibling elements (RightOf / Below). */
private fun buildRelativePanelSiblingExample(): WComponent {
    val panel = WRelativePanel()
    panel.width = 320.0
    panel.height = 160.0

    val anchor = buildTile(DEMO_BLUE, label = Messages.get("relativepanel.example.sibling.anchor"))
    val right = buildTile(DEMO_GREEN, label = Messages.get("relativepanel.example.sibling.right"))
    val below = buildTile(DEMO_RED, label = Messages.get("relativepanel.example.sibling.below"))
    val rightBelow = buildTile(DEMO_ORANGE, label = Messages.get("relativepanel.example.sibling.rightbelow"))

    panel.add(anchor)
    panel.add(right)
    panel.add(below)
    panel.add(rightBelow)

    panel.placeRightOf(right, anchor)
    panel.placeBelow(below, anchor)
    panel.placeRightOf(rightBelow, below)
    panel.placeBelow(rightBelow, right)

    return buildExample(Messages.get("relativepanel.example.sibling.title"), panel)
}

/** Placement relative to the panel (AlignXxxWithPanel). */
private fun buildRelativePanelAlignExample(): WComponent {
    val panel = WRelativePanel()
    panel.width = 320.0
    panel.height = 160.0

    panel.add(buildTile(WColor.LIGHT_GRAY, 320.0, 160.0)) // background (visualizes the panel bounds)

    val topRight = buildTile(DEMO_GREEN, 48.0, 48.0)
    panel.add(topRight)
    panel.alignRightWithPanel(topRight)
    panel.alignTopWithPanel(topRight)

    val bottomRight = buildTile(DEMO_RED, 48.0, 48.0)
    panel.add(bottomRight)
    panel.alignRightWithPanel(bottomRight)
    panel.alignBottomWithPanel(bottomRight)

    val center = buildTile(WColor.PURPLE, 48.0, 48.0)
    panel.add(center)
    panel.alignHorizontalCenterWithPanel(center)
    panel.alignVerticalCenterWithPanel(center)

    return buildExample(Messages.get("relativepanel.example.align.title"), panel)
}

// endregion

// region SplitView

/** The SplitView page: lines up demos for trying out WSplitView's various features. */
internal fun buildSplitViewPage(): WComponent {
    val page = buildPage("SplitView", Messages.get("splitview.description"))

    page.add(buildSplitViewExample())
    return page
}

/** Opening/closing the pane, display mode, and placement. */
private fun buildSplitViewExample(): WComponent {
    val paneContent = WPanel(spacing = 8.0)
    paneContent.add(WLabel(Messages.get("splitview.example.pane")))
    paneContent.add(WButton(Messages.get("splitview.example.menu1")))
    paneContent.add(WButton(Messages.get("splitview.example.menu2")))

    val pane = WBorder(paneContent)
    pane.background = WColor.LIGHT_GRAY
    pane.padding = 8.0

    val mainContent = WBorder(WLabel(Messages.get("splitview.example.content")))
    mainContent.padding = 16.0

    val splitView = WSplitView(pane = pane, content = mainContent)
    splitView.width = 480.0
    splitView.height = 200.0
    splitView.openPaneLength = 160.0
    splitView.displayMode = SplitViewDisplayMode.INLINE

    val toggleButton = WButton(Messages.get("splitview.example.toggle"))
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

    val placementButton = WButton(Messages.get("splitview.example.placement"))
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
    return buildExample(Messages.get("splitview.example.title"), body)
}

// endregion

// region StackPanel

/** The StackPanel page: lines up demos for trying out WPanel's various features. */
internal fun buildStackPanelPage(): WComponent {
    val page = buildPage("StackPanel", Messages.get("stackpanel.description"))

    page.add(buildStackPanelExample())
    return page
}

/** The direction children line up in, and the spacing between them. */
private fun buildStackPanelExample(): WComponent {
    val panel = WPanel(spacing = 8.0)
    panel.add(buildTile(DEMO_RED, 48.0, 48.0))
    panel.add(buildTile(DEMO_GREEN, 48.0, 48.0))
    panel.add(buildTile(DEMO_BLUE, 48.0, 48.0))

    val orientationButton = WButton(Messages.get("stackpanel.example.orientation"))
    orientationButton.addActionListener {
        panel.orientation = when (panel.orientation) {
            Orientation.VERTICAL -> Orientation.HORIZONTAL
            Orientation.HORIZONTAL -> Orientation.VERTICAL
        }
    }

    val spacingButton = WButton(Messages.get("stackpanel.example.spacing"))
    spacingButton.addActionListener {
        panel.spacing = if (panel.spacing > 8.0) 8.0 else 24.0
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(orientationButton)
    buttons.add(spacingButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(panel)
    return buildExample(Messages.get("stackpanel.example.title"), body)
}

// endregion

// region VariableSizedWrapGrid

/** The VariableSizedWrapGrid page: lines up demos for trying out WVariableSizedWrapGrid's various features. */
internal fun buildVariableSizedWrapGridPage(): WComponent {
    val page = buildPage("VariableSizedWrapGrid", Messages.get("variablesizedwrapgrid.description"))

    page.add(buildWrapGridSpanExample())
    return page
}

/** Cell spans and wrapping. */
private fun buildWrapGridSpanExample(): WComponent {
    val grid = WVariableSizedWrapGrid(itemWidth = 56.0, itemHeight = 56.0)
    grid.orientation = Orientation.HORIZONTAL
    grid.maximumRowsOrColumns = 4

    grid.add(buildTile(DEMO_BLUE, label = "2×2"), rowSpan = 2, columnSpan = 2)
    grid.add(buildTile(DEMO_RED, label = "1×1"), rowSpan = 1, columnSpan = 1)
    grid.add(buildTile(DEMO_GREEN, label = "1×2"), rowSpan = 1, columnSpan = 2)
    grid.add(buildTile(DEMO_ORANGE, label = "1×1"), rowSpan = 1, columnSpan = 1)
    grid.add(buildTile(WColor.PURPLE, label = "2×1"), rowSpan = 2, columnSpan = 1)
    grid.add(buildTile(WColor.GRAY, label = "1×1"), rowSpan = 1, columnSpan = 1)

    val maxButton = WButton(Messages.get("variablesizedwrapgrid.example.maximum"))
    maxButton.addActionListener {
        grid.maximumRowsOrColumns = if (grid.maximumRowsOrColumns == 4) 6 else 4
    }

    val orientationButton = WButton(Messages.get("variablesizedwrapgrid.example.orientation"))
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
    return buildExample(Messages.get("variablesizedwrapgrid.example.title"), body)
}

// endregion

// region SettingsCard

/** The SettingsCard page: lines up demos for trying out WSettingsCard's various features. */
internal fun buildSettingsCardPage(): WComponent {
    val page = buildPage(
        "SettingsCard",
        Messages.get("settingscard.description"),
    )

    page.add(buildSettingsCardBasicExample())
    page.add(buildSettingsCardClickExample())
    page.add(buildSettingsCardWrapExample())
    page.add(buildSettingsCardAlignmentExample())
    page.add(buildSettingsCardStateExample())
    return page
}

/** The basic form: a header, description, and icon on the left, with content on the right. */
private fun buildSettingsCardBasicExample(): WComponent {
    val toggleCard = WSettingsCard(
        Messages.get("settingscard.example.basic.toggle.header"),
        Messages.get("settingscard.example.basic.toggle.description"),
    )
    toggleCard.headerIcon = Symbol.SETTING
    toggleCard.content = WToggleSwitch()

    val comboCard = WSettingsCard(
        Messages.get("settingscard.example.basic.combo.header"),
        Messages.get("settingscard.example.basic.combo.description"),
    )
    comboCard.headerIcon = Symbol.VIEW_ALL
    comboCard.content = WComboBox(
        listOf(
            Messages.get("settingscard.example.basic.combo.light"),
            Messages.get("settingscard.example.basic.combo.dark"),
            Messages.get("settingscard.example.basic.combo.system"),
        ),
    ).also { it.selectedIndex = 2 }

    val plainCard = WSettingsCard(Messages.get("settingscard.example.basic.plain"))

    val body = WPanel(spacing = 4.0)
    body.width = SETTINGS_CARD_DEMO_WIDTH
    body.add(toggleCard)
    body.add(comboCard)
    body.add(plainCard)
    return buildExample(Messages.get("settingscard.example.basic.title"), body)
}

/** A clickable card and its chevron icon (ActionIcon). */
private fun buildSettingsCardClickExample(): WComponent {
    val state = WLabel(Messages.get("settingscard.example.click.count", "0"))
    var count = 0

    val card = WSettingsCard(
        Messages.get("settingscard.example.click.storage.header"),
        Messages.get("settingscard.example.click.storage.description"),
    )
    card.headerIcon = Symbol.SAVE
    card.isClickEnabled = true
    card.addActionListener {
        count++
        state.text = Messages.get("settingscard.example.click.count", count.toString())
    }

    val linkCard = WSettingsCard(
        Messages.get("settingscard.example.click.link.header"),
        Messages.get("settingscard.example.click.link.description"),
    )
    linkCard.headerIcon = Symbol.GLOBE
    linkCard.isClickEnabled = true
    linkCard.actionIcon = Symbol.GO
    linkCard.actionIconToolTip = Messages.get("settingscard.example.click.link.tooltip")

    val actionIconSwitch = WToggleSwitch(Messages.get("settingscard.example.click.chevron"))
    actionIconSwitch.isOn = true
    actionIconSwitch.addItemListener { isOn -> card.isActionIconVisible = isOn }

    val body = WPanel(spacing = 8.0)
    body.width = SETTINGS_CARD_DEMO_WIDTH
    body.add(card)
    body.add(linkCard)
    body.add(state)
    body.add(actionIconSwitch)
    return buildExample(Messages.get("settingscard.example.click.title"), body)
}

/** Automatic wrapping based on width (RightWrapped / RightWrappedNoIcon). */
private fun buildSettingsCardWrapExample(): WComponent {
    val card = WSettingsCard(
        Messages.get("settingscard.example.wrap.header"),
        Messages.get("settingscard.example.wrap.description"),
    )
    card.headerIcon = Symbol.SETTING
    card.content = WComboBox(
        listOf(
            Messages.get("settingscard.example.wrap.option1"),
            Messages.get("settingscard.example.wrap.option2"),
        ),
    ).also { it.selectedIndex = 0 }

    val widthLabel = WLabel(Messages.get("settingscard.example.wrap.width", SETTINGS_CARD_DEMO_WIDTH.toInt().toString()))
    val slider = WSlider(minimum = 200.0, maximum = SETTINGS_CARD_DEMO_WIDTH, value = SETTINGS_CARD_DEMO_WIDTH)
    slider.width = SETTINGS_CARD_DEMO_WIDTH
    slider.addChangeListener { value ->
        card.width = value
        widthLabel.text = Messages.get("settingscard.example.wrap.width", value.toInt().toString())
    }

    val body = WPanel(spacing = 8.0)
    body.width = SETTINGS_CARD_DEMO_WIDTH
    body.add(card)
    body.add(slider)
    body.add(widthLabel)
    return buildExample(Messages.get("settingscard.example.wrap.title"), body)
}

/** ContentAlignment (Right / Left / Vertical). */
private fun buildSettingsCardAlignmentExample(): WComponent {
    val card = WSettingsCard(
        Messages.get("settingscard.example.alignment.header"),
        Messages.get("settingscard.example.alignment.description"),
    )
    card.headerIcon = Symbol.ALIGN_LEFT
    card.content = WCheckBox(Messages.get("settingscard.example.alignment.checkbox"))

    val combo = WComboBox(ContentAlignment.entries.map { it.name })
    combo.selectedIndex = 0
    combo.addListSelectionListener {
        val index = combo.selectedIndex
        if (index >= 0) card.contentAlignment = ContentAlignment.entries[index]
    }

    val options = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    options.add(optionsLabel("ContentAlignment"))
    options.add(combo)

    val body = WPanel(spacing = 8.0)
    body.width = SETTINGS_CARD_DEMO_WIDTH
    body.add(card)
    body.add(options)
    return buildExample(Messages.get("settingscard.example.alignment.title"), body)
}

/** How the disabled state looks. */
private fun buildSettingsCardStateExample(): WComponent {
    val card = WSettingsCard(
        Messages.get("settingscard.example.state.header"),
        Messages.get("settingscard.example.state.description"),
    )
    card.headerIcon = Symbol.IMPORTANT
    card.content = WToggleSwitch()

    val enabledSwitch = WToggleSwitch(Messages.get("settingscard.example.state.switch"))
    enabledSwitch.isOn = true
    enabledSwitch.addItemListener { isOn -> card.isEnabled = isOn }

    val body = WPanel(spacing = 8.0)
    body.width = SETTINGS_CARD_DEMO_WIDTH
    body.add(card)
    body.add(enabledSwitch)
    return buildExample(Messages.get("settingscard.example.state.title"), body)
}

/** The width of the SettingsCard demos (a width that looks like a real settings page list). */
private const val SETTINGS_CARD_DEMO_WIDTH = 560.0

// endregion
