package com.appkitbox.winui4k

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * An E2E test that actually launches WinUI + WebView2 to verify WWebView's script
 * interop (ExecuteScript / PostWebMessage / postMessage from the page).
 *
 * WinUI's message loop exits once the last window closes and can't be restarted, so the
 * window and page are set up only once for the class ([BeforeAll]) and shared across all tests.
 */
class WWebViewTest {
    companion object {
        /** Upper bound on the wait for page load / script response (the first one includes browser-process startup). */
        private const val TIMEOUT_SECONDS = 60L

        private lateinit var frame: WFrame
        private lateinit var webView: WWebView

        /** An arrival-order queue of messages (JSON representation) received via WebMessageReceived. */
        private val receivedMessages = LinkedBlockingQueue<String>()

        /**
         * The test page. Messages from Kotlin are sent back with an explicit "echo:" prefix,
         * and a button click sends a fixed string in the same shape as the Gallery demo's
         * "Send to Kotlin".
         */
        private val TEST_HTML = """
            <!doctype html>
            <html><body>
            <h3 id="title">Script interop test</h3>
            <button id="send" onclick="window.chrome.webview.postMessage('Sent from button')">Send to Kotlin</button>
            <script>
            window.chrome.webview.addEventListener("message", (e) => {
                const payload = (typeof e.data === "string") ? e.data : JSON.stringify(e.data);
                window.chrome.webview.postMessage("echo:" + payload);
            });
            </script>
            </body></html>
        """.trimIndent()

        @JvmStatic
        @BeforeAll
        fun openTestPage() {
            val navigationCompleted = CountDownLatch(1)
            var navigationSucceeded = false
            onUiThread {
                frame = WFrame("WWebViewTest")
                webView = WWebView()
                webView.width = 640.0
                webView.height = 400.0
                webView.addWebMessageReceivedListener { messageAsJson ->
                    receivedMessages.add(messageAsJson)
                }
                webView.addNavigationCompletedListener { isSuccess, _ ->
                    navigationSucceeded = isSuccess
                    navigationCompleted.countDown()
                }
                frame.add(webView)
                webView.navigateToString(TEST_HTML)
                frame.isVisible = true
            }
            assertTrue(
                navigationCompleted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "The test page didn't finish loading within $TIMEOUT_SECONDS seconds",
            )
            assertTrue(navigationSucceeded, "The test page failed to load")
        }

        @JvmStatic
        @AfterAll
        fun closeWindow() {
            // Make sure no window is left behind even if BeforeAll failed
            if (Companion::frame.isInitialized) {
                onUiThread {
                    webView.close() // terminate the browser process first
                    frame.isVisible = false
                }
            }
            // Gradle's test worker calls System.exit right after the test finishes, so wait for
            // WinUI's message loop and cleanup (RoUninitialize, etc.) to finish before exiting
            // (without waiting, a COM upcall during shutdown can crash the JVM)
            Thread.getAllStackTraces().keys.firstOrNull { it.name == "WinUI4K-UI" }
                ?.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
        }

        /** Runs [block] on the UI thread and waits for it to finish. Rethrows any exception to the caller. */
        private fun onUiThread(block: () -> Unit) {
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

        /** Reads through the queue until the [expected] message arrives. Returns null if it never does. */
        private fun awaitMessage(expected: String): String? {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
            while (true) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) return null
                val message = receivedMessages.poll(remaining, TimeUnit.NANOSECONDS) ?: return null
                if (message == expected) return message
            }
        }
    }

    @Test
    fun `executeScript evaluates the page's DOM and returns the result as JSON`() {
        val results = LinkedBlockingQueue<String>()
        onUiThread {
            webView.executeScript("document.getElementById('title').textContent") { result ->
                results.add(result)
            }
        }
        val result = results.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertNotNull(result, "executeScript's result never came back")
        // The result is the JSON representation of the last expression's value (a string is quoted)
        assertEquals("\"Script interop test\"", result)
    }

    @Test
    fun `executeScript returns the JSON "null" when there's no value`() {
        val results = LinkedBlockingQueue<String>()
        onUiThread {
            webView.executeScript("void 0") { result ->
                results.add(result)
            }
        }
        assertEquals("null", results.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `postWebMessageAsString delivers the message to the page as-is`() {
        onUiThread { webView.postWebMessageAsString("Hello") }
        // The page side checks typeof e.data === "string" before echoing it.
        // The receiving side (WebMessageReceived) is JSON, so the whole thing ends up quoted
        val expected = "\"echo:Hello\""
        assertEquals(expected, awaitMessage(expected), "The string message's echo never arrived")
    }

    @Test
    fun `postWebMessageAsJson delivers a JSON-parsed object to the page`() {
        onUiThread { webView.postWebMessageAsJson("""{"value":42}""") }
        // On the page side, e.data becomes an object, and it comes back round-tripped through JSON.stringify
        val expected = "\"echo:{\\\"value\\\":42}\""
        assertEquals(expected, awaitMessage(expected), "The JSON message's echo never arrived")
    }

    @Test
    fun `a click on the in-page button reaches WebMessageReceived`() {
        // Same path as the Gallery demo's "Send to Kotlin" button (a DOM click event -> postMessage)
        onUiThread { webView.executeScript("document.getElementById('send').click()") }
        val expected = "\"Sent from button\""
        assertEquals(expected, awaitMessage(expected), "The button-click message never arrived")
    }

    @Test
    fun `isCoreWebView2Initialized becomes true after navigation completes`() {
        var initialized = false
        onUiThread { initialized = webView.isCoreWebView2Initialized }
        assertTrue(initialized)
    }

    @Test
    fun `setting source to an empty string is ignored and the current value is kept`() {
        var before = ""
        var after = ""
        onUiThread {
            before = webView.source
            webView.source = "" // ignored because an empty string becomes a NULL HSTRING and CreateUri fails
            after = webView.source
        }
        assertEquals(before, after)
    }

    @Test
    fun `operation APIs are no-ops (not exceptions) before CoreWebView2 is initialized`() {
        // Call each API on a WWebView that isn't placed in a window = CoreWebView2 never gets
        // initialized. The native side throws E_ILLEGAL_METHOD_CALL for calls before
        // initialization, but the wrapper is specified to no-op instead (if an exception leaks
        // out, onUiThread rethrows it and the test fails)
        onUiThread {
            val detached = WWebView()
            assertTrue(!detached.isCoreWebView2Initialized)
            detached.reload()
            detached.goBack()
            detached.goForward()
            detached.executeScript("1 + 1") { }
            detached.postWebMessageAsString("before initialization")
            assertEquals("", detached.documentTitle)
        }
    }

    @Test
    fun `defaultBackgroundColor returns exactly the color that was set`() {
        var color = WColor.BLACK
        onUiThread {
            webView.defaultBackgroundColor = WColor(200, 100, 50, 255)
            color = webView.defaultBackgroundColor
        }
        assertEquals(200, color.red)
        assertEquals(100, color.green)
        assertEquals(50, color.blue)
        assertEquals(255, color.alpha)
    }
}
