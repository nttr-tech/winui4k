package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/** Tests WSlider's value and range (RangeBase and model), ChangeListener, and the JSlider-style tick and other settings. */
@Tags("E2E")
class WSliderTest : FunSpec() {
    /** The native RangeBase's [Minimum, Maximum, Value]. */
    private fun nativeRange(slider: WSlider): List<Double> {
        val rangeBase = slider.inspectable.queryInterface(XamlInterop.IID_IRangeBase)
        return try {
            listOf(
                rangeBase.getDouble(XamlInterop.IRangeBase_get_Minimum),
                rangeBase.getDouble(XamlInterop.IRangeBase_get_Maximum),
                rangeBase.getDouble(XamlInterop.IRangeBase_get_Value),
            )
        } finally {
            rangeBase.release()
        }
    }

    /** Adds a WChangeListener to [slider] and returns a list that records each notification as a string made by [describe]. */
    private fun record(slider: WSlider, describe: (WSlider) -> String = { "${it.value}" }): MutableList<String> {
        val events = mutableListOf<String>()
        slider.addChangeListener(WChangeListener { e -> events += describe(e.source as WSlider) })
        return events
    }

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

        test("WChangeListener is called once for each change of the value, minimum, maximum and extent with the slider as the source, and not for setting the same value") {
            val (events, sameSource) = onUiThreadGet {
                val slider = WSlider()
                var sameSource = true
                slider.addChangeListener(WChangeListener { e -> sameSource = sameSource && e.source === slider })
                val events = record(slider) { "${it.minimum}/${it.value}/${it.extent}/${it.maximum}" }
                slider.value = 30.0
                slider.value = 30.0
                slider.maximum = 80.0
                slider.extent = 10.0
                slider.minimum = 40.0 // The value 30 is also raised to 40, but only one notification is sent
                slider.minimum = 40.0
                events to sameSource
            }
            events shouldContainExactly listOf("0.0/30.0/0.0/100.0", "0.0/30.0/0.0/80.0", "0.0/30.0/10.0/80.0", "40.0/40.0/10.0/80.0")
            sameSource.shouldBeTrue()
        }

        test("the extent is represented by setting the native Maximum to maximum - extent, and the value does not exceed maximum - extent (150 gives 90 with 0..100 and extent 10)") {
            val (value, native) = onUiThreadGet {
                val slider = WSlider()
                slider.extent = 10.0
                slider.value = 150.0
                slider.value to nativeRange(slider)
            }
            value shouldBe 90.0
            native shouldContainExactly listOf(0.0, 90.0, 90.0)
        }

        test("widening the range after clamping does not restore the value (WinUI remembers the value before clamping and restores it, but JSlider does not)") {
            val (value, native) = onUiThreadGet {
                val slider = WSlider()
                slider.value = 150.0
                slider.maximum = 200.0
                slider.value to nativeRange(slider)
            }
            value shouldBe 100.0
            native shouldContainExactly listOf(0.0, 200.0, 100.0)
        }

        test("setting the maximum below the minimum lowers the minimum and the value too (same as Swing's model)") {
            val (values, native) = onUiThreadGet {
                val slider = WSlider(minimum = 50.0, maximum = 100.0, value = 70.0)
                slider.maximum = 20.0
                listOf(slider.minimum, slider.value, slider.maximum) to nativeRange(slider)
            }
            values shouldContainExactly listOf(20.0, 20.0, 20.0)
            native shouldContainExactly listOf(20.0, 20.0, 20.0)
        }

        test("the Kotlin lambda form is called only when the value changes (not for changes of only the minimum, maximum or dragging state)") {
            val received = onUiThreadGet {
                val slider = WSlider()
                val received = mutableListOf<Double>()
                slider.addChangeListener { v -> received += v }
                slider.maximum = 50.0
                slider.valueIsAdjusting = true
                slider.value = 20.0
                slider.valueIsAdjusting = false
                slider.minimum = 30.0
                received
            }
            received shouldContainExactly listOf(20.0, 30.0)
        }

        test("a valueIsAdjusting change is also notified as one change and goes into the model") {
            val (events, modelAdjusting) = onUiThreadGet {
                val slider = WSlider()
                val events = record(slider) { "${it.value}:${it.valueIsAdjusting}" }
                slider.valueIsAdjusting = true
                slider.value = 10.0
                val modelAdjusting = slider.model.valueIsAdjusting
                slider.valueIsAdjusting = false
                events to modelAdjusting
            }
            events shouldContainExactly listOf("0.0:true", "10.0:true", "10.0:false")
            modelAdjusting.shouldBeTrue()
        }

        test("the default model is a WDefaultBoundedRangeModel, and changing the model directly moves the slider and the native side and calls the listeners") {
            val (isDefault, events, native) = onUiThreadGet {
                val slider = WSlider()
                val events = record(slider)
                val model = slider.model
                model.value = 25.0
                Triple(model is WDefaultBoundedRangeModel, events.toList(), nativeRange(slider))
            }
            isDefault.shouldBeTrue()
            events shouldContainExactly listOf("25.0")
            native shouldContainExactly listOf(0.0, 100.0, 25.0)
        }

        test("when two sliders share one model, moving either moves both (including the native side), and each one's listeners are called with itself as the source") {
            val (events, natives) = onUiThreadGet {
                val model = WDefaultBoundedRangeModel(20.0, 0.0, 0.0, 50.0)
                val first = WSlider(model)
                val second = WSlider()
                second.model = model
                val events = mutableListOf<String>()
                first.addChangeListener(WChangeListener { e -> events += "first:${(e.source as WSlider).value}:${e.source === first}" })
                second.addChangeListener(WChangeListener { e -> events += "second:${(e.source as WSlider).value}:${e.source === second}" })
                first.value = 40.0
                model.value = 10.0
                events.sorted() to (nativeRange(first) + nativeRange(second))
            }
            events shouldContainExactly listOf("first:10.0:true", "first:40.0:true", "second:10.0:true", "second:40.0:true")
            natives shouldContainExactly listOf(0.0, 50.0, 10.0, 0.0, 50.0, 10.0)
        }

