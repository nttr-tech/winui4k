package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.DefaultInkStrokeModel
import com.appkitbox.winui4k.ink.InkPoint
import com.appkitbox.winui4k.ink.InkRect
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.ink.InkStrokeModel
import com.appkitbox.winui4k.ink.InkStrokeModelEvent
import com.appkitbox.winui4k.ink.InkStrokeModelListener
import com.appkitbox.winui4k.ink.InkStrokeRenderingSegment
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.InkInterop
import java.io.InputStream
import java.io.OutputStream

/**
 * Windows.UI.Input.Inking.InkPersistenceFormat (the format for saving ink).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd (GifWithEmbeddedIsf=0, Isf=1).
 */
enum class InkPersistenceFormat(internal val native: Int) {
    /**
     * A GIF image with embedded ISF (can be displayed as an image and turns back into strokes when loaded;
     * the WinUI default).
     */
    GIF_WITH_EMBEDDED_ISF(0),

    /** ISF (Ink Serialized Format) only. */
    ISF(1),
}

/**
 * The WinUI 3 InkCanvas (experimental in Windows App SDK 2.5; Swing has no equivalent): a canvas for drawing lines
 * with a pen, mouse, or touch.
 *
 * Like Swing's JTable and TableModel, the data of the drawn lines is held by an [InkStrokeModel] (the
 * com.appkitbox.winui4k.ink package), and this canvas is a view that draws the model's lines and writes the results of
 * input back to the model:
 * - Lines drawn with the pen (StrokesCollected) are appended to the model, and lines erased with the eraser
 *   (StrokesErased) are removed from it
 * - Lines added to, replaced in, or removed from the model are reflected on the canvas (one model can be shared by
 *   several canvases)
 * - The results of moving, deleting, pasting, and loading selected lines are also reflected in the model
 *
 * Input settings (input devices, drawing mode, processing mode) and events are in [inkPresenter]. By default,
 * InkCanvas accepts **pen input only**, so set [WInkPresenter.inputDeviceTypes] to draw with a mouse or touch.
 * The display state of strokes, such as selection, IDs, and bounding rectangles, is held per canvas and addressed by
 * index (the same position as in the model).
 *
 * ```kotlin
 * val canvas = WInkCanvas()
 * canvas.inkPresenter.inputDeviceTypes = setOf(CoreInputDeviceType.PEN, CoreInputDeviceType.MOUSE)
 * canvas.model.addStroke(InkStroke(listOf(InkPoint(10.0, 10.0), InkPoint(100.0, 50.0))))
 * ```
 */
