package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WTabView from Java the way a developer used to Swing's JTabbedPane would write it. */
class WTabViewForJavaTest {
    @Test
    void tabsCreatedWithTheThreeAddTabFormsCanBeReadWithGetTitleAtAndOthers() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            WPanel general = new WPanel();
            WPanel detail = new WPanel();
            WLabel about = new WLabel("about");
            tabs.addTab("General", general);
            tabs.addTab("Detail", Symbol.SETTING, detail);
            tabs.addTab("About", null, about, "About this app");
            assertEquals(3, tabs.getTabCount());
            assertEquals("Detail", tabs.getTitleAt(1));
            assertSame(Symbol.SETTING, tabs.getIconAt(1));
            assertEquals("About this app", tabs.getToolTipTextAt(2));
            assertSame(detail, tabs.getComponentAt(1));
            assertEquals(2, tabs.indexOfComponent(about));
            assertEquals(0, tabs.indexOfTab("General"));
            assertFalse(tabs.getTab(0).isClosable());
            assertEquals(0, tabs.getSelectedIndex());
            assertSame(general, tabs.getSelectedComponent());
        });
    }

    @Test
    void anonymousChangeListenerIsCalledAfterSetSelectedIndexAndRemoveTabAt() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            tabs.addTab("A", new WLabel("a"));
            tabs.addTab("B", new WLabel("b"));
            tabs.insertTab("C", null, new WLabel("c"), null, 2);
            List<Integer> selected = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            tabs.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                    selected.add(((WTabView) e.getSource()).getSelectedIndex());
                }
            });
            tabs.setSelectedIndex(2);
            tabs.removeTabAt(2);
            tabs.removeAll();
            assertEquals(Arrays.asList(2, 1, -1), selected);
            assertSame(tabs, sources.get(0));
        });
    }

    @Test
    void lambdaChangeListenerIsRemovedByRemoveChangeListenerAndSetSelectedComponentSelects() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            WLabel second = new WLabel("second");
            tabs.addTab("A", new WLabel("a"));
            tabs.addTab("B", second);
            List<Integer> selected = new ArrayList<>();
            WChangeListener listener = e -> selected.add(tabs.getSelectedIndex());
            tabs.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, tabs.getChangeListeners());
            tabs.setSelectedComponent(second);
            tabs.removeChangeListener(listener);
            tabs.setSelectedIndex(0);
            assertEquals(Arrays.asList(1), selected);
            assertEquals(0, tabs.getChangeListeners().length);
        });
    }

    @Test
    void outOfRangeIndexThrowsIndexOutOfBoundsExceptionAndAMissingComponentThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            tabs.addTab("A", new WLabel("a"));
            assertThrows(IndexOutOfBoundsException.class, () -> tabs.setSelectedIndex(1));
            assertThrows(IndexOutOfBoundsException.class, () -> tabs.getTitleAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> tabs.insertTab("B", null, null, null, 2));
            assertThrows(IllegalArgumentException.class, () -> tabs.setSelectedComponent(new WLabel("other")));
        });
    }

    @Test
    void perTabSettingsSuchAsSetTitleAtAndTheIconOfWTabViewItem() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            tabs.addTab("A", new WLabel("a"));
            tabs.setTitleAt(0, "Renamed");
            tabs.setIconAt(0, new WFontIcon("X"));
            tabs.setToolTipTextAt(0, "tip");
            tabs.setEnabledAt(0, false);
            WTabViewItem tab = tabs.getTab(0);
            assertEquals("Renamed", tab.getHeader());
            assertEquals(new WFontIcon("X"), tab.getIcon());
            assertEquals("tip", tab.getToolTipText());
            assertFalse(tabs.isEnabledAt(0));
            tab.setIcon(null);
            assertNull(tabs.getIconAt(0));
            tabs.remove(tab);
            assertEquals(-1, tabs.getSelectedIndex());
        });
    }

    @Test
    void tabsCanBeAddedWithAddLikeJTabbedPaneWithNameTitleIconAndPosition() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            WPanel general = new WPanel();
            general.setName("General");
            WPanel detail = new WPanel();
            WLabel about = new WLabel("about");
            WLabel first = new WLabel("first");
            WLabel saved = new WLabel("saved");
            assertSame(general, tabs.add(general));
            assertSame(detail, tabs.add("Detail", detail));
            tabs.add(about, "About");
            assertSame(first, tabs.add(first, 0));
            tabs.add(saved, Symbol.SAVE, -1);
            assertEquals(5, tabs.getTabCount());
            assertEquals("", tabs.getTitleAt(0));
            assertEquals("General", tabs.getTitleAt(1));
            assertEquals("Detail", tabs.getTitleAt(2));
            assertEquals("About", tabs.getTitleAt(3));
            assertSame(Symbol.SAVE, tabs.getIconAt(4));
            assertSame(saved, tabs.getComponentAt(4));
            assertThrows(IndexOutOfBoundsException.class, () -> tabs.add(new WLabel("x"), 9));
        });
    }

    @Test
    void anonymousClassAndLambdaListenersCanBeAddedAndRemoved() {
        onUiThread(() -> {
            WTabView tabs = new WTabView();
            List<String> calls = new ArrayList<>();
            Runnable selection = () -> calls.add("selection");
            Runnable addButton = new Runnable() {
                @Override
                public void run() {
                    calls.add("add");
                }
            };
            java.util.function.IntConsumer close = index -> calls.add("close " + index);
            tabs.addSelectionListener(selection);
            tabs.addAddTabButtonClickListener(addButton);
            tabs.addTabCloseRequestedListener(close);
            tabs.removeSelectionListener(selection);
            tabs.removeAddTabButtonClickListener(addButton);
            tabs.removeTabCloseRequestedListener(close);
            tabs.addTab("A", new WLabel("a"));
            tabs.addTab("B", new WLabel("b"));
            tabs.setSelectedIndex(1);
            assertEquals(new ArrayList<String>(), calls);
            tabs.setTabWidthMode(TabViewWidthMode.COMPACT);
            assertEquals(TabViewWidthMode.COMPACT, tabs.getTabWidthMode());
            tabs.setAddTabButtonVisible(false);
            tabs.setCanReorderTabs(false);
            assertFalse(tabs.isAddTabButtonVisible());
            assertFalse(tabs.getCanReorderTabs());
        });
    }
}
