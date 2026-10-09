package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.ElementTheme
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WRibbonTitleBar
import com.appkitbox.winui4k.WRibbonToolBar
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonToolBarModel
import com.appkitbox.winui4k.ribbon.RibbonToolBarOrientation
import com.appkitbox.winui4k.ribbon.RibbonZoomModel

/**
 * Word-style ribbon demo (same layout as word.png in the RibbonSpace README).
 * Title bar (AutoSave, QAT, title, search, and account), app rail on the left, ribbon, document, and status bar (view
 * switching and zoom).
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.WORD)
        val frame = WFrame(title = "Quarterly Report - Word (WinUI4K Ribbon)")
        val model = createWordModel()
        val ribbon = WRibbon(model)
        val lastCommand = WLabel("Last command: none")
        ribbon.addItemInvokedListener { event -> lastCommand.text = "Last command: ${event.item.label ?: event.item.id}" }
        ribbon.addTabChangeListener { event -> lastCommand.text = "Last command: Tab: ${event.newTab?.label}" }

        val titleBar = WRibbonTitleBar(ribbon)
        titleBar.appIcon = RibbonIcons.DOCUMENT
        titleBar.subtitle = "• Saved to this PC"
        titleBar.startContent = autoSaveSwitch()
        titleBar.endContent = avatar()

        val root = WGrid()
        root.addRow(GridLength.AUTO)
        root.addRow(GridLength.star())
        root.addRow(GridLength.AUTO)
        root.addColumn(GridLength.AUTO)
        root.addColumn(GridLength.star())
        root.add(titleBar, row = 0, column = 0, columnSpan = 2)
        root.add(appRail(ribbon), row = 1, column = 0, rowSpan = 2)
        val body = WGrid()
        body.addRow(GridLength.AUTO)
        body.addRow(GridLength.star())
        body.add(ribbon, row = 0, column = 0)
        body.add(WRibbonTheme.surface(documentArea(model, root, lastCommand)), row = 1, column = 0)
        root.add(body, row = 1, column = 1)
        root.add(statusBar(ribbon), row = 2, column = 1)
        frame.setContentPane(WRibbonTheme.surface(root, "RibbonChromeBackgroundBrush"))
        titleBar.attachToWindow(frame)
        frame.appWindow.resize(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.isVisible = true
    }
}

/** The [AutoSave] switch in the title bar. */
private fun autoSaveSwitch(): WComponent {
    val toggle = WToggleSwitch()
    toggle.isOn = true
    toggle.onContent = ""
    toggle.offContent = ""
    val panel = WPanel(spacing = 6.0, orientation = Orientation.HORIZONTAL)
    panel.add(toggle.also { it.verticalAlignment = VerticalAlignment.CENTER })
    panel.add(WRibbonTheme.label("AutoSave", "RibbonTitleBarForegroundBrush", LABEL_SIZE).also { it.verticalAlignment = VerticalAlignment.CENTER })
    return panel
}

/** The account at the right end of the title bar (a circle with initials). */
private fun avatar(): WComponent {
    val circle = WBorder(
        WLabel("WS").also {
            it.foreground = WColor(255, 255, 255)
            it.fontWeight = SEMI_BOLD
            it.horizontalAlignment = HorizontalAlignment.CENTER
            it.verticalAlignment = VerticalAlignment.CENTER
        },
    )
    circle.width = AVATAR_SIZE
    circle.height = AVATAR_SIZE
    circle.cornerRadius = AVATAR_SIZE / 2
    circle.background = WColor(0x18, 0x5A, 0xBD)
    return circle
}

/** The app rail on the left (documents, spreadsheets, slides, drawing, and settings; a vertical, icon-only toolbar). */
private fun appRail(ribbon: WRibbon): WComponent {
    val model = RibbonToolBarModel("rail")
    model.orientation = RibbonToolBarOrientation.VERTICAL
    model.showLabels = false
    fun item(id: String, label: String, icon: RibbonIcon, checked: Boolean = false) =
        toggle(id, label, icon, RibbonItemSize.SMALL).also {
            it.groupName = "rail"
            it.isChecked = checked
        }
    model.items.add(item("rail.word", "Word", RibbonIcons.DOCUMENT, checked = true))
    model.items.add(item("rail.excel", "Excel", RibbonIcons.TABLE))
    model.items.add(item("rail.ppt", "PowerPoint", RibbonIcons.PRESENT))
    model.items.add(item("rail.draw", "Draw", RibbonIcons.PEN))
    model.items.add(item("rail.settings", "Settings", RibbonIcons.SETTINGS))
    val rail = WRibbonToolBar(model)
    rail.ribbon = ribbon
    rail.margin = RAIL_MARGIN
    return rail
}

