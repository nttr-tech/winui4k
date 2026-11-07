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
    MOVE(Messages.get("tool.move"), RibbonIcon.glyph("\uE7C2"), ToolContext.MOVE),
    SELECT(Messages.get("tool.select"), RibbonIcon.glyph("\uE8B3"), ToolContext.MOVE),
    BRUSH(Messages.get("tool.brush"), RibbonIcon.glyph("\uE771"), ToolContext.BRUSH),
    PEN(Messages.get("tool.pen"), RibbonIcon.glyph("\uEDFB"), ToolContext.BRUSH),
    ERASER(Messages.get("tool.eraser"), RibbonIcon.glyph("\uE75C"), ToolContext.BRUSH),
    TYPE(Messages.get("tool.type"), RibbonIcon.glyph("\uE8D2"), ToolContext.TEXT),
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
        val window = RibbonDemoWindow(Messages.get("window.title"), RibbonDemoApp.TOOLS, null)
        val status = RibbonLabelModel("status.tool", Tool.BRUSH.label)
        val canvasText = WLabel(Messages.get("canvas.hint"))
        val options = WRibbonContextualToolBar()
        options.setContent(ToolContext.MOVE.name, moveOptions())
        options.setContent(ToolContext.BRUSH.name, brushOptions())
        options.setContent(ToolContext.TEXT.name, textOptions())
        options.activeContext = ToolContext.BRUSH.name
        options.margin = OPTIONS_MARGIN
        val palette = toolPalette { tool ->
            options.activeContext = tool.context.name
            status.label = tool.label
            canvasText.text = Messages.get("canvas.using", tool.label)
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
    val recent = item("file.recent", Messages.get("menu.file.recent")).also {
        it.items.add(item("file.recent.poster", "poster.psd"))
        it.items.add(item("file.recent.logo", "logo.svg"))
    }
    model.items.add(
        menu(
            "file",
            Messages.get("common.file"),
            item("file.new", Messages.get("menu.file.new"), "Ctrl+N"),
            item("file.open", Messages.get("menu.file.open"), "Ctrl+O"),
            recent,
            RibbonMenuSeparatorModel(),
            item("file.save", Messages.get("menu.file.save"), "Ctrl+S"),
            item("file.export", Messages.get("menu.file.export"), "Alt+Shift+Ctrl+W"),
        ),
    )
    model.items.add(
        menu(
            "edit",
            Messages.get("menu.edit"),
            item("edit.undo", Messages.get("common.undo"), "Ctrl+Z"),
            item("edit.redo", Messages.get("common.redo"), "Shift+Ctrl+Z"),
            RibbonMenuSeparatorModel(),
            item("edit.cut", Messages.get("menu.edit.cut"), "Ctrl+X"),
            item("edit.copy", Messages.get("common.copy"), "Ctrl+C"),
            item("edit.paste", Messages.get("menu.edit.paste"), "Ctrl+V"),
        ),
    )
    model.items.add(menu("image", Messages.get("menu.image"), item("image.size", Messages.get("menu.image.size")), item("image.canvas", Messages.get("menu.image.canvas")), check("image.aspect", Messages.get("menu.image.aspect"), true)))
    model.items.add(menu("layer", Messages.get("menu.layer"), item("layer.new", Messages.get("menu.layer.new"), "Shift+Ctrl+N"), item("layer.duplicate", Messages.get("menu.layer.duplicate"))))
    model.items.add(menu("view", Messages.get("common.view"), check("view.rulers", Messages.get("menu.rulers"), true, "Ctrl+R"), check("view.grid", Messages.get("menu.grid"), false), check("view.snap", Messages.get("menu.snap"), true)))
    model.items.add(menu("window", Messages.get("common.window"), check("window.layers", Messages.get("menu.layers"), true), check("window.properties", Messages.get("menu.properties"), true)))
    model.items.add(menu("help", Messages.get("common.help"), item("help.shortcuts", Messages.get("menu.help.shortcuts")), item("help.about", Messages.get("menu.help.about"))))
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
    Messages.get("options.move"),
    Tool.MOVE.icon,
    RibbonCheckBoxModel("move.autoSelect", Messages.get("options.move.auto.select"), isChecked = true),
    RibbonCheckBoxModel("move.transform", Messages.get("options.move.transform")),
    RibbonSeparatorModel(),
    button("move.alignLeft", Messages.get("common.align.left"), RibbonIcons.ALIGN_LEFT, RibbonItemSize.SMALL),
    button("move.alignCenter", Messages.get("common.align.center"), RibbonIcons.ALIGN_CENTER, RibbonItemSize.SMALL),
    button("move.alignRight", Messages.get("common.align.right"), RibbonIcons.ALIGN_RIGHT, RibbonItemSize.SMALL),
)

