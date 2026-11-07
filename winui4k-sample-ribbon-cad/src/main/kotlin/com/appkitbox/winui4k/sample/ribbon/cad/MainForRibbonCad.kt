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
        (model.findItem("layerCombo") as RibbonComboBoxModel).addCommitListener { e -> echo(Messages.get("log.current.layer", e.text)) }
        val titleBar = window.titleBar!!
        titleBar.appIcon = ic(CadIcons.APP_LOGO)
        titleBar.isAppIconMenuEnabled = true
        titleBar.subtitle = "WinUI4K CAD"
        titleBar.endContent = WPanel(spacing = 2.0, orientation = Orientation.HORIZONTAL).also {
            it.add(WButton(Messages.get("title.sign.in")))
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
            isText && (choice == null || choice == Messages.get("ribbon.text.0")) -> showTextEditor()
            isHatch && (choice == null || choice == Messages.get("ribbon.hatch.0") || id == "gradient") -> showHatchCreation()
            id == "closeTextEditor" -> closeContextual(Messages.get("context.text.editor"))
            id == "closeHatchCreation" -> closeContextual(Messages.get("context.hatch.creation"))
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
            "status.model" -> (model.findItem(id) ?: statusItem(id))?.label = if (checked) Messages.get("status.model.toggle") else Messages.get("status.paper")
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
            echo(Messages.get("log.command", typed.uppercase()))
            ribbon.executeSearchEntry(result.entry)
            return
        }
        echo(Messages.get("log.unknown.command", typed.uppercase()))
    }

    private fun echoCommand(id: String, label: String?, choice: String?) {
        val command = CAD_COMMANDS[id]
        if (command != null) {
            echo(Messages.get("log.command", "_" + if (choice != null) "${command.first} ($choice)" else command.first))
            echo(command.second)
        } else if (!label.isNullOrEmpty()) {
            echo(Messages.get("log.command", label.replace('\n', ' ')))
        }
    }

    private fun echo(line: String) = viewport.commandLine.echo(line)

    // ---------------------------------------------------------------- State

    private fun applyWorkspace(workspace: CadWorkspace) {
        applyWorkspace(model, workspace)
        model.selectedTabId = "home"
        echo(Messages.get("log.workspace", workspace.label))
    }

    private fun showTextEditor() {
        model.setActiveContextualGroups("textEditor")
        viewport.showTextEditor(true)
        echo(Messages.get("log.mtext"))
    }

    private fun showHatchCreation() {
        model.setActiveContextualGroups("hatchCreation")
        echo(Messages.get("log.hatch"))
        echo(Messages.get("prompt.hatch"))
    }

    private fun closeContextual(name: String) {
        viewport.showTextEditor(false)
        model.setActiveContextualGroups()
        model.selectedTabId = "home"
        echo(Messages.get("log.closed", name))
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
        for ((id, label) in listOf("start" to Messages.get("file.start"), "drawing1" to "Drawing1*", "floorPlan" to "Floor Plan")) {
            tabs.items.add(
                tgl("file.$id", label, ic(if (id == "start") CadIcons.APP_LOGO else CadIcons.FILE_TABS), RibbonItemSize.MEDIUM).also {
                    it.groupName = "documents"
                    it.showLabelInSimplified = true
                    it.isChecked = id == "drawing1"
                },
            )
        }
        tabs.items.add(cmd("file.new", Messages.get("file.new"), RibbonIcon.glyph(""), RibbonItemSize.SMALL))
        return WRibbonToolBar(tabs).also { it.ribbon = ribbon }
    }

    /** The AutoCAD menu browser (search, commands and subcommands, recent documents, options and exit). */
    private fun applicationMenu(): WRibbonApplicationMenu {
        val menu = RibbonApplicationMenuModel()
        menu.recentHeader = Messages.get("app.recent")
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
        menu.recentItems.add(RibbonRecentItemModel("r1", "Floor Plan.dwg", Messages.get("app.recent.riverside")).also { it.isPinned = true })
        menu.recentItems.add(RibbonRecentItemModel("r2", "Gearbox Housing.dwg", Messages.get("app.recent.mechanical")).also { it.isPinned = true })
        menu.recentItems.add(RibbonRecentItemModel("r3", "Site Layout.dwg", Messages.get("app.recent.riverside")))
        menu.recentItems.add(RibbonRecentItemModel("r4", "Bracket Detail.dwg", Messages.get("app.recent.mechanical")))
        menu.recentItems.add(RibbonRecentItemModel("r5", "Electrical Plan.dwg", Messages.get("app.recent.documents")))
        menu.footerItems.add(cmd("appmenu.options", Messages.get("common.options"), ic(CadIcons.OPTIONS), RibbonItemSize.MEDIUM))
        menu.footerItems.add(cmd("appmenu.exit", Messages.get("app.exit"), ic(CadIcons.EXIT), RibbonItemSize.MEDIUM))
        return WRibbonApplicationMenu(menu, ribbon).also { app ->
            app.addItemInvokedListener { e -> echo(Messages.get("log.app.menu", e.item.label)) }
        }
    }

    /** The CAD status bar (layout tabs, coordinates, model / paper, drafting aid toggles, annotation scale, workspace). */
    private fun statusBarModel(): RibbonStatusBarModel {
        val bar = RibbonStatusBarModel()
        bar.items.add(
            RibbonSegmentedModel("status.layout", Messages.get("common.layout")).also { segmented ->
                listOf(Messages.get("status.model"), Messages.get("status.layout1"), Messages.get("status.layout2")).forEach { segmented.segments.add(RibbonSegmentModel(it, it)) }
                segmented.selectedSegment = segmented.segments[0]
            },
        )
        bar.endItems.add(RibbonLabelModel("status.coords", "6215.7290, 3820.4106, 0.0000"))
        bar.endItems.add(tgl("status.model", Messages.get("status.model.toggle"), null, RibbonItemSize.SMALL).also { it.isChecked = true })
        bar.endItems.add(statusToggle("status.grid", Messages.get("status.grid"), CadIcons.GRID, checked = true, shortcut = "F7"))
        bar.endItems.add(statusToggle("status.snap", Messages.get("status.snap"), CadIcons.SNAP, shortcut = "F9"))
        bar.endItems.add(RibbonSeparatorModel())
        bar.endItems.add(statusToggle("status.ortho", Messages.get("status.ortho"), CadIcons.ORTHO, shortcut = "F8"))
        bar.endItems.add(statusToggle("status.polar", Messages.get("status.polar"), CadIcons.POLAR, checked = true, shortcut = "F10"))
        bar.endItems.add(statusToggle("status.otrack", Messages.get("status.otrack"), CadIcons.OBJECT_SNAP_TRACKING, checked = true, shortcut = "F11"))
        bar.endItems.add(objectSnap())
        bar.endItems.add(RibbonSeparatorModel())
        bar.endItems.add(statusToggle("status.lwt", Messages.get("status.lineweight"), CadIcons.SHOW_LINEWEIGHT))
        bar.endItems.add(statusToggle("status.transparency", Messages.get("status.transparency"), CadIcons.SHOW_TRANSPARENCY))
        bar.endItems.add(statusToggle("status.selectionCycling", Messages.get("status.selection.cycling"), CadIcons.SELECTION_CYCLING))
        bar.endItems.add(RibbonSeparatorModel())
        bar.endItems.add(
            ddn(
                "status.annoScale",
                "1:1",
                ic(CadIcons.ANNOTATION_SCALE),
                RibbonItemSize.SMALL,
                *listOf("1:1", "1:2", "1:5", "1:10", "1:50", "1:100").mapIndexed { i, s -> radioItem("status.annoScale.$i", s, "annoScale", i == 0) }.toTypedArray(),
                sep(),
                mi("status.annoScale.custom", Messages.get("status.anno.custom"), null),
            ).also { it.screenTip = tip(Messages.get("status.anno.tip")) },
        )
        bar.endItems.add(
            ddn(
                "status.workspace",
                Messages.get("status.workspace"),
                ic(CadIcons.WORKSPACE),
                RibbonItemSize.SMALL,
                *CadWorkspace.entries.map { radioItem("workspace.${it.name}", it.label, "workspace", it == CadWorkspace.DRAFTING) }.toTypedArray(),
                sep(),
                mi("status.workspace.save", Messages.get("status.workspace.save"), null),
                mi("status.workspace.settings", Messages.get("status.workspace.settings"), null),
            ).also { it.showLabel = false },
        )
        bar.endItems.add(statusToggle("status.cleanScreen", Messages.get("status.clean.screen"), CadIcons.CLEAN_SCREEN, shortcut = "Ctrl+0"))
        bar.endItems.add(cmd("status.customize", Messages.get("status.customization"), RibbonIcon.glyph(""), RibbonItemSize.SMALL).also { it.showLabel = false })
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
        val snaps = listOf(Messages.get("osnap.endpoint") to true, Messages.get("osnap.midpoint") to true, Messages.get("osnap.center") to true, Messages.get("osnap.geometric.center") to false, Messages.get("osnap.node") to false, Messages.get("osnap.quadrant") to false, Messages.get("osnap.intersection") to true, Messages.get("osnap.extension") to true, Messages.get("osnap.perpendicular") to false, Messages.get("osnap.tangent") to false, Messages.get("osnap.nearest") to false)
        return spl(
            "status.osnap",
            Messages.get("status.osnap"),
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
            mi("status.osnap.settings", Messages.get("status.osnap.settings"), null),
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
            RibbonMinimizeBehavior.TABS to Messages.get("minimize.tabs"),
            RibbonMinimizeBehavior.PANEL_TITLES to Messages.get("minimize.panel.titles"),
            RibbonMinimizeBehavior.PANEL_BUTTONS to Messages.get("minimize.panel.buttons"),
            RibbonMinimizeBehavior.CYCLE_ALL to Messages.get("minimize.cycle.all"),
        )

        val APP_MENU = listOf(
            AppMenuEntry("new", Messages.get("qat.new"), CadIcons.NEW, false, listOf(Messages.get("app.drawing") to Messages.get("app.new.drawing"), Messages.get("app.sheet.set") to Messages.get("app.new.sheet.set"))),
            AppMenuEntry(
                "open",
                Messages.get("common.backstage.open"),
                CadIcons.OPEN,
                false,
                listOf(Messages.get("app.drawing") to Messages.get("app.open.drawing"), Messages.get("app.open.web") to Messages.get("app.open.web.text"), Messages.get("app.sheet.set") to Messages.get("app.open.sheet.set"), "DGN" to Messages.get("app.open.dgn")),
            ),
            AppMenuEntry("save", Messages.get("common.save"), CadIcons.SAVE, false, emptyList()),
            AppMenuEntry(
                "saveAs",
                Messages.get("qat.save.as"),
                CadIcons.SAVE_AS,
                false,
                listOf(
                    Messages.get("app.drawing") to Messages.get("app.save.as.drawing"),
                    Messages.get("app.save.as.template") to Messages.get("app.save.as.template.text"),
                    Messages.get("app.save.as.standards") to Messages.get("app.save.as.standards.text"),
                    Messages.get("app.other.formats") to Messages.get("app.save.as.other.text"),
                ),
            ),
            AppMenuEntry(
                "import",
                Messages.get("app.import"),
                CadIcons.IMPORT_PDF,
                true,
                listOf("PDF" to Messages.get("app.import.pdf"), "DGN" to Messages.get("app.import.dgn"), Messages.get("app.other.formats") to Messages.get("app.import.other")),
            ),
            AppMenuEntry(
                "export",
                Messages.get("app.export"),
                CadIcons.EXPORT_PDF,
                false,
                listOf("PDF" to Messages.get("app.export.pdf"), "DWF" to Messages.get("app.export.dwf"), "DGN" to Messages.get("app.export.dgn"), "FBX" to Messages.get("app.export.fbx")),
            ),
            AppMenuEntry(
                "publish",
                Messages.get("app.publish"),
                CadIcons.PUBLISH,
                false,
                listOf(Messages.get("app.publish.share") to Messages.get("app.publish.share.text"), Messages.get("app.publish.print3d") to Messages.get("app.publish.print3d.text"), Messages.get("app.publish.archive") to Messages.get("app.publish.archive.text"), Messages.get("app.publish.etransmit") to Messages.get("app.publish.etransmit.text")),
            ),
            AppMenuEntry(
                "print",
                Messages.get("app.print"),
                CadIcons.PLOT,
                false,
                listOf(Messages.get("qat.plot") to Messages.get("app.print.plot.text"), Messages.get("app.print.batch") to Messages.get("app.print.batch.text"), Messages.get("app.print.page.setup") to Messages.get("app.print.page.setup.text"), Messages.get("app.print.preview") to Messages.get("app.print.preview.text")),
            ),
            AppMenuEntry(
                "utilities",
                Messages.get("app.utilities"),
                CadIcons.UNITS,
                true,
                listOf(Messages.get("app.utilities.units") to Messages.get("app.utilities.units.text"), Messages.get("app.utilities.audit") to Messages.get("app.utilities.audit.text"), Messages.get("app.utilities.status") to Messages.get("app.utilities.status.text"), Messages.get("app.utilities.purge") to Messages.get("app.utilities.purge.text"), Messages.get("app.utilities.recover") to Messages.get("app.utilities.recover.text")),
            ),
            AppMenuEntry("close", Messages.get("app.close"), CadIcons.CLOSE, false, listOf(Messages.get("app.close.current") to Messages.get("app.close.current.text"), Messages.get("app.close.all") to Messages.get("app.close.all.text"))),
        )
    }
}
