package com.appkitbox.winui4k.sample.ribbon.cad

/** Command names and prompts shown on the command line (item id → (command, prompt)). Same as the RibbonSpace CAD demo. */
internal val CAD_COMMANDS: Map<String, Pair<String, String>> = mapOf(
    "line" to ("LINE" to Messages.get("prompt.first.point")),
    "polyline" to ("PLINE" to Messages.get("prompt.start.point")),
    "circle" to ("CIRCLE" to Messages.get("prompt.circle")),
    "arc" to ("ARC" to Messages.get("prompt.arc")),
    "rectangle" to ("RECTANG" to Messages.get("prompt.rectangle")),
    "hatch" to ("HATCH" to Messages.get("prompt.hatch")),
    "ellipse" to ("ELLIPSE" to Messages.get("prompt.ellipse")),
    "spline" to ("SPLINE" to Messages.get("prompt.spline")),
    "constructionLine" to ("XLINE" to Messages.get("prompt.xline")),
    "ray" to ("RAY" to Messages.get("prompt.start.point")),
    "multiplePoints" to ("POINT" to Messages.get("prompt.point")),
    "region" to ("REGION" to Messages.get("prompt.select.objects")),
    "revisionCloud" to ("REVCLOUD" to Messages.get("prompt.revcloud")),
    "donut" to ("DONUT" to Messages.get("prompt.donut")),
    "move" to ("MOVE" to Messages.get("prompt.select.objects")),
    "copy" to ("COPY" to Messages.get("prompt.select.objects")),
    "stretch" to ("STRETCH" to Messages.get("prompt.stretch")),
    "rotate" to ("ROTATE" to Messages.get("prompt.select.objects")),
    "mirror" to ("MIRROR" to Messages.get("prompt.select.objects")),
    "scale" to ("SCALE" to Messages.get("prompt.select.objects")),
    "trim" to ("TRIM" to Messages.get("prompt.trim")),
    "extend" to ("EXTEND" to Messages.get("prompt.extend")),
    "fillet" to ("FILLET" to Messages.get("prompt.fillet")),
    "chamfer" to ("CHAMFER" to Messages.get("prompt.chamfer")),
    "array" to ("ARRAYRECT" to Messages.get("prompt.select.objects")),
    "erase" to ("ERASE" to Messages.get("prompt.select.objects")),
    "explode" to ("EXPLODE" to Messages.get("prompt.select.objects")),
    "offset" to ("OFFSET" to Messages.get("prompt.offset")),
    "break" to ("BREAK" to Messages.get("prompt.select.objects")),
    "join" to ("JOIN" to Messages.get("prompt.join")),
    "text" to ("MTEXT" to Messages.get("prompt.first.corner")),
    "dimension" to ("DIMLINEAR" to Messages.get("prompt.dimension")),
    "leader" to ("MLEADER" to Messages.get("prompt.leader")),
    "table" to ("TABLE" to Messages.get("prompt.insertion.point")),
    "layerProperties" to ("LAYER" to Messages.get("prompt.layer")),
    "insertBlock" to ("INSERT" to Messages.get("prompt.insert")),
    "createBlock" to ("BLOCK" to Messages.get("prompt.block")),
    "matchProperties" to ("MATCHPROP" to Messages.get("prompt.matchprop")),
    "group" to ("GROUP" to Messages.get("prompt.group")),
    "measure" to ("MEASUREGEOM" to Messages.get("prompt.measure")),
    "quickSelect" to ("QSELECT" to Messages.get("prompt.qselect")),
    "selectAll" to ("SELECTALL" to Messages.get("prompt.select.all")),
    "idPoint" to ("ID" to Messages.get("prompt.specify.point")),
    "paste" to ("PASTECLIP" to Messages.get("prompt.insertion.point")),
    "copyClip" to ("COPYCLIP" to Messages.get("prompt.select.objects")),
    "cutClip" to ("CUTCLIP" to Messages.get("prompt.select.objects")),
    "plot" to ("PLOT" to Messages.get("prompt.plot")),
    "qat.plot" to ("PLOT" to Messages.get("prompt.plot")),
    "qat.save" to ("QSAVE" to Messages.get("prompt.qsave")),
    "qat.undo" to ("U" to "LINE"),
    "qat.redo" to ("MREDO" to Messages.get("prompt.mredo")),
    "box" to ("BOX" to Messages.get("prompt.box")),
    "extrude" to ("EXTRUDE" to Messages.get("prompt.extrude")),
    "purge" to ("PURGE" to Messages.get("prompt.purge")),
    "zoomExtents" to ("ZOOM" to Messages.get("prompt.zoom")),
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
