package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.InfoBarSeverity
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.ToolTipPlacement
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WInfoBadge
import com.appkitbox.winui4k.WInfoBar
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WProgressBar
import com.appkitbox.winui4k.WProgressRing
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WToolTip

/**
 * Status & info category: demo pages for InfoBadge / InfoBar / ProgressBar / ProgressRing / ToolTip.
 */

// region InfoBadge

/** The InfoBadge page: lines up demos for trying out WInfoBadge's various features. */
internal fun buildInfoBadgePage(): WComponent {
    val page = buildPage(
        "InfoBadge",
        "A small badge that unobtrusively shows an unread count or draws attention. Try out WInfoBadge's various features.",
    )

    page.add(buildInfoBadgeKindsExample())
    page.add(buildInfoBadgeDynamicExample())
    return page
}

/** The kinds of badge: dot badge / numeric badge / icon badge. */
private fun buildInfoBadgeKindsExample(): WComponent {
    val dot = WInfoBadge() // the default value = -1 is a dot badge

    val number = WInfoBadge()
    number.value = 5

    val icon = WInfoBadge()
    icon.setSymbolIcon(Symbol.MESSAGE)

    val row = WPanel(spacing = 24.0, orientation = Orientation.HORIZONTAL)
    row.add(labeledColumn("Dot badge", dot))
    row.add(labeledColumn("Numeric badge", number))
    row.add(labeledColumn("Icon badge", icon))
    return buildExample("Kinds of badge (Value = -1 / a number / IconSource)", row)
}

