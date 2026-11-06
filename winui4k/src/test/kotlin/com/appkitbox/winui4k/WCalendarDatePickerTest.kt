package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/** Tests for WCalendarDatePicker's properties, clamping to min / max, ChangeListener and PopupMenuListener. */
@Tags("E2E")
class WCalendarDatePickerTest : FunSpec() {
    init {
        test("ChangeListener is called only once per change of date even before being placed on screen, and not for setting the same date again") {
            val (seen, sources, picker) = onUiThreadGet {
                val datePicker = WCalendarDatePicker()
                val seen = mutableListOf<LocalDate?>()
                val sources = mutableListOf<Any>()
                datePicker.addChangeListener { e ->
                    sources += e.source
                    seen += (e.source as WCalendarDatePicker).date
                }
                datePicker.date = LocalDate.of(2026, 10, 20)
                datePicker.date = LocalDate.of(2026, 10, 20)
                datePicker.date = LocalDate.of(2024, 2, 29)
                datePicker.date = null
                datePicker.date = null
                Triple(seen, sources, datePicker)
            }
            seen shouldContainExactly listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2024, 2, 29), null)
            sources.forEach { it shouldBeSameInstanceAs picker }
        }

        test("even on a CalendarDatePicker placed on screen, one setting of date calls ChangeListener only once (not overlapping with the native notification)") {
            val seen = mutableListOf<LocalDate?>()
            val datePicker = onUiThreadGet {
                WCalendarDatePicker().also { it.addChangeListener { e -> seen += (e.source as WCalendarDatePicker).date } }
            }
            UiTestHarness.attachAndAwaitLoaded(datePicker)
            try {
                onUiThread {
                    datePicker.date = LocalDate.of(2030, 1, 31)
                    datePicker.date = LocalDate.of(2030, 1, 31)
                }
                seen shouldContainExactly listOf(LocalDate.of(2030, 1, 31))
            } finally {
                UiTestHarness.detach(datePicker)
            }
        }

        test("by default no date is selected and the calendar is closed") {
            val (date, open) = onUiThreadGet {
                val picker = WCalendarDatePicker()
                picker.date to picker.isPopupVisible
            }
            date shouldBe null
            open shouldBe false
        }

        test("the display format, range and calendar settings can be read back from native as set") {
            val values = onUiThreadGet {
                val picker = WCalendarDatePicker()
                picker.dateFormat = "{year.full}/{month.integer(2)}/{day.integer(2)}"
                picker.minDate = LocalDate.of(2000, 1, 1)
                picker.maxDate = LocalDate.of(2030, 12, 31)
                picker.isTodayHighlighted = false
                picker.firstDayOfWeek = DayOfWeek.MONDAY
                picker.displayMode = CalendarViewDisplayMode.YEAR
                picker.calendarIdentifier = "JapaneseCalendar"
                picker.isGroupLabelVisible = true
                picker.isOutOfScopeEnabled = false
                picker.header = "Due date"
                listOf(
                    picker.dateFormat, picker.minDate, picker.maxDate, picker.isTodayHighlighted, picker.firstDayOfWeek,
                    picker.displayMode, picker.calendarIdentifier, picker.isGroupLabelVisible, picker.isOutOfScopeEnabled, picker.header,
                )
            }
            values shouldContainExactly listOf(
                "{year.full}/{month.integer(2)}/{day.integer(2)}", LocalDate.of(2000, 1, 1), LocalDate.of(2030, 12, 31), false, DayOfWeek.MONDAY,
                CalendarViewDisplayMode.YEAR, "JapaneseCalendar", true, false, "Due date",
            )
        }

        test("a date before minDate or after maxDate is clamped to the nearer end, and the ChangeListener is called once with the clamped date") {
            val (dates, seen) = onUiThreadGet {
                val picker = WCalendarDatePicker()
                picker.minDate = LocalDate.of(2026, 1, 1)
                picker.maxDate = LocalDate.of(2026, 12, 31)
                val seen = mutableListOf<LocalDate?>()
                picker.addChangeListener { e -> seen += (e.source as WCalendarDatePicker).date }
                val dates = mutableListOf<LocalDate?>()
                picker.date = LocalDate.of(2025, 6, 1)
                dates += picker.date
                picker.date = LocalDate.of(2027, 3, 3)
                dates += picker.date
                picker.date = LocalDate.of(2026, 1, 1)
                dates += picker.date
                picker.date = LocalDate.of(2026, 6, 15)
                dates += picker.date
                dates to seen
            }
            dates shouldContainExactly listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 15))
            seen shouldContainExactly listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 15))
        }

        test("narrowing minDate / maxDate later does not re-clamp the selected date") {
            val date = onUiThreadGet {
                val picker = WCalendarDatePicker()
                picker.date = LocalDate.of(2026, 6, 15)
                picker.minDate = LocalDate.of(2026, 7, 1)
                picker.maxDate = LocalDate.of(2026, 12, 31)
                picker.date
            }
            date shouldBe LocalDate.of(2026, 6, 15)
        }

        test("opening with showPopup calls popupMenuWillBecomeVisible, and closing with hidePopup without changing the date calls popupMenuCanceled and popupMenuWillBecomeInvisible") {
            val events = mutableListOf<String>()
            val sources = mutableListOf<Any>()
            val picker = onUiThreadGet {
                WCalendarDatePicker().also {
                    it.addPopupMenuListener(object : WPopupMenuListener {
                        override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                            sources += e.source
                            events += "visible:${it.isPopupVisible}"
                        }

                        override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                            sources += e.source
                            events += "invisible:${it.isPopupVisible}"
                        }

                        override fun popupMenuCanceled(e: WPopupMenuEvent) {
                            sources += e.source
                            events += "canceled"
                        }
                    })
                }
            }
            UiTestHarness.attachAndAwaitLoaded(picker)
            try {
                onUiThread { picker.showPopup() }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible:true") }
                onUiThread { picker.hidePopup() }
                eventually(5.seconds) {
                    onUiThreadGet { events.toList() } shouldContainExactly listOf("visible:true", "canceled", "invisible:false")
                }
                onUiThreadGet { sources.toList() }.forEach { it shouldBeSameInstanceAs picker }
            } finally {
                UiTestHarness.detach(picker)
            }
        }

        test("popupMenuCanceled is not called when the date changed while open before closing") {
            val events = mutableListOf<String>()
            val picker = onUiThreadGet {
                WCalendarDatePicker().also {
                    it.addPopupMenuListener(object : WPopupMenuAdapter() {
                        override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                            events += "visible"
                        }

                        override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                            events += "invisible"
                        }

                        override fun popupMenuCanceled(e: WPopupMenuEvent) {
                            events += "canceled"
                        }
                    })
                }
            }
            UiTestHarness.attachAndAwaitLoaded(picker)
            try {
                onUiThread { picker.isPopupVisible = true }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible") }
                onUiThread {
                    picker.date = LocalDate.of(2026, 3, 14)
                    picker.isPopupVisible = false
                }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible", "invisible") }
            } finally {
                UiTestHarness.detach(picker)
            }
        }

        test("a listener removed with removePopupMenuListener disappears from getPopupMenuListeners (removing a listener never added does nothing)") {
            val (before, after) = onUiThreadGet {
                val picker = WCalendarDatePicker()
                val first = object : WPopupMenuAdapter() {}
                val second = object : WPopupMenuAdapter() {}
                picker.addPopupMenuListener(first)
                picker.addPopupMenuListener(second)
                val before = picker.getPopupMenuListeners().toList() == listOf(first, second)
                picker.removePopupMenuListener(first)
                picker.removePopupMenuListener(object : WPopupMenuAdapter() {})
                before to picker.getPopupMenuListeners().toList()
            }
            before shouldBe true
            after.size shouldBe 1
        }

        test("a listener removed with removeChangeListener is not called") {
            val (calls, remaining) = onUiThreadGet {
                val datePicker = WCalendarDatePicker()
                val calls = mutableListOf<String>()
                val listener = WChangeListener { calls += "called" }
                datePicker.addChangeListener(listener)
                datePicker.date = LocalDate.of(2026, 1, 1)
                datePicker.removeChangeListener(listener)
                datePicker.date = LocalDate.of(2026, 1, 2)
                calls to datePicker.getChangeListeners().size
            }
            calls shouldContainExactly listOf("called")
            remaining shouldBe 0
        }
    }
}
