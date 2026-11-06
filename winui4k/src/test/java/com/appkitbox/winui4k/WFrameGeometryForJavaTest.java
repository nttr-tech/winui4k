package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that handle the position and size of WFrame in the same way developers who have used JFrame would write it in Java:
 * {@code frame.setSize(400, 300)}, {@code frame.setLocationRelativeTo(null)}, {@code frame.pack()},
 * {@code frame.setMinimumSize(new WDimension(...))}, {@code frame.getBounds()}, etc. Own windows are disposed at the end.
 */
class WFrameGeometryForJavaTest {
    private static final String TITLE = "winui4k frame geometry java test";

    private WFrame frame;

    @BeforeEach
    void openTheSharedWindow() {
        UiTestHarness.sharedFrame(); // So that closing its own window does not exit the app
    }

    @AfterEach
    void closeOwnWindow() {
        if (frame != null) {
            onUiThread(frame::dispose);
        }
    }

    @Test
    void setSizeThenSetLocationRelativeToInTheSameOrderAsJFrameShowsItAtTheCenterOfTheScreen() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            frame.setDefaultCloseOperation(WFrame.DISPOSE_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
        onUiThread(() -> {
            assertEquals(400, frame.getWidth());
            assertEquals(300, frame.getHeight());
            assertEquals(new WDimension(400, 300), frame.getSize());
            WRectangle area = WDisplayArea.primary().getWorkArea();
            WDimension size = frame.getAppWindow().getSize();
            WPoint position = frame.getAppWindow().getPosition();
            assertEquals(area.getX() + (area.getWidth() - size.getWidth()) / 2, position.getX());
            assertEquals(area.getY() + (area.getHeight() - size.getHeight()) / 2, position.getY());
        });
    }

    @Test
    void setBoundsSetLocationGetBoundsGetXAndGetYHaveTheSameShapeAsSwing() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            frame.setBounds(120, 90, 500, 350);
            assertEquals(new WRectangle(120, 90, 500, 350), frame.getBounds());
            frame.setLocation(140, 100);
            assertEquals(140, frame.getX());
            assertEquals(100, frame.getY());
            assertEquals(new WPoint(140, 100), frame.getLocation());
            frame.setBounds(new WRectangle(150, 110, 450, 320));
            assertEquals(new WRectangle(150, 110, 450, 320), frame.getBounds());
            frame.setSize(new WDimension(460, 330));
            assertEquals(new WDimension(460, 330), frame.getSize());
        });
    }

    @Test
    void setSizeSmallerThanSetMinimumSizeIsEnlargedToMinimumSizeAndNullRemovesIt() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            assertNull(frame.getMinimumSize());
            frame.setMinimumSize(new WDimension(450, 350));
            assertEquals(new WDimension(450, 350), frame.getMinimumSize());
            frame.setSize(100, 100);
            assertEquals(new WDimension(450, 350), frame.getSize());
            frame.setMinimumSize(null);
            frame.setSize(300, 200);
            assertEquals(new WDimension(300, 200), frame.getSize());
        });
    }

    @Test
    void packAfterAddingChildrenMakesTheClientAreaTheSizeOfTheContent() throws InterruptedException {
        WPanel panel = onUiThreadGet(() -> {
            frame = new WFrame(TITLE);
            WPanel box = new WPanel();
            box.setWidth(200.0);
            box.setHeight(100.0);
            frame.add(box);
            frame.pack();
            frame.setVisible(true);
            return box;
        });
        // The client area becomes 248 x 148 (DIP), the contentPane padding of 24 added on every side, and the content lines up at 200 x 100
        long deadline = System.currentTimeMillis() + 5_000;
        while (Math.abs(onUiThreadGet(() -> frame.getContentPane().getActualWidth()) - 200.0) > 1.0
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        onUiThread(() -> {
            assertEquals(200.0, frame.getContentPane().getActualWidth(), 1.0); // getActualWidth does not include the padding
            assertEquals(100.0, frame.getContentPane().getActualHeight(), 1.0);
            assertEquals(200.0, panel.getActualWidth(), 0.5);
            assertTrue(frame.getHeight() > 148, "getHeight is the height of the whole window including the title bar");
        });
    }
}
