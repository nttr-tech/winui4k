package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
import java.util.function.IntConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
        fun of(native: Int): TabViewWidthMode = entries.first { it.native == native }
    }
}

/**
 * A single JTabbedPane-tab-like: WinUI 3's TabViewItem.
 * Provides [header] (the tab's label) / [icon] / [isClosable] / [content] (what is shown when selected).
 * The tab's tooltip is set with [toolTipText] (corresponds to JTabbedPane's setToolTipTextAt), and whether it can be pressed with [isEnabled].
 */
class WTabViewItem @JvmOverloads constructor(header: String = "") : WControl(
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

    /**
     * The icon shown to the left of the label (TabViewItem.IconSource). Null shows none (the default).
     * [Symbol] / [WImageIcon] / [WFontIcon] are converted to an IconSource and set. get returns the WIcon that was set
     * (because a WIcon cannot be recovered from the native IconSource). Unsupported implementations throw [IllegalArgumentException].
     */
    var icon: WIcon? = null
        set(value) {
            val source = value?.createIconSource()
            try {
                inspectable.call(XamlInterop.ITabViewItem_put_IconSource, source?.ptr)
            } finally {
                source?.release()
            }
            field = value
        }

    /**
     * The tab version of JTabbedPane.getMnemonicAt / setMnemonicAt: the key code of the access key (UIElement.AccessKey). Uses the same values as AWT's KeyEvent.VK_A..VK_Z
     * (65..90) / VK_0..VK_9 (48..57); leaving it unset or setting 0 clears it (reads as 0). Anything other than letters and digits throws
     * [IllegalArgumentException]. It appears in the key tips shown when Alt is pressed, and pressing that key selects the tab
     * (unlike Swing, the character in the label is not underlined, so there is no counterpart of setDisplayedMnemonicIndexAt).
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /** Whether to show a close button on the tab (TabViewItem.IsClosable). WinUI's default is true. */
    var isClosable: Boolean
        get() = inspectable.getBool(XamlInterop.ITabViewItem_get_IsClosable)
        set(value) = inspectable.putBool(XamlInterop.ITabViewItem_put_IsClosable, value)

    /** The content shown in TabView's content area while this tab is selected (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            contentControl.call(XamlInterop.IContentControl_put_Content, value?.uiElement?.ptr)
        }

    override fun detachChild(child: WComponent) {
        if (this.content === child) this.content = null
    }

    /**
     * The [WTabView] containing this tab (a weak reference; the tab view holds the tab strongly). Used to remove it from the original first when it is added to another tab view.
     * It is not made the [WComponent.parent] (the tab view is not exposed as the tab's parent component).
     */
    internal var owningTabView: WeakReference<WTabView>? = null

    init {
        if (header.isNotEmpty()) this.header = header
    }
}

/**
 * A JTabbedPane-like: WinUI 3's TabView.
 *
 * Swing-shaped [addTab] (title, icon, content, tooltip) / [insertTab] / [add] / [removeTabAt] / [removeAll] /
 * [indexOfTab] / [indexOfComponent] / [getTitleAt] and so on, and with [addTab] / [removeTab] / [getTab] that take a [WTabViewItem] directly.
 * Indexes are computed from the order of the native TabItems, so they match the on-screen order even after the user reorders tabs by dragging.
 * Selection changes are notified by [addChangeListener] (ChangeListener-style), and close buttons by [addTabCloseRequestedListener].
 * and the "+" button via [addAddTabButtonClickListener].
 *
 * The selection follows the same rules as Swing: adding the first tab selects it, adding or removing a tab before the selection shifts the selected index,
 * and removing the selected tab selects the tab at the same position (the previous one if it was last). Without tabs [selectedIndex] is -1.
 *
 * WinUI's defaults (the tabs' close buttons, the "+" button, reordering by dragging) are kept. Only tabs created with the Swing-shaped [addTab] / [insertTab]
 * show no close button (JTabbedPane's tabs have none). Hide the "+" button with [isAddTabButtonVisible].
 * Combine with [WFrame.extendsContentIntoTitleBar] to place it in the title bar area.
 */
