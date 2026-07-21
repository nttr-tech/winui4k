package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winui.Abi

/**
 * JCheckBox-like: WinUI 3's CheckBox.
 * All functionality comes from [WToggleButton] (ICheckBox itself has no members).
 * Setting [isThreeState] = true cycles through true → null (indeterminate) → false.
 */
class WCheckBox(text: String = "") : WToggleButton(
    Activation.composeDefault(Abi.CLS_CheckBox, Abi.IID_ICheckBoxFactory), // default interface = ICheckBox
) {
    init {
        if (text.isNotEmpty()) this.text = text
    }
}
