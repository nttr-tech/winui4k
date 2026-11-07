package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the heights of the box and the text of a WCheckBox stretched to the row height (display issue 20 in doc/swing_to_winui4k_issues.md).
 * WinUI's CheckBox template puts the box at the vertical center and the text at the top (VerticalContentAlignment = Top), so they
 * drift apart when the height grows. Swing's JCheckBox lines up the box and the text at the same height.
 */
@Tags("E2E")
class WCheckBoxStretchTest : FunSpec() {
    /** The vertical center of the template's [part] in [box] (measured from the top of [box]). */
    private fun partCenterY(box: WCheckBox, part: String): Double {
        val element: ComPtr = checkNotNull(Xaml.templatePartOrNull(box.inspectable, part)) { "no $part" }
        return try {
            val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
            try {
                Xaml.positionInRoot(element)[1] - Xaml.positionInRoot(box.uiElement)[1] +
                    framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight) / 2
            } finally {
                framework.release()
            }
        } finally {
            element.release()
        }
    }

    /** The vertical center of the text minus the vertical center of the box (DIP). */
    private fun textOffset(box: WCheckBox): Double = onUiThreadGet {
        partCenterY(box, "ContentPresenter") - partCenterY(box, "NormalRectangle")
    }

    /** Shows [boxes] in a panel (300 x [height]) with [layout]. */
    private fun show(layout: WLayoutManager, height: Double, vararg boxes: WCheckBox): WLayoutPanel {
        val panel = onUiThreadGet {
            WLayoutPanel(layout).also {
                it.width = 300.0
                it.height = height
                it.horizontalAlignment = HorizontalAlignment.LEFT
                it.verticalAlignment = VerticalAlignment.TOP
                boxes.forEach(it::add)
            }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            panel.revalidate()
            panel.validate()
            panel.updateLayout()
        }
        return panel
    }

    init {
        test("the text of a check box stretched to a GridLayout row height lines up at the same height as the box, as when not stretched") {
            val natural = onUiThreadGet { WCheckBox("Remember me") }
            val flow = show(WFlowLayout(WFlowLayout.LEFT), 100.0, natural)
            val naturalOffset = try {
                textOffset(natural)
            } finally {
                UiTestHarness.detach(flow)
            }
            // Even when not stretched, the center of the text is within 1.5 DIP of the center of the box (the same as WinUI's default look)
            abs(naturalOffset) shouldBeLessThanOrEqual 1.5

            val boxes = onUiThreadGet { listOf(WCheckBox("Send me the newsletter"), WCheckBox("Remember me")) }
            val grid = show(WGridLayout(0, 1), 200.0, *boxes.toTypedArray())
            try {
                eventually(3.seconds) { onUiThreadGet { boxes[0].actualHeight } shouldBe (100.0 plusOrMinus 0.5) }
                boxes.forEach { box ->
                    withClue("\"${onUiThreadGet { box.text }}\"") { textOffset(box) shouldBe (naturalOffset plusOrMinus 0.5) }
                }
            } finally {
                UiTestHarness.detach(grid)
            }
        }

        test("even with a different font size and stretched, the text lines up at the same height as the box") {
            val box = onUiThreadGet { WCheckBox("Large text").also { it.font = WFont("Segoe UI", WFont.PLAIN, 24.0) } }
            val grid = show(WGridLayout(0, 1), 120.0, box)
            try {
                eventually(3.seconds) { onUiThreadGet { box.actualHeight } shouldBe (120.0 plusOrMinus 0.5) }
                abs(textOffset(box)) shouldBeLessThanOrEqual 1.5
            } finally {
                UiTestHarness.detach(grid)
            }
        }
    }
}
