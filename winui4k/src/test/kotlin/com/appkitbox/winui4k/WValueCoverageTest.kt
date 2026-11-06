package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the public members of the value components that had no tests (WSlider.stepFrequency, WSpinner.largeChange,
 * WColorPicker.spectrumShape and the int forms of JColorChooser.setColor, the WinUI properties of WRatingControl, and WComboBox's
 * Runnable selection listener and text submit listener). Defaults are the defaults in the WinUI sources (DependencyProperty.cpp and each idl).
 */
@Tags("E2E")
class WValueCoverageTest : FunSpec() {
    init {
        context("Defaults") {
            test("WSlider's stepFrequency is 1 (SLIDER_DEFAULT_STEP_FREQUENCY)") {
                onUiThreadGet { WSlider().stepFrequency } shouldBe 1.0
            }

            test("WSpinner's smallChange is 1 and largeChange is 10 (NumberBox.idl)") {
                onUiThreadGet { WSpinner().let { it.smallChange to it.largeChange } } shouldBe (1.0 to 10.0)
            }

            test("WColorPicker's spectrumShape is BOX (ColorPicker.idl)") {
                onUiThreadGet { WColorPicker().spectrumShape } shouldBe ColorSpectrumShape.BOX
            }

            test("WRatingControl's isClearEnabled is true, placeholderValue is -1 and caption is empty (RatingControl.idl)") {
                onUiThreadGet {
                    val rating = WRatingControl()
                    Triple(rating.isClearEnabled, rating.placeholderValue, rating.caption)
                } shouldBe Triple(true, -1.0, "")
            }
        }

        context("Reading and writing") {
            test("WSlider's stepFrequency and WSpinner's largeChange read back the set values") {
                onUiThreadGet {
                    val slider = WSlider()
                    slider.stepFrequency = 5.0
                    val spinner = WSpinner()
                    spinner.largeChange = 25.0
                    slider.stepFrequency to spinner.largeChange
                } shouldBe (5.0 to 25.0)
            }

            test("WColorPicker's spectrumShape reads back RING after setting RING") {
                onUiThreadGet {
                    val picker = WColorPicker()
                    picker.spectrumShape = ColorSpectrumShape.RING
                    picker.spectrumShape
                } shouldBe ColorSpectrumShape.RING
            }

            test("WRatingControl's isClearEnabled, placeholderValue and caption read back the set values, and placeholderValue does not change value") {
                onUiThreadGet {
                    val rating = WRatingControl()
                    rating.isClearEnabled = false
                    rating.placeholderValue = 3.5
                    rating.caption = "12 ratings"
                    listOf(rating.isClearEnabled, rating.placeholderValue, rating.caption, rating.value)
                } shouldBe listOf(false, 3.5, "12 ratings", -1.0)
            }
        }

        context("Events") {
            test("WComboBox's addListSelectionListener (lambda) is called on every selection change and removeListSelectionListener detaches it") {
                onUiThreadGet {
                    val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                    val seen = mutableListOf<Int>()
                    val listener: () -> Unit = { seen += comboBox.selectedIndex }
                    comboBox.addListSelectionListener(listener)
                    comboBox.selectedIndex = 1
                    comboBox.selectedIndex = 2
                    comboBox.removeListSelectionListener(listener)
                    comboBox.selectedIndex = 0
                    seen.toList()
                } shouldContainExactly listOf(1, 2)
            }

            test("WComboBox's addTextSubmitListener is not called for selections or text set from code (ComboBox.TextSubmitted is only for user submits)") {
                onUiThreadGet {
                    val comboBox = WComboBox(listOf("Red", "Green")).apply { isEditable = true }
                    val submitted = mutableListOf<String>()
                    comboBox.addTextSubmitListener { submitted += it }
                    comboBox.selectedIndex = 1
                    comboBox.text = "Purple"
                    submitted.toList()
                } shouldBe emptyList()
            }
        }

        context("Boundaries and exceptions") {
            test("WColorPicker.setColor(r, g, b) throws IllegalArgumentException for a component outside 0..255 and the color does not change (as in java.awt.Color)") {
                onUiThreadGet {
                    val picker = WColorPicker(WColor(1, 2, 3))
                    val errors = listOf(
                        runCatching { picker.setColor(256, 0, 0) },
                        runCatching { picker.setColor(0, -1, 0) },
                        runCatching { picker.setColor(0, 0, 300) },
                    ).map { it.exceptionOrNull()?.javaClass }
                    errors to picker.color
                } shouldBe (List(3) { IllegalArgumentException::class.java } to WColor(1, 2, 3))
            }
        }

        context("Swing compatibility") {
            test("like JColorChooser.setColor(r, g, b), passing components selects that color as opaque") {
                onUiThreadGet {
                    val picker = WColorPicker()
                    picker.setColor(10, 20, 30)
                    picker.color
                } shouldBe WColor(10, 20, 30, 255)
            }

            test("like JColorChooser.setColor(int), the lower 24 bits of 0xRRGGBB become the color and the upper 8 bits are ignored to make it opaque") {
                onUiThreadGet {
                    val picker = WColorPicker()
                    picker.setColor(0x123456)
                    val first = picker.color
                    picker.setColor(0x7FABCDEF)
                    first to picker.color
                } shouldBe (WColor(0x12, 0x34, 0x56, 255) to WColor(0xAB, 0xCD, 0xEF, 255))
            }

            test("the int form of setColor also changes the model's color and calls the ChangeListener once") {
                onUiThreadGet {
                    val picker = WColorPicker()
                    val model = picker.selectionModel
                    val colors = mutableListOf<WColor>()
                    model.addChangeListener { colors += model.selectedColor }
                    picker.setColor(0, 128, 255)
                    colors.toList()
                } shouldContainExactly listOf(WColor(0, 128, 255))
            }
        }

        context("Input") {
            test("WComboBox's addTextSubmitListener receives the string submitted with Enter in an editable combo and is not called after removal") {
                val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")).apply { isEditable = true } }
                val submitted = mutableListOf<String>()
                val listener: (String) -> Unit = { submitted += it }
                onUiThread { comboBox.addTextSubmitListener(listener) }
                UiTestHarness.attachAndAwaitLoaded(comboBox)
                try {
                    onUiThread {
                        comboBox.text = "Purple"
                        check(comboBox.requestFocusInWindow()) { "Cannot move the focus" }
                    }
                    TestKeyboard.pressEnter()
                    eventually(3.seconds) { onUiThreadGet { submitted.toList() } shouldContainExactly listOf("Purple") }
                    onUiThread {
                        comboBox.removeTextSubmitListener(listener)
                        comboBox.text = "Yellow"
                    }
                    TestKeyboard.pressEnter()
                    eventually(3.seconds) { onUiThreadGet { comboBox.selectedItem } shouldBe "Yellow" }
                    onUiThreadGet { submitted.toList() } shouldContainExactly listOf("Purple")
                } finally {
                    UiTestHarness.detach(comboBox)
                }
            }
        }
    }
}
