package com.appkitbox.winui4k.sample.ribbon.tools

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.extension.ribbon.WRibbonContextualToolBar
import com.appkitbox.winui4k.extension.ribbon.WRibbonMenuBar
import com.appkitbox.winui4k.extension.ribbon.WRibbonStatusBar
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme
import com.appkitbox.winui4k.extension.ribbon.WRibbonToolBar
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonLabelModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuBarItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSegmentModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSegmentedModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSliderModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStatusBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToolBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToolBarOrientation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.menuItem
import com.appkitbox.winui4k.sample.ribbon.common.toggle

/** The kinds of options bar contents (RibbonContextualToolBar.Context in the RibbonSpace demo). */
private enum class ToolContext { MOVE, BRUSH, TEXT }

/** The radio tools of the tool palette, and the options bar each tool shows. */
private enum class Tool(val label: String, val icon: RibbonIcon, val context: ToolContext) {
    MOVE("Move (V)", RibbonIcon.glyph(""), ToolContext.MOVE),
    SELECT("Select (M)", RibbonIcon.glyph(""), ToolContext.MOVE),
    BRUSH("Brush (B)", RibbonIcon.glyph(""), ToolContext.BRUSH),
    PEN("Pen (P)", RibbonIcon.glyph(""), ToolContext.BRUSH),
    ERASER("Eraser (E)", RibbonIcon.glyph(""), ToolContext.BRUSH),
    TYPE("Text (T)", RibbonIcon.glyph(""), ToolContext.TEXT),
}

/**
 * Tools demo (same layout as ToolsPage.xaml in the RibbonSpace demo): a menu bar, an options bar that switches with the
 * active tool, a two-column tool palette (with a split button that follows the last chosen shape), a panel rail on the
 * right, a command bar floating above the canvas, and a status bar. Built only from toolbars made with ribbon item
 * models, without a ribbon.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.WORD)
        val window = RibbonDemoWindow("Tools - WinUI4K Ribbon", RibbonDemoApp.TOOLS, null)
        val status = RibbonLabelModel("status.tool", Tool.BRUSH.label)
        val canvasText = WLabel("Choose a tool and the options bar follows the active tool")
        val options = WRibbonContextualToolBar()
        options.setContent(ToolContext.MOVE.name, moveOptions())
        options.setContent(ToolContext.BRUSH.name, brushOptions())
        options.setContent(ToolContext.TEXT.name, textOptions())
        options.activeContext = ToolContext.BRUSH.name
        options.margin = OPTIONS_MARGIN
        val palette = toolPalette { tool ->
            options.activeContext = tool.context.name
            status.label = tool.label
            canvasText.text = "Using ${tool.label}"
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
        body.add(canvas(canvasText), row = 0, column = 1)
        body.add(panelRail(), row = 0, column = 2)
        window.setContent(body)
        window.setStatusBar(statusBar(status))
        window.show()
    }
}

/** A classic menu bar. */
private fun menuBarModel(): RibbonMenuBarModel = RibbonMenuBarModel().also { model ->
    fun menu(id: String, label: String, vararg items: RibbonNodeModel) = RibbonMenuBarItemModel(id, label).also { m -> items.forEach { m.items.add(it) } }
    fun item(id: String, label: String, shortcut: String? = null) = menuItem(id, label).also { it.shortcut = shortcut }
    fun check(id: String, label: String, checked: Boolean, shortcut: String? = null) = item(id, label, shortcut).also {
        it.isCheckable = true
        it.isChecked = checked
    }
    val recent = item("file.recent", "Open Recent").also {
        it.items.add(item("file.recent.poster", "poster.psd"))
        it.items.add(item("file.recent.logo", "logo.svg"))
    }
    model.items.add(
        menu(
            "file",
            "File",
            item("file.new", "New...", "Ctrl+N"),
            item("file.open", "Open...", "Ctrl+O"),
            recent,
            RibbonMenuSeparatorModel(),
            item("file.save", "Save", "Ctrl+S"),
            item("file.export", "Export As...", "Alt+Shift+Ctrl+W"),
        ),
    )
    model.items.add(
        menu(
            "edit",
            "Edit",
            item("edit.undo", "Undo", "Ctrl+Z"),
            item("edit.redo", "Redo", "Shift+Ctrl+Z"),
            RibbonMenuSeparatorModel(),
            item("edit.cut", "Cut", "Ctrl+X"),
            item("edit.copy", "Copy", "Ctrl+C"),
            item("edit.paste", "Paste", "Ctrl+V"),
        ),
    )
    model.items.add(menu("image", "Image", item("image.size", "Image Size..."), item("image.canvas", "Canvas Size..."), check("image.aspect", "Pixel Aspect Ratio Correction", true)))
    model.items.add(menu("layer", "Layer", item("layer.new", "New Layer", "Shift+Ctrl+N"), item("layer.duplicate", "Duplicate Layer")))
    model.items.add(menu("view", "View", check("view.rulers", "Rulers", true, "Ctrl+R"), check("view.grid", "Grid", false), check("view.snap", "Snap", true)))
    model.items.add(menu("window", "Window", check("window.layers", "Layers", true), check("window.properties", "Properties", true)))
    model.items.add(menu("help", "Help", item("help.shortcuts", "Keyboard Shortcuts"), item("help.about", "About")))
}

