package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The key bindings of one component (like JComponent's InputMap / ActionMap) and the native handlers that drive them.
 *
 * WinUI's KeyboardAccelerator is not used (measured in doc/verification.md: unless marked handled it presses the owner button and searches no further,
 * it cannot be used for key releases or character input, the usable keys are limited, and it shows key tips on the owner). Instead, they are looked up with the routed key events
 * in the same order as Swing's JComponent.processKeyBindings:
 * 1. [WComponent.WHEN_FOCUSED]: in PreviewKeyDown / PreviewKeyUp / CharacterReceived of the focused component itself,
 *    after the key listeners ([KeyInput]) (not looked up if a listener consumed it). This comes before the control's own key handling (KeyDown), so,
 *    just as a Swing user's bindings replace the UI's default bindings, it takes precedence over a button's Space etc.
 * 2. [WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT]: in the bubbling of KeyDown / KeyUp / CharacterReceived (from the focus to the root),
 *    looked up for each component on the way (including the focused component itself). After the control's own key handling
 * 3. [WComponent.WHEN_IN_FOCUSED_WINDOW]: in the bubbling at the window content (XamlRoot.Content), looking up those of components in the same
 *    window. Before WinUI's global accelerators (such as menu item accelerators)
 *
 * No stage looks up keys that are already handled (Handled). When an action runs, the key is marked handled, stopping the later stages and WinUI's default handling.
 * If the action is disabled ([WAction.isEnabled] is false), the key is not consumed and the next stage is tried (the same as Swing).
 *
 * The handlers are registered only once, when the first map is created, and are not removed (they do nothing if the maps are empty). None of the handlers
 * captures the component or this object strongly; they look up the component from [WComponent.listenerAnchor].
 * While shown, the wrapper is held strongly ([ListenerAnchor.listenerAddedFromFirstLoaded]).
 */
internal class KeyBindings(private val owner: WComponent) {
    private val inputMaps = arrayOfNulls<WInputMap>(CONDITION_COUNT)
    private var actions: WActionMap? = null
    private var handlersAdded = false
    private var windowHandlersRequested = false

    /** The window content (IWeakReference) to which the WHEN_IN_FOCUSED_WINDOW handlers were attached. Used to avoid attaching them again. */
    private var windowContent: ComPtr? = null

    /** The mapping for [condition]. If absent, created only when [create]. [IllegalArgumentException] if the condition is none of the three. */
    fun inputMap(condition: Int, create: Boolean): WInputMap? {
        require(condition in 0 until CONDITION_COUNT) {
            "condition must be one of WComponent.WHEN_IN_FOCUSED_WINDOW, WComponent.WHEN_FOCUSED or " +
                "WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT: $condition"
        }
        inputMaps[condition]?.let { return it }
        if (!create) return null
        val map = WInputMap()
        inputMaps[condition] = map
        ensureHandlers()
        if (condition == WComponent.WHEN_IN_FOCUSED_WINDOW) ensureWindowHandlers()
        return map
    }

    /** The action mapping. If absent, created only when [create]. */
    fun actionMap(create: Boolean): WActionMap? {
        actions?.let { return it }
        if (!create) return null
        return WActionMap().also {
            actions = it
            ensureHandlers()
        }
    }

    /** The same as JComponent.registerKeyboardAction: maps [stroke] to an action wrapping [listener]. */
    fun register(listener: WActionListener?, command: String?, stroke: WKeyStroke?, condition: Int) {
        val map = checkNotNull(inputMap(condition, true))
        val standin = KeyboardActionStandin(listener, command)
        map.put(stroke, standin)
        checkNotNull(actionMap(true)).put(standin, standin)
    }

    /** The same as JComponent.unregisterKeyboardAction: removes the mappings of [stroke] in the three conditions and the actions they point to. */
    fun unregister(stroke: WKeyStroke?) {
        for (map in inputMaps) {
            if (map == null) continue
            val key = map[stroke]
            if (key != null) actions?.remove(key)
            map.remove(stroke)
        }
    }

    /** The same as JComponent.getRegisteredKeyStrokes: the keystrokes of the mappings of the three conditions (including parents), in condition order. */
    fun registeredKeyStrokes(): Array<WKeyStroke> = inputMaps.flatMap { it?.allKeys()?.asList().orEmpty() }.toTypedArray()

    /** The same as JComponent.getConditionForKeyStroke: the first condition that maps [stroke]. [WComponent.UNDEFINED_CONDITION] if none. */
    fun conditionFor(stroke: WKeyStroke?): Int {
        val index = inputMaps.indexOfFirst { it?.get(stroke) != null }
        return if (index < 0) WComponent.UNDEFINED_CONDITION else index
    }

    /** The same as JComponent.resetKeyboardActions: empties the mappings of the three conditions and the action mapping (parents are not removed). */
    fun reset() {
        inputMaps.forEach { it?.clear() }
        actions?.clear()
    }

