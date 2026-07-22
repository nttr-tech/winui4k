package jp.hisano.winui4k.gallery

import jp.hisano.winui4k.CompactOverlaySize
import jp.hisano.winui4k.GridLength
import jp.hisano.winui4k.HorizontalAlignment
import jp.hisano.winui4k.NavigationViewBackButtonVisible
import jp.hisano.winui4k.Orientation
import jp.hisano.winui4k.TextWrapping
import jp.hisano.winui4k.TitleBarHeightOption
import jp.hisano.winui4k.TitleBarTheme
import jp.hisano.winui4k.WAppWindow
import jp.hisano.winui4k.WAppWindowPresenterKind
import jp.hisano.winui4k.WAppWindowTitleBar
import jp.hisano.winui4k.WAutoSuggestBox
import jp.hisano.winui4k.WButton
import jp.hisano.winui4k.WCheckBox
import jp.hisano.winui4k.WColor
import jp.hisano.winui4k.WColorPicker
import jp.hisano.winui4k.WComboBox
import jp.hisano.winui4k.WCompactOverlayPresenter
import jp.hisano.winui4k.WComponent
import jp.hisano.winui4k.WDisplayArea
import jp.hisano.winui4k.WFrame
import jp.hisano.winui4k.WFullScreenPresenter
import jp.hisano.winui4k.WGrid
import jp.hisano.winui4k.WLabel
import jp.hisano.winui4k.WNavigationView
import jp.hisano.winui4k.WNavigationViewItem
import jp.hisano.winui4k.WOverlappedPresenter
import jp.hisano.winui4k.WPanel
import jp.hisano.winui4k.WTextField
import jp.hisano.winui4k.WTitleBar

/**
 * Windowing category: the 4 pages AppWindow / AppWindowTitleBar / Multiple windows / TitleBar.
 * Since these demos mostly manipulate native windows themselves, destructive operations (switching
 * to FullScreen / CompactOverlay / a different Presenter, etc.) are never applied to the main
 * Gallery window — always do them on a child window (a separate [WFrame]). Always attach a
 * "close" button to child windows.
 */

// region AppWindow page

/** The AppWindow page: lines up demos for trying out WAppWindow's various features. */
internal fun buildAppWindowPage(): WComponent {
    val page = buildPage(
        "AppWindow",
        "WinUI 3's AppWindow, which manages a native window (title, position, size, behavior). " +
            "All operations are performed on a child window and never affect the Gallery itself.",
    )

    page.add(buildAppWindowBasicExample())
    page.add(buildAppWindowCenterExample())
    page.add(buildAppWindowPresenterExample())
    page.add(buildAppWindowMinMaxSizeExample())
    page.add(buildAppWindowModalExample())
    page.add(buildAppWindowFullScreenExample())
    page.add(buildAppWindowCompactOverlayExample())
    return page
}

/** Creates and shows a single child window (with a close button). The basis for each demo below. */
private fun openChildFrame(title: String): WFrame {
    val frame = WFrame(title = title)
    frame.isVisible = true
    return frame
}

/** The "close" button always placed at the top of a child window's content. */
private fun WFrame.addCloseButton() {
    val closeButton = WButton("Close this window")
    closeButton.addActionListener { isVisible = false }
    add(closeButton)
}

/** 1) Title / Resize / Move / SetIcon. */
private fun buildAppWindowBasicExample(): WComponent {
    val status = WLabel("No child window has been created yet")
    var child: WFrame? = null

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("AppWindow demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("You can operate this window's AppWindow."))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = "Opened a child window (Title = \"${frame.appWindow.title}\")"
    }

    val titleButton = WButton("Change the title")
    titleButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.title = "Changed title (${System.currentTimeMillis() % 1000})"
        status.text = "Title = \"${frame.appWindow.title}\""
    }

    val resizeButton = WButton("Resize(640, 480)")
    resizeButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.resize(640, 480)
        status.text = "Size = ${frame.appWindow.size} / ClientSize = ${frame.appWindow.clientSize}"
    }

    val moveButton = WButton("Move(100, 100)")
    moveButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.move(100, 100)
        status.text = "Position = ${frame.appWindow.position}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(titleButton)
    buttons.add(resizeButton)
    buttons.add(moveButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample("Title, size, and position (Title / Resize / Move / Size / ClientSize / Position)", body)
}

