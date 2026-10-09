package com.appkitbox.winui4k.sample.ribbon.powerpoint

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonStatusBar
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.common.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.common.button

/**
 * PowerPoint-style ribbon demo (same layout as the RibbonSpace PowerPoint demo): simplified ribbon, slide list on the
 * left, slide, and status bar. Pointing at the theme gallery on the [Design] tab shows a live preview on the slide, and
 * the id and parameter of the invoked item are shown in the status bar.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.POWER_POINT)
        val slide = SlideView()
        val model = createPowerPointModel()
        val themes = model.findItem("pp.themes") as RibbonGalleryModel
        themes.addPreviewListener { event -> slide.show((event.item?.value as? SlideTheme) ?: slide.applied) }
        val ribbon = WRibbon(model)
        val status = RibbonLabelModel("status.hint", "Point at the theme gallery on [Design] for a live preview")
        ribbon.addItemInvokedListener { event ->
            (event.parameter as? SlideTheme)?.let { slide.apply(it) }
            val parameter = event.parameter?.let { if (it is SlideTheme) it.name else it.toString() }
            status.label = event.item.id + (parameter?.let { " ($it)" } ?: "")
        }
        val window = RibbonDemoWindow("Product Launch - PowerPoint (WinUI4K Ribbon)", RibbonDemoApp.POWER_POINT, ribbon)
        window.titleBar!!.subtitle = "• Simplified ribbon"
        window.setTop(ribbon)
        val body = WGrid()
        body.addColumn(GridLength.AUTO)
        body.addColumn(GridLength.star())
        body.add(slideList(), row = 0, column = 0)
        body.add(slide.element, row = 0, column = 1)
        window.setContent(body)
        window.setStatusBar(statusBar(ribbon, status))
        window.show()
    }
}

/** The slide (reflects the theme colors). */
private class SlideView {
    private val title = WLabel("Introducing WinUI4K").also {
        it.fontSize = TITLE_SIZE
        it.fontWeight = SEMI_BOLD
    }
    private val subtitle = WLabel("A modern Office-style ribbon, written in Kotlin").also {
        it.fontSize = SUBTITLE_SIZE
        it.textWrapping = TextWrapping.WRAP
    }
    private val page = WBorder(
        WPanel(spacing = 12.0).also {
            it.add(title)
            it.add(subtitle)
            it.verticalAlignment = VerticalAlignment.CENTER
        },
    )
    val element: WComponent = WScrollPane(page)
    var applied: SlideTheme = THEMES[0]
        private set

    init {
        page.width = SLIDE_WIDTH
        page.height = SLIDE_HEIGHT
        page.padding = SLIDE_PADDING
        page.borderColor = WColor(0xD0, 0xD0, 0xD0)
        page.borderThickness = 1.0
        page.horizontalAlignment = HorizontalAlignment.CENTER
        page.margin = SLIDE_MARGIN
        show(THEMES[0])
    }

    fun show(theme: SlideTheme) {
        page.background = color(theme.background)
        title.foreground = color(theme.foreground)
        subtitle.foreground = color(theme.foreground)
    }

    fun apply(theme: SlideTheme) {
        applied = theme
        show(theme)
    }

    private fun color(hex: String): WColor = RibbonColor.parse(hex).let { WColor(it.r, it.g, it.b, it.a) }
}

/** The slide list on the left (thumbnails). */
private fun slideList(): WComponent {
    val panel = WPanel(spacing = 12.0)
    panel.margin = LIST_MARGIN
    for (index in 1..SLIDES) {
        val thumbnail = WBorder(
            WLabel("Slide $index").also {
                it.foreground = WColor(0x76, 0x76, 0x76)
                it.verticalAlignment = VerticalAlignment.TOP
            },
        )
        thumbnail.width = THUMB_WIDTH
        thumbnail.height = THUMB_HEIGHT
        thumbnail.padding = THUMB_PADDING
        thumbnail.background = WColor(255, 255, 255)
        thumbnail.borderColor = if (index == 1) WColor(0xC4, 0x3E, 0x1C) else WColor(0xD0, 0xD0, 0xD0)
        thumbnail.borderThickness = if (index == 1) 2.0 else 1.0
        thumbnail.cornerRadius = 4.0
        panel.add(thumbnail)
    }
    return WScrollPane(panel)
}

/** The status bar (slide number and hint, and notes and zoom). */
private fun statusBar(ribbon: WRibbon, status: RibbonLabelModel): WComponent {
    val model = RibbonStatusBarModel()
    model.items.add(RibbonLabelModel("status.slide", "Slide 1 of $SLIDES"))
    model.items.add(status)
    model.endItems.add(button("status.notes", "Notes", RibbonIcon.glyph("")))
    model.endItems.add(RibbonZoomModel("status.zoom", ZOOM))
    val bar = WRibbonStatusBar(model)
    bar.ribbon = ribbon
    return bar
}

private const val SLIDES = 4
private const val ZOOM = 64.0
private const val TITLE_SIZE = 40.0
private const val SUBTITLE_SIZE = 22.0
private const val SEMI_BOLD = 600
private const val SLIDE_WIDTH = 820.0
private const val SLIDE_HEIGHT = 460.0
private const val SLIDE_PADDING = 48.0
private const val SLIDE_MARGIN = 24.0
private const val LIST_MARGIN = 16.0
private const val THUMB_WIDTH = 200.0
private const val THUMB_HEIGHT = 112.0
private const val THUMB_PADDING = 8.0
