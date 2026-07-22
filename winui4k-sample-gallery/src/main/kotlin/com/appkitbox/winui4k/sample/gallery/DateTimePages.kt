package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.CalendarViewDisplayMode
import com.appkitbox.winui4k.CalendarViewSelectionMode
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WCalendarDatePicker
import com.appkitbox.winui4k.WCalendarView
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WDatePicker
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTimePicker
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Date & time category: demo pages for CalendarDatePicker / CalendarView / DatePicker / TimePicker.
 */

// region CalendarDatePicker page

/** The CalendarDatePicker page: a picker that selects a date from a calendar drop-down. */
internal fun buildCalendarDatePickerPage(): WComponent {
    val page = buildPage(
        "CalendarDatePicker",
        "A picker that selects a single date from a calendar drop-down. " +
            "Header sets the label, and PlaceholderText sets the text shown when nothing is selected.",
    )

    page.add(buildCalendarDatePickerExample())
    return page
}

/** Header + PlaceholderText + DateChanged demo. */
private fun buildCalendarDatePickerExample(): WComponent {
    val picker = WCalendarDatePicker()
    picker.header = "Calendar"
    picker.placeholderText = "Pick a date"

    val result = WLabel("Selected date: (none)")
    result.foreground = TEXT_SECONDARY
    result.textWrapping = TextWrapping.WRAP

    picker.addDateChangedListener { date ->
        result.text = if (date != null) {
            "Selected date: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        } else {
            "Selected date: (none)"
        }
    }

    val body = WPanel(spacing = 12.0)
    body.add(picker)
    body.add(result)
    return buildExample("Header and PlaceholderText (Header / PlaceholderText / DateChanged)", body)
}

// endregion

// region CalendarView page

/** The CalendarView page: a control that shows a calendar at all times. */
internal fun buildCalendarViewPage(): WComponent {
    val page = buildPage(
        "CalendarView",
        "A control that shows a calendar at all times and lets you select dates. " +
            "SelectionMode switches the selection style, and DisplayMode switches between " +
            "month / year / decade views.",
    )

    page.add(buildCalendarViewExample())
    return page
}

/** Switching SelectionMode / IsGroupLabelVisible / IsOutOfScopeEnabled / CalendarIdentifier. */
private fun buildCalendarViewExample(): WComponent {
    val calendarView = WCalendarView()
    calendarView.selectionMode = CalendarViewSelectionMode.SINGLE

    val body = WPanel(spacing = 12.0)
    body.add(calendarView)

    // Options
    val isGroupLabelVisible = WCheckBox("IsGroupLabelVisible")
    isGroupLabelVisible.isChecked = true
    isGroupLabelVisible.addItemListener { calendarView.isGroupLabelVisible = it == true }

    val isOutOfScopeEnabled = WCheckBox("IsOutOfScopeEnabled")
    isOutOfScopeEnabled.isChecked = true
    isOutOfScopeEnabled.addItemListener { calendarView.isOutOfScopeEnabled = it == true }

    val selectionModeCombo = WComboBox(listOf("None", "Single", "Multiple"))
    selectionModeCombo.header = "SelectionMode"
    selectionModeCombo.width = 240.0
    selectionModeCombo.selectedIndex = 1
    selectionModeCombo.addListSelectionListener {
        calendarView.selectionMode = when (selectionModeCombo.selectedItem) {
            "None" -> CalendarViewSelectionMode.NONE
            "Multiple" -> CalendarViewSelectionMode.MULTIPLE
            else -> CalendarViewSelectionMode.SINGLE
        }
    }

    val calendarIdentifiers = listOf(
        "GregorianCalendar", "HebrewCalendar", "HijriCalendar",
        "JapaneseCalendar", "JulianCalendar", "KoreanCalendar",
        "PersianCalendar", "TaiwanCalendar", "ThaiCalendar", "UmAlQuraCalendar",
    )
    val calendarIdCombo = WComboBox(calendarIdentifiers)
    calendarIdCombo.header = "CalendarIdentifier"
    calendarIdCombo.width = 240.0
    calendarIdCombo.selectedIndex = 0
    calendarIdCombo.addListSelectionListener {
        val selected = calendarIdCombo.selectedItem
        if (selected != null) calendarView.calendarIdentifier = selected
    }

    val options = WPanel(spacing = 12.0)
    options.add(isGroupLabelVisible)
    options.add(isOutOfScopeEnabled)
    options.add(selectionModeCombo)
    options.add(calendarIdCombo)

    return buildExample(
        "Displaying a calendar (SelectionMode / IsGroupLabelVisible / IsOutOfScopeEnabled / CalendarIdentifier)",
        body,
        options,
    )
}

// endregion

// region DatePicker page

