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

        test("a WToggleSwitch with an ActionListener that was toggled is collected after being placed in the window and removed") {
            val log = received
            val toggleSwitch = shownAndHidden {
                WToggleSwitch("Wi-Fi").also {
                    it.addActionListener { log += "action" }
                    it.isOn = true
                }
            }
            Reclamation.awaitCollected(toggleSwitch).shouldBeTrue()
        }

        test("a WToggleSplitButton, WComboBox and menu headings bound to a shared action are collected even while the action is kept") {
            val log = received
            val shared = object : WAbstractAction("Shared") {
                override fun actionPerformed(e: WActionEvent) {
                    log += "action"
                }
            }
            shared.putValue(WAction.SELECTED_KEY, true)
            val split = shownAndHidden { WToggleSplitButton().also { it.action = shared } }
            val combo = shownAndHidden { WComboBox(listOf("a")).also { it.action = shared } }
            val menu = WeakReference(onUiThreadGet { WMenuBarItem(shared) })
            val sub = WeakReference(onUiThreadGet { WMenuFlyoutSubItem(shared) })
            Reclamation.awaitCollected(split).shouldBeTrue()
            Reclamation.awaitCollected(combo).shouldBeTrue()
            Reclamation.awaitCollected(menu).shouldBeTrue()
            Reclamation.awaitCollected(sub).shouldBeTrue()
            shared.putValue(WAction.NAME, "After collection") // Notifications to collected components are discarded
        }
    }
}
