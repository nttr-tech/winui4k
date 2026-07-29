package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.ColorSpectrumShape;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SliderSnapsTo;
import com.appkitbox.winui4k.TickPlacement;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WColorPicker;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WCommand;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WDropDownButton;
import com.appkitbox.winui4k.WFlyout;
import com.appkitbox.winui4k.WHyperlinkButton;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WRatingControl;
import com.appkitbox.winui4k.WRepeatButton;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WSplitButton;
import com.appkitbox.winui4k.WToggleButton;
import com.appkitbox.winui4k.WToggleSplitButton;
import com.appkitbox.winui4k.WToggleSwitch;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


/*
 * Basic input category: demo pages for Button / CheckBox / ColorPicker / ComboBox / DropDownButton / HyperlinkButton / RadioButton / RatingControl / RepeatButton / Slider / SplitButton / ToggleButton / ToggleSplitButton / ToggleSwitch.
 */
final class BasicInputPages {
    private BasicInputPages() {
    }

    // region Button

    /** The Button page: lines up demos for trying out WButton's various features. */
    static WComponent buildButtonPage() {
        WPanel page = GalleryScaffold.buildPage("Button", "A button that responds to clicks. Try out WButton's various features.");

        page.add(buildSimpleButtonExample());
        page.add(buildFlyoutButtonExample());
        page.add(buildCommandButtonExample());
        return page;
    }

