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
 * Tests in which a developer who has used Swing's Document uses getDocument() in the form written in Java
 * ({@code field.getDocument().addDocumentListener(...)}, {@code doc.getText(0, doc.getLength())}, etc.).
 */
class WDocumentForJavaTest {
    @Test
    void aDocumentListenerAddedToGetDocumentReadsTheLengthAndTextFromE_getDocument() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            List<String> seen = new ArrayList<>();
            field.getDocument().addDocumentListener(new WDocumentListener() {
                @Override
                public void insertUpdate(WDocumentEvent e) {
                    WDocument doc = e.getDocument();
                    seen.add("insert " + doc.getText(0, doc.getLength()));
                }

                @Override
                public void removeUpdate(WDocumentEvent e) {
                    seen.add("remove " + e.getDocument().getLength());
                }

                @Override
                public void changedUpdate(WDocumentEvent e) {
                }
            });
            field.setText("abc");
            field.setText("");
            assertEquals(Arrays.asList("insert abc", "remove 0"), seen);
        });
    }

    @Test
    void insertStringAppendsAtTheEndAndRemoveDeletes_theJTextAreaLogForm() {
        onUiThread(() -> {
            WTextPane log = new WTextPane();
            WDocument doc = log.getDocument();
            doc.insertString(doc.getLength(), "Line 1\n", null);
            doc.insertString(doc.getLength(), "Line 2\n", null);
            assertEquals("Line 1\nLine 2\n", log.getText());
            doc.remove(0, doc.getText(0, doc.getLength()).indexOf('\n') + 1);
            assertEquals("Line 2\n", log.getText());
        });
    }

    @Test
    void thePasswordFieldGetDocumentHasTheSameForm_outOfRangeThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WPasswordField field = new WPasswordField();
            WDocument doc = field.getDocument();
            assertSame(doc, field.getDocument());
            doc.insertString(0, "pass", null);
            assertEquals("pass", new String(field.getPassword()));
            assertThrows(IllegalArgumentException.class, () -> doc.insertString(5, "x", null));
            assertThrows(IllegalArgumentException.class, () -> doc.remove(2, 3));
            assertEquals("pass", doc.getText(0, doc.getLength()));
        });
    }
}
