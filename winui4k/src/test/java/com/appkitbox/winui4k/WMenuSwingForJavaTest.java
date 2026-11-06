package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WinUI4K menus and toolbars from Java in the form written with Swing's JMenu / JMenuItem / JCheckBoxMenuItem / JRadioButtonMenuItem / JToolBar
 * (fileMenu.setMnemonic(KeyEvent.VK_F), removing all with getActionListeners, doClick(int), getItemListeners, getSelectedObjects,
 * new JRadioButtonMenuItem(icon, selected), new JToolBar("Name")).
 * Opening, closing and key input are checked in WMenuSwingTest.
 */
class WMenuSwingForJavaTest {
    @Test
    void setTextAndSetMnemonicSetTheMenuHeadingAndAccessKeyLikeJMenu() {
        onUiThread(() -> {
            WMenuBar menuBar = new WMenuBar();
            WMenuBarItem fileMenu = new WMenuBarItem("File");
            fileMenu.setMnemonic(WKeyEvent.VK_F);
            menuBar.add(fileMenu);
            WMenuBarItem editMenu = new WMenuBarItem();
            editMenu.setText("Edit");
            editMenu.setMnemonic('e');
            menuBar.add(editMenu);
            assertEquals(WKeyEvent.VK_F, fileMenu.getMnemonic());
            assertEquals("Edit", editMenu.getText());
            assertEquals("Edit", editMenu.getTitle());
            assertEquals(WKeyEvent.VK_E, editMenu.getMnemonic());
            assertFalse(fileMenu.isPopupMenuVisible());
            fileMenu.setPopupMenuVisible(false);
            fileMenu.doClick();
            assertFalse(fileMenu.isPopupMenuVisible());
        });
    }

    @Test
    void removingAllListenersReturnedByGetActionListeners_doClickCallsNothing() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WMenuFlyoutItem open = new WMenuFlyoutItem("Open");
            WActionListener listener = e -> calls.add("listener:" + e.getActionCommand());
            open.addActionListener(listener);
            open.addActionListener(() -> calls.add("runnable"));
            WActionListener[] listeners = open.getActionListeners();
            assertEquals(2, listeners.length);
            assertSame(listener, listeners[0]);
            open.doClick(100);
            for (WActionListener l : open.getActionListeners()) {
                open.removeActionListener(l);
            }
            open.doClick();
            assertEquals(0, open.getActionListeners().length);
            assertEquals(Arrays.asList("listener:Open", "runnable"), calls);
        });
    }

    @Test
    void allListenersReturnedByGetItemListenersCanBeRemoved() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WToggleMenuFlyoutItem bold = new WToggleMenuFlyoutItem("Bold");
            WItemListener listener = e -> calls.add("bold:" + e.getStateChange());
            bold.addItemListener(listener);
            WRadioMenuFlyoutItem small = new WRadioMenuFlyoutItem("Small");
            small.addItemListener(new WItemListener() {
                @Override
                public void itemStateChanged(WItemEvent e) {
                    calls.add("small:" + e.getStateChange());
                }
            });
            assertSame(listener, bold.getItemListeners()[0]);
            bold.doClick(0);
            for (WItemListener l : bold.getItemListeners()) {
                bold.removeItemListener(l);
            }
            for (WItemListener l : small.getItemListeners()) {
                small.removeItemListener(l);
            }
            bold.setSelected(false);
            small.setSelected(true);
            assertEquals(0, bold.getItemListeners().length);
            assertEquals(0, small.getItemListeners().length);
            assertEquals(Arrays.asList("bold:" + WItemEvent.SELECTED), calls);
        });
    }

    @Test
    void getSelectedObjectsIsAnArrayOfOnlyTheTextWhenSelected_nullWhenNotSelected() {
        onUiThread(() -> {
            WToggleMenuFlyoutItem bold = new WToggleMenuFlyoutItem("Bold", true);
            WRadioMenuFlyoutItem small = new WRadioMenuFlyoutItem("Small");
            assertArrayEquals(new Object[] {"Bold"}, bold.getSelectedObjects());
            assertNull(small.getSelectedObjects());
        });
    }

    @Test
    void canBeCreatedWithAnIconAndSelectionStateLikeJRadioButtonMenuItem() {
        onUiThread(() -> {
            WRadioMenuFlyoutItem zoomIn = new WRadioMenuFlyoutItem(Symbol.ZOOM_IN, true);
            WRadioMenuFlyoutItem zoomOut = new WRadioMenuFlyoutItem(Symbol.ZOOM_OUT, false);
            WButtonGroup group = new WButtonGroup();
            group.add(zoomIn);
            group.add(zoomOut);
            assertSame(Symbol.ZOOM_IN, zoomIn.getIcon());
            assertTrue(zoomIn.isSelected());
            zoomOut.doClick();
            assertTrue(zoomOut.isSelected());
            assertFalse(zoomIn.isSelected());
        });
    }

    @Test
    void toolBarCanBeCreatedWithANameAndOrientationLikeJToolBar() {
        onUiThread(() -> {
            WCommandBar edit = new WCommandBar("Edit");
            WCommandBar format = new WCommandBar("Format", WCommandBar.HORIZONTAL);
            WCommandBar plain = new WCommandBar(WCommandBar.HORIZONTAL);
            assertEquals("Edit", edit.getName());
            assertEquals("Format", format.getName());
            assertEquals(WCommandBar.HORIZONTAL, plain.getOrientation());
            assertThrows(IllegalArgumentException.class, () -> new WCommandBar(WCommandBar.VERTICAL));
        });
    }
}
