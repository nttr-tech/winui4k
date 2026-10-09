package com.appkitbox.winui4k.sample.ribbon.word

import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.shell.button
import com.appkitbox.winui4k.sample.ribbon.shell.dropDown
import com.appkitbox.winui4k.sample.ribbon.shell.group
import com.appkitbox.winui4k.sample.ribbon.shell.menuItem
import com.appkitbox.winui4k.sample.ribbon.shell.split
import com.appkitbox.winui4k.sample.ribbon.shell.tab

// Word's contextual tabs: [Table Design] and [Layout] under [Table Tools] (same layout as WordPage.xaml), and
// [Picture Format] under [Picture Tools] (the RibbonSpace demo only declares the group without a tab, so this was added to match Word)

/** Table styles (gallery items; the value is the color of the header row of the table in the document). */
internal val TABLE_STYLES: List<Pair<String, String>> = listOf(
    "Plain Table 1" to "#F2F2F2",
    "Grid Table 1 Light" to "#DEEBF7",
    "Grid Table 4 - Accent 1" to "#4472C4",
    "Grid Table 4 - Accent 2" to "#ED7D31",
    "Grid Table 4 - Accent 6" to "#70AD47",
    "List Table 3" to "#A5A5A5",
    "List Table 7 Colorful" to "#FFC000",
    "Grid Table 5 Dark" to "#44546A",
)

/** [Table Design] under [Table Tools]. */
internal fun createTableDesignTab(): RibbonTabModel = tab(
    "tableDesign",
    "Table Design",
    "JT",
    group(
        "tableStyleOptions",
        "Table Style Options",
        RibbonCheckBoxModel("headerRow", "Header Row", isChecked = true),
        RibbonCheckBoxModel("totalRow", "Total Row"),
        RibbonCheckBoxModel("bandedRows", "Banded Rows", isChecked = true),
        RibbonCheckBoxModel("firstColumn", "First Column", isChecked = true),
        RibbonCheckBoxModel("lastColumn", "Last Column"),
        RibbonCheckBoxModel("bandedColumns", "Banded Columns"),
    ),
    group(
        "tableStyles",
        "Table Styles",
        RibbonGalleryModel("tableStylesGallery", "Table Styles").also { gallery ->
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
        RibbonColorPickerModel("tableShading", "Shading", RibbonIcons.SHADING).also {
            it.size = RibbonItemSize.LARGE
            it.selectedColor = RibbonColor.parse("#DEEBF7")
            it.showNoColor = true
        },
    ).also { it.reductionOrder = 1 },
    group(
        "tableBorders",
        "Borders",
        button("borderStyles", "Border Styles", RibbonIcons.BORDERS, RibbonItemSize.LARGE),
        split("tableBordersButton", "Borders", RibbonIcons.BORDERS, RibbonItemSize.LARGE, menuItem("tableBorders.all", "All Borders"), menuItem("tableBorders.outside", "Outside Borders"), menuItem("tableBorders.none", "No Border")),
        button("borderPainter", "Border Painter", RibbonIcons.FORMAT_PAINTER, RibbonItemSize.LARGE),
    ),
).also { it.contextualGroupId = TABLE_TOOLS }

/** [Layout] under [Table Tools]. */
internal fun createTableLayoutTab(): RibbonTabModel = tab(
    "tableLayout",
    "Layout",
    "JL",
    group(
        "rowsColumns",
        "Rows & Columns",
        button("deleteRows", "Delete", glyph(''), RibbonItemSize.LARGE),
        button("insertAbove", "Insert Above", glyph(''), RibbonItemSize.LARGE),
        button("insertBelow", "Insert Below", glyph(''), RibbonItemSize.LARGE),
        button("insertLeft", "Insert Left", glyph('')),
        button("insertRight", "Insert Right", glyph('')),
    ),
    group(
        "cellSize",
        "Cell Size",
        spinner("rowHeight", "Height:", ROW_HEIGHT, CELL_MAX, CM_STEP, "cm", "0.00"),
        spinner("columnWidth", "Width:", COLUMN_WIDTH, CELL_MAX, CM_STEP, "cm", "0.00"),
        button("autoFit", "AutoFit", glyph('')),
    ).also { it.isDialogLauncherVisible = true },
).also { it.contextualGroupId = TABLE_TOOLS }

/** [Picture Format] under [Picture Tools]. */
internal fun createPictureFormatTab(): RibbonTabModel = tab(
    "pictureFormat",
    "Picture Format",
    "JP",
    group(
        "adjust",
        "Adjust",
        button("removeBackground", "Remove Background", glyph(''), RibbonItemSize.LARGE),
        dropDown("corrections", "Corrections", glyph(''), RibbonItemSize.MEDIUM, menuItem("corrections.sharpen", "Sharpen: 25%"), menuItem("corrections.brightness", "Brightness: +20%")),
        dropDown("pictureColor", "Color", glyph(''), RibbonItemSize.MEDIUM, menuItem("pictureColor.grayscale", "Grayscale"), menuItem("pictureColor.sepia", "Sepia")),
        dropDown("artisticEffects", "Artistic Effects", glyph(''), RibbonItemSize.MEDIUM, menuItem("artistic.pencil", "Pencil Sketch"), menuItem("artistic.blur", "Blur")),
    ),
    group(
        "pictureStyles",
        "Picture Styles",
        RibbonColorPickerModel("pictureBorder", "Picture Border", RibbonIcons.BORDERS).also {
            it.size = RibbonItemSize.MEDIUM
            it.selectedColor = RibbonColor.parse("#4472C4")
            it.showNoColor = true
        },
        dropDown("pictureEffects", "Picture Effects", glyph(''), RibbonItemSize.MEDIUM, menuItem("effects.shadow", "Shadow"), menuItem("effects.glow", "Glow"), menuItem("effects.soft", "Soft Edges")),
    ),
    group(
        "pictureSize",
        "Size",
        button("crop", "Crop", glyph(''), RibbonItemSize.LARGE),
        spinner("pictureHeight", "Height:", PICTURE_HEIGHT, PICTURE_MAX, CM_STEP, "cm", "0.00"),
        spinner("pictureWidth", "Width:", PICTURE_WIDTH, PICTURE_MAX, CM_STEP, "cm", "0.00"),
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
