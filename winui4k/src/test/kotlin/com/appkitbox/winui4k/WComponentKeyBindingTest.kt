package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for key bindings like JComponent's InputMap / ActionMap ([WComponent.getInputMap] / [WComponent.actionMap] /
 * [WComponent.registerKeyboardAction]). The expected values are decided from the contracts of Swing's JComponent.processKeyBindings and
 * SwingUtilities.notifyAction (the order of conditions, disabled actions do not consume, source and actionCommand).
 * Keys are sent with [TestKeyboard] (modifier keys are not delivered, so keys without modifiers such as F7 are pressed).
 */
@Tags("E2E")
class WComponentKeyBindingTest : FunSpec() {
    /** A form with an inner panel (with a button inside), an outer button, a target button and a text field arranged in a root panel. */
    private class Form {
        val root = WPanel()
        val inner = WPanel()
        val innerButton = WButton("Inner")
        val outerButton = WButton("Outer")
        val target = WButton("Target")
        val field = WTextField()
        val events = mutableListOf<String>()
        private val names = mapOf<Any, String>(root to "root", inner to "inner", innerButton to "innerButton", outerButton to "outerButton", target to "target", field to "field")

        init {
            inner.add(innerButton)
            listOf(inner, outerButton, target, field).forEach { root.add(it) }
            target.addActionListener { events += "click" }
            field.addActionListener { events += "fieldAction" }
        }

        /** An action that records "name:source name:actionCommand" when called. */
        fun action(name: String, enabled: Boolean = true): WAction = object : WAbstractAction(name) {
            override fun actionPerformed(e: WActionEvent) {
                events += "$name:${names[e.source]}:${e.actionCommand}"
            }
        }.also { it.isEnabled = enabled }

        /** Maps [stroke] to the action [name] in the map for [condition] of [component]. */
        fun bind(component: WComponent, condition: Int, stroke: String, name: String, enabled: Boolean = true) {
            component.getInputMap(condition).put(WKeyStroke.getKeyStroke(stroke), name)
            component.actionMap.put(name, action(name, enabled))
        }
    }

    private fun showForm(): Form {
        val form = onUiThreadGet { Form() }
        UiTestHarness.attachAndAwaitLoaded(form.root)
        return form
    }

    private fun focus(form: Form, component: WComponent) = onUiThread {
        form.events.clear()
        check(component.requestFocusInWindow()) { "Cannot move the focus" }
    }

