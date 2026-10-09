package com.appkitbox.winui4k.ribbon

import com.appkitbox.winui4k.WComponent
import java.util.EventObject

/**
 * An item of a [RibbonGalleryModel].
 *
 * Its appearance is determined in this order of priority: the gallery's [RibbonGalleryModel.itemRenderer] → [content] →
 * a text sample ([previewText] and the preview properties; Word's [Styles]) → [icon] and label.
 */
open class RibbonGalleryItemModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    category: String? = null,
    value: Any? = null,
) : RibbonNodeModel(id, label) {
    init {
        this.icon = icon
    }

    /** Category used to group items in the expanded gallery. */
    var category: String? by observable(category)

    /** Component drawn as the sample. */
    var content: WComponent? by observable(null)

    /** Value passed to the gallery's command (if null, the item itself). */
    var value: Any? by observable(value)

    /** Text sample ("AaBbCcDd"). */
    var previewText: String? by observable(null)

    /** Color of the text sample. */
    var previewForeground: RibbonColor? by observable(null)

    /** Background of the sample (color swatches, themes). */
    var previewBackground: RibbonColor? by observable(null)

    /** Font of the text sample. */
    var previewFontFamily: String? by observable(null)

    /** Font size of the text sample. */
    var previewFontSize: Double by observable(14.0)

    /** Whether the text sample is bold. */
    var previewBold: Boolean by observable(false)

    /** Whether the text sample is italic. */
    var previewItalic: Boolean by observable(false)
}

/** Live preview of a gallery item (if [item] is null, the preview has ended). */
class RibbonGalleryPreviewEvent(
    source: RibbonGalleryModel,
    /** The item under the pointer (null when the preview ends). */
    val item: RibbonGalleryItemModel?,
) : EventObject(source) {
    /** The gallery being previewed. */
    override fun getSource(): RibbonGalleryModel = super.getSource() as RibbonGalleryModel
}

/** Receives live previews of a gallery. */
fun interface RibbonGalleryPreviewListener {
    /** Called when the pointer enters or leaves an item. */
    fun preview(event: RibbonGalleryPreviewEvent)
}

/**
 * A gallery in the ribbon ([Styles], [Shape Styles], [Themes], [Transitions], [Chart Styles]).
 * The number of columns follows the group's width; it can expand into a popup with categories and a filter, and has
 * menu commands in its footer. Selecting an item executes it with that item's [RibbonGalleryItemModel.value] (or the
 * item if there is none) as the argument. Hovering over an item triggers live preview ([previewCommand] and
 * [addPreviewListener]).
 */
open class RibbonGalleryModel @JvmOverloads constructor(id: String? = null, label: String? = null) : RibbonItemModel(id, label) {
    init {
        size = RibbonItemSize.LARGE
    }

    /** Items. */
    val items: RibbonList<RibbonGalleryItemModel> = RibbonList()

    /** Menu items in the footer of the expanded gallery ("Clear Formatting", "Apply Styles..."). */
    val menuItems: RibbonList<RibbonNodeModel> = RibbonList()

    /** The selected item. */
    var selectedItem: RibbonGalleryItemModel? by observable(null)

    /** Live preview command: executed with the value of the item under the pointer, and with null when the pointer leaves. */
    var previewCommand: RibbonCommand? by observable(null)

    /** Number of columns shown in the ribbon when the group is in the medium state. */
    var minColumns: Int by observable(3, coerce = { maxOf(1, it) })

    /** Number of columns shown in the ribbon when the group is in the large state. */
    var maxColumns: Int by observable(7, coerce = { maxOf(1, it) })

    /** Number of columns in the expanded popup. */
    var dropDownColumns: Int by observable(7, coerce = { maxOf(1, it) })

    /** Width of an item. */
    var itemWidth: Double by observable(72.0)

    /** Height of an item. */
    var itemHeight: Double by observable(60.0)

    /** Whether to show a filter box in the expanded popup. */
    var isFilterEnabled: Boolean by observable(false)

    /** Number of rows shown in the ribbon. */
    var rows: Int by observable(1, coerce = { maxOf(1, it) })

    /** Whether to show item labels below the samples. */
    var showLabels: Boolean by observable(true)

    /** Always shown as a drop-down button (not laid out in the ribbon). */
    var isDropDownOnly: Boolean by observable(false)

    /** Maximum height of the expanded popup. */
    var maxDropDownHeight: Double by observable(480.0)

    /** How items are drawn (if null, the default sample). */
    var itemRenderer: RibbonItemRenderer? by observable(null)

