package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.StoragePickersInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.util.function.Consumer

/**
 * JFileChooser's "Open" dialog-like: the WinAppSDK's FileOpenPicker.
 * Shows the OS file-selection dialog modally over [owner]'s window and
 * receives the selected file's path.
 *
 * The dialog stays open until the user closes it, so the result is delivered via a callback
 * instead of waiting ([pickSingleFile] / [pickMultipleFiles]). Callbacks run on the UI thread.
 *
 * Note that the picker remembers and restores the last-selected location and view mode, so
 * [suggestedStartLocation] and [viewMode] only take effect on the first showing (before any
 * memory exists).
 */
class WFileOpenPicker(owner: WFrame) {
    private val picker: ComPtr = StoragePickers.create(
        StoragePickersInterop.CLS_FileOpenPicker,
        StoragePickersInterop.IID_IFileOpenPickerFactory,
        owner,
    )

    init {
        // Release the picker on the UI thread once it becomes unreachable to GC (same mechanism as WComponent)
        ComLifetime.adopt(this, picker)
    }

    /** How the dialog's file list is displayed (FileOpenPicker.ViewMode). */
    var viewMode: PickerViewMode
        get() = PickerViewMode.of(picker.getInt(StoragePickersInterop.IFileOpenPicker_get_ViewMode))
        set(value) = picker.call(StoragePickersInterop.IFileOpenPicker_put_ViewMode, value.native)

    /** The folder shown first (FileOpenPicker.SuggestedStartLocation). */
    var suggestedStartLocation: PickerLocationId
        get() = PickerLocationId.of(picker.getInt(StoragePickersInterop.IFileOpenPicker_get_SuggestedStartLocation))
        set(value) = picker.call(StoragePickersInterop.IFileOpenPicker_put_SuggestedStartLocation, value.native)

    /** The commit button's caption (FileOpenPicker.CommitButtonText). Empty means the OS default ("Open"). */
    var commitButtonText: String
        get() = picker.getString(StoragePickersInterop.IFileOpenPicker_get_CommitButtonText)
        set(value) = Hstring.use(value) { h ->
            picker.call(StoragePickersInterop.IFileOpenPicker_put_CommitButtonText, h)
        }

    /** The current list of file types (a snapshot of FileOpenPicker.FileTypeFilter). */
    val fileTypeFilter: List<String>
        get() {
            val filter = picker.getPtr(StoragePickersInterop.IFileOpenPicker_get_FileTypeFilter)
            try {
                return Ffi.backend.withScope { scope ->
                    val memory = Ffi.backend.memory
                    val size = scope.allocate(4)
                    filter.call(FoundationInterop.IVector_get_Size, size)
                    (0 until memory.getInt(size, 0)).map { i ->
                        val out = scope.allocate(8)
                        filter.call(FoundationInterop.IVector_GetAt, i, out)
                        val hstring = memory.getPtr(out, 0)
                        try {
                            Hstring.read(hstring)
                        } finally {
                            Hstring.free(hstring)
                        }
                    }
                }
            } finally {
                filter.release()
            }
        }

    /**
     * Adds selectable file types (FileOpenPicker.FileTypeFilter).
     * Pass an extension like ".txt", or "*" for all files.
     * Opening the dialog without adding any results in an HRESULT exception.
     */
    fun addFileTypeFilter(vararg extensions: String) {
        val filter = picker.getPtr(StoragePickersInterop.IFileOpenPicker_get_FileTypeFilter)
        try {
            for (extension in extensions) {
                Hstring.use(extension) { h -> filter.call(FoundationInterop.IVector_Append, h) }
            }
        } finally {
            filter.release()
        }
    }

    /** Empties the list of file types (FileTypeFilter.Clear). */
    fun clearFileTypeFilter() {
        val filter = picker.getPtr(StoragePickersInterop.IFileOpenPicker_get_FileTypeFilter)
        try {
            filter.call(FoundationInterop.IVector_Clear)
        } finally {
            filter.release()
        }
    }

