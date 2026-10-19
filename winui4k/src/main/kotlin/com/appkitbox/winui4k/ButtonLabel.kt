package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Builds the button's appearance placed in ContentControl.Content (a string, a component, or an arrangement of icon and text).
 * Shared by [WButtonBase] and [WSplitButton] (both derive from ContentControl). For the meaning of the public API, see
 * `text` / `content` / `icon` / `horizontalTextPosition` / `verticalTextPosition` / `iconTextGap` of [WButtonBase].
 *
 * [host] is the button that owns this object (where the accessibility name is put), and [contentControl] is host's
 * IContentControl view (owned by host).
 */
internal class ButtonLabel(private val host: WControl, private val contentControl: ComPtr) {
    private var contentComponent: WComponent? = null

    /** The label string while shown with an icon (null while [icon] is null). */
    private var iconLabelText: String? = null

    /**
     * The string put into AutomationProperties.Name when the icon and text are arranged together (a name set by the user
     * is not overwritten).
     */
    private var labelAutomationName: String? = null

    /** The icon shown alongside (null for no icon). */
    private var currentIcon: WIcon? = null

    var icon: WIcon?
        get() = currentIcon
        set(value) = changeIcon(value)

    var horizontalTextPosition: HorizontalTextPosition = HorizontalTextPosition.TRAILING
        set(value) {
            if (value == field) return
            field = value
            if (currentIcon != null) showIconLabel()
        }

    var verticalTextPosition: VerticalTextPosition = VerticalTextPosition.CENTER
        set(value) {
            if (value == field) return
            field = value
            if (currentIcon != null) showIconLabel()
        }

    var iconTextGap: Double = DEFAULT_ICON_TEXT_GAP
        set(value) {
            require(value >= 0.0 && value.isFinite()) { "iconTextGap must be a finite value of 0 or more: $value" }
            if (value == field) return
            field = value
            if (currentIcon != null) showIconLabel()
        }

    var text: String
        get() {
            iconLabelText?.let { return it }
            val boxed = contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            contentComponent = null
            if (currentIcon != null) {
                iconLabelText = value
                showIconLabel()
            } else {
                putContentText(value)
            }
        }

    var content: WComponent?
        get() = contentComponent
        set(value) {
            contentComponent = value
            currentIcon = null
            iconLabelText = null
            releaseLabelAutomationName()
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    private fun putContentText(value: String) {
        val boxed = PropertyValues.boxString(value)
        contentControl.call(XamlInterop.IContentControl_put_Content, boxed.ptr)
        boxed.release()
    }

    /**
     * Changes the icon (the setter of [icon]). Keeps the current [text] and redisplays it laid out with the icon
     * (removing any component in [content]). If null, removes the icon and returns to a string-only Content.
     */
    private fun changeIcon(newIcon: WIcon?) {
        newIcon?.requireSupported()
        if (newIcon == null && currentIcon == null) return
        val label = text
        contentComponent = null
        currentIcon = newIcon
        if (newIcon == null) {
            iconLabelText = null
            releaseLabelAutomationName()
            putContentText(label)
        } else {
            iconLabelText = label
            showIconLabel()
        }
    }

    /**
     * Rebuilds Content from [icon] and [iconLabelText]. If the text is empty, only the icon (IconElement); otherwise the
     * icon and a TextBlock are arranged in a StackPanel in the order and direction given by [horizontalTextPosition] /
     * [verticalTextPosition], with a gap of [iconTextGap].
     * The TextBlock inherits its text color and font from the button's ContentPresenter.
     * With a composite Content, the button's UI Automation name is no longer determined from a string
     * (ButtonBaseAutomationPeer uses Content as the name only when it is a string), so the string is put into
     * AutomationProperties.Name.
     */
    private fun showIconLabel() {
        val icon = checkNotNull(currentIcon)
        val label = checkNotNull(iconLabelText)
        val iconElement = XamlElement.adopt(icon.createIconElement())
        if (label.isEmpty()) {
            releaseLabelAutomationName()
            contentControl.call(XamlInterop.IContentControl_put_Content, iconElement.uiElement.ptr)
            return
        }
        // CENTER + TOP / BOTTOM stacks them vertically; LEADING / TRAILING places them side by side (the vertical position
        // becomes the text's vertical alignment). CENTER + CENTER (text overlaid on the icon) cannot be built with a
        // StackPanel, so it is the same as TRAILING (text after the icon).
        val vertical = horizontalTextPosition == HorizontalTextPosition.CENTER &&
            verticalTextPosition != VerticalTextPosition.CENTER
        val textFirst = if (vertical) {
            verticalTextPosition == VerticalTextPosition.TOP
        } else {
            horizontalTextPosition == HorizontalTextPosition.LEADING
        }
        val panel = WPanel(iconTextGap, if (vertical) Orientation.VERTICAL else Orientation.HORIZONTAL)
        val textBlock = WLabel(label)
        val children = if (textFirst) listOf(textBlock, iconElement) else listOf(iconElement, textBlock)
        for (child in children) {
            if (vertical) {
                child.horizontalAlignment = HorizontalAlignment.CENTER
            } else {
                child.verticalAlignment = if (child === textBlock) verticalTextPosition.alignment else VerticalAlignment.CENTER
            }
            panel.add(child)
        }
        contentControl.call(XamlInterop.IContentControl_put_Content, panel.uiElement.ptr)
        val current = host.accessibleName
        if (current.isEmpty() || current == labelAutomationName) {
            host.accessibleName = label
            labelAutomationName = label
        }
    }

    /**
     * Removes the AutomationProperties.Name put by [showIconLabel] (left as is if the user has changed it to a different
     * name).
     */
    private fun releaseLabelAutomationName() {
        val name = labelAutomationName ?: return
        labelAutomationName = null
        if (host.accessibleName == name) host.accessibleName = ""
    }
}
