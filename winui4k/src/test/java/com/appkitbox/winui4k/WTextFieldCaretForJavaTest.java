package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WTextField's CaretListener in the same Java form as developers who have used Swing's CaretListener would write. */
class WTextFieldCaretForJavaTest {
    @Test
    void lambdaCaretListenerReceivesDotAndMarkOfSelect() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Hello, World");
            List<String> events = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            field.addCaretListener(e -> {
                events.add(e.getDot() + "-" + e.getMark());
                sources.add(e.getSource());
            });
            field.select(7, 12);
            field.setCaretPosition(0);
            assertEquals(Arrays.asList("12-7", "0-0"), events);
            assertSame(field, sources.get(0));
        });
    }

    @Test
    void anonymousClassCaretListenerStopsReceivingOnceRemoved() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("abc");
            List<Integer> dots = new ArrayList<>();
            WCaretListener listener = new WCaretListener() {
                @Override
                public void caretUpdate(WCaretEvent e) {
                    dots.add(e.getDot());
                }
            };
            field.addCaretListener(listener);
            assertEquals(1, field.getCaretListeners().length);
            field.setCaretPosition(2);
            field.removeCaretListener(listener);
            field.setCaretPosition(1);
            assertEquals(Arrays.asList(2), dots);
        });
    }
}
