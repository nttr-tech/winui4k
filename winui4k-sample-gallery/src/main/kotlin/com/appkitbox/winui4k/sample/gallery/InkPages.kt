package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.CoreInputDeviceType
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.InkHighContrastAdjustment
import com.appkitbox.winui4k.InkInputProcessingMode
import com.appkitbox.winui4k.InkPersistenceFormat
import com.appkitbox.winui4k.InkPointerEvent
import com.appkitbox.winui4k.InkStrokeInputListener
import com.appkitbox.winui4k.InkToolbarButtonFlyoutPlacement
import com.appkitbox.winui4k.InkToolbarInitialControls
import com.appkitbox.winui4k.InkToolbarMenuKind
import com.appkitbox.winui4k.InkToolbarStencilKind
import com.appkitbox.winui4k.InkToolbarTool
import com.appkitbox.winui4k.InkUnprocessedInputListener
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFileOpenPicker
import com.appkitbox.winui4k.WFileSavePicker
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WInfoBar
import com.appkitbox.winui4k.WInkCanvas
import com.appkitbox.winui4k.WInkToolbar
import com.appkitbox.winui4k.WInkToolbarCustomPen
import com.appkitbox.winui4k.WInkToolbarCustomPenButton
import com.appkitbox.winui4k.WInkToolbarCustomToggleButton
import com.appkitbox.winui4k.WInkToolbarCustomToolButton
import com.appkitbox.winui4k.WInkToolbarEraserButton
import com.appkitbox.winui4k.WInkToolbarPenButton
import com.appkitbox.winui4k.WInkToolbarPenConfigurationControl
import com.appkitbox.winui4k.WInkToolbarStencilButton
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.ink.DefaultInkStrokeModel
import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.ink.InkPoint
import com.appkitbox.winui4k.ink.InkRect
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.ink.InkStrokeModelEvent
import com.appkitbox.winui4k.ink.InkTransform
import com.appkitbox.winui4k.ink.PenTipShape
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/*
 * Ink category: demo pages for InkCanvas / InkToolbar (Windows App SDK 2.5 experimental).
 */

/** Allows drawing with mouse, pen, and touch alike (InkCanvas defaults to pen only). */
private val ALL_DEVICES = setOf(CoreInputDeviceType.PEN, CoreInputDeviceType.MOUSE, CoreInputDeviceType.TOUCH)

/** Color choices (name and color). */
private val INK_COLORS = listOf(
    "Black" to WColor.BLACK,
    "Red" to WColor.RED,
    "Green" to WColor.GREEN,
    "Blue" to WColor.BLUE,
    "Orange" to WColor.ORANGE,
    "Purple" to WColor.PURPLE,
)

/** A label that shows a Segoe Fluent Icons glyph (the code point equals the Symbol value). */
private fun glyph(codePoint: Int): WLabel = WLabel(String(Character.toChars(codePoint))).also {
    it.fontFamily = "Segoe Fluent Icons"
    it.fontSize = 16.0
}

/** Code points of Symbol.EDIT / SELECT_ALL / TOUCH_POINTER. */
private const val GLYPH_EDIT = 0xE104
private const val GLYPH_SELECT_ALL = 0xE14E
private const val GLYPH_TOUCH_POINTER = 0xE1E3

/** A canvas placed on white paper so the drawn ink is visible (black ink stays visible even in the dark theme). */
private fun inkSurface(canvas: WInkCanvas, width: Double = 560.0, height: Double = 260.0): WBorder {
    canvas.width = width
    canvas.height = height
    val paper = WBorder(canvas)
    paper.background = WColor.WHITE
    paper.borderColor = CARD_BORDER
    paper.borderThickness = 1.0
    paper.cornerRadius = 4.0
    return paper
}

/** A single wavy stroke (an example of adding to the model from code). The pressure increases gradually. */
private fun waveStroke(top: Double, attributes: InkDrawingAttributes): InkStroke {
    val points = (0..60).map { i ->
        InkPoint(30.0 + i * 8.0, top + 20.0 * sin(i / 6.0 * PI / 2), pressure = (0.2 + 0.6 * i / 60).toFloat())
    }
    return InkStroke(points, attributes)
}

/** Describes a pointer event. If the arguments cannot be read by the time WinUI delivers it, says so. */
private fun describe(event: InkPointerEvent): String {
    val point = event.pointerPoint ?: return "Position and other details could not be retrieved (a limitation of WinUI 2.5 experimental)"
    return "${point.deviceType} (%.0f, %.0f) pressure %.2f modifiers ${event.modifiers}".format(point.x, point.y, point.pressure)
}

private fun describe(rect: InkRect): String =
    if (rect.isEmpty) "None" else "x=%.0f y=%.0f width=%.0f height=%.0f".format(rect.x, rect.y, rect.width, rect.height)

