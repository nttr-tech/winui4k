package jp.hisano.winui4k.winui

import jp.hisano.winui4k.winrt.WinRt

/**
 * WinUI 3 ABI constants (IIDs / vtable slot numbers).
 *
 * All values were mechanically extracted with tools/dump_winmd.py from
 * metadata/Microsoft.UI.Xaml.winmd in Microsoft.WindowsAppSDK.WinUI 2.2.1 (a dependency
 * of Microsoft.WindowsAppSDK 2.2.0), Microsoft.Windows.ApplicationModel.Resources.winmd
 * from the WinAppSDK runtime, and Windows.Foundation.FoundationContract.winmd from the
 * Windows SDK. None of these values are hand-written or guessed.
 * (Cross-checked against the 1.7.260224002 winmd — all existing IIDs / slots match.)
 *
 * Slot numbering convention: IUnknown = 0..2, IInspectable = 3..5, and the interface
 * body starts at 6, following the winmd's method declaration order.
 */
object Abi {
    // ---- Microsoft.UI.Xaml.Application ----
    const val CLS_Application = "Microsoft.UI.Xaml.Application"
    const val IID_IApplicationStatics = "4e0d09f5-4358-512c-a987-503b52848e95"
    const val IApplicationStatics_Start = 7            // vtbl[7] Start(ApplicationInitializationCallback)
    const val IID_IApplicationFactory = "9fd96657-5294-5a65-a1db-4fea143597da"
    const val IApplicationFactory_CreateInstance = 6   // (outer, out inner, out instance)
    const val IID_IApplicationOverrides = "a33e81ef-c665-503b-8827-d27ef1720a06"
    // IApplicationOverrides: vtbl[6] OnLaunched(LaunchActivatedEventArgs)
    const val IID_IApplication = "06a8f4e7-1146-55af-820d-ebd55643b021"
    const val IApplication_get_Resources = 6           // get_Resources(out ResourceDictionary)
    const val IApplication_put_Resources = 7           // put_Resources(ResourceDictionary)

    /** delegate Microsoft.UI.Xaml.ApplicationInitializationCallback — Invoke is vtbl[3] */
    const val IID_ApplicationInitializationCallback = "d8eef1c9-1234-56f1-9963-45dd9c80a661"

    // ---- Microsoft.UI.Xaml.Window ----
    const val CLS_Window = "Microsoft.UI.Xaml.Window"
    const val IID_IWindowFactory = "f0441536-afef-5222-918f-324a9b2dec75"
    const val IID_IWindow = "61f0ec79-5d52-56b5-86fb-40fa4af288b0"
    const val IWindow_put_Content = 9
    const val IWindow_put_Title = 15
    const val IWindow_Activate = 26
    const val IWindow_Close = 27

    // ---- Microsoft.UI.Xaml.UIElement / FrameworkElement ----
    const val IID_IUIElement = "c3c01020-320c-5cf6-9d24-d396bbfa4d8b"
    const val IUIElement_get_XamlRoot = 109            // get_XamlRoot(out XamlRoot)
    const val IUIElement_put_XamlRoot = 110            // put_XamlRoot(XamlRoot)
    const val IUIElement_get_ContextFlyout = 45        // get_ContextFlyout(out FlyoutBase)
    const val IUIElement_put_ContextFlyout = 46        // put_ContextFlyout(FlyoutBase)
    const val IUIElement_get_KeyboardAccelerators = 78 // get_KeyboardAccelerators(out IVector<KeyboardAccelerator>)
    const val IID_IFrameworkElement = "fe08f13d-dc6a-5495-ad44-c2d8d21863b0"
    const val IFrameworkElement_put_Width = 16
    const val IFrameworkElement_put_Height = 18
    const val IFrameworkElement_get_HorizontalAlignment = 27 // get_HorizontalAlignment(out HorizontalAlignment)
    const val IFrameworkElement_put_HorizontalAlignment = 28 // put_HorizontalAlignment(HorizontalAlignment)
    const val IFrameworkElement_get_VerticalAlignment = 29   // get_VerticalAlignment(out VerticalAlignment)
    const val IFrameworkElement_put_VerticalAlignment = 30   // put_VerticalAlignment(VerticalAlignment)
    const val IFrameworkElement_put_Margin = 32        // put_Margin(Thickness) — struct passed by value
    const val IFrameworkElement_put_RequestedTheme = 58 // put_RequestedTheme(ElementTheme)
    const val IFrameworkElement_get_ActualTheme = 60   // get_ActualTheme(out ElementTheme)
    const val IFrameworkElement_add_ActualThemeChanged = 73 // add_ActualThemeChanged(TypedEventHandler<FrameworkElement, Object>, out token)
    const val IFrameworkElement_remove_ActualThemeChanged = 74 // remove_ActualThemeChanged(token)

    // ---- Microsoft.UI.Xaml.Controls.StackPanel ----
    const val CLS_StackPanel = "Microsoft.UI.Xaml.Controls.StackPanel"
    const val IID_IStackPanelFactory = "64c1d388-47a2-5a74-a75b-559d151ee5ac"
    const val IID_IStackPanel = "493ab00b-3a6a-5e4a-9452-407cd5197406"
    const val IStackPanel_get_Orientation = 8          // get_Orientation(out Orientation)
    const val IStackPanel_put_Orientation = 9          // put_Orientation(Orientation)
    const val IStackPanel_get_Spacing = 20
    const val IStackPanel_put_Spacing = 21
    const val IID_IPanel = "27a1b418-56f3-525e-b883-cefed905eed3"
    const val IPanel_get_Children = 6                  // -> IVector<UIElement>

    // ---- Microsoft.UI.Xaml.Controls.Border ----
    const val CLS_Border = "Microsoft.UI.Xaml.Controls.Border"
    const val IID_IBorder = "1ca13b47-ff5c-5abc-a411-a177df9482a9"
    const val IBorder_put_BorderBrush = 7              // put_BorderBrush(Brush)
    const val IBorder_put_BorderThickness = 9          // put_BorderThickness(Thickness) — struct passed by value
    const val IBorder_put_Background = 11              // put_Background(Brush)
    const val IBorder_put_CornerRadius = 15            // put_CornerRadius(CornerRadius) — struct passed by value
    const val IBorder_put_Padding = 17                 // put_Padding(Thickness) — struct passed by value
    const val IBorder_get_Child = 18                   // get_Child(out UIElement)
    const val IBorder_put_Child = 19                   // put_Child(UIElement)

    // ---- Microsoft.UI.Xaml.Media.SolidColorBrush ----
    const val CLS_SolidColorBrush = "Microsoft.UI.Xaml.Media.SolidColorBrush"
    const val IID_ISolidColorBrush = "b3865c31-37c8-55c1-8a72-d41c67642e2a"
    const val ISolidColorBrush_put_Color = 7           // put_Color(Windows.UI.Color) — struct (u8×4: A,R,G,B) passed by value

    // ---- Microsoft.UI.Xaml.Controls.Canvas ----
    const val CLS_Canvas = "Microsoft.UI.Xaml.Controls.Canvas"
    const val IID_ICanvasFactory = "374c5050-3481-5557-9948-804c0b8eea89"
    const val IID_ICanvas = "457ba139-1146-51d2-807e-d9d65c927060" // default interface (no members)
    const val IID_ICanvasStatics = "c00d5e0f-77e3-5c59-8fcd-86761f0c6607"
    const val ICanvasStatics_GetLeft = 7               // GetLeft(UIElement, out DOUBLE)
    const val ICanvasStatics_SetLeft = 8               // SetLeft(UIElement, DOUBLE)
    const val ICanvasStatics_GetTop = 10               // GetTop(UIElement, out DOUBLE)
    const val ICanvasStatics_SetTop = 11               // SetTop(UIElement, DOUBLE)
    const val ICanvasStatics_SetZIndex = 14            // SetZIndex(UIElement, INT32)

    // ---- Microsoft.UI.Xaml.Controls.Expander ----
    const val CLS_Expander = "Microsoft.UI.Xaml.Controls.Expander"
    const val IID_IExpanderFactory = "51a5afc2-b16d-516e-83ae-5a10476b13af"
    const val IID_IExpander = "ca633942-e584-55c2-b7ee-cffc73c8127a"
    const val IExpander_put_Header = 7                 // put_Header(IInspectable)
    const val IExpander_get_IsExpanded = 12            // get_IsExpanded(out boolean)
    const val IExpander_put_IsExpanded = 13            // put_IsExpanded(boolean)
    const val IExpander_get_ExpandDirection = 14       // get_ExpandDirection(out ExpandDirection)
    const val IExpander_put_ExpandDirection = 15       // put_ExpandDirection(ExpandDirection)
    const val IExpander_add_Expanding = 16             // add_Expanding(TypedEventHandler, out token)
    const val IExpander_remove_Expanding = 17          // remove_Expanding(token)
    const val IExpander_add_Collapsed = 18             // add_Collapsed(TypedEventHandler, out token)
    const val IExpander_remove_Collapsed = 19          // remove_Collapsed(token)
    const val IID_IExpanderExpandingEventArgs = "433f2e36-19e7-579c-b4ce-9ce5d510d001"
    const val IID_IExpanderCollapsedEventArgs = "968a6870-7426-535e-a526-279e6eedecd0"

    // ---- Microsoft.UI.Xaml.Controls.Grid ----
    const val CLS_Grid = "Microsoft.UI.Xaml.Controls.Grid"
    const val IID_IGridFactory = "b16bf561-fc6c-57c6-8ebc-0b06ce4513aa"
    const val IID_IGrid = "c4496219-9014-58a1-b4ad-c5044913a5bb"
    const val IGrid_get_RowDefinitions = 6             // get_RowDefinitions(out IVector<RowDefinition>)
    const val IGrid_get_ColumnDefinitions = 7          // get_ColumnDefinitions(out IVector<ColumnDefinition>)
    const val IGrid_get_RowSpacing = 18                // get_RowSpacing(out DOUBLE)
    const val IGrid_put_RowSpacing = 19                // put_RowSpacing(DOUBLE)
    const val IGrid_get_ColumnSpacing = 20             // get_ColumnSpacing(out DOUBLE)
    const val IGrid_put_ColumnSpacing = 21             // put_ColumnSpacing(DOUBLE)
    const val IID_IGridStatics = "ef9cf81d-a431-50f4-abf5-3023fe447704"
    const val IGridStatics_SetRow = 15                 // SetRow(FrameworkElement, INT32)
    const val IGridStatics_SetColumn = 18              // SetColumn(FrameworkElement, INT32)
    const val IGridStatics_SetRowSpan = 21             // SetRowSpan(FrameworkElement, INT32)
    const val IGridStatics_SetColumnSpan = 24          // SetColumnSpan(FrameworkElement, INT32)
    const val CLS_RowDefinition = "Microsoft.UI.Xaml.Controls.RowDefinition"
    const val IID_IRowDefinition = "fe870f2f-89ef-5dac-9f33-968d0dc577c3"
    const val IRowDefinition_put_Height = 7            // put_Height(GridLength) — struct passed by value
    const val CLS_ColumnDefinition = "Microsoft.UI.Xaml.Controls.ColumnDefinition"
    const val IID_IColumnDefinition = "454cea14-87ec-5890-bb62-f1d82a94758e"
    const val IColumnDefinition_put_Width = 7          // put_Width(GridLength) — struct passed by value

