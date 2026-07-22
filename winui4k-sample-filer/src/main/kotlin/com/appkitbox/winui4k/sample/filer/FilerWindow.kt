package com.appkitbox.winui4k.sample.filer

import com.appkitbox.winui4k.ContentDialogButton
import com.appkitbox.winui4k.ContentDialogResult
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.ItemsViewSelectionMode
import com.appkitbox.winui4k.ListViewSelectionMode
import com.appkitbox.winui4k.NavigationViewBackButtonVisible
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.SystemBackdropType
import com.appkitbox.winui4k.TabViewWidthMode
import com.appkitbox.winui4k.TextAlignment
import com.appkitbox.winui4k.TextTrimming
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.VirtualKey
import com.appkitbox.winui4k.VirtualKeyModifier
import com.appkitbox.winui4k.WAppBarButton
import com.appkitbox.winui4k.WAutoSuggestBox
import com.appkitbox.winui4k.WBreadcrumbBar
import com.appkitbox.winui4k.WCommandBar
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WDisplayArea
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WItemContainer
import com.appkitbox.winui4k.WItemsView
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItem
import com.appkitbox.winui4k.WMenuFlyoutSeparator
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WNavigationViewItem
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WSelectorBar
import com.appkitbox.winui4k.WSelectorBarItem
import com.appkitbox.winui4k.WTabView
import com.appkitbox.winui4k.WTabViewItem
import com.appkitbox.winui4k.WTable
import com.appkitbox.winui4k.WTableColumn
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTitleBar
import com.appkitbox.winui4k.WUniformGridLayout
import com.appkitbox.winui4k.WinUiUtilities
import java.io.File
import java.io.IOException

/** The state held by a single tab (the current folder, back / forward history, and view mode). */
internal class FilerTab(var directory: File) {
    val backStack = ArrayDeque<File>()
    val forwardStack = ArrayDeque<File>()
    var isIconView = false
}

/**
 * The Fluent Design filer's main window.
 * Composed of a title-bar-integrated TabView, a navigation toolbar + BreadcrumbBar, a CommandBar,
 * a NavigationView sidebar, a SelectorBar for switching the view,
 * a details (WTable) / icon (WItemsView) listing, and a status bar.
 */
@Suppress("TooManyFunctions") // Acts as the controller for the whole window, so it naturally has one method per feature
internal class FilerWindow {
    private val frame = WFrame(title = "winui4k Filer")
    private val titleBar = WTitleBar()
    private val tabView = WTabView()

    private val tabs = mutableListOf(FilerTab(defaultDirectory()))
    private var activeTabIndex = 0
    private val activeTab: FilerTab
        get() = tabs[activeTabIndex]

    /** A guard so TabView's SelectionChanged is ignored while tabs are being added/removed. */
    private var isSyncingTabs = false

    // The toolbar (back / forward / up / refresh + BreadcrumbBar / path entry + view switch + filter)
    private val backButton = compactButton("Back (Alt+←)", Symbol.BACK)
    private val forwardButton = compactButton("Forward (Alt+→)", Symbol.FORWARD)
    private val upButton = compactButton("Up (Alt+↑)", Symbol.UP)
    private val refreshButton = compactButton("Refresh (F5)", Symbol.REFRESH)
    private val breadcrumbBar = WBreadcrumbBar()
    private val pathBox = WAutoSuggestBox("Type a path and press Enter")
    private val pathEditButton = compactButton("Edit path (Ctrl+L)", Symbol.EDIT)
    private val viewSelector = WSelectorBar()
    private val filterBox = WTextField("Filter")

    /** The File corresponding to each level of BreadcrumbBar (looks up the destination from ItemClicked's index). */
    private var breadcrumbParts: List<File> = emptyList()

    // The file listing (details = WTable / icons = WItemsView)
    private val table = WTable(
        listOf(
            WTableColumn("Name", width = 320.0, comparator = NAME_COMPARATOR),
            WTableColumn("Date modified", width = 150.0),
            WTableColumn("Type", width = 150.0),
            WTableColumn("Size", width = 110.0, comparator = SIZE_COMPARATOR),
        ),
    )
    private val itemsView = WItemsView()
    private val contentHost = WGrid()
    private val navigationView = WNavigationView()
    private val sidebarTargets = LinkedHashMap<WNavigationViewItem, File>()

    private val itemCountLabel = WLabel("")
    private val selectionLabel = WLabel("")

