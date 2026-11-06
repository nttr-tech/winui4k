package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use menu bars, separators and WCommandBarFlyout from Java in the same way as building a JMenuBar / JMenu / JToolBar.
 * The native state is checked in WMenuBarPartsTest.
 */
class WMenuBarPartsForJavaTest {
    private final List<String> events = new ArrayList<>();

    @Test
    void addAcceleratorsAndListenersToMenusTheSameWayAsJMenuBar() {
        onUiThread(() -> {
            WMenuBar menuBar = new WMenuBar();
            WMenuBarItem fileMenu = new WMenuBarItem("File");
            menuBar.add(fileMenu);
            WMenuFlyoutItem open = fileMenu.add("Open");
            fileMenu.addSeparator();
            WMenuFlyoutItem exit = fileMenu.add("Exit");
            WKeyStroke ctrlO = WKeyStroke.getKeyStroke(WKeyEvent.VK_O, WInputEvent.CTRL_DOWN_MASK);
            open.setAccelerator(ctrlO);
            open.addActionListener(e -> events.add("open"));
            exit.addActionListener(e -> events.add("exit"));

            open.doClick();
            exit.doClick();

            assertEquals("File", fileMenu.getTitle());
            assertSame(ctrlO, open.getAccelerator());
            assertEquals(3, fileMenu.getItemCount());
            assertNull(fileMenu.getItem(1));
            assertTrue(fileMenu.getMenuComponent(1) instanceof WMenuFlyoutSeparator);
            assertEquals(Arrays.asList("open", "exit"), events);
        });
    }

    @Test
    void theHeadingTitleCanBeChangedWithSetTitle() {
        onUiThread(() -> {
            WMenuBarItem edit = new WMenuBarItem("Edit");
            edit.setTitle("View");
            assertEquals("View", edit.getTitle());
            assertEquals("", new WMenuBarItem().getTitle());
        });
    }

    @Test
    void primaryAndSecondaryCommandsCanBeAddedToWCommandBarFlyout() {
        onUiThread(() -> {
            WCommandBarFlyout flyout = new WCommandBarFlyout();
            flyout.addPrimaryCommand(new WAppBarButton("Copy", Symbol.COPY));
            flyout.addPrimaryCommand(new WAppBarSeparator());
            flyout.addSecondaryCommand(new WAppBarToggleButton("Bold", Symbol.BOLD));
            flyout.addSecondaryCommand(new WAppBarSeparator());
        });
    }
}
