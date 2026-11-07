package com.appkitbox.winui4k.sample.ribbon.powerpoint

import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDisplayMode
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontSizeComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRowModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSimplifiedVisibility
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSizeDefinition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
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
    SlideTheme(Messages.get("theme.office"), "#FFFFFF", "#262626"),
    SlideTheme(Messages.get("theme.facet"), "#90C226", "#FFFFFF"),
    SlideTheme(Messages.get("theme.ion"), "#1E5155", "#FFFFFF"),
    SlideTheme(Messages.get("theme.retrospect"), "#E48312", "#FFFFFF"),
    SlideTheme(Messages.get("theme.slice"), "#146194", "#FFFFFF"),
    SlideTheme(Messages.get("theme.wisp"), "#EFE3C6", "#5E3A1B"),
    SlideTheme(Messages.get("theme.berlin"), "#A6452E", "#FFFFFF"),
    SlideTheme(Messages.get("theme.dividend"), "#4D1434", "#FFFFFF"),
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
    applicationButtonLabel = Messages.get("common.file")
    title = Messages.get("title")
    tabs.add(createHomeTab())
    tabs.add(createInsertTab())
    tabs.add(createDesignTab())
    tabs.add(createTransitionsTab())
    tabs.add(createSlideShowTab())
    tabs.add(createViewTab())
    quickAccessItems.add(button("pp.save", Messages.get("common.save"), glyph('\uE74E')))
    quickAccessItems.add(button("pp.undo", Messages.get("common.undo"), glyph('\uE7A7')))
    quickAccessItems.add(button("pp.fromBeginning", Messages.get("from.beginning"), glyph('\uE768')).also { it.shortcut = "F5" })
    tabStripItems.add(button("pp.record", Messages.get("record"), glyph('\uE7C8')).also { it.showLabelInSimplified = true })
    tabStripItems.add(split("pp.present", Messages.get("present"), glyph('\uE7F4'), SMALL, menuItem("pp.present.beginning", Messages.get("from.beginning")), menuItem("pp.present.current", Messages.get("from.current"))).also { it.showLabelInSimplified = true })
    tabStripItems.add(button("pp.share", Messages.get("common.share"), glyph('\uE72D')).also { it.showLabelInSimplified = true })
}

