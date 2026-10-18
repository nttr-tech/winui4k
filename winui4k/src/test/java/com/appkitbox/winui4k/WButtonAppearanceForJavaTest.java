package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use Swing JButton's appearance toggles (setContentAreaFilled / setBorderPainted) and WinUI's accent style
 * (setAccent) from Java.
 * The style and border thickness actually drawn are checked by the Kotlin WButtonAppearanceTest; here we only look
 * at the values read back through the getters.
 */
class WButtonAppearanceForJavaTest {
    // ---- accent ----

    @Test
    void isAccentIsFalseByDefaultAndCanBeToggledWithSetAccentAndReadBack() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            assertFalse(button.isAccent());
            button.setAccent(true);
            assertTrue(button.isAccent());
            button.setAccent(false);
            assertFalse(button.isAccent());
        });
    }

    @Test
    void passingTheSameValueToSetAccentRepeatedlyDoesNotChangeTheReadBackValue() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            button.setAccent(true);
            button.setAccent(true);
            assertTrue(button.isAccent());
            button.setAccent(false);
            button.setAccent(false);
            assertFalse(button.isAccent());
        });
    }

    // ---- contentAreaFilled ----

    @Test
    void isContentAreaFilledIsTrueByDefaultCanBeSetToFalseAndBackToTrue() {
        onUiThread(() -> {
            WButton button = new WButton("Close");
            assertTrue(button.isContentAreaFilled());
            button.setContentAreaFilled(false);
            assertFalse(button.isContentAreaFilled());
            button.setContentAreaFilled(true);
            assertTrue(button.isContentAreaFilled());
        });
    }

    @Test
    void passingTheSameValueToSetContentAreaFilledRepeatedlyDoesNotChangeTheReadBackValue() {
        onUiThread(() -> {
            WButton button = new WButton("Close");
            button.setContentAreaFilled(false);
            button.setContentAreaFilled(false);
            assertFalse(button.isContentAreaFilled());
            button.setContentAreaFilled(true);
            button.setContentAreaFilled(true);
            assertTrue(button.isContentAreaFilled());
        });
    }

    // ---- borderPainted ----

    @Test
    void isBorderPaintedIsTrueByDefaultCanBeSetToFalseAndBackToTrue() {
        onUiThread(() -> {
            WButton button = new WButton("Border");
            assertTrue(button.isBorderPainted());
            button.setBorderPainted(false);
            assertFalse(button.isBorderPainted());
            button.setBorderPainted(true);
            assertTrue(button.isBorderPainted());
        });
    }

    @Test
    void passingTheSameValueToSetBorderPaintedRepeatedlyDoesNotChangeTheReadBackValue() {
        onUiThread(() -> {
            WButton button = new WButton("Border");
            button.setBorderPainted(false);
            button.setBorderPainted(false);
            assertFalse(button.isBorderPainted());
            button.setBorderPainted(true);
            button.setBorderPainted(true);
            assertTrue(button.isBorderPainted());
        });
    }

    // ---- Combinations ----

    @Test
    void accentContentAreaFilledAndBorderPaintedAreReadBackAsIndependentValues() {
        onUiThread(() -> {
            WButton button = new WButton("Close");
            button.setContentAreaFilled(false);
            button.setAccent(true);
            // Even though accent is drawn in preference to no fill, each setting keeps its own value
            assertTrue(button.isAccent());
            assertFalse(button.isContentAreaFilled());
            assertTrue(button.isBorderPainted());

            button.setBorderPainted(false);
            assertTrue(button.isAccent());
            assertFalse(button.isContentAreaFilled());
            assertFalse(button.isBorderPainted());

            button.setAccent(false);
            assertFalse(button.isAccent());
            assertFalse(button.isContentAreaFilled());
            assertFalse(button.isBorderPainted());

            button.setContentAreaFilled(true);
            assertFalse(button.isAccent());
            assertTrue(button.isContentAreaFilled());
            assertFalse(button.isBorderPainted());
        });
    }

    @Test
    void appearanceSettingsCanBeToggledAndReadBackAfterAttachingToAWindow() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("After attaching"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setAccent(true);
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
                assertTrue(button.isAccent());
                assertFalse(button.isContentAreaFilled());
                assertFalse(button.isBorderPainted());
                button.setAccent(false);
                button.setContentAreaFilled(true);
                button.setBorderPainted(true);
                assertFalse(button.isAccent());
                assertTrue(button.isContentAreaFilled());
                assertTrue(button.isBorderPainted());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
