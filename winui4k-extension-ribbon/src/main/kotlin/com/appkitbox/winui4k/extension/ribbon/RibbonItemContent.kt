package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WSize
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMetrics
import kotlin.math.max

/** How each part of a split button is drawn (RibbonItemContentPart in RibbonSpace). */
internal enum class RibbonContentPart {
    /** The icon, label, and chevron. */
    ALL,

    /** The icon only (the top half of a large split button, or the primary action part of a small split button). */
    ICON_ONLY,

    /** The label and chevron (the bottom half of a large split button). */
    LABEL_AND_CHEVRON,
}

/** The specification of how an item's content is drawn. */
internal data class RibbonContentSpec(
    val label: String?,
    val icon: RibbonIcon?,
    val largeIcon: RibbonIcon?,
    val size: RibbonItemSize,
    val metrics: RibbonMetrics,
    val showLabel: Boolean = true,
    val showChevron: Boolean = false,
    val isSimplified: Boolean = false,
    val part: RibbonContentPart = RibbonContentPart.ALL,
    val colorBar: RibbonColor? = null,
    /** The icon foreground (a XAML value; the disabled text color when disabled). */
    val iconForeground: String = RibbonIconXaml.ICON_BRUSH,
)

/** The computed size of the content and the XAML that draws it (laid out at absolute positions on a Canvas). */
internal data class RibbonContentLayout(val width: Double, val height: Double, val xaml: String)

/**
 * Computes the placement of an item's icon, label (split optimally into two lines for large items, as Office does),
 * chevron, and color bar (a port of MeasureOverride / ArrangeOverride / SplitLabel of RibbonItemContent in RibbonSpace).
 *
 * Text widths come from [RibbonTextWidths], so the computation itself does not depend on the UI. The returned size is the
 * size of the content and does not include the item button's outer border (a 1px border on each side).
 */
internal object RibbonItemContent {
    /** The width of the chevron. */
    private const val CHEVRON_WIDTH = 10.0

    /**
     * The chevron width used when evaluating splits where the second line has a chevron (the same as SplitLabel in
     * RibbonSpace).
     */
    private const val SPLIT_CHEVRON_WIDTH = 12.0

    /** Computes the size and XAML of the content. */
    fun layout(spec: RibbonContentSpec, widths: RibbonTextWidths): RibbonContentLayout {
        val measures = measure(spec, widths)
        val canvas = ContentCanvas(spec, measures.iconSize, spec.metrics.fontSize + 2)
        val size = if (measures.large) arrangeLarge(canvas, measures) else arrangeRow(canvas, measures)
        return RibbonContentLayout(size.width, size.height, canvas.toXaml(size.width, size.height))
    }

    /**
     * Values used to compute the placement (whether there is an icon, chevron, and color bar, and the lines shown and
     * their widths).
     */
    @Suppress("LongParameterList") // A holder for the set of values RibbonItemContent measures, as is
    private class Measures(
        val spec: RibbonContentSpec,
        val large: Boolean,
        val icon: RibbonIcon?,
        val iconSize: Double,
        val chevronW: Double,
        val hasBar: Boolean,
        val line1: String,
        val line2: String,
        val l1: Double,
        val l2: Double,
    ) {
        val iconW: Double get() = if (icon != null) iconSize else 0.0
    }

    private fun measure(spec: RibbonContentSpec, widths: RibbonTextWidths): Measures {
        val m = spec.metrics
        val large = spec.size == RibbonItemSize.LARGE && !spec.isSimplified
        val icon = when {
            spec.part == RibbonContentPart.LABEL_AND_CHEVRON -> null
            large -> spec.largeIcon ?: spec.icon
            else -> spec.icon
        }
        val chevron = spec.showChevron && spec.part != RibbonContentPart.ICON_ONLY
        val lines = lines(spec, large, icon != null, widths)
        val lineWidths = widths.widths(listOf(lines.first, lines.second), m.fontSize, false)
        return Measures(
            spec = spec,
            large = large,
            icon = icon,
            iconSize = if (large) m.largeIconSize else m.smallIconSize,
            chevronW = if (chevron) CHEVRON_WIDTH else 0.0,
            hasBar = spec.colorBar != null && spec.part != RibbonContentPart.LABEL_AND_CHEVRON,
            line1 = lines.first,
            line2 = lines.second,
            l1 = lineWidths[0],
            l2 = lineWidths[1],
        )
    }

