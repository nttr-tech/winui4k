package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** The groundwork of [WComponent.isShowing] and [WComponent.topLevelAncestor] (the chain of visibility in the visual tree and the window it is placed in). */
internal object ComponentShowing {
    /** Whether [component] itself and all its parents in the visual tree are visible (Visibility.Visible). */
    fun isVisibleInTree(component: WComponent): Boolean {
        if (!component.isVisible) return false
        var current: ComPtr? = Xaml.parentOf(component.uiElement)
        try {
            while (current != null) {
                if (!isElementVisible(current)) return false
                val parent = Xaml.parentOf(current)
                current.release()
                current = parent
            }
            return true
        } finally {
            current?.release()
        }
    }

    /** Whether the Visibility of [node] (an element of the visual tree; borrowed) is Visible. true if it is not a UIElement. */
    private fun isElementVisible(node: ComPtr): Boolean {
        val element = node.queryInterfaceOrNull(XamlInterop.IID_IUIElement) ?: return true
        return try {
            element.getInt(XamlInterop.IUIElement_get_Visibility) == VISIBILITY_VISIBLE
        } finally {
            element.release()
        }
    }

    /** The open [WFrame] with the same XamlRoot as [component] (null if none). */
    fun frameOf(component: WComponent): WFrame? {
        val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        try {
            return WFrame.frames.firstOrNull { frame ->
                val frameRoot = frame.content.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)
                frameRoot != null &&
                    try {
                        Xaml.sameObject(root, frameRoot)
                    } finally {
                        frameRoot.release()
                    }
            }
        } finally {
            root.release()
        }
    }

    /** The value of Visibility.Visible (winmd: Visible = 0, Collapsed = 1). */
    private const val VISIBILITY_VISIBLE = 0
}
