package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/** Tests verifying WTabView's tab management, selection, and properties, plus WTabViewItem's header updates. */
class WTabViewTest : FunSpec() {
    init {
        test("tabs added via addTab can be retrieved from tabCount and getTab") {
            val (count, header) = onUiThreadGet {
                val tabView = WTabView()
                tabView.addTab(WTabViewItem("Tab 1"))
                tabView.addTab(WTabViewItem("Tab 2"))
                tabView.tabCount to tabView.getTab(1).header
            }
            count shouldBe 2
            header shouldBe "Tab 2"
        }

        test("header returns exactly the value that was set") {
            onUiThreadGet {
                val tab = WTabViewItem("old label")
                tab.header = "new label"
                tab.header
            } shouldBe "new label"
        }

        test("selectedIndex returns exactly the value that was set") {
            onUiThreadGet {
                val tabView = WTabView()
                repeat(3) { tabView.addTab(WTabViewItem("Tab $it")) }
                tabView.selectedIndex = 2
                tabView.selectedIndex
            } shouldBe 2
        }

        test("removeTab decreases tabCount and shifts the later tabs down") {
            val (count, header) = onUiThreadGet {
                val tabView = WTabView()
                repeat(3) { tabView.addTab(WTabViewItem("Tab $it")) }
                tabView.removeTab(1)
                tabView.tabCount to tabView.getTab(1).header
            }
            count shouldBe 2
            header shouldBe "Tab 2"
        }

        test("a tab added via addTab after Loaded is reflected in the display (the tab strip's width grows)") {
            // If adding to TabItems (the model) isn't reflected in rendering, SIZE_TO_CONTENT's width won't change
            val tabView = onUiThreadGet {
                val tabView = WTabView()
                // Make the width content-dependent so a change in tab count can be observed via actualWidth
                tabView.horizontalAlignment = HorizontalAlignment.LEFT
                tabView.tabWidthMode = TabViewWidthMode.SIZE_TO_CONTENT
                tabView.addTab(WTabViewItem("Tab 1"))
                tabView.selectedIndex = 0
                tabView
            }
            UiTestHarness.attachAndAwaitLoaded(tabView)
            try {
                val (before, after) = onUiThreadGet {
                    tabView.updateLayout()
                    val before = tabView.actualWidth
                    tabView.addTab(WTabViewItem("Tab 2"))
                    tabView.updateLayout()
                    before to tabView.actualWidth
                }
                withClue("The tab strip's width after adding (before=$before after=$after)") {
                    (after > before).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(tabView)
            }
        }

        test("isClosable=false hides the close button (the tab's width shrinks)") {
            val tabView = onUiThreadGet {
                val tabView = WTabView()
                // Make the width content-dependent so whether the close button is shown can be observed via actualWidth
                tabView.horizontalAlignment = HorizontalAlignment.LEFT
                tabView.tabWidthMode = TabViewWidthMode.SIZE_TO_CONTENT
                tabView.addTab(WTabViewItem("Tab 1"))
                tabView.selectedIndex = 0
                tabView
            }
            UiTestHarness.attachAndAwaitLoaded(tabView)
            try {
                val (before, after) = onUiThreadGet {
                    tabView.updateLayout()
                    val before = tabView.actualWidth
                    tabView.getTab(0).isClosable = false
                    tabView.updateLayout()
                    before to tabView.actualWidth
                }
                withClue("The tab strip's width after hiding the close button (before=$before after=$after)") {
                    (after < before).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(tabView)
            }
        }

        test("the SelectionChanged listener fires on every change to selectedIndex") {
            // SelectionChanged only fires once the internal ListView's template has been applied,
            // so attach it to a shared window and wait for Loaded before switching the selection
            var count = 0
            val tabView = onUiThreadGet {
                val tabView = WTabView()
                repeat(3) { tabView.addTab(WTabViewItem("Tab $it")) }
                tabView.selectedIndex = 0
                tabView
            }
            UiTestHarness.attachAndAwaitLoaded(tabView)
            try {
                onUiThread {
                    tabView.addSelectionListener { count++ }
                    tabView.selectedIndex = 1
                    tabView.selectedIndex = 2
                }
            } finally {
                UiTestHarness.detach(tabView)
            }
            count shouldBe 2
        }

        test("isAddTabButtonVisible and canReorderTabs return exactly the values that were set") {
            val (addVisible, reorder) = onUiThreadGet {
                val tabView = WTabView()
                tabView.isAddTabButtonVisible = false
                tabView.canReorderTabs = false
                tabView.isAddTabButtonVisible to tabView.canReorderTabs
            }
            addVisible.shouldBeFalse()
            reorder.shouldBeFalse()
        }
    }
}
