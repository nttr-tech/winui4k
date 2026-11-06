package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.function.DoubleFunction;
import org.junit.jupiter.api.Test;

/** Tests for using WAnnotatedScrollBar from Java (adding labels, DetailLabelRequested with a DoubleFunction lambda). Compiling at all verifies the shape of the API. */
class WAnnotatedScrollBarForJavaTest {
    @Test
    void addLabelAndClearLabelsCanBeUsedAndConnectToIsFalseWhileTheScrollViewIsNotShown() {
        onUiThread(() -> {
            WAnnotatedScrollBar bar = new WAnnotatedScrollBar();
            bar.addLabel("A", 0.0);
            bar.addLabel("B", 120.0);
            bar.clearLabels();
            assertFalse(bar.connectTo(new WScrollView(new WLabel("Content"))));
        });
    }

    @Test
    void aLambdaCanBePassedToAddDetailLabelRequestedListenerAndRemovedWithTheSameReference() {
        onUiThread(() -> {
            WAnnotatedScrollBar bar = new WAnnotatedScrollBar();
            DoubleFunction<String> listener = offset -> "Position " + offset;
            bar.addDetailLabelRequestedListener(listener);
            bar.removeDetailLabelRequestedListener(listener);
            bar.removeDetailLabelRequestedListener(listener);
        });
    }
}
