package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.CommandBarDefaultLabelPosition;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.StandardUICommandKind;
import com.appkitbox.winui4k.SwipeMode;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.VirtualKey;
import com.appkitbox.winui4k.VirtualKeyModifier;
import com.appkitbox.winui4k.WAppBarButton;
import com.appkitbox.winui4k.WAppBarSeparator;
import com.appkitbox.winui4k.WAppBarToggleButton;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WCommandBar;
import com.appkitbox.winui4k.WCommandBarFlyout;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WDropDownButton;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WMenuBar;
import com.appkitbox.winui4k.WMenuBarItem;
import com.appkitbox.winui4k.WMenuFlyout;
import com.appkitbox.winui4k.WMenuFlyoutItem;
import com.appkitbox.winui4k.WMenuFlyoutSeparator;
import com.appkitbox.winui4k.WMenuFlyoutSubItem;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WRadioMenuFlyoutItem;
import com.appkitbox.winui4k.WStandardUICommand;
import com.appkitbox.winui4k.WSwipeControl;
import com.appkitbox.winui4k.WSwipeItem;
import com.appkitbox.winui4k.WSwipeItems;
import com.appkitbox.winui4k.WToggleMenuFlyoutItem;
import com.appkitbox.winui4k.WXamlUICommand;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

/*
 * Menus & toolbars category: demo pages for AppBarButton / AppBarSeparator / AppBarToggleButton / CommandBar / CommandBarFlyout / MenuBar / MenuFlyout / SwipeControl / StandardUICommand / XamlUICommand.
 */
final class MenusToolbarsPages {
    private MenusToolbarsPages() {
    }

    // region AppBarButton

    /** AppBarButton page: lines up demos exercising WAppBarButton. */
    static WComponent buildAppBarButtonPage() {
        WPanel page = GalleryScaffold.buildPage("AppBarButton", "A toolbar button with an icon and label stacked vertically. Try out WAppBarButton.");

        page.add(buildSimpleAppBarButtonExample());
        page.add(buildFlyoutAppBarButtonExample());
        return page;
    }

