package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.Locale

/**
 * Copies strings starting with `<html>` (BasicHTML.isHTMLString) in the text of Swing components (JLabel, JButton labels, tooltips)
 * to the XAML of a WinUI TextBlock (Run, Bold, Italic, Underline, LineBreak, and Span with Foreground).
 *
 * Only the basic tags commonly used in Swing are copied:
 * - `<b>` / `<strong>` become Bold, `<i>` / `<em>` Italic, and `<u>` Underline
 * - `<br>` becomes LineBreak. `<p>` / `<div>` and `</p>` / `</div>` start a new line if there is text before (no space above or below paragraphs)
 * - `<font color="...">` becomes a Span with Foreground (#RRGGBB, #RGB and the 16 color names of HTML 3.2). size and face are not used
 * - the content of `<head>` / `<style>` / `<script>` / `<title>` is not output
 * - other tags (`<html>` / `<body>` / `<center>` / `<a>`, etc.) are removed and only their text is output
 * - character references (`&amp;` `&lt;` `&gt;` `&quot;` `&apos;` `&nbsp;` `&#NNN;` `&#xHH;`) are decoded, and runs of spaces and line breaks become one space, as in HTML
 * Unclosed tags are closed at the end, and unmatched closing tags are ignored.
 */
internal object HtmlText {
    private const val XAML_NAMESPACE = "http://schemas.microsoft.com/winfx/2006/xaml/presentation"
    private const val HTML_PREFIX_LENGTH = 6
    private const val HEX_RADIX = 16

    /** The same as BasicHTML.isHTMLString: whether the string starts with `<html>` (case-insensitive). */
    fun isHtml(text: String?): Boolean =
        text != null && text.length >= HTML_PREFIX_LENGTH && text[0] == '<' && text[HTML_PREFIX_LENGTH - 1] == '>' &&
            text.substring(1, HTML_PREFIX_LENGTH - 1).equals("html", ignoreCase = true)

    /**
     * Sets the text of a TextBlock ([textBlock] is ITextBlock) to [value]. If [isHtml], it becomes the copied sequence of Inlines, otherwise TextBlock.Text
     * (setting Text replaces the Inlines with a single Run).
     */
    fun putText(textBlock: ComPtr, value: String) {
        Hstring.use(if (isHtml(value)) "" else value) { h -> textBlock.call(XamlInterop.ITextBlock_put_Text, h) }
        if (!isHtml(value)) return
        val loaded = Xaml.load(toTextBlockXaml(value))
        try {
            val source = loaded.queryInterface(XamlInterop.IID_ITextBlock)
            try {
                moveInlines(source, textBlock)
            } finally {
                source.release()
            }
        } finally {
            loaded.release()
        }
    }

