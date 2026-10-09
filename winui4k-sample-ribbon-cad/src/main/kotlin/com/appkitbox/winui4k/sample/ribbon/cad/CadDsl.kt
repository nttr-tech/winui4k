package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDropDownButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIconLayer
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRowModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonScreenTip
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSliderModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTextBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel

// Short builder functions for CAD ribbon items, used by CadRibbonTabs.kt (generated from XAML).

/** A CAD line-art icon (a value of [CadIcons]). */
internal fun ic(value: String): RibbonIcon = CadIcons.icon(value)

/** A CAD illustration (a value of [CadArt]; the grid is the header's viewbox, or 32 if absent). */
internal fun art(value: String): RibbonIcon = RibbonIcon.path(value, RibbonIconLayer.parseViewBox(value) ?: ART_GRID)

/** A command button. */
internal fun cmd(id: String, label: String, icon: RibbonIcon?, size: RibbonItemSize): RibbonButtonModel =
    RibbonButtonModel(id, label, icon).also { it.size = size }

/** A toggle button. */
internal fun tgl(id: String, label: String, icon: RibbonIcon?, size: RibbonItemSize): RibbonToggleButtonModel =
    RibbonToggleButtonModel(id, label, icon).also { it.size = size }

/** A split button (with [followLastChoice], the primary action becomes the last chosen menu item). */
@Suppress("LongParameterList") // the parameters map 1:1 to the XAML attributes of RibbonSpace's RibbonSplitButton
internal fun spl(id: String, label: String, icon: RibbonIcon?, size: RibbonItemSize, followLastChoice: Boolean, vararg entries: RibbonNodeModel): RibbonSplitButtonModel =
    RibbonSplitButtonModel(id, label, icon).also { split ->
        split.size = size
        split.followLastChoice = followLastChoice
        split.menuItems.addAll(entries)
    }

/** A drop-down button. */
internal fun ddn(id: String, label: String, icon: RibbonIcon?, size: RibbonItemSize, vararg entries: RibbonNodeModel): RibbonDropDownButtonModel =
    RibbonDropDownButtonModel(id, label, icon, size).also { it.menuItems.addAll(entries) }

/** A menu item. */
internal fun mi(id: String, label: String, icon: RibbonIcon?): RibbonMenuItemModel = RibbonMenuItemModel(id, label, icon)

/** A menu separator. */
internal fun sep(): RibbonMenuSeparatorModel = RibbonMenuSeparatorModel()

/** A stack of items (a vertical column if [vertical], otherwise a horizontal row). */
internal fun stack(vertical: Boolean, vararg items: RibbonItemModel): RibbonRowModel =
    RibbonRowModel(*items).also { if (vertical) it.orientation = Orientation.VERTICAL }

/** A combo box (with [selected] selected). */
@Suppress("LongParameterList") // the parameters map 1:1 to the XAML attributes of RibbonSpace's RibbonComboBox
internal fun combo(id: String, label: String?, icon: RibbonIcon?, width: Double, editable: Boolean, selected: String?, vararg items: String): RibbonComboBoxModel =
    RibbonComboBoxModel(id, label, items.toList()).also { box ->
        box.icon = icon
        box.inputWidth = width
        box.isEditable = editable
        box.selectedItem = selected
        box.text = selected
        box.showLabel = label != null
    }

/** A font name combo box. */
internal fun fontCombo(id: String, selected: String, width: Double): RibbonFontComboBoxModel =
    RibbonFontComboBoxModel(id).also { box ->
        box.selectedItem = selected
        box.text = selected
        box.inputWidth = width
    }

/** A slider. */
internal fun slider(id: String, label: String, icon: RibbonIcon?, width: Double, value: Double): RibbonSliderModel =
    RibbonSliderModel(id, label).also { s ->
        s.icon = icon
        s.sliderWidth = width
        s.value = value
    }

/** A spinner. */
internal fun spinner(id: String, label: String, width: Double, value: Double, unit: String?): RibbonSpinnerModel =
    RibbonSpinnerModel(id, label, value).also { s ->
        s.inputWidth = width
        s.unit = unit
        s.maximum = SPINNER_MAX
    }

/** A text input box. */
internal fun textBox(id: String, icon: RibbonIcon?, width: Double): RibbonTextBoxModel =
    RibbonTextBoxModel(id).also { box ->
        box.icon = icon
        box.inputWidth = width
    }

/** A gallery (with the first item selected). */
internal fun gallery(id: String, label: String, icon: RibbonIcon?, size: RibbonItemSize, vararg items: RibbonGalleryItemModel): RibbonGalleryModel =
    RibbonGalleryModel(id, label).also { g ->
        g.icon = icon
        g.size = size
        g.items.addAll(items)
        g.selectedItem = items.firstOrNull()
    }

/** A gallery item. */
internal fun gi(label: String, icon: RibbonIcon?, category: String?): RibbonGalleryItemModel = RibbonGalleryItemModel(label, label, icon, category)

/** A description-only ScreenTip (the title is the item's label). */
internal fun tip(description: String): RibbonScreenTip = RibbonScreenTip(null, description)

/** A progressive ScreenTip (shows a detailed description and an illustration after hovering for a while). */
internal fun richTip(title: String?, description: String, extendedDescription: String?, extendedImage: RibbonIcon?, helpText: String?): RibbonScreenTip =
    RibbonScreenTip(title, description).also { tip ->
        tip.extendedDescription = extendedDescription
        tip.extendedImage = extendedImage
        tip.helpText = helpText
    }

private const val ART_GRID = 32.0
private const val SPINNER_MAX = 360.0
