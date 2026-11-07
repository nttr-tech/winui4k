package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.PasswordRevealMode
import com.appkitbox.winui4k.SpinButtonPlacementMode
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.TextChangeReason
import com.appkitbox.winui4k.TextTrimming
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WAutoSuggestBox
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WPasswordField
import com.appkitbox.winui4k.WRichTextBlock
import com.appkitbox.winui4k.WSpinner
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTextPane

/*
 * Text category: demo pages for AutoSuggestBox / NumberBox / PasswordBox / RichEditBox / RichTextBlock / TextBlock / TextBox.
 */

// region AutoSuggestBox

/** AutoSuggestBox page: lines up demos exercising WAutoSuggestBox. */
internal fun buildAutoSuggestBoxPage(): WComponent {
    val page = buildPage(
        "AutoSuggestBox",
        Messages.get("autosuggestbox.description"),
    )

    page.add(buildSimpleAutoSuggestBoxExample())
    return page
}

/** Filtering suggestions: replace suggestions via TextChanged, and confirm via QuerySubmitted. */
private fun buildSimpleAutoSuggestBoxExample(): WComponent {
    val fruits = listOf(
        Messages.get("autosuggestbox.fruit.apple"),
        Messages.get("autosuggestbox.fruit.orange"),
        Messages.get("autosuggestbox.fruit.grape"),
        Messages.get("autosuggestbox.fruit.peach"),
        Messages.get("autosuggestbox.fruit.cherry"),
        Messages.get("autosuggestbox.fruit.banana"),
        Messages.get("autosuggestbox.fruit.pineapple"),
        Messages.get("autosuggestbox.fruit.melon"),
        Messages.get("autosuggestbox.fruit.strawberry"),
        Messages.get("autosuggestbox.fruit.kiwi"),
    )
    val result = WLabel(Messages.get("autosuggestbox.result.initial")).also { it.foreground = TEXT_SECONDARY }

    val suggestBox = WAutoSuggestBox(placeholder = Messages.get("autosuggestbox.placeholder"))
    suggestBox.width = 300.0
    suggestBox.header = Messages.get("autosuggestbox.header")
    suggestBox.queryIcon = Symbol.FIND
    suggestBox.addTextChangedListener { text, reason ->
        // Only filter suggestions on the user's own keystrokes (do nothing for e.g. suggestion selection)
        if (reason == TextChangeReason.USER_INPUT) {
            suggestBox.setSuggestions(fruits.filter { it.contains(text, ignoreCase = true) })
        }
    }
    suggestBox.addQuerySubmittedListener { queryText, chosenSuggestion ->
        result.text = if (chosenSuggestion != null) {
            Messages.get("autosuggestbox.result.suggestion", chosenSuggestion)
        } else {
            Messages.get("autosuggestbox.result.query", queryText)
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(suggestBox)
    body.add(result)
    return buildExample(Messages.get("autosuggestbox.example.filter.title"), body)
}

// endregion

// region NumberBox

/** NumberBox page: lines up demos exercising WSpinner. */
internal fun buildNumberBoxPage(): WComponent {
    val page = buildPage("NumberBox", Messages.get("numberbox.description"))

    page.add(buildExpressionNumberBoxExample())
    page.add(buildSpinButtonNumberBoxExample())
    return page
}

/** Expression input: evaluate expressions like "(5 + 3) * 2" via AcceptsExpression. */
private fun buildExpressionNumberBoxExample(): WComponent {
    val result = WLabel(Messages.get("numberbox.result.initial")).also { it.foreground = TEXT_SECONDARY }

    val spinner = WSpinner(Double.NaN) // Start empty to show the placeholder text (WSpinner() starts at 0, as with JSpinner)
    spinner.width = 300.0
    spinner.header = Messages.get("numberbox.expression.header")
    spinner.placeholderText = "1 + 2 * 3"
    spinner.acceptsExpression = true
    spinner.spinButtonPlacementMode = SpinButtonPlacementMode.HIDDEN // No spin buttons, as in the original Gallery
    spinner.addChangeListener { value ->
        result.text = if (value.isNaN()) {
            Messages.get("numberbox.result.empty")
        } else {
            Messages.get("numberbox.result.value", value.toString())
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(spinner)
    body.add(result)
    return buildExample(Messages.get("numberbox.example.expression.title"), body)
}

/** Spin buttons: placement, step, and wrapping of the increment/decrement buttons. */
private fun buildSpinButtonNumberBoxExample(): WComponent {
    val spinner = WSpinner(value = 10.0)
    spinner.width = 200.0
    spinner.header = Messages.get("numberbox.spin.header")
    spinner.minimum = 0.0
    spinner.maximum = 100.0
    spinner.smallChange = 10.0
    spinner.largeChange = 25.0
    spinner.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE
    spinner.isWrapEnabled = true
    return buildExample(Messages.get("numberbox.example.spin.title"), spinner)
}

// endregion

// region PasswordBox

/** PasswordBox page: lines up demos exercising WPasswordField. */
internal fun buildPasswordBoxPage(): WComponent {
    val page = buildPage("PasswordBox", Messages.get("passwordbox.description"))

    page.add(buildSimplePasswordBoxExample())
    page.add(buildRevealModePasswordBoxExample())
    return page
}

/** Basic password box: simple validation via PasswordChanged. */
private fun buildSimplePasswordBoxExample(): WComponent {
    val result = WLabel(Messages.get("passwordbox.result.initial")).also { it.foreground = TEXT_SECONDARY }

    val passwordField = WPasswordField().apply { placeholderText = Messages.get("passwordbox.placeholder") }
    passwordField.width = 300.0
    passwordField.header = Messages.get("passwordbox.header")
    passwordField.passwordRevealMode = PasswordRevealMode.PEEK // Show the reveal button (the default is HIDDEN with no reveal button, as in Swing)
    passwordField.addPasswordChangedListener { password ->
        result.text = when {
            password.isEmpty() -> Messages.get("passwordbox.result.initial")
            password.length < 8 -> Messages.get("passwordbox.result.short", 8 - password.length)
            else -> Messages.get("passwordbox.result.ok", password.length.toString())
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(passwordField)
    body.add(result)
    return buildExample(Messages.get("passwordbox.example.simple.title"), body)
}

/** Reveal mode and mask character: PasswordRevealMode and PasswordChar. */
private fun buildRevealModePasswordBoxExample(): WComponent {
    val hiddenField = WPasswordField().apply { placeholderText = Messages.get("passwordbox.placeholder.hidden") }
    hiddenField.width = 300.0

    val visibleField = WPasswordField().apply { placeholderText = Messages.get("passwordbox.placeholder.visible") }
    visibleField.width = 300.0
    visibleField.echoChar = 0.toChar()

    val customCharField = WPasswordField().apply { placeholderText = Messages.get("passwordbox.placeholder.custom") }
    customCharField.width = 300.0
    customCharField.passwordChar = "#"

    val body = WPanel(spacing = 8.0)
    body.add(hiddenField)
    body.add(visibleField)
    body.add(customCharField)
    return buildExample(Messages.get("passwordbox.example.reveal.title"), body)
}

// endregion

// region RichEditBox

/** RichEditBox page: lines up demos exercising WTextPane. */
internal fun buildRichEditBoxPage(): WComponent {
    val page = buildPage(
        "RichEditBox",
        Messages.get("richeditbox.description"),
    )

    page.add(buildFormattingRichEditBoxExample())
    return page
}

/** Editing formatted text: toggling bold/italic on the selection, plus Undo/Redo. */
private fun buildFormattingRichEditBoxExample(): WComponent {
    val textPane = WTextPane().apply { placeholderText = Messages.get("richeditbox.placeholder") }
    textPane.width = 400.0
    textPane.height = 150.0
    textPane.lineWrap = true // Wrap at the width, as with the RichEditBox of the original Gallery (WTextPane does not wrap by default, as with JTextArea)

    val boldButton = WButton(Messages.get("richeditbox.bold"))
    boldButton.addActionListener { textPane.toggleSelectionBold() }

    val italicButton = WButton(Messages.get("richeditbox.italic"))
    italicButton.addActionListener { textPane.toggleSelectionItalic() }

    val undoButton = WButton(Messages.get("richeditbox.undo"))
    undoButton.addActionListener { if (textPane.canUndo()) textPane.undo() }

    val redoButton = WButton(Messages.get("richeditbox.redo"))
    redoButton.addActionListener { if (textPane.canRedo()) textPane.redo() }

    val toolbar = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    toolbar.add(boldButton)
    toolbar.add(italicButton)
    toolbar.add(undoButton)
    toolbar.add(redoButton)

    val body = WPanel(spacing = 8.0)
    body.add(toolbar)
    body.add(textPane)
    return buildExample(Messages.get("richeditbox.example.formatting.title"), body)
}

// endregion

// region RichTextBlock

/** RichTextBlock page: lines up demos exercising WRichTextBlock. */
internal fun buildRichTextBlockPage(): WComponent {
    val page = buildPage(
        "RichTextBlock",
        Messages.get("richtextblock.description"),
    )

    page.add(buildSimpleRichTextBlockExample())
    page.add(buildSelectionRichTextBlockExample())
    return page
}

/** Displaying formatted text: composing paragraphs from Run / Bold / Italic / Underline. */
private fun buildSimpleRichTextBlockExample(): WComponent {
    val richTextBlock = WRichTextBlock()
    richTextBlock.width = 400.0
    richTextBlock.addParagraph {
        run(Messages.get("richtextblock.sample.lead"))
        bold(Messages.get("richtextblock.sample.bold"))
        run(Messages.get("richtextblock.sample.separator1"))
        italic(Messages.get("richtextblock.sample.italic"))
        run(Messages.get("richtextblock.sample.separator2"))
        underline(Messages.get("richtextblock.sample.underline"))
        run(Messages.get("richtextblock.sample.tail"))
    }
    richTextBlock.addParagraph {
        run(Messages.get("richtextblock.sample.paragraph2"))
    }
    return buildExample(Messages.get("richtextblock.example.simple.title"), richTextBlock)
}

/** Text selection: SelectAll and reading SelectedText. */
private fun buildSelectionRichTextBlockExample(): WComponent {
    val result = WLabel(Messages.get("richtextblock.result.initial")).also { it.foreground = TEXT_SECONDARY }

    val richTextBlock = WRichTextBlock()
    richTextBlock.width = 400.0
    richTextBlock.addParagraph {
        run(Messages.get("richtextblock.selection.text"))
    }

    val readButton = WButton(Messages.get("richtextblock.getselection"))
    readButton.addActionListener {
        val selected = richTextBlock.selectedText
        result.text = if (selected.isEmpty()) {
            Messages.get("richtextblock.result.none")
        } else {
            Messages.get("richtextblock.result.selected", selected)
        }
    }

    val selectAllButton = WButton(Messages.get("richtextblock.selectall"))
    selectAllButton.addActionListener { richTextBlock.selectAll() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(readButton)
    row.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(richTextBlock)
    body.add(row)
    body.add(result)
    return buildExample(Messages.get("richtextblock.example.selection.title"), body)
}

// endregion

// region TextBlock

/** TextBlock page: lines up demos exercising WLabel. */
internal fun buildTextBlockPage(): WComponent {
    val page = buildPage("TextBlock", Messages.get("textblock.description"))

    page.add(buildSimpleTextBlockExample())
    page.add(buildTextBlockStyleExample())
    page.add(buildTextBlockWrappingExample())
    page.add(buildTextBlockSelectionExample())
    return page
}

/** Basic text display. */
private fun buildSimpleTextBlockExample(): WComponent {
    return buildExample(Messages.get("textblock.example.simple.title"), WLabel("I am a TextBlock."))
}

/** Text appearance: change font size, weight, and color. */
private fun buildTextBlockStyleExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel(Messages.get("textblock.style.size")).also { it.fontSize = 18.0 })
    body.add(WLabel(Messages.get("textblock.style.weight")).also { it.fontWeight = 600 })
    body.add(WLabel(Messages.get("textblock.style.color")).also { it.foreground = WColor(0, 95, 184) })
    return buildExample(Messages.get("textblock.example.style.title"), body)
}

/** Wrapping and trimming: handling text that doesn't fit the width. */
private fun buildTextBlockWrappingExample(): WComponent {
    val longText = Messages.get("textblock.longtext")

    val wrapped = WLabel(longText)
    wrapped.width = 300.0
    wrapped.textWrapping = TextWrapping.WRAP

    val trimmed = WLabel(longText)
    trimmed.width = 300.0
    trimmed.textTrimming = TextTrimming.CHARACTER_ELLIPSIS

    val body = WPanel(spacing = 8.0)
    body.add(WLabel("TextWrapping.WRAP:").also { it.foreground = TEXT_SECONDARY })
    body.add(wrapped)
    body.add(WLabel("TextTrimming.CHARACTER_ELLIPSIS:").also { it.foreground = TEXT_SECONDARY })
    body.add(trimmed)
    return buildExample(Messages.get("textblock.example.wrapping.title"), body)
}

/** Text selection: allow selecting and copying with the mouse. */
private fun buildTextBlockSelectionExample(): WComponent {
    val selectable = WLabel(Messages.get("textblock.selection.text"))
    selectable.isTextSelectionEnabled = true
    return buildExample(Messages.get("textblock.example.selection.title"), selectable)
}

// endregion

// region TextBox

/** TextBox page: lines up demos exercising WTextField. */
internal fun buildTextBoxPage(): WComponent {
    val page = buildPage("TextBox", Messages.get("textbox.description"))

    page.add(buildSimpleTextBoxExample())
    page.add(buildHeaderTextBoxExample())
    page.add(buildMultiLineTextBoxExample())
    page.add(buildReadOnlyTextBoxExample())
    page.add(buildTextChangedTextBoxExample())
    return page
}

/** Basic text box: with a placeholder. */
private fun buildSimpleTextBoxExample(): WComponent {
    val textField = WTextField().apply { placeholderText = Messages.get("textbox.placeholder.name") }
    textField.width = 300.0
    return buildExample(Messages.get("textbox.example.simple.title"), textField)
}

/** Header and max length: Header and MaxLength. */
private fun buildHeaderTextBoxExample(): WComponent {
    val textField = WTextField().apply { placeholderText = Messages.get("textbox.placeholder.maxlength") }
    textField.width = 300.0
    textField.header = Messages.get("textbox.header.username")
    textField.maxLength = 10
    return buildExample(Messages.get("textbox.example.header.title"), textField)
}

/** Multi-line input: AcceptsReturn and TextWrapping. */
private fun buildMultiLineTextBoxExample(): WComponent {
    val textArea = WTextField().apply { placeholderText = Messages.get("textbox.placeholder.multiline") }
    textArea.width = 400.0
    textArea.height = 120.0
    textArea.acceptsReturn = true
    textArea.textWrapping = TextWrapping.WRAP
    return buildExample(Messages.get("textbox.example.multiline.title"), textArea)
}

/** Read-only: toggling IsReadOnly. */
private fun buildReadOnlyTextBoxExample(): WComponent {
    val textField = WTextField()
    textField.width = 300.0
    textField.text = Messages.get("textbox.readonly.text")
    textField.isReadOnly = true

    val toggleButton = WButton(Messages.get("textbox.readonly.allow"))
    toggleButton.addActionListener {
        textField.isReadOnly = !textField.isReadOnly
        toggleButton.text = if (textField.isReadOnly) {
            Messages.get("textbox.readonly.allow")
        } else {
            Messages.get("textbox.readonly.restore")
        }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(textField)
    row.add(toggleButton)
    return buildExample(Messages.get("textbox.example.readonly.title"), row)
}

/** Watching input: mirror the input via TextChanged, and select all via SelectAll. */
private fun buildTextChangedTextBoxExample(): WComponent {
    val mirror = WLabel(Messages.get("textbox.mirror.initial")).also { it.foreground = TEXT_SECONDARY }

    val textField = WTextField().apply { placeholderText = Messages.get("textbox.placeholder.mirror") }
    textField.width = 300.0
    textField.addTextChangedListener { text ->
        mirror.text = if (text.isEmpty()) Messages.get("textbox.mirror.initial") else text
    }

    val selectAllButton = WButton(Messages.get("textbox.selectall"))
    selectAllButton.addActionListener { textField.selectAll() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(textField)
    row.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(mirror)
    return buildExample(Messages.get("textbox.example.textchanged.title"), body)
}

// endregion
