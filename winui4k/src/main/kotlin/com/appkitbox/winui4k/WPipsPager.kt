package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.PipsPagerButtonVisibility (how the previous/next page buttons are shown).
 * Values extracted from the winmd (Visible=0, VisibleOnPointerOver=1, Collapsed=2).
 */
enum class PipsPagerButtonVisibility(internal val native: Int) {
    /** Always show it. */
    VISIBLE(0),

    /** Show only when the pointer is hovering. */
    VISIBLE_ON_POINTER_OVER(1),

    /** Don't show it (the default). */
    COLLAPSED(2),
    ;

    internal companion object {
        fun of(native: Int): PipsPagerButtonVisibility = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's PipsPager (a Control subclass). A pager that lets users move between pages via a
 * row of dots (pips) instead of explicit page numbers. No Swing equivalent, so we keep WinUI's
 * class name as-is.
 *
 * Shows [numberOfPages] dots, and [selectedPageIndex] is the current page. Subscribe to
 * selection changes via [addSelectedIndexChangedListener] (SelectedIndexChanged).
 */
class WPipsPager : WControl(
    Activation.composeDefault(Abi.CLS_PipsPager, Abi.IID_IPipsPagerFactory), // default interface = IPipsPager
) {
    /** SelectedIndexChanged event tokens registered via addSelectedIndexChangedListener. */
    private val selectedIndexChangedTokens = ListenerTokens<(Int) -> Unit>()

    /** The total page count (PipsPager.NumberOfPages). -1 means unbounded. */
    var numberOfPages: Int
        get() = inspectable.getInt(Abi.IPipsPager_get_NumberOfPages)
        set(value) = inspectable.call(Abi.IPipsPager_put_NumberOfPages, value)

    /** The currently selected page (PipsPager.SelectedPageIndex, 0-based). */
    var selectedPageIndex: Int
        get() = inspectable.getInt(Abi.IPipsPager_get_SelectedPageIndex)
        set(value) = inspectable.call(Abi.IPipsPager_put_SelectedPageIndex, value)

    /** The maximum number of dots shown at once (PipsPager.MaxVisiblePips). Excess pips scroll into view. */
    var maxVisiblePips: Int
        get() = inspectable.getInt(Abi.IPipsPager_get_MaxVisiblePips)
        set(value) = inspectable.call(Abi.IPipsPager_put_MaxVisiblePips, value)

    /** The direction the dots are laid out (PipsPager.Orientation, default HORIZONTAL). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(Abi.IPipsPager_get_Orientation))
        set(value) = inspectable.call(Abi.IPipsPager_put_Orientation, value.native)

    /** How the previous-page button is shown (PipsPager.PreviousButtonVisibility, default COLLAPSED). */
    var previousButtonVisibility: PipsPagerButtonVisibility
        get() = PipsPagerButtonVisibility.of(inspectable.getInt(Abi.IPipsPager_get_PreviousButtonVisibility))
        set(value) = inspectable.call(Abi.IPipsPager_put_PreviousButtonVisibility, value.native)

    /** How the next-page button is shown (PipsPager.NextButtonVisibility, default COLLAPSED). */
    var nextButtonVisibility: PipsPagerButtonVisibility
        get() = PipsPagerButtonVisibility.of(inspectable.getInt(Abi.IPipsPager_get_NextButtonVisibility))
        set(value) = inspectable.call(Abi.IPipsPager_put_NextButtonVisibility, value.native)

    /**
     * Subscribes to changes of the selected page (PipsPager.SelectedIndexChanged). The listener
     * receives the new [selectedPageIndex].
     */
    fun addSelectedIndexChangedListener(listener: (Int) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.PipsPagerSelectedIndexChangedHandler",
            Abi.IID_PipsPagerSelectedIndexChangedHandler,
            Abi.IPipsPager_add_SelectedIndexChanged,
        ) { _, _ ->
            // args (PipsPagerSelectedIndexChangedEventArgs) carries no information, so read the
            // selection from the control itself
            listener(selectedPageIndex)
        }
        selectedIndexChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectedIndexChangedListener]. */
    fun removeSelectedIndexChangedListener(listener: (Int) -> Unit) {
        val token = selectedIndexChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IPipsPager_remove_SelectedIndexChanged, token)
    }
}
