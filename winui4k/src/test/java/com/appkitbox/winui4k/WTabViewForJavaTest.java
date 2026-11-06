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
}
