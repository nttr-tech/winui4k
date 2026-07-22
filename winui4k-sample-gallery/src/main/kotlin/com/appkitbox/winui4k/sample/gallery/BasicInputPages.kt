package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ColorSpectrumShape
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SliderSnapsTo
import com.appkitbox.winui4k.TickPlacement
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WColorPicker
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WCommand
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WDropDownButton
import com.appkitbox.winui4k.WFlyout
import com.appkitbox.winui4k.WHyperlinkButton
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRadioButton
import com.appkitbox.winui4k.WRatingControl
import com.appkitbox.winui4k.WRepeatButton
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WSplitButton
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

/**
 * Basic input category: demo pages for Button / CheckBox / ColorPicker / ComboBox / DropDownButton / HyperlinkButton / RadioButton / RatingControl / RepeatButton / Slider / SplitButton / ToggleButton / ToggleSplitButton / ToggleSwitch.
 */

// region Button

/** The Button page: lines up demos for trying out WButton's various features. */
internal fun buildButtonPage(): WComponent {
    val page = buildPage("Button", "A button that responds to clicks. Try out WButton's various features.")

    page.add(buildSimpleButtonExample())
    page.add(buildFlyoutButtonExample())
    page.add(buildCommandButtonExample())
    page.add(buildCoroutineButtonExample())
    return page
}

/** A basic button: responding to clicks and toggling isEnabled. */
private fun buildSimpleButtonExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val standardButton = WButton("Standard XAML Button")
    standardButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val toggleButton = WButton("Disable button")
    toggleButton.addActionListener {
        standardButton.isEnabled = !standardButton.isEnabled
        toggleButton.text = if (standardButton.isEnabled) "Disable button" else "Enable button"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(standardButton)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Simple button", row)
}

/** A button with a flyout: opens a popup on click. */
private fun buildFlyoutButtonExample(): WComponent {
    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)

    flyoutContent.add(WLabel("Delete all items?"))
    flyoutContent.add(
        WButton("Yes, delete all").also { button ->
            button.addActionListener { flyout.hide() }
        },
    )

    val flyoutButton = WButton("Show options")
    flyoutButton.flyout = flyout
    return buildExample("Button with a flyout", flyoutButton)
}

/** A button with a WCommand: running the command and auto-disabling via isEnabled. */
private fun buildCommandButtonExample(): WComponent {
    val result = WLabel("Command has not run yet")
    val command = WCommand { parameter ->
        result.text = "Command ran (parameter = $parameter)"
    }

    val commandButton = WButton("Run command")
    commandButton.command = command
    commandButton.commandParameter = "Gallery"

    val toggleButton = WButton("Disable command")
    toggleButton.addActionListener {
        command.isEnabled = !command.isEnabled
        toggleButton.text = if (command.isEnabled) "Disable command" else "Enable command"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(commandButton)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Button with a command", row)
}

/** The UI-thread coroutine scope used across the gallery (launches on Dispatchers.WinUi). */
private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.WinUi)

