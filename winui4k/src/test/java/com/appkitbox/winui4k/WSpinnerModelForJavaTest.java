package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use the spinner models from Java the way a developer used to Swing's SpinnerNumberModel and others would write it. */
class WSpinnerModelForJavaTest {
    @Test
    void fourIntConstructorGivesAnIntegerModelAndTheNextAndPreviousValuesCanBeCastToInteger() {
        WSpinnerNumberModel model = new WSpinnerNumberModel(5, 0, 10, 1);
        Integer value = (Integer) model.getValue();
        assertEquals(5, value.intValue());
        assertEquals(6, ((Integer) model.getNextValue()).intValue());
        assertEquals(4, ((Integer) model.getPreviousValue()).intValue());
        assertEquals(5, model.getNumber().intValue());
        assertEquals(0, model.getMinimum());
        assertEquals(10, model.getMaximum());
        assertEquals(1, model.getStepSize());
    }

    @Test
    void fourDoubleConstructorAndNoArgumentConstructor() {
        WSpinnerNumberModel doubles = new WSpinnerNumberModel(0.5, 0.0, 1.0, 0.1);
        assertEquals(0.5, (Double) doubles.getValue(), 0.0);
        WSpinnerNumberModel empty = new WSpinnerNumberModel();
        assertEquals(Integer.valueOf(0), empty.getValue());
        assertNull(empty.getMinimum());
        assertNull(empty.getMaximum());
        assertEquals(Integer.valueOf(1), empty.getStepSize());
    }

    @Test
    void canBeCreatedWithNumberAndNullRangesAndTheNextValueBeyondTheUpperLimitIsNull() {
        WSpinnerNumberModel model = new WSpinnerNumberModel(Integer.valueOf(10), null, Integer.valueOf(10), Integer.valueOf(1));
        assertNull(model.getNextValue());
        model.setMaximum(null);
        assertEquals(Integer.valueOf(11), model.getNextValue());
    }

    @Test
    void setValueDoesNotClampToTheRangeAndNonNumericValuesThrowIllegalArgumentException() {
        WSpinnerNumberModel model = new WSpinnerNumberModel(5, 0, 10, 1);
        model.setValue(150);
        assertEquals(Integer.valueOf(150), model.getValue());
        assertThrows(IllegalArgumentException.class, () -> model.setValue("6"));
        assertThrows(IllegalArgumentException.class, () -> model.setValue(null));
        assertThrows(IllegalArgumentException.class, () -> new WSpinnerNumberModel(11, 0, 10, 1));
    }

    @Test
    void anonymousClassAndLambdaChangeListenersCanReadTheValueFromTheSourceModel() {
        WSpinnerModel model = new WSpinnerNumberModel(5, 0, 10, 1);
        List<Object> values = new ArrayList<>();
        model.addChangeListener(new WChangeListener() {
            @Override
            public void stateChanged(WChangeEvent e) {
                values.add(((WSpinnerModel) e.getSource()).getValue());
            }
        });
        WChangeListener lambda = e -> values.add("lambda");
        model.addChangeListener(lambda);
        model.setValue(model.getNextValue());
        assertEquals(Arrays.asList("lambda", 6), values);
        model.removeChangeListener(lambda);
        model.setValue(7);
        assertEquals(Arrays.asList("lambda", 6, 7), values);
    }

    @Test
    void ownModelExtendsWAbstractSpinnerModelAndNotifiesWithFireStateChanged() {
        class Counter extends WAbstractSpinnerModel {
            private int count;

            @Override
            public Object getValue() {
                return count;
            }

            @Override
            public void setValue(Object value) {
                count = (Integer) value;
                fireStateChanged();
            }

            @Override
            public Object getNextValue() {
                return count + 1;
            }

            @Override
            public Object getPreviousValue() {
                return count - 1;
            }
        }
        Counter counter = new Counter();
        List<WChangeEvent> events = new ArrayList<>();
        counter.addChangeListener(events::add);
        counter.setValue(counter.getNextValue());
        assertEquals(1, events.size());
        assertSame(counter, events.get(0).getSource());
        assertEquals(1, counter.getChangeListeners().length);
    }

    @Test
    void listModelCanBeCreatedFromAnArrayAndAListAndAValueNotInTheListThrowsIllegalArgumentException() {
        WSpinnerListModel model = new WSpinnerListModel(new String[] {"Mon", "Tue", "Wed"});
        assertEquals("Mon", model.getValue());
        assertEquals("Tue", model.getNextValue());
        assertNull(model.getPreviousValue());
        model.setValue("Wed");
        assertNull(model.getNextValue());
        assertThrows(IllegalArgumentException.class, () -> model.setValue("Thu"));
        WSpinnerListModel fromList = new WSpinnerListModel(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList(1, 2, 3), fromList.getList());
        assertThrows(IllegalArgumentException.class, () -> new WSpinnerListModel(new Object[0]));
    }

    @Test
    void dateModelIsCreatedWithLocalDateTimeAndChronoUnitAndTheNextValueBeyondTheRangeIsNull() {
        LocalDateTime noon = LocalDateTime.of(2024, 12, 31, 12, 0);
        WSpinnerDateModel model = new WSpinnerDateModel(noon, null, noon.plusHours(1), ChronoUnit.HOURS);
        assertEquals(LocalDateTime.of(2024, 12, 31, 13, 0), model.getNextValue());
        assertEquals(LocalDateTime.of(2024, 12, 31, 11, 0), model.getPreviousValue());
        model.setCalendarField(ChronoUnit.DAYS);
        assertNull(model.getNextValue());
        assertEquals(ChronoUnit.DAYS, model.getCalendarField());
        assertEquals(noon, model.getDate());
        assertThrows(IllegalArgumentException.class, () -> model.setValue(new java.util.Date()));
        assertEquals(ChronoUnit.DAYS, new WSpinnerDateModel().getCalendarField());
    }
}
