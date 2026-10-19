package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify, via the brush colors of template elements, how background / foreground of WToggleButton / WCheckBox /
 * WRadioButton are painted in each class's template visual states (including checked and indeterminate).
 * - WToggleButton: the background applies only to the unchecked state (and indeterminate). Checked stays at the theme's
 *   accent color (Swing's JToggleButton also paints the selected state with the selection color, not the background
 *   color). The text color applies to all states including checked
 * - WCheckBox / WRadioButton: the background is the area behind the mark and the text (the template's RootGrid), and is
 *   the same color in all states, including disabled (same as JCheckBox.setBackground). The mark's fill is not changed.
 *   The text color is the same in all states except disabled
 * Visual states are switched with VisualStateManager.GoToState. The Java-facing form is in WToggleButtonColorForJavaTest.
 */
class WToggleButtonColorTest : FunSpec() {
    /** The background and text colors in one visual state. */
    private data class Look(val background: WColor?, val foreground: WColor?)

    /** In ToggleButton the ContentPresenter draws both background and text; in CheckBox / RadioButton the RootGrid draws the background. */
    private fun lookOf(button: WToggleButton): Look {
        val background = if (button is WCheckBox || button is WRadioButton) {
            TemplateParts.brushColor(button, "RootGrid", XamlInterop.IID_IPanel, XamlInterop.IPanel_get_Background)
        } else {
            TemplateParts.brushColor(button, "ContentPresenter", XamlInterop.IID_IContentPresenter, XamlInterop.IContentPresenter_get_Background)
        }
        val foreground = TemplateParts.brushColor(
            button,
            "ContentPresenter",
            XamlInterop.IID_IContentPresenter,
            XamlInterop.IContentPresenter_get_Foreground,
        )
        return Look(background, foreground)
    }

    /** Puts [button] into visual state [state] and waits until its look becomes [expected]. If [expected] is null, waits a little and records it. */
    private suspend fun lookIn(button: WToggleButton, state: String, expected: Look? = null): Look {
        onUiThread { check(Xaml.goToState(button.inspectable, state)) { "cannot go to visual state $state" } }
        if (expected == null) {
            Thread.sleep(STATE_SETTLE_MILLIS)
            return onUiThreadGet { lookOf(button) }
        }
        var last: Look? = null
        eventually(3.seconds) {
            last = onUiThreadGet { lookOf(button) }
            last shouldBe expected
        }
        return checkNotNull(last)
    }

    /** The looks while going through [states] in order (returning to the first state at the end). */
    private suspend fun looks(button: WToggleButton, states: List<String>, expected: List<Look>? = null): List<Look> {
        val result = states.mapIndexed { i, state -> lookIn(button, state, expected?.get(i)) }
        lookIn(button, states.first(), expected?.first())
        return result
    }

    private fun <T : WToggleButton> loaded(create: () -> T): T {
        val button = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(button)
        return button
    }

    /** The look in each state of a button of the same kind with no colors set (the theme defaults). */
    private suspend fun themeDefaults(states: List<String>, create: () -> WToggleButton): List<Look> {
        val reference = loaded(create)
        return try {
            looks(reference, states)
        } finally {
            UiTestHarness.detach(reference)
        }
    }

    private suspend fun <T : WToggleButton> withLoaded(create: () -> T, block: suspend (T) -> Unit) {
        val button = loaded(create)
        try {
            block(button)
        } finally {
            UiTestHarness.detach(button)
        }
    }

    init {
        test("WToggleButton's background paints Normal / PointerOver / Pressed of unchecked and indeterminate at 1.0 / 0.9 / 0.8, and checked stays as the theme") {
            val states = TOGGLE_STATES
            val defaults = themeDefaults(states) { WToggleButton("Default") }
            withLoaded({ WToggleButton("Background") }) { button ->
                onUiThread { button.background = WColor(200, 40, 40) }
                val red = { alpha: Int -> WColor(200, 40, 40, alpha) }
                looks(
                    button,
                    states,
                    listOf(
                        Look(red(255), defaults[0].foreground),
                        Look(red(230), defaults[1].foreground), // 255 × 0.9 = 229.5 → 230
                        Look(red(204), defaults[2].foreground), // 255 × 0.8
                        defaults[3], // Checked stays at the theme's accent color
                        defaults[4],
                        defaults[5],
                        Look(red(255), defaults[6].foreground),
                        Look(red(230), defaults[7].foreground),
                        Look(red(204), defaults[8].foreground),
                    ),
                )
            }
        }

        test("WToggleButton's foreground applies to all states including checked and indeterminate, with Pressed at 0.7 opacity") {
            val states = TOGGLE_STATES
            val defaults = themeDefaults(states) { WToggleButton("Default") }
            withLoaded({ WToggleButton("Text") }) { button ->
                onUiThread { button.foreground = WColor(0, 0, 160) }
                val blue = { alpha: Int -> WColor(0, 0, 160, alpha) }
                val expectedAlpha = listOf(255, 255, 179, 255, 255, 179, 255, 255, 179) // 255 × 0.7 = 178.5 → 179
                looks(button, states, states.indices.map { Look(defaults[it].background, blue(expectedAlpha[it])) })
                onUiThreadGet { button.foreground } shouldBe WColor(0, 0, 160)
            }
        }

        test("WToggleButton's disabled look (Disabled / CheckedDisabled) stays as the theme even when colors are set") {
            val states = listOf("Disabled", "CheckedDisabled")
            val defaults = themeDefaults(states) { WToggleButton("Default").also { it.isEnabled = false } }
            withLoaded({
                WToggleButton("Disabled").also {
                    it.background = WColor(200, 40, 40)
                    it.foreground = WColor(255, 255, 255)
                    it.isEnabled = false
                }
            }) { button ->
                looks(button, states, defaults)
                defaults[0].background shouldNotBe WColor(200, 40, 40)
            }
        }

        test("setting foreground on a toggle button shown as checked changes the text color without changing the state") {
            withLoaded({ WToggleButton("Checked", true) }) { button ->
                onUiThread { button.foreground = WColor(0, 120, 0) }
                eventually(3.seconds) {
                    onUiThreadGet { lookOf(button).foreground } shouldBe WColor(0, 120, 0)
                }
            }
        }

        test("setting null to WToggleButton's background and foreground returns all states to the theme defaults") {
            val states = TOGGLE_STATES
            val defaults = themeDefaults(states) { WToggleButton("Default") }
            withLoaded({
                WToggleButton("Reset").also {
                    it.background = WColor(200, 40, 40)
                    it.foreground = WColor(255, 255, 255)
                }
            }) { button ->
                onUiThread {
                    button.background = null
                    button.foreground = null
                }
                looks(button, states, defaults)
            }
        }

        test("background returns the color that was set, and after setting null returns the theme's default background color") {
            withLoaded({ WCheckBox("Read") }) { box ->
                val (initial, set, cleared) = onUiThreadGet {
                    val initial = box.background
                    box.background = WColor(1, 2, 3)
                    val set = box.background
                    box.background = null
                    Triple(initial, set, box.background)
                }
                set shouldBe WColor(1, 2, 3)
                initial shouldNotBe null
                cleared shouldBe initial
            }
        }

        test("WCheckBox's background paints the area behind the mark and the text with the same color in all states, including checked, indeterminate and disabled") {
            val states = CHECK_BOX_STATES
            val defaults = themeDefaults(states) { WCheckBox("Default") }
            withLoaded({ WCheckBox("Background") }) { box ->
                onUiThread { box.background = WColor(255, 240, 200) }
                looks(box, states, states.indices.map { Look(WColor(255, 240, 200), defaults[it].foreground) })
            }
        }

        test("WCheckBox's foreground is the same color in all states except disabled, and disabled stays as the theme") {
            val states = CHECK_BOX_STATES
            val defaults = themeDefaults(states) { WCheckBox("Default") }
            withLoaded({ WCheckBox("Text") }) { box ->
                onUiThread { box.foreground = WColor(0, 0, 160) }
                looks(
                    box,
                    states,
                    states.mapIndexed { i, state ->
                        Look(defaults[i].background, if (state.endsWith("Disabled")) defaults[i].foreground else WColor(0, 0, 160))
                    },
                )
            }
        }

        test("WCheckBox colors set before loading also apply in the checked state") {
            withLoaded({
                WCheckBox("Set before", true).also {
                    it.background = WColor(255, 240, 200)
                    it.foreground = WColor(0, 0, 160)
                }
            }) { box ->
                lookIn(box, "CheckedNormal", Look(WColor(255, 240, 200), WColor(0, 0, 160)))
                lookIn(box, "UncheckedNormal", Look(WColor(255, 240, 200), WColor(0, 0, 160)))
            }
        }

        test("setting background on a WCheckBox shown as checked changes the background without changing the state") {
            withLoaded({ WCheckBox("Checked", true) }) { box ->
                onUiThread { box.background = WColor(255, 240, 200) }
                eventually(3.seconds) {
                    onUiThreadGet { lookOf(box).background } shouldBe WColor(255, 240, 200)
                }
            }
        }

        test("WRadioButton's background is the same color in all states (including disabled), and its foreground in all states except disabled") {
            val states = listOf("Normal", "PointerOver", "Pressed", "Disabled")
            val defaults = themeDefaults(states) { WRadioButton("Default") }
            withLoaded({ WRadioButton("Color") }) { radio ->
                onUiThread {
                    radio.background = WColor(255, 240, 200)
                    radio.foreground = WColor(0, 0, 160)
                }
                looks(
                    radio,
                    states,
                    listOf(
                        Look(WColor(255, 240, 200), WColor(0, 0, 160)),
                        Look(WColor(255, 240, 200), WColor(0, 0, 160)),
                        Look(WColor(255, 240, 200), WColor(0, 0, 160)),
                        Look(WColor(255, 240, 200), defaults[3].foreground),
                    ),
                )
                onUiThread {
                    radio.background = null
                    radio.foreground = null
                }
                looks(radio, states, defaults)
            }
        }
    }

    private companion object {
        const val STATE_SETTLE_MILLIS = 300L

        /** ToggleButton's CommonStates (except disabled; the same names as ChangeVisualState in ToggleButton_Partial.cpp). */
        val TOGGLE_STATES = listOf(
            "Normal",
            "PointerOver",
            "Pressed",
            "Checked",
            "CheckedPointerOver",
            "CheckedPressed",
            "Indeterminate",
            "IndeterminatePointerOver",
            "IndeterminatePressed",
        )

        /** CheckBox's CombinedStates (the same names as ChangeVisualState in CheckBox_Partial.cpp). */
        val CHECK_BOX_STATES = listOf("Unchecked", "Checked", "Indeterminate").flatMap { check ->
            listOf("Normal", "PointerOver", "Pressed", "Disabled").map { "$check$it" }
        }
    }
}
