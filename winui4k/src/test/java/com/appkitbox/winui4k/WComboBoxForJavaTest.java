package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WComboBox from Java in the same way developers who have used Swing's JComboBox would write it. */
class WComboBoxForJavaTest {
    @Test
    void theArrayConstructorSelectsTheFirstItemAndItemsCanBeReadWithGetItemAtAndGetItemCount() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox(new String[] {"Red", "Green", "Blue"});
            assertEquals(3, comboBox.getItemCount());
            assertEquals("Green", comboBox.getItemAt(1));
            assertNull(comboBox.getItemAt(3));
            assertEquals(0, comboBox.getSelectedIndex());
            assertEquals("Red", comboBox.getSelectedItem());
            assertArrayEquals(new Object[] {"Red"}, comboBox.getSelectedObjects());
        });
    }

    @Test
    void anAnonymousItemListenerAndALambdaActionListenerAreCalledInTheSameOrderAsSwing() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox(new String[] {"Red", "Green"});
            List<String> events = new ArrayList<>();
            comboBox.addItemListener(new WItemListener() {
                @Override
                public void itemStateChanged(WItemEvent e) {
                    String state = e.getStateChange() == WItemEvent.SELECTED ? "SELECTED" : "DESELECTED";
                    events.add(state + ":" + e.getItem());
                }
            });
            comboBox.addActionListener(e -> {
                WComboBox source = (WComboBox) e.getSource();
                events.add(e.getActionCommand() + ":" + source.getSelectedItem());
            });
            comboBox.setSelectedItem("Green");
            assertEquals(Arrays.asList("DESELECTED:Red", "SELECTED:Green", "comboBoxChanged:Green"), events);
        });
    }

    @Test
    void setSelectedIndexThrowsIllegalArgumentExceptionWhenOutOfRange() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox(new String[] {"Red"});
            assertThrows(IllegalArgumentException.class, () -> comboBox.setSelectedIndex(1));
            comboBox.setSelectedIndex(-1);
            assertNull(comboBox.getSelectedItem());
        });
    }

    @Test
    void setSelectedIndexBeforeAddingItemsDoesNotThrowAndIsRememberedAndSelectsTheItemWhenAdded() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox();
            comboBox.setSelectedIndex(2);
            assertEquals(-1, comboBox.getSelectedIndex());
            comboBox.addItem("Red");
            comboBox.addItem("Green");
            comboBox.addItem("Blue");
            assertEquals(2, comboBox.getSelectedIndex());
            assertEquals("Blue", comboBox.getSelectedItem());
        });
    }

    @Test
    void itemsCanBeEditedWithInsertItemAtRemoveItemAtAndRemoveItem() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox();
            comboBox.addItem("Red");
            comboBox.addItem("Blue");
            comboBox.insertItemAt("Green", 1);
            comboBox.removeItemAt(0);
            comboBox.removeItem("Blue");
            assertEquals(1, comboBox.getItemCount());
            assertEquals("Green", comboBox.getItemAt(0));
        });
    }

    @Test
    void popupMenuListenerCanBeWrittenWithWPopupMenuAdapterAndReadWithGetPopupMenuListeners() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox();
            WPopupMenuListener listener = new WPopupMenuAdapter() {
                @Override
                public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                }
            };
            comboBox.addPopupMenuListener(listener);
            assertSame(listener, comboBox.getPopupMenuListeners()[0]);
            comboBox.removePopupMenuListener(listener);
            assertEquals(0, comboBox.getPopupMenuListeners().length);
            assertFalse(comboBox.isPopupVisible());
            assertEquals(8, comboBox.getMaximumRowCount());
            comboBox.setMaximumRowCount(3);
            assertEquals(3, comboBox.getMaximumRowCount());
        });
    }

    @Test
    void comboBoxModelConstructorAndSetModelGetModelHandleItemsOfAnyType() {
        onUiThread(() -> {
            WDefaultComboBoxModel<Integer> numbers = new WDefaultComboBoxModel<>(new Integer[] {10, 20, 30});
            WComboBox comboBox = new WComboBox(numbers);
            assertSame(numbers, comboBox.getModel());
            assertEquals("10", comboBox.getSelectedItem());
            comboBox.setSelectedIndex(2);
            assertEquals(30, numbers.getSelectedItem());
            assertEquals(30, comboBox.getSelectedObjects()[0]);

            WDefaultComboBoxModel<String> colors = new WDefaultComboBoxModel<>();
            colors.addElement("Red");
            comboBox.setModel(colors);
            assertEquals(1, comboBox.getItemCount());
            assertEquals("Red", comboBox.getSelectedItem());
            WComboBoxModel<?> model = comboBox.getModel();
            assertEquals("Red", model.getElementAt(0));
        });
    }

    @Test
    void actionCommandCanBeReadAndWrittenWithTheGetterAndSetter() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox();
            assertEquals("comboBoxChanged", comboBox.getActionCommand());
            comboBox.setActionCommand("color");
            assertEquals("color", comboBox.getActionCommand());
            comboBox.setEditable(true);
            assertTrue(comboBox.isEditable());
        });
    }
}
