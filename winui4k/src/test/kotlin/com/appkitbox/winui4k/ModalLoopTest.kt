package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Verification of the nested message loop ([ModalLoop]) ("Nested message loop" in doc/verification.md).
 * Runs Swing's shape of blocking the caller until a dialog is opened and closed (JOptionPane.showConfirmDialog and so on) from a button click,
 * a menu item, a keyboard accelerator and invokeLater, and checks that key input, invokeLater and GC-driven releases keep working while blocked,
 * and that closing the dialog with the keyboard returns. Keys are sent to the shared window with [TestKeyboard].
 */
@Tags("Integration")
class ModalLoopTest : FunSpec() {
    /** Events recorded on the UI thread and read from the test thread (guarded by a lock). */
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    /** Opens a dialog in [owner]'s window, waits in the nested loop until it closes, and records "result@depth" (the prototype of blocking). */
    private fun showAndWait(dialog: WContentDialog, owner: WComponent, name: String) {
        var result: ContentDialogResult? = null
        dialog.show(owner) { result = it }
        val depth = ModalLoop.depth + 1
        val returned = ModalLoop.run { result != null }
        events += "$name:${if (returned) result else "quit"}@$depth"
    }

    private fun dialog(): WContentDialog = WContentDialog("Confirm", WLabel("Continue?")).apply {
        primaryButtonText = "OK"
        closeButtonText = "Cancel"
        defaultButton = ContentDialogButton.PRIMARY
    }

    /** Waits until [dialog] opens, becomes Loaded and can receive key input. */
    private suspend fun awaitOpen(dialog: WContentDialog) {
        eventually(5.seconds) {
            onUiThreadGet { dialog.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
        }
        Thread.sleep(OPEN_SETTLE_MILLIS) // Focus moves inside during the opening animation
    }

    private suspend fun awaitEvents(vararg expected: String) {
        eventually(5.seconds) { events() shouldBe expected.toList() }
    }

    private fun pressEscape() = TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)