// region InkCanvas page

/** The InkCanvas page: a guide plus demos of input, drawing attributes, MVC, selection, saving, events, and custom drying. */
internal fun buildInkCanvasPage(): WComponent {
    val page = buildPage(
        "InkCanvas",
        "A canvas for drawing handwritten strokes (ink) with a pen, mouse, or touch. Like Swing's JTable and TableModel, " +
            "the stroke data is held by InkStrokeModel (com.appkitbox.winui4k.ink), and WInkCanvas handles its display and input.",
    )
    page.add(buildInkCanvasGuideExample())
    page.add(buildInkCanvasBasicExample())
    page.add(buildInkDrawingAttributesExample())
    page.add(buildInkModelExample())
    page.add(buildInkSelectionExample())
    page.add(buildInkSaveLoadExample())
    page.add(buildInkInputEventsExample())
    page.add(buildInkCustomDryingExample())
    return page
}

/** Guide: the roles of the classes involved. */
private fun buildInkCanvasGuideExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel("• WInkCanvas — the canvas that draws strokes (the view). Writes drawn and erased strokes back to the model"))
    body.add(WLabel("• WInkPresenter — input devices, processing mode, drawing attributes for new strokes, and input events (canvas.inkPresenter)"))
    body.add(WLabel("• InkStrokeModel / DefaultInkStrokeModel — the model of the stroke sequence. One model can be shared by several canvases"))
    body.add(WLabel("• InkStroke / InkPoint / InkDrawingAttributes — value objects for strokes, points, and drawing attributes (independent of WinUI)"))
    body.add(
        WLabel(
            "Strokes added to, replaced in, or removed from the model appear on the canvas, and strokes drawn on the canvas, strokes erased with the eraser, " +
                "and moving or deleting a selection are reflected in the model.",
        ).also { it.textWrapping = TextWrapping.WRAP },
    )
    val note = WInfoBar()
    note.isOpen = true
    note.isClosable = false
    note.message = "InkCanvas is an experimental feature of Windows App SDK 2.5. It accepts only pen input by default, so " +
        "the canvases on this page also allow drawing with mouse and touch."
    body.add(note)
    return buildExample("Guide: the roles of the classes", body)
}

/** Basics: input devices, enabling input, processing mode, and high contrast. */
private fun buildInkCanvasBasicExample(): WComponent {
    val canvas = WInkCanvas()
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val count = WLabel("Strokes: 0")
    canvas.model.addInkStrokeModelListener { count.text = "Strokes: ${canvas.model.getStrokeCount()}" }

    val pen = WCheckBox("Pen").also { it.isSelected = true }
    val mouse = WCheckBox("Mouse").also { it.isSelected = true }
    val touch = WCheckBox("Touch").also { it.isSelected = true }
    val updateDevices = { _: Boolean? ->
        canvas.inkPresenter.inputDeviceTypes = buildSet {
            if (pen.isSelected) add(CoreInputDeviceType.PEN)
            if (mouse.isSelected) add(CoreInputDeviceType.MOUSE)
            if (touch.isSelected) add(CoreInputDeviceType.TOUCH)
        }
    }
    pen.addItemListener(updateDevices)
    mouse.addItemListener(updateDevices)
    touch.addItemListener(updateDevices)

    val enabled = WToggleSwitch().also { it.isOn = true }
    enabled.addItemListener { canvas.inkPresenter.isInputEnabled = it }

    val mode = WComboBox(listOf("Draw (INKING)", "Erase (ERASING)", "No processing (NONE)"))
    mode.selectedIndex = 0
    mode.addListSelectionListener { canvas.inkPresenter.inputProcessingMode = InkInputProcessingMode.entries[mode.selectedIndex] }

    val highContrast = WComboBox(InkHighContrastAdjustment.entries.map { it.name })
    highContrast.selectedIndex = 0
    highContrast.addListSelectionListener {
        canvas.inkPresenter.highContrastAdjustment = InkHighContrastAdjustment.entries[highContrast.selectedIndex]
    }

    val clear = WButton("Erase all")
    clear.addActionListener { canvas.model.clear() }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(count)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Input devices (InputDeviceTypes)"))
    options.add(pen)
    options.add(mouse)
    options.add(touch)
    options.add(optionsLabel("Accept input (IsInputEnabled)"))
    options.add(enabled)
    options.add(optionsLabel("Processing mode (InputProcessingConfiguration.Mode)"))
    options.add(mode)
    options.add(optionsLabel("Colors in high contrast (HighContrastAdjustment)"))
    options.add(highContrast)
    options.add(clear)
    return buildExample("Drawing and erasing strokes (WInkCanvas / WInkPresenter)", example, options)
}

