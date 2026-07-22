package com.appkitbox.winui4k.gallery

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.ScrollBarVisibility
import com.appkitbox.winui4k.Stretch
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WImage
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLinearGradientPaint
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WToggleButton
import com.appkitbox.winui4k.WVariableSizedWrapGrid
import java.io.File
import java.util.prefs.Preferences

/**
 * Equivalent of the real WinUI 3 Gallery's Home page.
 * A header with a hero image, tiles linking out, and a Recent / Favorites toggle view below it.
 * "Recently visited" and "Favorites" are persisted via [GallerySettings].
 */

/** Persists recently-visited / favorite pages via Java Preferences (equivalent to the real Gallery's SettingsHelper). */
internal object GallerySettings {
    private const val MAX_RECENTLY_VISITED = 8

    private val preferences = Preferences.userRoot().node("com/appkitbox/winui4k/gallery")

    /** Page names can contain spaces, so store them newline-separated. */
    private fun load(key: String): List<String> =
        preferences.get(key, "").split('\n').filter { it.isNotEmpty() }

    private fun store(key: String, names: List<String>) =
        preferences.put(key, names.joinToString("\n"))

    /** Recently visited page names (newest first). */
    var recentlyVisited: List<String>
        get() = load("recentlyVisited")
        private set(value) = store("recentlyVisited", value)

    /** Page names registered as favorites (in registration order). */
    var favorites: List<String>
        get() = load("favorites")
        private set(value) = store("favorites", value)

    /** Records that a page was visited (moves it to the front, dropping the oldest entries once over the limit). */
    fun addRecentlyVisited(name: String) {
        recentlyVisited = (listOf(name) + (recentlyVisited - name)).take(MAX_RECENTLY_VISITED)
    }

    fun isFavorite(name: String): Boolean = name in favorites

    fun setFavorite(name: String, isFavorite: Boolean) {
        favorites = if (isFavorite) (favorites - name) + name else favorites - name
    }
}

/** The header's background color (a representative color from the real light theme's #CED8E4-#D5DBE3 gradient). */
private val HEADER_BACKGROUND = WColor(206, 216, 228)

/** Recently added/updated pages (equivalent to the real Gallery's IsNew / IsUpdated). Swap these out with each release. */
private val recentlyAddedOrUpdatedPages = listOf(
    "ListBox",
    "AppWindow",
    "AppWindowTitleBar",
    "Multiple windows",
    "SystemBackdrop",
    "TitleBar",
)

