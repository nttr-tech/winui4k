package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies WTextField's ActionListener (called when Enter is pressed in a single-line field, as in JTextField) and that
 * Enter presses the WFrame's default button when there is no listener.
 * The Enter key is sent with [TestKeyboard] to the input child window of the shared window.
 */
class WTextFieldActionTest : FunSpec() {
    private val frame by lazy { UiTestHarness.sharedFrame() }

    /** A form with single-line and multi-line fields and a default button. Records what happens in [events]. */
    private class Form {
        val panel = WPanel()
        val events = mutableListOf<String>()
        val field = WTextField()
        val multiLine = WTextField().also { it.acceptsReturn = true }
        val ok = WButton("OK")

        init {
            ok.addActionListener { events += "OK" }
            listOf(field, multiLine, ok).forEach { panel.add(it) }
        }
    }

    private fun showForm(): Form {
        val form = onUiThreadGet { Form() }
        UiTestHarness.attachAndAwaitLoaded(form.panel)
        return form
    }

    private fun closeForm(form: Form) {
        onUiThread { frame.defaultButton = null }
        UiTestHarness.detach(form.panel)
    }

    /**
     * Moves focus to [target], presses Enter, waits until [expected] is recorded, then waits a little longer and returns
     * the record.
     */
    private suspend fun pressEnterOn(form: Form, target: WComponent, expected: List<String>): List<String> {
        onUiThread {
            form.events.clear()
            check(target.requestFocusInWindow()) { "could not move focus" }
        }
        TestKeyboard.pressEnter()
        eventually(3.seconds) {
            onUiThreadGet { form.events.toList() } shouldBe expected
        }
        // Also verify that no delayed second notification or click arrives
        Thread.sleep(SETTLE_MILLIS)
        return onUiThreadGet { form.events.toList() }
    }