@Suppress("TooManyFunctions") // Lists JTabbedPane's API (per-tab get / set) as is
class WTabView : WControl(
    Activation.composeDefault(XamlInterop.CLS_TabView, XamlInterop.IID_ITabViewFactory), // default interface = ITabView
) {
    /**
     * The reverse lookup of tab wrappers (the TabViewItem's IUnknown address -> wrapper). Only the native TabItems holds the order.
     * The wrapper references the native object, so the address does not change while it is registered.
     */
    private val tabsByIdentity = HashMap<Long, WTabViewItem>()

    /** Listener → event token (used by the remove functions). */
    private val selectionTokens = ListenerTokens<Runnable>()
    private val addTabButtonTokens = ListenerTokens<Runnable>()
    private val closeRequestedTokens = ListenerTokens<IntConsumer>()

    /** The ChangeListeners. The SelectionChanged and TabDragCompleted handlers are registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addSelectionTracking() }

    /** The last notified selection (the index and the IUnknown address of the selected tab). Null means it is not tracked (no listeners added). */
    private var notifiedSelection: Pair<Int, Long>? = null

    /** In the middle of adding or removing tabs (intermediate selections the native side makes temporarily are not notified; compared once after it ends). */
    private var mutating = false

    /** The number of tabs (the number of elements in TabItems). */
    val tabCount: Int
        get() = withTabItemVector(inspectable, ::sizeOf)

    /** How tab widths are decided (TabView.TabWidthMode). */
    var tabWidthMode: TabViewWidthMode
        get() = TabViewWidthMode.of(inspectable.getInt(XamlInterop.ITabView_get_TabWidthMode))
        set(value) = inspectable.call(XamlInterop.ITabView_put_TabWidthMode, value.native)

    /** Whether to show the "+" button at the end of the tab strip (TabView.IsAddTabButtonVisible). WinUI's default is true. */
    var isAddTabButtonVisible: Boolean
        get() = inspectable.getBool(XamlInterop.ITabView_get_IsAddTabButtonVisible)
        set(value) = inspectable.putBool(XamlInterop.ITabView_put_IsAddTabButtonVisible, value)

    /**
     * Whether tabs can be reordered by dragging (TabView.CanReorderTabs). WinUI's default is true.
     * After reordering, the indexes of [getTab] and the like refer to the new order.
     */
    var canReorderTabs: Boolean
        get() = inspectable.getBool(XamlInterop.ITabView_get_CanReorderTabs)
        set(value) = inspectable.putBool(XamlInterop.ITabView_put_CanReorderTabs, value)

    /**
     * The index of the selected tab. -1 if there are no tabs or nothing is selected (TabView.SelectedIndex).
     * Only -1 (deselect) or 0 to [tabCount] - 1 can be set; anything else throws [IndexOutOfBoundsException] (same as Swing).
     * When setting it changes the selection, the [addChangeListener] listeners are called.
     */
    var selectedIndex: Int
        get() = if (tabCount == 0) -1 else inspectable.getInt(XamlInterop.ITabView_get_SelectedIndex)
        set(value) {
            if (value != -1) checkIndex(value)
            mutate { inspectable.call(XamlInterop.ITabView_put_SelectedIndex, value) }
        }

    /**
     * The content of the selected tab ([WTabViewItem.content]). Null if no tab is selected.
     * Setting it selects the tab with that content. [IllegalArgumentException] if it is not the content of any tab (same as Swing).
     */
    var selectedComponent: WComponent?
        get() {
            val index = selectedIndex
            return if (index < 0) null else getTab(index).content
        }
        set(value) {
            val index = if (value == null) -1 else indexOfComponent(value)
            require(index >= 0) { "component not found in tabbed pane" }
            selectedIndex = index
        }

    /**
     * Adds a tab at the end (TabItems.Append). If the tab is already in this tab view, it is removed from its old position and moved to the end.
     * If the tab is in another WTabView, it is removed from there first (the same as Swing's add removing it from the original parent; the selection on the removed side also follows Swing's rules).
     */
    fun addTab(tab: WTabViewItem) {
        val existing = indexOfItem(tab)
        if (existing >= 0) removeTab(existing)
        insertItem(tab, tabCount)
    }

    /**
     * Like JTabbedPane.addTab: adds a tab titled [title] at the end that shows [component] when selected.
     * The created tab shows no close button ([WTabViewItem.isClosable] = false). If [component] is already the content of some tab, that tab is removed.
     */
    fun addTab(title: String?, component: WComponent?) {
        insertTab(title, null, component, null, tabCount)
    }

    /** Like JTabbedPane.addTab: adds a tab at the end that also has an icon ([icon]) and a tooltip ([tip], none if null). Otherwise the same as the two-argument [addTab]. */
    @JvmOverloads
    fun addTab(title: String?, icon: WIcon?, component: WComponent?, tip: String? = null) {
        insertTab(title, icon, component, tip, tabCount)
    }

    /**
     * Like JTabbedPane.insertTab: inserts a tab at position [index]. [index] must be 0 to [tabCount]; anything else throws [IndexOutOfBoundsException].
     * If [component] is already the content of some tab, that tab is removed first (an [index] after the removed tab shifts forward by one; same as Swing).
     * The created tab shows no close button.
     */
    fun insertTab(title: String?, icon: WIcon?, component: WComponent?, tip: String?, index: Int) {
        var newIndex = index
        val removeIndex = if (component == null) -1 else indexOfComponent(component)
        if (removeIndex >= 0) {
            removeTabAt(removeIndex)
            if (newIndex > removeIndex) newIndex--
        }
        val count = tabCount
        if (newIndex !in 0..count) throw IndexOutOfBoundsException("Index: $newIndex, Tab count: $count")
        val tab = WTabViewItem(title ?: "")
        tab.isClosable = false
        tab.icon = icon
        tab.toolTipText = tip
        tab.content = component
        insertItem(tab, newIndex)
    }

    /**
     * JTabbedPane.add(Component)-like: appends a tab titled with the name of [component] ([WComponent.name], "" if unset), and
     * returns [component] (a tab without a close button, the same as [addTab] (title, component)).
     * If [component] is a [WTabViewItem], adds that tab itself (the same as [addTab] (WTabViewItem)).
     */
    fun add(component: WComponent): WComponent {
        if (component is WTabViewItem) addTab(component) else addTab(component.name, component)
        return component
    }

    /** JTabbedPane.add(String, Component)-like: the same as [addTab] (title, component), returning [component]. */
    fun add(title: String?, component: WComponent): WComponent {
        addTab(title, component)
        return component
    }

    /**
     * JTabbedPane.add(Component, int)-like: inserts a tab titled with the name of [component] at [index] (-1 for the end), and returns [component].
     * Other out-of-range [index] values throw [IndexOutOfBoundsException] (the same as [insertTab]).
     */
    fun add(component: WComponent, index: Int): WComponent {
        insertTab(component.name, null, component, null, if (index == -1) tabCount else index)
        return component
    }

    /**
     * JTabbedPane.add(Component, Object)-like: appends a tab whose title is [constraints] if it is a string, or whose icon is [constraints] if it is a [WIcon]
     * (title ""). Other values (including null) use the name as the title, the same as [add] (component).
     */
    fun add(component: WComponent, constraints: Any?) {
        when (constraints) {
            is String -> addTab(constraints, component)
            is WIcon -> addTab(null, constraints, component)
            else -> add(component)
        }
    }

    /**
     * JTabbedPane.add(Component, Object, int)-like: inserts a tab whose title is [constraints] if it is a string, or whose icon it is if it is a [WIcon],
     * at [index] (-1 for the end). Otherwise the title is "" (as in Swing, the name is not used).
     */
    fun add(component: WComponent, constraints: Any?, index: Int) {
        insertTab(constraints as? String, constraints as? WIcon, component, null, if (index == -1) tabCount else index)
    }

    /** Removes the [index]th tab (TabItems.RemoveAt). [IndexOutOfBoundsException] if out of range. */
    fun removeTab(index: Int) {
        checkIndex(index)
        mutate {
            val selected = selectedIndex
            withTabItemVector(inspectable) { vector ->
                tabsByIdentity.remove(identityAt(vector, index))?.owningTabView = null
                vector.call(FoundationInterop.IVector_RemoveAt, index)
            }
            val count = tabCount
            val expected = when {
                selected > index -> selected - 1
                selected >= count -> selected - 1
                else -> selected
            }
            applySelectedIndex(expected)
        }
    }

    /** Like JTabbedPane.removeTabAt: same as [removeTab]. */
    fun removeTabAt(index: Int) = removeTab(index)

    /**
     * Like JTabbedPane.remove(Component): removes the tab whose content is [component] (or the tab that is [component] itself).
     * Does nothing if it is neither.
     */
    fun remove(component: WComponent?) {
        if (component == null) return
        val index = if (component is WTabViewItem) indexOfItem(component) else -1
        val target = if (index >= 0) index else indexOfComponent(component)
        if (target >= 0) removeTab(target)
    }

    /** Like JTabbedPane.removeAll: removes all tabs. The selection becomes -1, and if something was selected the ChangeListeners are called once. */
    fun removeAll() {
        mutate {
            applySelectedIndex(-1)
            withTabItemVector(inspectable) { vector ->
                for (index in sizeOf(vector) - 1 downTo 0) {
                    tabsByIdentity.remove(identityAt(vector, index))?.owningTabView = null
                    vector.call(FoundationInterop.IVector_RemoveAt, index)
                }
            }
            applySelectedIndex(-1)
        }
    }

    /**
     * Returns the [index]th tab. [IndexOutOfBoundsException] if out of range.
     * Tabs that came in other than through this class's API (dragging from another TabView and so on) have no wrapper, so [IllegalStateException].
     */
    fun getTab(index: Int): WTabViewItem {
        checkIndex(index)
        val identity = withTabItemVector(inspectable) { identityAt(it, index) }
        return checkNotNull(tabsByIdentity[identity]) { "tab added other than through the WTabView API (index=$index)" }
    }

    /** Like JTabbedPane.indexOfTab(String): the index of the first tab whose label is [title], or -1 if none. */
    fun indexOfTab(title: String?): Int = (0 until tabCount).firstOrNull { getTab(it).header == (title ?: "") } ?: -1

    /** Like JTabbedPane.indexOfTab(Icon): the index of the first tab whose icon equals [icon], or -1 if none. */
    fun indexOfTab(icon: WIcon?): Int = (0 until tabCount).firstOrNull { getTab(it).icon == icon } ?: -1

    /** Like JTabbedPane.indexOfComponent: the index of the first tab whose content is [component] (the same object), or -1 if none. */
    fun indexOfComponent(component: WComponent?): Int =
        (0 until tabCount).firstOrNull { getTab(it).content === component } ?: -1

    /** The label of the [index]th tab ([WTabViewItem.header]). */
    fun getTitleAt(index: Int): String = getTab(index).header

    /** Changes the label of the [index]th tab. Null is an empty string. */
    fun setTitleAt(index: Int, title: String?) {
        getTab(index).header = title ?: ""
    }

    /** The icon of the [index]th tab ([WTabViewItem.icon]). */
    fun getIconAt(index: Int): WIcon? = getTab(index).icon

    /** Changes the icon of the [index]th tab. Null removes it. */
    fun setIconAt(index: Int, icon: WIcon?) {
        getTab(index).icon = icon
    }

    /** The tooltip of the [index]th tab ([WTabViewItem.toolTipText]). */
    fun getToolTipTextAt(index: Int): String? = getTab(index).toolTipText

    /** Changes the tooltip of the [index]th tab. Null removes it. */
    fun setToolTipTextAt(index: Int, toolTipText: String?) {
        getTab(index).toolTipText = toolTipText
    }

    /** Whether the [index]th tab can be pressed ([WTabViewItem.isEnabled]). */
    fun isEnabledAt(index: Int): Boolean = getTab(index).isEnabled

    /** Changes whether the [index]th tab can be pressed. Tabs that cannot be pressed can still be selected from the program (same as Swing). */
    fun setEnabledAt(index: Int, enabled: Boolean) {
        getTab(index).isEnabled = enabled
    }

    /** JTabbedPane.getMnemonicAt-like: the access key code of the tab at [index] ([WTabViewItem.mnemonic]). 0 if unset. */
    fun getMnemonicAt(index: Int): Int = getTab(index).mnemonic

    /**
     * JTabbedPane.setMnemonicAt-like: sets the access key of the tab at [index] to [mnemonic] (the same values as KeyEvent.VK_A etc.; 0 clears it)
     * ([WTabViewItem.mnemonic]). Anything other than letters and digits throws [IllegalArgumentException].
     */
    fun setMnemonicAt(index: Int, mnemonic: Int) {
        getTab(index).mnemonic = mnemonic
    }

    /** The content of the [index]th tab ([WTabViewItem.content]). */
    fun getComponentAt(index: Int): WComponent? = getTab(index).content

    /** Changes the content of the [index]th tab. */
    fun setComponentAt(index: Int, component: WComponent?) {
        getTab(index).content = component
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this tab view) every time the selection changes.
     * Called when the selected index or the selected tab changes by the user's click or reordering by dragging, setting [selectedIndex], or adding or removing tabs
     * (as with JTabbedPane, also when the selected tab is removed and the next tab at the same position is selected).
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order they were added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    /**
     * Subscribes to selection changes (TabView.SelectionChanged). The native notifications are passed through as is, so it is called even for selections in the middle of adding or removing tabs.
     * To be notified at the same points as Swing, use [addChangeListener].
     */
    @JvmSynthetic
    fun addSelectionListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addSelectionListenerForJava(adapter)
        selectionTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addSelectionListener")
    fun addSelectionListenerForJava(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ITabView_add_SelectionChanged,
        ) { _, _ -> listener.run() }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectionListener]. */
    @JvmSynthetic
    fun removeSelectionListener(listener: () -> Unit) {
        val adapter = selectionTokens.removeKotlinAdapter(listener) ?: return
        removeSelectionListenerForJava(adapter)
    }

    @JvmName("removeSelectionListener")
    fun removeSelectionListenerForJava(listener: Runnable) {
        val token = selectionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITabView_remove_SelectionChanged, token)
    }

    /** Subscribes to a click of the "+" button at the end of the tab strip (TabView.AddTabButtonClick). */
    @JvmSynthetic
    fun addAddTabButtonClickListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addAddTabButtonClickListenerForJava(adapter)
        addTabButtonTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addAddTabButtonClickListener")
    fun addAddTabButtonClickListenerForJava(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TabViewAddTabButtonClickHandler",
            XamlInterop.IID_TabViewAddTabButtonClickHandler,
            XamlInterop.ITabView_add_AddTabButtonClick,
        ) { _, _ -> listener.run() }
        addTabButtonTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addAddTabButtonClickListener]. */
    @JvmSynthetic
    fun removeAddTabButtonClickListener(listener: () -> Unit) {
        val adapter = addTabButtonTokens.removeKotlinAdapter(listener) ?: return
        removeAddTabButtonClickListenerForJava(adapter)
    }

    @JvmName("removeAddTabButtonClickListener")
    fun removeAddTabButtonClickListenerForJava(listener: Runnable) {
        val token = addTabButtonTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITabView_remove_AddTabButtonClick, token)
    }

    /**
     * Subscribes to a click of a tab's close button (TabView.TabCloseRequested).
     * The listener receives the target tab's index in the current order. TabView does not close tabs automatically, so
     * so call [removeTab] from the listener if it's OK to close it.
     */
    @JvmSynthetic
    fun addTabCloseRequestedListener(listener: (Int) -> Unit) {
        val adapter = IntConsumer { listener(it) }
        addTabCloseRequestedListenerForJava(adapter)
        closeRequestedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addTabCloseRequestedListener")
    fun addTabCloseRequestedListenerForJava(listener: IntConsumer) {
        // The handler stays on the native side, so it does not capture this wrapper. The index is computed from the sender's (TabView) TabItems
        val token = inspectable.addEventHandler(
            "WinUI4K.TabViewTabCloseRequestedHandler",
            XamlInterop.IID_TabViewTabCloseRequestedHandler,
            XamlInterop.ITabView_add_TabCloseRequested,
        ) { sender, args ->
            val index = indexOfClosingTab(ComPtr(sender), ComPtr(args))
            if (index >= 0) listener.accept(index)
        }
        closeRequestedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTabCloseRequestedListener]. */
    @JvmSynthetic
    fun removeTabCloseRequestedListener(listener: (Int) -> Unit) {
        val adapter = closeRequestedTokens.removeKotlinAdapter(listener) ?: return
        removeTabCloseRequestedListenerForJava(adapter)
    }

    @JvmName("removeTabCloseRequestedListener")
    fun removeTabCloseRequestedListenerForJava(listener: IntConsumer) {
        val token = closeRequestedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITabView_remove_TabCloseRequested, token)
    }

    /**
     * Inserts [tab] at [index] and aligns the selection with Swing's rules (select the first tab, shift when inserted before the selection).
     * After loading, the native side does not select a tab added to an empty TabView, and before loading it does not shift because SelectedIndex stays at its default 0,
     * so it is rewritten to give the same result in both states.
     */
    private fun insertItem(tab: WTabViewItem, index: Int) {
        tab.owningTabView?.get()?.takeIf { it !== this }?.remove(tab)
        mutate {
            val selected = selectedIndex
            val wasEmpty = tabCount == 0
            withTabItemVector(inspectable) { vector ->
                vector.call(FoundationInterop.IVector_InsertAt, index, tab.inspectable.ptr)
            }
            tabsByIdentity[identityOf(tab.inspectable)] = tab
            tab.owningTabView = WeakReference(this)
            applySelectedIndex(
                when {
                    wasEmpty -> 0
                    selected >= index -> selected + 1
                    else -> selected
                },
            )
        }
    }

    /** Writes the native SelectedIndex only when it differs from [expected] (writing the same value would cause an extra SelectionChanged). */
    private fun applySelectedIndex(expected: Int) {
        if (inspectable.getInt(XamlInterop.ITabView_get_SelectedIndex) != expected) {
            inspectable.call(XamlInterop.ITabView_put_SelectedIndex, expected)
        }
    }

    /** During [block], native selection notifications are not passed on; afterwards, if the selection changed, it is notified once. */
    private inline fun mutate(block: () -> Unit) {
        val outer = mutating
        mutating = true
        try {
            block()
        } finally {
            mutating = outer
        }
        if (!outer) notifySelectionIfChanged()
    }

    private fun notifySelectionIfChanged() {
        val old = notifiedSelection ?: return
        if (mutating) return
        val now = currentSelection()
        notifiedSelection = now
        changeListeners.fireIfChanged(old, now)
    }

    private fun currentSelection(): Pair<Int, Long> {
        val index = selectedIndex
        val identity = if (index < 0) 0L else withTabItemVector(inspectable) { identityAt(it, index) }
        return index to identity
    }

    /**
     * Starts tracking the selection. User clicks are learned from SelectionChanged and reordering by dragging from TabDragCompleted
     * (reordering may change the selected index without a SelectionChanged, and TabDragCompleted is sent after SelectedIndex is
     * reread from the internal ListView; microsoft-ui-xaml TabView::OnListViewDragItemsCompleted).
     * The handlers stay on the native side, so this wrapper is not strongly captured; it is looked up through the anchor (see ListenerAnchor).
     */
    private fun addSelectionTracking() {
        notifiedSelection = currentSelection()
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.TabViewSelectionTrackingHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ITabView_add_SelectionChanged,
        ) { _, _ -> (anchor.get() as? WTabView)?.notifySelectionIfChanged() }
        inspectable.addEventHandler(
            "WinUI4K.TabViewTabDragCompletedHandler",
            XamlInterop.IID_TabViewTabDragCompletedHandler,
            XamlInterop.ITabView_add_TabDragCompleted,
        ) { _, _ -> (anchor.get() as? WTabView)?.notifySelectionIfChanged() }
    }

    private fun checkIndex(index: Int) {
        val count = tabCount
        if (index < 0 || index >= count) throw IndexOutOfBoundsException("Index: $index, Tab count: $count")
    }

    /** The index of [tab] in the current order. -1 if it is not in this tab view. */
    private fun indexOfItem(tab: WTabViewItem): Int {
        val target = identityOf(tab.inspectable)
        return withTabItemVector(inspectable) { indexOfIdentity(it, target) }
    }

    private companion object {
        /**
         * Gets the IVector<Object> view of [tabView]'s (ITabView) TabItems, passes it to [block], and releases it after use.
         *
         * When its internal ListView is Loaded, TabView replaces the object behind the TabItems property with ListView.Items
         * (the `TabItems(lvItems)` at the end of microsoft-ui-xaml TabView::OnListViewLoaded).
         * So caching the vector would, after Loaded, operate on the orphaned collection from before the replacement
         * and the changes would not be reflected on screen (Append itself succeeds and get_Size also grows).
         * Always get it again with get_TabItems every time.
         */
        inline fun <T> withTabItemVector(tabView: ComPtr, block: (ComPtr) -> T): T {
            val items = tabView.getPtr(XamlInterop.ITabView_get_TabItems)
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

        fun sizeOf(vector: ComPtr): Int = vector.getInt(FoundationInterop.IVector_get_Size)

        /** The COM identity (IUnknown address) of the [index]th element of [vector]. */
        fun identityAt(vector: ComPtr, index: Int): Long {
            val item = vector.getPtr(FoundationInterop.IVector_GetAt, index)
            return try {
                identityOf(item)
            } finally {
                item.release()
            }
        }

        /** The index of the element in [vector] whose identity is [identity], or -1 if none. */
        fun indexOfIdentity(vector: ComPtr, identity: Long): Int =
            (0 until sizeOf(vector)).firstOrNull { identityAt(vector, it) == identity } ?: -1

        /** The identity of an object by COM's identity rule (QI for IUnknown always returns the same pointer). */
        fun identityOf(pointer: ComPtr): Long {
            val unknown = pointer.queryInterface(KComObject.IID_IUNKNOWN)
            return try {
                unknown.ptr.address
            } finally {
                unknown.release()
            }
        }

        /** The index of the tab being closed ([args]' Tab) in the order of the TabCloseRequested sender [sender] (the TabView; borrowed). */
        fun indexOfClosingTab(sender: ComPtr, args: ComPtr): Int {
            val tab = args.getPtr(XamlInterop.ITabViewTabCloseRequestedEventArgs_get_Tab)
            val target = try {
                identityOf(tab)
            } finally {
                tab.release()
            }
            val tabView = sender.queryInterface(XamlInterop.IID_ITabView)
            return try {
                withTabItemVector(tabView) { indexOfIdentity(it, target) }
            } finally {
                tabView.release()
            }
        }
    }
}
