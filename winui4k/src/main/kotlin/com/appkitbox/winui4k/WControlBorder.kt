package com.appkitbox.winui4k

/**
 * javax.swing.border.Border-style: a control's border (set on [WControl.border]).
 * The kinds are the line border [WLineBorder], the empty border [WEmptyBorder] and the combination [WCompoundBorder],
 * created with [WBorderFactory].
 * `WBorder` is the class of a bordered container, so this interface, which corresponds to Swing's `Border`, has a
 * different name.
 *
 * Swing's TitledBorder, EtchedBorder, BevelBorder and MatteBorder are not supported (TitledBorder needs a place to draw
 * the title, which a WinUI Control's border cannot express). A WinUI border is only a single-color line and inner
 * padding, so there can be at most one line border.
 */
sealed interface WControlBorder {
    /**
     * Border.getBorderInsets-style: the width (DIP) this border takes around the content.
     * The thickness for a line, the specified padding for an empty border, and the sum of inside and outside for a combination.
     */
    val borderInsets: WInsets
}

/**
 * LineBorder-style: a single-color line border. Drawn with the Control's BorderBrush (color) and BorderThickness (the
 * same thickness on all four sides).
 * Swing's rounded corners (roundedCorners) are not supported (corners can be rounded with [WControl.cornerRadius]).
 */
data class WLineBorder @JvmOverloads constructor(
    /** Line color. */
    val color: WColor,
    /** Line thickness (DIP, 0 or more). 1, the same as Swing's default. */
    val thickness: Int = 1,
) : WControlBorder {
    init {
        require(thickness >= 0) { "line thickness must be 0 or more: $thickness" }
    }

    override val borderInsets: WInsets get() = WInsets(thickness.toDouble())
}

/**
 * EmptyBorder-style: a border that draws nothing and only takes padding around the content. Becomes the Control's Padding.
 * The argument order is top, left, bottom, right, as in Swing (unlike WinUI's Thickness, which is left, top, right, bottom).
 */
data class WEmptyBorder(
    /** Top padding (DIP, 0 or more). */
    val top: Int,
    /** Left padding (DIP, 0 or more). */
    val left: Int,
    /** Bottom padding (DIP, 0 or more). */
    val bottom: Int,
    /** Right padding (DIP, 0 or more). */
    val right: Int,
) : WControlBorder {
    init {
        require(top >= 0 && left >= 0 && bottom >= 0 && right >= 0) { "padding must be 0 or more: $top, $left, $bottom, $right" }
    }

    override val borderInsets: WInsets
        get() = WInsets(top = top.toDouble(), left = left.toDouble(), bottom = bottom.toDouble(), right = right.toDouble())
}

/**
 * CompoundBorder-style: a combination of the outside border [outsideBorder] and the inside border [insideBorder].
 * null is the same as an empty border (as in Swing).
 *
 * In a WinUI Control, the border line is always outside the padding. So the outside/inside order is not distinguished,
 * at most one line border may be contained (IllegalArgumentException if there are two or more when set on
 * [WControl.border]), and the padding of the contained empty borders is summed into Padding.
 */
data class WCompoundBorder(
    /** Outside border. */
    val outsideBorder: WControlBorder?,
    /** Inside border. */
    val insideBorder: WControlBorder?,
) : WControlBorder {
    override val borderInsets: WInsets
        get() {
            val outside = outsideBorder?.borderInsets ?: WInsets(0.0)
            val inside = insideBorder?.borderInsets ?: WInsets(0.0)
            return WInsets(
                top = outside.top + inside.top,
                left = outside.left + inside.left,
                bottom = outside.bottom + inside.bottom,
                right = outside.right + inside.right,
            )
        }
}

/**
 * BorderFactory-style: a factory that creates [WControlBorder]s. From Java, call it like
 * `WBorderFactory.createLineBorder(WColor.RED, 2)`.
 * Set the created border on [WControl.border].
 */
object WBorderFactory {
    /** Creates a line border of color [color] and thickness [thickness] (default 1). */
    @JvmStatic
    @JvmOverloads
    fun createLineBorder(color: WColor, thickness: Int = 1): WLineBorder = WLineBorder(color, thickness)

    /** Creates an empty border that only takes padding. The argument order is top, left, bottom, right. */
    @JvmStatic
    fun createEmptyBorder(top: Int, left: Int, bottom: Int, right: Int): WEmptyBorder = WEmptyBorder(top, left, bottom, right)

    /** Creates an empty border with no padding. */
    @JvmStatic
    fun createEmptyBorder(): WEmptyBorder = WEmptyBorder(0, 0, 0, 0)

    /** Combines the outside [outside] and inside [inside] borders. null is the same as an empty border. */
    @JvmStatic
    fun createCompoundBorder(outside: WControlBorder?, inside: WControlBorder?): WCompoundBorder = WCompoundBorder(outside, inside)

    /** Creates a combination where both are null (the same as an empty border with no padding). */
    @JvmStatic
    fun createCompoundBorder(): WCompoundBorder = WCompoundBorder(null, null)
}

/** A border flattened into a line border (at most one) and the total empty padding, to be transferred to Control properties. */
internal class FlatBorder(val line: WLineBorder?, val padding: WInsets?) {
    companion object {
        /** Flattens [border]. IllegalArgumentException if it has two or more line borders. */
        fun of(border: WControlBorder): FlatBorder {
            val lines = ArrayList<WLineBorder>()
            var padding: WInsets? = null

            fun visit(node: WControlBorder?) {
                when (node) {
                    null -> Unit
                    is WLineBorder -> lines += node
                    is WEmptyBorder -> padding = padding.plus(node.borderInsets)
                    is WCompoundBorder -> {
                        visit(node.outsideBorder)
                        visit(node.insideBorder)
                    }
                }
            }
            visit(border)
            require(lines.size <= 1) { "only one line border can be set (a WinUI border is a single color with the same thickness on all four sides): $border" }
            return FlatBorder(lines.firstOrNull(), padding)
        }

        private fun WInsets?.plus(other: WInsets): WInsets =
            if (this == null) other else WInsets(top + other.top, left + other.left, bottom + other.bottom, right + other.right)
    }
}
