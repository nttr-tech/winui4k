package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.LocalDate

/** Tests WCalendarDatePicker's ChangeListener. */
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
