package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Tests verifying WDatePicker's date (LocalDate <-> WinRT DateTime conversion) and listeners. */
class WDatePickerTest : FunSpec() {
    init {
        test("date returns exactly the date that was set (including a leap day)") {
            onUiThreadGet {
                val datePicker = WDatePicker()
                datePicker.date = LocalDate.of(2024, 2, 29)
                datePicker.date
            } shouldBe LocalDate.of(2024, 2, 29)
        }

        test("changing date reaches the SelectedDateChanged listener with the new date") {
            // SelectedDateChanged fires via the message loop for a DatePicker on the visual tree
            val received = LinkedBlockingQueue<LocalDate>()
            val datePicker = onUiThreadGet {
                WDatePicker().also { it.addSelectedDateChangedListener { date -> date?.let(received::add) } }
            }
            UiTestHarness.attach(datePicker)
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
