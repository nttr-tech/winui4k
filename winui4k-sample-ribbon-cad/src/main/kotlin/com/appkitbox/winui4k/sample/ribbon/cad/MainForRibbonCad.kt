package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VirtualKey
import com.appkitbox.winui4k.VirtualKeyModifier
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.extension.ribbon.RibbonItemInvokedEvent
import com.appkitbox.winui4k.extension.ribbon.RibbonMenuIconConverter
import com.appkitbox.winui4k.extension.ribbon.WRibbon
import com.appkitbox.winui4k.extension.ribbon.WRibbonApplicationMenu
import com.appkitbox.winui4k.extension.ribbon.WRibbonIcon
import com.appkitbox.winui4k.extension.ribbon.WRibbonStatusBar
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme
import com.appkitbox.winui4k.extension.ribbon.WRibbonToolBar
import com.appkitbox.winui4k.extension.ribbon.model.RibbonApplicationMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonApplicationMenuModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDropDownButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonLabelModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMinimizeBehavior
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRecentItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonReductionStrategy
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSegmentModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSegmentedModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStatusBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToolBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonVisibilityMode
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.common.radioItem

/**
 * An AutoCAD-style ribbon demo (same layout as the RibbonSpace CAD demo): the CAD theme (dark), the application menu
 * opened from the app icon, a QAT with a workspace combo box, line-art command icons, split buttons that remember the
 * last chosen method, cycling through the expanded panel, floating panel and minimized states, the [Text Editor] and
 * [Hatch Creation] contextual tabs, document tabs, a floor plan, the ViewCube and navigation bar, a command line that
 * runs commands by alias, and a status bar with drafting aid toggles.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.apply(RibbonThemePalette.CAD, RibbonChromeStyle.NEUTRAL, RibbonThemeStyle.CAD)
        // Menu items cannot show line-art icons, so use the closest glyph instead
        WRibbonTheme.menuIconConverter = RibbonMenuIconConverter { cadMenuIcon(it) }
        CadApp(createCadModel()).show()
    }
}

/** The screens of the CAD demo and its command handling. */
private class CadApp(private val model: RibbonModel) {
    private val ribbon = WRibbon(model).also {
        it.isMinimizeButtonVisible = true
        it.isDisplayOptionsButtonVisible = false
        it.canFloatGroups = true
        it.isVisibilityMenuEnabled = true
        it.reductionStrategy = RibbonReductionStrategy.GROUP_BY_GROUP
    }
    private val window = RibbonDemoWindow("Drawing1.dwg - CAD (WinUI4K Ribbon)", RibbonDemoApp.CAD, ribbon)
    private val viewport = CadViewport(ribbon)
    private val fileTabs = fileTabs()
    private val statusBarModel = statusBarModel()
    private var dark = true

    fun show() {
        addMenus()
        ribbon.applicationMenu = applicationMenu().createFlyout()
        ribbon.addItemInvokedListener { onItemInvoked(it) }
        (model.quickAccessItems.first { it.id == "qat.workspace" } as RibbonComboBoxModel).addCommitListener { e -> (e.item as? CadWorkspace)?.let { applyWorkspace(it) } }
        (model.findItem("layerCombo") as RibbonComboBoxModel).addCommitListener { e -> echo("Command: Current layer: \"${e.text}\"") }
        val titleBar = window.titleBar!!
        titleBar.appIcon = ic(CadIcons.APP_LOGO)
        titleBar.isAppIconMenuEnabled = true
        titleBar.subtitle = "WinUI4K CAD"
        titleBar.endContent = WPanel(spacing = 2.0, orientation = Orientation.HORIZONTAL).also {
            it.add(WButton("Sign In"))
            it.add(WButton().also { help -> help.content = WRibbonIcon(ic(CadIcons.HELP), HELP_ICON) })
        }
        val area = WGrid()
        area.addRow(GridLength.AUTO)
        area.addRow(GridLength.star())
        area.add(fileTabs, row = 0, column = 0)
        area.add(viewport.element, row = 1, column = 0)
        window.setTop(ribbon)
        window.setContent(area)
        window.setStatusBar(WRibbonStatusBar(statusBarModel).also { it.ribbon = ribbon })
        window.setDark(true)
        // Ctrl+0 toggles Clean Screen (same as the status bar toggle)
        area.addKeyboardAccelerator(VirtualKey.NUMBER_0, VirtualKeyModifier.CONTROL) { toggleCleanScreen() }
        window.show()
    }

