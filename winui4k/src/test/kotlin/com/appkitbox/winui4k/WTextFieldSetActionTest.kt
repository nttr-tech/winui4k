package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [WTextField.action], like JTextField.setAction / getAction. The expected values are those from the same operations on Swing's JTextField
 * (JTextField.configurePropertiesFromAction copies only enabled, toolTipText and actionCommand, not NAME etc.).
 */
@Tags("E2E")
class WTextFieldSetActionTest : FunSpec() {
    /** An action that records the source and actionCommand when called. */
    private class Recording(name: String = "Search") : WAbstractAction(name) {
        val calls = mutableListOf<String>()

        override fun actionPerformed(e: WActionEvent) {
            calls += "${(e.source as? WTextField)?.text}:${e.actionCommand}"
        }
    }

    init {
        context("Defaults") {
            test("action is null by default") {
                onUiThreadGet { WTextField().action } shouldBe null
            }
        }

        context("Reading and writing") {
            test("setAction copies the action's enabled, SHORT_DESCRIPTION and ACTION_COMMAND_KEY, not NAME (the text field's text does not change)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "Tokyo"
                    val action = Recording()
                    action.putValue(WAction.SHORT_DESCRIPTION, "Search by place name")
                    action.putValue(WAction.ACTION_COMMAND_KEY, "search")
                    action.isEnabled = false
                    field.action = action
                    listOf(field.action === action, field.isEnabled, field.toolTipText, field.actionCommand, field.text)
                } shouldBe listOf(true, false, "Search by place name", "search", "Tokyo")
            }

            test("it follows changes to the action's values after binding") {
                onUiThreadGet {
                    val field = WTextField()
                    val action = Recording()
                    field.action = action
                    action.putValue(WAction.SHORT_DESCRIPTION, "Later")
                    action.isEnabled = false
                    field.toolTipText to field.isEnabled
                } shouldBe ("Later" to false)
            }

            test("resetting it to null puts the copied items in the no-value state (the tooltip is null, actionCommand falls back to the text, and it becomes enabled)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abc"
                    val action = Recording()
                    action.putValue(WAction.SHORT_DESCRIPTION, "Hint")
                    action.putValue(WAction.ACTION_COMMAND_KEY, "cmd")
                    action.isEnabled = false
                    field.action = action
                    field.action = null
                    listOf(field.action, field.toolTipText, field.actionCommand, field.isEnabled)
                } shouldBe listOf(null, null, "abc", true)
            }
        }

        context("Events") {
            test("postActionEvent calls the action's actionPerformed with the text field as the source, and it is one entry in actionListeners") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "Osaka"
                    val action = Recording()
                    field.action = action
                    field.postActionEvent()
                    action.calls.toList() to field.actionListeners.size
                } shouldBe (listOf("Osaka:Osaka") to 1)
            }

            test("after switching to another action, the previous action is not called and changes to its values are not followed") {
                onUiThreadGet {
                    val field = WTextField()
                    val first = Recording()
                    val second = Recording()
                    field.action = first
                    field.action = second
                    first.putValue(WAction.SHORT_DESCRIPTION, "Previous action")
                    field.postActionEvent()
                    listOf(first.calls.size, second.calls.size, field.toolTipText, field.actionListeners.size)
                } shouldBe listOf(0, 1, null, 1)
            }
        }

        context("Input") {
            test("pressing Enter calls the action, and the default button of the same window is not pressed (because the action is also an ActionListener)") {
                val frame = UiTestHarness.sharedFrame()
                val events = mutableListOf<String>()
                val action = Recording()
                val (panel, field) = onUiThreadGet {
                    val panel = WPanel()
                    val field = WTextField().also { it.text = "Nagoya" }
                    val ok = WButton("OK").also { it.addActionListener { events += "OK" } }
                    panel.add(field)
                    panel.add(ok)
                    field.action = action
                    frame.defaultButton = ok
                    panel to field
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                try {
                    onUiThread { check(field.requestFocusInWindow()) { "Cannot move the focus" } }
                    TestKeyboard.pressEnter()
                    eventually(3.seconds) { onUiThreadGet { action.calls.toList() } shouldBe listOf("Nagoya:Nagoya") }
                    Thread.sleep(SETTLE_MILLIS)
                    onUiThreadGet { events.toList() } shouldBe emptyList()
                } finally {
                    onUiThread { frame.defaultButton = null }
                    UiTestHarness.detach(panel)
                }
            }
        }
    }

    private companion object {
        /** The time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
