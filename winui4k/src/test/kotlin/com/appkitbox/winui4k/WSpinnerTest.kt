package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests verifying WSpinner (NumberBox)'s value and ChangeListener. */
class WSpinnerTest : FunSpec() {
    init {
        test("the initial value with nothing entered is NaN") {
            onUiThreadGet { WSpinner().value.isNaN() }.shouldBeTrue()
        }

        test("the value passed to the constructor can be retrieved via value") {
            onUiThreadGet { WSpinner(7.5).value } shouldBe 7.5
        }

        test("minimum / maximum / smallChange return exactly the values that were set") {
            onUiThreadGet {
                val spinner = WSpinner()
                spinner.minimum = 0.0
                spinner.maximum = 10.0
                spinner.smallChange = 0.5
                Triple(spinner.minimum, spinner.maximum, spinner.smallChange)
            } shouldBe Triple(0.0, 10.0, 0.5)
        }

        test("the ChangeListener fires with the new value on every change to value") {
            val received = mutableListOf<Double>()
            onUiThread {
                val spinner = WSpinner()
                spinner.addChangeListener { received.add(it) }
                spinner.value = 3.0
                spinner.value = 8.0
            }
            received shouldContainExactly listOf(3.0, 8.0)
        }

        test("isWrapEnabled and acceptsExpression return exactly the values that were set") {
            val (wrap, expression) = onUiThreadGet {
                val spinner = WSpinner()
                spinner.isWrapEnabled = true
                spinner.acceptsExpression = true
                spinner.isWrapEnabled to spinner.acceptsExpression
            }
            wrap.shouldBeTrue()
            expression.shouldBeTrue()
        }
    }
}
