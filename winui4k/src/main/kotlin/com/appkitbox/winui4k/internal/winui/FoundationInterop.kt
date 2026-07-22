package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for Windows.Foundation (OS-side,
 * Windows.Foundation.FoundationContract.winmd from the Windows SDK).
 * Covers the IVector / IMap / IIterable collections, IReference<T> box/unbox, the base IIDs
 * of generic types like TypedEventHandler / EventHandler, and their parameterized concrete
 * IIDs (computed at runtime via SHA-1).
 *
 * Values are mechanically extracted with tools/dump_winmd.py. Not a single value is
 * handwritten or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, the interface body starts
 * at 6 and follows the winmd's method declaration order.
 */
internal object FoundationInterop {
    // ---- Windows.Foundation.Uri (OS-side, FoundationContract.winmd) ----
    const val CLS_Uri = "Windows.Foundation.Uri"
    const val IID_IUriRuntimeClassFactory = "44a9796f-723e-4fdf-a218-033e75b0c084"
    const val IUriRuntimeClassFactory_CreateUri = 6    // CreateUri(HSTRING, out Uri)
    const val IUriRuntimeClass_get_AbsoluteUri = 6     // get_AbsoluteUri(out HSTRING)

    /** Base IID of Windows.Foundation.TypedEventHandler`2. */
    internal const val IID_TypedEventHandler_OPEN = "9de1c534-6ae1-11e0-84e1-18a905bcc53f"

    /** Base IID of Windows.Foundation.EventHandler`1. */
    internal const val IID_EventHandler_OPEN = "9de1c535-6ae1-11e0-84e1-18a905bcc53f"

    /** Concrete IID of EventHandler<Object> (computed at runtime). Used by Popup.Opened / Closed. */
    val IID_EventHandler_Object: String by lazy {
        Pinterface.iid(
            "pinterface({$IID_EventHandler_OPEN};cinterface(IInspectable))",
        )
    }

    /** Base IID of Windows.Foundation.IReference`1 (from FoundationContract.winmd). */
    private const val IID_IReference_OPEN = "61c17706-2d65-11e0-9ae8-d48564015472"

    /**
     * Concrete IID of IReference<Boolean> (computed at runtime). boolean's signature is b1.
     * Used to box/unbox ToggleButton.IsChecked (null = indeterminate).
     */
    val IID_IReference_Boolean: String by lazy {
        Pinterface.iid("pinterface({$IID_IReference_OPEN};b1)")
    }

    /** Concrete IID of IReference<DateTime>. DateTime's signature is struct(Windows.Foundation.DateTime;i8). */
    val IID_IReference_DateTime: String by lazy {
        Pinterface.iid("pinterface({$IID_IReference_OPEN};struct(Windows.Foundation.DateTime;i8))")
    }

    /** Concrete IID of IReference<TimeSpan>. TimeSpan's signature is struct(Windows.Foundation.TimeSpan;i8). */
    val IID_IReference_TimeSpan: String by lazy {
        Pinterface.iid("pinterface({$IID_IReference_OPEN};struct(Windows.Foundation.TimeSpan;i8))")
    }

    // ---- Windows.Foundation.Collections.IVector<T> (OS-side, FoundationContract.winmd) ----
    // GetAt=6 get_Size=7 GetView=8 IndexOf=9 SetAt=10 InsertAt=11 RemoveAt=12 Append=13
    // RemoveAtEnd=14 Clear=15 GetMany=16 ReplaceAll=17
    const val IVector_GetAt = 6                        // GetAt(UINT32, out T)
    const val IVector_get_Size = 7                     // get_Size(out UINT32)
    const val IVector_IndexOf = 9                      // IndexOf(T, out UINT32 index, out boolean found)
    const val IVector_RemoveAt = 12                    // RemoveAt(UINT32)
    const val IVector_Append = 13                      // Append(T)
    const val IVector_Clear = 15                       // Clear()
    internal const val IID_IVector_OPEN = "913337e9-11a1-4345-a3a2-4e7f956e222d" // Base IID of IVector`1

    /**
     * Concrete IID of IVector<Microsoft.UI.Xaml.UIElement>.
     * Computed at runtime via SHA-1 from the WinRT-spec signature (= ea4a1af0-4286-5f11-8142-6b0169f4e9de).
     */
    val IID_IVector_UIElement: String by lazy {
        Pinterface.iid(
            "pinterface({$IID_IVector_OPEN};rc(Microsoft.UI.Xaml.UIElement;{${XamlInterop.IID_IUIElement}}))",
        )
    }

    /** Concrete IID of IVector<Microsoft.UI.Xaml.Media.GradientStop> (GradientStopCollection's default interface). */
    val IID_IVector_GradientStop: String by lazy {
        Pinterface.iid(
            "pinterface({$IID_IVector_OPEN};rc(Microsoft.UI.Xaml.Media.GradientStop;{${XamlInterop.IID_IGradientStop}}))",
        )
    }