    // ---------------------------------------------------------------- Commands

    private fun onItemInvoked(event: RibbonItemInvokedEvent) {
        val id = event.item.id
        val choice = (event.item as? RibbonSplitButtonModel)?.lastChoice?.label
        val checked = (event.item as? RibbonToggleButtonModel)?.isChecked == true
        val handlers = listOf({ handleView(id, checked) }, { handleRibbonFeature(id) }, { handleStatus(id, checked) }, { handleMenus(id, event.parameter) })
        if (handlers.any { it() }) return
        handleDrawing(id, choice)
        if (!isSilent(id)) echoCommand(id, event.item.label, choice)
    }

    /** Items that are not echoed to the command line (status bar, the command line itself, navigation bar). */
    private fun isSilent(id: String): Boolean =
        id.startsWith("status.") || id.startsWith("command.") || (id.startsWith("nav.") && id != "nav.zoomExtents")

    /** The command line and the menus of drop-downs that have no content. Returns true if handled. */
    private fun handleMenus(id: String, parameter: Any?): Boolean {
        when {
            id == "command.input" -> (parameter as? String)?.let { runCommand(it) }
            id == "command.close" -> setCommandLineVisible(false)
            id.startsWith("minimizeBehavior.") -> model.minimizeBehavior = RibbonMinimizeBehavior.valueOf(id.removePrefix("minimizeBehavior."))
            id.startsWith("workspace.") -> applyWorkspace(CadWorkspace.valueOf(id.removePrefix("workspace.")))
            else -> return false
        }
        return true
    }

    /** Drawing commands that change the screen state (contextual tabs, zoom). */
    private fun handleDrawing(id: String, choice: String?) {
        val isText = id == "text" || id == "annotateMText"
        val isHatch = id == "hatch" || id == "gradient"
        when {
            isText && (choice == null || choice == "Multiline Text") -> showTextEditor()
            isHatch && (choice == null || choice == "Hatch" || id == "gradient") -> showHatchCreation()
            id == "closeTextEditor" -> closeContextual("Text Editor")
            id == "closeHatchCreation" -> closeContextual("Hatch Creation")
            id == "nav.zoomExtents" || id == "zoomExtents" -> viewport.drawing.zoomExtents()
        }
    }

    /** The toggles and buttons on the [View] tab. Returns true if handled. */
    private fun handleView(id: String, checked: Boolean): Boolean {
        when (id) {
            "ucsIcon" -> viewport.setUcsIconVisible(checked)
            "viewCube" -> viewport.setViewCubeVisible(checked)
            "navBar" -> viewport.setNavBarVisible(checked)
            "commandLine" -> setCommandLineVisible(checked)
            "fileTabs" -> fileTabs.isVisible = checked
            "layoutTabs" -> (statusBarModel.items.first() as RibbonSegmentedModel).isVisible = checked
            "lightTheme" -> setDark(!checked)
            "panelTitles" -> model.showGroupCaptions = checked
            else -> return false
        }
        return true
    }

    /** The [Ribbon] panel on the [View] tab (buttons for trying out ribbon features). Returns true if handled. */
    private fun handleRibbonFeature(id: String): Boolean {
        when (id) {
            "cycleMinimize" -> ribbon.toggleMinimized()
            "floatLayers" -> model.findGroup("layers")?.let { ribbon.floatGroup(it) }
            "returnPanels" -> ribbon.returnAllPanelsToRibbon()
            "showTextEditor" -> showTextEditor()
            "showHatch" -> showHatchCreation()
            else -> return false
        }
        return true
    }

