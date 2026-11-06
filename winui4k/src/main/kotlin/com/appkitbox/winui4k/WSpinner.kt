package com.appkitbox.winui4k

import com.appkitbox.winui4k.WSpinnerNumberModel.Companion.isIntegral
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
import java.text.ParseException
import java.util.function.DoubleConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.NumberBoxSpinButtonPlacementMode (spin button placement).
 * Values extracted from the winmd (Hidden=0, Compact=1, Inline=2).
 */
enum class SpinButtonPlacementMode(internal val native: Int) {
    /** Don't show spin buttons (default). */
    HIDDEN(0),

    /** Show them in a popup on focus. */
    COMPACT(1),

    /** Always show them at the right edge of the box. */
    INLINE(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): SpinButtonPlacementMode = entries.first { it.native == native }
    }
}

/**
 * JSpinner-like: WinUI 3's NumberBox.
 *
 * Provides numeric input, validation and incrementing/decrementing with spin buttons. The sequence of values is held by [model] (a [WSpinnerNumberModel]), and [value] / [minimum] / [maximum] /
 * [smallChange] read and write the model. Model changes are copied to the native NumberBox (Minimum / Maximum / SmallChange / Value),
 * and a native Value changed by user input or the spin buttons is written back to the model. As in Swing, the [addChangeListener] listeners
 * are called once for any model change (the source is this spinner).
 * NaN when empty. Enabling [acceptsExpression] lets it evaluate expressions like "(5 + 3) * 2".
 *
 * Differences from Swing:
 * - Only numeric models ([WSpinnerNumberModel]) can be displayed (see [model]). [value] is a `Double`, not an `Object`
 *   (Java's `(Integer) spinner.getValue()` does not compile, so write `(Integer) spinner.getModel().getValue()` or `(int) spinner.getValue()`)
 * - The value of the default model is not Swing's Integer 0 but a Double NaN representing no input (as before). Clearing the input with the default model makes [value] NaN.
 *   A spinner given a model through the constructor or [model] reverts to the previous value when the input is cleared, like Swing's COMMIT_OR_REVERT
 * - WinUI's NumberBox clamps values outside the range to the edge of the range (validation stays the default InvalidInputOverwritten). Putting a value outside the range into the model or [value]
 *   rewrites both the display and the model to the edge value (Swing's SpinnerNumberModel does not clamp). Going past the edge with the spin buttons also
 *   gives the edge value instead of stopping as in Swing
 * - The value is committed when focus leaves the input field or Enter is pressed (not on every character)
 * - With integer models such as Integer, a typed decimal is truncated toward 0 to an integer, as Swing's NumberFormatter does, and the display goes back to that integer
 *   (NumberBox's display format NumberFormatter stays the default; integer values are shown without a decimal point)
 */
