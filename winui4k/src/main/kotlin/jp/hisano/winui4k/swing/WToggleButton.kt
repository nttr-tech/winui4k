package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.PropertyValues
import jp.hisano.winui4k.winrt.addEventHandler
import jp.hisano.winui4k.winrt.removeEventHandler
import jp.hisano.winui4k.winui.Abi

/**
 * JToggleButton-like: WinUI 3's Primitives.ToggleButton.
 * Also the base of CheckBox / RadioButton.
 *
 * Provides [isChecked] (null = indeterminate), [isThreeState], and
 * [addItemListener] / [removeItemListener] (Checked / Unchecked / Indeterminate).
 */
open class WToggleButton internal constructor(inspectable: ComPtr) : WButtonBase(inspectable) {
    constructor(text: String = "") : this(
        Activation.composeDefault(Abi.CLS_ToggleButton, Abi.IID_IToggleButtonFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    /** The IToggleButton view holding IsChecked / IsThreeState / the Checked-family events. */
    private val toggleButton: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IToggleButton)
    }

    /** Event tokens registered via addItemListener (3 per listener: Checked / Unchecked / Indeterminate). */
    private val itemTokens = ArrayDeque<Pair<(Boolean?) -> Unit, LongArray>>()

    private companion object {
        val ADD_SLOTS = intArrayOf(
            Abi.IToggleButton_add_Checked,
            Abi.IToggleButton_add_Unchecked,
            Abi.IToggleButton_add_Indeterminate,
        )
        val REMOVE_SLOTS = intArrayOf(
            Abi.IToggleButton_remove_Checked,
            Abi.IToggleButton_remove_Unchecked,
            Abi.IToggleButton_remove_Indeterminate,
        )
    }

    /**
     * The checked state (ToggleButton.IsChecked). null means indeterminate (the third state).
     * The type is IReference<Boolean>, so it's boxed/unboxed when passed.
     */
    var isChecked: Boolean?
        get() {
            val boxed = toggleButton.getPtrOrNull(Abi.IToggleButton_get_IsChecked) ?: return null
            return try {
                PropertyValues.unboxBool(boxed)
            } finally {
                boxed.release()
            }
        }
        set(value) {
            if (value == null) {
                toggleButton.call(Abi.IToggleButton_put_IsChecked, null)
                return
            }
            val boxed = PropertyValues.boxBool(value)
            val reference = boxed.queryInterface(Abi.IID_IReference_Boolean)
            toggleButton.call(Abi.IToggleButton_put_IsChecked, reference.ptr)
            reference.release()
            boxed.release()
        }

    /** Whether clicking cycles through true → null (indeterminate) → false (ToggleButton.IsThreeState). */
    var isThreeState: Boolean
        get() = toggleButton.getBool(Abi.IToggleButton_get_IsThreeState)
        set(value) = toggleButton.putBool(Abi.IToggleButton_put_IsThreeState, value)

    /**
     * ItemListener-like: subscribes to changes in the checked state. The listener receives
     * the new [isChecked] value. Subscribes to ToggleButton.Checked / Unchecked / Indeterminate
     * (all RoutedEventHandler) together as one unit.
     */
    fun addItemListener(listener: (Boolean?) -> Unit) {
        val tokens = LongArray(ADD_SLOTS.size) { i ->
            toggleButton.addEventHandler(
                "WinUI4K.ToggleHandler", Abi.IID_RoutedEventHandler, ADD_SLOTS[i],
            ) { _, _ -> listener(isChecked) }
        }
        itemTokens.addLast(listener to tokens)
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    fun removeItemListener(listener: (Boolean?) -> Unit) {
        val index = itemTokens.indexOfLast { it.first === listener }
        if (index < 0) return
        val (_, tokens) = itemTokens.removeAt(index)
        for (i in REMOVE_SLOTS.indices) {
            toggleButton.removeEventHandler(REMOVE_SLOTS[i], tokens[i])
        }
    }
}
