package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that handle the state and decorations of WFrame in the same way developers who have used JFrame would write it in Java:
 * {@code frame.setExtendedState(WFrame.MAXIMIZED_BOTH)}, {@code frame.setResizable(false)}, {@code frame.setAlwaysOnTop(true)},
 * {@code frame.setUndecorated(true)}, {@code frame.toFront()}, {@code frame.setIconImage(path)}, {@code frame.setJMenuBar(menuBar)}.
 * Own windows are disposed at the end.
 */
class WFrameStateForJavaTest {
    private static final String TITLE = "winui4k frame state java test";

    private WFrame frame;

    @BeforeEach
    void openTheSharedWindow() {
        UiTestHarness.sharedFrame(); // So that closing its own window does not exit the app
    }

    @AfterEach
    void closeOwnWindow() {
        if (frame != null) {
            onUiThread(frame::dispose);
        }
    }

    @Test
    void setExtendedStateMaximizesAndGetExtendedStateReadsIt() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            frame.setExtendedState(WFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
            assertEquals(WFrame.MAXIMIZED_BOTH, frame.getExtendedState());
            frame.setExtendedState(WFrame.NORMAL);
            assertEquals(WFrame.NORMAL, frame.getExtendedState());
        });
    }

    @Test
    void setResizableSetAlwaysOnTopAndSetUndecoratedHaveTheSameShapeAsJFrame() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            assertTrue(frame.isResizable());
            assertFalse(frame.isAlwaysOnTop());
            assertFalse(frame.isUndecorated());
            frame.setResizable(false);
            frame.setAlwaysOnTop(true);
            frame.setUndecorated(true);
            frame.setVisible(true);
            assertFalse(frame.isResizable());
            assertTrue(frame.isAlwaysOnTop());
            assertTrue(frame.isUndecorated());
            frame.toBack();
            frame.toFront();
        });
    }

    @Test
    void aPngPathCanBePassedToSetIconImage() throws IOException {
        File dir = Files.createTempDirectory("winui4k-frame-icon").toFile();
        File png = new File(dir, "icon.png");
        Files.write(png.toPath(), WindowNatives.solidPng(32, 0xFF20A040));
        try {
            onUiThread(() -> {
                frame = new WFrame(TITLE);
                frame.setIconImage(png.getPath());
                assertEquals(png.getPath(), frame.getIconImage());
                frame.setIconImage(new WImageIcon(png.getPath()));
                assertThrows(IllegalArgumentException.class, () -> frame.setIconImage(new File(dir, "none.ico").getPath()));
            });
        } finally {
            png.delete();
            dir.delete();
        }
    }

    @Test
    void setJMenuBarAddsAMenuBarAndGetJMenuBarReadsIt() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            WMenuBar menuBar = new WMenuBar();
            WMenuBarItem file = new WMenuBarItem("File");
            menuBar.add(file);
            assertNull(frame.getJMenuBar());
            frame.setJMenuBar(menuBar);
            frame.add(new WButton("Content"));
            frame.setVisible(true);
            assertSame(menuBar, frame.getJMenuBar());
            frame.setJMenuBar(null);
            assertNull(frame.getJMenuBar());
        });
    }

    @Test
    void setStateSetAutoRequestFocusAndIsAlwaysOnTopSupportedHaveTheSameShapeAsFrameAndWindow() {
        onUiThread(() -> {
            frame = new WFrame(TITLE);
            assertTrue(frame.isAlwaysOnTopSupported());
            assertTrue(frame.isAutoRequestFocus());
            frame.setAutoRequestFocus(false);
            assertFalse(frame.isAutoRequestFocus());
            assertEquals(WFrame.NORMAL, frame.getState());
            frame.setState(WFrame.ICONIFIED);
            assertEquals(WFrame.ICONIFIED, frame.getState());
            frame.setState(WFrame.NORMAL);
            assertEquals(WFrame.NORMAL, frame.getState());
            frame.setVisible(true);
            assertTrue(frame.isVisible());
            frame.setSystemBackdrop(SystemBackdropType.MICA);
            assertEquals(SystemBackdropType.MICA, frame.getSystemBackdrop());
        });
    }
}
