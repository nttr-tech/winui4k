package com.appkitbox.winui4k.ribbon

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.WComponent

/**
 * Common base of items placed in groups, toolbars, the Quick Access Toolbar and menus.
 *
 * Executing an item executes [command] (or, if there is none, [commandId] from the command catalog), and notifies the
 * listeners added with [addActionListener] and the item execution listeners of [com.appkitbox.winui4k.WRibbon].
 */
abstract class RibbonItemModel protected constructor(id: String?, label: String?) : RibbonNodeModel(id, label) {
    /** Preferred (largest) size. Also determines the default size definition when [sizeDefinition] is null. */
    var size: RibbonItemSize by observable(RibbonItemSize.MEDIUM) { old, new ->
        if (sizeDefinition == null) {
            firePropertyChange("effectiveSizeDefinition", RibbonSizeDefinition.forPreferredSize(old), RibbonSizeDefinition.forPreferredSize(new))
        }
    }

    /** Explicit adaptive size definition. If null, it is determined from [size]. */
    var sizeDefinition: RibbonSizeDefinition? by observable(null) { old, new ->
        firePropertyChange(
            "effectiveSizeDefinition",
            old ?: RibbonSizeDefinition.forPreferredSize(size),
            new ?: RibbonSizeDefinition.forPreferredSize(size),
        )
    }

    /** The size definition actually used. */
    val effectiveSizeDefinition: RibbonSizeDefinition
        get() = sizeDefinition ?: RibbonSizeDefinition.forPreferredSize(size)

    /** How the item is handled in the simplified ribbon. */
    var simplifiedVisibility: RibbonSimplifiedVisibility by observable(RibbonSimplifiedVisibility.AUTO)

    /** Whether to show the label next to the icon in the simplified ribbon (if null, only items whose preferred size is large show it). */
    var showLabelInSimplified: Boolean? by observable(null)

    /** Whether to show the label at medium and large sizes (false for icon-only toolbar buttons). */
    var showLabel: Boolean by observable(true)

    /** Id of a command registered in the command catalog. */
    var commandId: String? by observable(null)

    /** Command executed when the item is executed. */
    var command: RibbonCommand? by observable(null)

    /** Argument passed to [command]. */
    var commandParameter: Any? by observable(null)

    /** Keyboard shortcut to display (e.g. "Ctrl+B"). The view executes the item with this shortcut. */
    var shortcut: String? by observable(null)

    /** Whether it can be added with "Add to Quick Access Toolbar". */
    var canAddToQuickAccess: Boolean by observable(true)
}

/** A push button. */
open class RibbonButtonModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    command: RibbonCommand? = null,
    size: RibbonItemSize = RibbonItemSize.MEDIUM,
) : RibbonItemModel(id, label) {
    init {
        this.icon = icon
        this.command = command
        this.size = size
    }

    /** Whether to show the drop-down chevron. */
    var showChevron: Boolean by observable(false)
}

/** An on / off toggle button. Toggles with the same [groupName] behave like radio buttons. */
open class RibbonToggleButtonModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    command: RibbonCommand? = null,
    size: RibbonItemSize = RibbonItemSize.SMALL,
) : RibbonButtonModel(id, label, icon, command, size) {
    /** Checked state. */
    var isChecked: Boolean by observable(false)

    /** Name of the mutually exclusive set (e.g. "align" for align left / center / right). Scoped to the containing group, toolbar or ribbon. */
    var groupName: String? by observable(null)
}

/**
 * A button with a drop-down menu (it has no primary action).
 *
 * Besides [RibbonMenuItemModel], [RibbonMenuSeparatorModel] and [RibbonMenuHeaderModel], [menuItems] can contain
 * [RibbonGalleryModel], [RibbonColorPickerModel] (a palette), [RibbonGridPickerModel] (insert table) and
 * any [RibbonItemModel].
 */
