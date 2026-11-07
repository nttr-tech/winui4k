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

/*
 * Menus & toolbars category: demo pages for AppBarButton / AppBarSeparator / AppBarToggleButton / CommandBar / CommandBarFlyout / MenuBar / MenuFlyout / SwipeControl / StandardUICommand / XamlUICommand.
 */

// region AppBarButton

/** AppBarButton page: lines up demos exercising WAppBarButton. */
internal fun buildAppBarButtonPage(): WComponent {
    val page = buildPage("AppBarButton", Messages.get("appbarbutton.description"))

    page.add(buildSimpleAppBarButtonExample())
    page.add(buildFlyoutAppBarButtonExample())
    return page
}

/** A basic AppBarButton: a Symbol icon and a click subscription. */
private fun buildSimpleAppBarButtonExample(): WComponent {
    val result = WLabel(Messages.get("appbarbutton.result.click.none"))

    val likeButton = WAppBarButton(Messages.get("appbarbutton.like"), Symbol.LIKE)
    likeButton.addActionListener { result.text = Messages.get("appbarbutton.result.click", Messages.get("appbarbutton.like")) }

    val saveButton = WAppBarButton(Messages.get("appbarbutton.save"), Symbol.SAVE)
    saveButton.keyboardAcceleratorText = "Ctrl+S" // shown in the tooltip
    saveButton.addActionListener { result.text = Messages.get("appbarbutton.result.click", Messages.get("appbarbutton.save")) }

    val disabledButton = WAppBarButton(Messages.get("appbarbutton.disabled"), Symbol.CANCEL)
    disabledButton.isEnabled = false

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(likeButton)
    row.add(saveButton)
    row.add(disabledButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("appbarbutton.example.simple.title"), row)
}

/** An AppBarButton with a flyout: opens a menu on click. */
private fun buildFlyoutAppBarButtonExample(): WComponent {
    val result = WLabel(Messages.get("appbarbutton.result.selected.none"))

    val menuFlyout = WMenuFlyout()
    for (text in listOf(Messages.get("appbarbutton.format.png"), Messages.get("appbarbutton.format.jpeg"), Messages.get("appbarbutton.format.svg"))) {
        menuFlyout.add(
            WMenuFlyoutItem(text).also { item ->
                item.addActionListener { result.text = Messages.get("appbarbutton.result.selected", text) }
            },
        )
    }

    val exportButton = WAppBarButton(Messages.get("appbarbutton.export"), Symbol.DOWNLOAD)
    exportButton.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(exportButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("appbarbutton.example.flyout.title"), row)
}

// endregion

// region AppBarSeparator

/** AppBarSeparator page: lines up a WAppBarSeparator demo. */
internal fun buildAppBarSeparatorPage(): WComponent {
    val page = buildPage("AppBarSeparator", Messages.get("appbarseparator.description"))

    page.add(buildAppBarSeparatorExample())
    return page
}

/** Dividing a row of AppBarButtons with AppBarSeparator. */
private fun buildAppBarSeparatorExample(): WComponent {
    val row = WPanel(spacing = 4.0, orientation = Orientation.HORIZONTAL)
    row.add(WAppBarButton(Messages.get("appbarseparator.back"), Symbol.BACK))
    row.add(WAppBarButton(Messages.get("appbarseparator.forward"), Symbol.FORWARD))
    row.add(WAppBarSeparator())
    row.add(WAppBarButton(Messages.get("appbarseparator.refresh"), Symbol.REFRESH))
    row.add(WAppBarSeparator())
    row.add(WAppBarButton(Messages.get("appbarseparator.favorite"), Symbol.FAVORITE))
    return buildExample(Messages.get("appbarseparator.example.title"), row)
}

// endregion

// region AppBarToggleButton

/** AppBarToggleButton page: lines up demos exercising WAppBarToggleButton. */
internal fun buildAppBarToggleButtonPage(): WComponent {
    val page = buildPage("AppBarToggleButton", Messages.get("appbartogglebutton.description"))

    page.add(buildSimpleAppBarToggleButtonExample())
    return page
}

