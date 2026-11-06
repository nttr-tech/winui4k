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
 * [title] / [suggestedFolder] are features from Windows App SDK 2.3 (IFolderPicker2 etc.); if the runtime does not have them,
 * reading returns empty ("") and setting throws [UnsupportedOperationException].
 */
class WFolderPicker internal constructor(windowId: Long) {
    private val picker: ComPtr = StoragePickers.create(
        StoragePickersInterop.CLS_FolderPicker,
        StoragePickersInterop.IID_IFolderPickerFactory,
        windowId,
    )

    /** Creates a picker that opens on the window of [owner]. */
    constructor(owner: WFrame) : this(owner.appWindow.id)

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

    /** The dialog title (FolderPicker.Title). Empty means the OS default. */
    var title: String
        get() = StoragePickers.withV2(picker, StoragePickersInterop.IID_IFolderPicker2) {
            it.getString(StoragePickersInterop.IFolderPicker2_get_Title)
        }.orEmpty()
        set(value) = StoragePickers.requireV2(picker, StoragePickersInterop.IID_IFolderPicker2, "FolderPicker.Title") {
            Hstring.use(value) { h -> it.call(StoragePickersInterop.IFolderPicker2_put_Title, h) }
        }

    /**
     * The absolute path of the folder shown when opened (FolderPicker.SuggestedFolder). Takes precedence over [suggestedStartLocation] and the last location.
     * Empty means not specified. A folder that does not exist is ignored.
     */
    var suggestedFolder: String
        get() = StoragePickers.withV2(picker, StoragePickersInterop.IID_IFolderPicker2) {
            it.getString(StoragePickersInterop.IFolderPicker2_get_SuggestedFolder)
        }.orEmpty()
        set(value) = StoragePickers.requireV2(picker, StoragePickersInterop.IID_IFolderPicker2, "FolderPicker.SuggestedFolder") {
            Hstring.use(value) { h -> it.call(StoragePickersInterop.IFolderPicker2_put_SuggestedFolder, h) }
        }

    /** A version of [pickSingleFolder] that receives failures with [onError] (for WFileChooser). */
    internal fun pickSingleFolder(onError: (RuntimeException) -> Unit, onPicked: (String?) -> Unit) {
        StoragePickers.pick(
            { picker.getPtr(StoragePickersInterop.IFolderPicker_PickSingleFolderAsync) },
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFolderResult,
            "FolderPicker.PickSingleFolderAsync",
            StoragePickers::pathOf,
            StoragePickers.deliver(onError, onPicked),
        )
    }

    /**
     * Opens a dialog in which multiple folders can be selected (FolderPicker.PickMultipleFoldersAsync, from Windows App SDK 2.3).
     * Passes the list of selected folder paths (empty if canceled) to [onPicked] on the UI thread, and failures to [onError] (for WFileChooser).
     */
    internal fun pickMultipleFolders(onError: (RuntimeException) -> Unit, onPicked: (List<String>) -> Unit) {
        StoragePickers.pick(
            {
                StoragePickers.withV2(picker, StoragePickersInterop.IID_IFolderPicker2) {
                    it.getPtr(StoragePickersInterop.IFolderPicker2_PickMultipleFoldersAsync)
                } ?: throw UnsupportedOperationException("FolderPicker.PickMultipleFoldersAsync is not available in this Windows App SDK runtime")
            },
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFolderResultList,
            "FolderPicker.PickMultipleFoldersAsync",
            StoragePickers::pathsOf,
            StoragePickers.deliver(onError, onPicked),
        )
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
