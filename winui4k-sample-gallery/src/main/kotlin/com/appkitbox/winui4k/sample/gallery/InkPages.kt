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
 * Ink category: demo pages for InkCanvas / InkToolbar (Windows App SDK 2.6 experimental).
 */

/** Allows drawing with mouse, pen, and touch alike (InkCanvas defaults to pen only). */
private val ALL_DEVICES = setOf(CoreInputDeviceType.PEN, CoreInputDeviceType.MOUSE, CoreInputDeviceType.TOUCH)

/** Color choices (name and color). */
private val INK_COLORS = listOf(
    Messages.get("inkcanvas.color.black") to WColor.BLACK,
    Messages.get("inkcanvas.color.red") to DEMO_RED,
    Messages.get("inkcanvas.color.green") to DEMO_GREEN,
    Messages.get("inkcanvas.color.blue") to DEMO_BLUE,
    Messages.get("inkcanvas.color.orange") to DEMO_ORANGE,
    Messages.get("inkcanvas.color.purple") to WColor.PURPLE,
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
    val point = event.pointerPoint ?: return Messages.get("inkcanvas.event.nopoint")
    return Messages.get(
        "inkcanvas.event.point",
        point.deviceType.toString(),
        "%.0f".format(point.x),
        "%.0f".format(point.y),
        "%.2f".format(point.pressure),
        event.modifiers.toString(),
    )
}

private fun describe(rect: InkRect): String = if (rect.isEmpty) {
    Messages.get("inkcanvas.rect.none")
} else {
    Messages.get(
        "inkcanvas.rect.bounds",
        "%.0f".format(rect.x),
        "%.0f".format(rect.y),
        "%.0f".format(rect.width),
        "%.0f".format(rect.height),
    )
}

// region InkCanvas page

/** The InkCanvas page: a guide plus demos of input, drawing attributes, MVC, selection, saving, events, and custom drying. */
internal fun buildInkCanvasPage(): WComponent {
    val page = buildPage(
        "InkCanvas",
        Messages.get("inkcanvas.description"),
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
    body.add(WLabel(Messages.get("inkcanvas.guide.canvas")))
    body.add(WLabel(Messages.get("inkcanvas.guide.presenter")))
    body.add(WLabel(Messages.get("inkcanvas.guide.model")))
    body.add(WLabel(Messages.get("inkcanvas.guide.values")))
    body.add(
        WLabel(
            Messages.get("inkcanvas.guide.sync"),
        ).also { it.textWrapping = TextWrapping.WRAP },
    )
    val note = WInfoBar()
    note.isOpen = true
    note.isClosable = false
    note.message = Messages.get("inkcanvas.guide.note")
    body.add(note)
    return buildExample(Messages.get("inkcanvas.guide.title"), body)
}

/** Basics: input devices, enabling input, processing mode, and high contrast. */
private fun buildInkCanvasBasicExample(): WComponent {
    val canvas = WInkCanvas()
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val count = WLabel(Messages.get("inkcanvas.basic.count", "0"))
    canvas.model.addInkStrokeModelListener {
        count.text = Messages.get("inkcanvas.basic.count", canvas.model.getStrokeCount().toString())
    }

    val pen = WCheckBox(Messages.get("inkcanvas.basic.pen")).also { it.isSelected = true }
    val mouse = WCheckBox(Messages.get("inkcanvas.basic.mouse")).also { it.isSelected = true }
    val touch = WCheckBox(Messages.get("inkcanvas.basic.touch")).also { it.isSelected = true }
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

    // The display order differs from the enum's declaration order (NONE, INKING, ERASING), so keep display names paired with values
    val modes = listOf(
        Messages.get("inkcanvas.basic.mode.inking") to InkInputProcessingMode.INKING,
        Messages.get("inkcanvas.basic.mode.erasing") to InkInputProcessingMode.ERASING,
        Messages.get("inkcanvas.basic.mode.none") to InkInputProcessingMode.NONE,
    )
    val mode = WComboBox(modes.map { it.first })
    mode.selectedIndex = 0
    mode.addListSelectionListener {
        canvas.inkPresenter.inputProcessingMode = modes[mode.selectedIndex.coerceAtLeast(0)].second
    }

    val highContrast = WComboBox(InkHighContrastAdjustment.entries.map { it.name })
    highContrast.selectedIndex = 0
    highContrast.addListSelectionListener {
        canvas.inkPresenter.highContrastAdjustment = InkHighContrastAdjustment.entries[highContrast.selectedIndex]
    }

    val clear = WButton(Messages.get("inkcanvas.basic.clear"))
    clear.addActionListener { canvas.model.clear() }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(count)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("inkcanvas.basic.devices")))
    options.add(pen)
    options.add(mouse)
    options.add(touch)
    options.add(optionsLabel(Messages.get("inkcanvas.basic.inputenabled")))
    options.add(enabled)
    options.add(optionsLabel(Messages.get("inkcanvas.basic.mode")))
    options.add(mode)
    options.add(optionsLabel(Messages.get("inkcanvas.basic.highcontrast")))
    options.add(highContrast)
    options.add(clear)
    return buildExample(Messages.get("inkcanvas.basic.title"), example, options)
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
            Messages.get("inkcanvas.attributes.color") to color,
            Messages.get("inkcanvas.attributes.size") to size,
            Messages.get("inkcanvas.attributes.rectangletip") to rectangleTip,
            Messages.get("inkcanvas.attributes.rotate") to rotate,
            Messages.get("inkcanvas.attributes.highlighter") to highlighter,
            Messages.get("inkcanvas.attributes.pencil") to pencil,
            Messages.get("inkcanvas.attributes.pencilopacity") to pencilOpacity,
            Messages.get("inkcanvas.attributes.fittocurve") to fitToCurve,
            Messages.get("inkcanvas.attributes.ignorepressure") to ignorePressure,
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
        current.text = Messages.get(
            "inkcanvas.attributes.current",
            read.kind.toString(),
            read.width.toString(),
            read.penTip.toString(),
            read.drawAsHighlighter.toString(),
        )
    }
    controls.addChangeListener(apply)
    apply()

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(current)
    return buildExample(Messages.get("inkcanvas.attributes.title"), example, controls.options())
}