    /** All entries in the current folder (already sorted folders-first), and the entries shown after applying the filter. */
    private var allEntries: List<File> = emptyList()
    private var displayedEntries: List<File> = emptyList()

    /** The generation counter for asynchronous enumeration. Used to discard stale enumeration results after navigating to a different folder. */
    private var enumerationGeneration = 0

    fun show() {
        buildFileViews()
        buildToolbarActions()
        buildSidebar()

        val rootGrid = WGrid()
        rootGrid.addColumn(GridLength.star())
        rootGrid.addRow(GridLength.AUTO)
        rootGrid.addRow(GridLength.AUTO)
        rootGrid.addRow(GridLength.AUTO)
        rootGrid.addRow(GridLength.star())
        rootGrid.addRow(GridLength.AUTO)
        rootGrid.add(buildTitleBar(), row = 0, column = 0)
        rootGrid.add(buildToolbar(), row = 1, column = 0)
        rootGrid.add(buildCommandBar(), row = 2, column = 0)
        rootGrid.add(buildNavigationArea(), row = 3, column = 0)
        rootGrid.add(buildStatusBar(), row = 4, column = 0)

        frame.setContentPane(rootGrid)
        frame.extendsContentIntoTitleBar = true
        frame.setTitleBar(titleBar)
        frame.systemBackdrop = SystemBackdropType.MICA
        resizeToWorkArea()
        refresh()
        frame.isVisible = true
    }

    /** Sizes the window as a ratio of the monitor's work area (a physical-pixel size, so this follows DPI). */
    private fun resizeToWorkArea() {
        val workArea = WDisplayArea.primary().workArea
        frame.appWindow.resize(
            (workArea.width * WINDOW_WIDTH_RATIO).toInt(),
            (workArea.height * WINDOW_HEIGHT_RATIO).toInt(),
        )
    }

    // region Building the UI

    private fun buildTitleBar(): WComponent {
        titleBar.iconGlyph = "" // The Folder glyph in Segoe Fluent Icons

        tabView.tabWidthMode = TabViewWidthMode.SIZE_TO_CONTENT
        tabView.canReorderTabs = false // Tabs are managed by index, so reordering is disabled
        tabView.isAddTabButtonVisible = true
        tabView.addTab(WTabViewItem(tabLabel(activeTab.directory)))
        tabView.selectedIndex = 0
        tabView.addAddTabButtonClickListener { addTab() }
        tabView.addTabCloseRequestedListener { index -> closeTab(index) }
        tabView.addSelectionListener {
            if (isSyncingTabs) return@addSelectionListener
            val index = tabView.selectedIndex
            if (index >= 0 && index != activeTabIndex) activateTab(index)
        }
        // Ctrl+T / Ctrl+W ride on TabView as app-wide shortcuts for tab operations
        tabView.addKeyboardAccelerator(VirtualKey.T, VirtualKeyModifier.CONTROL) { addTab() }
        tabView.addKeyboardAccelerator(VirtualKey.W, VirtualKeyModifier.CONTROL) { closeTab(activeTabIndex) }

        // Left-align the tab strip like Files does (TitleBar's content area is centered by default)
        tabView.horizontalAlignment = HorizontalAlignment.LEFT
        titleBar.content = tabView
        WTitleBar.setIsDragRegion(tabView, false)
        return titleBar
    }

    private fun buildToolbar(): WComponent {
        val navPanel = WPanel(spacing = 0.0, orientation = Orientation.HORIZONTAL)
        navPanel.add(backButton)
        navPanel.add(forwardButton)
        navPanel.add(upButton)
        navPanel.add(refreshButton)

        pathBox.isVisible = false
        pathBox.verticalAlignment = VerticalAlignment.CENTER
        breadcrumbBar.verticalAlignment = VerticalAlignment.CENTER
        val addressHost = WGrid()
        addressHost.addRow(GridLength.star())
        addressHost.addColumn(GridLength.star())
        addressHost.add(breadcrumbBar, row = 0, column = 0)
        addressHost.add(pathBox, row = 0, column = 0)

        viewSelector.addItem(WSelectorBarItem("Details"))
        viewSelector.addItem(WSelectorBarItem("Icons"))
        viewSelector.selectedIndex = 0
        viewSelector.addSelectionListener { index -> setIconView(index == 1) }
        viewSelector.verticalAlignment = VerticalAlignment.CENTER

        filterBox.verticalAlignment = VerticalAlignment.CENTER

        val toolbar = WGrid()
        toolbar.columnSpacing = 8.0
        toolbar.setMargin(8.0, 4.0, 8.0, 4.0)
        toolbar.addRow(GridLength.AUTO)
        toolbar.addColumn(GridLength.AUTO)
        toolbar.addColumn(GridLength.star())
        toolbar.addColumn(GridLength.AUTO)
        toolbar.addColumn(GridLength.AUTO)
        toolbar.addColumn(GridLength.pixel(FILTER_BOX_WIDTH))
        toolbar.add(navPanel, row = 0, column = 0)
        toolbar.add(addressHost, row = 0, column = 1)
        toolbar.add(pathEditButton, row = 0, column = 2)
        toolbar.add(viewSelector, row = 0, column = 3)
        toolbar.add(filterBox, row = 0, column = 4)
        return toolbar
    }

