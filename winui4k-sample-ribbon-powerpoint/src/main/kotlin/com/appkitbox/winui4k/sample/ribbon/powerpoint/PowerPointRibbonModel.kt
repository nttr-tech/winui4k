package com.appkitbox.winui4k.sample.ribbon.powerpoint

import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode
import com.appkitbox.winui4k.ribbon.RibbonFontComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedVisibility
import com.appkitbox.winui4k.ribbon.RibbonSizeDefinition
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.dropDown
import com.appkitbox.winui4k.sample.ribbon.common.group
import com.appkitbox.winui4k.sample.ribbon.common.menuItem
import com.appkitbox.winui4k.sample.ribbon.common.split
import com.appkitbox.winui4k.sample.ribbon.common.tab
import com.appkitbox.winui4k.sample.ribbon.common.toggle

// The PowerPoint ribbon model (same layout as PowerPointPage.xaml in the RibbonSpace PowerPoint demo; starts as a simplified ribbon)

/** A slide theme (the gallery swatch, and the background and text colors applied to the slide). */
internal data class SlideTheme(val name: String, val background: String, val foreground: String)

/** The themes on [Design] (same colors as Themes in PowerPointPage.xaml). */
internal val THEMES = listOf(
    SlideTheme("Office Theme", "#FFFFFF", "#262626"),
    SlideTheme("Facet", "#90C226", "#FFFFFF"),
    SlideTheme("Ion", "#1E5155", "#FFFFFF"),
    SlideTheme("Retrospect", "#E48312", "#FFFFFF"),
    SlideTheme("Slice", "#146194", "#FFFFFF"),
    SlideTheme("Wisp", "#EFE3C6", "#5E3A1B"),
    SlideTheme("Berlin", "#A6452E", "#FFFFFF"),
    SlideTheme("Dividend", "#4D1434", "#FFFFFF"),
)

private val LARGE = RibbonItemSize.LARGE
private val MEDIUM = RibbonItemSize.MEDIUM
private val SMALL = RibbonItemSize.SMALL

private fun glyph(code: Char): RibbonIcon = RibbonIcon.glyph(code.toString())

private fun <T : RibbonItemModel> T.pinned(): T = also { it.simplifiedVisibility = RibbonSimplifiedVisibility.PINNED }

private fun <T : RibbonItemModel> T.overflow(): T = also { it.simplifiedVisibility = RibbonSimplifiedVisibility.OVERFLOW }

private fun <T : RibbonItemModel> T.alwaysLarge(): T = also { it.sizeDefinition = RibbonSizeDefinition.ALWAYS_LARGE }

/** The PowerPoint ribbon model. */
internal fun createPowerPointModel(): RibbonModel = RibbonModel().apply {
    displayMode = RibbonDisplayMode.SIMPLIFIED
    applicationButtonLabel = "File"
    title = "Product Launch"
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(createDesignTab())
    tabs.add(createTransitionsTab())
    tabs.add(createSlideShowTab())
    tabs.add(createViewTab())
    quickAccessItems.add(button("pp.save", "Save", glyph('')))
    quickAccessItems.add(button("pp.undo", "Undo", glyph('')))
    quickAccessItems.add(button("pp.fromBeginning", "From Beginning", glyph('')).also { it.shortcut = "F5" })
    tabStripItems.add(button("pp.record", "Record", glyph('')).also { it.showLabelInSimplified = true })
    tabStripItems.add(split("pp.present", "Present", glyph(''), SMALL, menuItem("pp.present.beginning", "From Beginning"), menuItem("pp.present.current", "From Current Slide")).also { it.showLabelInSimplified = true })
    tabStripItems.add(button("pp.share", "Share", glyph('')).also { it.showLabelInSimplified = true })
}