/** The controls for the drawing attributes demo, and building drawing attributes from their state. */
private class InkAttributeControls {
    val color = WComboBox(INK_COLORS.map { it.first }).also { it.selectedIndex = 0 }
    val size = WSlider(minimum = 1.0, maximum = 24.0, value = 4.0)
    val rectangleTip = WToggleSwitch().also { it.isOn = false }
    val rotate = WToggleSwitch().also { it.isOn = false }
    val highlighter = WToggleSwitch().also { it.isOn = false }
    val pencil = WToggleSwitch().also { it.isOn = false }
    val pencilOpacity = WSlider(minimum = 0.1, maximum = 5.0, value = 1.0).also { it.stepFrequency = 0.1 }
    val fitToCurve = WToggleSwitch().also { it.isOn = true }
    val ignorePressure = WToggleSwitch().also { it.isOn = false }

    /** Items that cannot be changed for the pencil (pen tip shape, transform, highlighter). */
    private val penOnly = listOf(rectangleTip, rotate, highlighter)

    /** Calls [onChange] when a value changes. */
    fun addChangeListener(onChange: () -> Unit) {
        color.addListSelectionListener(onChange)
        size.addChangeListener { onChange() }
        pencilOpacity.addChangeListener { onChange() }
        (penOnly + listOf(pencil, fitToCurve, ignorePressure)).forEach { switch -> switch.addItemListener { onChange() } }
    }

    /** The drawing attributes for the controls' current state. Disables the pen-only items for the pencil. */
    fun attributes(): InkDrawingAttributes {
        penOnly.forEach { it.isEnabled = !pencil.isOn }
        pencilOpacity.isEnabled = pencil.isOn
        val builder = if (pencil.isOn) {
            InkDrawingAttributes.pencilBuilder().pencilOpacity(pencilOpacity.value)
        } else {
            InkDrawingAttributes.builder()
                .penTip(if (rectangleTip.isOn) PenTipShape.RECTANGLE else PenTipShape.CIRCLE)
                .penTipTransform(if (rotate.isOn) InkTransform.rotation(PI / 4) else InkTransform.IDENTITY)
                .drawAsHighlighter(highlighter.isOn)
        }
        return builder
            .color(INK_COLORS[color.selectedIndex.coerceAtLeast(0)].second)
            .size(size.value)
            .fitToCurve(fitToCurve.isOn)
            .ignorePressure(ignorePressure.isOn)
            .build()
    }

    /** The Options panel. */
    fun options(): WPanel {
        val options = WPanel(spacing = 6.0)
        listOf(
            "Color (Color)" to color,
            "Size (Size)" to size,
            "Rectangular pen tip (PenTip)" to rectangleTip,
            "Rotate the pen tip 45° (PenTipTransform)" to rotate,
            "Highlighter (DrawAsHighlighter)" to highlighter,
            "Pencil (CreateForPencil)" to pencil,
            "Pencil opacity (PencilProperties.Opacity)" to pencilOpacity,
            "Fit to curve (FitToCurve)" to fitToCurve,
            "Ignore pressure (IgnorePressure)" to ignorePressure,
        ).forEach { (label, control) ->
            options.add(optionsLabel(label))
            options.add(control)
        }
        return options
    }
}

/** Drawing attributes: the color, size, pen tip, highlighter, and pencil for new strokes. */
private fun buildInkDrawingAttributesExample(): WComponent {
    val canvas = WInkCanvas()
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val controls = InkAttributeControls()
    val current = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val apply = {
        canvas.inkPresenter.defaultDrawingAttributes = controls.attributes()
        val read = canvas.inkPresenter.defaultDrawingAttributes
        current.text = "Current: ${read.kind} / size ${read.width} / pen tip ${read.penTip} / highlighter ${read.drawAsHighlighter}"
    }
    controls.addChangeListener(apply)
    apply()

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(current)
    return buildExample("How new strokes are drawn (WInkPresenter.defaultDrawingAttributes)", example, controls.options())
}

