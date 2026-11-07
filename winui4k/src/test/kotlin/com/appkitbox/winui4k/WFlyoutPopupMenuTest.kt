package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the JPopupMenu-like API (show(invoker, x, y), isVisible, invoker, PopupMenuListener), JMenu's MenuListener and
 * JComponent.componentPopupMenu, by actually opening menus in a window. Expected values come from Swing's behavior
 * (cancellation is canceled → willBecomeInvisible, setVisible(false) is not a cancellation, getInvoker is the reference element of the display).
 * The shape seen from Java is tested in WFlyoutPopupMenuForJavaTest.
 */
@Tags("Integration")
class WFlyoutPopupMenuTest : FunSpec() {
    /** A listener that records "visible" / "canceled" / "invisible" in the order received, and whether the source is [expected]. */
    private fun recorder(expected: Any, events: MutableList<String>) = object : WPopupMenuListener {
        override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
            events += "visible:${e.source === expected}"
        }

        override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
            events += "invisible:${e.source === expected}"
        }

        override fun popupMenuCanceled(e: WPopupMenuEvent) {
            events += "canceled:${e.source === expected}"
        }
    }

    /** The position (HorizontalOffset, VerticalOffset) of the first Popup open in the window of [owner]. null if none. Call on the UI thread. */
    private fun openPopupOffset(owner: WComponent): Pair<Double, Double>? {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val popups = try {
            statics.getPtr(XamlInterop.IVisualTreeHelperStatics_GetOpenPopupsForXamlRoot, root.ptr)
        } finally {
            statics.release()
            root.release()
        }
        return try {
            if (popups.getInt(FoundationInterop.IVectorView_get_Size) == 0) return null
            val popup: ComPtr = popups.getPtr(FoundationInterop.IVectorView_GetAt, 0)
            try {
                popup.getDouble(XamlInterop.IPopup_get_HorizontalOffset) to popup.getDouble(XamlInterop.IPopup_get_VerticalOffset)
            } finally {
                popup.release()
            }
        } finally {
            popups.release()
        }
    }

    private fun pressEscape() = TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)

    /** Runs [block] with a [WPanel] put in the shared window (with one button at the top-left), and removes it at the end. */
    private suspend fun withPanel(block: suspend (panel: WPanel, button: WButton) -> Unit) {
        val panel = onUiThreadGet { WPanel() }
        val button = onUiThreadGet { WButton("Anchor").also { panel.add(it) } }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            block(panel, button)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    private suspend fun awaitOpen(flyout: WFlyoutBase, open: Boolean) {
        eventually(5.seconds) { onUiThreadGet { flyout.isOpen } shouldBe open }
    }

    init {
        test("show(invoker, x, y) opens at the position in the invoker's coordinates, and returns isVisible and invoker") {
            withPanel { _, button ->
                val menu = onUiThreadGet { WMenuFlyout().also { it.add("Open") } }
                onUiThread { menu.show(button, 10.0, 20.0) }
                awaitOpen(menu, true)
                val first = onUiThreadGet { listOf(menu.isVisible, menu.invoker === button) to openPopupOffset(button)!! }
                onUiThread { menu.hide() }
                awaitOpen(menu, false)
                onUiThread { menu.show(button, 60.0, 50.0) }
                awaitOpen(menu, true)
                val second = onUiThreadGet { openPopupOffset(button)!! }
                val origin = onUiThreadGet { Xaml.positionInRoot(button.uiElement) }
                onUiThread { menu.isVisible = false }
                awaitOpen(menu, false)

                first.first shouldContainExactly listOf(true, true)
                // The difference between the two positions equals the difference in x / y (both positions fit in the window, so they are not shifted)
                (second.first - first.second.first) shouldBe (50.0 plusOrMinus 0.5)
                (second.second - first.second.second) shouldBe (30.0 plusOrMinus 0.5)
                // The position itself is near the button's top-left + (x, y) (WinUI may shift it by the menu's shadow)
                first.second.first shouldBe ((origin[0] + 10.0) plusOrMinus 16.0)
                first.second.second shouldBe ((origin[1] + 20.0) plusOrMinus 16.0)
                onUiThreadGet { menu.isVisible }.shouldBeFalse()
            }
        }

        test("in Kotlin too, show can be called with Int coordinates (the x / y of a mouse event), as with JPopupMenu.show(c, int, int), and opens at the same position as with Double") {
            withPanel { _, button ->
                val menu = onUiThreadGet { WMenuFlyout().also { it.add("Open") } }
                onUiThread { menu.show(button, 10.0, 20.0) }
                awaitOpen(menu, true)
                val withDouble = onUiThreadGet { openPopupOffset(button)!! }
                onUiThread { menu.hide() }
                awaitOpen(menu, false)
                val x: Int = 10
                val y: Int = 20
                onUiThread { menu.show(button, x, y) }
                awaitOpen(menu, true)
                val withInt = onUiThreadGet { openPopupOffset(button)!! }
                onUiThread { menu.hide() }
                awaitOpen(menu, false)

                withInt.first shouldBe (withDouble.first plusOrMinus 0.5)
                withInt.second shouldBe (withDouble.second plusOrMinus 0.5)
            }
        }

        test("closing with Esc calls willBecomeVisible → canceled → willBecomeInvisible in that order, in the order added (as in Swing)") {
            withPanel { _, button ->
                val events = mutableListOf<String>()
                val menu = onUiThreadGet {
                    WMenuFlyout().also {
                        it.add("Open")
                        it.addPopupMenuListener(recorder(it, events))
                        it.addPopupMenuListener(object : WPopupMenuAdapter() {
                            override fun popupMenuCanceled(e: WPopupMenuEvent) {
                                events += "second:canceled"
                            }
                        })
                    }
                }
                onUiThread { menu.show(button, 0.0, 0.0) }
                awaitOpen(menu, true)
                onUiThreadGet { events.toList() } shouldContainExactly listOf("visible:true")
                pressEscape()
                awaitOpen(menu, false)
                onUiThreadGet { events.toList() } shouldContainExactly
                    listOf("visible:true", "canceled:true", "second:canceled", "invisible:true")
            }
        }

        test("closing with hide and isVisible = false does not call canceled (same as Swing's setVisible(false))") {
            withPanel { _, button ->
                val events = mutableListOf<String>()
                val menu = onUiThreadGet { WMenuFlyout().also { it.add("Open") } }
                onUiThread { menu.addPopupMenuListener(recorder(menu, events)) }
                onUiThread { menu.showAt(button) }
                awaitOpen(menu, true)
                onUiThread { menu.hide() }
                awaitOpen(menu, false)
                onUiThread { menu.isVisible = true } // Reopen relative to the last invoker (button)
                awaitOpen(menu, true)
                onUiThread { menu.isVisible = false }
                awaitOpen(menu, false)
                onUiThreadGet { events.toList() } shouldContainExactly
                    listOf("visible:true", "invisible:true", "visible:true", "invisible:true")
            }
        }

        test("closing by choosing an item does not call canceled, and in WinUI's order the item's ActionListener comes before willBecomeInvisible") {
            withPanel { _, button ->
                val events = mutableListOf<String>()
                val sub = onUiThreadGet { WMenuFlyoutSubItem("Sub") }
                val (menu, item) = onUiThreadGet {
                    val menu = WMenuFlyout()
                    val item = menu.add("Save").also { it.addActionListener { events += "action" } }
                    menu.add(sub).add("Inner")
                    menu.addPopupMenuListener(recorder(menu, events))
                    menu to item
                }
                onUiThread { menu.show(button, 5.0, 5.0) }
                awaitOpen(menu, true)
                eventually(5.seconds) { onUiThreadGet { item.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue() }
                onUiThread { item.doClick() }
                awaitOpen(menu, false)
                // Opening next time and closing with Esc is a cancellation this time (the previous item choice is not carried over)
                onUiThread { menu.show(button, 5.0, 5.0) }
                awaitOpen(menu, true)
                pressEscape()
                awaitOpen(menu, false)
                onUiThreadGet { events.toList() } shouldContainExactly listOf(
                    "visible:true",
                    "action",
                    "invisible:true",
                    "visible:true",
                    "canceled:true",
                    "invisible:true",
                )
            }
        }

        test("closing a WCommandBarFlyout by pressing a button or toggle in its lower menu does not call canceled (the same as choosing an item)") {
            withPanel { _, button ->
                val events = mutableListOf<String>()
                val (flyout, save, bold) = onUiThreadGet {
                    val flyout = WCommandBarFlyout()
                    val save = WAppBarButton("Save").also { flyout.addSecondaryCommand(it) }
                    val bold = WAppBarToggleButton("Bold").also { flyout.addSecondaryCommand(it) }
                    flyout.addPopupMenuListener(recorder(flyout, events))
                    Triple(flyout, save, bold)
                }
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                onUiThread { save.doClick() }
                awaitOpen(flyout, false)
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                onUiThread { bold.doClick() }
                awaitOpen(flyout, false)
                onUiThreadGet { events.toList() } shouldContainExactly
                    listOf("visible:true", "invisible:true", "visible:true", "invisible:true")
            }
        }

        test("WCommandBarFlyout's mini toolbar buttons do not close it when pressed, so a following Esc is a cancellation") {
            withPanel { _, button ->
                val events = mutableListOf<String>()
                val (flyout, copy) = onUiThreadGet {
                    val flyout = WCommandBarFlyout()
                    val copy = WAppBarButton("Copy").also { flyout.addPrimaryCommand(it) }
                    flyout.addSecondaryCommand(WAppBarButton("Save"))
                    flyout.addPopupMenuListener(recorder(flyout, events))
                    flyout to copy
                }
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                onUiThread { copy.doClick() }
                onUiThreadGet { flyout.isOpen }.shouldBeTrue()
                pressEscape()
                awaitOpen(flyout, false)
                onUiThreadGet { events.toList() } shouldContainExactly listOf("visible:true", "canceled:true", "invisible:true")
            }
        }

        test("a listener removed with removePopupMenuListener is not called, and popupMenuListeners are in the order added") {
            withPanel { _, button ->
                val events = mutableListOf<String>()
                val menu = onUiThreadGet { WFlyout(WLabel("Content")) }
                val first = recorder(menu, events)
                val second = object : WPopupMenuAdapter() {}
                onUiThreadGet {
                    menu.addPopupMenuListener(first)
                    menu.addPopupMenuListener(second)
                    menu.popupMenuListeners.toList()
                } shouldContainExactly listOf<WPopupMenuListener>(first, second)
                onUiThread { menu.showAt(button) }
                awaitOpen(menu, true)
                onUiThread { menu.removePopupMenuListener(first) }
                pressEscape()
                awaitOpen(menu, false)
                onUiThreadGet { events.toList() } shouldContainExactly listOf("visible:true")
                onUiThreadGet { menu.popupMenuListeners.toList() } shouldContainExactly listOf<WPopupMenuListener>(second)
            }
        }

        test("the invoker of a menu opened as a button's flyout is that button, and the invoker before opening is null") {
            withPanel { _, button ->
                val menu = onUiThreadGet { WMenuFlyout().also { it.add("Open") } }
                onUiThreadGet {
                    button.flyout = menu
                    menu.invoker
                }.shouldBeNull()
                onUiThread { button.doClick() }
                awaitOpen(menu, true)
                onUiThreadGet { menu.invoker === button }.shouldBeTrue()
                onUiThread { menu.hide() }
                awaitOpen(menu, false)
            }
        }

        test("setting isVisible = true without an invoker throws IllegalStateException") {
            onUiThreadGet {
                runCatching { WMenuFlyout().isVisible = true }.exceptionOrNull()?.javaClass
            } shouldBe IllegalStateException::class.java
        }

        test("componentPopupMenu is contextFlyout seen as a WMenuFlyout (null for other flyouts)") {
            onUiThreadGet {
                val label = WLabel("Right click")
                val menu = WMenuFlyout()
                label.componentPopupMenu = menu
                val asMenu = listOf(label.contextFlyout === menu, label.componentPopupMenu === menu)
                label.contextFlyout = WFlyout()
                val other = label.componentPopupMenu
                label.componentPopupMenu = null
                asMenu + listOf(other, label.contextFlyout)
            } shouldContainExactly listOf(true, true, null, null)
        }

        test("opening a menu set as componentPopupMenu with showAt makes that element the invoker") {
            withPanel { panel, button ->
                val menu = onUiThreadGet { WMenuFlyout().also { it.add("Cut") } }
                onUiThread { panel.componentPopupMenu = menu }
                onUiThread { menu.showAt(button) }
                awaitOpen(menu, true)
                onUiThreadGet { menu.invoker === button }.shouldBeTrue()
                onUiThread { menu.hide() }
                awaitOpen(menu, false)
            }
        }

        test("a WMenuBarItem's MenuListener calls menuSelected when opened and menuDeselected when closed, and never menuCanceled") {
            val events = mutableListOf<String>()
            val panel = onUiThreadGet { WPanel() }
            val (file, item) = onUiThreadGet {
                val bar = WMenuBar().also { panel.add(it) }
                val file = bar.add(WMenuBarItem("File"))
                val item = file.add("Open")
                file.addMenuListener(object : WMenuListener {
                    override fun menuSelected(e: WMenuEvent) {
                        events += "selected:${e.source === file}"
                    }

                    override fun menuDeselected(e: WMenuEvent) {
                        events += "deselected:${e.source === file}"
                    }

                    override fun menuCanceled(e: WMenuEvent) {
                        events += "canceled"
                    }
                })
                file to item
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                // Listeners added after it is loaded are called too
                onUiThread {
                    file.addMenuListener(object : WMenuAdapter() {
                        override fun menuSelected(e: WMenuEvent) {
                            events += "late:selected"
                        }
                    })
                }
                onUiThread { Xaml.invoke(file.inspectable) }
                eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("selected:true", "late:selected") }
                eventually(5.seconds) { onUiThreadGet { item.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue() }
                pressEscape()
                eventually(5.seconds) {
                    onUiThreadGet { events.toList() } shouldContainExactly listOf("selected:true", "late:selected", "deselected:true")
                }
                onUiThreadGet { file.menuListeners.size } shouldBe 2
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
    }
}
