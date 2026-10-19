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
}
