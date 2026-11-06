package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
import java.util.function.DoubleConsumer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.Primitives.SliderSnapsTo (where the thumb snaps to).
 * Values extracted from the winmd (StepValues=0, Ticks=1).
 */
enum class SliderSnapsTo(internal val native: Int) {
    /** Snaps to multiples of stepFrequency (default). */
    STEP_VALUES(0),

    /** Snaps to the tickFrequency graduations. */
    TICKS(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): SliderSnapsTo = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.Primitives.TickPlacement (where tick marks are drawn).
 * Values extracted from the winmd (None=0, TopLeft=1, BottomRight=2, Outside=3, Inline=4).
 */
enum class TickPlacement(internal val native: Int) {
    /** Do not show ticks. */
    NONE(0),

    /** Above when horizontal, left when vertical. */
    TOP_LEFT(1),

    /** Below when horizontal, right when vertical. */
    BOTTOM_RIGHT(2),

    /** On both sides of the track. */
    OUTSIDE(3),

    /** On the track (Slider's default; ticks are drawn only when [WSlider.tickFrequency] is greater than 0). */
    INLINE(4),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): TickPlacement = entries.first { it.native == native }
    }
}

/**
 * JSlider-like: WinUI 3's Slider (a RangeBase subclass).
 *
 * The range of values is held by [model] (a [WBoundedRangeModel]; [WDefaultBoundedRangeModel] by default), and [value] / [minimum] / [maximum] /
 * [extent] / [valueIsAdjusting] read and write the model. Model changes are copied to the native RangeBase (Minimum / Maximum / Value),
 * and a native Value changed by user interaction is written back to the model. As in Swing, the [addChangeListener] listeners are
 * called once for any change (not only the value but also the minimum, maximum, extent and whether it is being dragged; the source is this slider).
 *
 * Differences from Swing:
 * - The value is `Double`, not `int` (write Java's `int v = slider.getValue()` as `(int) slider.getValue()`). The default value is 0, not JSlider's 50
 * - WinUI's Slider has no extent (thumb length), so [extent] is represented only by setting the native Maximum to `maximum - extent` so that the value
 *   does not exceed `maximum - extent` (the thumb looks the same)
 * - [orientation] is an [Orientation] (WinUI's values are the reverse of JSlider's ints, so no int form is provided; in Java use [HORIZONTAL] / [VERTICAL])
 * - There is one level of ticks (see [majorTickSpacing] / [minorTickSpacing]). There are no tick labels (paintLabels / setLabelTable)
 *
 * Also provides [stepFrequency] / [snapsTo] / [tickFrequency] / [tickPlacement] / [isDirectionReversed] / [header] (Slider).
 */
