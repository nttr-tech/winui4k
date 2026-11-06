package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the public members of the date and time components that had no tests (WDatePicker's month and year visibility and day format,
 * attaching and detaching typed listeners, and WCalendarDatePicker.addDateChangedListener).
 * Defaults are the defaults in the WinUI sources (c_strDayFormat in DependencyProperty.cpp, DatePickerIntegrationTests).
 */
@Tags("E2E")
class WDateTimeCoverageTest : FunSpec() {
    init {
        context("Defaults") {
            test("WDatePicker's monthVisible and yearVisible are true and dayFormat is \"day\"") {
                onUiThreadGet {
                    val picker = WDatePicker()
                    Triple(picker.monthVisible, picker.yearVisible, picker.dayFormat)
                } shouldBe Triple(true, true, "day")
            }
        }

        context("Reading and writing") {
            test("WDatePicker's monthVisible, yearVisible and dayFormat read back the set values") {
                onUiThreadGet {
                    val picker = WDatePicker()
                    picker.monthVisible = false
                    picker.yearVisible = false
                    picker.dayFormat = "{day.integer(2)}"
                    Triple(picker.monthVisible, picker.yearVisible, picker.dayFormat)
                } shouldBe Triple(false, false, "{day.integer(2)}")
            }
        }

        context("Events") {
            test("a listener detached with WDatePicker's removeSelectedDateChangedListener is not called on the next selection change") {
                val received = mutableListOf<LocalDate?>()
                val listener: (LocalDate?) -> Unit = { received += it }
                val picker = onUiThreadGet { WDatePicker().also { it.addSelectedDateChangedListener(listener) } }
                UiTestHarness.attachAndAwaitLoaded(picker)
                try {
                    onUiThread { picker.selectedDate = LocalDate.of(2030, 5, 17) }
                    eventually(3.seconds) { onUiThreadGet { received.toList() } shouldContainExactly listOf(LocalDate.of(2030, 5, 17)) }
                    onUiThread {
                        picker.removeSelectedDateChangedListener(listener)
                        picker.selectedDate = LocalDate.of(2031, 6, 18)
                    }
                    onUiThreadGet { picker.selectedDate to received.toList() } shouldBe
                        (LocalDate.of(2031, 6, 18) to listOf(LocalDate.of(2030, 5, 17)))
                } finally {
                    UiTestHarness.detach(picker)
                }
            }

            test("a listener detached with WTimePicker's removeSelectedTimeChangedListener is not called on the next selection change") {
                onUiThreadGet {
                    val picker = WTimePicker()
                    val received = mutableListOf<LocalTime?>()
                    val listener: (LocalTime?) -> Unit = { received += it }
                    picker.addSelectedTimeChangedListener(listener)
                    picker.selectedTime = LocalTime.of(9, 30)
                    picker.removeSelectedTimeChangedListener(listener)
                    picker.selectedTime = LocalTime.of(10, 45)
                    received.toList()
                } shouldContainExactly listOf<LocalTime?>(LocalTime.of(9, 30))
            }

            test("WCalendarDatePicker's addDateChangedListener receives the new date (null when cleared) on every date change even before being on screen, and is not called after removal") {
                onUiThreadGet {
                    val picker = WCalendarDatePicker()
                    val received = mutableListOf<LocalDate?>()
                    val listener: (LocalDate?) -> Unit = { received += it }
                    picker.addDateChangedListener(listener)
                    picker.date = LocalDate.of(2026, 10, 20)
                    picker.date = null
                    picker.removeDateChangedListener(listener)
                    picker.date = LocalDate.of(2027, 1, 1)
                    received.toList()
                } shouldContainExactly listOf(LocalDate.of(2026, 10, 20), null)
            }
        }

        context("Boundaries and exceptions") {
            test("a WCalendarDatePicker date before minDate is clamped to minDate, and addDateChangedListener receives the clamped date only once") {
                onUiThreadGet {
                    val picker = WCalendarDatePicker()
                    picker.minDate = LocalDate.of(2020, 1, 1)
                    val received = mutableListOf<LocalDate?>()
                    picker.addDateChangedListener { received += it }
                    picker.date = LocalDate.of(2019, 6, 1)
                    picker.date to received.toList()
                } shouldBe (LocalDate.of(2020, 1, 1) to listOf(LocalDate.of(2020, 1, 1)))
            }
        }
    }
}
