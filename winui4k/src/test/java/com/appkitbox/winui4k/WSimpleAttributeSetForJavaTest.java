package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests in which a developer who has used Swing's SimpleAttributeSet and StyleConstants uses
 * WSimpleAttributeSet and WStyleConstants in the form written in Java (no window is used).
 */
class WSimpleAttributeSetForJavaTest {
    @Test
    void formattingCanBeBuiltAndReadWithTheStaticSettersAndGettersOfStyleConstants() {
        WSimpleAttributeSet attrs = new WSimpleAttributeSet();
        WStyleConstants.setBold(attrs, true);
        WStyleConstants.setItalic(attrs, false);
        WStyleConstants.setUnderline(attrs, true);
        WStyleConstants.setForeground(attrs, WColor.BLUE);
        WStyleConstants.setFontSize(attrs, 24);
        WStyleConstants.setFontFamily(attrs, "Yu Gothic UI");

        assertTrue(WStyleConstants.isBold(attrs));
        assertFalse(WStyleConstants.isItalic(attrs));
        assertTrue(WStyleConstants.isUnderline(attrs));
        assertEquals(WColor.BLUE, WStyleConstants.getForeground(attrs));
        assertEquals(24, WStyleConstants.getFontSize(attrs));
        assertEquals("Yu Gothic UI", WStyleConstants.getFontFamily(attrs));
        assertEquals(6, attrs.getAttributeCount());
    }

    @Test
    void theKeyConstantsCanBePassedDirectlyToAddAttributeAndGetAttribute() {
        WMutableAttributeSet attrs = new WSimpleAttributeSet();
        attrs.addAttribute(WStyleConstants.Bold, Boolean.TRUE);
        attrs.addAttribute(WStyleConstants.FontSize, 18);
        assertEquals(Boolean.TRUE, attrs.getAttribute(WStyleConstants.Bold));
        assertEquals(18, attrs.getAttribute(WStyleConstants.FontSize));
        assertTrue(attrs.isDefined(WStyleConstants.FontSize));
        assertTrue(attrs.containsAttribute(WStyleConstants.Bold, true));

        Set<Object> names = new HashSet<>();
        Enumeration<?> e = attrs.getAttributeNames();
        while (e.hasMoreElements()) {
            names.add(e.nextElement());
        }
        assertEquals(2, names.size());
        assertTrue(names.contains(WStyleConstants.Bold));
    }

    @Test
    void emptyTheCopyConstructorAndEqualsHaveTheSameFormAsSwing() {
        WAttributeSet empty = WSimpleAttributeSet.EMPTY;
        assertEquals(0, empty.getAttributeCount());

        WSimpleAttributeSet a = new WSimpleAttributeSet();
        WStyleConstants.setBold(a, true);
        WSimpleAttributeSet b = new WSimpleAttributeSet(a);
        assertEquals(a, b);
        WStyleConstants.setBold(a, false);
        assertTrue(WStyleConstants.isBold(b));
        assertFalse(a.equals(b));
        assertTrue(new WSimpleAttributeSet().isEmpty());
    }
}
