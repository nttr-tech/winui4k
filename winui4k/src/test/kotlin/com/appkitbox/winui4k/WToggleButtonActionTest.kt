package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests the Swing-style constructors, icons and WAction of toggle-type buttons (WToggleButton / WCheckBox / WRadioButton).
 * As with Swing's JToggleButton.setAction, WAction's SELECTED_KEY mirrors the selected state between the action and the
 * button in both directions.
 * The form seen from Java is in WToggleButtonActionForJavaTest.
 */
class WToggleButtonActionTest : FunSpec() {
    /** An action that records the value of SELECTED_KEY when called. */
    private class RecordingAction(name: String? = null, icon: WIcon? = null) : WAbstractAction(name, icon) {
        val selectedWhenPerformed = mutableListOf<Any?>()

        override fun actionPerformed(e: WActionEvent) {
            selectedWhenPerformed += getValue(WAction.SELECTED_KEY)
        }
    }

    /** A set of functions that create the three toggle-type classes with the same constructor forms as Swing. */
    private class Kind(
        val name: String,
        val ofTextSelected: (String, Boolean) -> WToggleButton,
        val ofIcon: (WIcon) -> WToggleButton,
        val ofTextIcon: (String, WIcon?) -> WToggleButton,
        val ofTextIconSelected: (String, WIcon?, Boolean) -> WToggleButton,
        val ofAction: (WAction) -> WToggleButton,
    )

    private val kinds = listOf(
        Kind("WToggleButton", ::WToggleButton, ::WToggleButton, ::WToggleButton, ::WToggleButton, ::WToggleButton),
        Kind("WCheckBox", ::WCheckBox, ::WCheckBox, ::WCheckBox, ::WCheckBox, ::WCheckBox),
        Kind("WRadioButton", ::WRadioButton, ::WRadioButton, ::WRadioButton, ::WRadioButton, ::WRadioButton),
    )

    init {
        test("SELECTED_KEY has the same key name as Swing's Action.SELECTED_KEY") {
            WAction.SELECTED_KEY shouldBe "SwingSelectedKey"
        }

        for (kind in kinds) {
            test("${kind.name}(text, selected) sets the text and selected state, with no icon") {
                onUiThreadGet {
                    val selected = kind.ofTextSelected("Selected", true)
                    val unselected = kind.ofTextSelected("Unselected", false)
                    listOf(selected.text, selected.isSelected, selected.icon, unselected.text, unselected.isSelected)
                } shouldContainExactly listOf("Selected", true, null, "Unselected", false)
            }

            test("${kind.name}(icon) has only the icon without text, and is not selected") {
                onUiThreadGet {
                    val button = kind.ofIcon(Symbol.SAVE)
                    listOf(button.text, button.icon, button.isSelected, ButtonContents.contentOf(button))
                } shouldContainExactly listOf("", Symbol.SAVE, false, listOf("Symbol:SAVE"))
            }

            test("${kind.name}(text, icon) places the text after the icon, and only the text for a null icon") {
                onUiThreadGet {
                    val withIcon = kind.ofTextIcon("Save", Symbol.SAVE)
                    val withoutIcon = kind.ofTextIcon("Save", null)
                    listOf(withIcon.text, ButtonContents.contentOf(withIcon), withoutIcon.icon, ButtonContents.contentOf(withoutIcon))
                } shouldContainExactly listOf(
                    "Save",
                    listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save"),
                    null,
                    listOf("String:Save"),
                )
            }

            test("${kind.name}(text, icon, selected) sets the text, icon and selected state") {
                onUiThreadGet {
                    val button = kind.ofTextIconSelected("Bold", Symbol.BOLD, true)
                    listOf(button.text, button.icon, button.isSelected)
                } shouldContainExactly listOf("Bold", Symbol.BOLD, true)
            }

            test("${kind.name}(action) copies the action's values, and is created selected if SELECTED_KEY is true") {
                onUiThreadGet {
                    val action = RecordingAction("Bold", Symbol.BOLD)
                    action.putValue(WAction.SELECTED_KEY, true)
                    val button = kind.ofAction(action)
                    listOf(button.action === action, button.text, button.icon, button.isSelected)
                } shouldContainExactly listOf(true, "Bold", Symbol.BOLD, true)
            }

            test("${kind.name}'s icon keeps the text when replaced later, and null returns it to text only") {
                onUiThreadGet {
                    val button = kind.ofTextSelected("Open", false)
                    button.icon = WFontIcon("\uE8E5")
                    val withIcon = ButtonContents.contentOf(button)
                    button.icon = null
                    listOf(withIcon, button.text, ButtonContents.contentOf(button))
                } shouldContainExactly listOf(
                    listOf("Panel:HORIZONTAL:4.0", "Font:\uE8E5/Segoe Fluent Icons", "TextBlock:Open"),
                    "Open",
                    listOf("String:Open"),
                )
            }
        }

        test("changing the action's SELECTED_KEY makes the button's selected state follow, and ItemListener is called too") {
            val (selections, states) = onUiThreadGet {
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, false)
                val button = WToggleButton(action)
                val states = mutableListOf<Int>()
                button.addItemListener(WItemListener { e -> states += e.stateChange })
                val selections = mutableListOf<Boolean>()
                action.putValue(WAction.SELECTED_KEY, true)
                selections += button.isSelected
                action.putValue(WAction.SELECTED_KEY, false)
                selections += button.isSelected
                selections to states
            }
            selections shouldContainExactly listOf(true, false)
            states shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
        }

