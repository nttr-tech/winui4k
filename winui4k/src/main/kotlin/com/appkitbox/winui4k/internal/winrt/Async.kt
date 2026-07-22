package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.win32.Win32

/**
 * Waits for a Windows.Foundation async object (IAsyncAction / IAsyncOperation<T>) to complete.
 *
 * The Completed handler is called from an OS thread pool thread and signals a Win32 event.
 * The wait itself uses CoWaitForMultipleObjects, so on an STA (the UI thread) incoming COM
 * calls are still dispatched while we wait, avoiding a deadlock caused by marshalling back
 * onto the STA. That said, this still blocks the caller, so only use it for operations that
 * complete quickly (e.g. reading/writing the JumpList).
 */
internal object Async {
    // ---- Windows.Foundation async ABI constants (from FoundationContract.winmd, same policy as Abi.kt) ----
    private const val IID_IAsyncInfo = "00000036-0000-0000-c000-000000000046"
    private const val IAsyncInfo_get_Status = 7        // get_Status(out AsyncStatus)
    private const val IAsyncInfo_get_ErrorCode = 8     // get_ErrorCode(out HRESULT)

    /** AsyncStatus.Completed = 1 */
    private const val AsyncStatus_Completed = 1

    private const val IAsyncAction_put_Completed = 6   // put_Completed(AsyncActionCompletedHandler)

    /** delegate AsyncActionCompletedHandler — Invoke(IAsyncAction, AsyncStatus) is vtbl[3] */
    private const val IID_AsyncActionCompletedHandler = "a4ed5c81-76c9-40bd-8be6-b1d90fb20ae7"

    // IAsyncOperation`1: put_Completed=6 get_Completed=7 GetResults=8
    private const val IAsyncOperation_put_Completed = 6
    private const val IAsyncOperation_GetResults = 8

    /** Upper bound on the wait. Beyond this, the async operation is treated as stuck. */
    private const val TIMEOUT_MILLIS = 10_000

    /**
     * COWAIT_DEFAULT: on an STA, incoming COM calls are still dispatched while we wait
     * (COWAIT_DISPATCH_CALLS is ASTA-only and returns E_INVALIDARG on a classic STA).
     */
    private const val COWAIT_DEFAULT = 0

    /** CoWaitForMultipleObjects's return value on timeout (RPC_S_CALLPENDING). */
    private val RPC_S_CALLPENDING = 0x80010115.toInt()

    /** The delegate's Invoke(this, asyncInfo, AsyncStatus) — vtbl[3] */
    private val DESC_COMPLETED_HANDLER =
        CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.I32)

    private val coWaitForMultipleObjects by lazy {
        // HRESULT CoWaitForMultipleObjects(DWORD flags, DWORD timeout, ULONG count, HANDLE* handles, DWORD* index)
        Ffi.backend.function(
            "ole32.dll", "CoWaitForMultipleObjects",
            CallDescriptor(ValueKind.I32, ArgKind.I32, ArgKind.I32, ArgKind.I32, ArgKind.PTR, ArgKind.PTR),
        )
    }

    /** Waits for an IAsyncAction to complete. Throws an HRESULT exception on failure. */
    fun await(action: ComPtr, what: String) {
        val completedEvent = Win32.newAutoResetEvent()
        val handler = completedHandler(IID_AsyncActionCompletedHandler, completedEvent)
        try {
            action.call(IAsyncAction_put_Completed, handler.primary)
        } finally {
            handler.release() // put_Completed holds a reference to it; it's reclaimed by Release after completion fires
        }
        awaitEvent(completedEvent, what)
        checkStatus(action, what)
    }

    /**
     * Waits for an IAsyncOperation<T> to complete and returns the result of GetResults.
     * [handlerIid] is the actual IID of AsyncOperationCompletedHandler<T> (a pinterface-computed value).
     */
    fun awaitResult(operation: ComPtr, handlerIid: String, what: String): ComPtr {
        val completedEvent = Win32.newAutoResetEvent()
        val handler = completedHandler(handlerIid, completedEvent)
        try {
            operation.call(IAsyncOperation_put_Completed, handler.primary)
        } finally {
            handler.release() // put_Completed holds a reference to it; it's reclaimed by Release after completion fires
        }
        awaitEvent(completedEvent, what)
        checkStatus(operation, what)
        return operation.getPtr(IAsyncOperation_GetResults)
    }

    /** The delegate that signals [completedEvent] on completion (Action and Operation<T> both have the same Invoke shape). */
    private fun completedHandler(handlerIid: String, completedEvent: Ptr): KComObject =
        KComObject("WinUI4K.AsyncCompletedHandler", inspectable = false)
            .addInterface(
                handlerIid,
                listOf(
                    KComObject.Method(DESC_COMPLETED_HANDLER) {
                        Win32.signalEvent(completedEvent)
                        KComObject.S_OK
                    },
                ),
            )

    /**
     * Waits for [completedEvent] to be signaled, dispatching incoming COM calls while it waits.
     * On timeout the handler could still signal the event later, so the handle is deliberately
     * not closed here (closing it would risk the OS reusing the handle value and a late signal
     * hitting an unrelated object).
     */
    private fun awaitEvent(completedEvent: Ptr, what: String) {
        val waitResult = Ffi.backend.withScope { scope ->
            val handles = scope.allocate(8)
            Ffi.backend.memory.putPtr(handles, 0, completedEvent)
            val signaledIndex = scope.allocate(4)
            coWaitForMultipleObjects(
                COWAIT_DEFAULT, TIMEOUT_MILLIS, 1, handles, signaledIndex,
            ) as Int
        }
        check(waitResult != RPC_S_CALLPENDING) { "$what timed out" }
        checkHr(waitResult, "CoWaitForMultipleObjects($what)")
        Win32.closeEventHandle(completedEvent)
    }

    /** If IAsyncInfo.Status isn't Completed, throws using ErrorCode's HRESULT. */
    private fun checkStatus(async: ComPtr, what: String) {
        val info = async.queryInterface(IID_IAsyncInfo)
        try {
            if (info.getInt(IAsyncInfo_get_Status) != AsyncStatus_Completed) {
                checkHr(info.getInt(IAsyncInfo_get_ErrorCode), what)
            }
        } finally {
            info.release()
        }
    }
}