/** The options for [Brush], [Pen], and [Eraser]. */
private fun brushOptions(): WComponent = optionBar(
    "brush",
    Messages.get("options.brush"),
    Tool.BRUSH.icon,
    RibbonSpinnerModel("brush.size", Messages.get("options.brush.size"), BRUSH_SIZE).also {
        it.minimum = 1.0
        it.maximum = MAX_SIZE
        it.unit = "px"
        it.inputWidth = SIZE_WIDTH
    },
    RibbonSpinnerModel("brush.hardness", Messages.get("options.brush.hardness"), HARDNESS).also {
        it.maximum = PERCENT
        it.unit = "%"
        it.inputWidth = HARDNESS_WIDTH
    },
    RibbonComboBoxModel("brush.mode", Messages.get("options.brush.mode"), listOf(Messages.get("options.brush.normal"), Messages.get("options.brush.multiply"), Messages.get("options.brush.screen"), Messages.get("options.brush.overlay"))).also {
        it.selectedItem = Messages.get("options.brush.normal")
        it.text = Messages.get("options.brush.normal")
        it.showLabel = true
        it.inputWidth = MODE_WIDTH
    },
    RibbonSliderModel("brush.opacity", Messages.get("options.brush.opacity")).also {
        it.value = PERCENT
        it.maximum = PERCENT
        it.sliderWidth = OPACITY_WIDTH
    },
    toggle("brush.airbrush", Messages.get("options.brush.airbrush"), RibbonIcon.glyph("\uE706")),
    toggle("brush.pressure", Messages.get("options.brush.pressure"), RibbonIcon.glyph("\uEDFB")),
    RibbonColorPickerModel("brush.color", Messages.get("options.brush.color"), RibbonIcon.glyph("\uE790")).also { it.selectedColor = RibbonColor.parse("#1473E6") },
)

/** The options for [Text] (alignment is a segmented switch). */
private fun textOptions(): WComponent = optionBar(
    "text",
    Messages.get("options.text"),
    Tool.TYPE.icon,
    RibbonFontComboBoxModel("text.font").also { it.selectedItem = "Segoe UI" },
    RibbonFontSizeComboBoxModel("text.size").also { it.selectedItem = TEXT_SIZE },
    RibbonButtonGroupModel(
        toggle("text.bold", Messages.get("common.bold"), RibbonIcons.BOLD),
        toggle("text.italic", Messages.get("common.italic"), RibbonIcons.ITALIC),
    ),
    RibbonSegmentedModel("text.alignment", Messages.get("options.text.alignment")).also { segmented ->
        segmented.segments.add(RibbonSegmentModel("left", Messages.get("options.text.left"), RibbonIcons.ALIGN_LEFT))
        segmented.segments.add(RibbonSegmentModel("center", Messages.get("options.text.center"), RibbonIcons.ALIGN_CENTER))
        segmented.segments.add(RibbonSegmentModel("right", Messages.get("options.text.right"), RibbonIcons.ALIGN_RIGHT))
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
        RibbonSplitButtonModel("tool.shape", Messages.get("palette.rectangle")).also { shape ->
            shape.icon = RibbonIcon.glyph("\uE739")
            shape.size = RibbonItemSize.SMALL
            shape.followLastChoice = true
            shape.menuItems.add(RibbonMenuItemModel("shape.rectangle", Messages.get("palette.rectangle"), RibbonIcon.glyph("\uE739")))
            shape.menuItems.add(RibbonMenuItemModel("shape.ellipse", Messages.get("palette.ellipse"), RibbonIcon.glyph("\uEA3A")))
            shape.menuItems.add(RibbonMenuItemModel("shape.polygon", Messages.get("palette.polygon"), RibbonIcon.glyph("\uE879")))
            shape.menuItems.add(RibbonMenuItemModel("shape.line", Messages.get("palette.line"), RibbonIcon.glyph("\uE7C9")))
        },
    )
    model.items.add(button("tool.crop", Messages.get("palette.crop"), RibbonIcon.glyph("\uE7A8"), RibbonItemSize.SMALL))
    model.items.add(button("tool.zoom", Messages.get("common.zoom"), RibbonIcon.glyph("\uE71E"), RibbonItemSize.SMALL))
    model.items.add(button("tool.hand", Messages.get("palette.hand"), RibbonIcon.glyph("\uE7C9"), RibbonItemSize.SMALL))
    model.items.add(RibbonSeparatorModel())
    model.items.add(RibbonColorPickerModel("palette.foreground", Messages.get("palette.foreground"), RibbonIcon.glyph("\uE790")).also { it.selectedColor = RibbonColor.parse("#1473E6") })
    model.items.add(RibbonColorPickerModel("palette.background", Messages.get("palette.background"), RibbonIcon.glyph("\uE790")).also { it.selectedColor = RibbonColor.WHITE })
    val bar = WRibbonToolBar(model)
    bar.margin = 6.0
    bar.verticalAlignmentInParent = VerticalAlignment.TOP
    return WRibbonTheme.surface(bar, "RibbonCommandBarBackgroundBrush")
}

