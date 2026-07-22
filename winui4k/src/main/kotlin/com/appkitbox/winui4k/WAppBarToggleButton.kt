package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * JToggleButton-like for toolbars: WinUI 3's AppBarToggleButton (a Primitives.ToggleButton subclass).
 * [isChecked] / [addItemListener] are inherited from WToggleButton; this adds
 * [label] and [icon] for use on a WCommandBar.
 */
class WAppBarToggleButton(label: String = "", icon: Symbol? = null) : WToggleButton(
    Activation.composeDefault(Abi.CLS_AppBarToggleButton, Abi.IID_IAppBarToggleButtonFactory),
) {
    /** The ICommandBarElement view that holds IsCompact / DynamicOverflowOrder. */
    private val commandBarElement: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_ICommandBarElement))
    }

    /** The label shown below (or to the right of) the icon (AppBarToggleButton.Label). */
    var label: String
        get() = inspectable.getString(Abi.IAppBarToggleButton_get_Label)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(Abi.IAppBarToggleButton_put_Label, h)
        }

    /** The button's icon (AppBarToggleButton.Icon). Creates and passes a SymbolIcon. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.IAppBarToggleButton_put_Icon, null)
                return
            }
            val iconElement = value.createIconElement()
            inspectable.call(Abi.IAppBarToggleButton_put_Icon, iconElement.ptr)
            iconElement.release()
        }

    /** The label position for just this button (AppBarToggleButton.LabelPosition). */
    var labelPosition: CommandBarLabelPosition
        get() = CommandBarLabelPosition.of(
            inspectable.getInt(Abi.IAppBarToggleButton_get_LabelPosition),
        )
        set(value) = inspectable.call(Abi.IAppBarToggleButton_put_LabelPosition, value.native)

    /** Whether it's shown in the compact form with the label hidden (ICommandBarElement.IsCompact). */
    var isCompact: Boolean
        get() = commandBarElement.getBool(Abi.ICommandBarElement_get_IsCompact)
        set(value) = commandBarElement.putBool(Abi.ICommandBarElement_put_IsCompact, value)

    init {
        if (label.isNotEmpty()) this.label = label
        if (icon != null) this.icon = icon
    }
}

/** JToolBar.Separator-like: WinUI 3's AppBarSeparator (a toolbar divider line). */
class WAppBarSeparator : WControl(
    Activation.composeDefault(Abi.CLS_AppBarSeparator, Abi.IID_IAppBarSeparatorFactory),
)
