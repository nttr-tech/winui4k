package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference
import java.util.function.IntConsumer

/**
 * Tests that a WSelectorBar with selection listeners (IntConsumer, Kotlin lambda, WChangeListener) is not captured strongly by handlers
 * and is collected after being removed from the window (continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass).
 * Collection is awaited while prompting GC ([Reclamation]).
 */
class WrapperReleaseSelectorBarTest : FunSpec() {
    /** The values the listeners received. The listeners do not reference the SelectorBar (they only record here). */
    private val received = mutableListOf<Any>()

    /** Puts the SelectorBar made by [create] in a window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WSelectorBar): WeakReference<WSelectorBar> {
        val bar = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(bar)
        UiTestHarness.detach(bar)
        Reclamation.awaitWithGc { !onUiThreadGet { bar.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish in the same cycle
        return WeakReference(bar)
    }

    init {
        test("a SelectorBar with an IntConsumer receiving selectedIndex is collected once released after being loaded and removed") {
            val sink = received
            val reference = shownAndHidden {
                WSelectorBar().also {
                    it.addItem(WSelectorBarItem("A"))
                    it.addSelectionListenerForJava(IntConsumer { index -> sink += index })
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a SelectorBar with a Kotlin lambda and a WChangeListener is also collected once released after being loaded and removed") {
            val sink = received
            val reference = shownAndHidden {
                WSelectorBar().also {
                    it.addItem(WSelectorBarItem("A"))
                    it.addSelectionListener { index -> sink += index }
                    it.addChangeListener(WChangeListener { e -> sink += e.source })
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a SelectorBar with listeners is not collected until removed even with no references anywhere, and receives selection notifications") {
            val sink = received
            sink.clear()
            val reference = onUiThreadGet {
                val bar = WSelectorBar()
                bar.addItem(WSelectorBarItem("A"))
                bar.addSelectionListenerForJava(IntConsumer { index -> sink += index })
                WeakReference(bar)
            }
            Reclamation.survivesGc(reference).shouldBeTrue()
            onUiThread { (reference.get() as WSelectorBar).selectedIndex = 0 }
            (sink == listOf<Any>(0)).shouldBeTrue()
        }
    }
}
