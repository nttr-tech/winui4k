package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JButton-like control that fires Click repeatedly while held down: WinUI 3's Primitives.RepeatButton.
 * [delay] (wait before the first fire) and [interval] (time between fires) control the repeat speed.
 */
class WRepeatButton(text: String = "") : WButtonBase(
    Activation.activate(XamlInterop.CLS_RepeatButton, XamlInterop.IID_IRepeatButton), // created via the default factory
) {
    /** Milliseconds to wait after pressing before Click starts firing repeatedly (RepeatButton.Delay). */
    var delay: Int
        get() = inspectable.getInt(XamlInterop.IRepeatButton_get_Delay)
        set(value) = inspectable.call(XamlInterop.IRepeatButton_put_Delay, value)

    /** Milliseconds between repeated fires (RepeatButton.Interval). */
    var interval: Int
        get() = inspectable.getInt(XamlInterop.IRepeatButton_get_Interval)
        set(value) = inspectable.call(XamlInterop.IRepeatButton_put_Interval, value)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
