package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonColorPalette
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonColorSwatch
import com.appkitbox.winui4k.ribbon.RibbonGridPickerModel
import com.appkitbox.winui4k.ribbon.RibbonGridSize
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonSegmentModel
import com.appkitbox.winui4k.ribbon.RibbonSegmentedModel
import com.appkitbox.winui4k.ribbon.RibbonStrings
import kotlin.math.floor

/**
 * A switch chosen by segments (RibbonSegmentedControl in RibbonSpace). The segments are toggle buttons, and choosing one
 * executes with that segment's value as the argument. The Left, Right, Home, and End keys move the selection, and the
 * primary action (KeyTips, command search) selects the next segment.
 */
internal class RibbonSegmentedView(override val model: RibbonSegmentedModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load(
        "<StackPanel Orientation=\"Horizontal\" Spacing=\"1\" IsTabStop=\"True\" UseSystemFocusVisuals=\"True\" Background=\"Transparent\" />",
    )
    private var buttons: List<Pair<RibbonSegmentModel, XamlElement>> = emptyList()
    private val listListener = RibbonListListener<RibbonSegmentModel> {
        build()
        host.invalidateItemsLayout()
    }

    override fun attach() {
        element.onKeyDown { e -> onKeyDown(e) }
        model.segments.addListListener(listListener)
        super.attach()
        build()
    }

    override fun dispose() {
        super.dispose()
        model.segments.removeListListener(listListener)
    }

    private fun segmentSpec(segment: RibbonSegmentModel, layout: RibbonItemLayout) = RibbonContentSpec(
        label = segment.label,
        icon = segment.icon,
        largeIcon = null,
        size = RibbonItemSize.MEDIUM,
        metrics = layout.metrics,
        showLabel = segment.label != null,
        isSimplified = layout.isSimplified,
    )

    override fun measure(layout: RibbonItemLayout): WSize {
        val parts = model.segments.map { RibbonItemContent.layout(segmentSpec(it, layout), host.textWidths) }
        if (parts.isEmpty()) return WSize(0.0, 0.0)
        return WSize(parts.sumOf { it.width + 2 } + (parts.size - 1), parts.maxOf { it.height + 2 })
    }

    private fun build() {
        element.clearChildren()
        buttons = model.segments.map { segment ->
            val button = XamlElement.load("<ToggleButton Style=\"{StaticResource RibbonItemToggleButtonStyle}\" IsTabStop=\"False\" />")
            button.onClick { select(segment) }
            button.setAutomationName(segment.label)
            element.addChild(button)
            segment to button
        }
        applyLayoutCore()
    }

    override fun applyLayoutCore() {
        for ((segment, button) in buttons) {
            val content = RibbonItemContent.layout(segmentSpec(segment, layout), host.textWidths)
            button.setContent(XamlElement.load(content.xaml))
            button.setSize(content.width + 2, content.height + 2)
        }
        syncChecked()
    }

    private fun syncChecked() {
        for ((segment, button) in buttons) {
            val checked = segment === model.selectedSegment
            if (button.toggleChecked != checked) button.toggleChecked = checked
        }
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "selectedSegment") syncChecked()
    }

    /** Selects a segment and executes with its value as the argument. */
    fun select(segment: RibbonSegmentModel) {
        model.selectedSegment = segment
        syncChecked()
        execute(segment.value ?: segment.label)
    }

    private fun onKeyDown(e: XamlKeyEvent) {
        val segments = model.segments.toList()
        if (segments.isEmpty()) return
        val current = segments.indexOf(model.selectedSegment).coerceAtLeast(0)
        val target = when (e.key) {
            RibbonInputViews.VK_LEFT -> maxOf(0, current - 1)
            RibbonInputViews.VK_RIGHT -> minOf(segments.size - 1, current + 1)
            RibbonInputViews.VK_HOME -> 0
            RibbonInputViews.VK_END -> segments.size - 1
            RibbonInputViews.VK_ENTER, RibbonInputViews.VK_SPACE -> current
            else -> return
        }
        select(segments[target])
        e.handled = true
    }

    override fun invoke(): Boolean {
        val segments = model.segments.toList()
        if (segments.isEmpty() || !isEffectivelyEnabled) return false
        select(segments[(segments.indexOf(model.selectedSegment).coerceAtLeast(0) + 1) % segments.size])
        return true
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> {
        val groupName = "RibbonSegmented" + System.identityHashCode(model)
        return model.segments.map { segment ->
            WRadioMenuFlyoutItem(segment.label.orEmpty(), groupName).also { item ->
                item.isChecked = segment === model.selectedSegment
                item.addActionListener { select(segment) }
            }
        }
    }
}

