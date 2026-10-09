package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonList
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonStrings

/**
 * Lays out a list of item models ([items]) in a StackPanel (shared by the QAT, tab-row items, toolbars, and status bars).
 * Views are rebuilt incrementally as models are added or removed, and the layout is determined by [layoutOf].
 */
internal class RibbonItemStrip(
    private var items: RibbonList<RibbonItemModel>,
    private val host: RibbonItemHost,
    private val layoutOf: (RibbonItemView) -> RibbonItemLayout,
    vertical: Boolean = false,
    spacing: Double = 1.0,
) {
    /** The element to place. */
    val element: XamlElement = XamlElement.load(
        "<StackPanel Orientation=\"${if (vertical) "Vertical" else "Horizontal"}\" Spacing=\"${Xaml.num(spacing)}\" VerticalAlignment=\"Center\" />",
    )
    private val views = mutableListOf<RibbonItemView>()
    private val listener = RibbonListListener<RibbonItemModel> { sync() }
    private var attached = false

    /** The item views (in display order). */
    fun views(): List<RibbonItemView> = views.toList()

    /** Starts observing the model. */
    fun attach() {
        if (attached) return
        attached = true
        items.addListListener(listener)
        sync()
    }

    /** Stops observing the model and discards the views. */
    fun dispose() {
        if (!attached) return
        attached = false
        items.removeListListener(listener)
        views.forEach { it.dispose() }
        views.clear()
        element.clearChildren()
    }

    /** Lays out the items of another list (when the ribbon's model is replaced). */
    fun rebind(newItems: RibbonList<RibbonItemModel>) {
        dispose()
        items = newItems
        attach()
    }

    /** Changes the layout orientation. */
    fun setVertical(vertical: Boolean) {
        element.view(XamlInterop.IID_IStackPanel).call(XamlInterop.IStackPanel_put_Orientation, (if (vertical) Orientation.VERTICAL else Orientation.HORIZONTAL).native)
        views.filterIsInstance<RibbonSeparatorView>().forEach { it.isHorizontal = vertical }
        applyLayouts()
    }

    private fun sync() {
        val existing = views.associateBy { it.model }
        views.filter { it.model !in items }.forEach { it.dispose() }
        views.clear()
        element.clearChildren()
        for (item in items) {
            val view = existing[item] ?: RibbonItemViews.create(item, host)?.also { it.attach() } ?: continue
            views += view
            element.addChild(view.element)
        }
        applyLayouts()
        host.invalidateItemsLayout()
    }

    /** Applies the layout to all items. */
    fun applyLayouts() {
        views.forEach { it.applyLayout(layoutOf(it)) }
    }
}

/**
 * The Quick Access Toolbar (RibbonQuickAccessToolBar in RibbonSpace). It lays out the model's QAT items as small buttons,
 * and the button at the end opens the "Customize Quick Access Toolbar" menu.
 */
internal class RibbonQuickAccessBar(private val ribbon: WRibbon, host: RibbonItemHost) {
    val element: XamlElement = XamlElement.load(
        "<StackPanel Orientation=\"Horizontal\" Spacing=\"1\" VerticalAlignment=\"Center\">" +
            "<Button x:Name=\"PART_CustomizeButton\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Width=\"20\" Height=\"26\" Padding=\"0\">" +
            "<FontIcon Glyph=\"&#xE70D;\" FontSize=\"9\" /></Button></StackPanel>",
    )
    private val customizeButton = element.part("PART_CustomizeButton")
    val strip = RibbonItemStrip(ribbon.model.quickAccessItems, host, { view ->
        RibbonItemLayout(com.appkitbox.winui4k.ribbon.RibbonItemSize.SMALL, host.metrics, false, ribbon.model.showQuickAccessLabels)
    })

    init {
        element.insertChild(0, strip.element)
        val name = RibbonStrings.current.customizeQuickAccessToolbar
        customizeButton.setAutomationName(name)
        customizeButton.setToolTipValue(name)
        customizeButton.isVisible = ribbon.model.showQuickAccessCustomizeButton
        customizeButton.onClick {
            val menu = ribbon.buildQuickAccessCustomizeMenu()
            menu.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
            menu.showAt(customizeButton)
        }
    }

    /** Applies the model's QAT settings (labels and the customize button). */
    fun applyOptions() {
        customizeButton.isVisible = ribbon.model.showQuickAccessCustomizeButton
        strip.applyLayouts()
    }

    /** The KeyTip targets (numbered 1, 2, 3... and 09, 08..., as in Office). */
    fun keyTipTargets(): List<RibbonKeyTipTarget> = strip.views().filter { it.isShown }.flatMap { it.keyTipTargets() }
}
