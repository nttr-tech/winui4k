package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

/**
 * A shared part combining the ActionListener-style listener list of single-line text fields ([WTextField] and
 * [WPasswordField]) and the handling of KeyDown for Enter. As with JTextField, Enter without modifier keys calls the
 * listeners if there are any, and otherwise presses the default button of the same window ([WFrame.defaultButton]).
 *
 * - [actionCommand]: the string to use as [WActionEvent.actionCommand] of the event (the text field's actionCommand
 *   property)
 * - [multiLine]: whether the source of the Enter event (the native text field; borrowed, so it is not released) is a
 *   multi-line field that inputs line breaks. If it is multi-line, Enter becomes a line break and neither the listeners
 *   nor the default button are triggered. It must not capture the text field's wrapper (because the handler stays in
 *   the native object, it would form a cycle for the same reason as [ListenerAnchor])
 *
 * The text field calls [install] in init. Listeners are added and removed with [add] / [remove], and while there are
 * listeners the text field is kept alive by [WComponent.listenerAnchor].
 */
internal class ActionEvents(
    private val owner: WComponent,
    private val actionCommand: () -> String?,
    private val multiLine: (ComPtr) -> Boolean,
) {
    /** Registered listeners (the registered object and the function that receives the event). Called in registration order. */
    private val listeners = ArrayDeque<Pair<Any, (WActionEvent) -> Unit>>()

    val hasListeners: Boolean get() = listeners.isNotEmpty()

    /**
     * Subscribes to KeyDown for Enter. TextBox passes Enter's KeyDown to its text engine and marks it handled even when
     * single-line (OnKeyDown in TextBoxBase.cpp), so the default button's global accelerator does not fire. It is marked
     * handled after the element's handlers (measured with 2.5.4-experimental), but to avoid relying on the order, the
     * subscription also receives handled events.
     */
    fun install() {
        val target = EnterKeyTarget(WeakReference(this), multiLine)
        Xaml.addHandlerHandledToo(
            owner,
            XamlInterop.IUIElementStatics_get_KeyDownEvent,
            XamlInterop.IID_KeyEventHandler,
            FoundationInterop.IID_IReference_KeyEventHandler,
        ) { sender, args -> target.onKeyDown(sender, XamlKeyEvent(ComPtr(args))) }
    }

    fun add(key: Any, invoke: (WActionEvent) -> Unit) {
        listeners.addLast(key to invoke)
        owner.listenerAnchor.listenerAdded()
    }

    fun remove(key: Any) {
        val index = listeners.indexOfLast { it.first === key }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    /** Calls the registered listeners in registration order, as when Enter is pressed. Does not press the default button. */
    fun post() {
        val event = WActionEvent(owner, actionCommand(), WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()))
        for ((_, invoke) in listeners.toList()) invoke(event)
    }
}

/**
 * Handling of KeyDown for Enter in a text field. Called from the handler attached to the native text field, and gets
 * the listener list from [actions] (a text field with listeners is kept by [WComponent.listenerAnchor], so the list is
 * collected only for text fields without listeners; even then the default button is pressed).
 */
private class EnterKeyTarget(
    private val actions: WeakReference<ActionEvents>,
    private val multiLine: (ComPtr) -> Boolean,
) {
    /**
     * On Enter without modifier keys, if [sender] (TextBox / PasswordBox) is single-line, calls the listeners or presses
     * the default button and marks the event handled. Because it is marked handled, the default button's Enter
     * accelerator does not press it a second time.
     */
    fun onKeyDown(sender: Ptr, event: XamlKeyEvent) {
        if (event.key != VirtualKey.ENTER.native || Xaml.currentModifiers() != 0) return
        val senderPtr = ComPtr(sender) // borrowed (not released)
        // Does nothing for an Enter consumed in a KeyListener's keyPressed (in Swing, too, bindings of a consumed key do not run)
        if (ConsumedKeyPresses.take(senderPtr, VirtualKey.ENTER.native)) return
        if (multiLine(senderPtr)) return
        val events = actions.get()
        if (events != null && events.hasListeners) {
            event.handled = true
            events.post()
            return
        }
        val button = DefaultButtons.inRootOf(senderPtr) ?: return
        if (button.isEnabled) {
            event.handled = true
            button.doClick()
        }
    }
}
