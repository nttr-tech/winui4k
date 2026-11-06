package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

/**
 * A verification app that [ModalLoopProcessTest] launches as a child process. Exiting the app (Application.Exit, closing the last window,
 * System.exit) cannot be done inside the shared-window tests, so each scenario is launched in a separate JVM. The FFI backend is
 * chosen by the parent with -Dwinui4k.ffi. Results are written to standard output as "RESULT <content>" lines.
 *
 * Scenarios (the first argument):
 * - dialog: opens a dialog inside a button's Click and waits, closes it with Esc, then presses the default button with Enter
 * - exit: calls WinUiUtilities.exit (Application.Exit) inside the loop
 * - close-last-window: closes the only window inside the loop (automatic exit)
 * - system-exit: calls System.exit(7) from another thread inside the loop
 * - system-exit-no-loop: for comparison, calls System.exit(7) without running the loop (the UI thread is in WinUI's own loop)
 */
object ModalLoopProbe {
    /** The window title. [TestKeyboard] sends keys to the window with this title. */
    private const val TITLE = "winui4k tests"
    private const val TIMEOUT_SECONDS = 20L
    private const val SETTLE_MILLIS = 600L
    private const val VK_ESCAPE = 0x1B
    private const val SCAN_ESCAPE = 0x01

    /** The exit code [ModalLoopProcessTest] expects in the System.exit scenarios. */
    const val SYSTEM_EXIT_CODE = 7

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            when (val scenario = args.single()) {
                "dialog" -> dialog()
                "exit" -> quitInLoop { WinUiUtilities.exit() }
                "close-last-window" -> quitInLoop { TopLevelWindows.close(checkNotNull(TopLevelWindows.find(null, TITLE))) }
                "system-exit" -> systemExitInLoop()
                "system-exit-no-loop" -> systemExitWithoutLoop()
                else -> error("unknown scenario: $scenario")
            }
        } catch (e: Throwable) {
            // The UI thread is non-daemon, so end explicitly on failure (the parent learns of the failure from the exit code and output)
            println(e.stackTraceToString())
            System.out.flush()
            exitProcess(1)
        }
    }

    private fun <T> ui(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        val done = CountDownLatch(1)
        WinUiUtilities.invokeLater {
            result.set(runCatching(block))
            done.countDown()
        }
        check(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "the UI thread does not respond" }
        return result.get().getOrThrow()
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "gave up waiting: $what" }
            Thread.sleep(50)
        }
    }

    /** Shows [button] in a window titled [TITLE] and waits for Loaded. */
    private fun showFrame(button: WButton) {
        val loaded = CountDownLatch(1)
        ui {
            val frame = WFrame(TITLE)
            frame.add(button)
            button.addLoadedListener { loaded.countDown() }
            frame.isVisible = true
        }
        check(loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "the window does not open" }
    }

    private fun dialog() {
        val results = java.util.Collections.synchronizedList(mutableListOf<String>())
        val button = ui { WButton("Open") }
        val dialog = ui {
            WContentDialog("Confirm", WLabel("Continue?")).apply {
                primaryButtonText = "OK"
                closeButtonText = "Cancel"
                defaultButton = ContentDialogButton.PRIMARY
            }
        }
        ui {
            button.addActionListener {
                var result: ContentDialogResult? = null
                dialog.show(button) { result = it }
                val returned = ModalLoop.run { result != null }
                results += "$returned:$result@${ModalLoop.depth + 1}"
            }
        }
        showFrame(button)
        for (key in listOf("escape", "enter")) {
            ui { check(button.requestFocusInWindow()) }
            TestKeyboard.pressEnter()
            await("the dialog opens") { ui { dialog.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            check(ui { ModalLoop.depth } == 1) // invokeLater runs inside the loop too
            Thread.sleep(SETTLE_MILLIS)
            if (key == "escape") TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE) else TestKeyboard.pressEnter()
            val expected = if (key == "escape") 1 else 2
            await("the dialog closes and the rest of Click runs") { results.size == expected }
            await("the dialog finishes closing") { !ui { dialog.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
        }
        println("RESULT dialog ${results.joinToString(",")}")
        WinUiUtilities.exit()
    }

    /** Runs the loop and calls [quit] inside it. Writes the value the loop returned and leaves the rest to the app exiting (the end of main). */
    private fun quitInLoop(quit: () -> Unit) {
        val button = ui { WButton("Wait") }
        showFrame(button)
        val returned = CountDownLatch(1)
        WinUiUtilities.invokeLater {
            val value = ModalLoop.run { false }
            println("RESULT returned=$value depth=${ModalLoop.depth}")
            returned.countDown()
        }
        await("enters the loop") { ui { ModalLoop.depth } == 1 }
        quit()
        check(returned.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "the loop does not return on WM_QUIT" }
        // The JVM ends when the UI thread (non-daemon) leaves Application.Start
    }

    private fun systemExitInLoop() {
        val button = ui { WButton("Wait") }
        showFrame(button)
        WinUiUtilities.invokeLater { ModalLoop.run { false } }
        await("enters the loop") { ui { ModalLoop.depth } == 1 }
        println("RESULT system-exit")
        System.out.flush()
        exitProcess(SYSTEM_EXIT_CODE)
    }

    private fun systemExitWithoutLoop() {
        showFrame(ui { WButton("Wait") })
        println("RESULT system-exit")
        System.out.flush()
        exitProcess(SYSTEM_EXIT_CODE)
    }
}
