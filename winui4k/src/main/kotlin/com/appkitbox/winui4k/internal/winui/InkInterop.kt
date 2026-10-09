package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for ink (InkCanvas / InkToolbar and the OS-side types they use, such as Windows.UI.Input.Inking)
 * (Microsoft.Windows.Storage.Pickers, WinAppSDK).
 *
 * - Microsoft.UI.Xaml.Controls.Ink* are extracted from metadata/Microsoft.UI.Xaml.winmd in
 *   Microsoft.WindowsAppSDK.WinUI 2.3.10-experimental (a dependency of Microsoft.WindowsAppSDK 2.5.4-experimental).
 *   The types are experimental (MUX_PREVIEW) and may change before the stable release, so they are kept separate
 *   from XamlInterop
 * - Windows.UI.Input.Inking / Windows.UI.Core / Windows.UI.Input / Windows.Devices.Input /
 *   Windows.Storage.Streams are extracted from the Windows SDK's Windows.Foundation.UniversalApiContract.winmd,
 *   and Windows.Foundation.IClosable from Windows.Foundation.FoundationContract.winmd
 *
 * All values are mechanically extracted with tools/dump_winmd.py. Not a single value is handwritten or guessed.
 * The concrete IIDs of generic types are computed at runtime with SHA-1 from their WinRT signatures ([Pinterface]).
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, and the interface body
 * starts at 6 in the winmd's method-declaration order.
 */
internal object InkInterop {
    // ==================================================================
    // Microsoft.UI.Xaml.Controls (Microsoft.UI.Xaml.winmd, experimental)
    // ==================================================================

