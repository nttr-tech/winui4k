package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

/** Tests that check the default values of WTabView and WTabViewItem (the "+" button and close buttons) match JTabbedPane. */
@Tags("E2E")
class WTabViewDefaultsTest : FunSpec() {
    init {
        test("the \"+\" button is not shown by default, as with JTabbedPane, and appears when a + listener is added (the native IsAddTabButtonVisible too)") {
            onUiThreadGet {
                val tabView = WTabView()
                val initial = tabView.isAddTabButtonVisible to tabView.inspectable.getBool(XamlInterop.ITabView_get_IsAddTabButtonVisible)
                tabView.addAddTabButtonClickListener {}
                initial to tabView.isAddTabButtonVisible
            } shouldBe ((false to false) to true)
        }

        test("a tab view whose \"+\" button was hidden explicitly keeps it hidden even when a + listener is added") {
            onUiThreadGet {
                val tabView = WTabView()
                tabView.isAddTabButtonVisible = false
                tabView.addAddTabButtonClickListener {}
                tabView.isAddTabButtonVisible
            }.shouldBeFalse()
        }

        test("tabs of new WTabViewItem(title) also have no close button, as with addTab(title, component) and JTabbedPane. Setting true shows it") {
            onUiThreadGet {
                val tabView = WTabView()
                val item = WTabViewItem("a")
                tabView.addTab(item)
                tabView.addTab("b", WLabel("b"))
                val defaults = listOf(item.isClosable, tabView.getTab(1).isClosable)
                item.isClosable = true
                defaults to item.isClosable
            } shouldBe (listOf(false, false) to true)
        }
    }
}
