package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WBoxLayout and WBox from Java as a Swing developer using BoxLayout / Box would write it
 * (integer constants, the `new BoxLayout(panel, BoxLayout.Y_AXIS)` form, Box's static methods, Box.Filler,
 * and float constants such as Component.LEFT_ALIGNMENT with setAlignmentX).
 */
class WBoxLayoutForJavaTest {
    @Test
    void constantsAndConstructorWithTheSameNamesAndValuesAsBoxLayoutCanBeUsed() {
        assertEquals(0, WBoxLayout.X_AXIS);
        assertEquals(1, WBoxLayout.Y_AXIS);
        assertEquals(2, WBoxLayout.LINE_AXIS);
        assertEquals(3, WBoxLayout.PAGE_AXIS);
        onUiThread(() -> {
            WLayoutPanel panel = new WLayoutPanel();
            WBoxLayout layout = new WBoxLayout(panel, WBoxLayout.PAGE_AXIS);
            panel.setLayout(layout);
            assertSame(panel, layout.getTarget());
            assertEquals(WBoxLayout.PAGE_AXIS, layout.getAxis());
            assertThrows(IllegalArgumentException.class, () -> new WBoxLayout(panel, 9));
        });
    }

    @Test
    void alignmentConstantsSameAsComponentCanBePassedToSetAlignmentXAndSetAlignmentY() {
        assertEquals(0.0f, WComponent.LEFT_ALIGNMENT);
        assertEquals(0.5f, WComponent.CENTER_ALIGNMENT);
        assertEquals(1.0f, WComponent.RIGHT_ALIGNMENT);
        assertEquals(0.0f, WComponent.TOP_ALIGNMENT);
        assertEquals(1.0f, WComponent.BOTTOM_ALIGNMENT);
        onUiThread(() -> {
            WButton button = new WButton("OK");
            assertEquals(WComponent.LEFT_ALIGNMENT, button.getAlignmentX());
            assertEquals(WComponent.CENTER_ALIGNMENT, button.getAlignmentY());
            button.setAlignmentX(WComponent.RIGHT_ALIGNMENT);
            button.setAlignmentY(WComponent.BOTTOM_ALIGNMENT);
            assertEquals(1.0f, button.getAlignmentX());
            assertEquals(1.0f, button.getAlignmentY());
        });
    }

    @Test
    void boxesStrutsAndGlueCanBeCreatedWithBoxStaticMethodsAndFillerCanBeCreatedDirectly() {
        onUiThread(() -> {
            WLayoutPanel vertical = WBox.createVerticalBox();
            WLayoutPanel horizontal = WBox.createHorizontalBox();
            assertEquals(WBoxLayout.Y_AXIS, ((WBoxLayout) vertical.getLayout()).getAxis());
            assertEquals(WBoxLayout.X_AXIS, ((WBoxLayout) horizontal.getLayout()).getAxis());

            vertical.add(new WLabel("Name"));
            vertical.add(WBox.createVerticalStrut(8)); // ints can be passed, as in Swing
            vertical.add(WBox.createRigidArea(new WSize(4.0, 4.0)));
            vertical.add(WBox.createVerticalGlue());
            horizontal.add(WBox.createHorizontalGlue());
            horizontal.add(WBox.createHorizontalStrut(6));
            horizontal.add(WBox.createGlue());
            assertEquals(4, vertical.getComponentCount());
            assertEquals(3, horizontal.getComponentCount());

            WBox.Filler filler = new WBox.Filler(new WSize(1.0, 2.0), new WSize(3.0, 4.0), new WSize(5.0, 6.0));
            assertEquals(new WSize(1.0, 2.0), filler.getMinimumSize());
            assertEquals(new WSize(3.0, 4.0), filler.getPreferredSize());
            assertEquals(new WSize(5.0, 6.0), filler.getMaximumSize());
            filler.changeShape(new WSize(0.0, 0.0), new WSize(7.0, 8.0), new WSize(9.0, 10.0));
            assertEquals(new WSize(7.0, 8.0), filler.getPreferredSize());
        });
    }

    @Test
    void positionsOfChildrenStackedInAVerticalBoxCanBeRead() {
        WLayoutPanel box = UiTestHarness.onUiThreadGet(() -> {
            WLayoutPanel b = WBox.createVerticalBox();
            b.setWidth(100.0);
            b.setHeight(100.0);
            b.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
            b.setVerticalAlignmentInParent(VerticalAlignment.TOP);
            return b;
        });
        WCanvas first = UiTestHarness.onUiThreadGet(() -> rigid(30.0, 10.0));
        WCanvas second = UiTestHarness.onUiThreadGet(() -> rigid(30.0, 10.0));
        onUiThread(() -> {
            box.add(first);
            box.add(WBox.createVerticalStrut(5));
            box.add(second);
            first.setAlignmentX(WComponent.LEFT_ALIGNMENT);
            second.setAlignmentX(WComponent.LEFT_ALIGNMENT);
        });
        UiTestHarness.attachAndAwaitLoaded(box);
        try {
            onUiThread(() -> {
                box.revalidate();
                box.validate();
                box.updateLayout();
                assertEquals(new WRectangle(0, 0, 30, 10), first.getBounds());
                assertEquals(new WRectangle(0, 15, 30, 10), second.getBounds());
            });
        } finally {
            UiTestHarness.detach(box);
        }
    }

    private static WCanvas rigid(double width, double height) {
        WCanvas canvas = new WCanvas();
        canvas.setWidth(width);
        canvas.setHeight(height);
        canvas.setMaximumSize(new WSize(width, height));
        return canvas;
    }
}
