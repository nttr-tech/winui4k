package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winui.Abi

/**
 * Common base for WinUI 3's Control-derived controls (Button, TextBox, ...).
 * Non-Control components such as StackPanel stay as plain [WComponent].
 */
abstract class WControl internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    private val control: ComPtr by lazy { inspectable.queryInterface(Abi.IID_IControl) }

    /** setEnabled-like (Control.IsEnabled). */
    var isEnabled: Boolean
        get() = control.getBool(Abi.IControl_get_IsEnabled)
        set(value) = control.putBool(Abi.IControl_put_IsEnabled, value)
}
