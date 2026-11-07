package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.ExpandDirection (the direction the content expands toward).
 * Values extracted from the winmd (Down=0, Up=1).
 */
enum class ExpandDirection(internal val native: Int) {
    /** Expands downward (default). */
    DOWN(0),

    /** Expands upward. */
    UP(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): ExpandDirection = entries.first { it.native == native }
    }
}

/**
 * Collapsible JPanel-like: WinUI 3's Expander.
 * Clicking the header ([header]) expands or collapses the content ([content]). Expand/collapse is notified by [addChangeListener] (once per change in either direction),
 * or one direction at a time by [addExpandListener] / [addCollapseListener].
 */
class WExpander @JvmOverloads constructor(header: String = "", content: WComponent? = null) : WControl(
    Activation.composeDefault(XamlInterop.CLS_Expander, XamlInterop.IID_IExpanderFactory), // default interface = IExpander
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** The ChangeListeners. One pair of native handlers (Expanding and Collapsed) is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addChangeHandlers() }

    /** Listener → event token (used by the remove functions). */
    private val expandTokens = ListenerTokens<Runnable>()
    private val collapseTokens = ListenerTokens<Runnable>()

    /**
     * The header string (Expander.Header). It is of type Object, so a boxed string is passed, and when read it is read back from the native side and unboxed
     * (so strings set on the XAML side can be read). Returns "" for anything other than a string (such as an element set on the XAML side) or when unset.
     */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.IExpander_get_Header) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            try {
                inspectable.call(XamlInterop.IExpander_put_Header, boxed.ptr)
            } finally {
                boxed.release()
            }
        }

    /** The content shown while expanded (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    override fun detachChild(child: WComponent) {
        if (this.content === child) this.content = null
    }

    /** Whether it's currently expanded (Expander.IsExpanded). */
    var isExpanded: Boolean
        get() = inspectable.getBool(XamlInterop.IExpander_get_IsExpanded)
        set(value) = inspectable.putBool(XamlInterop.IExpander_put_IsExpanded, value)

    /** The direction it expands toward (Expander.ExpandDirection). */
    var expandDirection: ExpandDirection
        get() = ExpandDirection.of(inspectable.getInt(XamlInterop.IExpander_get_ExpandDirection))
        set(value) = inspectable.call(XamlInterop.IExpander_put_ExpandDirection, value.native)

    init {
        if (header.isNotEmpty()) this.header = header
        if (content != null) this.content = content
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this Expander) on every expand or collapse (change of [isExpanded]).
     * Called once per change in either direction, and when it is called [isExpanded] returns the new value.
     * Called both for user interaction (clicking the header) and when [isExpanded] is set.
     *
     * The existing [addExpandListener] (Expanding) and [addCollapseListener] (Collapsed) handle one direction each,
     * and are called at different times: within the IsExpanded change notification, WinUI sends Expanding before updating the visual state when expanding,
     * and Collapsed after updating the visual state when collapsing (in both cases [isExpanded] has the new value).
     * ChangeListeners are called at the timing of Expanding when expanding and of Collapsed when collapsing.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order they were added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    private fun addChangeHandlers() {
        // The handlers stay on the native side, so do not strongly capture the Expander; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        for ((iid, slot) in listOf(
            XamlInterop.IID_ExpanderExpandingHandler to XamlInterop.IExpander_add_Expanding,
            XamlInterop.IID_ExpanderCollapsedHandler to XamlInterop.IExpander_add_Collapsed,
        )) {
            inspectable.addEventHandler("WinUI4K.ExpanderHandler", iid, slot) { _, _ ->
                (anchor.get() as? WExpander)?.changeListeners?.fire()
            }
        }
    }

    /** Registers a listener called when it expands (Expander.Expanding). */
    @JvmSynthetic
    fun addExpandListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addExpandListener(adapter)
        expandTokens.addKotlinAdapter(listener, adapter)
    }

    fun addExpandListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ExpanderHandler",
            XamlInterop.IID_ExpanderExpandingHandler,
            XamlInterop.IExpander_add_Expanding,
        ) { _, _ -> listener.run() }
        expandTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addExpandListener]. */
    @JvmSynthetic
    fun removeExpandListener(listener: () -> Unit) {
        val adapter = expandTokens.removeKotlinAdapter(listener) ?: return
        removeExpandListener(adapter)
    }

    fun removeExpandListener(listener: Runnable) {
        val token = expandTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IExpander_remove_Expanding, token)
    }

    /** Registers a listener called when it collapses (Expander.Collapsed). */
    @JvmSynthetic
    fun addCollapseListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addCollapseListener(adapter)
        collapseTokens.addKotlinAdapter(listener, adapter)
    }

    fun addCollapseListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ExpanderHandler",
            XamlInterop.IID_ExpanderCollapsedHandler,
            XamlInterop.IExpander_add_Collapsed,
        ) { _, _ -> listener.run() }
        collapseTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCollapseListener]. */
    @JvmSynthetic
    fun removeCollapseListener(listener: () -> Unit) {
        val adapter = collapseTokens.removeKotlinAdapter(listener) ?: return
        removeCollapseListener(adapter)
    }

    fun removeCollapseListener(listener: Runnable) {
        val token = collapseTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IExpander_remove_Collapsed, token)
    }
}