/** The kind and range of the model change [e], and the number of strokes after the change [strokeCount]. */
private fun describe(e: InkStrokeModelEvent, strokeCount: Int): String {
    val kind = when (e.type) {
        InkStrokeModelEvent.INSERT -> Messages.get("inkcanvas.model.kind.insert")
        InkStrokeModelEvent.DELETE -> Messages.get("inkcanvas.model.kind.delete")
        else -> if (e.isDataChanged) Messages.get("inkcanvas.model.kind.all") else Messages.get("inkcanvas.model.kind.replace")
    }
    val count = strokeCount.toString()
    return if (e.isDataChanged) {
        Messages.get("inkcanvas.model.log", kind, count)
    } else {
        Messages.get("inkcanvas.model.log.range", kind, e.firstIndex.toString(), e.lastIndex.toString(), count)
    }
}

/** MVC: two canvases share one model, and the model is manipulated from code. */
private fun buildInkModelExample(): WComponent {
    val model = DefaultInkStrokeModel()
    val left = WInkCanvas(model).also { it.inkPresenter.inputDeviceTypes = ALL_DEVICES }
    val right = WInkCanvas(model).also { it.inkPresenter.inputDeviceTypes = ALL_DEVICES }
    val log = WLabel(Messages.get("inkcanvas.model.log.none")).also { it.textWrapping = TextWrapping.WRAP }
    val undone = ArrayDeque<InkStroke>()
    model.addInkStrokeModelListener { e -> log.text = describe(e, model.getStrokeCount()) }
    // When a new stroke is drawn, discard the redo history
    left.inkPresenter.addStrokesCollectedListener { undone.clear() }
    right.inkPresenter.addStrokesCollectedListener { undone.clear() }

    val addWave = WButton(Messages.get("inkcanvas.model.addwave"))
    addWave.addActionListener {
        val top = 40.0 + (model.getStrokeCount() % 5) * 40.0
        model.addStroke(waveStroke(top, InkDrawingAttributes.builder().color(DEMO_BLUE).size(4.0).build()))
    }
    val undo = WButton(Messages.get("inkcanvas.model.undo"))
    undo.addActionListener {
        val last = model.getStrokeCount() - 1
        if (last >= 0) {
            undone.addLast(model.getStroke(last))
            model.removeStroke(last)
        }
    }
    val redo = WButton(Messages.get("inkcanvas.model.redo"))
    redo.addActionListener { undone.removeLastOrNull()?.let { model.addStroke(it) } }
    val recolor = WButton(Messages.get("inkcanvas.model.recolor"))
    recolor.addActionListener {
        for (i in 0 until model.getStrokeCount()) {
            val stroke = model.getStroke(i)
            model.setStroke(i, stroke.withDrawingAttributes(stroke.drawingAttributes.toBuilder().color(DEMO_RED).build()))
        }
    }
    val shift = WButton(Messages.get("inkcanvas.model.shift"))
    shift.addActionListener {
        for (i in 0 until model.getStrokeCount()) {
            val stroke = model.getStroke(i)
            model.setStroke(i, stroke.withPointTransform(stroke.pointTransform.then(InkTransform.translation(20.0, 0.0))))
        }
    }
    val clear = WButton(Messages.get("inkcanvas.model.clear"))
    clear.addActionListener { model.clear() }

    val canvases = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    canvases.add(inkSurface(left, width = 300.0))
    canvases.add(inkSurface(right, width = 300.0))
    val example = WPanel(spacing = 8.0)
    example.add(canvases)
    example.add(log)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("inkcanvas.model.hint")))
    listOf(addWave, undo, redo, recolor, shift, clear).forEach { options.add(it) }
    return buildExample(Messages.get("inkcanvas.model.title"), example, options)
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
    waveStroke(60.0, InkDrawingAttributes.builder().color(DEMO_BLUE).size(4.0).build()),
    waveStroke(140.0, InkDrawingAttributes.builder().color(DEMO_GREEN).size(6.0).build()),
)