    /** The status bar toggles. Returns true if handled. */
    private fun handleStatus(id: String, checked: Boolean): Boolean {
        when (id) {
            "status.grid" -> viewport.drawing.isGridVisible = checked
            "status.lwt" -> viewport.drawing.showLineweights = checked
            "status.model" -> (model.findItem(id) ?: statusItem(id))?.label = if (checked) "MODEL" else "PAPER"
            "status.cleanScreen" -> setCleanScreen(checked)
            else -> return false
        }
        return true
    }

    private fun statusItem(id: String) = (statusBarModel.items + statusBarModel.endItems).firstOrNull { it.id == id }

    /** Runs a typed command: an alias runs its item; anything else runs the first result of the ribbon search. */
    private fun runCommand(text: String) {
        val typed = text.trim()
        viewport.commandLine.clearInput()
        if (typed.isEmpty()) return
        val item = CAD_ALIASES[typed.lowercase()]?.let { model.findItem(it) }
        if (item != null) {
            ribbon.performClick(item)
            return
        }
        val result = ribbon.search(typed, 1).firstOrNull()
        if (result != null && result.score > 0) {
            echo("Command: ${typed.uppercase()}")
            ribbon.executeSearchEntry(result.entry)
            return
        }
        echo("Unknown command \"${typed.uppercase()}\".  Press F1 for help.")
    }

    private fun echoCommand(id: String, label: String?, choice: String?) {
        val command = CAD_COMMANDS[id]
        if (command != null) {
            echo("Command: _" + if (choice != null) "${command.first} ($choice)" else command.first)
            echo(command.second)
        } else if (!label.isNullOrEmpty()) {
            echo("Command: " + label.replace('\n', ' '))
        }
    }

    private fun echo(line: String) = viewport.commandLine.echo(line)

    // ---------------------------------------------------------------- State

    private fun applyWorkspace(workspace: CadWorkspace) {
        applyWorkspace(model, workspace)
        model.selectedTabId = "home"
        echo("Command: Workspace: ${workspace.label}")
    }

    private fun showTextEditor() {
        model.setActiveContextualGroups("textEditor")
        viewport.showTextEditor(true)
        echo("Command: _MTEXT  Current text style:  \"Standard\"  Text height: 2.5  Annotative: No")
    }

    private fun showHatchCreation() {
        model.setActiveContextualGroups("hatchCreation")
        echo("Command: _HATCH")
        echo("Pick internal point or [Select objects/Undo/seTtings]:")
    }

    private fun closeContextual(name: String) {
        viewport.showTextEditor(false)
        model.setActiveContextualGroups()
        model.selectedTabId = "home"
        echo("$name closed.")
    }

    private fun setCommandLineVisible(visible: Boolean) {
        viewport.commandLine.element.isVisible = visible
        (model.findItem("commandLine") as? RibbonToggleButtonModel)?.isChecked = visible
    }

    private fun setDark(value: Boolean) {
        dark = value
        window.setDark(value)
        viewport.applyTheme(value)
    }

    private fun toggleCleanScreen() {
        val toggle = statusItem("status.cleanScreen") as RibbonToggleButtonModel
        toggle.isChecked = !toggle.isChecked
        setCleanScreen(toggle.isChecked)
    }

    /** Clean Screen: hides the ribbon (full-screen mode), the title bar and the document tabs to enlarge the drawing area. */
    private fun setCleanScreen(clean: Boolean) {
        model.visibilityMode = if (clean) RibbonVisibilityMode.FULL_SCREEN else RibbonVisibilityMode.ALWAYS_SHOW
        window.titleBar?.isVisible = !clean
        fileTabs.isVisible = !clean && (model.findItem("fileTabs") as? RibbonToggleButtonModel)?.isChecked != false
    }

    // ---------------------------------------------------------------- Assembly