    /**
     * Looks up the keystroke of [event] in the mapping of [condition] and calls the action (the same as JComponent.processKeyBinding).
     * false (the key is not consumed) when the component is disabled, when there is no mapping or action, or when the action is disabled.
     */
    fun process(condition: Int, event: WKeyEvent): Boolean {
        val map = inputMaps[condition] ?: return false
        val actionMap = actions ?: return false
        if ((owner as? WControl)?.isEnabled == false) return false
        val binding = map[strokeOf(event)] ?: return false
        val action = actionMap[binding] ?: return false
        return notifyAction(action, event)
    }

    /** The same as SwingUtilities.notifyAction: calls the enabled [action] with a [WActionEvent] whose source is this component. */
    private fun notifyAction(action: WAction, event: WKeyEvent): Boolean {
        if (!action.isEnabled) return false
        val commandValue = action.getValue(WAction.ACTION_COMMAND_KEY)
        // Without ACTION_COMMAND_KEY, the key's character (present only for character input). If null was passed to registerKeyboardAction, it stays null
        val command = when {
            commandValue != null -> commandValue.toString()
            action is KeyboardActionStandin -> null
            event.keyChar != WKeyEvent.CHAR_UNDEFINED -> event.keyChar.toString()
            else -> null
        }
        action.actionPerformed(WActionEvent(owner, command, event.modifiers, event.`when`))
        return true
    }

    private fun ensureHandlers() {
        if (handlersAdded) return
        handlersAdded = true
        owner.input.keys.ensureHandlers() // WHEN_FOCUSED is looked up from the key listener handlers
        val anchor = owner.listenerAnchor
        addKeyHandlers(owner.uiElement) { id, args, _ -> anchor.get()?.keyBindingsOrNull?.onAncestorKey(id, args) }
        anchor.listenerAddedFromFirstLoaded()
    }

    /** Attaches the WHEN_IN_FOCUSED_WINDOW handlers to the content of the window it is placed in (checked every time it is placed again). */
    private fun ensureWindowHandlers() {
        if (windowHandlersRequested) return
        windowHandlersRequested = true
        val anchor = owner.listenerAnchor
        owner.frameworkElement.addEventHandler(
            "WinUI4K.KeyBindingsLoaded",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> anchor.get()?.keyBindingsOrNull?.attachToWindow() }
        attachToWindow()
    }

    /** Attaches the handlers to the current window content if not attached yet. Does nothing if not placed. */
    private fun attachToWindow() {
        val content = windowContentOf(owner) ?: return
        try {
            val previous = windowContent?.getPtrOrNull(RESOLVE, Guid.of(XamlInterop.IID_IUIElement))
            val attached = try {
                previous != null && Xaml.sameObject(previous, content)
            } finally {
                previous?.release()
            }
            if (attached) return
            val anchor = owner.listenerAnchor
            addKeyHandlers(content) { id, args, sender -> anchor.get()?.keyBindingsOrNull?.onWindowKey(id, args, sender) }
            windowContent?.release()
            windowContent = weakReferenceOf(content)
        } finally {
            content.release()
        }
    }

    /** Attaches bubbling KeyDown / KeyUp / CharacterReceived (handled ones also arrive) to [element] (IUIElement). */
    private fun addKeyHandlers(element: ComPtr, handler: (id: Int, args: Ptr, sender: Ptr) -> Unit) {
        for ((eventSlot, id) in listOf(
            XamlInterop.IUIElementStatics_get_KeyDownEvent to WKeyEvent.KEY_PRESSED,
            XamlInterop.IUIElementStatics_get_KeyUpEvent to WKeyEvent.KEY_RELEASED,
        )) {
            Xaml.addHandlerHandledToo(
                element,
                eventSlot,
                XamlInterop.IID_KeyEventHandler,
                FoundationInterop.IID_IReference_KeyEventHandler,
            ) { sender, args -> handler(id, args, sender) }
        }
        Xaml.addHandlerHandledToo(
            element,
            XamlInterop.IUIElementStatics_get_CharacterReceivedEvent,
            XamlInterop.IID_CharacterReceivedHandler,
            FoundationInterop.IID_IReference_CharacterReceivedHandler,
        ) { sender, args -> handler(WKeyEvent.KEY_TYPED, args, sender) }
    }