    // ---- Microsoft.UI.Xaml.Controls.RelativePanel ----
    const val CLS_RelativePanel = "Microsoft.UI.Xaml.Controls.RelativePanel"
    const val IID_IRelativePanelFactory = "c85f1443-d973-50fd-9497-b867f492468f"
    const val IID_IRelativePanel = "c432fcc4-88f2-59d8-9d0e-a237beaeb07f"
    const val IID_IRelativePanelStatics = "bdd929a2-76cc-59c4-82c1-f14b5da4221a"
    // All the relative-placement Set* methods are SetXxx(UIElement element, Object value) — value is the anchor element
    const val IRelativePanelStatics_SetLeftOf = 9
    const val IRelativePanelStatics_SetAbove = 12
    const val IRelativePanelStatics_SetRightOf = 15
    const val IRelativePanelStatics_SetBelow = 18
    const val IRelativePanelStatics_SetAlignHorizontalCenterWith = 21
    const val IRelativePanelStatics_SetAlignVerticalCenterWith = 24
    const val IRelativePanelStatics_SetAlignLeftWith = 27
    const val IRelativePanelStatics_SetAlignTopWith = 30
    const val IRelativePanelStatics_SetAlignRightWith = 33
    const val IRelativePanelStatics_SetAlignBottomWith = 36
    // All the panel-relative Set* methods are SetXxxWithPanel(UIElement element, boolean value)
    const val IRelativePanelStatics_SetAlignLeftWithPanel = 39
    const val IRelativePanelStatics_SetAlignTopWithPanel = 42
    const val IRelativePanelStatics_SetAlignRightWithPanel = 45
    const val IRelativePanelStatics_SetAlignBottomWithPanel = 48
    const val IRelativePanelStatics_SetAlignHorizontalCenterWithPanel = 51
    const val IRelativePanelStatics_SetAlignVerticalCenterWithPanel = 54

    // ---- Microsoft.UI.Xaml.Controls.SplitView ----
    const val CLS_SplitView = "Microsoft.UI.Xaml.Controls.SplitView"
    const val IID_ISplitViewFactory = "389ece72-75ce-561b-aad3-c52125ca6a50"
    const val IID_ISplitView = "10ae18f7-1666-5897-bbce-1e687e7784a8"
    const val ISplitView_put_Content = 7               // put_Content(UIElement)
    const val ISplitView_put_Pane = 9                  // put_Pane(UIElement)
    const val ISplitView_get_IsPaneOpen = 10           // get_IsPaneOpen(out boolean)
    const val ISplitView_put_IsPaneOpen = 11           // put_IsPaneOpen(boolean)
    const val ISplitView_get_OpenPaneLength = 12       // get_OpenPaneLength(out DOUBLE)
    const val ISplitView_put_OpenPaneLength = 13       // put_OpenPaneLength(DOUBLE)
    const val ISplitView_get_PanePlacement = 16        // get_PanePlacement(out SplitViewPanePlacement)
    const val ISplitView_put_PanePlacement = 17        // put_PanePlacement(SplitViewPanePlacement)
    const val ISplitView_get_DisplayMode = 18          // get_DisplayMode(out SplitViewDisplayMode)
    const val ISplitView_put_DisplayMode = 19          // put_DisplayMode(SplitViewDisplayMode)

    // ---- Microsoft.UI.Xaml.Controls.VariableSizedWrapGrid ----
    const val CLS_VariableSizedWrapGrid = "Microsoft.UI.Xaml.Controls.VariableSizedWrapGrid"
    const val IID_IVariableSizedWrapGrid = "bfecd12b-e16a-58a0-af5f-4672627462d5"
    const val IVariableSizedWrapGrid_put_ItemHeight = 7  // put_ItemHeight(DOUBLE)
    const val IVariableSizedWrapGrid_put_ItemWidth = 9   // put_ItemWidth(DOUBLE)
    const val IVariableSizedWrapGrid_get_Orientation = 10 // get_Orientation(out Orientation)
    const val IVariableSizedWrapGrid_put_Orientation = 11 // put_Orientation(Orientation)
    const val IVariableSizedWrapGrid_get_MaximumRowsOrColumns = 16 // get_MaximumRowsOrColumns(out INT32)
    const val IVariableSizedWrapGrid_put_MaximumRowsOrColumns = 17 // put_MaximumRowsOrColumns(INT32)
    const val IID_IVariableSizedWrapGridStatics = "0d979fe6-64af-5af5-914a-dd38f2ccf2d7"
    const val IVariableSizedWrapGridStatics_SetRowSpan = 14    // SetRowSpan(UIElement, INT32)
    const val IVariableSizedWrapGridStatics_SetColumnSpan = 17 // SetColumnSpan(UIElement, INT32)

    // ---- Microsoft.UI.Xaml.Controls.TextBlock ----
    const val CLS_TextBlock = "Microsoft.UI.Xaml.Controls.TextBlock"
    const val IID_ITextBlock = "1ac8d84f-392c-5c7e-83f5-a53e3bf0abb0"
    const val ITextBlock_get_FontSize = 6
    const val ITextBlock_put_FontSize = 7
    const val ITextBlock_put_FontWeight = 11           // put_FontWeight(FontWeight { u2 Weight } passed by value)
    const val ITextBlock_put_Foreground = 19           // put_Foreground(Brush)
    const val ITextBlock_get_TextWrapping = 20         // get_TextWrapping(out TextWrapping)
    const val ITextBlock_put_TextWrapping = 21         // put_TextWrapping(TextWrapping)
    const val ITextBlock_get_Text = 26
    const val ITextBlock_put_Text = 27

    // ---- Microsoft.UI.Xaml.Controls.TextBox ----
    const val CLS_TextBox = "Microsoft.UI.Xaml.Controls.TextBox"
    const val IID_ITextBoxFactory = "e1d8b82e-bc60-5d27-b646-5ca4c4a69432"
    const val IID_ITextBox = "873af7c2-ab89-5d76-8dbe-3d6325669df5"
    const val ITextBox_get_Text = 6
    const val ITextBox_put_Text = 7
    const val ITextBox_put_PlaceholderText = 35

    // ---- Microsoft.UI.Xaml.Controls.Control ----
    const val IID_IControl = "857d6e8a-d45a-5c69-a99c-bf6a5c54fb38"
    const val IControl_get_IsEnabled = 28              // get_IsEnabled(out boolean)
    const val IControl_put_IsEnabled = 29              // put_IsEnabled(boolean)
    const val IControl_put_FontWeight = 17             // put_FontWeight(FontWeight) — struct passed by value
    const val IControl_put_Padding = 35                // put_Padding(Thickness) — struct passed by value
    const val IControl_put_HorizontalContentAlignment = 37 // put_HorizontalContentAlignment(HorizontalAlignment)
    const val IControl_put_Background = 41             // put_Background(Brush)
    const val IControl_put_BorderThickness = 45        // put_BorderThickness(Thickness) — struct passed by value
    const val IControl_put_BorderBrush = 47            // put_BorderBrush(Brush)
    const val IControl_put_CornerRadius = 53           // put_CornerRadius(CornerRadius) — struct passed by value

    // ---- Microsoft.UI.Xaml.Controls.Button ----
    const val CLS_Button = "Microsoft.UI.Xaml.Controls.Button"
    const val IID_IButtonFactory = "fe393422-d91c-57b1-9a9c-2c7e3f41f77c"
    const val IID_IButton = "216c183d-d07a-5aa5-b8a4-0300a2683e87"
    const val IButton_put_Flyout = 7                   // put_Flyout(FlyoutBase)
    const val IID_IContentControl = "07e81761-11b2-52ae-8f8b-4d53d2b5900a"
    const val IContentControl_get_Content = 6          // get_Content(out IInspectable)
    const val IContentControl_put_Content = 7          // put_Content(IInspectable)
    const val IID_IButtonBase = "65714269-2473-5327-a652-0ea6bce7f403" // Controls.Primitives
    const val IButtonBase_get_ClickMode = 6            // get_ClickMode(out ClickMode)
    const val IButtonBase_put_ClickMode = 7            // put_ClickMode(ClickMode)
    const val IButtonBase_get_IsPointerOver = 8        // get_IsPointerOver(out boolean)
    const val IButtonBase_get_IsPressed = 9            // get_IsPressed(out boolean)
    const val IButtonBase_get_Command = 10             // get_Command(out ICommand)
    const val IButtonBase_put_Command = 11             // put_Command(ICommand)
    const val IButtonBase_get_CommandParameter = 12    // get_CommandParameter(out IInspectable)
    const val IButtonBase_put_CommandParameter = 13    // put_CommandParameter(IInspectable)
    const val IButtonBase_add_Click = 14               // add_Click(RoutedEventHandler, out token)
    const val IButtonBase_remove_Click = 15            // remove_Click(token)

    /** delegate Microsoft.UI.Xaml.RoutedEventHandler(sender, RoutedEventArgs) — Invoke is vtbl[3] */
    const val IID_RoutedEventHandler = "dae23d85-69ca-5bdf-805b-6161a3a215cc"

    // ---- Microsoft.UI.Xaml.Input.ICommand (the type of ButtonBase.Command) ----
    // Implemented on the Kotlin side (WCommand). Method order 6..9 when building the vtable follows this declaration order.
    const val IID_ICommand = "e5af3542-ca67-4081-995b-709dd13792df"
    const val ICommand_add_CanExecuteChanged = 6       // add_CanExecuteChanged(EventHandler<Object>, out token)
    const val ICommand_remove_CanExecuteChanged = 7    // remove_CanExecuteChanged(token)
    const val ICommand_CanExecute = 8                  // CanExecute(IInspectable, out boolean)
    const val ICommand_Execute = 9                     // Execute(IInspectable)

    // ---- Microsoft.UI.Xaml.Controls.Flyout (the concrete type behind Button.Flyout) ----
    const val CLS_Flyout = "Microsoft.UI.Xaml.Controls.Flyout"
    const val IID_IFlyoutFactory = "fd19002e-66b3-5656-b49c-b2aca11e9602"
    const val IID_IFlyout = "d4a1eb7d-59b8-5df9-87c3-bd5e3856923f"
    const val IFlyout_put_Content = 7                  // put_Content(UIElement)
    const val IID_IFlyoutBase = "bb6603bf-744d-5c31-a87d-744394634d77" // Controls.Primitives
    const val IFlyoutBase_get_Placement = 6            // get_Placement(out FlyoutPlacementMode)
    const val IFlyoutBase_put_Placement = 7            // put_Placement(FlyoutPlacementMode)
    const val IFlyoutBase_get_IsOpen = 27              // get_IsOpen(out boolean)
    const val IFlyoutBase_ShowAt = 38                  // ShowAt(FrameworkElement)
    const val IFlyoutBase_Hide = 40                    // Hide()

    // ---- Microsoft.UI.Xaml.Controls.ContentDialog ----
    const val CLS_ContentDialog = "Microsoft.UI.Xaml.Controls.ContentDialog"
    const val IID_IContentDialogFactory = "a05b3ad7-c60e-545a-9ee4-f098220ed816"
    const val IID_IContentDialog = "ac2145a3-4a32-5305-a81d-47509515bfce"
    const val IContentDialog_put_Title = 7             // put_Title(IInspectable)
    const val IContentDialog_get_PrimaryButtonText = 12 // get_PrimaryButtonText(out HSTRING)
    const val IContentDialog_put_PrimaryButtonText = 13 // put_PrimaryButtonText(HSTRING)
    const val IContentDialog_get_SecondaryButtonText = 14 // get_SecondaryButtonText(out HSTRING)
    const val IContentDialog_put_SecondaryButtonText = 15 // put_SecondaryButtonText(HSTRING)
    const val IContentDialog_get_CloseButtonText = 16  // get_CloseButtonText(out HSTRING)
    const val IContentDialog_put_CloseButtonText = 17  // put_CloseButtonText(HSTRING)
    const val IContentDialog_get_IsPrimaryButtonEnabled = 30 // get_IsPrimaryButtonEnabled(out boolean)
    const val IContentDialog_put_IsPrimaryButtonEnabled = 31 // put_IsPrimaryButtonEnabled(boolean)
    const val IContentDialog_get_IsSecondaryButtonEnabled = 32 // get_IsSecondaryButtonEnabled(out boolean)
    const val IContentDialog_put_IsSecondaryButtonEnabled = 33 // put_IsSecondaryButtonEnabled(boolean)
    const val IContentDialog_get_DefaultButton = 40    // get_DefaultButton(out ContentDialogButton)
    const val IContentDialog_put_DefaultButton = 41    // put_DefaultButton(ContentDialogButton)
    const val IContentDialog_add_Closed = 44           // add_Closed(TypedEventHandler, out token)
    const val IContentDialog_remove_Closed = 45        // remove_Closed(token)
    const val IContentDialog_Hide = 54                 // Hide()
    const val IContentDialog_ShowAsync = 55            // ShowAsync(out IAsyncOperation<ContentDialogResult>)
    const val IID_IContentDialogClosedEventArgs = "9b84e681-1ab6-5485-88b2-d0d3c05b29f3"
    const val IContentDialogClosedEventArgs_get_Result = 6 // get_Result(out ContentDialogResult)