/** Descriptions shown on Home's cards for each page (equivalent to the real Gallery's ControlInfoData.json Description). */
internal val pageDescriptions: Map<String, String> = mapOf(
    "AppBarButton" to "An icon button placed on a command bar.",
    "AppBarSeparator" to "A vertical line that divides command bar buttons into groups.",
    "AppBarToggleButton" to "A button placed on a command bar that toggles on/off.",
    "AutoSuggestBox" to "A text box that shows suggestions as you type.",
    "AppNotification" to "Sends a toast notification that arrives in the action center.",
    "BadgeNotification" to "Shows a numeric or glyph badge on the taskbar icon.",
    "Border" to "A container that draws a border, background, and rounded corners around a single child.",
    "Button" to "A button that responds to clicks.",
    "Canvas" to "A panel that positions children with absolute coordinates.",
    "CheckBox" to "A check box that toggles between three states: on, off, and indeterminate.",
    "ColorPicker" to "A control for choosing a color via a spectrum or sliders.",
    "ComboBox" to "A control for choosing an item from a drop-down list.",
    "CommandBar" to "A toolbar that groups an app's commands together.",
    "CommandBarFlyout" to "Shows commands floating near the current selection.",
    "ContentDialog" to "A modal dialog that waits for a user response.",
    "DropDownButton" to "A button with a chevron that opens a menu on click.",
    "Expander" to "A control that expands/collapses its content when the header is clicked.",
    "Flyout" to "Shows content in a lightweight popup.",
    "Grid" to "A layout panel that places children into cells laid out in rows and columns.",
    "HyperlinkButton" to "A button styled as a link that responds to clicks.",
    "JumpList" to "Adds items to the taskbar's right-click menu.",
    "ListBox" to "A control for selecting an item from an always-visible list.",
    "ListView" to "A list that lines items up vertically for selection.",
    "MenuBar" to "A menu that runs along the top of a window.",
    "MenuFlyout" to "A transient menu such as a context menu.",
    "NavigationView" to "Provides an app's top-level navigation.",
    "NumberBox" to "Lets you enter a number and adjust it with spin buttons.",
    "PasswordBox" to "A password input field that hides the typed characters.",
    "Popup" to "Shows content floating at an arbitrary position.",
    "RadioButton" to "A button that lets you select just one option from a group.",
    "RatingControl" to "A control for entering and displaying a rating as a number of stars.",
    "RelativePanel" to "A panel that lays children out relative to one another.",
    "RepeatButton" to "A button that repeatedly fires its click event while held down.",
    "RichEditBox" to "An input field for editing formatted text.",
    "RichTextBlock" to "A control that displays formatted text.",
    "Slider" to "Lets you drag a thumb to choose a value within a range.",
    "SplitButton" to "A button that combines a default action with a menu.",
    "SplitView" to "A container that shows a pane alongside content.",
    "StackPanel" to "A panel that lines children up in a single vertical or horizontal row.",
    "StandardUICommand" to "Provides standard commands such as copy or delete.",
    "SwipeControl" to "A container that lets you run commands via a swipe gesture.",
    "TableView" to "A table that displays data in rows and columns.",
    "TeachingTip" to "Shows a hint that points to and explains a specific piece of UI.",
    "TextBlock" to "Displays read-only text.",
    "TextBox" to "Lets you enter single-line or multi-line text.",
    "ToggleButton" to "A button that toggles between an on and off state.",
    "ToggleSplitButton" to "A button that combines an on/off toggle with a menu.",
    "ToggleSwitch" to "A switch that toggles between on and off.",
    "TreeView" to "A tree that can expand and collapse hierarchical data.",
    "VariableSizedWrapGrid" to "A panel that wraps children in cell-sized units.",
    "XamlUICommand" to "A reusable command that carries a label and an icon.",
    "AppWindow" to "Controls a window's size, position, and presenter.",
    "AppWindowTitleBar" to "Customizes the title bar's colors and button region.",
    "Multiple windows" to "Creates and manages multiple windows.",
    "SystemBackdrop" to "Applies a background material such as Mica or Acrylic.",
    "TitleBar" to "A title bar that can host a back button and a search box.",
)

/** Cache of file URIs for images extracted from resources to a temp file (null if not found). */
private val extractedImageUris = mutableMapOf<String, String?>()

/**
 * Extracts a resource image (/images/[fileName]) to a temp file and returns its file URI.
 * WinUI's BitmapImage only accepts a URI (returns null if not found).
 */
private fun galleryImageUri(fileName: String): String? = extractedImageUris.getOrPut(fileName) {
    val resource = object {}.javaClass.getResourceAsStream("/images/$fileName")
        ?: return@getOrPut null
    val file = File.createTempFile("winui4k-gallery-", "-$fileName")
    file.deleteOnExit()
    resource.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    file.toPath().toUri().toString()
}

/** One of [buildHorizontalScroller]'s left/right scroll buttons (the real Gallery's ScrollButtonStyle 16x38 chevron). */
private fun buildScrollButton(glyph: String): WButton {
    val icon = WLabel(glyph)
    icon.fontFamily = "Segoe Fluent Icons"
    icon.fontSize = 8.0 // the real Gallery's FlipViewButtonFontSize (8, in generic.xaml)

    val button = WButton()
    button.content = icon
    button.padding = 0.0
    button.width = 16.0
    button.height = 38.0
    button.verticalAlignment = VerticalAlignment.CENTER
    button.margin = 8.0
    button.isVisible = false // stays hidden until overflow is detected
    return button
}

/**
 * Overlays left/right scroll buttons when a horizontal row of content overflows
 * (equivalent to the real Gallery's HorizontalScrollContainer). Doesn't show a scrollbar;
 * clicking a button animates a scroll by one viewport's worth.
 */
