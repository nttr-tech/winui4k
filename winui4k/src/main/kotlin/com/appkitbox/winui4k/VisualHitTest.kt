package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/** Converting element coordinates and finding elements at a point (VisualTreeHelper.FindElementsInHostCoordinates). Use from the UI thread. */
internal object VisualHitTest {
    /**
     * Converts the coordinates ([x], [y]) (DIP) of [element] (something that QIs to UIElement; borrowed) to the coordinates of [relativeTo] (something that QIs to UIElement; borrowed).
     * If [relativeTo] is null, the host coordinates (the top-left of the window content).
     */
    fun transform(element: ComPtr, relativeTo: ComPtr?, x: Double, y: Double): Pair<Double, Double> {
        val uiElement = element.queryInterface(XamlInterop.IID_IUIElement)
        val target = relativeTo?.queryInterface(XamlInterop.IID_IUIElement)
        val transform = try {
            uiElement.getPtr(XamlInterop.IUIElement_TransformToVisual, target?.ptr ?: Ptr.NULL)
        } finally {
            uiElement.release()
            target?.release()
        }
        return try {
            Ffi.backend.withScope { scope ->
                val point = scope.allocate(XamlStructs.POINT_FLOAT)
                Ffi.backend.memory.putFloat(point.ptr, 0, x.toFloat())
                Ffi.backend.memory.putFloat(point.ptr, 4, y.toFloat())
                val result = scope.allocate(XamlStructs.POINT_FLOAT)
                transform.call(XamlInterop.IGeneralTransform_TransformPoint, point, result.ptr)
                Ffi.backend.memory.getFloat(result.ptr, 0).toDouble() to Ffi.backend.memory.getFloat(result.ptr, 4).toDouble()
            }
        } finally {
            transform.release()
        }
    }

    /**
     * Returns the elements (UIElement) at the coordinates ([x], [y]) of [subtree] within [subtree] (something that QIs to UIElement; borrowed), from the frontmost.
     * The caller releases the returned references. Empty if not shown. If [includeAllElements] is true, elements excluded from hit testing
     * (IsHitTestVisible = false or panels without a background) are also included (includeAllElements of FindElementsInHostCoordinates).
     */
    fun elementsAt(subtree: ComPtr, x: Double, y: Double, includeAllElements: Boolean = false): List<ComPtr> {
        val (hostX, hostY) = transform(subtree, null, x, y)
        val subtreeElement = subtree.queryInterface(XamlInterop.IID_IUIElement)
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val iterable = try {
            Ffi.backend.withScope { scope ->
                val point = scope.allocate(XamlStructs.POINT_FLOAT)
                Ffi.backend.memory.putFloat(point.ptr, 0, hostX.toFloat())
                Ffi.backend.memory.putFloat(point.ptr, 4, hostY.toFloat())
                if (includeAllElements) {
                    statics.getPtrOrNull(
                        XamlInterop.IVisualTreeHelperStatics_FindAllElementsInHostCoordinates,
                        point,
                        subtreeElement.ptr,
                        1, // A boolean (1 byte) is passed in the low part of an I32 register
                    )
                } else {
                    statics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_FindElementsInHostCoordinates, point, subtreeElement.ptr)
                }
            }
        } finally {
            statics.release()
            subtreeElement.release()
        } ?: return emptyList()
        val iterator = try {
            iterable.getPtr(FoundationInterop.IIterable_First)
        } finally {
            iterable.release()
        }
        val result = ArrayList<ComPtr>()
        try {
            var hasCurrent = iterator.getBool(FoundationInterop.IIterator_get_HasCurrent)
            while (hasCurrent) {
                iterator.getPtrOrNull(FoundationInterop.IIterator_get_Current)?.let { result += it }
                hasCurrent = iterator.getBool(FoundationInterop.IIterator_MoveNext)
            }
        } finally {
            iterator.release()
        }
        return result
    }

    /** The ActualWidth and ActualHeight of [element] (something that QIs to FrameworkElement; borrowed). */
    fun actualSizeOf(element: ComPtr): Pair<Double, Double> {
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth) to framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight)
        } finally {
            framework.release()
        }
    }
}