/** 2) Screen centering: computes the center point from WDisplayArea.nearest(appWindow).workArea. */
private fun buildAppWindowCenterExample(): WComponent {
    val status = WLabel("No child window has been created yet")
    var child: WFrame? = null

    val openButton = WButton("Open a child window (placed toward the bottom-right of the screen)")
    openButton.addActionListener {
        val frame = openChildFrame("Screen centering demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("The \"Center it\" button computes the center point from DisplayArea.WorkArea."))
        frame.appWindow.resize(360, 240)
        frame.appWindow.move(50, 50) // First place it toward a screen edge
        child = frame
        status.text = "Opened a child window"
    }

    val centerButton = WButton("Center it")
    centerButton.addActionListener {
        val frame = child ?: return@addActionListener
        val workArea = WDisplayArea.nearest(frame.appWindow).workArea
        val size = frame.appWindow.size
        val x = workArea.x + (workArea.width - size.width) / 2
        val y = workArea.y + (workArea.height - size.height) / 2
        frame.appWindow.move(x, y)
        status.text = "WorkArea = $workArea -> Position = ${frame.appWindow.position}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(centerButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample("Screen centering (WDisplayArea.nearest / workArea)", body)
}

/** 3) OverlappedPresenter's 6 toggles + Maximize/Minimize/Restore + the State display. */
private fun buildAppWindowPresenterExample(): WComponent {
    val status = WLabel("No child window has been created yet")
    var child: WFrame? = null
    var presenter: WOverlappedPresenter? = null

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("OverlappedPresenter demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("Use the toggles and buttons to operate OverlappedPresenter's properties."))
        frame.appWindow.resize(480, 360)
        val newPresenter = WOverlappedPresenter.create()
        frame.appWindow.setPresenter(newPresenter)
        child = frame
        presenter = newPresenter
        status.text = "state = ${newPresenter.state}"
    }

    fun refreshState() {
        status.text = "state = ${presenter?.state}"
    }

    val alwaysOnTopCheck = WCheckBox("IsAlwaysOnTop")
    alwaysOnTopCheck.addItemListener { checked -> presenter?.isAlwaysOnTop = checked == true }
    val maximizableCheck = WCheckBox("IsMaximizable").also { it.isChecked = true }
    maximizableCheck.addItemListener { checked -> presenter?.isMaximizable = checked == true }
    val minimizableCheck = WCheckBox("IsMinimizable").also { it.isChecked = true }
    minimizableCheck.addItemListener { checked -> presenter?.isMinimizable = checked == true }
    val resizableCheck = WCheckBox("IsResizable").also { it.isChecked = true }
    resizableCheck.addItemListener { checked -> presenter?.isResizable = checked == true }
    val borderCheck = WCheckBox("HasBorder").also { it.isChecked = true }
    val titleBarCheck = WCheckBox("HasTitleBar").also { it.isChecked = true }
    val applyBorderButton = WButton("Apply Border/TitleBar (SetBorderAndTitleBar)")
    applyBorderButton.addActionListener {
        presenter?.setBorderAndTitleBar(borderCheck.isChecked == true, titleBarCheck.isChecked == true)
    }

    val checks = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    checks.add(alwaysOnTopCheck)
    checks.add(maximizableCheck)
    checks.add(minimizableCheck)
    checks.add(resizableCheck)

    val borderRow = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    borderRow.add(borderCheck)
    borderRow.add(titleBarCheck)
    borderRow.add(applyBorderButton)

    val maximizeButton = WButton("Maximize")
    maximizeButton.addActionListener { presenter?.maximize(); refreshState() }
    val minimizeButton = WButton("Minimize")
    minimizeButton.addActionListener { presenter?.minimize(); refreshState() }
    val restoreButton = WButton("Restore")
    restoreButton.addActionListener { presenter?.restore(); refreshState() }
    val stateButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    stateButtons.add(maximizeButton)
    stateButtons.add(minimizeButton)
    stateButtons.add(restoreButton)

    val body = WPanel(spacing = 8.0)
    body.add(openButton)
    body.add(checks)
    body.add(borderRow)
    body.add(stateButtons)
    body.add(status)
    return buildExample(
        "OverlappedPresenter (IsAlwaysOnTop / IsMaximizable / IsMinimizable / IsResizable / " +
            "SetBorderAndTitleBar / Maximize / Minimize / Restore / State)",
        body,
    )
}

