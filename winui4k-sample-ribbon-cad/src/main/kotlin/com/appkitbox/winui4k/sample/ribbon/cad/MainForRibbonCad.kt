package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCanvas
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonApplicationMenu
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WRibbonToolBar
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonApplicationMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonApplicationMenuModel
import com.appkitbox.winui4k.ribbon.RibbonChromeStyle
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonRecentItemModel
import com.appkitbox.winui4k.ribbon.RibbonReductionStrategy
import com.appkitbox.winui4k.ribbon.RibbonSegmentModel
import com.appkitbox.winui4k.ribbon.RibbonSegmentedModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle
import com.appkitbox.winui4k.ribbon.RibbonToolBarModel
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/**
 * An AutoCAD-style ribbon demo (same layout as cad.png in the RibbonSpace README): the CAD theme (dark), the
 * application menu opened from the app icon, a QAT with a workspace combo box, cycling through the expanded panel,
 * floating panel and minimized states, document tabs, a drawing, a command line, and an icon-only status bar.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.apply(RibbonThemePalette.CAD, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.CAD)
        val model = createCadModel()
        model.quickAccessItems.add(button("qat.new", "New", RibbonIcons.NEW_DOCUMENT))
        model.quickAccessItems.add(button("qat.open", "Open", RibbonIcons.OPEN))
        model.quickAccessItems.add(button("qat.save", "Save", RibbonIcons.SAVE))
        model.quickAccessItems.add(button("qat.undo", "Undo", RibbonIcons.UNDO))
        model.quickAccessItems.add(button("qat.redo", "Redo", RibbonIcons.REDO))
        model.quickAccessItems.add(button("qat.plot", "Plot", RibbonIcons.PRINT))
        model.quickAccessItems.add(
            RibbonComboBoxModel("qat.workspace", "Workspace", listOf("Drafting & Annotation", "3D Basics", "3D Modeling")).also {
                it.selectedItem = "Drafting & Annotation"
                it.inputWidth = WORKSPACE_WIDTH
            },
        )
        val ribbon = WRibbon(model)
        ribbon.isMinimizeButtonVisible = true
        ribbon.canFloatGroups = true
        ribbon.isVisibilityMenuEnabled = true
        ribbon.reductionStrategy = RibbonReductionStrategy.GROUP_BY_GROUP
        ribbon.applicationMenu = applicationMenu(ribbon).createFlyout()
        val commandLine = CommandLine()
        ribbon.addItemInvokedListener { event -> commandLine.echo("_" + (event.item.id ?: event.item.label.orEmpty()).uppercase()) }
        val window = RibbonDemoWindow("Drawing1.dwg - CAD (WinUI4K Ribbon)", RibbonDemoApp.CAD, ribbon)
        val titleBar = window.titleBar!!
        titleBar.isAppIconMenuEnabled = true
        titleBar.subtitle = "WinUI4K CAD"
        titleBar.endContent = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL).also {
            it.add(WButton("Sign In"))
            it.add(WButton("Hatch").also { b -> b.addActionListener { model.setContextualGroupVisible("hatchEditor", !model.findContextualGroup("hatchEditor")!!.isVisible) } })
        }
        window.setTop(ribbon)
        window.setContent(drawingArea(commandLine))
        window.setStatusBar(statusBar(ribbon))
        window.setDark(true)
        window.show()
    }
}

/** The AutoCAD menu browser (search, commands and subcommands, recent documents, options and exit). */
private fun applicationMenu(ribbon: WRibbon): WRibbonApplicationMenu {
    val model = RibbonApplicationMenuModel()
    fun item(id: String, label: String, icon: com.appkitbox.winui4k.ribbon.RibbonIcon, vararg subs: Pair<String, String>) =
        RibbonApplicationMenuItemModel(id, label, icon).also { item ->
            subs.forEach { (subLabel, description) ->
                item.items.add(RibbonApplicationMenuItemModel("$id.$subLabel", subLabel, icon).also { it.description = description })
            }
        }
    model.items.add(item("new", "New", RibbonIcons.NEW_DOCUMENT, "Drawing" to "Creates a new drawing from a template.", "Sheet Set" to "Creates a new sheet set."))
    model.items.add(item("open", "Open", RibbonIcons.OPEN, "Drawing" to "Opens an existing drawing file.", "DGN" to "Imports a MicroStation DGN file."))
    model.items.add(item("save", "Save", RibbonIcons.SAVE))
    model.items.add(
        item(
            "saveAs",
            "Save As",
            RibbonIcons.SAVE_AS,
            "Drawing" to "Saves the current drawing as a DWG file.",
            "Drawing Template" to "Creates a drawing template (DWT) file from which new drawings can be created.",
            "Drawing Standards" to "Creates a drawing standards (DWS) file for checking drawings.",
            "Other Formats" to "Saves in DWG, DWT, DWS, or DXF format.",
        ),
    )
    model.items.add(item("import", "Import", RibbonIcons.IMPORT, "PDF" to "Imports geometry from a PDF.").also { it.hasSeparatorBefore = true })
    model.items.add(item("export", "Export", RibbonIcons.EXPORT, "PDF" to "Exports to PDF."))
    model.items.add(item("publish", "Publish", RibbonIcons.SHARE, "Batch Plot" to "Plots multiple sheets."))
    model.items.add(item("print", "Print", RibbonIcons.PRINT, "Plot" to "Plots the drawing."))
    model.items.add(item("utilities", "Drawing Utilities", RibbonIcons.MEASURE, "Purge" to "Removes unused named items.").also { it.hasSeparatorBefore = true })
    model.items.add(item("close", "Close", RibbonIcons.CLOSE, "Current Drawing" to "Closes the current drawing."))
    model.recentItems.add(RibbonRecentItemModel("r1", "Floor Plan.dwg", "C:\\Projects\\House").also { it.isPinned = true })
    model.recentItems.add(RibbonRecentItemModel("r2", "Site Layout.dwg", "C:\\Projects\\House"))
    model.recentItems.add(RibbonRecentItemModel("r3", "Details.dwg", "C:\\Projects\\Office"))
    model.footerItems.add(button("appmenu.options", "Options", RibbonIcons.SETTINGS))
    model.footerItems.add(button("appmenu.exit", "Exit WinUI4K CAD", RibbonIcons.CLOSE))
    return WRibbonApplicationMenu(model, ribbon)
}

