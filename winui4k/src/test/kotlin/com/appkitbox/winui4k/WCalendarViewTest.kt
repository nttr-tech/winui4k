package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.LocalDate

/**
 * Tests for WCalendarView's selection API and ChangeListener / WCalendarViewSelectionListener.
 * The first two ChangeListener cases create the selection by changing the native SelectedDates (IVector<DateTime>) directly.
 */
class WCalendarViewTest : FunSpec() {
    /** Applies [block] to CalendarView.SelectedDates (the obtained IVector is released). Call on the UI thread. */
    private fun WCalendarView.editSelectedDates(block: (ComPtr) -> Unit) {
        val vector = inspectable.getPtr(XamlInterop.ICalendarView_get_SelectedDates)
        try {
            block(vector)
        } finally {
            vector.release()
        }
    }

    init {
        test("ChangeListener is called once per change of the set of selected dates even before being placed on screen, and the source is the CalendarView") {
            val (calls, sources, calendar) = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.MULTIPLE
                val calls = mutableListOf<String>()
                val sources = mutableListOf<Any>()
                calendar.addChangeListener { e ->
                    sources += e.source
                    calls += "changed"
                }
                calendar.editSelectedDates { it.call(FoundationInterop.IVector_Append, DateTimeConversions.localDateToTicks(LocalDate.of(2026, 10, 1))) }
                calendar.editSelectedDates { it.call(FoundationInterop.IVector_Append, DateTimeConversions.localDateToTicks(LocalDate.of(2026, 10, 2))) }
                calendar.editSelectedDates { it.call(FoundationInterop.IVector_Clear) }
                Triple(calls, sources, calendar)
            }
            calls shouldContainExactly listOf("changed", "changed", "changed")
            sources.forEach { it shouldBeSameInstanceAs calendar }
        }

        test("a listener removed with removeChangeListener is not called, and getChangeListeners returns the listeners in the order they were added") {
            val (calls, same) = onUiThreadGet {
                val calendar = WCalendarView()
                val calls = mutableListOf<String>()
                val first = WChangeListener { calls += "first" }
                val second = WChangeListener { calls += "second" }
                calendar.addChangeListener(first)
                calendar.addChangeListener(second)
                val same = calendar.getChangeListeners().toList() == listOf(first, second)
                calendar.removeChangeListener(first)
                calendar.editSelectedDates { it.call(FoundationInterop.IVector_Append, DateTimeConversions.localDateToTicks(LocalDate.of(2026, 10, 3))) }
                calls to same
            }
            calls shouldContainExactly listOf("second")
            same shouldBe true
        }

