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
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode
import com.appkitbox.winui4k.ribbon.RibbonFontComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedVisibility
import com.appkitbox.winui4k.ribbon.RibbonStatusBarModel
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonZoomModel
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoApp
import com.appkitbox.winui4k.sample.ribbon.shell.RibbonDemoWindow
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.dropDown
import com.appkitbox.winui4k.sample.ribbon.shell.group
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.split
import com.appkitbox.winui4k.sample.ribbon.shell.tab
import com.appkitbox.winui4k.sample.ribbon.shell.toggle

/** A slide theme (background, title, and body text colors). */
private data class SlideTheme(val name: String, val background: String, val title: String, val text: String)

private val THEMES = listOf(
    SlideTheme("Office Theme", "#FFFFFF", "#242424", "#555555"),
    SlideTheme("Facet", "#F2F7EE", "#2F5D20", "#4A6B3E"),
    SlideTheme("Ion", "#1B3A57", "#FFFFFF", "#C9DCEF"),
    SlideTheme("Integral", "#F6F0E6", "#7A3E12", "#8C5B33"),
    SlideTheme("Slice", "#2D2B55", "#F7C948", "#E0DDF5"),
    SlideTheme("Wisp", "#FBF4EC", "#9C4A1A", "#6E5A48"),
)

/**
 * PowerPoint-style ribbon demo (same layout as ppt.png in the RibbonSpace README): simplified ribbon, slide list on the
 * left, slide, and status bar. Pointing at the theme gallery on the [Design] tab shows a live preview on the slide.
 */
fun main() {
    WinUiUtilities.invokeLater {
        WRibbonTheme.applyPalette(RibbonThemePalette.POWER_POINT)
        val slide = SlideView()
        val model = createPowerPointModel(slide)
        val ribbon = WRibbon(model)
        val window = RibbonDemoWindow("Product Launch - PowerPoint (WinUI4K Ribbon)", RibbonDemoApp.POWER_POINT, ribbon)
        window.titleBar!!.subtitle = "• Simplified ribbon"
        window.setTop(ribbon)
        val body = WGrid()
        body.addColumn(GridLength.AUTO)
        body.addColumn(GridLength.star())
        body.add(slideList(), row = 0, column = 0)
        body.add(slide.element, row = 0, column = 1)
        window.setContent(body)
        window.setStatusBar(statusBar(ribbon))
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
        title.foreground = color(theme.title)
        subtitle.foreground = color(theme.text)
    }

    fun apply(theme: SlideTheme) {
        applied = theme
        show(theme)
    }

    private fun color(hex: String): WColor = RibbonColor.parse(hex).let { WColor(it.r, it.g, it.b, it.a) }
}

