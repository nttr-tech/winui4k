package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the menu and toolbar APIs added by the comparison with Swing (AUDIT):
 * JMenuItem's getActionListeners and doClick(int), JCheckBoxMenuItem / JRadioButtonMenuItem's getItemListeners and getSelectedObjects,
 * JRadioButtonMenuItem(Icon, boolean), JMenu's getText / setText, setMnemonic, isPopupMenuVisible / setPopupMenuVisible and doClick,
 * JToolBar(String) / JToolBar(int) / JToolBar(String, int). Expected values are decided from Swing's behavior.
 */
@Tags("E2E")
class WMenuSwingTest : FunSpec() {
    private fun recorder(events: MutableList<String>, name: String) = WActionListener { e -> events += "$name:${e.actionCommand}" }

    private fun accessKeyOf(component: WComponent): String = component.uiElement.getString(XamlInterop.IUIElement_get_AccessKey)

    private fun pressEscape() = TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)

    /** Places [menu] (holding one item) and a button to put the focus on in a menu bar, runs [block], and removes them at the end. */
    private suspend fun withMenuBar(menu: WMenuBarItem, block: suspend (button: WButton) -> Unit) {
        val (panel, button) = onUiThreadGet {
            val panel = WPanel()
            val bar = WMenuBar().also { panel.add(it) }
            bar.add(menu)
            panel to WButton("Anchor").also { panel.add(it) }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            block(button)
        } finally {
            onUiThread { menu.isPopupMenuVisible = false }
            UiTestHarness.detach(panel)
        }
    }

    init {
        context("Defaults") {
            test("a newly created item has empty actionListeners, and check and radio items have empty getItemListeners") {
                onUiThreadGet {
                    listOf(
                        WMenuFlyoutItem("Open").actionListeners.size,
                        WToggleMenuFlyoutItem("Bold").getItemListeners().size,
                        WRadioMenuFlyoutItem("Small").getItemListeners().size,
                    )
                } shouldContainExactly listOf(0, 0, 0)
            }

            test("WMenuBarItem's text is the same as title, mnemonic is 0, and isPopupMenuVisible is false before being placed in a window") {
                onUiThreadGet {
                    val menu = WMenuBarItem("File")
                    listOf(menu.text, menu.mnemonic, menu.isPopupMenuVisible)
                } shouldContainExactly listOf("File", 0, false)
            }

            test("WCommandBar(name) is a horizontal bar with the name") {
                onUiThreadGet {
                    val bar = WCommandBar("Edit")
                    bar.name to bar.orientation
                } shouldBe ("Edit" to WCommandBar.HORIZONTAL)
            }
        }

        context("Reading and writing") {
            test("setting WMenuBarItem's text changes title and the native Title") {
                onUiThreadGet {
                    val menu = WMenuBarItem("File")
                    menu.text = "Edit"
                    val afterText = listOf(menu.title, menu.inspectable.getString(XamlInterop.IMenuBarItem_get_Title))
                    menu.title = "View"
                    afterText + menu.text
                } shouldContainExactly listOf("Edit", "Edit", "View")
            }

            test("WMenuBarItem's mnemonic becomes the native AccessKey, setMnemonic(char) upper-cases letters, and 0 removes it") {
                onUiThreadGet {
                    val menu = WMenuBarItem("File")
                    menu.mnemonic = 'F'.code
                    val byCode = listOf(menu.mnemonic, accessKeyOf(menu))
                    menu.setMnemonic('e')
                    val byChar = listOf(menu.mnemonic, accessKeyOf(menu))
                    menu.mnemonic = 0
                    byCode + byChar + listOf(menu.mnemonic, accessKeyOf(menu))
                } shouldContainExactly listOf('F'.code, "F", 'E'.code, "E", 0, "")
            }

            test("selectedObjects is an array of only the text when selected and null when not (check and radio items)") {
                onUiThreadGet {
                    val bold = WToggleMenuFlyoutItem("Bold")
                    val small = WRadioMenuFlyoutItem("Small")
                    val before = listOf(bold.selectedObjects, small.selectedObjects)
                    bold.isSelected = true
                    small.isSelected = true
                    before + listOf(bold.selectedObjects?.toList(), small.selectedObjects?.toList())
                } shouldContainExactly listOf(null, null, listOf("Bold"), listOf("Small"))
            }

            test("WRadioMenuFlyoutItem(icon, selected) sets the icon and selection state, and the text is empty") {
                onUiThreadGet {
                    val on = WRadioMenuFlyoutItem(Symbol.ZOOM_IN, true)
                    val off = WRadioMenuFlyoutItem(Symbol.ZOOM_OUT, false)
                    listOf(on.icon, on.isSelected, on.text, off.icon, off.isSelected)
                } shouldContainExactly listOf(Symbol.ZOOM_IN, true, "", Symbol.ZOOM_OUT, false)
            }

            test("WCommandBar(HORIZONTAL) and WCommandBar(name, HORIZONTAL) are horizontal bars") {
                onUiThreadGet {
                    val plain = WCommandBar(WCommandBar.HORIZONTAL)
                    val named = WCommandBar("Format", WCommandBar.HORIZONTAL)
                    listOf(plain.name, plain.orientation, named.name, named.orientation)
                } shouldContainExactly listOf("", WCommandBar.HORIZONTAL, "Format", WCommandBar.HORIZONTAL)
            }
        }

        context("Events") {
            test("actionListeners are in the order added, WActionListeners appear as themselves, and after removing all returned ones doClick calls none") {
                val events = mutableListOf<String>()
                val first = recorder(events, "a")
                val (listeners, after) = onUiThreadGet {
                    val item = WMenuFlyoutItem("Open")
                    item.addActionListener(first)
                    item.addActionListener { e -> events += "kotlin:${e.actionCommand}" }
                    item.addActionListener(Runnable { events += "runnable" })
                    val listeners = item.actionListeners
                    item.doClick()
                    listeners.forEach { item.removeActionListener(it) }
                    item.doClick()
                    listeners to item.actionListeners
                }
                listeners.size shouldBe 3
                listeners[0] shouldBeSameInstanceAs first
                after.shouldBeEmpty()
                events shouldContainExactly listOf("a:Open", "kotlin:Open", "runnable")
            }

            test("while an action is bound, actionListeners include one internal listener that disappears when unbound") {
                onUiThreadGet {
                    val item = WMenuFlyoutItem("Open")
                    val action = object : WAbstractAction("Save") {
                        override fun actionPerformed(e: WActionEvent) = Unit
                    }
                    item.action = action
                    val bound = item.actionListeners.size
                    item.action = null
                    bound to item.actionListeners.size
                } shouldBe (1 to 0)
            }

            test("doClick(pressTime) clicks once like doClick and toggles a check item") {
                val events = mutableListOf<String>()
                val selected = onUiThreadGet {
                    val item = WMenuFlyoutItem("Open")
                    item.addActionListener(recorder(events, "item"))
                    item.doClick(100)
                    val bold = WToggleMenuFlyoutItem("Bold")
                    bold.addActionListener(recorder(events, "bold"))
                    bold.doClick(0)
                    bold.isSelected
                }
                selected shouldBe true
                events shouldContainExactly listOf("item:Open", "bold:Bold")
            }

            test("getItemListeners are in the order added, WItemListeners appear as themselves, and removed returned ones are no longer called") {
                val events = mutableListOf<String>()
                val listener = WItemListener { e -> events += "listener:${e.stateChange}" }
                val (sizes, same) = onUiThreadGet {
                    val bold = WToggleMenuFlyoutItem("Bold")
                    val small = WRadioMenuFlyoutItem("Small")
                    bold.addItemListener(listener)
                    bold.addItemListener(WItemListener { e -> events += "second:${e.stateChange}" })
                    small.addItemListener(listener)
                    val boldListeners = bold.getItemListeners()
                    val smallListeners = small.getItemListeners()
                    boldListeners.forEach { bold.removeItemListener(it) }
                    smallListeners.forEach { small.removeItemListener(it) }
                    bold.isSelected = true
                    small.isSelected = true
                    listOf(boldListeners.size, smallListeners.size, bold.getItemListeners().size, small.getItemListeners().size) to
                        (boldListeners[0] === listener && smallListeners[0] === listener)
                }
                sizes shouldContainExactly listOf(2, 1, 0, 0)
                same shouldBe true
                onUiThreadGet { events.toList() }.shouldBeEmpty()
            }

            test("WMenuBarItem's isPopupMenuVisible = true opens the menu and calls menuSelected, and false closes it") {
                val events = mutableListOf<String>()
                val menu = onUiThreadGet {
                    WMenuBarItem("File").also { menu ->
                        menu.add("Open")
                        menu.addMenuListener(object : WMenuAdapter() {
                            override fun menuSelected(e: WMenuEvent) {
                                events += "selected"
                            }

                            override fun menuDeselected(e: WMenuEvent) {
                                events += "deselected"
                            }
                        })
                    }
                }
                withMenuBar(menu) {
                    onUiThread { menu.isPopupMenuVisible = true }
                    eventually(5.seconds) { onUiThreadGet { menu.isPopupMenuVisible } shouldBe true }
                    // Setting true again while open does not close it (the same as Swing's setPopupMenuVisible)
                    onUiThread { menu.isPopupMenuVisible = true }
                    onUiThreadGet { menu.isPopupMenuVisible } shouldBe true
                    onUiThread { menu.isPopupMenuVisible = false }
                    eventually(5.seconds) { onUiThreadGet { menu.isPopupMenuVisible } shouldBe false }
                    eventually(5.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("selected", "deselected") }
                }
            }

            test("WMenuBarItem's doClick opens a closed menu and closes an open menu (the same as clicking the menu bar heading)") {
                val menu = onUiThreadGet { WMenuBarItem("File").also { it.add("Open") } }
                withMenuBar(menu) {
                    onUiThread { menu.doClick() }
                    eventually(5.seconds) { onUiThreadGet { menu.isPopupMenuVisible } shouldBe true }
                    onUiThread { menu.doClick() }
                    eventually(5.seconds) { onUiThreadGet { menu.isPopupMenuVisible } shouldBe false }
                }
            }
        }

        context("Boundaries and exceptions") {
            test("doClick(pressTime) on a disabled item does nothing") {
                val events = mutableListOf<String>()
                onUiThreadGet {
                    val item = WMenuFlyoutItem("Open")
                    item.addActionListener(recorder(events, "item"))
                    item.isEnabled = false
                    item.doClick(10)
                    val bold = WToggleMenuFlyoutItem("Bold")
                    bold.isEnabled = false
                    bold.doClick(10)
                    bold.isSelected
                } shouldBe false
                events.shouldBeEmpty()
            }

            test("WCommandBar(VERTICAL) and values that are not orientations throw IllegalArgumentException (the same as Swing's JToolBar(int))") {
                onUiThread {
                    shouldThrow<IllegalArgumentException> { WCommandBar(WCommandBar.VERTICAL) }
                    shouldThrow<IllegalArgumentException> { WCommandBar("Edit", 5) }
                }
            }

            test("setting a WMenuBarItem mnemonic other than a letter or digit throws IllegalArgumentException and the value does not change") {
                onUiThreadGet {
                    val menu = WMenuBarItem("File")
                    menu.mnemonic = 'F'.code
                    shouldThrow<IllegalArgumentException> { menu.mnemonic = '!'.code }
                    shouldThrow<IllegalArgumentException> { menu.setMnemonic('!') }
                    menu.mnemonic
                } shouldBe 'F'.code
            }

            test("a WMenuBarItem without items does not open even with isPopupMenuVisible = true (WinUI does not open an empty menu)") {
                val menu = onUiThreadGet { WMenuBarItem("Empty") }
                withMenuBar(menu) {
                    onUiThread { menu.isPopupMenuVisible = true }
                    onUiThreadGet { menu.isPopupMenuVisible } shouldBe false
                }
            }

            test("setting isPopupMenuVisible and doClick on a WMenuBarItem not placed in a window do nothing") {
                onUiThreadGet {
                    val menu = WMenuBarItem("File").also { it.add("Open") }
                    menu.isPopupMenuVisible = true
                    menu.doClick()
                    menu.isPopupMenuVisible
                } shouldBe false
            }
        }

        context("Input") {
            test("pressing Alt and the key of WMenuBarItem's mnemonic opens the menu (the same as Swing's JMenu.setMnemonic)") {
                val menu = onUiThreadGet {
                    WMenuBarItem("File").also {
                        it.add("Open")
                        it.setMnemonic('F')
                    }
                }
                withMenuBar(menu) { button ->
                    onUiThread { check(button.requestFocusInWindow()) { "Cannot move the focus" } }
                    TestKeyboard.pressWithAlt('F'.code, SCAN_F)
                    eventually(5.seconds) { onUiThreadGet { menu.isPopupMenuVisible } shouldBe true }
                    pressEscape()
                    eventually(5.seconds) { onUiThreadGet { menu.isPopupMenuVisible } shouldBe false }
                }
            }
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val SCAN_F = 0x21
    }
}
