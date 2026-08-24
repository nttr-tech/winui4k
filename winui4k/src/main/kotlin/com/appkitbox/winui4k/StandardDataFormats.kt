package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.DataTransferInterop

/**
 * Names of the standard clipboard data formats: Windows.ApplicationModel.DataTransfer.StandardDataFormats.
 * Used to match against [DataPackageView.contains] and [DataPackageView.availableFormats].
 * The values are fetched from the OS static properties on first reference.
 */
object StandardDataFormats {
    /** Plain text (StandardDataFormats.Text). */
    @JvmStatic
    val TEXT: String by lazy { statics(DataTransferInterop.IStandardDataFormatsStatics_get_Text) }

    /** The HTML clipboard format (StandardDataFormats.Html). */
    @JvmStatic
    val HTML: String by lazy { statics(DataTransferInterop.IStandardDataFormatsStatics_get_Html) }

    /** Rich text (StandardDataFormats.Rtf). */
    @JvmStatic
    val RTF: String by lazy { statics(DataTransferInterop.IStandardDataFormatsStatics_get_Rtf) }

    /** A bitmap image (StandardDataFormats.Bitmap). */
    @JvmStatic
    val BITMAP: String by lazy { statics(DataTransferInterop.IStandardDataFormatsStatics_get_Bitmap) }

    /** Files/folders (StandardDataFormats.StorageItems). */
    @JvmStatic
    val STORAGE_ITEMS: String by lazy { statics(DataTransferInterop.IStandardDataFormatsStatics_get_StorageItems) }

    /** A web page URI (StandardDataFormats.WebLink). The format [DataPackage.setUri] sets. */
    @JvmStatic
    val WEB_LINK: String by lazy { statics2(DataTransferInterop.IStandardDataFormatsStatics2_get_WebLink) }

    /** A URI to in-app content (StandardDataFormats.ApplicationLink). */
    @JvmStatic
    val APPLICATION_LINK: String by lazy { statics2(DataTransferInterop.IStandardDataFormatsStatics2_get_ApplicationLink) }

    private fun statics(slot: Int): String = read(DataTransferInterop.IID_IStandardDataFormatsStatics, slot)

    private fun statics2(slot: Int): String = read(DataTransferInterop.IID_IStandardDataFormatsStatics2, slot)

    private fun read(iid: String, slot: Int): String {
        val statics = Activation.factory(DataTransferInterop.CLS_StandardDataFormats, iid)
        return try {
            statics.getString(slot)
        } finally {
            statics.release()
        }
    }
}
