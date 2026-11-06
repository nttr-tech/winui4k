package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Tests verifying WDatePicker's date (LocalDate <-> WinRT DateTime conversion) and listeners. */
class WDatePickerTest : FunSpec() {
    init {
        test("an unselected date is null (DatePicker's default Date is the sentinel value for unselected, and 1601-01-01 is not returned)") {
            val (date, selected) = onUiThreadGet { WDatePicker().let { it.date to it.selectedDate } }
            date.shouldBeNull()
            selected.shouldBeNull()
        }

        test("setting date to null goes back to unselected, and both date and selectedDate return null") {
            val (before, after) = onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.date = LocalDate.of(2026, 10, 20)
                val before = datePicker.date to datePicker.selectedDate
                datePicker.date = null
                before to (datePicker.date to datePicker.selectedDate)
            }
            before shouldBe (LocalDate.of(2026, 10, 20) to LocalDate.of(2026, 10, 20))
            after shouldBe (null to null)
        }

        test("a leap day set to selectedDate is reflected in date as the same date, and setting null goes back to unselected") {
            val (set, cleared) = onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.selectedDate = LocalDate.of(2028, 2, 29)
                val set = datePicker.selectedDate to datePicker.date
                datePicker.selectedDate = null
                set to (datePicker.selectedDate to datePicker.date)
            }
            set shouldBe (LocalDate.of(2028, 2, 29) to LocalDate.of(2028, 2, 29))
            cleared shouldBe (null to null)
        }

        test("on a DatePicker placed on screen, a date before minYear is pulled to January 1 of minYear and a date after maxYear to December 31 of maxYear") {
            val datePicker = onUiThreadGet {
                WDatePicker().also {
                    it.minYear = LocalDate.of(2000, 6, 15)
                    it.maxYear = LocalDate.of(2010, 6, 15)
                }
            }
            UiTestHarness.attachAndAwaitLoaded(datePicker)
            try {
                val (low, high) = onUiThreadGet {
                    datePicker.date = LocalDate.of(1990, 3, 3)
                    val low = datePicker.date
                    datePicker.date = LocalDate.of(2020, 3, 3)
                    low to datePicker.date
                }
                low shouldBe LocalDate.of(2000, 1, 1)
                high shouldBe LocalDate.of(2010, 12, 31)
            } finally {
                UiTestHarness.detach(datePicker)
            }
        }

        test("date returns exactly the date that was set (including a leap day)") {
            onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.date = LocalDate.of(2024, 2, 29)
                datePicker.date
            } shouldBe LocalDate.of(2024, 2, 29)
        }

        test("changing date reaches the SelectedDateChanged listener with the new date") {
            // DatePicker doesn't fire SelectedDateChanged for a date change made before template
            // application (before Loaded), so add it to the visual tree and wait for Loaded first
            val received = LinkedBlockingQueue<LocalDate>()
            val datePicker = onUiThreadGet {
                WDatePicker().also { it.addSelectedDateChangedListener { date -> date?.let(received::add) } }
            }
            UiTestHarness.attachAndAwaitLoaded(datePicker)
            try {
                onUiThread { datePicker.date = LocalDate.of(2030, 5, 17) }
                received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe LocalDate.of(2030, 5, 17)
            } finally {
                UiTestHarness.detach(datePicker)
            }
        }

        test("minYear and maxYear return exactly the dates that were set") {
            val (min, max) = onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.minYear = LocalDate.of(2000, 1, 1)
                datePicker.maxYear = LocalDate.of(2050, 12, 31)
                datePicker.minYear to datePicker.maxYear
            }
            min shouldBe LocalDate.of(2000, 1, 1)
            max shouldBe LocalDate.of(2050, 12, 31)
        }

        test("dayVisible = false changes whether the day spinner is shown") {
            onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.dayVisible = false
                datePicker.dayVisible
            }.shouldBeFalse()
        }

        test("header returns exactly the string that was set") {
            onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.header = "Start date"
                datePicker.header
            } shouldBe "Start date"
        }
    }
}
