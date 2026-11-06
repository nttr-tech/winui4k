package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

// A tool for table tests to find elements in the visual tree (such as cell TextBlocks).

/** The runtime class name of [node] (DependencyObject) (IInspectable.GetRuntimeClassName). */
internal fun tableTreeClassName(node: ComPtr): String = Ffi.backend.withScope { scope ->
    val out = scope.allocate(8)
    node.call(4, out) // IInspectable::GetRuntimeClassName
    val hstring = Ffi.backend.memory.getPtr(out, 0)
    try {
        Hstring.read(hstring)
    } finally {
        Hstring.free(hstring)
    }
}

/** The children of [node] (VisualTreeHelper.GetChild). The caller releases them. */
internal fun tableTreeChildren(node: ComPtr): List<ComPtr> {
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

/** The first TextBlock under [node] whose text is [text] (an owned reference). null if not found. */
internal fun tableTreeFindText(node: ComPtr, text: String): ComPtr? {
    val matched = node.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { textBlock ->
        try {
            textBlock.getString(XamlInterop.ITextBlock_get_Text) == text
        } finally {
            textBlock.release()
        }
    } == true
    if (matched) return node.also { it.addRef() }
    val children = tableTreeChildren(node)
    try {
        return children.firstNotNullOfOrNull { tableTreeFindText(it, text) }
    } finally {
        children.forEach { it.release() }
    }
}
