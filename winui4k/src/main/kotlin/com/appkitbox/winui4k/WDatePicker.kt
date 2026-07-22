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
import java.time.LocalDate

/**
 * WinUI 3's DatePicker (a Control subclass).
 * A picker that selects a date via day / month / year spinners.
 */
class WDatePicker : WControl(
    Activation.composeDefault(Abi.CLS_DatePicker, Abi.IID_IDatePickerFactory),
) {
    private val selectedDateChangedTokens = ListenerTokens<(LocalDate?) -> Unit>()

    /** The heading text (DatePicker.Header). */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(Abi.IDatePicker_get_Header) ?: return ""
            val text = PropertyValues.unboxString(boxed) ?: ""
            boxed.release()
            return text
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IDatePicker_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The current date (DatePicker.Date, never null). DateTime is 100ns ticks from 1601-01-01 UTC. */
    var date: LocalDate
        get() {
            val ticks = Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                inspectable.call(Abi.IDatePicker_get_Date, out)
                Ffi.backend.memory.getLong(out, 0)
            }
            return DateTimeConversions.ticksToLocalDate(ticks)
        }
        set(value) = inspectable.call(Abi.IDatePicker_put_Date, DateTimeConversions.localDateToTicks(value))

    /** Whether the day spinner is shown (DatePicker.DayVisible). */
    var dayVisible: Boolean
        get() = inspectable.getBool(Abi.IDatePicker_get_DayVisible)
        set(value) = inspectable.putBool(Abi.IDatePicker_put_DayVisible, value)

    /** Whether the month spinner is shown (DatePicker.MonthVisible). */
    var monthVisible: Boolean
        get() = inspectable.getBool(Abi.IDatePicker_get_MonthVisible)
        set(value) = inspectable.putBool(Abi.IDatePicker_put_MonthVisible, value)

    /** Whether the year spinner is shown (DatePicker.YearVisible). */
    var yearVisible: Boolean
        get() = inspectable.getBool(Abi.IDatePicker_get_YearVisible)
        set(value) = inspectable.putBool(Abi.IDatePicker_put_YearVisible, value)

    /** The day format string (DatePicker.DayFormat). E.g. "{day.integer} ({dayofweek.abbreviated})". */
    var dayFormat: String
        get() = inspectable.getString(Abi.IDatePicker_get_DayFormat)
        set(value) = Hstring.use(value) { inspectable.call(Abi.IDatePicker_put_DayFormat, it) }

    /** The earliest selectable year (DatePicker.MinYear). */
    var minYear: LocalDate
        get() {
            val ticks = Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                inspectable.call(Abi.IDatePicker_get_MinYear, out)
                Ffi.backend.memory.getLong(out, 0)
            }
            return DateTimeConversions.ticksToLocalDate(ticks)
        }
        set(value) = inspectable.call(Abi.IDatePicker_put_MinYear, DateTimeConversions.localDateToTicks(value))

    /** The latest selectable year (DatePicker.MaxYear). */
    var maxYear: LocalDate
        get() {
            val ticks = Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                inspectable.call(Abi.IDatePicker_get_MaxYear, out)
                Ffi.backend.memory.getLong(out, 0)
            }
            return DateTimeConversions.ticksToLocalDate(ticks)
        }
        set(value) = inspectable.call(Abi.IDatePicker_put_MaxYear, DateTimeConversions.localDateToTicks(value))

    /** The picker's orientation (DatePicker.Orientation). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(Abi.IDatePicker_get_Orientation))
        set(value) = inspectable.call(Abi.IDatePicker_put_Orientation, value.native)

    /** Registers a listener for when the selected date changes (DatePicker.SelectedDateChanged). */
    fun addSelectedDateChangedListener(listener: (LocalDate?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.DatePickerSelectedDateChangedHandler",
            Abi.IID_DatePickerSelectedDateChangedHandler,
            Abi.IDatePicker_add_SelectedDateChanged,
        ) { _, args ->
            val eventArgs = ComPtr(args)
            val boxed = eventArgs.getPtrOrNull(Abi.IDatePickerSelectedValueChangedEventArgs_get_NewDate)
            val newDate = boxed?.let {
                val ticks = PropertyValues.unboxDateTime(it)
                it.release()
                ticks?.let { t -> DateTimeConversions.ticksToLocalDate(t) }
            }
            listener(newDate)
        }
        selectedDateChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectedDateChangedListener]. */
    fun removeSelectedDateChangedListener(listener: (LocalDate?) -> Unit) {
        val token = selectedDateChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IDatePicker_remove_SelectedDateChanged, token)
    }
}
