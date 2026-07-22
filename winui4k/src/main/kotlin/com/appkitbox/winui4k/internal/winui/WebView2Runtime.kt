package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.win32.Win32
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

/**
 * Preparation of the WebView2 WinRT implementation DLL (Microsoft.Web.WebView2.Core.dll).
 *
 * Since WinAppSDK 1.2, the runtime package only carries the activation registration for
 * Microsoft.Web.WebView2.Core; the DLL itself is expected to be placed by the app (a packaged
 * app has it resolved from its own package). In an unpackaged setup like winui4k's,
 * RoGetActivationFactory can't locate the DLL and fails with 0x8007007E (ERROR_MOD_NOT_FOUND),
 * so the DLL bundled in the JAR is extracted to a temp directory and pre-loaded by its
 * absolute path (once loaded, a module is subsequently resolved by LoadLibrary on its base name).
 */
internal object WebView2Runtime {
    private const val DLL_NAME = "Microsoft.Web.WebView2.Core.dll"

    @Volatile
    private var loaded = false

    private var extractedDirectory: File? = null

    /** Loads Microsoft.Web.WebView2.Core.dll into the process (a no-op on subsequent calls). */
    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            // Do nothing if it's already loaded (e.g. the app placed it itself)
            if (Win32.moduleFilePath(DLL_NAME) == null) {
                val path = System.getProperty("winui4k.webview2.dll")
                    ?: extractFromClasspath()
                    ?: DLL_NAME // last resort: leave it to the normal DLL search path
                Win32.loadLibrary(path)
            }
            loaded = true
        }
    }

    private fun extractFromClasspath(): String? {
        val arch = System.getProperty("os.arch").lowercase()
        val rid = when {
            arch == "aarch64" || arch == "arm64" -> "win-arm64"
            arch.contains("64") -> "win-x64"
            else -> "win-x86"
        }
        val resourcePath = "/native/$rid/$DLL_NAME"
        val stream = WebView2Runtime::class.java.getResourceAsStream(resourcePath) ?: return null
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val uniqueId = UUID.randomUUID().toString().substring(0, 8)
        val tempDirectory = File(System.getProperty("java.io.tmpdir"), "winui4k/${timestamp}_$uniqueId")
        tempDirectory.mkdirs()
        extractedDirectory = tempDirectory
        Runtime.getRuntime().addShutdownHook(Thread(::cleanupExtractedFiles, "winui4k-webview2-cleanup"))
        val dll = File(tempDirectory, DLL_NAME)
        stream.use { input -> FileOutputStream(dll).use { input.copyTo(it) } }
        return dll.absolutePath
    }

    private fun cleanupExtractedFiles() {
        extractedDirectory?.let { directory ->
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
            extractedDirectory = null
        }
    }
}
