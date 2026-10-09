package com.appkitbox.winui4k.sample.ribbon.cad

/** Command names and prompts shown on the command line (item id → (command, prompt)). Same as the RibbonSpace CAD demo. */
internal val CAD_COMMANDS: Map<String, Pair<String, String>> = mapOf(
    "line" to ("LINE" to "Specify first point:"),
    "polyline" to ("PLINE" to "Specify start point:"),
    "circle" to ("CIRCLE" to "Specify center point for circle or [3P/2P/Ttr (tan tan radius)]:"),
    "arc" to ("ARC" to "Specify start point of arc or [Center]:"),
    "rectangle" to ("RECTANG" to "Specify first corner point or [Chamfer/Elevation/Fillet/Thickness/Width]:"),
    "hatch" to ("HATCH" to "Pick internal point or [Select objects/Undo/seTtings]:"),
    "ellipse" to ("ELLIPSE" to "Specify axis endpoint of ellipse or [Arc/Center]:"),
    "spline" to ("SPLINE" to "Specify first point or [Method/Knots/Object]:"),
    "constructionLine" to ("XLINE" to "Specify a point or [Hor/Ver/Ang/Bisect/Offset]:"),
    "ray" to ("RAY" to "Specify start point:"),
    "multiplePoints" to ("POINT" to "Specify a point:"),
    "region" to ("REGION" to "Select objects:"),
    "revisionCloud" to ("REVCLOUD" to "Specify first corner point or [Arc length/Object/Rectangular/Polygonal/Freehand/Style/Modify]:"),
    "donut" to ("DONUT" to "Specify inside diameter of donut <0.5000>:"),
    "move" to ("MOVE" to "Select objects:"),
    "copy" to ("COPY" to "Select objects:"),
    "stretch" to ("STRETCH" to "Select objects to stretch by crossing-window or crossing-polygon..."),
    "rotate" to ("ROTATE" to "Select objects:"),
    "mirror" to ("MIRROR" to "Select objects:"),
    "scale" to ("SCALE" to "Select objects:"),
    "trim" to ("TRIM" to "Select object to trim or shift-select to extend:"),
    "extend" to ("EXTEND" to "Select object to extend or shift-select to trim:"),
    "fillet" to ("FILLET" to "Select first object or [Undo/Polyline/Radius/Trim/Multiple]:"),
    "chamfer" to ("CHAMFER" to "Select first line or [Undo/Polyline/Distance/Angle/Trim/mEthod/Multiple]:"),
    "array" to ("ARRAYRECT" to "Select objects:"),
    "erase" to ("ERASE" to "Select objects:"),
    "explode" to ("EXPLODE" to "Select objects:"),
    "offset" to ("OFFSET" to "Specify offset distance or [Through/Erase/Layer] <Through>:"),
    "break" to ("BREAK" to "Select objects:"),
    "join" to ("JOIN" to "Select source object or multiple objects to join at once:"),
    "text" to ("MTEXT" to "Specify first corner:"),
    "dimension" to ("DIMLINEAR" to "Specify first extension line origin or <select object>:"),
    "leader" to ("MLEADER" to "Specify leader arrowhead location or [leader Landing first/Content first/Options] <Options>:"),
    "table" to ("TABLE" to "Specify insertion point:"),
    "layerProperties" to ("LAYER" to "Layer Properties Manager opened."),
    "insertBlock" to ("INSERT" to "Specify insertion point or [Basepoint/Scale/Rotate]:"),
    "createBlock" to ("BLOCK" to "Enter block name or [?]:"),
    "matchProperties" to ("MATCHPROP" to "Select source object:"),
    "group" to ("GROUP" to "Select objects or [Name/Description]:"),
    "measure" to ("MEASUREGEOM" to "Move cursor or [Distance/Radius/Angle/ARea/Volume/Quick/Mode/eXit] <eXit>:"),
    "quickSelect" to ("QSELECT" to "Quick Select dialog opened."),
    "selectAll" to ("SELECTALL" to "12 found"),
    "idPoint" to ("ID" to "Specify point:"),
    "paste" to ("PASTECLIP" to "Specify insertion point:"),
    "copyClip" to ("COPYCLIP" to "Select objects:"),
    "cutClip" to ("CUTCLIP" to "Select objects:"),
    "plot" to ("PLOT" to "Plot - Model dialog opened."),
    "qat.plot" to ("PLOT" to "Plot - Model dialog opened."),
    "qat.save" to ("QSAVE" to "Drawing1.dwg saved."),
    "qat.undo" to ("U" to "LINE"),
    "qat.redo" to ("MREDO" to "Everything has been redone"),
    "box" to ("BOX" to "Specify first corner or [Center]:"),
    "extrude" to ("EXTRUDE" to "Select objects to extrude or [MOde]:"),
    "purge" to ("PURGE" to "Purge dialog opened."),
    "zoomExtents" to ("ZOOM" to "Specify corner of window or [Extents/Previous/Window] <real time>: _e"),
)

/** Names and aliases typed on the command line → item id (same as the RibbonSpace CAD demo). */
internal val CAD_ALIASES: Map<String, String> = buildMap {
    fun alias(id: String, vararg names: String) = names.forEach { put(it, id) }
    alias("line", "l", "line")
    alias("polyline", "pl", "pline")
    alias("circle", "c", "circle")
    alias("arc", "a", "arc")
    alias("rectangle", "rec", "rectang")
    alias("hatch", "h", "hatch")
    alias("ellipse", "el", "ellipse")
    alias("spline", "spl", "spline")
    alias("constructionLine", "xl", "xline")
    alias("ray", "ray")
    alias("multiplePoints", "po", "point")
    alias("region", "reg")
    alias("revisionCloud", "revcloud")
    alias("donut", "do", "donut")
    alias("move", "m", "move")
    alias("copy", "co", "cp", "copy")
    alias("stretch", "s", "stretch")
    alias("rotate", "ro", "rotate")
    alias("mirror", "mi", "mirror")
    alias("scale", "sc", "scale")
    alias("trim", "tr", "trim")
    alias("extend", "ex", "extend")
    alias("fillet", "f", "fillet")
    alias("chamfer", "cha", "chamfer")
    alias("array", "ar", "array", "arrayrect")
    alias("erase", "e", "erase")
    alias("explode", "x", "explode")
    alias("offset", "o", "offset")
    alias("break", "br", "break")
    alias("join", "j", "join")
    alias("text", "t", "mt", "mtext")
    alias("dimension", "dli", "dimlinear")
    alias("leader", "mld", "mleader")
    alias("table", "tb", "table")
    alias("layerProperties", "la", "layer")
    alias("insertBlock", "i", "insert")
    alias("createBlock", "b", "block")
    alias("matchProperties", "ma", "matchprop")
    alias("group", "g", "group")
    alias("measure", "mea", "measuregeom")
    alias("quickSelect", "qselect")
    alias("selectAll", "selectall")
    alias("idPoint", "id")
    alias("plot", "plot", "print")
    alias("purge", "pu", "purge")
    alias("box", "box")
    alias("extrude", "ext", "extrude")
}
