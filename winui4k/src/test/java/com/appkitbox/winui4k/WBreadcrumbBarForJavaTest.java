package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WBreadcrumbBar from Java the way a developer used to Swing's ActionListener would write it. */
class WBreadcrumbBarForJavaTest {
    @Test
    void anonymousActionListenerReceivesTheLabelOfTheClickedLevel() {
        WBreadcrumbBar bar = onUiThreadGet(() -> {
            WBreadcrumbBar created = new WBreadcrumbBar();
            created.setItems(Arrays.asList("C:", "Users", "hisano"));
            return created;
        });
        List<String> commands = new ArrayList<>();
        List<Object> sources = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(bar);
        try {
            onUiThread(() -> {
                bar.updateLayout();
                bar.addActionListener(new WActionListener() {
                    @Override
                    public void actionPerformed(WActionEvent e) {
                        commands.add(e.getActionCommand());
                        sources.add(e.getSource());
                    }
                });
                bar.addItemClickedListener(indexes::add);
                NavigationClicks.clickBreadcrumbItem(bar, "Users");
            });
        } finally {
            UiTestHarness.detach(bar);
        }
        assertEquals(Arrays.asList("Users"), commands);
        assertSame(bar, sources.get(0));
        assertEquals(Arrays.asList(1), indexes);
    }

    @Test
    void lambdaActionListenerIsRemovedByRemoveActionListener() {
        onUiThread(() -> {
            WBreadcrumbBar bar = new WBreadcrumbBar();
            WActionListener listener = e -> { };
            bar.addActionListener(listener);
            assertEquals(1, bar.getActionListeners().length);
            bar.removeActionListener(listener);
            assertEquals(0, bar.getActionListeners().length);
        });
    }
}
