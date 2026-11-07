package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.CompactOverlaySize
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.NavigationViewBackButtonVisible
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SystemBackdropType
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.TitleBarHeightOption
import com.appkitbox.winui4k.TitleBarTheme
import com.appkitbox.winui4k.WAppWindow
import com.appkitbox.winui4k.WAppWindowPresenterKind
import com.appkitbox.winui4k.WAppWindowTitleBar
import com.appkitbox.winui4k.WAutoSuggestBox
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WColorPicker
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WCompactOverlayPresenter
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WDisplayArea
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WFullScreenPresenter
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WNavigationViewItem
import com.appkitbox.winui4k.WOverlappedPresenter
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTitleBar

/*
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
        Messages.get("appwindow.description"),
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
    frame.setContentPane(stackedContent())
    frame.isVisible = true
    return frame
}

/** The child window's content: a panel that stacks the added components vertically from the top (the default content is a BorderLayout as in JFrame, so it is replaced). */
private fun stackedContent(): WPanel = WPanel(spacing = 12.0).also { it.margin = 24.0 }

/** The "close" button always placed at the top of a child window's content. */
private fun WFrame.addCloseButton() {
    val closeButton = WButton(Messages.get("appwindow.child.close"))
    closeButton.addActionListener { dispose() }
    add(closeButton)
}

/** 1) Title / Resize / Move / SetIcon. */
private fun buildAppWindowBasicExample(): WComponent {
    val status = WLabel(Messages.get("appwindow.child.none"))
    var child: WFrame? = null

    val openButton = WButton(Messages.get("appwindow.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindow.basic.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindow.basic.child.label")))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = Messages.get("appwindow.basic.opened", frame.appWindow.title)
    }

    val titleButton = WButton(Messages.get("appwindow.basic.changetitle"))
    titleButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.title = Messages.get("appwindow.basic.newtitle", (System.currentTimeMillis() % 1000).toString())
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
    return buildExample(Messages.get("appwindow.basic.title"), body)
}

/** 2) Screen centering: computes the center point from WDisplayArea.nearest(appWindow).workArea. */
private fun buildAppWindowCenterExample(): WComponent {
    val status = WLabel(Messages.get("appwindow.child.none"))
    var child: WFrame? = null

    val openButton = WButton(Messages.get("appwindow.center.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindow.center.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindow.center.child.label")))
        frame.appWindow.resize(360, 240)
        frame.appWindow.move(50, 50) // First place it toward a screen edge
        child = frame
        status.text = Messages.get("appwindow.child.opened")
    }

    val centerButton = WButton(Messages.get("appwindow.center.button"))
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
    return buildExample(Messages.get("appwindow.center.title"), body)
}

/** 3) OverlappedPresenter's 6 toggles + Maximize/Minimize/Restore + the State display. */
@Suppress("LongMethod") // Declarative UI-building sample code
private fun buildAppWindowPresenterExample(): WComponent {
    val status = WLabel(Messages.get("appwindow.child.none"))
    var child: WFrame? = null
    var presenter: WOverlappedPresenter? = null

    val openButton = WButton(Messages.get("appwindow.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindow.presenter.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindow.presenter.child.label")))
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
    val applyBorderButton = WButton(Messages.get("appwindow.presenter.applyborder"))
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
    maximizeButton.addActionListener {
        presenter?.maximize()
        refreshState()
    }
    val minimizeButton = WButton("Minimize")
    minimizeButton.addActionListener {
        presenter?.minimize()
        refreshState()
    }
    val restoreButton = WButton("Restore")
    restoreButton.addActionListener {
        presenter?.restore()
        refreshState()
    }
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
    val status = WLabel(Messages.get("appwindow.child.none"))
    var presenter: WOverlappedPresenter? = null

    val openButton = WButton(Messages.get("appwindow.minmax.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindow.minmax.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindow.minmax.child.label")))
        frame.appWindow.resize(400, 300)
        val newPresenter = WOverlappedPresenter.create()
        newPresenter.preferredMinimumWidth = 300
        newPresenter.preferredMinimumHeight = 200
        newPresenter.preferredMaximumWidth = 800
        newPresenter.preferredMaximumHeight = 600
        frame.appWindow.setPresenter(newPresenter)
        presenter = newPresenter
        status.text = Messages.get("appwindow.minmax.set")
    }

    val clearButton = WButton(Messages.get("appwindow.minmax.clear"))
    clearButton.addActionListener {
        val p = presenter ?: return@addActionListener
        p.preferredMinimumWidth = null
        p.preferredMinimumHeight = null
        p.preferredMaximumWidth = null
        p.preferredMaximumHeight = null
        status.text = Messages.get("appwindow.minmax.cleared")
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample(Messages.get("appwindow.minmax.title"), body)
}

