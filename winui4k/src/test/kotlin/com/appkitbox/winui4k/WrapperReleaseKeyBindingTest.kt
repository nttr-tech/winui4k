package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference

/**
 * Tests that the key binding handlers ([WComponent.getInputMap] / [WComponent.actionMap]) (the component's own routed key events and the ones attached to
 * the window content for WHEN_IN_FOCUSED_WINDOW) do not capture the wrapper strongly and are collected after removal.
 * Even if an action references the component, the cycle is only within the JVM, so it is collected. Waits for collection while encouraging GC ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleaseKeyBindingTest : FunSpec() {
    /** Places the component created by [create] (created on the UI thread) in the window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WComponent): WeakReference<WComponent> {
        val component = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish running in the same turn
        return WeakReference(component)
    }

    /** Maps F7 in the map for [condition] to an action that references [button]. */
    private fun bindF7(button: WButton, condition: Int) {
        button.getInputMap(condition).put(WKeyStroke.getKeyStroke("F7"), "press")
        button.actionMap.put(
            "press",
            object : WAbstractAction() {
                override fun actionPerformed(e: WActionEvent) {
                    button.text = "Pressed"
                }
            },
        )
    }

    init {
        context("Lifetime") {
            test("a button with WHEN_FOCUSED and WHEN_ANCESTOR_OF_FOCUSED_COMPONENT key bindings is also collected after being placed in the window and removed") {
                val reference = shownAndHidden {
                    WButton("OK").also {
                        bindF7(it, WComponent.WHEN_FOCUSED)
                        bindF7(it, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    }
                }
                Reclamation.awaitCollected(reference).shouldBeTrue()
            }

            test("a button with a WHEN_IN_FOCUSED_WINDOW key binding is also collected after removal, with the handlers left attached to the window content") {
                val reference = shownAndHidden { WButton("OK").also { bindF7(it, WComponent.WHEN_IN_FOCUSED_WINDOW) } }
                Reclamation.awaitCollected(reference).shouldBeTrue()
            }

            test("a button that registered a listener referencing the button with registerKeyboardAction is also collected if never placed") {
                val reference = onUiThreadGet {
                    val button = WButton("OK")
                    button.registerKeyboardAction({ button.text = "Pressed" }, WKeyStroke.getKeyStroke("F7"), WComponent.WHEN_IN_FOCUSED_WINDOW)
                    WeakReference(button)
                }
                Reclamation.awaitCollected(reference).shouldBeTrue()
            }
        }
    }
}
