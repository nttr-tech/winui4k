package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.WCanvas
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.extension.ribbon.WRibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The floor plan in the drawing area (the same shapes as BuildPlan of CadDrawingCanvas in the RibbonSpace CAD demo; MIT License).
 *
 * Shapes in model space (mm, y pointing up) are combined into a layered path per drawing layer and drawn with [WRibbonIcon].
 * Text is a [WLabel] sized to the model height. When the window is resized, everything is redrawn to fit (Zoom Extents).
 */
internal class CadDrawing(private val canvas: WCanvas) {
    private val polylines = mutableListOf<Polyline>()
    private val arcs = mutableListOf<Arc>()
    private val texts = mutableListOf<Text>()

    /** Whether the theme is dark (the default) or light. */
    var isDark: Boolean = true
        set(value) {
            field = value
            render()
        }

    /** Whether to show the drawing grid ([Grid Display] on the status bar). */
    var isGridVisible: Boolean = true
        set(value) {
            field = value
            render()
        }

    /** Whether to display lineweights ([Show/Hide Lineweight] on the status bar). */
    var showLineweights: Boolean = false
        set(value) {
            field = value
            render()
        }

    /** The canvas background color. */
    val canvasColor: WColor get() = if (isDark) WColor(0x21, 0x28, 0x30) else WColor(0xFA, 0xFB, 0xFC)

    init {
        buildPlan()
        canvas.addSizeChangedListener { render() }
    }

    /** Redraws so that everything fits ([Zoom Extents] on the navigation bar). */
    fun zoomExtents() = render()

    /** The on-screen position of a point in model space. */
    fun modelToScreen(x: Double, y: Double): Pair<Double, Double> {
        val view = view()
        return view.x(x) to view.y(y)
    }

    private fun view(): View {
        val width = max(1.0, canvas.actualWidth - INSET_LEFT - INSET_RIGHT)
        val height = max(1.0, canvas.actualHeight - INSET_TOP - INSET_BOTTOM)
        val scale = min(width / (MAX_X - MIN_X), height / (MAX_Y - MIN_Y))
        val offsetX = INSET_LEFT + (width - (MAX_X - MIN_X) * scale) / 2
        val offsetY = INSET_TOP + (height - (MAX_Y - MIN_Y) * scale) / 2
        return View(scale, offsetX, offsetY)
    }

    private fun render() {
        canvas.removeAll()
        if (canvas.actualWidth <= 0) return
        val view = view()
        // Draw the line work of every layer as a single icon whose square design grid is the model extents
        val side = max(MAX_X - MIN_X, MAX_Y - MIN_Y)
        val size = side * view.scale
        val stroke = 1.0 / view.scale
        val layers = mutableListOf<String>()
        if (isGridVisible) layers += layer(gridData(), stroke, if (isDark) "#FF333D49" else "#FFDCE0E5")
        for (name in LAYER_ORDER) {
            val width = if (showLineweights && name == "A-WALL") 2.0 else 1.0
            polylines.filter { it.layer == name && it.fill }.joinToString(" ") { it.data() }.takeIf { it.isNotEmpty() }?.let {
                layers += "[color=${colorOf(name)}]$it"
            }
            val lines = (polylines.filter { it.layer == name && !it.fill }.map { it.data() } + arcs.filter { it.layer == name }.map { it.data() })
                .joinToString(" ")
            if (lines.isNotEmpty()) layers += layer(lines, stroke * width, colorOf(name))
        }
        // Prefix the first layer's header with the design grid size ("[stroke=..." → "[viewbox=N;stroke=...")
        val icon = RibbonIcon.path("[viewbox=${fmt(side)};" + layers.joinToString("|").removePrefix("["), side)
        canvas.add(WRibbonIcon(icon, size), view.x(MIN_X), view.y(MAX_Y))
        for (text in texts) {
            val label = WLabel(text.value)
            label.fontSize = max(MIN_FONT, text.height * view.scale * FONT_RATIO)
            label.foreground = argb(colorOf(text.layer))
            val (x, y) = view.x(text.x) to view.y(text.y)
            canvas.add(label, x - label.fontSize * text.value.length * CHAR_RATIO, y - label.fontSize * LINE_RATIO)
        }
    }

