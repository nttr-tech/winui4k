package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Creates the XAML objects (Binding / DataTemplate) used by [WTableView] and registers
 * dependency property change callbacks.
 */
internal object TableViewXaml {
    private const val XMLNS =
        "xmlns=\"http://schemas.microsoft.com/winfx/2006/xaml/presentation\" " +
            "xmlns:x=\"http://schemas.microsoft.com/winfx/2006/xaml\""

    /** FrameworkElement.TagProperty (cached for the lifetime of the process). */
    val tagProperty: ComPtr by lazy {
        val statics = Activation.factory(XamlInterop.CLS_FrameworkElement, XamlInterop.IID_IFrameworkElementStatics)
        try {
            statics.getPtr(XamlInterop.IFrameworkElementStatics_get_TagProperty)
        } finally {
            statics.release()
        }
    }

    private val bindingOperations: ComPtr by lazy {
        Activation.factory(XamlInterop.CLS_BindingOperations, XamlInterop.IID_IBindingOperationsStatics)
    }

    /** Creates a Binding (IBinding) that binds the path [path] for display (OneWay). The caller releases it. */
    fun createBinding(path: String): ComPtr {
        val binding = Activation.composeDefault(XamlInterop.CLS_Binding, XamlInterop.IID_IBindingFactory)
        val pathFactory = Activation.factory(XamlInterop.CLS_PropertyPath, XamlInterop.IID_IPropertyPathFactory)
        try {
            val propertyPath = Hstring.use(path) { h -> pathFactory.getPtr(XamlInterop.IPropertyPathFactory_CreateInstance, h) }
            binding.call(XamlInterop.IBinding_put_Path, propertyPath.ptr)
            propertyPath.release()
        } finally {
            pathFactory.release()
        }
        binding.call(XamlInterop.IBinding_put_Mode, XamlInterop.BindingMode_OneWay)
        return binding
    }

    /** Sets a OneWay binding with the path [path] on the [property] of [target] (a DependencyObject). */
    fun setBinding(target: ComPtr, property: ComPtr, path: String) {
        val binding = createBinding(path)
        val bindingBase = binding.queryInterface(XamlInterop.IID_IBindingBase)
        try {
            bindingOperations.call(XamlInterop.IBindingOperationsStatics_SetBinding, target.ptr, property.ptr, bindingBase.ptr)
        } finally {
            bindingBase.release()
            binding.release()
        }
    }

    /**
     * Creates a DataTemplate (IDataTemplate) from the DataTemplate content [content] (XAML with a single root element).
     * The default namespace and the x: namespace are added automatically. The caller releases it.
     */
    fun createDataTemplate(content: String): ComPtr {
        val statics = Activation.factory(XamlInterop.CLS_XamlReader, XamlInterop.IID_IXamlReaderStatics)
        val loaded = try {
            Hstring.use("<DataTemplate $XMLNS>$content</DataTemplate>") { h ->
                statics.getPtr(XamlInterop.IXamlReaderStatics_Load, h)
            }
        } finally {
            statics.release()
        }
        return try {
            loaded.queryInterface(XamlInterop.IID_IDataTemplate)
        } finally {
            loaded.release()
        }
    }

    /**
     * Returns the content of a DataTemplate for a cell that takes the cell value from the binding path [valuePath] and
     * wraps the app's DataTemplate content [content] so that `{Binding}` becomes that value.
     */
    fun valueTemplate(valuePath: String, content: String): String =
        "<ContentPresenter Content=\"{Binding $valuePath}\" HorizontalAlignment=\"Stretch\" " +
            "HorizontalContentAlignment=\"Stretch\" VerticalContentAlignment=\"Center\">" +
            "<ContentPresenter.ContentTemplate><DataTemplate>$content</DataTemplate></ContentPresenter.ContentTemplate>" +
            "</ContentPresenter>"

    /**
     * Calls [onChanged] whenever the [property] of [target] (a DependencyObject) changes
     * (DependencyObject.RegisterPropertyChangedCallback). The callback's sender is the element that changed.
     */
    fun registerPropertyChangedCallback(target: ComPtr, property: ComPtr, onChanged: (sender: Ptr) -> Unit) {
        val callback = KComObject("WinUI4K.DependencyPropertyChangedCallback", inspectable = false)
            .addInterface(
                XamlInterop.IID_DependencyPropertyChangedCallback,
                listOf(
                    // Invoke(this, DependencyObject sender, DependencyProperty dp) — vtbl[3]
                    KComObject.Method(CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)) { args ->
                        onChanged(args[1] as Ptr)
                        KComObject.S_OK
                    },
                ),
            )
        try {
            Ffi.backend.withScope { scope ->
                val token = scope.allocate(8) // out i8
                target.call(XamlInterop.IDependencyObject_RegisterPropertyChangedCallback, property.ptr, callback.primary, token)
            }
        } finally {
            // The registration target holds a reference, so release the creation reference
            // (it is reclaimed when the element is destroyed)
            callback.release()
        }
    }
}