class WSpinner @JvmOverloads constructor(value: Double = Double.NaN) : WControl(
    Activation.composeDefault(XamlInterop.CLS_NumberBox, XamlInterop.IID_INumberBoxFactory), // default interface = INumberBox
) {
    /**
     * Like JSpinner(SpinnerModel): creates a spinner that steps through the values of [model]. Clearing the input reverts to the previous value.
     * @throws IllegalArgumentException when a model other than [WSpinnerNumberModel] is passed (see [model])
     */
    constructor(model: WSpinnerModel) : this() {
        this.model = model
    }

    /** Its own IsTabStop is false, and UIElement.Focus passes focus to the template's input field (TextBox) ([isFocusable] / [requestFocusInWindow]). */
    override val focusIsDelegated: Boolean get() = true

    /** The ChangeListeners. The native ValueChanged is subscribed from creation, so nothing is registered here. */
    private val changeListeners = ChangeListeners(this) {}

    private var numberModel = WSpinnerNumberModel(value, null, null, 1.0)

    /**
     * A listener that copies model changes to the native side and passes them on to this spinner's listeners. Through it the model strongly references
     * the spinner, but the native side references the spinner only weakly ([addValueChangedHandler]), so there is no cycle across the boundary.
     */
    private val modelListener = WChangeListener { onModelChanged() }

    /** Whether the model state is being copied to the native side (native ValueChanged during that time is not written back to the model). */
    private var pushing = false

    /** Whether [model] has been read or written (the first time, the spinner is pinned with ListenerAnchor; [pinForModel]). */
    private var modelExposed = false

    /** Whether clearing the input reverts to the previous value (only for spinners given a model; the default model puts NaN). */
    private var revertsEmpty = false

    /**
     * Like JSpinner.getModel / setModel: the model of the sequence of values. The default is a [WSpinnerNumberModel] with value NaN (the constructor's value), no range and step 1.0.
     * Sharing one model between several spinners makes all of them move whichever one is changed. When replaced, the native side is updated to the new model, but
     * as with JSpinner the [addChangeListener] listeners are not called. A spinner with a model set reverts to the previous value when the input is cleared.
     * The model references the spinner through a listener, so keeping the model keeps the spinner from being collected too (same as Swing).
     * A spinner whose model has been read or written even once (including the constructor that takes a model) is, for the other components sharing the model, the same as when listeners are added:
     * it is not collected while shown even without references (it is not collected if never put in a window; it is collected after being put in and removed).
     *
     * At this point NumberBox can only display numbers, so only [WSpinnerNumberModel] is accepted
     * ([WSpinnerListModel] and [WSpinnerDateModel] cannot be attached).
     * @throws IllegalArgumentException when a model other than [WSpinnerNumberModel] is set
     */
    var model: WSpinnerModel
        get() {
            pinForModel()
            return numberModel
        }
        set(newModel) {
            require(newModel is WSpinnerNumberModel) {
                "WSpinner can only display a WSpinnerNumberModel: ${newModel.javaClass.name}"
            }
            pinForModel()
            revertsEmpty = true
            if (newModel === numberModel) return
            numberModel.removeChangeListener(modelListener)
            numberModel = newModel
            newModel.addChangeListener(modelListener)
            pushToNative()
        }

    /**
     * The current value ([model]'s value as a Double). NaN if nothing is entered. WinUI clamps values outside the range to the edge, and the model becomes the edge value too.
     * Decimals set on an integer model are truncated toward 0.
     * @throws IllegalArgumentException when NaN is set on an integer model
     */
    var value: Double
        get() = numberModel.number.toDouble()
        set(value) {
            numberModel.value = toModelNumber(value, numberModel.number)
        }

    /** The minimum ([model]'s minimum; NumberBox's default `-Double.MAX_VALUE` if null). */
    var minimum: Double
        get() = numberModel.minimum?.toDouble() ?: -Double.MAX_VALUE
        set(value) {
            numberModel.minimum = value
        }

    /** The maximum ([model]'s maximum; NumberBox's default `Double.MAX_VALUE` if null). */
    var maximum: Double
        get() = numberModel.maximum?.toDouble() ?: Double.MAX_VALUE
        set(value) {
            numberModel.maximum = value
        }

    /** The amount one spin button press changes the value by ([model]'s stepSize; copied to NumberBox.SmallChange). */
    var smallChange: Double
        get() = numberModel.stepSize.toDouble()
        set(value) {
            numberModel.stepSize = value
        }

    /** The amount PageUp / PageDown change the value by (NumberBox.LargeChange; not in the model). */
    var largeChange: Double
        get() = inspectable.getDouble(XamlInterop.INumberBox_get_LargeChange)
        set(value) = inspectable.call(XamlInterop.INumberBox_put_LargeChange, value)

    /** Like JSpinner.getNextValue: the next value in [model] (null if it goes beyond the upper limit of the range). Does not change the value. */
    val nextValue: Any? get() = numberModel.nextValue

    /** Like JSpinner.getPreviousValue: the previous value in [model] (null if it goes below the lower limit of the range). Does not change the value. */
    val previousValue: Any? get() = numberModel.previousValue

    /**
     * The header above the spinner (NumberBox.Header). It is of type Object, so a boxed string is passed, and it is unboxed from WinUI when read.
     * Returns "" for anything other than a string (such as an element put in on the XAML side) or when unset.
     */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.INumberBox_get_Header) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            try {
                inspectable.call(XamlInterop.INumberBox_put_Header, boxed.ptr)
            } finally {
                boxed.release()
            }
        }

    /** The placeholder shown when empty (NumberBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.INumberBox_get_PlaceholderText)
        set(value) {
            Hstring.use(value) { h -> inspectable.call(XamlInterop.INumberBox_put_PlaceholderText, h) }
        }

    /** Spin button placement (NumberBox.SpinButtonPlacementMode). */
    var spinButtonPlacementMode: SpinButtonPlacementMode
        get() = SpinButtonPlacementMode.of(inspectable.getInt(XamlInterop.INumberBox_get_SpinButtonPlacementMode))
        set(value) = inspectable.call(XamlInterop.INumberBox_put_SpinButtonPlacementMode, value.native)

    /** Whether to wrap around to the minimum once the maximum is exceeded (NumberBox.IsWrapEnabled). */
    var isWrapEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.INumberBox_get_IsWrapEnabled)
        set(value) = inspectable.putBool(XamlInterop.INumberBox_put_IsWrapEnabled, value)

    /** Whether to evaluate expression input like "(5 + 3) * 2" (NumberBox.AcceptsExpression). */
    var acceptsExpression: Boolean
        get() = inspectable.getBool(XamlInterop.INumberBox_get_AcceptsExpression)
        set(value) = inspectable.putBool(XamlInterop.INumberBox_put_AcceptsExpression, value)

    init {
        numberModel.addChangeListener(modelListener)
        addValueChangedHandler()
        pushToNative()
    }

    /**
     * Subscribes to value changes. The listener receives the new value. Called only when the value changes (not for changes of only the minimum, maximum or step).
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
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this spinner) every time the spinner's state changes.
     * As with JSpinner, it is called once per change whichever of [model]'s value, minimum, maximum or step changes.
     * Called both for user input and for settings from the program. Read the new value with `((WSpinner) e.getSource()).getValue()`.
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

    /** Like JSpinner.getChangeListeners: the ChangeListeners currently added (in the order they were added; Kotlin lambdas appear wrapped). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    /**
     * JSpinner.commitEdit-like: makes the NumberBox read the text being typed in the input field (the template's TextBox) into a value, the same as when the focus leaves
     * or Enter is pressed (also expressions if [acceptsExpression]). The read value is clamped to the range and written back to the model, and if the value changes,
     * the ChangeListeners are called. The text in the input field is replaced by the display of the committed value.
     *
     * An empty field sets the value to NaN with the default model (as before). With a spinner given a model or an integer model, it is treated as unreadable text,
     * as with Swing's JFormattedTextField. Before being placed in a window (while the template's input field does not exist yet), there is nothing to commit, so it does nothing.
     *
     * @throws ParseException for text that cannot be read as a number (expression). The value and model do not change, and the text stays in the input field (the same as Swing;
     *   WinUI's NumberBox returns unreadable text to the display of the previous value when the focus leaves, but this call does not)
     */
    @Throws(ParseException::class)
    fun commitEdit() {
        val input = Xaml.appliedTemplatePartOrNull(inspectable, INPUT_BOX_PART) ?: return
        val textBox = try {
            input.queryInterface(XamlInterop.IID_ITextBox)
        } finally {
            input.release()
        }
        try {
            val raw = textBox.getString(XamlInterop.ITextBox_get_Text)
            // After setting Value to NaN and putting the text back into Text, NumberBox reads it in ValidateInput (UpdateValueToText in NumberBox.cpp),
            // and only a readable value goes into Value. If it stays NaN, it could not be read (an empty field also becomes NaN)
            val wasPushing = pushing
            pushing = true
            val committed = try {
                inspectable.call(XamlInterop.INumberBox_put_Value, Double.NaN)
                Hstring.use(raw) { h -> inspectable.call(XamlInterop.INumberBox_put_Text, h) }
                inspectable.getDouble(XamlInterop.INumberBox_get_Value)
            } finally {
                pushing = wasPushing
            }
            // Only the default model (Double, which becomes NaN when the input is cleared) accepts an empty field as NaN
            val acceptsEmpty = !revertsEmpty && !numberModel.number.isIntegral()
            if (committed.isNaN() && (raw.isNotBlank() || !acceptsEmpty)) {
                pushToNative() // Return the value and display to the model's value, then put the typed text back
                Hstring.use(raw) { h -> textBox.call(XamlInterop.ITextBox_put_Text, h) }
                throw ParseException("Cannot be read as a number: \"$raw\"", 0)
            }
            onNativeValueChanged(committed)
        } finally {
            textBox.release()
        }
    }

    /** Passes the new value to [deliver] only when the value changed (for the legacy value listeners; NaN to NaN is not a change). */
    private fun valueChangeAdapter(deliver: (Double) -> Unit): WChangeListener {
        var last = value
        return WChangeListener {
            val now = value
            if (!sameValue(now, last)) {
                last = now
                deliver(now)
            }
        }
    }

    private fun onModelChanged() {
        // When WinUI clamped and rewrote the model, the listeners have already been told within the rewrite's notification
        if (!pushToNative()) changeListeners.fire()
    }

    /**
     * Copies the model state to the native side (minimum -> maximum -> step -> value). If WinUI clamps the value to the range, rewrites the model to that value too and
     * returns true (the rewrite's notification passes through here again and tells the listeners).
     */
    private fun pushToNative(): Boolean {
        val model = numberModel
        val wasPushing = pushing
        pushing = true
        val nativeValue = try {
            inspectable.call(XamlInterop.INumberBox_put_Minimum, model.minimum?.toDouble() ?: -Double.MAX_VALUE)
            inspectable.call(XamlInterop.INumberBox_put_Maximum, model.maximum?.toDouble() ?: Double.MAX_VALUE)
            val step = model.stepSize
            val integral = model.number.isIntegral()
            inspectable.call(XamlInterop.INumberBox_put_SmallChange, if (integral) step.toLong().toDouble() else step.toDouble())
            inspectable.call(XamlInterop.INumberBox_put_Value, model.number.toDouble())
            inspectable.getDouble(XamlInterop.INumberBox_get_Value)
        } finally {
            pushing = wasPushing
        }
        if (sameValue(nativeValue, model.number.toDouble())) return false
        val before = model.value
        model.value = toModelNumber(nativeValue, model.number)
        return model.value !== before
    }

    /** Writes the native value changed by user input, the spin buttons or keys back to the model. */
    private fun onNativeValueChanged(newValue: Double) {
        if (pushing) return
        val model = numberModel
        val current = model.number
        if (newValue.isNaN() && (revertsEmpty || current.isIntegral())) {
            pushToNative() // COMMIT_OR_REVERT: an empty field reverts to the previous value
            return
        }
        model.value = toModelNumber(newValue, current)
        // Even when truncating to an integer did not change the value (typing 3.2 over 3 and so on), set the display back to the model's value
        if (!sameValue(model.number.toDouble(), newValue)) pushToNative()
    }

    /**
     * Pins the spinner with ListenerAnchor only once when a model is passed or read, the same as when listeners are added.
     * The native handlers reference the spinner only weakly, so without pinning it would be collected even while shown and stop writing input back to the model.
     */
    private fun pinForModel() {
        if (modelExposed) return
        modelExposed = true
        listenerAnchor.listenerAdded()
    }

    private fun addValueChangedHandler() {
        // The handler stays on the native side, so reference the spinner weakly (while shown with listeners, the ListenerAnchor holds it strongly)
        val spinner = WeakReference(this)
        inspectable.addEventHandler(
            "WinUI4K.NumberBoxValueChangedHandler",
            XamlInterop.IID_NumberBoxValueChangedHandler,
            XamlInterop.INumberBox_add_ValueChanged,
        ) { _, args ->
            // args is NumberBoxValueChangedEventArgs; read NewValue and pass it along
            val newValue = ComPtr(args).getDouble(XamlInterop.INumberBoxValueChangedEventArgs_get_NewValue)
            spinner.get()?.onNativeValueChanged(newValue)
        }
    }

    private companion object {
        /** The name of the TextBox for entering text in the NumberBox template. */
        const val INPUT_BOX_PART = "InputBox"

        /** Compares treating NaN and NaN as equal too. */
        fun sameValue(a: Double, b: Double): Boolean = a == b || (a.isNaN() && b.isNaN())

        /** Converts [value] to a number of the same type as [template] (truncated toward 0 for integer types, as Swing's NumberFormatter does). */
        fun toModelNumber(value: Double, template: Number): Number {
            require(!value.isNaN() || !template.isIntegral()) { "NaN cannot be put into an integer model" }
            return when (template) {
                is Int -> value.toInt()
                is Long -> value.toLong()
                is Short -> value.toInt().toShort()
                is Byte -> value.toInt().toByte()
                is Float -> value.toFloat()
                else -> value
            }
        }
    }
}
