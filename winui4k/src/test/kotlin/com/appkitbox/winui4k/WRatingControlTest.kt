package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/** Tests WRatingControl's value (unset is -1) and ChangeListener (both user input and setting the value; notifications of the same value are dropped). */
@Tags("E2E")
class WRatingControlTest : FunSpec() {
    init {
        test("the default is unset (value -1) with 5 stars; it returns the value set, and setting -1 makes it unset again") {
            val values = onUiThreadGet {
                val rating = WRatingControl()
                val initial = rating.value to rating.maxRating
                rating.value = 3.0
                val set = rating.value
                rating.value = -1.0
                listOf(initial.first, initial.second.toDouble(), set, rating.value)
            }
            values shouldContainExactly listOf(-1.0, 5.0, 3.0, -1.0)
        }

        test("setting value calls the ChangeListener with the control as the source, and setting the same value does not") {
            val (values, sourcesAreRating) = onUiThreadGet {
                val rating = WRatingControl()
                val values = mutableListOf<Double>()
                val sources = mutableListOf<Any>()
                rating.addChangeListener(
                    WChangeListener { e ->
                        sources += e.source
                        values += (e.source as WRatingControl).value
                    },
                )
                rating.value = 3.0
                rating.value = 3.0
                rating.value = -1.0
                rating.value = -1.0
                values to sources.all { it === rating }
            }
            values shouldContainExactly listOf(3.0, -1.0)
            sourcesAreRating.shouldBeTrue()
        }

        test("the Kotlin lambda form receives the new value and is no longer called after removeChangeListener") {
            val received = onUiThreadGet {
                val rating = WRatingControl()
                val received = mutableListOf<Double>()
                val listener: (Double) -> Unit = { received += it }
                rating.addChangeListener(listener)
                rating.value = 2.0
                rating.removeChangeListener(listener)
                rating.value = 4.0
                received
            }
            received shouldContainExactly listOf(2.0)
        }

        test("a listener added twice is called twice, remove removes one at a time, and getChangeListeners is in the order they were added") {
            val (calls, counts) = onUiThreadGet {
                val rating = WRatingControl()
                var calls = 0
                val first = WChangeListener { calls++ }
                val second = WChangeListener { }
                rating.addChangeListener(first)
                rating.addChangeListener(second)
                rating.addChangeListener(first)
                val listed = rating.getChangeListeners().toList()
                check(listed == listOf(first, second, first)) { "not in the order they were added: $listed" }
                rating.value = 1.0
                rating.removeChangeListener(first)
                rating.value = 2.0
                val afterOne = rating.getChangeListeners().size
                rating.removeChangeListener(first)
                rating.value = 3.0
                calls to listOf(afterOne, rating.getChangeListeners().size)
            }
            calls shouldBe 3
            counts shouldContainExactly listOf(2, 1)
        }

        test("it is also called when the rating changes by key, and input that would go beyond the highest star (a native notification without a value change) is dropped") {
            val rating = onUiThreadGet { WRatingControl() }
            val values = mutableListOf<Double>()
            onUiThread {
                rating.addChangeListener(WChangeListener { values += rating.value })
            }
            UiTestHarness.attachAndAwaitLoaded(rating)
            try {
                onUiThread {
                    rating.value = 4.0
                    check(rating.requestFocusInWindow()) { "cannot move the focus" }
                }
                TestKeyboard.keyDown(VK_RIGHT, SCAN_RIGHT, extended = true)
                TestKeyboard.keyUp(VK_RIGHT, SCAN_RIGHT, extended = true)
                eventually(3.seconds) { onUiThreadGet { values.toList() } shouldContainExactly listOf(4.0, 5.0) }
                TestKeyboard.keyDown(VK_RIGHT, SCAN_RIGHT, extended = true) // Stays at the maximum. The native side sends ValueChanged
                TestKeyboard.keyUp(VK_RIGHT, SCAN_RIGHT, extended = true)
                TestKeyboard.keyDown(VK_LEFT, SCAN_LEFT, extended = true)
                TestKeyboard.keyUp(VK_LEFT, SCAN_LEFT, extended = true)
                eventually(3.seconds) { onUiThreadGet { rating.value } shouldBe 4.0 }
                onUiThreadGet { values.toList() } shouldContainExactly listOf(4.0, 5.0, 4.0)
            } finally {
                UiTestHarness.detach(rating)
            }
        }
    }

    private companion object {
        const val VK_LEFT = 0x25
        const val SCAN_LEFT = 0x4B
        const val VK_RIGHT = 0x27
        const val SCAN_RIGHT = 0x4D
    }
}
