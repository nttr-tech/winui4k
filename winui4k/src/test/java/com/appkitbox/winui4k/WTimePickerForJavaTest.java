package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WTimePicker's time the way a Java developer would write it (getter / setter and null checks). */
class WTimePickerForJavaTest {
    @Test
    void getTimeIsNullWhenUnselectedReadsTheTimeSetWithSetTimeAndPassingNullToSetTimeGoesBackToUnselected() {
        onUiThread(() -> {
            WTimePicker picker = new WTimePicker();
            assertNull(picker.getTime());

            picker.setTime(LocalTime.of(7, 45));
            assertEquals(LocalTime.of(7, 45), picker.getTime());

            picker.setTime(null);
            assertNull(picker.getTime());
            assertNull(picker.getSelectedTime());
        });
    }

    @Test
    void lambdaChangeListenerReadsTheChangedTimeWithGetTime() {
        onUiThread(() -> {
            WTimePicker picker = new WTimePicker();
            List<LocalTime> times = new ArrayList<>();
            WChangeListener listener = e -> times.add(picker.getTime());
            picker.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, picker.getChangeListeners());

            picker.setTime(LocalTime.of(9, 0));
            picker.setSelectedTime(LocalTime.of(9, 0));
            picker.setTime(null);
            assertEquals(Arrays.asList(LocalTime.of(9, 0), null), times);

            picker.removeChangeListener(listener);
            picker.setTime(LocalTime.of(10, 0));
            assertEquals(2, times.size());
        });
    }
}
