package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel

/** A page's skeleton (large heading + favorite star + description). Each page adds its demos onto this return value. */
internal fun buildPage(title: String, description: String): WPanel {
    val titleRow = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    titleRow.add(WLabel(title).also { it.fontSize = 28.0; it.fontWeight = 600 })
    if (title in pages) {
        titleRow.add(buildFavoriteToggle(title).also { it.verticalAlignment = VerticalAlignment.CENTER })
    }

    val header = WPanel(spacing = 4.0)
    header.add(titleRow)
    header.add(
        WLabel(description).also {
            it.foreground = TEXT_SECONDARY
            it.textWrapping = TextWrapping.WRAP
        },
    )

    val page = WPanel(spacing = 24.0)
    page.add(header)
    return page
}

/** One demo section (heading + body placed on a card). */
internal fun buildExample(title: String, body: WComponent): WComponent {
    // Like the real Gallery, keep the demo body from stretching to the card's
    // full width; left-align it instead
    body.horizontalAlignment = HorizontalAlignment.LEFT

    val card = WBorder(body)
    card.background = CARD_BACKGROUND
    card.borderColor = CARD_BORDER
    card.borderThickness = 1.0
    card.cornerRadius = 8.0
    card.padding = 16.0

    val section = WPanel(spacing = 8.0)
    section.add(WLabel(title).also { it.fontWeight = 600; it.textWrapping = TextWrapping.WRAP })
    section.add(card)
    return section
}

/**
 * A demo section with an Options panel (equivalent to the real WinUI 3 Gallery's ControlExample).
 * Places the example body [example] on the left and property-editing controls [options] on the
 * right as separate cards, so developers can try out the control's behavior on the spot.
 */
internal fun buildExample(title: String, example: WComponent, options: WComponent): WComponent {
    // The example body. Don't stretch it to the card's full width; align it to the top-left
    // (matches the single-argument buildExample overload)
    example.horizontalAlignment = HorizontalAlignment.LEFT
    example.verticalAlignment = VerticalAlignment.TOP

    val exampleCard = WBorder(example)
    exampleCard.background = CARD_BACKGROUND
    exampleCard.borderColor = CARD_BORDER
    exampleCard.borderThickness = 1.0
    exampleCard.cornerRadius = 8.0
    exampleCard.padding = 16.0

    // The Options card on the right. As in the real Gallery, lines up property-editing
    // controls vertically
    val optionsCard = WBorder(options)
    optionsCard.background = CARD_BACKGROUND
    optionsCard.borderColor = CARD_BORDER
    optionsCard.borderThickness = 1.0
    optionsCard.cornerRadius = 8.0
    optionsCard.padding = 16.0
    optionsCard.width = 280.0
    optionsCard.verticalAlignment = VerticalAlignment.TOP

    // Lay the example (fills remaining width) and Options (fixed width) side by side
    val grid = WGrid()
    grid.columnSpacing = 12.0
    grid.addColumn(GridLength.star())
    grid.addColumn(GridLength.AUTO)
    grid.addRow(GridLength.AUTO)
    grid.add(exampleCard, row = 0, column = 0)
    grid.add(optionsCard, row = 0, column = 1)

    val section = WPanel(spacing = 8.0)
    section.add(WLabel(title).also { it.fontWeight = 600; it.textWrapping = TextWrapping.WRAP })
    section.add(grid)
    return section
}

/** The heading label for an Options panel (equivalent to the real Gallery's ComboBox/Slider Header; a muted text color). */
internal fun optionsLabel(text: String): WLabel = WLabel(text).also {
    it.foreground = TEXT_SECONDARY
    it.textWrapping = TextWrapping.WRAP
}

/** A colored tile for the layout demos (a Border painted with a background). Fills its parent if a size isn't given. */
internal fun buildTile(color: WColor, width: Double = Double.NaN, height: Double = Double.NaN, label: String = ""): WBorder {
    val tile = WBorder()
    tile.background = color
    tile.cornerRadius = 4.0
    if (!width.isNaN()) tile.width = width
    if (!height.isNaN()) tile.height = height
    if (label.isNotEmpty()) {
        tile.padding = 8.0
        tile.child = WLabel(label)
    }
    return tile
}