/** A basic AppBarToggleButton: subscribing to and flipping the checked state. */
private fun buildSimpleAppBarToggleButtonExample(): WComponent {
    val result = WLabel(Messages.get("appbartogglebutton.result.shuffle.off"))

    val shuffleButton = WAppBarToggleButton(Messages.get("appbartogglebutton.shuffle"), Symbol.SHUFFLE)
    shuffleButton.addItemListener { isChecked ->
        result.text = if (isChecked == true) Messages.get("appbartogglebutton.result.shuffle.on") else Messages.get("appbartogglebutton.result.shuffle.off")
    }

    val boldButton = WAppBarToggleButton(Messages.get("appbartogglebutton.bold"), Symbol.BOLD)
    boldButton.isChecked = true

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(shuffleButton)
    row.add(boldButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("appbartogglebutton.example.simple.title"), row)
}

// endregion

// region CommandBar

/** CommandBar page: lines up demos exercising WCommandBar. */
internal fun buildCommandBarPage(): WComponent {
    val page = buildPage("CommandBar", Messages.get("commandbar.description"))

    page.add(buildSimpleCommandBarExample())
    page.add(buildCommandBarLabelPositionExample())
    return page
}

/** A basic command bar: Primary / Secondary commands and open/close. */
private fun buildSimpleCommandBarExample(): WComponent {
    val result = WLabel(Messages.get("commandbar.result.click.none"))

    fun appBarButton(label: String, icon: Symbol) = WAppBarButton(label, icon).also { button ->
        button.addActionListener { result.text = Messages.get("commandbar.result.click", label) }
    }

    val commandBar = WCommandBar()
    commandBar.addPrimaryCommand(appBarButton(Messages.get("commandbar.add"), Symbol.ADD))
    commandBar.addPrimaryCommand(appBarButton(Messages.get("commandbar.edit"), Symbol.EDIT))
    commandBar.addPrimaryCommand(WAppBarSeparator())
    commandBar.addPrimaryCommand(appBarButton(Messages.get("commandbar.share"), Symbol.SHARE))
    // Secondary commands go into the overflow menu opened via […]
    commandBar.addSecondaryCommand(appBarButton(Messages.get("commandbar.settings"), Symbol.SETTING))
    commandBar.addSecondaryCommand(appBarButton(Messages.get("commandbar.help"), Symbol.HELP))

    val openButton = WButton(Messages.get("commandbar.toggleopen"))
    openButton.addActionListener { commandBar.isOpen = !commandBar.isOpen }

    val body = WPanel(spacing = 8.0)
    body.add(commandBar)
    body.add(
        WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL).also { controls ->
            controls.add(openButton)
            controls.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
        },
    )
    return buildExample(Messages.get("commandbar.example.simple.title"), body)
}

/** Changing the label position: showing labels to the right of icons via DefaultLabelPosition. */
private fun buildCommandBarLabelPositionExample(): WComponent {
    val commandBar = WCommandBar()
    commandBar.defaultLabelPosition = CommandBarDefaultLabelPosition.RIGHT
    commandBar.addPrimaryCommand(WAppBarButton(Messages.get("commandbar.add"), Symbol.ADD))
    commandBar.addPrimaryCommand(WAppBarButton(Messages.get("commandbar.edit"), Symbol.EDIT))
    commandBar.addPrimaryCommand(WAppBarButton(Messages.get("commandbar.delete"), Symbol.DELETE))
    return buildExample(Messages.get("commandbar.example.labelposition.title"), commandBar)
}

// endregion

// region CommandBarFlyout

/** CommandBarFlyout page: lines up demos exercising WCommandBarFlyout. */
internal fun buildCommandBarFlyoutPage(): WComponent {
    val page = buildPage("CommandBarFlyout", Messages.get("commandbarflyout.description"))

    page.add(buildCommandBarFlyoutExample())
    return page
}

