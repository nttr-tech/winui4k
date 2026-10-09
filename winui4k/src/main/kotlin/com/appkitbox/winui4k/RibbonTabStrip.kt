package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonKeyTipAssigner
import com.appkitbox.winui4k.ribbon.RibbonTabModel

/**
 * A header in the tab row (RibbonTabHeader in RibbonSpace). Contextual tabs are distinguished by the group's color bar and
 * text color. The label width is fixed at its bold (selected) width so that selection does not shift the row's layout.
 */
internal class RibbonTabHeaderView(val tab: RibbonTabModel, private val strip: RibbonTabStrip) : RibbonKeyTipTarget {
    /** The header button (rebuilt when the contextual color changes). */
    var element: XamlElement = build(null)
        private set
    private var contextual: RibbonContextualGroupModel? = null
    private var builtColor: RibbonColor? = null

    /** The width of the header. */
    var headerWidth: Double = 0.0
        private set

    /** Creates the header with the color ([color] is the contextual tab's color) embedded in the XAML. */
    private fun build(color: RibbonColor?): XamlElement {
        val colors = color?.let { " BorderBrush=\"${it.toHex(includeAlpha = true)}\" Foreground=\"${it.toHex(includeAlpha = true)}\"" }.orEmpty()
        val button = XamlElement.load("<Button Style=\"{StaticResource RibbonTabHeaderStyle}\"$colors />")
        button.onClick { strip.headerClicked(this) }
        button.onDoubleTapped { e ->
            if (strip.headerDoubleTapped()) e.markHandled()
        }
        button.onRightTapped { e ->
            val p = e.position(button)
            if (strip.headerRightTapped(this, p[0], p[1])) e.markHandled()
        }
        button.onKeyDown { e -> if (strip.headerKeyDown(this, e.key)) e.handled = true }
        return button
    }

    /** Applies the label, the contextual color, and the enabled state. */
    fun update(group: RibbonContextualGroupModel?, isDark: Boolean) {
        contextual = group
        val color = group?.color?.let { if (isDark) it.lighten(DARK_LIGHTEN) else it }
        if (color != builtColor) {
            builtColor = color
            element = build(color)
        }
        val label = strip.labelOf(tab)
        val icon = RibbonIconXaml.build(tab.icon, ICON_SIZE, "{Binding Foreground, RelativeSource={RelativeSource TemplatedParent}}")
        if (icon == null) {
            element.setContentText(label)
        } else {
            // The icon goes before the header text (PART_Icon in RibbonSpace)
            element.setContent(
                XamlElement.load(
                    "<StackPanel Orientation=\"Horizontal\" Spacing=\"${Xaml.num(ICON_SPACING)}\">$icon" +
                        "<TextBlock Text=\"${Xaml.escape(label)}\" VerticalAlignment=\"Center\" /></StackPanel>",
                ),
            )
        }
        // Header widths are measured with the density's font size, so display with the same font size (with the style's
        // default of 12, the text is truncated in compact density)
        element.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IControl).call(com.appkitbox.winui4k.internal.winui.XamlInterop.IControl_put_FontSize, strip.metrics.fontSize)
        element.setAutomationName(label)
        element.setAutomationId(tab.automationId ?: "RibbonTab_${tab.id}")
        element.setAutomationAccessKey(explicitKeyTip)
        // The tooltip is group name › header for a contextual tab, and the ScreenTip (description) for a regular tab
        if (group?.label != null) {
            element.setToolTipValue("${group.label} › $label")
        } else {
            element.setToolTipValue(RibbonScreenTips.create(label, tab.screenTip, tab.description, null, tab.isEnabled)?.takeIf { tab.screenTip != null || tab.description != null })
        }
        element.setAutomationHelpText(tab.screenTip?.description ?: tab.description)
        element.isControlEnabled = tab.isEnabled && (group?.isEnabled ?: true)
        val widths = strip.textWidths.widths(listOf(label), strip.metrics.fontSize, true)
        headerWidth = widths[0] + PADDING * 2 + MARGIN + if (icon != null) ICON_SIZE + ICON_SPACING else 0.0
        element.applyTemplate()
        element.goToState(if (group != null) "Contextual" else "Regular")
    }

    /** Applies the selection state (only the selected header is a Tab stop). */
    fun setSelected(selected: Boolean) {
        element.goToState(
            when {
                !selected -> "Unselected"
                contextual != null -> "SelectedContextual"
                else -> "Selected"
            },
        )
        element.isTabStop = selected
    }

    override val keyTipLabel: String? get() = strip.labelOf(tab)
    override val explicitKeyTip: String? get() = tab.keyTip ?: contextualKeyTip()
    override val keyTipAnchor: XamlElement get() = element
    override val keyTipModel: RibbonTabModel get() = tab

    /** A contextual tab's KeyTip is the group's KeyTip (e.g. "J") + the first letter of the header (e.g. "T") = "JT". */
    private fun contextualKeyTip(): String? {
        val prefix = contextual?.keyTip?.takeIf { it.isNotEmpty() } ?: return null
        val letters = RibbonKeyTipAssigner.normalize(strip.labelOf(tab))
        return if (letters.isNotEmpty()) prefix + letters[0] else null
    }

    override fun onKeyTip(): RibbonKeyTipResult = strip.headerKeyTip(this)

    private companion object {
        const val PADDING = 10.0
        const val MARGIN = 2.0
        const val ICON_SIZE = 16.0
        const val ICON_SPACING = 6.0
        const val DARK_LIGHTEN = 0.35
    }
}

