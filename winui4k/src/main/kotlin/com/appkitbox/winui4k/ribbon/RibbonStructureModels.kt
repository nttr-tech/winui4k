package com.appkitbox.winui4k.ribbon

import com.appkitbox.winui4k.WComponent

/**
 * A ribbon group ([Clipboard], [Font], [Paragraph]).
 *
 * The dialog box launcher (the small arrow at the bottom right of the caption) appears when
 * [dialogLauncherCommand] / [dialogLauncherCommandId] is set or [isDialogLauncherVisible] is true. Clicking it is
 * notified as an execution of the group ([addActionListener]).
 */
open class RibbonGroupModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
) : RibbonNodeModel(id, label) {
    init {
        this.icon = icon
    }

    /** Items. */
    val items: RibbonList<RibbonItemModel> = RibbonList()

    /**
     * Less frequently used commands placed in the expanded part (slide-out) opened from the arrow in the panel title
     * (AutoCAD's expanded panel).
     */
    val slideOutItems: RibbonList<RibbonItemModel> = RibbonList()

    /** How the items are arranged. */
    var itemsLayout: RibbonGroupItemsLayout by observable(RibbonGroupItemsLayout.COLUMNS)

    /** The number of medium and small items stacked in one column (1..3). */
    var rowCount: Int by observable(3, coerce = { it.coerceIn(1, 3) })

    /** The command of the dialog box launcher. */
    var dialogLauncherCommand: RibbonCommand? by observable(null)

    /** The command catalog id of the dialog box launcher. */
    var dialogLauncherCommandId: String? by observable(null)

    /** Whether to show the dialog box launcher even without a command (receive clicks with [addActionListener]). */
    var isDialogLauncherVisible: Boolean by observable(false)

    /** The ScreenTip of the dialog box launcher. */
    var dialogLauncherScreenTip: RibbonScreenTip? by observable(null)

    /** When the width is insufficient, groups with larger values shrink first (from the right for equal values). */
    var reductionOrder: Int by observable(0)

    /** Whether the group can collapse into a single drop-down button. */
    var canCollapse: Boolean by observable(true)

    /** How the whole group is handled in the simplified ribbon. */
    var simplifiedVisibility: RibbonSimplifiedVisibility by observable(RibbonSimplifiedVisibility.AUTO)

    /** Whether to show the dialog box launcher (there is a command, or it is explicitly requested). */
    val hasDialogLauncher: Boolean
        get() = isDialogLauncherVisible || dialogLauncherCommand != null || dialogLauncherCommandId != null
}

/** A ribbon tab ([Home], [Insert]). */
open class RibbonTabModel @JvmOverloads constructor(id: String? = null, label: String? = null) : RibbonNodeModel(id, label) {
    /** The groups. */
    val groups: RibbonList<RibbonGroupModel> = RibbonList()

    /** The id of the [RibbonContextualGroupModel] that owns this tab. Null for a regular tab. */
    var contextualGroupId: String? by observable(null) { old, new ->
        firePropertyChange("isContextual", old != null, new != null)
    }

    /** Whether the tab belongs to a contextual group. */
    val isContextual: Boolean get() = contextualGroupId != null
}

/** A contextual tab group shown according to the selection ([Table Tools], [Picture Tools]). Hidden by default. */
open class RibbonContextualGroupModel @JvmOverloads constructor(id: String? = null, label: String? = null) : RibbonNodeModel(id, label) {
    init {
        isVisible = false
    }

    /** The accent color of the tabs. */
    var color: RibbonColor by observable(RibbonColor.parse("#0F6CBD"))

    /** The selection behavior when shown. */
    var activation: RibbonContextualActivation by observable(RibbonContextualActivation.NONE)
}

/**
 * A navigation item of the Backstage (the [File] tab). It is a page if it has [content]; otherwise it is an action
 * (runs a command and closes the Backstage if [closesBackstage]).
 */
open class RibbonBackstageItemModel @JvmOverloads constructor(
    id: String? = null,
    label: String? = null,
    icon: RibbonIcon? = null,
    content: WComponent? = null,
) : RibbonNodeModel(id, label) {
    init {
        this.icon = icon
    }

    /** The content of the page. */
    var content: WComponent? by observable(content)

    /** The command catalog id of the action ([Save], [Close]). */
    var commandId: String? by observable(null)

    /** The command of the action. */
    var command: RibbonCommand? by observable(null)

    /** The command parameter. */
    var commandParameter: Any? by observable(null)

    /** Whether it is in the top list or the footer list. */
    var placement: RibbonBackstagePlacement by observable(RibbonBackstagePlacement.TOP)

    /** Whether to close the Backstage after running the action. */
    var closesBackstage: Boolean by observable(true)

    /** Whether to draw a separator above the item. */
    var hasSeparatorBefore: Boolean by observable(false)

    /** Whether it is a page item. */
    val isPage: Boolean get() = content != null
}

/** The Backstage (the full-screen view of the [File] tab). */
open class RibbonBackstageModel : RibbonObservable() {
    /** The navigation items. */
    val items: RibbonList<RibbonBackstageItemModel> = RibbonList()

    /** The selected page. */
    var selectedItem: RibbonBackstageItemModel? by observable(null)

    /** Whether it is open. */
    var isOpen: Boolean by observable(false)

    /** The title shown at the top of the navigation pane (the app name). */
    var title: String? by observable(null)

    /** The width of the navigation pane. */
    var navigationPaneWidth: Double by observable(200.0)

    /** Whether to show the heading of the selected page as a large title. */
    var showPageTitle: Boolean by observable(true)

    /** Arbitrary content placed above the navigation items (a logo or an account). */
    var paneHeader: WComponent? by observable(null)
}
