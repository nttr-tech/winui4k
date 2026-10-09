package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMetrics

/**
 * A component that places just the color picker palette on its own (RibbonSpace's RibbonColorPalette). Place it next to
 * a toolbar or in a dialog or panel. The model is [RibbonColorPickerModel] (automatic, theme colors, standard colors,
 * recent colors, no color, more colors). Choosing a color changes [RibbonColorPickerModel.selectedColor], runs the
 * command and notifies [addItemInvokedListener].
 */
class WRibbonColorPalette @JvmOverloads constructor(
    /** The palette's model. */
    val model: RibbonColorPickerModel = RibbonColorPickerModel(),
) : WRibbonBar("<Grid Background=\"{ThemeResource RibbonPopupBackgroundBrush}\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />") {
    private val host = RibbonStandaloneHost(this)
    private val view = RibbonColorPickerView(model, host, embedded = true)

    init {
        root.addChild(measureHost)
        view.attach()
        view.applyLayout(RibbonItemLayout(RibbonItemSize.LARGE, RibbonMetrics.COMFORTABLE, false, true))
        root.addChild(view.element)
    }

    override fun barScope(): List<RibbonItemModel> = listOf(model)

    /** The number of swatches (for tests). */
    internal val swatchCount: Int get() = view.swatchCount
}
