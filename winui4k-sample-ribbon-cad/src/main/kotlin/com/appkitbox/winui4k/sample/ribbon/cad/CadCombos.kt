package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.extension.ribbon.WRibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemRenderer
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemTextProvider

/** A drawing layer (name and color, on/freeze/lock state). */
data class CadLayer(val name: String, val color: WColor, val isOn: Boolean = true, val isFrozen: Boolean = false, val isLocked: Boolean = false) {
    override fun toString(): String = name
}

/** Drawing layers (CadLayer.Samples from the RibbonSpace CAD demo). */
val CAD_LAYERS: List<CadLayer> = listOf(
    CadLayer("0", WColor(0xF2, 0xF2, 0xF2)),
    CadLayer("A-WALL", WColor(0xF2, 0xF2, 0xF2)),
    CadLayer("A-DOOR", WColor(0xFF, 0xD5, 0x4F)),
    CadLayer("A-GLAZ", WColor(0x4D, 0xD0, 0xE1)),
    CadLayer("A-FURN", WColor(0x9C, 0xCC, 0x65), isLocked = true),
    CadLayer("A-ANNO-DIMS", WColor(0xEF, 0x53, 0x50)),
    CadLayer("A-ANNO-TEXT", WColor(0xF2, 0xF2, 0xF2)),
    CadLayer("A-HATCH", WColor(0x8C, 0x9A, 0xAB), isFrozen = true),
    CadLayer("A-AREA", WColor(0xB3, 0x88, 0xFF), isOn = false),
    CadLayer("CENTER", WColor(0xB3, 0x88, 0xFF)),
    CadLayer("DEFPOINTS", WColor(0xF2, 0xF2, 0xF2), isOn = false, isLocked = true),
)

/** A color option (ByLayer / ByBlock are marked with a diagonal mark; if [color] is null it is "Select Color..."). */
data class CadColorOption(val name: String, val color: WColor?, val isSpecial: Boolean = false) {
    override fun toString(): String = name
}

/** Color options. */
val CAD_COLORS: List<CadColorOption> = listOf(
    CadColorOption("ByLayer", WColor(0xF2, 0xF2, 0xF2), isSpecial = true),
    CadColorOption("ByBlock", WColor(0xF2, 0xF2, 0xF2), isSpecial = true),
    CadColorOption("Red", WColor(0xFF, 0x00, 0x00)),
    CadColorOption("Yellow", WColor(0xFF, 0xFF, 0x00)),
    CadColorOption("Green", WColor(0x00, 0xFF, 0x00)),
    CadColorOption("Cyan", WColor(0x00, 0xFF, 0xFF)),
    CadColorOption("Blue", WColor(0x00, 0x00, 0xFF)),
    CadColorOption("Magenta", WColor(0xFF, 0x00, 0xFF)),
    CadColorOption("White", WColor(0xFF, 0xFF, 0xFF)),
    CadColorOption("Select Color...", null),
)

/** A linetype ([dashes] repeats dash and gap lengths; null means a continuous line). */
data class CadLinetype(val name: String, val dashes: List<Double>?) {
    override fun toString(): String = name
}

/** Linetype options. */
val CAD_LINETYPES: List<CadLinetype> = listOf(
    CadLinetype("ByLayer", null),
    CadLinetype("ByBlock", null),
    CadLinetype("Continuous", null),
    CadLinetype("CENTER", listOf(8.0, 2.0, 2.0, 2.0)),
    CadLinetype("DASHED", listOf(4.0, 2.0)),
    CadLinetype("HIDDEN", listOf(2.0, 2.0)),
    CadLinetype("PHANTOM", listOf(9.0, 2.0, 2.0, 2.0, 2.0, 2.0)),
    CadLinetype("DOT", listOf(0.5, 2.0)),
)

/** A lineweight ([thickness] is the thickness of the sample line). */
data class CadLineweight(val name: String, val thickness: Double) {
    override fun toString(): String = name
}

/** Lineweight options. */
val CAD_LINEWEIGHTS: List<CadLineweight> = listOf(
    CadLineweight("ByLayer", 1.0),
    CadLineweight("ByBlock", 1.0),
    CadLineweight("Default", 1.0),
    CadLineweight("0.00 mm", 1.0),
    CadLineweight("0.13 mm", 1.0),
    CadLineweight("0.25 mm", 1.5),
    CadLineweight("0.35 mm", 2.0),
    CadLineweight("0.50 mm", 3.0),
    CadLineweight("0.70 mm", 4.0),
    CadLineweight("1.00 mm", 5.0),
)

/** The layer drop-down (as in AutoCAD, shows on/freeze/lock icons, a color swatch, and the name). */
internal fun layerCombo(): RibbonComboBoxModel = RibbonComboBoxModel("layerCombo", "Layer", CAD_LAYERS).also { combo ->
    combo.showLabel = false
    combo.selectedItem = CAD_LAYERS[1]
    combo.inputWidth = LAYER_WIDTH
    combo.itemTextProvider = RibbonItemTextProvider { (it as? CadLayer)?.name.orEmpty() }
    combo.itemRenderer = RibbonItemRenderer { item -> layerRow(item as CadLayer) }
    combo.screenTip = tip("Current layer: pick a layer to make it current, or toggle its on / freeze / lock state.")
}

