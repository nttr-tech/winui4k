package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.DateTimeConversions
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.ChartsInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import java.time.LocalDateTime

/**
 * The tick unit of a date-time axis (Charts.DateTimeIntervalType). Values are extracted from the winmd.
 */
enum class DateTimeIntervalType(internal val native: Int) {
    /** Determined automatically from the data range. */
    AUTO(0),

    /** Every day. */
    DAY(1),

    /** Every week. */
    WEEK(2),

    /** Every month. */
    MONTH(3),

    /** Every year. */
    YEAR(4),
    ;

    internal companion object {
        fun of(native: Int): DateTimeIntervalType = entries.first { it.native == native }
    }
}

/**
 * Charts.DateTimeAxis of WinUI 3: a date-time axis (time series).
 *
 * Data values are put into the [SampleModel] as java.time values (LocalDate / LocalDateTime / Instant, etc.).
 * If the range ([minimum] / [maximum]) is null, it is determined automatically from the data.
 *
 * Chart handles date-times in UTC (tick boundaries and label dates are based on UTC). So LocalDate / LocalDateTime
 * (both data and range) are passed as UTC date-times as is, without attaching a time zone, and the specified date is
 * shown on the axis. Instant / OffsetDateTime / ZonedDateTime / java.util.Date are absolute times, so they are shown
 * with their UTC date (times before 9:00 Japan time fall on the previous day).
 */
class DateTimeAxis @JvmOverloads constructor(label: String = "") : CartesianAxis(
    Activation.activate(ChartsInterop.CLS_DateTimeAxis, ChartsInterop.IID_IDateTimeAxis), // default interface = IDateTimeAxis
) {
    /** IDateTimeAxis (the default interface). */
    private val dateTimeAxis: ComPtr = inspectable

    /** The axis minimum (DateTimeAxis.Minimum). If null, it is determined automatically from the data. */
    var minimum: LocalDateTime?
        get() = dateTimeAxis.getDateTimeReference(ChartsInterop.IDateTimeAxis_get_Minimum)
        set(value) {
            dateTimeAxis.putDateTimeReference(ChartsInterop.IDateTimeAxis_put_Minimum, value)
        }

    /** The axis maximum (DateTimeAxis.Maximum). If null, it is determined automatically from the data. */
    var maximum: LocalDateTime?
        get() = dateTimeAxis.getDateTimeReference(ChartsInterop.IDateTimeAxis_get_Maximum)
        set(value) {
            dateTimeAxis.putDateTimeReference(ChartsInterop.IDateTimeAxis_put_Maximum, value)
        }

    /** The tick unit (DateTimeAxis.IntervalType). */
    var intervalType: DateTimeIntervalType
        get() = DateTimeIntervalType.of(dateTimeAxis.getInt(ChartsInterop.IDateTimeAxis_get_IntervalType))
        set(value) {
            dateTimeAxis.call(ChartsInterop.IDateTimeAxis_put_IntervalType, value.native)
        }

    /**
     * The format of the tick labels (DateTimeAxis.LabelFormat), specified as a template of
     * Windows.Globalization.DateTimeFormatting.DateTimeFormatter ("month day" / "shortdate" / "year", etc.).
     * If empty, a default format suited to the tick unit is used.
     */
    var labelFormat: String
        get() = dateTimeAxis.getString(ChartsInterop.IDateTimeAxis_get_LabelFormat)
        set(value) {
            Hstring.use(value) { h -> dateTimeAxis.call(ChartsInterop.IDateTimeAxis_put_LabelFormat, h) }
        }

    init {
        if (label.isNotEmpty()) this.label = label
    }
}

/** Reads the IReference<DateTime> property [slot]. Returns null if it is null (not set). */
private fun ComPtr.getDateTimeReference(slot: Int): LocalDateTime? {
    val boxed = getPtrOrNull(slot) ?: return null
    return try {
        PropertyValues.unboxDateTime(boxed)?.let { DateTimeConversions.ticksToUtcDateTime(it) }
    } finally {
        boxed.release()
    }
}

/** Sets [value] on the IReference<DateTime> property [slot]. null resets it to not set (automatic). */
private fun ComPtr.putDateTimeReference(slot: Int, value: LocalDateTime?) {
    if (value == null) {
        call(slot, null)
        return
    }
    val boxed = PropertyValues.boxDateTime(DateTimeConversions.utcDateTimeToTicks(value))
    val reference = boxed.queryInterface(FoundationInterop.IID_IReference_DateTime)
    try {
        call(slot, reference)
    } finally {
        reference.release()
        boxed.release()
    }
}
