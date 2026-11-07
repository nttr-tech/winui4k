package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ColorSpectrumShape
import com.appkitbox.winui4k.HorizontalTextPosition
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SliderSnapsTo
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.TickPlacement
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.VerticalTextPosition
import com.appkitbox.winui4k.WAbstractAction
import com.appkitbox.winui4k.WAction
import com.appkitbox.winui4k.WActionEvent
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WColorPicker
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WCommand
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WDropDownButton
import com.appkitbox.winui4k.WFlyout
import com.appkitbox.winui4k.WFontIcon
import com.appkitbox.winui4k.WHyperlinkButton
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRadioButton
import com.appkitbox.winui4k.WRatingControl
import com.appkitbox.winui4k.WRepeatButton
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WSplitButton
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WToggleButton
import com.appkitbox.winui4k.WToggleSplitButton
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.extension.coroutines.WinUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Basic input category: demo pages for Button / CheckBox / ColorPicker / ComboBox / DropDownButton / HyperlinkButton / RadioButton / RatingControl / RepeatButton / Slider / SplitButton / ToggleButton / ToggleSplitButton / ToggleSwitch.
 */

// region Button

/** The Button page: lines up demos for trying out WButton's various features. */
internal fun buildButtonPage(): WComponent {
    val page = buildPage("Button", Messages.get("button.description"))

    page.add(buildSimpleButtonExample())
    page.add(buildFlyoutButtonExample())
    page.add(buildCommandButtonExample())
    page.add(buildCoroutineButtonExample())
    page.add(buildIconButtonExample())
    page.add(buildActionButtonExample())
    page.add(buildAppearanceButtonExample())
    page.add(buildDefaultButtonExample())
    return page
}

/** A basic button: responding to clicks and toggling isEnabled. */
private fun buildSimpleButtonExample(): WComponent {
    val result = WLabel(Messages.get("button.example.simple.clicks", "0"))
    var count = 0

    val standardButton = WButton("Standard XAML Button")
    standardButton.addActionListener {
        count++
        result.text = Messages.get("button.example.simple.clicks", count.toString())
    }

    val toggleButton = WButton(Messages.get("button.example.simple.disable"))
    toggleButton.addActionListener {
        standardButton.isEnabled = !standardButton.isEnabled
        toggleButton.text = if (standardButton.isEnabled) Messages.get("button.example.simple.disable") else Messages.get("button.example.simple.enable")
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(standardButton)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("button.example.simple.title"), row)
}

/** A button with a flyout: opens a popup on click. */
private fun buildFlyoutButtonExample(): WComponent {
    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)

    flyoutContent.add(WLabel(Messages.get("button.example.flyout.question")))
    flyoutContent.add(
        WButton(Messages.get("button.example.flyout.confirm")).also { button ->
            button.addActionListener { flyout.hide() }
        },
    )

    val flyoutButton = WButton(Messages.get("button.example.flyout.show"))
    flyoutButton.flyout = flyout
    return buildExample(Messages.get("button.example.flyout.title"), flyoutButton)
}

/** A button with a WCommand: running the command and auto-disabling via isEnabled. */
private fun buildCommandButtonExample(): WComponent {
    val result = WLabel(Messages.get("button.example.command.none"))
    val command = WCommand { parameter ->
        result.text = Messages.get("button.example.command.executed", "$parameter")
    }

    val commandButton = WButton(Messages.get("button.example.command.run"))
    commandButton.command = command
    commandButton.commandParameter = "Gallery"

    val toggleButton = WButton(Messages.get("button.example.command.disable"))
    toggleButton.addActionListener {
        command.isEnabled = !command.isEnabled
        toggleButton.text = if (command.isEnabled) Messages.get("button.example.command.disable") else Messages.get("button.example.command.enable")
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(commandButton)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("button.example.command.title"), row)
}

