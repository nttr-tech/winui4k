package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WIcon (WImageIcon, WFontIcon, Symbol) from Java, written the way developers used to Swing's
 * ImageIcon / Icon would write them. Checks their behavior as values (path-to-URI conversion and equals / hashCode).
 * What they display is covered by WIconTest.
 */
class WIconForJavaTest {
    @Test
    void aSymbolCanBeUsedDirectlyAsAWIcon() {
        WIcon icon = Symbol.SAVE;
        assertEquals(Symbol.SAVE, icon);
    }

    // ---- WImageIcon ----

    @Test
    void aFilePathBecomesAnAbsoluteFileUriAndGetPathReturnsItAsPassed() {
        WImageIcon icon = new WImageIcon("C:/icons/save.png");
        assertEquals("file:///C:/icons/save.png", icon.getUri());
        assertEquals("C:/icons/save.png", icon.getPath());
    }

    @Test
    void aWindowsPathWithBackslashesGivesTheSameFileUriAndAnEqualIcon() {
        WImageIcon slash = new WImageIcon("C:/icons/save.png");
        WImageIcon backslash = new WImageIcon("C:\\icons\\save.png");
        assertEquals("file:///C:/icons/save.png", backslash.getUri());
        assertEquals(slash, backslash);
        assertEquals(slash.hashCode(), backslash.hashCode());
    }

    @Test
    void spacesInAPathArePercentEncodedInTheUri() {
        assertEquals("file:///C:/My%20Icons/save.png", new WImageIcon("C:/My Icons/save.png").getUri());
    }

    @Test
    void aRelativePathIsResolvedAgainstTheWorkingDirectoryLikeImageIcon() {
        String expected = new File(System.getProperty("user.dir"), "icons/save.png").toPath().toUri().toString();
        assertEquals(expected, new WImageIcon("icons/save.png").getUri());
    }

    @Test
    void fileMsAppxHttpAndHttpsUrisAreUsedAsIs() {
        String[] uris = {
            "file:///C:/icons/save.png",
            "ms-appx:///Assets/save.png",
            "http://example.com/save.png",
            "https://example.com/save.png",
        };
        for (String uri : uris) {
            assertEquals(uri, new WImageIcon(uri).getUri());
        }
    }

    @Test
    void iconsPointingToDifferentFilesAreNotEqual() {
        assertNotEquals(new WImageIcon("C:/icons/save.png"), new WImageIcon("C:/icons/open.png"));
    }

    @Test
    void anEmptyPathThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new WImageIcon(""));
    }

    // ---- WFontIcon ----

    @Test
    void aWFontIconWithoutAFontIsASegoeFluentIconsGlyph() {
        WFontIcon icon = new WFontIcon("\uE74E");
        assertEquals("\uE74E", icon.getGlyph());
        assertEquals("Segoe Fluent Icons", icon.getFontFamily());
        assertEquals(WFontIcon.DEFAULT_FONT_FAMILY, icon.getFontFamily());
    }

    @Test
    void wFontIconsWithTheSameGlyphAndFontAreEqual() {
        assertEquals(new WFontIcon("\uE74E"), new WFontIcon("\uE74E", "Segoe Fluent Icons"));
        assertEquals(new WFontIcon("\uE74E").hashCode(), new WFontIcon("\uE74E", "Segoe Fluent Icons").hashCode());
        assertNotEquals(new WFontIcon("\uE74E"), new WFontIcon("\uE74E", "Segoe MDL2 Assets"));
        assertNotEquals(new WFontIcon("\uE74E"), new WFontIcon("\uE8E5"));
    }

    @Test
    void anEmptyGlyphOrEmptyFontNameThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new WFontIcon(""));
        assertThrows(IllegalArgumentException.class, () -> new WFontIcon("\uE74E", ""));
    }

    // ---- Custom implementations (written as if implementing Swing's Icon) ----

    /** An icon implemented in Java like a Swing Icon. WIcon is sealed in Kotlin, but Java 8 output won't enforce it. */
    private static WIcon myIcon() {
        return new WIcon() {
        };
    }

    @Test
    void passingACustomWIconToSetIconThrowsIllegalArgumentExceptionListingTheThreeUsableTypesAndKeepsTheIcon() {
        onUiThread(() -> {
            WButton button = new WButton("Save", Symbol.SAVE);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> button.setIcon(myIcon()));
            String message = e.getMessage();
            assertTrue(message.contains("Symbol"), message);
            assertTrue(message.contains("WImageIcon"), message);
            assertTrue(message.contains("WFontIcon"), message);
            assertEquals(Symbol.SAVE, button.getIcon());
            assertEquals("Save", button.getText());
        });
    }

    @Test
    void passingACustomWIconToAConstructorAlsoThrowsIllegalArgumentException() {
        onUiThread(() -> {
            assertThrows(IllegalArgumentException.class, () -> new WButton(myIcon()));
            assertThrows(IllegalArgumentException.class, () -> new WButton("Save", myIcon()));
        });
    }

    @Test
    void afterRejectingACustomWIconNullCanStillRemoveTheIcon() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            assertThrows(IllegalArgumentException.class, () -> button.setIcon(myIcon()));
            assertNull(button.getIcon());
            button.setIcon(Symbol.SAVE);
            button.setIcon(null);
            assertNull(button.getIcon());
        });
    }
}