/**
 * 5) Modal (createForDialog + WAppWindow.create(presenter, owner) + isModal).
 * Since an owner is required to create a raw AppWindow (unrelated to WFrame), note that its
 * content ends up empty.
 */
private fun buildAppWindowModalExample(): WComponent {
    val status = WLabel(Messages.get("appwindow.modal.none"))

    val openButton = WButton(Messages.get("appwindow.modal.open"))
    openButton.addActionListener {
        // Creates a raw AppWindow that requires an owner (the Gallery itself). Even with
        // IsModal=true it ends up a pure AppWindow with no Content, so real usage would need
        // separate UI layered on top of it.
        val mainFrame = WFrame(title = "Gallery")
        val presenter = WOverlappedPresenter.createForDialog()
        presenter.isModal = true
        val modalWindow = WAppWindow.create(presenter, owner = mainFrame)
        modalWindow.title = Messages.get("appwindow.modal.window.title")
        modalWindow.resize(360, 200)
        status.text = Messages.get("appwindow.modal.created", presenter.isModal.toString())
    }

    val note = WLabel(
        Messages.get("appwindow.modal.note"),
    ).also {
        it.foreground = TEXT_SECONDARY
        it.textWrapping = TextWrapping.WRAP
    }

    val body = WPanel(spacing = 8.0)
    body.add(note)
    body.add(openButton)
    body.add(status)
    return buildExample(Messages.get("appwindow.modal.title"), body)
}

/** 6) FullScreen (a "restore" button is required). */
private fun buildAppWindowFullScreenExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel(Messages.get("appwindow.child.none"))

    val openButton = WButton(Messages.get("appwindow.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindow.fullscreen.child.title"))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = Messages.get("appwindow.child.opened")
        // addCloseButton is skipped since it can be unreachable while full-screen; provide a separate restore button instead
        frame.add(WLabel(Messages.get("appwindow.fullscreen.child.label")))
    }

    val fullScreenButton = WButton(Messages.get("appwindow.fullscreen.enter"))
    fullScreenButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.setPresenter(WFullScreenPresenter.create())
        status.text = Messages.get("appwindow.fullscreen.entered")
    }

    val restoreButton = WButton(Messages.get("appwindow.restore"))
    restoreButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.setPresenter(WAppWindowPresenterKind.DEFAULT)
        status.text = Messages.get("appwindow.restored")
    }

    val closeButton = WButton(Messages.get("appwindow.child.close"))
    closeButton.addActionListener { child?.dispose() }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(fullScreenButton)
    buttons.add(restoreButton)
    buttons.add(closeButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample(Messages.get("appwindow.fullscreen.title"), body)
}

