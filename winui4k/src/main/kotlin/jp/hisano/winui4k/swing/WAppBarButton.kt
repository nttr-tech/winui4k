package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.MemorySegment

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
        fun of(native: Int): CommandBarLabelPosition = entries.first { it.native == native }
    }
}

/**
 * JButton-like for toolbars: WinUI 3's AppBarButton (a Button subclass).
 * Meant to sit on a WCommandBar, so it has [label] and [icon]; Click / Command /
 * Flyout work the same way they do on Button.
 */
class WAppBarButton(label: String = "", icon: Symbol? = null) : WButtonBase(
    WinRt.composeDefault(Abi.CLS_AppBarButton, Abi.IID_IAppBarButtonFactory),
) {
    /** The IButton view that holds Flyout (AppBarButton is a Button subclass). */
    private val button: ComPtr by lazy { inspectable.queryInterface(Abi.IID_IButton) }

    /** The ICommandBarElement view that holds IsCompact / DynamicOverflowOrder. */
    private val commandBarElement: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_ICommandBarElement)
    }

    /** The label shown below (or to the right of) the icon (AppBarButton.Label). */
    var label: String
        get() = inspectable.getString(Abi.IAppBarButton_get_Label)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IAppBarButton_put_Label, h) }

    /** The button's icon (AppBarButton.Icon). Creates and passes a SymbolIcon. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.IAppBarButton_put_Icon, MemorySegment.NULL)
                return
            }
            val iconElement = value.createIconElement()
            inspectable.call(Abi.IAppBarButton_put_Icon, iconElement.ptr)
            iconElement.release()
        }

    /** The label position for just this button (AppBarButton.LabelPosition). */
    var labelPosition: CommandBarLabelPosition
        get() = CommandBarLabelPosition.of(inspectable.getInt(Abi.IAppBarButton_get_LabelPosition))
        set(value) = inspectable.call(Abi.IAppBarButton_put_LabelPosition, value.native)

    /**
     * A shortcut string shown in the tooltip instead of the label
     * (AppBarButton.KeyboardAcceleratorTextOverride). Display only; it doesn't respond to input.
     */
    var keyboardAcceleratorText: String
        get() = inspectable.getString(Abi.IAppBarButton_get_KeyboardAcceleratorTextOverride)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(Abi.IAppBarButton_put_KeyboardAcceleratorTextOverride, h)
        }

    /** The flyout opened by clicking the button (Button.Flyout). Pass a WMenuFlyout to get a dropdown menu. */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            button.call(Abi.IButton_put_Flyout, value?.flyoutBase?.ptr ?: MemorySegment.NULL)
        }

    /** Whether it's shown in the compact form with the label hidden (ICommandBarElement.IsCompact). */
    var isCompact: Boolean
        get() = commandBarElement.getBool(Abi.ICommandBarElement_get_IsCompact)
        set(value) = commandBarElement.putBool(Abi.ICommandBarElement_put_IsCompact, value)

    init {
        if (label.isNotEmpty()) this.label = label
        if (icon != null) this.icon = icon
    }
}
