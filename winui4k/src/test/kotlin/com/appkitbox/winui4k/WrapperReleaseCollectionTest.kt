package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.RowFilter
import com.appkitbox.winui4k.table.TableRowSorter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference

/**
 * Tests that native handlers attached to collection components (WTable, WTree, WItemsView) do not strongly capture the wrapper
 * and that the components are collected after being removed (continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass).
 * Waits for collection while prompting GC ([Reclamation]).
 */
class WrapperReleaseCollectionTest : FunSpec() {
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
        test("a WTable placed in the window and removed is collected, both the wrapper and the model holding the row values") {
            val modelReference = arrayOfNulls<WeakReference<DefaultTableModel>>(1)
            val reference = shownAndHidden {
                val rows = arrayOf<Array<Any?>>(arrayOf("Apple", 3), arrayOf("Orange", 5))
                val model = DefaultTableModel(rows, arrayOf<Any?>("Name", "Count"))
                modelReference[0] = WeakReference(model)
                WTable(model)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitCollected(checkNotNull(modelReference[0])).shouldBeTrue()
        }

        test("a WTable using sorting, filtering, grouping, row double-click and selection listeners is also collected after being placed in the window and removed") {
            val reference = shownAndHidden {
                WTable(listOf(listOf<Any?>("Apple", 3), listOf<Any?>("Orange", 5)), listOf<Any?>("Name", "Count")).also { table ->
                    table.autoCreateRowSorter = true
                    (table.rowSorter as TableRowSorter<*>).setRowFilter(RowFilter.regexFilter("e", 0))
                    table.groupBy(1)
                    table.addRowInvokedListener { }
                    table.addRowSelectionListener { }
                    table.addSortingListener { }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WTable is not collected while placed in the window even without references, and model changes reach the table") {
            val model = onUiThreadGet { DefaultTableModel(arrayOf<Array<Any?>>(arrayOf("Apple")), arrayOf<Any?>("Name")) }
            val reference = onUiThreadGet { WeakReference(WTable(model)) }
            UiTestHarness.attachAndAwaitLoaded(checkNotNull(reference.get()))
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                onUiThreadGet {
                    model.addRow(arrayOf<Any?>("Orange"))
                    checkNotNull(reference.get()).rowCount
                } shouldBe 2
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WTree with ItemInvoked / Expanding / Collapsed listeners that do not reference the component is also collected after being placed in the window and removed") {
            val reference = shownAndHidden {
                WTree().also { tree ->
                    tree.addRootNode(WTreeNode("Fruit").also { it.add(WTreeNode("Apple")) })
                    tree.addItemInvokedListener { }
                    tree.addExpandingListener { }
                    tree.addCollapsedListener { }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WItemsView with an ItemInvoked listener that does not reference the component is also collected after being placed in the window and removed") {
            val reference = shownAndHidden {
                WItemsView().also { view ->
                    view.setItems(listOf(WItemContainer(WLabel("First")), WItemContainer(WLabel("Second"))))
                    view.isItemInvokedEnabled = true
                    view.addItemInvokedListener { }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }
    }
}
