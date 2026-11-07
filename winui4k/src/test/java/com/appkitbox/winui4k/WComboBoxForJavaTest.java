package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
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
import java.util.concurrent.Callable;
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
    void setSelectedIndexBeforeAddingItemsThrowsLikeJComboBoxAndTheFirstItemAddedIsSelected() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox();
            assertThrows(IllegalArgumentException.class, () -> comboBox.setSelectedIndex(2));
            assertEquals(-1, comboBox.getSelectedIndex());
            comboBox.addItem("Red");
            comboBox.addItem("Green");
            comboBox.addItem("Blue");
            assertEquals(0, comboBox.getSelectedIndex());
            assertEquals("Red", comboBox.getSelectedItem());
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
    void removeItemOfNumericItemsRemovesTheEqualItemAsInSwingAndRemoveItemAtRemovesByPosition() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox(new WDefaultComboBoxModel<>(new Integer[] {3, 2, 1, 0}));
            comboBox.removeItem(1);
            assertEquals(Arrays.asList(3, 2, 0), elements(comboBox));
            comboBox.removeItem(7);
            assertEquals(Arrays.asList(3, 2, 0), elements(comboBox));
            comboBox.removeItemAt(1);
            assertEquals(Arrays.asList(3, 0), elements(comboBox));
            assertThrows(IndexOutOfBoundsException.class, () -> comboBox.removeItemAt(2));
        });
    }

    @Test
    void getSelectedItemAndGetItemAtReturnTheItemItselfNotTheDisplayedTextLikeJComboBox() {
        onUiThread(() -> {
            Country japan = new Country("Japan");
            Country france = new Country("France");
            WComboBox comboBox = new WComboBox(new WDefaultComboBoxModel<>(new Country[] {japan, france}));
            assertSame(japan, comboBox.getSelectedItem());
            comboBox.setSelectedIndex(1);
            Country selected = (Country) comboBox.getSelectedItem();
            assertSame(france, selected);
            assertEquals("France", comboBox.getSelectedItemText());
            assertSame(japan, comboBox.getItemAt(0));
            assertSame(france, comboBox.getItemAt(1));
            assertNull(comboBox.getItemAt(2));
            assertNull(comboBox.getItemAt(-1));
            comboBox.setSelectedIndex(-1);
            assertNull(comboBox.getSelectedItem());
            assertNull(comboBox.getSelectedItemText());
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
            assertEquals(10, comboBox.getSelectedItem());
            assertEquals("10", comboBox.getSelectedItemText());
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

    @Test
    void popupMenuCanceledArrivesOnlyWhenClosedWithEscAndNotWithHidePopup() throws InterruptedException {
        List<String> events = new ArrayList<>();
        WComboBox comboBox = onUiThreadGet(() -> new WComboBox(new String[] {"Red", "Green"}));
        onUiThread(() -> comboBox.addPopupMenuListener(new WPopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                events.add("visible");
            }

            @Override
            public void popupMenuWillBecomeInvisible(WPopupMenuEvent e) {
                events.add("invisible");
            }

            @Override
            public void popupMenuCanceled(WPopupMenuEvent e) {
                events.add("canceled");
            }
        }));
        UiTestHarness.attachAndAwaitLoaded(comboBox);
        try {
            onUiThread(comboBox::showPopup);
            awaitEvents(events, Arrays.asList("visible"));
            onUiThread(comboBox::hidePopup);
            awaitEvents(events, Arrays.asList("visible", "invisible"));

            onUiThread(() -> {
                events.clear();
                assertTrue(comboBox.requestFocusInWindow());
                comboBox.setPopupVisible(true);
            });
            awaitEvents(events, Arrays.asList("visible"));
            TestKeyboard.INSTANCE.press(VK_ESCAPE, SCAN_ESCAPE);
            awaitEvents(events, Arrays.asList("visible", "canceled", "invisible"));
            assertFalse(onUiThreadGet(comboBox::isPopupVisible));
        } finally {
            UiTestHarness.detach(comboBox);
        }
    }

    /** An item that is not a string (toString is the name). */
    private static final class Country {
        private final String name;

        Country(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Reads the items of the combo box's model in order. */
    private static List<Object> elements(WComboBox comboBox) {
        List<Object> elements = new ArrayList<>();
        WComboBoxModel<?> model = comboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            elements.add(model.getElementAt(i));
        }
        return elements;
    }

    /** Waits until [events] read on the UI thread becomes [expected], and checks that it does not change after waiting a little longer. */
    private static void awaitEvents(List<String> events, List<String> expected) throws InterruptedException {
        Callable<List<String>> snapshot = () -> new ArrayList<>(events);
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!onUiThreadGet(snapshot).equals(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        Thread.sleep(SETTLE_MILLIS);
        assertEquals(expected, onUiThreadGet(snapshot));
    }

    private static final int VK_ESCAPE = 0x1B;
    private static final int SCAN_ESCAPE = 0x01;
    private static final long TIMEOUT_MILLIS = 3_000;
    private static final long SETTLE_MILLIS = 500;
}
