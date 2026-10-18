package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTextField's ActionListener from Java, written the way developers used to Swing's JTextField would
 * write them. Listeners are invoked with {@code postActionEvent()} (same as JTextField; the same notification as
 * pressing Enter). Behavior with an actual Enter key press is covered by WTextFieldActionTest.
 */
class WTextFieldForJavaTest {
    private final List<String> calls = new ArrayList<>();

    private void onEnter() {
        calls.add("noArg");
    }

    private void onAction(WActionEvent e) {
        calls.add("event:" + e.getActionCommand());
    }

    @Test
    void aLambdaListenerTakingEReceivesAnEventWithTheFieldAsSourceAndTheTextAsActionCommand() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Tokyo");
            List<WActionEvent> events = new ArrayList<>();
            field.addActionListener(e -> events.add(e));
            field.postActionEvent();
            assertEquals(1, events.size());
            assertSame(field, events.get(0).getSource());
            assertEquals("Tokyo", events.get(0).getActionCommand());
        });
    }

    @Test
    void noArgLambdaAndMethodReferenceListenersAreAlsoCalledInRegistrationOrder() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Input");
            field.addActionListener(() -> calls.add("lambda"));
            field.addActionListener(this::onEnter);
            field.addActionListener(this::onAction);
            field.postActionEvent();
            assertEquals(Arrays.asList("lambda", "noArg", "event:Input"), calls);
        });
    }

    @Test
    void theStringSetWithSetActionCommandGoesIntoTheEventAndNullRevertsToTheFieldText() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Tokyo");
            field.addActionListener(this::onAction);

            field.setActionCommand("search");
            assertEquals("search", field.getActionCommand());
            field.postActionEvent();

            field.setActionCommand(null);
            assertEquals("Tokyo", field.getActionCommand());
            field.postActionEvent();

            assertEquals(Arrays.asList("event:search", "event:Tokyo"), calls);
        });
    }

    @Test
    void aListenerRemovedWithRemoveActionListenerIsNotCalled() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            Runnable plain = () -> calls.add("plain");
            WActionListener withEvent = e -> calls.add("withEvent");
            field.addActionListener(plain);
            field.addActionListener(withEvent);
            field.removeActionListener(plain);
            field.postActionEvent();
            field.removeActionListener(withEvent);
            field.postActionEvent();
            assertEquals(Arrays.asList("withEvent"), calls);
        });
    }
}
