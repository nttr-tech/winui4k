package com.appkitbox.winui4k.table

import java.util.Date
import java.util.regex.Pattern

/**
 * Like javax.swing.SortOrder: the sort direction (corresponds to WinUI's Tabular.SortDirection).
 */
enum class SortOrder {
    /** Ascending. */
    ASCENDING,

    /** Descending. */
    DESCENDING,

    /** Not sorted. */
    UNSORTED,
}

/**
 * Like javax.swing.RowFilter: a filter that selects the rows to show.
 *
 * In [com.appkitbox.winui4k.WTableView], it works as the predicate of TableViewSource.Filter, and rows for which
 * [include] returns false are not shown (they are not removed from the model).
 * Build one with [regexFilter] / [numberFilter] / [dateFilter] / [andFilter] / [orFilter] / [notFilter], or
 * implement [include].
 *
 * @param M the type of the model (usually [TableModel])
 * @param I the type of the row identifier (usually the model's row index, Int)
 */
abstract class RowFilter<M, I> {
    /** How [numberFilter] / [dateFilter] compare. */
    enum class ComparisonType {
        /** Before (less than) the given value. */
        BEFORE,

        /** After (greater than) the given value. */
        AFTER,

        /** Equal to the given value. */
        EQUAL,

        /** Not equal to the given value. */
        NOT_EQUAL,
    }

    /** The values of one row passed to a filter. */
    abstract class Entry<M, I> {
        /** The model of the row. */
        abstract fun getModel(): M

        /** The number of values (the column count). */
        abstract fun getValueCount(): Int

        /** The value at [index] (a model column index). */
        abstract fun getValue(index: Int): Any?

        /** The value at [index] as a string (null becomes an empty string). */
        open fun getStringValue(index: Int): String = getValue(index)?.toString() ?: ""

        /** The row identifier (the model's row index). */
        abstract fun getIdentifier(): I
    }

    /** Returns true to show the row of [entry]. */
    abstract fun include(entry: Entry<out M, out I>): Boolean

    companion object {
        /**
         * Keeps rows where the string of any of the columns [indices] (all columns if omitted) partially matches the
         * regular expression [regex]. For case-insensitive matching, prefix it with `(?i)`.
         */
        @JvmStatic
        fun <M, I> regexFilter(regex: String, vararg indices: Int): RowFilter<M, I> {
            val pattern = Pattern.compile(regex)
            return object : RowFilter<M, I>() {
                override fun include(entry: Entry<out M, out I>): Boolean =
                    columnsOf(entry, indices).any { pattern.matcher(entry.getStringValue(it)).find() }
            }
        }

        /**
         * Keeps rows where a number in any of the columns [indices] (all columns if omitted) has the relation [type] to
         * [number].
         */
        @JvmStatic
        fun <M, I> numberFilter(type: ComparisonType, number: Number, vararg indices: Int): RowFilter<M, I> =
            object : RowFilter<M, I>() {
                override fun include(entry: Entry<out M, out I>): Boolean = columnsOf(entry, indices).any { index ->
                    val value = entry.getValue(index) as? Number ?: return@any false
                    matches(type, value.toDouble().compareTo(number.toDouble()))
                }
            }

        /** Keeps rows where a date in any of the columns [indices] (all columns if omitted) has the relation [type] to [date]. */
        @JvmStatic
        fun <M, I> dateFilter(type: ComparisonType, date: Date, vararg indices: Int): RowFilter<M, I> =
            object : RowFilter<M, I>() {
                override fun include(entry: Entry<out M, out I>): Boolean = columnsOf(entry, indices).any { index ->
                    val value = entry.getValue(index) as? Date ?: return@any false
                    matches(type, value.compareTo(date))
                }
            }

        /** Keeps the rows that all of [filters] keep. */
        @JvmStatic
        fun <M, I> andFilter(filters: Iterable<RowFilter<in M, in I>>): RowFilter<M, I> = object : RowFilter<M, I>() {
            override fun include(entry: Entry<out M, out I>): Boolean = filters.all { includes(it, entry) }
        }

        /** Keeps the rows that any of [filters] keeps. */
        @JvmStatic
        fun <M, I> orFilter(filters: Iterable<RowFilter<in M, in I>>): RowFilter<M, I> = object : RowFilter<M, I>() {
            override fun include(entry: Entry<out M, out I>): Boolean = filters.any { includes(it, entry) }
        }

        /** Keeps the rows that [filter] does not keep. */
        @JvmStatic
        fun <M, I> notFilter(filter: RowFilter<M, I>): RowFilter<M, I> = object : RowFilter<M, I>() {
            override fun include(entry: Entry<out M, out I>): Boolean = !filter.include(entry)
        }

        private fun columnsOf(entry: Entry<*, *>, indices: IntArray): Iterable<Int> =
            if (indices.isEmpty()) 0 until entry.getValueCount() else indices.asIterable()

        private fun matches(type: ComparisonType, comparison: Int): Boolean = when (type) {
            ComparisonType.BEFORE -> comparison < 0
            ComparisonType.AFTER -> comparison > 0
            ComparisonType.EQUAL -> comparison == 0
            ComparisonType.NOT_EQUAL -> comparison != 0
        }

        /** Passes [entry] to a [filter] with contravariant type arguments (safe because Entry is read-only). */
        @Suppress("UNCHECKED_CAST")
        private fun <M, I> includes(filter: RowFilter<in M, in I>, entry: Entry<out M, out I>): Boolean =
            (filter as RowFilter<M, I>).include(entry)
    }
}
