package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Tests for how the text of a WCheckBox under a layout manager appears (display issue 9 in doc/swing_to_winui4k_issues.md).
 * The text in the CheckBox template (ContentPresenter) has TextWrapping = Wrap, so if the assigned width is even slightly narrower than the preferred width,
 * the last word goes to a second line and is cut off by the one-line height.
 */
@Tags("E2E")
class WCheckBoxLayoutTest : FunSpec() {
    /** Shows buttons of [texts] (made with [create]) in a [WFlowLayout] panel and waits until they are laid out. */
    private fun <T : WToggleButton> showInFlow(texts: List<String>, create: (String) -> T): Pair<WLayoutPanel, List<T>> {
        val (panel, boxes) = onUiThreadGet {
            val boxes = texts.map(create)
            val panel = WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also {
                it.width = 2000.0
                it.height = 400.0
                it.horizontalAlignment = HorizontalAlignment.LEFT
                it.verticalAlignment = VerticalAlignment.TOP
                boxes.forEach(it::add)
            }
            panel to boxes
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            panel.revalidate()
            panel.validate()
            panel.updateLayout()
        }
        return panel to boxes
    }

    init {
        test("the text of a check box without a given width appears on one line without wrapping for any text (the text height is the same as for one word)") {
            assertSingleLine(::WCheckBox)
        }

        test("the text of a radio button without a given width also appears on one line without wrapping for any text (the template is built the same way as CheckBox)") {
            assertSingleLine(::WRadioButton)
        }

        test("the preferred width of a check box is not narrower than the text width (only 1 DIP of slack is added)") {
            val (panel, boxes) = showInFlow(listOf("I agree to the terms of use"), ::WCheckBox)
            try {
                val preferred = onUiThreadGet { boxes[0].preferredSize().width }
                // The difference from the preferred width measured with an explicit width (without slack) is 1 DIP
                val plain = onUiThreadGet {
                    boxes[0].clearLayoutSizeForMeasure()
                    panel.updateLayout()
                    XamlStructs.getSizeFloat(boxes[0].uiElement, XamlInterop.IUIElement_get_DesiredSize)[0]
                }
                preferred - plain shouldBe (1.0 plusOrMinus 0.01)
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }

    /** Lines up the one-word "I" and buttons of [TEXTS], and checks that every text has the same height as "I" (does not wrap). */
    private fun <T : WToggleButton> assertSingleLine(create: (String) -> T) {
        val (panel, boxes) = showInFlow(listOf("I") + TEXTS, create)
        try {
            val heights = onUiThreadGet { boxes.map { textHeightOf(it) } }
            val reference = heights.first()
            TEXTS.forEachIndexed { i, text ->
                withClue("height of \"$text\" (width ${onUiThreadGet { boxes[i + 1].actualWidth }})") {
                    heights[i + 1] shouldBe (reference plusOrMinus 0.5)
                }
            }
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    /** The height the button's text (the template's ContentPresenter) asks for with the assigned width (two lines if it wraps). */
    private fun textHeightOf(box: WToggleButton): Double {
        val presenter = checkNotNull(Xaml.templatePartOrNull(box.inspectable, "ContentPresenter")) { "no ContentPresenter" }
        return try {
            val element = presenter.queryInterface(XamlInterop.IID_IUIElement)
            try {
                XamlStructs.getSizeFloat(element, XamlInterop.IUIElement_get_DesiredSize)[1]
            } finally {
                element.release()
            }
        } finally {
            presenter.release()
        }
    }

    private companion object {
        /** English sentences (with many word breaks, which tend to give fractional widths) and "Disabled check", which was cut in a trial. */
        val TEXTS = listOf(
            "I agree to the terms of use",
            "Disabled check",
            "Subscribe to the newsletter",
            "Send me the newsletter",
            "Remember me",
            "Show hidden files and folders",
            "Enable automatic updates",
            "Wi-Fi",
            "Bold",
            "Italic text style",
            "Use a proxy server for your LAN",
            "Ask before closing multiple tabs",
        )
    }
}