/** Coroutine integration: launch / delay / withContext / cancellation on Dispatchers.WinUi. */
private fun buildCoroutineButtonExample(): WComponent {
    val result = WLabel("Not run yet")
    var job: Job? = null

    val startButton = WButton("Start a 3-second task")
    val cancelButton = WButton("Cancel")
    cancelButton.isEnabled = false

    startButton.addActionListener {
        startButton.isEnabled = false
        cancelButton.isEnabled = true
        job = uiScope.launch {
            try {
                // delay doesn't block the UI thread; it waits via a DispatcherQueueTimer
                for (remaining in 3 downTo 1) {
                    result.text = "Working... $remaining seconds left"
                    delay(1_000)
                }
                // Move the heavy computation to a worker thread, only bringing the result back to the UI thread
                val sum = withContext(Dispatchers.Default) {
                    (1L..1_000_000_000L).sum()
                }
                result.text = "Done (sum of 1 to 1 billion = $sum)"
            } catch (e: CancellationException) {
                result.text = "Cancelled"
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
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Coroutine integration (Dispatchers.WinUi / delay / withContext / cancel)", row)
}

// endregion

// region CheckBox

/** The CheckBox page: lines up demos for trying out WCheckBox's various features. */
internal fun buildCheckBoxPage(): WComponent {
    val page = buildPage("CheckBox", "A control for toggling checked/unchecked (and indeterminate). Try out WCheckBox's various features.")

    page.add(buildSimpleCheckBoxExample())
    page.add(buildThreeStateCheckBoxExample())
    page.add(buildSelectAllCheckBoxExample())
    return page
}

/** A basic checkbox: responding to state changes (Checked / Unchecked). */
private fun buildSimpleCheckBoxExample(): WComponent {
    val result = WLabel("State: off")

    val checkBox = WCheckBox("Receive notifications")
    checkBox.addItemListener { checked ->
        result.text = if (checked == true) "State: on" else "State: off"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(checkBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple checkbox", row)
}

/** Three states: isThreeState cycles on -> indeterminate -> off. */
private fun buildThreeStateCheckBoxExample(): WComponent {
    val result = WLabel("State: off")

    val checkBox = WCheckBox("Three-state checkbox")
    checkBox.isThreeState = true
    checkBox.addItemListener { checked ->
        result.text = when (checked) {
            true -> "State: on"
            false -> "State: off"
            null -> "State: indeterminate"
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(checkBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Three states (IsThreeState)", row)
}

/** Select all: a parent checkbox controls three children, and the children's state makes the parent indeterminate. */
private fun buildSelectAllCheckBoxExample(): WComponent {
    val parent = WCheckBox("Select all options")
    val children = listOf(
        WCheckBox("Option 1"),
        WCheckBox("Option 2"),
        WCheckBox("Option 3"),
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
    return buildExample("Select all (parent/child linkage and indeterminate state)", body)
}

// endregion

// region ColorPicker

/** The ColorPicker page: lines up demos for trying out WColorPicker's various features. */
internal fun buildColorPickerPage(): WComponent {
    val page = buildPage("ColorPicker", "A control for picking a color from a spectrum. Try out WColorPicker's various features.")

    page.add(buildSimpleColorPickerExample())
    page.add(buildColorPickerOptionsExample())
    return page
}

/** A basic color picker: reflecting color changes (ColorChanged) onto a tile. */
private fun buildSimpleColorPickerExample(): WComponent {
    val tile = buildTile(WColor.BLUE, width = 64.0, height = 64.0)

    val colorPicker = WColorPicker()
    colorPicker.color = WColor.BLUE
    colorPicker.addChangeListener { color ->
        tile.background = color
    }

    val row = WPanel(spacing = 24.0, orientation = Orientation.HORIZONTAL)
    row.add(colorPicker)
    row.add(tile)
    return buildExample("A simple color picker (Color / ColorChanged)", row)
}

/** Display options: isAlphaEnabled / spectrumShape / isMoreButtonVisible. */
private fun buildColorPickerOptionsExample(): WComponent {
    val colorPicker = WColorPicker()
    colorPicker.isAlphaEnabled = true
    colorPicker.isMoreButtonVisible = true

    val shapeButton = WButton("Make the spectrum a ring")
    shapeButton.addActionListener {
        val ring = colorPicker.spectrumShape == ColorSpectrumShape.RING
        colorPicker.spectrumShape = if (ring) ColorSpectrumShape.BOX else ColorSpectrumShape.RING
        shapeButton.text = if (ring) "Make the spectrum a ring" else "Make the spectrum a box"
    }

    val body = WPanel(spacing = 8.0)
    body.add(shapeButton)
    body.add(colorPicker)
    return buildExample("Display options (IsAlphaEnabled / ColorSpectrumShape / IsMoreButtonVisible)", body)
}

// endregion

// region ComboBox

/** The ComboBox page: lines up demos for trying out WComboBox's various features. */
internal fun buildComboBoxPage(): WComponent {
    val page = buildPage("ComboBox", "A control for picking one item from a drop-down. Try out WComboBox's various features.")

    page.add(buildSimpleComboBoxExample())
    page.add(buildHeaderComboBoxExample())
    page.add(buildEditableComboBoxExample())
    return page
}

/** A basic combo box: responding to selection changes (SelectionChanged). */
private fun buildSimpleComboBoxExample(): WComponent {
    val result = WLabel("Selected: none")

    val comboBox = WComboBox(listOf("Red", "Green", "Blue", "Yellow"))
    comboBox.width = 200.0
    comboBox.addListSelectionListener {
        val item = comboBox.selectedItem
        result.text = if (item == null) "Selected: none" else "Selected: $item (index = ${comboBox.selectedIndex})"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(comboBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple combo box (SelectionChanged)", row)
}

/** Heading and placeholder: header / placeholderText. */
private fun buildHeaderComboBoxExample(): WComponent {
    val comboBox = WComboBox(listOf("Meiryo", "Yu Gothic", "BIZ UDGothic"))
    comboBox.width = 200.0
    comboBox.header = "Font"
    comboBox.placeholderText = "Choose a font"
    return buildExample("Heading and placeholder (Header / PlaceholderText)", comboBox)
}

/** An editable combo box: using isEditable and TextSubmitted to add values not in the list. */
private fun buildEditableComboBoxExample(): WComponent {
    val result = WLabel("Submitted: none")

    val comboBox = WComboBox(listOf("10", "20", "30"))
    comboBox.width = 200.0
    comboBox.isEditable = true
    comboBox.addTextSubmitListener { text ->
        result.text = "Submitted: $text"
        if ((0 until comboBox.itemCount).none { comboBox.getItem(it) == text }) {
            comboBox.addItem(text)
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(comboBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("An editable combo box (IsEditable / TextSubmitted)", row)
}

// endregion

// region DropDownButton

/** The DropDownButton page: lines up demos for trying out WDropDownButton's various features. */
internal fun buildDropDownButtonPage(): WComponent {
    val page = buildPage("DropDownButton", "A button that opens a flyout of choices when clicked. Try out WDropDownButton's various features.")

    page.add(buildSimpleDropDownButtonExample())
    return page
}

/** A basic drop-down button: choosing from a flyout menu. */
private fun buildSimpleDropDownButtonExample(): WComponent {
    val result = WLabel("Selected: none")

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    for (name in listOf("Mail", "Calendar", "Contacts")) {
        menu.add(
            WButton(name).also { button ->
                button.width = 120.0
                button.addActionListener {
                    result.text = "Selected: $name"
                    flyout.hide()
                }
            },
        )
    }

    val dropDownButton = WDropDownButton("New")
    dropDownButton.flyout = flyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(dropDownButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple drop-down button (Flyout)", row)
}

// endregion

// region HyperlinkButton

/** The HyperlinkButton page: lines up demos for trying out WHyperlinkButton's various features. */
internal fun buildHyperlinkButtonPage(): WComponent {
    val page = buildPage("HyperlinkButton", "A button displayed as a hyperlink. Try out WHyperlinkButton's various features.")

    page.add(buildNavigateUriHyperlinkExample())
    page.add(buildClickHyperlinkExample())
    return page
}

/** NavigateUri: clicking opens the default browser. */
private fun buildNavigateUriHyperlinkExample(): WComponent {
    val hyperlinkButton = WHyperlinkButton(
        text = "Open the WinUI 3 documentation",
        navigateUri = "https://learn.microsoft.com/windows/apps/winui/winui3/",
    )
    return buildExample("Navigating to a URI (NavigateUri)", hyperlinkButton)
}

/** Handling Click: respond to clicks in code without setting NavigateUri. */
private fun buildClickHyperlinkExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val hyperlinkButton = WHyperlinkButton("A link whose click is handled in code")
    hyperlinkButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(hyperlinkButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Handling the Click event", row)
}

// endregion

// region RadioButton

/** The RadioButton page: lines up demos for trying out WRadioButton's various features. */
internal fun buildRadioButtonPage(): WComponent {
    val page = buildPage("RadioButton", "A control for picking exactly one option within a group. Try out WRadioButton's various features.")

    page.add(buildSimpleRadioButtonExample())
    page.add(buildRadioButtonGroupExample())
    return page
}

/** A basic radio button: mutually exclusive selection within the same group. */
private fun buildSimpleRadioButtonExample(): WComponent {
    val result = WLabel("Selected: none")

    val body = WPanel(spacing = 4.0)
    for (name in listOf("Small", "Medium", "Large")) {
        body.add(
            WRadioButton(name).also { radioButton ->
                radioButton.groupName = "Size"
                radioButton.addItemListener { checked ->
                    if (checked == true) result.text = "Selected: $name"
                }
            },
        )
    }
    body.add(result)
    return buildExample("A simple radio button (mutually exclusive selection)", body)
}

/** Multiple groups: separate groupName values let groups be selected independently. */
private fun buildRadioButtonGroupExample(): WComponent {
    val result = WLabel("Background: unselected / Foreground: unselected")
    var background = "unselected"
    var foreground = "unselected"

    fun buildGroup(title: String, group: String, onSelect: (String) -> Unit): WComponent {
        val panel = WPanel(spacing = 4.0)
        panel.add(WLabel(title))
        for (name in listOf("White", "Black", "Blue")) {
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
    row.add(buildGroup("Background", "background") { background = it; result.text = "Background: $background / Foreground: $foreground" })
    row.add(buildGroup("Foreground", "foreground") { foreground = it; result.text = "Background: $background / Foreground: $foreground" })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("Multiple groups (GroupName)", body)
}

// endregion

// region RatingControl

/** The RatingControl page: lines up demos for trying out WRatingControl's various features. */
internal fun buildRatingControlPage(): WComponent {
    val page = buildPage("RatingControl", "A control for entering a star rating. Try out WRatingControl's various features.")

    page.add(buildSimpleRatingExample())
    page.add(buildPlaceholderRatingExample())
    page.add(buildReadOnlyRatingExample())
    return page
}

/** A basic rating: responding to value changes (ValueChanged) and clearing it. */
private fun buildSimpleRatingExample(): WComponent {
    val result = WLabel("Rating: unset")

    val rating = WRatingControl()
    rating.isClearEnabled = true
    rating.addChangeListener { value ->
        result.text = if (value < 0) "Rating: unset" else "Rating: ${value.toInt()}"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(rating)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple rating (ValueChanged / IsClearEnabled)", row)
}

/** A placeholder: faintly shows something like an average value before the user rates it. */
private fun buildPlaceholderRatingExample(): WComponent {
    val rating = WRatingControl()
    rating.placeholderValue = 3.5
    rating.caption = "512 reviews"
    return buildExample("A placeholder (PlaceholderValue / Caption)", rating)
}

/** Read-only and star count: isReadOnly / maxRating. */
private fun buildReadOnlyRatingExample(): WComponent {
    val rating = WRatingControl()
    rating.maxRating = 10
    rating.value = 7.0
    rating.isReadOnly = true

    val toggleButton = WButton("Turn off read-only")
    toggleButton.addActionListener {
        rating.isReadOnly = !rating.isReadOnly
        toggleButton.text = if (rating.isReadOnly) "Turn off read-only" else "Make read-only"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(rating)
    row.add(toggleButton)
    return buildExample("Read-only and star count (IsReadOnly / MaxRating)", row)
}

// endregion

// region RepeatButton

/** The RepeatButton page: lines up demos for trying out WRepeatButton's various features. */
internal fun buildRepeatButtonPage(): WComponent {
    val page = buildPage("RepeatButton", "A button that fires Click repeatedly while held down. Try out WRepeatButton's various features.")

    page.add(buildSimpleRepeatButtonExample())
    page.add(buildRepeatButtonSpeedExample())
    return page
}

/** A basic repeat button: the counter keeps increasing while held down. */
private fun buildSimpleRepeatButtonExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val repeatButton = WRepeatButton("Press and hold")
    repeatButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(repeatButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple repeat button", row)
}

/** Repeat speed: the difference between delay (wait before the first fire) and interval (time between fires). */
private fun buildRepeatButtonSpeedExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for ((label, delay, interval) in listOf(
        Triple("Slow (500ms interval)", 500, 500),
        Triple("Fast (50ms interval)", 250, 50),
    )) {
        row.add(
            WRepeatButton(label).also { repeatButton ->
                repeatButton.delay = delay
                repeatButton.interval = interval
                repeatButton.addActionListener {
                    count++
                    result.text = "Click count: $count"
                }
            },
        )
    }
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Repeat speed (Delay / Interval)", row)
}

// endregion

// region Slider

/** The Slider page: lines up demos for trying out WSlider's various features. */
internal fun buildSliderPage(): WComponent {
    val page = buildPage("Slider", "A control for picking a value in a range by moving a thumb along a track. Try out WSlider's various features.")

    page.add(buildSimpleSliderExample())
    page.add(buildRangeSliderExample())
    page.add(buildTickSliderExample())
    page.add(buildVerticalSliderExample())
    return page
}

/** A basic slider: responding to value changes (ValueChanged). */
private fun buildSimpleSliderExample(): WComponent {
    val result = WLabel("Value: 0")

    val slider = WSlider()
    slider.width = 300.0
    slider.addChangeListener { value ->
        result.text = "Value: ${value.toInt()}"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(slider)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple slider (ValueChanged)", row)
}

/** Range and step: minimum / maximum / stepFrequency / header. */
private fun buildRangeSliderExample(): WComponent {
    val slider = WSlider(minimum = 500.0, maximum = 1000.0, value = 800.0)
    slider.width = 300.0
    slider.stepFrequency = 10.0
    slider.header = "Range 500-1000, step 10"
    return buildExample("Range and step (Minimum / Maximum / StepFrequency / Header)", slider)
}

/** Tick marks: tickFrequency / tickPlacement and snapsTo. */
private fun buildTickSliderExample(): WComponent {
    val slider = WSlider(maximum = 50.0)
    slider.width = 300.0
    slider.tickFrequency = 10.0
    slider.tickPlacement = TickPlacement.OUTSIDE
    slider.snapsTo = SliderSnapsTo.TICKS
    return buildExample("Tick marks (TickFrequency / TickPlacement / SnapsTo)", slider)
}

/** Vertical: orientation and isDirectionReversed. */
private fun buildVerticalSliderExample(): WComponent {
    val slider = WSlider(value = 30.0)
    slider.height = 160.0
    slider.orientation = Orientation.VERTICAL
    slider.isDirectionReversed = true
    return buildExample("Vertical (Orientation / IsDirectionReversed)", slider)
}

// endregion

// region SplitButton

/** The SplitButton page: lines up demos for trying out WSplitButton's various features. */
internal fun buildSplitButtonPage(): WComponent {
    val page = buildPage("SplitButton", "A two-part button split between clicking the body and expanding choices. Try out WSplitButton's various features.")

    page.add(buildSimpleSplitButtonExample())
    return page
}

/** A basic split button: clicking the body applies the current color, the arrow picks a color. */
private fun buildSimpleSplitButtonExample(): WComponent {
    val tile = buildTile(WColor.LIGHT_GRAY, width = 48.0, height = 48.0)
    var currentColor = WColor.RED

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    val splitButton = WSplitButton("Apply color")
    for ((name, color) in listOf("Red" to WColor.RED, "Green" to WColor.GREEN, "Blue" to WColor.BLUE)) {
        menu.add(
            WButton(name).also { button ->
                button.width = 100.0
                button.addActionListener {
                    currentColor = color
                    tile.background = color
                    splitButton.text = "Apply color ($name)"
                    flyout.hide()
                }
            },
        )
    }
    splitButton.flyout = flyout
    splitButton.addActionListener { tile.background = currentColor }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(splitButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(tile)
    return buildExample("A simple split button (Click + Flyout)", row)
}

// endregion

// region ToggleButton

/** The ToggleButton page: lines up demos for trying out WToggleButton's various features. */
internal fun buildToggleButtonPage(): WComponent {
    val page = buildPage("ToggleButton", "A button that toggles on/off each time it's pressed. Try out WToggleButton's various features.")

    page.add(buildSimpleToggleButtonExample())
    return page
}

/** A basic toggle button: displaying isChecked and toggling it from code. */
private fun buildSimpleToggleButtonExample(): WComponent {
    val result = WLabel("State: off")

    val toggleButton = WToggleButton("Mute")
    toggleButton.addItemListener { checked ->
        result.text = if (checked == true) "State: on" else "State: off"
    }

    val codeButton = WButton("Toggle from code")
    codeButton.addActionListener {
        toggleButton.isChecked = toggleButton.isChecked != true
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleButton)
    row.add(codeButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple toggle button (IsChecked)", row)
}

// endregion

// region ToggleSplitButton

/** The ToggleSplitButton page: lines up demos for trying out WToggleSplitButton's various features. */
internal fun buildToggleSplitButtonPage(): WComponent {
    val page = buildPage("ToggleSplitButton", "A split button whose body toggles on/off when clicked. Try out WToggleSplitButton's various features.")

    page.add(buildSimpleToggleSplitButtonExample())
    return page
}

/** A basic toggle split button: toggling a bulleted list on/off and choosing its marker. */
private fun buildSimpleToggleSplitButtonExample(): WComponent {
    val result = WLabel("")
    var marker = "•"
    val items = listOf("Apple", "Orange", "Grape")

    val toggleSplitButton = WToggleSplitButton("Bulleted list")
    fun render() {
        val prefix = if (toggleSplitButton.isChecked) marker else ""
        result.text = items.joinToString("\n") { prefix + it }
    }
    render()

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    for (name in listOf("•", "-", "◆")) {
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
    row.add(toggleSplitButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple toggle split button (IsChecked + Flyout)", row)
}

// endregion

// region ToggleSwitch

/** The ToggleSwitch page: lines up demos for trying out WToggleSwitch's various features. */
internal fun buildToggleSwitchPage(): WComponent {
    val page = buildPage("ToggleSwitch", "A switch for toggling between two on/off states. Try out WToggleSwitch's various features.")

    page.add(buildSimpleToggleSwitchExample())
    page.add(buildCustomContentToggleSwitchExample())
    return page
}

/** A basic toggle switch: responding to toggling (Toggled) and switching it from code. */
private fun buildSimpleToggleSwitchExample(): WComponent {
    val result = WLabel("State: off")

    val toggleSwitch = WToggleSwitch()
    toggleSwitch.addItemListener { isOn ->
        result.text = if (isOn) "State: on" else "State: off"
    }

    val codeButton = WButton("Toggle from code")
    codeButton.addActionListener {
        toggleSwitch.isOn = !toggleSwitch.isOn
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleSwitch)
    row.add(codeButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple toggle switch (IsOn / Toggled)", row)
}

/** Customizing the displayed text: header / onContent / offContent. */
private fun buildCustomContentToggleSwitchExample(): WComponent {
    val toggleSwitch = WToggleSwitch(header = "Server status")
    toggleSwitch.onContent = "Running"
    toggleSwitch.offContent = "Stopped"
    return buildExample("Displayed text (Header / OnContent / OffContent)", toggleSwitch)
}

// endregion
