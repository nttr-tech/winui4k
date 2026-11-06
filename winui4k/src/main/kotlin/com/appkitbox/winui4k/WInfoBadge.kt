package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * WinUI 3's InfoBadge (a notification dot / count badge). No Swing equivalent, so we keep
 * WinUI's class name as-is. Overlaid on a NavigationViewItem or a button to unobtrusively
 * show an unread count or draw attention.
 *
 * Setting [value] to 0 or greater shows a numeric badge; the default -1 shows a small dot
 * badge instead. To show an icon, set [icon] (or [setSymbolIcon]) and make [value] -1 (a number takes priority when 0 or greater).
 */
class WInfoBadge : WControl(
    Activation.composeDefault(XamlInterop.CLS_InfoBadge, XamlInterop.IID_IInfoBadgeFactory), // default interface = IInfoBadge
) {
    /**
     * The number shown on the badge (InfoBadge.Value). 0 or greater gives a numeric badge (the number is shown even if an icon is set), and -1 a dot badge
     * (an icon badge if an icon is set). Values smaller than -1 throw [IllegalArgumentException], and the value does not change.
     */
    var value: Int
        get() = inspectable.getInt(XamlInterop.IInfoBadge_get_Value)
        set(value) {
            // WinUI rejects values below -1 with E_BOUNDS, but the value has already been stored by then (InfoBadge::OnPropertyChanged). Reject it first
            require(value >= -1) { "value must be -1 or more (-1 is a dot badge): $value" }
            inspectable.call(XamlInterop.IInfoBadge_put_Value, value)
        }

    /**
     * The icon of an icon badge (InfoBadge.IconSource). If null (default), a numeric or dot badge from [value].
     * An icon set with [setSymbolIcon] can also be read here.
     */
    var icon: WIcon? = null
        set(value) {
            val source = value?.createIconSource()
            try {
                inspectable.call(XamlInterop.IInfoBadge_put_IconSource, source?.ptr)
            } finally {
                source?.release()
            }
            field = value
        }

    /** Makes it an icon badge (InfoBadge.IconSource). Same as setting [symbol] to [icon]. */
    fun setSymbolIcon(symbol: Symbol) {
        icon = symbol
    }
}