    private fun buildCommandBar(): WComponent {
        val commandBar = WCommandBar()
        commandBar.addPrimaryCommand(
            commandButton("New folder", Symbol.NEW_FOLDER, VirtualKey.N, VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT) {
                createNewFolder()
            },
        )
        commandBar.addPrimaryCommand(
            commandButton("Cut", Symbol.CUT, VirtualKey.X, VirtualKeyModifier.CONTROL) { cutSelection() },
        )
        commandBar.addPrimaryCommand(
            commandButton("Copy", Symbol.COPY, VirtualKey.C, VirtualKeyModifier.CONTROL) { copySelection() },
        )
        commandBar.addPrimaryCommand(
            commandButton("Paste", Symbol.PASTE, VirtualKey.V, VirtualKeyModifier.CONTROL) { pasteClipboard() },
        )
        commandBar.addPrimaryCommand(
            commandButton("Rename", Symbol.RENAME, VirtualKey.F2) { renameSelection() },
        )
        commandBar.addPrimaryCommand(
            commandButton("Delete", Symbol.DELETE, VirtualKey.DELETE) { deleteSelection() },
        )
        return commandBar
    }

    private fun buildNavigationArea(): WComponent {
        contentHost.addRow(GridLength.star())
        contentHost.addColumn(GridLength.star())
        contentHost.setMargin(0.0, 0.0, 8.0, 0.0)
        navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
        navigationView.isSettingsVisible = false
        navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
        navigationView.isPaneToggleButtonVisible = false
        navigationView.openPaneLength = SIDEBAR_WIDTH
        navigationView.content = contentHost
        return navigationView
    }

    private fun buildStatusBar(): WComponent {
        val statusBar = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
        statusBar.setMargin(12.0, 4.0, 12.0, 4.0)
        itemCountLabel.fontSize = STATUS_FONT_SIZE
        selectionLabel.fontSize = STATUS_FONT_SIZE
        statusBar.add(itemCountLabel)
        statusBar.add(selectionLabel)
        return statusBar
    }

    private fun buildSidebar() {
        val home = System.getProperty("user.home")
        addSidebarItem("Home", Symbol.HOME, File(home))
        addSidebarItem("Desktop", Symbol.FOLDER, File(home, "Desktop"))
        addSidebarItem("Documents", Symbol.DOCUMENT, File(home, "Documents"))
        addSidebarItem("Downloads", Symbol.DOWNLOAD, File(home, "Downloads"))
        addSidebarItem("Pictures", Symbol.PICTURES, File(home, "Pictures"))
        File.listRoots()?.forEach { root ->
            addSidebarItem(root.path, Symbol.MAP_DRIVE, root)
        }
        navigationView.addSelectionListener { item ->
            val target = item?.let { sidebarTargets[it] }
            if (target != null && target != activeTab.directory) navigateTo(target)
        }
    }

    private fun addSidebarItem(label: String, icon: Symbol, target: File) {
        if (!target.isDirectory) return
        val item = WNavigationViewItem(label, icon)
        navigationView.addItem(item)
        sidebarTargets[item] = target
    }

    private fun buildFileViews() {
        table.selectionMode = ListViewSelectionMode.EXTENDED
        table.addRowInvokedListener { index -> displayedEntries.getOrNull(index)?.let(::open) }
        table.addRowSelectionListener { updateSelectionLabel() }
        table.contextFlyout = buildContextMenu()

        val layout = WUniformGridLayout()
        layout.minItemWidth = ICON_ITEM_WIDTH
        layout.minColumnSpacing = 8.0
        layout.minRowSpacing = 8.0
        itemsView.layout = layout
        itemsView.selectionMode = ItemsViewSelectionMode.NONE
        itemsView.isItemInvokedEnabled = true
        itemsView.addItemInvokedListener { index -> displayedEntries.getOrNull(index)?.let(::open) }
    }

