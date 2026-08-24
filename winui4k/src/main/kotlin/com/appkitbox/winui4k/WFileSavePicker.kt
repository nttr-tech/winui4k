package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.StoragePickersInterop
import java.util.function.Consumer

/**
 * JFileChooser's "Save As" dialog-like: the WinAppSDK's FileSavePicker.
 * Shows the OS save dialog modally over [owner]'s window and receives the destination path.
 *
 * The picker only returns a path — it doesn't create the file — so writing to the received
 * path is the caller's job. The result is delivered via a callback instead of waiting
 * ([pickSaveFile]). Callbacks run on the UI thread.
 */
class WFileSavePicker(owner: WFrame) {
    private val picker: ComPtr = StoragePickers.create(
        StoragePickersInterop.CLS_FileSavePicker,
        StoragePickersInterop.IID_IFileSavePickerFactory,
        owner,
    )

    init {
        // Release the picker on the UI thread once it becomes unreachable to GC (same mechanism as WComponent)
        ComLifetime.adopt(this, picker)
    }

    /** The folder shown first (FileSavePicker.SuggestedStartLocation). Only applies on the first showing. */
    var suggestedStartLocation: PickerLocationId
        get() = PickerLocationId.of(picker.getInt(StoragePickersInterop.IFileSavePicker_get_SuggestedStartLocation))
        set(value) = picker.call(StoragePickersInterop.IFileSavePicker_put_SuggestedStartLocation, value.native)

    /** The commit button's caption (FileSavePicker.CommitButtonText). Empty means the OS default ("Save"). */
    var commitButtonText: String
        get() = picker.getString(StoragePickersInterop.IFileSavePicker_get_CommitButtonText)
        set(value) = Hstring.use(value) { h ->
            picker.call(StoragePickersInterop.IFileSavePicker_put_CommitButtonText, h)
        }

    /** The extension selected by default in "Save as type" (FileSavePicker.DefaultFileExtension). ".txt" form. */
    var defaultFileExtension: String
        get() = picker.getString(StoragePickersInterop.IFileSavePicker_get_DefaultFileExtension)
        set(value) = Hstring.use(value) { h ->
            picker.call(StoragePickersInterop.IFileSavePicker_put_DefaultFileExtension, h)
        }

    /** The name pre-filled in the file name field (FileSavePicker.SuggestedFileName). */
    var suggestedFileName: String
        get() = picker.getString(StoragePickersInterop.IFileSavePicker_get_SuggestedFileName)
        set(value) = Hstring.use(value) { h ->
            picker.call(StoragePickersInterop.IFileSavePicker_put_SuggestedFileName, h)
        }

    /**
     * The absolute path of the folder shown first (FileSavePicker.SuggestedFolder).
     * Takes precedence over [suggestedStartLocation]. Empty means unspecified.
     */
    var suggestedFolder: String
        get() = picker.getString(StoragePickersInterop.IFileSavePicker_get_SuggestedFolder)
        set(value) = Hstring.use(value) { h ->
            picker.call(StoragePickersInterop.IFileSavePicker_put_SuggestedFolder, h)
        }

    /**
     * Adds a "Save as type" choice (FileSavePicker.FileTypeChoices).
     * [displayName] is a display name like "Text Files"; [extensions] are extensions in ".txt" form.
     */
    fun addFileTypeChoice(displayName: String, vararg extensions: String) {
        val choices = picker.getPtr(StoragePickersInterop.IFileSavePicker_get_FileTypeChoices)
        try {
            val vector = StringVector(extensions.toList())
            Ffi.backend.withScope { scope ->
                val replaced = scope.allocate(1, 1) // Insert(key, value, out boolean replaced)
                Hstring.use(displayName) { key ->
                    choices.call(FoundationInterop.IMap_Insert, key, vector.comObject.primary, replaced)
                }
            }
            vector.comObject.release() // Insert holds a reference to it; the picker's Release reclaims it
        } finally {
            choices.release()
        }
    }

    /**
     * Opens the save dialog (FileSavePicker.PickSaveFileAsync).
     * Passes the destination path the user decided on (null on cancel) to [onPicked] on the UI thread.
     */
    fun pickSaveFile(onPicked: Consumer<String?>) {
        Async.onPtrResult(
            picker.getPtr(StoragePickersInterop.IFileSavePicker_PickSaveFileAsync),
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFileResult,
            "FileSavePicker.PickSaveFileAsync",
        ) { result ->
            val path = StoragePickers.pathOf(result)
            WinUiUtilities.invokeLater { onPicked.accept(path) }
        }
    }
}
