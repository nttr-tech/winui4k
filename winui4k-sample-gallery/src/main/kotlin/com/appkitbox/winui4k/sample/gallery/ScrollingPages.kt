package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.PipsPagerButtonVisibility
import com.appkitbox.winui4k.ScrollBarVisibility
import com.appkitbox.winui4k.ScrollingContentOrientation
import com.appkitbox.winui4k.ScrollingScrollBarVisibility
import com.appkitbox.winui4k.ScrollingZoomMode
import com.appkitbox.winui4k.Stretch
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WAnnotatedScrollBar
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WImage
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WList
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WPipsPager
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WScrollView
import com.appkitbox.winui4k.WSemanticZoom
import com.appkitbox.winui4k.WSlider

/*
 * Scrolling category: demo pages for AnnotatedScrollBar / PipsPager / ScrollView / ScrollViewer / SemanticZoom.
 */

// region Common helper

/** A tile grid (14 rows x 6 columns) larger than the viewport, used by the scrolling demos. Overflows in both directions. */
private fun buildScrollingTileContent(): WComponent {
    val palette = listOf(
        WColor(0, 120, 212),
        WColor(16, 137, 62),
        WColor(202, 80, 16),
        WColor(142, 68, 173),
        WColor(193, 0, 76),
        WColor(0, 153, 188),
    )
    val content = WPanel(spacing = 8.0)
    for (row in 0 until 14) {
        val rowPanel = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
        for (column in 0 until 6) {
            val tile = buildTile(palette[(row + column) % palette.size], width = 130.0, height = 48.0)
            tile.child = WLabel("$row-$column").also { it.foreground = WColor.WHITE }
            tile.padding = 8.0
            rowPanel.add(tile)
        }
        content.add(rowPanel)
    }
    return content
}

// endregion

// region AnnotatedScrollBar

/** The AnnotatedScrollBar page: lines up a demo for trying out WAnnotatedScrollBar's features. */
internal fun buildAnnotatedScrollBarPage(): WComponent {
    val page = buildPage(
        "AnnotatedScrollBar",
        "A control that extends a vertical scrollbar with labels along the rail so you can jump " +
            "quickly through a large collection. Try it out with WAnnotatedScrollBar connected " +
            "as a ScrollView's vertical scroll controller. Clicking a label jumps to that " +
            "position, and hovering over the rail shows the color group name as a tooltip " +
            "(DetailLabelRequested). (The real Gallery uses ItemsRepeater for the content, but " +
            "since winui4k doesn't support that yet, this substitutes a tile grid.)",
    )

    page.add(buildAnnotatedScrollBarExample())
    return page
}

