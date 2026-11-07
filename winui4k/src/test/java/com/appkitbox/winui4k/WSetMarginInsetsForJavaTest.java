package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Tests that check, in Java as Swing developers write it, that {@code setMargin(WInsets)} becomes the component's inner padding (Control.Padding),
 * as with AbstractButton.setMargin(Insets) / JTextComponent.setMargin(Insets)
 * (writing {@code button.setMargin(new Insets(2, 14, 2, 14))} with {@code new WInsets(...)} instead).
 * WInsets is in the order top, left, bottom, right, as with Swing's Insets.
 */
class WSetMarginInsetsForJavaTest {
    /** Returns getInsets (including the border's 1) after setMargin(1, 2, 3, 4) on a component from [create] with a line border of 1. */
    private static <C extends WControl> WInsets insetsAfterSetMargin(Supplier<C> create, java.util.function.BiConsumer<C, WInsets> setMargin) {
        C control = onUiThreadGet(create::get);
        UiTestHarness.attachAndAwaitLoaded(control);
        try {
            return onUiThreadGet(() -> {
                control.setBorder(WBorderFactory.createLineBorder(WColor.BLACK, 1));
                setMargin.accept(control, new WInsets(1, 2, 3, 4));
                return control.getInsets();
            });
        } finally {
            UiTestHarness.detach(control);
        }
    }

    @Test
    void setMarginOfWButtonBecomesTheInnerPaddingInTheOrderTopLeftBottomRightAndIsIncludedInGetInsets() {
        assertEquals(new WInsets(2, 3, 4, 5), insetsAfterSetMargin(() -> new WButton("OK"), WButtonBase::setMargin));
    }

    @Test
    void setMarginOfWCheckBoxAlsoBecomesTheInnerPadding() {
        assertEquals(new WInsets(2, 3, 4, 5), insetsAfterSetMargin(() -> new WCheckBox("Bold"), WButtonBase::setMargin));
    }

    @Test
    void setMarginOfWTextFieldBecomesTheInnerPadding() {
        assertEquals(new WInsets(2, 3, 4, 5), insetsAfterSetMargin(WTextField::new, WTextField::setMargin));
    }

    @Test
    void setMarginOfWPasswordFieldBecomesTheInnerPadding() {
        assertEquals(new WInsets(2, 3, 4, 5), insetsAfterSetMargin(WPasswordField::new, WPasswordField::setMargin));
    }

    @Test
    void setMarginOfWTextPaneBecomesTheInnerPadding() {
        assertEquals(new WInsets(2, 3, 4, 5), insetsAfterSetMargin(WTextPane::new, WTextPane::setMargin));
    }

    @Test
    void setMarginWithEqualSidesCanBeReadWithGetPaddingAndGetMarginOfTheOuterSpaceStaysZero() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            button.setMargin(new WInsets(5));
            assertEquals(5.0, button.getPadding());
            assertEquals(0.0, button.getMargin());
        });
    }

    @Test
    void wideningTheInnerPaddingWithSetMarginOfAButtonWidensTheButtonItself() {
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            double narrow = onUiThreadGet(() -> {
                button.setMargin(new WInsets(0, 0, 0, 0));
                button.validate();
                return button.getPreferredSize().getWidth();
            });
            double wide = onUiThreadGet(() -> {
                button.setMargin(new WInsets(0, 20, 0, 30));
                button.validate();
                return button.getPreferredSize().getWidth();
            });
            assertEquals(narrow + 50, wide, 0.5);
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
