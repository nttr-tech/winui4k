package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/** Tests that specify an image for WImage the way a developer used to Swing's JLabel(Icon) and ImageIcon would write it in Java. */
class WImageForJavaTest {
    private static String tempImagePath() throws IOException {
        File folder = Files.createTempDirectory("winui4k image java test ").toFile();
        folder.deleteOnExit();
        File file = new File(folder, "icon.png");
        file.deleteOnExit();
        return file.getPath();
    }

    @Test
    void creatingWithAFilePathStringGivesAFileUriAsSourceUri() throws IOException {
        String path = tempImagePath();
        onUiThread(() -> {
            WImage image = new WImage(path);
            assertTrue(image.getSourceUri().startsWith("file:///"));
            assertTrue(image.getSourceUri().endsWith("/icon.png"));
        });
    }

    @Test
    void creatingWithWImageIconShapedLikeImageIconLetsGetIconReadTheSameIcon() throws IOException {
        String path = tempImagePath();
        onUiThread(() -> {
            WImageIcon icon = new WImageIcon(path);
            WImage image = new WImage(icon);
            assertEquals(icon, image.getIcon());
            assertEquals(icon.getUri(), image.getSourceUri());
        });
    }

    @Test
    void setIconReplacesTheImageAndNullRemovesIt() throws IOException {
        String path = tempImagePath();
        onUiThread(() -> {
            WImage image = new WImage();
            assertNull(image.getIcon());
            image.setIcon(new WImageIcon(path));
            assertEquals(new WImageIcon(path), image.getIcon());
            image.setIcon(null);
            assertNull(image.getIcon());
            assertNull(image.getSourceUri());
        });
    }

    @Test
    void setIconWithASymbolThrowsIllegalArgumentExceptionBecauseAnImageCannotShowIt() {
        onUiThread(() -> {
            WImage image = new WImage();
            assertThrows(IllegalArgumentException.class, () -> image.setIcon(Symbol.FIND));
            assertNull(image.getIcon());
        });
    }

    @Test
    void canStillBeCreatedWithAUriStringAndSetStretchCanBeUsed() {
        onUiThread(() -> {
            WImage image = new WImage("ms-appx:///Assets/logo.png");
            assertEquals("ms-appx:///Assets/logo.png", image.getSourceUri());
            image.setStretch(Stretch.UNIFORM_TO_FILL);
            assertEquals(Stretch.UNIFORM_TO_FILL, image.getStretch());
        });
    }
}
