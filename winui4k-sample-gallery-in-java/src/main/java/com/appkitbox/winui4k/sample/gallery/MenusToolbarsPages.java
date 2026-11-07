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
import com.appkitbox.winui4k.WItemEvent;
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
        WPanel page = GalleryScaffold.buildPage("AppBarButton", Messages.get("appbarbutton.description"));

        page.add(buildSimpleAppBarButtonExample());
        page.add(buildFlyoutAppBarButtonExample());
        return page;
    }

    /** A basic AppBarButton: a Symbol icon and a click subscription. */
    private static WComponent buildSimpleAppBarButtonExample() {
        WLabel result = new WLabel(Messages.get("appbarbutton.result.click.none"));

        WAppBarButton likeButton = new WAppBarButton(Messages.get("appbarbutton.like"), Symbol.LIKE);
        likeButton.addActionListener(() -> {
            result.setText(Messages.get("appbarbutton.result.click", Messages.get("appbarbutton.like")));
        });

        WAppBarButton saveButton = new WAppBarButton(Messages.get("appbarbutton.save"), Symbol.SAVE);
        saveButton.setKeyboardAcceleratorText("Ctrl+S"); // shown in the tooltip
        saveButton.addActionListener(() -> {
            result.setText(Messages.get("appbarbutton.result.click", Messages.get("appbarbutton.save")));
        });

        WAppBarButton disabledButton = new WAppBarButton(Messages.get("appbarbutton.disabled"), Symbol.CANCEL);
        disabledButton.setEnabled(false);

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(likeButton);
        row.add(saveButton);
        row.add(disabledButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("appbarbutton.example.simple.title"), row);
    }

    /** An AppBarButton with a flyout: opens a menu on click. */
    private static WComponent buildFlyoutAppBarButtonExample() {
        WLabel result = new WLabel(Messages.get("appbarbutton.result.selected.none"));

        WMenuFlyout menuFlyout = new WMenuFlyout();
        for (String text : Arrays.asList(Messages.get("appbarbutton.format.png"), Messages.get("appbarbutton.format.jpeg"), Messages.get("appbarbutton.format.svg"))) {
            WMenuFlyoutItem item = new WMenuFlyoutItem(text);
            item.addActionListener(() -> {
                result.setText(Messages.get("appbarbutton.result.selected", text));
            });
            menuFlyout.add(item);
        }

        WAppBarButton exportButton = new WAppBarButton(Messages.get("appbarbutton.export"), Symbol.DOWNLOAD);
        exportButton.setFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(exportButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("appbarbutton.example.flyout.title"), row);
    }

    // endregion

    // region AppBarSeparator

    /** AppBarSeparator page: lines up a WAppBarSeparator demo. */
    static WComponent buildAppBarSeparatorPage() {
        WPanel page = GalleryScaffold.buildPage("AppBarSeparator", Messages.get("appbarseparator.description"));

        page.add(buildAppBarSeparatorExample());
        return page;
    }

    /** Dividing a row of AppBarButtons with AppBarSeparator. */
    private static WComponent buildAppBarSeparatorExample() {
        WPanel row = new WPanel(4.0, Orientation.HORIZONTAL);
        row.add(new WAppBarButton(Messages.get("appbarseparator.back"), Symbol.BACK));
        row.add(new WAppBarButton(Messages.get("appbarseparator.forward"), Symbol.FORWARD));
        row.add(new WAppBarSeparator());
        row.add(new WAppBarButton(Messages.get("appbarseparator.refresh"), Symbol.REFRESH));
        row.add(new WAppBarSeparator());
        row.add(new WAppBarButton(Messages.get("appbarseparator.favorite"), Symbol.FAVORITE));
        return GalleryScaffold.buildExample(Messages.get("appbarseparator.example.title"), row);
    }

    // endregion

    // region AppBarToggleButton

    /** AppBarToggleButton page: lines up demos exercising WAppBarToggleButton. */
    static WComponent buildAppBarToggleButtonPage() {
        WPanel page = GalleryScaffold.buildPage("AppBarToggleButton", Messages.get("appbartogglebutton.description"));

        page.add(buildSimpleAppBarToggleButtonExample());
        return page;
    }

    /** A basic AppBarToggleButton: subscribing to and flipping the checked state. */
    private static WComponent buildSimpleAppBarToggleButtonExample() {
        WLabel result = new WLabel(Messages.get("appbartogglebutton.result.shuffle.off"));

        WAppBarToggleButton shuffleButton = new WAppBarToggleButton(Messages.get("appbartogglebutton.shuffle"), Symbol.SHUFFLE);
        shuffleButton.addItemListener(e -> {
            result.setText(e.getStateChange() == WItemEvent.SELECTED ? Messages.get("appbartogglebutton.result.shuffle.on") : Messages.get("appbartogglebutton.result.shuffle.off"));
        });

        WAppBarToggleButton boldButton = new WAppBarToggleButton(Messages.get("appbartogglebutton.bold"), Symbol.BOLD);
        boldButton.setSelected(true);

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(shuffleButton);
        row.add(boldButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("appbartogglebutton.example.simple.title"), row);
    }

    // endregion

    // region CommandBar

    /** CommandBar page: lines up demos exercising WCommandBar. */
    static WComponent buildCommandBarPage() {
        WPanel page = GalleryScaffold.buildPage("CommandBar", Messages.get("commandbar.description"));

        page.add(buildSimpleCommandBarExample());
        page.add(buildCommandBarLabelPositionExample());
        return page;
    }

    /** A basic command bar: Primary / Secondary commands and open/close. */
    private static WComponent buildSimpleCommandBarExample() {
        WLabel result = new WLabel(Messages.get("commandbar.result.click.none"));

        BiFunction<String, Symbol, WAppBarButton> appBarButton = (label, icon) -> {
            WAppBarButton button = new WAppBarButton(label, icon);
            button.addActionListener(() -> {
                result.setText(Messages.get("commandbar.result.click", label));
            });
            return button;
        };

        WCommandBar commandBar = new WCommandBar();
        commandBar.addPrimaryCommand(appBarButton.apply(Messages.get("commandbar.add"), Symbol.ADD));
        commandBar.addPrimaryCommand(appBarButton.apply(Messages.get("commandbar.edit"), Symbol.EDIT));
        commandBar.addPrimaryCommand(new WAppBarSeparator());
        commandBar.addPrimaryCommand(appBarButton.apply(Messages.get("commandbar.share"), Symbol.SHARE));
        // Secondary commands go into the overflow menu opened via […]
        commandBar.addSecondaryCommand(appBarButton.apply(Messages.get("commandbar.settings"), Symbol.SETTING));
        commandBar.addSecondaryCommand(appBarButton.apply(Messages.get("commandbar.help"), Symbol.HELP));

        WButton openButton = new WButton(Messages.get("commandbar.toggleopen"));
        openButton.addActionListener(() -> {
            commandBar.setOpen(!commandBar.isOpen());
        });

        WPanel body = new WPanel(8.0);
        body.add(commandBar);
        WPanel controls = new WPanel(16.0, Orientation.HORIZONTAL);
        controls.add(openButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        controls.add(result);
        body.add(controls);
        return GalleryScaffold.buildExample(Messages.get("commandbar.example.simple.title"), body);
    }

    /** Changing the label position: showing labels to the right of icons via DefaultLabelPosition. */
    private static WComponent buildCommandBarLabelPositionExample() {
        WCommandBar commandBar = new WCommandBar();
        commandBar.setDefaultLabelPosition(CommandBarDefaultLabelPosition.RIGHT);
        commandBar.addPrimaryCommand(new WAppBarButton(Messages.get("commandbar.add"), Symbol.ADD));
        commandBar.addPrimaryCommand(new WAppBarButton(Messages.get("commandbar.edit"), Symbol.EDIT));
        commandBar.addPrimaryCommand(new WAppBarButton(Messages.get("commandbar.delete"), Symbol.DELETE));
        return GalleryScaffold.buildExample(Messages.get("commandbar.example.labelposition.title"), commandBar);
    }

    // endregion

    // region CommandBarFlyout

    /** CommandBarFlyout page: lines up demos exercising WCommandBarFlyout. */
    static WComponent buildCommandBarFlyoutPage() {
        WPanel page = GalleryScaffold.buildPage("CommandBarFlyout", Messages.get("commandbarflyout.description"));

        page.add(buildCommandBarFlyoutExample());
        return page;
    }

    /** Attaching a CommandBarFlyout to an image-like tile: opens via right-click or a button. */
    private static WComponent buildCommandBarFlyoutExample() {
        WLabel result = new WLabel(Messages.get("commandbarflyout.result.click.none"));

        BiFunction<String, Symbol, WAppBarButton> appBarButton = (label, icon) -> {
            WAppBarButton button = new WAppBarButton(label, icon);
            button.addActionListener(() -> {
                result.setText(Messages.get("commandbarflyout.result.click", label));
            });
            return button;
        };

        WCommandBarFlyout flyout = new WCommandBarFlyout();
        flyout.addPrimaryCommand(appBarButton.apply(Messages.get("commandbarflyout.share"), Symbol.SHARE));
        flyout.addPrimaryCommand(appBarButton.apply(Messages.get("commandbarflyout.save"), Symbol.SAVE));
        flyout.addPrimaryCommand(appBarButton.apply(Messages.get("commandbarflyout.delete"), Symbol.DELETE));
        // Secondary commands go into the menu below the mini toolbar
        flyout.addSecondaryCommand(appBarButton.apply(Messages.get("commandbarflyout.resize"), Symbol.ZOOM));
        flyout.addSecondaryCommand(appBarButton.apply(Messages.get("commandbarflyout.move"), Symbol.MOVE_TO_FOLDER));

        WLabel targetLabel = new WLabel(Messages.get("commandbarflyout.target"));
        targetLabel.setMargin(32.0);
        WBorder target = new WBorder(targetLabel);
        target.setBackground(new WColor(226, 246, 235, 255));
        target.setCornerRadius(8.0);
        target.setContextFlyout(flyout);

        WButton showButton = new WButton(Messages.get("commandbarflyout.show"));
        showButton.addActionListener(() -> {
            flyout.showAt(target);
        });

        WPanel body = new WPanel(8.0);
        body.add(target);
        WPanel controls = new WPanel(16.0, Orientation.HORIZONTAL);
        controls.add(showButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        controls.add(result);
        body.add(controls);
        return GalleryScaffold.buildExample(Messages.get("commandbarflyout.example.title"), body);
    }

    // endregion

    // region MenuBar

    /** MenuBar page: lines up demos exercising WMenuBar / WMenuBarItem. */
    static WComponent buildMenuBarPage() {
        WPanel page = GalleryScaffold.buildPage("MenuBar", Messages.get("menubar.description"));

        page.add(buildSimpleMenuBarExample());
        page.add(buildRichMenuBarExample());
        return page;
    }

    /** A basic menu bar: File / Edit / Help menus, with item-click subscriptions. */
    private static WComponent buildSimpleMenuBarExample() {
        WLabel result = new WLabel(Messages.get("menubar.result.selected.none"));

        Function<String, WMenuFlyoutItem> item = (text) -> {
            WMenuFlyoutItem menuItem = new WMenuFlyoutItem(text);
            menuItem.addActionListener(() -> {
                result.setText(Messages.get("menubar.result.selected", text));
            });
            return menuItem;
        };

        WMenuBarItem fileMenu = new WMenuBarItem(Messages.get("menubar.file"));
        fileMenu.add(item.apply(Messages.get("menubar.new")));
        fileMenu.add(item.apply(Messages.get("menubar.open")));
        fileMenu.add(item.apply(Messages.get("menubar.save")));
        fileMenu.add(new WMenuFlyoutSeparator());
        fileMenu.add(item.apply(Messages.get("menubar.exit")));

        WMenuBarItem editMenu = new WMenuBarItem(Messages.get("menubar.edit"));
        editMenu.add(item.apply(Messages.get("menubar.undo")));
        editMenu.add(item.apply(Messages.get("menubar.cut")));
        editMenu.add(item.apply(Messages.get("menubar.copy")));
        editMenu.add(item.apply(Messages.get("menubar.paste")));

        WMenuBarItem helpMenu = new WMenuBarItem(Messages.get("menubar.help"));
        helpMenu.add(item.apply(Messages.get("menubar.about")));

        WMenuBar menuBar = new WMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        WPanel body = new WPanel(8.0);
        body.add(menuBar);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("menubar.example.simple.title"), body);
    }

    /** A richer menu bar: icons, shortcuts, submenus, toggle and radio items. */
    private static WComponent buildRichMenuBarExample() {
        WLabel result = new WLabel(Messages.get("menubar.result.selected.none"));

        BiFunction<String, Symbol, WMenuFlyoutItem> item = (text, icon) -> {
            WMenuFlyoutItem menuItem = new WMenuFlyoutItem(text, icon);
            menuItem.addActionListener(() -> {
                result.setText(Messages.get("menubar.result.selected", text));
            });
            return menuItem;
        };

        // Icons plus shortcuts that actually fire (work even without opening the menu)
        WMenuFlyoutItem newItem = item.apply(Messages.get("menubar.new"), Symbol.ADD);
        newItem.addKeyboardAccelerator(VirtualKey.N, VirtualKeyModifier.CONTROL);
        WMenuFlyoutItem openItem = item.apply(Messages.get("menubar.open"), Symbol.OPEN_FILE);
        openItem.addKeyboardAccelerator(VirtualKey.O, VirtualKeyModifier.CONTROL);
        WMenuFlyoutItem saveItem = item.apply(Messages.get("menubar.save"), Symbol.SAVE);
        saveItem.addKeyboardAccelerator(VirtualKey.S, VirtualKeyModifier.CONTROL);

        // A submenu (cascading)
        WMenuFlyoutSubItem shareSubMenu = new WMenuFlyoutSubItem(Messages.get("menubar.share"), Symbol.SHARE);
        shareSubMenu.add(item.apply(Messages.get("menubar.sendmail"), Symbol.MAIL));
        shareSubMenu.add(item.apply(Messages.get("menubar.copylink"), Symbol.LINK));

        WMenuBarItem fileMenu = new WMenuBarItem(Messages.get("menubar.file"));
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(new WMenuFlyoutSeparator());
        fileMenu.add(shareSubMenu);

        // A toggle item and radio items (mutually exclusive selection)
        WToggleMenuFlyoutItem statusBarItem = new WToggleMenuFlyoutItem(Messages.get("menubar.statusbar"));
        statusBarItem.setChecked(true);
        statusBarItem.addActionListener(() -> {
            result.setText(Messages.get("menubar.result.statusbar", String.valueOf(statusBarItem.isChecked())));
        });

        Function<String, WRadioMenuFlyoutItem> radio = (text) -> {
            WRadioMenuFlyoutItem radioItem = new WRadioMenuFlyoutItem(text, "orientation");
            radioItem.addActionListener(() -> {
                result.setText(Messages.get("menubar.result.orientation", text));
            });
            return radioItem;
        };

        WRadioMenuFlyoutItem landscapeItem = radio.apply(Messages.get("menubar.landscape"));
        landscapeItem.setChecked(true);

        WMenuBarItem viewMenu = new WMenuBarItem(Messages.get("menubar.view"));
        viewMenu.add(statusBarItem);
        viewMenu.add(new WMenuFlyoutSeparator());
        viewMenu.add(landscapeItem);
        viewMenu.add(radio.apply(Messages.get("menubar.portrait")));

        WMenuBar menuBar = new WMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);

        WPanel body = new WPanel(8.0);
        body.add(menuBar);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("menubar.example.rich.title"), body);
    }

    // endregion

    // region MenuFlyout

    /** MenuFlyout page: lines up demos exercising WMenuFlyout. */
    static WComponent buildMenuFlyoutPage() {
        WPanel page = GalleryScaffold.buildPage("MenuFlyout", Messages.get("menuflyout.description"));

        page.add(buildSimpleMenuFlyoutExample());
        page.add(buildRadioMenuFlyoutExample());
        page.add(buildContextMenuFlyoutExample());
        return page;
    }

    /** A basic menu flyout: opened from a DropDownButton. */
    private static WComponent buildSimpleMenuFlyoutExample() {
        WLabel result = new WLabel(Messages.get("menuflyout.result.selected.none"));

        WMenuFlyout menuFlyout = new WMenuFlyout();
        for (String text : Arrays.asList(Messages.get("menuflyout.reset"), Messages.get("menuflyout.repeat"), Messages.get("menuflyout.shuffle"))) {
            WMenuFlyoutItem item = new WMenuFlyoutItem(text);
            item.addActionListener(() -> {
                result.setText(Messages.get("menuflyout.result.selected", text));
            });
            menuFlyout.add(item);
        }

        WDropDownButton button = new WDropDownButton(Messages.get("menuflyout.options"));
        button.setFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(button);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("menuflyout.example.simple.title"), row);
    }

    /** A menu flyout with radio items: mutually-exclusive sort criteria. */
    private static WComponent buildRadioMenuFlyoutExample() {
        WLabel result = new WLabel(Messages.get("menuflyout.result.sort", Messages.get("menuflyout.sort.rating")));

        WMenuFlyout menuFlyout = new WMenuFlyout();
        for (String text : Arrays.asList(Messages.get("menuflyout.sort.rating"), Messages.get("menuflyout.sort.name"), Messages.get("menuflyout.sort.date"))) {
            WRadioMenuFlyoutItem item = new WRadioMenuFlyoutItem(text, "sort");
            item.setChecked(text.equals(Messages.get("menuflyout.sort.rating")));
            item.addActionListener(() -> {
                result.setText(Messages.get("menuflyout.result.sort", text));
            });
            menuFlyout.add(item);
        }

        WDropDownButton button = new WDropDownButton(Messages.get("menuflyout.sort"));
        button.setFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(button);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("menuflyout.example.radio.title"), row);
    }

    /** A context menu: a MenuFlyout opened by right-click (UIElement.ContextFlyout). */
    private static WComponent buildContextMenuFlyoutExample() {
        WLabel result = new WLabel(Messages.get("menuflyout.result.selected.none"));

        WMenuFlyout menuFlyout = new WMenuFlyout();
        WMenuFlyoutItem copyItem = new WMenuFlyoutItem(Messages.get("menuflyout.copy"), Symbol.COPY);
        copyItem.setKeyboardAcceleratorText("Ctrl+C"); // a display-only shortcut string
        copyItem.addActionListener(() -> {
            result.setText(Messages.get("menuflyout.result.selected", Messages.get("menuflyout.copy")));
        });
        menuFlyout.add(copyItem);
        WMenuFlyoutItem deleteItem = new WMenuFlyoutItem(Messages.get("menuflyout.delete"), Symbol.DELETE);
        deleteItem.addActionListener(() -> {
            result.setText(Messages.get("menuflyout.result.selected", Messages.get("menuflyout.delete")));
        });
        menuFlyout.add(deleteItem);

        WLabel targetLabel = new WLabel(Messages.get("menuflyout.target"));
        targetLabel.setMargin(24.0);
        WBorder target = new WBorder(targetLabel);
        target.setBackground(new WColor(226, 235, 246, 255));
        target.setCornerRadius(8.0);
        target.setContextFlyout(menuFlyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(target);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("menuflyout.example.context.title"), row);
    }

    // endregion

    // region SwipeControl

    /** SwipeControl page: lines up demos exercising WSwipeControl. */
    static WComponent buildSwipeControlPage() {
        WPanel page = GalleryScaffold.buildPage(
                "SwipeControl",
                Messages.get("swipecontrol.description"));

        page.add(buildSwipeControlExample());
        return page;
    }

    /** Attaching Reveal / Execute swipe items to list-row-like content. */
    private static WComponent buildSwipeControlExample() {
        WLabel result = new WLabel(Messages.get("swipecontrol.result.executed.none"));

        // Swipe right (from the left edge) -> reveals a pin button (Reveal)
        WSwipeItem pinItem = new WSwipeItem(Messages.get("swipecontrol.pin"), Symbol.PIN);
        pinItem.setBackground(new WColor(96, 165, 250, 255));
        pinItem.addActionListener(() -> {
            result.setText(Messages.get("swipecontrol.result.executed", Messages.get("swipecontrol.pin")));
        });
        WSwipeItems leftItems = new WSwipeItems(SwipeMode.REVEAL);
        leftItems.add(pinItem);

        // Swipe left (from the right edge) -> deletes immediately once fully swiped (Execute)
        WSwipeItem deleteItem = new WSwipeItem(Messages.get("swipecontrol.delete"), Symbol.DELETE);
        deleteItem.setBackground(new WColor(239, 68, 68, 255));
        deleteItem.addActionListener(() -> {
            result.setText(Messages.get("swipecontrol.result.executed", Messages.get("swipecontrol.delete")));
        });
        WSwipeItems rightItems = new WSwipeItems(SwipeMode.EXECUTE);
        rightItems.add(deleteItem);

        WLabel rowContentLabel = new WLabel(Messages.get("swipecontrol.row"));
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
        return GalleryScaffold.buildExample(Messages.get("swipecontrol.example.title"), body);
    }

    // endregion

    // region StandardUICommand

    /** StandardUICommand page: lines up demos exercising WStandardUICommand. */
    static WComponent buildStandardUICommandPage() {
        WPanel page = GalleryScaffold.buildPage(
                "StandardUICommand",
                Messages.get("standarduicommand.description"));

        page.add(buildStandardUICommandExample());
        return page;
    }

    /** A list of predefined commands: each Kind's label and icon apply automatically. */
    private static WComponent buildStandardUICommandExample() {
        WLabel result = new WLabel(Messages.get("standarduicommand.result.none"));

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
                result.setText(Messages.get("standarduicommand.result", command.getLabel()));
            });
            // The label, icon, and shortcut (e.g. Ctrl+C) apply automatically from the command
            WAppBarButton button = new WAppBarButton();
            button.setCommand(command);
            row.add(button);
        }

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("standarduicommand.example.title"), body);
    }

    // endregion

    // region XamlUICommand

    /** XamlUICommand page: lines up demos exercising WXamlUICommand. */
    static WComponent buildXamlUICommandPage() {
        WPanel page = GalleryScaffold.buildPage(
                "XamlUICommand",
                Messages.get("xamluicommand.description"));

        page.add(buildXamlUICommandExample());
        return page;
    }

    /** A custom command: sharing the same look and behavior across multiple controls. */
    private static WComponent buildXamlUICommandExample() {
        WLabel result = new WLabel(Messages.get("xamluicommand.result.count", "0"));
        int[] count = {0};

        WXamlUICommand command = new WXamlUICommand(Messages.get("xamluicommand.label"));
        command.setIcon(Symbol.FAVORITE);
        command.setDescription(Messages.get("xamluicommand.commanddescription"));
        command.addKeyboardAccelerator(VirtualKey.F, VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT);
        command.addExecuteListener((parameter) -> {
            count[0]++;
            result.setText(Messages.get("xamluicommand.result.count", String.valueOf(count[0])));
        });

        // Set the same command on a Button and an AppBarButton (label and icon apply automatically)
        WButton button = new WButton("");
        button.setCommand(command);
        WAppBarButton appBarButton = new WAppBarButton();
        appBarButton.setCommand(command);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        button.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(button);
        row.add(appBarButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);

        WPanel body = new WPanel(8.0);
        body.add(row);
        WLabel shortcutLabel = new WLabel(Messages.get("xamluicommand.shortcut"));
        shortcutLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
        body.add(shortcutLabel);
        return GalleryScaffold.buildExample(Messages.get("xamluicommand.example.title"), body);
    }

    // endregion
}
