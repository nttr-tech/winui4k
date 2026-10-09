package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextAlignment
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.extension.ribbon.RibbonItemInvokedEvent
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoWindow

/**
 * The document of the Word demo (same as PageSheet in WordPage.xaml: heading, body, buttons for the table and dark mode,
 * a sample table, and the last command). Applies the ribbon's formatting commands to the body and heading (same as
 * OnItemInvoked in WordPage.xaml.cs).
 */
internal class WordDocument(private val model: RibbonModel, private val window: RibbonDemoWindow) {
    private val heading = WLabel("Quarterly Report")
    private val body = WLabel(BODY_TEXT)
    private val bodyBox = WBorder(body)
    private val lastCommand = WLabel("Last command: —")
    private val content = WPanel(spacing = 14.0)
    private val page = WBorder(content)
    private val table = TableSample()
    private val picture = pictureSample()
    private var bodySize = BODY_SIZE
    private var zoom = 1.0
    private var style = WordStyle.NORMAL
    private var dark = false

    /** The document view (a scrolling page). */
    val view: WComponent

    init {
        heading.textWrapping = TextWrapping.WRAP
        body.textWrapping = TextWrapping.WRAP
        body.foreground = TEXT_COLOR
        lastCommand.fontSize = LOG_SIZE
        lastCommand.foreground = LOG_COLOR
        content.add(heading)
        content.add(bodyBox)
        content.add(buttons())
        content.add(table.view.also { it.isVisible = false })
        content.add(picture.also { it.isVisible = false })
        content.add(lastCommand)
        page.background = WColor.WHITE
        page.borderColor = PAGE_BORDER
        page.borderThickness = 1.0
        page.cornerRadius = 2.0
        page.padding = PAGE_PADDING
        page.horizontalAlignment = HorizontalAlignment.CENTER
        page.margin = PAGE_MARGIN
        applyStyle(style)
        applyZoom()
        val styles = model.findItem("stylesGallery") as RibbonGalleryModel
        styles.addPreviewListener { event -> applyStyle(event.item?.value as? WordStyle ?: style) }
        fontCombo("fontFamily").addCommitListener { event -> body.fontFamily = event.text.ifBlank { "Segoe UI" } }
        fontCombo("fontSize").addCommitListener { event ->
            event.text.toDoubleOrNull()?.takeIf { it > 1 && it < MAX_FONT_SIZE }?.let { setBodySize(it) }
        }
        view = WScrollPane(page)
    }

    private fun buttons(): WComponent {
        val tableButton = WButton("Select the table (shows Table Tools)")
        tableButton.addActionListener { showTable(!table.view.isVisible) }
        val pictureButton = WButton("Select the picture (shows Picture Tools)")
        pictureButton.addActionListener {
            picture.isVisible = !picture.isVisible
            model.setContextualGroupVisible(PICTURE_TOOLS, picture.isVisible)
        }
        val darkButton = WButton("Toggle dark mode")
        darkButton.addActionListener {
            dark = !dark
            window.setDark(dark)
        }
        val panel = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
        panel.add(tableButton)
        panel.add(pictureButton)
        panel.add(darkButton)
        return panel
    }

    /** Shows [message] in the last command field. */
    fun log(message: String) {
        lastCommand.text = "Last command: $message"
    }

    /** Applies the status bar's zoom (%) to the document's text size. */
    fun setZoom(percent: Double) {
        zoom = percent / PERCENT
        applyZoom()
    }

    /** Handles an invoked ribbon item (applies the formatting to the body and shows it as the last command). */
    fun onItemInvoked(event: RibbonItemInvokedEvent) {
        val id = event.item.id
        if (!applyFormatting(id, event.parameter)) applyInsert(id, event.parameter)
        val parameter = event.parameter?.let { if (it is WordStyle) it.label else it.toString() }
        log((event.item.label ?: id) + (parameter?.let { " ($it)" } ?: ""))
    }

