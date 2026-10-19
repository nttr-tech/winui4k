package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** Helpers for reading elements inside a control's template and their colors in tests. Call on the UI thread. */
internal object TemplateParts {
    /** The element in [control]'s template whose name (FrameworkElement.Name) is [name]. null if absent. The caller releases it. */
    fun find(control: WControl, name: String): ComPtr? {
        val root = Xaml.childAt(control.inspectable, 0) ?: return null
        return findByName(root, name)
    }

    /** Searches [element] (taking ownership) and its descendants for the element named [name]. */
    private fun findByName(element: ComPtr, name: String): ComPtr? {
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        val found = try {
            framework.getString(XamlInterop.IFrameworkElement_get_Name) == name
        } finally {
            framework.release()
        }
        if (found) return element
        try {
            for (i in 0 until Xaml.childCount(element)) {
                val child = Xaml.childAt(element, i) ?: continue
                findByName(child, name)?.let { return it }
            }
        } finally {
            element.release()
        }
        return null
    }

    /** The color of [slot] (a get_Xxx returning a brush) of the [iid] view of element [name] in [control]'s template. null if the element or brush is absent. */
    fun brushColor(control: WControl, name: String, iid: String, slot: Int): WColor? {
        val element = find(control, name) ?: return null
        try {
            val view = element.queryInterface(iid)
            try {
                val brush = view.getPtrOrNull(slot) ?: return null
                return try {
                    colorOf(brush)
                } finally {
                    brush.release()
                }
            } finally {
                view.release()
            }
        } finally {
            element.release()
        }
    }
}
