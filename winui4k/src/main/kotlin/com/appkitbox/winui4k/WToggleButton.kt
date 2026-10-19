package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * JToggleButton-like: WinUI 3's Primitives.ToggleButton.
 * Also the base of CheckBox / RadioButton.
 *
 * Provides [isChecked] (null = indeterminate), [isThreeState], and
 * [addItemListener] / [removeItemListener] (Checked / Unchecked / Indeterminate).
 */
open class WToggleButton internal constructor(inspectable: ComPtr) : WButtonBase(inspectable) {
    @JvmOverloads
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
    private val itemTokens = ArrayDeque<Pair<Any, LongArray>>()

    /** Maps listeners registered as Kotlin lambdas to the [WItemListener]s wrapping them (for removal). */
    private val itemListenerAdapters = KotlinListenerAdapters<WItemListener>()

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

        /** The value put in [WItemEvent.stateChange] for each event in [ADD_SLOTS]. */
        val STATES = intArrayOf(WItemEvent.SELECTED, WItemEvent.DESELECTED, WItemEvent.INDETERMINATE)
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

    /**
     * A two-state view in the style of Swing's AbstractButton.isSelected.
     * It is true only when [isChecked] is true; false and indeterminate (null) are both treated as false.
     * Use [isChecked] when the indeterminate state has to be kept or read.
     */
    var isSelected: Boolean
        get() = isChecked == true
        set(value) {
            isChecked = value
        }

    /** Whether clicking cycles through true → null (indeterminate) → false (ToggleButton.IsThreeState). */
    var isThreeState: Boolean
        get() = toggleButton.getBool(XamlInterop.IToggleButton_get_IsThreeState)
        set(value) = toggleButton.putBool(XamlInterop.IToggleButton_put_IsThreeState, value)

    /**
     * ItemListener-style: subscribes to changes of the check state. The listener receives the new [isChecked]
     * (true / false, or null when indeterminate).
     * Under the hood this subscribes to ToggleButton.Checked / Unchecked / Indeterminate (all RoutedEventHandler) at once.
     * To receive a [WItemEvent], pass a [WItemListener] instead (`addItemListener(WItemListener { e -> })`).
     */
    @JvmSynthetic
    fun addItemListener(listener: (Boolean?) -> Unit) {
        val adapter = WItemListener { listener(WItemEvent.checkedOf(it.stateChange)) }
        addItemListener(adapter)
        itemListenerAdapters.add(listener, adapter)
    }

    /**
     * The legacy `addItemListener(Consumer<Boolean>)` for Java. It is kept only for binary compatibility (linking of
     * already compiled callers) and is not visible from Java source (HIDDEN). In Java, pass a [WItemListener] instead.
     */
    @Deprecated("Use the addItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("addItemListener")
    fun addItemListenerForJava(listener: Consumer<Boolean?>) {
        subscribeItem(listener) { listener.accept(WItemEvent.checkedOf(it.stateChange)) }
    }

    /**
     * ItemListener.itemStateChanged-style: receives a [WItemEvent] each time the selection state changes.
     * It is also called when the state becomes indeterminate ([WItemEvent.INDETERMINATE]), which Swing's
     * `getStateChange() == SELECTED` check treats the same as unchecked.
     */
    fun addItemListener(listener: WItemListener) {
        subscribeItem(listener) { listener.itemStateChanged(it) }
    }

    private fun subscribeItem(key: Any, onChange: (WItemEvent) -> Unit) {
        // The handler stays on the native object, so it does not capture the button strongly but looks it up from anchor
        // (see ListenerAnchor)
        val anchor = listenerAnchor
        val tokens = LongArray(ADD_SLOTS.size) { i ->
            val state = STATES[i]
            toggleButton.addEventHandler(
                "WinUI4K.ToggleHandler",
                XamlInterop.IID_RoutedEventHandler,
                ADD_SLOTS[i],
            ) { _, _ -> (anchor.get() as? WToggleButton)?.let { button -> onChange(WItemEvent(button, state)) } }
        }
        itemTokens.addLast(key to tokens)
        anchor.listenerAdded()
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    @JvmSynthetic
    fun removeItemListener(listener: (Boolean?) -> Unit) {
        val adapter = itemListenerAdapters.remove(listener) ?: return
        removeItemListener(adapter)
    }

    /** The legacy `removeItemListener(Consumer<Boolean>)` (not visible from Java source), for the same reason as [addItemListenerForJava]. */
    @Deprecated("Use the removeItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("removeItemListener")
    fun removeItemListenerForJava(listener: Consumer<Boolean?>) {
        unsubscribeItem(listener)
    }

    /** Unsubscribes a [WItemListener] registered with [addItemListener]. */
    fun removeItemListener(listener: WItemListener) {
        unsubscribeItem(listener)
    }

    private fun unsubscribeItem(key: Any) {
        val index = itemTokens.indexOfLast { it.first === key }
        if (index < 0) return
        val (_, tokens) = itemTokens.removeAt(index)
        for (i in REMOVE_SLOTS.indices) {
            toggleButton.removeEventHandler(REMOVE_SLOTS[i], tokens[i])
        }
        listenerAnchor.listenerRemoved()
    }
}