/** Navigation paired with a ScrollView: Labels / connecting a ScrollController / DetailLabelRequested. */
private fun buildAnnotatedScrollBarExample(): WComponent {
    // The same color groups as the real Gallery. Attach a label at each group's starting scroll offset
    data class ColorGroup(val name: String, val color: WColor, val count: Int)
    val groups = listOf(
        ColorGroup("Azure", WColor(0, 120, 212), 12),
        ColorGroup("Crimson", WColor(220, 20, 60), 18),
        ColorGroup("Cyan", WColor(0, 153, 188), 6),
        ColorGroup("Fuchsia", WColor(194, 57, 179), 24),
        ColorGroup("Gold", WColor(202, 149, 16), 18),
    )
    val columns = 6
    val tileSize = 100.0
    val rowSpacing = 4.0
    val rowPitch = tileSize + rowSpacing

    val content = WPanel(spacing = rowSpacing)
    // Each group's starting scroll offset (in pixels) and name. Used by the labels and DetailLabelRequested
    val groupStartOffsets = mutableListOf<Pair<Double, String>>()
    var rowIndex = 0
    for (group in groups) {
        groupStartOffsets.add(rowIndex * rowPitch to group.name)
        var remaining = group.count
        while (remaining > 0) {
            val rowPanel = WPanel(spacing = rowSpacing, orientation = Orientation.HORIZONTAL)
            repeat(minOf(columns, remaining)) {
                rowPanel.add(buildTile(group.color, width = tileSize, height = tileSize))
            }
            remaining -= minOf(columns, remaining)
            content.add(rowPanel)
            rowIndex++
        }
    }

    val scrollView = WScrollView(content)
    scrollView.width = 680.0
    scrollView.height = 500.0
    // AnnotatedScrollBar takes over the vertical bar's role, so hide it (scrolling itself stays enabled)
    scrollView.verticalScrollBarVisibility = ScrollingScrollBarVisibility.HIDDEN

    val annotatedScrollBar = WAnnotatedScrollBar()
    annotatedScrollBar.height = 500.0
    annotatedScrollBar.verticalAlignment = VerticalAlignment.TOP
    annotatedScrollBar.addDetailLabelRequestedListener { offset ->
        // Return the name of the group containing this offset, as the tooltip
        groupStartOffsets.lastOrNull { it.first <= offset }?.second ?: groups.first().name
    }

    // Connect it as the vertical scroll controller after the ScrollView's template is applied (Loaded)
    scrollView.addLoadedListener { annotatedScrollBar.connectTo(scrollView) }
    // Place the labels (markers) after the content is laid out (SizeChanged = the scroll range is
    // finalized). The real Gallery also rebuilds its labels on ItemsRepeater's SizeChanged
    content.addSizeChangedListener {
        annotatedScrollBar.connectTo(scrollView) // connect if not already connected
        annotatedScrollBar.clearLabels()
        for ((offset, name) in groupStartOffsets) {
            annotatedScrollBar.addLabel(name, offset)
        }
    }

    val body = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    body.add(scrollView)
    body.add(annotatedScrollBar)

    // Options: as in the real Gallery, change AnnotatedScrollBar's height to observe labels being thinned out
    val heightSlider = WSlider(minimum = 100.0, maximum = 500.0, value = 500.0)
    heightSlider.header = "AnnotatedScrollBar height"
    heightSlider.width = 240.0
    heightSlider.addChangeListener { annotatedScrollBar.height = it }

    val options = WPanel(spacing = 12.0)
    options.add(optionsLabel("Shrinking the height lets you see labels get thinned out so they don't collide."))
    options.add(heightSlider)

    return buildExample(
        "Navigation paired with a ScrollView (Labels / ScrollController / DetailLabelRequested)",
        body,
        options,
    )
}

// endregion

// region PipsPager

/** The PipsPager page: lines up demos for trying out WPipsPager's various features. */
internal fun buildPipsPagerPage(): WComponent {
    val page = buildPage(
        "PipsPager",
        "A pager that moves between pages via a row of dots (pips) instead of explicit page " +
            "numbers. Try out WPipsPager's various features.",
    )

    page.add(buildPipsPagerGalleryExample())
    page.add(buildPipsPagerOptionsExample())
    return page
}

/** Paired with an image gallery: NumberOfPages / SelectedPageIndex / SelectedIndexChanged. */
private fun buildPipsPagerGalleryExample(): WComponent {
    // The same 8 landscape photos as the real Gallery. Swap the displayed image based on the selected page
    val imageUris = (1..8).map { galleryImageUri("LandscapeImage$it.jpg") }

    val image = WImage(imageUris.first())
    image.width = 400.0
    image.height = 270.0

    val pager = WPipsPager()
    pager.numberOfPages = imageUris.size
    // Unlike the real Gallery's FlipView example, also show the buttons so paging works via them too
    pager.previousButtonVisibility = PipsPagerButtonVisibility.VISIBLE
    pager.nextButtonVisibility = PipsPagerButtonVisibility.VISIBLE
    pager.horizontalAlignment = HorizontalAlignment.CENTER
    pager.addSelectedIndexChangedListener { index ->
        image.sourceUri = imageUris[index]
    }

    val body = WPanel(spacing = 12.0)
    body.add(image)
    body.add(pager)
    return buildExample("Paired with an image gallery (NumberOfPages / SelectedPageIndex / SelectedIndexChanged)", body)
}

