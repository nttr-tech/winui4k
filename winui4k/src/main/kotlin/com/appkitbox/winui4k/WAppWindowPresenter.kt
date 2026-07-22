package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WindowingInterop

/**
 * Microsoft.UI.Windowing.AppWindowPresenterKind (an OS default behavior set).
 * Values are extracted from the winmd (Default=0, CompactOverlay=1, FullScreen=2, Overlapped=3).
 */
enum class WAppWindowPresenterKind(internal val native: Int) {
    /** The OS default behavior (on a normal desktop, equivalent to Overlapped). */
    DEFAULT(0),

    /** A compact overlay that always floats on top. */
    COMPACT_OVERLAY(1),

    /** Full-screen display. */
    FULL_SCREEN(2),

    /** A normal overlapped window (can be maximized/minimized/resized). */
    OVERLAPPED(3),
    ;

    internal companion object {
        fun of(native: Int): WAppWindowPresenterKind = entries.first { it.native == native }
    }
}

/**
 * The common base of WinUI 3's AppWindowPresenter (a window's behavior set). No Swing equivalent.
 * Create one of [WOverlappedPresenter] / [WFullScreenPresenter] / [WCompactOverlayPresenter] via
 * each companion's `create()`, and pass it to [WAppWindow.setPresenter].
 */
sealed class WAppWindowPresenter protected constructor(internal val presenterPtr: ComPtr)

/**
 * Microsoft.UI.Windowing.OverlappedPresenterState (the current display state).
 * Values are extracted from the winmd (Maximized=0, Minimized=1, Restored=2).
 */
enum class OverlappedPresenterState(internal val native: Int) {
    /** Currently maximized. */
    MAXIMIZED(0),

    /** Currently minimized. */
    MINIMIZED(1),

    /** Normal display. */
    RESTORED(2),
    ;

    internal companion object {
        fun of(native: Int): OverlappedPresenterState = entries.first { it.native == native }
    }
}

/**
 * The behavior of a normal overlapped window: WinUI 3's OverlappedPresenter.
 * Controls whether it can be maximized/minimized/resized, and its min/max size.
 *
 * Use [create] for a normal window and [createForDialog] for a dialog (thin border, not shown
 * in the taskbar).
 */
class WOverlappedPresenter internal constructor(ptr: ComPtr) : WAppWindowPresenter(ptr) {
    /** An IOverlappedPresenter3 view for PreferredMinimum/MaximumWidth/Height. */
    private val presenter3: ComPtr by lazy { presenterPtr.queryInterface(WindowingInterop.IID_IOverlappedPresenter3) }

