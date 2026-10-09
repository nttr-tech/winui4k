package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winui.InkInterop

/**
 * Bridges Java byte arrays and WinRT streams (IOutputStream / IInputStream in Windows.Storage.Streams).
 * Provides the streams passed to InkStrokeContainer.SaveAsync / LoadAsync as in-memory streams
 * (InMemoryRandomAccessStream, which is agile and can therefore also be used from the ink thread).
 */
internal object InkStreams {
    /** Lets [write] write to an IOutputStream and returns the bytes written. */
    fun captureOutput(write: (output: ComPtr) -> Unit): ByteArray = withMemoryStream { stream ->
        val output = stream.queryInterface(InkInterop.IID_IOutputStream)
        try {
            write(output)
        } finally {
            output.release()
        }
        val size = Ffi.backend.withScope { scope ->
            val out = scope.allocate(8)
            stream.call(InkInterop.IRandomAccessStream_get_Size, out)
            Ffi.backend.memory.getLong(out, 0)
        }
        require(size <= Int.MAX_VALUE) { "Stream is too large: $size bytes" }
        val input = stream.getPtr(InkInterop.IRandomAccessStream_GetInputStreamAt, 0L)
        try {
            readAll(input, size.toInt())
        } finally {
            input.release()
        }
    }

    /** Passes an IInputStream whose content is [bytes] to [read]. */
    fun withInput(bytes: ByteArray, read: (input: ComPtr) -> Unit) {
        withMemoryStream { stream ->
            writeAll(stream, bytes)
            val input = stream.getPtr(InkInterop.IRandomAccessStream_GetInputStreamAt, 0L)
            try {
                read(input)
            } finally {
                input.release()
            }
        }
    }

    private fun <T> withMemoryStream(block: (stream: ComPtr) -> T): T {
        val stream = Activation.activate(InkInterop.CLS_InMemoryRandomAccessStream, InkInterop.IID_IRandomAccessStream)
        try {
            return block(stream)
        } finally {
            stream.release()
        }
    }

    /** Reads [size] bytes from [input] with DataReader. */
    private fun readAll(input: ComPtr, size: Int): ByteArray {
        if (size == 0) return ByteArray(0)
        val factory = Activation.factory(InkInterop.CLS_DataReader, InkInterop.IID_IDataReaderFactory)
        val reader = try {
            factory.getPtr(InkInterop.IDataReaderFactory_CreateDataReader, input)
        } finally {
            factory.release()
        }
        try {
            val load = reader.getPtr(InkInterop.IDataReader_LoadAsync, size)
            try {
                Async.awaitIntResult(load, InkInterop.IID_AsyncOperationCompletedHandler_UInt32, "DataReader.LoadAsync")
            } finally {
                load.release()
            }
            return Ffi.backend.withScope { scope ->
                val buffer = scope.allocate(size.toLong(), 1)
                // ReadBytes(u1[]) fills an array allocated by the caller: (UINT32 length, BYTE* array)
                reader.call(InkInterop.IDataReader_ReadBytes, size, buffer)
                ByteArray(size) { Ffi.backend.memory.getByte(buffer, it.toLong()) }
            }
        } finally {
            reader.release()
        }
    }

    /** Writes [bytes] to [stream] from the beginning with DataWriter. */
    private fun writeAll(stream: ComPtr, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val output = stream.getPtr(InkInterop.IRandomAccessStream_GetOutputStreamAt, 0L)
        val factory = Activation.factory(InkInterop.CLS_DataWriter, InkInterop.IID_IDataWriterFactory)
        val writer = try {
            factory.getPtr(InkInterop.IDataWriterFactory_CreateDataWriter, output)
        } finally {
            factory.release()
            output.release()
        }
        try {
            Ffi.backend.withScope { scope ->
                val buffer = scope.allocate(bytes.size.toLong(), 1)
                bytes.forEachIndexed { i, b -> Ffi.backend.memory.putByte(buffer, i.toLong(), b) }
                // WriteBytes(u1[]) takes (UINT32 length, BYTE* array)
                writer.call(InkInterop.IDataWriter_WriteBytes, bytes.size, buffer)
            }
            val store = writer.getPtr(InkInterop.IDataWriter_StoreAsync)
            try {
                Async.awaitIntResult(store, InkInterop.IID_AsyncOperationCompletedHandler_UInt32, "DataWriter.StoreAsync")
            } finally {
                store.release()
            }
            // Detach the stream so that disposing the DataWriter does not close it
            writer.getPtr(InkInterop.IDataWriter_DetachStream).release()
        } finally {
            writer.release()
        }
    }
}
