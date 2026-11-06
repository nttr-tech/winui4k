package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for the per-state icons (AbstractButton.selectedIcon / disabledIcon and JLabel.disabledIcon) with the IconElement being shown.
 * All icons are Symbols, told apart by the Symbol of the SymbolIcon being shown.
 */
@Tags("E2E")
class WButtonStateIconTest : FunSpec() {
    /** The icon arranged in the button's Content (null if none). */
    private fun shownIcon(button: WButtonBase): String? = ButtonContents.contentOf(button).firstOrNull { it.startsWith("Symbol:") }

    /** The icon arranged inside the label (null if none). */
    private fun shownIcon(label: WLabel): String? {
        val found = mutableListOf<String>()
        fun walk(element: ComPtr) {
            for (index in 0 until Xaml.childCount(element)) {
                val child = Xaml.childAt(element, index) ?: continue
                try {
                    if (IconElements.isIconElement(child)) found += IconElements.describe(child) else walk(child)
                } finally {
                    child.release()
                }
            }
        }
        walk(label.inspectable)
        return found.firstOrNull()
    }

    init {
        context("Defaults") {
            test("selectedIcon and disabledIcon are null by default, and the label's isEnabled is true and disabledIcon null") {
                onUiThreadGet {
                    val toggle = WToggleButton("Bold")
                    val label = WLabel("Name")
                    listOf(toggle.selectedIcon, toggle.disabledIcon, label.disabledIcon, label.isEnabled)
                } shouldContainExactly listOf(null, null, null, true)
            }
        }

        context("Reading and writing") {
            test("selecting the toggle shows selectedIcon and deselecting it shows icon, and getIcon returns icon even while selected") {
                onUiThreadGet {
                    val toggle = WToggleButton("", Symbol.PLAY).apply { selectedIcon = Symbol.PAUSE }
                    val shown = mutableListOf(shownIcon(toggle))
                    toggle.isSelected = true
                    shown += shownIcon(toggle)
                    val iconWhileSelected = toggle.icon
                    toggle.isSelected = false
                    shown += shownIcon(toggle)
                    shown to iconWhileSelected
                } shouldBe (listOf("Symbol:PLAY", "Symbol:PAUSE", "Symbol:PLAY") to Symbol.PLAY)
            }

            test("disabling shows disabledIcon and enabling again shows icon") {
                onUiThreadGet {
                    val button = WButton("", Symbol.SAVE).apply { disabledIcon = Symbol.CANCEL }
                    button.isEnabled = false
                    val disabled = shownIcon(button)
                    button.isEnabled = true
                    disabled to shownIcon(button)
                } shouldBe ("Symbol:CANCEL" to "Symbol:SAVE")
            }

            test("while disabled and selected, selectedIcon is shown, or disabledIcon if there is no selectedIcon (Swing's getDisabledSelectedIcon)") {
                onUiThreadGet {
                    val box = WCheckBox("", Symbol.PLAY).apply {
                        selectedIcon = Symbol.PAUSE
                        disabledIcon = Symbol.CANCEL
                        isSelected = true
                        isEnabled = false
                    }
                    val withSelectedIcon = shownIcon(box)
                    box.selectedIcon = null
                    withSelectedIcon to shownIcon(box)
                } shouldBe ("Symbol:PAUSE" to "Symbol:CANCEL")
            }

            test("as with JButton, selectedIcon of a WButton without a selection state is not used for display") {
                onUiThreadGet {
                    val button = WButton("", Symbol.SAVE).apply { selectedIcon = Symbol.ACCEPT }
                    shownIcon(button)
                } shouldBe "Symbol:SAVE"
            }

            test("while icon is null, no icon is shown even with selectedIcon and disabledIcon") {
                onUiThreadGet {
                    val toggle = WToggleButton("Text").apply {
                        selectedIcon = Symbol.PAUSE
                        disabledIcon = Symbol.CANCEL
                        isSelected = true
                        isEnabled = false
                    }
                    ButtonContents.contentOf(toggle)
                } shouldContainExactly listOf("String:Text")
            }

            test("setting content returns icon to null, and no icon is shown even when the state changes") {
                onUiThreadGet {
                    val toggle = WToggleButton("", Symbol.PLAY).apply { selectedIcon = Symbol.PAUSE }
                    toggle.content = WLabel("Content")
                    toggle.isSelected = true
                    toggle.icon to shownIcon(toggle)
                } shouldBe (null to null)
            }

            test("disabling the label shows disabledIcon, and disabledIcon is shown while disabled even if icon is null (the same as JLabel)") {
                val label = onUiThreadGet { WLabel("Name", Symbol.CONTACT).apply { disabledIcon = Symbol.DELETE } }
                UiTestHarness.attachAndAwaitLoaded(label)
                try {
                    onUiThreadGet {
                        val shown = mutableListOf(shownIcon(label))
                        label.isEnabled = false
                        shown += shownIcon(label)
                        label.icon = null
                        shown += shownIcon(label)
                        label.isEnabled = true
                        shown += shownIcon(label)
                        shown
                    } shouldContainExactly listOf("Symbol:CONTACT", "Symbol:DELETE", "Symbol:DELETE", null)
                } finally {
                    UiTestHarness.detach(label)
                }
            }
        }

        context("Events") {
            test("clicking (doClick) a toggle placed in the window also switches to selectedIcon") {
                val toggle = onUiThreadGet { WToggleButton("", Symbol.PLAY).apply { selectedIcon = Symbol.PAUSE } }
                UiTestHarness.attachAndAwaitLoaded(toggle)
                try {
                    onUiThread { toggle.doClick() }
                    onUiThreadGet { shownIcon(toggle) } shouldBe "Symbol:PAUSE"
                } finally {
                    UiTestHarness.detach(toggle)
                }
            }
        }

        context("Boundaries and exceptions") {
            test("resetting selectedIcon and disabledIcon to null shows icon in every state") {
                onUiThreadGet {
                    val toggle = WToggleButton("", Symbol.PLAY).apply {
                        selectedIcon = Symbol.PAUSE
                        disabledIcon = Symbol.CANCEL
                        isSelected = true
                        isEnabled = false
                    }
                    toggle.selectedIcon = null
                    toggle.disabledIcon = null
                    shownIcon(toggle)
                } shouldBe "Symbol:PLAY"
            }
        }
    }
}
