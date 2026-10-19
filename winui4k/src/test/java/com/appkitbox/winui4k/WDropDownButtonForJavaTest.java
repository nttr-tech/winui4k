package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests using WDropDownButton from Java as a member of the JButton family (WButton).
 * How colors take effect in the template is checked in WDropDownButtonTest.
 */
class WDropDownButtonForJavaTest {
    @Test
    void canBeStoredInWButtonVariableAndUsedLikeJButton() {
        onUiThread(() -> {
            WButton button = new WDropDownButton("Other");
            button.setIcon(Symbol.MORE);
            button.setBackground(new WColor(30, 90, 30));
            button.setForeground(new WColor(255, 255, 255));
            assertEquals("Other", button.getText());
            assertSame(Symbol.MORE, button.getIcon());
            assertEquals(new WColor(30, 90, 30), button.getBackground());
            assertEquals(new WColor(255, 255, 255), button.getForeground());
        });
    }

    @Test
    void setFlyoutAndGetFlyoutHoldDropDownMenu() {
        onUiThread(() -> {
            WDropDownButton button = new WDropDownButton("Sort");
            WMenuFlyout menu = new WMenuFlyout();
            button.setFlyout(menu);
            assertSame(menu, button.getFlyout());
        });
    }

    @Test
    void bindingAnonymousClassActionIsCalledOnClick() {
        onUiThread(() -> {
            List<String> commands = new ArrayList<>();
            WDropDownButton button = new WDropDownButton();
            button.setAction(new WAbstractAction("Share", Symbol.SHARE) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    commands.add(e.getActionCommand());
                }
            });
            button.doClick();
            assertEquals("Share", button.getText());
            assertEquals(1, commands.size());
            assertEquals("Share", commands.get(0));
        });
    }
}
