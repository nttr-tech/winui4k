package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WPopup
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.XamlKeyEvent
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommandDescriptor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSearchEngine
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSearchEntry
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSearchResult
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.EventObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/** A list of search results (shared by the search box and the command palette). Up / Down move the highlight, and Enter runs it. */
internal class RibbonSearchResultsList(private val onInvoked: (RibbonSearchEntry) -> Unit) {
    private val panel = XamlElement.load("<StackPanel Padding=\"4\" />")
    val root: XamlElement = XamlElement.load(
        "<ScrollViewer MaxHeight=\"420\" VerticalScrollBarVisibility=\"Auto\" HorizontalScrollBarVisibility=\"Disabled\" />",
    ).also { it.setContent(panel) }
    private var rows: List<Pair<XamlElement, RibbonSearchEntry>> = emptyList()
    private var highlight = -1

    /** The element that announces the highlighted result (the search box; notified when the highlight moves by keyboard). */
    var announcer: XamlElement? = null

    /** The highlighted (enabled) item. */
    val highlighted: RibbonSearchEntry? get() = rows.getOrNull(highlight)?.second?.takeIf { it.isEnabled }

    /** The number of rows. */
    val count: Int get() = rows.size

    /** Shows the results (a "Recently used" header if the query is empty). */
    fun show(results: List<RibbonSearchResult>, query: String?) {
        panel.clearChildren()
        val strings = RibbonStrings.current
        val empty = query.isNullOrBlank()
        if (results.isEmpty()) {
            rows = emptyList()
            highlight = -1
            panel.addChild(
                XamlElement.load(
                    "<TextBlock Text=\"${Xaml.escape(if (empty) strings.searchRecent else strings.searchNoResults)}\" Margin=\"8,6,8,6\" Opacity=\"0.7\" />",
                ),
            )
            return
        }
        panel.addChild(
            XamlElement.load(
                "<TextBlock Text=\"${Xaml.escape(if (empty) strings.searchRecent else strings.searchActions)}\" FontSize=\"12\" FontWeight=\"SemiBold\" " +
                    "Margin=\"8,4,8,4\" Opacity=\"0.8\" />",
            ),
        )
        rows = results.map { createRow(it.entry) }
        rows.forEach { panel.addChild(it.first) }
        setHighlight(rows.indexOfFirst { it.second.isEnabled })
    }

    private fun createRow(entry: RibbonSearchEntry): Pair<XamlElement, RibbonSearchEntry> {
        val icon = when (val target = entry.target) {
            is RibbonNodeModel -> target.icon
            is RibbonCommandDescriptor -> target.icon
            else -> null
        }
        val iconXaml = RibbonIconXaml.build(icon, ICON_SIZE).orEmpty()
        val path = entry.path?.takeIf { it.isNotEmpty() }?.let {
            "<TextBlock Text=\"${Xaml.escape(it)}\" FontSize=\"11\" Opacity=\"0.65\" TextTrimming=\"CharacterEllipsis\" />"
        }.orEmpty()
        val shortcut = entry.shortcut?.takeIf { it.isNotEmpty() }?.let {
            "<TextBlock Grid.Column=\"2\" Text=\"${Xaml.escape(it)}\" FontSize=\"11\" Opacity=\"0.65\" VerticalAlignment=\"Center\" />"
        }.orEmpty()
        val button = XamlElement.load(
            "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Stretch\">" +
                "<Grid ColumnSpacing=\"10\"><Grid.ColumnDefinitions><ColumnDefinition Width=\"20\" /><ColumnDefinition Width=\"*\" />" +
                "<ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions><Border VerticalAlignment=\"Center\">$iconXaml</Border>" +
                "<StackPanel Grid.Column=\"1\"><TextBlock Text=\"${Xaml.escape(entry.label)}\" TextTrimming=\"CharacterEllipsis\" />$path</StackPanel>" +
                "$shortcut</Grid></Button>",
        )
        button.isControlEnabled = entry.isEnabled
        button.setAutomationName(entry.label)
        entry.path?.let { button.setAutomationHelpText(it) }
        button.onClick { if (entry.isEnabled) onInvoked(entry) }
        return button to entry
    }

    /** Moves the highlight by [delta] (+1 / -1), skipping disabled items. */
    fun move(delta: Int) {
        if (rows.isEmpty() || delta == 0) return
        val step = if (delta > 0) 1 else -1
        var index = highlight + step
        while (index in rows.indices) {
            if (rows[index].second.isEnabled) {
                setHighlight(index)
                announce(rows[index].second)
                return
            }
            index += step
        }
    }

