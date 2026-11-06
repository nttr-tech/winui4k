package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that create determinate progress rings from Java in the same way as Swing's new JProgressBar(0, 100) / new JProgressBar(model).
 */
class WProgressRingRangeForJavaTest {
    @Test
    void passingMinAndMaxToNewMakesADeterminateProgressRing() {
        onUiThread(() -> {
            WProgressRing ring = new WProgressRing(0, 100);
            ring.setValue(25);
            assertFalse(ring.isIndeterminate());
            assertEquals(0.25, ring.getPercentComplete(), 1e-9);
        });
    }

    @Test
    void passingAModelAlsoMakesItDeterminate_aReversedRangeThrows() {
        onUiThread(() -> {
            WProgressRing ring = new WProgressRing(new WDefaultBoundedRangeModel(1, 0, 0, 4));
            assertFalse(ring.isIndeterminate());
            assertThrows(IllegalArgumentException.class, () -> new WProgressRing(3, 1));
        });
    }
}
