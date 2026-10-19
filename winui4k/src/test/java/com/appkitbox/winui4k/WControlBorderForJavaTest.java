package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests using the same form as JComponent.setBorder(BorderFactory.createXxxBorder(...)), in Java as Swing developers
 * write it (the static methods of WBorderFactory, setBorder / getBorder, the border getters and getBorderInsets).
 */
class WControlBorderForJavaTest {
    @Test
    void setBorderWithCreateLineBorderReturnsSameBorderFromGetBorder() {
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        WLineBorder line = WBorderFactory.createLineBorder(WColor.RED, 2);
        onUiThread(() -> button.setBorder(line));
        WControlBorder border = onUiThreadGet(button::getBorder);
        assertSame(line, border);
        assertTrue(border instanceof WLineBorder);
        assertEquals(WColor.RED, ((WLineBorder) border).getColor());
        assertEquals(2, ((WLineBorder) border).getThickness());
    }

    @Test
    void createLineBorderDefaultsToThicknessOne() {
        assertEquals(1, WBorderFactory.createLineBorder(WColor.BLUE).getThickness());
    }

    @Test
    void createEmptyBorderArgumentsAreTopLeftBottomRight() {
        WEmptyBorder empty = WBorderFactory.createEmptyBorder(1, 2, 3, 4);
        assertEquals(1, empty.getTop());
        assertEquals(2, empty.getLeft());
        assertEquals(3, empty.getBottom());
        assertEquals(4, empty.getRight());
        assertEquals(new WInsets(1.0, 2.0, 3.0, 4.0), empty.getBorderInsets());
        assertEquals(new WEmptyBorder(0, 0, 0, 0), WBorderFactory.createEmptyBorder());
    }

    @Test
    void createCompoundBorderCombinesOutsideAndInsideAndCanBeSetOnTextField() {
        WTextField field = onUiThreadGet(WTextField::new);
        WControlBorder outside = WBorderFactory.createLineBorder(WColor.GRAY, 1);
        WControlBorder inside = WBorderFactory.createEmptyBorder(4, 8, 4, 8);
        WCompoundBorder compound = WBorderFactory.createCompoundBorder(outside, inside);
        onUiThread(() -> field.setBorder(compound));
        WCompoundBorder got = (WCompoundBorder) onUiThreadGet(field::getBorder);
        assertSame(outside, got.getOutsideBorder());
        assertSame(inside, got.getInsideBorder());
        assertEquals(new WInsets(5.0, 9.0, 5.0, 9.0), got.getBorderInsets());
    }

    @Test
    void setBorderWithNullMakesGetBorderNull() {
        WCheckBox box = onUiThreadGet(() -> new WCheckBox("Confirm"));
        onUiThread(() -> box.setBorder(WBorderFactory.createEmptyBorder(2, 2, 2, 2)));
        onUiThread(() -> box.setBorder(null));
        assertNull(onUiThreadGet(box::getBorder));
    }

    @Test
    void settingCombinationWithTwoLinesThrowsIllegalArgumentException() {
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        WCompoundBorder twoLines = WBorderFactory.createCompoundBorder(
                WBorderFactory.createLineBorder(WColor.RED),
                WBorderFactory.createLineBorder(WColor.BLUE));
        assertThrows(IllegalArgumentException.class, () -> onUiThread(() -> button.setBorder(twoLines)));
        assertNull(onUiThreadGet(button::getBorder));
    }

    @Test
    void negativeThicknessThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> WBorderFactory.createLineBorder(WColor.RED, -1));
        assertThrows(IllegalArgumentException.class, () -> WBorderFactory.createEmptyBorder(0, -1, 0, 0));
    }
}