    /** Concrete IID of IVector<Microsoft.UI.Xaml.ResourceDictionary> (MergedDictionaries). */
    val IID_IVector_ResourceDictionary: String by lazy {
        Pinterface.iid(
            "pinterface({$IID_IVector_OPEN};" +
                "rc(Microsoft.UI.Xaml.ResourceDictionary;{${XamlInterop.IID_IResourceDictionary}}))",
        )
    }

    /** Concrete IID of IVector<Object> (ItemsControl.Items). Object's signature is cinterface(IInspectable). */
    val IID_IVector_Object: String by lazy {
        Pinterface.iid("pinterface({$IID_IVector_OPEN};cinterface(IInspectable))")
    }

    /** Concrete IID of IVector<Microsoft.UI.Xaml.Documents.Block> (RichTextBlock.Blocks). */
    val IID_IVector_Block: String by lazy {
        Pinterface.iid(
            "pinterface({$IID_IVector_OPEN};rc(Microsoft.UI.Xaml.Documents.Block;{${XamlInterop.IID_IBlock}}))",
        )
    }

    /** Concrete IID of IVector<Microsoft.UI.Xaml.Documents.Inline> (Paragraph.Inlines / Span.Inlines). */
    val IID_IVector_Inline: String by lazy {
        Pinterface.iid(
            "pinterface({$IID_IVector_OPEN};rc(Microsoft.UI.Xaml.Documents.Inline;{${XamlInterop.IID_IInline}}))",
        )
    }

    // ---- Windows.Foundation.Collections.IMap<K, V> (OS-side, FoundationContract.winmd) ----
    // Lookup=6 get_Size=7 HasKey=8 GetView=9 Insert=10 Remove=11 Clear=12
    const val IMap_Lookup = 6                          // Lookup(K, out V)
    const val IMap_Insert = 10                         // Insert(K, V, out boolean replaced)
    private const val IID_IMap_OPEN = "3c2925fe-8519-45c1-aa79-197b6718c1c1" // Base IID of IMap`2

    /** Concrete IID of IMap<Object, Object> (the key->resource dictionary ResourceDictionary implements). */
    val IID_IMap_Object_Object: String by lazy {
        Pinterface.iid("pinterface({$IID_IMap_OPEN};cinterface(IInspectable);cinterface(IInspectable))")
    }

    // ---- Windows.Foundation.Collections.IIterable<T> / IIterator<T> (OS-side, FoundationContract.winmd) ----
    // IIterable: First=6. IIterator: get_Current=6 get_HasCurrent=7 MoveNext=8 GetMany=9
    private const val IID_IIterable_OPEN = "faa585ea-6214-4217-afda-7f46de5869b3" // Base IID of IIterable`1
    private const val IID_IIterator_OPEN = "6a79e863-4300-459a-9966-cbb660963ee1" // Base IID of IIterator`1

    /** Concrete IID of IIterable<Object> (passed to ItemsControl.ItemsSource). */
    val IID_IIterable_Object: String by lazy {
        Pinterface.iid("pinterface({$IID_IIterable_OPEN};cinterface(IInspectable))")
    }

    /** Concrete IID of IIterator<Object>. */
    val IID_IIterator_Object: String by lazy {
        Pinterface.iid("pinterface({$IID_IIterator_OPEN};cinterface(IInspectable))")
    }

    // ---- Windows.Foundation async ----
    // (constants shared by IAsyncAction / IAsyncOperation<T> are held privately by winrt.Async)
    internal const val IID_AsyncOperationCompletedHandler_OPEN = "fcdcf02c-e5d8-4478-915a-4d90b74b83a5"

    // ---- Windows.Foundation.IReference<Int32> (box/unbox via PropertyValue.CreateInt32/GetInt32) ----
    /** Used by OverlappedPresenter's PreferredMinimum/MaximumWidth/Height (null = unset). */
    val IID_IReference_Int32: String by lazy {
        Pinterface.iid("pinterface({$IID_IReference_OPEN};i4)")
    }

    // ---- Windows.Foundation.IReference<Double> (box via PropertyValue.CreateDouble) ----
    /** Used by ScrollViewer.ChangeView's offset argument (null = no change). */
    val IID_IReference_Double: String by lazy {
        Pinterface.iid("pinterface({$IID_IReference_OPEN};f8)")
    }

    // ---- Windows.Foundation.IReference<Windows.UI.Color> (PropertyValue has no CreateColor, so implemented ourselves) ----
    /** Used by AppWindowTitleBar's 12 color properties. Implementation is internal.winui.ColorReference. */
    val IID_IReference_Color: String by lazy {
        Pinterface.iid("pinterface({$IID_IReference_OPEN};struct(Windows.UI.Color;u1;u1;u1;u1))")
    }

    /** IReference<T> (both Color and Int32): get_Value is always vtbl[6]. */
    const val IReference_Color_get_Value = 6
}
