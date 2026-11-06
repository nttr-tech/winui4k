package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use toolbars from Java in the same way as JToolBar (add(Action), add(Component), addSeparator, getComponentCount,
 * getComponentAtIndex, remove, setFloatable, setOrientation).
 * Agreement with the native sequence is checked in WCommandBarToolBarTest.
 */
class WCommandBarToolBarForJavaTest {
    @Test
    void actionsComponentsAndSeparatorsCanBeAddedTheSameWayAsJToolBar() {
        onUiThread(() -> {
            WCommandBar toolBar = new WCommandBar();
            int[] performed = {0};
            WAction save = new WAbstractAction("Save", null) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    performed[0]++;
                }
            };
            WAppBarButton saveButton = toolBar.add(save);
            toolBar.addSeparator();
            WTextField search = new WTextField();
            WComponent returned = toolBar.add(search);

            saveButton.doClick();
            assertEquals("Save", saveButton.getLabel());
            assertEquals(1, performed[0]);
            assertSame(search, returned);
            assertEquals(3, toolBar.getComponentCount());
            assertSame(saveButton, toolBar.getComponentAtIndex(0));
            assertSame(search, toolBar.getComponentAtIndex(2));
            assertEquals(2, toolBar.getComponentIndex(search));
            assertNull(toolBar.getComponentAtIndex(3));
            assertEquals(-1, toolBar.getComponentIndex(new WAppBarButton()));

            toolBar.remove(search);
            assertEquals(2, toolBar.getComponentCount());
            toolBar.removeAll();
            assertEquals(0, toolBar.getComponentCount());
        });
    }

    @Test
    void floatableIsAlwaysFalseRolloverIsAlwaysTrueAndVerticalCannotBeSet() {
        onUiThread(() -> {
            WCommandBar toolBar = new WCommandBar();
            toolBar.setFloatable(true);
            toolBar.setRollover(false);
            assertFalse(toolBar.isFloatable());
            assertTrue(toolBar.isRollover());
            toolBar.setOrientation(WCommandBar.HORIZONTAL);
            assertEquals(WCommandBar.HORIZONTAL, toolBar.getOrientation());
            assertThrows(IllegalArgumentException.class, () -> toolBar.setOrientation(WCommandBar.VERTICAL));
        });
    }

    @Test
    void passingANonICommandBarElementComponentToAddPrimaryCommandThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WCommandBar toolBar = new WCommandBar();
            assertThrows(IllegalArgumentException.class, () -> toolBar.addPrimaryCommand(new WTextField()));
            WCommandBarFlyout flyout = new WCommandBarFlyout();
            assertThrows(IllegalArgumentException.class, () -> flyout.addPrimaryCommand(new WTextField()));
        });
    }
}