    init {
        beforeTest { events.clear() }

        test("while a loop run inside invokeLater is running, invokeLater from other threads runs, and it returns true when the condition becomes true") {
            var done = false
            WinUiUtilities.invokeLater {
                events += "start@${ModalLoop.depth}"
                events += "returned:${ModalLoop.run { done }}@${ModalLoop.depth}"
            }
            eventually(5.seconds) { events() shouldBe listOf("start@0") }
            onUiThreadGet { ModalLoop.depth } shouldBe 1 // The test thread's onUiThread ran inside the loop
            onUiThread { done = true }
            awaitEvents("start@0", "returned:true@0")
        }

        test("if the condition is true from the start it returns true without dispatching messages; IllegalStateException off the UI thread") {
            onUiThreadGet { ModalLoop.run { true } to ModalLoop.depth } shouldBe (true to 0)
            shouldThrow<IllegalStateException> { ModalLoop.run { true } }.message shouldContain "UI thread"
        }

        test("native objects of wrappers collected by GC are released on the UI thread during the loop too (ReleasePump)") {
            var done = false
            WinUiUtilities.invokeLater { ModalLoop.run { done } }
            try {
                eventually(5.seconds) { onUiThreadGet { ModalLoop.depth } shouldBe 1 }
                val probe = onUiThreadGet { NativeProbe(WButton("Collect").inspectable) }
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
                onUiThreadGet { ModalLoop.depth } shouldBe 1
            } finally {
                onUiThread { done = true }
            }
            eventually(5.seconds) { onUiThreadGet { ModalLoop.depth } shouldBe 0 }
        }

        test("opening a dialog inside a button's Click and waiting, closing it with Esc continues Click with NONE; Enter gives the default button (PRIMARY)") {
            val button = onUiThreadGet { WButton("Open") }
            val dialog = onUiThreadGet { dialog() }
            onUiThread { button.addActionListener { showAndWait(dialog, button, "click") } }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { check(button.requestFocusInWindow()) }
                TestKeyboard.pressEnter()
                awaitOpen(dialog)
                onUiThreadGet { ModalLoop.depth } shouldBe 1
                pressEscape()
                awaitEvents("click:NONE@1")

                onUiThread { check(button.requestFocusInWindow()) }
                TestKeyboard.pressEnter()
                awaitOpen(dialog)
                TestKeyboard.pressEnter()
                awaitEvents("click:NONE@1", "click:PRIMARY@1")
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("opening a dialog inside a menu item's Click and waiting returns when it is closed with Esc") {
            val button = onUiThreadGet { WButton("Menu") }
            val item = onUiThreadGet { WMenuFlyoutItem("Open...") }
            val menu = onUiThreadGet { WMenuFlyout().also { it.add(item) } }
            val dialog = onUiThreadGet { dialog() }
            onUiThread { item.addActionListener { showAndWait(dialog, button, "menu") } }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { menu.showAt(button) }
                eventually(5.seconds) {
                    onUiThreadGet { item.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
                }
                Thread.sleep(OPEN_SETTLE_MILLIS)
                onUiThread { check(item.requestFocusInWindow()) }
                TestKeyboard.pressEnter()
                awaitOpen(dialog)
                pressEscape()
                awaitEvents("menu:NONE@1")
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("opening a dialog inside a keyboard accelerator's (F5) Invoked and waiting returns when it is closed with Esc") {
            val button = onUiThreadGet { WButton("Refresh") }
            val dialog = onUiThreadGet { dialog() }
            onUiThread {
                button.addKeyboardAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0)) { showAndWait(dialog, button, "F5") }
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { check(button.requestFocusInWindow()) }
                TestKeyboard.press(VK_F5, SCAN_F5)
                awaitOpen(dialog)
                pressEscape()
                awaitEvents("F5:NONE@1")
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("nested (depth 2): waiting again inside a dialog wait, the outer one does not return until the inner one does even if the outer condition holds first") {
            val button = onUiThreadGet { WButton("Open") }
            val dialog = onUiThreadGet { dialog() }
            var innerDone = false
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                WinUiUtilities.invokeLater { showAndWait(dialog, button, "outer") }
                awaitOpen(dialog)
                WinUiUtilities.invokeLater {
                    val depth = ModalLoop.depth + 1
                    events += "inner:${ModalLoop.run { innerDone }}@$depth"
                }
                eventually(5.seconds) { onUiThreadGet { ModalLoop.depth } shouldBe 2 }
                pressEscape() // The outer dialog closes (the outer condition holds)
                eventually(5.seconds) {
                    onUiThreadGet { dialog.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } shouldBe false
                }
                Thread.sleep(SETTLE_MILLIS)
                events() shouldBe emptyList()
                onUiThreadGet { ModalLoop.depth } shouldBe 2
                onUiThread { innerDone = true }
                awaitEvents("inner:true@2", "outer:NONE@1")
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("nested (depth 2): waiting for a file-open picker inside a dialog wait, closing the OS dialog returns the inner and then the outer") {
            val button = onUiThreadGet { WButton("Open") }
            val dialog = onUiThreadGet { dialog() }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                WinUiUtilities.invokeLater { showAndWait(dialog, button, "outer") }
                awaitOpen(dialog)
                WinUiUtilities.invokeLater {
                    var picked: String? = null
                    var done = false
                    val picker = WFileOpenPicker(UiTestHarness.sharedFrame())
                    picker.pickSingleFile { path ->
                        picked = path
                        done = true
                    }
                    val depth = ModalLoop.depth + 1
                    val returned = ModalLoop.run { done }
                    events += "picker:$returned:$picked@$depth"
                }
                val fileDialog = eventually(10.seconds) { TopLevelWindows.find(TopLevelWindows.DIALOG_CLASS, null).shouldNotBeNull() }
                onUiThreadGet { ModalLoop.depth } shouldBe 2 // During the OS dialog, the UI thread processes invokeLater inside the loop too
                TopLevelWindows.close(fileDialog)
                awaitEvents("picker:true:null@2")
                Thread.sleep(OPEN_SETTLE_MILLIS) // Until the OS dialog closes and the window accepts input again
                pressEscape()
                awaitEvents("picker:true:null@2", "outer:NONE@1")
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("investigation: closing the owner window while a dialog is open delivers no dialog Closed, and the owner element's IsLoaded becomes false") {
            val frame = onUiThreadGet { WFrame(OWNER_TITLE) }
            val button = onUiThreadGet { WButton("Open").also { frame.add(it) } }
            val loaded = CountDownLatch(1)
            onUiThread {
                button.addLoadedListener { loaded.countDown() }
                frame.isVisible = true
            }
            loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
            val dialog = onUiThreadGet { dialog() }
            var windowCloseSent = false
            var timedOut = false
            WinUiUtilities.invokeLater {
                var result: ContentDialogResult? = null
                dialog.show(button) { result = it }
                val timer = Dispatcher.schedule(OWNER_CLOSE_TIMEOUT_MILLIS) { timedOut = true }
                val ownerLoaded = { button.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }
                ModalLoop.run { result != null || timedOut || (windowCloseSent && !ownerLoaded()) }
                timer.close()
                events += "result=$result ownerLoaded=${ownerLoaded()} timedOut=$timedOut"
            }
            awaitOpen(dialog)
            val window = TopLevelWindows.find(null, OWNER_TITLE).shouldNotBeNull()
            onUiThread { windowCloseSent = true }
            TopLevelWindows.close(window)
            eventually((OWNER_CLOSE_TIMEOUT_MILLIS / 1000 + 5).seconds) { events().size shouldBe 1 }
            events() shouldBe listOf("result=null ownerLoaded=false timedOut=false")
            TopLevelWindows.find(null, OWNER_TITLE) shouldBe null
        }

        test("investigation: running the loop inside SizeChanged stops XAML layout during the loop (an element placed later does not become Loaded)") {
            probeLayoutFreeze(XamlInterop.IFrameworkElement_add_SizeChanged, XamlInterop.IID_SizeChangedEventHandler, "sizeChanged")
        }

        test("investigation: running the loop inside Loaded stops XAML layout during the loop (an element placed later does not become Loaded)") {
            probeLayoutFreeze(XamlInterop.IFrameworkElement_add_Loaded, XamlInterop.IID_RoutedEventHandler, "loaded")
        }

        test("running it from inside addSizeChangedListener and addLoadedListener listeners throws IllegalStateException") {
            val panel = onUiThreadGet { WPanel() }
            onUiThread {
                panel.addSizeChangedListener {
                    events += "sizeChanged:" + runCatching { ModalLoop.run { true } }.exceptionOrNull()?.javaClass?.simpleName
                }
                panel.addLoadedListener {
                    events += "loaded:" + runCatching { ModalLoop.run { true } }.exceptionOrNull()?.javaClass?.simpleName
                }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                eventually(5.seconds) {
                    events().toSet() shouldBe setOf("sizeChanged:IllegalStateException", "loaded:IllegalStateException")
                }
                onUiThreadGet { ModalLoop.run { true } } shouldBe true // Usable outside the listeners
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("running it from inside WComponentListener's componentResized (SizeChanged) and componentMoved (LayoutUpdated) throws IllegalStateException") {
            val panel = onUiThreadGet { WPanel() }
            val box = onUiThreadGet { WLabel("box") }
            onUiThread {
                panel.add(box)
                box.addComponentListener(
                    object : WComponentAdapter() {
                        override fun componentResized(e: WComponentEvent) {
                            events += "resized:" + runCatching { ModalLoop.run { true } }.exceptionOrNull()?.javaClass?.simpleName
                        }

                        override fun componentMoved(e: WComponentEvent) {
                            events += "moved:" + runCatching { ModalLoop.run { true } }.exceptionOrNull()?.javaClass?.simpleName
                        }
                    },
                )
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                eventually(5.seconds) { events() shouldContain "resized:IllegalStateException" }
                // Inserting a child before it moves the box down, and componentMoved arrives with the LayoutUpdated after the layout
                onUiThread { panel.add(WLabel("before"), 0) }
                eventually(5.seconds) {
                    events().toSet() shouldBe setOf("resized:IllegalStateException", "moved:IllegalStateException")
                }
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }

    /**
     * Subscribes to the [addSlot] event (SizeChanged / Loaded) directly without going through W*, and runs the nested loop inside it. During the loop, places another
     * element and records whether it becomes Loaded (whether layout progresses), waiting up to [FREEZE_PROBE_MILLIS].
     */
    private suspend fun probeLayoutFreeze(addSlot: Int, handlerIid: String, name: String) {
        val panel = onUiThreadGet { WPanel() }
        val late = onUiThreadGet { WButton("Later") }
        var lateLoaded = false
        var timedOut = false
        var entered = false
        onUiThread {
            late.addLoadedListener { lateLoaded = true }
            panel.frameworkElement.addEventHandler("WinUI4K.Test.ModalLoopProbe", handlerIid, addSlot) { _, _ ->
                if (!entered) {
                    entered = true
                    Dispatcher.invokeLater { panel.add(late) }
                    val timer = Dispatcher.schedule(FREEZE_PROBE_MILLIS) { timedOut = true }
                    ModalLoop.run { lateLoaded || timedOut }
                    timer.close()
                    events += "$name:lateLoadedInLoop=$lateLoaded"
                }
            }
        }
        UiTestHarness.attach(panel)
        try {
            eventually(10.seconds) { events().size shouldBe 1 }
            events() shouldBe listOf("$name:lateLoadedInLoop=false")
            eventually(5.seconds) { onUiThreadGet { lateLoaded }.shouldBeTrue() } // Layout progresses once the loop is left
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val VK_F5 = 0x74
        const val SCAN_F5 = 0x3F
        const val OPEN_SETTLE_MILLIS = 600L
        const val SETTLE_MILLIS = 500L
        const val FREEZE_PROBE_MILLIS = 2_000L
        const val OWNER_CLOSE_TIMEOUT_MILLIS = 5_000L
        const val OWNER_TITLE = "winui4k modal owner"
    }
}