/** A dynamic badge: increment/reset the number with buttons. */
private fun buildInfoBadgeDynamicExample(): WComponent {
    val badge = WInfoBadge()
    badge.value = 0

    val addButton = WButton("+1")
    addButton.addActionListener { badge.value += 1 }

    val clearButton = WButton("Reset to 0")
    clearButton.addActionListener { badge.value = 0 }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(labeledColumn("Unread", badge))
    row.add(addButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(clearButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Updating the number (Value)", row)
}

/** A small column that stacks a control under a label (used by the badge demos). */
private fun labeledColumn(label: String, control: WComponent): WComponent {
    val column = WPanel(spacing = 6.0)
    column.horizontalAlignment = HorizontalAlignment.LEFT
    column.add(WLabel(label).also { it.foreground = TEXT_SECONDARY })
    column.add(control.also { it.horizontalAlignment = HorizontalAlignment.LEFT })
    return column
}

// endregion

// region InfoBar

/** The InfoBar page: lines up demos for trying out WInfoBar's various features. */
internal fun buildInfoBarPage(): WComponent {
    val page = buildPage(
        "InfoBar",
        "An inline notification bar that reports an in-app state change. Try out WInfoBar's various features.",
    )

    page.add(buildInfoBarSeverityExample())
    page.add(buildInfoBarInteractiveExample())
    page.add(buildInfoBarActionExample())
    return page
}

/** Severity: lines up all four of Informational / Success / Warning / Error. */
private fun buildInfoBarSeverityExample(): WComponent {
    val column = WPanel(spacing = 12.0)
    val severities = listOf(
        InfoBarSeverity.INFORMATIONAL to ("Informational" to "This is an informational message."),
        InfoBarSeverity.SUCCESS to ("Success" to "Your changes have been saved."),
        InfoBarSeverity.WARNING to ("Warning" to "The connection is unstable."),
        InfoBarSeverity.ERROR to ("Error" to "The save failed."),
    )
    for ((severity, text) in severities) {
        val infoBar = WInfoBar()
        infoBar.severity = severity
        infoBar.title = text.first
        infoBar.message = text.second
        infoBar.isClosable = false
        infoBar.isOpen = true
        infoBar.width = 500.0
        column.add(infoBar)
    }
    return buildExample("Severity (Severity: Informational / Success / Warning / Error)", column)
}

/** Opening and closing: toggle isOpen with a button and receive close (x) button clicks. */
private fun buildInfoBarInteractiveExample(): WComponent {
    val result = WLabel("The bar is closed")

    val infoBar = WInfoBar()
    infoBar.severity = InfoBarSeverity.SUCCESS
    infoBar.title = "Download complete"
    infoBar.message = "The file has finished downloading."
    infoBar.width = 500.0
    infoBar.addCloseButtonListener { result.text = "Closed via the close (x) button" }

    val toggleButton = WButton("Open the bar")
    toggleButton.addActionListener {
        infoBar.isOpen = !infoBar.isOpen
        toggleButton.text = if (infoBar.isOpen) "Close the bar" else "Open the bar"
        result.text = if (infoBar.isOpen) "The bar is open" else "The bar is closed"
    }

    val column = WPanel(spacing = 12.0)
    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    column.add(row)
    column.add(infoBar)
    return buildExample("Opening/closing and the close button (IsOpen / CloseButtonClick)", column)
}

/** An action button: places an action button next to the body text. */
private fun buildInfoBarActionExample(): WComponent {
    val result = WLabel("")

    val actionButton = WButton("Restart")
    actionButton.addActionListener { result.text = "A restart was requested" }

    val infoBar = WInfoBar()
    infoBar.severity = InfoBarSeverity.WARNING
    infoBar.title = "An update is pending"
    infoBar.message = "Restart the app to apply the changes."
    infoBar.actionButton = actionButton
    infoBar.isClosable = false
    infoBar.isOpen = true
    infoBar.width = 500.0

    val column = WPanel(spacing = 12.0)
    column.add(infoBar)
    column.add(result)
    return buildExample("Action button (ActionButton)", column)
}

// endregion

// region ProgressBar

/** The ProgressBar page: lines up demos for trying out WProgressBar's various features. */
internal fun buildProgressBarPage(): WComponent {
    val page = buildPage(
        "ProgressBar",
        "A control that shows a task's progress as a bar. Try out WProgressBar's various features.",
    )

    page.add(buildDeterminateProgressBarExample())
    page.add(buildIndeterminateProgressBarExample())
    page.add(buildProgressBarStateExample())
    return page
}

/** Determinate progress: drag a slider to move value and reflect it on the bar. */
private fun buildDeterminateProgressBarExample(): WComponent {
    val progressBar = WProgressBar(value = 40.0)
    progressBar.width = 300.0

    val label = WLabel("40%")
    val slider = WSlider(value = 40.0)
    slider.width = 300.0
    slider.addChangeListener { value ->
        progressBar.value = value
        label.text = "${value.toInt()}%"
    }

    val column = WPanel(spacing = 12.0)
    column.add(progressBar)
    column.add(slider)
    column.add(label)
    return buildExample("Determinate progress (Value / Minimum / Maximum)", column)
}

/** Indeterminate progress: indicates a task whose completion time is unknown. */
private fun buildIndeterminateProgressBarExample(): WComponent {
    val progressBar = WProgressBar()
    progressBar.width = 300.0
    progressBar.isIndeterminate = true

    val toggleButton = WButton("Stop")
    toggleButton.addActionListener {
        progressBar.isIndeterminate = !progressBar.isIndeterminate
        toggleButton.text = if (progressBar.isIndeterminate) "Stop" else "Start"
    }

    val column = WPanel(spacing = 12.0)
    column.add(progressBar)
    column.add(toggleButton.also { it.horizontalAlignment = HorizontalAlignment.LEFT })
    return buildExample("Indeterminate progress (IsIndeterminate)", column)
}

/** Visual states: toggle the showError / showPaused visual states. */
private fun buildProgressBarStateExample(): WComponent {
    val progressBar = WProgressBar(value = 60.0)
    progressBar.width = 300.0

    val pauseButton = WButton("Pause")
    pauseButton.addActionListener {
        progressBar.showPaused = !progressBar.showPaused
        pauseButton.text = if (progressBar.showPaused) "Resume" else "Pause"
    }

    val errorButton = WButton("Error")
    errorButton.addActionListener {
        progressBar.showError = !progressBar.showError
        errorButton.text = if (progressBar.showError) "Clear error" else "Error"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(pauseButton)
    buttons.add(errorButton)

    val column = WPanel(spacing = 12.0)
    column.add(progressBar)
    column.add(buttons)
    return buildExample("Visual states (ShowPaused / ShowError)", column)
}

// endregion

// region ProgressRing

/** The ProgressRing page: lines up demos for trying out WProgressRing's various features. */
internal fun buildProgressRingPage(): WComponent {
    val page = buildPage(
        "ProgressRing",
        "A control that shows an ongoing operation or progress as a circular ring. Try out WProgressRing's various features.",
    )

    page.add(buildIndeterminateProgressRingExample())
    page.add(buildDeterminateProgressRingExample())
    return page
}

/** Indeterminate progress: toggle the spinning animation with IsActive. */
private fun buildIndeterminateProgressRingExample(): WComponent {
    val progressRing = WProgressRing()
    progressRing.width = 60.0
    progressRing.height = 60.0
    progressRing.isActive = true

    val toggleButton = WButton("Stop")
    toggleButton.addActionListener {
        progressRing.isActive = !progressRing.isActive
        toggleButton.text = if (progressRing.isActive) "Stop" else "Start"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(progressRing)
    row.add(toggleButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Indeterminate progress (IsActive / IsIndeterminate)", row)
}

/** Determinate progress: drag a slider to move value and reflect it on the ring. */
private fun buildDeterminateProgressRingExample(): WComponent {
    val progressRing = WProgressRing()
    progressRing.width = 60.0
    progressRing.height = 60.0
    progressRing.isIndeterminate = false
    progressRing.value = 40.0

    val label = WLabel("40%")
    val slider = WSlider(value = 40.0)
    slider.width = 300.0
    slider.addChangeListener { value ->
        progressRing.value = value
        label.text = "${value.toInt()}%"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(progressRing)
    val column = WPanel(spacing = 12.0)
    column.add(slider)
    column.add(label)
    row.add(column.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Determinate progress (IsIndeterminate = false / Value)", row)
}

// endregion

// region ToolTip

/** The ToolTip page: lines up demos for trying out WComponent.toolTip / WToolTip's various features. */
internal fun buildToolTipPage(): WComponent {
    val page = buildPage(
        "ToolTip",
        "Shows a floating explanation of an element on hover. Try out the toolTip property and WToolTip.",
    )

    page.add(buildSimpleToolTipExample())
    page.add(buildPlacementToolTipExample())
    page.add(buildRichToolTipExample())
    return page
}

/** A string hint: hover over the toolTip property to see the explanation. */
private fun buildSimpleToolTipExample(): WComponent {
    val button = WButton("Hover over me")
    button.toolTip = "This is a plain string tooltip."
    return buildExample("A string hint (WComponent.toolTip)", button)
}

/** Specifying placement: choose WToolTip's placement from top / bottom / left / right. */
private fun buildPlacementToolTipExample(): WComponent {
    val row = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    val placements = listOf(
        "Top" to ToolTipPlacement.TOP,
        "Bottom" to ToolTipPlacement.BOTTOM,
        "Left" to ToolTipPlacement.LEFT,
        "Right" to ToolTipPlacement.RIGHT,
    )
    for ((label, placement) in placements) {
        val button = WButton(label)
        val toolTip = WToolTip()
        toolTip.text = "Shown to the $label"
        toolTip.placement = placement
        button.setToolTip(toolTip)
        row.add(button)
    }
    return buildExample("Specifying placement (WToolTip.placement)", row)
}

/** A non-string hint: put an arbitrary component in WToolTip. */
private fun buildRichToolTipExample(): WComponent {
    val button = WButton("Rich hint")

    val content = WPanel(spacing = 4.0)
    content.add(WLabel("Heading").also { it.fontWeight = 600 })
    content.add(WLabel("The explanation can span multiple lines.").also { it.foreground = TEXT_SECONDARY })

    val toolTip = WToolTip()
    toolTip.content = content
    button.setToolTip(toolTip)
    return buildExample("A non-string hint (WToolTip.content)", button)
}

// endregion
