package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Tests that verify [WSwingWorker], which corresponds to javax.swing.SwingWorker. The expected values come from Swing's rules (doInBackground runs off the UI thread,
 * process / done / change notifications run on the UI thread, all published values arrive in order, get in done returns the result, an exception becomes an
 * ExecutionException, the state goes PENDING -> STARTED -> DONE, and the progress is 0 to 100).
 */
@Tags("E2E")
class WSwingWorkerTest : FunSpec() {
    /** A worker that publishes 1 to [count], advances the progress, and returns the sum. Remembers where it was called. */
    private class SumWorker(private val count: Int) : WSwingWorker<Int, Int>() {
        val doneLatch = CountDownLatch(1)

        @Volatile var backgroundOnUiThread: Boolean? = null

        val processed: MutableList<Int> = Collections.synchronizedList(ArrayList())
        val processOnUiThread: MutableList<Boolean> = Collections.synchronizedList(ArrayList())

        @Volatile var doneOnUiThread: Boolean? = null

        @Volatile var resultInDone: Int? = null

        override fun doInBackground(): Int {
            backgroundOnUiThread = WinUiUtilities.isDispatchThread
            var sum = 0
            for (i in 1..count) {
                sum += i
                publish(i)
                progress = i * 100 / count
            }
            return sum
        }

        override fun process(chunks: List<Int>) {
            processOnUiThread += WinUiUtilities.isDispatchThread
            processed += chunks
        }

        override fun done() {
            doneOnUiThread = WinUiUtilities.isDispatchThread
            resultInDone = get()
            doneLatch.countDown()
        }
    }

    init {
        context("running") {
            test("doInBackground is called off the UI thread, process and done on the UI thread, and get in done returns the result") {
                val worker = SumWorker(20)
                worker.execute()
                worker.doneLatch.await(10, TimeUnit.SECONDS) shouldBe true
                listOf(worker.backgroundOnUiThread, worker.doneOnUiThread, worker.resultInDone) shouldBe listOf(false, true, 210)
                worker.processOnUiThread.all { it } shouldBe true
            }

            test("all published values arrive at process in order, even when coalesced") {
                val worker = SumWorker(200)
                worker.execute()
                worker.doneLatch.await(10, TimeUnit.SECONDS) shouldBe true
                // process is posted before done, so after done, one round of the UI thread delivers everything
                UiTestHarness.onUiThread { }
                worker.processed.toList() shouldBe (1..200).toList()
            }

            test("the state starts at PENDING, becomes DONE when finished, get returns the result, and isDone is true") {
                val worker = SumWorker(3)
                val initial = worker.state
                worker.execute()
                worker.get(10, TimeUnit.SECONDS) shouldBe 6
                worker.doneLatch.await(10, TimeUnit.SECONDS) shouldBe true
                UiTestHarness.onUiThread { }
                listOf(initial, worker.state, worker.isDone, worker.isCancelled) shouldBe
                    listOf(WSwingWorker.StateValue.PENDING, WSwingWorker.StateValue.DONE, true, false)
            }

            test("changes of progress and state arrive at the WPropertyChangeListener on the UI thread, and the last progress is 100") {
                val worker = SumWorker(10)
                val names = Collections.synchronizedList(ArrayList<String>())
                val onUi = Collections.synchronizedList(ArrayList<Boolean>())
                val lastProgress = Collections.synchronizedList(ArrayList<Any?>())
                val stateDone = CountDownLatch(1)
                worker.addPropertyChangeListener { evt ->
                    names += evt.propertyName
                    onUi += WinUiUtilities.isDispatchThread
                    if (evt.propertyName == "progress") lastProgress += evt.newValue
                    if (evt.newValue == WSwingWorker.StateValue.DONE) stateDone.countDown()
                    (evt.source === worker) shouldBe true
                }
                worker.execute()
                stateDone.await(10, TimeUnit.SECONDS) shouldBe true
                listOf(onUi.all { it }, "progress" in names, "state" in names, lastProgress.last()) shouldBe listOf(true, true, true, 100)
            }
        }

        context("exceptions and cancellation") {
            test("an exception in doInBackground becomes an ExecutionException in get, and done is called") {
                val failure = IllegalStateException("failure")
                val doneCalled = CountDownLatch(1)
                val worker = object : WSwingWorker<String, Unit>() {
                    override fun doInBackground(): String = throw failure

                    override fun done() {
                        doneCalled.countDown()
                    }
                }
                worker.execute()
                doneCalled.await(10, TimeUnit.SECONDS) shouldBe true
                val thrown = shouldThrow<ExecutionException> { worker.get() }
                (thrown.cause === failure) shouldBe true
            }

            test("cancelling before running makes isCancelled true, doInBackground is not called, and done is called") {
                var ran = false
                val doneCalled = CountDownLatch(1)
                val worker = object : WSwingWorker<String, Unit>() {
                    override fun doInBackground(): String {
                        ran = true
                        return ""
                    }

                    override fun done() {
                        doneCalled.countDown()
                    }
                }
                worker.cancel(true) shouldBe true
                worker.execute()
                doneCalled.await(10, TimeUnit.SECONDS) shouldBe true
                Thread.sleep(100)
                (worker.isCancelled to ran) shouldBe (true to false)
            }

            test("setting progress outside 0 to 100 throws IllegalArgumentException") {
                val worker = object : WSwingWorker<Unit, Unit>() {
                    override fun doInBackground() {
                        progress = 101
                    }
                }
                worker.execute()
                val thrown = shouldThrow<ExecutionException> { worker.get(10, TimeUnit.SECONDS) }
                (thrown.cause is IllegalArgumentException) shouldBe true
            }
        }
    }
}
