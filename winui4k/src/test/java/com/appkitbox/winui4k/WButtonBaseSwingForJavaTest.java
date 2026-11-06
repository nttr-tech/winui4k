package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WinUI4K buttons from Java in the forms written with Swing's AbstractButton (removing all via getActionListeners, setFocusPainted, setHideActionText, doClick(int),
 * getSelectedObjects, new JToggleButton(text, selected)).
 */
class WButtonBaseSwingForJavaTest {
    @Test
    void removingAllListenersReturnedByGetActionListenersMeansDoClickCallsNothing() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WButton button = new WButton("Save");
            WActionListener listener = e -> calls.add("listener");
            button.addActionListener(listener);
            button.addActionListener(() -> calls.add("runnable"));
            WActionListener[] listeners = button.getActionListeners();
            assertEquals(2, listeners.length);
            assertSame(listener, listeners[0]);
            for (WActionListener l : button.getActionListeners()) {
                button.removeActionListener(l);
            }
            button.doClick();
            assertEquals(0, button.getActionListeners().length);
            assertTrue(calls.isEmpty());
        });
    }

    @Test
    void passingPressTimeToDoClickClicksOnlyOnce() {
        onUiThread(() -> {
            int[] count = {0};
            WButton button = new WButton("Save");
            button.addActionListener(e -> count[0]++);
            button.doClick(100);
            assertEquals(1, count[0]);

            WCheckBox box = new WCheckBox("Notify");
            box.doClick(100);
            assertTrue(box.isSelected());
        });
    }

    @Test
    void theValueToggledWithSetFocusPaintedCanBeReadWithIsFocusPainted() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            button.setFocusPainted(false);
            assertFalse(button.isFocusPainted());
            button.setFocusPainted(true);
            assertTrue(button.isFocusPainted());
        });
    }

    @Test
    void setHideActionTextMakesAnIconOnlyButtonThatDoesNotShowTheActionName() {
        onUiThread(() -> {
            WAbstractAction open = new WAbstractAction("Open") {
                @Override
                public void actionPerformed(WActionEvent e) {
                }
            };
            WButton button = new WButton(open);
            assertFalse(button.getHideActionText());
            button.setHideActionText(true);
            assertTrue(button.getHideActionText());
            assertEquals("", button.getText());
            button.setHideActionText(false);
            assertEquals("Open", button.getText());
        });
    }

    @Test
    void getSelectedObjectsIsAnArrayOfOnlyTheTextWhenSelectedAndNullWhenNot() {
        onUiThread(() -> {
            WToggleButton bold = new WToggleButton("Bold");
            assertNull(bold.getSelectedObjects());
            bold.setSelected(true);
            assertArrayEquals(new Object[] {"Bold"}, bold.getSelectedObjects());
        });
    }

    @Test
    void allListenersReturnedByGetItemListenersCanBeRemoved() {
        onUiThread(() -> {
            List<Integer> states = new ArrayList<>();
            WToggleButton bold = new WToggleButton("Bold");
            bold.addItemListener(e -> states.add(e.getStateChange()));
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            toggleSwitch.addItemListener(e -> states.add(e.getStateChange()));
            WToggleSplitButton split = new WToggleSplitButton("Split");
            split.addItemListener(e -> states.add(e.getStateChange()));
            bold.setSelected(true);
            assertEquals(Arrays.asList(WItemEvent.SELECTED), states);
            assertEquals(1, bold.getItemListeners().length);
            assertEquals(1, toggleSwitch.getItemListeners().length);
            assertEquals(1, split.getItemListeners().length);
            for (WItemListener l : bold.getItemListeners()) {
                bold.removeItemListener(l);
            }
            for (WItemListener l : toggleSwitch.getItemListeners()) {
                toggleSwitch.removeItemListener(l);
            }
            for (WItemListener l : split.getItemListeners()) {
                split.removeItemListener(l);
            }
            bold.setSelected(false);
            toggleSwitch.setSelected(true);
            split.setSelected(true);
            assertEquals(Arrays.asList(WItemEvent.SELECTED), states);
        });
    }

    @Test
    void wToggleSwitchCanBeCreatedWithAHeaderAndASelectionState() {
        onUiThread(() -> {
            WToggleSwitch on = new WToggleSwitch("Notifications", true);
            assertEquals("Notifications", on.getHeader());
            assertTrue(on.isSelected());
            assertFalse(new WToggleSwitch("Notifications", false).isSelected());
        });
    }

    @Test
    void listenersReturnedByWSplitButtonGetActionListenersCanBeRemoved() {
        onUiThread(() -> {
            WSplitButton split = new WSplitButton("Split");
            split.addActionListener(e -> { });
            split.addActionListener(() -> { });
            assertEquals(2, split.getActionListeners().length);
            for (WActionListener l : split.getActionListeners()) {
                split.removeActionListener(l);
            }
            assertEquals(0, split.getActionListeners().length);
        });
    }
}