    private fun buildContextMenu(): WMenuFlyout {
        val menu = WMenuFlyout()
        menu.add(menuItem("Open", Symbol.OPEN_FILE) { selectedFiles().firstOrNull()?.let(::open) })
        menu.add(WMenuFlyoutSeparator())
        menu.add(menuItem("Cut", Symbol.CUT) { cutSelection() })
        menu.add(menuItem("Copy", Symbol.COPY) { copySelection() })
        menu.add(menuItem("Paste", Symbol.PASTE) { pasteClipboard() })
        menu.add(menuItem("Delete", Symbol.DELETE) { deleteSelection() })
        menu.add(menuItem("Delete permanently", Symbol.DELETE) { deleteSelectionPermanently() })
        menu.add(menuItem("Rename", Symbol.RENAME) { renameSelection() })
        menu.add(WMenuFlyoutSeparator())
        menu.add(menuItem("Copy path", Symbol.LINK) { copySelectedPaths() })
        menu.add(menuItem("New folder", Symbol.NEW_FOLDER) { createNewFolder() })
        return menu
    }

    private fun buildToolbarActions() {
        backButton.addKeyboardAccelerator(VirtualKey.LEFT, VirtualKeyModifier.MENU)
        backButton.addActionListener { goBack() }
        forwardButton.addKeyboardAccelerator(VirtualKey.RIGHT, VirtualKeyModifier.MENU)
        forwardButton.addActionListener { goForward() }
        upButton.addKeyboardAccelerator(VirtualKey.UP, VirtualKeyModifier.MENU)
        upButton.addActionListener { goUp() }
        refreshButton.addKeyboardAccelerator(VirtualKey.F5)
        refreshButton.addActionListener { refresh() }

        pathEditButton.addKeyboardAccelerator(VirtualKey.L, VirtualKeyModifier.CONTROL)
        pathEditButton.addActionListener { togglePathBox() }
        pathBox.addQuerySubmittedListener { query, _ ->
            val target = File(query.trim())
            if (target.isDirectory) {
                showPathBox(false)
                navigateTo(target)
            }
        }
        breadcrumbBar.addItemClickedListener { index ->
            breadcrumbParts.getOrNull(index)?.let { navigateTo(it) }
        }
        filterBox.addTextChangedListener { applyFilterAndShow() }
    }

    // endregion

    // region Navigation

    private fun navigateTo(directory: File, recordHistory: Boolean = true) {
        val target = directory.absoluteFile
        if (!target.isDirectory) return
        if (recordHistory && target != activeTab.directory) {
            activeTab.backStack.addLast(activeTab.directory)
            activeTab.forwardStack.clear()
        }
        activeTab.directory = target
        refresh()
    }

    private fun goBack() {
        val previous = activeTab.backStack.removeLastOrNull() ?: return
        activeTab.forwardStack.addLast(activeTab.directory)
        activeTab.directory = previous
        refresh()
    }

    private fun goForward() {
        val next = activeTab.forwardStack.removeLastOrNull() ?: return
        activeTab.backStack.addLast(activeTab.directory)
        activeTab.directory = next
        refresh()
    }

    private fun goUp() {
        activeTab.directory.parentFile?.let { navigateTo(it) }
    }

    private fun open(file: File) {
        if (file.isDirectory) {
            navigateTo(file)
        } else {
            FileOperations.openWithShell(file)
        }
    }

    /** Re-enumerates the active tab's contents and refreshes the display. Enumeration runs on a separate thread so it doesn't block the UI. */
    private fun refresh() {
        tabView.getTab(activeTabIndex).header = tabLabel(activeTab.directory)
        updateBreadcrumb()
        updateNavButtons()
        selectionLabel.text = ""
        itemCountLabel.text = "Loading..."

        val directory = activeTab.directory
        val generation = ++enumerationGeneration
        Thread {
            val entries = sortEntries(directory.listFiles()?.toList() ?: emptyList())
            WinUiUtilities.invokeLater {
                if (generation == enumerationGeneration) {
                    allEntries = entries
                    applyFilterAndShow()
                }
            }
        }.start()
    }

    // endregion

    // region Updating the display

