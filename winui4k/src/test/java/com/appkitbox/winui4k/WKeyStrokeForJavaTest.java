package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shape as KeyStroke from Java as a Swing developer would write it
 * (KeyStroke.getKeyStroke static methods, KeyEvent.VK_* and InputEvent.CTRL_DOWN_MASK, Action.ACCELERATOR_KEY).
 * The expected values are the actual values of Swing's KeyStroke (the output of tools/swing_probes/KeyStrokeProbe.java).
 */
class WKeyStrokeForJavaTest {
    @Test
    @SuppressWarnings("deprecation") // Checks that the old-style CTRL_MASK is also accepted, as in Swing
    void getKeyStrokeWithVkSAndCtrlDownMaskGivesCtrlPressedSEqualToTheOldCtrlMaskAndStringForms() {
        WKeyStroke ctrlS = WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WKeyEvent.CTRL_DOWN_MASK);
        assertEquals("ctrl pressed S", ctrlS.toString());
        assertEquals(WKeyEvent.VK_S, ctrlS.getKeyCode());
        assertEquals(WInputEvent.CTRL_DOWN_MASK | WInputEvent.CTRL_MASK, ctrlS.getModifiers());
        assertEquals(WKeyEvent.CHAR_UNDEFINED, ctrlS.getKeyChar());
        assertFalse(ctrlS.isOnKeyRelease());
        assertEquals(WKeyEvent.KEY_PRESSED, ctrlS.getKeyEventType());
        assertEquals(ctrlS, WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_MASK));
        assertEquals(ctrlS, WKeyStroke.getKeyStroke("ctrl S"));
        assertEquals(ctrlS, WKeyStroke.getKeyStroke("control pressed S"));
        assertEquals(1442316290, ctrlS.hashCode());
    }

    @Test
    void stringFormReadsTheSameFormatAsSwingAndReturnsNullForErrorsEmptyAndNull() {
        assertEquals("shift ctrl pressed S", WKeyStroke.getKeyStroke("ctrl shift S").toString());
        assertEquals("alt pressed F4", WKeyStroke.getKeyStroke("alt F4").toString());
        assertEquals("meta pressed W", WKeyStroke.getKeyStroke("meta W").toString());
        assertEquals("released ENTER", WKeyStroke.getKeyStroke("released ENTER").toString());
        assertEquals("shift typed a", WKeyStroke.getKeyStroke("shift typed a").toString());
        assertNull(WKeyStroke.getKeyStroke("ctrl s"));
        assertNull(WKeyStroke.getKeyStroke("CTRL S"));
        assertNull(WKeyStroke.getKeyStroke("ctrl"));
        assertNull(WKeyStroke.getKeyStroke(""));
        assertNull(WKeyStroke.getKeyStroke((String) null));
    }

    @Test
    void characterGetKeyStrokeIsTypedAndPassingCharAndInt0GivesKeyCode97Numpad1AsInSwing() {
        WKeyStroke typed = WKeyStroke.getKeyStroke('a');
        assertEquals("typed a", typed.toString());
        assertEquals(WKeyEvent.VK_UNDEFINED, typed.getKeyCode());
        assertEquals('a', typed.getKeyChar());
        assertEquals(WKeyEvent.KEY_TYPED, typed.getKeyEventType());

        WKeyStroke numpad1 = WKeyStroke.getKeyStroke('a', 0);
        assertEquals("pressed NUMPAD1", numpad1.toString());
        assertEquals(WKeyEvent.VK_NUMPAD1, numpad1.getKeyCode());
    }

    @Test
    void passingACharacterAndModifiersToGetKeyStrokeGivesAKeystrokeForTheCharacterWithModifiersAndNullThrows() {
        WKeyStroke ctrlA = WKeyStroke.getKeyStroke(Character.valueOf('a'), WInputEvent.CTRL_DOWN_MASK);
        assertEquals("ctrl typed a", ctrlA.toString());
        assertEquals('a', ctrlA.getKeyChar());
        assertEquals(WKeyEvent.VK_UNDEFINED, ctrlA.getKeyCode());
        assertEquals(WKeyEvent.KEY_TYPED, ctrlA.getKeyEventType());
        assertEquals(WKeyStroke.getKeyStroke("ctrl typed a"), ctrlA);
        assertThrows(IllegalArgumentException.class, () -> WKeyStroke.getKeyStroke((Character) null, 0));
    }

    @Test
    void getKeyStrokeForEventCreatesAKeystrokeFromTheEventAKeyListenerReceivedEqualToTheStringForm() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            WKeyEvent pressed = new WKeyEvent(panel, WKeyEvent.KEY_PRESSED, 0L, WInputEvent.CTRL_DOWN_MASK, WKeyEvent.VK_S,
                    WKeyEvent.CHAR_UNDEFINED);
            WKeyEvent typed = new WKeyEvent(panel, WKeyEvent.KEY_TYPED, 0L, WInputEvent.SHIFT_DOWN_MASK, WKeyEvent.VK_UNDEFINED, 'A');
            assertEquals(WKeyStroke.getKeyStroke("ctrl S"), WKeyStroke.getKeyStrokeForEvent(pressed));
            assertEquals(WKeyStroke.getKeyStroke("shift typed A"), WKeyStroke.getKeyStrokeForEvent(typed));
        });
    }

    @Test
    void releaseKeyStrokeIsReleasedAndCanBeUsedLikeAltF4() {
        WKeyStroke stroke = WKeyStroke.getKeyStroke(WKeyEvent.VK_F4, WKeyEvent.ALT_DOWN_MASK, true);
        assertEquals("alt released F4", stroke.toString());
        assertTrue(stroke.isOnKeyRelease());
        assertEquals(WKeyEvent.KEY_RELEASED, stroke.getKeyEventType());
        assertNotEquals(WKeyStroke.getKeyStroke(WKeyEvent.VK_F4, WKeyEvent.ALT_DOWN_MASK), stroke);
    }

    @Test
    void keyStrokeCanBePutIntoActionAcceleratorKeyAndReadBack() {
        WAction save = new WAbstractAction("Save") {
            @Override
            public void actionPerformed(WActionEvent e) {
                // Not used
            }
        };
        save.putValue(WAction.ACCELERATOR_KEY, WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WKeyEvent.CTRL_DOWN_MASK));
        save.putValue(WAction.LONG_DESCRIPTION, "Save the file");
        assertEquals(WKeyStroke.getKeyStroke("ctrl S"), save.getValue(WAction.ACCELERATOR_KEY));
        assertEquals("AcceleratorKey", WAction.ACCELERATOR_KEY);
        assertEquals("LongDescription", WAction.LONG_DESCRIPTION);
        assertEquals("SwingLargeIconKey", WAction.LARGE_ICON_KEY);
        assertEquals("SwingDisplayedMnemonicIndexKey", WAction.DISPLAYED_MNEMONIC_INDEX_KEY);
    }

    @Test
    void addKeyboardAcceleratorWithKeyStrokeAndLambdaIsCalledOnF5AndReleaseKeyStrokeThrows() throws InterruptedException {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        WPanel panel = onUiThreadGet(WPanel::new);
        WButton refresh = onUiThreadGet(() -> new WButton("Refresh"));
        onUiThread(() -> {
            panel.add(refresh);
            refresh.addKeyboardAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0), () -> events.add("refresh"));
            WKeyStroke released = WKeyStroke.getKeyStroke(WKeyEvent.VK_F6, 0, true);
            assertThrows(IllegalArgumentException.class, () -> refresh.addKeyboardAccelerator(released));
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> assertTrue(refresh.requestFocusInWindow()));
            TestKeyboard.INSTANCE.press(0x74, 0x3F); // F5
            long deadline = System.currentTimeMillis() + 3_000;
            while (events.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(Collections.singletonList("refresh"), new ArrayList<>(events));
        } finally {
            UiTestHarness.detach(panel);
        }
    }
}
