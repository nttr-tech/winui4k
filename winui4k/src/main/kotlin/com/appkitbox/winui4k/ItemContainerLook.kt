package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The implementation of the item look (JList's selectionBackground / fixedCellHeight) of [WList] (ListView) / [WListBox] (ListBox).
 *
 * - Selection background: puts the lightweight styling keys the item template looks up ([selectionKeys]; ListViewItemBackgroundSelected etc.,
 *   three of them: selected, selected with the pointer over it, and selected and pressed) into the list's FrameworkElement.Resources
 * - Item height: Height / MinHeight of the ItemContainerStyle
 *
 * The selection color of ListViewItem is an attribute of the template's ListViewItemPresenter (`{ThemeResource ListViewItemBackgroundSelected}`),
 * resolved only once when the template is applied. So after putting in the keys, the ItemContainerStyle is replaced with a new Style to
 * make the items be recreated (ItemsControl::OnItemContainerStyleChanged → RefreshContainers). The Style is BasedOn the default item Style,
 * so the theme look stays. When both are back to their defaults, the ItemContainerStyle is removed.
 */
internal class ItemContainerLook(
    private val owner: WControl,
    /** The type of the item container (the Style's TargetType; "ListViewItem" / "ListBoxItem"). */
    private val containerType: String,
    /** The lightweight styling keys of the selection background. */
    private val selectionKeys: StateColorKeys,
    /** Called when [fixedCellHeight] changes (to decide the visibleRowCount height again). */
    private val onCellHeightChanged: () -> Unit,
) {
    /** The background color of selected items. null for the theme default. */
    var selectionBackground: WColor? = null
        set(value) {
            if (field == value) return
            field = value
            if (value == null) selectionKeys.remove(owner) else selectionKeys.put(owner, value)
            applyStyle()
        }

    /** The item height (DIP). Not decided if negative (decided by the item content and the theme's minimum height). */
    var fixedCellHeight: Int = -1
        set(value) {
            if (field == value) return
            field = value
            applyStyle()
            onCellHeightChanged()
        }

    private fun applyStyle() {
        val itemsControl = owner.inspectable.queryInterface(XamlInterop.IID_IItemsControl)
        try {
            if (selectionBackground == null && fixedCellHeight < 0) {
                itemsControl.call(XamlInterop.IItemsControl_put_ItemContainerStyle, null)
                return
            }
            val loaded = Xaml.load(styleXaml())
            val style = try {
                loaded.queryInterface(XamlInterop.IID_IStyle)
            } finally {
                loaded.release()
            }
            try {
                // Use a new Style every time (with the same Style the items are not recreated and the template's selection color is not resolved again)
                itemsControl.call(XamlInterop.IItemsControl_put_ItemContainerStyle, style.ptr)
            } finally {
                style.release()
            }
        } finally {
            itemsControl.release()
        }
    }

    private fun styleXaml(): String {
        val height = fixedCellHeight
        val setters = if (height >= 0) "<Setter Property=\"MinHeight\" Value=\"$height\"/><Setter Property=\"Height\" Value=\"$height\"/>" else ""
        return "<Style TargetType=\"$containerType\" BasedOn=\"{StaticResource Default${containerType}Style}\">$setters</Style>"
    }

    companion object {
        /** The selection background keys of ListViewItem (ListViewItem_themeresources.xaml). The factors are 1.0 / 0.9 / 0.8, the same as the button background. */
        val LIST_VIEW_SELECTION = StateColorKeys.of("ListViewItemBackgroundSelected", SELECTION_BACKGROUND_SHADES)

        /** The selection background keys of ListBoxItem (ListBox_themeresources.xaml). */
        val LIST_BOX_SELECTION = StateColorKeys.of("ListBoxItemBackgroundSelected", SELECTION_BACKGROUND_SHADES)
    }
}

/** The background factors for selected, selected with the pointer over it, and selected and pressed (the state name appended to the key name, and the opacity factor). */
internal val SELECTION_BACKGROUND_SHADES = mapOf("" to 1.0, "PointerOver" to POINTER_OVER_BACKGROUND_OPACITY, "Pressed" to PRESSED_BACKGROUND_OPACITY)
