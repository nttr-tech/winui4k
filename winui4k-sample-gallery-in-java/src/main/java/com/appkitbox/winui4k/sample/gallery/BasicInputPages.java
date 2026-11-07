package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.ColorSpectrumShape;
import com.appkitbox.winui4k.HorizontalTextPosition;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SliderSnapsTo;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.TickPlacement;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.VerticalTextPosition;
import com.appkitbox.winui4k.WAbstractAction;
import com.appkitbox.winui4k.WAction;
import com.appkitbox.winui4k.WActionEvent;
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
import com.appkitbox.winui4k.WFontIcon;
import com.appkitbox.winui4k.WHyperlinkButton;
import com.appkitbox.winui4k.WItemEvent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WRadioButton;
import com.appkitbox.winui4k.WRatingControl;
import com.appkitbox.winui4k.WRepeatButton;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WSplitButton;
import com.appkitbox.winui4k.WTextField;
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
        WPanel page = GalleryScaffold.buildPage("Button", Messages.get("button.description"));

        page.add(buildSimpleButtonExample());
        page.add(buildFlyoutButtonExample());
        page.add(buildCommandButtonExample());
        page.add(buildIconButtonExample());
        page.add(buildActionButtonExample());
        page.add(buildAppearanceButtonExample());
        page.add(buildDefaultButtonExample());
        return page;
    }

    /** A basic button: responding to clicks and toggling isEnabled. */
    private static WComponent buildSimpleButtonExample() {
        WLabel result = new WLabel(Messages.get("button.example.simple.clicks", "0"));
        int[] count = {0};

        WButton standardButton = new WButton("Standard XAML Button");
        standardButton.addActionListener(() -> {
            count[0]++;
            result.setText(Messages.get("button.example.simple.clicks", String.valueOf(count[0])));
        });

        WButton toggleButton = new WButton(Messages.get("button.example.simple.disable"));
        toggleButton.addActionListener(() -> {
            standardButton.setEnabled(!standardButton.isEnabled());
            toggleButton.setText(standardButton.isEnabled() ? Messages.get("button.example.simple.disable") : Messages.get("button.example.simple.enable"));
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(standardButton);
        row.add(toggleButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("button.example.simple.title"), row);
    }

    /** A button with a flyout: opens a popup on click. */
    private static WComponent buildFlyoutButtonExample() {
        WPanel flyoutContent = new WPanel(8.0);
        WFlyout flyout = new WFlyout(flyoutContent);

        flyoutContent.add(new WLabel(Messages.get("button.example.flyout.question")));
        WButton deleteButton = new WButton(Messages.get("button.example.flyout.confirm"));
        deleteButton.addActionListener(() -> {
            flyout.hide();
        });
        flyoutContent.add(deleteButton);

        WButton flyoutButton = new WButton(Messages.get("button.example.flyout.show"));
        flyoutButton.setFlyout(flyout);
        return GalleryScaffold.buildExample(Messages.get("button.example.flyout.title"), flyoutButton);
    }

    /** A button with a WCommand: running the command and auto-disabling via isEnabled. */
    private static WComponent buildCommandButtonExample() {
        WLabel result = new WLabel(Messages.get("button.example.command.none"));
        WCommand command = new WCommand( (parameter) -> {
            result.setText(Messages.get("button.example.command.executed", String.valueOf(parameter)));
        });

        WButton commandButton = new WButton(Messages.get("button.example.command.run"));
        commandButton.setCommand(command);
        commandButton.setCommandParameter("Gallery");

        WButton toggleButton = new WButton(Messages.get("button.example.command.disable"));
        toggleButton.addActionListener(() -> {
            command.setEnabled(!command.isEnabled());
            toggleButton.setText(command.isEnabled() ? Messages.get("button.example.command.disable") : Messages.get("button.example.command.enable"));
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(commandButton);
        row.add(toggleButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("button.example.command.title"), row);
    }

    /** Buttons with icons: same shape as Swing's JButton(String, Icon); tries a Symbol, a font glyph, text positions */
    private static WComponent buildIconButtonExample() {
        WButton saveButton = new WButton(Messages.get("button.save"), Symbol.SAVE);
        saveButton.setToolTipText(Messages.get("button.save.tooltip"));

        WButton nextButton = new WButton(Messages.get("button.example.icon.next"), Symbol.FORWARD);
        nextButton.setHorizontalTextPosition(HorizontalTextPosition.LEADING); // Put the text before (left of) the icon

        WButton glyphButton = new WButton(Messages.get("button.example.icon.folder"), new WFontIcon("\uE8B7")); // A Segoe Fluent Icons glyph
        glyphButton.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
        glyphButton.setVerticalTextPosition(VerticalTextPosition.TOP); // Put the text above the icon
        glyphButton.setIconTextGap(8.0);

        WButton iconOnlyButton = new WButton(Symbol.SETTING);
        iconOnlyButton.setToolTipText(Messages.get("button.example.icon.settings"));

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(saveButton);
        row.add(nextButton);
        row.add(glyphButton);
        row.add(iconOnlyButton);
        return GalleryScaffold.buildExample(Messages.get("button.example.icon.title"), row);
    }

    /** Buttons with a WAction: two buttons share one action's text, tooltip and enabled state (like Swing's Action). */
    private static WComponent buildActionButtonExample() {
        WLabel result = new WLabel(Messages.get("button.notrun"));
        int[] count = {0};
        WAbstractAction action = new WAbstractAction(Messages.get("button.save"), Symbol.SAVE) {
            @Override
            public void actionPerformed(WActionEvent e) {
                count[0]++;
                result.setText(Messages.get("button.example.action.executed", String.valueOf(count[0]), String.valueOf(e.getActionCommand())));
            }
        };
        action.putValue(WAction.SHORT_DESCRIPTION, Messages.get("button.save.tooltip"));
        action.putValue(WAction.MNEMONIC_KEY, (int) 'S');

        WButton saveButton = new WButton(action);
        WButton duplicateButton = new WButton(action); // Shares the action, so name and disabled state change together

        WButton renameButton = new WButton(Messages.get("button.example.action.rename"));
        renameButton.addActionListener(() -> {
            boolean renamed = Messages.get("button.save").equals(action.getValue(WAction.NAME));
            action.putValue(WAction.NAME, renamed ? Messages.get("button.example.action.saveover") : Messages.get("button.save"));
        });
        WButton toggleButton = new WButton(Messages.get("button.example.action.disable"));
        toggleButton.addActionListener(() -> {
            action.setEnabled(!action.isEnabled());
            toggleButton.setText(action.isEnabled() ? Messages.get("button.example.action.disable") : Messages.get("button.example.action.enable"));
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(saveButton);
        row.add(duplicateButton);
        row.add(renameButton);
        row.add(toggleButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("button.example.action.title"), row);
    }

    /** Button appearance: background and text colors, no background fill (toolbar-style), and no border. */
    private static WComponent buildAppearanceButtonExample() {
        WButton coloredButton = new WButton(Messages.get("button.example.appearance.colored"));
        coloredButton.setBackground(new WColor(0, 120, 80));
        coloredButton.setForeground(new WColor(255, 255, 255));

        WButton flatButton = new WButton(Messages.get("button.example.appearance.flat"));
        flatButton.setContentAreaFilled(false); // Lightly filled only while the pointer is over it

        WButton borderlessButton = new WButton(Messages.get("button.example.appearance.borderless"));
        borderlessButton.setBorderPainted(false);

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(coloredButton);
        row.add(flatButton);
        row.add(borderlessButton);
        return GalleryScaffold.buildExample(Messages.get("button.example.appearance.title"), row);
    }

    /** Default button: pressing Enter in a text field presses the default button (like JRootPane.setDefaultButton). */
    private static WComponent buildDefaultButtonExample() {
        WLabel result = new WLabel(Messages.get("button.example.default.notsent"));
        WTextField nameField = new WTextField();
        nameField.setPlaceholderText(Messages.get("button.example.default.placeholder"));
        WButton submitButton = new WButton(Messages.get("button.example.default.send"));
        submitButton.addActionListener(() -> result.setText(Messages.get("button.example.default.sent", nameField.getText())));

        WCheckBox useDefault = new WCheckBox(Messages.get("button.example.default.usedefault"));
        useDefault.addItemListener(e -> {
            MainForGallery.galleryFrame.setDefaultButton(e.getStateChange() == WItemEvent.SELECTED ? submitButton : null);
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(nameField);
        row.add(submitButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        WPanel body = new WPanel(8.0);
        body.add(useDefault);
        body.add(row);
        return GalleryScaffold.buildExample(Messages.get("button.example.default.title"), body);
    }

    // endregion

    // region CheckBox

    /** The CheckBox page: lines up demos for trying out WCheckBox's various features. */
    static WComponent buildCheckBoxPage() {
        WPanel page = GalleryScaffold.buildPage("CheckBox", Messages.get("checkbox.description"));

        page.add(buildSimpleCheckBoxExample());
        page.add(buildThreeStateCheckBoxExample());
        page.add(buildSelectAllCheckBoxExample());
        return page;
    }

    /** A basic checkbox: responding to state changes (Checked / Unchecked). */
    private static WComponent buildSimpleCheckBoxExample() {
        WLabel result = new WLabel(Messages.get("checkbox.state.off"));

        WCheckBox checkBox = new WCheckBox(Messages.get("checkbox.example.simple.label"));
        checkBox.addItemListener(e -> {
            result.setText(e.getStateChange() == WItemEvent.SELECTED ? Messages.get("checkbox.state.on") : Messages.get("checkbox.state.off"));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(checkBox);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("checkbox.example.simple.title"), row);
    }

    /** Three states: isThreeState cycles on -> indeterminate -> off. */
    private static WComponent buildThreeStateCheckBoxExample() {
        WLabel result = new WLabel(Messages.get("checkbox.state.off"));

        WCheckBox checkBox = new WCheckBox(Messages.get("checkbox.example.threestate.label"));
        checkBox.setThreeState(true);
        checkBox.addItemListener(e -> {
            if (e.getStateChange() == WItemEvent.INDETERMINATE) {
                result.setText(Messages.get("checkbox.state.indeterminate"));
            } else if (e.getStateChange() == WItemEvent.SELECTED) {
                result.setText(Messages.get("checkbox.state.on"));
            } else {
                result.setText(Messages.get("checkbox.state.off"));
            }
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(checkBox);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("checkbox.example.threestate.title"), row);
    }

    /** Select all: a parent checkbox controls three children, and the children's state makes the parent indeterminate. */
    private static WComponent buildSelectAllCheckBoxExample() {
        WCheckBox parent = new WCheckBox(Messages.get("checkbox.example.selectall.parent"));
        List<WCheckBox> children = Arrays.asList(
            new WCheckBox(Messages.get("checkbox.example.selectall.option1")),
            new WCheckBox(Messages.get("checkbox.example.selectall.option2")),
            new WCheckBox(Messages.get("checkbox.example.selectall.option3")));

        // Guard against an infinite event loop from the parent and children updating each other
        boolean[] updating = {false};
        parent.addItemListener(e -> {
            if (updating[0] || e.getStateChange() == WItemEvent.INDETERMINATE) {
                return;
            }
            updating[0] = true;
            for (WCheckBox child : children) {
                child.setSelected(e.getStateChange() == WItemEvent.SELECTED);
            }
            updating[0] = false;
        });
        for (WCheckBox child : children) {
            child.setMargin(4.0);
            child.addItemListener(e -> {
                if (updating[0]) {
                    return;
                }
                updating[0] = true;
                int checkedCount = 0;
                for (WCheckBox c : children) {
                    if (c.isSelected()) {
                        checkedCount++;
                    }
                }
                if (checkedCount == 0) {
                    parent.setSelected(false);
                } else if (checkedCount == children.size()) {
                    parent.setSelected(true);
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
        return GalleryScaffold.buildExample(Messages.get("checkbox.example.selectall.title"), body);
    }

    // endregion

    // region ColorPicker

    /** The ColorPicker page: lines up demos for trying out WColorPicker's various features. */
    static WComponent buildColorPickerPage() {
        WPanel page = GalleryScaffold.buildPage("ColorPicker", Messages.get("colorpicker.description"));

        page.add(buildSimpleColorPickerExample());
        page.add(buildColorPickerOptionsExample());
        return page;
    }

    /** A basic color picker: reflecting color changes (ColorChanged) onto a tile. */
    private static WComponent buildSimpleColorPickerExample() {
        WBorder tile = GalleryScaffold.buildTile(GalleryTheme.DEMO_BLUE, 64.0, 64.0);

        WColorPicker colorPicker = new WColorPicker();
        colorPicker.setColor(GalleryTheme.DEMO_BLUE);
        colorPicker.addChangeListener((color) -> {
            tile.setBackground(color);
        });

        WPanel row = new WPanel(24.0, Orientation.HORIZONTAL);
        row.add(colorPicker);
        row.add(tile);
        return GalleryScaffold.buildExample(Messages.get("colorpicker.example.simple.title"), row);
    }

    /** Display options: isAlphaEnabled / spectrumShape / isMoreButtonVisible. */
    private static WComponent buildColorPickerOptionsExample() {
        WColorPicker colorPicker = new WColorPicker();
        colorPicker.setAlphaEnabled(true);
        colorPicker.setMoreButtonVisible(true);

        WButton shapeButton = new WButton(Messages.get("colorpicker.example.options.ring"));
        shapeButton.addActionListener(() -> {
            boolean ring = colorPicker.getSpectrumShape() == ColorSpectrumShape.RING;
            colorPicker.setSpectrumShape(ring ? ColorSpectrumShape.BOX : ColorSpectrumShape.RING);
            shapeButton.setText(ring ? Messages.get("colorpicker.example.options.ring") : Messages.get("colorpicker.example.options.box"));
        });

        WPanel body = new WPanel(8.0);
        body.add(shapeButton);
        body.add(colorPicker);
        return GalleryScaffold.buildExample(Messages.get("colorpicker.example.options.title"), body);
    }

    // endregion

    // region ComboBox

    /** The ComboBox page: lines up demos for trying out WComboBox's various features. */
    static WComponent buildComboBoxPage() {
        WPanel page = GalleryScaffold.buildPage("ComboBox", Messages.get("combobox.description"));

        page.add(buildSimpleComboBoxExample());
        page.add(buildHeaderComboBoxExample());
        page.add(buildEditableComboBoxExample());
        return page;
    }

    /** A basic combo box: responding to selection changes (SelectionChanged). */
    private static WComponent buildSimpleComboBoxExample() {
        WLabel result = new WLabel(Messages.get("combobox.example.simple.none"));

        WComboBox comboBox = new WComboBox(Arrays.asList(Messages.get("combobox.example.simple.red"), Messages.get("combobox.example.simple.green"), Messages.get("combobox.example.simple.blue"), Messages.get("combobox.example.simple.yellow")));
        comboBox.setSelectedIndex(-1); // Start with nothing selected, as in the original Gallery (WComboBox selects the first item, as with JComboBox)
        comboBox.setWidth(200.0);
        comboBox.addListSelectionListener(() -> {
            String item = (String) comboBox.getSelectedItem();
            result.setText(item == null
                ? Messages.get("combobox.example.simple.none")
                : Messages.get("combobox.example.simple.selected", item, String.valueOf(comboBox.getSelectedIndex())));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(comboBox);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("combobox.example.simple.title"), row);
    }

    /** Heading and placeholder: header / placeholderText. */
    private static WComponent buildHeaderComboBoxExample() {
        WComboBox comboBox = new WComboBox(Arrays.asList(Messages.get("combobox.example.header.font1"), Messages.get("combobox.example.header.font2"), Messages.get("combobox.example.header.font3")));
        comboBox.setSelectedIndex(-1); // Show the placeholder text (WComboBox selects the first item, as with JComboBox)
        comboBox.setWidth(200.0);
        comboBox.setHeader(Messages.get("combobox.example.header.header"));
        comboBox.setPlaceholderText(Messages.get("combobox.example.header.placeholder"));
        return GalleryScaffold.buildExample(Messages.get("combobox.example.header.title"), comboBox);
    }

    /** An editable combo box: using isEditable and TextSubmitted to add values not in the list. */
    private static WComponent buildEditableComboBoxExample() {
        WLabel result = new WLabel(Messages.get("combobox.example.editable.none"));

        WComboBox comboBox = new WComboBox(Arrays.asList("10", "20", "30"));
        comboBox.setSelectedIndex(-1); // Start with nothing selected, as in the original Gallery (WComboBox selects the first item, as with JComboBox)
        comboBox.setWidth(200.0);
        comboBox.setEditable(true);
        comboBox.addTextSubmitListener((text) -> {
            result.setText(Messages.get("combobox.example.editable.submitted", text));
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
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("combobox.example.editable.title"), row);
    }

    // endregion

    // region DropDownButton

    /** The DropDownButton page: lines up demos for trying out WDropDownButton's various features. */
    static WComponent buildDropDownButtonPage() {
        WPanel page = GalleryScaffold.buildPage("DropDownButton", Messages.get("dropdownbutton.description"));

        page.add(buildSimpleDropDownButtonExample());
        return page;
    }

    /** A basic drop-down button: choosing from a flyout menu. */
    private static WComponent buildSimpleDropDownButtonExample() {
        WLabel result = new WLabel(Messages.get("dropdownbutton.example.simple.none"));

        WPanel menu = new WPanel(4.0);
        WFlyout flyout = new WFlyout(menu);
        for (String name : Arrays.asList(Messages.get("dropdownbutton.example.simple.email"), Messages.get("dropdownbutton.example.simple.calendar"), Messages.get("dropdownbutton.example.simple.contacts"))) {
            WButton button = new WButton(name);
            button.setWidth(120.0);
            button.addActionListener(() -> {
                result.setText(Messages.get("dropdownbutton.example.simple.selected", name));
                flyout.hide();
            });
            menu.add(button);
        }

        WDropDownButton dropDownButton = new WDropDownButton(Messages.get("dropdownbutton.example.simple.new"));
        dropDownButton.setFlyout(flyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(dropDownButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("dropdownbutton.example.simple.title"), row);
    }

    // endregion

    // region HyperlinkButton

    /** The HyperlinkButton page: lines up demos for trying out WHyperlinkButton's various features. */
    static WComponent buildHyperlinkButtonPage() {
        WPanel page = GalleryScaffold.buildPage("HyperlinkButton", Messages.get("hyperlinkbutton.description"));

        page.add(buildNavigateUriHyperlinkExample());
        page.add(buildClickHyperlinkExample());
        return page;
    }

    /** NavigateUri: clicking opens the default browser. */
    private static WComponent buildNavigateUriHyperlinkExample() {
        WHyperlinkButton hyperlinkButton = new WHyperlinkButton(
            Messages.get("hyperlinkbutton.example.navigate.text"),
            "https://learn.microsoft.com/windows/apps/winui/winui3/");
        return GalleryScaffold.buildExample(Messages.get("hyperlinkbutton.example.navigate.title"), hyperlinkButton);
    }

    /** Handling Click: respond to clicks in code without setting NavigateUri. */
    private static WComponent buildClickHyperlinkExample() {
        WLabel result = new WLabel(Messages.get("hyperlinkbutton.example.click.clicks", "0"));
        int[] count = {0};

        WHyperlinkButton hyperlinkButton = new WHyperlinkButton(Messages.get("hyperlinkbutton.example.click.text"), "");
        hyperlinkButton.addActionListener(() -> {
            count[0]++;
            result.setText(Messages.get("hyperlinkbutton.example.click.clicks", String.valueOf(count[0])));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(hyperlinkButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("hyperlinkbutton.example.click.title"), row);
    }

    // endregion

    // region RadioButton

    /** The RadioButton page: lines up demos for trying out WRadioButton's various features. */
    static WComponent buildRadioButtonPage() {
        WPanel page = GalleryScaffold.buildPage("RadioButton", Messages.get("radiobutton.description"));

        page.add(buildSimpleRadioButtonExample());
        page.add(buildRadioButtonGroupExample());
        return page;
    }

    /** A basic radio button: mutually exclusive selection within the same group. */
    private static WComponent buildSimpleRadioButtonExample() {
        WLabel result = new WLabel(Messages.get("radiobutton.example.simple.none"));

        WPanel body = new WPanel(4.0);
        for (String name : Arrays.asList(Messages.get("radiobutton.example.simple.small"), Messages.get("radiobutton.example.simple.medium"), Messages.get("radiobutton.example.simple.large"))) {
            WRadioButton radioButton = new WRadioButton(name);
            radioButton.setGroupName("size");
            radioButton.addItemListener(e -> {
                if (e.getStateChange() == WItemEvent.SELECTED) {
                    result.setText(Messages.get("radiobutton.example.simple.selected", name));
                }
            });
            body.add(radioButton);
        }
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("radiobutton.example.simple.title"), body);
    }

    /** Multiple groups: separate groupName values let groups be selected independently. */
    private static WComponent buildRadioButtonGroupExample() {
        WLabel result = new WLabel(Messages.get("radiobutton.example.groups.result", Messages.get("radiobutton.example.groups.unselected"), Messages.get("radiobutton.example.groups.unselected")));
        String[] background = {Messages.get("radiobutton.example.groups.unselected")};
        String[] foreground = {Messages.get("radiobutton.example.groups.unselected")};

        WPanel row = new WPanel(32.0, Orientation.HORIZONTAL);
        row.add(buildRadioButtonGroup(Messages.get("radiobutton.example.groups.background"), "background", (name) -> {
            background[0] = name;
            result.setText(Messages.get("radiobutton.example.groups.result", background[0], foreground[0]));
        }));
        row.add(buildRadioButtonGroup(Messages.get("radiobutton.example.groups.foreground"), "foreground", (name) -> {
            foreground[0] = name;
            result.setText(Messages.get("radiobutton.example.groups.result", background[0], foreground[0]));
        }));

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("radiobutton.example.groups.title"), body);
    }

    /** One group of radio buttons (a local function named buildGroup in the Kotlin version). */
    private static WComponent buildRadioButtonGroup(String title, String group, Consumer<String> onSelect) {
        WPanel panel = new WPanel(4.0);
        panel.add(new WLabel(title));
        for (String name : Arrays.asList(Messages.get("radiobutton.example.groups.white"), Messages.get("radiobutton.example.groups.black"), Messages.get("radiobutton.example.groups.blue"))) {
            WRadioButton radioButton = new WRadioButton(name);
            radioButton.setGroupName(group);
            radioButton.addItemListener(e -> {
                if (e.getStateChange() == WItemEvent.SELECTED) {
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
        WPanel page = GalleryScaffold.buildPage("RatingControl", Messages.get("ratingcontrol.description"));

        page.add(buildSimpleRatingExample());
        page.add(buildPlaceholderRatingExample());
        page.add(buildReadOnlyRatingExample());
        return page;
    }

    /** A basic rating: responding to value changes (ValueChanged) and clearing it. */
    private static WComponent buildSimpleRatingExample() {
        WLabel result = new WLabel(Messages.get("ratingcontrol.example.simple.notset"));

        WRatingControl rating = new WRatingControl();
        rating.setClearEnabled(true);
        rating.addChangeListener(e -> {
            double value = rating.getValue();
            result.setText(value < 0 ? Messages.get("ratingcontrol.example.simple.notset") : Messages.get("ratingcontrol.example.simple.rating", String.valueOf((int) value)));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(rating);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("ratingcontrol.example.simple.title"), row);
    }

    /** A placeholder: faintly shows something like an average value before the user rates it. */
    private static WComponent buildPlaceholderRatingExample() {
        WRatingControl rating = new WRatingControl();
        rating.setPlaceholderValue(3.5);
        rating.setCaption(Messages.get("ratingcontrol.example.placeholder.caption"));
        return GalleryScaffold.buildExample(Messages.get("ratingcontrol.example.placeholder.title"), rating);
    }

    /** Read-only and star count: isReadOnly / maxRating. */
    private static WComponent buildReadOnlyRatingExample() {
        WRatingControl rating = new WRatingControl();
        rating.setMaxRating(10);
        rating.setValue(7.0);
        rating.setReadOnly(true);

        WButton toggleButton = new WButton(Messages.get("ratingcontrol.example.readonly.unset"));
        toggleButton.addActionListener(() -> {
            rating.setReadOnly(!rating.isReadOnly());
            toggleButton.setText(rating.isReadOnly() ? Messages.get("ratingcontrol.example.readonly.unset") : Messages.get("ratingcontrol.example.readonly.set"));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(rating);
        row.add(toggleButton);
        return GalleryScaffold.buildExample(Messages.get("ratingcontrol.example.readonly.title"), row);
    }

    // endregion

    // region RepeatButton

    /** The RepeatButton page: lines up demos for trying out WRepeatButton's various features. */
    static WComponent buildRepeatButtonPage() {
        WPanel page = GalleryScaffold.buildPage("RepeatButton", Messages.get("repeatbutton.description"));

        page.add(buildSimpleRepeatButtonExample());
        page.add(buildRepeatButtonSpeedExample());
        return page;
    }

    /** A basic repeat button: the counter keeps increasing while held down. */
    private static WComponent buildSimpleRepeatButtonExample() {
        WLabel result = new WLabel(Messages.get("repeatbutton.clicks", "0"));
        int[] count = {0};

        WRepeatButton repeatButton = new WRepeatButton(Messages.get("repeatbutton.example.simple.text"));
        repeatButton.addActionListener(() -> {
            count[0]++;
            result.setText(Messages.get("repeatbutton.clicks", String.valueOf(count[0])));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(repeatButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("repeatbutton.example.simple.title"), row);
    }

    /** Repeat speed: the difference between delay (wait before the first fire) and interval (time between fires). */
    private static WComponent buildRepeatButtonSpeedExample() {
        WLabel result = new WLabel(Messages.get("repeatbutton.clicks", "0"));
        int[] count = {0};

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        String[] labels = {Messages.get("repeatbutton.example.speed.slow"), Messages.get("repeatbutton.example.speed.fast")};
        int[] delays = {500, 250};
        int[] intervals = {500, 50};
        for (int i = 0; i < labels.length; i++) {
            WRepeatButton repeatButton = new WRepeatButton(labels[i]);
            repeatButton.setDelay(delays[i]);
            repeatButton.setInterval(intervals[i]);
            repeatButton.addActionListener(() -> {
                count[0]++;
                result.setText(Messages.get("repeatbutton.clicks", String.valueOf(count[0])));
            });
            row.add(repeatButton);
        }
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("repeatbutton.example.speed.title"), row);
    }

    // endregion

    // region Slider

    /** The Slider page: lines up demos for trying out WSlider's various features. */
    static WComponent buildSliderPage() {
        WPanel page = GalleryScaffold.buildPage("Slider", Messages.get("slider.description"));

        page.add(buildSimpleSliderExample());
        page.add(buildRangeSliderExample());
        page.add(buildTickSliderExample());
        page.add(buildVerticalSliderExample());
        return page;
    }

    /** A basic slider: responding to value changes (ValueChanged). */
    private static WComponent buildSimpleSliderExample() {
        WLabel result = new WLabel(Messages.get("slider.example.simple.value", "0"));

        WSlider slider = new WSlider(0.0, 100.0, 0.0);
        slider.setWidth(300.0);
        slider.addChangeListener(e -> {
            double value = slider.getValue();
            result.setText(Messages.get("slider.example.simple.value", String.valueOf((int) value)));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(slider);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("slider.example.simple.title"), row);
    }

    /** Range and step: minimum / maximum / stepFrequency / header. */
    private static WComponent buildRangeSliderExample() {
        WSlider slider = new WSlider(500.0, 1000.0, 800.0);
        slider.setWidth(300.0);
        slider.setStepFrequency(10.0);
        slider.setHeader(Messages.get("slider.example.range.header"));
        return GalleryScaffold.buildExample(Messages.get("slider.example.range.title"), slider);
    }

    /** Tick marks: tickFrequency / tickPlacement and snapsTo. */
    private static WComponent buildTickSliderExample() {
        WSlider slider = new WSlider(0.0, 50.0, 0.0);
        slider.setWidth(300.0);
        slider.setTickFrequency(10.0);
        slider.setTickPlacement(TickPlacement.OUTSIDE);
        slider.setSnapsTo(SliderSnapsTo.TICKS);
        return GalleryScaffold.buildExample(Messages.get("slider.example.tick.title"), slider);
    }

    /** Vertical: orientation and isDirectionReversed. */
    private static WComponent buildVerticalSliderExample() {
        WSlider slider = new WSlider(0.0, 100.0, 30.0);
        slider.setHeight(160.0);
        slider.setOrientation(Orientation.VERTICAL);
        slider.setDirectionReversed(true);
        return GalleryScaffold.buildExample(Messages.get("slider.example.vertical.title"), slider);
    }

    // endregion

    // region SplitButton

    /** The SplitButton page: lines up demos for trying out WSplitButton's various features. */
    static WComponent buildSplitButtonPage() {
        WPanel page = GalleryScaffold.buildPage("SplitButton", Messages.get("splitbutton.description"));

        page.add(buildSimpleSplitButtonExample());
        return page;
    }

    /** A basic split button: clicking the body applies the current color, the arrow picks a color. */
    private static WComponent buildSimpleSplitButtonExample() {
        WBorder tile = GalleryScaffold.buildTile(WColor.LIGHT_GRAY, 48.0, 48.0);
        WColor[] currentColor = {GalleryTheme.DEMO_RED};

        WPanel menu = new WPanel(4.0);
        WFlyout flyout = new WFlyout(menu);
        WSplitButton splitButton = new WSplitButton(Messages.get("splitbutton.example.simple.apply"));
        String[] names = {Messages.get("splitbutton.example.simple.red"), Messages.get("splitbutton.example.simple.green"), Messages.get("splitbutton.example.simple.blue")};
        WColor[] colors = {GalleryTheme.DEMO_RED, GalleryTheme.DEMO_GREEN, GalleryTheme.DEMO_BLUE};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            WColor color = colors[i];
            WButton button = new WButton(name);
            button.setWidth(100.0);
            button.addActionListener(() -> {
                currentColor[0] = color;
                tile.setBackground(color);
                splitButton.setText(Messages.get("splitbutton.example.simple.applywith", name));
                flyout.hide();
            });
            menu.add(button);
        }
        splitButton.setFlyout(flyout);
        splitButton.addActionListener(() -> {
            tile.setBackground(currentColor[0]);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        splitButton.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(splitButton);
        row.add(tile);
        return GalleryScaffold.buildExample(Messages.get("splitbutton.example.simple.title"), row);
    }

    // endregion

    // region ToggleButton

    /** The ToggleButton page: lines up demos for trying out WToggleButton's various features. */
    static WComponent buildToggleButtonPage() {
        WPanel page = GalleryScaffold.buildPage("ToggleButton", Messages.get("togglebutton.description"));

        page.add(buildSimpleToggleButtonExample());
        return page;
    }

    /** A basic toggle button: displaying isChecked and toggling it from code. */
    private static WComponent buildSimpleToggleButtonExample() {
        WLabel result = new WLabel(Messages.get("togglebutton.state.off"));

        WToggleButton toggleButton = new WToggleButton(Messages.get("togglebutton.example.simple.mute"));
        toggleButton.addItemListener(e -> {
            result.setText(e.getStateChange() == WItemEvent.SELECTED ? Messages.get("togglebutton.state.on") : Messages.get("togglebutton.state.off"));
        });

        WButton codeButton = new WButton(Messages.get("togglebutton.example.simple.fromcode"));
        codeButton.addActionListener(() -> {
            toggleButton.setSelected(!toggleButton.isSelected());
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(toggleButton);
        row.add(codeButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("togglebutton.example.simple.title"), row);
    }

    // endregion

    // region ToggleSplitButton

    /** The ToggleSplitButton page: lines up demos for trying out WToggleSplitButton's various features. */
    static WComponent buildToggleSplitButtonPage() {
        WPanel page = GalleryScaffold.buildPage("ToggleSplitButton", Messages.get("togglesplitbutton.description"));

        page.add(buildSimpleToggleSplitButtonExample());
        return page;
    }

    /** A basic toggle split button: toggling a bulleted list on/off and choosing its marker. */
    private static WComponent buildSimpleToggleSplitButtonExample() {
        WLabel result = new WLabel("");
        String[] marker = {Messages.get("togglesplitbutton.example.simple.bullet")};
        List<String> items = Arrays.asList(Messages.get("togglesplitbutton.example.simple.apple"), Messages.get("togglesplitbutton.example.simple.orange"), Messages.get("togglesplitbutton.example.simple.grape"));

        WToggleSplitButton toggleSplitButton = new WToggleSplitButton(Messages.get("togglesplitbutton.example.simple.bullets"));
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
        for (String name : Arrays.asList(Messages.get("togglesplitbutton.example.simple.bullet"), "-", "◆")) {
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
        toggleSplitButton.addItemListener(e -> {
            render.run();
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        toggleSplitButton.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(toggleSplitButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("togglesplitbutton.example.simple.title"), row);
    }

    // endregion

    // region ToggleSwitch

    /** The ToggleSwitch page: lines up demos for trying out WToggleSwitch's various features. */
    static WComponent buildToggleSwitchPage() {
        WPanel page = GalleryScaffold.buildPage("ToggleSwitch", Messages.get("toggleswitch.description"));

        page.add(buildSimpleToggleSwitchExample());
        page.add(buildCustomContentToggleSwitchExample());
        return page;
    }

    /** A basic toggle switch: responding to toggling (Toggled) and switching it from code. */
    private static WComponent buildSimpleToggleSwitchExample() {
        WLabel result = new WLabel(Messages.get("toggleswitch.state.off"));

        WToggleSwitch toggleSwitch = new WToggleSwitch("");
        toggleSwitch.addItemListener(e -> {
            result.setText(e.getStateChange() == WItemEvent.SELECTED ? Messages.get("toggleswitch.state.on") : Messages.get("toggleswitch.state.off"));
        });

        WButton codeButton = new WButton(Messages.get("toggleswitch.example.simple.fromcode"));
        codeButton.addActionListener(() -> {
            toggleSwitch.setOn(!toggleSwitch.isOn());
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(toggleSwitch);
        codeButton.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(codeButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("toggleswitch.example.simple.title"), row);
    }

    /** Customizing the displayed text: header / onContent / offContent. */
    private static WComponent buildCustomContentToggleSwitchExample() {
        WToggleSwitch toggleSwitch = new WToggleSwitch(Messages.get("toggleswitch.example.custom.header"));
        toggleSwitch.setOnContent(Messages.get("toggleswitch.example.custom.on"));
        toggleSwitch.setOffContent(Messages.get("toggleswitch.example.custom.off"));
        return GalleryScaffold.buildExample(Messages.get("toggleswitch.example.custom.title"), toggleSwitch);
    }

    // endregion
}
