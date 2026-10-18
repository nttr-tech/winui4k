package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WButton icons from Java, written the way developers used to Swing's JButton(String, Icon) / setIcon /
 * setHorizontalTextPosition / setIconTextGap would write them. The arrangement of the Content is checked in
 * WButtonIconTest.
 */
class WButtonIconForJavaTest {
    @Test
    void aButtonCanBeCreatedWithTextAndASymbolIcon() {
        onUiThread(() -> {
            WButton button = new WButton("Save", Symbol.SAVE);
            assertEquals("Save", button.getText());
            assertSame(Symbol.SAVE, button.getIcon());
        });
    }

    @Test
    void aButtonCanBeCreatedWithOnlyAnImageFileIcon() {
        onUiThread(() -> {
            WButton button = new WButton(new WImageIcon("C:/path/icon.png"));
            assertEquals("", button.getText());
            assertEquals(new WImageIcon("C:/path/icon.png"), button.getIcon());
        });
    }

    @Test
    void creatingWithTextAndANullIconGivesNoIcon() {
        onUiThread(() -> {
            WButton button = new WButton("OK", null);
            assertEquals("OK", button.getText());
            assertNull(button.getIcon());
        });
    }

    @Test
    void setIconReplacesTheIconAndNullRemovesIt() {
        onUiThread(() -> {
            WButton button = new WButton("Open");
            assertNull(button.getIcon());

            button.setIcon(new WFontIcon("\uE8E5"));
            assertEquals(new WFontIcon("\uE8E5"), button.getIcon());
            assertEquals("Open", button.getText());

            button.setIcon(null);
            assertNull(button.getIcon());
            assertEquals("Open", button.getText());
        });
    }

    @Test
    void setHorizontalTextPositionSetsTheTextPosition() {
        onUiThread(() -> {
            WButton button = new WButton("Save", Symbol.SAVE);
            assertEquals(TextPosition.TRAILING, button.getHorizontalTextPosition());
            button.setHorizontalTextPosition(TextPosition.LEADING);
            assertEquals(TextPosition.LEADING, button.getHorizontalTextPosition());
            button.setHorizontalTextPosition(TextPosition.BOTTOM);
            assertEquals(TextPosition.BOTTOM, button.getHorizontalTextPosition());
        });
    }

    @Test
    void setIconTextGapAcceptsAnIntAndDefaultsToFour() {
        onUiThread(() -> {
            WButton button = new WButton("Save", Symbol.SAVE);
            assertEquals(4.0, button.getIconTextGap());
            button.setIconTextGap(8);
            assertEquals(8.0, button.getIconTextGap());
        });
    }

    @Test
    void setIconTextGapWithANegativeValueThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WButton button = new WButton("Save", Symbol.SAVE);
            assertThrows(IllegalArgumentException.class, () -> button.setIconTextGap(-1));
            assertEquals(4.0, button.getIconTextGap());
        });
    }

    @Test
    void getActionCommandInAnActionListenerIsTheTextEvenForAButtonWithAnIcon() {
        onUiThread(() -> {
            WButton button = new WButton("Save", Symbol.SAVE);
            String[] command = {null};
            button.addActionListener(e -> command[0] = e.getActionCommand());
            button.doClick();
            assertEquals("Save", command[0]);
        });
    }
}