/** 7) CompactOverlay (a size ComboBox). */
private fun buildAppWindowCompactOverlayExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel(Messages.get("appwindow.child.none"))

    val openButton = WButton(Messages.get("appwindow.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindow.compactoverlay.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindow.compactoverlay.child.label")))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = Messages.get("appwindow.child.opened")
    }

    val sizeCombo = WComboBox(CompactOverlaySize.entries.map { it.name })
    sizeCombo.selectedIndex = 1 // MEDIUM

    val applyButton = WButton(Messages.get("appwindow.compactoverlay.apply"))
    applyButton.addActionListener {
        val frame = child ?: return@addActionListener
        val size = CompactOverlaySize.entries[sizeCombo.selectedIndex.coerceAtLeast(0)]
        val presenter = WCompactOverlayPresenter.create()
        presenter.initialSize = size
        frame.appWindow.setPresenter(presenter)
        status.text = Messages.get("appwindow.compactoverlay.applied", size.toString())
    }

    val restoreButton = WButton(Messages.get("appwindow.restore"))
    restoreButton.addActionListener {
        val frame = child ?: return@addActionListener
        frame.appWindow.setPresenter(WAppWindowPresenterKind.DEFAULT)
        status.text = Messages.get("appwindow.restored")
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
        Messages.get("appwindowtitlebar.description"),
    )

    page.add(buildTitleBarColorExample())
    page.add(buildTitleBarExtendAndHeightExample())
    page.add(buildTitleBarThemeExample())
    return page
}

/** 1) The 12 colors (choose the target with a WComboBox + WColorPicker + ResetToDefault). */
private fun buildTitleBarColorExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel(Messages.get("appwindowtitlebar.child.none"))

    val openButton = WButton(Messages.get("appwindowtitlebar.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindowtitlebar.color.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindowtitlebar.color.child.label")))
        // BackgroundColor/ForegroundColor and friends only take visual effect on the system-drawn
        // title bar (legacy colors, ExtendsContentIntoTitleBar = false). Setting it to true means the
        // app's content draws the title bar area itself, so the background/text colors stop being
        // used (the real WinUI-Gallery's AppWindowTitleBarWindow.xaml.cs leaves it unset too).
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = Messages.get("appwindowtitlebar.child.opened")
    }

    val targetCombo = WComboBox(TITLE_BAR_COLOR_SLOTS.map { it.name })
    targetCombo.selectedIndex = 0

    val picker = WColorPicker()
    picker.color = DEMO_BLUE

    val applyButton = WButton(Messages.get("appwindowtitlebar.color.apply"))
    applyButton.addActionListener {
        val frame = child ?: return@addActionListener
        val slot = TITLE_BAR_COLOR_SLOTS[targetCombo.selectedIndex.coerceAtLeast(0)]
        slot.set(frame.appWindow.titleBar, picker.color)
        status.text = Messages.get("appwindowtitlebar.color.applied", slot.name, picker.color.toString())
    }

    val resetButton = WButton(Messages.get("appwindowtitlebar.color.reset"))
    resetButton.addActionListener {
        child?.appWindow?.titleBar?.resetToDefault()
        status.text = Messages.get("appwindowtitlebar.color.resetdone")
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
    return buildExample(Messages.get("appwindowtitlebar.color.title"), body)
}

/** 2) ExtendsContentIntoTitleBar + PreferredHeightOption (a child window). */
private fun buildTitleBarExtendAndHeightExample(): WComponent {
    var child: WFrame? = null
    val status = WLabel(Messages.get("appwindowtitlebar.child.none"))

    val openButton = WButton(Messages.get("appwindowtitlebar.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindowtitlebar.extend.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindowtitlebar.extend.child.label")))
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = Messages.get("appwindowtitlebar.child.opened")
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
    val status = WLabel(Messages.get("appwindowtitlebar.child.none"))

    val openButton = WButton(Messages.get("appwindowtitlebar.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("appwindowtitlebar.theme.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("appwindowtitlebar.theme.child.label")))
        // PreferredTheme also needs ExtendsContentIntoTitleBar = true, otherwise it stays the legacy colors and the change won't be visible
        frame.extendsContentIntoTitleBar = true
        frame.appWindow.resize(480, 320)
        child = frame
        status.text = Messages.get("appwindowtitlebar.child.opened")
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
    return buildExample(Messages.get("appwindowtitlebar.theme.title"), body)
}