    init {
        test("actionCommand returns the field's text when unset, the set value once set, and the text again after null") {
            onUiThreadGet {
                val field = WTextField()
                field.text = "Tokyo"
                val initial = field.actionCommand
                field.actionCommand = "search"
                val custom = field.actionCommand
                field.actionCommand = null
                listOf(initial, custom, field.actionCommand)
            } shouldContainExactly listOf("Tokyo", "search", "Tokyo")
        }

        test("pressing Enter in a single-line field calls the ActionListener once; source is the field and actionCommand is the entered text") {
            val form = showForm()
            try {
                val sources = mutableListOf<Any?>()
                onUiThread {
                    form.field.text = "Tokyo"
                    form.field.addActionListener { e ->
                        sources += e.source
                        form.events += "field:${e.actionCommand}:${e.modifiers}"
                    }
                }
                pressEnterOn(form, form.field, listOf("field:Tokyo:0")) shouldContainExactly listOf("field:Tokyo:0")
                onUiThreadGet { sources.toList() } shouldContainExactly listOf(form.field)
            } finally {
                closeForm(form)
            }
        }

        test("Enter in a field with actionCommand set puts that actionCommand in the event") {
            val form = showForm()
            try {
                onUiThread {
                    form.field.text = "Tokyo"
                    form.field.actionCommand = "search"
                    form.field.addActionListener { e -> form.events += "field:${e.actionCommand}" }
                }
                pressEnterOn(form, form.field, listOf("field:search")) shouldContainExactly listOf("field:search")
            } finally {
                closeForm(form)
            }
        }

        test("Enter in a single-line field with an ActionListener does not press the default button (same as Swing)") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    form.field.addActionListener { form.events += "field" }
                }
                pressEnterOn(form, form.field, listOf("field")) shouldContainExactly listOf("field")
            } finally {
                closeForm(form)
            }
        }

        test("multiple listeners are called in registration order, and a listener removed with removeActionListener is not called") {
            val form = showForm()
            try {
                onUiThread {
                    val removed: (WActionEvent) -> Unit = { form.events += "removed" }
                    form.field.addActionListener { form.events += "first" }
                    form.field.addActionListener(removed)
                    form.field.addActionListener { form.events += "second" }
                    form.field.removeActionListener(removed)
                }
                pressEnterOn(form, form.field, listOf("first", "second")) shouldContainExactly listOf("first", "second")
            } finally {
                closeForm(form)
            }
        }

        test("removing all ActionListeners makes Enter press the default button") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    val listener: (WActionEvent) -> Unit = { form.events += "field" }
                    form.field.addActionListener(listener)
                    form.field.removeActionListener(listener)
                }
                pressEnterOn(form, form.field, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("Enter in a multi-line (acceptsReturn) field inserts a line break and calls neither the ActionListener nor the default button") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    form.multiLine.text = "First line"
                    form.multiLine.addActionListener { form.events += "multiLine" }
                    form.multiLine.addTextChangedListener { form.events += "changed" }
                }
                // Inserting a line break delivers TextChanged once
                pressEnterOn(form, form.multiLine, listOf("changed")) shouldContainExactly listOf("changed")
                val text = onUiThreadGet { form.multiLine.text }
                text.length shouldBe "First line".length + 1
                text.count { it == '\r' } shouldBe 1
            } finally {
                closeForm(form)
            }
        }

        test("a field with only wrapping (textWrapping = WRAP) enabled is treated as single-line, and Enter calls the ActionListener once") {
            // With acceptsReturn = false and wrapping on, TextBox does not pass Enter to the text engine (ShouldProcessKeyMessage in TextBoxBase.cpp)
            val form = showForm()
            try {
                onUiThread {
                    form.field.textWrapping = TextWrapping.WRAP
                    form.field.addActionListener { form.events += "field" }
                }
                pressEnterOn(form, form.field, listOf("field")) shouldContainExactly listOf("field")
                onUiThreadGet { form.field.text } shouldBe ""
            } finally {
                closeForm(form)
            }
        }

        test("Enter in a field with only wrapping enabled and no listener presses the default button only once (not twice)") {
            val form = showForm()
            try {
                onUiThread {
                    form.field.textWrapping = TextWrapping.WRAP
                    frame.defaultButton = form.ok
                }
                pressEnterOn(form, form.field, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("measured: Enter's KeyDown in a single-line TextBox reaches a handler attached to the element before it is marked handled") {
            // TextBox passes Enter to the text engine (RichEdit) and marks it handled (CTextBoxBase::OnKeyDown in TextBoxBase.cpp)
            // after the handlers attached to the element are called (that the global accelerator then does not run is measured in WDefaultButtonTest).
            // So as not to rely on this order, WTextField subscribes with UIElement.AddHandler(KeyDownEvent, handledEventsToo = true), which also receives handled events
            val form = showForm()
            val textBox = onUiThreadGet { XamlElement.load("<TextBox />") }
            onUiThread { form.panel.add(textBox) }
            try {
                val removeHandler = onUiThreadGet {
                    textBox.onKeyDown { if (it.key == VirtualKey.ENTER.native) form.events += "KeyDown" }
                    Xaml.addHandlerHandledToo(
                        textBox,
                        XamlInterop.IUIElementStatics_get_KeyDownEvent,
                        XamlInterop.IID_KeyEventHandler,
                        FoundationInterop.IID_IReference_KeyEventHandler,
                    ) { _, args ->
                        val event = XamlKeyEvent(ComPtr(args))
                        if (event.key == VirtualKey.ENTER.native) form.events += "handledEventsToo:handled=${event.handled}"
                    }
                }
                try {
                    val expected = listOf("KeyDown", "handledEventsToo:handled=false")
                    pressEnterOn(form, textBox, expected) shouldContainExactly expected
                } finally {
                    onUiThread { removeHandler() }
                }
            } finally {
                closeForm(form)
            }
        }

        test("a handler removed by calling the return value of Xaml.addHandlerHandledToo is not called on later Enter presses") {
            val form = showForm()
            val textBox = onUiThreadGet { XamlElement.load("<TextBox />") }
            onUiThread { form.panel.add(textBox) }
            try {
                val removeHandler = onUiThreadGet {
                    Xaml.addHandlerHandledToo(
                        textBox,
                        XamlInterop.IUIElementStatics_get_KeyDownEvent,
                        XamlInterop.IID_KeyEventHandler,
                        FoundationInterop.IID_IReference_KeyEventHandler,
                    ) { _, args -> if (XamlKeyEvent(ComPtr(args)).key == VirtualKey.ENTER.native) form.events += "handler" }
                }
                pressEnterOn(form, textBox, listOf("handler")) shouldContainExactly listOf("handler")
                onUiThread {
                    removeHandler()
                    removeHandler() // The second call does nothing
                }
                pressEnterOn(form, textBox, emptyList()) shouldContainExactly emptyList()
            } finally {
                closeForm(form)
            }
        }

        test("Enter in a field with neither a listener nor a default button does nothing and leaves the text unchanged") {
            val form = showForm()
            try {
                onUiThread { form.field.text = "Unchanged" }
                pressEnterOn(form, form.field, emptyList()) shouldContainExactly emptyList()
                onUiThreadGet { form.field.text } shouldBe "Unchanged"
            } finally {
                closeForm(form)
            }
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 700L
    }
}
