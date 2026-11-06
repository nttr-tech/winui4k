package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WInfoBar's Closing / Closed listeners (lambdas and method references) and the icon of WInfoBar / WInfoBadge / WTeachingTip
 * from Java.
 */
class WInfoBarForJavaTest {
    @Test
    void closingAndClosedListenersCanBeAddedAndRemovedWithLambdas() {
        onUiThread(() -> {
            List<String> events = new ArrayList<>();
            WInfoBar bar = new WInfoBar();
            bar.setOpen(true);
            bar.addClosingListener(e -> {
                events.add("closing:" + e.getReason());
                e.setCancel(e.getReason() == InfoBarCloseReason.CLOSE_BUTTON);
            });
            Consumer<InfoBarCloseReason> closed = reason -> events.add("closed:" + reason);
            bar.addCloseListener(closed);
            bar.setOpen(false);
            assertEquals(Arrays.asList("closing:PROGRAMMATIC", "closed:PROGRAMMATIC"), events);

            bar.removeCloseListener(closed);
            bar.setOpen(true);
            bar.setOpen(false);
            assertEquals(Arrays.asList("closing:PROGRAMMATIC", "closed:PROGRAMMATIC", "closing:PROGRAMMATIC"), events);
        });
    }

    @Test
    void displayedContentCanBeSetWithGettersAndSetters_closeButtonListenersCanBeAddedAndRemovedAsRunnables() {
        onUiThread(() -> {
            WInfoBar bar = new WInfoBar();
            bar.setTitle("An update is available");
            bar.setMessage("It applies after a restart");
            bar.setSeverity(InfoBarSeverity.WARNING);
            bar.setClosable(false);
            bar.setIconVisible(false);
            WButton restart = new WButton("Restart");
            bar.setActionButton(restart);
            WLabel detail = new WLabel("Details");
            bar.setContent(detail);
            assertEquals("An update is available", bar.getTitle());
            assertEquals("It applies after a restart", bar.getMessage());
            assertEquals(InfoBarSeverity.WARNING, bar.getSeverity());
            assertFalse(bar.isClosable());
            assertFalse(bar.isIconVisible());
            assertSame(restart, bar.getActionButton());
            assertSame(detail, bar.getContent());

            Runnable onClose = () -> { };
            bar.addCloseButtonListener(onClose);
            bar.removeCloseButtonListener(onClose);
            bar.removeCloseButtonListener(onClose); // Removing one that is not attached does nothing
        });
    }

    @Test
    void iconCanBeReadAndWrittenWithTheGetterAndSetter() {
        onUiThread(() -> {
            WInfoBar bar = new WInfoBar();
            assertNull(bar.getIcon());
            bar.setIcon(Symbol.SAVE);
            assertSame(Symbol.SAVE, bar.getIcon());

            WInfoBadge badge = new WInfoBadge();
            badge.setIcon(new WFontIcon("!"));
            assertTrue(badge.getIcon() instanceof WFontIcon);

            WTeachingTip tip = new WTeachingTip("Title");
            tip.setIcon(Symbol.SHARE);
            tip.setActionButtonText("Try it");
            assertSame(Symbol.SHARE, tip.getIcon());
            assertEquals("Try it", tip.getActionButtonText());
        });
    }
}
