package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.LocalDate

/** Tests WCalendarView's ChangeListener. Selections are made by changing the native SelectedDates (IVector<DateTime>) directly. */
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
    }
}