    private fun announce(entry: RibbonSearchEntry) {
        val target = announcer ?: return
        val text = if (entry.path.isNullOrEmpty()) entry.label else "${entry.label}, ${entry.path}"
        Xaml.announce(target.inspectable, text, "RibbonSearchHighlight")
    }

    private fun setHighlight(index: Int) {
        highlight = if (rows.isEmpty()) -1 else index
        val brush = WinUiUtilities.lookupApplicationResource("RibbonItemHoverBrush")
        try {
            rows.forEachIndexed { i, (button, _) ->
                button.view(XamlInterop.IID_IControl).call(XamlInterop.IControl_put_Background, if (i == highlight) brush.ptr else null)
            }
        } finally {
            brush.release()
        }
    }

    private companion object {
        const val ICON_SIZE = 16.0
    }
}

/** Just before a query is committed (a chance for the app to add results such as help, documents or people). */
class RibbonSearchQueryEvent(
    source: WRibbonSearchBox,
    /** The query. */
    val query: String,
) : EventObject(source) {
    /** Items added to the results only for this query. */
    val additionalEntries: MutableList<RibbonSearchEntry> = mutableListOf()

    /** The search box the event originated from. */
    override fun getSource(): WRibbonSearchBox = super.getSource() as WRibbonSearchBox
}

/**
 * A Microsoft Search-style command search box ("Search (Alt+Q)"; RibbonSpace's RibbonSearchBox). Place it in a title bar
 * or the tab row; it searches all tabs, contextual tabs, Backstage and the catalog of the ribbon ([ribbon]) and runs the
 * chosen command. When the box is hidden (in a narrow title bar), Alt+Q opens the command palette.
 */
