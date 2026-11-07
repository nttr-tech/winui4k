package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Tests that verify WButtonBase.horizontalAlignment / verticalAlignment (the alignment of the text and icon inside the button, the same as
 * AbstractButton.setHorizontalAlignment / setVerticalAlignment). They check the values go into WinUI's Control.HorizontalContentAlignment /
 * VerticalContentAlignment, the text moves inside a widened button, and it is separate from the button's position in its parent.
 */
@Tags("E2E")
class WButtonAlignmentTest : FunSpec() {
    /** The first TextBlock found in the visual tree under [element] (the caller releases it; null if none). */
    private fun firstTextBlock(element: ComPtr): ComPtr? {
        for (index in 0 until Xaml.childCount(element)) {
            val child = Xaml.childAt(element, index) ?: continue
            if (child.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.also { it.release() } != null) return child
            val found = firstTextBlock(child)
            child.release()
            if (found != null) return found
        }
        return null
    }

    /** The left, top, right and bottom edges of [element], measured from the top left of [origin]. */
    private fun edgesIn(element: ComPtr, origin: ComPtr): List<Double> {
        val position = Xaml.positionInRoot(element)
        val originPosition = Xaml.positionInRoot(origin)
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            val left = position[0] - originPosition[0]
            val top = position[1] - originPosition[1]
            listOf(
                left,
                top,
                left + framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth),
                top + framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight),
            )
        } finally {
            framework.release()
        }
    }

    /** Shows a 300 x 100 button at the top left and reads the left, top, right and bottom edges of the text (TextBlock) inside. */
    private fun textEdgesIn300x100(create: () -> WButtonBase, configure: WButtonBase.() -> Unit): List<Double> {
        val button = onUiThreadGet {
            create().also {
                it.width = BUTTON_WIDTH
                it.height = BUTTON_HEIGHT
                it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                it.verticalAlignmentInParent = VerticalAlignment.TOP
                it.configure()
            }
        }
        UiTestHarness.attachAndAwaitLoaded(button)
        try {
            return onUiThreadGet {
                button.updateLayout()
                val text = checkNotNull(firstTextBlock(button.uiElement))
                try {
                    edgesIn(text, button.uiElement)
                } finally {
                    text.release()
                }
            }
        } finally {
            UiTestHarness.detach(button)
        }
    }

    init {
        test("the default alignments are the same as Swing: CENTER for WButton and WToggleButton, and LEFT horizontally for WCheckBox and WRadioButton (corresponding to LEADING)") {
            // The same values before being added to a window (before the theme is applied) and after
            val expected = listOf(
                HorizontalAlignment.CENTER to VerticalAlignment.CENTER,
                HorizontalAlignment.CENTER to VerticalAlignment.CENTER,
                HorizontalAlignment.LEFT to VerticalAlignment.CENTER,
                // The vertical alignment of WRadioButton is TOP as in WinUI's template (unlike Swing's CENTER)
                HorizontalAlignment.LEFT to VerticalAlignment.TOP,
            )
            val (panel, buttons, before) = onUiThreadGet {
                val buttons = listOf(WButton("A"), WToggleButton("B"), WCheckBox("C"), WRadioButton("D"))
                val panel = WPanel()
                buttons.forEach { panel.add(it) }
                Triple(panel, buttons, buttons.map { it.horizontalAlignment to it.verticalAlignment })
            }
            before shouldBe expected
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThreadGet {
                    panel.updateLayout()
                    buttons.map { it.horizontalAlignment to it.verticalAlignment }
                } shouldBe expected
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("horizontalAlignment / verticalAlignment go into Control's HorizontalContentAlignment / VerticalContentAlignment") {
            onUiThreadGet {
                val button = WButton("Save")
                button.horizontalAlignment = HorizontalAlignment.RIGHT
                button.verticalAlignment = VerticalAlignment.BOTTOM
                listOf(button.horizontalContentAlignment, button.verticalContentAlignment, button.horizontalAlignment, button.verticalAlignment)
            } shouldBe listOf(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM, HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM)
        }

        test("setting horizontalAlignment to LEFT moves the text to the left of a widened button (the default CENTER is centered)") {
            val (centerLeft, _, centerRight) = textEdgesIn300x100({ WButton("Save") }) { }
            val (left) = textEdgesIn300x100({ WButton("Save") }) { horizontalAlignment = HorizontalAlignment.LEFT }
            ((centerLeft + centerRight) / 2) shouldBe (BUTTON_WIDTH / 2 plusOrMinus 2.0)
            left shouldBeLessThan 20.0
        }

        test("setting horizontalAlignment to RIGHT moves the text to the right of a widened button") {
            val (_, _, right) = textEdgesIn300x100({ WButton("Save") }) { horizontalAlignment = HorizontalAlignment.RIGHT }
            right shouldBeGreaterThan BUTTON_WIDTH - 20.0
        }

        test("setting verticalAlignment to TOP / BOTTOM moves the text to the top / bottom of a taller button") {
            val (_, top) = textEdgesIn300x100({ WButton("Save") }) { verticalAlignment = VerticalAlignment.TOP }
            val (_, _, _, bottom) = textEdgesIn300x100({ WButton("Save") }) { verticalAlignment = VerticalAlignment.BOTTOM }
            top shouldBeLessThan 15.0
            bottom shouldBeGreaterThan BUTTON_HEIGHT - 15.0
        }

        test("setting horizontalAlignment of a WCheckBox to RIGHT moves the text to the right of a widened check box") {
            val (left) = textEdgesIn300x100({ WCheckBox("Notify") }) { }
            val (_, _, right) = textEdgesIn300x100({ WCheckBox("Notify") }) { horizontalAlignment = HorizontalAlignment.RIGHT }
            left shouldBeLessThan BUTTON_WIDTH / 2
            right shouldBeGreaterThan BUTTON_WIDTH - 20.0
        }

        test("for a button with an icon too, setting horizontalAlignment to LEFT moves the icon and text group to the left") {
            val (left) = textEdgesIn300x100({ WButton("Save").also { it.icon = Symbol.SAVE } }) {
                horizontalAlignment = HorizontalAlignment.LEFT
            }
            // The text comes after the icon (20) and the gap
            left shouldBeLessThan 50.0
        }

        test("horizontalAlignment / verticalAlignment do not change the button's position in its parent (...InParent), and vice versa") {
            onUiThreadGet {
                val button = WButton("Save")
                button.horizontalAlignment = HorizontalAlignment.LEFT
                button.verticalAlignment = VerticalAlignment.TOP
                val inParent = button.horizontalAlignmentInParent to button.verticalAlignmentInParent
                button.horizontalAlignmentInParent = HorizontalAlignment.RIGHT
                button.verticalAlignmentInParent = VerticalAlignment.BOTTOM
                inParent to (button.horizontalAlignment to button.verticalAlignment)
            } shouldBe ((HorizontalAlignment.STRETCH to VerticalAlignment.STRETCH) to (HorizontalAlignment.LEFT to VerticalAlignment.TOP))
        }

        test("STRETCH is accepted as a value that stretches the button's content and can be read back (the same as WinUI's content alignment)") {
            onUiThreadGet {
                val button = WButton("Save")
                button.horizontalAlignment = HorizontalAlignment.STRETCH
                button.verticalAlignment = VerticalAlignment.STRETCH
                button.horizontalAlignment to button.verticalAlignment
            } shouldBe (HorizontalAlignment.STRETCH to VerticalAlignment.STRETCH)
        }
    }

    private companion object {
        const val BUTTON_WIDTH = 300.0
        const val BUTTON_HEIGHT = 100.0
    }
}
