package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Tests that use WDefaultListModel from Java the way a developer used to Swing's DefaultListModel / AbstractListModel would write it. */
class WDefaultListModelForJavaTest {
    /** An anonymous-class listener, as commonly written in Swing, that records each event as a "type:index0-index1" string. */
    private static WListDataListener recordingTo(List<String> events) {
        return new WListDataListener() {
            @Override
            public void intervalAdded(WListDataEvent e) {
                events.add("added:" + e.getIndex0() + "-" + e.getIndex1());
            }

            @Override
            public void intervalRemoved(WListDataEvent e) {
                events.add("removed:" + e.getIndex0() + "-" + e.getIndex1());
            }

            @Override
            public void contentsChanged(WListDataEvent e) {
                events.add("changed:" + e.getIndex0() + "-" + e.getIndex1());
            }
        };
    }

    @Test
    void addElementGetElementAtAndGetSizeWorkLikeSwing() {
        WDefaultListModel<String> model = new WDefaultListModel<>();
        model.addElement("a");
        model.addElement("b");
        assertEquals(2, model.getSize());
        assertEquals(2, model.size());
        assertEquals("b", model.getElementAt(1));
        assertEquals("a", model.get(0));
        assertEquals("a", model.elementAt(0));
        assertEquals("a", model.firstElement());
        assertEquals("b", model.lastElement());
        assertEquals(false, model.isEmpty());
    }

    @Test
    void anonymousWListDataListenerReceivesTheSameRangesAsSwingForAddReplaceAndRemove() {
        WDefaultListModel<String> model = new WDefaultListModel<>();
        List<String> events = new ArrayList<>();
        model.addListDataListener(recordingTo(events));
        model.addElement("a");
        model.add(0, "b");
        model.insertElementAt("c", 1);
        model.set(0, "z");
        model.setElementAt("y", 2);
        model.addAll(Arrays.asList("p", "q"));
        model.remove(0);
        model.removeElement("y");
        model.removeRange(0, 1);
        model.clear();
        assertEquals(
                Arrays.asList(
                        "added:0-0", "added:0-0", "added:1-1", "changed:0-0", "changed:2-2", "added:3-4",
                        "removed:0-0", "removed:1-1", "removed:0-1", "removed:0-0"),
                events);
    }

    @Test
    void outOfRangeIndexThrowsArrayIndexOutOfBoundsExceptionAndFirstElementOfEmptyThrowsNoSuchElementException() {
        WDefaultListModel<String> model = new WDefaultListModel<>();
        assertThrows(NoSuchElementException.class, model::firstElement);
        model.addElement("a");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.getElementAt(1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> model.add(5, "x"));
        assertThrows(IllegalArgumentException.class, () -> model.removeRange(1, 0));
    }

    @Test
    void toArrayElementsIndexOfAndContains() {
        WDefaultListModel<String> model = new WDefaultListModel<>();
        model.addAll(Arrays.asList("a", "b", "a"));
        assertEquals(Arrays.asList("a", "b", "a"), Arrays.asList(model.toArray()));
        assertEquals(Arrays.asList("a", "b", "a"), Collections.list(model.elements()));
        assertEquals(2, model.lastIndexOf("a"));
        assertEquals(2, model.indexOf("a", 1));
        assertTrue(model.contains("b"));
        assertEquals("[a, b, a]", model.toString());
    }

    @Test
    void readOnlyModelCanBeMadeAsWAbstractListModelSubclassAndCallTheProtectedFireMethods() {
        List<String> events = new ArrayList<>();
        class Months extends WAbstractListModel<String> {
            @Override
            public int getSize() {
                return 12;
            }

            @Override
            public String getElementAt(int index) {
                return "Month " + (index + 1);
            }

            void changed() {
                fireContentsChanged(this, 0, 11);
            }
        }
        Months model = new Months();
        WListDataListener listener = recordingTo(events);
        model.addListDataListener(listener);
        model.changed();
        assertEquals("Month 3", model.getElementAt(2));
        assertEquals(Arrays.asList("changed:0-11"), events);
        assertEquals(1, model.getListDataListeners().length);
        assertSame(listener, model.getListDataListeners()[0]);
        model.removeListDataListener(listener);
        model.changed();
        assertEquals(1, events.size());
    }

    @Test
    void wListDataEventConstantsHaveTheSameValuesAsSwingAndIndex0AndIndex1AreInAscendingOrder() {
        assertEquals(0, WListDataEvent.CONTENTS_CHANGED);
        assertEquals(1, WListDataEvent.INTERVAL_ADDED);
        assertEquals(2, WListDataEvent.INTERVAL_REMOVED);
        WListDataEvent event = new WListDataEvent("src", WListDataEvent.INTERVAL_REMOVED, 7, 3);
        assertEquals(3, event.getIndex0());
        assertEquals(7, event.getIndex1());
        assertEquals(2, event.getType());
        assertEquals("src", event.getSource());
    }

    @Test
    void vectorDerivedSetSizeCopyIntoEnsureCapacityTrimToSizeAndCapacityCanBeCalledTheSameWayAsSwing() {
        WDefaultListModel<String> model = new WDefaultListModel<>();
        List<String> events = new ArrayList<>();
        model.addElement("a");
        model.addElement("b");
        model.addListDataListener(recordingTo(events));
        model.ensureCapacity(20);
        assertTrue(model.capacity() >= 20);
        model.setSize(3);
        Object[] copy = new Object[model.size()];
        model.copyInto(copy);
        assertEquals(Arrays.asList("a", "b", null), Arrays.asList(copy));
        model.setSize(1);
        model.trimToSize();
        assertEquals(1, model.capacity());
        assertEquals(Arrays.asList("added:2-2", "removed:1-2"), events);
    }
}