    /** Makes the tooltip (ToolTipService.ToolTip) of [dependencyObject] a formatted TextBlock with [html] copied. */
    fun setToolTip(dependencyObject: ComPtr, html: String) {
        val textBlock = XamlElement.adopt(Activation.activate(XamlInterop.CLS_TextBlock, XamlInterop.IID_ITextBlock))
        putText(textBlock.view(XamlInterop.IID_ITextBlock), html)
        toolTipServiceStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, dependencyObject, textBlock.uiElement.ptr)
    }

    /** Removes the Inlines of [from] in order and moves them to the end of the Inlines of [to] (an Inline can be in only one owner). */
    private fun moveInlines(from: ComPtr, to: ComPtr) {
        val source = inlinesOf(from)
        val target = inlinesOf(to)
        try {
            target.call(FoundationInterop.IVector_Clear)
            while (source.getInt(FoundationInterop.IVector_get_Size) > 0) {
                val inline = source.getPtr(FoundationInterop.IVector_GetAt, 0)
                try {
                    source.call(FoundationInterop.IVector_RemoveAt, 0)
                    target.call(FoundationInterop.IVector_Append, inline)
                } finally {
                    inline.release()
                }
            }
        } finally {
            source.release()
            target.release()
        }
    }

    private fun inlinesOf(textBlock: ComPtr): ComPtr {
        val collection = textBlock.getPtr(XamlInterop.ITextBlock_get_Inlines)
        return try {
            collection.queryInterface(FoundationInterop.IID_IVector_Inline)
        } finally {
            collection.release()
        }
    }

    /** The XAML of a formatted TextBlock with [html] copied (passed to XamlReader.Load). */
    fun toTextBlockXaml(html: String): String = "<TextBlock xmlns=\"$XAML_NAMESPACE\">" + toInlinesXaml(html) + "</TextBlock>"

    /** The text of [html] without tags (for UI Automation names etc.; line breaks are `\n`). */
    fun toPlainText(html: String): String = buildString {
        convert(
            html,
            object : Sink {
                override fun text(value: String) {
                    append(value)
                }

                override fun lineBreak() {
                    append('\n')
                }

                override fun open(element: Element) = Unit

                override fun close(element: Element) = Unit
            },
        )
    }

    /** The sequence of Inline XAML with [html] copied (the children of a TextBlock). */
    internal fun toInlinesXaml(html: String): String = buildString {
        convert(
            html,
            object : Sink {
                override fun text(value: String) {
                    append("<Run Text=\"").append(escapeAttribute(value)).append("\"/>")
                }

                override fun lineBreak() {
                    append("<LineBreak/>")
                }

                override fun open(element: Element) {
                    append(element.openXaml)
                }

                override fun close(element: Element) {
                    append(element.closeXaml)
                }
            },
        )
    }

    /** The output target of the conversion. */
    private interface Sink {
        fun text(value: String)

        fun lineBreak()

        fun open(element: Element)

        fun close(element: Element)
    }

    /** Opening and closing of a formatting tag (a XAML Span or its relatives). */
    private class Element(val tag: String, val openXaml: String, val closeXaml: String)

    private fun convert(html: String, sink: Sink) = Converter(html, sink).run()

    /** Reads [html] from the start and passes text and tags to [sink]. */
    private class Converter(private val html: String, private val sink: Sink) {
        private val open = ArrayList<Element>()
        private var hidden = 0 // The depth inside head / style / script / title
        private var pendingSpace = false // A space not output yet, to merge runs of spaces into one
        private var lineHasText = false // Whether the current line has text (whether p / div start a new line)
        private var pendingBreak = false // A line break from p / div (output when text follows, so a trailing </p> does not make an empty line)
        private val text = StringBuilder()

        fun run() {
            var index = 0
            while (index < html.length) {
                index = if (html[index] == '<') readTag(index) else readText(index)
            }
            flushText()
            for (i in open.size - 1 downTo 0) sink.close(open[i])
        }

        /**
         * Reads the tag starting at the `<` at [start] and returns the next position. Anything after an unclosed tag is dropped (not output, as with Swing's HTML parsing).
         * If what follows `<` is not a letter, `/`, `!` or `?`, it is not a tag and is read as the character `<`.
         */
        private fun readTag(start: Int): Int {
            val next = html.getOrNull(start + 1)
            if (next == null || !(next.isLetter() || next in TAG_MARKS)) return readText(start)
            val end = html.indexOf('>', start + 1)
            if (end < 0) return html.length
            parseTag(html.substring(start + 1, end))?.let(::onTag)
            return end + 1
        }

        private fun onTag(tag: Tag) {
            when (tag.name) {
                "head", "style", "script", "title" -> hidden = if (tag.closing) maxOf(0, hidden - 1) else hidden + 1
                "br" -> if (hidden == 0 && !tag.closing) breakLine()
                "p", "div" -> if (hidden == 0 && lineHasText) pendingBreak = true
                "b", "strong", "i", "em", "u", "font" -> if (hidden == 0) {
                    if (tag.closing) closeElement(tag.name) else openElement(tag)
                }
                else -> Unit
            }
        }

        private fun openElement(tag: Tag) {
            if (pendingBreak) breakLine()
            if (pendingSpace && lineHasText) text.append(' ')
            pendingSpace = false
            flushText()
            val element = elementOf(tag)
            open += element
            sink.open(element)
        }

        private fun closeElement(name: String) {
            val at = open.indexOfLast { it.tag == name }
            if (at < 0) return
            flushText()
            for (i in open.size - 1 downTo at) sink.close(open.removeAt(i))
        }

        /** Reads the character at [start] (the whole reference for a character reference) and returns the next position. */
        private fun readText(start: Int): Int {
            var next = start + 1
            var decoded = html[start].toString()
            if (html[start] == '&') {
                val semicolon = html.indexOf(';', next)
                val entity = if (semicolon in next..(next + MAX_ENTITY_LENGTH)) decodeEntity(html.substring(next, semicolon)) else null
                if (entity != null) {
                    decoded = entity
                    next = semicolon + 1
                }
            }
            if (hidden == 0) decoded.forEach(::onChar)
            return next
        }

        private fun onChar(ch: Char) {
            if (ch.isWhitespace() && ch != NBSP) {
                pendingSpace = true
                return
            }
            if (pendingBreak) breakLine()
            if (pendingSpace && lineHasText) text.append(' ')
            pendingSpace = false
            text.append(if (ch == NBSP) ' ' else ch)
            lineHasText = true
        }

        private fun flushText() {
            if (text.isEmpty()) return
            sink.text(text.toString())
            text.setLength(0)
        }

        private fun breakLine() {
            flushText()
            sink.lineBreak()
            pendingSpace = false
            pendingBreak = false
            lineHasText = false
        }
    }

    /** Characters other than letters that make it a tag when they come right after `<` (closing tags, comments, declarations). */
    private const val TAG_MARKS = "/!?"
    private const val MAX_ENTITY_LENGTH = 10
    private const val NBSP = ' '

    private class Tag(val name: String, val closing: Boolean, val attributes: Map<String, String>)

    /** Reads the [body] between `<` and `>`. null for comments and declarations (starting with `!` / `?`). */
    private fun parseTag(body: String): Tag? {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed[0] == '!' || trimmed[0] == '?') return null
        val closing = trimmed[0] == '/'
        val rest = (if (closing) trimmed.substring(1) else trimmed).trimEnd('/').trim()
        val nameEnd = rest.indexOfFirst { it.isWhitespace() }.let { if (it < 0) rest.length else it }
        val name = rest.substring(0, nameEnd).lowercase(Locale.ROOT)
        val attributes = HashMap<String, String>()
        ATTRIBUTE.findAll(rest.substring(nameEnd)).forEach { match ->
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }.ifEmpty { match.groupValues[4] }
            attributes[match.groupValues[1].lowercase(Locale.ROOT)] = value
        }
        return Tag(name, closing, attributes)
    }

    private val ATTRIBUTE = Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""")

    private fun elementOf(tag: Tag): Element = when (tag.name) {
        "b", "strong" -> Element(tag.name, "<Bold>", "</Bold>")
        "i", "em" -> Element(tag.name, "<Italic>", "</Italic>")
        "u" -> Element(tag.name, "<Underline>", "</Underline>")
        else -> {
            val color = tag.attributes["color"]?.let(::colorOf)
            if (color == null) Element(tag.name, "<Span>", "</Span>") else Element(tag.name, "<Span Foreground=\"$color\">", "</Span>")
        }
    }

    /** An HTML color (#RRGGBB, #RGB, the 16 color names) as XAML #AARRGGBB. null if it cannot be read (no color). */
    internal fun colorOf(value: String): String? {
        val trimmed = value.trim().lowercase(Locale.ROOT)
        val hex = NAMED_COLORS[trimmed] ?: trimmed.removePrefix("#")
        if (hex.any { Character.digit(it, HEX_RADIX) < 0 }) return null
        val full = when (hex.length) {
            HEX_SHORT -> hex.map { "$it$it" }.joinToString("")
            HEX_FULL -> hex
            else -> return null
        }
        return "#FF" + full.uppercase(Locale.ROOT)
    }

    private const val HEX_SHORT = 3
    private const val HEX_FULL = 6

    /** The 16 colors of HTML 3.2 (the same values as Swing's CSS.stringToColor). */
    private val NAMED_COLORS = mapOf(
        "black" to "000000",
        "silver" to "c0c0c0",
        "gray" to "808080",
        "white" to "ffffff",
        "maroon" to "800000",
        "red" to "ff0000",
        "purple" to "800080",
        "fuchsia" to "ff00ff",
        "green" to "008000",
        "lime" to "00ff00",
        "olive" to "808000",
        "yellow" to "ffff00",
        "navy" to "000080",
        "blue" to "0000ff",
        "teal" to "008080",
        "aqua" to "00ffff",
    )

    private fun decodeEntity(name: String): String? = when {
        name.startsWith("#x") || name.startsWith("#X") -> name.substring(2).toIntOrNull(HEX_RADIX)?.let(::codePointString)
        name.startsWith("#") -> name.substring(1).toIntOrNull()?.let(::codePointString)
        else -> when (name) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "nbsp" -> NBSP.toString()
            else -> null
        }
    }

    private fun codePointString(codePoint: Int): String? =
        if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else null

    private fun escapeAttribute(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '{' -> append("{}{") // So it is not taken as the start of a XAML markup extension
                else -> append(ch)
            }
        }
    }
}
