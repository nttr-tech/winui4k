package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Test;

/** Tests using WLabel's font, text color and setLabelFor in the form developers who have used Swing's JLabel write in Java. */
class WLabelForJavaTest {
    @Test
    void fontSetWithSetFontIsReadBackWithGetFont() {
        onUiThread(() -> {
            WLabel label = new WLabel("Heading");
            label.setFont(new WFont("Consolas", WFont.BOLD, 20));
            WFont font = label.getFont();
            assertEquals("Consolas", font.getName());
            assertEquals(WFont.BOLD, font.getStyle());
            assertEquals(20, font.getSize());
            assertEquals(700, label.getFontWeight());
        });
    }

    @Test
    void fontMadeBoldWithDeriveFontCanBeSet() {
        onUiThread(() -> {
            WLabel label = new WLabel("Emphasis");
            label.setFont(label.getFont().deriveFont(WFont.BOLD, 18f));
            assertTrue(label.getFont().isBold());
            assertEquals(18f, label.getFont().getSize2D());
        });
    }

    @Test
    void setLabelForAndSetDisplayedMnemonicGiveInputFieldNameAndAccessKey() {
        onUiThread(() -> {
            WLabel label = new WLabel("Name:");
            WTextField field = new WTextField();
            label.setLabelFor(field);
            label.setDisplayedMnemonic('n');
            assertSame(field, label.getLabelFor());
            assertEquals(KeyEvent.VK_N, label.getDisplayedMnemonic());
        });
    }

    @Test
    void setDisplayedMnemonicAcceptsAwtVkConstantsAndInvalidValuesThrow() {
        onUiThread(() -> {
            WLabel label = new WLabel("Address:");
            label.setDisplayedMnemonic(KeyEvent.VK_A);
            assertEquals(KeyEvent.VK_A, label.getDisplayedMnemonic());
            assertThrows(IllegalArgumentException.class, () -> label.setDisplayedMnemonic('#'));
            assertEquals(KeyEvent.VK_A, label.getDisplayedMnemonic());
        });
    }

    @Test
    void colorSetWithSetForegroundIsReadBackWithGetForeground() {
        onUiThread(() -> {
            WLabel label = new WLabel("Red");
            WColor before = label.getForeground();
            label.setForeground(WColor.RED);
            assertEquals(WColor.RED, label.getForeground());
            label.setForeground(null);
            assertEquals(before, label.getForeground());
        });
    }
}