/** An options bar (starts with the tool's label and a separator). */
private fun optionBar(id: String, label: String, icon: RibbonIcon, vararg items: RibbonItemModel): WComponent {
    val model = RibbonToolBarModel(id)
    model.showLabels = true
    model.items.add(RibbonLabelModel("$id.label", label).also { it.icon = icon })
    model.items.add(RibbonSeparatorModel())
    items.forEach { model.items.add(it) }
    return WRibbonToolBar(model)
}

/** The options for [Move] and [Select]. */
private fun moveOptions(): WComponent = optionBar(
    "move",
    "Move",
    Tool.MOVE.icon,
    RibbonCheckBoxModel("move.autoSelect", "Auto-Select", isChecked = true),
    RibbonCheckBoxModel("move.transform", "Show Transform Controls"),
    RibbonSeparatorModel(),
    button("move.alignLeft", "Align Left", RibbonIcons.ALIGN_LEFT, RibbonItemSize.SMALL),
    button("move.alignCenter", "Align Center", RibbonIcons.ALIGN_CENTER, RibbonItemSize.SMALL),
    button("move.alignRight", "Align Right", RibbonIcons.ALIGN_RIGHT, RibbonItemSize.SMALL),
)

/** The options for [Brush], [Pen], and [Eraser]. */
private fun brushOptions(): WComponent = optionBar(
    "brush",
    "Brush",
    Tool.BRUSH.icon,
    RibbonSpinnerModel("brush.size", "Size:", BRUSH_SIZE).also {
        it.minimum = 1.0
        it.maximum = MAX_SIZE
        it.unit = "px"
        it.inputWidth = SIZE_WIDTH
    },
    RibbonSpinnerModel("brush.hardness", "Hardness:", HARDNESS).also {
        it.maximum = PERCENT
        it.unit = "%"
        it.inputWidth = HARDNESS_WIDTH
    },
    RibbonComboBoxModel("brush.mode", "Mode", listOf("Normal", "Multiply", "Screen", "Overlay")).also {
        it.selectedItem = "Normal"
        it.text = "Normal"
        it.showLabel = true
        it.inputWidth = MODE_WIDTH
    },
    RibbonSliderModel("brush.opacity", "Opacity").also {
        it.value = PERCENT
        it.maximum = PERCENT
        it.sliderWidth = OPACITY_WIDTH
    },
    toggle("brush.airbrush", "Airbrush", RibbonIcon.glyph("")),
    toggle("brush.pressure", "Use Pressure for Size", RibbonIcon.glyph("")),
    RibbonColorPickerModel("brush.color", "Brush Color", RibbonIcon.glyph("")).also { it.selectedColor = RibbonColor.parse("#1473E6") },
)

