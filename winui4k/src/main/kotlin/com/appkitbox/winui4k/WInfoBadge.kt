package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * WinUI 3's InfoBadge (a notification dot / count badge). No Swing equivalent, so we keep
 * WinUI's class name as-is. Overlaid on a NavigationViewItem or a button to unobtrusively
 * show an unread count or draw attention.
 *
 * Setting [value] to 0 or greater shows a numeric badge; the default -1 shows a small dot
 * badge instead. Use [setSymbolIcon] to show an icon (takes priority over value).
 */
class WInfoBadge : WControl(
    Activation.composeDefault(Abi.CLS_InfoBadge, Abi.IID_IInfoBadgeFactory), // default interface = IInfoBadge
) {
    /**
     * The number shown on the badge (InfoBadge.Value). 0 or greater is a numeric badge, -1 is a
     * dot badge. Ignored once an icon badge ([setSymbolIcon]) has been set.
     */
    var value: Int
        get() = inspectable.getInt(Abi.IInfoBadge_get_Value)
        set(value) = inspectable.call(Abi.IInfoBadge_put_Value, value)

    /** Turns this into an icon badge (InfoBadge.IconSource). Creates and passes a SymbolIconSource. */
    fun setSymbolIcon(symbol: Symbol) {
        val iconSource = symbol.createIconSource()
        try {
            inspectable.call(Abi.IInfoBadge_put_IconSource, iconSource.ptr)
        } finally {
            iconSource.release()
        }
    }
}
