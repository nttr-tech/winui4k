package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

/**
 * Windows App SDK bootstrap (Microsoft.WindowsAppRuntime.Bootstrap.dll).
 * Binds an installed WinAppSDK runtime package to this process as a dynamic reference.
 */
internal object WinAppSdkBootstrap {
    /**
     * Windows App SDK 2.x (majorMinorVersion = 0x00020000 for MddBootstrapInitialize2).
     * Since release 2.0, the minor part is ignored and resolution is based on major
     * only, so the desired version is specified via [WINAPPSDK_MIN_VERSION] (minVersion).
     */
    private const val WINAPPSDK_MAJOR_MINOR = 0x0002_0000

    /** Minimum runtime version 2.2.0.0 (PACKAGE_VERSION: Major<<48 | Minor<<32 | Build<<16 | Revision). */
    private const val WINAPPSDK_MIN_VERSION = 0x0002_0002_0000_0000L

    /** MddBootstrapInitializeOptions_OnNoMatch_ShowUI: prompts the user to install the runtime if it's missing. */
    private const val BOOTSTRAP_ON_NO_MATCH_SHOW_UI = 0x08

    private val library: String
        get() = System.getProperty("winui4k.bootstrap.dll")
            ?: "Microsoft.WindowsAppRuntime.Bootstrap.dll" // resolved from PATH / the current directory

    fun initialize() {
        // HRESULT MddBootstrapInitialize2(UINT32 majorMinor, PCWSTR versionTag,
        //                                 PACKAGE_VERSION minVersion, MddBootstrapInitializeOptions options)
        val bootstrapInitialize = Ffi.backend.function(
            library, "MddBootstrapInitialize2",
            CallDescriptor(ValueKind.I32, ArgKind.I32, ArgKind.PTR, ArgKind.I64, ArgKind.I32),
        )
        Ffi.backend.withScope { scope ->
            val emptyTag = scope.allocate(2, 2) // L"" (the stable channel)
            Ffi.backend.memory.putUtf16z(emptyTag, 0, "")
            val hr = bootstrapInitialize(
                WINAPPSDK_MAJOR_MINOR, emptyTag, WINAPPSDK_MIN_VERSION, BOOTSTRAP_ON_NO_MATCH_SHOW_UI,
            ) as Int
            checkHr(hr, "MddBootstrapInitialize2 (is the Windows App SDK 2.2 runtime installed?)")
        }
    }

    fun shutdown() {
        runCatching {
            Ffi.backend.function(library, "MddBootstrapShutdown", CallDescriptor(ValueKind.VOID))()
        }
    }
}
