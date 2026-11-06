package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WControl.background (the common background color like JComponent.setBackground). For text fields, checks with the native brush colors that
 * the template frame (BorderElement) is painted with this color also in the pointer-over and focused visual states.
 */
@Tags("E2E")
class WControlBackgroundTest : FunSpec() {
    /** The TextBox-like control with visual states within the text field [component] (for AutoSuggestBox and NumberBox, the inner TextBox). The caller releases it. */
    private fun stateOwner(component: WControl): ComPtr = when (component) {
        is WAutoSuggestBox -> checkNotNull(Xaml.templatePartOrNull(component.inspectable, "TextBox"))
        is WSpinner -> checkNotNull(Xaml.templatePartOrNull(component.inspectable, "InputBox"))
        else -> component.inspectable.also { it.addRef() }
    }

    /** The background color of the BorderElement of [owner]'s template (null if not a solid color). */
    private fun borderBackground(owner: ComPtr): WColor? {
        val border = checkNotNull(Xaml.appliedTemplatePartOrNull(owner, "BorderElement")) { "No BorderElement" }
        val view = border.queryInterface(XamlInterop.IID_IBorder)
        border.release()
        return try {
            view.getPtrOrNull(XamlInterop.IBorder_get_Background)?.let { brush ->
                try {
                    colorOf(brush)
                } finally {
                    brush.release()
                }
            }
        } finally {
            view.release()
        }
    }

    /** Puts the text field of [component] into the visual state [state] and waits until the frame background becomes [expected]. */
    private suspend fun expectStateBackground(component: WControl, state: String, expected: WColor) {
        val owner = onUiThreadGet { stateOwner(component) }
        try {
            onUiThread { check(Xaml.goToState(owner, state)) { "Cannot go to the visual state $state" } }
            eventually(3.seconds) {
                onUiThreadGet { borderBackground(owner) } shouldBe expected
            }
        } finally {
            onUiThread { owner.release() }
        }
    }

    /** Puts the text field of [component] into the visual state [state], waits a little, and reads the frame background. */
    private suspend fun stateBackground(component: WControl, state: String): WColor? {
        val owner = onUiThreadGet { stateOwner(component) }
        return try {
            onUiThread { check(Xaml.goToState(owner, state)) { "Cannot go to the visual state $state" } }
            Thread.sleep(STATE_SETTLE_MILLIS)
            onUiThreadGet { borderBackground(owner) }
        } finally {
            onUiThread { owner.release() }
        }
    }

    private fun <T : WControl> loaded(create: () -> T): T {
        val component = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(component)
        return component
    }

    init {
        context("Defaults") {
            test("background before being set is the theme's default color (the native effective value), not null") {
                val field = loaded { WTextField() }
                try {
                    onUiThreadGet { field.background } shouldNotBe null
                } finally {
                    UiTestHarness.detach(field)
                }
            }

            test("WChart's background stays null until set, as before") {
                onUiThreadGet { WChart().background } shouldBe null
            }
        }

        context("Reading and writing") {
            test("colors set on WSlider, WComboBox and WTextField read back and also go into Control.Background") {
                val color = WColor(10, 120, 60)
                val (readBack, native) = onUiThreadGet {
                    val controls = listOf(WSlider(), WComboBox(), WTextField())
                    controls.forEach { it.background = color }
                    controls.map { it.background } to controls.map { it.nativeBackground() }
                }
                readBack shouldBe listOf(color, color, color)
                native shouldBe listOf(color, color, color)
            }

            test("setting null returns to the theme's default color") {
                val reference = loaded { WTextField() }
                val field = loaded { WTextField().apply { background = WColor(200, 40, 40) } }
                try {
                    val (themeDefault, restored) = onUiThreadGet {
                        field.background = null
                        reference.background to field.background
                    }
                    restored shouldBe themeDefault
                } finally {
                    UiTestHarness.detach(field)
                    UiTestHarness.detach(reference)
                }
            }
        }

        context("Layout") {
            test("with a color set on WTextField after Loaded, the frame is painted in Normal, PointerOver and Focused") {
                val color = WColor(200, 40, 40)
                val field = loaded { WTextField("Input") }
                try {
                    onUiThread { field.background = color }
                    for (state in listOf("Normal", "PointerOver", "Focused", "Normal")) expectStateBackground(field, state, color)
                } finally {
                    UiTestHarness.detach(field)
                }
            }

            test("a color set before Loaded is kept in PointerOver and Focused of WPasswordField, WTextPane, WAutoSuggestBox and WSpinner") {
                val color = WColor(30, 60, 200)
                val controls = listOf(
                    loaded { WPasswordField().apply { background = color } },
                    loaded { WTextPane().apply { background = color } },
                    loaded { WAutoSuggestBox().apply { background = color } },
                    loaded { WSpinner(1.0).apply { background = color } },
                )
                try {
                    for (control in controls) {
                        for (state in listOf("PointerOver", "Focused", "Normal")) expectStateBackground(control, state, color)
                    }
                } finally {
                    controls.forEach { UiTestHarness.detach(it) }
                }
            }

            test("the disabled look stays the theme default even with background set") {
                val reference = loaded { WTextField().apply { isEnabled = false } }
                val field = loaded {
                    WTextField().apply {
                        background = WColor(200, 40, 40)
                        isEnabled = false
                    }
                }
                try {
                    val themeDisabled = stateBackground(reference, "Disabled")
                    themeDisabled shouldNotBe WColor(200, 40, 40)
                    expectStateBackground(field, "Disabled", checkNotNull(themeDisabled))
                } finally {
                    UiTestHarness.detach(field)
                    UiTestHarness.detach(reference)
                }
            }

            test("resetting it to null also returns the PointerOver frame to the theme's default color") {
                val reference = loaded { WTextField() }
                val field = loaded { WTextField().apply { background = WColor(200, 40, 40) } }
                try {
                    val themePointerOver = checkNotNull(stateBackground(reference, "PointerOver"))
                    onUiThread { field.background = null }
                    expectStateBackground(field, "PointerOver", themePointerOver)
                } finally {
                    UiTestHarness.detach(field)
                    UiTestHarness.detach(reference)
                }
            }
        }

        context("Swing compatibility") {
            test("setBackground works through the WControl type, and the button and toggle backgrounds use their own visual state mechanisms") {
                val color = WColor(1, 2, 3)
                onUiThreadGet {
                    val controls: List<WControl> = listOf(WButton("a"), WToggleButton("b"), WCheckBox("c"), WSlider())
                    controls.forEach { it.background = color }
                    controls.map { it.background }
                } shouldBe listOf(color, color, color, color)
            }
        }
    }

    private companion object {
        /** The time to wait for the visual state Storyboard to take effect when there is no expected value. */
        const val STATE_SETTLE_MILLIS = 300L
    }
}
