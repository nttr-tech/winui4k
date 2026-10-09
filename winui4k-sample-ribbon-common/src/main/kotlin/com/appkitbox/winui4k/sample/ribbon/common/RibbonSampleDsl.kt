package com.appkitbox.winui4k.sample.ribbon.common

import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDropDownButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel

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
