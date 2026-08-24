package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests that verify Clipboard / DataPackage / DataPackageView round trips on the real clipboard.
 * They actually rewrite the OS clipboard, so anything copied during the test run is lost.
 */
class ClipboardTest : FunSpec() {
    init {
        // The clipboard is accessed from the STA (UI thread). Open the shared window first
        beforeSpec { UiTestHarness.sharedFrame() }

        test("text copied via setText reads back via contains / getText") {
            val expected = "winui4k clipboard test ${System.nanoTime()}"
            onUiThread {
                val dataPackage = DataPackage()
                dataPackage.setText(expected)
                Clipboard.setContent(dataPackage)
            }
            onUiThreadGet { Clipboard.getContent().contains(StandardDataFormats.TEXT) }.shouldBeTrue()
            awaitResult<String> { onResult ->
                Clipboard.getContent().getText { onResult(it) }
            } shouldBe expected
        }

        test("the copied format (Text) appears in availableFormats") {
            onUiThread {
                val dataPackage = DataPackage()
                dataPackage.setText("Format list test")
                Clipboard.setContent(dataPackage)
            }
            onUiThreadGet { Clipboard.getContent().availableFormats } shouldContain StandardDataFormats.TEXT
        }

        test("a URI copied via setUri reads back via getUri") {
            val expected = "https://example.com/winui4k"
            onUiThread {
                val dataPackage = DataPackage()
                dataPackage.setUri(expected)
                Clipboard.setContent(dataPackage)
            }
            onUiThreadGet { Clipboard.getContent().contains(StandardDataFormats.WEB_LINK) }.shouldBeTrue()
            val uri = awaitResult<String> { onResult ->
                Clipboard.getContent().getUri { onResult(it) }
            }
            uri.trimEnd('/') shouldBe expected // AbsoluteUri can gain a trailing "/"
        }

        test("requestedOperation set on the DataPackage reads back from the DataPackageView") {
            onUiThread {
                val dataPackage = DataPackage()
                dataPackage.setText("Operation kind test")
                dataPackage.requestedOperation = DataPackageOperation.MOVE
                dataPackage.requestedOperation shouldBe DataPackageOperation.MOVE
                Clipboard.setContent(dataPackage)
            }
            onUiThreadGet { Clipboard.getContent().requestedOperation } shouldBe DataPackageOperation.MOVE
        }

        test("a file path copied via setStorageItems reads back via getStorageItems") {
            val file = File.createTempFile("winui4k-clipboard", ".txt")
            file.deleteOnExit()
            onUiThread {
                val dataPackage = DataPackage()
                dataPackage.setStorageItems(file.absolutePath)
                Clipboard.setContent(dataPackage)
            }
            onUiThreadGet { Clipboard.getContent().contains(StandardDataFormats.STORAGE_ITEMS) }.shouldBeTrue()
            val paths = awaitResult<List<String>> { onResult ->
                Clipboard.getContent().getStorageItems { onResult(it) }
            }
            // On CI the temp directory comes back in 8.3 short form (e.g. RUNNER~1) while
            // paths from the clipboard are in long form, so resolve to real paths before comparing
            paths.map { it.toRealPath() } shouldContain file.absolutePath.toRealPath()
        }

        test("ClipboardContentOptions properties read back the set values, and setContent with options succeeds") {
            val options = ClipboardContentOptions()
            options.isAllowedInHistory = false
            options.isRoamable = false
            options.isAllowedInHistory.shouldBeFalse()
            options.isRoamable.shouldBeFalse()
            onUiThreadGet {
                val dataPackage = DataPackage()
                dataPackage.setText("Text kept out of history")
                Clipboard.setContent(dataPackage, options)
            }.shouldBeTrue()
        }

        test("clear removes the text from the clipboard") {
            onUiThread {
                val dataPackage = DataPackage()
                dataPackage.setText("Text to be cleared")
                Clipboard.setContent(dataPackage)
            }
            onUiThread { Clipboard.clear() }
            onUiThreadGet { Clipboard.getContent().contains(StandardDataFormats.TEXT) }.shouldBeFalse()
        }

        test("setContent invokes ContentChanged listeners, and not after removal") {
            val count = AtomicInteger()
            val notified = CountDownLatch(1)
            val listener: () -> Unit = {
                count.incrementAndGet()
                notified.countDown()
            }
            onUiThread { Clipboard.addContentChangedListener(listener) }
            try {
                copyText("Change notification test")
                notified.await(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
            } finally {
                onUiThread { Clipboard.removeContentChangedListener(listener) }
            }

            // Not called for setContent after removal. A separate still-subscribed listener confirms the notification made a round trip
            val countAfterRemove = count.get()
            val sentinelNotified = CountDownLatch(1)
            val sentinel: () -> Unit = { sentinelNotified.countDown() }
            onUiThread { Clipboard.addContentChangedListener(sentinel) }
            try {
                copyText("After removal test")
                sentinelNotified.await(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
            } finally {
                onUiThread { Clipboard.removeContentChangedListener(sentinel) }
            }
            count.get() shouldBe countAfterRemove
        }
    }

    /** Resolves to the real path with 8.3 short names expanded. */
    private fun String.toRealPath(): String = File(this).toPath().toRealPath().toString()

    /** Copies [text] to the clipboard on the UI thread. */
    private fun copyText(text: String) {
        onUiThread {
            val dataPackage = DataPackage()
            dataPackage.setText(text)
            Clipboard.setContent(dataPackage)
        }
    }

    /** Waits for and returns the result delivered via a callback on the UI thread. */
    private fun <T> awaitResult(start: ((T) -> Unit) -> Unit): T {
        val done = CountDownLatch(1)
        val result = AtomicReference<T>()
        onUiThread {
            start { value ->
                result.set(value)
                done.countDown()
            }
        }
        check(done.await(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "The callback never arrived" }
        return result.get()
    }
}