/**
 * A grid for picking a size with the pointer (RibbonGridPicker in RibbonSpace). Inside a drop-down ([embedded]) it is the
 * grid itself; in the ribbon, a drop-down button opens the grid. The arrow keys change the size, and Enter / Space commits it.
 */
internal class RibbonGridPickerView(override val model: RibbonGridPickerModel, host: RibbonItemHost, private val embedded: Boolean) :
    RibbonItemView(model, host) {
    override val element: XamlElement = if (embedded) {
        XamlElement.load("<ContentControl IsTabStop=\"True\" UseSystemFocusVisuals=\"True\" HorizontalContentAlignment=\"Left\" />")
    } else {
        XamlElement.load("<Button Style=\"{StaticResource RibbonItemButtonStyle}\" />")
    }
    private val flyout: WFlyout? = if (embedded) null else WFlyout()
    private var picker: GridPanel? = null

    init {
        if (flyout != null) {
            flyout.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
            RibbonMenus.applyFlyoutStyle(flyout, bare = false)
            RibbonMenus.onOpening(flyout) { flyout.content = GridPanel(null).root }
            RibbonMenus.onOpened(flyout) { element.goToState("Active") }
            RibbonMenus.onClosed(flyout) { element.goToState("Inactive") }
            element.onClick { if (isEffectivelyEnabled) flyout.showAt(element) }
        }
    }

    override fun attach() {
        if (embedded) {
            val panel = GridPanel(element)
            picker = panel
            element.setContent(panel.root)
            element.onKeyDown { e -> panel.onKeyDown(e) }
            element.onFocus(true) { if (panel.highlighted.rows == 0) panel.highlight(RibbonGridSize(1, 1)) }
        }
        super.attach()
    }

    private fun spec(layout: RibbonItemLayout) = RibbonContentSpec(
        label = model.label,
        icon = model.icon,
        largeIcon = model.largeIcon,
        size = layout.size,
        metrics = layout.metrics,
        showLabel = showsLabel(layout),
        showChevron = true,
        isSimplified = layout.isSimplified,
        iconForeground = if (isEffectivelyEnabled) RibbonIconXaml.ICON_BRUSH else DISABLED_ICON_BRUSH,
    )

    override fun measure(layout: RibbonItemLayout): WSize {
        if (embedded) return WSize(PADDING * 2 + model.columns * CELL_PITCH, PADDING * 2 + CAPTION_HEIGHT + model.rows * CELL_PITCH)
        val content = RibbonItemContent.layout(spec(layout), host.textWidths)
        return WSize(content.width + 2, content.height + 2)
    }

    override fun applyLayoutCore() {
        if (embedded) {
            picker?.rebuild()
            return
        }
        val content = RibbonItemContent.layout(spec(layout), host.textWidths)
        element.setContent(XamlElement.load(content.xaml))
        element.setSize(content.width + 2, content.height + 2)
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "rows" || name == "columns") {
            host.invalidateItemsLayout()
            applyLayoutCore()
        }
    }

    /** Commits the size (runs the command with a [RibbonGridSize] and closes the drop-down). */
    fun pick(size: RibbonGridSize) {
        flyout?.hide()
        execute(size)
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        if (flyout != null) flyout.showAt(element) else element.focus()
        return true
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        invoke()
        return RibbonKeyTipResult.Close
    }

    /** The heading and the grid (each cell has two layers, a frame and a highlight, and the highlight's opacity is toggled). */
    private inner class GridPanel(private val focusTarget: XamlElement?) {
        val root: XamlElement = XamlElement.load("<StackPanel Padding=\"${Xaml.num(PADDING)}\" />")
        private val caption = XamlElement.load("<TextBlock Margin=\"2,0,2,6\" />")
        private val grid = XamlElement.load("<Canvas Background=\"Transparent\" />")
        private var cells: List<Pair<RibbonGridSize, XamlElement>> = emptyList()
        var highlighted = RibbonGridSize(0, 0)
            private set
        private var captionSet = false

        init {
            root.addChild(caption)
            root.addChild(grid)
            grid.onPointer(XamlInterop.IUIElement_add_PointerMoved) { e -> highlight(cellAt(e.position(grid))) }
            grid.onPointer(XamlInterop.IUIElement_add_PointerExited) { highlight(RibbonGridSize(0, 0)) }
            grid.onTapped { e ->
                val size = cellAt(e.position(grid))
                if (size.rows > 0) pick(size)
            }
            rebuild()
        }

        private fun cellAt(position: DoubleArray): RibbonGridSize {
            val column = floor(position[0] / CELL_PITCH).toInt() + 1
            val row = floor(position[1] / CELL_PITCH).toInt() + 1
            if (column !in 1..model.columns || row !in 1..model.rows) return RibbonGridSize(0, 0)
            return RibbonGridSize(row, column)
        }

        fun rebuild() {
            grid.clearChildren()
            val list = mutableListOf<Pair<RibbonGridSize, XamlElement>>()
            for (r in 1..model.rows) {
                for (c in 1..model.columns) {
                    val cell = XamlElement.load(
                        "<Grid Width=\"${Xaml.num(CELL_SIZE)}\" Height=\"${Xaml.num(CELL_SIZE)}\" IsHitTestVisible=\"False\">" +
                            "<Border BorderThickness=\"1\" CornerRadius=\"1\" BorderBrush=\"{ThemeResource RibbonInputBorderBrush}\" />" +
                            "<Border Opacity=\"0\" BorderThickness=\"1\" CornerRadius=\"1\" Background=\"{ThemeResource RibbonAccentSubtleStrongBrush}\" " +
                            "BorderBrush=\"{ThemeResource RibbonAccentBrush}\" /></Grid>",
                    )
                    cell.setCanvasPosition((c - 1) * CELL_PITCH, (r - 1) * CELL_PITCH)
                    grid.addChild(cell)
                    list += RibbonGridSize(r, c) to cell
                }
            }
            cells = list
            grid.setSize(model.columns * CELL_PITCH, model.rows * CELL_PITCH)
            highlight(RibbonGridSize(0, 0))
        }

        fun highlight(size: RibbonGridSize) {
            if (size == highlighted && captionSet) return
            captionSet = true
            highlighted = size
            val strings = RibbonStrings.current
            val text = if (size.rows == 0) strings.insertTable else strings.tablePickerFormat(size.columns, size.rows)
            caption.setText(text)
            for ((cellSize, cell) in cells) {
                val active = cellSize.rows <= size.rows && cellSize.columns <= size.columns
                Xaml.childAt(cell.inspectable, 1)?.let { overlay ->
                    XamlElement(overlay).opacity = if (active) 1.0 else 0.0
                    overlay.release()
                }
            }
            (focusTarget ?: root).setAutomationName(text)
        }

        fun onKeyDown(e: XamlKeyEvent) {
            val s = if (highlighted.rows == 0) RibbonGridSize(1, 1) else highlighted
            val next = when (e.key) {
                RibbonInputViews.VK_RIGHT -> s.copy(columns = minOf(model.columns, s.columns + 1))
                RibbonInputViews.VK_LEFT -> s.copy(columns = maxOf(1, s.columns - 1))
                RibbonInputViews.VK_DOWN -> s.copy(rows = minOf(model.rows, s.rows + 1))
                RibbonInputViews.VK_UP -> s.copy(rows = maxOf(1, s.rows - 1))
                RibbonInputViews.VK_HOME -> RibbonGridSize(1, 1)
                RibbonInputViews.VK_ENTER, RibbonInputViews.VK_SPACE -> {
                    pick(s)
                    e.handled = true
                    return
                }
                else -> return
            }
            highlight(next)
            e.handled = true
        }
    }

    private companion object {
        const val PADDING = 4.0
        const val CELL_SIZE = 16.0
        const val CELL_PITCH = 18.0
        const val CAPTION_HEIGHT = 22.0
    }
}

