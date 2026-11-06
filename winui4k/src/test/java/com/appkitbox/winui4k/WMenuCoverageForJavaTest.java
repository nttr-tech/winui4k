package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use from Java the public members of menus and toolbars that had no tests from Java (WCommandBar's WinUI properties and content,
 * WMenuFlyoutItem's keyboardAcceleratorText). The native state is checked in WMenuCoverageTest.
 */
class WMenuCoverageForJavaTest {
    @Test
    void toolBarAppearancePropertiesCanBeSetWithSettersAndReadWithGetters() {
        onUiThread(() -> {
            WCommandBar toolBar = new WCommandBar("Format");
            toolBar.setContent(new WLabel("Format"));
            toolBar.setSticky(true);
            toolBar.setClosedDisplayMode(AppBarClosedDisplayMode.MINIMAL);
            toolBar.setDefaultLabelPosition(CommandBarDefaultLabelPosition.RIGHT);
            toolBar.setOverflowButtonVisibility(CommandBarOverflowButtonVisibility.COLLAPSED);
            toolBar.setDynamicOverflowEnabled(false);
            assertTrue(toolBar.getContent() instanceof WLabel);
            assertTrue(toolBar.isSticky());
            assertFalse(toolBar.isOpen());
            assertSame(AppBarClosedDisplayMode.MINIMAL, toolBar.getClosedDisplayMode());
            assertSame(CommandBarDefaultLabelPosition.RIGHT, toolBar.getDefaultLabelPosition());
            assertSame(CommandBarOverflowButtonVisibility.COLLAPSED, toolBar.getOverflowButtonVisibility());
            assertFalse(toolBar.isDynamicOverflowEnabled());
            toolBar.setContent(null);
            assertNull(toolBar.getContent());
        });
    }

    @Test
    void onlyTheShortcutDisplayOfAMenuItemCanBeReplaced() {
        onUiThread(() -> {
            WMenuFlyoutItem save = new WMenuFlyoutItem("Save");
            save.setAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK));
            save.setKeyboardAcceleratorText("Ctrl+S (overwrite)");
            assertEquals("Ctrl+S (overwrite)", save.getKeyboardAcceleratorText());
            assertEquals(WKeyStroke.getKeyStroke("ctrl S"), save.getAccelerator());
        });
    }
}
