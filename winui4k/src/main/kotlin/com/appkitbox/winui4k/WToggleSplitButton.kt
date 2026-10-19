package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A [WSplitButton] that toggles on/off: WinUI 3's ToggleSplitButton.
 * Clicking the body flips [isChecked]; the right-side arrow opens the flyout.
 */
class WToggleSplitButton @JvmOverloads constructor(text: String = "") : WSplitButton(
    Activation.composeDefault(XamlInterop.CLS_ToggleSplitButton, XamlInterop.IID_IToggleSplitButtonFactory),
) {
    /** The IToggleSplitButton view holding IsChecked / IsCheckedChanged. */
    private val toggleSplitButton: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IToggleSplitButton))
    }

    /** IsCheckedChanged event tokens registered via addItemListener. */
    private val itemTokens = ListenerTokens<Any>()

    /** The checked state (ToggleSplitButton.IsChecked). Unlike ToggleButton, a plain boolean. */
    var isChecked: Boolean
        get() = toggleSplitButton.getBool(XamlInterop.IToggleSplitButton_get_IsChecked)
        set(value) = toggleSplitButton.putBool(XamlInterop.IToggleSplitButton_put_IsChecked, value)

    /** AbstractButton.isSelected-like: same as [isChecked]. */
    var isSelected: Boolean
        get() = isChecked
        set(value) {
            isChecked = value
        }

    /**
     * ItemListener-like: subscribes to changes in the checked state. The listener receives
     * the new [isChecked] value. Subscribes to ToggleSplitButton.IsCheckedChanged (TypedEventHandler) under the hood.
     * WinUI does not raise this event for [isChecked] changes until the control is Loaded (shown in a window with its
     * template applied), so changing [isSelected] before it is shown does not call the listener.
     * To receive a [WItemEvent], use the overload that takes a [WItemListener] (`addItemListener(WItemListener { e -> })`).
     */
    @JvmSynthetic
    fun addItemListener(listener: (Boolean) -> Unit) {
        val adapter = WItemListener { listener(it.stateChange == WItemEvent.SELECTED) }
        addItemListener(adapter)
        itemTokens.addKotlinAdapter(listener, adapter)
    }

    /**
     * The legacy `addItemListener(Consumer<Boolean>)` for Java. It is kept only for binary compatibility (linking of
     * already compiled callers) and is not visible from Java source (HIDDEN). In Java, pass a [WItemListener] instead.
     */
    @Deprecated("Use the addItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("addItemListener")
    fun addItemListenerForJava(listener: Consumer<Boolean>) {
        subscribeItem(listener) { listener.accept(it.stateChange == WItemEvent.SELECTED) }
    }

    /** ItemListener.itemStateChanged-like: receives a [WItemEvent] every time the checked state changes. */
    fun addItemListener(listener: WItemListener) {
        subscribeItem(listener) { listener.itemStateChanged(it) }
    }

    private fun subscribeItem(key: Any, onChange: (WItemEvent) -> Unit) {
        // The handler stays on the native object, so it does not capture the button strongly but looks it up from anchor
        // (see ListenerAnchor)
        val anchor = listenerAnchor
        val token = toggleSplitButton.addEventHandler(
            "WinUI4K.ToggleSplitButtonHandler",
            XamlInterop.IID_ToggleSplitButtonIsCheckedChangedHandler,
            XamlInterop.IToggleSplitButton_add_IsCheckedChanged,
        ) { _, _ ->
            (anchor.get() as? WToggleSplitButton)?.let { button ->
                onChange(WItemEvent(button, if (button.isChecked) WItemEvent.SELECTED else WItemEvent.DESELECTED))
            }
        }
        itemTokens.add(key, token)
        anchor.listenerAdded()
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    @JvmSynthetic
    fun removeItemListener(listener: (Boolean) -> Unit) {
        unsubscribeItem(itemTokens.removeKotlinAdapter(listener) ?: return)
    }

    /** The legacy `removeItemListener(Consumer<Boolean>)` (not visible from Java source), for the same reason as [addItemListenerForJava]. */
    @Deprecated("Use the removeItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("removeItemListener")
    fun removeItemListenerForJava(listener: Consumer<Boolean>) {
        unsubscribeItem(listener)
    }

    /** Unsubscribes a [WItemListener] registered with [addItemListener]. */
    fun removeItemListener(listener: WItemListener) {
        unsubscribeItem(listener)
    }

    private fun unsubscribeItem(key: Any) {
        val token = itemTokens.remove(key) ?: return
        toggleSplitButton.removeEventHandler(XamlInterop.IToggleSplitButton_remove_IsCheckedChanged, token)
        listenerAnchor.listenerRemoved()
    }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
