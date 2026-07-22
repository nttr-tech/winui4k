package jp.hisano.winui4k.internal.winrt

import jp.hisano.winui4k.internal.com.checkHr
import jp.hisano.winui4k.internal.ffi.api.ArgKind
import jp.hisano.winui4k.internal.ffi.api.CallDescriptor
import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.ValueKind
import jp.hisano.winui4k.internal.ffi.api.function

/** Initialization and teardown of the WinRT runtime (the Ro* flat API in combase.dll). */
internal object WinRtRuntime {
    private val roInitialize by lazy {
        Ffi.backend.function(
            "combase.dll", "RoInitialize",
            CallDescriptor(ValueKind.I32, ArgKind.I32),
        )
    }
    private val roUninitialize by lazy {
        Ffi.backend.function("combase.dll", "RoUninitialize", CallDescriptor(ValueKind.VOID))
    }

    /** Initializes the UI thread as STA (equivalent to C#'s [STAThread]). */
    fun initializeSta() {
        val hr = roInitialize(0) as Int // RO_INIT_SINGLETHREADED
        checkHr(hr, "RoInitialize")
    }

    /**
     * Closes the COM apartment. Deferred COM releases (which involve upcalls) must be
     * finished while the JVM is still alive; otherwise an upcall from a native thread
     * during JVM shutdown fails to attach and aborts the whole JVM.
     */
    fun uninitialize() {
        runCatching { roUninitialize() }
    }
}