/** The canvas (a white image in a dark work area) and the command bar floating above it (overflows into "…" when too narrow). */
private fun canvas(text: WLabel): WComponent {
    text.horizontalAlignmentInParent = HorizontalAlignment.CENTER
    text.verticalAlignmentInParent = VerticalAlignment.CENTER
    text.foreground = WColor(0x55, 0x55, 0x55)
    val page = WBorder(text)
    page.width = PAGE_WIDTH
    page.height = PAGE_HEIGHT
    page.background = WColor.WHITE
    page.cornerRadius = 2.0
    page.horizontalAlignmentInParent = HorizontalAlignment.CENTER
    page.verticalAlignmentInParent = VerticalAlignment.CENTER
    val floatingModel = RibbonToolBarModel("floating")
    floatingModel.showLabels = false
    listOf(
        button("float.undo", Messages.get("common.undo"), RibbonIcons.UNDO, RibbonItemSize.SMALL),
        button("float.redo", Messages.get("common.redo"), RibbonIcons.REDO, RibbonItemSize.SMALL),
        RibbonSeparatorModel(),
        button("float.zoomIn", Messages.get("floating.zoom.in"), RibbonIcon.glyph("\uE8A3"), RibbonItemSize.SMALL),
        button("float.zoomOut", Messages.get("floating.zoom.out"), RibbonIcon.glyph("\uE71F"), RibbonItemSize.SMALL),
        button("float.fit", Messages.get("floating.fit"), RibbonIcon.glyph("\uE740"), RibbonItemSize.SMALL),
        RibbonSeparatorModel(),
        toggle("float.grid", Messages.get("menu.grid"), RibbonIcon.glyph("\uE80A")),
        toggle("float.snap", Messages.get("menu.snap"), RibbonIcon.glyph("\uE8AD")).also { it.isChecked = true },
        toggle("float.rulers", Messages.get("menu.rulers"), RibbonIcon.glyph("\uECC6")),
        button("float.rotate", Messages.get("floating.rotate"), RibbonIcon.glyph("\uE7AD"), RibbonItemSize.SMALL),
        button("float.flip", Messages.get("floating.flip"), RibbonIcon.glyph("\uE8AB"), RibbonItemSize.SMALL),
        button("float.export", Messages.get("floating.export"), RibbonIcon.glyph("\uEDE1"), RibbonItemSize.SMALL),
    ).forEach { floatingModel.items.add(it) }
    val floating = WBorder(WRibbonToolBar(floatingModel))
    floating.maxWidth = FLOATING_WIDTH
    floating.cornerRadius = BAR_RADIUS
    floating.padding = FLOATING_PADDING
    floating.horizontalAlignmentInParent = HorizontalAlignment.CENTER
    floating.verticalAlignmentInParent = VerticalAlignment.BOTTOM
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
    model.items.add(panel("panel.layers", Messages.get("menu.layers"), RibbonIcon.glyph("\uE81E")).also { it.isChecked = true })
    model.items.add(panel("panel.properties", Messages.get("menu.properties"), RibbonIcon.glyph("\uE713")))
    model.items.add(panel("panel.history", Messages.get("panel.history"), RibbonIcon.glyph("\uE81C")))
    model.items.add(panel("panel.library", Messages.get("panel.library"), RibbonIcon.glyph("\uE8F1")))
    val bar = WRibbonToolBar(model)
    bar.margin = 6.0
    bar.verticalAlignmentInParent = VerticalAlignment.TOP
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
