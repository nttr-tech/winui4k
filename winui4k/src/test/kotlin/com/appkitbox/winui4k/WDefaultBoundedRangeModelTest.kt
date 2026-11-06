package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests the invariant `min <= value <= value + extent <= max` and the notifications of WDefaultBoundedRangeModel
 * (DefaultBoundedRangeModel-style) (no window is used). The expected values were worked out by hand by following
 * the same operations on Swing's DefaultBoundedRangeModel. The shape seen from Java is in WBoundedRangeModelForJavaTest.
 */
class WDefaultBoundedRangeModelTest : FunSpec() {
    /** The (value, extent, min, max) quadruple. */
    private fun WBoundedRangeModel.state() = listOf(value, extent, minimum, maximum)

    /** Adds a listener that counts how many times it is called. */
    private fun WBoundedRangeModel.countChanges(): IntArray {
        val count = IntArray(1)
        addChangeListener { count[0]++ }
        return count
    }

    init {
        test("the no-argument default is value 0, extent 0, min 0 and max 100, as in Swing") {
            WDefaultBoundedRangeModel().state() shouldContainExactly listOf(0.0, 0.0, 0.0, 100.0)
        }

        test("the argument order is the same as Swing (value, extent, min, max)") {
            WDefaultBoundedRangeModel(50.0, 10.0, 5.0, 90.0).state() shouldContainExactly listOf(50.0, 10.0, 5.0, 90.0)
        }

        test("arguments that break the invariant throw IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> { WDefaultBoundedRangeModel(150.0, 0.0, 0.0, 100.0) } // value > max
            shouldThrow<IllegalArgumentException> { WDefaultBoundedRangeModel(5.0, 0.0, 10.0, 100.0) } // value < min
            shouldThrow<IllegalArgumentException> { WDefaultBoundedRangeModel(50.0, 60.0, 0.0, 100.0) } // value + extent > max
            shouldThrow<IllegalArgumentException> { WDefaultBoundedRangeModel(0.0, -5.0, 0.0, 100.0) } // extent < 0
            shouldThrow<IllegalArgumentException> { WDefaultBoundedRangeModel(0.0, 0.0, 10.0, 5.0) } // max < min
        }

        test("arguments exactly on the boundary (value + extent == max, min == max) are accepted") {
            WDefaultBoundedRangeModel(90.0, 10.0, 0.0, 100.0).state() shouldContainExactly listOf(90.0, 10.0, 0.0, 100.0)
            WDefaultBoundedRangeModel(7.0, 0.0, 7.0, 7.0).state() shouldContainExactly listOf(7.0, 0.0, 7.0, 7.0)
        }

        test("setValue clamps so that value + extent fits within max (setting 150 with 0..100 and extent 10 gives 90)") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            model.value = 150.0
            model.value shouldBe 90.0
            model.value = -5.0
            model.value shouldBe 0.0
            model.value = 33.5
            model.value shouldBe 33.5
        }

