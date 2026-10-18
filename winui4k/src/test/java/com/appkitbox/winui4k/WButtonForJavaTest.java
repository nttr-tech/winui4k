package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WButton from Java, written the way developers used to Swing's JButton would write them.
 * Compiling at all verifies the shape of the API; the test bodies check the resulting behavior of the calls.
 * Clicks are triggered with {@code doClick()} (UI Automation's Invoke).
 */
class WButtonForJavaTest {
    private int plainCalls;
    private final List<WActionEvent> received = new ArrayList<>();

    private void onClick() {
        plainCalls++;
    }

    private void onAction(WActionEvent e) {
        received.add(e);
    }

    // ---- Creation and text ----

    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WButton().getText());
            assertEquals("OK", new WButton("OK").getText());
        });
    }

    @Test
    void setTextReplacesTheLabel() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            button.setText("Cancel");
            assertEquals("Cancel", button.getText());
        });
    }

    // ---- ActionListener ----

    @Test
    void aNoArgLambdaListenerIsCalledOncePerDoClick() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            assertEquals(0, count[0]);
            button.doClick();
            assertEquals(1, count[0]);
            button.doClick();
            assertEquals(2, count[0]);
        });
    }

    @Test
    void aLambdaListenerTakingEReceivesAnEventWhoseSourceIsTheButtonItself() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            List<WActionEvent> events = new ArrayList<>();
            button.addActionListener(e -> events.add(e));
            button.doClick();
            assertEquals(1, events.size());
            assertSame(button, events.get(0).getSource());
        });
    }

    @Test
    void methodReferencesWorkForBothNoArgMethodsAndMethodsTakingWActionEvent() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            button.addActionListener(this::onClick);
            button.addActionListener(this::onAction);
            button.doClick();
            assertEquals(1, plainCalls);
            assertEquals(1, received.size());
            assertSame(button, received.get(0).getSource());
        });
    }

    @Test
    void anonymousRunnableAndWActionListenerClassesCanAlsoBeRegistered() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            List<String> log = new ArrayList<>();
            button.addActionListener(new Runnable() {
                @Override
                public void run() {
                    log.add("runnable");
                }
            });
            button.addActionListener(new WActionListener() {
                @Override
                public void actionPerformed(WActionEvent e) {
                    log.add("listener:" + e.getActionCommand());
                }
            });
            button.doClick();
            assertEquals(2, log.size());
            assertTrue(log.contains("runnable"));
            assertTrue(log.contains("listener:OK"));
        });
    }

    @Test
    void aRunnableRemovedWithRemoveActionListenerIsNotCalledAndOnlyTheRemainingListenersAre() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            int[] counts = {0, 0};
            Runnable removed = () -> counts[0]++;
            Runnable kept = () -> counts[1]++;
            button.addActionListener(removed);
            button.addActionListener(kept);
            button.removeActionListener(removed);
            button.doClick();
            assertEquals(0, counts[0]);
            assertEquals(1, counts[1]);
        });
    }

    @Test
    void aWActionListenerRemovedWithRemoveActionListenerIsNotCalled() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            int[] counts = {0, 0};
            WActionListener removed = e -> counts[0]++;
            WActionListener kept = e -> counts[1]++;
            button.addActionListener(removed);
            button.addActionListener(kept);
            button.removeActionListener(removed);
            button.doClick();
            assertEquals(0, counts[0]);
            assertEquals(1, counts[1]);
        });
    }

    @Test
    void removingAnUnregisteredListenerDoesNotThrowOrAffectOtherListeners() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.removeActionListener(() -> { });
            button.removeActionListener(e -> { });
            button.doClick();
            assertEquals(1, count[0]);
        });
    }

    @Test
    void doClickOnADisabledButtonDoesNothingAndItCanBeClickedAgainOnceReEnabled() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.setEnabled(false);
            assertFalse(button.isEnabled());
            button.doClick();
            assertEquals(0, count[0]);
            button.setEnabled(true);
            assertTrue(button.isEnabled());
            button.doClick();
            assertEquals(1, count[0]);
        });
    }

    // ---- actionCommand and WActionEvent ----

    @Test
    void eventActionCommandIsTheButtonTextWhenUnsetAndTheSetValueOtherwise() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            List<String> commands = new ArrayList<>();
            button.addActionListener(e -> commands.add(e.getActionCommand()));
            button.doClick();
            button.setActionCommand("save-command");
            button.doClick();
            button.setActionCommand(null);
            button.setText("Save As");
            button.doClick();
            assertEquals(Arrays.asList("Save", "save-command", "Save As"), commands);
        });
    }

    @Test
    void getActionCommandReturnsGetTextWhenUnsetAndSettingNullUnsetsIt() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            assertEquals("Save", button.getActionCommand());
            button.setActionCommand("save");
            assertEquals("save", button.getActionCommand());
            button.setActionCommand(null);
            assertEquals("Save", button.getActionCommand());
            button.setText("Close");
            assertEquals("Close", button.getActionCommand());
        });
    }

    @Test
    void doClickWithNoKeysPressedHasZeroModifiersAndWhenIsTheClickTime() {
        long before = System.currentTimeMillis();
        onUiThread(() -> {
            WButton button = new WButton("OK");
            List<WActionEvent> events = new ArrayList<>();
            button.addActionListener(e -> events.add(e));
            button.doClick();
            WActionEvent event = events.get(0);
            assertEquals(0, event.getModifiers());
            assertTrue(event.getWhen() >= before);
            assertTrue(event.getWhen() <= System.currentTimeMillis());
        });
    }

    @Test
    void modifierKeyConstantsHaveTheSameValuesAsAwtActionEvent() {
        assertEquals(1, WActionEvent.SHIFT_MASK);
        assertEquals(2, WActionEvent.CTRL_MASK);
        assertEquals(4, WActionEvent.META_MASK);
        assertEquals(8, WActionEvent.ALT_MASK);
        assertEquals(java.awt.event.ActionEvent.SHIFT_MASK, WActionEvent.SHIFT_MASK);
        assertEquals(java.awt.event.ActionEvent.CTRL_MASK, WActionEvent.CTRL_MASK);
        assertEquals(java.awt.event.ActionEvent.META_MASK, WActionEvent.META_MASK);
        assertEquals(java.awt.event.ActionEvent.ALT_MASK, WActionEvent.ALT_MASK);
    }

    @Test
    void wActionEventCanBeCreatedWithSourceActionCommandAndModifiers() {
        Object source = new Object();
        WActionEvent plain = new WActionEvent(source, "cmd");
        assertSame(source, plain.getSource());
        assertEquals("cmd", plain.getActionCommand());
        assertEquals(0, plain.getModifiers());

        WActionEvent withKeys = new WActionEvent(source, "cmd", WActionEvent.CTRL_MASK | WActionEvent.SHIFT_MASK);
        assertEquals(3, withKeys.getModifiers());
        assertNull(new WActionEvent(source, null).getActionCommand());
    }

    @Test
    void wActionEventCannotBeCreatedWithANullSource() {
        // java.awt.event.ActionEvent also rejects a null source, following EventObject
        assertThrows(RuntimeException.class, () -> new WActionEvent(null, "cmd"));
    }

    // ---- mnemonic ----

    @Test
    void setMnemonicWithACharIsReadBackAsTheUppercaseKeyCode() {
        onUiThread(() -> {
            WButton button = new WButton("Open");
            assertEquals(0, button.getMnemonic());
            button.setMnemonic('O');
            assertEquals(KeyEvent.VK_O, button.getMnemonic());
            button.setMnemonic('s');
            assertEquals(KeyEvent.VK_S, button.getMnemonic());
        });
    }

    @Test
    void setMnemonicAcceptsKeyEventVkConstants() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            button.setMnemonic(KeyEvent.VK_S);
            assertEquals(83, button.getMnemonic());
            button.setMnemonic(KeyEvent.VK_5);
            assertEquals(53, button.getMnemonic());
        });
    }

    @Test
    void setMnemonicWithZeroClearsItAndNonAlphanumericValuesThrowIllegalArgumentException() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            button.setMnemonic('S');
            button.setMnemonic(0);
            assertEquals(0, button.getMnemonic());

            button.setMnemonic('S');
            assertThrows(IllegalArgumentException.class, () -> button.setMnemonic('!'));
            assertThrows(IllegalArgumentException.class, () -> button.setMnemonic(KeyEvent.VK_SPACE));
            assertEquals(KeyEvent.VK_S, button.getMnemonic());
        });
    }

    // ---- Fonts (colors and appearance toggles are in WButtonColorForJavaTest / WButtonAppearanceForJavaTest) ----

    @Test
    void setFontWithAWFontReadsBackBoldWithGetFont() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Bold"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                button.setFont(new WFont("Segoe UI", WFont.BOLD, 14));
                WFont font = button.getFont();
                assertEquals("Segoe UI", font.getName());
                assertTrue(font.isBold());
                assertFalse(font.isItalic());
                assertEquals(14, font.getSize());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void setFontWithNullRestoresTheThemeDefaultFont() {
        WButton button = UiTestHarness.onUiThreadGet(() -> new WButton("Default"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                WFont before = button.getFont();
                button.setFont(new WFont("Consolas", WFont.ITALIC, 20));
                assertNotEquals(before, button.getFont());
                assertTrue(button.getFont().isItalic());
                button.setFont(null);
                assertEquals(before, button.getFont());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