        test("by default nothing is selected, selectedDates is empty and selectedDate is null") {
            val (dates, date) = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectedDates to calendar.selectedDate
            }
            dates.shouldBeEmpty()
            date shouldBe null
        }

        test("in SINGLE, setting selectedDate replaces the selection, and ChangeListeners are called once per replacement and not when the same date is set again") {
            val (states, calls) = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.SINGLE
                var calls = 0
                calendar.addChangeListener { calls++ }
                val states = mutableListOf<List<LocalDate>>()
                calendar.selectedDate = LocalDate.of(2026, 10, 1)
                states += calendar.selectedDates
                calendar.selectedDate = LocalDate.of(2024, 2, 29)
                states += calendar.selectedDates
                calendar.selectedDate = LocalDate.of(2024, 2, 29)
                states += calendar.selectedDates
                calendar.selectedDate = null
                states += calendar.selectedDates
                states to calls
            }
            states shouldContainExactly listOf(listOf(LocalDate.of(2026, 10, 1)), listOf(LocalDate.of(2024, 2, 29)), listOf(LocalDate.of(2024, 2, 29)), emptyList())
            calls shouldBe 3
        }

        test("in MULTIPLE, addSelectedDate adds in the order selected, adding a selected date does not add it again, and removeSelectedDate and clearSelection remove") {
            val (states, calls) = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.MULTIPLE
                var calls = 0
                calendar.addChangeListener { calls++ }
                val states = mutableListOf<List<LocalDate>>()
                calendar.addSelectedDate(LocalDate.of(2026, 10, 5))
                calendar.addSelectedDate(LocalDate.of(2026, 10, 1))
                calendar.addSelectedDate(LocalDate.of(2026, 10, 5))
                states += calendar.selectedDates
                calendar.removeSelectedDate(LocalDate.of(2026, 10, 5))
                calendar.removeSelectedDate(LocalDate.of(2026, 10, 5))
                states += calendar.selectedDates
                calendar.addSelectedDate(LocalDate.of(2026, 10, 9))
                calendar.clearSelection()
                calendar.clearSelection()
                states += calendar.selectedDates
                states to calls
            }
            states shouldContainExactly listOf(
                listOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)),
                listOf(LocalDate.of(2026, 10, 1)),
                emptyList(),
            )
            // 2 additions + 1 removal + 1 addition + 1 clear (operations that change nothing do not call it)
            calls shouldBe 5
        }

        test("in MULTIPLE with two days selected, setting selectedDate makes the selection that one day and calls ChangeListeners only once") {
            val (date, calls) = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.MULTIPLE
                calendar.addSelectedDate(LocalDate.of(2026, 1, 1))
                calendar.addSelectedDate(LocalDate.of(2026, 1, 2))
                var calls = 0
                calendar.addChangeListener { calls++ }
                calendar.selectedDate = LocalDate.of(2026, 1, 3)
                calendar.selectedDates to calls
            }
            date shouldContainExactly listOf(LocalDate.of(2026, 1, 3))
            calls shouldBe 1
        }

        test("in NONE, dates cannot be selected and IllegalStateException is thrown, and addSelectedDate in SINGLE with another date selected also throws IllegalStateException") {
            onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.NONE
                shouldThrow<IllegalStateException> { calendar.addSelectedDate(LocalDate.of(2026, 5, 5)) }
                shouldThrow<IllegalStateException> { calendar.selectedDate = LocalDate.of(2026, 5, 5) }
                calendar.selectedDate = null
                calendar.selectedDates.shouldBeEmpty()

                calendar.selectionMode = CalendarViewSelectionMode.SINGLE
                calendar.addSelectedDate(LocalDate.of(2026, 5, 5))
                calendar.addSelectedDate(LocalDate.of(2026, 5, 5))
                shouldThrow<IllegalStateException> { calendar.addSelectedDate(LocalDate.of(2026, 5, 6)) }
                calendar.selectedDates shouldContainExactly listOf(LocalDate.of(2026, 5, 5))
            }
        }

        test("WCalendarViewSelectionListener receives the added and removed dates, and the source is that CalendarView") {
            val (events, calendar) = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.MULTIPLE
                val events = mutableListOf<Triple<Any, List<LocalDate>, List<LocalDate>>>()
                calendar.addSelectedDatesChangedListener { e -> events += Triple(e.source, e.addedDates, e.removedDates) }
                calendar.addSelectedDate(LocalDate.of(2026, 8, 1))
                calendar.addSelectedDate(LocalDate.of(2026, 8, 2))
                calendar.removeSelectedDate(LocalDate.of(2026, 8, 1))
                events to calendar
            }
            events.map { it.second to it.third } shouldContainExactly listOf(
                listOf(LocalDate.of(2026, 8, 1)) to emptyList(),
                listOf(LocalDate.of(2026, 8, 2)) to emptyList(),
                emptyList<LocalDate>() to listOf(LocalDate.of(2026, 8, 1)),
            )
            events.forEach { it.first shouldBeSameInstanceAs calendar }
        }

        test("a no-arg lambda resolves to the existing () -> Unit version, and a selection listener removed with removeSelectedDatesChangedListener is not called") {
            val calls = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.selectionMode = CalendarViewSelectionMode.MULTIPLE
                val calls = mutableListOf<String>()
                calendar.addSelectedDatesChangedListener { calls += "plain" }
                val listener = WCalendarViewSelectionListener { calls += "event" }
                calendar.addSelectedDatesChangedListener(listener)
                calendar.addSelectedDate(LocalDate.of(2026, 8, 1))
                calendar.removeSelectedDatesChangedListener(listener)
                calendar.addSelectedDate(LocalDate.of(2026, 8, 2))
                calls
            }
            calls shouldContainExactly listOf("plain", "event", "plain")
        }

        test("minDate / maxDate read back the set dates") {
            val range = onUiThreadGet {
                val calendar = WCalendarView()
                calendar.minDate = LocalDate.of(2020, 1, 1)
                calendar.maxDate = LocalDate.of(2029, 12, 31)
                calendar.minDate to calendar.maxDate
            }
            range shouldBe (LocalDate.of(2020, 1, 1) to LocalDate.of(2029, 12, 31))
        }
    }
}
