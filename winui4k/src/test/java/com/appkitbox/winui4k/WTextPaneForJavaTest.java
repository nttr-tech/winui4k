package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for reading and writing WTextPane's text from Java written the same way as by developers used to Swing's
 * JTextArea. Selection and caret are in WTextPaneSelectionForJavaTest, and DocumentListener is in
 * WTextPaneDocumentForJavaTest.
 */
class WTextPaneForJavaTest {
    @Test
    void setTextAndGetTextReturnTheTextWithoutTrailingParagraphMark() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            assertEquals("", pane.getText());
            pane.setText("Ln1\nLn2");
            assertEquals("Ln1\nLn2", pane.getText());
            assertEquals(7, pane.getLength());
            pane.setText("trail\n");
            assertEquals("trail\n", pane.getText());
        });
    }

    @Test
    void appendInsertAndReplaceRangeWorkAtTheSamePositionsAsJTextArea() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.append("world");
            pane.insert("Hello, ", 0);
            assertEquals("Hello, world", pane.getText());
            pane.replaceRange("WinUI", 7, 12);
            assertEquals("Hello, WinUI", pane.getText());
            pane.append(null);
            pane.insert("", 3);
            assertEquals("Hello, WinUI", pane.getText());
        });
    }

    @Test
    void outOfRangePositionThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abc");
            assertThrows(IllegalArgumentException.class, () -> pane.insert("x", 4));
            assertThrows(IllegalArgumentException.class, () -> pane.replaceRange("x", 2, 1));
            assertThrows(IllegalArgumentException.class, () -> pane.replaceRange("x", 0, 4));
            assertThrows(IllegalArgumentException.class, () -> pane.getLineStartOffset(1));
            assertEquals("abc", pane.getText());
        });
    }

    @Test
    void lineCountAndLineStartAndEndOffsetsCanBeReadWithGetLineXxx() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("ab\ncde\nf");
            assertEquals(3, pane.getLineCount());
            assertEquals(1, pane.getLineOfOffset(3));
            assertEquals(3, pane.getLineStartOffset(1));
            assertEquals(7, pane.getLineEndOffset(1));
            assertEquals(8, pane.getLineEndOffset(2));
        });
    }

    @Test
    void setLineWrapAndSetWrapStyleWordCanBeUsedWithTheSameNamesAsJTextArea() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            // The default lineWrap is decided by WinUI's default style after placement (unlike JTextArea's false), so set it explicitly and check
            pane.setLineWrap(false);
            assertFalse(pane.getLineWrap());
            assertFalse(pane.getWrapStyleWord());
            pane.setLineWrap(true);
            pane.setWrapStyleWord(true);
            assertTrue(pane.getLineWrap());
            assertTrue(pane.getWrapStyleWord());
            assertEquals(TextWrapping.WRAP_WHOLE_WORDS, pane.getTextWrapping());
        });
    }

    @Test
    void isEditableIsTheInverseOfIsReadOnlyAndSetTextOrAppendCanChangeTheTextEvenWhenFalse() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            assertTrue(pane.isEditable());
            pane.setEditable(false);
            assertTrue(pane.isReadOnly());
            pane.setText("Fixed");
            pane.append(" text");
            assertEquals("Fixed text", pane.getText());
        });
    }

    @Test
    void constructorWithRowsAndColumnsAndSetRowsAndSetColumns() {
        onUiThread(() -> {
            WTextPane sized = new WTextPane(10, 40);
            assertEquals(10, sized.getRows());
            assertEquals(40, sized.getColumns());
            WTextPane withText = new WTextPane("Initial", 3, 20);
            assertEquals("Initial", withText.getText());
            assertEquals(3, withText.getRows());
            sized.setRows(5);
            sized.setColumns(30);
            assertEquals(5, sized.getRows());
            assertEquals(30, sized.getColumns());
            assertThrows(IllegalArgumentException.class, () -> sized.setRows(-1));
            assertThrows(IllegalArgumentException.class, () -> sized.setColumns(-1));
            assertEquals(5, sized.getRows());
            assertEquals(30, sized.getColumns());
        });
    }

    @Test
    void thePlaceholderConstructorCanStillBeCalledWithASingleString() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane("Type here");
            assertEquals("Type here", pane.getPlaceholderText());
            assertEquals("", pane.getText());
        });
    }
}
