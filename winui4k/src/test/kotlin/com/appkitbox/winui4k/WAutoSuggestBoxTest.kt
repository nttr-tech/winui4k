package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify the text, icon, ActionListener and query submission of WAutoSuggestBox (a text field that shows
 * suggestions, close to an editable JComboBox).
 * Key input is sent to the shared window with [TestKeyboard].
 */
class WAutoSuggestBoxTest : FunSpec() {
    /** The placed box and the record of what happened. */
    private class Shown(val box: WAutoSuggestBox) {
        val events = mutableListOf<String>()
        val sources = mutableListOf<Any?>()
    }

    private fun shown(configure: (WAutoSuggestBox, Shown) -> Unit = { _, _ -> }): Shown {
        val result = onUiThreadGet {
            val box = WAutoSuggestBox()
            val shown = Shown(box)
            configure(box, shown)
            shown
        }
        UiTestHarness.attachAndAwaitLoaded(result.box)
        return result
    }

    /** Moves focus to the box and presses the keys. */
    private fun typeKeys(shown: Shown, vararg keys: Pair<Int, Int>) {
        onUiThread { check(shown.box.requestFocusInWindow()) { "could not move focus" } }
        for ((virtualKey, scanCode) in keys) {
            TestKeyboard.press(virtualKey, scanCode)
            // The text of the text field reaches the box's Text and QueryText later, so wait before the next key
            Thread.sleep(KEY_INTERVAL_MILLIS)
        }
    }

    /** The content of the box's QueryIcon ([IconElements.describe]), or "none" if there is none. */
    private fun nativeQueryIcon(box: WAutoSuggestBox): String {
        val icon = box.inspectable.getPtrOrNull(XamlInterop.IAutoSuggestBox_get_QueryIcon) ?: return "none"
        return try {
            IconElements.describe(icon)
        } finally {
            icon.release()
        }
    }

    init {
        test("text and placeholderText read back the strings set, and the constructor's placeholder is also applied") {
            val (text, placeholder, fromConstructor) = onUiThreadGet {
                val box = WAutoSuggestBox("Find")
                box.text = "Apple"
                box.placeholderText = "Another hint"
                Triple(box.text, box.placeholderText, WAutoSuggestBox("Find").placeholderText)
            }
            text shouldBe "Apple"
            placeholder shouldBe "Another hint"
            fromConstructor shouldBe "Find"
        }

        test("queryIcon puts any of a Symbol, an image or a font glyph into the native QueryIcon, and returns the icon as set") {
            val results = onUiThreadGet {
                val box = WAutoSuggestBox()
                val none = nativeQueryIcon(box) to box.queryIcon
                box.queryIcon = Symbol.FIND
                val symbol = nativeQueryIcon(box) to box.queryIcon
                box.queryIcon = WFontIcon("\uE8E5")
                val font = nativeQueryIcon(box) to box.queryIcon
                box.queryIcon = WImageIcon("C:/icons/find.png")
                val image = nativeQueryIcon(box) to box.queryIcon
                listOf(none, symbol, font, image)
            }
            results shouldContainExactly listOf(
                "none" to null,
                "Symbol:FIND" to Symbol.FIND,
                "Font:\uE8E5/Segoe Fluent Icons" to WFontIcon("\uE8E5"),
                "Image:file:///C:/icons/find.png" to WImageIcon("C:/icons/find.png"),
            )
        }

        test("setting queryIcon to null removes the icon") {
            onUiThreadGet {
                val box = WAutoSuggestBox()
                box.queryIcon = Symbol.FIND
                box.queryIcon = null
                nativeQueryIcon(box) to box.queryIcon
            } shouldBe ("none" to null)
        }

        test("an addActionListener listener is called once on Enter, with the box as source and the typed text as actionCommand") {
            val form = shown { box, shown ->
                box.addActionListener { e ->
                    shown.sources += e.source
                    shown.events += "action:${e.actionCommand}:${e.modifiers}"
                }
            }
            try {
                typeKeys(form, KEY_A, KEY_B, KEY_ENTER)
                eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe listOf("action:ab:0") }
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { form.events.toList() } shouldContainExactly listOf("action:ab:0")
                onUiThreadGet { form.sources.toList() } shouldContainExactly listOf(form.box)
            } finally {
                UiTestHarness.detach(form.box)
            }
        }

        test("just typing characters does not call the ActionListener") {
            val form = shown { box, shown -> box.addActionListener { shown.events += "action" } }
            try {
                typeKeys(form, KEY_A, KEY_B)
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { form.events.toList() } shouldContainExactly emptyList()
            } finally {
                UiTestHarness.detach(form.box)
            }
        }

