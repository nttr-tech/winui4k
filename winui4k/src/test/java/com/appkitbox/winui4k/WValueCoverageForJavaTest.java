package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests that use, in the form a developer used to Swing writes in Java, the members of the value components that had no tests
 * (the int forms of JColorChooser.setColor, the Runnable and Consumer listeners of WComboBox, and setStart / setEnd of SpinnerDateModel).
 */
class WValueCoverageForJavaTest {
    @Test
    void setColorTakesThreeComponentsOrAnRgbIntLikeJColorChooser() {
        onUiThread(() -> {
            WColorPicker chooser = new WColorPicker();
            chooser.setColor(255, 128, 0);
            assertEquals(new WColor(255, 128, 0), chooser.getColor());
            chooser.setColor(0x336699);
            assertEquals(new WColor(0x33, 0x66, 0x99), chooser.getColor());
            assertThrows(IllegalArgumentException.class, () -> chooser.setColor(256, 0, 0));
            assertEquals(new WColor(0x33, 0x66, 0x99), chooser.getColor());
        });
    }

    @Test
    void WComboBoxAddListSelectionListenerTakesARunnableLambda_removeDetachesIt() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox(new String[] {"Red", "Green", "Blue"});
            List<Integer> seen = new ArrayList<>();
            Runnable listener = () -> seen.add(comboBox.getSelectedIndex());
            comboBox.addListSelectionListener(listener);
            comboBox.setSelectedIndex(2);
            comboBox.removeListSelectionListener(listener);
            comboBox.setSelectedIndex(1);
            assertEquals(Arrays.asList(2), seen);
        });
    }

    @Test
    void WComboBoxAddTextSubmitListenerTakesAConsumerLambda_notCalledForSettingsFromCode() {
        onUiThread(() -> {
            WComboBox comboBox = new WComboBox(new String[] {"Red", "Green"});
            comboBox.setEditable(true);
            List<String> submitted = new ArrayList<>();
            Consumer<String> listener = submitted::add;
            comboBox.addTextSubmitListener(listener);
            comboBox.setText("Purple");
            comboBox.setSelectedIndex(1);
            comboBox.removeTextSubmitListener(listener);
            assertTrue(submitted.isEmpty());
        });
    }

    @Test
    void setStartAndSetEndChangeTheRangeWithNotificationLikeSpinnerDateModel_previousAndNextOutsideTheRangeAreNull() {
        LocalDateTime value = LocalDateTime.of(2026, 10, 20, 9, 0);
        WSpinnerDateModel model = new WSpinnerDateModel(value, null, null, ChronoUnit.DAYS);
        List<Object> sources = new ArrayList<>();
        model.addChangeListener(e -> sources.add(e.getSource()));
        model.setEnd(value);
        model.setStart(value);
        model.setStart(value);
        assertEquals(value, model.getEnd());
        assertEquals(value, model.getStart());
        assertNull(model.getNextValue());
        assertNull(model.getPreviousValue());
        assertEquals(Arrays.asList(model, model), sources);
    }
}
