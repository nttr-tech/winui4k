package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that a [WPopup], [WContentDialog] (including the callback form of [WOptionPane]) and [WFlyout] whose owner window ([WFrame]) was closed while open
 * release their while-open state even without a closed notification (Closed), and that the wrappers and natives are collected once the references are released
 * ([Reclamation]). Also that a [WFlyout] with a PopupMenuListener capturing its own flyout is collected.
 * The shared "winui4k tests" window is kept open (the app exits when the last window closes).
 */
@Tags("Memory")
class WrapperReleaseWindowCloseTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    /** No listener records the source (recording it would keep the wrapper from being collected). */
    private fun recorder(): WPopupMenuListener {
        val events = events
        return object : WPopupMenuAdapter() {
            override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                events += "visible"
            }

            override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                events += "invisible"
            }
        }
    }

    /** Shows another window and waits until the owner button put on it becomes Loaded. */
    private fun openFrame(): Pair<WFrame, WButton> {
        val frame = onUiThreadGet { WFrame(TITLE) }
        val owner = onUiThreadGet { WButton("Owner").also { frame.add(it) } }
        val loaded = CountDownLatch(1)
        onUiThread {
            owner.addLoadedListener { loaded.countDown() }
            frame.isVisible = true
        }
        loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
        return frame to owner
    }

    /** A mark that does not reference the content, to check what the callback captures. */
    private class Marker

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        beforeTest { events.clear() }

        test("a WPopup whose owner window was closed while open counts as closed, and is collected and its native destroyed once the reference is released") {
            val (frame, owner) = openFrame()
            val (reference, probe) = onUiThreadGet {
                val popup = WPopup(WLabel("Content"))
                popup.show(owner, 5.0, 5.0)
                WeakReference(popup) to NativeProbe(popup.inspectable)
            }
            eventually(5.seconds) { onUiThreadGet { reference.get()?.isOpen } shouldBe true }
            onUiThread { frame.dispose() }
            eventually(5.seconds) { onUiThreadGet { reference.get()?.isOpen } shouldBe false }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WContentDialog whose owner window was closed while open is collected together with its show callback, and its native destroyed") {
            val (frame, owner) = openFrame()
            val (reference, marker, probe) = onUiThreadGet {
                val dialog = WContentDialog("Confirm", WLabel("Continue?")).apply { closeButtonText = "Close" }
                val marker = Marker()
                dialog.show(owner) { events += "callback:$marker" }
                Triple(WeakReference(dialog), WeakReference(marker), NativeProbe(dialog.inspectable))
            }
            eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeTrue() }
            onUiThread { frame.dispose() }
            onUiThreadGet { reference.get()?.isVisible } shouldBe false
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitCollected(marker).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
            events().shouldContainExactly(emptyList())
        }

        test("a dialog of WOptionPane's callback form whose owner window was closed with × while open is collected together with the callback, and its native destroyed") {
            val (_, owner) = openFrame()
            val marker = onUiThreadGet {
                val marker = Marker()
                WOptionPane.showConfirmDialog(owner, "Save?", "Confirm", WOptionPane.YES_NO_OPTION, WOptionPane.QUESTION_MESSAGE, null) {
                    events += "result:$marker"
                }
                WeakReference(marker)
            }
            eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeTrue() }
            val probe = OpenContentDialogs.probe(owner).shouldNotBeNull()
            TopLevelWindows.closeByTitle(TITLE)
            eventually(5.seconds) { OpenContentDialogs.isLoaded(owner) shouldBe false }
            Reclamation.awaitCollected(marker).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WFlyout whose owner window was closed while open is collected and its native destroyed once the reference is released") {
            val (frame, owner) = openFrame()
            val (reference, probe) = onUiThreadGet {
                val flyout = WFlyout(WLabel("Content"))
                flyout.addPopupMenuListener(recorder())
                flyout.showAt(owner)
                WeakReference(flyout) to NativeProbe(flyout.inspectable)
            }
            eventually(5.seconds) { events() shouldContainExactly listOf("visible") }
            onUiThread { frame.dispose() }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }
    }

    private companion object {
        const val TITLE = "winui4k window close release test"
    }
}
