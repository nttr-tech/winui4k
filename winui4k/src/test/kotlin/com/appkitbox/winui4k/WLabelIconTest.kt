package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Tests that verify WLabel.icon / horizontalTextPosition / verticalTextPosition / iconTextGap (JLabel's setIcon and so
 * on). Places the label in the shared window, reads the positions of the icon and text after layout, and checks that the
 * arrangement is the same as Swing's JLabel.
 */
class WLabelIconTest : FunSpec() {
    /** The position and size of an element after layout (from the top-left of the shared window's content, in DIP). */
    private data class Box(val x: Double, val y: Double, val width: Double, val height: Double) {
        val right get() = x + width
        val bottom get() = y + height
        val centerX get() = x + width / 2
        val centerY get() = y + height / 2
    }

    /** The positions of the icon and the text. [icon] is null if there is no icon, and [text] is null if no text is placed. */
    private data class Placement(val label: Box, val icon: Box?, val text: Box?, val iconDescription: String?)

    private fun boxOf(element: ComPtr): Box {
        val ui = element.queryInterface(XamlInterop.IID_IUIElement)
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            val position = Xaml.positionInRoot(ui)
            Box(
                position[0],
                position[1],
                framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth),
                framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight),
            )
        } finally {
            ui.release()
            framework.release()
        }
    }

    /** Collects the elements of the visual tree under [element] depth-first (the caller releases them). */
    private fun descendants(element: ComPtr, into: MutableList<ComPtr>) {
        for (index in 0 until Xaml.childCount(element)) {
            val child = Xaml.childAt(element, index) ?: continue
            into += child
            // Do not look inside icons (such as the TextBlock in the SymbolIcon template)
            if (!IconElements.isIconElement(child)) descendants(child, into)
        }
    }

    /** Places [label], lays it out and reads the positions of the icon and text. */
    private fun placementOf(label: WLabel): Placement {
        UiTestHarness.attachAndAwaitLoaded(label)
        try {
            return onUiThreadGet {
                label.updateLayout()
                val elements = mutableListOf<ComPtr>()
                descendants(label.inspectable, elements)
                try {
                    val icon = elements.firstOrNull { IconElements.isIconElement(it) }
                    val text = elements.firstOrNull { Xaml.sameObject(it, label.textBlock) }
                    Placement(boxOf(label.inspectable), icon?.let { boxOf(it) }, text?.let { boxOf(it) }, icon?.let { IconElements.describe(it) })
                } finally {
                    elements.forEach { it.release() }
                }
            }
        } finally {
            UiTestHarness.detach(label)
        }
    }

    /** A label placed at the top-left without a set width (it takes the size of its content). */
    private fun compactLabel(text: String, icon: WIcon?, configure: WLabel.() -> Unit = {}): WLabel = onUiThreadGet {
        WLabel(text, icon).also {
            it.horizontalAlignment = HorizontalAlignment.LEFT
            it.verticalAlignment = VerticalAlignment.TOP
            it.configure()
        }
    }

    init {
        test("by default there is no icon, the text is after the icon (TRAILING), vertically centered (CENTER), with a spacing of 4 (same as Swing's JLabel)") {
            onUiThreadGet {
                val label = WLabel("Name")
                listOf(label.icon, label.horizontalTextPosition, label.verticalTextPosition, label.iconTextGap)
            } shouldBe listOf(null, HorizontalTextPosition.TRAILING, VerticalTextPosition.CENTER, 4.0)
        }

        test("with the text and icon constructor, the text is placed to the right of the icon with a spacing of 4, and their vertical centers align") {
            val placement = placementOf(compactLabel("Save now", Symbol.SAVE))
            val icon = checkNotNull(placement.icon)
            val text = checkNotNull(placement.text)
            placement.iconDescription shouldBe "Symbol:SAVE"
            text.x shouldBe (icon.right + 4.0 plusOrMinus TOLERANCE)
            text.centerY shouldBe (icon.centerY plusOrMinus TOLERANCE)
            placement.label.width shouldBe (icon.width + 4.0 + text.width plusOrMinus TOLERANCE)
        }

        test("with LEADING the icon is placed to the right of the text with spacing, and even in a wide label the icon is next to the text (not pushed to the right edge)") {
            val label = onUiThreadGet {
                WLabel("Save now", Symbol.SAVE).also {
                    it.horizontalTextPosition = HorizontalTextPosition.LEADING
                    it.iconTextGap = 10.0
                    it.width = 400.0
                    it.horizontalAlignment = HorizontalAlignment.LEFT
                }
            }
            val placement = placementOf(label)
            val icon = checkNotNull(placement.icon)
            val text = checkNotNull(placement.text)
            text.x shouldBe (placement.label.x plusOrMinus TOLERANCE)
            icon.x shouldBe (text.right + 10.0 plusOrMinus TOLERANCE)
            icon.right shouldBeLessThan placement.label.right - 100.0
        }

        test("with CENTER + TOP the text is above the icon, with CENTER + BOTTOM it is below with spacing, and their horizontal centers align") {
            val above = placementOf(
                compactLabel("Top", Symbol.SAVE) {
                    horizontalTextPosition = HorizontalTextPosition.CENTER
                    verticalTextPosition = VerticalTextPosition.TOP
                },
            )
            val aboveIcon = checkNotNull(above.icon)
            val aboveText = checkNotNull(above.text)
            aboveIcon.y shouldBe (aboveText.bottom + 4.0 plusOrMinus TOLERANCE)
            aboveText.centerX shouldBe (aboveIcon.centerX plusOrMinus TOLERANCE)

            val below = placementOf(
                compactLabel("Bottom", Symbol.SAVE) {
                    horizontalTextPosition = HorizontalTextPosition.CENTER
                    verticalTextPosition = VerticalTextPosition.BOTTOM
                },
            )
            val belowIcon = checkNotNull(below.icon)
            val belowText = checkNotNull(below.text)
            belowText.y shouldBe (belowIcon.bottom + 4.0 plusOrMinus TOLERANCE)
            belowText.centerX shouldBe (belowIcon.centerX plusOrMinus TOLERANCE)
        }

        test("with CENTER + CENTER the text overlaps the icon and their centers align (same as Swing)") {
            val placement = placementOf(compactLabel("Overlay", Symbol.SAVE) { horizontalTextPosition = HorizontalTextPosition.CENTER })
            val icon = checkNotNull(placement.icon)
            val text = checkNotNull(placement.text)
            text.centerX shouldBe (icon.centerX plusOrMinus TOLERANCE)
            text.centerY shouldBe (icon.centerY plusOrMinus TOLERANCE)
        }

        test("when laid out side by side with TOP / BOTTOM, the text aligns to the top / bottom within the icon's height") {
            val big = WFontIcon("\uE74E")
            val top = placementOf(
                compactLabel("Top", big) {
                    verticalTextPosition = VerticalTextPosition.TOP
                    font = WFont("Segoe UI", WFont.PLAIN, 8.0)
                },
            )
            checkNotNull(top.text).y shouldBe (checkNotNull(top.icon).y plusOrMinus TOLERANCE)
            val bottom = placementOf(
                compactLabel("Bottom", big) {
                    verticalTextPosition = VerticalTextPosition.BOTTOM
                    font = WFont("Segoe UI", WFont.PLAIN, 8.0)
                },
            )
            checkNotNull(bottom.text).bottom shouldBe (checkNotNull(bottom.icon).bottom plusOrMinus TOLERANCE)
        }

        test("with empty text there is only the icon and no spacing (the label is the size of the icon)") {
            val placement = placementOf(onUiThreadGet { WLabel(Symbol.SAVE).also { it.horizontalAlignment = HorizontalAlignment.LEFT } })
            val icon = checkNotNull(placement.icon)
            placement.text shouldBe null
            placement.label.width shouldBe (icon.width plusOrMinus TOLERANCE)
        }

        test("putting text in an icon-only label lays them out together, and emptying it returns to only the icon") {
            val label = onUiThreadGet { WLabel(Symbol.SAVE).also { it.horizontalAlignment = HorizontalAlignment.LEFT } }
            onUiThreadGet { label.text = "Save" }
            val withText = placementOf(label)
            checkNotNull(withText.text).x shouldBe (checkNotNull(withText.icon).right + 4.0 plusOrMinus TOLERANCE)
            onUiThreadGet { label.text = "" }
            placementOf(label).text shouldBe null
        }

        test("resetting icon to null leaves only the text, and the text and label sizes are the same as with no icon from the start") {
            val plain = placementOf(compactLabel("Save now", null))
            val restored = placementOf(
                compactLabel("Save now", Symbol.SAVE) {
                    icon = null
                },
            )
            restored.icon shouldBe null
            (restored.label.width to restored.label.height) shouldBe (plain.label.width to plain.label.height)
        }

        test("when the width is insufficient, the icon is not shrunk and the text side is trimmed with textTrimming") {
            val label = onUiThreadGet {
                WLabel("a very long label string", Symbol.SAVE).also {
                    it.textTrimming = TextTrimming.CHARACTER_ELLIPSIS
                    it.width = 80.0
                    it.horizontalAlignment = HorizontalAlignment.LEFT
                }
            }
            UiTestHarness.attachAndAwaitLoaded(label)
            try {
                onUiThreadGet {
                    label.updateLayout()
                    label.textBlock.getBool(XamlInterop.ITextBlock_get_IsTextTrimmed)
                } shouldBe true
            } finally {
                UiTestHarness.detach(label)
            }
            val placement = placementOf(label)
            checkNotNull(placement.icon).width shouldBe (20.0 plusOrMinus TOLERANCE) // the size of SymbolIcon
            checkNotNull(placement.text).right shouldBeLessThan placement.label.right + TOLERANCE
        }

        test("with an icon, the group of icon and text moves toward the textAlignment direction (with RIGHT the text's right edge is the label's right edge)") {
            val placement = placementOf(
                onUiThreadGet {
                    WLabel("Right", Symbol.SAVE).also {
                        it.width = 300.0
                        it.horizontalAlignment = HorizontalAlignment.LEFT
                        it.textAlignment = TextAlignment.RIGHT
                    }
                },
            )
            val icon = checkNotNull(placement.icon)
            icon.x shouldBeGreaterThan placement.label.x + 200.0
            checkNotNull(placement.text).right shouldBe (placement.label.right plusOrMinus TOLERANCE)
        }

        test("WImageIcon and WFontIcon can also be laid out") {
            onUiThreadGet { WLabel("Image", WImageIcon("C:/icons/open.png")) }.let { placementOf(it) }.iconDescription shouldBe
                "Image:file:///C:/icons/open.png"
            onUiThreadGet { WLabel("Star", WFontIcon("★", "Segoe UI Symbol")) }.let { placementOf(it) }.iconDescription shouldBe
                "Font:★/Segoe UI Symbol"
        }

        test("a negative value or NaN cannot be set to iconTextGap, and the value is unchanged") {
            val (negative, nan, gap) = onUiThreadGet {
                val label = WLabel("Spacing")
                label.iconTextGap = 6.0
                Triple(
                    runCatching { label.iconTextGap = -1.0 }.exceptionOrNull(),
                    runCatching { label.iconTextGap = Double.NaN }.exceptionOrNull(),
                    label.iconTextGap,
                )
            }
            (negative is IllegalArgumentException) shouldBe true
            (nan is IllegalArgumentException) shouldBe true
            gap shouldBe 6.0
        }
    }

    private companion object {
        /** The tolerance (DIP) allowing for layout rounding (rounding of positions to the display scale). */
        const val TOLERANCE = 1.0
    }
}
