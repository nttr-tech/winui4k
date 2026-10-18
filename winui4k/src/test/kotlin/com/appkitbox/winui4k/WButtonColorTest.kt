package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that WButton.background / foreground are kept in the visual states when the pointer is over the button
 * (PointerOver) and when it is pressed (Pressed), by checking the colors of the brushes the template's ContentPresenter
 * actually draws with. Visual states are switched with VisualStateManager.GoToState (the state's Storyboard takes effect
 * at the next render, so the test waits until the expected color appears).
 */
class WButtonColorTest : FunSpec() {
    /** The ContentPresenter's background and foreground colors in one visual state. */
    private data class Brushes(val background: WColor?, val foreground: WColor?)

    private fun presenterBrushes(button: WButton): Brushes {
        val root = checkNotNull(Xaml.childAt(button.inspectable, 0)) { "the template has not been applied" }
        val presenter = root.queryInterface(XamlInterop.IID_IContentPresenter)
        root.release()
        return try {
            Brushes(
                brushColor(presenter.getPtrOrNull(XamlInterop.IContentPresenter_get_Background)),
                brushColor(presenter.getPtrOrNull(XamlInterop.IContentPresenter_get_Foreground)),
            )
        } finally {
            presenter.release()
        }
    }

    private fun brushColor(brush: ComPtr?): WColor? = brush?.let {
        try {
            colorOf(it)
        } finally {
            it.release()
        }
    }

    /** Puts [button] into visual state [state], waits until the ContentPresenter's brushes become [expected], and returns them. */
    private suspend fun brushesIn(button: WButton, state: String, expected: Brushes? = null): Brushes {
        onUiThread { Xaml.goToState(button.inspectable, state).also { check(it) { "could not go to visual state $state" } } }
        var last: Brushes? = null
        if (expected == null) {
            // With no expected value (only recording the theme default), wait a little for the state's Storyboard to take effect
            Thread.sleep(STATE_SETTLE_MILLIS)
            return onUiThreadGet { presenterBrushes(button) }
        }
        eventually(3.seconds) {
            last = onUiThreadGet { presenterBrushes(button) }
            last shouldBe expected
        }
        return checkNotNull(last)
    }

    /** The appearance in the three visual states (Normal -> PointerOver -> Pressed, then back to Normal). */
    private suspend fun allStates(button: WButton, expected: List<Brushes>? = null): List<Brushes> {
        val states = listOf("Normal", "PointerOver", "Pressed")
        val result = states.mapIndexed { index, state -> brushesIn(button, state, expected?.get(index)) }
        brushesIn(button, "Normal", expected?.get(0))
        return result
    }

    private fun loadedButton(text: String, configure: (WButton) -> Unit = {}): WButton {
        val button = onUiThreadGet { WButton(text).also(configure) }
        UiTestHarness.attachAndAwaitLoaded(button)
        return button
    }

    /** The appearance in each state of a button with no colors set (the theme default). */
    private suspend fun themeDefaults(configure: (WButton) -> Unit = {}): List<Brushes> {
        val reference = loadedButton("Default", configure)
        return try {
            allStates(reference)
        } finally {
            UiTestHarness.detach(reference)
        }
    }