@Suppress("LongMethod", "CyclomaticComplexMethod") // Declaratively builds the [Home] tab's groups from top to bottom (only the scope functions count as branches)
private fun createHomeTab(): RibbonTabModel = tab(
    "home",
    "Home",
    "H",
    group(
        "clipboard",
        "Clipboard",
        split("pp.paste", "Paste", glyph(''), LARGE, menuItem("pp.paste.keep", "Keep Source Formatting"), menuItem("pp.paste.picture", "Picture"), menuItem("pp.paste.text", "Keep Text Only")).alwaysLarge().pinned(),
        button("pp.cut", "Cut", glyph('')),
        button("pp.copy", "Copy", glyph('')),
        toggle("pp.formatPainter", "Format Painter", glyph(''), MEDIUM).overflow(),
    ).withIcon(glyph('\uE77F')),
    group(
        "slides",
        "Slides",
        split("pp.newSlide", "New Slide", glyph(''), LARGE, menuItem("pp.newSlide.title", "Title Slide"), menuItem("pp.newSlide.content", "Title and Content"), menuItem("pp.newSlide.blank", "Blank"))
            .alwaysLarge()
            .also { it.shortcut = "Ctrl+M" },
        dropDown("pp.layout", "Layout", glyph(''), MEDIUM, menuItem("pp.layout.title", "Title Slide"), menuItem("pp.layout.twoContent", "Two Content")),
        button("pp.reset", "Reset", glyph('')).overflow(),
        dropDown("pp.section", "Section", glyph(''), MEDIUM, menuItem("pp.section.add", "Add Section")).overflow(),
    ).withIcon(glyph('\uE7F4')),
    group(
        "font",
        "Font",
        RibbonRowModel(
            RibbonFontComboBoxModel("pp.font").also {
                it.items.add(0, "Aptos Display")
                it.selectedItem = "Aptos Display"
            },
            RibbonFontSizeComboBoxModel("pp.fontSize").also { it.selectedItem = FONT_SIZE },
        ),
        RibbonRowModel(
            RibbonButtonGroupModel(
                toggle("pp.bold", "Bold", RibbonIcons.BOLD).also { it.shortcut = "Ctrl+B" },
                toggle("pp.italic", "Italic", RibbonIcons.ITALIC).also { it.shortcut = "Ctrl+I" },
                toggle("pp.underline", "Underline", RibbonIcons.UNDERLINE).also { it.shortcut = "Ctrl+U" },
            ),
            RibbonColorPickerModel("pp.fontColor", "Font Color", RibbonIcons.FONT_COLOR).also { it.selectedColor = RibbonColor.parse("#C43E1C") },
        ),
    ).also {
        it.icon = glyph('\uE8D2')
        it.itemsLayout = RibbonGroupItemsLayout.ROWS
        it.rowCount = 2
    },
    group(
        "paragraph",
        "Paragraph",
        split("pp.bullets", "Bullets", RibbonIcons.BULLETS, SMALL, menuItem("pp.bullets.dot", "• Filled Round Bullets"), menuItem("pp.bullets.square", "■ Filled Square Bullets")).also { it.isCheckable = true },
        RibbonButtonGroupModel(
            toggle("pp.left", "Align Left", RibbonIcons.ALIGN_LEFT).also {
                it.groupName = "ppalign"
                it.isChecked = true
            },
            toggle("pp.center", "Center", RibbonIcons.ALIGN_CENTER).also { it.groupName = "ppalign" },
            toggle("pp.right", "Align Right", RibbonIcons.ALIGN_RIGHT).also { it.groupName = "ppalign" },
        ),
        dropDown("pp.textDirection", "Text Direction", glyph(''), MEDIUM, menuItem("pp.textDirection.horizontal", "Horizontal"), menuItem("pp.textDirection.vertical", "Stacked")).overflow(),
    ).withIcon(RibbonIcons.BULLETS),
    group(
        "drawing",
        "Drawing",
        dropDown("pp.shapes", "Shapes", glyph(''), LARGE, menuItem("pp.shapes.rect", "Rectangle"), menuItem("pp.shapes.oval", "Oval"), menuItem("pp.shapes.arrow", "Arrow")).alwaysLarge(),
        dropDown("pp.arrange", "Arrange", glyph(''), MEDIUM, menuItem("pp.arrange.front", "Bring to Front"), menuItem("pp.arrange.back", "Send to Back")),
        RibbonColorPickerModel("pp.shapeFill", "Shape Fill", glyph('')).also { it.selectedColor = RibbonColor.parse("#C43E1C") },
        RibbonColorPickerModel("pp.shapeOutline", "Shape Outline", glyph('')).also { it.selectedColor = RibbonColor.parse("#404040") },
    ).withIcon(glyph('\uE91B')),
    group("designer", "Designer", button("pp.designer", "Designer", glyph(''), LARGE).pinned()).withIcon(glyph('')),
)

