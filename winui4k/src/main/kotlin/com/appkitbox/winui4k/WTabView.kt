package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.TabViewWidthMode (how tab widths are decided).
 * Values extracted from the winmd (Equal=0, SizeToContent=1, Compact=2).
 */
enum class TabViewWidthMode(internal val native: Int) {
    /** Gives every tab the same width (default). */
    EQUAL(0),

    /** Sizes each tab to fit its header content. */
    SIZE_TO_CONTENT(1),

    /** Shrinks unselected tabs down to icon width. */
    COMPACT(2),
    ;

    internal companion object {
        fun of(native: Int): TabViewWidthMode = entries.first { it.native == native }
    }
}

/**
 * A single JTabbedPane-tab-like: WinUI 3's TabViewItem.
 * Provides [header] (the tab's label), [isClosable], and [content] (shown while selected).
 */
class WTabViewItem(header: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_TabViewItem, XamlInterop.IID_ITabViewItemFactory), // default interface = ITabViewItem
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** The tab's label (TabViewItem.Header). Object-typed, so a boxed string is passed. */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ITabViewItem_get_Header) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ITabViewItem_put_Header, boxed.ptr)
            boxed.release()
        }

    /** Whether this tab shows a close button (TabViewItem.IsClosable). */
    var isClosable: Boolean
        get() = inspectable.getBool(XamlInterop.ITabViewItem_get_IsClosable)
        set(value) = inspectable.putBool(XamlInterop.ITabViewItem_put_IsClosable, value)

    /** The content shown in TabView's content area while this tab is selected (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(XamlInterop.IContentControl_put_Content, value?.uiElement?.ptr)
        }

    init {
        if (header.isNotEmpty()) this.header = header
    }
}

/**
 * A JTabbedPane-like: WinUI 3's TabView.
 *
 * Manage tabs with [addTab] / [removeTab] / [getTab] / [tabCount] / [selectedIndex]; subscribe to
 * tab switches via [addSelectionListener], the close button via [addTabCloseRequestedListener],
 * and the "+" button via [addAddTabButtonClickListener].
 * Combine with [WFrame.extendsContentIntoTitleBar] to place it in the title bar area.
 */
