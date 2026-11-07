package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.common.button
import com.appkitbox.winui4k.sample.ribbon.common.dropDown
import com.appkitbox.winui4k.sample.ribbon.common.group
import com.appkitbox.winui4k.sample.ribbon.common.menuItem
import com.appkitbox.winui4k.sample.ribbon.common.split
import com.appkitbox.winui4k.sample.ribbon.common.tab

// Word's contextual tabs: [Table Design] and [Layout] under [Table Tools] (same layout as WordPage.xaml), and
// [Picture Format] under [Picture Tools] (the RibbonSpace demo only declares the group without a tab, so this was added to match Word)

/** Table styles (gallery items; the value is the color of the header row of the table in the document). */
internal val TABLE_STYLES: List<Pair<String, String>> = listOf(
    Messages.get("table.style.plain1") to "#F2F2F2",
    Messages.get("table.style.grid1.light") to "#DEEBF7",
    Messages.get("table.style.grid4.accent1") to "#4472C4",
    Messages.get("table.style.grid4.accent2") to "#ED7D31",
    Messages.get("table.style.grid4.accent6") to "#70AD47",
    Messages.get("table.style.list3") to "#A5A5A5",
    Messages.get("table.style.list7.colorful") to "#FFC000",
    Messages.get("table.style.grid5.dark") to "#44546A",
)

/** [Table Design] under [Table Tools]. */
internal fun createTableDesignTab(): RibbonTabModel = tab(
    "tableDesign",
    Messages.get("table.design"),
    "JT",
    group(
        "tableStyleOptions",
        Messages.get("table.style.options"),
        RibbonCheckBoxModel("headerRow", Messages.get("table.header.row"), isChecked = true),
        RibbonCheckBoxModel("totalRow", Messages.get("table.total.row")),
        RibbonCheckBoxModel("bandedRows", Messages.get("table.banded.rows"), isChecked = true),
        RibbonCheckBoxModel("firstColumn", Messages.get("table.first.column"), isChecked = true),
        RibbonCheckBoxModel("lastColumn", Messages.get("table.last.column")),
        RibbonCheckBoxModel("bandedColumns", Messages.get("table.banded.columns")),
    ),
    group(
        "tableStyles",
        Messages.get("table.styles"),
        RibbonGalleryModel("tableStylesGallery", Messages.get("table.styles")).also { gallery ->
            gallery.maxColumns = TABLE_STYLE_MAX_COLUMNS
            gallery.minColumns = TABLE_STYLE_MIN_COLUMNS
            gallery.itemWidth = TABLE_STYLE_ITEM_WIDTH
            gallery.itemHeight = TABLE_STYLE_ITEM_HEIGHT
            gallery.showLabels = false
            TABLE_STYLES.forEachIndexed { index, (label, color) ->
                gallery.items.add(RibbonGalleryItemModel("tableStyle$index", label, value = color).also { it.previewBackground = RibbonColor.parse(color) })
            }
            gallery.selectedItem = gallery.items[2]
        },
        RibbonColorPickerModel("tableShading", Messages.get("home.shading"), RibbonIcons.SHADING).also {
            it.size = RibbonItemSize.LARGE
            it.selectedColor = RibbonColor.parse("#DEEBF7")
            it.showNoColor = true
        },
    ).also { it.reductionOrder = 1 },
    group(
        "tableBorders",
        Messages.get("table.borders.group"),
        button("borderStyles", Messages.get("table.border.styles"), RibbonIcons.BORDERS, RibbonItemSize.LARGE),
        split("tableBordersButton", Messages.get("common.borders"), RibbonIcons.BORDERS, RibbonItemSize.LARGE, menuItem("tableBorders.all", Messages.get("common.borders.all")), menuItem("tableBorders.outside", Messages.get("common.borders.outside")), menuItem("tableBorders.none", Messages.get("common.borders.none"))),
        button("borderPainter", Messages.get("table.border.painter"), RibbonIcons.FORMAT_PAINTER, RibbonItemSize.LARGE),
    ),
).also { it.contextualGroupId = TABLE_TOOLS }

