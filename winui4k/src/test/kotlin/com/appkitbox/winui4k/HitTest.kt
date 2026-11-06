package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Hit testing for tests: uses WinUI's own VisualTreeHelper.FindElementsInHostCoordinates to check whether [component] itself is among the elements
 * hit when the pointer is at the point ([x], [y]) DIP from [component]'s top-left.
 * Checks with the native hit testing that a Panel without Background is not hit (does not receive mouse events).
 * Use on the UI thread, after placing it in a window and finishing the layout.
 */
internal object HitTest {
    fun hits(component: WComponent, x: Double, y: Double): Boolean {
        val origin = Xaml.positionInRoot(component.uiElement)
        val target = InkNative.identityOf(component.uiElement)
        val root = checkNotNull(component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)) { "not placed in a window" }
        val content = try {
            root.getPtr(XamlInterop.IXamlRoot_get_Content)
        } finally {
            root.release()
        }
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val iterable = try {
            Ffi.backend.withScope { scope ->
                val point = InkNative.pointValue(scope, origin[0] + x, origin[1] + y)
                statics.getPtr(XamlInterop.IVisualTreeHelperStatics_FindElementsInHostCoordinates, point, content.ptr)
            }
        } finally {
            statics.release()
            content.release()
        }
        val iterator = try {
            iterable.getPtr(ITERABLE_FIRST)
        } finally {
            iterable.release()
        }
        return try {
            contains(iterator, target)
        } finally {
            iterator.release()
        }
    }

    /** Whether the rest of IIterator<UIElement> contains an element whose COM identity is [target]. */
    private fun contains(iterator: ComPtr, target: Long): Boolean {
        while (iterator.getBool(ITERATOR_HAS_CURRENT)) {
            val element = iterator.getPtr(ITERATOR_CURRENT)
            val same = try {
                InkNative.identityOf(element) == target
            } finally {
                element.release()
            }
            if (same) return true
            iterator.getBool(ITERATOR_MOVE_NEXT)
        }
        return false
    }

    // IIterable<T>.First / IIterator<T>.get_Current, get_HasCurrent, MoveNext (the same numbers as noted in FoundationInterop)
    private const val ITERABLE_FIRST = 6
    private const val ITERATOR_CURRENT = 6
    private const val ITERATOR_HAS_CURRENT = 7
    private const val ITERATOR_MOVE_NEXT = 8
}