/** The simplified ribbon model. */
@Suppress("LongMethod") // Declaratively builds the PowerPoint ribbon from top to bottom
private fun createPowerPointModel(slide: SlideView): RibbonModel = RibbonModel().apply {
    displayMode = RibbonDisplayMode.SIMPLIFIED
    applicationButtonLabel = "File"
    title = "Product Launch"
    tabs.add(
        tab(
            "home", "Home", "H",
            group(
                "clipboard",
                "Clipboard",
                split("paste", "Paste", RibbonIcons.PASTE, RibbonItemSize.LARGE, menuItem("paste.keep", "Keep Source Formatting")),
                button("cut", "Cut", RibbonIcons.CUT, RibbonItemSize.SMALL),
                button("copy", "Copy", RibbonIcons.COPY, RibbonItemSize.SMALL),
            ),
            group(
                "slides",
                "Slides",
                split("newSlide", "New Slide", RibbonIcons.ADD, RibbonItemSize.LARGE, menuItem("layout.title", "Title Slide"), menuItem("layout.content", "Title and Content")),
                dropDown("layout", "Layout", RibbonIcons.LAYOUT, RibbonItemSize.SMALL, menuItem("layout.blank", "Blank")),
            ),
            group(
                "font",
                "Font",
                RibbonFontComboBoxModel("font.name").also {
                    it.selectedItem = "Aptos Display"
                    it.items.add(0, "Aptos Display")
                },
                RibbonFontSizeComboBoxModel("font.size").also { it.selectedItem = 28.0 },
                RibbonButtonGroupModel(
                    toggle("bold", "Bold", RibbonIcons.BOLD),
                    toggle("italic", "Italic", RibbonIcons.ITALIC),
                    toggle("underline", "Underline", RibbonIcons.UNDERLINE),
                ),
                RibbonColorPickerModel("fontColor", "Font Color", RibbonIcons.FONT_COLOR),
            ),
            group(
                "paragraph",
                "Paragraph",
                split("bullets", "Bullets", RibbonIcons.BULLETS, RibbonItemSize.SMALL, menuItem("bullets.dot", "Filled Round Bullets")),
                RibbonButtonGroupModel(
                    toggle("align.left", "Align Left", RibbonIcons.ALIGN_LEFT).also {
                        it.groupName = "align"
                        it.isChecked = true
                    },
                    toggle("align.center", "Center", RibbonIcons.ALIGN_CENTER).also { it.groupName = "align" },
                    toggle("align.right", "Align Right", RibbonIcons.ALIGN_RIGHT).also { it.groupName = "align" },
                ),
            ),
            group(
                "drawing",
                "Drawing",
                dropDown("picture", "Pictures", RibbonIcons.PICTURE, RibbonItemSize.SMALL, menuItem("picture.device", "This Device...")),
                dropDown("shapes", "Shapes", RibbonIcons.SHAPES, RibbonItemSize.SMALL, menuItem("shapes.rect", "Rectangle")),
                RibbonColorPickerModel("shapeFill", "Shape Fill", RibbonIcons.FILL),
                dropDown("arrange", "Arrange", RibbonIcons.GRID, RibbonItemSize.SMALL, menuItem("arrange.front", "Bring to Front")),
            ),
            group(
                "designer",
                "Designer",
                button("designer", "Designer", RibbonIcons.PALETTE, RibbonItemSize.LARGE).also { it.simplifiedVisibility = RibbonSimplifiedVisibility.PINNED },
            ),
        ),
    )
    tabs.add(tab("insert", "Insert", "N", group("insertSlides", "Slides", button("insert.slide", "New Slide", RibbonIcons.ADD, RibbonItemSize.LARGE)), group("media", "Media", button("insert.video", "Video", RibbonIcons.VIDEO, RibbonItemSize.LARGE), button("insert.audio", "Audio", RibbonIcons.AUDIO, RibbonItemSize.LARGE))))
    tabs.add(tab("design", "Design", "G", group("themes", "Themes", themesGallery(slide))))
    tabs.add(tab("transitions", "Transitions", "K", group("transition", "Transitions", button("transition.fade", "Fade", RibbonIcons.TRANSITION, RibbonItemSize.LARGE))))
    tabs.add(tab("slideShow", "Slide Show", "S", group("start", "Start Slide Show", button("show.start", "From Beginning", RibbonIcons.PLAY, RibbonItemSize.LARGE).also { it.shortcut = "F5" })))
    tabs.add(tab("view", "View", "W", group("presentationViews", "Presentation Views", toggle("view.normal", "Normal", RibbonIcons.SLIDE, RibbonItemSize.LARGE).also { it.isChecked = true })))
    quickAccessItems.add(button("qat.save", "Save", RibbonIcons.SAVE))
    quickAccessItems.add(button("qat.undo", "Undo", RibbonIcons.UNDO))
    quickAccessItems.add(button("qat.start", "Start From Beginning", RibbonIcons.PLAY))
    tabStripItems.add(button("record", "Record", RibbonIcons.RECORD))
    tabStripItems.add(split("present", "Present", RibbonIcons.PRESENT, RibbonItemSize.MEDIUM, menuItem("present.current", "From Current Slide")))
    tabStripItems.add(button("share", "Share", RibbonIcons.SHARE))
}

/** The theme gallery (pointing at a theme shows a live preview on the slide; selecting one applies it). */
private fun themesGallery(slide: SlideView): RibbonGalleryModel = RibbonGalleryModel("themes.gallery", "Themes").also { gallery ->
    gallery.icon = RibbonIcons.THEME
    gallery.itemWidth = 96.0
    gallery.itemHeight = 60.0
    gallery.maxColumns = 6
    for (theme in THEMES) {
        gallery.items.add(
            RibbonGalleryItemModel(theme.name, theme.name, value = theme).also {
                it.previewText = "Aa"
                it.previewFontSize = 20.0
                it.previewBackground = RibbonColor.parse(theme.background)
                it.previewForeground = RibbonColor.parse(theme.title)
            },
        )
    }
    gallery.selectedItem = gallery.items[0]
    gallery.addPreviewListener { event -> slide.show((event.item?.value as? SlideTheme) ?: slide.applied) }
    gallery.addActionListener { event -> (event.parameter as? SlideTheme)?.let { slide.apply(it) } }
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
private fun statusBar(ribbon: WRibbon): WComponent {
    val model = RibbonStatusBarModel()
    model.items.add(RibbonLabelModel("status.slide", "Slide 1 of $SLIDES"))
    model.items.add(RibbonLabelModel("status.hint", "Point at the theme gallery on [Design] for a live preview"))
    model.endItems.add(toggle("status.notes", "Notes", RibbonIcons.NOTEBOOK))
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