    // ---- Microsoft.UI.Xaml.Controls.InkCanvas ----
    // base: Microsoft.UI.Xaml.FrameworkElement / composable factory: Microsoft.UI.Xaml.Controls.IInkCanvasFactory
    const val CLS_InkCanvas = "Microsoft.UI.Xaml.Controls.InkCanvas"
    const val IID_IInkCanvas = "a851b443-43f0-50be-9c41-9ee631e06b06"
    const val IInkCanvas_get_InkPresenter = 6   // get_InkPresenter(out InkPresenter)
    const val IID_IInkCanvasFactory = "4538da03-dff2-5828-bfe0-434e725beddb"
    const val IInkCanvasFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkCanvas)

    // ---- Microsoft.UI.Xaml.Controls.InkPresenter ----
    // base: System.Object
    const val CLS_InkPresenter = "Microsoft.UI.Xaml.Controls.InkPresenter"
    const val IID_IInkPresenter = "7c5d0955-41a1-5b10-9435-ae870b1b7b14"
    const val IInkPresenter_get_InputDeviceTypes = 6                // get_InputDeviceTypes(out CoreInputDeviceTypes)
    const val IInkPresenter_put_InputDeviceTypes = 7                // put_InputDeviceTypes(CoreInputDeviceTypes)
    const val IInkPresenter_get_IsInputEnabled = 8                  // get_IsInputEnabled(out boolean)
    const val IInkPresenter_put_IsInputEnabled = 9                  // put_IsInputEnabled(boolean)
    const val IInkPresenter_UpdateDefaultDrawingAttributes = 10     // UpdateDefaultDrawingAttributes(InkDrawingAttributes)
    const val IInkPresenter_CopyDefaultDrawingAttributes = 11       // CopyDefaultDrawingAttributes(out InkDrawingAttributes)
    const val IInkPresenter_SetPredefinedConfiguration = 12         // SetPredefinedConfiguration(InkPresenterPredefinedConfiguration)
    const val IInkPresenter_get_HighContrastAdjustment = 13         // get_HighContrastAdjustment(out InkHighContrastAdjustment)
    const val IInkPresenter_put_HighContrastAdjustment = 14         // put_HighContrastAdjustment(InkHighContrastAdjustment)
    const val IInkPresenter_get_StrokeContainer = 15                // get_StrokeContainer(out InkStrokeContainer)
    const val IInkPresenter_get_InputProcessingConfiguration = 16   // get_InputProcessingConfiguration(out InkInputProcessingConfiguration)
    const val IInkPresenter_get_InputConfiguration = 17             // get_InputConfiguration(out InkInputConfiguration)
    const val IInkPresenter_get_StrokeInput = 18                    // get_StrokeInput(out InkStrokeInput)
    const val IInkPresenter_get_UnprocessedInput = 19               // get_UnprocessedInput(out InkUnprocessedInput)
    const val IInkPresenter_ActivateCustomDrying = 20               // ActivateCustomDrying(out InkSynchronizer)
    const val IInkPresenter_add_StrokesCollected = 21               // add_StrokesCollected(TypedEventHandler<InkPresenter, InkStrokesCollectedEventArgs>, out EventRegistrationToken)
    const val IInkPresenter_remove_StrokesCollected = 22            // remove_StrokesCollected(EventRegistrationToken)
    const val IInkPresenter_add_StrokesErased = 23                  // add_StrokesErased(TypedEventHandler<InkPresenter, InkStrokesErasedEventArgs>, out EventRegistrationToken)
    const val IInkPresenter_remove_StrokesErased = 24               // remove_StrokesErased(EventRegistrationToken)

    // ---- Microsoft.UI.Xaml.Controls.InkStrokeContainer ----
    // base: System.Object
    const val CLS_InkStrokeContainer = "Microsoft.UI.Xaml.Controls.InkStrokeContainer"
    const val IID_IInkStrokeContainer = "01d0507d-22ab-537f-9729-cee80c764b68"
    const val IInkStrokeContainer_Clear = 6                      // Clear()
    const val IInkStrokeContainer_GetStrokes = 7                 // GetStrokes(out IVectorView<InkStroke>)
    const val IInkStrokeContainer_AddStroke = 8                  // AddStroke(InkStroke)
    const val IInkStrokeContainer_AddStrokes = 9                 // AddStrokes(IIterable<InkStroke>)
    const val IInkStrokeContainer_SaveAsync = 10                 // SaveAsync(IOutputStream, out IAsyncAction)
    const val IInkStrokeContainer_SaveWithFormatAsync = 11       // SaveAsync(IOutputStream, InkPersistenceFormat, out IAsyncAction)
    const val IInkStrokeContainer_LoadAsync = 12                 // LoadAsync(IInputStream, out IAsyncAction)
    const val IInkStrokeContainer_GetStrokeById = 13             // GetStrokeById(u4, out InkStroke)
    const val IInkStrokeContainer_DeleteSelected = 14            // DeleteSelected(out Rect)
    const val IInkStrokeContainer_MoveSelected = 15              // MoveSelected(Point, out Rect)
    const val IInkStrokeContainer_SelectWithLine = 16            // SelectWithLine(Point, Point, out Rect)
    const val IInkStrokeContainer_SelectWithPolyLine = 17        // SelectWithPolyLine(IIterable<Point>, out Rect)
    const val IInkStrokeContainer_get_BoundingRect = 18          // get_BoundingRect(out Rect)
    const val IInkStrokeContainer_CopySelectedToClipboard = 19   // CopySelectedToClipboard()
    const val IInkStrokeContainer_PasteFromClipboard = 20        // PasteFromClipboard(Point, out Rect)
    const val IInkStrokeContainer_CanPasteFromClipboard = 21     // CanPasteFromClipboard(out boolean)

    // ---- Microsoft.UI.Xaml.Controls.InkInputProcessingConfiguration ----
    // base: System.Object
    const val CLS_InkInputProcessingConfiguration = "Microsoft.UI.Xaml.Controls.InkInputProcessingConfiguration"
    const val IID_IInkInputProcessingConfiguration = "576bc6cb-6067-5a63-8443-ea32c1003408"
    const val IInkInputProcessingConfiguration_get_Mode = 6              // get_Mode(out InkInputProcessingMode)
    const val IInkInputProcessingConfiguration_put_Mode = 7              // put_Mode(InkInputProcessingMode)
    const val IInkInputProcessingConfiguration_get_RightDragAction = 8   // get_RightDragAction(out InkInputRightDragAction)
    const val IInkInputProcessingConfiguration_put_RightDragAction = 9   // put_RightDragAction(InkInputRightDragAction)

    // ---- Microsoft.UI.Xaml.Controls.InkInputConfiguration ----
    // base: System.Object
    const val CLS_InkInputConfiguration = "Microsoft.UI.Xaml.Controls.InkInputConfiguration"
    const val IID_IInkInputConfiguration = "da428852-86b9-5a60-8d38-cd5910ead03a"
    const val IInkInputConfiguration_get_IsPrimaryBarrelButtonInputEnabled = 6   // get_IsPrimaryBarrelButtonInputEnabled(out boolean)
    const val IInkInputConfiguration_put_IsPrimaryBarrelButtonInputEnabled = 7   // put_IsPrimaryBarrelButtonInputEnabled(boolean)
    const val IInkInputConfiguration_get_IsEraserInputEnabled = 8                // get_IsEraserInputEnabled(out boolean)
    const val IInkInputConfiguration_put_IsEraserInputEnabled = 9                // put_IsEraserInputEnabled(boolean)

    // ---- Microsoft.UI.Xaml.Controls.InkStrokeInput ----
    // base: System.Object
    const val CLS_InkStrokeInput = "Microsoft.UI.Xaml.Controls.InkStrokeInput"
    const val IID_IInkStrokeInput = "33d98fed-9ea6-55fe-a0ab-74442046e808"
    const val IInkStrokeInput_add_StrokeStarted = 6        // add_StrokeStarted(TypedEventHandler<InkStrokeInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkStrokeInput_remove_StrokeStarted = 7     // remove_StrokeStarted(EventRegistrationToken)
    const val IInkStrokeInput_add_StrokeContinued = 8      // add_StrokeContinued(TypedEventHandler<InkStrokeInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkStrokeInput_remove_StrokeContinued = 9   // remove_StrokeContinued(EventRegistrationToken)
    const val IInkStrokeInput_add_StrokeEnded = 10         // add_StrokeEnded(TypedEventHandler<InkStrokeInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkStrokeInput_remove_StrokeEnded = 11      // remove_StrokeEnded(EventRegistrationToken)
    const val IInkStrokeInput_add_StrokeCanceled = 12      // add_StrokeCanceled(TypedEventHandler<InkStrokeInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkStrokeInput_remove_StrokeCanceled = 13   // remove_StrokeCanceled(EventRegistrationToken)
    const val IInkStrokeInput_get_InkPresenter = 14        // get_InkPresenter(out InkPresenter)

    // ---- Microsoft.UI.Xaml.Controls.InkUnprocessedInput ----
    // base: System.Object
    const val CLS_InkUnprocessedInput = "Microsoft.UI.Xaml.Controls.InkUnprocessedInput"
    const val IID_IInkUnprocessedInput = "7b754563-ba8e-5beb-b5b8-84839992856e"
    const val IInkUnprocessedInput_add_PointerEntered = 6        // add_PointerEntered(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerEntered = 7     // remove_PointerEntered(EventRegistrationToken)
    const val IInkUnprocessedInput_add_PointerHovered = 8        // add_PointerHovered(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerHovered = 9     // remove_PointerHovered(EventRegistrationToken)
    const val IInkUnprocessedInput_add_PointerExited = 10        // add_PointerExited(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerExited = 11     // remove_PointerExited(EventRegistrationToken)
    const val IInkUnprocessedInput_add_PointerPressed = 12       // add_PointerPressed(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerPressed = 13    // remove_PointerPressed(EventRegistrationToken)
    const val IInkUnprocessedInput_add_PointerMoved = 14         // add_PointerMoved(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerMoved = 15      // remove_PointerMoved(EventRegistrationToken)
    const val IInkUnprocessedInput_add_PointerReleased = 16      // add_PointerReleased(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerReleased = 17   // remove_PointerReleased(EventRegistrationToken)
    const val IInkUnprocessedInput_add_PointerLost = 18          // add_PointerLost(TypedEventHandler<InkUnprocessedInput, PointerEventArgs>, out EventRegistrationToken)
    const val IInkUnprocessedInput_remove_PointerLost = 19       // remove_PointerLost(EventRegistrationToken)
    const val IInkUnprocessedInput_get_InkPresenter = 20         // get_InkPresenter(out InkPresenter)

    // ---- Microsoft.UI.Xaml.Controls.InkSynchronizer ----
    // base: System.Object
    const val CLS_InkSynchronizer = "Microsoft.UI.Xaml.Controls.InkSynchronizer"
    const val IID_IInkSynchronizer = "c4c9e61d-a71b-55d3-81e7-9cb5627b0da8"
    const val IInkSynchronizer_BeginDry = 6   // BeginDry(out IVectorView<InkStroke>)
    const val IInkSynchronizer_EndDry = 7     // EndDry()

    // ---- Microsoft.UI.Xaml.Controls.InkStrokesCollectedEventArgs ----
    // base: System.Object
    const val CLS_InkStrokesCollectedEventArgs = "Microsoft.UI.Xaml.Controls.InkStrokesCollectedEventArgs"
    const val IID_IInkStrokesCollectedEventArgs = "f185279c-f5a7-5e6c-a8ca-eed96cec1010"
    const val IInkStrokesCollectedEventArgs_get_Strokes = 6   // get_Strokes(out IVectorView<InkStroke>)

    // ---- Microsoft.UI.Xaml.Controls.InkStrokesErasedEventArgs ----
    // base: System.Object
    const val CLS_InkStrokesErasedEventArgs = "Microsoft.UI.Xaml.Controls.InkStrokesErasedEventArgs"
    const val IID_IInkStrokesErasedEventArgs = "7b5fc466-da0d-57e0-8fce-f1a34d66c104"
    const val IInkStrokesErasedEventArgs_get_Strokes = 6   // get_Strokes(out IVectorView<InkStroke>)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbar ----
    // base: Microsoft.UI.Xaml.Controls.Control / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarStatics
    const val CLS_InkToolbar = "Microsoft.UI.Xaml.Controls.InkToolbar"
    const val IID_IInkToolbar = "7420c575-1e05-5af0-9e41-acad21bdecdb"
    const val IInkToolbar_get_InitialControls = 6                     // get_InitialControls(out InkToolbarInitialControls)
    const val IInkToolbar_put_InitialControls = 7                     // put_InitialControls(InkToolbarInitialControls)
    const val IInkToolbar_get_Children = 8                            // get_Children(out DependencyObjectCollection)
    const val IInkToolbar_get_ActiveTool = 9                          // get_ActiveTool(out InkToolbarToolButton)
    const val IInkToolbar_put_ActiveTool = 10                         // put_ActiveTool(InkToolbarToolButton)
    const val IInkToolbar_get_InkDrawingAttributes = 11               // get_InkDrawingAttributes(out InkDrawingAttributes)
    const val IInkToolbar_get_IsRulerButtonChecked = 12               // get_IsRulerButtonChecked(out boolean)
    const val IInkToolbar_put_IsRulerButtonChecked = 13               // put_IsRulerButtonChecked(boolean)
    const val IInkToolbar_get_TargetInkCanvas = 14                    // get_TargetInkCanvas(out InkCanvas)
    const val IInkToolbar_put_TargetInkCanvas = 15                    // put_TargetInkCanvas(InkCanvas)
    const val IInkToolbar_get_IsStencilButtonChecked = 16             // get_IsStencilButtonChecked(out boolean)
    const val IInkToolbar_put_IsStencilButtonChecked = 17             // put_IsStencilButtonChecked(boolean)
    const val IInkToolbar_get_ButtonFlyoutPlacement = 18              // get_ButtonFlyoutPlacement(out InkToolbarButtonFlyoutPlacement)
    const val IInkToolbar_put_ButtonFlyoutPlacement = 19              // put_ButtonFlyoutPlacement(InkToolbarButtonFlyoutPlacement)
    const val IInkToolbar_get_Orientation = 20                        // get_Orientation(out Orientation)
    const val IInkToolbar_put_Orientation = 21                        // put_Orientation(Orientation)
    const val IInkToolbar_get_TargetInkPresenter = 22                 // get_TargetInkPresenter(out InkPresenter)
    const val IInkToolbar_put_TargetInkPresenter = 23                 // put_TargetInkPresenter(InkPresenter)
    const val IInkToolbar_add_ActiveToolChanged = 24                  // add_ActiveToolChanged(TypedEventHandler<InkToolbar, object>, out EventRegistrationToken)
    const val IInkToolbar_remove_ActiveToolChanged = 25               // remove_ActiveToolChanged(EventRegistrationToken)
    const val IInkToolbar_add_InkDrawingAttributesChanged = 26        // add_InkDrawingAttributesChanged(TypedEventHandler<InkToolbar, object>, out EventRegistrationToken)
    const val IInkToolbar_remove_InkDrawingAttributesChanged = 27     // remove_InkDrawingAttributesChanged(EventRegistrationToken)
    const val IInkToolbar_add_EraseAllClicked = 28                    // add_EraseAllClicked(TypedEventHandler<InkToolbar, object>, out EventRegistrationToken)
    const val IInkToolbar_remove_EraseAllClicked = 29                 // remove_EraseAllClicked(EventRegistrationToken)
    const val IInkToolbar_add_IsStencilButtonCheckedChanged = 30      // add_IsStencilButtonCheckedChanged(TypedEventHandler<InkToolbar, InkToolbarIsStencilButtonCheckedChangedEventArgs>, out EventRegistrationToken)
    const val IInkToolbar_remove_IsStencilButtonCheckedChanged = 31   // remove_IsStencilButtonCheckedChanged(EventRegistrationToken)
    const val IInkToolbar_GetToolButton = 32                          // GetToolButton(InkToolbarTool, out InkToolbarToolButton)
    const val IInkToolbar_GetToggleButton = 33                        // GetToggleButton(InkToolbarToggle, out InkToolbarToggleButton)
    const val IInkToolbar_GetMenuButton = 34                          // GetMenuButton(InkToolbarMenuKind, out InkToolbarMenuButton)
    const val IID_IInkToolbarFactory = "af5298a5-ce4a-5108-bc03-da99ee76084e"
    const val IInkToolbarFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbar)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarIsStencilButtonCheckedChangedEventArgs ----
    // base: System.Object
    const val CLS_InkToolbarIsStencilButtonCheckedChangedEventArgs = "Microsoft.UI.Xaml.Controls.InkToolbarIsStencilButtonCheckedChangedEventArgs"
    const val IID_IInkToolbarIsStencilButtonCheckedChangedEventArgs = "ba030e39-52a4-5729-8d8f-bad8c7b91a39"
    const val IInkToolbarIsStencilButtonCheckedChangedEventArgs_get_StencilButton = 6   // get_StencilButton(out InkToolbarStencilButton)
    const val IInkToolbarIsStencilButtonCheckedChangedEventArgs_get_StencilKind = 7     // get_StencilKind(out InkToolbarStencilKind)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarCustomPen ----
    // base: Microsoft.UI.Xaml.DependencyObject / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarCustomPenFactory
    const val CLS_InkToolbarCustomPen = "Microsoft.UI.Xaml.Controls.InkToolbarCustomPen"
    const val IID_IInkToolbarCustomPen = "cf589fbe-0875-504a-b5c1-fa20abe152a6"
    const val IInkToolbarCustomPen_CreateInkDrawingAttributes = 6   // CreateInkDrawingAttributes(Brush, r8, out InkDrawingAttributes)
    const val IID_IInkToolbarCustomPenOverrides = "468745d2-a3d9-5db7-ae9c-7632546b017a"
    const val IInkToolbarCustomPenOverrides_CreateInkDrawingAttributesCore = 6   // CreateInkDrawingAttributesCore(Brush, r8, out InkDrawingAttributes)
    const val IID_IInkToolbarCustomPenFactory = "1584193f-e933-5c7e-b38b-3f3f9c694ef4"
    const val IInkToolbarCustomPenFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarCustomPen)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarPenConfigurationControl ----
    // base: Microsoft.UI.Xaml.Controls.Control / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarPenConfigurationControlFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarPenConfigurationControlStatics
    const val CLS_InkToolbarPenConfigurationControl = "Microsoft.UI.Xaml.Controls.InkToolbarPenConfigurationControl"
    const val IID_IInkToolbarPenConfigurationControl = "71d4f71d-2d68-5a7c-b27a-8d21dcd130c7"
    const val IInkToolbarPenConfigurationControl_get_PenButton = 6   // get_PenButton(out InkToolbarPenButton)
    const val IID_IInkToolbarPenConfigurationControlFactory = "74fa9efd-4cfb-5266-b865-538b7d851ff6"
    const val IInkToolbarPenConfigurationControlFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarPenConfigurationControl)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarFlyoutItem ----
    // base: Microsoft.UI.Xaml.Controls.Primitives.ButtonBase / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarFlyoutItemFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarFlyoutItemStatics
    const val CLS_InkToolbarFlyoutItem = "Microsoft.UI.Xaml.Controls.InkToolbarFlyoutItem"
    const val IID_IInkToolbarFlyoutItem = "df85304d-17b5-5fe6-8deb-981ed909f0c1"
    const val IInkToolbarFlyoutItem_get_Kind = 6            // get_Kind(out InkToolbarFlyoutItemKind)
    const val IInkToolbarFlyoutItem_put_Kind = 7            // put_Kind(InkToolbarFlyoutItemKind)
    const val IInkToolbarFlyoutItem_get_IsChecked = 8       // get_IsChecked(out boolean)
    const val IInkToolbarFlyoutItem_put_IsChecked = 9       // put_IsChecked(boolean)
    const val IInkToolbarFlyoutItem_add_Checked = 10        // add_Checked(TypedEventHandler<InkToolbarFlyoutItem, object>, out EventRegistrationToken)
    const val IInkToolbarFlyoutItem_remove_Checked = 11     // remove_Checked(EventRegistrationToken)
    const val IInkToolbarFlyoutItem_add_Unchecked = 12      // add_Unchecked(TypedEventHandler<InkToolbarFlyoutItem, object>, out EventRegistrationToken)
    const val IInkToolbarFlyoutItem_remove_Unchecked = 13   // remove_Unchecked(EventRegistrationToken)
    const val IID_IInkToolbarFlyoutItemFactory = "c07f2757-6b78-5ed5-a543-75d74e32b2d2"
    const val IInkToolbarFlyoutItemFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarFlyoutItem)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarToolButton ----
    // base: Microsoft.UI.Xaml.Controls.RadioButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarToolButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarToolButtonStatics
    const val CLS_InkToolbarToolButton = "Microsoft.UI.Xaml.Controls.InkToolbarToolButton"
    const val IID_IInkToolbarToolButton = "2085f266-5f65-586f-84d0-792d99185605"
    const val IInkToolbarToolButton_get_ToolKind = 6                // get_ToolKind(out InkToolbarTool)
    const val IInkToolbarToolButton_get_IsExtensionGlyphShown = 7   // get_IsExtensionGlyphShown(out boolean)
    const val IInkToolbarToolButton_put_IsExtensionGlyphShown = 8   // put_IsExtensionGlyphShown(boolean)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarPenButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarToolButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarPenButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarPenButtonStatics
    const val CLS_InkToolbarPenButton = "Microsoft.UI.Xaml.Controls.InkToolbarPenButton"
    const val IID_IInkToolbarPenButton = "d8c89e29-6e76-5ac0-a627-088ea840bc48"
    const val IInkToolbarPenButton_get_Palette = 6                // get_Palette(out IVector<Brush>)
    const val IInkToolbarPenButton_put_Palette = 7                // put_Palette(IVector<Brush>)
    const val IInkToolbarPenButton_get_MinStrokeWidth = 8         // get_MinStrokeWidth(out r8)
    const val IInkToolbarPenButton_put_MinStrokeWidth = 9         // put_MinStrokeWidth(r8)
    const val IInkToolbarPenButton_get_MaxStrokeWidth = 10        // get_MaxStrokeWidth(out r8)
    const val IInkToolbarPenButton_put_MaxStrokeWidth = 11        // put_MaxStrokeWidth(r8)
    const val IInkToolbarPenButton_get_SelectedBrush = 12         // get_SelectedBrush(out Brush)
    const val IInkToolbarPenButton_get_SelectedBrushIndex = 13    // get_SelectedBrushIndex(out i4)
    const val IInkToolbarPenButton_put_SelectedBrushIndex = 14    // put_SelectedBrushIndex(i4)
    const val IInkToolbarPenButton_get_SelectedStrokeWidth = 15   // get_SelectedStrokeWidth(out r8)
    const val IInkToolbarPenButton_put_SelectedStrokeWidth = 16   // put_SelectedStrokeWidth(r8)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarBallpointPenButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarPenButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarBallpointPenButtonFactory
    const val CLS_InkToolbarBallpointPenButton = "Microsoft.UI.Xaml.Controls.InkToolbarBallpointPenButton"
    const val IID_IInkToolbarBallpointPenButton = "6e60bf1e-419a-5f77-8f98-9a41dc8d9b57"
    const val IID_IInkToolbarBallpointPenButtonFactory = "2bd27cb0-3b57-5e39-8648-007ba7606105"
    const val IInkToolbarBallpointPenButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarBallpointPenButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarPencilButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarPenButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarPencilButtonFactory
    const val CLS_InkToolbarPencilButton = "Microsoft.UI.Xaml.Controls.InkToolbarPencilButton"
    const val IID_IInkToolbarPencilButton = "5db9fe28-1847-543d-a6ea-6b16e3015d88"
    const val IID_IInkToolbarPencilButtonFactory = "adaff6ec-56d9-5689-accf-b51dee6b8845"
    const val IInkToolbarPencilButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarPencilButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarHighlighterButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarPenButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarHighlighterButtonFactory
    const val CLS_InkToolbarHighlighterButton = "Microsoft.UI.Xaml.Controls.InkToolbarHighlighterButton"
    const val IID_IInkToolbarHighlighterButton = "2bcd1bf8-2031-5e5b-8a00-348d4a33d16f"
    const val IID_IInkToolbarHighlighterButtonFactory = "66916276-3718-5ce8-b588-555acff8c5ca"
    const val IInkToolbarHighlighterButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarHighlighterButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarCustomPenButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarPenButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarCustomPenButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarCustomPenButtonStatics
    const val CLS_InkToolbarCustomPenButton = "Microsoft.UI.Xaml.Controls.InkToolbarCustomPenButton"
    const val IID_IInkToolbarCustomPenButton = "4ccc35e9-cf7b-54df-8754-09994530a3ad"
    const val IInkToolbarCustomPenButton_get_CustomPen = 6              // get_CustomPen(out InkToolbarCustomPen)
    const val IInkToolbarCustomPenButton_put_CustomPen = 7              // put_CustomPen(InkToolbarCustomPen)
    const val IInkToolbarCustomPenButton_get_ConfigurationContent = 8   // get_ConfigurationContent(out UIElement)
    const val IInkToolbarCustomPenButton_put_ConfigurationContent = 9   // put_ConfigurationContent(UIElement)
    const val IID_IInkToolbarCustomPenButtonFactory = "9a7e0173-3349-5dc9-8b78-9af31bdd28ec"
    const val IInkToolbarCustomPenButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarCustomPenButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarEraserButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarToolButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarEraserButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarEraserButtonStatics
    const val CLS_InkToolbarEraserButton = "Microsoft.UI.Xaml.Controls.InkToolbarEraserButton"
    const val IID_IInkToolbarEraserButton = "d8756172-9476-5e46-b037-a8e42dc250fb"
    const val IInkToolbarEraserButton_get_IsClearAllVisible = 6   // get_IsClearAllVisible(out boolean)
    const val IInkToolbarEraserButton_put_IsClearAllVisible = 7   // put_IsClearAllVisible(boolean)
    const val IID_IInkToolbarEraserButtonFactory = "d2eb98df-3820-546a-a911-c8b003e028f5"
    const val IInkToolbarEraserButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarEraserButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarCustomToolButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarToolButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarCustomToolButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarCustomToolButtonStatics
    const val CLS_InkToolbarCustomToolButton = "Microsoft.UI.Xaml.Controls.InkToolbarCustomToolButton"
    const val IID_IInkToolbarCustomToolButton = "e6577e4c-c0fa-57ea-8d92-3240981f77ea"
    const val IInkToolbarCustomToolButton_get_ConfigurationContent = 6   // get_ConfigurationContent(out UIElement)
    const val IInkToolbarCustomToolButton_put_ConfigurationContent = 7   // put_ConfigurationContent(UIElement)
    const val IID_IInkToolbarCustomToolButtonFactory = "1853dbd7-2b03-5c86-80e5-a6416080190a"
    const val IInkToolbarCustomToolButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarCustomToolButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarToggleButton ----
    // base: Microsoft.UI.Xaml.Controls.CheckBox / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarToggleButtonFactory
    const val CLS_InkToolbarToggleButton = "Microsoft.UI.Xaml.Controls.InkToolbarToggleButton"
    const val IID_IInkToolbarToggleButton = "40f44be5-5838-500b-a704-8fc3a5dc21c4"
    const val IInkToolbarToggleButton_get_ToggleKind = 6   // get_ToggleKind(out InkToolbarToggle)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarRulerButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarToggleButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarRulerButtonFactory
    const val CLS_InkToolbarRulerButton = "Microsoft.UI.Xaml.Controls.InkToolbarRulerButton"
    const val IID_IInkToolbarRulerButton = "16d2fc0e-8d66-5a67-b9f4-a765d93b1b36"
    const val IID_IInkToolbarRulerButtonFactory = "458e26ed-8e56-5cea-82ed-62f6b5262cc4"
    const val IInkToolbarRulerButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarRulerButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarCustomToggleButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarToggleButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarCustomToggleButtonFactory
    const val CLS_InkToolbarCustomToggleButton = "Microsoft.UI.Xaml.Controls.InkToolbarCustomToggleButton"
    const val IID_IInkToolbarCustomToggleButton = "b9d79226-39e6-5116-8e81-620b089ec173"
    const val IID_IInkToolbarCustomToggleButtonFactory = "5b6520e7-e8f0-5ef8-8b23-9d4b8aae0eb2"
    const val IInkToolbarCustomToggleButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarCustomToggleButton)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarMenuButton ----
    // base: Microsoft.UI.Xaml.Controls.Primitives.ToggleButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarMenuButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarMenuButtonStatics
    const val CLS_InkToolbarMenuButton = "Microsoft.UI.Xaml.Controls.InkToolbarMenuButton"
    const val IID_IInkToolbarMenuButton = "5629abb2-f387-5887-af47-674e7e6d8d98"
    const val IInkToolbarMenuButton_get_MenuKind = 6                // get_MenuKind(out InkToolbarMenuKind)
    const val IInkToolbarMenuButton_get_IsExtensionGlyphShown = 7   // get_IsExtensionGlyphShown(out boolean)
    const val IInkToolbarMenuButton_put_IsExtensionGlyphShown = 8   // put_IsExtensionGlyphShown(boolean)

    // ---- Microsoft.UI.Xaml.Controls.InkToolbarStencilButton ----
    // base: Microsoft.UI.Xaml.Controls.InkToolbarMenuButton / composable factory: Microsoft.UI.Xaml.Controls.IInkToolbarStencilButtonFactory / statics: Microsoft.UI.Xaml.Controls.IInkToolbarStencilButtonStatics
    const val CLS_InkToolbarStencilButton = "Microsoft.UI.Xaml.Controls.InkToolbarStencilButton"
    const val IID_IInkToolbarStencilButton = "22fa8228-ba22-5369-a20c-1ca266180d84"
    const val IInkToolbarStencilButton_get_Ruler = 6                      // get_Ruler(out InkPresenterRuler)
    const val IInkToolbarStencilButton_get_Protractor = 7                 // get_Protractor(out InkPresenterProtractor)
    const val IInkToolbarStencilButton_get_SelectedStencil = 8            // get_SelectedStencil(out InkToolbarStencilKind)
    const val IInkToolbarStencilButton_put_SelectedStencil = 9            // put_SelectedStencil(InkToolbarStencilKind)
    const val IInkToolbarStencilButton_get_IsRulerItemVisible = 10        // get_IsRulerItemVisible(out boolean)
    const val IInkToolbarStencilButton_put_IsRulerItemVisible = 11        // put_IsRulerItemVisible(boolean)
    const val IInkToolbarStencilButton_get_IsProtractorItemVisible = 12   // get_IsProtractorItemVisible(out boolean)
    const val IInkToolbarStencilButton_put_IsProtractorItemVisible = 13   // put_IsProtractorItemVisible(boolean)
    const val IID_IInkToolbarStencilButtonFactory = "c9c2d613-0ee2-5dad-80b9-5501b2b8d583"
    const val IInkToolbarStencilButtonFactory_CreateInstance = 6   // CreateInstance(object, ref object, out InkToolbarStencilButton)

    // ---- Microsoft.UI.Xaml.Media.Brush (element type of IVector<Brush>; used to read and write the pen palette) ----
    const val IID_IBrush = "2de3cb83-1329-5679-88f8-c822bc5442cb"

    // ---- Concrete IIDs of generic types (XAML side) ----

    /** Concrete IID of IVector<Microsoft.UI.Xaml.DependencyObject> (InkToolbar.Children = DependencyObjectCollection). */
    val IID_IVector_DependencyObject: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_IVector_OPEN}};" +
                "rc(Microsoft.UI.Xaml.DependencyObject;{${XamlInterop.IID_IDependencyObject}}))",
        )
    }

    /** Handler for InkPresenter.StrokesCollected: TypedEventHandler<InkPresenter, InkStrokesCollectedEventArgs>. */
    val IID_InkPresenterStrokesCollectedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkPresenter;{$IID_IInkPresenter});" +
                "rc($CLS_InkStrokesCollectedEventArgs;{$IID_IInkStrokesCollectedEventArgs}))",
        )
    }

    /** Handler for InkPresenter.StrokesErased: TypedEventHandler<InkPresenter, InkStrokesErasedEventArgs>. */
    val IID_InkPresenterStrokesErasedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkPresenter;{$IID_IInkPresenter});" +
                "rc($CLS_InkStrokesErasedEventArgs;{$IID_IInkStrokesErasedEventArgs}))",
        )
    }

    /** Handler for each InkStrokeInput event: TypedEventHandler<InkStrokeInput, Windows.UI.Core.PointerEventArgs>. */
    val IID_InkStrokeInputPointerHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkStrokeInput;{$IID_IInkStrokeInput});" +
                "rc($CLS_PointerEventArgs;{$IID_IPointerEventArgs}))",
        )
    }

    /** Handler for each InkUnprocessedInput event: TypedEventHandler<InkUnprocessedInput, Windows.UI.Core.PointerEventArgs>. */
    val IID_InkUnprocessedInputPointerHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkUnprocessedInput;{$IID_IInkUnprocessedInput});" +
                "rc($CLS_PointerEventArgs;{$IID_IPointerEventArgs}))",
        )
    }

    /** Handler for InkToolbar.ActiveToolChanged / InkDrawingAttributesChanged / EraseAllClicked: TypedEventHandler<InkToolbar, Object>. */
    val IID_InkToolbarObjectHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkToolbar;{$IID_IInkToolbar});" +
                "cinterface(IInspectable))",
        )
    }

    /** Handler for InkToolbar.IsStencilButtonCheckedChanged. */
    val IID_InkToolbarIsStencilButtonCheckedChangedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkToolbar;{$IID_IInkToolbar});" +
                "rc($CLS_InkToolbarIsStencilButtonCheckedChangedEventArgs;" +
                "{$IID_IInkToolbarIsStencilButtonCheckedChangedEventArgs}))",
        )
    }

    /** Handler for InkToolbarFlyoutItem.Checked / Unchecked: TypedEventHandler<InkToolbarFlyoutItem, Object>. */
    val IID_InkToolbarFlyoutItemObjectHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_InkToolbarFlyoutItem;{$IID_IInkToolbarFlyoutItem});" +
                "cinterface(IInspectable))",
        )
    }

    // ==================================================================
    // OS side (UniversalApiContract.winmd / FoundationContract.winmd from the Windows SDK)
    // ==================================================================

    // ---- Windows.UI.Input.Inking.InkDrawingAttributes ----
    // base: System.Object / activatable factory: <default IActivationFactory> / statics: Windows.UI.Input.Inking.IInkDrawingAttributesStatics
    const val CLS_InkDrawingAttributes = "Windows.UI.Input.Inking.InkDrawingAttributes"
    const val IID_IInkDrawingAttributes = "97a2176c-6774-48ad-84f0-48f5a9be74f9"
    const val IInkDrawingAttributes_get_Color = 6             // get_Color(out Color)
    const val IInkDrawingAttributes_put_Color = 7             // put_Color(Color)
    const val IInkDrawingAttributes_get_PenTip = 8            // get_PenTip(out PenTipShape)
    const val IInkDrawingAttributes_put_PenTip = 9            // put_PenTip(PenTipShape)
    const val IInkDrawingAttributes_get_Size = 10             // get_Size(out Size)
    const val IInkDrawingAttributes_put_Size = 11             // put_Size(Size)
    const val IInkDrawingAttributes_get_IgnorePressure = 12   // get_IgnorePressure(out boolean)
    const val IInkDrawingAttributes_put_IgnorePressure = 13   // put_IgnorePressure(boolean)
    const val IInkDrawingAttributes_get_FitToCurve = 14       // get_FitToCurve(out boolean)
    const val IInkDrawingAttributes_put_FitToCurve = 15       // put_FitToCurve(boolean)
    const val IID_IInkDrawingAttributes2 = "7cab6508-8ec4-42fd-a5a5-e4b7d1d5316d"
    const val IInkDrawingAttributes2_get_PenTipTransform = 6     // get_PenTipTransform(out Matrix3x2)
    const val IInkDrawingAttributes2_put_PenTipTransform = 7     // put_PenTipTransform(Matrix3x2)
    const val IInkDrawingAttributes2_get_DrawAsHighlighter = 8   // get_DrawAsHighlighter(out boolean)
    const val IInkDrawingAttributes2_put_DrawAsHighlighter = 9   // put_DrawAsHighlighter(boolean)
    const val IID_IInkDrawingAttributes3 = "72020002-7d5b-4690-8af4-e664cbe2b74f"
    const val IInkDrawingAttributes3_get_Kind = 6               // get_Kind(out InkDrawingAttributesKind)
    const val IInkDrawingAttributes3_get_PencilProperties = 7   // get_PencilProperties(out InkDrawingAttributesPencilProperties)
    const val IID_IInkDrawingAttributes4 = "ef65dc25-9f19-456d-91a3-bc3a3d91c5fb"
    const val IInkDrawingAttributes4_get_IgnoreTilt = 6   // get_IgnoreTilt(out boolean)
    const val IInkDrawingAttributes4_put_IgnoreTilt = 7   // put_IgnoreTilt(boolean)
    const val IID_IInkDrawingAttributes5 = "d11aa0bb-0775-4852-ae64-41143a7ae6c9"
    const val IInkDrawingAttributes5_get_ModelerAttributes = 6   // get_ModelerAttributes(out InkModelerAttributes)
    const val IID_IInkDrawingAttributesPencilProperties = "4f2534cb-2d86-41bb-b0e8-e4c2a0253c52"
    const val IInkDrawingAttributesPencilProperties_get_Opacity = 6   // get_Opacity(out r8)
    const val IInkDrawingAttributesPencilProperties_put_Opacity = 7   // put_Opacity(r8)
    const val IID_IInkDrawingAttributesStatics = "f731e03f-1a65-4862-96cb-6e1665e17f6d"
    const val IInkDrawingAttributesStatics_CreateForPencil = 6   // CreateForPencil(out InkDrawingAttributes)

    // ---- Windows.UI.Input.Inking.InkModelerAttributes ----
    const val IID_IInkModelerAttributes = "bad31f27-0cd9-4bfd-b6f3-9e03ba8d7454"
    const val IInkModelerAttributes_get_PredictionTime = 6   // get_PredictionTime(out TimeSpan)
    const val IInkModelerAttributes_put_PredictionTime = 7   // put_PredictionTime(TimeSpan)
    const val IInkModelerAttributes_get_ScalingFactor = 8    // get_ScalingFactor(out r4)
    const val IInkModelerAttributes_put_ScalingFactor = 9    // put_ScalingFactor(r4)
    const val IID_IInkModelerAttributes2 = "86d1d09a-4ef8-5e25-b7bc-b65424f16bb3"
    const val IInkModelerAttributes2_get_UseVelocityBasedPressure = 6   // get_UseVelocityBasedPressure(out boolean)
    const val IInkModelerAttributes2_put_UseVelocityBasedPressure = 7   // put_UseVelocityBasedPressure(boolean)

    // ---- Windows.UI.Input.Inking.InkPoint ----
    // base: System.Object / activatable factory: Windows.UI.Input.Inking.IInkPointFactory / activatable factory: Windows.UI.Input.Inking.IInkPointFactory2
    const val CLS_InkPoint = "Windows.UI.Input.Inking.InkPoint"
    const val IID_IInkPoint = "9f87272b-858c-46a5-9b41-d195970459fd"
    const val IInkPoint_get_Position = 6   // get_Position(out Point)
    const val IInkPoint_get_Pressure = 7   // get_Pressure(out r4)
    const val IID_IInkPoint2 = "fba9c3f7-ae56-4d5c-bd2f-0ac45f5e4af9"
    const val IInkPoint2_get_TiltX = 6       // get_TiltX(out r4)
    const val IInkPoint2_get_TiltY = 7       // get_TiltY(out r4)
    const val IInkPoint2_get_Timestamp = 8   // get_Timestamp(out u8)
    const val IID_IInkPointFactory = "29e5d51c-c98f-405d-9f3b-e53e31068d4d"
    const val IInkPointFactory_CreateInkPoint = 6   // CreateInkPoint(Point, r4, out InkPoint)
    const val IID_IInkPointFactory2 = "e0145e85-daff-45f2-ad69-050d8256a209"
    const val IInkPointFactory2_CreateInkPointWithTiltAndTimestamp = 6   // CreateInkPointWithTiltAndTimestamp(Point, r4, r4, r4, u8, out InkPoint)

    // ---- Windows.UI.Input.Inking.InkStroke ----
    const val IID_IInkStroke = "15144d60-cce3-4fcf-9d52-11518ab6afd4"
    const val IInkStroke_get_DrawingAttributes = 6   // get_DrawingAttributes(out InkDrawingAttributes)
    const val IInkStroke_put_DrawingAttributes = 7   // put_DrawingAttributes(InkDrawingAttributes)
    const val IInkStroke_get_BoundingRect = 8        // get_BoundingRect(out Rect)
    const val IInkStroke_get_Selected = 9            // get_Selected(out boolean)
    const val IInkStroke_put_Selected = 10           // put_Selected(boolean)
    const val IInkStroke_get_Recognized = 11         // get_Recognized(out boolean)
    const val IInkStroke_GetRenderingSegments = 12   // GetRenderingSegments(out IVectorView<InkStrokeRenderingSegment>)
    const val IInkStroke_Clone = 13                  // Clone(out InkStroke)
    const val IID_IInkStroke2 = "5db9e4f4-bafa-4de1-89d3-201b1ed7d89b"
    const val IInkStroke2_get_PointTransform = 6   // get_PointTransform(out Matrix3x2)
    const val IInkStroke2_put_PointTransform = 7   // put_PointTransform(Matrix3x2)
    const val IInkStroke2_GetInkPoints = 8         // GetInkPoints(out IVectorView<InkPoint>)
    const val IID_IInkStroke3 = "4a807374-9499-411d-a1c4-68855d03d65f"
    const val IInkStroke3_get_Id = 6                  // get_Id(out u4)
    const val IInkStroke3_get_StrokeStartedTime = 7   // get_StrokeStartedTime(out IReference<DateTime>)
    const val IInkStroke3_put_StrokeStartedTime = 8   // put_StrokeStartedTime(IReference<DateTime>)
    const val IInkStroke3_get_StrokeDuration = 9      // get_StrokeDuration(out IReference<TimeSpan>)
    const val IInkStroke3_put_StrokeDuration = 10     // put_StrokeDuration(IReference<TimeSpan>)
    const val IID_IInkStroke4 = "cd5b62e5-b6e9-5b91-a577-1921d2348690"
    const val IInkStroke4_get_PointerId = 6   // get_PointerId(out u4)

    // ---- Windows.UI.Input.Inking.InkStrokeBuilder ----
    // base: System.Object / activatable factory: <default IActivationFactory>
    const val CLS_InkStrokeBuilder = "Windows.UI.Input.Inking.InkStrokeBuilder"
    const val IID_IInkStrokeBuilder = "82bbd1dc-1c63-41dc-9e07-4b4a70ced801"
    const val IInkStrokeBuilder_SetDefaultDrawingAttributes = 10   // SetDefaultDrawingAttributes(InkDrawingAttributes)
    const val IID_IInkStrokeBuilder3 = "b2c71fcd-5472-46b1-a81d-c37a3d169441"
    const val IInkStrokeBuilder3_CreateStrokeFromInkPoints = 6   // CreateStrokeFromInkPoints(IIterable<InkPoint>, Matrix3x2, IReference<DateTime>, IReference<TimeSpan>, out InkStroke)

    // ---- Windows.UI.Input.Inking.InkStrokeRenderingSegment ----
    const val IID_IInkStrokeRenderingSegment = "68510f1f-88e3-477a-a2fa-569f5f1f9bd5"
    const val IInkStrokeRenderingSegment_get_Position = 6              // get_Position(out Point)
    const val IInkStrokeRenderingSegment_get_BezierControlPoint1 = 7   // get_BezierControlPoint1(out Point)
    const val IInkStrokeRenderingSegment_get_BezierControlPoint2 = 8   // get_BezierControlPoint2(out Point)
    const val IInkStrokeRenderingSegment_get_Pressure = 9              // get_Pressure(out r4)
    const val IInkStrokeRenderingSegment_get_TiltX = 10                // get_TiltX(out r4)
    const val IInkStrokeRenderingSegment_get_TiltY = 11                // get_TiltY(out r4)
    const val IInkStrokeRenderingSegment_get_Twist = 12                // get_Twist(out r4)

    // ---- Windows.UI.Input.Inking.InkPresenterRuler / InkPresenterProtractor (share IInkPresenterStencil) ----
    const val IID_IInkPresenterStencil = "30d12d6d-3e06-4d02-b116-277fb5d8addc"
    const val IInkPresenterStencil_get_Kind = 6               // get_Kind(out InkPresenterStencilKind)
    const val IInkPresenterStencil_get_IsVisible = 7          // get_IsVisible(out boolean)
    const val IInkPresenterStencil_put_IsVisible = 8          // put_IsVisible(boolean)
    const val IInkPresenterStencil_get_BackgroundColor = 9    // get_BackgroundColor(out Color)
    const val IInkPresenterStencil_put_BackgroundColor = 10   // put_BackgroundColor(Color)
    const val IInkPresenterStencil_get_ForegroundColor = 11   // get_ForegroundColor(out Color)
    const val IInkPresenterStencil_put_ForegroundColor = 12   // put_ForegroundColor(Color)
    const val IInkPresenterStencil_get_Transform = 13         // get_Transform(out Matrix3x2)
    const val IInkPresenterStencil_put_Transform = 14         // put_Transform(Matrix3x2)
    const val IID_IInkPresenterRuler = "6cda7d5a-dec7-4dd7-877a-2133f183d48a"
    const val IInkPresenterRuler_get_Length = 6   // get_Length(out r8)
    const val IInkPresenterRuler_put_Length = 7   // put_Length(r8)
    const val IInkPresenterRuler_get_Width = 8    // get_Width(out r8)
    const val IInkPresenterRuler_put_Width = 9    // put_Width(r8)
    const val IID_IInkPresenterRuler2 = "45130dc1-bc61-44d4-a423-54712ae671c4"
    const val IInkPresenterRuler2_get_AreTickMarksVisible = 6   // get_AreTickMarksVisible(out boolean)
    const val IInkPresenterRuler2_put_AreTickMarksVisible = 7   // put_AreTickMarksVisible(boolean)
    const val IInkPresenterRuler2_get_IsCompassVisible = 8      // get_IsCompassVisible(out boolean)
    const val IInkPresenterRuler2_put_IsCompassVisible = 9      // put_IsCompassVisible(boolean)
    const val IID_IInkPresenterProtractor = "7de3f2aa-ef6c-4e91-a73b-5b70d56fbd17"
    const val IInkPresenterProtractor_get_AreTickMarksVisible = 6      // get_AreTickMarksVisible(out boolean)
    const val IInkPresenterProtractor_put_AreTickMarksVisible = 7      // put_AreTickMarksVisible(boolean)
    const val IInkPresenterProtractor_get_AreRaysVisible = 8           // get_AreRaysVisible(out boolean)
    const val IInkPresenterProtractor_put_AreRaysVisible = 9           // put_AreRaysVisible(boolean)
    const val IInkPresenterProtractor_get_IsCenterMarkerVisible = 10   // get_IsCenterMarkerVisible(out boolean)
    const val IInkPresenterProtractor_put_IsCenterMarkerVisible = 11   // put_IsCenterMarkerVisible(boolean)
    const val IInkPresenterProtractor_get_IsAngleReadoutVisible = 12   // get_IsAngleReadoutVisible(out boolean)
    const val IInkPresenterProtractor_put_IsAngleReadoutVisible = 13   // put_IsAngleReadoutVisible(boolean)
    const val IInkPresenterProtractor_get_IsResizable = 14             // get_IsResizable(out boolean)
    const val IInkPresenterProtractor_put_IsResizable = 15             // put_IsResizable(boolean)
    const val IInkPresenterProtractor_get_Radius = 16                  // get_Radius(out r8)
    const val IInkPresenterProtractor_put_Radius = 17                  // put_Radius(r8)
    const val IInkPresenterProtractor_get_AccentColor = 18             // get_AccentColor(out Color)
    const val IInkPresenterProtractor_put_AccentColor = 19             // put_AccentColor(Color)

    // ---- Windows.UI.Core.PointerEventArgs (event args of InkStrokeInput / InkUnprocessedInput) ----
    const val IID_IPointerEventArgs = "920d9cb1-a5fc-4a21-8c09-49dfe6ffe25f"
    const val IPointerEventArgs_get_CurrentPoint = 6        // get_CurrentPoint(out PointerPoint)
    const val IPointerEventArgs_get_KeyModifiers = 7        // get_KeyModifiers(out VirtualKeyModifiers)
    const val IPointerEventArgs_GetIntermediatePoints = 8   // GetIntermediatePoints(out IVector<PointerPoint>)
    const val IID_ICoreWindowEventArgs = "272b1ef3-c633-4da5-a26c-c6d0f56b29da"
    const val ICoreWindowEventArgs_get_Handled = 6   // get_Handled(out boolean)
    const val ICoreWindowEventArgs_put_Handled = 7   // put_Handled(boolean)

    // ---- Windows.UI.Input.PointerPoint (OS side; a different type from WindowingInterop's Microsoft.UI.Input.PointerPoint) ----
    const val IID_IPointerPoint = "e995317d-7296-42d9-8233-c5be73b74a4a"
    const val IPointerPoint_get_PointerDevice = 6   // get_PointerDevice(out PointerDevice)
    const val IPointerPoint_get_Position = 7        // get_Position(out Point)
    const val IPointerPoint_get_RawPosition = 8     // get_RawPosition(out Point)
    const val IPointerPoint_get_PointerId = 9       // get_PointerId(out u4)
    const val IPointerPoint_get_FrameId = 10        // get_FrameId(out u4)
    const val IPointerPoint_get_Timestamp = 11      // get_Timestamp(out u8)
    const val IPointerPoint_get_IsInContact = 12    // get_IsInContact(out boolean)
    const val IPointerPoint_get_Properties = 13     // get_Properties(out PointerPointProperties)
    const val IID_IPointerPointProperties = "c79d8a4b-c163-4ee7-803f-67ce79f9972d"
    const val IPointerPointProperties_get_Pressure = 6                  // get_Pressure(out r4)
    const val IPointerPointProperties_get_IsInverted = 7                // get_IsInverted(out boolean)
    const val IPointerPointProperties_get_IsEraser = 8                  // get_IsEraser(out boolean)
    const val IPointerPointProperties_get_Orientation = 9               // get_Orientation(out r4)
    const val IPointerPointProperties_get_XTilt = 10                    // get_XTilt(out r4)
    const val IPointerPointProperties_get_YTilt = 11                    // get_YTilt(out r4)
    const val IPointerPointProperties_get_Twist = 12                    // get_Twist(out r4)
    const val IPointerPointProperties_get_ContactRect = 13              // get_ContactRect(out Rect)
    const val IPointerPointProperties_get_ContactRectRaw = 14           // get_ContactRectRaw(out Rect)
    const val IPointerPointProperties_get_TouchConfidence = 15          // get_TouchConfidence(out boolean)
    const val IPointerPointProperties_get_IsLeftButtonPressed = 16      // get_IsLeftButtonPressed(out boolean)
    const val IPointerPointProperties_get_IsRightButtonPressed = 17     // get_IsRightButtonPressed(out boolean)
    const val IPointerPointProperties_get_IsMiddleButtonPressed = 18    // get_IsMiddleButtonPressed(out boolean)
    const val IPointerPointProperties_get_MouseWheelDelta = 19          // get_MouseWheelDelta(out i4)
    const val IPointerPointProperties_get_IsHorizontalMouseWheel = 20   // get_IsHorizontalMouseWheel(out boolean)
    const val IPointerPointProperties_get_IsPrimary = 21                // get_IsPrimary(out boolean)
    const val IPointerPointProperties_get_IsInRange = 22                // get_IsInRange(out boolean)
    const val IPointerPointProperties_get_IsCanceled = 23               // get_IsCanceled(out boolean)
    const val IPointerPointProperties_get_IsBarrelButtonPressed = 24    // get_IsBarrelButtonPressed(out boolean)
    const val IPointerPointProperties_get_IsXButton1Pressed = 25        // get_IsXButton1Pressed(out boolean)
    const val IPointerPointProperties_get_IsXButton2Pressed = 26        // get_IsXButton2Pressed(out boolean)
    const val IPointerPointProperties_get_PointerUpdateKind = 27        // get_PointerUpdateKind(out PointerUpdateKind)
    const val IPointerPointProperties_HasUsage = 28                     // HasUsage(u4, u4, out boolean)
    const val IPointerPointProperties_GetUsageValue = 29                // GetUsageValue(u4, u4, out i4)

    // ---- Windows.Devices.Input.PointerDevice ----
    const val IID_IPointerDevice = "93c9bafc-ebcb-467e-82c6-276feae36b5a"
    const val IPointerDevice_get_PointerDeviceType = 6   // get_PointerDeviceType(out PointerDeviceType)

    // ---- Windows.Storage.Streams.InMemoryRandomAccessStream (passes data when saving and loading ISF) ----
    // base: System.Object / activatable factory: <default IActivationFactory>
    const val CLS_InMemoryRandomAccessStream = "Windows.Storage.Streams.InMemoryRandomAccessStream"
    const val IID_IRandomAccessStream = "905a0fe1-bc53-11df-8c49-001e4fc686da"
    const val IRandomAccessStream_get_Size = 6            // get_Size(out u8)
    const val IRandomAccessStream_GetInputStreamAt = 8    // GetInputStreamAt(u8, out IInputStream)
    const val IRandomAccessStream_GetOutputStreamAt = 9   // GetOutputStreamAt(u8, out IOutputStream)
    const val IID_IOutputStream = "905a0fe6-bc53-11df-8c49-001e4fc686da"
    const val IID_IInputStream = "905a0fe2-bc53-11df-8c49-001e4fc686da"

    // ---- Windows.Storage.Streams.DataReader ----
    // base: System.Object / activatable factory: Windows.Storage.Streams.IDataReaderFactory / statics: Windows.Storage.Streams.IDataReaderStatics
    const val CLS_DataReader = "Windows.Storage.Streams.DataReader"
    const val IID_IDataReader = "e2b50029-b4c1-4314-a4b8-fb813a2f275e"
    const val IDataReader_ReadBytes = 14   // ReadBytes(u1[])
    const val IDataReader_LoadAsync = 29   // LoadAsync(u4, out DataReaderLoadOperation)
    const val IID_IDataReaderFactory = "d7527847-57da-4e15-914c-06806699a098"
    const val IDataReaderFactory_CreateDataReader = 6   // CreateDataReader(IInputStream, out DataReader)

    // ---- Windows.Storage.Streams.DataWriter ----
    // base: System.Object / activatable factory: Windows.Storage.Streams.IDataWriterFactory / activatable factory: <default IActivationFactory>
    const val CLS_DataWriter = "Windows.Storage.Streams.DataWriter"
    const val IID_IDataWriter = "64b89265-d341-4922-b38a-dd4af8808c4e"
    const val IDataWriter_WriteBytes = 12     // WriteBytes(u1[])
    const val IDataWriter_StoreAsync = 29     // StoreAsync(out DataWriterStoreOperation)
    const val IDataWriter_DetachStream = 32   // DetachStream(out IOutputStream)
    const val IID_IDataWriterFactory = "338c67c2-8b84-4c2b-9c50-7b8767847a1f"
    const val IDataWriterFactory_CreateDataWriter = 6   // CreateDataWriter(IOutputStream, out DataWriter)

    // ---- Windows.Foundation.IClosable ----
    const val IID_IClosable = "30d5a829-7fa4-4026-83bb-d75bae4ea99e"
    const val IClosable_Close = 6   // Close()

    /** Runtime class name of PointerEventArgs (used in the signature of the handlers' concrete IIDs). */
    const val CLS_PointerEventArgs = "Windows.UI.Core.PointerEventArgs"

    /** Runtime class names of InkStroke / InkPoint (used in the signature of the concrete IIDs of IIterable<T>). */
    const val CLS_InkStroke = "Windows.UI.Input.Inking.InkStroke"

    // ---- Concrete IIDs of generic types (OS side) ----

    /** Concrete IID of IIterable<InkPoint> (argument of InkStrokeBuilder.CreateStrokeFromInkPoints). */
    val IID_IIterable_InkPoint: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterable_OPEN}};rc($CLS_InkPoint;{$IID_IInkPoint}))")
    }

    /** Concrete IID of IIterator<InkPoint>. */
    val IID_IIterator_InkPoint: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterator_OPEN}};rc($CLS_InkPoint;{$IID_IInkPoint}))")
    }

    /** Concrete IID of IIterable<InkStroke> (argument of InkStrokeContainer.AddStrokes). */
    val IID_IIterable_InkStroke: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterable_OPEN}};rc($CLS_InkStroke;{$IID_IInkStroke}))")
    }

    /** Concrete IID of IIterator<InkStroke>. */
    val IID_IIterator_InkStroke: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterator_OPEN}};rc($CLS_InkStroke;{$IID_IInkStroke}))")
    }

    /** Concrete IID of IIterable<Windows.Foundation.Point> (argument of InkStrokeContainer.SelectWithPolyLine). */
    val IID_IIterable_Point: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterable_OPEN}};struct(Windows.Foundation.Point;f4;f4))")
    }

    /** Concrete IID of IIterator<Windows.Foundation.Point>. */
    val IID_IIterator_Point: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IIterator_OPEN}};struct(Windows.Foundation.Point;f4;f4))")
    }

    /** Concrete IID of AsyncOperationCompletedHandler<UInt32> (waits for DataReader.LoadAsync / DataWriter.StoreAsync to complete). */
    val IID_AsyncOperationCompletedHandler_UInt32: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};u4)")
    }
}
