package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A two-state on/off switch: WinUI 3's ToggleSwitch.
 * Subscribe to changes in [isOn] via [addItemListener] (Toggled).
 * Customize the displayed text with [header] / [onContent] / [offContent].
 */
class WToggleSwitch @JvmOverloads constructor(header: String = "") : WControl(
    Activation.activate(XamlInterop.CLS_ToggleSwitch, XamlInterop.IID_IToggleSwitch), // created via the default factory
) {
    /** Toggled event tokens registered via addItemListener. */
    private val itemTokens = ListenerTokens<Any>()

    /** Whether the switch is on (ToggleSwitch.IsOn). */
    var isOn: Boolean
        get() = inspectable.getBool(XamlInterop.IToggleSwitch_get_IsOn)
        set(value) = inspectable.putBool(XamlInterop.IToggleSwitch_put_IsOn, value)

    /** AbstractButton.isSelected-like: same as [isOn]. */
    var isSelected: Boolean
        get() = isOn
        set(value) {
            isOn = value
        }

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

    /** ItemListener.itemStateChanged-like: receives a [WItemEvent] every time it toggles on/off (on is SELECTED, off is DESELECTED). */
    fun addItemListener(listener: WItemListener) {
        subscribeItem(listener) { listener.itemStateChanged(it) }
    }

    private fun subscribeItem(key: Any, onChange: (WItemEvent) -> Unit) {
        // The handler stays in the native object, so the switch is not captured strongly but looked up from anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        val token = inspectable.addEventHandler(
            "WinUI4K.ToggledHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IToggleSwitch_add_Toggled,
        ) { _, _ ->
            (anchor.get() as? WToggleSwitch)?.let { toggleSwitch ->
                onChange(WItemEvent(toggleSwitch, if (toggleSwitch.isOn) WItemEvent.SELECTED else WItemEvent.DESELECTED))
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
        inspectable.removeEventHandler(XamlInterop.IToggleSwitch_remove_Toggled, token)
        listenerAnchor.listenerRemoved()
    }
}
