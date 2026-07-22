package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winrt.getString
import jp.hisano.winui4k.winui.Abi

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
    Activation.activate(Abi.CLS_RichTextBlock).queryInterface(Abi.IID_IRichTextBlock),
) {
    /** The IVector<Block> view of RichTextBlock.Blocks (BlockCollection). */
    private val blockVector: ComPtr by lazy {
        inspectable.getPtr(Abi.IRichTextBlock_get_Blocks)
            .queryInterface(Abi.IID_IVector_Block)
    }

    /** How text wraps (RichTextBlock.TextWrapping). Wraps by default. */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(Abi.IRichTextBlock_get_TextWrapping))
        set(value) = inspectable.call(Abi.IRichTextBlock_put_TextWrapping, value.native)

    /** How text that doesn't fit is trimmed (RichTextBlock.TextTrimming). */
    var textTrimming: TextTrimming
        get() = TextTrimming.of(inspectable.getInt(Abi.IRichTextBlock_get_TextTrimming))
        set(value) = inspectable.call(Abi.IRichTextBlock_put_TextTrimming, value.native)

    /** Whether text can be selected with the mouse (RichTextBlock.IsTextSelectionEnabled). Enabled by default. */
    var isTextSelectionEnabled: Boolean
        get() = inspectable.getBool(Abi.IRichTextBlock_get_IsTextSelectionEnabled)
        set(value) = inspectable.putBool(Abi.IRichTextBlock_put_IsTextSelectionEnabled, value)

    /** The currently selected text (RichTextBlock.SelectedText). */
    val selectedText: String
        get() = inspectable.getString(Abi.IRichTextBlock_get_SelectedText)

    /** Selects all text (RichTextBlock.SelectAll). */
    fun selectAll() {
        inspectable.call(Abi.IRichTextBlock_SelectAll)
    }

    /** Appends a paragraph at the end (Blocks.Append). Content is assembled via [ParagraphBuilder]. */
    fun addParagraph(build: ParagraphBuilder.() -> Unit) {
        // Create the Paragraph, fill Inlines with inline content, then append it to Blocks
        val paragraph = Activation.activate(Abi.CLS_Paragraph).queryInterface(Abi.IID_IParagraph)
        try {
            val inlines = paragraph.getPtr(Abi.IParagraph_get_Inlines)
                .queryInterface(Abi.IID_IVector_Inline)
            try {
                ParagraphBuilder(inlines).build()
            } finally {
                inlines.release()
            }
            val block = paragraph.queryInterface(Abi.IID_IBlock)
            try {
                blockVector.call(Abi.IVector_Append, block)
            } finally {
                block.release()
            }
        } finally {
            paragraph.release()
        }
    }

    /** Removes all paragraphs (Blocks.Clear). */
    fun removeAllParagraphs() {
        blockVector.call(Abi.IVector_Clear)
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
            appendSpan(Abi.CLS_Bold, text)
        }

        /** Appends italic text (Documents.Italic + Run). */
        fun italic(text: String) {
            appendSpan(Abi.CLS_Italic, text)
        }

        /** Appends underlined text (Documents.Underline + Run). */
        fun underline(text: String) {
            appendSpan(Abi.CLS_Underline, text)
        }

        /** Creates a Run, sets its Text, and returns a pointer to the IRun. The caller releases it. */
        private fun createRun(text: String): ComPtr {
            val run = Activation.activate(Abi.CLS_Run).queryInterface(Abi.IID_IRun)
            Hstring.use(text) { h -> run.call(Abi.IRun_put_Text, h) }
            return run
        }

        /** Creates a Bold / Italic / Underline Span, puts a single Run inside it, and appends it. */
        private fun appendSpan(runtimeClass: String, text: String) {
            val span = Activation.activate(runtimeClass).queryInterface(Abi.IID_ISpan)
            try {
                val spanInlines = span.getPtr(Abi.ISpan_get_Inlines)
                    .queryInterface(Abi.IID_IVector_Inline)
                try {
                    val run = createRun(text)
                    try {
                        val inline = run.queryInterface(Abi.IID_IInline)
                        try {
                            spanInlines.call(Abi.IVector_Append, inline)
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
            val inline = ptr.queryInterface(Abi.IID_IInline)
            try {
                inlines.call(Abi.IVector_Append, inline)
            } finally {
                inline.release()
            }
        }
    }
}
