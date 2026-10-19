package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer

/**
 * WinUI 3's RichTextBlock (a direct FrameworkElement, so it derives from [WComponent]). No Swing equivalent.
 *
 * Displays read-only formatted text mixing bold and italic.
 * Plain text is read and written with [text]; to mix formatting, build it paragraph by paragraph with [addParagraph]:
 *
 * ```kotlin
 * richTextBlock.addParagraph {
 *     run("Mixing in ")
 *     bold("bold")
 *     run(" with regular text.")
 * }
 * ```
 *
 * A line break `"\n"` in [text] separates paragraphs, and reading [text] returns the paragraphs, including formatted text,
 * joined with `"\n"`.
 */
class WRichTextBlock : WComponent(
    Activation.activate(XamlInterop.CLS_RichTextBlock, XamlInterop.IID_IRichTextBlock),
) {
    /** The IVector<Block> view of RichTextBlock.Blocks (BlockCollection). */
    private val blockVector: ComPtr by lazy {
        val blocks = own(inspectable.getPtr(XamlInterop.IRichTextBlock_get_Blocks))
        own(blocks.queryInterface(FoundationInterop.IID_IVector_Block))
    }

    /**
     * JComponent.getFont / setFont-like: the font (RichTextBlock.FontFamily / FontWeight / FontStyle / FontSize).
     * Paragraph text is drawn in this font, except for parts changed individually with [ParagraphBuilder.bold] and the like.
     * Reads return the native effective value, and setting null clears it and restores the default (same as [WLabel.font]).
     */
    var font: WFont?
        get() = FontProperties.read(inspectable, FontProperties.RICH_TEXT_BLOCK)
        set(value) = FontProperties.write(inspectable, dependencyObject, FontProperties.RICH_TEXT_BLOCK, value)

    /**
     * JLabel.getText / setText-like: the displayed string. The paragraphs joined with `"\n"`, including formatted text such
     * as bold.
     * Setting it removes all existing paragraphs and makes one paragraph (a single unformatted Run) per line break (`"\n"`,
     * `"\r\n"`, `"\r"`).
     * An empty string leaves no paragraphs. Paragraphs are created even if empty before or after a line break (`"a\n"` is
     * 2 paragraphs and reads back as the same string).
     * Non-text content such as images (InlineUIContainer, BlockUIContainer) is not included in the string.
     */
    var text: String
        get() = readText()
        set(value) {
            removeAllParagraphs()
            append(value)
        }

    /**
     * JTextArea.append-like: appends [str] to the end of the text. Does nothing if null or empty.
     * The first line continues the end of the last paragraph, and what follows a line break (`"\n"` etc.) becomes a new
     * paragraph. A paragraph is created if there is none.
     */
    fun append(str: String?) {
        if (str.isNullOrEmpty()) return
        val lines = str.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        lines.forEachIndexed { index, line ->
            val paragraph = if (index == 0) lastParagraphOrNew() else newParagraph()
            try {
                if (line.isNotEmpty()) {
                    val inlines = inlinesOf(paragraph, XamlInterop.IParagraph_get_Inlines)
                    try {
                        ParagraphBuilder(inlines).run(line)
                    } finally {
                        inlines.release()
                    }
                }
            } finally {
                paragraph.release()
            }
        }
    }

    /**
     * JLabel.getForeground / setForeground-like: the text color (RichTextBlock.Foreground). Passed as a SolidColorBrush.
     * Reads return the native effective value, which is the default color even before setting (null if not a solid color
     * brush).
     * Setting null clears it and restores the default (the color inherited from the parent) (DependencyObject.ClearValue).
     * Paragraph text is drawn in this color, except for parts colored individually.
     */
    var foreground: WColor?
        get() {
            val brush = inspectable.getPtrOrNull(XamlInterop.IRichTextBlock_get_Foreground) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        }
        set(value) {
            if (value == null) {
                clearDependencyProperty(
                    dependencyObject,
                    XamlInterop.CLS_RichTextBlock,
                    XamlInterop.IID_IRichTextBlockStatics,
                    XamlInterop.IRichTextBlockStatics_get_ForegroundProperty,
                )
                return
            }
            val brush = value.createBrush()
            try {
                inspectable.call(XamlInterop.IRichTextBlock_put_Foreground, brush.ptr)
            } finally {
                brush.release()
            }
        }

    /** How text wraps (RichTextBlock.TextWrapping). Wraps by default. */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(XamlInterop.IRichTextBlock_get_TextWrapping))
        set(value) = inspectable.call(XamlInterop.IRichTextBlock_put_TextWrapping, value.native)

    /** How text that doesn't fit is trimmed (RichTextBlock.TextTrimming). */
    var textTrimming: TextTrimming
        get() = TextTrimming.of(inspectable.getInt(XamlInterop.IRichTextBlock_get_TextTrimming))
        set(value) = inspectable.call(XamlInterop.IRichTextBlock_put_TextTrimming, value.native)

    /** Whether text can be selected with the mouse (RichTextBlock.IsTextSelectionEnabled). Enabled by default. */
    var isTextSelectionEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IRichTextBlock_get_IsTextSelectionEnabled)
        set(value) = inspectable.putBool(XamlInterop.IRichTextBlock_put_IsTextSelectionEnabled, value)

    /** The currently selected text (RichTextBlock.SelectedText). */
    val selectedText: String
        get() = inspectable.getString(XamlInterop.IRichTextBlock_get_SelectedText)

    /** Selects all text (RichTextBlock.SelectAll). */
    fun selectAll() {
        inspectable.call(XamlInterop.IRichTextBlock_SelectAll)
    }

    /** Appends a paragraph at the end (Blocks.Append). Content is assembled via [ParagraphBuilder]. */
    @JvmSynthetic
    fun addParagraph(build: ParagraphBuilder.() -> Unit) {
        // Create the Paragraph, fill Inlines with inline content, then append it to Blocks
        val paragraph = Activation.activate(XamlInterop.CLS_Paragraph, XamlInterop.IID_IParagraph)
        try {
            val inlines = inlinesOf(paragraph, XamlInterop.IParagraph_get_Inlines)
            try {
                ParagraphBuilder(inlines).build()
            } finally {
                inlines.release()
            }
            val block = paragraph.queryInterface(XamlInterop.IID_IBlock)
            try {
                blockVector.call(FoundationInterop.IVector_Append, block)
            } finally {
                block.release()
            }
        } finally {
            paragraph.release()
        }
    }

    /** A Java-friendly overload of [addParagraph]. [build] receives a [ParagraphBuilder]. */
    fun addParagraph(build: Consumer<ParagraphBuilder>) {
        addParagraph { build.accept(this) }
    }

    /** Creates a new paragraph (Paragraph) and appends it to the end of Blocks. The caller must release it. */
    private fun newParagraph(): ComPtr {
        val paragraph = Activation.activate(XamlInterop.CLS_Paragraph, XamlInterop.IID_IParagraph)
        val block = paragraph.queryInterface(XamlInterop.IID_IBlock)
        try {
            blockVector.call(FoundationInterop.IVector_Append, block)
        } finally {
            block.release()
        }
        return paragraph
    }

    /** Returns the last block if it is a paragraph; otherwise (empty, or a non-paragraph block) a new paragraph. The caller must release it. */
    private fun lastParagraphOrNew(): ComPtr {
        val size = blockVector.getInt(FoundationInterop.IVector_get_Size)
        if (size > 0) {
            val last = blockVector.getPtr(FoundationInterop.IVector_GetAt, size - 1)
            try {
                val paragraph = last.queryInterfaceOrNull(XamlInterop.IID_IParagraph)
                if (paragraph != null) return paragraph
            } finally {
                last.release()
            }
        }
        return newParagraph()
    }

    /** The paragraphs joined with `"\n"`. Non-paragraph blocks count as empty paragraphs. */
    private fun readText(): String {
        val size = blockVector.getInt(FoundationInterop.IVector_get_Size)
        val paragraphs = (0 until size).map { index ->
            val block = blockVector.getPtr(FoundationInterop.IVector_GetAt, index)
            try {
                val paragraph = block.queryInterfaceOrNull(XamlInterop.IID_IParagraph)
                if (paragraph == null) {
                    ""
                } else {
                    try {
                        textOfInlines(paragraph, XamlInterop.IParagraph_get_Inlines)
                    } finally {
                        paragraph.release()
                    }
                }
            } finally {
                block.release()
            }
        }
        return paragraphs.joinToString("\n")
    }

    /** Removes all paragraphs (Blocks.Clear). */
    fun removeAllParagraphs() {
        blockVector.call(FoundationInterop.IVector_Clear)
    }

    /** A builder that appends inline content to a paragraph (Paragraph.Inlines). */
    class ParagraphBuilder internal constructor(private val inlines: ComPtr) {
        /** Appends regular text (Documents.Run). */
        fun run(text: String) {
            val run = createRun(text)
            try {
                appendInline(run)
            } finally {
                run.release()
            }
        }

        /** Appends bold text (Documents.Bold + Run). */
        fun bold(text: String) {
            appendSpan(XamlInterop.CLS_Bold, text)
        }

        /** Appends italic text (Documents.Italic + Run). */
        fun italic(text: String) {
            appendSpan(XamlInterop.CLS_Italic, text)
        }

        /** Appends underlined text (Documents.Underline + Run). */
        fun underline(text: String) {
            appendSpan(XamlInterop.CLS_Underline, text)
        }

        /** Creates a Run, sets its Text, and returns a pointer to the IRun. The caller releases it. */
        private fun createRun(text: String): ComPtr {
            val run = Activation.activate(XamlInterop.CLS_Run, XamlInterop.IID_IRun)
            Hstring.use(text) { h -> run.call(XamlInterop.IRun_put_Text, h) }
            return run
        }

        /** Creates a Bold / Italic / Underline Span, puts a single Run inside it, and appends it. */
        @Suppress("NestedBlockDepth") // Only looks deep due to the try/finally releasing the COM reference
        private fun appendSpan(runtimeClass: String, text: String) {
            val span = Activation.activate(runtimeClass, XamlInterop.IID_ISpan)
            try {
                val spanInlines = inlinesOf(span, XamlInterop.ISpan_get_Inlines)
                try {
                    val run = createRun(text)
                    try {
                        val inline = run.queryInterface(XamlInterop.IID_IInline)
                        try {
                            spanInlines.call(FoundationInterop.IVector_Append, inline)
                        } finally {
                            inline.release()
                        }
                    } finally {
                        run.release()
                    }
                } finally {
                    spanInlines.release()
                }
                appendInline(span)
            } finally {
                span.release()
            }
        }

        /** QIs [ptr] to IInline and appends it to the paragraph's Inlines. [ptr] itself is not released. */
        private fun appendInline(ptr: ComPtr) {
            val inline = ptr.queryInterface(XamlInterop.IID_IInline)
            try {
                inlines.call(FoundationInterop.IVector_Append, inline)
            } finally {
                inline.release()
            }
        }
    }
}

