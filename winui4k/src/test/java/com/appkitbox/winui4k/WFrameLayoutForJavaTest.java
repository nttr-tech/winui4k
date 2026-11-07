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
 * Tests that decide how the content of a WFrame is laid out, written the way developers who have used JFrame write Java: {@code frame.add(c, WBorderLayout.NORTH)}
 * and {@code frame.getContentPane().add(c, BorderLayout.CENTER)} without calling setLayout (the default content is BorderLayout, as in JFrame),
 * {@code getContentPane()} after {@code frame.setLayout(new WFlowLayout())}, {@code frame.getLayout()},
 * and IllegalArgumentException when adding with constraints to content that cannot use them. Each test disposes its own window at the end.
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
    void addAndGetContentPaneAddPutChildrenInNorthCenterAndSouthWithoutCallingSetLayout() {
        onUiThread(() -> {
            frame = new WFrame("winui4k frame layout java test");
            assertTrue(frame.getLayout() instanceof WBorderLayout);
            WButton north = new WButton("North");
            WButton center = new WButton("Center");
            WButton south = new WButton("South");
            frame.add(north, WBorderLayout.NORTH);
            frame.getContentPane().add(center, WBorderLayout.CENTER);
            frame.getContentPane().add(south, "South"); // Swing's string constraint (BorderLayout.SOUTH)
            WBorderLayout layout = (WBorderLayout) frame.getLayout();
            assertSame(north, layout.getLayoutComponent(WBorderLayout.NORTH));
            assertSame(center, layout.getLayoutComponent(WBorderLayout.CENTER));
            assertSame(south, layout.getLayoutComponent(WBorderLayout.SOUTH));
            assertSame(frame.getContentPane(), center.getParent());
        });
    }

    @Test
    void afterSetLayoutGetContentPaneIsTheSameContentAndAddedChildrenAreLaidOutByThatManager() {
        onUiThread(() -> {
            frame = new WFrame("winui4k frame layout java test");
            WContainer pane = frame.getContentPane();
            WFlowLayout flow = new WFlowLayout();
            frame.setLayout(flow);
            assertSame(pane, frame.getContentPane());
            assertSame(flow, frame.getLayout());
            WButton button = new WButton("Button");
            frame.getContentPane().add(button);
            assertSame(pane, button.getParent());
        });
    }

    @Test
    void addingWithConstraintsAfterMakingAVerticalWPanelTheContentThrowsIllegalArgumentException() {
        onUiThread(() -> {
            frame = new WFrame("winui4k frame layout java test");
            WPanel stack = new WPanel();
            frame.setContentPane(stack);
            assertSame(stack, frame.getContentPane());
            assertNull(frame.getLayout());
            WButton north = new WButton("North");
            assertThrows(IllegalArgumentException.class, () -> frame.add(north, WBorderLayout.NORTH));
            assertThrows(IllegalArgumentException.class, () -> frame.getContentPane().add(north, WBorderLayout.NORTH));
            assertNull(north.getParent());
        });
    }
}
