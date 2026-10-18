package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.nio.file.Paths

/**
 * javax.swing.Icon-like: an icon shown on a button ([WButton.icon]) and elsewhere.
 * There are the predefined icons [Symbol], image file icons [WImageIcon], and font glyph icons [WFontIcon].
 * A WinUI IconElement (SymbolIcon / ImageIcon / FontIcon) is created each time it is shown, so the same WIcon may be
 * passed to multiple buttons.
 * The display mechanism is internal to the library, so implementations other than these three cannot be created (sealed).
 */
sealed interface WIcon

/**
 * Creates a new IconElement that shows this icon. The caller must release it.
 * (Kotlin interfaces cannot have internal members, so this dispatches with a branch over the sealed types)
 */
internal fun WIcon.createIconElement(): ComPtr = when (this) {
    is Symbol -> createSymbolIconElement()
    is WImageIcon -> createImageIconElement()
    is WFontIcon -> createFontIconElement()
}

/**
 * ImageIcon-like: an icon from an image file (.png / .jpg / .bmp / .ico / .gif, etc.) (WinUI 3's ImageIcon + BitmapImage).
 * The image is shown at its original size (same as Swing's ImageIcon; it is not scaled up or down).
 *
 * Pass a file path to [path], as with ImageIcon(String filename) ("C:/icons/save.png", "C:\\icons\\save.png", or a path
 * relative to the working directory, "icons/save.png"). URIs starting with "file:///", "ms-appx:///", "http://",
 * "https://", and so on are also accepted as is (a string with a scheme of two or more characters is treated as a URI;
 * "C:" is treated as a drive name).
 * The path is converted to an absolute file URI ([uri]) at creation time. A failure to load does not throw an exception;
 * nothing is shown.
 * Equal if [uri] is equal ("C:/a.png" and "C:\\a.png", which point to the same file, are equal).
 */
class WImageIcon(
    /** The path or URI as passed (corresponds to the default value of ImageIcon.getDescription). */
    val path: String,
) : WIcon {
    /** The image URI. A file path is converted to an absolute file URI such as "file:///C:/icons/save.png". */
    val uri: String = toUri(path)

    @JvmSynthetic
    internal fun createImageIconElement(): ComPtr {
        val uriFactory = Activation.factory(FoundationInterop.CLS_Uri, FoundationInterop.IID_IUriRuntimeClassFactory)
        val nativeUri = try {
            Hstring.use(uri) { h -> uriFactory.getPtr(FoundationInterop.IUriRuntimeClassFactory_CreateUri, h) }
        } finally {
            uriFactory.release()
        }
        val bitmapFactory = Activation.factory(XamlInterop.CLS_BitmapImage, XamlInterop.IID_IBitmapImageFactory)
        val bitmap = try {
            bitmapFactory.getPtr(XamlInterop.IBitmapImageFactory_CreateInstanceWithUriSource, nativeUri.ptr)
        } finally {
            bitmapFactory.release()
            nativeUri.release()
        }
        val imageSource = try {
            bitmap.queryInterface(XamlInterop.IID_IImageSource)
        } finally {
            bitmap.release()
        }
        val imageIcon = Activation.composeDefault(XamlInterop.CLS_ImageIcon, XamlInterop.IID_IImageIconFactory)
        return try {
            imageIcon.call(XamlInterop.IImageIcon_put_Source, imageSource.ptr)
            imageIcon.queryInterface(XamlInterop.IID_IIconElement)
        } finally {
            imageSource.release()
            imageIcon.release()
        }
    }

    override fun equals(other: Any?): Boolean = other is WImageIcon && uri == other.uri

    override fun hashCode(): Int = uri.hashCode()

    override fun toString(): String = "WImageIcon($uri)"

    private companion object {
        /** A scheme of two or more characters such as "ms-appx:" or "https:" (one character is a drive name like "C:"). */
        val URI_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]+:")

        fun toUri(path: String): String {
            require(path.isNotEmpty()) { "The image path is empty" }
            if (URI_SCHEME.containsMatchIn(path)) return path
            // Path.toUri gives the form "file:///C:/..." (File.toURI's "file:/C:/..." is sometimes not accepted by
            // WinRT's Uri)
            return Paths.get(path).toAbsolutePath().toUri().toString()
        }
    }
}

/**
 * A font glyph icon (WinUI 3's FontIcon). [glyph] is the character to show; with the default font
 * "Segoe Fluent Icons", pass its code point (for example, "\uE74E" is Save). Use it for icons not in [Symbol].
 * Equal if [glyph] and [fontFamily] are equal.
 */
class WFontIcon @JvmOverloads constructor(
    /** The character to show (FontIcon.Glyph). */
    val glyph: String,
    /** The family name of the font that has the glyph (FontIcon.FontFamily). */
    val fontFamily: String = DEFAULT_FONT_FAMILY,
) : WIcon {
    init {
        require(glyph.isNotEmpty()) { "The glyph is empty" }
        require(fontFamily.isNotEmpty()) { "The font family name is empty" }
    }

    @JvmSynthetic
    internal fun createFontIconElement(): ComPtr {
        val fontIcon = Activation.composeDefault(XamlInterop.CLS_FontIcon, XamlInterop.IID_IFontIconFactory)
        return try {
            Hstring.use(glyph) { h -> fontIcon.call(XamlInterop.IFontIcon_put_Glyph, h) }
            val family = createFontFamily(fontFamily)
            try {
                fontIcon.call(XamlInterop.IFontIcon_put_FontFamily, family.ptr)
            } finally {
                family.release()
            }
            fontIcon.queryInterface(XamlInterop.IID_IIconElement)
        } finally {
            fontIcon.release()
        }
    }

    override fun equals(other: Any?): Boolean = other is WFontIcon && glyph == other.glyph && fontFamily == other.fontFamily

    override fun hashCode(): Int = glyph.hashCode() * HASH_FACTOR + fontFamily.hashCode()

    override fun toString(): String = "WFontIcon(glyph=$glyph, fontFamily=$fontFamily)"

    companion object {
        /** The Windows 11 system icon font. */
        const val DEFAULT_FONT_FAMILY = "Segoe Fluent Icons"

        private const val HASH_FACTOR = 31
    }
}
