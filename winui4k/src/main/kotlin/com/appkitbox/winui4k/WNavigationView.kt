package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.NavigationViewDisplayMode (the pane's current display state).
 * Values extracted from the winmd (Minimal=0, Compact=1, Expanded=2).
 */
enum class NavigationViewDisplayMode(internal val native: Int) {
    /** Hides the pane, showing only the hamburger button. */
    MINIMAL(0),

    /** Shows a narrow, icon-only pane. */
    COMPACT(1),

    /** Shows a wide pane with labels. */
    EXPANDED(2),
    ;

    internal companion object {
        fun of(native: Int): NavigationViewDisplayMode = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.NavigationViewPaneDisplayMode (how the pane is placed).
 * Values extracted from the winmd (Auto=0, Left=1, Top=2, LeftCompact=3, LeftMinimal=4).
 */
enum class NavigationViewPaneDisplayMode(internal val native: Int) {
    /** Switches automatically based on the window's width (default). */
    AUTO(0),

    /** Always shows a wide pane on the left. */
    LEFT(1),

    /** Shows items side-by-side along the top. */
    TOP(2),

    /** Shows a narrow, icon-only pane on the left. */
    LEFT_COMPACT(3),

    /** Hides the left pane, showing only the hamburger button. */
    LEFT_MINIMAL(4),
    ;

    internal companion object {
        fun of(native: Int): NavigationViewPaneDisplayMode = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.NavigationViewBackButtonVisible (whether the back button is shown).
 * Values extracted from the winmd (Collapsed=0, Visible=1, Auto=2).
 */
enum class NavigationViewBackButtonVisible(internal val native: Int) {
    /** Never show it. */
    COLLAPSED(0),

    /** Always show it. */
    VISIBLE(1),

    /** Switches based on the system setting (default). */
    AUTO(2),
    ;

    internal companion object {
        fun of(native: Int): NavigationViewBackButtonVisible = entries.first { it.native == native }
    }
}

/**
 * JRootPane-like with top-level navigation built in: WinUI 3's NavigationView.
 *
 * Shows the menu of [WNavigationViewItem]s added via [addItem] in the left (or top)
 * pane, providing an app skeleton that swaps [content] based on the selection.
 * Subscribe to selection changes with [addSelectionListener], and item clicks with
 * [addItemInvokedListener].
 */
class WNavigationView : WControl(
    Activation.composeDefault(XamlInterop.CLS_NavigationView, XamlInterop.IID_INavigationViewFactory), // default interface = INavigationView
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }
    private val navigationView2: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_INavigationView2))
    }

    /** The IVector<Object> view of NavigationView.MenuItems / FooterMenuItems. */
    private val menuItemVector: ComPtr by lazy {
        val menuItems = own(inspectable.getPtr(XamlInterop.INavigationView_get_MenuItems))
        own(menuItems.queryInterface(FoundationInterop.IID_IVector_Object))
    }
    private val footerItemVector: ComPtr by lazy {
        val footerItems = own(inspectable.getPtr(XamlInterop.INavigationView_get_FooterMenuItems))
        own(footerItems.queryInterface(FoundationInterop.IID_IVector_Object))
    }

    /** Items added via [addItem] / [addFooterItem] (used to resolve the selected item back). */
    private val items = mutableListOf<WNavigationViewItem>()

    /** Listener -> event token (used to remove). */
    private val selectionTokens = ListenerTokens<(WNavigationViewItem?) -> Unit>()
    private val itemInvokedTokens = ListenerTokens<(String) -> Unit>()

    /** Whether the pane is open (NavigationView.IsPaneOpen). */
    var isPaneOpen: Boolean
        get() = inspectable.getBool(XamlInterop.INavigationView_get_IsPaneOpen)
        set(value) = inspectable.putBool(XamlInterop.INavigationView_put_IsPaneOpen, value)

    /** The heading above the content (NavigationView.Header). Object-typed, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.INavigationView_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The pane's current display state (NavigationView.DisplayMode). Read-only; changes automatically based on width. */
    val displayMode: NavigationViewDisplayMode
        get() = NavigationViewDisplayMode.of(inspectable.getInt(XamlInterop.INavigationView_get_DisplayMode))

    /** Whether to show the settings item at the bottom of the pane (NavigationView.IsSettingsVisible). */
    var isSettingsVisible: Boolean
        get() = inspectable.getBool(XamlInterop.INavigationView_get_IsSettingsVisible)
        set(value) = inspectable.putBool(XamlInterop.INavigationView_put_IsSettingsVisible, value)

    /** Whether to show the hamburger button for opening/closing the pane (NavigationView.IsPaneToggleButtonVisible). */
    var isPaneToggleButtonVisible: Boolean
        get() = inspectable.getBool(XamlInterop.INavigationView_get_IsPaneToggleButtonVisible)
        set(value) = inspectable.putBool(XamlInterop.INavigationView_put_IsPaneToggleButtonVisible, value)

    /** The pane's width when closed (NavigationView.CompactPaneLength). */
    var compactPaneLength: Double
        get() = inspectable.getDouble(XamlInterop.INavigationView_get_CompactPaneLength)
        set(value) = inspectable.call(XamlInterop.INavigationView_put_CompactPaneLength, value)

    /** The pane's width when open (NavigationView.OpenPaneLength). */
    var openPaneLength: Double
        get() = inspectable.getDouble(XamlInterop.INavigationView_get_OpenPaneLength)
        set(value) = inspectable.call(XamlInterop.INavigationView_put_OpenPaneLength, value)

    /**
     * The currently selected item, or null if none is selected (NavigationView.SelectedItem).
     * The getter resolves the item back by COM identity from the items added via
     * [addItem] / [addFooterItem] (including descendants).
     */
    var selectedItem: WNavigationViewItem?
        get() {
            val selected = inspectable.getPtrOrNull(XamlInterop.INavigationView_get_SelectedItem) ?: return null
            return try {
                resolveItem(selected)
            } finally {
                selected.release()
            }
        }
        set(value) {
            inspectable.call(
                XamlInterop.INavigationView_put_SelectedItem,
                value?.inspectable?.ptr,
            )
        }

    /** The content area that shows the selected item (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    /** The title string at the top of the pane (NavigationView.PaneTitle). */
    var paneTitle: String
        get() = navigationView2.getString(XamlInterop.INavigationView2_get_PaneTitle)
        set(value) = Hstring.use(value) { h -> navigationView2.call(XamlInterop.INavigationView2_put_PaneTitle, h) }

    /** How the pane is placed (NavigationView.PaneDisplayMode). */
    var paneDisplayMode: NavigationViewPaneDisplayMode
        get() = NavigationViewPaneDisplayMode.of(navigationView2.getInt(XamlInterop.INavigationView2_get_PaneDisplayMode))
        set(value) = navigationView2.call(XamlInterop.INavigationView2_put_PaneDisplayMode, value.native)

    /** Whether the back button is shown (NavigationView.IsBackButtonVisible). */
    var isBackButtonVisible: NavigationViewBackButtonVisible
        get() = NavigationViewBackButtonVisible.of(navigationView2.getInt(XamlInterop.INavigationView2_get_IsBackButtonVisible))
        set(value) = navigationView2.call(XamlInterop.INavigationView2_put_IsBackButtonVisible, value.native)

    /** Whether the back button is pressable (NavigationView.IsBackEnabled). */
    var isBackEnabled: Boolean
        get() = navigationView2.getBool(XamlInterop.INavigationView2_get_IsBackEnabled)
        set(value) = navigationView2.putBool(XamlInterop.INavigationView2_put_IsBackEnabled, value)

    /** Appends a menu item to the end (MenuItems.Append). */
    fun addItem(item: WNavigationViewItem) {
        menuItemVector.call(FoundationInterop.IVector_Append, item.inspectable.ptr)
        items += item
    }

    /** Adds an item to the footer menu at the bottom of the pane (FooterMenuItems.Append). */
    fun addFooterItem(item: WNavigationViewItem) {
        footerItemVector.call(FoundationInterop.IVector_Append, item.inspectable.ptr)
        items += item
    }

    /**
     * Subscribes to selection changes (NavigationView.SelectionChanged).
     * The listener receives the selected [WNavigationViewItem]
     * (null if something other than an added item is selected, such as the settings item).
     */
    fun addSelectionListener(listener: (WNavigationViewItem?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.NavigationViewHandler",
            XamlInterop.IID_NavigationViewSelectionChangedHandler,
            XamlInterop.INavigationView_add_SelectionChanged,
        ) { _, args ->
            val e = ComPtr(args)
            val selected = e.getPtrOrNull(XamlInterop.INavigationViewSelectionChangedEventArgs_get_SelectedItem)
            val item = try {
                selected?.let(::resolveItem)
            } finally {
                selected?.release()
            }
            listener(item)
        }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectionListener]. */
    fun removeSelectionListener(listener: (WNavigationViewItem?) -> Unit) {
        val token = selectionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.INavigationView_remove_SelectionChanged, token)
    }

    /**
     * Subscribes to item clicks (NavigationView.ItemInvoked).
     * The listener receives the clicked item's label string (InvokedItem is Content's value).
     * Also fires when re-clicking an already-selected item.
     */
    fun addItemInvokedListener(listener: (String) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.NavigationViewHandler",
            XamlInterop.IID_NavigationViewItemInvokedHandler,
            XamlInterop.INavigationView_add_ItemInvoked,
        ) { _, args ->
            val e = ComPtr(args)
            val boxed = e.getPtrOrNull(XamlInterop.INavigationViewItemInvokedEventArgs_get_InvokedItem)
            val item = try {
                boxed?.let(PropertyValues::unboxString) ?: ""
            } finally {
                boxed?.release()
            }
            listener(item)
        }
        itemInvokedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemInvokedListener]. */
    fun removeItemInvokedListener(listener: (String) -> Unit) {
        val token = itemInvokedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.INavigationView_remove_ItemInvoked, token)
    }

    /**
     * Resolves an added [WNavigationViewItem] back from the SelectedItem pointer.
     * Compares addresses under COM's identity rule (QI'ing IUnknown always returns the same pointer).
     */
    private fun resolveItem(selected: ComPtr): WNavigationViewItem? {
        val target = selected.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            for (item in items) {
                for (candidate in item.selfAndDescendants()) {
                    val mine = candidate.inspectable.queryInterface(KComObject.IID_IUNKNOWN)
                    val matched = mine.ptr.address == target.ptr.address
                    mine.release()
                    if (matched) return candidate
                }
            }
            return null
        } finally {
            target.release()
        }
    }
}
