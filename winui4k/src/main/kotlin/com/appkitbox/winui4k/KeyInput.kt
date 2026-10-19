package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The key listeners (KeyListener-style) of one component, and the native handlers that call them.
 *
 * - keyPressed / keyReleased are PreviewKeyDown / PreviewKeyUp (routed from parent to child, and delivered before the
 *   control's own KeyDown handling). Setting Handled with consume() stops the control's default handling (such as a
 *   button's click on Enter / Space) and keyboard accelerators (such as the default button), just as Swing's consume
 *   stops key bindings
 * - keyTyped is CharacterReceived. A TextBox inserts the character before the handler runs, so consume() cannot stop
 *   the input
 * - All of them are called only when the focused element is this component itself (or a part of its template); they
 *   are not called while a child has focus
 */
internal class KeyInput(private val owner: WComponent) {
    private val listeners = ArrayList<WKeyListener>()
    private var handlersAdded = false

    fun addKeyListener(listener: WKeyListener) {
        if (!handlersAdded) {
            addHandlers()
            handlersAdded = true
        }
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun removeKeyListener(listener: WKeyListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    fun keyListeners(): Array<WKeyListener> = listeners.toTypedArray()

    private fun addHandlers() {
        val anchor = owner.listenerAnchor
        for ((eventSlot, id) in listOf(
            XamlInterop.IUIElementStatics_get_PreviewKeyDownEvent to WKeyEvent.KEY_PRESSED,
            XamlInterop.IUIElementStatics_get_PreviewKeyUpEvent to WKeyEvent.KEY_RELEASED,
        )) {
            Xaml.addHandlerHandledToo(
                owner,
                eventSlot,
                XamlInterop.IID_KeyEventHandler,
                FoundationInterop.IID_IReference_KeyEventHandler,
            ) { _, args -> anchor.get()?.input?.keys?.onKey(id, args) }
        }
        Xaml.addHandlerHandledToo(
            owner,
            XamlInterop.IUIElementStatics_get_CharacterReceivedEvent,
            XamlInterop.IID_CharacterReceivedHandler,
            FoundationInterop.IID_IReference_CharacterReceivedHandler,
        ) { _, args -> anchor.get()?.input?.keys?.onCharacter(args) }
    }

    private fun onKey(id: Int, args: Ptr) {
        val event = XamlKeyEvent(ComPtr(args))
        if (id == WKeyEvent.KEY_PRESSED) ConsumedKeyPresses.clear()
        if (listeners.isEmpty() || !isFromOwnPart(args)) return
        val key = event.key
        val keyEvent = WKeyEvent(
            owner,
            id,
            System.currentTimeMillis(),
            WInputEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()),
            WKeyEvent.keyCodeOf(key),
            WKeyEvent.CHAR_UNDEFINED,
            virtualKeyOf(key),
        )
        keyEvent.onConsume = {
            event.handled = true
            if (id == WKeyEvent.KEY_PRESSED) ConsumedKeyPresses.mark(owner, key)
        }
        try {
            for (listener in listeners.toList()) {
                if (id == WKeyEvent.KEY_PRESSED) listener.keyPressed(keyEvent) else listener.keyReleased(keyEvent)
            }
        } finally {
            keyEvent.onConsume = null
        }
    }

    private fun onCharacter(args: Ptr) {
        if (listeners.isEmpty() || !isFromOwnPart(args)) return
        val character = ComPtr(args)
        val code = character.getInt(XamlInterop.ICharacterReceivedRoutedEventArgs_get_Character) and CHAR16_MASK
        // AWT's KEY_TYPED for Enter is '\n' (Windows' WM_CHAR is '\r')
        val typed = if (code == CARRIAGE_RETURN) '\n' else code.toChar()
        val keyEvent = WKeyEvent(
            owner,
            WKeyEvent.KEY_TYPED,
            System.currentTimeMillis(),
            WInputEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()),
            WKeyEvent.VK_UNDEFINED,
            typed,
        )
        keyEvent.onConsume = { character.putBool(XamlInterop.ICharacterReceivedRoutedEventArgs_put_Handled, true) }
        try {
            for (listener in listeners.toList()) listener.keyTyped(keyEvent)
        } finally {
            keyEvent.onConsume = null
        }
    }

    private fun isFromOwnPart(args: Ptr): Boolean {
        val source = Xaml.originalSource(args) ?: return false
        return try {
            isOwnPart(owner, source)
        } finally {
            source.release()
        }
    }

    private companion object {
        const val CHAR16_MASK = 0xFFFF
        const val CARRIAGE_RETURN = 13
    }
}

/**
 * Key presses consumed in keyPressed (used only on the UI thread). WinUI4K's own handling that runs on KeyDown (the
 * ActionListener on Enter in WTextField and the default button) uses this to ignore consumed keys, as Swing does.
 * PreviewKeyDown arrives before KeyDown, so the KeyDown of a marked press calls [take]. The mark is cleared by the
 * PreviewKeyDown of the next press.
 */
internal object ConsumedKeyPresses {
    /** The marked element (the address of its IUnknown; no reference is held) and key. */
    private var element = 0L
    private var key = 0

    fun mark(owner: WComponent, key: Int) {
        element = identityOf(owner.inspectable)
        this.key = key
    }

    fun clear() {
        element = 0L
        key = 0
    }

    /** If the press of [key] on [target] is marked, clears the mark and returns true. */
    fun take(target: ComPtr, key: Int): Boolean {
        if (element == 0L || this.key != key || identityOf(target) != element) return false
        clear()
        return true
    }

    private fun identityOf(target: ComPtr): Long {
        val unknown = target.queryInterface(IID_IUNKNOWN)
        return try {
            unknown.ptr.address
        } finally {
            unknown.release()
        }
    }

    private const val IID_IUNKNOWN = "00000000-0000-0000-c000-000000000046"
}

/** The [VirtualKey] for a Windows virtual-key value; [VirtualKey.NONE] if the value is not in the winmd. */
internal fun virtualKeyOf(native: Int): VirtualKey = VirtualKey.entries.firstOrNull { it.native == native } ?: VirtualKey.NONE
