package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

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
        inspectable.getPtr(Abi.IAnnotatedScrollBar_get_ScrollController)
    }

    /** The label collection (AnnotatedScrollBar.Labels, an IVector<AnnotatedScrollBarLabel>). */
    private val labels: ComPtr by lazy {
        inspectable.getPtr(Abi.IAnnotatedScrollBar_get_Labels)
    }

    /** The activatable factory that creates AnnotatedScrollBarLabel instances. */
    private val labelFactory: ComPtr by lazy {
        Activation.factory(Abi.CLS_AnnotatedScrollBarLabel, Abi.IID_IAnnotatedScrollBarLabelFactory)
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
     * Adds a label (marker) at the given offset (AnnotatedScrollBar.Labels.Append).
     * [content] is the label's content, and [offset] is the vertical scroll offset on the rail
     * (in pixels).
     *
     * Note: markers on the rail are drawn via the template installed by
     * [applyMarkerLabelTemplate], and the [content] string itself is not shown on the rail
     * (see the LabelTemplate limitation described below). Consider using
     * [addDetailLabelRequestedListener]'s hover tooltip to convey what each offset means.
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
     * Draws the marker on the rail (AnnotatedScrollBar.LabelTemplate) as a small fixed indicator
     * of position.
     *
     * Limitation: ideally the label's string (AnnotatedScrollBarLabel.Content) would be shown,
     * but a classic Binding in a DataTemplate loaded via XamlReader can't resolve Content (the
     * real WinUI 3 Gallery uses compiled x:Bind, and winui4k has no x:Bind equivalent). So we
     * draw a fixed indicator instead of the string, and convey what each position means via the
     * DetailLabelRequested hover tooltip.
     */
    private fun applyMarkerLabelTemplate() {
        val statics = Activation.factory(Abi.CLS_XamlReader, Abi.IID_IXamlReaderStatics)
        val template = Hstring.use(MARKER_LABEL_TEMPLATE_XAML) { h ->
            statics.getPtr(Abi.IXamlReaderStatics_Load, h)
        }
        statics.release()
        val elementFactory = template.queryInterface(Abi.IID_IElementFactory)
        template.release()
        inspectable.call(Abi.IAnnotatedScrollBar_put_LabelTemplate, elementFactory.ptr)
        elementFactory.release()
    }

    init {
        applyMarkerLabelTemplate()
    }

    private companion object {
        /** A position marker on the rail (a small dot). Fixed, since the string can't be obtained via Binding. */
        const val MARKER_LABEL_TEMPLATE_XAML =
            "<DataTemplate xmlns=\"http://schemas.microsoft.com/winfx/2006/xaml/presentation\">" +
                "<Border Width=\"10\" Height=\"10\" CornerRadius=\"5\" Background=\"#0078D4\" " +
                "VerticalAlignment=\"Center\" />" +
                "</DataTemplate>"
    }
}
