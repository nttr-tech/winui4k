package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.TeachingTipPlacementMode ([WTeachingTip.preferredPlacement]).
 * Values extracted from the winmd.
 */
enum class TeachingTipPlacement(internal val native: Int) {
    AUTO(0),
    TOP(1),
    BOTTOM(2),
    LEFT(3),
    RIGHT(4),
    TOP_RIGHT(5),
    TOP_LEFT(6),
    BOTTOM_RIGHT(7),
    BOTTOM_LEFT(8),
    LEFT_TOP(9),
    LEFT_BOTTOM(10),
    RIGHT_TOP(11),
    RIGHT_BOTTOM(12),
    CENTER(13),
    ;

    internal companion object {
        fun of(native: Int): TeachingTipPlacement = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.TeachingTipCloseReason (how it was closed).
 * Values extracted from the winmd (CloseButton=0, LightDismiss=1, Programmatic=2).
 */
enum class TeachingTipCloseReason(internal val native: Int) {
    /** Closed via the close button (× / [WTeachingTip.closeButtonText]). */
    CLOSE_BUTTON(0),

    /** Closed by an outside click (light dismiss). */
    LIGHT_DISMISS(1),

    /** Closed programmatically ([WTeachingTip.hide]). */
    PROGRAMMATIC(2),
    ;

    internal companion object {
        fun of(native: Int): TeachingTipCloseReason = entries.first { it.native == native }
    }
}

/**
 * JToolTip-like: WinUI 3's TeachingTip. Shows a callout pointing at an element ([target])
 * for things like feature announcements. If target isn't set, it's shown in a screen corner.
 * As in the real Gallery, add it into a container in the XAML tree ahead of time before use
 * (it renders nothing while closed). Opening it outside the tree shows no content.
 */
class WTeachingTip(title: String = "", subtitle: String = "") : WControl(
    Activation.composeDefault(Abi.CLS_TeachingTip, Abi.IID_ITeachingTipFactory), // default interface = ITeachingTip
) {
    /** Listener -> event token (used to remove). */
    private val actionTokens = ListenerTokens<() -> Unit>()
    private val closeTokens = ListenerTokens<(TeachingTipCloseReason) -> Unit>()

    /** The callout's heading (TeachingTip.Title). */
    var title: String
        get() = inspectable.getString(Abi.ITeachingTip_get_Title)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITeachingTip_put_Title, h) }

    /** The description below the heading (TeachingTip.Subtitle). */
    var subtitle: String
        get() = inspectable.getString(Abi.ITeachingTip_get_Subtitle)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITeachingTip_put_Subtitle, h) }

    /** The element the callout's tail points at (TeachingTip.Target). Null shows it in a screen corner. */
    var target: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.ITeachingTip_put_Target, value?.frameworkElement?.ptr)
        }

    /** The preferred display position relative to [target] (TeachingTip.PreferredPlacement). Adjusted automatically if it doesn't fit. */
    var preferredPlacement: TeachingTipPlacement
        get() = TeachingTipPlacement.of(inspectable.getInt(Abi.ITeachingTip_get_PreferredPlacement))
        set(value) = inspectable.call(Abi.ITeachingTip_put_PreferredPlacement, value.native)

    /** Whether it closes automatically on an outside click (TeachingTip.IsLightDismissEnabled). */
    var isLightDismissEnabled: Boolean
        get() = inspectable.getBool(Abi.ITeachingTip_get_IsLightDismissEnabled)
        set(value) = inspectable.putBool(Abi.ITeachingTip_put_IsLightDismissEnabled, value)

    /** The action button's label. Object-typed, so a boxed string is passed. Left empty, it isn't shown. */
    var actionButtonText: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.ITeachingTip_put_ActionButtonContent, boxed.ptr)
            boxed.release()
        }

    /** The close button's label. Object-typed, so a boxed string is passed. Left empty, only the × button shows. */
    var closeButtonText: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.ITeachingTip_put_CloseButtonContent, boxed.ptr)
            boxed.release()
        }

    val isOpen: Boolean
        get() = inspectable.getBool(Abi.ITeachingTip_get_IsOpen)

    init {
        if (title.isNotEmpty()) this.title = title
        if (subtitle.isNotEmpty()) this.subtitle = subtitle
    }

    /** Opens the callout (TeachingTip.IsOpen = true). */
    fun show() {
        inspectable.putBool(Abi.ITeachingTip_put_IsOpen, true)
    }

    fun hide() {
        inspectable.putBool(Abi.ITeachingTip_put_IsOpen, false)
    }

    /** Registers a listener called when the action button is pressed (TeachingTip.ActionButtonClick). */
    fun addActionListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TeachingTipHandler", Abi.IID_TeachingTipObjectHandler, Abi.ITeachingTip_add_ActionButtonClick,
        ) { _, _ -> listener() }
        actionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    fun removeActionListener(listener: () -> Unit) {
        val token = actionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.ITeachingTip_remove_ActionButtonClick, token)
    }

    /** Registers a listener called when it closes (TeachingTip.Closed). Passed the reason it closed. */
    fun addCloseListener(listener: (TeachingTipCloseReason) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TeachingTipHandler", Abi.IID_TeachingTipClosedHandler, Abi.ITeachingTip_add_Closed,
        ) { _, args ->
            listener(TeachingTipCloseReason.of(ComPtr(args).getInt(Abi.ITeachingTipClosedEventArgs_get_Reason)))
        }
        closeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCloseListener]. */
    fun removeCloseListener(listener: (TeachingTipCloseReason) -> Unit) {
        val token = closeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.ITeachingTip_remove_Closed, token)
    }
}
