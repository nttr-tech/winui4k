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
        "A text box that shows a list of suggestions as you type. Try out WAutoSuggestBox.",
    )

    page.add(buildSimpleAutoSuggestBoxExample())
    return page
}

/** Filtering suggestions: replace suggestions via TextChanged, and confirm via QuerySubmitted. */
private fun buildSimpleAutoSuggestBoxExample(): WComponent {
    val fruits = listOf(
        "Apple", "Orange", "Grape", "Peach", "Cherry",
        "Banana", "Pineapple", "Melon", "Strawberry", "Kiwi",
    )
    val result = WLabel("Confirm with Enter or by choosing a suggestion").also { it.foreground = TEXT_SECONDARY }

    val suggestBox = WAutoSuggestBox(placeholder = "Enter a fruit name")
    suggestBox.width = 300.0
    suggestBox.header = "Search fruits"
    suggestBox.queryIcon = Symbol.FIND
    suggestBox.addTextChangedListener { text, reason ->
        // Only filter suggestions on the user's own keystrokes (do nothing for e.g. suggestion selection)
        if (reason == TextChangeReason.USER_INPUT) {
            suggestBox.setSuggestions(fruits.filter { it.contains(text) })
        }
    }
    suggestBox.addQuerySubmittedListener { queryText, chosenSuggestion ->
        result.text = if (chosenSuggestion != null) {
            "Confirmed from suggestion: $chosenSuggestion"
        } else {
            "Confirmed as typed: $queryText"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(suggestBox)
    body.add(result)
    return buildExample("Filtering suggestions (TextChanged / QuerySubmitted)", body)
}

// endregion

// region NumberBox

/** NumberBox page: lines up demos exercising WSpinner. */
internal fun buildNumberBoxPage(): WComponent {
    val page = buildPage("NumberBox", "A control for entering, validating, and incrementing/decrementing numbers. Try out WSpinner.")

    page.add(buildExpressionNumberBoxExample())
    page.add(buildSpinButtonNumberBoxExample())
    return page
}

/** Expression input: evaluate expressions like "(5 + 3) * 2" via AcceptsExpression. */
private fun buildExpressionNumberBoxExample(): WComponent {
    val result = WLabel("The confirmed value appears here").also { it.foreground = TEXT_SECONDARY }

    val spinner = WSpinner()
    spinner.width = 300.0
    spinner.header = "You can also enter an expression (e.g. (5 + 3) * 2)"
    spinner.placeholderText = "1 + 2 * 3"
    spinner.acceptsExpression = true
    spinner.addChangeListener { value ->
        result.text = if (value.isNaN()) "Not entered" else "Value: $value"
    }

    val body = WPanel(spacing = 8.0)
    body.add(spinner)
    body.add(result)
    return buildExample("Expression input (AcceptsExpression / ValueChanged)", body)
}

/** Spin buttons: placement, step, and wrapping of the increment/decrement buttons. */
private fun buildSpinButtonNumberBoxExample(): WComponent {
    val spinner = WSpinner(value = 10.0)
    spinner.width = 150.0
    spinner.header = "0 to 100 (steps of 10, wraps around)"
    spinner.minimum = 0.0
    spinner.maximum = 100.0
    spinner.smallChange = 10.0
    spinner.largeChange = 25.0
    spinner.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE
    spinner.isWrapEnabled = true
    return buildExample("Spin buttons (SpinButtonPlacementMode / SmallChange / IsWrapEnabled)", spinner)
}

// endregion

// region PasswordBox

/** PasswordBox page: lines up demos exercising WPasswordField. */
internal fun buildPasswordBoxPage(): WComponent {
    val page = buildPage("PasswordBox", "A control for entering a password as masked characters. Try out WPasswordField.")

    page.add(buildSimplePasswordBoxExample())
    page.add(buildRevealModePasswordBoxExample())
    return page
}

/** Basic password box: simple validation via PasswordChanged. */
private fun buildSimplePasswordBoxExample(): WComponent {
    val result = WLabel("Enter a password of at least 8 characters").also { it.foreground = TEXT_SECONDARY }

    val passwordField = WPasswordField(placeholder = "Enter password")
    passwordField.width = 300.0
    passwordField.header = "Password"
    passwordField.addPasswordChangedListener { password ->
        result.text = when {
            password.isEmpty() -> "Enter a password of at least 8 characters"
            password.length < 8 -> "${8 - password.length} more characters needed"
            else -> "OK (${password.length} characters)"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(passwordField)
    body.add(result)
    return buildExample("Simple password box (Header / PasswordChanged)", body)
}

/** Reveal mode and mask character: PasswordRevealMode and PasswordChar. */
private fun buildRevealModePasswordBoxExample(): WComponent {
    val hiddenField = WPasswordField(placeholder = "No reveal button (HIDDEN)")
    hiddenField.width = 300.0
    hiddenField.passwordRevealMode = PasswordRevealMode.HIDDEN

    val customCharField = WPasswordField(placeholder = "Use # as the mask character")
    customCharField.width = 300.0
    customCharField.passwordChar = "#"

    val body = WPanel(spacing = 8.0)
    body.add(hiddenField)
    body.add(customCharField)
    return buildExample("Reveal mode and mask character (PasswordRevealMode / PasswordChar)", body)
}

// endregion

// region RichEditBox

/** RichEditBox page: lines up demos exercising WTextPane. */
internal fun buildRichEditBoxPage(): WComponent {
    val page = buildPage(
        "RichEditBox",
        "A control for editing formatted text such as bold and italic. Try out WTextPane.",
    )

    page.add(buildFormattingRichEditBoxExample())
    return page
}

/** Editing formatted text: toggling bold/italic on the selection, plus Undo/Redo. */
private fun buildFormattingRichEditBoxExample(): WComponent {
    val textPane = WTextPane(placeholder = "Enter text, select it, then press a formatting button")
    textPane.width = 400.0
    textPane.height = 150.0

    val boldButton = WButton("Bold")
    boldButton.addActionListener { textPane.toggleSelectionBold() }

    val italicButton = WButton("Italic")
    italicButton.addActionListener { textPane.toggleSelectionItalic() }

    val undoButton = WButton("Undo")
    undoButton.addActionListener { if (textPane.canUndo) textPane.undo() }

    val redoButton = WButton("Redo")
    redoButton.addActionListener { if (textPane.canRedo) textPane.redo() }

    val toolbar = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    toolbar.add(boldButton)
    toolbar.add(italicButton)
    toolbar.add(undoButton)
    toolbar.add(redoButton)

    val body = WPanel(spacing = 8.0)
    body.add(toolbar)
    body.add(textPane)
    return buildExample("Editing formatted text (Bold / Italic / Undo / Redo)", body)
}

// endregion

// region RichTextBlock

/** RichTextBlock page: lines up demos exercising WRichTextBlock. */
internal fun buildRichTextBlockPage(): WComponent {
    val page = buildPage(
        "RichTextBlock",
        "A control that displays read-only formatted text mixing bold and italic. Try out WRichTextBlock.",
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
        run("RichTextBlock can mix ")
        bold("bold")
        run(", ")
        italic("italic")
        run(", and ")
        underline("underlined")
        run(" text together in a single block.")
    }
    richTextBlock.addParagraph {
        run("Adding multiple paragraphs displays them with spacing in between.")
    }
    return buildExample("Displaying formatted text (Paragraph / Run / Bold / Italic / Underline)", richTextBlock)
}

/** Text selection: SelectAll and reading SelectedText. */
private fun buildSelectionRichTextBlockExample(): WComponent {
    val result = WLabel("The selected text appears here").also { it.foreground = TEXT_SECONDARY }

    val richTextBlock = WRichTextBlock()
    richTextBlock.width = 400.0
    richTextBlock.addParagraph {
        run("This text can be selected with the mouse. Drag to select, then press the button below.")
    }

    val readButton = WButton("Get selected text")
    readButton.addActionListener {
        val selected = richTextBlock.selectedText
        result.text = if (selected.isEmpty()) "Nothing is selected" else "Selected: $selected"
    }

    val selectAllButton = WButton("Select all")
    selectAllButton.addActionListener { richTextBlock.selectAll() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(readButton)
    row.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(richTextBlock)
    body.add(row)
    body.add(result)
    return buildExample("Text selection (IsTextSelectionEnabled / SelectedText / SelectAll)", body)
}

// endregion

// region TextBlock

/** TextBlock page: lines up demos exercising WLabel. */
internal fun buildTextBlockPage(): WComponent {
    val page = buildPage("TextBlock", "A basic control that displays read-only text. Try out WLabel.")

    page.add(buildSimpleTextBlockExample())
    page.add(buildTextBlockStyleExample())
    page.add(buildTextBlockWrappingExample())
    page.add(buildTextBlockSelectionExample())
    return page
}

/** Basic text display. */
private fun buildSimpleTextBlockExample(): WComponent {
    return buildExample("Simple text", WLabel("I am a TextBlock."))
}

/** Text appearance: change font size, weight, and color. */
private fun buildTextBlockStyleExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel("Text at font size 18").also { it.fontSize = 18.0 })
    body.add(WLabel("SemiBold (600) text").also { it.fontWeight = 600 })
    body.add(WLabel("Colored text").also { it.foreground = WColor(0, 95, 184) })
    return buildExample("Changing the style (FontSize / FontWeight / Foreground)", body)
}

/** Wrapping and trimming: handling text that doesn't fit the width. */
private fun buildTextBlockWrappingExample(): WComponent {
    val longText = "This text is long enough that it doesn't fit the control's width, so you can see wrapping and trimming in action."

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
    return buildExample("Wrapping and trimming (TextWrapping / TextTrimming)", body)
}

/** Text selection: allow selecting and copying with the mouse. */
private fun buildTextBlockSelectionExample(): WComponent {
    val selectable = WLabel("This text can be selected by dragging with the mouse.")
    selectable.isTextSelectionEnabled = true
    return buildExample("Text selection (IsTextSelectionEnabled)", selectable)
}

// endregion

// region TextBox

/** TextBox page: lines up demos exercising WTextField. */
internal fun buildTextBoxPage(): WComponent {
    val page = buildPage("TextBox", "A control for entering single-line or multi-line text. Try out WTextField.")

    page.add(buildSimpleTextBoxExample())
    page.add(buildHeaderTextBoxExample())
    page.add(buildMultiLineTextBoxExample())
    page.add(buildReadOnlyTextBoxExample())
    page.add(buildTextChangedTextBoxExample())
    return page
}

/** Basic text box: with a placeholder. */
private fun buildSimpleTextBoxExample(): WComponent {
    val textField = WTextField(placeholder = "Enter your name")
    textField.width = 300.0
    return buildExample("Simple text box (PlaceholderText)", textField)
}

/** Header and max length: Header and MaxLength. */
private fun buildHeaderTextBoxExample(): WComponent {
    val textField = WTextField(placeholder = "You can enter up to 10 characters")
    textField.width = 300.0
    textField.header = "Username"
    textField.maxLength = 10
    return buildExample("Header and max length (Header / MaxLength)", textField)
}

/** Multi-line input: AcceptsReturn and TextWrapping. */
private fun buildMultiLineTextBoxExample(): WComponent {
    val textArea = WTextField(placeholder = "Press Enter to add a new line")
    textArea.width = 400.0
    textArea.height = 120.0
    textArea.acceptsReturn = true
    textArea.textWrapping = TextWrapping.WRAP
    return buildExample("Multi-line input (AcceptsReturn / TextWrapping)", textArea)
}

/** Read-only: toggling IsReadOnly. */
private fun buildReadOnlyTextBoxExample(): WComponent {
    val textField = WTextField()
    textField.width = 300.0
    textField.text = "This text is read-only"
    textField.isReadOnly = true

    val toggleButton = WButton("Allow editing")
    toggleButton.addActionListener {
        textField.isReadOnly = !textField.isReadOnly
        toggleButton.text = if (textField.isReadOnly) "Allow editing" else "Make read-only again"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(textField)
    row.add(toggleButton)
    return buildExample("Read-only (IsReadOnly)", row)
}

/** Watching input: mirror the input via TextChanged, and select all via SelectAll. */
private fun buildTextChangedTextBoxExample(): WComponent {
    val mirror = WLabel("The text you type appears here").also { it.foreground = TEXT_SECONDARY }

    val textField = WTextField(placeholder = "Reflected below as you type")
    textField.width = 300.0
    textField.addTextChangedListener { text ->
        mirror.text = if (text.isEmpty()) "The text you type appears here" else text
    }

    val selectAllButton = WButton("Select all")
    selectAllButton.addActionListener { textField.selectAll() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(textField)
    row.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(mirror)
    return buildExample("Watching input (TextChanged / SelectAll)", body)
}

// endregion
