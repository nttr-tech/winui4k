package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Verifies WButton.icon / horizontalTextPosition / iconTextGap (such as AbstractButton's setIcon).
 * Reads the contents of the button's Content with [contentOf] and checks the order, orientation, and spacing of the
 * icon and text.
 */
class WButtonIconTest : FunSpec() {
    /**
     * Represents the button's Content as a list of strings. A string Content is "String:<string>", an IconElement is
     * [IconElements.describe], and a StackPanel is "Panel:<orientation>:<spacing>" followed by its children (a TextBlock is
     * "TextBlock:<string>"). Empty if there is no Content.
     */
    private fun contentOf(button: WButtonBase): List<String> {
        val contentControl = button.inspectable.queryInterface(XamlInterop.IID_IContentControl)
        val content = try {
            contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content)
        } finally {
            contentControl.release()
        } ?: return emptyList()
        return try {
            PropertyValues.unboxString(content)?.let { return listOf("String:$it") }
            val panel = content.queryInterfaceOrNull(XamlInterop.IID_IStackPanel) ?: return listOf(describeElement(content))
            try {
                val orientation = Orientation.of(panel.getInt(XamlInterop.IStackPanel_get_Orientation))
                val spacing = panel.getDouble(XamlInterop.IStackPanel_get_Spacing)
                listOf("Panel:$orientation:$spacing") + childrenOf(panel).map { child ->
                    try {
                        describeElement(child)
                    } finally {
                        child.release()
                    }
                }
            } finally {
                panel.release()
            }
        } finally {
            content.release()
        }
    }

    private fun childrenOf(stackPanel: ComPtr): List<ComPtr> {
        val panel = stackPanel.queryInterface(XamlInterop.IID_IPanel)
        val children = try {
            panel.getPtr(XamlInterop.IPanel_get_Children)
        } finally {
            panel.release()
        }
        return try {
            (0 until children.getInt(FoundationInterop.IVector_get_Size)).map { children.getPtr(FoundationInterop.IVector_GetAt, it) }
        } finally {
            children.release()
        }
    }

    private fun describeElement(element: ComPtr): String {
        val textBlock = element.queryInterfaceOrNull(XamlInterop.IID_ITextBlock) ?: return IconElements.describe(element)
        return try {
            "TextBlock:" + textBlock.getString(XamlInterop.ITextBlock_get_Text)
        } finally {
            textBlock.release()
        }
    }

    /** The effective foreground color of the TextBlock inside the Content's StackPanel. */
    private fun textBlockForeground(button: WButton): WColor? {
        val contentControl = button.inspectable.queryInterface(XamlInterop.IID_IContentControl)
        val content = try {
            contentControl.getPtr(XamlInterop.IContentControl_get_Content)
        } finally {
            contentControl.release()
        }
        val children = try {
            childrenOf(content)
        } finally {
            content.release()
        }
        try {
            val textBlock = children.firstNotNullOf { it.queryInterfaceOrNull(XamlInterop.IID_ITextBlock) }
            try {
                val brush = textBlock.getPtrOrNull(XamlInterop.ITextBlock_get_Foreground) ?: return null
                return try {
                    colorOf(brush)
                } finally {
                    brush.release()
                }
            } finally {
                textBlock.release()
            }
        } finally {
            children.forEach { it.release() }
        }
    }

    init {
        test("a button with text and an icon becomes a horizontal StackPanel (spacing 4) with the icon then the text, and text returns the text") {
            val (content, text, icon) = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                Triple(contentOf(button), button.text, button.icon)
            }
            content shouldContainExactly listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save")
            text shouldBe "Save"
            icon shouldBe Symbol.SAVE
        }

        test("an icon-only button has only the IconElement as Content and text is an empty string") {
            val (content, text) = onUiThreadGet {
                val button = WButton(Symbol.DELETE)
                contentOf(button) to button.text
            }
            content shouldContainExactly listOf("Symbol:DELETE")
            text shouldBe ""
        }

        test("a button with no icon set has a null icon and its Content stays a string") {
            val (icon, content) = onUiThreadGet {
                val button = WButton("OK", null)
                button.icon to contentOf(button)
            }
            icon shouldBe null
            content shouldContainExactly listOf("String:OK")
        }

        test("WImageIcon and WFontIcon are also placed before the text") {
            val (image, font) = onUiThreadGet {
                contentOf(WButton("Open", WImageIcon("C:/icons/open.png"))) to contentOf(WButton("Star", WFontIcon("★", "Segoe UI Symbol")))
            }
            image shouldContainExactly listOf("Panel:HORIZONTAL:4.0", "Image:file:///C:/icons/open.png", "TextBlock:Open")
            font shouldContainExactly listOf("Panel:HORIZONTAL:4.0", "Font:★/Segoe UI Symbol", "TextBlock:Star")
        }

        test("horizontalTextPosition is TRAILING by default, and LEADING, TOP, and BOTTOM change the order and orientation") {
            val results = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                val initial = button.horizontalTextPosition
                val layouts = listOf(TextPosition.LEADING, TextPosition.TOP, TextPosition.BOTTOM, TextPosition.TRAILING).map {
                    button.horizontalTextPosition = it
                    button.horizontalTextPosition to contentOf(button)
                }
                initial to layouts
            }
            results.first shouldBe TextPosition.TRAILING
            results.second shouldContainExactly listOf(
                TextPosition.LEADING to listOf("Panel:HORIZONTAL:4.0", "TextBlock:Save", "Symbol:SAVE"),
                TextPosition.TOP to listOf("Panel:VERTICAL:4.0", "TextBlock:Save", "Symbol:SAVE"),
                TextPosition.BOTTOM to listOf("Panel:VERTICAL:4.0", "Symbol:SAVE", "TextBlock:Save"),
                TextPosition.TRAILING to listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save"),
            )
        }

        test("horizontalTextPosition and iconTextGap chosen before setting the icon also take effect once the icon is set") {
            onUiThreadGet {
                val button = WButton("Save")
                button.horizontalTextPosition = TextPosition.LEADING
                button.iconTextGap = 10.0
                val beforeIcon = contentOf(button)
                button.icon = Symbol.SAVE
                beforeIcon to contentOf(button)
            } shouldBe (listOf("String:Save") to listOf("Panel:HORIZONTAL:10.0", "TextBlock:Save", "Symbol:SAVE"))
        }

        test("iconTextGap is 4 by default, the value set becomes the StackPanel spacing, and 0 is allowed") {
            val (initial, eight, zero) = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                val initial = button.iconTextGap
                button.iconTextGap = 8.0
                val eight = button.iconTextGap to contentOf(button).first()
                button.iconTextGap = 0.0
                Triple(initial, eight, button.iconTextGap to contentOf(button).first())
            }
            initial shouldBe 4.0
            eight shouldBe (8.0 to "Panel:HORIZONTAL:8.0")
            zero shouldBe (0.0 to "Panel:HORIZONTAL:0.0")
        }

        test("setting iconTextGap to a negative value, NaN, or infinity throws IllegalArgumentException and leaves the value unchanged") {
            val gaps = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                button.iconTextGap = 6.0
                listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).map { gap ->
                    shouldThrow<IllegalArgumentException> { button.iconTextGap = gap }
                    button.iconTextGap to contentOf(button).first()
                }
            }
            gaps shouldContainExactly List(3) { 6.0 to "Panel:HORIZONTAL:6.0" }
        }

        test("changing text while the icon is present changes the TextBlock text; empty leaves only the icon, and restoring lines them up again") {
            val steps = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                button.text = "Overwrite"
                val renamed = button.text to contentOf(button)
                button.text = ""
                val empty = button.text to contentOf(button)
                button.text = "Save"
                listOf(renamed, empty, button.text to contentOf(button))
            }
            steps shouldContainExactly listOf(
                "Overwrite" to listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Overwrite"),
                "" to listOf("Symbol:SAVE"),
                "Save" to listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save"),
            )
        }

        test("setting text first and icon second gives the same arrangement as setting icon first and text second") {
            val (textFirst, iconFirst) = onUiThreadGet {
                val a = WButton()
                a.text = "Save"
                a.icon = Symbol.SAVE
                val b = WButton()
                b.icon = Symbol.SAVE
                b.text = "Save"
                contentOf(a) to contentOf(b)
            }
            textFirst shouldContainExactly listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save")
            iconFirst shouldContainExactly textFirst
        }

        test("changing icon to another icon replaces only the icon and keeps the text") {
            onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                button.icon = Symbol.ACCEPT
                button.text to contentOf(button)
            } shouldBe ("Save" to listOf("Panel:HORIZONTAL:4.0", "Symbol:ACCEPT", "TextBlock:Save"))
        }

        test("setting icon back to null returns Content to just the string and keeps text") {
            val (content, text, icon) = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                button.icon = null
                Triple(contentOf(button), button.text, button.icon)
            }
            content shouldContainExactly listOf("String:Save")
            text shouldBe "Save"
            icon shouldBe null
        }

        test("putting a component in content removes the icon, and setting icon later removes the content and leaves only the icon") {
            val result = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                val label = WLabel("Content")
                button.content = label
                val withContent = listOf(button.icon, button.text, button.content === label)
                button.icon = Symbol.SAVE
                val withIcon = listOf(button.icon, button.text, button.content)
                button.text = "Save"
                Triple(withContent, withIcon, contentOf(button))
            }
            result.first shouldContainExactly listOf(null, "", true)
            result.second shouldContainExactly listOf(Symbol.SAVE, "", null)
            result.third shouldContainExactly listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save")
        }

        test("setting icon to null while content is set leaves content as is") {
            val (label, content) = onUiThreadGet {
                val button = WButton()
                val label = WLabel("Content")
                button.content = label
                button.icon = null
                label to button.content
            }
            content shouldBeSameInstanceAs label
        }

        test("while the icon and text are lined up, accessibleName is the text, and removing the icon returns it to empty") {
            val names = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                val composite = button.accessibleName
                button.text = "Overwrite"
                val renamed = button.accessibleName
                button.text = ""
                val iconOnly = button.accessibleName
                button.text = "Save"
                button.icon = null
                listOf(composite, renamed, iconOnly, button.accessibleName)
            }
            names shouldContainExactly listOf("Save", "Overwrite", "", "")
        }

        test("an accessibleName set by the user is not overwritten when the icon is added or removed") {
            val names = onUiThreadGet {
                val button = WButton("Save")
                button.accessibleName = "Save file"
                button.icon = Symbol.SAVE
                val withIcon = button.accessibleName
                button.icon = null
                withIcon to button.accessibleName
            }
            names shouldBe ("Save file" to "Save file")
        }

        test("the default actionCommand of a button with an icon is its text, and it is also in the doClick event") {
            val (command, events) = onUiThreadGet {
                val button = WButton("Save", Symbol.SAVE)
                val events = mutableListOf<String?>()
                button.addActionListener { events += it.actionCommand }
                button.doClick()
                button.actionCommand to events
            }
            command shouldBe "Save"
            events shouldContainExactly listOf("Save")
        }

        test("setting foreground after display also gives the lined-up TextBlock that text color (inherited from the ContentPresenter)") {
            val button = onUiThreadGet { WButton("Save", Symbol.SAVE) }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThreadGet {
                    button.foreground = WColor(0, 0, 160)
                    button.uiElement.call(XamlInterop.IUIElement_UpdateLayout)
                    textBlockForeground(button)
                } shouldBe WColor(0, 0, 160)
            } finally {
                UiTestHarness.detach(button)
            }
        }
    }
}