open class RibbonDropDownButtonModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    size: RibbonItemSize = RibbonItemSize.MEDIUM,
) : RibbonItemModel(id, label) {
    init {
        this.icon = icon
        this.size = size
    }

    /** Items of the drop-down. */
    val menuItems: RibbonList<RibbonNodeModel> = RibbonList()

    /** Arbitrary content shown instead of (or above) the menu. */
    var dropDownContent: WComponent? by observable(null)
}

/** A button with a primary action and a drop-down part (Office's [Paste], [Bullets], [Shapes]). */
open class RibbonSplitButtonModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    command: RibbonCommand? = null,
    size: RibbonItemSize = RibbonItemSize.LARGE,
) : RibbonDropDownButtonModel(id, label, icon, size) {
    init {
        this.command = command
    }

    /** The primary action part becomes a toggle ([Bullets] / [Numbering]). */
    var isCheckable: Boolean by observable(false)

    /** Checked state when [isCheckable]. */
    var isChecked: Boolean by observable(false)

    /**
     * Choosing a menu item makes the primary action part take over that item's icon, label and action (a tool split
     * button). The chosen item is stored in [lastChoice] ([label] and [icon] are not rewritten).
     */
    var followLastChoice: Boolean by observable(false)

    /** The menu item last chosen with [followLastChoice] (shown and executed by the primary action part). */
    var lastChoice: RibbonMenuItemModel? by observable(null)

    /** Color bar below the icon ([Font Color], [Text Highlight Color]). Not shown if null. */
    var colorBar: RibbonColor? by observable(null)
}

/** A check box. */
open class RibbonCheckBoxModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    isChecked: Boolean = false,
) : RibbonItemModel(id, label) {
    init {
        size = RibbonItemSize.MEDIUM
    }

    /** Checked state (null is the indeterminate state). */
    var isChecked: Boolean? by observable(isChecked)

    /** Whether the indeterminate state can be used. */
    var isThreeState: Boolean by observable(false)
}

/** Determines the display text of items in combo boxes and the like (equivalent to DisplayMemberPath / ItemTextSelector). */
fun interface RibbonItemTextProvider {
    /** Display text of [item]. */
    fun getText(item: Any?): String
}

/**
 * Like javax.swing.ListCellRenderer: creates the components that draw a combo box's drop-down and selection box and
 * a gallery's items (equivalent to ItemTemplate / SelectionBoxTemplate). It must return a new component each time it
 * is called.
 */
fun interface RibbonItemRenderer {
    /** Creates a component that displays [item]. */
    fun createComponent(item: Any?): WComponent
}

/**
 * An editable / read-only combo box (font name, font size, number format, zoom).
 * Choosing an item or committing text executes it with that item (or the text, if editable and no item matches) as
 * the argument.
 */
open class RibbonComboBoxModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    items: Collection<Any> = emptyList(),
) : RibbonItemModel(id, label) {
    init {
        size = RibbonItemSize.SMALL
    }

    /** Selectable values. */
    val items: RibbonList<Any> = RibbonList(items)

    /** The selected value. */
    var selectedItem: Any? by observable(null)

    /** Text of the input box (committed with Enter, moving focus or selection). */
    var text: String? by observable(null)

    /** Whether any value can be entered (font size "11.5", zoom "137%"). */
    var isEditable: Boolean by observable(false)

    /** Width of the input box. */
    var inputWidth: Double by observable(120.0)

    /** Text shown when the value is empty or mixed (e.g. "*Mixed*"). */
    var placeholder: String? by observable(null)

    /** Draws each item in its own font (for choosing a font name). */
    var previewFontFamily: Boolean by observable(false)

    /** Maximum height of the drop-down. */
    var maxDropDownHeight: Double by observable(420.0)

    /** Display text of items (if null, toString, or the label for a [RibbonNodeModel]). */
    var itemTextProvider: RibbonItemTextProvider? by observable(null)

    /** How items are drawn (if null, the display text). Used for the drop-down and for the selection box when read-only. */
    var itemRenderer: RibbonItemRenderer? by observable(null)

    /** Display text of [item]. */
    fun getItemText(item: Any?): String = itemTextProvider?.getText(item) ?: when (item) {
        null -> ""
        is RibbonNodeModel -> item.label ?: item.id
        else -> item.toString()
    }
}

