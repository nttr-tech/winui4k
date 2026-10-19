package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for using WToggleSplitButton from Java in a way similar to JToggleButton.
 * ToggleSplitButton does not fire IsCheckedChanged for changes before Loaded, so tests that check the ItemListener place
 * the button in the shared window and wait for Loaded before changing the state.
 */
class WToggleSplitButtonForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WToggleSplitButton().getText());
            assertEquals("Bold", new WToggleSplitButton("Bold").getText());
        });
    }

    @Test
    void setSelectedAndIsSelectedReadAndWriteTheSameStateAsSetCheckedAndIsChecked() {
        onUiThread(() -> {
            WToggleSplitButton button = new WToggleSplitButton("Bold");
            assertFalse(button.isSelected());
            button.setSelected(true);
            assertTrue(button.isChecked());
            button.setChecked(false);
            assertFalse(button.isSelected());
        });
    }

    @Test
    void lambdaItemListenerReceivesSelectedOnCheckAndDeselectedOnUncheck() {
        WToggleSplitButton button = UiTestHarness.onUiThreadGet(() -> new WToggleSplitButton("Bold"));
        List<Integer> states = new ArrayList<>();
        onUiThread(() -> button.addItemListener(e -> states.add(e.getStateChange())));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setSelected(true);
                button.setSelected(false);
            });
        } finally {
            UiTestHarness.detach(button);
        }
        assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.DESELECTED), states);
    }

    @Test
    void anonymousClassItemListenerReceivesEventsWhoseSourceIsTheButtonAndCanBeRemoved() {
        WToggleSplitButton button = UiTestHarness.onUiThreadGet(() -> new WToggleSplitButton("Bold"));
        UiTestHarness.attachAndAwaitLoaded(button);
        List<WItemEvent> events = new ArrayList<>();
        try {
            onUiThread(() -> {
                WItemListener listener = new WItemListener() {
                    @Override
                    public void itemStateChanged(WItemEvent e) {
                        events.add(e);
                    }
                };
                button.addItemListener(listener);
                button.setSelected(true);
                button.removeItemListener(listener);
                button.setSelected(false);
            });
        } finally {
            UiTestHarness.detach(button);
        }
        assertEquals(1, events.size());
        assertSame(button, events.get(0).getSource());
        assertEquals(WItemEvent.SELECTED, events.get(0).getStateChange());
    }

    @Test
    void doClickTogglesTheSelectionAndCallsTheActionListenerOnceLikeJToggleButton() {
        WToggleSplitButton button = UiTestHarness.onUiThreadGet(() -> new WToggleSplitButton("Bold"));
        List<WActionEvent> events = new ArrayList<>();
        onUiThread(() -> button.addActionListener(e -> events.add(e)));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                assertFalse(button.isSelected());
                button.doClick();
                assertTrue(button.isSelected());
                assertEquals(1, events.size());
                assertSame(button, events.get(0).getSource());
                assertEquals("Bold", events.get(0).getActionCommand());
                button.doClick();
                assertFalse(button.isSelected());
                assertEquals(2, events.size());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void doClickInWindowDeliversOnceEachToItemListenerAndActionListener() {
        WToggleSplitButton button = UiTestHarness.onUiThreadGet(() -> new WToggleSplitButton("Bold"));
        List<String> order = new ArrayList<>();
        onUiThread(() -> {
            button.addItemListener(e -> order.add("item" + e.getStateChange()));
            button.addActionListener(() -> order.add("action"));
        });
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(button::doClick);
        } finally {
            UiTestHarness.detach(button);
        }
        assertEquals(Arrays.asList("item" + WItemEvent.SELECTED, "action"), order);
    }

    @Test
    void doClickOnDisabledButtonChangesNeitherSelectionNorActionListener() {
        WToggleSplitButton button = UiTestHarness.onUiThreadGet(() -> new WToggleSplitButton("Bold"));
        int[] count = {0};
        onUiThread(() -> button.addActionListener(() -> count[0]++));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setEnabled(false);
                button.doClick();
                assertFalse(button.isSelected());
                assertEquals(0, count[0]);
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void doClickBeforeBeingPlacedInWindowDoesNothing() {
        onUiThread(() -> {
            WToggleSplitButton button = new WToggleSplitButton("Bold");
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.doClick();
            assertFalse(button.isSelected());
            assertEquals(0, count[0]);
        });
    }

    @Test
    void setIconSetMnemonicAndSetActionCommandCanBeUsedTheSameWayAsWSplitButton() {
        onUiThread(() -> {
            WToggleSplitButton button = new WToggleSplitButton("Bold");
            button.setIcon(Symbol.BOLD);
            button.setMnemonic('b');
            button.setActionCommand("bold");
            assertSame(Symbol.BOLD, button.getIcon());
            assertEquals('B', button.getMnemonic());
            assertEquals("bold", button.getActionCommand());
        });
    }
}
