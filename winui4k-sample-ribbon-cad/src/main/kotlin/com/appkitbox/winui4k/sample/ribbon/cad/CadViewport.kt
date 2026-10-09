package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WCanvas
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonIcon
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WRibbonToolBar
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonTextBoxModel
import com.appkitbox.winui4k.ribbon.RibbonToolBarModel
import com.appkitbox.winui4k.ribbon.RibbonToolBarOrientation

/**
 * The drawing area (same as the RibbonSpace CAD demo): the floor plan, the viewport caption, the UCS icon, the ViewCube,
 * the navigation bar, the multiline text editor frame, and the command line. Visibility is switched with the toggles on
 * the [View] tab.
 */
internal class CadViewport(ribbon: WRibbon) {
    private val canvas = WCanvas()
    private val overlay = WCanvas()

    /** The floor plan. */
    val drawing = CadDrawing(canvas)
    private val surface = WBorder(canvas)
    private val viewportLabel = WLabel("[-][Top][2D Wireframe]")
    private val ucsIcon = ucsIcon()
    private val cubeRing = WBorder()
    private val cubeFace = WBorder(label("TOP", BOLD))
    private val cubeLabels = listOf("N" to (0.0 to -44.0), "S" to (0.0 to 44.0), "W" to (-44.0 to 0.0), "E" to (44.0 to 0.0))
        .map { (text, offset) -> label(text, BOLD) to offset }
    private val wcsBadge = WBorder(label("WCS ⌄", 0))
    private val viewCube = viewCube()
    private val navBar = navBar(ribbon)
    private val mtextEditor = WBorder(WLabel("Open-plan living, oak floors").also { it.fontSize = MTEXT_FONT })

    /** The command line. */
    val commandLine = CadCommandLine(ribbon)

    /** The whole drawing area. */
    val element: WComponent

    init {
        val grid = WGrid()
        grid.add(surface, row = 0, column = 0)
        grid.add(overlay, row = 0, column = 0)
        viewportLabel.margin = LABEL_MARGIN
        viewportLabel.horizontalAlignment = HorizontalAlignment.LEFT
        viewportLabel.verticalAlignment = VerticalAlignment.TOP
        grid.add(viewportLabel, row = 0, column = 0)
        grid.add(ucsIcon, row = 0, column = 0)
        grid.add(viewCube, row = 0, column = 0)
        grid.add(navBar, row = 0, column = 0)
        grid.add(commandLine.element, row = 0, column = 0)
        mtextEditor.borderThickness = 1.0
        mtextEditor.padding = MTEXT_PADDING
        mtextEditor.isVisible = false
        overlay.add(mtextEditor, 0.0, 0.0)
        element = grid
        applyTheme(dark = true)
    }

    /** Visibility of the UCS icon, the ViewCube and the navigation bar. */
    fun setUcsIconVisible(visible: Boolean) {
        ucsIcon.isVisible = visible
    }

    fun setViewCubeVisible(visible: Boolean) {
        viewCube.isVisible = visible
    }

    fun setNavBarVisible(visible: Boolean) {
        navBar.isVisible = visible
    }

    /** Shows the multiline text editor frame over the drawing (together with the [Text Editor] contextual tab). */
    fun showTextEditor(visible: Boolean) {
        if (visible) {
            val (left, top) = drawing.modelToScreen(MTEXT_LEFT, MTEXT_TOP)
            val (right, _) = drawing.modelToScreen(MTEXT_RIGHT, MTEXT_TOP)
            mtextEditor.width = maxOf(MTEXT_MIN_WIDTH, right - left)
            overlay.setLocation(mtextEditor, left, top)
        }
        mtextEditor.isVisible = visible
    }

    /** Applies the dark / light colors. */
    fun applyTheme(dark: Boolean) {
        drawing.isDark = dark
        surface.background = drawing.canvasColor
        val text = if (dark) WColor(0xE1, 0xE6, 0xEC) else WColor(0x2F, 0x37, 0x42)
        viewportLabel.foreground = if (dark) WColor(0xC9, 0xD1, 0xDB) else WColor(0x3F, 0x48, 0x55)
        cubeRing.borderColor = if (dark) WColor(0x5F, 0x6E, 0x86) else WColor(0xA9, 0xB4, 0xC2)
        cubeFace.background = if (dark) WColor(0x4A, 0x55, 0x68) else WColor(0xF7, 0xF8, 0xF9)
        cubeFace.borderColor = if (dark) WColor(0x8C, 0x99, 0xAD) else WColor(0x8C, 0x97, 0xA5)
        wcsBadge.background = if (dark) WColor(0x3B, 0x44, 0x53) else WColor(0xF0, 0xF1, 0xF3)
        wcsBadge.borderColor = cubeFace.borderColor
        ((cubeFace.child as WLabel)).foreground = text
        (wcsBadge.child as WLabel).foreground = text
        cubeLabels.forEach { (label, _) -> label.foreground = text }
        mtextEditor.borderColor = text
        (mtextEditor.child as WLabel).foreground = if (dark) WColor(0xF2, 0xF2, 0xF2) else WColor(0x1F, 0x24, 0x2B)
        commandLine.applyTheme(dark)
    }

