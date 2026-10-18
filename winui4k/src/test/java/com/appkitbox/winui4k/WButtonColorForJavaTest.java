package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests that use setBackground / setForeground with WColor, the same as Swing's JButton.
 * The per-visual-state painting is checked by the Kotlin WButtonColorTest; here we only look at the colors read back
 * through the getters.
 * Fonts (setFont) are not colors, so they live in WButtonForJavaTest.
 */
class WButtonColorForJavaTest {
    // ---- background ----

    @Test
    void theColorSetWithSetBackgroundCanBeReadBackWithGetBackground() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Background"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setBackground(new WColor(10, 120, 200));
                assertEquals(new WColor(10, 120, 200, 255), button.getBackground());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void getBackgroundReturnsTheThemeDefaultBeforeBeingSetAndSettingNullRestoresIt() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Default background"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                WColor themeDefault = button.getBackground();
                assertNotNull(themeDefault);
                button.setBackground(WColor.RED);
                assertNotEquals(themeDefault, button.getBackground());
                button.setBackground(null);
                assertEquals(themeDefault, button.getBackground());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void backgroundSetBeforeAttachingToAWindowCanAlsoBeReadBack() {
        WButton button = UiTestHarness.onUiThreadGet(() -> {
            WButton b = new WButton("Set beforehand");
            b.setBackground(new WColor(10, 120, 60));
            return b;
        });
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> assertEquals(new WColor(10, 120, 60, 255), button.getBackground()));
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void callingSetBackgroundRepeatedlyReadsBackTheLastColorPassed() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Overwritten"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setBackground(WColor.RED);
                button.setBackground(new WColor(1, 2, 3));
                assertEquals(new WColor(1, 2, 3, 255), button.getBackground());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    // ---- foreground ----

    @Test
    void setForegroundWithAWColorConstantReadsBackTheSameColorWithGetForeground() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Text color"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setForeground(WColor.RED);
                assertEquals(WColor.RED, button.getForeground());
                assertEquals(237, button.getForeground().getRed());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void settingForegroundToNullRestoresTheThemeDefaultTextColor() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Default text color"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                WColor themeDefault = button.getForeground();
                assertNotNull(themeDefault);
                button.setForeground(WColor.RED);
                assertNotEquals(themeDefault, button.getForeground());
                button.setForeground(null);
                assertEquals(themeDefault, button.getForeground());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void backgroundAndForegroundDoNotAffectEachOtherAndAreReadBackSeparately() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Both"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                WColor defaultForeground = button.getForeground();
                button.setBackground(new WColor(10, 120, 200));
                assertEquals(defaultForeground, button.getForeground());
                button.setForeground(WColor.RED);
                assertEquals(new WColor(10, 120, 200, 255), button.getBackground());
                assertEquals(WColor.RED, button.getForeground());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