/** 4) Min/max size (PreferredMinimum/MaximumWidth/Height, null clears them). */
private fun buildAppWindowMinMaxSizeExample(): WComponent {
    val status = WLabel("No child window has been created yet")
    var presenter: WOverlappedPresenter? = null

    val openButton = WButton("Open a child window (min 300x200 / max 800x600)")
    openButton.addActionListener {
        val frame = openChildFrame("Min/max size demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("Drag-resize the window to see the constraints in action."))
        frame.appWindow.resize(400, 300)
        val newPresenter = WOverlappedPresenter.create()
        newPresenter.preferredMinimumWidth = 300
        newPresenter.preferredMinimumHeight = 200
        newPresenter.preferredMaximumWidth = 800
        newPresenter.preferredMaximumHeight = 600
        frame.appWindow.setPresenter(newPresenter)
        presenter = newPresenter
        status.text = "Set Min = (300, 200) / Max = (800, 600)"
    }

    val clearButton = WButton("Clear the constraints (null)")
    clearButton.addActionListener {
        val p = presenter ?: return@addActionListener
        p.preferredMinimumWidth = null
        p.preferredMinimumHeight = null
        p.preferredMaximumWidth = null
        p.preferredMaximumHeight = null
        status.text = "Cleared the constraints"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample("Min/max size (PreferredMinimum/MaximumWidth/Height)", body)
}

/**
 * 5) Modal (createForDialog + WAppWindow.create(presenter, owner) + isModal).
 * Since an owner is required to create a raw AppWindow (unrelated to WFrame), note that its
 * content ends up empty.
 */
private fun buildAppWindowModalExample(): WComponent {
    val status = WLabel("Not created yet")

    val openButton = WButton("Open a modal-like child window (empty content)")
    openButton.addActionListener {
        // Creates a raw AppWindow that requires an owner (the Gallery itself). Even with
        // IsModal=true it ends up a pure AppWindow with no Content, so real usage would need
        // separate UI layered on top of it.
        val mainFrame = WFrame(title = "Gallery")
        val presenter = WOverlappedPresenter.createForDialog()
        presenter.isModal = true
        val modalWindow = WAppWindow.create(presenter, owner = mainFrame)
        modalWindow.title = "Modal AppWindow (no content)"
        modalWindow.resize(360, 200)
        status.text = "Created an AppWindow with IsModal = ${presenter.isModal} " +
            "(it looks like an empty window since Content isn't set)"
    }

    val note = WLabel(
        "IsModal requires an owner window. An AppWindow with an owner can only be created via " +
            "AppWindowStatics.Create(presenter, ownerWindowId), and is a separate \"raw AppWindow\" " +
            "unrelated to a WFrame's Content.",
    ).also { it.foreground = TEXT_SECONDARY; it.textWrapping = TextWrapping.WRAP }

    val body = WPanel(spacing = 8.0)
    body.add(note)
    body.add(openButton)
    body.add(status)
    return buildExample("Modal (CreateForDialog / IsModal / WAppWindow.create(presenter, owner))", body)
}

/** 6) FullScreen (a "restore" button is required). */
private fun buildAppWindowFullScreenExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel("No child window has been created yet")

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("FullScreen demo (child window)")
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = "Opened a child window"
        // addCloseButton is skipped since it can be unreachable while full-screen; provide a separate restore button instead
        frame.add(WLabel("Try \"Go full screen\" followed by \"Restore\"."))
    }

    val fullScreenButton = WButton("Go full screen")
    fullScreenButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.setPresenter(WFullScreenPresenter.create())
        status.text = "Switched to FullScreenPresenter"
    }

    val restoreButton = WButton("Restore (back to the default Presenter)")
    restoreButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.setPresenter(WAppWindowPresenterKind.DEFAULT)
        status.text = "Restored to the default Presenter"
    }

    val closeButton = WButton("Close this window")
    closeButton.addActionListener { child?.isVisible = false }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(fullScreenButton)
    buttons.add(restoreButton)
    buttons.add(closeButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample("Full screen (FullScreenPresenter / restore via the default Presenter)", body)
}

