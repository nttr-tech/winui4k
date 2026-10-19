package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for using WPasswordField from Java written the same way as by developers used to Swing's JPasswordField.
 * getPassword() returns a char[], and the idiom of zeroing the used array with Arrays.fill works as is.
 * Behavior on the Enter key is in WPasswordFieldActionTest, and DocumentListener with key input is in
 * WPasswordFieldDocumentTest.
 */
class WPasswordFieldForJavaTest {
    @Test
    void getPasswordReturnsCharArrayAndZeroingItWithArraysFillDoesNotChangeTheField() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            field.setText("s3cret");
            char[] password = field.getPassword();
            assertArrayEquals(new char[] {'s', '3', 'c', 'r', 'e', 't'}, password);
            assertEquals("s3cret", new String(password));

            Arrays.fill(password, '\0');
            assertArrayEquals(new char[] {'s', '3', 'c', 'r', 'e', 't'}, field.getPassword());
            assertNotSame(field.getPassword(), field.getPassword());
        });
    }

    @Test
    void setTextWithNullEmptiesTheFieldAndGetPasswordReturnsEmptyArray() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            field.setText("abc");
            field.setText(null);
            assertEquals(0, field.getPassword().length);
        });
    }

    @Test
    void echoCharIsSetIsTrueByDefaultAndSetEchoCharZeroShowsPlainText() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            assertTrue(field.echoCharIsSet());
            assertEquals(PasswordRevealMode.HIDDEN, field.getPasswordRevealMode());

            field.setEchoChar('\0');
            assertFalse(field.echoCharIsSet());
            assertEquals('\0', field.getEchoChar());
            assertEquals(PasswordRevealMode.VISIBLE, field.getPasswordRevealMode());

            field.setEchoChar('*');
            assertTrue(field.echoCharIsSet());
            assertEquals('*', field.getEchoChar());
            assertEquals(PasswordRevealMode.HIDDEN, field.getPasswordRevealMode());
        });
    }

    @Test
    void anonymousDocumentListenerReceivesRemoveAndInsertOfSetTextInOrder() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            field.setText("old");
            List<String> events = new ArrayList<>();
            WDocumentListener listener = new WDocumentListener() {
                @Override
                public void insertUpdate(WDocumentEvent e) {
                    events.add("insert " + e.getOffset() + " " + e.getLength());
                    assertSame(field, e.getSource());
                }

                @Override
                public void removeUpdate(WDocumentEvent e) {
                    events.add("remove " + e.getOffset() + " " + e.getLength());
                }

                @Override
                public void changedUpdate(WDocumentEvent e) {
                    events.add("changed");
                }
            };
            field.addDocumentListener(listener);
            field.setText("new!");
            assertEquals(Arrays.asList("remove 0 3", "insert 0 4"), events);

            field.removeDocumentListener(listener);
            field.setText("");
            assertEquals(2, events.size());
            assertEquals(0, field.getDocumentListeners().length);
        });
    }

    @Test
    void actionListenerCanBeAddedAsLambdaOrMethodReferenceAndIsCalledByPostActionEvent() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            field.setText("pw");
            List<String> calls = new ArrayList<>();
            WActionListener byEvent = e -> calls.add("event:" + e.getActionCommand());
            field.addActionListener(e -> {
                assertSame(field, e.getSource());
                calls.add("lambda");
            });
            field.addActionListener(() -> calls.add("runnable"));
            field.addActionListener(byEvent);

            field.postActionEvent();
            assertEquals(Arrays.asList("lambda", "runnable", "event:null"), calls);

            field.setActionCommand("login");
            assertEquals("login", field.getActionCommand());
            calls.clear();
            field.removeActionListener(byEvent);
            field.postActionEvent();
            assertEquals(Arrays.asList("lambda", "runnable"), calls);
        });
    }

    @Test
    void getActionCommandDefaultsToNullAndDoesNotReturnThePassword() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            field.setText("secret");
            assertNull(field.getActionCommand());
        });
    }

    @Test
    void placeholderRevealModeAndMaxLengthCanBeUsed() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField("Hint");
            assertEquals("Hint", field.getPlaceholderText());
            field.setPasswordRevealMode(PasswordRevealMode.PEEK);
            assertEquals(PasswordRevealMode.PEEK, field.getPasswordRevealMode());
            field.setMaxLength(4);
            assertEquals(4, field.getMaxLength());
        });
    }
}
