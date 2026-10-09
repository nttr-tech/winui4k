package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/**
 * Word-style ribbon demo (same layout as the RibbonSpace Word demo).
 * Title bar (AutoSave, QAT, title, search, and account), ribbon, document, and status bar (view switching and zoom).
 * The ribbon's formatting commands apply to the document's body and heading.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.WORD)
        var document: WordDocument? = null
        val model = createWordModel { document?.log("Document saved") }
        val ribbon = WRibbon(model)
        val window = RibbonDemoWindow("Quarterly Report - Word (WinUI4K Ribbon)", RibbonDemoApp.WORD, ribbon)
        val doc = WordDocument(model, window)
        document = doc
        ribbon.addItemInvokedListener(doc::onItemInvoked)
        ribbon.addTabChangeListener { event -> doc.log("Tab: ${event.newTab?.label}") }
        val titleBar = window.titleBar!!
        titleBar.subtitle = "• Saved to this PC"
        titleBar.startContent = autoSaveSwitch()
        titleBar.endContent = RibbonDemoWindow.avatar("WS", WColor(0x18, 0x5A, 0xBD))
        window.setTop(ribbon)
        window.setContent(doc.view)
        window.setStatusBar(statusBar(ribbon, doc))
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

/** The status bar (page, word count, and language, and view switching and zoom). The zoom applies to the document's text size. */
private fun statusBar(ribbon: WRibbon, document: WordDocument): WComponent {
    val model = RibbonStatusBarModel()
    model.items.add(RibbonLabelModel("status.page", "Page 1 of 1"))
    model.items.add(RibbonLabelModel("status.words", "64 words"))
    model.items.add(RibbonLabelModel("status.language", "English (United States)"))
    fun view(id: String, label: String, icon: RibbonIcon, checked: Boolean = false) =
        toggle(id, label, icon).also {
            it.groupName = "statusView"
            it.isChecked = checked
        }
    model.endItems.add(button("status.focus", "Focus", glyph('')))
    model.endItems.add(view("status.read", "Read Mode", glyph('')))
    model.endItems.add(view("status.print", "Print Layout", glyph(''), checked = true))
    model.endItems.add(view("status.web", "Web Layout", glyph('')))
    val zoom = RibbonZoomModel("status.zoom", ZOOM)
    zoom.addPropertyChangeListener { event -> if (event.propertyName == "value") document.setZoom(zoom.value) }
    model.endItems.add(zoom)
    val bar = WRibbonStatusBar(model)
    bar.ribbon = ribbon
    return bar
}

private const val LABEL_SIZE = 14.0
private const val ZOOM = 100.0
