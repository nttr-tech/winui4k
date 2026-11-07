package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.common.group
import com.appkitbox.winui4k.sample.ribbon.common.radioItem
import com.appkitbox.winui4k.sample.ribbon.common.tab

// The ribbon tabs of the RibbonSpace CAD demo (samples/RibbonSpace.Demo/Pages/CadPage.xaml, MIT License),
// converted to WinUI4K model building by tools/gen_cad_ribbon.py. The screen text is in the ribbon.* keys of messages.properties (English) and
// messages_ja.properties (Japanese).

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
    "home", Messages.get("ribbon.tab.home"), "H",
    group(
        "draw", Messages.get("ribbon.group.draw"),
        cmd("line", Messages.get("ribbon.line"), ic(CadIcons.LINE), RibbonItemSize.LARGE).also {
            it.keyTip = "LI"
            it.screenTip = richTip(Messages.get("ribbon.line.tip.title"), Messages.get("ribbon.line.tip"), extendedDescription = Messages.get("ribbon.line.tip.extended"), extendedImage = art(CadArt.LINE_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        cmd("polyline", Messages.get("ribbon.polyline"), ic(CadIcons.POLYLINE), RibbonItemSize.LARGE).also {
            it.keyTip = "PL"
            it.screenTip = richTip(Messages.get("ribbon.polyline.tip.title"), Messages.get("ribbon.polyline.tip"), extendedDescription = Messages.get("ribbon.polyline.tip.extended"), extendedImage = art(CadArt.POLYLINE_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        spl(
            "circle",
            Messages.get("ribbon.circle"),
            ic(CadIcons.CIRCLE_CENTER_RADIUS),
            RibbonItemSize.LARGE,
            true,
            mi("circle.0", Messages.get("ribbon.circle.0"), ic(CadIcons.CIRCLE_CENTER_RADIUS)),
            mi("circle.1", Messages.get("ribbon.circle.1"), ic(CadIcons.CIRCLE_TWO_POINT)),
            sep(),
            mi("circle.3", Messages.get("ribbon.circle.3"), ic(CadIcons.CIRCLE_TWO_POINT)),
            mi("circle.4", Messages.get("ribbon.circle.4"), ic(CadIcons.CIRCLE_THREE_POINT)),
            sep(),
            mi("circle.6", Messages.get("ribbon.circle.6"), ic(CadIcons.CIRCLE_TAN_TAN_RADIUS)),
            mi("circle.7", Messages.get("ribbon.circle.7"), ic(CadIcons.CIRCLE_TAN_TAN_TAN)),
        ).also {
            it.keyTip = "CI"
            it.screenTip = richTip(Messages.get("ribbon.circle.tip.title"), Messages.get("ribbon.circle.tip"), extendedDescription = Messages.get("ribbon.circle.tip.extended"), extendedImage = art(CadArt.CIRCLE_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        spl(
            "arc",
            Messages.get("ribbon.arc"),
            ic(CadIcons.ARC),
            RibbonItemSize.LARGE,
            true,
            mi("arc.0", Messages.get("ribbon.arc.0"), ic(CadIcons.ARC)),
            sep(),
            mi("arc.2", Messages.get("ribbon.arc.2"), ic(CadIcons.ARC_START_CENTER_END)),
            mi("arc.3", Messages.get("ribbon.arc.3"), ic(CadIcons.ARC_START_CENTER_END)),
            mi("arc.4", Messages.get("ribbon.arc.4"), ic(CadIcons.ARC_START_CENTER_END)),
            sep(),
            mi("arc.6", Messages.get("ribbon.arc.6"), ic(CadIcons.ARC)),
            mi("arc.7", Messages.get("ribbon.arc.7"), ic(CadIcons.ARC)),
            mi("arc.8", Messages.get("ribbon.arc.8"), ic(CadIcons.ARC)),
            sep(),
            mi("arc.10", Messages.get("ribbon.arc.10"), ic(CadIcons.ARC)),
        ).also {
            it.keyTip = "AC"
            it.screenTip = tip(Messages.get("ribbon.arc.tip"))
        },
        spl(
            "rectangle",
            Messages.get("ribbon.rectangle"),
            ic(CadIcons.RECTANGLE),
            RibbonItemSize.SMALL,
            true,
            mi("rectangle.0", Messages.get("ribbon.rectangle.0"), ic(CadIcons.RECTANGLE)),
            mi("rectangle.1", Messages.get("ribbon.rectangle.1"), ic(CadIcons.POLYGON)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.rectangle.tip"))
        },
        spl(
            "hatch",
            Messages.get("ribbon.hatch"),
            ic(CadIcons.HATCH),
            RibbonItemSize.SMALL,
            true,
            mi("hatch.0", Messages.get("ribbon.hatch.0"), ic(CadIcons.HATCH)),
            mi("hatch.1", Messages.get("ribbon.hatch.1"), ic(CadIcons.GRADIENT)),
            mi("hatch.2", Messages.get("ribbon.hatch.2"), ic(CadIcons.BOUNDARY)),
        ).also {
            it.keyTip = "H"
            it.screenTip = richTip(Messages.get("ribbon.hatch.tip.title"), Messages.get("ribbon.hatch.tip"), extendedDescription = Messages.get("ribbon.hatch.tip.extended"), extendedImage = art(CadArt.HATCH_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        spl(
            "ellipse",
            Messages.get("ribbon.ellipse"),
            ic(CadIcons.ELLIPSE),
            RibbonItemSize.SMALL,
            true,
            mi("ellipse.0", Messages.get("ribbon.ellipse.0"), ic(CadIcons.ELLIPSE)),
            mi("ellipse.1", Messages.get("ribbon.ellipse.1"), ic(CadIcons.ELLIPSE)),
            mi("ellipse.2", Messages.get("ribbon.ellipse.2"), ic(CadIcons.ELLIPTICAL_ARC)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.ellipse.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.LINE)
        g.description = Messages.get("ribbon.group.draw.description")
        g.slideOutItems.add(
            stack(
                false,
                cmd("spline", Messages.get("ribbon.spline"), ic(CadIcons.SPLINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.spline.tip"))
                },
                cmd("constructionLine", Messages.get("ribbon.constructionline"), ic(CadIcons.CONSTRUCTION_LINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.constructionline.tip"))
                },
                cmd("ray", Messages.get("ribbon.ray"), ic(CadIcons.RAY), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.ray.tip"))
                },
                cmd("multiplePoints", Messages.get("ribbon.multiplepoints"), ic(CadIcons.POINT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.multiplepoints.tip"))
                },
                cmd("multiline", Messages.get("ribbon.multiline"), ic(CadIcons.MULTI_LINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.multiline.tip"))
                },
            ),
        )
        g.slideOutItems.add(
            stack(
                false,
                cmd("region", Messages.get("ribbon.region"), ic(CadIcons.REGION), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.region.tip"))
                },
                cmd("revisionCloud", Messages.get("ribbon.revisioncloud"), ic(CadIcons.REVISION_CLOUD), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.revisioncloud.tip"))
                },
                cmd("donut", Messages.get("ribbon.donut"), ic(CadIcons.DONUT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.donut.tip"))
                },
                cmd("boundary", Messages.get("ribbon.boundary"), ic(CadIcons.BOUNDARY), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.boundary.tip"))
                },
                cmd("gradient", Messages.get("ribbon.gradient"), ic(CadIcons.GRADIENT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.gradient.tip"))
                },
            ),
        )
        g.slideOutItems.add(
            cmd("divide", Messages.get("ribbon.divide"), ic(CadIcons.POINT), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.divide.tip"))
            },
        )
    },
    group(
        "modify", Messages.get("ribbon.group.modify"),
        cmd("move", Messages.get("ribbon.move"), ic(CadIcons.MOVE), RibbonItemSize.MEDIUM).also {
            it.keyTip = "MV"
            it.screenTip = tip(Messages.get("ribbon.move.tip"))
        },
        cmd("copy", Messages.get("ribbon.copy"), ic(CadIcons.COPY), RibbonItemSize.MEDIUM).also {
            it.keyTip = "CO"
            it.screenTip = tip(Messages.get("ribbon.copy.tip"))
        },
        cmd("stretch", Messages.get("ribbon.stretch"), ic(CadIcons.STRETCH), RibbonItemSize.MEDIUM).also {
            it.keyTip = "ST"
            it.screenTip = tip(Messages.get("ribbon.stretch.tip"))
        },
        cmd("rotate", Messages.get("ribbon.rotate"), ic(CadIcons.ROTATE), RibbonItemSize.MEDIUM).also {
            it.keyTip = "RO"
            it.screenTip = tip(Messages.get("ribbon.rotate.tip"))
        },
        cmd("mirror", Messages.get("ribbon.mirror"), ic(CadIcons.MIRROR), RibbonItemSize.MEDIUM).also {
            it.keyTip = "MI"
            it.screenTip = tip(Messages.get("ribbon.mirror.tip"))
        },
        cmd("scale", Messages.get("ribbon.scale"), ic(CadIcons.SCALE), RibbonItemSize.MEDIUM).also {
            it.keyTip = "SC"
            it.screenTip = tip(Messages.get("ribbon.scale.tip"))
        },
        spl(
            "trim",
            Messages.get("ribbon.trim"),
            ic(CadIcons.TRIM),
            RibbonItemSize.MEDIUM,
            true,
            mi("trim.0", Messages.get("ribbon.trim.0"), ic(CadIcons.TRIM)),
            mi("trim.1", Messages.get("ribbon.trim.1"), ic(CadIcons.EXTEND)),
        ).also {
            it.keyTip = "TR"
            it.screenTip = richTip(Messages.get("ribbon.trim.tip.title"), Messages.get("ribbon.trim.tip"), extendedDescription = Messages.get("ribbon.trim.tip.extended"), extendedImage = art(CadArt.TRIM_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        spl(
            "fillet",
            Messages.get("ribbon.fillet"),
            ic(CadIcons.FILLET),
            RibbonItemSize.MEDIUM,
            true,
            mi("fillet.0", Messages.get("ribbon.fillet.0"), ic(CadIcons.FILLET)),
            mi("fillet.1", Messages.get("ribbon.fillet.1"), ic(CadIcons.CHAMFER)),
            mi("fillet.2", Messages.get("ribbon.fillet.2"), ic(CadIcons.BLEND)),
        ).also {
            it.keyTip = "F"
            it.screenTip = richTip(Messages.get("ribbon.fillet.tip.title"), Messages.get("ribbon.fillet.tip"), extendedDescription = Messages.get("ribbon.fillet.tip.extended"), extendedImage = art(CadArt.FILLET_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        spl(
            "array",
            Messages.get("ribbon.array"),
            ic(CadIcons.ARRAY_RECTANGULAR),
            RibbonItemSize.MEDIUM,
            true,
            mi("array.0", Messages.get("ribbon.array.0"), ic(CadIcons.ARRAY_RECTANGULAR)),
            mi("array.1", Messages.get("ribbon.array.1"), ic(CadIcons.ARRAY_PATH)),
            mi("array.2", Messages.get("ribbon.array.2"), ic(CadIcons.ARRAY_POLAR)),
        ).also {
            it.keyTip = "AR"
            it.screenTip = richTip(Messages.get("ribbon.array.tip.title"), Messages.get("ribbon.array.tip"), extendedDescription = Messages.get("ribbon.array.tip.extended"), extendedImage = art(CadArt.ARRAY_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
        cmd("erase", Messages.get("ribbon.erase"), ic(CadIcons.ERASE), RibbonItemSize.SMALL).also {
            it.keyTip = "E"
            it.screenTip = tip(Messages.get("ribbon.erase.tip"))
        },
        cmd("explode", Messages.get("ribbon.explode"), ic(CadIcons.EXPLODE), RibbonItemSize.SMALL).also {
            it.keyTip = "X"
            it.screenTip = tip(Messages.get("ribbon.explode.tip"))
        },
        cmd("offset", Messages.get("ribbon.offset"), ic(CadIcons.OFFSET), RibbonItemSize.SMALL).also {
            it.keyTip = "O"
            it.screenTip = richTip(Messages.get("ribbon.offset.tip.title"), Messages.get("ribbon.offset.tip"), extendedDescription = Messages.get("ribbon.offset.tip.extended"), extendedImage = art(CadArt.OFFSET_HELP), helpText = Messages.get("ribbon.tip.help"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.MOVE)
        g.description = Messages.get("ribbon.group.modify.description")
        g.slideOutItems.add(
            stack(
                false,
                cmd("breakAtPoint", Messages.get("ribbon.breakatpoint"), ic(CadIcons.BREAK_AT_POINT), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.breakatpoint.tip"))
                },
                cmd("break", Messages.get("ribbon.break"), ic(CadIcons.BREAK), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.break.tip"))
                },
                cmd("join", Messages.get("ribbon.join"), ic(CadIcons.JOIN), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.join.tip"))
                },
                cmd("chamfer", Messages.get("ribbon.chamfer"), ic(CadIcons.CHAMFER), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.chamfer.tip"))
                },
                cmd("blend", Messages.get("ribbon.blend"), ic(CadIcons.BLEND), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.blend.tip"))
                },
                cmd("lengthen", Messages.get("ribbon.lengthen"), ic(CadIcons.LENGTHEN), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.lengthen.tip"))
                },
            ),
        )
        g.slideOutItems.add(
            stack(
                false,
                cmd("align", Messages.get("ribbon.align"), ic(CadIcons.ALIGN), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.align.tip"))
                },
                cmd("reverse", Messages.get("ribbon.reverse"), ic(CadIcons.REVERSE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.reverse.tip"))
                },
                cmd("editPolyline", Messages.get("ribbon.editpolyline"), ic(CadIcons.EDIT_POLYLINE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.editpolyline.tip"))
                },
                cmd("extend", Messages.get("ribbon.extend"), ic(CadIcons.EXTEND), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.extend.tip"))
                },
                cmd("arrayPolar", Messages.get("ribbon.arraypolar"), ic(CadIcons.ARRAY_POLAR), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.arraypolar.tip"))
                },
                cmd("arrayPath", Messages.get("ribbon.arraypath"), ic(CadIcons.ARRAY_PATH), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.arraypath.tip"))
                },
            ),
        )
    },
    group(
        "annotation",
        Messages.get("ribbon.group.annotation"),
        spl(
            "text",
            Messages.get("ribbon.text"),
            ic(CadIcons.MULTILINE_TEXT),
            RibbonItemSize.LARGE,
            true,
            mi("text.0", Messages.get("ribbon.text.0"), ic(CadIcons.MULTILINE_TEXT)),
            mi("text.1", Messages.get("ribbon.text.1"), ic(CadIcons.SINGLE_LINE_TEXT)),
        ).also {
            it.keyTip = "TX"
            it.screenTip = tip(Messages.get("ribbon.text.tip"))
        },
        spl(
            "dimension",
            Messages.get("ribbon.dimension"),
            ic(CadIcons.DIMENSION_LINEAR),
            RibbonItemSize.LARGE,
            true,
            mi("dimension.0", Messages.get("ribbon.dimension.0"), ic(CadIcons.DIMENSION_LINEAR)),
            mi("dimension.1", Messages.get("ribbon.dimension.1"), ic(CadIcons.DIMENSION_ALIGNED)),
            mi("dimension.2", Messages.get("ribbon.dimension.2"), ic(CadIcons.DIMENSION_ANGULAR)),
            mi("dimension.3", Messages.get("ribbon.dimension.3"), ic(CadIcons.DIMENSION_RADIUS)),
            mi("dimension.4", Messages.get("ribbon.dimension.4"), ic(CadIcons.DIMENSION_DIAMETER)),
        ).also {
            it.keyTip = "D"
            it.screenTip = tip(Messages.get("ribbon.dimension.tip"))
        },
        spl(
            "leader",
            Messages.get("ribbon.leader"),
            ic(CadIcons.MULTILEADER),
            RibbonItemSize.MEDIUM,
            true,
            mi("leader.0", Messages.get("ribbon.leader.0"), ic(CadIcons.MULTILEADER)),
            mi("leader.1", Messages.get("ribbon.leader.1"), ic(CadIcons.MULTILEADER)),
            mi("leader.2", Messages.get("ribbon.leader.2"), ic(CadIcons.MULTILEADER)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.leader.tip"))
        },
        cmd("table", Messages.get("ribbon.table"), ic(CadIcons.TABLE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.table.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.MULTILINE_TEXT)
        g.description = Messages.get("ribbon.group.annotation.description")
        g.reductionOrder = 5
        g.slideOutItems.add(
            cmd("field", Messages.get("ribbon.field"), ic(CadIcons.FIELD), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.field.tip"))
            },
        )
        g.slideOutItems.add(
            combo("textStyle", Messages.get("ribbon.textstyle.label"), ic(CadIcons.TEXT_STYLE), 150.0, false, "Standard", "Standard", Messages.get("ribbon.textstyle.item.1"), Messages.get("ribbon.textstyle.item.2"), Messages.get("ribbon.textstyle.item.3")),
        )
        g.slideOutItems.add(
            combo("dimStyle", Messages.get("ribbon.dimstyle.label"), ic(CadIcons.DIMENSION_STYLE), 150.0, false, "ISO-25", "ISO-25", Messages.get("ribbon.dimstyle.item.1"), Messages.get("ribbon.dimstyle.item.2"), "Standard"),
        )
        g.slideOutItems.add(
            combo("mleaderStyle", Messages.get("ribbon.mleaderstyle.label"), ic(CadIcons.MULTILEADER), 150.0, false, "Standard", "Standard", Messages.get("ribbon.mleaderstyle.item.1")),
        )
        g.slideOutItems.add(
            combo("tableStyle", Messages.get("ribbon.tablestyle.label"), ic(CadIcons.TABLE), 150.0, false, "Standard", "Standard", Messages.get("ribbon.tablestyle.item.1")),
        )
    },
    group(
        "layers",
        Messages.get("ribbon.group.layers"),
        cmd("layerProperties", Messages.get("ribbon.layerproperties"), ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.LARGE).also {
            it.keyTip = "LA"
            it.screenTip = tip(Messages.get("ribbon.layerproperties.tip"))
        },
        stack(
            true,
            stack(
                false,
                cmd("layerOff", Messages.get("ribbon.layeroff"), ic(CadIcons.LAYER_OFF), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layeroff.tip"))
                },
                cmd("layerIsolate", Messages.get("ribbon.layerisolate"), ic(CadIcons.LAYER_ISOLATE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerisolate.tip"))
                },
                cmd("layerFreeze", Messages.get("ribbon.layerfreeze"), ic(CadIcons.LAYER_FREEZE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerfreeze.tip"))
                },
                cmd("layerLock", Messages.get("ribbon.layerlock"), ic(CadIcons.LAYER_LOCK), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerlock.tip"))
                },
                cmd("layerUnisolate", Messages.get("ribbon.layerunisolate"), ic(CadIcons.LAYER_UNISOLATE), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerunisolate.tip"))
                },
                cmd("layerPrevious", Messages.get("ribbon.layerprevious"), ic(CadIcons.LAYER_PREVIOUS), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerprevious.tip"))
                },
            ),
            layerCombo(),
            stack(
                false,
                cmd("makeCurrent", Messages.get("ribbon.makecurrent"), ic(CadIcons.MAKE_CURRENT), RibbonItemSize.MEDIUM).also {
                    it.screenTip = tip(Messages.get("ribbon.makecurrent.tip"))
                },
                cmd("matchLayer", Messages.get("ribbon.matchlayer"), ic(CadIcons.MATCH_LAYER), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.matchlayer.tip"))
                },
            ),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.LAYER_PROPERTIES)
        g.description = Messages.get("ribbon.group.layers.description")
        g.reductionOrder = 1
        g.slideOutItems.add(
            combo("layerState", Messages.get("ribbon.layerstate.label"), null, 170.0, false, Messages.get("ribbon.layerstate.item.0"), Messages.get("ribbon.layerstate.item.0"), Messages.get("ribbon.layerstate.item.1"), Messages.get("ribbon.layerstate.item.2"), Messages.get("ribbon.layerstate.item.3"), Messages.get("ribbon.layerstate.item.4")),
        )
        g.slideOutItems.add(
            stack(
                false,
                cmd("layerOnAll", Messages.get("ribbon.layeronall"), ic(CadIcons.LAYER_ON), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layeronall.tip"))
                },
                cmd("layerThawAll", Messages.get("ribbon.layerthawall"), ic(CadIcons.LAYER_THAW), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerthawall.tip"))
                },
                cmd("layerUnlock", Messages.get("ribbon.layerunlock"), ic(CadIcons.LAYER_UNLOCK), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layerunlock.tip"))
                },
                cmd("layerMatch2", Messages.get("ribbon.layermatch2"), ic(CadIcons.MATCH_LAYER), RibbonItemSize.SMALL).also {
                    it.screenTip = tip(Messages.get("ribbon.layermatch2.tip"))
                },
            ),
        )
        g.slideOutItems.add(
            cmd("layerWalk", Messages.get("ribbon.layerwalk"), ic(CadIcons.LAYER_ISOLATE), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.layerwalk.tip"))
            },
        )
    },
    group(
        "block",
        Messages.get("ribbon.group.block"),
        gallery(
            "insertBlock", Messages.get("ribbon.insertblock"), ic(CadIcons.INSERT_BLOCK), RibbonItemSize.LARGE,
            gi(Messages.get("ribbon.insertblock.item.0"), art(CadArt.BLOCK_DOOR), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.1"), art(CadArt.BLOCK_WINDOW), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.2"), art(CadArt.BLOCK_CHAIR), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.3"), art(CadArt.BLOCK_TABLE), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.4"), art(CadArt.BLOCK_BED), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.5"), art(CadArt.BLOCK_TOILET), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.6"), art(CadArt.BLOCK_TREE), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.7"), art(CadArt.BLOCK_NORTH_ARROW), Messages.get("ribbon.insertblock.category.0")),
            gi(Messages.get("ribbon.insertblock.item.8"), art(CadArt.BLOCK_BOLT), Messages.get("ribbon.insertblock.category.1")),
            gi(Messages.get("ribbon.insertblock.item.9"), art(CadArt.BLOCK_NUT), Messages.get("ribbon.insertblock.category.1")),
            gi(Messages.get("ribbon.insertblock.item.10"), art(CadArt.BLOCK_SECTION), Messages.get("ribbon.insertblock.category.1")),
            gi(Messages.get("ribbon.insertblock.item.11"), art(CadArt.BLOCK_TITLE), Messages.get("ribbon.insertblock.category.1")),
        ).also { g ->
            g.itemWidth = 84.0
            g.itemHeight = 78.0
            g.dropDownColumns = 4
            g.isDropDownOnly = true
            g.keyTip = "I"
            g.screenTip = tip(Messages.get("ribbon.insertblock.tip"))
            g.menuItems.add(mi("insertBlock.footer0", Messages.get("ribbon.insertblock.footer0"), ic(CadIcons.RECENT)))
            g.menuItems.add(mi("insertBlock.footer1", Messages.get("ribbon.insertblock.footer1"), ic(CadIcons.OPEN)))
        },
        cmd("createBlock", Messages.get("ribbon.createblock"), ic(CadIcons.CREATE_BLOCK), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.createblock.tip"))
        },
        cmd("editBlock", Messages.get("ribbon.editblock"), ic(CadIcons.EDIT_BLOCK), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.editblock.tip"))
        },
        spl(
            "editAttributes",
            Messages.get("ribbon.editattributes"),
            ic(CadIcons.EDIT_ATTRIBUTE),
            RibbonItemSize.MEDIUM,
            true,
            mi("editAttributes.0", Messages.get("ribbon.editattributes.0"), ic(CadIcons.EDIT_ATTRIBUTE)),
            mi("editAttributes.1", Messages.get("ribbon.editattributes.1"), ic(CadIcons.EDIT_ATTRIBUTE)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.editattributes.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.INSERT_BLOCK)
        g.description = Messages.get("ribbon.group.block.description")
        g.reductionOrder = 4
        g.slideOutItems.add(
            cmd("defineAttributes", Messages.get("ribbon.defineattributes"), ic(CadIcons.DEFINE_ATTRIBUTES), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.defineattributes.tip"))
            },
        )
        g.slideOutItems.add(
            cmd("manageAttributes", Messages.get("ribbon.manageattributes"), ic(CadIcons.EDIT_ATTRIBUTE), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.manageattributes.tip"))
            },
        )
        g.slideOutItems.add(
            cmd("synchronize", Messages.get("ribbon.synchronize"), ic(CadIcons.EDIT_BLOCK), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.synchronize.tip"))
            },
        )
    },
    group(
        "properties",
        Messages.get("ribbon.group.properties"),
        cmd("matchProperties", Messages.get("ribbon.matchproperties"), ic(CadIcons.MATCH_PROPERTIES), RibbonItemSize.LARGE).also {
            it.keyTip = "MA"
            it.screenTip = tip(Messages.get("ribbon.matchproperties.tip"))
        },
        stack(
            true,
            colorCombo("objectColor"),
            lineweightCombo("objectLineweight"),
            linetypeCombo("objectLinetype"),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.MATCH_PROPERTIES)
        g.description = Messages.get("ribbon.group.properties.description")
        g.reductionOrder = 2
        g.isDialogLauncherVisible = true
        g.slideOutItems.add(
            slider("transparency", Messages.get("ribbon.transparency"), ic(CadIcons.TRANSPARENCY), 120.0, 0.0),
        )
        g.slideOutItems.add(
            cmd("list", Messages.get("ribbon.list"), ic(CadIcons.PROPERTIES_PALETTE), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.list.tip"))
            },
        )
        g.slideOutItems.add(
            combo("plotStyle", Messages.get("ribbon.plotstyle.label"), null, 120.0, false, "ByColor", "ByColor", "ByLayer"),
        )
    },
    group(
        "groups",
        Messages.get("ribbon.group.groups"),
        cmd("group", Messages.get("ribbon.group"), ic(CadIcons.GROUP), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.group.tip"))
        },
        cmd("ungroup", Messages.get("ribbon.ungroup"), ic(CadIcons.UNGROUP), RibbonItemSize.SMALL).also {
            it.screenTip = tip(Messages.get("ribbon.ungroup.tip"))
        },
        cmd("groupEdit", Messages.get("ribbon.groupedit"), ic(CadIcons.GROUP_EDIT), RibbonItemSize.SMALL).also {
            it.screenTip = tip(Messages.get("ribbon.groupedit.tip"))
        },
        tgl("groupSelection", Messages.get("ribbon.groupselection"), ic(CadIcons.GROUP), RibbonItemSize.SMALL).also {
            it.screenTip = tip(Messages.get("ribbon.groupselection.tip"))
            it.isChecked = true
        },
    ).also { g ->
        g.icon = ic(CadIcons.GROUP)
        g.reductionOrder = 6
    },
    group(
        "utilities",
        Messages.get("ribbon.group.utilities"),
        spl(
            "measure",
            Messages.get("ribbon.measure"),
            ic(CadIcons.MEASURE_DISTANCE),
            RibbonItemSize.LARGE,
            true,
            mi("measure.0", Messages.get("ribbon.measure.0"), ic(CadIcons.MEASURE_DISTANCE)),
            mi("measure.1", Messages.get("ribbon.measure.1"), ic(CadIcons.MEASURE_DISTANCE)),
            mi("measure.2", Messages.get("ribbon.measure.2"), ic(CadIcons.MEASURE_ANGLE)),
            mi("measure.3", Messages.get("ribbon.measure.3"), ic(CadIcons.MEASURE_AREA)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.measure.tip"))
        },
        cmd("quickSelect", Messages.get("ribbon.quickselect"), ic(CadIcons.QUICK_SELECT), RibbonItemSize.SMALL).also {
            it.screenTip = tip(Messages.get("ribbon.quickselect.tip"))
        },
        cmd("selectAll", Messages.get("ribbon.selectall"), ic(CadIcons.SELECT_ALL), RibbonItemSize.SMALL).also {
            it.shortcut = "Ctrl+A"
            it.screenTip = tip(Messages.get("ribbon.selectall.tip"))
        },
        cmd("idPoint", Messages.get("ribbon.idpoint"), ic(CadIcons.ID_POINT), RibbonItemSize.SMALL).also {
            it.screenTip = tip(Messages.get("ribbon.idpoint.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.MEASURE_DISTANCE)
        g.reductionOrder = 7
        g.slideOutItems.add(
            cmd("calculator", Messages.get("ribbon.calculator"), ic(CadIcons.CALCULATOR), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.calculator.tip"))
            },
        )
        g.slideOutItems.add(
            cmd("pointStyle", Messages.get("ribbon.pointstyle"), ic(CadIcons.POINT), RibbonItemSize.MEDIUM).also {
                it.screenTip = tip(Messages.get("ribbon.pointstyle.tip"))
            },
        )
    },
    group(
        "clipboard",
        Messages.get("ribbon.group.clipboard"),
        spl(
            "paste",
            Messages.get("ribbon.paste"),
            ic(CadIcons.PASTE),
            RibbonItemSize.LARGE,
            false,
            mi("paste.0", Messages.get("ribbon.paste.0"), ic(CadIcons.PASTE)),
            mi("paste.1", Messages.get("ribbon.paste.1"), ic(CadIcons.INSERT_BLOCK)),
            mi("paste.2", Messages.get("ribbon.paste.2"), ic(CadIcons.PASTE)),
            sep(),
            mi("paste.4", Messages.get("ribbon.paste.4"), null),
        ).also {
            it.shortcut = "Ctrl+V"
            it.screenTip = tip(Messages.get("ribbon.paste.tip"))
        },
        cmd("copyClip", Messages.get("ribbon.copyclip"), ic(CadIcons.COPY_CLIP), RibbonItemSize.SMALL).also {
            it.shortcut = "Ctrl+C"
            it.screenTip = tip(Messages.get("ribbon.copyclip.tip"))
        },
        cmd("cutClip", Messages.get("ribbon.cutclip"), ic(CadIcons.CUT), RibbonItemSize.SMALL).also {
            it.shortcut = "Ctrl+X"
            it.screenTip = tip(Messages.get("ribbon.cutclip.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.PASTE)
        g.reductionOrder = 8
    },
    group(
        "homeView",
        Messages.get("ribbon.group.homeview"),
        ddn(
            "baseView",
            Messages.get("ribbon.baseview"),
            ic(CadIcons.VIEWPORTS),
            RibbonItemSize.LARGE,
            mi("baseView.0", Messages.get("ribbon.baseview.0"), ic(CadIcons.MODEL)),
            mi("baseView.1", Messages.get("ribbon.baseview.1"), ic(CadIcons.BOX)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.baseview.tip"))
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
    Messages.get("ribbon.tab.solid"),
    "SO",
    group(
        "primitive",
        Messages.get("ribbon.group.primitive"),
        spl(
            "box",
            Messages.get("ribbon.box"),
            ic(CadIcons.BOX),
            RibbonItemSize.LARGE,
            true,
            mi("box.0", Messages.get("ribbon.box.0"), ic(CadIcons.BOX)),
            mi("box.1", Messages.get("ribbon.box.1"), ic(CadIcons.CYLINDER)),
            mi("box.2", Messages.get("ribbon.box.2"), ic(CadIcons.CONE)),
            mi("box.3", Messages.get("ribbon.box.3"), ic(CadIcons.SPHERE)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.box.tip"))
        },
        cmd("cylinder", Messages.get("ribbon.cylinder"), ic(CadIcons.CYLINDER), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.cylinder.tip"))
        },
        cmd("sphere", Messages.get("ribbon.sphere"), ic(CadIcons.SPHERE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.sphere.tip"))
        },
        cmd("cone", Messages.get("ribbon.cone"), ic(CadIcons.CONE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.cone.tip"))
        },
        cmd("polysolid", Messages.get("ribbon.polysolid"), ic(CadIcons.MULTI_LINE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.polysolid.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.BOX)
    },
    group(
        "solidCreate",
        Messages.get("ribbon.group.solidcreate"),
        cmd("extrude", Messages.get("ribbon.extrude"), ic(CadIcons.EXTRUDE), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.extrude.tip"))
        },
        cmd("presspull", Messages.get("ribbon.presspull"), ic(CadIcons.PRESSPULL), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.presspull.tip"))
        },
        cmd("revolve", Messages.get("ribbon.revolve"), ic(CadIcons.REVOLVE), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.revolve.tip"))
        },
        cmd("sweep", Messages.get("ribbon.sweep"), ic(CadIcons.SWEEP), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.sweep.tip"))
        },
        cmd("loft", Messages.get("ribbon.loft"), ic(CadIcons.LOFT), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.loft.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.EXTRUDE)
    },
    group(
        "boolean",
        Messages.get("ribbon.group.boolean"),
        cmd("union", Messages.get("ribbon.union"), ic(CadIcons.UNION), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.union.tip"))
        },
        cmd("subtract", Messages.get("ribbon.subtract"), ic(CadIcons.SUBTRACT), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.subtract.tip"))
        },
        cmd("intersect", Messages.get("ribbon.intersect"), ic(CadIcons.INTERSECT), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.intersect.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.UNION)
    },
    group(
        "solidEditing",
        Messages.get("ribbon.group.solidediting"),
        cmd("filletEdge", Messages.get("ribbon.filletedge"), ic(CadIcons.FILLET), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.filletedge.tip"))
        },
        cmd("chamferEdge", Messages.get("ribbon.chamferedge"), ic(CadIcons.CHAMFER), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.chamferedge.tip"))
        },
        cmd("slice", Messages.get("ribbon.slice"), ic(CadIcons.BREAK), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.slice.tip"))
        },
        cmd("shell", Messages.get("ribbon.shell"), ic(CadIcons.OFFSET), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.shell.tip"))
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
    Messages.get("ribbon.tab.surface"),
    "SU",
    group(
        "surfaceCreate",
        Messages.get("ribbon.group.surfacecreate"),
        cmd("networkSurface", Messages.get("ribbon.networksurface"), ic(CadIcons.SURFACE_NETWORK), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.networksurface.tip"))
        },
        cmd("planarSurface", Messages.get("ribbon.planarsurface"), ic(CadIcons.SURFACE_PLANAR), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.planarsurface.tip"))
        },
        cmd("loftSurface", Messages.get("ribbon.loftsurface"), ic(CadIcons.LOFT), RibbonItemSize.MEDIUM),
        cmd("sweepSurface", Messages.get("ribbon.sweepsurface"), ic(CadIcons.SWEEP), RibbonItemSize.MEDIUM),
        cmd("revolveSurface", Messages.get("ribbon.revolvesurface"), ic(CadIcons.REVOLVE), RibbonItemSize.MEDIUM),
        cmd("extrudeSurface", Messages.get("ribbon.extrudesurface"), ic(CadIcons.EXTRUDE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.LOFT)
    },
    group(
        "surfaceEdit",
        Messages.get("ribbon.group.surfaceedit"),
        cmd("surfaceFillet", Messages.get("ribbon.surfacefillet"), ic(CadIcons.FILLET), RibbonItemSize.MEDIUM),
        cmd("surfaceTrim", Messages.get("ribbon.surfacetrim"), ic(CadIcons.TRIM), RibbonItemSize.MEDIUM),
        cmd("surfaceExtend", Messages.get("ribbon.surfaceextend"), ic(CadIcons.EXTEND), RibbonItemSize.MEDIUM),
        cmd("surfaceBlend", Messages.get("ribbon.surfaceblend"), ic(CadIcons.BLEND), RibbonItemSize.MEDIUM),
        cmd("surfacePatch", Messages.get("ribbon.surfacepatch"), ic(CadIcons.SURFACE_PLANAR), RibbonItemSize.MEDIUM),
        cmd("surfaceOffset", Messages.get("ribbon.surfaceoffset"), ic(CadIcons.OFFSET), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.FILLET)
    },
    group(
        "controlVertices",
        Messages.get("ribbon.group.controlvertices"),
        tgl("showCv", Messages.get("ribbon.showcv"), ic(CadIcons.SPLINE), RibbonItemSize.LARGE),
        cmd("rebuild", Messages.get("ribbon.rebuild"), ic(CadIcons.SURFACE_NETWORK), RibbonItemSize.MEDIUM),
        cmd("addCv", Messages.get("ribbon.addcv"), ic(CadIcons.POINT), RibbonItemSize.MEDIUM),
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
    Messages.get("ribbon.tab.mesh"),
    "E",
    group(
        "meshPrimitives",
        Messages.get("ribbon.group.meshprimitives"),
        spl(
            "meshBox",
            Messages.get("ribbon.meshbox"),
            ic(CadIcons.MESH_BOX),
            RibbonItemSize.LARGE,
            true,
            mi("meshBox.0", Messages.get("ribbon.meshbox.0"), ic(CadIcons.MESH_BOX)),
            mi("meshBox.1", Messages.get("ribbon.meshbox.1"), ic(CadIcons.CYLINDER)),
            mi("meshBox.2", Messages.get("ribbon.meshbox.2"), ic(CadIcons.SPHERE)),
        ).also {
            it.screenTip = tip(Messages.get("ribbon.meshbox.tip"))
        },
        cmd("revolvedSurface", Messages.get("ribbon.revolvedsurface"), ic(CadIcons.REVOLVE), RibbonItemSize.MEDIUM),
        cmd("edgeSurface", Messages.get("ribbon.edgesurface"), ic(CadIcons.SURFACE_NETWORK), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MESH_BOX)
    },
    group(
        "meshEdit",
        Messages.get("ribbon.group.meshedit"),
        cmd("smoothObject", Messages.get("ribbon.smoothobject"), ic(CadIcons.MESH_SMOOTH), RibbonItemSize.LARGE),
        cmd("smoothMore", Messages.get("ribbon.smoothmore"), ic(CadIcons.SPHERE), RibbonItemSize.MEDIUM),
        cmd("smoothLess", Messages.get("ribbon.smoothless"), ic(CadIcons.MESH_BOX), RibbonItemSize.MEDIUM),
        cmd("refineMesh", Messages.get("ribbon.refinemesh"), ic(CadIcons.GRID), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MESH_SMOOTH)
    },
    group(
        "convertMesh",
        Messages.get("ribbon.group.convertmesh"),
        cmd("convertSolid", Messages.get("ribbon.convertsolid"), ic(CadIcons.BOX), RibbonItemSize.LARGE),
        cmd("convertSurface", Messages.get("ribbon.convertsurface"), ic(CadIcons.SURFACE_PLANAR), RibbonItemSize.LARGE),
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
    Messages.get("ribbon.tab.visualize"),
    "Z",
    group(
        "visualStyles",
        Messages.get("ribbon.group.visualstyles"),
        gallery(
            "visualStyleGallery", Messages.get("ribbon.visualstylegallery"), art(CadArt.STYLE_SHADED), RibbonItemSize.LARGE,
            gi(Messages.get("ribbon.visualstylegallery.item.0"), art(CadArt.STYLE2_D_WIREFRAME), null),
            gi(Messages.get("ribbon.visualstylegallery.item.1"), art(CadArt.STYLE_HIDDEN), null),
            gi(Messages.get("ribbon.visualstylegallery.item.2"), art(CadArt.STYLE_SHADED), null),
            gi(Messages.get("ribbon.visualstylegallery.item.3"), art(CadArt.STYLE_REALISTIC), null),
            gi(Messages.get("ribbon.visualstylegallery.item.4"), art(CadArt.STYLE_CONCEPTUAL), null),
            gi(Messages.get("ribbon.visualstylegallery.item.5"), art(CadArt.STYLE_X_RAY), null),
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
        Messages.get("ribbon.group.lights"),
        ddn(
            "createLight",
            Messages.get("ribbon.createlight"),
            ic(CadIcons.LIGHT),
            RibbonItemSize.LARGE,
            mi("createLight.0", Messages.get("ribbon.createlight.0"), ic(CadIcons.LIGHT)),
            mi("createLight.1", Messages.get("ribbon.createlight.1"), ic(CadIcons.LIGHT)),
            mi("createLight.2", Messages.get("ribbon.createlight.2"), ic(CadIcons.SUN)),
        ),
        tgl("sunStatus", Messages.get("ribbon.sunstatus"), ic(CadIcons.SUN), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.LIGHT)
    },
    group(
        "materials",
        Messages.get("ribbon.group.materials"),
        cmd("materialsBrowser", Messages.get("ribbon.materialsbrowser"), ic(CadIcons.MATERIAL), RibbonItemSize.LARGE),
        cmd("attachByLayer", Messages.get("ribbon.attachbylayer"), ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM),
        cmd("removeMaterials", Messages.get("ribbon.removematerials"), ic(CadIcons.ERASE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MATERIAL)
    },
    group(
        "render",
        Messages.get("ribbon.group.render"),
        cmd("renderButton", Messages.get("ribbon.renderbutton"), ic(CadIcons.RENDER), RibbonItemSize.LARGE),
        combo("renderPreset", Messages.get("ribbon.renderpreset.label"), null, 120.0, false, Messages.get("ribbon.renderpreset.item.1"), Messages.get("ribbon.renderpreset.item.0"), Messages.get("ribbon.renderpreset.item.1"), Messages.get("ribbon.renderpreset.item.2"), Messages.get("ribbon.renderpreset.item.3")),
        combo("renderSize", Messages.get("ribbon.rendersize.label"), null, 120.0, false, "800 x 600 px", "640 x 480 px", "800 x 600 px", "1920 x 1080 px"),
    ).also { g ->
        g.icon = ic(CadIcons.RENDER)
        g.isDialogLauncherVisible = true
    },
    group(
        "camera",
        Messages.get("ribbon.group.camera"),
        cmd("createCamera", Messages.get("ribbon.createcamera"), ic(CadIcons.CAMERA), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.CAMERA)
    },
).also { t ->
    t.isVisible = false
}

/** The [Insert] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun insertTab(): RibbonTabModel = tab(
    "insert", Messages.get("ribbon.tab.insert"), "I",
    group(
        "insertBlockGroup",
        Messages.get("ribbon.group.insertblockgroup"),
        cmd("insertBlock2", Messages.get("ribbon.insertblock2"), ic(CadIcons.INSERT_BLOCK), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.insertblock2.tip"))
        },
        cmd("editAttributes2", Messages.get("ribbon.editattributes2"), ic(CadIcons.EDIT_ATTRIBUTE), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.INSERT_BLOCK)
    },
    group(
        "blockDefinition",
        Messages.get("ribbon.group.blockdefinition"),
        cmd("createBlock2", Messages.get("ribbon.createblock2"), ic(CadIcons.CREATE_BLOCK), RibbonItemSize.LARGE),
        cmd("defineAttributes2", Messages.get("ribbon.defineattributes2"), ic(CadIcons.DEFINE_ATTRIBUTES), RibbonItemSize.LARGE),
        cmd("manageAttributes2", Messages.get("ribbon.manageattributes2"), ic(CadIcons.EDIT_ATTRIBUTE), RibbonItemSize.MEDIUM),
        cmd("blockEditor", Messages.get("ribbon.blockeditor"), ic(CadIcons.EDIT_BLOCK), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.CREATE_BLOCK)
    },
    group(
        "reference",
        Messages.get("ribbon.group.reference"),
        cmd("attach", Messages.get("ribbon.attach"), ic(CadIcons.ATTACH), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.attach.tip"))
        },
        cmd("clip", Messages.get("ribbon.clip"), ic(CadIcons.CLIP), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.clip.tip"))
        },
        cmd("adjust", Messages.get("ribbon.adjust"), ic(CadIcons.TRANSPARENCY), RibbonItemSize.MEDIUM),
        cmd("underlayLayers", Messages.get("ribbon.underlaylayers"), ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM),
        combo("frames", Messages.get("ribbon.frames.label"), null, 120.0, false, Messages.get("ribbon.frames.item.0"), Messages.get("ribbon.frames.item.0"), Messages.get("ribbon.frames.item.1"), Messages.get("ribbon.frames.item.2"), Messages.get("ribbon.frames.item.3")),
    ).also { g ->
        g.icon = ic(CadIcons.ATTACH)
        g.isDialogLauncherVisible = true
    },
    group(
        "importGroup",
        Messages.get("ribbon.group.importgroup"),
        cmd("pdfImport", Messages.get("ribbon.pdfimport"), ic(CadIcons.IMPORT_PDF), RibbonItemSize.LARGE),
        cmd("importFile", Messages.get("ribbon.importfile"), ic(CadIcons.OPEN), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.IMPORT_PDF)
    },
    group(
        "data",
        Messages.get("ribbon.group.data"),
        cmd("fieldInsert", Messages.get("ribbon.fieldinsert"), ic(CadIcons.FIELD), RibbonItemSize.LARGE),
        cmd("updateFields", Messages.get("ribbon.updatefields"), ic(CadIcons.REDO), RibbonItemSize.MEDIUM),
        cmd("hyperlink", Messages.get("ribbon.hyperlink"), ic(CadIcons.SHARE), RibbonItemSize.MEDIUM),
        cmd("oleObject", Messages.get("ribbon.oleobject"), ic(CadIcons.TABLE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.FIELD)
    },
    group(
        "linking",
        Messages.get("ribbon.group.linking"),
        cmd("dataExtraction", Messages.get("ribbon.dataextraction"), ic(CadIcons.DATA_EXTRACTION), RibbonItemSize.LARGE),
        cmd("dataLink", Messages.get("ribbon.datalink"), ic(CadIcons.TABLE), RibbonItemSize.MEDIUM),
        cmd("downloadSource", Messages.get("ribbon.downloadsource"), ic(CadIcons.OPEN), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.DATA_EXTRACTION)
    },
)

/** The [Annotate] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun annotateTab(): RibbonTabModel = tab(
    "annotate", Messages.get("ribbon.tab.annotate"), "A",
    group(
        "annotateText",
        Messages.get("ribbon.group.annotatetext"),
        spl(
            "annotateMText",
            Messages.get("ribbon.annotatemtext"),
            ic(CadIcons.MULTILINE_TEXT),
            RibbonItemSize.LARGE,
            true,
            mi("annotateMText.0", Messages.get("ribbon.annotatemtext.0"), ic(CadIcons.MULTILINE_TEXT)),
            mi("annotateMText.1", Messages.get("ribbon.annotatemtext.1"), ic(CadIcons.SINGLE_LINE_TEXT)),
        ),
        cmd("checkSpelling", Messages.get("ribbon.checkspelling"), RibbonIcon.glyph("\uE8FB"), RibbonItemSize.LARGE),
        stack(
            true,
            combo("annotateTextStyle", null, ic(CadIcons.TEXT_STYLE), 140.0, false, "Standard", "Standard", Messages.get("ribbon.annotatetextstyle.item.1")),
            textBox("findText", ic(CadIcons.SEARCH), 140.0),
            combo("textHeight", null, ic(CadIcons.SINGLE_LINE_TEXT), 140.0, true, "2.5", "1.8", "2.5", "3.5", "5"),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.MULTILINE_TEXT)
        g.isDialogLauncherVisible = true
    },
    group(
        "dimensions", Messages.get("ribbon.group.dimensions"),
        spl(
            "annotateDimension",
            Messages.get("ribbon.annotatedimension"),
            ic(CadIcons.DIMENSION_LINEAR),
            RibbonItemSize.LARGE,
            true,
            mi("annotateDimension.0", Messages.get("ribbon.annotatedimension.0"), ic(CadIcons.DIMENSION_LINEAR)),
            mi("annotateDimension.1", Messages.get("ribbon.annotatedimension.1"), ic(CadIcons.DIMENSION_ALIGNED)),
            mi("annotateDimension.2", Messages.get("ribbon.annotatedimension.2"), ic(CadIcons.DIMENSION_ANGULAR)),
            mi("annotateDimension.3", Messages.get("ribbon.annotatedimension.3"), ic(CadIcons.DIMENSION_RADIUS)),
            mi("annotateDimension.4", Messages.get("ribbon.annotatedimension.4"), ic(CadIcons.DIMENSION_DIAMETER)),
        ),
        cmd("quickDim", Messages.get("ribbon.quickdim"), ic(CadIcons.DIMENSION_ALIGNED), RibbonItemSize.MEDIUM),
        cmd("continueDim", Messages.get("ribbon.continuedim"), ic(CadIcons.DIMENSION_LINEAR), RibbonItemSize.MEDIUM),
        cmd("baselineDim", Messages.get("ribbon.baselinedim"), ic(CadIcons.DIMENSION_LINEAR), RibbonItemSize.MEDIUM),
        cmd("dimBreak", Messages.get("ribbon.dimbreak"), ic(CadIcons.BREAK), RibbonItemSize.SMALL),
        cmd("dimSpace", Messages.get("ribbon.dimspace"), ic(CadIcons.STRETCH), RibbonItemSize.SMALL),
        cmd("dimUpdate", Messages.get("ribbon.dimupdate"), ic(CadIcons.REDO), RibbonItemSize.SMALL),
        combo("annotateDimStyle", null, ic(CadIcons.DIMENSION_STYLE), 120.0, false, "ISO-25", "ISO-25", Messages.get("ribbon.annotatedimstyle.item.1")),
    ).also { g ->
        g.icon = ic(CadIcons.DIMENSION_LINEAR)
        g.isDialogLauncherVisible = true
    },
    group(
        "leaders",
        Messages.get("ribbon.group.leaders"),
        cmd("multileader", Messages.get("ribbon.multileader"), ic(CadIcons.MULTILEADER), RibbonItemSize.LARGE),
        cmd("addLeader", Messages.get("ribbon.addleader"), ic(CadIcons.MULTILEADER), RibbonItemSize.MEDIUM),
        cmd("alignLeaders", Messages.get("ribbon.alignleaders"), ic(CadIcons.ALIGN), RibbonItemSize.MEDIUM),
        cmd("collectLeaders", Messages.get("ribbon.collectleaders"), ic(CadIcons.GROUP), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.MULTILEADER)
        g.isDialogLauncherVisible = true
    },
    group(
        "tables",
        Messages.get("ribbon.group.tables"),
        cmd("tableInsert", Messages.get("ribbon.tableinsert"), ic(CadIcons.TABLE), RibbonItemSize.LARGE),
        cmd("extractTable", Messages.get("ribbon.extracttable"), ic(CadIcons.DATA_EXTRACTION), RibbonItemSize.MEDIUM),
        cmd("linkData", Messages.get("ribbon.linkdata"), ic(CadIcons.FIELD), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.TABLE)
        g.isDialogLauncherVisible = true
    },
    group(
        "markup",
        Messages.get("ribbon.group.markup"),
        cmd("revcloud2", Messages.get("ribbon.revcloud2"), ic(CadIcons.REVISION_CLOUD), RibbonItemSize.LARGE),
        cmd("wipeout", Messages.get("ribbon.wipeout"), ic(CadIcons.REGION), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.REVISION_CLOUD)
    },
    group(
        "annotationScaling",
        Messages.get("ribbon.group.annotationscaling"),
        cmd("scaleList", Messages.get("ribbon.scalelist"), ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.LARGE),
        cmd("addCurrentScale", Messages.get("ribbon.addcurrentscale"), ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.MEDIUM),
        cmd("syncScale", Messages.get("ribbon.syncscale"), ic(CadIcons.ALIGN), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.ANNOTATION_SCALE)
    },
)

/** The [Parametric] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun parametricTab(): RibbonTabModel = tab(
    "parametric",
    Messages.get("ribbon.tab.parametric"),
    "P",
    group(
        "geometric", Messages.get("ribbon.group.geometric"),
        cmd("autoConstrain", Messages.get("ribbon.autoconstrain"), ic(CadIcons.CONSTRAINT_AUTO), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.autoconstrain.tip"))
        },
        cmd("coincident", Messages.get("ribbon.coincident"), ic(CadIcons.CONSTRAINT_COINCIDENT), RibbonItemSize.SMALL),
        cmd("collinear", Messages.get("ribbon.collinear"), ic(CadIcons.CONSTRAINT_COLLINEAR), RibbonItemSize.SMALL),
        cmd("concentric", Messages.get("ribbon.concentric"), ic(CadIcons.CONSTRAINT_CONCENTRIC), RibbonItemSize.SMALL),
        cmd("fix", Messages.get("ribbon.fix"), ic(CadIcons.LAYER_LOCK), RibbonItemSize.SMALL),
        cmd("parallel", Messages.get("ribbon.parallel"), ic(CadIcons.CONSTRAINT_PARALLEL), RibbonItemSize.SMALL),
        cmd("perpendicular", Messages.get("ribbon.perpendicular"), ic(CadIcons.CONSTRAINT_PERPENDICULAR), RibbonItemSize.SMALL),
        cmd("horizontal", Messages.get("ribbon.horizontal"), ic(CadIcons.CONSTRAINT_HORIZONTAL), RibbonItemSize.SMALL),
        cmd("vertical", Messages.get("ribbon.vertical"), ic(CadIcons.CONSTRAINT_VERTICAL), RibbonItemSize.SMALL),
        cmd("tangent", Messages.get("ribbon.tangent"), ic(CadIcons.CONSTRAINT_TANGENT), RibbonItemSize.SMALL),
        cmd("symmetric", Messages.get("ribbon.symmetric"), ic(CadIcons.MIRROR), RibbonItemSize.SMALL),
        cmd("equal", Messages.get("ribbon.equal"), ic(CadIcons.CONSTRAINT_EQUAL), RibbonItemSize.SMALL),
        cmd("smooth", Messages.get("ribbon.smooth"), ic(CadIcons.SPLINE), RibbonItemSize.SMALL),
        cmd("showHide", Messages.get("ribbon.showhide"), ic(CadIcons.LAYER_ON), RibbonItemSize.MEDIUM),
        cmd("showAll", Messages.get("ribbon.showall"), ic(CadIcons.LAYER_ON), RibbonItemSize.MEDIUM),
        cmd("hideAll", Messages.get("ribbon.hideall"), ic(CadIcons.LAYER_OFF), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.CONSTRAINT_AUTO)
        g.isDialogLauncherVisible = true
    },
    group(
        "dimensional",
        Messages.get("ribbon.group.dimensional"),
        spl(
            "dimConstraint",
            Messages.get("ribbon.dimconstraint"),
            ic(CadIcons.DIMENSION_LINEAR),
            RibbonItemSize.LARGE,
            true,
            mi("dimConstraint.0", Messages.get("ribbon.dimconstraint.0"), ic(CadIcons.DIMENSION_LINEAR)),
            mi("dimConstraint.1", Messages.get("ribbon.dimconstraint.1"), ic(CadIcons.DIMENSION_ALIGNED)),
            mi("dimConstraint.2", Messages.get("ribbon.dimconstraint.2"), ic(CadIcons.DIMENSION_ANGULAR)),
            mi("dimConstraint.3", Messages.get("ribbon.dimconstraint.3"), ic(CadIcons.DIMENSION_RADIUS)),
            mi("dimConstraint.4", Messages.get("ribbon.dimconstraint.4"), ic(CadIcons.DIMENSION_DIAMETER)),
        ),
        cmd("convertConstraint", Messages.get("ribbon.convertconstraint"), ic(CadIcons.REDO), RibbonItemSize.MEDIUM),
        cmd("showDynamic", Messages.get("ribbon.showdynamic"), ic(CadIcons.LAYER_ON), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.DIMENSION_LINEAR)
        g.isDialogLauncherVisible = true
    },
    group(
        "parametricManage",
        Messages.get("ribbon.group.parametricmanage"),
        cmd("deleteConstraints", Messages.get("ribbon.deleteconstraints"), ic(CadIcons.ERASE), RibbonItemSize.LARGE),
        cmd("parametersManager", Messages.get("ribbon.parametersmanager"), ic(CadIcons.CALCULATOR), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.CUSTOMIZE)
    },
)

/** The [View] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun viewTab(): RibbonTabModel = tab(
    "view", Messages.get("ribbon.tab.view"), "V",
    group(
        "viewportTools",
        Messages.get("ribbon.group.viewporttools"),
        tgl("ucsIcon", Messages.get("ribbon.ucsicon"), ic(CadIcons.MODEL), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.ucsicon.tip"))
            it.isChecked = true
        },
        tgl("viewCube", Messages.get("ribbon.viewcube"), ic(CadIcons.BOX), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.viewcube.tip"))
            it.isChecked = true
        },
        tgl("navBar", Messages.get("ribbon.navbar"), ic(CadIcons.PAN), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.navbar.tip"))
            it.isChecked = true
        },
    ).also { g ->
        g.icon = ic(CadIcons.MODEL)
    },
    group(
        "namedViews",
        Messages.get("ribbon.group.namedviews"),
        cmd("viewManager", Messages.get("ribbon.viewmanager"), ic(CadIcons.VIEW_MANAGER), RibbonItemSize.MEDIUM),
        cmd("newView", Messages.get("ribbon.newview"), ic(CadIcons.NAMED_VIEWS), RibbonItemSize.MEDIUM),
        combo("viewList", null, null, 120.0, false, Messages.get("ribbon.viewlist.item.0"), Messages.get("ribbon.viewlist.item.0"), Messages.get("ribbon.viewlist.item.1"), Messages.get("ribbon.viewlist.item.2")),
    ).also { g ->
        g.icon = ic(CadIcons.NAMED_VIEWS)
    },
    group(
        "modelViewports",
        Messages.get("ribbon.group.modelviewports"),
        ddn(
            "viewportConfiguration",
            Messages.get("ribbon.viewportconfiguration"),
            ic(CadIcons.VIEWPORTS),
            RibbonItemSize.LARGE,
            mi("viewportConfiguration.0", Messages.get("ribbon.viewportconfiguration.0"), null),
            mi("viewportConfiguration.1", Messages.get("ribbon.viewportconfiguration.1"), null),
            mi("viewportConfiguration.2", Messages.get("ribbon.viewportconfiguration.2"), null),
            mi("viewportConfiguration.3", Messages.get("ribbon.viewportconfiguration.3"), null),
        ),
        cmd("namedViewports", Messages.get("ribbon.namedviewports"), ic(CadIcons.NAMED_VIEWS), RibbonItemSize.MEDIUM),
        cmd("joinViewports", Messages.get("ribbon.joinviewports"), ic(CadIcons.JOIN), RibbonItemSize.MEDIUM),
        cmd("restoreViewports", Messages.get("ribbon.restoreviewports"), ic(CadIcons.UNDO), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.VIEWPORTS)
    },
    group(
        "palettes",
        Messages.get("ribbon.group.palettes"),
        tgl("toolPalettes", Messages.get("ribbon.toolpalettes"), ic(CadIcons.WORKSPACE), RibbonItemSize.LARGE),
        tgl("propertiesPalette", Messages.get("ribbon.propertiespalette"), ic(CadIcons.PROPERTIES_PALETTE), RibbonItemSize.LARGE).also {
            it.shortcut = "Ctrl+1"
        },
        tgl("layerPalette", Messages.get("ribbon.layerpalette"), ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM),
        tgl("commandLine", Messages.get("ribbon.commandline"), ic(CadIcons.COMMAND_LINE), RibbonItemSize.MEDIUM).also {
            it.shortcut = "Ctrl+9"
            it.isChecked = true
        },
        tgl("externalReferences", Messages.get("ribbon.externalreferences"), ic(CadIcons.ATTACH), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.PROPERTIES_PALETTE)
    },
    group(
        "interface",
        Messages.get("ribbon.group.interface"),
        tgl("fileTabs", Messages.get("ribbon.filetabs"), ic(CadIcons.FILE_TABS), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
        tgl("layoutTabs", Messages.get("ribbon.layouttabs"), ic(CadIcons.PAPER), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
        tgl("lightTheme", Messages.get("ribbon.lighttheme"), ic(CadIcons.SUN), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.lighttheme.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.WORKSPACE)
    },
    group(
        "ribbonFeatures", Messages.get("ribbon.group.ribbonfeatures"),
        ddn(
            "minimizeBehavior",
            Messages.get("ribbon.minimizebehavior"),
            ic(CadIcons.CLEAN_SCREEN),
            RibbonItemSize.LARGE,
        ).also {
            it.screenTip = tip(Messages.get("ribbon.minimizebehavior.tip"))
        },
        cmd("cycleMinimize", Messages.get("ribbon.cycleminimize"), ic(CadIcons.REDO), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.cycleminimize.tip"))
        },
        tgl("panelTitles", Messages.get("ribbon.paneltitles"), ic(CadIcons.MULTILINE_TEXT), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.paneltitles.tip"))
            it.isChecked = true
        },
        cmd("floatLayers", Messages.get("ribbon.floatlayers"), ic(CadIcons.LAYER_PROPERTIES), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.floatlayers.tip"))
        },
        cmd("returnPanels", Messages.get("ribbon.returnpanels"), ic(CadIcons.UNDO), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.returnpanels.tip"))
        },
        cmd("showTextEditor", Messages.get("ribbon.showtexteditor"), ic(CadIcons.MULTILINE_TEXT), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.showtexteditor.tip"))
        },
        cmd("showHatch", Messages.get("ribbon.showhatch"), ic(CadIcons.HATCH), RibbonItemSize.MEDIUM).also {
            it.screenTip = tip(Messages.get("ribbon.showhatch.tip"))
        },
    ).also { g ->
        g.icon = ic(CadIcons.CLEAN_SCREEN)
        g.description = Messages.get("ribbon.group.ribbonfeatures.description")
    },
)

/** The [Manage] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun manageTab(): RibbonTabModel = tab(
    "manage",
    Messages.get("ribbon.tab.manage"),
    "M",
    group(
        "actionRecorder",
        Messages.get("ribbon.group.actionrecorder"),
        tgl("record", Messages.get("ribbon.record"), ic(CadIcons.ACTION_RECORDER), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.record.tip"))
        },
        cmd("play", Messages.get("ribbon.play"), RibbonIcon.glyph("\uE768"), RibbonItemSize.MEDIUM),
        cmd("insertMessage", Messages.get("ribbon.insertmessage"), ic(CadIcons.MULTILINE_TEXT), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.ACTION_RECORDER)
    },
    group(
        "customization",
        Messages.get("ribbon.group.customization"),
        cmd("userInterface", Messages.get("ribbon.userinterface"), ic(CadIcons.CUSTOMIZE), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.userinterface.tip"))
        },
        cmd("toolPalettesCustomize", Messages.get("ribbon.toolpalettescustomize"), ic(CadIcons.WORKSPACE), RibbonItemSize.MEDIUM),
        cmd("importCustomization", Messages.get("ribbon.importcustomization"), ic(CadIcons.OPEN), RibbonItemSize.MEDIUM),
        cmd("editAliases", Messages.get("ribbon.editaliases"), ic(CadIcons.COMMAND_LINE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.CUSTOMIZE)
    },
    group(
        "applications",
        Messages.get("ribbon.group.applications"),
        cmd("loadApplication", Messages.get("ribbon.loadapplication"), ic(CadIcons.LOAD_APPLICATION), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.loadapplication.tip"))
        },
        cmd("runScript", Messages.get("ribbon.runscript"), ic(CadIcons.COMMAND_LINE), RibbonItemSize.MEDIUM),
        cmd("scriptEditor", Messages.get("ribbon.scripteditor"), ic(CadIcons.EDIT_POLYLINE), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.LOAD_APPLICATION)
    },
    group(
        "cadStandards",
        Messages.get("ribbon.group.cadstandards"),
        cmd("checkStandards", Messages.get("ribbon.checkstandards"), ic(CadIcons.QUICK_SELECT), RibbonItemSize.MEDIUM),
        cmd("configureStandards", Messages.get("ribbon.configurestandards"), ic(CadIcons.CUSTOMIZE), RibbonItemSize.MEDIUM),
        cmd("layerTranslator", Messages.get("ribbon.layertranslator"), ic(CadIcons.MATCH_LAYER), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.QUICK_SELECT)
    },
    group(
        "cleanup",
        Messages.get("ribbon.group.cleanup"),
        cmd("purge", Messages.get("ribbon.purge"), ic(CadIcons.PURGE), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.purge.tip"))
        },
        cmd("overkill", Messages.get("ribbon.overkill"), ic(CadIcons.ERASE), RibbonItemSize.MEDIUM),
        cmd("units", Messages.get("ribbon.units"), ic(CadIcons.UNITS), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.PURGE)
    },
)

/** The [Output] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun outputTab(): RibbonTabModel = tab(
    "output",
    Messages.get("ribbon.tab.output"),
    "O",
    group(
        "plotGroup",
        Messages.get("ribbon.group.plotgroup"),
        cmd("plot", Messages.get("ribbon.plot"), ic(CadIcons.PLOT), RibbonItemSize.LARGE).also {
            it.shortcut = "Ctrl+P"
            it.screenTip = tip(Messages.get("ribbon.plot.tip"))
        },
        cmd("batchPlot", Messages.get("ribbon.batchplot"), ic(CadIcons.BATCH_PLOT), RibbonItemSize.LARGE),
        cmd("pageSetup", Messages.get("ribbon.pagesetup"), ic(CadIcons.PAGE_SETUP), RibbonItemSize.MEDIUM),
        cmd("plotterManager", Messages.get("ribbon.plottermanager"), ic(CadIcons.PLOT), RibbonItemSize.MEDIUM),
        cmd("preview", Messages.get("ribbon.preview"), ic(CadIcons.ZOOM_WINDOW), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.PLOT)
    },
    group(
        "exportGroup",
        Messages.get("ribbon.group.exportgroup"),
        spl(
            "exportSplit",
            Messages.get("ribbon.exportsplit"),
            ic(CadIcons.EXPORT_PDF),
            RibbonItemSize.LARGE,
            true,
            mi("exportSplit.0", "PDF", ic(CadIcons.EXPORT_PDF)),
            mi("exportSplit.1", "DWF", ic(CadIcons.PUBLISH)),
            mi("exportSplit.2", "DWFx", ic(CadIcons.PUBLISH)),
        ),
        combo("exportExtents", Messages.get("ribbon.exportextents.label"), null, 110.0, false, Messages.get("ribbon.exportextents.item.0"), Messages.get("ribbon.exportextents.item.0"), Messages.get("ribbon.exportextents.item.1"), Messages.get("ribbon.exportextents.item.2")),
        combo("exportPageSetup", Messages.get("ribbon.exportpagesetup.label"), null, 110.0, false, Messages.get("ribbon.exportpagesetup.item.0"), Messages.get("ribbon.exportpagesetup.item.0"), Messages.get("ribbon.exportpagesetup.item.1")),
        cmd("exportPreview", Messages.get("ribbon.exportpreview"), ic(CadIcons.ZOOM_WINDOW), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.EXPORT_PDF)
    },
    group(
        "publishGroup",
        Messages.get("ribbon.group.publishgroup"),
        cmd("publishButton", Messages.get("ribbon.publishbutton"), ic(CadIcons.PUBLISH), RibbonItemSize.LARGE),
        cmd("print3d", Messages.get("ribbon.print3d"), ic(CadIcons.BOX), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.PUBLISH)
    },
)

/** The [Collaborate] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun collaborateTab(): RibbonTabModel = tab(
    "collaborate",
    Messages.get("ribbon.tab.collaborate"),
    "C",
    group(
        "share",
        Messages.get("ribbon.group.share"),
        cmd("shareDrawing", Messages.get("ribbon.sharedrawing"), ic(CadIcons.SHARE), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.sharedrawing.tip"))
        },
        cmd("sharedViews", Messages.get("ribbon.sharedviews"), ic(CadIcons.NAMED_VIEWS), RibbonItemSize.LARGE),
        cmd("pushToDocs", Messages.get("ribbon.pushtodocs"), ic(CadIcons.PUBLISH), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.SHARE)
    },
    group(
        "compare",
        Messages.get("ribbon.group.compare"),
        cmd("dwgCompare", Messages.get("ribbon.dwgcompare"), ic(CadIcons.COMPARE), RibbonItemSize.LARGE).also {
            it.screenTip = tip(Messages.get("ribbon.dwgcompare.tip"))
        },
        cmd("xrefCompare", Messages.get("ribbon.xrefcompare"), ic(CadIcons.ATTACH), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.COMPARE)
    },
    group(
        "collaborateMarkup",
        Messages.get("ribbon.group.collaboratemarkup"),
        cmd("markupImport", Messages.get("ribbon.markupimport"), ic(CadIcons.IMPORT_PDF), RibbonItemSize.LARGE),
        cmd("markupAssist", Messages.get("ribbon.markupassist"), ic(CadIcons.REVISION_CLOUD), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.REVISION_CLOUD)
    },
)

/** The [Text Editor] tab. */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo
private fun textEditorTab(): RibbonTabModel = tab(
    "textEditorTab", Messages.get("ribbon.tab.texteditortab"), "X",
    group(
        "teStyle",
        Messages.get("ribbon.group.testyle"),
        gallery(
            "teStyleGallery",
            Messages.get("ribbon.testylegallery"),
            ic(CadIcons.TEXT_STYLE),
            RibbonItemSize.LARGE,
            gi("Standard", null, null),
            gi(Messages.get("ribbon.testylegallery.item.1"), null, null),
            gi(Messages.get("ribbon.testylegallery.item.2"), null, null),
        ).also { g ->
            g.itemWidth = 64.0
            g.itemHeight = 50.0
            g.minColumns = 2
            g.maxColumns = 3
        },
        stack(
            true,
            tgl("teAnnotative", Messages.get("ribbon.teannotative"), ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.MEDIUM),
            combo("teHeight", null, ic(CadIcons.SINGLE_LINE_TEXT), 90.0, true, "2.5", "1.8", "2.5", "3.5", "5"),
            cmd("teMask", Messages.get("ribbon.temask"), ic(CadIcons.REGION), RibbonItemSize.MEDIUM),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.TEXT_STYLE)
    },
    group(
        "teFormatting",
        Messages.get("ribbon.group.teformatting"),
        stack(
            false,
            RibbonButtonGroupModel(
                tgl("teBold", Messages.get("ribbon.tebold"), RibbonIcon.glyph("\uE8DD"), RibbonItemSize.SMALL).also {
                    it.shortcut = "Ctrl+B"
                },
                tgl("teItalic", Messages.get("ribbon.teitalic"), RibbonIcon.glyph("\uE8DB"), RibbonItemSize.SMALL).also {
                    it.shortcut = "Ctrl+I"
                },
                tgl("teUnderline", Messages.get("ribbon.teunderline"), RibbonIcon.glyph("\uE8DC"), RibbonItemSize.SMALL).also {
                    it.shortcut = "Ctrl+U"
                },
                tgl("teStrike", Messages.get("ribbon.testrike"), RibbonIcon.glyph("\uEDE0"), RibbonItemSize.SMALL),
            ),
            fontCombo("teFont", "Arial", 130.0),
        ),
        stack(
            false,
            colorCombo("teColor"),
            cmd("teClearFormatting", Messages.get("ribbon.teclearformatting"), RibbonIcon.glyph("\uE8E6"), RibbonItemSize.SMALL),
            cmd("teMatch", Messages.get("ribbon.tematch"), ic(CadIcons.MATCH_PROPERTIES), RibbonItemSize.SMALL),
        ),
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE8D2")
        g.itemsLayout = RibbonGroupItemsLayout.ROWS
        g.rowCount = 2
    },
    group(
        "teParagraph",
        Messages.get("ribbon.group.teparagraph"),
        ddn(
            "teJustification",
            Messages.get("ribbon.tejustification"),
            RibbonIcon.glyph("\uE8E4"),
            RibbonItemSize.LARGE,
            radioItem("teJustification.0", Messages.get("ribbon.tejustification.0"), "just", true),
            radioItem("teJustification.1", Messages.get("ribbon.tejustification.1"), "just", false),
            radioItem("teJustification.2", Messages.get("ribbon.tejustification.2"), "just", false),
            radioItem("teJustification.3", Messages.get("ribbon.tejustification.3"), "just", false),
        ),
        RibbonButtonGroupModel(
            tgl("teLeft", Messages.get("ribbon.teleft"), RibbonIcon.glyph("\uE8E4"), RibbonItemSize.SMALL).also {
                it.isChecked = true
                it.groupName = "teAlign"
            },
            tgl("teCenter", Messages.get("ribbon.tecenter"), RibbonIcon.glyph("\uE8E3"), RibbonItemSize.SMALL).also {
                it.groupName = "teAlign"
            },
            tgl("teRight", Messages.get("ribbon.teright"), RibbonIcon.glyph("\uE8E2"), RibbonItemSize.SMALL).also {
                it.groupName = "teAlign"
            },
        ),
        ddn(
            "teBullets",
            Messages.get("ribbon.tebullets"),
            RibbonIcon.glyph("\uE8FD"),
            RibbonItemSize.SMALL,
            mi("teBullets.0", Messages.get("ribbon.tebullets.0"), null),
            mi("teBullets.1", Messages.get("ribbon.tebullets.1"), null),
            mi("teBullets.2", Messages.get("ribbon.tebullets.2"), null),
            mi("teBullets.3", Messages.get("ribbon.tebullets.3"), null),
        ),
        ddn(
            "teLineSpacing",
            Messages.get("ribbon.telinespacing"),
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
        Messages.get("ribbon.group.teinsert"),
        ddn(
            "teColumns",
            Messages.get("ribbon.tecolumns"),
            ic(CadIcons.TABLE),
            RibbonItemSize.LARGE,
            mi("teColumns.0", Messages.get("ribbon.tecolumns.0"), null),
            mi("teColumns.1", Messages.get("ribbon.tecolumns.1"), null),
            mi("teColumns.2", Messages.get("ribbon.tecolumns.2"), null),
        ),
        ddn(
            "teSymbol",
            Messages.get("ribbon.tesymbol"),
            RibbonIcon.text("Ø"),
            RibbonItemSize.LARGE,
            mi("teSymbol.0", Messages.get("ribbon.tesymbol.0"), null),
            mi("teSymbol.1", Messages.get("ribbon.tesymbol.1"), null),
            mi("teSymbol.2", Messages.get("ribbon.tesymbol.2"), null),
            mi("teSymbol.3", Messages.get("ribbon.tesymbol.3"), null),
        ),
        cmd("teField", Messages.get("ribbon.tefield"), ic(CadIcons.FIELD), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.FIELD)
    },
    group(
        "teSpell",
        Messages.get("ribbon.group.tespell"),
        tgl("teSpellCheck", Messages.get("ribbon.tespellcheck"), RibbonIcon.glyph("\uE8FB"), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE8FB")
    },
    group(
        "teTools",
        Messages.get("ribbon.group.tetools"),
        cmd("teFind", Messages.get("ribbon.tefind"), ic(CadIcons.SEARCH), RibbonItemSize.LARGE).also {
            it.shortcut = "Ctrl+R"
        },
    ).also { g ->
        g.icon = ic(CadIcons.SEARCH)
    },
    group(
        "teOptions",
        Messages.get("ribbon.group.teoptions"),
        cmd("teMore", Messages.get("ribbon.temore"), RibbonIcon.glyph("\uE712"), RibbonItemSize.MEDIUM),
        tgl("teRuler", Messages.get("ribbon.teruler"), ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.MEDIUM).also {
            it.isChecked = true
        },
        cmd("teUndo", Messages.get("ribbon.teundo"), ic(CadIcons.UNDO), RibbonItemSize.SMALL),
        cmd("teRedo", Messages.get("ribbon.teredo"), ic(CadIcons.REDO), RibbonItemSize.SMALL),
    ).also { g ->
        g.icon = RibbonIcon.glyph("\uE713")
    },
    group(
        "teClose",
        Messages.get("ribbon.group.teclose"),
        cmd("closeTextEditor", Messages.get("ribbon.closetexteditor"), ic(CadIcons.CLOSE_EDITOR), RibbonItemSize.LARGE).also {
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
    "hatchCreationTab", Messages.get("ribbon.tab.hatchcreationtab"), "W",
    group(
        "hcBoundaries",
        Messages.get("ribbon.group.hcboundaries"),
        cmd("hcPickPoints", Messages.get("ribbon.hcpickpoints"), ic(CadIcons.BOUNDARY), RibbonItemSize.LARGE),
        cmd("hcSelect", Messages.get("ribbon.hcselect"), ic(CadIcons.QUICK_SELECT), RibbonItemSize.MEDIUM),
        cmd("hcRemove", Messages.get("ribbon.hcremove"), ic(CadIcons.ERASE), RibbonItemSize.MEDIUM),
        cmd("hcRecreate", Messages.get("ribbon.hcrecreate"), ic(CadIcons.REDO), RibbonItemSize.MEDIUM),
    ).also { g ->
        g.icon = ic(CadIcons.BOUNDARY)
    },
    group(
        "hcPattern",
        Messages.get("ribbon.group.hcpattern"),
        gallery(
            "hcPatternGallery", Messages.get("ribbon.hcpatterngallery"), ic(CadIcons.HATCH), RibbonItemSize.LARGE,
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
        Messages.get("ribbon.group.hcproperties"),
        stack(
            true,
            combo("hcType", null, ic(CadIcons.HATCH), 110.0, false, Messages.get("ribbon.hctype.item.2"), Messages.get("ribbon.hctype.item.0"), Messages.get("ribbon.hctype.item.1"), Messages.get("ribbon.hctype.item.2"), Messages.get("ribbon.hctype.item.3")),
            colorCombo("hcColor"),
            combo("hcBackground", null, ic(CadIcons.GRADIENT), 110.0, false, Messages.get("ribbon.hcbackground.item.0"), Messages.get("ribbon.hcbackground.item.0"), "ByLayer"),
        ),
        stack(
            true,
            slider("hcTransparency", Messages.get("ribbon.hctransparency"), null, 96.0, 0.0),
            spinner("hcAngle", Messages.get("ribbon.hcangle"), 70.0, 0.0, "°"),
            spinner("hcScale", Messages.get("ribbon.hcscale"), 70.0, 1.0, null),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.COLOR)
    },
    group(
        "hcOrigin",
        Messages.get("ribbon.group.hcorigin"),
        cmd("hcSetOrigin", Messages.get("ribbon.hcsetorigin"), ic(CadIcons.MODEL), RibbonItemSize.LARGE),
    ).also { g ->
        g.icon = ic(CadIcons.MODEL)
    },
    group(
        "hcOptions",
        Messages.get("ribbon.group.hcoptions"),
        tgl("hcAssociative", Messages.get("ribbon.hcassociative"), ic(CadIcons.JOIN), RibbonItemSize.LARGE).also {
            it.isChecked = true
        },
        tgl("hcAnnotative", Messages.get("ribbon.hcannotative"), ic(CadIcons.ANNOTATION_SCALE), RibbonItemSize.LARGE),
        spl(
            "hcMatch",
            Messages.get("ribbon.hcmatch"),
            ic(CadIcons.MATCH_PROPERTIES),
            RibbonItemSize.LARGE,
            false,
            mi("hcMatch.0", Messages.get("ribbon.hcmatch.0"), null),
            mi("hcMatch.1", Messages.get("ribbon.hcmatch.1"), null),
        ),
    ).also { g ->
        g.icon = ic(CadIcons.MATCH_PROPERTIES)
        g.isDialogLauncherVisible = true
    },
    group(
        "hcClose",
        Messages.get("ribbon.group.hcclose"),
        cmd("closeHatchCreation", Messages.get("ribbon.closehatchcreation"), ic(CadIcons.CLOSE_EDITOR), RibbonItemSize.LARGE).also {
            it.keyTip = "C"
        },
    ).also { g ->
        g.icon = ic(CadIcons.CLOSE)
    },
).also { t ->
    t.contextualGroupId = "hatchCreation"
}