private fun buildHorizontalScroller(content: WComponent): WComponent {
    val scroller = WScrollPane(content)
    scroller.horizontalScrollBarVisibility = ScrollBarVisibility.HIDDEN
    scroller.verticalScrollBarVisibility = ScrollBarVisibility.DISABLED

    // Segoe Fluent Icons: EDD9 = ChevronLeftSmall, EDDA = ChevronRightSmall (same glyphs as the real Gallery)
    val backButton = buildScrollButton("")
    backButton.horizontalAlignment = HorizontalAlignment.LEFT
    val forwardButton = buildScrollButton("")
    forwardButton.horizontalAlignment = HorizontalAlignment.RIGHT

    // Equivalent to the real Gallery's Scroller_ViewChanging / UpdateScrollButtonsVisibility:
    // hide the back button at the left edge, and the forward button at the right edge
    fun updateButtons() {
        backButton.isVisible = scroller.horizontalOffset > 1
        forwardButton.isVisible = scroller.horizontalOffset < scroller.scrollableWidth - 1
    }
    scroller.addViewChangedListener { updateButtons() }
    scroller.addSizeChangedListener { updateButtons() }

    backButton.addActionListener {
        scroller.scrollToHorizontalOffset(scroller.horizontalOffset - scroller.viewportWidth)
    }
    forwardButton.addActionListener {
        scroller.scrollToHorizontalOffset(scroller.horizontalOffset + scroller.viewportWidth)
    }

    // Placing them in the same cell draws the buttons in front of the scrolled content
    val container = WGrid()
    container.addRow()
    container.add(scroller, row = 0, column = 0)
    container.add(backButton, row = 0, column = 0)
    container.add(forwardButton, row = 0, column = 0)
    return container
}

/** Gets a page name's card-icon image file name ("Multiple windows" -> "MultipleWindows.png"). */
private fun controlImageFileName(pageName: String): String = pageName.replace(" ", "") + ".png"

/** Opens a URL in the default browser. */
private fun openUrl(url: String) {
    ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
}

/**
 * Assembles the whole Home page.
 * [navigateTo] is the callback invoked with a page name when a card is clicked, to navigate to that page.
 */
internal fun buildHomePage(navigateTo: (String) -> Unit): WComponent {
    val page = WPanel(spacing = 0.0)
    page.add(buildHomeHeader())
    page.add(buildFilterSection(navigateTo))
    return page
}

