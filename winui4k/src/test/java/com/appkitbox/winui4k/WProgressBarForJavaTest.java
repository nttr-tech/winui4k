package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WProgressBar from Java the way a developer used to Swing's JProgressBar would write it. */
class WProgressBarForJavaTest {
    @Test
    void twoIntArgumentConstructorStartsTheValueAtTheMinimumAndTheThreeDoubleArgumentFormCanBeUsedToo() {
        onUiThread(() -> {
            WProgressBar progressBar = new WProgressBar(0, 200);
            assertEquals(0, (int) progressBar.getValue());
            assertEquals(200, (int) progressBar.getMaximum());
            WProgressBar started = new WProgressBar(0, 100, 30);
            assertEquals(30, (int) started.getValue());
            WProgressBar defaults = new WProgressBar();
            assertEquals(100, (int) defaults.getMaximum());
        });
    }

    @Test
    void anonymousChangeListenerCanReadGetPercentCompleteAndGetString() {
        onUiThread(() -> {
            WProgressBar progressBar = new WProgressBar(0, 100);
            List<String> strings = new ArrayList<>();
            progressBar.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    WProgressBar source = (WProgressBar) e.getSource();
                    strings.add(source.getString() + "/" + source.getPercentComplete());
                }
            });
            progressBar.setValue(25);
            progressBar.setValue(25);
            progressBar.setValue(50);
            assertEquals(Arrays.asList("25%/0.25", "50%/0.5"), strings);
        });
    }

    @Test
    void setStringAndSetStringPaintedCanBeUsedWithTheSameNamesAsSwingAndNullGoesBackToTheDefaultString() {
        onUiThread(() -> {
            WProgressBar progressBar = new WProgressBar(0, 10);
            assertFalse(progressBar.isStringPainted());
            progressBar.setStringPainted(true);
            progressBar.setString("Loading");
            assertEquals("Loading", progressBar.getString());
            progressBar.setString(null);
            progressBar.setValue(10);
            assertEquals("100%", progressBar.getString());
        });
    }

    @Test
    void boundedRangeModelCanBeSharedWithWSliderAndReceivedWithALambdaChangeListener() {
        onUiThread(() -> {
            WBoundedRangeModel model = new WDefaultBoundedRangeModel(0, 0, 0, 100);
            WSlider slider = new WSlider(model);
            WProgressBar progressBar = new WProgressBar(model);
            assertSame(model, progressBar.getModel());
            List<Integer> values = new ArrayList<>();
            WChangeListener listener = e -> values.add((int) progressBar.getValue());
            progressBar.addChangeListener(listener);
            slider.setValue(70);
            progressBar.removeChangeListener(listener);
            slider.setValue(80);
            assertEquals(Arrays.asList(70), values);
            assertEquals(80, (int) progressBar.getValue());
        });
    }
}
