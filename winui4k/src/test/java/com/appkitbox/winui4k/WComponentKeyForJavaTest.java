package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same form as Component.addKeyListener, in Java as a Swing developer would write it
 * (an anonymous subclass of KeyAdapter, comparing getKeyCode with KeyEvent.VK_*, getKeyChar, consume).
 * Keys are sent with {@link TestKeyboard} to the input child window of the shared window.
 */
class WComponentKeyForJavaTest {
    private static final long TIMEOUT_MILLIS = 3_000;
    private static final long SETTLE_MILLIS = 500;

    /** Waits until [events] read on the UI thread becomes [expected], and checks it does not change after a short wait. */
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
    void anonymousKeyAdapterSubclassStartsSearchOnEnterComparedWithVkEnterAndKeyTypedGivesNewlineChar() throws InterruptedException {
        List<String> events = new ArrayList<>();
        WTextField field = onUiThreadGet(WTextField::new);
        UiTestHarness.attachAndAwaitLoaded(field);
        try {
            onUiThread(() -> {
                field.setText("Tokyo");
                field.addKeyListener(new WKeyAdapter() {
                    @Override
                    public void keyPressed(WKeyEvent e) {
                        assertSame(field, e.getSource());
                        assertEquals(WKeyEvent.KEY_PRESSED, e.getID());
                        assertEquals(WKeyEvent.CHAR_UNDEFINED, e.getKeyChar());
                        if (e.getKeyCode() == WKeyEvent.VK_ENTER) {
                            events.add("search:" + field.getText());
                        }
                    }

                    @Override
                    public void keyTyped(WKeyEvent e) {
                        if (e.getKeyChar() == '\n') {
                            events.add("typed-enter");
                        }
                    }
                });
                assertTrue(field.requestFocusInWindow());
            });
            TestKeyboard.INSTANCE.pressEnter();
            awaitEvents(events, Arrays.asList("search:Tokyo", "typed-enter"));
        } finally {
            UiTestHarness.detach(field);
        }
    }

    @Test
    void getVirtualKeyReadsWinUiKeysAndIsActionKeyDistinguishesNonCharacterKeys() throws InterruptedException {
        List<String> events = new ArrayList<>();
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.addKeyListener(new WKeyAdapter() {
                    @Override
                    public void keyReleased(WKeyEvent e) {
                        events.add(e.getVirtualKey() + ":" + e.getKeyCode() + ":" + e.isActionKey() + ":" + e.isShiftDown());
                    }
                });
                assertTrue(button.requestFocusInWindow());
            });
            TestKeyboard.INSTANCE.press(0x41, 0x1E); // A
            TestKeyboard.INSTANCE.keyDown(0x70, 0x3B); // F1
            TestKeyboard.INSTANCE.keyUp(0x70, 0x3B);
            awaitEvents(events, Arrays.asList("A:65:false:false", "F1:112:true:false"));
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void classImplementingKeyListenerCanBeAddedAndRemovedAndKeyEventCanBeCreatedManually() {
        onUiThread(() -> {
            class Recorder implements WKeyListener {
                final List<Integer> codes = new ArrayList<>();

                @Override
                public void keyTyped(WKeyEvent e) {
                }

                @Override
                public void keyPressed(WKeyEvent e) {
                    codes.add(e.getKeyCode());
                }

                @Override
                public void keyReleased(WKeyEvent e) {
                }
            }
            WTextField field = new WTextField();
            Recorder recorder = new Recorder();
            field.addKeyListener(recorder);
            assertArrayEquals(new WKeyListener[] {recorder}, field.getKeyListeners());
            WKeyEvent event = new WKeyEvent(field, WKeyEvent.KEY_PRESSED, 0L, WInputEvent.CTRL_DOWN_MASK,
                WKeyEvent.VK_S, WKeyEvent.CHAR_UNDEFINED);
            recorder.keyPressed(event);
            assertEquals(Arrays.asList(WKeyEvent.VK_S), recorder.codes);
            assertTrue(event.isControlDown());
            assertEquals(WInputEvent.CTRL_MASK, event.getModifiers());
            assertEquals(VirtualKey.NONE, event.getVirtualKey());
            assertFalse(event.isActionKey());
            field.removeKeyListener(recorder);
            assertEquals(0, field.getKeyListeners().length);
        });
        assertEquals(10, WKeyEvent.VK_ENTER);
        assertEquals(127, WKeyEvent.VK_DELETE);
        assertEquals(155, WKeyEvent.VK_INSERT);
        assertEquals('\uFFFF', WKeyEvent.CHAR_UNDEFINED);
        assertEquals(401, WKeyEvent.KEY_PRESSED);
    }
}
