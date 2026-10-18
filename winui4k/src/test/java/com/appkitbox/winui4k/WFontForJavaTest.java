package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WFont from Java, written the way Swing developers who used java.awt.Font would write them
 * (verifying a value type that needs no window).
 */
class WFontForJavaTest {
    @Test
    void styleConstantsHaveTheSameValuesAsAwtFontAndAreUsableAsStaticFields() {
        assertEquals(Font.PLAIN, WFont.PLAIN);
        assertEquals(Font.BOLD, WFont.BOLD);
        assertEquals(Font.ITALIC, WFont.ITALIC);
    }

    @Test
    void canBeCreatedWithAnIntSizeAndReadsBothIntegerAndFractionalPoints() {
        WFont font = new WFont("Segoe UI", WFont.BOLD, 14);
        assertEquals("Segoe UI", font.getName());
        assertEquals(WFont.BOLD, font.getStyle());
        assertEquals(14, font.getSize());
        float size2D = font.getSize2D(); // The same float as AWT's Font.getSize2D()
        assertEquals(14.0f, size2D);
        assertTrue(font.isBold());
        assertFalse(font.isItalic());
    }

    @Test
    void creatingWithADoubleSizeRoundsGetSizeAndKeepsTheOriginalValueInGetSize2D() {
        WFont font = new WFont("Yu Gothic UI", WFont.PLAIN, 12.6);
        assertEquals(13, font.getSize());
        float size2D = font.getSize2D();
        assertEquals(12.6f, size2D);
    }

    @Test
    void isBoldAndIsItalicAreDeterminedIndependentlyByEachStyleBit() {
        WFont plain = new WFont("Segoe UI", WFont.PLAIN, 12);
        assertFalse(plain.isBold());
        assertFalse(plain.isItalic());
        assertFalse(new WFont("Segoe UI", WFont.BOLD, 12).isItalic());
        assertFalse(new WFont("Segoe UI", WFont.ITALIC, 12).isBold());
        assertTrue(new WFont("Segoe UI", WFont.ITALIC, 12).isItalic());
    }

    @Test
    void boldAndItalicCanBeCombinedWithBitwiseOr() {
        WFont font = new WFont("Consolas", WFont.BOLD | WFont.ITALIC, 12);
        assertTrue(font.isBold());
        assertTrue(font.isItalic());
    }

    @Test
    void deriveFontReturnsANewFontWithOnlyTheSizeReplacedForFloatOrTheStyleForInt() {
        WFont base = new WFont("Consolas", WFont.PLAIN, 12);
        assertEquals(new WFont("Consolas", WFont.BOLD, 12), base.deriveFont(WFont.BOLD));
        assertEquals(new WFont("Consolas", WFont.PLAIN, 20), base.deriveFont(20f));
        assertEquals(new WFont("Consolas", WFont.ITALIC, 9), base.deriveFont(WFont.ITALIC, 9f));
        assertEquals(new WFont("Consolas", WFont.PLAIN, 12), base);
    }

    @Test
    void equalsAndHashCodeMatchOnlyWhenNameStyleAndSizeAreAllEqual() {
        WFont base = new WFont("Consolas", WFont.BOLD, 16);
        assertEquals(base, new WFont("Consolas", WFont.BOLD, 16.0));
        assertEquals(base.hashCode(), new WFont("Consolas", WFont.BOLD, 16.0).hashCode());
        assertNotEquals(base, new WFont("Consolas", WFont.PLAIN, 16));
        assertNotEquals(base, new WFont("Consolas", WFont.BOLD, 17));
        assertNotEquals(base, new WFont("Meiryo", WFont.BOLD, 16));
    }

    @Test
    void aFontWithASizeOfZeroOrLessOrNaNCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> new WFont("Consolas", WFont.PLAIN, 0));
        assertThrows(IllegalArgumentException.class, () -> new WFont("Consolas", WFont.PLAIN, -1));
        assertThrows(IllegalArgumentException.class, () -> new WFont("Consolas", WFont.PLAIN, Double.NaN));
    }

    @Test
    void aFontWithUndefinedStyleBitsCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> new WFont("Consolas", 4, 12));
        assertThrows(IllegalArgumentException.class, () -> new WFont("Consolas", -1, 12));
    }
}
