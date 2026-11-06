package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Tests that use popup menus from Java in the same way as Swing's JPopupMenu / JMenu / JComponent (setComponentPopupMenu, show(invoker, x, y),
 * setVisible, getInvoker, addPopupMenuListener, addMenuListener). The open/close order is checked in WFlyoutPopupMenuTest.
 */
class WFlyoutPopupMenuForJavaTest {
    private final List<String> events = new ArrayList<>();

    @Test
    void setComponentPopupMenuAndGetComponentPopupMenu() {
        onUiThread(() -> {
            WLabel label = new WLabel("Right click");
            WMenuFlyout popup = new WMenuFlyout();
            popup.add("Copy");
            label.setComponentPopupMenu(popup);
            assertSame(popup, label.getComponentPopupMenu());
            assertSame(popup, label.getContextFlyout());
            label.setContextFlyout(new WFlyout());
            assertNull(label.getComponentPopupMenu());
            label.setComponentPopupMenu(null);
            assertNull(label.getContextFlyout());
        });
    }

    @Test
    void popupMenuListenersCanBeAddedAndRemovedAsAnonymousClassesAndAdapters() {
        onUiThread(() -> {
            WMenuFlyout popup = new WMenuFlyout();
            WPopupMenuListener listener = new WPopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                    events.add("visible");
                }

                @Override
                public void popupMenuWillBecomeInvisible(WPopupMenuEvent e) {
                    events.add("invisible");
                }

                @Override
                public void popupMenuCanceled(WPopupMenuEvent e) {
                    events.add("canceled");
                }
            };
            WPopupMenuAdapter adapter = new WPopupMenuAdapter() {
                @Override
                public void popupMenuCanceled(WPopupMenuEvent e) {
                    events.add("adapter");
                }
            };
            popup.addPopupMenuListener(listener);
            popup.addPopupMenuListener(adapter);
            assertEquals(Arrays.asList(listener, adapter), Arrays.asList(popup.getPopupMenuListeners()));
            popup.removePopupMenuListener(listener);
            assertEquals(Arrays.<WPopupMenuListener>asList(adapter), Arrays.asList(popup.getPopupMenuListeners()));
            assertFalse(popup.isVisible());
            assertNull(popup.getInvoker());
            assertThrows(IllegalStateException.class, () -> popup.setVisible(true));
        });
    }

    @Test
    void openWithShowAndCloseWithSetVisible() throws Exception {
        AtomicReference<WPanel> panel = new AtomicReference<>();
        AtomicReference<WButton> button = new AtomicReference<>();
        AtomicReference<WMenuFlyout> popup = new AtomicReference<>();
        onUiThread(() -> {
            panel.set(new WPanel());
            button.set(new WButton("Anchor"));
            panel.get().add(button.get());
            WMenuFlyout menu = new WMenuFlyout();
            menu.add("Undo");
            menu.addPopupMenuListener(new WPopupMenuAdapter() {
                @Override
                public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                    events.add("visible:" + (e.getSource() == menu));
                }

                @Override
                public void popupMenuWillBecomeInvisible(WPopupMenuEvent e) {
                    events.add("invisible:" + (e.getSource() == menu));
                }
            });
            popup.set(menu);
        });
        UiTestHarness.attachAndAwaitLoaded(panel.get());
        try {
            onUiThread(() -> popup.get().show(button.get(), 4, 8));
            awaitOpen(popup.get(), true);
            onUiThread(() -> {
                assertTrue(popup.get().isVisible());
                assertSame(button.get(), popup.get().getInvoker());
                popup.get().setVisible(false);
            });
            awaitOpen(popup.get(), false);
            onUiThread(() -> assertEquals(Arrays.asList("visible:true", "invisible:true"), events));
        } finally {
            UiTestHarness.detach(panel.get());
        }
    }

    @Test
    void menuListenersCanBeAddedAndRemovedAsAnonymousClassesAndAdapters() {
        onUiThread(() -> {
            WMenuBarItem file = new WMenuBarItem("File");
            WMenuListener listener = new WMenuListener() {
                @Override
                public void menuSelected(WMenuEvent e) {
                    events.add("selected");
                }

                @Override
                public void menuDeselected(WMenuEvent e) {
                    events.add("deselected");
                }

                @Override
                public void menuCanceled(WMenuEvent e) {
                    events.add("canceled");
                }
            };
            WMenuAdapter adapter = new WMenuAdapter() {
            };
            file.addMenuListener(listener);
            file.addMenuListener(adapter);
            assertEquals(Arrays.asList(listener, adapter), Arrays.asList(file.getMenuListeners()));
            file.removeMenuListener(listener);
            assertEquals(Arrays.<WMenuListener>asList(adapter), Arrays.asList(file.getMenuListeners()));
            assertSame(file, new WMenuEvent(file).getSource());
        });
    }

    private static void awaitOpen(WFlyoutBase flyout, boolean open) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            boolean[] current = new boolean[1];
            onUiThread(() -> current[0] = flyout.isOpen());
            if (current[0] == open) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("isOpen did not become " + open);
    }
}