/** 7) CompactOverlay (a size ComboBox). */
private fun buildAppWindowCompactOverlayExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel("No child window has been created yet")

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("CompactOverlay demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("Try a compact display that always floats on top (picture-in-picture-like)."))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = "Opened a child window"
    }

    val sizeCombo = WComboBox(CompactOverlaySize.entries.map { it.name })
    sizeCombo.selectedIndex = 1 // MEDIUM

    val applyButton = WButton("Switch to CompactOverlay")
    applyButton.addActionListener {
        val frame = child ?: return@addActionListener
        val size = CompactOverlaySize.entries[sizeCombo.selectedIndex.coerceAtLeast(0)]
        val presenter = WCompactOverlayPresenter.create()
        presenter.initialSize = size
        frame.appWindow.setPresenter(presenter)
        status.text = "Switched to CompactOverlayPresenter (InitialSize = $size)"
    }

    val restoreButton = WButton("Restore (back to the default Presenter)")
    restoreButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.setPresenter(WAppWindowPresenterKind.DEFAULT)
        status.text = "Restored to the default Presenter"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(sizeCombo)
    buttons.add(applyButton)
    buttons.add(restoreButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample("CompactOverlay (CompactOverlayPresenter.InitialSize)", body)
}

// endregion

// region AppWindowTitleBar page

/** The name of one of the 12 selectable color properties, plus its getter/setter. */
private class TitleBarColorSlot(val name: String, val get: (WAppWindowTitleBar) -> WColor?, val set: (WAppWindowTitleBar, WColor?) -> Unit)

private val TITLE_BAR_COLOR_SLOTS: List<TitleBarColorSlot> = listOf(
    TitleBarColorSlot("BackgroundColor", { it.backgroundColor }, { t, v -> t.backgroundColor = v }),
    TitleBarColorSlot("ForegroundColor", { it.foregroundColor }, { t, v -> t.foregroundColor = v }),
    TitleBarColorSlot("InactiveBackgroundColor", { it.inactiveBackgroundColor }, { t, v -> t.inactiveBackgroundColor = v }),
    TitleBarColorSlot("InactiveForegroundColor", { it.inactiveForegroundColor }, { t, v -> t.inactiveForegroundColor = v }),
    TitleBarColorSlot("ButtonBackgroundColor", { it.buttonBackgroundColor }, { t, v -> t.buttonBackgroundColor = v }),
    TitleBarColorSlot("ButtonForegroundColor", { it.buttonForegroundColor }, { t, v -> t.buttonForegroundColor = v }),
    TitleBarColorSlot("ButtonHoverBackgroundColor", { it.buttonHoverBackgroundColor }, { t, v -> t.buttonHoverBackgroundColor = v }),
    TitleBarColorSlot("ButtonHoverForegroundColor", { it.buttonHoverForegroundColor }, { t, v -> t.buttonHoverForegroundColor = v }),
    TitleBarColorSlot("ButtonPressedBackgroundColor", { it.buttonPressedBackgroundColor }, { t, v -> t.buttonPressedBackgroundColor = v }),
    TitleBarColorSlot("ButtonPressedForegroundColor", { it.buttonPressedForegroundColor }, { t, v -> t.buttonPressedForegroundColor = v }),
    TitleBarColorSlot("ButtonInactiveBackgroundColor", { it.buttonInactiveBackgroundColor }, { t, v -> t.buttonInactiveBackgroundColor = v }),
    TitleBarColorSlot("ButtonInactiveForegroundColor", { it.buttonInactiveForegroundColor }, { t, v -> t.buttonInactiveForegroundColor = v }),
)

