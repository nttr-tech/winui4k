package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests the handling of the unhandled errors that XAML reports (Application.UnhandledException).
 * XAML forcibly terminates the process (0xC000027B) unless an unhandled error is marked Handled,
 * so winui4k marks it Handled, passes it to the UI thread's uncaught exception handler and keeps the app running (doc/troubleshooting.md).
 * The unhandled error is raised by calling combase's RoReportUnhandledError from the UI thread,
 * the same function that XAML's own ErrorHelper::ReportUnhandledError uses.
 */
class ApplicationUnhandledExceptionTest : FunSpec() {
    init {
        test("an unhandled XAML error reaches the UI thread's uncaught exception handler, and the process keeps running so the UI thread stays usable") {
            val received = mutableListOf<Throwable>()
            lateinit var previous: Thread.UncaughtExceptionHandler
            onUiThread {
                val thread = Thread.currentThread()
                previous = thread.uncaughtExceptionHandler
                thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, t -> received += t }
            }
            try {
                onUiThread { reportUnhandledError(E_ILLEGAL_METHOD_CALL, "winui4k unhandled error test") }
            } finally {
                onUiThread { Thread.currentThread().uncaughtExceptionHandler = previous }
            }
            received.size shouldBe 1
            val error = received.single().shouldBeInstanceOf<WindowsRuntimeException>()
            error.hresult shouldBe E_ILLEGAL_METHOD_CALL
            error.message shouldContain "winui4k unhandled error test"
            onUiThreadGet { WButton("Continue").text } shouldBe "Continue"
        }
    }

    /**
     * Originates an error with [hresult] and [message] on the current thread (RoOriginateErrorW) and reports its
     * error information to CoreApplication as unhandled (RoReportUnhandledError). The same steps as XAML's ErrorHelper::ReportUnhandledError.
     */
    private fun reportUnhandledError(hresult: Int, message: String) {
        val backend = Ffi.backend
        // BOOL RoOriginateErrorW(HRESULT error, UINT cchMax, PCWSTR message)
        val originate = backend.function(
            "combase.dll",
            "RoOriginateErrorW",
            CallDescriptor(ValueKind.I32, ArgKind.I32, ArgKind.I32, ArgKind.PTR),
        )
        // HRESULT GetRestrictedErrorInfo(IRestrictedErrorInfo** info) / HRESULT RoReportUnhandledError(IRestrictedErrorInfo* info)
        val getRestrictedErrorInfo = backend.function("combase.dll", "GetRestrictedErrorInfo", CallDescriptor(ValueKind.I32, ArgKind.PTR))
        val report = backend.function("combase.dll", "RoReportUnhandledError", CallDescriptor(ValueKind.I32, ArgKind.PTR))
        backend.withScope { scope ->
            val buffer = scope.allocate((message.length + 1).toLong() * 2, 2)
            backend.memory.putUtf16z(buffer, 0, message)
            check(originate(hresult, message.length, buffer) as Int != 0) { "RoOriginateErrorW did not create the error information" }
            val out = scope.allocate(8)
            checkHr(getRestrictedErrorInfo(out) as Int, "GetRestrictedErrorInfo")
            val info = ComPtr(backend.memory.getPtr(out, 0))
            try {
                checkHr(report(info.ptr) as Int, "RoReportUnhandledError")
            } finally {
                info.release()
            }
        }
    }

    private companion object {
        const val E_ILLEGAL_METHOD_CALL = 0x8000000E.toInt()
    }
}
