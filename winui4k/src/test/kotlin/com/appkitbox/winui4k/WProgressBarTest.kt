package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/** Tests verifying WProgressBar's value/range (RangeBase) and display state. */
class WProgressBarTest : FunSpec() {
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
    }
}
