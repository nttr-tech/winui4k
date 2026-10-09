package com.appkitbox.winui4k.chart

/**
 * Like javax.swing.ListModel: the model of a one-dimensional data sequence of a chart (corresponds to Charts.Samples
 * of WinUI 3).
 *
 * A series ([CartesianSeries]) receives its X values and Y values as one SampleModel each, and draws elements at the
 * same position as one data point (if X values are omitted, the element numbers 1, 2, 3, ... become X).
 * One model may be shared by multiple series (e.g. using month categories as the X values of all series).
 *
 * Element types and the axes used to display them:
 * - numbers ([Number], drawn as Double) — [LinearAxis]
 * - strings (category names) — [CategoryAxis]
 * - date-times (Instant / LocalDate / LocalDateTime / OffsetDateTime / ZonedDateTime of java.time, and java.util.Date) — [DateTimeAxis]
 *   (see [DateTimeAxis] for how dates are handled)
 *
 * When the contents of the model change, notify [SampleModelListener]s with a [SampleModelEvent]
 * (usually via the fire methods of [AbstractSampleModel]). A displayed chart redraws immediately on notification.
 * Make changes and notifications on the UI thread.
 */
interface SampleModel<E> {
    /** The number of elements. */
    fun getSize(): Int

    /** The [index]-th element. */
    fun getElementAt(index: Int): E

    /** Subscribes to the model's change notifications. */
    fun addSampleModelListener(listener: SampleModelListener)

    /** Removes a listener registered with [addSampleModelListener]. */
    fun removeSampleModelListener(listener: SampleModelListener)
}
