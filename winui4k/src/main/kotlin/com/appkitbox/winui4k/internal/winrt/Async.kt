package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Waits for a Windows.Foundation async object (IAsyncAction / IAsyncOperation<T>) to complete.
 *
 * The Completed handler is called from an OS thread pool thread, so we wait on a
 * CountDownLatch. Since this blocks the caller (the UI thread), only use it for
 * operations that complete quickly (e.g. reading/writing the JumpList).
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
    private const val TIMEOUT_SECONDS = 10L

    /** The delegate's Invoke(this, asyncInfo, AsyncStatus) — vtbl[3] */
    private val DESC_COMPLETED_HANDLER =
        CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.I32)

    /** Waits for an IAsyncAction to complete. Throws an HRESULT exception on failure. */
    fun await(action: ComPtr, what: String) {
        val latch = CountDownLatch(1)
        action.call(IAsyncAction_put_Completed, completedHandler(IID_AsyncActionCompletedHandler, latch))
        awaitLatch(latch, what)
        checkStatus(action, what)
    }

    /**
     * Waits for an IAsyncOperation<T> to complete and returns the result of GetResults.
     * [handlerIid] is the actual IID of AsyncOperationCompletedHandler<T> (a pinterface-computed value).
     */
    fun awaitResult(operation: ComPtr, handlerIid: String, what: String): ComPtr {
        val latch = CountDownLatch(1)
        operation.call(IAsyncOperation_put_Completed, completedHandler(handlerIid, latch))
        awaitLatch(latch, what)
        checkStatus(operation, what)
        return operation.getPtr(IAsyncOperation_GetResults)
    }

    /** The delegate that releases [latch] on completion (Action and Operation<T> both have the same Invoke shape). */
    private fun completedHandler(handlerIid: String, latch: CountDownLatch) =
        KComObject("WinUI4K.AsyncCompletedHandler", inspectable = false)
            .addInterface(
                handlerIid,
                listOf(
                    KComObject.Method(DESC_COMPLETED_HANDLER) {
                        latch.countDown()
                        KComObject.S_OK
                    },
                ),
            )
            .primary

    private fun awaitLatch(latch: CountDownLatch, what: String) {
        check(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "$what timed out" }
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
