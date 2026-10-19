package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** A helper for reading a button's Content (a string, an icon, or a StackPanel with an icon and text) in tests. Call on the UI thread. */
internal object ButtonContents {
    /**
     * Represents the button's Content as a list of strings. A string Content is "String:<string>", an IconElement is
     * [IconElements.describe], and a StackPanel is "Panel:<orientation>:<spacing>" followed by its children (a TextBlock is
     * "TextBlock:<string>"). Empty if there is no Content.
     */
    fun contentOf(button: WButtonBase): List<String> {
        val contentControl = button.inspectable.queryInterface(XamlInterop.IID_IContentControl)
        val content = try {
            contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content)
        } finally {
            contentControl.release()
        } ?: return emptyList()
        return try {
            PropertyValues.unboxString(content)?.let { return listOf("String:$it") }
            val panel = content.queryInterfaceOrNull(XamlInterop.IID_IStackPanel) ?: return listOf(describeElement(content))
            try {
                val orientation = Orientation.of(panel.getInt(XamlInterop.IStackPanel_get_Orientation))
                val spacing = panel.getDouble(XamlInterop.IStackPanel_get_Spacing)
                listOf("Panel:$orientation:$spacing") + childrenOf(panel).map { child ->
                    try {
                        describeElement(child)
                    } finally {
                        child.release()
                    }
                }
            } finally {
                panel.release()
            }
        } finally {
            content.release()
        }
    }

    /** The list of references to the children (Panel.Children) of a StackPanel. The caller must release each of them. */
    fun childrenOf(stackPanel: ComPtr): List<ComPtr> {
        val panel = stackPanel.queryInterface(XamlInterop.IID_IPanel)
        val children = try {
            panel.getPtr(XamlInterop.IPanel_get_Children)
        } finally {
            panel.release()
        }
        return try {
            (0 until children.getInt(FoundationInterop.IVector_get_Size)).map { children.getPtr(FoundationInterop.IVector_GetAt, it) }
        } finally {
            children.release()
        }
    }

    private fun describeElement(element: ComPtr): String {
        val textBlock = element.queryInterfaceOrNull(XamlInterop.IID_ITextBlock) ?: return IconElements.describe(element)
        return try {
            "TextBlock:" + textBlock.getString(XamlInterop.ITextBlock_get_Text)
        } finally {
            textBlock.release()
        }
    }
}
