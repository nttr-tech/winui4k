package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify WLabel.horizontalAlignment / verticalAlignment (the alignment of the text and icon group inside the label, the same as
 * JLabel.setHorizontalAlignment / setVerticalAlignment). They are separate from the position of the label itself in its parent (horizontalAlignmentInParent etc.),
 * and the tests also check they do not affect each other.
 */
@Tags("E2E")
class WLabelAlignmentTest : FunSpec() {
    /** The left edge of the first character of the TextBlock [element] (from the TextBlock's top left; TextPointer.GetCharacterRect). */
    private fun firstCharacterX(element: ComPtr): Double {
        val textBlock = element.queryInterface(XamlInterop.IID_ITextBlock)
        val start = try {
            textBlock.getPtr(XamlInterop.ITextBlock_get_ContentStart)
        } finally {
            textBlock.release()
        }
        return try {
            Ffi.backend.withScope { scope ->
                val rect = scope.allocate(RECT_BYTES, FLOAT_ALIGNMENT) // Windows.Foundation.Rect { float X, Y, Width, Height }
                start.call(XamlInterop.ITextPointer_GetCharacterRect, XamlInterop.LogicalDirection_Forward, rect)
                Ffi.backend.memory.getFloat(rect, 0).toDouble()
            }
        } finally {
            start.release()
        }
    }