/** The AppWindowTitleBar page: lines up demos for trying out WAppWindowTitleBar's various features. */
internal fun buildAppWindowTitleBarPage(): WComponent {
    val page = buildPage(
        "AppWindowTitleBar",
        "WAppWindowTitleBar, which sets the system title bar's appearance (color, height, theme). " +
            "Colors like BackgroundColor only take visual effect on the system-drawn title bar " +
            "(ExtendsContentIntoTitleBar = false), and the Inactive-prefixed colors are used while inactive.",
    )

    page.add(buildTitleBarColorExample())
    page.add(buildTitleBarExtendAndHeightExample())
    page.add(buildTitleBarThemeExample())
    return page
}

/** 1) The 12 colors (choose the target with a WComboBox + WColorPicker + ResetToDefault). */
private fun buildTitleBarColorExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel("No child window has been created yet")

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("TitleBar color demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("Pick a target color property from the combo below and change it with the ColorPicker."))
        // BackgroundColor/ForegroundColor and friends only take visual effect on the system-drawn
        // title bar (legacy colors, ExtendsContentIntoTitleBar = false). Setting it to true means the
        // app's content draws the title bar area itself, so the background/text colors stop being
        // used (the real WinUI-Gallery's AppWindowTitleBarWindow.xaml.cs leaves it unset too).
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = "Opened a child window"
    }

    val targetCombo = WComboBox(TITLE_BAR_COLOR_SLOTS.map { it.name })
    targetCombo.selectedIndex = 0

    val picker = WColorPicker()
    picker.color = WColor.BLUE

    val applyButton = WButton("Apply this color")
    applyButton.addActionListener {
        val frame = child ?: return@addActionListener
        val slot = TITLE_BAR_COLOR_SLOTS[targetCombo.selectedIndex.coerceAtLeast(0)]
        slot.set(frame.appWindow.titleBar, picker.color)
        status.text = "Applied ${slot.name} = ${picker.color}"
    }

    val resetButton = WButton("ResetToDefault (restore all colors)")
    resetButton.addActionListener {
        child?.appWindow?.titleBar?.resetToDefault()
        status.text = "Restored all colors to their defaults"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(targetCombo)
    buttons.add(applyButton)
    buttons.add(resetButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(picker)
    body.add(status)
    return buildExample("The 12 color properties (IReference<Windows.UI.Color> / ResetToDefault)", body)
}

/** 2) ExtendsContentIntoTitleBar + PreferredHeightOption (a child window). */
private fun buildTitleBarExtendAndHeightExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel("No child window has been created yet")

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("ExtendsContentIntoTitleBar demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("Extend the content into the title bar area, and switch its height."))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = "Opened a child window"
    }

    val extendCheck = WCheckBox("ExtendsContentIntoTitleBar")
    extendCheck.addItemListener { checked -> child?.extendsContentIntoTitleBar = checked == true }

    val heightButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (option in TitleBarHeightOption.entries) {
        heightButtons.add(
            WButton(option.name).also { button ->
                button.addActionListener {
                    val frame = child ?: return@addActionListener
                    frame.appWindow.titleBar.preferredHeightOption = option
                    status.text = "PreferredHeightOption = $option (Height = ${frame.appWindow.titleBar.height}px)"
                }
            },
        )
    }

    val body = WPanel(spacing = 8.0)
    body.add(openButton)
    body.add(extendCheck)
    body.add(heightButtons)
    body.add(status)
    return buildExample("ExtendsContentIntoTitleBar / PreferredHeightOption", body)
}

/** 3) PreferredTheme. */
private fun buildTitleBarThemeExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel("No child window has been created yet")

    val openButton = WButton("Open a child window")
    openButton.addActionListener {
        val frame = openChildFrame("PreferredTheme demo (child window)")
        frame.addCloseButton()
        frame.add(WLabel("Switch the title bar's color theme."))
        // PreferredTheme also needs ExtendsContentIntoTitleBar = true, otherwise it stays the legacy colors and the change won't be visible
        frame.extendsContentIntoTitleBar = true
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = "Opened a child window"
    }

    val themeButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (theme in TitleBarTheme.entries) {
        themeButtons.add(
            WButton(theme.name).also { button ->
                button.addActionListener {
                    val frame = child ?: return@addActionListener
                    frame.appWindow.titleBar.preferredTheme = theme
                    status.text = "PreferredTheme = $theme"
                }
            },
        )
    }

    val body = WPanel(spacing = 8.0)
    body.add(openButton)
    body.add(themeButtons)
    body.add(status)
    return buildExample("Color theme (PreferredTheme)", body)
}