/**
 * Office's color palette (RibbonColorPalette in RibbonSpace): Automatic, Theme Colors with their tints and shades,
 * Standard Colors, Recent Colors, No Color, and More Colors. Placing the pointer on a swatch notifies a live preview.
 */
internal class RibbonColorPaletteView(
    private val model: RibbonColorPickerModel,
    private val onChosen: (RibbonColor?) -> Unit,
    private val onMoreColors: () -> Unit,
) {
    /** The palette's element (the arrow keys move between swatches, and Tab stops only once). */
    val element: XamlElement = XamlElement.load(
        "<StackPanel Spacing=\"4\" Padding=\"4\" XYFocusKeyboardNavigation=\"Enabled\" TabFocusNavigation=\"Once\" />",
    )
    private val swatches = mutableListOf<Pair<RibbonColor, XamlElement>>()
    private var previewing = false

    init {
        build()
    }

    private fun build() {
        val strings = RibbonStrings.current
        element.clearChildren()
        swatches.clear()
        if (model.showAutomatic) element.addChild(commandRow(strings.automatic, model.automaticColor, null) { select(model.automaticColor) })
        element.addChild(header(strings.themeColors))
        val grid = RibbonColorPalette.buildThemeGrid(model.themeColors.toList().ifEmpty { null })
        element.addChild(swatchRow(grid[0]))
        val shades = XamlElement.load("<StackPanel />")
        for (row in grid.drop(1)) shades.addChild(swatchRow(row))
        element.addChild(shades)
        element.addChild(header(strings.standardColors))
        element.addChild(swatchRow(model.standardColors.toList().ifEmpty { RibbonColorPalette.STANDARD_COLORS }))
        if (model.recentColors.isNotEmpty()) {
            element.addChild(header(strings.recentColors))
            element.addChild(swatchRow(model.recentColors.take(RECENT_MAX).map { RibbonColorSwatch(it, it.toHex()) }))
        }
        if (model.showNoColor) element.addChild(commandRow(strings.noColor, null, "&#xE711;") { select(null) })
        if (model.showMoreColors) element.addChild(commandRow(strings.moreColors, null, "&#xE790;") { onMoreColors() })
    }

    private fun header(text: String) =
        XamlElement.load("<TextBlock Text=\"${Xaml.escape(text)}\" FontSize=\"12\" FontWeight=\"SemiBold\" Margin=\"2,4,2,0\" />")

    private fun swatchRow(row: List<RibbonColorSwatch>): XamlElement {
        val panel = XamlElement.load("<StackPanel Orientation=\"Horizontal\" Spacing=\"3\" Margin=\"2,0,2,0\" />")
        for (swatch in row) {
            val color = swatch.color
            val button = XamlElement.load(
                "<Button Style=\"{StaticResource RibbonSwatchButtonStyle}\" Width=\"18\" Height=\"18\" Padding=\"0\" MinWidth=\"0\" MinHeight=\"0\" " +
                    "Background=\"${color.toHex(includeAlpha = true)}\" />",
            )
            button.setToolTipValue(swatch.name)
            button.setAutomationName(swatch.name)
            button.onClick { select(color) }
            button.onPointer(XamlInterop.IUIElement_add_PointerEntered) { preview(color) }
            button.onPointer(XamlInterop.IUIElement_add_PointerExited) { preview(null) }
            button.applyTemplate()
            button.goToState(if (color == model.selectedColor) "Selected" else "Unselected")
            swatches += color to button
            panel.addChild(button)
        }
        return panel
    }

    private fun commandRow(text: String, swatch: RibbonColor?, glyph: String?, action: () -> Unit): XamlElement {
        val leading = when {
            swatch != null ->
                "<Border Width=\"16\" Height=\"16\" CornerRadius=\"2\" BorderThickness=\"1\" " +
                    "BorderBrush=\"{ThemeResource RibbonSwatchBorderBrush}\" Background=\"${swatch.toHex(includeAlpha = true)}\" />"
            glyph != null -> "<FontIcon Glyph=\"$glyph\" FontSize=\"14\" />"
            else -> ""
        }
        val button = XamlElement.load(
            "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Left\">" +
                "<StackPanel Orientation=\"Horizontal\" Spacing=\"8\">$leading" +
                "<TextBlock Text=\"${Xaml.escape(text)}\" VerticalAlignment=\"Center\" /></StackPanel></Button>",
        )
        button.setAutomationName(text)
        button.onClick { action() }
        return button
    }

    /** Puts the focus on the selected swatch (or the first one if none). */
    fun focusSelected() {
        (swatches.firstOrNull { it.first == model.selectedColor } ?: swatches.firstOrNull())?.second?.focus(XamlInterop.FocusState_Programmatic)
    }

    /** Ends the live preview. */
    fun endPreview() {
        if (previewing) {
            previewing = false
            model.firePreview(null, true)
        }
    }

    private fun preview(color: RibbonColor?) {
        if (color == null) {
            endPreview()
            return
        }
        previewing = true
        model.firePreview(color, false)
    }

    /** Selects a color (colors not in the palette are added to the recent colors). */
    fun select(color: RibbonColor?) {
        endPreview()
        model.selectedColor = color
        if (color != null && !isPaletteColor(color)) model.rememberRecent(color)
        onChosen(color)
    }

    private fun isPaletteColor(color: RibbonColor): Boolean = RibbonColorPalette.STANDARD_COLORS.any { it.color == color } ||
        RibbonColorPalette.buildThemeGrid().flatten().any { it.color == color }

    private companion object {
        const val RECENT_MAX = 10
    }
}

