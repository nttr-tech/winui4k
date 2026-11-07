package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Builds the button's appearance placed in ContentControl.Content (a string, a component, or an arrangement of icon and text).
 * Shared by [WButtonBase] and [WSplitButton] (both derive from ContentControl). For the meaning of the public API, see
 * `text` / `content` / `icon` / `horizontalTextPosition` / `verticalTextPosition` / `iconTextGap` of [WButtonBase].
 *
 * [host] is the button that owns this object (where the accessibility name is put), and [contentControl] is host's
 * IContentControl view (owned by host).
 *
 * Revalidates host when the content changes (as Swing's AbstractButton.setText / setIcon do). Even if it changes after being shown, the parent
 * [WLayoutPanel] lays it out again with the new size.
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

    /** The string starting with `<html>` put in [text] (when there is no icon; Content is a formatted TextBlock). null if not HTML. */
    private var htmlText: String? = null

    var text: String
        get() {
            iconLabelText?.let { return it }
            htmlText?.let { return it }
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
            htmlText = null
            releaseLabelAutomationName()
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
            host.revalidate()
        }

    /**
     * Makes the Content text only. As with Swing's JButton, a string starting with `<html>` makes a TextBlock with the formatting copied the Content
     * ([HtmlText]). With composite Content, the button's UI Automation name is not decided from the string, so the text without tags is set as the name.
     */
    private fun putContentText(value: String) {
        if (HtmlText.isHtml(value)) {
            val textBlock = XamlElement.adopt(Activation.activate(XamlInterop.CLS_TextBlock, XamlInterop.IID_ITextBlock))
            HtmlText.putText(textBlock.view(XamlInterop.IID_ITextBlock), value)
            contentControl.call(XamlInterop.IContentControl_put_Content, textBlock.uiElement.ptr)
            htmlText = value
            setLabelAutomationName(HtmlText.toPlainText(value))
        } else {
            htmlText = null
            releaseLabelAutomationName()
            val boxed = PropertyValues.boxString(value)
            contentControl.call(XamlInterop.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }
        host.revalidate()
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
        htmlText = null
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
        val iconElement = XamlElement.adopt(icon.createIconElement(host))
        if (label.isEmpty()) {
            releaseLabelAutomationName()
            contentControl.call(XamlInterop.IContentControl_put_Content, iconElement.uiElement.ptr)
            host.revalidate()
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
        // WLabel's root is a Grid, so the text inside a button uses a TextBlock directly (without a Grid in between)
        val textBlock = XamlElement.adopt(Activation.activate(XamlInterop.CLS_TextBlock, XamlInterop.IID_ITextBlock))
        HtmlText.putText(textBlock.view(XamlInterop.IID_ITextBlock), label)
        val children = if (textFirst) listOf(textBlock, iconElement) else listOf(iconElement, textBlock)
        for (child in children) {
            if (vertical) {
                child.horizontalAlignmentInParent = HorizontalAlignment.CENTER
            } else {
                child.verticalAlignmentInParent = if (child === textBlock) verticalTextPosition.alignment else VerticalAlignment.CENTER
            }
            panel.add(child)
        }
        contentControl.call(XamlInterop.IContentControl_put_Content, panel.uiElement.ptr)
        setLabelAutomationName(if (HtmlText.isHtml(label)) HtmlText.toPlainText(label) else label)
        host.revalidate()
    }

    /** Sets the button's AutomationProperties.Name to [name] (kept as is if the user set another name). */
    private fun setLabelAutomationName(name: String) {
        val current = host.accessibleName
        if (current.isEmpty() || current == labelAutomationName) {
            host.accessibleName = name
            labelAutomationName = name
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
