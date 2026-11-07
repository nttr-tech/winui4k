package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Tests that check text starting with `<html>` (BasicHTML) in Swing's JLabel, JButton and tooltips is copied to WinUI TextBlock formatting (Run, Bold, Italic,
 * Underline, LineBreak, and Span with Foreground). Previously the `<html>` tags were shown as is.
 * The formatting is represented by walking the TextBlock's Inlines as `B[...]` (Bold), `I[...]`, `U[...]`, `S[...]` (Span), `LB` (LineBreak) and `"text"` (Run).
 */
@Tags("E2E")
class HtmlTextTest : FunSpec() {
    init {
        context("interpreting HTML (before turning it into XAML)") {
            test("only strings starting with <html> are taken as HTML (case-insensitive; the same as Swing's BasicHTML.isHTMLString)") {
                listOf("<html>a", "<HTML><b>a</b>", "<Html>", "<b>a</b>", " <html>a", "html", "", null)
                    .map(HtmlText::isHtml) shouldBe listOf(true, true, true, false, false, false, false, false)
            }

            test("the text without tags merges runs of spaces and line breaks into one space, starts new lines at <br> and <p>, and decodes character references") {
                HtmlText.toPlainText("<html><body>  Save\n  the <b>file</b>?<br>Yes &amp; No&nbsp;!<p>Next &#x41;&#66;</p></body></html>") shouldBe
                    "Save the file?\nYes & No !\nNext AB"
            }

            test("the content of <head>, <style> and <title> is not output") {
                HtmlText.toPlainText("<html><head><title>T</title><style>b { color: red }</style></head>Body</html>") shouldBe "Body"
            }

            test("<font color> makes #RRGGBB, #RGB and the 16 color names the Foreground, and an unreadable color becomes a Span without color") {
                listOf("#ff8000", "#F80", "red", "Navy", "#12345", "nocolor").map(HtmlText::colorOf) shouldBe
                    listOf("#FFFF8000", "#FFFF8800", "#FFFF0000", "#FF000080", null, null)
            }

            test("unclosed tags are closed at the end, unmatched closing tags are ignored, and special XAML characters are escaped in attributes") {
                HtmlText.toInlinesXaml("<html><b>a <i>\"{x}\" & <</i></u>b") shouldBe
                    "<Bold><Run Text=\"a \"/><Italic><Run Text=\"&quot;{}{x}&quot; &amp; &lt;\"/></Italic><Run Text=\"b\"/></Bold>"
            }
        }

        context("WLabel") {
            test("in <html> text the tags are not shown, <b>, <i>, <u>, <br> and <font color> become formatting, and getText stays the string as set") {
                val html = "<html>Name: <b>Tokyo</b><br><i>it</i> <u>un</u> <font color=red>red</font></html>"
                onUiThreadGet {
                    val label = WLabel(html)
                    listOf(label.text, describeTextBlock(label.automationTarget), nativeText(label.automationTarget).contains('<'))
                } shouldBe listOf(
                    html,
                    "\"Name: \" B[\"Tokyo\"] LB I[\"it\"] \" \" U[\"un\"] \" \" S[\"red\"]",
                    false,
                )
            }

            test("an HTML label made two lines with <br> is taller than a one-line label") {
                val panel = onUiThreadGet {
                    WPanel().also {
                        it.add(WLabel("Line 1"))
                        it.add(WLabel("<html>Line 1<br>Line 2</html>"))
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                try {
                    val (single, double) = onUiThreadGet {
                        panel.updateLayout()
                        panel.getComponent(0).actualHeight to panel.getComponent(1).actualHeight
                    }
                    double shouldBeGreaterThan single * 1.5
                } finally {
                    UiTestHarness.detach(panel)
                }
            }

            test("setting plain text after HTML removes the formatting and gives a single Run") {
                onUiThreadGet {
                    val label = WLabel("<html><b>Bold</b></html>")
                    label.text = "Plain <b>text</b>"
                    label.text to describeTextBlock(label.automationTarget)
                } shouldBe ("Plain <b>text</b>" to "\"Plain <b>text</b>\"")
            }
        }

        context("WButton") {
            test("the text of an <html> button becomes a formatted TextBlock, getText is the string as set, and the name is the text without tags") {
                val html = "<html><b>Save</b> file</html>"
                onUiThreadGet {
                    val button = WButton(html)
                    val content = contentOf(button)
                    val described = try {
                        content.queryInterface(XamlInterop.IID_ITextBlock).let { textBlock ->
                            try {
                                describeTextBlock(textBlock)
                            } finally {
                                textBlock.release()
                            }
                        }
                    } finally {
                        content.release()
                    }
                    listOf(button.text, described, button.accessibleName)
                } shouldBe listOf(html, "B[\"Save\"] \" file\"", "Save file")
            }

            test("the <html> text of a button with an icon is also formatted") {
                onUiThreadGet {
                    val button = WButton("<html><i>Open</i></html>")
                    button.icon = Symbol.OPEN_FILE
                    button.text to button.accessibleName
                } shouldBe ("<html><i>Open</i></html>" to "Open")
            }

            test("setting an <html> button back to plain text returns to string Content without a name") {
                onUiThreadGet {
                    val button = WButton("<html><b>Save</b></html>")
                    button.text = "Save"
                    val content = contentOf(button)
                    val boxed = try {
                        PropertyValues.unboxString(content)
                    } finally {
                        content.release()
                    }
                    listOf(button.text, boxed, button.accessibleName)
                } shouldBe listOf("Save", "Save", "")
            }
        }

        context("tooltips") {
            test("an <html> toolTipText becomes a hint with a formatted TextBlock, and getToolTipText stays the string as set") {
                val html = "<html><b>Save</b><br>Ctrl+S</html>"
                onUiThreadGet {
                    val button = WButton("Save")
                    button.toolTipText = html
                    val tip = toolTipServiceStatics.getPtr(XamlInterop.IToolTipServiceStatics_GetToolTip, button.dependencyObject)
                    val described = try {
                        tip.queryInterface(XamlInterop.IID_ITextBlock).let { textBlock ->
                            try {
                                describeTextBlock(textBlock)
                            } finally {
                                textBlock.release()
                            }
                        }
                    } finally {
                        tip.release()
                    }
                    val htmlTip = button.toolTipText
                    button.toolTipText = "Plain"
                    listOf(htmlTip, described, button.toolTipText)
                } shouldBe listOf(html, "B[\"Save\"] LB \"Ctrl+S\"", "Plain")
            }
        }
    }

    /** The button's ContentControl.Content (the caller releases it). */
    private fun contentOf(button: WButton): ComPtr {
        val contentControl = button.inspectable.queryInterface(XamlInterop.IID_IContentControl)
        return try {
            contentControl.getPtr(XamlInterop.IContentControl_get_Content)
        } finally {
            contentControl.release()
        }
    }

    private fun nativeText(textBlock: ComPtr): String = textBlock.getString(XamlInterop.ITextBlock_get_Text)

    /** Represents the Inlines of a TextBlock ([textBlock] is ITextBlock) in the form described in the class description. */
    private fun describeTextBlock(textBlock: ComPtr): String = describeInlines(textBlock.getPtr(XamlInterop.ITextBlock_get_Inlines))

    private fun describeInlines(collection: ComPtr): String {
        val inlines = try {
            collection.queryInterface(FoundationInterop.IID_IVector_Inline)
        } finally {
            collection.release()
        }
        try {
            return (0 until inlines.getInt(FoundationInterop.IVector_get_Size)).joinToString(" ") { index ->
                val inline = inlines.getPtr(FoundationInterop.IVector_GetAt, index)
                try {
                    describeInline(inline)
                } finally {
                    inline.release()
                }
            }
        } finally {
            inlines.release()
        }
    }

    private fun describeInline(inline: ComPtr): String {
        inline.queryInterfaceOrNull(XamlInterop.IID_IRun)?.let { run ->
            return try {
                "\"" + run.getString(XamlInterop.IRun_get_Text) + "\""
            } finally {
                run.release()
            }
        }
        if (implements(inline, XamlInterop.IID_ILineBreak)) return "LB"
        val kind = when {
            implements(inline, XamlInterop.IID_IBold) -> "B"
            implements(inline, XamlInterop.IID_IItalic) -> "I"
            implements(inline, XamlInterop.IID_IUnderline) -> "U"
            else -> "S"
        }
        val span = inline.queryInterface(XamlInterop.IID_ISpan)
        return try {
            kind + "[" + describeInlines(span.getPtr(XamlInterop.ISpan_get_Inlines)) + "]"
        } finally {
            span.release()
        }
    }

    private fun implements(inline: ComPtr, iid: String): Boolean {
        val view = inline.queryInterfaceOrNull(iid) ?: return false
        view.release()
        return true
    }
}
