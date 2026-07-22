package com.appkitbox.winui4k

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * An E2E test that actually launches WinUI + WebView2 to verify WWebView's script
 * interop (ExecuteScript / PostWebMessage / postMessage from the page).
 *
 * The window comes from [UiTestHarness]'s shared window; the page is set up only once for the
 * spec (beforeSpec) and shared across all tests.
 */
class WWebViewTest : FunSpec() {
    companion object {
        /** Upper bound on the wait for page load / script response (the first one includes browser-process startup). */
        private const val TIMEOUT_SECONDS = UiTestHarness.TIMEOUT_SECONDS
        private val TIMEOUT = TIMEOUT_SECONDS.seconds

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
    }

    private lateinit var webView: WWebView

    /** An arrival-order queue of messages (JSON representation) received via WebMessageReceived. */
    private val receivedMessages = LinkedBlockingQueue<String>()

    /** Messages already drained from [receivedMessages]. Accumulates across [awaitMessage] retries. */
    private val drainedMessages = mutableListOf<String>()

    init {
        beforeSpec {
            val navigationCompleted = CountDownLatch(1)
            var navigationSucceeded = false
            onUiThread {
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
            }
            UiTestHarness.attach(webView)
            onUiThread { webView.navigateToString(TEST_HTML) }
            withClue("The test page didn't finish loading within $TIMEOUT_SECONDS seconds") {
                navigationCompleted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
            }
            withClue("The test page failed to load") {
                navigationSucceeded.shouldBeTrue()
            }
        }

        afterSpec {
            if (::webView.isInitialized) {
                onUiThread { webView.close() } // terminate the browser process
                UiTestHarness.detach(webView)
            }
        }

        test("executeScript evaluates the page's DOM and returns the result as JSON") {
            val results = LinkedBlockingQueue<String>()
            onUiThread {
                webView.executeScript("document.getElementById('title').textContent") { result ->
                    results.add(result)
                }
            }
            // The result is the JSON representation of the last expression's value (a string is quoted)
            withClue("executeScript's result never came back") {
                results.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "\"Script interop test\""
            }
        }

        test("executeScript returns the JSON \"null\" when there's no value") {
            val results = LinkedBlockingQueue<String>()
            onUiThread {
                webView.executeScript("void 0") { result ->
                    results.add(result)
                }
            }
            results.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "null"
        }

        test("postWebMessageAsString delivers the message to the page as-is") {
            onUiThread { webView.postWebMessageAsString("Hello") }
            // The page side checks typeof e.data === "string" before echoing it.
            // The receiving side (WebMessageReceived) is JSON, so the whole thing ends up quoted
            withClue("The string message's echo never arrived") {
                awaitMessage("\"echo:Hello\"")
            }
        }

        test("postWebMessageAsJson delivers a JSON-parsed object to the page") {
            onUiThread { webView.postWebMessageAsJson("""{"value":42}""") }
            // On the page side, e.data becomes an object, and it comes back round-tripped through JSON.stringify
            withClue("The JSON message's echo never arrived") {
                awaitMessage("\"echo:{\\\"value\\\":42}\"")
            }
        }

        test("a click on the in-page button reaches WebMessageReceived") {
            // Same path as the Gallery demo's "Send to Kotlin" button (a DOM click event -> postMessage)
            onUiThread { webView.executeScript("document.getElementById('send').click()") }
            withClue("The button-click message never arrived") {
                awaitMessage("\"Sent from button\"")
            }
        }

        test("isCoreWebView2Initialized becomes true after navigation completes") {
            var initialized = false
            onUiThread { initialized = webView.isCoreWebView2Initialized }
            initialized.shouldBeTrue()
        }

        test("setting source to an empty string is ignored and the current value is kept") {
            var before = ""
            var after = ""
            onUiThread {
                before = webView.source
                webView.source = "" // ignored because an empty string becomes a NULL HSTRING and CreateUri fails
                after = webView.source
            }
            after shouldBe before
        }

        test("operation APIs are no-ops (not exceptions) before CoreWebView2 is initialized") {
            // Call each API on a WWebView that isn't placed in a window = CoreWebView2 never gets
            // initialized. The native side throws E_ILLEGAL_METHOD_CALL for calls before
            // initialization, but the wrapper is specified to no-op instead (if an exception leaks
            // out, onUiThread rethrows it and the test fails)
            onUiThread {
                val detached = WWebView()
                detached.isCoreWebView2Initialized.shouldBeFalse()
                detached.reload()
                detached.goBack()
                detached.goForward()
                detached.executeScript("1 + 1") { }
                detached.postWebMessageAsString("before initialization")
                detached.documentTitle shouldBe ""
            }
        }

        test("defaultBackgroundColor returns exactly the color that was set") {
            var color = WColor.BLACK
            onUiThread {
                webView.defaultBackgroundColor = WColor(200, 100, 50, 255)
                color = webView.defaultBackgroundColor
            }
            assertSoftly(color) {
                red shouldBe 200
                green shouldBe 100
                blue shouldBe 50
                alpha shouldBe 255
            }
        }
    }

    /** Runs [block] on the UI thread and waits for it to finish. Rethrows any exception to the caller. */
    private fun onUiThread(block: () -> Unit) = UiTestHarness.onUiThread(block)

    /** Waits until the [expected] message arrives. Fails with the list of received messages so far if it never does. */
    private suspend fun awaitMessage(expected: String) {
        eventually(TIMEOUT) {
            receivedMessages.drainTo(drainedMessages)
            drainedMessages shouldContain expected
        }
    }
}
