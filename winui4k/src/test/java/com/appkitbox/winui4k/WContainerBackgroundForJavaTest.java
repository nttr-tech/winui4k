package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WContainer's background from Java as a Swing developer adding a background and mouse listeners to a JPanel would write it
 * (getBackground / setBackground and an anonymous WMouseAdapter).
 */
class WContainerBackgroundForJavaTest {
    @Test
    void colorSetWithSetBackgroundIsReadBackWithGetBackgroundAndNullRemovesIt() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            assertNull(panel.getBackground());
            panel.setBackground(new WColor(10, 20, 30));
            assertEquals(new WColor(10, 20, 30), panel.getBackground());
            panel.setBackground(null);
            assertNull(panel.getBackground());
        });
    }

    @Test
    void panelWithAnAnonymousWMouseAdapterIsHitTestedOverItsWholeBoundsAndTheBackgroundStaysNull() {
        WPanel panel = UiTestHarness.onUiThreadGet(() -> {
            WPanel p = new WPanel();
            p.setWidth(200.0);
            p.setHeight(100.0);
            p.setHorizontalAlignment(HorizontalAlignment.LEFT);
            p.setVerticalAlignment(VerticalAlignment.TOP);
            p.addMouseListener(new WMouseAdapter() {
                @Override
                public void mouseClicked(WMouseEvent e) {
                    // Only checks hit testing
                }
            });
            return p;
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                panel.updateLayout();
                assertTrue(HitTest.INSTANCE.hits(panel, 100.0, 50.0));
                assertNull(panel.getBackground());
            });
        } finally {
            UiTestHarness.detach(panel);
        }
    }
}
