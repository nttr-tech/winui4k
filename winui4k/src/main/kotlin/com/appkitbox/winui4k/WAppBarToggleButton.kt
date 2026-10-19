package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JToggleButton-like for toolbars: WinUI 3's AppBarToggleButton (a Primitives.ToggleButton subclass).
 * [isChecked] / [addItemListener] are inherited from WToggleButton; this adds
 * [label] and [icon] for use on a WCommandBar.
 */
class WAppBarToggleButton @JvmOverloads constructor(label: String = "", icon: WIcon? = null) : WToggleButton(
    Activation.composeDefault(XamlInterop.CLS_AppBarToggleButton, XamlInterop.IID_IAppBarToggleButtonFactory),
) {
    /** The ICommandBarElement view that holds IsCompact / DynamicOverflowOrder. */
    private val commandBarElement: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ICommandBarElement))
    }

    /** The label shown below (or to the right of) the icon (AppBarToggleButton.Label). */
    var label: String
        get() = inspectable.getString(XamlInterop.IAppBarToggleButton_get_Label)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(XamlInterop.IAppBarToggleButton_put_Label, h)
        }

    /**
     * The legacy `WAppBarToggleButton(String, Symbol)`. Kept only for binary compatibility (linking of compiled callers)
     * and invisible from source (HIDDEN). Symbol is a WIcon, so in source the same call picks the form taking a WIcon.
     * The default arguments are there to also keep the synthetic constructor (String, Symbol, int,
     * DefaultConstructorMarker) used by Kotlin callers compiled with arguments omitted.
     */
    @Deprecated("Use the constructor that takes a WIcon", level = DeprecationLevel.HIDDEN)
    constructor(label: String = "", icon: Symbol? = null) : this(label, icon as WIcon?)

    /**
     * AbstractButton.setIcon-style: the button's icon (AppBarToggleButton.Icon). Returns the icon as set; null for no icon.
     * Unlike [WButtonBase.icon], which is laid out in Content, this creates an IconElement (SymbolIcon / ImageIcon /
     * FontIcon) and passes it to WinUI's AppBarToggleButton's own icon. The template decides the arrangement of icon and
     * label, so [horizontalTextPosition] / [verticalTextPosition] / [iconTextGap] are not used for display (it is decided
     * by [labelPosition] and the CommandBar settings).
     */
    override var icon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val iconElement = value?.createIconElement()
            try {
                inspectable.call(XamlInterop.IAppBarToggleButton_put_Icon, iconElement?.ptr)
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
     * AbstractButton.getText / setText-style: an alias of [label]. The AppBarToggleButton template displays Label rather
     * than Content, so Swing's setText and the action's [WAction.NAME] also go into the label. The default of
     * [actionCommand] is also this string.
     */
    override var text: String
        get() = label
        set(value) {
            label = value
        }

    /** The label position for just this button (AppBarToggleButton.LabelPosition). */
    var labelPosition: CommandBarLabelPosition
        get() = CommandBarLabelPosition.of(
            inspectable.getInt(XamlInterop.IAppBarToggleButton_get_LabelPosition),
        )
        set(value) = inspectable.call(XamlInterop.IAppBarToggleButton_put_LabelPosition, value.native)

    /** Whether it's shown in the compact form with the label hidden (ICommandBarElement.IsCompact). */
    var isCompact: Boolean
        get() = commandBarElement.getBool(XamlInterop.ICommandBarElement_get_IsCompact)
        set(value) = commandBarElement.putBool(XamlInterop.ICommandBarElement_put_IsCompact, value)

    init {
        if (label.isNotEmpty()) this.label = label
        if (icon != null) this.icon = icon
    }
}

/** JToolBar.Separator-like: WinUI 3's AppBarSeparator (a toolbar divider line). */
class WAppBarSeparator : WControl(
    Activation.composeDefault(XamlInterop.CLS_AppBarSeparator, XamlInterop.IID_IAppBarSeparatorFactory),
)
