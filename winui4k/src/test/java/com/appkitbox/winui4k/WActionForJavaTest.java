package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WAbstractAction and WButton(Action) from Java, written the way developers used to Swing's
 * AbstractAction / JButton(Action) would write them. Compiling at all verifies the shape of the API; the test bodies
 * check value synchronization and the results of clicks. Clicks are triggered with {@code doClick()}.
 */
class WActionForJavaTest {
    private final List<WActionEvent> performed = new ArrayList<>();

    /** The shape of writing new AbstractAction("Save", icon) { ... } in Swing. */
    private WAbstractAction saveAction() {
        return new WAbstractAction("Save", Symbol.SAVE) {
            @Override
            public void actionPerformed(WActionEvent e) {
                performed.add(e);
            }
        };
    }

    @Test
    void keyConstantsAreReadableAsStaticFieldsWithTheSameStringsAsSwingAction() {
        assertEquals("Name", WAction.NAME);
        assertEquals("SmallIcon", WAction.SMALL_ICON);
        assertEquals("ShortDescription", WAction.SHORT_DESCRIPTION);
        assertEquals("MnemonicKey", WAction.MNEMONIC_KEY);
        assertEquals("ActionCommandKey", WAction.ACTION_COMMAND_KEY);
    }

    @Test
    void wAbstractActionCanBeCreatedWithNoArgumentsANameOrANameAndIcon() {
        WAbstractAction empty = new WAbstractAction() {
            @Override
            public void actionPerformed(WActionEvent e) {
            }
        };
        WAbstractAction named = new WAbstractAction("Save") {
            @Override
            public void actionPerformed(WActionEvent e) {
            }
        };
        assertNull(empty.getValue(WAction.NAME));
        assertEquals("Save", named.getValue(WAction.NAME));
        assertNull(named.getValue(WAction.SMALL_ICON));
        assertEquals(Symbol.SAVE, saveAction().getValue(WAction.SMALL_ICON));
        assertTrue(empty.isEnabled());
    }

    @Test
    void newWButtonWithActionCopiesTheNameAndIconToTheButton() {
        onUiThread(() -> {
            WButton button = new WButton(saveAction());
            assertEquals("Save", button.getText());
            assertSame(Symbol.SAVE, button.getIcon());
            assertTrue(button.isEnabled());
        });
    }

    @Test
    void changingValuesWithPutValueIsReflectedInTheButtonGetters() {
        onUiThread(() -> {
            WAbstractAction action = saveAction();
            WButton button = new WButton(action);

            action.putValue(WAction.NAME, "Overwrite");
            action.putValue(WAction.SHORT_DESCRIPTION, "Overwrites the file");
            action.putValue(WAction.MNEMONIC_KEY, KeyEvent.VK_S);
            action.putValue(WAction.ACTION_COMMAND_KEY, "save");

            assertEquals("Overwrite", button.getText());
            assertEquals("Overwrites the file", button.getToolTipText());
            assertEquals(KeyEvent.VK_S, button.getMnemonic());
            assertEquals("save", button.getActionCommand());
        });
    }

    @Test
    void lowercaseCharForMnemonicKeyIsReadAsTheUppercaseKeyCode() {
        onUiThread(() -> {
            WAbstractAction action = saveAction();
            WButton button = new WButton(action);

            action.putValue(WAction.MNEMONIC_KEY, 's');
            assertEquals(KeyEvent.VK_S, button.getMnemonic());
        });
    }

    @Test
    void setEnabledFalseDisablesTheButtonAndDoClickDoesNotCallActionPerformed() {
        onUiThread(() -> {
            WAbstractAction action = saveAction();
            WButton button = new WButton(action);

            action.setEnabled(false);
            assertFalse(button.isEnabled());
            button.doClick();
            assertEquals(0, performed.size());

            action.setEnabled(true);
            assertTrue(button.isEnabled());
            button.doClick();
            assertEquals(1, performed.size());
        });
    }

    @Test
    void clickingCallsActionPerformedWithTheButtonAsSourceAndTheTextAsActionCommand() {
        onUiThread(() -> {
            WButton button = new WButton(saveAction());
            button.doClick();
            assertEquals(1, performed.size());
            assertSame(button, performed.get(0).getSource());
            assertEquals("Save", performed.get(0).getActionCommand());
        });
    }

    @Test
    void afterSetActionReplacesTheActionThePreviousActionIsNotCalled() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WAbstractAction first = new WAbstractAction("First") {
                @Override
                public void actionPerformed(WActionEvent e) {
                    calls.add("first");
                }
            };
            WAbstractAction second = new WAbstractAction("Second") {
                @Override
                public void actionPerformed(WActionEvent e) {
                    calls.add("second");
                }
            };
            WButton button = new WButton(first);
            button.setAction(second);
            assertSame(second, button.getAction());
            assertEquals("Second", button.getText());

            first.putValue(WAction.NAME, "First (changed)");
            assertEquals("Second", button.getText());
            button.doClick();
            assertEquals(Arrays.asList("second"), calls);
        });
    }

    @Test
    void setActionWithNullResetsTheButtonPropertiesAndClickingDoesNotCallTheAction() {
        onUiThread(() -> {
            WAbstractAction action = saveAction();
            action.putValue(WAction.SHORT_DESCRIPTION, "Hint");
            WButton button = new WButton(action);
            button.setAction(null);

            assertNull(button.getAction());
            assertEquals("", button.getText());
            assertNull(button.getIcon());
            assertNull(button.getToolTipText());
            assertEquals(0, button.getMnemonic());
            button.doClick();
            assertEquals(0, performed.size());
        });
    }

    @Test
    void changeListenersCanBeLambdasAndReceiveTheChangedKey() {
        WAbstractAction action = saveAction();
        List<String> keys = new ArrayList<>();
        WActionChangeListener listener = (a, key) -> keys.add(key);
        action.addPropertyChangeListener(listener);
        action.putValue(WAction.NAME, "Open");
        action.setEnabled(false);
        action.removePropertyChangeListener(listener);
        action.putValue(WAction.NAME, "Close");
        assertEquals(Arrays.asList(WAction.NAME, WAction.ENABLED), keys);
    }
}
