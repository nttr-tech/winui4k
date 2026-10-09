package com.appkitbox.winui4k.ribbon

/** Display size of a ribbon item. */
enum class RibbonItemSize {
    /** Large: a label (which may wrap onto 2 lines) below a large icon. Takes up the full height of the group. */
    LARGE,

    /** Medium: a label to the right of a small icon. Up to 3 are stacked in a column. */
    MEDIUM,

    /** Small: icon only. Up to 3 are stacked in a column. */
    SMALL,
}

/** State of a group's adaptive layout (widest first). */
enum class RibbonGroupState {
    /** Shows items at their largest size. */
    LARGE,

    /** Shows items at medium size. */
    MEDIUM,

    /** Shows items at their smallest size. */
    SMALL,

    /** Combines the whole group into a single drop-down button that shows all items in a popup when clicked. */
    COLLAPSED,
}

/** Layout of the whole ribbon. */
enum class RibbonDisplayMode {
    /** The 3-row classic ribbon with group names (Office's "Classic Ribbon"). */
    CLASSIC,

    /** The single-row ribbon with an overflow menu (Office's "Simplified Ribbon"). */
    SIMPLIFIED,
}

/** How the ribbon is shown (Office's "Ribbon Display Options" and AutoCAD's minimized states). */
enum class RibbonVisibilityMode {
    /** Always shows the tabs and commands. */
    ALWAYS_SHOW,

    /** Shows only the tabs; selecting a tab shows the commands in a temporary overlay. */
    TABS_ONLY,

    /** Hides the ribbon until it is called up from the top edge of the screen (Office's "Full-screen mode"). */
    FULL_SCREEN,

    /** Tabs and a button for each panel (group). Clicking one opens the panel (AutoCAD's "Minimize to Panel Buttons"). */
    PANEL_BUTTONS,

    /** Tabs and panel (group) titles. Clicking one opens the panel (AutoCAD's "Minimize to Panel Titles"). */
    PANEL_TITLES,
}

/** Behavior of the minimize button (and [com.appkitbox.winui4k.WRibbon.toggleMinimized]) (AutoCAD's minimize behavior). */
enum class RibbonMinimizeBehavior {
    /** Toggles between the full ribbon and tabs only (Office's behavior). */
    TABS,

    /** Toggles between the full ribbon and panel titles. */
    PANEL_TITLES,

    /** Toggles between the full ribbon and panel buttons. */
    PANEL_BUTTONS,

    /** Cycles through full → panel buttons → panel titles → tabs → full (AutoCAD's "Cycle Through All"). */
    CYCLE_ALL,
}

/** Position of the Quick Access Toolbar (QAT). */
enum class RibbonQuickAccessPosition {
    /** Above the ribbon (next to the title and the [File] button). */
    ABOVE_RIBBON,

    /** Below the command area of the ribbon. */
    BELOW_RIBBON,
}

/** Spacing density (Office's "Touch/Mouse Mode"). */
enum class RibbonDensity {
    /** The default density for mouse use. */
    COMFORTABLE,

    /** A tighter density for business tools. */
    COMPACT,

    /** Large targets for touch input. */
    TOUCH,
}

/** How an item is handled in the simplified ribbon (single row). */
enum class RibbonSimplifiedVisibility {
    /** Shown in the row while there is enough width; moves to the overflow menu when there is not. */
    AUTO,

    /** Always shown in the row (moved to the overflow only as a last resort). */
    PINNED,

    /** Always shown in the overflow menu. */
    OVERFLOW,

    /** Not shown in the simplified ribbon. */
    HIDDEN,
}

/** How labels are shown in the simplified ribbon. */
enum class RibbonSimplifiedLabel {
    /** Shows labels only for items whose preferred size is [RibbonItemSize.LARGE]. */
    AUTO,

    /** Always shows labels. */
    SHOW,

    /** Shows icons only. */
    HIDE,
}

/** How the items of a classic group are arranged. */
enum class RibbonGroupItemsLayout {
    /** Large items take up a column, and medium and small items are stacked in columns (Office's default). */
    COLUMNS,

    /** Children are laid out in horizontal rows, and the rows are stacked vertically (Office's [Font] / [Paragraph] groups). */
    ROWS,
}

/** Behavior when a contextual tab group is shown. */
enum class RibbonContextualActivation {
    /** Does not automatically select the shown tab. */
    NONE,

    /** Selects the first tab when the group is shown. */
    SELECT_ON_SHOW,
}

/** How a node is handled when merging models ([RibbonModelMerger]) (plug-ins, MDI child documents). */
enum class RibbonMergeAction {
    /** Adds the node. If a node with the same id exists, merges the children. */
    MERGE,

    /** Replaces the existing node with the same id. */
    REPLACE,

    /** Removes the existing node with the same id. */
    REMOVE,

    /** Always adds the node, even if a node with the same id exists. */
    ADD,
}

/** Position of a Backstage navigation item. */
enum class RibbonBackstagePlacement {
    /** The main list at the top of the navigation pane. */
    TOP,

    /** The footer list at the bottom of the navigation pane (Account, Feedback, Options). */
    BOTTOM,
}

/** Orientation of a toolbar. */
enum class RibbonToolBarOrientation {
    /** Horizontal (command bar, options bar). */
    HORIZONTAL,

    /** Vertical (tool palette / rail). */
    VERTICAL,
}

/** How groups shrink when there is not enough width ([RibbonAdaptiveLayout]). */
enum class RibbonReductionStrategy {
    /**
     * Office: all groups to medium, then all groups to small, and finally collapsed
     * (within each step, groups with a larger [RibbonGroupModel.reductionOrder] first).
     */
    STEPWISE,

    /**
     * AutoCAD style: shrinks the group with the largest reduction order (the rightmost one among equals) all the way
     * to collapsed before shrinking the next group. Keeps important panels large for as long as possible.
     */
    GROUP_BY_GROUP,
}

/** How panels (groups) are presented (AutoCAD's minimized states). */
enum class RibbonPanelPresentation {
    /** Normal groups. */
    FULL,

    /** One button per panel (icon and label). Clicking one opens the panel. */
    BUTTONS,

    /** Panel titles only. Clicking one opens the panel. */
    TITLES,
}

/** Color scheme of the window decorations such as the title bar (Office themes). */
enum class RibbonChromeStyle {
    /** A title bar in the accent color (Office's "Colorful"). */
    COLORFUL,

    /** A neutral title bar matching the ribbon (Office's "White" / modern Office). */
    NEUTRAL,
}

/** Look of the whole ribbon surface (independent of the accent palette and light / dark). */
enum class RibbonThemeStyle {
    /** Modern Office: a floating rounded command bar, an underlined selected tab and undecorated panel titles. */
    OFFICE,

    /**
     * CAD, modeled on AutoCAD-family apps: a blue-gray surface, an edge-to-edge command bar, panel title bars,
     * flat tabs that show selection with their background, and shapes without rounded corners.
     */
    CAD,
}

/** The page the customization dialog opens first. */
enum class RibbonCustomizePage {
    /** "Customize Ribbon". */
    RIBBON,

    /** "Quick Access Toolbar". */
    QUICK_ACCESS_TOOLBAR,
}
