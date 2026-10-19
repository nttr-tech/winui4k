package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ItemEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WToggleButton the way developers used to Swing's JToggleButton write it in Java.
 * Compiling at all verifies the shape of the API; the test bodies check the resulting behavior of the calls.
 */
class WToggleButtonForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WToggleButton().getText());
            assertEquals("Toggle", new WToggleButton("Toggle").getText());
        });
    }

    @Test
    void setSelectedAndIsSelectedReadAndWriteItAsTwoState() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            assertFalse(button.isSelected());
            button.setSelected(true);
            assertTrue(button.isSelected());
            button.setSelected(false);
            assertFalse(button.isSelected());
        });
    }

    // ---- ItemListener ----

    @Test
    void lambdaItemListenerCanTellSelectedAndDeselectedByGetStateChange() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            List<String> log = new ArrayList<>();
            button.addItemListener(e -> {
                if (e.getStateChange() == WItemEvent.SELECTED) {
                    log.add("selected");
                } else if (e.getStateChange() == WItemEvent.DESELECTED) {
                    log.add("deselected");
                }
            });
            button.setSelected(true);
            button.setSelected(false);
            assertEquals(Arrays.asList("selected", "deselected"), log);
        });
    }

    @Test
    void anonymousClassItemListenerAlsoReceivesEventsWhoseSourceIsTheButtonItself() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            List<WItemEvent> events = new ArrayList<>();
            button.addItemListener(new WItemListener() {
                @Override
                public void itemStateChanged(WItemEvent e) {
                    events.add(e);
                }
            });
            button.setSelected(true);
            assertEquals(1, events.size());
            WItemEvent event = events.get(0);
            assertSame(button, event.getSource());
            assertSame(button, event.getItemSelectable());
            assertSame(button, event.getItem());
            assertEquals(ItemEvent.ITEM_STATE_CHANGED, event.getID());
            assertEquals(ItemEvent.SELECTED, event.getStateChange());
        });
    }

    @Test
    void itemEventConstantsHaveTheSameValuesAsAwtItemEvent() {
        assertEquals(ItemEvent.SELECTED, WItemEvent.SELECTED);
        assertEquals(ItemEvent.DESELECTED, WItemEvent.DESELECTED);
        assertEquals(ItemEvent.ITEM_STATE_CHANGED, WItemEvent.ITEM_STATE_CHANGED);
        assertEquals(ItemEvent.ITEM_FIRST, WItemEvent.ITEM_FIRST);
        assertEquals(ItemEvent.ITEM_LAST, WItemEvent.ITEM_LAST);
    }

    @Test
    void threeStateDeliversIndeterminateWhenIndeterminateAndItIsNotJudgedSelected() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            button.setThreeState(true);
            List<Integer> states = new ArrayList<>();
            button.addItemListener(e -> states.add(e.getStateChange()));
            button.setChecked(true);
            button.setChecked(null);
            assertNull(button.isChecked());
            assertFalse(button.isSelected()); // Indeterminate is false for Swing's isSelected
            button.setChecked(false);
            assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.INDETERMINATE, WItemEvent.DESELECTED), states);
        });
    }

    @Test
    void listenerRemovedByRemoveItemListenerIsNotCalledAndOnlyTheRemainingOnesAre() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            List<String> log = new ArrayList<>();
            WItemListener removed = e -> log.add("removed");
            button.addItemListener(removed);
            button.addItemListener(e -> log.add("kept"));
            button.setSelected(true);
            button.removeItemListener(removed);
            button.setSelected(false);
            assertEquals(Arrays.asList("removed", "kept", "kept"), log);
        });
    }

    // ---- doClick ----

    @Test
    void doClickTogglesTheCheckAndCallsBothActionListenerAndItemListener() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            List<String> actions = new ArrayList<>();
            List<Integer> states = new ArrayList<>();
            button.addActionListener(e -> actions.add(e.getActionCommand()));
            button.addItemListener(e -> states.add(e.getStateChange()));
            button.doClick();
            assertTrue(button.isSelected());
            button.doClick();
            assertFalse(button.isSelected());
            assertEquals(Arrays.asList("Toggle", "Toggle"), actions);
            assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.DESELECTED), states);
        });
    }

    @Test
    void doClickOnADisabledToggleButtonDoesNothing() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Toggle");
            int[] calls = {0};
            button.addActionListener(() -> calls[0]++);
            button.setEnabled(false);
            button.doClick();
            assertFalse(button.isSelected());
            assertEquals(0, calls[0]);
        });
    }

    // ---- Binary compatibility with the legacy form (Consumer) ----

    @Test
    @SuppressWarnings("unchecked")
    void legacyConsumerVersionRemainsAsSyntheticMethodsCallableFromCompiledCallers() throws Exception {
        Method add = WToggleButton.class.getMethod("addItemListener", Consumer.class);
        Method remove = WToggleButton.class.getMethod("removeItemListener", Consumer.class);
        // Not visible from Java source (javac ignores synthetic methods), but already compiled calls can link to it
        assertTrue(add.isSynthetic());
        assertTrue(remove.isSynthetic());
        WToggleButton[] holder = new WToggleButton[1];
        List<Boolean> received = new ArrayList<>();
        Consumer<Boolean> consumer = received::add;
        onUiThread(() -> {
            try {
                holder[0] = new WToggleButton("Toggle");
                add.invoke(holder[0], consumer);
                holder[0].setSelected(true);
                remove.invoke(holder[0], consumer);
                holder[0].setSelected(false);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
        assertEquals(Arrays.asList(Boolean.TRUE), received);
    }
}