    /** Whether the border (including the caption) is shown (read-only; change it with [setBorderAndTitleBar]). */
    val hasBorder: Boolean get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_HasBorder)

    /** Whether the title bar is shown (read-only; change it with [setBorderAndTitleBar]). */
    val hasTitleBar: Boolean get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_HasTitleBar)

    /** Whether it's always shown on top. */
    var isAlwaysOnTop: Boolean
        get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_IsAlwaysOnTop)
        set(value) = presenterPtr.putBool(WindowingInterop.IOverlappedPresenter_put_IsAlwaysOnTop, value)

    /** Whether the maximize button/action is enabled. */
    var isMaximizable: Boolean
        get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_IsMaximizable)
        set(value) = presenterPtr.putBool(WindowingInterop.IOverlappedPresenter_put_IsMaximizable, value)

    /** Whether the minimize button/action is enabled. */
    var isMinimizable: Boolean
        get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_IsMinimizable)
        set(value) = presenterPtr.putBool(WindowingInterop.IOverlappedPresenter_put_IsMinimizable, value)

    /**
     * Whether it's modal. Requires an owner window (use this on an AppWindow created via
     * [WAppWindow.create] with an owner specified).
     */
    var isModal: Boolean
        get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_IsModal)
        set(value) = presenterPtr.putBool(WindowingInterop.IOverlappedPresenter_put_IsModal, value)

    /** Whether resizing is enabled. */
    var isResizable: Boolean
        get() = presenterPtr.getBool(WindowingInterop.IOverlappedPresenter_get_IsResizable)
        set(value) = presenterPtr.putBool(WindowingInterop.IOverlappedPresenter_put_IsResizable, value)

    /** The current display state (maximized/minimized/normal, read-only). */
    val state: OverlappedPresenterState
        get() = OverlappedPresenterState.of(presenterPtr.getInt(WindowingInterop.IOverlappedPresenter_get_State))

    /** The minimum height (px). Setting it to null clears the constraint (IReference<Int32>). */
    var preferredMinimumHeight: Int?
        get() = getPreferredInt(WindowingInterop.IOverlappedPresenter3_get_PreferredMinimumHeight)
        set(value) = putPreferredInt(WindowingInterop.IOverlappedPresenter3_put_PreferredMinimumHeight, value)

    /** The minimum width (px). Setting it to null clears the constraint (IReference<Int32>). */
    var preferredMinimumWidth: Int?
        get() = getPreferredInt(WindowingInterop.IOverlappedPresenter3_get_PreferredMinimumWidth)
        set(value) = putPreferredInt(WindowingInterop.IOverlappedPresenter3_put_PreferredMinimumWidth, value)

    /** The maximum width (px). Setting it to null clears the constraint (IReference<Int32>). */
    var preferredMaximumWidth: Int?
        get() = getPreferredInt(WindowingInterop.IOverlappedPresenter3_get_PreferredMaximumWidth)
        set(value) = putPreferredInt(WindowingInterop.IOverlappedPresenter3_put_PreferredMaximumWidth, value)

    /** The maximum height (px). Setting it to null clears the constraint (IReference<Int32>). */
    var preferredMaximumHeight: Int?
        get() = getPreferredInt(WindowingInterop.IOverlappedPresenter3_get_PreferredMaximumHeight)
        set(value) = putPreferredInt(WindowingInterop.IOverlappedPresenter3_put_PreferredMaximumHeight, value)

    /** Maximizes the window (OverlappedPresenter.Maximize). */
    fun maximize() = presenterPtr.call(WindowingInterop.IOverlappedPresenter_Maximize)

    /** Minimizes the window (OverlappedPresenter.Minimize). */
    fun minimize() = presenterPtr.call(WindowingInterop.IOverlappedPresenter_Minimize)

    /** Restores the window to normal display (OverlappedPresenter.Restore). */
    fun restore() = presenterPtr.call(WindowingInterop.IOverlappedPresenter_Restore)

    /** Toggles the border and title bar's visibility together (OverlappedPresenter.SetBorderAndTitleBar). */
    fun setBorderAndTitleBar(hasBorder: Boolean, hasTitleBar: Boolean) {
        presenterPtr.callWith(
            WindowingInterop.IOverlappedPresenter_SetBorderAndTitleBar,
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.U8, ArgKind.U8),
            if (hasBorder) 1.toByte() else 0.toByte(),
            if (hasTitleBar) 1.toByte() else 0.toByte(),
        )
    }

    private fun getPreferredInt(slot: Int): Int? {
        val boxed = presenter3.getPtrOrNull(slot) ?: return null
        return try {
            PropertyValues.unboxInt(boxed)
        } finally {
            boxed.release()
        }
    }

    private fun putPreferredInt(slot: Int, value: Int?) {
        if (value == null) {
            presenter3.call(slot, null)
            return
        }
        val boxed = PropertyValues.boxInt(value)
        val reference = boxed.queryInterface(FoundationInterop.IID_IReference_Int32)
        presenter3.call(slot, reference.ptr)
        reference.release()
        boxed.release()
    }

    companion object {
        /** Creates a new OverlappedPresenter for a normal window (OverlappedPresenterStatics.Create). */
        fun create(): WOverlappedPresenter {
            val statics = Activation.factory(WindowingInterop.CLS_OverlappedPresenter, WindowingInterop.IID_IOverlappedPresenterStatics)
            return try {
                WOverlappedPresenter(statics.getPtr(WindowingInterop.IOverlappedPresenterStatics_Create))
            } finally {
                statics.release()
            }
        }

        /**
         * Creates an OverlappedPresenter for a dialog (thin border, not shown in the taskbar)
         * (OverlappedPresenterStatics.CreateForDialog). Also set [isModal] to true to make it modal.
         */
        fun createForDialog(): WOverlappedPresenter {
            val statics = Activation.factory(WindowingInterop.CLS_OverlappedPresenter, WindowingInterop.IID_IOverlappedPresenterStatics)
            return try {
                WOverlappedPresenter(statics.getPtr(WindowingInterop.IOverlappedPresenterStatics_CreateForDialog))
            } finally {
                statics.release()
            }
        }
    }
}

/**
 * The behavior of full-screen display: WinUI 3's FullScreenPresenter. It has no properties of its
 * own, and only provides creation.
 * To return, use [WAppWindow.setPresenter] to switch to a different presenter (the default is
 * [WAppWindowPresenterKind.DEFAULT]).
 */
class WFullScreenPresenter internal constructor(ptr: ComPtr) : WAppWindowPresenter(ptr) {
    companion object {
        /** Creates a new FullScreenPresenter (FullScreenPresenterStatics.Create). */
        fun create(): WFullScreenPresenter {
            val statics = Activation.factory(WindowingInterop.CLS_FullScreenPresenter, WindowingInterop.IID_IFullScreenPresenterStatics)
            return try {
                WFullScreenPresenter(statics.getPtr(WindowingInterop.IFullScreenPresenterStatics_Create))
            } finally {
                statics.release()
            }
        }
    }
}

/**
 * Microsoft.UI.Windowing.CompactOverlaySize (CompactOverlay's initial size).
 * Values are extracted from the winmd (Small=0, Medium=1, Large=2).
 */
enum class CompactOverlaySize(internal val native: Int) {
    /** Small size. */
    SMALL(0),

    /** Medium size (default). */
    MEDIUM(1),

    /** Large size. */
    LARGE(2),
    ;

    internal companion object {
        fun of(native: Int): CompactOverlaySize = entries.first { it.native == native }
    }
}

/**
 * A compact overlay that always floats on top: WinUI 3's CompactOverlayPresenter
 * (a picture-in-picture-like window).
 */
class WCompactOverlayPresenter internal constructor(ptr: ComPtr) : WAppWindowPresenter(ptr) {
    /** The initial size (CompactOverlayPresenter.InitialSize). */
    var initialSize: CompactOverlaySize
        get() = CompactOverlaySize.of(presenterPtr.getInt(WindowingInterop.ICompactOverlayPresenter_get_InitialSize))
        set(value) = presenterPtr.call(WindowingInterop.ICompactOverlayPresenter_put_InitialSize, value.native)

    companion object {
        /** Creates a new CompactOverlayPresenter (CompactOverlayPresenterStatics.Create). */
        fun create(): WCompactOverlayPresenter {
            val statics = Activation.factory(WindowingInterop.CLS_CompactOverlayPresenter, WindowingInterop.IID_ICompactOverlayPresenterStatics)
            return try {
                WCompactOverlayPresenter(statics.getPtr(WindowingInterop.ICompactOverlayPresenterStatics_Create))
            } finally {
                statics.release()
            }
        }
    }
}
