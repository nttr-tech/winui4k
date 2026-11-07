package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests that check, as with java.awt.Point / Dimension / Rectangle / Insets, that Java can read coordinate, size and margin values through the fields
 * (p.x, size.width, etc.), and that the existing getters (getX() etc.) still work.
 */
class GeometryFieldsForJavaTest {
    @Test
    void wPointXAndYCanBeReadThroughFieldsAndGetters() {
        WPoint p = new WPoint(10, -20);
        assertEquals(10, p.x);
        assertEquals(-20, p.y);
        assertEquals(10, p.getX());
        assertEquals(-20, p.getY());
    }

    @Test
    void wSizeWidthAndHeightCanBeReadThroughDoubleFieldsAndGetters() {
        WSize size = new WSize(120.5, 0.0);
        double width = size.width;
        double height = size.height;
        assertEquals(120.5, width);
        assertEquals(0.0, height);
        assertEquals(120.5, size.getWidth());
        assertEquals(0.0, size.getHeight());
    }

    @Test
    void wDimensionWidthAndHeightCanBeReadThroughFieldsAndGetters() {
        WDimension d = new WDimension(800, 600);
        assertEquals(800, d.width);
        assertEquals(600, d.height);
        assertEquals(800, d.getWidth());
        assertEquals(600, d.getHeight());
    }

    @Test
    void wRectanglePositionAndSizeCanBeReadThroughFieldsAndGetters() {
        WRectangle r = new WRectangle(1, 2, 3, 4);
        assertEquals(1, r.x);
        assertEquals(2, r.y);
        assertEquals(3, r.width);
        assertEquals(4, r.height);
        assertEquals(1, r.getX());
        assertEquals(4, r.getHeight());
    }

    @Test
    void wInsetsAllFourSidesCanBeReadThroughFieldsAndGetters() {
        WInsets insets = new WInsets(1.0, 2.0, 3.0, 4.0);
        assertEquals(1.0, insets.top);
        assertEquals(2.0, insets.left);
        assertEquals(3.0, insets.bottom);
        assertEquals(4.0, insets.right);
        assertEquals(1.0, insets.getTop());
        assertEquals(4.0, insets.getRight());
    }
}
