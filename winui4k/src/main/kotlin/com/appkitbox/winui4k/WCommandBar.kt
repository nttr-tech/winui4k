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
        fun of(native: Int): CommandBarOverflowButtonVisibility =
            entries.first { it.native == native }
    }
}

/**
 * JToolBar-like: WinUI 3's CommandBar.
 * Buttons added with [addPrimaryCommand] line up on the bar; buttons added with
 * [addSecondaryCommand] go into the […] overflow menu.
 */
class WCommandBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_CommandBar, XamlInterop.IID_ICommandBarFactory),
) {
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
     * WAppBarToggleButton / WAppBarSeparator).
     */
    fun addPrimaryCommand(element: WControl) {
        appendCommandBarElement(primaryCommands, element)
    }

    /** Appends a command placed in the overflow menu (CommandBar.SecondaryCommands). */
    fun addSecondaryCommand(element: WControl) {
        appendCommandBarElement(secondaryCommands, element)
    }
}

/** QIs [element] to ICommandBarElement and appends it to the IVector<ICommandBarElement>. */
internal fun appendCommandBarElement(vector: ComPtr, element: WControl) {
    val commandBarElement = element.inspectable.queryInterface(XamlInterop.IID_ICommandBarElement)
    try {
        vector.call(FoundationInterop.IVector_Append, commandBarElement.ptr)
    } finally {
        commandBarElement.release()
    }
}