/** The DatePicker page: a picker that selects a date via day / month / year spinners. */
internal fun buildDatePickerPage(): WComponent {
    val page = buildPage(
        "DatePicker",
        "A picker that selects a date via day / month / year spinners. " +
            "Header sets the label, and DayFormat customizes the spinner's display format.",
    )

    page.add(buildDatePickerSimpleExample())
    page.add(buildDatePickerCustomFormatExample())
    return page
}

/** A simple DatePicker + Header. */
private fun buildDatePickerSimpleExample(): WComponent {
    val picker = WDatePicker()
    picker.header = "Pick a date"

    val body = WPanel(spacing = 12.0)
    body.add(picker)
    return buildExample("A DatePicker with a Header (Header)", body)
}

/** A custom DayFormat + YearVisible=false + MinYear/MaxYear constraints. */
private fun buildDatePickerCustomFormatExample(): WComponent {
    val picker = WDatePicker()
    picker.dayFormat = "{day.integer} ({dayofweek.abbreviated})"
    picker.yearVisible = false
    picker.date = LocalDate.now().plusMonths(2)
    picker.minYear = LocalDate.now()
    picker.maxYear = LocalDate.now().plusYears(5)

    val result = WLabel("")
    result.foreground = TEXT_SECONDARY
    result.textWrapping = TextWrapping.WRAP

    picker.addSelectedDateChangedListener { date ->
        result.text = if (date != null) {
            "Selected: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        } else {
            ""
        }
    }

    val body = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    body.add(picker)
    body.add(result)

    // Options
    val dayVisibleCheck = WCheckBox("DayVisible")
    dayVisibleCheck.isChecked = true
    dayVisibleCheck.addItemListener { picker.dayVisible = it == true }

    val monthVisibleCheck = WCheckBox("MonthVisible")
    monthVisibleCheck.isChecked = true
    monthVisibleCheck.addItemListener { picker.monthVisible = it == true }

    val yearVisibleCheck = WCheckBox("YearVisible")
    yearVisibleCheck.isChecked = false
    yearVisibleCheck.addItemListener { picker.yearVisible = it == true }

    val options = WPanel(spacing = 12.0)
    options.add(optionsLabel("Toggle which columns are shown with the checkboxes."))
    options.add(dayVisibleCheck)
    options.add(monthVisibleCheck)
    options.add(yearVisibleCheck)

    return buildExample(
        "A custom format and constraints (DayFormat / YearVisible / MinYear / MaxYear / SelectedDateChanged)",
        body,
        options,
    )
}

// endregion

// region TimePicker page

/** The TimePicker page: a picker that selects a time via hour / minute spinners. */
internal fun buildTimePickerPage(): WComponent {
    val page = buildPage(
        "TimePicker",
        "A picker that selects a time via hour / minute spinners. " +
            "ClockIdentifier switches between 12-hour and 24-hour notation, and MinuteIncrement " +
            "sets the minute step.",
    )

    page.add(buildTimePickerSimpleExample())
    page.add(buildTimePickerOptionsExample())
    return page
}

/** A simple TimePicker. */
private fun buildTimePickerSimpleExample(): WComponent {
    val picker = WTimePicker()

    val body = WPanel(spacing = 12.0)
    body.add(picker)
    return buildExample("The default TimePicker", body)
}

/** Header + MinuteIncrement + ClockIdentifier switched via Options. */
private fun buildTimePickerOptionsExample(): WComponent {
    val picker = WTimePicker()
    picker.header = "Arrival time"
    picker.minuteIncrement = 15
    picker.time = LocalTime.now()

    val result = WLabel("")
    result.foreground = TEXT_SECONDARY
    result.textWrapping = TextWrapping.WRAP

    picker.addSelectedTimeChangedListener { time ->
        result.text = if (time != null) {
            "Selected: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } else {
            ""
        }
    }

    val body = WPanel(spacing = 12.0)
    body.add(picker)
    body.add(result)

    // Options
    val clockCombo = WComboBox(listOf("12HourClock", "24HourClock"))
    clockCombo.header = "ClockIdentifier"
    clockCombo.width = 240.0
    clockCombo.selectedIndex = 0
    clockCombo.addListSelectionListener {
        val selected = clockCombo.selectedItem
        if (selected != null) picker.clockIdentifier = selected
    }

    val minuteIncrementCombo = WComboBox(listOf("1", "5", "10", "15", "30"))
    minuteIncrementCombo.header = "MinuteIncrement"
    minuteIncrementCombo.width = 240.0
    minuteIncrementCombo.selectedIndex = 3
    minuteIncrementCombo.addListSelectionListener {
        val selected = minuteIncrementCombo.selectedItem
        if (selected != null) picker.minuteIncrement = selected.toInt()
    }

    val options = WPanel(spacing = 12.0)
    options.add(clockCombo)
    options.add(minuteIncrementCombo)

    return buildExample(
        "Header and the step (Header / ClockIdentifier / MinuteIncrement / SelectedTimeChanged)",
        body,
        options,
    )
}

// endregion
