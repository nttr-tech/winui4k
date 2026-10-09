package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.ContentDialogButton
import com.appkitbox.winui4k.ContentDialogResult
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomGroup
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomTab
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomization
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomizePage
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonQuickAccessPosition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonState
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStateSerializer
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import java.util.UUID

/**
 * Office's "Customize the Ribbon" / "Quick Access Toolbar" dialog (RibbonCustomizeDialog in RibbonSpace).
 *
 * It has the command list (with search), the tree of tabs and groups (with visibility check boxes), Add / Remove,
 * Move Up / Move Down, New Tab / New Group, Rename, Reset, and JSON import / export. It edits a copy of the state
 * ([workingState]) and applies it on OK.
 */
class RibbonCustomizeDialog(private val ribbon: WRibbon, private val page: RibbonCustomizePage) {
    /** A copy of the state being edited (applied on OK). */
    val workingState: RibbonState = ribbon.getState().also { if (it.quickAccessItemIds == null) it.quickAccessItemIds = mutableListOf() }

    private val c: RibbonCustomization get() = workingState.customization
    private val strings get() = RibbonStrings.current
    private val commands: List<CommandRow> = collectCommands()
    private val commandList = SelectableList()
    private val structureList = SelectableList()
    private val qatCommandList = SelectableList()
    private val qatList = SelectableList()
    private val search = XamlElement.load("<TextBox />")
    private val qatBelow = XamlElement.load("<CheckBox />")
    private var rows: List<StructureRow> = emptyList()
    private val dialog = WContentDialog()
    private val ribbonPage = XamlElement.load("<Grid />")
    private val qatPage = XamlElement.load("<Grid />")
    private var nested: (() -> Unit)? = null

    private data class CommandRow(val id: String, val label: String, val path: String)

    private data class StructureRow(val id: String, val label: String, val level: Int, val hidden: Boolean, val isCustom: Boolean, val parentId: String?)

    init {
        search.setPlaceholderText(strings.search)
        qatBelow.setContentText(strings.showBelowRibbon)
        dialog.title = if (page == RibbonCustomizePage.RIBBON) strings.customizeRibbon.trimEnd('.') else strings.customizeQuickAccessToolbar
        dialog.primaryButtonText = strings.ok
        dialog.closeButtonText = strings.cancel
        dialog.defaultButton = ContentDialogButton.PRIMARY
        dialog.content = buildContent()
        search.onTextChanged { fillCommands() }
        structureList.onToggle = { id, visible -> onVisibilityToggled(id, visible) }
        fillCommands()
        fillStructure()
        qatCommandList.fill(commands.map { ListRow(it.id, it.label, it.path, 0, null) })
        fillQuickAccess()
    }

    /** Opens the dialog. */
    fun show() {
        try {
            dialog.show(ribbon) { result -> onClosed(result) }
        } catch (e: IllegalStateException) {
            // Only one ContentDialog can be open per window (it is not shown while another dialog is open)
            System.err.println("RibbonCustomizeDialog: ${e.message}")
        }
    }

    private fun onClosed(result: ContentDialogResult) {
        val next = nested
        nested = null
        when {
            next != null -> next()
            result == ContentDialogResult.PRIMARY -> ribbon.applyState(workingState)
        }
    }

    /**
     * A nested dialog (Rename / Import): closes this dialog, shows the nested one, and shows this dialog again when it
     * closes.
     */
    private fun runNested(build: () -> WContentDialog, onPrimary: () -> Unit) {
        nested = {
            val inner = build()
            inner.show(ribbon) { result ->
                if (result == ContentDialogResult.PRIMARY) onPrimary()
                show()
            }
        }
        dialog.hide()
    }