    // Body formatting ([Font] and [Paragraph] on [Home], and [Page Color] on [Design]). Returns true if handled
    @Suppress("CyclomaticComplexMethod") // This is simply the branching on each ribbon command id
    private fun applyFormatting(id: String, parameter: Any?): Boolean {
        when (id) {
            "bold", "italic", "underline", "strikethrough" -> updateFont()
            "alignLeft" -> body.textAlignment = TextAlignment.LEFT
            "alignCenter" -> body.textAlignment = TextAlignment.CENTER
            "alignRight" -> body.textAlignment = TextAlignment.RIGHT
            "justify" -> body.textAlignment = TextAlignment.JUSTIFY
            "growFont" -> setBodySize(minOf(MAX_STEP_SIZE, bodySize + FONT_STEP))
            "shrinkFont" -> setBodySize(maxOf(MIN_STEP_SIZE, bodySize - FONT_STEP))
            "clearFormatting" -> clearFormatting()
            "fontColor" -> body.foreground = (parameter as? RibbonColor)?.toWColor() ?: TEXT_COLOR
            "highlight" -> bodyBox.background = (parameter as? RibbonColor)?.toWColor()
            "pageColor" -> page.background = (parameter as? RibbonColor)?.toWColor() ?: WColor.WHITE
            "stylesGallery" -> (parameter as? WordStyle)?.let {
                style = it
                applyStyle(it)
            }
            else -> return false
        }
        return true
    }

    // Tables and pictures ([Table] on [Insert], and the [Table Tools] and [Picture Tools] tabs)
    private fun applyInsert(id: String, parameter: Any?) {
        when (id) {
            "tablePicker" -> (parameter as? RibbonGridSize)?.let { showTable(true) }
            "tableStylesGallery" -> (parameter as? String)?.let { table.headerColor = RibbonColor.parse(it) }
            "tableShading" -> table.bandColor = parameter as? RibbonColor
            "headerRow" -> table.hasHeader = parameter == true
            "pictureBorder" -> picture.borderColor = (parameter as? RibbonColor)?.toWColor()
        }
    }

    private fun showTable(show: Boolean) {
        table.view.isVisible = show
        model.setContextualGroupVisible(TABLE_TOOLS, show)
    }

    private fun isChecked(id: String): Boolean = (model.findItem(id) as? RibbonToggleButtonModel)?.isChecked == true

    private fun fontCombo(id: String): RibbonComboBoxModel = model.findItem(id) as RibbonComboBoxModel

    private fun updateFont() {
        body.fontWeight = if (isChecked("bold")) BOLD else NORMAL
        body.isItalic = isChecked("italic")
        body.isUnderline = isChecked("underline")
        body.isStrikethrough = isChecked("strikethrough")
    }

    private fun clearFormatting() {
        for (id in listOf("bold", "italic", "underline", "strikethrough")) (model.findItem(id) as? RibbonToggleButtonModel)?.isChecked = false
        body.foreground = TEXT_COLOR
        bodyBox.background = null
        setBodySize(BODY_SIZE)
        updateFont()
    }

    private fun setBodySize(size: Double) {
        bodySize = size
        fontCombo("fontSize").text = formatSize(size)
        applyZoom()
    }

    private fun applyStyle(target: WordStyle) {
        heading.fontSize = target.headingSize * zoom
        heading.foreground = RibbonColor.parse(target.headingColor).toWColor()
        heading.isItalic = target.isItalic
        heading.fontWeight = if (target == WordStyle.STRONG) BOLD else NORMAL
    }

    private fun applyZoom() {
        body.fontSize = bodySize * zoom
        heading.fontSize = style.headingSize * zoom
        page.width = PAGE_WIDTH * zoom
    }

    private fun pictureSample(): WBorder {
        val image = WLabel("\uEB9F")
        image.fontFamily = "Segoe Fluent Icons"
        image.fontSize = PICTURE_GLYPH_SIZE
        image.foreground = WColor(0x87, 0x64, 0xB8)
        image.horizontalAlignment = HorizontalAlignment.CENTER
        val border = WBorder(image)
        border.width = PICTURE_SIZE
        border.height = PICTURE_SIZE * 2 / 3
        border.background = WColor(0xF3, 0xEE, 0xFA)
        border.borderColor = WColor(0x44, 0x72, 0xC4)
        border.borderThickness = 2.0
        border.horizontalAlignment = HorizontalAlignment.LEFT
        return border
    }