class WRibbonSearchBox(
    /** The ribbon to search and run commands in. */
    val ribbon: WRibbon,
) : WComponent(RibbonThemeResources.load("<Grid MinWidth=\"160\" />")) {
    private val root = XamlElement(inspectable.also { it.addRef() })
    private val textBox = XamlElement.load(
        "<TextBox Height=\"30\" MinHeight=\"0\" VerticalAlignment=\"Center\" Padding=\"30,5,8,4\" CornerRadius=\"4\" " +
            "Background=\"{ThemeResource RibbonSearchBackgroundBrush}\" BorderBrush=\"{ThemeResource RibbonSearchBorderBrush}\" />",
    )
    private val results = RibbonSearchResultsList { execute(it) }
    private val popup: WPopup
    private val queryEntries = mutableListOf<RibbonSearchEntry>()
    private val dismissLayer = RibbonDismissLayer { close() }
    private var lastText = ""
    private var ignoredText: String? = null

    /** Whether the box has been pressed with the pointer but has not yet received focus (to identify focus gained by a click). */
    private var isPointerPressed = false
    private val queryListeners = CopyOnWriteArrayList<Consumer<RibbonSearchQueryEvent>>()
    private val executedListeners = CopyOnWriteArrayList<Consumer<RibbonSearchEntry>>()

    /** The maximum number of results. */
    var maxResults: Int = DEFAULT_MAX_RESULTS

    /** The text shown when empty ("Search (Alt+Q)" if null). */
    var placeholderText: String? = null
        set(value) {
            field = value
            textBox.setPlaceholderText(value ?: RibbonStrings.current.searchPlaceholder)
        }

    init {
        RibbonThemeResources.ensure()
        root.addChild(textBox)
        root.addChild(
            XamlElement.load(
                "<FontIcon Glyph=\"&#xE721;\" FontSize=\"13\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Center\" Margin=\"10,0,0,0\" " +
                    "IsHitTestVisible=\"False\" Opacity=\"0.8\" />",
            ),
        )
        textBox.setPlaceholderText(RibbonStrings.current.searchPlaceholder)
        textBox.setAutomationName(RibbonStrings.current.search)
        val chrome = XamlElement.load(
            "<Border BorderThickness=\"1\" CornerRadius=\"8\" Background=\"{ThemeResource RibbonPopupBackgroundBrush}\" " +
                "BorderBrush=\"{ThemeResource RibbonPopupBorderBrush}\" />",
        )
        chrome.setChild(results.root)
        popup = WPopup(chrome)
        popup.isLightDismissEnabled = false
        popup.addCloseListener {
            removeQueryEntries()
            dismissLayer.hide()
        }
        results.announcer = textBox
        textBox.onTextChanged { onTextChanged() }
        // Do not show results on the automatic focus when the window opens (only when the box was pressed or entered
        // with the Tab key). FocusState can be Pointer even for automatic focus, so FocusState is not used to tell them apart
        textBox.onPointerHandledToo(XamlInterop.IUIElementStatics_get_PointerPressedEvent) {
            if (textBox.hasFocus) {
                if (!isResultsOpen) showResults()
            } else {
                isPointerPressed = true
            }
        }
        textBox.onFocus(true) {
            val byPointer = isPointerPressed
            isPointerPressed = false
            if (byPointer || Xaml.isKeyDown(VK_TAB)) showResults()
        }
        textBox.onFocus(false) { isPointerPressed = false }
        textBox.onPreviewKeyDown { e -> onKeyDown(e) }
        ribbon.addSearchRequestListener { onSearchRequested() }
    }

    /** Whether results are shown. */
    val isResultsOpen: Boolean get() = popup.isOpen

    /** Whether the layer that receives clicks outside the results is open. */
    internal val isDismissLayerOpen: Boolean get() = dismissLayer.isOpen

    /** The query. */
    var text: String
        get() = textBox.textBoxText
        set(value) {
            textBox.textBoxText = value
        }

    private fun onSearchRequested() {
        if (!isVisible || actualWidth <= 0) {
            // When hidden in a narrow window, use the command palette instead
            WRibbonCommandPalette.show(ribbon)
            return
        }
        focusSearch()
    }

    /** Puts the focus on the box and shows recently used commands. */
    fun focusSearch() {
        textBox.focus()
        textBox.selectAllText()
        showResults()
    }

    private fun onTextChanged() {
        val current = textBox.textBoxText
        // Do not show results on a TextChanged where the text has not changed (e.g. right after creation)
        if (current == lastText) return
        lastText = current
        if (ignoredText != null && current == ignoredText) {
            ignoredText = null
            return
        }
        ignoredText = null
        if (textBox.hasFocus || isResultsOpen) showResults()
    }

    private fun onKeyDown(e: XamlKeyEvent) {
        when (e.key) {
            RibbonInputViews.VK_DOWN -> {
                if (isResultsOpen) results.move(1) else showResults()
                e.handled = true
            }
            RibbonInputViews.VK_UP -> if (isResultsOpen) {
                results.move(-1)
                e.handled = true
            }
            RibbonInputViews.VK_ENTER -> if (isResultsOpen) {
                results.highlighted?.let { execute(it) }
                e.handled = true
            }
            RibbonInputViews.VK_ESCAPE -> {
                close()
                ribbon.tabStrip.headerOf(ribbon.selectedTab)?.element?.focus()
                e.handled = true
            }
        }
    }

    private fun showResults() {
        val query = textBox.textBoxText
        val event = RibbonSearchQueryEvent(this, query)
        queryListeners.forEach { it.accept(event) }
        removeQueryEntries()
        for (extra in event.additionalEntries) {
            if (ribbon.additionalSearchEntries.none { it.id == extra.id }) {
                ribbon.additionalSearchEntries += extra
                queryEntries += extra
            }
        }
        results.show(ribbon.search(query, maxResults), query)
        (popup.child as? XamlElement)?.let {
            it.requestedTheme = ribbon.actualTheme
            it.setSize(maxOf(actualWidth, MIN_POPUP_WIDTH), Double.NaN)
        }
        val position = root.positionInRoot()
        popup.horizontalOffset = position[0]
        popup.verticalOffset = position[1] + actualHeight + 2
        if (!popup.isOpen) {
            dismissLayer.show(this)
            popup.show(this)
        }
    }

    private fun removeQueryEntries() {
        ribbon.additionalSearchEntries.removeAll(queryEntries.toSet())
        queryEntries.clear()
    }

    private fun execute(entry: RibbonSearchEntry) {
        if (!entry.isEnabled) return
        close()
        if (textBox.textBoxText.isNotEmpty()) {
            ignoredText = ""
            textBox.textBoxText = ""
        }
        ribbon.executeSearchEntry(entry)
        executedListeners.forEach { it.accept(entry) }
    }

    /** Closes the results. */
    fun close() {
        dismissLayer.hide()
        if (popup.isOpen) popup.hide()
    }

    /** Subscribes to the moment just before a query is committed (to add results). */
    fun addQueryListener(listener: Consumer<RibbonSearchQueryEvent>) {
        queryListeners += listener
    }

    /** Unsubscribes a listener added with [addQueryListener]. */
    fun removeQueryListener(listener: Consumer<RibbonSearchQueryEvent>) {
        queryListeners -= listener
    }

    /** Subscribes to results being run. */
    fun addResultExecutedListener(listener: Consumer<RibbonSearchEntry>) {
        executedListeners += listener
    }

    /** Unsubscribes a listener added with [addResultExecutedListener]. */
    fun removeResultExecutedListener(listener: Consumer<RibbonSearchEntry>) {
        executedListeners -= listener
    }

    private companion object {
        const val DEFAULT_MAX_RESULTS = 12
        const val MIN_POPUP_WIDTH = 360.0
        const val VK_TAB = 9
    }
}

