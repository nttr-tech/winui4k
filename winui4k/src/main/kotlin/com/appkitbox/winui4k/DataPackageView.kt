package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.DataTransferInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import java.util.function.Consumer

/**
 * The read-only view of the clipboard's content: Windows.ApplicationModel.DataTransfer.DataPackageView.
 * Received from [Clipboard.getContent]. Check the format via [contains] / [availableFormats]
 * first, then read via [getText] and friends.
 *
 * Reading involves requesting the data from the copying app (delayed rendering), so it's
 * asynchronous and the result arrives via a callback on the UI thread.
 */
class DataPackageView internal constructor(private val view: ComPtr) {
    init {
        // Release on the UI thread once it becomes unreachable to GC (same mechanism as WComponent)
        ComLifetime.adopt(this, view)
    }

    /** The operation the copy source requests (DataPackageView.RequestedOperation). */
    val requestedOperation: DataPackageOperation
        get() = DataPackageOperation.of(view.getInt(DataTransferInterop.IDataPackageView_get_RequestedOperation))

    /** The formats currently on the clipboard (a snapshot of DataPackageView.AvailableFormats). */
    val availableFormats: List<String>
        get() {
            val formats = view.getPtr(DataTransferInterop.IDataPackageView_get_AvailableFormats)
            try {
                return Ffi.backend.withScope { scope ->
                    val memory = Ffi.backend.memory
                    val size = scope.allocate(4)
                    formats.call(FoundationInterop.IVectorView_get_Size, size)
                    (0 until memory.getInt(size, 0)).map { i ->
                        val out = scope.allocate(8)
                        formats.call(FoundationInterop.IVectorView_GetAt, i, out)
                        val hstring = memory.getPtr(out, 0)
                        try {
                            Hstring.read(hstring)
                        } finally {
                            Hstring.free(hstring)
                        }
                    }
                }
            } finally {
                formats.release()
            }
        }

    /** Whether content in [format] (a [StandardDataFormats] value, etc.) exists (DataPackageView.Contains). */
    fun contains(format: String): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        Hstring.use(format) { h -> view.call(DataTransferInterop.IDataPackageView_Contains, h, out) }
        Ffi.backend.memory.getByte(out, 0).toInt() != 0
    }

    /**
     * Reads plain text (DataPackageView.GetTextAsync).
     * The result is passed to [onResult] on the UI thread. When there is no Text format
     * this raises an HRESULT exception, so check with [contains] first.
     */
    fun getText(onResult: Consumer<String>) =
        onStringResult(DataTransferInterop.IDataPackageView_GetTextAsync, "DataPackageView.GetTextAsync", onResult)

    /** Reads an HTML clipboard format string (DataPackageView.GetHtmlFormatAsync). */
    fun getHtmlFormat(onResult: Consumer<String>) = onStringResult(
        DataTransferInterop.IDataPackageView_GetHtmlFormatAsync,
        "DataPackageView.GetHtmlFormatAsync",
        onResult,
    )

    /** Reads rich text (RTF) (DataPackageView.GetRtfAsync). */
    fun getRtf(onResult: Consumer<String>) =
        onStringResult(DataTransferInterop.IDataPackageView_GetRtfAsync, "DataPackageView.GetRtfAsync", onResult)

    /** Reads a URI (the WebLink format) (DataPackageView.GetUriAsync). */
    fun getUri(onResult: Consumer<String>) {
        Async.onPtrResult(
            view.getPtr(DataTransferInterop.IDataPackageView_GetUriAsync),
            DataTransferInterop.IID_AsyncOperationCompletedHandler_Uri,
            "DataPackageView.GetUriAsync",
        ) { uri ->
            val value = uri?.let {
                try {
                    it.getString(FoundationInterop.IUriRuntimeClass_get_AbsoluteUri)
                } finally {
                    it.release()
                }
            }.orEmpty()
            WinUiUtilities.invokeLater { onResult.accept(value) }
        }
    }

    /**
     * Reads the list of file/folder paths (DataPackageView.GetStorageItemsAsync).
     * The result is passed to [onResult] on the UI thread.
     */
    fun getStorageItems(onResult: Consumer<List<String>>) {
        Async.onPtrResult(
            view.getPtr(DataTransferInterop.IDataPackageView_GetStorageItemsAsync),
            DataTransferInterop.IID_AsyncOperationCompletedHandler_StorageItemList,
            "DataPackageView.GetStorageItemsAsync",
        ) { items ->
            val paths = pathsOf(items)
            WinUiUtilities.invokeLater { onResult.accept(paths) }
        }
    }

    /** Calls a slot returning IAsyncOperation<String> and passes the result to [onResult] on the UI thread. */
    private fun onStringResult(slot: Int, what: String, onResult: Consumer<String>) {
        Async.onStringResult(
            view.getPtr(slot),
            DataTransferInterop.IID_AsyncOperationCompletedHandler_String,
            what,
        ) { text ->
            WinUiUtilities.invokeLater { onResult.accept(text) }
        }
    }

    /** Reads the list of Paths from an IVectorView<IStorageItem> (null = no content) and releases the reference. */
    private fun pathsOf(items: ComPtr?): List<String> {
        if (items == null) return emptyList()
        try {
            return Ffi.backend.withScope { scope ->
                val memory = Ffi.backend.memory
                val size = scope.allocate(4)
                items.call(FoundationInterop.IVectorView_get_Size, size)
                (0 until memory.getInt(size, 0)).map { i ->
                    val out = scope.allocate(8)
                    items.call(FoundationInterop.IVectorView_GetAt, i, out)
                    val item = ComPtr(memory.getPtr(out, 0))
                    try {
                        item.getString(DataTransferInterop.IStorageItem_get_Path)
                    } finally {
                        item.release()
                    }
                }
            }
        } finally {
            items.release()
        }
    }
}
