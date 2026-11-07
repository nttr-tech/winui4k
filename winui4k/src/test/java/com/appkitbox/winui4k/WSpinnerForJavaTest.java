package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WSpinner from Java the way a developer used to Swing's JSpinner would write it. */
class WSpinnerForJavaTest {
    @Test
    void createdWithASpinnerNumberModelAndTheValueOfGetModelCanBeReadAsInteger() {
        onUiThread(() -> {
            WSpinner spinner = new WSpinner(new WSpinnerNumberModel(5, 0, 10, 1));
            Integer value = (Integer) spinner.getModel().getValue();
            assertEquals(5, value.intValue());
            assertEquals(5, (int) spinner.getValue());
            assertEquals(6, spinner.getNextValue());
            assertEquals(4, spinner.getPreviousValue());
        });
    }

    @Test
    void canBeFormattedTheSameWayAsJSpinnerNumberEditor() {
        onUiThread(() -> {
            WSpinner price = new WSpinner(new WSpinnerNumberModel(12.0, 0.0, 100.0, 0.5));
            WSpinner.NumberEditor editor = new WSpinner.NumberEditor(price, "0.00");
            price.setEditor(editor);
            assertSame(editor, price.getEditor());
            assertSame(price, editor.getSpinner());
            DecimalFormat format = editor.getFormat();
            assertEquals("12.00", format.format(12.0));
            // A NumberEditor without a pattern uses #,##0.###, the same as NumberFormat.getNumberInstance
            assertEquals("1,234.568", new WSpinner.NumberEditor(price).getFormat().format(1234.5678));
            price.setEditor(null);
            assertNull(price.getEditor());
            assertThrows(IllegalArgumentException.class, () -> new WSpinner.NumberEditor(price, "0.00.0"));
        });
    }

    @Test
    void anonymousChangeListenerIsCalledOnEverySetValueAndCanReadTheValueFromTheSource() {
        onUiThread(() -> {
            WSpinner spinner = new WSpinner(new WSpinnerNumberModel(0, 0, 100, 1));
            List<Integer> values = new ArrayList<>();
            spinner.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    WSpinner source = (WSpinner) e.getSource();
                    values.add((Integer) source.getModel().getValue());
                }
            });
            spinner.setValue(30);
            spinner.setValue(30);
            spinner.getModel().setValue(spinner.getNextValue());
            assertEquals(Arrays.asList(30, 31), values);
            assertEquals(1, spinner.getChangeListeners().length);
        });
    }

    @Test
    void lambdaChangeListenerIsRemovedByRemoveChangeListener() {
        onUiThread(() -> {
            WSpinner spinner = new WSpinner(0.0);   // Created with a Double value because decimals are entered (without arguments it is Integer 0)
            List<Double> values = new ArrayList<>();
            WChangeListener listener = e -> values.add(spinner.getValue());
            spinner.addChangeListener(listener);
            spinner.setValue(1.5);
            spinner.removeChangeListener(listener);
            spinner.setValue(2.5);
            assertEquals(Arrays.asList(1.5), values);
        });
    }

    @Test
    void setModelAcceptsOnlySpinnerNumberModel() {
        onUiThread(() -> {
            WSpinner spinner = new WSpinner();
            WSpinnerNumberModel model = new WSpinnerNumberModel(1.0, 0.0, 2.0, 0.5);
            spinner.setModel(model);
            assertSame(model, spinner.getModel());
            assertEquals(0.5, spinner.getSmallChange(), 0.0);
            assertThrows(IllegalArgumentException.class, () -> spinner.setModel(new WSpinnerListModel(new String[] {"a"})));
            assertThrows(IllegalArgumentException.class, () -> new WSpinner(new WSpinnerDateModel()));
            assertSame(model, spinner.getModel());
        });
    }

    @Test
    void nextValueOfAModelWithoutARangeHasNoUpperLimitAndIsNullAtTheEdgeOfTheRange() {
        onUiThread(() -> {
            WSpinner unbounded = new WSpinner(new WSpinnerNumberModel());
            assertEquals(1, unbounded.getNextValue());
            WSpinner bounded = new WSpinner(new WSpinnerNumberModel(10, 0, 10, 1));
            assertNull(bounded.getNextValue());
        });
    }
}