/** Attaching a CommandBarFlyout to an image-like tile: opens via right-click or a button. */
private fun buildCommandBarFlyoutExample(): WComponent {
    val result = WLabel(Messages.get("commandbarflyout.result.click.none"))

    fun appBarButton(label: String, icon: Symbol) = WAppBarButton(label, icon).also { button ->
        button.addActionListener { result.text = Messages.get("commandbarflyout.result.click", label) }
    }

    val flyout = WCommandBarFlyout()
    flyout.addPrimaryCommand(appBarButton(Messages.get("commandbarflyout.share"), Symbol.SHARE))
    flyout.addPrimaryCommand(appBarButton(Messages.get("commandbarflyout.save"), Symbol.SAVE))
    flyout.addPrimaryCommand(appBarButton(Messages.get("commandbarflyout.delete"), Symbol.DELETE))
    // Secondary commands go into the menu below the mini toolbar
    flyout.addSecondaryCommand(appBarButton(Messages.get("commandbarflyout.resize"), Symbol.ZOOM))
    flyout.addSecondaryCommand(appBarButton(Messages.get("commandbarflyout.move"), Symbol.MOVE_TO_FOLDER))

    val target = WBorder(
        WLabel(Messages.get("commandbarflyout.target")).also { it.margin = 32.0 },
    )
    target.background = WColor(226, 246, 235)
    target.cornerRadius = 8.0
    target.contextFlyout = flyout

    val showButton = WButton(Messages.get("commandbarflyout.show"))
    showButton.addActionListener { flyout.showAt(target) }

    val body = WPanel(spacing = 8.0)
    body.add(target)
    body.add(
        WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL).also { controls ->
            controls.add(showButton)
            controls.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
        },
    )
    return buildExample(Messages.get("commandbarflyout.example.title"), body)
}

// endregion

// region MenuBar

/** MenuBar page: lines up demos exercising WMenuBar / WMenuBarItem. */
internal fun buildMenuBarPage(): WComponent {
    val page = buildPage("MenuBar", Messages.get("menubar.description"))

    page.add(buildSimpleMenuBarExample())
    page.add(buildRichMenuBarExample())
    return page
}

/** A basic menu bar: File / Edit / Help menus, with item-click subscriptions. */
private fun buildSimpleMenuBarExample(): WComponent {
    val result = WLabel(Messages.get("menubar.result.selected.none"))

    fun item(text: String) = WMenuFlyoutItem(text).also { item ->
        item.addActionListener { result.text = Messages.get("menubar.result.selected", text) }
    }

    val fileMenu = WMenuBarItem(Messages.get("menubar.file"))
    fileMenu.add(item(Messages.get("menubar.new")))
    fileMenu.add(item(Messages.get("menubar.open")))
    fileMenu.add(item(Messages.get("menubar.save")))
    fileMenu.add(WMenuFlyoutSeparator())
    fileMenu.add(item(Messages.get("menubar.exit")))

    val editMenu = WMenuBarItem(Messages.get("menubar.edit"))
    editMenu.add(item(Messages.get("menubar.undo")))
    editMenu.add(item(Messages.get("menubar.cut")))
    editMenu.add(item(Messages.get("menubar.copy")))
    editMenu.add(item(Messages.get("menubar.paste")))

    val helpMenu = WMenuBarItem(Messages.get("menubar.help"))
    helpMenu.add(item(Messages.get("menubar.about")))

    val menuBar = WMenuBar()
    menuBar.add(fileMenu)
    menuBar.add(editMenu)
    menuBar.add(helpMenu)

    val body = WPanel(spacing = 8.0)
    body.add(menuBar)
    body.add(result)
    return buildExample(Messages.get("menubar.example.simple.title"), body)
}

