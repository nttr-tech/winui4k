package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.Collections
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the JColorChooser-like [WColorPicker]: reading back the color and display settings (from native), two-way sync and notifications
 * with the color model ([WColorSelectionModel] / [WDefaultColorSelectionModel]) (the source is the model, in the same order as Swing),
 * the existing ColorChanged listeners, and [WColorPicker.showDialog] that waits until closed and its callback form.
 * Open dialogs are inspected and pressed from outside with [OpenContentDialogs]. That the handlers do not capture the picker (GC) is tested in [WrapperReleaseValueTest].
 */
class WColorPickerTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private suspend fun awaitEvents(vararg expected: String) {
        eventually(5.seconds) { events() shouldBe expected.toList() }
    }

    /** Puts an owner button in the shared window, runs [block], and removes it at the end. */
    private suspend fun withOwner(block: suspend (WButton) -> Unit) {
        val owner = onUiThreadGet { WButton("Owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(owner)
        }
    }

    /** Waits until a dialog opens in the window of [owner] and can receive keys, and returns how it looks. */
    private suspend fun awaitOpen(owner: WComponent): OpenContentDialogs.Description {
        eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeTrue() }
        Thread.sleep(OPEN_SETTLE_MILLIS)
        return OpenContentDialogs.describe(owner).shouldNotBeNull()
    }

    /** Waits until it has finished closing (so that the next dialog can be opened in the same window). */
    private suspend fun awaitClosed(owner: WComponent) {
        eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeFalse() }
        eventually(5.seconds) { OpenContentDialogs.describe(owner) shouldBe null }
    }

    private val orange = WColor(255, 127, 39)
    private val navy = WColor(10, 20, 130)

    init {
        beforeTest { events.clear() }

        test("color reads back the set color and keeps the alpha") {
            onUiThread {
                val picker = WColorPicker()
                picker.isAlphaEnabled = true
                picker.color = WColor(10, 20, 130, 200)
                picker.color shouldBe WColor(10, 20, 130, 200)
                picker.color = orange
                picker.color shouldBe orange
            }
        }

        test("isMoreButtonVisible and isHexInputVisible read native values, not fields (they change even when written directly to native)") {
            onUiThread {
                val picker = WColorPicker()
                picker.isMoreButtonVisible shouldBe false
                picker.isHexInputVisible shouldBe true
                picker.inspectable.putBool(XamlInterop.IColorPicker_put_IsMoreButtonVisible, true)
                picker.inspectable.putBool(XamlInterop.IColorPicker_put_IsHexInputVisible, false)
                picker.isMoreButtonVisible shouldBe true
                picker.isHexInputVisible shouldBe false
                picker.isMoreButtonVisible = false
                picker.isHexInputVisible = true
                picker.inspectable.getBool(XamlInterop.IColorPicker_get_IsMoreButtonVisible) shouldBe false
                picker.inspectable.getBool(XamlInterop.IColorPicker_get_IsHexInputVisible) shouldBe true
            }
        }

        test("constructors: (initial color) selects that color, and (model) makes the model's color the picker's color and selectionModel returns that model") {
            onUiThread {
                WColorPicker(orange).color shouldBe orange
                val model = WDefaultColorSelectionModel(navy)
                val picker = WColorPicker(model)
                picker.color shouldBe navy
                (picker.selectionModel === model) shouldBe true
            }
        }

        test("the default selectionModel holds the picker's current color, and changing the picker's color changes the model's color and delivers one notification with the model as source") {
            onUiThread {
                val picker = WColorPicker(orange)
                val model = picker.selectionModel
                model.selectedColor shouldBe orange
                val sources = mutableListOf<Any>()
                model.addChangeListener(WChangeListener { e -> sources += e.source })
                picker.color = navy
                model.selectedColor shouldBe navy
                sources.size shouldBe 1
                (sources[0] === model) shouldBe true
                picker.color = navy // The same color does not notify
                sources.size shouldBe 1
                (picker.selectionModel === model) shouldBe true
            }
        }

        test("changing the model's color changes the picker's color. Only the model notifies, once") {
            onUiThread {
                val model = WDefaultColorSelectionModel(orange)
                val picker = WColorPicker(model)
                var count = 0
                model.addChangeListener(WChangeListener { count++ })
                model.selectedColor = navy
                picker.color shouldBe navy
                count shouldBe 1
            }
        }

        test("when two pickers share one model, moving either one moves the other") {
            onUiThread {
                val model = WDefaultColorSelectionModel(orange)
                val first = WColorPicker(model)
                val second = WColorPicker(model)
                first.color = navy
                second.color shouldBe navy
                second.color = WColor(1, 2, 3)
                first.color shouldBe WColor(1, 2, 3)
                model.selectedColor shouldBe WColor(1, 2, 3)
            }
        }

        test("replacing selectionModel makes the picker's color the new model's color and disconnects the old model. Replacing calls neither the new nor the old listeners") {
            onUiThread {
                val picker = WColorPicker(orange)
                val oldModel = picker.selectionModel
                val newModel = WDefaultColorSelectionModel(navy)
                var oldCount = 0
                var newCount = 0
                oldModel.addChangeListener(WChangeListener { oldCount++ })
                newModel.addChangeListener(WChangeListener { newCount++ })
                picker.selectionModel = newModel
                picker.color shouldBe navy
                (picker.selectionModel === newModel) shouldBe true
                picker.color = orange
                newModel.selectedColor shouldBe orange
                oldModel.selectedColor shouldBe orange // Still the color before the replacement (picker changes no longer reach the old model)
                listOf(oldCount, newCount) shouldBe listOf(0, 1)
                oldModel.selectedColor = WColor(5, 5, 5)
                picker.color shouldBe orange
            }
        }

        test("the existing addChangeListener (Consumer form, Kotlin lambda) receives the new color on ColorChanged and is removed with removeChangeListener") {
            onUiThread {
                val picker = WColorPicker(orange)
                val received = mutableListOf<WColor>()
                val consumer = Consumer<WColor> { received += it }
                picker.addChangeListenerForJava(consumer)
                val lambda: (WColor) -> Unit = { received += it }
                picker.addChangeListener(lambda)
                picker.color = navy
                received shouldBe listOf(navy, navy)
                picker.removeChangeListenerForJava(consumer)
                picker.removeChangeListener(lambda)
                picker.color = orange
                received.size shouldBe 2
            }
        }

        test("WDefaultColorSelectionModel: defaults to white, notifies only on changes, and calls listeners starting from the last one added") {
            val model = WDefaultColorSelectionModel()
            model.selectedColor shouldBe WColor.WHITE
            val order = mutableListOf<String>()
            val first = WChangeListener { order += "first" }
            model.addChangeListener(first)
            model.addChangeListener(WChangeListener { e -> order += "second:${(e.source as WColorSelectionModel).selectedColor.red}" })
            model.selectedColor = WColor.WHITE
            order shouldBe emptyList()
            model.selectedColor = WColor(7, 8, 9)
            order shouldBe listOf("second:7", "first")
            model.getChangeListeners().size shouldBe 2
        }

        test("WDefaultColorSelectionModel: adding the same listener twice calls it twice, remove removes only one, and a listener removed during a notification still receives the current event") {
            val model = WDefaultColorSelectionModel()
            var count = 0
            val listener = WChangeListener { count++ }
            model.addChangeListener(listener)
            model.addChangeListener(listener)
            model.selectedColor = navy
            count shouldBe 2
            model.removeChangeListener(listener)
            model.selectedColor = orange
            count shouldBe 3

            val seen = mutableListOf<String>()
            val removed = WChangeListener { seen += "removed" }
            model.addChangeListener(removed)
            model.addChangeListener(WChangeListener { model.removeChangeListener(removed) }) // Added later, so called first
            model.selectedColor = navy
            seen shouldBe listOf("removed")
            model.selectedColor = orange
            seen shouldBe listOf("removed")
        }

        test("WDefaultColorSelectionModel: toString includes the color") {
            WDefaultColorSelectionModel(navy).toString() shouldContain "selectedColor=WColor(red=10, green=20, blue=130, alpha=255)"
        }

        test("showDialog blocks until closed, and returns the initial color on OK and null on Cancel / Esc. The title and button texts are set too") {
            withOwner { owner ->
                WinUiUtilities.invokeLater { events += "r:${WColorPicker.showDialog(owner, "Choose a color", navy)}" }
                val shown = awaitOpen(owner)
                listOf(shown.title, shown.primary, shown.close) shouldBe
                    listOf("Choose a color", WOptionPane.okButtonText, WOptionPane.cancelButtonText)
                OpenContentDialogs.press(owner, "PrimaryButton")
                awaitEvents("r:$navy")
                awaitClosed(owner)

                WinUiUtilities.invokeLater { events += "r:${WColorPicker.showDialog(owner, "Choose a color", navy)}" }
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "CloseButton")
                awaitEvents("r:$navy", "r:null")
                awaitClosed(owner)

                WinUiUtilities.invokeLater { events += "r:${WColorPicker.showDialog(owner, "Choose a color", navy)}" }
                awaitOpen(owner)
                TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)
                awaitEvents("r:$navy", "r:null", "r:null")
                awaitClosed(owner)
            }
        }

        test("showDialog starts from white if the initial color is null") {
            withOwner { owner ->
                WinUiUtilities.invokeLater { events += "r:${WColorPicker.showDialog(owner, "Color", null)}" }
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "PrimaryButton")
                awaitEvents("r:${WColor.WHITE}")
                awaitClosed(owner)
            }
        }

        test("the callback form of showDialog returns without blocking and passes the color (null on cancel) when closed") {
            withOwner { owner ->
                onUiThread {
                    WColorPicker.showDialog(owner, "Color", orange) { events += "cb:$it" }
                    events += "returned"
                }
                events() shouldBe listOf("returned")
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "PrimaryButton")
                awaitEvents("returned", "cb:$orange")
                awaitClosed(owner)

                onUiThread { WColorPicker.showDialog(owner, "Color", orange) { events += "cb:$it" } }
                awaitOpen(owner)
                OpenContentDialogs.press(owner, "CloseButton")
                awaitEvents("returned", "cb:$orange", "cb:null")
                awaitClosed(owner)
            }
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val OPEN_SETTLE_MILLIS = 600L
    }
}
