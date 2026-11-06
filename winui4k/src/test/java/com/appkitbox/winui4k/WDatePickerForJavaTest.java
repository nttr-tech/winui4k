package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WDatePicker's date the way a Java developer would write it (getter / setter and null checks). */
class WDatePickerForJavaTest {
    @Test
    void getDateIsNullWhenUnselectedReadsTheDateSetWithSetDateAndPassingNullToSetDateGoesBackToUnselected() {
        onUiThread(() -> {
            WDatePicker picker = new WDatePicker();
            assertNull(picker.getDate());

            picker.setDate(LocalDate.of(2025, 3, 14));
            assertEquals(LocalDate.of(2025, 3, 14), picker.getDate());
            assertEquals(LocalDate.of(2025, 3, 14), picker.getSelectedDate());

            picker.setDate(null);
            assertNull(picker.getDate());
            assertNull(picker.getSelectedDate());
        });
    }

    @Test
    void anonymousChangeListenerReadsTheChangedDateFromTheSourceAndAlsoReceivesClearingToNull() {
        onUiThread(() -> {
            WDatePicker picker = new WDatePicker();
            List<LocalDate> dates = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            WChangeListener listener = new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                    dates.add(((WDatePicker) e.getSource()).getDate());
                }
            };
            picker.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, picker.getChangeListeners());

            picker.setDate(LocalDate.of(2026, 10, 20));
            picker.setDate(LocalDate.of(2026, 10, 20));
            picker.setDate(null);
            assertEquals(Arrays.asList(LocalDate.of(2026, 10, 20), null), dates);
            assertSame(picker, sources.get(0));

            picker.removeChangeListener(listener);
            picker.setDate(LocalDate.of(2026, 10, 21));
            assertEquals(2, dates.size());
        });
    }
}