    private fun layer(data: String, stroke: Double, color: String) = "[stroke=${fmt(stroke)};color=$color]$data"

    private fun gridData(): String = buildString {
        var x = MIN_X - MIN_X % GRID_STEP
        while (x <= MAX_X) {
            append("M${fmt(x - MIN_X)},0 L${fmt(x - MIN_X)},${fmt(MAX_Y - MIN_Y)} ")
            x += GRID_STEP
        }
        var y = MIN_Y - MIN_Y % GRID_STEP
        while (y <= MAX_Y) {
            append("M0,${fmt(MAX_Y - y)} L${fmt(MAX_X - MIN_X)},${fmt(MAX_Y - y)} ")
            y += GRID_STEP
        }
    }.trim()

    private fun colorOf(layer: String): String {
        val (dark, light) = LAYER_COLORS[layer] ?: DEFAULT_COLORS
        return if (isDark) dark else light
    }

    // ---------------------------------------------------------------- Shapes (model space)

    /** A polyline (a polygon if closed). Emitted in design grid coordinates (y pointing down). */
    private inner class Polyline(val layer: String, val points: List<Pair<Double, Double>>, val closed: Boolean, val fill: Boolean = false) {
        fun data(): String = buildString {
            points.forEachIndexed { i, (x, y) -> append(if (i == 0) "M" else " L").append(fmt(x - MIN_X)).append(',').append(fmt(MAX_Y - y)) }
            if (closed) append(" Z")
        }
    }

    /** An arc (angles in degrees, counterclockwise is positive; a circle if 360 degrees or more). */
    private inner class Arc(val layer: String, val cx: Double, val cy: Double, val r: Double, val start: Double, val sweep: Double) {
        private fun at(deg: Double) = (cx + r * cos(deg * PI / DEGREES_HALF) - MIN_X) to (MAX_Y - (cy + r * sin(deg * PI / DEGREES_HALF)))

        fun data(): String {
            val (sx, sy) = at(start)
            if (abs(sweep) >= DEGREES_FULL) {
                val (mx, my) = at(start + DEGREES_HALF)
                return "M${fmt(sx)},${fmt(sy)} A${fmt(r)},${fmt(r)} 0 1 0 ${fmt(mx)},${fmt(my)} A${fmt(r)},${fmt(r)} 0 1 0 ${fmt(sx)},${fmt(sy)} Z"
            }
            val (ex, ey) = at(start + sweep)
            val large = if (abs(sweep) > DEGREES_HALF) 1 else 0
            // Model space has y pointing up and the screen has y pointing down, so a counterclockwise arc is clockwise on screen
            val clockwise = if (sweep > 0) 0 else 1
            return "M${fmt(sx)},${fmt(sy)} A${fmt(r)},${fmt(r)} 0 $large $clockwise ${fmt(ex)},${fmt(ey)}"
        }
    }

    private class Text(val value: String, val x: Double, val y: Double, val height: Double, val layer: String)

    private class View(val scale: Double, val offsetX: Double, val offsetY: Double) {
        fun x(modelX: Double) = offsetX + (modelX - MIN_X) * scale

        fun y(modelY: Double) = offsetY + (MAX_Y - modelY) * scale
    }

    @Suppress("LongParameterList") // the parameters map 1:1 to the shape functions of RibbonSpace's CadDrawingCanvas
    private fun rect(layer: String, x1: Double, y1: Double, x2: Double, y2: Double, fill: Boolean = false) {
        polylines += Polyline(layer, listOf(x1 to y1, x2 to y1, x2 to y2, x1 to y2), closed = true, fill = fill)
    }

