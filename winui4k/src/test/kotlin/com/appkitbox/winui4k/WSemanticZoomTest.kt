package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests WSemanticZoom's view switching and the ChangeListener that reports the end of a switch.
 * ViewChangeCompleted arrives after the switching animation, so the test waits while keeping the UI thread free.
 */
class WSemanticZoomTest : FunSpec() {
    private fun semanticZoom(): WSemanticZoom = WSemanticZoom(WList(listOf("a1", "a2", "b1")), WList(listOf("a", "b")))

    /** Waits until [condition] becomes true on the UI thread (up to 10 seconds). True if it became true. */
    private fun awaitOnUiThread(condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TIMEOUT_NANOS
        while (System.nanoTime() < deadline) {
            if (onUiThreadGet(condition)) return true
            Thread.sleep(POLL_MILLIS)
        }
        return false
    }

    init {
        test("toggleActiveView swaps the zoomed-out view and the zoomed-in view") {
            val zoom = onUiThreadGet { semanticZoom() }
            UiTestHarness.attachAndAwaitLoaded(zoom)
            try {
                val states = mutableListOf<Boolean>()
                onUiThread { states += zoom.isZoomedInViewActive }
                onUiThread { zoom.toggleActiveView() }
                awaitOnUiThread { !zoom.isZoomedInViewActive }
                onUiThread { states += zoom.isZoomedInViewActive }
                states shouldBe listOf(true, false)
            } finally {
                UiTestHarness.detach(zoom)
            }
        }

        test("ChangeListener is called every time a switch finishes, and inside the listener isZoomedInViewActive after the switch can be read") {
            val zoom = onUiThreadGet { semanticZoom() }
            UiTestHarness.attachAndAwaitLoaded(zoom)
            try {
                val notified = mutableListOf<Boolean>()
                val sources = mutableListOf<Any>()
                onUiThread {
                    zoom.addChangeListener { e ->
                        sources += e.source
                        notified += (e.source as WSemanticZoom).isZoomedInViewActive
                    }
                    zoom.toggleActiveView()
                }
                awaitOnUiThread { notified.size == 1 }
                onUiThread { zoom.isZoomedInViewActive = true }
                awaitOnUiThread { notified.size == 2 }
                notified shouldBe listOf(false, true)
                sources.all { it === zoom } shouldBe true
            } finally {
                UiTestHarness.detach(zoom)
            }
        }

        test("a listener removed with removeChangeListener is not called") {
            val zoom = onUiThreadGet { semanticZoom() }
            UiTestHarness.attachAndAwaitLoaded(zoom)
            try {
                val calls = mutableListOf<String>()
                onUiThread {
                    val removed = WChangeListener { calls += "removed" }
                    zoom.addChangeListener { calls += "kept" }
                    zoom.addChangeListener(removed)
                    zoom.removeChangeListener(removed)
                    zoom.toggleActiveView()
                }
                awaitOnUiThread { calls.isNotEmpty() }
                onUiThread { calls += zoom.getChangeListeners().size.toString() }
                calls shouldBe listOf("kept", "1")
            } finally {
                UiTestHarness.detach(zoom)
            }
        }
    }

    private companion object {
        const val TIMEOUT_NANOS = 10_000_000_000L
        const val POLL_MILLIS = 50L
    }
}
