package com.appkitbox.winui4k

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Conversions between Windows.Foundation.DateTime / TimeSpan and java.time types. */
internal object DateTimeConversions {
    /** The difference between 1601-01-01 UTC and 1970-01-01 UTC (in 100ns ticks). */
    private const val EPOCH_OFFSET_TICKS = 116_444_736_000_000_000L

    /** The number of 100ns ticks per millisecond. */
    private const val TICKS_PER_MILLI = 10_000L

    /** Windows.Foundation.DateTime (100ns ticks) -> LocalDate (in the system time zone). */
    fun ticksToLocalDate(ticks: Long): LocalDate {
        val epochMillis = (ticks - EPOCH_OFFSET_TICKS) / TICKS_PER_MILLI
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    /** LocalDate -> Windows.Foundation.DateTime (100ns ticks). Midnight (00:00) local time on that day. */
    fun localDateToTicks(date: LocalDate): Long {
        val epochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return epochMillis * TICKS_PER_MILLI + EPOCH_OFFSET_TICKS
    }

    /** Windows.Foundation.TimeSpan (100ns ticks) -> LocalTime. */
    fun ticksToLocalTime(ticks: Long): LocalTime {
        val totalNanos = ticks * 100L
        return LocalTime.ofNanoOfDay(totalNanos)
    }

    /** LocalTime -> Windows.Foundation.TimeSpan (100ns ticks). */
    fun localTimeToTicks(time: LocalTime): Long {
        return time.toNanoOfDay() / 100L
    }

    /** Windows.Globalization.DayOfWeek (Sunday=0..Saturday=6) -> java.time.DayOfWeek. */
    fun nativeDayOfWeekToJava(native: Int): java.time.DayOfWeek {
        return if (native == 0) java.time.DayOfWeek.SUNDAY
        else java.time.DayOfWeek.of(native)
    }

    /** java.time.DayOfWeek -> Windows.Globalization.DayOfWeek (Sunday=0..Saturday=6). */
    fun javaDayOfWeekToNative(dayOfWeek: java.time.DayOfWeek): Int {
        return if (dayOfWeek == java.time.DayOfWeek.SUNDAY) 0 else dayOfWeek.value
    }
}
