package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.ContentDialogButton;
import com.appkitbox.winui4k.ContentDialogResult;
import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WContentDialog;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WNavigationView;
import com.appkitbox.winui4k.WPanel;

import java.util.Arrays;
import java.util.function.Consumer;


/*
 * The Settings page: a reproduction of the real WinUI 3 Gallery's SettingsPage
 * (the real one's Sound settings and About section are out of scope here).
 * The real one uses the Community Toolkit's SettingsCard, which winui4k doesn't have, so
 * {@link #buildSettingsCard} builds the same layout (an icon on the left, a heading / description, and a control on the right).
 */
final class SettingsPage {
    private SettingsPage() {
    }

    /** The name that represents the Settings page in the back history (a value that does not collide with the page names in {@link GalleryNavigation#pages}). */
    static final String SETTINGS_PAGE_NAME = "Settings";

    static WComponent buildSettingsPage(WNavigationView navigationView, Consumer<String> applyAppTheme) {
        WPanel page = new WPanel();
        page.setMaxWidth(1064.0); // the real SettingsPage's MaxWidth

        WLabel titleLabel = new WLabel(SETTINGS_PAGE_NAME);
        titleLabel.setFontSize(28.0);
        titleLabel.setFontWeight(600);
        page.add(titleLabel);

        // Equivalent to the real SettingsSectionHeaderTextBlockStyle (BodyStrong + Margin="1,30,0,6")
        WLabel sectionLabel = new WLabel("Appearance & behavior");
        sectionLabel.setFontWeight(600);
        sectionLabel.setMargin(1.0, 30.0, 0.0, 6.0);
        page.add(sectionLabel);

        WPanel cards = new WPanel(4.0); // the real SettingsCardSpacing
        cards.add(buildAppThemeCard(applyAppTheme));
        cards.add(buildNavigationStyleCard(navigationView));
        cards.add(buildManageSamplesCard());
        page.add(cards);
        return page;
    }

    /** App theme: choosing the app theme (Light / Dark / Use system setting). */
    private static WComponent buildAppThemeCard(Consumer<String> applyAppTheme) {
        WComboBox themeCombo = new WComboBox(Arrays.asList("Light", "Dark", "Use system setting"));
        String currentTheme = GallerySettings.getAppTheme();
        themeCombo.setSelectedIndex(
            "Light".equals(currentTheme) ? 0 : "Dark".equals(currentTheme) ? 1 : 2);
        themeCombo.addListSelectionListener(() -> {
            int selectedIndex = themeCombo.getSelectedIndex();
            String appTheme = selectedIndex == 0 ? "Light" : selectedIndex == 1 ? "Dark" : "Default";
            GallerySettings.setAppTheme(appTheme);
            applyAppTheme.accept(appTheme);
        });
        return buildSettingsCard(
            "\uE790",
            "App theme",
            "Select which app theme to display",
            themeCombo);
    }

    /** Navigation style: where the navigation pane is placed (Left / Top). */
    private static WComponent buildNavigationStyleCard(WNavigationView navigationView) {
        WComboBox locationCombo = new WComboBox(Arrays.asList("Left", "Top"));
        locationCombo.setSelectedIndex("Top".equals(GallerySettings.getNavigationStyle()) ? 1 : 0);
        locationCombo.addListSelectionListener(() -> {
            boolean isLeft = locationCombo.getSelectedIndex() == 0;
            GallerySettings.setNavigationStyle(isLeft ? "Left" : "Top");
            // The real one also uses Auto for Left (auto-switches among Left-family modes by width)
            navigationView.setPaneDisplayMode(
                isLeft ? NavigationViewPaneDisplayMode.AUTO : NavigationViewPaneDisplayMode.TOP);
        });
        return buildSettingsCard(
            "\uF594",
            "Navigation style",
            null,
            locationCombo);
    }

