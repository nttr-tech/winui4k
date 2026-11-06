package com.appkitbox.winui4k

import java.time.LocalDate
import java.util.EventListener
import java.util.EventObject

/**
 * ListSelectionEvent-like: an event that tells a [WCalendarViewSelectionListener] that the dates selected in a [WCalendarView] increased or decreased
 * (CalendarViewSelectedDatesChangedEventArgs of CalendarView.SelectedDatesChanged).
 * Swing's ListSelectionEvent has the range of changed rows, but this has the added dates ([addedDates]) and removed dates ([removedDates]).
 * The source is that WCalendarView.
 */
class WCalendarViewSelectionEvent(
    source: Any,
    /** The dates added to the selection (in the order the native side reported them). From Java, `getAddedDates()`. */
    val addedDates: List<LocalDate>,
    /** The dates removed from the selection (in the order the native side reported them). From Java, `getRemovedDates()`. */
    val removedDates: List<LocalDate>,
) : EventObject(source) {
    override fun toString(): String = "${javaClass.name}[source=$source,addedDates=$addedDates,removedDates=$removedDates]"
}

/**
 * ListSelectionListener-like: a listener that receives increases and decreases of the selected dates of a [WCalendarView] (Swing has no counterpart of CalendarView).
 * From Java it can be written as `e -> e.getAddedDates()` or as an anonymous class.
 */
fun interface WCalendarViewSelectionListener : EventListener {
    /** Called when the selected dates increase or decrease. */
    fun selectedDatesChanged(e: WCalendarViewSelectionEvent)
}
