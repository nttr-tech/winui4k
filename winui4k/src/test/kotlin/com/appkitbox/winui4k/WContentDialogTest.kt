package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Tests the JDialog-style [WContentDialog]: reading back the title and others from the native side, [WContentDialog.showAndWait] blocking until closed
 * (the result for each button pressed with Esc or Enter, and when the owner window is closed), the order and canceling of the open/close and button listeners,
 * the exception for a second dialog. That the handlers do not capture the wrapper (GC) is in [WrapperReleaseDialogTest]. Keys are sent to the shared window with [TestKeyboard].
 */
class WContentDialogTest : FunSpec() {
    /** Events recorded on the UI thread and read from the test thread (guarded by a lock). */
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private suspend fun awaitEvents(vararg expected: String) {
        eventually(5.seconds) { events() shouldBe expected.toList() }
    }

    private fun dialog(): WContentDialog = WContentDialog("Confirm", WLabel("Continue?")).apply {
        primaryButtonText = "Yes"
        secondaryButtonText = "No"
        closeButtonText = "Cancel"
        defaultButton = ContentDialogButton.PRIMARY
    }

    /** Adds open/close and button listeners and records "name:value" in the order received. Does not capture the dialog itself. */
    private fun recordListeners(dialog: WContentDialog) {
        val events = events
        dialog.addDialogListener(
            object : WContentDialogListener {
                override fun dialogOpened(e: WContentDialogEvent) {
                    events += "opened:${e.result}"
                }

                override fun dialogClosing(e: WContentDialogEvent) {
                    events += "closing:${e.result}"
                }

                override fun dialogClosed(e: WContentDialogEvent) {
                    events += "closed:${e.result}"
                }
            },
        )
        dialog.addButtonClickListener { e -> events += "button:${e.button}" }
    }

    private fun isLoaded(dialog: WContentDialog): Boolean = OpenContentDialogs.isLoadedOnUi(dialog)

    /** Waits until [dialog] opens, becomes Loaded and can receive key input. */
    private suspend fun awaitOpen(dialog: WContentDialog) {
        eventually(5.seconds) { onUiThreadGet { isLoaded(dialog) }.shouldBeTrue() }
        Thread.sleep(OPEN_SETTLE_MILLIS) // Focus moves inside during the opening animation
    }

    /** Waits until [dialog] finishes closing (Unloaded), so that the next dialog can be opened in the same window. */
    private suspend fun awaitClosed(dialog: WContentDialog) {
        eventually(5.seconds) { onUiThreadGet { isLoaded(dialog) }.shouldBeFalse() }
    }

    /** On the UI thread, opens [dialog] in [owner]'s window, waits until it closes, and records the returned value as "returned:result". */
    private fun showAndWaitLater(dialog: WContentDialog, owner: WComponent) {
        WinUiUtilities.invokeLater { events += "returned:${dialog.showAndWait(owner)}" }
    }

