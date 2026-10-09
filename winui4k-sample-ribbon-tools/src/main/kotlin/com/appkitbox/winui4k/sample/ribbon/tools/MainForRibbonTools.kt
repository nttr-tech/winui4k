package com.appkitbox.winui4k.sample.ribbon.tools

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WRibbonContextualToolBar
import com.appkitbox.winui4k.WRibbonMenuBar
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WRibbonToolBar
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonMenuBarItemModel
import com.appkitbox.winui4k.ribbon.RibbonMenuBarModel
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonSliderModel
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonToolBarModel
import com.appkitbox.winui4k.ribbon.RibbonToolBarOrientation
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/** A tool (an item in the tool palette and the contents of the options bar). */
private enum class Tool(val label: String, val icon: RibbonIcon) {
    MOVE("Move", RibbonIcons.MOVE),
    SELECT("Select", RibbonIcons.SELECT_ALL),
    BRUSH("Brush", RibbonIcons.PEN),
    PEN("Pen", RibbonIcons.HIGHLIGHTER),
    ERASER("Eraser", RibbonIcons.ERASER),
    TEXT("Text", RibbonIcons.TEXT),
    SHAPE("Shape", RibbonIcons.RECTANGLE),
    CROP("Crop", RibbonIcons.CROP),
    ZOOM("Zoom", RibbonIcons.ZOOM),
    HAND("Hand", RibbonIcons.POINTER),
}

/**
 * Tools demo (same layout as tools.png in the RibbonSpace README): a menu bar, an options bar that switches with the
 * active tool, a two-column tool palette, a panel rail on the right, a command bar floating above the canvas, and a
 * status bar. Built only from toolbars made with ribbon item models, without a ribbon.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.WORD)
        val window = RibbonDemoWindow("Tools - WinUI4K Ribbon", RibbonDemoApp.TOOLS, null)
        val status = RibbonLabelModel("status.tool", "Brush Tool")
        val options = WRibbonContextualToolBar()
        Tool.entries.forEach { options.setContent(it.name, optionBar(it)) }
        options.activeContext = Tool.BRUSH.name
        val palette = toolPalette { tool ->
            options.activeContext = tool.name
            status.label = "${tool.label} Tool"
        }
        val top = WGrid()
        top.addRow(GridLength.AUTO)
        top.addRow(GridLength.AUTO)
        top.add(WRibbonMenuBar(menuBarModel()), row = 0, column = 0)
        top.add(options, row = 1, column = 0)
        window.setTop(top)
        val body = WGrid()
        body.addColumn(GridLength.AUTO)
        body.addColumn(GridLength.star())
        body.addColumn(GridLength.AUTO)
        body.add(palette, row = 0, column = 0)
        body.add(canvas(), row = 0, column = 1)
        body.add(panelRail(), row = 0, column = 2)
        window.setContent(body)
        window.setStatusBar(statusBar(status))
        window.show()
    }
}

/** A classic menu bar. */
private fun menuBarModel(): RibbonMenuBarModel = RibbonMenuBarModel().also { model ->
    fun menu(id: String, label: String, vararg items: com.appkitbox.winui4k.ribbon.RibbonNodeModel) =
        RibbonMenuBarItemModel(id, label).also { m -> items.forEach { m.items.add(it) } }
    model.items.add(menu("file", "File", menuItem("file.new", "New...", RibbonIcons.NEW).also { it.shortcut = "Ctrl+N" }, menuItem("file.open", "Open...", RibbonIcons.OPEN).also { it.shortcut = "Ctrl+O" }, RibbonMenuSeparatorModel(), menuItem("file.export", "Export", RibbonIcons.EXPORT)))
    model.items.add(menu("edit", "Edit", menuItem("edit.undo", "Undo", RibbonIcons.UNDO).also { it.shortcut = "Ctrl+Z" }, menuItem("edit.redo", "Redo", RibbonIcons.REDO).also { it.shortcut = "Ctrl+Y" }))
    model.items.add(menu("image", "Image", menuItem("image.size", "Image Size..."), menuItem("image.canvas", "Canvas Size...")))
    model.items.add(menu("layer", "Layer", menuItem("layer.new", "New Layer", RibbonIcons.LAYERS), menuItem("layer.duplicate", "Duplicate Layer")))
    model.items.add(menu("view", "View", menuItem("view.zoomIn", "Zoom In", RibbonIcons.ZOOM_IN), menuItem("view.zoomOut", "Zoom Out", RibbonIcons.ZOOM_OUT)))
    model.items.add(menu("window", "Window", menuItem("window.layers", "Layers"), menuItem("window.history", "History")))
    model.items.add(menu("help", "Help", menuItem("help.about", "About")))
}

