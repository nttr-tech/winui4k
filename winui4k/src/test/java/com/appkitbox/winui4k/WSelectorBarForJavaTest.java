package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WSelectorBar in the same way developers who have used JTabbedPane / JList would write it in Java. Compiling at all
 * verifies the shape of the API; in particular, that selection listeners can be written as lambdas without ambiguity, both in the form passing Swing's ChangeListener (WChangeListener)
 * and in the form receiving the index directly (addSelectionListener's IntConsumer).
 */
class WSelectorBarForJavaTest {
    private static WSelectorBar barOf(String... texts) {
        WSelectorBar bar = new WSelectorBar();
        for (String text : texts) {
            bar.addItem(new WSelectorBarItem(text));
        }
        return bar;
    }

    @Test
    void aLambdaCanBePassedToAddChangeListenerTheSourceIsTheSelectorBarAndGetSelectedIndexReadsTheSelection() {
        List<Integer> indexes = new ArrayList<>();
        onUiThread(() -> {
            WSelectorBar bar = barOf("A", "B");
            bar.addChangeListener(e -> indexes.add(((WSelectorBar) e.getSource()).getSelectedIndex()));
            bar.setSelectedIndex(1);
            bar.setSelectedIndex(-1);
        });
        assertEquals(Arrays.asList(1, -1), indexes);
    }

    @Test
    void aLambdaCanAlsoBePassedToAddSelectionListenerWithoutAmbiguity() {
        List<Integer> indexes = new ArrayList<>();
        onUiThread(() -> {
            WSelectorBar bar = barOf("A", "B");
            bar.addSelectionListener(index -> indexes.add(index));
            bar.addChangeListener(e -> indexes.add(100));
            bar.setSelectedIndex(0);
        });
        assertEquals(Arrays.asList(0, 100), indexes);
    }

    @Test
    void listenersCanBeManagedWithGetChangeListenersAndRemoveChangeListener() {
        onUiThread(() -> {
            WSelectorBar bar = new WSelectorBar();
            WChangeListener listener = e -> { };
            bar.addChangeListener(listener);
            assertEquals(1, bar.getChangeListeners().length);
            assertSame(listener, bar.getChangeListeners()[0]);
            bar.removeChangeListener(listener);
            assertEquals(0, bar.getChangeListeners().length);
        });
    }

    @Test
    void setSelectedIndexClearsWithMinusOneAndThrowsIndexOutOfBoundsExceptionWhenOutOfRange() {
        onUiThread(() -> {
            WSelectorBar bar = barOf("A", "B");
            bar.setSelectedIndex(1);
            assertEquals(1, bar.getSelectedIndex());
            bar.setSelectedIndex(-1);
            assertEquals(-1, bar.getSelectedIndex());
            assertThrows(IndexOutOfBoundsException.class, () -> bar.setSelectedIndex(2));
            assertThrows(IndexOutOfBoundsException.class, () -> bar.setSelectedIndex(-2));
        });
    }

    @Test
    void getItemInsertItemAndRemoveItemCanBeUsedTheSameWayAsJTabbedPane() {
        onUiThread(() -> {
            WSelectorBar bar = barOf("A", "C");
            WSelectorBarItem b = new WSelectorBarItem("B");
            bar.insertItem(b, 1);
            assertEquals(3, bar.getItemCount());
            assertSame(b, bar.getItem(1));
            bar.removeItem(b);
            assertEquals("C", bar.getItem(1).getText());
            bar.removeItem(0);
            assertEquals(1, bar.getItemCount());
            assertThrows(IndexOutOfBoundsException.class, () -> bar.getItem(1));
            assertThrows(IndexOutOfBoundsException.class, () -> bar.insertItem(new WSelectorBarItem("X"), 2));
        });
    }

    @Test
    void itemIconsTakeAWIconSuchAsSymbolAndCustomIconImplementationsAreRejected() {
        onUiThread(() -> {
            WSelectorBarItem item = new WSelectorBarItem("A");
            assertNull(item.getIcon());
            item.setIcon(Symbol.HOME);
            assertSame(Symbol.HOME, item.getIcon());
            assertFalse(item.isSelected());
            // Java does not enforce sealed, so a custom implementation meant as a Swing Icon can be passed
            WIcon foreign = new WIcon() { };
            assertThrows(IllegalArgumentException.class, () -> item.setIcon(foreign));
        });
    }
}
