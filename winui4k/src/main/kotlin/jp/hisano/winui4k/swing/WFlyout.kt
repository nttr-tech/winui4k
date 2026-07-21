package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winui.Abi

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
 * Common base for FlyoutBase-derived types (Flyout / MenuFlyout / CommandBarFlyout).
 * FlyoutBase is not a UIElement, so this is not a subclass of WComponent.
 * Provides [placement] / [isOpen] / [showAt] / [hide].
 */
abstract class WFlyoutBase internal constructor(
    /** The flyout's default interface pointer (IFlyout, IMenuFlyout, ...). */
    internal val inspectable: ComPtr,
) {
    /** The FlyoutBase view required by Button.put_Flyout / ShowAt and the like. */
    internal val flyoutBase: ComPtr by lazy { inspectable.queryInterface(Abi.IID_IFlyoutBase) }

    /** Where it opens (FlyoutBase.Placement). */
    var placement: FlyoutPlacement
        get() = FlyoutPlacement.of(flyoutBase.getInt(Abi.IFlyoutBase_get_Placement))
        set(value) = flyoutBase.call(Abi.IFlyoutBase_put_Placement, value.native)

    val isOpen: Boolean
        get() = flyoutBase.getBool(Abi.IFlyoutBase_get_IsOpen)

    /** Opens relative to [anchor] (FlyoutBase.ShowAt). Only usable on an already-visible element. */
    fun showAt(anchor: WComponent) {
        flyoutBase.call(Abi.IFlyoutBase_ShowAt, anchor.frameworkElement.ptr)
    }

    fun hide() {
        flyoutBase.call(Abi.IFlyoutBase_Hide)
    }
}

/** JPopupMenu-like: WinUI 3's Flyout. Set on WButton.flyout to open it on click. */
class WFlyout(content: WComponent? = null) : WFlyoutBase(
    Activation.composeDefault(Abi.CLS_Flyout, Abi.IID_IFlyoutFactory),
) {
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.IFlyout_put_Content, value?.uiElement?.ptr)
        }

    init {
        if (content != null) this.content = content
    }
}
