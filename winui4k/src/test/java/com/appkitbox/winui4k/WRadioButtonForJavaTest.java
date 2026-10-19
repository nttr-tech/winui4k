package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WRadioButton the way a developer used to Swing's JRadioButton would write it in Java (ButtonGroup is in WButtonGroupForJavaTest). */
class WRadioButtonForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WRadioButton().getText());
            assertEquals("Option A", new WRadioButton("Option A").getText());
        });
    }

    @Test
    void doClickSelectsAndDeselectsOtherRadioButtonsInTheSameParent() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WRadioButton small = new WRadioButton("Small");
            WRadioButton large = new WRadioButton("Large");
            panel.add(small);
            panel.add(large);
            small.doClick();
            assertTrue(small.isSelected());
            assertFalse(large.isSelected());
            large.doClick();
            assertFalse(small.isSelected());
            assertTrue(large.isSelected());
        });
    }

    @Test
    void doClickOnSelectedRadioButtonKeepsSelectionAndCallsOnlyActionListener() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WRadioButton radio = new WRadioButton("Small");
            panel.add(radio);
            radio.setSelected(true);
            List<String> log = new ArrayList<>();
            radio.addActionListener(e -> log.add("action:" + e.getActionCommand()));
            radio.addItemListener(e -> log.add("item"));
            radio.doClick();
            assertTrue(radio.isSelected());
            assertEquals(Arrays.asList("action:Small"), log);
        });
    }

    @Test
    void itemListenerReceivesSelectedForChosenAndDeselectedForTheOther() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WRadioButton first = new WRadioButton("A");
            WRadioButton second = new WRadioButton("B");
            panel.add(first);
            panel.add(second);
            List<String> log = new ArrayList<>();
            first.addItemListener(e -> log.add("A:" + e.getStateChange()));
            second.addItemListener(e -> log.add("B:" + e.getStateChange()));
            first.setSelected(true);
            second.setSelected(true);
            assertEquals(
                Arrays.asList("A:" + WItemEvent.SELECTED, "A:" + WItemEvent.DESELECTED, "B:" + WItemEvent.SELECTED),
                log);
        });
    }

    @Test
    void doClickOnDisabledRadioButtonDoesNothing() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WRadioButton radio = new WRadioButton("A");
            panel.add(radio);
            radio.setEnabled(false);
            radio.doClick();
            assertFalse(radio.isSelected());
        });
    }
}
