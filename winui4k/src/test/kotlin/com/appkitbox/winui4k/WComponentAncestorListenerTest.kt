package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [WComponent.addAncestorListener], which corresponds to JComponent.addAncestorListener.
 * Expected values come from Swing's rules (javax.swing.AncestorNotifier): ancestorAdded when it and all its ancestors become visible and connected
 * to a window (placed in a window, or a hidden ancestor was shown), ancestorRemoved the opposite, and ancestorMoved when it or an ancestor moved.
 * Events arrive on the next turn of the UI thread, so the checks wait for them (eventually).
 */
@Tags("Integration")
class WComponentAncestorListenerTest : FunSpec() {
    /** A listener that keeps received events together with their names (touched only from the UI thread). */
    private class Recorder : WAncestorListener {
        val names = ArrayList<String>()
        val events = ArrayList<WAncestorEvent>()

        override fun ancestorAdded(event: WAncestorEvent) = log("added", event)

        override fun ancestorRemoved(event: WAncestorEvent) = log("removed", event)

        override fun ancestorMoved(event: WAncestorEvent) = log("moved", event)

        private fun log(name: String, event: WAncestorEvent) {
            names += name
            events += event
        }
    }

    /** Waits until [component] is removed from XAML (IsLoaded becomes false) and also finishes the turn after Unloaded. */
    private suspend fun awaitUnloaded(component: WComponent) {
        eventually(10.seconds) {
            onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } shouldBe false
        }
        onUiThread { }
        onUiThread { }
    }

    init {
        context("Defaults") {
            test("ancestorListeners of a component without listeners are empty") {
                onUiThreadGet { WLabel("Label").ancestorListeners.toList() }.shouldBeEmpty()
            }
        }

        context("Events") {
            test("placing a panel containing a label with a listener in the window brings ancestorAdded once, and removing it brings ancestorRemoved") {
                val recorder = Recorder()
                val (panel, label) = onUiThreadGet {
                    val label = WLabel("Label").also { it.addAncestorListener(recorder) }
                    WPanel().also { it.add(label) } to label
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("added") }
                onUiThreadGet {
                    val event = recorder.events.single()
                    listOf(event.id, event.component === label, event.source === label, event.ancestor === label, event.ancestorParent === panel)
                } shouldBe listOf(WAncestorEvent.ANCESTOR_ADDED, true, true, true, true)
                UiTestHarness.detach(panel)
                eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("added", "removed") }
                onUiThreadGet { recorder.events.last().id } shouldBe WAncestorEvent.ANCESTOR_REMOVED
            }

            test("merely adding a listener to a component on screen does not bring ancestorAdded") {
                val recorder = Recorder()
                val label = onUiThreadGet { WLabel("Label") }
                UiTestHarness.attachAndAwaitLoaded(label)
                try {
                    onUiThread { label.addAncestorListener(recorder) }
                    onUiThread { label.text = "Change it to trigger a layout" }
                    label.updateLayoutOnUi()
                    onUiThreadGet { recorder.names.toList() }.shouldBeEmpty()
                } finally {
                    onUiThread { label.removeAncestorListener(recorder) }
                    UiTestHarness.detach(label)
                }
            }

            test("hiding the parent panel brings ancestorRemoved to the child, and showing it again brings ancestorAdded") {
                val recorder = Recorder()
                val (panel, label) = onUiThreadGet {
                    val label = WLabel("Label")
                    WPanel().also { it.add(label) } to label
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                try {
                    onUiThread { label.addAncestorListener(recorder) }
                    onUiThread { panel.isVisible = false }
                    eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("removed") }
                    onUiThread { panel.isVisible = true }
                    eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("removed", "added") }
                } finally {
                    onUiThread { label.removeAncestorListener(recorder) }
                    UiTestHarness.detach(panel)
                }
            }

            test("when an upper sibling grows taller and the parent panel shifts down, the child label gets ancestorMoved (staying on screen)") {
                val recorder = Recorder()
                val (outer, spacer, label) = onUiThreadGet {
                    val label = WLabel("Label")
                    val spacer = WPanel().also { it.height = 10.0 }
                    val outer = WPanel().also {
                        it.add(spacer)
                        it.add(WPanel().also { inner -> inner.add(label) })
                    }
                    Triple(outer, spacer, label)
                }
                UiTestHarness.attachAndAwaitLoaded(outer)
                try {
                    onUiThread { label.addAncestorListener(recorder) }
                    onUiThread { spacer.height = 60.0 }
                    eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("moved") }
                    onUiThreadGet { recorder.events.single().id } shouldBe WAncestorEvent.ANCESTOR_MOVED
                } finally {
                    onUiThread { label.removeAncestorListener(recorder) }
                    UiTestHarness.detach(outer)
                }
            }

            test("a listener removed with removeAncestorListener gets nothing even when later removed from the window") {
                val recorder = Recorder()
                val label = onUiThreadGet { WLabel("Label").also { it.addAncestorListener(recorder) } }
                UiTestHarness.attachAndAwaitLoaded(label)
                eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("added") }
                onUiThread { label.removeAncestorListener(recorder) }
                UiTestHarness.detach(label)
                awaitUnloaded(label)
                onUiThreadGet { recorder.names.toList() to label.ancestorListeners.size } shouldBe (listOf("added") to 0)
            }
        }

        context("Boundaries and exceptions") {
            test("adding or removing null does nothing") {
                onUiThreadGet {
                    val label = WLabel("Label")
                    label.addAncestorListener(null)
                    label.removeAncestorListener(null)
                    label.ancestorListeners.size
                } shouldBe 0
            }
        }

        context("Swing compatibility") {
            test("hiding the component itself also brings ancestorRemoved (Swing's AncestorNotifier also watches its own componentHidden)") {
                val recorder = Recorder()
                val label = onUiThreadGet { WLabel("Label") }
                UiTestHarness.attachAndAwaitLoaded(label)
                try {
                    onUiThread { label.addAncestorListener(recorder) }
                    onUiThread { label.isVisible = false }
                    eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("removed") }
                } finally {
                    onUiThread { label.removeAncestorListener(recorder) }
                    UiTestHarness.detach(label)
                }
            }

            test("adding the same listener twice makes it called twice, and it appears twice in ancestorListeners") {
                val recorder = Recorder()
                val label = onUiThreadGet {
                    WLabel("Label").also {
                        it.addAncestorListener(recorder)
                        it.addAncestorListener(recorder)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(label)
                try {
                    eventually(10.seconds) { onUiThreadGet { recorder.names.toList() } shouldBe listOf("added", "added") }
                    onUiThreadGet { label.ancestorListeners.size } shouldBe 2
                } finally {
                    onUiThread {
                        label.removeAncestorListener(recorder)
                        label.removeAncestorListener(recorder)
                    }
                    UiTestHarness.detach(label)
                }
            }
        }
    }

    /** Finishes the layout on the UI thread and also the turn after it (the event check). */
    private fun WComponent.updateLayoutOnUi() {
        onUiThread { updateLayout() }
        onUiThread { }
        onUiThread { }
    }
}
