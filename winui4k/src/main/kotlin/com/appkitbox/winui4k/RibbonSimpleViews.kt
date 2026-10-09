package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel
import com.appkitbox.winui4k.ribbon.RibbonCustomItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonLabelModel
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonRowModel
import com.appkitbox.winui4k.ribbon.RibbonSeparatorModel
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel

/** Creates item views from models (RibbonElementFactory.CreateItem in RibbonSpace). */
internal object RibbonItemViews {
    /**
     * Creates the view for [model] (not yet [RibbonItemView.attach]ed). Returns null for an unsupported model.
     * [embedded] is for placing it inside a drop-down (stretched to the full width).
     */
    fun create(model: RibbonItemModel, host: RibbonItemHost, embedded: Boolean = false): RibbonItemView? = when (model) {
        is RibbonSplitButtonModel -> RibbonSplitButtonView(model, host)
        is RibbonToggleButtonModel -> RibbonToggleButtonView(model, host, embedded)
        is RibbonButtonModel -> RibbonButtonView(model, host, embedded)
        is com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel -> RibbonButtonView(model, host, embedded)
        is RibbonCheckBoxModel -> RibbonCheckBoxView(model, host)
        is RibbonLabelModel -> RibbonLabelView(model, host)
        is RibbonSeparatorModel -> RibbonSeparatorView(model, host)
        is RibbonRowModel -> RibbonStackView(model, host)
        is RibbonButtonGroupModel -> RibbonButtonGroupView(model, host)
        is RibbonCustomItemModel -> RibbonCustomView(model, host)
        else -> RibbonInputViews.create(model, host, embedded)
    }
}

/** Static text (RibbonLabel in RibbonSpace). An icon, if any, is shown on the left. */
internal class RibbonLabelView(model: RibbonLabelModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load(
        "<StackPanel Orientation=\"Horizontal\" Spacing=\"5\" Padding=\"4,0\" IsHitTestVisible=\"True\" Background=\"Transparent\" />",
    )

    override fun measure(layout: RibbonItemLayout): WSize {
        val text = model.label.orEmpty()
        val textWidth = if (text.isEmpty()) 0.0 else host.textWidths.widths(listOf(text), layout.metrics.fontSize, false)[0]
        val icon = if (model.icon != null) layout.metrics.smallIconSize + SPACING else 0.0
        val height = if (layout.isSimplified) layout.metrics.simplifiedItemHeight else layout.metrics.rowHeight
        return WSize(PADDING * 2 + icon + textWidth, height)
    }

    override fun applyLayoutCore() {
        element.clearChildren()
        RibbonIconXaml.build(model.icon, layout.metrics.smallIconSize)?.let {
            element.addChild(XamlElement.load("<Border VerticalAlignment=\"Center\">$it</Border>"))
        }
        element.addChild(
            XamlElement.load(
                "<TextBlock Text=\"${Xaml.escape(model.label)}\" FontSize=\"${Xaml.num(layout.metrics.fontSize)}\" " +
                    "Foreground=\"{ThemeResource RibbonForegroundBrush}\" VerticalAlignment=\"Center\" IsTextScaleFactorEnabled=\"False\" />",
            ),
        )
        val size = measure(layout)
        element.setSize(size.width, size.height)
    }

    override fun refreshEnabled() {
        // Static text has no enabled state
    }

    override val isKeyTipEnabled: Boolean get() = false

    override fun invoke(): Boolean = false

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = emptyList()

    private companion object {
        const val PADDING = 4.0
        const val SPACING = 5.0
    }
}

/** A separator (RibbonSeparator in RibbonSpace). Vertical in a group; horizontal in a vertical toolbar or a menu. */
internal class RibbonSeparatorView(model: RibbonSeparatorModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<Rectangle Fill=\"{ThemeResource RibbonSeparatorBrush}\" />")

    /** Whether it is horizontal (inside a vertical toolbar or a menu). */
    var isHorizontal: Boolean = false

    override fun measure(layout: RibbonItemLayout): WSize = if (isHorizontal) {
        WSize(0.0, 1.0 + MARGIN * 2)
    } else {
        val height = if (layout.isSimplified) layout.metrics.simplifiedItemHeight - 8 else layout.metrics.groupContentHeight - 6
        WSize(1.0 + MARGIN * 2, height + MARGIN * 2)
    }

    override fun applyLayoutCore() {
        val size = measure(layout)
        if (isHorizontal) {
            element.setSize(Double.NaN, 1.0)
            element.horizontalAlignment = HorizontalAlignment.STRETCH
            element.setMargin(4.0, MARGIN, 4.0, MARGIN)
        } else {
            element.setSize(1.0, size.height - MARGIN * 2)
            element.setMargin(MARGIN, MARGIN, MARGIN, MARGIN)
        }
    }

    override fun refreshEnabled() {
        // A separator has no enabled state
    }

    override fun refreshToolTip() {
        // A separator has no tooltip
    }

    override val isKeyTipEnabled: Boolean get() = false

    override fun invoke(): Boolean = false

    override fun keyTipTargets(): List<RibbonKeyTipTarget> = emptyList()

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = listOf(WMenuFlyoutSeparator())

    private companion object {
        const val MARGIN = 3.0
    }
}

/**
 * A container that lays out child items (RibbonItemsContainer in RibbonSpace). Child views are rebuilt incrementally as
 * models are added or removed.
 */