    /** Menus of the drop-downs that have no content in XAML (minimize behavior, workspace). */
    private fun addMenus() {
        val behaviors = (model.findItem("minimizeBehavior") as RibbonDropDownButtonModel).menuItems
        for (behavior in RibbonMinimizeBehavior.entries) {
            behaviors += radioItem("minimizeBehavior.${behavior.name}", MINIMIZE_LABELS.getValue(behavior), "minimizeBehavior", behavior == model.minimizeBehavior)
        }
    }

    /** The document tabs (Start, drawings, New). */
    private fun fileTabs(): WComponent {
        val tabs = RibbonToolBarModel("documents")
        tabs.showLabels = true
        for ((id, label) in listOf("start" to "Start", "drawing1" to "Drawing1*", "floorPlan" to "Floor Plan")) {
            tabs.items.add(
                tgl("file.$id", label, ic(if (id == "start") CadIcons.APP_LOGO else CadIcons.FILE_TABS), RibbonItemSize.MEDIUM).also {
                    it.groupName = "documents"
                    it.showLabelInSimplified = true
                    it.isChecked = id == "drawing1"
                },
            )
        }
        tabs.items.add(cmd("file.new", "New Drawing", RibbonIcon.glyph(""), RibbonItemSize.SMALL))
        return WRibbonToolBar(tabs).also { it.ribbon = ribbon }
    }

    /** The AutoCAD menu browser (search, commands and subcommands, recent documents, options and exit). */
    private fun applicationMenu(): WRibbonApplicationMenu {
        val menu = RibbonApplicationMenuModel()
        menu.recentHeader = "Recent Documents"
        for (entry in APP_MENU) {
            menu.items.add(
                RibbonApplicationMenuItemModel(entry.id, entry.label, ic(entry.icon)).also { item ->
                    item.hasSeparatorBefore = entry.separator
                    for ((index, sub) in entry.subs.withIndex()) {
                        item.items.add(RibbonApplicationMenuItemModel("${entry.id}.$index", sub.first, ic(entry.icon)).also { it.description = sub.second })
                    }
                },
            )
        }
        menu.recentItems.add(RibbonRecentItemModel("r1", "Floor Plan.dwg", "Projects › Riverside House").also { it.isPinned = true })
        menu.recentItems.add(RibbonRecentItemModel("r2", "Gearbox Housing.dwg", "Projects › Mechanical").also { it.isPinned = true })
        menu.recentItems.add(RibbonRecentItemModel("r3", "Site Layout.dwg", "Projects › Riverside House"))
        menu.recentItems.add(RibbonRecentItemModel("r4", "Bracket Detail.dwg", "Projects › Mechanical"))
        menu.recentItems.add(RibbonRecentItemModel("r5", "Electrical Plan.dwg", "Documents"))
        menu.footerItems.add(cmd("appmenu.options", "Options", ic(CadIcons.OPTIONS), RibbonItemSize.MEDIUM))
        menu.footerItems.add(cmd("appmenu.exit", "Exit WinUI4K CAD", ic(CadIcons.EXIT), RibbonItemSize.MEDIUM))
        return WRibbonApplicationMenu(menu, ribbon).also { app ->
            app.addItemInvokedListener { e -> echo("Command: Application Menu: ${e.item.label}") }
        }
    }

