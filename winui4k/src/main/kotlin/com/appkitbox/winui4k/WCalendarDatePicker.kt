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
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * WinUI 3's CalendarDatePicker (a Control subclass).
 * A picker that selects a single date from a calendar drop-down.
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
        addDateChangedListenerForJava(adapter)
        dateChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addDateChangedListener")
    fun addDateChangedListenerForJava(listener: Consumer<LocalDate?>) {
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
        removeDateChangedListenerForJava(adapter)
    }

    @JvmName("removeDateChangedListener")
    fun removeDateChangedListenerForJava(listener: Consumer<LocalDate?>) {
        val token = dateChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ICalendarDatePicker_remove_DateChanged, token)
    }
}