    private fun buildContent(): WComponent {
        val root = XamlElement.load("<StackPanel Width=\"860\" />")
        val ribbonTab = XamlElement.load("<Button Padding=\"12,6,12,6\" />").also { it.setContentText(strings.customizeRibbon.trimEnd('.')) }
        val qatTab = XamlElement.load("<Button Padding=\"12,6,12,6\" />").also { it.setContentText(strings.quickAccessToolbar) }
        val header = XamlElement.load("<StackPanel Orientation=\"Horizontal\" Spacing=\"4\" Margin=\"0,0,0,10\" />")
        header.addChild(ribbonTab)
        header.addChild(qatTab)
        buildRibbonPage()
        buildQuickAccessPage()
        val pages = XamlElement.load("<Grid />")
        pages.addChild(ribbonPage)
        pages.addChild(qatPage)
        fun showPage(p: RibbonCustomizePage) {
            ribbonPage.isVisible = p == RibbonCustomizePage.RIBBON
            qatPage.isVisible = p == RibbonCustomizePage.QUICK_ACCESS_TOOLBAR
            markTab(ribbonTab, p == RibbonCustomizePage.RIBBON)
            markTab(qatTab, p == RibbonCustomizePage.QUICK_ACCESS_TOOLBAR)
        }
        ribbonTab.onClick { showPage(RibbonCustomizePage.RIBBON) }
        qatTab.onClick { showPage(RibbonCustomizePage.QUICK_ACCESS_TOOLBAR) }
        showPage(page)
        root.addChild(header)
        root.addChild(pages)
        Xaml.rootSize(ribbon)?.let { size -> root.setSize(maxOf(MIN_WIDTH, minOf(MAX_WIDTH, size[0] - DIALOG_MARGIN)), Double.NaN) }
        return root
    }

