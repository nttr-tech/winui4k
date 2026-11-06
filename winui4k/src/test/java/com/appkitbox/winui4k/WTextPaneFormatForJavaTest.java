package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests in which a developer who has formatted text with Swing's JTextPane changes WTextPane formatting in the form written in Java
 * (building a SimpleAttributeSet + StyleConstants and passing it to getStyledDocument().setCharacterAttributes).
 */
class WTextPaneFormatForJavaTest {
    @Test
    void setCharacterAttributesOfGetStyledDocumentMakesARangeBoldAndRed_getCharacterAttributesReadsIt() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("Hello World");
            WSimpleAttributeSet keyword = new WSimpleAttributeSet();
            WStyleConstants.setBold(keyword, true);
            WStyleConstants.setForeground(keyword, WColor.RED);
            WStyledDocument doc = pane.getStyledDocument();
            doc.setCharacterAttributes(0, 5, keyword, false);

            assertTrue(WStyleConstants.isBold(doc.getCharacterAttributes(0)));
            assertEquals(WColor.RED, WStyleConstants.getForeground(doc.getCharacterAttributes(4)));
            assertFalse(WStyleConstants.isBold(doc.getCharacterAttributes(6)));

            pane.setCaretPosition(1);
            assertTrue(WStyleConstants.isBold(pane.getCharacterAttributes()));
        });
    }

    @Test
    void passingAttributesToInsertStringAddsAColoredLineToALog() {
        onUiThread(() -> {
            WTextPane log = new WTextPane();
            WStyledDocument doc = log.getStyledDocument();
            WSimpleAttributeSet error = new WSimpleAttributeSet();
            WStyleConstants.setForeground(error, WColor.RED);
            WStyleConstants.setFontSize(error, 18);
            doc.insertString(doc.getLength(), "ok\n", null);
            doc.insertString(doc.getLength(), "error\n", error);

            assertEquals("ok\nerror\n", log.getText());
            assertEquals(18, WStyleConstants.getFontSize(doc.getCharacterAttributes(3)));
            assertEquals(WColor.RED, WStyleConstants.getForeground(doc.getCharacterAttributes(3)));
        });
    }

    @Test
    void setCharacterAttributesUnderlinesTheSelection() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("abcd");
            pane.select(1, 3);
            WSimpleAttributeSet underline = new WSimpleAttributeSet();
            WStyleConstants.setUnderline(underline, true);
            pane.setCharacterAttributes(underline, false);
            assertTrue(WStyleConstants.isUnderline(pane.getStyledDocument().getCharacterAttributes(2)));
            assertFalse(WStyleConstants.isUnderline(pane.getStyledDocument().getCharacterAttributes(0)));
        });
    }
}
