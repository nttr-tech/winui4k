package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi
import java.time.LocalTime

/**
 * WinUI 3's TimePicker (a Control subclass).
 * A picker that selects a time via hour / minute spinners.
 */
class WTimePicker : WControl(
    Activation.composeDefault(Abi.CLS_TimePicker, Abi.IID_ITimePickerFactory),
) {
    private val selectedTimeChangedTokens = ListenerTokens<(LocalTime?) -> Unit>()

    /** The heading text (TimePicker.Header). */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(Abi.ITimePicker_get_Header) ?: return ""
            val text = PropertyValues.unboxString(boxed) ?: ""
            boxed.release()
            return text
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.ITimePicker_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The clock format (TimePicker.ClockIdentifier). "12HourClock" or "24HourClock". */
    var clockIdentifier: String
        get() = inspectable.getString(Abi.ITimePicker_get_ClockIdentifier)
        set(value) = Hstring.use(value) { inspectable.call(Abi.ITimePicker_put_ClockIdentifier, it) }

    /** The minute increment (TimePicker.MinuteIncrement). E.g. 15 yields 0, 15, 30, 45. */
    var minuteIncrement: Int
        get() = inspectable.getInt(Abi.ITimePicker_get_MinuteIncrement)
        set(value) = inspectable.call(Abi.ITimePicker_put_MinuteIncrement, value)

    /** The current time (TimePicker.Time, never null). TimeSpan is 100ns ticks. */
    var time: LocalTime
        get() {
            val ticks = Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                inspectable.call(Abi.ITimePicker_get_Time, out)
                Ffi.backend.memory.getLong(out, 0)
            }
            return DateTimeConversions.ticksToLocalTime(ticks)
        }
        set(value) = inspectable.call(Abi.ITimePicker_put_Time, DateTimeConversions.localTimeToTicks(value))

    /** The selected time (TimePicker.SelectedTime, null = nothing selected). */
    var selectedTime: LocalTime?
        get() {
            val boxed = inspectable.getPtrOrNull(Abi.ITimePicker_get_SelectedTime) ?: return null
            val ticks = PropertyValues.unboxTimeSpan(boxed)
            boxed.release()
            return ticks?.let { DateTimeConversions.ticksToLocalTime(it) }
        }
        set(value) {
            if (value == null) {
                inspectable.call(Abi.ITimePicker_put_SelectedTime, null)
            } else {
                val boxed = PropertyValues.boxTimeSpan(DateTimeConversions.localTimeToTicks(value))
                inspectable.call(Abi.ITimePicker_put_SelectedTime, boxed.ptr)
                boxed.release()
            }
        }

    /** Registers a listener for when the selected time changes (TimePicker.SelectedTimeChanged). */
    fun addSelectedTimeChangedListener(listener: (LocalTime?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TimePickerSelectedTimeChangedHandler",
            Abi.IID_TimePickerSelectedTimeChangedHandler,
            Abi.ITimePicker_add_SelectedTimeChanged,
        ) { _, args ->
            val eventArgs = ComPtr(args)
            val boxed = eventArgs.getPtrOrNull(Abi.ITimePickerSelectedValueChangedEventArgs_get_NewTime)
            val newTime = boxed?.let {
                val ticks = PropertyValues.unboxTimeSpan(it)
                it.release()
                ticks?.let { t -> DateTimeConversions.ticksToLocalTime(t) }
            }
            listener(newTime)
        }
        selectedTimeChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectedTimeChangedListener]. */
    fun removeSelectedTimeChangedListener(listener: (LocalTime?) -> Unit) {
        val token = selectedTimeChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.ITimePicker_remove_SelectedTimeChanged, token)
    }
}
