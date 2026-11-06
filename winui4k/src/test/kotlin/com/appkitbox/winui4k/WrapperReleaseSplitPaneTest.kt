package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference

/**
 * Tests that a [WSplitPane] (the size change and Loaded handlers, and the divider's mouse listener) is not captured strongly by handlers,
 * and is collected together with its children and divider after removal or when never loaded (continuation of [WrapperReleaseTest]).
 */
class WrapperReleaseSplitPaneTest : FunSpec() {
    private class Parts(val pane: WeakReference<WSplitPane>, val divider: WeakReference<WComponent>, val left: WeakReference<WComponent>)

    private fun parts(pane: WSplitPane): Parts =
        Parts(WeakReference(pane), WeakReference(pane.divider), WeakReference(pane.leftComponent))

    private fun newPane(): WSplitPane = WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("left"), WLabel("right")).also {
        it.addChangeListener { }
        it.removeChangeListener(it.getChangeListeners()[0])
    }

    /** Loads a WSplitPane with a ChangeListener (which adds the divider listeners), removes it, and returns only a weak reference. */
    private fun shownAndHidden(): Parts {
        val pane = onUiThreadGet { newPane().also { pane -> pane.addChangeListener { } } }
        UiTestHarness.attachAndAwaitLoaded(pane)
        UiTestHarness.detach(pane)
        Reclamation.awaitWithGc { !onUiThreadGet { pane.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish in the same cycle
        onUiThreadGet { pane.divider.mouseListeners.size } shouldBe 1
        return parts(pane)
    }

    init {
        test("a WSplitPane never loaded is collected together with its children and divider") {
            val p = onUiThreadGet { parts(newPane()) }
            Reclamation.awaitCollected(p.pane).shouldBeTrue()
            Reclamation.awaitCollected(p.divider).shouldBeTrue()
            Reclamation.awaitCollected(p.left).shouldBeTrue()
        }

        test("a WSplitPane that was loaded and accepted divider drags is also collected when removed with the ChangeListener still added") {
            val p = shownAndHidden()
            Reclamation.awaitCollected(p.pane).shouldBeTrue()
            Reclamation.awaitCollected(p.divider).shouldBeTrue()
            Reclamation.awaitCollected(p.left).shouldBeTrue()
        }
    }
}
