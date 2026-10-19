package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
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

    /** The user's ItemListeners. The Checked / Unchecked / Indeterminate handlers are registered one at a time when first needed. */
    private val itemListeners = ItemListeners(this) { addItemHandlers() }

    private companion object {
        val ADD_SLOTS = intArrayOf(
            XamlInterop.IToggleButton_add_Checked,
            XamlInterop.IToggleButton_add_Unchecked,
            XamlInterop.IToggleButton_add_Indeterminate,
        )

        /** The value put in [WItemEvent.stateChange] for each event in [ADD_SLOTS]. */
        val STATES = intArrayOf(WItemEvent.SELECTED, WItemEvent.DESELECTED, WItemEvent.INDETERMINATE)
    }

    private fun addItemHandlers() {
        // The handler stays on the native object, so it does not capture the button strongly but looks it up from anchor
        // (see ListenerAnchor)
        val anchor = listenerAnchor
        for (i in ADD_SLOTS.indices) {
            val state = STATES[i]
            toggleButton.addEventHandler(
                "WinUI4K.ToggleHandler",
                XamlInterop.IID_RoutedEventHandler,
                ADD_SLOTS[i],
            ) { _, _ -> (anchor.get() as? WToggleButton)?.onItemStateChanged(state) }
        }
    }

    /**
     * Dispatches one native notification (Checked / Unchecked / Indeterminate) in the same order as Swing's JToggleButton.
     * 1. Mutual exclusion of [WButtonGroup] (if newly selected, deselects the old selection, whose DESELECTED is dispatched
     *    before this button's SELECTED. If the notification tries to deselect the selected button, the group restores
     *    the selection and this notification is dispatched to no one)
     * 2. The user's ItemListeners (in the order added, regardless of whether they were added before or after the group
     *    or the action)
     * 3. Writing back to the SELECTED_KEY of [action] (as in Swing's AbstractButton, rewritten only when the action has a
     *    SELECTED_KEY value and it differs)
     */
    private fun onItemStateChanged(stateChange: Int) {
        val group = buttonGroup
        if (group != null && !group.onItemStateChanged(this, stateChange)) return
        itemListeners.fire(WItemEvent(this, stateChange))
        val action = action
        val current = action?.getValue(WAction.SELECTED_KEY)
        if (action != null && current != null) {
            val selected = isSelected
            if (current != selected) action.putValue(WAction.SELECTED_KEY, selected)
        }
    }

    /** Matches the selection state to the action's SELECTED_KEY (false if absent). Throws [IllegalArgumentException] if it is not a Boolean. */
    override fun applyActionSelected(action: WAction?) {
        val selected = action?.typedValue<Boolean>(WAction.SELECTED_KEY, "Boolean") == true
        if (selected != isSelected) isSelected = selected
    }

    /** Whether an action is bound ([onActionChanged]). While bound, it counts as one listener for writing back the selection state. */
    private var actionBound = false

    /** Receives selection state notifications only while an action is bound (to write them back). */
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

    /**
     * The [WButtonGroup] this button belongs to (changed only by [WButtonGroup.add] / [WButtonGroup.remove]).
     * The button holds the group strongly, in the same direction as Swing's ButtonModel holding the group. The group
     * also holds the button strongly, but the cycle stays inside the JVM heap and neither native handler captures them,
     * so once they are removed from the window and no longer referenced, they are collected together.
     */
    internal var buttonGroup: WButtonGroup? = null
        private set

    /** Becomes a member of [group]. It receives selection state notifications (for mutual exclusion), so it is treated as holding one listener. */
    internal fun joinGroup(group: WButtonGroup) {
        check(buttonGroup == null) { "already belongs to another ButtonGroup" }
        buttonGroup = group
        itemListeners.ensureNativeHandler()
        listenerAnchor.listenerAdded()
    }

    /** Stops being a member of the group. */
    internal fun leaveGroup() {
        if (buttonGroup == null) return
        buttonGroup = null
        listenerAnchor.listenerRemoved()
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
        itemListeners.addAdapter(listener, WItemListener { listener(WItemEvent.checkedOf(it.stateChange)) })
    }

    /**
     * The legacy `addItemListener(Consumer<Boolean>)` for Java. It is kept only for binary compatibility (linking of
     * already compiled callers) and is not visible from Java source (HIDDEN). In Java, pass a [WItemListener] instead.
     */
    @Deprecated("Use the addItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("addItemListener")
    fun addItemListenerForJava(listener: Consumer<Boolean?>) {
        itemListeners.addAdapter(listener, WItemListener { listener.accept(WItemEvent.checkedOf(it.stateChange)) })
    }

    /**
     * ItemListener.itemStateChanged-style: receives a [WItemEvent] each time the selection state changes.
     * It is also called when the state becomes indeterminate ([WItemEvent.INDETERMINATE]), which Swing's
     * `getStateChange() == SELECTED` check treats the same as unchecked.
     * Each change passes one event to all listeners in the order added. When the selection is changed within a
     * [WButtonGroup], the DESELECTED of the deselected button arrives before the SELECTED of the selected button, and
     * the SELECTED_KEY of [action] is rewritten after all listeners are called (both as in Swing, regardless of whether
     * the listener was added before or after the button joined the group).
     */
    fun addItemListener(listener: WItemListener) {
        itemListeners.add(listener)
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    @JvmSynthetic
    fun removeItemListener(listener: (Boolean?) -> Unit) {
        itemListeners.removeAdapter(listener)
    }

    /** The legacy `removeItemListener(Consumer<Boolean>)` (not visible from Java source), for the same reason as [addItemListenerForJava]. */
    @Deprecated("Use the removeItemListener that takes a WItemListener", level = DeprecationLevel.HIDDEN)
    @JvmName("removeItemListener")
    fun removeItemListenerForJava(listener: Consumer<Boolean?>) {
        itemListeners.removeAdapter(listener)
    }

    /** Unsubscribes a [WItemListener] registered with [addItemListener]. */
    fun removeItemListener(listener: WItemListener) {
        itemListeners.remove(listener)
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
