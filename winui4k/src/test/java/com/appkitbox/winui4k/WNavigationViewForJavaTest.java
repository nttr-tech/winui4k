package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WNavigationView and WNavigationViewItem from Java the way a developer used to Swing's ChangeListener / ActionListener would write it. */
class WNavigationViewForJavaTest {
    @Test
    void anonymousChangeListenerReadsTheSelectionAfterSetSelectedItem() {
        onUiThread(() -> {
            WNavigationView navigation = new WNavigationView();
            WNavigationViewItem home = new WNavigationViewItem("Home", Symbol.HOME);
            WNavigationViewItem mail = new WNavigationViewItem("Mail");
            navigation.addItem(home);
            navigation.addItem(mail);
            List<String> selected = new ArrayList<>();
            navigation.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    WNavigationViewItem item = ((WNavigationView) e.getSource()).getSelectedItem();
                    selected.add(item == null ? null : item.getText());
                }
            });
            navigation.setSelectedItem(mail);
            navigation.setSelectedItem(mail);
            navigation.setSelectedItem(home);
            assertEquals(Arrays.asList("Mail", "Home"), selected);
            assertEquals(1, navigation.getChangeListeners().length);
        });
    }

    @Test
    void anonymousActionListenerOfAnItemReceivesTheLabelAsActionCommandOnClick() throws Exception {
        WNavigationViewItem home = onUiThreadGet(() -> new WNavigationViewItem("Home", Symbol.HOME));
        WNavigationViewItem mail = onUiThreadGet(() -> new WNavigationViewItem("Mail"));
        WNavigationView navigation = onUiThreadGet(() -> {
            WNavigationView view = new WNavigationView();
            view.setPaneDisplayMode(NavigationViewPaneDisplayMode.LEFT);
            view.addItem(home);
            view.addItem(mail);
            return view;
        });
        List<String> commands = new ArrayList<>();
        List<Object> sources = new ArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(navigation);
        try {
            onUiThread(() -> {
                navigation.updateLayout();
                mail.addActionListener(new WActionListener() {
                    @Override
                    public void actionPerformed(WActionEvent e) {
                        commands.add(e.getActionCommand());
                        sources.add(e.getSource());
                    }
                });
                WActionListener homeListener = e -> commands.add("home:" + e.getActionCommand());
                home.setActionCommand("go-home");
                home.addActionListener(homeListener);
                assertArrayEquals(new WActionListener[] {homeListener}, home.getActionListeners());
                NavigationClicks.clickNavigationItem(mail);
                NavigationClicks.clickNavigationItem(home);
                home.removeActionListener(homeListener);
                NavigationClicks.clickNavigationItem(home);
            });
        } finally {
            UiTestHarness.detach(navigation);
        }
        assertEquals(Arrays.asList("Mail", "home:go-home"), commands);
        assertSame(mail, sources.get(0));
    }

    @Test
    void iconCanBeReadAndWrittenAsAWIconAndTheSymbolConstructorStillWorks() {
        onUiThread(() -> {
            WNavigationViewItem item = new WNavigationViewItem("Home", Symbol.HOME);
            WIcon icon = item.getIcon();
            assertSame(Symbol.HOME, icon);
            item.setIcon(new WFontIcon("X"));
            assertEquals(new WFontIcon("X"), item.getIcon());
            item.setIcon(null);
            assertNull(item.getIcon());
            assertEquals("Home", item.getActionCommand());
        });
    }
}