        test("setExtent clamps to 0 or more and max - value or less") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            model.extent = 60.0
            model.extent shouldBe 50.0
            model.extent = -5.0
            model.extent shouldBe 0.0
            model.state() shouldContainExactly listOf(50.0, 0.0, 0.0, 100.0)
        }

        test("setMinimum raises a smaller value (setting min to 60 with value 50 makes value 60 and keeps extent 10)") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            model.minimum = 60.0
            model.state() shouldContainExactly listOf(60.0, 10.0, 60.0, 100.0)
        }

        test("setMinimum above max raises max too (extent becomes 0)") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            model.minimum = 120.0
            model.state() shouldContainExactly listOf(120.0, 0.0, 120.0, 120.0)
        }

        test("setMaximum keeps extent and lowers value so that value + extent fits within max") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            model.maximum = 70.0
            model.state() shouldContainExactly listOf(50.0, 10.0, 0.0, 70.0)
            model.maximum = 55.0
            model.state() shouldContainExactly listOf(45.0, 10.0, 0.0, 55.0)
        }

        test("setMaximum below value lowers value to max - extent (same as the JDK's DefaultBoundedRangeModel.setMaximum)") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            model.maximum = 40.0
            model.state() shouldContainExactly listOf(30.0, 10.0, 0.0, 40.0)
        }

        test("setMaximum below min lowers min and value too, and shrinks extent to at most max - min") {
            val model = WDefaultBoundedRangeModel(70.0, 10.0, 50.0, 100.0)
            model.maximum = 20.0
            model.state() shouldContainExactly listOf(20.0, 0.0, 20.0, 20.0)
        }

        test("setRangeProperties fixes the values to satisfy the invariant and notifies only once") {
            val model = WDefaultBoundedRangeModel()
            val count = model.countChanges()
            model.setRangeProperties(20.0, 200.0, 0.0, 100.0, false)
            model.state() shouldContainExactly listOf(20.0, 80.0, 0.0, 100.0)
            count[0] shouldBe 1
        }

        test("setRangeProperties with min > max sets min to max, and widens max to value when value is outside") {
            val model = WDefaultBoundedRangeModel()
            model.setRangeProperties(5.0, 0.0, 10.0, 0.0, false)
            model.state() shouldContainExactly listOf(5.0, 0.0, 0.0, 5.0)
        }

        test("changing valueIsAdjusting is notified, and setting the same value again is not") {
            val model = WDefaultBoundedRangeModel()
            val count = model.countChanges()
            model.valueIsAdjusting = true
            model.valueIsAdjusting shouldBe true
            model.valueIsAdjusting = true
            count[0] shouldBe 1
            model.valueIsAdjusting = false
            count[0] shouldBe 2
        }

        test("notifies only on an actual change, not for settings clamped to the same value or the same value set again") {
            val model = WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0)
            val count = model.countChanges()
            model.value = 50.0
            model.extent = 10.0
            model.minimum = 0.0
            model.maximum = 100.0
            model.value = 95.0 // Clamped to 90, so it changes
            model.value = 90.0 // Already 90
            model.value = 200.0 // Clamped to 90, so it does not change
            count[0] shouldBe 1
        }

        test("the source of the notified event is the model itself") {
            val model = WDefaultBoundedRangeModel()
            var source: Any? = null
            model.addChangeListener { source = it.source }
            model.value = 1.0
            (source === model) shouldBe true
        }

        test("a listener added twice is called twice, and remove removes only one") {
            val model = WDefaultBoundedRangeModel()
            var count = 0
            val listener = WChangeListener { count++ }
            model.addChangeListener(listener)
            model.addChangeListener(listener)
            model.value = 1.0
            count shouldBe 2
            model.removeChangeListener(listener)
            model.value = 2.0
            count shouldBe 3
            model.removeChangeListener(listener)
            model.value = 3.0
            count shouldBe 3
        }

        test("removing a listener that is not added does nothing") {
            val model = WDefaultBoundedRangeModel()
            model.removeChangeListener { }
            model.getChangeListeners().size shouldBe 0
        }

        test("listeners are called starting from the most recently added one, as in Swing, and getChangeListeners is in the order they were added") {
            val model = WDefaultBoundedRangeModel()
            val calls = mutableListOf<String>()
            val first = WChangeListener { calls += "first" }
            val second = WChangeListener { calls += "second" }
            model.addChangeListener(first)
            model.addChangeListener(second)
            model.value = 1.0
            calls shouldContainExactly listOf("second", "first")
            model.getChangeListeners().toList() shouldContainExactly listOf(first, second)
        }

        test("a listener removed during a notification still receives it (it goes to the list as it was when it started)") {
            val model = WDefaultBoundedRangeModel()
            val calls = mutableListOf<String>()
            val removed = WChangeListener { calls += "removed" }
            // Listeners are called starting from the most recently added one, so the remover, called first, removes removed
            model.addChangeListener(removed)
            model.addChangeListener { model.removeChangeListener(removed) }
            model.value = 1.0
            calls shouldContainExactly listOf("removed")
            model.value = 2.0
            calls shouldContainExactly listOf("removed")
        }

        test("a listener added during a notification is not called for it") {
            val model = WDefaultBoundedRangeModel()
            var lateCalls = 0
            model.addChangeListener { model.addChangeListener { lateCalls++ } }
            model.value = 1.0
            lateCalls shouldBe 0
        }

        test("toString has the same format as Swing") {
            WDefaultBoundedRangeModel(50.0, 10.0, 0.0, 100.0).toString() shouldBe
                "com.appkitbox.winui4k.WDefaultBoundedRangeModel[value=50.0, extent=10.0, min=0.0, max=100.0, adj=false]"
        }
    }
}
