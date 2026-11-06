package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that set per-state icons from Java in the same way as Swing's setSelectedIcon / setDisabledIcon (AbstractButton and JLabel).
 */
class WButtonStateIconForJavaTest {
    @Test
    void iconsSetWithSetSelectedIconAndSetDisabledIconReadBackWithTheGetters() {
        onUiThread(() -> {
            WToggleButton toggle = new WToggleButton("Play", Symbol.PLAY);
            toggle.setSelectedIcon(Symbol.PAUSE);
            toggle.setDisabledIcon(Symbol.CANCEL);
            toggle.setSelected(true);
            assertEquals(Symbol.PAUSE, toggle.getSelectedIcon());
            assertEquals(Symbol.CANCEL, toggle.getDisabledIcon());
            assertEquals(Symbol.PLAY, toggle.getIcon());
        });
    }

    @Test
    void setEnabledAndSetDisabledIconCanBeUsedLikeJLabel() {
        onUiThread(() -> {
            WLabel label = new WLabel("Name", Symbol.CONTACT);
            label.setDisabledIcon(Symbol.DELETE);
            label.setEnabled(false);
            assertFalse(label.isEnabled());
            assertEquals(Symbol.DELETE, label.getDisabledIcon());
            assertEquals(Symbol.CONTACT, label.getIcon());
            label.setDisabledIcon(null);
            assertNull(label.getDisabledIcon());
        });
    }

    @Test
    void passingAWIconImplementedOnYourOwnThrowsIllegalArgumentException() {
        WIcon custom = new WIcon() {
        };
        onUiThread(() -> {
            assertThrows(IllegalArgumentException.class, () -> new WToggleButton().setSelectedIcon(custom));
            assertThrows(IllegalArgumentException.class, () -> new WButton().setDisabledIcon(custom));
            assertThrows(IllegalArgumentException.class, () -> new WLabel().setDisabledIcon(custom));
        });
    }
}
