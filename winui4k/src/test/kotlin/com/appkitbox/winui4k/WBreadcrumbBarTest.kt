package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests WBreadcrumbBar's levels and the click ActionListener.
 * Clicks are made with UI Automation peers (BreadcrumbBarItem's Invoke) ([NavigationClicks]).
 */
class WBreadcrumbBarTest : FunSpec() {
    /** Places [bar] in the shared window, waits for Loaded, calls [block] on the UI thread, and then removes it. */
    private fun <T> withShown(bar: WBreadcrumbBar, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(bar)
        try {
            onUiThread { bar.updateLayout() } // Make it create the level elements (BreadcrumbBarItem)
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(bar)
        }
    }

    init {
        test("items returns exactly the hierarchy that was set via setItems") {
            onUiThreadGet {
                val breadcrumbBar = WBreadcrumbBar()
                breadcrumbBar.setItems(listOf("C:", "Users", "hisano"))
                breadcrumbBar.items
            } shouldBe listOf("C:", "Users", "hisano")
        }

        test("calling setItems again replaces the hierarchy, and items reflects the replacement") {
            onUiThreadGet {
                val breadcrumbBar = WBreadcrumbBar()
                breadcrumbBar.setItems(listOf("C:", "Users"))
                breadcrumbBar.setItems(listOf("D:"))
                breadcrumbBar.items
            } shouldBe listOf("D:")
        }

        test("clicking a level calls the ActionListener with the label as actionCommand and the breadcrumb bar as source, and the index arrives through ItemClicked") {
            val bar = onUiThreadGet { WBreadcrumbBar().also { it.setItems(listOf("C:", "Users", "hisano")) } }
            val (events, indexes) = withShown(bar) {
                val events = mutableListOf<Pair<Any, String?>>()
                val indexes = mutableListOf<Int>()
                bar.addActionListener { e -> events += e.source to e.actionCommand }
                bar.addItemClickedListener { indexes += it }
                NavigationClicks.clickBreadcrumbItem(bar, "Users")
                NavigationClicks.clickBreadcrumbItem(bar, "C:")
                events to indexes
            }
            events shouldBe listOf(bar to "Users", bar to "C:")
            indexes shouldBe listOf(1, 0)
        }

        test("a listener removed with removeActionListener is not called and disappears from getActionListeners") {
            val bar = onUiThreadGet { WBreadcrumbBar().also { it.setItems(listOf("C:", "Users")) } }
            val result = withShown(bar) {
                val calls = mutableListOf<String?>()
                val kept = WActionListener { e -> calls += "kept:${e.actionCommand}" }
                val removed = WActionListener { e -> calls += "removed:${e.actionCommand}" }
                bar.addActionListener(kept)
                bar.addActionListener(removed)
                bar.removeActionListener(removed)
                NavigationClicks.clickBreadcrumbItem(bar, "C:")
                calls to bar.getActionListeners().toList()
            }
            result.first shouldBe listOf("kept:C:")
            result.second.size shouldBe 1
        }
    }
}