        test("setting actionCommand puts that string in the event, and setting it back to null makes it the text") {
            val form = shown { box, shown ->
                box.actionCommand = "search"
                box.addActionListener { e -> shown.events += "action:${e.actionCommand}" }
            }
            try {
                typeKeys(form, KEY_A, KEY_ENTER)
                eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe listOf("action:search") }
                onUiThread { form.box.actionCommand = null }
                typeKeys(form, KEY_ENTER)
                eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe listOf("action:search", "action:a") }
            } finally {
                UiTestHarness.detach(form.box)
            }
        }

        test("multiple listeners are called in registration order, a listener removed with removeActionListener is not called, and QuerySubmitted listeners are also called on the same Enter") {
            val form = shown { box, shown ->
                val removed: (WActionEvent) -> Unit = { shown.events += "removed" }
                box.addActionListener { shown.events += "first" }
                box.addActionListener(removed)
                box.addActionListener { shown.events += "second" }
                box.removeActionListener(removed)
                box.addQuerySubmittedListener { query, chosen -> shown.events += "query:$query:$chosen" }
            }
            try {
                typeKeys(form, KEY_A, KEY_ENTER)
                eventually(3.seconds) {
                    onUiThreadGet { form.events.sorted() } shouldBe listOf("first", "query:a:null", "second")
                }
                // ActionListeners are called in registration order (the order relative to QuerySubmitted listeners is not defined)
                val order = onUiThreadGet { form.events.filter { it == "first" || it == "second" } }
                order shouldContainExactly listOf("first", "second")
            } finally {
                UiTestHarness.detach(form.box)
            }
        }

        test("postActionEvent calls the same listeners as pressing Enter") {
            onUiThreadGet {
                val box = WAutoSuggestBox()
                val events = mutableListOf<String>()
                box.text = "search term"
                box.addActionListener { e -> events += "first:${e.actionCommand}" }
                box.addActionListener { events += "second" }
                box.postActionEvent()
                events.toList()
            } shouldContainExactly listOf("first:search term", "second")
        }

        test("TextChanged listeners receive the reason USER_INPUT for key input and PROGRAMMATIC_CHANGE for changes from code") {
            val form = shown { box, shown ->
                box.addTextChangedListener { text, reason -> shown.events += "$text:$reason" }
            }
            try {
                typeKeys(form, KEY_A)
                eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe listOf("a:USER_INPUT") }
                onUiThread { form.box.text = "code" }
                eventually(3.seconds) {
                    onUiThreadGet { form.events.toList() } shouldBe listOf("a:USER_INPUT", "code:PROGRAMMATIC_CHANGE")
                }
            } finally {
                UiTestHarness.detach(form.box)
            }
        }

        test("QuerySubmitted listeners receive the text submitted with Enter and null (no suggestion chosen)") {
            val form = shown { box, shown ->
                box.addQuerySubmittedListener { query, chosen -> shown.events += "$query:$chosen" }
            }
            try {
                typeKeys(form, KEY_A, KEY_B, KEY_ENTER)
                eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe listOf("ab:null") }
            } finally {
                UiTestHarness.detach(form.box)
            }
        }

        test("on Enter after showing suggestions and choosing one with the Down key, QuerySubmitted receives the chosen suggestion and the ActionListener's actionCommand is the submitted text") {
            val form = shown { box, shown ->
                box.addTextChangedListener { text, reason ->
                    if (reason == TextChangeReason.USER_INPUT) box.setSuggestions(listOf(text + "-one", text + "-two"))
                }
                box.addQuerySubmittedListener { query, chosen -> shown.events += "query:$query:$chosen" }
                box.addActionListener { e -> shown.events += "action:${e.actionCommand}" }
            }
            try {
                typeKeys(form, KEY_A)
                Thread.sleep(SETTLE_MILLIS) // wait for the suggestions to be replaced and the list to open
                TestKeyboard.press(KEY_DOWN.first, KEY_DOWN.second)
                Thread.sleep(SETTLE_MILLIS)
                TestKeyboard.press(KEY_ENTER.first, KEY_ENTER.second)
                eventually(3.seconds) {
                    onUiThreadGet { form.events.sorted() } shouldBe listOf("action:a-one", "query:a-one:a-one")
                }
            } finally {
                UiTestHarness.detach(form.box)
            }
        }
    }

    private companion object {
        val KEY_A = 0x41 to 0x1E
        val KEY_B = 0x42 to 0x30
        val KEY_ENTER = 0x0D to 0x1C
        val KEY_DOWN = 0x28 to 0x50

        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L

        /** Time to wait between keys. */
        const val KEY_INTERVAL_MILLIS = 300L
    }
}
