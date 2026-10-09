package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonDensity
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMinimizeBehavior
import com.appkitbox.winui4k.ribbon.RibbonModel

/** Workspaces (the same three as the RibbonSpace CAD demo) and the tabs shown in each. */
enum class CadWorkspace(val label: String, val tabIds: Set<String>) {
    /** Drafting & Annotation (the default). */
    DRAFTING("Drafting & Annotation", setOf("home", "insert", "annotate", "parametric", "view", "manage", "output", "collaborate")),

    /** 3D Basics. */
    BASICS_3D("3D Basics", setOf("home", "visualize", "insert", "view", "manage", "output", "collaborate")),

    /** 3D Modeling. */
    MODELING_3D("3D Modeling", setOf("home", "solid", "surface", "mesh", "visualize", "parametric", "insert", "annotate", "view", "manage", "output", "collaborate")),
    ;

    override fun toString(): String = label
}

/**
 * An AutoCAD-style ribbon model (the same structure as CadPage.xaml in the RibbonSpace CAD demo): [Home] [Insert] [Annotate]
 * [Parametric] [View] [Manage] [Output] [Collaborate], plus [Solid] [Surface] [Mesh] [Visualize] shown in the 3D workspaces,
 * the [Text Editor] and [Hatch Creation] contextual tabs, and a QAT with a workspace combo box.
 */
fun createCadModel(): RibbonModel = RibbonModel().apply {
    title = "Drawing1.dwg"
    isApplicationButtonVisible = false
    minimizeBehavior = RibbonMinimizeBehavior.CYCLE_ALL
    density = RibbonDensity.COMPACT
    contextualGroups.add(contextual("textEditor", "Text Editor", "#4CAF50"))
    contextualGroups.add(contextual("hatchCreation", "Hatch Creation", "#3DA9F5"))
    tabs.addAll(cadTabs())
    quickAccessItems.add(cmd("qat.new", "New", ic(CadIcons.NEW), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+N" })
    quickAccessItems.add(cmd("qat.open", "Open", ic(CadIcons.OPEN), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+O" })
    quickAccessItems.add(cmd("qat.save", "Save", ic(CadIcons.SAVE), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+S" })
    quickAccessItems.add(cmd("qat.saveAs", "Save As", ic(CadIcons.SAVE_AS), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+Shift+S" })
    quickAccessItems.add(cmd("qat.undo", "Undo", ic(CadIcons.UNDO), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+Z" })
    quickAccessItems.add(cmd("qat.redo", "Redo", ic(CadIcons.REDO), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+Y" })
    quickAccessItems.add(cmd("qat.plot", "Plot", ic(CadIcons.PLOT), RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+P" })
    quickAccessItems.add(
        RibbonComboBoxModel("qat.workspace", "Workspace", CadWorkspace.entries).also {
            it.showLabel = false
            it.selectedItem = CadWorkspace.DRAFTING
            it.inputWidth = WORKSPACE_WIDTH
            it.canAddToQuickAccess = false
        },
    )
    applyWorkspace(this, CadWorkspace.DRAFTING)
}

/** Shows only the tabs of [workspace] (contextual tabs are left as they are). */
fun applyWorkspace(model: RibbonModel, workspace: CadWorkspace) {
    for (tab in model.tabs.filter { it.contextualGroupId == null }) tab.isVisible = tab.id in workspace.tabIds
    (model.quickAccessItems.firstOrNull { it.id == "qat.workspace" } as? RibbonComboBoxModel)?.let {
        it.selectedItem = workspace
        it.text = workspace.label
    }
    if (model.selectedTab?.isVisible != true) model.selectedTabId = "home"
}

private fun contextual(id: String, label: String, color: String) = RibbonContextualGroupModel(id, label).also {
    it.color = RibbonColor.parse(color)
    it.activation = RibbonContextualActivation.SELECT_ON_SHOW
}

/**
 * Substitutes for CAD icons shown in menu items (menu items can only have single-color icons and cannot show line art;
 * the RibbonSpace demo renders the line art to images with Skia, but here we use similar glyphs).
 */
fun cadMenuIcon(icon: RibbonIcon): RibbonIcon? = MENU_ICONS[icon.value]

private val MENU_ICONS: Map<String, RibbonIcon> = mapOf(
    CadIcons.CIRCLE_CENTER_RADIUS to RibbonIcons.CIRCLE,
    CadIcons.CIRCLE_TWO_POINT to RibbonIcons.CIRCLE,
    CadIcons.CIRCLE_THREE_POINT to RibbonIcons.CIRCLE,
    CadIcons.CIRCLE_TAN_TAN_RADIUS to RibbonIcons.CIRCLE,
    CadIcons.CIRCLE_TAN_TAN_TAN to RibbonIcons.CIRCLE,
    CadIcons.ELLIPSE to RibbonIcons.CIRCLE,
    CadIcons.RECTANGLE to RibbonIcons.RECTANGLE,
    CadIcons.POLYGON to RibbonIcons.POLYGON,
    CadIcons.LINE to RibbonIcons.LINE,
    CadIcons.HATCH to RibbonIcons.FILL,
    CadIcons.GRADIENT to RibbonIcons.FILL,
    CadIcons.TRIM to RibbonIcons.CROP,
    CadIcons.MOVE to RibbonIcons.MOVE,
    CadIcons.ROTATE to RibbonIcons.ROTATE,
    CadIcons.MULTILINE_TEXT to RibbonIcons.TEXT,
    CadIcons.SINGLE_LINE_TEXT to RibbonIcons.TEXT,
    CadIcons.MEASURE_DISTANCE to RibbonIcons.MEASURE,
    CadIcons.PASTE to RibbonIcons.PASTE,
    CadIcons.INSERT_BLOCK to RibbonIcons.COMPONENT,
    CadIcons.EXPORT_PDF to RibbonIcons.EXPORT,
    CadIcons.PUBLISH to RibbonIcons.SHARE,
    CadIcons.ZOOM_EXTENTS to RibbonIcons.FULL_SCREEN,
    CadIcons.ZOOM_WINDOW to RibbonIcons.ZOOM,
    CadIcons.LIGHT to RibbonIcons.LIGHT,
    CadIcons.SUN to RibbonIcons.LIGHT,
    CadIcons.BOX to RibbonIcons.COMPONENT,
    CadIcons.MODEL to RibbonIcons.VIEW,
)

private const val WORKSPACE_WIDTH = 168.0