    /** A basic button: responding to clicks and toggling isEnabled. */
    private static WComponent buildSimpleButtonExample() {
        WLabel result = new WLabel("Click count: 0");
        int[] count = {0};

        WButton standardButton = new WButton("Standard XAML Button");
        standardButton.addActionListener(() -> {
            count[0]++;
            result.setText("Click count: " + count[0]);
        });

        WButton toggleButton = new WButton("Disable button");
        toggleButton.addActionListener(() -> {
            standardButton.setEnabled(!standardButton.isEnabled());
            toggleButton.setText(standardButton.isEnabled() ? "Disable button" : "Enable button");
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(standardButton);
        row.add(toggleButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Simple button", row);
    }

    /** A button with a flyout: opens a popup on click. */
    private static WComponent buildFlyoutButtonExample() {
        WPanel flyoutContent = new WPanel(8.0);
        WFlyout flyout = new WFlyout(flyoutContent);

        flyoutContent.add(new WLabel("Delete all items?"));
        WButton deleteButton = new WButton("Yes, delete everything");
        deleteButton.addActionListener(() -> {
            flyout.hide();
        });
        flyoutContent.add(deleteButton);

        WButton flyoutButton = new WButton("Show options");
        flyoutButton.setFlyout(flyout);
        return GalleryScaffold.buildExample("Button with a flyout", flyoutButton);
    }

    /** A button with a WCommand: running the command and auto-disabling via isEnabled. */
    private static WComponent buildCommandButtonExample() {
        WLabel result = new WLabel("Command has not run yet");
        WCommand command = new WCommand( (parameter) -> {
            result.setText("Command ran (parameter = " + parameter + ")");
        });

        WButton commandButton = new WButton("Run command");
        commandButton.setCommand(command);
        commandButton.setCommandParameter("Gallery");

        WButton toggleButton = new WButton("Disable command");
        toggleButton.addActionListener(() -> {
            command.setEnabled(!command.isEnabled());
            toggleButton.setText(command.isEnabled() ? "Disable command" : "Enable command");
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(commandButton);
        row.add(toggleButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Button with a command", row);
    }

    // endregion

    // region CheckBox

    /** The CheckBox page: lines up demos for trying out WCheckBox's various features. */
    static WComponent buildCheckBoxPage() {
        WPanel page = GalleryScaffold.buildPage("CheckBox", "A control for toggling checked/unchecked (and indeterminate). Try out WCheckBox's various features.");

        page.add(buildSimpleCheckBoxExample());
        page.add(buildThreeStateCheckBoxExample());
        page.add(buildSelectAllCheckBoxExample());
        return page;
    }

    /** A basic checkbox: responding to state changes (Checked / Unchecked). */
    private static WComponent buildSimpleCheckBoxExample() {
        WLabel result = new WLabel("State: off");

        WCheckBox checkBox = new WCheckBox("Receive notifications");
        checkBox.addItemListener((checked) -> {
            result.setText(Boolean.TRUE.equals(checked) ? "State: on" : "State: off");
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(checkBox);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple checkbox", row);
    }

    /** Three states: isThreeState cycles on -> indeterminate -> off. */
    private static WComponent buildThreeStateCheckBoxExample() {
        WLabel result = new WLabel("State: off");

        WCheckBox checkBox = new WCheckBox("Three-state checkbox");
        checkBox.setThreeState(true);
        checkBox.addItemListener((checked) -> {
            if (checked == null) {
                result.setText("State: indeterminate");
            } else if (checked) {
                result.setText("State: on");
            } else {
                result.setText("State: off");
            }
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(checkBox);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Three states (IsThreeState)", row);
    }

    /** Select all: a parent checkbox controls three children, and the children's state makes the parent indeterminate. */
    private static WComponent buildSelectAllCheckBoxExample() {
        WCheckBox parent = new WCheckBox("Select all options");
        List<WCheckBox> children = Arrays.asList(
            new WCheckBox("Option 1"),
            new WCheckBox("Option 2"),
            new WCheckBox("Option 3"));

        // Guard against an infinite event loop from the parent and children updating each other
        boolean[] updating = {false};
        parent.addItemListener((checked) -> {
            if (updating[0] || checked == null) {
            }
            updating[0] = true;
            for (WCheckBox child : children) {
                child.setChecked(checked);
            }
            updating[0] = false;
        });
        for (WCheckBox child : children) {
            child.setMargin(4.0);
            child.addItemListener((checked) -> {
                if (updating[0]) {
                }
                updating[0] = true;
                int checkedCount = 0;
                for (WCheckBox c : children) {
                    if (Boolean.TRUE.equals(c.isChecked())) {
                        checkedCount++;
                    }
                }
                if (checkedCount == 0) {
                    parent.setChecked(false);
                } else if (checkedCount == children.size()) {
                    parent.setChecked(true);
                } else {
                    parent.setChecked(null);
                }
                updating[0] = false;
            });
        }

        WPanel body = new WPanel(4.0);
        body.add(parent);
        for (WCheckBox child : children) {
            body.add(child);
        }
        return GalleryScaffold.buildExample("Select all (parent/child linkage and indeterminate state)", body);
    }

    // endregion

    // region ColorPicker

    /** The ColorPicker page: lines up demos for trying out WColorPicker's various features. */
    static WComponent buildColorPickerPage() {
        WPanel page = GalleryScaffold.buildPage("ColorPicker", "A control for picking a color from a spectrum. Try out WColorPicker's various features.");

        page.add(buildSimpleColorPickerExample());
        page.add(buildColorPickerOptionsExample());
        return page;
    }

    /** A basic color picker: reflecting color changes (ColorChanged) onto a tile. */
    private static WComponent buildSimpleColorPickerExample() {
        WBorder tile = GalleryScaffold.buildTile(WColor.Companion.getBLUE(), 64.0, 64.0);

        WColorPicker colorPicker = new WColorPicker();
        colorPicker.setColor(WColor.Companion.getBLUE());
        colorPicker.addChangeListener((color) -> {
            tile.setBackground(color);
        });

        WPanel row = new WPanel(24.0, Orientation.HORIZONTAL);
        row.add(colorPicker);
        row.add(tile);
        return GalleryScaffold.buildExample("A simple color picker (Color / ColorChanged)", row);
    }

    /** Display options: isAlphaEnabled / spectrumShape / isMoreButtonVisible. */
    private static WComponent buildColorPickerOptionsExample() {
        WColorPicker colorPicker = new WColorPicker();
        colorPicker.setAlphaEnabled(true);
        colorPicker.setMoreButtonVisible(true);

        WButton shapeButton = new WButton("Make the spectrum a ring");
        shapeButton.addActionListener(() -> {
            boolean ring = colorPicker.getSpectrumShape() == ColorSpectrumShape.RING;
            colorPicker.setSpectrumShape(ring ? ColorSpectrumShape.BOX : ColorSpectrumShape.RING);
            shapeButton.setText(ring ? "Make the spectrum a ring" : "Make the spectrum a box");
        });

        WPanel body = new WPanel(8.0);
        body.add(shapeButton);
        body.add(colorPicker);
        return GalleryScaffold.buildExample("Display options (IsAlphaEnabled / ColorSpectrumShape / IsMoreButtonVisible)", body);
    }

    // endregion

    // region ComboBox

    /** The ComboBox page: lines up demos for trying out WComboBox's various features. */
    static WComponent buildComboBoxPage() {
        WPanel page = GalleryScaffold.buildPage("ComboBox", "A control for picking one item from a drop-down. Try out WComboBox's various features.");

        page.add(buildSimpleComboBoxExample());
        page.add(buildHeaderComboBoxExample());
        page.add(buildEditableComboBoxExample());
        return page;
    }

    /** A basic combo box: responding to selection changes (SelectionChanged). */
    private static WComponent buildSimpleComboBoxExample() {
        WLabel result = new WLabel("Selected: none");

        WComboBox comboBox = new WComboBox(Arrays.asList("Red", "Green", "Blue", "Yellow"));
        comboBox.setWidth(200.0);
        comboBox.addListSelectionListener(() -> {
            String item = comboBox.getSelectedItem();
            result.setText(item == null
                ? "Selected: none"
                : "Selected: " + item + " (index = " + comboBox.getSelectedIndex() + ")");
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(comboBox);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple combo box (SelectionChanged)", row);
    }

    /** Heading and placeholder: header / placeholderText. */
    private static WComponent buildHeaderComboBoxExample() {
        WComboBox comboBox = new WComboBox(Arrays.asList("Meiryo", "Yu Gothic", "BIZ UDGothic"));
        comboBox.setWidth(200.0);
        comboBox.setHeader("Font");
        comboBox.setPlaceholderText("Choose a font");
        return GalleryScaffold.buildExample("Heading and placeholder (Header / PlaceholderText)", comboBox);
    }

    /** An editable combo box: using isEditable and TextSubmitted to add values not in the list. */
    private static WComponent buildEditableComboBoxExample() {
        WLabel result = new WLabel("Submitted: none");

        WComboBox comboBox = new WComboBox(Arrays.asList("10", "20", "30"));
        comboBox.setWidth(200.0);
        comboBox.setEditable(true);
        comboBox.addTextSubmitListener((text) -> {
            result.setText("Submitted: " + text);
            boolean exists = false;
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                if (comboBox.getItem(i).equals(text)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                comboBox.addItem(text);
            }
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(comboBox);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("An editable combo box (IsEditable / TextSubmitted)", row);
    }

    // endregion

    // region DropDownButton

    /** The DropDownButton page: lines up demos for trying out WDropDownButton's various features. */
    static WComponent buildDropDownButtonPage() {
        WPanel page = GalleryScaffold.buildPage("DropDownButton", "A button that opens a flyout of choices when clicked. Try out WDropDownButton's various features.");

        page.add(buildSimpleDropDownButtonExample());
        return page;
    }

    /** A basic drop-down button: choosing from a flyout menu. */
    private static WComponent buildSimpleDropDownButtonExample() {
        WLabel result = new WLabel("Selected: none");

        WPanel menu = new WPanel(4.0);
        WFlyout flyout = new WFlyout(menu);
        for (String name : Arrays.asList("Mail", "Calendar", "Contacts")) {
            WButton button = new WButton(name);
            button.setWidth(120.0);
            button.addActionListener(() -> {
                result.setText("Selected: " + name);
                flyout.hide();
            });
            menu.add(button);
        }

        WDropDownButton dropDownButton = new WDropDownButton("New");
        dropDownButton.setFlyout(flyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(dropDownButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple drop-down button (Flyout)", row);
    }

    // endregion

    // region HyperlinkButton

    /** The HyperlinkButton page: lines up demos for trying out WHyperlinkButton's various features. */
    static WComponent buildHyperlinkButtonPage() {
        WPanel page = GalleryScaffold.buildPage("HyperlinkButton", "A button displayed as a hyperlink. Try out WHyperlinkButton's various features.");

        page.add(buildNavigateUriHyperlinkExample());
        page.add(buildClickHyperlinkExample());
        return page;
    }

    /** NavigateUri: clicking opens the default browser. */
    private static WComponent buildNavigateUriHyperlinkExample() {
        WHyperlinkButton hyperlinkButton = new WHyperlinkButton(
            "Open the WinUI 3 documentation",
            "https://learn.microsoft.com/windows/apps/winui/winui3/");
        return GalleryScaffold.buildExample("Navigating to a URI (NavigateUri)", hyperlinkButton);
    }

    /** Handling Click: respond to clicks in code without setting NavigateUri. */
    private static WComponent buildClickHyperlinkExample() {
        WLabel result = new WLabel("Click count: 0");
        int[] count = {0};

        WHyperlinkButton hyperlinkButton = new WHyperlinkButton("A link whose click is handled in code", "");
        hyperlinkButton.addActionListener(() -> {
            count[0]++;
            result.setText("Click count: " + count[0]);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(hyperlinkButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Handling the Click event", row);
    }

    // endregion

    // region RadioButton

    /** The RadioButton page: lines up demos for trying out WRadioButton's various features. */
    static WComponent buildRadioButtonPage() {
        WPanel page = GalleryScaffold.buildPage("RadioButton", "A control for picking exactly one option within a group. Try out WRadioButton's various features.");

        page.add(buildSimpleRadioButtonExample());
        page.add(buildRadioButtonGroupExample());
        return page;
    }

    /** A basic radio button: mutually exclusive selection within the same group. */
    private static WComponent buildSimpleRadioButtonExample() {
        WLabel result = new WLabel("Selected: none");

        WPanel body = new WPanel(4.0);
        for (String name : Arrays.asList("Small", "Medium", "Large")) {
            WRadioButton radioButton = new WRadioButton(name);
            radioButton.setGroupName("Size");
            radioButton.addItemListener((checked) -> {
                if (Boolean.TRUE.equals(checked)) {
                    result.setText("Selected: " + name);
                }
            });
            body.add(radioButton);
        }
        body.add(result);
        return GalleryScaffold.buildExample("A simple radio button (mutually exclusive selection)", body);
    }

    /** Multiple groups: separate groupName values let groups be selected independently. */
    private static WComponent buildRadioButtonGroupExample() {
        WLabel result = new WLabel("Background: unselected / Foreground: unselected");
        String[] background = {"unselected"};
        String[] foreground = {"unselected"};

        WPanel row = new WPanel(32.0, Orientation.HORIZONTAL);
        row.add(buildRadioButtonGroup("background", "background", (name) -> {
            background[0] = name;
            result.setText("Background: " + background[0] + " / Foreground: " + foreground[0]);
        }));
        row.add(buildRadioButtonGroup("foreground", "foreground", (name) -> {
            foreground[0] = name;
            result.setText("Background: " + background[0] + " / Foreground: " + foreground[0]);
        }));

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample("Multiple groups (GroupName)", body);
    }

    /** One group of radio buttons (a local function named buildGroup in the Kotlin version). */
    private static WComponent buildRadioButtonGroup(String title, String group, Consumer<String> onSelect) {
        WPanel panel = new WPanel(4.0);
        panel.add(new WLabel(title));
        for (String name : Arrays.asList("White", "Black", "Blue")) {
            WRadioButton radioButton = new WRadioButton(name);
            radioButton.setGroupName(group);
            radioButton.addItemListener((checked) -> {
                if (Boolean.TRUE.equals(checked)) {
                    onSelect.accept(name);
                }
            });
            panel.add(radioButton);
        }
        return panel;
    }

    // endregion

    // region RatingControl

    /** The RatingControl page: lines up demos for trying out WRatingControl's various features. */
    static WComponent buildRatingControlPage() {
        WPanel page = GalleryScaffold.buildPage("RatingControl", "A control for entering a star rating. Try out WRatingControl's various features.");

        page.add(buildSimpleRatingExample());
        page.add(buildPlaceholderRatingExample());
        page.add(buildReadOnlyRatingExample());
        return page;
    }

    /** A basic rating: responding to value changes (ValueChanged) and clearing it. */
    private static WComponent buildSimpleRatingExample() {
        WLabel result = new WLabel("Rating: unset");

        WRatingControl rating = new WRatingControl();
        rating.setClearEnabled(true);
        rating.addChangeListener((value) -> {
            result.setText(value < 0 ? "Rating: unset" : "Rating: " + (int) value);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(rating);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple rating (ValueChanged / IsClearEnabled)", row);
    }

    /** A placeholder: faintly shows something like an average value before the user rates it. */
    private static WComponent buildPlaceholderRatingExample() {
        WRatingControl rating = new WRatingControl();
        rating.setPlaceholderValue(3.5);
        rating.setCaption("512 reviews");
        return GalleryScaffold.buildExample("A placeholder (PlaceholderValue / Caption)", rating);
    }

    /** Read-only and star count: isReadOnly / maxRating. */
    private static WComponent buildReadOnlyRatingExample() {
        WRatingControl rating = new WRatingControl();
        rating.setMaxRating(10);
        rating.setValue(7.0);
        rating.setReadOnly(true);

        WButton toggleButton = new WButton("Turn off read-only");
        toggleButton.addActionListener(() -> {
            rating.setReadOnly(!rating.isReadOnly());
            toggleButton.setText(rating.isReadOnly() ? "Turn off read-only" : "Make read-only");
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(rating);
        row.add(toggleButton);
        return GalleryScaffold.buildExample("Read-only and star count (IsReadOnly / MaxRating)", row);
    }

    // endregion

    // region RepeatButton

    /** The RepeatButton page: lines up demos for trying out WRepeatButton's various features. */
    static WComponent buildRepeatButtonPage() {
        WPanel page = GalleryScaffold.buildPage("RepeatButton", "A button that fires Click repeatedly while held down. Try out WRepeatButton's various features.");

        page.add(buildSimpleRepeatButtonExample());
        page.add(buildRepeatButtonSpeedExample());
        return page;
    }

    /** A basic repeat button: the counter keeps increasing while held down. */
    private static WComponent buildSimpleRepeatButtonExample() {
        WLabel result = new WLabel("Click count: 0");
        int[] count = {0};

        WRepeatButton repeatButton = new WRepeatButton("Press and hold");
        repeatButton.addActionListener(() -> {
            count[0]++;
            result.setText("Click count: " + count[0]);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(repeatButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple repeat button", row);
    }

    /** Repeat speed: the difference between delay (wait before the first fire) and interval (time between fires). */
    private static WComponent buildRepeatButtonSpeedExample() {
        WLabel result = new WLabel("Click count: 0");
        int[] count = {0};

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        String[] labels = {"Slow (500ms interval)", "Fast (50ms interval)"};
        int[] delays = {500, 250};
        int[] intervals = {500, 50};
        for (int i = 0; i < labels.length; i++) {
            WRepeatButton repeatButton = new WRepeatButton(labels[i]);
            repeatButton.setDelay(delays[i]);
            repeatButton.setInterval(intervals[i]);
            repeatButton.addActionListener(() -> {
                count[0]++;
                result.setText("Click count: " + count[0]);
            });
            row.add(repeatButton);
        }
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Repeat speed (Delay / Interval)", row);
    }

    // endregion

    // region Slider

    /** The Slider page: lines up demos for trying out WSlider's various features. */
    static WComponent buildSliderPage() {
        WPanel page = GalleryScaffold.buildPage("Slider", "A control for picking a value in a range by moving a thumb along a track. Try out WSlider's various features.");

        page.add(buildSimpleSliderExample());
        page.add(buildRangeSliderExample());
        page.add(buildTickSliderExample());
        page.add(buildVerticalSliderExample());
        return page;
    }

    /** A basic slider: responding to value changes (ValueChanged). */
    private static WComponent buildSimpleSliderExample() {
        WLabel result = new WLabel("Value: 0");

        WSlider slider = new WSlider(0.0, 100.0, 0.0);
        slider.setWidth(300.0);
        slider.addChangeListener((value) -> {
            result.setText("Value: " + (int) value);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(slider);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple slider (ValueChanged)", row);
    }

    /** Range and step: minimum / maximum / stepFrequency / header. */
    private static WComponent buildRangeSliderExample() {
        WSlider slider = new WSlider(500.0, 1000.0, 800.0);
        slider.setWidth(300.0);
        slider.setStepFrequency(10.0);
        slider.setHeader("Range 500-1000, step 10");
        return GalleryScaffold.buildExample("Range and step (Minimum / Maximum / StepFrequency / Header)", slider);
    }

    /** Tick marks: tickFrequency / tickPlacement and snapsTo. */
    private static WComponent buildTickSliderExample() {
        WSlider slider = new WSlider(0.0, 50.0, 0.0);
        slider.setWidth(300.0);
        slider.setTickFrequency(10.0);
        slider.setTickPlacement(TickPlacement.OUTSIDE);
        slider.setSnapsTo(SliderSnapsTo.TICKS);
        return GalleryScaffold.buildExample("Tick marks (TickFrequency / TickPlacement / SnapsTo)", slider);
    }

    /** Vertical: orientation and isDirectionReversed. */
    private static WComponent buildVerticalSliderExample() {
        WSlider slider = new WSlider(0.0, 100.0, 30.0);
        slider.setHeight(160.0);
        slider.setOrientation(Orientation.VERTICAL);
        slider.setDirectionReversed(true);
        return GalleryScaffold.buildExample("Vertical (Orientation / IsDirectionReversed)", slider);
    }

    // endregion

    // region SplitButton

    /** The SplitButton page: lines up demos for trying out WSplitButton's various features. */
    static WComponent buildSplitButtonPage() {
        WPanel page = GalleryScaffold.buildPage("SplitButton", "A two-part button split between clicking the body and expanding choices. Try out WSplitButton's various features.");

        page.add(buildSimpleSplitButtonExample());
        return page;
    }

    /** A basic split button: clicking the body applies the current color, the arrow picks a color. */
    private static WComponent buildSimpleSplitButtonExample() {
        WBorder tile = GalleryScaffold.buildTile(WColor.Companion.getLIGHT_GRAY(), 48.0, 48.0);
        WColor[] currentColor = {WColor.Companion.getRED()};

        WPanel menu = new WPanel(4.0);
        WFlyout flyout = new WFlyout(menu);
        WSplitButton splitButton = new WSplitButton("Apply color");
        String[] names = {"Red", "Green", "Blue"};
        WColor[] colors = {WColor.Companion.getRED(), WColor.Companion.getGREEN(), WColor.Companion.getBLUE()};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            WColor color = colors[i];
            WButton button = new WButton(name);
            button.setWidth(100.0);
            button.addActionListener(() -> {
                currentColor[0] = color;
                tile.setBackground(color);
                splitButton.setText("Apply color (" + name + ")");
                flyout.hide();
            });
            menu.add(button);
        }
        splitButton.setFlyout(flyout);
        splitButton.addActionListener(() -> {
            tile.setBackground(currentColor[0]);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        splitButton.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(splitButton);
        row.add(tile);
        return GalleryScaffold.buildExample("A simple split button (Click + Flyout)", row);
    }

    // endregion

    // region ToggleButton

    /** The ToggleButton page: lines up demos for trying out WToggleButton's various features. */
    static WComponent buildToggleButtonPage() {
        WPanel page = GalleryScaffold.buildPage("ToggleButton", "A button that toggles on/off each time it's pressed. Try out WToggleButton's various features.");

        page.add(buildSimpleToggleButtonExample());
        return page;
    }

    /** A basic toggle button: displaying isChecked and toggling it from code. */
    private static WComponent buildSimpleToggleButtonExample() {
        WLabel result = new WLabel("State: off");

        WToggleButton toggleButton = new WToggleButton("Mute");
        toggleButton.addItemListener((checked) -> {
            result.setText(Boolean.TRUE.equals(checked) ? "State: on" : "State: off");
        });

        WButton codeButton = new WButton("Toggle from code");
        codeButton.addActionListener(() -> {
            toggleButton.setChecked(!Boolean.TRUE.equals(toggleButton.isChecked()));
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(toggleButton);
        row.add(codeButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple toggle button (IsChecked)", row);
    }

    // endregion

    // region ToggleSplitButton

    /** The ToggleSplitButton page: lines up demos for trying out WToggleSplitButton's various features. */
    static WComponent buildToggleSplitButtonPage() {
        WPanel page = GalleryScaffold.buildPage("ToggleSplitButton", "A split button whose body toggles on/off when clicked. Try out WToggleSplitButton's various features.");

        page.add(buildSimpleToggleSplitButtonExample());
        return page;
    }

    /** A basic toggle split button: toggling a bulleted list on/off and choosing its marker. */
    private static WComponent buildSimpleToggleSplitButtonExample() {
        WLabel result = new WLabel("");
        String[] marker = {"•"};
        List<String> items = Arrays.asList("Apple", "Orange", "Grape");

        WToggleSplitButton toggleSplitButton = new WToggleSplitButton("Bulleted list");
        Runnable render = () -> {
            String prefix = toggleSplitButton.isChecked() ? marker[0] : "";
            StringBuilder text = new StringBuilder();
            for (String item : items) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(prefix).append(item);
            }
            result.setText(text.toString());
        };
        render.run();

        WPanel menu = new WPanel(4.0);
        WFlyout flyout = new WFlyout(menu);
        for (String name : Arrays.asList("•", "-", "◆")) {
            WButton button = new WButton(name);
            button.setWidth(80.0);
            button.addActionListener(() -> {
                marker[0] = name;
                toggleSplitButton.setChecked(true);
                render.run();
                flyout.hide();
            });
            menu.add(button);
        }
        toggleSplitButton.setFlyout(flyout);
        toggleSplitButton.addItemListener((checked) -> {
            render.run();
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        toggleSplitButton.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(toggleSplitButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple toggle split button (IsChecked + Flyout)", row);
    }

    // endregion

    // region ToggleSwitch

    /** The ToggleSwitch page: lines up demos for trying out WToggleSwitch's various features. */
    static WComponent buildToggleSwitchPage() {
        WPanel page = GalleryScaffold.buildPage("ToggleSwitch", "A switch for toggling between two on/off states. Try out WToggleSwitch's various features.");

        page.add(buildSimpleToggleSwitchExample());
        page.add(buildCustomContentToggleSwitchExample());
        return page;
    }

    /** A basic toggle switch: responding to toggling (Toggled) and switching it from code. */
    private static WComponent buildSimpleToggleSwitchExample() {
        WLabel result = new WLabel("State: off");

        WToggleSwitch toggleSwitch = new WToggleSwitch("");
        toggleSwitch.addItemListener((isOn) -> {
            result.setText(isOn ? "State: on" : "State: off");
        });

        WButton codeButton = new WButton("Toggle from code");
        codeButton.addActionListener(() -> {
            toggleSwitch.setOn(!toggleSwitch.isOn());
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(toggleSwitch);
        codeButton.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(codeButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple toggle switch (IsOn / Toggled)", row);
    }

    /** Customizing the displayed text: header / onContent / offContent. */
    private static WComponent buildCustomContentToggleSwitchExample() {
        WToggleSwitch toggleSwitch = new WToggleSwitch("Server status");
        toggleSwitch.setOnContent("Running");
        toggleSwitch.setOffContent("Stopped");
        return GalleryScaffold.buildExample("Displayed text (Header / OnContent / OffContent)", toggleSwitch);
    }

    // endregion
}
