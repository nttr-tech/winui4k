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
    const val IID_IFrameworkElement = "fe08f13d-dc6a-5495-ad44-c2d8d21863b0"
    const val IFrameworkElement_put_Width = 16
    const val IFrameworkElement_put_Height = 18
    const val IFrameworkElement_put_Margin = 32        // put_Margin(Thickness) — struct passed by value

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
    const val IID_ISelector = "8f7e2159-e61d-576f-8476-f83fde3d689e" // Controls.Primitives
    const val ISelector_get_SelectedIndex = 6          // get_SelectedIndex(out INT32)
    const val ISelector_put_SelectedIndex = 7          // put_SelectedIndex(INT32)
    const val ISelector_get_SelectedItem = 8           // get_SelectedItem(out IInspectable)
    const val ISelector_add_SelectionChanged = 16      // add_SelectionChanged(SelectionChangedEventHandler, out token)
    const val ISelector_remove_SelectionChanged = 17   // remove_SelectionChanged(token)
    const val IID_IItemsControl = "bf1ccb54-83e2-5b98-acbc-736f876c3d35"
    const val IItemsControl_get_Items = 8              // get_Items(out ItemCollection)

    /** delegate Controls.SelectionChangedEventHandler(sender, SelectionChangedEventArgs) — Invoke is vtbl[3] */
    const val IID_SelectionChangedEventHandler = "a232390d-0e34-595e-8931-fa928a9909f4"

    /** delegate Controls.ItemClickEventHandler(sender, ItemClickEventArgs) — Invoke is vtbl[3] */
    const val IID_ItemClickEventHandler = "a3903624-3393-566c-a6b9-a6b4b3e301c3"
    const val IID_IItemClickEventArgs = "1cf87a70-6348-57ec-9eac-fa0565adc60f"
    const val IItemClickEventArgs_get_ClickedItem = 6  // get_ClickedItem(out IInspectable)

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
}