/** MVC: two canvases share one model, and the model is manipulated from code. */
private fun buildInkModelExample(): WComponent {
    val model = DefaultInkStrokeModel()
    val left = WInkCanvas(model).also { it.inkPresenter.inputDeviceTypes = ALL_DEVICES }
    val right = WInkCanvas(model).also { it.inkPresenter.inputDeviceTypes = ALL_DEVICES }
    val log = WLabel("Model change: none").also { it.textWrapping = TextWrapping.WRAP }
    val undone = ArrayDeque<InkStroke>()
    model.addInkStrokeModelListener { e ->
        val kind = when (e.type) {
            InkStrokeModelEvent.INSERT -> "Insert"
            InkStrokeModelEvent.DELETE -> "Delete"
            else -> if (e.isDataChanged) "Whole change" else "Replace"
        }
        val range = if (e.isDataChanged) "" else " (${e.firstIndex}-${e.lastIndex})"
        log.text = "Model change: $kind$range / strokes: ${model.getStrokeCount()}"
    }
    // When a new stroke is drawn, discard the redo history
    left.inkPresenter.addStrokesCollectedListener { undone.clear() }
    right.inkPresenter.addStrokesCollectedListener { undone.clear() }

    val addWave = WButton("Add a wavy line from code")
    addWave.addActionListener {
        val top = 40.0 + (model.getStrokeCount() % 5) * 40.0
        model.addStroke(waveStroke(top, InkDrawingAttributes.builder().color(WColor.BLUE).size(4.0).build()))
    }
    val undo = WButton("Undo the last stroke")
    undo.addActionListener {
        val last = model.getStrokeCount() - 1
        if (last >= 0) {
            undone.addLast(model.getStroke(last))
            model.removeStroke(last)
        }
    }
    val redo = WButton("Redo")
    redo.addActionListener { undone.removeLastOrNull()?.let { model.addStroke(it) } }
    val recolor = WButton("Make all strokes red")
    recolor.addActionListener {
        for (i in 0 until model.getStrokeCount()) {
            val stroke = model.getStroke(i)
            model.setStroke(i, stroke.withDrawingAttributes(stroke.drawingAttributes.toBuilder().color(WColor.RED).build()))
        }
    }
    val shift = WButton("Shift all strokes right by 20")
    shift.addActionListener {
        for (i in 0 until model.getStrokeCount()) {
            val stroke = model.getStroke(i)
            model.setStroke(i, stroke.withPointTransform(stroke.pointTransform.then(InkTransform.translation(20.0, 0.0))))
        }
    }
    val clear = WButton("Empty the model")
    clear.addActionListener { model.clear() }

    val canvases = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    canvases.add(inkSurface(left, width = 300.0))
    canvases.add(inkSurface(right, width = 300.0))
    val example = WPanel(spacing = 8.0)
    example.add(canvases)
    example.add(log)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Whichever canvas you draw on, the stroke also appears on the other one, which shows the same model."))
    listOf(addWave, undo, redo, recolor, shift, clear).forEach { options.add(it) }
    return buildExample("Sharing one model between two canvases (InkStrokeModel)", example, options)
}

/**
 * Draws a lasso from [canvas]'s unprocessed input (UnprocessedInput) and selects the strokes it encloses.
 * Calls [onSelected] with the bounds after selecting, or [onUnavailable] if the positions could not be read and no lasso was formed.
 */
private fun installLasso(canvas: WInkCanvas, onSelected: (InkRect) -> Unit, onUnavailable: () -> Unit) {
    val lasso = mutableListOf<InkPoint>()
    canvas.inkPresenter.addUnprocessedInputListener(
        object : InkUnprocessedInputListener {
            override fun pointerPressed(event: InkPointerEvent) {
                lasso.clear()
                event.pointerPoint?.let { lasso += it.toInkPoint() }
            }

            override fun pointerMoved(event: InkPointerEvent) {
                event.pointerPoint?.let { if (lasso.isNotEmpty()) lasso += it.toInkPoint() }
            }

            override fun pointerReleased(event: InkPointerEvent) {
                if (lasso.size > 2) onSelected(canvas.selectWithPolyLine(lasso)) else onUnavailable()
                lasso.clear()
            }
        },
    )
}

/** Two wavy lines, blue and green (the initial content of the selection demo). */
private fun twoWaves(): List<InkStroke> = listOf(
    waveStroke(60.0, InkDrawingAttributes.builder().color(WColor.BLUE).size(4.0).build()),
    waveStroke(140.0, InkDrawingAttributes.builder().color(WColor.GREEN).size(6.0).build()),
)

