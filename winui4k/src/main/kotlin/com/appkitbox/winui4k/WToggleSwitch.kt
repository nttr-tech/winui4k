package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A two-state on/off switch: WinUI 3's ToggleSwitch.
 * Subscribe to changes in [isOn] via [addItemListener] (Toggled).
 * Customize the displayed text with [header] / [onContent] / [offContent].
 */
class WToggleSwitch(header: String = "") : WControl(
    Activation.activate(XamlInterop.CLS_ToggleSwitch, XamlInterop.IID_IToggleSwitch), // created via the default factory
) {
    /** Toggled event tokens registered via addItemListener. */
    private val itemTokens = ListenerTokens<(Boolean) -> Unit>()

    /** Whether the switch is on (ToggleSwitch.IsOn). */
    var isOn: Boolean
        get() = inspectable.getBool(XamlInterop.IToggleSwitch_get_IsOn)
        set(value) = inspectable.putBool(XamlInterop.IToggleSwitch_put_IsOn, value)

    /** The heading above the switch (ToggleSwitch.Header). Object-typed, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IToggleSwitch_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The text shown while on (ToggleSwitch.OnContent). Default is "On". */
    var onContent: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IToggleSwitch_put_OnContent, boxed.ptr)
            boxed.release()
        }

    /** The text shown while off (ToggleSwitch.OffContent). Default is "Off". */
    var offContent: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IToggleSwitch_put_OffContent, boxed.ptr)
            boxed.release()
        }

    init {
        if (header.isNotEmpty()) this.header = header
    }

    /**
     * ItemListener-like: subscribes to on/off changes. The listener receives the new [isOn] value.
     * Subscribes to ToggleSwitch.Toggled (RoutedEventHandler) under the hood.
     */
    fun addItemListener(listener: (Boolean) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ToggledHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IToggleSwitch_add_Toggled,
        ) { _, _ -> listener(isOn) }
        itemTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    fun removeItemListener(listener: (Boolean) -> Unit) {
        val token = itemTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IToggleSwitch_remove_Toggled, token)
    }
}
