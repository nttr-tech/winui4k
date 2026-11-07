package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests that check the constructors that take int in Swing (FlowLayout(int, int, int), BorderLayout(int, int), GridLayout(int, int, int, int),
 * JSlider(int, int, int), JProgressBar(int, int), etc.) can be written with Int in Kotlin too, without `.0` or `toDouble()`, and
 * give the same values as the Double forms. In Java, int widens to double, so they could already be written.
 */
@Tags("E2E")
class IntArgumentOverloadsTest : FunSpec() {
    init {
        test("WFlowLayout(LEFT, 10, 12) takes the alignment and gaps as Int, and hgap / vgap are 10.0 / 12.0") {
            val layout = WFlowLayout(WFlowLayout.LEFT, 10, 12)
            listOf(layout.alignment.toDouble(), layout.hgap, layout.vgap) shouldContainExactly
                listOf(WFlowLayout.LEFT.toDouble(), 10.0, 12.0)
        }

        test("WBorderLayout(4, 6) takes the gaps as Int, and hgap / vgap are 4.0 / 6.0") {
            val layout = WBorderLayout(4, 6)
            (layout.hgap to layout.vgap) shouldBe (4.0 to 6.0)
        }

        test("WGridLayout(2, 3, 4, 5) takes the rows, columns and gaps as Int") {
            val layout = WGridLayout(2, 3, 4, 5)
            listOf(layout.rows.toDouble(), layout.columns.toDouble(), layout.hgap, layout.vgap) shouldContainExactly
                listOf(2.0, 3.0, 4.0, 5.0)
        }

        test("WSlider(0, 200, 30) takes the minimum, maximum and value as Int and gives the same values as the Double form") {
            onUiThreadGet {
                val slider = WSlider(0, 200, 30)
                listOf(slider.minimum, slider.maximum, slider.value)
            } shouldContainExactly listOf(0.0, 200.0, 30.0)
        }

        test("WSlider(10, 20) takes the minimum and maximum as Int, and the value is the same as the Double WSlider(10.0, 20.0)") {
            onUiThreadGet {
                val withInt = WSlider(10, 20)
                val withDouble = WSlider(10.0, 20.0)
                listOf(withInt.minimum, withInt.maximum, withInt.value) to listOf(withDouble.minimum, withDouble.maximum, withDouble.value)
            }.let { (withInt, withDouble) ->
                withInt shouldContainExactly withDouble
                withInt.take(2) shouldContainExactly listOf(10.0, 20.0)
            }
        }

        test("WSlider(VERTICAL, -10, 10, 5) takes the orientation and an Int range and value") {
            onUiThreadGet {
                val slider = WSlider(Orientation.VERTICAL, -10, 10, 5)
                Triple(slider.orientation, slider.minimum to slider.maximum, slider.value)
            } shouldBe Triple(Orientation.VERTICAL, -10.0 to 10.0, 5.0)
        }

        test("WProgressBar(0, 200, 50) takes the minimum, maximum and value as Int") {
            onUiThreadGet {
                val bar = WProgressBar(0, 200, 50)
                listOf(bar.minimum, bar.maximum, bar.value)
            } shouldContainExactly listOf(0.0, 200.0, 50.0)
        }
    }
}
