package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for the clipboard
 * (Windows.ApplicationModel.DataTransfer, OS-side).
 * Also includes the Windows.Storage types used by SetStorageItems / GetStorageItemsAsync.
 *
 * All values were mechanically extracted from Windows.Foundation.UniversalApiContract.winmd
 * (Windows SDK contracts) via tools/dump_winmd.py. Not a single value is hand-written or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, and the interface body
 * starts at 6 in the winmd's method-declaration order.
 */
internal object DataTransferInterop {
    // ---- Windows.ApplicationModel.DataTransfer.Clipboard (a static class) ----
    const val CLS_Clipboard = "Windows.ApplicationModel.DataTransfer.Clipboard"
    const val IID_IClipboardStatics = "c627e291-34e2-4963-8eed-93cbb0ea3d70"
    const val IClipboardStatics_GetContent = 6             // GetContent(out DataPackageView)
    const val IClipboardStatics_SetContent = 7             // SetContent(DataPackage)
    const val IClipboardStatics_Flush = 8
    const val IClipboardStatics_Clear = 9
    const val IClipboardStatics_add_ContentChanged = 10    // add(EventHandler<Object>, out token)
    const val IClipboardStatics_remove_ContentChanged = 11

    const val IID_IClipboardStatics2 = "d2ac1b6a-d29f-554b-b303-f0452345fe02"
    const val IClipboardStatics2_ClearHistory = 7          // ClearHistory(out boolean)
    const val IClipboardStatics2_IsHistoryEnabled = 10     // IsHistoryEnabled(out boolean)
    const val IClipboardStatics2_IsRoamingEnabled = 11     // IsRoamingEnabled(out boolean)
    const val IClipboardStatics2_SetContentWithOptions = 12 // SetContentWithOptions(DataPackage, ClipboardContentOptions, out boolean)

    // ---- Windows.ApplicationModel.DataTransfer.DataPackage ----
    const val CLS_DataPackage = "Windows.ApplicationModel.DataTransfer.DataPackage"
    const val IID_IDataPackage = "61ebf5c7-efea-4346-9554-981d7e198ffe"
    const val IDataPackage_get_RequestedOperation = 8      // get_RequestedOperation(out DataPackageOperation)
    const val IDataPackage_put_RequestedOperation = 9      // put_RequestedOperation(DataPackageOperation)
    const val IDataPackage_SetText = 16                    // SetText(HSTRING)
    const val IDataPackage_SetUri = 17                     // SetUri(Windows.Foundation.Uri)
    const val IDataPackage_SetHtmlFormat = 18              // SetHtmlFormat(HSTRING)
    const val IDataPackage_SetRtf = 20                     // SetRtf(HSTRING)
    const val IDataPackage_SetStorageItems = 22            // SetStorageItems(IIterable<IStorageItem>)

    // ---- Windows.ApplicationModel.DataTransfer.DataPackageView ----
    const val IID_IDataPackageView = "7b840471-5900-4d85-a90b-10cb85fe3552"
    const val IDataPackageView_get_RequestedOperation = 7  // get_RequestedOperation(out DataPackageOperation)
    const val IDataPackageView_get_AvailableFormats = 9    // get_AvailableFormats(out IVectorView<HSTRING>)
    const val IDataPackageView_Contains = 10               // Contains(HSTRING, out boolean)
    const val IDataPackageView_GetTextAsync = 12           // () -> IAsyncOperation<HSTRING>
    const val IDataPackageView_GetUriAsync = 14            // () -> IAsyncOperation<Uri>
    const val IDataPackageView_GetHtmlFormatAsync = 15     // () -> IAsyncOperation<HSTRING>
    const val IDataPackageView_GetRtfAsync = 17            // () -> IAsyncOperation<HSTRING>
    const val IDataPackageView_GetStorageItemsAsync = 19   // () -> IAsyncOperation<IVectorView<IStorageItem>>

