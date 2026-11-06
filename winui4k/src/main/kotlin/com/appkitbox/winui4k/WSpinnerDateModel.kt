package com.appkitbox.winui4k

import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * SpinnerDateModel-style: a [WSpinnerModel] that steps between a start ([start]) and an end ([end]) by a date-time unit ([calendarField]).
 *
 * Instead of Swing's `java.util.Date` and `Calendar` fields (`Calendar.DAY_OF_MONTH` and so on), it uses java.time's [LocalDateTime] and [ChronoUnit]
 * (`ChronoUnit.DAYS` and so on), like WinUI4K's other date-time components (no form that takes a `Date` is provided).
 * As in Swing:
 * - Setting [value] does not clamp it to the range. Null or values that are not a [LocalDateTime] throw [IllegalArgumentException]
 * - [nextValue] / [previousValue] are the date-time shifted by one unit, and null when after [end] or before [start]
 *   (adding a month to the month-end 1/31 gives 2/28 or 2/29, also as Calendar does). Also null when the date-time cannot be represented by a [LocalDateTime]
 * - Notifications are sent only when the value, start, end or unit actually changed
 *
 * At this point [WSpinner] can display only numeric models ([WSpinnerNumberModel]), so this model cannot be attached to a spinner.
 *
 * @throws IllegalArgumentException if the constructor does not satisfy `start <= value <= end`, or if [calendarField] is
 *   [ChronoUnit.FOREVER]
 */
class WSpinnerDateModel(value: LocalDateTime, start: LocalDateTime?, end: LocalDateTime?, calendarField: ChronoUnit) :
    WAbstractSpinnerModel() {
    /** Like SpinnerDateModel(): creates a model whose value is the current date-time, with no range, stepping one day at a time. */
    constructor() : this(LocalDateTime.now(), null, null, ChronoUnit.DAYS)

    private var current: LocalDateTime = value

    init {
        requireUnit(calendarField)
        require((start == null || start <= value) && (end == null || end >= value)) { "(start <= value <= end) is false" }
    }

    /** The start of the range (no limit if null). Changing it does not clamp [value]. */
    var start: LocalDateTime? = start
        set(n) {
            if (n == field) return
            field = n
            fireStateChanged()
        }

    /** The end of the range (no limit if null). Changing it does not clamp [value]. */
    var end: LocalDateTime? = end
        set(n) {
            if (n == field) return
            field = n
            fireStateChanged()
        }

    /**
     * Like SpinnerDateModel.getCalendarField / setCalendarField: the unit [nextValue] / [previousValue] shift by (`ChronoUnit.DAYS` and so on).
     * @throws IllegalArgumentException when [ChronoUnit.FOREVER] is set
     */
    var calendarField: ChronoUnit = calendarField
        set(unit) {
            requireUnit(unit)
            if (unit == field) return
            field = unit
            fireStateChanged()
        }

    /** Like SpinnerDateModel.getDate: the current value (returns the same thing as [value] as a [LocalDateTime]). */
    val date: LocalDateTime get() = current

    /**
     * The current value (a [LocalDateTime]). Values outside the range are kept without clamping.
     * @throws IllegalArgumentException when null or a value that is not a [LocalDateTime] is set
     */
    override var value: Any?
        get() = current
        set(v) {
            require(v is LocalDateTime) { "illegal value" }
            if (v == current) return
            current = v
            fireStateChanged()
        }

    /** [value] advanced by one [calendarField]. Null when it would be after [end]. */
    override val nextValue: Any?
        get() {
            val next = shifted(1) ?: return null
            val limit = end
            return if (limit == null || limit >= next) next else null
        }

    /** [value] moved back by one [calendarField]. Null when it would be before [start]. */
    override val previousValue: Any?
        get() {
            val previous = shifted(-1) ?: return null
            val limit = start
            return if (limit == null || limit <= previous) previous else null
        }

    private fun shifted(amount: Long): LocalDateTime? = try {
        current.plus(amount, calendarField)
    } catch (_: DateTimeException) {
        null // Beyond the range of LocalDateTime (+/-999999999 years)
    } catch (_: ArithmeticException) {
        null
    }

    private companion object {
        fun requireUnit(unit: ChronoUnit) {
            require(unit != ChronoUnit.FOREVER) { "invalid calendarField" }
        }
    }
}
