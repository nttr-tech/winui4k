package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.time.LocalDate
import java.util.TimeZone

/**
 * Tests that the date pickers return the set calendar date as is even when the JVM's default time zone is far from UTC (even if it differs
 * from the OS time zone). The default time zone is changed only during the test and restored afterwards.
 */
@Tags("E2E")
class DatePickerTimeZoneTest : FunSpec() {
    init {
        // UTC+14, UTC-11, UTC-3:30 (a 30-minute offset). Zones where midnight converted to UTC falls on the previous or next day
        listOf("Pacific/Kiritimati", "Pacific/Pago_Pago", "America/St_Johns").forEach { zone ->
            test("even with the default time zone $zone, the four pickers return the same calendar date as the set date") {
                val original = TimeZone.getDefault()
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                try {
                    val dates = onUiThreadGet {
                        val leapDay = LocalDate.of(2024, 2, 29)
                        val calendarDatePicker = WCalendarDatePicker().also { it.date = leapDay }
                        val bounded = WCalendarDatePicker().also {
                            it.minDate = LocalDate.of(2024, 3, 1)
                            it.date = LocalDate.of(2024, 2, 1)
                        }
                        val calendarView = WCalendarView().also {
                            it.selectionMode = CalendarViewSelectionMode.MULTIPLE
                            it.addSelectedDate(leapDay)
                            it.addSelectedDate(LocalDate.of(2024, 12, 31))
                        }
                        val datePicker = WDatePicker().also { it.date = leapDay }
                        listOf(calendarDatePicker.date, bounded.date, bounded.minDate, datePicker.date) + calendarView.selectedDates
                    }
                    dates shouldContainExactly listOf(
                        LocalDate.of(2024, 2, 29),
                        LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 2, 29),
                        LocalDate.of(2024, 2, 29),
                        LocalDate.of(2024, 12, 31),
                    )
                } finally {
                    TimeZone.setDefault(original)
                }
            }
        }
    }
}