/** Header: overlays the app name and external-link tiles on top of the hero image. */
private fun buildHomeHeader(): WComponent {
    // Fit the hero image to the area while preserving its aspect ratio, letting the background color show through faintly
    val hero = WImage(galleryImageUri("GalleryHeaderImage.png"))
    hero.stretch = Stretch.UNIFORM_TO_FILL
    hero.opacity = 0.9

    // The height matches the real OpacityMaskView's Height="400". Without a fixed height the cell would
    // grow to the image's natural size, and if it's shorter than the overlay (title + tiles ~= 370) the
    // image centers vertically and a band of the background color shows above the hero, so pin it to a
    // value larger than the overlay to cover it completely
    val heroBackground = WBorder(hero)
    heroBackground.background = HEADER_BACKGROUND
    heroBackground.height = 400.0

    // Equivalent to the real OpacityMaskView's mask (fades out at 0.75-0.85). Since an opacity mask isn't
    // available, overlay a gradient toward the composited color of the content area below (Mica + translucent
    // white ~= near white) on top of the hero. As in the real Gallery, fully blend it in the gap between the
    // link tiles (0.7-0.8 of the fixed 400 height = 280-320), staying opaque past that. The boundary's y only
    // depends on the hero's fixed height, so it doesn't move even if the window width changes how the image is cropped
    val heroFade = WBorder()
    heroFade.backgroundGradient = WLinearGradientPaint(
        listOf(
            0.0 to WColor(252, 252, 252, 0),
            0.7 to WColor(252, 252, 252, 0),
            0.8 to WColor(252, 252, 252),
            1.0 to WColor(252, 252, 252),
        ),
    )

    val titleBlock = WPanel(spacing = 0.0)
    titleBlock.add(WLabel("Windows App SDK").also { it.fontSize = 18.0 })
    titleBlock.add(WLabel("WinUI4K Gallery").also { it.fontSize = 40.0; it.fontWeight = 600 })

    val tiles = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    tiles.add(
        buildLinkTile(
            "Getting started",
            "See an overview of winui4k and WinUI in the docs.",
            imageFileName = "Header-WinUI.png",
            url = "https://github.com/hisano/winui4k#readme",
        ),
    )
    tiles.add(
        buildLinkTile(
            "Design",
            "Guidelines and toolkits for building beautiful WinUI experiences.",
            imageFileName = "Header-WindowsDesign.png",
            url = "https://learn.microsoft.com/windows/apps/design/",
        ),
    )
    tiles.add(
        buildLinkTile(
            "winui4k on GitHub",
            "View winui4k's source code and repository.",
            glyph = "", // Segoe Fluent Icons: Globe
            url = "https://github.com/hisano/winui4k",
        ),
    )
    tiles.add(
        buildLinkTile(
            "Community Toolkit",
            "A collection of helper functions, controls, and app services.",
            imageFileName = "Header-Toolkit.png",
            url = "https://apps.microsoft.com/store/detail/windows-community-toolkit-sample-app/9NBLGGH4TLCQ",
        ),
    )
    tiles.add(
        buildLinkTile(
            "Code samples",
            "Find samples that show specific tasks, features, and APIs.",
            glyph = "", // Segoe Fluent Icons: Code
            url = "https://learn.microsoft.com/windows/apps/get-started/samples",
        ),
    )
    tiles.add(
        buildLinkTile(
            "Partner Center",
            "Publish your app to the Microsoft Store.",
            imageFileName = "Header-Store.light.png",
            url = "https://developer.microsoft.com/windows/",
        ),
    )

    // The real spacing: the title block is Margin="36,48,0,0", and the tiles are 56 below the title
    val overlay = WPanel(spacing = 56.0)
    overlay.setMargin(36.0, 48.0, 36.0, 36.0)
    overlay.add(titleBlock)
    overlay.add(buildHorizontalScroller(tiles))

    // Placing children in the same cell draws the later-added one on top (overlays text on the image)
    val header = WGrid()
    header.addRow()
    header.add(heroBackground, row = 0, column = 0)
    header.add(heroFade, row = 0, column = 0)
    header.add(overlay, row = 0, column = 0)
    return header
}

/** One external-link tile. The icon is either an image ([imageFileName]) or a font glyph ([glyph]). */
private fun buildLinkTile(
    title: String,
    description: String,
    imageFileName: String? = null,
    glyph: String? = null,
    url: String,
): WComponent {
    val icon: WComponent = if (imageFileName != null) {
        WImage(galleryImageUri(imageFileName)).also {
            it.width = 36.0 // the real Gallery fills the whole icon row (height 36) with the image
            it.height = 36.0
            it.stretch = Stretch.UNIFORM
            it.horizontalAlignment = HorizontalAlignment.LEFT
        }
    } else {
        WLabel(glyph ?: "").also {
            it.fontFamily = "Segoe Fluent Icons"
            it.fontSize = 24.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
            it.setMargin(0.0, 8.0, 0.0, 0.0) // the real FontIcon's Margin="0,8,0,0"
        }
    }

    val texts = WPanel(spacing = 4.0)
    texts.add(WLabel(title).also { it.fontWeight = 600 })
    texts.add(
        WLabel(description).also {
            it.foreground = TEXT_SECONDARY
            it.fontSize = 12.0
            it.textWrapping = TextWrapping.WRAP
        },
    )
    texts.setMargin(0.0, 16.0, 0.0, 0.0) // equivalent to the real one's RowSpacing="16"

    // Equivalent to the real Tile's inner Grid (Padding="24", rows 36 / *). Fix the icon row's
    // height so the y position below the title lines up across every tile, whether the icon is
    // an image or a glyph
    val content = WGrid()
    content.addRow(GridLength.pixel(36.0))
    content.addRow(GridLength.star())
    content.add(icon, row = 0, column = 0)
    content.add(texts, row = 1, column = 0)
    content.margin = 24.0

    // Equivalent to the real Tile's bottom-right FontIcon (E8A7 = OpenInNewWindow), indicating it's an
    // external link. The real one uses Margin="-12" inside a Grid with Padding 24, so place it 12 in from the tile's edge
    val cornerIcon = WLabel("")
    cornerIcon.fontFamily = "Segoe Fluent Icons"
    cornerIcon.fontSize = 14.0
    cornerIcon.foreground = TEXT_SECONDARY
    cornerIcon.horizontalAlignment = HorizontalAlignment.RIGHT
    cornerIcon.verticalAlignment = VerticalAlignment.BOTTOM
    cornerIcon.margin = 12.0

    // Placing them in the same cell draws the icon at the body's bottom-right
    val body = WGrid()
    body.addRow()
    body.add(content, row = 0, column = 0)
    body.add(cornerIcon, row = 0, column = 0)

    val tile = WButton()
    tile.content = body
    // Spacing is managed via content / cornerIcon's margins (equivalent to the real HyperlinkButton's Padding="-1")
    tile.padding = 0.0
    // Left at the default (CENTER), the Grid wouldn't fill the whole button and the icon wouldn't reach the bottom-right
    tile.horizontalContentAlignment = HorizontalAlignment.STRETCH
    tile.verticalContentAlignment = VerticalAlignment.STRETCH
    // The same dimensions as the real Tile (232x172)
    tile.width = 232.0
    tile.height = 172.0
    tile.addActionListener { openUrl(url) }
    return tile
}

