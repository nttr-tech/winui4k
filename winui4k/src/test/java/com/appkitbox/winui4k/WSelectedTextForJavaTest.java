package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests that check, in Java as Swing developers write it, that it returns null when nothing is selected, as JTextComponent.getSelectedText does
 * (the common way of enabling "Cut" and "Copy" with {@code field.getSelectedText() != null} when opening a menu).
 */
class WSelectedTextForJavaTest {
    @Test
    void wTextFieldGetSelectedTextIsNullWithoutSelectionReturnsTheTextWhenSelectedAndReturnsToNullWhenDeselected() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            assertNull(field.getSelectedText());
            field.setText("Hello World");
            assertNull(field.getSelectedText());
            field.select(6, 11);
            assertEquals("World", field.getSelectedText());
            field.setCaretPosition(3);
            assertNull(field.getSelectedText());
        });
    }

    @Test
    void selectingAllOfAWTextFieldReturnsTheWholeTextAndSelectingTheSamePositionGivesNull() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("abc");
            field.selectAll();
            assertEquals("abc", field.getSelectedText());
            field.select(2, 2);
            assertNull(field.getSelectedText());
        });
    }

    @Test
    void wTextPaneGetSelectedTextIsNullWithoutSelectionAndReturnsTheTextWhenSelectedAcrossLineBreaks() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            assertNull(pane.getSelectedText());
            pane.setText("Hello\nWorld");
            pane.setCaretPosition(0);
            assertNull(pane.getSelectedText());
            pane.select(4, 8);
            assertEquals("o\nWo", pane.getSelectedText());
            pane.setCaretPosition(8);
            assertNull(pane.getSelectedText());
        });
    }
}
