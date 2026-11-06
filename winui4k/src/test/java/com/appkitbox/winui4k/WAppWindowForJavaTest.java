package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WAppWindow (getSize / resize / move / getTitleBar), WOverlappedPresenter (create / setPresenter / maximize / getState)
 * and WDisplayArea.nearest in the same way Java developers would write it.
 * Own windows are disposed at the end.
 */
class WAppWindowForJavaTest {
    private static final String TITLE = "winui4k app window java test";

    private WFrame frame;

    @BeforeEach
    void openWindows() {
        UiTestHarness.sharedFrame(); // So that closing its own window does not exit the app
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            frame.setVisible(true);
        });
    }

    @AfterEach
    void closeOwnWindow() {
        onUiThread(frame::dispose);
    }

    @Test
    void resizeAndMoveCanBeReadBackWithGetters() {
        onUiThread(() -> {
            WAppWindow window = frame.getAppWindow();
            WRectangle area = WDisplayArea.primary().getWorkArea();
            window.resize(720, 540);
            window.move(area.getX() + 40, area.getY() + 30);
            assertEquals(new WDimension(720, 540), window.getSize());
            assertEquals(new WPoint(area.getX() + 40, area.getY() + 30), window.getPosition());
            window.setSize(new WDimension(800, 600));
            assertEquals(800, window.getSize().getWidth());
            assertEquals(600, window.getSize().getHeight());
        });
    }

    @Test
    void titleBarColorsCanBeWrittenWithWColorReadBackAndResetToUnsetWithNull() {
        onUiThread(() -> {
            WAppWindowTitleBar titleBar = frame.getAppWindow().getTitleBar();
            assertNull(titleBar.getBackgroundColor());
            titleBar.setBackgroundColor(new WColor(10, 20, 30));
            WColor read = titleBar.getBackgroundColor();
            assertEquals(10, read.getRed());
            assertEquals(20, read.getGreen());
            assertEquals(30, read.getBlue());
            assertEquals(255, read.getAlpha());
            titleBar.setBackgroundColor(null);
            assertNull(titleBar.getBackgroundColor());
            titleBar.setExtendsContentIntoTitleBar(true); // TALL can be chosen only while the content extends into the title bar
            titleBar.setPreferredHeightOption(TitleBarHeightOption.TALL);
            assertEquals(TitleBarHeightOption.TALL, titleBar.getPreferredHeightOption());
        });
    }

    @Test
    void anOverlappedPresenterCanBeCreatedPassedAndMaximizedAndRestored() throws Exception {
        WOverlappedPresenter presenter = onUiThreadGet(() -> {
            WOverlappedPresenter created = WOverlappedPresenter.create();
            frame.getAppWindow().setPresenter(created);
            return created;
        });
        assertEquals(OverlappedPresenterState.RESTORED, onUiThreadGet(presenter::getState));
        onUiThread(presenter::maximize);
        waitForState(presenter, OverlappedPresenterState.MAXIMIZED);
        onUiThread(presenter::restore);
        waitForState(presenter, OverlappedPresenterState.RESTORED);
        onUiThread(() -> {
            presenter.setResizable(false);
            assertFalse(presenter.isResizable());
            presenter.setResizable(true);
            assertTrue(presenter.isResizable());
        });
    }

    @Test
    void nearestReturnsTheDisplayContainingTheWindowPosition() {
        onUiThread(() -> {
            WAppWindow window = frame.getAppWindow();
            WRectangle area = WDisplayArea.primary().getWorkArea();
            window.move(area.getX() + 20, area.getY() + 20);
            WRectangle bounds = WDisplayArea.nearest(window).getBounds();
            WPoint position = window.getPosition();
            assertTrue(position.getX() >= bounds.getX() && position.getX() < bounds.getX() + bounds.getWidth());
            assertTrue(position.getY() >= bounds.getY() && position.getY() < bounds.getY() + bounds.getHeight());
        });
    }

    private static void waitForState(WOverlappedPresenter presenter, OverlappedPresenterState expected) throws InterruptedException {
        OverlappedPresenterState actual = null;
        for (int i = 0; i < 50; i++) {
            actual = onUiThreadGet(presenter::getState);
            if (actual == expected) {
                return;
            }
            Thread.sleep(100);
        }
        assertEquals(expected, actual);
    }
}
