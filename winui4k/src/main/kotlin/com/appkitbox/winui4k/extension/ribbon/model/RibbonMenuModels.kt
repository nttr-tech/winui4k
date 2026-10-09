package com.appkitbox.winui4k.extension.ribbon.model

/** A menu item of a drop-down, split button, or context menu. */
open class RibbonMenuItemModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    command: RibbonCommand? = null,
) : RibbonNodeModel(id, label) {
    init {
        this.icon = icon
    }

    /** The id of the command resolved through the command catalog. */
    var commandId: String? by observable(null)

    /** The command. */
    var command: RibbonCommand? by observable(command)

    /** The command parameter. */
    var commandParameter: Any? by observable(null)

    /** Draws the item as a check item (a radio item if it has a [groupName]). */
    var isCheckable: Boolean by observable(false)

    /** Checked state. */
    var isChecked: Boolean by observable(false)

    /** The name of the radio group. */
    var groupName: String? by observable(null)

    /** The shortcut text shown at the right edge. */
    var shortcut: String? by observable(null)

    /** The items of the submenu. */
    val items: RibbonList<RibbonNodeModel> = RibbonList()
}

/** A menu separator. */
class RibbonMenuSeparatorModel : RibbonNodeModel(null, null)

/** A non-interactive menu header ("Paste Options:", "Recently Used Shapes"). */
class RibbonMenuHeaderModel(label: String) : RibbonNodeModel(null, label)