/** A richer menu bar: icons, shortcuts, submenus, toggle and radio items. */
private fun buildRichMenuBarExample(): WComponent {
    val result = WLabel(Messages.get("menubar.result.selected.none"))

    fun item(text: String, icon: Symbol? = null) = WMenuFlyoutItem(text, icon).also { item ->
        item.addActionListener { result.text = Messages.get("menubar.result.selected", text) }
    }

    // Icons plus shortcuts that actually fire (work even without opening the menu)
    val newItem = item(Messages.get("menubar.new"), Symbol.ADD)
    newItem.addKeyboardAccelerator(VirtualKey.N, VirtualKeyModifier.CONTROL)
    val openItem = item(Messages.get("menubar.open"), Symbol.OPEN_FILE)
    openItem.addKeyboardAccelerator(VirtualKey.O, VirtualKeyModifier.CONTROL)
    val saveItem = item(Messages.get("menubar.save"), Symbol.SAVE)
    saveItem.addKeyboardAccelerator(VirtualKey.S, VirtualKeyModifier.CONTROL)

    // A submenu (cascading)
    val shareSubMenu = WMenuFlyoutSubItem(Messages.get("menubar.share"), Symbol.SHARE)
    shareSubMenu.add(item(Messages.get("menubar.sendmail"), Symbol.MAIL))
    shareSubMenu.add(item(Messages.get("menubar.copylink"), Symbol.LINK))

    val fileMenu = WMenuBarItem(Messages.get("menubar.file"))
    fileMenu.add(newItem)
    fileMenu.add(openItem)
    fileMenu.add(saveItem)
    fileMenu.add(WMenuFlyoutSeparator())
    fileMenu.add(shareSubMenu)

    // A toggle item and radio items (mutually exclusive selection)
    val statusBarItem = WToggleMenuFlyoutItem(Messages.get("menubar.statusbar"))
    statusBarItem.isChecked = true
    statusBarItem.addActionListener {
        result.text = Messages.get("menubar.result.statusbar", statusBarItem.isChecked.toString())
    }

    fun radio(text: String) = WRadioMenuFlyoutItem(text, groupName = "orientation").also { item ->
        item.addActionListener { result.text = Messages.get("menubar.result.orientation", text) }
    }

    val landscapeItem = radio(Messages.get("menubar.landscape"))
    landscapeItem.isChecked = true

    val viewMenu = WMenuBarItem(Messages.get("menubar.view"))
    viewMenu.add(statusBarItem)
    viewMenu.add(WMenuFlyoutSeparator())
    viewMenu.add(landscapeItem)
    viewMenu.add(radio(Messages.get("menubar.portrait")))

    val menuBar = WMenuBar()
    menuBar.add(fileMenu)
    menuBar.add(viewMenu)

    val body = WPanel(spacing = 8.0)
    body.add(menuBar)
    body.add(result)
    return buildExample(Messages.get("menubar.example.rich.title"), body)
}

// endregion

// region MenuFlyout

/** MenuFlyout page: lines up demos exercising WMenuFlyout. */
internal fun buildMenuFlyoutPage(): WComponent {
    val page = buildPage("MenuFlyout", Messages.get("menuflyout.description"))

    page.add(buildSimpleMenuFlyoutExample())
    page.add(buildRadioMenuFlyoutExample())
    page.add(buildContextMenuFlyoutExample())
    return page
}

/** A basic menu flyout: opened from a DropDownButton. */
private fun buildSimpleMenuFlyoutExample(): WComponent {
    val result = WLabel(Messages.get("menuflyout.result.selected.none"))

    val menuFlyout = WMenuFlyout()
    for (text in listOf(Messages.get("menuflyout.reset"), Messages.get("menuflyout.repeat"), Messages.get("menuflyout.shuffle"))) {
        menuFlyout.add(
            WMenuFlyoutItem(text).also { item ->
                item.addActionListener { result.text = Messages.get("menuflyout.result.selected", text) }
            },
        )
    }

    val button = WDropDownButton(Messages.get("menuflyout.options"))
    button.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("menuflyout.example.simple.title"), row)
}

/** A menu flyout with radio items: mutually-exclusive sort criteria. */
private fun buildRadioMenuFlyoutExample(): WComponent {
    val result = WLabel(Messages.get("menuflyout.result.sort", Messages.get("menuflyout.sort.rating")))

    val menuFlyout = WMenuFlyout()
    for (text in listOf(Messages.get("menuflyout.sort.rating"), Messages.get("menuflyout.sort.name"), Messages.get("menuflyout.sort.date"))) {
        val item = WRadioMenuFlyoutItem(text, groupName = "sort")
        item.isChecked = text == Messages.get("menuflyout.sort.rating")
        item.addActionListener { result.text = Messages.get("menuflyout.result.sort", text) }
        menuFlyout.add(item)
    }

    val button = WDropDownButton(Messages.get("menuflyout.sort"))
    button.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("menuflyout.example.radio.title"), row)
}

/** A context menu: a MenuFlyout opened by right-click (UIElement.ContextFlyout). */
private fun buildContextMenuFlyoutExample(): WComponent {
    val result = WLabel(Messages.get("menuflyout.result.selected.none"))

    val menuFlyout = WMenuFlyout()
    val copyItem = WMenuFlyoutItem(Messages.get("menuflyout.copy"), Symbol.COPY)
    copyItem.keyboardAcceleratorText = "Ctrl+C" // a display-only shortcut string
    copyItem.addActionListener { result.text = Messages.get("menuflyout.result.selected", Messages.get("menuflyout.copy")) }
    menuFlyout.add(copyItem)
    menuFlyout.add(
        WMenuFlyoutItem(Messages.get("menuflyout.delete"), Symbol.DELETE).also { item ->
            item.addActionListener { result.text = Messages.get("menuflyout.result.selected", Messages.get("menuflyout.delete")) }
        },
    )

    val target = WBorder(
        WLabel(Messages.get("menuflyout.target")).also { it.margin = 24.0 },
    )
    target.background = WColor(226, 235, 246)
    target.cornerRadius = 8.0
    target.contextFlyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(target)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("menuflyout.example.context.title"), row)
}