    private fun applyFilterAndShow() {
        val filter = filterBox.text.trim().lowercase()
        displayedEntries = if (filter.isEmpty()) {
            allEntries
        } else {
            allEntries.filter { it.name.lowercase().contains(filter) }
        }
        contentHost.removeAll()
        if (activeTab.isIconView) {
            itemsView.setItems(displayedEntries.map(::buildIconCard))
            contentHost.add(itemsView, row = 0, column = 0)
        } else {
            table.removeAllRows()
            table.addRows(
                displayedEntries.map { listOf(it.name, formatDate(it), formatKind(it), formatSize(it)) },
            )
            contentHost.add(table, row = 0, column = 0)
        }
        itemCountLabel.text = "${displayedEntries.size} items"
        updateSelectionLabel()
    }

    private fun buildIconCard(file: File): WItemContainer {
        val glyph = WLabel(if (file.isDirectory) "" else "") // Folder / Document
        glyph.fontFamily = "Segoe Fluent Icons"
        glyph.fontSize = ICON_GLYPH_SIZE
        glyph.horizontalAlignment = HorizontalAlignment.CENTER

        val name = WLabel(file.name)
        name.textTrimming = TextTrimming.WORD_ELLIPSIS
        name.textAlignment = TextAlignment.CENTER

        val card = WPanel(spacing = 4.0, orientation = Orientation.VERTICAL)
        card.margin = 8.0
        card.add(glyph)
        card.add(name)
        return WItemContainer(card)
    }

    private fun setIconView(isIconView: Boolean) {
        if (activeTab.isIconView == isIconView) return
        activeTab.isIconView = isIconView
        applyFilterAndShow()
    }

    private fun updateBreadcrumb() {
        breadcrumbParts = generateSequence(activeTab.directory) { it.parentFile }.toList().asReversed()
        breadcrumbBar.setItems(breadcrumbParts.map(::tabLabel))
    }

    private fun updateNavButtons() {
        backButton.isEnabled = activeTab.backStack.isNotEmpty()
        forwardButton.isEnabled = activeTab.forwardStack.isNotEmpty()
        upButton.isEnabled = activeTab.directory.parentFile != null
    }

    private fun updateSelectionLabel() {
        val files = selectedFiles()
        selectionLabel.text = if (files.isEmpty()) {
            ""
        } else {
            val bytes = files.filter { it.isFile }.sumOf { it.length() }
            "${files.size} items selected ${formatSizeApprox(bytes)}"
        }
    }

    private fun togglePathBox() {
        showPathBox(!pathBox.isVisible)
    }

    private fun showPathBox(show: Boolean) {
        pathBox.isVisible = show
        breadcrumbBar.isVisible = !show
        if (show) pathBox.text = activeTab.directory.absolutePath
    }

    // endregion

    // region Tabs

    private fun activateTab(index: Int) {
        activeTabIndex = index
        filterBox.text = ""
        refresh()
    }

    private fun addTab() {
        tabs.add(FilerTab(defaultDirectory()))
        isSyncingTabs = true
        tabView.addTab(WTabViewItem(tabLabel(tabs.last().directory)))
        isSyncingTabs = false
        activeTabIndex = tabs.size - 1
        tabView.selectedIndex = activeTabIndex
        filterBox.text = ""
        refresh()
    }

    private fun closeTab(index: Int) {
        if (tabs.size <= 1) return
        tabs.removeAt(index)
        isSyncingTabs = true
        tabView.removeTab(index)
        // TabView moves the selection on its own, so match our own state to the result
        activeTabIndex = tabView.selectedIndex.coerceIn(0, tabs.size - 1)
        if (tabView.selectedIndex != activeTabIndex) tabView.selectedIndex = activeTabIndex
        isSyncingTabs = false
        filterBox.text = ""
        refresh()
    }

    // endregion

    // region File operations

    /** The currently selected files. Returns empty in icon view, since it's browse-only for now (MVP). */
    private fun selectedFiles(): List<File> {
        if (activeTab.isIconView) return emptyList()
        return table.selectedRows.mapNotNull { displayedEntries.getOrNull(it) }
    }

    private fun copySelection() {
        val files = selectedFiles()
        if (files.isNotEmpty()) FileOperations.copy(files)
    }

    private fun cutSelection() {
        val files = selectedFiles()
        if (files.isNotEmpty()) FileOperations.cut(files)
    }

    private fun pasteClipboard() {
        if (FileOperations.clipboardFiles.isEmpty()) return
        val directory = activeTab.directory
        runInBackground(
            action = { FileOperations.paste(directory) },
            onDone = { error ->
                if (error != null) showError(error) else refresh()
            },
        )
    }

