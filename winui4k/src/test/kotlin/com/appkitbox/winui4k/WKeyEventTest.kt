package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the KeyListener-style [WComponent.addKeyListener]. Keys are sent with [TestKeyboard] to the input child
 * window of the shared window (WM_CHAR is produced by WinUI's TranslateMessage). The expected values are derived from
 * the contract of AWT's KeyEvent (key codes, characters and order).
 */
class WKeyEventTest : FunSpec() {
    /** A form with a text field, a button and a numeric field. Records what the listeners received in [events]. */
    private class Form {
        val panel = WPanel()
        val field = WTextField()
        val button = WButton("OK")
        val spinner = WSpinner(1.0)
        val events = mutableListOf<String>()
        val received = mutableListOf<WKeyEvent>()

        init {
            listOf(field, button, spinner).forEach { panel.add(it) }
        }

        /** Attaches to [component] a listener that records each received event as "name:kind:key code:character code". */
        fun record(component: WComponent, name: String, consumePressed: Int? = null): WKeyListener = object : WKeyListener {
            private fun record(kind: String, e: WKeyEvent) {
                events += "$name:$kind:${e.keyCode}:${e.keyChar.code}"
                received += e
            }

            override fun keyTyped(e: WKeyEvent) = record("typed", e)

            override fun keyPressed(e: WKeyEvent) {
                record("pressed", e)
                if (e.keyCode == consumePressed) e.consume()
            }

            override fun keyReleased(e: WKeyEvent) = record("released", e)
        }.also { component.addKeyListener(it) }
    }

    private fun showForm(): Form {
        val form = onUiThreadGet { Form() }
        UiTestHarness.attachAndAwaitLoaded(form.panel)
        return form
    }

    private fun focus(component: WComponent) = onUiThread { check(component.requestFocusInWindow()) { "could not move focus" } }

    /** Waits until the records (excluding those containing [ignoring]) become [expected], and checks they stay unchanged for a while. */
    private suspend fun awaitEvents(form: Form, expected: List<String>, ignoring: String? = null) {
        fun events() = onUiThreadGet { form.events.filterNot { ignoring != null && ignoring in it } }
        eventually(3.seconds) { events() shouldBe expected }
        Thread.sleep(SETTLE_MILLIS)
        events() shouldContainExactly expected
    }

    init {
        test("pressing and releasing A in a text field calls keyPressed, keyTyped and keyReleased in order, with key code VK_A and the character 'a' only in typed") {
            val form = showForm()
            try {
                onUiThread { form.record(form.field, "field") }
                focus(form.field)
                TestKeyboard.keyDown(VK_A, SCAN_A)
                awaitEvents(form, listOf("field:pressed:65:65535", "field:typed:0:97"))
                TestKeyboard.keyUp(VK_A, SCAN_A)
                awaitEvents(form, listOf("field:pressed:65:65535", "field:typed:0:97", "field:released:65:65535"))
                val (pressed, typed, released) = onUiThreadGet { form.received.toList() }
                listOf(pressed.id, typed.id, released.id) shouldBe listOf(WKeyEvent.KEY_PRESSED, WKeyEvent.KEY_TYPED, WKeyEvent.KEY_RELEASED)
                pressed.source shouldBeSameInstanceAs form.field
                pressed.component shouldBeSameInstanceAs form.field
                listOf(pressed.virtualKey, typed.virtualKey, released.virtualKey) shouldBe
                    listOf(VirtualKey.A, VirtualKey.NONE, VirtualKey.A)
                listOf(pressed.keyChar, typed.keyChar) shouldBe listOf(WKeyEvent.CHAR_UNDEFINED, 'a')
                pressed.isActionKey.shouldBeFalse()
                onUiThreadGet { form.field.text } shouldBe "a"
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("Enter has keyCode AWT's VK_ENTER (10), the keyTyped character '\\n', and virtualKey WinUI's ENTER (13)") {
            val form = showForm()
            try {
                onUiThread { form.record(form.field, "field") }
                focus(form.field)
                TestKeyboard.keyDown(VK_RETURN, SCAN_RETURN)
                awaitEvents(form, listOf("field:pressed:10:65535", "field:typed:0:10"))
                TestKeyboard.keyUp(VK_RETURN, SCAN_RETURN)
                awaitEvents(form, listOf("field:pressed:10:65535", "field:typed:0:10", "field:released:10:65535"))
                onUiThreadGet { form.received.first().virtualKey } shouldBe VirtualKey.ENTER
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("Delete (46 -> 127) and Insert (45 -> 155), whose values differ between Windows virtual keys and AWT, are mapped, and arrows are isActionKey") {
            val form = showForm()
            try {
                onUiThread { form.record(form.button, "button") }
                focus(form.button)
                for ((key, scan) in listOf(VK_WIN_DELETE to SCAN_DELETE, VK_WIN_INSERT to SCAN_INSERT, VK_WIN_LEFT to SCAN_LEFT)) {
                    TestKeyboard.keyDown(key, scan, extended = true)
                    TestKeyboard.keyUp(key, scan, extended = true)
                }
                awaitEvents(
                    form,
                    listOf(
                        "button:pressed:127:65535",
                        "button:released:127:65535",
                        "button:pressed:155:65535",
                        "button:released:155:65535",
                        "button:pressed:37:65535",
                        "button:released:37:65535",
                    ),
                )
                onUiThreadGet { form.received.map { it.virtualKey } } shouldBe
                    listOf(VirtualKey.DELETE, VirtualKey.DELETE, VirtualKey.INSERT, VirtualKey.INSERT, VirtualKey.LEFT, VirtualKey.LEFT)
                onUiThreadGet { form.received.map { it.isActionKey } } shouldBe listOf(false, false, true, true, true, true)
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("a panel's listener is not called for keys while a child inside the panel has focus (as in Swing)") {
            val form = showForm()
            try {
                onUiThread {
                    form.record(form.panel, "panel")
                    form.record(form.field, "field")
                }
                focus(form.field)
                TestKeyboard.keyDown(VK_A, SCAN_A)
                awaitEvents(form, listOf("field:pressed:65:65535", "field:typed:0:97"))
                TestKeyboard.keyUp(VK_A, SCAN_A)
                awaitEvents(form, listOf("field:pressed:65:65535", "field:typed:0:97", "field:released:65:65535"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("a WSpinner (NumberBox) listener is called for keys while its inner text field has focus") {
            val form = showForm()
            try {
                onUiThread {
                    form.record(form.spinner, "spinner")
                    check(form.spinner.focus(XamlInterop.FocusState_Programmatic)) { "could not move focus" }
                }
                TestKeyboard.keyDown(VK_5, SCAN_5)
                awaitEvents(form, listOf("spinner:pressed:53:65535", "spinner:typed:0:53"))
                TestKeyboard.keyUp(VK_5, SCAN_5)
                awaitEvents(form, listOf("spinner:pressed:53:65535", "spinner:typed:0:53", "spinner:released:53:65535"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("consuming Enter in a button's keyPressed prevents the click, and not consuming it lets the click happen") {
            val form = showForm()
            try {
                onUiThread {
                    form.button.addActionListener { form.events += "action" }
                    form.record(form.button, "button", consumePressed = WKeyEvent.VK_ENTER)
                }
                focus(form.button)
                TestKeyboard.pressEnter()
                // keyTyped for a press that became handled may or may not arrive (WinUI behavior; see doc/verification.md)
                awaitEvents(form, listOf("button:pressed:10:65535", "button:released:10:65535"), ignoring = ":typed:")
                onUiThreadGet { form.received.first().isConsumed }.shouldBeTrue()
                onUiThread {
                    form.events.clear()
                    form.button.keyListeners.forEach { form.button.removeKeyListener(it) }
                    form.record(form.button, "button")
                }
                TestKeyboard.pressEnter()
                awaitEvents(form, listOf("button:pressed:10:65535", "button:released:10:65535", "action"), ignoring = ":typed:")
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("consuming Enter in WTextField's keyPressed prevents the ActionListener from being called (as in Swing)") {
            val form = showForm()
            try {
                onUiThread {
                    form.field.addActionListener { form.events += "action" }
                    form.record(form.field, "field", consumePressed = WKeyEvent.VK_ENTER)
                }
                focus(form.field)
                TestKeyboard.pressEnter()
                awaitEvents(form, listOf("field:pressed:10:65535", "field:released:10:65535"), ignoring = ":typed:")
                // It is called on the next Enter (not consumed)
                onUiThread {
                    form.events.clear()
                    form.field.keyListeners.forEach { form.field.removeKeyListener(it) }
                }
                TestKeyboard.pressEnter()
                awaitEvents(form, listOf("action"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("consuming in keyTyped does not stop the character from being inserted into the text field (WinUI's TextBox inserts first)") {
            val form = showForm()
            try {
                onUiThread {
                    form.field.addKeyListener(object : WKeyAdapter() {
                        override fun keyTyped(e: WKeyEvent) {
                            e.consume()
                            form.events += "typed:${e.keyChar}:${e.isConsumed}"
                        }
                    })
                }
                focus(form.field)
                TestKeyboard.press(VK_A, SCAN_A)
                awaitEvents(form, listOf("typed:a:true"))
                onUiThreadGet { form.field.text } shouldBe "a"
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("a listener removed with removeKeyListener is not called, and adding or removing null does nothing") {
            val form = showForm()
            try {
                val listener = onUiThreadGet {
                    form.field.addKeyListener(null)
                    form.record(form.field, "field").also {
                        form.field.removeKeyListener(null)
                        form.field.removeKeyListener(it)
                    }
                }
                onUiThreadGet { form.field.keyListeners.toList() }.shouldBeEmpty()
                focus(form.field)
                TestKeyboard.press(VK_A, SCAN_A)
                eventually(3.seconds) { onUiThreadGet { form.field.text } shouldBe "a" }
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { form.events.toList() }.shouldBeEmpty()
                onUiThreadGet {
                    form.field.addKeyListener(listener)
                    form.field.addKeyListener(listener)
                    form.field.keyListeners.toList()
                } shouldBe listOf(listener, listener)
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("Windows virtual keys map to AWT's KeyEvent.VK_*, and to VK_UNDEFINED when there is no counterpart") {
            val pairs = listOf(
                13 to WKeyEvent.VK_ENTER, 8 to WKeyEvent.VK_BACK_SPACE, 9 to WKeyEvent.VK_TAB, 27 to WKeyEvent.VK_ESCAPE,
                32 to WKeyEvent.VK_SPACE, 33 to WKeyEvent.VK_PAGE_UP, 40 to WKeyEvent.VK_DOWN, 44 to WKeyEvent.VK_PRINTSCREEN,
                45 to WKeyEvent.VK_INSERT, 46 to WKeyEvent.VK_DELETE, 47 to WKeyEvent.VK_HELP, 48 to WKeyEvent.VK_0,
                57 to WKeyEvent.VK_9, 65 to WKeyEvent.VK_A, 90 to WKeyEvent.VK_Z, 91 to WKeyEvent.VK_WINDOWS,
                92 to WKeyEvent.VK_WINDOWS, 93 to WKeyEvent.VK_CONTEXT_MENU, 96 to WKeyEvent.VK_NUMPAD0, 111 to WKeyEvent.VK_DIVIDE,
                112 to WKeyEvent.VK_F1, 123 to WKeyEvent.VK_F12, 124 to WKeyEvent.VK_F13, 135 to WKeyEvent.VK_F24,
                144 to WKeyEvent.VK_NUM_LOCK, 145 to WKeyEvent.VK_SCROLL_LOCK, 160 to WKeyEvent.VK_SHIFT, 163 to WKeyEvent.VK_CONTROL,
                165 to WKeyEvent.VK_ALT, 186 to WKeyEvent.VK_SEMICOLON, 187 to WKeyEvent.VK_EQUALS, 188 to WKeyEvent.VK_COMMA,
                189 to WKeyEvent.VK_MINUS, 190 to WKeyEvent.VK_PERIOD, 191 to WKeyEvent.VK_SLASH, 192 to WKeyEvent.VK_BACK_QUOTE,
                219 to WKeyEvent.VK_OPEN_BRACKET, 220 to WKeyEvent.VK_BACK_SLASH, 221 to WKeyEvent.VK_CLOSE_BRACKET,
                222 to WKeyEvent.VK_QUOTE, 25 to WKeyEvent.VK_KANJI, 28 to WKeyEvent.VK_CONVERT, 29 to WKeyEvent.VK_NONCONVERT,
                229 to WKeyEvent.VK_UNDEFINED, 0 to WKeyEvent.VK_UNDEFINED, 255 to WKeyEvent.VK_UNDEFINED,
            )
            pairs.map { (windows, _) -> WKeyEvent.keyCodeOf(windows) } shouldBe pairs.map { it.second }
            listOf(virtualKeyOf(13), virtualKeyOf(0), virtualKeyOf(0x7FFF)) shouldBe listOf(VirtualKey.ENTER, VirtualKey.NONE, VirtualKey.NONE)
        }

        test("KeyEvent's ID, character and key code constants have the same values as AWT") {
            listOf(WKeyEvent.KEY_FIRST, WKeyEvent.KEY_LAST, WKeyEvent.KEY_TYPED, WKeyEvent.KEY_PRESSED, WKeyEvent.KEY_RELEASED) shouldBe
                listOf(400, 402, 400, 401, 402)
            WKeyEvent.CHAR_UNDEFINED.code shouldBe 0xFFFF
            listOf(
                WKeyEvent.VK_ENTER, WKeyEvent.VK_DELETE, WKeyEvent.VK_INSERT, WKeyEvent.VK_F13, WKeyEvent.VK_F24,
                WKeyEvent.VK_WINDOWS, WKeyEvent.VK_CONTEXT_MENU, WKeyEvent.VK_META, WKeyEvent.VK_UNDEFINED,
            ) shouldBe listOf(10, 127, 155, 0xF000, 0xF00B, 524, 525, 157, 0)
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 500L

        // Windows virtual keys and scan codes (values sent to TestKeyboard)
        const val VK_A = 0x41
        const val SCAN_A = 0x1E
        const val VK_5 = 0x35
        const val SCAN_5 = 0x06
        const val VK_RETURN = 0x0D
        const val SCAN_RETURN = 0x1C
        const val VK_WIN_DELETE = 0x2E
        const val SCAN_DELETE = 0x53
        const val VK_WIN_INSERT = 0x2D
        const val SCAN_INSERT = 0x52
        const val VK_WIN_LEFT = 0x25
        const val SCAN_LEFT = 0x4B
    }
}