/** The color combo box (ByLayer by default). */
internal fun colorCombo(id: String): RibbonComboBoxModel = RibbonComboBoxModel(id, "Color", CAD_COLORS).also { combo ->
    combo.showLabel = false
    combo.icon = ic(CadIcons.COLOR)
    combo.selectedItem = CAD_COLORS[0]
    combo.inputWidth = PROPERTY_WIDTH
    combo.itemTextProvider = RibbonItemTextProvider { (it as? CadColorOption)?.name.orEmpty() }
    combo.itemRenderer = RibbonItemRenderer { item -> colorRow(item as CadColorOption) }
    combo.screenTip = tip("Object Color: sets the color of selected objects (ByLayer by default).")
}

/** The linetype combo box (with linetype samples). */
internal fun linetypeCombo(id: String): RibbonComboBoxModel = RibbonComboBoxModel(id, "Linetype", CAD_LINETYPES).also { combo ->
    combo.showLabel = false
    combo.icon = ic(CadIcons.LINETYPE)
    combo.selectedItem = CAD_LINETYPES[0]
    combo.inputWidth = PROPERTY_WIDTH
    combo.itemTextProvider = RibbonItemTextProvider { (it as? CadLinetype)?.name.orEmpty() }
    combo.itemRenderer = RibbonItemRenderer { item -> sampleRow(linetypeIcon((item as CadLinetype).dashes), item.name) }
    combo.screenTip = tip("Linetype: sets the linetype of selected objects.")
}

/** The lineweight combo box (with lineweight samples). */
internal fun lineweightCombo(id: String): RibbonComboBoxModel = RibbonComboBoxModel(id, "Lineweight", CAD_LINEWEIGHTS).also { combo ->
    combo.showLabel = false
    combo.icon = ic(CadIcons.LINEWEIGHT)
    combo.selectedItem = CAD_LINEWEIGHTS[0]
    combo.inputWidth = PROPERTY_WIDTH
    combo.itemTextProvider = RibbonItemTextProvider { (it as? CadLineweight)?.name.orEmpty() }
    combo.itemRenderer = RibbonItemRenderer { item -> sampleRow(lineweightIcon((item as CadLineweight).thickness), item.name) }
    combo.screenTip = tip("Lineweight: sets the lineweight of selected objects.")
}

private fun layerRow(layer: CadLayer): WComponent {
    val row = WPanel(spacing = 3.0, orientation = Orientation.HORIZONTAL)
    row.add(WRibbonIcon(ic(if (layer.isOn) CadIcons.LAYER_ON else CadIcons.LAYER_OFF), ICON_SIZE))
    row.add(WRibbonIcon(ic(if (layer.isFrozen) CadIcons.LAYER_FREEZE else CadIcons.LAYER_THAW), ICON_SIZE))
    row.add(WRibbonIcon(ic(if (layer.isLocked) CadIcons.LAYER_LOCK else CadIcons.LAYER_UNLOCK), ICON_SIZE))
    row.add(swatch(layer.color).also { it.setMargin(3.0, 0.0, 5.0, 0.0) })
    row.add(WLabel(layer.name).also { it.verticalAlignment = VerticalAlignment.CENTER })
    return row
}

private fun colorRow(option: CadColorOption): WComponent {
    val row = WPanel(spacing = 6.0, orientation = Orientation.HORIZONTAL)
    if (option.color != null) {
        // ByLayer / ByBlock overlay a diagonal mark on the color swatch
        val mark = if (option.isSpecial) RibbonIcon.path("M0,14 L14,0 L14,14 Z", SWATCH_GRID, "#99808080") else null
        val box = swatch(option.color)
        box.child = mark?.let { WRibbonIcon(it, SWATCH) }
        row.add(box)
    } else {
        row.add(WBorder().also { it.width = SWATCH })
    }
    row.add(WLabel(option.name).also { it.verticalAlignment = VerticalAlignment.CENTER })
    return row
}

private fun sampleRow(sample: RibbonIcon, name: String): WComponent {
    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(WRibbonIcon(sample, SAMPLE_WIDTH))
    row.add(WLabel(name).also { it.verticalAlignment = VerticalAlignment.CENTER })
    return row
}

/** A linetype sample (a line of repeating dashes and gaps centered in a 40-wide grid). */
private fun linetypeIcon(dashes: List<Double>?): RibbonIcon {
    val data = if (dashes == null) {
        "M0,20 L40,20"
    } else {
        buildString {
            var x = 0.0
            var index = 0
            while (x < SAMPLE_WIDTH) {
                val length = dashes[index % dashes.size]
                if (index % 2 == 0) append("M${fmt(x)},20 L${fmt(minOf(SAMPLE_WIDTH, x + length))},20 ")
                x += length
                index++
            }
        }.trim()
    }
    return RibbonIcon.path("[viewbox=40;stroke=1.4]$data", SAMPLE_WIDTH)
}

/** A lineweight sample. */
private fun lineweightIcon(thickness: Double): RibbonIcon = RibbonIcon.path("[viewbox=40;stroke=${fmt(thickness)}]M0,20 L40,20", SAMPLE_WIDTH)

private fun swatch(color: WColor): WBorder = WBorder().also {
    it.width = SWATCH
    it.height = SWATCH
    it.background = color
    it.borderColor = WColor(0x80, 0x80, 0x80)
    it.borderThickness = 1.0
    it.verticalAlignment = VerticalAlignment.CENTER
}

private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private const val ICON_SIZE = 16.0
private const val SWATCH = 12.0
private const val SWATCH_GRID = 14.0
private const val SAMPLE_WIDTH = 40.0
private const val LAYER_WIDTH = 164.0
private const val PROPERTY_WIDTH = 122.0