    /**
     * Opens the single-selection dialog (FileOpenPicker.PickSingleFileAsync).
     * Passes the path of the file the user picked (null on cancel) to [onPicked] on the UI thread.
     */
    fun pickSingleFile(onPicked: Consumer<String?>) {
        Async.onPtrResult(
            picker.getPtr(StoragePickersInterop.IFileOpenPicker_PickSingleFileAsync),
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFileResult,
            "FileOpenPicker.PickSingleFileAsync",
        ) { result ->
            val path = StoragePickers.pathOf(result)
            WinUiUtilities.invokeLater { onPicked.accept(path) }
        }
    }

    /**
     * Opens the multiple-selection dialog (FileOpenPicker.PickMultipleFilesAsync).
     * Passes the list of paths the user picked (empty on cancel) to [onPicked] on the UI thread.
     */
    fun pickMultipleFiles(onPicked: Consumer<List<String>>) {
        Async.onPtrResult(
            picker.getPtr(StoragePickersInterop.IFileOpenPicker_PickMultipleFilesAsync),
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFileResultList,
            "FileOpenPicker.PickMultipleFilesAsync",
        ) { results ->
            val paths = StoragePickers.pathsOf(results)
            WinUiUtilities.invokeLater { onPicked.accept(paths) }
        }
    }
}

/** How a picker's file list is displayed (PickerViewMode). Values extracted from the winmd. */
enum class PickerViewMode(internal val native: Int) {
    /** A list view. */
    LIST(0),

    /** A thumbnail view. */
    THUMBNAIL(1),

    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int) = entries.first { it.native == native }
    }
}

/** The folder a picker shows first (PickerLocationId). Values extracted from the winmd. */
enum class PickerLocationId(internal val native: Int) {
    /** Documents. */
    DOCUMENTS_LIBRARY(0),

    /** This PC (the computer). */
    COMPUTER_FOLDER(1),

    /** Desktop. */
    DESKTOP(2),

    /** Downloads. */
    DOWNLOADS(3),

    /** Music. */
    MUSIC_LIBRARY(5),

    /** Pictures. */
    PICTURES_LIBRARY(6),

    /** Videos. */
    VIDEOS_LIBRARY(7),

    /** 3D objects. */
    OBJECTS_3D(8),

    /** Unspecified (left to the OS). */
    UNSPECIFIED(9),

    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int) = entries.first { it.native == native }
    }
}

/** Creation and result reading shared by the three file/folder picker classes. */
internal object StoragePickers {
    /** Creates a picker via the activatable factory's CreateInstance(WindowId). */
    fun create(runtimeClass: String, factoryIid: String, owner: WFrame): ComPtr {
        val factory = Activation.factory(runtimeClass, factoryIid)
        return try {
            Ffi.backend.withScope { scope ->
                val windowId = XamlStructs.windowIdValue(scope, owner.appWindow.id)
                factory.getPtr(StoragePickersInterop.IPickerFactory_CreateInstance, windowId)
            }
        } finally {
            factory.release()
        }
    }

    /** Reads the Path of a PickFileResult / PickFolderResult (null = canceled) and releases the reference. */
    fun pathOf(result: ComPtr?): String? = result?.let {
        try {
            it.getString(StoragePickersInterop.IPickResult_get_Path)
        } finally {
            it.release()
        }
    }

    /** Reads the list of Paths from an IVectorView<PickFileResult> (null = canceled) and releases the reference. */
    fun pathsOf(results: ComPtr?): List<String> {
        if (results == null) return emptyList()
        try {
            return Ffi.backend.withScope { scope ->
                val memory = Ffi.backend.memory
                val size = scope.allocate(4)
                results.call(FoundationInterop.IVectorView_get_Size, size)
                (0 until memory.getInt(size, 0)).map { i ->
                    val out = scope.allocate(8)
                    results.call(FoundationInterop.IVectorView_GetAt, i, out)
                    checkNotNull(pathOf(ComPtr(memory.getPtr(out, 0))))
                }
            }
        } finally {
            results.release()
        }
    }
}
