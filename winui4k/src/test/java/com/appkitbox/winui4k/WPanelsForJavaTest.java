package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WPanel, WGrid, WVariableSizedWrapGrid, WBorder and WSplitView straightforwardly from Java
 * (overloaded constructors and methods, GridLength's static factories, getters / setters).
 */
class WPanelsForJavaTest {
    @Test
    void wPanelCanBeCreatedWithTheThreeConstructorsNoArgsSpacingOnlyAndSpacingAndOrientation() {
        onUiThread(() -> {
            WPanel plain = new WPanel();
            assertEquals(Orientation.VERTICAL, plain.getOrientation());
            assertEquals(0.0, plain.getSpacing());

            WPanel spaced = new WPanel(8.0);
            assertEquals(8.0, spaced.getSpacing());
            assertEquals(Orientation.VERTICAL, spaced.getOrientation());

            WPanel row = new WPanel(4.0, Orientation.HORIZONTAL);
            assertEquals(Orientation.HORIZONTAL, row.getOrientation());
            row.setSpacing(12.0);
            assertEquals(12.0, row.getSpacing());
        });
    }

    @Test
    void wGridCanAddRowsAndColumnsAndSpecifyCellsAndAddCanBeCalledWithAPositionOnlyOrWithSpans() {
        onUiThread(() -> {
            WGrid grid = new WGrid(4.0, 8.0);
            assertEquals(4.0, grid.getRowSpacing());
            assertEquals(8.0, grid.getColumnSpacing());
            grid.addRow(GridLength.pixel(50.0));
            grid.addRow(); // Auto by default
            grid.addColumn(GridLength.star());
            grid.addColumn(GridLength.star(2.0));
            WPanel a = new WPanel();
            WPanel b = new WPanel();
            grid.add(a, 0, 0);
            grid.add(b, 1, 0, 1, 2);
            assertEquals(2, grid.getComponentCount());
            assertSame(a, grid.getComponent(0));
            assertSame(b, grid.getComponent(1));
        });
    }

    @Test
    void wVariableSizedWrapGridIsCreatedWithCellSizesAndChildrenCanBeAddedWithSpans() {
        onUiThread(() -> {
            WVariableSizedWrapGrid grid = new WVariableSizedWrapGrid(40.0, 20.0);
            assertEquals(40.0, grid.getItemWidth());
            assertEquals(20.0, grid.getItemHeight());
            grid.setOrientation(Orientation.HORIZONTAL);
            grid.setMaximumRowsOrColumns(3);
            WPanel child = new WPanel();
            grid.add(child, 2, 1);
            assertEquals(3, grid.getMaximumRowsOrColumns());
            assertSame(child, grid.getComponent(0));
        });
    }

    @Test
    void wBorderAndWSplitViewCanBeCreatedWithChildrenAndReadBackWithGetters() {
        onUiThread(() -> {
            WPanel child = new WPanel();
            WBorder border = new WBorder(child);
            assertSame(child, border.getChild());
            assertSame(border, child.getParent());

            WPanel pane = new WPanel();
            WPanel content = new WPanel();
            WSplitView split = new WSplitView(pane, content);
            assertSame(pane, split.getPane());
            assertSame(content, split.getContent());
            split.setPaneOpen(true);
            assertEquals(true, split.isPaneOpen());
            split.setDisplayMode(SplitViewDisplayMode.INLINE);
            assertEquals(SplitViewDisplayMode.INLINE, split.getDisplayMode());
        });
    }
}