/** The options bar of a tool (its contents differ per tool). */
private fun optionBar(tool: Tool): WComponent {
    val model = RibbonToolBarModel("options.${tool.name}")
    model.items.add(button("options.tool", tool.label, tool.icon).also { it.showLabelInSimplified = true })
    model.items.add(RibbonSeparatorModel())
    val items: List<RibbonItemModel> = when (tool) {
        Tool.BRUSH, Tool.PEN, Tool.ERASER -> listOf(
            RibbonSpinnerModel("brush.size", "Size:", BRUSH_SIZE).also {
                it.unit = "px"
                it.maximum = MAX_SIZE
            },
            RibbonSpinnerModel("brush.hardness", "Hardness:", HARDNESS).also { it.unit = "%" },
            RibbonComboBoxModel("brush.mode", "Mode", listOf("Normal", "Multiply", "Screen", "Overlay")).also {
                it.selectedItem = "Normal"
                it.showLabel = true
            },
            RibbonSliderModel("brush.opacity", "Opacity").also { it.value = OPACITY },
            button("brush.flow", "Flow", RibbonIcons.LIGHT, RibbonItemSize.SMALL),
            button("brush.pressure", "Pressure", RibbonIcons.PEN, RibbonItemSize.SMALL),
            RibbonColorPickerModel("brush.color", "Foreground Color", RibbonIcons.PALETTE),
        )
        Tool.TEXT -> listOf(
            RibbonComboBoxModel("text.font", "Font", listOf("Segoe UI", "Yu Gothic UI", "Meiryo UI")).also {
                it.selectedItem = "Segoe UI"
                it.showLabel = true
            },
            RibbonSpinnerModel("text.size", "Size:", TEXT_SIZE).also { it.unit = "pt" },
            RibbonColorPickerModel("text.color", "Text Color", RibbonIcons.FONT_COLOR),
        )
        Tool.SHAPE -> listOf(
            RibbonColorPickerModel("shape.fill", "Fill", RibbonIcons.FILL),
            RibbonSpinnerModel("shape.stroke", "Stroke:", 2.0).also { it.unit = "px" },
        )
        else -> listOf(RibbonLabelModel("options.none", "This tool has no options"))
    }
    items.forEach { model.items.add(it) }
    return WRibbonToolBar(model)
}

/** A two-column tool palette (radio toggles). */
private fun toolPalette(onTool: (Tool) -> Unit): WComponent {
    val model = RibbonToolBarModel("palette")
    model.orientation = RibbonToolBarOrientation.VERTICAL
    model.columns = 2
    model.showLabels = false
    for (tool in Tool.entries) {
        if (tool == Tool.SHAPE || tool == Tool.ZOOM) model.items.add(RibbonSeparatorModel())
        model.items.add(
            toggle("tool.${tool.name}", tool.label, tool.icon, RibbonItemSize.SMALL).also { item ->
                item.groupName = "tool"
                item.isChecked = tool == Tool.BRUSH
                item.addActionListener { onTool(tool) }
            },
        )
    }
    model.items.add(RibbonSeparatorModel())
    model.items.add(RibbonColorPickerModel("palette.foreground", "Foreground Color", RibbonIcons.PALETTE))
    model.items.add(RibbonColorPickerModel("palette.background", "Background Color", RibbonIcons.FILL))
    val bar = WRibbonToolBar(model)
    bar.margin = 6.0
    return WRibbonTheme.surface(bar, "RibbonCommandBarBackgroundBrush")
}

