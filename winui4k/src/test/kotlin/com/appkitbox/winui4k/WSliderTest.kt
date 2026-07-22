package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests verifying WSlider's value/range (RangeBase) and ChangeListener. */
class WSliderTest : FunSpec() {
    init {
        test("the minimum / maximum / value passed to the constructor take effect") {
            val slider = onUiThreadGet { WSlider(minimum = 10.0, maximum = 20.0, value = 15.0) }
            onUiThreadGet { Triple(slider.minimum, slider.maximum, slider.value) } shouldBe
                Triple(10.0, 20.0, 15.0)
        }

        test("value returns exactly the value that was set") {
            onUiThreadGet {
                val slider = WSlider()
                slider.value = 42.0
                slider.value
            } shouldBe 42.0
        }

        test("a value above maximum gets clamped to maximum") {
            onUiThreadGet {
                val slider = WSlider(maximum = 100.0)
                slider.value = 150.0
                slider.value
            } shouldBe 100.0
        }

        test("a value below minimum gets clamped up to minimum") {
            onUiThreadGet {
                val slider = WSlider(minimum = 10.0)
                slider.value = -5.0
                slider.value
            } shouldBe 10.0
        }

        test("the ChangeListener fires with the new value on every change to value") {
            val received = mutableListOf<Double>()
            onUiThread {
                val slider = WSlider()
                slider.addChangeListener { received.add(it) }
                slider.value = 30.0
                slider.value = 60.0
            }
            received shouldContainExactly listOf(30.0, 60.0)
        }

        test("a listener removed via removeChangeListener no longer fires") {
            val received = mutableListOf<Double>()
            onUiThread {
                val slider = WSlider()
                val listener: (Double) -> Unit = { received.add(it) }
                slider.addChangeListener(listener)
                slider.value = 30.0
                slider.removeChangeListener(listener)
                slider.value = 60.0
            }
            received shouldContainExactly listOf(30.0)
        }

        test("orientation and isDirectionReversed return exactly the values that were set") {
            val (orientation, reversed) = onUiThreadGet {
                val slider = WSlider()
                slider.orientation = Orientation.VERTICAL
                slider.isDirectionReversed = true
                slider.orientation to slider.isDirectionReversed
            }
            orientation shouldBe Orientation.VERTICAL
            reversed shouldBe true
        }
    }
}
