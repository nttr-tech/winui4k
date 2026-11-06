package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the WProgressRing constructors that take a range (like JProgressBar(int, int) / JProgressBar(BoundedRangeModel)) and the
 * default of indeterminate (isIndeterminate) when a range or model is passed. WinUI's ProgressRing defaults to indeterminate (true), and JProgressBar to determinate (false).
 */
@Tags("E2E")
class WProgressRingRangeTest : FunSpec() {
    init {
        context("Defaults") {
            test("a progress ring created without arguments is indeterminate (isIndeterminate = true), as WinUI's default") {
                onUiThreadGet { WProgressRing().isIndeterminate } shouldBe true
            }

            test("a progress ring created with a model is determinate (isIndeterminate = false) like JProgressBar, and the native value is also false") {
                onUiThreadGet {
                    val ring = WProgressRing(WDefaultBoundedRangeModel(30.0, 0.0, 0.0, 50.0))
                    listOf(ring.isIndeterminate, ring.value, ring.maximum)
                } shouldContainExactly listOf(false, 30.0, 50.0)
            }

            test("WProgressRing(min, max) is determinate, and the value starts at the minimum (the same as JProgressBar(int, int))") {
                onUiThreadGet {
                    val ring = WProgressRing(10, 20)
                    listOf(ring.isIndeterminate, ring.minimum, ring.maximum, ring.value, ring.percentComplete)
                } shouldContainExactly listOf(false, 10.0, 20.0, 10.0, 0.0)
            }
        }

        context("Reading and writing") {
            test("even after creating it with a range, isIndeterminate = true makes it indeterminate again") {
                onUiThreadGet {
                    val ring = WProgressRing(0, 100)
                    ring.isIndeterminate = true
                    ring.isIndeterminate
                } shouldBe true
            }
        }

        context("Boundaries and exceptions") {
            test("WProgressRing(min, max) with a maximum less than the minimum throws IllegalArgumentException (the same as Swing)") {
                onUiThreadGet { runCatching { WProgressRing(10, 5) }.exceptionOrNull() }.shouldBeInstanceOf<IllegalArgumentException>()
            }

            test("a range whose minimum and maximum are the same can also be created, and percentComplete is NaN (the same as Swing)") {
                onUiThreadGet { WProgressRing(5, 5).percentComplete.isNaN() } shouldBe true
            }
        }
    }
}
