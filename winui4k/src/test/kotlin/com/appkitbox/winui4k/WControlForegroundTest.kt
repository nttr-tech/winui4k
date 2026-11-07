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
 * Tests that verify WControl.foreground (a JComponent.setForeground-like text color) of text fields. The text field templates change the Foreground
 * of the ContentElement that draws the text to a theme resource color in the pointer-over and focused visual states, so the tests check with the
 * native brush color that the set color is used in those states too.
 */
@Tags("E2E")
class WControlForegroundTest : FunSpec() {
    /** The TextBox-like control with visual states in the text field [component] (the TextBox inside for AutoSuggestBox and NumberBox). The caller releases it. */
    private fun stateOwner(component: WControl): ComPtr = when (component) {
        is WAutoSuggestBox -> checkNotNull(Xaml.templatePartOrNull(component.inspectable, "TextBox"))
        is WSpinner -> checkNotNull(Xaml.templatePartOrNull(component.inspectable, "InputBox"))
        else -> component.inspectable.also { it.addRef() }
    }

    /** The text color of the ContentElement (the ScrollViewer that draws the text) in [owner]'s template (null if not a solid color). */
    private fun contentForeground(owner: ComPtr): WColor? {
        val element = checkNotNull(Xaml.appliedTemplatePartOrNull(owner, "ContentElement")) { "no ContentElement" }
        val view = element.queryInterface(XamlInterop.IID_IControl)
        element.release()
        return try {
            view.getPtrOrNull(XamlInterop.IControl_get_Foreground)?.let { brush ->
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

    /** Puts the text field of [component] into the visual state [state] and waits until the text color becomes [expected]. */
    private suspend fun expectStateForeground(component: WControl, state: String, expected: WColor) {
        val owner = onUiThreadGet { stateOwner(component) }
        try {
            onUiThread { check(Xaml.goToState(owner, state)) { "cannot go to visual state $state" } }
            eventually(3.seconds) {
                onUiThreadGet { contentForeground(owner) } shouldBe expected
            }
        } finally {
            onUiThread { owner.release() }
        }
    }

    /** Puts the text field of [component] into the visual state [state], waits a little and reads the text color. */
    private suspend fun stateForeground(component: WControl, state: String): WColor? {
        val owner = onUiThreadGet { stateOwner(component) }
        return try {
            onUiThread { check(Xaml.goToState(owner, state)) { "cannot go to visual state $state" } }
            Thread.sleep(STATE_SETTLE_MILLIS)
            onUiThreadGet { contentForeground(owner) }
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
        test("with a text color set on a WTextField after Loaded, the text is drawn in it in Normal, PointerOver and Focused") {
            val color = WColor(0, 90, 158)
            val field = loaded { WTextField() }
            try {
                onUiThread { field.foreground = color }
                for (state in listOf("Normal", "PointerOver", "Focused", "Normal")) expectStateForeground(field, state, color)
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a text color set before Loaded is kept in PointerOver and Focused of WPasswordField, WTextPane, WAutoSuggestBox and WSpinner") {
            val color = WColor(0, 90, 158)
            val controls = listOf(
                loaded { WPasswordField().apply { foreground = color } },
                loaded { WTextPane().apply { foreground = color } },
                loaded { WAutoSuggestBox().apply { foreground = color } },
                loaded { WSpinner(1.0).apply { foreground = color } },
            )
            try {
                for (control in controls) {
                    for (state in listOf("PointerOver", "Focused", "Normal")) expectStateForeground(control, state, color)
                }
            } finally {
                controls.forEach { UiTestHarness.detach(it) }
            }
        }

        test("the disabled (Disabled) look stays the theme default even with foreground set") {
            val color = WColor(200, 40, 40)
            val reference = loaded { WTextField().apply { isEnabled = false } }
            val field = loaded {
                WTextField().apply {
                    foreground = color
                    isEnabled = false
                }
            }
            try {
                val themeDisabled = stateForeground(reference, "Disabled")
                themeDisabled shouldNotBe color
                expectStateForeground(field, "Disabled", checkNotNull(themeDisabled))
            } finally {
                UiTestHarness.detach(field)
                UiTestHarness.detach(reference)
            }
        }

        test("setting it back to null returns the focused text to the theme default color") {
            val reference = loaded { WTextField() }
            val field = loaded { WTextField().apply { foreground = WColor(200, 40, 40) } }
            try {
                val themeFocused = checkNotNull(stateForeground(reference, "Focused"))
                themeFocused shouldNotBe WColor(200, 40, 40)
                onUiThread { field.foreground = null }
                expectStateForeground(field, "Focused", themeFocused)
                onUiThreadGet { field.foreground } shouldBe onUiThreadGet { reference.foreground }
            } finally {
                UiTestHarness.detach(field)
                UiTestHarness.detach(reference)
            }
        }
    }

    private companion object {
        /** How long to wait for the visual state's Storyboard to take effect when there is no expected value. */
        const val STATE_SETTLE_MILLIS = 300L
    }
}