/** Orientation and button visibility: switch Orientation / the previous/next buttons' visibility via Options. */
private fun buildPipsPagerOptionsExample(): WComponent {
    val pager = WPipsPager()
    pager.numberOfPages = 10
    pager.previousButtonVisibility = PipsPagerButtonVisibility.VISIBLE
    pager.nextButtonVisibility = PipsPagerButtonVisibility.VISIBLE

    val selected = WLabel("Selected page: 1")
    selected.foreground = TEXT_SECONDARY
    pager.addSelectedIndexChangedListener { index ->
        selected.text = "Selected page: ${index + 1}"
    }

    val body = WPanel(spacing = 12.0)
    body.add(pager)
    body.add(selected)

    // Options: the same controls as the real Gallery's PipsPagerPage Example2
    val orientationCombo = WComboBox(listOf("Horizontal", "Vertical"))
    orientationCombo.header = "Orientation"
    orientationCombo.width = 240.0
    orientationCombo.selectedIndex = 0
    orientationCombo.addListSelectionListener {
        pager.orientation =
            if (orientationCombo.selectedItem == "Vertical") Orientation.VERTICAL else Orientation.HORIZONTAL
    }

    fun visibilityCombo(header: String, apply: (PipsPagerButtonVisibility) -> Unit): WComboBox {
        val combo = WComboBox(listOf("Visible", "VisibleOnPointerOver", "Collapsed"))
        combo.header = header
        combo.width = 240.0
        combo.selectedIndex = 0
        combo.addListSelectionListener {
            apply(
                when (combo.selectedItem) {
                    "VisibleOnPointerOver" -> PipsPagerButtonVisibility.VISIBLE_ON_POINTER_OVER
                    "Collapsed" -> PipsPagerButtonVisibility.COLLAPSED
                    else -> PipsPagerButtonVisibility.VISIBLE
                },
            )
        }
        return combo
    }

    val options = WPanel(spacing = 12.0)
    options.add(orientationCombo)
    options.add(visibilityCombo("Previous button visibility") { pager.previousButtonVisibility = it })
    options.add(visibilityCombo("Next button visibility") { pager.nextButtonVisibility = it })

    return buildExample(
        "Orientation and button visibility (Orientation / PreviousButtonVisibility / NextButtonVisibility)",
        body,
        options,
    )
}

// endregion

// region ScrollView

/** The ScrollView page: lines up demos for trying out WScrollView's various features. */
internal fun buildScrollViewPage(): WComponent {
    val page = buildPage(
        "ScrollView",
        "A container that shows content larger than the viewport by scrolling, panning, and " +
            "zooming it. Try out the various features of WScrollView, the successor to ScrollViewer.",
    )

    page.add(buildScrollViewImageExample())
    page.add(buildScrollViewProgrammaticExample())
    return page
}

/** Zooming an image: scale via ContentOrientation=None + ZoomMode, with Options to change the behavior. */
private fun buildScrollViewImageExample(): WComponent {
    val image = WImage(galleryImageUri("cliff.jpg"))
    image.stretch = Stretch.UNIFORM

    val scrollView = WScrollView(image)
    scrollView.width = 400.0
    scrollView.height = 266.0
    scrollView.contentOrientation = ScrollingContentOrientation.NONE
    scrollView.zoomMode = ScrollingZoomMode.ENABLED

    val note = WLabel("Zoom with Ctrl+mouse wheel, or a touch pinch gesture. Use the settings on the right to change the behavior.")
    note.textWrapping = TextWrapping.WRAP
    note.foreground = TEXT_SECONDARY

    val body = WPanel(spacing = 12.0)
    body.add(note)
    body.add(scrollView)

    // Options: the same controls as the real Gallery's ScrollViewPage Example1 (zoom-value fields omitted due to the float constraint)
    val zoomModeCombo = WComboBox(listOf("Enabled", "Disabled"))
    zoomModeCombo.header = "ZoomMode"
    zoomModeCombo.width = 240.0
    zoomModeCombo.selectedIndex = 0
    zoomModeCombo.addListSelectionListener {
        scrollView.zoomMode =
            if (zoomModeCombo.selectedItem == "Disabled") ScrollingZoomMode.DISABLED else ScrollingZoomMode.ENABLED
    }

    val orientationCombo = WComboBox(listOf("None", "Vertical", "Horizontal", "Both"))
    orientationCombo.header = "ContentOrientation"
    orientationCombo.width = 240.0
    orientationCombo.selectedIndex = 0
    orientationCombo.addListSelectionListener {
        scrollView.contentOrientation = when (orientationCombo.selectedItem) {
            "Vertical" -> ScrollingContentOrientation.VERTICAL
            "Horizontal" -> ScrollingContentOrientation.HORIZONTAL
            "Both" -> ScrollingContentOrientation.BOTH
            else -> ScrollingContentOrientation.NONE
        }
    }

    fun scrollBarCombo(header: String, apply: (ScrollingScrollBarVisibility) -> Unit): WComboBox {
        val combo = WComboBox(listOf("Auto", "Visible", "Hidden"))
        combo.header = header
        combo.width = 240.0
        combo.selectedIndex = 0
        combo.addListSelectionListener {
            apply(
                when (combo.selectedItem) {
                    "Visible" -> ScrollingScrollBarVisibility.VISIBLE
                    "Hidden" -> ScrollingScrollBarVisibility.HIDDEN
                    else -> ScrollingScrollBarVisibility.AUTO
                },
            )
        }
        return combo
    }

    val options = WPanel(spacing = 12.0)
    options.add(zoomModeCombo)
    options.add(orientationCombo)
    options.add(scrollBarCombo("Horizontal ScrollBar") { scrollView.horizontalScrollBarVisibility = it })
    options.add(scrollBarCombo("Vertical ScrollBar") { scrollView.verticalScrollBarVisibility = it })

    return buildExample(
        "Zooming an image (ZoomMode / ContentOrientation / ScrollBarVisibility)",
        body,
        options,
    )
}

