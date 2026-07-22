package com.appkitbox.winui4k

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A WinUI test harness shared by every test spec.
 *
 * WinUI's message loop can only be started once per JVM, and it ends (and can never be
 * restarted) once the last window closes (see WinUiUtilities.ensureStarted). So a single
 * shared window is lazily created once and reused across every spec, and [ProjectConfig]
 * .afterProject closes it via [shutdown] after all tests finish. Each spec should [attach]
 * its component to the shared window and [detach] it in afterSpec so it doesn't leak state
 * into other specs.
 */
object UiTestHarness {
    /** Upper bound on the wait for UI-thread work / event arrival. */
    const val TIMEOUT_SECONDS = 60L

    private var frame: WFrame? = null

    /** Returns the shared window, opening it first if it doesn't exist yet. */
    private fun ensureFrame(): WFrame {
        frame?.let { return it }
        lateinit var created: WFrame
        onUiThread {
            created = WFrame("winui4k tests")
            // Window.Activate (isVisible = true) brings the window to the foreground and steals
            // focus, so show it inactively via AppWindow.Show(false) instead (WebView2
            // initialization needs a visible window, so it can't stay hidden)
            created.appWindow.show(activate = false)
        }
        frame = created
        return created
    }

    /** Adds [component] to the shared window's contentPane. */
    fun attach(component: WComponent) {
        val frame = ensureFrame()
        onUiThread { frame.add(component) }
    }

    /**
     * Adds [component] to the shared window and waits for its Loaded event (template applied).
     * Control-internal events like TextChanged and SelectedDateChanged don't fire for property
     * changes made before template application, so tests that verify such events should use
     * this instead of [attach].
     */
    fun attachAndAwaitLoaded(component: WComponent) {
        val loaded = CountDownLatch(1)
        val listener: () -> Unit = { loaded.countDown() }
        onUiThread { component.addLoadedListener(listener) }
        attach(component)
        check(loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "The Loaded event never arrived" }
        onUiThread { component.removeLoadedListener(listener) }
    }

    /** Removes an [attach]ed [component] from the shared window. */
    fun detach(component: WComponent) {
        val frame = frame ?: return
        onUiThread { frame.contentPane.remove(component) }
    }

    /** Runs [block] on the UI thread and waits for it to finish. Rethrows any exception to the caller. */
    fun onUiThread(block: () -> Unit) {
        val done = CountDownLatch(1)
        var error: Throwable? = null
        WinUiUtilities.invokeLater {
            try {
                block()
            } catch (t: Throwable) {
                error = t
            } finally {
                done.countDown()
            }
        }
        check(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "Processing on the UI thread timed out" }
        error?.let { throw it }
    }

    /** Runs [block] on the UI thread and returns its result. */
    fun <T> onUiThreadGet(block: () -> T): T {
        var result: T? = null
        onUiThread { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * Closes the shared window and waits for WinUI's message loop to finish. Call this exactly
     * once, after all tests have finished. Gradle's test worker calls System.exit right after
     * the test finishes, so wait for WinUI's message loop and cleanup (RoUninitialize, etc.) to
     * finish before exiting (without waiting, a COM upcall during shutdown can crash the JVM).
     */
    fun shutdown() {
        // No UI thread = WinUI never started (no test that uses the UI ever ran), so do nothing
        val uiThread = Thread.getAllStackTraces().keys.firstOrNull { it.name == "WinUI4K-UI" } ?: return
        val frame = this.frame
        if (frame != null) {
            this.frame = null
            onUiThread { frame.isVisible = false } // the last window closing triggers auto-exit
        } else {
            // The message loop is still running even if only window-free tests ran, so exit explicitly.
            // Swallow the IllegalStateException that exit() throws if it has already exited
            runCatching { WinUiUtilities.exit() }
        }
        uiThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
    }
}