/** Selection: select with a lasso, then move, delete, copy, and paste. */
private fun buildInkSelectionExample(): WComponent {
    val canvas = WInkCanvas(DefaultInkStrokeModel(twoWaves()))
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val info = WLabel("Selection: none").also { it.textWrapping = TextWrapping.WRAP }
    fun showSelection(rect: InkRect? = null) {
        val selected = canvas.getSelectedStrokeIndices()
        info.text = "Selection: ${selected.size} strokes ${selected.toList()}" + (rect?.let { " / bounds ${describe(it)}" } ?: "")
    }

    // Lasso: input in no-processing mode and right-drag in inking mode (the RightDragAction default) arrive at UnprocessedInput
    installLasso(canvas, { showSelection(it) }) {
        info.text = "Could not get the lasso's position (in WinUI 2.5 experimental, ink input positions sometimes cannot be read). " +
            "Try selecting with the buttons."
    }

    val selectMode = WToggleSwitch().also { it.isOn = false }
    selectMode.addItemListener { on ->
        canvas.inkPresenter.inputProcessingMode = if (on) InkInputProcessingMode.NONE else InkInputProcessingMode.INKING
    }
    val selectAll = WButton("Select all")
    selectAll.addActionListener {
        canvas.selectAll()
        showSelection()
    }
    val clearSelection = WButton("Clear selection")
    clearSelection.addActionListener {
        canvas.clearSelection()
        showSelection()
    }
    val byLine = WButton("Select strokes crossing the vertical line x=100")
    byLine.addActionListener { showSelection(canvas.selectWithLine(100.0, 0.0, 100.0, 260.0)) }
    val byPolyLine = WButton("Select by enclosing the top half")
    byPolyLine.addActionListener {
        val area = listOf(InkPoint(0.0, 0.0), InkPoint(560.0, 0.0), InkPoint(560.0, 110.0), InkPoint(0.0, 110.0))
        showSelection(canvas.selectWithPolyLine(area))
    }
    val move = WButton("Move the selection down and right")
    move.addActionListener { showSelection(canvas.moveSelected(30.0, 20.0)) }
    val delete = WButton("Delete the selection")
    delete.addActionListener { showSelection(canvas.deleteSelected()) }
    val copy = WButton("Copy the selection")
    copy.addActionListener {
        runCatching { canvas.copySelectedToClipboard() }.onFailure { info.text = "Could not copy: ${it.message}" }
    }
    val paste = WButton("Paste (to the top left)")
    paste.addActionListener {
        if (canvas.canPasteFromClipboard()) {
            showSelection(canvas.pasteFromClipboard(20.0, 20.0))
        } else {
            info.text = "There is no ink on the clipboard."
        }
    }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(info)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Select with a lasso (processing mode NONE). In inking mode you can also select by right-dragging with the mouse"))
    options.add(selectMode)
    listOf(selectAll, clearSelection, byLine, byPolyLine, move, delete, copy, paste).forEach { options.add(it) }
    return buildExample("Selection, moving, deleting, and the clipboard (selectWithPolyLine / moveSelected, etc.)", example, options)
}

/** Saving and loading: ISF / GIF with embedded ISF. */
private fun buildInkSaveLoadExample(): WComponent {
    val canvas = WInkCanvas(
        DefaultInkStrokeModel(listOf(waveStroke(100.0, InkDrawingAttributes.builder().color(WColor.PURPLE).size(5.0).build()))),
    )
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }

    val save = WButton("Save to file")
    save.addActionListener {
        runCatching {
            val picker = WFileSavePicker(galleryFrame)
            picker.suggestedFileName = "ink"
            picker.addFileTypeChoice("GIF image with embedded ISF", ".gif")
            picker.addFileTypeChoice("Ink Serialized Format", ".isf")
            picker.pickSaveFile { path ->
                if (path == null) {
                    result.text = "Canceled."
                    return@pickSaveFile
                }
                runCatching {
                    val format = if (path.endsWith(".isf", ignoreCase = true)) InkPersistenceFormat.ISF else InkPersistenceFormat.GIF_WITH_EMBEDDED_ISF
                    File(path).outputStream().use { canvas.write(it, format) }
                    result.text = "Saved ${canvas.model.getStrokeCount()} strokes to $path (${File(path).length()} bytes)."
                }.onFailure { result.text = "Could not save: ${it.message}" }
            }
        }.onFailure { result.text = "Could not save: ${it.message}" }
    }
    val load = WButton("Load from file")
    load.addActionListener {
        runCatching {
            val picker = WFileOpenPicker(galleryFrame)
            picker.addFileTypeFilter(".gif", ".isf")
            picker.pickSingleFile { path ->
                if (path == null) {
                    result.text = "Canceled."
                    return@pickSingleFile
                }
                runCatching {
                    File(path).inputStream().use { canvas.read(it) }
                    result.text = "Loaded ${canvas.model.getStrokeCount()} strokes from $path."
                }.onFailure { result.text = "Could not load: ${it.message}" }
            }
        }.onFailure { result.text = "Could not load: ${it.message}" }
    }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(result)
    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("The GIF can also be opened as an image. Loading replaces the strokes on the canvas and in the model."))
    options.add(save)
    options.add(load)
    return buildExample("Saving and loading (write / read; ISF and GIF with embedded ISF)", example, options)
}

