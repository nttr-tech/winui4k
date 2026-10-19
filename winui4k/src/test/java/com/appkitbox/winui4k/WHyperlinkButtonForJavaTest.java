package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for using WHyperlinkButton from Java as a button that looks like a link.
 * doClick on a button with navigateUri set would launch the default browser, so doClick is only done on buttons without
 * navigateUri.
 */
class WHyperlinkButtonForJavaTest {
    @Test
    void canBeCreatedWithTextOnlyOrWithTextAndUri() {
        onUiThread(() -> {
            assertEquals("", new WHyperlinkButton().getText());
            WHyperlinkButton help = new WHyperlinkButton("Help");
            assertEquals("Help", help.getText());
            assertEquals("", help.getNavigateUri());
            WHyperlinkButton docs = new WHyperlinkButton("Documents", "https://example.com/docs");
            assertEquals("Documents", docs.getText());
            assertEquals("https://example.com/docs", docs.getNavigateUri());
        });
    }

    @Test
    void setNavigateUriSetsItAndEmptyStringClearsIt() {
        onUiThread(() -> {
            WHyperlinkButton button = new WHyperlinkButton("Links");
            button.setNavigateUri("https://example.org/page?x=1");
            assertEquals("https://example.org/page?x=1", button.getNavigateUri());
            button.setNavigateUri("");
            assertEquals("", button.getNavigateUri());
        });
    }

    @Test
    void eachDoClickOnLinkWithoutUriCallsActionListenerOnce() {
        onUiThread(() -> {
            WHyperlinkButton button = new WHyperlinkButton("Help");
            int[] count = {0};
            button.addActionListener(() -> count[0]++);
            button.doClick();
            button.doClick();
            assertEquals(2, count[0]);
        });
    }

    @Test
    void listenerReceivingEventGetsTextAsActionCommand() {
        onUiThread(() -> {
            WHyperlinkButton button = new WHyperlinkButton("Help");
            String[] command = {null};
            button.addActionListener(e -> command[0] = e.getActionCommand());
            button.doClick();
            assertEquals("Help", command[0]);
        });
    }
}
