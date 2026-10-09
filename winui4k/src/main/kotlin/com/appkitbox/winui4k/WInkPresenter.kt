package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.InkInterop

/**
 * Microsoft.UI.Xaml.Controls.InkInputProcessingMode (the processing mode for ink input).
 * Values extracted from Microsoft.UI.Xaml.winmd (None=0, Inking=1, Erasing=2).
 */
enum class InkInputProcessingMode(internal val native: Int) {
    /** Not processed as ink (the input goes to [InkUnprocessedInputListener]). */
    NONE(0),

    /** Draws lines (the default). */
    INKING(1),

    /** Erases the strokes that are touched. */
    ERASING(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkInputProcessingMode = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkInputRightDragAction (how drags with the pen's barrel button or the right mouse button
 * are handled).
 * Values extracted from Microsoft.UI.Xaml.winmd (LeaveUnprocessed=0, AllowProcessing=1).
 */
enum class InkInputRightDragAction(internal val native: Int) {
    /**
     * Passed to [InkUnprocessedInputListener] without being processed as ink (the default; used for lasso selection and
     * the like).
     */
    LEAVE_UNPROCESSED(0),

    /** Processed according to the processing mode, like normal input. */
    ALLOW_PROCESSING(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkInputRightDragAction = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkHighContrastAdjustment (how ink colors are handled in high contrast).
 * Values extracted from Microsoft.UI.Xaml.winmd (UseSystemColorsWhenNecessary=0, UseSystemColors=1, UseOriginalColors=2).
 */
enum class InkHighContrastAdjustment(internal val native: Int) {
    /** Uses system colors only when the contrast with the background is insufficient (the default). */
    USE_SYSTEM_COLORS_WHEN_NECESSARY(0),

    /** Always draws with system colors. */
    USE_SYSTEM_COLORS(1),

    /** Always draws with the original colors. */
    USE_ORIGINAL_COLORS(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkHighContrastAdjustment = entries.first { it.native == native }
    }
}

/**
 * Windows.UI.Input.Inking.InkPresenterPredefinedConfiguration (OS-defined ink input configurations).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd (SimpleSinglePointer=0, SimpleMultiplePointer=1).
 */
enum class InkPresenterPredefinedConfiguration(internal val native: Int) {
    /** Draws ink with a single pointer only (touch is ignored while drawing with the pen). */
    SIMPLE_SINGLE_POINTER(0),

    /** Ink can be drawn with multiple pointers at the same time. */
    SIMPLE_MULTIPLE_POINTER(1),
}

/**
 * The WinUI 3 InkPresenter (experimental in Windows App SDK 2.5; Swing has no equivalent):
 * manages the ink input and drawing of a canvas. Obtained with [WInkCanvas.inkPresenter].
 *
 * - Input: [inputDeviceTypes] (pen only by default) / [isInputEnabled] / [inputProcessingMode] / [rightDragAction] /
 *   [isPrimaryBarrelButtonInputEnabled] / [isEraserInputEnabled] / [setPredefinedConfiguration]
 * - Drawing: [defaultDrawingAttributes] (the drawing attributes of lines drawn from now on) / [highContrastAdjustment]
 * - Events: [addStrokesCollectedListener] / [addStrokesErasedListener] (notified after being reflected in the model),
 *   [addStrokeInputListener] (StrokeInput), [addUnprocessedInputListener] (UnprocessedInput)
 * - Custom drying of ink: [activateCustomDrying]
 *
 * The stroke data (InkPresenter.StrokeContainer) is handled by [WInkCanvas.model].
 */
class WInkPresenter internal constructor(
    /** The canvas that owns this InkPresenter. */
    val inkCanvas: WInkCanvas,
    /** IInkPresenter (owned by the canvas). */
    internal val presenter: ComPtr,
) {
    private val inputProcessingConfiguration: ComPtr by lazy {
        inkCanvas.own(presenter.getPtr(InkInterop.IInkPresenter_get_InputProcessingConfiguration))
    }
    private val inputConfiguration: ComPtr by lazy {
        inkCanvas.own(presenter.getPtr(InkInterop.IInkPresenter_get_InputConfiguration))
    }
    private val strokeInput: ComPtr by lazy { inkCanvas.own(presenter.getPtr(InkInterop.IInkPresenter_get_StrokeInput)) }
    private val unprocessedInput: ComPtr by lazy {
        inkCanvas.own(presenter.getPtr(InkInterop.IInkPresenter_get_UnprocessedInput))
    }

    private val strokesCollectedListeners = mutableListOf<InkStrokesListener>()
    private val strokesErasedListeners = mutableListOf<InkStrokesListener>()
    private val strokeInputListeners = mutableListOf<InkStrokeInputListener>()
    private val unprocessedInputListeners = mutableListOf<InkUnprocessedInputListener>()

    /** The tokens of the 4 StrokeInput events (subscribed only while there are listeners). */
    private var strokeInputTokens: LongArray? = null

    /** The tokens of the 7 UnprocessedInput events (subscribed only while there are listeners). */
    private var unprocessedInputTokens: LongArray? = null

    private var synchronizer: WInkSynchronizer? = null

    /**
     * The devices that can input ink (InputDeviceTypes). The InkCanvas default is pen only
     * ({[CoreInputDeviceType.PEN]}).
     */
    var inputDeviceTypes: Set<CoreInputDeviceType>
        get() = CoreInputDeviceType.setOf(presenter.getInt(InkInterop.IInkPresenter_get_InputDeviceTypes))
        set(value) = presenter.call(InkInterop.IInkPresenter_put_InputDeviceTypes, CoreInputDeviceType.maskOf(value))

    /** Whether input is accepted (IsInputEnabled). When false, the canvas is display-only. */
    var isInputEnabled: Boolean
        get() = presenter.getBool(InkInterop.IInkPresenter_get_IsInputEnabled)
        set(value) = presenter.putBool(InkInterop.IInkPresenter_put_IsInputEnabled, value)

    /**
     * The drawing attributes of lines drawn from now on. Getting uses InkPresenter.CopyDefaultDrawingAttributes
     * (a copy of the current value), and setting uses UpdateDefaultDrawingAttributes. When an InkToolbar is attached,
     * this is rewritten to the attributes of the selected pen.
     */
    var defaultDrawingAttributes: InkDrawingAttributes
        get() {
            val native = presenter.getPtr(InkInterop.IInkPresenter_CopyDefaultDrawingAttributes)
            return try {
                InkNative.readDrawingAttributes(native)
            } finally {
                native.release()
            }
        }
        set(value) {
            val native = InkNative.createDrawingAttributes(value)
            try {
                presenter.call(InkInterop.IInkPresenter_UpdateDefaultDrawingAttributes, native)
            } finally {
                native.release()
            }
        }

    /** Applies an OS-defined input configuration (SetPredefinedConfiguration). */
    fun setPredefinedConfiguration(configuration: InkPresenterPredefinedConfiguration) {
        presenter.call(InkInterop.IInkPresenter_SetPredefinedConfiguration, configuration.native)
    }

    /** How ink colors are handled in high contrast (HighContrastAdjustment). */
    var highContrastAdjustment: InkHighContrastAdjustment
        get() = InkHighContrastAdjustment.of(presenter.getInt(InkInterop.IInkPresenter_get_HighContrastAdjustment))
        set(value) = presenter.call(InkInterop.IInkPresenter_put_HighContrastAdjustment, value.native)

    /** The input processing mode (InputProcessingConfiguration.Mode): draw, erase, or do not process. */
    var inputProcessingMode: InkInputProcessingMode
        get() = InkInputProcessingMode.of(inputProcessingConfiguration.getInt(InkInterop.IInkInputProcessingConfiguration_get_Mode))
        set(value) = inputProcessingConfiguration.call(InkInterop.IInkInputProcessingConfiguration_put_Mode, value.native)

    /**
     * How drags with the barrel button or the right button are handled (InputProcessingConfiguration.RightDragAction).
     */
    var rightDragAction: InkInputRightDragAction
        get() = InkInputRightDragAction.of(
            inputProcessingConfiguration.getInt(InkInterop.IInkInputProcessingConfiguration_get_RightDragAction),
        )
        set(value) = inputProcessingConfiguration.call(InkInterop.IInkInputProcessingConfiguration_put_RightDragAction, value.native)

    /**
     * Whether input made while pressing the pen's barrel button is treated as ink
     * (InputConfiguration.IsPrimaryBarrelButtonInputEnabled).
     */
    var isPrimaryBarrelButtonInputEnabled: Boolean
        get() = inputConfiguration.getBool(InkInterop.IInkInputConfiguration_get_IsPrimaryBarrelButtonInputEnabled)
        set(value) = inputConfiguration.putBool(InkInterop.IInkInputConfiguration_put_IsPrimaryBarrelButtonInputEnabled, value)

    /**
     * Whether input with the eraser end of the pen is treated as an eraser (InputConfiguration.IsEraserInputEnabled).
     */
    var isEraserInputEnabled: Boolean
        get() = inputConfiguration.getBool(InkInterop.IInkInputConfiguration_get_IsEraserInputEnabled)
        set(value) = inputConfiguration.putBool(InkInterop.IInkInputConfiguration_put_IsEraserInputEnabled, value)

    // ------------------------------------------------------------------
    // Custom drying of ink
    // ------------------------------------------------------------------

    /** Whether [activateCustomDrying] has been called. */
    val isCustomDryingActive: Boolean
        get() = synchronizer != null

    /**
     * Switches to a mode where the app takes over drawing finished lines (dry ink) (ActivateCustomDrying).
     * Receive the finished lines with [WInkSynchronizer.beginDry] of the returned [WInkSynchronizer], draw them in the
     * app, and then call [WInkSynchronizer.endDry] (usually done inside [addStrokesCollectedListener]).
     *
     * Once enabled, this canvas neither writes drawn lines to the model nor draws the model's lines
     * (because InkStrokeContainer, where dry ink is kept, goes away). It is used, for example, to put the finished
     * lines into the model of another canvas to display them. Call it before the canvas starts receiving input
     * (before it is placed in a window), while no lines are displayed yet (the model is empty). Once any line has been
     * displayed or manipulated, WinUI rejects the call with E_ILLEGAL_METHOD_CALL. From the second call on, the same
     * [WInkSynchronizer] is returned.
     */
    fun activateCustomDrying(): WInkSynchronizer {
        synchronizer?.let { return it }
        val native = inkCanvas.own(presenter.getPtr(InkInterop.IInkPresenter_ActivateCustomDrying))
        return WInkSynchronizer(this, native).also {
            synchronizer = it
            inkCanvas.onCustomDryingActivated()
        }
    }

    // ------------------------------------------------------------------
    // StrokesCollected / StrokesErased (notified after the canvas reflects them in the model)
    // ------------------------------------------------------------------

    /**
     * Subscribes to notifications when lines are drawn (StrokesCollected). Called after the canvas adds them to the
     * model.
     */
    fun addStrokesCollectedListener(listener: InkStrokesListener) {
        strokesCollectedListeners += listener
    }

    /** Removes a listener registered with [addStrokesCollectedListener]. */
    fun removeStrokesCollectedListener(listener: InkStrokesListener) {
        strokesCollectedListeners -= listener
    }

    /**
     * Subscribes to notifications when lines are erased with the eraser (StrokesErased). Called after the canvas
     * removes them from the model.
     */
    fun addStrokesErasedListener(listener: InkStrokesListener) {
        strokesErasedListeners += listener
    }

    /** Removes a listener registered with [addStrokesErasedListener]. */
    fun removeStrokesErasedListener(listener: InkStrokesListener) {
        strokesErasedListeners -= listener
    }

    internal fun fireStrokesCollected(strokes: List<InkStroke>) {
        val event = InkStrokesEvent(this, strokes)
        for (listener in strokesCollectedListeners.toList()) listener.strokesChanged(event)
    }

    internal fun fireStrokesErased(strokes: List<InkStroke>) {
        val event = InkStrokesEvent(this, strokes)
        for (listener in strokesErasedListeners.toList()) listener.strokesChanged(event)
    }

    // ------------------------------------------------------------------
    // StrokeInput / UnprocessedInput
    // ------------------------------------------------------------------

    /** Subscribes to stroke input events (InkPresenter.StrokeInput). */
    fun addStrokeInputListener(listener: InkStrokeInputListener) {
        if (strokeInputTokens == null) {
            strokeInputTokens = LongArray(STROKE_INPUT_ADD_SLOTS.size) { kind ->
                strokeInput.addEventHandler(
                    "WinUI4K.InkStrokeInputHandler",
                    InkInterop.IID_InkStrokeInputPointerHandler,
                    STROKE_INPUT_ADD_SLOTS[kind],
                ) { _, args -> dispatchStrokeInput(kind, ComPtr(args)) }
            }
        }
        strokeInputListeners += listener
    }

    /** Removes a listener registered with [addStrokeInputListener]. */
    fun removeStrokeInputListener(listener: InkStrokeInputListener) {
        strokeInputListeners -= listener
        val tokens = strokeInputTokens
        if (strokeInputListeners.isEmpty() && tokens != null) {
            STROKE_INPUT_REMOVE_SLOTS.forEachIndexed { i, slot -> strokeInput.removeEventHandler(slot, tokens[i]) }
            strokeInputTokens = null
        }
    }

    private fun dispatchStrokeInput(kind: Int, args: ComPtr) {
        val event = InkPointerEvent(this, args)
        try {
            for (listener in strokeInputListeners.toList()) {
                when (kind) {
                    0 -> listener.strokeStarted(event)
                    1 -> listener.strokeContinued(event)
                    2 -> listener.strokeEnded(event)
                    else -> listener.strokeCanceled(event)
                }
            }
        } finally {
            event.dispose()
        }
    }

    /** Subscribes to events for input that was not processed as ink (InkPresenter.UnprocessedInput). */
    fun addUnprocessedInputListener(listener: InkUnprocessedInputListener) {
        if (unprocessedInputTokens == null) {
            unprocessedInputTokens = LongArray(UNPROCESSED_INPUT_ADD_SLOTS.size) { kind ->
                unprocessedInput.addEventHandler(
                    "WinUI4K.InkUnprocessedInputHandler",
                    InkInterop.IID_InkUnprocessedInputPointerHandler,
                    UNPROCESSED_INPUT_ADD_SLOTS[kind],
                ) { _, args -> dispatchUnprocessedInput(kind, ComPtr(args)) }
            }
        }
        unprocessedInputListeners += listener
    }

    /** Removes a listener registered with [addUnprocessedInputListener]. */
    fun removeUnprocessedInputListener(listener: InkUnprocessedInputListener) {
        unprocessedInputListeners -= listener
        val tokens = unprocessedInputTokens
        if (unprocessedInputListeners.isEmpty() && tokens != null) {
            UNPROCESSED_INPUT_REMOVE_SLOTS.forEachIndexed { i, slot -> unprocessedInput.removeEventHandler(slot, tokens[i]) }
            unprocessedInputTokens = null
        }
    }

    @Suppress("CyclomaticComplexMethod") // Only dispatches by event kind (7 kinds)
    private fun dispatchUnprocessedInput(kind: Int, args: ComPtr) {
        val event = InkPointerEvent(this, args)
        try {
            for (listener in unprocessedInputListeners.toList()) {
                when (kind) {
                    0 -> listener.pointerEntered(event)
                    1 -> listener.pointerHovered(event)
                    2 -> listener.pointerExited(event)
                    3 -> listener.pointerPressed(event)
                    4 -> listener.pointerMoved(event)
                    5 -> listener.pointerReleased(event)
                    else -> listener.pointerLost(event)
                }
            }
        } finally {
            event.dispose()
        }
    }

    private companion object {
        /**
         * In the order StrokeStarted / StrokeContinued / StrokeEnded / StrokeCanceled (matches the kind in
         * dispatchStrokeInput).
         */
        val STROKE_INPUT_ADD_SLOTS = intArrayOf(
            InkInterop.IInkStrokeInput_add_StrokeStarted,
            InkInterop.IInkStrokeInput_add_StrokeContinued,
            InkInterop.IInkStrokeInput_add_StrokeEnded,
            InkInterop.IInkStrokeInput_add_StrokeCanceled,
        )
        val STROKE_INPUT_REMOVE_SLOTS = intArrayOf(
            InkInterop.IInkStrokeInput_remove_StrokeStarted,
            InkInterop.IInkStrokeInput_remove_StrokeContinued,
            InkInterop.IInkStrokeInput_remove_StrokeEnded,
            InkInterop.IInkStrokeInput_remove_StrokeCanceled,
        )

        /**
         * In the order Entered / Hovered / Exited / Pressed / Moved / Released / Lost
         * (matches the kind in dispatchUnprocessedInput).
         */
        val UNPROCESSED_INPUT_ADD_SLOTS = intArrayOf(
            InkInterop.IInkUnprocessedInput_add_PointerEntered,
            InkInterop.IInkUnprocessedInput_add_PointerHovered,
            InkInterop.IInkUnprocessedInput_add_PointerExited,
            InkInterop.IInkUnprocessedInput_add_PointerPressed,
            InkInterop.IInkUnprocessedInput_add_PointerMoved,
            InkInterop.IInkUnprocessedInput_add_PointerReleased,
            InkInterop.IInkUnprocessedInput_add_PointerLost,
        )
        val UNPROCESSED_INPUT_REMOVE_SLOTS = intArrayOf(
            InkInterop.IInkUnprocessedInput_remove_PointerEntered,
            InkInterop.IInkUnprocessedInput_remove_PointerHovered,
            InkInterop.IInkUnprocessedInput_remove_PointerExited,
            InkInterop.IInkUnprocessedInput_remove_PointerPressed,
            InkInterop.IInkUnprocessedInput_remove_PointerMoved,
            InkInterop.IInkUnprocessedInput_remove_PointerReleased,
            InkInterop.IInkUnprocessedInput_remove_PointerLost,
        )
    }
}

/**
 * Synchronization for custom drying of ink (Microsoft.UI.Xaml.Controls.InkSynchronizer).
 * Obtained with [WInkPresenter.activateCustomDrying].
 */
class WInkSynchronizer internal constructor(
    /** The InkPresenter that owns this synchronizer. */
    val inkPresenter: WInkPresenter,
    private val synchronizer: ComPtr,
) {
    /**
     * Begins handing over finished lines (BeginDry). When the app draws the returned lines and then calls [endDry],
     * the lines being drawn on the canvas (wet ink) disappear. Calling this when there are no finished lines makes
     * WinUI fail with E_UNEXPECTED (call it inside a [WInkPresenter.addStrokesCollectedListener] notification).
     */
    fun beginDry(): List<InkStroke> {
        val view = synchronizer.getPtr(InkInterop.IInkSynchronizer_BeginDry)
        return try {
            InkNative.readVectorView(view).map { native ->
                try {
                    InkNative.readStroke(native)
                } finally {
                    native.release()
                }
            }
        } finally {
            view.release()
        }
    }

    /** Notifies that drawing the lines received with [beginDry] is finished (EndDry). */
    fun endDry() {
        synchronizer.call(InkInterop.IInkSynchronizer_EndDry)
    }
}
