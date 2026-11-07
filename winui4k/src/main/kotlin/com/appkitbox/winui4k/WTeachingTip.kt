package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
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
        @JvmSynthetic
        fun of(native: Int): TeachingTipCloseReason = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's TeachingTip: a callout pointing at an element ([target]) that shows notifications such as feature introductions. Swing has no counterpart, so
 * the WinUI class name is used as is (the hint that appears on hover is [WToolTip] / [WComponent.toolTipText]).
 * If target is not set, it appears in a corner of the screen. It can have a title, subtitle, [icon], an action button and a close button.
 * As in the real Gallery, add it into a container in the XAML tree ahead of time before use
 * (it renders nothing while closed). Opening it outside the tree shows no content.
 */
class WTeachingTip @JvmOverloads constructor(title: String = "", subtitle: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_TeachingTip, XamlInterop.IID_ITeachingTipFactory), // default interface = ITeachingTip
) {
    /** Listener -> event token (used to remove). */
    private val actionTokens = ListenerTokens<Runnable>()
    private val closeTokens = ListenerTokens<Consumer<TeachingTipCloseReason>>()

    /** The callout's heading (TeachingTip.Title). */
    var title: String
        get() = inspectable.getString(XamlInterop.ITeachingTip_get_Title)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ITeachingTip_put_Title, h) }

    /** The description below the heading (TeachingTip.Subtitle). */
    var subtitle: String
        get() = inspectable.getString(XamlInterop.ITeachingTip_get_Subtitle)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ITeachingTip_put_Subtitle, h) }

    /** The element the callout's tail points at (TeachingTip.Target). Null shows it in a screen corner. */
    var target: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.ITeachingTip_put_Target, value?.frameworkElement?.ptr)
        }

    /** The preferred display position relative to [target] (TeachingTip.PreferredPlacement). Adjusted automatically if it doesn't fit. */
    var preferredPlacement: TeachingTipPlacement
        get() = TeachingTipPlacement.of(inspectable.getInt(XamlInterop.ITeachingTip_get_PreferredPlacement))
        set(value) = inspectable.call(XamlInterop.ITeachingTip_put_PreferredPlacement, value.native)

    /** Whether it closes automatically on an outside click (TeachingTip.IsLightDismissEnabled). */
    var isLightDismissEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ITeachingTip_get_IsLightDismissEnabled)
        set(value) = inspectable.putBool(XamlInterop.ITeachingTip_put_IsLightDismissEnabled, value)

    /** The action button text (TeachingTip.ActionButtonContent). Not shown while empty. */
    var actionButtonText: String
        get() = contentText(XamlInterop.ITeachingTip_get_ActionButtonContent)
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ITeachingTip_put_ActionButtonContent, boxed.ptr)
            boxed.release()
        }

    /** The close button text (TeachingTip.CloseButtonContent). Only the × button while empty. */
    var closeButtonText: String
        get() = contentText(XamlInterop.ITeachingTip_get_CloseButtonContent)
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ITeachingTip_put_CloseButtonContent, boxed.ptr)
            boxed.release()
        }

    /** The icon to the left of the title (TeachingTip.IconSource). null (default) means no icon. */
    var icon: WIcon? = null
        set(value) {
            val source = value?.createIconSource()
            try {
                inspectable.call(XamlInterop.ITeachingTip_put_IconSource, source?.ptr)
            } finally {
                source?.release()
            }
            field = value
        }

    /** Whether the callout is open (TeachingTip.IsOpen). */
    val isOpen: Boolean
        get() = inspectable.getBool(XamlInterop.ITeachingTip_get_IsOpen)

    /** The button Content (object) if it is a string, otherwise "". */
    private fun contentText(slot: Int): String = inspectable.getPtrOrNull(slot)?.let { boxed ->
        try {
            PropertyValues.unboxString(boxed)
        } finally {
            boxed.release()
        }
    }.orEmpty()

    init {
        if (title.isNotEmpty()) this.title = title
        if (subtitle.isNotEmpty()) this.subtitle = subtitle
    }

    /** Opens the callout (TeachingTip.IsOpen = true). */
    fun show() {
        inspectable.putBool(XamlInterop.ITeachingTip_put_IsOpen, true)
    }

    /**
     * Closes the callout (TeachingTip.IsOpen = false). The close reason is [TeachingTipCloseReason.PROGRAMMATIC].
     * If called during the opening animation, TeachingTip sets IsOpen back to true and does not close (WinUI behavior).
     */
    fun hide() {
        inspectable.putBool(XamlInterop.ITeachingTip_put_IsOpen, false)
    }

    /** Registers a listener called when the action button is pressed (TeachingTip.ActionButtonClick). */
    @JvmSynthetic
    fun addActionListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addActionListener(adapter)
        actionTokens.addKotlinAdapter(listener, adapter)
    }

    fun addActionListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TeachingTipHandler",
            XamlInterop.IID_TeachingTipObjectHandler,
            XamlInterop.ITeachingTip_add_ActionButtonClick,
        ) { _, _ -> listener.run() }
        actionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: () -> Unit) {
        val adapter = actionTokens.removeKotlinAdapter(listener) ?: return
        removeActionListener(adapter)
    }

    fun removeActionListener(listener: Runnable) {
        val token = actionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITeachingTip_remove_ActionButtonClick, token)
    }

    /** Registers a listener called when it closes (TeachingTip.Closed). Passed the reason it closed. */
    @JvmSynthetic
    fun addCloseListener(listener: (TeachingTipCloseReason) -> Unit) {
        val adapter = Consumer<TeachingTipCloseReason> { listener(it) }
        addCloseListener(adapter)
        closeTokens.addKotlinAdapter(listener, adapter)
    }

    fun addCloseListener(listener: Consumer<TeachingTipCloseReason>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TeachingTipHandler",
            XamlInterop.IID_TeachingTipClosedHandler,
            XamlInterop.ITeachingTip_add_Closed,
        ) { _, args ->
            listener.accept(
                TeachingTipCloseReason.of(ComPtr(args).getInt(XamlInterop.ITeachingTipClosedEventArgs_get_Reason)),
            )
        }
        closeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCloseListener]. */
    @JvmSynthetic
    fun removeCloseListener(listener: (TeachingTipCloseReason) -> Unit) {
        val adapter = closeTokens.removeKotlinAdapter(listener) ?: return
        removeCloseListener(adapter)
    }

    fun removeCloseListener(listener: Consumer<TeachingTipCloseReason>) {
        val token = closeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITeachingTip_remove_Closed, token)
    }
}
