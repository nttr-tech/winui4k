package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [WFrame.addComponentListener], like Component.addComponentListener: changing the window's size and position delivers
 * componentResized / componentMoved, the source is the window and getComponent is contentPane. The expected values are decided from how a
 * ComponentListener added to a JFrame is called.
 */
@Tags("Integration")
class WFrameComponentListenerTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private var frame: WFrame? = null

    /** A listener that records received events as "resized" / "moved" and checks source, component and id. */
    private fun recorder(frame: WFrame) = object : WComponentAdapter() {
        override fun componentResized(e: WComponentEvent) = record("resized", e, WComponentEvent.COMPONENT_RESIZED)

        override fun componentMoved(e: WComponentEvent) = record("moved", e, WComponentEvent.COMPONENT_MOVED)

        override fun componentShown(e: WComponentEvent) = record("shown", e, WComponentEvent.COMPONENT_SHOWN)

        override fun componentHidden(e: WComponentEvent) = record("hidden", e, WComponentEvent.COMPONENT_HIDDEN)

        private fun record(name: String, e: WComponentEvent, id: Int) {
            val ok = e.id == id && e.source === frame && e.component === frame.contentPane
            events += if (ok) name else "$name:wrong"
        }
    }

    /** Shows our own window (waits for the notifications that come with showing to finish, then clears the record). */
    private suspend fun showFrame(): WFrame {
        val frame = onUiThreadGet {
            WFrame(TITLE).also {
                frame = it
                it.setBounds(100, 100, 400, 300)
                it.isVisible = true
            }
        }
        eventually(5.seconds) { onUiThreadGet { frame.isActive } shouldBe true }
        Thread.sleep(SETTLE_MILLIS)
        return frame
    }

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        beforeTest { events.clear() }
        afterTest {
            frame?.let { onUiThread { it.dispose() } }
            frame = null
        }

        context("Defaults") {
            test("componentListeners are empty at first and listed in the order added. null is not added") {
                onUiThread {
                    val frame = WFrame(TITLE).also { frame = it }
                    frame.componentListeners.size shouldBe 0
                    val first = recorder(frame)
                    val second = recorder(frame)
                    frame.addComponentListener(first)
                    frame.addComponentListener(null)
                    frame.addComponentListener(second)
                    frame.componentListeners.toList() shouldBe listOf(first, second)
                }
            }
        }

        context("Events") {
            test("setSize delivers componentResized and setLocation componentMoved, the source is the window, and getComponent is contentPane") {
                val frame = showFrame()
                onUiThread {
                    frame.addComponentListener(recorder(frame))
                    frame.setSize(500, 420)
                }
                eventually(5.seconds) { events() shouldContain "resized" }
                events() shouldNotContain "moved"
                events.clear()
                onUiThread { frame.setLocation(160, 140) }
                eventually(5.seconds) { events() shouldContain "moved" }
                events() shouldNotContain "resized"
                events().filter { it.endsWith(":wrong") } shouldBe emptyList()
            }

            test("a listener removed with removeComponentListener is not called") {
                val frame = showFrame()
                val listener = onUiThreadGet { recorder(frame).also { frame.addComponentListener(it) } }
                onUiThread {
                    frame.removeComponentListener(listener)
                    frame.componentListeners.size shouldBe 0
                    frame.setBounds(120, 120, 480, 360)
                }
                Thread.sleep(SETTLE_MILLIS)
                events() shouldBe emptyList()
            }
        }

        context("Swing compatibility") {
            test("unlike Swing, hiding and showing do not call componentShown / componentHidden") {
                val frame = showFrame()
                onUiThread {
                    frame.addComponentListener(recorder(frame))
                    frame.isVisible = false
                    frame.isVisible = true
                }
                Thread.sleep(SETTLE_MILLIS)
                events().filter { it.startsWith("shown") || it.startsWith("hidden") } shouldBe emptyList()
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame component listener test"
        const val SETTLE_MILLIS = 500L
    }
}