/** Selection: select with a lasso, then move, delete, copy, and paste. */
private fun buildInkSelectionExample(): WComponent {
    val canvas = WInkCanvas(DefaultInkStrokeModel(twoWaves()))
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val info = WLabel(Messages.get("inkcanvas.selection.none")).also { it.textWrapping = TextWrapping.WRAP }
    fun showSelection(rect: InkRect? = null) {
        val selected = canvas.getSelectedStrokeIndices()
        val size = selected.size.toString()
        val indices = selected.toList().toString()
        info.text = if (rect == null) {
            Messages.get("inkcanvas.selection.info", size, indices)
        } else {
            Messages.get("inkcanvas.selection.info.range", size, indices, describe(rect))
        }
    }

    // Lasso: input in no-processing mode and right-drag in inking mode (the RightDragAction default) arrive at UnprocessedInput
    installLasso(canvas, { showSelection(it) }) {
        info.text = Messages.get("inkcanvas.selection.lassounavailable")
    }

    val selectMode = WToggleSwitch().also { it.isOn = false }
    selectMode.addItemListener { on ->
        canvas.inkPresenter.inputProcessingMode = if (on) InkInputProcessingMode.NONE else InkInputProcessingMode.INKING
    }
    val selectAll = WButton(Messages.get("inkcanvas.selection.selectall"))
    selectAll.addActionListener {
        canvas.selectAll()
        showSelection()
    }
    val clearSelection = WButton(Messages.get("inkcanvas.selection.clear"))
    clearSelection.addActionListener {
        canvas.clearSelection()
        showSelection()
    }
    val byLine = WButton(Messages.get("inkcanvas.selection.byline"))
    byLine.addActionListener { showSelection(canvas.selectWithLine(100.0, 0.0, 100.0, 260.0)) }
    val byPolyLine = WButton(Messages.get("inkcanvas.selection.bypolyline"))
    byPolyLine.addActionListener {
        val area = listOf(InkPoint(0.0, 0.0), InkPoint(560.0, 0.0), InkPoint(560.0, 110.0), InkPoint(0.0, 110.0))
        showSelection(canvas.selectWithPolyLine(area))
    }
    val move = WButton(Messages.get("inkcanvas.selection.move"))
    move.addActionListener { showSelection(canvas.moveSelected(30.0, 20.0)) }
    val delete = WButton(Messages.get("inkcanvas.selection.delete"))
    delete.addActionListener { showSelection(canvas.deleteSelected()) }
    val copy = WButton(Messages.get("inkcanvas.selection.copy"))
    copy.addActionListener {
        runCatching { canvas.copySelectedToClipboard() }.onFailure { info.text = Messages.get("inkcanvas.selection.copyfailed", it.message) }
    }
    val paste = WButton(Messages.get("inkcanvas.selection.paste"))
    paste.addActionListener {
        if (canvas.canPasteFromClipboard()) {
            showSelection(canvas.pasteFromClipboard(20.0, 20.0))
        } else {
            info.text = Messages.get("inkcanvas.selection.noink")
        }
    }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(info)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("inkcanvas.selection.lassohint")))
    options.add(selectMode)
    listOf(selectAll, clearSelection, byLine, byPolyLine, move, delete, copy, paste).forEach { options.add(it) }
    return buildExample(Messages.get("inkcanvas.selection.title"), example, options)
}

