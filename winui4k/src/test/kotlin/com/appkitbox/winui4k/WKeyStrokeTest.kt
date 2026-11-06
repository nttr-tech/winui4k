package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests the KeyStroke-style [WKeyStroke], the mapping from AWT key codes to Windows virtual keys ([WKeyEvent.windowsKeyOf]),
 * and [WComponent.addKeyboardAccelerator] (WKeyStroke).
 *
 * The expected strings, modifiers and hash values were taken from the actual values of Swing's KeyStroke (the output of
 * tools/swing_probes/KeyStrokeProbe.java run on Temurin JDK 25). The expected virtual keys are the VK_* values in WinUser.h.
 */
class WKeyStrokeTest : FunSpec() {
    /** Values measured with Swing's KeyStroke (toString, keyCode, modifiers, the keyChar code, onKeyRelease, keyEventType, hashCode). */
    private data class Measured(
        val text: String,
        val keyCode: Int,
        val modifiers: Int,
        val keyChar: Int,
        val onKeyRelease: Boolean,
        val keyEventType: Int,
        val hashCode: Int,
    )

    private fun WKeyStroke.measured() =
        Measured(toString(), keyCode, modifiers, keyChar.code, isOnKeyRelease, keyEventType, hashCode())

    /** The names and values of [WKeyEvent]'s VK_* constants (read from the declarations only to enumerate the constants). */
    private val vkConstants: Map<String, Int> = WKeyEvent::class.java.fields
        .filter { it.name.startsWith("VK_") && it.type == Int::class.javaPrimitiveType }
        .associate { it.name to it.getInt(null) }

    /** (Key, Modifiers) of each element of UIElement.KeyboardAccelerators. */
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