    /** The CAD status bar (layout tabs, coordinates, model / paper, drafting aid toggles, annotation scale, workspace). */
    private fun statusBarModel(): RibbonStatusBarModel {
        val bar = RibbonStatusBarModel()
        bar.items.add(
            RibbonSegmentedModel("status.layout", "Layout").also { segmented ->
                listOf("Model", "Layout1", "Layout2").forEach { segmented.segments.add(RibbonSegmentModel(it, it)) }
                segmented.selectedSegment = segmented.segments[0]
            },
        )
        bar.endItems.add(RibbonLabelModel("status.coords", "6215.7290, 3820.4106, 0.0000"))
        bar.endItems.add(tgl("status.model", "MODEL", null, RibbonItemSize.SMALL).also { it.isChecked = true })
        bar.endItems.add(statusToggle("status.grid", "Grid Display", CadIcons.GRID, checked = true, shortcut = "F7"))
        bar.endItems.add(statusToggle("status.snap", "Snap Mode", CadIcons.SNAP, shortcut = "F9"))
        bar.endItems.add(RibbonSeparatorModel())
        bar.endItems.add(statusToggle("status.ortho", "Ortho Mode", CadIcons.ORTHO, shortcut = "F8"))
        bar.endItems.add(statusToggle("status.polar", "Polar Tracking", CadIcons.POLAR, checked = true, shortcut = "F10"))
        bar.endItems.add(statusToggle("status.otrack", "Object Snap Tracking", CadIcons.OBJECT_SNAP_TRACKING, checked = true, shortcut = "F11"))
        bar.endItems.add(objectSnap())
        bar.endItems.add(RibbonSeparatorModel())
        bar.endItems.add(statusToggle("status.lwt", "Show/Hide Lineweight", CadIcons.SHOW_LINEWEIGHT))
        bar.endItems.add(statusToggle("status.transparency", "Show/Hide Transparency", CadIcons.SHOW_TRANSPARENCY))
        bar.endItems.add(statusToggle("status.selectionCycling", "Selection Cycling", CadIcons.SELECTION_CYCLING))
        bar.endItems.add(RibbonSeparatorModel())
        bar.endItems.add(
            ddn(
                "status.annoScale",
                "1:1",
                ic(CadIcons.ANNOTATION_SCALE),
                RibbonItemSize.SMALL,
                *listOf("1:1", "1:2", "1:5", "1:10", "1:50", "1:100").mapIndexed { i, s -> radioItem("status.annoScale.$i", s, "annoScale", i == 0) }.toTypedArray(),
                sep(),
                mi("status.annoScale.custom", "Custom...", null),
            ).also { it.screenTip = tip("Annotation scale of the current view") },
        )
        bar.endItems.add(
            ddn(
                "status.workspace",
                "Workspace Switching",
                ic(CadIcons.WORKSPACE),
                RibbonItemSize.SMALL,
                *CadWorkspace.entries.map { radioItem("workspace.${it.name}", it.label, "workspace", it == CadWorkspace.DRAFTING) }.toTypedArray(),
                sep(),
                mi("status.workspace.save", "Save Current As...", null),
                mi("status.workspace.settings", "Workspace Settings...", null),
            ).also { it.showLabel = false },
        )
        bar.endItems.add(statusToggle("status.cleanScreen", "Clean Screen", CadIcons.CLEAN_SCREEN, shortcut = "Ctrl+0"))
        bar.endItems.add(cmd("status.customize", "Customization", RibbonIcon.glyph(""), RibbonItemSize.SMALL).also { it.showLabel = false })
        return bar
    }

    private fun statusToggle(id: String, label: String, icon: String, checked: Boolean = false, shortcut: String? = null) =
        tgl(id, label, ic(icon), RibbonItemSize.SMALL).also {
            it.showLabel = false
            it.isChecked = checked
            it.shortcut = shortcut
        }

    /** Object snap (on / off, and check items for the snap types). */
    private fun objectSnap(): RibbonSplitButtonModel {
        val snaps = listOf("Endpoint" to true, "Midpoint" to true, "Center" to true, "Geometric Center" to false, "Node" to false, "Quadrant" to false, "Intersection" to true, "Extension" to true, "Perpendicular" to false, "Tangent" to false, "Nearest" to false)
        return spl(
            "status.osnap",
            "Object Snap",
            ic(CadIcons.OBJECT_SNAP),
            RibbonItemSize.SMALL,
            false,
            *snaps.mapIndexed { i, (label, on) ->
                RibbonMenuItemModel("status.osnap.$i", label).also {
                    it.isCheckable = true
                    it.isChecked = on
                }
            }.toTypedArray(),
            sep(),
            mi("status.osnap.settings", "Object Snap Settings...", null),
        ).also {
            it.showLabel = false
            it.isCheckable = true
            it.isChecked = true
            it.shortcut = "F3"
        }
    }

    private class AppMenuEntry(val id: String, val label: String, val icon: String, val separator: Boolean, val subs: List<Pair<String, String>>)

