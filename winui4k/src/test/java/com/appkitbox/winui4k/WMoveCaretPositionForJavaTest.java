package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that make selections with setCaretPosition and moveCaretPosition in the form a Swing developer writes in Java
 * (the form of Swing code that selects a found word from the back).
 */
class WMoveCaretPositionForJavaTest {
    @Test
    void setCaretPositionAndMoveCaretPositionSelectFromBackToFront_getCaretPositionAndCaretEventReturnTheMovedToPosition() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("find the word");
            List<String> events = new ArrayList<>();
            field.addCaretListener(e -> events.add(e.getDot() + "/" + e.getMark()));
            field.setCaretPosition(13);
            field.moveCaretPosition(9);
            assertEquals("word", field.getSelectedText());
            assertEquals(9, field.getCaretPosition());
            assertEquals(9, field.getSelectionStart());
            assertEquals(13, field.getSelectionEnd());
            assertEquals("9/13", events.get(events.size() - 1));
        });
    }

    @Test
    void theSameFormWorksForWTextPane_outOfRangeThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("line 1\nline 2");
            pane.setCaretPosition(7);
            pane.moveCaretPosition(13);
            assertEquals("line 2", pane.getSelectedText());
            assertEquals(13, pane.getCaretPosition());
            assertThrows(IllegalArgumentException.class, () -> pane.moveCaretPosition(14));
        });
    }
}
