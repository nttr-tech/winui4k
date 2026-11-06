package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference

/**
 * Tests that value components (WSpinner and others) with a ChangeListener or a shared model attached are not strongly captured by the handlers or the model
 * and are collected after being removed (continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass).
 * Waits for collection while prompting GC ([Reclamation]).
 */
class WrapperReleaseValueTest : FunSpec() {
    /** The values and sources the ChangeListener received. The listener does not reference the component (it only records here). */
    private val changeSources = mutableListOf<Any>()

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

    init {
        test("a WSpinner with a ChangeListener and a shared model is collected after being placed in the window and removed if released together with the model") {
            val sources = changeSources
            var model: WeakReference<WSpinnerModel>? = null
            val plain = shownAndHidden { WSpinner().also { it.addChangeListener { value -> sources += value } } }
            val modelled = shownAndHidden {
                val shared = WSpinnerNumberModel(1, 0, 10, 1)
                model = WeakReference(shared)
                WSpinner(shared).also { it.addChangeListener(WChangeListener { e -> sources += e.source }) }
            }
            Reclamation.awaitCollected(plain).shouldBeTrue()
            Reclamation.awaitCollected(modelled).shouldBeTrue()
            Reclamation.awaitCollected(checkNotNull(model)).shouldBeTrue()
        }
    }
}
