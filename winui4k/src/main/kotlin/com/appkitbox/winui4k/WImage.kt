package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.getString
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
        @JvmSynthetic
        fun of(native: Int): Stretch = entries.first { it.native == native }
    }
}

/**
 * JLabel + ImageIcon-like: WinUI 3's Image.
 *
 * The image is specified with [sourceUri] (a file path or URI; .png / .ico etc. like "file:///C:/...") or [icon]
 * ([WImageIcon]).
 * It can be created from a file path like `WImage("C:/images/logo.png")`, or in the same form as JLabel(Icon) like
 * `WImage(WImageIcon("logo.png"))`.
 * [stretch] controls how it's fit into its allotted area.
 */
class WImage @JvmOverloads constructor(sourceUri: String? = null) : WComponent(
    Activation.activate(XamlInterop.CLS_Image, XamlInterop.IID_IImage), // created via the default factory
) {
    /** JLabel(Icon image)-like: creates an Image that shows the image of [icon]. */
    constructor(icon: WIcon) : this(null) {
        this.icon = icon
    }

    /**
     * The file path or URI of the image to show (Image.Source). null hides it.
     * As with [WImageIcon], "C:/images/logo.png", "C:\\images\\logo.png" and paths relative to the working directory are
     * converted to absolute file URIs, and URIs such as "file:///", "ms-appx:///", "http://" and "https://" are used as is.
     * Reads return the native BitmapImage.UriSource as the passed string (in file URI form when set with a path).
     * Images load asynchronously; if one cannot be loaded, no exception is thrown and nothing is shown.
     * The URI of images other than BitmapImage (such as those set in XAML) cannot be read, so it is null.
     */
    var sourceUri: String?
        get() = readSourceUri()
        set(value) {
            if (value == null) {
                inspectable.call(XamlInterop.IImage_put_Source, null)
                return
            }
            val imageSource = createImageSource(WImageIcon(value).uri)
            try {
                inspectable.call(XamlInterop.IImage_put_Source, imageSource.ptr)
            } finally {
                imageSource.release()
            }
        }

    /**
     * JLabel.getIcon / setIcon-like: the icon of the image to show. Equivalent to [sourceUri] wrapped in a [WImageIcon];
     * setting it shows the image at [WImageIcon.uri]. null hides it.
     * The image is scaled to fit the allotted area ([stretch] defaults to [Stretch.UNIFORM]; use [Stretch.NONE] to show
     * it at its original size).
     * Reads return a new [WImageIcon] equal to the one passed (same URI).
     *
     * Image can only show image files, so [Symbol] and [WFontIcon] cannot be set.
     *
     * @throws IllegalArgumentException for an icon other than [WImageIcon] (the image does not change)
     */
    var icon: WIcon?
        get() = sourceUri?.let { WImageIcon(it) }
        set(value) {
            if (value == null) {
                sourceUri = null
                return
            }
            value.requireSupported()
            val image: Any = value
            require(image is WImageIcon) { "WImage can only show a WImageIcon: ${image.javaClass.name} (Symbol and WFontIcon are for IconElement)" }
            sourceUri = image.uri
        }

    /** If Image.Source is a BitmapImage, the original string of its UriSource; otherwise null. */
    private fun readSourceUri(): String? {
        val source = inspectable.getPtrOrNull(XamlInterop.IImage_get_Source) ?: return null
        try {
            val bitmap = source.queryInterfaceOrNull(XamlInterop.IID_IBitmapImage) ?: return null
            try {
                val uri = bitmap.getPtrOrNull(XamlInterop.IBitmapImage_get_UriSource) ?: return null
                try {
                    return uri.getString(FoundationInterop.IUriRuntimeClass_get_RawUri)
                } finally {
                    uri.release()
                }
            } finally {
                bitmap.release()
            }
        } finally {
            source.release()
        }
    }

    /** How the image is fit into its allotted area (Image.Stretch). */
    var stretch: Stretch
        get() = Stretch.of(inspectable.getInt(XamlInterop.IImage_get_Stretch))
        set(value) = inspectable.call(XamlInterop.IImage_put_Stretch, value.native)

    init {
        if (sourceUri != null) this.sourceUri = sourceUri
    }
}