    /** The table in the document (3 rows x 3 columns, same as TableSample in WordPage.xaml). The table style changes the header row and banding colors. */
    private class TableSample {
        private val cells = List(ROWS) { row -> List(COLUMNS) { column -> WLabel(TABLE[row][column]) } }
        private val borders = cells.map { row -> row.map { label -> WBorder(label).also { it.padding = CELL_PADDING } } }

        /** The table view. */
        val view: WComponent

        var headerColor: RibbonColor = RibbonColor.parse("#4472C4")
            set(value) {
                field = value
                refresh()
            }

        var bandColor: RibbonColor? = RibbonColor.parse("#DEEBF7")
            set(value) {
                field = value
                refresh()
            }

        var hasHeader: Boolean = true
            set(value) {
                field = value
                refresh()
            }

        init {
            val grid = WGrid(rowSpacing = 1.0, columnSpacing = 1.0)
            repeat(ROWS) { grid.addRow() }
            repeat(COLUMNS) { grid.addColumn(GridLength.star()) }
            borders.forEachIndexed { row, line -> line.forEachIndexed { column, border -> grid.add(border, row, column) } }
            view = WBorder(grid)
            refresh()
        }

        private fun refresh() {
            val outline = view as WBorder
            outline.background = headerColor.toWColor()
            outline.borderColor = headerColor.toWColor()
            outline.borderThickness = 1.0
            for (column in 0 until COLUMNS) {
                borders[0][column].background = if (hasHeader) headerColor.toWColor() else WColor.WHITE
                cells[0][column].foreground = if (hasHeader && !headerColor.prefersDarkForeground) WColor.WHITE else TEXT_COLOR
                cells[0][column].fontWeight = if (hasHeader) SEMI_BOLD else NORMAL
                borders[1][column].background = bandColor?.toWColor() ?: WColor.WHITE
                borders[2][column].background = WColor.WHITE
                cells[1][column].foreground = TEXT_COLOR
                cells[2][column].foreground = TEXT_COLOR
            }
        }
    }
}

private fun RibbonColor.toWColor(): WColor = WColor(r, g, b, a)

private fun formatSize(size: Double): String = if (size % 1.0 == 0.0) size.toInt().toString() else size.toString()

private const val BODY_TEXT =
    "WinUI4K's ribbon provides an Office-style ribbon in pure Kotlin. Try choosing styles, fonts, colors, and alignment on the ribbon " +
        "to change the formatting of this paragraph. Point at the style gallery for a live preview, press Alt for KeyTips, " +
        "Alt+Q to search, Ctrl+F1 to collapse the ribbon, and right-click a command to add it to the QAT."

private val TABLE = listOf(listOf("Region", "Q3", "Q4"), listOf("North", "1.2M", "1.5M"), listOf("South", "0.9M", "1.1M"))
private const val ROWS = 3
private const val COLUMNS = 3
private val TEXT_COLOR = WColor(0x24, 0x24, 0x24)
private val LOG_COLOR = WColor(0x42, 0x42, 0x42)
private val PAGE_BORDER = WColor(0, 0, 0, 0x20)
private const val BODY_SIZE = 16.0
private const val LOG_SIZE = 12.0
private const val FONT_STEP = 2.0
private const val MIN_STEP_SIZE = 8.0
private const val MAX_STEP_SIZE = 96.0
private const val MAX_FONT_SIZE = 400.0
private const val PERCENT = 100.0
private const val PAGE_WIDTH = 760.0
private const val PAGE_PADDING = 64.0
private const val PAGE_MARGIN = 24.0
private const val CELL_PADDING = 8.0
private const val PICTURE_SIZE = 240.0
private const val PICTURE_GLYPH_SIZE = 72.0
private const val NORMAL = 400
private const val SEMI_BOLD = 600
private const val BOLD = 700
