package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for the file/folder pickers
 * (Microsoft.Windows.Storage.Pickers, WinAppSDK).
 *
 * All values were mechanically extracted from Microsoft.Windows.Storage.Pickers.winmd
 * (WinAppSDK Foundation 2.1.0) via tools/dump_winmd.py. Not a single value is
 * hand-written or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, and the interface body
 * starts at 6 in the winmd's method-declaration order.
 */
internal object StoragePickersInterop {
    // For all three classes, the activatable factory's CreateInstance(WindowId) -> picker is vtbl[6]
    const val IPickerFactory_CreateInstance = 6

    // ---- Microsoft.Windows.Storage.Pickers.FileOpenPicker ----
    const val CLS_FileOpenPicker = "Microsoft.Windows.Storage.Pickers.FileOpenPicker"
    const val IID_IFileOpenPickerFactory = "315e86d7-d7a2-5d81-b379-7af78207b1af"
    const val IID_IFileOpenPicker = "9d00f175-c783-51bd-8c93-fb63695d3abc"
    const val IFileOpenPicker_get_ViewMode = 6             // get_ViewMode(out PickerViewMode)
    const val IFileOpenPicker_put_ViewMode = 7             // put_ViewMode(PickerViewMode)
    const val IFileOpenPicker_get_SuggestedStartLocation = 8
    const val IFileOpenPicker_put_SuggestedStartLocation = 9 // put_SuggestedStartLocation(PickerLocationId)
    const val IFileOpenPicker_get_CommitButtonText = 10
    const val IFileOpenPicker_put_CommitButtonText = 11    // put_CommitButtonText(HSTRING)
    const val IFileOpenPicker_get_FileTypeFilter = 12      // get_FileTypeFilter(out IVector<HSTRING>)
    const val IFileOpenPicker_PickSingleFileAsync = 13     // () -> IAsyncOperation<PickFileResult>
    const val IFileOpenPicker_PickMultipleFilesAsync = 14  // () -> IAsyncOperation<IVectorView<PickFileResult>>

    // ---- Microsoft.Windows.Storage.Pickers.FileSavePicker ----
    const val CLS_FileSavePicker = "Microsoft.Windows.Storage.Pickers.FileSavePicker"
    const val IID_IFileSavePickerFactory = "2e256696-30b6-5a05-a8f5-c752db6dd268"
    const val IID_IFileSavePicker = "79f1f4df-741b-59b2-aa06-fe9ac817b7dd"
    const val IFileSavePicker_get_SuggestedStartLocation = 6
    const val IFileSavePicker_put_SuggestedStartLocation = 7 // put_SuggestedStartLocation(PickerLocationId)
    const val IFileSavePicker_get_CommitButtonText = 8
    const val IFileSavePicker_put_CommitButtonText = 9     // put_CommitButtonText(HSTRING)
    const val IFileSavePicker_get_FileTypeChoices = 10     // get_FileTypeChoices(out IMap<HSTRING, IVector<HSTRING>>)
    const val IFileSavePicker_get_DefaultFileExtension = 11
    const val IFileSavePicker_put_DefaultFileExtension = 12 // put_DefaultFileExtension(HSTRING)
    const val IFileSavePicker_get_SuggestedFileName = 13
    const val IFileSavePicker_put_SuggestedFileName = 14   // put_SuggestedFileName(HSTRING)
    const val IFileSavePicker_get_SuggestedFolder = 15
    const val IFileSavePicker_put_SuggestedFolder = 16     // put_SuggestedFolder(HSTRING)
    const val IFileSavePicker_PickSaveFileAsync = 17       // () -> IAsyncOperation<PickFileResult>

    // ---- Microsoft.Windows.Storage.Pickers.FolderPicker ----
    const val CLS_FolderPicker = "Microsoft.Windows.Storage.Pickers.FolderPicker"
    const val IID_IFolderPickerFactory = "e1550d89-b389-5886-8395-022b1588d6a8"
    const val IID_IFolderPicker = "3ef0d1ca-97c6-5873-8ea2-02c450174290"
    const val IFolderPicker_get_ViewMode = 6               // get_ViewMode(out PickerViewMode)
    const val IFolderPicker_put_ViewMode = 7               // put_ViewMode(PickerViewMode)
    const val IFolderPicker_get_SuggestedStartLocation = 8
    const val IFolderPicker_put_SuggestedStartLocation = 9 // put_SuggestedStartLocation(PickerLocationId)
    const val IFolderPicker_get_CommitButtonText = 10
    const val IFolderPicker_put_CommitButtonText = 11      // put_CommitButtonText(HSTRING)
    const val IFolderPicker_PickSingleFolderAsync = 12     // () -> IAsyncOperation<PickFolderResult>

    // ---- Microsoft.Windows.Storage.Pickers.PickFileResult / PickFolderResult ----
    const val IID_IPickFileResult = "e6f2e3d6-7bb0-5d81-9e7d-6fd35a1f25ab"
    const val IID_IPickFolderResult = "6f7fd316-fe29-59d1-9343-c49cf5cde680"
    const val IPickResult_get_Path = 6                     // get_Path(out HSTRING) is vtbl[6] for both classes

    // The enum PickerViewMode / PickerLocationId values are defined on the public API side (WFileOpenPicker.kt)

    /** Concrete IID of AsyncOperationCompletedHandler<PickFileResult> (PickSingleFileAsync / PickSaveFileAsync). */
    val IID_AsyncOperationCompletedHandler_PickFileResult: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "rc(Microsoft.Windows.Storage.Pickers.PickFileResult;{$IID_IPickFileResult}))",
        )
    }

    /** Concrete IID of AsyncOperationCompletedHandler<IVectorView<PickFileResult>> (PickMultipleFilesAsync). */
    val IID_AsyncOperationCompletedHandler_PickFileResultList: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "pinterface({${FoundationInterop.IID_IVectorView_OPEN}};" +
                "rc(Microsoft.Windows.Storage.Pickers.PickFileResult;{$IID_IPickFileResult})))",
        )
    }

    /** Concrete IID of AsyncOperationCompletedHandler<PickFolderResult> (PickSingleFolderAsync). */
    val IID_AsyncOperationCompletedHandler_PickFolderResult: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "rc(Microsoft.Windows.Storage.Pickers.PickFolderResult;{$IID_IPickFolderResult}))",
        )
    }
}