// endregion

// region SystemBackdrop page

/** The SystemBackdrop page: lines up demos for trying out WFrame.systemBackdrop's various materials. */
internal fun buildSystemBackdropPage(): WComponent {
    val page = buildPage(
        "SystemBackdrop",
        Messages.get("systembackdrop.description"),
    )

    page.add(buildSystemBackdropSwitchExample())
    return page
}

/** Switching backdrops: applies every SystemBackdropType to a child window. */
private fun buildSystemBackdropSwitchExample(): WComponent {
    val status = WLabel(Messages.get("systembackdrop.child.none"))
    var child: WFrame? = null

    val openButton = WButton(Messages.get("systembackdrop.child.open"))
    openButton.addActionListener {
        val frame = openChildFrame(Messages.get("systembackdrop.child.title"))
        frame.addCloseButton()
        frame.add(WLabel(Messages.get("systembackdrop.child.label")))
        frame.appWindow.resize(480, 320)
        frame.systemBackdrop = SystemBackdropType.MICA
        child = frame
        status.text = "SystemBackdrop = ${frame.systemBackdrop}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    for (type in SystemBackdropType.entries) {
        buttons.add(
            WButton(type.name).also { button ->
                button.addActionListener {
                    val frame = child ?: return@addActionListener
                    frame.systemBackdrop = type
                    status.text = "SystemBackdrop = ${frame.systemBackdrop}"
                }
            },
        )
    }

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(status)
    return buildExample(Messages.get("systembackdrop.switch.title"), body)
}

// endregion

// region Multiple windows page

/** The Multiple windows page: lines up a demo for creating and managing multiple child windows. */
internal fun buildMultipleWindowsPage(): WComponent {
    val page = buildPage(
        "Multiple windows",
        Messages.get("multiplewindows.description"),
    )

    page.add(buildMultipleWindowsExample())
    return page
}