        test("an action with SELECTED_KEY receives the button's selected state on click, and the value has already changed at actionPerformed") {
            onUiThreadGet {
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, false)
                val button = WCheckBox(action)
                button.doClick()
                val afterFirst = action.getValue(WAction.SELECTED_KEY)
                button.doClick()
                listOf(afterFirst, action.getValue(WAction.SELECTED_KEY), action.selectedWhenPerformed)
            } shouldContainExactly listOf(true, false, listOf(true, false))
        }

        test("changing the selection programmatically with setSelected is also mirrored to an action with SELECTED_KEY") {
            onUiThreadGet {
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, false)
                val button = WToggleButton(action)
                button.isSelected = true
                action.getValue(WAction.SELECTED_KEY)
            } shouldBe true
        }

        test("the selected state is not written to an action without SELECTED_KEY, even on click (as in Swing)") {
            onUiThreadGet {
                val action = RecordingAction("Bold")
                val button = WToggleButton(action)
                button.doClick()
                listOf(button.isSelected, action.getValue(WAction.SELECTED_KEY), action.selectedWhenPerformed)
            } shouldContainExactly listOf(true, null, listOf(null))
        }

        test("setting an action without SELECTED_KEY deselects a selected button (as in Swing's setAction)") {
            onUiThreadGet {
                val button = WCheckBox("Bold", true)
                button.action = RecordingAction("Bold")
                button.isSelected
            } shouldBe false
        }

        test("when a three-state button becomes indeterminate, the action's SELECTED_KEY becomes false") {
            onUiThreadGet {
                val action = RecordingAction("Mixed")
                action.putValue(WAction.SELECTED_KEY, true)
                val button = WCheckBox(action)
                button.isThreeState = true
                button.isChecked = null
                action.getValue(WAction.SELECTED_KEY)
            } shouldBe false
        }

        test("selecting another radio button in the group also sets SELECTED_KEY of the deselected button's action to false") {
            onUiThreadGet {
                val smallAction = RecordingAction("Small")
                smallAction.putValue(WAction.SELECTED_KEY, true)
                val largeAction = RecordingAction("Large")
                largeAction.putValue(WAction.SELECTED_KEY, false)
                val small = WRadioButton(smallAction)
                val large = WRadioButton(largeAction)
                val group = WButtonGroup()
                group.add(small)
                group.add(large)
                large.isSelected = true
                listOf(small.isSelected, smallAction.getValue(WAction.SELECTED_KEY), largeAction.getValue(WAction.SELECTED_KEY))
            } shouldContainExactly listOf(false, false, true)
        }

        test("setting the action's SELECTED_KEY to false deselects a selected button in a group, and the group's selection is cleared too") {
            // Swing's AbstractButton.setSelectedFromAction calls ButtonGroup.clearSelection when the group prevents deselection
            val (trace, events) = onUiThreadGet {
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, true)
                val button = WCheckBox(action)
                val other = WCheckBox("Italic")
                val group = WButtonGroup()
                group.add(button)
                group.add(other)
                val events = mutableListOf<Int>()
                button.addItemListener(WItemListener { events += it.stateChange })
                action.putValue(WAction.SELECTED_KEY, false)
                listOf(button.isSelected, group.selection, action.getValue(WAction.SELECTED_KEY)) to events
            }
            trace shouldContainExactly listOf(false, null, false)
            events shouldContainExactly listOf(WItemEvent.DESELECTED)
        }

        test("setting action to null on a selected button in a group deselects it, and the group's selection is cleared too") {
            onUiThreadGet {
                val action = RecordingAction("Small")
                action.putValue(WAction.SELECTED_KEY, true)
                val button = WRadioButton(action)
                val group = WButtonGroup()
                group.add(button)
                group.add(WRadioButton("Large"))
                button.action = null
                listOf(button.isSelected, group.selection, action.getValue(WAction.SELECTED_KEY))
            } shouldContainExactly listOf(false, null, true)
        }

        test("the SELECTED_KEY write-back reaches ItemListeners added both before and after setAction with a delay (as in Swing)") {
            onUiThreadGet {
                val trace = mutableListOf<String>()
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, false)
                val button = WToggleButton()
                button.addItemListener(WItemListener { trace += "before:${it.stateChange}:${action.getValue(WAction.SELECTED_KEY)}" })
                button.action = action
                button.addItemListener(WItemListener { trace += "after:${it.stateChange}:${action.getValue(WAction.SELECTED_KEY)}" })
                action.addPropertyChangeListener { changed, key -> trace += "$key:${changed.getValue(key)}" }
                button.isSelected = true
                trace
            } shouldContainExactly listOf("before:1:false", "after:1:false", "${WAction.SELECTED_KEY}:true")
        }

        test("when reselecting in a group, the deselected button's notification and write-back finish before those of the selected button") {
            onUiThreadGet {
                val trace = mutableListOf<String>()
                val group = WButtonGroup()
                val buttons = listOf("Small", "Large").map { name ->
                    val action = RecordingAction(name)
                    action.putValue(WAction.SELECTED_KEY, false)
                    action.addPropertyChangeListener { changed, key -> trace += "$name.$key:${changed.getValue(key)}" }
                    WRadioButton(action).also {
                        it.addItemListener(WItemListener { e -> trace += "$name:${e.stateChange}" })
                        group.add(it)
                    }
                }
                buttons[0].doClick()
                trace.clear()
                buttons[1].doClick()
                trace
            } shouldContainExactly listOf(
                "Small:${WItemEvent.DESELECTED}",
                "Small.${WAction.SELECTED_KEY}:false",
                "Large:${WItemEvent.SELECTED}",
                "Large.${WAction.SELECTED_KEY}:true",
            )
        }

        test("setting action to null deselects the button, after which the action and the button's selected state no longer mirror each other") {
            onUiThreadGet {
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, true)
                val button = WToggleButton(action)
                button.action = null
                val afterDetach = button.isSelected
                action.putValue(WAction.SELECTED_KEY, true)
                val afterActionChange = button.isSelected
                button.isSelected = true
                action.putValue(WAction.SELECTED_KEY, false)
                listOf(afterDetach, afterActionChange, button.isSelected, action.getValue(WAction.SELECTED_KEY))
            } shouldContainExactly listOf(false, false, true, false)
        }

        test("IllegalArgumentException if the value of SELECTED_KEY is not a Boolean") {
            onUiThreadGet {
                val action = RecordingAction("Bold")
                action.putValue(WAction.SELECTED_KEY, "yes")
                shouldThrow<IllegalArgumentException> { WToggleButton(action) }
            }
        }

        test("WButton, which has no selected state, does not read SELECTED_KEY (regardless of the value's type)") {
            onUiThreadGet {
                val action = RecordingAction("Save")
                action.putValue(WAction.SELECTED_KEY, "yes")
                WButton(action).text
            } shouldBe "Save"
        }

        test("WHyperlinkButton and WRepeatButton also have action and icon, and clicking calls the action") {
            onUiThreadGet {
                val action = RecordingAction("Details", Symbol.HELP)
                val link = WHyperlinkButton()
                link.action = action
                val repeat = WRepeatButton()
                repeat.action = action
                link.doClick()
                repeat.doClick()
                listOf(link.text, link.icon, repeat.text, repeat.icon, action.selectedWhenPerformed.size)
            } shouldContainExactly listOf("Details", Symbol.HELP, "Details", Symbol.HELP, 2)
        }
    }
}