    // ---- Windows.ApplicationModel.DataTransfer.ClipboardContentOptions ----
    const val CLS_ClipboardContentOptions = "Windows.ApplicationModel.DataTransfer.ClipboardContentOptions"
    const val IID_IClipboardContentOptions = "e888a98c-ad4b-5447-a056-ab3556276d2b"
    const val IClipboardContentOptions_get_IsRoamable = 6
    const val IClipboardContentOptions_put_IsRoamable = 7  // put_IsRoamable(boolean)
    const val IClipboardContentOptions_get_IsAllowedInHistory = 8
    const val IClipboardContentOptions_put_IsAllowedInHistory = 9 // put_IsAllowedInHistory(boolean)

    // ---- Windows.ApplicationModel.DataTransfer.StandardDataFormats (a static class) ----
    const val CLS_StandardDataFormats = "Windows.ApplicationModel.DataTransfer.StandardDataFormats"
    const val IID_IStandardDataFormatsStatics = "7ed681a1-a880-40c9-b4ed-0bee1e15f549"
    const val IStandardDataFormatsStatics_get_Text = 6
    const val IStandardDataFormatsStatics_get_Uri = 7
    const val IStandardDataFormatsStatics_get_Html = 8
    const val IStandardDataFormatsStatics_get_Rtf = 9
    const val IStandardDataFormatsStatics_get_Bitmap = 10
    const val IStandardDataFormatsStatics_get_StorageItems = 11
    const val IID_IStandardDataFormatsStatics2 = "42a254f4-9d76-42e8-861b-47c25dd0cf71"
    const val IStandardDataFormatsStatics2_get_WebLink = 6
    const val IStandardDataFormatsStatics2_get_ApplicationLink = 7

    // The enum DataPackageOperation values are defined on the public API side (DataPackage.kt)

    // ---- Windows.Storage.StorageFile / IStorageItem (file items passed to SetStorageItems) ----
    const val CLS_StorageFile = "Windows.Storage.StorageFile"
    const val IID_IStorageFileStatics = "5984c710-daf2-43c8-8bb4-a4d3eacfd03f"
    const val IStorageFileStatics_GetFileFromPathAsync = 6 // GetFileFromPathAsync(HSTRING) -> IAsyncOperation<StorageFile>
    const val IID_IStorageFile = "fa3f6186-4214-428c-a64c-14c9ac7315ea"
    const val IID_IStorageItem = "4207a996-ca2f-42f7-bde8-8b10457a7f30"
    const val IStorageItem_get_Path = 12                   // get_Path(out HSTRING)

    // ---- Windows.Foundation.Uri (used by SetUri / GetUriAsync; the factory is in FoundationInterop) ----
    const val IID_IUriRuntimeClass = "9e365e57-48b2-4160-956f-c7385120bbfc"

    /** Concrete IID of AsyncOperationCompletedHandler<String> (GetTextAsync / GetHtmlFormatAsync / GetRtfAsync). */
    val IID_AsyncOperationCompletedHandler_String: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};string)")
    }

    /** Concrete IID of AsyncOperationCompletedHandler<Uri> (GetUriAsync). */
    val IID_AsyncOperationCompletedHandler_Uri: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "rc(Windows.Foundation.Uri;{$IID_IUriRuntimeClass}))",
        )
    }

    /** Concrete IID of AsyncOperationCompletedHandler<StorageFile> (GetFileFromPathAsync). */
    val IID_AsyncOperationCompletedHandler_StorageFile: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "rc(Windows.Storage.StorageFile;{$IID_IStorageFile}))",
        )
    }

    /** Concrete IID of AsyncOperationCompletedHandler<IVectorView<IStorageItem>> (GetStorageItemsAsync). */
    val IID_AsyncOperationCompletedHandler_StorageItemList: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "pinterface({${FoundationInterop.IID_IVectorView_OPEN}};{$IID_IStorageItem}))",
        )
    }

    /** Concrete IID of IIterable<IStorageItem> (claimed by the Kotlin implementation passed to SetStorageItems). */
    val IID_IIterable_StorageItem: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterable_OPEN}};{$IID_IStorageItem})")
    }

    /** Concrete IID of IIterator<IStorageItem>. */
    val IID_IIterator_StorageItem: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterator_OPEN}};{$IID_IStorageItem})")
    }
}
