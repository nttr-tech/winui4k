package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * WinUI 3's CalendarDatePicker (a Control subclass).
 * A picker that selects a single date from a calendar drop-down.
 *
 * The native side clamps [date] into the range [minDate] to [maxDate] (CalendarDatePicker::OnDateChanged). Setting a date out of range does not
 * throw; it becomes the nearer end date. The same applies before it is shown on screen. Changing [minDate] / [maxDate] later
 * does not re-clamp an already selected [date].
 */
class WCalendarDatePicker : WControl(
    Activation.composeDefault(XamlInterop.CLS_CalendarDatePicker, XamlInterop.IID_ICalendarDatePickerFactory),
) {
    private val dateChangedTokens = ListenerTokens<Consumer<LocalDate?>>()

    /** The ChangeListeners. A single native handler (DateChanged) is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addChangeHandler() }

    /** The [date] last notified to the ChangeListeners (or when they were added). Compared from both the native notification and the setter so each change is notified only once. */
    private var notifiedDate: LocalDate? = null

    /** Whether [addChangeHandler] has been done (until it has, [notifiedDate] is not used). */
    private var changeHandlerAdded = false

    /** The PopupMenuListeners (in the order added). The native handlers (Opened / Closed) are registered only once, when the first listener is added. */
    private val popupMenuListeners = ArrayList<WPopupMenuListener>()

    private var popupHandlersAdded = false

    /** [date] when the drop-down was opened. Compared on close; if unchanged, it is reported as a cancellation. */
    private var dateWhenOpened: LocalDate? = null

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
            notifyIfDateChanged()
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

    /** The date display format (CalendarDatePicker.DateFormat). A DateTimeFormatter pattern such as "{month.full} {day.integer}". Empty means the short date format. */
    var dateFormat: String
        get() = inspectable.getString(XamlInterop.ICalendarDatePicker_get_DateFormat)
        set(value) = Hstring.use(value) { inspectable.call(XamlInterop.ICalendarDatePicker_put_DateFormat, it) }

    /** The first selectable date (CalendarDatePicker.MinDate). See the class description for out-of-range [date]. */
    var minDate: LocalDate
        get() = DateTimeConversions.ticksToLocalDate(DateTimeConversions.readTicks(inspectable, XamlInterop.ICalendarDatePicker_get_MinDate))
        set(value) = inspectable.call(XamlInterop.ICalendarDatePicker_put_MinDate, DateTimeConversions.localDateToTicks(value))

    /** The last selectable date (CalendarDatePicker.MaxDate). See the class description for out-of-range [date]. */
    var maxDate: LocalDate
        get() = DateTimeConversions.ticksToLocalDate(DateTimeConversions.readTicks(inspectable, XamlInterop.ICalendarDatePicker_get_MaxDate))
        set(value) = inspectable.call(XamlInterop.ICalendarDatePicker_put_MaxDate, DateTimeConversions.localDateToTicks(value))

    /** Whether the calendar highlights today (CalendarDatePicker.IsTodayHighlighted). */
    var isTodayHighlighted: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarDatePicker_get_IsTodayHighlighted)
        set(value) = inspectable.putBool(XamlInterop.ICalendarDatePicker_put_IsTodayHighlighted, value)

    /** The first day of the week in the calendar (CalendarDatePicker.FirstDayOfWeek). */
    var firstDayOfWeek: DayOfWeek
        get() = DateTimeConversions.nativeDayOfWeekToJava(inspectable.getInt(XamlInterop.ICalendarDatePicker_get_FirstDayOfWeek))
        set(value) = inspectable.call(XamlInterop.ICalendarDatePicker_put_FirstDayOfWeek, DateTimeConversions.javaDayOfWeekToNative(value))

    /** The display mode when the calendar is opened (CalendarDatePicker.DisplayMode). */
    var displayMode: CalendarViewDisplayMode
        get() = CalendarViewDisplayMode.of(inspectable.getInt(XamlInterop.ICalendarDatePicker_get_DisplayMode))
        set(value) = inspectable.call(XamlInterop.ICalendarDatePicker_put_DisplayMode, value.native)

    /** The calendar identifier (CalendarDatePicker.CalendarIdentifier), such as "GregorianCalendar". */
    var calendarIdentifier: String
        get() = inspectable.getString(XamlInterop.ICalendarDatePicker_get_CalendarIdentifier)
        set(value) = Hstring.use(value) { inspectable.call(XamlInterop.ICalendarDatePicker_put_CalendarIdentifier, it) }

    /** Whether the calendar shows group labels (month and year headings) (CalendarDatePicker.IsGroupLabelVisible). */
    var isGroupLabelVisible: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarDatePicker_get_IsGroupLabelVisible)
        set(value) = inspectable.putBool(XamlInterop.ICalendarDatePicker_put_IsGroupLabelVisible, value)

    /** Whether the calendar shows dates outside the displayed month distinctly (CalendarDatePicker.IsOutOfScopeEnabled). */
    var isOutOfScopeEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ICalendarDatePicker_get_IsOutOfScopeEnabled)
        set(value) = inspectable.putBool(XamlInterop.ICalendarDatePicker_put_IsOutOfScopeEnabled, value)

    /**
     * JComboBox-like: whether the calendar drop-down is open (same as [isCalendarOpen]).
     * From Java, `isPopupVisible()` / `setPopupVisible(boolean)`. Setting true before it is shown on screen opens it once shown.
     */
    var isPopupVisible: Boolean
        get() = isCalendarOpen
        set(value) {
            isCalendarOpen = value
        }

    /** JComboBox-like: opens the calendar drop-down (sets [isCalendarOpen] to true). */
    fun showPopup() {
        isCalendarOpen = true
    }

    /** JComboBox-like: closes the calendar drop-down (sets [isCalendarOpen] to false). */
    fun hidePopup() {
        isCalendarOpen = false
    }

    /**
     * PopupMenuListener-like: receives the opening and closing of the calendar drop-down (corresponds to JComboBox.addPopupMenuListener).
     * The event source is this CalendarDatePicker.
     * - [WPopupMenuListener.popupMenuWillBecomeVisible]: when the drop-down opens (CalendarDatePicker.Opened).
     *   WinUI reports it after opening, so unlike Swing it is not "just before opening".
     * - [WPopupMenuListener.popupMenuCanceled]: when it closes without [date] changing while open (Esc, a click outside,
     *   [hidePopup], etc. Selecting a date makes WinUI close it, which is not a cancellation). popupMenuWillBecomeInvisible is also called next.
     * - [WPopupMenuListener.popupMenuWillBecomeInvisible]: when the drop-down closes (CalendarDatePicker.Closed). Called after closing.
     *
     * All of them are called only when the native Flyout opens or closes, and not if it is not shown on screen. Called in the order added.
     */
    fun addPopupMenuListener(listener: WPopupMenuListener) {
        if (!popupHandlersAdded) {
            addPopupHandlers()
            popupHandlersAdded = true
        }
        popupMenuListeners += listener
        listenerAnchor.listenerAdded()
    }

    /** Removes one listener added with [addPopupMenuListener]. Does nothing if it was not added. */
    fun removePopupMenuListener(listener: WPopupMenuListener) {
        val index = popupMenuListeners.indexOfLast { it === listener }
        if (index < 0) return
        popupMenuListeners.removeAt(index)
        listenerAnchor.listenerRemoved()
    }

    /** The PopupMenuListeners currently added (in the order added). */
    fun getPopupMenuListeners(): Array<WPopupMenuListener> = popupMenuListeners.toTypedArray()

    private fun addPopupHandlers() {
        // The handler stays on the native side, so do not strongly capture the CalendarDatePicker; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.CalendarDatePickerOpenedHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.ICalendarDatePicker_add_Opened,
        ) { _, _ -> (anchor.get() as? WCalendarDatePicker)?.onPopupOpened() }
        inspectable.addEventHandler(
            "WinUI4K.CalendarDatePickerClosedHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.ICalendarDatePicker_add_Closed,
        ) { _, _ -> (anchor.get() as? WCalendarDatePicker)?.onPopupClosed() }
    }

    private fun onPopupOpened() {
        dateWhenOpened = date
        val event = WPopupMenuEvent(this)
        for (listener in popupMenuListeners.toList()) listener.popupMenuWillBecomeVisible(event)
    }

    private fun onPopupClosed() {
        val canceled = date == dateWhenOpened
        val event = WPopupMenuEvent(this)
        val snapshot = popupMenuListeners.toList()
        if (canceled) snapshot.forEach { it.popupMenuCanceled(event) }
        snapshot.forEach { it.popupMenuWillBecomeInvisible(event) }
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this CalendarDatePicker) every time [date] changes. Corresponds to the ChangeListener of Swing's date
     * JSpinner (SpinnerDateModel). Called once per change of the value, whether by choosing in the calendar, setting [date], or clearing to null.
     * When it is called, [date] returns the new value.
     * Setting the same date again does not call it (it is compared with the value last notified and not notified if the same). Also called for changes before it is placed on screen.
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
        notifiedDate = date
        changeHandlerAdded = true
        // The handler stays on the native side, so do not strongly capture the CalendarDatePicker; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.CalendarDatePickerDateChangedHandler",
            XamlInterop.IID_CalendarDatePickerDateChangedHandler,
            XamlInterop.ICalendarDatePicker_add_DateChanged,
        ) { _, _ -> (anchor.get() as? WCalendarDatePicker)?.notifyIfDateChanged() }
    }

    /** If ChangeListeners are added, compares the current [date] with the value last notified and notifies if it changed. */
    private fun notifyIfDateChanged() {
        if (!changeHandlerAdded) return
        val current = date
        val old = notifiedDate
        notifiedDate = current
        changeListeners.fireIfChanged(old, current)
    }

    /** Registers a listener for when the selected date changes (CalendarDatePicker.DateChanged). */
    @JvmSynthetic
    fun addDateChangedListener(listener: (LocalDate?) -> Unit) {
        val adapter = Consumer<LocalDate?> { listener(it) }
        addDateChangedListener(adapter)
        dateChangedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addDateChangedListener(listener: Consumer<LocalDate?>) {
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
            listener.accept(newDate)
        }
        dateChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addDateChangedListener]. */
    @JvmSynthetic
    fun removeDateChangedListener(listener: (LocalDate?) -> Unit) {
        val adapter = dateChangedTokens.removeKotlinAdapter(listener) ?: return
        removeDateChangedListener(adapter)
    }

    fun removeDateChangedListener(listener: Consumer<LocalDate?>) {
        val token = dateChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ICalendarDatePicker_remove_DateChanged, token)
    }
}