private fun buildMultipleWindowsExample(): WComponent {
    val children = mutableListOf<WFrame>()
    val count = WLabel(Messages.get("multiplewindows.count", "0"))

    fun updateCount() {
        children.removeAll { !it.isDisplayable }
        count.text = Messages.get("multiplewindows.count", children.size.toString())
    }

    val openButton = WButton(Messages.get("multiplewindows.open"))
    openButton.addActionListener {
        val frame = WFrame(title = Messages.get("multiplewindows.child.title", (children.size + 1).toString()))
        frame.setContentPane(stackedContent())
        // Matching the real Gallery's Multiple windows demo, open it with an extended title bar
        frame.extendsContentIntoTitleBar = true
        val closeButton = WButton(Messages.get("multiplewindows.child.close"))
        closeButton.addActionListener {
            frame.dispose()
            updateCount()
        }
        frame.add(WLabel(Messages.get("multiplewindows.child.label")))
        frame.add(closeButton)
        frame.isVisible = true
        frame.appWindow.resizeClient(600, 400)
        children += frame
        updateCount()
    }

    val closeAllButton = WButton(Messages.get("multiplewindows.closeall"))
    closeAllButton.addActionListener {
        for (frame in children) frame.dispose()
        updateCount()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(openButton)
    buttons.add(closeAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(count)
    return buildExample(Messages.get("multiplewindows.title"), body)
}

// endregion

// region TitleBar page

/** The TitleBar page: lines up demos for trying out WTitleBar's various features. */
internal fun buildTitleBarPage(): WComponent {
    val page = buildPage(
        "TitleBar",
        Messages.get("titlebar.description"),
    )

    page.add(buildTitleBarInlineExample())
    page.add(buildTitleBarDragRegionExample())
    page.add(buildTitleBarEndToEndExample())
    return page
}

/** 1) Operating properties inline (Title/Subtitle/back/pane-toggle/content=WAutoSuggestBox). */
private fun buildTitleBarInlineExample(): WComponent {
    val titleBar = WTitleBar()
    titleBar.title = Messages.get("titlebar.inline.sampletitle")
    titleBar.subtitle = Messages.get("titlebar.inline.subtitle")
    titleBar.isBackButtonVisible = true
    titleBar.isPaneToggleButtonVisible = true

    val log = WLabel(Messages.get("titlebar.inline.event.none"))
    log.textWrapping = TextWrapping.WRAP
    titleBar.addBackRequestedListener { log.text = Messages.get("titlebar.inline.event.back") }
    titleBar.addPaneToggleRequestedListener { log.text = Messages.get("titlebar.inline.event.panetoggle") }

    val titleField = WTextField().apply { placeholderText = Messages.get("titlebar.inline.placeholder.title") }.also { it.width = 200.0 }
    titleField.text = titleBar.title
    val subtitleField = WTextField().apply { placeholderText = Messages.get("titlebar.inline.subtitle") }.also { it.width = 200.0 }
    subtitleField.text = titleBar.subtitle
    val applyButton = WButton(Messages.get("titlebar.inline.apply"))
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

    val useSearchBoxCheck = WCheckBox(Messages.get("titlebar.inline.searchbox"))
    useSearchBoxCheck.addItemListener { checked ->
        titleBar.content = if (checked == true) WAutoSuggestBox(Messages.get("titlebar.inline.search")) else null
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
        Messages.get("titlebar.inline.title"),
        body,
    )
}

/** 2) Drag region (setIsDragRegion). */
private fun buildTitleBarDragRegionExample(): WComponent {
    val titleBar = WTitleBar()
    titleBar.title = Messages.get("titlebar.drag.sampletitle")

    val dragButton = WButton(Messages.get("titlebar.drag.region"))
    WTitleBar.setIsDragRegion(dragButton, true)

    val autoButton = WButton(Messages.get("titlebar.drag.auto"))
    autoButton.addActionListener { WTitleBar.setIsDragRegion(dragButton, null) }

    val nonDragButton = WButton("SetIsDragRegion = false")
    nonDragButton.addActionListener { WTitleBar.setIsDragRegion(dragButton, false) }

    val note = WLabel(
        Messages.get("titlebar.drag.note"),
    ).also {
        it.foreground = TEXT_SECONDARY
        it.textWrapping = TextWrapping.WRAP
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(dragButton)
    buttons.add(autoButton)
    buttons.add(nonDragButton)

    val body = WPanel(spacing = 8.0)
    body.add(note)
    body.add(titleBar)
    body.add(buttons)
    return buildExample(Messages.get("titlebar.drag.title"), body)
}

/** 3) An end-to-end child window (a full WTitleBar + WNavigationView setup). */
private fun buildTitleBarEndToEndExample(): WComponent {
    val status = WLabel(Messages.get("titlebar.endtoend.none"))

    val openButton = WButton(Messages.get("titlebar.endtoend.open"))
    openButton.addActionListener {
        val frame = WFrame(title = Messages.get("titlebar.endtoend.window.title"))

        val titleBar = WTitleBar()
        titleBar.title = Messages.get("titlebar.endtoend.window.title")
        titleBar.isPaneToggleButtonVisible = true

        val navigationView = WNavigationView()
        navigationView.isPaneToggleButtonVisible = false // Avoid showing it twice; only the TitleBar side shows it
        navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
        val item1 = WNavigationViewItem(Messages.get("titlebar.endtoend.page1"))
        val item2 = WNavigationViewItem(Messages.get("titlebar.endtoend.page2"))
        navigationView.addItem(item1)
        navigationView.addItem(item2)
        val content = WLabel(Messages.get("titlebar.endtoend.page1"))
        content.horizontalAlignmentInParent = HorizontalAlignment.LEFT
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
        status.text = Messages.get("titlebar.endtoend.opened")
    }

    val body = WPanel(spacing = 8.0)
    body.add(openButton)
    body.add(status)
    return buildExample(Messages.get("titlebar.endtoend.title"), body)
}

// endregion
