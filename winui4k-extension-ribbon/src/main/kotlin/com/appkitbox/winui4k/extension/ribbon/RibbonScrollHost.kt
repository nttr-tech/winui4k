package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.math.max

/**
 * An area that scrolls horizontally with left and right arrow buttons and the mouse wheel when the width is insufficient
 * (RibbonScrollPanel in RibbonSpace).
 *
 * The content is placed on a Canvas and scrolled by shifting Canvas.Left negative (operated with the arrows and the wheel;
 * no scroll bar is shown). The overflowing part is clipped with a RectangleGeometry the size of the viewport. The caller
 * provides the content width ([extent]).
 */
internal class RibbonScrollHost {
    /** The element placed in the parent (a Grid). */
    val element: XamlElement = XamlElement.load(
        "<Grid Background=\"Transparent\">" +
            "<Canvas x:Name=\"PART_Viewport\" />" +
            "<Button x:Name=\"PART_Left\" Style=\"{StaticResource RibbonScrollButtonStyle}\" Width=\"22\" HorizontalAlignment=\"Left\" " +
            "VerticalAlignment=\"Stretch\" Visibility=\"Collapsed\" IsTabStop=\"False\"><FontIcon Glyph=\"&#xE76B;\" FontSize=\"10\" /></Button>" +
            "<Button x:Name=\"PART_Right\" Style=\"{StaticResource RibbonScrollButtonStyle}\" Width=\"22\" HorizontalAlignment=\"Right\" " +
            "VerticalAlignment=\"Stretch\" Visibility=\"Collapsed\" IsTabStop=\"False\"><FontIcon Glyph=\"&#xE76C;\" FontSize=\"10\" /></Button>" +
            "</Grid>",
    )
    private val viewport = element.part("PART_Viewport")
    private val left = element.part("PART_Left")
    private val right = element.part("PART_Right")
    private var content: XamlElement? = null

    /** The width of the content. */
    var extent: Double = 0.0
        private set

    /** The width of the viewport. */
    var viewportWidth: Double = 0.0
        private set

    /** The height of the viewport. */
    var viewportHeight: Double = 0.0
        private set

    /** The scroll position (the distance from the left edge of the content). */
    var offset: Double = 0.0
        private set

    /** Whether the content overflows the viewport. */
    val hasOverflow: Boolean get() = extent > viewportWidth + 0.5

    init {
        left.setAutomationName(RibbonStrings.current.scrollLeft)
        right.setAutomationName(RibbonStrings.current.scrollRight)
        left.onClick { scrollBy(-max(SCROLL_STEP, viewportWidth * SCROLL_RATIO)) }
        right.onClick { scrollBy(max(SCROLL_STEP, viewportWidth * SCROLL_RATIO)) }
        element.onPointer(XamlInterop.IUIElement_add_PointerWheelChanged) { e ->
            if (hasOverflow) {
                scrollBy(-e.wheelDelta.toDouble())
                e.handled = true
            }
        }
    }

    /** Replaces the content (the scroll position returns to the start). */
    fun setContent(newContent: XamlElement?) {
        content?.let { viewport.removeChild(it) }
        content = newContent
        newContent?.let { viewport.addChild(it) }
        offset = 0.0
        arrange()
    }

    /** Given the content width [extent] and the viewport size, updates the scroll position, the arrows, and the clipping. */
    fun update(extent: Double, viewportWidth: Double, viewportHeight: Double) {
        this.extent = extent
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        arrange()
    }

    /** Scrolls by [delta]. */
    fun scrollBy(delta: Double) {
        offset = (offset + delta).coerceIn(0.0, max(0.0, extent - viewportWidth))
        arrange()
    }

    /** Scrolls so that the range [x]..[x]+[width] of the content is visible. */
    fun bringIntoView(x: Double, width: Double) {
        if (!hasOverflow) return
        if (x < offset) {
            offset = max(0.0, x - MARGIN)
        } else if (x + width > offset + viewportWidth) {
            offset = minOf(extent - viewportWidth, x + width - viewportWidth + MARGIN)
        }
        arrange()
    }

    private fun arrange() {
        val overflow = hasOverflow
        offset = if (overflow) offset.coerceIn(0.0, extent - viewportWidth) else 0.0
        content?.setCanvasPosition(-offset, 0.0)
        left.isVisible = overflow && offset > 0.5
        right.isVisible = overflow && offset < extent - viewportWidth - 0.5
        viewport.setSize(viewportWidth, viewportHeight)
        if (viewportWidth > 0 && viewportHeight > 0) {
            val clip = Xaml.load("<RectangleGeometry Rect=\"0,0,${Xaml.num(viewportWidth)},${Xaml.num(viewportHeight)}\" />")
            try {
                viewport.uiElement.call(XamlInterop.IUIElement_put_Clip, clip.ptr)
            } finally {
                clip.release()
            }
        }
    }

    private companion object {
        /** The minimum scroll amount of the arrow buttons. */
        const val SCROLL_STEP = 120.0

        /** The ratio of the arrow buttons' scroll amount to the viewport width. */
        const val SCROLL_RATIO = 0.6

        /** The margin used when bringing an element into view. */
        const val MARGIN = 24.0
    }
}
