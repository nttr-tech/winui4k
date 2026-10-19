package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the selection, caret and editing of WTextField the way developers used to Swing's JTextField
 * (JTextComponent) write it in Java.
 * The finer rules of selection (such as clamping the range) are checked in WTextFieldEditTest.
 */
class WTextFieldEditForJavaTest {
    @Test
    void setEditableAndIsEditableToggleWhetherEditingIsAllowed() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            assertTrue(field.isEditable());
            field.setEditable(false);
            assertFalse(field.isEditable());
            assertTrue(field.isReadOnly());
        });
    }

    @Test
    void selectSelectsByStartAndEndReadableByGetSelectionStartAndGetSelectionEnd() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Hello, World");
            field.select(7, 12);
            assertEquals("World", field.getSelectedText());
            assertEquals(7, field.getSelectionStart());
            assertEquals(12, field.getSelectionEnd());
            assertEquals(12, field.getCaretPosition());
        });
    }

    @Test
    void replaceSelectionInsertsTextAtThePositionSetBySetCaretPosition() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Hello World");
            field.setCaretPosition(5);
            field.replaceSelection(",");
            assertEquals("Hello, World", field.getText());
            assertEquals(6, field.getCaretPosition());
        });
    }

    @Test
    void setCaretPositionOutsideTheStringThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("abc");
            assertThrows(IllegalArgumentException.class, () -> field.setCaretPosition(4));
        });
    }

    @Test
    void afterSelectAllSetSelectionStartMovesOnlyTheStart() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("abcdef");
            field.selectAll();
            field.setSelectionStart(2);
            assertEquals("cdef", field.getSelectedText());
            field.setSelectionEnd(4);
            assertEquals("cd", field.getSelectedText());
        });
    }

    @Test
    void afterAnEditCanUndoBecomesTrueAndUndoRevertsIt() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("abc");
            field.setCaretPosition(3);
            field.replaceSelection("d");
            assertTrue(field.canUndo());
            field.undo();
            assertEquals("abc", field.getText());
            assertTrue(field.canRedo());
            field.redo();
            assertEquals("abcd", field.getText());
        });
    }
}