    @Suppress("LongParameterList") // the parameters map 1:1 to the shape functions of RibbonSpace's CadDrawingCanvas
    private fun line(layer: String, x1: Double, y1: Double, x2: Double, y2: Double, dashed: Boolean = false) {
        if (!dashed) {
            polylines += Polyline(layer, listOf(x1 to y1, x2 to y2), closed = false)
            return
        }
        val length = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
        val (ux, uy) = (x2 - x1) / length to (y2 - y1) / length
        var d = 0.0
        while (d < length) {
            val e = min(length, d + DASH)
            polylines += Polyline(layer, listOf((x1 + ux * d) to (y1 + uy * d), (x1 + ux * e) to (y1 + uy * e)), closed = false)
            d += DASH + GAP
        }
    }

    private fun poly(layer: String, vararg xy: Double) {
        polylines += Polyline(layer, xy.toList().chunked(2) { it[0] to it[1] }, closed = true)
    }

    private fun circle(layer: String, x: Double, y: Double, r: Double) {
        arcs += Arc(layer, x, y, r, 0.0, DEGREES_FULL)
    }

    @Suppress("LongParameterList") // the parameters map 1:1 to the shape functions of RibbonSpace's CadDrawingCanvas
    private fun arc(layer: String, x: Double, y: Double, r: Double, start: Double, sweep: Double) {
        arcs += Arc(layer, x, y, r, start, sweep)
    }

    private fun text(value: String, x: Double, y: Double, height: Double = DEFAULT_TEXT, layer: String = "A-ANNO-TEXT") {
        texts += Text(value, x, y, height, layer)
    }

    /** A wall along X, broken by openings (start and end). */
    private fun wallX(y1: Double, y2: Double, x1: Double, x2: Double, vararg openings: Pair<Double, Double>) {
        var x = x1
        for ((from, to) in openings.sortedBy { it.first }) {
            rect("A-WALL", x, y1, from, y2, fill = true)
            x = to
        }
        rect("A-WALL", x, y1, x2, y2, fill = true)
    }

    /** A wall along Y, broken by openings. */
    private fun wallY(x1: Double, x2: Double, y1: Double, y2: Double, vararg openings: Pair<Double, Double>) {
        var y = y1
        for ((from, to) in openings.sortedBy { it.first }) {
            rect("A-WALL", x1, y, x2, from, fill = true)
            y = to
        }
        rect("A-WALL", x1, y, x2, y2, fill = true)
    }

    private fun windowX(y1: Double, y2: Double, x1: Double, x2: Double) {
        val t = (y2 - y1) / 3
        rect("A-GLAZ", x1, y1, x2, y2)
        line("A-GLAZ", x1, y1 + t, x2, y1 + t)
        line("A-GLAZ", x1, y1 + 2 * t, x2, y1 + 2 * t)
    }

    private fun windowY(x1: Double, x2: Double, y1: Double, y2: Double) {
        val t = (x2 - x1) / 3
        rect("A-GLAZ", x1, y1, x2, y2)
        line("A-GLAZ", x1 + t, y1, x1 + t, y2)
        line("A-GLAZ", x1 + 2 * t, y1, x1 + 2 * t, y2)
    }

    /** A door of width w hinged at (hx, hy), opened sweep degrees from its closed direction. */
    private fun door(hx: Double, hy: Double, w: Double, closedAngle: Double, sweep: Double) {
        val open = (closedAngle + sweep) * PI / DEGREES_HALF
        line("A-DOOR", hx, hy, hx + w * cos(open), hy + w * sin(open))
        arc("A-DOOR", hx, hy, w, closedAngle, sweep)
    }

