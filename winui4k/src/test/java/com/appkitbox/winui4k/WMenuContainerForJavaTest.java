package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that build menu bars and menus from Java in the same way as Swing's JMenuBar / JMenu / JPopupMenu (return values of add, add(String), add(Action),
 * addSeparator, insert, remove, getItem, getItemCount). The native Items are checked in WMenuContainerTest.
 */
class WMenuContainerForJavaTest {
    private final List<String> events = new ArrayList<>();

    @Test
    void buildMenusTheSameWayAsJMenuBarAndJMenu() {
        onUiThread(() -> {
            WMenuBar menuBar = new WMenuBar();
            WMenuBarItem fileMenu = menuBar.add(new WMenuBarItem("File"));
            WMenuFlyoutItem open = fileMenu.add("Open");
            WMenuFlyoutItem save = fileMenu.add(new WMenuFlyoutItem("Save"));
            fileMenu.addSeparator();
            WMenuFlyoutSubItem recent = fileMenu.add(new WMenuFlyoutSubItem("Recent files"));
            recent.add("a.txt");
            WMenuFlyoutItem exit = fileMenu.add(new WAbstractAction("Exit") {
                @Override
                public void actionPerformed(WActionEvent e) {
                    events.add("exit:" + e.getActionCommand());
                }
            });

            assertEquals(1, menuBar.getMenuCount());
            assertSame(fileMenu, menuBar.getMenu(0));
            assertEquals(0, menuBar.getComponentIndex(fileMenu));
            assertEquals(5, fileMenu.getItemCount());
            assertEquals(5, fileMenu.getMenuComponentCount());
            assertSame(open, fileMenu.getItem(0));
            assertSame(save, fileMenu.getItem(1));
            assertNull(fileMenu.getItem(2)); // A separator is null (as in Swing)
            assertTrue(fileMenu.getMenuComponent(2) instanceof WMenuFlyoutSeparator);
            assertSame(recent, fileMenu.getItem(3));
            assertEquals("a.txt", ((WMenuFlyoutItem) recent.getItem(0)).getText());
            assertEquals("Exit", exit.getText());
            assertEquals(5, fileMenu.getMenuComponents().length);

            exit.doClick();
            assertEquals(Collections.singletonList("exit:Exit"), events);
        });
    }

    @Test
    void insertAndRemoveUseTheSamePositionsAndExceptionsAsSwing() {
        onUiThread(() -> {
            WMenuBarItem edit = new WMenuBarItem("Edit");
            edit.add("Paste");
            WMenuFlyoutItem cut = edit.insert(new WMenuFlyoutItem("Cut"), 0);
            edit.insert("Copy", 1);
            edit.insertSeparator(3);
            edit.insert("Select all", 100); // A position larger than the number of items means the end
            assertEquals(5, edit.getItemCount());
            assertSame(cut, edit.getItem(0));
            assertEquals("Copy", ((WMenuFlyoutItem) edit.getItem(1)).getText());
            assertNull(edit.getItem(3));
            assertEquals("Select all", ((WMenuFlyoutItem) edit.getItem(4)).getText());

            edit.remove(cut);
            edit.remove(2); // The separator
            assertEquals(3, edit.getItemCount());
            assertEquals("Copy", ((WMenuFlyoutItem) edit.getItem(0)).getText());

            assertThrows(IllegalArgumentException.class, () -> edit.insert("x", -1));
            assertThrows(IllegalArgumentException.class, () -> edit.insertSeparator(-1));
            assertThrows(IllegalArgumentException.class, () -> edit.remove(-1));
            assertThrows(IllegalArgumentException.class, () -> edit.remove(3));
            assertThrows(IllegalArgumentException.class, () -> edit.getItem(-1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> edit.getItem(3));

            edit.removeAll();
            assertEquals(0, edit.getItemCount());
        });
    }

    @Test
    void buildPopupMenusTheSameWayAsJPopupMenu() {
        onUiThread(() -> {
            WMenuFlyout popup = new WMenuFlyout();
            WMenuFlyoutItem undo = popup.add("Undo");
            popup.addSeparator();
            WToggleMenuFlyoutItem wrap = popup.add(new WToggleMenuFlyoutItem("Wrap", true));
            assertEquals(3, popup.getComponentCount());
            assertSame(undo, popup.getComponent(0));
            assertSame(wrap, popup.getComponent(2));
            assertEquals(2, popup.getComponentIndex(wrap));
            assertEquals(3, popup.getComponents().length);

            // Adding to another menu moves it from the original menu (same as Container.add)
            WMenuBarItem view = new WMenuBarItem("View");
            view.add(wrap);
            assertEquals(2, popup.getComponentCount());
            assertEquals(-1, popup.getComponentIndex(wrap));
            assertSame(wrap, view.getItem(0));

            popup.removeAll();
            assertEquals(0, popup.getComponentCount());
        });
    }

    @Test
    void jMenuBarRemoveAndRemoveAll() {
        onUiThread(() -> {
            WMenuBar menuBar = new WMenuBar();
            WMenuBarItem file = menuBar.add(new WMenuBarItem("File"));
            WMenuBarItem edit = menuBar.add(new WMenuBarItem("Edit"));
            menuBar.add(new WMenuBarItem("View"));
            menuBar.remove(edit);
            assertEquals(2, menuBar.getMenuCount());
            assertEquals("View", menuBar.getMenu(1).getTitle());
            menuBar.remove(0);
            assertEquals(-1, menuBar.getComponentIndex(file));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> menuBar.getMenu(1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> menuBar.remove(1));
            menuBar.removeAll();
            assertEquals(0, menuBar.getMenuCount());
        });
    }
}
