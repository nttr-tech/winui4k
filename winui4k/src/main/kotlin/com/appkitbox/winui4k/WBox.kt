package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * javax.swing.Box-style: a collection of static methods that create [WBoxLayout] boxes and invisible components that make gaps in a box ([Filler]).
 *
 * Swing's Box is a container class with a BoxLayout, but in WinUI4K [createHorizontalBox] / [createVerticalBox] return
 * a [WLayoutPanel] with a [WBoxLayout] (no instance of Box itself is created).
 *
 * - Strut: a gap of a fixed length. [createHorizontalStrut] fixes the width and [createVerticalStrut] the height, and the crossing direction stretches
 * - Rigid area: a gap of the size given to [createRigidArea] that neither grows nor shrinks
 * - Glue: a gap of size 0 that takes the spare space in proportion to how much it can grow. Placing it on both sides centers the children between
 *
 * Unbounded lengths are Short.MAX_VALUE (32767), as in Swing.
 */
object WBox {
    /** Short.MAX_VALUE, which Swing's Box uses for unbounded lengths. */
    private const val UNBOUNDED = 32767.0

    /** Like Box.createHorizontalBox: a panel that lays out children from left to right ([WBoxLayout.X_AXIS]). */
    @JvmStatic
    fun createHorizontalBox(): WLayoutPanel = boxPanel(WBoxLayout.X_AXIS)

    /** Like Box.createVerticalBox: a panel that lays out children from top to bottom ([WBoxLayout.Y_AXIS]). */
    @JvmStatic
    fun createVerticalBox(): WLayoutPanel = boxPanel(WBoxLayout.Y_AXIS)

    private fun boxPanel(axis: Int): WLayoutPanel = WLayoutPanel(null).also { it.layout = WBoxLayout(it, axis) }

    /** Like Box.createRigidArea: a gap whose minimum, preferred and maximum are all [size], which neither grows nor shrinks. */
    @JvmStatic
    fun createRigidArea(size: WSize): Filler = Filler(size, size, size)

    /** Like Box.createHorizontalStrut: a gap of width [width] (the height stretches from 0 to unbounded). */
    @JvmStatic
    fun createHorizontalStrut(width: Double): Filler =
        Filler(WSize(width, 0.0), WSize(width, 0.0), WSize(width, UNBOUNDED))

    /** Like Box.createVerticalStrut: a gap of height [height] (the width stretches from 0 to unbounded). */
    @JvmStatic
    fun createVerticalStrut(height: Double): Filler =
        Filler(WSize(0.0, height), WSize(0.0, height), WSize(UNBOUNDED, height))

    /** Like Box.createGlue: glue that takes the spare space both vertically and horizontally. */
    @JvmStatic
    fun createGlue(): Filler = Filler(WSize(0.0, 0.0), WSize(0.0, 0.0), WSize(UNBOUNDED, UNBOUNDED))

    /** Like Box.createHorizontalGlue: glue that takes the horizontal spare space (height 0). */
    @JvmStatic
    fun createHorizontalGlue(): Filler = Filler(WSize(0.0, 0.0), WSize(0.0, 0.0), WSize(UNBOUNDED, 0.0))

    /** Like Box.createVerticalGlue: glue that takes the vertical spare space (width 0). */
    @JvmStatic
    fun createVerticalGlue(): Filler = Filler(WSize(0.0, 0.0), WSize(0.0, 0.0), WSize(0.0, UNBOUNDED))

    /**
     * Like Box.Filler: an invisible component with only minimum, preferred and maximum sizes (a Canvas that draws nothing). It does not take focus.
     * The sizes are put into [WComponent.minimumSize] / [WComponent.preferredSize] / [WComponent.maximumSize].
     */
    class Filler(minimum: WSize, preferred: WSize, maximum: WSize) : WComponent(
        Activation.composeDefault(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasFactory),
    ) {
        override val isCanvasElement: Boolean get() = true

        init {
            isFocusable = false
            applyShape(minimum, preferred, maximum)
        }

        /** Like Box.Filler.changeShape: changes the minimum, preferred and maximum sizes and rearranges the parent ([revalidate]). */
        fun changeShape(minimum: WSize, preferred: WSize, maximum: WSize) {
            applyShape(minimum, preferred, maximum)
            revalidate()
        }

        private fun applyShape(minimum: WSize, preferred: WSize, maximum: WSize) {
            minimumSize = minimum
            preferredSize = preferred
            maximumSize = maximum
        }
    }
}