    private fun deleteSelection() {
        val files = selectedFiles()
        if (files.isEmpty()) return
        runInBackground(
            action = {
                if (!FileOperations.moveToRecycleBin(files)) {
                    throw IOException("Couldn't move to the Recycle Bin. Use \"Delete permanently\" instead.")
                }
            },
            onDone = { error ->
                if (error != null) showError(error) else refresh()
            },
        )
    }

    private fun deleteSelectionPermanently() {
        val files = selectedFiles()
        if (files.isEmpty()) return
        val message = WLabel("This will permanently delete ${files.size} item(s). This action cannot be undone.")
        val dialog = WContentDialog("Delete permanently", message)
        dialog.primaryButtonText = "Delete"
        dialog.closeButtonText = "Cancel"
        dialog.defaultButton = ContentDialogButton.CLOSE
        dialog.show(navigationView) { result ->
            if (result == ContentDialogResult.PRIMARY) {
                FileOperations.deletePermanently(files)
                refresh()
            }
        }
    }

    private fun renameSelection() {
        val file = selectedFiles().firstOrNull() ?: return
        val field = WTextField()
        field.text = file.name
        val dialog = WContentDialog("Rename", field)
        dialog.primaryButtonText = "Rename"
        dialog.closeButtonText = "Cancel"
        dialog.defaultButton = ContentDialogButton.PRIMARY
        dialog.show(navigationView) { result ->
            if (result != ContentDialogResult.PRIMARY) return@show
            val newName = field.text.trim()
            if (newName.isEmpty() || newName == file.name) return@show
            try {
                FileOperations.rename(file, newName)
                refresh()
            } catch (e: IOException) {
                showError(e.message ?: "Couldn't rename")
            }
        }
    }

    private fun createNewFolder() {
        val field = WTextField()
        field.text = "New folder"
        val dialog = WContentDialog("New folder", field)
        dialog.primaryButtonText = "Create"
        dialog.closeButtonText = "Cancel"
        dialog.defaultButton = ContentDialogButton.PRIMARY
        dialog.show(navigationView) { result ->
            if (result != ContentDialogResult.PRIMARY) return@show
            val name = field.text.trim()
            if (name.isEmpty()) return@show
            try {
                FileOperations.createFolder(activeTab.directory, name)
                refresh()
            } catch (e: IOException) {
                showError(e.message ?: "Couldn't create the folder")
            }
        }
    }

    private fun copySelectedPaths() {
        val files = selectedFiles().ifEmpty { listOf(activeTab.directory) }
        FileOperations.copyTextToClipboard(files.joinToString("\n") { it.absolutePath })
    }

    // endregion

    // region Helpers

    /** Runs [action] on a separate thread, then calls [onDone] on the UI thread once it finishes (with a message if it failed). */
    private fun runInBackground(action: () -> Unit, onDone: (String?) -> Unit) {
        Thread {
            val error = try {
                action()
                null
            } catch (e: IOException) {
                e.message ?: "The operation failed"
            }
            WinUiUtilities.invokeLater { onDone(error) }
        }.start()
    }

    private fun showError(message: String) {
        val dialog = WContentDialog("Error", WLabel(message))
        dialog.closeButtonText = "OK"
        dialog.show(navigationView)
    }

    private fun compactButton(toolTip: String, icon: Symbol): WAppBarButton {
        val button = WAppBarButton("", icon)
        button.isCompact = true
        button.toolTip = toolTip
        return button
    }

    private fun commandButton(
        label: String,
        icon: Symbol,
        key: VirtualKey,
        vararg modifiers: VirtualKeyModifier,
        action: () -> Unit,
    ): WAppBarButton {
        val button = WAppBarButton(label, icon)
        button.addKeyboardAccelerator(key, *modifiers)
        button.addActionListener(action)
        return button
    }

    private fun menuItem(text: String, icon: Symbol, action: () -> Unit): WMenuFlyoutItem {
        val item = WMenuFlyoutItem(text, icon)
        item.addActionListener(action)
        return item
    }

    private fun tabLabel(directory: File): String =
        directory.name.ifEmpty { directory.path.removeSuffix(File.separator) }

    private companion object {
        const val WINDOW_WIDTH_RATIO = 0.6
        const val WINDOW_HEIGHT_RATIO = 0.7
        const val SIDEBAR_WIDTH = 220.0
        const val FILTER_BOX_WIDTH = 200.0
        const val ICON_ITEM_WIDTH = 120.0
        const val ICON_GLYPH_SIZE = 40.0
        const val STATUS_FONT_SIZE = 12.0

        fun defaultDirectory(): File = File(System.getProperty("user.home"))
    }
}
