package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shape as JComponent.getInsets in the Java that Swing developers would write
 * (reading the inner width of a control with a border via top / left / bottom / right of getInsets()).
 */
class WControlInsetsForJavaTest {
    @Test
    void getInsetsOfAButtonWithALineBorderAndAnEmptyBorderIsTheSumOfBothWidths() {
        WButton button = onUiThreadGet(() -> new WButton("Press"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setBorder(WBorderFactory.createCompoundBorder(
                        WBorderFactory.createLineBorder(WColor.BLACK, 2),
                        WBorderFactory.createEmptyBorder(1, 2, 3, 4)));
                WInsets insets = button.getInsets();
                assertEquals(3.0, insets.getTop());
                assertEquals(4.0, insets.getLeft());
                assertEquals(5.0, insets.getBottom());
                assertEquals(6.0, insets.getRight());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void setPaddingAndSetHorizontalContentAlignmentReadBackTheSetValues() {
        WButton button = onUiThreadGet(() -> new WButton("Press"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setBorder(WBorderFactory.createLineBorder(WColor.BLACK, 1));
                button.setPadding(4, 3, 2, 1);
                assertEquals(new WInsets(4.0, 5.0, 2.0, 3.0), button.getInsets());
                button.setHorizontalContentAlignment(HorizontalAlignment.RIGHT);
                button.setVerticalContentAlignment(VerticalAlignment.TOP);
                assertEquals(HorizontalAlignment.RIGHT, button.getHorizontalContentAlignment());
                assertEquals(VerticalAlignment.TOP, button.getVerticalContentAlignment());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
