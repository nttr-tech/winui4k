package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests WProgressBar's value, range (synchronization of the model and RangeBase), ChangeListener, progress string and display states.
 * The shape seen from Java is in WProgressBarForJavaTest.
 */
@Tags("E2E")
class WProgressBarTest : FunSpec() {
    /** The native RangeBase's (Value, Minimum, Maximum). */
    private fun WProgressBar.nativeState(): List<Double> {
        val rangeBase = inspectable.queryInterface(XamlInterop.IID_IRangeBase)
        return try {
            listOf(
                rangeBase.getDouble(XamlInterop.IRangeBase_get_Value),
                rangeBase.getDouble(XamlInterop.IRangeBase_get_Minimum),
                rangeBase.getDouble(XamlInterop.IRangeBase_get_Maximum),
            )
        } finally {
            rangeBase.release()
        }
    }

    private fun WProgressBar.itemStatus(): String = Xaml.getAutomation(automationTarget, XamlInterop.IAutomationPropertiesStatics_GetItemStatus)

    init {
        test("the minimum / maximum / value passed to the constructor take effect") {
            onUiThreadGet {
                val progressBar = WProgressBar(minimum = 5.0, maximum = 50.0, value = 25.0)
                Triple(progressBar.minimum, progressBar.maximum, progressBar.value)
            } shouldBe Triple(5.0, 50.0, 25.0)
        }

        test("a value above maximum gets clamped to maximum") {
            onUiThreadGet {
                val progressBar = WProgressBar(maximum = 100.0)
                progressBar.value = 200.0
                progressBar.value
            } shouldBe 100.0
        }

        test("isIndeterminate / showError / showPaused return exactly the values that were set") {
            val states = onUiThreadGet {
                val progressBar = WProgressBar()
                progressBar.isIndeterminate = true
                progressBar.showError = true
                progressBar.showPaused = true
                Triple(progressBar.isIndeterminate, progressBar.showError, progressBar.showPaused)
            }
            states.first.shouldBeTrue()
            states.second.shouldBeTrue()
            states.third.shouldBeTrue()
        }

        test("the default model is a WDefaultBoundedRangeModel with 0..100 and value 0, and it is copied to the native side") {
            onUiThread {
                val progressBar = WProgressBar()
                progressBar.model.shouldBeInstanceOf<WDefaultBoundedRangeModel>()
                progressBar.nativeState() shouldContainExactly listOf(0.0, 0.0, 100.0)
            }
        }

        test("the two-Int-argument constructor starts the value at the minimum, as JProgressBar does") {
            onUiThread {
                val positive = WProgressBar(10, 20)
                listOf(positive.value, positive.minimum, positive.maximum) shouldContainExactly listOf(10.0, 10.0, 20.0)
                WProgressBar(-10, 10).value shouldBe -10.0
            }
        }

        test("the constructor taking a model uses the model as is and sets its extent to 0, as JProgressBar does") {
            onUiThread {
                val model = WDefaultBoundedRangeModel(20.0, 10.0, 0.0, 50.0)
                val progressBar = WProgressBar(model)
                progressBar.model shouldBeSameInstanceAs model
                model.extent shouldBe 0.0
                progressBar.nativeState() shouldContainExactly listOf(20.0, 0.0, 50.0)
            }
        }

        test("ChangeListener is called once for each change of the value, minimum and maximum and not for the same value; the source is the progress bar") {
            onUiThread {
                val progressBar = WProgressBar()
                val sources = mutableListOf<Any>()
                val listener = WChangeListener { e -> sources += e.source }
                progressBar.addChangeListener(listener)
                progressBar.value = 30.0
                progressBar.value = 30.0
                progressBar.minimum = 10.0
                progressBar.maximum = 80.0
                sources.size shouldBe 3
                sources.forEach { it shouldBeSameInstanceAs progressBar }
                progressBar.getChangeListeners().toList() shouldContainExactly listOf(listener)
                progressBar.removeChangeListener(listener)
                progressBar.value = 40.0
                sources.size shouldBe 3
            }
        }

        test("making the maximum smaller than the value and back does not restore the pre-clamping value WinUI remembers (the model's value is rewritten)") {
            onUiThread {
                val progressBar = WProgressBar(0.0, 100.0, 80.0)
                progressBar.maximum = 50.0
                progressBar.value shouldBe 50.0
                progressBar.maximum = 100.0
                progressBar.value shouldBe 50.0 // As with Swing's setMaximum, the value stays lowered
                progressBar.nativeState() shouldContainExactly listOf(50.0, 0.0, 100.0)
                progressBar.minimum = 60.0 // Making the minimum larger than the value raises the value too
                progressBar.minimum = 0.0
                progressBar.nativeState() shouldContainExactly listOf(60.0, 0.0, 100.0)
            }
        }

        test("percentComplete is (value - minimum) / (maximum - minimum), and NaN if the minimum and maximum are the same") {
            onUiThread {
                val progressBar = WProgressBar(100.0, 300.0, 150.0)
                progressBar.percentComplete shouldBe 0.25
                progressBar.value = 300.0
                progressBar.percentComplete shouldBe 1.0
                progressBar.maximum = 100.0
                progressBar.percentComplete.isNaN().shouldBeTrue()
            }
        }

        test("string is the percentage completed unless set, and setting a set string back to null returns to the percentage") {
            onUiThread {
                val progressBar = WProgressBar(0.0, 3.0, 1.0)
                progressBar.string shouldBe "33%"
                progressBar.value = 3.0
                progressBar.string shouldBe "100%"
                progressBar.string = "Copying"
                progressBar.string shouldBe "Copying"
                progressBar.string = null
                progressBar.string shouldBe "100%"
            }
        }

        test("while isStringPainted is true, string is exposed as the UI Automation ItemStatus and is put in again when the value changes") {
            onUiThread {
                val progressBar = WProgressBar()
                progressBar.isStringPainted shouldBe false
                progressBar.value = 50.0
                progressBar.itemStatus() shouldBe ""
                progressBar.isStringPainted = true
                progressBar.itemStatus() shouldBe "50%"
                progressBar.value = 75.0
                progressBar.itemStatus() shouldBe "75%"
                progressBar.string = "Almost done"
                progressBar.itemStatus() shouldBe "Almost done"
                progressBar.isStringPainted = false
                progressBar.itemStatus() shouldBe ""
            }
        }

        test("sharing one model with a WSlider makes the progress bar move when the slider moves, and the progress bar's listeners are called too") {
            onUiThread {
                val model = WDefaultBoundedRangeModel()
                val slider = WSlider(model)
                val progressBar = WProgressBar(model)
                val sources = mutableListOf<Any>()
                progressBar.addChangeListener { e -> sources += e.source }
                slider.value = 40.0
                progressBar.value shouldBe 40.0
                progressBar.nativeState()[0] shouldBe 40.0
                sources.size shouldBe 1
                sources[0] shouldBeSameInstanceAs progressBar
                progressBar.value = 60.0
                slider.value shouldBe 60.0
                slider.maximum = 80.0
                progressBar.nativeState() shouldContainExactly listOf(60.0, 0.0, 80.0)
            }
        }
    }
}
