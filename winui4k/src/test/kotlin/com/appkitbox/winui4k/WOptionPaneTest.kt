package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Tests the JOptionPane-style [WOptionPane]: constants with the same values as Swing, assembling the buttons, title and icon (Japanese and English by locale),
 * the return values of the blocking forms (per button, CLOSED_OPTION for Esc, when the owner window is closed), input and choices,
 * the callback forms, and the exceptions for a second dialog and wrong arguments. Opened dialogs are looked at and pressed from outside with [OpenContentDialogs].
 */
@Tags("Integration")
class WOptionPaneTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private suspend fun awaitEvents(vararg expected: String) {
        eventually(5.seconds) { events() shouldBe expected.toList() }
    }

    private var savedLocale: Locale = Locale.getDefault()

    /** Places an owner button in the shared window, runs [block], and removes it at the end. */
    private suspend fun withOwner(block: suspend (WButton) -> Unit) {
        val owner = onUiThreadGet { WButton("Owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(owner)
        }
    }

    /** Calls [show] (the blocking form) on the UI thread and records the returned value as "r:value". */
    private fun later(show: () -> Any?) {
        WinUiUtilities.invokeLater { events += "r:${show()}" }
    }

    /** Waits until a dialog opens in [owner]'s window and can receive keys, and returns how it looks. */
    private suspend fun awaitOpen(owner: WComponent): OpenContentDialogs.Description {
        eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeTrue() }
        Thread.sleep(OPEN_SETTLE_MILLIS)
        return OpenContentDialogs.describe(owner).shouldNotBeNull()
    }

    /** Waits until it finishes closing (so the next dialog can be opened in the same window). */
    private suspend fun awaitClosed(owner: WComponent) {
        eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeFalse() }
        eventually(5.seconds) { OpenContentDialogs.describe(owner) shouldBe null }
    }

    private fun pressEscape() = TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)

    init {
        beforeTest {
            events.clear()
            savedLocale = Locale.getDefault()
            Locale.setDefault(Locale.JAPAN)
        }
        afterTest {
            Locale.setDefault(savedLocale)
            onUiThread { WOptionPane.resetButtonTexts() }
        }

        test("the constants have the same values as JOptionPane") {
            listOf(
                WOptionPane.YES_OPTION,
                WOptionPane.NO_OPTION,
                WOptionPane.CANCEL_OPTION,
                WOptionPane.OK_OPTION,
                WOptionPane.CLOSED_OPTION,
            ) shouldBe listOf(0, 1, 2, 0, -1)
            listOf(
                WOptionPane.DEFAULT_OPTION,
                WOptionPane.YES_NO_OPTION,
                WOptionPane.YES_NO_CANCEL_OPTION,
                WOptionPane.OK_CANCEL_OPTION,
            ) shouldBe listOf(-1, 0, 1, 2)
            listOf(
                WOptionPane.ERROR_MESSAGE,
                WOptionPane.INFORMATION_MESSAGE,
                WOptionPane.WARNING_MESSAGE,
                WOptionPane.QUESTION_MESSAGE,
                WOptionPane.PLAIN_MESSAGE,
            ) shouldBe listOf(0, 1, 2, 3, -1)
            WOptionPane.UNINITIALIZED_VALUE shouldBe "uninitializedValue"
        }

        test("showConfirmDialog(parent, message) has the Japanese title Select an Option, Yes / No / Cancel and a question icon, and the default button is Yes") {
            withOwner { owner ->
                later { WOptionPane.showConfirmDialog(owner, "Do you want to save?") }
                val shown = awaitOpen(owner)
                shown.title shouldBe "\u30aa\u30d7\u30b7\u30e7\u30f3\u306e\u9078\u629e"
                listOf(shown.primary, shown.secondary, shown.close) shouldBe listOf("\u306f\u3044", "\u3044\u3044\u3048", "\u30ad\u30e3\u30f3\u30bb\u30eb")
                shown.defaultButton shouldBe ContentDialogButton.PRIMARY
                shown.texts shouldBe listOf("Do you want to save?")
                shown.glyphs shouldBe listOf(WOptionPane.GLYPH_QUESTION)
                TestKeyboard.pressEnter()
                awaitEvents("r:${WOptionPane.YES_OPTION}")
                awaitClosed(owner)
            }
        }

        test("showConfirmDialog returns NO_OPTION for No, CANCEL_OPTION for the Cancel button, and CLOSED_OPTION for Esc, distinguished from Cancel") {
            withOwner { owner ->
                val expected = mutableListOf<String>()
                for ((press, result) in listOf<Pair<() -> Unit, Int>>(
                    { OpenContentDialogs.press(owner, "SecondaryButton") } to WOptionPane.NO_OPTION,
                    { OpenContentDialogs.press(owner, "CloseButton") } to WOptionPane.CANCEL_OPTION,
                    ::pressEscape to WOptionPane.CLOSED_OPTION,
                )) {
                    later { WOptionPane.showConfirmDialog(owner, "Do you want to save?", "Save", WOptionPane.YES_NO_CANCEL_OPTION) }
                    awaitOpen(owner).title shouldBe "Save"
                    press()
                    expected += "r:$result"
                    awaitEvents(*expected.toTypedArray())
                    awaitClosed(owner)
                }
            }
        }

        test("buttons per optionType: YES_NO has no Cancel, OK_CANCEL has OK and Cancel (CANCEL_OPTION), DEFAULT_OPTION has only OK") {
            withOwner { owner ->
                later { WOptionPane.showConfirmDialog(owner, "Continue?", "Confirm", WOptionPane.YES_NO_OPTION) }
                awaitOpen(owner).let { listOf(it.primary, it.secondary, it.close) } shouldBe listOf("\u306f\u3044", "\u3044\u3044\u3048", "")
                pressEscape() // Esc gives CLOSED_OPTION even without a close button
                awaitEvents("r:-1")
                awaitClosed(owner)

                later { WOptionPane.showConfirmDialog(owner, "Send?", "Confirm", WOptionPane.OK_CANCEL_OPTION) }
                awaitOpen(owner).let { listOf(it.primary, it.secondary, it.close) } shouldBe listOf("OK", "", "\u30ad\u30e3\u30f3\u30bb\u30eb")
                OpenContentDialogs.press(owner, "CloseButton")
                awaitEvents("r:-1", "r:${WOptionPane.CANCEL_OPTION}")
                awaitClosed(owner)

                later { WOptionPane.showConfirmDialog(owner, "Done.", "Confirm", WOptionPane.DEFAULT_OPTION) }
                awaitOpen(owner).let { listOf(it.primary, it.secondary, it.close) } shouldBe listOf("OK", "", "")
                TestKeyboard.pressEnter()
                awaitEvents("r:-1", "r:2", "r:${WOptionPane.OK_OPTION}")
                awaitClosed(owner)
            }
        }

        test("in an English locale the buttons and title are English; button texts can be changed with yesButtonText and the like, and resetButtonTexts restores them") {
            withOwner { owner ->
                Locale.setDefault(Locale.US)
                later { WOptionPane.showConfirmDialog(owner, "Save?") }
                awaitOpen(owner).let { listOf(it.title, it.primary, it.secondary, it.close) } shouldBe
                    listOf("Select an Option", "Yes", "No", "Cancel")
                pressEscape()
                awaitEvents("r:-1")
                awaitClosed(owner)

                onUiThread {
                    WOptionPane.yesButtonText = "Ja"
                    WOptionPane.noButtonText = "Nein"
                    WOptionPane.cancelButtonText = "Abbrechen"
                    WOptionPane.okButtonText = "Gut"
                }
                later { WOptionPane.showConfirmDialog(owner, "Speichern?") }
                awaitOpen(owner).let { listOf(it.primary, it.secondary, it.close) } shouldBe listOf("Ja", "Nein", "Abbrechen")
                pressEscape()
                awaitEvents("r:-1", "r:-1")
                awaitClosed(owner)
                onUiThread {
                    WOptionPane.okButtonText shouldBe "Gut"
                    WOptionPane.resetButtonTexts()
                    listOf(WOptionPane.yesButtonText, WOptionPane.okButtonText) shouldBe listOf("Yes", "OK")
                }
            }
        }

        test("icons per messageType (error, information, warning, question), PLAIN_MESSAGE has no icon, and passing icon uses that icon") {
            withOwner { owner ->
                val cases = listOf(
                    WOptionPane.ERROR_MESSAGE to listOf(WOptionPane.GLYPH_ERROR),
                    WOptionPane.INFORMATION_MESSAGE to listOf(WOptionPane.GLYPH_INFORMATION),
                    WOptionPane.WARNING_MESSAGE to listOf(WOptionPane.GLYPH_WARNING),
                    WOptionPane.QUESTION_MESSAGE to listOf(WOptionPane.GLYPH_QUESTION),
                    WOptionPane.PLAIN_MESSAGE to emptyList(),
                )
                for ((type, glyphs) in cases) {
                    later { WOptionPane.showMessageDialog(owner, "Body", "Heading", type) }
                    awaitOpen(owner).glyphs shouldBe glyphs
                    TestKeyboard.pressEnter()
                    eventually(5.seconds) { events().size shouldBe cases.indexOfFirst { it.first == type } + 1 }
                    awaitClosed(owner)
                }
                later { WOptionPane.showMessageDialog(owner, "Body", "Heading", WOptionPane.ERROR_MESSAGE, WFontIcon(CUSTOM_GLYPH)) }
                awaitOpen(owner).glyphs shouldBe listOf(CUSTOM_GLYPH)
                TestKeyboard.pressEnter()
                eventually(5.seconds) { events().size shouldBe cases.size + 1 }
                awaitClosed(owner)
            }
        }

        test("showMessageDialog(parent, message) has the Japanese title Message, only OK and an information icon, and does not return until closed") {
            withOwner { owner ->
                later { WOptionPane.showMessageDialog(owner, "Saved.") }
                val shown = awaitOpen(owner)
                listOf(shown.title, shown.primary, shown.secondary, shown.close) shouldBe listOf("\u30e1\u30c3\u30bb\u30fc\u30b8", "OK", "", "")
                shown.glyphs shouldBe listOf(WOptionPane.GLYPH_INFORMATION)
                events() shouldBe emptyList()
                pressEscape()
                awaitEvents("r:kotlin.Unit")
                awaitClosed(owner)
            }
        }

        test("message can be a string, a component or an array of them, and a component passed in can be used in the next dialog after closing") {
            withOwner { owner ->
                val extra = onUiThreadGet { WLabel("Details") }
                for (round in 1..2) {
                    later { WOptionPane.showConfirmDialog(owner, arrayOf("Line 1", extra), "Confirm", WOptionPane.YES_NO_OPTION) }
                    awaitOpen(owner).texts shouldBe listOf("Line 1", "Details")
                    TestKeyboard.pressEnter()
                    eventually(5.seconds) { events().size shouldBe round }
                    awaitClosed(owner)
                }
                events() shouldBe listOf("r:0", "r:0")
            }
        }

        test("showInputDialog returns the entered text, the initial text can be given, and Cancel and Esc give null; the title is the Japanese Input") {
            withOwner { owner ->
                later { WOptionPane.showInputDialog(owner, "Name") }
                val shown = awaitOpen(owner)
                listOf(shown.title, shown.primary, shown.close) shouldBe listOf("\u5165\u529b", "OK", "\u30ad\u30e3\u30f3\u30bb\u30eb")
                shown.glyphs shouldBe listOf(WOptionPane.GLYPH_QUESTION)
                OpenContentDialogs.setText(owner, "Smith")
                TestKeyboard.pressEnter() // Enter in the input field presses the default button (OK)
                awaitEvents("r:Smith")
                awaitClosed(owner)

                later { WOptionPane.showInputDialog(owner, "Name", "Jones") }
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "PrimaryButton")
                awaitEvents("r:Smith", "r:Jones")
                awaitClosed(owner)

                later { WOptionPane.showInputDialog(owner, "Name", "Enter Name", WOptionPane.PLAIN_MESSAGE) }
                awaitOpen(owner).let { it.title to it.glyphs } shouldBe ("Enter Name" to emptyList())
                OpenContentDialogs.press(owner, "CloseButton")
                awaitEvents("r:Smith", "r:Jones", "r:null")
                awaitClosed(owner)

                later { WOptionPane.showInputDialog(owner, "Name", "Brown") }
                awaitOpen(owner)
                pressEscape()
                awaitEvents("r:Smith", "r:Jones", "r:null", "r:null")
                awaitClosed(owner)
            }
        }

        test("showInputDialog given selectionValues shows a combo box with initialSelectionValue selected first and returns the chosen element itself") {
            withOwner { owner ->
                val values = arrayOf<Any?>(Size("S"), Size("M"), Size("L"))
                val chosen = Collections.synchronizedList(mutableListOf<Any?>())
                WinUiUtilities.invokeLater {
                    chosen += WOptionPane.showInputDialog(owner, "Size", "Choose", WOptionPane.QUESTION_MESSAGE, null, values, values[1])
                }
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "PrimaryButton")
                eventually(5.seconds) { chosen.size shouldBe 1 }
                awaitClosed(owner)

                WinUiUtilities.invokeLater {
                    chosen += WOptionPane.showInputDialog(owner, "Size", "Choose", WOptionPane.QUESTION_MESSAGE, null, values, null)
                }
                awaitOpen(owner)
                OpenContentDialogs.select(owner, 2)
                OpenContentDialogs.press(owner, "PrimaryButton")
                eventually(5.seconds) { chosen.size shouldBe 2 }
                awaitClosed(owner)
                (chosen[0] === values[1]).shouldBeTrue()
                (chosen[1] === values[2]).shouldBeTrue()
            }
        }

        test("opening the drop-down of the selectionValues combo box and pressing Esc closes only the drop-down, and another Esc closes the dialog with null") {
            withOwner { owner ->
                val values = arrayOf<Any?>("S", "M", "L")
                later { WOptionPane.showInputDialog(owner, "Size", "Choose", WOptionPane.QUESTION_MESSAGE, null, values, values[0]) }
                awaitOpen(owner)
                OpenContentDialogs.focusComboBox(owner)
                TestKeyboard.press(VK_F4, SCAN_F4)
                eventually(5.seconds) { OpenContentDialogs.isDropDownOpen(owner) shouldBe true }
                Thread.sleep(OPEN_SETTLE_MILLIS)
                pressEscape()
                eventually(5.seconds) { OpenContentDialogs.isDropDownOpen(owner) shouldBe false }
                Thread.sleep(OPEN_SETTLE_MILLIS)
                OpenContentDialogs.isOpen(owner).shouldBeTrue()
                events().shouldBeEmpty()
                pressEscape()
                awaitEvents("r:null")
                awaitClosed(owner)
            }
        }

        test("showOptionDialog makes options into buttons and returns the position, initialValue is the default button, Esc is CLOSED_OPTION, and null options use the optionType buttons") {
            withOwner { owner ->
                val options = arrayOf<Any?>("Save", "Don't Save", "Back")
                val expected = mutableListOf<String>()
                for ((part, index) in listOf("PrimaryButton" to 0, "SecondaryButton" to 1, "CloseButton" to 2, null to -1)) {
                    later {
                        WOptionPane.showOptionDialog(
                            owner,
                            "Save changes?",
                            "Save",
                            WOptionPane.DEFAULT_OPTION,
                            WOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            "Don't Save",
                        )
                    }
                    val shown = awaitOpen(owner)
                    listOf(shown.primary, shown.secondary, shown.close) shouldBe listOf("Save", "Don't Save", "Back")
                    shown.defaultButton shouldBe ContentDialogButton.SECONDARY
                    shown.glyphs shouldBe listOf(WOptionPane.GLYPH_WARNING)
                    if (part != null) OpenContentDialogs.press(owner, part) else pressEscape()
                    expected += "r:$index"
                    awaitEvents(*expected.toTypedArray())
                    awaitClosed(owner)
                }
                later {
                    WOptionPane.showOptionDialog(
                        owner,
                        "Continue?",
                        "Confirm",
                        WOptionPane.YES_NO_OPTION,
                        WOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        null,
                    )
                }
                awaitOpen(owner).let { listOf(it.primary, it.secondary, it.close) } shouldBe listOf("\u306f\u3044", "\u3044\u3044\u3048", "")
                OpenContentDialogs.press(owner, "SecondaryButton")
                expected += "r:${WOptionPane.NO_OPTION}"
                awaitEvents(*expected.toTypedArray())
                awaitClosed(owner)
            }
        }

        test("the callback forms open without blocking and pass the result when closed (IntConsumer, Consumer, Runnable)") {
            withOwner { owner ->
                onUiThread {
                    WOptionPane.showConfirmDialog(owner, "Do you want to save?", "Save", WOptionPane.YES_NO_CANCEL_OPTION, WOptionPane.QUESTION_MESSAGE, null) {
                        events += "confirm:$it"
                    }
                    events += "returned"
                }
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "CloseButton")
                awaitEvents("returned", "confirm:${WOptionPane.CANCEL_OPTION}")
                awaitClosed(owner)

                onUiThread { WOptionPane.showInputDialog(owner, "Name", "Input", WOptionPane.QUESTION_MESSAGE) { events += "input:$it" } }
                awaitOpen(owner)
                OpenContentDialogs.setText(owner, "Taylor")
                OpenContentDialogs.press(owner, "PrimaryButton")
                awaitEvents("returned", "confirm:2", "input:Taylor")
                awaitClosed(owner)

                onUiThread { WOptionPane.showMessageDialog(owner, "Done", "Notice", WOptionPane.PLAIN_MESSAGE, null) { events += "message" } }
                awaitOpen(owner)
                pressEscape()
                awaitEvents("returned", "confirm:2", "input:Taylor", "message")
                awaitClosed(owner)
            }
        }

        test("opening another dialog while one is open throws IllegalStateException (they do not nest)") {
            withOwner { owner ->
                later { WOptionPane.showConfirmDialog(owner, "First") }
                awaitOpen(owner)
                onUiThread {
                    val error = shouldThrow<IllegalStateException> { WOptionPane.showMessageDialog(owner, "Second") }
                    error.message shouldContain "only one"
                    shouldThrow<IllegalStateException> {
                        WOptionPane.showConfirmDialog(owner, "Second", "Confirm", WOptionPane.YES_NO_OPTION, WOptionPane.QUESTION_MESSAGE, null) { }
                    }
                }
                OpenContentDialogs.describe(owner).shouldNotBeNull().texts shouldBe listOf("First")
                pressEscape()
                awaitEvents("r:-1")
                awaitClosed(owner)
            }
        }

        test("wrong arguments: unknown optionType and messageType, and 4 or more options throw IllegalArgumentException") {
            withOwner { owner ->
                onUiThread {
                    shouldThrow<IllegalArgumentException> { WOptionPane.showConfirmDialog(owner, "x", "t", 5) }.message shouldContain "optionType"
                    shouldThrow<IllegalArgumentException> { WOptionPane.showMessageDialog(owner, "x", "t", 4) }.message shouldContain "messageType"
                    shouldThrow<IllegalArgumentException> {
                        WOptionPane.showOptionDialog(owner, "x", "t", WOptionPane.DEFAULT_OPTION, WOptionPane.PLAIN_MESSAGE, null, arrayOf("a", "b", "c", "d"), null)
                    }.message shouldContain "at most 3"
                }
                OpenContentDialogs.isOpen(owner).shouldBeFalse()
            }
        }

        test("with a null parent and no window shown at all, IllegalStateException (the dialog does not open)") {
            val shared = UiTestHarness.sharedFrame()
            onUiThread { shared.appWindow.hide() }
            try {
                // Call right after hiding (without waiting for Activated (inactive))
                shouldThrow<IllegalStateException> {
                    onUiThreadGet { WOptionPane.showMessageDialog(null, "No parent", "Notice", WOptionPane.INFORMATION_MESSAGE, null) { } }
                }.message shouldContain "WFrame"
            } finally {
                onUiThread { shared.appWindow.show(activate = false) }
            }
        }

        test("a null parent and showInputDialog(message) without a parent open on the last shown window (Swing's parentless form)") {
            val frame = onUiThreadGet { WFrame(PARENTLESS_TITLE) }
            try {
                // Loaded arrives even before showing (Activate), so subscribe within the same UI thread task as attaching (subscribing later, it may arrive in between and never be received)
                val loaded = CountDownLatch(1)
                val button = onUiThreadGet {
                    WButton("This window").also {
                        it.addLoadedListener { loaded.countDown() }
                        frame.add(it)
                    }
                }
                onUiThread {
                    frame.isVisible = true
                }
                loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()

                later { WOptionPane.showInputDialog("Name") }
                awaitOpen(button).texts.joinToString() shouldContain "Name"
                OpenContentDialogs.setText(button, "Smith")
                OpenContentDialogs.press(button, "PrimaryButton")
                awaitEvents("r:Smith")
                awaitClosed(button)

                events.clear()
                later { WOptionPane.showInputDialog("With an initial value", "Jones") }
                awaitOpen(button)
                OpenContentDialogs.press(button, "PrimaryButton")
                awaitEvents("r:Jones")
                awaitClosed(button)

                events.clear()
                onUiThread { WOptionPane.showMessageDialog(null, "No parent", "Notice", WOptionPane.INFORMATION_MESSAGE, null) { events += "closed" } }
                awaitOpen(button).texts.joinToString() shouldContain "No parent"
                OpenContentDialogs.press(button, "PrimaryButton")
                awaitEvents("closed")
                awaitClosed(button)
            } finally {
                onUiThread { frame.dispose() }
            }
        }

        test("closing the owner window while it is open makes showConfirmDialog return CLOSED_OPTION") {
            val frame = onUiThreadGet { WFrame(OWNER_TITLE) }
            // Loaded arrives even before showing (Activate), so subscribe within the same UI thread task as attaching (subscribing later, it may arrive in between and never be received)
            val loaded = CountDownLatch(1)
            val owner = onUiThreadGet {
                WButton("Open").also {
                    it.addLoadedListener { loaded.countDown() }
                    frame.add(it)
                }
            }
            onUiThread {
                frame.isVisible = true
            }
            loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
            later { WOptionPane.showConfirmDialog(owner, "Close?") }
            awaitOpen(owner)
            TopLevelWindows.close(TopLevelWindows.find(null, OWNER_TITLE).shouldNotBeNull())
            awaitEvents("r:${WOptionPane.CLOSED_OPTION}")
        }

        context("Swing compatibility") {
            test("passing a WFrame as the parent (the JOptionPane.showMessageDialog(frame, …) form) opens on that window, not the active window") {
                val frame = onUiThreadGet { WFrame(OWNER_TITLE) }
                try {
                    // Loaded arrives even before showing (Activate), so subscribe within the same UI thread task as attaching (subscribing later, it may arrive in between and never be received)
                    val loaded = CountDownLatch(1)
                    val button = onUiThreadGet {
                        WButton("This window").also {
                            it.addLoadedListener { loaded.countDown() }
                            frame.add(it)
                        }
                    }
                    onUiThread {
                        frame.isAutoRequestFocus = false // Keep the shared window active
                        frame.isVisible = true
                    }
                    loaded.await(5, TimeUnit.SECONDS).shouldBeTrue()
                    later { WOptionPane.showConfirmDialog(frame, "Ask in this window") }
                    awaitOpen(button).texts.joinToString() shouldContain "Ask in this window"
                    OpenContentDialogs.isOpen(UiTestHarness.sharedFrame().contentPane).shouldBeFalse()
                    OpenContentDialogs.press(button, "SecondaryButton")
                    awaitEvents("r:${WOptionPane.NO_OPTION}")
                    awaitClosed(button)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("a WIcon passed as message is shown as an icon (the same as JOptionPane's Icon message). The same for a WIcon in an array") {
                withOwner { owner ->
                    later { WOptionPane.showMessageDialog(owner, WFontIcon(CUSTOM_GLYPH), "Heading", WOptionPane.PLAIN_MESSAGE) }
                    awaitOpen(owner).let { it.glyphs to it.texts } shouldBe (listOf(CUSTOM_GLYPH) to emptyList())
                    TestKeyboard.pressEnter()
                    awaitEvents("r:kotlin.Unit")
                    awaitClosed(owner)

                    later { WOptionPane.showMessageDialog(owner, arrayOf<Any>("Body", WFontIcon(CUSTOM_GLYPH)), "Heading", WOptionPane.PLAIN_MESSAGE) }
                    awaitOpen(owner).let { it.glyphs to it.texts } shouldBe (listOf(CUSTOM_GLYPH) to listOf("Body"))
                    TestKeyboard.pressEnter()
                    awaitEvents("r:kotlin.Unit", "r:kotlin.Unit")
                    awaitClosed(owner)
                }
            }

            test("showInputDialog opens with the initial text selected (the same as JOptionPane). Without initial text there is no selection") {
                withOwner { owner ->
                    later { WOptionPane.showInputDialog(owner, "Name", "Brown") }
                    awaitOpen(owner)
                    OpenContentDialogs.selectedText(owner) shouldBe "Brown"
                    TestKeyboard.pressEnter()
                    awaitEvents("r:Brown")
                    awaitClosed(owner)

                    later { WOptionPane.showInputDialog(owner, "Name") }
                    awaitOpen(owner)
                    OpenContentDialogs.selectedText(owner) shouldBe ""
                    pressEscape()
                    awaitEvents("r:Brown", "r:null")
                    awaitClosed(owner)
                }
            }
        }
    }

    /** An option shown by toString (to check that the chosen element itself is returned). */
    private class Size(private val label: String) {
        override fun toString(): String = label
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val VK_F4 = 0x73
        const val SCAN_F4 = 0x3E
        const val OPEN_SETTLE_MILLIS = 600L
        const val OWNER_TITLE = "winui4k option pane owner"
        const val PARENTLESS_TITLE = "winui4k option pane parentless"
        const val CUSTOM_GLYPH = "\uE8A5"
    }
}