    // ---- Microsoft.UI.Xaml.Controls.Primitives.Popup ----
    const val CLS_Popup = "Microsoft.UI.Xaml.Controls.Primitives.Popup"
    const val IID_IPopup = "4e3ab19d-2f95-579c-9535-906c58629437"
    const val IPopup_put_Child = 7                     // put_Child(UIElement)
    const val IPopup_get_IsOpen = 8                    // get_IsOpen(out boolean)
    const val IPopup_put_IsOpen = 9                    // put_IsOpen(boolean)
    const val IPopup_get_HorizontalOffset = 10         // get_HorizontalOffset(out DOUBLE)
    const val IPopup_put_HorizontalOffset = 11         // put_HorizontalOffset(DOUBLE)
    const val IPopup_get_VerticalOffset = 12           // get_VerticalOffset(out DOUBLE)
    const val IPopup_put_VerticalOffset = 13           // put_VerticalOffset(DOUBLE)
    const val IPopup_get_IsLightDismissEnabled = 16    // get_IsLightDismissEnabled(out boolean)
    const val IPopup_put_IsLightDismissEnabled = 17    // put_IsLightDismissEnabled(boolean)
    const val IPopup_add_Opened = 23                   // add_Opened(EventHandler<Object>, out token)
    const val IPopup_remove_Opened = 24                // remove_Opened(token)
    const val IPopup_add_Closed = 25                   // add_Closed(EventHandler<Object>, out token)
    const val IPopup_remove_Closed = 26                // remove_Closed(token)

    // ---- Microsoft.UI.Xaml.Controls.TeachingTip ----
    const val CLS_TeachingTip = "Microsoft.UI.Xaml.Controls.TeachingTip"
    const val IID_ITeachingTipFactory = "a3ecd47d-2972-5d19-a62e-ddfbc5e1ad57"
    const val IID_ITeachingTip = "daebd5f7-3b47-5b12-b804-f4e1442b2113"
    const val ITeachingTip_get_Title = 6               // get_Title(out HSTRING)
    const val ITeachingTip_put_Title = 7               // put_Title(HSTRING)
    const val ITeachingTip_get_Subtitle = 8            // get_Subtitle(out HSTRING)
    const val ITeachingTip_put_Subtitle = 9            // put_Subtitle(HSTRING)
    const val ITeachingTip_get_IsOpen = 10             // get_IsOpen(out boolean)
    const val ITeachingTip_put_IsOpen = 11             // put_IsOpen(boolean)
    const val ITeachingTip_put_Target = 13             // put_Target(FrameworkElement)
    const val ITeachingTip_put_ActionButtonContent = 17 // put_ActionButtonContent(IInspectable)
    const val ITeachingTip_put_CloseButtonContent = 25 // put_CloseButtonContent(IInspectable)
    const val ITeachingTip_get_IsLightDismissEnabled = 36 // get_IsLightDismissEnabled(out boolean)
    const val ITeachingTip_put_IsLightDismissEnabled = 37 // put_IsLightDismissEnabled(boolean)
    const val ITeachingTip_get_PreferredPlacement = 38 // get_PreferredPlacement(out TeachingTipPlacementMode)
    const val ITeachingTip_put_PreferredPlacement = 39 // put_PreferredPlacement(TeachingTipPlacementMode)
    const val ITeachingTip_add_ActionButtonClick = 47  // add_ActionButtonClick(TypedEventHandler<TeachingTip, Object>, out token)
    const val ITeachingTip_remove_ActionButtonClick = 48 // remove_ActionButtonClick(token)
    const val ITeachingTip_add_Closed = 53             // add_Closed(TypedEventHandler, out token)
    const val ITeachingTip_remove_Closed = 54          // remove_Closed(token)
    const val IID_ITeachingTipClosedEventArgs = "2536f506-4038-59db-9e35-a9252fb5adb2"
    const val ITeachingTipClosedEventArgs_get_Reason = 6 // get_Reason(out TeachingTipCloseReason)

    // ---- Microsoft.UI.Xaml.Controls.ListView ----
    const val CLS_ListView = "Microsoft.UI.Xaml.Controls.ListView"
    const val IID_IListViewFactory = "03ebefb8-f64a-5bf9-9570-cb09eeea2335"
    const val IID_IListView = "f6015db1-df63-52fd-a164-0df44715ee0a" // default interface (no members)
    const val IID_IListViewBase = "775c57ac-abce-5beb-8e34-3b8158aedd80"
    const val IListViewBase_get_SelectionMode = 7      // get_SelectionMode(out ListViewSelectionMode)
    const val IListViewBase_put_SelectionMode = 8      // put_SelectionMode(ListViewSelectionMode)
    const val IListViewBase_get_IsItemClickEnabled = 15 // get_IsItemClickEnabled(out boolean)
    const val IListViewBase_put_IsItemClickEnabled = 16 // put_IsItemClickEnabled(boolean)
    const val IListViewBase_add_ItemClick = 32         // add_ItemClick(ItemClickEventHandler, out token)
    const val IListViewBase_remove_ItemClick = 33      // remove_ItemClick(token)
    const val IListViewBase_SelectAll = 45             // SelectAll()
    const val IListViewBase_get_Header = 54            // get_Header(out IInspectable)
    const val IListViewBase_put_Header = 55            // put_Header(IInspectable)
    const val IID_ISelector = "8f7e2159-e61d-576f-8476-f83fde3d689e" // Controls.Primitives
    const val ISelector_get_SelectedIndex = 6          // get_SelectedIndex(out INT32)
    const val ISelector_put_SelectedIndex = 7          // put_SelectedIndex(INT32)
    const val ISelector_get_SelectedItem = 8           // get_SelectedItem(out IInspectable)
    const val ISelector_add_SelectionChanged = 16      // add_SelectionChanged(SelectionChangedEventHandler, out token)
    const val ISelector_remove_SelectionChanged = 17   // remove_SelectionChanged(token)
    const val IID_IItemsControl = "bf1ccb54-83e2-5b98-acbc-736f876c3d35"
    const val IItemsControl_get_Items = 8              // get_Items(out ItemCollection)
    const val IItemsControl_put_ItemContainerStyle = 19 // put_ItemContainerStyle(Style)

    // ---- Microsoft.UI.Xaml.Style ----
    const val IID_IStyle = "65e1d164-572f-5b0e-a80f-9c02441fac49"

    // ---- Microsoft.UI.Xaml.Markup.XamlReader ----
    const val CLS_XamlReader = "Microsoft.UI.Xaml.Markup.XamlReader"
    const val IID_IXamlReaderStatics = "82a4cd9e-435e-5aeb-8c4f-300cece45cae"
    const val IXamlReaderStatics_Load = 6              // Load(string, out IInspectable)

    /** delegate Controls.SelectionChangedEventHandler(sender, SelectionChangedEventArgs) — Invoke is vtbl[3] */
    const val IID_SelectionChangedEventHandler = "a232390d-0e34-595e-8931-fa928a9909f4"

    /** delegate Controls.ItemClickEventHandler(sender, ItemClickEventArgs) — Invoke is vtbl[3] */
    const val IID_ItemClickEventHandler = "a3903624-3393-566c-a6b9-a6b4b3e301c3"
    const val IID_IItemClickEventArgs = "1cf87a70-6348-57ec-9eac-fa0565adc60f"
    const val IItemClickEventArgs_get_ClickedItem = 6  // get_ClickedItem(out IInspectable)

    // ---- Microsoft.UI.Xaml.Controls.Primitives.ToggleButton ----
    const val CLS_ToggleButton = "Microsoft.UI.Xaml.Controls.Primitives.ToggleButton"
    const val IID_IToggleButtonFactory = "519511bb-d35b-5e2d-966c-8369405a4408"
    const val IID_IToggleButton = "686fbaa4-c866-568b-8f75-481d8d545291"
    const val IToggleButton_get_IsChecked = 6          // get_IsChecked(out IReference<boolean>) — can be null
    const val IToggleButton_put_IsChecked = 7          // put_IsChecked(IReference<boolean>)
    const val IToggleButton_get_IsThreeState = 8       // get_IsThreeState(out boolean)
    const val IToggleButton_put_IsThreeState = 9       // put_IsThreeState(boolean)
    const val IToggleButton_add_Checked = 10           // add_Checked(RoutedEventHandler, out token)
    const val IToggleButton_remove_Checked = 11
    const val IToggleButton_add_Unchecked = 12         // add_Unchecked(RoutedEventHandler, out token)
    const val IToggleButton_remove_Unchecked = 13
    const val IToggleButton_add_Indeterminate = 14     // add_Indeterminate(RoutedEventHandler, out token)
    const val IToggleButton_remove_Indeterminate = 15

    // ---- Microsoft.UI.Xaml.Controls.CheckBox ----
    const val CLS_CheckBox = "Microsoft.UI.Xaml.Controls.CheckBox"
    const val IID_ICheckBoxFactory = "f43ff58d-31d5-5835-af7b-375bc6a9bcf3"
    // ICheckBox (c5830000-4c9d-5fdd-9346-674c71cd80c5) has no members. All functionality comes from ToggleButton

    // ---- Microsoft.UI.Xaml.Controls.RadioButton ----
    const val CLS_RadioButton = "Microsoft.UI.Xaml.Controls.RadioButton"
    const val IID_IRadioButtonFactory = "5772c79a-b3eb-5719-8005-2a513429495a"
    const val IID_IRadioButton = "38f30cee-e75a-5ba1-ae64-4474a3abeac7"
    const val IRadioButton_get_GroupName = 6           // get_GroupName(out HSTRING)
    const val IRadioButton_put_GroupName = 7           // put_GroupName(HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.Primitives.RepeatButton ----
    const val CLS_RepeatButton = "Microsoft.UI.Xaml.Controls.Primitives.RepeatButton"
    const val IID_IRepeatButton = "97f4c728-4a94-56b5-91e4-e7c6f6a1251a" // activatable (default factory)
    const val IRepeatButton_get_Delay = 6              // get_Delay(out INT32) — milliseconds
    const val IRepeatButton_put_Delay = 7              // put_Delay(INT32)
    const val IRepeatButton_get_Interval = 8           // get_Interval(out INT32) — milliseconds
    const val IRepeatButton_put_Interval = 9           // put_Interval(INT32)

    // ---- Microsoft.UI.Xaml.Controls.HyperlinkButton ----
    const val CLS_HyperlinkButton = "Microsoft.UI.Xaml.Controls.HyperlinkButton"
    const val IID_IHyperlinkButtonFactory = "01f775ea-c5ed-514a-a23d-89c494a8f09d"
    const val IID_IHyperlinkButton = "6dbee605-8df0-50cc-9a42-250eb138f0c6"
    const val IHyperlinkButton_get_NavigateUri = 6     // get_NavigateUri(out Windows.Foundation.Uri)
    const val IHyperlinkButton_put_NavigateUri = 7     // put_NavigateUri(Windows.Foundation.Uri)

    // ---- Windows.Foundation.Uri (OS side, FoundationContract.winmd) ----
    const val CLS_Uri = "Windows.Foundation.Uri"
    const val IID_IUriRuntimeClassFactory = "44a9796f-723e-4fdf-a218-033e75b0c084"
    const val IUriRuntimeClassFactory_CreateUri = 6    // CreateUri(HSTRING, out Uri)
    const val IUriRuntimeClass_get_AbsoluteUri = 6     // get_AbsoluteUri(out HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.DropDownButton ----
    const val CLS_DropDownButton = "Microsoft.UI.Xaml.Controls.DropDownButton"
    const val IID_IDropDownButtonFactory = "7cf3e13b-668d-57e7-b5d6-f5ca3dbc80bd"
    // IDropDownButton (c1e9fa91-4f95-5796-8a7b-3b7594a12c69) has no members. Flyout comes from IButton