    /** Manage samples: clears the recently-visited/favorites history behind a confirmation dialog. */
    private static WComponent buildManageSamplesCard() {
        WButton clearRecentButton = new WButton("Clear recents");
        WButton removeFavoritesButton = new WButton("Remove favorites");

        // Equivalent to the real CheckRecentAndFavoriteButtonStates: disable the button when its target is empty
        Runnable updateButtonStates = () -> {
            clearRecentButton.setEnabled(!GallerySettings.getRecentlyVisited().isEmpty());
            removeFavoritesButton.setEnabled(!GallerySettings.getFavorites().isEmpty());
        };
        updateButtonStates.run();

        clearRecentButton.addActionListener(() -> {
            WContentDialog dialog = new WContentDialog(
                "Clear recently visited samples?",
                new WLabel("This will remove all samples from your recent history."));
            dialog.setPrimaryButtonText("Clear");
            dialog.setCloseButtonText("Cancel");
            dialog.setDefaultButton(ContentDialogButton.PRIMARY);
            dialog.show(clearRecentButton, result -> {
                if (result == ContentDialogResult.PRIMARY) {
                    GallerySettings.clearRecentlyVisited();
                    updateButtonStates.run();
                }
            });
        });

        removeFavoritesButton.addActionListener(() -> {
            WContentDialog dialog = new WContentDialog(
                "Remove all favorites?",
                new WLabel("This will unfavorite all your samples."));
            dialog.setPrimaryButtonText("Remove");
            dialog.setCloseButtonText("Cancel");
            dialog.setDefaultButton(ContentDialogButton.PRIMARY);
            dialog.show(removeFavoritesButton, result -> {
                if (result == ContentDialogResult.PRIMARY) {
                    GallerySettings.clearFavorites();
                    updateButtonStates.run();
                }
            });
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(clearRecentButton);
        buttons.add(removeFavoritesButton);
        return buildSettingsCard(
            "\uE8A9",
            "Manage samples",
            "Clear your recent or favorite samples",
            buttons);
    }

    /**
     * A single-row card equivalent to SettingsCard:
     * a Segoe Fluent Icons {@code glyph} on the left, {@code header} and an optional {@code description} in the middle, and {@code control} at the right edge.
     */
    private static WComponent buildSettingsCard(
        String glyph,
        String header,
        String description,
        WComponent control
    ) {
        WGrid grid = new WGrid(0.0, 16.0);
        grid.addColumn(GridLength.Companion.getAUTO());
        grid.addColumn(GridLength.Companion.star(1.0));
        grid.addColumn(GridLength.Companion.getAUTO());

        WLabel icon = new WLabel(glyph);
        icon.setFontFamily("Segoe Fluent Icons");
        icon.setFontSize(20.0);
        icon.setVerticalAlignment(VerticalAlignment.CENTER);
        grid.add(icon, 0, 0, 1, 1);

        WPanel labels = new WPanel(2.0);
        labels.setVerticalAlignment(VerticalAlignment.CENTER);
        WLabel headerLabel = new WLabel(header);
        headerLabel.setTextWrapping(TextWrapping.WRAP);
        labels.add(headerLabel);
        if (description != null) {
            WLabel descriptionLabel = new WLabel(description);
            descriptionLabel.setFontSize(12.0);
            descriptionLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
            descriptionLabel.setTextWrapping(TextWrapping.WRAP);
            labels.add(descriptionLabel);
        }
        grid.add(labels, 0, 1, 1, 1);

        control.setVerticalAlignment(VerticalAlignment.CENTER);
        grid.add(control, 0, 2, 1, 1);

        WBorder card = new WBorder(grid);
        card.setBackground(GalleryTheme.CARD_BACKGROUND());
        card.setBorderColor(GalleryTheme.CARD_BORDER());
        card.setBorderThickness(1.0);
        card.setCornerRadius(4.0); // the real SettingsCard uses ControlCornerRadius (4px)
        card.setPadding(16.0);
        return card;
    }
}