    private companion object {
        const val HELP_ICON = 18.0

        val MINIMIZE_LABELS = mapOf(
            RibbonMinimizeBehavior.TABS to "Minimize to Tabs",
            RibbonMinimizeBehavior.PANEL_TITLES to "Minimize to Panel Titles",
            RibbonMinimizeBehavior.PANEL_BUTTONS to "Minimize to Panel Buttons",
            RibbonMinimizeBehavior.CYCLE_ALL to "Cycle Through All",
        )

        val APP_MENU = listOf(
            AppMenuEntry("new", "New", CadIcons.NEW, false, listOf("Drawing" to "Creates a new drawing from a template.", "Sheet Set" to "Creates a new sheet set to organize layouts.")),
            AppMenuEntry(
                "open",
                "Open",
                CadIcons.OPEN,
                false,
                listOf("Drawing" to "Opens an existing drawing file.", "From Web & Mobile" to "Opens a drawing saved in the cloud.", "Sheet Set" to "Opens a sheet set data file.", "DGN" to "Imports the data from a DGN file into a new drawing."),
            ),
            AppMenuEntry("save", "Save", CadIcons.SAVE, false, emptyList()),
            AppMenuEntry(
                "saveAs",
                "Save As",
                CadIcons.SAVE_AS,
                false,
                listOf(
                    "Drawing" to "Saves the current drawing as a DWG file.",
                    "Drawing Template" to "Creates a drawing template (DWT) file from which new drawings can be created.",
                    "Drawing Standards" to "Creates a drawing standards (DWS) file for checking drawings.",
                    "Other Formats" to "Saves the current drawing in DWG, DWT, DWS, or DXF format.",
                ),
            ),
            AppMenuEntry(
                "import",
                "Import",
                CadIcons.IMPORT_PDF,
                true,
                listOf("PDF" to "Imports geometry, fills, raster images, and text from a PDF file.", "DGN" to "Imports the data from a DGN file into the current drawing.", "Other Formats" to "Imports files of various formats into the current drawing."),
            ),
            AppMenuEntry(
                "export",
                "Export",
                CadIcons.EXPORT_PDF,
                false,
                listOf("PDF" to "Exports the current drawing or layout to a PDF file.", "DWF" to "Creates a DWF file and lets you set individual page setup overrides.", "DGN" to "Exports the current drawing to a DGN file.", "FBX" to "Exports 3D objects, cameras, and lights to an FBX file."),
            ),
            AppMenuEntry(
                "publish",
                "Publish",
                CadIcons.PUBLISH,
                false,
                listOf("Shared Views" to "Publishes design views of the drawing to a web browser.", "Send to 3D Print Service" to "Sends a 3D model to a 3D print service.", "Archive" to "Packages the files of the current sheet set for archiving.", "eTransmit" to "Creates a package of a drawing and its dependent files."),
            ),
            AppMenuEntry(
                "print",
                "Print",
                CadIcons.PLOT,
                false,
                listOf("Plot" to "Plots a drawing to a plotter, printer or file.", "Batch Plot" to "Publishes drawings to DWF, DWFx, and PDF files, or to printers and plotters.", "Page Setup Manager" to "Manages the page layout, plotting device, paper size, and other settings for each new layout.", "Plot Preview" to "Displays the drawing as it will be plotted."),
            ),
            AppMenuEntry(
                "utilities",
                "Drawing Utilities",
                CadIcons.UNITS,
                true,
                listOf("Units" to "Controls the display format and precision of coordinates and angles.", "Audit" to "Evaluates the integrity of a drawing and corrects some errors.", "Drawing Status" to "Displays drawing statistics, modes, and extents.", "Purge" to "Removes unused items, such as block definitions and layers, from the drawing.", "Recover" to "Repairs a damaged drawing file and then opens it."),
            ),
            AppMenuEntry("close", "Close", CadIcons.CLOSE, false, listOf("Current Drawing" to "Closes the current drawing.", "All Drawings" to "Closes all open drawings.")),
        )
    }
}