/** [Layout] under [Table Tools]. */
internal fun createTableLayoutTab(): RibbonTabModel = tab(
    "tableLayout",
    Messages.get("common.layout"),
    "JL",
    group(
        "rowsColumns",
        Messages.get("table.rows.columns"),
        button("deleteRows", Messages.get("common.delete"), glyph('\uE74D'), RibbonItemSize.LARGE),
        button("insertAbove", Messages.get("table.insert.above"), glyph('\uE710'), RibbonItemSize.LARGE),
        button("insertBelow", Messages.get("table.insert.below"), glyph('\uE710'), RibbonItemSize.LARGE),
        button("insertLeft", Messages.get("table.insert.left"), glyph('\uE710')),
        button("insertRight", Messages.get("table.insert.right"), glyph('\uE710')),
    ),
    group(
        "cellSize",
        Messages.get("table.cell.size"),
        spinner("rowHeight", Messages.get("size.height"), ROW_HEIGHT, CELL_MAX, CM_STEP, "cm", "0.00"),
        spinner("columnWidth", Messages.get("size.width"), COLUMN_WIDTH, CELL_MAX, CM_STEP, "cm", "0.00"),
        button("autoFit", Messages.get("table.auto.fit"), glyph('\uE740')),
    ).also { it.isDialogLauncherVisible = true },
).also { it.contextualGroupId = TABLE_TOOLS }

/** [Picture Format] under [Picture Tools]. */
internal fun createPictureFormatTab(): RibbonTabModel = tab(
    "pictureFormat",
    Messages.get("picture.format"),
    "JP",
    group(
        "adjust",
        Messages.get("picture.adjust"),
        button("removeBackground", Messages.get("picture.remove.background"), glyph('\uE75C'), RibbonItemSize.LARGE),
        dropDown("corrections", Messages.get("picture.corrections"), glyph('\uE706'), RibbonItemSize.MEDIUM, menuItem("corrections.sharpen", Messages.get("picture.sharpen")), menuItem("corrections.brightness", Messages.get("picture.brightness"))),
        dropDown("pictureColor", Messages.get("picture.color"), glyph('\uE790'), RibbonItemSize.MEDIUM, menuItem("pictureColor.grayscale", Messages.get("picture.grayscale")), menuItem("pictureColor.sepia", Messages.get("picture.sepia"))),
        dropDown("artisticEffects", Messages.get("picture.artistic.effects"), glyph('\uE771'), RibbonItemSize.MEDIUM, menuItem("artistic.pencil", Messages.get("picture.pencil.sketch")), menuItem("artistic.blur", Messages.get("picture.blur"))),
    ),
    group(
        "pictureStyles",
        Messages.get("picture.styles"),
        RibbonColorPickerModel("pictureBorder", Messages.get("picture.border"), RibbonIcons.BORDERS).also {
            it.size = RibbonItemSize.MEDIUM
            it.selectedColor = RibbonColor.parse("#4472C4")
            it.showNoColor = true
        },
        dropDown("pictureEffects", Messages.get("picture.effects"), glyph('\uE9D2'), RibbonItemSize.MEDIUM, menuItem("effects.shadow", Messages.get("picture.shadow")), menuItem("effects.glow", Messages.get("picture.glow")), menuItem("effects.soft", Messages.get("picture.soft.edges"))),
    ),
    group(
        "pictureSize",
        Messages.get("layout.size"),
        button("crop", Messages.get("picture.crop"), glyph('\uE7A8'), RibbonItemSize.LARGE),
        spinner("pictureHeight", Messages.get("size.height"), PICTURE_HEIGHT, PICTURE_MAX, CM_STEP, "cm", "0.00"),
        spinner("pictureWidth", Messages.get("size.width"), PICTURE_WIDTH, PICTURE_MAX, CM_STEP, "cm", "0.00"),
    ).also { it.isDialogLauncherVisible = true },
).also { it.contextualGroupId = PICTURE_TOOLS }

private const val TABLE_STYLE_MAX_COLUMNS = 7
private const val TABLE_STYLE_MIN_COLUMNS = 3
private const val TABLE_STYLE_ITEM_WIDTH = 60.0
private const val TABLE_STYLE_ITEM_HEIGHT = 48.0
private const val ROW_HEIGHT = 0.5
private const val COLUMN_WIDTH = 4.2
private const val CELL_MAX = 55.0
private const val CM_STEP = 0.1
private const val PICTURE_HEIGHT = 6.0
private const val PICTURE_WIDTH = 9.0
private const val PICTURE_MAX = 55.0
