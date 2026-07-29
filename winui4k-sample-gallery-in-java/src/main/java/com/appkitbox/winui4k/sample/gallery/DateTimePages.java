package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.CalendarViewSelectionMode;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.WCalendarDatePicker;
import com.appkitbox.winui4k.WCalendarView;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WDatePicker;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WTimePicker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


/*
 * Date & time category: demo pages for CalendarDatePicker / CalendarView / DatePicker / TimePicker.
 */
final class DateTimePages {
    private DateTimePages() {
    }

    // region CalendarDatePicker page

    /** The CalendarDatePicker page: a picker that selects a date from a calendar drop-down. */
    static WComponent buildCalendarDatePickerPage() {
        WPanel page = GalleryScaffold.buildPage(
                "CalendarDatePicker",
                "A picker that selects a single date from a calendar drop-down. "
                        + "Header sets the label, and PlaceholderText sets the text shown when nothing is selected.");

        page.add(buildCalendarDatePickerExample());
        return page;
    }

    /** Header + PlaceholderText + DateChanged demo. */
    private static WComponent buildCalendarDatePickerExample() {
        WCalendarDatePicker picker = new WCalendarDatePicker();
        picker.setHeader("Calendar");
        picker.setPlaceholderText("Pick a date");

        WLabel result = new WLabel("Selected date: (none)");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());
        result.setTextWrapping(TextWrapping.WRAP);

        picker.addDateChangedListener(date -> {
            if (date != null) {
                result.setText("Selected date: " + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            } else {
                result.setText("Selected date: (none)");
            }
        });

        WPanel body = new WPanel(12.0);
        body.add(picker);
        body.add(result);
        return GalleryScaffold.buildExample("Header and PlaceholderText (Header / PlaceholderText / DateChanged)", body);
    }

    // endregion

    // region CalendarView page

    /** The CalendarView page: a control that shows a calendar at all times. */
    static WComponent buildCalendarViewPage() {
        WPanel page = GalleryScaffold.buildPage(
                "CalendarView",
                "A control that shows a calendar at all times and lets you select dates. "
                        + "SelectionMode switches the selection style, and DisplayMode switches between "
                        + "month / year / decade views.");

        page.add(buildCalendarViewExample());
        return page;
    }

    /** Switching SelectionMode / IsGroupLabelVisible / IsOutOfScopeEnabled / CalendarIdentifier. */
    private static WComponent buildCalendarViewExample() {
        WCalendarView calendarView = new WCalendarView();
        calendarView.setSelectionMode(CalendarViewSelectionMode.SINGLE);

        WPanel body = new WPanel(12.0);
        body.add(calendarView);

        // Options
        WCheckBox isGroupLabelVisible = new WCheckBox("IsGroupLabelVisible");
        isGroupLabelVisible.setChecked(true);
        isGroupLabelVisible.addItemListener(checked -> {
            calendarView.setGroupLabelVisible(Boolean.TRUE.equals(checked));
        });

        WCheckBox isOutOfScopeEnabled = new WCheckBox("IsOutOfScopeEnabled");
        isOutOfScopeEnabled.setChecked(true);
        isOutOfScopeEnabled.addItemListener(checked -> {
            calendarView.setOutOfScopeEnabled(Boolean.TRUE.equals(checked));
        });

        WComboBox selectionModeCombo = new WComboBox(Arrays.asList("None", "Single", "Multiple"));
        selectionModeCombo.setHeader("SelectionMode");
        selectionModeCombo.setWidth(240.0);
        selectionModeCombo.setSelectedIndex(1);
        selectionModeCombo.addListSelectionListener(() -> {
            String selected = selectionModeCombo.getSelectedItem();
            if ("None".equals(selected)) {
                calendarView.setSelectionMode(CalendarViewSelectionMode.NONE);
            } else if ("Multiple".equals(selected)) {
                calendarView.setSelectionMode(CalendarViewSelectionMode.MULTIPLE);
            } else {
                calendarView.setSelectionMode(CalendarViewSelectionMode.SINGLE);
            }
        });

        List<String> calendarIdentifiers = Arrays.asList(
                "GregorianCalendar", "HebrewCalendar", "HijriCalendar",
                "JapaneseCalendar", "JulianCalendar", "KoreanCalendar",
                "PersianCalendar", "TaiwanCalendar", "ThaiCalendar", "UmAlQuraCalendar");
        WComboBox calendarIdCombo = new WComboBox(calendarIdentifiers);
        calendarIdCombo.setHeader("CalendarIdentifier");
        calendarIdCombo.setWidth(240.0);
        calendarIdCombo.setSelectedIndex(0);
        calendarIdCombo.addListSelectionListener(() -> {
            String selected = calendarIdCombo.getSelectedItem();
            if (selected != null) {
                calendarView.setCalendarIdentifier(selected);
            }
        });

        WPanel options = new WPanel(12.0);
        options.add(isGroupLabelVisible);
        options.add(isOutOfScopeEnabled);
        options.add(selectionModeCombo);
        options.add(calendarIdCombo);

        return GalleryScaffold.buildExample(
                "Displaying a calendar (SelectionMode / IsGroupLabelVisible / IsOutOfScopeEnabled / CalendarIdentifier)",
                body,
                options);
    }

    // endregion

    // region DatePicker page

    /** The DatePicker page: a picker that selects a date via day / month / year spinners. */
    static WComponent buildDatePickerPage() {
        WPanel page = GalleryScaffold.buildPage(
                "DatePicker",
                "A picker that selects a date via day / month / year spinners. "
                        + "Header sets the label, and DayFormat customizes the spinner's display format.");

        page.add(buildDatePickerSimpleExample());
        page.add(buildDatePickerCustomFormatExample());
        return page;
    }