    /** The first and second lines to show (the second line only for large items). Both are empty if the label is not shown. */
    private fun lines(spec: RibbonContentSpec, large: Boolean, hasIcon: Boolean, widths: RibbonTextWidths): Pair<String, String> {
        val label = spec.label
        val showText = when {
            spec.part == RibbonContentPart.ICON_ONLY || label.isNullOrEmpty() -> false
            large || !hasIcon -> true
            spec.size == RibbonItemSize.SMALL && !spec.isSimplified -> false
            else -> spec.showLabel
        }
        if (!showText) return "" to ""
        if (!large) return label!!.replace('\n', ' ') to ""
        return splitLabel(label!!, spec.metrics.fontSize, spec.showChevron, widths)
    }

    /**
     * Splits the label of a large item into two lines. If there is an explicit line break, splits there; otherwise splits
     * at the word boundary that makes the longer line shortest (including the chevron width if the second line has one).
     * A single word stays on one line.
     */
    fun splitLabel(label: String, fontSize: Double, showChevron: Boolean, widths: RibbonTextWidths): Pair<String, String> {
        val explicitBreak = label.indexOf('\n')
        if (explicitBreak >= 0) return label.substring(0, explicitBreak).trim() to label.substring(explicitBreak + 1).trim()
        val words = label.split(' ').filter { it.isNotEmpty() }
        if (words.size < 2) return label to ""
        val candidates = (1 until words.size).map { i -> words.subList(0, i).joinToString(" ") to words.subList(i, words.size).joinToString(" ") }
        val measured = widths.widths(candidates.flatMap { listOf(it.first, it.second) }, fontSize, false)
        val chevron = if (showChevron) SPLIT_CHEVRON_WIDTH else 0.0
        var best = 0
        var bestWidth = Double.MAX_VALUE
        for (i in candidates.indices) {
            val width = max(measured[i * 2], measured[i * 2 + 1] + chevron)
            if (width < bestWidth - 0.5) {
                bestWidth = width
                best = i
            }
        }
        return candidates[best]
    }

    /** Determines the size of a large item and stacks the icon, color bar, two-line label, and chevron vertically. */
    private fun arrangeLarge(canvas: ContentCanvas, s: Measures): WSize {
        val spec = s.spec
        val m = spec.metrics
        val secondLine = if (s.line2.isEmpty()) s.chevronW else s.l2 + (if (s.chevronW > 0) s.chevronW + 2 else 0.0)
        val width = max(max(max(s.iconW, s.l1), secondLine) + m.itemPadding * 2 - 2, m.largeItemMinWidth)
        val height = when (spec.part) {
            RibbonContentPart.ICON_ONLY -> m.largeIconSize + 6
            RibbonContentPart.LABEL_AND_CHEVRON -> m.largeItemHeight - m.largeIconSize - 6
            RibbonContentPart.ALL -> m.largeItemHeight
        }
        if (spec.part != RibbonContentPart.LABEL_AND_CHEVRON) {
            val iconTop = if (spec.part == RibbonContentPart.ICON_ONLY) (height - s.iconW) / 2 + 1 else 3.0
            s.icon?.let { canvas.icon(it, (width - s.iconW) / 2, iconTop - (if (s.hasBar) 2 else 0)) }
            if (s.hasBar) canvas.colorBar(spec.colorBar!!, (width - s.iconW) / 2, iconTop + s.iconW - 3, s.iconW, 5.0)
        }
        if (spec.part != RibbonContentPart.ICON_ONLY) {
            arrangeLargeLabel(canvas, s, width, if (spec.part == RibbonContentPart.LABEL_AND_CHEVRON) 0.0 else m.largeIconSize + 5)
        }
        return WSize(width, height)
    }

    /**
     * Lays out a large item's label (two lines) and chevron centered from [textTop]. The chevron goes to the right of
     * the second line (centered if there is no second line).
     */
    private fun arrangeLargeLabel(canvas: ContentCanvas, s: Measures, width: Double, textTop: Double) {
        val lineH = s.spec.metrics.fontSize + 2
        if (s.line1.isNotEmpty()) canvas.text(s.line1, (width - s.l1) / 2, textTop)
        if (s.line2.isEmpty()) {
            if (s.chevronW > 0) canvas.chevron((width - s.chevronW) / 2, textTop + lineH, s.chevronW, lineH, large = true)
            return
        }
        val total = s.l2 + (if (s.chevronW > 0) s.chevronW + 2 else 0.0)
        val x = (width - total) / 2
        canvas.text(s.line2, x, textTop + lineH)
        if (s.chevronW > 0) canvas.chevron(x + s.l2 + 2, textTop + lineH, s.chevronW, lineH, large = true)
    }

