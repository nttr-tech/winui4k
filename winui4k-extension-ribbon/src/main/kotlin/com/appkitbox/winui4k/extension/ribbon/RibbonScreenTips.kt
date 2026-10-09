package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonScreenTip
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Creates ScreenTips (rich tooltips) (RibbonScreenTipService and the RibbonScreenTip template in RibbonSpace).
 *
 * Stacks a bold title (with the shortcut), an image, a description, the reason it is disabled, and a help footer
 * vertically. The extended description ([RibbonScreenTip.extendedDescription] / [RibbonScreenTip.extendedImage]) appears
 * after the tooltip stays open for [extendedDelayMillis] (an AutoCAD-style progressive tooltip).
 */
internal object RibbonScreenTips {
    /**
     * The time until the extended description of a progressive tooltip appears (milliseconds). 0 or less shows it
     * immediately.
     */
    var extendedDelayMillis: Long = 1500

    /** Whether to show the extended description of progressive tooltips (AutoCAD's "Show extended ToolTips"). */
    var isExtendedEnabled: Boolean = true

    private const val NORMAL_MAX_WIDTH = 320.0
    private const val EXTENDED_MAX_WIDTH = 440.0
    private const val IMAGE_SIZE = 96.0
    private const val EXTENDED_IMAGE_SIZE = 160.0

    /**
     * Creates a tooltip (a ToolTip element). Returns null if there is no ScreenTip, description, or label.
     * Without a ScreenTip and a description, it is a simple tooltip with only the title (and the shortcut).
     * The reason it is disabled is shown only when [isEnabled] is false.
     */
    fun create(label: String?, tip: RibbonScreenTip?, description: String?, shortcut: String?, isEnabled: Boolean): XamlElement? {
        val title = label?.replace('\n', ' ')
        if (tip == null && description.isNullOrBlank()) {
            if (title.isNullOrBlank()) return null
            return toolTip("<TextBlock Text=\"${Xaml.escape(RibbonScreenTip.formatTitle(title, shortcut))}\" TextWrapping=\"Wrap\" />")
        }
        val merged = RibbonScreenTip(tip?.title ?: title, tip?.description ?: description, tip?.shortcut ?: shortcut).apply {
            helpText = tip?.helpText
            disabledReason = if (isEnabled) null else tip?.disabledReason
            image = tip?.image
            extendedDescription = tip?.extendedDescription
            extendedImage = tip?.extendedImage
        }
        val element = toolTip(contentXaml(merged))
        if (merged.hasExtendedContent && isExtendedEnabled) attachProgressive(element)
        return element
    }

    /** The tooltip of a group's dialog launcher ("{group name} settings"). */
    fun dialogLauncher(groupLabel: String?, tip: RibbonScreenTip?): XamlElement? =
        create(RibbonStrings.current.dialogLauncher(groupLabel.orEmpty()), tip, null, null, true)

    private fun toolTip(content: String): XamlElement =
        XamlElement.load("<ToolTip Style=\"{StaticResource RibbonToolTipStyle}\">$content</ToolTip>")

    private fun contentXaml(tip: RibbonScreenTip): String = buildString {
        append("<StackPanel x:Name=\"PART_Root\" Spacing=\"6\" Padding=\"2\" MaxWidth=\"").append(Xaml.num(NORMAL_MAX_WIDTH)).append("\">")
        append("<TextBlock Text=\"").append(Xaml.escape(RibbonScreenTip.formatTitle(tip.title, tip.shortcut)))
        append("\" FontWeight=\"SemiBold\" TextWrapping=\"Wrap\" />")
        RibbonIconXaml.build(tip.image, IMAGE_SIZE)?.let { append("<Border HorizontalAlignment=\"Left\">").append(it).append("</Border>") }
        appendText(tip.description, "Opacity=\"0.9\"")
        appendText(tip.disabledReason, "FontStyle=\"Italic\" Opacity=\"0.8\"")
        if (tip.hasExtendedContent) {
            append("<StackPanel x:Name=\"PART_Extended\" Spacing=\"6\" Visibility=\"Collapsed\" BorderThickness=\"0,1,0,0\" ")
            append("Padding=\"0,6,0,0\" BorderBrush=\"{ThemeResource RibbonSeparatorBrush}\">")
            RibbonIconXaml.build(tip.extendedImage, EXTENDED_IMAGE_SIZE)?.let { append("<Border HorizontalAlignment=\"Left\">").append(it).append("</Border>") }
            appendText(tip.extendedDescription, "Opacity=\"0.9\"")
            append("</StackPanel>")
        }
        if (!tip.helpText.isNullOrBlank()) {
            append("<StackPanel Orientation=\"Horizontal\" Spacing=\"6\" BorderThickness=\"0,1,0,0\" Padding=\"0,6,0,0\" ")
            append("BorderBrush=\"{ThemeResource RibbonSeparatorBrush}\"><FontIcon Glyph=\"&#xE897;\" FontSize=\"12\" />")
            appendText(tip.helpText, "")
            append("</StackPanel>")
        }
        append("</StackPanel>")
    }

    private fun StringBuilder.appendText(text: String?, attributes: String) {
        if (text.isNullOrBlank()) return
        append("<TextBlock Text=\"").append(Xaml.escape(text)).append("\" TextWrapping=\"Wrap\" ").append(attributes).append(" />")
    }

    /** Shows the extended description if the tooltip is still open [extendedDelayMillis] after it opened. */
    private fun attachProgressive(tip: XamlElement) {
        var timer: AutoCloseable? = null
        val root = tip.part("PART_Root")
        val extended = tip.part("PART_Extended")
        fun show(visible: Boolean) {
            extended.isVisible = visible
            root.maxWidth = if (visible) EXTENDED_MAX_WIDTH else NORMAL_MAX_WIDTH
        }
        val toolTip = tip.view(XamlInterop.IID_IToolTip)
        toolTip.addEventHandler("WinUI4K.ToolTipOpened", XamlInterop.IID_RoutedEventHandler, XamlInterop.IToolTip_add_Opened) { _, _ ->
            show(false)
            timer?.close()
            timer = if (extendedDelayMillis <= 0) {
                show(true)
                null
            } else {
                WinUiUtilities.schedule(extendedDelayMillis) { show(true) }
            }
        }
        toolTip.addEventHandler("WinUI4K.ToolTipClosed", XamlInterop.IID_RoutedEventHandler, XamlInterop.IToolTip_add_Closed) { _, _ ->
            timer?.close()
            timer = null
        }
    }
}
