package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.StoragePickersInterop
import java.util.function.Consumer

/**
 * JFileChooser's folder selection (DIRECTORIES_ONLY)-like: the WinAppSDK's FolderPicker.
 * Shows the OS folder-selection dialog modally over [owner]'s window and
 * receives the selected folder's path.
 *
 * The dialog stays open until the user closes it, so the result is delivered via a callback
 * instead of waiting ([pickSingleFolder]). Callbacks run on the UI thread.
 */
class WFolderPicker(owner: WFrame) {
    private val picker: ComPtr = StoragePickers.create(
        StoragePickersInterop.CLS_FolderPicker,
        StoragePickersInterop.IID_IFolderPickerFactory,
        owner,
    )

    init {
        // Release the picker on the UI thread once it becomes unreachable to GC (same mechanism as WComponent)
        ComLifetime.adopt(this, picker)
    }

    /** How the dialog's list is displayed (FolderPicker.ViewMode). Only applies on the first showing. */
    var viewMode: PickerViewMode
        get() = PickerViewMode.of(picker.getInt(StoragePickersInterop.IFolderPicker_get_ViewMode))
        set(value) = picker.call(StoragePickersInterop.IFolderPicker_put_ViewMode, value.native)

    /** The folder shown first (FolderPicker.SuggestedStartLocation). Only applies on the first showing. */
    var suggestedStartLocation: PickerLocationId
        get() = PickerLocationId.of(picker.getInt(StoragePickersInterop.IFolderPicker_get_SuggestedStartLocation))
        set(value) = picker.call(StoragePickersInterop.IFolderPicker_put_SuggestedStartLocation, value.native)

    /** The commit button's caption (FolderPicker.CommitButtonText). Empty means the OS default ("Select Folder"). */
    var commitButtonText: String
        get() = picker.getString(StoragePickersInterop.IFolderPicker_get_CommitButtonText)
        set(value) = Hstring.use(value) { h ->
            picker.call(StoragePickersInterop.IFolderPicker_put_CommitButtonText, h)
        }

    /**
     * Opens the folder-selection dialog (FolderPicker.PickSingleFolderAsync).
     * Passes the path of the folder the user picked (null on cancel) to [onPicked] on the UI thread.
     */
    fun pickSingleFolder(onPicked: Consumer<String?>) {
        Async.onPtrResult(
            picker.getPtr(StoragePickersInterop.IFolderPicker_PickSingleFolderAsync),
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFolderResult,
            "FolderPicker.PickSingleFolderAsync",
        ) { result ->
            val path = StoragePickers.pathOf(result)
            WinUiUtilities.invokeLater { onPicked.accept(path) }
        }
    }
}
