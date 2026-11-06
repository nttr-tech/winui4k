package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that bind an Action to a WTextField like JTextField.setAction, in the form a Swing developer writes in Java. */
class WTextFieldSetActionForJavaTest {
    @Test
    void anAnonymousAbstractActionBoundWithSetActionIsCalledByPostActionEvent_getActionReadsIt() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("query");
            List<String> commands = new ArrayList<>();
            WAbstractAction search = new WAbstractAction("Search") {
                @Override
                public void actionPerformed(WActionEvent e) {
                    commands.add(e.getActionCommand());
                }
            };
            search.putValue(WAction.SHORT_DESCRIPTION, "Press Enter to search");
            field.setAction(search);

            assertSame(search, field.getAction());
            assertEquals("Press Enter to search", field.getToolTipText());
            field.postActionEvent();
            assertEquals(1, commands.size());
            assertEquals("query", commands.get(0));

            search.setEnabled(false);
            assertFalse(field.isEnabled());
            field.setAction(null);
            assertNull(field.getAction());
            assertNull(field.getToolTipText());
        });
    }
}
