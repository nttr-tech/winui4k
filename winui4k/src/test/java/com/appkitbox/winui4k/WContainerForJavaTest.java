package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use Container's child list API (add(Component, int), getComponent, getComponents, getComponentCount, remove(int),
 * isAncestorOf, getParent, revalidate / validate / invalidate / repaint) from Java as a Swing developer would write it.
 */
class WContainerForJavaTest {
    /** A layout manager that records the constraints it receives (implemented in Java). */
    private static final class RecordingLayout implements WLayoutManager {
        final List<Object> constraints = new ArrayList<>();
        int layoutCount;

        @Override
        public void addLayoutComponent(WComponent component, Object constraint) {
            constraints.add(constraint);
        }

        @Override
        public WSize preferredLayoutSize(WLayoutPanel parent) {
            return new WSize(10.0, 10.0);
        }

        @Override
        public void layoutContainer(WLayoutPanel parent) {
            layoutCount++;
        }
    }

    @Test
    void addAtPositionGetComponentGetComponentsAndGetComponentCountWorkLikeSwing() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WLabel first = new WLabel("first");
            WLabel second = new WLabel("second");
            WButton button = new WButton("OK");
            panel.add(second);
            panel.add(first, 0);
            panel.add(button, -1);
            assertEquals(3, panel.getComponentCount());
            assertSame(first, panel.getComponent(0));
            assertArrayEquals(new WComponent[] {first, second, button}, panel.getComponents());
            panel.remove(1);
            assertArrayEquals(new WComponent[] {first, button}, panel.getComponents());
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> panel.getComponent(5));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> panel.remove(5));
            assertThrows(IllegalArgumentException.class, () -> panel.add(second, 9));
        });
    }

    @Test
    void passingAnIntegerToWLayoutPanelAddSpecifiesAPositionNotAConstraint() {
        onUiThread(() -> {
            RecordingLayout layout = new RecordingLayout();
            WLayoutPanel panel = new WLayoutPanel(layout);
            WLabel a = new WLabel("a");
            WLabel b = new WLabel("b");
            WLabel c = new WLabel("c");
            panel.add(a);
            panel.add(b, 0); // As in Swing, add(Component, int) is chosen
            panel.add(c, WBorderLayout.Constraint.NORTH, 1);
            assertArrayEquals(new WComponent[] {b, c, a}, panel.getComponents());
            assertEquals(Arrays.asList(null, null, WBorderLayout.Constraint.NORTH), layout.constraints);
            assertEquals(Arrays.asList(b, c, a), panel.getLayoutChildren());
        });
    }

    @Test
    void getParentAndIsAncestorOfFollowTheHierarchyAndAddingToAnotherPanelRemovesFromTheOriginal() {
        onUiThread(() -> {
            WPanel outer = new WPanel();
            WPanel inner = new WPanel();
            WBorder border = new WBorder();
            WLabel label = new WLabel("label");
            outer.add(inner);
            inner.add(border);
            border.setChild(label);
            assertSame(border, label.getParent());
            assertSame(inner, border.getParent());
            assertTrue(outer.isAncestorOf(label));
            assertFalse(inner.isAncestorOf(outer));

            outer.add(border);
            assertEquals(0, inner.getComponentCount());
            assertSame(outer, border.getParent());
            assertThrows(IllegalArgumentException.class, () -> inner.add(outer));

            outer.removeAll();
            assertNull(inner.getParent());
            assertNull(border.getParent());
        });
    }

    @Test
    void swingReLayoutCallsCanBeWrittenAsIsAndValidateRunsTheScheduledLayoutImmediately() {
        onUiThread(() -> {
            RecordingLayout layout = new RecordingLayout();
            WLayoutPanel panel = new WLayoutPanel(layout);
            WLabel label = new WLabel("label");
            panel.add(label);
            label.setText("Change to a longer string");
            label.invalidate();
            label.revalidate();
            label.repaint();
            assertEquals(0, layout.layoutCount); // revalidate only schedules
            panel.validate();
            assertEquals(1, layout.layoutCount);
        });
    }
}
