package com.appkitbox.winui4k

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A WinUI test harness shared by every test spec.
 *
 * The WinUI message loop can be started only once per JVM, and once the last window closes it exits and cannot be
 * restarted (see WinUiUtilities.ensureStarted). So a single shared window is created lazily and reused across all specs
 * and test classes, and [UiTestSessionListener] closes it with [shutdown] when the entire JUnit Platform run finishes.
 * Both the Kotest (Kotlin) and JUnit Jupiter (Java) engines run in the same test task, so it is not closed by per-engine
 * teardown. Each test places its components in the shared window with [attach] and, when done, calls [detach] so that
 * nothing is left behind to affect other tests.
 *
 * Java tests can call these as static methods, such as `UiTestHarness.onUiThread(() -> ...)`
 * (onUiThread takes a Runnable and onUiThreadGet takes a Callable).
 */
object UiTestHarness {
    /** Upper bound on the wait for UI-thread work / event arrival. */
    const val TIMEOUT_SECONDS = 60L

    @Volatile
    private var frame: WFrame? = null

    /** Returns the shared window, opening it first if it doesn't exist yet. */
    @Synchronized
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

    /**
     * Hands out the shared window itself for tests that need it (e.g. to specify a picker's
     * owner). Because this uses [onUiThread] internally, never call it from the UI thread
     * (inside an [onUiThread] block) — that deadlocks waiting on itself.
     */
    @JvmStatic
    fun sharedFrame(): WFrame = ensureFrame()

    /** Adds [component] to the shared window's contentPane. */
    @JvmStatic
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
    @JvmStatic
    fun attachAndAwaitLoaded(component: WComponent) {
        val loaded = CountDownLatch(1)
        val listener: () -> Unit = { loaded.countDown() }
        onUiThread { component.addLoadedListener(listener) }
        attach(component)
        check(loaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "The Loaded event never arrived" }
        onUiThread { component.removeLoadedListener(listener) }
    }

    /** Removes an [attach]ed [component] from the shared window. */
    @JvmStatic
    fun detach(component: WComponent) {
        val frame = frame ?: return
        onUiThread { frame.contentPane.remove(component) }
    }

    /** Runs [block] on the UI thread and waits for it to finish. Rethrows any exception to the caller. */
    @JvmSynthetic
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

    /** [onUiThread] for Java (takes a Runnable so it can be called as `UiTestHarness.onUiThread(() -> ...)`). */
    @JvmStatic
    @JvmName("onUiThread")
    fun onUiThreadForJava(block: Runnable) {
        onUiThread { block.run() }
    }

    /** Runs [block] on the UI thread and returns its result. */
    @JvmSynthetic
    fun <T> onUiThreadGet(block: () -> T): T {
        var result: T? = null
        onUiThread { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /** [onUiThreadGet] for Java (takes a Callable so it can be called as `UiTestHarness.onUiThreadGet(() -> ...)`). */
    @JvmStatic
    @JvmName("onUiThreadGet")
    fun <T> onUiThreadGetForJava(block: Callable<T>): T = onUiThreadGet { block.call() }

    /**
     * Closes the shared window and waits for the WinUI message loop to exit. Call only once, after all tests finish
     * ([UiTestSessionListener] calls it when the JUnit Platform session ends).
     * once, after all tests have finished. Gradle's test worker calls System.exit right after
     * the test finishes, so wait for WinUI's message loop and cleanup (RoUninitialize, etc.) to
     * finish before exiting (without waiting, a COM upcall during shutdown can crash the JVM).
     */
    @JvmStatic
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
