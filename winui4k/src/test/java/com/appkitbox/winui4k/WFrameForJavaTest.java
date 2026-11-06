package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WFrame from Java in the same way developers who have used JFrame would write it: {@code frame.setDefaultButton(button)},
 * the same form as JRootPane.setDefaultButton, receiving windowClosing with an anonymous {@code WWindowAdapter},
 * {@code frame.setDefaultCloseOperation(WFrame.DO_NOTHING_ON_CLOSE)}, {@code WFrame.getFrames()}, {@code dispose()},
 * {@code frame.add(panel, WBorderLayout.CENTER)}. Own windows are disposed at the end, and the shared window is not closed.
 */
class WFrameForJavaTest {
    private static final String TITLE = "winui4k frame java test";
    private static final long TIMEOUT_MILLIS = 5_000;

    private final WFrame frame = UiTestHarness.sharedFrame();
    private final List<String> events = Collections.synchronizedList(new ArrayList<>());
    private WFrame own;

    @AfterEach
    void clearDefaultButton() {
        onUiThread(() -> frame.setDefaultButton(null));
        if (own != null) {
            onUiThread(own::dispose);
        }
    }

    private void awaitEvents(List<String> expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!snapshot().equals(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(expected, snapshot());
    }

    private List<String> snapshot() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    @Test
    void closeOperationConstantsCanBeWrittenAsStaticFieldsOfWFrameLikeWindowConstants() {
        assertEquals(0, WFrame.DO_NOTHING_ON_CLOSE);
        assertEquals(1, WFrame.HIDE_ON_CLOSE);
        assertEquals(2, WFrame.DISPOSE_ON_CLOSE);
        assertEquals(3, WFrame.EXIT_ON_CLOSE);
        assertEquals(3, WWindowConstants.EXIT_ON_CLOSE);
        assertEquals(6, WFrame.MAXIMIZED_BOTH);
        assertEquals(201, WWindowEvent.WINDOW_CLOSING);
        WWindowConstants constants = onUiThreadGet(() -> own = new WFrame(TITLE));
        assertNotNull(constants);
    }

    @Test
    void anAnonymousWWindowAdapterReceivesWindowClosingAndDisposesItselfWithDoNothingOnClose() throws InterruptedException {
        onUiThread(() -> {
            own = new WFrame(TITLE);
            own.setDefaultCloseOperation(WFrame.DO_NOTHING_ON_CLOSE);
            own.addWindowListener(new WWindowAdapter() {
                @Override
                public void windowOpened(WWindowEvent e) {
                    events.add("opened");
                }

                @Override
                public void windowClosing(WWindowEvent e) {
                    assertEquals(WWindowEvent.WINDOW_CLOSING, e.getID());
                    assertSame(own, e.getWindow());
                    events.add("closing");
                    e.getWindow().dispose();
                }

                @Override
                public void windowClosed(WWindowEvent e) {
                    events.add("closed");
                }
            });
            own.setVisible(true);
        });
        awaitEvents(Arrays.asList("opened"));
        TopLevelWindows.closeByTitle(TITLE); // The same way of closing as the title bar ×
        awaitEvents(Arrays.asList("opened", "closing", "closed"));
        onUiThread(() -> {
            assertFalse(own.isDisplayable());
            assertFalse(Arrays.asList(WFrame.getFrames()).contains(own));
        });
    }

    @Test
    void setVisibleFalseOnlyHidesAndKeepsItInGetFramesAndDisposeClosesIt() {
        onUiThread(() -> {
            own = new WFrame(TITLE);
            assertTrue(Arrays.asList(WFrame.getFrames()).contains(own));
            own.setVisible(true);
            assertTrue(own.isVisible());
            own.setVisible(false);
            assertFalse(own.isVisible());
            assertTrue(own.isDisplayable());
            assertTrue(Arrays.asList(WFrame.getFrames()).contains(own));
            own.dispose();
            assertFalse(own.isVisible());
            assertFalse(own.isDisplayable());
            assertFalse(Arrays.asList(WFrame.getFrames()).contains(own));
            assertThrows(IllegalStateException.class, () -> own.setVisible(true));
            assertThrows(IllegalArgumentException.class, () -> own.setDefaultCloseOperation(7));
        });
    }

    @Test
    void aLambdaWWindowStateListenerAndGetWindowListeners() {
        onUiThread(() -> {
            own = new WFrame(TITLE);
            WWindowStateListener listener = e -> events.add(e.getOldState() + "->" + e.getNewState());
            own.addWindowStateListener(listener);
            assertArrayEquals(new WWindowStateListener[] {listener}, own.getWindowStateListeners());
            own.removeWindowStateListener(listener);
            assertEquals(0, own.getWindowStateListeners().length);
            assertEquals(0, own.getWindowListeners().length);
            assertEquals(0, own.getWindowFocusListeners().length);
        });
    }

    @Test
    void frameAddWithAConstraintGoesIntoTheBorderLayoutPanelSetWithSetContentPane() {
        onUiThread(() -> {
            own = new WFrame(TITLE);
            WLayoutPanel root = new WLayoutPanel(new WBorderLayout());
            own.setContentPane(root);
            WButton center = new WButton("Center");
            own.add(center, WBorderLayout.CENTER);
            assertSame(root, center.getParent());
            assertSame(center, ((WBorderLayout) root.getLayout()).getLayoutComponent(WBorderLayout.CENTER));
            assertEquals(0, own.getContentPane().getComponentCount());
        });
    }

    @Test
    void onlyTheButtonSetWithSetDefaultButtonBecomesIsDefaultButton() {
        onUiThread(() -> {
            WButton ok = new WButton("OK");
            WButton cancel = new WButton("Cancel");
            assertNull(frame.getDefaultButton());
            assertFalse(ok.isDefaultButton());

            frame.setDefaultButton(ok);
            assertSame(ok, frame.getDefaultButton());
            assertTrue(ok.isDefaultButton());
            assertFalse(cancel.isDefaultButton());
        });
    }

    @Test
    void switchingToAnotherButtonMakesThePreviousButtonNoLongerIsDefaultButton() {
        onUiThread(() -> {
            WButton ok = new WButton("OK");
            WButton cancel = new WButton("Cancel");
            frame.setDefaultButton(ok);
            frame.setDefaultButton(cancel);
            assertFalse(ok.isDefaultButton());
            assertTrue(cancel.isDefaultButton());
            assertSame(cancel, frame.getDefaultButton());
        });
    }

    @Test
    void passingNullClearsTheDefaultButton() {
        onUiThread(() -> {
            WButton ok = new WButton("OK");
            frame.setDefaultButton(ok);
            frame.setDefaultButton(null);
            assertNull(frame.getDefaultButton());
            assertFalse(ok.isDefaultButton());
        });
    }
}
