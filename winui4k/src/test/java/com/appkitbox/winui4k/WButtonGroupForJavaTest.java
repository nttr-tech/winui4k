package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests using WButtonGroup in the form developers who have used Swing's ButtonGroup write in Java. */
class WButtonGroupForJavaTest {
    @Test
    void addingThreeRadioButtonsShowsInGetButtonCountAndGetElements() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WRadioButton small = new WRadioButton("Small");
            WRadioButton medium = new WRadioButton("Medium");
            WRadioButton large = new WRadioButton("Large");
            group.add(small);
            group.add(medium);
            group.add(large);
            assertEquals(3, group.getButtonCount());
            List<String> texts = new ArrayList<>();
            Enumeration<WToggleButton> elements = group.getElements();
            while (elements.hasMoreElements()) {
                texts.add(elements.nextElement().getText());
            }
            assertEquals(Arrays.asList("Small", "Medium", "Large"), texts);
        });
    }

    @Test
    void selectedRadioButtonIsReturnedByGetSelectionAndIdentifiedByGetActionCommand() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WButtonGroup group = new WButtonGroup();
            WRadioButton small = new WRadioButton("Small");
            WRadioButton large = new WRadioButton("Large");
            small.setActionCommand("size-small");
            large.setActionCommand("size-large");
            for (WRadioButton radio : Arrays.asList(small, large)) {
                panel.add(radio);
                group.add(radio);
            }
            assertNull(group.getSelection());
            large.doClick();
            assertSame(large, group.getSelection());
            assertEquals("size-large", group.getSelection().getActionCommand());
            assertTrue(group.isSelected(large));
            assertFalse(group.isSelected(small));
        });
    }

    @Test
    void selectingWithSetSelectedDeselectsOtherButtons() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WCheckBox a = new WCheckBox("A");
            WCheckBox b = new WCheckBox("B");
            group.add(a);
            group.add(b);
            group.setSelected(a, true);
            assertTrue(a.isSelected());
            group.setSelected(b, true);
            assertFalse(a.isSelected());
            assertTrue(b.isSelected());
            assertSame(b, group.getSelection());
        });
    }

    @Test
    void clearSelectionDeselectsAllAndGetSelectionReturnsNull() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WToggleButton a = new WToggleButton("A");
            WToggleButton b = new WToggleButton("B");
            group.add(a);
            group.add(b);
            group.setSelected(b, true);
            group.clearSelection();
            assertFalse(a.isSelected());
            assertFalse(b.isSelected());
            assertNull(group.getSelection());
        });
    }

    @Test
    void removedButtonLeavesCountAndGetSelectionBecomesNullIfItWasSelected() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WCheckBox a = new WCheckBox("A");
            WCheckBox b = new WCheckBox("B");
            group.add(a);
            group.add(b);
            group.setSelected(a, true);
            group.remove(a);
            assertEquals(1, group.getButtonCount());
            assertNull(group.getSelection());
        });
    }

    @Test
    void worksWithUsualSwingStyleItemListenerAndActionListenerOnButtons() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WCheckBox a = new WCheckBox("A");
            WCheckBox b = new WCheckBox("B");
            group.add(a);
            group.add(b);
            List<String> log = new ArrayList<>();
            a.addItemListener(e -> log.add("A:" + (e.getStateChange() == WItemEvent.SELECTED ? "selected" : "deselected")));
            b.addActionListener(e -> log.add("B:action"));
            a.doClick();
            b.doClick();
            assertEquals(Arrays.asList("A:selected", "A:deselected", "B:action"), log);
        });
    }
}
