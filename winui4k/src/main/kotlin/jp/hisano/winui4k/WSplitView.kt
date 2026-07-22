package jp.hisano.winui4k

import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.SplitViewDisplayMode (how the pane is displayed).
 * Values extracted from the winmd (Overlay=0, Inline=1, CompactOverlay=2, CompactInline=3).
 */
enum class SplitViewDisplayMode(internal val native: Int) {
    /** Displayed on top of the content (default). */
    OVERLAY(0),

    /** Displayed by shifting the content aside. */
    INLINE(1),

    /** Shown as a thin strip while closed, then overlays when opened. */
    COMPACT_OVERLAY(2),

    /** Shown as a thin strip while closed, then shifts the content aside when opened. */
    COMPACT_INLINE(3),
    ;

    internal companion object {
        fun of(native: Int): SplitViewDisplayMode = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.SplitViewPanePlacement (the pane's position).
 * Values extracted from the winmd (Left=0, Right=1).
 */
enum class SplitViewPanePlacement(internal val native: Int) {
    /** On the left (default). */
    LEFT(0),

    /** On the right. */
    RIGHT(1),
    ;

    internal companion object {
        fun of(native: Int): SplitViewPanePlacement = entries.first { it.native == native }
    }
}

/**
 * JSplitPane-like: WinUI 3's SplitView.
 * Displays a collapsible [pane] alongside the main [content].
 */
class WSplitView(pane: WComponent? = null, content: WComponent? = null) : WControl(
    Activation.composeDefault(Abi.CLS_SplitView, Abi.IID_ISplitViewFactory), // default interface = ISplitView
) {
    /** The main content (SplitView.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISplitView_put_Content, value?.uiElement?.ptr)
        }

    /** The collapsible pane (SplitView.Pane). */
    var pane: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.ISplitView_put_Pane, value?.uiElement?.ptr)
        }

    /** Whether the pane is open (SplitView.IsPaneOpen). */
    var isPaneOpen: Boolean
        get() = inspectable.getBool(Abi.ISplitView_get_IsPaneOpen)
        set(value) = inspectable.putBool(Abi.ISplitView_put_IsPaneOpen, value)

    /** The pane's width while open (SplitView.OpenPaneLength). */
    var openPaneLength: Double
        get() = inspectable.getDouble(Abi.ISplitView_get_OpenPaneLength)
        set(value) = inspectable.call(Abi.ISplitView_put_OpenPaneLength, value)

    /** The pane's position (SplitView.PanePlacement). */
    var panePlacement: SplitViewPanePlacement
        get() = SplitViewPanePlacement.of(inspectable.getInt(Abi.ISplitView_get_PanePlacement))
        set(value) = inspectable.call(Abi.ISplitView_put_PanePlacement, value.native)

    /** How the pane is displayed (SplitView.DisplayMode). */
    var displayMode: SplitViewDisplayMode
        get() = SplitViewDisplayMode.of(inspectable.getInt(Abi.ISplitView_get_DisplayMode))
        set(value) = inspectable.call(Abi.ISplitView_put_DisplayMode, value.native)

    init {
        if (pane != null) this.pane = pane
        if (content != null) this.content = content
    }
}