    private fun onAncestorKey(id: Int, args: Ptr) {
        val map = inputMaps[WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT] ?: return
        if (map.size() == 0 && map.parent == null) return
        dispatchRouted(id, args, WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    }

    private fun onWindowKey(id: Int, args: Ptr, sender: Ptr) {
        val map = inputMaps[WComponent.WHEN_IN_FOCUSED_WINDOW] ?: return
        if (map.size() == 0 && map.parent == null) return
        // Do not look up for old handlers of a component that moved to another window, or for components not on screen (the same as Swing)
        val content = windowContentOf(owner) ?: return
        val sameWindow = try {
            Xaml.sameObject(content, ComPtr(sender))
        } finally {
            content.release()
        }
        if (!sameWindow || !owner.isShowing) return
        dispatchRouted(id, args, WComponent.WHEN_IN_FOCUSED_WINDOW)
    }

    /** If the routed event's [args] is not handled yet, looks up the mapping of [condition], and marks it handled if an action ran. */
    private fun dispatchRouted(id: Int, args: Ptr, condition: Int) {
        if (id == WKeyEvent.KEY_TYPED) {
            val character = ComPtr(args)
            if (character.getBool(XamlInterop.ICharacterReceivedRoutedEventArgs_get_Handled)) return
            val code = character.getInt(XamlInterop.ICharacterReceivedRoutedEventArgs_get_Character)
            if (process(condition, typedEvent(owner, code))) {
                character.putBool(XamlInterop.ICharacterReceivedRoutedEventArgs_put_Handled, true)
            }
        } else {
            val key = XamlKeyEvent(ComPtr(args))
            if (key.handled) return
            if (process(condition, keyEvent(owner, id, key.key))) key.handled = true
        }
    }

    companion object {
        private const val CONDITION_COUNT = 3

        /** IWeakReferenceSource (weakreference.h). GetWeakReference(out IWeakReference) is vtbl[3]. */
        private const val IID_IWEAK_REFERENCE_SOURCE = "00000038-0000-0000-c000-000000000046"
        private const val GET_WEAK_REFERENCE = 3

        /** IWeakReference.Resolve(REFIID, out IInspectable) is vtbl[3]. Returns null if already destroyed. */
        private const val RESOLVE = 3
        private const val CHAR16_MASK = 0xFFFF
        private const val CARRIAGE_RETURN = 13

        /** The same keystroke as Swing's processKeyBindings: character input is only the character (modifiers are ignored); others are the key, modifiers, and pressed or released. */
        fun strokeOf(event: WKeyEvent): WKeyStroke = if (event.id == WKeyEvent.KEY_TYPED) {
            WKeyStroke.getKeyStroke(event.keyChar)
        } else {
            WKeyStroke.getKeyStroke(event.keyCode, event.modifiersEx, event.id == WKeyEvent.KEY_RELEASED)
        }

        /** A press ([WKeyEvent.KEY_PRESSED]) or release ([WKeyEvent.KEY_RELEASED]) event of the Windows virtual key [key] (the same form as [KeyInput]). */
        fun keyEvent(source: WComponent, id: Int, key: Int): WKeyEvent = WKeyEvent(
            source,
            id,
            System.currentTimeMillis(),
            WInputEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()),
            WKeyEvent.keyCodeOf(key),
            WKeyEvent.CHAR_UNDEFINED,
            virtualKeyOf(key),
        )

        /** An input event of the CharacterReceived character [code] (as in [KeyInput], Enter is '\n'). */
        fun typedEvent(source: WComponent, code: Int): WKeyEvent {
            val char16 = code and CHAR16_MASK
            return WKeyEvent(
                source,
                WKeyEvent.KEY_TYPED,
                System.currentTimeMillis(),
                WInputEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()),
                WKeyEvent.VK_UNDEFINED,
                if (char16 == CARRIAGE_RETURN) '\n' else char16.toChar(),
            )
        }

        /** The IWeakReference of [target]. The caller owns the returned reference. */
        private fun weakReferenceOf(target: ComPtr): ComPtr {
            val source = target.queryInterface(IID_IWEAK_REFERENCE_SOURCE)
            return try {
                source.getPtr(GET_WEAK_REFERENCE)
            } finally {
                source.release()
            }
        }

        /** The window content (XamlRoot.Content) [component] is placed in. null if not placed. The caller owns the returned reference. */
        private fun windowContentOf(component: WComponent): ComPtr? {
            val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
            return try {
                root.getPtrOrNull(XamlInterop.IXamlRoot_get_Content)
            } finally {
                root.release()
            }
        }
    }
}

/**
 * The same as JComponent.ActionStandin: a wrapper that puts the [listener] of [WComponent.registerKeyboardAction] into a [WActionMap] as a [WAction].
 * ACTION_COMMAND_KEY is [command] (if null, [WActionEvent.actionCommand] is also null). Disabled if [listener] is null
 * (as in Swing, the mapping is kept but does not let the parent's mapping be looked up either). If [listener] is also a [WAction], its enabled state and values are used.
 */
internal class KeyboardActionStandin(val listener: WActionListener?, private val command: String?) : WAction {
    private val action = listener as? WAction

    override fun actionPerformed(e: WActionEvent) {
        listener?.actionPerformed(e)
    }

    override fun getValue(key: String): Any? = when (key) {
        WAction.ACTION_COMMAND_KEY -> command
        else -> action?.getValue(key) ?: if (key == WAction.NAME) "ActionStandin" else null
    }

    /** Values cannot be added (the same as Swing). */
    override fun putValue(key: String, value: Any?) = Unit

    override var isEnabled: Boolean
        get() = listener != null && action?.isEnabled != false
        set(value) {
            // The enabled state is decided by the wrapped listener (does nothing, the same as Swing's ActionStandin.setEnabled)
        }

    override fun addPropertyChangeListener(listener: WActionChangeListener) = Unit

    override fun removePropertyChangeListener(listener: WActionChangeListener) = Unit
}
