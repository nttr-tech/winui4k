package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * WinUI 3's Controls.TitleBar. No Swing equivalent.
 * Set [WFrame.extendsContentIntoTitleBar] = true and pass this to [WFrame.setTitleBar] to build a
 * custom title bar (including things like a back button, pane-toggle button, or search box).
 *
 * Provides [title] / [subtitle] / [iconUri] / [isBackButtonVisible] / [isBackButtonEnabled] /
 * [isPaneToggleButtonVisible] / [leftHeader] / [content] / [rightHeader], plus
 * [addBackRequestedListener] / [addPaneToggleRequestedListener].
 */
class WTitleBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_TitleBar, XamlInterop.IID_ITitleBarFactory),
) {
    /** Event tokens for listeners registered via addBackRequestedListener. */
    private val backRequestedTokens = ListenerTokens<() -> Unit>()

    /** Event tokens for listeners registered via addPaneToggleRequestedListener. */
    private val paneToggleRequestedTokens = ListenerTokens<() -> Unit>()

    /** The heading string (TitleBar.Title). */
    var title: String
        get() = inspectable.getString(XamlInterop.ITitleBar_get_Title)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ITitleBar_put_Title, h) }

    /** A subtitle shown below the heading (TitleBar.Subtitle). */
    var subtitle: String
        get() = inspectable.getString(XamlInterop.ITitleBar_get_Subtitle)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ITitleBar_put_Subtitle, h) }

    /**
     * The URI of an icon image shown to the left of the title (TitleBar.IconSource). Hidden if null.
     * Accepts a file URI like "file:///C:/..." (.ico / .png etc.).
     * Builds Uri -> BitmapImage -> ImageIconSource in order, then sets the result to IconSource.
     */
    var iconUri: String? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(XamlInterop.ITitleBar_put_IconSource, null)
                return
            }
            val uriFactory = Activation.factory(FoundationInterop.CLS_Uri, FoundationInterop.IID_IUriRuntimeClassFactory)
            val uri = Hstring.use(value) { h ->
                uriFactory.getPtr(FoundationInterop.IUriRuntimeClassFactory_CreateUri, h)
            }
            uriFactory.release()
            val bitmapFactory = Activation.factory(XamlInterop.CLS_BitmapImage, XamlInterop.IID_IBitmapImageFactory)
            val bitmap = bitmapFactory.getPtr(XamlInterop.IBitmapImageFactory_CreateInstanceWithUriSource, uri.ptr)
            bitmapFactory.release()
            uri.release()
            val imageSource = bitmap.queryInterface(XamlInterop.IID_IImageSource)
            bitmap.release()
            val imageIconSource = Activation.composeDefault(XamlInterop.CLS_ImageIconSource, XamlInterop.IID_IImageIconSourceFactory)
            imageIconSource.call(XamlInterop.IImageIconSource_put_ImageSource, imageSource.ptr)
            imageSource.release()
            val iconSource = imageIconSource.queryInterface(XamlInterop.IID_IIconSource)
            imageIconSource.release()
            inspectable.call(XamlInterop.ITitleBar_put_IconSource, iconSource.ptr)
            iconSource.release()
        }

    /**
     * The glyph of a font icon shown to the left of the title (TitleBar.IconSource). null hides it.
     * Pass a Segoe Fluent Icons code point like "".
     * Builds a FontIconSource and assigns it to IconSource (this writes to the same property as
     * [iconUri]; whichever is set last wins).
     */
    var iconGlyph: String? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(XamlInterop.ITitleBar_put_IconSource, null)
                return
            }
            val fontIconSource = Activation.composeDefault(XamlInterop.CLS_FontIconSource, XamlInterop.IID_IFontIconSourceFactory)
            Hstring.use(value) { h -> fontIconSource.call(XamlInterop.IFontIconSource_put_Glyph, h) }
            val iconSource = fontIconSource.queryInterface(XamlInterop.IID_IIconSource)
            fontIconSource.release()
            inspectable.call(XamlInterop.ITitleBar_put_IconSource, iconSource.ptr)
            iconSource.release()
        }

    /** Whether the back button is shown (TitleBar.IsBackButtonVisible). */
    var isBackButtonVisible: Boolean
        get() = inspectable.getBool(XamlInterop.ITitleBar_get_IsBackButtonVisible)
        set(value) = inspectable.putBool(XamlInterop.ITitleBar_put_IsBackButtonVisible, value)

    /** Whether the back button is operable (the usual pattern is to set it false when there's no history). */
    var isBackButtonEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ITitleBar_get_IsBackButtonEnabled)
        set(value) = inspectable.putBool(XamlInterop.ITitleBar_put_IsBackButtonEnabled, value)

    /** Whether the pane-toggle button (the hamburger menu) is shown (TitleBar.IsPaneToggleButtonVisible). */
    var isPaneToggleButtonVisible: Boolean
        get() = inspectable.getBool(XamlInterop.ITitleBar_get_IsPaneToggleButtonVisible)
        set(value) = inspectable.putBool(XamlInterop.ITitleBar_put_IsPaneToggleButtonVisible, value)

    /** Optional content placed to the right of the back/pane-toggle buttons (TitleBar.LeftHeader). */
    var leftHeader: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.ITitleBar_put_LeftHeader, value?.uiElement?.ptr)
        }

    /** Optional content that replaces [title] / [subtitle] in the center (TitleBar.Content, e.g. a search box). */
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.ITitleBar_put_Content, value?.uiElement?.ptr)
        }

    /** Optional content placed to the left of the caption buttons (minimize/maximize/close) (TitleBar.RightHeader). */
    var rightHeader: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.ITitleBar_put_RightHeader, value?.uiElement?.ptr)
        }

    /** Subscribes to back button clicks (TitleBar.BackRequested). */
    fun addBackRequestedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TitleBarBackRequestedHandler",
            XamlInterop.IID_TitleBarEventHandler,
            XamlInterop.ITitleBar_add_BackRequested,
        ) { _, _ -> listener() }
        backRequestedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addBackRequestedListener]. */
    fun removeBackRequestedListener(listener: () -> Unit) {
        val token = backRequestedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITitleBar_remove_BackRequested, token)
    }

    /** Subscribes to pane-toggle button clicks (TitleBar.PaneToggleRequested). */
    fun addPaneToggleRequestedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TitleBarPaneToggleRequestedHandler",
            XamlInterop.IID_TitleBarEventHandler,
            XamlInterop.ITitleBar_add_PaneToggleRequested,
        ) { _, _ -> listener() }
        paneToggleRequestedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addPaneToggleRequestedListener]. */
    fun removePaneToggleRequestedListener(listener: () -> Unit) {
        val token = paneToggleRequestedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITitleBar_remove_PaneToggleRequested, token)
    }

    companion object {
        /** An ITitleBarStatics2 for SetIsDragRegion operations. */
        private val statics2: ComPtr by lazy {
            Activation.factory(XamlInterop.CLS_TitleBar, XamlInterop.IID_ITitleBarStatics2)
        }

        /**
         * Sets whether [component] is treated as a draggable region (the TitleBar.IsDragRegion
         * attached property). Passing null restores the default auto-detection (AutoRefreshDragRegions).
         */
        fun setIsDragRegion(component: WComponent, isDragRegion: Boolean?) {
            if (isDragRegion == null) {
                statics2.call(XamlInterop.ITitleBarStatics2_SetIsDragRegion, component.uiElement.ptr, null)
                return
            }
            val boxed = PropertyValues.boxBool(isDragRegion)
            val reference = boxed.queryInterface(FoundationInterop.IID_IReference_Boolean)
            statics2.call(XamlInterop.ITitleBarStatics2_SetIsDragRegion, component.uiElement.ptr, reference.ptr)
            reference.release()
            boxed.release()
        }
    }
}
