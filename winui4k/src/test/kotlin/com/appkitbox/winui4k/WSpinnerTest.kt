package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/** Tests WSpinner's (NumberBox) value, synchronization with the model ([WSpinnerNumberModel]) and ChangeListener. The shape seen from Java is in WSpinnerForJavaTest. */
@Tags("E2E")
class WSpinnerTest : FunSpec() {
    /** The native NumberBox's (Value, Minimum, Maximum, SmallChange). */
    private fun WSpinner.nativeState() = listOf(
        inspectable.getDouble(XamlInterop.INumberBox_get_Value),
        inspectable.getDouble(XamlInterop.INumberBox_get_Minimum),
        inspectable.getDouble(XamlInterop.INumberBox_get_Maximum),
        inspectable.getDouble(XamlInterop.INumberBox_get_SmallChange),
    )

    /** Reads the string in the template's input field (InputBox). */
    private fun WSpinner.inputText(): String {
        val box = checkNotNull(Xaml.templatePartOrNull(inspectable, "InputBox")) { "no InputBox" }
        return try {
            box.queryInterface(XamlInterop.IID_ITextBox).let { textBox ->
                try {
                    textBox.getString(XamlInterop.ITextBox_get_Text)
                } finally {
                    textBox.release()
                }
            }
        } finally {
            box.release()
        }
    }

    /** Puts [text] into the input field of [spinner] placed in the window and commits it with Enter (going through ValidateInput by the same path as user input). */
    private fun typeAndCommit(spinner: WSpinner, text: String) {
        onUiThread {
            check(spinner.requestFocusInWindow()) { "cannot move the focus" }
            val box = checkNotNull(Xaml.templatePartOrNull(spinner.inspectable, "InputBox"))
            try {
                val textBox = box.queryInterface(XamlInterop.IID_ITextBox)
                try {
                    Hstring.use(text) { h -> textBox.call(XamlInterop.ITextBox_put_Text, h) }
                } finally {
                    textBox.release()
                }
            } finally {
                box.release()
            }
        }
        TestKeyboard.pressEnter()
    }

    /** Whether the upper spin button of the template (UpSpinButton) is shown. false if there is no template yet. */
    private fun WSpinner.isUpSpinButtonShown(): Boolean {
        val button = Xaml.appliedTemplatePartOrNull(inspectable, "UpSpinButton") ?: return false
        return try {
            button.queryInterface(XamlInterop.IID_IUIElement).let { element ->
                try {
                    element.getInt(XamlInterop.IUIElement_get_Visibility) == 0 // Visibility.Visible
                } finally {
                    element.release()
                }
            }
        } finally {
            button.release()
        }
    }

    /** Places it in the window, runs [block], and removes it at the end. */
    private suspend fun withAttached(spinner: WSpinner, block: suspend () -> Unit) {
        UiTestHarness.attachAndAwaitLoaded(spinner)
        try {
            block()
        } finally {
            UiTestHarness.detach(spinner)
        }
    }

