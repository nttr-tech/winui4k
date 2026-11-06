package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests WNavigationView's selection ChangeListener and WNavigationViewItem's ActionListener and icon.
 * Clicks are made with UI Automation peers ([NavigationClicks]). The expected values are based on the behavior of Swing's ChangeListener (once, only when it changes) and
 * JMenuItem's ActionListener (once per press; the actionCommand defaults to the label).
 */
class WNavigationViewTest : FunSpec() {
    /** A navigation view with its pane always open as a left menu, with the item containers created. */
    private fun navigationView(vararg items: WNavigationViewItem): WNavigationView = WNavigationView().also { view ->
        view.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
        items.forEach(view::addItem)
    }

    /** Places [view] in the shared window, waits for Loaded, calls [block] on the UI thread, and then removes it. */
    private fun <T> withShown(view: WNavigationView, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(view)
        try {
            onUiThread { view.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(view)
        }
    }

    /** The kind of [item]'s native icon ("none" / "symbol" / "font" / "image"). */
    private fun nativeIconKind(item: WNavigationViewItem): String {
        val icon = item.inspectable.getPtrOrNull(XamlInterop.INavigationViewItem_get_Icon) ?: return "none"
        try {
            val kinds = listOf("symbol" to XamlInterop.IID_ISymbolIcon, "font" to XamlInterop.IID_IFontIcon, "image" to XamlInterop.IID_IImageIcon)
            for ((kind, iid) in kinds) {
                val view = icon.queryInterfaceOrNull(iid) ?: continue
                view.release()
                return kind
            }
            return "other"
        } finally {
            icon.release()
        }
    }

    init {
        test("setting selectedItem calls ChangeListener, and setting the same item again does not (before being placed in the window)") {
            val notified = onUiThreadGet {
                val a = WNavigationViewItem("A")
                val b = WNavigationViewItem("B")
                val view = navigationView(a, b)
                val notified = mutableListOf<String?>()
                view.addChangeListener { e -> notified += (e.source as WNavigationView).selectedItem?.text }
                view.selectedItem = a
                view.selectedItem = a
                view.selectedItem = b
                view.selectedItem = null
                notified
            }
            notified shouldBe listOf("A", "B", null)
        }

        test("when the user clicks an item ChangeListener is called once, and clicking the already selected item does not call it") {
            val a = onUiThreadGet { WNavigationViewItem("A") }
            val b = onUiThreadGet { WNavigationViewItem("B") }
            val view = onUiThreadGet { navigationView(a, b) }
            val result = withShown(view) {
                val notified = mutableListOf<String?>()
                val selections = mutableListOf<String?>()
                view.addChangeListener { notified += view.selectedItem?.text }
                view.addSelectionListener { item -> selections += item?.text }
                NavigationClicks.clickNavigationItem(b)
                NavigationClicks.clickNavigationItem(b)
                NavigationClicks.clickNavigationItem(a)
                notified to selections
            }
            result shouldBe (listOf("B", "A") to listOf("B", "A"))
        }

        test("a listener removed with removeChangeListener is not called") {
            val notified = onUiThreadGet {
                val a = WNavigationViewItem("A")
                val view = navigationView(a)
                val notified = mutableListOf<String?>()
                val listener = WChangeListener { notified += "called" }
                view.addChangeListener(listener)
                view.removeChangeListener(listener)
                view.selectedItem = a
                notified + view.getChangeListeners().size.toString()
            }
            notified shouldBe listOf("0")
        }

        test("an item's ActionListener is called on every click of that item, with the label as the default actionCommand and the item as the source") {
            val a = onUiThreadGet { WNavigationViewItem("A") }
            val b = onUiThreadGet { WNavigationViewItem("B").also { it.actionCommand = "open-b" } }
            val view = onUiThreadGet { navigationView(a, b) }
            val events = withShown(view) {
                val events = mutableListOf<Pair<Any, String?>>()
                a.addActionListener { e -> events += e.source to e.actionCommand }
                b.addActionListener { e -> events += e.source to e.actionCommand }
                NavigationClicks.clickNavigationItem(a)
                NavigationClicks.clickNavigationItem(a) // Called on a press even if already selected (same as ItemInvoked)
                NavigationClicks.clickNavigationItem(b)
                events
            }
            events shouldBe listOf(a to "A", a to "A", b to "open-b")
        }

        test("clicking a child item notifies only the child item's ActionListener, and removeActionListener removes it") {
            val parent = onUiThreadGet { WNavigationViewItem("Parent") }
            val child = onUiThreadGet { WNavigationViewItem("Child").also(parent::addItem) }
            val view = onUiThreadGet { navigationView(parent) }
            val calls = withShown(view) {
                val calls = mutableListOf<String>()
                parent.addActionListener { calls += "parent" }
                val childListener = WActionListener { calls += "child" }
                child.addActionListener(childListener)
                parent.isExpanded = true
                view.updateLayout()
                NavigationClicks.clickNavigationItem(child)
                child.removeActionListener(childListener)
                NavigationClicks.clickNavigationItem(child)
                calls + child.getActionListeners().size.toString()
            }
            calls shouldBe listOf("child", "0")
        }

        test("icon accepts Symbol, WFontIcon and WImageIcon, returns them as set, and null removes it") {
            val result = onUiThreadGet {
                val item = WNavigationViewItem("A", Symbol.HOME)
                val kinds = mutableListOf(nativeIconKind(item))
                val font = WFontIcon("X")
                item.icon = font
                kinds += nativeIconKind(item)
                val fontReadBack = item.icon
                item.icon = WImageIcon("icons/home.png")
                kinds += nativeIconKind(item)
                item.icon = null
                kinds += nativeIconKind(item)
                kinds to (fontReadBack == font && item.icon == null)
            }
            result shouldBe (listOf("symbol", "font", "image", "none") to true)
        }
    }
}
