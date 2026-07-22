package jp.hisano.winui4k

import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.Hstring
import jp.hisano.winui4k.internal.winrt.getString
import jp.hisano.winui4k.internal.winui.Abi

/**
 * JRadioButton-like: WinUI 3's RadioButton.
 * Instead of a ButtonGroup, [groupName] forms the exclusive group
 * (RadioButtons in the same parent are already in the same group even without a groupName).
 */
class WRadioButton(text: String = "") : WToggleButton(
    Activation.composeDefault(Abi.CLS_RadioButton, Abi.IID_IRadioButtonFactory), // default interface = IRadioButton
) {
    /** The name of the exclusive group (RadioButton.GroupName). */
    var groupName: String
        get() = inspectable.getString(Abi.IRadioButton_get_GroupName)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IRadioButton_put_GroupName, h) }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
