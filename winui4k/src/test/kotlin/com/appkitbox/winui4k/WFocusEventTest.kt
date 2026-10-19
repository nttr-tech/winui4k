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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the FocusListener-style [WComponent.addFocusListener]. Focus is moved with requestFocusInWindow, and since
 * GotFocus / LostFocus arrive asynchronously, each test waits for the record to match the expectation and then checks that
 * no extra notification arrives after a short wait.
 */
class WFocusEventTest : FunSpec() {
    /** A form with a text field, a button and a numeric field. Records what the listeners received in [events]. */
    private class Form {
        val panel = WPanel()
        val field = WTextField()
        val button = WButton("OK")
        val spinner = WSpinner(1.0)
        val events = mutableListOf<String>()
        val received = mutableListOf<WFocusEvent>()

        init {
            listOf(field, button, spinner).forEach { panel.add(it) }
        }

        /** Adds to [component] a listener that records received events as "[name]:gained" / "[name]:lost". */
        fun record(component: WComponent, name: String): WFocusListener = object : WFocusListener {
            override fun focusGained(e: WFocusEvent) {
                events += "$name:gained"
                received += e
            }

            override fun focusLost(e: WFocusEvent) {
                events += "$name:lost"
                received += e
            }
        }.also { component.addFocusListener(it) }
    }

    private fun showForm(): Form {
        val form = onUiThreadGet { Form() }
        UiTestHarness.attachAndAwaitLoaded(form.panel)
        return form
    }

