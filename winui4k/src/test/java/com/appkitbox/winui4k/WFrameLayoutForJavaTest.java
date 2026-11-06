package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that decide how WFrame's content is arranged in the form a developer who has used JFrame writes in Java:
 * {@code frame.add(c, WBorderLayout.NORTH)} after {@code frame.setLayout(new WBorderLayout())}, {@code frame.getLayout()}, and
 * the IllegalArgumentException when adding with a constraint without calling setLayout. Each test disposes its own window at the end.
 */
class WFrameLayoutForJavaTest {
    private WFrame frame;

    @BeforeEach
    void openTheSharedWindow() {
        UiTestHarness.sharedFrame(); // So that closing our own window does not end the app
    }

    @AfterEach
    void closeOwnWindow() {
        if (frame != null) {
            onUiThread(frame::dispose);
        }
    }

    @Test
    void setLayoutToBorderLayoutThenAddToNorthAndCenter() {
        onUiThread(() -> {
            frame = new WFrame("winui4k frame layout java test");
            assertNull(frame.getLayout());
            frame.setLayout(new WBorderLayout());
            WButton north = new WButton("North");
            WButton center = new WButton("Center");
            frame.add(north, WBorderLayout.NORTH);
            frame.add(center, WBorderLayout.CENTER);
            assertTrue(frame.getLayout() instanceof WBorderLayout);
            WBorderLayout layout = (WBorderLayout) frame.getLayout();
            assertSame(north, layout.getLayoutComponent(WBorderLayout.NORTH));
            assertSame(center, layout.getLayoutComponent(WBorderLayout.CENTER));
        });
    }

    @Test
    void addingWithAConstraintWithoutCallingSetLayoutThrowsIllegalArgumentException() {
        onUiThread(() -> {
            frame = new WFrame("winui4k frame layout java test");
            WButton north = new WButton("North");
            assertThrows(IllegalArgumentException.class, () -> frame.add(north, WBorderLayout.NORTH));
            assertNull(north.getParent());
        });
    }
}