/** The command line (history and input). */
private class CommandLine {
    private val history = WLabel("Command: _.OPEN \"Drawing1.dwg\"\nRegenerating model.\nCommand:")
    private val input = WTextField("Type a command")
    val element: WComponent

    init {
        history.foreground = WColor(0xDD, 0xDD, 0xDD)
        val panel = WPanel(spacing = 6.0)
        panel.add(history)
        panel.add(input)
        val border = WBorder(panel)
        border.background = WColor(0x2B, 0x31, 0x3B, 0xE6)
        border.padding = 10.0
        border.cornerRadius = 4.0
        border.margin = COMMAND_MARGIN
        border.verticalAlignment = VerticalAlignment.BOTTOM
        border.horizontalAlignment = HorizontalAlignment.STRETCH
        element = border
    }

    fun echo(command: String) {
        val lines = (history.text.split('\n') + "Command: $command").takeLast(HISTORY_LINES)
        history.text = lines.joinToString("\n")
    }
}

/** The document tabs, the drawing (floor plan) and the command line. */
private fun drawingArea(commandLine: CommandLine): WComponent {
    val tabsModel = RibbonToolBarModel("documents")
    for ((id, label) in listOf("start" to "Start", "drawing1" to "Drawing1*", "floorPlan" to "Floor Plan")) {
        tabsModel.items.add(
            toggle(id, label, RibbonIcons.DOCUMENT, RibbonItemSize.MEDIUM).also {
                it.groupName = "documents"
                it.showLabelInSimplified = true
                it.isChecked = id == "drawing1"
            },
        )
    }
    tabsModel.items.add(button("newDrawing", "New Drawing", RibbonIcons.ADD, RibbonItemSize.SMALL))
    val documentTabs = WRibbonToolBar(tabsModel)

    val canvas = WCanvas()
    val plan = FloorPlan(canvas)
    plan.draw()
    val area = WGrid()
    area.addRow(GridLength.AUTO)
    area.addRow(GridLength.star())
    area.add(documentTabs, row = 0, column = 0)
    val drawing = WGrid()
    drawing.add(WBorder(canvas).also { it.background = WColor(0x21, 0x26, 0x30) }, row = 0, column = 0)
    drawing.add(
        WLabel("[-][Top][2D Wireframe]").also {
            it.foreground = WColor(0xDD, 0xDD, 0xDD)
            it.margin = 8.0
        },
        row = 0,
        column = 0,
    )
    drawing.add(commandLine.element, row = 0, column = 0)
    area.add(drawing, row = 1, column = 0)
    return area
}

/** Draws the floor plan (walls, rooms, furniture, dimensions) with Border lines. */
private class FloorPlan(private val canvas: WCanvas) {
    @Suppress("LongParameterList") // The parameters map 1:1 to the position, size, color and thickness of the rectangle drawn with lines
    private fun rect(x: Double, y: Double, w: Double, h: Double, color: WColor, thickness: Double = 1.0) {
        val border = WBorder()
        border.width = w
        border.height = h
        border.borderColor = color
        border.borderThickness = thickness
        canvas.add(border, x, y)
    }