/** Input events: StrokeInput and information about drawn strokes (StrokesCollected). */
private fun buildInkInputEventsExample(): WComponent {
    val canvas = WInkCanvas()
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val log = WLabel("Input events appear here.").also { it.textWrapping = TextWrapping.WRAP }
    val lines = ArrayDeque<String>()
    fun append(line: String) {
        lines.addLast(line)
        while (lines.size > 6) lines.removeFirst()
        log.text = lines.joinToString("\n")
    }
    var moves = 0
    canvas.inkPresenter.addStrokeInputListener(
        object : InkStrokeInputListener {
            override fun strokeStarted(event: InkPointerEvent) {
                moves = 0
                append("Stroke started: " + describe(event))
            }

            override fun strokeContinued(event: InkPointerEvent) {
                moves++
            }

            override fun strokeEnded(event: InkPointerEvent) {
                append("Stroke ended: $moves moves in between / " + describe(event))
            }

            override fun strokeCanceled(event: InkPointerEvent) {
                append("Canceled")
            }
        },
    )
    canvas.inkPresenter.addStrokesCollectedListener { event ->
        val index = canvas.model.getStrokeCount() - 1
        val stroke = event.strokes.last()
        append(
            "Added to the model: ID ${canvas.getStrokeId(index)} / ${stroke.points.size} points / " +
                "${canvas.getRenderingSegments(index).size} segments / bounds ${describe(canvas.getStrokeBounds(index))}",
        )
    }
    canvas.inkPresenter.addStrokesErasedListener { append("Erased ${it.strokes.size} strokes with the eraser") }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(log)
    return buildExample("Input events (StrokeInput / StrokesCollected / StrokesErased)", example)
}

/** Custom drying: the app receives finished strokes and shows them (in a different color) in another canvas's model. */
private fun buildInkCustomDryingExample(): WComponent {
    val dried = DefaultInkStrokeModel()
    val dryCanvas = WInkCanvas(dried).also { it.inkPresenter.isInputEnabled = false }
    val wetCanvas = WInkCanvas().also { it.inkPresenter.inputDeviceTypes = ALL_DEVICES }
    // Activate before strokes are shown (before the canvas is placed in the window)
    val synchronizer = wetCanvas.inkPresenter.activateCustomDrying()
    wetCanvas.inkPresenter.addStrokesCollectedListener {
        val strokes = synchronizer.beginDry()
        dried.addStrokes(strokes.map { it.withDrawingAttributes(it.drawingAttributes.toBuilder().color(WColor.BLUE).build()) })
        synchronizer.endDry()
    }
    val clear = WButton("Clear")
    clear.addActionListener { dried.clear() }

    // Overlay the canvas that receives input on top of the canvas that shows the dry ink
    val layers = WGrid()
    layers.addRow(GridLength.AUTO)
    layers.addColumn(GridLength.AUTO)
    layers.add(inkSurface(dryCanvas), row = 0, column = 0)
    wetCanvas.width = 560.0
    wetCanvas.height = 260.0
    layers.add(wetCanvas, row = 0, column = 0)

    val example = WPanel(spacing = 8.0)
    example.add(layers)
    example.add(
        WLabel(
            "Strokes drawn in black (wet ink) are received by the app when finished (beginDry) and added in blue to the model of the canvas below (endDry).",
        ).also { it.textWrapping = TextWrapping.WRAP },
    )
    example.add(clear)
    return buildExample("Custom drying of dry ink (activateCustomDrying / WInkSynchronizer)", example)
}

// endregion

// region InkToolbar page

/** The InkToolbar page: lines up demos of the built-in tools, appearance, custom buttons, and stencils. */
internal fun buildInkToolbarPage(): WComponent {
    val page = buildPage(
        "InkToolbar",
        "A toolbar for choosing the pen, pencil, highlighter, eraser, ruler, and protractor used with InkCanvas. " +
            "When bound to targetInkCanvas, the selected tool, color, and size are applied to the canvas.",
    )
    page.add(buildInkToolbarBasicExample())
    page.add(buildInkToolbarAppearanceExample())
    page.add(buildInkToolbarCustomExample())
    page.add(buildInkToolbarStencilExample())
    return page
}

/** Stacks a toolbar and a canvas vertically. */
private fun toolbarWithCanvas(toolbar: WInkToolbar, canvas: WInkCanvas): WPanel {
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    toolbar.targetInkCanvas = canvas
    val panel = WPanel(spacing = 8.0)
    panel.add(toolbar)
    panel.add(inkSurface(canvas))
    return panel
}