    init {
        test("getKeyStroke(VK_S, CTRL_MASK) and CTRL_DOWN_MASK give the same value, and modifiers has both the new and old bits (130)") {
            val expected = Measured("ctrl pressed S", 83, 130, 65535, false, WKeyEvent.KEY_PRESSED, 1442316290)
            WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_MASK).measured() shouldBe expected
            WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK).measured() shouldBe expected
            WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_MASK) shouldBe
                WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK)
        }

        test("modifiers are written in the order shift, ctrl, meta, alt, altGraph, button1-3, and old-style mouse buttons are dropped") {
            WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK or WInputEvent.SHIFT_DOWN_MASK).measured() shouldBe
                Measured("shift ctrl pressed S", 83, 195, 65535, false, WKeyEvent.KEY_PRESSED, -2136997886)
            WKeyStroke.getKeyStroke(WKeyEvent.VK_S, 0x3fff).measured() shouldBe Measured(
                "shift ctrl meta alt altGraph button1 button2 button3 pressed S",
                83,
                16367,
                65535,
                false,
                WKeyEvent.KEY_PRESSED,
                -176160766,
            )
            WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.BUTTON1_MASK).measured() shouldBe
                Measured("pressed S", 83, 0, 65535, false, WKeyEvent.KEY_PRESSED, 11010050)
        }

        test("release key strokes are written as released, and keyEventType is KEY_RELEASED") {
            WKeyStroke.getKeyStroke(WKeyEvent.VK_F4, WInputEvent.ALT_DOWN_MASK, true).measured() shouldBe
                Measured("alt released F4", 115, 520, 65535, true, WKeyEvent.KEY_RELEASED, -668467199)
        }

        test("character key strokes are written as typed with keyCode VK_UNDEFINED, and key codes not declared are written as UNKNOWN") {
            WKeyStroke.getKeyStroke('a').measured() shouldBe Measured("typed a", 0, 0, 97, false, WKeyEvent.KEY_TYPED, 198)
            WKeyStroke.getKeyStroke(0x1234, 0).measured() shouldBe
                Measured("pressed UNKNOWN", 4660, 0, 65535, false, WKeyEvent.KEY_PRESSED, 610926594)
        }

        test("getKeyStroke(String) reads AWT's format and gives the same values as Swing") {
            val expected = mapOf(
                "ctrl shift S" to Measured("shift ctrl pressed S", 83, 195, 65535, false, WKeyEvent.KEY_PRESSED, -2136997886),
                "alt F4" to Measured("alt pressed F4", 115, 520, 65535, false, WKeyEvent.KEY_PRESSED, -668467198),
                "control DELETE" to Measured("ctrl pressed DELETE", 127, 130, 65535, false, WKeyEvent.KEY_PRESSED, -2097151998),
                "meta W" to Measured("meta pressed W", 87, 260, 65535, false, WKeyEvent.KEY_PRESSED, -1284505598),
                "shift typed a" to Measured("shift typed a", 0, 65, 97, false, WKeyEvent.KEY_TYPED, 12938),
                "typed a" to Measured("typed a", 0, 0, 97, false, WKeyEvent.KEY_TYPED, 198),
                "released ENTER" to Measured("released ENTER", 10, 0, 65535, true, WKeyEvent.KEY_RELEASED, 1441793),
                "pressed ENTER" to Measured("pressed ENTER", 10, 0, 65535, false, WKeyEvent.KEY_PRESSED, 1441794),
                "ctrl pressed S" to Measured("ctrl pressed S", 83, 130, 65535, false, WKeyEvent.KEY_PRESSED, 1442316290),
                "S" to Measured("pressed S", 83, 0, 65535, false, WKeyEvent.KEY_PRESSED, 11010050),
                "altGraph Q" to Measured("altGraph pressed Q", 81, 8224, 65535, false, WKeyEvent.KEY_PRESSED, -1792802814),
                "button1 A" to Measured("button1 pressed A", 65, 1024, 65535, false, WKeyEvent.KEY_PRESSED, 277086210),
                "  ctrl   S  " to Measured("ctrl pressed S", 83, 130, 65535, false, WKeyEvent.KEY_PRESSED, 1442316290),
                "alt shift ctrl meta X" to Measured("shift ctrl meta alt pressed X", 88, 975, 65535, false, WKeyEvent.KEY_PRESSED, -1499463678),
                "shift  S" to Measured("shift pressed S", 83, 65, 65535, false, WKeyEvent.KEY_PRESSED, 726663170),
                "ctrl 9" to Measured("ctrl pressed 9", 57, 130, 65535, false, WKeyEvent.KEY_PRESSED, 995885058),
                "SEMICOLON" to Measured("pressed SEMICOLON", 59, 0, 65535, false, WKeyEvent.KEY_PRESSED, 7864322),
            )
            expected.mapValues { (text, _) -> WKeyStroke.getKeyStroke(text)?.measured() } shouldBe expected
        }

        test("getKeyStroke(String) returns null for null, empty and malformed input (lowercase key names, uppercase modifiers, modifiers without a key, wrong word order)") {
            val invalid = listOf(
                null, "", "ctrl s", "CTRL S", "ctrl", "ctrl typed ab", "typed a b", "pressed ctrl S", "ctrl FOO", "typed  ", "typed",
            )
            invalid.filter { WKeyStroke.getKeyStroke(it) != null }.shouldBeEmpty()
        }

        test("passing the toString string to getKeyStroke(String) gives back the same key stroke") {
            val strokes = listOf(
                WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK or WInputEvent.SHIFT_DOWN_MASK),
                WKeyStroke.getKeyStroke(WKeyEvent.VK_F4, WInputEvent.ALT_DOWN_MASK, true),
                WKeyStroke.getKeyStroke(WKeyEvent.VK_DELETE, WInputEvent.META_DOWN_MASK),
                WKeyStroke.getKeyStroke('x'),
            )
            strokes.map { WKeyStroke.getKeyStroke(it.toString()) } shouldBe strokes
        }

        test("equals compares all of the character, key code, modifiers and whether it is for release") {
            val ctrlS = WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK)
            ctrlS shouldBe WKeyStroke.getKeyStroke("ctrl S")
            ctrlS shouldNotBe WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK, true)
            ctrlS shouldNotBe WKeyStroke.getKeyStroke(WKeyEvent.VK_S, 0)
            ctrlS shouldNotBe WKeyStroke.getKeyStroke(WKeyEvent.VK_A, WInputEvent.CTRL_DOWN_MASK)
            WKeyStroke.getKeyStroke('a') shouldNotBe WKeyStroke.getKeyStroke("shift typed a")
        }

        test("every AWT VK_* constant maps to a Windows virtual key and back to the original key code with keyCodeOf (except VK_UNDEFINED and VK_META)") {
            val unmapped = vkConstants.filterValues { WKeyEvent.windowsKeyOf(it) == null }.keys
            unmapped shouldBe setOf("VK_UNDEFINED", "VK_META")
            val roundTrip = vkConstants.filterValues { it != WKeyEvent.VK_UNDEFINED && it != WKeyEvent.VK_META }
                .filter { (_, code) -> WKeyEvent.keyCodeOf(checkNotNull(WKeyEvent.windowsKeyOf(code))) != code }
            roundTrip.keys.shouldBeEmpty()
        }

        test("keys whose values differ from AWT map to the virtual keys in WinUser.h (left/right keys to the left/right-agnostic value, the Windows key to the left one)") {
            val expected = mapOf(
                WKeyEvent.VK_ENTER to 0x0D,
                WKeyEvent.VK_DELETE to 0x2E,
                WKeyEvent.VK_INSERT to 0x2D,
                WKeyEvent.VK_HELP to 0x2F,
                WKeyEvent.VK_PRINTSCREEN to 0x2C,
                WKeyEvent.VK_WINDOWS to 0x5B,
                WKeyEvent.VK_CONTEXT_MENU to 0x5D,
                WKeyEvent.VK_SHIFT to 0x10,
                WKeyEvent.VK_CONTROL to 0x11,
                WKeyEvent.VK_ALT to 0x12,
                WKeyEvent.VK_SEMICOLON to 0xBA,
                WKeyEvent.VK_EQUALS to 0xBB,
                WKeyEvent.VK_COMMA to 0xBC,
                WKeyEvent.VK_MINUS to 0xBD,
                WKeyEvent.VK_PERIOD to 0xBE,
                WKeyEvent.VK_SLASH to 0xBF,
                WKeyEvent.VK_BACK_QUOTE to 0xC0,
                WKeyEvent.VK_OPEN_BRACKET to 0xDB,
                WKeyEvent.VK_BACK_SLASH to 0xDC,
                WKeyEvent.VK_CLOSE_BRACKET to 0xDD,
                WKeyEvent.VK_QUOTE to 0xDE,
                WKeyEvent.VK_F13 to 0x7C,
                WKeyEvent.VK_F24 to 0x87,
                WKeyEvent.VK_A to 0x41,
                WKeyEvent.VK_F1 to 0x70,
                WKeyEvent.VK_NUMPAD0 to 0x60,
                WKeyEvent.VK_ESCAPE to 0x1B,
            )
            expected.mapValues { (code, _) -> WKeyEvent.windowsKeyOf(code) } shouldBe expected
        }

        test("new-style modifiers map to VirtualKeyModifiers (Ctrl=1, Alt=2, Shift=4, Win=8), and old-style bits and buttons are ignored") {
            WInputEvent.toVirtualKeyModifiers(WInputEvent.CTRL_DOWN_MASK) shouldBe 1
            WInputEvent.toVirtualKeyModifiers(WInputEvent.ALT_DOWN_MASK) shouldBe 2
            WInputEvent.toVirtualKeyModifiers(WInputEvent.SHIFT_DOWN_MASK) shouldBe 4
            WInputEvent.toVirtualKeyModifiers(WInputEvent.META_DOWN_MASK) shouldBe 8
            WInputEvent.toVirtualKeyModifiers(WInputEvent.CTRL_MASK or WInputEvent.BUTTON1_DOWN_MASK) shouldBe 0
            WKeyStroke.getKeyStroke("shift ctrl meta alt X")!!.modifiers.let(WInputEvent::toVirtualKeyModifiers) shouldBe 15
        }

        test("getKeyStrokeForEvent creates a key stroke from the key event's kind, key code, character and modifiers") {
            val source = onUiThreadGet { WButton("x") }
            val pressed = WKeyEvent(source, WKeyEvent.KEY_PRESSED, 0, WInputEvent.CTRL_DOWN_MASK, WKeyEvent.VK_S, WKeyEvent.CHAR_UNDEFINED)
            val released = WKeyEvent(source, WKeyEvent.KEY_RELEASED, 0, 0, WKeyEvent.VK_S, WKeyEvent.CHAR_UNDEFINED)
            val typed = WKeyEvent(source, WKeyEvent.KEY_TYPED, 0, WInputEvent.SHIFT_DOWN_MASK, WKeyEvent.VK_UNDEFINED, 'A')
            WKeyStroke.getKeyStrokeForEvent(pressed) shouldBe WKeyStroke.getKeyStroke("ctrl S")
            WKeyStroke.getKeyStrokeForEvent(released) shouldBe WKeyStroke.getKeyStroke("released S")
            WKeyStroke.getKeyStrokeForEvent(typed) shouldBe WKeyStroke.getKeyStroke("shift typed A")
        }

        test("addKeyboardAccelerator(WKeyStroke) puts the Windows values into the KeyboardAccelerator's Key and Modifiers (including OEM keys not in VirtualKey)") {
            val button = onUiThreadGet { WButton("x") }
            val result = onUiThreadGet {
                button.addKeyboardAccelerator(WKeyStroke.getKeyStroke("ctrl SEMICOLON")!!)
                button.addKeyboardAccelerator(WKeyStroke.getKeyStroke("shift meta alt F13")!!) {}
                button.addKeyboardAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_DELETE, 0))
                accelerators(button)
            }
            result shouldContainExactly listOf(0xBA to 1, 0x7C to (4 or 8 or 2), 0x2E to 0)
        }

        test("key strokes for character input, key release, AltGraph, mouse buttons and keys Windows does not have cannot be accelerators") {
            val button = onUiThreadGet { WButton("x") }
            val rejected = listOf(
                WKeyStroke.getKeyStroke('a'),
                WKeyStroke.getKeyStroke(WKeyEvent.VK_F4, WInputEvent.ALT_DOWN_MASK, true),
                WKeyStroke.getKeyStroke("altGraph Q")!!,
                WKeyStroke.getKeyStroke("button1 A")!!,
                WKeyStroke.getKeyStroke(WKeyEvent.VK_META, 0),
            )
            onUiThread {
                for (stroke in rejected) {
                    shouldThrow<IllegalArgumentException> { button.addKeyboardAccelerator(stroke) }
                    shouldThrow<IllegalArgumentException> { button.addKeyboardAccelerator(stroke, Runnable {}) }
                }
            }
            onUiThreadGet { accelerators(button) }.shouldBeEmpty()
        }

        test("addKeyboardAccelerator(WKeyStroke, Runnable) calls the action when F5 is pressed and does not click the button") {
            val panel = onUiThreadGet { WPanel() }
            val button = onUiThreadGet { WButton("x").also { panel.add(it) } }
            val events = mutableListOf<String>()
            onUiThread {
                button.addActionListener { events += "click" }
                button.addKeyboardAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_F5, 0)) { events += "F5" }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread { check(button.requestFocusInWindow()) { "cannot move the focus" } }
                TestKeyboard.press(VK_F5, SCAN_F5)
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldBe listOf("F5") }
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { events.toList() } shouldBe listOf("F5")
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("key names such as WAction.ACCELERATOR_KEY are the same as Swing's Action") {
            WAction.ACCELERATOR_KEY shouldBe "AcceleratorKey"
            WAction.LONG_DESCRIPTION shouldBe "LongDescription"
            WAction.LARGE_ICON_KEY shouldBe "SwingLargeIconKey"
            WAction.DISPLAYED_MNEMONIC_INDEX_KEY shouldBe "SwingDisplayedMnemonicIndexKey"
            val action = object : WAbstractAction("Save") {
                override fun actionPerformed(e: WActionEvent) = Unit
            }
            action.putValue(WAction.ACCELERATOR_KEY, WKeyStroke.getKeyStroke("ctrl S"))
            action.getValue(WAction.ACCELERATOR_KEY) shouldBe WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_MASK)
            WKeyStroke.getKeyStroke("typed").shouldBeNull()
        }
    }

    private companion object {
        const val VK_F5 = 0x74
        const val SCAN_F5 = 0x3F
        const val SETTLE_MILLIS = 300L
    }
}