/** Buttons with icons: same shape as Swing's JButton(String, Icon); tries a Symbol, a font glyph, text positions */
private fun buildIconButtonExample(): WComponent {
    val saveButton = WButton(Messages.get("button.save"), Symbol.SAVE)
    saveButton.toolTipText = Messages.get("button.save.tooltip")

    val nextButton = WButton(Messages.get("button.example.icon.next"), Symbol.FORWARD)
    nextButton.horizontalTextPosition = HorizontalTextPosition.LEADING // Put the text before (left of) the icon

    val glyphButton = WButton(Messages.get("button.example.icon.folder"), WFontIcon("\uE8B7")) // A Segoe Fluent Icons glyph
    glyphButton.horizontalTextPosition = HorizontalTextPosition.CENTER
    glyphButton.verticalTextPosition = VerticalTextPosition.TOP // Put the text above the icon
    glyphButton.iconTextGap = 8.0

    val iconOnlyButton = WButton(Symbol.SETTING)
    iconOnlyButton.toolTipText = Messages.get("button.example.icon.settings")

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    listOf(saveButton, nextButton, glyphButton, iconOnlyButton).forEach { row.add(it) }
    return buildExample(Messages.get("button.example.icon.title"), row)
}

/** Buttons with a WAction: two buttons share one action's text, tooltip and enabled state (like Swing's Action). */
private fun buildActionButtonExample(): WComponent {
    val result = WLabel(Messages.get("button.notrun"))
    var count = 0
    val action = object : WAbstractAction(Messages.get("button.save"), Symbol.SAVE) {
        override fun actionPerformed(e: WActionEvent) {
            count++
            result.text = Messages.get("button.example.action.executed", count.toString(), "${e.actionCommand}")
        }
    }
    action.putValue(WAction.SHORT_DESCRIPTION, Messages.get("button.save.tooltip"))
    action.putValue(WAction.MNEMONIC_KEY, 'S'.code)

    val saveButton = WButton(action)
    val duplicateButton = WButton(action) // Shares the action, so name and disabled state change together

    val renameButton = WButton(Messages.get("button.example.action.rename"))
    renameButton.addActionListener {
        val renamed = action.getValue(WAction.NAME) == Messages.get("button.save")
        action.putValue(WAction.NAME, if (renamed) Messages.get("button.example.action.saveover") else Messages.get("button.save"))
    }
    val toggleButton = WButton(Messages.get("button.example.action.disable"))
    toggleButton.addActionListener {
        action.isEnabled = !action.isEnabled
        toggleButton.text = if (action.isEnabled) Messages.get("button.example.action.disable") else Messages.get("button.example.action.enable")
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    listOf(saveButton, duplicateButton, renameButton, toggleButton).forEach { row.add(it) }
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("button.example.action.title"), row)
}

/** Button appearance: background and text colors, no background fill (toolbar-style), and no border. */
private fun buildAppearanceButtonExample(): WComponent {
    val coloredButton = WButton(Messages.get("button.example.appearance.colored"))
    coloredButton.background = WColor(0, 120, 80)
    coloredButton.foreground = WColor(255, 255, 255)

    val flatButton = WButton(Messages.get("button.example.appearance.flat"))
    flatButton.isContentAreaFilled = false // Lightly filled only while the pointer is over it

    val borderlessButton = WButton(Messages.get("button.example.appearance.borderless"))
    borderlessButton.isBorderPainted = false

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    listOf(coloredButton, flatButton, borderlessButton).forEach { row.add(it) }
    return buildExample(Messages.get("button.example.appearance.title"), row)
}