    private fun dimX(x1: Double, x2: Double, y: Double, from: Double) {
        line("A-ANNO-DIMS", x1, from, x1, y - DIM_EXT)
        line("A-ANNO-DIMS", x2, from, x2, y - DIM_EXT)
        line("A-ANNO-DIMS", x1 - DIM_EXT, y, x2 + DIM_EXT, y)
        line("A-ANNO-DIMS", x1 - DIM_TICK, y - DIM_TICK, x1 + DIM_TICK, y + DIM_TICK)
        line("A-ANNO-DIMS", x2 - DIM_TICK, y - DIM_TICK, x2 + DIM_TICK, y + DIM_TICK)
        text((x2 - x1).toInt().toString(), (x1 + x2) / 2, y + DIM_TEXT_GAP, DIM_TEXT, "A-ANNO-DIMS")
    }

    private fun dimY(y1: Double, y2: Double, x: Double, from: Double) {
        line("A-ANNO-DIMS", from, y1, x - DIM_EXT, y1)
        line("A-ANNO-DIMS", from, y2, x - DIM_EXT, y2)
        line("A-ANNO-DIMS", x, y1 - DIM_EXT, x, y2 + DIM_EXT)
        line("A-ANNO-DIMS", x - DIM_TICK, y1 - DIM_TICK, x + DIM_TICK, y1 + DIM_TICK)
        line("A-ANNO-DIMS", x - DIM_TICK, y2 - DIM_TICK, x + DIM_TICK, y2 + DIM_TICK)
        // Text cannot be rotated, so the value of a vertical dimension is placed off to the left so it does not overlap the dimension line
        text((y2 - y1).toInt().toString(), x - DIM_TEXT_GAP * 2, (y1 + y2) / 2, DIM_TEXT, "A-ANNO-DIMS")
    }

