package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WToggleButton.isContentAreaFilled / isBorderPainted (like AbstractButton.setContentAreaFilled / setBorderPainted)
 * with the background color drawn by the template's ContentPresenter and the thickness of the border actually drawn.
 */
@Tags("E2E")
class WToggleButtonFillTest : FunSpec() {
    private fun presenterBackground(button: WToggleButton): WColor? =
        TemplateParts.brushColor(button, "ContentPresenter", XamlInterop.IID_IContentPresenter, XamlInterop.IContentPresenter_get_Background)

    /** Puts [button] into the visual state [state] and waits until the background becomes [expected]. */
    private suspend fun expectBackground(button: WToggleButton, state: String, expected: WColor?) {
        onUiThread { check(Xaml.goToState(button.inspectable, state)) { "Cannot go to the visual state $state" } }
        eventually(3.seconds) { onUiThreadGet { presenterBackground(button) } shouldBe expected }
    }

    /** Puts [button] into the visual state [state], waits a little, and reads the background. */
    private suspend fun backgroundIn(button: WToggleButton, state: String): WColor? {
        onUiThread { check(Xaml.goToState(button.inspectable, state)) { "Cannot go to the visual state $state" } }
        Thread.sleep(STATE_SETTLE_MILLIS)
        return onUiThreadGet { presenterBackground(button) }
    }

    /** The thickness (left, top, right, bottom) of the border drawn by the template's ContentPresenter. */
    private fun presenterBorderThickness(button: WToggleButton): List<Double> {
        val presenter = checkNotNull(TemplateParts.find(button, "ContentPresenter"))
        val view = presenter.queryInterface(XamlInterop.IID_IContentPresenter)
        presenter.release()
        return try {
            XamlStructs.getThickness(view, XamlInterop.IContentPresenter_get_BorderThickness).toList()
        } finally {
            view.release()
        }
    }

    private fun loaded(create: () -> WToggleButton): WToggleButton {
        val button = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(button)
        return button
    }

    init {
        context("Defaults") {
            test("isContentAreaFilled and isBorderPainted are true by default (also for check boxes and radio buttons)") {
                onUiThreadGet {
                    listOf(WToggleButton(), WCheckBox(), WRadioButton()).flatMap { listOf(it.isContentAreaFilled, it.isBorderPainted) }
                } shouldContainExactly List(6) { true }
            }
        }

        context("Layout") {
            test("while isContentAreaFilled = false, unchecked Normal / PointerOver / Pressed are transparent, and checked is filled with the theme color") {
                val reference = loaded { WToggleButton("Default") }
                val button = loaded { WToggleButton("No fill").apply { isContentAreaFilled = false } }
                try {
                    val themeChecked = backgroundIn(reference, "Checked")
                    for (state in listOf("Normal", "PointerOver", "Pressed")) expectBackground(button, state, WColor(0, 0, 0, 0))
                    expectBackground(button, "Checked", themeChecked)
                } finally {
                    UiTestHarness.detach(button)
                    UiTestHarness.detach(reference)
                }
            }

            test("while isContentAreaFilled = false, background is not filled but the color is kept, and setting true fills it again") {
                val color = WColor(200, 40, 40)
                val button = loaded { WToggleButton("Background").apply { background = color } }
                try {
                    onUiThread { button.isContentAreaFilled = false }
                    expectBackground(button, "Normal", WColor(0, 0, 0, 0))
                    expectBackground(button, "PointerOver", WColor(0, 0, 0, 0))
                    onUiThreadGet { button.background } shouldBe color
                    onUiThread { button.isContentAreaFilled = true }
                    expectBackground(button, "Normal", WColor(200, 40, 40, 255))
                    expectBackground(button, "PointerOver", WColor(200, 40, 40, 230))
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("setting isContentAreaFilled back to true returns to the theme's default background if no color is set") {
                val reference = loaded { WToggleButton("Default") }
                val button = loaded { WToggleButton("Restore").apply { isContentAreaFilled = false } }
                try {
                    val themeNormal = backgroundIn(reference, "Normal")
                    themeNormal shouldNotBe WColor(0, 0, 0, 0)
                    onUiThread { button.isContentAreaFilled = true }
                    expectBackground(button, "Normal", themeNormal)
                } finally {
                    UiTestHarness.detach(button)
                    UiTestHarness.detach(reference)
                }
            }

            test("isBorderPainted = false makes the drawn border thickness 0, and setting true returns to the theme's default thickness") {
                val button = loaded { WToggleButton("Border") }
                try {
                    val themeThickness = onUiThreadGet { presenterBorderThickness(button) }
                    themeThickness shouldNotBe listOf(0.0, 0.0, 0.0, 0.0)
                    onUiThread { button.isBorderPainted = false }
                    eventually(3.seconds) { onUiThreadGet { presenterBorderThickness(button) } shouldBe listOf(0.0, 0.0, 0.0, 0.0) }
                    onUiThreadGet { button.isBorderPainted } shouldBe false
                    onUiThread { button.isBorderPainted = true }
                    eventually(3.seconds) { onUiThreadGet { presenterBorderThickness(button) } shouldBe themeThickness }
                } finally {
                    UiTestHarness.detach(button)
                }
            }
        }
    }

    private companion object {
        const val STATE_SETTLE_MILLIS = 300L
    }
}
