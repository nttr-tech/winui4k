package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A [WSplitButton] that toggles on/off: WinUI 3's ToggleSplitButton.
 * Clicking the body flips [isChecked]; the right-side arrow opens the flyout.
 * [doClick] works like clicking the body: it toggles the checked state and calls ItemListener and ActionListener once each
 * (it does nothing before being shown in a window; see [WSplitButton.doClick] for why).
 */
class WToggleSplitButton @JvmOverloads constructor(text: String = "") : WSplitButton(
    Activation.composeDefault(XamlInterop.CLS_ToggleSplitButton, XamlInterop.IID_IToggleSplitButtonFactory),
) {
    /** The IToggleSplitButton view holding IsChecked / IsCheckedChanged. */
    private val toggleSplitButton: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IToggleSplitButton))
    }

    /** The list of ItemListeners. Only one IsCheckedChanged handler is registered, when the first listener is added. */
    private val itemListeners = ItemListeners(this) { addIsCheckedChangedHandler() }

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
        itemListeners.addAdapter(listener, WItemListener { listener(it.stateChange == WItemEvent.SELECTED) })
    }

    /**
     * The legacy `addItemListener(Consumer<Boolean>)` for Java. It is kept only for binary compatibility (linking of
     * already compiled callers) and is not visible from Java source (HIDDEN). In Java, pass a [WItemListener] instead.
     */
    @Deprecated("Use the addItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("addItemListener")
    fun addItemListenerForJava(listener: Consumer<Boolean>) {
        itemListeners.addAdapter(listener, WItemListener { listener.accept(it.stateChange == WItemEvent.SELECTED) })
    }

    /**
     * ItemListener.itemStateChanged-like: receives a [WItemEvent] every time the checked state changes.
     * One event per change is passed to all listeners, so even if an earlier listener changes [isChecked], later listeners
     * receive the state of that change (as in Swing).
     */
    fun addItemListener(listener: WItemListener) {
        itemListeners.add(listener)
    }

    private fun addIsCheckedChangedHandler() {
        // The handler stays on the native object, so it does not capture the button strongly but looks it up from anchor
        // (see ListenerAnchor)
        val anchor = listenerAnchor
        toggleSplitButton.addEventHandler(
            "WinUI4K.ToggleSplitButtonHandler",
            XamlInterop.IID_ToggleSplitButtonIsCheckedChangedHandler,
            XamlInterop.IToggleSplitButton_add_IsCheckedChanged,
        ) { _, _ -> (anchor.get() as? WToggleSplitButton)?.onCheckedChanged() }
    }

    /**
     * Dispatches one IsCheckedChanged in the same order as Swing's JToggleButton: after the user's ItemListeners (in the order added), if [action]
     * has a SELECTED_KEY value, it is rewritten only when different (the same as [WToggleButton]).
     */
    private fun onCheckedChanged() {
        val selected = isChecked
        itemListeners.fire(WItemEvent(this, if (selected) WItemEvent.SELECTED else WItemEvent.DESELECTED))
        val action = action
        val current = action?.getValue(WAction.SELECTED_KEY)
        if (action != null && current != null && current != selected) action.putValue(WAction.SELECTED_KEY, selected)
    }

    /** Matches the checked state to the action's SELECTED_KEY (false if absent). [IllegalArgumentException] if it is not a Boolean. */
    override fun applyActionSelected(action: WAction?) {
        isChecked = action?.typedValue<Boolean>(WAction.SELECTED_KEY, "Boolean") == true
    }

    /** Whether it is bound to an action. While bound, it counts as one listener for writing back the checked state. */
    private var actionBound = false

    /** Receives checked state notifications only while an action is bound (to write it back). */
    override fun onActionChanged(action: WAction?) {
        val bound = action != null
        if (bound == actionBound) return
        actionBound = bound
        if (bound) {
            itemListeners.ensureNativeHandler()
            listenerAnchor.listenerAdded()
        } else {
            listenerAnchor.listenerRemoved()
        }
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    @JvmSynthetic
    fun removeItemListener(listener: (Boolean) -> Unit) {
        itemListeners.removeAdapter(listener)
    }

    /** The legacy `removeItemListener(Consumer<Boolean>)` (not visible from Java source), for the same reason as [addItemListenerForJava]. */
    @Deprecated("Use the removeItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("removeItemListener")
    fun removeItemListenerForJava(listener: Consumer<Boolean>) {
        itemListeners.removeAdapter(listener)
    }

    /** Unsubscribes a [WItemListener] registered with [addItemListener]. */
    fun removeItemListener(listener: WItemListener) {
        itemListeners.remove(listener)
    }

    /** AbstractButton.getItemListeners-like: the ItemListeners currently added (in the order added; Kotlin lambdas appear wrapped). */
    fun getItemListeners(): Array<WItemListener> = itemListeners.toArray()

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
