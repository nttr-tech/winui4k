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
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
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
 * Note that the picker remembers and restores the last-selected location and view mode, so [suggestedStartLocation] and
 * [viewMode] only take effect on the first showing (before any memory exists) ([suggestedFolder] takes effect every time).
 * [title] / [suggestedFolder] / [addFileTypeChoice] / [initialFileTypeIndex] are features from Windows App SDK 2.3 (IFileOpenPicker2 etc.); if the runtime does not have them,
 * reading returns empty ("") and setting throws [UnsupportedOperationException].
 */
class WFileOpenPicker internal constructor(windowId: Long) {
    private val picker: ComPtr = StoragePickers.create(
        StoragePickersInterop.CLS_FileOpenPicker,
        StoragePickersInterop.IID_IFileOpenPickerFactory,
        windowId,
    )

    /** Creates a picker that opens on the window of [owner]. */
    constructor(owner: WFrame) : this(owner.appWindow.id)

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

    /** The dialog title (FileOpenPicker.Title). Empty means the OS default ("Open"). */
    var title: String
        get() = picker2String(StoragePickersInterop.IFileOpenPicker2_get_Title)
        set(value) = putPicker2String(StoragePickersInterop.IFileOpenPicker2_put_Title, value)

    /**
     * The absolute path of the folder shown when opened (FileOpenPicker.SuggestedFolder). Takes precedence over [suggestedStartLocation] and the last location.
     * Empty means not specified. A folder that does not exist is ignored.
     */
    var suggestedFolder: String
        get() = picker2String(StoragePickersInterop.IFileOpenPicker2_get_SuggestedFolder)
        set(value) = putPicker2String(StoragePickersInterop.IFileOpenPicker2_put_SuggestedFolder, value)

    /**
     * The position of the "file type" selected when opened (FileOpenPicker.InitialFileTypeIndex, 0-based). -1 means the OS default.
     * If it is larger than the number of [addFileTypeChoice] choices, the dialog does not open (HRESULT exception).
     */
    var initialFileTypeIndex: Int
        get() = StoragePickers.withV2(picker, StoragePickersInterop.IID_IFileOpenPicker2) {
            it.getInt(StoragePickersInterop.IFileOpenPicker2_get_InitialFileTypeIndex)
        } ?: -1
        set(value) {
            StoragePickers.requireV2(picker, StoragePickersInterop.IID_IFileOpenPicker2, "InitialFileTypeIndex") {
                it.call(StoragePickersInterop.IFileOpenPicker2_put_InitialFileTypeIndex, value)
            }
        }

    /**
     * Adds a "file type" choice with a display name (FileOpenPicker.FileTypeChoices). [extensions] are in the ".txt" form or "*".
     * If there is at least one choice, it takes precedence over [fileTypeFilter]. Adding the same [displayName] replaces its extensions.
     */
    fun addFileTypeChoice(displayName: String, vararg extensions: String) {
        StoragePickers.requireV2(picker, StoragePickersInterop.IID_IFileOpenPicker2, "FileTypeChoices") {
            val choices = it.getPtr(StoragePickersInterop.IFileOpenPicker2_get_FileTypeChoices)
            try {
                StoragePickers.insertChoice(choices, displayName, extensions.toList())
            } finally {
                choices.release()
            }
        }
    }

    /** The choices added with [addFileTypeChoice] (display names and extensions, in the order added). For checking in tests. */
    internal val fileTypeChoices: List<Pair<String, List<String>>>
        get() = StoragePickers.withV2(picker, StoragePickersInterop.IID_IFileOpenPicker2) {
            val choices = it.getPtr(StoragePickersInterop.IFileOpenPicker2_get_FileTypeChoices)
            try {
                StoragePickers.readChoices(choices)
            } finally {
                choices.release()
            }
        }.orEmpty()

    private fun picker2String(slot: Int): String =
        StoragePickers.withV2(picker, StoragePickersInterop.IID_IFileOpenPicker2) { it.getString(slot) }.orEmpty()

    private fun putPicker2String(slot: Int, value: String) {
        StoragePickers.requireV2(picker, StoragePickersInterop.IID_IFileOpenPicker2, "FileOpenPicker") {
            Hstring.use(value) { h -> it.call(slot, h) }
        }
    }

    /** A version of [pickSingleFile] that receives failures with [onError] (for WFileChooser). */
    internal fun pickSingleFile(onError: (RuntimeException) -> Unit, onPicked: (String?) -> Unit) {
        StoragePickers.pick(
            { picker.getPtr(StoragePickersInterop.IFileOpenPicker_PickSingleFileAsync) },
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFileResult,
            "FileOpenPicker.PickSingleFileAsync",
            StoragePickers::pathOf,
            StoragePickers.deliver(onError, onPicked),
        )
    }

