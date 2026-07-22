package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Media.Stretch (how an image is fit into its allotted area).
 * Values extracted from the winmd (None=0, Fill=1, Uniform=2, UniformToFill=3).
 */
enum class Stretch(internal val native: Int) {
    /** Displayed at its original size. */
    NONE(0),

    /** Stretched to fill the area, ignoring aspect ratio. */
    FILL(1),

    /** Scaled to the largest size that fits within the area while preserving aspect ratio (default). */
    UNIFORM(2),

    /** Scaled to the smallest size that covers the area while preserving aspect ratio (overflow is clipped). */
    UNIFORM_TO_FILL(3),
    ;

    internal companion object {
        fun of(native: Int): Stretch = entries.first { it.native == native }
    }
}

/**
 * JLabel + ImageIcon-like: WinUI 3's Image.
 *
 * Loads and displays an image from [sourceUri], a file URI ("file:///C:/..." pointing at a .png / .ico, etc.).
 * [stretch] controls how it's fit into its allotted area.
 */
class WImage(sourceUri: String? = null) : WComponent(
    Activation.activate(XamlInterop.CLS_Image, XamlInterop.IID_IImage), // created via the default factory
) {
    /**
     * The URI of the image to display (Image.Source). Null hides it.
     * Built as Uri -> BitmapImage -> ImageSource in that order, then set onto Source.
     */
    var sourceUri: String? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(XamlInterop.IImage_put_Source, null)
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
            inspectable.call(XamlInterop.IImage_put_Source, imageSource.ptr)
            imageSource.release()
        }

    /** How the image is fit into its allotted area (Image.Stretch). */
    var stretch: Stretch
        get() = Stretch.of(inspectable.getInt(XamlInterop.IImage_get_Stretch))
        set(value) = inspectable.call(XamlInterop.IImage_put_Stretch, value.native)

    init {
        if (sourceUri != null) this.sourceUri = sourceUri
    }
}
