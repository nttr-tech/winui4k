package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.PasswordRevealMode;
import com.appkitbox.winui4k.SpinButtonPlacementMode;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.TextChangeReason;
import com.appkitbox.winui4k.TextTrimming;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.WAutoSuggestBox;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WPasswordField;
import com.appkitbox.winui4k.WRichTextBlock;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WTextPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Unit;

/*
 * Text category: demo pages for AutoSuggestBox / NumberBox / PasswordBox / RichEditBox / RichTextBlock / TextBlock / TextBox.
 */
final class TextPages {
    private TextPages() {
    }

    // region AutoSuggestBox

    /** AutoSuggestBox page: lines up demos exercising WAutoSuggestBox. */
    static WComponent buildAutoSuggestBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "AutoSuggestBox",
                "A text box that shows a list of suggestions as you type. Try out WAutoSuggestBox.");

        page.add(buildSimpleAutoSuggestBoxExample());
        return page;
    }

    /** Filtering suggestions: replace suggestions via TextChanged, and confirm via QuerySubmitted. */
    private static WComponent buildSimpleAutoSuggestBoxExample() {
        List<String> fruits = Arrays.asList(
                "Apple", "Orange", "Grape", "Peach", "Cherry",
                "Banana", "Pineapple", "Melon", "Strawberry", "Kiwi");
        WLabel result = new WLabel("Confirm with Enter or by choosing a suggestion");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WAutoSuggestBox suggestBox = new WAutoSuggestBox("Enter a fruit name");
        suggestBox.setWidth(300.0);
        suggestBox.setHeader("Search fruits");
        suggestBox.setQueryIcon(Symbol.FIND);
        suggestBox.addTextChangedListener((text, reason) -> {
            // Only filter suggestions on the user's own keystrokes (do nothing for e.g. suggestion selection)
            if (reason == TextChangeReason.USER_INPUT) {
                List<String> filtered = new ArrayList<>();
                for (String fruit : fruits) {
                    if (fruit.contains(text)) {
                        filtered.add(fruit);
                    }
                }
                suggestBox.setSuggestions(filtered);
            }
            return Unit.INSTANCE;
        });
        suggestBox.addQuerySubmittedListener((queryText, chosenSuggestion) -> {
            if (chosenSuggestion != null) {
                result.setText("Committed from a suggestion: " + chosenSuggestion);
            } else {
                result.setText("Committed as typed: " + queryText);
            }
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(suggestBox);
        body.add(result);
        return GalleryScaffold.buildExample("Filtering suggestions (TextChanged / QuerySubmitted)", body);
    }

    // endregion

    // region NumberBox

    /** NumberBox page: lines up demos exercising WSpinner. */
    static WComponent buildNumberBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "NumberBox", "A control for entering, validating, and incrementing/decrementing numbers. Try out WSpinner.");

        page.add(buildExpressionNumberBoxExample());
        page.add(buildSpinButtonNumberBoxExample());
        return page;
    }

    /** Expression input: evaluate expressions like "(5 + 3) * 2" via AcceptsExpression. */
    private static WComponent buildExpressionNumberBoxExample() {
        WLabel result = new WLabel("The confirmed value appears here");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WSpinner spinner = new WSpinner(Double.NaN);
        spinner.setWidth(300.0);
        spinner.setHeader("You can also enter an expression (e.g. (5 + 3) * 2)");
        spinner.setPlaceholderText("1 + 2 * 3");
        spinner.setAcceptsExpression(true);
        spinner.addChangeListener(value -> {
            result.setText(value.isNaN() ? "Not entered" : "Value: " + value);
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(spinner);
        body.add(result);
        return GalleryScaffold.buildExample("Expression input (AcceptsExpression / ValueChanged)", body);
    }

    /** Spin buttons: placement, step, and wrapping of the increment/decrement buttons. */
    private static WComponent buildSpinButtonNumberBoxExample() {
        WSpinner spinner = new WSpinner(10.0);
        spinner.setWidth(150.0);
        spinner.setHeader("0 to 100 (steps of 10, wraps around)");
        spinner.setMinimum(0.0);
        spinner.setMaximum(100.0);
        spinner.setSmallChange(10.0);
        spinner.setLargeChange(25.0);
        spinner.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);
        spinner.setWrapEnabled(true);
        return GalleryScaffold.buildExample("Spin buttons (SpinButtonPlacementMode / SmallChange / IsWrapEnabled)", spinner);
    }

    // endregion

    // region PasswordBox

    /** PasswordBox page: lines up demos exercising WPasswordField. */
    static WComponent buildPasswordBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "PasswordBox", "A control for entering a password as masked characters. Try out WPasswordField.");

        page.add(buildSimplePasswordBoxExample());
        page.add(buildRevealModePasswordBoxExample());
        return page;
    }

    /** Basic password box: simple validation via PasswordChanged. */
    private static WComponent buildSimplePasswordBoxExample() {
        WLabel result = new WLabel("Enter a password of at least 8 characters");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WPasswordField passwordField = new WPasswordField("Enter password");
        passwordField.setWidth(300.0);
        passwordField.setHeader("Password");
        passwordField.addPasswordChangedListener(password -> {
            if (password.isEmpty()) {
                result.setText("Enter a password of at least 8 characters");
            } else if (password.length() < 8) {
                result.setText((8 - password.length()) + " more characters needed");
            } else {
                result.setText("OK (" + password.length() + " characters)");
            }
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(passwordField);
        body.add(result);
        return GalleryScaffold.buildExample("Simple password box (Header / PasswordChanged)", body);
    }

    /** Reveal mode and mask character: PasswordRevealMode and PasswordChar. */
    private static WComponent buildRevealModePasswordBoxExample() {
        WPasswordField hiddenField = new WPasswordField("No reveal button (HIDDEN)");
        hiddenField.setWidth(300.0);
        hiddenField.setPasswordRevealMode(PasswordRevealMode.HIDDEN);

        WPasswordField customCharField = new WPasswordField("Use # as the mask character");
        customCharField.setWidth(300.0);
        customCharField.setPasswordChar("#");

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(hiddenField);
        body.add(customCharField);
        return GalleryScaffold.buildExample("Reveal mode and mask character (PasswordRevealMode / PasswordChar)", body);
    }

    // endregion

    // region RichEditBox

    /** RichEditBox page: lines up demos exercising WTextPane. */
    static WComponent buildRichEditBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "RichEditBox",
                "A control for editing formatted text such as bold and italic. Try out WTextPane.");

        page.add(buildFormattingRichEditBoxExample());
        return page;
    }

    /** Editing formatted text: toggling bold/italic on the selection, plus Undo/Redo. */
    private static WComponent buildFormattingRichEditBoxExample() {
        WTextPane textPane = new WTextPane("Enter text, select it, then press a formatting button");
        textPane.setWidth(400.0);
        textPane.setHeight(150.0);

        WButton boldButton = new WButton("Bold");
        boldButton.addActionListener(() -> {
            textPane.toggleSelectionBold();
            return Unit.INSTANCE;
        });

        WButton italicButton = new WButton("Italic");
        italicButton.addActionListener(() -> {
            textPane.toggleSelectionItalic();
            return Unit.INSTANCE;
        });

        WButton undoButton = new WButton("Undo");
        undoButton.addActionListener(() -> {
            if (textPane.getCanUndo()) {
                textPane.undo();
            }
            return Unit.INSTANCE;
        });

        WButton redoButton = new WButton("Redo");
        redoButton.addActionListener(() -> {
            if (textPane.getCanRedo()) {
                textPane.redo();
            }
            return Unit.INSTANCE;
        });

        WPanel toolbar = new WPanel(8.0, Orientation.HORIZONTAL);
        toolbar.add(boldButton);
        toolbar.add(italicButton);
        toolbar.add(undoButton);
        toolbar.add(redoButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(toolbar);
        body.add(textPane);
        return GalleryScaffold.buildExample("Editing formatted text (Bold / Italic / Undo / Redo)", body);
    }

    // endregion

    // region RichTextBlock

    /** RichTextBlock page: lines up demos exercising WRichTextBlock. */
    static WComponent buildRichTextBlockPage() {
        WPanel page = GalleryScaffold.buildPage(
                "RichTextBlock",
                "A control that displays read-only formatted text mixing bold and italic. Try out WRichTextBlock.");

        page.add(buildSimpleRichTextBlockExample());
        page.add(buildSelectionRichTextBlockExample());
        return page;
    }

    /** Displaying formatted text: composing paragraphs from Run / Bold / Italic / Underline. */
    private static WComponent buildSimpleRichTextBlockExample() {
        WRichTextBlock richTextBlock = new WRichTextBlock();
        richTextBlock.setWidth(400.0);
        richTextBlock.addParagraph(paragraph -> {
            paragraph.run("RichTextBlock can mix ");
            paragraph.bold("Bold");
            paragraph.run(", ");
            paragraph.italic("Italic");
            paragraph.run(", and ");
            paragraph.underline("underlined");
            paragraph.run(" text together in a single block.");
            return Unit.INSTANCE;
        });
        richTextBlock.addParagraph(paragraph -> {
            paragraph.run("Adding multiple paragraphs displays them with spacing in between.");
            return Unit.INSTANCE;
        });
        return GalleryScaffold.buildExample(
                "Displaying formatted text (Paragraph / Run / Bold / Italic / Underline)", richTextBlock);
    }

    /** Text selection: SelectAll and reading SelectedText. */
    private static WComponent buildSelectionRichTextBlockExample() {
        WLabel result = new WLabel("The selected text appears here");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WRichTextBlock richTextBlock = new WRichTextBlock();
        richTextBlock.setWidth(400.0);
        richTextBlock.addParagraph(paragraph -> {
            paragraph.run("This text can be selected with the mouse. Drag to select, then press the button below.");
            return Unit.INSTANCE;
        });

        WButton readButton = new WButton("Get selected text");
        readButton.addActionListener(() -> {
            String selected = richTextBlock.getSelectedText();
            result.setText(selected.isEmpty() ? "Nothing is selected" : "Selected: " + selected);
            return Unit.INSTANCE;
        });

        WButton selectAllButton = new WButton("Select all");
        selectAllButton.addActionListener(() -> {
            richTextBlock.selectAll();
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(readButton);
        row.add(selectAllButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(richTextBlock);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample("Text selection (IsTextSelectionEnabled / SelectedText / SelectAll)", body);
    }

    // endregion

    // region TextBlock

    /** TextBlock page: lines up demos exercising WLabel. */
    static WComponent buildTextBlockPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TextBlock", "A basic control that displays read-only text. Try out WLabel.");

        page.add(buildSimpleTextBlockExample());
        page.add(buildTextBlockStyleExample());
        page.add(buildTextBlockWrappingExample());
        page.add(buildTextBlockSelectionExample());
        return page;
    }

    /** Basic text display. */
    private static WComponent buildSimpleTextBlockExample() {
        return GalleryScaffold.buildExample("Simple text", new WLabel("I am a TextBlock."));
    }

    /** Text appearance: change font size, weight, and color. */
    private static WComponent buildTextBlockStyleExample() {
        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        WLabel sized = new WLabel("Text at font size 18");
        sized.setFontSize(18.0);
        body.add(sized);
        WLabel weighted = new WLabel("SemiBold (600) text");
        weighted.setFontWeight(600);
        body.add(weighted);
        WLabel colored = new WLabel("Colored text");
        colored.setForeground(new WColor(0, 95, 184, 255));
        body.add(colored);
        return GalleryScaffold.buildExample("Changing the style (FontSize / FontWeight / Foreground)", body);
    }

    /** Wrapping and trimming: handling text that doesn't fit the width. */
    private static WComponent buildTextBlockWrappingExample() {
        String longText = "This text is long enough that it doesn't fit the control's width, so you can see wrapping and trimming in action.";

        WLabel wrapped = new WLabel(longText);
        wrapped.setWidth(300.0);
        wrapped.setTextWrapping(TextWrapping.WRAP);

        WLabel trimmed = new WLabel(longText);
        trimmed.setWidth(300.0);
        trimmed.setTextTrimming(TextTrimming.CHARACTER_ELLIPSIS);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        WLabel wrappedHeader = new WLabel("TextWrapping.WRAP:");
        wrappedHeader.setForeground(GalleryTheme.TEXT_SECONDARY());
        body.add(wrappedHeader);
        body.add(wrapped);
        WLabel trimmedHeader = new WLabel("TextTrimming.CHARACTER_ELLIPSIS:");
        trimmedHeader.setForeground(GalleryTheme.TEXT_SECONDARY());
        body.add(trimmedHeader);
        body.add(trimmed);
        return GalleryScaffold.buildExample("Wrapping and trimming (TextWrapping / TextTrimming)", body);
    }

    /** Text selection: allow selecting and copying with the mouse. */
    private static WComponent buildTextBlockSelectionExample() {
        WLabel selectable = new WLabel("This text can be selected by dragging with the mouse.");
        selectable.setTextSelectionEnabled(true);
        return GalleryScaffold.buildExample("Text selection (IsTextSelectionEnabled)", selectable);
    }

    // endregion

    // region TextBox

    /** TextBox page: lines up demos exercising WTextField. */
    static WComponent buildTextBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TextBox", "A control for entering single-line or multi-line text. Try out WTextField.");

        page.add(buildSimpleTextBoxExample());
        page.add(buildHeaderTextBoxExample());
        page.add(buildMultiLineTextBoxExample());
        page.add(buildReadOnlyTextBoxExample());
        page.add(buildTextChangedTextBoxExample());
        return page;
    }

    /** Basic text box: with a placeholder. */
    private static WComponent buildSimpleTextBoxExample() {
        WTextField textField = new WTextField("Enter your name");
        textField.setWidth(300.0);
        return GalleryScaffold.buildExample("Simple text box (PlaceholderText)", textField);
    }

    /** Header and max length: Header and MaxLength. */
    private static WComponent buildHeaderTextBoxExample() {
        WTextField textField = new WTextField("You can enter up to 10 characters");
        textField.setWidth(300.0);
        textField.setHeader("Username");
        textField.setMaxLength(10);
        return GalleryScaffold.buildExample("Header and max length (Header / MaxLength)", textField);
    }

    /** Multi-line input: AcceptsReturn and TextWrapping. */
    private static WComponent buildMultiLineTextBoxExample() {
        WTextField textArea = new WTextField("Press Enter to add a new line");
        textArea.setWidth(400.0);
        textArea.setHeight(120.0);
        textArea.setAcceptsReturn(true);
        textArea.setTextWrapping(TextWrapping.WRAP);
        return GalleryScaffold.buildExample("Multi-line input (AcceptsReturn / TextWrapping)", textArea);
    }

    /** Read-only: toggling IsReadOnly. */
    private static WComponent buildReadOnlyTextBoxExample() {
        WTextField textField = new WTextField("");
        textField.setWidth(300.0);
        textField.setText("This text is read-only");
        textField.setReadOnly(true);

        WButton toggleButton = new WButton("Allow editing");
        toggleButton.addActionListener(() -> {
            textField.setReadOnly(!textField.isReadOnly());
            toggleButton.setText(textField.isReadOnly() ? "Allow editing" : "Make read-only again");
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(textField);
        row.add(toggleButton);
        return GalleryScaffold.buildExample("Read-only (IsReadOnly)", row);
    }

    /** Watching input: mirror the input via TextChanged, and select all via SelectAll. */
    private static WComponent buildTextChangedTextBoxExample() {
        WLabel mirror = new WLabel("The text you type appears here");
        mirror.setForeground(GalleryTheme.TEXT_SECONDARY());

        WTextField textField = new WTextField("Reflected below as you type");
        textField.setWidth(300.0);
        textField.addTextChangedListener(text -> {
            mirror.setText(text.isEmpty() ? "The text you type appears here" : text);
            return Unit.INSTANCE;
        });

        WButton selectAllButton = new WButton("Select all");
        selectAllButton.addActionListener(() -> {
            textField.selectAll();
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(textField);
        row.add(selectAllButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(row);
        body.add(mirror);
        return GalleryScaffold.buildExample("Watching input (TextChanged / SelectAll)", body);
    }

    // endregion
}