/** A font name combo box (draws each item in its own font; Office's [Font]). */
open class RibbonFontComboBoxModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    fonts: Collection<String> = DEFAULT_FONTS,
) : RibbonComboBoxModel(id, label, fonts) {
    init {
        previewFontFamily = true
        isEditable = true
        inputWidth = 150.0
    }

    companion object {
        /** Commonly used fonts. */
        @JvmField
        val DEFAULT_FONTS: List<String> = listOf(
            "Aptos", "Arial", "Bahnschrift", "Calibri", "Cambria", "Candara", "Consolas", "Constantia", "Corbel",
            "Courier New", "Georgia", "Segoe UI", "Segoe UI Variable", "Tahoma", "Times New Roman", "Trebuchet MS",
            "Verdana", "Yu Gothic UI", "Meiryo UI", "MS Gothic",
        )
    }
}

/** A font size combo box (Office's [Font Size]). */
open class RibbonFontSizeComboBoxModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    sizes: Collection<Double> = DEFAULT_SIZES,
) : RibbonComboBoxModel(id, label, sizes) {
    init {
        isEditable = true
        inputWidth = 52.0
        itemTextProvider = RibbonItemTextProvider { item ->
            if (item is Double && item == Math.floor(item)) item.toLong().toString() else item?.toString().orEmpty()
        }
    }

    companion object {
        /** Office's font sizes. */
        @JvmField
        val DEFAULT_SIZES: List<Double> =
            listOf(8.0, 9.0, 10.0, 10.5, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0, 28.0, 36.0, 48.0, 72.0)
    }
}

/**
 * A numeric spinner with a unit (indent, spacing, width, rotation). It can be changed with the arrow buttons, arrow
 * keys, the wheel or by dragging the label. Value changes are notified as property changes of [value], and user
 * commits as executions ([addActionListener], with the value as the argument).
 */
open class RibbonSpinnerModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    value: Double = 0.0,
) : RibbonItemModel(id, label) {
    init {
        size = RibbonItemSize.MEDIUM
    }

    /** Value (kept within [minimum]..[maximum]). */
    var value: Double by observable(value)

    /** Minimum value. */
    var minimum: Double by observable(0.0)

    /** Maximum value. */
    var maximum: Double by observable(100.0)

    /** Increment of the arrows, arrow keys and wheel (10 times for PageUp / PageDown). */
    var increment: Double by observable(1.0)

    /** Number format (a .NET format such as "0.##"; only 0, # and . are interpreted). */
    var format: String by observable("0.##")

    /** Unit ("pt", "cm", "\"", "°", "%"). */
    var unit: String? by observable(null)

    /** Width of the input box. */
    var inputWidth: Double by observable(64.0)

    /** Whether the value can be changed by dragging the label horizontally. */
    var isScrubEnabled: Boolean by observable(true)
}

/** Single-line text input (Enter executes it with the input text as the argument). */
open class RibbonTextBoxModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
) : RibbonItemModel(id, label) {
    init {
        size = RibbonItemSize.MEDIUM
    }

    /** Input text. */
    var text: String? by observable(null)

    /** Text shown when empty. */
    var placeholder: String? by observable(null)

    /** Width of the input box. */
    var inputWidth: Double by observable(140.0)
}

