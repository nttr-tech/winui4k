package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.StringReader
import java.io.StringWriter

/**
 * Tests for the JTextComponent / JTextField / JPasswordField APIs added through the comparison with Swing (AUDIT)
 * (getActionListeners, getText(offset, length), selectionColor, focusAccelerator, read / write, JPasswordField's columns)
 * on each input field class.
 */
@Tags("E2E")
class WTextComponentSwingTest : FunSpec() {
    init {
        context("Defaults") {
            test("focusAccelerator is '\\u0000' (unset)") {
                onUiThreadGet {
                    listOf(WTextField().focusAccelerator, WPasswordField().focusAccelerator, WTextPane().focusAccelerator)
                } shouldContainExactly listOf('\u0000', '\u0000', '\u0000')
            }

            test("a newly created input field has empty actionListeners") {
                onUiThreadGet {
                    listOf(WTextField().actionListeners, WPasswordField().actionListeners, WAutoSuggestBox().actionListeners)
                        .map { it.size }
                } shouldContainExactly listOf(0, 0, 0)
            }

            test("WPasswordField's columns defaults to 0") {
                onUiThreadGet { WPasswordField().columns } shouldBe 0
            }

            test("selectionColor of an input field put in a window is the theme's selection color (not null)") {
                val fields = onUiThreadGet { listOf(WTextField(), WPasswordField(), WTextPane()) }
                fields.forEach { UiTestHarness.attachAndAwaitLoaded(it) }
                try {
                    val colors = onUiThreadGet {
                        listOf(
                            (fields[0] as WTextField).selectionColor,
                            (fields[1] as WPasswordField).selectionColor,
                            (fields[2] as WTextPane).selectionColor,
                        )
                    }
                    colors.forEach { it shouldNotBe null }
                } finally {
                    fields.forEach { UiTestHarness.detach(it) }
                }
            }
        }

        context("Reading and writing") {
            test("focusAccelerator reads back letters in upper case, and '\\u0000' clears it") {
                onUiThreadGet {
                    val field = WTextField()
                    val password = WPasswordField()
                    val pane = WTextPane()
                    field.focusAccelerator = 'n'
                    password.focusAccelerator = 'P'
                    pane.focusAccelerator = '3'
                    val set = listOf(field.focusAccelerator, password.focusAccelerator, pane.focusAccelerator)
                    field.focusAccelerator = '\u0000'
                    set + field.focusAccelerator
                } shouldContainExactly listOf('N', 'P', '3', '\u0000')
            }

            test("focusAccelerator goes into the access key (the same mechanism as WLabel.displayedMnemonic)") {
                onUiThreadGet {
                    val field = WTextField()
                    val label = WLabel("Name")
                    label.labelFor = field
                    label.setDisplayedMnemonic('m')
                    val fromLabel = field.focusAccelerator
                    field.focusAccelerator = 'k'
                    fromLabel to field.focusAccelerator
                } shouldBe ('M' to 'K')
            }

            test("selectionColor reads back the set color, and null returns to the theme color") {
                val fields = onUiThreadGet { listOf(WTextField(), WPasswordField(), WTextPane()) }
                fields.forEach { UiTestHarness.attachAndAwaitLoaded(it) }
                try {
                    val red = WColor(255, 0, 0)
                    val results = onUiThreadGet {
                        val accessors = listOf(
                            { (fields[0] as WTextField).selectionColor } to { c: WColor? -> (fields[0] as WTextField).selectionColor = c },
                            { (fields[1] as WPasswordField).selectionColor } to { c: WColor? -> (fields[1] as WPasswordField).selectionColor = c },
                            { (fields[2] as WTextPane).selectionColor } to { c: WColor? -> (fields[2] as WTextPane).selectionColor = c },
                        )
                        accessors.map { (get, set) ->
                            val theme = get()
                            set(red)
                            val changed = get()
                            set(null)
                            Triple(theme, changed, get())
                        }
                    }
                    results.forEach { (theme, changed, restored) ->
                        changed shouldBe red
                        restored shouldBe theme
                    }
                } finally {
                    fields.forEach { UiTestHarness.detach(it) }
                }
            }

            test("getText(offset, length) returns length characters from offset") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abcdef"
                    val pane = WTextPane()
                    pane.text = "ab\ncd"
                    listOf(field.getText(1, 3), field.getText(0, 6), field.getText(6, 0), pane.getText(1, 3), pane.getText(3, 2))
                } shouldContainExactly listOf("bcd", "abcdef", "", "b\nc", "cd")
            }

