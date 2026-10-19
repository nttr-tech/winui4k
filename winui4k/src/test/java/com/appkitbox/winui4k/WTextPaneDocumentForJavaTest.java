package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests using WTextPane's DocumentListener in Java, in the form developers who have used Swing's DocumentListener write.
 * Where Swing would write {@code pane.getDocument().addDocumentListener(...)}, the listener is attached directly to the
 * text pane.
 * Notifications from key input are checked in WTextPaneDocumentTest.
 */
class WTextPaneDocumentForJavaTest {
    private static WDocumentListener recorder(WTextPane pane, List<String> events) {
        return new WDocumentListener() {
            @Override
            public void insertUpdate(WDocumentEvent e) {
                events.add("insert " + e.getOffset() + " " + e.getLength());
                assertSame(pane, e.getSource());
            }

            @Override
            public void removeUpdate(WDocumentEvent e) {
                events.add("remove " + e.getOffset() + " " + e.getLength());
            }

            @Override
            public void changedUpdate(WDocumentEvent e) {
                events.add("changed " + e.getOffset() + " " + e.getLength());
            }
        };
    }

    @Test
    void anonymousDocumentListenerReceivesRemoveAndInsertOfSetTextInOrder() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("old");
            List<String> events = new ArrayList<>();
            pane.addDocumentListener(recorder(pane, events));
            pane.setText("new!\nline");
            assertEquals(Arrays.asList("remove 0 3", "insert 0 9"), events);
        });
    }

    @Test
    void appendInsertAndReplaceRangeArriveAtSamePositionsAsJTextArea() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abc");
            List<String> events = new ArrayList<>();
            pane.addDocumentListener(recorder(pane, events));
            pane.append("def");
            pane.insert("X", 0);
            pane.replaceRange("", 1, 3);
            assertEquals(Arrays.asList("insert 3 3", "insert 0 1", "remove 1 2"), events);
        });
    }

    @Test
    void togglingBoldArrivesAsChangedUpdate() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abcdef");
            List<String> events = new ArrayList<>();
            pane.addDocumentListener(recorder(pane, events));
            pane.select(1, 4);
            pane.toggleSelectionBold();
            assertEquals(Arrays.asList("changed 1 3"), events);
        });
    }

    @Test
    void removedListenerReceivesNothingAndDisappearsFromGetDocumentListeners() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            List<String> events = new ArrayList<>();
            WDocumentListener listener = recorder(pane, events);
            pane.addDocumentListener(listener);
            assertEquals(1, pane.getDocumentListeners().length);
            pane.removeDocumentListener(listener);
            pane.setText("after removal");
            assertEquals(0, pane.getDocumentListeners().length);
            assertEquals(0, events.size());
        });
    }
}
