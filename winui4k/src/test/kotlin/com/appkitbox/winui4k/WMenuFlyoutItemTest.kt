package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the Swing-shaped constructors, icon, ActionListener, actionCommand, mnemonic, accelerator, doClick and action of the
 * JMenuItem-like WMenuFlyoutItem (and the JMenu-like WMenuFlyoutSubItem).
 * Expected values come from Swing's JMenuItem behavior (the source is the item, the action command defaults to the text, doClick calls nothing
 * when disabled, accelerators work without opening the menu). The native state (Icon, AccessKey, KeyboardAccelerators) is also read and checked.
 * The shape seen from Java is tested in WMenuFlyoutItemForJavaTest.
 */
@Tags("Integration")
class WMenuFlyoutItemTest : FunSpec() {
    /** A description of the item's native Icon ("Symbol:SAVE" etc.). null if none. */
    private fun nativeIconOf(item: WMenuFlyoutItem): String? {
        val view = item.inspectable.queryInterface(XamlInterop.IID_IMenuFlyoutItem)
        return try {
            view.getPtrOrNull(XamlInterop.IMenuFlyoutItem_get_Icon)?.let { describe(it) }
        } finally {
            view.release()
        }
    }

    private fun nativeIconOf(item: WMenuFlyoutSubItem): String? =
        item.inspectable.getPtrOrNull(XamlInterop.IMenuFlyoutSubItem_get_Icon)?.let { describe(it) }

    /** A description of an IconElement. Font glyphs become "Font:<glyph>" without the font name. */
    private fun describe(element: ComPtr): String = try {
        IconElements.describe(element).let { if (it.startsWith("Font:")) it.substringBefore('/') else it }
    } finally {
        element.release()
    }

    private fun accessKeyOf(component: WComponent): String = component.uiElement.getString(XamlInterop.IUIElement_get_AccessKey)

    /** The list of (Windows virtual key, VirtualKeyModifiers) of the element's KeyboardAccelerators. */
    private fun accelerators(component: WComponent): List<Pair<Int, Int>> {
        val vector = component.uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        return try {
            (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
                val accelerator = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                try {
                    accelerator.getInt(XamlInterop.IKeyboardAccelerator_get_Key) to
                        accelerator.getInt(XamlInterop.IKeyboardAccelerator_get_Modifiers)
                } finally {
                    accelerator.release()
                }
            }
        } finally {
            vector.release()
        }
    }

    /** A listener that records received events as pairs of "item if the source is the item, otherwise the type name" and the action command. */
    private fun recorder(item: WMenuFlyoutItem, events: MutableList<String>, tag: String): (WActionEvent) -> Unit = { e ->
        events += "$tag:${if (e.source === item) "item" else e.source.javaClass.name}:${e.actionCommand}"
    }

    /** An action that records the event source and action command when called. */
    private class RecordingAction(name: String? = null, icon: WIcon? = null) : WAbstractAction(name, icon) {
        val performed = mutableListOf<Pair<Any, String?>>()

        override fun actionPerformed(e: WActionEvent) {
            performed += e.source to e.actionCommand
        }
    }

    /** Opens [menu] from [owner] and waits until [item] is loaded and can receive focus. */
    private suspend fun openMenu(menu: WMenuFlyout, owner: WComponent, item: WMenuFlyoutItem) {
        onUiThread { menu.showAt(owner) }
        eventually(5.seconds) {
            onUiThreadGet { item.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
        }
        Thread.sleep(OPEN_SETTLE_MILLIS) // Focus moves inside during the opening animation
    }

    init {
        test("WMenuFlyoutItem(text) has only text, with no icon, mnemonic, accelerator or action") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Open")
                listOf(item.text, item.icon, nativeIconOf(item), item.mnemonic, item.accelerator, item.action, item.actionCommand)
            } shouldContainExactly listOf("Open", null, null, 0, null, null, "Open")
        }