internal abstract class RibbonContainerView(override val model: RibbonButtonGroupModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<StackPanel Orientation=\"Horizontal\" />")

    /** The child views (in model order). */
    protected val children = mutableListOf<RibbonItemView>()

    private val listListener = RibbonListListener<RibbonItemModel> { syncChildren() }

    override fun attach() {
        super.attach()
        model.items.addListListener(listListener)
        syncChildren()
    }

    override fun dispose() {
        super.dispose()
        model.items.removeListListener(listListener)
        children.forEach { it.dispose() }
        children.clear()
    }

    private fun syncChildren() {
        val existing = children.associateBy { it.model }
        children.filter { it.model !in model.items }.forEach { it.dispose() }
        children.clear()
        element.clearChildren()
        for (item in model.items) {
            val view = existing[item] ?: RibbonItemViews.create(item, host)?.also { it.attach() } ?: continue
            children += view
            element.addChild(view.element)
        }
        host.invalidateItemsLayout()
    }

    /** The layout applied to a child. */
    protected abstract fun childLayout(child: RibbonItemView, layout: RibbonItemLayout): RibbonItemLayout

    /** The orientation of the children (true for vertical). */
    protected open val isVertical: Boolean get() = false

    /** Spacing between children. */
    protected open val spacing: Double get() = 0.0

    override fun measure(layout: RibbonItemLayout): WSize {
        val sizes = children.filter { it.isShown }.map { it.measure(childLayout(it, layout)) }
        if (sizes.isEmpty()) return WSize(0.0, 0.0)
        val gaps = spacing * (sizes.size - 1)
        return if (isVertical) {
            WSize(sizes.maxOf { it.width }, sizes.sumOf { it.height } + gaps)
        } else {
            WSize(sizes.sumOf { it.width } + gaps, sizes.maxOf { it.height })
        }
    }

    override fun applyLayoutCore() {
        element.view(XamlInterop.IID_IStackPanel).call(XamlInterop.IStackPanel_put_Orientation, (if (isVertical) Orientation.VERTICAL else Orientation.HORIZONTAL).native)
        element.view(XamlInterop.IID_IStackPanel).call(XamlInterop.IStackPanel_put_Spacing, spacing)
        children.forEach { it.applyLayout(childLayout(it, layout)) }
        val size = measure(layout)
        element.setSize(size.width, size.height)
    }

    override fun refreshEnabled() {
        // Each child item has its own enabled state
    }

    override fun refreshToolTip() {
        // Each child item has its own tooltip
    }

    override fun invoke(): Boolean = false

    override fun keyTipTargets(): List<RibbonKeyTipTarget> = children.filter { it.isShown }.flatMap { it.keyTipTargets() }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = children.filter { it.isShown }.flatMap { it.overflowMenuItems() }

    /** The child views (for KeyTips, search, and tests). */
    fun childViews(): List<RibbonItemView> = children.toList()
}

/** A group of small buttons laid out seamlessly (RibbonButtonGroup in RibbonSpace). Children are small and show no labels. */
internal class RibbonButtonGroupView(model: RibbonButtonGroupModel, host: RibbonItemHost) : RibbonContainerView(model, host) {
    override fun childLayout(child: RibbonItemView, layout: RibbonItemLayout): RibbonItemLayout =
        layout.copy(size = RibbonItemSize.SMALL, showLabel = false)
}

/**
 * A row that behaves as a single item (RibbonStackPanel in RibbonSpace). Children follow their own size definitions
 * (small in the simplified ribbon), and the orientation and spacing are specified by the model.
 */
internal class RibbonStackView(override val model: RibbonRowModel, host: RibbonItemHost) : RibbonContainerView(model, host) {
    override val isVertical: Boolean get() = model.orientation == Orientation.VERTICAL

    override val spacing: Double get() = model.spacing

    override fun childLayout(child: RibbonItemView, layout: RibbonItemLayout): RibbonItemLayout = layout.copy(
        size = if (layout.isSimplified) RibbonItemSize.SMALL else child.model.effectiveSizeDefinition.getSize(layout.groupState),
        showLabel = null,
    )

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "orientation" || name == "spacing") {
            host.invalidateItemsLayout()
            applyLayoutCore()
        }
    }
}

/** An item that hosts an arbitrary component (CreateCustom in RibbonSpace). Its size is the component's desired size. */
internal class RibbonCustomView(override val model: RibbonCustomItemModel, host: RibbonItemHost) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<Border VerticalAlignment=\"Center\" />")
    private var hosted: WComponent? = null

    override fun attach() {
        super.attach()
        syncContent()
    }

    private fun syncContent() {
        val content = model.content
        if (content === hosted) return
        element.setChild(null)
        content?.let { Xaml.detach(it) }
        hosted = content
        element.setChild(content)
        host.invalidateItemsLayout()
    }

    override fun measure(layout: RibbonItemLayout): WSize {
        val content = hosted ?: return WSize(0.0, 0.0)
        val desired = content.preferredSize()
        return WSize(desired.width, desired.height)
    }

    override fun applyLayoutCore() {
        // The component determines its own size
    }

    override fun refreshEnabled() {
        // The component has its own enabled state
    }

    override fun refreshToolTip() {
        // The component has its own tooltip
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "content") syncContent()
    }

    override fun invoke(): Boolean = false

    override fun keyTipTargets(): List<RibbonKeyTipTarget> = emptyList()

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = emptyList()
}
