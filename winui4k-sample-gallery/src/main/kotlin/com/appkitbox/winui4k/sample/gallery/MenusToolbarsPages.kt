package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.CommandBarDefaultLabelPosition
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.StandardUICommandKind
import com.appkitbox.winui4k.SwipeMode
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.VirtualKey
import com.appkitbox.winui4k.VirtualKeyModifier
import com.appkitbox.winui4k.WAppBarButton
import com.appkitbox.winui4k.WAppBarSeparator
import com.appkitbox.winui4k.WAppBarToggleButton
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WCommandBar
import com.appkitbox.winui4k.WCommandBarFlyout
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WDropDownButton
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WMenuBar
import com.appkitbox.winui4k.WMenuBarItem
import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItem
import com.appkitbox.winui4k.WMenuFlyoutSeparator
import com.appkitbox.winui4k.WMenuFlyoutSubItem
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WRadioMenuFlyoutItem
import com.appkitbox.winui4k.WStandardUICommand
import com.appkitbox.winui4k.WSwipeControl
import com.appkitbox.winui4k.WSwipeItem
import com.appkitbox.winui4k.WSwipeItems
import com.appkitbox.winui4k.WToggleMenuFlyoutItem
import com.appkitbox.winui4k.WXamlUICommand

/**
 * Menus & toolbars category: demo pages for AppBarButton / AppBarSeparator / AppBarToggleButton / CommandBar / CommandBarFlyout / MenuBar / MenuFlyout / SwipeControl / StandardUICommand / XamlUICommand.
 */

// region AppBarButton

/** AppBarButton page: lines up demos exercising WAppBarButton. */
internal fun buildAppBarButtonPage(): WComponent {
    val page = buildPage("AppBarButton", "A toolbar button with an icon and label stacked vertically. Try out WAppBarButton.")

    page.add(buildSimpleAppBarButtonExample())
    page.add(buildFlyoutAppBarButtonExample())
    return page
}