        test("WMenuFlyoutItem() has empty text and an empty action command") {
            onUiThreadGet {
                val item = WMenuFlyoutItem()
                listOf(item.text, item.actionCommand, item.icon)
            } shouldContainExactly listOf("", "", null)
        }

        test("WMenuFlyoutItem(icon) has only an icon without text, and a SymbolIcon goes into the native Icon") {
            onUiThreadGet {
                val item = WMenuFlyoutItem(Symbol.SAVE)
                listOf(item.text, item.icon, nativeIconOf(item))
            } shouldContainExactly listOf("", Symbol.SAVE, "Symbol:SAVE")
        }

        test("WMenuFlyoutItem(text, icon) also accepts font glyph and image icons, and is text only if null") {
            onUiThreadGet {
                val glyph = WMenuFlyoutItem("Open", WFontIcon(""))
                val plain = WMenuFlyoutItem("Close", null)
                listOf(glyph.text, glyph.icon, nativeIconOf(glyph), plain.text, plain.icon, nativeIconOf(plain))
            } shouldContainExactly listOf("Open", WFontIcon(""), "Font:", "Close", null, null)
        }

        test("setting icon to null removes the native Icon too, and changing to another kind of icon switches to it") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Save", Symbol.SAVE)
                item.icon = WFontIcon("")
                val replaced = listOf(item.icon, nativeIconOf(item))
                item.icon = null
                replaced + listOf(item.icon, nativeIconOf(item))
            } shouldContainExactly listOf(WFontIcon(""), "Font:", null, null)
        }

        test("WMenuFlyoutItem(text, mnemonic) sets the text and mnemonic, and the native AccessKey becomes that character") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Save", WKeyEvent.VK_S)
                listOf(item.text, item.mnemonic, accessKeyOf(item))
            } shouldContainExactly listOf("Save", WKeyEvent.VK_S, "S")
        }

        test("setMnemonic(char) turns letters into upper-case key codes and clears with '\\u0000'. Anything other than letters and digits throws IllegalArgumentException") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Open")
                item.setMnemonic('o')
                val set = listOf(item.mnemonic, accessKeyOf(item))
                shouldThrow<IllegalArgumentException> { item.setMnemonic('!') }
                shouldThrow<IllegalArgumentException> { item.mnemonic = '!'.code }
                val afterError = item.mnemonic
                item.setMnemonic('\u0000')
                set + listOf(afterError, item.mnemonic, accessKeyOf(item))
            } shouldContainExactly listOf(WKeyEvent.VK_O, "O", WKeyEvent.VK_O, 0, "")
        }

        test("actionCommand is the text while unset (following text changes), returns the set value, and becomes unset again with null") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Open")
                item.text = "Open file"
                val followed = item.actionCommand
                item.actionCommand = "open"
                val set = item.actionCommand
                item.actionCommand = null
                listOf(followed, set, item.actionCommand)
            } shouldContainExactly listOf("Open file", "open", "Open file")
        }

        test("doClick calls listeners in the order added, with the item as source and actionCommand as the action command") {
            val events = mutableListOf<String>()
            onUiThread {
                val item = WMenuFlyoutItem("Open")
                item.addActionListener(recorder(item, events, "a"))
                item.addActionListener(recorder(item, events, "b"))
                item.doClick()
                item.actionCommand = "open"
                item.doClick()
            }
            onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Open", "b:item:Open", "a:item:open", "b:item:open")
        }

        test("a listener removed with removeActionListener is not called by doClick, and removing a listener never added does nothing") {
            val events = mutableListOf<String>()
            onUiThread {
                val item = WMenuFlyoutItem("Open")
                val first = recorder(item, events, "a")
                val second = recorder(item, events, "b")
                item.addActionListener(first)
                item.addActionListener(second)
                item.removeActionListener(first)
                item.removeActionListener(recorder(item, events, "c"))
                item.doClick()
            }
            onUiThreadGet { events.toList() } shouldContainExactly listOf("b:item:Open")
        }

        test("doClick on a disabled item calls nothing (same as Swing)") {
            val events = mutableListOf<String>()
            onUiThread {
                val item = WMenuFlyoutItem("Open")
                item.addActionListener(recorder(item, events, "a"))
                item.isEnabled = false
                item.doClick()
                item.isEnabled = true
                item.doClick()
            }
            onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Open")
        }

        test("doClick also executes the item's command (WCommand)") {
            val executed = mutableListOf<String?>()
            onUiThread {
                val item = WMenuFlyoutItem("Open")
                item.command = WCommand { parameter -> executed += parameter }
                item.commandParameter = "Argument"
                item.doClick()
            }
            onUiThreadGet { executed.toList() } shouldContainExactly listOf("Argument")
        }

        test("selecting an item of an open menu with the keyboard (Enter) calls the listener with the item as source and closes the menu") {
            val button = onUiThreadGet { WButton("Menu") }
            val item = onUiThreadGet { WMenuFlyoutItem("Open") }
            val menu = onUiThreadGet { WMenuFlyout().also { it.add(item) } }
            val events = mutableListOf<String>()
            onUiThread { item.addActionListener(recorder(item, events, "a")) }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                openMenu(menu, button, item)
                onUiThread { check(item.requestFocusInWindow()) }
                TestKeyboard.pressEnter()
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Open") }
                eventually(3.seconds) { onUiThreadGet { menu.isOpen }.shouldBeFalse() }
            } finally {
                onUiThread { menu.hide() }
                UiTestHarness.detach(button)
            }
        }

        test("doClick on an item of an open menu calls the listener and closes the menu (same as selecting with the mouse)") {
            val button = onUiThreadGet { WButton("Menu") }
            val item = onUiThreadGet { WMenuFlyoutItem("Open") }
            val menu = onUiThreadGet { WMenuFlyout().also { it.add(item) } }
            val events = mutableListOf<String>()
            onUiThread { item.addActionListener(recorder(item, events, "a")) }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                openMenu(menu, button, item)
                onUiThread { item.doClick() }
                onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Open")
                eventually(3.seconds) { onUiThreadGet { menu.isOpen }.shouldBeFalse() }
            } finally {
                onUiThread { menu.hide() }
                UiTestHarness.detach(button)
            }
        }

        test("accelerator puts exactly one into KeyboardAccelerators, replaces the previous one when changed, and is removed with null") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Save")
                item.accelerator = WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0)
                val first = accelerators(item)
                item.accelerator = WKeyStroke.getKeyStroke("ctrl S")
                val replaced = accelerators(item)
                val read = item.accelerator
                item.accelerator = null
                listOf(first, replaced, read, accelerators(item), item.accelerator)
            } shouldContainExactly listOf(
                listOf(VK_F5 to 0),
                listOf(VK_S to MODIFIER_CONTROL),
                WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK),
                emptyList<Pair<Int, Int>>(),
                null,
            )
        }

        test("changing accelerator keeps accelerators added separately with addKeyboardAccelerator") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Save")
                item.addKeyboardAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_F2, 0))
                item.accelerator = WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0)
                item.accelerator = null
                accelerators(item)
            } shouldContainExactly listOf(VK_F2 to 0)
        }

        test("a keystroke that cannot be an accelerator throws IllegalArgumentException and the previous accelerator stays") {
            onUiThreadGet {
                val item = WMenuFlyoutItem("Save")
                val stroke = WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0)
                item.accelerator = stroke
                shouldThrow<IllegalArgumentException> { item.accelerator = WKeyStroke.getKeyStroke('a') }
                shouldThrow<IllegalArgumentException> {
                    item.accelerator = WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK, true)
                }
                listOf(item.accelerator, accelerators(item))
            } shouldContainExactly listOf(WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0), listOf(VK_F5 to 0))
        }

        test("the accelerator (F5) of an item in a ContextFlyout menu raises Click without opening the menu, and keeps working after the owner is removed (WinUI behavior)") {
            val panel = onUiThreadGet { WPanel() }
            val owner = onUiThreadGet { WButton("Owner").also { panel.add(it) } }
            val other = onUiThreadGet { WButton("Other").also { panel.add(it) } }
            val item = onUiThreadGet { WMenuFlyoutItem("Refresh") }
            val menu = onUiThreadGet { WMenuFlyout().also { it.add(item) } }
            val events = mutableListOf<String>()
            onUiThread {
                owner.contextFlyout = menu
                item.accelerator = WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0)
                item.addActionListener(recorder(item, events, "a"))
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread { check(other.requestFocusInWindow()) { "Cannot move the focus" } }
                TestKeyboard.press(VK_F5, SCAN_F5)
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Refresh") }
                onUiThreadGet { menu.isOpen }.shouldBeFalse()
            } finally {
                UiTestHarness.detach(panel)
            }
            Thread.sleep(ACCELERATOR_SETTLE_MILLIS)
            TestKeyboard.press(VK_F5, SCAN_F5)
            Thread.sleep(ACCELERATOR_SETTLE_MILLIS)
            // WinUI behavior: even after the owner is removed from the window, the accelerators of ContextFlyout items still work
            // (doc/troubleshooting.md. In Swing, accelerators of removed components do not work)
            onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Refresh", "a:item:Refresh")
        }

        test("the accelerator (F7) of a menu bar item raises Click without opening the menu") {
            val panel = onUiThreadGet { WPanel() }
            val bar = onUiThreadGet { WMenuBar().also { panel.add(it) } }
            val other = onUiThreadGet { WButton("Other").also { panel.add(it) } }
            val item = onUiThreadGet { WMenuFlyoutItem("Refresh") }
            val barItem = onUiThreadGet { WMenuBarItem("File") }
            val events = mutableListOf<String>()
            onUiThread {
                barItem.add(item)
                bar.add(barItem)
                item.accelerator = WKeyStroke.getKeyStroke(WKeyEvent.VK_F7, 0)
                item.addActionListener(recorder(item, events, "a"))
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                // MenuBarItem moves its items to the ContextFlyout menu when its template is applied, so wait until it is loaded
                eventually(5.seconds) {
                    onUiThreadGet { barItem.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
                }
                // Right after loading, the accelerator may not work yet (wait until WinUI counts it as an active accelerator)
                Thread.sleep(ACCELERATOR_SETTLE_MILLIS)
                onUiThread { check(other.requestFocusInWindow()) { "Cannot move the focus" } }
                TestKeyboard.press(VK_F7, SCAN_F7)
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("a:item:Refresh") }
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("WMenuFlyoutItem(action) copies the action's values and doClick calls the action with source = the item") {
            val action = RecordingAction("Save", Symbol.SAVE)
            val described = onUiThreadGet {
                action.putValue(WAction.SHORT_DESCRIPTION, "Save over the file")
                action.putValue(WAction.MNEMONIC_KEY, WKeyEvent.VK_S)
                action.putValue(WAction.ACTION_COMMAND_KEY, "save")
                action.putValue(WAction.ACCELERATOR_KEY, WKeyStroke.getKeyStroke("ctrl S"))
                val item = WMenuFlyoutItem(action)
                item.doClick()
                listOf(
                    item.text,
                    item.icon,
                    nativeIconOf(item),
                    item.toolTipText,
                    item.mnemonic,
                    item.actionCommand,
                    item.accelerator,
                    accelerators(item),
                    item.action === action,
                    action.performed.map { (source, command) -> (source === item) to command },
                )
            }
            described shouldContainExactly listOf(
                "Save",
                Symbol.SAVE,
                "Symbol:SAVE",
                "Save over the file",
                WKeyEvent.VK_S,
                "save",
                WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK),
                listOf(VK_S to MODIFIER_CONTROL),
                true,
                listOf(true to "save"),
            )
        }

        test("changing the action's values later makes the item follow, and disabling it means doClick does not call it") {
            val action = RecordingAction("Save")
            val described = onUiThreadGet {
                val item = WMenuFlyoutItem()
                item.action = action
                action.putValue(WAction.NAME, "Save as")
                action.putValue(WAction.SMALL_ICON, WFontIcon(""))
                action.putValue(WAction.ACCELERATOR_KEY, WKeyStroke.getKeyStroke(WKeyEvent.VK_F12, 0))
                action.isEnabled = false
                item.doClick()
                val disabled = listOf(item.text, item.icon, accelerators(item), item.isEnabled, action.performed.size)
                action.isEnabled = true
                action.putValue(WAction.ACCELERATOR_KEY, null)
                item.doClick()
                disabled + listOf(item.isEnabled, accelerators(item), action.performed.size)
            }
            described shouldContainExactly listOf(
                "Save as",
                WFontIcon(""),
                listOf(VK_F12 to 0),
                false,
                0,
                true,
                emptyList<Pair<Int, Int>>(),
                1,
            )
        }

        test("replacing with another action or null unbinds the previous action, and null resets the item to unset") {
            val first = RecordingAction("Save", Symbol.SAVE)
            val second = RecordingAction("Open")
            val described = onUiThreadGet {
                first.putValue(WAction.ACCELERATOR_KEY, WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0))
                val item = WMenuFlyoutItem(first)
                item.action = second
                first.putValue(WAction.NAME, "Previous action")
                item.doClick()
                val switched = listOf(item.text, item.icon, accelerators(item), first.performed.size, second.performed.size)
                item.action = null
                item.doClick()
                switched + listOf(item.text, item.icon, item.accelerator, item.action, second.performed.size)
            }
            described shouldContainExactly listOf("Open", null, emptyList<Pair<Int, Int>>(), 0, 1, "", null, null, null, 1)
        }

        test("an action value of the wrong type (ACCELERATOR_KEY that is not a WKeyStroke) throws IllegalArgumentException") {
            onUiThread {
                val action = RecordingAction("Save")
                action.putValue(WAction.ACCELERATOR_KEY, "ctrl S")
                shouldThrow<IllegalArgumentException> { WMenuFlyoutItem(action) }
            }
        }

        test("WToggleMenuFlyoutItem also accepts a WIcon icon") {
            onUiThreadGet {
                val item = WToggleMenuFlyoutItem("Bold", WFontIcon(""))
                listOf(item.text, item.icon, nativeIconOf(item))
            } shouldContainExactly listOf("Bold", WFontIcon(""), "Font:")
        }

        test("WMenuFlyoutSubItem has a WIcon icon and a mnemonic, which go into the native Icon and AccessKey") {
            onUiThreadGet {
                val sub = WMenuFlyoutSubItem("Recent files", Symbol.CLOCK)
                sub.setMnemonic('r')
                val first = listOf(sub.text, sub.icon, nativeIconOf(sub), sub.mnemonic, accessKeyOf(sub))
                sub.icon = null
                first + listOf(sub.icon, nativeIconOf(sub))
            } shouldContainExactly listOf("Recent files", Symbol.CLOCK, "Symbol:CLOCK", WKeyEvent.VK_R, "R", null, null)
        }

        test("WMenuFlyoutSubItem() has empty text and no icon or mnemonic") {
            onUiThreadGet {
                val sub = WMenuFlyoutSubItem()
                listOf(sub.text, sub.icon, sub.mnemonic)
            } shouldContainExactly listOf("", null, 0)
        }

        test("WMenuFlyoutItem has no accelerator by default") {
            onUiThreadGet { accelerators(WMenuFlyoutItem("x")) }.shouldBeEmpty()
            onUiThreadGet { WMenuFlyoutItem("x").accelerator }.shouldBeNull()
        }
    }

    private companion object {
        const val VK_F2 = 0x71
        const val VK_F5 = 0x74
        const val VK_F12 = 0x7B
        const val VK_S = 0x53
        const val SCAN_F5 = 0x3F
        const val VK_F7 = 0x76
        const val SCAN_F7 = 0x41
        const val MODIFIER_CONTROL = 1
        const val OPEN_SETTLE_MILLIS = 500L
        const val ACCELERATOR_SETTLE_MILLIS = 500L
    }
}
