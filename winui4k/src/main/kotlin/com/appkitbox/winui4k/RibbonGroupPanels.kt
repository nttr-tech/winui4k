package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.ribbon.RibbonStrings
import kotlin.math.abs
import kotlin.math.max

/**
 * A group's expanded panel (slide-out; the expanded panel of RibbonGroup.Panels in RibbonSpace).
 * It opens below the group from the arrow on the panel title, and when pinned it does not close on an outside click.
 */
internal class RibbonGroupSlideOut(private val group: RibbonGroupView) {
    private var popup: WPopup? = null
    private var host: XamlElement? = null
    private var pinButton: XamlElement? = null

    /** Whether it is pinned (it stays open, and reopens when the tab is selected again). */
    var isPinned: Boolean = false
        set(value) {
            field = value
            popup?.isLightDismissEnabled = !value
            updatePinButton()
        }

    /** Whether it is open. */
    val isOpen: Boolean get() = popup?.isOpen == true

    /** Opens it (below the group, with light dismiss unless pinned). */
    fun open() {
        val p = popup ?: build()
        Xaml.detach(group.slideOutPanel)
        host!!.setChild(group.slideOutPanel)
        group.applySlideOutLayouts()
        p.isLightDismissEnabled = !isPinned
        updatePinButton()
        val position = group.element.positionInRoot()
        p.horizontalOffset = position[0]
        p.verticalOffset = position[1] + group.element.actualHeight
        (p.child as? XamlElement)?.let {
            it.requestedTheme = group.container.themeSource.actualTheme
            it.setMinWidth(group.element.actualWidth)
        }
        p.show(group.element)
        group.updateSlideOutButton()
    }

    /** Closes it (the pinned setting is kept). */
    fun close() {
        if (isOpen) popup?.hide()
    }

    private fun build(): WPopup {
        val metrics = group.metrics
        val chrome = XamlElement.load(
            "<Border BorderThickness=\"1\" CornerRadius=\"0,0,3,3\" Background=\"{ThemeResource RibbonCommandBarBackgroundBrush}\" " +
                "BorderBrush=\"{ThemeResource RibbonPopupBorderBrush}\"><Grid><Grid.RowDefinitions><RowDefinition Height=\"Auto\" />" +
                "<RowDefinition Height=\"Auto\" /></Grid.RowDefinitions><Border x:Name=\"PART_Host\" Padding=\"4,3,4,3\" />" +
                "<Grid Grid.Row=\"1\" Height=\"${Xaml.num(metrics.groupCaptionHeight + 2)}\" Background=\"{ThemeResource RibbonGroupCaptionBackgroundBrush}\">" +
                "<TextBlock Text=\"${Xaml.escape(group.label)}\" FontSize=\"${Xaml.num(metrics.captionFontSize)}\" HorizontalAlignment=\"Center\" " +
                "VerticalAlignment=\"Center\" Margin=\"24,0,8,1\" Foreground=\"{ThemeResource RibbonGroupCaptionForegroundBrush}\" />" +
                "<ToggleButton x:Name=\"PART_Pin\" Padding=\"0\" Width=\"22\" Height=\"18\" MinWidth=\"0\" MinHeight=\"0\" BorderThickness=\"0\" " +
                "Background=\"Transparent\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Center\" " +
                "Foreground=\"{ThemeResource RibbonSecondaryForegroundBrush}\" /></Grid></Grid></Border>",
        )
        host = chrome.part("PART_Host")
        val pin = chrome.part("PART_Pin")
        pinButton = pin
        pin.onClick {
            isPinned = pin.toggleChecked == true
            group.container.ribbon?.raiseStateChanged()
        }
        group.container.attachPopupKeyboard(chrome)
        chrome.onKeyDown { e ->
            if (e.key == RibbonInputViews.VK_ESCAPE && !e.handled) {
                isPinned = false
                close()
                group.focusSlideOutButton()
                e.handled = true
            }
        }
        val created = WPopup(chrome)
        created.addCloseListener {
            host?.setChild(null)
            group.updateSlideOutButton()
        }
        popup = created
        return created
    }

