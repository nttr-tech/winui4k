package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WList from Java the way a developer used to Swing's JList and DefaultListModel would write it.
 * Compiling at all verifies the shape of the API, and the body checks the model and the resulting display and selection.
 */
class WListForJavaTest {
    @Test
    void constructorWithAnArrayLetsGetModelReadTheElementsAndGetSelectedValueIsTheSelectedElement() {
        onUiThread(() -> {
            WList list = new WList(new String[] {"Apple", "Orange", "Grape"});
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
            WList list = new WList(model);
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
            WList list = new WList();
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
            WList list = new WList(new WAbstractListModel<String>() {
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
            WList list = new WList(days);
            list.addItem("Wed");
            assertEquals(3, list.getModel().getSize());
            assertTrue(list.getModel() instanceof WDefaultListModel);
            assertEquals(0, new WList().getItemCount());
        });
    }

    @Test
    void listSelectionModelConstantsEnableMultipleIntervalSelectionAndIntervalSelectionAndGetSelectedIndicesWork() {
        onUiThread(() -> {
            WList list = new WList(new String[] {"a", "b", "c", "d", "e"});
            list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setSelectionInterval(1, 2);
            list.addSelectionInterval(4, 4);
            assertArrayEquals(new int[] {1, 2, 4}, list.getSelectedIndices());
            assertEquals(1, list.getMinSelectionIndex());
            assertEquals(4, list.getMaxSelectionIndex());
            assertEquals(4, list.getLeadSelectionIndex());
            assertTrue(list.isSelectedIndex(2));
            assertEquals(Arrays.asList("b", "c", "e"), list.getSelectedValuesList());
            assertEquals(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION, list.getSelectionModel().getSelectionMode());
            list.clearSelection();
            assertTrue(list.isSelectionEmpty());
        });
    }

    @Test
    void aListSelectionListenerLambdaCanCheckGetValueIsAdjustingAndTheSourceIsTheList() {
        onUiThread(() -> {
            WList list = new WList(new String[] {"a", "b", "c"});
            List<String> log = new ArrayList<>();
            list.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    log.add(e.getFirstIndex() + "-" + e.getLastIndex() + ":" + (e.getSource() == list));
                }
            });
            list.setSelectedIndex(2);
            list.setSelectedIndex(0);
            assertEquals(Arrays.asList("2-2:true", "0-2:true"), log);
            assertEquals(1, list.getListSelectionListeners().length);
        });
    }

    @Test
    void noArgRunnableListenersStillWorkAndAreCalledOnlyOnceForSetSelectedIndices() {
        onUiThread(() -> {
            WList list = new WList(new String[] {"a", "b", "c", "d"});
            list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            int[] runs = {0};
            list.addListSelectionListener(() -> runs[0]++);
            list.setSelectedIndices(new int[] {0, 3});
            assertEquals(1, runs[0]);
            assertArrayEquals(new int[] {0, 3}, list.getSelectedIndices());
        });
    }

    @Test
    void settingACustomDefaultListSelectionModelReflectsItsSelectionAndSetSelectedValueSelectsByValue() {
        onUiThread(() -> {
            WList list = new WList(new String[] {"Red", "Green", "Blue"});
            WDefaultListSelectionModel selection = new WDefaultListSelectionModel();
            selection.setSelectionMode(WListSelectionModel.SINGLE_SELECTION);
            selection.setSelectionInterval(1, 1);
            list.setSelectionModel(selection);
            assertSame(selection, list.getSelectionModel());
            assertEquals("Green", list.getSelectedValue());
            list.setSelectedValue("Blue", true);
            assertEquals(2, list.getSelectedIndex());
            assertEquals(2, selection.getLeadSelectionIndex());
            assertThrows(IllegalArgumentException.class, () -> list.setSelectionMode(7));
        });
    }

    @Test
    void setVisibleRowCountMakesTheHeightFitTheRowsAndLocationToIndexIsMinusOneWhenNotShown() {
        onUiThread(() -> {
            WList list = new WList(new String[] {"Red", "Green", "Blue"});
            assertEquals(8, list.getVisibleRowCount());
            list.setVisibleRowCount(4);
            assertEquals(4, list.getVisibleRowCount());
            assertEquals(160.0, list.getHeight());
            assertEquals(-1, list.locationToIndex(new WPoint(5, 5)));
            assertEquals(-1, list.getFirstVisibleIndex());
            assertNull(list.indexToLocation(0));
            assertNull(list.getCellBounds(0, 2));
            list.ensureIndexIsVisible(-1);
            list.ensureIndexIsVisible(2);
        });
    }

    @Test
    void inAShownListAPointInsideAnItemsBoundsPassedToLocationToIndexGivesThatItem() {
        WList[] holder = new WList[1];
        onUiThread(() -> holder[0] = new WList(new String[] {"Red", "Green", "Blue"}));
        WList list = holder[0];
        UiTestHarness.attachAndAwaitLoaded(list);
        try {
            onUiThread(() -> {
                list.updateLayout();
                WRectangle cell = list.getCellBounds(1, 1);
                WPoint center = new WPoint(cell.getX() + cell.getWidth() / 2, cell.getY() + cell.getHeight() / 2);
                assertEquals(1, list.locationToIndex(center));
                assertEquals(new WPoint(cell.getX(), cell.getY()), list.indexToLocation(1));
                assertEquals(0, list.getFirstVisibleIndex());
                assertEquals(2, list.getLastVisibleIndex());
            });
        } finally {
            UiTestHarness.detach(list);
        }
    }

    @Test
    void elementsCanBeDrawnWithARendererExtendingDefaultListCellRendererAndALambdaRenderer() {
        WList[] holder = new WList[1];
        List<Integer> selectedIndices = new ArrayList<>();
        onUiThread(() -> {
            WList list = new WList(new Object[] {"Red", 2, null});
            assertNull(list.getCellRenderer());
            list.setCellRenderer(new WDefaultListCellRenderer() {
                @Override
                protected String getText(Object value) {
                    return "Color: " + value;
                }

                @Override
                protected void configure(WLabel label, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (isSelected) {
                        selectedIndices.add(index);
                    }
                }
            });
            holder[0] = list;
        });
        WList list = holder[0];
        UiTestHarness.attachAndAwaitLoaded(list);
        try {
            onUiThread(() -> {
                list.updateLayout();
                list.setSelectedIndex(1);
                assertEquals(Collections.singletonList(1), selectedIndices);
                WListCellRenderer<Object> plain = (l, value, index, isSelected, cellHasFocus) -> new WLabel(String.valueOf(value));
                list.setCellRenderer(plain);
                assertSame(plain, list.getCellRenderer());
                assertEquals(2, list.getSelectedValue());
            });
        } finally {
            UiTestHarness.detach(list);
        }
    }
}