/**
 * A color picker (RibbonColorPicker in RibbonSpace). It is a split button whose primary action part applies the current
 * color (or opens the palette if not split), and a color bar shows the selected color. Inside a drop-down ([embedded])
 * it shows the palette itself.
 */
internal class RibbonColorPickerView(override val model: RibbonColorPickerModel, host: RibbonItemHost, private val embedded: Boolean) :
    RibbonItemView(model, host) {
    override val element: XamlElement = if (embedded) {
        XamlElement.load("<Border />")
    } else {
        XamlElement.load("<ContentControl Style=\"{StaticResource RibbonSplitButtonHostStyle}\" />")
    }
    private val primary: XamlElement? by lazy { if (embedded) null else element.templatePart("PART_PrimaryButton") }
    private val secondary: XamlElement? by lazy { if (embedded) null else element.templatePart("PART_SecondaryButton") }
    private val flyout: WFlyout? = if (embedded) null else WFlyout()
    private var palette: RibbonColorPaletteView? = null

    override fun attach() {
        if (embedded) {
            val view = RibbonColorPaletteView(model, ::chosen, ::showMoreColors)
            palette = view
            element.setChild(view.element)
        } else {
            val f = flyout!!
            f.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
            RibbonMenus.applyFlyoutStyle(f, bare = false)
            RibbonMenus.onOpening(f) {
                val view = RibbonColorPaletteView(model, ::chosen, ::showMoreColors)
                palette = view
                f.content = view.element
            }
            RibbonMenus.onOpened(f) {
                element.goToState("DropDownOpen")
                palette?.focusSelected()
            }
            RibbonMenus.onClosed(f) {
                element.goToState("DropDownClosed")
                palette?.endPreview()
            }
            primary!!.onClick { invokePrimary() }
            secondary!!.onClick { openDropDown() }
            // Open the palette with Alt+Down / F4 whether the focus is on the primary action or the arrow
            element.onKeyDown { e ->
                if (RibbonInputViews.isDropDownKey(e.key)) {
                    openDropDown()
                    e.handled = true
                }
            }
        }
        super.attach()
    }

    private fun parts(layout: RibbonItemLayout): Pair<RibbonContentLayout, RibbonContentLayout> {
        val iconBrush = if (isEffectivelyEnabled) RibbonIconXaml.ICON_BRUSH else DISABLED_ICON_BRUSH
        val base = RibbonContentSpec(
            model.label,
            model.icon,
            model.largeIcon,
            layout.size,
            layout.metrics,
            isSimplified = layout.isSimplified,
            colorBar = model.selectedColor ?: RibbonColor.TRANSPARENT,
            iconForeground = iconBrush,
        )
        val primarySpec = base.copy(showLabel = showsLabel(layout))
        val secondarySpec = base.copy(label = null, icon = null, largeIcon = null, showChevron = true, colorBar = null)
        return RibbonItemContent.layout(primarySpec, host.textWidths) to RibbonItemContent.layout(secondarySpec, host.textWidths)
    }

    override fun measure(layout: RibbonItemLayout): WSize {
        if (embedded) return WSize(PALETTE_WIDTH, PALETTE_HEIGHT)
        val (p, s) = parts(layout)
        return WSize(p.width + s.width + 2, maxOf(p.height, s.height) + 2)
    }

    override fun applyLayoutCore() {
        if (embedded) return
        val (p, s) = parts(layout)
        val height = maxOf(p.height, s.height)
        primary!!.setContent(XamlElement.load(p.xaml))
        secondary!!.setContent(XamlElement.load(s.xaml))
        primary!!.setSize(p.width, height)
        secondary!!.setSize(s.width, height)
        primary!!.horizontalAlignment = HorizontalAlignment.LEFT
        secondary!!.horizontalAlignment = HorizontalAlignment.LEFT
        secondary!!.setMargin(p.width, 0.0, 0.0, 0.0)
        element.setSize(p.width + s.width + 2, height + 2)
        val name = model.label?.replace('\n', ' ').orEmpty()
        primary!!.setAutomationName(name)
        secondary!!.setAutomationName(RibbonStrings.current.splitButtonOptions(name).trim())
    }

    override fun toolTipTarget(): XamlElement = primary ?: element

    override fun refreshEnabled() {
        super.refreshEnabled()
        if (!embedded) applyLayoutCore()
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "selectedColor") applyLayoutCore()
    }

    private fun chosen(color: RibbonColor?) {
        flyout?.hide()
        applyLayoutCore()
        execute(color)
    }

    /** Primary action: applies the current color if split; otherwise opens the palette. */
    private fun invokePrimary() {
        if (!model.isSplit) {
            openDropDown()
            return
        }
        execute(model.selectedColor)
    }

    /** Opens the palette. */
    fun openDropDown() {
        if (model.isEnabled) flyout?.showAt(element)
    }

    /** Shows the "More Colors" dialog (color selection and hex input), and selects the color on OK. */
    private fun showMoreColors() {
        flyout?.hide()
        val owner: WComponent = host.ribbon ?: element
        val initial = model.selectedColor ?: RibbonColor.BLACK
        val picker = WColorPicker()
        picker.color = WColor(initial.r, initial.g, initial.b, initial.a)
        val strings = RibbonStrings.current
        val dialog = WContentDialog(strings.moreColors.trimEnd('.', '…'), picker)
        dialog.primaryButtonText = strings.ok
        dialog.closeButtonText = strings.cancel
        dialog.defaultButton = ContentDialogButton.PRIMARY
        try {
            dialog.show(owner) { result ->
                if (result == ContentDialogResult.PRIMARY) {
                    val c = picker.color
                    val color = RibbonColor(c.alpha, c.red, c.green, c.blue)
                    model.selectedColor = color
                    model.rememberRecent(color)
                    chosen(color)
                }
            }
        } catch (e: IllegalStateException) {
            // Only one ContentDialog can be open per window (it is not shown while another dialog is open)
            System.err.println("RibbonColorPicker: More Colors dialog failed: ${e.message}")
        }
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        invokePrimary()
        return true
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        if (embedded) palette?.focusSelected() else openDropDown()
        return RibbonKeyTipResult.Close
    }

    override fun keyTipTargets(): List<RibbonKeyTipTarget> = if (embedded) emptyList() else listOf(this)

    private companion object {
        const val PALETTE_WIDTH = 226.0
        const val PALETTE_HEIGHT = 300.0
    }
}
