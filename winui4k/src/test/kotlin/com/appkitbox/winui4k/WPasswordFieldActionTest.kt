package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WPasswordField's ActionListener (JPasswordField is a JTextField, so it is called when Enter is pressed), and that
 * Enter presses the WFrame's default button when there is no listener.
 * The Enter key is sent with [TestKeyboard] to the input child window of the shared window.
 */
class WPasswordFieldActionTest : FunSpec() {
    private val frame by lazy { UiTestHarness.sharedFrame() }

    /** A form with a password field, an input field and a default button. Records what happened in [events]. */
    private class Form {
        val panel = WPanel()
        val events = mutableListOf<String>()
        val password = WPasswordField()
        val ok = WButton("OK")

        init {
            ok.addActionListener { events += "OK" }
            listOf(password, ok).forEach { panel.add(it) }
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

    /** Moves focus to the password field, presses Enter, waits until [expected] is recorded, then returns the records after waiting a little. */
    private suspend fun pressEnter(form: Form, expected: List<String>): List<String> {
        onUiThread {
            form.events.clear()
            check(form.password.requestFocusInWindow()) { "could not move focus" }
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
        test("actionCommand is null by default and the password is not put in the event (unlike JTextField, it is not the entered string)") {
            onUiThreadGet {
                val field = WPasswordField()
                field.password = "secret"
                val initial = field.actionCommand
                field.actionCommand = "login"
                initial to field.actionCommand
            } shouldBe (null to "login")
        }

        test("pressing Enter calls the ActionListener once, with the password field as source and the set string as actionCommand") {
            val form = showForm()
            try {
                val sources = mutableListOf<Any?>()
                onUiThread {
                    form.password.password = "secret"
                    form.password.actionCommand = "login"
                    form.password.addActionListener { e ->
                        sources += e.source
                        form.events += "password:${e.actionCommand}:${e.modifiers}"
                    }
                }
                pressEnter(form, listOf("password:login:0")) shouldContainExactly listOf("password:login:0")
                onUiThreadGet { sources.toList() } shouldContainExactly listOf(form.password)
                onUiThreadGet { form.password.password } shouldBe "secret"
            } finally {
                closeForm(form)
            }
        }

        test("on Enter in a password field without actionCommand set, the event's actionCommand is null") {
            val form = showForm()
            try {
                onUiThread {
                    form.password.password = "secret"
                    form.password.addActionListener { e -> form.events += "password:${e.actionCommand}" }
                }
                pressEnter(form, listOf("password:null")) shouldContainExactly listOf("password:null")
            } finally {
                closeForm(form)
            }
        }

        test("Enter while there is an ActionListener does not press the default button (same as Swing)") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    form.password.addActionListener { form.events += "password" }
                }
                pressEnter(form, listOf("password")) shouldContainExactly listOf("password")
            } finally {
                closeForm(form)
            }
        }

        test("multiple listeners are called in registration order, and a listener removed with removeActionListener is not called") {
            val form = showForm()
            try {
                onUiThread {
                    val removed: (WActionEvent) -> Unit = { form.events += "removed" }
                    form.password.addActionListener { form.events += "first" }
                    form.password.addActionListener(removed)
                    form.password.addActionListener { form.events += "second" }
                    form.password.removeActionListener(removed)
                }
                pressEnter(form, listOf("first", "second")) shouldContainExactly listOf("first", "second")
            } finally {
                closeForm(form)
            }
        }

        test("Enter in a password field without listeners presses the default button only once") {
            val form = showForm()
            try {
                onUiThread { frame.defaultButton = form.ok }
                pressEnter(form, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("removing all ActionListeners makes Enter press the default button") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    val listener: (WActionEvent) -> Unit = { form.events += "password" }
                    form.password.addActionListener(listener)
                    form.password.removeActionListener(listener)
                }
                pressEnter(form, listOf("OK")) shouldContainExactly listOf("OK")
            } finally {
                closeForm(form)
            }
        }

        test("a disabled default button is not pressed by Enter, and Enter in a password field with neither listeners nor a default button does nothing") {
            val form = showForm()
            try {
                onUiThread {
                    frame.defaultButton = form.ok
                    form.ok.isEnabled = false
                    form.password.password = "unchanged"
                }
                pressEnter(form, emptyList()) shouldContainExactly emptyList()
                onUiThreadGet { form.password.password } shouldBe "unchanged"
            } finally {
                closeForm(form)
            }
        }

        test("postActionEvent calls the same listeners as pressing Enter and does not press the default button") {
            onUiThreadGet {
                val field = WPasswordField()
                val events = mutableListOf<String>()
                field.actionCommand = "cmd"
                field.addActionListener { e -> events += "first:${e.actionCommand}" }
                field.addActionListener { events += "second" }
                field.postActionEvent()
                events.toList()
            } shouldContainExactly listOf("first:cmd", "second")
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 700L
    }
}
