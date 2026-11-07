package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify sliders, progress bars, progress rings and spinners without a given width do not shrink or disappear under a layout manager ([WFlowLayout])
 * (display issue 5 in doc/swing_to_winui4k_issues.md). The preferred sizes match Swing
 * (200 for JSlider and 146 for JProgressBar; BasicSliderUI.getPreferredHorizontalSize / BasicProgressBarUI.getPreferredInnerHorizontal).
 */
@Tags("E2E")
class WRangeControlPreferredSizeTest : FunSpec() {
    /** Shows [component] in a 600 x 300 panel with a left-aligned [WFlowLayout] and waits until it is laid out. */
    private fun showInFlow(component: WComponent): WLayoutPanel {
        val panel = onUiThreadGet {
            WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also {
                it.width = 600.0
                it.height = 300.0
                it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                it.verticalAlignmentInParent = VerticalAlignment.TOP
                it.add(component)
            }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            panel.revalidate()
            panel.validate()
        }
        return panel
    }

    private suspend fun <T : WComponent> withShown(component: T, block: suspend (T) -> Unit) {
        val panel = showInFlow(component)
        try {
            block(component)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    private fun boundsOf(component: WComponent): WRectangle = onUiThreadGet { component.bounds }

    init {
        test("a horizontal slider without a given width is placed with a width of 200, as with JSlider, and the track is visible") {
            withShown(onUiThreadGet { WSlider(0.0, 100.0, 30.0) }) { slider ->
                onUiThreadGet { slider.preferredSize().width } shouldBe (SLIDER_LENGTH plusOrMinus TOLERANCE)
                eventually(3.seconds) { boundsOf(slider).width.toDouble() shouldBe (SLIDER_LENGTH plusOrMinus TOLERANCE) }
                // The height is decided by the content (it is not 0)
                boundsOf(slider).height.toDouble() shouldBeGreaterThan 0.0
            }
        }

        test("a vertical slider without a given height is placed with a height of 200, as with JSlider") {
            withShown(onUiThreadGet { WSlider(Orientation.VERTICAL, 0.0, 100.0, 30.0) }) { slider ->
                onUiThreadGet { slider.preferredSize().height } shouldBe (SLIDER_LENGTH plusOrMinus TOLERANCE)
                eventually(3.seconds) { boundsOf(slider).height.toDouble() shouldBe (SLIDER_LENGTH plusOrMinus TOLERANCE) }
                boundsOf(slider).width.toDouble() shouldBeGreaterThan 0.0
            }
        }

        test("a slider with an explicit width gets that width even if narrower than 200") {
            withShown(onUiThreadGet { WSlider().also { it.width = 80.0 } }) { slider ->
                onUiThreadGet { slider.preferredSize().width } shouldBe (80.0 plusOrMinus TOLERANCE)
                eventually(3.seconds) { boundsOf(slider).width.toDouble() shouldBe (80.0 plusOrMinus TOLERANCE) }
            }
        }

        test("a progress bar without a given width is placed with a width of 146, as with JProgressBar, and the bar's height is not 0") {
            withShown(onUiThreadGet { WProgressBar(0, 100).also { it.value = 40.0 } }) { bar ->
                onUiThreadGet { bar.preferredSize().width } shouldBe (PROGRESS_BAR_LENGTH plusOrMinus TOLERANCE)
                eventually(3.seconds) { boundsOf(bar).width.toDouble() shouldBe (PROGRESS_BAR_LENGTH plusOrMinus TOLERANCE) }
                boundsOf(bar).height.toDouble() shouldBeGreaterThan 0.0
            }
        }

        test("a progress bar with an explicit width gets that width even if wider than 146") {
            withShown(onUiThreadGet { WProgressBar().also { it.width = 300.0 } }) { bar ->
                eventually(3.seconds) { boundsOf(bar).width.toDouble() shouldBe (300.0 plusOrMinus TOLERANCE) }
            }
        }

        test("a progress ring without a given size is placed at 32 x 32, the same as WinUI's default style") {
            withShown(onUiThreadGet { WProgressRing() }) { ring ->
                onUiThreadGet { ring.preferredSize() } shouldBe WSize(PROGRESS_RING_SIZE, PROGRESS_RING_SIZE)
                eventually(3.seconds) { boundsOf(ring) shouldBe WRectangle(5, 5, 32, 32) }
            }
        }

        test("a progress ring with an explicit size gets that size") {
            withShown(onUiThreadGet { WProgressRing().also { it.preferredSize = WSize(60.0, 60.0) } }) { ring ->
                eventually(3.seconds) { boundsOf(ring) shouldBe WRectangle(5, 5, 60, 60) }
            }
        }

        test("a spinner without a given width keeps enough width for the value's text when it gets focus and the clear button appears") {
            val spinner = onUiThreadGet {
                WSpinner(WSpinnerNumberModel(1, 0, 10, 1)).also { it.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE }
            }
            withShown(spinner) {
                onUiThread { check(spinner.requestFocusInWindow()) { "cannot move focus" } }
                eventually(3.seconds) { onUiThreadGet { deleteButtonWidth(spinner) } shouldBeGreaterThan 0.0 }
                onUiThread { spinner.parent?.validate() }
                // The value "10" (the longest value in the range) fits in the input field's text area (the ContentElement's width without its padding)
                val (textArea, needed) = onUiThreadGet { textAreaWidth(spinner) to textWidth(spinner, "10") }
                withClue("text area $textArea, needed width $needed") { textArea shouldBeGreaterThanOrEqual needed }
            }
        }

        test("the preferred width of a spinner does not change with or without focus (it does not move when laid out again)") {
            val spinner = onUiThreadGet {
                WSpinner(WSpinnerNumberModel(5, 0, 10, 1)).also { it.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE }
            }
            withShown(spinner) {
                val before = onUiThreadGet { spinner.preferredSize().width }
                onUiThread { check(spinner.requestFocusInWindow()) { "cannot move focus" } }
                eventually(3.seconds) { onUiThreadGet { deleteButtonWidth(spinner) } shouldBeGreaterThan 0.0 }
                onUiThread {
                    spinner.revalidate()
                    spinner.parent?.validate()
                }
                onUiThreadGet { spinner.preferredSize().width } shouldBe (before plusOrMinus TOLERANCE)
            }
        }
    }

    /** Calls [block] with the element [name] in the template of the input field (InputBox). */
    private fun <T> inputBoxPart(spinner: WSpinner, name: String, block: (com.appkitbox.winui4k.internal.com.ComPtr) -> T): T {
        val box = checkNotNull(Xaml.templatePartOrNull(spinner.inspectable, "InputBox")) { "no InputBox" }
        try {
            val part = checkNotNull(Xaml.templatePartOrNull(box, name)) { "no $name" }
            try {
                return block(part)
            } finally {
                part.release()
            }
        } finally {
            box.release()
        }
    }

    private fun actualWidthOf(element: com.appkitbox.winui4k.internal.com.ComPtr): Double {
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth)
        } finally {
            framework.release()
        }
    }