    private val previewListeners = mutableListOf<RibbonGalleryPreviewListener>()

    /** Subscribes to live previews. */
    fun addPreviewListener(listener: RibbonGalleryPreviewListener) {
        previewListeners += listener
    }

    /** Removes a listener registered with [addPreviewListener]. */
    fun removePreviewListener(listener: RibbonGalleryPreviewListener) {
        previewListeners -= listener
    }

    /** Notifies a live preview and executes [previewCommand] (called by the view). */
    internal fun firePreview(item: RibbonGalleryItemModel?) {
        val command = previewCommand
        val parameter = item?.let { it.value ?: it }
        if (command != null && command.canExecute(parameter)) command.execute(parameter)
        if (previewListeners.isEmpty()) return
        val event = RibbonGalleryPreviewEvent(this, item)
        for (listener in previewListeners.toList()) listener.preview(event)
    }
}

/** Live preview of a color (if [color] is null, the preview has ended or it is "No Color"). */
class RibbonColorPreviewEvent(
    source: RibbonColorPickerModel,
    /** The color under the pointer. */
    val color: RibbonColor?,
    /** Whether the preview has ended. */
    val isEnded: Boolean,
) : EventObject(source) {
    /** The color picker being previewed. */
    override fun getSource(): RibbonColorPickerModel = super.getSource() as RibbonColorPickerModel
}

/** Receives live previews of colors. */
fun interface RibbonColorPreviewListener {
    /** Called when the pointer enters or leaves a color swatch. */
    fun preview(event: RibbonColorPreviewEvent)
}

/**
 * Office's color picker ([Font Color], [Text Highlight Color], [Shape Fill]): Automatic, the theme color grid, the
 * standard colors, recent colors and [More Colors]. Choosing a color executes it with that color ("No Color" is null)
 * as the argument. When put in the menu items of a drop-down ([RibbonDropDownButtonModel.menuItems]), it is drawn as
 * a palette.
 */
open class RibbonColorPickerModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    command: RibbonCommand? = null,
) : RibbonItemModel(id, label) {
    init {
        this.icon = icon
        this.command = command
        size = RibbonItemSize.SMALL
    }

    /** The selected color (null means "No Color"). */
    var selectedColor: RibbonColor? by observable(RibbonColor.parse("#FF0000"))

    /** Whether to show "Automatic". */
    var showAutomatic: Boolean by observable(true)

    /** The automatic color. */
    var automaticColor: RibbonColor by observable(RibbonColor.BLACK)

    /** Whether to show "No Color". */
    var showNoColor: Boolean by observable(false)

    /** Whether to show "More Colors...". */
    var showMoreColors: Boolean by observable(true)

    /** Whether to make it a split button (clicking applies the current color). If false, clicking opens the palette. */
    var isSplit: Boolean by observable(true)

    /** Base colors of the theme (tints and shades are generated automatically). If empty, the Office theme. */
    val themeColors: RibbonList<RibbonColorSwatch> = RibbonList()

    /** Standard colors. If empty, Office's standard colors. */
    val standardColors: RibbonList<RibbonColorSwatch> = RibbonList()

    /** Recent colors (updated automatically; up to 10). */
    val recentColors: RibbonList<RibbonColor> = RibbonList()

    private val previewListeners = mutableListOf<RibbonColorPreviewListener>()

    /** Subscribes to live previews of colors. */
    fun addPreviewListener(listener: RibbonColorPreviewListener) {
        previewListeners += listener
    }

    /** Removes a listener registered with [addPreviewListener]. */
    fun removePreviewListener(listener: RibbonColorPreviewListener) {
        previewListeners -= listener
    }

    /** Notifies a live preview of a color (called by the view). */
    internal fun firePreview(color: RibbonColor?, isEnded: Boolean) {
        if (previewListeners.isEmpty()) return
        val event = RibbonColorPreviewEvent(this, color, isEnded)
        for (listener in previewListeners.toList()) listener.preview(event)
    }

    /** Puts [color] at the top of the recent colors (up to 10). */
    internal fun rememberRecent(color: RibbonColor) {
        val index = recentColors.indexOf(color)
        if (index == 0) return
        if (index > 0) recentColors.removeAt(index)
        recentColors.add(0, color)
        while (recentColors.size > MAX_RECENT_COLORS) recentColors.removeAt(recentColors.size - 1)
    }

    internal companion object {
        /** Maximum number of recent colors. */
        const val MAX_RECENT_COLORS = 10
    }
}