/** The options for [Text] (alignment is a segmented switch). */
private fun textOptions(): WComponent = optionBar(
    "text",
    "Text",
    Tool.TYPE.icon,
    RibbonFontComboBoxModel("text.font").also { it.selectedItem = "Segoe UI" },
    RibbonFontSizeComboBoxModel("text.size").also { it.selectedItem = TEXT_SIZE },
    RibbonButtonGroupModel(
        toggle("text.bold", "Bold", RibbonIcons.BOLD),
        toggle("text.italic", "Italic", RibbonIcons.ITALIC),
    ),
    RibbonSegmentedModel("text.alignment", "Alignment").also { segmented ->
        segmented.segments.add(RibbonSegmentModel("left", "Left", RibbonIcons.ALIGN_LEFT))
        segmented.segments.add(RibbonSegmentModel("center", "Center", RibbonIcons.ALIGN_CENTER))
        segmented.segments.add(RibbonSegmentModel("right", "Right", RibbonIcons.ALIGN_RIGHT))
        segmented.selectedSegment = segmented.segments[0]
    },
)

/** A two-column tool palette (radio tools, and a split button that follows the last chosen shape). */
private fun toolPalette(onTool: (Tool) -> Unit): WComponent {
    val model = RibbonToolBarModel("palette")
    model.orientation = RibbonToolBarOrientation.VERTICAL
    model.columns = 2
    model.showLabels = false
    for (tool in Tool.entries) {
        model.items.add(
            toggle("tool.${tool.name}", tool.label, tool.icon).also { item ->
                item.groupName = "tool"
                item.isChecked = tool == Tool.BRUSH
                item.addActionListener { if (item.isChecked) onTool(tool) }
            },
        )
    }
    model.items.add(RibbonSeparatorModel())
    model.items.add(
        RibbonSplitButtonModel("tool.shape", "Rectangle").also { shape ->
            shape.icon = RibbonIcon.glyph("\uE739")
            shape.size = RibbonItemSize.SMALL
            shape.followLastChoice = true
            shape.menuItems.add(RibbonMenuItemModel("shape.rectangle", "Rectangle", RibbonIcon.glyph("")))
            shape.menuItems.add(RibbonMenuItemModel("shape.ellipse", "Ellipse", RibbonIcon.glyph("")))
            shape.menuItems.add(RibbonMenuItemModel("shape.polygon", "Polygon", RibbonIcon.glyph("")))
            shape.menuItems.add(RibbonMenuItemModel("shape.line", "Line", RibbonIcon.glyph("")))
        },
    )
    model.items.add(button("tool.crop", "Crop", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
    model.items.add(button("tool.zoom", "Zoom", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
    model.items.add(button("tool.hand", "Hand", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
    model.items.add(RibbonSeparatorModel())
    model.items.add(RibbonColorPickerModel("palette.foreground", "Foreground Color", RibbonIcon.glyph("")).also { it.selectedColor = RibbonColor.parse("#1473E6") })
    model.items.add(RibbonColorPickerModel("palette.background", "Background Color", RibbonIcon.glyph("")).also { it.selectedColor = RibbonColor.WHITE })
    val bar = WRibbonToolBar(model)
    bar.margin = 6.0
    bar.verticalAlignment = VerticalAlignment.TOP
    return WRibbonTheme.surface(bar, "RibbonCommandBarBackgroundBrush")
}

/** The canvas (a white image in a dark work area) and the command bar floating above it (overflows into "…" when too narrow). */
private fun canvas(text: WLabel): WComponent {
    text.horizontalAlignment = HorizontalAlignment.CENTER
    text.verticalAlignment = VerticalAlignment.CENTER
    text.foreground = WColor(0x55, 0x55, 0x55)
    val page = WBorder(text)
    page.width = PAGE_WIDTH
    page.height = PAGE_HEIGHT
    page.background = WColor.WHITE
    page.cornerRadius = 2.0
    page.horizontalAlignment = HorizontalAlignment.CENTER
    page.verticalAlignment = VerticalAlignment.CENTER
    val floatingModel = RibbonToolBarModel("floating")
    floatingModel.showLabels = false
    listOf(
        button("float.undo", "Undo", RibbonIcons.UNDO, RibbonItemSize.SMALL),
        button("float.redo", "Redo", RibbonIcons.REDO, RibbonItemSize.SMALL),
        RibbonSeparatorModel(),
        button("float.zoomIn", "Zoom In", RibbonIcon.glyph(""), RibbonItemSize.SMALL),
        button("float.zoomOut", "Zoom Out", RibbonIcon.glyph(""), RibbonItemSize.SMALL),
        button("float.fit", "Fit on Screen", RibbonIcon.glyph(""), RibbonItemSize.SMALL),
        RibbonSeparatorModel(),
        toggle("float.grid", "Grid", RibbonIcon.glyph("")),
        toggle("float.snap", "Snap", RibbonIcon.glyph("")).also { it.isChecked = true },
        toggle("float.rulers", "Rulers", RibbonIcon.glyph("")),
        button("float.rotate", "Rotate", RibbonIcon.glyph(""), RibbonItemSize.SMALL),
        button("float.flip", "Flip", RibbonIcon.glyph(""), RibbonItemSize.SMALL),
        button("float.export", "Export", RibbonIcon.glyph(""), RibbonItemSize.SMALL),
    ).forEach { floatingModel.items.add(it) }
    val floating = WBorder(WRibbonToolBar(floatingModel))
    floating.maxWidth = FLOATING_WIDTH
    floating.cornerRadius = BAR_RADIUS
    floating.padding = FLOATING_PADDING
    floating.horizontalAlignment = HorizontalAlignment.CENTER
    floating.verticalAlignment = VerticalAlignment.BOTTOM
    floating.margin = FLOATING_MARGIN
    WRibbonTheme.setThemeBrush(floating, "RibbonCommandBarBackgroundBrush") { floating.background = it }
    WRibbonTheme.setThemeBrush(floating, "RibbonCommandBarBorderBrush") { floating.borderColor = it }
    floating.borderThickness = 1.0
    val grid = WGrid()
    grid.add(page, row = 0, column = 0)
    grid.add(floating, row = 0, column = 0)
    val area = WBorder(grid)
    area.background = WColor(0x3A, 0x3A, 0x3A)
    area.cornerRadius = BAR_RADIUS
    area.margin = 6.0
    return area
}

/** The panel rail on the right (Layers, Properties, History, and Libraries, with labels). */
private fun panelRail(): WComponent {
    val model = RibbonToolBarModel("panels")
    model.orientation = RibbonToolBarOrientation.VERTICAL
    model.showLabels = true
    fun panel(id: String, label: String, icon: RibbonIcon) = toggle(id, label, icon).also {
        it.groupName = "panel"
        it.showLabelInSimplified = true
    }
    model.items.add(panel("panel.layers", "Layers", RibbonIcon.glyph("")).also { it.isChecked = true })
    model.items.add(panel("panel.properties", "Properties", RibbonIcon.glyph("")))
    model.items.add(panel("panel.history", "History", RibbonIcon.glyph("")))
    model.items.add(panel("panel.library", "Libraries", RibbonIcon.glyph("")))
    val bar = WRibbonToolBar(model)
    bar.margin = 6.0
    bar.verticalAlignment = VerticalAlignment.TOP
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
private const val MAX_SIZE = 5000.0
private const val HARDNESS = 80.0
private const val PERCENT = 100.0
private const val TEXT_SIZE = 24.0
private const val SIZE_WIDTH = 70.0
private const val HARDNESS_WIDTH = 62.0
private const val MODE_WIDTH = 110.0
private const val OPACITY_WIDTH = 100.0
private const val BAR_RADIUS = 6.0
private const val OPTIONS_MARGIN = 4.0
private const val PAGE_WIDTH = 480.0
private const val PAGE_HEIGHT = 320.0
private const val FLOATING_WIDTH = 360.0
private const val FLOATING_MARGIN = 16.0
private const val FLOATING_PADDING = 4.0
private const val ZOOM = 66.0