    private fun label(text: String, weight: Int) = WLabel(text).also {
        it.fontSize = CUBE_FONT
        if (weight > 0) it.fontWeight = weight
        it.horizontalAlignment = HorizontalAlignment.CENTER
        it.verticalAlignment = VerticalAlignment.CENTER
    }

    /** The UCS icon (green arrow for Y, red arrow for X). */
    private fun ucsIcon(): WComponent {
        val box = WCanvas()
        box.width = UCS_SIZE
        box.height = UCS_SIZE
        val art = "[viewbox=70;stroke=2;color=#5CB85C]M8,62 L8,10|[color=#5CB85C]M3,16 L8,6 L13,16 Z|[stroke=2;color=#E05A5A]M8,62 L60,62" +
            "|[color=#E05A5A]M54,57 L64,62 L54,67 Z|[stroke=1.2;color=#C9CFD6]M4,58 L12,58 L12,66 L4,66 Z"
        box.add(WRibbonIcon(RibbonIcon.path(art, UCS_SIZE), UCS_SIZE), 0.0, 0.0)
        box.add(WLabel("Y").also { it.foreground = WColor(0x5C, 0xB8, 0x5C) }, 14.0, 0.0)
        box.add(WLabel("X").also { it.foreground = WColor(0xE0, 0x5A, 0x5A) }, 58.0, 40.0)
        box.horizontalAlignment = HorizontalAlignment.LEFT
        box.verticalAlignment = VerticalAlignment.BOTTOM
        box.setMargin(UCS_MARGIN_LEFT, 0.0, 0.0, UCS_MARGIN_BOTTOM)
        return box
    }

    /** The ViewCube (top view, with compass directions and WCS). */
    private fun viewCube(): WComponent {
        val cube = WCanvas()
        cube.width = CUBE_WIDTH
        cube.height = CUBE_HEIGHT
        cubeRing.width = RING
        cubeRing.height = RING
        cubeRing.cornerRadius = RING / 2
        cubeRing.borderThickness = RING_THICKNESS
        cubeRing.opacity = RING_OPACITY
        cube.add(cubeRing, (CUBE_WIDTH - RING) / 2, 2.0)
        cubeFace.width = FACE
        cubeFace.height = FACE
        cubeFace.borderThickness = 1.0
        cube.add(cubeFace, (CUBE_WIDTH - FACE) / 2, 2 + (RING - FACE) / 2)
        val center = CUBE_WIDTH / 2 to 2 + RING / 2
        for ((label, offset) in cubeLabels) cube.add(label, center.first + offset.first - LETTER / 2, center.second + offset.second - LETTER / 2)
        wcsBadge.cornerRadius = 2.0
        wcsBadge.borderThickness = 1.0
        wcsBadge.setPadding(8.0, 1.0, 8.0, 1.0)
        cube.add(wcsBadge, CUBE_WIDTH / 2 - BADGE_HALF, CUBE_HEIGHT - BADGE_HEIGHT)
        cube.horizontalAlignment = HorizontalAlignment.RIGHT
        cube.verticalAlignment = VerticalAlignment.TOP
        cube.setMargin(0.0, CUBE_MARGIN_TOP, CUBE_MARGIN_RIGHT, 0.0)
        return cube
    }

    /** The navigation bar (Full Navigation Wheel, Pan, Zoom, Orbit). */
    private fun navBar(ribbon: WRibbon): WComponent {
        val model = RibbonToolBarModel("nav")
        model.orientation = RibbonToolBarOrientation.VERTICAL
        model.items.add(cmd("nav.wheel", "Full Navigation Wheel", ic(CadIcons.ORBIT), RibbonItemSize.SMALL))
        model.items.add(tgl("nav.pan", "Pan", ic(CadIcons.PAN), RibbonItemSize.SMALL))
        model.items.add(
            spl(
                "nav.zoomExtents",
                "Zoom Extents",
                ic(CadIcons.ZOOM_EXTENTS),
                RibbonItemSize.SMALL,
                true,
                mi("nav.zoom.extents", "Zoom Extents", ic(CadIcons.ZOOM_EXTENTS)),
                mi("nav.zoom.window", "Zoom Window", ic(CadIcons.ZOOM_WINDOW)),
                mi("nav.zoom.in", "Zoom In", ic(CadIcons.ZOOM_WINDOW)),
                mi("nav.zoom.out", "Zoom Out", ic(CadIcons.ZOOM_WINDOW)),
            ),
        )
        model.items.add(cmd("nav.orbit", "Orbit", ic(CadIcons.ORBIT), RibbonItemSize.SMALL))
        val bar = WRibbonToolBar(model)
        bar.ribbon = ribbon
        val frame = WRibbonTheme.surface(bar, "RibbonCommandBarBackgroundBrush")
        frame.horizontalAlignment = HorizontalAlignment.RIGHT
        frame.verticalAlignment = VerticalAlignment.TOP
        frame.setMargin(0.0, NAV_MARGIN_TOP, NAV_MARGIN_RIGHT, 0.0)
        return frame
    }

