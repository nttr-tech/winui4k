package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/**
 * Word-style ribbon demo (same layout as word.png in the RibbonSpace README).
 * Title bar (AutoSave, QAT, title, search, and account), app rail on the left, ribbon, document, and status bar (view
 * switching and zoom).
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.WORD)
        val model = createWordModel()
        val ribbon = WRibbon(model)
        val window = RibbonDemoWindow("Quarterly Report - Word (WinUI4K Ribbon)", RibbonDemoApp.WORD, ribbon)
        val lastCommand = WLabel("Last command: none")
        ribbon.addItemInvokedListener { event -> lastCommand.text = "Last command: ${event.item.label ?: event.item.id}" }
        ribbon.addTabChangeListener { event -> lastCommand.text = "Last command: Tab: ${event.newTab?.label}" }
        val titleBar = window.titleBar!!
        titleBar.subtitle = "• Saved to this PC"
        titleBar.startContent = autoSaveSwitch()
        titleBar.endContent = RibbonDemoWindow.avatar("WS", WColor(0x18, 0x5A, 0xBD))
        window.setTop(ribbon)
        window.setContent(documentArea(model, window, lastCommand))
        window.setStatusBar(statusBar(ribbon))
        window.show()
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

/** The document page (heading, body, and buttons for Table Tools and dark mode). */
private fun documentArea(model: RibbonModel, window: RibbonDemoWindow, lastCommand: WLabel): WComponent {
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
        window.setDark(dark)
        pageBorder.background = if (dark) WColor(0x29, 0x29, 0x29) else WColor(255, 255, 255)
        heading.foreground = if (dark) WColor(0x8F, 0xB8, 0xF0) else WColor(0x1F, 0x4E, 0x99)
        body.foreground = if (dark) WColor(240, 240, 240) else WColor(0x24, 0x24, 0x24)
    }
    // Try heading sizes with the style gallery's live preview
    (model.findItem("styles.gallery") as com.appkitbox.winui4k.ribbon.RibbonGalleryModel).addPreviewListener { event ->
        heading.fontSize = event.item?.previewFontSize?.let { it * PREVIEW_SCALE } ?: HEADING_SIZE
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

private const val LABEL_SIZE = 14.0
private const val HEADING_SIZE = 32.0
private const val PREVIEW_SCALE = 1.8
private const val BODY_SIZE = 16.0
private const val PAGE_WIDTH = 860.0
private const val PAGE_PADDING = 56.0
private const val PAGE_MARGIN = 24.0
private const val ZOOM = 100.0
