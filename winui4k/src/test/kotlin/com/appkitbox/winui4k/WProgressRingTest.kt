package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

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
    }
}
