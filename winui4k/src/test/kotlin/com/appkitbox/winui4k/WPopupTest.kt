package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WPopup's Swing-like ways of opening (show(owner, x, y) at coordinates of the owner, isVisible), the Opened listener and the PopupMenuListener,
 * by actually opening it in the shared window. The shape seen from Java is tested in WPopupForJavaTest.
 */
@Tags("Integration")
class WPopupTest : FunSpec() {
    private suspend fun withOwner(block: suspend (owner: WButton) -> Unit) {
        val panel = onUiThreadGet { WPanel() }
        val owner = onUiThreadGet { WButton("Owner").also { panel.add(it) } }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    private suspend fun awaitOpen(popup: WPopup, open: Boolean) {
        eventually(5.seconds) { onUiThreadGet { popup.isOpen } shouldBe open }
    }

    init {
        test("show(owner, x, y) opens with the offset set to the position in the owner's window + (x, y), and can be opened and closed with isVisible") {
            withOwner { owner ->
                val popup = onUiThreadGet { WPopup(WLabel("Content")) }
                val origin = onUiThreadGet { Xaml.positionInRoot(owner.uiElement) }
                onUiThread { popup.show(owner, 12.0, 34.0) }
                awaitOpen(popup, true)
                val shown = onUiThreadGet { listOf(popup.horizontalOffset, popup.verticalOffset) }
                onUiThreadGet { popup.isVisible } shouldBe true
                onUiThread { popup.isVisible = false }
                awaitOpen(popup, false)
                onUiThreadGet { popup.isVisible } shouldBe false
                onUiThread { popup.isVisible = true } // Reopen at the same position in the last owner's window
                awaitOpen(popup, true)
                val reopened = onUiThreadGet { listOf(popup.horizontalOffset, popup.verticalOffset) }
                onUiThread { popup.hide() }
                awaitOpen(popup, false)

                shown[0] shouldBe ((origin[0] + 12.0) plusOrMinus 0.01)
                shown[1] shouldBe ((origin[1] + 34.0) plusOrMinus 0.01)
                reopened shouldContainExactly shown
            }
        }

        test("setting isVisible = true on a WPopup that has never been opened throws IllegalStateException") {
            onUiThreadGet {
                runCatching { WPopup().isVisible = true }.exceptionOrNull()?.javaClass
            } shouldBe IllegalStateException::class.java
        }

        test("addOpenListener is called every time it opens, and can be removed with removeOpenListener") {
            withOwner { owner ->
                val events = mutableListOf<String>()
                val popup = onUiThreadGet { WPopup(WLabel("Content")) }
                val listener: () -> Unit = { events += "open" }
                onUiThread {
                    popup.addOpenListener(listener)
                    popup.addCloseListener { events += "close" }
                }
                onUiThread { popup.show(owner) }
                awaitOpen(popup, true)
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("open") }
                onUiThread { popup.hide() }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("open", "close") }
                onUiThread { popup.removeOpenListener(listener) }
                onUiThread { popup.show(owner) }
                awaitOpen(popup, true)
                onUiThread { popup.hide() }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("open", "close", "close") }
            }
        }

        test("child, horizontalOffset, verticalOffset and isLightDismissEnabled read back the set values from their defaults") {
            // The defaults are WinUI's Popup defaults (offset 0, light dismiss disabled)
            val label = onUiThreadGet { WLabel("Content") }
            val results = onUiThreadGet {
                val popup = WPopup()
                val initial = listOf(popup.child == null, popup.horizontalOffset, popup.verticalOffset, popup.isLightDismissEnabled)
                popup.child = label
                popup.horizontalOffset = 12.5
                popup.verticalOffset = -7.25
                popup.isLightDismissEnabled = true
                val set = listOf(popup.child === label, popup.horizontalOffset, popup.verticalOffset, popup.isLightDismissEnabled)
                popup.isLightDismissEnabled = false
                initial + set + popup.isLightDismissEnabled
            }
            results shouldContainExactly listOf(true, 0.0, 0.0, false, true, 12.5, -7.25, true, false)
        }

        test("show(owner) opens without changing the offsets and shows child. Even with light dismiss enabled, hide closes it and Closed is called") {
            withOwner { owner ->
                val label = onUiThreadGet { WLabel("Content") }
                val events = mutableListOf<String>()
                val popup = onUiThreadGet {
                    WPopup(label).also {
                        it.horizontalOffset = 30.0
                        it.verticalOffset = 40.0
                        it.isLightDismissEnabled = true
                        it.addCloseListener { events += "close" }
                    }
                }
                onUiThread { popup.show(owner) }
                awaitOpen(popup, true)
                eventually(5.seconds) {
                    onUiThreadGet { label.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } shouldBe true
                }
                onUiThreadGet { listOf(popup.horizontalOffset, popup.verticalOffset) } shouldContainExactly listOf(30.0, 40.0)
                onUiThread { popup.hide() }
                awaitOpen(popup, false)
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("close") }
            }
        }

        test("the PopupMenuListener does not treat closing with hide as a cancellation, and when closed otherwise calls canceled before willBecomeInvisible") {
            withOwner { owner ->
                val events = mutableListOf<String>()
                val popup = onUiThreadGet { WPopup(WLabel("Content")) }
                val listener = object : WPopupMenuListener {
                    override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                        events += "visible:${e.source === popup}"
                    }

                    override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                        events += "invisible:${e.source === popup}"
                    }

                    override fun popupMenuCanceled(e: WPopupMenuEvent) {
                        events += "canceled:${e.source === popup}"
                    }
                }
                onUiThread { popup.addPopupMenuListener(listener) }
                onUiThreadGet { popup.popupMenuListeners.toList() } shouldContainExactly listOf<WPopupMenuListener>(listener)
                onUiThread { popup.show(owner, 0.0, 0.0) }
                awaitOpen(popup, true)
                onUiThread { popup.hide() }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible:true", "invisible:true") }
                onUiThread { popup.show(owner, 0.0, 0.0) }
                awaitOpen(popup, true)
                // Close natively without going through WinUI4K's hide, the same as a light dismiss
                onUiThread { popup.inspectable.putBool(XamlInterop.IPopup_put_IsOpen, false) }
                eventually(5.seconds) {
                    onUiThreadGet { events.toList() } shouldContainExactly listOf(
                        "visible:true",
                        "invisible:true",
                        "visible:true",
                        "canceled:true",
                        "invisible:true",
                    )
                }
                onUiThread { popup.removePopupMenuListener(listener) }
                onUiThreadGet { popup.popupMenuListeners.size } shouldBe 0
            }
        }

        context("Events") {
            test("a lambda removed with removeCloseListener is not called on closing, and the remaining listeners are called") {
                withOwner { owner ->
                    val events = mutableListOf<String>()
                    val popup = onUiThreadGet { WPopup(WLabel("Content")) }
                    val removed: () -> Unit = { events += "removed" }
                    onUiThread {
                        popup.addCloseListener(removed)
                        popup.addCloseListener { events += "close" }
                        popup.removeCloseListener(removed)
                        popup.removeCloseListener(removed) // The second time does nothing
                    }
                    onUiThread { popup.show(owner) }
                    awaitOpen(popup, true)
                    onUiThread { popup.hide() }
                    awaitOpen(popup, false)
                    eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("close") }
                }
            }
        }
    }
}