    /** Determines the size of a row item (medium, small, simplified) and lays out the icon, label, and chevron horizontally. */
    private fun arrangeRow(canvas: ContentCanvas, s: Measures): WSize {
        val spec = s.spec
        val pad = spec.metrics.itemPadding
        val iconOnly = spec.part == RibbonContentPart.ICON_ONLY
        val height = if (spec.isSimplified) spec.metrics.simplifiedItemHeight else spec.metrics.rowHeight
        val textW = if (s.l1 > 0) s.l1 + (if (s.iconW > 0) 5 else 0) else 0.0
        val width = max(s.iconW + textW + (if (s.chevronW > 0) s.chevronW + 3 else 0.0) + pad * 2 - (if (iconOnly) 1 else 0), height)
        val cy = height / 2
        var x = pad - (if (iconOnly) 1 else 0)
        s.icon?.let { canvas.icon(it, x, cy - s.iconW / 2 - (if (s.hasBar) 1.5 else 0.0)) }
        if (s.hasBar) canvas.colorBar(spec.colorBar!!, x, cy + s.iconW / 2 - 2, s.iconW, 3.0)
        x += s.iconW
        if (s.line1.isNotEmpty()) {
            x += if (s.iconW > 0) 5 else 0
            canvas.text(s.line1, x, cy - (spec.metrics.fontSize + 2) / 2)
        }
        if (s.chevronW > 0) canvas.chevron(width - pad - CHEVRON_WIDTH, cy - 5, CHEVRON_WIDTH, CHEVRON_WIDTH, large = false)
        return WSize(width, height)
    }

    /** Builds the XAML of a Canvas that lays out the content elements at absolute positions. */
    private class ContentCanvas(private val spec: RibbonContentSpec, private val iconSize: Double, private val lineHeight: Double) {
        private val children = StringBuilder()

        fun icon(icon: RibbonIcon, x: Double, y: Double) {
            val xaml = RibbonIconXaml.build(icon, iconSize, spec.iconForeground) ?: return
            children.append(place(xaml, x, y))
        }

        fun colorBar(color: RibbonColor, x: Double, y: Double, width: Double, height: Double) {
            children.append(
                "<Rectangle Fill=\"${color.toHex(true)}\" Width=\"${Xaml.num(width)}\" Height=\"${Xaml.num(height)}\" " +
                    "Canvas.Left=\"${Xaml.num(x)}\" Canvas.Top=\"${Xaml.num(y)}\" IsHitTestVisible=\"False\" />",
            )
        }

        fun text(text: String, x: Double, y: Double) {
            val fontSize = Xaml.num(spec.metrics.fontSize)
            children.append(
                "<TextBlock Text=\"${Xaml.escape(text)}\" FontSize=\"$fontSize\" LineHeight=\"${Xaml.num(lineHeight)}\" " +
                    "LineStackingStrategy=\"BlockLineHeight\" TextLineBounds=\"Full\" TextWrapping=\"NoWrap\" " +
                    "IsTextScaleFactorEnabled=\"False\" IsHitTestVisible=\"False\" " +
                    "Canvas.Left=\"${Xaml.num(x)}\" Canvas.Top=\"${Xaml.num(y)}\" />",
            )
        }

        fun chevron(x: Double, y: Double, width: Double, height: Double, large: Boolean) {
            children.append(
                "<Grid Width=\"${Xaml.num(width)}\" Height=\"${Xaml.num(height)}\" Canvas.Left=\"${Xaml.num(x)}\" Canvas.Top=\"${Xaml.num(y)}\" " +
                    "IsHitTestVisible=\"False\"><FontIcon Glyph=\"&#xE70D;\" FontSize=\"${if (large) 8 else 7}\" " +
                    "HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" IsTextScaleFactorEnabled=\"False\" /></Grid>",
            )
        }

        private fun place(xaml: String, x: Double, y: Double): String {
            // Add the Canvas attached properties to the start tag of the generated element
            val tagEnd = xaml.indexOfAny(charArrayOf(' ', '>'))
            return xaml.substring(0, tagEnd) + " Canvas.Left=\"${Xaml.num(x)}\" Canvas.Top=\"${Xaml.num(y)}\"" + xaml.substring(tagEnd)
        }

        fun toXaml(width: Double, height: Double): String =
            "<Canvas Width=\"${Xaml.num(width)}\" Height=\"${Xaml.num(height)}\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" " +
                "IsHitTestVisible=\"False\">$children</Canvas>"
    }
}