/** Saving and loading: ISF / GIF with embedded ISF. */
private fun buildInkSaveLoadExample(): WComponent {
    val canvas = WInkCanvas(
        DefaultInkStrokeModel(listOf(waveStroke(100.0, InkDrawingAttributes.builder().color(WColor.PURPLE).size(5.0).build()))),
    )
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }

    val save = WButton(Messages.get("inkcanvas.save.save"))
    save.addActionListener {
        runCatching {
            val picker = WFileSavePicker(galleryFrame)
            picker.suggestedFileName = "ink"
            picker.addFileTypeChoice(Messages.get("inkcanvas.save.giftype"), ".gif")
            picker.addFileTypeChoice("Ink Serialized Format", ".isf")
            picker.pickSaveFile { path ->
                if (path == null) {
                    result.text = Messages.get("inkcanvas.save.cancelled")
                    return@pickSaveFile
                }
                runCatching {
                    val format = if (path.endsWith(".isf", ignoreCase = true)) InkPersistenceFormat.ISF else InkPersistenceFormat.GIF_WITH_EMBEDDED_ISF
                    File(path).outputStream().use { canvas.write(it, format) }
                    result.text = Messages.get(
                        "inkcanvas.save.saved",
                        canvas.model.getStrokeCount().toString(),
                        path,
                        File(path).length().toString(),
                    )
                }.onFailure { result.text = Messages.get("inkcanvas.save.savefailed", it.message) }
            }
        }.onFailure { result.text = Messages.get("inkcanvas.save.savefailed", it.message) }
    }
    val load = WButton(Messages.get("inkcanvas.save.load"))
    load.addActionListener {
        runCatching {
            val picker = WFileOpenPicker(galleryFrame)
            picker.addFileTypeFilter(".gif", ".isf")
            picker.pickSingleFile { path ->
                if (path == null) {
                    result.text = Messages.get("inkcanvas.save.cancelled")
                    return@pickSingleFile
                }
                runCatching {
                    File(path).inputStream().use { canvas.read(it) }
                    result.text = Messages.get("inkcanvas.save.loaded", path, canvas.model.getStrokeCount().toString())
                }.onFailure { result.text = Messages.get("inkcanvas.save.loadfailed", it.message) }
            }
        }.onFailure { result.text = Messages.get("inkcanvas.save.loadfailed", it.message) }
    }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(result)
    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("inkcanvas.save.hint")))
    options.add(save)
    options.add(load)
    return buildExample(Messages.get("inkcanvas.save.title"), example, options)
}

