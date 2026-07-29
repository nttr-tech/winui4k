package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * The content placement of a [WSettingsCard] (equivalent to the Toolkit's ContentAlignment).
 * It is a Toolkit-specific enum, so it has no values derived from a winmd.
 */
enum class ContentAlignment {
    /**
     * Places the content at the right edge (the default). As the card gets narrower the content
     * automatically wraps to the row below the header, and when it gets narrower still the header
     * icon is hidden as well (the Toolkit's RightWrapped family of states).
     */
    RIGHT,

    /**
     * Hides the header, description, and icon and places the content on the left.
     * Intended for putting a checkbox or a custom layout directly on the card.
     */
    LEFT,

    /** Lays the content out across the full width of the row below the header and description. */
    VERTICAL,
}

/**
 * Equivalent to the Windows Community Toolkit's SettingsCard: a card representing a single entry
 * on a settings page.
 *
 * WinUI 3 has no native SettingsCard (the Toolkit's is a C# implementation), so this reproduces it
 * by applying a Style / ControlTemplate with the same look as the Toolkit's SettingsCard.xaml to a
 * native Button via XamlReader. The colors reference theme resources (CardBackgroundFillColorDefault
 * and so on), so they follow theme switches automatically.
 *
 * From left to right: [headerIcon], the two lines of text made of [header] and [description],
 * [content], and the chevron icon shown when the card is clickable ([actionIcon]). Enabling
 * [isClickEnabled] makes the whole card respond as a button ([addActionListener] / [command]),
 * with a background change on hover / press and the ActionIcon appearing.
 *
 * The content position can be changed with [contentAlignment]. With the default
 * [ContentAlignment.RIGHT] it wraps automatically according to the card width, using the same
 * thresholds as the Toolkit (476 / 286px).
 */
class WSettingsCard @JvmOverloads constructor(header: String = "", description: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_Button, XamlInterop.IID_IButtonFactory), // default interface = IButton
) {
    /** The Primitives.IButtonBase view, used for Click / Command. */
    private val buttonBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IButtonBase))
    }

    /** The Click event tokens registered by addActionListener (used by removeActionListener). */
    private val clickTokens = ListenerTokens<Runnable>()

    /** The FrameworkElement view of the card's internal layout (a Grid built by XamlReader). The root for FindName. */
    private val layoutRoot: ComPtr

    /** The IGrid view of the internal layout (used to switch RowSpacing when wrapping). */
    private val layoutGrid: ComPtr

    /** The TextBlock holding the header string (an ITextBlock view). */
    private val headerText: ComPtr

    /** The TextBlock holding the description (an ITextBlock view). */
    private val descriptionText: ComPtr

    /** The TextBlock that displays the header icon's glyph (an ITextBlock view). */
    private val headerIconText: ComPtr

    /** The Viewbox that fits the header icon into 20px (an IUIElement view, for toggling visibility). */
    private val headerIconHolder: ComPtr

    /** The StackPanel that stacks the header and description (an IFrameworkElement view, for the margin and visibility). */
    private val headerPanel: ComPtr

    /** The Border that the user content is inserted into (an IBorder view). */
    private val contentHolder: ComPtr

    /** The IFrameworkElement view of [contentHolder] (for switching the layout cell and the alignment). */
    private val contentHolderElement: ComPtr

    /** The TextBlock that displays the ActionIcon's glyph (an ITextBlock view). */
    private val actionIconText: ComPtr

    /** The Viewbox of the ActionIcon (an IUIElement view; the target of the visibility toggle and the tooltip). */
    private val actionIconHolder: ComPtr

    /** The layout state currently applied (used to skip re-applying it on SizeChanged). */
    private var appliedState: LayoutState? = null

    /** The card heading. An empty string hides the heading line entirely. */
    var header: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> headerText.call(XamlInterop.ITextBlock_put_Text, h) }
            setVisible(headerText, value.isNotEmpty())
            applyLayoutState()
        }

    /** The description shown below the heading. An empty string hides the description line entirely. */
    var description: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> descriptionText.call(XamlInterop.ITextBlock_put_Text, h) }
            setVisible(descriptionText, value.isNotEmpty())
            applyLayoutState()
        }

    /** The icon at the left edge. null hides it (along with the indent reserved for the icon). */
    var headerIcon: Symbol? = null
        set(value) {
            field = value
            putGlyph(headerIconText, value)
            applyLayoutState()
        }

    /**
     * The icon shown at the right edge while [isClickEnabled] is on. null means the default chevron
     * (ChevronRightSmall; it is represented by null because the Symbol enum has no such glyph).
     */
    var actionIcon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                Hstring.use(DEFAULT_ACTION_GLYPH) { h -> actionIconText.call(XamlInterop.ITextBlock_put_Text, h) }
            } else {
                putGlyph(actionIconText, value)
            }
        }

    /** The text hint shown when hovering over the ActionIcon (ToolTipService.ToolTip). null clears it. */
    var actionIconToolTip: String? = null
        set(value) {
            field = value
            val target = actionIconHolder.queryInterface(XamlInterop.IID_IDependencyObject)
            if (value == null) {
                toolTipStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, target, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                toolTipStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, target, boxed.ptr)
                boxed.release()
            }
            target.release()
        }

    /** The content shown on the right side (a WToggleSwitch, a WComboBox, and so on). null removes it. */
    var content: WComponent? = null
        set(value) {
            field = value
            contentHolder.call(XamlInterop.IBorder_put_Child, value?.uiElement?.ptr)
            applyLayoutState()
        }

    /** The content placement. The default is [ContentAlignment.RIGHT] (wraps automatically when narrow). */
    var contentAlignment: ContentAlignment = ContentAlignment.RIGHT
        set(value) {
            field = value
            applyLayoutState()
        }

    /**
     * Whether the whole card is clickable (false by default). Enabling it makes the background change
     * on hover / press, shows the ActionIcon at the right edge, and makes the listeners of
     * [addActionListener] and [command] fire on a click.
     */
    var isClickEnabled: Boolean = false
        set(value) {
            field = value
            applyStyle(value)
            updateActionIconVisibility()
        }

    /** Whether the ActionIcon at the right edge is shown while [isClickEnabled] is on (true by default). */
    var isActionIconVisible: Boolean = true
        set(value) {
            field = value
            updateActionIconVisibility()
        }

    /**
     * The equivalent of Swing's Action (ButtonBase.Command). A click calls [WCommand.execute] with
     * [commandParameter], and the card is disabled too when WCommand.isEnabled=false.
     */
    var command: WCommandBase? = null
        set(value) {
            field = value
            buttonBase.call(XamlInterop.IButtonBase_put_Command, value?.commandPtr)
        }

    /** The argument passed when [command] runs (ButtonBase.CommandParameter). */
    var commandParameter: String? = null
        set(value) {
            field = value
            if (value == null) {
                buttonBase.call(XamlInterop.IButtonBase_put_CommandParameter, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                buttonBase.call(XamlInterop.IButtonBase_put_CommandParameter, boxed.ptr)
                boxed.release()
            }
        }

    init {
        applyStyle(clickable = false)

        val loaded = loadXaml(LAYOUT_XAML)
        layoutRoot = own(loaded.queryInterface(XamlInterop.IID_IFrameworkElement))
        layoutGrid = own(loaded.queryInterface(XamlInterop.IID_IGrid))
        val contentControl = inspectable.queryInterface(XamlInterop.IID_IContentControl)
        contentControl.call(XamlInterop.IContentControl_put_Content, loaded.ptr)
        contentControl.release()
        loaded.release()

        headerText = own(findName("HeaderText").queryAndRelease(XamlInterop.IID_ITextBlock))
        descriptionText = own(findName("DescriptionText").queryAndRelease(XamlInterop.IID_ITextBlock))
        headerIconText = own(findName("HeaderIconText").queryAndRelease(XamlInterop.IID_ITextBlock))
        headerIconHolder = own(findName("HeaderIconHolder").queryAndRelease(XamlInterop.IID_IUIElement))
        headerPanel = own(findName("HeaderPanel").queryAndRelease(XamlInterop.IID_IFrameworkElement))
        contentHolder = own(findName("ContentHolder").queryAndRelease(XamlInterop.IID_IBorder))
        contentHolderElement = own(contentHolder.queryInterface(XamlInterop.IID_IFrameworkElement))
        actionIconText = own(findName("ActionIconText").queryAndRelease(XamlInterop.IID_ITextBlock))
        actionIconHolder = own(findName("ActionIconHolder").queryAndRelease(XamlInterop.IID_IUIElement))

        if (header.isNotEmpty()) this.header = header
        if (description.isNotEmpty()) this.description = description

        // Automatic wrapping based on width (the Toolkit uses ControlSizeTrigger; here it is reproduced with SizeChanged)
        addSizeChangedListener {
            if (computeLayoutState() != appliedState) applyLayoutState()
        }
    }

    /** Subscribes to clicks on the card. Use it when [isClickEnabled] is on. */
    @JvmSynthetic
    fun addActionListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addActionListenerForJava(adapter)
        clickTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: Runnable) {
        val token = buttonBase.addEventHandler(
            "WinUI4K.ClickHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IButtonBase_add_Click,
        ) { _, _ -> listener.run() }
        clickTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered with [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: () -> Unit) {
        val adapter = clickTokens.removeKotlinAdapter(listener) ?: return
        removeActionListenerForJava(adapter)
    }

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: Runnable) {
        val token = clickTokens.remove(listener) ?: return
        buttonBase.removeEventHandler(XamlInterop.IButtonBase_remove_Click, token)
    }

    /**
     * The content placement state (equivalent to the Toolkit's ContentAlignmentStates).
     * The RIGHT family is determined from the card's actual width using the same thresholds as the Toolkit.
     * Each value carries the contents of the Toolkit's VisualState setters as attributes.
     */
    private enum class LayoutState(
        /** Whether the content goes in the row below the header (row 1, column 1). false means the right edge (row 0, column 2). */
        val wrapsContent: Boolean,
        /** Whether the header and description (HeaderPanel) are hidden. */
        val hidesHeaderPanel: Boolean,
        /** Whether the header icon is hidden. */
        val hidesHeaderIcon: Boolean,
        /** The alignment of the content. */
        val contentPlacement: HorizontalAlignment,
        /** The space to the right of the heading (set to 0 while wrapped, where it shares a column with the content). */
        val headerMarginRight: Double,
        /** Whether a row gap is allowed between the heading and the content below it (ContentSpacing). */
        val allowsContentSpacing: Boolean,
    ) {
        RIGHT(false, false, false, HorizontalAlignment.RIGHT, 24.0, false),
        RIGHT_WRAPPED(true, false, false, HorizontalAlignment.STRETCH, 0.0, true),
        RIGHT_WRAPPED_NO_ICON(true, false, true, HorizontalAlignment.STRETCH, 0.0, true),
        LEFT(true, true, true, HorizontalAlignment.LEFT, 24.0, false),
        VERTICAL(true, false, false, HorizontalAlignment.STRETCH, 24.0, true),
    }

    /** Works out the layout state to apply from the current [contentAlignment] and the card width. */
    private fun computeLayoutState(): LayoutState = when (contentAlignment) {
        ContentAlignment.LEFT -> LayoutState.LEFT
        ContentAlignment.VERTICAL -> LayoutState.VERTICAL
        ContentAlignment.RIGHT -> {
            val width = actualWidth
            when {
                width <= 0.0 -> LayoutState.RIGHT // keep the default arrangement before layout has run
                width < WRAP_NO_ICON_THRESHOLD -> LayoutState.RIGHT_WRAPPED_NO_ICON
                width <= WRAP_THRESHOLD -> LayoutState.RIGHT_WRAPPED
                else -> LayoutState.RIGHT
            }
        }
    }

    /** Reflects the layout state (the set of placements and visibilities of each part; equivalent to the Toolkit's VisualState setters). */
    private fun applyLayoutState() {
        val state = computeLayoutState()
        appliedState = state

        setVisible(headerPanel, !state.hidesHeaderPanel)
        setVisible(headerIconHolder, headerIcon != null && !state.hidesHeaderIcon)

        gridStatics.call(XamlInterop.IGridStatics_SetRow, contentHolderElement.ptr, if (state.wrapsContent) 1 else 0)
        gridStatics.call(XamlInterop.IGridStatics_SetColumn, contentHolderElement.ptr, if (state.wrapsContent) 1 else 2)
        contentHolderElement.call(XamlInterop.IFrameworkElement_put_HorizontalAlignment, state.contentPlacement.native)

        XamlStructs.putThickness(headerPanel, XamlInterop.IFrameworkElement_put_Margin, 0.0, 0.0, state.headerMarginRight, 0.0)

        val needsSpacing = state.allowsContentSpacing && content != null && (header.isNotEmpty() || description.isNotEmpty())
        layoutGrid.call(XamlInterop.IGrid_put_RowSpacing, if (needsSpacing) VERTICAL_CONTENT_SPACING else 0.0)
    }

    /** Applies the equivalent of the Toolkit's SettingsCard style. Whether it is clickable changes the hover / press state definitions. */
    private fun applyStyle(clickable: Boolean) {
        val loaded = loadXaml(styleXaml(clickable))
        val style = loaded.queryInterface(XamlInterop.IID_IStyle)
        loaded.release()
        frameworkElement.call(XamlInterop.IFrameworkElement_put_Style, style.ptr)
        style.release()
    }

    /** The ActionIcon is only shown when the card is clickable and it is set to be visible (the same rule as the Toolkit). */
    private fun updateActionIconVisibility() {
        setVisible(actionIconHolder, isClickEnabled && isActionIconVisible)
    }

    /** Finds the element named [name] in [layoutRoot]'s XAML name scope (FrameworkElement.FindName). */
    private fun findName(name: String): ComPtr =
        Hstring.use(name) { h -> layoutRoot.getPtr(XamlInterop.IFrameworkElement_FindName, h) }

    private companion object {
        /** The card width at which the content wraps from the right edge to the row below (the Toolkit's SettingsCardWrapThreshold). */
        const val WRAP_THRESHOLD = 476.0

        /** The card width at which the header icon is hidden as well (the Toolkit's SettingsCardWrapNoIconThreshold). */
        const val WRAP_NO_ICON_THRESHOLD = 286.0

        /** The row gap between the heading and the content below it (the Toolkit's SettingsCardVerticalHeaderContentSpacing). */
        const val VERTICAL_CONTENT_SPACING = 8.0

        /** The default glyph of the ActionIcon (ChevronRightSmall, the same as the Toolkit's default). */
        const val DEFAULT_ACTION_GLYPH = "\uE974"

        /** Manipulates Grid's attached properties (IGridStatics). Used to move the content when wrapping. */
        val gridStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Grid, XamlInterop.IID_IGridStatics) }

        /** Manipulates ToolTipService's attached properties (for ActionIconToolTip). */
        val toolTipStatics: ComPtr by lazy {
            Activation.factory(XamlInterop.CLS_ToolTipService, XamlInterop.IID_IToolTipServiceStatics)
        }

        /** Toggles the visibility (Collapsed) of the element behind [ptr] (any view of it). */
        fun setVisible(ptr: ComPtr, visible: Boolean) {
            val uiElement = ptr.queryInterface(XamlInterop.IID_IUIElement)
            uiElement.call(XamlInterop.IUIElement_put_Visibility, if (visible) 0 else 1)
            uiElement.release()
        }

        /** Writes [symbol]'s glyph character into the TextBlock [target]. null clears it. */
        fun putGlyph(target: ComPtr, symbol: Symbol?) {
            val glyph = if (symbol == null) "" else symbol.native.toChar().toString()
            Hstring.use(glyph) { h -> target.call(XamlInterop.ITextBlock_put_Text, h) }
        }

        /** Queries an interface and releases the original reference (for the throwaway conversion of FindName's result). */
        fun ComPtr.queryAndRelease(iid: String): ComPtr =
            try {
                queryInterface(iid)
            } finally {
                release()
            }

        /** Turns [xaml] into an object with XamlReader.Load. The caller must release the returned reference. */
        fun loadXaml(xaml: String): ComPtr {
            val statics = Activation.factory(XamlInterop.CLS_XamlReader, XamlInterop.IID_IXamlReaderStatics)
            return try {
                Hstring.use(xaml) { h -> statics.getPtr(XamlInterop.IXamlReaderStatics_Load, h) }
            } finally {
                statics.release()
            }
        }

        /**
         * The card's internal layout (equivalent to the contents of the Toolkit's SettingsCard template).
         * From left to right: HeaderIcon (a 20px Viewbox) / Header + Description stacked vertically /
         * the user content / ActionIcon (a 13px Viewbox).
         * Moving the content when wrapping is done by [applyLayoutState] through attached properties.
         */
        val LAYOUT_XAML = """
            <Grid xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                  xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
                <Grid.RowDefinitions>
                    <RowDefinition Height="*" />
                    <RowDefinition Height="Auto" />
                </Grid.RowDefinitions>
                <Grid.ColumnDefinitions>
                    <ColumnDefinition Width="Auto" />
                    <ColumnDefinition Width="*" />
                    <ColumnDefinition Width="Auto" />
                    <ColumnDefinition Width="Auto" />
                </Grid.ColumnDefinitions>
                <Viewbox x:Name="HeaderIconHolder" MaxWidth="20" MaxHeight="20"
                         Margin="2,0,20,0" Visibility="Collapsed">
                    <TextBlock x:Name="HeaderIconText" FontFamily="{ThemeResource SymbolThemeFontFamily}" />
                </Viewbox>
                <StackPanel x:Name="HeaderPanel" Grid.Column="1" Margin="0,0,24,0" VerticalAlignment="Center">
                    <TextBlock x:Name="HeaderText" TextWrapping="Wrap" Visibility="Collapsed" />
                    <TextBlock x:Name="DescriptionText" TextWrapping="Wrap" FontSize="12"
                               Foreground="{ThemeResource TextFillColorSecondaryBrush}" Visibility="Collapsed" />
                </StackPanel>
                <Border x:Name="ContentHolder" Grid.Column="2"
                        HorizontalAlignment="Right" VerticalAlignment="Center" />
                <Viewbox x:Name="ActionIconHolder" Grid.RowSpan="2" Grid.Column="3" MaxWidth="13" MaxHeight="13"
                         Margin="14,0,0,0" VerticalAlignment="Center" Visibility="Collapsed">
                    <TextBlock x:Name="ActionIconText" FontFamily="{ThemeResource SymbolThemeFontFamily}" Text="&#xE974;" />
                </Viewbox>
            </Grid>
        """.trimIndent()

        /**
         * Builds a Style equivalent to the Toolkit's DefaultSettingsCardStyle.
         * The colors and dimensions are copied straight from the theme resource definitions in
         * SettingsCard.xaml (SettingsCardBackground = CardBackgroundFillColorDefaultBrush and so on).
         * The PointerOver / Pressed states are only defined when [clickable] is true
         * (a GoToState for a state that does not exist is ignored, so the background does not change
         * when the card is not clickable).
         */
        @Suppress("LongMethod") // the body is a declarative XAML literal copied from the Toolkit's style definition
        fun styleXaml(clickable: Boolean): String {
            val pointerStates = if (!clickable) {
                ""
            } else {
                """
                <VisualState x:Name="PointerOver">
                    <VisualState.Setters>
                        <Setter Target="RootGrid.Background" Value="{ThemeResource ControlFillColorSecondaryBrush}" />
                        <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource ControlElevationBorderBrush}" />
                    </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                    <VisualState.Setters>
                        <Setter Target="RootGrid.Background" Value="{ThemeResource ControlFillColorTertiaryBrush}" />
                        <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource ControlStrokeColorDefaultBrush}" />
                        <Setter Target="Presenter.Foreground" Value="{ThemeResource TextFillColorSecondaryBrush}" />
                    </VisualState.Setters>
                </VisualState>
                """
            }
            return """
                <Style xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                       xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
                       TargetType="Button">
                    <Setter Property="Background" Value="{ThemeResource CardBackgroundFillColorDefaultBrush}" />
                    <Setter Property="Foreground" Value="{ThemeResource TextFillColorPrimaryBrush}" />
                    <Setter Property="BorderBrush" Value="{ThemeResource CardStrokeColorDefaultBrush}" />
                    <Setter Property="BorderThickness" Value="1" />
                    <Setter Property="CornerRadius" Value="{ThemeResource ControlCornerRadius}" />
                    <Setter Property="MinHeight" Value="68" />
                    <Setter Property="MinWidth" Value="148" />
                    <Setter Property="Padding" Value="16" />
                    <Setter Property="HorizontalAlignment" Value="Stretch" />
                    <Setter Property="HorizontalContentAlignment" Value="Stretch" />
                    <Setter Property="IsTabStop" Value="${if (clickable) "True" else "False"}" />
                    <Setter Property="UseSystemFocusVisuals" Value="True" />
                    <Setter Property="FocusVisualMargin" Value="-3" />
                    <Setter Property="Template">
                        <Setter.Value>
                            <ControlTemplate TargetType="Button">
                                <Grid x:Name="RootGrid"
                                      Background="{TemplateBinding Background}"
                                      BackgroundSizing="InnerBorderEdge"
                                      BorderBrush="{TemplateBinding BorderBrush}"
                                      BorderThickness="{TemplateBinding BorderThickness}"
                                      CornerRadius="{TemplateBinding CornerRadius}"
                                      Padding="{TemplateBinding Padding}">
                                    <VisualStateManager.VisualStateGroups>
                                        <VisualStateGroup x:Name="CommonStates">
                                            <VisualState x:Name="Normal" />
                                            $pointerStates
                                            <VisualState x:Name="Disabled">
                                                <VisualState.Setters>
                                                    <Setter Target="RootGrid.Background" Value="{ThemeResource ControlFillColorDisabledBrush}" />
                                                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource ControlStrokeColorDefaultBrush}" />
                                                    <Setter Target="Presenter.Foreground" Value="{ThemeResource TextFillColorDisabledBrush}" />
                                                </VisualState.Setters>
                                            </VisualState>
                                        </VisualStateGroup>
                                    </VisualStateManager.VisualStateGroups>
                                    <ContentPresenter x:Name="Presenter"
                                                      Content="{TemplateBinding Content}"
                                                      Foreground="{TemplateBinding Foreground}"
                                                      HorizontalAlignment="Stretch"
                                                      VerticalAlignment="Center" />
                                </Grid>
                            </ControlTemplate>
                        </Setter.Value>
                    </Setter>
                </Style>
            """.trimIndent()
        }
    }
}
