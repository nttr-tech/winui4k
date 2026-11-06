package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTeachingTip's constructors, reading back title / subtitle / actionButtonText / closeButtonText, and icon from Java.
 * Opening, closing and the close reason are checked in WTeachingTipTest.
 */
class WTeachingTipForJavaTest {
    @Test
    void theTitleAndSubtitleCanBePassedWithTheConstructorOverloads() {
        onUiThread(() -> {
            WTeachingTip empty = new WTeachingTip();
            assertEquals("", empty.getTitle());
            assertEquals("", empty.getSubtitle());
            WTeachingTip titled = new WTeachingTip("New feature");
            assertEquals("New feature", titled.getTitle());
            assertEquals("", titled.getSubtitle());
            WTeachingTip both = new WTeachingTip("New feature", "You can share from here");
            assertEquals("New feature", both.getTitle());
            assertEquals("You can share from here", both.getSubtitle());
        });
    }

    @Test
    void buttonTextsReadBackTheValuesSetWithSettersAndDefaultToEmpty() {
        onUiThread(() -> {
            WTeachingTip tip = new WTeachingTip("Title");
            assertEquals("", tip.getActionButtonText());
            assertEquals("", tip.getCloseButtonText());
            tip.setActionButtonText("Try it");
            tip.setCloseButtonText("Later");
            assertEquals("Try it", tip.getActionButtonText());
            assertEquals("Later", tip.getCloseButtonText());
            tip.setActionButtonText("");
            assertEquals("", tip.getActionButtonText());
        });
    }

    @Test
    void iconCanBeSetWithTheGetterAndSetterAndNullRemovesIt() {
        onUiThread(() -> {
            WTeachingTip tip = new WTeachingTip();
            assertNull(tip.getIcon());
            tip.setIcon(Symbol.SHARE);
            assertSame(Symbol.SHARE, tip.getIcon());
            WFontIcon font = new WFontIcon("!");
            tip.setIcon(font);
            assertSame(font, tip.getIcon());
            tip.setIcon(null);
            assertNull(tip.getIcon());
        });
    }

    @Test
    void targetPlacementAndLightDismissCanBeSet_listenersCanBeAddedAndRemovedAsLambdas() {
        onUiThread(() -> {
            WTeachingTip tip = new WTeachingTip("Share");
            WButton share = new WButton("Share");
            assertNull(tip.getTarget());
            assertEquals(TeachingTipPlacement.AUTO, tip.getPreferredPlacement());
            assertFalse(tip.isLightDismissEnabled());
            assertFalse(tip.isOpen());
            tip.setTarget(share);
            tip.setPreferredPlacement(TeachingTipPlacement.BOTTOM);
            tip.setLightDismissEnabled(true);
            assertSame(share, tip.getTarget());
            assertEquals(TeachingTipPlacement.BOTTOM, tip.getPreferredPlacement());
            assertTrue(tip.isLightDismissEnabled());

            Runnable onAction = () -> { };
            Consumer<TeachingTipCloseReason> onClose = reason -> { };
            tip.addActionListener(onAction);
            tip.addCloseListener(onClose);
            tip.removeActionListener(onAction);
            tip.removeCloseListener(onClose);
            tip.removeCloseListener(onClose); // Removing one that is not attached does nothing
        });
    }
}