    // ---- Microsoft.UI.Xaml.Controls.SplitButton ----
    const val CLS_SplitButton = "Microsoft.UI.Xaml.Controls.SplitButton"
    const val IID_ISplitButtonFactory = "07510092-2612-55e7-981c-a536ddd4570e"
    const val IID_ISplitButton = "f627202d-d2d7-5ff6-bb05-8c48eb6b1fc6"
    const val ISplitButton_get_Flyout = 6              // get_Flyout(out FlyoutBase)
    const val ISplitButton_put_Flyout = 7              // put_Flyout(FlyoutBase)
    const val ISplitButton_add_Click = 12              // add_Click(TypedEventHandler, out token)
    const val ISplitButton_remove_Click = 13           // remove_Click(token)
    const val IID_ISplitButtonClickEventArgs = "6af896c2-e65a-5998-9c82-2af8f3e0741f"

    // ---- Microsoft.UI.Xaml.Controls.ToggleSplitButton ----
    const val CLS_ToggleSplitButton = "Microsoft.UI.Xaml.Controls.ToggleSplitButton"
    const val IID_IToggleSplitButtonFactory = "25459d02-0ffc-5c7c-af56-f55aad6db5e7"
    const val IID_IToggleSplitButton = "5c0f247b-bd00-5509-88a8-b09007ae22b0"
    const val IToggleSplitButton_get_IsChecked = 6     // get_IsChecked(out boolean) — plain boolean
    const val IToggleSplitButton_put_IsChecked = 7     // put_IsChecked(boolean)
    const val IToggleSplitButton_add_IsCheckedChanged = 8 // add_IsCheckedChanged(TypedEventHandler, out token)
    const val IToggleSplitButton_remove_IsCheckedChanged = 9
    const val IID_IToggleSplitButtonIsCheckedChangedEventArgs = "6cab1e15-c017-5760-828b-dafc21d54eb2"

    // ---- Microsoft.UI.Xaml.Controls.Primitives.RangeBase (Slider's base) ----
    const val IID_IRangeBase = "540d6d61-8fac-5d5c-b5b0-e172a7dde103"
    const val IRangeBase_get_Minimum = 6               // get_Minimum(out DOUBLE)
    const val IRangeBase_put_Minimum = 7               // put_Minimum(DOUBLE)
    const val IRangeBase_get_Maximum = 8               // get_Maximum(out DOUBLE)
    const val IRangeBase_put_Maximum = 9               // put_Maximum(DOUBLE)
    const val IRangeBase_get_SmallChange = 10          // get_SmallChange(out DOUBLE)
    const val IRangeBase_put_SmallChange = 11          // put_SmallChange(DOUBLE)
    const val IRangeBase_get_LargeChange = 12          // get_LargeChange(out DOUBLE)
    const val IRangeBase_put_LargeChange = 13          // put_LargeChange(DOUBLE)
    const val IRangeBase_get_Value = 14                // get_Value(out DOUBLE)
    const val IRangeBase_put_Value = 15                // put_Value(DOUBLE)
    const val IRangeBase_add_ValueChanged = 16         // add_ValueChanged(RangeBaseValueChangedEventHandler, out token)
    const val IRangeBase_remove_ValueChanged = 17

    /** delegate Primitives.RangeBaseValueChangedEventHandler(sender, RangeBaseValueChangedEventArgs) — Invoke is vtbl[3] */
    const val IID_RangeBaseValueChangedEventHandler = "23f0e209-9455-54cb-b8bc-0b49553c7dcc"
    const val IID_IRangeBaseValueChangedEventArgs = "b0181692-9578-51c7-9d1c-adfcf8945aa9"
    const val IRangeBaseValueChangedEventArgs_get_OldValue = 6 // get_OldValue(out DOUBLE)
    const val IRangeBaseValueChangedEventArgs_get_NewValue = 7 // get_NewValue(out DOUBLE)

    // ---- Microsoft.UI.Xaml.Controls.Slider ----
    const val CLS_Slider = "Microsoft.UI.Xaml.Controls.Slider"
    const val IID_ISliderFactory = "06604d71-34ca-5f39-9656-29d81d3c110c"
    const val IID_ISlider = "f7418ecf-7c35-5216-8bf1-d82d47cce5df"
    const val ISlider_get_StepFrequency = 8            // get_StepFrequency(out DOUBLE)
    const val ISlider_put_StepFrequency = 9            // put_StepFrequency(DOUBLE)
    const val ISlider_get_SnapsTo = 10                 // get_SnapsTo(out SliderSnapsTo)
    const val ISlider_put_SnapsTo = 11                 // put_SnapsTo(SliderSnapsTo)
    const val ISlider_get_TickFrequency = 12           // get_TickFrequency(out DOUBLE)
    const val ISlider_put_TickFrequency = 13           // put_TickFrequency(DOUBLE)
    const val ISlider_get_TickPlacement = 14           // get_TickPlacement(out TickPlacement)
    const val ISlider_put_TickPlacement = 15           // put_TickPlacement(TickPlacement)
    const val ISlider_get_Orientation = 16             // get_Orientation(out Orientation)
    const val ISlider_put_Orientation = 17             // put_Orientation(Orientation)
    const val ISlider_get_IsDirectionReversed = 18     // get_IsDirectionReversed(out boolean)
    const val ISlider_put_IsDirectionReversed = 19     // put_IsDirectionReversed(boolean)
    const val ISlider_put_Header = 25                  // put_Header(IInspectable)

    // ---- Microsoft.UI.Xaml.Controls.ToggleSwitch ----
    const val CLS_ToggleSwitch = "Microsoft.UI.Xaml.Controls.ToggleSwitch"
    const val IID_IToggleSwitch = "1b17eeb1-74bf-5a83-8161-a86f0fdcdf24" // activatable (default factory)
    const val IToggleSwitch_get_IsOn = 6               // get_IsOn(out boolean)
    const val IToggleSwitch_put_IsOn = 7               // put_IsOn(boolean)
    const val IToggleSwitch_put_Header = 9             // put_Header(IInspectable)
    const val IToggleSwitch_put_OnContent = 13         // put_OnContent(IInspectable)
    const val IToggleSwitch_put_OffContent = 17        // put_OffContent(IInspectable)
    const val IToggleSwitch_add_Toggled = 21           // add_Toggled(RoutedEventHandler, out token)
    const val IToggleSwitch_remove_Toggled = 22

    // ---- Microsoft.UI.Xaml.Controls.ComboBox ----
    const val CLS_ComboBox = "Microsoft.UI.Xaml.Controls.ComboBox"
    const val IID_IComboBoxFactory = "71c1014b-acdf-5c03-b5ed-02871caaeb6b"
    const val IID_IComboBox = "c77da58b-4fd7-51e0-a431-f84658a83e9e"
    const val IComboBox_get_IsDropDownOpen = 6         // get_IsDropDownOpen(out boolean)
    const val IComboBox_put_IsDropDownOpen = 7         // put_IsDropDownOpen(boolean)
    const val IComboBox_get_IsEditable = 8             // get_IsEditable(out boolean)
    const val IComboBox_put_IsEditable = 9             // put_IsEditable(boolean)
    const val IComboBox_put_Header = 17                // put_Header(IInspectable)
    const val IComboBox_get_PlaceholderText = 20       // get_PlaceholderText(out HSTRING)
    const val IComboBox_put_PlaceholderText = 21       // put_PlaceholderText(HSTRING)
    const val IComboBox_get_Text = 30                  // get_Text(out HSTRING)
    const val IComboBox_put_Text = 31                  // put_Text(HSTRING)
    const val IComboBox_add_TextSubmitted = 40         // add_TextSubmitted(TypedEventHandler, out token)
    const val IComboBox_remove_TextSubmitted = 41
    const val IID_IComboBoxTextSubmittedEventArgs = "0d7a9794-73b5-585e-bfbb-de6df7eb9fcf"
    const val IComboBoxTextSubmittedEventArgs_get_Text = 6 // get_Text(out HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.RatingControl ----
    const val CLS_RatingControl = "Microsoft.UI.Xaml.Controls.RatingControl"
    const val IID_IRatingControlFactory = "a53b9b73-bff9-548d-a294-ac63d819f78a"
    const val IID_IRatingControl = "5488193b-ea4b-52c6-8544-c063219bcd90"
    const val IRatingControl_get_Caption = 6           // get_Caption(out HSTRING)
    const val IRatingControl_put_Caption = 7           // put_Caption(HSTRING)
    const val IRatingControl_get_IsClearEnabled = 10   // get_IsClearEnabled(out boolean)
    const val IRatingControl_put_IsClearEnabled = 11   // put_IsClearEnabled(boolean)
    const val IRatingControl_get_IsReadOnly = 12       // get_IsReadOnly(out boolean)
    const val IRatingControl_put_IsReadOnly = 13       // put_IsReadOnly(boolean)
    const val IRatingControl_get_MaxRating = 14        // get_MaxRating(out INT32)
    const val IRatingControl_put_MaxRating = 15        // put_MaxRating(INT32)
    const val IRatingControl_get_PlaceholderValue = 16 // get_PlaceholderValue(out DOUBLE)
    const val IRatingControl_put_PlaceholderValue = 17 // put_PlaceholderValue(DOUBLE)
    const val IRatingControl_get_Value = 20            // get_Value(out DOUBLE) — unset is -1
    const val IRatingControl_put_Value = 21            // put_Value(DOUBLE)
    const val IRatingControl_add_ValueChanged = 22     // add_ValueChanged(TypedEventHandler<RatingControl, Object>, out token)
    const val IRatingControl_remove_ValueChanged = 23

    // ---- Microsoft.UI.Xaml.Controls.ColorPicker ----
    const val CLS_ColorPicker = "Microsoft.UI.Xaml.Controls.ColorPicker"
    const val IID_IColorPickerFactory = "72c350e2-0a20-5b9b-ac54-633b97d7ffde"
    const val IID_IColorPicker = "ae72b24b-f93f-5a19-8ce4-a18b73c3356d"
    const val IColorPicker_get_Color = 6               // get_Color(out Windows.UI.Color) — struct
    const val IColorPicker_put_Color = 7               // put_Color(Windows.UI.Color) — struct passed by value
    const val IColorPicker_get_IsAlphaEnabled = 10     // get_IsAlphaEnabled(out boolean)
    const val IColorPicker_put_IsAlphaEnabled = 11     // put_IsAlphaEnabled(boolean)
    const val IColorPicker_put_IsMoreButtonVisible = 21 // put_IsMoreButtonVisible(boolean)
    const val IColorPicker_put_IsHexInputVisible = 27  // put_IsHexInputVisible(boolean)
    const val IColorPicker_get_ColorSpectrumShape = 40 // get_ColorSpectrumShape(out ColorSpectrumShape)
    const val IColorPicker_put_ColorSpectrumShape = 41 // put_ColorSpectrumShape(ColorSpectrumShape)
    const val IColorPicker_add_ColorChanged = 44       // add_ColorChanged(TypedEventHandler, out token)
    const val IColorPicker_remove_ColorChanged = 45
    const val IID_IColorChangedEventArgs = "148d57a2-b1cb-5f5d-b6b5-512805d71761"
    const val IColorChangedEventArgs_get_NewColor = 7  // get_NewColor(out Windows.UI.Color) — struct

    // ---- Microsoft.UI.Xaml.Controls.ScrollViewer ----
    const val CLS_ScrollViewer = "Microsoft.UI.Xaml.Controls.ScrollViewer"
    const val IID_IScrollViewer = "1dc28c2e-996c-5394-89c3-4dc656b4ad46" // activatable (default factory)

    // ---- Microsoft.UI.Xaml.Controls.IconElement / SymbolIcon ----
    const val IID_IIconElement = "18f69350-279e-50ea-8d23-138e717ed939"
    const val CLS_SymbolIcon = "Microsoft.UI.Xaml.Controls.SymbolIcon"
    const val IID_ISymbolIconFactory = "d4430447-567c-5aad-996a-a547774e2c3c"
    const val ISymbolIconFactory_CreateInstanceWithSymbol = 6 // CreateInstanceWithSymbol(Symbol, out SymbolIcon)
    const val IID_ISymbolIcon = "a4322906-0dbe-5eb7-8b64-3e832246eb7f"
    const val ISymbolIcon_get_Symbol = 6               // get_Symbol(out Symbol)
    const val ISymbolIcon_put_Symbol = 7               // put_Symbol(Symbol)

