package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WSplitButton the way developers used to Swing's JButton (and menu buttons with an arrow) write it in Java.
 * check value synchronization and the results of clicks. Clicks are triggered with {@code doClick()}.
 */
class WSplitButtonForJavaTest {
    /**
     * A SplitButton has no real template (the main Button) until it is placed in a window, and until then doClick cannot
     * raise Click, so tests that check doClick place the button in the shared window, wait for Loaded, and then run
     * action on the UI thread.
     */
    private static void whileShown(WSplitButton button, Runnable action) {
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(action);
        } finally {
            UiTestHarness.detach(button);
        }
    }

    private static WSplitButton create(String text) {
        return UiTestHarness.onUiThreadGet(() -> new WSplitButton(text));
    }

    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WSplitButton().getText());
            assertEquals("Split", new WSplitButton("Split").getText());
        });
    }

    @Test
    void setTextAndSetContentChangeTheDisplayTheSameWayAsJButton() {
        onUiThread(() -> {
            WSplitButton button = new WSplitButton("Paste");
            button.setText("Paste(P)");
            assertEquals("Paste(P)", button.getText());
            WLabel label = new WLabel("Content");
            button.setContent(label);
            assertSame(label, button.getContent());
            assertEquals("", button.getText());
        });
    }

    @Test
    void aNoArgLambdaListenerIsCalledOncePerDoClick() {
        WSplitButton button = create("Paste");
        whileShown(button, () -> {
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.doClick();
            assertEquals(1, count[0]);
            button.doClick();
            assertEquals(2, count[0]);
        });
    }

    @Test
    void aLambdaListenerTakingEReceivesAnEventWhoseSourceIsTheButtonItself() {
        WSplitButton button = create("Paste");
        whileShown(button, () -> {
            List<WActionEvent> events = new ArrayList<>();
            button.addActionListener(e -> events.add(e));
            button.doClick();
            assertEquals(1, events.size());
            assertSame(button, events.get(0).getSource());
            assertEquals("Paste", events.get(0).getActionCommand());
        });
    }

    @Test
    void anonymousClassWActionListenerIsNotCalledAfterItIsRemoved() {
        WSplitButton button = create("Paste");
        whileShown(button, () -> {
            List<String> commands = new ArrayList<>();
            WActionListener listener = new WActionListener() {
                @Override
                public void actionPerformed(WActionEvent e) {
                    commands.add(e.getActionCommand());
                }
            };
            button.addActionListener(listener);
            button.setActionCommand("paste");
            button.doClick();
            button.removeActionListener(listener);
            button.doClick();
            assertEquals(Arrays.asList("paste"), commands);
        });
    }

    @Test
    void runnableListenersCanAlsoBeAddedAndRemoved() {
        WSplitButton button = create("Paste");
        whileShown(button, () -> {
            int[] count = {0};
            Runnable listener = () -> count[0]++;
            button.addActionListener(listener);
            button.doClick();
            button.removeActionListener(listener);
            button.doClick();
            assertEquals(1, count[0]);
        });
    }

    @Test
    void getActionCommandIsTheTextWhenUnsetAndCanBeChangedBySetActionCommand() {
        onUiThread(() -> {
            WSplitButton button = new WSplitButton("Paste");
            assertEquals("Paste", button.getActionCommand());
            button.setActionCommand("paste");
            assertEquals("paste", button.getActionCommand());
            button.setActionCommand(null);
            assertEquals("Paste", button.getActionCommand());
        });
    }

    @Test
    void setMnemonicAcceptsACharAndKeyEventVkConstants() {
        onUiThread(() -> {
            WSplitButton button = new WSplitButton("Paste");
            assertEquals(0, button.getMnemonic());
            button.setMnemonic('p');
            assertEquals(KeyEvent.VK_P, button.getMnemonic());
            button.setMnemonic(KeyEvent.VK_5);
            assertEquals(KeyEvent.VK_5, button.getMnemonic());
            button.setMnemonic(0);
            assertEquals(0, button.getMnemonic());
            assertThrows(IllegalArgumentException.class, () -> button.setMnemonic(KeyEvent.VK_SPACE));
        });
    }

    @Test
    void iconSetBySetIconNextToTheTextCanBeReadBackByGetIcon() {
        onUiThread(() -> {
            WSplitButton button = new WSplitButton("Paste");
            assertNull(button.getIcon());
            button.setIcon(Symbol.PASTE);
            assertSame(Symbol.PASTE, button.getIcon());
            assertEquals("Paste", button.getText());
            button.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
            button.setVerticalTextPosition(VerticalTextPosition.BOTTOM);
            button.setIconTextGap(8.0);
            assertEquals(HorizontalTextPosition.CENTER, button.getHorizontalTextPosition());
            assertEquals(VerticalTextPosition.BOTTOM, button.getVerticalTextPosition());
            assertEquals(8.0, button.getIconTextGap());
            button.setIcon(null);
            assertNull(button.getIcon());
        });
    }

    @Test
    void setCommandAndSetCommandParameterBindACommandToTheClick() {
        WSplitButton button = create("Paste");
        whileShown(button, () -> {
            List<String> received = new ArrayList<>();
            button.setCommand(new WCommand(received::add));
            button.setCommandParameter("Argument");
            assertEquals("Argument", button.getCommandParameter());
            button.doClick();
            assertEquals(Arrays.asList("Argument"), received);
        });
    }

    @Test
    void doClickOnADisabledButtonDoesNothing() {
        WSplitButton button = create("Paste");
        whileShown(button, () -> {
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.setEnabled(false);
            button.doClick();
            assertEquals(0, count[0]);
        });
    }
}
