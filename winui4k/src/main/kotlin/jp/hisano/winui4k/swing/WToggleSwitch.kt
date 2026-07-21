package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.PropertyValues
import jp.hisano.winui4k.winrt.addEventHandler
import jp.hisano.winui4k.winrt.removeEventHandler
import jp.hisano.winui4k.winui.Abi

/**
 * A two-state on/off switch: WinUI 3's ToggleSwitch.
 * Subscribe to changes in [isOn] via [addItemListener] (Toggled).
 * Customize the displayed text with [header] / [onContent] / [offContent].
 */
class WToggleSwitch(header: String = "") : WControl(
    Activation.activate(Abi.CLS_ToggleSwitch).queryInterface(Abi.IID_IToggleSwitch), // created via the default factory
) {
    /** Toggled event tokens registered via addItemListener. */
    private val itemTokens = ListenerTokens<(Boolean) -> Unit>()

    /** Whether the switch is on (ToggleSwitch.IsOn). */
    var isOn: Boolean
        get() = inspectable.getBool(Abi.IToggleSwitch_get_IsOn)
        set(value) = inspectable.putBool(Abi.IToggleSwitch_put_IsOn, value)

    /** The heading above the switch (ToggleSwitch.Header). Object-typed, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IToggleSwitch_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The text shown while on (ToggleSwitch.OnContent). Default is "On". */
    var onContent: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IToggleSwitch_put_OnContent, boxed.ptr)
            boxed.release()
        }

    /** The text shown while off (ToggleSwitch.OffContent). Default is "Off". */
    var offContent: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IToggleSwitch_put_OffContent, boxed.ptr)
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
            "WinUI4K.ToggledHandler", Abi.IID_RoutedEventHandler, Abi.IToggleSwitch_add_Toggled,
        ) { _, _ -> listener(isOn) }
        itemTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    fun removeItemListener(listener: (Boolean) -> Unit) {
        val token = itemTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IToggleSwitch_remove_Toggled, token)
    }
}
