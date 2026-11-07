package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationTargetException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests that verify [WTimer], which corresponds to javax.swing.Timer, and [WinUiUtilities.invokeAndWait], which corresponds to SwingUtilities.invokeAndWait.
 * The expected values come from Swing's rules (repeating by default, listeners on the UI thread, setRepeats(false) stops after once, negative intervals throw,
 * invokeAndWait waits until the end and wraps exceptions in InvocationTargetException, and a call from the UI thread throws Error).
 * Times are compared in "at least" / "at most" forms so the tests pass on slow PCs too.
 */
@Tags("E2E")
class WTimerTest : FunSpec() {
    /** A listener that counts firings and remembers whether the firing thread was the UI thread. */
    private class Recorder(expected: Int) : WActionListener {
        val latch = CountDownLatch(expected)
        val events: MutableList<WActionEvent> = Collections.synchronizedList(ArrayList())
        val onUiThread: MutableList<Boolean> = Collections.synchronizedList(ArrayList())

        override fun actionPerformed(e: WActionEvent) {
            events += e
            onUiThread += WinUiUtilities.isDispatchThread
            latch.countDown()
        }
    }

    init {
        context("WTimer defaults and settings") {
            test("the defaults are repeating, coalescing, initialDelay equal to the constructor's delay, and stopped (as in Swing)") {
                val timer = WTimer(250, null)
                listOf(timer.isRepeats, timer.isCoalesce, timer.initialDelay, timer.delay, timer.isRunning()) shouldBe
                    listOf(true, true, 250, 250, false)
            }

            test("negative delay and initialDelay throw IllegalArgumentException (as in Swing)") {
                shouldThrow<IllegalArgumentException> { WTimer(-1, null) }
                val timer = WTimer(0, null)
                shouldThrow<IllegalArgumentException> { timer.delay = -1 }
                shouldThrow<IllegalArgumentException> { timer.initialDelay = -5 }
            }

            test("listeners added with addActionListener can be read with actionListeners in the order added and are removed with remove") {
                val first = WActionListener { }
                val second = WActionListener { }
                val timer = WTimer(10, first)
                timer.addActionListener(second)
                timer.addActionListener(null)
                val before = timer.actionListeners.toList()
                timer.removeActionListener(first)
                (before == listOf(first, second) && timer.actionListeners.toList() == listOf(second)) shouldBe true
            }
        }

        context("WTimer firing") {
            test("after start, the listener is called repeatedly on the UI thread, source is the timer, and actionCommand is the set value") {
                val recorder = Recorder(3)
                val timer = WTimer(30, recorder)
                timer.actionCommand = "tick"
                timer.start()
                try {
                    recorder.latch.await(5, TimeUnit.SECONDS) shouldBe true
                    timer.isRunning() shouldBe true
                } finally {
                    timer.stop()
                }
                recorder.onUiThread.toList() shouldContainOnly listOf(true)
                recorder.events.all { it.source === timer && it.actionCommand == "tick" } shouldBe true
            }

            test("after stop it is not called, and isRunning is false") {
                val recorder = Recorder(1)
                val timer = WTimer(20, recorder)
                timer.start()
                recorder.latch.await(5, TimeUnit.SECONDS) shouldBe true
                timer.stop()
                // Wait for firings posted before stop to arrive before counting
                UiTestHarness.onUiThread { }
                val count = recorder.events.size
                Thread.sleep(200)
                (recorder.events.size to timer.isRunning()) shouldBe (count to false)
            }

            test("with setRepeats(false) it is called only once, and then isRunning is false (false even inside the firing)") {
                val runningInListener = Collections.synchronizedList(ArrayList<Boolean>())
                lateinit var timer: WTimer
                val recorder = Recorder(1)
                timer = WTimer(20) { e ->
                    runningInListener += timer.isRunning()
                    recorder.actionPerformed(e)
                }
                timer.isRepeats = false
                timer.start()
                recorder.latch.await(5, TimeUnit.SECONDS) shouldBe true
                Thread.sleep(200)
                listOf(recorder.events.size, timer.isRunning(), runningInListener.toList()) shouldBe listOf(1, false, listOf(false))
            }

            test("the first firing is after initialDelay, and from the second on every delay") {
                val times = Collections.synchronizedList(ArrayList<Long>())
                val latch = CountDownLatch(3)
                val timer = WTimer(40) {
                    times += System.nanoTime()
                    latch.countDown()
                }
                timer.initialDelay = 400
                val started = System.nanoTime()
                timer.start()
                try {
                    latch.await(5, TimeUnit.SECONDS) shouldBe true
                } finally {
                    timer.stop()
                }
                val firstMillis = TimeUnit.NANOSECONDS.toMillis(times[0] - started).toInt()
                val secondGap = TimeUnit.NANOSECONDS.toMillis(times[2] - times[1]).toInt()
                firstMillis shouldBeGreaterThanOrEqual 350
                (secondGap < 350) shouldBe true
            }

            test("restart stops and starts again, and a second start while running does nothing") {
                val recorder = Recorder(1)
                val timer = WTimer(10_000, recorder)
                timer.start()
                timer.start()
                timer.initialDelay = 20
                timer.restart()
                try {
                    recorder.latch.await(5, TimeUnit.SECONDS) shouldBe true
                } finally {
                    timer.stop()
                }
                timer.isRunning() shouldBe false
            }

            test("even when created and started / stopped off the UI thread, the listener is called on the UI thread") {
                val recorder = Recorder(2)
                val timer = onUiThreadGet { WTimer(20, recorder) }
                timer.start()
                try {
                    recorder.latch.await(5, TimeUnit.SECONDS) shouldBe true
                } finally {
                    timer.stop()
                }
                recorder.onUiThread.toList() shouldContainOnly listOf(true)
            }
        }

        context("invokeAndWait") {
            test("runs on the UI thread and waits until it ends") {
                var ranOnUiThread = false
                var finished = false
                WinUiUtilities.invokeAndWait {
                    Thread.sleep(100)
                    ranOnUiThread = WinUiUtilities.isDispatchThread
                    finished = true
                }
                (ranOnUiThread to finished) shouldBe (true to true)
            }

            test("an exception in the work is wrapped in InvocationTargetException and thrown to the calling thread") {
                val failure = IllegalStateException("failure")
                val thrown = shouldThrow<InvocationTargetException> { WinUiUtilities.invokeAndWait { throw failure } }
                (thrown.targetException === failure) shouldBe true
            }

            test("a call from the UI thread throws Error (as in Swing)") {
                onUiThreadGet {
                    runCatching { WinUiUtilities.invokeAndWait { } }.exceptionOrNull()?.javaClass
                } shouldBe Error::class.java
            }
        }
    }
}
