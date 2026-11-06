package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.LocalTime

/** Tests verifying WTimePicker's time (LocalTime <-> WinRT TimeSpan conversion) and listeners. */
@Tags("E2E")
class WTimePickerTest : FunSpec() {
    init {
        test("an unselected time returns null without throwing (TimePicker's default Time is the sentinel value -1 for unselected)") {
            val (time, selected) = onUiThreadGet { WTimePicker().let { it.time to it.selectedTime } }
            time.shouldBeNull()
            selected.shouldBeNull()
        }

        test("setting time to null goes back to unselected, and both time and selectedTime return null") {
            val (before, after) = onUiThreadGet {
                val timePicker = WTimePicker()
                timePicker.time = LocalTime.of(23, 59)
                val before = timePicker.time to timePicker.selectedTime
                timePicker.time = null
                before to (timePicker.time to timePicker.selectedTime)
            }
            before shouldBe (LocalTime.of(23, 59) to LocalTime.of(23, 59))
            after shouldBe (null to null)
        }

        test("time is truncated to the minuteIncrement step, and midnight 00:00 is distinguished from unselected") {
            val (snapped, midnight) = onUiThreadGet {
                val timePicker = WTimePicker()
                timePicker.minuteIncrement = 15
                timePicker.time = LocalTime.of(10, 29)
                val snapped = timePicker.time
                timePicker.time = LocalTime.MIDNIGHT
                snapped to timePicker.time
            }
            snapped shouldBe LocalTime.of(10, 15)
            midnight shouldBe LocalTime.MIDNIGHT
        }

        test("time returns exactly the time that was set") {
            onUiThreadGet {
                val timePicker = WTimePicker()
                timePicker.time = LocalTime.of(13, 45)
                timePicker.time
            } shouldBe LocalTime.of(13, 45)
        }

        test("selectedTime starts out null (unselected) and returns exactly the time that was set") {
            val (initial, selected) = onUiThreadGet {
                val timePicker = WTimePicker()
                val initial = timePicker.selectedTime
                timePicker.selectedTime = LocalTime.of(9, 30)
                initial to timePicker.selectedTime
            }
            initial.shouldBeNull()
            selected shouldBe LocalTime.of(9, 30)
        }

        test("changing selectedTime reaches the SelectedTimeChanged listener with the new time") {
            val received = mutableListOf<LocalTime?>()
            onUiThread {
                val timePicker = WTimePicker()
                timePicker.addSelectedTimeChangedListener { received.add(it) }
                timePicker.selectedTime = LocalTime.of(18, 0)
            }
            received shouldContainExactly listOf<LocalTime?>(LocalTime.of(18, 0))
        }

        test("clockIdentifier and minuteIncrement return exactly the values that were set") {
            val (clock, increment) = onUiThreadGet {
                val timePicker = WTimePicker()
                timePicker.clockIdentifier = "24HourClock"
                timePicker.minuteIncrement = 15
                timePicker.clockIdentifier to timePicker.minuteIncrement
            }
            clock shouldBe "24HourClock"
            increment shouldBe 15
        }

        test("ChangeListener is called only once per change of time, and once when pulled to the minuteIncrement step") {
            val (seen, sources, picker) = onUiThreadGet {
                val timePicker = WTimePicker()
                val seen = mutableListOf<LocalTime?>()
                val sources = mutableListOf<Any>()
                timePicker.addChangeListener { e ->
                    sources += e.source
                    seen += (e.source as WTimePicker).time
                }
                timePicker.time = LocalTime.of(10, 20)
                timePicker.time = LocalTime.of(10, 20)
                timePicker.minuteIncrement = 15
                timePicker.minuteIncrement = 5
                timePicker.selectedTime = LocalTime.of(11, 0)
                timePicker.time = null
                timePicker.selectedTime = null
                Triple(seen, sources, timePicker)
            }
            seen shouldContainExactly listOf(LocalTime.of(10, 20), LocalTime.of(10, 15), LocalTime.of(11, 0), null)
            sources.forEach { it shouldBeSameInstanceAs picker }
        }

        test("even on a TimePicker placed on screen, one setting of time calls ChangeListener only once (not overlapping with the native notification)") {
            val seen = mutableListOf<LocalTime?>()
            val timePicker = onUiThreadGet { WTimePicker().also { it.addChangeListener { e -> seen += (e.source as WTimePicker).time } } }
            UiTestHarness.attachAndAwaitLoaded(timePicker)
            try {
                onUiThread {
                    timePicker.time = LocalTime.of(8, 30)
                    timePicker.time = LocalTime.of(8, 30)
                    timePicker.time = null
                }
                seen shouldContainExactly listOf(LocalTime.of(8, 30), null)
            } finally {
                UiTestHarness.detach(timePicker)
            }
        }

        test("a listener removed with removeChangeListener is not called, and getChangeListeners returns the listeners added") {
            val (calls, remaining) = onUiThreadGet {
                val timePicker = WTimePicker()
                val calls = mutableListOf<String>()
                val listener = WChangeListener { calls += "called" }
                timePicker.addChangeListener(listener)
                timePicker.time = LocalTime.of(1, 0)
                timePicker.removeChangeListener(listener)
                timePicker.time = LocalTime.of(2, 0)
                calls to timePicker.getChangeListeners().size
            }
            calls shouldContainExactly listOf("called")
            remaining shouldBe 0
        }
    }
}
