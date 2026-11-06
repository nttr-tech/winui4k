package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference

/**
 * Tests that the button state change callbacks (ChangeListener, per-state icons) and ActionListeners and Actions attached to components other than buttons
 * do not capture the button strongly and are collected after being placed in the window and removed (a continuation of [WrapperReleaseTest]).
 * Waits for collection while encouraging GC ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleaseButtonTest : FunSpec() {
    /** The number received by the listeners. The listeners do not reference the components (they only record here). */
    private val received = mutableListOf<String>()

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

    init {
        test("a WButton and WToggleButton with a ChangeListener whose states were changed are collected after being placed in the window and removed") {
            val log = received
            val button = shownAndHidden {
                WButton("Save").also {
                    it.addChangeListener(WChangeListener { log += "button" })
                    it.isEnabled = false
                }
            }
            val toggle = shownAndHidden {
                WToggleButton("Bold").also {
                    it.addChangeListener(WChangeListener { log += "toggle" })
                    it.isSelected = true
                }
            }
            Reclamation.awaitCollected(button).shouldBeTrue()
            Reclamation.awaitCollected(toggle).shouldBeTrue()
        }

        test("a toggle with selectedIcon and disabledIcon (kept through state notifications while shown) is collected after being placed in the window and removed") {
            val toggle = shownAndHidden {
                WToggleButton("", Symbol.PLAY).also {
                    it.selectedIcon = Symbol.PAUSE
                    it.disabledIcon = Symbol.CANCEL
                    it.isSelected = true
                }
            }
            Reclamation.awaitCollected(toggle).shouldBeTrue()
        }
    }
}