/** Scrolling programmatically: ScrollTo / ScrollBy and subscribing to scroll position (ViewChanged). */
private fun buildScrollViewProgrammaticExample(): WComponent {
    val scrollView = WScrollView(buildScrollingTileContent())
    scrollView.width = 400.0
    scrollView.height = 260.0
    scrollView.contentOrientation = ScrollingContentOrientation.BOTH

    val offset = WLabel("Position: (0, 0)")
    offset.foreground = TEXT_SECONDARY
    scrollView.addViewChangedListener {
        offset.text = "Position: (${scrollView.horizontalOffset.toInt()}, ${scrollView.verticalOffset.toInt()})"
    }

    val body = WPanel(spacing = 12.0)
    body.add(scrollView)
    body.add(offset)

    val toStart = WButton("To start")
    toStart.addActionListener { scrollView.scrollTo(0.0, 0.0) }
    val toEnd = WButton("To end")
    toEnd.addActionListener { scrollView.scrollTo(scrollView.scrollableWidth, scrollView.scrollableHeight) }
    val down = WButton("Down +100")
    down.addActionListener { scrollView.scrollBy(0.0, 100.0) }
    val right = WButton("Right +100")
    right.addActionListener { scrollView.scrollBy(100.0, 0.0) }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("ScrollTo animates to an absolute position; ScrollBy animates by a relative amount from the current position."))
    options.add(toStart)
    options.add(toEnd)
    options.add(down)
    options.add(right)

    return buildExample("Scrolling programmatically (ScrollTo / ScrollBy / ViewChanged)", body, options)
}

// endregion

// region ScrollViewer

/** The ScrollViewer page: lines up demos for trying out WScrollPane's various features. */
internal fun buildScrollViewerPage(): WComponent {
    val page = buildPage(
        "ScrollViewer",
        "The traditional container that shows content larger than the viewport by scrolling it. " +
            "Try out WScrollPane's various features.",
    )

    page.add(buildScrollViewerVisibilityExample())
    page.add(buildScrollViewerProgrammaticExample())
    return page
}

/** How scrollbars are shown: switch horizontal / vertical ScrollBarVisibility via Options. */
private fun buildScrollViewerVisibilityExample(): WComponent {
    val scrollPane = WScrollPane(buildScrollingTileContent())
    scrollPane.width = 400.0
    scrollPane.height = 260.0
    // Horizontal scrolling is disabled by default, so set it to AUTO to also allow interacting with the horizontal overflow
    scrollPane.horizontalScrollBarVisibility = ScrollBarVisibility.AUTO
    scrollPane.verticalScrollBarVisibility = ScrollBarVisibility.VISIBLE

    val body = WPanel(spacing = 12.0)
    body.add(scrollPane)

    fun visibilityCombo(header: String, initial: ScrollBarVisibility, apply: (ScrollBarVisibility) -> Unit): WComboBox {
        val names = listOf("Disabled", "Auto", "Hidden", "Visible")
        val combo = WComboBox(names)
        combo.header = header
        combo.width = 240.0
        combo.selectedIndex = names.indexOf(initial.name.lowercase().replaceFirstChar { it.uppercase() })
        combo.addListSelectionListener {
            apply(
                when (combo.selectedItem) {
                    "Disabled" -> ScrollBarVisibility.DISABLED
                    "Hidden" -> ScrollBarVisibility.HIDDEN
                    "Visible" -> ScrollBarVisibility.VISIBLE
                    else -> ScrollBarVisibility.AUTO
                },
            )
        }
        return combo
    }

    val options = WPanel(spacing = 12.0)
    options.add(visibilityCombo("Horizontal ScrollBar", ScrollBarVisibility.AUTO) { scrollPane.horizontalScrollBarVisibility = it })
    options.add(visibilityCombo("Vertical ScrollBar", ScrollBarVisibility.VISIBLE) { scrollPane.verticalScrollBarVisibility = it })

    return buildExample("How scrollbars are shown (HorizontalScrollBarVisibility / VerticalScrollBarVisibility)", body, options)
}