// endregion

// region Multiple windows page

/** The Multiple windows page: lines up a demo for creating and managing multiple child windows. */
internal fun buildMultipleWindowsPage(): WComponent {
    val page = buildPage(
        "Multiple windows",
        "A demo of opening and managing several native windows (WFrame) at the same time.",
    )

    page.add(buildMultipleWindowsExample())
    return page
}

private fun buildMultipleWindowsExample(): WComponent {
    val children = mutableListOf<WFrame>()
    val count = WLabel("Open windows: 0")

    fun updateCount() {
        children.removeAll { !it.isVisible }
        count.text = "Open windows: ${children.size}"
    }

    val openButton = WButton("Open another child window")
    openButton.addActionListener {
        val frame = WFrame(title = "Child window #${children.size + 1}")
        // Matching the real Gallery's Multiple windows demo, open it with an extended title bar
        frame.extendsContentIntoTitleBar = true
        val closeButton = WButton("Close this window")
        closeButton.addActionListener {
            frame.isVisible = false
            updateCount()
        }
        frame.add(WLabel("This window is a WFrame independent of the Gallery itself."))
        frame.add(closeButton)
        frame.isVisible = true
        frame.appWindow.resizeClient(600, 400)
        children += frame
        updateCount()
    }

    val closeAllButton = WButton("Close all")
    closeAllButton.addActionListener {
        for (frame in children) frame.isVisible = false
        updateCount()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(closeAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(count)
    return buildExample("Creating and managing multiple windows (extendsContentIntoTitleBar / resizeClient)", body)
}

// endregion

// region TitleBar page

/** The TitleBar page: lines up demos for trying out WTitleBar's various features. */
internal fun buildTitleBarPage(): WComponent {
    val page = buildPage(
        "TitleBar",
        "WTitleBar, a custom title bar that can have a back button, a pane-toggle button, and " +
            "arbitrary content. It's also used for the Gallery's own title bar.",
    )

    page.add(buildTitleBarInlineExample())
    page.add(buildTitleBarDragRegionExample())
    page.add(buildTitleBarEndToEndExample())
    return page
}

/** 1) Operating properties inline (Title/Subtitle/back/pane-toggle/content=WAutoSuggestBox). */
private fun buildTitleBarInlineExample(): WComponent {
    val titleBar = WTitleBar()
    titleBar.title = "Sample title"
    titleBar.subtitle = "Subtitle"
    titleBar.isBackButtonVisible = true
    titleBar.isPaneToggleButtonVisible = true

    val log = WLabel("Event: none")
    log.textWrapping = TextWrapping.WRAP
    titleBar.addBackRequestedListener { log.text = "Event: BackRequested" }
    titleBar.addPaneToggleRequestedListener { log.text = "Event: PaneToggleRequested" }

    val titleField = WTextField("Title").also { it.width = 200.0 }
    titleField.text = titleBar.title
    val subtitleField = WTextField("Subtitle").also { it.width = 200.0 }
    subtitleField.text = titleBar.subtitle
    val applyButton = WButton("Apply Title/Subtitle")
    applyButton.addActionListener {
        titleBar.title = titleField.text
        titleBar.subtitle = subtitleField.text
    }

    val backVisibleCheck = WCheckBox("IsBackButtonVisible").also { it.isChecked = true }
    backVisibleCheck.addItemListener { checked -> titleBar.isBackButtonVisible = checked == true }
    val backEnabledCheck = WCheckBox("IsBackButtonEnabled").also { it.isChecked = true }
    backEnabledCheck.addItemListener { checked -> titleBar.isBackButtonEnabled = checked == true }
    val paneToggleCheck = WCheckBox("IsPaneToggleButtonVisible").also { it.isChecked = true }
    paneToggleCheck.addItemListener { checked -> titleBar.isPaneToggleButtonVisible = checked == true }

    val useSearchBoxCheck = WCheckBox("Show a search box as Content")
    useSearchBoxCheck.addItemListener { checked ->
        titleBar.content = if (checked == true) WAutoSuggestBox("Search") else null
    }

    val fields = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    fields.add(titleField)
    fields.add(subtitleField)
    fields.add(applyButton)

    val checks = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    checks.add(backVisibleCheck)
    checks.add(backEnabledCheck)
    checks.add(paneToggleCheck)
    checks.add(useSearchBoxCheck)

    val body = WPanel(spacing = 8.0)
    body.add(titleBar)
    body.add(fields)
    body.add(checks)
    body.add(log)
    return buildExample(
        "Inline display (Title / Subtitle / IsBackButtonVisible / IsBackButtonEnabled / " +
            "IsPaneToggleButtonVisible / Content / BackRequested / PaneToggleRequested)",
        body,
    )
}

/** 2) Drag region (setIsDragRegion). */
private fun buildTitleBarDragRegionExample(): WComponent {
    val titleBar = WTitleBar()
    titleBar.title = "Drag region demo"

    val dragButton = WButton("This spot is a drag region (SetIsDragRegion = true)")
    WTitleBar.setIsDragRegion(dragButton, true)

    val autoButton = WButton("Restore default auto-detection (null)")
    autoButton.addActionListener { WTitleBar.setIsDragRegion(dragButton, null) }

    val nonDragButton = WButton("SetIsDragRegion = false")
    nonDragButton.addActionListener { WTitleBar.setIsDragRegion(dragButton, false) }

    val note = WLabel(
        "The TitleBar.IsDragRegion attached property lets you make any component a draggable " +
            "region (by default, AutoRefreshDragRegions automatically makes things like buttons non-draggable).",
    ).also { it.foreground = TEXT_SECONDARY; it.textWrapping = TextWrapping.WRAP }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(dragButton)
    buttons.add(autoButton)
    buttons.add(nonDragButton)

    val body = WPanel(spacing = 8.0)
    body.add(note)
    body.add(titleBar)
    body.add(buttons)
    return buildExample("Drag region (TitleBar.SetIsDragRegion)", body)
}

/** 3) An end-to-end child window (a full WTitleBar + WNavigationView setup). */
private fun buildTitleBarEndToEndExample(): WComponent {
    val status = WLabel("No child window has been created yet")

    val openButton = WButton("Open a WTitleBar + NavigationView child window")
    openButton.addActionListener {
        val frame = WFrame(title = "TitleBar full setup demo")

        val titleBar = WTitleBar()
        titleBar.title = "TitleBar full setup demo"
        titleBar.isPaneToggleButtonVisible = true

        val navigationView = WNavigationView()
        navigationView.isPaneToggleButtonVisible = false // Avoid showing it twice; only the TitleBar side shows it
        navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
        val item1 = WNavigationViewItem("Page 1")
        val item2 = WNavigationViewItem("Page 2")
        navigationView.addItem(item1)
        navigationView.addItem(item2)
        val content = WLabel("Page 1")
        content.horizontalAlignment = HorizontalAlignment.LEFT
        navigationView.content = content
        navigationView.addSelectionListener { item -> content.text = item?.text ?: "" }
        navigationView.selectedItem = item1

        titleBar.addPaneToggleRequestedListener { navigationView.isPaneOpen = !navigationView.isPaneOpen }

        val root = WGrid()
        root.addRow(GridLength.AUTO)
        root.addRow(GridLength.star())
        root.add(titleBar, row = 0, column = 0)
        root.add(navigationView, row = 1, column = 0)

        frame.setContentPane(root)
        frame.extendsContentIntoTitleBar = true
        frame.setTitleBar(titleBar)
        frame.appWindow.titleBar.preferredHeightOption = TitleBarHeightOption.TALL
        frame.appWindow.resize(560, 400)
        frame.isVisible = true
        status.text = "Opened a child window"
    }

    val body = WPanel(spacing = 8.0)
    body.add(openButton)
    body.add(status)
    return buildExample("End-to-end (WTitleBar + WNavigationView + Grid + setTitleBar)", body)
}

// endregion
