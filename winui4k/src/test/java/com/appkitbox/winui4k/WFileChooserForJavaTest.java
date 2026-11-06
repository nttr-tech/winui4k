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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WFileChooser in the same way developers who have used JFileChooser would write it in Java
 * (new WFileChooser(dir), comparisons with constants, getters / setters, anonymous FileFilter classes, callback lambdas).
 * Only one test, with an ActionListener, opens an OS dialog, and it closes it right away.
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

    @Test
    void remainingConstantsAcceptAndChangeToParentDirectoryCanBeCalledLikeJFileChooser() {
        assertEquals(0, WFileChooser.OPEN_DIALOG);
        assertEquals(1, WFileChooser.SAVE_DIALOG);
        assertEquals(0, WFileChooser.FILES_ONLY);
        assertEquals("ApproveSelection", WFileChooser.APPROVE_SELECTION);
        assertEquals("CancelSelection", WFileChooser.CANCEL_SELECTION);

        File dir = new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile();
        File child = new File(dir, "winui4k-chooser-java");
        assertTrue(child.isDirectory() || child.mkdirs());
        WFileChooser chooser = new WFileChooser();
        chooser.setCurrentDirectory(child);
        chooser.setSelectedFile(new File(child, "a.txt"));
        chooser.setFileFilter(new WFileNameExtensionFilter("Text", "txt"));
        assertTrue(chooser.accept(new File("b.txt")));
        assertFalse(chooser.accept(new File("b.csv")));
        assertTrue(chooser.getAcceptAllFileFilter().accept(new File("b.csv")));

        chooser.changeToParentDirectory();
        assertEquals(child.getParentFile(), chooser.getCurrentDirectory());
        assertNull(chooser.getSelectedFile());
    }

    @Test
    void actionListenersCanBeAddedAndRemovedAsAnonymousClassesAndLambdas_closingTheDialogDeliversCancelSelection() throws InterruptedException {
        WButton button = onUiThreadGet(() -> new WButton("Open"));
        UiTestHarness.attachAndAwaitLoaded(button);
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        WFileChooser chooser = new WFileChooser();
        String title = "WFileChooserForJavaTest dialog";
        chooser.setDialogTitle(title);
        WActionListener counter = new WActionListener() {
            @Override
            public void actionPerformed(WActionEvent e) {
                events.add("anonymous:" + e.getActionCommand());
            }
        };
        WActionListener removed = e -> events.add("removed");
        chooser.addActionListener(counter);
        chooser.addActionListener(removed);
        chooser.addActionListener(e -> {
            if (WFileChooser.CANCEL_SELECTION.equals(e.getActionCommand())) {
                events.add("cancel");
            }
        });
        chooser.removeActionListener(removed);
        assertEquals(2, chooser.getActionListeners().length);
        try {
            WinUiUtilities.invokeLater(() -> events.add("r:" + chooser.showOpenDialog(button)));
            long deadline = System.currentTimeMillis() + 10_000;
            boolean closed = false;
            while (!closed && System.currentTimeMillis() < deadline) {
                try {
                    TopLevelWindows.closeByTitle(title);
                    closed = true;
                } catch (IllegalStateException notYet) {
                    Thread.sleep(100);
                }
            }
            assertTrue(closed);
            List<String> expected = Arrays.asList("anonymous:CancelSelection", "cancel", "r:" + WFileChooser.CANCEL_OPTION);
            while (!snapshot(events).equals(expected) && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(expected, snapshot(events));
        } finally {
            UiTestHarness.detach(button);
        }
    }

    private static List<String> snapshot(List<String> events) {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }
}
