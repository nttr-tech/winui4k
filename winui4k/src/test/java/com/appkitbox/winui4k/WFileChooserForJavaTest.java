package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WFileChooser in the same way developers who have used JFileChooser would write it in Java
 * (new WFileChooser(dir), comparisons with constants, getters / setters, anonymous FileFilter classes, callback lambdas).
 * The OS dialogs are not opened.
 */
class WFileChooserForJavaTest {
    @Test
    void constantsCanBeComparedAsStaticFields() {
        assertEquals(0, WFileChooser.APPROVE_OPTION);
        assertEquals(1, WFileChooser.CANCEL_OPTION);
        assertEquals(-1, WFileChooser.ERROR_OPTION);
        assertEquals(2, WFileChooser.CUSTOM_DIALOG);
        assertEquals(1, WFileChooser.DIRECTORIES_ONLY);
        assertEquals(2, WFileChooser.FILES_AND_DIRECTORIES);
    }

    @Test
    void canBeConfiguredWithTheSameGettersAndSettersAsJFileChooser() {
        File dir = new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile();
        WFileChooser chooser = new WFileChooser(dir.getPath());
        assertEquals(dir, chooser.getCurrentDirectory());

        chooser.setDialogTitle("Open");
        chooser.setApproveButtonText("Confirm");
        chooser.setDialogType(WFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(WFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        assertEquals("Open", chooser.getDialogTitle());
        assertEquals("Confirm", chooser.getApproveButtonText());
        assertEquals(WFileChooser.SAVE_DIALOG, chooser.getDialogType());
        assertEquals(WFileChooser.DIRECTORIES_ONLY, chooser.getFileSelectionMode());
        assertTrue(chooser.isMultiSelectionEnabled());
        assertTrue(chooser.isDirectorySelectionEnabled());
        assertFalse(chooser.isFileSelectionEnabled());
        assertThrows(IllegalArgumentException.class, () -> chooser.setDialogType(5));

        File a = new File(dir, "a.txt");
        chooser.setSelectedFiles(new File[] {a});
        assertEquals(a, chooser.getSelectedFile());
        assertArrayEquals(new File[] {a}, chooser.getSelectedFiles());

        WFileChooser byDefault = new WFileChooser();
        assertNull(byDefault.getCurrentDirectory());
    }

    @Test
    void fileTypesCanBeAddedWithFileNameExtensionFilterAndAnonymousClasses() {
        WFileChooser chooser = new WFileChooser();
        WFileNameExtensionFilter text = new WFileNameExtensionFilter("Text", "txt", "log");
        WFileFilter hidden = new WFileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().startsWith(".");
            }

            @Override
            public String getDescription() {
                return "Hidden files";
            }
        };
        chooser.addChoosableFileFilter(text);
        chooser.addChoosableFileFilter(hidden);
        chooser.setFileFilter(text);
        chooser.setAcceptAllFileFilterUsed(false);

        assertArrayEquals(new WFileFilter[] {text, hidden}, chooser.getChoosableFileFilters());
        assertSame(text, chooser.getFileFilter());
        assertFalse(chooser.isAcceptAllFileFilterUsed());
        assertTrue(text.accept(new File("a.LOG")));
        assertTrue(hidden.accept(new File(".gitignore")));
        assertArrayEquals(new String[] {"txt", "log"}, text.getExtensions());
        assertTrue(chooser.removeChoosableFileFilter(hidden));
        chooser.resetChoosableFileFilters();
        assertNull(chooser.getFileFilter());
        assertThrows(IllegalArgumentException.class, () -> new WFileNameExtensionFilter("None"));
    }

    @Test
    void theCallbackFormCanBeWrittenWithALambdaAndAParentNotInAWindowThrowsIllegalArgumentException() {
        WFileChooser chooser = new WFileChooser();
        assertThrows(IllegalArgumentException.class, () -> onUiThreadGet(() -> {
            chooser.showOpenDialog(new WButton("Not loaded"), result -> {
                if (result == WFileChooser.APPROVE_OPTION) {
                    chooser.getSelectedFile();
                }
            });
            return null;
        }));
        assertThrows(IllegalArgumentException.class, () -> onUiThreadGet(() -> chooser.showSaveDialog(new WButton("Not loaded"))));
        assertEquals(WFileChooser.SAVE_DIALOG, chooser.getDialogType());
    }
}