class WInkCanvas @JvmOverloads constructor(model: InkStrokeModel = DefaultInkStrokeModel()) : WComponent(
    Activation.composeDefault(InkInterop.CLS_InkCanvas, InkInterop.IID_IInkCanvasFactory), // Default interface = IInkCanvas
) {
    /** The input and drawing settings and events of this canvas (InkCanvas.InkPresenter). */
    val inkPresenter: WInkPresenter = WInkPresenter(this, own(inspectable.getPtr(InkInterop.IInkCanvas_get_InkPresenter)))

    /** InkPresenter.StrokeContainer (IInkStrokeContainer). Holds the strokes in the same order as the model. */
    internal val container: ComPtr = own(inkPresenter.presenter.getPtr(InkInterop.IInkPresenter_get_StrokeContainer))

    /** The native strokes corresponding to each stroke in the model (in the same order as the model). */
    private val entries = ArrayList<NativeStroke>()

    /** Whether custom drying of ink is active (synchronization with the model is stopped). */
    private var customDrying = false

    /**
     * Strokes already added on the native side (drawn lines, pastes, loads). When the model notifies an addition
     * (INSERT), these are assigned in order instead of creating new ones.
     */
    private var pendingAdoption: ArrayDeque<ComPtr>? = null

    /**
     * The target of the model's removal notification (DELETE) is already gone on the native side
     * (eraser, deleting the selection, loading).
     */
    private var nativeAlreadyRemoved = false

    /**
     * The target of the model's replacement notification (UPDATE) has already changed on the native side
     * (moving the selection).
     */
    private var nativeAlreadyUpdated = false

    private val modelListener = InkStrokeModelListener { onModelChanged(it) }

    /** The model of strokes to display. Changing it rebuilds the display from the new model's content. */
    var model: InkStrokeModel = model
        set(value) {
            if (field === value) return
            field.removeInkStrokeModelListener(modelListener)
            field = value
            attachModel()
        }

    init {
        inkPresenter.presenter.addEventHandler(
            "WinUI4K.InkStrokesCollectedHandler",
            InkInterop.IID_InkPresenterStrokesCollectedHandler,
            InkInterop.IInkPresenter_add_StrokesCollected,
        ) { _, args -> onStrokesCollected(ComPtr(args)) }
        inkPresenter.presenter.addEventHandler(
            "WinUI4K.InkStrokesErasedHandler",
            InkInterop.IID_InkPresenterStrokesErasedHandler,
            InkInterop.IInkPresenter_add_StrokesErased,
        ) { _, args -> onStrokesErased(ComPtr(args)) }
        attachModel()
    }

    // ------------------------------------------------------------------
    // Selection (display state; InkStroke.Selected)
    // ------------------------------------------------------------------

    /** Whether the stroke at [index] is selected. */
    fun isStrokeSelected(index: Int): Boolean = entry(index).native.getBool(InkInterop.IInkStroke_get_Selected)

    /**
     * Changes the selection state of the stroke at [index]. Selected lines are displayed with an outline and become
     * the target of [moveSelected] and similar operations.
     */
    fun setStrokeSelected(index: Int, selected: Boolean) {
        entry(index).native.putBool(InkInterop.IInkStroke_put_Selected, selected)
    }

    /** The indices of the selected strokes (in ascending order). */
    fun getSelectedStrokeIndices(): IntArray {
        checkNotCustomDrying()
        return entries.indices.filter { entries[it].native.getBool(InkInterop.IInkStroke_get_Selected) }.toIntArray()
    }

    /** Selects all strokes. */
    fun selectAll() {
        checkNotCustomDrying()
        entries.forEach { it.native.putBool(InkInterop.IInkStroke_put_Selected, true) }
    }

    /** Clears the whole selection. */
    fun clearSelection() {
        checkNotCustomDrying()
        entries.forEach { it.native.putBool(InkInterop.IInkStroke_put_Selected, false) }
    }

    /**
     * Selects the strokes that intersect the line segment from ([fromX], [fromY]) to ([toX], [toY]) (SelectWithLine).
     * Returns the bounding rectangle of the selection.
     */
    fun selectWithLine(fromX: Double, fromY: Double, toX: Double, toY: Double): InkRect {
        checkNotCustomDrying()
        return Ffi.backend.withScope { scope ->
            InkNative.callForRect(
                container,
                InkInterop.IInkStrokeContainer_SelectWithLine,
                InkNative.pointValue(scope, fromX, fromY),
                InkNative.pointValue(scope, toX, toY),
            )
        }
    }

    /**
     * Selects the strokes enclosed by the polygon (lasso) connecting [points] (SelectWithPolyLine).
     * Only the positions of the points are used. Returns the bounding rectangle of the selection.
     */
    fun selectWithPolyLine(points: List<InkPoint>): InkRect {
        checkNotCustomDrying()
        val iterable = ComIterable(
            "WinUI4K.PointIterable",
            InkInterop.IID_IIterable_Point,
            InkInterop.IID_IIterator_Point,
            points.size,
            POINT_SIZE,
        ) { out, index ->
            Ffi.backend.memory.putFloat(out, 0, points[index].x.toFloat())
            Ffi.backend.memory.putFloat(out, 4, points[index].y.toFloat())
        }
        try {
            return InkNative.callForRect(container, InkInterop.IInkStrokeContainer_SelectWithPolyLine, iterable.comObject.primary)
        } finally {
            iterable.comObject.release()
        }
    }

    /**
     * Moves the selected strokes by ([dx], [dy]) (MoveSelected; a translation is added to the point transform
     * [InkStroke.pointTransform]). The moved strokes are also replaced in the model. Returns the area that needs
     * to be redrawn.
     */
    fun moveSelected(dx: Double, dy: Double): InkRect {
        val selected = getSelectedStrokeIndices()
        val rect = Ffi.backend.withScope { scope ->
            InkNative.callForRect(container, InkInterop.IInkStrokeContainer_MoveSelected, InkNative.pointValue(scope, dx, dy))
        }
        nativeAlreadyUpdated = true
        try {
            for (index in selected) model.setStroke(index, InkNative.readStroke(entries[index].native))
        } finally {
            nativeAlreadyUpdated = false
        }
        return rect
    }

    /**
     * Deletes the selected strokes (DeleteSelected). They are also removed from the model.
     * Returns the area that needs to be redrawn.
     */
    fun deleteSelected(): InkRect {
        val selected = getSelectedStrokeIndices()
        val rect = InkNative.callForRect(container, InkInterop.IInkStrokeContainer_DeleteSelected)
        nativeAlreadyRemoved = true
        try {
            for (index in selected.reversed()) model.removeStroke(index)
        } finally {
            nativeAlreadyRemoved = false
        }
        return rect
    }

    // ------------------------------------------------------------------
    // Clipboard
    // ------------------------------------------------------------------

    /** Copies the selected strokes to the clipboard (CopySelectedToClipboard). */
    fun copySelectedToClipboard() {
        checkNotCustomDrying()
        container.call(InkInterop.IInkStrokeContainer_CopySelectedToClipboard)
    }

    /** Whether the clipboard contains ink that can be pasted (CanPasteFromClipboard). */
    fun canPasteFromClipboard(): Boolean {
        checkNotCustomDrying()
        return container.getBool(InkInterop.IInkStrokeContainer_CanPasteFromClipboard)
    }

    /**
     * Pastes the ink on the clipboard at ([x], [y]) (PasteFromClipboard) and appends it to the model.
     * Returns the pasted area.
     */
    fun pasteFromClipboard(x: Double, y: Double): InkRect {
        checkNotCustomDrying()
        val rect = Ffi.backend.withScope { scope ->
            InkNative.callForRect(container, InkInterop.IInkStrokeContainer_PasteFromClipboard, InkNative.pointValue(scope, x, y))
        }
        adoptNewNativeStrokes()
        return rect
    }

    // ------------------------------------------------------------------
    // Display information of strokes
    // ------------------------------------------------------------------

    /**
     * The bounding rectangle of all strokes (InkStrokeContainer.BoundingRect; includes the line width).
     * (0, 0, 0, 0) if there are no lines.
     */
    val boundingRect: InkRect
        get() {
            checkNotCustomDrying()
            return InkNative.callForRect(container, InkInterop.IInkStrokeContainer_get_BoundingRect)
        }

    /**
     * The bounding rectangle of the stroke at [index] (InkStroke.BoundingRect; includes the line width and curve
     * fitting).
     */
    fun getStrokeBounds(index: Int): InkRect = InkNative.callForRect(entry(index).native, InkInterop.IInkStroke_get_BoundingRect)

    /** The segments of the stroke at [index], curve-fitted for rendering (GetRenderingSegments). */
    fun getRenderingSegments(index: Int): List<InkStrokeRenderingSegment> = InkNative.readRenderingSegments(entry(index).native)

    /** The ID of the stroke at [index] (InkStroke.Id; unique within this canvas). */
    fun getStrokeId(index: Int): Int =
        InkNative.withView(entry(index).native, InkInterop.IID_IInkStroke3) { it.getInt(InkInterop.IInkStroke3_get_Id) }

    /** The ID of the pointer that drew the stroke at [index] (InkStroke.PointerId; 0 for lines added in code). */
    fun getStrokePointerId(index: Int): Int =
        InkNative.withView(entry(index).native, InkInterop.IID_IInkStroke4) { it.getInt(InkInterop.IInkStroke4_get_PointerId) }

    /** Whether the stroke at [index] has been handwriting-recognized (InkStroke.Recognized). */
    fun isStrokeRecognized(index: Int): Boolean = entry(index).native.getBool(InkInterop.IInkStroke_get_Recognized)

    /** The index of the stroke whose ID is [id] (InkStrokeContainer.GetStrokeById), or -1 if there is none. */
    fun indexOfStrokeId(id: Int): Int {
        checkNotCustomDrying()
        val native = container.getPtrOrNull(InkInterop.IInkStrokeContainer_GetStrokeById, id) ?: return -1
        val identity = try {
            InkNative.identityOf(native)
        } finally {
            native.release()
        }
        return entries.indexOfFirst { it.identity == identity }
    }

    // ------------------------------------------------------------------
    // Saving and loading (like JTextComponent.write / read)
    // ------------------------------------------------------------------

    /** Writes all strokes to [output] as a GIF image with embedded ISF (InkStrokeContainer.SaveAsync). */
    fun write(output: OutputStream) {
        checkNotCustomDrying()
        output.write(InkStreams.captureOutput { stream -> awaitAction(container.getPtr(InkInterop.IInkStrokeContainer_SaveAsync, stream)) })
    }

    /** Writes all strokes to [output] in [format] (InkStrokeContainer.SaveAsync(stream, format)). */
    fun write(output: OutputStream, format: InkPersistenceFormat) {
        checkNotCustomDrying()
        val bytes = InkStreams.captureOutput { stream ->
            awaitAction(container.getPtr(InkInterop.IInkStrokeContainer_SaveWithFormatAsync, stream, format.native))
        }
        output.write(bytes)
    }

    /**
     * Reads ISF (or a GIF with embedded ISF) from [input] and replaces all strokes on the canvas
     * (InkStrokeContainer.LoadAsync). The model's content is also replaced with the loaded strokes.
     */
    fun read(input: InputStream) {
        checkNotCustomDrying()
        InkStreams.withInput(input.readBytes()) { stream ->
            awaitAction(container.getPtr(InkInterop.IInkStrokeContainer_LoadAsync, stream))
        }
        // LoadAsync replaces all native strokes, so empty the model before assigning the loaded lines
        nativeAlreadyRemoved = true
        try {
            model.clear()
        } finally {
            nativeAlreadyRemoved = false
        }
        adoptNewNativeStrokes()
    }

    private fun awaitAction(action: ComPtr) {
        try {
            Async.await(action, "saving or loading InkStrokeContainer")
        } finally {
            action.release()
        }
    }

    // ------------------------------------------------------------------
    // Model → native
    // ------------------------------------------------------------------

    private fun attachModel() {
        model.addInkStrokeModelListener(modelListener)
        rebuildNative()
    }

    private fun onModelChanged(event: InkStrokeModelEvent) {
        if (customDrying) return
        when {
            event.isDataChanged -> rebuildNative()
            event.type == InkStrokeModelEvent.INSERT && event.firstIndex in 0..entries.size ->
                onStrokesInserted(event.firstIndex, event.lastIndex)
            event.type == InkStrokeModelEvent.DELETE && event.lastIndex < entries.size ->
                onStrokesDeleted(event.firstIndex, event.lastIndex)
            event.type == InkStrokeModelEvent.UPDATE && event.lastIndex < entries.size ->
                onStrokesUpdated(event.firstIndex, event.lastIndex)
            else -> rebuildNative() // Rebuild if the notified range does not match the display (inconsistent model implementation)
        }
    }

    private fun onStrokesInserted(firstIndex: Int, lastIndex: Int) {
        val appended = firstIndex == entries.size
        val created = ArrayList<NativeStroke>()
        for (index in firstIndex..lastIndex) {
            val stroke = model.getStroke(index)
            val adopted = pendingAdoption?.removeFirstOrNull()
            val entry = if (adopted != null) {
                NativeStroke(adopted, stroke)
            } else {
                NativeStroke(InkNative.createStroke(stroke), stroke).also { created += it }
            }
            entries.add(index, entry)
        }
        when {
            // Native strokes can only be appended at the end, so an insertion in the middle reorders them
            !appended -> reorderNative()
            created.isNotEmpty() -> addNative(created)
        }
    }

    private fun onStrokesDeleted(firstIndex: Int, lastIndex: Int) {
        val range = entries.subList(firstIndex, lastIndex + 1)
        val removed = range.toList()
        range.clear()
        if (!nativeAlreadyRemoved) {
            if (entries.isEmpty()) {
                container.call(InkInterop.IInkStrokeContainer_Clear)
            } else {
                deleteNative(removed.map { it.native })
            }
        }
        removed.forEach { it.dispose() }
    }

    private fun onStrokesUpdated(firstIndex: Int, lastIndex: Int) {
        var reorder = false
        for (index in firstIndex..lastIndex) {
            val entry = entries[index]
            val stroke = model.getStroke(index)
            if (!nativeAlreadyUpdated) {
                if (entry.stroke.points == stroke.points) {
                    updateInPlace(entry.native, entry.stroke, stroke)
                } else {
                    // The point sequence cannot be rewritten natively, so recreate the stroke and reorder everything
                    // to keep the order
                    entry.replace(InkNative.createStroke(stroke))
                    reorder = true
                }
            }
            entry.stroke = stroke
        }
        if (reorder) reorderNative()
    }

    /**
     * For a stroke with the same point sequence, rewrites only the drawing attributes, point transform, and time
     * natively.
     */
    private fun updateInPlace(native: ComPtr, old: InkStroke, new: InkStroke) {
        if (old.drawingAttributes != new.drawingAttributes) {
            val attributes = InkNative.createDrawingAttributes(new.drawingAttributes)
            try {
                native.call(InkInterop.IInkStroke_put_DrawingAttributes, attributes)
            } finally {
                attributes.release()
            }
        }
        if (old.pointTransform != new.pointTransform) {
            InkNative.withView(native, InkInterop.IID_IInkStroke2) { view ->
                Ffi.backend.withScope { scope ->
                    view.call(InkInterop.IInkStroke2_put_PointTransform, InkNative.matrixValue(scope, new.pointTransform))
                }
            }
        }
        if (old.strokeStartedTime != new.strokeStartedTime || old.strokeDuration != new.strokeDuration) {
            InkNative.withView(native, InkInterop.IID_IInkStroke3) { view ->
                putReference(view, InkInterop.IInkStroke3_put_StrokeStartedTime, FoundationInterop.IID_IReference_DateTime) {
                    new.strokeStartedTime?.let { PropertyValues.boxDateTime(InkNative.instantToTicks(it)) }
                }
                putReference(view, InkInterop.IInkStroke3_put_StrokeDuration, FoundationInterop.IID_IReference_TimeSpan) {
                    new.strokeDuration?.let { PropertyValues.boxTimeSpan(InkNative.durationToTicks(it)) }
                }
            }
        }
    }

    /** Puts the value created by [box] (or null if it returns null) to a property of type IReference<T>. */
    private fun putReference(target: ComPtr, slot: Int, referenceIid: String, box: () -> ComPtr?) {
        val boxed = box()
        if (boxed == null) {
            target.call(slot, null)
            return
        }
        val reference = try {
            boxed.queryInterface(referenceIid)
        } finally {
            boxed.release()
        }
        try {
            target.call(slot, reference)
        } finally {
            reference.release()
        }
    }

    /**
     * Empties the native side and recreates all strokes of the model. Does not touch the container if no lines are
     * displayed (once the OS container has been operated on, activateCustomDrying can no longer be called later).
     */
    private fun rebuildNative() {
        if (customDrying) return
        if (entries.isNotEmpty()) container.call(InkInterop.IInkStrokeContainer_Clear)
        entries.forEach { it.dispose() }
        entries.clear()
        val strokes = model.getStrokes()
        strokes.mapTo(entries) { NativeStroke(InkNative.createStroke(it), it) }
        if (entries.isNotEmpty()) addNative(entries)
    }

    /**
     * Realigns the native order to the order of [entries]. A stroke once removed from a container cannot be put back
     * into the same container (AddStroke fails), so it is replaced with a copy (InkStroke.Clone) and added again.
     */
    private fun reorderNative() {
        container.call(InkInterop.IInkStrokeContainer_Clear)
        for (entry in entries) entry.replace(entry.native.getPtr(InkInterop.IInkStroke_Clone))
        if (entries.isNotEmpty()) addNative(entries)
    }

    private fun addNative(strokes: List<NativeStroke>) {
        if (strokes.size == 1) {
            container.call(InkInterop.IInkStrokeContainer_AddStroke, strokes[0].native)
            return
        }
        val iterable = InkNative.iterableOf(strokes.map { it.native }, InkInterop.IID_IIterable_InkStroke, InkInterop.IID_IIterator_InkStroke)
        try {
            container.call(InkInterop.IInkStrokeContainer_AddStrokes, iterable.comObject.primary)
        } finally {
            iterable.comObject.release()
        }
    }

    /**
     * Deletes only the native strokes [natives]. The container has no way to delete individual strokes, so this
     * selects only the targets, calls DeleteSelected, and restores the selection state of the other strokes.
     */
    private fun deleteNative(natives: List<ComPtr>) {
        val selectedOthers = entries.filter { it.native.getBool(InkInterop.IInkStroke_get_Selected) }
        selectedOthers.forEach { it.native.putBool(InkInterop.IInkStroke_put_Selected, false) }
        natives.forEach { it.putBool(InkInterop.IInkStroke_put_Selected, true) }
        InkNative.callForRect(container, InkInterop.IInkStrokeContainer_DeleteSelected)
        selectedOthers.forEach { it.native.putBool(InkInterop.IInkStroke_put_Selected, true) }
    }

    // ------------------------------------------------------------------
    // Native → model
    // ------------------------------------------------------------------

    private fun onStrokesCollected(args: ComPtr) {
        collectStrokes(readStrokes(args, InkInterop.IInkStrokesCollectedEventArgs_get_Strokes))
    }

    private fun onStrokesErased(args: ComPtr) {
        eraseStrokes(readStrokes(args, InkInterop.IInkStrokesErasedEventArgs_get_Strokes))
    }

    /**
     * The body of StrokesCollected handling: appends [natives] drawn natively (owned references, already added to
     * the container) to the model and notifies listeners.
     */
    internal fun collectStrokes(natives: List<ComPtr>) {
        val strokes = natives.map { InkNative.readStroke(it) }
        if (customDrying) {
            natives.forEach { it.release() }
        } else {
            // Notifications reach the UI thread late, so lines erased in the meantime (by Clear and so on) are not
            // added to the model
            val alive = natives.filter { isInContainer(it) }
            natives.filterNot { it in alive }.forEach { it.release() }
            adopt(alive) {
                for (native in alive) model.addStroke(strokes[natives.indexOf(native)])
            }
        }
        inkPresenter.fireStrokesCollected(strokes)
    }

    /**
     * The body of StrokesErased handling: removes [natives] erased natively (owned references, already removed from
     * the container) from the model and notifies listeners.
     */
    internal fun eraseStrokes(natives: List<ComPtr>) {
        if (customDrying) {
            val strokes = natives.map { InkNative.readStroke(it) }
            natives.forEach { it.release() }
            inkPresenter.fireStrokesErased(strokes)
            return
        }
        val identities = natives.map { native ->
            try {
                InkNative.identityOf(native)
            } finally {
                native.release()
            }
        }
        val indices = identities.mapNotNull { identity -> entries.indexOfFirst { it.identity == identity }.takeIf { it >= 0 } }.sorted()
        val strokes = indices.map { entries[it].stroke }
        nativeAlreadyRemoved = true
        try {
            for (index in indices.asReversed()) model.removeStroke(index)
        } finally {
            nativeAlreadyRemoved = false
        }
        inkPresenter.fireStrokesErased(strokes)
    }

    private fun readStrokes(args: ComPtr, slot: Int): List<ComPtr> {
        val view = args.getPtr(slot)
        return try {
            InkNative.readVectorView(view)
        } finally {
            view.release()
        }
    }

    /** Whether [native] is still in the container (looked up by ID and compared by identity). */
    private fun isInContainer(native: ComPtr): Boolean {
        val id = InkNative.withView(native, InkInterop.IID_IInkStroke3) { it.getInt(InkInterop.IInkStroke3_get_Id) }
        val found = container.getPtrOrNull(InkInterop.IInkStrokeContainer_GetStrokeById, id) ?: return false
        return try {
            InkNative.identityOf(found) == InkNative.identityOf(native)
        } finally {
            found.release()
        }
    }

    /** Appends strokes that were added on the native side (with no counterpart in the display) to the model. */
    private fun adoptNewNativeStrokes() {
        val known = entries.mapTo(HashSet()) { it.identity }
        val view = container.getPtr(InkInterop.IInkStrokeContainer_GetStrokes)
        val natives = try {
            InkNative.readVectorView(view)
        } finally {
            view.release()
        }
        val fresh = natives.filter { InkNative.identityOf(it) !in known }
        natives.filterNot { it in fresh }.forEach { it.release() }
        adopt(fresh) {
            for (native in fresh) model.addStroke(InkNative.readStroke(native))
        }
    }

    /**
     * Assigns [natives] already added on the native side, in order, to the additions (INSERT) to the model made
     * inside [block]. Any that the model did not add are also deleted natively, keeping the display and the model in
     * sync.
     */
    private fun adopt(natives: List<ComPtr>, block: () -> Unit) {
        if (natives.isEmpty()) return
        val queue = ArrayDeque(natives)
        pendingAdoption = queue
        try {
            block()
        } finally {
            pendingAdoption = null
        }
        if (queue.isNotEmpty()) {
            deleteNative(queue)
            queue.forEach { it.release() }
        }
    }

    // ------------------------------------------------------------------
    // Custom drying of ink
    // ------------------------------------------------------------------

    /** Called from [WInkPresenter.activateCustomDrying]. Stops synchronization with the model from then on. */
    internal fun onCustomDryingActivated() {
        customDrying = true
        entries.forEach { it.dispose() }
        entries.clear()
    }

    private fun checkNotCustomDrying() {
        check(!customDrying) { "strokes cannot be manipulated during custom drying of ink (activateCustomDrying)" }
    }

    private fun entry(index: Int): NativeStroke {
        checkNotCustomDrying()
        if (index !in entries.indices) throw IndexOutOfBoundsException("index=$index, size=${entries.size}")
        return entries[index]
    }

    /**
     * The native stroke (IInkStroke) corresponding to one stroke of the model, and its ownership.
     * It is released with [dispose] when it leaves the display, or through the GC path when the whole canvas is
     * reclaimed.
     */
    private class NativeStroke(native: ComPtr, var stroke: InkStroke) {
        var native: ComPtr = native
            private set

        /** The COM identity (used to match strokes in event arguments). */
        var identity: Long = InkNative.identityOf(native)
            private set

        private var lifetime = ComLifetime.adopt(this, native)

        /** Replaces the native stroke with [replacement] (an owned reference) and releases the original reference. */
        fun replace(replacement: ComPtr) {
            lifetime.close()
            native = replacement
            identity = InkNative.identityOf(replacement)
            lifetime = ComLifetime.adopt(this, replacement)
        }

        fun dispose() {
            lifetime.close()
        }
    }

    private companion object {
        /** The size of Windows.Foundation.Point (float × 2). */
        const val POINT_SIZE = 8L
    }
}
