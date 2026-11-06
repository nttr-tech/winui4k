package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Test helpers that click navigation controls through the same path as the user (pressing them with UI Automation peers).
 * Used from both Kotlin and Java tests. Call on the UI thread after placing the control in a window.
 */
object NavigationClicks {
    /** Raises NavigationView's ItemInvoked and selection, the same as clicking [item] (the peer's SelectionItem.Select). */
    @JvmStatic
    fun clickNavigationItem(item: WNavigationViewItem) {
        Xaml.invoke(item.inspectable)
    }

    /** Clicks the level (BreadcrumbBarItem) of [bar] whose label is [label] (the peer's Invoke). IllegalStateException if not found. */
    @JvmStatic
    fun clickBreadcrumbItem(bar: WBreadcrumbBar, label: String) {
        val item = checkNotNull(findBreadcrumbItem(bar.dependencyObject, label)) { "level $label not found" }
        try {
            Xaml.invoke(item)
        } finally {
            item.release()
        }
    }

    /** The first BreadcrumbBarItem under [node] (a DependencyObject; VisualTreeHelper accepts only this view) with a TextBlock whose string is [label] (an owned reference). */
    private fun findBreadcrumbItem(node: ComPtr, label: String): ComPtr? {
        if (runtimeClassName(node) == "Microsoft.UI.Xaml.Controls.BreadcrumbBarItem" && label in texts(node)) {
            node.addRef()
            return node
        }
        for (child in children(node)) {
            try {
                findBreadcrumbItem(child, label)?.let { return it }
            } finally {
                child.release()
            }
        }
        return null
    }

    /** The runtime class name of [node] (a DependencyObject) (IInspectable.GetRuntimeClassName). */
    private fun runtimeClassName(node: ComPtr): String = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        node.call(4, out) // IInspectable::GetRuntimeClassName
        val hstring = Ffi.backend.memory.getPtr(out, 0)
        try {
            Hstring.read(hstring)
        } finally {
            Hstring.free(hstring)
        }
    }

    /** The child elements of [node] (VisualTreeHelper.GetChild). The caller releases them. */
    private fun children(node: ComPtr): List<ComPtr> {
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        try {
            val count = Ffi.backend.withScope { scope ->
                val out = scope.allocate(4)
                statics.call(XamlInterop.IVisualTreeHelperStatics_GetChildrenCount, node.ptr, out)
                Ffi.backend.memory.getInt(out, 0)
            }
            return (0 until count).mapNotNull { statics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetChild, node.ptr, it) }
        } finally {
            statics.release()
        }
    }

    /** The strings of the TextBlocks under [node]. */
    private fun texts(node: ComPtr): List<String> {
        val own = node.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { textBlock ->
            textBlock.getString(XamlInterop.ITextBlock_get_Text).also { textBlock.release() }
        }
        return listOfNotNull(own) + children(node).flatMap { child -> texts(child).also { child.release() } }
    }
}
