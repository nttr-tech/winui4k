package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WPopup from Java in a way close to Swing's Popup / JPopupMenu (show(owner, x, y), setVisible, isVisible, addPopupMenuListener,
 * addOpenListener). Positions and the open/close order are checked in WPopupTest.
 */
class WPopupForJavaTest {
    private final List<String> events = new ArrayList<>();

    @Test
    void setVisibleTrueOnAPopupNeverOpenedThrowsIllegalStateException() {
        onUiThread(() -> {
            WPopup popup = new WPopup(new WLabel("Content"));
            assertFalse(popup.isVisible());
            assertThrows(IllegalStateException.class, () -> popup.setVisible(true));
        });
    }

    @Test
    void openingWithShowAndClosingWithSetVisibleFalseCallsTheListeners() throws Exception {
        AtomicReference<WPanel> panel = new AtomicReference<>();
        AtomicReference<WButton> owner = new AtomicReference<>();
        AtomicReference<WPopup> popup = new AtomicReference<>();
        onUiThread(() -> {
            panel.set(new WPanel());
            owner.set(new WButton("Owner"));
            panel.get().add(owner.get());
            WPopup created = new WPopup(new WLabel("Content"));
            created.addOpenListener(() -> events.add("open"));
            created.addPopupMenuListener(new WPopupMenuAdapter() {
                @Override
                public void popupMenuWillBecomeInvisible(WPopupMenuEvent e) {
                    events.add("invisible:" + (e.getSource() == created));
                }

                @Override
                public void popupMenuCanceled(WPopupMenuEvent e) {
                    events.add("canceled");
                }
            });
            popup.set(created);
        });
        UiTestHarness.attachAndAwaitLoaded(panel.get());
        try {
            onUiThread(() -> popup.get().show(owner.get(), 5, 6));
            awaitEvents(Arrays.asList("open"));
            onUiThread(() -> {
                assertTrue(popup.get().isVisible());
                popup.get().setVisible(false);
            });
            awaitEvents(Arrays.asList("open", "invisible:true"));
        } finally {
            UiTestHarness.detach(panel.get());
        }
    }

    private void awaitEvents(List<String> expected) throws InterruptedException {
        List<String> current = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            onUiThread(() -> {
                current.clear();
                current.addAll(events);
            });
            if (current.equals(expected)) {
                return;
            }
            Thread.sleep(50);
        }
        assertEquals(expected, current);
    }
}
