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
    const val IFrameworkElement_put_Margin = 32        // put_Margin(Thickness) — struct passed by value

    // ---- Microsoft.UI.Xaml.Controls.StackPanel ----
    const val CLS_StackPanel = "Microsoft.UI.Xaml.Controls.StackPanel"
    const val IID_IStackPanelFactory = "64c1d388-47a2-5a74-a75b-559d151ee5ac"
    const val IID_IStackPanel = "493ab00b-3a6a-5e4a-9452-407cd5197406"
    const val IStackPanel_put_Spacing = 21
    const val IID_IPanel = "27a1b418-56f3-525e-b883-cefed905eed3"
    const val IPanel_get_Children = 6                  // -> IVector<UIElement>

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

    // ---- Windows.Foundation.Collections.IVector<T> (OS side, FoundationContract.winmd) ----
    // GetAt=6 get_Size=7 GetView=8 IndexOf=9 SetAt=10 InsertAt=11 RemoveAt=12 Append=13 ...
    const val IVector_Append = 13
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
}
