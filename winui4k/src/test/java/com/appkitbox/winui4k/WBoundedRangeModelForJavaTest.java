package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WDefaultBoundedRangeModel from Java the way a developer used to Swing's BoundedRangeModel would write it. */
class WBoundedRangeModelForJavaTest {
    @Test
    void noArgumentConstructorGives0_0_0_100LikeSwing() {
        WBoundedRangeModel model = new WDefaultBoundedRangeModel();
        assertEquals(0, model.getValue(), 0.0);
        assertEquals(0, model.getExtent(), 0.0);
        assertEquals(0, model.getMinimum(), 0.0);
        assertEquals(100, model.getMaximum(), 0.0);
        assertEquals(false, model.getValueIsAdjusting());
    }

    @Test
    void setValueAcceptsIntegerLiteralsAndClampsToTheRange() {
        WBoundedRangeModel model = new WDefaultBoundedRangeModel(50, 10, 0, 100);
        model.setValue(150);
        assertEquals(90, model.getValue(), 0.0);
        model.setValue(-5);
        assertEquals(0, model.getValue(), 0.0);
    }

    @Test
    void constructorWithInvalidArgumentsThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new WDefaultBoundedRangeModel(150, 0, 0, 100));
    }

    @Test
    void lambdaWChangeListenerReceivesChangesWithTheModelAsSource() {
        WBoundedRangeModel model = new WDefaultBoundedRangeModel(0, 0, 0, 100);
        List<Object> sources = new ArrayList<>();
        WChangeListener listener = e -> sources.add(e.getSource());
        model.addChangeListener(listener);
        model.setValue(5);
        model.setValue(5);
        assertEquals(1, sources.size());
        assertSame(model, sources.get(0));
        model.removeChangeListener(listener);
        model.setValue(6);
        assertEquals(1, sources.size());
    }

    @Test
    void anonymousClassWChangeListenerWithGetChangeListenersAndSetRangeProperties() {
        WDefaultBoundedRangeModel model = new WDefaultBoundedRangeModel();
        int[] count = new int[1];
        model.addChangeListener(new WChangeListener() {
            @Override
            public void stateChanged(WChangeEvent e) {
                count[0]++;
            }
        });
        assertEquals(1, model.getChangeListeners().length);
        model.setRangeProperties(20, 200, 0, 100, true);
        assertEquals(1, count[0]);
        assertEquals(80, model.getExtent(), 0.0);
        assertEquals(true, model.getValueIsAdjusting());
    }
}
