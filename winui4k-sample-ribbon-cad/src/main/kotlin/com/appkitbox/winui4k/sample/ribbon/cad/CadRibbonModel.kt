package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemRenderer
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMinimizeBehavior
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonScreenTip
import com.appkitbox.winui4k.ribbon.RibbonSeparatorModel
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.dropDown
import com.appkitbox.winui4k.sample.ribbon.shell.group
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.tab
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/** A drawing layer (name and color, on/freeze/lock state). */
data class CadLayer(val name: String, val color: String, val isOn: Boolean = true, val isFrozen: Boolean = false, val isLocked: Boolean = false) {
    override fun toString(): String = name
}

/** Drawing layers. */
val CAD_LAYERS: List<CadLayer> = listOf(
    CadLayer("0", "#FFFFFF"), CadLayer("A-WALL", "#FFFFFF"), CadLayer("A-DOOR", "#F2C94C"), CadLayer("A-GLAZ", "#4FC3D9"),
    CadLayer("A-FURN", "#8BC34A"), CadLayer("A-ANNO-DIMS", "#E5534B"), CadLayer("A-ANNO-TEXT", "#FFFFFF"),
    CadLayer("A-HATCH", "#9E9E9E", isFrozen = true), CadLayer("A-AREA", "#B388FF", isOn = false), CadLayer("CENTER", "#B388FF"),
    CadLayer("DEFPOINTS", "#FFFFFF", isOn = false),
)

/** One layer row (on/freeze/lock icons, a color swatch, and the name; AutoCAD's layer drop-down). */
private fun layerRow(layer: CadLayer): WComponent {
    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    fun glyph(code: String, color: WColor) = WLabel(code).also {
        it.fontFamily = "Segoe Fluent Icons"
        it.fontSize = GLYPH_SIZE
        it.foreground = color
        it.verticalAlignment = VerticalAlignment.CENTER
    }
    row.add(glyph("", if (layer.isOn) WColor(0xF2, 0xC9, 0x4C) else WColor(0x88, 0x88, 0x88)))
    row.add(glyph(if (layer.isFrozen) "" else "", if (layer.isFrozen) WColor(0x4F, 0xC3, 0xD9) else WColor(0xF2, 0xC9, 0x4C)))
    row.add(glyph(if (layer.isLocked) "" else "", WColor(0xF2, 0xC9, 0x4C)))
    val swatch = WBorder()
    swatch.width = SWATCH
    swatch.height = SWATCH
    swatch.background = RibbonColor.parse(layer.color).let { WColor(it.r, it.g, it.b) }
    swatch.verticalAlignment = VerticalAlignment.CENTER
    row.add(swatch)
    row.add(WLabel(layer.name).also { it.verticalAlignment = VerticalAlignment.CENTER })
    return row
}

/** Color, linetype, and lineweight combo boxes (ByLayer). */
private fun propertyCombo(id: String, label: String, values: List<String>, swatch: Boolean) =
    RibbonComboBoxModel(id, label, values).also { combo ->
        combo.selectedItem = values[0]
        combo.inputWidth = PROPERTY_WIDTH
        if (swatch) {
            combo.itemRenderer = RibbonItemRenderer { item ->
                WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL).also { row ->
                    row.add(
                        WBorder().also {
                            it.width = SWATCH
                            it.height = SWATCH
                            it.background = WColor(0xEE, 0xEE, 0xEE)
                        },
                    )
                    row.add(WLabel(item.toString()))
                }
            }
        }
    }

