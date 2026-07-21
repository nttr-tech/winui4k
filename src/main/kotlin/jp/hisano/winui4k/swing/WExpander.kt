package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

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
    private val expandTokens = ArrayDeque<Pair<() -> Unit, Long>>()
    private val collapseTokens = ArrayDeque<Pair<() -> Unit, Long>>()

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
        expandTokens.addLast(
            listener to subscribe(Abi.IExpander_add_Expanding, Abi.IID_ExpanderExpandingHandler, listener),
        )
    }

    /** Unsubscribes a listener registered via [addExpandListener]. */
    fun removeExpandListener(listener: () -> Unit) {
        unsubscribe(expandTokens, Abi.IExpander_remove_Expanding, listener)
    }

    /** Registers a listener called when it collapses (Expander.Collapsed). */
    fun addCollapseListener(listener: () -> Unit) {
        collapseTokens.addLast(
            listener to subscribe(Abi.IExpander_add_Collapsed, Abi.IID_ExpanderCollapsedHandler, listener),
        )
    }

    /** Unsubscribes a listener registered via [addCollapseListener]. */
    fun removeCollapseListener(listener: () -> Unit) {
        unsubscribe(collapseTokens, Abi.IExpander_remove_Collapsed, listener)
    }

    /** Implements TypedEventHandler<Expander, TArgs> with KComObject and passes it to add_XXX. */
    private fun subscribe(addSlot: Int, handlerIid: String, listener: () -> Unit): Long {
        val handler = KComObject("WinUI4K.ExpanderHandler", inspectable = false)
            .addInterface(
                handlerIid,
                listOf(
                    // Invoke(this, Expander sender, TArgs e) — vtbl[3]
                    KComObject.Method(
                        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                    ) {
                        listener()
                        KComObject.S_OK
                    },
                ),
            )
        return Arena.ofConfined().use { a ->
            val out = a.allocate(JAVA_LONG) // EventRegistrationToken (int64)
            inspectable.call(addSlot, handler.primary, out)
            out.get(JAVA_LONG, 0)
        }
    }

    private fun unsubscribe(tokens: ArrayDeque<Pair<() -> Unit, Long>>, removeSlot: Int, listener: () -> Unit) {
        val index = tokens.indexOfLast { it.first === listener }
        if (index < 0) return
        val (_, token) = tokens.removeAt(index)
        inspectable.call(removeSlot, token)
    }
}
