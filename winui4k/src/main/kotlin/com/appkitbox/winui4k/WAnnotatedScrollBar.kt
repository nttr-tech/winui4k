package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * WinUI 3's AnnotatedScrollBar (a Control subclass). Extends a vertical scrollbar with a rail
 * of clickable labels (markers) so users can jump quickly through a large collection.
 * No Swing equivalent, so we keep WinUI's class name as-is.
 *
 * It doesn't do anything on its own; connect it to a [WScrollView] as its vertical scroll
 * controller via [connectTo]. Do this only after the ScrollView's template has been applied
 * (i.e. after Loaded).
 */
class WAnnotatedScrollBar : WControl(
    Activation.composeDefault(Abi.CLS_AnnotatedScrollBar, Abi.IID_IAnnotatedScrollBarFactory), // default interface = IAnnotatedScrollBar
) {
    /** DetailLabelRequested event tokens registered via addDetailLabelRequestedListener. */
    private val detailLabelRequestedTokens = ListenerTokens<(Double) -> String>()

    /** The IScrollController view (AnnotatedScrollBar.ScrollController). Passed to a ScrollView. */
    private val scrollController: ComPtr by lazy {
        own(inspectable.getPtr(Abi.IAnnotatedScrollBar_get_ScrollController))
    }

    /** The label collection (AnnotatedScrollBar.Labels, an IVector<AnnotatedScrollBarLabel>). */
    private val labels: ComPtr by lazy {
        own(inspectable.getPtr(Abi.IAnnotatedScrollBar_get_Labels))
    }

    /** The activatable factory that creates AnnotatedScrollBarLabel instances. */
    private val labelFactory: ComPtr by lazy {
        own(Activation.factory(Abi.CLS_AnnotatedScrollBarLabel, Abi.IID_IAnnotatedScrollBarLabelFactory))
    }

    /**
     * Connects this AnnotatedScrollBar to [scrollView] as its vertical scroll controller.
     * ScrollView.ScrollPresenter is only available after Loaded, so call this like
     * `scrollView.addLoadedListener { annotatedScrollBar.connectTo(scrollView) }`.
     * Returns false if the connection couldn't be made (the ScrollPresenter isn't created yet).
     */
    fun connectTo(scrollView: WScrollView): Boolean {
        val presenter = scrollView.scrollPresenter ?: return false
        presenter.call(Abi.IScrollPresenter_put_VerticalScrollController, scrollController.ptr)
        return true
    }

    /**
     * Adds a label at the given offset (AnnotatedScrollBar.Labels.Append). [content] is the
     * label string shown to the left of the rail, and [offset] is the vertical scroll offset
     * on the rail (in pixels).
     */
    fun addLabel(content: String, offset: Double) {
        val boxed = PropertyValues.boxString(content)
        val label = labelFactory.getPtr(Abi.IAnnotatedScrollBarLabelFactory_CreateInstance, boxed.ptr, offset)
        boxed.release()
        labels.call(Abi.IVector_Append, label.ptr)
    }

    /** Removes all added labels (AnnotatedScrollBar.Labels.Clear). */
    fun clearLabels() {
        labels.call(Abi.IVector_Clear)
    }

    /**
     * Supplies the content of the detail label (tooltip) shown when hovering over the rail
     * (AnnotatedScrollBar.DetailLabelRequested). The listener receives the target scroll offset,
     * and the string it returns is displayed as the tooltip.
     */
    fun addDetailLabelRequestedListener(listener: (Double) -> String) {
        val token = inspectable.addEventHandler(
            "WinUI4K.AnnotatedScrollBarDetailLabelRequestedHandler",
            Abi.IID_AnnotatedScrollBarDetailLabelRequestedHandler,
            Abi.IAnnotatedScrollBar_add_DetailLabelRequested,
        ) { _, args ->
            val eventArgs = ComPtr(args)
            val offset = eventArgs.getDouble(Abi.IAnnotatedScrollBarDetailLabelRequestedEventArgs_get_ScrollOffset)
            val boxed = PropertyValues.boxString(listener(offset))
            eventArgs.call(Abi.IAnnotatedScrollBarDetailLabelRequestedEventArgs_put_Content, boxed.ptr)
            boxed.release()
        }
        detailLabelRequestedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addDetailLabelRequestedListener]. */
    fun removeDetailLabelRequestedListener(listener: (Double) -> String) {
        val token = detailLabelRequestedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IAnnotatedScrollBar_remove_DetailLabelRequested, token)
    }

    /**
     * Applies a LabelTemplate that displays the label string (AnnotatedScrollBarLabel.Content).
     *
     * The real WinUI default template shows the string via `{Binding Content}`, but winui4k has
     * no app-side XamlTypeInfo to resolve a property path, so that binding always resolves to
     * nothing (a path-less `{Binding}` still resolves to the DataContext object itself). To work
     * around this, we use a path-less Binding piped through the Kotlin-implemented
     * [labelContentConverter], which pulls Content out of the label object. The visuals
     * (right alignment, margin, minimum width) match the real default template.
     */
    private fun applyLabelTemplate() {
        registerLabelContentConverter()
        val statics = Activation.factory(Abi.CLS_XamlReader, Abi.IID_IXamlReaderStatics)
        val template = Hstring.use(LABEL_TEMPLATE_XAML) { h ->
            statics.getPtr(Abi.IXamlReaderStatics_Load, h)
        }
        statics.release()
        val elementFactory = template.queryInterface(Abi.IID_IElementFactory)
        template.release()
        inspectable.call(Abi.IAnnotatedScrollBar_put_LabelTemplate, elementFactory.ptr)
        elementFactory.release()
    }

    init {
        applyLabelTemplate()
    }

    private companion object {
        /** The key under which [labelContentConverter] is registered in Application.Resources. */
        const val CONVERTER_RESOURCE_KEY = "WinUI4KAnnotatedScrollBarLabelContentConverter"

        /** Equivalent to the real default LabelTemplate (MinWidth 44 = LabelsGridMinWidth; Content goes through the converter). */
        val LABEL_TEMPLATE_XAML =
            "<DataTemplate xmlns=\"http://schemas.microsoft.com/winfx/2006/xaml/presentation\">" +
                "<Border MinWidth=\"44\">" +
                "<TextBlock Margin=\"0,-5,0,-2\" HorizontalAlignment=\"Right\" HorizontalTextAlignment=\"Right\" " +
                "TextWrapping=\"NoWrap\" " +
                "Text=\"{Binding Converter={StaticResource $CONVERTER_RESOURCE_KEY}}\" />" +
                "</Border>" +
                "</DataTemplate>"

        /** IValueConverter.Convert / ConvertBack (this, object, TypeName byval, object, HSTRING, out object). */
        val DESC_CONVERT = CallDescriptor(
            ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.Struct(XamlStructs.TYPE_NAME),
            ArgKind.PTR, ArgKind.PTR, ArgKind.PTR,
        )

        const val E_NOTIMPL = -0x7FFF_BFFF // 0x80004001

        /** An IValueConverter (Kotlin-implemented) that extracts Content (boxed string) out of a label object. */
        val labelContentConverter: KComObject by lazy {
            KComObject("WinUI4K.AnnotatedScrollBarLabelContentConverter")
                .addInterface(
                    Abi.IID_IValueConverter,
                    listOf(
                        // vtbl[6] Convert(this, value, targetType, parameter, language, out result)
                        KComObject.Method(DESC_CONVERT) { args ->
                            val value = args[1] as Ptr
                            val out = args[5] as Ptr
                            var content = Ptr.NULL
                            if (!value.isNull) {
                                val label = ComPtr(value)
                                    .queryInterfaceOrNull(Abi.IID_IAnnotatedScrollBarLabel)
                                if (label != null) {
                                    // Transfer ownership into out (an out object is released by the caller)
                                    content = label.getPtrOrNull(Abi.IAnnotatedScrollBarLabel_get_Content)
                                        ?.ptr ?: Ptr.NULL
                                    label.release()
                                }
                            }
                            Ffi.backend.memory.putPtr(out, 0, content)
                            KComObject.S_OK
                        },
                        // vtbl[7] ConvertBack — not used, since this binding is one-way
                        KComObject.Method(DESC_CONVERT) { args ->
                            Ffi.backend.memory.putPtr(args[5] as Ptr, 0, Ptr.NULL)
                            E_NOTIMPL
                        },
                    ),
                )
        }

        /** True once registered. Registered into Application.Resources exactly once per process. */
        var converterRegistered = false

        /** Registers [labelContentConverter] into Application.Resources so it can be referenced via `{StaticResource}`. */
        fun registerLabelContentConverter() {
            if (converterRegistered) return
            WinUiUtilities.insertApplicationResource(CONVERTER_RESOURCE_KEY, labelContentConverter.primary)
            converterRegistered = true
        }
    }
}
