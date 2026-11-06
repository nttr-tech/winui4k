package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests that use getComponentAt / findComponentAt in the Java a Swing developer writes
 * (looking up the child at the pressed position from getPoint() of a panel's mouse event).
 */
class WContainerComponentAtForJavaTest {
    @Test
    void getComponentAtAndFindComponentAtLookUpTheChildFromTheMousePosition() {
        WCanvas[] holder = new WCanvas[2];
        WButton[] button = new WButton[1];
        WLabel[] label = new WLabel[1];
        onUiThread(() -> {
            WCanvas canvas = new WCanvas();
            canvas.setPreferredSize(new WSize(200, 100));
            WButton ok = new WButton("OK");
            canvas.add(ok, 10, 10);
            ok.setSize(80, 30);
            WCanvas inner = new WCanvas();
            canvas.add(inner, 100, 10);
            inner.setSize(80, 60);
            WLabel caption = new WLabel("Heading");
            inner.add(caption, 0, 0);
            caption.setSize(60, 30);
            holder[0] = canvas;
            holder[1] = inner;
            button[0] = ok;
            label[0] = caption;
        });
        WCanvas canvas = holder[0];
        UiTestHarness.attachAndAwaitLoaded(canvas);
        try {
            onUiThread(() -> {
                canvas.updateLayout();
                WMouseEvent press = new WMouseEvent(canvas, WMouseEvent.MOUSE_PRESSED, 0L, 0, 110.0, 20.0, 1, false,
                        WMouseEvent.BUTTON1);
                assertSame(holder[1], canvas.getComponentAt(press.getPoint()));
                assertSame(label[0], canvas.findComponentAt(press.getPoint()));
                assertSame(button[0], canvas.getComponentAt(20, 20));
                assertSame(canvas, canvas.getComponentAt(195, 95));
                assertNull(canvas.getComponentAt(-1, 0));
            });
        } finally {
            UiTestHarness.detach(canvas);
        }
        assertNull(onUiThreadGet(() -> new WPanel().findComponentAt(0, 0)));
    }
}