/** Input events: StrokeInput and information about drawn strokes (StrokesCollected). */
private fun buildInkInputEventsExample(): WComponent {
    val canvas = WInkCanvas()
    canvas.inkPresenter.inputDeviceTypes = ALL_DEVICES
    val log = WLabel(Messages.get("inkcanvas.events.initial")).also { it.textWrapping = TextWrapping.WRAP }
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
                append(Messages.get("inkcanvas.events.started", describe(event)))
            }

            override fun strokeContinued(event: InkPointerEvent) {
                moves++
            }

            override fun strokeEnded(event: InkPointerEvent) {
                append(Messages.get("inkcanvas.events.ended", moves.toString(), describe(event)))
            }

            override fun strokeCanceled(event: InkPointerEvent) {
                append(Messages.get("inkcanvas.events.canceled"))
            }
        },
    )
    canvas.inkPresenter.addStrokesCollectedListener { event ->
        val index = canvas.model.getStrokeCount() - 1
        val stroke = event.strokes.last()
        append(
            Messages.get(
                "inkcanvas.events.collected",
                canvas.getStrokeId(index).toString(),
                stroke.points.size.toString(),
                canvas.getRenderingSegments(index).size.toString(),
                describe(canvas.getStrokeBounds(index)),
            ),
        )
    }
    canvas.inkPresenter.addStrokesErasedListener { append(Messages.get("inkcanvas.events.erased", it.strokes.size.toString())) }

    val example = WPanel(spacing = 8.0)
    example.add(inkSurface(canvas))
    example.add(log)
    return buildExample(Messages.get("inkcanvas.events.title"), example)
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
        dried.addStrokes(strokes.map { it.withDrawingAttributes(it.drawingAttributes.toBuilder().color(DEMO_BLUE).build()) })
        synchronizer.endDry()
    }
    val clear = WButton(Messages.get("inkcanvas.drying.clear"))
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
            Messages.get("inkcanvas.drying.explanation"),
        ).also { it.textWrapping = TextWrapping.WRAP },
    )
    example.add(clear)
    return buildExample(Messages.get("inkcanvas.drying.title"), example)
}

// endregion

// region InkToolbar page

/** The InkToolbar page: lines up demos of the built-in tools, appearance, custom buttons, and stencils. */
internal fun buildInkToolbarPage(): WComponent {
    val page = buildPage(
        "InkToolbar",
        Messages.get("inktoolbar.description"),
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
        val color = attributes?.color?.let { "(${it.red}, ${it.green}, ${it.blue})" }
        val summary = Messages.get(
            "inktoolbar.basic.status",
            tool.toString(),
            color.toString(),
            attributes?.width.toString(),
            attributes?.kind.toString(),
        )
        status.text = summary + (if (message.isEmpty()) "" else "\n$message")
    }
    toolbar.addActiveToolChangedListener { showStatus() }
    toolbar.addInkDrawingAttributesChangedListener { showStatus() }
    toolbar.addEraseAllClickedListener {
        showStatus(Messages.get("inktoolbar.basic.eraseall", canvas.model.getStrokeCount().toString()))
    }
    toolbar.addLoadedListener { showStatus() }

    val example = toolbarWithCanvas(toolbar, canvas)
    example.add(status)
    return buildExample(Messages.get("inktoolbar.basic.title"), example)
}