class WTabView : WControl(
    Activation.composeDefault(XamlInterop.CLS_TabView, XamlInterop.IID_ITabViewFactory), // default interface = ITabView
) {
    /**
     * Gets the IVector<Object> view of TabView.TabItems, passes it to [block], and releases it afterward.
     *
     * TabView swaps out what TabItems actually points to onto ListView.Items when its internal
     * ListView's Loaded fires (the `TabItems(lvItems)` call at the end of microsoft-ui-xaml's
     * `TabView::OnListViewLoaded`). So caching the vector means that after Loaded, you'd be
     * operating on the orphaned pre-swap collection instead, with no effect on the display (the
     * Append call itself succeeds and get_Size does increase). Always re-fetch via get_TabItems.
     */
    private inline fun <T> withTabItemVector(block: (ComPtr) -> T): T {
        val items = inspectable.getPtr(XamlInterop.ITabView_get_TabItems)
        try {
            val vector = items.queryInterface(FoundationInterop.IID_IVector_Object)
            try {
                return block(vector)
            } finally {
                vector.release()
            }
        } finally {
            items.release()
        }
    }

    /** The tabs added via [addTab] (used to reverse-look-up an index). */
    private val tabs = mutableListOf<WTabViewItem>()

    /** Listener → event token (used by the remove functions). */
    private val selectionTokens = ListenerTokens<() -> Unit>()
    private val addTabButtonTokens = ListenerTokens<() -> Unit>()
    private val closeRequestedTokens = ListenerTokens<(Int) -> Unit>()

    /** The number of tabs. */
    val tabCount: Int
        get() = tabs.size

    /** How tab widths are decided (TabView.TabWidthMode). */
    var tabWidthMode: TabViewWidthMode
        get() = TabViewWidthMode.of(inspectable.getInt(XamlInterop.ITabView_get_TabWidthMode))
        set(value) = inspectable.call(XamlInterop.ITabView_put_TabWidthMode, value.native)

    /** Whether the "+" button at the end of the tab strip is shown (TabView.IsAddTabButtonVisible). */
    var isAddTabButtonVisible: Boolean
        get() = inspectable.getBool(XamlInterop.ITabView_get_IsAddTabButtonVisible)
        set(value) = inspectable.putBool(XamlInterop.ITabView_put_IsAddTabButtonVisible, value)

    /**
     * Whether tabs can be reordered by dragging (TabView.CanReorderTabs).
     * Reordering doesn't move [getTab]'s indices along with it, so set this to false when
     * managing tabs by index.
     */
    var canReorderTabs: Boolean
        get() = inspectable.getBool(XamlInterop.ITabView_get_CanReorderTabs)
        set(value) = inspectable.putBool(XamlInterop.ITabView_put_CanReorderTabs, value)

    /** The index of the currently selected tab, or -1 if none is selected (TabView.SelectedIndex). */
    var selectedIndex: Int
        get() = inspectable.getInt(XamlInterop.ITabView_get_SelectedIndex)
        set(value) = inspectable.call(XamlInterop.ITabView_put_SelectedIndex, value)

    /** Appends a tab to the end (TabItems.Append). */
    fun addTab(tab: WTabViewItem) {
        withTabItemVector { it.call(FoundationInterop.IVector_Append, tab.inspectable.ptr) }
        tabs += tab
    }

    /** Removes the tab at [index] (TabItems.RemoveAt). */
    fun removeTab(index: Int) {
        withTabItemVector { it.call(FoundationInterop.IVector_RemoveAt, index) }
        tabs.removeAt(index)
    }

    /** Returns the tab at [index]. */
    fun getTab(index: Int): WTabViewItem = tabs[index]

    /** Subscribes to selection changes (TabView.SelectionChanged). */
    fun addSelectionListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ITabView_add_SelectionChanged,
        ) { _, _ -> listener() }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectionListener]. */
    fun removeSelectionListener(listener: () -> Unit) {
        val token = selectionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITabView_remove_SelectionChanged, token)
    }

    /** Subscribes to a click of the "+" button at the end of the tab strip (TabView.AddTabButtonClick). */
    fun addAddTabButtonClickListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TabViewAddTabButtonClickHandler",
            XamlInterop.IID_TabViewAddTabButtonClickHandler,
            XamlInterop.ITabView_add_AddTabButtonClick,
        ) { _, _ -> listener() }
        addTabButtonTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addAddTabButtonClickListener]. */
    fun removeAddTabButtonClickListener(listener: () -> Unit) {
        val token = addTabButtonTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITabView_remove_AddTabButtonClick, token)
    }

    /**
     * Subscribes to a click of a tab's close button (TabView.TabCloseRequested).
     * The listener receives the target tab's index. TabView doesn't close the tab automatically,
     * so call [removeTab] from the listener if it's OK to close it.
     */
    fun addTabCloseRequestedListener(listener: (Int) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TabViewTabCloseRequestedHandler",
            XamlInterop.IID_TabViewTabCloseRequestedHandler,
            XamlInterop.ITabView_add_TabCloseRequested,
        ) { _, args ->
            val tab = ComPtr(args).getPtr(XamlInterop.ITabViewTabCloseRequestedEventArgs_get_Tab)
            val index = try {
                indexOfTab(tab)
            } finally {
                tab.release()
            }
            if (index >= 0) listener(index)
        }
        closeRequestedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTabCloseRequestedListener]. */
    fun removeTabCloseRequestedListener(listener: (Int) -> Unit) {
        val token = closeRequestedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITabView_remove_TabCloseRequested, token)
    }

    /**
     * Reverse-looks-up an added tab's index from a TabViewItem pointer.
     * Compares addresses using the COM identity rule (QI'ing IUnknown always returns the same pointer).
     */
    private fun indexOfTab(target: ComPtr): Int {
        val targetUnknown = target.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            return tabs.indexOfFirst { tab ->
                val mine = tab.inspectable.queryInterface(KComObject.IID_IUNKNOWN)
                val matched = mine.ptr.address == targetUnknown.ptr.address
                mine.release()
                matched
            }
        } finally {
            targetUnknown.release()
        }
    }
}
