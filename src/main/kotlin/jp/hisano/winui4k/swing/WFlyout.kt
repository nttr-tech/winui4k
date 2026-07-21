package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.MemorySegment

/**
 * Microsoft.UI.Xaml.Controls.Primitives.FlyoutPlacementMode.
 * Values extracted from the winmd (via the dump_winmd.py / dump_enum lineage of tools).
 */
enum class FlyoutPlacement(internal val native: Int) {
    TOP(0),
    BOTTOM(1),
    LEFT(2),
    RIGHT(3),
    FULL(4),
    TOP_EDGE_ALIGNED_LEFT(5),
    TOP_EDGE_ALIGNED_RIGHT(6),
    BOTTOM_EDGE_ALIGNED_LEFT(7),
    BOTTOM_EDGE_ALIGNED_RIGHT(8),
    LEFT_EDGE_ALIGNED_TOP(9),
    LEFT_EDGE_ALIGNED_BOTTOM(10),
    RIGHT_EDGE_ALIGNED_TOP(11),
    RIGHT_EDGE_ALIGNED_BOTTOM(12),
    AUTO(13),
    ;

    internal companion object {
        fun of(native: Int): FlyoutPlacement =
            entries.first { it.native == native }
    }
}

/**
 * JPopupMenu-like: WinUI 3's Flyout. Set on WButton.flyout to open it on click.
 * FlyoutBase is not a UIElement, so this is not a subclass of WComponent.
 */
class WFlyout(content: WComponent? = null) {
    /** The default interface (IFlyout). */
    private val flyout: ComPtr =
        WinRt.composeDefault(Abi.CLS_Flyout, Abi.IID_IFlyoutFactory)

    /** The FlyoutBase view required by Button.put_Flyout / ShowAt and the like. */
    internal val flyoutBase: ComPtr by lazy { flyout.queryInterface(Abi.IID_IFlyoutBase) }

    var content: WComponent? = null
        set(value) {
            field = value
            flyout.call(Abi.IFlyout_put_Content, value?.uiElement?.ptr ?: MemorySegment.NULL)
        }

    /** Where it opens (FlyoutBase.Placement). */
    var placement: FlyoutPlacement
        get() = FlyoutPlacement.of(flyoutBase.getInt(Abi.IFlyoutBase_get_Placement))
        set(value) = flyoutBase.call(Abi.IFlyoutBase_put_Placement, value.native)

    val isOpen: Boolean
        get() = flyoutBase.getBool(Abi.IFlyoutBase_get_IsOpen)

    init {
        if (content != null) this.content = content
    }

    /** Opens relative to [anchor] (FlyoutBase.ShowAt). Only usable on an already-visible element. */
    fun showAt(anchor: WComponent) {
        flyoutBase.call(Abi.IFlyoutBase_ShowAt, anchor.frameworkElement.ptr)
    }

    fun hide() {
        flyoutBase.call(Abi.IFlyoutBase_Hide)
    }
}
