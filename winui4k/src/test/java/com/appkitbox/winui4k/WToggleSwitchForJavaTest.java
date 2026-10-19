package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WToggleSwitch from Java in a way close to JToggleButton. */
class WToggleSwitchForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutHeader() {
        onUiThread(() -> {
            assertEquals("", new WToggleSwitch().getHeader());
            assertEquals("Airplane mode", new WToggleSwitch("Airplane mode").getHeader());
        });
    }

    @Test
    void setSelectedAndIsSelectedReadAndWriteTheSameStateAsSetOnAndIsOn() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            assertFalse(toggleSwitch.isSelected());
            toggleSwitch.setSelected(true);
            assertTrue(toggleSwitch.isOn());
            toggleSwitch.setOn(false);
            assertFalse(toggleSwitch.isSelected());
        });
    }

    @Test
    void lambdaItemListenerReceivesSelectedWhenOnAndDeselectedWhenOff() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            List<Integer> states = new ArrayList<>();
            toggleSwitch.addItemListener(e -> states.add(e.getStateChange()));
            toggleSwitch.setSelected(true);
            toggleSwitch.setSelected(false);
            assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.DESELECTED), states);
        });
    }

    @Test
    void anonymousClassItemListenerAlsoReceivesEventsWhoseSourceIsTheSwitchItself() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            List<WItemEvent> events = new ArrayList<>();
            toggleSwitch.addItemListener(new WItemListener() {
                @Override
                public void itemStateChanged(WItemEvent e) {
                    events.add(e);
                }
            });
            toggleSwitch.setOn(true);
            assertEquals(1, events.size());
            assertSame(toggleSwitch, events.get(0).getSource());
            assertSame(toggleSwitch, events.get(0).getItemSelectable());
            assertSame(toggleSwitch, events.get(0).getItem());
        });
    }

    @Test
    void listenerRemovedByRemoveItemListenerIsNoLongerCalled() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            List<Integer> states = new ArrayList<>();
            WItemListener listener = e -> states.add(e.getStateChange());
            toggleSwitch.addItemListener(listener);
            toggleSwitch.setSelected(true);
            toggleSwitch.removeItemListener(listener);
            toggleSwitch.setSelected(false);
            assertEquals(Arrays.asList(WItemEvent.SELECTED), states);
        });
    }

    @Test
    void doClickTogglesOnAndOffAndItemListenerReceivesSelectedAndDeselected() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            List<Integer> states = new ArrayList<>();
            toggleSwitch.addItemListener(e -> states.add(e.getStateChange()));
            toggleSwitch.doClick();
            assertTrue(toggleSwitch.isSelected());
            toggleSwitch.doClick();
            assertFalse(toggleSwitch.isSelected());
            assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.DESELECTED), states);
        });
    }

    @Test
    void doClickOnADisabledSwitchDoesNothing() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            toggleSwitch.setEnabled(false);
            toggleSwitch.doClick();
            assertFalse(toggleSwitch.isSelected());
        });
    }

    @Test
    void textWrittenBySetHeaderSetOnContentAndSetOffContentCanBeReadByGetters() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch();
            // By default WinUI puts text matching the display language (e.g. On / Off in English, and the localized words in other languages)
            assertFalse(toggleSwitch.getOnContent().isEmpty());
            assertFalse(toggleSwitch.getOffContent().isEmpty());
            assertNotEquals(toggleSwitch.getOnContent(), toggleSwitch.getOffContent());
            toggleSwitch.setHeader("Airplane mode");
            toggleSwitch.setOnContent("Enabled");
            toggleSwitch.setOffContent("Disabled");
            assertEquals("Airplane mode", toggleSwitch.getHeader());
            assertEquals("Enabled", toggleSwitch.getOnContent());
            assertEquals("Disabled", toggleSwitch.getOffContent());
        });
    }

    @Test
    void setMnemonicAcceptsACharOrAKeyCodeAndGetMnemonicReturnsTheAwtVkValue() {
        onUiThread(() -> {
            WToggleSwitch toggleSwitch = new WToggleSwitch("Notifications");
            assertEquals(0, toggleSwitch.getMnemonic());
            toggleSwitch.setMnemonic('n');
            assertEquals(KeyEvent.VK_N, toggleSwitch.getMnemonic());
            toggleSwitch.setMnemonic(KeyEvent.VK_5);
            assertEquals(KeyEvent.VK_5, toggleSwitch.getMnemonic());
            toggleSwitch.setMnemonic(0);
            assertEquals(0, toggleSwitch.getMnemonic());
        });
    }
}
