package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
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
}
