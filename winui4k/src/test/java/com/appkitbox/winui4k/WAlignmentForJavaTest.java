package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WinUI4K components from Java with the same names and meanings as Swing's setHorizontalAlignment / setVerticalAlignment (the content alignment of JLabel, JTextField and AbstractButton).
 * The position of a component in its parent is setHorizontalAlignmentInParent / setVerticalAlignmentInParent.
 */
class WAlignmentForJavaTest {
    @Test
    void setHorizontalAlignmentAndSetVerticalAlignmentOfWLabelAlignTheTextInsideTheLabel() {
        onUiThread(() -> {
            WLabel label = new WLabel("Right");
            assertEquals(HorizontalAlignment.LEFT, label.getHorizontalAlignment());
            assertEquals(VerticalAlignment.CENTER, label.getVerticalAlignment());
            label.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            label.setVerticalAlignment(VerticalAlignment.TOP);
            assertEquals(HorizontalAlignment.RIGHT, label.getHorizontalAlignment());
            assertEquals(TextAlignment.RIGHT, label.getTextAlignment());
            assertEquals(VerticalAlignment.TOP, label.getVerticalAlignment());
            assertEquals(HorizontalAlignment.STRETCH, label.getHorizontalAlignmentInParent());
            assertThrows(IllegalArgumentException.class, () -> label.setHorizontalAlignment(HorizontalAlignment.STRETCH));
            assertThrows(IllegalArgumentException.class, () -> label.setVerticalAlignment(VerticalAlignment.STRETCH));
        });
    }

    @Test
    void setHorizontalAlignmentOfWTextFieldAlignsTheTextInsideTheField() {
        onUiThread(() -> {
            WTextField price = new WTextField("1,980", 10);
            assertEquals(HorizontalAlignment.LEFT, price.getHorizontalAlignment());
            price.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            assertEquals(HorizontalAlignment.RIGHT, price.getHorizontalAlignment());
            assertEquals(TextAlignment.RIGHT, price.getTextAlignment());
            assertEquals(HorizontalAlignment.STRETCH, price.getHorizontalAlignmentInParent());
            assertThrows(IllegalArgumentException.class, () -> price.setHorizontalAlignment(HorizontalAlignment.STRETCH));
        });
    }

    @Test
    void setHorizontalAlignmentAndSetVerticalAlignmentOfButtonsAlignTheButtonContent() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            button.setHorizontalAlignment(HorizontalAlignment.LEFT);
            button.setVerticalAlignment(VerticalAlignment.BOTTOM);
            assertEquals(HorizontalAlignment.LEFT, button.getHorizontalAlignment());
            assertEquals(VerticalAlignment.BOTTOM, button.getVerticalAlignment());
            assertEquals(HorizontalAlignment.LEFT, button.getHorizontalContentAlignment());
            assertEquals(VerticalAlignment.BOTTOM, button.getVerticalContentAlignment());

            WCheckBox check = new WCheckBox("Notify");
            assertEquals(HorizontalAlignment.LEFT, check.getHorizontalAlignment());
            check.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            assertEquals(HorizontalAlignment.RIGHT, check.getHorizontalContentAlignment());
        });
    }

    @Test
    void setHorizontalAlignmentInParentAndSetVerticalAlignmentInParentPlaceTheComponentInItsParent() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            panel.setHorizontalAlignmentInParent(HorizontalAlignment.RIGHT);
            panel.setVerticalAlignmentInParent(VerticalAlignment.BOTTOM);
            assertEquals(HorizontalAlignment.RIGHT, panel.getHorizontalAlignmentInParent());
            assertEquals(VerticalAlignment.BOTTOM, panel.getVerticalAlignmentInParent());

            WButton button = new WButton("OK");
            button.setHorizontalAlignmentInParent(HorizontalAlignment.CENTER);
            assertEquals(HorizontalAlignment.CENTER, button.getHorizontalAlignmentInParent());
            assertEquals(HorizontalAlignment.CENTER, button.getHorizontalAlignment());
        });
    }
}
