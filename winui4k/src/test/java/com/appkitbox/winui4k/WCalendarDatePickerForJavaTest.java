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

/** Tests that use WCalendarDatePicker's ChangeListener from Java as a Swing developer would write it. */
class WCalendarDatePickerForJavaTest {
    @Test
    void anonymousChangeListenerReadsTheChangedDateFromTheSource() {
        onUiThread(() -> {
            WCalendarDatePicker picker = new WCalendarDatePicker();
            assertNull(picker.getDate());
            List<LocalDate> dates = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            WChangeListener listener = new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                    dates.add(((WCalendarDatePicker) e.getSource()).getDate());
                }
            };
            picker.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, picker.getChangeListeners());

            picker.setDate(LocalDate.of(2026, 12, 24));
            picker.setDate(LocalDate.of(2026, 12, 24));
            picker.setDate(null);
            assertEquals(Arrays.asList(LocalDate.of(2026, 12, 24), null), dates);
            assertSame(picker, sources.get(0));

            picker.removeChangeListener(listener);
            picker.setDate(LocalDate.of(2026, 12, 25));
            assertEquals(2, dates.size());
        });
    }
}