/** Basics: the built-in tools. Shows the selected tool, the drawing attributes, and Erase all ink. */
private fun buildInkToolbarBasicExample(): WComponent {
    val canvas = WInkCanvas()
    val toolbar = WInkToolbar()
    val status = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    fun showStatus(message: String = "") {
        val attributes = toolbar.inkDrawingAttributes
        val tool = toolbar.activeTool?.toolKind
        status.text = "Tool: $tool / color: ${attributes?.color?.let { "(${it.red}, ${it.green}, ${it.blue})" }} / " +
            "size: ${attributes?.width} / kind: ${attributes?.kind}" + (if (message.isEmpty()) "" else "\n$message")
    }
    toolbar.addActiveToolChangedListener { showStatus() }
    toolbar.addInkDrawingAttributesChangedListener { showStatus() }
    toolbar.addEraseAllClickedListener { showStatus("\"Erase all ink\": erasing ${canvas.model.getStrokeCount()} strokes") }
    toolbar.addLoadedListener { showStatus() }

    val example = toolbarWithCanvas(toolbar, canvas)
    example.add(status)
    return buildExample("Built-in tools (activeTool / inkDrawingAttributes / EraseAllClicked)", example)
}

/** Appearance: vertical placement, flyout placement, built-in buttons, and pen color choices. */
private fun buildInkToolbarAppearanceExample(): WComponent {
    val host = WPanel()
    val orientation = WComboBox(listOf("Horizontal (HORIZONTAL)", "Vertical (VERTICAL)")).also { it.selectedIndex = 1 }
    val placement = WComboBox(InkToolbarButtonFlyoutPlacement.entries.map { it.name }).also { it.selectedIndex = 0 }
    val initial = WComboBox(InkToolbarInitialControls.entries.map { it.name }).also { it.selectedIndex = 0 }
    val customPalette = WToggleSwitch().also { it.isOn = false }
    val clearAllVisible = WToggleSwitch().also { it.isOn = true }

    // InitialControls takes effect at load time, so rebuild the toolbar when a setting changes
    fun rebuild() {
        val canvas = WInkCanvas()
        canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
        val toolbar = WInkToolbar()
        toolbar.targetInkCanvas = canvas
        toolbar.orientation = if (orientation.selectedIndex == 1) Orientation.VERTICAL else Orientation.HORIZONTAL
        toolbar.buttonFlyoutPlacement = InkToolbarButtonFlyoutPlacement.entries[placement.selectedIndex.coerceAtLeast(0)]
        toolbar.initialControls = InkToolbarInitialControls.entries[initial.selectedIndex.coerceAtLeast(0)]
        toolbar.addLoadedListener {
            val pen = toolbar.getToolButton(InkToolbarTool.BALLPOINT_PEN) as? WInkToolbarPenButton
            if (pen != null && customPalette.isOn) {
                pen.palette = listOf(WColor.BLACK, WColor.RED, WColor.BLUE, WColor(0, 160, 120))
                pen.minStrokeWidth = 2.0
                pen.maxStrokeWidth = 12.0
                pen.selectedBrushIndex = 1
            }
            (toolbar.getToolButton(InkToolbarTool.ERASER) as? WInkToolbarEraserButton)?.isClearAllVisible = clearAllVisible.isOn
        }
        val layout = if (toolbar.orientation == Orientation.VERTICAL) {
            WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
        } else {
            WPanel(spacing = 8.0)
        }
        layout.add(toolbar)
        layout.add(inkSurface(canvas, width = 480.0))
        host.removeAll()
        host.add(layout)
    }
    orientation.addListSelectionListener { rebuild() }
    placement.addListSelectionListener { rebuild() }
    initial.addListSelectionListener { rebuild() }
    customPalette.addItemListener { rebuild() }
    clearAllVisible.addItemListener { rebuild() }
    rebuild()

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Orientation (Orientation)"))
    options.add(orientation)
    options.add(optionsLabel("Flyout placement (ButtonFlyoutPlacement)"))
    options.add(placement)
    options.add(optionsLabel("Built-in buttons (InitialControls)"))
    options.add(initial)
    options.add(optionsLabel("Change the ballpoint pen's color choices and size range (Palette / Min/MaxStrokeWidth)"))
    options.add(customPalette)
    options.add(optionsLabel("Show the eraser's \"Erase all ink\" (IsClearAllVisible)"))
    options.add(clearAllVisible)
    return buildExample("Appearance and built-in buttons", host, options)
}

/** A calligraphy-style pen: draws in the selected color with a long, thin rectangular pen tip tilted 45°. */
private class CalligraphyPen : WInkToolbarCustomPen() {
    override fun createInkDrawingAttributesCore(color: WColor?, strokeWidth: Double): InkDrawingAttributes =
        InkDrawingAttributes.builder()
            .color(color ?: WColor.BLACK)
            .penTip(PenTipShape.RECTANGLE)
            .size(strokeWidth, strokeWidth * 3)
            .penTipTransform(InkTransform.rotation(PI / 4))
            .ignorePressure(true)
            .build()
}