    init {
        test("setting background after Loaded paints Normal with that color, PointerOver at 0.9x opacity, and Pressed at 0.8x") {
            val defaults = themeDefaults()
            val button = loadedButton("Background")
            try {
                onUiThread { button.background = WColor(200, 40, 40) }
                allStates(
                    button,
                    listOf(
                        Brushes(WColor(200, 40, 40, 255), defaults[0].foreground),
                        Brushes(WColor(200, 40, 40, 230), defaults[1].foreground), // 255 × 0.9 = 229.5 → 230
                        Brushes(WColor(200, 40, 40, 204), defaults[2].foreground), // 255 × 0.8 = 204
                    ),
                )
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("background set before Loaded is also kept in each state, and a translucent color has its original opacity multiplied") {
            val defaults = themeDefaults()
            val button = loadedButton("Background") { it.background = WColor(10, 120, 60, 100) }
            try {
                allStates(
                    button,
                    listOf(
                        Brushes(WColor(10, 120, 60, 100), defaults[0].foreground),
                        Brushes(WColor(10, 120, 60, 90), defaults[1].foreground), // 100 × 0.9
                        Brushes(WColor(10, 120, 60, 80), defaults[2].foreground), // 100 × 0.8
                    ),
                )
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("foreground is that color in Normal and PointerOver and 0.7x opacity in Pressed, while the background stays the theme default") {
            val defaults = themeDefaults()
            val button = loadedButton("Text")
            try {
                onUiThread { button.foreground = WColor(0, 0, 160) }
                allStates(
                    button,
                    listOf(
                        Brushes(defaults[0].background, WColor(0, 0, 160, 255)),
                        Brushes(defaults[1].background, WColor(0, 0, 160, 255)),
                        Brushes(defaults[2].background, WColor(0, 0, 160, 179)), // 255 × 0.7 = 178.5 → 179
                    ),
                )
                onUiThreadGet { button.foreground } shouldBe WColor(0, 0, 160)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("setting background and foreground to null returns every state to the theme default appearance") {
            val defaults = themeDefaults()
            val button = loadedButton("Reset") {
                it.background = WColor(200, 40, 40)
                it.foreground = WColor(255, 255, 255)
            }
            try {
                onUiThread {
                    button.background = null
                    button.foreground = null
                }
                allStates(button, defaults)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("the disabled (Disabled) appearance stays the theme default even when background and foreground are set") {
            val reference = loadedButton("Default") { it.isEnabled = false }
            val defaultDisabled = try {
                brushesIn(reference, "Disabled")
            } finally {
                UiTestHarness.detach(reference)
            }
            val button = loadedButton("Disabled") {
                it.background = WColor(200, 40, 40)
                it.foreground = WColor(255, 255, 255)
                it.isEnabled = false
            }
            try {
                brushesIn(button, "Disabled", defaultDisabled)
                defaultDisabled.background shouldNotBe WColor(200, 40, 40)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("background and foreground take effect in each state with the built-in styles of isAccent and isContentAreaFilled = false too") {
            for (configure in listOf<(WButton) -> Unit>({ it.isAccent = true }, { it.isContentAreaFilled = false })) {
                val button = loadedButton("Accent", configure)
                try {
                    onUiThread {
                        button.background = WColor(30, 30, 30)
                        button.foreground = WColor(250, 250, 0)
                    }
                    allStates(
                        button,
                        listOf(
                            Brushes(WColor(30, 30, 30, 255), WColor(250, 250, 0, 255)),
                            Brushes(WColor(30, 30, 30, 230), WColor(250, 250, 0, 255)),
                            Brushes(WColor(30, 30, 30, 204), WColor(250, 250, 0, 179)),
                        ),
                    )
                } finally {
                    UiTestHarness.detach(button)
                }
            }
        }

        test("background and foreground are kept when switching built-in styles after setting the colors") {
            val button = loadedButton("Switch") {
                it.background = WColor(30, 90, 30)
                it.foreground = WColor(255, 255, 255)
            }
            try {
                onUiThread {
                    button.isAccent = true
                    // Changing the Style rebuilds the template, so finish layout before switching visual states
                    button.uiElement.call(XamlInterop.IUIElement_UpdateLayout)
                }
                allStates(
                    button,
                    listOf(
                        Brushes(WColor(30, 90, 30, 255), WColor(255, 255, 255, 255)),
                        Brushes(WColor(30, 90, 30, 230), WColor(255, 255, 255, 255)),
                        Brushes(WColor(30, 90, 30, 204), WColor(255, 255, 255, 179)),
                    ),
                )
            } finally {
                onUiThread { button.isAccent = false }
                UiTestHarness.detach(button)
            }
        }

        test("background returns the color that was set, and after setting null returns the theme's default background color") {
            val button = loadedButton("Read")
            try {
                val (initial, set, cleared) = onUiThreadGet {
                    val initial = button.background
                    button.background = WColor(1, 2, 3)
                    val set = button.background
                    button.background = null
                    Triple(initial, set, button.background)
                }
                set shouldBe WColor(1, 2, 3)
                initial shouldNotBe null
                cleared shouldBe initial
            } finally {
                UiTestHarness.detach(button)
            }
        }
    }

    private companion object {
        const val STATE_SETTLE_MILLIS = 300L
    }
}