/** The canvas (a white image in a dark work area) and the command bar floating above it. */
private fun canvas(): WComponent {
    val page = WBorder(
        WLabel("Choose a tool and the options bar follows the active tool").also {
            it.horizontalAlignment = HorizontalAlignment.CENTER
            it.verticalAlignment = VerticalAlignment.CENTER
        },
    )
    page.width = PAGE_WIDTH
    page.height = PAGE_HEIGHT
    page.background = WColor(255, 255, 255)
    page.horizontalAlignment = HorizontalAlignment.CENTER
    page.verticalAlignment = VerticalAlignment.CENTER
    val floatingModel = RibbonToolBarModel("floating")
    floatingModel.showLabels = false
    listOf(
        button("float.undo", "Undo", RibbonIcons.UNDO, RibbonItemSize.SMALL),
        button("float.redo", "Redo", RibbonIcons.REDO, RibbonItemSize.SMALL),
        RibbonSeparatorModel(),
        button("float.zoomIn", "Zoom In", RibbonIcons.ZOOM_IN, RibbonItemSize.SMALL),
        button("float.zoomOut", "Zoom Out", RibbonIcons.ZOOM_OUT, RibbonItemSize.SMALL),
        button("float.fit", "Fit on Screen", RibbonIcons.FULL_SCREEN, RibbonItemSize.SMALL),
        RibbonSeparatorModel(),
        toggle("float.grid", "Grid", RibbonIcons.GRID),
        toggle("float.snap", "Snap", RibbonIcons.MOVE).also { it.isChecked = true },
        toggle("float.rulers", "Rulers", RibbonIcons.RULER),
        button("float.more1", "Transform", RibbonIcons.ROTATE, RibbonItemSize.SMALL),
        button("float.more2", "Flip", RibbonIcons.SPLIT, RibbonItemSize.SMALL),
    ).forEach { floatingModel.items.add(it) }
    val floating = WRibbonToolBar(floatingModel)
    floating.width = FLOATING_WIDTH
    val floatingBorder = WBorder(floating)
    floatingBorder.background = WColor(255, 255, 255)
    floatingBorder.borderColor = WColor(0xD0, 0xD0, 0xD0)
    floatingBorder.borderThickness = 1.0
    floatingBorder.cornerRadius = 8.0
    floatingBorder.padding = 4.0
    floatingBorder.horizontalAlignment = HorizontalAlignment.CENTER
    floatingBorder.verticalAlignment = VerticalAlignment.BOTTOM
    floatingBorder.margin = 18.0
    val grid = WGrid()
    grid.add(page, row = 0, column = 0)
    grid.add(floatingBorder, row = 0, column = 0)
    val area = WBorder(grid)
    area.background = WColor(0x3A, 0x3A, 0x3A)
    area.cornerRadius = 6.0
    area.margin = 8.0
    return area
}

/** The panel rail on the right (Layers, Properties, History, and Libraries). */
private fun panelRail(): WComponent {
    val model = RibbonToolBarModel("panels")
    model.orientation = RibbonToolBarOrientation.VERTICAL
    model.showLabels = false
    model.items.add(
        toggle("panel.layers", "Layers", RibbonIcons.LAYERS).also {
            it.groupName = "panel"
            it.isChecked = true
        },
    )
    model.items.add(toggle("panel.properties", "Properties", RibbonIcons.SETTINGS).also { it.groupName = "panel" })
    model.items.add(toggle("panel.history", "History", RibbonIcons.HISTORY).also { it.groupName = "panel" })
    model.items.add(toggle("panel.library", "Libraries", RibbonIcons.LIBRARY).also { it.groupName = "panel" })
    val bar = WRibbonToolBar(model)
    bar.margin = 6.0
    return WRibbonTheme.surface(bar, "RibbonCommandBarBackgroundBrush")
}

/** The status bar (active tool and image size, and zoom). */
private fun statusBar(status: RibbonLabelModel): WComponent {
    val model = RibbonStatusBarModel()
    model.items.add(status)
    model.items.add(RibbonLabelModel("status.size", "1920 × 1080 px · RGB/8"))
    model.endItems.add(RibbonZoomModel("status.zoom", ZOOM))
    return WRibbonStatusBar(model)
}

private const val BRUSH_SIZE = 24.0
private const val MAX_SIZE = 500.0
private const val HARDNESS = 80.0
private const val OPACITY = 90.0
private const val TEXT_SIZE = 18.0
private const val PAGE_WIDTH = 720.0
private const val PAGE_HEIGHT = 420.0
private const val FLOATING_WIDTH = 380.0
private const val ZOOM = 66.0
