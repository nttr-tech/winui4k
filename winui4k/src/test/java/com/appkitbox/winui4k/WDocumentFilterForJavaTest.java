package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests in which a developer who has used Swing's DocumentFilter uses WDocumentFilter in the form written in Java
 * (overriding insertString / replace in an anonymous class and calling super to let a change through).
 */
class WDocumentFilterForJavaTest {
    /** A filter that lets only digits through, in the form commonly written in Swing. */
    private static WDocumentFilter digitsOnly() {
        return new WDocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, WAttributeSet attr) {
                if (string != null && string.chars().allMatch(Character::isDigit)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, WAttributeSet attrs) {
                if (text == null || text.chars().allMatch(Character::isDigit)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        };
    }

    @Test
    void aDigitsOnlyFilterSetWithSetDocumentFilterOnGetDocumentRejectsLettersFromSetTextAndInsertString() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            WDocument doc = field.getDocument();
            WDocumentFilter filter = digitsOnly();
            doc.setDocumentFilter(filter);
            assertSame(filter, doc.getDocumentFilter());

            field.setText("123");
            field.setText("abc");
            doc.insertString(doc.getLength(), "x", null);
            doc.insertString(doc.getLength(), "4", null);
            assertEquals("1234", field.getText());
        });
    }

    @Test
    void aFilterThatCutsTheExcessOverTheMaximumLengthWithFilterBypass() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            field.getDocument().setDocumentFilter(new WDocumentFilter() {
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, WAttributeSet attrs) {
                    int room = 4 - (fb.getDocument().getLength() - length);
                    String cut = text == null ? null : text.substring(0, Math.min(text.length(), Math.max(room, 0)));
                    fb.replace(offset, length, cut, attrs);
                }
            });
            field.setText("password");
            assertEquals("pass", new String(field.getPassword()));

            field.getDocument().setDocumentFilter(null);
            assertNull(field.getDocument().getDocumentFilter());
            field.setText("password");
            assertEquals("password", new String(field.getPassword()));
        });
    }
}