    private fun text(x: Double, y: Double, value: String, color: WColor = WHITE, size: Double = 12.0) {
        canvas.add(
            WLabel(value).also {
                it.foreground = color
                it.fontSize = size
            },
            x,
            y,
        )
    }

    fun draw() {
        rect(OX, OY, 640.0, 420.0, WALL, 8.0)
        rect(OX + 260, OY, 8.0, 170.0, WALL, 4.0)
        rect(OX + 400, OY, 8.0, 170.0, WALL, 4.0)
        rect(OX, OY + 166, 640.0, 8.0, WALL, 4.0)
        rect(OX + 340, OY + 166, 8.0, 254.0, WALL, 4.0)
        text(OX + 100, OY + 85, "Bedroom")
        text(OX + 100, OY + 103, "14.9 m²", WHITE, 9.0)
        text(OX + 315, OY + 70, "Bath", WHITE, 10.0)
        text(OX + 490, OY + 105, "Bedroom 2", WHITE, 11.0)
        text(OX + 130, OY + 255, "Living")
        text(OX + 130, OY + 273, "30.1 m²", WHITE, 9.0)
        text(OX + 440, OY + 225, "Kitchen / Dining")
        rect(OX + 30, OY + 30, 120.0, 70.0, FURNITURE)
        rect(OX + 430, OY + 30, 100.0, 90.0, FURNITURE)
        rect(OX + 60, OY + 320, 140.0, 50.0, FURNITURE)
        rect(OX + 220, OY + 280, 50.0, 50.0, FURNITURE)
        val table = WBorder()
        table.width = 60.0
        table.height = 60.0
        table.cornerRadius = 30.0
        table.borderColor = SELECTED
        table.borderThickness = 2.0
        canvas.add(table, OX + 450, OY + 280)
        rect(OX + 590, OY + 200, 30.0, 150.0, FURNITURE)
        rect(OX, OY + 460, 640.0, 1.0, DIMENSION)
        text(OX + 300, OY + 440, "12000", DIMENSION, 10.0)
        rect(OX - 50, OY, 1.0, 420.0, DIMENSION)
        text(OX - 80, OY + 200, "8000", DIMENSION, 10.0)
        text(OX + 250, OY + 500, "Floor Plan  1:100", WHITE, 14.0)
    }

    private companion object {
        const val OX = 260.0
        const val OY = 60.0
        val WALL = WColor(0xC8, 0xCC, 0xD2)
        val FURNITURE = WColor(0x9C, 0xCC, 0x65)
        val DIMENSION = WColor(0xE5, 0x53, 0x4B)
        val SELECTED = WColor(0x4F, 0xC3, 0xD9)
        val WHITE = WColor(0xEE, 0xEE, 0xEE)
    }
}

/** The CAD status bar (model / layout switching, coordinates, drafting aid toggles; icons only). */
private fun statusBar(ribbon: WRibbon): WComponent {
    val model = RibbonStatusBarModel()
    model.showLabels = false
    model.items.add(
        RibbonSegmentedModel("space", "Space").also { segmented ->
            listOf("Model", "Layout1", "Layout2").forEach { segmented.segments.add(RibbonSegmentModel(it, it)) }
            segmented.selectedSegment = segmented.segments[0]
        },
    )
    model.endItems.add(RibbonLabelModel("coords", "6215.7290, 3820.4106, 0.0000").also { it.showLabel = true })
    model.endItems.add(toggle("status.grid", "Grid Display", RibbonIcons.GRID).also { it.isChecked = true })
    model.endItems.add(toggle("status.snap", "Snap", RibbonIcons.MAP))
    model.endItems.add(toggle("status.ortho", "Ortho Mode", RibbonIcons.ALIGN).also { it.isChecked = true })
    model.endItems.add(toggle("status.polar", "Polar Tracking", RibbonIcons.ROTATE).also { it.isChecked = true })
    model.endItems.add(toggle("status.osnap", "Object Snap", RibbonIcons.MEASURE).also { it.isChecked = true })
    model.endItems.add(button("status.scale", "Annotation Scale 1:1", RibbonIcons.ZOOM))
    model.endItems.add(button("status.customize", "Customization", RibbonIcons.MENU))
    val bar = WRibbonStatusBar(model)
    bar.ribbon = ribbon
    return bar
}

private const val WORKSPACE_WIDTH = 170.0
private const val HISTORY_LINES = 4
private const val COMMAND_MARGIN = 24.0