/**
 * The pill-shaped toggle that switches between Recent / Favorites (equivalent to the real Gallery's
 * SelectorBar TokenView style). Uses the same Symbol Clock / Favorite glyphs as the real Gallery.
 */
private fun buildFilterToggle(text: String, glyph: String): WToggleButton {
    val icon = WLabel(glyph)
    icon.fontFamily = "Segoe Fluent Icons"
    icon.fontSize = 16.0
    icon.verticalAlignment = VerticalAlignment.CENTER

    val label = WLabel(text)
    label.verticalAlignment = VerticalAlignment.CENTER

    val content = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    content.add(icon)
    content.add(label)

    val toggle = WToggleButton()
    toggle.content = content
    // The same padding as the real TokenView, plus a corner radius of half the height, for a pill shape
    toggle.setPadding(14.0, 5.0, 14.0, 6.0)
    toggle.cornerRadius = 16.0
    return toggle
}

/** The Recent / Favorites toggle and the card list below it. */
private fun buildFilterSection(navigateTo: (String) -> Unit): WComponent {
    val contentArea = WPanel(spacing = 0.0)

    // Segoe Fluent Icons: E121 = Clock (matches the real Gallery's SelectorBarItem Icon="Clock"), E113 = Favorite
    val recentToggle = buildFilterToggle("Recent", "")
    val favoritesToggle = buildFilterToggle("Favorites", "")

    fun select(showRecent: Boolean) {
        recentToggle.isChecked = showRecent
        favoritesToggle.isChecked = !showRecent
        contentArea.removeAll()
        contentArea.add(if (showRecent) buildRecentView(navigateTo) else buildFavoritesView(navigateTo))
    }
    recentToggle.addActionListener { select(showRecent = true) }
    favoritesToggle.addActionListener { select(showRecent = false) }
    select(showRecent = true)

    val toggles = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    toggles.add(recentToggle)
    toggles.add(favoritesToggle)
    toggles.horizontalAlignment = HorizontalAlignment.CENTER

    // The real spacing: the SelectorBar is Margin="36,24,0,16", the content below it is Margin="36,0,36,36"
    // (the 16 gap between the toggle and the content is expressed via spacing)
    val section = WPanel(spacing = 16.0)
    section.setMargin(36.0, 24.0, 36.0, 36.0)
    section.add(toggles)
    section.add(contentArea)
    return section
}

/** The Recent view: a row of recently-visited pages, plus a grid of recently added/updated pages. */
private fun buildRecentView(navigateTo: (String) -> Unit): WComponent {
    val view = WPanel(spacing = 12.0)

    // Don't show page names that no longer exist (equivalent to the real Gallery's GetValidItems)
    val recentlyVisited = GallerySettings.recentlyVisited.filter { it in pages }
    if (recentlyVisited.isNotEmpty()) {
        view.add(buildSectionTitle("Recently visited"))
        val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
        for (name in recentlyVisited) {
            row.add(buildControlCard(name, navigateTo))
        }
        // Same as the real Gallery: let horizontal scrolling reveal any overflow
        view.add(buildHorizontalScroller(row))
    }

    view.add(buildSectionTitle("Recently added or updated"))
    view.add(buildCardGrid(recentlyAddedOrUpdatedPages, navigateTo))
    return view
}

