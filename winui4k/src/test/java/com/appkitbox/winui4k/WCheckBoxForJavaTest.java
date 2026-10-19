package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WCheckBox the way developers used to Swing's JCheckBox write it in Java. */
class WCheckBoxForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WCheckBox().getText());
            assertEquals("I agree", new WCheckBox("I agree").getText());
        });
    }

    @Test
    void lambdaItemListenerCanEnableAndDisableOtherComponentsByTheSelection() {
        onUiThread(() -> {
            WCheckBox agree = new WCheckBox("I agree");
            WButton submit = new WButton("Send");
            submit.setEnabled(false);
            agree.addItemListener(e -> submit.setEnabled(e.getStateChange() == WItemEvent.SELECTED));
            agree.setSelected(true);
            assertTrue(submit.isEnabled());
            agree.setSelected(false);
            assertFalse(submit.isEnabled());
        });
    }

    @Test
    void doClickTogglesTheCheckAndActionListenerSourceAndActionCommandAreTheCheckBox() {
        onUiThread(() -> {
            WCheckBox checkBox = new WCheckBox("Receive notifications");
            List<WActionEvent> actions = new ArrayList<>();
            List<WItemEvent> items = new ArrayList<>();
            checkBox.addActionListener(actions::add);
            checkBox.addItemListener(items::add);
            checkBox.doClick();
            assertTrue(checkBox.isSelected());
            assertEquals(1, actions.size());
            assertSame(checkBox, actions.get(0).getSource());
            assertEquals("Receive notifications", actions.get(0).getActionCommand());
            assertEquals(1, items.size());
            assertSame(checkBox, items.get(0).getItemSelectable());
            assertEquals(WItemEvent.SELECTED, items.get(0).getStateChange());
        });
    }

    @Test
    void doClickOnADisabledCheckBoxDoesNothing() {
        onUiThread(() -> {
            WCheckBox checkBox = new WCheckBox("I agree");
            List<Integer> states = new ArrayList<>();
            checkBox.addItemListener(e -> states.add(e.getStateChange()));
            checkBox.setEnabled(false);
            checkBox.doClick();
            assertFalse(checkBox.isSelected());
            assertEquals(0, states.size());
        });
    }

    @Test
    void threeStateCheckBoxAdvancesThroughSelectedIndeterminateDeselectedOnDoClick() {
        onUiThread(() -> {
            WCheckBox checkBox = new WCheckBox("Three-state");
            checkBox.setThreeState(true);
            List<Integer> states = new ArrayList<>();
            checkBox.addItemListener(e -> states.add(e.getStateChange()));
            checkBox.doClick();
            checkBox.doClick();
            assertNull(checkBox.isChecked());
            assertFalse(checkBox.isSelected()); // Indeterminate is false for Swing's isSelected
            checkBox.doClick();
            assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.INDETERMINATE, WItemEvent.DESELECTED), states);
        });
    }
}
