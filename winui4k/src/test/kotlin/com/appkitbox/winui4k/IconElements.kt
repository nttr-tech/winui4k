package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** Helper for reading the contents of an IconElement (SymbolIcon / ImageIcon / FontIcon) in tests. Call on the UI thread. */
internal object IconElements {
    /**
     * Which WIcon [element] displays (the Symbol for a SymbolIcon, the image URI for an ImageIcon, and the glyph and font
     * for a FontIcon).
     */
    fun describe(element: ComPtr): String {
        element.queryInterfaceOrNull(XamlInterop.IID_ISymbolIcon)?.let { symbolIcon ->
            return try {
                "Symbol:" + Symbol.of(symbolIcon.getInt(XamlInterop.ISymbolIcon_get_Symbol)).name
            } finally {
                symbolIcon.release()
            }
        }
        element.queryInterfaceOrNull(XamlInterop.IID_IImageIcon)?.let { imageIcon ->
            return try {
                "Image:" + imageUri(imageIcon)
            } finally {
                imageIcon.release()
            }
        }
        element.queryInterfaceOrNull(XamlInterop.IID_IFontIcon)?.let { fontIcon ->
            return try {
                val family = fontIcon.getPtr(XamlInterop.IFontIcon_get_FontFamily)
                val familyName = try {
                    family.getString(XamlInterop.IFontFamily_get_Source)
                } finally {
                    family.release()
                }
                "Font:" + fontIcon.getString(XamlInterop.IFontIcon_get_Glyph) + "/" + familyName
            } finally {
                fontIcon.release()
            }
        }
        return if (isIconElement(element)) "IconElement" else "other"
    }

    /** Whether [element] is an IconElement. */
    fun isIconElement(element: ComPtr): Boolean {
        val icon = element.queryInterfaceOrNull(XamlInterop.IID_IIconElement) ?: return false
        icon.release()
        return true
    }

    private fun imageUri(imageIcon: ComPtr): String {
        val source = imageIcon.getPtrOrNull(XamlInterop.IImageIcon_get_Source) ?: return "(none)"
        try {
            val bitmap = source.queryInterface(XamlInterop.IID_IBitmapImage)
            try {
                val uri = bitmap.getPtrOrNull(XamlInterop.IBitmapImage_get_UriSource) ?: return "(none)"
                return try {
                    uri.getString(FoundationInterop.IUriRuntimeClass_get_AbsoluteUri)
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
}
