package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Checks that a [WLayoutPanel] (which always has its own SizeChanged / Loaded handlers) is collected without being captured strongly by the handlers
 * (also when it is the window content). It also checks that [ListenerAnchor] entrusts wrappers with listeners to the root of a removed subtree without being misled by
 * "adding a Loaded handler to an already loaded element makes IsLoaded false until it fires" (doc/verification.md).
 * Waits for collection while encouraging GC ([Reclamation]). The shared "winui4k tests" window is left open.
 */
@Tags("Memory")
class WrapperReleaseLayoutTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    /** Weak references to the window, the content and the child wrappers, and native weak references to the Window and the children. */
    private class BuiltFrame(
        val frame: WeakReference<WFrame>,
        val content: WeakReference<WComponent>,
        val child: WeakReference<WButton>,
        val window: NativeProbe,
        val childNative: NativeProbe,
    )

    /** Waits until [component] is removed from the window ([WComponent.isDisplayable] becomes false) and also finishes the turn after Unloaded. */
    private fun awaitDetached(component: WComponent) {
        Reclamation.awaitWithGc { !onUiThreadGet { component.isDisplayable } }.shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish running in the same turn
    }

    /** Adds a label to the shared window and removes it. When an element enters the tree, WinUI sends the pending Loaded events together (RaiseLoadedEvent in eventmgr.cpp). */
    private fun flushPendingLoaded() {
        val label = onUiThreadGet { WLabel("flush") }
        UiTestHarness.attachAndAwaitLoaded(label)
        UiTestHarness.detach(label)
        awaitDetached(label)
    }

    /**
     * Shows a window whose content is built with [configure], checks that it opened, disposes it, and returns only weak references.
     * If [beforeLoad], it is disposed right away when it becomes active (windowActivated) if the content is not loaded yet
     * (WFrame waits for the content's Loaded to put the initial focus, but Loaded does not come for a closed window).
     */
    private suspend fun shownAndDisposedFrame(
        beforeLoad: Boolean = false,
        configure: (WFrame, WButton) -> WComponent,
    ): BuiltFrame {
        val events = events
        val built = onUiThreadGet {
            val frame = WFrame(TITLE)
            frame.addWindowListener(
                object : WWindowAdapter() {
                    override fun windowOpened(e: WWindowEvent) {
                        events += "opened"
                    }

                    override fun windowActivated(e: WWindowEvent) {
                        if (beforeLoad && !frame.contentPane.isDisplayable) {
                            events += "disposed before load"
                            frame.dispose()
                        }
                    }

                    override fun windowClosed(e: WWindowEvent) {
                        events += "closed"
                    }
                },
            )
            val button = WButton("Center")
            val content = configure(frame, button)
            frame.isVisible = true
            BuiltFrame(WeakReference(frame), WeakReference(content), WeakReference(button), NativeProbe(frame.window), NativeProbe(button.inspectable))
        }
        eventually(5.seconds) { events() shouldContain "opened" }
        if (beforeLoad) {
            eventually(5.seconds) { events() shouldContain "disposed before load" }
        } else {
            eventually(5.seconds) { onUiThreadGet { built.child.get()?.isDisplayable } shouldBe true }
            onUiThread { built.frame.get()?.dispose() }
        }
        eventually(5.seconds) { events() shouldContain "closed" }
        return built
    }

    private fun BuiltFrame.shouldBeReclaimed() {
        Reclamation.awaitCollected(frame).shouldBeTrue()
        Reclamation.awaitCollected(content).shouldBeTrue()
        Reclamation.awaitCollected(child).shouldBeTrue()
        Reclamation.awaitDestroyed(window).shouldBeTrue()
        Reclamation.awaitDestroyed(childNative).shouldBeTrue()
    }

    /** Weak references to the removed panel (held only by native code) and the label with a listener inside it, and the native reference to the panel held by the test. */
    private class DetachedPanel(val panel: WeakReference<WPanel>, val label: WeakReference<WLabel>, val native: ComPtr)

    /**
     * Places a panel with a label in the shared window, removes it, and leaves a reference only in the panel's native object (the same as when native code
     * keeps a subtree, as with Flyouts and tabs). If [lateListener], the label's ComponentListener is added after it is placed and Loaded
     * (the label's [ListenerAnchor] is created then and adds a Loaded handler to the already loaded label).
     */
    private fun detachedPanelKeptNatively(lateListener: Boolean): DetachedPanel {
        val (panel, label) = onUiThreadGet {
            val label = WLabel("Label")
            if (!lateListener) label.addComponentListener(object : WComponentAdapter() {})
            WPanel().also { it.add(label) } to label
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        if (lateListener) {
            // Remove it in the same turn as adding (so that the pending Loaded is not sent by other elements entering the tree)
            onUiThread {
                label.addComponentListener(object : WComponentAdapter() {})
                UiTestHarness.sharedFrame().contentPane.remove(panel)
            }
        } else {
            UiTestHarness.detach(panel)
        }
        awaitDetached(panel)
        val native = onUiThreadGet { panel.uiElement.also { it.addRef() } }
        return DetachedPanel(WeakReference(panel), WeakReference(label), native)
    }

    /** Places a WLayoutPanel with a child in the shared window, removes it, and returns weak references to the panel, the child and the Canvas. */
    private fun shownAndHiddenLayoutPanel(): Triple<WeakReference<WLayoutPanel>, WeakReference<WButton>, NativeProbe> {
        val (panel, child) = onUiThreadGet {
            val child = WButton("Child")
            WLayoutPanel(WFlowLayout()).also { it.add(child) } to child
        }
        val probe = onUiThreadGet { NativeProbe(panel.inspectable) }
        UiTestHarness.attachAndAwaitLoaded(panel)
        UiTestHarness.detach(panel)
        awaitDetached(panel)
        return Triple(WeakReference(panel), WeakReference(child), probe)
    }

    /**
     * Adds a panel containing a button with a listener to [outer], which is in the tree, and waits for Loaded. After adding a Loaded listener to [outer]
     * (while pending, the IsLoaded of [outer] is false), removes the panel, has the pending Loaded sent, and returns weak references to the panel and the button.
     */
    private fun innerPanelRemovedFromPendingParent(outer: WPanel): Pair<WeakReference<WPanel>, WeakReference<WButton>> {
        val (inner, button) = onUiThreadGet {
            val button = WButton("Button").also { it.addActionListener { } }
            WPanel().also { it.add(button) } to button
        }
        awaitLoaded(inner) { outer.add(inner) }
        onUiThread { outer.addLoadedListener { } }
        onUiThread { outer.remove(inner) }
        awaitDetached(inner)
        flushPendingLoaded()
        return WeakReference(inner) to WeakReference(button)
    }

    /** Adds a Loaded listener to [component], calls [attach] on the UI thread, waits until Loaded, and removes the listener. */
    private fun awaitLoaded(component: WComponent, attach: () -> Unit) {
        val loaded = CountDownLatch(1)
        val listener: () -> Unit = { loaded.countDown() }
        onUiThread {
            component.addLoadedListener(listener)
            attach()
        }
        loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
        onUiThread { component.removeLoadedListener(listener) }
    }

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        beforeTest { events.clear() }

        context("Lifetime") {
            test("a WLayoutPanel with a child that was never placed has the panel and the child collected and the Canvas destroyed once the reference is dropped") {
                val (panel, child, probe) = onUiThreadGet {
                    val child = WButton("Child")
                    val panel = WLayoutPanel(WBorderLayout()).also { it.add(child, WBorderLayout.CENTER) }
                    Triple(WeakReference(panel), WeakReference(child), NativeProbe(panel.inspectable))
                }
                Reclamation.awaitCollected(panel).shouldBeTrue()
                Reclamation.awaitCollected(child).shouldBeTrue()
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
            }

            test("a WSettingsCard that was never placed has the card collected and the Canvas destroyed once the reference is dropped") {
                val (card, probe) = onUiThreadGet {
                    val card = WSettingsCard("Heading", "Description")
                    WeakReference(card) to NativeProbe(card.inspectable)
                }
                Reclamation.awaitCollected(card).shouldBeTrue()
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
            }

            test("a WLayoutPanel with a child placed in the shared window and removed has the panel and the child collected and the Canvas destroyed once the reference is dropped") {
                val (panel, child, probe) = shownAndHiddenLayoutPanel()
                Reclamation.awaitCollected(panel).shouldBeTrue()
                Reclamation.awaitCollected(child).shouldBeTrue()
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
            }

            test("a window with layout set to WBorderLayout and children added, shown, disposed and dropped, has the WFrame, content and children collected and the Window destroyed") {
                val built = shownAndDisposedFrame { frame, button ->
                    frame.layout = WBorderLayout()
                    frame.add(button, WBorderLayout.CENTER)
                    frame.add(WLabel("Top"), WBorderLayout.NORTH)
                    checkNotNull(button.parent) // The content WLayoutPanel
                }
                built.shouldBeReclaimed()
            }

            test("a window with a WLayoutPanel set as its content with setContentPane, shown, disposed and dropped, has the WFrame, content and children collected and the Window destroyed") {
                val built = shownAndDisposedFrame { frame, button ->
                    val panel = WLayoutPanel(WFlowLayout()).also { it.add(button) }
                    frame.setContentPane(panel)
                    panel
                }
                built.shouldBeReclaimed()
            }

            test("for a window disposed when it became active before the content loaded, releasing the references also collects the WFrame, the content and the children, and destroys the Window") {
                val built = shownAndDisposedFrame(beforeLoad = true) { frame, button ->
                    val panel = WLayoutPanel(WFlowLayout()).also { it.add(button) }
                    frame.setContentPane(panel)
                    panel
                }
                built.shouldBeReclaimed()
            }

            test("a label with a ComponentListener added before placing is not collected while only native code holds the removed panel, and is collected once that native object is dropped") {
                val detached = detachedPanelKeptNatively(lateListener = false)
                Reclamation.awaitCollected(detached.panel).shouldBeTrue()
                Reclamation.survivesGc(detached.label).shouldBeTrue()
                onUiThread { detached.native.release() }
                Reclamation.awaitCollected(detached.label).shouldBeTrue()
            }

            test("a label with a ComponentListener added after placing is also not collected while only native code holds the removed panel, and is collected once that native object is dropped") {
                val detached = detachedPanelKeptNatively(lateListener = true)
                Reclamation.awaitCollected(detached.panel).shouldBeTrue()
                Reclamation.survivesGc(detached.label).shouldBeTrue()
                onUiThread { detached.native.release() }
                Reclamation.awaitCollected(detached.label).shouldBeTrue()
            }

            test("even if a Loaded listener is later added to a parent still in the tree, the button with a listener inside a panel removed from that parent and dropped is collected") {
                val outer = onUiThreadGet { WPanel() }
                UiTestHarness.attachAndAwaitLoaded(outer)
                try {
                    val (inner, button) = innerPanelRemovedFromPendingParent(outer)
                    Reclamation.awaitCollected(inner).shouldBeTrue()
                    Reclamation.awaitCollected(button).shouldBeTrue()
                } finally {
                    UiTestHarness.detach(outer)
                }
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k layout release test"
    }
}
