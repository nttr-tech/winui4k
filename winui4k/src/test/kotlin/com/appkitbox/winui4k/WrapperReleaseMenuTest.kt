package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import java.lang.ref.WeakReference

/**
 * Tests that menu item listeners (Click's ActionListener, WAction binding, ItemListener through the IsChecked change callback) do not create cycles
 * across the native boundary. The handler looks up the item through [ListenerAnchor], so releasing a menu that was opened and closed also collects its items.
 * Collection is awaited while prompting GC with [Reclamation].
 */
class WrapperReleaseMenuTest : FunSpec() {
    /** The sources of events received by listeners. The listeners do not reference the item (they only record here). */
    private val sources = mutableListOf<Any>()

    private fun isLoaded(component: WComponent): Boolean =
        onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }

    /**
     * Puts the item made by [create] into a new menu, opens and closes it from a button in the shared window, and removes the button too.
     * The test holds the menu and item only through weak references (a suspend function would leave strong references in lambdas and state while waiting, so this is a normal function).
     */
    private fun <T : WMenuFlyoutItem> openedAndClosed(
        configure: (WMenuFlyout) -> Unit = {},
        create: () -> T,
    ): Pair<WeakReference<WMenuFlyout>, WeakReference<T>> {
        val button = onUiThreadGet { WButton("Menu") }
        val (menu, item) = onUiThreadGet {
            val item = create()
            WMenuFlyout().also { it.add(item) }.also(configure) to item
        }
        UiTestHarness.attachAndAwaitLoaded(button)
        try {
            onUiThread { menu.showAt(button) }
            Reclamation.awaitWithGc { isLoaded(item) }.shouldBeTrue()
            onUiThread { menu.hide() }
            Reclamation.awaitWithGc { !isLoaded(item) }.shouldBeTrue()
            onUiThread { } // Wait for the Unloaded handlers to finish in the same cycle
        } finally {
            UiTestHarness.detach(button)
        }
        return WeakReference(menu) to WeakReference(item)
    }

    /** Loads a menu bar whose heading has a MenuListener, opens and closes the heading, and removes it. Returns the heading only as a weak reference. */
    private fun openedAndClosedMenuBar(): WeakReference<WMenuBarItem> {
        // The listener does not reference the heading (it only records in text whether the source is the heading)
        val events = mutableListOf<String>()
        val panel = onUiThreadGet { WPanel() }
        val file = onUiThreadGet {
            val bar = WMenuBar().also { panel.add(it) }
            bar.add(WMenuBarItem("File")).also { file ->
                file.add("Open")
                file.addMenuListener(object : WMenuAdapter() {
                    override fun menuSelected(e: WMenuEvent) {
                        events += "selected:" + (e.source is WMenuBarItem)
                    }

                    override fun menuDeselected(e: WMenuEvent) {
                        events += "deselected:" + (e.source is WMenuBarItem)
                    }
                })
            }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            onUiThread { Xaml.invoke(file.inspectable) }
            Reclamation.awaitWithGc { onUiThreadGet { events.toList() == listOf("selected:true") && isOpenMenuShown(panel) } }.shouldBeTrue()
            TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)
            Reclamation.awaitWithGc { onUiThreadGet { events.toList() == listOf("selected:true", "deselected:true") } }.shouldBeTrue()
        } finally {
            UiTestHarness.detach(panel)
        }
        return WeakReference(file)
    }

    /** Whether there is a popup (an open menu) in the window of [component]. */
    private fun isOpenMenuShown(component: WComponent): Boolean {
        val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val popups = try {
            statics.getPtr(XamlInterop.IVisualTreeHelperStatics_GetOpenPopupsForXamlRoot, root.ptr)
        } finally {
            statics.release()
            root.release()
        }
        return try {
            popups.getInt(FoundationInterop.IVectorView_get_Size) > 0
        } finally {
            popups.release()
        }
    }

    init {
        test("an item with an ActionListener that does not reference the item is collected once the menu is opened, closed and released") {
            val recorded = sources
            val (menu, item) = openedAndClosed {
                WMenuFlyoutItem("Open").also { it.addActionListener { e -> recorded += e.source } }
            }
            Reclamation.awaitCollected(menu).shouldBeTrue()
            Reclamation.awaitCollected(item).shouldBeTrue()
        }

        test("items of a menu never opened are collected once all four forms of ActionListener are removed") {
            val reference = onUiThreadGet {
                val item = WMenuFlyoutItem("Remove")
                WMenuFlyout().add(item)
                val lambda: (WActionEvent) -> Unit = { }
                val runnable = Runnable { }
                val listener = WActionListener { }
                item.addActionListener(lambda)
                item.addActionListenerForJava(runnable)
                item.addActionListenerForJava(listener)
                item.removeActionListener(lambda)
                item.removeActionListenerForJava(runnable)
                item.removeActionListenerForJava(listener)
                WeakReference(item)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("an item with an ActionListener is not collected until it is removed even with no references anywhere, and the doClick source is the item") {
            val recorded = sources
            val listener: (WActionEvent) -> Unit = { e -> recorded += e.source }
            val item = onUiThreadGet { WeakReference(WMenuFlyoutItem("Open").also { it.addActionListener(listener) }) }
            Reclamation.survivesGc(item).shouldBeTrue()
            onUiThreadGet {
                sources.clear()
                val target = checkNotNull(item.get())
                target.doClick()
                sources.map { if (it === target) "item" else it.javaClass.name }.also { sources.clear() }
            } shouldContainExactly listOf("item")
            onUiThread { item.get()?.removeActionListener(listener) }
            Reclamation.awaitCollected(item).shouldBeTrue()
        }

        test("an item bound to a shared WAction is collected once the menu is opened, closed and released even while the action remains") {
            val action = object : WAbstractAction("Shared") {
                override fun actionPerformed(e: WActionEvent) = Unit
            }
            val (menu, item) = openedAndClosed { WMenuFlyoutItem(action) }
            Reclamation.awaitCollected(menu).shouldBeTrue()
            Reclamation.awaitCollected(item).shouldBeTrue()
            onUiThread { action.putValue(WAction.NAME, "After collection") } // Notifications to the collected item are dropped
        }

        test("check and radio items with an ItemListener that does not reference the item are collected once the menu is opened, closed and released") {
            val recorded = sources
            val (toggleMenu, toggle) = openedAndClosed {
                WToggleMenuFlyoutItem("Bold").also { it.addItemListener { e -> recorded += e.source } }
            }
            val (radioMenu, radio) = openedAndClosed {
                WRadioMenuFlyoutItem("Small").also { it.addItemListener { e -> recorded += e.source } }
            }
            Reclamation.awaitCollected(toggleMenu).shouldBeTrue()
            Reclamation.awaitCollected(toggle).shouldBeTrue()
            Reclamation.awaitCollected(radioMenu).shouldBeTrue()
            Reclamation.awaitCollected(radio).shouldBeTrue()
        }

        test("check items of a menu never opened are collected once the ItemListener is removed and they are removed from the WButtonGroup") {
            val reference = onUiThreadGet {
                val item = WToggleMenuFlyoutItem("Remove")
                val listener = WItemListener { }
                val group = WButtonGroup()
                item.addItemListener(listener)
                group.add(item)
                item.isSelected = true
                item.removeItemListener(listener)
                group.remove(item)
                WeakReference(item)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a radio item with an ItemListener is not collected until it is removed even with no references anywhere, and the ItemEvent source is the item") {
            val recorded = sources
            val listener = WItemListener { e -> recorded += e.source }
            val item = onUiThreadGet { WeakReference(WRadioMenuFlyoutItem("Small").also { it.addItemListener(listener) }) }
            Reclamation.survivesGc(item).shouldBeTrue()
            onUiThreadGet {
                sources.clear()
                val target = checkNotNull(item.get())
                target.isSelected = true
                sources.map { if (it === target) "item" else it.javaClass.name }.also { sources.clear() }
            } shouldContainExactly listOf("item")
            onUiThread { item.get()?.removeItemListener(listener) }
            Reclamation.awaitCollected(item).shouldBeTrue()
        }

        test("a menu with a PopupMenuListener that does not reference the menu is collected together with its items once opened, closed and released") {
            val recorded = sources
            val (menu, item) = openedAndClosed(
                configure = { menu ->
                    menu.addPopupMenuListener(object : WPopupMenuAdapter() {
                        override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                            recorded += "invisible" // The source (the menu) is not recorded (recording it would prevent collection)
                        }
                    })
                },
            ) { WMenuFlyoutItem("Open") }
            Reclamation.awaitCollected(menu).shouldBeTrue()
            Reclamation.awaitCollected(item).shouldBeTrue()
        }

        test("a menu bar heading with a MenuListener is also collected once loaded, opened, closed, removed and released") {
            val reference = openedAndClosedMenuBar()
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
    }
}
