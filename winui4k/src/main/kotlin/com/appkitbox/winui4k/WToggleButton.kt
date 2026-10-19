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
 * It can be created with the same constructors as Swing's JToggleButton (text, icon, selection state, [WAction]).
 */
open class WToggleButton internal constructor(inspectable: ComPtr) : WButtonBase(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_ToggleButton, XamlInterop.IID_IToggleButtonFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    /** JToggleButton(String, boolean)-style: a button with the specified text and selection state. */
    constructor(text: String, selected: Boolean) : this(text) {
        if (selected) isSelected = true
    }

    /** JToggleButton(Icon)-style: a button with only an icon. */
    constructor(icon: WIcon) : this("") {
        this.icon = icon
    }

    /** JToggleButton(String, Icon)-style: a button with text and an icon. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?) : this(text) {
        if (icon != null) this.icon = icon
    }

    /** JToggleButton(String, Icon, boolean)-style: a button with the specified text, icon and selection state. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?, selected: Boolean) : this(text, icon) {
        if (selected) isSelected = true
    }

    /** JToggleButton(Action)-style: a button that copies the values of [action] (including the SELECTED_KEY selection state) and calls the action on click. */
    constructor(action: WAction) : this("") {
        this.action = action
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

        /** The key in [itemTokens] for the subscription that [WButtonGroup] adds (counted separately from user listeners). */
        val GROUP_KEY = Any()

        /**
         * The [WButtonGroup] subscription. It captures neither the button nor the group (both are looked up from the
         * event source), so no reference extends from the handler left in the native object to the group or other buttons.
         */
        val GROUP_LISTENER: (WItemEvent) -> Unit = { e ->
            val button = e.source as WToggleButton
            button.buttonGroup?.onItemStateChanged(button, e.stateChange)
        }

        /** The key in [itemTokens] for the subscription that writes the selection state back to [action] (counted separately from user listeners). */
        val ACTION_KEY = Any()

        /**
         * The subscription that writes the selection state back to the SELECTED_KEY of [action]. As in Swing's AbstractButton,
         * it rewrites the value only when the action has a SELECTED_KEY value and it differs. Like [GROUP_LISTENER], it
         * captures nothing (the button is looked up from the event source).
         */
        val ACTION_LISTENER: (WItemEvent) -> Unit = { e ->
            val button = e.source as WToggleButton
            val action = button.action
            val current = action?.getValue(WAction.SELECTED_KEY)
            if (action != null && current != null) {
                val selected = button.isSelected
                if (current != selected) action.putValue(WAction.SELECTED_KEY, selected)
            }
        }
    }

    /** Matches the selection state to the action's SELECTED_KEY (false if absent). Throws [IllegalArgumentException] if it is not a Boolean. */
    override fun applyActionSelected(action: WAction?) {
        val selected = action?.typedValue<Boolean>(WAction.SELECTED_KEY, "Boolean") == true
        if (selected != isSelected) isSelected = selected
    }

    /** Holds the subscription that writes the selection state back to the action only while an action is bound. */
    override fun onActionChanged(action: WAction?) {
        unsubscribeItem(ACTION_KEY)
        if (action != null) subscribeItem(ACTION_KEY, ACTION_LISTENER)
    }

    /**
     * The [WButtonGroup] this button belongs to (changed only by [WButtonGroup.add] / [WButtonGroup.remove]).
     * The button holds the group strongly, in the same direction as Swing's ButtonModel holding the group. The group
     * also holds the button strongly, but the cycle stays inside the JVM heap and neither native handler captures them,
     * so once they are removed from the window and no longer referenced, they are collected together.
     */
    internal var buttonGroup: WButtonGroup? = null
        private set

    /** Becomes a member of [group]. Adds an ItemListener (for the group's mutual exclusion), so it is treated as holding one listener. */
    internal fun joinGroup(group: WButtonGroup) {
        check(buttonGroup == null) { "already belongs to another ButtonGroup" }
        buttonGroup = group
        subscribeItem(GROUP_KEY, GROUP_LISTENER)
    }

    /** Stops being a member of the group (removes the ItemListener). */
    internal fun leaveGroup() {
        if (buttonGroup == null) return
        buttonGroup = null
        unsubscribeItem(GROUP_KEY)
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
     * JComponent.getBackground / setBackground-style: the background color. The color for each visual state is put in
     * the class's theme resource keys ([stateColors]), and the color of the normal state is also set as the local value
     * of Control.Background (the same mechanism as [WButton.background]).
     * - [WToggleButton]: paints the unchecked and indeterminate states, at 0.9x opacity on pointer over and 0.8x when
     *   pressed. The checked state keeps the theme's accent color (Swing's JToggleButton also paints the selected state
     *   with the selection color rather than the background color, so it is kept so that being checked stays
     *   distinguishable)
     * - [WCheckBox] / [WRadioButton]: paints the area behind the mark and text (the template's RootGrid) with the same
     *   color in every state: checked, indeterminate, pointer over, pressed and disabled (the same as setBackground of
     *   JCheckBox / JRadioButton painting the component's area; by default WinUI makes every state transparent). The fill
     *   of the mark (the check box square or the radio circle) is not changed
     *
     * The disabled look stays the theme default for [WToggleButton]. Reading returns the set color, or the effective
     * native value (the theme's default color) if unset. Setting null removes the keys and the local value and returns it
     * to the theme default.
     * Changing the color of the current visual state (such as checked) while shown is reflected as is, without changing
     * the state.
     * [WAppBarToggleButton] has no per-visual-state colors and the color affects only the normal state.
     */
    var background: WColor?
        get() = customBackground ?: nativeBackground()
        set(value) {
            customBackground = value
            val keys = stateColors.background
            if (value == null) keys.remove(this) else keys.put(this, value)
            putLocalBackground(value)
        }

    /** The color set to [background]. null means unset (the theme default). */
    private var customBackground: WColor? = null

    /**
     * JComponent.getForeground / setForeground-style: the text color. Like [background], it is also put in the keys for
     * each visual state.
     * [WToggleButton] uses 0.7x opacity only when pressed, in every state including checked and indeterminate (the same
     * as [WButton.foreground]).
     * [WCheckBox] / [WRadioButton] use the same color in every state (WinUI's default does not change the text color when
     * pressed either).
     * The disabled look stays the theme default. null returns it to the theme default.
     */
    override var foreground: WColor?
        get() = super.foreground
        set(value) {
            val keys = stateColors.foreground
            if (value == null) keys.remove(this) else keys.put(this, value)
            super.foreground = value
        }

    /** The background and text color keys that this class's template looks up for each visual state. */
    internal open val stateColors: ToggleStateColors
        get() = TOGGLE_BUTTON_COLORS

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

/** Theme resource keys for the background and text color of toggle-type buttons for each visual state ([WToggleButton.stateColors]). */
internal class ToggleStateColors(val background: StateColorKeys, val foreground: StateColorKeys)

/**
 * ToggleButton keys (controls/dev/CommonStyles/ToggleButton_themeresources.xaml). The background covers only the
 * unchecked and indeterminate states (ToggleButtonBackground* / ToggleButtonBackgroundIndeterminate*), and
 * ToggleButtonBackgroundChecked* is not included.
 * The foreground is ToggleButtonForeground* / ToggleButtonForegroundChecked* / ToggleButtonForegroundIndeterminate*.
 */
private val TOGGLE_BUTTON_COLORS = ToggleStateColors(
    background = StateColorKeys.of("ToggleButtonBackground", BUTTON_BACKGROUND_SHADES.withChecks("", "Indeterminate")),
    foreground = StateColorKeys.of("ToggleButtonForeground", BUTTON_FOREGROUND_SHADES.withChecks("", "Checked", "Indeterminate")),
)

/** A map with each of [checks] (check state names; "" is unchecked) prefixed to the state names (the keys of this map). */
internal fun Map<String, Double>.withChecks(vararg checks: String): Map<String, Double> =
    checks.flatMap { check -> map { (state, factor) -> "$check$state" to factor } }.toMap()
