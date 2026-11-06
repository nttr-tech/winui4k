package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The part that receives changes of a button's ([WButtonBase]) state (what corresponds to pressed / rollover / selected / enabled of Swing's ButtonModel).
 * Registers, only once when first needed ([ensureRegistered]), the dependency property change callbacks (DependencyObject.RegisterPropertyChangedCallback) of
 * ButtonBase.IsPressed / IsPointerOver, Control.IsEnabled and, for buttons with a selection state ([WToggleButton] and its subclasses),
 * ToggleButton.IsChecked. The callbacks are called only when a value changes, both for changes from the program and
 * for user operations. However, IsEnabled also gets notifications without a value change (twice when set to false, once even when set to the same value), so
 * the four states are compared together with the previous values, and [WButtonBase.onButtonStateChanged] is called only when they changed.
 *
 * The callbacks stay in native code, so the button is not captured strongly but looked up from [WComponent.listenerAnchor] ([ListenerAnchor]).
 * Used only from the UI thread.
 */
internal class ButtonStateChanges(private val owner: WButtonBase, private val hasSelection: Boolean) {
    private var registered = false

    /** The last notified state (starts from the state at the time of [ensureRegistered]). */
    private var last: State? = null

    fun ensureRegistered() {
        if (registered) return
        registered = true
        last = current()
        val anchor = owner.listenerAnchor
        val properties = if (hasSelection) STATE_PROPERTIES + isCheckedProperty else STATE_PROPERTIES
        for (property in properties) {
            TableViewXaml.registerPropertyChangedCallback(owner.dependencyObject, property) { _ ->
                (anchor.get() as? WButtonBase)?.let { button -> if (button.stateChanges.changed()) button.onButtonStateChanged() }
            }
        }
    }

    /** If the current state differs from the previous one, remembers it and returns true. */
    fun changed(): Boolean {
        val now = current()
        if (now == last) return false
        last = now
        return true
    }

    private fun current(): State = State(
        owner.isPressed,
        owner.isPointerOver,
        owner.isEnabled,
        (owner as? WToggleButton)?.isChecked,
    )

    private data class State(val pressed: Boolean, val pointerOver: Boolean, val enabled: Boolean, val checked: Boolean?)

    private companion object {
        /** ButtonBase.IsPressedProperty and IsPointerOverProperty and Control.IsEnabledProperty (used for the life of the process, so not released). */
        val STATE_PROPERTIES: List<ComPtr> by lazy {
            listOf(
                staticProperty(XamlInterop.CLS_ButtonBase, XamlInterop.IID_IButtonBaseStatics, XamlInterop.IButtonBaseStatics_get_IsPressedProperty),
                staticProperty(
                    XamlInterop.CLS_ButtonBase,
                    XamlInterop.IID_IButtonBaseStatics,
                    XamlInterop.IButtonBaseStatics_get_IsPointerOverProperty,
                ),
                staticProperty(XamlInterop.CLS_Control, XamlInterop.IID_IControlStatics, XamlInterop.IControlStatics_get_IsEnabledProperty),
            )
        }

        /** ToggleButton.IsCheckedProperty. */
        val isCheckedProperty: ComPtr by lazy {
            staticProperty(XamlInterop.CLS_ToggleButton, XamlInterop.IID_IToggleButtonStatics, XamlInterop.IToggleButtonStatics_get_IsCheckedProperty)
        }

        fun staticProperty(className: String, staticsIid: String, slot: Int): ComPtr {
            val statics = Activation.factory(className, staticsIid)
            return try {
                statics.getPtr(slot)
            } finally {
                statics.release()
            }
        }
    }
}