/** A slider (zoom, opacity, brush size). User interaction executes it with the value as the argument. */
open class RibbonSliderModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
) : RibbonItemModel(id, label) {
    init {
        size = RibbonItemSize.MEDIUM
    }

    /** Value. */
    var value: Double by observable(0.0)

    /** Minimum value. */
    var minimum: Double by observable(0.0)

    /** Maximum value. */
    var maximum: Double by observable(100.0)

    /** Step. */
    var stepFrequency: Double by observable(1.0)

    /** Width of the thumb's travel range. */
    var sliderWidth: Double by observable(120.0)
}

/** Static text. */
open class RibbonLabelModel @JvmOverloads constructor(id: String? = null, label: String? = null) : RibbonItemModel(id, label) {
    init {
        canAddToQuickAccess = false
    }
}

/** A separator between items (vertical; horizontal in vertical toolbars and menus). */
open class RibbonSeparatorModel : RibbonItemModel(null, null) {
    init {
        canAddToQuickAccess = false
    }
}

/** A set of small buttons laid out seamlessly ([Bold] / [Italic] / [Underline], alignment buttons). */
open class RibbonButtonGroupModel(id: String?, vararg items: RibbonItemModel) : RibbonItemModel(id, null) {
    /** A set without an id. */
    constructor(vararg items: RibbonItemModel) : this(null, *items)

    init {
        size = RibbonItemSize.SMALL
        canAddToQuickAccess = false
    }

    /** Child items. */
    val items: RibbonList<RibbonItemModel> = RibbonList(items.toList())
}

/**
 * A sequence of items that behaves as a single item. By default it is a single horizontal row, used for the rows of
 * [RibbonGroupItemsLayout.ROWS] groups ([Font] / [Paragraph]). Setting [orientation] to vertical stacks it in a column.
 */
open class RibbonRowModel(id: String?, vararg items: RibbonItemModel) : RibbonButtonGroupModel(id, *items) {
    /** A row without an id. */
    constructor(vararg items: RibbonItemModel) : this(null, *items)

    /** Direction to lay out in. */
    var orientation: Orientation by observable(Orientation.HORIZONTAL)

    /** Spacing between children. */
    var spacing: Double by observable(2.0)
}

/** Size of the table chosen with a [RibbonGridPickerModel]. */
data class RibbonGridSize(
    /** The number of rows. */
    val rows: Int,
    /** The number of columns. */
    val columns: Int,
) {
    override fun toString(): String = "${columns}x$rows"
}

/** A grid for choosing a size with the pointer (Office's [Insert Table]). The command receives a [RibbonGridSize]. */
open class RibbonGridPickerModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    command: RibbonCommand? = null,
) : RibbonItemModel(id, label) {
    init {
        this.command = command
    }

    /** The number of rows. */
    var rows: Int by observable(8, coerce = { maxOf(1, it) })

    /** The number of columns. */
    var columns: Int by observable(10, coerce = { maxOf(1, it) })
}

/** One segment of a [RibbonSegmentedModel]. */
open class RibbonSegmentModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    value: Any? = null,
) : RibbonNodeModel(id, label) {
    init {
        this.icon = icon
    }

    /** Value represented by the segment (if null, the id). */
    var value: Any? by observable(value ?: id)
}

/** A switch chosen by segments (such as switching workspaces; single selection). When the selection changes, it executes with the chosen segment's value as the argument. */
open class RibbonSegmentedModel @JvmOverloads constructor(id: String? = null, label: String? = null) : RibbonItemModel(id, label) {
    init {
        canAddToQuickAccess = false
    }

    /** Segments. */
    val segments: RibbonList<RibbonSegmentModel> = RibbonList()

    /** The selected segment. */
    var selectedSegment: RibbonSegmentModel? by observable(null)
}

/** An item that places an arbitrary component in a group. */
open class RibbonCustomItemModel @JvmOverloads constructor(
    id: String? = null,
    content: WComponent? = null,
) : RibbonItemModel(id, null) {
    init {
        canAddToQuickAccess = false
    }

    /** The component to place. */
    var content: WComponent? by observable(content)
}
