package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A two-state on/off switch: WinUI 3's ToggleSwitch.
 * Subscribe to changes in [isOn] via [addItemListener] (Toggled).
 * Customize the displayed text with [header] / [onContent] / [offContent] (all read back from WinUI).
 * [isSelected] / [doClick] / [mnemonic] / [text] / [actionCommand] / [addActionListener] are aliases and operations matching JToggleButton
 * (AbstractButton).
 *
 * Differences from Swing: it is not derived from AbstractButton and cannot be put in a [WButtonGroup] (a switch is a component toggled independently one by one).
 * It has no icon ([WButtonBase.icon]) or Action ([WButtonBase.action]).
 */
class WToggleSwitch @JvmOverloads constructor(header: String = "") : WControl(
    Activation.activate(XamlInterop.CLS_ToggleSwitch, XamlInterop.IID_IToggleSwitch), // created via the default factory
) {
    /** The list of ItemListeners. Only one Toggled handler is registered, when the first listener is added. */
    private val itemListeners = ItemListeners(this) { ensureToggledHandler() }

    /** The list of ActionListeners. The Toggled handler is shared with [itemListeners]. */
    private val actionListenerList = AnchoredListeners<WActionListener>(this) { ensureToggledHandler() }

    /** Whether the Toggled handler has been registered. */
    private var toggledHandlerAdded = false

    /** Whether the program is in the middle of changing [isOn] (ActionListeners are not called for Toggled in the meantime). */
    private var settingProgrammatically = false

    /**
     * Whether the switch is on (ToggleSwitch.IsOn). Changing it from the program calls ItemListeners but not ActionListeners
     * (the same as Swing's setSelected; ToggleSwitch raises Toggled synchronously within the IsOn change).
     */
    var isOn: Boolean
        get() = inspectable.getBool(XamlInterop.IToggleSwitch_get_IsOn)
        set(value) {
            val was = settingProgrammatically
            settingProgrammatically = true
            try {
                inspectable.putBool(XamlInterop.IToggleSwitch_put_IsOn, value)
            } finally {
                settingProgrammatically = was
            }
        }

    /** AbstractButton.getText / setText-like: an alias of [header] (the text above the switch; corresponds to the text of a JToggleButton). */
    var text: String
        get() = header
        set(value) {
            header = value
        }

    /**
     * AbstractButton.getActionCommand-like: the [WActionEvent.actionCommand] passed to the [addActionListener] listeners.
     * While unset (null), returns [text]. Setting null returns it to unset.
     */
    var actionCommand: String? = null
        get() = field ?: text

    /** AbstractButton.isSelected-like: same as [isOn]. */
    var isSelected: Boolean
        get() = isOn
        set(value) {
            isOn = value
        }

    /**
     * The heading above the switch (ToggleSwitch.Header). It is of type Object, so a boxed string is passed and unboxed
     * when read. Returns "" for non-strings (such as elements set on the XAML side) or when unset.
     */
    var header: String
        get() = readText(XamlInterop.IToggleSwitch_get_Header)
        set(value) = writeText(XamlInterop.IToggleSwitch_put_Header, value)

    /** The text shown in the on state (ToggleSwitch.OnContent). Defaults to text WinUI sets for the display language (e.g. "On" in English, and the localized word in other display languages). */
    var onContent: String
        get() = readText(XamlInterop.IToggleSwitch_get_OnContent)
        set(value) = writeText(XamlInterop.IToggleSwitch_put_OnContent, value)

    /** The text shown in the off state (ToggleSwitch.OffContent). Defaults to text WinUI sets for the display language (e.g. "Off" in English, and the localized word in other display languages). */
    var offContent: String
        get() = readText(XamlInterop.IToggleSwitch_get_OffContent)
        set(value) = writeText(XamlInterop.IToggleSwitch_put_OffContent, value)

    private fun readText(getSlot: Int): String {
        val boxed = inspectable.getPtrOrNull(getSlot) ?: return ""
        return try {
            PropertyValues.unboxString(boxed) ?: ""
        } finally {
            boxed.release()
        }
    }

    private fun writeText(putSlot: Int, value: String) {
        val boxed = PropertyValues.boxString(value)
        try {
            inspectable.call(putSlot, boxed.ptr)
        } finally {
            boxed.release()
        }
    }

    /**
     * AbstractButton.getMnemonic / setMnemonic(int)-like: the key code of the access key (UIElement.AccessKey).
     * Same values as AWT's KeyEvent.VK_A..VK_Z (65..90) / VK_0..VK_9 (48..57); setting 0 clears it (and reads back 0).
     * Anything other than letters and digits throws [IllegalArgumentException]. It is the key tip shown when Alt is pressed,
     * and pressing that key toggles the switch.
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /** AbstractButton.setMnemonic(char)-like: sets the access key by character. Letters are converted to uppercase, and '\u0000' clears it. */
    fun setMnemonic(mnemonic: Char) {
        this.mnemonic = Mnemonics.keyCodeOf(mnemonic)
    }

    /**
     * AbstractButton.doClick-like: clicks the switch once programmatically to toggle it on or off.
     * Implemented with UI Automation's Toggle, so ItemListeners ([addItemListener]) and then ActionListeners ([addActionListener]) run in order,
     * exactly like a normal click. Does nothing when disabled ([isEnabled] is false), as in Swing.
     */
    fun doClick() {
        if (!isEnabled) return
        Xaml.invoke(inspectable)
    }

    /** AbstractButton.doClick(int pressTime)-like: the same as [doClick] ([pressTime] is not used, for the same reason as [WButtonBase.doClick]). */
    @Suppress("UNUSED_PARAMETER")
    fun doClick(pressTime: Int) {
        doClick()
    }

    /** JToggleButton(String, boolean)-like: creates a switch with the header [header], on if [selected]. */
    constructor(header: String, selected: Boolean) : this(header) {
        isOn = selected
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
     * ItemListener.itemStateChanged-like: receives a [WItemEvent] every time it toggles on/off (on is SELECTED, off is
     * DESELECTED).
     * One event per toggle is passed to all listeners, so even if an earlier listener changes [isOn], later listeners
     * receive the state of that toggle (as in Swing).
     */
    fun addItemListener(listener: WItemListener) {
        itemListeners.add(listener)
    }

    private fun ensureToggledHandler() {
        if (toggledHandlerAdded) return
        toggledHandlerAdded = true
        // The handler stays in the native object, so the switch is not captured strongly but looked up from anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.ToggledHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IToggleSwitch_add_Toggled,
        ) { _, _ -> (anchor.get() as? WToggleSwitch)?.onToggled() }
    }

    /** Toggled. Calls the ItemListeners, and the ActionListeners only for user operations and [doClick]. */
    private fun onToggled() {
        val state = if (isOn) WItemEvent.SELECTED else WItemEvent.DESELECTED
        itemListeners.fire(WItemEvent(this, state))
        if (settingProgrammatically || actionListenerList.isEmpty()) return
        val event = WActionEvent(this, actionCommand, WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()))
        for (listener in actionListenerList.toList()) listener.actionPerformed(event)
    }

    /**
     * ActionListener-like: receives a [WActionEvent] when the user toggles the switch (click, keys, UI Automation) and on [doClick]
     * (the source is this switch and [WActionEvent.actionCommand] is [actionCommand]). Not called when [isOn] / [isSelected] is changed from the program
     * (the same as Swing). Called in the order added, after the ItemListeners for the same toggle.
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) {
        actionListenerList.addAdapter(listener, WActionListener(listener))
    }

    /** ActionListener-like: receives toggles with a [WActionListener] (the same as the Kotlin lambda form). */
    fun addActionListener(listener: WActionListener) {
        actionListenerList.add(listener)
    }

    /** Removes one lambda added with [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) {
        actionListenerList.removeAdapter(listener)
    }

    /** Removes one [WActionListener] added with [addActionListener]. Does nothing if it is not attached. */
    fun removeActionListener(listener: WActionListener) {
        actionListenerList.remove(listener)
    }

    /** AbstractButton.getActionListeners-like: the ActionListeners currently attached (in the order added; Kotlin lambdas appear wrapped). */
    val actionListeners: Array<WActionListener>
        get() = actionListenerList.toList().toTypedArray()

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
}
