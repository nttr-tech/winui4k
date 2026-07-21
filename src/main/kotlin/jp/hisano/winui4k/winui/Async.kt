package jp.hisano.winui4k.winui

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.ffi.Native
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Awaits completion of a Windows.Foundation async object (IAsyncAction / IAsyncOperation<T>).
 *
 * The Completed handler is called from an OS thread pool, so a CountDownLatch is used to
 * wait for it. Since this blocks the calling thread (the UI thread), only use it for
 * operations that complete quickly (such as reading/writing a JumpList).
 */
internal object Async {
    /** The cap on how long to wait for completion. Beyond this, the async operation is assumed to be stuck. */
    private const val TIMEOUT_SECONDS = 10L

    /** The delegate's Invoke(this, asyncInfo, AsyncStatus) — vtbl[3] */
    private val DESC_COMPLETED_HANDLER =
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)

    /** Awaits an IAsyncAction's completion. Throws an HRESULT exception if it failed. */
    fun await(action: ComPtr, what: String) {
        val latch = CountDownLatch(1)
        action.call(Abi.IAsyncAction_put_Completed, completedHandler(Abi.IID_AsyncActionCompletedHandler, latch))
        awaitLatch(latch, what)
        checkStatus(action, what)
    }

    /**
     * Awaits an IAsyncOperation<T>'s completion and returns GetResults' result.
     * [handlerIid] is the actual IID (a pinterface-computed value) of AsyncOperationCompletedHandler<T>.
     */
    fun awaitResult(operation: ComPtr, handlerIid: String, what: String): ComPtr {
        val latch = CountDownLatch(1)
        operation.call(Abi.IAsyncOperation_put_Completed, completedHandler(handlerIid, latch))
        awaitLatch(latch, what)
        checkStatus(operation, what)
        return operation.getPtr(Abi.IAsyncOperation_GetResults)
    }

    /** A delegate that releases [latch] on completion (Action and Operation<T> share the same Invoke shape). */
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
        val info = async.queryInterface(Abi.IID_IAsyncInfo)
        try {
            if (info.getInt(Abi.IAsyncInfo_get_Status) != Abi.AsyncStatus_Completed) {
                Native.checkHr(info.getInt(Abi.IAsyncInfo_get_ErrorCode), what)
            }
        } finally {
            info.release()
        }
    }
}