    private fun pressEscape() = TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)

    /** Places an owner button in the shared window, runs [block], and removes it at the end. */
    private suspend fun withOwner(block: suspend (WButton) -> Unit) {
        val owner = onUiThreadGet { WButton("Owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(owner)
        }
    }

    init {
        beforeTest { events.clear() }

        test("title is read back from the native ContentDialog.Title and defaults to an empty string; isFullSizeDesired defaults to false and can be read and written") {
            onUiThread {
                WContentDialog().title shouldBe ""
                val dialog = WContentDialog("Confirm Save")
                dialog.title shouldBe "Confirm Save"
                dialog.title = "Confirm Overwrite"
                dialog.title shouldBe "Confirm Overwrite"
                dialog.isFullSizeDesired shouldBe false
                dialog.isFullSizeDesired = true
                dialog.isFullSizeDesired shouldBe true
            }
        }

        test("the JDialog(owner, title, modal)-style constructor has an owner and a title and is always modal; modal = false and isModal = false throw IllegalArgumentException") {
            onUiThread {
                val owner = WButton("Owner")
                val dialog = WContentDialog(owner, "Settings")
                dialog.owner shouldBe owner
                dialog.title shouldBe "Settings"
                dialog.isModal shouldBe true
                WContentDialog(null, "No owner", true).owner shouldBe null
                shouldThrow<IllegalArgumentException> { WContentDialog(owner, "Modeless", false) }
                shouldThrow<IllegalArgumentException> { dialog.isModal = false }
                dialog.isModal = true
                WContentDialog().owner shouldBe null
            }
        }

        test("isVisible before opening is false, and showAndWait() or isVisible = true without an owner throws IllegalStateException") {
            onUiThread {
                val dialog = dialog()
                dialog.isVisible shouldBe false
                shouldThrow<IllegalStateException> { dialog.showAndWait() }.message shouldContain "owner"
                shouldThrow<IllegalStateException> { dialog.isVisible = true }
                dialog.isVisible shouldBe false
            }
        }

        test("it cannot be opened for an owner not placed in a window (IllegalArgumentException), and showAndWait off the UI thread throws IllegalStateException") {
            val dialog = onUiThreadGet { dialog() }
            onUiThread { shouldThrow<IllegalArgumentException> { dialog.show(WButton("Not placed")) } }
            val owner = onUiThreadGet { WButton("Owner") }
            shouldThrow<IllegalStateException> { dialog.showAndWait(owner) }
            onUiThreadGet { dialog.isVisible } shouldBe false
        }

        test("showAndWait does not return until closed, and Esc presses the close button and returns NONE; the listeners are in the order open, button, before close, after close") {
            withOwner { owner ->
                val dialog = onUiThreadGet { dialog().also { recordListeners(it) } }
                showAndWaitLater(dialog, owner)
                awaitOpen(dialog)
                awaitEvents("opened:NONE")
                onUiThreadGet { dialog.isVisible to ModalLoop.depth } shouldBe (true to 1)
                pressEscape()
                awaitEvents("opened:NONE", "button:CLOSE", "closing:NONE", "closed:NONE", "returned:NONE")
                onUiThreadGet { dialog.isVisible } shouldBe false
                awaitClosed(dialog)
            }
        }

        test("Enter presses the default button, and PRIMARY and SECONDARY become the result of showAndWait as is; making the close button the default gives NONE") {
            withOwner { owner ->
                val dialog = onUiThreadGet { dialog() }
                val expected = mutableListOf<String>()
                for ((button, result) in listOf(
                    ContentDialogButton.PRIMARY to ContentDialogResult.PRIMARY,
                    ContentDialogButton.SECONDARY to ContentDialogResult.SECONDARY,
                    ContentDialogButton.CLOSE to ContentDialogResult.NONE,
                )) {
                    onUiThread { dialog.defaultButton = button }
                    showAndWaitLater(dialog, owner)
                    awaitOpen(dialog)
                    TestKeyboard.pressEnter()
                    expected += "returned:$result"
                    awaitEvents(*expected.toTypedArray())
                    awaitClosed(dialog)
                }
            }
        }

        test("setting isCancel to true in a button listener keeps it open, and the next listener receives that value; stopping the cancel closes it") {
            withOwner { owner ->
                val dialog = onUiThreadGet { dialog() }
                var veto = true
                onUiThread {
                    dialog.addButtonClickListener { e -> e.isCancel = veto }
                    dialog.addButtonClickListener { e -> events += "second:${e.button}:${e.isCancel}" }
                }
                showAndWaitLater(dialog, owner)
                awaitOpen(dialog)
                TestKeyboard.pressEnter()
                awaitEvents("second:PRIMARY:true")
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { dialog.isVisible } shouldBe true
                events() shouldBe listOf("second:PRIMARY:true")
                onUiThread { veto = false }
                TestKeyboard.pressEnter()
                awaitEvents("second:PRIMARY:true", "second:PRIMARY:false", "returned:PRIMARY")
                awaitClosed(dialog)
            }
        }

        test("setting isCancel to true in dialogClosing keeps even hide() from closing; removing the listener closes it, and dispose() gives the same NONE as hide()") {
            withOwner { owner ->
                val dialog = onUiThreadGet { dialog() }
                val vetoing = object : WContentDialogAdapter() {
                    override fun dialogClosing(e: WContentDialogEvent) {
                        events += "closing:${e.result}"
                        e.isCancel = true
                    }
                }
                onUiThread { dialog.addDialogListener(vetoing) }
                showAndWaitLater(dialog, owner)
                awaitOpen(dialog)
                onUiThread { dialog.hide() }
                awaitEvents("closing:NONE")
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { dialog.isVisible } shouldBe true
                onUiThread {
                    dialog.removeDialogListener(vetoing)
                    dialog.dispose()
                }
                awaitEvents("closing:NONE", "returned:NONE")
                awaitClosed(dialog)
            }
        }

        test("isVisible = true does not return until closed (same as a modal JDialog.setVisible), and isVisible = false closes it") {
            withOwner { owner ->
                val dialog = onUiThreadGet { WContentDialog(owner, "Settings").apply { closeButtonText = "Close" } }
                WinUiUtilities.invokeLater {
                    dialog.isVisible = true
                    events += "returned:${dialog.isVisible}"
                }
                awaitOpen(dialog)
                events() shouldBe emptyList()
                onUiThread { dialog.isVisible = false }
                awaitEvents("returned:false")
                awaitClosed(dialog)
            }
        }

        test("show opens without blocking and the result reaches the callback when closed; opening the same dialog or another dialog in the same window while it is open throws IllegalStateException") {
            withOwner { owner ->
                val first = onUiThreadGet { dialog() }
                val second = onUiThreadGet { dialog() }
                onUiThread {
                    first.show(owner) { events += "callback:$it" }
                    first.isVisible shouldBe true
                    shouldThrow<IllegalStateException> { first.show(owner) }
                    val error = shouldThrow<IllegalStateException> { second.show(owner) }
                    error.message shouldContain "only one"
                    shouldThrow<IllegalStateException> { second.showAndWait(owner) }
                    second.isVisible shouldBe false
                }
                awaitOpen(first)
                pressEscape()
                awaitEvents("callback:NONE")
                awaitClosed(first)
            }
        }

        test("showAndWait from inside an addSizeChangedListener listener throws IllegalStateException without opening the dialog") {
            val panel = onUiThreadGet { WPanel() }
            val dialog = onUiThreadGet { dialog() }
            onUiThread {
                panel.addSizeChangedListener {
                    if (events().isEmpty()) {
                        events += "sizeChanged:" + runCatching { dialog.showAndWait(panel) }.exceptionOrNull()?.javaClass?.simpleName
                    }
                }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                awaitEvents("sizeChanged:IllegalStateException")
                onUiThreadGet { dialog.isVisible } shouldBe false
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("closing the owner window while it is open makes showAndWait return NONE and isVisible false") {
            val frame = onUiThreadGet { WFrame(OWNER_TITLE) }
            val owner = onUiThreadGet { WButton("Open").also { frame.add(it) } }
            val loaded = CountDownLatch(1)
            onUiThread {
                owner.addLoadedListener { loaded.countDown() }
                frame.isVisible = true
            }
            loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
            val dialog = onUiThreadGet { dialog() }
            showAndWaitLater(dialog, owner)
            awaitOpen(dialog)
            TopLevelWindows.close(TopLevelWindows.find(null, OWNER_TITLE).shouldNotBeNull())
            awaitEvents("returned:NONE")
            onUiThreadGet { dialog.isVisible } shouldBe false
        }

        context("Threads and reentrancy") {
            test("replacing the owner window's content while open does not make showAndWait return, and pressing a button afterwards returns with that button's result") {
                val frame = onUiThreadGet { WFrame(OWNER_TITLE) }
                try {
                    val owner = onUiThreadGet { WButton("Open").also { frame.add(it) } }
                    val loaded = CountDownLatch(1)
                    onUiThread {
                        owner.addLoadedListener { loaded.countDown() }
                        frame.isVisible = true
                    }
                    loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
                    val dialog = onUiThreadGet { dialog() }
                    showAndWaitLater(dialog, owner)
                    awaitOpen(dialog)
                    val replacement = onUiThreadGet { WButton("New content") }
                    val replaced = CountDownLatch(1)
                    onUiThread {
                        replacement.addLoadedListener { replaced.countDown() }
                        frame.setContentPane(replacement)
                    }
                    replaced.await(5, TimeUnit.SECONDS).shouldBeTrue()
                    Thread.sleep(SETTLE_MILLIS)
                    events() shouldBe emptyList()
                    onUiThreadGet { dialog.isVisible } shouldBe true
                    OpenContentDialogs.press(replacement, "PrimaryButton")
                    awaitEvents("returned:PRIMARY")
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val OPEN_SETTLE_MILLIS = 600L
        const val SETTLE_MILLIS = 500L
        const val OWNER_TITLE = "winui4k dialog owner"
    }
}