    /** The left, top, width and height of [element], measured from the top left of [origin]. */
    private fun boundsIn(element: ComPtr, origin: ComPtr): List<Double> {
        val position = Xaml.positionInRoot(element)
        val originPosition = Xaml.positionInRoot(origin)
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            listOf(
                position[0] - originPosition[0],
                position[1] - originPosition[1],
                framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth),
                framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight),
            )
        } finally {
            framework.release()
        }
    }

    /** Shows a 300-wide label at the top left and reads the left edge of the first character of the text (from the label's left edge). */
    private fun firstCharacterXIn300(configure: WLabel.() -> Unit): Double {
        val label = onUiThreadGet {
            WLabel("Right").also {
                it.width = LABEL_WIDTH
                it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                it.configure()
            }
        }
        UiTestHarness.attachAndAwaitLoaded(label)
        try {
            return onUiThreadGet {
                label.updateLayout()
                firstCharacterX(label.textBlock)
            }
        } finally {
            UiTestHarness.detach(label)
        }
    }

    /** Shows [label] in CENTER of a 300 x 200 [WBorderLayout] panel and returns the result of [measure]. */
    private suspend fun <T> inStretchedCenter(label: WLabel, measure: () -> T): T {
        val panel = onUiThreadGet {
            WLayoutPanel(WBorderLayout()).also {
                it.width = LABEL_WIDTH
                it.height = LABEL_HEIGHT
                it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                it.verticalAlignmentInParent = VerticalAlignment.TOP
                it.add(label, WBorderLayout.CENTER)
            }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            onUiThread {
                panel.revalidate()
                panel.validate()
                panel.updateLayout()
            }
            eventually(3.seconds) { onUiThreadGet { label.actualHeight } shouldBe (LABEL_HEIGHT plusOrMinus 0.5) }
            return onUiThreadGet(measure)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    /** The Grid in the label arranging the icon and the text (null if there is no icon). */
    private fun iconGroupOf(label: WLabel): ComPtr? = Xaml.childAt(label.uiElement, 0)?.let { child ->
        if (Xaml.sameObject(child, label.textBlock)) {
            child.release()
            null
        } else {
            child
        }
    }

    init {
        test("the default horizontalAlignment is LEFT and verticalAlignment is CENTER (corresponding to JLabel's LEADING and CENTER)") {
            onUiThreadGet {
                val label = WLabel("Name")
                label.horizontalAlignment to label.verticalAlignment
            } shouldBe (HorizontalAlignment.LEFT to VerticalAlignment.CENTER)
        }

        test("setting horizontalAlignment to RIGHT moves the text to the right edge of a widened label") {
            val left = firstCharacterXIn300 { }
            val right = firstCharacterXIn300 { horizontalAlignment = HorizontalAlignment.RIGHT }
            left shouldBe (0.0 plusOrMinus 1.0)
            right shouldBeGreaterThan LABEL_WIDTH - 30.0
        }

        test("setting horizontalAlignment to CENTER puts the text at the horizontal center of a widened label") {
            val center = firstCharacterXIn300 { horizontalAlignment = HorizontalAlignment.CENTER }
            center shouldBeGreaterThan LABEL_WIDTH / 2 - 20.0
            center shouldBeLessThan LABEL_WIDTH / 2
        }

        test("horizontalAlignment and textAlignment represent the same alignment (setting one can be read with the other)") {
            onUiThreadGet {
                val label = WLabel("Name")
                label.horizontalAlignment = HorizontalAlignment.RIGHT
                val text = label.textAlignment
                label.textAlignment = TextAlignment.CENTER
                val horizontal = label.horizontalAlignment
                label.textAlignment = TextAlignment.JUSTIFY
                listOf(text, horizontal, label.horizontalAlignment)
            } shouldBe listOf(TextAlignment.RIGHT, HorizontalAlignment.CENTER, HorizontalAlignment.LEFT)
        }

        test("horizontalAlignment does not change the label's position in its parent (horizontalAlignmentInParent), and vice versa") {
            onUiThreadGet {
                val label = WLabel("Name")
                label.horizontalAlignment = HorizontalAlignment.RIGHT
                val inParent = label.horizontalAlignmentInParent
                label.horizontalAlignmentInParent = HorizontalAlignment.CENTER
                label.verticalAlignment = VerticalAlignment.TOP
                listOf(inParent, label.horizontalAlignment, label.verticalAlignmentInParent, label.verticalAlignment)
            } shouldBe listOf(HorizontalAlignment.STRETCH, HorizontalAlignment.RIGHT, VerticalAlignment.STRETCH, VerticalAlignment.TOP)
        }

        test("with an icon, setting horizontalAlignment to RIGHT moves the icon and text group to the right edge of the label") {
            val label = onUiThreadGet {
                WLabel("Right", Symbol.SAVE).also {
                    it.width = LABEL_WIDTH
                    it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                    it.horizontalAlignment = HorizontalAlignment.RIGHT
                }
            }
            UiTestHarness.attachAndAwaitLoaded(label)
            try {
                val (x, _, width) = onUiThreadGet {
                    label.updateLayout()
                    val group = checkNotNull(iconGroupOf(label))
                    try {
                        boundsIn(group, label.uiElement)
                    } finally {
                        group.release()
                    }
                }
                x shouldBeGreaterThan LABEL_WIDTH / 2
                (x + width) shouldBe (LABEL_WIDTH plusOrMinus 1.0)
            } finally {
                UiTestHarness.detach(label)
            }
        }

        test("setting verticalAlignment to TOP shows the text at the top of a vertically stretched label") {
            val label = onUiThreadGet { WLabel("Top").also { it.verticalAlignment = VerticalAlignment.TOP } }
            val (_, top, _, height) = inStretchedCenter(label) { boundsIn(label.textBlock, label.uiElement) }
            height shouldBeGreaterThan 0.0
            top shouldBe (0.0 plusOrMinus 1.0)
        }

        test("setting verticalAlignment to BOTTOM shows the text at the bottom of a vertically stretched label") {
            val label = onUiThreadGet { WLabel("Bottom").also { it.verticalAlignment = VerticalAlignment.BOTTOM } }
            val (_, top, _, height) = inStretchedCenter(label) { boundsIn(label.textBlock, label.uiElement) }
            (top + height) shouldBe (LABEL_HEIGHT plusOrMinus 1.0)
        }

        test("changing verticalAlignment after showing makes the vertical position of the text follow") {
            val label = onUiThreadGet { WLabel("Later") }
            val (centerTop, topTop) = inStretchedCenter(label) {
                val before = boundsIn(label.textBlock, label.uiElement)[1]
                label.verticalAlignment = VerticalAlignment.TOP
                label.updateLayout()
                before to boundsIn(label.textBlock, label.uiElement)[1]
            }
            centerTop shouldBeGreaterThan 50.0
            topTop shouldBe (0.0 plusOrMinus 1.0)
        }

        test("with an icon, setting verticalAlignment to TOP shows the icon and text group at the top of the label (the same when the icon is added later)") {
            val label = onUiThreadGet {
                WLabel("Top").also {
                    it.verticalAlignment = VerticalAlignment.TOP
                    it.icon = Symbol.SAVE
                }
            }
            val top = inStretchedCenter(label) {
                val group = checkNotNull(iconGroupOf(label))
                try {
                    boundsIn(group, label.uiElement)[1]
                } finally {
                    group.release()
                }
            }
            top shouldBe (0.0 plusOrMinus 1.0)
        }

        test("STRETCH cannot be used for content alignment and throws IllegalArgumentException, and the value does not change") {
            val (horizontal, vertical, values) = onUiThreadGet {
                val label = WLabel("Name")
                label.horizontalAlignment = HorizontalAlignment.CENTER
                label.verticalAlignment = VerticalAlignment.BOTTOM
                Triple(
                    runCatching { label.horizontalAlignment = HorizontalAlignment.STRETCH }.exceptionOrNull(),
                    runCatching { label.verticalAlignment = VerticalAlignment.STRETCH }.exceptionOrNull(),
                    label.horizontalAlignment to label.verticalAlignment,
                )
            }
            horizontal.shouldBeInstanceOf<IllegalArgumentException>()
            vertical.shouldBeInstanceOf<IllegalArgumentException>()
            values shouldBe (HorizontalAlignment.CENTER to VerticalAlignment.BOTTOM)
        }
    }

    private companion object {
        const val LABEL_WIDTH = 300.0
        const val LABEL_HEIGHT = 200.0
        const val RECT_BYTES = 16L
        const val FLOAT_ALIGNMENT = 4L
    }
}
