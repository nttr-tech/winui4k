package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTextPane in the same Java form as developers who have used Swing JTextComponent's selection and caret
 * would write.
 * Key operations and the clipboard are checked in WTextPaneSelectionTest.
 */
class WTextPaneSelectionForJavaTest {
    @Test
    void selectExcludesEndAndGetSelectedTextGetSelectionStartAndGetSelectionEndCanBeRead() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("Hello\nWorld");
            pane.select(4, 8);
            assertEquals("o\nWo", pane.getSelectedText());
            assertEquals(4, pane.getSelectionStart());
            assertEquals(8, pane.getSelectionEnd());
            assertEquals(8, pane.getCaretPosition());
        });
    }

    @Test
    void selectAllSetCaretPositionAndOutOfRangeExceptions() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abc");
            pane.selectAll();
            assertEquals("abc", pane.getSelectedText());
            pane.setCaretPosition(1);
            assertEquals("", pane.getSelectedText());
            assertThrows(IllegalArgumentException.class, () -> pane.setCaretPosition(4));
            assertEquals(1, pane.getCaretPosition());
        });
    }

    @Test
    void replaceSelectionReplacesSelectionAndPutsCaretAfterInsertedText() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abcdef");
            pane.select(1, 3);
            pane.replaceSelection("XY!");
            assertEquals("aXY!def", pane.getText());
            assertEquals(4, pane.getCaretPosition());
        });
    }

    @Test
    void lambdaCaretListenerReceivesDotAndMarkOfSelect() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("Hello, World");
            List<String> events = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            pane.addCaretListener(e -> {
                events.add(e.getDot() + "-" + e.getMark());
                sources.add(e.getSource());
            });
            pane.select(7, 12);
            pane.setCaretPosition(0);
            assertEquals(Arrays.asList("12-7", "0-0"), events);
            assertSame(pane, sources.get(0));
        });
    }

    @Test
    void anonymousClassCaretListenerStopsReceivingOnceRemoved() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abc");
            List<Integer> dots = new ArrayList<>();
            WCaretListener listener = new WCaretListener() {
                @Override
                public void caretUpdate(WCaretEvent e) {
                    dots.add(e.getDot());
                }
            };
            pane.addCaretListener(listener);
            assertEquals(1, pane.getCaretListeners().length);
            pane.setCaretPosition(2);
            pane.removeCaretListener(listener);
            pane.setCaretPosition(1);
            assertEquals(Arrays.asList(2), dots);
        });
    }
}