@Suppress("LongMethod", "CyclomaticComplexMethod") // Declaratively builds the [Home] tab's groups from top to bottom (only the scope functions count as branches)
private fun createHomeTab(): RibbonTabModel = tab(
    "home",
    Messages.get("common.home"),
    "H",
    group(
        "clipboard",
        Messages.get("common.clipboard"),
        split("pp.paste", Messages.get("common.paste"), glyph('\uE77F'), LARGE, menuItem("pp.paste.keep", Messages.get("common.paste.keep")), menuItem("pp.paste.picture", Messages.get("home.paste.picture")), menuItem("pp.paste.text", Messages.get("common.paste.text"))).alwaysLarge().pinned(),
        button("pp.cut", Messages.get("common.cut"), glyph('\uE8C6')),
        button("pp.copy", Messages.get("common.copy"), glyph('\uE8C8')),
        toggle("pp.formatPainter", Messages.get("common.format.painter"), glyph('\uE771'), MEDIUM).overflow(),
    ).withIcon(glyph('\uE77F')),
    group(
        "slides",
        Messages.get("home.slides"),
        split("pp.newSlide", Messages.get("home.new.slide"), glyph('\uE710'), LARGE, menuItem("pp.newSlide.title", Messages.get("home.title.slide")), menuItem("pp.newSlide.content", Messages.get("home.title.content")), menuItem("pp.newSlide.blank", Messages.get("home.blank")))
            .alwaysLarge()
            .also { it.shortcut = "Ctrl+M" },
        dropDown("pp.layout", Messages.get("common.layout"), glyph('\uE8A9'), MEDIUM, menuItem("pp.layout.title", Messages.get("home.title.slide")), menuItem("pp.layout.twoContent", Messages.get("home.two.content"))),
        button("pp.reset", Messages.get("home.reset"), glyph('\uE72C')).overflow(),
        dropDown("pp.section", Messages.get("home.section"), glyph('\uE8FD'), MEDIUM, menuItem("pp.section.add", Messages.get("home.section.add"))).overflow(),
    ).withIcon(glyph('\uE7F4')),
    group(
        "font",
        Messages.get("common.font"),
        RibbonRowModel(
            RibbonFontComboBoxModel("pp.font").also {
                it.items.add(0, "Aptos Display")
                it.selectedItem = "Aptos Display"
            },
            RibbonFontSizeComboBoxModel("pp.fontSize").also { it.selectedItem = FONT_SIZE },
        ),
        RibbonRowModel(
            RibbonButtonGroupModel(
                toggle("pp.bold", Messages.get("common.bold"), RibbonIcons.BOLD).also { it.shortcut = "Ctrl+B" },
                toggle("pp.italic", Messages.get("common.italic"), RibbonIcons.ITALIC).also { it.shortcut = "Ctrl+I" },
                toggle("pp.underline", Messages.get("common.underline"), RibbonIcons.UNDERLINE).also { it.shortcut = "Ctrl+U" },
            ),
            RibbonColorPickerModel("pp.fontColor", Messages.get("common.font.color"), RibbonIcons.FONT_COLOR).also { it.selectedColor = RibbonColor.parse("#C43E1C") },
        ),
    ).also {
        it.icon = glyph('\uE8D2')
        it.itemsLayout = RibbonGroupItemsLayout.ROWS
        it.rowCount = 2
    },
    group(
        "paragraph",
        Messages.get("common.paragraph"),
        split("pp.bullets", Messages.get("common.bullets"), RibbonIcons.BULLETS, SMALL, menuItem("pp.bullets.dot", Messages.get("common.bullets.dot")), menuItem("pp.bullets.square", Messages.get("common.bullets.square"))).also { it.isCheckable = true },
        RibbonButtonGroupModel(
            toggle("pp.left", Messages.get("common.align.left"), RibbonIcons.ALIGN_LEFT).also {
                it.groupName = "ppalign"
                it.isChecked = true
            },
            toggle("pp.center", Messages.get("common.align.center"), RibbonIcons.ALIGN_CENTER).also { it.groupName = "ppalign" },
            toggle("pp.right", Messages.get("common.align.right"), RibbonIcons.ALIGN_RIGHT).also { it.groupName = "ppalign" },
        ),
        dropDown("pp.textDirection", Messages.get("home.text.direction"), glyph('\uE8D2'), MEDIUM, menuItem("pp.textDirection.horizontal", Messages.get("home.horizontal")), menuItem("pp.textDirection.vertical", Messages.get("home.vertical"))).overflow(),
    ).withIcon(RibbonIcons.BULLETS),
    group(
        "drawing",
        Messages.get("home.drawing"),
        dropDown("pp.shapes", Messages.get("common.shapes"), glyph('\uE91B'), LARGE, menuItem("pp.shapes.rect", Messages.get("common.shape.rectangle")), menuItem("pp.shapes.oval", Messages.get("common.shape.oval")), menuItem("pp.shapes.arrow", Messages.get("home.arrow"))).alwaysLarge(),
        dropDown("pp.arrange", Messages.get("home.arrange"), glyph('\uE8F4'), MEDIUM, menuItem("pp.arrange.front", Messages.get("common.bring.to.front")), menuItem("pp.arrange.back", Messages.get("common.send.to.back"))),
        RibbonColorPickerModel("pp.shapeFill", Messages.get("home.shape.fill"), glyph('\uE790')).also { it.selectedColor = RibbonColor.parse("#C43E1C") },
        RibbonColorPickerModel("pp.shapeOutline", Messages.get("home.shape.outline"), glyph('\uE80A')).also { it.selectedColor = RibbonColor.parse("#404040") },
    ).withIcon(glyph('\uE91B')),
    group("designer", Messages.get("home.designer"), button("pp.designer", Messages.get("home.designer"), glyph('\uE790'), LARGE).pinned()).withIcon(glyph('\uE790')),
)

private fun createInsertTab(): RibbonTabModel = tab(
    "insert",
    Messages.get("common.insert"),
    "N",
    group("insertSlides", Messages.get("home.slides"), split("pp.newSlide2", Messages.get("home.new.slide"), glyph('\uE710'), LARGE, menuItem("pp.newSlide2.title", Messages.get("home.title.slide"))).alwaysLarge()),
    group("insertTables", Messages.get("insert.tables"), dropDown("pp.table", Messages.get("common.table"), glyph('\uE80A'), LARGE, menuItem("pp.table.insert", Messages.get("common.insert.table")))),
    group(
        "images",
        Messages.get("insert.images"),
        dropDown("pp.pictures", Messages.get("common.pictures"), glyph('\uEB9F'), LARGE, menuItem("pp.pictures.device", Messages.get("common.pictures.device")), menuItem("pp.pictures.stock", Messages.get("insert.stock.images"))),
        button("pp.screenshot", Messages.get("common.screenshot"), glyph('\uE722')),
        button("pp.photoAlbum", Messages.get("insert.photo.album"), glyph('\uE8F1')),
    ),
    group(
        "pp.illustrations",
        Messages.get("common.illustrations"),
        dropDown("pp.shapes2", Messages.get("common.shapes"), glyph('\uE91B'), LARGE, menuItem("pp.shapes2.rect", Messages.get("common.shape.rectangle"))),
        button("pp.icons", Messages.get("common.icons"), glyph('\uED58'), LARGE),
        button("pp.chart", Messages.get("common.chart"), glyph('\uE9D2')),
    ),
    group(
        "pp.media",
        Messages.get("common.media"),
        dropDown("pp.video", Messages.get("insert.video"), glyph('\uE714'), LARGE, menuItem("pp.video.device", Messages.get("common.pictures.device"))),
        dropDown("pp.audio", Messages.get("insert.audio"), glyph('\uE8D6'), LARGE, menuItem("pp.audio.record", Messages.get("insert.audio.record"))),
    ),
)