/**
 * An AutoCAD-style ribbon model (the same structure as cad.png in the RibbonSpace README): [Home] [Insert] [Annotate]
 * [Parametric] [View] [Manage] [Output] [Collaborate], an expanded panel (slide-out), the layer drop-down, and the
 * [Hatch Editor] contextual tab.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // builds AutoCAD's [Home] tab declaratively from top to bottom
fun createCadModel(): RibbonModel = RibbonModel().apply {
    title = "Drawing1.dwg"
    applicationButtonLabel = "A"
    isApplicationButtonVisible = false
    minimizeBehavior = RibbonMinimizeBehavior.CYCLE_ALL
    displayMode = RibbonDisplayMode.CLASSIC
    tabs.add(
        tab(
            "home", "Home", "H",
            group(
                "draw", "Draw",
                button("line", "Line", RibbonIcons.LINE, RibbonItemSize.LARGE).also {
                    it.shortcut = "L"
                    it.screenTip = RibbonScreenTip("Line", "Creates straight line segments.").also { tip ->
                        tip.extendedDescription = "You can create a series of contiguous line segments. Each segment is a line object that can be edited separately. Press Enter to end."
                        tip.extendedImage = RibbonIcons.LINE
                        tip.helpText = "Press F1 for more help"
                    }
                },
                button("polyline", "Polyline", RibbonIcons.POLYGON, RibbonItemSize.LARGE),
                dropDown("circle", "Circle", RibbonIcons.CIRCLE, RibbonItemSize.LARGE, menuItem("circle.center", "Center, Radius"), menuItem("circle.diameter", "Center, Diameter"), menuItem("circle.3p", "3-Point")),
                dropDown("arc", "Arc", RibbonIcons.REDO, RibbonItemSize.LARGE, menuItem("arc.3p", "3-Point"), menuItem("arc.start", "Start, Center, End")),
                dropDown("rectangle", "Rectangle", RibbonIcons.RECTANGLE, RibbonItemSize.SMALL, menuItem("rect", "Rectangle"), menuItem("polygon", "Polygon")),
                dropDown("hatch", "Hatch", RibbonIcons.FILL, RibbonItemSize.SMALL, menuItem("hatch.hatch", "Hatch"), menuItem("hatch.gradient", "Gradient")),
                dropDown("ellipse", "Ellipse", RibbonIcons.CIRCLE, RibbonItemSize.SMALL, menuItem("ellipse.center", "Center")),
            ).also { g ->
                g.slideOutItems.add(button("spline", "Spline", RibbonIcons.PEN))
                g.slideOutItems.add(button("construction", "Construction Line", RibbonIcons.LINE))
                g.slideOutItems.add(button("revcloud", "Revision Cloud", RibbonIcons.CLOUD))
                g.slideOutItems.add(button("donut", "Donut", RibbonIcons.CIRCLE))
            },
            group(
                "modify",
                "Modify",
                RibbonRowModel(button("move", "Move", RibbonIcons.MOVE), button("rotate", "Rotate", RibbonIcons.ROTATE), dropDown("trim", "Trim", RibbonIcons.CROP, RibbonItemSize.MEDIUM, menuItem("trim.trim", "Trim"), menuItem("trim.extend", "Extend"))),
                RibbonRowModel(button("copyObj", "Copy", RibbonIcons.COPY), button("mirror", "Mirror", RibbonIcons.SPLIT), dropDown("fillet", "Fillet", RibbonIcons.UNDO, RibbonItemSize.MEDIUM, menuItem("fillet.fillet", "Fillet"), menuItem("fillet.chamfer", "Chamfer"))),
                RibbonRowModel(button("stretch", "Stretch", RibbonIcons.RESIZE), button("scale", "Scale", RibbonIcons.ZOOM), dropDown("array", "Array", RibbonIcons.GRID, RibbonItemSize.MEDIUM, menuItem("array.rect", "Rectangular Array"), menuItem("array.polar", "Polar Array"))),
                RibbonButtonGroupModel(button("erase", "Erase", RibbonIcons.ERASER, RibbonItemSize.SMALL), button("explode", "Explode", RibbonIcons.UNGROUP, RibbonItemSize.SMALL), button("offset", "Offset", RibbonIcons.LAYERS, RibbonItemSize.SMALL)),
            ).also { g ->
                g.itemsLayout = com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout.ROWS
                g.slideOutItems.add(button("join", "Join", RibbonIcons.MERGE))
                g.slideOutItems.add(button("break", "Break", RibbonIcons.CUT))
                g.reductionOrder = 1
            },
            group("annotation", "Annotation", dropDown("annotation.text", "Annotation", RibbonIcons.TEXT, RibbonItemSize.LARGE, menuItem("text.multi", "Multiline Text"), menuItem("dim.linear", "Linear"))).also { it.reductionOrder = 3 },
            createLayersGroup(),
            group(
                "block",
                "Block",
                dropDown("block.insert", "Insert", RibbonIcons.COMPONENT, RibbonItemSize.LARGE, menuItem("block.recent", "Recent Blocks..."), menuItem("block.library", "From Libraries...")),
                button("block.create", "Create", RibbonIcons.ADD, RibbonItemSize.SMALL),
                button("block.edit", "Edit", RibbonIcons.PEN, RibbonItemSize.SMALL),
            ).also { it.reductionOrder = 2 },
            group(
                "properties",
                "Properties",
                button("matchProp", "Match Properties", RibbonIcons.BRUSH, RibbonItemSize.LARGE),
                propertyCombo("prop.color", "Color", listOf("ByLayer", "ByBlock", "Red", "Yellow", "Green", "Cyan"), swatch = true),
                propertyCombo("prop.linetype", "Linetype", listOf("ByLayer", "ByBlock", "Continuous", "CENTER", "HIDDEN"), swatch = false),
                propertyCombo("prop.lineweight", "Lineweight", listOf("ByLayer", "ByBlock", "0.25 mm", "0.35 mm", "0.50 mm"), swatch = false),
            ).also {
                it.isDialogLauncherVisible = true
                it.reductionOrder = 2
            },
            group("groups", "Groups", dropDown("groups.group", "Group", RibbonIcons.GROUP, RibbonItemSize.LARGE, menuItem("group.create", "Group"), menuItem("group.ungroup", "Ungroup"))).also { it.reductionOrder = 4 },
            group("utilities", "Utilities", dropDown("utilities.measure", "Measure", RibbonIcons.MEASURE, RibbonItemSize.LARGE, menuItem("measure.distance", "Distance"), menuItem("measure.area", "Area"))).also { it.reductionOrder = 4 },
            group("clipboard", "Clipboard", dropDown("clipboard.paste", "Paste", RibbonIcons.PASTE, RibbonItemSize.LARGE, menuItem("paste.paste", "Paste"), menuItem("paste.block", "Paste as Block"))).also { it.reductionOrder = 4 },
            group("view", "View", dropDown("view.views", "Views", RibbonIcons.VIEW, RibbonItemSize.LARGE, menuItem("view.top", "Top"), menuItem("view.iso", "SE Isometric"))).also { it.reductionOrder = 4 },
        ),
    )
    for ((id, label) in listOf("insert" to "Insert", "annotate" to "Annotate", "parametric" to "Parametric", "viewTab" to "View", "manage" to "Manage", "output" to "Output", "collaborate" to "Collaborate")) {
        tabs.add(tab(id, label, null, group("$id.main", label, button("$id.command", label, RibbonIcons.SETTINGS, RibbonItemSize.LARGE))))
    }
    contextualGroups.add(
        RibbonContextualGroupModel("hatchEditor", "Hatch Editor").also {
            it.color = RibbonColor.parse("#2E9BD6")
            it.activation = RibbonContextualActivation.SELECT_ON_SHOW
        },
    )
    tabs.add(
        tab(
            "hatchCreation",
            "Hatch Creation",
            null,
            group("boundaries", "Boundaries", button("hatch.pick", "Pick Points", RibbonIcons.POINTER, RibbonItemSize.LARGE)),
            group("pattern", "Pattern", button("hatch.solid", "SOLID", RibbonIcons.FILL, RibbonItemSize.LARGE)),
            group("close", "Close", button("hatch.close", "Close Hatch Creation", RibbonIcons.CHECKMARK, RibbonItemSize.LARGE)),
        ).also { it.contextualGroupId = "hatchEditor" },
    )
}

/** The [Layers] group (Layer Properties, layer toggle buttons, the layer drop-down, and Make Current). */
private fun createLayersGroup(): RibbonGroupModel = group(
    "layers",
    "Layers",
    button("layer.properties", "Layer Properties", RibbonIcons.LAYERS, RibbonItemSize.LARGE),
    RibbonRowModel(
        toggle("layer.isolate", "Layer Off", RibbonIcons.LIGHT),
        toggle("layer.freeze", "Layer Freeze", RibbonIcons.SYNC),
        toggle("layer.lock", "Layer Lock", RibbonIcons.LOCK),
        RibbonSeparatorModel(),
        button("layer.match", "Match Layer", RibbonIcons.LAYERS, RibbonItemSize.SMALL),
    ),
    RibbonComboBoxModel("layer.current", "Layer", CAD_LAYERS).also { combo ->
        combo.selectedItem = CAD_LAYERS[1]
        combo.inputWidth = LAYER_WIDTH
        combo.itemRenderer = RibbonItemRenderer { item -> layerRow(item as CadLayer) }
    },
    button("layer.makeCurrent", "Make Current", RibbonIcons.CHECKMARK),
).also {
    it.itemsLayout = com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout.COLUMNS
    it.slideOutItems.add(button("layer.walk", "Layer Walk", RibbonIcons.VIEW))
    it.slideOutItems.add(button("layer.merge", "Layer Merge", RibbonIcons.MERGE))
}

private const val GLYPH_SIZE = 14.0
private const val SWATCH = 12.0
private const val LAYER_WIDTH = 190.0
private const val PROPERTY_WIDTH = 150.0