/** Scrolling horizontally from code: ChangeView and subscribing to scroll position (ViewChanged). */
private fun buildScrollViewerProgrammaticExample(): WComponent {
    val scrollPane = WScrollPane(buildScrollingTileContent())
    scrollPane.width = 400.0
    scrollPane.height = 200.0
    scrollPane.horizontalScrollBarVisibility = ScrollBarVisibility.AUTO

    val offset = WLabel("Horizontal position: 0 / Scrollable width: 0")
    offset.foreground = TEXT_SECONDARY
    scrollPane.addViewChangedListener {
        offset.text = "Horizontal position: ${scrollPane.horizontalOffset.toInt()} / Scrollable width: ${scrollPane.scrollableWidth.toInt()}"
    }

    val body = WPanel(spacing = 12.0)
    body.add(scrollPane)
    body.add(offset)

    val toStart = WButton("To left edge")
    toStart.addActionListener { scrollPane.scrollToHorizontalOffset(0.0) }
    val toEnd = WButton("To right edge")
    toEnd.addActionListener { scrollPane.scrollToHorizontalOffset(scrollPane.scrollableWidth) }
    val center = WButton("To center")
    center.addActionListener { scrollPane.scrollToHorizontalOffset(scrollPane.scrollableWidth / 2.0) }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("ScrollToHorizontalOffset (ChangeView) animates the horizontal scroll position."))
    options.add(toStart)
    options.add(center)
    options.add(toEnd)

    return buildExample("Scrolling horizontally from code (ChangeView / ViewChanged)", body, options)
}

// endregion

// region SemanticZoom

/** The SemanticZoom page: lines up a demo for trying out WSemanticZoom's features. */
internal fun buildSemanticZoomPage(): WComponent {
    val page = buildPage(
        "SemanticZoom",
        "A control that switches between a detail view and a summary view of a single " +
            "collection, making large collections easier to navigate. Try out WSemanticZoom's " +
            "various features. (The real Gallery uses a grouped CollectionViewSource, but since " +
            "winui4k doesn't support that yet, this demo simplifies it to detail view = all " +
            "items, summary view = range labels.)",
    )

    page.add(buildSemanticZoomExample())
    return page
}

/** Switching views: ZoomedInView / ZoomedOutView / ToggleActiveView / ViewChangeStarted. */
private fun buildSemanticZoomExample(): WComponent {
    // Detail view: all 60 items. Summary view: range labels grouped by 20 items
    val zoomedIn = WList((1..60).map { "Item $it" })
    val zoomedOut = WList(listOf("Item 1-20", "Item 21-40", "Item 41-60"))

    val semanticZoom = WSemanticZoom(zoomedIn, zoomedOut)
    semanticZoom.width = 400.0
    semanticZoom.height = 400.0
    semanticZoom.canChangeViews = true
    semanticZoom.isZoomOutButtonEnabled = true

    val activeView = WLabel("Showing: detail view (ZoomedIn)")
    activeView.foreground = TEXT_SECONDARY
    semanticZoom.addViewChangeStartedListener {
        // ViewChangeStarted fires at the "start" of a switch, so this shows the view about to be displayed
        activeView.text = if (semanticZoom.isZoomedInViewActive) "Showing: summary view (ZoomedOut)" else "Showing: detail view (ZoomedIn)"
    }

    val body = WPanel(spacing = 12.0)
    body.add(semanticZoom)
    body.add(activeView)

    val toggle = WButton("Switch view (ToggleActiveView)")
    toggle.addActionListener { semanticZoom.toggleActiveView() }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("You can also switch by zooming out with Ctrl+mouse wheel, or via the zoom-out button in the bottom-left of the summary view."))
    options.add(toggle)

    return buildExample("Switching views (ZoomedInView / ZoomedOutView / ToggleActiveView)", body, options)
}

// endregion
