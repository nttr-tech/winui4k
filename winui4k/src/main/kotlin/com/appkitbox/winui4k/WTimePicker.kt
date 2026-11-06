package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.time.LocalTime
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * WinUI 3's TimePicker (a Control subclass).
 * A picker that selects a time via hour / minute spinners.
 */
class WTimePicker : WControl(
    Activation.composeDefault(XamlInterop.CLS_TimePicker, XamlInterop.IID_ITimePickerFactory),
) {
    /** Its own IsTabStop is false, and UIElement.Focus passes focus to the template's button (FlyoutButton) ([isFocusable] / [requestFocusInWindow]). */
    override val focusIsDelegated: Boolean get() = true

    private val selectedTimeChangedTokens = ListenerTokens<Consumer<LocalTime?>>()

    /** The ChangeListeners. A single native handler (SelectedTimeChanged) is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addChangeHandler() }

    /** The [time] last notified to the ChangeListeners (or when they were added). Compared from both the native notification and the setter so each change is notified only once. */
    private var notifiedTime: LocalTime? = null

    /** Whether [addChangeHandler] has been done (until it has, [notifiedTime] is not used). */
    private var changeHandlerAdded = false

    /** The heading text (TimePicker.Header). */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ITimePicker_get_Header) ?: return ""
            val text = PropertyValues.unboxString(boxed) ?: ""
            boxed.release()
            return text
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ITimePicker_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The clock format (TimePicker.ClockIdentifier). "12HourClock" or "24HourClock". */
    var clockIdentifier: String
        get() = inspectable.getString(XamlInterop.ITimePicker_get_ClockIdentifier)
        set(value) = Hstring.use(value) { inspectable.call(XamlInterop.ITimePicker_put_ClockIdentifier, it) }

    /** The minute increment (TimePicker.MinuteIncrement). E.g. 15 yields 0, 15, 30, 45. */
    var minuteIncrement: Int
        get() = inspectable.getInt(XamlInterop.ITimePicker_get_MinuteIncrement)
        set(value) {
            inspectable.call(XamlInterop.ITimePicker_put_MinuteIncrement, value)
            notifyIfTimeChanged()
        }

    /**
     * The current time (TimePicker.Time). Null means unselected (WinUI represents unselected with Time's sentinel value -1 (TimeSpan), which is mapped to null).
     * Setting null goes back to unselected (sets SelectedTime to null). WinUI keeps Time and [selectedTime] in sync, so this always returns the same value as [selectedTime].
     * A time that is set is truncated to the [minuteIncrement] step (WinUI's behavior).
     */
    var time: LocalTime?
        get() {
            val ticks = Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                inspectable.call(XamlInterop.ITimePicker_get_Time, out)
                Ffi.backend.memory.getLong(out, 0)
            }
            return if (ticks == NULL_TIME_TICKS) null else DateTimeConversions.ticksToLocalTime(ticks)
        }
        set(value) {
            if (value == null) {
                inspectable.call(XamlInterop.ITimePicker_put_SelectedTime, null)
            } else {
                inspectable.call(XamlInterop.ITimePicker_put_Time, DateTimeConversions.localTimeToTicks(value))
            }
            notifyIfTimeChanged()
        }

    /** The selected time (TimePicker.SelectedTime, null = nothing selected). */
    var selectedTime: LocalTime?
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ITimePicker_get_SelectedTime) ?: return null
            val ticks = PropertyValues.unboxTimeSpan(boxed)
            boxed.release()
            return ticks?.let { DateTimeConversions.ticksToLocalTime(it) }
        }
        set(value) {
            if (value == null) {
                inspectable.call(XamlInterop.ITimePicker_put_SelectedTime, null)
            } else {
                val boxed = PropertyValues.boxTimeSpan(DateTimeConversions.localTimeToTicks(value))
                inspectable.call(XamlInterop.ITimePicker_put_SelectedTime, boxed.ptr)
                boxed.release()
            }
            notifyIfTimeChanged()
        }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this TimePicker) every time [time] changes. Corresponds to the ChangeListener of Swing's time
     * JSpinner (SpinnerDateModel). Called once per change of the value, whether by the user's choice, setting [time] / [selectedTime], clearing to null,
     * or being pulled to the [minuteIncrement] step. When it is called, [time] returns the new value.
     * Setting the same time again does not call it. Also called for changes before it is placed on screen.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order they were added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    private fun addChangeHandler() {
        notifiedTime = time
        changeHandlerAdded = true
        // The handler stays on the native side, so do not strongly capture the TimePicker; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.TimePickerSelectedTimeChangedHandler",
            XamlInterop.IID_TimePickerSelectedTimeChangedHandler,
            XamlInterop.ITimePicker_add_SelectedTimeChanged,
        ) { _, _ -> (anchor.get() as? WTimePicker)?.notifyIfTimeChanged() }
    }

    /** If ChangeListeners are added, compares the current [time] with the value last notified and notifies if it changed. */
    private fun notifyIfTimeChanged() {
        if (!changeHandlerAdded) return
        val current = time
        val old = notifiedTime
        notifiedTime = current
        changeListeners.fireIfChanged(old, current)
    }

    /** Registers a listener for when the selected time changes (TimePicker.SelectedTimeChanged). */
    @JvmSynthetic
    fun addSelectedTimeChangedListener(listener: (LocalTime?) -> Unit) {
        val adapter = Consumer<LocalTime?> { listener(it) }
        addSelectedTimeChangedListenerForJava(adapter)
        selectedTimeChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addSelectedTimeChangedListener")
    fun addSelectedTimeChangedListenerForJava(listener: Consumer<LocalTime?>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TimePickerSelectedTimeChangedHandler",
            XamlInterop.IID_TimePickerSelectedTimeChangedHandler,
            XamlInterop.ITimePicker_add_SelectedTimeChanged,
        ) { _, args ->
            val eventArgs = ComPtr(args)
            val boxed = eventArgs.getPtrOrNull(XamlInterop.ITimePickerSelectedValueChangedEventArgs_get_NewTime)
            val newTime = boxed?.let {
                val ticks = PropertyValues.unboxTimeSpan(it)
                it.release()
                ticks?.let { t -> DateTimeConversions.ticksToLocalTime(t) }
            }
            listener.accept(newTime)
        }
        selectedTimeChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectedTimeChangedListener]. */
    @JvmSynthetic
    fun removeSelectedTimeChangedListener(listener: (LocalTime?) -> Unit) {
        val adapter = selectedTimeChangedTokens.removeKotlinAdapter(listener) ?: return
        removeSelectedTimeChangedListenerForJava(adapter)
    }

    @JvmName("removeSelectedTimeChangedListener")
    fun removeSelectedTimeChangedListenerForJava(listener: Consumer<LocalTime?>) {
        val token = selectedTimeChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITimePicker_remove_SelectedTimeChanged, token)
    }

    private companion object {
        /** The sentinel value of TimePicker.Time that represents unselected (TimeSpan.Duration = -1). */
        const val NULL_TIME_TICKS = -1L
    }
}