        test("setModel, like JSlider, updates the native side to the new model without calling the listeners, and changes of the old model no longer arrive") {
            val (events, native, value) = onUiThreadGet {
                val slider = WSlider()
                val old = slider.model
                val events = record(slider)
                val replacement = WDefaultBoundedRangeModel(5.0, 0.0, 0.0, 10.0)
                slider.model = replacement
                old.value = 70.0
                check(slider.model === replacement) { "does not return the replaced model" }
                Triple(events.toList(), nativeRange(slider), slider.value)
            }
            events.shouldBeEmpty()
            native shouldContainExactly listOf(0.0, 10.0, 5.0)
            value shouldBe 5.0
        }

        test("a value moved by key also goes into the model and calls the listeners (without becoming a drag)") {
            val slider = onUiThreadGet { WSlider(minimum = 0.0, maximum = 10.0, value = 3.0) }
            val events = onUiThreadGet { record(slider) { "${it.value}:${it.valueIsAdjusting}" } }
            UiTestHarness.attachAndAwaitLoaded(slider)
            try {
                onUiThread { check(slider.requestFocusInWindow()) { "cannot move the focus" } }
                TestKeyboard.keyDown(VK_RIGHT, SCAN_RIGHT, extended = true)
                TestKeyboard.keyUp(VK_RIGHT, SCAN_RIGHT, extended = true)
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("4.0:false") }
                onUiThreadGet { slider.model.value } shouldBe 4.0
            } finally {
                UiTestHarness.detach(slider)
            }
        }

        test("the constructor taking an orientation and the constructor taking a model") {
            val (vertical, ranged, withModel) = onUiThreadGet {
                val vertical = WSlider(Orientation.VERTICAL)
                val ranged = WSlider(Orientation.VERTICAL, 10.0, 20.0, 15.0)
                val shared = WDefaultBoundedRangeModel(3.0, 1.0, 2.0, 9.0)
                val withModel = WSlider(shared)
                Triple(
                    listOf(vertical.orientation.name, "${vertical.minimum}", "${vertical.maximum}", "${vertical.value}"),
                    listOf(ranged.orientation.name, "${ranged.minimum}", "${ranged.maximum}", "${ranged.value}"),
                    listOf("${withModel.model === shared}", withModel.orientation.name) + nativeRange(withModel).map { "$it" },
                )
            }
            vertical shouldContainExactly listOf("VERTICAL", "0.0", "100.0", "0.0")
            ranged shouldContainExactly listOf("VERTICAL", "10.0", "20.0", "15.0")
            withModel shouldContainExactly listOf("true", "HORIZONTAL", "2.0", "8.0", "3.0")
        }

        test("majorTickSpacing becomes tickFrequency, and minorTickSpacing is used only when majorTickSpacing is 0") {
            val frequencies = onUiThreadGet {
                val slider = WSlider()
                val result = mutableListOf(slider.majorTickSpacing, slider.minorTickSpacing)
                slider.minorTickSpacing = 5.0
                result += slider.tickFrequency
                slider.majorTickSpacing = 25.0
                result += slider.tickFrequency
                slider.majorTickSpacing = 0.0
                result += slider.tickFrequency
                result
            }
            frequencies shouldContainExactly listOf(0.0, 0.0, 5.0, 25.0, 5.0)
        }

        test("paintTicks false makes it NONE, true from NONE makes it BOTTOM_RIGHT, and other positions are kept (the default is NONE and false, as with JSlider)") {
            val placements = onUiThreadGet {
                val slider = WSlider()
                val result = mutableListOf("${slider.tickPlacement}:${slider.paintTicks}")
                slider.paintTicks = false
                result += "${slider.tickPlacement}:${slider.paintTicks}"
                slider.paintTicks = true
                result += "${slider.tickPlacement}:${slider.paintTicks}"
                slider.tickPlacement = TickPlacement.OUTSIDE
                slider.paintTicks = true
                result += "${slider.tickPlacement}"
                result
            }
            placements shouldContainExactly listOf("NONE:false", "NONE:false", "BOTTOM_RIGHT:true", "OUTSIDE")
        }

        test("snapToTicks is snapsTo's TICKS / STEP_VALUES, and inverted is the same as isDirectionReversed") {
            val states = onUiThreadGet {
                val slider = WSlider()
                val result = mutableListOf("${slider.snapToTicks}:${slider.inverted}")
                slider.snapToTicks = true
                slider.inverted = true
                result += "${slider.snapsTo}:${slider.isDirectionReversed}"
                slider.snapToTicks = false
                slider.isDirectionReversed = false
                result += "${slider.snapsTo}:${slider.inverted}"
                result
            }
            states shouldContainExactly listOf("false:false", "TICKS:true", "STEP_VALUES:false")
        }

        test("header is read back from WinUI, and unset is an empty string") {
            val headers = onUiThreadGet {
                val slider = WSlider()
                val initial = slider.header
                slider.header = "Volume"
                listOf(initial, slider.header)
            }
            headers shouldContainExactly listOf("", "Volume")
        }
    }

    private companion object {
        const val VK_RIGHT = 0x27
        const val SCAN_RIGHT = 0x4D
    }
}
