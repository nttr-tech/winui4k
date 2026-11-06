package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference

/**
 * Tests that components with a ComponentListener, and containers connected through the child list ([WContainer.components] and [WComponent.parent]),
 * are not strongly captured by the handlers and are collected after being removed (continuation of [WrapperReleaseTest],
 * split off to avoid detekt's LargeClass). Waits for collection while prompting GC ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleaseComponentTest : FunSpec() {
    /** Places [component] in the shared window, waits for Loaded, and removes it. Also waits until the Unloaded after the removal has been processed on the UI thread. */
    private fun showAndHide(component: WComponent) {
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handler to finish running in the same turn
    }

    /** Places a component created by [create] (created on the UI thread) in the window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WComponent): WeakReference<WComponent> {
        val component = onUiThreadGet(create)
        showAndHide(component)
        return WeakReference(component)
    }

    /**
     * Places and removes a panel containing a label with a ComponentListener, and returns weak references to the panel and the label.
     * The label is entrusted to the handler of the detached subtree's root (the panel), and the label's parent points to the panel.
     */
    private fun shownAndHiddenPanelWithListeningLabel(): Pair<WeakReference<WPanel>, WeakReference<WLabel>> {
        val (panel, label) = onUiThreadGet {
            val label = WLabel("label").also { it.addComponentListener(object : WComponentAdapter() {}) }
            WPanel().also { it.add(label) } to label
        }
        showAndHide(panel)
        return WeakReference(panel) to WeakReference(label)
    }

    init {
        test("a panel with a ComponentListener that does not reference the component is also collected after being placed in the window and removed") {
            val reference = shownAndHidden { WPanel().also { it.addComponentListener(object : WComponentAdapter() {}) } }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a panel that was never placed is also collected once all its ComponentListeners are removed") {
            val reference = onUiThreadGet {
                val panel = WPanel()
                val listener = object : WComponentAdapter() {}
                panel.addComponentListener(listener)
                panel.addComponentListener(listener)
                panel.removeComponentListener(listener)
                panel.removeComponentListener(listener)
                WeakReference(panel)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a panel containing a child with a ComponentListener is also collected together with the child once it is removed and no longer referenced") {
            val (panel, label) = shownAndHiddenPanelWithListeningLabel()
            Reclamation.awaitCollected(panel).shouldBeTrue()
            Reclamation.awaitCollected(label).shouldBeTrue()
        }

        test("a panel with an AncestorListener that does not reference the component is also collected after being placed in the window and removed") {
            val reference = shownAndHidden { WPanel().also { it.addAncestorListener(EmptyAncestorListener()) } }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a panel that was never placed is also collected once all its AncestorListeners are removed") {
            val reference = onUiThreadGet {
                val panel = WPanel()
                val listener = EmptyAncestorListener()
                panel.addAncestorListener(listener)
                panel.removeAncestorListener(listener)
                WeakReference(panel)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a panel with a ContainerListener and its children are also collected after being removed (no native handler is used)") {
            val reference = shownAndHidden {
                WPanel().also { panel ->
                    panel.addContainerListener(object : WContainerAdapter() {})
                    panel.add(WLabel("child"))
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }
    }
}

/** A [WAncestorListener] that does nothing (does not reference the component). */
private class EmptyAncestorListener : WAncestorListener {
    override fun ancestorAdded(event: WAncestorEvent) {
        // Does nothing
    }

    override fun ancestorRemoved(event: WAncestorEvent) {
        // Does nothing
    }

    override fun ancestorMoved(event: WAncestorEvent) {
        // Does nothing
    }
}
