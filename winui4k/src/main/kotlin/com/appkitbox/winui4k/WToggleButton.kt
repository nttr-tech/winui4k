package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JToggleButton-like: WinUI 3's Primitives.ToggleButton.
 * Also the base of CheckBox / RadioButton.
 *
 * Provides [isChecked] (null = indeterminate), [isThreeState], and
 * [addItemListener] / [removeItemListener] (Checked / Unchecked / Indeterminate).
 */
open class WToggleButton internal constructor(inspectable: ComPtr) : WButtonBase(inspectable) {
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_ToggleButton, XamlInterop.IID_IToggleButtonFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    /** The IToggleButton view holding IsChecked / IsThreeState / the Checked-family events. */
    private val toggleButton: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IToggleButton))
    }

    /** Event tokens registered via addItemListener (3 per listener: Checked / Unchecked / Indeterminate). */
    private val itemTokens = ArrayDeque<Pair<(Boolean?) -> Unit, LongArray>>()

    private companion object {
        val ADD_SLOTS = intArrayOf(
            XamlInterop.IToggleButton_add_Checked,
            XamlInterop.IToggleButton_add_Unchecked,
            XamlInterop.IToggleButton_add_Indeterminate,
        )
        val REMOVE_SLOTS = intArrayOf(
            XamlInterop.IToggleButton_remove_Checked,
            XamlInterop.IToggleButton_remove_Unchecked,
            XamlInterop.IToggleButton_remove_Indeterminate,
        )
    }

    /**
     * The checked state (ToggleButton.IsChecked). null means indeterminate (the third state).
     * The type is IReference<Boolean>, so it's boxed/unboxed when passed.
     */
    var isChecked: Boolean?
        get() {
            val boxed = toggleButton.getPtrOrNull(XamlInterop.IToggleButton_get_IsChecked) ?: return null
            return try {
                PropertyValues.unboxBool(boxed)
            } finally {
                boxed.release()
            }
        }
        set(value) {
            if (value == null) {
                toggleButton.call(XamlInterop.IToggleButton_put_IsChecked, null)
                return
            }
            val boxed = PropertyValues.boxBool(value)
            val reference = boxed.queryInterface(FoundationInterop.IID_IReference_Boolean)
            toggleButton.call(XamlInterop.IToggleButton_put_IsChecked, reference.ptr)
            reference.release()
            boxed.release()
        }

    /** Whether clicking cycles through true → null (indeterminate) → false (ToggleButton.IsThreeState). */
    var isThreeState: Boolean
        get() = toggleButton.getBool(XamlInterop.IToggleButton_get_IsThreeState)
        set(value) = toggleButton.putBool(XamlInterop.IToggleButton_put_IsThreeState, value)

    /**
     * ItemListener-like: subscribes to changes in the checked state. The listener receives
     * the new [isChecked] value. Subscribes to ToggleButton.Checked / Unchecked / Indeterminate
     * (all RoutedEventHandler) together as one unit.
     */
    fun addItemListener(listener: (Boolean?) -> Unit) {
        val tokens = LongArray(ADD_SLOTS.size) { i ->
            toggleButton.addEventHandler(
                "WinUI4K.ToggleHandler",
                XamlInterop.IID_RoutedEventHandler,
                ADD_SLOTS[i],
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
