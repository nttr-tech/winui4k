package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.ffi.api.Ptr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winrt.addEventHandler
import jp.hisano.winui4k.winrt.getString
import jp.hisano.winui4k.winrt.removeEventHandler
import jp.hisano.winui4k.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.SwipeMode (behavior when swiped).
 * Values extracted from the winmd (Reveal=0, Execute=1).
 */
enum class SwipeMode(internal val native: Int) {
    /** Swiping reveals the button, and tapping it executes it (default). */
    REVEAL(0),

    /** Swiping all the way executes it immediately. */
    EXECUTE(1),
    ;

    internal companion object {
        fun of(native: Int): SwipeMode = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.SwipeBehaviorOnInvoked (whether it closes after the item runs).
 * Values extracted from the winmd (Auto=0, Close=1, RemainOpen=2).
 */
enum class SwipeBehaviorOnInvoked(internal val native: Int) {
    /** Automatic, based on the mode (default). */
    AUTO(0),

    /** Closes after running. */
    CLOSE(1),

    /** Stays open after running. */
    REMAIN_OPEN(2),
    ;

    internal companion object {
        fun of(native: Int): SwipeBehaviorOnInvoked = entries.first { it.native == native }
    }
}

/**
 * A container that reveals commands on a touch swipe: WinUI 3's SwipeControl (a ContentControl subclass).
 * Swiping [content] left, right, up, or down reveals the corresponding [WSwipeItems], such as [leftItems].
 * (Swiping is touch/pen only; it doesn't open on a mouse drag.)
 */
class WSwipeControl(content: WComponent? = null) : WControl(
    Activation.composeDefault(Abi.CLS_SwipeControl, Abi.IID_ISwipeControlFactory),
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }

    /** The content shown on the front, before swiping (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                Abi.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    /** Items revealed from the left edge on a rightward swipe (SwipeControl.LeftItems). */
    var leftItems: WSwipeItems? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISwipeControl_put_LeftItems, value.ptrOrNull())
        }

    /** Items revealed from the right edge on a leftward swipe (SwipeControl.RightItems). */
    var rightItems: WSwipeItems? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISwipeControl_put_RightItems, value.ptrOrNull())
        }

    /** Items revealed from the top edge on a downward swipe (SwipeControl.TopItems). */
    var topItems: WSwipeItems? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISwipeControl_put_TopItems, value.ptrOrNull())
        }

    /** Items revealed from the bottom edge on an upward swipe (SwipeControl.BottomItems). */
    var bottomItems: WSwipeItems? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISwipeControl_put_BottomItems, value.ptrOrNull())
        }

    init {
        if (content != null) this.content = content
    }

    /** Closes any open swipe item (SwipeControl.Close). */
    fun close() {
        inspectable.call(Abi.ISwipeControl_Close)
    }

    private fun WSwipeItems?.ptrOrNull(): Ptr? = this?.inspectable?.ptr
}

/**
 * The collection of items shown on one edge of a SwipeControl: WinUI 3's SwipeItems.
 * Not a UIElement, so this is not a subclass of WComponent.
 */
class WSwipeItems(mode: SwipeMode = SwipeMode.REVEAL) {
    /** The default interface (ISwipeItems). */
    internal val inspectable: ComPtr =
        Activation.composeDefault(Abi.CLS_SwipeItems, Abi.IID_ISwipeItemsFactory)

    /** The IVector<SwipeItem> view that has Append (SwipeItems implements IVector). */
    private val vector: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IVector_SwipeItem)
    }

    /** The swipe behavior (SwipeItems.Mode). In EXECUTE mode, only a single item is allowed. */
    var mode: SwipeMode
        get() = SwipeMode.of(inspectable.getInt(Abi.ISwipeItems_get_Mode))
        set(value) = inspectable.call(Abi.ISwipeItems_put_Mode, value.native)

    init {
        if (mode != SwipeMode.REVEAL) this.mode = mode
    }

    /** Appends an item (IVector<SwipeItem>.Append). */
    fun add(item: WSwipeItem) {
        vector.call(Abi.IVector_Append, item.inspectable.ptr)
    }
}

/**
 * A single button revealed by swiping: WinUI 3's SwipeItem.
 * Not a UIElement, so this is not a subclass of WComponent.
 */
class WSwipeItem(text: String = "", icon: Symbol? = null) {
    /** The default interface (ISwipeItem). */
    internal val inspectable: ComPtr =
        Activation.composeDefault(Abi.CLS_SwipeItem, Abi.IID_ISwipeItemFactory)

    /** Invoked event tokens registered via addActionListener. */
    private val invokedTokens = ListenerTokens<() -> Unit>()

    /** The button's label (SwipeItem.Text). */
    var text: String
        get() = inspectable.getString(Abi.ISwipeItem_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ISwipeItem_put_Text, h) }

    /** The button's icon (SwipeItem.IconSource). Creates and passes a SymbolIconSource. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.ISwipeItem_put_IconSource, null)
                return
            }
            val iconSource = value.createIconSource()
            inspectable.call(Abi.ISwipeItem_put_IconSource, iconSource.ptr)
            iconSource.release()
        }

    /** The button's background color (SwipeItem.Background). Creates and passes a SolidColorBrush. */
    var background: WColor? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.ISwipeItem_put_Background, null)
                return
            }
            val brush = value.createBrush()
            inspectable.call(Abi.ISwipeItem_put_Background, brush.ptr)
            brush.release()
        }

    /** Whether the swipe closes after this item runs (SwipeItem.BehaviorOnInvoked). */
    var behaviorOnInvoked: SwipeBehaviorOnInvoked
        get() = SwipeBehaviorOnInvoked.of(inspectable.getInt(Abi.ISwipeItem_get_BehaviorOnInvoked))
        set(value) = inspectable.call(Abi.ISwipeItem_put_BehaviorOnInvoked, value.native)

    /** The command run on tap (SwipeItem.Command). */
    var command: WCommandBase? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISwipeItem_put_Command, value?.commandPtr)
        }

    init {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /**
     * ActionListener-like: subscribes to the item running. Subscribes to SwipeItem.Invoked
     * (TypedEventHandler<SwipeItem, SwipeItemInvokedEventArgs>) under the hood.
     */
    fun addActionListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.SwipeItemInvokedHandler",
            Abi.IID_SwipeItemInvokedHandler,
            Abi.ISwipeItem_add_Invoked,
        ) { _, _ -> listener() }
        invokedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    fun removeActionListener(listener: () -> Unit) {
        val token = invokedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.ISwipeItem_remove_Invoked, token)
    }
}
