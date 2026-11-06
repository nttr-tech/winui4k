package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WFlowLayout and WGridLayout from Java as a Swing developer using FlowLayout / GridLayout would write it
 * (integer constants, constructors with Swing's argument order, getters / setters, use in a WLayoutPanel).
 */
class WFlowGridLayoutForJavaTest {
    private static WCanvas box(double width, double height) {
        WCanvas canvas = new WCanvas();
        canvas.setWidth(width);
        canvas.setHeight(height);
        return canvas;
    }

    @Test
    void constantsConstructorsAndAccessorsWithTheSameNamesAndValuesAsFlowLayout() {
        assertEquals(0, WFlowLayout.LEFT);
        assertEquals(1, WFlowLayout.CENTER);
        assertEquals(2, WFlowLayout.RIGHT);
        assertEquals(3, WFlowLayout.LEADING);
        assertEquals(4, WFlowLayout.TRAILING);

        WFlowLayout byDefault = new WFlowLayout();
        assertEquals(WFlowLayout.CENTER, byDefault.getAlignment());
        assertEquals(5.0, byDefault.getHgap());
        assertEquals(5.0, byDefault.getVgap());

        WFlowLayout aligned = new WFlowLayout(WFlowLayout.RIGHT);
        assertEquals(WFlowLayout.RIGHT, aligned.getAlignment());
        assertEquals(5.0, aligned.getHgap());

        WFlowLayout full = new WFlowLayout(WFlowLayout.LEFT, 8, 9); // int gaps can be passed too
        full.setAlignment(WFlowLayout.TRAILING);
        full.setHgap(2.0);
        full.setVgap(3.0);
        assertEquals(WFlowLayout.TRAILING, full.getAlignment());
        assertEquals(2.0, full.getHgap());
        assertEquals(3.0, full.getVgap());
    }

    @Test
    void preferredSizeOfWLayoutPanelWithWFlowLayoutCanBeRead() {
        onUiThread(() -> {
            WLayoutPanel panel = new WLayoutPanel(new WFlowLayout(WFlowLayout.LEFT, 4, 6));
            panel.add(box(30.0, 10.0));
            panel.add(box(20.0, 12.0));
            // Width 30 + 20 + 4 + 4 x 2 = 62, height 12 + 6 x 2 = 24
            assertEquals(new WSize(62.0, 24.0), panel.getPreferredSize());
        });
    }

    @Test
    void constructorsAndAccessorsWithGridLayoutArgumentOrderAndExceptionWhenRowsAndColumnsAreBoth0() {
        WGridLayout byDefault = new WGridLayout();
        assertEquals(1, byDefault.getRows());
        assertEquals(0, byDefault.getColumns());

        WGridLayout grid = new WGridLayout(2, 3, 4, 5); // int gaps can be passed too
        assertEquals(2, grid.getRows());
        assertEquals(3, grid.getColumns());
        assertEquals(4.0, grid.getHgap());
        assertEquals(5.0, grid.getVgap());
        grid.setRows(5);
        grid.setColumns(6);
        grid.setHgap(1.0);
        grid.setVgap(2.0);
        assertEquals(5, grid.getRows());
        assertEquals(6, grid.getColumns());
        assertEquals(1.0, grid.getHgap());
        assertEquals(2.0, grid.getVgap());

        assertThrows(IllegalArgumentException.class, () -> new WGridLayout(0, 0));
        WGridLayout rowsOnly = new WGridLayout(2, 0);
        assertThrows(IllegalArgumentException.class, () -> rowsOnly.setRows(0));
    }

    @Test
    void preferredSizeOfWLayoutPanelWithWGridLayoutCanBeRead() {
        onUiThread(() -> {
            WLayoutPanel panel = new WLayoutPanel(new WGridLayout(2, 3, 4, 4));
            for (int i = 0; i < 5; i++) {
                panel.add(box(10.0 + i, 8.0));
            }
            // 2 rows 3 columns, cells 14 x 8. Width 3 x 14 + 2 x 4 = 50, height 2 x 8 + 4 = 20
            assertEquals(new WSize(50.0, 20.0), panel.getPreferredSize());
        });
    }
}
