package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests that use WRepeatButton from Java, in the form of Swing's JButton plus a repeat rate. */
class WRepeatButtonForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WRepeatButton().getText());
            assertEquals("Increase", new WRepeatButton("Increase").getText());
        });
    }

    @Test
    void setDelayAndSetIntervalDetermineRepeatRateAndGettersReadThemBack() {
        onUiThread(() -> {
            WRepeatButton button = new WRepeatButton("Increase");
            int initialInterval = button.getInterval();
            button.setDelay(600);
            assertEquals(initialInterval, button.getInterval());
            button.setInterval(30);
            assertEquals(600, button.getDelay());
            assertEquals(30, button.getInterval());
        });
    }

    @Test
    void actionListenerIsCalledOncePerDoClick() {
        onUiThread(() -> {
            WRepeatButton button = new WRepeatButton("Increase");
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.doClick();
            button.doClick();
            assertEquals(2, count[0]);
        });
    }

    @Test
    void listenerTakingEventReceivesEventWhoseSourceIsTheButtonItself() {
        onUiThread(() -> {
            WRepeatButton button = new WRepeatButton("Increase");
            Object[] source = {null};
            button.addActionListener(e -> source[0] = e.getSource());
            button.doClick();
            assertEquals(button, source[0]);
        });
    }
}
