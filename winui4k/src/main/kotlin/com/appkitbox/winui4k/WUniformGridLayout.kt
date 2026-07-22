package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.UniformGridLayoutItemsJustification
 * (how items are aligned when there's leftover space along the row/column axis).
 * Values extracted from the winmd (Start=0, Center=1, End=2, SpaceAround=3, SpaceBetween=4, SpaceEvenly=5).
 */
enum class UniformGridLayoutItemsJustification(internal val native: Int) {
    /** Aligned to the start (default). */
    START(0),

    /** Centered. */
    CENTER(1),

    /** Aligned to the end. */
    END(2),

    /** Even space around each item's both sides. */
    SPACE_AROUND(3),

    /** Even space only between items. */
    SPACE_BETWEEN(4),

    /** Even space between items and at both ends. */
    SPACE_EVENLY(5),
    ;

    internal companion object {
        fun of(native: Int): UniformGridLayoutItemsJustification = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.UniformGridLayoutItemsStretch (how items grow into leftover width).
 * Values extracted from the winmd (None=0, Fill=1, Uniform=2).
 */
enum class UniformGridLayoutItemsStretch(internal val native: Int) {
    /** Don't grow. Leftover space remains at the end of the row (default). */
    NONE(0),

    /** Grow each item's width to fill the leftover space. */
    FILL(1),

    /** Grow while preserving the aspect ratio. */
    UNIFORM(2),
    ;

    internal companion object {
        fun of(native: Int): UniformGridLayoutItemsStretch = entries.first { it.native == native }
    }
}

/**
 * GridLayout-like: WinUI 3's UniformGridLayout.
 * Assign to [WItemsView.layout] to lay out every item in equally sized cells that wrap.
 * A cell's minimum size is [minItemWidth] / [minItemHeight]; spacing is [minColumnSpacing] / [minRowSpacing].
 */
class WUniformGridLayout {
    /** UniformGridLayout's default interface (IUniformGridLayout). */
    internal val inspectable: ComPtr =
        Activation.composeDefault(Abi.CLS_UniformGridLayout, Abi.IID_IUniformGridLayoutFactory)

    /** The record of COM references this wrapper owns (the same mechanism as WComponent). */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /** The ILayout view passed to ItemsView.Layout. */
    internal val layout: ComPtr by lazy { lifetime.own(inspectable.queryInterface(Abi.IID_ILayout)) }

    /** The direction items are laid out in (UniformGridLayout.Orientation). Defaults to horizontal (rows wrap). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(Abi.IUniformGridLayout_get_Orientation))
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_Orientation, value.native)

    /** The minimum width of a single item (UniformGridLayout.MinItemWidth). */
    var minItemWidth: Double
        get() = inspectable.getDouble(Abi.IUniformGridLayout_get_MinItemWidth)
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_MinItemWidth, value)

    /** The minimum height of a single item (UniformGridLayout.MinItemHeight). */
    var minItemHeight: Double
        get() = inspectable.getDouble(Abi.IUniformGridLayout_get_MinItemHeight)
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_MinItemHeight, value)

    /** The minimum spacing between rows (UniformGridLayout.MinRowSpacing). */
    var minRowSpacing: Double
        get() = inspectable.getDouble(Abi.IUniformGridLayout_get_MinRowSpacing)
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_MinRowSpacing, value)

    /** The minimum spacing between columns (UniformGridLayout.MinColumnSpacing). */
    var minColumnSpacing: Double
        get() = inspectable.getDouble(Abi.IUniformGridLayout_get_MinColumnSpacing)
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_MinColumnSpacing, value)

    /** How items are aligned when there's leftover space (UniformGridLayout.ItemsJustification). */
    var itemsJustification: UniformGridLayoutItemsJustification
        get() = UniformGridLayoutItemsJustification.of(inspectable.getInt(Abi.IUniformGridLayout_get_ItemsJustification))
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_ItemsJustification, value.native)

    /** How items grow into leftover width (UniformGridLayout.ItemsStretch). */
    var itemsStretch: UniformGridLayoutItemsStretch
        get() = UniformGridLayoutItemsStretch.of(inspectable.getInt(Abi.IUniformGridLayout_get_ItemsStretch))
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_ItemsStretch, value.native)

    /** The number of rows or columns before wrapping (UniformGridLayout.MaximumRowsOrColumns). -1 means unlimited (default). */
    var maximumRowsOrColumns: Int
        get() = inspectable.getInt(Abi.IUniformGridLayout_get_MaximumRowsOrColumns)
        set(value) = inspectable.call(Abi.IUniformGridLayout_put_MaximumRowsOrColumns, value)
}
