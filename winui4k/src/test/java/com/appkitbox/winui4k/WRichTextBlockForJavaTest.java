package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests that use the text, font and foreground of WRichTextBlock the way a developer used to Swing's JLabel and JTextArea would write it in Java. */
class WRichTextBlockForJavaTest {
    @Test
    void setTextAndGetTextReturnTheStringSetAndLineBreaksSeparateParagraphs() {
        onUiThread(() -> {
            WRichTextBlock block = new WRichTextBlock();
            assertEquals("", block.getText());
            block.setText("Line 1\nLine 2\n");
            assertEquals("Line 1\nLine 2\n", block.getText());
        });
    }

    @Test
    void appendContinuesAtTheEndAndFormattedTextBuiltWithAddParagraphCanBeReadWithGetText() {
        onUiThread(() -> {
            WRichTextBlock block = new WRichTextBlock();
            block.addParagraph(paragraph -> {
                paragraph.run("Normal ");
                paragraph.bold("Bold");
            });
            block.append(" continued");
            block.append("\nNext paragraph");
            assertEquals("Normal Bold continued\nNext paragraph", block.getText());
        });
    }

    @Test
    void valuesPassedToSetFontAndSetForegroundCanBeReadBackWithGetters() {
        onUiThread(() -> {
            WRichTextBlock block = new WRichTextBlock();
            block.setFont(new WFont("Consolas", WFont.ITALIC, 18));
            block.setForeground(new WColor(0, 100, 200));
            assertEquals("Consolas", block.getFont().getName());
            assertTrue(block.getFont().isItalic());
            assertEquals(new WColor(0, 100, 200), block.getForeground());
            block.setForeground(null);
            assertNotNull(block.getForeground(), "null restores the default color, and reading returns the default color");
        });
    }
}