    private fun markTab(button: XamlElement, selected: Boolean) {
        button.opacity = if (selected) 1.0 else UNSELECTED_OPACITY
        com.appkitbox.winui4k.internal.winui.XamlStructs.putFontWeight(
            button.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IControl),
            com.appkitbox.winui4k.internal.winui.XamlInterop.IControl_put_FontWeight,
            if (selected) SEMI_BOLD else NORMAL,
        )
    }

    private fun command(text: String, action: () -> Unit): XamlElement {
        val button = XamlElement.load("<Button HorizontalAlignment=\"Stretch\" MinWidth=\"110\" />")
        button.setContentText(text)
        button.onClick(action)
        return button
    }

    private fun stack(vararg children: WComponent): XamlElement {
        val panel = XamlElement.load("<StackPanel Spacing=\"6\" VerticalAlignment=\"Center\" />")
        children.forEach { panel.addChild(it) }
        return panel
    }

    private fun labeled(label: String, content: WComponent): XamlElement {
        val panel = XamlElement.load("<StackPanel Spacing=\"6\"><TextBlock Text=\"${Xaml.escape(label)}\" FontWeight=\"SemiBold\" /></StackPanel>")
        panel.addChild(content)
        return panel
    }

    private fun columns(target: XamlElement, left: WComponent, middle: XamlElement, right: WComponent, buttons: XamlElement) {
        val grid = XamlElement.load(
            "<Grid ColumnSpacing=\"12\"><Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" />" +
                "<ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions></Grid>",
        )
        listOf(left, middle, right, buttons).forEachIndexed { index, child ->
            grid.addChild(child)
            (child as XamlElement).setGridCell(0, index)
        }
        target.addChild(grid)
    }

    private fun buildRibbonPage() {
        val left = labeled(strings.chooseCommands, stack(search, commandList.root))
        val middle = stack(command(strings.add) { addToCustomGroup() }, command(strings.remove) { removeFromCustomGroup() })
        val right = labeled(strings.mainTabs, structureList.root)
        val buttons = stack(
            command(strings.moveUp) { moveStructure(-1) },
            command(strings.moveDown) { moveStructure(1) },
            command(strings.newTab) { newTab() },
            command(strings.newGroup) { newGroup() },
            command(strings.rename) { rename() },
            command(strings.reset) {
                workingState.customization = RibbonCustomization()
                fillStructure()
            },
            command(strings.importExport) { importExport() },
        )
        columns(ribbonPage, left, middle, right, buttons)
    }

    private fun buildQuickAccessPage() {
        val left = labeled(strings.chooseCommands, qatCommandList.root)
        val middle = stack(
            command(strings.add) {
                val row = qatCommandList.selected ?: return@command
                val ids = workingState.quickAccessItemIds!!
                if (row.id !in ids) {
                    ids += row.id
                    fillQuickAccess()
                }
            },
            command(strings.remove) {
                val row = qatList.selected ?: return@command
                workingState.quickAccessItemIds!!.remove(row.id)
                fillQuickAccess()
            },
        )
        val right = labeled(strings.quickAccessToolbar, qatList.root)
        qatBelow.toggleChecked = workingState.quickAccessPosition == RibbonQuickAccessPosition.BELOW_RIBBON
        qatBelow.onToggled { checked ->
            workingState.quickAccessPosition = if (checked == true) RibbonQuickAccessPosition.BELOW_RIBBON else RibbonQuickAccessPosition.ABOVE_RIBBON
        }
        val buttons = stack(
            command(strings.moveUp) { moveQuickAccess(-1) },
            command(strings.moveDown) { moveQuickAccess(1) },
            command(strings.reset) {
                // Restore the application default (as of the ribbon's first load), not the current QAT
                workingState.quickAccessItemIds = (ribbon.defaultQuickAccessItemIds ?: ribbon.quickAccessItemIds()).toMutableList()
                workingState.isQuickAccessVisible = true
                fillQuickAccess()
            },
            qatBelow,
        )
        columns(qatPage, left, middle, right, buttons)
    }

    // ---------------------------------------------------------------- List contents

    private fun collectCommands(): List<CommandRow> = ribbon.model.tabs.flatMap { tab ->
        tab.groups.flatMap { group ->
            RibbonModel.flatten(group.items)
                .filter { it.id != null && it.label != null && it.canAddToQuickAccess }
                .map { CommandRow(it.id!!, it.label!!.replace('\n', ' '), "${ribbon.tabLabel(tab)} › ${group.label.orEmpty()}") }
                .toList()
        }
    }.distinctBy { it.id }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

    private fun fillCommands() {
        val query = search.textBoxText
        commandList.fill(
            commands.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) || it.path.contains(query, ignoreCase = true) }
                .map { ListRow(it.id, it.label, it.path, 0, null) },
        )
    }

    private fun commandLabel(id: String): String = commands.firstOrNull { it.id == id }?.label ?: id

    private fun fillStructure() {
        val result = mutableListOf<StructureRow>()
        for (id in orderedTabIds()) {
            val custom = c.customTabs.firstOrNull { it.id == id }
            if (custom != null) addCustomTabRows(result, custom) else addTabRows(result, id)
        }
        rows = result
        val selected = structureList.selected?.id
        structureList.fill(rows.map { ListRow(it.id, it.label, null, it.level, if (it.isCustom && it.level == 2) null else !it.hidden) })
        selected?.let { structureList.select(it) }
    }

    /** The ids of the built-in tabs and the custom tabs (in the customized order). */
    private fun orderedTabIds(): List<String> {
        val ids = ribbon.model.tabs.filter { !it.isContextual && it.id != null }.map { it.id!! } + c.customTabs.map { it.id }
        if (c.tabOrder.isEmpty()) return ids
        return ids.sortedBy { id -> rank(c.tabOrder, id) }
    }

    private fun rank(order: List<String>, id: String?): Int = order.indexOf(id).let { if (it < 0) Int.MAX_VALUE else it }

    private fun addCustomTabRows(result: MutableList<StructureRow>, custom: RibbonCustomTab) {
        result += StructureRow(custom.id, custom.label + " " + strings.customGroupSuffix, 0, custom.id in c.hiddenTabIds, true, null)
        for (g in custom.groups) addCustomGroupRows(result, g, custom.id, includeSuffix = false)
    }

    private fun addTabRows(result: MutableList<StructureRow>, id: String) {
        val tab = ribbon.model.tabs.first { it.id == id }
        result += StructureRow(id, c.labels[id] ?: tab.label ?: id, 0, id in c.hiddenTabIds, false, null)
        val order = c.groupOrder[id].orEmpty()
        val groups = tab.groups.filter { it.id != null }.let { list -> if (order.isEmpty()) list else list.sortedBy { rank(order, it.id) } }
        for (group in groups) {
            val groupId = group.id!!
            result += StructureRow(groupId, c.labels[groupId] ?: group.label ?: groupId, 1, groupId in c.hiddenGroupIds, false, id)
        }
        for (g in c.customGroups.filter { it.tabId == id }) addCustomGroupRows(result, g, id, includeSuffix = true)
    }

    private fun addCustomGroupRows(result: MutableList<StructureRow>, g: RibbonCustomGroup, tabId: String, includeSuffix: Boolean) {
        val label = if (includeSuffix) g.label + " " + strings.customGroupSuffix else g.label
        result += StructureRow(g.id, label, 1, g.id in c.hiddenGroupIds, true, tabId)
        for (itemId in g.itemIds) result += StructureRow(g.id + "/" + itemId, commandLabel(itemId), 2, false, true, g.id)
    }

    private fun fillQuickAccess() {
        qatList.fill(
            workingState.quickAccessItemIds!!.map { id ->
                val command = commands.firstOrNull { it.id == id }
                ListRow(id, command?.label ?: ribbon.model.findItem(id)?.label ?: id, command?.path.orEmpty(), 0, null)
            },
        )
    }

    // ---------------------------------------------------------------- Editing

    private val selectedRow: StructureRow? get() = structureList.selected?.let { s -> rows.firstOrNull { it.id == s.id } }

    private fun onVisibilityToggled(id: String, visible: Boolean) {
        val row = rows.firstOrNull { it.id == id } ?: return
        if (row.level > 1) return
        val list = if (row.level == 0) c.hiddenTabIds else c.hiddenGroupIds
        list.remove(row.id)
        if (!visible) list += row.id
    }

    private fun findCustomGroup(id: String): RibbonCustomGroup? =
        c.customGroups.firstOrNull { it.id == id } ?: c.customTabs.flatMap { it.groups }.firstOrNull { it.id == id }

    private fun targetGroup(): RibbonCustomGroup? {
        val row = selectedRow ?: return null
        return when {
            row.level == 1 && row.isCustom -> findCustomGroup(row.id)
            row.level == 2 -> row.parentId?.let { findCustomGroup(it) }
            else -> null
        }
    }

    private fun addToCustomGroup() {
        val command = commandList.selected ?: return
        var group = targetGroup()
        if (group == null) {
            newGroup()
            group = targetGroup()
        }
        if (group != null && command.id !in group.itemIds) {
            group.itemIds += command.id
            fillStructure()
        }
    }

    private fun removeFromCustomGroup() {
        val row = selectedRow ?: return
        val parent = row.parentId
        when {
            row.level == 2 && parent != null -> findCustomGroup(parent)?.itemIds?.remove(row.id.substring(parent.length + 1))
            row.level == 1 && row.isCustom -> {
                c.customGroups.removeAll { it.id == row.id }
                c.customTabs.forEach { tab -> tab.groups.removeAll { it.id == row.id } }
            }
            row.level == 0 && row.isCustom -> {
                c.customTabs.removeAll { it.id == row.id }
                c.tabOrder.remove(row.id)
            }
        }
        fillStructure()
    }

    private fun newId(prefix: String) = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, ID_LENGTH)

    private fun newTab() {
        val groupId = newId("custom.group.")
        c.customTabs += RibbonCustomTab(newId("custom.tab."), strings.newTab).also { it.groups += RibbonCustomGroup(groupId, strings.newGroup) }
        fillStructure()
        structureList.select(groupId)
    }

    private fun newGroup() {
        val row = selectedRow
        val tabId = when {
            row == null -> ribbon.selectedTab?.id
            row.level == 0 -> row.id
            row.level == 1 -> row.parentId
            else -> rows.firstOrNull { it.id == row.parentId }?.parentId
        } ?: return
        val group = RibbonCustomGroup(newId("custom.group."), strings.newGroup, tabId)
        val customTab = c.customTabs.firstOrNull { it.id == tabId }
        if (customTab != null) {
            group.tabId = null
            customTab.groups += group
        } else {
            c.customGroups += group
        }
        fillStructure()
        structureList.select(group.id)
    }

    private fun rename() {
        val row = selectedRow ?: return
        if (row.level == 2) return
        val box = XamlElement.load("<TextBox />")
        box.textBoxText = row.label.replace(" " + strings.customGroupSuffix, "")
        runNested(
            {
                WContentDialog(strings.rename.trimEnd('.'), box).also {
                    it.primaryButtonText = strings.ok
                    it.closeButtonText = strings.cancel
                    it.defaultButton = ContentDialogButton.PRIMARY
                }
            },
        ) {
            val text = box.textBoxText
            if (text.isBlank()) return@runNested
            if (row.isCustom) {
                c.customTabs.firstOrNull { it.id == row.id }?.let { it.label = text } ?: findCustomGroup(row.id)?.let { it.label = text }
            } else {
                c.labels[row.id] = text
            }
            fillStructure()
        }
    }

    private fun moveStructure(delta: Int) {
        val row = selectedRow ?: return
        val moved = when (row.level) {
            0 -> moveTab(row, delta)
            1 -> if (row.isCustom) moveCustomGroup(row, delta) else moveBuiltInGroup(row, delta)
            else -> moveCustomItem(row, delta)
        }
        if (moved) {
            fillStructure()
            structureList.select(row.id)
        }
    }

    private fun moveTab(row: StructureRow, delta: Int): Boolean {
        val ids = rows.filter { it.level == 0 }.map { it.id }.toMutableList()
        return move(ids, row.id, delta).also { if (it) c.tabOrder = ids }
    }

    /** Built-in groups are reordered among the built-in groups in the tab (RibbonCustomization.groupOrder). */
    private fun moveBuiltInGroup(row: StructureRow, delta: Int): Boolean {
        val parent = row.parentId ?: return false
        val ids = rows.filter { it.level == 1 && !it.isCustom && it.parentId == parent }.map { it.id }.toMutableList()
        return move(ids, row.id, delta).also { if (it) c.groupOrder[parent] = ids }
    }

    private fun moveCustomItem(row: StructureRow, delta: Int): Boolean {
        val parent = row.parentId ?: return false
        val group = findCustomGroup(parent) ?: return false
        return move(group.itemIds, row.id.substring(parent.length + 1), delta)
    }

    private fun moveCustomGroup(row: StructureRow, delta: Int): Boolean {
        val tab = c.customTabs.firstOrNull { t -> t.groups.any { it.id == row.id } }
        if (tab != null) {
            val ids = tab.groups.map { it.id }.toMutableList()
            if (!move(ids, row.id, delta)) return false
            tab.groups = ids.map { id -> tab.groups.first { it.id == id } }.toMutableList()
            return true
        }
        // Custom groups in a built-in tab are reordered among that tab's custom groups
        val siblings = c.customGroups.filter { it.tabId == row.parentId }
        val ids = siblings.map { it.id }.toMutableList()
        if (!move(ids, row.id, delta)) return false
        c.customGroups = (c.customGroups.filter { it.tabId != row.parentId } + ids.map { id -> siblings.first { it.id == id } }).toMutableList()
        return true
    }

    private fun moveQuickAccess(delta: Int) {
        val row = qatList.selected ?: return
        if (move(workingState.quickAccessItemIds!!, row.id, delta)) {
            fillQuickAccess()
            qatList.select(row.id)
        }
    }

    private fun importExport() {
        val box = XamlElement.load(
            "<TextBox AcceptsReturn=\"True\" TextWrapping=\"Wrap\" Height=\"360\" Width=\"560\" FontFamily=\"Consolas\" />",
        )
        box.textBoxText = RibbonStateSerializer.serialize(workingState)
        runNested(
            {
                WContentDialog(strings.importExport, box).also {
                    it.primaryButtonText = strings.ok
                    it.closeButtonText = strings.cancel
                }
            },
        ) {
            val imported = RibbonStateSerializer.deserialize(box.textBoxText)
            if (imported == null) {
                nested = {
                    val error = WContentDialog(
                        strings.importExport,
                        XamlElement.load("<TextBlock TextWrapping=\"Wrap\" />").also {
                            it.setText(invalidImportMessage ?: strings.invalidImport)
                        },
                    )
                    error.closeButtonText = strings.ok
                    error.show(ribbon) { show() }
                }
                return@runNested
            }
            workingState.customization = imported.customization
            workingState.quickAccessItemIds = imported.quickAccessItemIds ?: workingState.quickAccessItemIds
            workingState.quickAccessPosition = imported.quickAccessPosition
            qatBelow.toggleChecked = workingState.quickAccessPosition == RibbonQuickAccessPosition.BELOW_RIBBON
            fillStructure()
            fillQuickAccess()
        }
    }

    companion object {
        /** Overrides the message shown when the imported string is invalid (null means [RibbonStrings.invalidImport]). */
        @JvmStatic
        var invalidImportMessage: String? = null

        private const val MIN_WIDTH = 320.0
        private const val MAX_WIDTH = 860.0
        private const val DIALOG_MARGIN = 96.0
        private const val UNSELECTED_OPACITY = 0.7
        private const val SEMI_BOLD = 600
        private const val NORMAL = 400
        private const val ID_LENGTH = 8

        /** Moves [id] in [ids] by [delta]. Returns false if it did not move. */
        internal fun move(ids: MutableList<String>, id: String, delta: Int): Boolean {
            val index = ids.indexOf(id)
            if (index < 0) return false
            val target = (index + delta).coerceIn(0, ids.size - 1)
            if (target == index) return false
            ids.removeAt(index)
            ids.add(target, id)
            return true
        }
    }
}

