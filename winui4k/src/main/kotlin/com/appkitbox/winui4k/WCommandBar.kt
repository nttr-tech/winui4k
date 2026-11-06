package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.AppBarClosedDisplayMode (what's shown while closed).
 * Values extracted from the winmd (Compact=0, Minimal=1, Hidden=2).
 */
enum class AppBarClosedDisplayMode(internal val native: Int) {
    /** Shows icons only (default). */
    COMPACT(0),

    /** Shows only a thin bar. */
    MINIMAL(1),

    /** Fully hidden. */
    HIDDEN(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): AppBarClosedDisplayMode = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.CommandBarDefaultLabelPosition (the default position for button labels).
 * Values extracted from the winmd (Bottom=0, Right=1, Collapsed=2).
 */
enum class CommandBarDefaultLabelPosition(internal val native: Int) {
    /** Below the icon (default). */
    BOTTOM(0),

    /** To the right of the icon. */
    RIGHT(1),

    /** Hidden. */
    COLLAPSED(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): CommandBarDefaultLabelPosition = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.CommandBarOverflowButtonVisibility (whether the […] button shows).
 * Values extracted from the winmd (Auto=0, Visible=1, Collapsed=2).
 */
enum class CommandBarOverflowButtonVisibility(internal val native: Int) {
    /** Shows only when there are overflow items (default). */
    AUTO(0),

    /** Always shown. */
    VISIBLE(1),

    /** Always hidden. */
    COLLAPSED(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): CommandBarOverflowButtonVisibility =
            entries.first { it.native == native }
    }
}

/**
 * JToolBar-like: WinUI 3's CommandBar.
 * Buttons added with [addPrimaryCommand] line up on the bar; buttons added with
 * [addSecondaryCommand] go into the […] overflow menu (no Swing counterpart; not counted in [componentCount] etc.).
 * They can also be handled with Swing's [add] (creating a button from a [WAction] / putting an arbitrary component), [addSeparator], [remove] and
 * [getComponentAtIndex]. It has the same sequence API as [WContainer], but is not a WContainer subclass.
 * Horizontal only ([orientation]), [isFloatable] is always false, and [isRollover] is always true.
 */
class WCommandBar() : WControl(
    Activation.composeDefault(XamlInterop.CLS_CommandBar, XamlInterop.IID_ICommandBarFactory),
) {
    /** JToolBar(String)-like: a bar with [name] (Component.getName; in Swing it is also the title of the window when detached). */
    constructor(name: String) : this() {
        this.name = name
    }

    /**
     * JToolBar(int)-like: a bar with the specified orientation. Only [HORIZONTAL] can be specified; [VERTICAL] and any other value throw
     * IllegalArgumentException (the same as [orientation]).
     */
    constructor(orientation: Int) : this() {
        this.orientation = orientation
    }

    /** JToolBar(String, int)-like: a bar with the specified [name] and orientation (HORIZONTAL only). */
    constructor(name: String, orientation: Int) : this(orientation) {
        this.name = name
    }

    /** The IAppBar view that holds IsOpen / IsSticky / ClosedDisplayMode (CommandBar is an AppBar subclass). */
    private val appBar: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IAppBar)) }

    /**
     * PrimaryCommands / SecondaryCommands are IObservableVector<ICommandBarElement>, so we
     * QI to and hold an IVector<ICommandBarElement> view that has Append.
     */
    private val primaryCommands: ComPtr by lazy {
        queryVector(XamlInterop.ICommandBar_get_PrimaryCommands)
    }
    private val secondaryCommands: ComPtr by lazy {
        queryVector(XamlInterop.ICommandBar_get_SecondaryCommands)
    }

    private fun queryVector(getSlot: Int): ComPtr {
        val observable = inspectable.getPtr(getSlot)
        return try {
            own(observable.queryInterface(XamlInterop.IID_IVector_ICommandBarElement))
        } finally {
            observable.release()
        }
    }

    /**
     * Arbitrary content shown on the bar's left side (ContentControl.Content; CommandBar derives from ContentControl).
     * Typically used for a heading label.
     */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(XamlInterop.IContentControl_put_Content, value?.uiElement?.ptr)
        }

    /** The IContentControl view that holds Content. */
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** Whether the overflow menu is open (AppBar.IsOpen). */
    var isOpen: Boolean
        get() = appBar.getBool(XamlInterop.IAppBar_get_IsOpen)
        set(value) = appBar.putBool(XamlInterop.IAppBar_put_IsOpen, value)

    /** Whether the open state stays open when clicking outside it (AppBar.IsSticky). */
    var isSticky: Boolean
        get() = appBar.getBool(XamlInterop.IAppBar_get_IsSticky)
        set(value) = appBar.putBool(XamlInterop.IAppBar_put_IsSticky, value)

    /** What's shown while closed (AppBar.ClosedDisplayMode). */
    var closedDisplayMode: AppBarClosedDisplayMode
        get() = AppBarClosedDisplayMode.of(appBar.getInt(XamlInterop.IAppBar_get_ClosedDisplayMode))
        set(value) = appBar.call(XamlInterop.IAppBar_put_ClosedDisplayMode, value.native)

    /** The default position for button labels (CommandBar.DefaultLabelPosition). */
    var defaultLabelPosition: CommandBarDefaultLabelPosition
        get() = CommandBarDefaultLabelPosition.of(
            inspectable.getInt(XamlInterop.ICommandBar_get_DefaultLabelPosition),
        )
        set(value) = inspectable.call(XamlInterop.ICommandBar_put_DefaultLabelPosition, value.native)

    /** Whether the […] overflow button is shown (CommandBar.OverflowButtonVisibility). */
    var overflowButtonVisibility: CommandBarOverflowButtonVisibility
        get() = CommandBarOverflowButtonVisibility.of(
            inspectable.getInt(XamlInterop.ICommandBar_get_OverflowButtonVisibility),
        )
        set(value) = inspectable.call(XamlInterop.ICommandBar_put_OverflowButtonVisibility, value.native)

    /** Whether Primary commands are moved to overflow automatically when width runs short (CommandBar.IsDynamicOverflowEnabled). */
    var isDynamicOverflowEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ICommandBar_get_IsDynamicOverflowEnabled)
        set(value) = inspectable.putBool(XamlInterop.ICommandBar_put_IsDynamicOverflowEnabled, value)

    /**
     * Appends a command shown on the bar (CommandBar.PrimaryCommands).
     * [element] must be an ICommandBarElement implementation (WAppBarButton /
     * WAppBarToggleButton / WAppBarSeparator). Otherwise IllegalArgumentException
     * (use [add] to put an arbitrary component). It is also included in the sequence such as [getComponentAtIndex].
     */
    fun addPrimaryCommand(element: WControl) {
        items.add(element, -1, wrap = false)
    }

    /** Appends a command placed in the overflow menu (CommandBar.SecondaryCommands). */
    fun addSecondaryCommand(element: WControl) {
        appendCommandBarElement(secondaryCommands, element)
    }

    private val items = CommandBarItems(this) { primaryCommands }

    /**
     * JToolBar.add(Action)-like: creates a [WAppBarButton] bound to [action] at the end and returns it. The label, icon and enabled state
     * follow the action by the same rules as [WButtonBase.action], and pressing it calls [WAction.actionPerformed].
     */
    fun add(action: WAction): WAppBarButton {
        val button = WAppBarButton()
        button.action = action
        items.add(button, -1, wrap = true)
        return button
    }

    /**
     * JToolBar.add(Component)-like: appends [component] at the end and returns it. AppBar buttons and separators (ICommandBarElement) are put
     * as is, and other components are wrapped in an internal AppBarElementContainer ([getComponentAtIndex] returns [component] itself).
     * If it is in another parent, it is removed from there first. Itself or an ancestor throws IllegalArgumentException.
     */
    fun add(component: WComponent): WComponent {
        items.add(component, -1, wrap = true)
        return component
    }

    /** JToolBar.add(Component, int)-like: adds at [index] (-1 for the end). Out of range throws IllegalArgumentException. */
    fun add(component: WComponent, index: Int): WComponent {
        items.add(component, index, wrap = true)
        return component
    }

    /** JToolBar.addSeparator-like: appends a separator ([WAppBarSeparator]) at the end and returns it. */
    fun addSeparator(): WAppBarSeparator {
        val separator = WAppBarSeparator()
        items.add(separator, -1, wrap = false)
        return separator
    }

    /** JToolBar.getComponentCount-like: the number of (Primary) children on the bar. Items from [addSecondaryCommand] are not counted. */
    val componentCount: Int
        get() = items.count

    /** JToolBar.getComponentAtIndex-like: the child at [index]. null if out of range (as in Swing). */
    fun getComponentAtIndex(index: Int): WComponent? = items.componentAt(index)

    /** JToolBar.getComponentIndex-like: the position of [component]. -1 if it is not a child (as in Swing). */
    fun getComponentIndex(component: WComponent?): Int = items.indexOf(component)

    /** Container.getComponents-like: the list of children (in order; a new array on each call). */
    val components: Array<WComponent>
        get() = items.components()

    /** JToolBar.remove(Component)-like: removes a child. Does nothing if it is not a child. */
    fun remove(component: WComponent) {
        items.remove(component)
    }

    /** Container.remove(int)-like: removes the child at [index]. Out of range throws ArrayIndexOutOfBoundsException. */
    fun remove(index: Int) {
        items.removeAt(index)
    }

    /** Container.removeAll-like: removes all children added with [add] / [addPrimaryCommand]. Items from [addSecondaryCommand] remain. */
    fun removeAll() {
        items.removeAll()
    }

    /** JToolBar.isFloatable-like: always false (a CommandBar cannot be moved; it is a horizontal bar that stays docked). */
    var isFloatable: Boolean
        get() = false

        @Suppress("UNUSED_PARAMETER")
        set(value) {
            // Does nothing: CommandBar has no feature to detach the bar
        }

    /**
     * JToolBar.getOrientation-like: always [HORIZONTAL]. CommandBar (ICommandBar / IAppBar) has no orientation setting and is fixed horizontal.
     * Setting [VERTICAL] throws IllegalArgumentException, and setting [HORIZONTAL] does nothing.
     */
    var orientation: Int
        get() = HORIZONTAL
        set(value) = requireHorizontalOrientation(value)

    /** JToolBar.isRollover-like: always true (AppBarButton changes its look when the pointer is over it by default). Setting does nothing. */
    var isRollover: Boolean
        get() = true

        @Suppress("UNUSED_PARAMETER")
        set(value) {
            // Does nothing: rollover cannot be chosen
        }

    override fun detachChild(child: WComponent) {
        items.remove(child)
    }

    companion object {
        /** Horizontal (the same value as SwingConstants.HORIZONTAL). */
        const val HORIZONTAL = 0

        /** Vertical (the same value as SwingConstants.VERTICAL). Cannot be set to [orientation]. */
        const val VERTICAL = 1
    }
}

/**
 * QIs [element] to ICommandBarElement and Appends it to an IVector<ICommandBarElement>.
 * IllegalArgumentException if it is not an ICommandBarElement.
 */
internal fun appendCommandBarElement(vector: ComPtr, element: WControl) {
    val commandBarElement = element.inspectable.queryInterfaceOrNull(XamlInterop.IID_ICommandBarElement)
        ?: throw IllegalArgumentException(
            "A component that is not an ICommandBarElement cannot be added: ${element.javaClass.simpleName}",
        )
    try {
        vector.call(FoundationInterop.IVector_Append, commandBarElement.ptr)
    } finally {
        commandBarElement.release()
    }
}
