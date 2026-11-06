package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.time.LocalDate
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/** CalendarView's display mode. */
enum class CalendarViewDisplayMode(internal val native: Int) {
    MONTH(0),
    YEAR(1),
    DECADE(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): CalendarViewDisplayMode = entries.first { it.native == native }
    }
}

/** CalendarView's selection mode. */
enum class CalendarViewSelectionMode(internal val native: Int) {
    NONE(0),
    SINGLE(1),
    MULTIPLE(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): CalendarViewSelectionMode = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's CalendarView (a Control subclass).
 * A control that shows a calendar at all times and lets you select dates.
 *
 * The selection is read with [selectedDates] / [selectedDate] and changed by setting [selectedDate] or with [addSelectedDate] / [removeSelectedDate] /
 * [clearSelection] (nothing can be selected if [selectionMode] is [CalendarViewSelectionMode.NONE], and up to one day if SINGLE).
 */
class WCalendarView : WControl(
    Activation.composeDefault(XamlInterop.CLS_CalendarView, XamlInterop.IID_ICalendarViewFactory),
) {
    private val selectedDatesChangedTokens = ListenerTokens<Runnable>()

    /** The ChangeListeners. A single native handler (SelectedDatesChanged) is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addChangeHandler() }

    /** The [addSelectedDatesChangedListener] (WCalendarViewSelectionListener) listeners (in the order added). */
    private val selectionListeners = ArrayList<WCalendarViewSelectionListener>()

    private var selectionHandlerAdded = false

    /** While greater than 0, native SelectedDatesChanged is not reported to ChangeListeners (several operations are reported as one change). */
    private var selectionEditDepth = 0

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

    /** The first date shown (CalendarView.MinDate). */
    var minDate: LocalDate
        get() = DateTimeConversions.ticksToLocalDate(DateTimeConversions.readTicks(inspectable, XamlInterop.ICalendarView_get_MinDate))
        set(value) = inspectable.call(XamlInterop.ICalendarView_put_MinDate, DateTimeConversions.localDateToTicks(value))

    /** The last date shown (CalendarView.MaxDate). */
    var maxDate: LocalDate
        get() = DateTimeConversions.ticksToLocalDate(DateTimeConversions.readTicks(inspectable, XamlInterop.ICalendarView_get_MaxDate))
        set(value) = inspectable.call(XamlInterop.ICalendarView_put_MaxDate, DateTimeConversions.localDateToTicks(value))

    /**
     * A snapshot of the selected dates (CalendarView.SelectedDates) at that point (read-only; in the order selected).
     * To change them, set [selectedDate] or use [addSelectedDate] / [removeSelectedDate] / [clearSelection].
     */
    val selectedDates: List<LocalDate>
        get() = withSelectedDates { readDates(it, FoundationInterop.IVector_get_Size, FoundationInterop.IVector_GetAt) }

    /**
     * JList.getSelectedValue-like: the first selected date (null if unselected).
     * Setting it replaces the selection with that single date, and null clears the selection. ChangeListeners are called once per replacement
     * (not when the same single day is set again). [addSelectedDatesChangedListener] listeners are called for each native notification, so
     * replacing a selection of two or more days results in two calls: "all removed" and "one day added".
     * Setting a non-null value when [selectionMode] is [CalendarViewSelectionMode.NONE] throws IllegalStateException.
     */
    var selectedDate: LocalDate?
        get() = selectedDates.firstOrNull()
        set(value) {
            if (value == null) {
                clearSelection()
                return
            }
            val old = selectedDates
            if (old == listOf(value)) return
            checkSelectable()
            val ticks = DateTimeConversions.localDateToTicks(value)
            editSelection(old) { vector ->
                if (old.size == 1) {
                    vector.call(FoundationInterop.IVector_SetAt, 0, ticks)
                } else {
                    vector.call(FoundationInterop.IVector_Clear)
                    vector.call(FoundationInterop.IVector_Append, ticks)
                }
            }
        }

    /**
     * JList.addSelectionInterval-like: adds [date] to the selection. Does nothing if it is already selected.
     * As in WinUI, when [selectionMode] is [CalendarViewSelectionMode.NONE], or SINGLE with another date selected, it cannot be
     * added and throws IllegalStateException (WinUI returns E_FAIL: CalendarView::OnSelectedDatesChanging).
     * To reselect in SINGLE, set [selectedDate].
     */
    fun addSelectedDate(date: LocalDate) {
        val old = selectedDates
        if (date in old) return
        checkSelectable()
        check(selectionMode != CalendarViewSelectionMode.SINGLE || old.isEmpty()) {
            "No more dates can be selected in a SINGLE CalendarView (set selectedDate to reselect)"
        }
        withSelectedDates { it.call(FoundationInterop.IVector_Append, DateTimeConversions.localDateToTicks(date)) }
    }

    /** JList.removeSelectionInterval-like: removes [date] from the selection. Does nothing if it is not selected. */
    fun removeSelectedDate(date: LocalDate) {
        val index = selectedDates.indexOf(date)
        if (index < 0) return
        withSelectedDates { it.call(FoundationInterop.IVector_RemoveAt, index) }
    }

    /** JList.clearSelection-like: clears the whole selection. Does nothing if nothing is selected (listeners are not called either). */
    fun clearSelection() {
        if (selectedDates.isEmpty()) return
        withSelectedDates { it.call(FoundationInterop.IVector_Clear) }
    }

    private fun checkSelectable() {
        check(selectionMode != CalendarViewSelectionMode.NONE) { "Dates cannot be selected in a CalendarView whose selectionMode is NONE" }
    }

    /** Gets SelectedDates (IVector<DateTime>), passes it to [block] and releases it. */
    private fun <T> withSelectedDates(block: (ComPtr) -> T): T {
        val vector = inspectable.getPtr(XamlInterop.ICalendarView_get_SelectedDates)
        try {
            return block(vector)
        } finally {
            vector.release()
        }
    }

    /** However many native notifications occur, reports to ChangeListeners only once if it changed from [old]. */
    private fun editSelection(old: List<LocalDate>, block: (ComPtr) -> Unit) {
        selectionEditDepth++
        try {
            withSelectedDates(block)
        } finally {
            selectionEditDepth--
        }
        changeListeners.fireIfChanged(old, selectedDates)
    }

    /** Scrolls the given date into view (CalendarView.SetDisplayDate). */
    fun setDisplayDate(date: LocalDate) {
        inspectable.call(XamlInterop.ICalendarView_SetDisplayDate, DateTimeConversions.localDateToTicks(date))
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this CalendarView) every time the set of selected dates (CalendarView.SelectedDates)
     * changes. Called once per change, whether by the user's selection, clearing the selection, or changing the selection from code.
     * WinUI sends SelectedDatesChanged only when the set gains or loses dates (CalendarView::RaiseSelectionChangedEventIfChanged), so
     * that is passed on as is. Also called for changes before it is placed on screen.
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
        // The handler stays on the native side, so do not strongly capture the CalendarView; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.CalendarViewSelectedDatesChangedHandler",
            XamlInterop.IID_CalendarViewSelectedDatesChangedHandler,
            XamlInterop.ICalendarView_add_SelectedDatesChanged,
        ) { _, _ -> (anchor.get() as? WCalendarView)?.onNativeSelectionChanged() }
    }

    private fun onNativeSelectionChanged() {
        if (selectionEditDepth == 0) changeListeners.fire()
    }

    /**
     * ListSelectionListener-like: every time the selected dates increase or decrease, receives a [WCalendarViewSelectionEvent] with the added and removed dates
     * (the source is this CalendarView). Called once per native SelectedDatesChanged, in the order added.
     * From Java it can be written as `e -> e.getAddedDates()` (a no-arg `() -> ...` resolves to the existing Runnable version).
     */
    fun addSelectedDatesChangedListener(listener: WCalendarViewSelectionListener) {
        if (!selectionHandlerAdded) {
            addSelectionHandler()
            selectionHandlerAdded = true
        }
        selectionListeners += listener
        listenerAnchor.listenerAdded()
    }

    /** Removes one [WCalendarViewSelectionListener] added with [addSelectedDatesChangedListener]. Does nothing if it was not added. */
    fun removeSelectedDatesChangedListener(listener: WCalendarViewSelectionListener) {
        val index = selectionListeners.indexOfLast { it === listener }
        if (index < 0) return
        selectionListeners.removeAt(index)
        listenerAnchor.listenerRemoved()
    }

    private fun addSelectionHandler() {
        // The handler stays on the native side, so do not strongly capture the CalendarView; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.CalendarViewSelectedDatesChangedHandler",
            XamlInterop.IID_CalendarViewSelectedDatesChangedHandler,
            XamlInterop.ICalendarView_add_SelectedDatesChanged,
        ) { _, args -> (anchor.get() as? WCalendarView)?.onSelectionEvent(ComPtr(args)) }
    }

    private fun onSelectionEvent(args: ComPtr) { // args is borrowed (not Released)
        if (selectionListeners.isEmpty()) return
        val event = WCalendarViewSelectionEvent(
            this,
            readEventDates(args, XamlInterop.ICalendarViewSelectedDatesChangedEventArgs_get_AddedDates),
            readEventDates(args, XamlInterop.ICalendarViewSelectedDatesChangedEventArgs_get_RemovedDates),
        )
        for (listener in selectionListeners.toList()) listener.selectedDatesChanged(event)
    }

    private fun readEventDates(args: ComPtr, slot: Int): List<LocalDate> {
        val view = args.getPtrOrNull(slot) ?: return emptyList()
        try {
            return readDates(view, FoundationInterop.IVectorView_get_Size, FoundationInterop.IVectorView_GetAt)
        } finally {
            view.release()
        }
    }

    /** Converts the elements of an IVector<DateTime> / IVectorView<DateTime> to dates (the caller releases it). */
    private fun readDates(collection: ComPtr, sizeSlot: Int, getAtSlot: Int): List<LocalDate> {
        val size = collection.getInt(sizeSlot)
        return List(size) { DateTimeConversions.ticksToLocalDate(DateTimeConversions.readTicks(collection, getAtSlot, it)) }
    }

    /** Registers a listener for when the selected dates change (CalendarView.SelectedDatesChanged). */
    @JvmSynthetic
    fun addSelectedDatesChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addSelectedDatesChangedListenerForJava(adapter)
        selectedDatesChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addSelectedDatesChangedListener")
    fun addSelectedDatesChangedListenerForJava(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.CalendarViewSelectedDatesChangedHandler",
            XamlInterop.IID_CalendarViewSelectedDatesChangedHandler,
            XamlInterop.ICalendarView_add_SelectedDatesChanged,
        ) { _, _ -> listener.run() }
        selectedDatesChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectedDatesChangedListener]. */
    @JvmSynthetic
    fun removeSelectedDatesChangedListener(listener: () -> Unit) {
        val adapter = selectedDatesChangedTokens.removeKotlinAdapter(listener) ?: return
        removeSelectedDatesChangedListenerForJava(adapter)
    }

    @JvmName("removeSelectedDatesChangedListener")
    fun removeSelectedDatesChangedListenerForJava(listener: Runnable) {
        val token = selectedDatesChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ICalendarView_remove_SelectedDatesChanged, token)
    }
}