/** One row of a [SelectableList]. If [isChecked] is null, no visibility check box is shown. */
internal data class ListRow(val id: String, val label: String, val detail: String?, val level: Int, val isChecked: Boolean?)

/** A list of selectable rows (the lists in the customize dialog). The Up and Down keys move the selection. */
internal class SelectableList {
    val root: XamlElement = XamlElement.load(
        "<Border Height=\"380\" BorderThickness=\"1\" CornerRadius=\"4\" BorderBrush=\"{ThemeResource RibbonInputBorderBrush}\">" +
            "<ScrollViewer VerticalScrollBarVisibility=\"Auto\"><StackPanel x:Name=\"PART_Rows\" /></ScrollViewer></Border>",
    )
    private val panel = root.part("PART_Rows")
    private var items: List<Pair<ListRow, XamlElement>> = emptyList()

    /** The selected row. */
    var selected: ListRow? = null
        private set

    /** Called when a visibility check changes (the row id and the new state). */
    var onToggle: ((String, Boolean) -> Unit)? = null

    init {
        root.onKeyDown { e ->
            val index = items.indexOfFirst { it.first.id == selected?.id }
            val next = when (e.key) {
                RibbonInputViews.VK_DOWN -> index + 1
                RibbonInputViews.VK_UP -> index - 1
                else -> return@onKeyDown
            }
            items.getOrNull(next.coerceIn(0, items.size - 1))?.let {
                select(it.first.id)
                it.second.focus()
            }
            e.handled = true
        }
    }

