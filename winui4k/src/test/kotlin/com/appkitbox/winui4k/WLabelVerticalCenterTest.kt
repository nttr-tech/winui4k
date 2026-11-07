package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the vertical position of the text of a stretched WLabel and of components on a WCommandBar (display issue 14 in doc/swing_to_winui4k_issues.md).
 * JLabel's default vertical alignment (verticalAlignmentInParent) is center, so the text of a label placed in a tall area appears vertically centered. The components of a JToolBar
 * are also lined up centered vertically.
 */
@Tags("E2E")
class WLabelVerticalCenterTest : FunSpec() {
    /** The top edge of [element] measured from the top left of [origin], and its height. */
    private fun verticalSpan(element: ComPtr, origin: ComPtr): Pair<Double, Double> {
        val top = Xaml.positionInRoot(element)[1] - Xaml.positionInRoot(origin)[1]
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            top to framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight)
        } finally {
            framework.release()
        }
    }

    /** The vertical center of [component] measured from the top left of [origin]. */
    private fun centerY(component: WComponent, origin: WComponent): Double {
        val (top, height) = verticalSpan(component.uiElement, origin.uiElement)
        return top + height / 2
    }

    /** Shows [label] in CENTER of a 300 x 200 [WBorderLayout] panel. */
    private fun showInCenter(label: WLabel): WLayoutPanel {
        val panel = onUiThreadGet {
            WLayoutPanel(WBorderLayout()).also {
                it.width = 300.0
                it.height = 200.0
                it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                it.verticalAlignmentInParent = VerticalAlignment.TOP
                it.add(label, WBorderLayout.CENTER)
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
        test("the text of a label stretched in CENTER of a BorderLayout appears vertically centered, not at the top (the same as JLabel's default)") {
            val label = onUiThreadGet { WLabel("Select a file") }
            val panel = showInCenter(label)
            try {
                eventually(3.seconds) { onUiThreadGet { label.actualHeight } shouldBe (200.0 plusOrMinus 0.5) }
                val (top, height) = onUiThreadGet { verticalSpan(label.textBlock, label.uiElement) }
                height shouldBeGreaterThan 0.0
                (top + height / 2) shouldBe (100.0 plusOrMinus 1.0)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("in a stretched label with an icon, the group of the icon and text also appears vertically centered") {
            val label = onUiThreadGet { WLabel("Save", Symbol.SAVE) }
            val panel = showInCenter(label)
            try {
                eventually(3.seconds) { onUiThreadGet { label.actualHeight } shouldBe (200.0 plusOrMinus 0.5) }
                val (top, height) = onUiThreadGet { verticalSpan(label.textBlock, label.uiElement) }
                (top + height / 2) shouldBe (100.0 plusOrMinus 1.0)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("the text position of a label that is not stretched (placed at its preferred height) does not change (the top is 0)") {
            val label = onUiThreadGet { WLabel("Name:") }
            val panel = onUiThreadGet {
                WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also {
                    it.width = 300.0
                    it.height = 200.0
                    it.add(label)
                }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread {
                    panel.validate()
                    panel.updateLayout()
                }
                val (top, height) = onUiThreadGet { verticalSpan(label.textBlock, label.uiElement) }
                top shouldBe (0.0 plusOrMinus 0.5)
                height shouldBe (onUiThreadGet { label.actualHeight } plusOrMinus 0.5)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a label and a combo box on a toolbar (WCommandBar) are aligned at their vertical centers") {
            val (bar, label, combo) = onUiThreadGet {
                val bar = WCommandBar()
                val label = WLabel("Font: ")
                val combo = WComboBox(arrayOf("Segoe UI", "Consolas"))
                bar.add(label)
                bar.add(combo)
                Triple(bar, label, combo)
            }
            UiTestHarness.attachAndAwaitLoaded(bar)
            try {
                eventually(3.seconds) { onUiThreadGet { combo.actualHeight } shouldBeGreaterThan 0.0 }
                onUiThread { bar.updateLayout() }
                val (labelCenter, comboCenter) = onUiThreadGet { centerY(label, bar) to centerY(combo, bar) }
                labelCenter shouldBe (comboCenter plusOrMinus 1.0)
            } finally {
                UiTestHarness.detach(bar)
            }
        }
    }
}
