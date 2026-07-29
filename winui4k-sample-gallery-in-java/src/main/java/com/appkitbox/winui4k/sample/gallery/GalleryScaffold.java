package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WToggleButton;

final class GalleryScaffold {
    private GalleryScaffold() {
    }

    /** A page's skeleton (large heading + favorite star + description). Each page adds its demos onto this return value. */
    static WPanel buildPage(String title, String description) {
        WPanel titleRow = new WPanel(12.0, Orientation.HORIZONTAL);
        WLabel titleLabel = new WLabel(title);
        titleLabel.setFontSize(28.0);
        titleLabel.setFontWeight(600);
        titleRow.add(titleLabel);
        if (GalleryNavigation.pages.containsKey(title)) {
            WToggleButton favorite = HomePage.buildFavoriteToggle(title);
            favorite.setVerticalAlignment(VerticalAlignment.CENTER);
            titleRow.add(favorite);
        }

        WPanel header = new WPanel(4.0);
        header.add(titleRow);
        WLabel descriptionLabel = new WLabel(description);
        descriptionLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
        descriptionLabel.setTextWrapping(TextWrapping.WRAP);
        header.add(descriptionLabel);

        WPanel page = new WPanel(24.0);
        page.add(header);
        return page;
    }

    /** One demo section (heading + body placed on a card). */
    static WComponent buildExample(String title, WComponent body) {
        // Same as the official Gallery: the demo body is left-aligned rather than stretched to the card width
        body.setHorizontalAlignment(HorizontalAlignment.LEFT);

        WBorder card = new WBorder(body);
        card.setBackground(GalleryTheme.CARD_BACKGROUND());
        card.setBorderColor(GalleryTheme.CARD_BORDER());
        card.setBorderThickness(1.0);
        card.setCornerRadius(8.0);
        card.setPadding(16.0);

        WPanel section = new WPanel(8.0);
        WLabel titleLabel = new WLabel(title);
        titleLabel.setFontWeight(600);
        titleLabel.setTextWrapping(TextWrapping.WRAP);
        section.add(titleLabel);
        section.add(card);
        return section;
    }

    /**
     * A demo section with an Options panel (equivalent to the real WinUI 3 Gallery's ControlExample).
     * Places the example body {@code example} on the left and {@code options}, which manipulates the properties, as a separate card on the right,
     * right as separate cards, so developers can try out the control's behavior on the spot.
     */
    static WComponent buildExample(String title, WComponent example, WComponent options) {
        // The example body. Aligned to the top-left rather than stretched to the card width (matching the single-argument buildExample)
        example.setHorizontalAlignment(HorizontalAlignment.LEFT);
        example.setVerticalAlignment(VerticalAlignment.TOP);

        WBorder exampleCard = new WBorder(example);
        exampleCard.setBackground(GalleryTheme.CARD_BACKGROUND());
        exampleCard.setBorderColor(GalleryTheme.CARD_BORDER());
        exampleCard.setBorderThickness(1.0);
        exampleCard.setCornerRadius(8.0);
        exampleCard.setPadding(16.0);

        // The Options card on the right. Stacks the property controls vertically, same as the official Gallery
        WBorder optionsCard = new WBorder(options);
        optionsCard.setBackground(GalleryTheme.CARD_BACKGROUND());
        optionsCard.setBorderColor(GalleryTheme.CARD_BORDER());
        optionsCard.setBorderThickness(1.0);
        optionsCard.setCornerRadius(8.0);
        optionsCard.setPadding(16.0);
        optionsCard.setWidth(280.0);
        optionsCard.setVerticalAlignment(VerticalAlignment.TOP);

        // Lay the example (fills remaining width) and Options (fixed width) side by side
        WGrid grid = new WGrid();
        grid.setColumnSpacing(12.0);
        grid.addColumn(GridLength.Companion.star(1.0));
        grid.addColumn(GridLength.Companion.getAUTO());
        grid.addRow(GridLength.Companion.getAUTO());
        grid.add(exampleCard, 0, 0, 1, 1);
        grid.add(optionsCard, 0, 1, 1, 1);

        WPanel section = new WPanel(8.0);
        WLabel titleLabel = new WLabel(title);
        titleLabel.setFontWeight(600);
        titleLabel.setTextWrapping(TextWrapping.WRAP);
        section.add(titleLabel);
        section.add(grid);
        return section;
    }

    /** The heading label for an Options panel (equivalent to the real Gallery's ComboBox/Slider Header; a muted text color). */
    static WLabel optionsLabel(String text) {
        WLabel label = new WLabel(text);
        label.setForeground(GalleryTheme.TEXT_SECONDARY());
        label.setTextWrapping(TextWrapping.WRAP);
        return label;
    }

    /** A colored tile for the layout demos (a Border painted with a background). Fills its parent if a size isn't given. */
    static WBorder buildTile(WColor color) {
        return buildTile(color, Double.NaN, Double.NaN, "");
    }

    /** A colored tile for the layout demos (a rectangle painted with a Border background). */
    static WBorder buildTile(WColor color, double width, double height) {
        return buildTile(color, width, height, "");
    }

    /** A colored tile for the layout demos (a Border painted with a background). Fills its parent if a size isn't given. */
    static WBorder buildTile(WColor color, double width, double height, String label) {
        WBorder tile = new WBorder();
        tile.setBackground(color);
        tile.setCornerRadius(4.0);
        if (!Double.isNaN(width)) {
            tile.setWidth(width);
        }
        if (!Double.isNaN(height)) {
            tile.setHeight(height);
        }
        if (!label.isEmpty()) {
            tile.setPadding(8.0);
            tile.setChild(new WLabel(label));
        }
        return tile;
    }
}
