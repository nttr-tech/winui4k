package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Checks that a [WLayoutPanel] (which always has its own SizeChanged / Loaded handlers) is collected without being captured strongly by the handlers
 * (also when it is the window content).
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

    /** Shows a window whose content is built by [configure], confirms it opened, disposes it, and returns only weak references. */
    private suspend fun shownAndDisposedFrame(configure: (WFrame, WButton) -> WComponent): BuiltFrame {
        val events = events
        val built = onUiThreadGet {
            val frame = WFrame(TITLE)
            frame.addWindowListener(
                object : WWindowAdapter() {
                    override fun windowOpened(e: WWindowEvent) {
                        events += "opened"
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
        eventually(5.seconds) { onUiThreadGet { built.child.get()?.isDisplayable } shouldBe true }
        onUiThread { built.frame.get()?.dispose() }
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
        }
    }

    private companion object {
        const val TITLE = "winui4k layout release test"
    }
}
