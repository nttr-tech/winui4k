package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;
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
}
