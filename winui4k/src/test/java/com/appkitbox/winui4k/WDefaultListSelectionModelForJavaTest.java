package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WDefaultListSelectionModel from Java the way a developer used to Swing's DefaultListSelectionModel would write it. */
class WDefaultListSelectionModelForJavaTest {
    @Test
    void constantsHaveTheSameValuesAsSwingListSelectionModelAndCanBeReadStatically() {
        assertEquals(0, WListSelectionModel.SINGLE_SELECTION);
        assertEquals(1, WListSelectionModel.SINGLE_INTERVAL_SELECTION);
        assertEquals(2, WListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    @Test
    void lambdaListenerReceivesTheRangeWithGetFirstIndexGetLastIndexAndGetValueIsAdjusting() {
        WListSelectionModel model = new WDefaultListSelectionModel();
        List<String> events = new ArrayList<>();
        model.addListSelectionListener(e ->
                events.add(e.getFirstIndex() + ".." + e.getLastIndex() + " adjusting=" + e.getValueIsAdjusting()));
        model.setSelectionInterval(3, 1);
        model.setValueIsAdjusting(true);
        model.addSelectionInterval(6, 6);
        model.setValueIsAdjusting(false);
        assertEquals(Arrays.asList("1..3 adjusting=false", "1..6 adjusting=true", "1..6 adjusting=false"), events);
    }

    @Test
    void getSelectedIndicesReturnsAnIntArrayInAscendingOrder() {
        WListSelectionModel model = new WDefaultListSelectionModel();
        model.addSelectionInterval(5, 6);
        model.addSelectionInterval(1, 1);
        assertArrayEquals(new int[] {1, 5, 6}, model.getSelectedIndices());
        assertEquals(3, model.getSelectedItemsCount());
        assertEquals(1, model.getMinSelectionIndex());
        assertEquals(6, model.getMaxSelectionIndex());
        assertTrue(model.isSelectedIndex(5));
        assertFalse(model.isSelectedIndex(2));
    }

    @Test
    void invalidSelectionModeThrowsIllegalArgumentException() {
        WListSelectionModel model = new WDefaultListSelectionModel();
        assertThrows(IllegalArgumentException.class, () -> model.setSelectionMode(7));
        model.setSelectionMode(WListSelectionModel.SINGLE_SELECTION);
        assertEquals(WListSelectionModel.SINGLE_SELECTION, model.getSelectionMode());
    }

    @Test
    void subclassCanNotifyWithProtectedFireValueChangedAndGetSourceIsTheModelItself() {
        class Custom extends WDefaultListSelectionModel {
            void fire() {
                fireValueChanged(2, 4);
            }
        }
        Custom model = new Custom();
        List<WListSelectionEvent> events = new ArrayList<>();
        model.addListSelectionListener(events::add);
        model.fire();
        assertEquals(1, events.size());
        assertSame(model, events.get(0).getSource());
        assertEquals(2, events.get(0).getFirstIndex());
        assertEquals(4, events.get(0).getLastIndex());
    }

    @Test
    void getListSelectionListenersReturnsAnArrayInRegistrationOrder() {
        WDefaultListSelectionModel model = new WDefaultListSelectionModel();
        WListSelectionListener a = e -> { };
        WListSelectionListener b = e -> { };
        model.addListSelectionListener(a);
        model.addListSelectionListener(b);
        assertArrayEquals(new WListSelectionListener[] {a, b}, model.getListSelectionListeners());
        model.removeListSelectionListener(a);
        assertArrayEquals(new WListSelectionListener[] {b}, model.getListSelectionListeners());
    }
}
