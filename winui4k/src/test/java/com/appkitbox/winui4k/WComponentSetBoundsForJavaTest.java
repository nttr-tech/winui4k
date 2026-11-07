package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests that use Component.setBounds / setLocation / setSize from Java as a Swing developer placing children in a null-layout JPanel would write it
 * (int arguments, and the forms taking WRectangle, WPoint and WSize, which correspond to Rectangle, Point and Dimension).
 */
class WComponentSetBoundsForJavaTest {
    @Test
    void childrenOfANullLayoutPanelCanBePlacedWithIntCoordinates() {
        WCanvas canvas = UiTestHarness.onUiThreadGet(() -> {
            WCanvas c = new WCanvas();
            c.setWidth(400.0);
            c.setHeight(300.0);
            c.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
            c.setVerticalAlignmentInParent(VerticalAlignment.TOP);
            return c;
        });
        WLabel label = UiTestHarness.onUiThreadGet(() -> new WLabel("Name"));
        WCanvas box = UiTestHarness.onUiThreadGet(WCanvas::new);
        UiTestHarness.onUiThread(() -> {
            canvas.add(label);
            canvas.add(box);
            label.setBounds(10, 20, 120, 30); // ints can be passed, as in Swing
            box.setLocation(new WPoint(5, 60));
            box.setSize(new WSize(40.0, 50.0));
        });
        UiTestHarness.attachAndAwaitLoaded(canvas);
        try {
            UiTestHarness.onUiThread(() -> {
                canvas.updateLayout();
                assertEquals(new WRectangle(10, 20, 120, 30), label.getBounds());
                assertEquals(new WRectangle(5, 60, 40, 50), box.getBounds());
                box.setBounds(new WRectangle(1, 2, 3, 4));
                box.setLocation(7, 8);
                box.setSize(9, 10);
                canvas.updateLayout();
                assertEquals(new WRectangle(7, 8, 9, 10), box.getBounds());
            });
        } finally {
            UiTestHarness.detach(canvas);
        }
    }
}
