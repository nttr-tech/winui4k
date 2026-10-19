package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests using WLabel's icon in the form developers who have used Swing's JLabel(String, Icon) / setIcon /
 * setHorizontalTextPosition / setVerticalTextPosition / setIconTextGap write in Java. The arrangement positions are
 * checked in WLabelIconTest.
 */
class WLabelIconForJavaTest {
    @Test
    void labelCanBeCreatedWithTextAndIcon() {
        onUiThread(() -> {
            WLabel label = new WLabel("Save", Symbol.SAVE);
            assertEquals("Save", label.getText());
            assertSame(Symbol.SAVE, label.getIcon());
        });
    }

    @Test
    void labelCanBeCreatedWithIconOnly() {
        onUiThread(() -> {
            WLabel label = new WLabel(new WImageIcon("C:/icons/open.png"));
            assertEquals("", label.getText());
            assertEquals(new WImageIcon("C:/icons/open.png"), label.getIcon());
        });
    }

    @Test
    void creatingWithTextAndANullIconGivesNoIcon() {
        onUiThread(() -> {
            WLabel label = new WLabel("Name", null);
            assertEquals("Name", label.getText());
            assertNull(label.getIcon());
        });
    }

    @Test
    void textPositionAndGapCanBeChangedWithSettersAndReadWithGetters() {
        onUiThread(() -> {
            WLabel label = new WLabel("Top", Symbol.SAVE);
            label.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
            label.setVerticalTextPosition(VerticalTextPosition.TOP);
            label.setIconTextGap(8);
            assertEquals(HorizontalTextPosition.CENTER, label.getHorizontalTextPosition());
            assertEquals(VerticalTextPosition.TOP, label.getVerticalTextPosition());
            assertEquals(8.0, label.getIconTextGap());
        });
    }

    @Test
    void setIconReplacesTheIconAndNullRemovesIt() {
        onUiThread(() -> {
            WLabel label = new WLabel("Open");
            label.setIcon(new WFontIcon("\uE8E5"));
            assertEquals(new WFontIcon("\uE8E5"), label.getIcon());
            label.setIcon(null);
            assertNull(label.getIcon());
            assertEquals("Open", label.getText());
        });
    }

    @Test
    void selfImplementedIconAsInSwingCannotBePassed() {
        onUiThread(() -> {
            WLabel label = new WLabel("Custom");
            WIcon custom = new WIcon() {};
            assertThrows(IllegalArgumentException.class, () -> label.setIcon(custom));
            assertNull(label.getIcon());
        });
    }

    @Test
    void negativeIconTextGapCannotBeSet() {
        onUiThread(() -> {
            WLabel label = new WLabel("Spacing");
            assertThrows(IllegalArgumentException.class, () -> label.setIconTextGap(-1));
            assertEquals(4.0, label.getIconTextGap());
        });
    }
}