/** Appearance: vertical placement, flyout placement, built-in buttons, and pen color choices. */
private fun buildInkToolbarAppearanceExample(): WComponent {
    val host = WPanel()
    val orientation = WComboBox(
        listOf(Messages.get("inktoolbar.appearance.horizontal"), Messages.get("inktoolbar.appearance.vertical")),
    ).also { it.selectedIndex = 1 }
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
                pen.palette = listOf(WColor.BLACK, DEMO_RED, DEMO_BLUE, WColor(0, 160, 120))
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
    options.add(optionsLabel(Messages.get("inktoolbar.appearance.orientation")))
    options.add(orientation)
    options.add(optionsLabel(Messages.get("inktoolbar.appearance.placement")))
    options.add(placement)
    options.add(optionsLabel(Messages.get("inktoolbar.appearance.initialcontrols")))
    options.add(initial)
    options.add(optionsLabel(Messages.get("inktoolbar.appearance.palette")))
    options.add(customPalette)
    options.add(optionsLabel(Messages.get("inktoolbar.appearance.clearall")))
    options.add(clearAllVisible)
    return buildExample(Messages.get("inktoolbar.appearance.title"), host, options)
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
    calligraphy.toolTipText = Messages.get("inktoolbar.custom.calligraphy")
    toolbar.add(calligraphy)

    // The lasso tool: selecting it sets the processing mode to NONE, and input arrives at UnprocessedInput
    val lassoTool = WInkToolbarCustomToolButton()
    lassoTool.content = glyph(GLYPH_SELECT_ALL)
    lassoTool.toolTipText = Messages.get("inktoolbar.custom.lasso")
    lassoTool.configurationContent = WLabel(Messages.get("inktoolbar.custom.lassoconfig"))
    toolbar.add(lassoTool)
    installLasso(canvas, { info.text = Messages.get("inktoolbar.custom.selected", canvas.getSelectedStrokeIndices().size.toString()) }) {
        info.text = Messages.get("inktoolbar.custom.lassounavailable")
    }

    // A toggle that switches whether touch draws (used the same way as in the original UWP sample)
    val touchToggle = WInkToolbarCustomToggleButton()
    touchToggle.content = glyph(GLYPH_TOUCH_POINTER)
    touchToggle.toolTipText = Messages.get("inktoolbar.custom.touch")
    touchToggle.isSelected = true
    touchToggle.addItemListener { checked ->
        canvas.inkPresenter.inputDeviceTypes = if (checked == true) ALL_DEVICES else ALL_DEVICES - CoreInputDeviceType.TOUCH
        info.text = Messages.get("inktoolbar.custom.touchstate", (checked == true).toString())
    }
    toolbar.add(touchToggle)

    toolbar.addActiveToolChangedListener {
        info.text = when (toolbar.activeTool) {
            calligraphy -> Messages.get("inktoolbar.custom.calligraphyactive", toolbar.inkDrawingAttributes?.penTip.toString())
            lassoTool -> Messages.get("inktoolbar.custom.lassoactive", canvas.inkPresenter.inputProcessingMode.toString())
            else -> Messages.get("inktoolbar.custom.tool", toolbar.activeTool?.toolKind.toString())
        }
    }
    val move = WButton(Messages.get("inktoolbar.custom.move"))
    move.addActionListener { canvas.moveSelected(30.0, 0.0) }

    val example = toolbarWithCanvas(toolbar, canvas)
    example.add(info)
    example.add(move)
    return buildExample(Messages.get("inktoolbar.custom.title"), example)
}

/** Stencils: the ruler and the protractor. */
private fun buildInkToolbarStencilExample(): WComponent {
    val canvas = WInkCanvas()
    val toolbar = WInkToolbar()
    val log = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    toolbar.addStencilCheckedListener { e ->
        log.text = Messages.get("inktoolbar.stencil.log", e.stencilKind.toString(), toolbar.isStencilButtonChecked.toString())
    }
    val stencil = { toolbar.getMenuButton(InkToolbarMenuKind.STENCIL) as? WInkToolbarStencilButton }

    val show = WToggleSwitch().also { it.isOn = false }
    show.addItemListener { toolbar.isStencilButtonChecked = it }
    val kind = WComboBox(
        listOf(Messages.get("inktoolbar.stencil.ruler"), Messages.get("inktoolbar.stencil.protractor")),
    ).also { it.selectedIndex = 0 }
    kind.addListSelectionListener { stencil()?.selectedStencil = InkToolbarStencilKind.entries[kind.selectedIndex.coerceAtLeast(0)] }
    val rulerItem = WToggleSwitch().also { it.isOn = true }
    rulerItem.addItemListener { stencil()?.isRulerItemVisible = it }
    val protractorItem = WToggleSwitch().also { it.isOn = true }
    protractorItem.addItemListener { stencil()?.isProtractorItemVisible = it }

    val example = toolbarWithCanvas(toolbar, canvas)
    example.add(log)
    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("inktoolbar.stencil.show")))
    options.add(show)
    options.add(optionsLabel(Messages.get("inktoolbar.stencil.selected")))
    options.add(kind)
    options.add(optionsLabel(Messages.get("inktoolbar.stencil.ruleritem")))
    options.add(rulerItem)
    options.add(optionsLabel(Messages.get("inktoolbar.stencil.protractoritem")))
    options.add(protractorItem)
    return buildExample(Messages.get("inktoolbar.stencil.title"), example, options)
}

// endregion
