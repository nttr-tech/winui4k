package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import java.lang.ref.WeakReference

/**
 * Tests that menu item listeners (Click's ActionListener, WAction binding, check ItemListener) do not create cycles
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
    private fun <T : WMenuFlyoutItem> openedAndClosed(create: () -> T): Pair<WeakReference<WMenuFlyout>, WeakReference<T>> {
        val button = onUiThreadGet { WButton("Menu") }
        val (menu, item) = onUiThreadGet {
            val item = create()
            WMenuFlyout().also { it.add(item) } to item
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
    }
}
