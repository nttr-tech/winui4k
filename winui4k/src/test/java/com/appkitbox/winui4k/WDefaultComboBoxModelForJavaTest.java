package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.junit.jupiter.api.Test;

/** Tests that use WDefaultComboBoxModel from Java in the same way developers who have used Swing's DefaultComboBoxModel would write it (no window is used). */
class WDefaultComboBoxModelForJavaTest {
    @Test
    void theArrayAndVectorConstructorsSelectTheFirstItemAndGetSelectedItemAndSetSelectedItemReadAndWriteIt() {
        WDefaultComboBoxModel<String> model = new WDefaultComboBoxModel<>(new String[] {"Red", "Green"});
        assertEquals("Red", model.getSelectedItem());
        model.setSelectedItem("Green");
        assertEquals("Green", model.getSelectedItem());
        Vector<Integer> numbers = new Vector<>(Arrays.asList(10, 20));
        WDefaultComboBoxModel<Integer> numberModel = new WDefaultComboBoxModel<>(numbers);
        assertEquals(10, numberModel.getSelectedItem());
        assertEquals(Integer.valueOf(20), numberModel.getElementAt(1));
        assertNull(numberModel.getElementAt(2));
    }

    @Test
    void itemsCanBeEditedAsAWMutableComboBoxModelAndNotificationsReceivedWithAnAnonymousListDataListener() {
        WDefaultComboBoxModel<String> model = new WDefaultComboBoxModel<>();
        List<String> events = new ArrayList<>();
        model.addListDataListener(new WListDataListener() {
            @Override
            public void intervalAdded(WListDataEvent e) {
                events.add("added:" + e.getIndex0());
            }

            @Override
            public void intervalRemoved(WListDataEvent e) {
                events.add("removed:" + e.getIndex0());
            }

            @Override
            public void contentsChanged(WListDataEvent e) {
                events.add("changed:" + e.getIndex0());
            }
        });
        WMutableComboBoxModel<String> mutable = model;
        mutable.addElement("Red");
        mutable.insertElementAt("Green", 0);
        mutable.removeElement("Red");
        assertEquals(Arrays.asList("added:0", "changed:-1", "added:0", "changed:-1", "removed:1"), events);
        assertEquals("Green", model.getSelectedItem());
        assertEquals(0, model.getIndexOf("Green"));
        assertEquals(1, model.getSize());
    }

    @Test
    void addAllThrowsArrayIndexOutOfBoundsExceptionForAnOutOfRangePosition() {
        WDefaultComboBoxModel<String> model = new WDefaultComboBoxModel<>();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.addAll(1, Arrays.asList("Red")));
        model.addAll(Arrays.asList("Red", "Green"));
        model.removeAllElements();
        assertEquals(0, model.getSize());
        assertNull(model.getSelectedItem());
    }
}
