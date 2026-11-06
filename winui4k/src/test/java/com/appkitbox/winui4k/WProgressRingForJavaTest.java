package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WProgressRing from Java with the same style as JProgressBar (model, ChangeListener, getPercentComplete). */
class WProgressRingForJavaTest {
    @Test
    void createdWithAModelAndGetPercentCompleteCanBeReadInAChangeListener() {
        onUiThread(() -> {
            WBoundedRangeModel model = new WDefaultBoundedRangeModel(0, 0, 0, 8);
            WProgressRing progressRing = new WProgressRing(model);
            progressRing.setIndeterminate(false);
            assertSame(model, progressRing.getModel());
            List<Double> percents = new ArrayList<>();
            progressRing.addChangeListener(e -> percents.add(((WProgressRing) e.getSource()).getPercentComplete()));
            progressRing.setValue(2);
            model.setValue(6);
            assertEquals(2, percents.size());
            assertEquals(0.25, percents.get(0), 0.0);
            assertEquals(0.75, percents.get(1), 0.0);
            assertEquals("75%", progressRing.getString());
        });
    }
}