private fun createInsertTab(): RibbonTabModel = tab(
    "insert",
    "Insert",
    "N",
    group("insertSlides", "Slides", split("pp.newSlide2", "New Slide", glyph(''), LARGE, menuItem("pp.newSlide2.title", "Title Slide")).alwaysLarge()),
    group("insertTables", "Tables", dropDown("pp.table", "Table", glyph(''), LARGE, menuItem("pp.table.insert", "Insert Table..."))),
    group(
        "images",
        "Images",
        dropDown("pp.pictures", "Pictures", glyph(''), LARGE, menuItem("pp.pictures.device", "This Device..."), menuItem("pp.pictures.stock", "Stock Images...")),
        button("pp.screenshot", "Screenshot", glyph('')),
        button("pp.photoAlbum", "Photo Album", glyph('')),
    ),
    group(
        "pp.illustrations",
        "Illustrations",
        dropDown("pp.shapes2", "Shapes", glyph(''), LARGE, menuItem("pp.shapes2.rect", "Rectangle")),
        button("pp.icons", "Icons", glyph(''), LARGE),
        button("pp.chart", "Chart", glyph('')),
    ),
    group(
        "pp.media",
        "Media",
        dropDown("pp.video", "Video", glyph(''), LARGE, menuItem("pp.video.device", "This Device...")),
        dropDown("pp.audio", "Audio", glyph(''), LARGE, menuItem("pp.audio.record", "Record Audio...")),
    ),
)

private fun createDesignTab(): RibbonTabModel = tab(
    "design",
    "Design",
    "G",
    group(
        "themes",
        "Themes",
        RibbonGalleryModel("pp.themes", "Themes").also { gallery ->
            gallery.icon = glyph('\uE790')
            gallery.maxColumns = THEME_MAX_COLUMNS
            gallery.minColumns = MIN_COLUMNS
            gallery.itemWidth = THEME_WIDTH
            gallery.itemHeight = THEME_HEIGHT
            gallery.showLabels = false
            for (theme in THEMES) {
                gallery.items.add(
                    RibbonGalleryItemModel(theme.name, theme.name, value = theme).also {
                        it.previewText = "Aa"
                        it.previewFontSize = THEME_FONT_SIZE
                        it.previewBackground = RibbonColor.parse(theme.background)
                        it.previewForeground = RibbonColor.parse(theme.foreground)
                    },
                )
            }
            gallery.selectedItem = gallery.items[0]
        },
    ).also { it.reductionOrder = 1 },
    group(
        "customize",
        "Customize",
        dropDown("pp.slideSize", "Slide Size", glyph(''), LARGE, menuItem("pp.slideSize.standard", "Standard (4:3)"), menuItem("pp.slideSize.wide", "Widescreen (16:9)")),
        button("pp.formatBackground", "Format Background", glyph(''), LARGE),
    ),
)

