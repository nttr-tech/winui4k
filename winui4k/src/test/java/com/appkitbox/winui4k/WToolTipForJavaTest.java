package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests that use JComponent.setToolTipText / getToolTipText and JToolTip's getTipText / setTipText / getComponent
 * in the same way they would be written in Java.
 */
class WToolTipForJavaTest {
    @Test
    void setToolTipTextAndGetToolTipTextRoundTrip() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            assertNull(button.getToolTipText());
            button.setToolTipText("Save to a file");
            assertEquals("Save to a file", button.getToolTipText());
            button.setToolTipText(null);
            assertNull(button.getToolTipText());
        });
    }

    @Test
    void canBeUsedWithTheSameGettersAndSettersAsJToolTip() {
        onUiThread(() -> {
            WButton button = new WButton("Open");
            WToolTip tip = new WToolTip();
            assertNull(tip.getTipText());
            assertNull(tip.getComponent());
            assertFalse(tip.isOpen());

            tip.setTipText("Open a file");
            button.setToolTip(tip);
            assertEquals("Open a file", tip.getTipText());
            assertSame(button, tip.getComponent());
            assertEquals("Open a file", button.getToolTipText());

            button.setToolTipText("Back to a string");
            assertNull(tip.getComponent());
        });
    }

    @Test
    void passingNullToSetToolTipRemovesTheHint() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            WToolTip tip = new WToolTip();
            tip.setTipText("Save to a file");
            button.setToolTip(tip);
            assertSame(tip, button.getToolTip());
            button.setToolTip(null);
            assertNull(button.getToolTip());
            assertNull(button.getToolTipText());
            assertNull(tip.getComponent());

            button.setToolTipText("Text tip");
            button.setToolTip(null);
            assertNull(button.getToolTipText());
        });
    }

    @Test
    void getToolTipIsNullWithOnlyATextHint() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            assertNull(button.getToolTip());
            button.setToolTipText("Text tip");
            assertNull(button.getToolTip());
            assertEquals("Text tip", button.getToolTipText());
        });
    }

    @Test
    void placementAndComponentContentCanBeSetWithGettersAndSetters() {
        onUiThread(() -> {
            WToolTip tip = new WToolTip();
            assertEquals(ToolTipPlacement.TOP, tip.getPlacement());
            tip.setPlacement(ToolTipPlacement.MOUSE);
            assertEquals(ToolTipPlacement.MOUSE, tip.getPlacement());
            WLabel label = new WLabel("Component tip");
            tip.setContent(label);
            assertSame(label, tip.getContent());
            assertNull(tip.getTipText());
            tip.setText("Text tip");
            assertEquals("Text tip", tip.getText());
            assertNull(tip.getContent());
        });
    }
}