private fun createDesignTab(): RibbonTabModel = tab(
    "design",
    Messages.get("common.design"),
    "G",
    group(
        "themes",
        Messages.get("design.themes"),
        RibbonGalleryModel("pp.themes", Messages.get("design.themes")).also { gallery ->
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
        Messages.get("design.customize"),
        dropDown("pp.slideSize", Messages.get("design.slide.size"), glyph('\uE740'), LARGE, menuItem("pp.slideSize.standard", Messages.get("design.slide.size.standard")), menuItem("pp.slideSize.wide", Messages.get("design.slide.size.wide"))),
        button("pp.formatBackground", Messages.get("design.format.background"), glyph('\uE790'), LARGE),
    ),
)

private fun createTransitionsTab(): RibbonTabModel = tab(
    "transitions",
    Messages.get("transitions"),
    "K",
    group("preview", Messages.get("transitions.preview"), button("pp.previewTransition", Messages.get("transitions.preview"), glyph('\uE768'), LARGE)),
    group(
        "transitionGallery",
        Messages.get("transitions.this.slide"),
        RibbonGalleryModel("pp.transitions", Messages.get("transitions")).also { gallery ->
            gallery.icon = glyph('\uE8AB')
            gallery.maxColumns = TRANSITION_MAX_COLUMNS
            gallery.minColumns = MIN_COLUMNS
            gallery.itemWidth = TRANSITION_SIZE
            gallery.itemHeight = TRANSITION_SIZE
            for ((label, code) in TRANSITIONS) gallery.items.add(RibbonGalleryItemModel("transition.$label", label, glyph(code)))
            gallery.selectedItem = gallery.items[0]
        },
        dropDown("pp.effectOptions", Messages.get("transitions.effect.options"), glyph('\uE713'), LARGE, menuItem("pp.effect.smoothly", Messages.get("transitions.smoothly")), menuItem("pp.effect.black", Messages.get("transitions.through.black"))),
    ).also { it.reductionOrder = 1 },
    group(
        "timing",
        Messages.get("transitions.timing"),
        RibbonSpinnerModel("pp.duration", Messages.get("transitions.duration"), DURATION).also {
            it.maximum = DURATION_MAX
            it.increment = DURATION_STEP
            it.format = "00.00"
        },
        RibbonCheckBoxModel("pp.onMouseClick", Messages.get("transitions.on.click"), isChecked = true),
        button("pp.applyToAll", Messages.get("transitions.apply.all"), glyph('\uE8B3')),
    ),
)

private fun createSlideShowTab(): RibbonTabModel = tab(
    "slideShow",
    Messages.get("slide.show"),
    "S",
    group(
        "start",
        Messages.get("slide.show.start"),
        button("pp.fromBeginning2", Messages.get("from.beginning"), glyph('\uE768'), LARGE).also { it.shortcut = "F5" },
        button("pp.fromCurrent", Messages.get("from.current"), glyph('\uE768'), LARGE).also { it.shortcut = "Shift+F5" },
    ),
    group(
        "setUp",
        Messages.get("slide.show.set.up"),
        button("pp.setUpShow", Messages.get("slide.show.set.up.show"), glyph('\uE713'), LARGE),
        toggle("pp.hideSlide", Messages.get("slide.show.hide.slide"), glyph('\uE890'), LARGE),
        RibbonCheckBoxModel("pp.useTimings", Messages.get("slide.show.use.timings"), isChecked = true),
    ),
)

private fun createViewTab(): RibbonTabModel = tab(
    "view",
    Messages.get("common.view"),
    "W",
    group(
        "presentationViews",
        Messages.get("view.presentation.views"),
        toggle("pp.normal", Messages.get("view.normal"), glyph('\uE7F4'), LARGE).also {
            it.groupName = "ppview"
            it.isChecked = true
        },
        toggle("pp.sorter", Messages.get("view.sorter"), glyph('\uE80A'), LARGE).also { it.groupName = "ppview" },
        toggle("pp.notesPage", Messages.get("view.notes.page"), glyph('\uE70B'), LARGE).also { it.groupName = "ppview" },
    ),
    group(
        "ppShow",
        Messages.get("view.show"),
        RibbonCheckBoxModel("pp.ruler", Messages.get("common.ruler")),
        RibbonCheckBoxModel("pp.gridlines", Messages.get("common.gridlines")),
        RibbonCheckBoxModel("pp.guides", Messages.get("view.guides")),
    ),
)

private fun RibbonGroupModel.withIcon(icon: RibbonIcon): RibbonGroupModel = also { it.icon = icon }

// Items in the [Transitions] gallery (label and glyph)
private val TRANSITIONS = listOf(
    Messages.get("transition.none") to '\uE711',
    Messages.get("transition.morph") to '\uE8AB',
    Messages.get("transition.fade") to '\uE706',
    Messages.get("transition.push") to '\uE76C',
    Messages.get("transition.wipe") to '\uE8B3',
    Messages.get("transition.split") to '\uE8A0',
    Messages.get("transition.reveal") to '\uE890',
    Messages.get("transition.cut") to '\uE8C6',
    Messages.get("transition.random.bars") to '\uE9D2',
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
