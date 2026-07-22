package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

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

    private const val DLL_NAME = "Microsoft.WindowsAppRuntime.Bootstrap.dll"

    @Volatile
    private var resolvedLibrary: String? = null

    private val library: String
        get() = resolvedLibrary ?: synchronized(this) {
            resolvedLibrary ?: resolveLibrary().also { resolvedLibrary = it }
        }

    private fun resolveLibrary(): String {
        System.getProperty("winui4k.bootstrap.dll")?.let { return it }
        extractFromClasspath()?.let { return it }
        return DLL_NAME
    }

    private var extractedDirectory: File? = null

    private fun extractFromClasspath(): String? {
        val arch = System.getProperty("os.arch").lowercase()
        val rid = when {
            arch == "aarch64" || arch == "arm64" -> "win-arm64"
            arch.contains("64") -> "win-x64"
            else -> "win-x86"
        }
        val resourcePath = "/native/$rid/$DLL_NAME"
        val stream = WinAppSdkBootstrap::class.java.getResourceAsStream(resourcePath) ?: return null
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val uniqueId = UUID.randomUUID().toString().substring(0, 8)
        val tempDirectory = File(System.getProperty("java.io.tmpdir"), "winui4k/${timestamp}_$uniqueId")
        tempDirectory.mkdirs()
        extractedDirectory = tempDirectory
        Runtime.getRuntime().addShutdownHook(Thread(::cleanupExtractedFiles, "winui4k-bootstrap-cleanup"))
        val dll = File(tempDirectory, DLL_NAME)
        stream.use { input -> FileOutputStream(dll).use { input.copyTo(it) } }
        return dll.absolutePath
    }

    fun cleanupExtractedFiles() {
        extractedDirectory?.let { directory ->
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
            extractedDirectory = null
        }
    }

    fun initialize() {
        try {
            callBootstrapInitialize()
        } catch (exception: WindowsRuntimeException) {
            if (installRuntimeIfAvailable()) {
                callBootstrapInitialize()
            } else {
                throw exception
            }
        }
    }

    private fun callBootstrapInitialize() {
        // HRESULT MddBootstrapInitialize2(UINT32 majorMinor, PCWSTR versionTag,
        //                                 PACKAGE_VERSION minVersion, MddBootstrapInitializeOptions options)
        val bootstrapInitialize = Ffi.backend.function(
            library,
            "MddBootstrapInitialize2",
            CallDescriptor(ValueKind.I32, ArgKind.I32, ArgKind.PTR, ArgKind.I64, ArgKind.I32),
        )
        Ffi.backend.withScope { scope ->
            val emptyTag = scope.allocate(2, 2) // L"" (the stable channel)
            Ffi.backend.memory.putUtf16z(emptyTag, 0, "")
            val hr = bootstrapInitialize(
                WINAPPSDK_MAJOR_MINOR,
                emptyTag,
                WINAPPSDK_MIN_VERSION,
                BOOTSTRAP_ON_NO_MATCH_SHOW_UI,
            ) as Int
            checkHr(hr, "MddBootstrapInitialize2 (is the Windows App SDK 2.2 runtime installed?)")
        }
    }

    private fun installRuntimeIfAvailable(): Boolean {
        val installer = findRuntimeInstaller() ?: return false
        val process = ProcessBuilder(installer.absolutePath, "--quiet")
            .inheritIO()
            .start()
        val exitCode = process.waitFor()
        return exitCode == 0
    }

    private fun findRuntimeInstaller(): File? {
        val arch = System.getProperty("os.arch").lowercase()
        val archSuffix = when {
            arch == "aarch64" || arch == "arm64" -> "arm64"
            arch.contains("64") -> "x64"
            else -> "x86"
        }
        val fileName = "WindowsAppRuntimeInstall-$archSuffix.exe"
        val installerDirectory = System.getProperty("winui4k.installer.dir")
        val baseDirectory = if (installerDirectory != null) File(installerDirectory) else File(System.getProperty("user.dir"))
        val candidate = baseDirectory.resolve(fileName)
        return if (candidate.isFile) candidate else null
    }

    fun shutdown() {
        runCatching {
            Ffi.backend.function(library, "MddBootstrapShutdown", CallDescriptor(ValueKind.VOID))()
        }
    }
}
