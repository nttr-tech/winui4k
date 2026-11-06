package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WToggleSwitch from Java in the same way as Swing's JToggleButton
 * (setText, setActionCommand, addActionListener, getActionListeners).
 */
class WToggleSwitchButtonForJavaTest {
    @Test
    void setTextAndSetActionCommandCanBeUsed() {
        onUiThread(() -> {
            WToggleSwitch wifi = new WToggleSwitch();
            wifi.setText("Wi-Fi");
            assertEquals("Wi-Fi", wifi.getHeader());
            assertEquals("Wi-Fi", wifi.getActionCommand());
            wifi.setActionCommand("wifi");
            assertEquals("wifi", wifi.getActionCommand());
        });
    }

    @Test
    void allListenersReturnedByGetActionListenersCanBeRemoved() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WToggleSwitch wifi = new WToggleSwitch("Wi-Fi");
            WActionListener listener = e -> calls.add(e.getActionCommand());
            wifi.addActionListener(listener);
            wifi.addActionListener(new WActionListener() {
                @Override
                public void actionPerformed(WActionEvent e) {
                    calls.add("anonymous");
                }
            });
            assertEquals(2, wifi.getActionListeners().length);
            for (WActionListener l : wifi.getActionListeners()) {
                wifi.removeActionListener(l);
            }
            assertEquals(0, wifi.getActionListeners().length);
            wifi.setSelected(true);
            assertTrue(calls.isEmpty());
        });
    }
}
