package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same forms as JComponent's getInputMap / getActionMap / registerKeyboardAction in the Java a Swing developer writes
 * (constants such as JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke strings, anonymous subclasses of AbstractAction).
 * Keys are sent with {@link TestKeyboard} to the input child window of the shared window (modifier keys are not delivered, so F7 is pressed).
 */
class WComponentKeyBindingForJavaTest {
    private static final long TIMEOUT_MILLIS = 3_000;
    private static final long SETTLE_MILLIS = 500;
    private static final int VK_F7 = 0x76;
    private static final int SCAN_F7 = 0x41;

    /** Waits until [events] read on the UI thread becomes [expected], and confirms it does not change after waiting a little. */
    private static void awaitEvents(List<String> events, List<String> expected) throws InterruptedException {
        Callable<List<String>> snapshot = () -> new ArrayList<>(events);
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!onUiThreadGet(snapshot).equals(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        Thread.sleep(SETTLE_MILLIS);
        assertEquals(expected, onUiThreadGet(snapshot));
    }

    @Test
    void bindingCtrlSAndF7ToASaveActionWithGetInputMapAndGetActionMap_F7SavesEvenWhenSomethingElseHasTheFocus() throws InterruptedException {
        List<String> events = new ArrayList<>();
        WPanel panel = onUiThreadGet(WPanel::new);
        WButton save = onUiThreadGet(() -> new WButton("Save"));
        WTextField field = onUiThreadGet(WTextField::new);
        onUiThread(() -> {
            panel.add(save);
            panel.add(field);
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                WAbstractAction saveAction = new WAbstractAction("Save") {
                    @Override
                    public void actionPerformed(WActionEvent e) {
                        assertSame(save, e.getSource());
                        events.add("save:" + e.getActionCommand());
                    }
                };
                save.getInputMap(WComponent.WHEN_IN_FOCUSED_WINDOW).put(WKeyStroke.getKeyStroke("ctrl S"), "save");
                save.getInputMap(WComponent.WHEN_IN_FOCUSED_WINDOW).put(WKeyStroke.getKeyStroke("F7"), "save");
                save.getActionMap().put("save", saveAction);
                assertEquals("save", save.getInputMap(WComponent.WHEN_IN_FOCUSED_WINDOW).get(WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK)));
                assertSame(saveAction, save.getActionMap().get("save"));
                assertEquals(WComponent.WHEN_IN_FOCUSED_WINDOW, save.getConditionForKeyStroke(WKeyStroke.getKeyStroke("ctrl S")));
                field.requestFocusInWindow();
            });
            TestKeyboard.INSTANCE.press(VK_F7, SCAN_F7);
            awaitEvents(events, Collections.singletonList("save:null"));
        } finally {
            UiTestHarness.detach(panel);
        }
    }

    @Test
    void passingALambdaAndACommandToRegisterKeyboardAction_F7WithTheFocusDeliversAnActionEventWithThatCommand() throws InterruptedException {
        List<String> events = new ArrayList<>();
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.registerKeyboardAction(e -> events.add(e.getActionCommand()), "refresh", WKeyStroke.getKeyStroke("F7"), WComponent.WHEN_FOCUSED);
                button.requestFocusInWindow();
            });
            TestKeyboard.INSTANCE.press(VK_F7, SCAN_F7);
            awaitEvents(events, Collections.singletonList("refresh"));
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void getRegisteredKeyStrokesUnregisterKeyboardActionAndResetKeyboardActionsCanBeCalledWithTheSameNamesAsSwing() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WKeyStroke f7 = WKeyStroke.getKeyStroke("F7");
            WKeyStroke escape = WKeyStroke.getKeyStroke("ESCAPE");
            panel.registerKeyboardAction(e -> { }, f7, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            panel.getInputMap().put(escape, "cancel");
            assertArrayEquals(new WKeyStroke[] {escape, f7}, panel.getRegisteredKeyStrokes());
            panel.unregisterKeyboardAction(f7);
            assertEquals(Arrays.asList(escape), Arrays.asList(panel.getRegisteredKeyStrokes()));
            panel.resetKeyboardActions();
            assertEquals(0, panel.getRegisteredKeyStrokes().length);
            assertEquals(WComponent.UNDEFINED_CONDITION, panel.getConditionForKeyStroke(escape));
            assertThrows(IllegalArgumentException.class, () -> panel.getInputMap(3));
        });
        assertEquals(0, WComponent.WHEN_FOCUSED);
        assertEquals(1, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        assertEquals(2, WComponent.WHEN_IN_FOCUSED_WINDOW);
        assertEquals(-1, WComponent.UNDEFINED_CONDITION);
    }
}
