package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * WColor can be created from Java with {@code new WColor(r, g, b)} and {@code new WColor(r, g, b, a)}
 * (a value type that needs no window).
 */
class WColorForJavaTest {
    @Test
    void creatingWithThreeComponentsGivesAnOpaqueAlphaOf255() {
        WColor color = new WColor(10, 20, 30);
        assertEquals(10, color.getRed());
        assertEquals(20, color.getGreen());
        assertEquals(30, color.getBlue());
        assertEquals(255, color.getAlpha());
        assertEquals(new WColor(10, 20, 30, 255), color);
    }

    @Test
    void creatingWithFourComponentsLetsYouSpecifyAlpha() {
        assertEquals(128, new WColor(10, 20, 30, 128).getAlpha());
    }
}