    // ---- Microsoft.UI.Xaml.Controls.NavigationView ----
    const val CLS_NavigationView = "Microsoft.UI.Xaml.Controls.NavigationView"
    const val IID_INavigationViewFactory = "ffea1ada-9232-5507-a320-ed2fadbe6127"
    const val IID_INavigationView = "e77a4b36-3dd1-53d9-9f97-65dccaa74a5c"
    const val INavigationView_get_IsPaneOpen = 6       // get_IsPaneOpen(out boolean)
    const val INavigationView_put_IsPaneOpen = 7       // put_IsPaneOpen(boolean)
    const val INavigationView_get_FooterMenuItems = 12 // get_FooterMenuItems(out IVector<Object>)
    const val INavigationView_put_Header = 18          // put_Header(IInspectable)
    const val INavigationView_get_DisplayMode = 21     // get_DisplayMode(out NavigationViewDisplayMode)
    const val INavigationView_get_IsSettingsVisible = 22 // get_IsSettingsVisible(out boolean)
    const val INavigationView_put_IsSettingsVisible = 23 // put_IsSettingsVisible(boolean)
    const val INavigationView_get_IsPaneToggleButtonVisible = 24 // get_IsPaneToggleButtonVisible(out boolean)
    const val INavigationView_put_IsPaneToggleButtonVisible = 25 // put_IsPaneToggleButtonVisible(boolean)
    const val INavigationView_get_CompactPaneLength = 28 // get_CompactPaneLength(out DOUBLE)
    const val INavigationView_put_CompactPaneLength = 29 // put_CompactPaneLength(DOUBLE)
    const val INavigationView_get_OpenPaneLength = 30  // get_OpenPaneLength(out DOUBLE)
    const val INavigationView_put_OpenPaneLength = 31  // put_OpenPaneLength(DOUBLE)
    const val INavigationView_get_SelectedItem = 34    // get_SelectedItem(out IInspectable)
    const val INavigationView_put_SelectedItem = 35    // put_SelectedItem(IInspectable)
    const val INavigationView_get_MenuItems = 36       // get_MenuItems(out IVector<Object>)
    const val INavigationView_add_SelectionChanged = 52 // add_SelectionChanged(TypedEventHandler, out token)
    const val INavigationView_remove_SelectionChanged = 53
    const val INavigationView_add_ItemInvoked = 54     // add_ItemInvoked(TypedEventHandler, out token)
    const val INavigationView_remove_ItemInvoked = 55
    const val IID_INavigationView2 = "05b428cf-014c-56dd-896a-a3e7089d73b5"
    const val INavigationView2_get_IsBackButtonVisible = 6 // get_IsBackButtonVisible(out NavigationViewBackButtonVisible)
    const val INavigationView2_put_IsBackButtonVisible = 7 // put_IsBackButtonVisible(NavigationViewBackButtonVisible)
    const val INavigationView2_get_IsBackEnabled = 8   // get_IsBackEnabled(out boolean)
    const val INavigationView2_put_IsBackEnabled = 9   // put_IsBackEnabled(boolean)
    const val INavigationView2_get_PaneTitle = 10      // get_PaneTitle(out HSTRING)
    const val INavigationView2_put_PaneTitle = 11      // put_PaneTitle(HSTRING)
    const val INavigationView2_get_PaneDisplayMode = 22 // get_PaneDisplayMode(out NavigationViewPaneDisplayMode)
    const val INavigationView2_put_PaneDisplayMode = 23 // put_PaneDisplayMode(NavigationViewPaneDisplayMode)
    const val IID_INavigationViewSelectionChangedEventArgs = "14a064a5-c79d-5f63-ac6e-1c313fe63566"
    const val INavigationViewSelectionChangedEventArgs_get_SelectedItem = 6 // get_SelectedItem(out IInspectable)
    const val IID_INavigationViewItemInvokedEventArgs = "074cebaa-5d05-547b-8cd6-d19ac2d9bb3b"
    const val INavigationViewItemInvokedEventArgs_get_InvokedItem = 6 // get_InvokedItem(out IInspectable)

    // ---- Microsoft.UI.Xaml.Controls.NavigationViewItem ----
    const val CLS_NavigationViewItem = "Microsoft.UI.Xaml.Controls.NavigationViewItem"
    const val IID_INavigationViewItemFactory = "de60a001-9385-5535-80e1-2b68f4bfde26"
    const val IID_INavigationViewItem = "3ab3d503-a37c-5836-8adb-2882062e73a1"
    const val INavigationViewItem_put_Icon = 7         // put_Icon(IconElement)
    const val IID_INavigationViewItem2 = "2d5bd889-9dac-5675-b254-68226f077a61"
    const val INavigationViewItem2_get_SelectsOnInvoked = 6 // get_SelectsOnInvoked(out boolean)
    const val INavigationViewItem2_put_SelectsOnInvoked = 7 // put_SelectsOnInvoked(boolean)
    const val INavigationViewItem2_get_IsExpanded = 8  // get_IsExpanded(out boolean)
    const val INavigationViewItem2_put_IsExpanded = 9  // put_IsExpanded(boolean)
    const val INavigationViewItem2_get_MenuItems = 14  // get_MenuItems(out IVector<Object>)

    // ---- Default control styles / metadata provider ----
    const val CLS_XamlControlsResources = "Microsoft.UI.Xaml.Controls.XamlControlsResources"
    const val IID_IResourceDictionary = "1b690975-a710-5783-a6e1-15836f6186c2"
    const val IResourceDictionary_get_MergedDictionaries = 8 // -> IVector<ResourceDictionary>
    const val CLS_XamlControlsXamlMetaDataProvider =
        "Microsoft.UI.Xaml.XamlTypeInfo.XamlControlsXamlMetaDataProvider"
    const val IID_IXamlMetadataProvider = "a96251f0-2214-5d53-8746-ce99a2593cd7"
    const val IXamlMetadataProvider_GetXamlType = 6           // GetXamlType(TypeName, out IXamlType)
    const val IXamlMetadataProvider_GetXamlTypeByFullName = 7 // GetXamlType(HSTRING, out IXamlType)
    const val IXamlMetadataProvider_GetXmlnsDefinitions = 8   // (out UINT32, out XmlnsDefinition*)

    // ---- Resource resolution for unpackaged apps (ResourceManagerRequested) ----
    const val IID_IApplication2 = "469e6d36-2e11-5b06-9e0a-c5eef0cf8f12"
    const val IApplication2_add_ResourceManagerRequested = 6
    const val IID_IResourceManagerRequestedEventArgs = "c35f4cf1-fcd6-5c6b-9be2-4cfaefb68b2a"
    const val IResourceManagerRequestedEventArgs_put_CustomResourceManager = 7
    const val CLS_ResourceManager = "Microsoft.Windows.ApplicationModel.Resources.ResourceManager"
    const val IID_IResourceManagerFactory = "d6acf18f-458a-535b-a5c4-ac2dc4e49099"
    // IResourceManagerFactory: vtbl[6] CreateInstance(HSTRING fileName, out ResourceManager)

    /** Base IID of Windows.Foundation.TypedEventHandler`2. */
    private const val IID_TypedEventHandler_OPEN = "9de1c534-6ae1-11e0-84e1-18a905bcc53f"

