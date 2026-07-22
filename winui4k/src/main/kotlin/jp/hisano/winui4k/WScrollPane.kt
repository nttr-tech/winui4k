package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.withScope
import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.PropertyValues
import jp.hisano.winui4k.internal.winrt.addEventHandler
import jp.hisano.winui4k.internal.winrt.removeEventHandler
import jp.hisano.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.ScrollBarVisibility (how a scrollbar is shown).
 * Values extracted from the winmd (Disabled=0, Auto=1, Hidden=2, Visible=3).
 */
enum class ScrollBarVisibility(internal val native: Int) {
    /** Don't allow scrolling (the default for the horizontal direction). */
    DISABLED(0),

    /** Show only when the content overflows. */
    AUTO(1),

    /** Allow scrolling, but don't show the bar. */
    HIDDEN(2),

    /** Always show it. */
    VISIBLE(3),
    ;

    internal companion object {
        fun of(native: Int): ScrollBarVisibility = entries.first { it.native == native }
    }
}

/**
 * JScrollPane-like: WinUI 3's ScrollViewer (a ContentControl subclass).
 * Shows scrollbars when [content] is larger than the viewport.
 * Horizontal scrolling is disabled by default; enable it by setting [horizontalScrollBarVisibility] to e.g. AUTO.
 */
class WScrollPane(content: WComponent? = null) : WControl(
    Activation.activate(Abi.CLS_ScrollViewer).queryInterface(Abi.IID_IScrollViewer), // created via the default factory
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }

    /**
     * How the horizontal scrollbar is shown (ScrollViewer.HorizontalScrollBarVisibility, default DISABLED).
     * Setting anything other than DISABLED also enables scrolling (HorizontalScrollMode = Enabled).
     */
    var horizontalScrollBarVisibility: ScrollBarVisibility
        get() = ScrollBarVisibility.of(inspectable.getInt(Abi.IScrollViewer_get_HorizontalScrollBarVisibility))
        set(value) {
            inspectable.call(Abi.IScrollViewer_put_HorizontalScrollBarVisibility, value.native)
            inspectable.call(
                Abi.IScrollViewer_put_HorizontalScrollMode,
                if (value == ScrollBarVisibility.DISABLED) 0 else 1, // ScrollMode.Disabled / Enabled
            )
        }

    /** How the vertical scrollbar is shown (ScrollViewer.VerticalScrollBarVisibility, default VISIBLE). */
    var verticalScrollBarVisibility: ScrollBarVisibility
        get() = ScrollBarVisibility.of(inspectable.getInt(Abi.IScrollViewer_get_VerticalScrollBarVisibility))
        set(value) {
            inspectable.call(Abi.IScrollViewer_put_VerticalScrollBarVisibility, value.native)
            inspectable.call(
                Abi.IScrollViewer_put_VerticalScrollMode,
                if (value == ScrollBarVisibility.DISABLED) 0 else 1, // ScrollMode.Disabled / Enabled
            )
        }

    /** The current horizontal scroll position (ScrollViewer.HorizontalOffset). */
    val horizontalOffset: Double
        get() = inspectable.getDouble(Abi.IScrollViewer_get_HorizontalOffset)

    /** The viewport's width (ScrollViewer.ViewportWidth). */
    val viewportWidth: Double
        get() = inspectable.getDouble(Abi.IScrollViewer_get_ViewportWidth)

    /** The scrollable width = content width - viewport width (ScrollViewer.ScrollableWidth). 0 means no overflow. */
    val scrollableWidth: Double
        get() = inspectable.getDouble(Abi.IScrollViewer_get_ScrollableWidth)

    /** Animates the horizontal scroll position to [offset] (ScrollViewer.ChangeView). */
    fun scrollToHorizontalOffset(offset: Double) {
        // Box it as an IReference<Double> to pass (vertical position and zoom are null = no change)
        val boxed = PropertyValues.boxDouble(offset)
        val reference = boxed.queryInterface(Abi.IID_IReference_Double)
        boxed.release()
        Ffi.backend.withScope { scope ->
            val handled = scope.allocate(8) // out boolean (unused)
            inspectable.call(Abi.IScrollViewer_ChangeView, reference.ptr, null, null, handled)
        }
        reference.release()
    }

    /** Event tokens registered via addViewChangedListener. */
    private val viewChangedTokens = ListenerTokens<() -> Unit>()

    /** Subscribes to scroll position changes (ScrollViewer.ViewChanged; also fires during inertial scrolling). */
    fun addViewChangedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ScrollViewerViewChangedHandler",
            Abi.IID_ScrollViewerViewChangedHandler,
            Abi.IScrollViewer_add_ViewChanged,
        ) { _, _ -> listener() }
        viewChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addViewChangedListener]. */
    fun removeViewChangedListener(listener: () -> Unit) {
        val token = viewChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IScrollViewer_remove_ViewChanged, token)
    }

    /** The content to scroll (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                Abi.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    init {
        if (content != null) this.content = content
    }
}
