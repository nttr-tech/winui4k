package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.common.group
import com.appkitbox.winui4k.sample.ribbon.common.radioItem
import com.appkitbox.winui4k.sample.ribbon.common.tab

// The ribbon tabs of the RibbonSpace CAD demo (samples/RibbonSpace.Demo/Pages/CadPage.xaml, MIT License),
// converted by tools/gen_cad_ribbon.py into code that builds the WinUI4K model.

/** The CAD ribbon tabs (the regular tabs, the 3D workspace tabs and the contextual tabs). */
internal fun cadTabs(): List<RibbonTabModel> = listOf(
    homeTab(),
    solidTab(),
    surfaceTab(),
    meshTab(),
    visualizeTab(),
    insertTab(),
    annotateTab(),
    parametricTab(),
    viewTab(),
    manageTab(),
    outputTab(),
    collaborateTab(),
    textEditorTab(),
    hatchCreationTab(),

)

/** The [Home] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun homeTab(): RibbonTabModel = tab(
    "home", "Home", "H",
    group(
        "draw", "Draw",
        cmd("line", "Line", ic(CadIcons.LINE), RibbonItemSize.LARGE).also {
            it.keyTip = "LI"
            it.screenTip = richTip("Line", "Creates straight line segments.\n\nLINE", extendedDescription = "With the LINE command, you can create a series of contiguous line segments. Each segment is a line object that can be edited separately. Specify the first point, then each next point; press Enter to end or type C to close the sequence.", extendedImage = art(CadArt.LINE_HELP), helpText = "Press F1 for more help")
        },
        cmd("polyline", "Polyline", ic(CadIcons.POLYLINE), RibbonItemSize.LARGE).also {
            it.keyTip = "PL"
            it.screenTip = richTip("Polyline", "Creates a 2D polyline, a single object that is composed of line and arc segments.\n\nPLINE", extendedDescription = "Switch between line and arc segments while drawing; set the width of each segment. Polylines can be edited, joined and offset as a single object.", extendedImage = art(CadArt.POLYLINE_HELP), helpText = "Press F1 for more help")
        },
        spl(
            "circle",
            "Circle",
            ic(CadIcons.CIRCLE_CENTER_RADIUS),
            RibbonItemSize.LARGE,
            true,
            mi("circle.0", "Center, Radius", ic(CadIcons.CIRCLE_CENTER_RADIUS)),
            mi("circle.1", "Center, Diameter", ic(CadIcons.CIRCLE_TWO_POINT)),
            sep(),
            mi("circle.3", "2-Point", ic(CadIcons.CIRCLE_TWO_POINT)),
            mi("circle.4", "3-Point", ic(CadIcons.CIRCLE_THREE_POINT)),
            sep(),
            mi("circle.6", "Tan, Tan, Radius", ic(CadIcons.CIRCLE_TAN_TAN_RADIUS)),
            mi("circle.7", "Tan, Tan, Tan", ic(CadIcons.CIRCLE_TAN_TAN_TAN)),
        ).also {
            it.keyTip = "CI"
            it.screenTip = richTip("Circle", "Creates a circle using a center point and a radius.\n\nCIRCLE", extendedDescription = "Pick the center point, then the radius. The drop-down offers diameter, 2-point, 3-point and tangent methods; the button keeps the method you used last.", extendedImage = art(CadArt.CIRCLE_HELP), helpText = "Press F1 for more help")
        },
        spl(
            "arc",
            "Arc",
            ic(CadIcons.ARC),
            RibbonItemSize.LARGE,
            true,
            mi("arc.0", "3-Point", ic(CadIcons.ARC)),
            sep(),
            mi("arc.2", "Start, Center, End", ic(CadIcons.ARC_START_CENTER_END)),
            mi("arc.3", "Start, Center, Angle", ic(CadIcons.ARC_START_CENTER_END)),
            mi("arc.4", "Start, Center, Length", ic(CadIcons.ARC_START_CENTER_END)),
            sep(),
            mi("arc.6", "Start, End, Angle", ic(CadIcons.ARC)),
            mi("arc.7", "Start, End, Direction", ic(CadIcons.ARC)),
            mi("arc.8", "Start, End, Radius", ic(CadIcons.ARC)),
            sep(),
            mi("arc.10", "Continue", ic(CadIcons.ARC)),
        ).also {
            it.keyTip = "AC"
            it.screenTip = tip("Creates an arc.\n\nARC")
        },
        spl(
            "rectangle",
            "Rectangle",
            ic(CadIcons.RECTANGLE),
            RibbonItemSize.SMALL,
            true,
            mi("rectangle.0", "Rectangle", ic(CadIcons.RECTANGLE)),
            mi("rectangle.1", "Polygon", ic(CadIcons.POLYGON)),
        ).also {
            it.screenTip = tip("Creates a rectangular polyline.\n\nRECTANG")
        },
        spl(
            "hatch",
            "Hatch",
            ic(CadIcons.HATCH),
            RibbonItemSize.SMALL,
            true,
            mi("hatch.0", "Hatch", ic(CadIcons.HATCH)),
            mi("hatch.1", "Gradient", ic(CadIcons.GRADIENT)),
            mi("hatch.2", "Boundary", ic(CadIcons.BOUNDARY)),
        ).also {
            it.keyTip = "H"
            it.screenTip = richTip("Hatch", "Fills an enclosed area or selected objects with a hatch pattern, solid fill or gradient fill.\n\nHATCH", extendedDescription = "Click inside an area bounded by objects to pick it, or select the boundary objects. The Hatch Creation contextual tab opens with patterns, scale, angle and options.", extendedImage = art(CadArt.HATCH_HELP), helpText = "Press F1 for more help")
        },
        spl(
            "ellipse",
            "Ellipse",
            ic(CadIcons.ELLIPSE),
            RibbonItemSize.SMALL,
            true,
            mi("ellipse.0", "Center", ic(CadIcons.ELLIPSE)),
            mi("ellipse.1", "Axis, End", ic(CadIcons.ELLIPSE)),
            mi("ellipse.2", "Elliptical Arc", ic(CadIcons.ELLIPTICAL_ARC)),
        ).also {
            it.screenTip = tip("Creates an ellipse or an elliptical arc.\n\nELLIPSE")
        },
    ).also { g ->
        g.icon = ic(CadIcons.LINE)
        g.description = "Lines, circles, arcs, polylines and other basic geometry."
        g.slideOutItems.add(
            stack(
                false,
                cmd("spline", "Spline Fit", ic(CadIcons.SPLINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates a smooth curve that passes through fit points.\n\nSPLINE")
                },
                cmd("constructionLine", "Construction Line", ic(CadIcons.CONSTRUCTION_LINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates a line of infinite length.\n\nXLINE")
                },
                cmd("ray", "Ray", ic(CadIcons.RAY), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates a line that starts at a point and continues to infinity.\n\nRAY")
                },
                cmd("multiplePoints", "Multiple Points", ic(CadIcons.POINT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates multiple point objects.\n\nPOINT")
                },
                cmd("multiline", "Multiline", ic(CadIcons.MULTI_LINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates multiple parallel lines.\n\nMLINE")
                },
            ),
        )
        g.slideOutItems.add(
            stack(
                false,
                cmd("region", "Region", ic(CadIcons.REGION), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Converts an object that encloses an area into a region object.\n\nREGION")
                },
                cmd("revisionCloud", "Revision Cloud", ic(CadIcons.REVISION_CLOUD), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates or modifies a revision cloud.\n\nREVCLOUD")
                },
                cmd("donut", "Donut", ic(CadIcons.DONUT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates a filled circle or a wide ring.\n\nDONUT")
                },
                cmd("boundary", "Boundary", ic(CadIcons.BOUNDARY), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates a region or a polyline from an enclosed area.\n\nBOUNDARY")
                },
                cmd("gradient", "Gradient", ic(CadIcons.GRADIENT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Fills an enclosed area with a gradient fill.\n\nGRADIENT")
                },
            ),
        )
        g.slideOutItems.add(
            cmd("divide", "Divide", ic(CadIcons.POINT), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Places point objects or blocks along an object at equal intervals.\n\nDIVIDE")
            },
        )
    },
    group(
        "modify", "Modify",
        cmd("move", "Move", ic(CadIcons.MOVE), RibbonItemSize.MEDIUM).also {
            it.keyTip = "MV"
            it.screenTip = tip("Moves objects a specified distance in a specified direction.\n\nMOVE")
        },
        cmd("copy", "Copy", ic(CadIcons.COPY), RibbonItemSize.MEDIUM).also {
            it.keyTip = "CO"
            it.screenTip = tip("Copies objects a specified distance in a specified direction.\n\nCOPY")
        },
        cmd("stretch", "Stretch", ic(CadIcons.STRETCH), RibbonItemSize.MEDIUM).also {
            it.keyTip = "ST"
            it.screenTip = tip("Stretches objects crossed by a selection window or polygon.\n\nSTRETCH")
        },
        cmd("rotate", "Rotate", ic(CadIcons.ROTATE), RibbonItemSize.MEDIUM).also {
            it.keyTip = "RO"
            it.screenTip = tip("Rotates objects around a base point.\n\nROTATE")
        },
        cmd("mirror", "Mirror", ic(CadIcons.MIRROR), RibbonItemSize.MEDIUM).also {
            it.keyTip = "MI"
            it.screenTip = tip("Creates a mirrored copy of selected objects.\n\nMIRROR")
        },
        cmd("scale", "Scale", ic(CadIcons.SCALE), RibbonItemSize.MEDIUM).also {
            it.keyTip = "SC"
            it.screenTip = tip("Enlarges or reduces selected objects, keeping the proportions after scaling.\n\nSCALE")
        },
        spl(
            "trim",
            "Trim",
            ic(CadIcons.TRIM),
            RibbonItemSize.MEDIUM,
            true,
            mi("trim.0", "Trim", ic(CadIcons.TRIM)),
            mi("trim.1", "Extend", ic(CadIcons.EXTEND)),
        ).also {
            it.keyTip = "TR"
            it.screenTip = richTip("Trim", "Trims objects to meet the edges of other objects.\n\nTRIM", extendedDescription = "Select the portion of an object to trim: it is removed up to the nearest intersecting objects. Hold Shift to extend instead. Drag across several objects to trim them at once.", extendedImage = art(CadArt.TRIM_HELP), helpText = "Press F1 for more help")
        },
        spl(
            "fillet",
            "Fillet",
            ic(CadIcons.FILLET),
            RibbonItemSize.MEDIUM,
            true,
            mi("fillet.0", "Fillet", ic(CadIcons.FILLET)),
            mi("fillet.1", "Chamfer", ic(CadIcons.CHAMFER)),
            mi("fillet.2", "Blend Curves", ic(CadIcons.BLEND)),
        ).also {
            it.keyTip = "F"
            it.screenTip = richTip("Fillet", "Rounds and fillets the edges of objects.\n\nFILLET", extendedDescription = "Select two objects: an arc of the current fillet radius is created tangent to both, and the objects are trimmed or extended to meet it. A radius of 0 creates a sharp corner.", extendedImage = art(CadArt.FILLET_HELP), helpText = "Press F1 for more help")
        },
        spl(
            "array",
            "Array",
            ic(CadIcons.ARRAY_RECTANGULAR),
            RibbonItemSize.MEDIUM,
            true,
            mi("array.0", "Rectangular Array", ic(CadIcons.ARRAY_RECTANGULAR)),
            mi("array.1", "Path Array", ic(CadIcons.ARRAY_PATH)),
            mi("array.2", "Polar Array", ic(CadIcons.ARRAY_POLAR)),
        ).also {
            it.keyTip = "AR"
            it.screenTip = richTip("Rectangular Array", "Distributes object copies into any combination of rows, columns and levels.\n\nARRAYRECT", extendedDescription = "The array stays associative: drag its grips or use the Array contextual tab to change the number of rows and columns and their spacing.", extendedImage = art(CadArt.ARRAY_HELP), helpText = "Press F1 for more help")
        },
        cmd("erase", "Erase", ic(CadIcons.ERASE), RibbonItemSize.SMALL).also {
            it.keyTip = "E"
            it.screenTip = tip("Removes objects from a drawing.\n\nERASE")
        },
        cmd("explode", "Explode", ic(CadIcons.EXPLODE), RibbonItemSize.SMALL).also {
            it.keyTip = "X"
            it.screenTip = tip("Breaks a compound object into its component objects.\n\nEXPLODE")
        },
        cmd("offset", "Offset", ic(CadIcons.OFFSET), RibbonItemSize.SMALL).also {
            it.keyTip = "O"
            it.screenTip = richTip("Offset", "Creates concentric circles, parallel lines and parallel curves.\n\nOFFSET", extendedDescription = "Specify the offset distance, select an object, then click on the side to offset. Repeat to create several parallel copies at the same distance.", extendedImage = art(CadArt.OFFSET_HELP), helpText = "Press F1 for more help")
        },
    ).also { g ->
        g.icon = ic(CadIcons.MOVE)
        g.description = "Move, copy, rotate, trim, fillet, array and other editing tools."
        g.slideOutItems.add(
            stack(
                false,
                cmd("breakAtPoint", "Break at Point", ic(CadIcons.BREAK_AT_POINT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Breaks the selected object at a single point.\n\nBREAKATPOINT")
                },
                cmd("break", "Break", ic(CadIcons.BREAK), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Breaks the selected object between two points.\n\nBREAK")
                },
                cmd("join", "Join", ic(CadIcons.JOIN), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Joins the endpoints of similar objects to form a single object.\n\nJOIN")
                },
                cmd("chamfer", "Chamfer", ic(CadIcons.CHAMFER), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Bevels the edges of objects.\n\nCHAMFER")
                },
                cmd("blend", "Blend Curves", ic(CadIcons.BLEND), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Creates a spline in the gap between two selected lines or curves.\n\nBLEND")
                },
                cmd("lengthen", "Lengthen", ic(CadIcons.LENGTHEN), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Changes the length of objects and the included angle of arcs.\n\nLENGTHEN")
                },
            ),
        )
        g.slideOutItems.add(
            stack(
                false,
                cmd("align", "Align", ic(CadIcons.ALIGN), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Aligns objects with other objects in 2D and 3D.\n\nALIGN")
                },
                cmd("reverse", "Reverse", ic(CadIcons.REVERSE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Reverses the vertex order of lines, polylines, splines and helixes.\n\nREVERSE")
                },
                cmd("editPolyline", "Edit Polyline", ic(CadIcons.EDIT_POLYLINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Modifies polylines and 3D polygon meshes.\n\nPEDIT")
                },
                cmd("extend", "Extend", ic(CadIcons.EXTEND), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Extends objects to meet the edges of other objects.\n\nEXTEND")
                },
                cmd("arrayPolar", "Polar Array", ic(CadIcons.ARRAY_POLAR), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Evenly distributes object copies in a circular pattern.\n\nARRAYPOLAR")
                },
                cmd("arrayPath", "Path Array", ic(CadIcons.ARRAY_PATH), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Evenly distributes object copies along a path.\n\nARRAYPATH")
                },
            ),
        )
    },
    group(
        "annotation",
        "Annotation",
        spl(
            "text",
            "Text",
            ic(CadIcons.MULTILINE_TEXT),
            RibbonItemSize.LARGE,
            true,
            mi("text.0", "Multiline Text", ic(CadIcons.MULTILINE_TEXT)),
            mi("text.1", "Single Line", ic(CadIcons.SINGLE_LINE_TEXT)),
        ).also {
            it.keyTip = "TX"
            it.screenTip = tip("Creates a multiline text object.\n\nMTEXT")
        },
        spl(
            "dimension",
            "Dimension",
            ic(CadIcons.DIMENSION_LINEAR),
            RibbonItemSize.LARGE,
            true,
            mi("dimension.0", "Linear", ic(CadIcons.DIMENSION_LINEAR)),
            mi("dimension.1", "Aligned", ic(CadIcons.DIMENSION_ALIGNED)),
            mi("dimension.2", "Angular", ic(CadIcons.DIMENSION_ANGULAR)),
            mi("dimension.3", "Radius", ic(CadIcons.DIMENSION_RADIUS)),
            mi("dimension.4", "Diameter", ic(CadIcons.DIMENSION_DIAMETER)),
        ).also {
            it.keyTip = "D"
            it.screenTip = tip("Creates a linear dimension with a horizontal or vertical dimension line.\n\nDIMLINEAR")
        },
        spl(
            "leader",
            "Leader",
            ic(CadIcons.MULTILEADER),
            RibbonItemSize.MEDIUM,
            true,
            mi("leader.0", "Leader", ic(CadIcons.MULTILEADER)),
            mi("leader.1", "Add Leader", ic(CadIcons.MULTILEADER)),
            mi("leader.2", "Remove Leader", ic(CadIcons.MULTILEADER)),
        ).also {
            it.screenTip = tip("Creates a multileader object.\n\nMLEADER")
        },
        cmd("table", "Table", ic(CadIcons.TABLE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates an empty table object.\n\nTABLE")
        },
    ).also { g ->
        g.icon = ic(CadIcons.MULTILINE_TEXT)
        g.description = "Text, dimensions, leaders and tables."
        g.reductionOrder = 5
        g.slideOutItems.add(
            cmd("field", "Field", ic(CadIcons.FIELD), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Creates a multiline text object with a field that updates automatically.\n\nFIELD")
            },
        )
        g.slideOutItems.add(
            combo("textStyle", "Text Style", ic(CadIcons.TEXT_STYLE), 150.0, false, "Standard", "Standard", "Annotative", "Arial Notes", "Titles"),
        )
        g.slideOutItems.add(
            combo("dimStyle", "Dimension Style", ic(CadIcons.DIMENSION_STYLE), 150.0, false, "ISO-25", "ISO-25", "Annotative", "Architectural", "Standard"),
        )
        g.slideOutItems.add(
            combo("mleaderStyle", "Multileader Style", ic(CadIcons.MULTILEADER), 150.0, false, "Standard", "Standard", "Annotative"),
        )
        g.slideOutItems.add(
            combo("tableStyle", "Table Style", ic(CadIcons.TABLE), 150.0, false, "Standard", "Standard", "Door Schedule"),
        )
    },
    group(
        "layers",
        "Layers",
        cmd("layerProperties", "Layer\nProperties", ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.LARGE).also {
            it.keyTip = "LA"
            it.screenTip = tip("Manages layers and layer properties.\n\nLAYER")
        },
        stack(
            true,
            stack(
                false,
                cmd("layerOff", "Off", ic(CadIcons.LAYER_OFF), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Turns off the layer of a selected object.\n\nLAYOFF")
                },
                cmd("layerIsolate", "Isolate", ic(CadIcons.LAYER_ISOLATE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Hides or locks all layers except those of the selected objects.\n\nLAYISO")
                },
                cmd("layerFreeze", "Freeze", ic(CadIcons.LAYER_FREEZE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Freezes the layer of selected objects.\n\nLAYFRZ")
                },
                cmd("layerLock", "Lock", ic(CadIcons.LAYER_LOCK), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Locks the layer of a selected object.\n\nLAYLCK")
                },
                cmd("layerUnisolate", "Unisolate", ic(CadIcons.LAYER_UNISOLATE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Restores all layers that were hidden or locked with LAYISO.\n\nLAYUNISO")
                },
                cmd("layerPrevious", "Previous", ic(CadIcons.LAYER_PREVIOUS), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Undoes the last change or set of changes made to layer settings.\n\nLAYERP")
                },
            ),
            layerCombo(),
            stack(
                false,
                cmd("makeCurrent", "Make Current", ic(CadIcons.MAKE_CURRENT), RibbonItemSize.MEDIUM).also {
                    it.screenTip = tip("Sets the current layer to that of a selected object.\n\nLAYMCUR")
                },
                cmd("matchLayer", "Match Layer", ic(CadIcons.MATCH_LAYER), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Changes the layer of selected objects to match the destination layer.\n\nLAYMCH")
                },
            ),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.LAYER_PROPERTIES)
        g.description = "Layer properties, the current layer and layer tools."
        g.reductionOrder = 1
        g.slideOutItems.add(
            combo("layerState", "Layer State", null, 170.0, false, "Unsaved Layer State", "Unsaved Layer State", "Plot - Floor Plan", "Furniture Off", "New Layer State...", "Manage Layer States..."),
        )
        g.slideOutItems.add(
            stack(
                false,
                cmd("layerOnAll", "Turn All Layers On", ic(CadIcons.LAYER_ON), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Turns on all layers in the drawing.\n\nLAYON")
                },
                cmd("layerThawAll", "Thaw All Layers", ic(CadIcons.LAYER_THAW), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Thaws all layers in the drawing.\n\nLAYTHW")
                },
                cmd("layerUnlock", "Unlock", ic(CadIcons.LAYER_UNLOCK), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Unlocks the layer of a selected object.\n\nLAYULK")
                },
                cmd("layerMatch2", "Change to Current Layer", ic(CadIcons.MATCH_LAYER), RibbonItemSize.SMALL).also {
                    it.screenTip = tip("Changes the layer of selected objects to the current layer.\n\nLAYCUR")
                },
            ),
        )
        g.slideOutItems.add(
            cmd("layerWalk", "Layer Walk", ic(CadIcons.LAYER_ISOLATE), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Displays objects on selected layers and hides objects on all other layers.\n\nLAYWALK")
            },
        )
    },
    group(
        "block",
        "Block",
        gallery(
            "insertBlock", "Insert", ic(CadIcons.INSERT_BLOCK), RibbonItemSize.LARGE,
            gi("Door - 900", art(CadArt.BLOCK_DOOR), "Current Drawing Blocks"),
            gi("Window - 1500", art(CadArt.BLOCK_WINDOW), "Current Drawing Blocks"),
            gi("Chair", art(CadArt.BLOCK_CHAIR), "Current Drawing Blocks"),
            gi("Dining Table", art(CadArt.BLOCK_TABLE), "Current Drawing Blocks"),
            gi("Bed - Double", art(CadArt.BLOCK_BED), "Current Drawing Blocks"),
            gi("WC", art(CadArt.BLOCK_TOILET), "Current Drawing Blocks"),
            gi("Tree", art(CadArt.BLOCK_TREE), "Current Drawing Blocks"),
            gi("North Arrow", art(CadArt.BLOCK_NORTH_ARROW), "Current Drawing Blocks"),
            gi("Hex Bolt M10", art(CadArt.BLOCK_BOLT), "Recent Blocks"),
            gi("Hex Nut M10", art(CadArt.BLOCK_NUT), "Recent Blocks"),
            gi("Section Mark", art(CadArt.BLOCK_SECTION), "Recent Blocks"),
            gi("A3 Title Block", art(CadArt.BLOCK_TITLE), "Recent Blocks"),
        ).also { g ->
            g.itemWidth = 84.0
            g.itemHeight = 78.0
            g.dropDownColumns = 4
            g.isDropDownOnly = true
            g.keyTip = "I"
            g.screenTip = tip("Inserts a block or a drawing into the current drawing.\n\nINSERT")
            g.menuItems.add(mi("insertBlock.footer0", "Recent Blocks...", ic(CadIcons.RECENT)))
            g.menuItems.add(mi("insertBlock.footer1", "Blocks from Libraries...", ic(CadIcons.OPEN)))
        },
        cmd("createBlock", "Create", ic(CadIcons.CREATE_BLOCK), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a block definition from selected objects.\n\nBLOCK")
        },
        cmd("editBlock", "Edit", ic(CadIcons.EDIT_BLOCK), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Opens the block definition in the Block Editor.\n\nBEDIT")
        },
        spl(
            "editAttributes",
            "Edit Attribute",
            ic(CadIcons.EDIT_ATTRIBUTE),
            RibbonItemSize.MEDIUM,
            true,
            mi("editAttributes.0", "Single", ic(CadIcons.EDIT_ATTRIBUTE)),
            mi("editAttributes.1", "Multiple", ic(CadIcons.EDIT_ATTRIBUTE)),
        ).also {
            it.screenTip = tip("Edits the attributes in a block reference.\n\nEATTEDIT")
        },
    ).also { g ->
        g.icon = ic(CadIcons.INSERT_BLOCK)
        g.description = "Insert, create and edit blocks."
        g.reductionOrder = 4
        g.slideOutItems.add(
            cmd("defineAttributes", "Define Attributes", ic(CadIcons.DEFINE_ATTRIBUTES), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Creates an attribute definition for storing data in a block.\n\nATTDEF")
            },
        )
        g.slideOutItems.add(
            cmd("manageAttributes", "Manage Attributes", ic(CadIcons.EDIT_ATTRIBUTE), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Manages the attributes of a selected block definition.\n\nBATTMAN")
            },
        )
        g.slideOutItems.add(
            cmd("synchronize", "Synchronize", ic(CadIcons.EDIT_BLOCK), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Updates block references with new attributes of the block definition.\n\nATTSYNC")
            },
        )
    },
    group(
        "properties",
        "Properties",
        cmd("matchProperties", "Match\nProperties", ic(CadIcons.MATCH_PROPERTIES), RibbonItemSize.LARGE).also {
            it.keyTip = "MA"
            it.screenTip = tip("Applies the properties of a selected object to other objects.\n\nMATCHPROP")
        },
        stack(
            true,
            colorCombo("objectColor"),
            lineweightCombo("objectLineweight"),
            linetypeCombo("objectLinetype"),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.MATCH_PROPERTIES)
        g.description = "Colour, lineweight, linetype and other object properties."
        g.reductionOrder = 2
        g.isDialogLauncherVisible = true
        g.slideOutItems.add(
            slider("transparency", "Transparency", ic(CadIcons.TRANSPARENCY), 120.0, 0.0),
        )
        g.slideOutItems.add(
            cmd("list", "List", ic(CadIcons.PROPERTIES_PALETTE), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Displays property data for selected objects.\n\nLIST")
            },
        )
        g.slideOutItems.add(
            combo("plotStyle", "Plot Style", null, 120.0, false, "ByColor", "ByColor", "ByLayer"),
        )
    },
    group(
        "groups",
        "Groups",
        cmd("group", "Group", ic(CadIcons.GROUP), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Creates and manages saved sets of objects called groups.\n\nGROUP")
        },
        cmd("ungroup", "Ungroup", ic(CadIcons.UNGROUP), RibbonItemSize.SMALL).also {
            it.screenTip = tip("Disassociates the objects of a group.\n\nUNGROUP")
        },
        cmd("groupEdit", "Group Edit", ic(CadIcons.GROUP_EDIT), RibbonItemSize.SMALL).also {
            it.screenTip = tip("Adds or removes objects from the selected group.\n\nGROUPEDIT")
        },
        tgl("groupSelection", "Group Selection", ic(CadIcons.GROUP), RibbonItemSize.SMALL).also {
            it.screenTip = tip("Selecting one object of a group selects the whole group.")
            it.isChecked = true
        },
    ).also { g ->
        g.icon = ic(CadIcons.GROUP)
        g.reductionOrder = 6
    },
    group(
        "utilities",
        "Utilities",
        spl(
            "measure",
            "Measure",
            ic(CadIcons.MEASURE_DISTANCE),
            RibbonItemSize.LARGE,
            true,
            mi("measure.0", "Quick", ic(CadIcons.MEASURE_DISTANCE)),
            mi("measure.1", "Distance", ic(CadIcons.MEASURE_DISTANCE)),
            mi("measure.2", "Angle", ic(CadIcons.MEASURE_ANGLE)),
            mi("measure.3", "Area", ic(CadIcons.MEASURE_AREA)),
        ).also {
            it.screenTip = tip("Measures the distance between two points.\n\nMEASUREGEOM")
        },
        cmd("quickSelect", "Quick Select", ic(CadIcons.QUICK_SELECT), RibbonItemSize.SMALL).also {
            it.screenTip = tip("Creates a selection set based on filtering criteria.\n\nQSELECT")
        },
        cmd("selectAll", "Select All", ic(CadIcons.SELECT_ALL), RibbonItemSize.SMALL).also {
            it.shortcut = "Ctrl+A"
            it.screenTip = tip("Selects all objects in the drawing.\n\nSELECTALL")
        },
        cmd("idPoint", "ID Point", ic(CadIcons.ID_POINT), RibbonItemSize.SMALL).also {
            it.screenTip = tip("Displays the coordinates of a location.\n\nID")
        },
    ).also { g ->
        g.icon = ic(CadIcons.MEASURE_DISTANCE)
        g.reductionOrder = 7
        g.slideOutItems.add(
            cmd("calculator", "Quick Calculator", ic(CadIcons.CALCULATOR), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Opens the calculator.\n\nQUICKCALC")
            },
        )
        g.slideOutItems.add(
            cmd("pointStyle", "Point Style...", ic(CadIcons.POINT), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip("Specifies the display style and size of point objects.\n\nPTYPE")
            },
        )
    },
    group(
        "clipboard",
        "Clipboard",
        spl(
            "paste",
            "Paste",
            ic(CadIcons.PASTE),
            RibbonItemSize.LARGE,
            false,
            mi("paste.0", "Paste", ic(CadIcons.PASTE)),
            mi("paste.1", "Paste as Block", ic(CadIcons.INSERT_BLOCK)),
            mi("paste.2", "Paste to Original Coordinates", ic(CadIcons.PASTE)),
            sep(),
            mi("paste.4", "Paste Special...", null),
        ).also {
            it.shortcut = "Ctrl+V"
            it.screenTip = tip("Inserts objects from the Clipboard into the current drawing.\n\nPASTECLIP")
        },
        cmd("copyClip", "Copy Clip", ic(CadIcons.COPY_CLIP), RibbonItemSize.SMALL).also {
            it.shortcut = "Ctrl+C"
            it.screenTip = tip("Copies selected objects to the Clipboard.\n\nCOPYCLIP")
        },
        cmd("cutClip", "Cut", ic(CadIcons.CUT), RibbonItemSize.SMALL).also {
            it.shortcut = "Ctrl+X"
            it.screenTip = tip("Copies selected objects to the Clipboard and removes them from the drawing.\n\nCUTCLIP")
        },
    ).also { g ->
        g.icon = ic(CadIcons.PASTE)
        g.reductionOrder = 8
    },
    group(
        "homeView",
        "View",
        ddn(
            "baseView",
            "Base",
            ic(CadIcons.VIEWPORTS),
            RibbonItemSize.LARGE,
            mi("baseView.0", "From Model Space", ic(CadIcons.MODEL)),
            mi("baseView.1", "From Inventor", ic(CadIcons.BOX)),
        ).also {
            it.screenTip = tip("Creates a base view from model space or from an Inventor model.\n\nVIEWBASE")
        },
    ).also { g ->
        g.icon = ic(CadIcons.VIEWPORTS)
        g.reductionOrder = 9
    },
)

/** The [Solid] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun solidTab(): RibbonTabModel = tab(
    "solid",
    "Solid",
    "SO",
    group(
        "primitive",
        "Primitive",
        spl(
            "box",
            "Box",
            ic(CadIcons.BOX),
            RibbonItemSize.LARGE,
            true,
            mi("box.0", "Box", ic(CadIcons.BOX)),
            mi("box.1", "Cylinder", ic(CadIcons.CYLINDER)),
            mi("box.2", "Cone", ic(CadIcons.CONE)),
            mi("box.3", "Sphere", ic(CadIcons.SPHERE)),
        ).also {
            it.screenTip = tip("Creates a 3D solid box.\n\nBOX")
        },
        cmd("cylinder", "Cylinder", ic(CadIcons.CYLINDER), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Creates a 3D solid cylinder.\n\nCYLINDER")
        },
        cmd("sphere", "Sphere", ic(CadIcons.SPHERE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a 3D solid sphere.\n\nSPHERE")
        },
        cmd("cone", "Cone", ic(CadIcons.CONE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a 3D solid with a circular or elliptical base tapering to a point.\n\nCONE")
        },
        cmd("polysolid", "Polysolid", ic(CadIcons.MULTI_LINE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a 3D wall-like solid.\n\nPOLYSOLID")
        },
    ).also { g ->
        g.icon = ic(CadIcons.BOX)
    },
    group(
        "solidCreate",
        "Solid",
        cmd("extrude", "Extrude", ic(CadIcons.EXTRUDE), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Extends the dimensions of a 2D object or 3D face into 3D space.\n\nEXTRUDE")
        },
        cmd("presspull", "Presspull", ic(CadIcons.PRESSPULL), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Extrudes or offsets bounded areas or faces dynamically.\n\nPRESSPULL")
        },
        cmd("revolve", "Revolve", ic(CadIcons.REVOLVE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a 3D solid or surface by sweeping a 2D object around an axis.\n\nREVOLVE")
        },
        cmd("sweep", "Sweep", ic(CadIcons.SWEEP), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a 3D solid or surface by sweeping a 2D object along a path.\n\nSWEEP")
        },
        cmd("loft", "Loft", ic(CadIcons.LOFT), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a 3D solid or surface between several cross sections.\n\nLOFT")
        },
    ).also { g ->
        g.icon = ic(CadIcons.EXTRUDE)
    },
    group(
        "boolean",
        "Boolean",
        cmd("union", "Union", ic(CadIcons.UNION), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Combines selected 3D solids, surfaces or 2D regions.\n\nUNION")
        },
        cmd("subtract", "Subtract", ic(CadIcons.SUBTRACT), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Creates a 3D solid by subtracting one set of solids from another.\n\nSUBTRACT")
        },
        cmd("intersect", "Intersect", ic(CadIcons.INTERSECT), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Creates a 3D solid from the overlapping volume of solids.\n\nINTERSECT")
        },
    ).also { g ->
        g.icon = ic(CadIcons.UNION)
    },
    group(
        "solidEditing",
        "Solid Editing",
        cmd("filletEdge", "Fillet Edge", ic(CadIcons.FILLET), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Rounds and fillets the edges of solid objects.\n\nFILLETEDGE")
        },
        cmd("chamferEdge", "Chamfer Edge", ic(CadIcons.CHAMFER), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Bevels the edges of 3D solids and surfaces.\n\nCHAMFEREDGE")
        },
        cmd("slice", "Slice", ic(CadIcons.BREAK), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates new 3D solids by slicing an existing object.\n\nSLICE")
        },
        cmd("shell", "Shell", ic(CadIcons.OFFSET), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Creates a hollow, thin wall with a specified thickness.\n\nSOLIDEDIT")
        },
    ).also { g ->
        g.icon = ic(CadIcons.FILLET)
    },
).also { t ->
    t.isVisible = false
}

/** The [Surface] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun surfaceTab(): RibbonTabModel = tab(
    "surface",
    "Surface",
    "SU",
    group(
        "surfaceCreate",
        "Create",
        cmd("networkSurface", "Network", ic(CadIcons.SURFACE_NETWORK), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Creates a surface in the space between several curves in the U and V directions.\n\nSURFNETWORK")
        },
        cmd("planarSurface", "Planar", ic(CadIcons.SURFACE_PLANAR), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Creates a planar surface.\n\nPLANESURF")
        },
        cmd("loftSurface", "Loft", ic(CadIcons.LOFT), RibbonItemSize.MEDIUM),
        cmd("sweepSurface", "Sweep", ic(CadIcons.SWEEP), RibbonItemSize.MEDIUM),
        cmd("revolveSurface", "Revolve", ic(CadIcons.REVOLVE), RibbonItemSize.MEDIUM),
        cmd("extrudeSurface", "Extrude", ic(CadIcons.EXTRUDE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.LOFT)
    },
    group(
        "surfaceEdit",
        "Edit",
        cmd("surfaceFillet", "Fillet", ic(CadIcons.FILLET), RibbonItemSize.MEDIUM),
        cmd("surfaceTrim", "Trim", ic(CadIcons.TRIM), RibbonItemSize.MEDIUM),
        cmd("surfaceExtend", "Extend", ic(CadIcons.EXTEND), RibbonItemSize.MEDIUM),
        cmd("surfaceBlend", "Blend", ic(CadIcons.BLEND), RibbonItemSize.MEDIUM),
        cmd("surfacePatch", "Patch", ic(CadIcons.SURFACE_PLANAR), RibbonItemSize.MEDIUM),
        cmd("surfaceOffset", "Offset", ic(CadIcons.OFFSET), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.FILLET)
    },
    group(
        "controlVertices",
        "Control Vertices",
        tgl("showCv", "Show CV", ic(CadIcons.SPLINE), RibbonItemSize.LARGE),
        cmd("rebuild", "Rebuild", ic(CadIcons.SURFACE_NETWORK), RibbonItemSize.MEDIUM),
        cmd("addCv", "Add", ic(CadIcons.POINT), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.SPLINE)
    },
).also { t ->
    t.isVisible = false
}

/** The [Mesh] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun meshTab(): RibbonTabModel = tab(
    "mesh",
    "Mesh",
    "E",
    group(
        "meshPrimitives",
        "Primitive",
        spl(
            "meshBox",
            "Mesh Box",
            ic(CadIcons.MESH_BOX),
            RibbonItemSize.LARGE,
            true,
            mi("meshBox.0", "Mesh Box", ic(CadIcons.MESH_BOX)),
            mi("meshBox.1", "Mesh Cylinder", ic(CadIcons.CYLINDER)),
            mi("meshBox.2", "Mesh Sphere", ic(CadIcons.SPHERE)),
        ).also {
            it.screenTip = tip("Creates a 3D mesh box.\n\nMESH")
        },
        cmd("revolvedSurface", "Revolved Surface", ic(CadIcons.REVOLVE), RibbonItemSize.MEDIUM),
        cmd("edgeSurface", "Edge Surface", ic(CadIcons.SURFACE_NETWORK), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MESH_BOX)
    },
    group(
        "meshEdit",
        "Mesh",
        cmd("smoothObject", "Smooth Object", ic(CadIcons.MESH_SMOOTH), RibbonItemSize.LARGE),
        cmd("smoothMore", "Smooth More", ic(CadIcons.SPHERE), RibbonItemSize.MEDIUM),
        cmd("smoothLess", "Smooth Less", ic(CadIcons.MESH_BOX), RibbonItemSize.MEDIUM),
        cmd("refineMesh", "Refine Mesh", ic(CadIcons.GRID), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MESH_SMOOTH)
    },
    group(
        "convertMesh",
        "Convert Mesh",
        cmd("convertSolid", "Convert to Solid", ic(CadIcons.BOX), RibbonItemSize.LARGE),
        cmd("convertSurface", "Convert to Surface", ic(CadIcons.SURFACE_PLANAR), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.BOX)
    },
).also { t ->
    t.isVisible = false
}

/** The [Visualize] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun visualizeTab(): RibbonTabModel = tab(
    "visualize",
    "Visualize",
    "Z",
    group(
        "visualStyles",
        "Visual Styles",
        gallery(
            "visualStyleGallery", "Visual Styles", art(CadArt.STYLE_SHADED), RibbonItemSize.LARGE,
            gi("2D Wireframe", art(CadArt.STYLE2_D_WIREFRAME), null),
            gi("Hidden", art(CadArt.STYLE_HIDDEN), null),
            gi("Shaded", art(CadArt.STYLE_SHADED), null),
            gi("Realistic", art(CadArt.STYLE_REALISTIC), null),
            gi("Conceptual", art(CadArt.STYLE_CONCEPTUAL), null),
            gi("X-Ray", art(CadArt.STYLE_X_RAY), null),
        ).also { g ->
            g.itemWidth = 58.0
            g.itemHeight = 58.0
            g.minColumns = 3
            g.maxColumns = 6
        },
    ).also { g ->
        g.icon = art(CadArt.STYLE_SHADED)
        g.reductionOrder = 1
    },
    group(
        "lights",
        "Lights",
        ddn(
            "createLight",
            "Create Light",
            ic(CadIcons.LIGHT),
            RibbonItemSize.LARGE,
            mi("createLight.0", "Point", ic(CadIcons.LIGHT)),
            mi("createLight.1", "Spot", ic(CadIcons.LIGHT)),
            mi("createLight.2", "Distant", ic(CadIcons.SUN)),
        ),
        tgl("sunStatus", "Sun Status", ic(CadIcons.SUN), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.LIGHT)
    },
    group(
        "materials",
        "Materials",
        cmd("materialsBrowser", "Materials Browser", ic(CadIcons.MATERIAL), RibbonItemSize.LARGE),
        cmd("attachByLayer", "Attach By Layer", ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM),
        cmd("removeMaterials", "Remove Materials", ic(CadIcons.ERASE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MATERIAL)
    },
    group(
        "render",
        "Render",
        cmd("renderButton", "Render to Size", ic(CadIcons.RENDER), RibbonItemSize.LARGE),
        combo("renderPreset", "Preset", null, 120.0, false, "Medium", "Low", "Medium", "High", "Coffee-Break Quality"),
        combo("renderSize", "Size", null, 120.0, false, "800 x 600 px", "640 x 480 px", "800 x 600 px", "1920 x 1080 px"),
    ).also { g ->
        g.icon = ic(CadIcons.RENDER)
        g.isDialogLauncherVisible = true
    },
    group(
        "camera",
        "Camera",
        cmd("createCamera", "Create Camera", ic(CadIcons.CAMERA), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.CAMERA)
    },
).also { t ->
    t.isVisible = false
}

/** The [Insert] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun insertTab(): RibbonTabModel = tab(
    "insert", "Insert", "I",
    group(
        "insertBlockGroup",
        "Block",
        cmd("insertBlock2", "Insert", ic(CadIcons.INSERT_BLOCK), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Inserts a block or a drawing into the current drawing.\n\nINSERT")
        },
        cmd("editAttributes2", "Edit Attributes", ic(CadIcons.EDIT_ATTRIBUTE), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.INSERT_BLOCK)
    },
    group(
        "blockDefinition",
        "Block Definition",
        cmd("createBlock2", "Create Block", ic(CadIcons.CREATE_BLOCK), RibbonItemSize.LARGE),
        cmd("defineAttributes2", "Define Attributes", ic(CadIcons.DEFINE_ATTRIBUTES), RibbonItemSize.LARGE),
        cmd("manageAttributes2", "Manage Attributes", ic(CadIcons.EDIT_ATTRIBUTE), RibbonItemSize.MEDIUM),
        cmd("blockEditor", "Block Editor", ic(CadIcons.EDIT_BLOCK), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.CREATE_BLOCK)
    },
    group(
        "reference",
        "Reference",
        cmd("attach", "Attach", ic(CadIcons.ATTACH), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Inserts a reference to an external file (drawing, image, PDF, DWF or DGN).\n\nATTACH")
        },
        cmd("clip", "Clip", ic(CadIcons.CLIP), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Crops the display of a selected external reference, image or underlay.\n\nCLIP")
        },
        cmd("adjust", "Adjust", ic(CadIcons.TRANSPARENCY), RibbonItemSize.MEDIUM),
        cmd("underlayLayers", "Underlay Layers", ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM),
        combo("frames", "Frames", null, 120.0, false, "Frames vary*", "Frames vary*", "Hide frames", "Display and plot frames", "Display but don't plot"),
    ).also { g ->
        g.icon = ic(CadIcons.ATTACH)
        g.isDialogLauncherVisible = true
    },
    group(
        "importGroup",
        "Import",
        cmd("pdfImport", "PDF Import", ic(CadIcons.IMPORT_PDF), RibbonItemSize.LARGE),
        cmd("importFile", "Import", ic(CadIcons.OPEN), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.IMPORT_PDF)
    },
    group(
        "data",
        "Data",
        cmd("fieldInsert", "Field", ic(CadIcons.FIELD), RibbonItemSize.LARGE),
        cmd("updateFields", "Update Fields", ic(CadIcons.REDO), RibbonItemSize.MEDIUM),
        cmd("hyperlink", "Hyperlink", ic(CadIcons.SHARE), RibbonItemSize.MEDIUM),
        cmd("oleObject", "OLE Object", ic(CadIcons.TABLE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.FIELD)
    },
    group(
        "linking",
        "Linking & Extraction",
        cmd("dataExtraction", "Extract Data", ic(CadIcons.DATA_EXTRACTION), RibbonItemSize.LARGE),
        cmd("dataLink", "Data Link", ic(CadIcons.TABLE), RibbonItemSize.MEDIUM),
        cmd("downloadSource", "Download from Source", ic(CadIcons.OPEN), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.DATA_EXTRACTION)
    },
)

/** The [Annotate] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun annotateTab(): RibbonTabModel = tab(
    "annotate", "Annotate", "A",
    group(
        "annotateText",
        "Text",
        spl(
            "annotateMText",
            "Multiline Text",
            ic(CadIcons.MULTILINE_TEXT),
            RibbonItemSize.LARGE,
            true,
            mi("annotateMText.0", "Multiline Text", ic(CadIcons.MULTILINE_TEXT)),
            mi("annotateMText.1", "Single Line", ic(CadIcons.SINGLE_LINE_TEXT)),
        ),
        cmd("checkSpelling", "Check Spelling", RibbonIcon.glyph("\uE8FB"), RibbonItemSize.LARGE),
        stack(
            true,
            combo("annotateTextStyle", null, ic(CadIcons.TEXT_STYLE), 140.0, false, "Standard", "Standard", "Annotative"),
            textBox("findText", ic(CadIcons.SEARCH), 140.0),
            combo("textHeight", null, ic(CadIcons.SINGLE_LINE_TEXT), 140.0, true, "2.5", "1.8", "2.5", "3.5", "5"),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.MULTILINE_TEXT)
        g.isDialogLauncherVisible = true
    },
    group(
        "dimensions", "Dimensions",
        spl(
            "annotateDimension",
            "Dimension",
            ic(CadIcons.DIMENSION_LINEAR),
            RibbonItemSize.LARGE,
            true,
            mi("annotateDimension.0", "Linear", ic(CadIcons.DIMENSION_LINEAR)),
            mi("annotateDimension.1", "Aligned", ic(CadIcons.DIMENSION_ALIGNED)),
            mi("annotateDimension.2", "Angular", ic(CadIcons.DIMENSION_ANGULAR)),
            mi("annotateDimension.3", "Radius", ic(CadIcons.DIMENSION_RADIUS)),
            mi("annotateDimension.4", "Diameter", ic(CadIcons.DIMENSION_DIAMETER)),
        ),
        cmd("quickDim", "Quick", ic(CadIcons.DIMENSION_ALIGNED), RibbonItemSize.MEDIUM),
        cmd("continueDim", "Continue", ic(CadIcons.DIMENSION_LINEAR), RibbonItemSize.MEDIUM),
        cmd("baselineDim", "Baseline", ic(CadIcons.DIMENSION_LINEAR), RibbonItemSize.MEDIUM),
        cmd("dimBreak", "Break", ic(CadIcons.BREAK), RibbonItemSize.SMALL),
        cmd("dimSpace", "Adjust Space", ic(CadIcons.STRETCH), RibbonItemSize.SMALL),
        cmd("dimUpdate", "Update", ic(CadIcons.REDO), RibbonItemSize.SMALL),
        combo("annotateDimStyle", null, ic(CadIcons.DIMENSION_STYLE), 120.0, false, "ISO-25", "ISO-25", "Architectural"),
    ).also { g ->
        g.icon = ic(CadIcons.DIMENSION_LINEAR)
        g.isDialogLauncherVisible = true
    },
    group(
        "leaders",
        "Leaders",
        cmd("multileader", "Multileader", ic(CadIcons.MULTILEADER), RibbonItemSize.LARGE),
        cmd("addLeader", "Add Leader", ic(CadIcons.MULTILEADER), RibbonItemSize.MEDIUM),
        cmd("alignLeaders", "Align", ic(CadIcons.ALIGN), RibbonItemSize.MEDIUM),
        cmd("collectLeaders", "Collect", ic(CadIcons.GROUP), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MULTILEADER)
        g.isDialogLauncherVisible = true
    },
    group(
        "tables",
        "Tables",
        cmd("tableInsert", "Table", ic(CadIcons.TABLE), RibbonItemSize.LARGE),
        cmd("extractTable", "Extract Data", ic(CadIcons.DATA_EXTRACTION), RibbonItemSize.MEDIUM),
        cmd("linkData", "Link Data", ic(CadIcons.FIELD), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.TABLE)
        g.isDialogLauncherVisible = true
    },
    group(
        "markup",
        "Markup",
        cmd("revcloud2", "Revision Cloud", ic(CadIcons.REVISION_CLOUD), RibbonItemSize.LARGE),
        cmd("wipeout", "Wipeout", ic(CadIcons.REGION), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.REVISION_CLOUD)
    },
    group(
        "annotationScaling",
        "Annotation Scaling",
        cmd("scaleList", "Scale List", ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.LARGE),
        cmd("addCurrentScale", "Add Current Scale", ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.MEDIUM),
        cmd("syncScale", "Sync Scale Positions", ic(CadIcons.ALIGN), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.ANNOTATION_SCALE)
    },
)

/** The [Parametric] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun parametricTab(): RibbonTabModel = tab(
    "parametric",
    "Parametric",
    "P",
    group(
        "geometric", "Geometric",
        cmd("autoConstrain", "Auto\nConstrain", ic(CadIcons.CONSTRAINT_AUTO), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Applies geometric constraints to a selection set based on tolerances.\n\nAUTOCONSTRAIN")
        },
        cmd("coincident", "Coincident", ic(CadIcons.CONSTRAINT_COINCIDENT), RibbonItemSize.SMALL),
        cmd("collinear", "Collinear", ic(CadIcons.CONSTRAINT_COLLINEAR), RibbonItemSize.SMALL),
        cmd("concentric", "Concentric", ic(CadIcons.CONSTRAINT_CONCENTRIC), RibbonItemSize.SMALL),
        cmd("fix", "Fix", ic(CadIcons.LAYER_LOCK), RibbonItemSize.SMALL),
        cmd("parallel", "Parallel", ic(CadIcons.CONSTRAINT_PARALLEL), RibbonItemSize.SMALL),
        cmd("perpendicular", "Perpendicular", ic(CadIcons.CONSTRAINT_PERPENDICULAR), RibbonItemSize.SMALL),
        cmd("horizontal", "Horizontal", ic(CadIcons.CONSTRAINT_HORIZONTAL), RibbonItemSize.SMALL),
        cmd("vertical", "Vertical", ic(CadIcons.CONSTRAINT_VERTICAL), RibbonItemSize.SMALL),
        cmd("tangent", "Tangent", ic(CadIcons.CONSTRAINT_TANGENT), RibbonItemSize.SMALL),
        cmd("symmetric", "Symmetric", ic(CadIcons.MIRROR), RibbonItemSize.SMALL),
        cmd("equal", "Equal", ic(CadIcons.CONSTRAINT_EQUAL), RibbonItemSize.SMALL),
        cmd("smooth", "Smooth", ic(CadIcons.SPLINE), RibbonItemSize.SMALL),
        cmd("showHide", "Show/Hide", ic(CadIcons.LAYER_ON), RibbonItemSize.MEDIUM),
        cmd("showAll", "Show All", ic(CadIcons.LAYER_ON), RibbonItemSize.MEDIUM),
        cmd("hideAll", "Hide All", ic(CadIcons.LAYER_OFF), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.CONSTRAINT_AUTO)
        g.isDialogLauncherVisible = true
    },
    group(
        "dimensional",
        "Dimensional",
        spl(
            "dimConstraint",
            "Linear",
            ic(CadIcons.DIMENSION_LINEAR),
            RibbonItemSize.LARGE,
            true,
            mi("dimConstraint.0", "Linear", ic(CadIcons.DIMENSION_LINEAR)),
            mi("dimConstraint.1", "Aligned", ic(CadIcons.DIMENSION_ALIGNED)),
            mi("dimConstraint.2", "Angular", ic(CadIcons.DIMENSION_ANGULAR)),
            mi("dimConstraint.3", "Radius", ic(CadIcons.DIMENSION_RADIUS)),
            mi("dimConstraint.4", "Diameter", ic(CadIcons.DIMENSION_DIAMETER)),
        ),
        cmd("convertConstraint", "Convert", ic(CadIcons.REDO), RibbonItemSize.MEDIUM),
        cmd("showDynamic", "Show Dynamic Constraints", ic(CadIcons.LAYER_ON), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.DIMENSION_LINEAR)
        g.isDialogLauncherVisible = true
    },
    group(
        "parametricManage",
        "Manage",
        cmd("deleteConstraints", "Delete Constraints", ic(CadIcons.ERASE), RibbonItemSize.LARGE),
        cmd("parametersManager", "Parameters Manager", ic(CadIcons.CALCULATOR), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.CUSTOMIZE)
    },
)

/** The [View] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun viewTab(): RibbonTabModel = tab(
    "view", "View", "V",
    group(
        "viewportTools",
        "Viewport Tools",
        tgl("ucsIcon", "UCS Icon", ic(CadIcons.MODEL), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Shows the UCS icon in the drawing area.\n\nUCSICON")
            it.isChecked = true
        },
        tgl("viewCube", "ViewCube", ic(CadIcons.BOX), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Shows the view cube navigation widget.")
            it.isChecked = true
        },
        tgl("navBar", "Navigation Bar", ic(CadIcons.PAN), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Shows the navigation bar (pan, zoom, orbit).")
            it.isChecked = true
        },
    ).also { g ->
        g.icon = ic(CadIcons.MODEL)
    },
    group(
        "namedViews",
        "Named Views",
        cmd("viewManager", "View Manager", ic(CadIcons.VIEW_MANAGER), RibbonItemSize.MEDIUM),
        cmd("newView", "New View", ic(CadIcons.NAMED_VIEWS), RibbonItemSize.MEDIUM),
        combo("viewList", null, null, 120.0, false, "Unsaved View", "Unsaved View", "Ground Floor", "Kitchen Detail"),
    ).also { g ->
        g.icon = ic(CadIcons.NAMED_VIEWS)
    },
    group(
        "modelViewports",
        "Model Viewports",
        ddn(
            "viewportConfiguration",
            "Viewport\nConfiguration",
            ic(CadIcons.VIEWPORTS),
            RibbonItemSize.LARGE,
            mi("viewportConfiguration.0", "Single", null),
            mi("viewportConfiguration.1", "Two: Vertical", null),
            mi("viewportConfiguration.2", "Three: Right", null),
            mi("viewportConfiguration.3", "Four: Equal", null),
        ),
        cmd("namedViewports", "Named", ic(CadIcons.NAMED_VIEWS), RibbonItemSize.MEDIUM),
        cmd("joinViewports", "Join", ic(CadIcons.JOIN), RibbonItemSize.MEDIUM),
        cmd("restoreViewports", "Restore", ic(CadIcons.UNDO), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.VIEWPORTS)
    },
    group(
        "palettes",
        "Palettes",
        tgl("toolPalettes", "Tool Palettes", ic(CadIcons.WORKSPACE), RibbonItemSize.LARGE),
        tgl("propertiesPalette", "Properties", ic(CadIcons.PROPERTIES_PALETTE), RibbonItemSize.LARGE).also {
            it.shortcut = "Ctrl+1"
        },
        tgl("layerPalette", "Layer Properties", ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM),
        tgl("commandLine", "Command Line", ic(CadIcons.COMMAND_LINE), RibbonItemSize.MEDIUM).also {
            it.shortcut = "Ctrl+9"
            it.isChecked = true
        },
        tgl("externalReferences", "External References", ic(CadIcons.ATTACH), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.PROPERTIES_PALETTE)
    },
    group(
        "interface",
        "Interface",
        tgl("fileTabs", "File Tabs", ic(CadIcons.FILE_TABS), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
        tgl("layoutTabs", "Layout Tabs", ic(CadIcons.PAPER), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
        tgl("lightTheme", "Light Theme", ic(CadIcons.SUN), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Switches the interface between the dark (default) and the light theme.")
        },
    ).also { g ->
        g.icon = ic(CadIcons.WORKSPACE)
    },
    group(
        "ribbonFeatures", "Ribbon",
        ddn(
            "minimizeBehavior",
            "Minimize Behavior",
            ic(CadIcons.CLEAN_SCREEN),
            RibbonItemSize.LARGE,
        ).also {
            it.screenTip = tip("What the minimize button at the end of the tab row (and Ctrl+F1) does.")
        },
        cmd("cycleMinimize", "Cycle State", ic(CadIcons.REDO), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Moves to the next minimize state: full ribbon → panel buttons → panel titles → tabs.")
        },
        tgl("panelTitles", "Panel Titles", ic(CadIcons.MULTILINE_TEXT), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Shows or hides the panel titles (also in the ribbon context menu).")
            it.isChecked = true
        },
        cmd("floatLayers", "Float Layers Panel", ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Floats the Layers panel; drag any panel title away from the ribbon to float it.")
        },
        cmd("returnPanels", "Return Panels", ic(CadIcons.UNDO), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Returns all floating panels to the ribbon.")
        },
        cmd("showTextEditor", "Text Editor Tab", ic(CadIcons.MULTILINE_TEXT), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Shows the Text Editor contextual tab (as when editing multiline text).")
        },
        cmd("showHatch", "Hatch Creation Tab", ic(CadIcons.HATCH), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip("Shows the Hatch Creation contextual tab.")
        },
    ).also { g ->
        g.icon = ic(CadIcons.CLEAN_SCREEN)
        g.description = "Try the AutoCAD-class ribbon features of RibbonSpace."
    },
)

/** The [Manage] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun manageTab(): RibbonTabModel = tab(
    "manage",
    "Manage",
    "M",
    group(
        "actionRecorder",
        "Action Recorder",
        tgl("record", "Record", ic(CadIcons.ACTION_RECORDER), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Starts the Action Recorder.\n\nACTRECORD")
        },
        cmd("play", "Play", RibbonIcon.glyph("\uE768"), RibbonItemSize.MEDIUM),
        cmd("insertMessage", "Insert Message", ic(CadIcons.MULTILINE_TEXT), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.ACTION_RECORDER)
    },
    group(
        "customization",
        "Customization",
        cmd("userInterface", "User\nInterface", ic(CadIcons.CUSTOMIZE), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Manages customized user interface elements.\n\nCUI")
        },
        cmd("toolPalettesCustomize", "Tool Palettes", ic(CadIcons.WORKSPACE), RibbonItemSize.MEDIUM),
        cmd("importCustomization", "Import", ic(CadIcons.OPEN), RibbonItemSize.MEDIUM),
        cmd("editAliases", "Edit Aliases", ic(CadIcons.COMMAND_LINE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.CUSTOMIZE)
    },
    group(
        "applications",
        "Applications",
        cmd("loadApplication", "Load\nApplication", ic(CadIcons.LOAD_APPLICATION), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Loads and unloads applications.\n\nAPPLOAD")
        },
        cmd("runScript", "Run Script", ic(CadIcons.COMMAND_LINE), RibbonItemSize.MEDIUM),
        cmd("scriptEditor", "Script Editor", ic(CadIcons.EDIT_POLYLINE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.LOAD_APPLICATION)
    },
    group(
        "cadStandards",
        "CAD Standards",
        cmd("checkStandards", "Check", ic(CadIcons.QUICK_SELECT), RibbonItemSize.MEDIUM),
        cmd("configureStandards", "Configure", ic(CadIcons.CUSTOMIZE), RibbonItemSize.MEDIUM),
        cmd("layerTranslator", "Layer Translator", ic(CadIcons.MATCH_LAYER), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.QUICK_SELECT)
    },
    group(
        "cleanup",
        "Cleanup",
        cmd("purge", "Purge", ic(CadIcons.PURGE), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Removes unused items from the drawing.\n\nPURGE")
        },
        cmd("overkill", "Delete Duplicates", ic(CadIcons.ERASE), RibbonItemSize.MEDIUM),
        cmd("units", "Units", ic(CadIcons.UNITS), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.PURGE)
    },
)

/** The [Output] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun outputTab(): RibbonTabModel = tab(
    "output",
    "Output",
    "O",
    group(
        "plotGroup",
        "Plot",
        cmd("plot", "Plot", ic(CadIcons.PLOT), RibbonItemSize.LARGE).also {
            it.shortcut = "Ctrl+P"
            it.screenTip = tip("Plots a drawing to a plotter, printer or file.\n\nPLOT")
        },
        cmd("batchPlot", "Batch Plot", ic(CadIcons.BATCH_PLOT), RibbonItemSize.LARGE),
        cmd("pageSetup", "Page Setup Manager", ic(CadIcons.PAGE_SETUP), RibbonItemSize.MEDIUM),
        cmd("plotterManager", "Plotter Manager", ic(CadIcons.PLOT), RibbonItemSize.MEDIUM),
        cmd("preview", "Preview", ic(CadIcons.ZOOM_WINDOW), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.PLOT)
    },
    group(
        "exportGroup",
        "Export to DWF/PDF",
        spl(
            "exportSplit",
            "Export",
            ic(CadIcons.EXPORT_PDF),
            RibbonItemSize.LARGE,
            true,
            mi("exportSplit.0", "PDF", ic(CadIcons.EXPORT_PDF)),
            mi("exportSplit.1", "DWF", ic(CadIcons.PUBLISH)),
            mi("exportSplit.2", "DWFx", ic(CadIcons.PUBLISH)),
        ),
        combo("exportExtents", "Export:", null, 110.0, false, "Display", "Display", "Extents", "Window"),
        combo("exportPageSetup", "Page Setup:", null, 110.0, false, "Current", "Current", "Override"),
        cmd("exportPreview", "Preview", ic(CadIcons.ZOOM_WINDOW), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.EXPORT_PDF)
    },
    group(
        "publishGroup",
        "Publish",
        cmd("publishButton", "Publish", ic(CadIcons.PUBLISH), RibbonItemSize.LARGE),
        cmd("print3d", "3D Print", ic(CadIcons.BOX), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.PUBLISH)
    },
)

/** The [Collaborate] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun collaborateTab(): RibbonTabModel = tab(
    "collaborate",
    "Collaborate",
    "C",
    group(
        "share",
        "Share",
        cmd("shareDrawing", "Share", ic(CadIcons.SHARE), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Shares a copy of the drawing with others.")
        },
        cmd("sharedViews", "Shared Views", ic(CadIcons.NAMED_VIEWS), RibbonItemSize.LARGE),
        cmd("pushToDocs", "Push to Cloud", ic(CadIcons.PUBLISH), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.SHARE)
    },
    group(
        "compare",
        "Compare",
        cmd("dwgCompare", "DWG Compare", ic(CadIcons.COMPARE), RibbonItemSize.LARGE).also {
            it.screenTip = tip("Highlights the differences between two revisions of a drawing.\n\nCOMPARE")
        },
        cmd("xrefCompare", "Xref Compare", ic(CadIcons.ATTACH), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.COMPARE)
    },
    group(
        "collaborateMarkup",
        "Markup",
        cmd("markupImport", "Markup Import", ic(CadIcons.IMPORT_PDF), RibbonItemSize.LARGE),
        cmd("markupAssist", "Markup Assist", ic(CadIcons.REVISION_CLOUD), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.REVISION_CLOUD)
    },
)

/** The [Text Editor] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun textEditorTab(): RibbonTabModel = tab(
    "textEditorTab", "Text Editor", "X",
    group(
        "teStyle",
        "Style",
        gallery(
            "teStyleGallery",
            "Text Style",
            ic(CadIcons.TEXT_STYLE),
            RibbonItemSize.LARGE,
            gi("Standard", null, null),
            gi("Annotative", null, null),
            gi("Titles", null, null),
        ).also { g ->
            g.itemWidth = 64.0
            g.itemHeight = 50.0
            g.minColumns = 2
            g.maxColumns = 3
        },
        stack(
            true,
            tgl("teAnnotative", "Annotative", ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.MEDIUM),
            combo("teHeight", null, ic(CadIcons.SINGLE_LINE_TEXT), 90.0, true, "2.5", "1.8", "2.5", "3.5", "5"),
            cmd("teMask", "Mask", ic(CadIcons.REGION), RibbonItemSize.MEDIUM),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.TEXT_STYLE)
    },
    group(
        "teFormatting",
        "Formatting",
        stack(
            false,
            RibbonButtonGroupModel(
                tgl("teBold", "Bold", RibbonIcon.glyph("\uE8DD"), RibbonItemSize.SMALL).also {
                    it.shortcut = "Ctrl+B"
                },
                tgl("teItalic", "Italic", RibbonIcon.glyph("\uE8DB"), RibbonItemSize.SMALL).also {
                    it.shortcut = "Ctrl+I"
                },
                tgl("teUnderline", "Underline", RibbonIcon.glyph("\uE8DC"), RibbonItemSize.SMALL).also {
                    it.shortcut = "Ctrl+U"
                },
                tgl("teStrike", "Strikethrough", RibbonIcon.glyph("\uEDE0"), RibbonItemSize.SMALL),
            ),
            fontCombo("teFont", "Arial", 130.0),
        ),
        stack(
            false,
            colorCombo("teColor"),
            cmd("teClearFormatting", "Clear Formatting", RibbonIcon.glyph("\uE8E6"), RibbonItemSize.SMALL),
            cmd("teMatch", "Match", ic(CadIcons.MATCH_PROPERTIES), RibbonItemSize.SMALL),
        ),
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE8D2")
        g.itemsLayout = RibbonGroupItemsLayout.ROWS
        g.rowCount = 2
    },
    group(
        "teParagraph",
        "Paragraph",
        ddn(
            "teJustification",
            "Justification",
            RibbonIcon.glyph("\uE8E4"),
            RibbonItemSize.LARGE,
            radioItem("teJustification.0", "Top Left TL", "just", true),
            radioItem("teJustification.1", "Top Center TC", "just", false),
            radioItem("teJustification.2", "Middle Center MC", "just", false),
            radioItem("teJustification.3", "Bottom Right BR", "just", false),
        ),
        RibbonButtonGroupModel(
            tgl("teLeft", "Left", RibbonIcon.glyph("\uE8E4"), RibbonItemSize.SMALL).also {
                it.isChecked = true
                it.groupName = "teAlign"
            },
            tgl("teCenter", "Center", RibbonIcon.glyph("\uE8E3"), RibbonItemSize.SMALL).also {
                it.groupName = "teAlign"
            },
            tgl("teRight", "Right", RibbonIcon.glyph("\uE8E2"), RibbonItemSize.SMALL).also {
                it.groupName = "teAlign"
            },
        ),
        ddn(
            "teBullets",
            "Bullets and Numbering",
            RibbonIcon.glyph("\uE8FD"),
            RibbonItemSize.SMALL,
            mi("teBullets.0", "Off", null),
            mi("teBullets.1", "Numbered", null),
            mi("teBullets.2", "Lettered", null),
            mi("teBullets.3", "Bulleted", null),
        ),
        ddn(
            "teLineSpacing",
            "Line Spacing",
            RibbonIcon.glyph("\uE9E9"),
            RibbonItemSize.SMALL,
            radioItem("teLineSpacing.0", "1.0x", "lineSpacing", true),
            radioItem("teLineSpacing.1", "1.5x", "lineSpacing", false),
            radioItem("teLineSpacing.2", "2.0x", "lineSpacing", false),
        ),
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE8E4")
    },
    group(
        "teInsert",
        "Insert",
        ddn(
            "teColumns",
            "Columns",
            ic(CadIcons.TABLE),
            RibbonItemSize.LARGE,
            mi("teColumns.0", "No Columns", null),
            mi("teColumns.1", "Dynamic Columns", null),
            mi("teColumns.2", "Static Columns", null),
        ),
        ddn(
            "teSymbol",
            "Symbol",
            RibbonIcon.text("Ø"),
            RibbonItemSize.LARGE,
            mi("teSymbol.0", "Degrees %%d  °", null),
            mi("teSymbol.1", "Plus/Minus %%p  ±", null),
            mi("teSymbol.2", "Diameter %%c  Ø", null),
            mi("teSymbol.3", "Centerline  ℄", null),
        ),
        cmd("teField", "Field", ic(CadIcons.FIELD), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.FIELD)
    },
    group(
        "teSpell",
        "Spell Check",
        tgl("teSpellCheck", "Spell Check", RibbonIcon.glyph("\uE8FB"), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE8FB")
    },
    group(
        "teTools",
        "Tools",
        cmd("teFind", "Find &\nReplace", ic(CadIcons.SEARCH), RibbonItemSize.LARGE).also {
            it.shortcut = "Ctrl+R"
        },
    ).also { g ->
        g.icon = ic(CadIcons.SEARCH)
    },
    group(
        "teOptions",
        "Options",
        cmd("teMore", "More", RibbonIcon.glyph("\uE712"), RibbonItemSize.MEDIUM),
        tgl("teRuler", "Ruler", ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.MEDIUM).also {
            it.isChecked = true
        },
        cmd("teUndo", "Undo", ic(CadIcons.UNDO), RibbonItemSize.SMALL),
        cmd("teRedo", "Redo", ic(CadIcons.REDO), RibbonItemSize.SMALL),
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE713")
    },
    group(
        "teClose",
        "Close",
        cmd("closeTextEditor", "Close\nText Editor", ic(CadIcons.CLOSE_EDITOR), RibbonItemSize.LARGE).also {
            it.keyTip = "C"
        },
    ).also { g ->
        g.icon = ic(CadIcons.CLOSE)
    },
).also { t ->
    t.contextualGroupId = "textEditor"
}

/** The [Hatch Creation] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun hatchCreationTab(): RibbonTabModel = tab(
    "hatchCreationTab", "Hatch Creation", "W",
    group(
        "hcBoundaries",
        "Boundaries",
        cmd("hcPickPoints", "Pick Points", ic(CadIcons.BOUNDARY), RibbonItemSize.LARGE),
        cmd("hcSelect", "Select", ic(CadIcons.QUICK_SELECT), RibbonItemSize.MEDIUM),
        cmd("hcRemove", "Remove", ic(CadIcons.ERASE), RibbonItemSize.MEDIUM),
        cmd("hcRecreate", "Recreate", ic(CadIcons.REDO), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.BOUNDARY)
    },
    group(
        "hcPattern",
        "Pattern",
        gallery(
            "hcPatternGallery", "Pattern", ic(CadIcons.HATCH), RibbonItemSize.LARGE,
            gi("SOLID", art(CadArt.HATCH_SOLID), null),
            gi("ANSI31", art(CadArt.HATCH_ANSI31), null),
            gi("ANSI37", art(CadArt.HATCH_ANSI37), null),
            gi("AR-BRSTD", art(CadArt.HATCH_BRICK), null),
            gi("AR-CONC", art(CadArt.HATCH_CONCRETE), null),
            gi("GRAVEL", art(CadArt.HATCH_GRAVEL), null),
            gi("HONEY", art(CadArt.HATCH_HONEY), null),
            gi("NET", art(CadArt.HATCH_NET), null),
            gi("GR_LINEAR", art(CadArt.HATCH_GRADIENT), null),
        ).also { g ->
            g.itemWidth = 50.0
            g.itemHeight = 50.0
            g.minColumns = 3
            g.maxColumns = 6
            g.rows = 1
            g.showLabels = false
        },
    ).also { g ->
        g.icon = ic(CadIcons.HATCH)
        g.reductionOrder = 1
    },
    group(
        "hcProperties",
        "Properties",
        stack(
            true,
            combo("hcType", null, ic(CadIcons.HATCH), 110.0, false, "Pattern", "Solid", "Gradient", "Pattern", "User defined"),
            colorCombo("hcColor"),
            combo("hcBackground", null, ic(CadIcons.GRADIENT), 110.0, false, "None", "None", "ByLayer"),
        ),
        stack(
            true,
            slider("hcTransparency", "Transparency", null, 96.0, 0.0),
            spinner("hcAngle", "Angle", 70.0, 0.0, "°"),
            spinner("hcScale", "Scale", 70.0, 1.0, null),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.COLOR)
    },
    group(
        "hcOrigin",
        "Origin",
        cmd("hcSetOrigin", "Set Origin", ic(CadIcons.MODEL), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.MODEL)
    },
    group(
        "hcOptions",
        "Options",
        tgl("hcAssociative", "Associative", ic(CadIcons.JOIN), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
        tgl("hcAnnotative", "Annotative", ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.LARGE),
        spl(
            "hcMatch",
            "Match Properties",
            ic(CadIcons.MATCH_PROPERTIES),
            RibbonItemSize.LARGE,
            false,
            mi("hcMatch.0", "Use Current Origin", null),
            mi("hcMatch.1", "Use Source Hatch Origin", null),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.MATCH_PROPERTIES)
        g.isDialogLauncherVisible = true
    },
    group(
        "hcClose",
        "Close",
        cmd("closeHatchCreation", "Close\nHatch Creation", ic(CadIcons.CLOSE_EDITOR), RibbonItemSize.LARGE).also {
            it.keyTip = "C"
        },
    ).also { g ->
        g.icon = ic(CadIcons.CLOSE)
    },
).also { t ->
    t.contextualGroupId = "hatchCreation"
}
