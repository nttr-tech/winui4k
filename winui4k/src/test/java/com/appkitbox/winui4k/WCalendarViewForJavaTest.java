package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests adding and removing WCalendarView's ChangeListener from Java as a Swing developer would write it. */
class WCalendarViewForJavaTest {
    @Test
    void lambdaAndAnonymousChangeListenersCanBeAddedAndRemoved() {
        onUiThread(() -> {
            WCalendarView calendar = new WCalendarView();
            List<Object> sources = new ArrayList<>();
            WChangeListener lambda = e -> sources.add(e.getSource());
            WChangeListener anonymous = new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                }
            };
            calendar.addChangeListener(lambda);
            calendar.addChangeListener(anonymous);
            assertArrayEquals(new WChangeListener[] {lambda, anonymous}, calendar.getChangeListeners());

            calendar.removeChangeListener(lambda);
            assertArrayEquals(new WChangeListener[] {anonymous}, calendar.getChangeListeners());
            calendar.removeChangeListener(anonymous);
            assertEquals(0, calendar.getChangeListeners().length);
            assertEquals(0, sources.size());
        });
    }

    @Test
    void aNoArgLambdaALambdaReceivingTheEventAndAnAnonymousClassCanBeAddedWithTheSameName() {
        onUiThread(() -> {
            WCalendarView calendar = new WCalendarView();
            calendar.setSelectionMode(CalendarViewSelectionMode.MULTIPLE);
            List<String> calls = new ArrayList<>();
            List<LocalDate> added = new ArrayList<>();
            List<LocalDate> removed = new ArrayList<>();
            calendar.addSelectedDatesChangedListener(() -> calls.add("runnable"));
            calendar.addSelectedDatesChangedListener(e -> added.addAll(e.getAddedDates()));
            WCalendarViewSelectionListener anonymous = new WCalendarViewSelectionListener() {
                @Override
                public void selectedDatesChanged(WCalendarViewSelectionEvent e) {
                    removed.addAll(e.getRemovedDates());
                    assertSame(calendar, e.getSource());
                }
            };
            calendar.addSelectedDatesChangedListener(anonymous);

            calendar.addSelectedDate(LocalDate.of(2026, 7, 7));
            calendar.removeSelectedDate(LocalDate.of(2026, 7, 7));
            calendar.removeSelectedDatesChangedListener(anonymous);
            calendar.addSelectedDate(LocalDate.of(2026, 7, 8));

            assertEquals(Arrays.asList("runnable", "runnable", "runnable"), calls);
            assertEquals(Arrays.asList(LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 8)), added);
            assertEquals(Collections.singletonList(LocalDate.of(2026, 7, 7)), removed);
        });
    }

    @Test
    void theSelectionCanBeReadAndWrittenWithTheSameOperationNamesAsJList() {
        onUiThread(() -> {
            WCalendarView calendar = new WCalendarView();
            assertNull(calendar.getSelectedDate());
            assertTrue(calendar.getSelectedDates().isEmpty());

            calendar.setSelectionMode(CalendarViewSelectionMode.SINGLE);
            calendar.setSelectedDate(LocalDate.of(2026, 3, 3));
            assertEquals(LocalDate.of(2026, 3, 3), calendar.getSelectedDate());
            assertThrows(IllegalStateException.class, () -> calendar.addSelectedDate(LocalDate.of(2026, 3, 4)));
            calendar.clearSelection();
            assertNull(calendar.getSelectedDate());

            calendar.setSelectionMode(CalendarViewSelectionMode.NONE);
            assertThrows(IllegalStateException.class, () -> calendar.setSelectedDate(LocalDate.of(2026, 3, 3)));

            calendar.setMinDate(LocalDate.of(2026, 1, 1));
            assertEquals(LocalDate.of(2026, 1, 1), calendar.getMinDate());
        });
    }
}