/**
 * A command palette (like VS Code's Ctrl+Shift+P or Figma's Ctrl+K; RibbonSpace's RibbonCommandPalette): a fuzzy search
 * shown at the top center of the screen. When it closes, the focus returns to the original element.
 */
object WRibbonCommandPalette {
    /** Shows a palette that searches the commands of [ribbon]. */
    @JvmStatic
    fun show(ribbon: WRibbon) = show(ribbon, { ribbon.search(it, MAX_RESULTS) }, { ribbon.executeSearchEntry(it) })

    /** Shows a palette searching arbitrary [entries] in the window of [owner]. The chosen entry is run with [execute]. */
    @JvmStatic
    fun show(owner: WComponent, entries: List<RibbonSearchEntry>, execute: Consumer<RibbonSearchEntry>) {
        val engine = RibbonSearchEngine()
        engine.setEntries(entries)
        show(owner, { engine.search(it, MAX_RESULTS) }) {
            engine.markUsed(it.id)
            execute.accept(it)
        }
    }

    private fun show(owner: WComponent, search: (String) -> List<RibbonSearchResult>, execute: (RibbonSearchEntry) -> Unit) {
        RibbonThemeResources.ensure()
        val size = Xaml.rootSize(owner) ?: return
        val box = XamlElement.load("<TextBox Margin=\"8,8,8,4\" />")
        box.setPlaceholderText(RibbonStrings.current.commandPalette)
        box.setAutomationName(RibbonStrings.current.commandPalette)
        lateinit var popup: WPopup
        val dismissLayer = RibbonDismissLayer { popup.hide() }
        var executing = false
        val run: (RibbonSearchEntry) -> Unit = { entry ->
            if (entry.isEnabled) {
                executing = true
                popup.hide()
                execute(entry)
            }
        }
        val results = RibbonSearchResultsList(run)
        results.announcer = box
        val width = maxOf(0.0, minOf(MAX_WIDTH, size[0] - MARGIN))
        popup = WPopup(chrome(width, box, results).also { it.requestedTheme = owner.actualTheme })
        popup.isLightDismissEnabled = false
        popup.horizontalOffset = maxOf(0.0, (size[0] - width) / 2)
        popup.verticalOffset = TOP
        val previousFocus = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)?.let { root ->
            try {
                Xaml.focusedElement(root)
            } finally {
                root.release()
            }
        }
        popup.addCloseListener {
            dismissLayer.hide()
            if (!executing) previousFocus?.queryInterfaceOrNull(XamlInterop.IID_IUIElement)?.let { XamlElement(it).focus(XamlInterop.FocusState_Programmatic) }
            previousFocus?.release()
        }
        box.onTextChanged { results.show(search(box.textBoxText), box.textBoxText) }
        box.onPreviewKeyDown { e ->
            when (e.key) {
                RibbonInputViews.VK_DOWN -> results.move(1)
                RibbonInputViews.VK_UP -> results.move(-1)
                RibbonInputViews.VK_ENTER -> results.highlighted?.let(run)
                RibbonInputViews.VK_ESCAPE -> popup.hide()
                else -> return@onPreviewKeyDown
            }
            e.handled = true
        }
        results.show(search(""), "")
        dismissLayer.show(owner)
        popup.show(owner)
        WinUiUtilities.invokeLater { box.focus() }
    }

    /** The palette's frame, stacking the search box and the results vertically. */
    private fun chrome(width: Double, box: XamlElement, results: RibbonSearchResultsList): XamlElement {
        val panel = XamlElement.load("<StackPanel />")
        panel.addChild(box)
        panel.addChild(results.root)
        val chrome = XamlElement.load(
            "<Border Width=\"${Xaml.num(width)}\" BorderThickness=\"1\" CornerRadius=\"8\" TabFocusNavigation=\"Cycle\" " +
                "Background=\"{ThemeResource RibbonPopupBackgroundBrush}\" BorderBrush=\"{ThemeResource RibbonPopupBorderBrush}\" />",
        )
        chrome.setChild(panel)
        return chrome
    }

    private const val MAX_RESULTS = 16
    private const val MAX_WIDTH = 600.0
    private const val MARGIN = 40.0
    private const val TOP = 72.0
}
