package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the window events, ways of closing, visibility and content replacement of the JFrame-like [WFrame].
 * Each test creates its own WFrame and disposes it at the end, and the shared "winui4k tests" window is not closed
 * (the app exits when the last window closes, so the shared window is opened first).
 * The user's close operation (the title bar ×) is reproduced with WM_CLOSE.
 */
@Tags("Integration")
class WFrameTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    /** A listener that records window events by names such as "opened" (state changes as "state:before->after"). */
    private val recorder = object : WWindowAdapter() {
        override fun windowOpened(e: WWindowEvent) = record("opened", e, WWindowEvent.WINDOW_OPENED)

        override fun windowClosing(e: WWindowEvent) = record("closing", e, WWindowEvent.WINDOW_CLOSING)

        override fun windowClosed(e: WWindowEvent) = record("closed", e, WWindowEvent.WINDOW_CLOSED)

        override fun windowIconified(e: WWindowEvent) = record("iconified", e, WWindowEvent.WINDOW_ICONIFIED)

        override fun windowDeiconified(e: WWindowEvent) = record("deiconified", e, WWindowEvent.WINDOW_DEICONIFIED)

        override fun windowStateChanged(e: WWindowEvent) = record("state:${e.oldState}->${e.newState}", e, WWindowEvent.WINDOW_STATE_CHANGED)

        private fun record(name: String, e: WWindowEvent, id: Int) {
            e.id shouldBe id
            e.window shouldBeSameInstanceAs e.source
            events += name
        }
    }

    private var frame: WFrame? = null

    /** Creates its own window and adds [recorder] (does not show it). */
    private fun newFrame(): WFrame = onUiThreadGet {
        WFrame(TITLE).also {
            it.addWindowListener(recorder)
            it.addWindowStateListener(recorder)
            frame = it
        }
    }

    /** Shows it and waits until windowOpened. */
    private suspend fun showAndAwaitOpened(frame: WFrame) {
        onUiThread { frame.isVisible = true }
        eventually(5.seconds) { events() shouldContain "opened" }
    }

    private fun userClose() = TopLevelWindows.close(TopLevelWindows.find(null, TITLE).shouldNotBeNull())

    init {
        beforeSpec { UiTestHarness.sharedFrame() } // So that closing its own window does not exit the app
        beforeTest { events.clear() }
        afterTest {
            frame?.let { onUiThread { it.dispose() } }
            frame = null
        }

        test("the constants have the same values as AWT's WindowEvent, WindowConstants and Frame") {
            listOf(
                WWindowEvent.WINDOW_FIRST, WWindowEvent.WINDOW_OPENED, WWindowEvent.WINDOW_CLOSING, WWindowEvent.WINDOW_CLOSED,
                WWindowEvent.WINDOW_ICONIFIED, WWindowEvent.WINDOW_DEICONIFIED, WWindowEvent.WINDOW_ACTIVATED,
                WWindowEvent.WINDOW_DEACTIVATED, WWindowEvent.WINDOW_GAINED_FOCUS, WWindowEvent.WINDOW_LOST_FOCUS,
                WWindowEvent.WINDOW_STATE_CHANGED, WWindowEvent.WINDOW_LAST,
            ) shouldBe listOf(200, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 209)
            listOf(
                WWindowConstants.DO_NOTHING_ON_CLOSE,
                WWindowConstants.HIDE_ON_CLOSE,
                WWindowConstants.DISPOSE_ON_CLOSE,
                WWindowConstants.EXIT_ON_CLOSE,
            ) shouldBe listOf(0, 1, 2, 3)
            listOf(WFrame.DO_NOTHING_ON_CLOSE, WFrame.HIDE_ON_CLOSE, WFrame.DISPOSE_ON_CLOSE, WFrame.EXIT_ON_CLOSE) shouldBe listOf(0, 1, 2, 3)
            listOf(WFrame.NORMAL, WFrame.ICONIFIED, WFrame.MAXIMIZED_HORIZ, WFrame.MAXIMIZED_VERT, WFrame.MAXIMIZED_BOTH) shouldBe
                listOf(0, 1, 2, 4, 6)
        }

        test("defaultCloseOperation defaults to DISPOSE_ON_CLOSE. An out-of-range value throws IllegalArgumentException and the value does not change") {
            val frame = newFrame()
            onUiThread {
                frame.defaultCloseOperation shouldBe WFrame.DISPOSE_ON_CLOSE
                frame.defaultCloseOperation = WFrame.DO_NOTHING_ON_CLOSE
                frame.defaultCloseOperation shouldBe WFrame.DO_NOTHING_ON_CLOSE
                shouldThrow<IllegalArgumentException> { frame.defaultCloseOperation = 4 }
                shouldThrow<IllegalArgumentException> { frame.defaultCloseOperation = -1 }
                frame.defaultCloseOperation shouldBe WFrame.DO_NOTHING_ON_CLOSE
            }
        }

        test("isVisible reads the native visibility. false hides without closing, and true shows it again") {
            val frame = newFrame()
            onUiThread { frame.isVisible.shouldBeFalse() }
            showAndAwaitOpened(frame)
            onUiThread {
                frame.isVisible.shouldBeTrue()
                frame.isVisible = false
                frame.isVisible.shouldBeFalse()
                frame.isDisplayable.shouldBeTrue()
                WFrame.frames.toList() shouldContain frame
            }
            TopLevelWindows.find(null, TITLE).shouldBeNull()
            onUiThread {
                frame.isVisible = true
                frame.isVisible.shouldBeTrue()
            }
            TopLevelWindows.find(null, TITLE).shouldNotBeNull()
            Thread.sleep(SETTLE_MILLIS)
            events() shouldBe listOf("opened") // windowOpened does not come on the second show
        }

        test("dispose closes it and removes it from frames. windowClosing does not come but windowClosed does. The second call does nothing. Showing again throws IllegalStateException") {
            val frame = newFrame()
            showAndAwaitOpened(frame)
            onUiThread {
                WFrame.frames.toList() shouldContain frame
                frame.dispose()
                frame.isDisplayable.shouldBeFalse()
                frame.isVisible.shouldBeFalse()
                WFrame.frames.toList() shouldNotContain frame
                frame.dispose()
                frame.isVisible = false // Hiding after closing does nothing
                shouldThrow<IllegalStateException> { frame.isVisible = true }
            }
            eventually(5.seconds) { events() shouldBe listOf("opened", "closed") }
            TopLevelWindows.find(null, TITLE).shouldBeNull()
        }

        test("when the user closes it, it closes according to DISPOSE_ON_CLOSE (the default) after windowClosing, and windowClosed comes") {
            val frame = newFrame()
            showAndAwaitOpened(frame)
            userClose()
            eventually(5.seconds) { events() shouldBe listOf("opened", "closing", "closed") }
            onUiThread {
                frame.isDisplayable.shouldBeFalse()
                WFrame.frames.toList() shouldNotContain frame
            }
        }

        test("with DO_NOTHING_ON_CLOSE, the user closing it only fires windowClosing and the window stays open") {
            val frame = newFrame()
            onUiThread { frame.defaultCloseOperation = WFrame.DO_NOTHING_ON_CLOSE }
            showAndAwaitOpened(frame)
            userClose()
            eventually(5.seconds) { events() shouldBe listOf("opened", "closing") }
            Thread.sleep(SETTLE_MILLIS)
            events() shouldBe listOf("opened", "closing")
            onUiThread {
                frame.isVisible.shouldBeTrue()
                frame.isDisplayable.shouldBeTrue()
            }
        }

        test("with HIDE_ON_CLOSE, the user closing it only hides it, and it stays open in frames") {
            val frame = newFrame()
            onUiThread { frame.defaultCloseOperation = WFrame.HIDE_ON_CLOSE }
            showAndAwaitOpened(frame)
            userClose()
            eventually(5.seconds) { onUiThreadGet { frame.isVisible }.shouldBeFalse() }
            Thread.sleep(SETTLE_MILLIS)
            events() shouldBe listOf("opened", "closing")
            onUiThread {
                frame.isDisplayable.shouldBeTrue()
                WFrame.frames.toList() shouldContain frame
            }
        }

        test("calling dispose inside windowClosing closes it as is (even with DO_NOTHING_ON_CLOSE)") {
            val frame = newFrame()
            onUiThread {
                frame.defaultCloseOperation = WFrame.DO_NOTHING_ON_CLOSE
                frame.addWindowListener(
                    object : WWindowAdapter() {
                        override fun windowClosing(e: WWindowEvent) = e.window.dispose()
                    },
                )
            }
            showAndAwaitOpened(frame)
            userClose()
            eventually(5.seconds) { events() shouldBe listOf("opened", "closing", "closed") }
            onUiThread { frame.isDisplayable.shouldBeFalse() }
        }

        test("iconifying, restoring and maximizing fire windowIconified / windowDeiconified and windowStateChanged (with Frame state values)") {
            val frame = newFrame()
            val presenter = onUiThreadGet { WOverlappedPresenter.create().also { frame.appWindow.setPresenter(it) } }
            showAndAwaitOpened(frame)
            events.clear()
            onUiThread { presenter.minimize() }
            eventually(5.seconds) { events() shouldBe listOf("iconified", "state:0->1") }
            onUiThread { presenter.restore() }
            eventually(5.seconds) { events() shouldBe listOf("iconified", "state:0->1", "deiconified", "state:1->0") }
            events.clear()
            onUiThread { presenter.maximize() }
            eventually(5.seconds) { events() shouldBe listOf("state:0->6") }
        }

        test("listeners are called in the order added, removed listeners are not called, and getWindowListeners etc. return them in the order added") {
            val frame = newFrame()
            val second = object : WWindowAdapter() {
                override fun windowOpened(e: WWindowEvent) {
                    events += "second"
                }
            }
            val removed = object : WWindowAdapter() {
                override fun windowOpened(e: WWindowEvent) {
                    events += "removed"
                }
            }
            onUiThread {
                frame.addWindowListener(second)
                frame.addWindowListener(removed)
                frame.addWindowListener(null)
                frame.removeWindowListener(removed)
                frame.windowListeners.toList() shouldBe listOf(recorder, second)
                frame.windowStateListeners.toList() shouldBe listOf(recorder)
                frame.addWindowFocusListener(recorder)
                frame.windowFocusListeners.toList() shouldBe listOf(recorder)
                frame.removeWindowFocusListener(recorder)
                frame.windowFocusListeners.toList() shouldBe emptyList()
            }
            showAndAwaitOpened(frame)
            eventually(5.seconds) { events() shouldBe listOf("opened", "second") }
        }

        test("a window shown and brought to the front becomes active, firing windowActivated → windowGainedFocus in that order") {
            val frame = newFrame()
            val order = Collections.synchronizedList(mutableListOf<String>())
            onUiThread {
                frame.addWindowListener(
                    object : WWindowAdapter() {
                        override fun windowActivated(e: WWindowEvent) {
                            order += "activated"
                        }
                    },
                )
                frame.addWindowFocusListener(
                    object : WWindowAdapter() {
                        override fun windowGainedFocus(e: WWindowEvent) {
                            order += "gained"
                        }
                    },
                )
            }
            showAndAwaitOpened(frame)
            eventually(5.seconds) { synchronized(order) { order.toList() } shouldBe listOf("activated", "gained") }
            onUiThread {
                frame.isActive.shouldBeTrue()
                frame.isFocused.shouldBeTrue()
            }
        }

        test("a shown WFrame is held by frames until closed even if the caller drops the reference, so it is not removed by GC") {
            val weak = onUiThreadGet { WeakReference(WFrame(TITLE).also { it.isVisible = true }) }
            Reclamation.survivesGc(weak).shouldBeTrue()
            val kept = weak.get().shouldNotBeNull()
            onUiThread { WFrame.frames.toList() shouldContain kept }
            onUiThread { kept.dispose() }
        }

        test("a WFrame never shown is not in frames and is collected by GC once the reference is dropped (the same as before it becomes displayable in Swing)") {
            val weak = onUiThreadGet {
                val neverShown = WFrame(TITLE)
                WFrame.frames.toList() shouldNotContain neverShown
                WeakReference(neverShown)
            }
            Reclamation.awaitCollected(weak).shouldBeTrue()
        }

        test("the number of open windows (used to decide the exit on the last window) increases only once when first shown and goes back when closed. Disposing without showing does not change it") {
            val before = onUiThreadGet { WinUiUtilities.openWindowCount }
            val frame = newFrame()
            onUiThreadGet { WinUiUtilities.openWindowCount } shouldBe before
            showAndAwaitOpened(frame)
            onUiThread {
                WinUiUtilities.openWindowCount shouldBe before + 1
                frame.isVisible = false
                frame.isVisible = true
                WinUiUtilities.openWindowCount shouldBe before + 1
                frame.dispose()
            }
            eventually(5.seconds) { onUiThreadGet { WinUiUtilities.openWindowCount } shouldBe before }
            onUiThread {
                val neverShown = WFrame(TITLE)
                WinUiUtilities.openWindowCount shouldBe before
                neverShown.dispose()
                neverShown.isDisplayable.shouldBeFalse()
            }
            Thread.sleep(SETTLE_MILLIS) // The count must not decrease even when the closed notification (Window.Closed) comes
            onUiThreadGet { WinUiUtilities.openWindowCount } shouldBe before
        }

        test("setContentPane removes a component that is in another container from there and makes it the content (as in Swing)") {
            val frame = newFrame()
            val (holder, pane) = onUiThreadGet {
                val holder = WPanel()
                val pane = WPanel()
                holder.add(pane)
                frame.setContentPane(pane)
                holder to pane
            }
            onUiThread {
                holder.componentCount shouldBe 0
                pane.parent.shouldBeNull()
                frame.add(WButton("Inner"))
                pane.componentCount shouldBe 1
            }
            showAndAwaitOpened(frame)
            eventually(5.seconds) { onUiThreadGet { pane.actualWidth } shouldBeGreaterThan 0.0 }
        }

        test("add goes into the content container replaced with setContentPane. add with a constraint passes it to that layout manager") {
            val frame = newFrame()
            onUiThread {
                val root = WLayoutPanel(WBorderLayout())
                frame.setContentPane(root)
                val north = WButton("North")
                val center = WButton("Center")
                frame.add(north, WBorderLayout.NORTH)
                frame.add(center)
                north.parent shouldBeSameInstanceAs root
                center.parent shouldBeSameInstanceAs root
                (root.layout as WBorderLayout).getLayoutComponent(WBorderLayout.NORTH) shouldBeSameInstanceAs north
                frame.contentPane shouldBeSameInstanceAs root // getContentPane is also the replaced content (as in Swing)
                val first = WButton("First")
                frame.add(first, 0)
                root.getComponent(0) shouldBeSameInstanceAs first
                frame.remove(first)
                first.parent.shouldBeNull()
            }
        }

        test("add after the content is replaced with a non-container component throws IllegalStateException") {
            val frame = newFrame()
            onUiThread {
                frame.setContentPane(WButton("Content"))
                shouldThrow<IllegalStateException> { frame.add(WButton("x")) }
                shouldThrow<IllegalStateException> { frame.add(WButton("x"), WBorderLayout.NORTH) }
            }
        }

        context("Reading and writing") {
            test("title shows the constructor value and values set later as the window title (caption)") {
                val frame = newFrame()
                showAndAwaitOpened(frame)
                onUiThread {
                    frame.title shouldBe TITLE
                    TopLevelWindows.find(null, TITLE) shouldBe WindowNatives.hwnd(frame)
                    frame.title = "$TITLE (edited)"
                    frame.title shouldBe "$TITLE (edited)"
                    TopLevelWindows.find(null, "$TITLE (edited)") shouldBe WindowNatives.hwnd(frame)
                }
            }
        }

        context("Swing compatibility") {
            test("isShowing, like isVisible, is false before it is shown, after it is hidden and after it is closed") {
                val frame = newFrame()
                onUiThreadGet { frame.isShowing }.shouldBeFalse()
                showAndAwaitOpened(frame)
                onUiThread {
                    frame.isShowing.shouldBeTrue()
                    frame.isVisible = false
                    frame.isShowing.shouldBeFalse()
                    frame.isVisible = true
                    frame.isShowing.shouldBeTrue()
                    frame.dispose()
                    frame.isShowing.shouldBeFalse()
                }
            }

            test("add(c, constraints, index) inserts at index of the current content, passing the constraint to the layout manager. An out-of-range index throws IllegalArgumentException") {
                val frame = newFrame()
                onUiThread {
                    val root = WLayoutPanel(WBorderLayout())
                    frame.setContentPane(root)
                    val center = WButton("Center")
                    frame.add(center)
                    val north = WButton("North")
                    frame.add(north, WBorderLayout.NORTH, 0)
                    root.getComponent(0) shouldBeSameInstanceAs north
                    root.getComponent(1) shouldBeSameInstanceAs center
                    (root.layout as WBorderLayout).getLayoutComponent(WBorderLayout.NORTH) shouldBeSameInstanceAs north
                    shouldThrow<IllegalArgumentException> { frame.add(WButton("x"), WBorderLayout.SOUTH, 5) }
                    root.componentCount shouldBe 2
                }
            }

            test("revalidate and validate of the frame relayout the content, and the positions of added children are decided when validate returns. repaint can be called") {
                val frame = newFrame()
                val root = onUiThreadGet {
                    WLayoutPanel(WBorderLayout()).also {
                        frame.setContentPane(it)
                        frame.setSize(400, 300)
                    }
                }
                showAndAwaitOpened(frame)
                eventually(5.seconds) { onUiThreadGet { root.actualWidth } shouldBeGreaterThan 0.0 }
                onUiThread {
                    val north = WButton("North")
                    frame.add(north, WBorderLayout.NORTH)
                    frame.revalidate()
                    frame.validate()
                    frame.repaint()
                    north.bounds.x shouldBe 0
                    north.bounds.y shouldBe 0
                    north.bounds.width shouldBe root.bounds.width
                    north.actualHeight shouldBeGreaterThan 0.0
                }
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame test"
        const val SETTLE_MILLIS = 500L
    }
}
