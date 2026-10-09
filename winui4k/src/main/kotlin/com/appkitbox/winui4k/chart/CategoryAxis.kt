package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.ChartsInterop

/**
 * The sort key of a category axis (Charts.CategorySortKey). Values are extracted from the winmd.
 */
enum class CategorySortKey(internal val native: Int) {
    /** The position in the data (default; with [SortOrder.ASCENDING] the model order is kept). */
    INDEX(0),

    /** The value for each category (the Y value), e.g. to sort a bar chart by descending value. */
    VALUE(1),
    ;

    internal companion object {
        fun of(native: Int): CategorySortKey = entries.first { it.native == native }
    }
}

/**
 * The sort direction of a category axis (Charts.SortOrder). Values are extracted from the winmd.
 */
enum class SortOrder(internal val native: Int) {
    /** Ascending. */
    ASCENDING(0),

    /** Descending. */
    DESCENDING(1),
    ;

    internal companion object {
        fun of(native: Int): SortOrder = entries.first { it.native == native }
    }
}

/**
 * Charts.CategoryAxis of WinUI 3: an axis that places string categories (month names, region names, etc.) at equal
 * intervals.
 *
 * The order of categories can be changed with [sortKey] and [sortOrder] (the order of the model does not change).
 */
class CategoryAxis @JvmOverloads constructor(label: String = "") : CartesianAxis(
    Activation.activate(ChartsInterop.CLS_CategoryAxis, ChartsInterop.IID_ICategoryAxis), // default interface = ICategoryAxis
) {
    /** ICategoryAxis (the default interface). */
    private val categoryAxis: ComPtr = inspectable

    /** The sort key (CategoryAxis.SortKey). */
    var sortKey: CategorySortKey
        get() = CategorySortKey.of(categoryAxis.getInt(ChartsInterop.ICategoryAxis_get_SortKey))
        set(value) {
            categoryAxis.call(ChartsInterop.ICategoryAxis_put_SortKey, value.native)
        }

    /** The sort direction (CategoryAxis.SortOrder). */
    var sortOrder: SortOrder
        get() = SortOrder.of(categoryAxis.getInt(ChartsInterop.ICategoryAxis_get_SortOrder))
        set(value) {
            categoryAxis.call(ChartsInterop.ICategoryAxis_put_SortOrder, value.native)
        }

    init {
        if (label.isNotEmpty()) this.label = label
    }
}
