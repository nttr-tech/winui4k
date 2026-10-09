package com.appkitbox.winui4k

/**
 * Shared styles for the ribbon controls (Themes/Shared.xaml, Items.xaml, Ribbon.xaml, and Inputs.xaml in RibbonSpace).
 *
 * RibbonSpace's templates target its own control types (such as RibbonButton), but here they are rebuilt to target the
 * native Button / ToggleButton / ContentControl. The content (icon and label) is built as XAML by Kotlin and put into a
 * ContentPresenter, and states (selected, checked, input hover and focus, and so on) are switched with
 * VisualStateManager.GoToState. All colors refer to ThemeResource (the brushes of RibbonThemeResources).
 */
@Suppress("LargeClass") // The body is the theme's declarative XAML literals
internal object RibbonStyles {
    /** The same styles as RibbonSpace's Shared.xaml (they target native types, so they can be used as is). */
    private const val SHARED = """
  <Style x:Key="RibbonChromeButtonStyle" TargetType="Button">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="BorderThickness" Value="0" />
    <Setter Property="Padding" Value="6,0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="CornerRadius" Value="4" />
    <Setter Property="VerticalAlignment" Value="Stretch" />
    <Setter Property="HorizontalContentAlignment" Value="Center" />
    <Setter Property="VerticalContentAlignment" Value="Center" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="Button">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" CornerRadius="{TemplateBinding CornerRadius}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="ActiveStates">
                <VisualState x:Name="Inactive" />
                <VisualState x:Name="Active">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <ContentPresenter x:Name="ContentPresenter"
                              Content="{TemplateBinding Content}"
                              ContentTemplate="{TemplateBinding ContentTemplate}"
                              Padding="{TemplateBinding Padding}"
                              Foreground="{TemplateBinding Foreground}"
                              HorizontalContentAlignment="{TemplateBinding HorizontalContentAlignment}"
                              VerticalContentAlignment="{TemplateBinding VerticalContentAlignment}" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonScrollButtonStyle" TargetType="Button" BasedOn="{StaticResource RibbonChromeButtonStyle}">
    <Setter Property="Background" Value="{ThemeResource RibbonScrollButtonBackgroundBrush}" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="CornerRadius" Value="4" />
  </Style>

  <Style x:Key="RibbonApplicationButtonStyle" TargetType="Button" BasedOn="{StaticResource RibbonChromeButtonStyle}">
    <Setter Property="Foreground" Value="{ThemeResource RibbonTabForegroundBrush}" />
    <Setter Property="Padding" Value="11,0" />
    <Setter Property="Margin" Value="0,3,2,1" />
  </Style>

  <Style x:Key="RibbonDialogLauncherButtonStyle" TargetType="Button" BasedOn="{StaticResource RibbonChromeButtonStyle}">
    <Setter Property="Width" Value="16" />
    <Setter Property="Height" Value="15" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="CornerRadius" Value="3" />
    <Setter Property="VerticalAlignment" Value="Center" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonSecondaryForegroundBrush}" />
    <Setter Property="FontFamily" Value="{ThemeResource SymbolThemeFontFamily}" />
    <Setter Property="FontSize" Value="9" />
    <Setter Property="Content" Value="&#xE8A7;" />
  </Style>

  <Style x:Key="RibbonSplitPartButtonStyle" TargetType="Button" BasedOn="{StaticResource RibbonChromeButtonStyle}">
    <Setter Property="Padding" Value="0" />
    <Setter Property="CornerRadius" Value="3" />
    <Setter Property="HorizontalAlignment" Value="Stretch" />
    <Setter Property="VerticalAlignment" Value="Stretch" />
    <Setter Property="HorizontalContentAlignment" Value="Stretch" />
    <Setter Property="VerticalContentAlignment" Value="Stretch" />
  </Style>

  <Style x:Key="RibbonBackstageNavButtonStyle" TargetType="Button">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonBackstagePaneForegroundBrush}" />
    <Setter Property="BorderThickness" Value="0" />
    <Setter Property="Padding" Value="18,0,12,0" />
    <Setter Property="Height" Value="40" />
    <Setter Property="FontSize" Value="14" />
    <Setter Property="HorizontalAlignment" Value="Stretch" />
    <Setter Property="HorizontalContentAlignment" Value="Left" />
    <Setter Property="VerticalContentAlignment" Value="Center" />
    <Setter Property="Margin" Value="6,1" />
    <Setter Property="CornerRadius" Value="4" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="Button">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" CornerRadius="{TemplateBinding CornerRadius}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="Hover.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="Hover.Opacity" Value="1" />
                    <Setter Target="ContentPresenter.Opacity" Value="0.8" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="ContentPresenter.Opacity" Value="0.45" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="SelectionStates">
                <VisualState x:Name="Unselected" />
                <VisualState x:Name="Selected">
                  <VisualState.Setters>
                    <Setter Target="Indicator.Opacity" Value="1" />
                    <Setter Target="SelectedFill.Opacity" Value="1" />
                    <Setter Target="ContentPresenter.FontWeight" Value="SemiBold" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Border x:Name="SelectedFill" Opacity="0" Background="{ThemeResource RibbonBackstagePaneSelectedBrush}" CornerRadius="{TemplateBinding CornerRadius}" />
            <Border x:Name="Hover" Opacity="0" Background="{ThemeResource RibbonBackstagePaneHoverBrush}" CornerRadius="{TemplateBinding CornerRadius}" />
            <Rectangle x:Name="Indicator" Width="3" Height="18" RadiusX="1.5" RadiusY="1.5" HorizontalAlignment="Left" Margin="4,0,0,0" Opacity="0" Fill="{ThemeResource RibbonBackstagePaneForegroundBrush}" />
            <ContentPresenter x:Name="ContentPresenter"
                              Content="{TemplateBinding Content}"
                              Padding="{TemplateBinding Padding}"
                              Foreground="{TemplateBinding Foreground}"
                              HorizontalContentAlignment="{TemplateBinding HorizontalContentAlignment}"
                              VerticalContentAlignment="{TemplateBinding VerticalContentAlignment}" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonMenuItemButtonStyle" TargetType="Button" BasedOn="{StaticResource RibbonChromeButtonStyle}">
    <Setter Property="Padding" Value="8,4" />
    <Setter Property="MinHeight" Value="26" />
    <Setter Property="HorizontalAlignment" Value="Stretch" />
    <Setter Property="HorizontalContentAlignment" Value="Left" />
  </Style>

  <Style x:Key="RibbonSpinButtonStyle" TargetType="RepeatButton">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="BorderThickness" Value="0" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="HorizontalAlignment" Value="Stretch" />
    <Setter Property="VerticalAlignment" Value="Stretch" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="RepeatButton">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled" />
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <ContentPresenter Content="{TemplateBinding Content}" Foreground="{TemplateBinding Foreground}" HorizontalAlignment="Center" VerticalAlignment="Center" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonGalleryScrollButtonStyle" TargetType="Button" BasedOn="{StaticResource RibbonChromeButtonStyle}">
    <Setter Property="Padding" Value="0" />
    <Setter Property="CornerRadius" Value="0" />
    <Setter Property="HorizontalAlignment" Value="Stretch" />
    <Setter Property="VerticalAlignment" Value="Stretch" />
    <Setter Property="IsTabStop" Value="False" />
  </Style>

  <Style x:Key="RibbonGalleryItemStyle" TargetType="Button">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="BorderBrush" Value="Transparent" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="CornerRadius" Value="3" />
    <Setter Property="HorizontalContentAlignment" Value="Stretch" />
    <Setter Property="VerticalContentAlignment" Value="Stretch" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="Button">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}" CornerRadius="{TemplateBinding CornerRadius}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Opacity" Value="0.4" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="SelectionStates">
                <VisualState x:Name="Unselected" />
                <VisualState x:Name="Selected">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource RibbonGalleryItemSelectedBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <ContentPresenter Content="{TemplateBinding Content}" Foreground="{TemplateBinding Foreground}" HorizontalContentAlignment="Stretch" VerticalContentAlignment="Stretch" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonSwatchButtonStyle" TargetType="Button">
    <Setter Property="BorderBrush" Value="{ThemeResource RibbonSwatchBorderBrush}" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="FocusVisualMargin" Value="-3" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="Button">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}" CornerRadius="2">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="Hover.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="Hover.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="SelectionStates">
                <VisualState x:Name="Unselected" />
                <VisualState x:Name="Selected">
                  <VisualState.Setters>
                    <Setter Target="SelectedRing.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Border x:Name="SelectedRing" Opacity="0" BorderBrush="{ThemeResource RibbonAccentBrush}" BorderThickness="2" Margin="-2" CornerRadius="3">
              <Border BorderBrush="{ThemeResource RibbonPopupBackgroundBrush}" BorderThickness="1" CornerRadius="1" />
            </Border>
            <Border x:Name="Hover" Opacity="0" BorderBrush="{ThemeResource RibbonAccentBrush}" BorderThickness="2" Margin="-2" CornerRadius="3" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonMenuFlyoutPresenterStyle" TargetType="MenuFlyoutPresenter">
    <Setter Property="Background" Value="{ThemeResource RibbonPopupBackgroundBrush}" />
    <Setter Property="BorderBrush" Value="{ThemeResource RibbonPopupBorderBrush}" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="CornerRadius" Value="{ThemeResource RibbonPopupCornerRadius}" />
  </Style>

  <Style x:Key="RibbonToolTipStyle" TargetType="ToolTip">
    <Setter Property="Background" Value="{ThemeResource RibbonScreenTipBackgroundBrush}" />
    <Setter Property="BorderBrush" Value="{ThemeResource RibbonScreenTipBorderBrush}" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="CornerRadius" Value="{ThemeResource RibbonPopupCornerRadius}" />
    <Setter Property="Padding" Value="9,7,9,8" />
    <Setter Property="MaxWidth" Value="480" />
  </Style>

  <Style x:Key="RibbonFlyoutPresenterStyle" TargetType="FlyoutPresenter">
    <Setter Property="Background" Value="{ThemeResource RibbonPopupBackgroundBrush}" />
    <Setter Property="BorderBrush" Value="{ThemeResource RibbonPopupBorderBrush}" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="Padding" Value="4" />
    <Setter Property="CornerRadius" Value="{ThemeResource RibbonPopupCornerRadius}" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MaxWidth" Value="1200" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="MaxHeight" Value="900" />
    <Setter Property="ScrollViewer.HorizontalScrollBarVisibility" Value="Disabled" />
    <Setter Property="ScrollViewer.VerticalScrollBarVisibility" Value="Disabled" />
  </Style>
"""

