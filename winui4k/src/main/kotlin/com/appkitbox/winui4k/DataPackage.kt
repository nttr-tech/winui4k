package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winui.DataTransferInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop

/**
 * The container for data handed to the clipboard: Windows.ApplicationModel.DataTransfer.DataPackage.
 * Fill it via [setText] / [setHtmlFormat] / [setRtf] / [setUri] / [setStorageItems] and pass
 * it to [Clipboard.setContent]. A single package can hold multiple formats at once.
 */
class DataPackage {
    internal val dataPackage: ComPtr =
        Activation.activate(DataTransferInterop.CLS_DataPackage, DataTransferInterop.IID_IDataPackage)

    init {
        // Release on the UI thread once it becomes unreachable to GC (same mechanism as WComponent)
        ComLifetime.adopt(this, dataPackage)
    }

    /** The operation requested of the paste target (DataPackage.RequestedOperation). Defaults to [DataPackageOperation.NONE]. */
    var requestedOperation: DataPackageOperation
        get() = DataPackageOperation.of(dataPackage.getInt(DataTransferInterop.IDataPackage_get_RequestedOperation))
        set(value) = dataPackage.call(DataTransferInterop.IDataPackage_put_RequestedOperation, value.native)

    /** Sets plain text (DataPackage.SetText). */
    fun setText(text: String) = Hstring.use(text) { h ->
        dataPackage.call(DataTransferInterop.IDataPackage_SetText, h)
    }

    /** Sets an HTML clipboard format string (DataPackage.SetHtmlFormat). */
    fun setHtmlFormat(html: String) = Hstring.use(html) { h ->
        dataPackage.call(DataTransferInterop.IDataPackage_SetHtmlFormat, h)
    }

    /** Sets rich text (RTF) (DataPackage.SetRtf). */
    fun setRtf(rtf: String) = Hstring.use(rtf) { h ->
        dataPackage.call(DataTransferInterop.IDataPackage_SetRtf, h)
    }

    /** Sets a URI (the WebLink format) (DataPackage.SetUri). */
    fun setUri(uri: String) {
        val factory = Activation.factory(FoundationInterop.CLS_Uri, FoundationInterop.IID_IUriRuntimeClassFactory)
        val uriObject = try {
            Hstring.use(uri) { h ->
                factory.getPtr(FoundationInterop.IUriRuntimeClassFactory_CreateUri, h)
            }
        } finally {
            factory.release()
        }
        try {
            dataPackage.call(DataTransferInterop.IDataPackage_SetUri, uriObject)
        } finally {
            uriObject.release()
        }
    }

    /**
     * Sets files/folders (DataPackage.SetStorageItems).
     * Opens each path in [paths] as a StorageFile (a nonexistent path raises an HRESULT exception).
     * When pasting into Explorer is expected, set [requestedOperation] as well.
     */
    fun setStorageItems(vararg paths: String) {
        val items = paths.map { path -> storageItemOf(path) }
        try {
            val iterable = StorageItemIterable(items)
            dataPackage.call(DataTransferInterop.IDataPackage_SetStorageItems, iterable.comObject.primary)
            iterable.comObject.release()
        } finally {
            // SetStorageItems copies the items into an internal collection, so our references can be let go
            items.forEach { it.release() }
        }
    }

    /** Opens [path]'s StorageFile and returns it as an IStorageItem (StorageFile.GetFileFromPathAsync). */
    private fun storageItemOf(path: String): ComPtr {
        val statics = Activation.factory(DataTransferInterop.CLS_StorageFile, DataTransferInterop.IID_IStorageFileStatics)
        val operation = try {
            Hstring.use(path) { h ->
                statics.getPtr(DataTransferInterop.IStorageFileStatics_GetFileFromPathAsync, h)
            }
        } finally {
            statics.release()
        }
        val file = Async.awaitResult(
            operation,
            DataTransferInterop.IID_AsyncOperationCompletedHandler_StorageFile,
            "StorageFile.GetFileFromPathAsync",
        )
        operation.release()
        return try {
            file.queryInterface(DataTransferInterop.IID_IStorageItem)
        } finally {
            file.release()
        }
    }
}

/** The operation requested of the paste target (DataPackageOperation). Values extracted from the winmd. */
enum class DataPackageOperation(internal val native: Int) {
    /** Unspecified. */
    NONE(0),

    /** Copy. */
    COPY(1),

    /** Move. */
    MOVE(2),

    /** Link. */
    LINK(4),

    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int) = entries.first { it.native == native }
    }
}

/**
 * A Kotlin implementation that exposes a List<ComPtr (IStorageItem)> as an IIterable<IStorageItem>.
 * Used to pass items to DataPackage.SetStorageItems (same structure as StringIterable).
 */
private class StorageItemIterable(private val items: List<ComPtr>) {
    /** The COM object handed over as an IIterable<IStorageItem>. */
    val comObject: KComObject = KComObject("WinUI4K.StorageItemIterable")
        .addInterface(
            DataTransferInterop.IID_IIterable_StorageItem,
            listOf(
                // vtbl[6] First(this, out IIterator<IStorageItem>)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )

    /** Creates the IIterator<IStorageItem> implementation. Each First call returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject("WinUI4K.StorageItemIterator").addInterface(
            DataTransferInterop.IID_IIterator_StorageItem,
            listOf(
                // vtbl[6] get_Current(this, out IStorageItem) — hands out a reference the caller releases
                KComObject.Method(DESC_THIS_PTR) { args ->
                    if (index >= items.size) return@Method E_BOUNDS
                    val item = items[index]
                    item.addRef()
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, item.ptr)
                    KComObject.S_OK
                },
                // vtbl[7] get_HasCurrent(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < items.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[8] MoveNext(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    index++
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < items.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetMany(this, UINT32 capacity, IStorageItem* items, out UINT32 actual)
                KComObject.Method(DESC_GET_MANY) { args ->
                    val capacity = args[1] as Int
                    val out = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < items.size) {
                        val item = items[index]
                        item.addRef()
                        Ffi.backend.memory.putPtr(out, written.toLong() * 8, item.ptr)
                        written++
                        index++
                    }
                    Ffi.backend.memory.putInt(args[3] as Ptr, 0, written)
                    KComObject.S_OK
                },
            ),
        )
    }

    private companion object {
        val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
        val DESC_GET_MANY = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
        val E_BOUNDS = 0x8000000B.toInt()
    }
}
