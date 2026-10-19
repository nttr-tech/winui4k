package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.CommandBarLabelPosition (per-button label position).
 * Values extracted from the winmd (Default=0, Collapsed=1).
 */
enum class CommandBarLabelPosition(internal val native: Int) {
    /** Follows CommandBar's DefaultLabelPosition (default). */
    DEFAULT(0),

    /** Hides the label for just this button. */
    COLLAPSED(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): CommandBarLabelPosition = entries.first { it.native == native }
    }
}

/**
 * JButton-like for toolbars: WinUI 3's AppBarButton (a Button subclass).
 * Meant to sit on a WCommandBar, so it has [label] and [icon]; Click / Command /
 * Flyout work the same way they do on Button.
 */
class WAppBarButton @JvmOverloads constructor(label: String = "", icon: WIcon? = null) : WButtonBase(
    Activation.composeDefault(XamlInterop.CLS_AppBarButton, XamlInterop.IID_IAppBarButtonFactory),
) {
    /** The IButton view that holds Flyout (AppBarButton is a Button subclass). */
    private val button: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IButton)) }

    /** The ICommandBarElement view that holds IsCompact / DynamicOverflowOrder. */
    private val commandBarElement: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ICommandBarElement))
    }

    /** The label shown below (or to the right of) the icon (AppBarButton.Label). */
    var label: String
        get() = inspectable.getString(XamlInterop.IAppBarButton_get_Label)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IAppBarButton_put_Label, h) }

    /**
     * The legacy `WAppBarButton(String, Symbol)`. It is kept only for binary compatibility (linking of compiled callers)
     * and is not visible from source (HIDDEN). Symbol is a WIcon, so in source the same call selects the overload taking
     * a WIcon.
     * The default arguments are there to also keep the synthetic constructor (String, Symbol, int,
     * DefaultConstructorMarker) used by Kotlin callers compiled with omitted arguments.
     */
    @Deprecated("Use the constructor that takes a WIcon", level = DeprecationLevel.HIDDEN)
    constructor(label: String = "", icon: Symbol? = null) : this(label, icon as WIcon?)

    /**
     * AbstractButton.setIcon-like: the button's icon (AppBarButton.Icon). Returns the icon as set. null means no icon.
     * Unlike [WButtonBase.icon], which is laid out in Content, this creates an IconElement (SymbolIcon / ImageIcon /
     * FontIcon) and passes it to WinUI's AppBarButton's own icon. The template decides the arrangement of the icon and
     * label, so [horizontalTextPosition] / [verticalTextPosition] / [iconTextGap] are not used for display (it is decided
     * by [labelPosition] and the CommandBar settings).
     */
    override var icon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val iconElement = value?.createIconElement()
            try {
                inspectable.call(XamlInterop.IAppBarButton_put_Icon, iconElement?.ptr)
            } finally {
                iconElement?.release()
            }
        }

    /** The legacy `setIcon(Symbol)` (for binary compatibility, not visible from source). Same reason as the legacy constructor. */
    @Deprecated("Use icon, which takes a WIcon", level = DeprecationLevel.HIDDEN)
    @JvmName("setIcon")
    fun setIconForSymbol(icon: Symbol?) {
        this.icon = icon
    }

    /** The legacy `Symbol getIcon()` (for binary compatibility, not visible from source). null if the icon is not a Symbol. */
    @Deprecated("Use icon, which returns a WIcon", level = DeprecationLevel.HIDDEN)
    @JvmName("getIcon")
    fun getIconAsSymbol(): Symbol? = icon as? Symbol

    /**
     * AbstractButton.getText / setText-like: an alias of [label]. AppBarButton's template shows Label, not Content, so
     * Swing's setText and the action's [WAction.NAME] also go into the label. [actionCommand] also defaults to this string.
     */
    override var text: String
        get() = label
        set(value) {
            label = value
        }

    /** The label position for just this button (AppBarButton.LabelPosition). */
    var labelPosition: CommandBarLabelPosition
        get() = CommandBarLabelPosition.of(inspectable.getInt(XamlInterop.IAppBarButton_get_LabelPosition))
        set(value) = inspectable.call(XamlInterop.IAppBarButton_put_LabelPosition, value.native)

    /**
     * A shortcut string shown in the tooltip instead of the label
     * (AppBarButton.KeyboardAcceleratorTextOverride). Display only; it doesn't respond to input.
     */
    var keyboardAcceleratorText: String
        get() = inspectable.getString(XamlInterop.IAppBarButton_get_KeyboardAcceleratorTextOverride)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(XamlInterop.IAppBarButton_put_KeyboardAcceleratorTextOverride, h)
        }

    /** The flyout opened by clicking the button (Button.Flyout). Pass a WMenuFlyout to get a dropdown menu. */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            button.call(XamlInterop.IButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    /** Whether it's shown in the compact form with the label hidden (ICommandBarElement.IsCompact). */
    var isCompact: Boolean
        get() = commandBarElement.getBool(XamlInterop.ICommandBarElement_get_IsCompact)
        set(value) = commandBarElement.putBool(XamlInterop.ICommandBarElement_put_IsCompact, value)

    init {
        if (label.isNotEmpty()) this.label = label
        if (icon != null) this.icon = icon
    }
}