    /** A simple DatePicker + Header. */
    private static WComponent buildDatePickerSimpleExample() {
        WDatePicker picker = new WDatePicker();
        picker.setHeader("Pick a date");

        WPanel body = new WPanel(12.0);
        body.add(picker);
        return GalleryScaffold.buildExample("A DatePicker with a Header (Header)", body);
    }

    /** A custom DayFormat + YearVisible=false + MinYear/MaxYear constraints. */
    private static WComponent buildDatePickerCustomFormatExample() {
        WDatePicker picker = new WDatePicker();
        picker.setDayFormat("{day.integer} ({dayofweek.abbreviated})");
        picker.setYearVisible(false);
        picker.setDate(LocalDate.now().plusMonths(2));
        picker.setMinYear(LocalDate.now());
        picker.setMaxYear(LocalDate.now().plusYears(5));

        WLabel result = new WLabel("");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());
        result.setTextWrapping(TextWrapping.WRAP);

        picker.addSelectedDateChangedListener(date -> {
            if (date != null) {
                result.setText("Selected: " + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            } else {
                result.setText("");
            }
        });

        WPanel body = new WPanel(12.0, Orientation.HORIZONTAL);
        body.add(picker);
        body.add(result);

        // Options
        WCheckBox dayVisibleCheck = new WCheckBox("DayVisible");
        dayVisibleCheck.setChecked(true);
        dayVisibleCheck.addItemListener(checked -> {
            picker.setDayVisible(Boolean.TRUE.equals(checked));
        });

        WCheckBox monthVisibleCheck = new WCheckBox("MonthVisible");
        monthVisibleCheck.setChecked(true);
        monthVisibleCheck.addItemListener(checked -> {
            picker.setMonthVisible(Boolean.TRUE.equals(checked));
        });

        WCheckBox yearVisibleCheck = new WCheckBox("YearVisible");
        yearVisibleCheck.setChecked(false);
        yearVisibleCheck.addItemListener(checked -> {
            picker.setYearVisible(Boolean.TRUE.equals(checked));
        });

        WPanel options = new WPanel(12.0);
        options.add(GalleryScaffold.optionsLabel("Toggle which columns are shown with the checkboxes."));
        options.add(dayVisibleCheck);
        options.add(monthVisibleCheck);
        options.add(yearVisibleCheck);

        return GalleryScaffold.buildExample(
                "A custom format and constraints (DayFormat / YearVisible / MinYear / MaxYear / SelectedDateChanged)",
                body,
                options);
    }

    // endregion

    // region TimePicker page

    /** The TimePicker page: a picker that selects a time via hour / minute spinners. */
    static WComponent buildTimePickerPage() {
        WPanel page = GalleryScaffold.buildPage(
                "TimePicker",
                "A picker that selects a time via hour / minute spinners. "
                        + "ClockIdentifier switches between 12-hour and 24-hour notation, and MinuteIncrement "
                        + "sets the step of the minutes.");

        page.add(buildTimePickerSimpleExample());
        page.add(buildTimePickerOptionsExample());
        return page;
    }

    /** A simple TimePicker. */
    private static WComponent buildTimePickerSimpleExample() {
        WTimePicker picker = new WTimePicker();

        WPanel body = new WPanel(12.0);
        body.add(picker);
        return GalleryScaffold.buildExample("The default TimePicker", body);
    }

    /** Header + MinuteIncrement + ClockIdentifier switched via Options. */
    private static WComponent buildTimePickerOptionsExample() {
        WTimePicker picker = new WTimePicker();
        picker.setHeader("Arrival time");
        picker.setMinuteIncrement(15);
        picker.setTime(LocalTime.now());

        WLabel result = new WLabel("");
        result.setForeground(GalleryTheme.TEXT_SECONDARY());
        result.setTextWrapping(TextWrapping.WRAP);

        picker.addSelectedTimeChangedListener(time -> {
            if (time != null) {
                result.setText("Selected: " + time.format(DateTimeFormatter.ofPattern("HH:mm")));
            } else {
                result.setText("");
            }
        });

        WPanel body = new WPanel(12.0);
        body.add(picker);
        body.add(result);

        // Options
        WComboBox clockCombo = new WComboBox(Arrays.asList("12HourClock", "24HourClock"));
        clockCombo.setHeader("ClockIdentifier");
        clockCombo.setWidth(240.0);
        clockCombo.setSelectedIndex(0);
        clockCombo.addListSelectionListener(() -> {
            String selected = clockCombo.getSelectedItem();
            if (selected != null) {
                picker.setClockIdentifier(selected);
            }
        });

        WComboBox minuteIncrementCombo = new WComboBox(Arrays.asList("1", "5", "10", "15", "30"));
        minuteIncrementCombo.setHeader("MinuteIncrement");
        minuteIncrementCombo.setWidth(240.0);
        minuteIncrementCombo.setSelectedIndex(3);
        minuteIncrementCombo.addListSelectionListener(() -> {
            String selected = minuteIncrementCombo.getSelectedItem();
            if (selected != null) {
                picker.setMinuteIncrement(Integer.parseInt(selected));
            }
        });

        WPanel options = new WPanel(12.0);
        options.add(clockCombo);
        options.add(minuteIncrementCombo);

        return GalleryScaffold.buildExample(
                "Header and the step (Header / ClockIdentifier / MinuteIncrement / SelectedTimeChanged)",
                body,
                options);
    }

    // endregion
}
