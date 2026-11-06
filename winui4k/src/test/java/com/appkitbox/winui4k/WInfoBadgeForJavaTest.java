package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WInfoBadge's value (getter / setter, and IllegalArgumentException for values smaller than -1) and icon from Java.
 * Showing a number, a dot or an icon is checked in WInfoBadgeTest.
 */
class WInfoBadgeForJavaTest {
    @Test
    void valueCanBeReadAndWrittenWithTheGetterAndSetterAndDefaultsToMinusOne() {
        onUiThread(() -> {
            WInfoBadge badge = new WInfoBadge();
            assertEquals(-1, badge.getValue());
            badge.setValue(0);
            assertEquals(0, badge.getValue());
            badge.setValue(99);
            assertEquals(99, badge.getValue());
            badge.setValue(-1);
            assertEquals(-1, badge.getValue());
        });
    }

    @Test
    void aValueSmallerThanMinusOneThrowsIllegalArgumentExceptionAndTheValueDoesNotChange() {
        onUiThread(() -> {
            WInfoBadge badge = new WInfoBadge();
            badge.setValue(5);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> badge.setValue(-2));
            assertTrue(error.getMessage().contains("-1"), error.getMessage());
            assertThrows(IllegalArgumentException.class, () -> badge.setValue(Integer.MIN_VALUE));
            assertEquals(5, badge.getValue());
        });
    }

    @Test
    void iconCanBeSetWithTheGetterSetterAndSetSymbolIconAndNullRemovesIt() {
        onUiThread(() -> {
            WInfoBadge badge = new WInfoBadge();
            assertNull(badge.getIcon());
            badge.setIcon(Symbol.MAIL);
            assertSame(Symbol.MAIL, badge.getIcon());
            WFontIcon font = new WFontIcon("!");
            badge.setIcon(font);
            assertSame(font, badge.getIcon());
            badge.setSymbolIcon(Symbol.SAVE);
            assertSame(Symbol.SAVE, badge.getIcon());
            badge.setIcon(null);
            assertNull(badge.getIcon());
        });
    }
}
