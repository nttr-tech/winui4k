package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ContentDialogButton
import com.appkitbox.winui4k.ContentDialogResult
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WPanel

/**
 * The Settings page: a reproduction of the real WinUI 3 Gallery's SettingsPage
 * (the real one's Sound settings and About section are out of scope here).
 * The real one uses the Community Toolkit's SettingsCard, which winui4k doesn't have, so
 * [buildSettingsCard] builds the same layout (a left icon + heading/description + a control on the right).
 */

/** The name representing the Settings page in the back history (a value that doesn't collide with any [pages] page name). */
internal const val SETTINGS_PAGE_NAME = "Settings"

internal fun buildSettingsPage(
    navigationView: WNavigationView,
    applyAppTheme: (String) -> Unit,
): WComponent {
    val page = WPanel()
    page.maxWidth = 1064.0 // the real SettingsPage's MaxWidth

    page.add(WLabel(SETTINGS_PAGE_NAME).also { it.fontSize = 28.0; it.fontWeight = 600 })

    // Equivalent to the real SettingsSectionHeaderTextBlockStyle (BodyStrong + Margin="1,30,0,6")
    page.add(
        WLabel("Appearance & behavior").also {
            it.fontWeight = 600
            it.setMargin(1.0, 30.0, 0.0, 6.0)
        },
    )

    val cards = WPanel(spacing = 4.0) // the real SettingsCardSpacing
    cards.add(buildAppThemeCard(applyAppTheme))
    cards.add(buildNavigationStyleCard(navigationView))
    cards.add(buildManageSamplesCard())
    page.add(cards)
    return page
}

/** App theme: choosing the app theme (Light / Dark / Use system setting). */
private fun buildAppThemeCard(applyAppTheme: (String) -> Unit): WComponent {
    val themeCombo = WComboBox(listOf("Light", "Dark", "Use system setting"))
    themeCombo.selectedIndex = when (GallerySettings.appTheme) {
        "Light" -> 0
        "Dark" -> 1
        else -> 2
    }
    themeCombo.addListSelectionListener {
        val appTheme = when (themeCombo.selectedIndex) {
            0 -> "Light"
            1 -> "Dark"
            else -> "Default"
        }
        GallerySettings.appTheme = appTheme
        applyAppTheme(appTheme)
    }
    return buildSettingsCard(
        glyph = "",
        header = "App theme",
        description = "Select which app theme to display",
        control = themeCombo,
    )
}

/** Navigation style: where the navigation pane is placed (Left / Top). */
private fun buildNavigationStyleCard(navigationView: WNavigationView): WComponent {
    val locationCombo = WComboBox(listOf("Left", "Top"))
    locationCombo.selectedIndex = if (GallerySettings.navigationStyle == "Top") 1 else 0
    locationCombo.addListSelectionListener {
        val isLeft = locationCombo.selectedIndex == 0
        GallerySettings.navigationStyle = if (isLeft) "Left" else "Top"
        // The real one also uses Auto for Left (auto-switches among Left-family modes by width)
        navigationView.paneDisplayMode =
            if (isLeft) NavigationViewPaneDisplayMode.AUTO else NavigationViewPaneDisplayMode.TOP
    }
    return buildSettingsCard(
        glyph = "",
        header = "Navigation style",
        description = null,
        control = locationCombo,
    )
}

/** Manage samples: clears the recently-visited/favorites history behind a confirmation dialog. */
private fun buildManageSamplesCard(): WComponent {
    val clearRecentButton = WButton("Clear recents")
    val removeFavoritesButton = WButton("Remove favorites")

    // Equivalent to the real CheckRecentAndFavoriteButtonStates: disable the button when its target is empty
    fun updateButtonStates() {
        clearRecentButton.isEnabled = GallerySettings.recentlyVisited.isNotEmpty()
        removeFavoritesButton.isEnabled = GallerySettings.favorites.isNotEmpty()
    }
    updateButtonStates()

    clearRecentButton.addActionListener {
        val dialog = WContentDialog(
            "Clear recently visited samples?",
            WLabel("This will remove all samples from your recent history."),
        )
        dialog.primaryButtonText = "Clear"
        dialog.closeButtonText = "Cancel"
        dialog.defaultButton = ContentDialogButton.PRIMARY
        dialog.show(clearRecentButton) { result ->
            if (result == ContentDialogResult.PRIMARY) {
                GallerySettings.clearRecentlyVisited()
                updateButtonStates()
            }
        }
    }

    removeFavoritesButton.addActionListener {
        val dialog = WContentDialog(
            "Remove all favorites?",
            WLabel("This will unfavorite all your samples."),
        )
        dialog.primaryButtonText = "Remove"
        dialog.closeButtonText = "Cancel"
        dialog.defaultButton = ContentDialogButton.PRIMARY
        dialog.show(removeFavoritesButton) { result ->
            if (result == ContentDialogResult.PRIMARY) {
                GallerySettings.clearFavorites()
                updateButtonStates()
            }
        }
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(clearRecentButton)
    buttons.add(removeFavoritesButton)
    return buildSettingsCard(
        glyph = "",
        header = "Manage samples",
        description = "Clear your recent or favorite samples",
        control = buttons,
    )
}

/**
 * A single-row card equivalent to SettingsCard:
 * a Segoe Fluent Icons [glyph] on the left, [header] and an optional [description] in the middle,
 * and [control] on the right.
 */
private fun buildSettingsCard(
    glyph: String,
    header: String,
    description: String?,
    control: WComponent,
): WComponent {
    val grid = WGrid(columnSpacing = 16.0)
    grid.addColumn(GridLength.AUTO)
    grid.addColumn(GridLength.star())
    grid.addColumn(GridLength.AUTO)

    val icon = WLabel(glyph)
    icon.fontFamily = "Segoe Fluent Icons"
    icon.fontSize = 20.0
    icon.verticalAlignment = VerticalAlignment.CENTER
    grid.add(icon, row = 0, column = 0)

    val labels = WPanel(spacing = 2.0)
    labels.verticalAlignment = VerticalAlignment.CENTER
    labels.add(WLabel(header).also { it.textWrapping = TextWrapping.WRAP })
    if (description != null) {
        labels.add(
            WLabel(description).also {
                it.fontSize = 12.0
                it.foreground = TEXT_SECONDARY
                it.textWrapping = TextWrapping.WRAP
            },
        )
    }
    grid.add(labels, row = 0, column = 1)

    control.verticalAlignment = VerticalAlignment.CENTER
    grid.add(control, row = 0, column = 2)

    val card = WBorder(grid)
    card.background = CARD_BACKGROUND
    card.borderColor = CARD_BORDER
    card.borderThickness = 1.0
    card.cornerRadius = 4.0 // the real SettingsCard uses ControlCornerRadius (4px)
    card.padding = 16.0
    return card
}