/** The contract of what has a tab row (the ribbon). */
internal interface RibbonTabStripOwner {
    val metrics: com.appkitbox.winui4k.ribbon.RibbonMetrics
    val textWidths: RibbonTextWidths

    /** The display name of a tab (the new name if it was renamed by customization). */
    fun tabLabel(tab: RibbonTabModel): String

    /** A header was clicked. */
    fun onHeaderClicked(tab: RibbonTabModel)

    /** A header was double-clicked (toggles ribbon minimization). Returns true if handled. */
    fun onHeaderDoubleTapped(): Boolean

    /** A header was right-clicked. */
    fun onHeaderRightTapped(tab: RibbonTabModel, element: XamlElement, x: Double, y: Double): Boolean

    /** Moves to the selected tab's commands with the Down key. */
    fun focusCommands(): Boolean

    /** A header's KeyTip (selects the tab and proceeds to the level of that tab's commands). */
    fun onHeaderKeyTip(tab: RibbonTabModel): RibbonKeyTipResult

    /** Selects a tab with the keyboard. */
    fun selectTab(tab: RibbonTabModel)
}

/**
 * The tab row (the tab strip of Ribbon in RibbonSpace). Headers are laid out horizontally and scroll with the left and
 * right arrows when they overflow. The Left, Right, Home, and End keys move between headers (a roving tab stop), and the
 * Down key moves to the selected tab's commands.
 */
internal class RibbonTabStrip(private val owner: RibbonTabStripOwner) {
    private val scrollHost = RibbonScrollHost()
    private val canvas = XamlElement.load("<Canvas HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />")
    private val cache = HashMap<RibbonTabModel, RibbonTabHeaderView>()

    /** The headers shown (in display order). */
    var headers: List<RibbonTabHeaderView> = emptyList()
        private set

    /** The element to place. */
    val element: XamlElement get() = scrollHost.element

    val metrics get() = owner.metrics
    val textWidths get() = owner.textWidths

    /** The display name of a tab. */
    fun labelOf(tab: RibbonTabModel): String = owner.tabLabel(tab)

    init {
        scrollHost.setContent(canvas)
    }

    /** Rebuilds the headers for the tabs to show, [tabs] (with their contextual groups). */
    fun update(tabs: List<Pair<RibbonTabModel, RibbonContextualGroupModel?>>, selected: RibbonTabModel?, isDark: Boolean) {
        canvas.clearChildren()
        cache.keys.retainAll(tabs.map { it.first }.toSet())
        headers = tabs.map { (tab, group) ->
            val header = cache.getOrPut(tab) { RibbonTabHeaderView(tab, this) }
            header.update(group, isDark)
            canvas.addChild(header.element)
            header
        }
        setSelected(selected)
    }

    /** Applies the selected header. */
    fun setSelected(selected: RibbonTabModel?) {
        for ((index, header) in headers.withIndex()) {
            header.setSelected(header.tab === selected)
            if (selected == null && index == 0) header.element.isTabStop = true
        }
        headerOf(selected)?.let { bringIntoView(it) }
    }

    /** Lays out the headers ([availableWidth] is the available width; [height] is the height of the tab row). */
    fun layout(availableWidth: Double, height: Double) {
        var x = 0.0
        for (header in headers) {
            header.element.setCanvasPosition(x, 0.0)
            header.element.setSize(header.headerWidth, height)
            x += header.headerWidth
        }
        canvas.setSize(x, height)
        scrollHost.update(x, availableWidth, height)
    }

    /** The total width of the headers. */
    val extent: Double get() = headers.sumOf { it.headerWidth }

    /** The header of [tab]. */
    fun headerOf(tab: RibbonTabModel?): RibbonTabHeaderView? = headers.firstOrNull { it.tab === tab }

    private fun bringIntoView(header: RibbonTabHeaderView) {
        var x = 0.0
        for (h in headers) {
            if (h === header) break
            x += h.headerWidth
        }
        scrollHost.bringIntoView(x, header.headerWidth)
    }

    fun headerClicked(header: RibbonTabHeaderView) = owner.onHeaderClicked(header.tab)

    fun headerDoubleTapped(): Boolean = owner.onHeaderDoubleTapped()

    fun headerRightTapped(header: RibbonTabHeaderView, x: Double, y: Double): Boolean = owner.onHeaderRightTapped(header.tab, header.element, x, y)

    fun headerKeyTip(header: RibbonTabHeaderView): RibbonKeyTipResult = owner.onHeaderKeyTip(header.tab)

    /** Key handling on a header (Left, Right, Home, End move; Down goes to the commands). Returns true if handled. */
    fun headerKeyDown(header: RibbonTabHeaderView, key: Int): Boolean {
        val index = headers.indexOf(header)
        if (index < 0 || headers.isEmpty()) return false
        // In a right-to-left layout, the visual left is the next tab
        val step = if (header.element.flowDirection == FlowDirection.RIGHT_TO_LEFT) -1 else 1
        val target = when (key) {
            RibbonInputViews.VK_LEFT -> index - step
            RibbonInputViews.VK_RIGHT -> index + step
            RibbonInputViews.VK_HOME -> 0
            RibbonInputViews.VK_END -> headers.size - 1
            RibbonInputViews.VK_DOWN -> return owner.focusCommands()
            else -> return false
        }
        val next = headers[(target + headers.size) % headers.size]
        owner.selectTab(next.tab)
        next.element.focus()
        return true
    }
}