    // The same floor plan as RibbonSpace's CadDrawingCanvas.BuildPlan (exterior walls, interior walls, doors, furniture, grid lines, dimensions, room names)
    @Suppress("LongMethod", "MagicNumber") // declares the floor plan shapes from top to bottom (coordinates are the drawing's actual dimensions)
    private fun buildPlan() {
        val ext = 250.0
        val int = 120.0
        wallX(0.0, ext, 0.0, 12000.0, 1200.0 to 3200.0, 4000.0 to 5500.0, 9000.0 to 10000.0)
        wallX(8000 - ext, 8000.0, 0.0, 12000.0, 1300.0 to 3300.0, 5700.0 to 6800.0, 9000.0 to 10800.0)
        wallY(0.0, ext, ext, 8000 - ext, 1500.0 to 3400.0)
        wallY(12000 - ext, 12000.0, ext, 8000 - ext, 1500.0 to 3000.0, 5800.0 to 7200.0)
        windowX(0.0, ext, 1200.0, 3200.0)
        windowX(0.0, ext, 4000.0, 5500.0)
        windowX(8000 - ext, 8000.0, 1300.0, 3300.0)
        windowX(8000 - ext, 8000.0, 5700.0, 6800.0)
        windowX(8000 - ext, 8000.0, 9000.0, 10800.0)
        windowY(0.0, ext, 1500.0, 3400.0)
        windowY(12000 - ext, 12000.0, 1500.0, 3000.0)
        windowY(12000 - ext, 12000.0, 5800.0, 7200.0)
        wallX(4800 - int / 2, 4800 + int / 2, ext, 12000 - ext, 3800.0 to 4700.0, 5560.0 to 6360.0, 7800.0 to 8700.0)
        wallY(6500 - int / 2, 6500 + int / 2, ext, 4800 - int / 2, 1300.0 to 3600.0)
        wallY(5000 - int / 2, 5000 + int / 2, 4800 + int / 2, 8000 - ext)
        wallY(7500 - int / 2, 7500 + int / 2, 4800 + int / 2, 8000 - ext)
        door(4700.0, 4860.0, 900.0, 180.0, -90.0)
        door(6360.0, 4860.0, 800.0, 180.0, -90.0)
        door(7800.0, 4860.0, 900.0, 0.0, 90.0)
        door(10000.0, ext, 1000.0, 180.0, -90.0)
        // Living room: sofa, coffee table, armchair, TV stand, rug
        rect("A-FURN", 700.0, 700.0, 3300.0, 1600.0)
        rect("A-FURN", 700.0, 700.0, 3300.0, 950.0)
        line("A-FURN", 1567.0, 950.0, 1567.0, 1600.0)
        line("A-FURN", 2433.0, 950.0, 2433.0, 1600.0)
        rect("A-FURN", 1400.0, 2100.0, 2600.0, 2800.0)
        rect("A-FURN", 3900.0, 1500.0, 4800.0, 2400.0)
        rect("A-FURN", 3900.0, 1500.0, 4800.0, 1700.0)
        rect("A-FURN", 900.0, 4300.0, 3100.0, 4680.0)
        line("A-FURN", 900.0, 1850.0, 3700.0, 1850.0, dashed = true)
        line("A-FURN", 3700.0, 1850.0, 3700.0, 3500.0, dashed = true)
        line("A-FURN", 3700.0, 3500.0, 900.0, 3500.0, dashed = true)
        line("A-FURN", 900.0, 3500.0, 900.0, 1850.0, dashed = true)
        // Kitchen: L-shaped counter, sink, stove, dining table and chairs
        poly("A-FURN", 11150.0, 600.0, 11750.0, 600.0, 11750.0, 4740.0, 8800.0, 4740.0, 8800.0, 4140.0, 11150.0, 4140.0)
        rect("A-FURN", 11250.0, 2000.0, 11650.0, 2900.0)
        circle("A-FURN", 11450.0, 2450.0, 120.0)
        for ((x, y) in listOf(9500.0 to 4300.0, 9900.0 to 4300.0, 9500.0 to 4580.0, 9900.0 to 4580.0)) circle("A-FURN", x, y, 110.0)
        circle("A-FURN", 8600.0, 2500.0, 600.0)
        rect("A-FURN", 8375.0, 3150.0, 8825.0, 3550.0)
        rect("A-FURN", 8375.0, 1450.0, 8825.0, 1850.0)
        rect("A-FURN", 7550.0, 2275.0, 7950.0, 2725.0)
        rect("A-FURN", 9250.0, 2275.0, 9650.0, 2725.0)
        // Bedroom: bed and pillows, nightstands, wardrobe
        rect("A-FURN", 900.0, 5500.0, 2600.0, 7600.0)
        rect("A-FURN", 1050.0, 7050.0, 1650.0, 7450.0)
        rect("A-FURN", 1850.0, 7050.0, 2450.0, 7450.0)
        line("A-FURN", 900.0, 6800.0, 2600.0, 6800.0)
        line("A-FURN", 900.0, 6800.0, 2600.0, 6400.0)
        rect("A-FURN", 350.0, 7150.0, 800.0, 7600.0)
        rect("A-FURN", 2700.0, 7150.0, 3150.0, 7600.0)
        rect("A-FURN", 3700.0, 7100.0, 4850.0, 7700.0)
        line("A-FURN", 3700.0, 7100.0, 4850.0, 7700.0)
        // Bathroom: bathtub, toilet, washbasin
        rect("A-FURN", 5150.0, 6950.0, 7300.0, 7700.0)
        rect("A-FURN", 5300.0, 7050.0, 7150.0, 7600.0)
        circle("A-FURN", 6900.0, 7325.0, 60.0)
        rect("A-FURN", 5150.0, 5600.0, 5550.0, 6100.0)
        arc("A-FURN", 5850.0, 5850.0, 300.0, 90.0, 180.0)
        line("A-FURN", 5850.0, 5550.0, 5850.0, 6150.0)
        rect("A-FURN", 6800.0, 5050.0, 7350.0, 5550.0)
        circle("A-FURN", 7075.0, 5300.0, 170.0)
        // Bedroom 2: bed, desk, chair
        rect("A-FURN", 9600.0, 5300.0, 11400.0, 7300.0)
        rect("A-FURN", 9800.0, 6850.0, 10400.0, 7200.0)
        rect("A-FURN", 10600.0, 6850.0, 11200.0, 7200.0)
        line("A-FURN", 9600.0, 6600.0, 11400.0, 6600.0)
        rect("A-FURN", 7700.0, 7100.0, 9000.0, 7700.0)
        circle("A-FURN", 8350.0, 6750.0, 230.0)
        // Grid lines
        line("CENTER", 6500.0, -400.0, 6500.0, 8400.0, dashed = true)
        line("CENTER", -400.0, 4800.0, 12400.0, 4800.0, dashed = true)
        // Dimensions
        dimX(0.0, 12000.0, -1300.0, -300.0)
        dimX(0.0, 6500.0, -700.0, -300.0)
        dimX(6500.0, 12000.0, -700.0, -300.0)
        dimY(0.0, 8000.0, -1300.0, -300.0)
        dimY(0.0, 4800.0, -700.0, -300.0)
        dimY(4800.0, 8000.0, -700.0, -300.0)
        // Room names and areas
        text("Living Room", 3250.0, 3150.0, 230.0)
        text("30.1 m²", 3250.0, 2830.0, 170.0)
        text("Kitchen / Dining", 9300.0, 3700.0, 230.0)
        text("24.3 m²", 9300.0, 3380.0, 170.0)
        text("Bedroom", 2500.0, 6200.0, 230.0)
        text("14.9 m²", 2500.0, 5880.0, 170.0)
        text("Bathroom", 6250.0, 6550.0, 200.0)
        text("Bedroom 2", 9800.0, 5950.0, 200.0)
        text("Floor Plan  1:100", 6000.0, -2000.0, 280.0)
    }

