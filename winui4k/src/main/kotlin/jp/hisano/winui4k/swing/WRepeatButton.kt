package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winui.Abi

/**
 * JButton-like control that fires Click repeatedly while held down: WinUI 3's Primitives.RepeatButton.
 * [delay] (wait before the first fire) and [interval] (time between fires) control the repeat speed.
 */
class WRepeatButton(text: String = "") : WButtonBase(
    Activation.activate(Abi.CLS_RepeatButton).queryInterface(Abi.IID_IRepeatButton), // created via the default factory
) {
    /** Milliseconds to wait after pressing before Click starts firing repeatedly (RepeatButton.Delay). */
    var delay: Int
        get() = inspectable.getInt(Abi.IRepeatButton_get_Delay)
        set(value) = inspectable.call(Abi.IRepeatButton_put_Delay, value)

    /** Milliseconds between repeated fires (RepeatButton.Interval). */
    var interval: Int
        get() = inspectable.getInt(Abi.IRepeatButton_get_Interval)
        set(value) = inspectable.call(Abi.IRepeatButton_put_Interval, value)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
