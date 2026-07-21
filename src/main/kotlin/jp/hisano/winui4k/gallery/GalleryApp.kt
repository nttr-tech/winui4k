package jp.hisano.winui4k.gallery

import jp.hisano.winui4k.swing.Orientation
import jp.hisano.winui4k.swing.WButton
import jp.hisano.winui4k.swing.WCommand
import jp.hisano.winui4k.swing.WComponent
import jp.hisano.winui4k.swing.WFlyout
import jp.hisano.winui4k.swing.WFrame
import jp.hisano.winui4k.swing.WLabel
import jp.hisano.winui4k.swing.WPanel
import jp.hisano.winui4k.winui.WinUiToolkit

/**
 * A WinUI 3 Gallery-style component gallery.
 * Shows a page navigation list on the left and the selected component's demo page on the right.
 * Currently only the Button page exists (add more to [pages] as pages are added).
 */
fun main() {
    WinUiToolkit.launch {
        val frame = WFrame(title = "WinUI4K Gallery")

        val root = WPanel(spacing = 32.0, orientation = Orientation.HORIZONTAL)
        root.add(buildNavigationPane())
        root.add(buildButtonPage())

        frame.add(root)
        frame.isVisible = true
    }
}

/** The left-hand navigation. Add buttons here as more pages are added. */
private fun buildNavigationPane(): WComponent {
    val pages = listOf("Button")

    val pane = WPanel(spacing = 8.0)
    pane.add(WLabel("WinUI4K Gallery").also { it.fontSize = 20.0 })
    for (page in pages) {
        pane.add(WButton(page).also { it.width = 160.0 })
    }
    return pane
}

/** The Button page: lines up demos for trying out WButton's various features. */
private fun buildButtonPage(): WComponent {
    val page = WPanel(spacing = 24.0)
    page.add(WLabel("Button").also { it.fontSize = 28.0 })
    page.add(WLabel("A button that responds to clicks. Try out WButton's various features."))

    page.add(buildSimpleButtonExample())
    page.add(buildFlyoutButtonExample())
    page.add(buildCommandButtonExample())
    return page
}

/** One demo section (heading + body). */
private fun buildExample(title: String, body: WComponent): WComponent {
    val section = WPanel(spacing = 8.0)
    section.add(WLabel(title).also { it.fontSize = 16.0 })
    section.add(body)
    return section
}

/** A basic button: responding to clicks and toggling isEnabled. */
private fun buildSimpleButtonExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val standardButton = WButton("Standard XAML Button")
    standardButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val toggleButton = WButton("Disable button")
    toggleButton.addActionListener {
        standardButton.isEnabled = !standardButton.isEnabled
        toggleButton.text = if (standardButton.isEnabled) "Disable button" else "Enable button"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(standardButton)
    row.add(toggleButton)
    row.add(result)
    return buildExample("Simple button", row)
}

/** A button with a flyout: opens a popup on click. */
private fun buildFlyoutButtonExample(): WComponent {
    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)

    flyoutContent.add(WLabel("Delete all items?"))
    flyoutContent.add(
        WButton("Yes, delete all").also { button ->
            button.addActionListener { flyout.hide() }
        },
    )

    val flyoutButton = WButton("Show options")
    flyoutButton.flyout = flyout
    return buildExample("Button with a flyout", flyoutButton)
}

/** A button with a WCommand: running the command and auto-disabling via isEnabled. */
private fun buildCommandButtonExample(): WComponent {
    val result = WLabel("Command has not run yet")
    val command = WCommand { parameter ->
        result.text = "Command ran (parameter = $parameter)"
    }

    val commandButton = WButton("Run command")
    commandButton.command = command
    commandButton.commandParameter = "Gallery"

    val toggleButton = WButton("Disable command")
    toggleButton.addActionListener {
        command.isEnabled = !command.isEnabled
        toggleButton.text = if (command.isEnabled) "Disable command" else "Enable command"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(commandButton)
    row.add(toggleButton)
    row.add(result)
    return buildExample("Button with a command", row)
}