    init {
        test("the default placement of the spin buttons is INLINE, showing the up and down buttons as with JSpinner, also with the constructor taking a model") {
            onUiThreadGet {
                listOf(WSpinner().spinButtonPlacementMode, WSpinner(WSpinnerNumberModel(1, 0, 10, 1)).spinButtonPlacementMode)
            } shouldContainExactly listOf(SpinButtonPlacementMode.INLINE, SpinButtonPlacementMode.INLINE)
        }

        test("a default spinner shows the up and down buttons when shown, and HIDDEN hides them") {
            val spinner = onUiThreadGet { WSpinner(WSpinnerNumberModel(1, 0, 10, 1)) }
            withAttached(spinner) {
                eventually(3.seconds) { onUiThreadGet { spinner.isUpSpinButtonShown() }.shouldBeTrue() }
                onUiThread { spinner.spinButtonPlacementMode = SpinButtonPlacementMode.HIDDEN }
                eventually(3.seconds) { onUiThreadGet { spinner.isUpSpinButtonShown() } shouldBe false }
            }
        }

        test("the initial value without arguments is Integer 0, the same as JSpinner(), and starting empty (NaN) requires WSpinner(Double.NaN) explicitly") {
            onUiThreadGet {
                val spinner = WSpinner()
                listOf<Any?>(spinner.value, spinner.model.value, WSpinner(Double.NaN).value.isNaN())
            } shouldContainExactly listOf(0.0, 0, true)
        }

        test("the value passed to the constructor can be retrieved via value") {
            onUiThreadGet { WSpinner(7.5).value } shouldBe 7.5
        }

        test("minimum / maximum / smallChange return exactly the values that were set") {
            onUiThreadGet {
                val spinner = WSpinner()
                spinner.minimum = 0.0
                spinner.maximum = 10.0
                spinner.smallChange = 0.5
                Triple(spinner.minimum, spinner.maximum, spinner.smallChange)
            } shouldBe Triple(0.0, 10.0, 0.5)
        }

        test("stepSize, with the same name as SpinnerNumberModel.setStepSize, reads and writes smallChange and the model's stepSize") {
            onUiThreadGet {
                val spinner = WSpinner()
                spinner.stepSize = 2.5
                val written = listOf(spinner.stepSize, spinner.smallChange, (spinner.model as WSpinnerNumberModel).stepSize.toDouble())
                spinner.smallChange = 0.25
                written + spinner.stepSize
            } shouldBe listOf(2.5, 2.5, 2.5, 0.25)
        }

        test("the ChangeListener fires with the new value on every change to value") {
            val received = mutableListOf<Double>()
            onUiThread {
                val spinner = WSpinner()
                spinner.addChangeListener { received.add(it) }
                spinner.value = 3.0
                spinner.value = 8.0
            }
            received shouldContainExactly listOf(3.0, 8.0)
        }

        test("isWrapEnabled and acceptsExpression return exactly the values that were set") {
            val (wrap, expression) = onUiThreadGet {
                val spinner = WSpinner()
                spinner.isWrapEnabled = true
                spinner.acceptsExpression = true
                spinner.isWrapEnabled to spinner.acceptsExpression
            }
            wrap.shouldBeTrue()
            expression.shouldBeTrue()
        }

        test("the default model is a WSpinnerNumberModel with value Integer 0, no range and step Integer 1, the same as JSpinner(), and the range is the same as NumberBox's default (+/-Double.MAX_VALUE)") {
            onUiThread {
                val spinner = WSpinner()
                val model = spinner.model
                model.shouldBeInstanceOf<WSpinnerNumberModel>()
                listOf<Any?>(model.value, model.stepSize, spinner.nextValue) shouldContainExactly listOf(0, 1, 1)
                model.minimum.shouldBeNull()
                model.maximum.shouldBeNull()
                listOf(spinner.minimum, spinner.maximum, spinner.smallChange) shouldContainExactly
                    listOf(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0)
                spinner.nativeState().drop(1) shouldContainExactly listOf(-Double.MAX_VALUE, Double.MAX_VALUE, 1.0)
            }
        }

        test("the constructor taking a model copies the model's value, range and step to the native side") {
            onUiThread {
                val model = WSpinnerNumberModel(5, 0, 10, 2)
                val spinner = WSpinner(model)
                spinner.model shouldBeSameInstanceAs model
                spinner.nativeState() shouldContainExactly listOf(5.0, 0.0, 10.0, 2.0)
                listOf(spinner.value, spinner.minimum, spinner.maximum, spinner.smallChange) shouldContainExactly
                    listOf(5.0, 0.0, 10.0, 2.0)
                spinner.nextValue shouldBe 7
                spinner.previousValue shouldBe 3
            }
        }

        test("non-numeric models throw IllegalArgumentException both in the constructor and in setModel, and the model does not change") {
            onUiThread {
                shouldThrow<IllegalArgumentException> { WSpinner(WSpinnerListModel(listOf("a", "b"))) }
                val spinner = WSpinner(1.0)
                val before = spinner.model
                shouldThrow<IllegalArgumentException> { spinner.model = WSpinnerDateModel() }
                spinner.model shouldBeSameInstanceAs before
                spinner.value shouldBe 1.0
            }
        }

        test("changing the model directly makes the spinner and the native side follow, and WChangeListener is called once for each change of the value, minimum, maximum and step") {
            onUiThread {
                val model = WSpinnerNumberModel(5, 0, 10, 1)
                val spinner = WSpinner(model)
                val sources = mutableListOf<Any>()
                val values = mutableListOf<Double>()
                spinner.addChangeListener(WChangeListener { e -> sources += e.source })
                spinner.addChangeListener { v -> values += v }
                model.value = 7
                model.value = 7
                model.minimum = 1
                model.maximum = 9
                model.stepSize = 2
                sources.size shouldBe 4
                sources.forEach { it shouldBeSameInstanceAs spinner }
                values shouldContainExactly listOf(7.0) // Value listeners only when the value changes
                spinner.nativeState() shouldContainExactly listOf(7.0, 1.0, 9.0, 2.0)
            }
        }

        test("setModel updates the native side to the new model without calling the listeners, and changes of the previous model are no longer reflected") {
            onUiThread {
                val spinner = WSpinner(1.0)
                val count = IntArray(1)
                spinner.addChangeListener(WChangeListener { count[0]++ })
                val old = spinner.model
                spinner.model = WSpinnerNumberModel(3, 0, 5, 1)
                count[0] shouldBe 0
                spinner.value shouldBe 3.0
                old.value = 4.0
                count[0] shouldBe 0
                spinner.value shouldBe 3.0
                spinner.nativeState()[0] shouldBe 3.0
            }
        }

        test("two spinners sharing one model both move whichever one is changed") {
            onUiThread {
                val model = WSpinnerNumberModel(1, 0, 10, 1)
                val first = WSpinner(model)
                val second = WSpinner(model)
                first.value = 6.0
                second.value shouldBe 6.0
                second.nativeState()[0] shouldBe 6.0
                second.maximum = 8.0
                first.maximum shouldBe 8.0
            }
        }

        test("WinUI clamps a value outside the range to the edge, the model is rewritten to the edge value (Integer) too, and the spinner's listeners are called only once") {
            onUiThread {
                val model = WSpinnerNumberModel(5, 0, 10, 1)
                val spinner = WSpinner(model)
                val modelValues = mutableListOf<Any?>()
                model.addChangeListener { modelValues += model.value }
                val count = IntArray(1)
                spinner.addChangeListener(WChangeListener { count[0]++ })
                model.value = 20
                model.value shouldBe 10
                spinner.value shouldBe 10.0
                count[0] shouldBe 1
                modelValues shouldContainExactly listOf(20, 10) // The model's listeners also receive the value before clamping
            }
        }

        test("even with the default model, a value larger than the maximum becomes the maximum as before") {
            onUiThreadGet {
                val spinner = WSpinner()
                spinner.maximum = 10.0
                spinner.value = 20.0
                spinner.value
            } shouldBe 10.0
        }

        test("decimals put into an integer model are truncated toward 0, and NaN throws IllegalArgumentException") {
            onUiThread {
                val spinner = WSpinner(WSpinnerNumberModel(5, -10, 10, 1))
                spinner.value = 3.7
                spinner.model.value shouldBe 3
                spinner.value = -2.5
                spinner.model.value shouldBe -2
                shouldThrow<IllegalArgumentException> { spinner.value = Double.NaN }
                spinner.value shouldBe -2.0
            }
        }

        test("header and placeholderText are read back from the native side") {
            onUiThread {
                val spinner = WSpinner()
                spinner.header shouldBe ""
                spinner.placeholderText shouldBe ""
                spinner.header = "Quantity"
                spinner.placeholderText = "1 or more"
                spinner.header shouldBe "Quantity"
                spinner.placeholderText shouldBe "1 or more"
            }
        }

        test("a spinner given a model reverts to the previous value when the input is cleared and committed (COMMIT_OR_REVERT)") {
            val spinner = onUiThreadGet { WSpinner(WSpinnerNumberModel(5, 0, 10, 1)) }
            withAttached(spinner) {
                typeAndCommit(spinner, "")
                Thread.sleep(SETTLE_MILLIS)
                eventually(3.seconds) {
                    onUiThreadGet { listOf<Any?>(spinner.model.value, spinner.inputText()) } shouldContainExactly listOf(5, "5")
                }
            }
        }

        test("a spinner without arguments returns to the previous value when the input is cleared and committed, as with JSpinner (COMMIT_OR_REVERT)") {
            val spinner = onUiThreadGet { WSpinner().also { it.value = 4.0 } }
            withAttached(spinner) {
                typeAndCommit(spinner, "")
                Thread.sleep(SETTLE_MILLIS)
                eventually(3.seconds) {
                    onUiThreadGet { listOf<Any?>(spinner.model.value, spinner.inputText()) } shouldContainExactly listOf(4, "4")
                }
            }
        }

        test("a spinner given a Double value still becomes NaN when the input is cleared and committed") {
            val spinner = onUiThreadGet { WSpinner(5.0) }
            withAttached(spinner) {
                typeAndCommit(spinner, "")
                eventually(3.seconds) { onUiThreadGet { spinner.value.isNaN() }.shouldBeTrue() }
            }
        }

        test("typing a decimal into an integer model and committing puts the truncated integer into the model and shows that integer") {
            val spinner = onUiThreadGet { WSpinner(WSpinnerNumberModel(5, 0, 10, 1)) }
            withAttached(spinner) {
                typeAndCommit(spinner, "3.7")
                eventually(3.seconds) {
                    onUiThreadGet { listOf<Any?>(spinner.model.value, spinner.inputText()) } shouldContainExactly listOf(3, "3")
                }
                typeAndCommit(spinner, "3.2") // The model's value (3) does not change, but the display goes back to 3
                eventually(3.seconds) { onUiThreadGet { spinner.inputText() } shouldBe "3" }
            }
        }

        test("typed values are written back to the model, and WChangeListener is called for user input too") {
            val spinner = onUiThreadGet { WSpinner(WSpinnerNumberModel(5, 0, 10, 1)) }
            val values = mutableListOf<Double>()
            onUiThread { spinner.addChangeListener(WChangeListener { e -> values += (e.source as WSpinner).value }) }
            withAttached(spinner) {
                typeAndCommit(spinner, "8")
                eventually(3.seconds) { onUiThreadGet { values.toList() } shouldContainExactly listOf(8.0) }
                onUiThreadGet { spinner.model.value } shouldBe 8
            }
        }
    }

    private companion object {
        /** How long to wait for native notifications that arrive late. */
        const val SETTLE_MILLIS = 300L
    }
}
