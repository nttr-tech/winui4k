package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Verifies WAction / WAbstractAction (in the style of javax.swing.Action / AbstractAction) and WButton.action.
 * Checks that the action's values are reflected in the button's properties, follow changes, and that a click invokes
 * the action. The Java-facing shape is covered by WActionForJavaTest.
 */
class WActionTest : FunSpec() {
    /** An action that records the [WActionEvent]s it receives. */
    private class RecordingAction(name: String? = null, icon: WIcon? = null) : WAbstractAction(name, icon) {
        val events = mutableListOf<WActionEvent>()

        override fun actionPerformed(e: WActionEvent) {
            events += e
        }
    }

    /**
     * A hand-written WAction that only counts registered change listeners (it also confirms that the button works with an
     * implementation that does not use WAbstractAction).
     */
    private class CountingAction : WAction {
        val values = HashMap<String, Any>()
        val listeners = mutableListOf<WActionChangeListener>()
        var performed = 0
        override var isEnabled: Boolean = true

        override fun actionPerformed(e: WActionEvent) {
            performed++
        }

        override fun getValue(key: String): Any? = values[key]

        override fun putValue(key: String, value: Any?) {
            if (value == null) values.remove(key) else values[key] = value
            listeners.toList().forEach { it.actionChanged(this, key) }
        }

        override fun addPropertyChangeListener(listener: WActionChangeListener) {
            listeners += listener
        }

        override fun removePropertyChangeListener(listener: WActionChangeListener) {
            listeners -= listener
        }
    }

    /** Bundles the six button properties tied to the action into a single value. */
    private data class Snapshot(
        val text: String,
        val icon: WIcon?,
        val toolTipText: String?,
        val mnemonic: Int,
        val actionCommand: String?,
        val isEnabled: Boolean,
    )

    private fun WButton.snapshot() = Snapshot(text, icon, toolTipText, mnemonic, actionCommand, isEnabled)