    private companion object {
        val LAYER_ORDER = listOf("CENTER", "A-FURN", "A-GLAZ", "A-DOOR", "A-WALL", "A-ANNO-DIMS")

        /** Layer colors (dark, light). */
        val LAYER_COLORS = mapOf(
            "A-WALL" to ("#FFC9CFD6" to "#FF3A414A"),
            "A-DOOR" to ("#FFFFD54F" to "#FFB07A00"),
            "A-GLAZ" to ("#FF4DD0E1" to "#FF00838F"),
            "A-FURN" to ("#FF9CCC65" to "#FF4E7D2A"),
            "A-ANNO-DIMS" to ("#FFEF6B68" to "#FFC62828"),
            "CENTER" to ("#FFB388FF" to "#FF6A3DB8"),
        )
        val DEFAULT_COLORS = "#FFE8ECF0" to "#FF1F242B"
        const val MIN_X = -1800.0
        const val MAX_X = 12600.0
        const val MIN_Y = -2400.0
        const val MAX_Y = 8600.0
        const val INSET_LEFT = 40.0
        const val INSET_TOP = 30.0
        const val INSET_RIGHT = 150.0
        const val INSET_BOTTOM = 130.0
        const val GRID_STEP = 1000.0
        const val DASH = 300.0
        const val GAP = 150.0
        const val DIM_EXT = 150.0
        const val DIM_TICK = 90.0
        const val DIM_TEXT = 200.0
        const val DIM_TEXT_GAP = 190.0
        const val DEFAULT_TEXT = 250.0
        const val DEGREES_HALF = 180.0
        const val DEGREES_FULL = 360.0
        const val MIN_FONT = 8.0
        const val FONT_RATIO = 1.3
        const val CHAR_RATIO = 0.3
        const val LINE_RATIO = 0.6
    }
}

private fun fmt(value: Double): String = "%.1f".format(java.util.Locale.ROOT, value).removeSuffix(".0")

/** Converts "#AARRGGBB" to a [WColor]. */
private fun argb(hex: String): WColor {
    val value = hex.removePrefix("#").toLong(HEX)
    return WColor((value shr 16 and 0xFF).toInt(), (value shr 8 and 0xFF).toInt(), (value and 0xFF).toInt(), (value shr 24 and 0xFF).toInt())
}

private const val HEX = 16
