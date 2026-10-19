package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the same form as Component.getX / getY / getLocation / getSize / getBounds / getLocationOnScreen,
 * in Java as a Swing developer would write it (getX() is an int, getBounds() is an integer rectangle, getSize() is a
 * width and height pair).
 * Values are DIP multiples of 4 so that physical pixels are integers even at 125% or 175% scale.
 */
class WComponentBoundsForJavaTest {
    private static WCanvas showCanvasWithBox(WCanvas box) {
        WCanvas outer = onUiThreadGet(() -> {
            WCanvas canvas = new WCanvas();
            canvas.setWidth(400);
            canvas.setHeight(300);
            box.setWidth(80);
            box.setHeight(40);
            canvas.add(box, 32, 20);
            return canvas;
        });
        UiTestHarness.attachAndAwaitLoaded(outer);
        onUiThread(outer::updateLayout);
        return outer;
    }

    @Test
    void getXGetYAndGetLocationReturnPositionFromParentTopLeftAsInt() {
        WCanvas box = onUiThreadGet(WCanvas::new);
        WCanvas outer = showCanvasWithBox(box);
        try {
            int x = onUiThreadGet(box::getX);
            int y = onUiThreadGet(box::getY);
            assertEquals(32, x);
            assertEquals(20, y);
            WPoint location = onUiThreadGet(box::getLocation);
            assertEquals(32, location.getX());
            assertEquals(20, location.getY());
        } finally {
            UiTestHarness.detach(outer);
        }
    }

    @Test
    void getSizeIsActualSizeAndGetActualWidthCorrespondsToGetWidth() {
        WCanvas box = onUiThreadGet(WCanvas::new);
        WCanvas outer = showCanvasWithBox(box);
        try {
            WSize size = onUiThreadGet(box::getSize);
            assertEquals(80.0, size.getWidth());
            assertEquals(40.0, size.getHeight());
            assertEquals(80.0, onUiThreadGet(box::getActualWidth));
            assertEquals(40.0, onUiThreadGet(box::getActualHeight));
        } finally {
            UiTestHarness.detach(outer);
        }
    }

    @Test
    void getBoundsIsIntegerRectangleOfPositionAndSize() {
        WCanvas box = onUiThreadGet(WCanvas::new);
        WCanvas outer = showCanvasWithBox(box);
        try {
            WRectangle bounds = onUiThreadGet(box::getBounds);
            assertEquals(32, bounds.getX());
            assertEquals(20, bounds.getY());
            assertEquals(80, bounds.getWidth());
            assertEquals(40, bounds.getHeight());
        } finally {
            UiTestHarness.detach(outer);
        }
    }

    @Test
    void getLocationOnScreenOfElementInWindowIsBelowRightOfWindowContentPosition() {
        WCanvas box = onUiThreadGet(WCanvas::new);
        WCanvas outer = showCanvasWithBox(box);
        try {
            WPoint outerOnScreen = onUiThreadGet(outer::getLocationOnScreen);
            WPoint boxOnScreen = onUiThreadGet(box::getLocationOnScreen);
            // The box is at (32, 20) DIP of the outer Canvas. In physical pixels it is below-right by that distance multiplied by the scale
            assertTrue(boxOnScreen.getX() >= outerOnScreen.getX() + 32);
            assertTrue(boxOnScreen.getY() >= outerOnScreen.getY() + 20);
        } finally {
            UiTestHarness.detach(outer);
        }
    }

    @Test
    void getLocationOnScreenOfElementNotInWindowThrowsIllegalStateException() {
        WButton button = onUiThreadGet(() -> new WButton("Not placed"));
        assertThrows(IllegalStateException.class, () -> onUiThreadGet(button::getLocationOnScreen));
    }

    @Test
    void positionAndSizeOfElementNotInWindowAreZero() {
        WButton button = onUiThreadGet(() -> new WButton("Not placed"));
        assertEquals(0, (int) onUiThreadGet(button::getX));
        assertEquals(0, (int) onUiThreadGet(button::getY));
        assertEquals(new WSize(0.0, 0.0), onUiThreadGet(button::getSize));
        assertEquals(new WRectangle(0, 0, 0, 0), onUiThreadGet(button::getBounds));
    }
}
