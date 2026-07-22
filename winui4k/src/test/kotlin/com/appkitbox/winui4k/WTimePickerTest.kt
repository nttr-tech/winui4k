package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalTime

/** Tests verifying WTimePicker's time (LocalTime <-> WinRT TimeSpan conversion) and listeners. */
class WTimePickerTest : FunSpec() {
    init {
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
    }
}