    init {
        test("WAction keys are the same strings as Swing's Action, and WAbstractAction defaults to enabled with no values") {
            listOf(
                WAction.NAME,
                WAction.SMALL_ICON,
                WAction.SHORT_DESCRIPTION,
                WAction.MNEMONIC_KEY,
                WAction.ACTION_COMMAND_KEY,
            ) shouldContainExactly listOf("Name", "SmallIcon", "ShortDescription", "MnemonicKey", "ActionCommandKey")
            val action = RecordingAction()
            action.isEnabled shouldBe true
            action.getValue(WAction.NAME) shouldBe null
            action.getValue(WAction.SMALL_ICON) shouldBe null
        }

        test("the name and icon given to the WAbstractAction constructor become the NAME and SMALL_ICON values") {
            val action = RecordingAction("Save", Symbol.SAVE)
            action.getValue(WAction.NAME) shouldBe "Save"
            action.getValue(WAction.SMALL_ICON) shouldBe Symbol.SAVE
        }

        test("putValue notifies listeners of the key only on change and removes on null; isEnabled changes notify ENABLED") {
            val action = RecordingAction()
            val notified = mutableListOf<String>()
            val listener = WActionChangeListener { _, key -> notified += key }
            action.addPropertyChangeListener(listener)
            action.putValue(WAction.NAME, "Save")
            action.putValue(WAction.NAME, "Save") // The same value does not notify
            action.putValue(WAction.SHORT_DESCRIPTION, "Save to file")
            action.putValue(WAction.NAME, null)
            action.putValue(WAction.NAME, null) // Removing a key that is already absent does not notify either
            action.isEnabled = false
            action.isEnabled = false
            notified shouldContainExactly listOf(WAction.NAME, WAction.SHORT_DESCRIPTION, WAction.NAME, WAction.ENABLED)
            action.getValue(WAction.NAME) shouldBe null
            action.removePropertyChangeListener(listener)
            action.putValue(WAction.NAME, "Open")
            notified.size shouldBe 4
        }

        test("WButton(action) copies every property of the action to the button") {
            val snapshot = onUiThreadGet {
                val action = RecordingAction("Save", Symbol.SAVE)
                action.putValue(WAction.SHORT_DESCRIPTION, "Save to file")
                action.putValue(WAction.MNEMONIC_KEY, 83) // VK_S
                action.putValue(WAction.ACTION_COMMAND_KEY, "save-command")
                action.isEnabled = false
                WButton(action).snapshot()
            }
            snapshot shouldBe Snapshot("Save", Symbol.SAVE, "Save to file", 83, "save-command", false)
        }

        test("the action getter returns the action that was set, and properties without values stay at their defaults (empty text, enabled)") {
            val (same, snapshot) = onUiThreadGet {
                val action = RecordingAction()
                val button = WButton("Original text")
                button.action = action
                (button.action === action) to button.snapshot()
            }
            same shouldBe true
            // Properties without values are left unset (same as Swing's setAction; the original text is cleared too)
            snapshot shouldBe Snapshot("", null, null, 0, "", true)
        }

        test("each time an action value changes, the corresponding button property follows") {
            onUiThreadGet {
                val action = RecordingAction("Save", Symbol.SAVE)
                val button = WButton(action)
                val steps = mutableListOf<Snapshot>()

                action.putValue(WAction.NAME, "Overwrite")
                steps += button.snapshot()
                action.putValue(WAction.SMALL_ICON, Symbol.OPEN_FILE)
                steps += button.snapshot()
                action.putValue(WAction.SHORT_DESCRIPTION, "Overwrite the file")
                steps += button.snapshot()
                action.putValue(WAction.MNEMONIC_KEY, 'U') // A Char is also accepted as a key code
                steps += button.snapshot()
                action.putValue(WAction.ACTION_COMMAND_KEY, "overwrite")
                steps += button.snapshot()
                action.isEnabled = false
                steps += button.snapshot()
                steps
            } shouldContainExactly listOf(
                Snapshot("Overwrite", Symbol.SAVE, null, 0, "Overwrite", true),
                Snapshot("Overwrite", Symbol.OPEN_FILE, null, 0, "Overwrite", true),
                Snapshot("Overwrite", Symbol.OPEN_FILE, "Overwrite the file", 0, "Overwrite", true),
                Snapshot("Overwrite", Symbol.OPEN_FILE, "Overwrite the file", 'U'.code, "Overwrite", true),
                Snapshot("Overwrite", Symbol.OPEN_FILE, "Overwrite the file", 'U'.code, "overwrite", true),
                Snapshot("Overwrite", Symbol.OPEN_FILE, "Overwrite the file", 'U'.code, "overwrite", false),
            )
        }

        test("passing a lowercase Char to MNEMONIC_KEY gives the uppercase key code, same as setMnemonic(Char)") {
            val codes = onUiThreadGet {
                val action = RecordingAction("Save", Symbol.SAVE)
                val button = WButton(action)
                listOf('s', 'S', '7').map { char ->
                    action.putValue(WAction.MNEMONIC_KEY, char)
                    button.mnemonic
                }
            }
            codes shouldContainExactly listOf('S'.code, 'S'.code, '7'.code)
        }

        test("passing a non-alphanumeric Char to MNEMONIC_KEY throws IllegalArgumentException") {
            onUiThreadGet {
                val action = RecordingAction("Save", Symbol.SAVE)
                action.putValue(WAction.MNEMONIC_KEY, '-')
                shouldThrow<IllegalArgumentException> { WButton(action) }
            }
        }

        test("putValue(key, null) and isEnabled=true return the button's properties to unset and enabled") {
            val snapshot = onUiThreadGet {
                val action = RecordingAction("Save", Symbol.SAVE)
                action.putValue(WAction.SHORT_DESCRIPTION, "Hint")
                action.putValue(WAction.MNEMONIC_KEY, 83)
                action.putValue(WAction.ACTION_COMMAND_KEY, "cmd")
                action.isEnabled = false
                val button = WButton(action)
                listOf(
                    WAction.NAME,
                    WAction.SMALL_ICON,
                    WAction.SHORT_DESCRIPTION,
                    WAction.MNEMONIC_KEY,
                    WAction.ACTION_COMMAND_KEY,
                ).forEach { action.putValue(it, null) }
                action.isEnabled = true
                button.snapshot()
            }
            snapshot shouldBe Snapshot("", null, null, 0, "", true)
        }

        test("a click calls the action's actionPerformed once; the event's source is the button and actionCommand is the ACTION_COMMAND_KEY value") {
            val (count, source, command) = onUiThreadGet {
                val action = RecordingAction("Save")
                action.putValue(WAction.ACTION_COMMAND_KEY, "save-command")
                val button = WButton(action)
                button.doClick()
                Triple(action.events.size, action.events.firstOrNull()?.source === button, action.events.firstOrNull()?.actionCommand)
            }
            count shouldBe 1
            source shouldBe true
            command shouldBe "save-command"
        }

        test("without ACTION_COMMAND_KEY, the event's actionCommand is the button's text") {
            onUiThreadGet {
                val action = RecordingAction("Save")
                WButton(action).doClick()
                action.events.single().actionCommand
            } shouldBe "Save"
        }

        test("disabling the action disables the button and doClick does nothing; re-enabling makes it call the action again") {
            val counts = onUiThreadGet {
                val action = RecordingAction("Save")
                val button = WButton(action)
                action.isEnabled = false
                button.doClick()
                val whileDisabled = action.events.size
                action.isEnabled = true
                button.doClick()
                Triple(button.isEnabled, whileDisabled, action.events.size)
            }
            counts shouldBe Triple(true, 0, 1)
        }

        test("listeners added with addActionListener keep working alongside action and receive the same event as the action") {
            val (listenerEvent, actionEvent) = onUiThreadGet {
                val action = RecordingAction("Save")
                val button = WButton("OK")
                val received = mutableListOf<WActionEvent>()
                button.addActionListener { e: WActionEvent -> received += e }
                button.action = action
                button.doClick()
                received.single() to action.events.single()
            }
            listenerEvent.actionCommand shouldBe "Save"
            listenerEvent.source shouldBeSameInstanceAs actionEvent.source
        }

        test("after switching to another action, the previous action's actionPerformed is not called, changes to its values are not followed, and properties without values become unset") {
            val result = onUiThreadGet {
                val first = RecordingAction("First", Symbol.SAVE)
                first.putValue(WAction.SHORT_DESCRIPTION, "First hint")
                val second = RecordingAction("Second")
                val button = WButton(first)
                button.action = second
                val afterReplace = button.snapshot()
                button.doClick()
                first.putValue(WAction.NAME, "First changed")
                first.isEnabled = false
                val afterOldChange = button.snapshot()
                second.putValue(WAction.NAME, "Second changed")
                listOf(afterReplace, afterOldChange, button.snapshot()) to (first.events.size to second.events.size)
            }
            result.first shouldContainExactly listOf(
                Snapshot("Second", null, null, 0, "Second", true),
                Snapshot("Second", null, null, 0, "Second", true),
                Snapshot("Second changed", null, null, 0, "Second changed", true),
            )
            result.second shouldBe (0 to 1)
        }

        test("switching from a disabled action to an enabled one enables the button") {
            onUiThreadGet {
                val disabled = RecordingAction("Disabled").also { it.isEnabled = false }
                val button = WButton(disabled)
                val before = button.isEnabled
                button.action = RecordingAction("Enabled")
                before to button.isEnabled
            } shouldBe (false to true)
        }

        test("setting action to null returns every property to unset, the action is not called, and the change listener is removed") {
            val (snapshot, counts) = onUiThreadGet {
                val action = CountingAction()
                action.values[WAction.NAME] = "Save"
                action.values[WAction.SMALL_ICON] = Symbol.SAVE
                action.values[WAction.SHORT_DESCRIPTION] = "Hint"
                action.values[WAction.MNEMONIC_KEY] = 83
                action.values[WAction.ACTION_COMMAND_KEY] = "cmd"
                action.isEnabled = false
                val button = WButton(action)
                val registered = action.listeners.size
                button.action = null
                action.putValue(WAction.NAME, "Changed later")
                button.doClick()
                button.snapshot() to Triple(registered, action.listeners.size, action.performed)
            }
            snapshot shouldBe Snapshot("", null, null, 0, "", true)
            // While bound there is one change listener, and zero after setting null. The action is never called
            counts shouldBe Triple(1, 0, 0)
        }

        test("setting the same action again does not duplicate the listener or the click invocation") {
            val (listeners, performed) = onUiThreadGet {
                val action = CountingAction()
                val button = WButton(action)
                button.action = action
                button.doClick()
                action.listeners.size to action.performed
            }
            listeners shouldBe 1
            performed shouldBe 1
        }

        test("passing one action to several buttons makes all of them follow it, and each click invokes the action") {
            val result = onUiThreadGet {
                val action = RecordingAction("Save")
                val a = WButton(action)
                val b = WButton(action)
                action.putValue(WAction.NAME, "Shared")
                action.isEnabled = false
                val disabled = listOf(a.snapshot(), b.snapshot())
                action.isEnabled = true
                a.doClick()
                b.doClick()
                disabled to action.events.map { it.source }
            }
            result.first shouldContainExactly List(2) { Snapshot("Shared", null, null, 0, "Shared", false) }
            result.second.size shouldBe 2
        }

        test("values of the wrong type (a number for NAME, a string for SMALL_ICON, a string for MNEMONIC_KEY) throw IllegalArgumentException") {
            onUiThreadGet {
                val action = RecordingAction("Save")
                val button = WButton(action)
                val errors = listOf(WAction.NAME to 1, WAction.SMALL_ICON to "icon", WAction.MNEMONIC_KEY to "S").map { (key, value) ->
                    runCatching { action.putValue(key, value) }.exceptionOrNull()
                }
                errors.map { it is IllegalArgumentException } to button.text
            } shouldBe (List(3) { true } to "Save")
        }

        test("a mnemonic key code that is not a letter or digit throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                onUiThreadGet {
                    val action = RecordingAction("Save")
                    action.putValue(WAction.MNEMONIC_KEY, 32)
                    WButton(action)
                }
            }
        }
    }
}
