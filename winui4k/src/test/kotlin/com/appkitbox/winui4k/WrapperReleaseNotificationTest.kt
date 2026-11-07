package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference

/**
 * Tests that the handlers and references of notification components ([WInfoBar]'s Closing / Closed, a component bound to a [WToolTip]) do not keep the wrappers alive,
 * and that the wrappers and natives are collected once the references are released ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleaseNotificationTest : FunSpec() {
    /** Puts the component made by [create] in a window and removes it (waiting until Unloaded is processed), and returns only a weak reference and a native watcher. */
    private fun shownAndHidden(create: () -> WComponent): Pair<WeakReference<WComponent>, NativeProbe> {
        val component = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish in the same cycle
        return WeakReference(component) to onUiThreadGet { NativeProbe(component.inspectable) }
    }

    init {
        test("a WInfoBar with Closing and Closed listeners added and removed is collected and its native is destroyed") {
            val (reference, probe) = onUiThreadGet {
                val bar = WInfoBar()
                val closing: (WInfoBarClosingEvent) -> Unit = { }
                val closed: (InfoBarCloseReason) -> Unit = { }
                bar.addClosingListener(closing)
                bar.addCloseListener(closed)
                bar.isOpen = true
                bar.isOpen = false
                bar.removeClosingListener(closing)
                bar.removeCloseListener(closed)
                WeakReference(bar) to NativeProbe(bar.inspectable)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WInfoBar put in a window and removed with Closing and Closed listeners still added is collected and its native is destroyed") {
            val (reference, probe) = shownAndHidden {
                WInfoBar().also { bar ->
                    bar.addClosingListener { }
                    bar.addCloseListener { }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a component bound to a WToolTip and the tooltip are both collected once the references are released") {
            val (button, tip) = onUiThreadGet {
                val button = WButton("Tooltip target")
                val tip = WToolTip().also { it.text = "Description" }
                button.toolTip = tip
                WeakReference(button) to WeakReference(tip)
            }
            Reclamation.awaitCollected(button).shouldBeTrue()
            Reclamation.awaitCollected(tip).shouldBeTrue()
        }
    }
}