            test("read replaces text with the Reader's content (CRLF and CR become LF), and write writes text to the Writer") {
                val (fieldText, paneText, written) = onUiThreadGet {
                    val field = WTextField()
                    field.text = "Old"
                    field.read(StringReader("New text"), null)
                    val pane = WTextPane()
                    pane.read(StringReader("Line 1\r\nLine 2\rLine 3\n"), "Description")
                    val writer = StringWriter()
                    pane.write(writer)
                    Triple(field.text, pane.text, writer.toString())
                }
                fieldText shouldBe "New text"
                paneText shouldBe "Line 1\nLine 2\nLine 3\n"
                written shouldBe "Line 1\nLine 2\nLine 3\n"
            }

            test("WTextField's write writes text as is") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "Text to save"
                    val writer = StringWriter()
                    field.write(writer)
                    writer.toString()
                } shouldBe "Text to save"
            }

            test("WPasswordField's columns and WPasswordField(columns) read back the column count") {
                onUiThreadGet {
                    val field = WPasswordField()
                    field.columns = 12
                    listOf(field.columns, WPasswordField(8).columns)
                } shouldContainExactly listOf(12, 8)
            }
        }

        context("Events") {
            test("WTextField's actionListeners are in the order added, and removing the returned ones stops them being called by postActionEvent") {
                val calls = mutableListOf<String>()
                val (sizes, same) = onUiThreadGet {
                    val field = WTextField()
                    val listener = WActionListener { calls += "listener" }
                    field.addActionListener { calls += "lambda" }
                    field.addActionListenerForJava(listener)
                    field.addActionListenerForJava(Runnable { calls += "runnable" })
                    val listeners = field.actionListeners
                    field.postActionEvent()
                    for (l in listeners) field.removeActionListenerForJava(l)
                    field.postActionEvent()
                    listOf(listeners.size, field.actionListeners.size) to (listeners[1] === listener)
                }
                sizes shouldContainExactly listOf(3, 0)
                same shouldBe true
                calls shouldContainExactly listOf("lambda", "listener", "runnable")
            }

            test("the returned actionListeners of WPasswordField and WAutoSuggestBox can also be removed") {
                val calls = mutableListOf<String>()
                val sizes = onUiThreadGet {
                    val password = WPasswordField()
                    val suggest = WAutoSuggestBox()
                    password.addActionListener { calls += "password" }
                    suggest.addActionListenerForJava(Runnable { calls += "suggest" })
                    val before = listOf(password.actionListeners.size, suggest.actionListeners.size)
                    for (l in password.actionListeners) password.removeActionListenerForJava(l)
                    for (l in suggest.actionListeners) suggest.removeActionListenerForJava(l)
                    password.postActionEvent()
                    suggest.postActionEvent()
                    before + listOf(password.actionListeners.size, suggest.actionListeners.size)
                }
                sizes shouldContainExactly listOf(1, 1, 0, 0)
                calls.shouldBeEmpty()
            }

            test("replacing with read delivers removeUpdate then insertUpdate to the DocumentListener, the same as setText") {
                val events = onUiThreadGet {
                    val field = WTextField()
                    field.text = "ab"
                    val events = mutableListOf<String>()
                    field.addDocumentListener(
                        object : WDocumentListener {
                            override fun insertUpdate(e: WDocumentEvent) {
                                events += "insert ${e.offset} ${e.length}"
                            }

                            override fun removeUpdate(e: WDocumentEvent) {
                                events += "remove ${e.offset} ${e.length}"
                            }

                            override fun changedUpdate(e: WDocumentEvent) = Unit
                        },
                    )
                    field.read(StringReader("xyz"), null)
                    events
                }
                events shouldContainExactly listOf("remove 0 2", "insert 0 3")
            }
        }

        context("Boundaries and exceptions") {
            test("IllegalArgumentException if the getText range is outside the string (corresponds to Swing's BadLocationException)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abc"
                    val pane = WTextPane()
                    pane.text = "abc"
                    listOf<() -> Unit>(
                        { field.getText(-1, 1) },
                        { field.getText(0, -1) },
                        { field.getText(2, 2) },
                        { field.getText(4, 0) },
                        { pane.getText(-1, 1) },
                        { pane.getText(1, 3) },
                    ).map { runCatching(it).exceptionOrNull()?.javaClass }
                }.forEach { it shouldBe IllegalArgumentException::class.java }
            }

            test("putting anything other than letters and digits into focusAccelerator throws IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    onUiThreadGet { WTextField().focusAccelerator = '!' }
                }
            }

            test("putting a negative value into WPasswordField's columns throws IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    onUiThreadGet { WPasswordField().columns = -1 }
                }
            }

            test("reading an empty Reader makes text empty") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abc"
                    field.read(StringReader(""), null)
                    field.text
                } shouldBe ""
            }
        }
    }
}
