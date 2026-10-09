package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonCustomizePage
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonNodeModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import java.util.EventObject

/** A ribbon item (button, menu item, gallery, group dialog launcher, etc.) was invoked. */
class RibbonItemInvokedEvent @JvmOverloads constructor(
    source: WComponent,
    /** The model of the invoked item. */
    val item: RibbonNodeModel,
    /** The id of the item in the command catalog. */
    val commandId: String?,
    /** The invocation argument (a toggle's checked state, a combo selection, a color, etc.). */
    val parameter: Any?,
    /** The ribbon the item is connected to (null for a bar not connected to a ribbon). */
    val ribbon: WRibbon? = source as? WRibbon,
) : EventObject(source) {
    /** The origin (the ribbon, or a bar such as a toolbar or status bar). */
    override fun getSource(): WComponent = super.getSource() as WComponent
}

/** Receives item invocations (suited to handling commands in one place by string id). */
fun interface RibbonItemInvokedListener {
    /** An item was invoked. */
    fun itemInvoked(event: RibbonItemInvokedEvent)
}

/** The selected tab changed. */
class RibbonTabChangedEvent(
    source: WRibbon,
    /** The previous tab. */
    val oldTab: RibbonTabModel?,
    /** The new tab. */
    val newTab: RibbonTabModel?,
) : EventObject(source) {
    /** The ribbon the event originated from. */
    override fun getSource(): WRibbon = super.getSource() as WRibbon
}

/** Receives changes of the selected tab. */
fun interface RibbonTabChangeListener {
    /** The selected tab changed. */
    fun tabChanged(event: RibbonTabChangedEvent)
}

/** A ribbon event that can be canceled (its default action can be prevented). */
open class RibbonHandledEvent(source: WRibbon) : EventObject(source) {
    /** Setting this to true skips the default action. */
    var isHandled: Boolean = false

    /** The ribbon the event originated from. */
    override fun getSource(): WRibbon = super.getSource() as WRibbon
}

/** Receives clicks on the application ([File]) button. Use [RibbonHandledEvent.isHandled] to prevent Backstage or the menu. */
fun interface RibbonApplicationButtonListener {
    /** The application button was clicked. */
    fun applicationButtonClicked(event: RibbonHandledEvent)
}

/** The ribbon's display state (display mode, QAT, customization, floating panels, etc.) changed (suited to auto-saving the state). */
fun interface RibbonStateChangeListener {
    /** The state changed. */
    fun stateChanged(event: EventObject)
}

/** Receives KeyTips being shown and hidden. */
fun interface RibbonKeyTipModeListener {
    /** KeyTip mode was entered ([active] is true) or exited. */
    fun keyTipModeChanged(source: WRibbon, active: Boolean)
}

/** Just before a right-click menu opens (add or remove items in [menu], or use [isHandled] for a custom menu). */
class RibbonContextMenuEvent(
    source: WRibbon,
    /** The model of the right-clicked item, group or tab (null for empty space in the ribbon). */
    val target: RibbonNodeModel?,
    /** The menu to open. */
    val menu: WMenuFlyout,
) : RibbonHandledEvent(source)

/** Receives right-click menus. */
fun interface RibbonContextMenuListener {
    /** Just before the menu opens. */
    fun contextMenuOpening(event: RibbonContextMenuEvent)
}

/** The user requested ribbon / QAT customization (use [isHandled] to prevent the default dialog and show a custom UI). */
class RibbonCustomizeEvent(
    source: WRibbon,
    /** The page to open. */
    val page: RibbonCustomizePage,
) : RibbonHandledEvent(source)

/** Receives customization requests. */
fun interface RibbonCustomizeListener {
    /** Customization was requested. */
    fun customizeRequested(event: RibbonCustomizeEvent)
}

/** A group became a floating panel or returned to the ribbon. */
fun interface RibbonGroupFloatingListener {
    /** The floating state of [group] became [isFloating]. */
    fun groupFloatingChanged(source: WRibbon, group: RibbonGroupModel, isFloating: Boolean)
}

/** Receives Backstage opening and closing. */
interface RibbonBackstageListener {
    /** Backstage opened. */
    fun backstageOpened(source: WRibbon) {
        // Does nothing by default
    }

    /** Backstage closed. */
    fun backstageClosed(source: WRibbon) {
        // Does nothing by default
    }
}

/** A change in the QAT's position, visibility or items (whatever hosts the QAT, such as a title bar, redoes its layout). */
fun interface RibbonQuickAccessListener {
    /** The QAT changed. */
    fun quickAccessChanged(source: WRibbon)
}

/** Receives the command search shortcut (Alt+Q) (the search box moves the focus to itself). */
fun interface RibbonSearchRequestListener {
    /** Search was requested. */
    fun searchRequested(source: WRibbon)
}