    /** A basic AppBarButton: a Symbol icon and a click subscription. */
    private static WComponent buildSimpleAppBarButtonExample() {
        WLabel result = new WLabel("Clicked: none");

        WAppBarButton likeButton = new WAppBarButton("Like", Symbol.LIKE);
        likeButton.addActionListener(() -> {
            result.setText("Click: Like");
        });

        WAppBarButton saveButton = new WAppBarButton("Save", Symbol.SAVE);
        saveButton.setKeyboardAcceleratorText("Ctrl+S"); // shown in the tooltip
        saveButton.addActionListener(() -> {
            result.setText("Click: Save");
        });

        WAppBarButton disabledButton = new WAppBarButton("Disabled", Symbol.CANCEL);
        disabledButton.setEnabled(false);

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(likeButton);
        row.add(saveButton);
        row.add(disabledButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple AppBarButton (Label / Icon / IsEnabled)", row);
    }

    /** An AppBarButton with a flyout: opens a menu on click. */
    private static WComponent buildFlyoutAppBarButtonExample() {
        WLabel result = new WLabel("Selected item: none");

        WMenuFlyout menuFlyout = new WMenuFlyout();
        for (String text : Arrays.asList("PNG format", "JPEG format", "SVG format")) {
            WMenuFlyoutItem item = new WMenuFlyoutItem(text);
            item.addActionListener(() -> {
                result.setText("Selected item: " + text);
            });
            menuFlyout.add(item);
        }

        WAppBarButton exportButton = new WAppBarButton("Export", Symbol.DOWNLOAD);
        exportButton.setFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(exportButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("An AppBarButton with a menu (Flyout)", row);
    }

    // endregion

    // region AppBarSeparator

    /** AppBarSeparator page: lines up a WAppBarSeparator demo. */
    static WComponent buildAppBarSeparatorPage() {
        WPanel page = GalleryScaffold.buildPage("AppBarSeparator", "A divider line that groups commands on a toolbar.");

        page.add(buildAppBarSeparatorExample());
        return page;
    }

    /** Dividing a row of AppBarButtons with AppBarSeparator. */
    private static WComponent buildAppBarSeparatorExample() {
        WPanel row = new WPanel(4.0, Orientation.HORIZONTAL);
        row.add(new WAppBarButton("Back", Symbol.BACK));
        row.add(new WAppBarButton("Forward", Symbol.FORWARD));
        row.add(new WAppBarSeparator());
        row.add(new WAppBarButton("Refresh", Symbol.REFRESH));
        row.add(new WAppBarSeparator());
        row.add(new WAppBarButton("Favorite", Symbol.FAVORITE));
        return GalleryScaffold.buildExample("Grouping commands", row);
    }

    // endregion

    // region AppBarToggleButton

    /** AppBarToggleButton page: lines up demos exercising WAppBarToggleButton. */
    static WComponent buildAppBarToggleButtonPage() {
        WPanel page = GalleryScaffold.buildPage("AppBarToggleButton", "A toolbar button with on/off state. Try out WAppBarToggleButton.");

        page.add(buildSimpleAppBarToggleButtonExample());
        return page;
    }

    /** A basic AppBarToggleButton: subscribing to and flipping the checked state. */
    private static WComponent buildSimpleAppBarToggleButtonExample() {
        WLabel result = new WLabel("Shuffle: off");

        WAppBarToggleButton shuffleButton = new WAppBarToggleButton("Shuffle", Symbol.SHUFFLE);
        shuffleButton.addItemListener((isChecked) -> {
            result.setText("Shuffle: " + (Boolean.TRUE.equals(isChecked) ? "on" : "off"));
        });

        WAppBarToggleButton boldButton = new WAppBarToggleButton("Bold", Symbol.BOLD);
        boldButton.setSelected(true);

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(shuffleButton);
        row.add(boldButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple AppBarToggleButton (IsChecked)", row);
    }

    // endregion

    // region CommandBar

    /** CommandBar page: lines up demos exercising WCommandBar. */
    static WComponent buildCommandBarPage() {
        WPanel page = GalleryScaffold.buildPage("CommandBar", "A toolbar of commands. Try out WCommandBar.");

        page.add(buildSimpleCommandBarExample());
        page.add(buildCommandBarLabelPositionExample());
        return page;
    }

    /** A basic command bar: Primary / Secondary commands and open/close. */
    private static WComponent buildSimpleCommandBarExample() {
        WLabel result = new WLabel("Clicked: none");

        BiFunction<String, Symbol, WAppBarButton> appBarButton = (label, icon) -> {
            WAppBarButton button = new WAppBarButton(label, icon);
            button.addActionListener(() -> {
                result.setText("Click: " + label);
            });
            return button;
        };

        WCommandBar commandBar = new WCommandBar();
        commandBar.addPrimaryCommand(appBarButton.apply("Add", Symbol.ADD));
        commandBar.addPrimaryCommand(appBarButton.apply("Edit", Symbol.EDIT));
        commandBar.addPrimaryCommand(new WAppBarSeparator());
        commandBar.addPrimaryCommand(appBarButton.apply("Share", Symbol.SHARE));
        // Secondary commands go into the overflow menu opened via […]
        commandBar.addSecondaryCommand(appBarButton.apply("Settings", Symbol.SETTING));
        commandBar.addSecondaryCommand(appBarButton.apply("Help", Symbol.HELP));

        WButton openButton = new WButton("Toggle IsOpen");
        openButton.addActionListener(() -> {
            commandBar.setOpen(!commandBar.isOpen());
        });

        WPanel body = new WPanel(8.0);
        body.add(commandBar);
        WPanel controls = new WPanel(16.0, Orientation.HORIZONTAL);
        controls.add(openButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        controls.add(result);
        body.add(controls);
        return GalleryScaffold.buildExample("A simple command bar (PrimaryCommands / SecondaryCommands / IsOpen)", body);
    }

    /** Changing the label position: showing labels to the right of icons via DefaultLabelPosition. */
    private static WComponent buildCommandBarLabelPositionExample() {
        WCommandBar commandBar = new WCommandBar();
        commandBar.setDefaultLabelPosition(CommandBarDefaultLabelPosition.RIGHT);
        commandBar.addPrimaryCommand(new WAppBarButton("Add", Symbol.ADD));
        commandBar.addPrimaryCommand(new WAppBarButton("Edit", Symbol.EDIT));
        commandBar.addPrimaryCommand(new WAppBarButton("Delete", Symbol.DELETE));
        return GalleryScaffold.buildExample("Showing labels to the right of icons (DefaultLabelPosition)", commandBar);
    }

    // endregion

    // region CommandBarFlyout

    /** CommandBarFlyout page: lines up demos exercising WCommandBarFlyout. */
    static WComponent buildCommandBarFlyoutPage() {
        WPanel page = GalleryScaffold.buildPage("CommandBarFlyout", "A context menu with a mini toolbar attached. Try out WCommandBarFlyout.");

        page.add(buildCommandBarFlyoutExample());
        return page;
    }

    /** Attaching a CommandBarFlyout to an image-like tile: opens via right-click or a button. */
    private static WComponent buildCommandBarFlyoutExample() {
        WLabel result = new WLabel("Clicked: none");

        BiFunction<String, Symbol, WAppBarButton> appBarButton = (label, icon) -> {
            WAppBarButton button = new WAppBarButton(label, icon);
            button.addActionListener(() -> {
                result.setText("Click: " + label);
            });
            return button;
        };

        WCommandBarFlyout flyout = new WCommandBarFlyout();
        flyout.addPrimaryCommand(appBarButton.apply("Share", Symbol.SHARE));
        flyout.addPrimaryCommand(appBarButton.apply("Save", Symbol.SAVE));
        flyout.addPrimaryCommand(appBarButton.apply("Delete", Symbol.DELETE));
        // Secondary commands go into the menu below the mini toolbar
        flyout.addSecondaryCommand(appBarButton.apply("Resize", Symbol.ZOOM));
        flyout.addSecondaryCommand(appBarButton.apply("Go", Symbol.MOVE_TO_FOLDER));

        WLabel targetLabel = new WLabel("Right-click here");
        targetLabel.setMargin(32.0);
        WBorder target = new WBorder(targetLabel);
        target.setBackground(new WColor(226, 246, 235, 255));
        target.setCornerRadius(8.0);
        target.setContextFlyout(flyout);

        WButton showButton = new WButton("Show flyout");
        showButton.addActionListener(() -> {
            flyout.showAt(target);
        });

        WPanel body = new WPanel(8.0);
        body.add(target);
        WPanel controls = new WPanel(16.0, Orientation.HORIZONTAL);
        controls.add(showButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        controls.add(result);
        body.add(controls);
        return GalleryScaffold.buildExample("A context menu with a mini toolbar attached (ContextFlyout / ShowAt)", body);
    }

    // endregion

    // region MenuBar

    /** MenuBar page: lines up demos exercising WMenuBar / WMenuBarItem. */
    static WComponent buildMenuBarPage() {
        WPanel page = GalleryScaffold.buildPage("MenuBar", "A menu bar that lines up top-level menus horizontally. Try out WMenuBar / WMenuBarItem.");

        page.add(buildSimpleMenuBarExample());
        page.add(buildRichMenuBarExample());
        return page;
    }

    /** A basic menu bar: File / Edit / Help menus, with item-click subscriptions. */
    private static WComponent buildSimpleMenuBarExample() {
        WLabel result = new WLabel("Selected item: none");

        Function<String, WMenuFlyoutItem> item = (text) -> {
            WMenuFlyoutItem menuItem = new WMenuFlyoutItem(text);
            menuItem.addActionListener(() -> {
                result.setText("Selected item: " + text);
            });
            return menuItem;
        };

        WMenuBarItem fileMenu = new WMenuBarItem("File");
        fileMenu.add(item.apply("New"));
        fileMenu.add(item.apply("Open"));
        fileMenu.add(item.apply("Save"));
        fileMenu.add(new WMenuFlyoutSeparator());
        fileMenu.add(item.apply("Exit"));

        WMenuBarItem editMenu = new WMenuBarItem("Edit");
        editMenu.add(item.apply("Undo"));
        editMenu.add(item.apply("Cut"));
        editMenu.add(item.apply("Copy"));
        editMenu.add(item.apply("Paste"));

        WMenuBarItem helpMenu = new WMenuBarItem("Help");
        helpMenu.add(item.apply("About"));

        WMenuBar menuBar = new WMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        WPanel body = new WPanel(8.0);
        body.add(menuBar);
        body.add(result);
        return GalleryScaffold.buildExample("A simple menu bar", body);
    }

    /** A richer menu bar: icons, shortcuts, submenus, toggle and radio items. */
    private static WComponent buildRichMenuBarExample() {
        WLabel result = new WLabel("Selected item: none");

        BiFunction<String, Symbol, WMenuFlyoutItem> item = (text, icon) -> {
            WMenuFlyoutItem menuItem = new WMenuFlyoutItem(text, icon);
            menuItem.addActionListener(() -> {
                result.setText("Selected item: " + text);
            });
            return menuItem;
        };

        // Icons plus shortcuts that actually fire (work even without opening the menu)
        WMenuFlyoutItem newItem = item.apply("New", Symbol.ADD);
        newItem.addKeyboardAccelerator(VirtualKey.N, VirtualKeyModifier.CONTROL);
        WMenuFlyoutItem openItem = item.apply("Open", Symbol.OPEN_FILE);
        openItem.addKeyboardAccelerator(VirtualKey.O, VirtualKeyModifier.CONTROL);
        WMenuFlyoutItem saveItem = item.apply("Save", Symbol.SAVE);
        saveItem.addKeyboardAccelerator(VirtualKey.S, VirtualKeyModifier.CONTROL);

        // A submenu (cascading)
        WMenuFlyoutSubItem shareSubMenu = new WMenuFlyoutSubItem("Share", Symbol.SHARE);
        shareSubMenu.add(item.apply("Send by email", Symbol.MAIL));
        shareSubMenu.add(item.apply("Copy link", Symbol.LINK));

        WMenuBarItem fileMenu = new WMenuBarItem("File");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(new WMenuFlyoutSeparator());
        fileMenu.add(shareSubMenu);

        // A toggle item and radio items (mutually exclusive selection)
        WToggleMenuFlyoutItem statusBarItem = new WToggleMenuFlyoutItem("Show status bar");
        statusBarItem.setChecked(true);
        statusBarItem.addActionListener(() -> {
            result.setText("Show status bar: " + statusBarItem.isChecked());
        });

        Function<String, WRadioMenuFlyoutItem> radio = (text) -> {
            WRadioMenuFlyoutItem radioItem = new WRadioMenuFlyoutItem(text, "orientation");
            radioItem.addActionListener(() -> {
                result.setText("Orientation: " + text);
            });
            return radioItem;
        };

        WRadioMenuFlyoutItem landscapeItem = radio.apply("Landscape");
        landscapeItem.setChecked(true);

        WMenuBarItem viewMenu = new WMenuBarItem("View");
        viewMenu.add(statusBarItem);
        viewMenu.add(new WMenuFlyoutSeparator());
        viewMenu.add(landscapeItem);
        viewMenu.add(radio.apply("Portrait"));

        WMenuBar menuBar = new WMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);

        WPanel body = new WPanel(8.0);
        body.add(menuBar);
        body.add(result);
        return GalleryScaffold.buildExample("Icons, shortcuts, submenus, and toggle / radio items", body);
    }

    // endregion

    // region MenuFlyout

    /** MenuFlyout page: lines up demos exercising WMenuFlyout. */
    static WComponent buildMenuFlyoutPage() {
        WPanel page = GalleryScaffold.buildPage("MenuFlyout", "A menu that temporarily shows a list of commands. Try out WMenuFlyout and its menu items.");

        page.add(buildSimpleMenuFlyoutExample());
        page.add(buildRadioMenuFlyoutExample());
        page.add(buildContextMenuFlyoutExample());
        return page;
    }

    /** A basic menu flyout: opened from a DropDownButton. */
    private static WComponent buildSimpleMenuFlyoutExample() {
        WLabel result = new WLabel("Selected item: none");

        WMenuFlyout menuFlyout = new WMenuFlyout();
        for (String text : Arrays.asList("Reset items", "Repeat", "Shuffle")) {
            WMenuFlyoutItem item = new WMenuFlyoutItem(text);
            item.addActionListener(() -> {
                result.setText("Selected item: " + text);
            });
            menuFlyout.add(item);
        }

        WDropDownButton button = new WDropDownButton("Options");
        button.setFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(button);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A simple menu flyout", row);
    }

    /** A menu flyout with radio items: mutually-exclusive sort criteria. */
    private static WComponent buildRadioMenuFlyoutExample() {
        WLabel result = new WLabel("Sort by: rating");

        WMenuFlyout menuFlyout = new WMenuFlyout();
        for (String text : Arrays.asList("Rating", "Name", "Date")) {
            WRadioMenuFlyoutItem item = new WRadioMenuFlyoutItem(text, "sort");
            item.setChecked(text.equals("Rating"));
            item.addActionListener(() -> {
                result.setText("Sort by: " + text);
            });
            menuFlyout.add(item);
        }

        WDropDownButton button = new WDropDownButton("Sort");
        button.setFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(button);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Mutually-exclusive selection with radio items (WRadioMenuFlyoutItem)", row);
    }

    /** A context menu: a MenuFlyout opened by right-click (UIElement.ContextFlyout). */
    private static WComponent buildContextMenuFlyoutExample() {
        WLabel result = new WLabel("Selected item: none");

        WMenuFlyout menuFlyout = new WMenuFlyout();
        WMenuFlyoutItem copyItem = new WMenuFlyoutItem("Copy", Symbol.COPY);
        copyItem.setKeyboardAcceleratorText("Ctrl+C"); // a display-only shortcut string
        copyItem.addActionListener(() -> {
            result.setText("Selected item: Copy");
        });
        menuFlyout.add(copyItem);
        WMenuFlyoutItem deleteItem = new WMenuFlyoutItem("Delete", Symbol.DELETE);
        deleteItem.addActionListener(() -> {
            result.setText("Selected item: Delete");
        });
        menuFlyout.add(deleteItem);

        WLabel targetLabel = new WLabel("Right-click here");
        targetLabel.setMargin(24.0);
        WBorder target = new WBorder(targetLabel);
        target.setBackground(new WColor(226, 235, 246, 255));
        target.setCornerRadius(8.0);
        target.setContextFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(target);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A context menu (ContextFlyout)", row);
    }

    // endregion

    // region SwipeControl

    /** SwipeControl page: lines up demos exercising WSwipeControl. */
    static WComponent buildSwipeControlPage() {
        WPanel page = GalleryScaffold.buildPage(
                "SwipeControl",
                "A container that reveals commands on a touch swipe. Swiping is touch/pen only; it doesn't open with a mouse.");

        page.add(buildSwipeControlExample());
        return page;
    }

    /** Attaching Reveal / Execute swipe items to list-row-like content. */
    private static WComponent buildSwipeControlExample() {
        WLabel result = new WLabel("Item run: none");

        // Swipe right (from the left edge) -> reveals a pin button (Reveal)
        WSwipeItem pinItem = new WSwipeItem("Pin", Symbol.PIN);
        pinItem.setBackground(new WColor(96, 165, 250, 255));
        pinItem.addActionListener(() -> {
            result.setText("Item run: Pin");
        });
        WSwipeItems leftItems = new WSwipeItems(SwipeMode.REVEAL);
        leftItems.add(pinItem);

        // Swipe left (from the right edge) -> deletes immediately once fully swiped (Execute)
        WSwipeItem deleteItem = new WSwipeItem("Delete", Symbol.DELETE);
        deleteItem.setBackground(new WColor(239, 68, 68, 255));
        deleteItem.addActionListener(() -> {
            result.setText("Item run: Delete");
        });
        WSwipeItems rightItems = new WSwipeItems(SwipeMode.EXECUTE);
        rightItems.add(deleteItem);

        WLabel rowContentLabel = new WLabel("Touch-swipe this row left or right");
        rowContentLabel.setMargin(16.0);
        WBorder rowContent = new WBorder(rowContentLabel);
        rowContent.setBackground(GalleryTheme.CARD_BACKGROUND());
        rowContent.setBorderColor(GalleryTheme.CARD_BORDER());
        rowContent.setBorderThickness(1.0);

        WSwipeControl swipeControl = new WSwipeControl(rowContent);
        swipeControl.setWidth(320.0);
        swipeControl.setLeftItems(leftItems);
        swipeControl.setRightItems(rightItems);

        WPanel body = new WPanel(8.0);
        body.add(swipeControl);
        body.add(result);
        return GalleryScaffold.buildExample("Swipe items (LeftItems: Reveal / RightItems: Execute)", body);
    }

    // endregion

    // region StandardUICommand

    /** StandardUICommand page: lines up demos exercising WStandardUICommand. */
    static WComponent buildStandardUICommandPage() {
        WPanel page = GalleryScaffold.buildPage(
                "StandardUICommand",
                "A predefined command with an OS-standard label, icon, and shortcut already set. Try out WStandardUICommand.");

        page.add(buildStandardUICommandExample());
        return page;
    }

    /** A list of predefined commands: each Kind's label and icon apply automatically. */
    private static WComponent buildStandardUICommandExample() {
        WLabel result = new WLabel("Command run: none");

        WPanel row = new WPanel(4.0, Orientation.HORIZONTAL);
        for (StandardUICommandKind kind : Arrays.asList(
                StandardUICommandKind.CUT,
                StandardUICommandKind.COPY,
                StandardUICommandKind.PASTE,
                StandardUICommandKind.DELETE,
                StandardUICommandKind.UNDO,
                StandardUICommandKind.REDO)) {
            WStandardUICommand command = new WStandardUICommand(kind);
            command.addExecuteListener((parameter) -> {
                result.setText("Command run: " + command.getLabel());
            });
            // The label, icon, and shortcut (e.g. Ctrl+C) apply automatically from the command
            WAppBarButton button = new WAppBarButton();
            button.setCommand(command);
            row.add(button);
        }

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample("A toolbar of predefined commands (Kind / ExecuteRequested)", body);
    }

    // endregion

    // region XamlUICommand

    /** XamlUICommand page: lines up demos exercising WXamlUICommand. */
    static WComponent buildXamlUICommandPage() {
        WPanel page = GalleryScaffold.buildPage(
                "XamlUICommand",
                "A reusable command that bundles a label, icon, and shortcut together. Try out WXamlUICommand.");

        page.add(buildXamlUICommandExample());
        return page;
    }

    /** A custom command: sharing the same look and behavior across multiple controls. */
    private static WComponent buildXamlUICommandExample() {
        WLabel result = new WLabel("Run count: 0");
        int[] count = {0};

        WXamlUICommand command = new WXamlUICommand("Add to favorites");
        command.setIcon(Symbol.FAVORITE);
        command.setDescription("Adds the selected item to your favorites");
        command.addKeyboardAccelerator(VirtualKey.F, VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT);
        command.addExecuteListener((parameter) -> {
            count[0]++;
            result.setText("Run count: " + count[0]);
        });

        // Set the same command on a Button and an AppBarButton (label and icon apply automatically)
        WButton button = new WButton("");
        button.setCommand(command);
        WAppBarButton appBarButton = new WAppBarButton();
        appBarButton.setCommand(command);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        button.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(button);
        row.add(appBarButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);

        WPanel body = new WPanel(8.0);
        body.add(row);
        WLabel shortcutLabel = new WLabel("The Ctrl+Shift+F shortcut also runs it");
        shortcutLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
        body.add(shortcutLabel);
        return GalleryScaffold.buildExample("Sharing a command (Label / IconSource / KeyboardAccelerator / ExecuteRequested)", body);
    }

    // endregion
}