    /** Replaces the rows. */
    fun fill(rows: List<ListRow>) {
        panel.clearChildren()
        items = rows.map { row ->
            val indent = row.level * INDENT
            val detail = row.detail?.takeIf { it.isNotEmpty() }?.let {
                "<TextBlock Grid.Column=\"2\" Text=\"${Xaml.escape(it)}\" Opacity=\"0.6\" FontSize=\"11\" VerticalAlignment=\"Center\" Margin=\"8,0,0,0\" " +
                    "TextTrimming=\"CharacterEllipsis\" />"
            }.orEmpty()
            val button = XamlElement.load(
                "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Stretch\" " +
                    "Padding=\"${Xaml.num(INDENT_BASE + indent)},2,6,2\"><Grid><Grid.ColumnDefinitions><ColumnDefinition Width=\"Auto\" />" +
                    "<ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
                    (if (row.isChecked != null) "<CheckBox x:Name=\"PART_Check\" MinWidth=\"0\" Margin=\"0,0,4,0\" />" else "") +
                    "<TextBlock Grid.Column=\"1\" Text=\"${Xaml.escape(row.label)}\" VerticalAlignment=\"Center\" " +
                    (if (row.level == 0) "FontWeight=\"SemiBold\" " else "") + "TextTrimming=\"CharacterEllipsis\" />$detail</Grid></Button>",
            )
            button.setAutomationName(row.label)
            button.onClick { select(row.id) }
            if (row.isChecked != null) {
                val check = button.part("PART_Check")
                check.toggleChecked = row.isChecked
                check.setAutomationName(row.label)
                check.onToggled { checked -> onToggle?.invoke(row.id, checked == true) }
            }
            panel.addChild(button)
            row to button
        }
        selected = items.firstOrNull { it.first.id == selected?.id }?.first
        updateSelection()
    }

    /** Selects the row with [id]. */
    fun select(id: String) {
        selected = items.firstOrNull { it.first.id == id }?.first
        updateSelection()
    }

    private fun updateSelection() {
        for ((row, button) in items) setBackground(button, if (row.id == selected?.id) "RibbonAccentSubtleBrush" else null)
    }

    /** Sets the button's background to the brush [brushKey] (null means the default). */
    private fun setBackground(button: XamlElement, brushKey: String?) {
        val control = button.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IControl)
        val brush = brushKey?.let { WinUiUtilities.lookupApplicationResource(it) }
        try {
            control.call(com.appkitbox.winui4k.internal.winui.XamlInterop.IControl_put_Background, brush?.ptr)
        } finally {
            brush?.release()
        }
    }

    private companion object {
        const val INDENT = 18.0
        const val INDENT_BASE = 6.0
    }
}