    /**
     * A flyout without a border or padding (popups for collapsed groups and the minimized ribbon; the content draws its
     * own border).
     */
    private const val BARE_FLYOUT = """
  <Style x:Key="RibbonBareFlyoutPresenterStyle" TargetType="FlyoutPresenter">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="BorderThickness" Value="0" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="CornerRadius" Value="0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MaxWidth" Value="10000" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="MaxHeight" Value="10000" />
    <Setter Property="ScrollViewer.HorizontalScrollBarVisibility" Value="Disabled" />
    <Setter Property="ScrollViewer.VerticalScrollBarVisibility" Value="Disabled" />
    <Setter Property="IsDefaultShadowEnabled" Value="False" />
  </Style>
"""

    /**
     * The button for ribbon items (equivalent to DefaultRibbonButtonStyle in RibbonSpace). Kotlin builds the content.
     * The disabled text color applies to TextBlocks that inherit the ContentPresenter's Foreground (Kotlin changes the
     * icon colors).
     */
    private const val ITEMS = """
  <Style x:Key="RibbonItemButtonStyle" TargetType="Button">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="BorderBrush" Value="Transparent" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="HorizontalContentAlignment" Value="Stretch" />
    <Setter Property="VerticalContentAlignment" Value="Stretch" />
    <Setter Property="CornerRadius" Value="{ThemeResource RibbonControlCornerRadius}" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="Button">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}" CornerRadius="{TemplateBinding CornerRadius}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource RibbonItemBorderHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="ActiveStates">
                <VisualState x:Name="Inactive" />
                <VisualState x:Name="Active">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <ContentPresenter x:Name="ContentPresenter" Content="{TemplateBinding Content}" Foreground="{TemplateBinding Foreground}" HorizontalContentAlignment="Stretch" VerticalContentAlignment="Stretch" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonItemToggleButtonStyle" TargetType="ToggleButton">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="BorderBrush" Value="Transparent" />
    <Setter Property="BorderThickness" Value="1" />
    <Setter Property="Padding" Value="0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="HorizontalContentAlignment" Value="Stretch" />
    <Setter Property="VerticalContentAlignment" Value="Stretch" />
    <Setter Property="CornerRadius" Value="{ThemeResource RibbonControlCornerRadius}" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ToggleButton">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}" CornerRadius="{TemplateBinding CornerRadius}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Checked">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemCheckedBrush}" />
                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource RibbonItemCheckedBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="CheckedPointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemCheckedHoverBrush}" />
                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource RibbonItemCheckedBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="CheckedPressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource RibbonItemCheckedBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="CheckedDisabled">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemCheckedBrush}" />
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Indeterminate">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.BorderBrush" Value="{ThemeResource RibbonItemCheckedBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="IndeterminatePointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="IndeterminatePressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="IndeterminateDisabled">
                  <VisualState.Setters>
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <ContentPresenter x:Name="ContentPresenter" Content="{TemplateBinding Content}" Foreground="{TemplateBinding Foreground}" HorizontalContentAlignment="Stretch" VerticalContentAlignment="Stretch" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonCheckBoxStyle" TargetType="ToggleButton">
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="Padding" Value="4,0,6,0" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="CornerRadius" Value="{ThemeResource RibbonControlCornerRadius}" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ToggleButton">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" CornerRadius="{TemplateBinding CornerRadius}" Padding="{TemplateBinding Padding}">
            <Grid.ColumnDefinitions>
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
            </Grid.ColumnDefinitions>
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Checked">
                  <VisualState.Setters>
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="Box.BorderBrush" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="CheckGlyph.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="CheckedPointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="Box.BorderBrush" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="CheckGlyph.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="CheckedPressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="Box.BorderBrush" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="CheckGlyph.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="CheckedDisabled">
                  <VisualState.Setters>
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                    <Setter Target="Box.BorderBrush" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                    <Setter Target="CheckGlyph.Opacity" Value="1" />
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Indeterminate">
                  <VisualState.Setters>
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="Box.BorderBrush" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="IndeterminateMark.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="IndeterminatePointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemHoverBrush}" />
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="Box.BorderBrush" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="IndeterminateMark.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="IndeterminatePressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                    <Setter Target="Box.Background" Value="{ThemeResource RibbonAccentBrush}" />
                    <Setter Target="IndeterminateMark.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="IndeterminateDisabled">
                  <VisualState.Setters>
                    <Setter Target="IndeterminateMark.Opacity" Value="1" />
                    <Setter Target="ContentPresenter.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Grid Width="14" Height="14" VerticalAlignment="Center">
              <Border x:Name="Box" CornerRadius="3" BorderThickness="1" BorderBrush="{ThemeResource RibbonInputHoverBorderBrush}" Background="{ThemeResource RibbonInputBackgroundBrush}" />
              <FontIcon x:Name="CheckGlyph" Glyph="&#xE73E;" FontSize="10" Opacity="0" Foreground="{ThemeResource RibbonAccentForegroundBrush}" />
              <Rectangle x:Name="IndeterminateMark" Width="7" Height="2" Opacity="0" Fill="{ThemeResource RibbonAccentForegroundBrush}" />
            </Grid>
            <ContentPresenter x:Name="ContentPresenter" Grid.Column="1" Margin="6,0,0,0" Content="{TemplateBinding Content}" Foreground="{TemplateBinding Foreground}" VerticalAlignment="Center" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonSplitButtonHostStyle" TargetType="ContentControl">
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="IsTabStop" Value="False" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ContentControl">
          <Grid x:Name="PART_Root" CornerRadius="{ThemeResource RibbonControlCornerRadius}" BorderThickness="1" BorderBrush="Transparent" Background="Transparent">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CheckStates">
                <VisualState x:Name="Unchecked" />
                <VisualState x:Name="Checked">
                  <VisualState.Setters>
                    <Setter Target="PART_Root.Background" Value="{ThemeResource RibbonItemCheckedBrush}" />
                    <Setter Target="PART_Root.BorderBrush" Value="{ThemeResource RibbonItemCheckedBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="DropDownStates">
                <VisualState x:Name="DropDownClosed" />
                <VisualState x:Name="DropDownOpen">
                  <VisualState.Setters>
                    <Setter Target="PART_SecondaryButton.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Button x:Name="PART_PrimaryButton" Style="{StaticResource RibbonSplitPartButtonStyle}" />
            <Button x:Name="PART_SecondaryButton" Style="{StaticResource RibbonSplitPartButtonStyle}" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>

  <Style x:Key="RibbonTabHeaderStyle" TargetType="Button">
    <Setter Property="Background" Value="Transparent" />
    <Setter Property="Foreground" Value="{ThemeResource RibbonTabForegroundBrush}" />
    <Setter Property="BorderThickness" Value="0" />
    <Setter Property="Padding" Value="10,0" />
    <Setter Property="MinWidth" Value="0" />
    <Setter Property="MinHeight" Value="0" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="VerticalAlignment" Value="Stretch" />
    <Setter Property="UseSystemFocusVisuals" Value="True" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="Button">
          <Grid x:Name="RootGrid" Background="{TemplateBinding Background}" CornerRadius="{ThemeResource RibbonTabCornerRadius}" Margin="{ThemeResource RibbonTabMargin}">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="CommonStates">
                <VisualState x:Name="Normal" />
                <VisualState x:Name="PointerOver">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonTabHoverBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Pressed">
                  <VisualState.Setters>
                    <Setter Target="RootGrid.Background" Value="{ThemeResource RibbonItemPressedBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="Disabled">
                  <VisualState.Setters>
                    <Setter Target="PART_Text.Opacity" Value="0.5" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="SelectionStates">
                <VisualState x:Name="Unselected" />
                <VisualState x:Name="Selected">
                  <VisualState.Setters>
                    <Setter Target="PART_Text.Foreground" Value="{ThemeResource RibbonTabSelectedForegroundBrush}" />
                    <Setter Target="PART_Text.FontWeight" Value="SemiBold" />
                    <Setter Target="PART_Indicator.Opacity" Value="1" />
                    <Setter Target="PART_SelectedBackground.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="SelectedContextual">
                  <VisualState.Setters>
                    <Setter Target="PART_Text.FontWeight" Value="SemiBold" />
                    <Setter Target="PART_ContextualIndicator.Opacity" Value="1" />
                    <Setter Target="PART_SelectedBackground.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
              <VisualStateGroup x:Name="ContextualStates">
                <VisualState x:Name="Regular" />
                <VisualState x:Name="Contextual">
                  <VisualState.Setters>
                    <Setter Target="PART_ContextualBand.Opacity" Value="1" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Border x:Name="PART_SelectedBackground" Opacity="0" Background="{ThemeResource RibbonTabSelectedBackgroundBrush}" CornerRadius="{ThemeResource RibbonTabCornerRadius}" />
            <Rectangle x:Name="PART_ContextualBand" Height="3" VerticalAlignment="Top" Margin="6,0" RadiusX="1.5" RadiusY="1.5" Opacity="0" Fill="{TemplateBinding BorderBrush}" />
            <ContentPresenter x:Name="PART_Text"
                              Content="{TemplateBinding Content}"
                              Foreground="{TemplateBinding Foreground}"
                              Margin="{TemplateBinding Padding}"
                              HorizontalAlignment="Center"
                              VerticalAlignment="Center" />
            <Rectangle x:Name="PART_Indicator" Height="3" VerticalAlignment="Bottom" Margin="10,0,10,1" RadiusX="1.5" RadiusY="1.5" Opacity="0" Fill="{ThemeResource RibbonTabIndicatorBrush}" />
            <Rectangle x:Name="PART_ContextualIndicator" Height="3" VerticalAlignment="Bottom" Margin="10,0,10,1" RadiusX="1.5" RadiusY="1.5" Opacity="0" Fill="{TemplateBinding BorderBrush}" />
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>
"""

