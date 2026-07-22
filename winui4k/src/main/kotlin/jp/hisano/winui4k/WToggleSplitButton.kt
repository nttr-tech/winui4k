package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.addEventHandler
import jp.hisano.winui4k.internal.winrt.removeEventHandler
import jp.hisano.winui4k.internal.winui.Abi

/**
 * A [WSplitButton] that toggles on/off: WinUI 3's ToggleSplitButton.
 * Clicking the body flips [isChecked]; the right-side arrow opens the flyout.
 */
class WToggleSplitButton(text: String = "") : WSplitButton(
    Activation.composeDefault(Abi.CLS_ToggleSplitButton, Abi.IID_IToggleSplitButtonFactory),
) {
    /** The IToggleSplitButton view holding IsChecked / IsCheckedChanged. */
    private val toggleSplitButton: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IToggleSplitButton)
    }

    /** IsCheckedChanged event tokens registered via addItemListener. */
    private val itemTokens = ListenerTokens<(Boolean) -> Unit>()

    /** The checked state (ToggleSplitButton.IsChecked). Unlike ToggleButton, a plain boolean. */
    var isChecked: Boolean
        get() = toggleSplitButton.getBool(Abi.IToggleSplitButton_get_IsChecked)
        set(value) = toggleSplitButton.putBool(Abi.IToggleSplitButton_put_IsChecked, value)

    /**
     * ItemListener-like: subscribes to changes in the checked state. The listener receives
     * the new [isChecked] value. Subscribes to ToggleSplitButton.IsCheckedChanged (TypedEventHandler) under the hood.
     */
    fun addItemListener(listener: (Boolean) -> Unit) {
        val token = toggleSplitButton.addEventHandler(
            "WinUI4K.ToggleSplitButtonHandler",
            Abi.IID_ToggleSplitButtonIsCheckedChangedHandler,
            Abi.IToggleSplitButton_add_IsCheckedChanged,
        ) { _, _ -> listener(isChecked) }
        itemTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    fun removeItemListener(listener: (Boolean) -> Unit) {
        val token = itemTokens.remove(listener) ?: return
        toggleSplitButton.removeEventHandler(Abi.IToggleSplitButton_remove_IsCheckedChanged, token)
    }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