class WSlider @JvmOverloads constructor(minimum: Double = 0.0, maximum: Double = 100.0, value: Double = 0.0) : WControl(
    Activation.composeDefault(XamlInterop.CLS_Slider, XamlInterop.IID_ISliderFactory), // default interface = ISlider
) {
    /** JSlider(int orientation)-style: creates a slider with the given orientation (range 0..100, value 0). */
    constructor(orientation: Orientation) : this() {
        this.orientation = orientation
    }

    /** JSlider(int orientation, int min, int max, int value)-style: creates a slider with the given orientation, range and value. */
    constructor(orientation: Orientation, minimum: Double, maximum: Double, value: Double) : this(minimum, maximum, value) {
        this.orientation = orientation
    }

    /** JSlider(BoundedRangeModel)-style: creates a slider that uses [model] for its range of values (horizontal). */
    constructor(model: WBoundedRangeModel) : this() {
        this.model = model
    }

    /** The Primitives.IRangeBase view holding Value / Minimum / Maximum / ValueChanged. */
    private val rangeBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IRangeBase))
    }

    /** The ChangeListeners. Drag detection (the pointer handlers) is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addDragHandlers() }

    private var rangeModel: WBoundedRangeModel = WDefaultBoundedRangeModel()

    /**
     * A listener that copies model changes to the native side and passes them on to this slider's listeners. Through it the model strongly references
     * the slider, but the native side references the slider only weakly ([addValueChangedHandler]), so there is no cycle across the boundary.
     */
    private val modelListener = WChangeListener { onModelChanged() }

    /** Whether the model state is being copied to the native side (native ValueChanged during that time is not written back to the model). */
    private var pushing = false

    private var dragHandlersAdded = false

    /** Whether [model] has been read or written (the first time, the slider is pinned with ListenerAnchor; [pinForModel]). */
    private var modelExposed = false

    /**
     * JSlider.getModel / setModel-style: the range model. [WDefaultBoundedRangeModel] by default (0..100, value 0, extent 0).
     * Sharing one model between several sliders (or components using the same model) makes all of them move whichever one is moved.
     * When replaced, the native side is updated to the new model, but as with JSlider the [addChangeListener] listeners are not called.
     * The model references the slider through a listener, so keeping the model keeps the slider from being collected too (same as Swing).
     * A slider whose model has been read or written even once (including the constructor that takes a model) is, for the other components sharing the model, the same as when listeners are added:
     * it is not collected while shown even without references (it is not collected if never put in a window; it is collected after being put in and removed).
     */
    var model: WBoundedRangeModel
        get() {
            pinForModel()
            return rangeModel
        }
        set(newModel) {
            pinForModel()
            if (newModel === rangeModel) return
            rangeModel.removeChangeListener(modelListener)
            rangeModel = newModel
            newModel.addChangeListener(modelListener)
            pushToNative()
        }

    /** The current value ([model]'s value). Values outside the range (`minimum..maximum - extent`) are clamped (with [WDefaultBoundedRangeModel]). */
    var value: Double
        get() = rangeModel.value
        set(value) {
            rangeModel.value = value
        }

    /** The minimum ([model]'s minimum). Increasing it also raises the value and the maximum (with [WDefaultBoundedRangeModel]). */
    var minimum: Double
        get() = rangeModel.minimum
        set(value) {
            rangeModel.minimum = value
        }

    /** The maximum ([model]'s maximum). Decreasing it also lowers the value and the minimum (with [WDefaultBoundedRangeModel]). */
    var maximum: Double
        get() = rangeModel.maximum
        set(value) {
            rangeModel.maximum = value
        }

    /**
     * JSlider.getExtent / setExtent-style: the extent ([model]'s extent). 0 by default. The value moves only up to `maximum - extent`
     * (represented by setting the native Maximum to `maximum - extent`; the thumb looks the same).
     */
    var extent: Double
        get() = rangeModel.extent
        set(value) {
            rangeModel.extent = value
        }

    /**
     * JSlider.getValueIsAdjusting / setValueIsAdjusting-style: whether the thumb is being dragged ([model]'s valueIsAdjusting).
     * Pressing the left button on the slider makes it true, and releasing or losing capture makes it false; each calls the [addChangeListener]
     * listeners as one change (the final value can be read on the false notification at release). WinUI moves the value to the thumb position before
     * reporting the press, so when the track is clicked, the value change notification arrives before the true notification.
     * Pointer detection starts when a listener is added or [model] is read or written.
     */
    var valueIsAdjusting: Boolean
        get() = rangeModel.valueIsAdjusting
        set(value) {
            rangeModel.valueIsAdjusting = value
        }

    /** The step the value moves by while dragging (Slider.StepFrequency). */
    var stepFrequency: Double
        get() = inspectable.getDouble(XamlInterop.ISlider_get_StepFrequency)
        set(value) = inspectable.call(XamlInterop.ISlider_put_StepFrequency, value)

    /** Where the thumb snaps to (Slider.SnapsTo). */
    var snapsTo: SliderSnapsTo
        get() = SliderSnapsTo.of(inspectable.getInt(XamlInterop.ISlider_get_SnapsTo))
        set(value) = inspectable.call(XamlInterop.ISlider_put_SnapsTo, value.native)

    /** The spacing between tick marks (Slider.TickFrequency). */
    var tickFrequency: Double
        get() = inspectable.getDouble(XamlInterop.ISlider_get_TickFrequency)
        set(value) = inspectable.call(XamlInterop.ISlider_put_TickFrequency, value)

    /** Where ticks are drawn (Slider.TickPlacement). [TickPlacement.INLINE] by default (confirmed in an E2E test). */
    var tickPlacement: TickPlacement
        get() = TickPlacement.of(inspectable.getInt(XamlInterop.ISlider_get_TickPlacement))
        set(value) = inspectable.call(XamlInterop.ISlider_put_TickPlacement, value.native)

    /** The orientation of the slider (Slider.Orientation). Horizontal by default. */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(XamlInterop.ISlider_get_Orientation))
        set(value) = inspectable.call(XamlInterop.ISlider_put_Orientation, value.native)

    /** Whether the increasing direction is reversed (Slider.IsDirectionReversed). */
    var isDirectionReversed: Boolean
        get() = inspectable.getBool(XamlInterop.ISlider_get_IsDirectionReversed)
        set(value) = inspectable.putBool(XamlInterop.ISlider_put_IsDirectionReversed, value)

    /** JSlider.getInverted / setInverted-style: same as [isDirectionReversed]. */
    var inverted: Boolean
        get() = isDirectionReversed
        set(value) {
            isDirectionReversed = value
        }

    /**
     * JSlider.getMajorTickSpacing / setMajorTickSpacing-style: the spacing of major ticks. 0 by default (no ticks).
     * WinUI has one level of ticks, so if this is greater than 0 it becomes [tickFrequency] (if 0, [minorTickSpacing] is used).
     * To show ticks, also set [paintTicks] to true (same as Swing). Returns the value set as is (changing [tickFrequency] directly does not change it).
     */
    var majorTickSpacing: Double = 0.0
        set(value) {
            field = value
            applyTickSpacing()
        }

    /**
     * JSlider.getMinorTickSpacing / setMinorTickSpacing-style: the spacing of minor ticks. 0 by default.
     * WinUI has one level of ticks, so this is used for [tickFrequency] only when [majorTickSpacing] is 0 (major ticks are drawn if there are any).
     */
    var minorTickSpacing: Double = 0.0
        set(value) {
            field = value
            applyTickSpacing()
        }

    /**
     * JSlider.getPaintTicks / setPaintTicks-style: whether ticks are drawn (whether [tickPlacement] is other than NONE).
     * When set to true, if [tickPlacement] is NONE it becomes [TickPlacement.BOTTOM_RIGHT], the same side as JSlider (below when horizontal, right when vertical),
     * and any other placement is kept. Setting it to false makes it NONE.
     * Unlike Swing, WinUI's default placement is INLINE, so it returns true if nothing is set (ticks are not drawn until a spacing is set;
     * the default placement is not made NONE so as not to change the existing appearance).
     */
    var paintTicks: Boolean
        get() = tickPlacement != TickPlacement.NONE
        set(value) {
            if (!value) {
                tickPlacement = TickPlacement.NONE
            } else if (tickPlacement == TickPlacement.NONE) {
                tickPlacement = TickPlacement.BOTTOM_RIGHT
            }
        }

    /** JSlider.getSnapToTicks / setSnapToTicks-style: whether the thumb snaps to ticks (whether [snapsTo] is TICKS; false is STEP_VALUES). */
    var snapToTicks: Boolean
        get() = snapsTo == SliderSnapsTo.TICKS
        set(value) {
            snapsTo = if (value) SliderSnapsTo.TICKS else SliderSnapsTo.STEP_VALUES
        }

    /**
     * The header above the slider (Slider.Header). It is of type Object, so a boxed string is passed, and it is unboxed from WinUI when read.
     * Returns "" for anything other than a string (such as an element put in on the XAML side) or when unset.
     */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ISlider_get_Header) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            try {
                inspectable.call(XamlInterop.ISlider_put_Header, boxed.ptr)
            } finally {
                boxed.release()
            }
        }

    init {
        rangeModel.addChangeListener(modelListener)
        addValueChangedHandler()
        this.minimum = minimum
        this.maximum = maximum
        this.value = value
        pushToNative()
    }

    /**
     * ChangeListener-like: subscribes to value changes. The listener receives the new value.
     * Called only when the value changes (not for changes of only the minimum, maximum, extent or whether it is being dragged).
     * To receive every change, use the form that takes a [WChangeListener] (`addChangeListener(WChangeListener { e -> })`).
     */
    @JvmSynthetic
    fun addChangeListener(listener: (Double) -> Unit) {
        changeListeners.addAdapter(listener, valueChangeAdapter { listener(it) })
    }

    /**
     * The legacy Java `addChangeListener(DoubleConsumer)`. Kept only for binary compatibility (linking already compiled callers)
     * and not visible from Java source (HIDDEN). In Java, use the form that takes a [WChangeListener].
     */
    @Deprecated("Use addChangeListener that takes a WChangeListener", level = DeprecationLevel.HIDDEN)
    @JvmName("addChangeListener")
    fun addChangeListenerForJava(listener: DoubleConsumer) {
        changeListeners.addAdapter(listener, valueChangeAdapter { listener.accept(it) })
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this slider) every time the slider's state changes.
     * As with JSlider, it is called once per change whichever of the value, minimum, maximum, extent or dragging state ([valueIsAdjusting]) changes.
     * Called both for user interaction and for settings from the program. Read the new value with `((WSlider) e.getSource()).getValue()`.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    @JvmSynthetic
    fun removeChangeListener(listener: (Double) -> Unit) {
        changeListeners.removeAdapter(listener)
    }

    /** The legacy Java `removeChangeListener(DoubleConsumer)` (HIDDEN; kept only for binary compatibility). */
    @Deprecated("Use removeChangeListener that takes a WChangeListener", level = DeprecationLevel.HIDDEN)
    @JvmName("removeChangeListener")
    fun removeChangeListenerForJava(listener: DoubleConsumer) {
        changeListeners.removeAdapter(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** JSlider.getChangeListeners-style: the ChangeListeners currently added (in the order they were added; Kotlin lambdas appear wrapped). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    /** Passes the new value to [deliver] only when the value changed (for the legacy value listeners). */
    private fun valueChangeAdapter(deliver: (Double) -> Unit): WChangeListener {
        var last = value
        return WChangeListener {
            val now = value
            if (now != last) {
                last = now
                deliver(now)
            }
        }
    }

    /**
     * Pins the slider with ListenerAnchor only once when a model is passed or read, the same as when listeners are added.
     * The native handlers reference the slider only weakly, so without pinning it would be collected even while shown and stop writing user operations back to the model
     * (and they would no longer reach the other components sharing the model or listeners added directly to the model). Drag detection also starts here.
     */
    private fun pinForModel() {
        addDragHandlers() // Also tell listeners added directly to the model whether a drag is in progress
        if (modelExposed) return
        modelExposed = true
        listenerAnchor.listenerAdded()
    }

    private fun applyTickSpacing() {
        tickFrequency = if (majorTickSpacing > 0) majorTickSpacing else minorTickSpacing
    }

    private fun onModelChanged() {
        pushToNative()
        changeListeners.fire()
    }

    /** Copies the model state to the native side. Writes minimum, then maximum, then value, so WinUI's intermediate clamping is overwritten by the final value. */
    private fun pushToNative() {
        val model = rangeModel
        val wasPushing = pushing
        pushing = true
        try {
            rangeBase.call(XamlInterop.IRangeBase_put_Minimum, model.minimum)
            rangeBase.call(XamlInterop.IRangeBase_put_Maximum, model.maximum - model.extent)
            // WinUI remembers the value before clamping to the range and restores it when the range is widened, so rewrite the model's value every time
            rangeBase.call(XamlInterop.IRangeBase_put_Value, model.value)
        } finally {
            pushing = wasPushing
        }
    }

    /** Writes the native value changed by user interaction (drag, click, key) back to the model. */
    private fun onNativeValueChanged(newValue: Double) {
        if (pushing) return
        rangeModel.value = newValue
    }

    /** Pressed with the pointer (true), or released or lost capture (false). */
    private fun onPointer(pressed: Boolean, args: Ptr) {
        if (pressed && !XamlPointerEvent(ComPtr(args)).isLeftButtonPressed) return
        if (rangeModel.valueIsAdjusting != pressed) rangeModel.valueIsAdjusting = pressed
    }

    private fun addValueChangedHandler() {
        // The handler stays on the native side, so reference the slider weakly (while shown with listeners, the ListenerAnchor holds it strongly)
        val slider = WeakReference(this)
        rangeBase.addEventHandler(
            "WinUI4K.ValueChangedHandler",
            XamlInterop.IID_RangeBaseValueChangedEventHandler,
            XamlInterop.IRangeBase_add_ValueChanged,
        ) { _, args ->
            val newValue = ComPtr(args).getDouble(XamlInterop.IRangeBaseValueChangedEventArgs_get_NewValue)
            slider.get()?.onNativeValueChanged(newValue)
        }
    }

    private fun addDragHandlers() {
        if (dragHandlersAdded) return
        dragHandlersAdded = true
        val slider = WeakReference(this)
        // The thumb (Thumb) and the track mark the press as handled, so also receive handled events
        for ((eventSlot, pressed) in listOf(
            XamlInterop.IUIElementStatics_get_PointerPressedEvent to true,
            XamlInterop.IUIElementStatics_get_PointerReleasedEvent to false,
            XamlInterop.IUIElementStatics_get_PointerCaptureLostEvent to false,
        )) {
            Xaml.addHandlerHandledToo(
                this,
                eventSlot,
                XamlInterop.IID_PointerEventHandler,
                FoundationInterop.IID_IReference_PointerEventHandler,
            ) { _, args -> slider.get()?.onPointer(pressed, args) }
        }
    }

    companion object {
        /** JSlider.HORIZONTAL-style: horizontal ([Orientation.HORIZONTAL]). For Java's `new WSlider(WSlider.HORIZONTAL)`. */
        @JvmField
        val HORIZONTAL: Orientation = Orientation.HORIZONTAL

        /** JSlider.VERTICAL-style: vertical ([Orientation.VERTICAL]). For Java's `new WSlider(WSlider.VERTICAL)`. */
        @JvmField
        val VERTICAL: Orientation = Orientation.VERTICAL
    }
}
