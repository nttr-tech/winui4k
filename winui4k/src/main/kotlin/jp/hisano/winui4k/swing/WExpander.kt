package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.MemorySegment

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
        fun of(native: Int): ExpandDirection = entries.first { it.native == native }
    }
}

/**
 * Collapsible JPanel-like: WinUI 3's Expander.
 * Clicking the [header] expands/collapses the [content].
 */
class WExpander(header: String = "", content: WComponent? = null) : WControl(
    WinRt.composeDefault(Abi.CLS_Expander, Abi.IID_IExpanderFactory), // default interface = IExpander
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }

    /** Listener → event token (used by the remove functions). */
    private val expandTokens = ListenerTokens<() -> Unit>()
    private val collapseTokens = ListenerTokens<() -> Unit>()

    /** The header text (Expander.Header). Object-typed, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = WinRt.boxString(value)
            inspectable.call(Abi.IExpander_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The content shown while expanded (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                Abi.IContentControl_put_Content,
                value?.uiElement?.ptr ?: MemorySegment.NULL,
            )
        }

    /** Whether it's currently expanded (Expander.IsExpanded). */
    var isExpanded: Boolean
        get() = inspectable.getBool(Abi.IExpander_get_IsExpanded)
        set(value) = inspectable.putBool(Abi.IExpander_put_IsExpanded, value)

    /** The direction it expands toward (Expander.ExpandDirection). */
    var expandDirection: ExpandDirection
        get() = ExpandDirection.of(inspectable.getInt(Abi.IExpander_get_ExpandDirection))
        set(value) = inspectable.call(Abi.IExpander_put_ExpandDirection, value.native)

    init {
        if (header.isNotEmpty()) this.header = header
        if (content != null) this.content = content
    }

    /** Registers a listener called when it expands (Expander.Expanding). */
    fun addExpandListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ExpanderHandler", Abi.IID_ExpanderExpandingHandler, Abi.IExpander_add_Expanding,
        ) { _, _ -> listener() }
        expandTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addExpandListener]. */
    fun removeExpandListener(listener: () -> Unit) {
        val token = expandTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IExpander_remove_Expanding, token)
    }

    /** Registers a listener called when it collapses (Expander.Collapsed). */
    fun addCollapseListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ExpanderHandler", Abi.IID_ExpanderCollapsedHandler, Abi.IExpander_add_Collapsed,
        ) { _, _ -> listener() }
        collapseTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCollapseListener]. */
    fun removeCollapseListener(listener: () -> Unit) {
        val token = collapseTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IExpander_remove_Collapsed, token)
    }
}