    /** Waits for the record to become [expected], and checks that it does not change after a short wait. */
    private suspend fun awaitEvents(form: Form, expected: List<String>) {
        eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe expected }
        Thread.sleep(SETTLE_MILLIS)
        onUiThreadGet { form.events.toList() } shouldContainExactly expected
    }

    private fun focus(component: WComponent) = onUiThread { check(component.requestFocusInWindow()) { "could not move focus" } }

    /** Whether the element that currently has focus (FocusManager.GetFocusedElement) is [component] itself (false for template parts or inside a Popup). */
    private fun focusIsOnItself(component: WComponent): Boolean = onUiThreadGet {
        val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return@onUiThreadGet false
        val focused = try {
            Xaml.focusedElement(root)
        } finally {
            root.release()
        } ?: return@onUiThreadGet false
        try {
            Xaml.sameObject(focused, component.inspectable)
        } finally {
            focused.release()
        }
    }

    init {
        test("gaining focus via requestFocusInWindow calls focusGained exactly once, with that component as the source") {
            val form = showForm()
            try {
                onUiThread { form.record(form.field, "field") }
                focus(form.field)
                awaitEvents(form, listOf("field:gained"))
                val event = onUiThreadGet { form.received.single() }
                event.source shouldBeSameInstanceAs form.field
                event.component shouldBeSameInstanceAs form.field
                event.id shouldBe WFocusEvent.FOCUS_GAINED
                event.isTemporary.shouldBeFalse()
                event.oppositeComponent.shouldBeNull()
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("when focus moves to another component, focusLost of the previous component and then focusGained of the next one are called") {
            val form = showForm()
            try {
                focus(form.field)
                onUiThread {
                    form.record(form.field, "field")
                    form.record(form.button, "button")
                }
                focus(form.button)
                awaitEvents(form, listOf("field:lost", "button:gained"))
                onUiThreadGet { form.received.map { it.id } } shouldContainExactly
                    listOf(WFocusEvent.FOCUS_LOST, WFocusEvent.FOCUS_GAINED)
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("a panel's listener is not called when a child inside the panel gains or loses focus (as in Swing)") {
            val form = showForm()
            try {
                onUiThread {
                    form.record(form.panel, "panel")
                    form.record(form.button, "button")
                }
                focus(form.field)
                focus(form.button)
                awaitEvents(form, listOf("button:gained"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("WSpinner (NumberBox), whose inner text field receives focus, reports that field's focus as its own focusGained / focusLost") {
            val form = showForm()
            try {
                onUiThread { form.record(form.spinner, "spinner") }
                // NumberBox itself has IsTabStop false, so UIElement.Focus passes focus to the TextBox inside its template
                onUiThread { check(form.spinner.focus(XamlInterop.FocusState_Programmatic)) { "could not move focus" } }
                awaitEvents(form, listOf("spinner:gained"))
                focus(form.button)
                awaitEvents(form, listOf("spinner:gained", "spinner:lost"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("opening and closing a WComboBox drop-down keeps focus while focus is on the drop-down items (as in JComboBox)") {
            val form = showForm()
            val combo = onUiThreadGet {
                WComboBox().also { combo ->
                    listOf("Small", "Medium", "Large").forEach { combo.addItem(it) }
                    form.panel.add(combo)
                }
            }
            try {
                eventually(3.seconds) { onUiThreadGet { combo.actualWidth > 0.0 }.shouldBeTrue() }
                onUiThread { form.record(combo, "combo") }
                focus(combo)
                awaitEvents(form, listOf("combo:gained"))
                // On open, WinUI moves focus to an item (a ComboBoxItem inside the drop-down Popup) (SetFocusedItem in ComboBox::OnOpen)
                onUiThread { combo.isDropDownOpen = true }
                eventually(3.seconds) { focusIsOnItself(combo).shouldBeFalse() }
                onUiThreadGet { combo.hasFocus() }.shouldBeTrue()
                onUiThread { combo.isDropDownOpen = false }
                eventually(3.seconds) { focusIsOnItself(combo).shouldBeTrue() }
                awaitEvents(form, listOf("combo:gained"))
                focus(form.button)
                awaitEvents(form, listOf("combo:gained", "combo:lost"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("a listener added while the component has focus still gets focusLost when focus moves away") {
            val form = showForm()
            try {
                focus(form.field)
                eventually(3.seconds) { onUiThreadGet { form.field.hasFocus() }.shouldBeTrue() }
                onUiThread { form.record(form.field, "field") }
                focus(form.button)
                awaitEvents(form, listOf("field:lost"))
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("adding the same listener twice calls it twice, one removeFocusListener removes only one, and another stops the calls") {
            val form = showForm()
            try {
                val listener = onUiThreadGet { form.record(form.field, "field").also { form.field.addFocusListener(it) } }
                focus(form.field)
                awaitEvents(form, listOf("field:gained", "field:gained"))
                onUiThread { form.field.removeFocusListener(listener) }
                focus(form.button)
                awaitEvents(form, listOf("field:gained", "field:gained", "field:lost"))
                onUiThread { form.field.removeFocusListener(listener) }
                focus(form.field)
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { form.events.toList() } shouldContainExactly listOf("field:gained", "field:gained", "field:lost")
            } finally {
                UiTestHarness.detach(form.panel)
            }
        }

        test("focusListeners returns the listeners in the order added, and adding or removing null or removing an unadded listener does nothing") {
            onUiThreadGet {
                val field = WTextField()
                val first = object : WFocusAdapter() {}
                val second = object : WFocusAdapter() {}
                val initial = field.focusListeners.toList()
                field.addFocusListener(null)
                field.addFocusListener(first)
                field.addFocusListener(second)
                field.removeFocusListener(null)
                field.removeFocusListener(object : WFocusAdapter() {})
                val added = field.focusListeners.toList()
                field.removeFocusListener(first)
                listOf(initial, added, field.focusListeners.toList()) shouldBe listOf(emptyList(), listOf(first, second), listOf(second))
            }
        }

        test("WFocusAdapter lets you skip unused methods, and FOCUS_GAINED / FOCUS_LOST have the same values as AWT's FocusEvent") {
            val gained = mutableListOf<Int>()
            val adapter = object : WFocusAdapter() {
                override fun focusGained(e: WFocusEvent) {
                    gained += e.id
                }
            }
            val event = onUiThreadGet { WFocusEvent(WButton(), WFocusEvent.FOCUS_GAINED) }
            adapter.focusGained(event)
            adapter.focusLost(event)
            gained shouldContainExactly listOf(1004)
            listOf(WFocusEvent.FOCUS_FIRST, WFocusEvent.FOCUS_LAST, WFocusEvent.FOCUS_GAINED, WFocusEvent.FOCUS_LOST) shouldBe
                listOf(1004, 1005, 1004, 1005)
        }

        test("focusListeners of a component without listeners is empty") {
            onUiThreadGet { WButton().focusListeners.toList() }.shouldBeEmpty()
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
