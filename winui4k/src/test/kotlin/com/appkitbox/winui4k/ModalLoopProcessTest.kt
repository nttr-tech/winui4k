package com.appkitbox.winui4k

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.Enabled
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Tests the nested message loop ([com.appkitbox.winui4k.internal.winui.ModalLoop]) with [ModalLoopProbe] launched as a separate process
 * for each of the three FFI backends (Panama, JNA, JNR) ("Nested message loop" in doc/verification.md).
 * Exiting the app (Application.Exit, closing the last window, System.exit) from inside the loop ends the process,
 * so it is checked in child processes rather than in shared-window tests. Child processes are cut off after [TIMEOUT_SECONDS] (a hang is a failure).
 *
 * The JNA backend is Windows x64 only (it builds struct pass-by-value with the x64 ABI; JnaBackendProvider.isAvailable), so
 * the 5 JNA tests are skipped on Arm64 JDKs. Otherwise the child process exits with code 1 with "FFI backend 'jna' is not available"
 * (it failed every time on windows-11-arm of GitHub Actions; doc/troubleshooting.md).
 */
@Tags("Process")
class ModalLoopProcessTest : FunSpec() {
    /** The child process's exit code, the standard output lines starting with "RESULT " (without the leading "RESULT "), and whether the JVM crashed (left an hs_err). */
    private data class Outcome(val exitCode: Int, val results: List<String>, val crashed: Boolean = false)

    private fun runProbe(backend: String, scenario: String): Outcome {
        val java = File(System.getProperty("java.home"), "bin/java.exe").absolutePath
        // Have the JVM write its crash record (hs_err) to a file only for this run, and tell whether it crashed by whether the file exists
        val errorFile = File.createTempFile("winui4k-modal-loop-hs_err-", ".log").apply { delete() }
        val process = ProcessBuilder(
            java,
            "--enable-native-access=ALL-UNNAMED",
            "-XX:ErrorFile=${errorFile.absolutePath}",
            "-Dwinui4k.ffi=$backend",
            "-cp",
            System.getProperty("java.class.path"),
            ModalLoopProbe::class.java.name,
            scenario,
        ).redirectErrorStream(true).start()
        val output = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach { synchronized(output) { output.appendLine(it) } } }
        }.apply { start() }
        val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly().waitFor()
        reader.join(TimeUnit.SECONDS.toMillis(5))
        val text = synchronized(output) { output.toString() }
        check(finished) { "the child process for $backend / $scenario does not end within ${TIMEOUT_SECONDS} seconds (the loop hung):\n$text" }
        val results = text.lineSequence().filter { it.startsWith("RESULT ") }.map { it.removePrefix("RESULT ") }.toList()
        val crashed = errorFile.exists()
        if (crashed) println("the child JVM for $backend / $scenario crashed: $errorFile")
        return Outcome(process.exitValue(), results, crashed).also { if (it.exitCode != EXPECTED_EXIT[scenario]) println(text) }
    }

    /** A test that runs a child process with [backend]. Skipped with a reason if the backend is not available on this architecture (JNA on Arm64). */
    private fun backendTest(backend: String, name: String, body: suspend () -> Unit) {
        test(name).config(enabledOrReasonIf = { availability(backend) }) { body() }
    }

    private fun availability(backend: String): Enabled =
        if (backend == "jna" && System.getProperty("os.arch").orEmpty().lowercase() !in X64_ARCHS) {
            Enabled.disabled("the JNA backend is Windows x64 only (os.arch=${System.getProperty("os.arch")})")
        } else {
            Enabled.enabled
        }

    init {
        for (backend in listOf("panama", "jna", "jnr")) {
            backendTest(backend, "$backend: waiting for a dialog inside a button's Click returns NONE on Esc and PRIMARY on Enter (invokeLater runs inside the loop too)") {
                runProbe(backend, "dialog") shouldBe Outcome(0, listOf("dialog true:NONE@1,true:PRIMARY@1"))
            }

            backendTest(backend, "$backend: calling WinUiUtilities.exit inside the loop makes the loop return false, and WM_QUIT reaches the outer loop and the process ends") {
                runProbe(backend, "exit") shouldBe Outcome(0, listOf("returned=false depth=0"))
            }

            backendTest(backend, "$backend: closing the only window inside the loop makes the loop return false on the automatic-exit WM_QUIT, and the process ends") {
                runProbe(backend, "close-last-window") shouldBe Outcome(0, listOf("returned=false depth=0"))
            }

            // Ending the JVM with System.exit while the UI thread remains can crash the JVM during shutdown with Panama and JNR
            // (leaving an hs_err; the exit code stays the System.exit value). It crashes the same way without running the loop
            // (doc/troubleshooting.md), so whether it crashed is not compared; only the exit code and output are checked
            backendTest(backend, "$backend: calling System.exit inside the loop ends the process with that exit code even while the UI thread is in the loop") {
                runProbe(backend, "system-exit").copy(crashed = false) shouldBe
                    Outcome(ModalLoopProbe.SYSTEM_EXIT_CODE, listOf("system-exit"))
            }

            backendTest(backend, "$backend: for comparison, calling System.exit without running the loop ends with the same exit code") {
                runProbe(backend, "system-exit-no-loop").copy(crashed = false) shouldBe
                    Outcome(ModalLoopProbe.SYSTEM_EXIT_CODE, listOf("system-exit"))
            }
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 45L

        /** The x64 os.arch values, the same as JnaBackendProvider.isAvailable. */
        val X64_ARCHS = setOf("amd64", "x86_64")
        val EXPECTED_EXIT = mapOf("dialog" to 0, "exit" to 0, "close-last-window" to 0, "system-exit" to ModalLoopProbe.SYSTEM_EXIT_CODE, "system-exit-no-loop" to ModalLoopProbe.SYSTEM_EXIT_CODE)
    }
}