private fun createTransitionsTab(): RibbonTabModel = tab(
    "transitions",
    "Transitions",
    "K",
    group("preview", "Preview", button("pp.previewTransition", "Preview", glyph(''), LARGE)),
    group(
        "transitionGallery",
        "Transition to This Slide",
        RibbonGalleryModel("pp.transitions", "Transitions").also { gallery ->
            gallery.icon = glyph('\uE8AB')
            gallery.maxColumns = TRANSITION_MAX_COLUMNS
            gallery.minColumns = MIN_COLUMNS
            gallery.itemWidth = TRANSITION_SIZE
            gallery.itemHeight = TRANSITION_SIZE
            for ((label, code) in TRANSITIONS) gallery.items.add(RibbonGalleryItemModel("transition.$label", label, glyph(code)))
            gallery.selectedItem = gallery.items[0]
        },
        dropDown("pp.effectOptions", "Effect Options", glyph(''), LARGE, menuItem("pp.effect.smoothly", "Smoothly"), menuItem("pp.effect.black", "Through Black")),
    ).also { it.reductionOrder = 1 },
    group(
        "timing",
        "Timing",
        RibbonSpinnerModel("pp.duration", "Duration:", DURATION).also {
            it.maximum = DURATION_MAX
            it.increment = DURATION_STEP
            it.format = "00.00"
        },
        RibbonCheckBoxModel("pp.onMouseClick", "On Mouse Click", isChecked = true),
        button("pp.applyToAll", "Apply To All", glyph('')),
    ),
)

private fun createSlideShowTab(): RibbonTabModel = tab(
    "slideShow",
    "Slide Show",
    "S",
    group(
        "start",
        "Start Slide Show",
        button("pp.fromBeginning2", "From Beginning", glyph(''), LARGE).also { it.shortcut = "F5" },
        button("pp.fromCurrent", "From Current Slide", glyph(''), LARGE).also { it.shortcut = "Shift+F5" },
    ),
    group(
        "setUp",
        "Set Up",
        button("pp.setUpShow", "Set Up Slide Show", glyph(''), LARGE),
        toggle("pp.hideSlide", "Hide Slide", glyph(''), LARGE),
        RibbonCheckBoxModel("pp.useTimings", "Use Timings", isChecked = true),
    ),
)

private fun createViewTab(): RibbonTabModel = tab(
    "view",
    "View",
    "W",
    group(
        "presentationViews",
        "Presentation Views",
        toggle("pp.normal", "Normal", glyph(''), LARGE).also {
            it.groupName = "ppview"
            it.isChecked = true
        },
        toggle("pp.sorter", "Slide Sorter", glyph(''), LARGE).also { it.groupName = "ppview" },
        toggle("pp.notesPage", "Notes Page", glyph(''), LARGE).also { it.groupName = "ppview" },
    ),
    group(
        "ppShow",
        "Show",
        RibbonCheckBoxModel("pp.ruler", "Ruler"),
        RibbonCheckBoxModel("pp.gridlines", "Gridlines"),
        RibbonCheckBoxModel("pp.guides", "Guides"),
    ),
)

private fun RibbonGroupModel.withIcon(icon: RibbonIcon): RibbonGroupModel = also { it.icon = icon }

// Items in the [Transitions] gallery (label and glyph)
private val TRANSITIONS = listOf(
    "None" to '',
    "Morph" to '',
    "Fade" to '',
    "Push" to '',
    "Wipe" to '',
    "Split" to '',
    "Reveal" to '',
    "Cut" to '',
    "Random Bars" to '',
)

private const val FONT_SIZE = 28.0
private const val MIN_COLUMNS = 3
private const val THEME_MAX_COLUMNS = 6
private const val THEME_WIDTH = 80.0
private const val THEME_HEIGHT = 56.0
private const val THEME_FONT_SIZE = 20.0
private const val TRANSITION_MAX_COLUMNS = 7
private const val TRANSITION_SIZE = 60.0
private const val DURATION = 2.0
private const val DURATION_MAX = 59.0
private const val DURATION_STEP = 0.25
