package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * WinUI 3's RichTextBlock (a direct FrameworkElement, so it derives from [WComponent]). No Swing equivalent.
 *
 * Displays read-only formatted text mixing bold and italic.
 * Content is assembled paragraph by paragraph via [addParagraph]:
 *
 * ```kotlin
 * richTextBlock.addParagraph {
 *     run("Mixing in ")
 *     bold("bold")
 *     run(" with regular text.")
 * }
 * ```
 */
class WRichTextBlock : WComponent(
    Activation.activate(XamlInterop.CLS_RichTextBlock, XamlInterop.IID_IRichTextBlock),
) {
    /** The IVector<Block> view of RichTextBlock.Blocks (BlockCollection). */
    private val blockVector: ComPtr by lazy {
        val blocks = own(inspectable.getPtr(XamlInterop.IRichTextBlock_get_Blocks))
        own(blocks.queryInterface(FoundationInterop.IID_IVector_Block))
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
    fun addParagraph(build: ParagraphBuilder.() -> Unit) {
        // Create the Paragraph, fill Inlines with inline content, then append it to Blocks
        val paragraph = Activation.activate(XamlInterop.CLS_Paragraph, XamlInterop.IID_IParagraph)
        try {
            val inlines = paragraph.getPtr(XamlInterop.IParagraph_get_Inlines)
                .queryInterface(FoundationInterop.IID_IVector_Inline)
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
        private fun appendSpan(runtimeClass: String, text: String) {
            val span = Activation.activate(runtimeClass).queryInterface(XamlInterop.IID_ISpan)
            try {
                val spanInlines = span.getPtr(XamlInterop.ISpan_get_Inlines)
                    .queryInterface(FoundationInterop.IID_IVector_Inline)
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
