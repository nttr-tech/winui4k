package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for using the toolbar toggle button from Java the same way as JToggleButton (setSelected / isSelected / doClick /
 * addItemListener). Reading and writing the label and icon are in WAppBarButtonForJavaTest.
 */
class WAppBarToggleButtonForJavaTest {
    @Test
    void setSelectedAndIsSelectedReadAndWriteTheSelectionState() {
        onUiThread(() -> {
            WAppBarToggleButton bold = new WAppBarToggleButton("Bold", Symbol.BOLD);
            assertFalse(bold.isSelected());
            bold.setSelected(true);
            assertTrue(bold.isSelected());
            bold.setSelected(false);
            assertFalse(bold.isSelected());
        });
    }

    @Test
    void doClickTogglesTheSelectionAndCallsTheActionListener() {
        onUiThread(() -> {
            WAppBarToggleButton bold = new WAppBarToggleButton("Bold", Symbol.BOLD);
            List<String> commands = new ArrayList<>();
            bold.addActionListener(e -> commands.add(e.getActionCommand()));
            bold.doClick();
            assertTrue(bold.isSelected());
            bold.doClick();
            assertFalse(bold.isSelected());
            assertEquals(Arrays.asList("Bold", "Bold"), commands);
        });
    }

    @Test
    void doClickInWindowDeliversSelectedAndDeselectedToItemListenerInOrder() {
        WAppBarToggleButton bold = UiTestHarness.onUiThreadGet(() -> new WAppBarToggleButton("Bold"));
        List<Integer> states = new ArrayList<>();
        onUiThread(() -> bold.addItemListener(e -> states.add(e.getStateChange())));
        UiTestHarness.attachAndAwaitLoaded(bold);
        try {
            onUiThread(() -> {
                bold.doClick();
                bold.doClick();
            });
        } finally {
            UiTestHarness.detach(bold);
        }
        assertEquals(Arrays.asList(WItemEvent.SELECTED, WItemEvent.DESELECTED), states);
    }

    @Test
    void labelPositionAndCompactCanBeHandledWithGettersAndSetters() {
        onUiThread(() -> {
            WAppBarToggleButton bold = new WAppBarToggleButton("Bold");
            assertEquals(CommandBarLabelPosition.DEFAULT, bold.getLabelPosition());
            assertFalse(bold.isCompact());
            bold.setLabelPosition(CommandBarLabelPosition.COLLAPSED);
            bold.setCompact(true);
            assertEquals(CommandBarLabelPosition.COLLAPSED, bold.getLabelPosition());
            assertTrue(bold.isCompact());
        });
    }

    @Test
    void canBeCreatedWithOnlyAnIconAnIconAndSelectionOrALabelAnIconAndSelectionLikeJToggleButton() {
        onUiThread(() -> {
            WAppBarToggleButton iconOnly = new WAppBarToggleButton(Symbol.BOLD);
            WAppBarToggleButton iconSelected = new WAppBarToggleButton(Symbol.ITALIC, true);
            WAppBarToggleButton full = new WAppBarToggleButton("Underline", Symbol.UNDERLINE, true);
            WAppBarToggleButton fullOff = new WAppBarToggleButton("Underline", Symbol.UNDERLINE, false);
            org.junit.jupiter.api.Assertions.assertEquals("", iconOnly.getLabel());
            org.junit.jupiter.api.Assertions.assertSame(Symbol.BOLD, iconOnly.getIcon());
            org.junit.jupiter.api.Assertions.assertFalse(iconOnly.isSelected());
            org.junit.jupiter.api.Assertions.assertSame(Symbol.ITALIC, iconSelected.getIcon());
            org.junit.jupiter.api.Assertions.assertTrue(iconSelected.isSelected());
            org.junit.jupiter.api.Assertions.assertEquals("Underline", full.getLabel());
            org.junit.jupiter.api.Assertions.assertTrue(full.isSelected());
            org.junit.jupiter.api.Assertions.assertFalse(fullOff.isSelected());
        });
    }
}