/** The document page (heading, body, and buttons for Table Tools and dark mode). */
private fun documentArea(model: RibbonModel, root: WComponent, lastCommand: WLabel): WComponent {
    val heading = WLabel("Quarterly Report")
    heading.fontSize = HEADING_SIZE
    heading.foreground = WColor(0x1F, 0x4E, 0x99)
    val body = WLabel(
        "WinUI4K's ribbon provides an Office-style ribbon in pure Kotlin. Try choosing styles, fonts, colors, and alignment on the ribbon. " +
            "Point at the style gallery for a live preview, press Alt for KeyTips, Alt+Q to search, Ctrl+F1 to collapse the ribbon, " +
            "and right-click a command to add it to the QAT.",
    )
    body.fontSize = BODY_SIZE
    body.textWrapping = TextWrapping.WRAP
    var tableShown = false
    val tableButton = WButton("Select the table (shows Table Tools)")
    tableButton.addActionListener {
        tableShown = !tableShown
        model.setContextualGroupVisible("tableTools", tableShown)
    }
    var dark = false
    val darkButton = WButton("Toggle dark mode")
    val page = WPanel(spacing = 16.0)
    val pageBorder = WBorder(page)
    darkButton.addActionListener {
        dark = !dark
        WRibbonTheme.setTheme(root, if (dark) ElementTheme.DARK else ElementTheme.LIGHT)
        pageBorder.background = if (dark) WColor(0x29, 0x29, 0x29) else WColor(255, 255, 255)
        heading.foreground = if (dark) WColor(0x8F, 0xB8, 0xF0) else WColor(0x1F, 0x4E, 0x99)
        body.foreground = if (dark) WColor(240, 240, 240) else WColor(0x24, 0x24, 0x24)
    }
    val buttons = WPanel(spacing = 10.0, orientation = Orientation.HORIZONTAL)
    buttons.add(tableButton)
    buttons.add(darkButton)
    page.add(heading)
    page.add(body)
    page.add(buttons)
    page.add(lastCommand.also { it.foreground = WColor(0x76, 0x76, 0x76) })
    pageBorder.background = WColor(255, 255, 255)
    pageBorder.borderColor = WColor(0xE0, 0xE0, 0xE0)
    pageBorder.borderThickness = 1.0
    pageBorder.padding = PAGE_PADDING
    pageBorder.maxWidth = PAGE_WIDTH
    pageBorder.horizontalAlignment = HorizontalAlignment.STRETCH
    pageBorder.margin = PAGE_MARGIN
    return WScrollPane(pageBorder)
}

/** The status bar (page, word count, and language, and view switching and zoom). */
private fun statusBar(ribbon: WRibbon): WComponent {
    val model = RibbonStatusBarModel()
    model.items.add(RibbonLabelModel("status.page", "Page 1 of 1"))
    model.items.add(RibbonLabelModel("status.words", "64 words"))
    model.items.add(RibbonLabelModel("status.language", "English (United States)"))
    fun view(id: String, label: String, icon: RibbonIcon, checked: Boolean = false) =
        toggle(id, label, icon).also {
            it.groupName = "view"
            it.isChecked = checked
        }
    model.endItems.add(button("status.focus", "Focus", RibbonIcons.FULL_SCREEN))
    model.endItems.add(view("status.read", "Read Mode", RibbonIcons.READING_MODE))
    model.endItems.add(view("status.print", "Print Layout", RibbonIcons.PAGE, checked = true))
    model.endItems.add(view("status.web", "Web Layout", RibbonIcons.GLOBE))
    model.endItems.add(RibbonZoomModel("status.zoom", ZOOM))
    val bar = WRibbonStatusBar(model)
    bar.ribbon = ribbon
    return bar
}

private const val WINDOW_WIDTH = 2400
private const val WINDOW_HEIGHT = 1350
private const val LABEL_SIZE = 14.0
private const val AVATAR_SIZE = 32.0
private const val SEMI_BOLD = 600
private const val HEADING_SIZE = 32.0
private const val BODY_SIZE = 16.0
private const val PAGE_WIDTH = 860.0
private const val PAGE_PADDING = 56.0
private const val PAGE_MARGIN = 24.0
private const val RAIL_MARGIN = 4.0
private const val ZOOM = 100.0
