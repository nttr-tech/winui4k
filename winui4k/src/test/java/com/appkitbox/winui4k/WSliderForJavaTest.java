package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WSlider from Java the way a developer used to Swing's JSlider would write it. */
class WSliderForJavaTest {
    @Test
    void canBeCreatedWithConstructorsTakingOrientationConstantsAndRange() {
        onUiThread(() -> {
            WSlider vertical = new WSlider(WSlider.VERTICAL);
            assertEquals(WSlider.VERTICAL, vertical.getOrientation());
            WSlider slider = new WSlider(WSlider.HORIZONTAL, 0, 10, 5);
            assertEquals(WSlider.HORIZONTAL, slider.getOrientation());
            assertEquals(0, slider.getMinimum());
            assertEquals(10, slider.getMaximum());
            assertEquals(5, (int) slider.getValue());
            WSlider ranged = new WSlider(10, 20);
            assertEquals(10, ranged.getValue());
        });
    }

    @Test
    void anonymousChangeListenerIsCalledOnEachSetValueOrSetMaximumAndCanReadTheValueFromTheSource() {
        onUiThread(() -> {
            WSlider slider = new WSlider();
            List<String> events = new ArrayList<>();
            slider.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    WSlider source = (WSlider) e.getSource();
                    events.add((int) source.getValue() + "/" + (int) source.getMaximum());
                }
            });
            slider.setValue(30);
            slider.setValue(30);
            slider.setMaximum(20);
            assertEquals(Arrays.asList("30/100", "20/20"), events);
        });
    }

    @Test
    void lambdaChangeListenerCanPickOnlyFinalValuesWithGetValueIsAdjustingAndIsRemovedByRemove() {
        onUiThread(() -> {
            WSlider slider = new WSlider();
            List<Integer> committed = new ArrayList<>();
            WChangeListener listener = e -> {
                if (!slider.getValueIsAdjusting()) {
                    committed.add((int) slider.getValue());
                }
            };
            slider.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, slider.getChangeListeners());
            slider.setValueIsAdjusting(true);
            slider.setValue(10);
            slider.setValue(20);
            slider.setValueIsAdjusting(false);
            slider.removeChangeListener(listener);
            slider.setValue(40);
            assertEquals(Arrays.asList(20), committed);
            assertEquals(0, slider.getChangeListeners().length);
        });
    }

    @Test
    void defaultBoundedRangeModelCanBeSharedByTwoSliders() {
        onUiThread(() -> {
            WDefaultBoundedRangeModel model = new WDefaultBoundedRangeModel(20, 0, 0, 100);
            WSlider first = new WSlider(model);
            WSlider second = new WSlider();
            second.setModel(model);
            assertSame(model, second.getModel());
            first.setValue(40);
            assertEquals(40, second.getValue());
            model.setValue(70);
            assertEquals(70, first.getValue());
            second.setExtent(10);
            assertEquals(10, first.getExtent());
        });
    }

    @Test
    void ticksSnappingAndInversionCanBeSetWithTheSameNamesAsJSlider() {
        onUiThread(() -> {
            WSlider slider = new WSlider(0, 100, 50);
            slider.setMajorTickSpacing(25);
            slider.setMinorTickSpacing(5);
            slider.setPaintTicks(true);
            slider.setSnapToTicks(true);
            slider.setInverted(true);
            assertEquals(25, slider.getMajorTickSpacing());
            assertEquals(5, slider.getMinorTickSpacing());
            assertEquals(25, slider.getTickFrequency());
            assertTrue(slider.getPaintTicks());
            assertTrue(slider.getSnapToTicks());
            assertTrue(slider.getInverted());
            slider.setPaintTicks(false);
            assertFalse(slider.getPaintTicks());
            assertEquals(TickPlacement.NONE, slider.getTickPlacement());
        });
    }
}