/** Default button: pressing Enter in a text field presses the default button (like JRootPane.setDefaultButton). */
private fun buildDefaultButtonExample(): WComponent {
    val result = WLabel(Messages.get("button.example.default.notsent"))
    val nameField = WTextField().apply { placeholderText = Messages.get("button.example.default.placeholder") }
    val submitButton = WButton(Messages.get("button.example.default.send"))
    submitButton.addActionListener { result.text = Messages.get("button.example.default.sent", nameField.text) }

    val useDefault = WCheckBox(Messages.get("button.example.default.usedefault"))
    useDefault.addItemListener { checked ->
        galleryFrame.defaultButton = if (checked == true) submitButton else null
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(nameField)
    row.add(submitButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    val body = WPanel(spacing = 8.0)
    body.add(useDefault)
    body.add(row)
    return buildExample(Messages.get("button.example.default.title"), body)
}

/** The UI-thread coroutine scope used across the gallery (launches on Dispatchers.WinUi). */
private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.WinUi)

/** Coroutine integration: launch / delay / withContext / cancellation on Dispatchers.WinUi. */
private fun buildCoroutineButtonExample(): WComponent {
    val result = WLabel(Messages.get("button.notrun"))
    var job: Job? = null

    val startButton = WButton(Messages.get("button.example.coroutine.start"))
    val cancelButton = WButton(Messages.get("button.example.coroutine.cancel"))
    cancelButton.isEnabled = false

    startButton.addActionListener {
        startButton.isEnabled = false
        cancelButton.isEnabled = true
        job = uiScope.launch {
            try {
                // delay doesn't block the UI thread; it waits via a DispatcherQueueTimer
                for (remaining in 3 downTo 1) {
                    result.text = Messages.get("button.example.coroutine.remaining", remaining.toString())
                    delay(1_000)
                }
                // Move the heavy computation to a worker thread, only bringing the result back to the UI thread
                val sum = withContext(Dispatchers.Default) {
                    (1L..1_000_000_000L).sum()
                }
                result.text = Messages.get("button.example.coroutine.done", sum.toString())
            } catch (e: CancellationException) {
                result.text = Messages.get("button.example.coroutine.canceled")
                throw e
            } finally {
                startButton.isEnabled = true
                cancelButton.isEnabled = false
            }
        }
    }
    cancelButton.addActionListener { job?.cancel() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(startButton)
    row.add(cancelButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("button.example.coroutine.title"), row)
}

// endregion

// region CheckBox

/** The CheckBox page: lines up demos for trying out WCheckBox's various features. */
internal fun buildCheckBoxPage(): WComponent {
    val page = buildPage("CheckBox", Messages.get("checkbox.description"))

    page.add(buildSimpleCheckBoxExample())
    page.add(buildThreeStateCheckBoxExample())
    page.add(buildSelectAllCheckBoxExample())
    return page
}

/** A basic checkbox: responding to state changes (Checked / Unchecked). */
private fun buildSimpleCheckBoxExample(): WComponent {
    val result = WLabel(Messages.get("checkbox.state.off"))

    val checkBox = WCheckBox(Messages.get("checkbox.example.simple.label"))
    checkBox.addItemListener { checked ->
        result.text = if (checked == true) Messages.get("checkbox.state.on") else Messages.get("checkbox.state.off")
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(checkBox)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("checkbox.example.simple.title"), row)
}

/** Three states: isThreeState cycles on -> indeterminate -> off. */
private fun buildThreeStateCheckBoxExample(): WComponent {
    val result = WLabel(Messages.get("checkbox.state.off"))

    val checkBox = WCheckBox(Messages.get("checkbox.example.threestate.label"))
    checkBox.isThreeState = true
    checkBox.addItemListener { checked ->
        result.text = when (checked) {
            true -> Messages.get("checkbox.state.on")
            false -> Messages.get("checkbox.state.off")
            null -> Messages.get("checkbox.state.indeterminate")
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(checkBox)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("checkbox.example.threestate.title"), row)
}

/** Select all: a parent checkbox controls three children, and the children's state makes the parent indeterminate. */
private fun buildSelectAllCheckBoxExample(): WComponent {
    val parent = WCheckBox(Messages.get("checkbox.example.selectall.parent"))
    val children = listOf(
        WCheckBox(Messages.get("checkbox.example.selectall.option1")),
        WCheckBox(Messages.get("checkbox.example.selectall.option2")),
        WCheckBox(Messages.get("checkbox.example.selectall.option3")),
    )

    // Guard against an infinite event loop from the parent and children updating each other
    var updating = false
    parent.addItemListener { checked ->
        if (updating || checked == null) return@addItemListener
        updating = true
        for (child in children) child.isChecked = checked
        updating = false
    }
    for (child in children) {
        child.margin = 4.0
        child.addItemListener {
            if (updating) return@addItemListener
            updating = true
            val checkedCount = children.count { it.isChecked == true }
            parent.isChecked = when (checkedCount) {
                0 -> false
                children.size -> true
                else -> null
            }
            updating = false
        }
    }

    val body = WPanel(spacing = 4.0)
    body.add(parent)
    for (child in children) body.add(child)
    return buildExample(Messages.get("checkbox.example.selectall.title"), body)
}

// endregion

// region ColorPicker

/** The ColorPicker page: lines up demos for trying out WColorPicker's various features. */
internal fun buildColorPickerPage(): WComponent {
    val page = buildPage("ColorPicker", Messages.get("colorpicker.description"))

    page.add(buildSimpleColorPickerExample())
    page.add(buildColorPickerOptionsExample())
    return page
}

/** A basic color picker: reflecting color changes (ColorChanged) onto a tile. */
private fun buildSimpleColorPickerExample(): WComponent {
    val tile = buildTile(DEMO_BLUE, width = 64.0, height = 64.0)

    val colorPicker = WColorPicker()
    colorPicker.color = DEMO_BLUE
    colorPicker.addChangeListener { color ->
        tile.background = color
    }

    val row = WPanel(spacing = 24.0, orientation = Orientation.HORIZONTAL)
    row.add(colorPicker)
    row.add(tile)
    return buildExample(Messages.get("colorpicker.example.simple.title"), row)
}

/** Display options: isAlphaEnabled / spectrumShape / isMoreButtonVisible. */
private fun buildColorPickerOptionsExample(): WComponent {
    val colorPicker = WColorPicker()
    colorPicker.isAlphaEnabled = true
    colorPicker.isMoreButtonVisible = true

    val shapeButton = WButton(Messages.get("colorpicker.example.options.ring"))
    shapeButton.addActionListener {
        val ring = colorPicker.spectrumShape == ColorSpectrumShape.RING
        colorPicker.spectrumShape = if (ring) ColorSpectrumShape.BOX else ColorSpectrumShape.RING
        shapeButton.text = if (ring) Messages.get("colorpicker.example.options.ring") else Messages.get("colorpicker.example.options.box")
    }

    val body = WPanel(spacing = 8.0)
    body.add(shapeButton)
    body.add(colorPicker)
    return buildExample(Messages.get("colorpicker.example.options.title"), body)
}

// endregion

// region ComboBox

/** The ComboBox page: lines up demos for trying out WComboBox's various features. */
internal fun buildComboBoxPage(): WComponent {
    val page = buildPage("ComboBox", Messages.get("combobox.description"))

    page.add(buildSimpleComboBoxExample())
    page.add(buildHeaderComboBoxExample())
    page.add(buildEditableComboBoxExample())
    return page
}

/** A basic combo box: responding to selection changes (SelectionChanged). */
private fun buildSimpleComboBoxExample(): WComponent {
    val result = WLabel(Messages.get("combobox.example.simple.none"))

    val comboBox = WComboBox(listOf(Messages.get("combobox.example.simple.red"), Messages.get("combobox.example.simple.green"), Messages.get("combobox.example.simple.blue"), Messages.get("combobox.example.simple.yellow")))
    comboBox.selectedIndex = -1 // Start with nothing selected, as in the original Gallery (WComboBox selects the first item, as with JComboBox)
    comboBox.width = 200.0
    comboBox.addListSelectionListener {
        val item = comboBox.selectedItem
        result.text = if (item == null) Messages.get("combobox.example.simple.none") else Messages.get("combobox.example.simple.selected", "$item", comboBox.selectedIndex.toString())
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(comboBox)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("combobox.example.simple.title"), row)
}

/** Heading and placeholder: header / placeholderText. */
private fun buildHeaderComboBoxExample(): WComponent {
    val comboBox = WComboBox(listOf(Messages.get("combobox.example.header.font1"), Messages.get("combobox.example.header.font2"), Messages.get("combobox.example.header.font3")))
    comboBox.selectedIndex = -1 // Show the placeholder text (WComboBox selects the first item, as with JComboBox)
    comboBox.width = 200.0
    comboBox.header = Messages.get("combobox.example.header.header")
    comboBox.placeholderText = Messages.get("combobox.example.header.placeholder")
    return buildExample(Messages.get("combobox.example.header.title"), comboBox)
}

/** An editable combo box: using isEditable and TextSubmitted to add values not in the list. */
private fun buildEditableComboBoxExample(): WComponent {
    val result = WLabel(Messages.get("combobox.example.editable.none"))

    val comboBox = WComboBox(listOf("10", "20", "30"))
    comboBox.selectedIndex = -1 // Start with nothing selected, as in the original Gallery (WComboBox selects the first item, as with JComboBox)
    comboBox.width = 200.0
    comboBox.isEditable = true
    comboBox.addTextSubmitListener { text ->
        result.text = Messages.get("combobox.example.editable.submitted", text)
        if ((0 until comboBox.itemCount).none { comboBox.getItem(it) == text }) {
            comboBox.addItem(text)
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(comboBox)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("combobox.example.editable.title"), row)
}

// endregion

// region DropDownButton

/** The DropDownButton page: lines up demos for trying out WDropDownButton's various features. */
internal fun buildDropDownButtonPage(): WComponent {
    val page = buildPage("DropDownButton", Messages.get("dropdownbutton.description"))

    page.add(buildSimpleDropDownButtonExample())
    return page
}

/** A basic drop-down button: choosing from a flyout menu. */
private fun buildSimpleDropDownButtonExample(): WComponent {
    val result = WLabel(Messages.get("dropdownbutton.example.simple.none"))

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    for (name in listOf(Messages.get("dropdownbutton.example.simple.email"), Messages.get("dropdownbutton.example.simple.calendar"), Messages.get("dropdownbutton.example.simple.contacts"))) {
        menu.add(
            WButton(name).also { button ->
                button.width = 120.0
                button.addActionListener {
                    result.text = Messages.get("dropdownbutton.example.simple.selected", name)
                    flyout.hide()
                }
            },
        )
    }

    val dropDownButton = WDropDownButton(Messages.get("dropdownbutton.example.simple.new"))
    dropDownButton.flyout = flyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(dropDownButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("dropdownbutton.example.simple.title"), row)
}

// endregion

// region HyperlinkButton

/** The HyperlinkButton page: lines up demos for trying out WHyperlinkButton's various features. */
internal fun buildHyperlinkButtonPage(): WComponent {
    val page = buildPage("HyperlinkButton", Messages.get("hyperlinkbutton.description"))

    page.add(buildNavigateUriHyperlinkExample())
    page.add(buildClickHyperlinkExample())
    return page
}

/** NavigateUri: clicking opens the default browser. */
private fun buildNavigateUriHyperlinkExample(): WComponent {
    val hyperlinkButton = WHyperlinkButton(
        text = Messages.get("hyperlinkbutton.example.navigate.text"),
        navigateUri = "https://learn.microsoft.com/windows/apps/winui/winui3/",
    )
    return buildExample(Messages.get("hyperlinkbutton.example.navigate.title"), hyperlinkButton)
}

/** Handling Click: respond to clicks in code without setting NavigateUri. */
private fun buildClickHyperlinkExample(): WComponent {
    val result = WLabel(Messages.get("hyperlinkbutton.example.click.clicks", "0"))
    var count = 0

    val hyperlinkButton = WHyperlinkButton(Messages.get("hyperlinkbutton.example.click.text"))
    hyperlinkButton.addActionListener {
        count++
        result.text = Messages.get("hyperlinkbutton.example.click.clicks", count.toString())
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(hyperlinkButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("hyperlinkbutton.example.click.title"), row)
}

// endregion

// region RadioButton

/** The RadioButton page: lines up demos for trying out WRadioButton's various features. */
internal fun buildRadioButtonPage(): WComponent {
    val page = buildPage("RadioButton", Messages.get("radiobutton.description"))

    page.add(buildSimpleRadioButtonExample())
    page.add(buildRadioButtonGroupExample())
    return page
}

/** A basic radio button: mutually exclusive selection within the same group. */
private fun buildSimpleRadioButtonExample(): WComponent {
    val result = WLabel(Messages.get("radiobutton.example.simple.none"))

    val body = WPanel(spacing = 4.0)
    for (name in listOf(Messages.get("radiobutton.example.simple.small"), Messages.get("radiobutton.example.simple.medium"), Messages.get("radiobutton.example.simple.large"))) {
        body.add(
            WRadioButton(name).also { radioButton ->
                radioButton.groupName = "size"
                radioButton.addItemListener { checked ->
                    if (checked == true) result.text = Messages.get("radiobutton.example.simple.selected", name)
                }
            },
        )
    }
    body.add(result)
    return buildExample(Messages.get("radiobutton.example.simple.title"), body)
}

/** Multiple groups: separate groupName values let groups be selected independently. */
private fun buildRadioButtonGroupExample(): WComponent {
    val result = WLabel(Messages.get("radiobutton.example.groups.result", Messages.get("radiobutton.example.groups.unselected"), Messages.get("radiobutton.example.groups.unselected")))
    var background = Messages.get("radiobutton.example.groups.unselected")
    var foreground = Messages.get("radiobutton.example.groups.unselected")

    fun buildGroup(title: String, group: String, onSelect: (String) -> Unit): WComponent {
        val panel = WPanel(spacing = 4.0)
        panel.add(WLabel(title))
        for (name in listOf(Messages.get("radiobutton.example.groups.white"), Messages.get("radiobutton.example.groups.black"), Messages.get("radiobutton.example.groups.blue"))) {
            panel.add(
                WRadioButton(name).also { radioButton ->
                    radioButton.groupName = group
                    radioButton.addItemListener { checked ->
                        if (checked == true) onSelect(name)
                    }
                },
            )
        }
        return panel
    }

    val row = WPanel(spacing = 32.0, orientation = Orientation.HORIZONTAL)
    row.add(
        buildGroup(Messages.get("radiobutton.example.groups.background"), "background") {
            background = it
            result.text = Messages.get("radiobutton.example.groups.result", background, foreground)
        },
    )
    row.add(
        buildGroup(Messages.get("radiobutton.example.groups.foreground"), "foreground") {
            foreground = it
            result.text = Messages.get("radiobutton.example.groups.result", background, foreground)
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample(Messages.get("radiobutton.example.groups.title"), body)
}

// endregion

// region RatingControl

/** The RatingControl page: lines up demos for trying out WRatingControl's various features. */
internal fun buildRatingControlPage(): WComponent {
    val page = buildPage("RatingControl", Messages.get("ratingcontrol.description"))

    page.add(buildSimpleRatingExample())
    page.add(buildPlaceholderRatingExample())
    page.add(buildReadOnlyRatingExample())
    return page
}

/** A basic rating: responding to value changes (ValueChanged) and clearing it. */
private fun buildSimpleRatingExample(): WComponent {
    val result = WLabel(Messages.get("ratingcontrol.example.simple.notset"))

    val rating = WRatingControl()
    rating.isClearEnabled = true
    rating.addChangeListener { value ->
        result.text = if (value < 0) Messages.get("ratingcontrol.example.simple.notset") else Messages.get("ratingcontrol.example.simple.rating", value.toInt().toString())
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(rating)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("ratingcontrol.example.simple.title"), row)
}

/** A placeholder: faintly shows something like an average value before the user rates it. */
private fun buildPlaceholderRatingExample(): WComponent {
    val rating = WRatingControl()
    rating.placeholderValue = 3.5
    rating.caption = Messages.get("ratingcontrol.example.placeholder.caption")
    return buildExample(Messages.get("ratingcontrol.example.placeholder.title"), rating)
}

/** Read-only and star count: isReadOnly / maxRating. */
private fun buildReadOnlyRatingExample(): WComponent {
    val rating = WRatingControl()
    rating.maxRating = 10
    rating.value = 7.0
    rating.isReadOnly = true

    val toggleButton = WButton(Messages.get("ratingcontrol.example.readonly.unset"))
    toggleButton.addActionListener {
        rating.isReadOnly = !rating.isReadOnly
        toggleButton.text = if (rating.isReadOnly) Messages.get("ratingcontrol.example.readonly.unset") else Messages.get("ratingcontrol.example.readonly.set")
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(rating)
    row.add(toggleButton)
    return buildExample(Messages.get("ratingcontrol.example.readonly.title"), row)
}

// endregion

// region RepeatButton

/** The RepeatButton page: lines up demos for trying out WRepeatButton's various features. */
internal fun buildRepeatButtonPage(): WComponent {
    val page = buildPage("RepeatButton", Messages.get("repeatbutton.description"))

    page.add(buildSimpleRepeatButtonExample())
    page.add(buildRepeatButtonSpeedExample())
    return page
}

/** A basic repeat button: the counter keeps increasing while held down. */
private fun buildSimpleRepeatButtonExample(): WComponent {
    val result = WLabel(Messages.get("repeatbutton.clicks", "0"))
    var count = 0

    val repeatButton = WRepeatButton(Messages.get("repeatbutton.example.simple.text"))
    repeatButton.addActionListener {
        count++
        result.text = Messages.get("repeatbutton.clicks", count.toString())
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(repeatButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("repeatbutton.example.simple.title"), row)
}

/** Repeat speed: the difference between delay (wait before the first fire) and interval (time between fires). */
private fun buildRepeatButtonSpeedExample(): WComponent {
    val result = WLabel(Messages.get("repeatbutton.clicks", "0"))
    var count = 0

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for ((label, delay, interval) in listOf(
        Triple(Messages.get("repeatbutton.example.speed.slow"), 500, 500),
        Triple(Messages.get("repeatbutton.example.speed.fast"), 250, 50),
    )) {
        row.add(
            WRepeatButton(label).also { repeatButton ->
                repeatButton.delay = delay
                repeatButton.interval = interval
                repeatButton.addActionListener {
                    count++
                    result.text = Messages.get("repeatbutton.clicks", count.toString())
                }
            },
        )
    }
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("repeatbutton.example.speed.title"), row)
}

// endregion

// region Slider

/** The Slider page: lines up demos for trying out WSlider's various features. */
internal fun buildSliderPage(): WComponent {
    val page = buildPage("Slider", Messages.get("slider.description"))

    page.add(buildSimpleSliderExample())
    page.add(buildRangeSliderExample())
    page.add(buildTickSliderExample())
    page.add(buildVerticalSliderExample())
    return page
}

/** A basic slider: responding to value changes (ValueChanged). */
private fun buildSimpleSliderExample(): WComponent {
    val result = WLabel(Messages.get("slider.example.simple.value", "0"))

    val slider = WSlider(value = 0.0) // Start from 0, as in the original Gallery (WSlider() is the middle 50, as with JSlider)
    slider.width = 300.0
    slider.addChangeListener { value ->
        result.text = Messages.get("slider.example.simple.value", value.toInt().toString())
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(slider)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("slider.example.simple.title"), row)
}

/** Range and step: minimum / maximum / stepFrequency / header. */
private fun buildRangeSliderExample(): WComponent {
    val slider = WSlider(minimum = 500.0, maximum = 1000.0, value = 800.0)
    slider.width = 300.0
    slider.stepFrequency = 10.0
    slider.header = Messages.get("slider.example.range.header")
    return buildExample(Messages.get("slider.example.range.title"), slider)
}

/** Tick marks: tickFrequency / tickPlacement and snapsTo. */
private fun buildTickSliderExample(): WComponent {
    val slider = WSlider(maximum = 50.0, value = 0.0)
    slider.width = 300.0
    slider.tickFrequency = 10.0
    slider.tickPlacement = TickPlacement.OUTSIDE
    slider.snapsTo = SliderSnapsTo.TICKS
    return buildExample(Messages.get("slider.example.tick.title"), slider)
}

/** Vertical: orientation and isDirectionReversed. */
private fun buildVerticalSliderExample(): WComponent {
    val slider = WSlider(value = 30.0)
    slider.height = 160.0
    slider.orientation = Orientation.VERTICAL
    slider.isDirectionReversed = true
    return buildExample(Messages.get("slider.example.vertical.title"), slider)
}

// endregion

// region SplitButton

/** The SplitButton page: lines up demos for trying out WSplitButton's various features. */
internal fun buildSplitButtonPage(): WComponent {
    val page = buildPage("SplitButton", Messages.get("splitbutton.description"))

    page.add(buildSimpleSplitButtonExample())
    return page
}

/** A basic split button: clicking the body applies the current color, the arrow picks a color. */
private fun buildSimpleSplitButtonExample(): WComponent {
    val tile = buildTile(WColor.LIGHT_GRAY, width = 48.0, height = 48.0)
    var currentColor = DEMO_RED

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    val splitButton = WSplitButton(Messages.get("splitbutton.example.simple.apply"))
    for ((name, color) in listOf(Messages.get("splitbutton.example.simple.red") to DEMO_RED, Messages.get("splitbutton.example.simple.green") to DEMO_GREEN, Messages.get("splitbutton.example.simple.blue") to DEMO_BLUE)) {
        menu.add(
            WButton(name).also { button ->
                button.width = 100.0
                button.addActionListener {
                    currentColor = color
                    tile.background = color
                    splitButton.text = Messages.get("splitbutton.example.simple.applywith", name)
                    flyout.hide()
                }
            },
        )
    }
    splitButton.flyout = flyout
    splitButton.addActionListener { tile.background = currentColor }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(splitButton.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    row.add(tile)
    return buildExample(Messages.get("splitbutton.example.simple.title"), row)
}

// endregion

// region ToggleButton

/** The ToggleButton page: lines up demos for trying out WToggleButton's various features. */
internal fun buildToggleButtonPage(): WComponent {
    val page = buildPage("ToggleButton", Messages.get("togglebutton.description"))

    page.add(buildSimpleToggleButtonExample())
    return page
}

/** A basic toggle button: displaying isChecked and toggling it from code. */
private fun buildSimpleToggleButtonExample(): WComponent {
    val result = WLabel(Messages.get("togglebutton.state.off"))

    val toggleButton = WToggleButton(Messages.get("togglebutton.example.simple.mute"))
    toggleButton.addItemListener { checked ->
        result.text = if (checked == true) Messages.get("togglebutton.state.on") else Messages.get("togglebutton.state.off")
    }

    val codeButton = WButton(Messages.get("togglebutton.example.simple.fromcode"))
    codeButton.addActionListener {
        toggleButton.isChecked = toggleButton.isChecked != true
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleButton)
    row.add(codeButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("togglebutton.example.simple.title"), row)
}

// endregion

// region ToggleSplitButton

/** The ToggleSplitButton page: lines up demos for trying out WToggleSplitButton's various features. */
internal fun buildToggleSplitButtonPage(): WComponent {
    val page = buildPage("ToggleSplitButton", Messages.get("togglesplitbutton.description"))

    page.add(buildSimpleToggleSplitButtonExample())
    return page
}

/** A basic toggle split button: toggling a bulleted list on/off and choosing its marker. */
private fun buildSimpleToggleSplitButtonExample(): WComponent {
    val result = WLabel("")
    var marker = Messages.get("togglesplitbutton.example.simple.bullet")
    val items = listOf(Messages.get("togglesplitbutton.example.simple.apple"), Messages.get("togglesplitbutton.example.simple.orange"), Messages.get("togglesplitbutton.example.simple.grape"))

    val toggleSplitButton = WToggleSplitButton(Messages.get("togglesplitbutton.example.simple.bullets"))
    fun render() {
        val prefix = if (toggleSplitButton.isChecked) marker else ""
        result.text = items.joinToString("\n") { prefix + it }
    }
    render()

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    for (name in listOf(Messages.get("togglesplitbutton.example.simple.bullet"), "-", "◆")) {
        menu.add(
            WButton(name).also { button ->
                button.width = 80.0
                button.addActionListener {
                    marker = name
                    toggleSplitButton.isChecked = true
                    render()
                    flyout.hide()
                }
            },
        )
    }
    toggleSplitButton.flyout = flyout
    toggleSplitButton.addItemListener { render() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleSplitButton.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("togglesplitbutton.example.simple.title"), row)
}

// endregion

// region ToggleSwitch

/** The ToggleSwitch page: lines up demos for trying out WToggleSwitch's various features. */
internal fun buildToggleSwitchPage(): WComponent {
    val page = buildPage("ToggleSwitch", Messages.get("toggleswitch.description"))

    page.add(buildSimpleToggleSwitchExample())
    page.add(buildCustomContentToggleSwitchExample())
    return page
}

/** A basic toggle switch: responding to toggling (Toggled) and switching it from code. */
private fun buildSimpleToggleSwitchExample(): WComponent {
    val result = WLabel(Messages.get("toggleswitch.state.off"))

    val toggleSwitch = WToggleSwitch()
    toggleSwitch.addItemListener { isOn ->
        result.text = if (isOn) Messages.get("toggleswitch.state.on") else Messages.get("toggleswitch.state.off")
    }

    val codeButton = WButton(Messages.get("toggleswitch.example.simple.fromcode"))
    codeButton.addActionListener {
        toggleSwitch.isOn = !toggleSwitch.isOn
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleSwitch)
    row.add(codeButton.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("toggleswitch.example.simple.title"), row)
}

/** Customizing the displayed text: header / onContent / offContent. */
private fun buildCustomContentToggleSwitchExample(): WComponent {
    val toggleSwitch = WToggleSwitch(header = Messages.get("toggleswitch.example.custom.header"))
    toggleSwitch.onContent = Messages.get("toggleswitch.example.custom.on")
    toggleSwitch.offContent = Messages.get("toggleswitch.example.custom.off")
    return buildExample(Messages.get("toggleswitch.example.custom.title"), toggleSwitch)
}

// endregion
