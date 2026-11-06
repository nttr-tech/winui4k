package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests that check, through the native state, menu-related parts that other units do not look at closely (WMenuBarItem's title, WMenuFlyoutSeparator,
 * WCommandBarFlyout's primary / secondary, WAppBarSeparator). The shape seen from Java is tested in WMenuBarPartsForJavaTest.
 */
@Tags("E2E")
class WMenuBarPartsTest : FunSpec() {
    /** Represents the command list of [slot] of a CommandBarFlyout as a sequence of "button" / "other" (an AppBarButton or anything else). */
    private fun nativeCommands(flyout: WCommandBarFlyout, slot: Int): List<String> {
        val observable = flyout.inspectable.getPtr(slot)
        val vector = observable.queryInterface(XamlInterop.IID_IVector_ICommandBarElement)
        observable.release()
        try {
            return (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index -> describe(vector, index) }
        } finally {
            vector.release()
        }
    }

    private fun describe(vector: ComPtr, index: Int): String {
        val element = vector.getPtr(FoundationInterop.IVector_GetAt, index)
        try {
            val button = element.queryInterfaceOrNull(XamlInterop.IID_IAppBarButton)
            button?.release()
            return if (button != null) "button" else "other"
        } finally {
            element.release()
        }
    }

    init {
        test("WMenuBarItem's title gets the constructor value, and rewriting it and resetting it to an empty string reach the native Title") {
            onUiThreadGet {
                val item = WMenuBarItem("File")
                val first = item.title
                item.title = "Edit"
                val second = item.title
                item.title = ""
                listOf(first, second, item.title, WMenuBarItem().title)
            } shouldContainExactly listOf("File", "Edit", "", "")
        }

        test("WMenuBarItems added to a WMenuBar are in the order added, and each keeps its title") {
            onUiThreadGet {
                val bar = WMenuBar()
                bar.add(WMenuBarItem("File"))
                bar.add(WMenuBarItem("Edit"))
                bar.add(WMenuBarItem("Help"))
                (0 until bar.menuCount).map { bar.getMenu(it).title }
            } shouldContainExactly listOf("File", "Edit", "Help")
        }

        test("WMenuFlyoutSeparator is not a WMenuFlyoutItem, so getItem returns null as in Swing, but it is counted in the number of items") {
            onUiThreadGet {
                val item = WMenuBarItem("File")
                item.add("Open")
                item.addSeparator()
                item.add("Close")
                listOf(item.itemCount, item.getItem(1), item.getMenuComponent(1) is WMenuFlyoutSeparator)
            } shouldContainExactly listOf(3, null, true)
        }

        test("WCommandBarFlyout's primary and secondary go into separate lists in the order added") {
            onUiThreadGet {
                val flyout = WCommandBarFlyout()
                flyout.addPrimaryCommand(WAppBarButton("Copy", Symbol.COPY))
                flyout.addPrimaryCommand(WAppBarSeparator())
                flyout.addPrimaryCommand(WAppBarButton("Paste", Symbol.PASTE))
                flyout.addSecondaryCommand(WAppBarButton("Delete", Symbol.DELETE))
                Pair(
                    nativeCommands(flyout, XamlInterop.ICommandBarFlyout_get_PrimaryCommands),
                    nativeCommands(flyout, XamlInterop.ICommandBarFlyout_get_SecondaryCommands),
                )
            } shouldBe Pair(listOf("button", "other", "button"), listOf("button"))
        }

        test("WCommandBarFlyout's primary and secondary are both empty when nothing is added") {
            onUiThreadGet {
                val flyout = WCommandBarFlyout()
                Pair(
                    nativeCommands(flyout, XamlInterop.ICommandBarFlyout_get_PrimaryCommands),
                    nativeCommands(flyout, XamlInterop.ICommandBarFlyout_get_SecondaryCommands),
                )
            } shouldBe Pair(emptyList(), emptyList())
        }

        test("WAppBarSeparator is an ICommandBarElement that is not an AppBarButton, and can also be added to secondary") {
            onUiThreadGet {
                val flyout = WCommandBarFlyout()
                flyout.addSecondaryCommand(WAppBarSeparator())
                nativeCommands(flyout, XamlInterop.ICommandBarFlyout_get_SecondaryCommands)
            } shouldContainExactly listOf("other")
        }
    }
}
