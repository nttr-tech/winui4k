package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.FlowDirection
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonPropertyChangeListener
import com.appkitbox.winui4k.extension.ribbon.model.RibbonQuickAccessPosition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * An Office-style title bar (RibbonSpace's RibbonTitleBar): the app icon, the ribbon's QAT (when it is placed above the
 * ribbon), the document title, a command search in the center, and content at the right end (account, share). When
 * integrated into the window's title bar with [attachToWindow], the empty parts become the window's drag region, while
 * the interactive parts (QAT, search, left and right content) keep receiving input.
 */
class WRibbonTitleBar(
    /** The connected ribbon (for the QAT and search). */
    val ribbon: WRibbon,
) : WComponent(RibbonThemeResources.load(XAML)) {
    private val root = XamlElement(inspectable.also { it.addRef() })
    private val dragRegion = root.part("PART_DragRegion")
    private val appIconHost = root.part("PART_AppIcon")
    private val appButton = root.part("PART_AppButton")
    private val startHost = root.part("PART_StartContent")
    private val quickAccessHost = root.part("PART_QuickAccessHost")
    private val titlePanel = root.part("PART_Title")
    private val titleText = root.part("PART_TitleText")
    private val subtitleText = root.part("PART_Subtitle")
    private val searchHost = root.part("PART_SearchHost")
    private val endHost = root.part("PART_EndContent")
    private val appIconListeners = CopyOnWriteArrayList<RibbonApplicationButtonListener>()
    private var frame: WFrame? = null
    private var passthroughPending = false

    /** The search box. */
    val searchBox: WRibbonSearchBox = WRibbonSearchBox(ribbon)

    /** The document / window title (the title of the ribbon's model if null). */
    var title: String? = null
        set(value) {
            field = value
            updateTitle()
        }

    /** Supplementary text next to the title ("Saved", "Editing"). */
    var subtitle: String? = null
        set(value) {
            field = value
            subtitleText.setText(value)
            subtitleText.isVisible = !value.isNullOrEmpty()
        }

    /** The app icon. */
    var appIcon: RibbonIcon? = null
        set(value) {
            field = value
            updateAppIcon()
        }

    /** Whether the icon is a button that opens the ribbon's application menu / Backstage (AutoCAD's application button). */
    var isAppIconMenuEnabled: Boolean = false
        set(value) {
            field = value
            updateAppIcon()
        }

    /** Content placed after the icon (an AutoSave switch, etc.). */
    var startContent: WComponent? = null
        set(value) {
            field = value
            startHost.setChild(value?.also { Xaml.detach(it) })
            schedulePassthrough()
        }

    /** Content placed at the right end (account, share, comments). */
    var endContent: WComponent? = null
        set(value) {
            field = value
            endHost.setChild(value?.also { Xaml.detach(it) })
            schedulePassthrough()
        }

    /** Whether to show the search box. */
    var isSearchVisible: Boolean = true
        set(value) {
            field = value
            updateAdaptive()
        }

    /** The width of the search box. */
    var searchWidth: Double = DEFAULT_SEARCH_WIDTH
        set(value) {
            field = value
            updateAdaptive()
        }

    private val modelListener = RibbonPropertyChangeListener { event -> if (event.propertyName == "title") updateTitle() }

    init {
        RibbonThemeResources.ensure()
        searchHost.setChild(searchBox)
        appButton.onClick { onAppButtonClick() }
        ribbon.isQuickAccessHostedExternally = true
        ribbon.addQuickAccessListener { hostQuickAccess() }
        ribbon.model.addPropertyChangeListener(modelListener)
        ribbon.addModelReplacedListener { old, new ->
            old.removePropertyChangeListener(modelListener)
            new.addPropertyChangeListener(modelListener)
            updateTitle()
        }
        addSizeChangedListener {
            updateAdaptive()
            WinUiUtilities.invokeLater { updateAdaptive() }
            schedulePassthrough()
        }
        addLoadedListener {
            hostQuickAccess()
            updateAdaptive()
            schedulePassthrough()
        }
        updateTitle()
        updateAppIcon()
        subtitle = null
    }

    private fun updateTitle() {
        titleText.setText(title ?: ribbon.model.title)
    }

    private fun updateAppIcon() {
        val xaml = RibbonIconXaml.build(appIcon, APP_ICON_SIZE, "{ThemeResource RibbonTitleBarIconBrush}")
        appIconHost.setChild(xaml?.let { XamlElement.load(it) })
        appIconHost.isVisible = !isAppIconMenuEnabled && xaml != null
        appButton.isVisible = isAppIconMenuEnabled
        appButton.setContent(RibbonIconXaml.build(appIcon, APP_BUTTON_ICON_SIZE, "{ThemeResource RibbonTitleBarIconBrush}")?.let { XamlElement.load(it) })
        val name = ribbon.model.applicationButtonLabel ?: RibbonStrings.current.file
        appButton.setAutomationName(name)
        appButton.setToolTipValue(name)
        schedulePassthrough()
    }

    private fun onAppButtonClick() {
        val event = RibbonHandledEvent(ribbon)
        appIconListeners.forEach { it.applicationButtonClicked(event) }
        if (event.isHandled) return
        val menu = ribbon.applicationMenu
        if (ribbon.model.backstage.items.isEmpty() && menu != null) menu.showAt(appButton) else ribbon.invokeApplicationButton()
    }

    /** Subscribes to clicks on the icon button (use [RibbonHandledEvent.isHandled] to prevent the default action). */
    fun addAppIconListener(listener: RibbonApplicationButtonListener) {
        appIconListeners += listener
    }

    /** Unsubscribes a listener added with [addAppIconListener]. */
    fun removeAppIconListener(listener: RibbonApplicationButtonListener) {
        appIconListeners -= listener
    }

    /** Whether the QAT is placed here. */
    var isHostingQuickAccess: Boolean = false
        private set

    /** When the QAT is shown above the ribbon, places it here. */
    private fun hostQuickAccess() {
        val model = ribbon.model
        val show = model.isQuickAccessVisible && model.quickAccessPosition == RibbonQuickAccessPosition.ABOVE_RIBBON
        isHostingQuickAccess = show
        if (show) {
            val qat = ribbon.quickAccessElement
            Xaml.detach(qat)
            quickAccessHost.setChild(qat)
        } else {
            quickAccessHost.setChild(null)
            ribbon.updateQuickAccessPlacement()
        }
        schedulePassthrough()
    }

    /** Shows and hides the search and title to fit the width (when narrow, hides the search first, then the title). */
    private fun updateAdaptive() {
        val width = actualWidth
        searchHost.isVisible = isSearchVisible && width > SEARCH_MIN_WINDOW
        searchBox.width = min(searchWidth, max(SEARCH_MIN_WIDTH, (width - SEARCH_RESERVED) / 2))
        titlePanel.isVisible = width == 0.0 || width > TITLE_MIN_WINDOW
        // Truncate the title to the width that fits in the center (empty) column (so it does not overlap the search box)
        val column = dragRegion.actualWidth
        if (column > 0) {
            val search = if (searchHost.isVisible) searchBox.width + TITLE_SPACING else 0.0
            titleText.maxWidth = max(TITLE_MIN_WIDTH, min(TITLE_MAX_WIDTH, column - search - subtitleText.actualWidth - TITLE_SPACING * 2))
        }
    }

    /**
     * Integrates into the window's title bar: extends the content into the title bar, makes the empty parts the drag
     * region, and leaves space at the right end for the caption buttons.
     */
    fun attachToWindow(frame: WFrame) {
        this.frame = frame
        frame.extendsContentIntoTitleBar = true
        frame.setTitleBar(dragRegion)
        WinUiUtilities.invokeLater { applyCaptionInset() }
        schedulePassthrough()
    }

    private fun applyCaptionInset() {
        val target = frame ?: return
        val scale = rasterizationScale()
        val titleBar = target.appWindow.titleBar
        // Padding is mirrored left-right in right-to-left layout, but the caption buttons are not, so swap them beforehand
        val rtl = flowDirection == FlowDirection.RIGHT_TO_LEFT
        val left = (if (rtl) titleBar.rightInset else titleBar.leftInset) / scale
        val right = (if (rtl) titleBar.leftInset else titleBar.rightInset) / scale
        root.view(XamlInterop.IID_IGrid).let { grid ->
            XamlStructs.putThickness(grid, XamlInterop.IGrid_put_Padding, left, 0.0, right, 0.0)
        }
        schedulePassthrough()
    }

    private fun rasterizationScale(): Double {
        val xamlRoot = uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return 1.0
        return try {
            xamlRoot.getDouble(XamlInterop.IXamlRoot_get_RasterizationScale).takeIf { it > 0 } ?: 1.0
        } finally {
            xamlRoot.release()
        }
    }

    private fun schedulePassthrough() {
        if (frame == null || passthroughPending) return
        passthroughPending = true
        WinUiUtilities.invokeLater {
            passthroughPending = false
            updatePassthroughRegions()
        }
    }

    /** Makes the interactive parts (icon button, left and right content, QAT, search) pass-through regions for title bar input. */
    private fun updatePassthroughRegions() {
        val target = frame ?: return
        val scale = rasterizationScale()
        val parts = listOf(appButton, startHost, quickAccessHost, searchHost, endHost).filter { it.isVisible && it.actualWidth > 0 }
        val rects = parts.map { part ->
            val p = part.positionInRoot()
            intArrayOf(
                (p[0] * scale).roundToInt(),
                (p[1] * scale).roundToInt(),
                (part.actualWidth * scale).roundToInt(),
                (part.actualHeight * scale).roundToInt(),
            )
        }
        val statics = Activation.factory(WindowingInterop.CLS_InputNonClientPointerSource, WindowingInterop.IID_IInputNonClientPointerSourceStatics)
        try {
            Ffi.backend.withScope { scope ->
                val source = statics.getPtr(
                    WindowingInterop.IInputNonClientPointerSourceStatics_GetForWindowId,
                    XamlStructs.windowIdValue(scope, target.appWindow.id),
                )
                try {
                    val buffer = scope.allocate((maxOf(1, rects.size) * RECT_BYTES).toLong())
                    val memory = Ffi.backend.memory
                    rects.forEachIndexed { index, rect -> rect.forEachIndexed { field, value -> memory.putInt(buffer, (index * RECT_BYTES + field * INT_BYTES).toLong(), value) } }
                    source.call(WindowingInterop.IInputNonClientPointerSource_SetRegionRects, WindowingInterop.NonClientRegionKind_Passthrough, rects.size, buffer)
                } finally {
                    source.release()
                }
            }
        } finally {
            statics.release()
        }
    }

    private companion object {
        const val DEFAULT_SEARCH_WIDTH = 460.0
        const val SEARCH_MIN_WIDTH = 180.0
        const val SEARCH_RESERVED = 520.0
        const val SEARCH_MIN_WINDOW = 720.0
        const val TITLE_MIN_WINDOW = 480.0
        const val TITLE_MIN_WIDTH = 60.0
        const val TITLE_MAX_WIDTH = 320.0
        const val TITLE_SPACING = 16.0
        const val APP_ICON_SIZE = 20.0
        const val APP_BUTTON_ICON_SIZE = 24.0
        const val RECT_BYTES = 16
        const val INT_BYTES = 4

        const val XAML = "<Grid Background=\"{ThemeResource RibbonTitleBarBackgroundBrush}\" Height=\"44\">" +
            "<Grid.ColumnDefinitions><ColumnDefinition Width=\"Auto\" /><ColumnDefinition Width=\"Auto\" /><ColumnDefinition Width=\"Auto\" />" +
            "<ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
            "<Border x:Name=\"PART_DragRegion\" Grid.Column=\"3\" Background=\"Transparent\" />" +
            "<Border x:Name=\"PART_AppIcon\" Margin=\"12,0,6,0\" VerticalAlignment=\"Center\" />" +
            "<Button x:Name=\"PART_AppButton\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Visibility=\"Collapsed\" Padding=\"6,4\" " +
            "Margin=\"6,0,2,0\" VerticalAlignment=\"Center\" />" +
            "<Border x:Name=\"PART_StartContent\" Grid.Column=\"1\" VerticalAlignment=\"Center\" />" +
            "<Border x:Name=\"PART_QuickAccessHost\" Grid.Column=\"2\" VerticalAlignment=\"Center\" Margin=\"4,0\" />" +
            "<StackPanel Grid.Column=\"3\" Orientation=\"Horizontal\" HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" Spacing=\"16\">" +
            "<StackPanel x:Name=\"PART_Title\" Orientation=\"Horizontal\" Spacing=\"6\" VerticalAlignment=\"Center\" IsHitTestVisible=\"False\">" +
            "<TextBlock x:Name=\"PART_TitleText\" Foreground=\"{ThemeResource RibbonTitleBarForegroundBrush}\" FontWeight=\"SemiBold\" " +
            "TextTrimming=\"CharacterEllipsis\" MaxWidth=\"320\" VerticalAlignment=\"Center\" />" +
            "<TextBlock x:Name=\"PART_Subtitle\" Foreground=\"{ThemeResource RibbonTitleBarForegroundBrush}\" Opacity=\"0.7\" VerticalAlignment=\"Center\" />" +
            "</StackPanel><Border x:Name=\"PART_SearchHost\" /></StackPanel>" +
            "<Border x:Name=\"PART_EndContent\" Grid.Column=\"4\" VerticalAlignment=\"Center\" Margin=\"0,0,10,0\" />" +
            "</Grid>"
    }
}