    private companion object {
        const val BOLD = 600
        const val CUBE_FONT = 11.0
        const val LABEL_MARGIN = 8.0
        const val UCS_SIZE = 70.0
        const val UCS_MARGIN_LEFT = 14.0
        const val UCS_MARGIN_BOTTOM = 120.0
        const val CUBE_WIDTH = 110.0
        const val CUBE_HEIGHT = 128.0
        const val RING = 96.0
        const val RING_THICKNESS = 9.0
        const val RING_OPACITY = 0.55
        const val FACE = 46.0
        const val LETTER = 12.0
        const val BADGE_HALF = 26.0
        const val BADGE_HEIGHT = 20.0
        const val CUBE_MARGIN_TOP = 10.0
        const val CUBE_MARGIN_RIGHT = 46.0
        const val NAV_MARGIN_TOP = 148.0
        const val NAV_MARGIN_RIGHT = 8.0
        const val MTEXT_FONT = 12.0
        const val MTEXT_PADDING = 6.0
        const val MTEXT_LEFT = 3700.0
        const val MTEXT_TOP = 4450.0
        const val MTEXT_RIGHT = 6300.0
        const val MTEXT_MIN_WIDTH = 260.0
    }
}

/** The command line (history of the last 3 lines and the field for typing commands). */
internal class CadCommandLine(ribbon: WRibbon) {
    private val history = mutableListOf("Command: _.OPEN \"Drawing1.dwg\"", "Regenerating model.", "Command:")
    private val historyLabel = WLabel()
    private val historyBox = WBorder(historyLabel)

    /** The field for typing commands (pressing Enter invokes "command.input" with the typed string as its argument). */
    val input = RibbonTextBoxModel("command.input").also {
        it.placeholder = "Type a command"
        it.inputWidth = INPUT_WIDTH
    }
    private val bar: WRibbonToolBar

    /** The whole command line. */
    val element: WComponent

    init {
        historyLabel.fontFamily = MONOSPACE
        historyLabel.fontSize = FONT
        historyBox.setPadding(10.0, 4.0, 10.0, 4.0)
        val model = RibbonToolBarModel("commandLine")
        model.showLabels = true
        model.isOverflowEnabled = false
        model.items.add(cmd("command.close", "Close Command Line", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
        model.items.add(cmd("command.customize", "Customize", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
        model.items.add(input)
        model.items.add(cmd("command.recent", "Recent Commands", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
        bar = WRibbonToolBar(model)
        bar.ribbon = ribbon
        val panel = WPanel(spacing = 0.0, orientation = Orientation.VERTICAL)
        panel.add(historyBox)
        panel.add(WRibbonTheme.surface(bar, "RibbonCommandBarBackgroundBrush"))
        panel.verticalAlignment = VerticalAlignment.BOTTOM
        panel.horizontalAlignment = HorizontalAlignment.CENTER
        panel.setMargin(MARGIN_SIDE, 0.0, MARGIN_SIDE, MARGIN_BOTTOM)
        element = panel
        update()
    }

    /** Appends a line to the history (replacing the previous line if it is an empty "Command:", as AutoCAD does). */
    fun echo(line: String) {
        if (history.lastOrNull() == "Command:") history.removeAt(history.size - 1)
        history += line
        while (history.size > MAX_HISTORY) history.removeAt(0)
        update()
    }

    /** Clears the typed text. */
    fun clearInput() {
        input.text = ""
    }

    fun applyTheme(dark: Boolean) {
        historyBox.background = if (dark) WColor(0x2B, 0x31, 0x3B, 0xD9) else WColor(0xF0, 0xF1, 0xF3, 0xE6)
        historyLabel.foreground = if (dark) WColor(0xAE, 0xB8, 0xC4) else WColor(0x4E, 0x58, 0x66)
    }

    private fun update() {
        historyLabel.text = history.takeLast(VISIBLE_LINES).joinToString("\n")
    }

    private companion object {
        const val MONOSPACE = "Consolas"
        const val FONT = 12.0
        const val INPUT_WIDTH = 760.0
        const val MAX_HISTORY = 40
        const val VISIBLE_LINES = 3
        const val MARGIN_SIDE = 70.0
        const val MARGIN_BOTTOM = 10.0
    }
}
