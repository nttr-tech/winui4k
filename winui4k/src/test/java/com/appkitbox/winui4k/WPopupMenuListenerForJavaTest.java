package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WPopupMenuListener and WPopupMenuAdapter from Java the way a developer used to writing Swing's PopupMenuListener would write it. */
class WPopupMenuListenerForJavaTest {
    @Test
    void wPopupMenuAdapterCanBeExtendedByAnAnonymousClassOverridingOnlyTheNeededMethods() {
        List<String> calls = new ArrayList<>();
        WPopupMenuAdapter adapter = new WPopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                calls.add("visible");
            }
        };
        Object source = new Object();
        WPopupMenuEvent event = new WPopupMenuEvent(source);
        adapter.popupMenuWillBecomeVisible(event);
        adapter.popupMenuWillBecomeInvisible(event);
        adapter.popupMenuCanceled(event);
        assertEquals(Arrays.asList("visible"), calls);
        assertSame(source, event.getSource());
    }

    @Test
    void wPopupMenuListenerCanImplementItsThreeMethodsInAnAnonymousClass() {
        List<String> calls = new ArrayList<>();
        WPopupMenuListener listener = new WPopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(WPopupMenuEvent e) {
                calls.add("visible");
            }

            @Override
            public void popupMenuWillBecomeInvisible(WPopupMenuEvent e) {
                calls.add("invisible");
            }

            @Override
            public void popupMenuCanceled(WPopupMenuEvent e) {
                calls.add("canceled");
            }
        };
        WPopupMenuEvent event = new WPopupMenuEvent("flyout");
        listener.popupMenuCanceled(event);
        listener.popupMenuWillBecomeInvisible(event);
        assertEquals(Arrays.asList("canceled", "invisible"), calls);
    }
}