    /**
     * The actual IID (computed at runtime) of
     * TypedEventHandler<Object, ResourceManagerRequestedEventArgs>. The first type
     * argument is Object, not Application (its signature is cinterface(IInspectable)).
     * Source: docs/design-notes/custom-mrt-resourcemanager.md in microsoft-ui-xaml.
     */
    val IID_ResourceManagerRequestedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "cinterface(IInspectable);" +
                "rc(Microsoft.UI.Xaml.ResourceManagerRequestedEventArgs;" +
                "{$IID_IResourceManagerRequestedEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<Expander, ExpanderExpandingEventArgs>. */
    val IID_ExpanderExpandingHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.Expander;{$IID_IExpander});" +
                "rc(Microsoft.UI.Xaml.Controls.ExpanderExpandingEventArgs;" +
                "{$IID_IExpanderExpandingEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<Expander, ExpanderCollapsedEventArgs>. */
    val IID_ExpanderCollapsedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.Expander;{$IID_IExpander});" +
                "rc(Microsoft.UI.Xaml.Controls.ExpanderCollapsedEventArgs;" +
                "{$IID_IExpanderCollapsedEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<SplitButton, SplitButtonClickEventArgs>. */
    val IID_SplitButtonClickHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.SplitButton;{$IID_ISplitButton});" +
                "rc(Microsoft.UI.Xaml.Controls.SplitButtonClickEventArgs;" +
                "{$IID_ISplitButtonClickEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<ToggleSplitButton, ToggleSplitButtonIsCheckedChangedEventArgs>. */
    val IID_ToggleSplitButtonIsCheckedChangedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.ToggleSplitButton;{$IID_IToggleSplitButton});" +
                "rc(Microsoft.UI.Xaml.Controls.ToggleSplitButtonIsCheckedChangedEventArgs;" +
                "{$IID_IToggleSplitButtonIsCheckedChangedEventArgs}))",
        )
    }

    /**
     * The actual IID (computed at runtime) of TypedEventHandler<RatingControl, Object>.
     * The second type argument is Object, so its signature is cinterface(IInspectable).
     */
    val IID_RatingControlValueChangedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.RatingControl;{$IID_IRatingControl});" +
                "cinterface(IInspectable))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<FrameworkElement, Object>. Used for ActualThemeChanged. */
    val IID_ActualThemeChangedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.FrameworkElement;{$IID_IFrameworkElement});" +
                "cinterface(IInspectable))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<ContentDialog, ContentDialogClosedEventArgs>. */
    val IID_ContentDialogClosedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.ContentDialog;{$IID_IContentDialog});" +
                "rc(Microsoft.UI.Xaml.Controls.ContentDialogClosedEventArgs;" +
                "{$IID_IContentDialogClosedEventArgs}))",
        )
    }

    /**
     * The actual IID (computed at runtime) of TypedEventHandler<TeachingTip, Object>.
     * Both ActionButtonClick and CloseButtonClick share this type (TArgs is Object).
     */
    val IID_TeachingTipObjectHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.TeachingTip;{$IID_ITeachingTip});" +
                "cinterface(IInspectable))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<TeachingTip, TeachingTipClosedEventArgs>. */
    val IID_TeachingTipClosedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.TeachingTip;{$IID_ITeachingTip});" +
                "rc(Microsoft.UI.Xaml.Controls.TeachingTipClosedEventArgs;" +
                "{$IID_ITeachingTipClosedEventArgs}))",
        )
    }

    /** Base IID of Windows.Foundation.EventHandler`1. */
    private const val IID_EventHandler_OPEN = "9de1c535-6ae1-11e0-84e1-18a905bcc53f"

    /** The actual IID (computed at runtime) of EventHandler<Object>. Used by Popup.Opened / Closed. */
    val IID_EventHandler_Object: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_EventHandler_OPEN};cinterface(IInspectable))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<ColorPicker, ColorChangedEventArgs>. */
    val IID_ColorPickerColorChangedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.ColorPicker;{$IID_IColorPicker});" +
                "rc(Microsoft.UI.Xaml.Controls.ColorChangedEventArgs;" +
                "{$IID_IColorChangedEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<ComboBox, ComboBoxTextSubmittedEventArgs>. */
    val IID_ComboBoxTextSubmittedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.ComboBox;{$IID_IComboBox});" +
                "rc(Microsoft.UI.Xaml.Controls.ComboBoxTextSubmittedEventArgs;" +
                "{$IID_IComboBoxTextSubmittedEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<NavigationView, NavigationViewSelectionChangedEventArgs>. */
    val IID_NavigationViewSelectionChangedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.NavigationView;{$IID_INavigationView});" +
                "rc(Microsoft.UI.Xaml.Controls.NavigationViewSelectionChangedEventArgs;" +
                "{$IID_INavigationViewSelectionChangedEventArgs}))",
        )
    }

    /** The actual IID (computed at runtime) of TypedEventHandler<NavigationView, NavigationViewItemInvokedEventArgs>. */
    val IID_NavigationViewItemInvokedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.NavigationView;{$IID_INavigationView});" +
                "rc(Microsoft.UI.Xaml.Controls.NavigationViewItemInvokedEventArgs;" +
                "{$IID_INavigationViewItemInvokedEventArgs}))",
        )
    }

    /** Base IID of Windows.Foundation.IReference`1 (from FoundationContract.winmd). */
    private const val IID_IReference_OPEN = "61c17706-2d65-11e0-9ae8-d48564015472"

    /**
     * The actual IID (computed at runtime) of IReference<Boolean>. The signature for boolean is b1.
     * Used to box/unbox ToggleButton.IsChecked (null = indeterminate).
     */
    val IID_IReference_Boolean: String by lazy {
        WinRt.pinterfaceIid("pinterface({$IID_IReference_OPEN};b1)")
    }

    // ---- Windows.Foundation.Collections.IVector<T> (OS side, FoundationContract.winmd) ----
    // GetAt=6 get_Size=7 GetView=8 IndexOf=9 SetAt=10 InsertAt=11 RemoveAt=12 Append=13
    // RemoveAtEnd=14 Clear=15 GetMany=16 ReplaceAll=17
    const val IVector_GetAt = 6                        // GetAt(UINT32, out T)
    const val IVector_get_Size = 7                      // get_Size(out UINT32)
    const val IVector_RemoveAt = 12                     // RemoveAt(UINT32)
    const val IVector_Append = 13                       // Append(T)
    const val IVector_Clear = 15                        // Clear()
    private const val IID_IVector_OPEN = "913337e9-11a1-4345-a3a2-4e7f956e222d" // base IID of IVector`1

    /**
     * The actual IID of IVector<Microsoft.UI.Xaml.UIElement>.
     * Computed at runtime via SHA-1 from the WinRT specification signature
     * (= ea4a1af0-4286-5f11-8142-6b0169f4e9de).
     */
    val IID_IVector_UIElement: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_IVector_OPEN};rc(Microsoft.UI.Xaml.UIElement;{$IID_IUIElement}))",
        )
    }

    /** The actual IID of IVector<Microsoft.UI.Xaml.ResourceDictionary> (used for MergedDictionaries). */
    val IID_IVector_ResourceDictionary: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_IVector_OPEN};" +
                "rc(Microsoft.UI.Xaml.ResourceDictionary;{$IID_IResourceDictionary}))",
        )
    }

    /** The actual IID of IVector<Object> (ItemsControl.Items). Object's signature is cinterface(IInspectable). */
    val IID_IVector_Object: String by lazy {
        WinRt.pinterfaceIid("pinterface({$IID_IVector_OPEN};cinterface(IInspectable))")
    }

    // ---- Microsoft.UI.Dispatching.DispatcherQueue (Microsoft.UI.winmd) ----
    const val CLS_DispatcherQueue = "Microsoft.UI.Dispatching.DispatcherQueue"
    const val IID_IDispatcherQueueStatics = "cd3382ea-a455-5124-b63a-ca40d34ca23c"
    const val IDispatcherQueueStatics_GetForCurrentThread = 6 // GetForCurrentThread(out DispatcherQueue)
    const val IID_IDispatcherQueue = "f6ebf8fa-be1c-5bf6-a467-73da28738ae8"
    const val IDispatcherQueue_CreateTimer = 6         // CreateTimer(out DispatcherQueueTimer)
    const val IDispatcherQueue_TryEnqueue = 7          // TryEnqueue(DispatcherQueueHandler, out boolean)

    /** delegate Microsoft.UI.Dispatching.DispatcherQueueHandler — Invoke() is vtbl[3], no arguments */
    const val IID_DispatcherQueueHandler = "2e0872a9-4e29-5f14-b688-fb96d5f9d5f8"

    // ---- Microsoft.UI.Dispatching.DispatcherQueueTimer (Microsoft.UI.winmd) ----
    const val IID_IDispatcherQueueTimer = "ad4d63fd-88fe-541f-ac11-bf2dc1ed2ce5"
    const val IDispatcherQueueTimer_put_Interval = 7   // put_Interval(TimeSpan) — an int64 in 100ns units, passed by value
    const val IDispatcherQueueTimer_put_IsRepeating = 10 // put_IsRepeating(boolean)
    const val IDispatcherQueueTimer_Start = 11         // Start()
    const val IDispatcherQueueTimer_Stop = 12          // Stop()
    const val IDispatcherQueueTimer_add_Tick = 13      // add_Tick(TypedEventHandler<DispatcherQueueTimer, Object>, out token)

    /** The actual IID (computed at runtime) of TypedEventHandler<DispatcherQueueTimer, Object>. */
    val IID_DispatcherQueueTimerTickHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Dispatching.DispatcherQueueTimer;{$IID_IDispatcherQueueTimer});" +
                "cinterface(IInspectable))",
        )
    }

    // ---- Windows.Foundation async (IAsyncAction / IAsyncOperation<T>) ----
    const val IID_IAsyncInfo = "00000036-0000-0000-c000-000000000046"
    const val IAsyncInfo_get_Status = 7                // get_Status(out AsyncStatus)
    const val IAsyncInfo_get_ErrorCode = 8             // get_ErrorCode(out HRESULT)

    /** AsyncStatus.Completed = 1 (from FoundationContract.winmd). */
    const val AsyncStatus_Completed = 1

    const val IID_IAsyncAction = "5a648006-843a-4da9-865b-9d26e5dfad7b"
    const val IAsyncAction_put_Completed = 6           // put_Completed(AsyncActionCompletedHandler)

    /** delegate AsyncActionCompletedHandler — Invoke(IAsyncAction, AsyncStatus) is vtbl[3] */
    const val IID_AsyncActionCompletedHandler = "a4ed5c81-76c9-40bd-8be6-b1d90fb20ae7"

    // IAsyncOperation`1: put_Completed=6 get_Completed=7 GetResults=8
    const val IAsyncOperation_put_Completed = 6        // put_Completed(AsyncOperationCompletedHandler<T>)
    const val IAsyncOperation_GetResults = 8           // GetResults(out T)
    private const val IID_AsyncOperationCompletedHandler_OPEN = "fcdcf02c-e5d8-4478-915a-4d90b74b83a5"

    // ---- Microsoft.Windows.AppNotifications.AppNotificationManager ----
    // (Microsoft.Windows.AppNotifications.winmd — WinAppSDK Foundation 2.1.0)
    const val CLS_AppNotificationManager = "Microsoft.Windows.AppNotifications.AppNotificationManager"
    const val IID_IAppNotificationManagerStatics = "6cfc0d8d-84a3-5592-b4c6-e3e7e7c680e4"
    const val IAppNotificationManagerStatics_get_Default = 6 // get_Default(out AppNotificationManager)
    const val IID_IAppNotificationManagerStatics2 = "6eb42a35-e82f-5732-98f1-129705602f2e"
    const val IAppNotificationManagerStatics2_IsSupported = 6 // IsSupported(out boolean)
    const val IID_IAppNotificationManager = "55129688-b4bd-550b-ae6b-c24061954d91"
    const val IAppNotificationManager_Register = 6
    const val IAppNotificationManager_Unregister = 7
    const val IAppNotificationManager_UnregisterAll = 8
    const val IAppNotificationManager_add_NotificationInvoked = 9 // add(TypedEventHandler<Manager, ActivatedEventArgs>, out token)
    const val IAppNotificationManager_remove_NotificationInvoked = 10
    const val IAppNotificationManager_Show = 11        // Show(AppNotification)
    const val IAppNotificationManager_get_Setting = 14 // get_Setting(out AppNotificationSetting)

    // ---- Microsoft.Windows.AppNotifications.AppNotification ----
    const val CLS_AppNotification = "Microsoft.Windows.AppNotifications.AppNotification"
    const val IID_IAppNotification = "373a6917-4116-5657-936a-15f99afdd667"
    const val IAppNotification_get_Tag = 6
    const val IAppNotification_put_Tag = 7
    const val IAppNotification_get_Group = 8
    const val IAppNotification_put_Group = 9
    const val IAppNotification_get_Id = 10             // get_Id(out UINT32)
    const val IAppNotification_get_Payload = 11        // get_Payload(out HSTRING)

    const val IID_IAppNotificationActivatedEventArgs = "7a8afaf9-31cb-51d5-82be-db6bd5878b77"
    const val IAppNotificationActivatedEventArgs_get_Argument = 6 // get_Argument(out HSTRING)

    /** The actual IID of TypedEventHandler<AppNotificationManager, AppNotificationActivatedEventArgs>. */
    val IID_NotificationInvokedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.Windows.AppNotifications.AppNotificationManager;" +
                "{$IID_IAppNotificationManager});" +
                "rc(Microsoft.Windows.AppNotifications.AppNotificationActivatedEventArgs;" +
                "{$IID_IAppNotificationActivatedEventArgs}))",
        )
    }

    // ---- Microsoft.Windows.AppNotifications.Builder.AppNotificationBuilder ----
    // (Microsoft.Windows.AppNotifications.Builder.winmd)
    const val CLS_AppNotificationBuilder =
        "Microsoft.Windows.AppNotifications.Builder.AppNotificationBuilder"
    const val IID_IAppNotificationBuilder = "e801d31f-ce03-505c-adec-8a02724ec9de"
    const val IAppNotificationBuilder_AddArgument = 6  // AddArgument(HSTRING, HSTRING, out this)
    const val IAppNotificationBuilder_SetDuration = 8  // SetDuration(AppNotificationDuration, out this)
    const val IAppNotificationBuilder_SetScenario = 9  // SetScenario(AppNotificationScenario, out this)
    const val IAppNotificationBuilder_AddText = 10     // AddText(HSTRING, out this)
    const val IAppNotificationBuilder_SetAttributionText = 12 // SetAttributionText(HSTRING, out this)
    const val IAppNotificationBuilder_AddButton = 29   // AddButton(AppNotificationButton, out this)
    const val IAppNotificationBuilder_BuildNotification = 32 // BuildNotification(out AppNotification)
    const val IAppNotificationBuilder_SetTag = 33      // SetTag(HSTRING, out this)
    const val IAppNotificationBuilder_SetGroup = 34    // SetGroup(HSTRING, out this)

    const val CLS_AppNotificationButton =
        "Microsoft.Windows.AppNotifications.Builder.AppNotificationButton"
    const val IID_IAppNotificationButtonFactory = "4f109286-0a6d-5a5e-9e8f-9fe31669fbb8"
    const val IAppNotificationButtonFactory_CreateInstance = 6 // CreateInstance(HSTRING content, out AppNotificationButton)
    const val IID_IAppNotificationButton = "a7c03031-5634-5098-aec9-47ecb60c3499"
    const val IAppNotificationButton_AddArgument = 24  // AddArgument(HSTRING, HSTRING, out this)

    // ---- Microsoft.Windows.BadgeNotifications.BadgeNotificationManager ----
    // (Microsoft.Windows.BadgeNotifications.winmd)
    const val CLS_BadgeNotificationManager =
        "Microsoft.Windows.BadgeNotifications.BadgeNotificationManager"
    const val IID_IBadgeNotificationManagerStatics = "a6e71616-7c9f-5d22-ad1c-f4ab874087b5"
    const val IBadgeNotificationManagerStatics_get_Current = 6 // get_Current(out BadgeNotificationManager)
    const val IID_IBadgeNotificationManager = "11cb6e8f-11ca-53f8-80f6-5330d44ba908"
    const val IBadgeNotificationManager_SetBadgeAsCount = 6 // SetBadgeAsCount(UINT32)
    const val IBadgeNotificationManager_SetBadgeAsGlyph = 7 // SetBadgeAsGlyph(BadgeNotificationGlyph)
    const val IBadgeNotificationManager_ClearBadge = 8

    // ---- Windows.UI.StartScreen.JumpList (UniversalApiContract.winmd — OS side) ----
    const val CLS_JumpList = "Windows.UI.StartScreen.JumpList"
    const val IID_IJumpListStatics = "a7e0c681-e67e-4b74-8250-3f322c4d92c3"
    const val IJumpListStatics_LoadCurrentAsync = 6    // LoadCurrentAsync(out IAsyncOperation<JumpList>)
    const val IJumpListStatics_IsSupported = 7         // IsSupported(out boolean)
    const val IID_IJumpList = "b0234c3e-cd6f-4cb6-a611-61fd505f3ed1"
    const val IJumpList_get_Items = 6                  // get_Items(out IVector<JumpListItem>)
    const val IJumpList_get_SystemGroupKind = 7        // get_SystemGroupKind(out JumpListSystemGroupKind)
    const val IJumpList_put_SystemGroupKind = 8
    const val IJumpList_SaveAsync = 9                  // SaveAsync(out IAsyncAction)

    const val CLS_JumpListItem = "Windows.UI.StartScreen.JumpListItem"
    const val IID_IJumpListItemStatics = "f1bfc4e8-c7aa-49cb-8dde-ecfccd7ad7e4"
    const val IJumpListItemStatics_CreateWithArguments = 6 // CreateWithArguments(HSTRING args, HSTRING displayName, out JumpListItem)
    const val IJumpListItemStatics_CreateSeparator = 7 // CreateSeparator(out JumpListItem)
    const val IID_IJumpListItem = "7adb6717-8b5d-4820-995b-9b418dbe48b0"
    const val IJumpListItem_get_Kind = 6               // get_Kind(out JumpListItemKind)
    const val IJumpListItem_get_Arguments = 7
    const val IJumpListItem_get_RemovedByUser = 8
    const val IJumpListItem_get_Description = 9
    const val IJumpListItem_put_Description = 10
    const val IJumpListItem_get_DisplayName = 11
    const val IJumpListItem_put_DisplayName = 12
    const val IJumpListItem_get_GroupName = 13
    const val IJumpListItem_put_GroupName = 14

    /** The actual IID of AsyncOperationCompletedHandler<JumpList> (LoadCurrentAsync's completion notice). */
    val IID_AsyncOperationCompletedHandler_JumpList: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_AsyncOperationCompletedHandler_OPEN};" +
                "rc(Windows.UI.StartScreen.JumpList;{$IID_IJumpList}))",
        )
    }

    // ---- Microsoft.UI.Xaml.Controls.MenuBar / MenuBarItem ----
    const val CLS_MenuBar = "Microsoft.UI.Xaml.Controls.MenuBar"
    const val IID_IMenuBarFactory = "76aa8759-04ee-5a4c-b98c-d03742d47cdb"
    const val IID_IMenuBar = "ba97f337-8f1e-5141-b53f-e77a8ba3ebbd"
    const val IMenuBar_get_Items = 6                   // get_Items(out IVector<MenuBarItem>)
    const val CLS_MenuBarItem = "Microsoft.UI.Xaml.Controls.MenuBarItem"
    const val IID_IMenuBarItemFactory = "87d02172-83cb-5459-940f-173f7501b300"
    const val IID_IMenuBarItem = "a7900980-51cc-531d-97c5-356b13573398"
    const val IMenuBarItem_get_Title = 6               // get_Title(out HSTRING)
    const val IMenuBarItem_put_Title = 7               // put_Title(HSTRING)
    const val IMenuBarItem_get_Items = 8               // get_Items(out IVector<MenuFlyoutItemBase>)

    // ---- Microsoft.UI.Xaml.Controls.MenuFlyout ----
    const val CLS_MenuFlyout = "Microsoft.UI.Xaml.Controls.MenuFlyout"
    const val IID_IMenuFlyoutFactory = "a3d225de-6b35-5442-b6c9-06fd24139a63"
    const val IID_IMenuFlyout = "f4c77c6c-1fa5-5d85-8559-5d02b7d4e5e7"
    const val IMenuFlyout_get_Items = 6                // get_Items(out IVector<MenuFlyoutItemBase>)

    // ---- Microsoft.UI.Xaml.Controls.MenuFlyoutItemBase (common base for menu items) ----
    const val IID_IMenuFlyoutItemBase = "4bee2715-44a1-5f94-86e8-02ddbe3dc6b9"

    // ---- Microsoft.UI.Xaml.Controls.MenuFlyoutItem ----
    const val CLS_MenuFlyoutItem = "Microsoft.UI.Xaml.Controls.MenuFlyoutItem"
    const val IID_IMenuFlyoutItemFactory = "9c3c9a1f-89af-521a-81a5-8a01db7a79af"
    const val IID_IMenuFlyoutItem = "4252df5a-44f9-5ee8-b1cc-53de9aaa4095"
    const val IMenuFlyoutItem_get_Text = 6             // get_Text(out HSTRING)
    const val IMenuFlyoutItem_put_Text = 7             // put_Text(HSTRING)
    const val IMenuFlyoutItem_put_Command = 9          // put_Command(ICommand)
    const val IMenuFlyoutItem_put_CommandParameter = 11 // put_CommandParameter(IInspectable)
    const val IMenuFlyoutItem_put_Icon = 13            // put_Icon(IconElement)
    const val IMenuFlyoutItem_get_KeyboardAcceleratorTextOverride = 14 // (out HSTRING)
    const val IMenuFlyoutItem_put_KeyboardAcceleratorTextOverride = 15 // (HSTRING)
    const val IMenuFlyoutItem_add_Click = 17           // add_Click(RoutedEventHandler, out token)
    const val IMenuFlyoutItem_remove_Click = 18        // remove_Click(token)

    // ---- Microsoft.UI.Xaml.Controls.MenuFlyoutSubItem (activatable) ----
    const val CLS_MenuFlyoutSubItem = "Microsoft.UI.Xaml.Controls.MenuFlyoutSubItem"
    const val IID_IMenuFlyoutSubItem = "6b0688c1-47b0-53b5-b6f9-5ec5d6623b84"
    const val IMenuFlyoutSubItem_get_Items = 6         // get_Items(out IVector<MenuFlyoutItemBase>)
    const val IMenuFlyoutSubItem_get_Text = 7          // get_Text(out HSTRING)
    const val IMenuFlyoutSubItem_put_Text = 8          // put_Text(HSTRING)
    const val IMenuFlyoutSubItem_put_Icon = 10         // put_Icon(IconElement)

    // ---- Microsoft.UI.Xaml.Controls.MenuFlyoutSeparator ----
    const val CLS_MenuFlyoutSeparator = "Microsoft.UI.Xaml.Controls.MenuFlyoutSeparator"
    const val IID_IMenuFlyoutSeparatorFactory = "26156c9c-95ef-5e55-8342-773fc43baac3"

    // ---- Microsoft.UI.Xaml.Controls.ToggleMenuFlyoutItem ----
    const val CLS_ToggleMenuFlyoutItem = "Microsoft.UI.Xaml.Controls.ToggleMenuFlyoutItem"
    const val IID_IToggleMenuFlyoutItemFactory = "426dfd57-6cc9-570f-950d-37437235dc89"
    const val IID_IToggleMenuFlyoutItem = "1803f260-67e4-5bc1-a63a-123510167bb8"
    const val IToggleMenuFlyoutItem_get_IsChecked = 6  // get_IsChecked(out boolean)
    const val IToggleMenuFlyoutItem_put_IsChecked = 7  // put_IsChecked(boolean)

    // ---- Microsoft.UI.Xaml.Controls.RadioMenuFlyoutItem ----
    const val CLS_RadioMenuFlyoutItem = "Microsoft.UI.Xaml.Controls.RadioMenuFlyoutItem"
    const val IID_IRadioMenuFlyoutItemFactory = "113822d2-35a3-5b0d-9d0d-c3830ec1973b"
    const val IID_IRadioMenuFlyoutItem = "9d6f45f5-5532-5669-8f02-05735953026a"
    const val IRadioMenuFlyoutItem_get_IsChecked = 6   // get_IsChecked(out boolean)
    const val IRadioMenuFlyoutItem_put_IsChecked = 7   // put_IsChecked(boolean)
    const val IRadioMenuFlyoutItem_get_GroupName = 8   // get_GroupName(out HSTRING)
    const val IRadioMenuFlyoutItem_put_GroupName = 9   // put_GroupName(HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.AppBar (the base of CommandBar) ----
    const val IID_IAppBar = "3d8c2927-5ac5-51bb-8bec-13ff4c1bd6c8"
    const val IAppBar_get_IsOpen = 6                   // get_IsOpen(out boolean)
    const val IAppBar_put_IsOpen = 7                   // put_IsOpen(boolean)
    const val IAppBar_get_IsSticky = 8                 // get_IsSticky(out boolean)
    const val IAppBar_put_IsSticky = 9                 // put_IsSticky(boolean)
    const val IAppBar_get_ClosedDisplayMode = 10       // get_ClosedDisplayMode(out AppBarClosedDisplayMode)
    const val IAppBar_put_ClosedDisplayMode = 11       // put_ClosedDisplayMode(AppBarClosedDisplayMode)
    const val IAppBar_add_Opened = 17                  // add_Opened(EventHandler<Object>, out token)
    const val IAppBar_remove_Opened = 18               // remove_Opened(token)
    const val IAppBar_add_Closed = 21                  // add_Closed(EventHandler<Object>, out token)
    const val IAppBar_remove_Closed = 22               // remove_Closed(token)

    // ---- Microsoft.UI.Xaml.Controls.CommandBar ----
    const val CLS_CommandBar = "Microsoft.UI.Xaml.Controls.CommandBar"
    const val IID_ICommandBarFactory = "8d4079c3-fa0a-5bb1-b45d-499c378761b4"
    const val IID_ICommandBar = "b7ca8ee3-a07a-5f69-8ab8-be4e3e4cf0c8"
    const val ICommandBar_get_PrimaryCommands = 6      // get_PrimaryCommands(out IObservableVector<ICommandBarElement>)
    const val ICommandBar_get_SecondaryCommands = 7    // get_SecondaryCommands(out IObservableVector<ICommandBarElement>)
    const val ICommandBar_get_DefaultLabelPosition = 11 // get_DefaultLabelPosition(out CommandBarDefaultLabelPosition)
    const val ICommandBar_put_DefaultLabelPosition = 12 // put_DefaultLabelPosition(CommandBarDefaultLabelPosition)
    const val ICommandBar_get_OverflowButtonVisibility = 13 // (out CommandBarOverflowButtonVisibility)
    const val ICommandBar_put_OverflowButtonVisibility = 14 // (CommandBarOverflowButtonVisibility)
    const val ICommandBar_get_IsDynamicOverflowEnabled = 15 // get_IsDynamicOverflowEnabled(out boolean)
    const val ICommandBar_put_IsDynamicOverflowEnabled = 16 // put_IsDynamicOverflowEnabled(boolean)

    // ---- Microsoft.UI.Xaml.Controls.ICommandBarElement (implemented by AppBarButton and others) ----
    const val IID_ICommandBarElement = "f8eb20b4-373e-5327-9942-66a1ea21f5f9"
    const val ICommandBarElement_get_IsCompact = 6     // get_IsCompact(out boolean)
    const val ICommandBarElement_put_IsCompact = 7     // put_IsCompact(boolean)
    const val ICommandBarElement_get_IsInOverflow = 8  // get_IsInOverflow(out boolean)
    const val ICommandBarElement_get_DynamicOverflowOrder = 9 // get_DynamicOverflowOrder(out INT32)
    const val ICommandBarElement_put_DynamicOverflowOrder = 10 // put_DynamicOverflowOrder(INT32)

    // ---- Microsoft.UI.Xaml.Controls.AppBarButton ----
    const val CLS_AppBarButton = "Microsoft.UI.Xaml.Controls.AppBarButton"
    const val IID_IAppBarButtonFactory = "4168a40a-d11f-5aeb-974e-bb43a6e7f9b2"
    const val IID_IAppBarButton = "8ab0e278-b6ae-569e-8dcd-d293552fe4d5"
    const val IAppBarButton_get_Label = 6              // get_Label(out HSTRING)
    const val IAppBarButton_put_Label = 7              // put_Label(HSTRING)
    const val IAppBarButton_put_Icon = 9               // put_Icon(IconElement)
    const val IAppBarButton_get_LabelPosition = 10     // get_LabelPosition(out CommandBarLabelPosition)
    const val IAppBarButton_put_LabelPosition = 11     // put_LabelPosition(CommandBarLabelPosition)
    const val IAppBarButton_get_KeyboardAcceleratorTextOverride = 12 // (out HSTRING)
    const val IAppBarButton_put_KeyboardAcceleratorTextOverride = 13 // (HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.AppBarToggleButton ----
    const val CLS_AppBarToggleButton = "Microsoft.UI.Xaml.Controls.AppBarToggleButton"
    const val IID_IAppBarToggleButtonFactory = "07bfb2d6-23b9-57a2-9122-006294bfa92f"
    const val IID_IAppBarToggleButton = "9687c0b1-c390-59be-acdc-4fc48f552823"
    const val IAppBarToggleButton_get_Label = 6        // get_Label(out HSTRING)
    const val IAppBarToggleButton_put_Label = 7        // put_Label(HSTRING)
    const val IAppBarToggleButton_put_Icon = 9         // put_Icon(IconElement)
    const val IAppBarToggleButton_get_LabelPosition = 10 // get_LabelPosition(out CommandBarLabelPosition)
    const val IAppBarToggleButton_put_LabelPosition = 11 // put_LabelPosition(CommandBarLabelPosition)
    const val IAppBarToggleButton_get_KeyboardAcceleratorTextOverride = 12 // (out HSTRING)
    const val IAppBarToggleButton_put_KeyboardAcceleratorTextOverride = 13 // (HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.AppBarSeparator ----
    const val CLS_AppBarSeparator = "Microsoft.UI.Xaml.Controls.AppBarSeparator"
    const val IID_IAppBarSeparatorFactory = "6497d326-fb55-5cf5-8cc4-c556b1a958fb"

    // ---- Microsoft.UI.Xaml.Controls.CommandBarFlyout ----
    const val CLS_CommandBarFlyout = "Microsoft.UI.Xaml.Controls.CommandBarFlyout"
    const val IID_ICommandBarFlyoutFactory = "a194dbe6-4311-5bd2-a8eb-b49c4733a33c"
    const val IID_ICommandBarFlyout = "18e2cc40-09cb-5f20-a715-f0b2039c5e18"
    const val ICommandBarFlyout_get_PrimaryCommands = 6 // get_PrimaryCommands(out IObservableVector<ICommandBarElement>)
    const val ICommandBarFlyout_get_SecondaryCommands = 7 // get_SecondaryCommands(out IObservableVector<ICommandBarElement>)

    // ---- Microsoft.UI.Xaml.Controls.SymbolIconSource (an IconSource subclass) ----
    const val IID_IIconSource = "39e6b320-a2af-5ee3-b7e9-4ba4aa80541a"
    const val CLS_SymbolIconSource = "Microsoft.UI.Xaml.Controls.SymbolIconSource"
    const val IID_ISymbolIconSourceFactory = "afbf55fa-ff79-552a-a3a1-48e2ef17152a"
    const val IID_ISymbolIconSource = "45120d6b-e868-59f2-a30a-b1915ce374bd"
    const val ISymbolIconSource_put_Symbol = 7         // put_Symbol(Symbol)

    // ---- Microsoft.UI.Xaml.Controls.SwipeControl / SwipeItems / SwipeItem ----
    const val CLS_SwipeControl = "Microsoft.UI.Xaml.Controls.SwipeControl"
    const val IID_ISwipeControlFactory = "6e9213f4-30c1-56ca-9905-46aa1a83dfc1"
    const val IID_ISwipeControl = "337800fe-f51d-5f6b-9d35-e7f5de10a97c"
    const val ISwipeControl_put_LeftItems = 7          // put_LeftItems(SwipeItems)
    const val ISwipeControl_put_RightItems = 9         // put_RightItems(SwipeItems)
    const val ISwipeControl_put_TopItems = 11          // put_TopItems(SwipeItems)
    const val ISwipeControl_put_BottomItems = 13       // put_BottomItems(SwipeItems)
    const val ISwipeControl_Close = 14                 // Close()
    const val CLS_SwipeItems = "Microsoft.UI.Xaml.Controls.SwipeItems"
    const val IID_ISwipeItemsFactory = "9a5d7d77-39bd-5b81-ab4e-f44aca6ede3f"
    const val IID_ISwipeItems = "40d3022b-c82b-5565-a4e4-ad9d7a66b1a9"
    const val ISwipeItems_get_Mode = 6                 // get_Mode(out SwipeMode)
    const val ISwipeItems_put_Mode = 7                 // put_Mode(SwipeMode)
    const val CLS_SwipeItem = "Microsoft.UI.Xaml.Controls.SwipeItem"
    const val IID_ISwipeItemFactory = "011513e1-1adc-5a69-a74f-623fcc18a51c"
    const val IID_ISwipeItem = "5e8d4950-b826-510a-b309-77afcdc65b1f"
    const val ISwipeItem_get_Text = 6                  // get_Text(out HSTRING)
    const val ISwipeItem_put_Text = 7                  // put_Text(HSTRING)
    const val ISwipeItem_put_IconSource = 9            // put_IconSource(IconSource)
    const val ISwipeItem_put_Background = 11           // put_Background(Brush)
    const val ISwipeItem_put_Foreground = 13           // put_Foreground(Brush)
    const val ISwipeItem_put_Command = 15              // put_Command(ICommand)
    const val ISwipeItem_get_BehaviorOnInvoked = 18    // get_BehaviorOnInvoked(out SwipeBehaviorOnInvoked)
    const val ISwipeItem_put_BehaviorOnInvoked = 19    // put_BehaviorOnInvoked(SwipeBehaviorOnInvoked)
    const val ISwipeItem_add_Invoked = 20              // add_Invoked(TypedEventHandler<SwipeItem, SwipeItemInvokedEventArgs>, out token)
    const val ISwipeItem_remove_Invoked = 21           // remove_Invoked(token)
    const val IID_ISwipeItemInvokedEventArgs = "7bf51c95-53a2-5a0d-bb32-2b4b1f7d2f20"

    /** The actual IID of TypedEventHandler<SwipeItem, SwipeItemInvokedEventArgs> (computed at runtime). */
    val IID_SwipeItemInvokedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.SwipeItem;{$IID_ISwipeItem});" +
                "rc(Microsoft.UI.Xaml.Controls.SwipeItemInvokedEventArgs;" +
                "{$IID_ISwipeItemInvokedEventArgs}))",
        )
    }

    // ---- Microsoft.UI.Xaml.Input.XamlUICommand / StandardUICommand ----
    const val CLS_XamlUICommand = "Microsoft.UI.Xaml.Input.XamlUICommand"
    const val IID_IXamlUICommandFactory = "f1f80a20-0e31-5505-8bc3-cdd1f0947f1d"
    const val IID_IXamlUICommand = "a457f2cb-51e0-541c-9c42-dd1dcbdf58fb"
    const val IXamlUICommand_get_Label = 6             // get_Label(out HSTRING)
    const val IXamlUICommand_put_Label = 7             // put_Label(HSTRING)
    const val IXamlUICommand_put_IconSource = 9        // put_IconSource(IconSource)
    const val IXamlUICommand_get_KeyboardAccelerators = 10 // get_KeyboardAccelerators(out IVector<KeyboardAccelerator>)
    const val IXamlUICommand_get_AccessKey = 11        // get_AccessKey(out HSTRING)
    const val IXamlUICommand_put_AccessKey = 12        // put_AccessKey(HSTRING)
    const val IXamlUICommand_get_Description = 13      // get_Description(out HSTRING)
    const val IXamlUICommand_put_Description = 14      // put_Description(HSTRING)
    const val IXamlUICommand_add_ExecuteRequested = 17 // add_ExecuteRequested(TypedEventHandler<XamlUICommand, ExecuteRequestedEventArgs>, out token)
    const val IXamlUICommand_remove_ExecuteRequested = 18 // remove_ExecuteRequested(token)
    const val IXamlUICommand_add_CanExecuteRequested = 19 // add_CanExecuteRequested(TypedEventHandler<XamlUICommand, CanExecuteRequestedEventArgs>, out token)
    const val IXamlUICommand_remove_CanExecuteRequested = 20 // remove_CanExecuteRequested(token)
    const val IXamlUICommand_NotifyCanExecuteChanged = 21 // NotifyCanExecuteChanged()
    const val IID_IExecuteRequestedEventArgs = "e1a9fd0c-34d0-5ae2-8f5d-377e7a8a2708"
    const val IExecuteRequestedEventArgs_get_Parameter = 6 // get_Parameter(out IInspectable)
    const val IID_ICanExecuteRequestedEventArgs = "e4bf6d7d-f6eb-53ca-a2d4-c741ec871e38"
    const val ICanExecuteRequestedEventArgs_put_CanExecute = 8 // put_CanExecute(boolean)

    const val CLS_StandardUICommand = "Microsoft.UI.Xaml.Input.StandardUICommand"
    const val IID_IStandardUICommandFactory = "5800f099-3746-5bcf-b1ce-af3d6bf8e83f"
    const val IStandardUICommandFactory_CreateInstanceWithKind = 7 // (StandardUICommandKind, outer, out inner, out instance)
    const val IID_IStandardUICommand = "5f395d50-5449-59ab-9cb2-4e3700033f03"
    const val IStandardUICommand_get_Kind = 6          // get_Kind(out StandardUICommandKind)
    const val IStandardUICommand_put_Kind = 7          // put_Kind(StandardUICommandKind)

    /** The actual IID of TypedEventHandler<XamlUICommand, ExecuteRequestedEventArgs> (computed at runtime). */
    val IID_XamlUICommandExecuteRequestedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Input.XamlUICommand;{$IID_IXamlUICommand});" +
                "rc(Microsoft.UI.Xaml.Input.ExecuteRequestedEventArgs;" +
                "{$IID_IExecuteRequestedEventArgs}))",
        )
    }

    /** The actual IID of TypedEventHandler<XamlUICommand, CanExecuteRequestedEventArgs> (computed at runtime). */
    val IID_XamlUICommandCanExecuteRequestedHandler: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_TypedEventHandler_OPEN};" +
                "rc(Microsoft.UI.Xaml.Input.XamlUICommand;{$IID_IXamlUICommand});" +
                "rc(Microsoft.UI.Xaml.Input.CanExecuteRequestedEventArgs;" +
                "{$IID_ICanExecuteRequestedEventArgs}))",
        )
    }

    // ---- Microsoft.UI.Xaml.Input.KeyboardAccelerator ----
    const val CLS_KeyboardAccelerator = "Microsoft.UI.Xaml.Input.KeyboardAccelerator"
    const val IID_IKeyboardAcceleratorFactory = "ca1d410a-af2a-51b9-a1de-6c0af9f3b598"
    const val IID_IKeyboardAccelerator = "6f8bf1e2-4e91-5cf9-a6be-4770caf3d770"
    const val IKeyboardAccelerator_put_Key = 7         // put_Key(VirtualKey)
    const val IKeyboardAccelerator_put_Modifiers = 9   // put_Modifiers(VirtualKeyModifiers)

    /** The actual IID of IVector<Microsoft.UI.Xaml.Controls.ICommandBarElement> (for PrimaryCommands' QI). */
    val IID_IVector_ICommandBarElement: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_IVector_OPEN};{$IID_ICommandBarElement})",
        )
    }

    /** The actual IID of IVector<Microsoft.UI.Xaml.Controls.SwipeItem> (for SwipeItems' QI). */
    val IID_IVector_SwipeItem: String by lazy {
        WinRt.pinterfaceIid(
            "pinterface({$IID_IVector_OPEN};" +
                "rc(Microsoft.UI.Xaml.Controls.SwipeItem;{$IID_ISwipeItem}))",
        )
    }
}
