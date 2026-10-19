package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTextField's DocumentListener in Java, in the form a developer who has used Swing's DocumentListener
 * would write. Where Swing has you write {@code field.getDocument().addDocumentListener(...)}, it is added directly to
 * the input field. Notifications from key input are checked in WTextFieldDocumentTest.
 */
class WTextFieldDocumentForJavaTest {
    @Test
    void anonymousDocumentListenerReceivesRemoveAndInsertOfSetTextInOrder() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("old");
            List<String> events = new ArrayList<>();
            field.addDocumentListener(new WDocumentListener() {
                @Override
                public void insertUpdate(WDocumentEvent e) {
                    events.add("insert " + e.getOffset() + " " + e.getLength());
                }

                @Override
                public void removeUpdate(WDocumentEvent e) {
                    events.add("remove " + e.getOffset() + " " + e.getLength());
                }

                @Override
                public void changedUpdate(WDocumentEvent e) {
                    events.add("changed");
                }
            });
            field.setText("new!");
            assertEquals(Arrays.asList("remove 0 3", "insert 0 4"), events);
        });
    }

    @Test
    void getTypeCanBeComparedWithEventTypeConstantsAndGetSourceIsTheField() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            List<WDocumentEvent> received = new ArrayList<>();
            WDocumentListener listener = new WDocumentListener() {
                @Override
                public void insertUpdate(WDocumentEvent e) {
                    received.add(e);
                }

                @Override
                public void removeUpdate(WDocumentEvent e) {
                    received.add(e);
                }

                @Override
                public void changedUpdate(WDocumentEvent e) {
                    received.add(e);
                }
            };
            field.addDocumentListener(listener);
            field.setText("abc");
            assertEquals(1, received.size());
            assertSame(WDocumentEvent.EventType.INSERT, received.get(0).getType());
            assertSame(field, received.get(0).getSource());
            assertSame(field, received.get(0).getComponent());
            assertEquals(1, field.getDocumentListeners().length);

            field.removeDocumentListener(listener);
            field.setText("");
            assertEquals(1, received.size());
            assertEquals(0, field.getDocumentListeners().length);
        });
    }
}