    /**
     * Templates for inputs (combo boxes, spinners, text boxes, sliders) (equivalent to Inputs.xaml in RibbonSpace).
     * Kotlin finds the parts (PART_*) by name from the template root to operate them, and switches the InputStates.
     */
    private const val INPUTS = """
  <Style x:Key="RibbonComboBoxHostStyle" TargetType="ContentControl">
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="IsTabStop" Value="False" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ContentControl">
          <Grid x:Name="PART_Root">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="InputStates">
                <VisualState x:Name="InputNormal" />
                <VisualState x:Name="InputPointerOver">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputHoverBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputFocused">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputFocusBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputDisabled">
                  <VisualState.Setters>
                    <Setter Target="PART_Label.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                    <Setter Target="PART_InputBorder.Opacity" Value="0.6" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Grid.ColumnDefinitions>
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
            </Grid.ColumnDefinitions>
            <Border x:Name="PART_Icon" Margin="3,0,4,0" VerticalAlignment="Center" Visibility="Collapsed" />
            <TextBlock x:Name="PART_Label" Grid.Column="1" VerticalAlignment="Center" Margin="0,0,6,0" Foreground="{TemplateBinding Foreground}" Visibility="Collapsed" />
            <Border x:Name="PART_InputBorder" Grid.Column="2" Background="{ThemeResource RibbonInputBackgroundBrush}" BorderBrush="{ThemeResource RibbonInputBorderBrush}" BorderThickness="1" CornerRadius="{ThemeResource RibbonControlCornerRadius}">
              <Grid>
                <Grid.ColumnDefinitions>
                  <ColumnDefinition Width="*" />
                  <ColumnDefinition Width="Auto" />
                </Grid.ColumnDefinitions>
                <TextBox x:Name="PART_TextBox" VerticalAlignment="Center" FontSize="{TemplateBinding FontSize}" MinWidth="0" MinHeight="0">
                  <TextBox.Resources>
                    <x:Double x:Key="TextControlThemeMinHeight">18</x:Double>
                    <x:Double x:Key="TextControlThemeMinWidth">20</x:Double>
                    <Thickness x:Key="TextControlThemePadding">5,1,4,1</Thickness>
                    <Thickness x:Key="TextControlBorderThemeThickness">0</Thickness>
                    <Thickness x:Key="TextControlBorderThemeThicknessFocused">0</Thickness>
                    <SolidColorBrush x:Key="TextControlBackground" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundPointerOver" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundFocused" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundDisabled" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrush" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrushPointerOver" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrushFocused" Color="Transparent" />
                  </TextBox.Resources>
                </TextBox>
                <Border x:Name="PART_SelectionBox" Visibility="Collapsed" IsHitTestVisible="False" VerticalAlignment="Center" HorizontalAlignment="Stretch" Margin="6,0,2,0" />
                <Button x:Name="PART_DropDownButton" Grid.Column="1" Style="{StaticResource RibbonChromeButtonStyle}" Width="18" Padding="0" CornerRadius="0,3,3,0" IsTabStop="False">
                  <FontIcon Glyph="&#xE70D;" FontSize="8" />
                </Button>
              </Grid>
            </Border>
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>
  <Style x:Key="RibbonSpinnerHostStyle" TargetType="ContentControl">
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="IsTabStop" Value="False" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ContentControl">
          <Grid x:Name="PART_Root">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="InputStates">
                <VisualState x:Name="InputNormal" />
                <VisualState x:Name="InputPointerOver">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputHoverBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputFocused">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputFocusBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputDisabled">
                  <VisualState.Setters>
                    <Setter Target="PART_Label.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                    <Setter Target="PART_InputBorder.Opacity" Value="0.6" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Grid.ColumnDefinitions>
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
            </Grid.ColumnDefinitions>
            <Border x:Name="PART_Icon" Margin="3,0,4,0" VerticalAlignment="Center" Visibility="Collapsed" />
            <TextBlock x:Name="PART_Label" Grid.Column="1" VerticalAlignment="Center" Margin="0,0,6,0" Foreground="{TemplateBinding Foreground}" Visibility="Collapsed" />
            <Border x:Name="PART_InputBorder" Grid.Column="2" Background="{ThemeResource RibbonInputBackgroundBrush}" BorderBrush="{ThemeResource RibbonInputBorderBrush}" BorderThickness="1" CornerRadius="{ThemeResource RibbonControlCornerRadius}">
              <Grid>
                <Grid.ColumnDefinitions>
                  <ColumnDefinition Width="*" />
                  <ColumnDefinition Width="Auto" />
                </Grid.ColumnDefinitions>
                <TextBox x:Name="PART_TextBox" VerticalAlignment="Center" FontSize="{TemplateBinding FontSize}" MinWidth="0" MinHeight="0">
                  <TextBox.Resources>
                    <x:Double x:Key="TextControlThemeMinHeight">18</x:Double>
                    <x:Double x:Key="TextControlThemeMinWidth">20</x:Double>
                    <Thickness x:Key="TextControlThemePadding">5,1,4,1</Thickness>
                    <Thickness x:Key="TextControlBorderThemeThickness">0</Thickness>
                    <Thickness x:Key="TextControlBorderThemeThicknessFocused">0</Thickness>
                    <SolidColorBrush x:Key="TextControlBackground" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundPointerOver" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundFocused" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundDisabled" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrush" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrushPointerOver" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrushFocused" Color="Transparent" />
                  </TextBox.Resources>
                </TextBox>
                <Grid Grid.Column="1" Width="16">
                  <Grid.RowDefinitions>
                    <RowDefinition Height="*" />
                    <RowDefinition Height="*" />
                  </Grid.RowDefinitions>
                  <RepeatButton x:Name="PART_UpButton" Style="{StaticResource RibbonSpinButtonStyle}" IsTabStop="False">
                    <FontIcon Glyph="&#xE70E;" FontSize="7" />
                  </RepeatButton>
                  <RepeatButton x:Name="PART_DownButton" Grid.Row="1" Style="{StaticResource RibbonSpinButtonStyle}" IsTabStop="False">
                    <FontIcon Glyph="&#xE70D;" FontSize="7" />
                  </RepeatButton>
                </Grid>
              </Grid>
            </Border>
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>
  <Style x:Key="RibbonTextBoxHostStyle" TargetType="ContentControl">
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="IsTabStop" Value="False" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ContentControl">
          <Grid x:Name="PART_Root">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="InputStates">
                <VisualState x:Name="InputNormal" />
                <VisualState x:Name="InputPointerOver">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputHoverBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputFocused">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputFocusBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputDisabled">
                  <VisualState.Setters>
                    <Setter Target="PART_Label.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                    <Setter Target="PART_InputBorder.Opacity" Value="0.6" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Grid.ColumnDefinitions>
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
            </Grid.ColumnDefinitions>
            <Border x:Name="PART_Icon" Margin="3,0,4,0" VerticalAlignment="Center" Visibility="Collapsed" />
            <TextBlock x:Name="PART_Label" Grid.Column="1" VerticalAlignment="Center" Margin="0,0,6,0" Foreground="{TemplateBinding Foreground}" Visibility="Collapsed" />
            <Border x:Name="PART_InputBorder" Grid.Column="2" Background="{ThemeResource RibbonInputBackgroundBrush}" BorderBrush="{ThemeResource RibbonInputBorderBrush}" BorderThickness="1" CornerRadius="{ThemeResource RibbonControlCornerRadius}">
                <TextBox x:Name="PART_TextBox" VerticalAlignment="Center" FontSize="{TemplateBinding FontSize}" MinWidth="0" MinHeight="0">
                  <TextBox.Resources>
                    <x:Double x:Key="TextControlThemeMinHeight">18</x:Double>
                    <x:Double x:Key="TextControlThemeMinWidth">20</x:Double>
                    <Thickness x:Key="TextControlThemePadding">5,1,4,1</Thickness>
                    <Thickness x:Key="TextControlBorderThemeThickness">0</Thickness>
                    <Thickness x:Key="TextControlBorderThemeThicknessFocused">0</Thickness>
                    <SolidColorBrush x:Key="TextControlBackground" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundPointerOver" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundFocused" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBackgroundDisabled" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrush" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrushPointerOver" Color="Transparent" />
                    <SolidColorBrush x:Key="TextControlBorderBrushFocused" Color="Transparent" />
                  </TextBox.Resources>
                </TextBox>
            </Border>
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>
  <Style x:Key="RibbonSliderHostStyle" TargetType="ContentControl">
    <Setter Property="Foreground" Value="{ThemeResource RibbonForegroundBrush}" />
    <Setter Property="FontSize" Value="12" />
    <Setter Property="IsTabStop" Value="False" />
    <Setter Property="HorizontalAlignment" Value="Left" />
    <Setter Property="VerticalAlignment" Value="Top" />
    <Setter Property="Template">
      <Setter.Value>
        <ControlTemplate TargetType="ContentControl">
          <Grid x:Name="PART_Root">
            <VisualStateManager.VisualStateGroups>
              <VisualStateGroup x:Name="InputStates">
                <VisualState x:Name="InputNormal" />
                <VisualState x:Name="InputPointerOver">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputHoverBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputFocused">
                  <VisualState.Setters>
                    <Setter Target="PART_InputBorder.BorderBrush" Value="{ThemeResource RibbonInputFocusBorderBrush}" />
                  </VisualState.Setters>
                </VisualState>
                <VisualState x:Name="InputDisabled">
                  <VisualState.Setters>
                    <Setter Target="PART_Label.Foreground" Value="{ThemeResource RibbonDisabledForegroundBrush}" />
                    <Setter Target="PART_InputBorder.Opacity" Value="0.6" />
                  </VisualState.Setters>
                </VisualState>
              </VisualStateGroup>
            </VisualStateManager.VisualStateGroups>
            <Grid.ColumnDefinitions>
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
              <ColumnDefinition Width="Auto" />
            </Grid.ColumnDefinitions>
            <Border x:Name="PART_Icon" Margin="3,0,4,0" VerticalAlignment="Center" Visibility="Collapsed" />
            <TextBlock x:Name="PART_Label" Grid.Column="1" VerticalAlignment="Center" Margin="0,0,6,0" Foreground="{TemplateBinding Foreground}" Visibility="Collapsed" />
            <Border x:Name="PART_InputBorder" Grid.Column="2" BorderThickness="0">
              <Slider x:Name="PART_Slider" VerticalAlignment="Center" MinHeight="0" Padding="0" />
            </Border>
          </Grid>
        </ControlTemplate>
      </Setter.Value>
    </Setter>
  </Style>
"""

    /**
     * The XAML of the whole resource dictionary of the ribbon theme (per-theme brushes and shapes, and shared styles).
     * [themeDictionaries] is the content of each of the "Light" / "Default" / "Dark" / "HighContrast" dictionaries.
     */
    fun dictionaryXaml(themeDictionaries: Map<String, String>): String = buildString {
        append("<ResourceDictionary ").append(Xaml.NAMESPACES).append(">\n")
        append("  <ResourceDictionary.ThemeDictionaries>\n")
        for ((key, body) in themeDictionaries) {
            append("    <ResourceDictionary x:Key=\"").append(key).append("\">\n")
            append(body)
            append("    </ResourceDictionary>\n")
        }
        append("  </ResourceDictionary.ThemeDictionaries>\n")
        append(SHARED)
        append(BARE_FLYOUT)
        append(ITEMS)
        append(INPUTS)
        append("</ResourceDictionary>")
    }
}
