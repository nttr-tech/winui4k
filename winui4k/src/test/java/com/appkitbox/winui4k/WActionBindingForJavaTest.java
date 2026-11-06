package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that bind Actions to split buttons, combo boxes and menu headings from Java in the same way as Swing's setAction / new JMenu(Action).
 */
class WActionBindingForJavaTest {
    /** An action created by passing a name and an icon, as with Swing's AbstractAction. */
    private static WAbstractAction saveAction(List<String> calls) {
        WAbstractAction action = new WAbstractAction("Save", Symbol.SAVE) {
            @Override
            public void actionPerformed(WActionEvent e) {
                calls.add(e.getActionCommand());
            }
        };
        action.putValue(WAction.SHORT_DESCRIPTION, "Save the file");
        action.putValue(WAction.ACTION_COMMAND_KEY, "save");
        return action;
    }

    @Test
    void setActionOnASplitButtonReadsBackWithGetActionAndCopiesTheNameAndIcon() {
        onUiThread(() -> {
            WAbstractAction action = saveAction(new ArrayList<>());
            WSplitButton button = new WSplitButton();
            button.setAction(action);
            assertSame(action, button.getAction());
            assertEquals("Save", button.getText());
            assertEquals(Symbol.SAVE, button.getIcon());
            assertEquals("Save the file", button.getToolTipText());
        });
    }

    @Test
    void setActionOnAComboBoxCallsTheActionOnSelectionChanges() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WComboBox combo = new WComboBox(java.util.Arrays.asList("a", "b"));
            combo.setAction(saveAction(calls));
            combo.setSelectedIndex(1);
            assertEquals(java.util.Collections.singletonList("save"), calls);
        });
    }

    @Test
    void aMenuCreatedWithNewAndAnActionGetsTheNameAndEnabledCopiedLikeJMenu() {
        onUiThread(() -> {
            WAbstractAction action = saveAction(new ArrayList<>());
            action.setEnabled(false);
            WMenuBarItem menu = new WMenuBarItem(action);
            WMenuFlyoutSubItem sub = new WMenuFlyoutSubItem(action);
            assertEquals("Save", menu.getTitle());
            assertFalse(menu.isEnabled());
            assertEquals("Save", sub.getText());
            assertEquals(Symbol.SAVE, sub.getIcon());
            sub.setAction(null);
            assertNull(sub.getAction());
            assertEquals("", sub.getText());
        });
    }
}
