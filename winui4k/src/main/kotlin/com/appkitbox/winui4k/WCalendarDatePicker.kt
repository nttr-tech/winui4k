package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.time.LocalDate

/**
 * WinUI 3's CalendarDatePicker (a Control subclass).
 * A picker that selects a single date from a calendar drop-down.
 */
class WCalendarDatePicker : WControl(
    Activation.composeDefault(XamlInterop.CLS_CalendarDatePicker, XamlInterop.IID_ICalendarDatePickerFactory),
) {
    private val dateChangedTokens = ListenerTokens<(LocalDate?) -> Unit>()

    /** The selected date (null = nothing selected). CalendarDatePicker.Date (IReference<DateTime>). */
    var date: LocalDate?
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ICalendarDatePicker_get_Date) ?: return null
            val ticks = PropertyValues.unboxDateTime(boxed)
            boxed.release()
            return ticks?.let { DateTimeConversions.ticksToLocalDate(it) }
        }
        set(value) {
            if (value == null) {
                inspectable.call(XamlInterop.ICalendarDatePicker_put_Date, null)
            } else {
                val boxed = PropertyValues.boxDateTime(DateTimeConversions.localDateToTicks(value))
                inspectable.call(XamlInterop.ICalendarDatePicker_put_Date, boxed.ptr)
                boxed.release()
            }
        }

    /** Whether the calendar drop-down is open (CalendarDatePicker.IsCalendarOpen). */
    var isCalendarOpen: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarDatePicker_get_IsCalendarOpen)
        set(value) = inspectable.putBool(XamlInterop.ICalendarDatePicker_put_IsCalendarOpen, value)

    /** The text shown when no date is selected (CalendarDatePicker.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.ICalendarDatePicker_get_PlaceholderText)
        set(value) = Hstring.use(value) { inspectable.call(XamlInterop.ICalendarDatePicker_put_PlaceholderText, it) }

    /** The heading text (CalendarDatePicker.Header). */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ICalendarDatePicker_get_Header) ?: return ""
            val text = PropertyValues.unboxString(boxed) ?: ""
            boxed.release()
            return text
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ICalendarDatePicker_put_Header, boxed.ptr)
            boxed.release()
        }

    /** Registers a listener for when the selected date changes (CalendarDatePicker.DateChanged). */
    fun addDateChangedListener(listener: (LocalDate?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.CalendarDatePickerDateChangedHandler",
            XamlInterop.IID_CalendarDatePickerDateChangedHandler,
            XamlInterop.ICalendarDatePicker_add_DateChanged,
        ) { _, args ->
            val eventArgs = ComPtr(args)
            val boxed = eventArgs.getPtrOrNull(XamlInterop.ICalendarDatePickerDateChangedEventArgs_get_NewDate)
            val newDate = boxed?.let {
                val ticks = PropertyValues.unboxDateTime(it)
                it.release()
                ticks?.let { t -> DateTimeConversions.ticksToLocalDate(t) }
            }
            listener(newDate)
        }
        dateChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addDateChangedListener]. */
    fun removeDateChangedListener(listener: (LocalDate?) -> Unit) {
        val token = dateChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ICalendarDatePicker_remove_DateChanged, token)
    }
}
