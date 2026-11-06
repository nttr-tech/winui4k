package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Tests the values, next/previous values and notifications of the spinner models (WSpinnerNumberModel / WSpinnerListModel / WSpinnerDateModel)
 * (no window is used). The expected values were worked out by hand by following the same operations on Swing's SpinnerNumberModel and others.
 * The shape seen from Java is in WSpinnerModelForJavaTest.
 */
@Tags("Pure")
class WSpinnerModelTest : FunSpec() {
    /** Adds a listener that counts how many times it is called. */
    private fun WSpinnerModel.countChanges(): IntArray {
        val count = IntArray(1)
        addChangeListener { count[0]++ }
        return count
    }

    init {
        test("the no-argument default of WSpinnerNumberModel is the same as Swing: Integer 0, no range, step 1") {
            val model = WSpinnerNumberModel()
            model.value shouldBe 0
            model.value.shouldBeInstanceOf<Int>()
            model.minimum.shouldBeNull()
            model.maximum.shouldBeNull()
            model.stepSize shouldBe 1
            model.nextValue shouldBe 1
            model.previousValue shouldBe -1
        }

        test("an Integer model returns Integer next/previous values too, and a Double model returns Double") {
            val ints = WSpinnerNumberModel(5, 0, 10, 2)
            ints.nextValue shouldBe 7
            ints.nextValue.shouldBeInstanceOf<Int>()
            ints.previousValue shouldBe 3
            val doubles = WSpinnerNumberModel(0.5, 0.0, 1.0, 0.25)
            doubles.nextValue shouldBe 0.75
            doubles.previousValue shouldBe 0.25
            WSpinnerNumberModel(5L, null, null, 3).nextValue shouldBe 8L
        }

        test("next/previous values beyond the range are null, and exactly at the edge the value is returned") {
            val model = WSpinnerNumberModel(9, 0, 10, 1)
            model.nextValue shouldBe 10
            model.value = 10
            model.nextValue.shouldBeNull()
            model.value = 1
            model.previousValue shouldBe 0
            model.value = 0
            model.previousValue.shouldBeNull()
            WSpinnerNumberModel(9, 0, 10, 2).nextValue.shouldBeNull() // 11 > 10. Not snapped to the edge 10
        }

        test("setValue does not clamp to the range (same as Swing)") {
            val model = WSpinnerNumberModel(5, 0, 10, 1)
            model.value = 150
            model.value shouldBe 150
            model.nextValue.shouldBeNull()
            model.previousValue.shouldBeNull() // 149 is also beyond 10
        }

        test("setValue with null or a non-numeric value throws IllegalArgumentException and the value does not change") {
            val model = WSpinnerNumberModel(5, 0, 10, 1)
            shouldThrow<IllegalArgumentException> { model.value = null }
            shouldThrow<IllegalArgumentException> { model.value = "6" }
            model.value shouldBe 5
        }

        test("a constructor value outside the range throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> { WSpinnerNumberModel(11, 0, 10, 1) }
            shouldThrow<IllegalArgumentException> { WSpinnerNumberModel(-1, 0, 10, 1) }
            WSpinnerNumberModel(10, 0, 10, 1).value shouldBe 10
            WSpinnerNumberModel(5.0, null, 10, 1).value shouldBe 5.0 // A maximum of a different type is also compared as a number
        }

        test("notifications are sent once each only when the value, minimum, maximum or step changes") {
            val model = WSpinnerNumberModel(5, 0, 10, 1)
            val count = model.countChanges()
            model.value = 5
            count[0] shouldBe 0
            model.value = 6
            model.minimum = 0
            model.minimum = 1
            model.maximum = 10
            model.maximum = null
            model.stepSize = 1
            model.stepSize = 2
            count[0] shouldBe 4
            model.value = 6.0 // Integer 6 and Double 6.0 differ by equals, so it notifies (same as Swing)
            count[0] shouldBe 5
            model.value.shouldBeInstanceOf<Double>()
        }

        test("Integer values wrap around as int on overflow, as in Swing") {
            WSpinnerNumberModel(Int.MAX_VALUE, null, null, 1).nextValue shouldBe Int.MIN_VALUE
        }

        test("a fractional step is added to integer values as a long (0.5 becomes 0)") {
            WSpinnerNumberModel(5, null, null, 0.5).nextValue shouldBe 5
        }

        test("listeners are called starting from the most recently added one with the model as the source; added twice they are called twice, and remove removes one") {
            val model = WSpinnerNumberModel()
            val calls = mutableListOf<String>()
            val first = WChangeListener { e ->
                e.source shouldBeSameInstanceAs model
                calls += "first"
            }
            model.addChangeListener(first)
            model.addChangeListener { calls += "second" }
            model.addChangeListener(first)
            model.value = 1
            calls shouldContainExactly listOf("first", "second", "first")
            calls.clear()
            model.removeChangeListener(first)
            model.value = 2
            calls shouldContainExactly listOf("second", "first")
            model.getChangeListeners().size shouldBe 2
        }

        test("a listener removed during a notification still receives that event") {
            val model = WSpinnerNumberModel()
            val calls = mutableListOf<String>()
            val late = WChangeListener { calls += "late" }
            model.addChangeListener(late)
            model.addChangeListener {
                calls += "remover"
                model.removeChangeListener(late)
            }
            model.value = 1
            calls shouldContainExactly listOf("remover", "late")
            calls.clear()
            model.value = 2
            calls shouldContainExactly listOf("remover")
        }

        test("WSpinnerListModel starts at the first element, and the next/previous values are null at the ends") {
            val model = WSpinnerListModel(listOf("Spring", "Summer", "Autumn", "Winter"))
            model.value shouldBe "Spring"
            model.previousValue.shouldBeNull()
            model.nextValue shouldBe "Summer"
            model.value = "Winter"
            model.previousValue shouldBe "Autumn"
            model.nextValue.shouldBeNull()
        }

        test("WSpinnerListModel throws IllegalArgumentException for a value not in the list or an empty list") {
            val model = WSpinnerListModel(arrayOf("a", "b"))
            shouldThrow<IllegalArgumentException> { model.value = "c" }
            model.value shouldBe "a"
            shouldThrow<IllegalArgumentException> { WSpinnerListModel(emptyList<String>()) }
            shouldThrow<IllegalArgumentException> { WSpinnerListModel(emptyArray<String>()) }
            shouldThrow<IllegalArgumentException> { model.list = emptyList<String>() }
            WSpinnerListModel().value shouldBe "empty"
        }

        test("WSpinnerListModel does not notify for setValue of the same element or setList with the same contents, and setList goes back to the first element") {
            val model = WSpinnerListModel(listOf("a", "b", "c"))
            val count = model.countChanges()
            model.value = "a"
            model.list = listOf("a", "b", "c")
            count[0] shouldBe 0
            model.value = "c"
            model.list = listOf("x", "y")
            count[0] shouldBe 2
            model.value shouldBe "x"
        }

        test("WSpinnerDateModel shifts by one unit and returns null when going beyond the range") {
            val start = LocalDateTime.of(2024, 1, 1, 0, 0)
            val end = LocalDateTime.of(2024, 3, 31, 0, 0)
            val model = WSpinnerDateModel(LocalDateTime.of(2024, 1, 31, 9, 30), start, end, ChronoUnit.MONTHS)
            model.nextValue shouldBe LocalDateTime.of(2024, 2, 29, 9, 30) // Rounded to the end of February in a leap year (same as Calendar.add)
            model.previousValue.shouldBeNull() // 2023-12-31 is before the start
            model.value = LocalDateTime.of(2024, 3, 1, 0, 0)
            model.nextValue.shouldBeNull() // 2024-04-01 is after the end
            model.calendarField = ChronoUnit.HOURS
            model.nextValue shouldBe LocalDateTime.of(2024, 3, 1, 1, 0)
            model.date shouldBe LocalDateTime.of(2024, 3, 1, 0, 0)
        }

        test("WSpinnerDateModel throws IllegalArgumentException for values outside the range, FOREVER, and values that are not date-times") {
            val day = LocalDateTime.of(2024, 6, 1, 0, 0)
            shouldThrow<IllegalArgumentException> { WSpinnerDateModel(day, day.plusDays(1), null, ChronoUnit.DAYS) }
            shouldThrow<IllegalArgumentException> { WSpinnerDateModel(day, null, day.minusDays(1), ChronoUnit.DAYS) }
            shouldThrow<IllegalArgumentException> { WSpinnerDateModel(day, null, null, ChronoUnit.FOREVER) }
            val model = WSpinnerDateModel(day, null, null, ChronoUnit.DAYS)
            shouldThrow<IllegalArgumentException> { model.value = "2024-06-02" }
            shouldThrow<IllegalArgumentException> { model.value = null }
            shouldThrow<IllegalArgumentException> { model.calendarField = ChronoUnit.FOREVER }
            model.value = day.plusYears(5) // Not clamped when there is no range
            model.value shouldBe day.plusYears(5)
        }

        test("WSpinnerDateModel makes the next/previous of date-times that LocalDateTime cannot represent null") {
            WSpinnerDateModel(LocalDateTime.MAX, null, null, ChronoUnit.DAYS).nextValue.shouldBeNull()
        }

        test("WSpinnerDateModel notifies only when the value, start, end or unit changes") {
            val day = LocalDateTime.of(2024, 6, 1, 0, 0)
            val model = WSpinnerDateModel(day, null, null, ChronoUnit.DAYS)
            val count = model.countChanges()
            model.value = day
            model.start = null
            model.calendarField = ChronoUnit.DAYS
            count[0] shouldBe 0
            model.value = day.plusDays(1)
            model.start = day
            model.end = day.plusDays(10)
            model.calendarField = ChronoUnit.WEEKS
            count[0] shouldBe 4
        }
    }
}