/** The Favorites view: a grid of favorites. Shows a fallback message if empty. */
private fun buildFavoritesView(navigateTo: (String) -> Unit): WComponent {
    val favorites = GallerySettings.favorites.filter { it in pages }
    if (favorites.isNotEmpty()) {
        return buildCardGrid(favorites, navigateTo)
    }

    val fallback = WPanel(spacing = 8.0)
    fallback.margin = 36.0
    fallback.add(
        WImage(galleryImageUri("RatingControl.png")).also {
            it.height = 36.0
            it.stretch = Stretch.UNIFORM
        },
    )
    fallback.add(
        WLabel("No favorites yet").also {
            it.fontWeight = 600
            it.horizontalAlignment = HorizontalAlignment.CENTER
        },
    )
    fallback.add(
        WLabel("Click the star icon on any page to have it show up here.").also {
            it.foreground = TEXT_SECONDARY
            it.horizontalAlignment = HorizontalAlignment.CENTER
        },
    )
    return fallback
}

/**
 * The favorite star placed next to a page heading (equivalent to the real Gallery's star icon on each sample page).
 * Syncs its checked state with [GallerySettings.favorites], reflecting it on Home's Favorites view.
 */
internal fun buildFavoriteToggle(pageName: String): WToggleButton {
    // Segoe Fluent Icons: E734 = star (outline), E735 = star (filled)
    val star = WLabel(if (GallerySettings.isFavorite(pageName)) "" else "")
    star.fontFamily = "Segoe Fluent Icons"
    star.fontSize = 14.0

    val toggle = WToggleButton()
    toggle.content = star
    toggle.isChecked = GallerySettings.isFavorite(pageName)
    toggle.addItemListener { checked ->
        val isFavorite = checked == true
        star.text = if (isFavorite) "" else ""
        GallerySettings.setFavorite(pageName, isFavorite)
    }
    return toggle
}

/** A section heading (e.g. "Recently visited"). */
private fun buildSectionTitle(title: String): WComponent =
    WLabel(title).also { it.fontSize = 16.0; it.fontWeight = 600 }

/** Lines cards up in a wrapping grid. */
private fun buildCardGrid(names: List<String>, navigateTo: (String) -> Unit): WComponent {
    val grid = WVariableSizedWrapGrid(itemWidth = 328.0, itemHeight = 96.0)
    grid.orientation = Orientation.HORIZONTAL
    for (name in names) {
        grid.add(buildControlCard(name, navigateTo))
    }
    return grid
}

/** A single page's card (icon + page name + description). Clicking it navigates to that page. */
private fun buildControlCard(name: String, navigateTo: (String) -> Unit): WComponent {
    val texts = WPanel(spacing = 2.0)
    texts.add(WLabel(name).also { it.fontWeight = 600 })
    texts.add(
        WLabel(pageDescriptions[name] ?: "").also {
            it.foreground = TEXT_SECONDARY
            it.fontSize = 12.0
            it.textWrapping = TextWrapping.WRAP
            // Inside a horizontal WPanel the width isn't fixed and wrapping won't kick in, so set it explicitly
            it.width = 235.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
        },
    )
    texts.verticalAlignment = VerticalAlignment.CENTER

    val content = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    content.add(
        WImage(galleryImageUri(controlImageFileName(name))).also {
            it.width = 36.0
            it.height = 36.0
            it.stretch = Stretch.UNIFORM
            it.verticalAlignment = VerticalAlignment.CENTER
        },
    )
    content.add(texts)
    content.horizontalAlignment = HorizontalAlignment.LEFT
    content.verticalAlignment = VerticalAlignment.CENTER

    val card = WButton()
    card.content = content
    card.width = 320.0
    card.height = 88.0
    card.addActionListener { navigateTo(name) }
    return card
}