    /** A version of [pickMultipleFiles] that receives failures with [onError] (for WFileChooser). */
    internal fun pickMultipleFiles(onError: (RuntimeException) -> Unit, onPicked: (List<String>) -> Unit) {
        StoragePickers.pick(
            { picker.getPtr(StoragePickersInterop.IFileOpenPicker_PickMultipleFilesAsync) },
            StoragePickersInterop.IID_AsyncOperationCompletedHandler_PickFileResultList,
            "FileOpenPicker.PickMultipleFilesAsync",
            StoragePickers::pathsOf,
            StoragePickers.deliver(onError, onPicked),
        )
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
    fun create(runtimeClass: String, factoryIid: String, windowId: Long): ComPtr {
        val factory = Activation.factory(runtimeClass, factoryIid)
        return try {
            Ffi.backend.withScope { scope ->
                factory.getPtr(StoragePickersInterop.IPickerFactory_CreateInstance, XamlStructs.windowIdValue(scope, windowId))
            }
        } finally {
            factory.release()
        }
    }

    /**
     * The WindowId of the window [component] is in (XamlRoot.ContentIslandEnvironment.AppWindowId).
     * [IllegalArgumentException] if it is not in a window.
     */
    fun windowIdOf(component: WComponent): Long {
        val root = requireNotNull(component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)) {
            "The parent is not in a window, so the window to open the dialog on is unknown"
        }
        try {
            val root2 = root.queryInterface(XamlInterop.IID_IXamlRoot2)
            try {
                val environment = root2.getPtr(XamlInterop.IXamlRoot2_get_ContentIslandEnvironment)
                try {
                    return XamlStructs.getWindowId(environment, WindowingInterop.IContentIslandEnvironment_get_AppWindowId)
                } finally {
                    environment.release()
                }
            } finally {
                root2.release()
            }
        } finally {
            root.release()
        }
    }

    /**
     * Calls [block] with the interface [iid] from Windows App SDK 2.3 (IFileOpenPicker2 etc.).
     * null if the runtime does not have it.
     */
    fun <T> withV2(picker: ComPtr, iid: String, block: (ComPtr) -> T): T? {
        val v2 = picker.queryInterfaceOrNull(iid) ?: return null
        try {
            return block(v2)
        } finally {
            v2.release()
        }
    }

    /** Same as [withV2], but throws [UnsupportedOperationException] if the runtime does not have it. */
    fun requireV2(picker: ComPtr, iid: String, what: String, block: (ComPtr) -> Unit) {
        withV2(picker, iid, block) ?: throw UnsupportedOperationException(
            "$what is not available in this Windows App SDK runtime (2.3 or later is required)",
        )
    }

    /** Puts choices of display names and extensions into the picker's FileTypeChoices (IMap<String, IVector<String>>). */
    fun insertChoice(choices: ComPtr, displayName: String, extensions: List<String>) {
        val vector = StringVector(extensions)
        try {
            Ffi.backend.withScope { scope ->
                val replaced = scope.allocate(1, 1) // Insert(key, value, out boolean replaced)
                Hstring.use(displayName) { key ->
                    choices.call(FoundationInterop.IMap_Insert, key, vector.comObject.primary, replaced)
                }
            }
        } finally {
            vector.comObject.release() // Insert keeps a reference (or copies the contents). It is reclaimed by the picker's Release
        }
    }

    /** Reads the contents of FileTypeChoices (display names and extensions, in their order). */
    fun readChoices(choices: ComPtr): List<Pair<String, List<String>>> {
        val iterable = choices.queryInterface(FoundationInterop.IID_IIterable_KeyValuePair_String_VectorString)
        val iterator = try {
            iterable.getPtr(FoundationInterop.IIterable_First)
        } finally {
            iterable.release()
        }
        try {
            val result = ArrayList<Pair<String, List<String>>>()
            while (iterator.getBool(FoundationInterop.IIterator_get_HasCurrent)) {
                val pair = iterator.getPtr(FoundationInterop.IIterator_get_Current)
                try {
                    result += readChoice(pair)
                } finally {
                    pair.release()
                }
                iterator.getBool(FoundationInterop.IIterator_MoveNext)
            }
            return result
        } finally {
            iterator.release()
        }
    }

    /** The display name and extensions of an IKeyValuePair<String, IVector<String>> (borrowed). */
    private fun readChoice(pair: ComPtr): Pair<String, List<String>> {
        val key = pair.getString(FoundationInterop.IKeyValuePair_get_Key)
        val value = pair.getPtr(FoundationInterop.IKeyValuePair_get_Value)
        try {
            return key to readStrings(value)
        } finally {
            value.release()
        }
    }

    /** Reads the contents of an IVector<String>. */
    fun readStrings(vector: ComPtr): List<String> = Ffi.backend.withScope { scope ->
        val memory = Ffi.backend.memory
        val size = scope.allocate(4)
        vector.call(FoundationInterop.IVector_get_Size, size)
        (0 until memory.getInt(size, 0)).map { i ->
            val out = scope.allocate(8)
            vector.call(FoundationInterop.IVector_GetAt, i, out)
            val hstring = memory.getPtr(out, 0)
            try {
                Hstring.read(hstring)
            } finally {
                Hstring.free(hstring)
            }
        }
    }

    /**
     * Starts Pick*Async with [start], and when it completes reads the result with [read] and passes it to [onDone] on the UI thread.
     * If it cannot be started or fails, passes a failure with that exception to [onDone] on the UI thread.
     */
    fun <T> pick(start: () -> ComPtr, handlerIid: String, what: String, read: (ComPtr?) -> T, onDone: (Result<T>) -> Unit) {
        val operation = try {
            start()
        } catch (e: RuntimeException) {
            WinUiUtilities.invokeLater { onDone(Result.failure(e)) }
            return
        }
        Async.onPtrResult(
            operation,
            handlerIid,
            what,
            onError = { e -> WinUiUtilities.invokeLater { onDone(Result.failure(e)) } },
        ) { result ->
            val value = read(result)
            WinUiUtilities.invokeLater { onDone(Result.success(value)) }
        }
    }

    /** Routes the result of [pick] to [onPicked] (success) or [onError] (failure). */
    fun <T> deliver(onError: (RuntimeException) -> Unit, onPicked: (T) -> Unit): (Result<T>) -> Unit =
        { result -> result.fold(onPicked) { onError(it as RuntimeException) } }

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
