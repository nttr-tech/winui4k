package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests verifying WProgressRing's display state and determinate progress value. */
class WProgressRingTest : FunSpec() {
    init {
        test("the initial state is isIndeterminate = true (keeps spinning to show work in progress)") {
            onUiThreadGet { WProgressRing().isIndeterminate }.shouldBeTrue()
        }

        test("isActive = false hides the ring") {
            onUiThreadGet {
                val progressRing = WProgressRing()
                progressRing.isActive = false
                progressRing.isActive
            }.shouldBeFalse()
        }

        test("isIndeterminate = false lets value / minimum / maximum express determinate progress") {
            onUiThreadGet {
                val progressRing = WProgressRing()
                progressRing.isIndeterminate = false
                progressRing.minimum = 0.0
                progressRing.maximum = 10.0
                progressRing.value = 7.0
                Triple(progressRing.minimum, progressRing.maximum, progressRing.value)
            } shouldBe Triple(0.0, 10.0, 7.0)
        }

        test("the default model is 0..100 with value 0, the same as ProgressRing's default, and model changes are copied to the native side") {
            onUiThread {
                val progressRing = WProgressRing()
                progressRing.model.shouldBeInstanceOf<WDefaultBoundedRangeModel>()
                progressRing.nativeState() shouldContainExactly listOf(0.0, 0.0, 100.0)
                progressRing.model.setRangeProperties(30.0, 0.0, 10.0, 40.0, false)
                progressRing.nativeState() shouldContainExactly listOf(30.0, 10.0, 40.0)
                progressRing.percentComplete shouldBe 2.0 / 3.0
                progressRing.string shouldBe "67%"
            }
        }

        test("ChangeListener is called once for each setting of the value (ProgressRing has no value event), and the source is the progress ring") {
            onUiThread {
                val progressRing = WProgressRing()
                val sources = mutableListOf<Any>()
                progressRing.addChangeListener { e -> sources += e.source }
                progressRing.value = 10.0
                progressRing.value = 10.0
                progressRing.maximum = 50.0
                sources.size shouldBe 2
                sources.forEach { it shouldBeSameInstanceAs progressRing }
            }
        }

        test("sharing a model with a WProgressBar makes the progress ring move when the progress bar's value is set") {
            onUiThread {
                val model = WDefaultBoundedRangeModel(0.0, 0.0, 0.0, 10.0)
                val progressRing = WProgressRing(model)
                val progressBar = WProgressBar(model)
                progressBar.value = 4.0
                progressRing.value shouldBe 4.0
                progressRing.nativeState() shouldContainExactly listOf(4.0, 0.0, 10.0)
            }
        }
    }

    /** The native ProgressRing's (Value, Minimum, Maximum). */
    private fun WProgressRing.nativeState() = listOf(
        inspectable.getDouble(XamlInterop.IProgressRing_get_Value),
        inspectable.getDouble(XamlInterop.IProgressRing_get_Minimum),
        inspectable.getDouble(XamlInterop.IProgressRing_get_Maximum),
    )
}