/** Custom buttons: a calligraphy pen, a lasso tool, and a toggle for drawing with touch. */
private fun buildInkToolbarCustomExample(): WComponent {
    val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(waveStroke(120.0, InkDrawingAttributes.builder().size(4.0).build()))))
    val toolbar = WInkToolbar()
    val info = WLabel("").also { it.textWrapping = TextWrapping.WRAP }

    val calligraphy = WInkToolbarCustomPenButton()
    calligraphy.customPen = CalligraphyPen()
    calligraphy.palette = INK_COLORS.map { it.second }
    calligraphy.configurationContent = WInkToolbarPenConfigurationControl()
    calligraphy.content = glyph(GLYPH_EDIT)
    calligraphy.toolTip = "Calligraphy pen"
    toolbar.add(calligraphy)

    // The lasso tool: selecting it sets the processing mode to NONE, and input arrives at UnprocessedInput
    val lassoTool = WInkToolbarCustomToolButton()
    lassoTool.content = glyph(GLYPH_SELECT_ALL)
    lassoTool.toolTip = "Lasso select"
    lassoTool.configurationContent = WLabel("Selects the strokes you enclose. Selected strokes can be moved with the \"Move\" button.")
    toolbar.add(lassoTool)
    installLasso(canvas, { info.text = "Selection: ${canvas.getSelectedStrokeIndices().size} strokes" }) {
        info.text = "Could not get the lasso's position (a limitation of WinUI 2.5 experimental)."
    }

    // A toggle that switches whether touch draws (used the same way as in the original UWP sample)
    val touchToggle = WInkToolbarCustomToggleButton()
    touchToggle.content = glyph(GLYPH_TOUCH_POINTER)
    touchToggle.toolTip = "Touch writing"
    touchToggle.isSelected = true
    touchToggle.addItemListener { checked ->
        canvas.inkPresenter.inputDeviceTypes = if (checked == true) ALL_DEVICES else ALL_DEVICES - CoreInputDeviceType.TOUCH
        info.text = "Touch writing: ${checked == true}"
    }
    toolbar.add(touchToggle)

    toolbar.addActiveToolChangedListener {
        info.text = when (toolbar.activeTool) {
            calligraphy -> "Calligraphy pen: draws with a ${toolbar.inkDrawingAttributes?.penTip} pen tip"
            lassoTool -> "Lasso: select strokes by enclosing them (processing mode ${canvas.inkPresenter.inputProcessingMode})"
            else -> "Tool: ${toolbar.activeTool?.toolKind}"
        }
    }
    val move = WButton("Move selected strokes right")
    move.addActionListener { canvas.moveSelected(30.0, 0.0) }

    val example = toolbarWithCanvas(toolbar, canvas)
    example.add(info)
    example.add(move)
    return buildExample("Custom pens, tools, and toggles (WInkToolbarCustomPen, etc.)", example)
}

/** Stencils: the ruler and the protractor. */
private fun buildInkToolbarStencilExample(): WComponent {
    val canvas = WInkCanvas()
    val toolbar = WInkToolbar()
    val log = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    toolbar.addStencilCheckedListener { e ->
        log.text = "Stencil: ${e.stencilKind} / on: ${toolbar.isStencilButtonChecked}"
    }
    val stencil = { toolbar.getMenuButton(InkToolbarMenuKind.STENCIL) as? WInkToolbarStencilButton }

    val show = WToggleSwitch().also { it.isOn = false }
    show.addItemListener { toolbar.isStencilButtonChecked = it }
    val kind = WComboBox(listOf("Ruler (RULER)", "Protractor (PROTRACTOR)")).also { it.selectedIndex = 0 }
    kind.addListSelectionListener { stencil()?.selectedStencil = InkToolbarStencilKind.entries[kind.selectedIndex.coerceAtLeast(0)] }
    val rulerItem = WToggleSwitch().also { it.isOn = true }
    rulerItem.addItemListener { stencil()?.isRulerItemVisible = it }
    val protractorItem = WToggleSwitch().also { it.isOn = true }
    protractorItem.addItemListener { stencil()?.isProtractorItemVisible = it }

    val example = toolbarWithCanvas(toolbar, canvas)
    example.add(log)
    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Show the stencil (IsStencilButtonChecked)"))
    options.add(show)
    options.add(optionsLabel("Stencil to show (SelectedStencil)"))
    options.add(kind)
    options.add(optionsLabel("Ruler in the menu (IsRulerItemVisible)"))
    options.add(rulerItem)
    options.add(optionsLabel("Protractor in the menu (IsProtractorItemVisible)"))
    options.add(protractorItem)
    return buildExample("Ruler and protractor (WInkToolbarStencilButton)", example, options)
}

// endregion