/** A basic AppBarButton: a Symbol icon and a click subscription. */
private fun buildSimpleAppBarButtonExample(): WComponent {
    val result = WLabel("Click: none")

    val likeButton = WAppBarButton("Like", Symbol.LIKE)
    likeButton.addActionListener { result.text = "Click: Like" }

    val saveButton = WAppBarButton("Save", Symbol.SAVE)
    saveButton.keyboardAcceleratorText = "Ctrl+S" // shown in the tooltip
    saveButton.addActionListener { result.text = "Click: Save" }

    val disabledButton = WAppBarButton("Disabled", Symbol.CANCEL)
    disabledButton.isEnabled = false

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(likeButton)
    row.add(saveButton)
    row.add(disabledButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple AppBarButton (Label / Icon / IsEnabled)", row)
}

/** An AppBarButton with a flyout: opens a menu on click. */
private fun buildFlyoutAppBarButtonExample(): WComponent {
    val result = WLabel("Selected item: none")

    val menuFlyout = WMenuFlyout()
    for (text in listOf("PNG format", "JPEG format", "SVG format")) {
        menuFlyout.add(
            WMenuFlyoutItem(text).also { item ->
                item.addActionListener { result.text = "Selected item: $text" }
            },
        )
    }

    val exportButton = WAppBarButton("Export", Symbol.DOWNLOAD)
    exportButton.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(exportButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("An AppBarButton with a menu (Flyout)", row)
}

// endregion

// region AppBarSeparator

/** AppBarSeparator page: lines up a WAppBarSeparator demo. */
internal fun buildAppBarSeparatorPage(): WComponent {
    val page = buildPage("AppBarSeparator", "A divider line that groups commands on a toolbar.")

    page.add(buildAppBarSeparatorExample())
    return page
}

/** Dividing a row of AppBarButtons with AppBarSeparator. */
private fun buildAppBarSeparatorExample(): WComponent {
    val row = WPanel(spacing = 4.0, orientation = Orientation.HORIZONTAL)
    row.add(WAppBarButton("Back", Symbol.BACK))
    row.add(WAppBarButton("Forward", Symbol.FORWARD))
    row.add(WAppBarSeparator())
    row.add(WAppBarButton("Refresh", Symbol.REFRESH))
    row.add(WAppBarSeparator())
    row.add(WAppBarButton("Favorite", Symbol.FAVORITE))
    return buildExample("Grouping commands", row)
}

// endregion

// region AppBarToggleButton

/** AppBarToggleButton page: lines up demos exercising WAppBarToggleButton. */
internal fun buildAppBarToggleButtonPage(): WComponent {
    val page = buildPage("AppBarToggleButton", "A toolbar button with on/off state. Try out WAppBarToggleButton.")

    page.add(buildSimpleAppBarToggleButtonExample())
    return page
}

/** A basic AppBarToggleButton: subscribing to and flipping the checked state. */
private fun buildSimpleAppBarToggleButtonExample(): WComponent {
    val result = WLabel("Shuffle: off")

    val shuffleButton = WAppBarToggleButton("Shuffle", Symbol.SHUFFLE)
    shuffleButton.addItemListener { isChecked ->
        result.text = "Shuffle: ${if (isChecked == true) "on" else "off"}"
    }

    val boldButton = WAppBarToggleButton("Bold", Symbol.BOLD)
    boldButton.isChecked = true

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(shuffleButton)
    row.add(boldButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple AppBarToggleButton (IsChecked)", row)
}

// endregion

// region CommandBar

/** CommandBar page: lines up demos exercising WCommandBar. */
internal fun buildCommandBarPage(): WComponent {
    val page = buildPage("CommandBar", "A toolbar of commands. Try out WCommandBar.")

    page.add(buildSimpleCommandBarExample())
    page.add(buildCommandBarLabelPositionExample())
    return page
}

/** A basic command bar: Primary / Secondary commands and open/close. */
private fun buildSimpleCommandBarExample(): WComponent {
    val result = WLabel("Click: none")

    fun appBarButton(label: String, icon: Symbol) = WAppBarButton(label, icon).also { button ->
        button.addActionListener { result.text = "Click: $label" }
    }

    val commandBar = WCommandBar()
    commandBar.addPrimaryCommand(appBarButton("Add", Symbol.ADD))
    commandBar.addPrimaryCommand(appBarButton("Edit", Symbol.EDIT))
    commandBar.addPrimaryCommand(WAppBarSeparator())
    commandBar.addPrimaryCommand(appBarButton("Share", Symbol.SHARE))
    // Secondary commands go into the overflow menu opened via […]
    commandBar.addSecondaryCommand(appBarButton("Settings", Symbol.SETTING))
    commandBar.addSecondaryCommand(appBarButton("Help", Symbol.HELP))

    val openButton = WButton("Toggle IsOpen")
    openButton.addActionListener { commandBar.isOpen = !commandBar.isOpen }

    val body = WPanel(spacing = 8.0)
    body.add(commandBar)
    body.add(
        WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL).also { controls ->
            controls.add(openButton)
            controls.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
        },
    )
    return buildExample("A simple command bar (PrimaryCommands / SecondaryCommands / IsOpen)", body)
}

/** Changing the label position: showing labels to the right of icons via DefaultLabelPosition. */
private fun buildCommandBarLabelPositionExample(): WComponent {
    val commandBar = WCommandBar()
    commandBar.defaultLabelPosition = CommandBarDefaultLabelPosition.RIGHT
    commandBar.addPrimaryCommand(WAppBarButton("Add", Symbol.ADD))
    commandBar.addPrimaryCommand(WAppBarButton("Edit", Symbol.EDIT))
    commandBar.addPrimaryCommand(WAppBarButton("Delete", Symbol.DELETE))
    return buildExample("Showing labels to the right of icons (DefaultLabelPosition)", commandBar)
}

// endregion

// region CommandBarFlyout

/** CommandBarFlyout page: lines up demos exercising WCommandBarFlyout. */
internal fun buildCommandBarFlyoutPage(): WComponent {
    val page = buildPage("CommandBarFlyout", "A context menu with a mini toolbar attached. Try out WCommandBarFlyout.")

    page.add(buildCommandBarFlyoutExample())
    return page
}

/** Attaching a CommandBarFlyout to an image-like tile: opens via right-click or a button. */
private fun buildCommandBarFlyoutExample(): WComponent {
    val result = WLabel("Click: none")

    fun appBarButton(label: String, icon: Symbol) = WAppBarButton(label, icon).also { button ->
        button.addActionListener { result.text = "Click: $label" }
    }

    val flyout = WCommandBarFlyout()
    flyout.addPrimaryCommand(appBarButton("Share", Symbol.SHARE))
    flyout.addPrimaryCommand(appBarButton("Save", Symbol.SAVE))
    flyout.addPrimaryCommand(appBarButton("Delete", Symbol.DELETE))
    // Secondary commands go into the menu below the mini toolbar
    flyout.addSecondaryCommand(appBarButton("Resize", Symbol.ZOOM))
    flyout.addSecondaryCommand(appBarButton("Move", Symbol.MOVE_TO_FOLDER))

    val target = WBorder(
        WLabel("Right-click here").also { it.margin = 32.0 },
    )
    target.background = WColor(226, 246, 235)
    target.cornerRadius = 8.0
    target.contextFlyout = flyout

    val showButton = WButton("Show flyout")
    showButton.addActionListener { flyout.showAt(target) }

    val body = WPanel(spacing = 8.0)
    body.add(target)
    body.add(
        WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL).also { controls ->
            controls.add(showButton)
            controls.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
        },
    )
    return buildExample("A context menu with a mini toolbar attached (ContextFlyout / ShowAt)", body)
}

// endregion

// region MenuBar

/** MenuBar page: lines up demos exercising WMenuBar / WMenuBarItem. */
internal fun buildMenuBarPage(): WComponent {
    val page = buildPage("MenuBar", "A menu bar that lines up top-level menus horizontally. Try out WMenuBar / WMenuBarItem.")

    page.add(buildSimpleMenuBarExample())
    page.add(buildRichMenuBarExample())
    return page
}

/** A basic menu bar: File / Edit / Help menus, with item-click subscriptions. */
private fun buildSimpleMenuBarExample(): WComponent {
    val result = WLabel("Selected item: none")

    fun item(text: String) = WMenuFlyoutItem(text).also { item ->
        item.addActionListener { result.text = "Selected item: $text" }
    }

    val fileMenu = WMenuBarItem("File")
    fileMenu.add(item("New"))
    fileMenu.add(item("Open"))
    fileMenu.add(item("Save"))
    fileMenu.add(WMenuFlyoutSeparator())
    fileMenu.add(item("Exit"))

    val editMenu = WMenuBarItem("Edit")
    editMenu.add(item("Undo"))
    editMenu.add(item("Cut"))
    editMenu.add(item("Copy"))
    editMenu.add(item("Paste"))

    val helpMenu = WMenuBarItem("Help")
    helpMenu.add(item("About"))

    val menuBar = WMenuBar()
    menuBar.add(fileMenu)
    menuBar.add(editMenu)
    menuBar.add(helpMenu)

    val body = WPanel(spacing = 8.0)
    body.add(menuBar)
    body.add(result)
    return buildExample("A simple menu bar", body)
}

/** A richer menu bar: icons, shortcuts, submenus, toggle and radio items. */
private fun buildRichMenuBarExample(): WComponent {
    val result = WLabel("Selected item: none")

    fun item(text: String, icon: Symbol? = null) = WMenuFlyoutItem(text, icon).also { item ->
        item.addActionListener { result.text = "Selected item: $text" }
    }

    // Icons plus shortcuts that actually fire (work even without opening the menu)
    val newItem = item("New", Symbol.ADD)
    newItem.addKeyboardAccelerator(VirtualKey.N, VirtualKeyModifier.CONTROL)
    val openItem = item("Open", Symbol.OPEN_FILE)
    openItem.addKeyboardAccelerator(VirtualKey.O, VirtualKeyModifier.CONTROL)
    val saveItem = item("Save", Symbol.SAVE)
    saveItem.addKeyboardAccelerator(VirtualKey.S, VirtualKeyModifier.CONTROL)

    // A submenu (cascading)
    val shareSubMenu = WMenuFlyoutSubItem("Share", Symbol.SHARE)
    shareSubMenu.add(item("Send by email", Symbol.MAIL))
    shareSubMenu.add(item("Copy link", Symbol.LINK))

    val fileMenu = WMenuBarItem("File")
    fileMenu.add(newItem)
    fileMenu.add(openItem)
    fileMenu.add(saveItem)
    fileMenu.add(WMenuFlyoutSeparator())
    fileMenu.add(shareSubMenu)

    // A toggle item and radio items (mutually exclusive selection)
    val statusBarItem = WToggleMenuFlyoutItem("Show status bar")
    statusBarItem.isChecked = true
    statusBarItem.addActionListener {
        result.text = "Show status bar: ${statusBarItem.isChecked}"
    }

    fun radio(text: String) = WRadioMenuFlyoutItem(text, groupName = "orientation").also { item ->
        item.addActionListener { result.text = "Orientation: $text" }
    }

    val landscapeItem = radio("Landscape")
    landscapeItem.isChecked = true

    val viewMenu = WMenuBarItem("View")
    viewMenu.add(statusBarItem)
    viewMenu.add(WMenuFlyoutSeparator())
    viewMenu.add(landscapeItem)
    viewMenu.add(radio("Portrait"))

    val menuBar = WMenuBar()
    menuBar.add(fileMenu)
    menuBar.add(viewMenu)

    val body = WPanel(spacing = 8.0)
    body.add(menuBar)
    body.add(result)
    return buildExample("Icons, shortcuts, submenus, and toggle / radio items", body)
}

// endregion

// region MenuFlyout

/** MenuFlyout page: lines up demos exercising WMenuFlyout. */
internal fun buildMenuFlyoutPage(): WComponent {
    val page = buildPage("MenuFlyout", "A menu that temporarily shows a list of commands. Try out WMenuFlyout and its menu items.")

    page.add(buildSimpleMenuFlyoutExample())
    page.add(buildRadioMenuFlyoutExample())
    page.add(buildContextMenuFlyoutExample())
    return page
}

/** A basic menu flyout: opened from a DropDownButton. */
private fun buildSimpleMenuFlyoutExample(): WComponent {
    val result = WLabel("Selected item: none")

    val menuFlyout = WMenuFlyout()
    for (text in listOf("Reset items", "Repeat", "Shuffle")) {
        menuFlyout.add(
            WMenuFlyoutItem(text).also { item ->
                item.addActionListener { result.text = "Selected item: $text" }
            },
        )
    }

    val button = WDropDownButton("Options")
    button.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple menu flyout", row)
}

/** A menu flyout with radio items: mutually-exclusive sort criteria. */
private fun buildRadioMenuFlyoutExample(): WComponent {
    val result = WLabel("Sort by: rating")

    val menuFlyout = WMenuFlyout()
    for (text in listOf("Rating", "Name", "Date")) {
        val item = WRadioMenuFlyoutItem(text, groupName = "sort")
        item.isChecked = text == "Rating"
        item.addActionListener { result.text = "Sort by: $text" }
        menuFlyout.add(item)
    }

    val button = WDropDownButton("Sort")
    button.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Mutually-exclusive selection with radio items (WRadioMenuFlyoutItem)", row)
}

/** A context menu: a MenuFlyout opened by right-click (UIElement.ContextFlyout). */
private fun buildContextMenuFlyoutExample(): WComponent {
    val result = WLabel("Selected item: none")

    val menuFlyout = WMenuFlyout()
    val copyItem = WMenuFlyoutItem("Copy", Symbol.COPY)
    copyItem.keyboardAcceleratorText = "Ctrl+C" // a display-only shortcut string
    copyItem.addActionListener { result.text = "Selected item: Copy" }
    menuFlyout.add(copyItem)
    menuFlyout.add(
        WMenuFlyoutItem("Delete", Symbol.DELETE).also { item ->
            item.addActionListener { result.text = "Selected item: Delete" }
        },
    )

    val target = WBorder(
        WLabel("Right-click here").also { it.margin = 24.0 },
    )
    target.background = WColor(226, 235, 246)
    target.cornerRadius = 8.0
    target.contextFlyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(target)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A context menu (ContextFlyout)", row)
}

// endregion

// region SwipeControl

/** SwipeControl page: lines up demos exercising WSwipeControl. */
internal fun buildSwipeControlPage(): WComponent {
    val page = buildPage(
        "SwipeControl",
        "A container that reveals commands on a touch swipe. Swiping is touch/pen only; it doesn't open with a mouse.",
    )

    page.add(buildSwipeControlExample())
    return page
}

/** Attaching Reveal / Execute swipe items to list-row-like content. */
private fun buildSwipeControlExample(): WComponent {
    val result = WLabel("Item run: none")

    // Swipe right (from the left edge) -> reveals a pin button (Reveal)
    val pinItem = WSwipeItem("Pin", Symbol.PIN)
    pinItem.background = WColor(96, 165, 250)
    pinItem.addActionListener { result.text = "Item run: Pin" }
    val leftItems = WSwipeItems(SwipeMode.REVEAL)
    leftItems.add(pinItem)

    // Swipe left (from the right edge) -> deletes immediately once fully swiped (Execute)
    val deleteItem = WSwipeItem("Delete", Symbol.DELETE)
    deleteItem.background = WColor(239, 68, 68)
    deleteItem.addActionListener { result.text = "Item run: Delete" }
    val rightItems = WSwipeItems(SwipeMode.EXECUTE)
    rightItems.add(deleteItem)

    val rowContent = WBorder(
        WLabel("Touch-swipe this row left or right").also { it.margin = 16.0 },
    )
    rowContent.background = CARD_BACKGROUND
    rowContent.borderColor = CARD_BORDER
    rowContent.borderThickness = 1.0

    val swipeControl = WSwipeControl(rowContent)
    swipeControl.width = 320.0
    swipeControl.leftItems = leftItems
    swipeControl.rightItems = rightItems

    val body = WPanel(spacing = 8.0)
    body.add(swipeControl)
    body.add(result)
    return buildExample("Swipe items (LeftItems: Reveal / RightItems: Execute)", body)
}

// endregion

// region StandardUICommand

/** StandardUICommand page: lines up demos exercising WStandardUICommand. */
internal fun buildStandardUICommandPage(): WComponent {
    val page = buildPage(
        "StandardUICommand",
        "A predefined command with an OS-standard label, icon, and shortcut already set. Try out WStandardUICommand.",
    )

    page.add(buildStandardUICommandExample())
    return page
}

/** A list of predefined commands: each Kind's label and icon apply automatically. */
private fun buildStandardUICommandExample(): WComponent {
    val result = WLabel("Command run: none")

    val row = WPanel(spacing = 4.0, orientation = Orientation.HORIZONTAL)
    for (kind in listOf(
        StandardUICommandKind.CUT,
        StandardUICommandKind.COPY,
        StandardUICommandKind.PASTE,
        StandardUICommandKind.DELETE,
        StandardUICommandKind.UNDO,
        StandardUICommandKind.REDO,
    )) {
        val command = WStandardUICommand(kind)
        command.addExecuteListener { result.text = "Command run: ${command.label}" }
        // The label, icon, and shortcut (e.g. Ctrl+C) apply automatically from the command
        row.add(WAppBarButton().also { it.command = command })
    }

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("A toolbar of predefined commands (Kind / ExecuteRequested)", body)
}

// endregion

// region XamlUICommand

/** XamlUICommand page: lines up demos exercising WXamlUICommand. */
internal fun buildXamlUICommandPage(): WComponent {
    val page = buildPage(
        "XamlUICommand",
        "A reusable command that bundles a label, icon, and shortcut together. Try out WXamlUICommand.",
    )

    page.add(buildXamlUICommandExample())
    return page
}

/** A custom command: sharing the same look and behavior across multiple controls. */
private fun buildXamlUICommandExample(): WComponent {
    val result = WLabel("Run count: 0")
    var count = 0

    val command = WXamlUICommand("Add to favorites")
    command.icon = Symbol.FAVORITE
    command.description = "Adds the selected item to your favorites"
    command.addKeyboardAccelerator(VirtualKey.F, VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT)
    command.addExecuteListener {
        count++
        result.text = "Run count: $count"
    }

    // Set the same command on a Button and an AppBarButton (label and icon apply automatically)
    val button = WButton()
    button.command = command
    val appBarButton = WAppBarButton()
    appBarButton.command = command

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(appBarButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(WLabel("The Ctrl+Shift+F shortcut also runs it").also { it.foreground = TEXT_SECONDARY })
    return buildExample("Sharing a command (Label / IconSource / KeyboardAccelerator / ExecuteRequested)", body)
}

// endregion
