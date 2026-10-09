package com.appkitbox.winui4k.sample.ribbon.shell

import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonNodeModel
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel

// Helpers shared by the demo apps for building ribbon models concisely

/** A push button. */
fun button(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize = RibbonItemSize.MEDIUM): RibbonButtonModel =
    RibbonButtonModel(id, label, icon).also { it.size = size }

/** A toggle button. */
fun toggle(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize = RibbonItemSize.SMALL): RibbonToggleButtonModel =
    RibbonToggleButtonModel(id, label).also {
        it.icon = icon
        it.size = size
    }

/** A drop-down button ([entries] are the menu items). */
fun dropDown(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize, vararg entries: RibbonNodeModel): RibbonDropDownButtonModel =
    RibbonDropDownButtonModel(id, label).also { d ->
        d.icon = icon
        d.size = size
        entries.forEach { d.menuItems.add(it) }
    }

/** A split button ([entries] are the menu items). */
fun split(id: String, label: String, icon: RibbonIcon, size: RibbonItemSize, vararg entries: RibbonNodeModel): RibbonSplitButtonModel =
    RibbonSplitButtonModel(id, label).also { s ->
        s.icon = icon
        s.size = size
        entries.forEach { s.menuItems.add(it) }
    }

/** A group. */
fun group(id: String, label: String, vararg items: RibbonItemModel): RibbonGroupModel =
    RibbonGroupModel(id, label).also { g -> items.forEach { g.items.add(it) } }

/** A tab. */
fun tab(id: String, label: String, keyTip: String?, vararg groups: RibbonGroupModel): RibbonTabModel =
    RibbonTabModel(id, label).also { t ->
        t.keyTip = keyTip
        groups.forEach { t.groups.add(it) }
    }

/** A menu item. */
fun menuItem(id: String, label: String, icon: RibbonIcon? = null): RibbonMenuItemModel = RibbonMenuItemModel(id, label, icon)

/** A radio menu item. */
fun radioItem(id: String, label: String, group: String, checked: Boolean = false, icon: RibbonIcon? = null): RibbonMenuItemModel =
    RibbonMenuItemModel(id, label, icon).also {
        it.isCheckable = true
        it.groupName = group
        it.isChecked = checked
    }
