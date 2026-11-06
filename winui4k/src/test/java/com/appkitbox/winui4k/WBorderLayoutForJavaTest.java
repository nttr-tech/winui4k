package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WBorderLayout and WLayoutManager from Java as a Swing developer using BorderLayout / LayoutManager2 would write it
 * (constants such as WBorderLayout.NORTH, string constraints, setHgap / setVgap, getLayoutComponent / getConstraints,
 * and that a manager implemented in Java can be used with the default methods as they are).
 */
class WBorderLayoutForJavaTest {
    /** A Java layout manager that implements only preferredLayoutSize and layoutContainer (uses the default methods). */
    private static final class MinimalLayout implements WLayoutManager {
        @Override
        public WSize preferredLayoutSize(WLayoutPanel parent) {
            return new WSize(70.0, 30.0);
        }

        @Override
        public void layoutContainer(WLayoutPanel parent) {}
    }

    /** A Java layout manager that overrides minimumLayoutSize and invalidateLayout. */
    private static final class OverridingLayout implements WLayoutManager {
        int invalidated;

        @Override
        public WSize preferredLayoutSize(WLayoutPanel parent) {
            return new WSize(70.0, 30.0);
        }

        @Override
        public WSize minimumLayoutSize(WLayoutPanel parent) {
            return new WSize(20.0, 10.0);
        }

        @Override
        public void invalidateLayout(WLayoutPanel parent) {
            invalidated++;
        }

        @Override
        public void layoutContainer(WLayoutPanel parent) {}
    }

    @Test
    void constraintsCanBeWrittenWithBorderLayoutConstantNamesAndSetHgapSetVgapGetLayoutComponentGetConstraintsWork() {
        onUiThread(() -> {
            WBorderLayout layout = new WBorderLayout(2.0, 3.0);
            layout.setHgap(6.0);
            layout.setVgap(7.0);
            assertEquals(6.0, layout.getHgap());
            assertEquals(7.0, layout.getVgap());

            WLayoutPanel panel = new WLayoutPanel(layout);
            WLabel north = new WLabel("north");
            WLabel start = new WLabel("start");
            WLabel center = new WLabel("center");
            panel.add(north, WBorderLayout.NORTH);
            panel.add(start, WBorderLayout.LINE_START);
            panel.add(center, "Center"); // Swing's string constraints pass through as is

            assertSame(north, layout.getLayoutComponent(WBorderLayout.NORTH));
            assertSame(start, layout.getLayoutComponent(WBorderLayout.LINE_START));
            assertSame(center, layout.getLayoutComponent(WBorderLayout.CENTER));
            assertSame(start, layout.getLayoutComponent(panel, WBorderLayout.WEST));
            assertNull(layout.getLayoutComponent(WBorderLayout.SOUTH));
            assertEquals(WBorderLayout.NORTH, layout.getConstraints(north));
            assertEquals(WBorderLayout.LINE_START, layout.getConstraints(start));
            assertEquals(WBorderLayout.CENTER, layout.getConstraints(center));
            assertNull(layout.getConstraints(new WLabel("other")));

            assertThrows(IllegalArgumentException.class, () -> panel.add(new WLabel("x"), "Nowhere"));
            assertThrows(IllegalArgumentException.class, () -> layout.getLayoutComponent("Nowhere"));
        });
    }

    @Test
    void allNineConstantsExistWithBorderLayoutNames() {
        assertEquals(WBorderLayout.Constraint.NORTH, WBorderLayout.NORTH);
        assertEquals(WBorderLayout.Constraint.SOUTH, WBorderLayout.SOUTH);
        assertEquals(WBorderLayout.Constraint.EAST, WBorderLayout.EAST);
        assertEquals(WBorderLayout.Constraint.WEST, WBorderLayout.WEST);
        assertEquals(WBorderLayout.Constraint.CENTER, WBorderLayout.CENTER);
        assertEquals(WBorderLayout.Constraint.PAGE_START, WBorderLayout.PAGE_START);
        assertEquals(WBorderLayout.Constraint.PAGE_END, WBorderLayout.PAGE_END);
        assertEquals(WBorderLayout.Constraint.LINE_START, WBorderLayout.LINE_START);
        assertEquals(WBorderLayout.Constraint.LINE_END, WBorderLayout.LINE_END);
    }

    @Test
    void managerImplementedInJavaWorksWithoutImplementingTheNewDefaultMethodsAndCanOverrideThem() {
        onUiThread(() -> {
            MinimalLayout minimal = new MinimalLayout();
            WLayoutPanel plain = new WLayoutPanel(minimal);
            assertEquals(new WSize(70.0, 30.0), minimal.minimumLayoutSize(plain));
            assertEquals(
                    new WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), minimal.maximumLayoutSize(plain));
            minimal.invalidateLayout(plain); // Does nothing by default

            OverridingLayout overriding = new OverridingLayout();
            WLayoutPanel panel = new WLayoutPanel(overriding);
            assertEquals(new WSize(20.0, 10.0), panel.getMinimumSize());
            panel.invalidate();
            assertEquals(1, overriding.invalidated);
        });
    }
}
