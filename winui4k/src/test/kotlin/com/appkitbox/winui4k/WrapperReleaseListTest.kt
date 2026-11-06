package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference

/**
 * Tests that the handlers of the Swing-style selection (sync with selectionModel) of lists and tables (WList, WListBox, WTable) do not capture the wrapper strongly,
 * and that they are collected after removal (continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass).
 * Collection is awaited while prompting GC ([Reclamation]).
 */
class WrapperReleaseListTest : FunSpec() {
    /** Puts [component] in the shared window, waits for Loaded, and removes it. Also waits until the Unloaded after removal is processed on the UI thread. */
    private fun showAndHide(component: WComponent) {
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish in the same cycle
    }

    /** Puts the component made by [create] (created on the UI thread) in a window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WComponent): WeakReference<WComponent> {
        val component = onUiThreadGet(create)
        showAndHide(component)
        return WeakReference(component)
    }

    init {
        test("WList and WListBox using selectionModel and WListSelectionListener are collected after being put in a window and removed") {
            val list = shownAndHidden {
                WList(arrayOf("a", "b")).also { list ->
                    list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                    list.selectionModel.setSelectionInterval(0, 1)
                    list.addListSelectionListener { _: WListSelectionEvent -> }
                }
            }
            val listBox = shownAndHidden {
                WListBox(arrayOf("a", "b")).also { list ->
                    list.selectedIndices = intArrayOf(1)
                    list.addListSelectionListener { _: WListSelectionEvent -> }
                }
            }
            Reclamation.awaitCollected(list).shouldBeTrue()
            Reclamation.awaitCollected(listBox).shouldBeTrue()
        }

        test("a shown WList is not collected even when only its selectionModel is held, display-side selection reaches the model, and it is collected once the model is also released and the list removed") {
            val selection = arrayOfNulls<WListSelectionModel>(1)
            val reference = onUiThreadGet {
                val list = WList(arrayOf("a", "b", "c"))
                selection[0] = list.selectionModel
                WeakReference(list)
            }
            UiTestHarness.attachAndAwaitLoaded(checkNotNull(reference.get()))
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                onUiThreadGet {
                    checkNotNull(reference.get()).selectedIndex = 2
                    checkNotNull(selection[0]).getSelectedIndices().toList()
                } shouldBe listOf(2)
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
            }
            // The selection model holds the list through its listener (as in Swing), so it is collected once the model is also released
            selection[0] = null
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }
    }
}
