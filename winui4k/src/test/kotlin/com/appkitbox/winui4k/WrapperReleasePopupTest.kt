package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that the handlers of [WPopup]'s Opened / Closed / PopupMenuListener and [WFlyout] / [WCommandBarFlyout]'s PopupMenuListener do not capture the wrapper,
 * and that the wrapper and native are collected once opened, closed and released ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleasePopupTest : FunSpec() {
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

    init {
        beforeTest { events.clear() }

        test("a WPopup never opened, with all Opened and Closed listeners removed, is collected and its native is destroyed") {
            val (reference, probe) = onUiThreadGet {
                val popup = WPopup(WLabel("Content"))
                val opened: () -> Unit = { }
                val closed: () -> Unit = { }
                val listener = recorder()
                popup.addOpenListener(opened)
                popup.addCloseListener(closed)
                popup.addPopupMenuListener(listener)
                popup.removeOpenListener(opened)
                popup.removeCloseListener(closed)
                popup.removePopupMenuListener(listener)
                WeakReference(popup) to NativeProbe(popup.inspectable)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WPopup opened and closed with listeners still added is collected and its native is destroyed once the owner is removed and the reference released") {
            val owner = onUiThreadGet { WButton("Owner") }
            UiTestHarness.attachAndAwaitLoaded(owner)
            val (reference, probe) = try {
                val closed = events
                val (reference, probe) = onUiThreadGet {
                    val popup = WPopup(WLabel("Content"))
                    popup.addOpenListener { closed += "open" }
                    popup.addCloseListener { closed += "close" }
                    popup.addPopupMenuListener(recorder())
                    WeakReference(popup) to NativeProbe(popup.inspectable)
                }
                onUiThread { reference.get()?.show(owner, 5.0, 5.0) }
                eventually(5.seconds) { events().toSet() shouldBe setOf("visible", "open") } // The order is checked by WPopupTest
                onUiThread { reference.get()?.hide() }
                eventually(5.seconds) { events().toSet() shouldBe setOf("visible", "open", "close", "invisible") }
                reference to probe
            } finally {
                UiTestHarness.detach(owner)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WFlyout opened and closed with a PopupMenuListener is collected and its native is destroyed once the button is removed and the reference released") {
            val button = onUiThreadGet { WButton("Open") }
            UiTestHarness.attachAndAwaitLoaded(button)
            val (reference, probe) = try {
                val (reference, probe) = onUiThreadGet {
                    val flyout = WFlyout(WLabel("Content"))
                    flyout.addPopupMenuListener(recorder())
                    WeakReference(flyout) to NativeProbe(flyout.inspectable)
                }
                onUiThread { reference.get()?.showAt(button) }
                eventually(5.seconds) { events() shouldContainExactly listOf("visible") }
                onUiThread { reference.get()?.hide() }
                eventually(5.seconds) { events() shouldContainExactly listOf("visible", "invisible") }
                reference to probe
            } finally {
                UiTestHarness.detach(button)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WCommandBarFlyout closed by pressing a button in its lower menu is collected together with the button, and its native destroyed, even if the listener captures that button") {
            val button = onUiThreadGet { WButton("Open") }
            UiTestHarness.attachAndAwaitLoaded(button)
            val references = try {
                val events = events
                val (flyout, native, save) = onUiThreadGet {
                    val flyout = WCommandBarFlyout()
                    val save = WAppBarButton("Save").also { flyout.addSecondaryCommand(it) }
                    flyout.addSecondaryCommand(WAppBarToggleButton("Bold"))
                    // If the button's native handler (which sets the "chosen" mark when pressed) captured the listener too,
                    // it would form a cycle: the button's native → listener → button wrapper → button's native
                    flyout.addPopupMenuListener(object : WPopupMenuAdapter() {
                        override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                            events += "invisible:${save.isEnabled}"
                        }
                    })
                    Triple(WeakReference(flyout), NativeProbe(flyout.inspectable), WeakReference(save))
                }
                onUiThread { flyout.get()?.showAt(button) }
                eventually(5.seconds) { onUiThreadGet { flyout.get()?.isOpen } shouldBe true }
                onUiThread { save.get()?.doClick() }
                eventually(5.seconds) { events() shouldContainExactly listOf("invisible:true") }
                Triple(flyout, native, save)
            } finally {
                UiTestHarness.detach(button)
            }
            Reclamation.awaitCollected(references.first).shouldBeTrue()
            Reclamation.awaitCollected(references.third).shouldBeTrue()
            Reclamation.awaitDestroyed(references.second).shouldBeTrue()
        }
    }
}
