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
import java.util.Locale;


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
                Messages.get("autosuggestbox.description"));

        page.add(buildSimpleAutoSuggestBoxExample());
        return page;
    }

    /** Filtering suggestions: replace suggestions via TextChanged, and confirm via QuerySubmitted. */
    private static WComponent buildSimpleAutoSuggestBoxExample() {
        List<String> fruits = Arrays.asList(
                Messages.get("autosuggestbox.fruit.apple"),
                Messages.get("autosuggestbox.fruit.orange"),
                Messages.get("autosuggestbox.fruit.grape"),
                Messages.get("autosuggestbox.fruit.peach"),
                Messages.get("autosuggestbox.fruit.cherry"),
                Messages.get("autosuggestbox.fruit.banana"),
                Messages.get("autosuggestbox.fruit.pineapple"),
                Messages.get("autosuggestbox.fruit.melon"),
                Messages.get("autosuggestbox.fruit.strawberry"),
                Messages.get("autosuggestbox.fruit.kiwi"));
        WLabel result = new WLabel(Messages.get("autosuggestbox.result.initial"));
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WAutoSuggestBox suggestBox = new WAutoSuggestBox(Messages.get("autosuggestbox.placeholder"));
        suggestBox.setWidth(300.0);
        suggestBox.setHeader(Messages.get("autosuggestbox.header"));
        suggestBox.setQueryIcon(Symbol.FIND);
        suggestBox.addTextChangedListener((text, reason) -> {
            // Only filter suggestions on the user's own keystrokes (do nothing for e.g. suggestion selection)
            if (reason == TextChangeReason.USER_INPUT) {
                List<String> filtered = new ArrayList<>();
                for (String fruit : fruits) {
                    if (fruit.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
                        filtered.add(fruit);
                    }
                }
                suggestBox.setSuggestions(filtered);
            }
        });
        suggestBox.addQuerySubmittedListener((queryText, chosenSuggestion) -> {
            if (chosenSuggestion != null) {
                result.setText(Messages.get("autosuggestbox.result.suggestion", chosenSuggestion));
            } else {
                result.setText(Messages.get("autosuggestbox.result.query", queryText));
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(suggestBox);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("autosuggestbox.example.filter.title"), body);
    }

    // endregion

    // region NumberBox

    /** NumberBox page: lines up demos exercising WSpinner. */
    static WComponent buildNumberBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "NumberBox", Messages.get("numberbox.description"));

        page.add(buildExpressionNumberBoxExample());
        page.add(buildSpinButtonNumberBoxExample());
        return page;
    }

    /** Expression input: evaluate expressions like "(5 + 3) * 2" via AcceptsExpression. */
    private static WComponent buildExpressionNumberBoxExample() {
        WLabel result = new WLabel(Messages.get("numberbox.result.initial"));
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WSpinner spinner = new WSpinner(Double.NaN);
        spinner.setWidth(300.0);
        spinner.setHeader(Messages.get("numberbox.expression.header"));
        spinner.setPlaceholderText("1 + 2 * 3");
        spinner.setAcceptsExpression(true);
        spinner.setSpinButtonPlacementMode(SpinButtonPlacementMode.HIDDEN); // No spin buttons, as in the original Gallery
        spinner.addChangeListener(e -> {
            double value = spinner.getValue();
            result.setText(Double.isNaN(value)
                    ? Messages.get("numberbox.result.empty")
                    : Messages.get("numberbox.result.value", String.valueOf(value)));
        });

        WPanel body = new WPanel(8.0);
        body.add(spinner);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("numberbox.example.expression.title"), body);
    }

    /** Spin buttons: placement, step, and wrapping of the increment/decrement buttons. */
    private static WComponent buildSpinButtonNumberBoxExample() {
        WSpinner spinner = new WSpinner(10.0);
        spinner.setWidth(200.0);
        spinner.setHeader(Messages.get("numberbox.spin.header"));
        spinner.setMinimum(0.0);
        spinner.setMaximum(100.0);
        spinner.setSmallChange(10.0);
        spinner.setLargeChange(25.0);
        spinner.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);
        spinner.setWrapEnabled(true);
        return GalleryScaffold.buildExample(Messages.get("numberbox.example.spin.title"), spinner);
    }

    // endregion

    // region PasswordBox

    /** PasswordBox page: lines up demos exercising WPasswordField. */
    static WComponent buildPasswordBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "PasswordBox", Messages.get("passwordbox.description"));

        page.add(buildSimplePasswordBoxExample());
        page.add(buildRevealModePasswordBoxExample());
        return page;
    }

    /** Basic password box: simple validation via PasswordChanged. */
    private static WComponent buildSimplePasswordBoxExample() {
        WLabel result = new WLabel(Messages.get("passwordbox.result.initial"));
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WPasswordField passwordField = new WPasswordField();
        passwordField.setPlaceholderText(Messages.get("passwordbox.placeholder"));
        passwordField.setWidth(300.0);
        passwordField.setHeader(Messages.get("passwordbox.header"));
        passwordField.setPasswordRevealMode(PasswordRevealMode.PEEK); // show the reveal button (the default is HIDDEN, no reveal button, as in Swing)
        passwordField.addPasswordChangedListener(password -> {
            if (password.isEmpty()) {
                result.setText(Messages.get("passwordbox.result.initial"));
            } else if (password.length() < 8) {
                result.setText(Messages.get("passwordbox.result.short", 8 - password.length()));
            } else {
                result.setText(Messages.get("passwordbox.result.ok", String.valueOf(password.length())));
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(passwordField);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("passwordbox.example.simple.title"), body);
    }

    /** Reveal mode and mask character: PasswordRevealMode and PasswordChar. */
    private static WComponent buildRevealModePasswordBoxExample() {
        WPasswordField hiddenField = new WPasswordField();
        hiddenField.setPlaceholderText(Messages.get("passwordbox.placeholder.hidden"));
        hiddenField.setWidth(300.0);

        WPasswordField visibleField = new WPasswordField();
        visibleField.setPlaceholderText(Messages.get("passwordbox.placeholder.visible"));
        visibleField.setWidth(300.0);
        visibleField.setEchoChar((char) 0);

        WPasswordField customCharField = new WPasswordField();
        customCharField.setPlaceholderText(Messages.get("passwordbox.placeholder.custom"));
        customCharField.setWidth(300.0);
        customCharField.setPasswordChar("#");

        WPanel body = new WPanel(8.0);
        body.add(hiddenField);
        body.add(visibleField);
        body.add(customCharField);
        return GalleryScaffold.buildExample(Messages.get("passwordbox.example.reveal.title"), body);
    }

    // endregion

    // region RichEditBox

    /** RichEditBox page: lines up demos exercising WTextPane. */
    static WComponent buildRichEditBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "RichEditBox",
                Messages.get("richeditbox.description"));

        page.add(buildFormattingRichEditBoxExample());
        return page;
    }

    /** Editing formatted text: toggling bold/italic on the selection, plus Undo/Redo. */
    private static WComponent buildFormattingRichEditBoxExample() {
        WTextPane textPane = new WTextPane();
        textPane.setPlaceholderText(Messages.get("richeditbox.placeholder"));
        textPane.setWidth(400.0);
        textPane.setHeight(150.0);
        textPane.setLineWrap(true); // Wrap at the width, as with the RichEditBox of the original Gallery (WTextPane does not wrap by default, as with JTextArea)

        WButton boldButton = new WButton(Messages.get("richeditbox.bold"));
        boldButton.addActionListener(() -> {
            textPane.toggleSelectionBold();
        });

        WButton italicButton = new WButton(Messages.get("richeditbox.italic"));
        italicButton.addActionListener(() -> {
            textPane.toggleSelectionItalic();
        });

        WButton undoButton = new WButton(Messages.get("richeditbox.undo"));
        undoButton.addActionListener(() -> {
            if (textPane.canUndo()) {
                textPane.undo();
            }
        });

        WButton redoButton = new WButton(Messages.get("richeditbox.redo"));
        redoButton.addActionListener(() -> {
            if (textPane.canRedo()) {
                textPane.redo();
            }
        });

        WPanel toolbar = new WPanel(8.0, Orientation.HORIZONTAL);
        toolbar.add(boldButton);
        toolbar.add(italicButton);
        toolbar.add(undoButton);
        toolbar.add(redoButton);

        WPanel body = new WPanel(8.0);
        body.add(toolbar);
        body.add(textPane);
        return GalleryScaffold.buildExample(Messages.get("richeditbox.example.formatting.title"), body);
    }

    // endregion

    // region RichTextBlock

    /** RichTextBlock page: lines up demos exercising WRichTextBlock. */
    static WComponent buildRichTextBlockPage() {
        WPanel page = GalleryScaffold.buildPage(
                "RichTextBlock",
                Messages.get("richtextblock.description"));

        page.add(buildSimpleRichTextBlockExample());
        page.add(buildSelectionRichTextBlockExample());
        return page;
    }

    /** Displaying formatted text: composing paragraphs from Run / Bold / Italic / Underline. */
    private static WComponent buildSimpleRichTextBlockExample() {
        WRichTextBlock richTextBlock = new WRichTextBlock();
        richTextBlock.setWidth(400.0);
        richTextBlock.addParagraph(paragraph -> {
            paragraph.run(Messages.get("richtextblock.sample.lead"));
            paragraph.bold(Messages.get("richtextblock.sample.bold"));
            paragraph.run(Messages.get("richtextblock.sample.separator1"));
            paragraph.italic(Messages.get("richtextblock.sample.italic"));
            paragraph.run(Messages.get("richtextblock.sample.separator2"));
            paragraph.underline(Messages.get("richtextblock.sample.underline"));
            paragraph.run(Messages.get("richtextblock.sample.tail"));
        });
        richTextBlock.addParagraph(paragraph -> {
            paragraph.run(Messages.get("richtextblock.sample.paragraph2"));
        });
        return GalleryScaffold.buildExample(
                Messages.get("richtextblock.example.simple.title"), richTextBlock);
    }

    /** Text selection: SelectAll and reading SelectedText. */
    private static WComponent buildSelectionRichTextBlockExample() {
        WLabel result = new WLabel(Messages.get("richtextblock.result.initial"));
        result.setForeground(GalleryTheme.TEXT_SECONDARY());

        WRichTextBlock richTextBlock = new WRichTextBlock();
        richTextBlock.setWidth(400.0);
        richTextBlock.addParagraph(paragraph -> {
            paragraph.run(Messages.get("richtextblock.selection.text"));
        });

        WButton readButton = new WButton(Messages.get("richtextblock.getselection"));
        readButton.addActionListener(() -> {
            String selected = richTextBlock.getSelectedText();
            result.setText(selected.isEmpty()
                    ? Messages.get("richtextblock.result.none")
                    : Messages.get("richtextblock.result.selected", selected));
        });

        WButton selectAllButton = new WButton(Messages.get("richtextblock.selectall"));
        selectAllButton.addActionListener(() -> {
            richTextBlock.selectAll();
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(readButton);
        row.add(selectAllButton);

        WPanel body = new WPanel(8.0);
        body.add(richTextBlock);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("richtextblock.example.selection.title"), body);
    }

    // endregion

    // region TextBlock

    /** TextBlock page: lines up demos exercising WLabel. */
    static WComponent buildTextBlockPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TextBlock", Messages.get("textblock.description"));

        page.add(buildSimpleTextBlockExample());
        page.add(buildTextBlockStyleExample());
        page.add(buildTextBlockWrappingExample());
        page.add(buildTextBlockSelectionExample());
        return page;
    }

    /** Basic text display. */
    private static WComponent buildSimpleTextBlockExample() {
        return GalleryScaffold.buildExample(Messages.get("textblock.example.simple.title"), new WLabel("I am a TextBlock."));
    }

    /** Text appearance: change font size, weight, and color. */
    private static WComponent buildTextBlockStyleExample() {
        WPanel body = new WPanel(8.0);
        WLabel sized = new WLabel(Messages.get("textblock.style.size"));
        sized.setFontSize(18.0);
        body.add(sized);
        WLabel weighted = new WLabel(Messages.get("textblock.style.weight"));
        weighted.setFontWeight(600);
        body.add(weighted);
        WLabel colored = new WLabel(Messages.get("textblock.style.color"));
        colored.setForeground(new WColor(0, 95, 184, 255));
        body.add(colored);
        return GalleryScaffold.buildExample(Messages.get("textblock.example.style.title"), body);
    }

    /** Wrapping and trimming: handling text that doesn't fit the width. */
    private static WComponent buildTextBlockWrappingExample() {
        String longText = Messages.get("textblock.longtext");

        WLabel wrapped = new WLabel(longText);
        wrapped.setWidth(300.0);
        wrapped.setTextWrapping(TextWrapping.WRAP);

        WLabel trimmed = new WLabel(longText);
        trimmed.setWidth(300.0);
        trimmed.setTextTrimming(TextTrimming.CHARACTER_ELLIPSIS);

        WPanel body = new WPanel(8.0);
        WLabel wrappedHeader = new WLabel("TextWrapping.WRAP:");
        wrappedHeader.setForeground(GalleryTheme.TEXT_SECONDARY());
        body.add(wrappedHeader);
        body.add(wrapped);
        WLabel trimmedHeader = new WLabel("TextTrimming.CHARACTER_ELLIPSIS:");
        trimmedHeader.setForeground(GalleryTheme.TEXT_SECONDARY());
        body.add(trimmedHeader);
        body.add(trimmed);
        return GalleryScaffold.buildExample(Messages.get("textblock.example.wrapping.title"), body);
    }

    /** Text selection: allow selecting and copying with the mouse. */
    private static WComponent buildTextBlockSelectionExample() {
        WLabel selectable = new WLabel(Messages.get("textblock.selection.text"));
        selectable.setTextSelectionEnabled(true);
        return GalleryScaffold.buildExample(Messages.get("textblock.example.selection.title"), selectable);
    }

    // endregion

    // region TextBox

    /** TextBox page: lines up demos exercising WTextField. */
    static WComponent buildTextBoxPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TextBox", Messages.get("textbox.description"));

        page.add(buildSimpleTextBoxExample());
        page.add(buildHeaderTextBoxExample());
        page.add(buildMultiLineTextBoxExample());
        page.add(buildReadOnlyTextBoxExample());
        page.add(buildTextChangedTextBoxExample());
        return page;
    }

    /** Basic text box: with a placeholder. */
    private static WComponent buildSimpleTextBoxExample() {
        WTextField textField = new WTextField();
        textField.setPlaceholderText(Messages.get("textbox.placeholder.name"));
        textField.setWidth(300.0);
        return GalleryScaffold.buildExample(Messages.get("textbox.example.simple.title"), textField);
    }

    /** Header and max length: Header and MaxLength. */
    private static WComponent buildHeaderTextBoxExample() {
        WTextField textField = new WTextField();
        textField.setPlaceholderText(Messages.get("textbox.placeholder.maxlength"));
        textField.setWidth(300.0);
        textField.setHeader(Messages.get("textbox.header.username"));
        textField.setMaxLength(10);
        return GalleryScaffold.buildExample(Messages.get("textbox.example.header.title"), textField);
    }

    /** Multi-line input: AcceptsReturn and TextWrapping. */
    private static WComponent buildMultiLineTextBoxExample() {
        WTextField textArea = new WTextField();
        textArea.setPlaceholderText(Messages.get("textbox.placeholder.multiline"));
        textArea.setWidth(400.0);
        textArea.setHeight(120.0);
        textArea.setAcceptsReturn(true);
        textArea.setTextWrapping(TextWrapping.WRAP);
        return GalleryScaffold.buildExample(Messages.get("textbox.example.multiline.title"), textArea);
    }

    /** Read-only: toggling IsReadOnly. */
    private static WComponent buildReadOnlyTextBoxExample() {
        WTextField textField = new WTextField();
        textField.setWidth(300.0);
        textField.setText(Messages.get("textbox.readonly.text"));
        textField.setReadOnly(true);

        WButton toggleButton = new WButton(Messages.get("textbox.readonly.allow"));
        toggleButton.addActionListener(() -> {
            textField.setReadOnly(!textField.isReadOnly());
            toggleButton.setText(textField.isReadOnly() ? Messages.get("textbox.readonly.allow") : Messages.get("textbox.readonly.restore"));
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(textField);
        row.add(toggleButton);
        return GalleryScaffold.buildExample(Messages.get("textbox.example.readonly.title"), row);
    }

    /** Watching input: mirror the input via TextChanged, and select all via SelectAll. */
    private static WComponent buildTextChangedTextBoxExample() {
        WLabel mirror = new WLabel(Messages.get("textbox.mirror.initial"));
        mirror.setForeground(GalleryTheme.TEXT_SECONDARY());

        WTextField textField = new WTextField();
        textField.setPlaceholderText(Messages.get("textbox.placeholder.mirror"));
        textField.setWidth(300.0);
        textField.addTextChangedListener(text -> {
            mirror.setText(text.isEmpty() ? Messages.get("textbox.mirror.initial") : text);
        });

        WButton selectAllButton = new WButton(Messages.get("textbox.selectall"));
        selectAllButton.addActionListener(() -> {
            textField.selectAll();
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(textField);
        row.add(selectAllButton);

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(mirror);
        return GalleryScaffold.buildExample(Messages.get("textbox.example.textchanged.title"), body);
    }

    // endregion
}