    private fun updatePinButton() {
        val pin = pinButton ?: return
        val strings = RibbonStrings.current
        val text = if (isPinned) strings.unpinPanel else strings.pinPanel
        pin.setAutomationName(text)
        pin.setToolTipValue(text)
        if (pin.toggleChecked != isPinned) pin.toggleChecked = isPinned
        pin.setContent(XamlElement.load("<FontIcon Glyph=\"${if (isPinned) "&#xE840;" else "&#xE718;"}\" FontSize=\"11\" />"))
    }
}

/**
 * A group's floating panel (the floating panels of RibbonGroup.Panels in RibbonSpace). It floats over the window outside
 * the ribbon and can be moved by dragging the left strip or the title. It stays open when another tab is selected, and
 * "Return to Ribbon" puts it back in its original place.
 */
internal class RibbonGroupFloating(private val group: RibbonGroupView) {
    private var popup: WPopup? = null
    private var itemsHost: XamlElement? = null
    private var slideOutHost: XamlElement? = null
    private var dragStart: DoubleArray? = null
    private var captionDragStart: DoubleArray? = null

    /** Whether it is a floating panel. */
    var isFloating: Boolean = false
        private set

    /** The position (in window coordinates). */
    var x: Double = DEFAULT_X
        private set

    /** The position (in window coordinates). */
    var y: Double = DEFAULT_Y
        private set

    /** Whether it is shown on screen. */
    val isOpen: Boolean get() = popup?.isOpen == true

    /** Floats the panel ([atX], [atY] are window coordinates; null means just below its position in the ribbon). */
    fun float(atX: Double?, atY: Double?) {
        if (atX != null && atY != null) {
            x = atX
            y = atY
        } else if (group.element.isVisible && group.element.actualWidth > 0) {
            val p = group.element.positionInRoot()
            x = p[0]
            y = p[1] + group.element.actualHeight + OFFSET
        }
        if (isFloating) {
            show()
            return
        }
        isFloating = true
        group.closePopup()
        group.closeSlideOut()
        show()
        group.container.groupFloatingChanged(group)
    }

    /** Returns it to its place in the ribbon. */
    fun returnToRibbon() {
        if (!isFloating) return
        isFloating = false
        suspend()
        itemsHost?.setChild(null)
        slideOutHost?.setChild(null)
        group.restoreItemsPanel()
        group.container.groupFloatingChanged(group)
    }

    /** Shows it on screen (it stays closed while the ribbon is hidden). */
    fun show() {
        if (!isFloating) return
        val p = popup ?: build()
        Xaml.detach(group.itemsPanel)
        group.placeForPopup()
        itemsHost!!.setChild(group.itemsPanel)
        if (group.hasSlideOut) {
            Xaml.detach(group.slideOutPanel)
            group.applySlideOutLayouts()
            slideOutHost!!.setChild(group.slideOutPanel)
            slideOutHost!!.isVisible = true
        } else {
            slideOutHost!!.isVisible = false
        }
        (p.child as? XamlElement)?.requestedTheme = group.container.themeSource.actualTheme
        clamp()
        p.horizontalOffset = x
        p.verticalOffset = y
        p.show(group.container.themeSource)
    }

    /** Closes it temporarily (when the ribbon is hidden). */
    fun suspend() {
        if (isOpen) popup?.hide()
    }

    private fun build(): WPopup {
        val strings = RibbonStrings.current
        val metrics = group.metrics
        val chrome = XamlElement.load(
            "<Border BorderThickness=\"1\" CornerRadius=\"3\" Background=\"{ThemeResource RibbonCommandBarBackgroundBrush}\" " +
                "BorderBrush=\"{ThemeResource RibbonPopupBorderBrush}\"><Grid><Grid.ColumnDefinitions><ColumnDefinition Width=\"Auto\" />" +
                "<ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
                "<Grid x:Name=\"PART_Bar\" Width=\"20\" Background=\"{ThemeResource RibbonFloatingPanelBarBrush}\"><Grid.RowDefinitions>" +
                "<RowDefinition Height=\"Auto\" /><RowDefinition Height=\"*\" /></Grid.RowDefinitions>" +
                "<Button x:Name=\"PART_Return\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Padding=\"0\" Width=\"18\" Height=\"20\" " +
                "HorizontalAlignment=\"Center\"><FontIcon Glyph=\"&#xE73F;\" FontSize=\"10\" /></Button>" +
                "<TextBlock Grid.Row=\"1\" Text=\"&#x22EE;&#x22EE;\" FontSize=\"10\" HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" " +
                "IsHitTestVisible=\"False\" Foreground=\"{ThemeResource RibbonSecondaryForegroundBrush}\" /></Grid>" +
                "<StackPanel Grid.Column=\"1\"><Border x:Name=\"PART_Items\" Padding=\"4,3,4,0\" />" +
                "<Border x:Name=\"PART_SlideOut\" BorderThickness=\"0,1,0,0\" Padding=\"4,3,4,0\" BorderBrush=\"{ThemeResource RibbonSeparatorBrush}\" />" +
                "<Grid x:Name=\"PART_Caption\" Height=\"${Xaml.num(metrics.groupCaptionHeight + 2)}\" " +
                "Background=\"{ThemeResource RibbonGroupCaptionBackgroundBrush}\">" +
                "<TextBlock Text=\"${Xaml.escape(group.label)}\" FontSize=\"${Xaml.num(metrics.captionFontSize)}\" HorizontalAlignment=\"Center\" " +
                "VerticalAlignment=\"Center\" Margin=\"8,0,8,1\" Foreground=\"{ThemeResource RibbonGroupCaptionForegroundBrush}\" /></Grid>" +
                "</StackPanel></Grid></Border>",
        )
        itemsHost = chrome.part("PART_Items")
        slideOutHost = chrome.part("PART_SlideOut")
        val returnButton = chrome.part("PART_Return")
        returnButton.setAutomationName(strings.returnPanelToRibbon)
        returnButton.setToolTipValue(strings.returnPanelToRibbon)
        returnButton.onClick { returnToRibbon() }
        chrome.setAutomationName(group.label)
        group.container.attachPopupKeyboard(chrome)
        attachDrag(chrome.part("PART_Bar"))
        attachDrag(chrome.part("PART_Caption"))
        chrome.onRightTapped { e ->
            val p = e.position(chrome)
            if (group.container.showGroupContextMenu(group, p[0], p[1])) e.markHandled()
        }
        val created = WPopup(chrome)
        created.isLightDismissEnabled = false
        popup = created
        return created
    }

    private fun attachDrag(handle: XamlElement) {
        handle.onPointer(XamlInterop.IUIElement_add_PointerPressed) { e ->
            if (popup == null || !e.isLeftButtonPressed || e.isFromButton) return@onPointer
            dragStart = e.position(null)
            e.capture(handle)
            e.handled = true
        }
        handle.onPointer(XamlInterop.IUIElement_add_PointerMoved) { e ->
            val start = dragStart ?: return@onPointer
            val p = popup ?: return@onPointer
            val current = e.position(null)
            x += current[0] - start[0]
            y += current[1] - start[1]
            dragStart = current
            clamp()
            p.horizontalOffset = x
            p.verticalOffset = y
            e.handled = true
        }
        val end: (XamlPointerEvent) -> Unit = { e ->
            if (dragStart != null) {
                dragStart = null
                e.release(handle)
                group.container.ribbon?.raiseStateChanged()
            }
        }
        handle.onPointer(XamlInterop.IUIElement_add_PointerReleased, end)
        handle.onPointer(XamlInterop.IUIElement_add_PointerCaptureLost, end)
    }

    private fun clamp() {
        val root = group.container.themeSource
        val width = popup?.child?.actualWidth?.takeIf { it > 0 } ?: MIN_WIDTH
        val rootWidth = root.actualWidth.takeIf { it > 0 } ?: Double.MAX_VALUE
        x = x.coerceIn(0.0, max(0.0, rootWidth - minOf(width, rootWidth)))
        y = max(0.0, y)
    }

    /** Dragging the panel's title ([caption]) far enough makes it a floating panel. */
    fun attachCaptionDrag(caption: XamlElement) {
        caption.onPointer(XamlInterop.IUIElement_add_PointerPressed) { e ->
            if (!group.container.canFloatGroups || !e.isLeftButtonPressed || e.isFromButton) return@onPointer
            captionDragStart = e.position(null)
            e.capture(caption)
        }
        caption.onPointer(XamlInterop.IUIElement_add_PointerMoved) { e ->
            val start = captionDragStart ?: return@onPointer
            val current = e.position(null)
            if (abs(current[1] - start[1]) > DRAG_THRESHOLD_Y || abs(current[0] - start[0]) > DRAG_THRESHOLD_X) {
                captionDragStart = null
                e.release(caption)
                float(current[0] - group.element.actualWidth / 2, current[1] - DRAG_GRAB_OFFSET)
            }
        }
        caption.onPointer(XamlInterop.IUIElement_add_PointerReleased) { e ->
            if (captionDragStart != null) {
                captionDragStart = null
                e.release(caption)
            }
        }
    }

    private companion object {
        const val DEFAULT_X = 120.0
        const val DEFAULT_Y = 160.0
        const val OFFSET = 8.0
        const val MIN_WIDTH = 120.0
        const val DRAG_THRESHOLD_X = 24.0
        const val DRAG_THRESHOLD_Y = 16.0
        const val DRAG_GRAB_OFFSET = 10.0
    }
}
