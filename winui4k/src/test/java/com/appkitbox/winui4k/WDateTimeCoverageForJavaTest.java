package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Tests that attach and detach the typed listeners (the Consumer form) of the date and time pickers in the form written in Java. */
class WDateTimeCoverageForJavaTest {
    @Test
    void WTimePickerAddSelectedTimeChangedListenerTakesAConsumerLambda_removeDetachesIt() {
        onUiThread(() -> {
            WTimePicker picker = new WTimePicker();
            List<LocalTime> received = new ArrayList<>();
            Consumer<LocalTime> listener = received::add;
            picker.addSelectedTimeChangedListener(listener);
            picker.setSelectedTime(LocalTime.of(9, 30));
            picker.removeSelectedTimeChangedListener(listener);
            picker.setSelectedTime(LocalTime.of(10, 45));
            assertEquals(Arrays.asList(LocalTime.of(9, 30)), received);
        });
    }

    @Test
    void WCalendarDatePickerAddDateChangedListenerTakesAConsumerLambda_clearingArrivesAsNull() {
        onUiThread(() -> {
            WCalendarDatePicker picker = new WCalendarDatePicker();
            List<LocalDate> received = new ArrayList<>();
            Consumer<LocalDate> listener = received::add;
            picker.addDateChangedListener(listener);
            picker.setDate(LocalDate.of(2026, 10, 20));
            picker.setDate(null);
            picker.removeDateChangedListener(listener);
            picker.setDate(LocalDate.of(2027, 1, 1));
            assertEquals(Arrays.asList(LocalDate.of(2026, 10, 20), null), received);
        });
    }

    @Test
    void WDatePickerAddSelectedDateChangedListenerTakesAConsumerLambda_removeDetachesIt() throws InterruptedException {
        WDatePicker picker = UiTestHarness.onUiThreadGet(WDatePicker::new);
        LinkedBlockingQueue<LocalDate> received = new LinkedBlockingQueue<>();
        Consumer<LocalDate> listener = received::add;
        onUiThread(() -> picker.addSelectedDateChangedListener(listener));
        UiTestHarness.attachAndAwaitLoaded(picker);
        try {
            onUiThread(() -> picker.setSelectedDate(LocalDate.of(2030, 5, 17)));
            assertEquals(LocalDate.of(2030, 5, 17), received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            onUiThread(() -> {
                picker.removeSelectedDateChangedListener(listener);
                picker.setSelectedDate(LocalDate.of(2031, 6, 18));
            });
            onUiThread(() -> assertEquals(LocalDate.of(2031, 6, 18), picker.getSelectedDate()));
            assertEquals(0, received.size());
        } finally {
            UiTestHarness.detach(picker);
        }
    }
}
