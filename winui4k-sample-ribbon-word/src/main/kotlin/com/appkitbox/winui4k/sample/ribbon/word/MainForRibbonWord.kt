package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.extension.ribbon.WRibbon
import com.appkitbox.winui4k.extension.ribbon.WRibbonStatusBar
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonLabelModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStatusBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.toggle

/**
 * Word-style ribbon demo (same layout as the RibbonSpace Word demo).
 * Title bar (AutoSave, QAT, title, search, and account), ribbon, document, and status bar (view switching and zoom).
 * The ribbon's formatting commands apply to the document's body and heading.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.WORD)
        var document: WordDocument? = null
        val model = createWordModel { document?.log(Messages.get("log.saved")) }
        val ribbon = WRibbon(model)
        val window = RibbonDemoWindow(Messages.get("window.title"), RibbonDemoApp.WORD, ribbon)
        val doc = WordDocument(model, window)
        document = doc
        ribbon.addItemInvokedListener(doc::onItemInvoked)
        ribbon.addTabChangeListener { event -> doc.log(Messages.get("log.tab", event.newTab?.label)) }
        val titleBar = window.titleBar!!
        titleBar.subtitle = Messages.get("title.subtitle")
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
    panel.add(toggle.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    panel.add(WRibbonTheme.label(Messages.get("title.autosave"), "RibbonTitleBarForegroundBrush", LABEL_SIZE).also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return panel
}

/** The status bar (page, word count, and language, and view switching and zoom). The zoom applies to the document's text size. */
private fun statusBar(ribbon: WRibbon, document: WordDocument): WComponent {
    val model = RibbonStatusBarModel()
    model.items.add(RibbonLabelModel("status.page", Messages.get("status.page")))
    model.items.add(RibbonLabelModel("status.words", Messages.get("status.words")))
    model.items.add(RibbonLabelModel("status.language", Messages.get("status.language")))
    fun view(id: String, label: String, icon: RibbonIcon, checked: Boolean = false) =
        toggle(id, label, icon).also {
            it.groupName = "statusView"
            it.isChecked = checked
        }
    model.endItems.add(button("status.focus", Messages.get("status.focus"), glyph('\uE740')))
    model.endItems.add(view("status.read", Messages.get("status.read"), glyph('\uE736')))
    model.endItems.add(view("status.print", Messages.get("status.print"), glyph('\uE7C3'), checked = true))
    model.endItems.add(view("status.web", Messages.get("status.web"), glyph('\uE774')))
    val zoom = RibbonZoomModel("status.zoom", ZOOM)
    zoom.addPropertyChangeListener { event -> if (event.propertyName == "value") document.setZoom(zoom.value) }
    model.endItems.add(zoom)
    val bar = WRibbonStatusBar(model)
    bar.ribbon = ribbon
    return bar
}

private const val LABEL_SIZE = 14.0
private const val ZOOM = 100.0
