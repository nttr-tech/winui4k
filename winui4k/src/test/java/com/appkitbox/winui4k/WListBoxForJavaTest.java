package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WListBox from Java the way a developer used to Swing's JList and DefaultListModel would write it.
 * Compiling at all verifies the shape of the API, and the body checks the model and the resulting display and selection.
 */
class WListBoxForJavaTest {
    @Test
    void constructorWithAnArrayLetsGetModelReadTheElementsAndGetSelectedValueIsTheSelectedElement() {
        onUiThread(() -> {
            WListBox list = new WListBox(new String[] {"Apple", "Orange", "Grape"});
            WListModel<?> model = list.getModel();
            assertEquals(3, model.getSize());
            assertEquals("Orange", model.getElementAt(1));
            assertNull(list.getSelectedValue());
            list.setSelectedIndex(2);
            assertEquals("Grape", list.getSelectedValue());
            assertEquals(Collections.singletonList("Grape"), list.getSelectedValuesList());
        });
    }

    @Test
    void modelCreatedLikeDefaultListModelAndPassedInDeliversModelChangesToTheDisplay() {
        onUiThread(() -> {
            WDefaultListModel<String> model = new WDefaultListModel<>();
            model.addElement("Mon");
            model.addElement("Tue");
            WListBox list = new WListBox(model);
            model.addElement("Wed");
            model.removeElementAt(0);
            assertSame(model, list.getModel());
            assertEquals(2, list.getItemCount());
            assertEquals("Tue", list.getItem(0));
        });
    }

    @Test
    void contentsCanBeReplacedWithSetModelAndSetListData() {
        onUiThread(() -> {
            WListBox list = new WListBox();
            WDefaultListModel<Integer> numbers = new WDefaultListModel<>();
            numbers.addElement(1);
            list.setModel(numbers);
            assertEquals("1", list.getItem(0));
            list.setListData(new Object[] {"a", "b"});
            assertEquals(2, list.getModel().getSize());
            Vector<String> vector = new Vector<>(Arrays.asList("x", "y", "z"));
            list.setListData(vector);
            assertEquals(3, list.getItemCount());
            assertEquals("z", list.getItem(2));
        });
    }

    @Test
    void anonymousAbstractListModelCanBeShownAndAddItemThrowsUnsupportedOperationException() {
        onUiThread(() -> {
            final String[] data = {"One", "Two"};
            WListBox list = new WListBox(new WAbstractListModel<String>() {
                @Override
                public int getSize() {
                    return data.length;
                }

                @Override
                public String getElementAt(int index) {
                    return data[index];
                }
            });
            assertEquals("Two", list.getItem(1));
            assertThrows(UnsupportedOperationException.class, () -> list.addItem("Three"));
        });
    }

    @Test
    void stringListConstructorAndNoArgumentConstructorCanBeUsedAndAddItemGoesIntoTheModel() {
        onUiThread(() -> {
            List<String> days = Arrays.asList("Mon", "Tue");
            WListBox list = new WListBox(days);
            list.addItem("Wed");
            assertEquals(3, list.getModel().getSize());
            assertTrue(list.getModel() instanceof WDefaultListModel);
            assertEquals(0, new WListBox().getItemCount());
        });
    }
}