    /** Waits until the record becomes [expected], and confirms it does not change after waiting a little. */
    private suspend fun awaitEvents(form: Form, expected: List<String>) {
        eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe expected }
        Thread.sleep(SETTLE_MILLIS)
        onUiThreadGet { form.events.toList() } shouldContainExactly expected
    }

    /** Moves the focus to [component], presses and releases [virtualKey], and confirms the record becomes [expected]. */
    private suspend fun pressOn(form: Form, component: WComponent, expected: List<String>, virtualKey: Int = VK_F7, scanCode: Int = SCAN_F7) {
        focus(form, component)
        TestKeyboard.press(virtualKey, scanCode)
        awaitEvents(form, expected)
    }

    private suspend fun withForm(block: suspend (Form) -> Unit) {
        val form = showForm()
        try {
            block(form)
        } finally {
            UiTestHarness.detach(form.root)
        }
    }

    init {
        context("Defaults") {
            test("getInputMap() is the WHEN_FOCUSED map and the same instance however many times it is read, a new map's keys is null, and the registered keystrokes are empty") {
                onUiThread {
                    val button = WButton("OK")
                    button.inputMap shouldBeSameInstanceAs button.getInputMap(WComponent.WHEN_FOCUSED)
                    button.actionMap shouldBeSameInstanceAs button.actionMap
                    button.inputMap.keys().shouldBeNull()
                    button.inputMap.allKeys().shouldBeNull()
                    button.actionMap.keys().shouldBeNull()
                    button.registeredKeyStrokes.toList().shouldBeEmpty()
                    button.getConditionForKeyStroke(WKeyStroke.getKeyStroke("F7")) shouldBe WComponent.UNDEFINED_CONDITION
                    listOf(
                        WComponent.WHEN_FOCUSED,
                        WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                        WComponent.WHEN_IN_FOCUSED_WINDOW,
                        WComponent.UNDEFINED_CONDITION,
                    ) shouldBe listOf(0, 1, 2, -1)
                }
            }
        }

        context("Reading and writing") {
            test("registerKeyboardAction goes into the condition's map and the action map, and is removed by unregisterKeyboardAction and resetKeyboardActions") {
                onUiThread {
                    val panel = WPanel()
                    val f7 = WKeyStroke.getKeyStroke("F7")!!
                    val f8 = WKeyStroke.getKeyStroke("F8")!!
                    val listener = WActionListener { }
                    panel.registerKeyboardAction(listener, "cmd", f7, WComponent.WHEN_IN_FOCUSED_WINDOW)
                    panel.registerKeyboardAction(listener, f8, WComponent.WHEN_FOCUSED)
                    panel.getConditionForKeyStroke(f7) shouldBe WComponent.WHEN_IN_FOCUSED_WINDOW
                    panel.getConditionForKeyStroke(f8) shouldBe WComponent.WHEN_FOCUSED
                    panel.registeredKeyStrokes.toList() shouldContainExactly listOf(f8, f7)
                    val key = panel.getInputMap(WComponent.WHEN_IN_FOCUSED_WINDOW)[f7]
                    panel.actionMap[key]!!.getValue(WAction.ACTION_COMMAND_KEY) shouldBe "cmd"
                    panel.actionMap.size() shouldBe 2
                    panel.unregisterKeyboardAction(f7)
                    panel.getConditionForKeyStroke(f7) shouldBe WComponent.UNDEFINED_CONDITION
                    panel.actionMap.size() shouldBe 1
                    panel.getInputMap(WComponent.WHEN_FOCUSED).parent = WInputMap().also { it.put(f7, "parent") }
                    panel.resetKeyboardActions()
                    panel.registeredKeyStrokes.toList() shouldContainExactly listOf(f7) // The parent map is not removed (the same as Swing)
                    panel.actionMap.size() shouldBe 0
                }
            }
        }

        context("Boundaries and exceptions") {
            test("passing anything other than the three conditions to getInputMap and registerKeyboardAction throws IllegalArgumentException") {
                onUiThread {
                    val button = WButton("OK")
                    shouldThrow<IllegalArgumentException> { button.getInputMap(3) }
                    shouldThrow<IllegalArgumentException> { button.getInputMap(WComponent.UNDEFINED_CONDITION) }
                    shouldThrow<IllegalArgumentException> {
                        button.registerKeyboardAction({ }, WKeyStroke.getKeyStroke("F7"), 5)
                    }
                }
            }
        }

        context("Input") {
            test("WHEN_FOCUSED works only when that component has the focus, with that component as the source and a null actionCommand") {
                withForm { form ->
                    onUiThread { form.bind(form.target, WComponent.WHEN_FOCUSED, "F7", "save") }
                    pressOn(form, form.target, listOf("save:target:null"))
                    pressOn(form, form.outerButton, emptyList())
                }
            }

            test("WHEN_ANCESTOR_OF_FOCUSED_COMPONENT works when a descendant has the focus and not when the focus is outside") {
                withForm { form ->
                    onUiThread { form.bind(form.inner, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "F7", "find") }
                    pressOn(form, form.innerButton, listOf("find:inner:null"))
                    pressOn(form, form.outerButton, emptyList())
                }
            }

            test("WHEN_FOCUSED runs before an ancestor's WHEN_ANCESTOR_OF_FOCUSED_COMPONENT and consumes the key, and a disabled action does not consume it") {
                withForm { form ->
                    onUiThread {
                        form.bind(form.innerButton, WComponent.WHEN_FOCUSED, "F7", "focused")
                        form.bind(form.inner, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "F7", "ancestor")
                        form.bind(form.root, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "F7", "rootAncestor")
                    }
                    pressOn(form, form.innerButton, listOf("focused:innerButton:null"))
                    onUiThread { form.innerButton.actionMap["focused"]!!.isEnabled = false }
                    pressOn(form, form.innerButton, listOf("ancestor:inner:null"))
                    onUiThread { form.inner.actionMap["ancestor"]!!.isEnabled = false }
                    pressOn(form, form.innerButton, listOf("rootAncestor:root:null"))
                }
            }

            test("WHEN_IN_FOCUSED_WINDOW works wherever the focus is in the same window, and not for hidden or disabled components") {
                withForm { form ->
                    onUiThread { form.bind(form.target, WComponent.WHEN_IN_FOCUSED_WINDOW, "F7", "save") }
                    pressOn(form, form.outerButton, listOf("save:target:null"))
                    pressOn(form, form.innerButton, listOf("save:target:null"))
                    onUiThread { form.target.isEnabled = false }
                    pressOn(form, form.outerButton, emptyList())
                    onUiThread {
                        form.target.isEnabled = true
                        form.target.isVisible = false
                    }
                    pressOn(form, form.outerButton, emptyList())
                }
            }

            test("an ancestor's WHEN_ANCESTOR_OF_FOCUSED_COMPONENT comes before WHEN_IN_FOCUSED_WINDOW, and WinUI accelerators come after") {
                withForm { form ->
                    onUiThread {
                        form.outerButton.addKeyboardAccelerator(WKeyStroke.getKeyStroke("F7")!!) { form.events += "accelerator" }
                        form.bind(form.target, WComponent.WHEN_IN_FOCUSED_WINDOW, "F7", "window")
                        form.bind(form.inner, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "F7", "ancestor")
                    }
                    pressOn(form, form.innerButton, listOf("ancestor:inner:null"))
                    pressOn(form, form.field, listOf("window:target:null"))
                    onUiThread { form.target.actionMap["window"]!!.isEnabled = false }
                    pressOn(form, form.field, listOf("accelerator"))
                }
            }

            test("a released keystroke works only when the key is released, not when it is pressed") {
                withForm { form ->
                    onUiThread { form.bind(form.target, WComponent.WHEN_FOCUSED, "released F7", "up") }
                    focus(form, form.target)
                    TestKeyboard.keyDown(VK_F7, SCAN_F7)
                    awaitEvents(form, emptyList())
                    TestKeyboard.keyUp(VK_F7, SCAN_F7)
                    awaitEvents(form, listOf("up:target:null"))
                }
            }

            test("for a character-input keystroke, actionCommand is the character, or the ACTION_COMMAND_KEY value if present") {
                withForm { form ->
                    onUiThread { form.bind(form.target, WComponent.WHEN_FOCUSED, "typed a", "typed") }
                    pressOn(form, form.target, listOf("typed:target:a"), VK_A, SCAN_A)
                    onUiThread { form.target.actionMap["typed"]!!.putValue(WAction.ACTION_COMMAND_KEY, "command") }
                    pressOn(form, form.target, listOf("typed:target:command"), VK_A, SCAN_A)
                }
            }

            test("a registerKeyboardAction listener is called with the actionCommand passed, and is not called after unregisterKeyboardAction") {
                withForm { form ->
                    val f7 = WKeyStroke.getKeyStroke("F7")!!
                    onUiThread {
                        form.target.registerKeyboardAction({ e -> form.events += "registered:${e.actionCommand}" }, "cmd", f7, WComponent.WHEN_IN_FOCUSED_WINDOW)
                    }
                    pressOn(form, form.outerButton, listOf("registered:cmd"))
                    onUiThread { form.target.unregisterKeyboardAction(f7) }
                    pressOn(form, form.outerButton, emptyList())
                }
            }

            test("key bindings do not work for keys consumed by a key listener") {
                withForm { form ->
                    onUiThread {
                        form.bind(form.target, WComponent.WHEN_FOCUSED, "F7", "focused")
                        form.bind(form.root, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "F7", "ancestor")
                        form.target.addKeyListener(object : WKeyAdapter() {
                            override fun keyPressed(e: WKeyEvent) = e.consume()
                        })
                    }
                    pressOn(form, form.target, emptyList())
                }
            }

            test("WHEN_FOCUSED Space runs before the button's default handling and stops the click, and an ancestor's WHEN_ANCESTOR_OF_FOCUSED_COMPONENT Space does not work because the button handles it") {
                withForm { form ->
                    onUiThread { form.bind(form.root, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "SPACE", "ancestor") }
                    pressOn(form, form.target, listOf("click"), VK_SPACE, SCAN_SPACE)
                    onUiThread { form.bind(form.target, WComponent.WHEN_FOCUSED, "SPACE", "focused") }
                    pressOn(form, form.target, listOf("focused:target:null"), VK_SPACE, SCAN_SPACE)
                }
            }

            test("a text field's WHEN_FOCUSED Enter runs before the text field's ActionListener and stops it (the same as Swing)") {
                withForm { form ->
                    pressOn(form, form.field, listOf("fieldAction"), VK_RETURN, SCAN_RETURN)
                    onUiThread { form.bind(form.field, WComponent.WHEN_FOCUSED, "ENTER", "accept") }
                    pressOn(form, form.field, listOf("accept:field:null"), VK_RETURN, SCAN_RETURN)
                }
            }
        }
    }

    private companion object {
        const val VK_F7 = 0x76
        const val SCAN_F7 = 0x41
        const val VK_A = 0x41
        const val SCAN_A = 0x1E
        const val VK_SPACE = 0x20
        const val SCAN_SPACE = 0x39
        const val VK_RETURN = 0x0D
        const val SCAN_RETURN = 0x1C
        const val SETTLE_MILLIS = 500L
    }
}