    /** The width of the clear button (0 if not shown). */
    private fun deleteButtonWidth(spinner: WSpinner): Double = inputBoxPart(spinner, "DeleteButton") { actualWidthOf(it) }

    /** The text area of the input field (the ContentElement's width minus its padding). */
    private fun textAreaWidth(spinner: WSpinner): Double = inputBoxPart(spinner, "ContentElement") { element ->
        val control = element.queryInterface(XamlInterop.IID_IControl)
        val padding = try {
            XamlStructs.getThickness(control, XamlInterop.IControl_get_Padding)
        } finally {
            control.release()
        }
        actualWidthOf(element) - padding[0] - padding[2]
    }

    /** The width of [text] drawn with the same font size as the spinner. */
    private fun textWidth(spinner: WSpinner, text: String): Double {
        val label = WLabel(text)
        label.fontSize = spinner.control.getDouble(XamlInterop.IControl_get_FontSize)
        val panel = spinner.parent as WContainer
        panel.add(label)
        try {
            label.uiElement.call(XamlInterop.IUIElement_UpdateLayout)
            return label.readDesiredSize().width
        } finally {
            panel.remove(label)
        }
    }

    private companion object {
        const val TOLERANCE = 1.0
        const val SLIDER_LENGTH = 200.0
        const val PROGRESS_BAR_LENGTH = 146.0
        const val PROGRESS_RING_SIZE = 32.0
    }
}
