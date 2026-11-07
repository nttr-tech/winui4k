package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

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
        @JvmSynthetic
        fun of(native: Int): FlyoutPlacement =
            entries.first { it.native == native }
    }
}

/**
 * Common base for FlyoutBase-derived types (Flyout / MenuFlyout / CommandBarFlyout).
 * FlyoutBase is not a UIElement, so this is not a subclass of WComponent.
 * Provides [placement] / [isOpen] / [showAt] / [hide], and the JPopupMenu-like [show] (opens at coordinates of the owner) / [isVisible] / [invoker] /
 * [addPopupMenuListener].
 */
abstract class WFlyoutBase internal constructor(
    /** The flyout's default interface pointer (IFlyout, IMenuFlyout, ...). */
    internal val inspectable: ComPtr,
) {
    /** The record of COM references this wrapper owns (the same mechanism as WComponent). */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /** Ties ownership of [ptr] to this wrapper's lifetime. */
    internal fun own(ptr: ComPtr): ComPtr = lifetime.own(ptr)

    /** The FlyoutBase view required by Button.put_Flyout / ShowAt and the like. */
    internal val flyoutBase: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IFlyoutBase)) }

    /** Where it opens (FlyoutBase.Placement). */
    var placement: FlyoutPlacement
        get() = FlyoutPlacement.of(flyoutBase.getInt(XamlInterop.IFlyoutBase_get_Placement))
        set(value) = flyoutBase.call(XamlInterop.IFlyoutBase_put_Placement, value.native)

    val isOpen: Boolean
        get() = flyoutBase.getBool(XamlInterop.IFlyoutBase_get_IsOpen)

    /**
     * An element that keeps receiving outside clicks and pointer enter/exit while the flyout is open
     * (FlyoutBase.OverlayInputPassThroughElement). In a menu bar, the headers' parent is specified here so that moving
     * the pointer to an adjacent header switches menus while one is open.
     */
    var overlayInputPassThroughElement: WComponent? = null
        set(value) {
            field = value
            flyoutBase.call(XamlInterop.IFlyoutBase_put_OverlayInputPassThroughElement, value?.dependencyObject)
        }

    /**
     * Opens with [anchor] as the reference position (FlyoutBase.ShowAt). Use it for elements on a window. For an element in a window that was just shown
     * (before the first layout), it opens after layout (Swing's show opens even right after showing).
     */
    fun showAt(anchor: WComponent) {
        explicitInvoker = anchor
        afterLaidOut(anchor) {
            events.showWhenClosed(isOpen) { flyoutBase.call(XamlInterop.IFlyoutBase_ShowAt, anchor.frameworkElement.ptr) }
        }
    }

    /**
     * If [anchor] was just added to a window and has not done the first layout yet, calls [show] after layout (cancelled by [hide]
     * before that). WinUI silently ignores ShowAt before the first layout. Otherwise it is called right away.
     */
    private fun afterLaidOut(anchor: WComponent, show: () -> Unit) {
        pendingAnchor?.laidOutRequests?.cancel(this)
        pendingAnchor = null
        if (!anchor.laidOutRequests.awaitsFirstLayout()) {
            show()
            return
        }
        pendingAnchor = anchor
        anchor.laidOutRequests.runOrDefer(this) {
            pendingAnchor = null
            show()
        }
    }

    /** The reference element waiting to open after layout ([afterLaidOut]). */
    private var pendingAnchor: WComponent? = null

    /**
     * JPopupMenu.show(Component, int, int)-like: opens at the position ([x], [y]; DIP) in the coordinates of [invoker]
     * (the Position of FlyoutBase.ShowAt(DependencyObject, FlyoutShowOptions)). Use it only on elements already shown.
     * It does not extend outside the window; WinUI shifts it to a position that fits ([placement] also takes effect).
     * [showAt] and this method, when called while closing (right after [hide], etc.), open after it has finished closing (because WinUI ignores
     * ShowAt to the same position for a closing flyout).
     */
    fun show(invoker: WComponent, x: Double, y: Double) {
        explicitInvoker = invoker
        afterLaidOut(invoker) { events.showWhenClosed(isOpen) { showWithPosition(invoker, x, y) } }
    }

    private fun showWithPosition(invoker: WComponent, x: Double, y: Double) {
        val options = Activation.composeDefault(XamlInterop.CLS_FlyoutShowOptions, XamlInterop.IID_IFlyoutShowOptionsFactory)
        try {
            val boxed = PropertyValues.boxPoint(x, y)
            val position = try {
                boxed.queryInterface(FoundationInterop.IID_IReference_Point)
            } finally {
                boxed.release()
            }
            try {
                options.call(XamlInterop.IFlyoutShowOptions_put_Position, position.ptr)
            } finally {
                position.release()
            }
            flyoutBase.call(XamlInterop.IFlyoutBase_ShowAtWithOptions, invoker.dependencyObject.ptr, options.ptr)
        } finally {
            options.release()
        }
    }

    /** Closes it (FlyoutBase.Hide). This way of closing does not call [WPopupMenuListener.popupMenuCanceled] (same as Swing's setVisible(false)). */
    fun hide() {
        pendingAnchor?.laidOutRequests?.cancel(this)
        pendingAnchor = null
        events.hideRequested = true
        flyoutBase.call(XamlInterop.IFlyoutBase_Hide)
    }

    /**
     * JPopupMenu.isVisible / setVisible-like: whether it is open (an alias of [isOpen]). Setting false calls [hide].
     * Setting true, if closed, opens it with [showAt] relative to the last [invoker]. Without an [invoker], IllegalStateException
     * (because the window to show it in is not determined; use [show] or [showAt]).
     */
    var isVisible: Boolean
        get() = isOpen
        set(value) {
            if (!value) {
                hide()
            } else if (!isOpen) {
                showAt(checkNotNull(invoker) { "There is no invoker to show it relative to. Open it with show(invoker, x, y) or showAt" })
            }
        }

    /** The element last passed to [show] / [showAt]. */
    private var explicitInvoker: WComponent? = null

    /** The element that made this flyout its ContextFlyout or Flyout (a weak reference; a candidate for [invoker]). */
    private val attachedOwners = ArrayList<WeakReference<WComponent>>()

    /** Made the ContextFlyout / Flyout of [owner] (called by WComponent.contextFlyout, WButton.flyout, etc.). */
    internal fun attachedTo(owner: WComponent) {
        attachedOwners.removeAll { it.get().let { o -> o == null || o === owner } }
        attachedOwners += WeakReference(owner)
    }

    /**
     * JPopupMenu.getInvoker-like: the reference element when it was last opened (FlyoutBase.Target). The element passed to [show] / [showAt], or
     * among the elements that made this flyout their contextFlyout / componentPopupMenu / button flyout, the one equal to Target.
     * null if not opened yet. When Target is a part inside a template (the right side of a SplitButton, etc.) and does not match,
     * the element passed to [show] / [showAt], or if none, the element this flyout was last attached to.
     */
    val invoker: WComponent?
        get() {
            val target = flyoutBase.getPtrOrNull(XamlInterop.IFlyoutBase_get_Target) ?: return null
            try {
                val candidates = listOfNotNull(explicitInvoker) + attachedOwners.mapNotNull { it.get() }
                return candidates.firstOrNull { Xaml.sameObject(it.frameworkElement, target) }
                    ?: explicitInvoker
                    ?: candidates.lastOrNull()
            } finally {
                target.release()
            }
        }

    /** The subscription to open/close events (calling [WPopupMenuListener]s and deferring reopening while closing). Subscribed on first use. */
    internal val events: FlyoutPopupMenuEvents by lazy { FlyoutPopupMenuEvents(this) }

    /**
     * JPopupMenu.addPopupMenuListener-like: adds an open/close listener. Just before opening (Opening), popupMenuWillBecomeVisible;
     * just before closing (Closing), popupMenuCanceled if canceled, then popupMenuWillBecomeInvisible, in the order added.
     * Canceled means it closed with an outside click or Esc without choosing an item (not including [hide] / `isVisible = false`; same as Swing).
     * In WinUI, item ActionListeners are called before popupMenuWillBecomeInvisible (after in Swing; MenuFlyoutItem.Invoke
     * raises Click and then closes the menu).
     */
    fun addPopupMenuListener(listener: WPopupMenuListener) {
        events.add(listener)
    }

    /** JPopupMenu.removePopupMenuListener-like: removes a listener added with [addPopupMenuListener]. Does nothing if not registered. */
    fun removePopupMenuListener(listener: WPopupMenuListener) {
        events.remove(listener)
    }

    /** JPopupMenu.getPopupMenuListeners-like: the added listeners (in the order added; a new array on each call). */
    val popupMenuListeners: Array<WPopupMenuListener>
        get() = events.all

    /** The items that close it when chosen (used to judge popupMenuCanceled). [WMenuFlyout] returns them including inside submenus. */
    internal open fun invocableMenuItems(): List<WMenuFlyoutItem> = emptyList()
}

/** JPopupMenu-like: WinUI 3's Flyout. Set on WButton.flyout to open it on click. */
class WFlyout @JvmOverloads constructor(content: WComponent? = null) : WFlyoutBase(
    Activation.composeDefault(XamlInterop.CLS_Flyout, XamlInterop.IID_IFlyoutFactory),
) {
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IFlyout_put_Content, value?.uiElement?.ptr)
        }

    init {
        if (content != null) this.content = content
    }
}