// endregion

// region SwipeControl

/** SwipeControl page: lines up demos exercising WSwipeControl. */
internal fun buildSwipeControlPage(): WComponent {
    val page = buildPage(
        "SwipeControl",
        Messages.get("swipecontrol.description"),
    )

    page.add(buildSwipeControlExample())
    return page
}

/** Attaching Reveal / Execute swipe items to list-row-like content. */
private fun buildSwipeControlExample(): WComponent {
    val result = WLabel(Messages.get("swipecontrol.result.executed.none"))

    // Swipe right (from the left edge) -> reveals a pin button (Reveal)
    val pinItem = WSwipeItem(Messages.get("swipecontrol.pin"), Symbol.PIN)
    pinItem.background = WColor(96, 165, 250)
    pinItem.addActionListener { result.text = Messages.get("swipecontrol.result.executed", Messages.get("swipecontrol.pin")) }
    val leftItems = WSwipeItems(SwipeMode.REVEAL)
    leftItems.add(pinItem)

    // Swipe left (from the right edge) -> deletes immediately once fully swiped (Execute)
    val deleteItem = WSwipeItem(Messages.get("swipecontrol.delete"), Symbol.DELETE)
    deleteItem.background = WColor(239, 68, 68)
    deleteItem.addActionListener { result.text = Messages.get("swipecontrol.result.executed", Messages.get("swipecontrol.delete")) }
    val rightItems = WSwipeItems(SwipeMode.EXECUTE)
    rightItems.add(deleteItem)

    val rowContent = WBorder(
        WLabel(Messages.get("swipecontrol.row")).also { it.margin = 16.0 },
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
    return buildExample(Messages.get("swipecontrol.example.title"), body)
}

// endregion

// region StandardUICommand

/** StandardUICommand page: lines up demos exercising WStandardUICommand. */
internal fun buildStandardUICommandPage(): WComponent {
    val page = buildPage(
        "StandardUICommand",
        Messages.get("standarduicommand.description"),
    )

    page.add(buildStandardUICommandExample())
    return page
}

/** A list of predefined commands: each Kind's label and icon apply automatically. */
private fun buildStandardUICommandExample(): WComponent {
    val result = WLabel(Messages.get("standarduicommand.result.none"))

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
        command.addExecuteListener { result.text = Messages.get("standarduicommand.result", command.label) }
        // The label, icon, and shortcut (e.g. Ctrl+C) apply automatically from the command
        row.add(WAppBarButton().also { it.command = command })
    }

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample(Messages.get("standarduicommand.example.title"), body)
}

// endregion

// region XamlUICommand

/** XamlUICommand page: lines up demos exercising WXamlUICommand. */
internal fun buildXamlUICommandPage(): WComponent {
    val page = buildPage(
        "XamlUICommand",
        Messages.get("xamluicommand.description"),
    )

    page.add(buildXamlUICommandExample())
    return page
}

/** A custom command: sharing the same look and behavior across multiple controls. */
private fun buildXamlUICommandExample(): WComponent {
    val result = WLabel(Messages.get("xamluicommand.result.count", "0"))
    var count = 0

    val command = WXamlUICommand(Messages.get("xamluicommand.label"))
    command.icon = Symbol.FAVORITE
    command.description = Messages.get("xamluicommand.commanddescription")
    command.addKeyboardAccelerator(VirtualKey.F, VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT)
    command.addExecuteListener {
        count++
        result.text = Messages.get("xamluicommand.result.count", count.toString())
    }

    // Set the same command on a Button and an AppBarButton (label and icon apply automatically)
    val button = WButton()
    button.command = command
    val appBarButton = WAppBarButton()
    appBarButton.command = command

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    row.add(appBarButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(WLabel(Messages.get("xamluicommand.shortcut")).also { it.foreground = TEXT_SECONDARY })
    return buildExample(Messages.get("xamluicommand.example.title"), body)
}

// endregion
