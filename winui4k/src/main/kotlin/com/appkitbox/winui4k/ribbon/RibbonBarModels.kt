package com.appkitbox.winui4k.ribbon

/**
 * Model of a command bar placed outside the ribbon (a horizontal command bar, a tool options bar, a vertical tool
 * palette or an activity rail). Items that do not fit move to the […] overflow menu.
 */
open class RibbonToolBarModel @JvmOverloads constructor(
    /** Identifier. */
    val id: String? = null,
) : RibbonObservable() {
    /** Items. */
    val items: RibbonList<RibbonItemModel> = RibbonList()

    /** Direction to lay out in. */
    var orientation: RibbonToolBarOrientation by observable(RibbonToolBarOrientation.HORIZONTAL)

    /** Number of columns of a vertical toolbar (tool palette: 1 or 2). */
    var columns: Int by observable(1, coerce = { it.coerceIn(1, 2) })

    /**
     * Whether item labels can be shown. If false, all items become small icon-only items. If true, they become
     * medium and show labels next to their icons according to each item's [RibbonItemModel.showLabelInSimplified].
     */
    var showLabels: Boolean by observable(false)

    /** Whether to move items that do not fit to the overflow menu (if false, they are clipped). */
    var isOverflowEnabled: Boolean by observable(true)

    /** Density. */
    var density: RibbonDensity by observable(RibbonDensity.COMFORTABLE)

    /** Command catalog that resolves commandId (if null, the ribbon's catalog is used). */
    var commandCatalog: RibbonCommandCatalog? by observable(null)
}

/** Model of the status bar (page count, word count, view switching and zoom). */
open class RibbonStatusBarModel : RibbonObservable() {
    /** Items at the left end. */
    val items: RibbonList<RibbonItemModel> = RibbonList()

    /** Items at the right end. */
    val endItems: RibbonList<RibbonItemModel> = RibbonList()

    /** Whether to show labels next to the icons (the CAD status bar shows icons only). */
    var showLabels: Boolean by observable(true)

    /** Command catalog that resolves commandId (if null, the ribbon's catalog is used). */
    var commandCatalog: RibbonCommandCatalog? by observable(null)
}

/**
 * The Office status bar zoom ([-] slider [+] 100%). It is executed with the value as its argument only on user
 * interaction (buttons or slider), not when [value] is changed from code. The percentage button is received with
 * [addZoomDialogListener].
 */
open class RibbonZoomModel @JvmOverloads constructor(id: String? = null, value: Double = 100.0) : RibbonItemModel(id, null) {
    init {
        canAddToQuickAccess = false
    }

    /** Value (a percentage, kept within [minimum]..[maximum]). */
    var value: Double by observable(value)

    /** Minimum value. */
    var minimum: Double by observable(10.0)

    /** Maximum value. */
    var maximum: Double by observable(500.0)

    /** Increment of the [-] / [+] buttons. */
    var step: Double by observable(10.0)

    private val zoomDialogListeners = mutableListOf<Runnable>()

    /** Subscribes to the percentage button (which opens the zoom dialog). */
    fun addZoomDialogListener(listener: Runnable) {
        zoomDialogListeners += listener
    }

    /** Unsubscribes a listener added with [addZoomDialogListener]. */
    fun removeZoomDialogListener(listener: Runnable) {
        zoomDialogListeners -= listener
    }

    /** Notifies that the percentage button was pressed (called by the view). */
    internal fun fireZoomDialogRequested() {
        zoomDialogListeners.toList().forEach { it.run() }
    }

    /** [value] clamped to the range. */
    fun clamp(value: Double): Double = if (maximum >= minimum) value.coerceIn(minimum, maximum) else minimum
}

/** One menu of a classic menu bar ([File], [Edit]). */
open class RibbonMenuBarItemModel @JvmOverloads constructor(id: String? = null, label: String? = null) : RibbonNodeModel(id, label) {
    /** Items of the menu (menu items, separators and headers). */
    val items: RibbonList<RibbonNodeModel> = RibbonList()

    private val openingListeners = mutableListOf<Runnable>()

    /** Subscribes to just before the menu opens (a chance to update the enabled and checked states). */
    fun addOpeningListener(listener: Runnable) {
        openingListeners += listener
    }

    /** Unsubscribes a listener added with [addOpeningListener]. */
    fun removeOpeningListener(listener: Runnable) {
        openingListeners -= listener
    }

    /** Notifies that the menu is about to open (called by the view). */
    internal fun fireOpening() {
        openingListeners.toList().forEach { it.run() }
    }
}

/** Model of a classic menu bar ([File] [Edit] [View] ...). */
open class RibbonMenuBarModel : RibbonObservable() {
    /** Menus. */
    val items: RibbonList<RibbonMenuBarItemModel> = RibbonList()
}

/**
 * A command of the application menu (AutoCAD's menu browser, Office 2007's application menu).
 * Items with subcommands show them on the right when hovered ([Save As] → [Drawing], [Template]).
 */
open class RibbonApplicationMenuItemModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
) : RibbonNodeModel(id, label) {
    init {
        this.icon = icon
    }

    /** Command (items with subcommands open their subcommands). */
    var command: RibbonCommand? by observable(null)

    /** Id in the command catalog. */
    var commandId: String? by observable(null)

    /** The command parameter. */
    var commandParameter: Any? by observable(null)

    /** Whether to draw a separator above the item. */
    var hasSeparatorBefore: Boolean by observable(false)

    /** Subcommands. */
    val items: RibbonList<RibbonApplicationMenuItemModel> = RibbonList()
}

/** A recent document in the application menu. */
open class RibbonRecentItemModel @JvmOverloads constructor(
    id: String? = null,
    /** Title of the document (file name). */
    label: String? = null,
    path: String? = null,
) : RibbonNodeModel(id, label) {
    /** Location shown below the title. */
    var path: String? by observable(path)

    /** Pinned documents stay at the top of the list. */
    var isPinned: Boolean by observable(false)

    /** Command executed to open it (the argument is the item). */
    var command: RibbonCommand? by observable(null)
}

/**
 * Model of the application menu (AutoCAD's menu browser): command search, commands on the left (subcommands on the
 * right), pinnable recent documents and buttons at the bottom ([Options], [Exit]).
 */
open class RibbonApplicationMenuModel : RibbonObservable() {
    /** Commands on the left. */
    val items: RibbonList<RibbonApplicationMenuItemModel> = RibbonList()

    /** Recent documents (pinned ones first). */
    val recentItems: RibbonList<RibbonRecentItemModel> = RibbonList()

    /** Buttons at the bottom. */
    val footerItems: RibbonList<RibbonItemModel> = RibbonList()

    /** Header of the recent documents (if null, the default text). */
    var recentHeader: String? by observable(null)

    /** Whether to show the command search box. */
    var isSearchVisible: Boolean by observable(true)
}
