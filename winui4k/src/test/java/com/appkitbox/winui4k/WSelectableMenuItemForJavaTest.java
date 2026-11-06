package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use check and radio menu items from Java in the same way as Swing's JCheckBoxMenuItem / JRadioButtonMenuItem and ButtonGroup
 * (constructors, getState / setState, isSelected, ItemListener, setAction, ButtonGroup.getSelection).
 * Details of the behavior are checked in WSelectableMenuItemTest.
 */
class WSelectableMenuItemForJavaTest {
    private final List<String> events = new ArrayList<>();

    @Test
    void getStateSetStateAndItemListenerCanBeUsedLikeJCheckBoxMenuItem() {
        onUiThread(() -> {
            WToggleMenuFlyoutItem wrap = new WToggleMenuFlyoutItem("Wrap", true);
            assertTrue(wrap.getState());
            wrap.addItemListener(new WItemListener() {
                @Override
                public void itemStateChanged(WItemEvent e) {
                    events.add((e.getStateChange() == WItemEvent.SELECTED ? "selected" : "deselected") + ":" + (e.getItem() == wrap));
                }
            });
            wrap.addActionListener(e -> events.add("action:" + e.getActionCommand()));
            wrap.doClick();
            assertFalse(wrap.getState());
            assertFalse(wrap.isSelected());
            wrap.setState(true);
            assertEquals(Arrays.asList("deselected:true", "action:Wrap", "selected:true"), events);
        });
    }

    @Test
    void canBeCreatedWithTheSameConstructorsAsJCheckBoxMenuItem() {
        onUiThread(() -> {
            WToggleMenuFlyoutItem icon = new WToggleMenuFlyoutItem(Symbol.BOLD);
            assertSame(Symbol.BOLD, icon.getIcon());
            WToggleMenuFlyoutItem all = new WToggleMenuFlyoutItem("Bold", Symbol.BOLD, true);
            assertTrue(all.isSelected());
            WToggleMenuFlyoutItem plain = new WToggleMenuFlyoutItem("Italic", null);
            assertNull(plain.getIcon());
        });
    }

    @Test
    void jRadioButtonMenuItemsInAButtonGroupCanBeIdentifiedWithGetSelectionAndGetActionCommand() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WRadioMenuFlyoutItem small = new WRadioMenuFlyoutItem("Small", true);
            WRadioMenuFlyoutItem large = new WRadioMenuFlyoutItem("Large");
            small.setActionCommand("size-small");
            large.setActionCommand("size-large");
            group.add(small);
            group.add(large);
            assertSame(small, group.getSelection());
            large.doClick();
            assertFalse(small.isSelected());
            assertEquals("size-large", group.getSelection().getActionCommand());
            assertTrue(group.isSelected(large));

            List<String> texts = new ArrayList<>();
            Enumeration<WSelectable> elements = group.getElements();
            while (elements.hasMoreElements()) {
                texts.add(elements.nextElement().getText());
            }
            assertEquals(Arrays.asList("Small", "Large"), texts);
        });
    }

    @Test
    void jRadioButtonMenuItemsNotInAButtonGroupAreIndependent() {
        onUiThread(() -> {
            WRadioMenuFlyoutItem a = new WRadioMenuFlyoutItem("A");
            WRadioMenuFlyoutItem b = new WRadioMenuFlyoutItem("B");
            a.setSelected(true);
            b.setSelected(true);
            assertTrue(a.isSelected());
            assertTrue(b.isSelected());
            assertEquals("", a.getGroupName());
        });
    }

    @Test
    void canBeCreatedWithTheSameConstructorsAsJRadioButtonMenuItem() {
        onUiThread(() -> {
            WRadioMenuFlyoutItem icon = new WRadioMenuFlyoutItem(Symbol.FONT_SIZE);
            assertSame(Symbol.FONT_SIZE, icon.getIcon());
            WRadioMenuFlyoutItem textIcon = new WRadioMenuFlyoutItem("Medium", Symbol.FONT_SIZE);
            assertEquals("Medium", textIcon.getText());
            WRadioMenuFlyoutItem all = new WRadioMenuFlyoutItem("Large", Symbol.FONT_SIZE, true);
            assertTrue(all.isSelected());
            WRadioMenuFlyoutItem named = new WRadioMenuFlyoutItem("Small", "Size");
            assertEquals("Size", named.getGroupName());
            // A null icon is ambiguous with the (String, String) form, so pass it with a type
            WRadioMenuFlyoutItem noIcon = new WRadioMenuFlyoutItem("Extra large", (WIcon) null);
            assertNull(noIcon.getIcon());
        });
    }

    @Test
    void setActionSelectedKeyIsMappedBothWaysLikeJCheckBoxMenuItem() {
        onUiThread(() -> {
            WAction wrap = new WAbstractAction("Wrap") {
                @Override
                public void actionPerformed(WActionEvent e) {
                    events.add("performed:" + getValue(SELECTED_KEY));
                }
            };
            wrap.putValue(WAction.SELECTED_KEY, Boolean.FALSE);
            WToggleMenuFlyoutItem item = new WToggleMenuFlyoutItem(wrap);
            item.doClick();
            assertEquals(Boolean.TRUE, wrap.getValue(WAction.SELECTED_KEY));
            wrap.putValue(WAction.SELECTED_KEY, Boolean.FALSE);
            assertFalse(item.getState());
            assertEquals(Arrays.asList("performed:true"), events);
        });
    }

    @Test
    void buttonsAndMenuItemsCanBeInTheSameButtonGroup() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WToggleButton toolbarBold = new WToggleButton("Bold");
            WToggleMenuFlyoutItem menuBold = new WToggleMenuFlyoutItem("Bold");
            group.add(toolbarBold);
            group.add(menuBold);
            toolbarBold.doClick();
            menuBold.doClick();
            assertSame(menuBold, group.getSelection());
            assertFalse(toolbarBold.isSelected());
        });
    }

    @Test
    void aCustomWSelectableImplementationCannotBeAddedToAButtonGroup() {
        onUiThread(() -> {
            WSelectable custom = new WSelectable() {
                @Override
                public boolean isSelected() {
                    return false;
                }

                @Override
                public void setSelected(boolean selected) {
                }

                @Override
                public String getText() {
                    return "";
                }

                @Override
                public void setText(String text) {
                }

                @Override
                public String getActionCommand() {
                    return null;
                }

                @Override
                public void setActionCommand(String command) {
                }

                @Override
                public void addItemListener(WItemListener listener) {
                }

                @Override
                public void removeItemListener(WItemListener listener) {
                }
            };
            WButtonGroup group = new WButtonGroup();
            assertThrows(IllegalArgumentException.class, () -> group.add(custom));
            assertEquals(0, group.getButtonCount());
        });
    }
}
