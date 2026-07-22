package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.time.LocalDate

/** CalendarView's display mode. */
enum class CalendarViewDisplayMode(internal val native: Int) {
    MONTH(0), YEAR(1), DECADE(2);
    companion object {
        fun of(native: Int): CalendarViewDisplayMode = entries.first { it.native == native }
    }
}

/** CalendarView's selection mode. */
enum class CalendarViewSelectionMode(internal val native: Int) {
    NONE(0), SINGLE(1), MULTIPLE(2);
    companion object {
        fun of(native: Int): CalendarViewSelectionMode = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's CalendarView (a Control subclass).
 * A control that shows a calendar at all times and lets you select dates.
 */
class WCalendarView : WControl(
    Activation.composeDefault(XamlInterop.CLS_CalendarView, XamlInterop.IID_ICalendarViewFactory),
) {
    private val selectedDatesChangedTokens = ListenerTokens<() -> Unit>()

    /** The selection mode (CalendarView.SelectionMode). */
    var selectionMode: CalendarViewSelectionMode
        get() = CalendarViewSelectionMode.of(inspectable.getInt(XamlInterop.ICalendarView_get_SelectionMode))
        set(value) = inspectable.call(XamlInterop.ICalendarView_put_SelectionMode, value.native)

    /** The display mode (CalendarView.DisplayMode). */
    var displayMode: CalendarViewDisplayMode
        get() = CalendarViewDisplayMode.of(inspectable.getInt(XamlInterop.ICalendarView_get_DisplayMode))
        set(value) = inspectable.call(XamlInterop.ICalendarView_put_DisplayMode, value.native)

    /** Whether group labels are shown (CalendarView.IsGroupLabelVisible). */
    var isGroupLabelVisible: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarView_get_IsGroupLabelVisible)
        set(value) = inspectable.putBool(XamlInterop.ICalendarView_put_IsGroupLabelVisible, value)

    /** Whether out-of-scope dates are shown (CalendarView.IsOutOfScopeEnabled). */
    var isOutOfScopeEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarView_get_IsOutOfScopeEnabled)
        set(value) = inspectable.putBool(XamlInterop.ICalendarView_put_IsOutOfScopeEnabled, value)

    /** Whether today's date is highlighted (CalendarView.IsTodayHighlighted). */
    var isTodayHighlighted: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarView_get_IsTodayHighlighted)
        set(value) = inspectable.putBool(XamlInterop.ICalendarView_put_IsTodayHighlighted, value)

    /** The calendar identifier (CalendarView.CalendarIdentifier). E.g. "GregorianCalendar". */
    var calendarIdentifier: String
        get() = inspectable.getString(XamlInterop.ICalendarView_get_CalendarIdentifier)
        set(value) = Hstring.use(value) { inspectable.call(XamlInterop.ICalendarView_put_CalendarIdentifier, it) }

    /** The first day of the week (CalendarView.FirstDayOfWeek). */
    var firstDayOfWeek: java.time.DayOfWeek
        get() {
            val native = inspectable.getInt(XamlInterop.ICalendarView_get_FirstDayOfWeek)
            return DateTimeConversions.nativeDayOfWeekToJava(native)
        }
        set(value) = inspectable.call(XamlInterop.ICalendarView_put_FirstDayOfWeek, DateTimeConversions.javaDayOfWeekToNative(value))

    /** The number of weeks shown in one view (CalendarView.NumberOfWeeksInView). */
    var numberOfWeeksInView: Int
        get() = inspectable.getInt(XamlInterop.ICalendarView_get_NumberOfWeeksInView)
        set(value) = inspectable.call(XamlInterop.ICalendarView_put_NumberOfWeeksInView, value)

    /** Scrolls the given date into view (CalendarView.SetDisplayDate). */
    fun setDisplayDate(date: LocalDate) {
        inspectable.call(XamlInterop.ICalendarView_SetDisplayDate, DateTimeConversions.localDateToTicks(date))
    }

    /** Registers a listener for when the selected dates change (CalendarView.SelectedDatesChanged). */
    fun addSelectedDatesChangedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.CalendarViewSelectedDatesChangedHandler",
            XamlInterop.IID_CalendarViewSelectedDatesChangedHandler,
            XamlInterop.ICalendarView_add_SelectedDatesChanged,
        ) { _, _ -> listener() }
        selectedDatesChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectedDatesChangedListener]. */
    fun removeSelectedDatesChangedListener(listener: () -> Unit) {
        val token = selectedDatesChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ICalendarView_remove_SelectedDatesChanged, token)
    }
}
