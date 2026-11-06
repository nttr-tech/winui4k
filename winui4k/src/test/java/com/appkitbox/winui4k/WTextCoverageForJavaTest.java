package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests that use from Java the public members of the text classes that had no Java tests (WLabel text decorations, headers, the mask character,
 * TextChanged listeners of WTextField / WTextPane, RTF and undo of WTextPane).
 */
class WTextCoverageForJavaTest {
    @Test
    void wLabelFontSizeFamilyItalicUnderlineStrikethroughTrimmingAndAlignmentCanBeSet() {
        onUiThread(() -> {
            WLabel label = new WLabel("Heading");
            label.setFontSize(20.0);
            label.setFontFamily("Consolas");
            label.setItalic(true);
            label.setUnderline(true);
            label.setStrikethrough(true);
            label.setTextTrimming(TextTrimming.WORD_ELLIPSIS);
            label.setTextAlignment(TextAlignment.CENTER);
            label.setTextWrapping(TextWrapping.WRAP);
            label.setTextSelectionEnabled(true);
            assertEquals(20.0, label.getFontSize());
            assertEquals("Consolas", label.getFontFamily());
            assertTrue(label.isItalic());
            assertTrue(label.isUnderline());
            assertTrue(label.isStrikethrough());
            assertEquals(TextTrimming.WORD_ELLIPSIS, label.getTextTrimming());
            assertEquals(TextAlignment.CENTER, label.getTextAlignment());
            assertEquals(TextWrapping.WRAP, label.getTextWrapping());
            assertTrue(label.isTextSelectionEnabled());
        });
    }

    @Test
    void inputFieldHeadersAlignmentAndTheMaskCharacterCanBeSet() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setHeader("Name");
            field.setTextAlignment(TextAlignment.RIGHT);
            assertEquals("Name", field.getHeader());
            assertEquals(TextAlignment.RIGHT, field.getTextAlignment());

            WPasswordField password = new WPasswordField();
            password.setHeader("Password");
            password.setPasswordChar("*");
            assertEquals("Password", password.getHeader());
            assertEquals("*", password.getPasswordChar());

            WTextPane pane = new WTextPane();
            pane.setHeader("Body");
            assertEquals("Body", pane.getHeader());

            WAutoSuggestBox box = new WAutoSuggestBox();
            box.setHeader("Search");
            assertEquals("Search", box.getHeader());
            assertFalse(box.isSuggestionListOpen());
        });
    }

    @Test
    void wTextPaneCanReadAndWriteWithFormattingViaRtfAndUndo() {
        onUiThread(() -> {
            WTextPane pane = new WTextPane();
            pane.setText("Make it bold");
            pane.selectAll();
            pane.toggleSelectionBold();
            String rtf = pane.getRtfText();
            assertTrue(rtf.contains("\\b"), rtf);

            WTextPane copy = new WTextPane();
            copy.setRtfText(rtf);
            assertEquals("Make it bold", copy.getText());

            pane.replaceSelection("Replacement");
            assertTrue(pane.getCanUndo());
            pane.undo();
            assertEquals("Make it bold", pane.getText());
            assertTrue(pane.getCanRedo());
            pane.redo();
            assertEquals("Replacement", pane.getText());
        });
    }

    @Test
    void wTextFieldTextChangedListenersCanBeAddedAndRemovedWithAConsumer() throws InterruptedException {
        LinkedBlockingQueue<String> removed = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<String> monitor = new LinkedBlockingQueue<>();
        Consumer<String> listener = removed::add;
        WTextField field = UiTestHarness.onUiThreadGet(() -> {
            WTextField f = new WTextField();
            f.addTextChangedListener(listener);
            f.addTextChangedListener(monitor::add);
            return f;
        });
        UiTestHarness.attachAndAwaitLoaded(field);
        try {
            onUiThread(() -> field.setText("One"));
            assertEquals("One", monitor.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            onUiThread(() -> {
                field.removeTextChangedListener(listener);
                field.setText("Two");
            });
            assertEquals("Two", monitor.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals(1, removed.size());
        } finally {
            UiTestHarness.detach(field);
        }
    }

    @Test
    void wTextPaneTextChangedListenersCanBeAddedAndRemovedWithARunnable() throws InterruptedException {
        LinkedBlockingQueue<String> removed = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<String> monitor = new LinkedBlockingQueue<>();
        Runnable listener = () -> removed.add("removed");
        WTextPane pane = UiTestHarness.onUiThreadGet(() -> {
            WTextPane p = new WTextPane();
            p.addTextChangedListener(listener);
            p.addTextChangedListener(() -> monitor.add(p.getText()));
            return p;
        });
        UiTestHarness.attachAndAwaitLoaded(pane);
        try {
            // A notification of the empty string right after loading may arrive first, so wait until the expected string arrives
            onUiThread(() -> pane.setText("One"));
            assertTrue(awaitValue(monitor, "One"));
            int removedBefore = UiTestHarness.onUiThreadGet(() -> {
                pane.removeTextChangedListener(listener);
                pane.setText("Two");
                return removed.size();
            });
            assertTrue(awaitValue(monitor, "Two"));
            assertTrue(removedBefore >= 1);
            assertEquals(removedBefore, removed.size());
        } finally {
            UiTestHarness.detach(pane);
        }
    }

    private static boolean awaitValue(LinkedBlockingQueue<String> queue, String expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UiTestHarness.TIMEOUT_SECONDS);
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            if (expected.equals(queue.poll(remaining, TimeUnit.NANOSECONDS))) {
                return true;
            }
        }
    }
}
