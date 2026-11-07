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

/*
 * Status & info category: demo pages for InfoBadge / InfoBar / ProgressBar / ProgressRing / ToolTip.
 */

// region InfoBadge

/** The InfoBadge page: lines up demos for trying out WInfoBadge's various features. */
internal fun buildInfoBadgePage(): WComponent {
    val page = buildPage(
        "InfoBadge",
        Messages.get("infobadge.description"),
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
    row.add(labeledColumn(Messages.get("infobadge.kinds.dot"), dot))
    row.add(labeledColumn(Messages.get("infobadge.kinds.number"), number))
    row.add(labeledColumn(Messages.get("infobadge.kinds.icon"), icon))
    return buildExample(Messages.get("infobadge.example.kinds.title"), row)
}

/** A dynamic badge: increment/reset the number with buttons. */
private fun buildInfoBadgeDynamicExample(): WComponent {
    val badge = WInfoBadge()
    badge.value = 0

    val addButton = WButton("+1")
    addButton.addActionListener { badge.value += 1 }

    val clearButton = WButton(Messages.get("infobadge.dynamic.reset"))
    clearButton.addActionListener { badge.value = 0 }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(labeledColumn(Messages.get("infobadge.dynamic.unread"), badge))
    row.add(addButton.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    row.add(clearButton.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("infobadge.example.dynamic.title"), row)
}

/** A small column that stacks a control under a label (used by the badge demos). */
private fun labeledColumn(label: String, control: WComponent): WComponent {
    val column = WPanel(spacing = 6.0)
    column.horizontalAlignmentInParent = HorizontalAlignment.LEFT
    column.add(WLabel(label).also { it.foreground = TEXT_SECONDARY })
    column.add(control.also { it.horizontalAlignmentInParent = HorizontalAlignment.LEFT })
    return column
}

// endregion

// region InfoBar

/** The InfoBar page: lines up demos for trying out WInfoBar's various features. */
internal fun buildInfoBarPage(): WComponent {
    val page = buildPage(
        "InfoBar",
        Messages.get("infobar.description"),
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
        InfoBarSeverity.INFORMATIONAL to (Messages.get("infobar.severity.informational.title") to Messages.get("infobar.severity.informational.message")),
        InfoBarSeverity.SUCCESS to (Messages.get("infobar.severity.success.title") to Messages.get("infobar.severity.success.message")),
        InfoBarSeverity.WARNING to (Messages.get("infobar.severity.warning.title") to Messages.get("infobar.severity.warning.message")),
        InfoBarSeverity.ERROR to (Messages.get("infobar.severity.error.title") to Messages.get("infobar.severity.error.message")),
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
    return buildExample(Messages.get("infobar.example.severity.title"), column)
}

/** Opening and closing: toggle isOpen with a button and receive close (x) button clicks. */
private fun buildInfoBarInteractiveExample(): WComponent {
    val result = WLabel(Messages.get("infobar.interactive.status.closed"))

    val infoBar = WInfoBar()
    infoBar.severity = InfoBarSeverity.SUCCESS
    infoBar.title = Messages.get("infobar.interactive.title")
    infoBar.message = Messages.get("infobar.interactive.message")
    infoBar.width = 500.0
    infoBar.addCloseButtonListener { result.text = Messages.get("infobar.interactive.status.closebutton") }

    val toggleButton = WButton(Messages.get("infobar.interactive.open"))
    toggleButton.addActionListener {
        infoBar.isOpen = !infoBar.isOpen
        toggleButton.text = if (infoBar.isOpen) Messages.get("infobar.interactive.close") else Messages.get("infobar.interactive.open")
        result.text = if (infoBar.isOpen) Messages.get("infobar.interactive.status.open") else Messages.get("infobar.interactive.status.closed")
    }

    val column = WPanel(spacing = 12.0)
    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    column.add(row)
    column.add(infoBar)
    return buildExample(Messages.get("infobar.example.interactive.title"), column)
}

/** An action button: places an action button next to the body text. */
private fun buildInfoBarActionExample(): WComponent {
    val result = WLabel("")

    val actionButton = WButton(Messages.get("infobar.action.restart"))
    actionButton.addActionListener { result.text = Messages.get("infobar.action.status") }

    val infoBar = WInfoBar()
    infoBar.severity = InfoBarSeverity.WARNING
    infoBar.title = Messages.get("infobar.action.title")
    infoBar.message = Messages.get("infobar.action.message")
    infoBar.actionButton = actionButton
    infoBar.isClosable = false
    infoBar.isOpen = true
    infoBar.width = 500.0

    val column = WPanel(spacing = 12.0)
    column.add(infoBar)
    column.add(result)
    return buildExample(Messages.get("infobar.example.action.title"), column)
}

// endregion

// region ProgressBar

/** The ProgressBar page: lines up demos for trying out WProgressBar's various features. */
internal fun buildProgressBarPage(): WComponent {
    val page = buildPage(
        "ProgressBar",
        Messages.get("progressbar.description"),
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
    return buildExample(Messages.get("progressbar.example.determinate.title"), column)
}

/** Indeterminate progress: indicates a task whose completion time is unknown. */
private fun buildIndeterminateProgressBarExample(): WComponent {
    val progressBar = WProgressBar()
    progressBar.width = 300.0
    progressBar.isIndeterminate = true

    val toggleButton = WButton(Messages.get("progressbar.stop"))
    toggleButton.addActionListener {
        progressBar.isIndeterminate = !progressBar.isIndeterminate
        toggleButton.text = if (progressBar.isIndeterminate) Messages.get("progressbar.stop") else Messages.get("progressbar.start")
    }

    val column = WPanel(spacing = 12.0)
    column.add(progressBar)
    column.add(toggleButton.also { it.horizontalAlignmentInParent = HorizontalAlignment.LEFT })
    return buildExample(Messages.get("progressbar.example.indeterminate.title"), column)
}

/** Visual states: toggle the showError / showPaused visual states. */
private fun buildProgressBarStateExample(): WComponent {
    val progressBar = WProgressBar(value = 60.0)
    progressBar.width = 300.0

    val pauseButton = WButton(Messages.get("progressbar.pause"))
    pauseButton.addActionListener {
        progressBar.showPaused = !progressBar.showPaused
        pauseButton.text = if (progressBar.showPaused) Messages.get("progressbar.resume") else Messages.get("progressbar.pause")
    }

    val errorButton = WButton(Messages.get("progressbar.error"))
    errorButton.addActionListener {
        progressBar.showError = !progressBar.showError
        errorButton.text = if (progressBar.showError) Messages.get("progressbar.clearerror") else Messages.get("progressbar.error")
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(pauseButton)
    buttons.add(errorButton)

    val column = WPanel(spacing = 12.0)
    column.add(progressBar)
    column.add(buttons)
    return buildExample(Messages.get("progressbar.example.state.title"), column)
}

// endregion

// region ProgressRing

/** The ProgressRing page: lines up demos for trying out WProgressRing's various features. */
internal fun buildProgressRingPage(): WComponent {
    val page = buildPage(
        "ProgressRing",
        Messages.get("progressring.description"),
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

    val toggleButton = WButton(Messages.get("progressring.stop"))
    toggleButton.addActionListener {
        progressRing.isActive = !progressRing.isActive
        toggleButton.text = if (progressRing.isActive) Messages.get("progressring.stop") else Messages.get("progressring.start")
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(progressRing)
    row.add(toggleButton.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("progressring.example.indeterminate.title"), row)
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
    row.add(column.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("progressring.example.determinate.title"), row)
}

// endregion

// region ToolTip

/** The ToolTip page: lines up demos for trying out WComponent.toolTipText / WComponent.toolTip (WToolTip)'s various features. */
internal fun buildToolTipPage(): WComponent {
    val page = buildPage(
        "ToolTip",
        Messages.get("tooltip.description"),
    )

    page.add(buildSimpleToolTipExample())
    page.add(buildPlacementToolTipExample())
    page.add(buildRichToolTipExample())
    return page
}

/** A string hint: hover over the toolTip property to see the explanation. */
private fun buildSimpleToolTipExample(): WComponent {
    val button = WButton(Messages.get("tooltip.simple.button"))
    button.toolTipText = Messages.get("tooltip.simple.tip")
    return buildExample(Messages.get("tooltip.example.simple.title"), button)
}

/** Specifying placement: choose WToolTip's placement from top / bottom / left / right. */
private fun buildPlacementToolTipExample(): WComponent {
    val row = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    val placements = listOf(
        Triple(Messages.get("tooltip.placement.top"), Messages.get("tooltip.placement.top.tip"), ToolTipPlacement.TOP),
        Triple(Messages.get("tooltip.placement.bottom"), Messages.get("tooltip.placement.bottom.tip"), ToolTipPlacement.BOTTOM),
        Triple(Messages.get("tooltip.placement.left"), Messages.get("tooltip.placement.left.tip"), ToolTipPlacement.LEFT),
        Triple(Messages.get("tooltip.placement.right"), Messages.get("tooltip.placement.right.tip"), ToolTipPlacement.RIGHT),
    )
    for ((label, tipText, placement) in placements) {
        val button = WButton(label)
        val toolTip = WToolTip()
        toolTip.text = tipText
        toolTip.placement = placement
        button.toolTip = toolTip
        row.add(button)
    }
    return buildExample(Messages.get("tooltip.example.placement.title"), row)
}

/** A non-string hint: put an arbitrary component in WToolTip. */
private fun buildRichToolTipExample(): WComponent {
    val button = WButton(Messages.get("tooltip.rich.button"))

    val content = WPanel(spacing = 4.0)
    content.add(WLabel(Messages.get("tooltip.rich.heading")).also { it.fontWeight = 600 })
    content.add(WLabel(Messages.get("tooltip.rich.body")).also { it.foreground = TEXT_SECONDARY })

    val toolTip = WToolTip()
    toolTip.content = content
    button.toolTip = toolTip
    return buildExample(Messages.get("tooltip.example.rich.title"), button)
}

// endregion
