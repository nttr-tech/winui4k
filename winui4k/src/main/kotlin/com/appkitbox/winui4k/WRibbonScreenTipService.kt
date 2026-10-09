package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonScreenTip

/**
 * The entry point for creating ribbon ScreenTips (rich tooltips; RibbonSpace's RibbonScreenTipService).
 * Tooltips with the same look can also be attached to any component outside the ribbon.
 */
object WRibbonScreenTipService {
    /** The delay (milliseconds) before showing the detailed description of progressive tooltips (AutoCAD). 0 shows it immediately. */
    @JvmStatic
    var extendedDelayMillis: Long
        get() = RibbonScreenTips.extendedDelayMillis
        set(value) {
            RibbonScreenTips.extendedDelayMillis = maxOf(0L, value)
        }

    /** Whether to show the extended description of progressive tooltips (AutoCAD's "Show extended ToolTips"). */
    @JvmStatic
    var isExtendedEnabled: Boolean
        get() = RibbonScreenTips.isExtendedEnabled
        set(value) {
            RibbonScreenTips.isExtendedEnabled = value
        }

    /**
     * Attaches a ribbon-style ScreenTip to [target] ([label] is the title, [tip] the details, [shortcut] the shortcut).
     * Removes the tooltip if there is no content at all.
     */
    @JvmStatic
    @JvmOverloads
    fun install(target: WComponent, label: String?, tip: RibbonScreenTip? = null, shortcut: String? = null) {
        RibbonThemeResources.ensure()
        Xaml.setToolTip(target.inspectable, RibbonScreenTips.create(label, tip, null, shortcut, true))
    }
}