/**
 * Returns the Inlines of [owner] (Paragraph / Span) as an IVector<Inline>. Intermediate references are released.
 * The caller must release it.
 */
private fun inlinesOf(owner: ComPtr, getInlinesSlot: Int): ComPtr {
    val collection = owner.getPtr(getInlinesSlot)
    return try {
        collection.queryInterface(FoundationInterop.IID_IVector_Inline)
    } finally {
        collection.release()
    }
}

/**
 * The text of the Inlines of [owner] (Paragraph / Span). A Run gives its Text, a Span (Bold / Italic / Underline, etc.)
 * follows its inner Inlines, and a LineBreak is `"\n"`. Other Inlines such as images are treated as empty.
 */
private fun textOfInlines(owner: ComPtr, getInlinesSlot: Int): String {
    val inlines = inlinesOf(owner, getInlinesSlot)
    try {
        val size = inlines.getInt(FoundationInterop.IVector_get_Size)
        val text = StringBuilder()
        for (index in 0 until size) {
            val inline = inlines.getPtr(FoundationInterop.IVector_GetAt, index)
            try {
                text.append(textOfInline(inline))
            } finally {
                inline.release()
            }
        }
        return text.toString()
    } finally {
        inlines.release()
    }
}

/** The text of one Inline (see [textOfInlines]). */
private fun textOfInline(inline: ComPtr): String {
    val run = inline.queryInterfaceOrNull(XamlInterop.IID_IRun)
    if (run != null) {
        try {
            return run.getString(XamlInterop.IRun_get_Text)
        } finally {
            run.release()
        }
    }
    val span = inline.queryInterfaceOrNull(XamlInterop.IID_ISpan)
    if (span != null) {
        try {
            return textOfInlines(span, XamlInterop.ISpan_get_Inlines)
        } finally {
            span.release()
        }
    }
    val lineBreak = inline.queryInterfaceOrNull(XamlInterop.IID_ILineBreak)
    if (lineBreak != null) {
        lineBreak.release()
        return "\n"
    }
    return ""
}
