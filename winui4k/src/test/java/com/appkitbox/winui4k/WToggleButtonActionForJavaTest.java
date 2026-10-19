package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WToggleButton / WCheckBox / WRadioButton the way a developer used to the constructors, setIcon and
 * setAction of Swing's JToggleButton / JCheckBox / JRadioButton would write it in Java. Details of how an action's
 * selection state is reflected are checked in WToggleButtonActionTest.
 */
class WToggleButtonActionForJavaTest {
    @Test
    void selectedKeyIsTheSameStringAsSwingActionSelectedKey() {
        assertEquals(Action.SELECTED_KEY, WAction.SELECTED_KEY);
    }

    @Test
    void canBeCreatedWithTheSameConstructorsAsSwing() {
        onUiThread(() -> {
            WCheckBox bold = new WCheckBox("Bold", true);
            assertEquals("Bold", bold.getText());
            assertTrue(bold.isSelected());

            WToggleButton iconOnly = new WToggleButton(Symbol.BOLD);
            assertEquals("", iconOnly.getText());
            assertSame(Symbol.BOLD, iconOnly.getIcon());

            WRadioButton small = new WRadioButton("Small", new WFontIcon(""));
            assertEquals("Small", small.getText());
            assertEquals(new WFontIcon("\uE8E8"), small.getIcon());
            assertFalse(small.isSelected());

            WRadioButton large = new WRadioButton("Large", Symbol.ZOOM_IN, true);
            assertSame(Symbol.ZOOM_IN, large.getIcon());
            assertTrue(large.isSelected());
        });
    }

    @Test
    void creatingWithTextAndANullIconGivesNoIcon() {
        onUiThread(() -> {
            WCheckBox box = new WCheckBox("Notifications", null);
            assertEquals("Notifications", box.getText());
            assertNull(box.getIcon());
            assertFalse(box.isSelected());
        });
    }

    @Test
    void setIconAndSetHorizontalTextPositionCanBeUsedLikeJButton() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Underline");
            button.setIcon(Symbol.UNDERLINE);
            button.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
            button.setVerticalTextPosition(VerticalTextPosition.BOTTOM);
            button.setIconTextGap(2.0);
            assertSame(Symbol.UNDERLINE, button.getIcon());
            assertEquals(HorizontalTextPosition.CENTER, button.getHorizontalTextPosition());
            assertEquals(VerticalTextPosition.BOTTOM, button.getVerticalTextPosition());
            assertEquals(2.0, button.getIconTextGap());
            assertEquals("Underline", button.getText());
        });
    }

    @Test
    void creatingWithAnAnonymousActionUsesSelectedKeyAsSelectionAndAClickWritesItBack() {
        onUiThread(() -> {
            List<Object> selectedWhenPerformed = new ArrayList<>();
            WAction action = new WAbstractAction("Bold", Symbol.BOLD) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    selectedWhenPerformed.add(getValue(WAction.SELECTED_KEY));
                }
            };
            action.putValue(WAction.SELECTED_KEY, Boolean.TRUE);
            WCheckBox box = new WCheckBox(action);
            assertSame(action, box.getAction());
            assertEquals("Bold", box.getText());
            assertTrue(box.isSelected());

            box.doClick();
            assertFalse(box.isSelected());
            assertEquals(Boolean.FALSE, action.getValue(WAction.SELECTED_KEY));
            assertEquals(Arrays.asList(Boolean.FALSE), selectedWhenPerformed);

            action.putValue(WAction.SELECTED_KEY, Boolean.TRUE);
            assertTrue(box.isSelected());
        });
    }

    @Test
    void setActionReplacesTheActionAndNullDeselects() {
        onUiThread(() -> {
            WToggleButton button = new WToggleButton("Fixed", true);
            WAction action = new WAbstractAction("Pin") {
                @Override
                public void actionPerformed(WActionEvent e) {
                }
            };
            action.putValue(WAction.SELECTED_KEY, Boolean.TRUE);
            button.setAction(action);
            assertEquals("Pin", button.getText());
            assertTrue(button.isSelected());
            button.setAction(null);
            assertNull(button.getAction());
            assertFalse(button.isSelected());
        });
    }

    @Test
    void selectedKeyOfTheActionIsStillThePreviousValueInsideItemListenerAndIsWrittenBackAfterAllListeners() {
        onUiThread(() -> {
            WAction action = new WAbstractAction("Bold") {
                @Override
                public void actionPerformed(WActionEvent e) {
                }
            };
            action.putValue(WAction.SELECTED_KEY, Boolean.FALSE);
            WCheckBox bold = new WCheckBox(action);
            List<Object> seen = new ArrayList<>();
            bold.addItemListener(e -> seen.add(action.getValue(WAction.SELECTED_KEY)));
            bold.doClick();
            assertEquals(Arrays.asList((Object) Boolean.FALSE), seen);
            assertEquals(Boolean.TRUE, action.getValue(WAction.SELECTED_KEY));
        });
    }

    @Test
    void nonBooleanSelectedKeyThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WAction action = new WAbstractAction("Bold") {
                @Override
                public void actionPerformed(WActionEvent e) {
                }
            };
            action.putValue(WAction.SELECTED_KEY, "true");
            assertThrows(IllegalArgumentException.class, () -> new WRadioButton(action));
        });
    }
}
